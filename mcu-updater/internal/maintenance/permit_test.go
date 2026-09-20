package maintenance

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
)

func TestPermitIsBoundToFirmwareAndSharedLockSurvives(t *testing.T) {
	dir := t.TempDir()
	f := Files{PermitPath: filepath.Join(dir, "permit.json"), LockPath: filepath.Join(dir, "update.lock")}
	m := release.Manifest{Version: "v1", Board: "board", SHA256: "digest"}
	if f.Acquire("id", m) == nil {
		t.Fatal("accepted missing permit")
	}
	p := Permit{TaskID: "id", Board: m.Board, Version: m.Version, SHA256: m.SHA256, OutputsSafe: true, ExpiresAt: time.Now().Add(time.Minute)}
	data, _ := json.Marshal(p)
	os.WriteFile(f.PermitPath, data, 0600)
	if f.Acquire("other-id", m) == nil {
		t.Fatal("accepted unrelated task")
	}
	if err := f.Acquire("id", m); err != nil {
		t.Fatal(err)
	}
	if _, err := os.Stat(f.PermitPath); !os.IsNotExist(err) {
		t.Fatal("maintenance grant was not consumed")
	}
	if f.Acquire("id", m) == nil {
		t.Fatal("allowed concurrent or repeated acquisition")
	}
	if f.Release("wrong-id") == nil {
		t.Fatal("released another task's lock")
	}
	if _, err := os.Stat(f.LockPath); err != nil {
		t.Fatal("lock vanished before confirmation")
	}
	if err := f.Release("id"); err != nil {
		t.Fatal(err)
	}
	p.ExpiresAt = time.Now().Add(-time.Minute)
	data, _ = json.Marshal(p)
	os.WriteFile(f.PermitPath, data, 0600)
	if f.Acquire("id", m) == nil {
		t.Fatal("accepted expired permit")
	}
}
