// Package maintenance consumes a controller-issued permit; it cannot decide that motion is safe.
package maintenance

import (
	"encoding/json"
	"errors"
	"io"
	"os"
	"path/filepath"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/storage"
)

type Files struct {
	PermitPath string `json:"permit_path"`
	LockPath   string `json:"lock_path"`
}

type Permit struct {
	TaskID      string    `json:"task_id"`
	Board       string    `json:"board"`
	Version     string    `json:"version"`
	SHA256      string    `json:"sha256"`
	OutputsSafe bool      `json:"outputs_safe"`
	ExpiresAt   time.Time `json:"expires_at"`
}

type owner struct {
	Target string `json:"target"`
	TaskID string `json:"task_id"`
}

func (f Files) Ready() error {
	if !filepath.IsAbs(f.PermitPath) || !filepath.IsAbs(f.LockPath) || f.PermitPath == f.LockPath {
		return errors.New("configure distinct absolute maintenance permit and shared lock paths")
	}
	return nil
}

func (f Files) Acquire(id string, m release.Manifest) error {
	if err := f.Ready(); err != nil {
		return err
	}
	info, err := os.Lstat(f.PermitPath)
	if err != nil || !info.Mode().IsRegular() || info.Mode().Perm()&0077 != 0 {
		return errors.New("controller maintenance permit missing or permissions are not 0600")
	}
	file, err := os.Open(f.PermitPath)
	if err != nil {
		return err
	}
	defer file.Close()
	var p Permit
	if err := json.NewDecoder(io.LimitReader(file, 4096)).Decode(&p); err != nil {
		return err
	}
	if p.TaskID != id || p.Board != m.Board || p.Version != m.Version || p.SHA256 != m.SHA256 ||
		!p.OutputsSafe || !p.ExpiresAt.After(time.Now()) || p.ExpiresAt.After(time.Now().Add(15*time.Minute)) {
		return errors.New("maintenance permit does not authorize this exact firmware task")
	}
	lock, err := os.OpenFile(f.LockPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if err != nil {
		return errors.New("shared update lock unavailable; existing maintenance requires recovery")
	}
	defer lock.Close()
	// A partial lock is deliberately retained on any storage error: fail closed.
	if err := json.NewEncoder(lock).Encode(owner{Target: "mcu", TaskID: id}); err != nil {
		return err
	}
	if err := lock.Sync(); err != nil {
		return err
	}
	if err := storage.SyncDir(filepath.Dir(f.LockPath)); err != nil {
		return err
	}
	// Consume the grant exactly once. After success/resume an old grant must not authorize a new process.
	if err := os.Remove(f.PermitPath); err != nil {
		return err
	}
	return storage.SyncDir(filepath.Dir(f.PermitPath))
}

func (f Files) Release(id string) error {
	data, err := os.ReadFile(f.LockPath)
	if err != nil {
		return err
	}
	var lock owner
	if json.Unmarshal(data, &lock) != nil || lock.Target != "mcu" || lock.TaskID != id {
		return errors.New("maintenance lock owner mismatch")
	}
	if err := os.Remove(f.LockPath); err != nil {
		return err
	}
	return storage.SyncDir(filepath.Dir(f.LockPath))
}
