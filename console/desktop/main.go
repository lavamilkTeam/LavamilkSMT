// smt-desktop hosts the existing Web UI; it never starts or stops controller.
package main

import (
	"context"
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/lavamilkTeam/LavamilkSMT/console/internal/webui"
	"github.com/wailsapp/wails/v2"
	"github.com/wailsapp/wails/v2/pkg/options"
	"github.com/wailsapp/wails/v2/pkg/options/assetserver"
	"github.com/wailsapp/wails/v2/pkg/options/linux"
	"github.com/wailsapp/wails/v2/pkg/runtime"
)

func main() {
	kiosk := flag.Bool("kiosk", false, "fullscreen, frameless device panel")
	software := flag.Bool("software-rendering", false, "disable WebKit hardware acceleration for legacy display drivers")
	flag.Parse()
	if flag.NArg() != 0 {
		log.Fatal("unexpected positional arguments; use --help")
	}
	state := options.Normal
	if *kiosk {
		state = options.Fullscreen
	}
	policy := linux.WebviewGpuPolicyOnDemand
	if *software {
		policy = linux.WebviewGpuPolicyNever
	}
	signals, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	err := wails.Run(&options.App{
		Title: "Lavamilk SMT 控制面板", Width: 1280, Height: 800,
		WindowStartState: state, Frameless: *kiosk,
		BackgroundColour: options.NewRGB(237, 241, 244),
		AssetServer:      &assetserver.Options{Handler: webui.Handler()},
		Linux:            &linux.Options{WebviewGpuPolicy: policy, ProgramName: "smt-desktop"},
		OnStartup: func(ctx context.Context) {
			go func() {
				select {
				case <-signals.Done():
					runtime.Quit(ctx)
				case <-ctx.Done():
				}
			}()
		},
		// No JS-to-Go bindings: the shared browser UI must remain portable.
	})
	if err != nil {
		log.Fatal(err)
	}
}
