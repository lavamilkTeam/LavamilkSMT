package release

import (
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestSignedReleaseAndDownload(t *testing.T) {
	public, private, _ := ed25519.GenerateKey(rand.Reader)
	image := []byte("test-only-image")
	digest := sha256.Sum256(image)
	var manifest Manifest
	corruptSignature := false
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/image.bin" {
			w.Write(image)
			return
		}
		payload, _ := json.Marshal(manifest)
		signature := ed25519.Sign(private, payload)
		if corruptSignature {
			signature[0] ^= 1
		}
		json.NewEncoder(w).Encode(map[string]string{"payload": base64.StdEncoding.EncodeToString(payload), "signature": base64.StdEncoding.EncodeToString(signature)})
	}))
	defer server.Close()
	source := New(Config{ManifestURL: server.URL + "/latest.json", PublicKey: base64.StdEncoding.EncodeToString(public), Board: "test-board", Protocol: 1, Address: 0x08000000, MaxSize: 1024 * 1024})
	source.Client.Transport = server.Client().Transport
	manifest = Manifest{Version: "v1", Board: "test-board", MCU: "STM32F407ZGT6", Protocol: 1, Address: 0x08000000, Size: int64(len(image)), SHA256: hex.EncodeToString(digest[:]), URL: server.URL + "/image.bin"}
	m, err := source.Latest(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	directory := t.TempDir()
	if err := source.Download(context.Background(), m, directory); err != nil {
		t.Fatal(err)
	}
	if data, _ := os.ReadFile(filepath.Join(directory, "image.bin")); string(data) != string(image) {
		t.Fatal("image differs")
	}
	image[0] ^= 1
	bad := t.TempDir()
	if source.Download(context.Background(), m, bad) == nil {
		t.Fatal("accepted corrupt download")
	}
	if _, err := os.Stat(filepath.Join(bad, "image.bin")); !os.IsNotExist(err) {
		t.Fatal("published corrupt image")
	}
	corruptSignature = true
	if _, err := source.Latest(context.Background()); err == nil {
		t.Fatal("accepted invalid signature")
	}
	corruptSignature = false
	manifest.Board = "other-board"
	if _, err := source.Latest(context.Background()); err == nil {
		t.Fatal("accepted wrong hardware")
	}
	manifest.Board = "test-board"
	manifest.URL = "https://another-origin.invalid/image.bin"
	if _, err := source.Latest(context.Background()); err == nil {
		t.Fatal("accepted a different download origin")
	}
	manifest.URL = server.URL + "/image.bin"
	manifest.Size = source.Config.MaxSize + 1
	if _, err := source.Latest(context.Background()); err == nil {
		t.Fatal("accepted an image outside the allowed Flash region")
	}
	source.Config.Address++
	if source.Ready() == nil {
		t.Fatal("accepted Flash region crossing a sector boundary")
	}
}

func TestRejectInsecureReleaseURLs(t *testing.T) {
	if validURL("http://example.com/latest") == nil {
		t.Fatal("accepted HTTP")
	}
	if validURL("https://secret@example.com/latest") == nil {
		t.Fatal("accepted embedded credentials")
	}
}
