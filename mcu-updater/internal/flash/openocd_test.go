package flash

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
)

func TestOpenOCDUsesFixedArgumentsAndChecksExit(t *testing.T) {
	dir := t.TempDir()
	tool := filepath.Join(dir, "fake-openocd")
	os.WriteFile(tool, []byte("#!/bin/sh\nprintf '%s\\n' \"$@\"\nexit 0\n"), 0700)
	programmer := OpenOCD{Executable: tool, Serial: "0123456789ABCDEF"}
	if err := programmer.Program(context.Background(), dir, release.Manifest{Address: 0x08000000}); err != nil {
		t.Fatal(err)
	}
	data, _ := os.ReadFile(filepath.Join(dir, "openocd.log"))
	if !strings.Contains(string(data), "program image.bin 0x08000000 verify reset exit") || !strings.Contains(string(data), "adapter serial 0123456789ABCDEF") {
		t.Fatal(string(data))
	}
	programmer.Serial = "01234567; shutdown"
	if programmer.Ready() == nil {
		t.Fatal("accepted Tcl command injection")
	}
	programmer.Serial = "01234567"
	os.WriteFile(tool, []byte("#!/bin/sh\nexit 1\n"), 0700)
	if programmer.Program(context.Background(), t.TempDir(), release.Manifest{}) == nil {
		t.Fatal("ignored flashing failure")
	}
}
