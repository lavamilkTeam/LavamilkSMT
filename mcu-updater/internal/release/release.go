// Package release verifies signed release metadata and downloads bounded firmware images.
package release

import (
	"context"
	"crypto/ed25519"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

type Manifest struct {
	Version  string `json:"version"`
	Board    string `json:"board"`
	MCU      string `json:"mcu"`
	Protocol int    `json:"protocol"`
	Address  uint32 `json:"address"`
	Size     int64  `json:"size"`
	SHA256   string `json:"sha256"`
	URL      string `json:"url"`
}

type Config struct {
	ManifestURL string `json:"manifest_url"`
	PublicKey   string `json:"public_key"` // base64 Ed25519 public key; supplied by the operator, never the download.
	Board       string `json:"board"`
	Protocol    int    `json:"protocol"`
	Address     uint32 `json:"address"`
	MaxSize     int64  `json:"max_size"`
}

type Source struct {
	Config Config
	Client *http.Client
}

func New(config Config) *Source {
	return &Source{Config: config, Client: &http.Client{Timeout: 60 * time.Second,
		CheckRedirect: func(*http.Request, []*http.Request) error { return errors.New("release redirects are not allowed") }}}
}

func (s *Source) Ready() error {
	c := s.Config
	key, err := base64.StdEncoding.DecodeString(c.PublicKey)
	if err != nil || len(key) != ed25519.PublicKeySize || c.Board == "" || c.Protocol < 1 || c.MaxSize <= 0 || c.MaxSize > 1024*1024 ||
		c.Address < 0x08000000 || uint64(c.Address)+uint64(c.MaxSize) > 0x08100000 {
		return errors.New("configure board, protocol, Flash region and release public key first")
	}
	// OpenOCD erases entire F407 sectors; both configured region boundaries must be sector boundaries.
	boundary := func(address uint64) bool {
		for _, offset := range []uint64{0, 0x4000, 0x8000, 0xc000, 0x10000, 0x20000, 0x40000, 0x60000, 0x80000, 0xa0000, 0xc0000, 0xe0000, 0x100000} {
			if address == 0x08000000+offset {
				return true
			}
		}
		return false
	}
	if !boundary(uint64(c.Address)) || !boundary(uint64(c.Address)+uint64(c.MaxSize)) {
		return errors.New("configured Flash region must align with STM32F407 sector boundaries")
	}
	return validURL(c.ManifestURL)
}

func validURL(raw string) error {
	u, err := url.Parse(raw)
	if err != nil || u.Scheme != "https" || u.Host == "" || u.User != nil || u.Fragment != "" {
		return errors.New("release URLs must use HTTPS without credentials or fragments")
	}
	return nil
}

func (s *Source) get(ctx context.Context, raw string) (io.ReadCloser, error) {
	if err := validURL(raw); err != nil {
		return nil, err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, raw, nil)
	if err != nil {
		return nil, err
	}
	resp, err := s.Client.Do(req)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		resp.Body.Close()
		return nil, fmt.Errorf("release HTTP status %d", resp.StatusCode)
	}
	return resp.Body, nil
}

func (s *Source) Latest(ctx context.Context) (Manifest, error) {
	var m Manifest
	if err := s.Ready(); err != nil {
		return m, err
	}
	body, err := s.get(ctx, s.Config.ManifestURL)
	if err != nil {
		return m, err
	}
	defer body.Close()
	data, err := io.ReadAll(io.LimitReader(body, 65537))
	if err != nil || len(data) > 65536 {
		return m, errors.New("invalid or oversized release manifest")
	}
	var envelope struct {
		Payload   string `json:"payload"`
		Signature string `json:"signature"`
	}
	if err := json.Unmarshal(data, &envelope); err != nil {
		return m, err
	}
	payload, err := base64.StdEncoding.DecodeString(envelope.Payload)
	if err != nil {
		return m, err
	}
	sig, err := base64.StdEncoding.DecodeString(envelope.Signature)
	key, _ := base64.StdEncoding.DecodeString(s.Config.PublicKey)
	if err != nil || !ed25519.Verify(key, payload, sig) {
		return m, errors.New("release signature verification failed")
	}
	if err := json.Unmarshal(payload, &m); err != nil {
		return m, err
	}
	if err := s.Validate(m); err != nil {
		return Manifest{}, err
	}
	return m, nil
}

func (s *Source) Validate(m Manifest) error {
	digest, err := hex.DecodeString(m.SHA256)
	if !regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9._+-]{0,63}$`).MatchString(m.Version) ||
		m.Board != s.Config.Board || m.MCU != "STM32F407ZGT6" || m.Protocol != s.Config.Protocol ||
		m.Address != s.Config.Address || m.Size <= 0 || m.Size > s.Config.MaxSize ||
		err != nil || len(digest) != sha256.Size || m.SHA256 != strings.ToLower(m.SHA256) {
		return errors.New("firmware version, board, MCU, protocol, Flash region, size or digest mismatch")
	}
	if err := validURL(m.URL); err != nil {
		return err
	}
	origin, _ := url.Parse(s.Config.ManifestURL)
	image, _ := url.Parse(m.URL)
	if origin.Host != image.Host {
		return errors.New("firmware must use the configured release origin")
	}
	return nil
}

// Download only creates image.bin after its exact size and signed digest match.
func (s *Source) Download(ctx context.Context, m Manifest, directory string) error {
	if err := s.Validate(m); err != nil {
		return err
	}
	body, err := s.get(ctx, m.URL)
	if err != nil {
		return err
	}
	defer body.Close()
	file, err := os.OpenFile(filepath.Join(directory, "image.part"), os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	defer func() { file.Close(); os.Remove(file.Name()) }()
	hash := sha256.New()
	n, err := io.Copy(io.MultiWriter(file, hash), io.LimitReader(body, m.Size+1))
	if err != nil {
		return err
	}
	if n != m.Size || hex.EncodeToString(hash.Sum(nil)) != m.SHA256 {
		return errors.New("firmware size or SHA-256 verification failed")
	}
	if err := file.Sync(); err != nil {
		return err
	}
	if err := file.Close(); err != nil {
		return err
	}
	return os.Rename(file.Name(), filepath.Join(directory, "image.bin"))
}
