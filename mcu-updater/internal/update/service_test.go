package update

import (
	"context"
	"errors"
	"os"
	"path/filepath"
	"sync/atomic"
	"testing"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/storage"
)

type testSource struct {
	downloadErr error
	wait        chan struct{}
}

func (s testSource) Ready() error { return nil }
func (s testSource) Latest(context.Context) (release.Manifest, error) {
	return release.Manifest{Version: "v1", SHA256: "digest"}, nil
}
func (s testSource) Download(context.Context, release.Manifest, string) error {
	if s.wait != nil {
		<-s.wait
	}
	return s.downloadErr
}

type testFlash struct {
	count atomic.Int32
	fail  bool
}

func (f *testFlash) Ready() error { return nil }
func (f *testFlash) Program(context.Context, string, release.Manifest) error {
	f.count.Add(1)
	if f.fail {
		return errors.New("USB disconnected")
	}
	return nil
}

type testMaintenance struct {
	permit   bool
	released atomic.Bool
}

func (m *testMaintenance) Ready() error { return nil }
func (m *testMaintenance) Acquire(string, release.Manifest) error {
	if !m.permit {
		return errors.New("no permission")
	}
	return nil
}
func (m *testMaintenance) Release(string) error { m.released.Store(true); return nil }

const testID = "0123456789abcdef0123456789abcdef"

func TestTaskDetachedFromRequestAndIdempotent(t *testing.T) {
	wait := make(chan struct{})
	flash, gate := &testFlash{}, &testMaintenance{permit: true}
	s, err := New(t.TempDir(), testSource{wait: wait}, flash, gate)
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithCancel(context.Background())
	first, err := s.Start(ctx, testID, "v1", "digest")
	if err != nil {
		t.Fatal(err)
	}
	cancel()
	again, err := s.Start(context.Background(), testID, "v1", "digest")
	if err != nil || first.ID != again.ID {
		t.Fatal("retry was not idempotent", err)
	}
	if _, err := s.Start(context.Background(), "1123456789abcdef0123456789abcdef", "v1", "digest"); err == nil {
		t.Fatal("allowed parallel update")
	}
	close(wait)
	s.Wait()
	if s.Status().State != "awaiting_confirmation" || flash.count.Load() != 1 || gate.released.Load() {
		t.Fatal("flash completion incorrectly released maintenance")
	}
	if _, err := s.Confirm(testID, "wrong"); err == nil {
		t.Fatal("accepted wrong running version")
	}
	if _, err := s.Confirm(testID, "v1"); err != nil {
		t.Fatal(err)
	}
	if !gate.released.Load() || s.Status().State != "succeeded" {
		t.Fatal("confirmation not recorded")
	}
}

func TestNoPermitAndFailedDownloadsNeverFlash(t *testing.T) {
	for _, permit := range []bool{false, true} {
		flash, gate := &testFlash{}, &testMaintenance{permit: permit}
		s, err := New(t.TempDir(), testSource{downloadErr: errors.New("checksum mismatch")}, flash, gate)
		if err != nil {
			t.Fatal(err)
		}
		_, err = s.Start(context.Background(), testID, "v1", "digest")
		if !permit && err == nil {
			t.Fatal("accepted without permit")
		}
		s.Wait()
		if flash.count.Load() != 0 || gate.released.Load() {
			t.Fatal("failure touched hardware or released maintenance")
		}
		if permit && s.Status().State != "failed" {
			t.Fatal("download failure not recorded")
		}
	}
}

func TestRestartDoesNotRetryFlash(t *testing.T) {
	dir := t.TempDir()
	os.MkdirAll(filepath.Join(dir, "tasks", testID), 0700)
	storage.Save(filepath.Join(dir, "current.json"), Task{ID: testID, State: "flashing"})
	flash := &testFlash{}
	s, err := New(dir, testSource{}, flash, &testMaintenance{})
	if err != nil {
		t.Fatal(err)
	}
	if s.Status().State != "interrupted" || flash.count.Load() != 0 {
		t.Fatal("restarted a physical operation")
	}
}

func TestFlashingFailureKeepsMaintenanceAndBlocksNewTasks(t *testing.T) {
	flash, gate := &testFlash{fail: true}, &testMaintenance{permit: true}
	s, err := New(t.TempDir(), testSource{}, flash, gate)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := s.Start(context.Background(), testID, "v1", "digest"); err != nil {
		t.Fatal(err)
	}
	s.Wait()
	if s.Status().State != "failed" || gate.released.Load() || flash.count.Load() != 1 {
		t.Fatal("flash failure was not latched")
	}
	if _, err := s.Start(context.Background(), "1123456789abcdef0123456789abcdef", "v1", "digest"); err == nil {
		t.Fatal("accepted a second flash after failure")
	}
}
