// Package flash owns the OpenOCD process, never the machine's maintenance decision.
package flash

import (
	"context"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
)

type OpenOCD struct {
	Executable string `json:"executable"`
	Serial     string `json:"serial"`
}

func (o OpenOCD) Ready() error {
	if !regexp.MustCompile(`^[0-9A-Fa-f]{8,64}$`).MatchString(o.Serial) {
		return errors.New("configure the exact ST-LINK serial first")
	}
	if o.Executable == "" {
		return errors.New("configure OpenOCD first")
	}
	_, err := exec.LookPath(o.Executable)
	return err
}

// cappedLog keeps draining process output even after the retained log reaches its limit.
type cappedLog struct {
	file      *os.File
	remaining int
}

func (w *cappedLog) Write(p []byte) (int, error) {
	n := len(p)
	if w.remaining > 0 {
		keep := min(n, w.remaining)
		if _, err := w.file.Write(p[:keep]); err != nil {
			return 0, err
		}
		w.remaining -= keep
	}
	return n, nil
}

func (o OpenOCD) Program(ctx context.Context, directory string, m release.Manifest) error {
	if err := o.Ready(); err != nil {
		return err
	}
	executable, err := exec.LookPath(o.Executable)
	if err != nil {
		return err
	}
	executable, err = filepath.Abs(executable)
	if err != nil {
		return err
	}
	log, err := os.OpenFile(filepath.Join(directory, "openocd.log"), os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	defer log.Close()
	ctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
	defer cancel()
	// Only fixed script names and validated hex serial/address reach the Tcl interpreter.
	cmd := exec.CommandContext(ctx, executable, "-f", "interface/stlink.cfg",
		"-c", "adapter serial "+o.Serial, "-f", "target/stm32f4x.cfg",
		"-c", fmt.Sprintf("program image.bin 0x%08x verify reset exit", m.Address))
	cmd.Dir = directory
	writer := &cappedLog{file: log, remaining: 1024 * 1024}
	cmd.Stdout, cmd.Stderr = writer, writer
	cmd.WaitDelay = 3 * time.Second
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("OpenOCD failed; inspect openocd.log: %w", err)
	}
	return log.Sync()
}
