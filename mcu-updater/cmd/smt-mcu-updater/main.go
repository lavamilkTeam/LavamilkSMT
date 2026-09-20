package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/api"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/flash"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/maintenance"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/update"
)

type config struct {
	Release     release.Config    `json:"release"`
	Flash       flash.OpenOCD     `json:"flash"`
	Maintenance maintenance.Files `json:"maintenance"`
}

func run() error {
	configPath := flag.String("config", "", "operator-owned JSON config; omitted means unconfigured/read-only")
	directory := flag.String("state-dir", "local/mcu-updater", "persistent tasks and logs")
	socketPath := flag.String("socket", "local/mcu-updater/service.sock", "controller-only Unix socket")
	flag.Parse()
	var c config
	if *configPath != "" {
		file, err := os.Open(*configPath)
		if err != nil {
			return err
		}
		defer file.Close()
		decoder := json.NewDecoder(io.LimitReader(file, 65536))
		decoder.DisallowUnknownFields()
		if err := decoder.Decode(&c); err != nil {
			return err
		}
		if err := decoder.Decode(new(any)); err != io.EOF {
			return errors.New("unexpected trailing configuration data")
		}
	}
	var err error
	*directory, err = filepath.Abs(*directory)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(*directory, 0700); err != nil {
		return err
	}
	lock, err := os.OpenFile(filepath.Join(*directory, "process.lock"), os.O_CREATE|os.O_RDWR, 0600)
	if err != nil {
		return err
	}
	defer lock.Close()
	if err := syscall.Flock(int(lock.Fd()), syscall.LOCK_EX|syscall.LOCK_NB); err != nil {
		return errors.New("another updater owns this state directory")
	}
	service, err := update.New(*directory, release.New(c.Release), c.Flash, c.Maintenance)
	if err != nil {
		return err
	}
	defer service.Wait()
	if err := os.MkdirAll(filepath.Dir(*socketPath), 0700); err != nil {
		return err
	}
	// Refuse an existing endpoint instead of unlinking another process's socket.
	listener, err := net.Listen("unix", *socketPath)
	if err != nil {
		return err
	}
	defer listener.Close()
	if err := os.Chmod(*socketPath, 0600); err != nil {
		return err
	}
	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()
	fmt.Printf("smt-mcu-updater listening on %s\n", *socketPath)
	return api.Serve(ctx, listener, service)
}

func main() {
	if err := run(); err != nil {
		log.Fatal(err)
	}
}
