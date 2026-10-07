// smt-panel provides browser access without a desktop or Node.js runtime.
package main

import (
	"context"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/console/internal/webui"
)

func main() {
	listen := flag.String("listen", "127.0.0.1:8080", "HTTP listen address; use 0.0.0.0:8080 for LAN access")
	flag.Parse()
	if flag.NArg() != 0 {
		log.Fatal("unexpected positional arguments; use --help")
	}
	srv := &http.Server{Addr: *listen, Handler: webui.Handler(), ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout: 10 * time.Second, WriteTimeout: 30 * time.Second, IdleTimeout: 60 * time.Second, MaxHeaderBytes: 16 << 10}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	done := make(chan struct{})
	go func() {
		defer close(done)
		<-ctx.Done()
		shutdown, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := srv.Shutdown(shutdown); err != nil {
			log.Printf("shutdown: %v", err)
		}
	}()
	log.Printf("SMT Web panel listening on %s (simulation UI, no machine control)", *listen)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatal(err)
	}
	<-done
}
