// Package api exposes a local Unix socket to the Python controller, not to the LAN.
package api

import (
	"bufio"
	"context"
	"encoding/json"
	"net"
	"sync"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/update"
)

type Request struct {
	Version        int    `json:"protocol_version"`
	ID             string `json:"request_id"`
	Type           string `json:"type"`
	TaskID         string `json:"task_id,omitempty"`
	ReleaseVersion string `json:"version,omitempty"`
	SHA256         string `json:"sha256,omitempty"`
}
type Response struct {
	Version int      `json:"protocol_version"`
	ID      string   `json:"request_id"`
	Result  any      `json:"result,omitempty"`
	Error   *Failure `json:"error,omitempty"`
}
type Failure struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func Serve(ctx context.Context, listener net.Listener, service *update.Service) error {
	var workers sync.WaitGroup
	defer workers.Wait()
	done := make(chan struct{})
	defer close(done)
	go func() {
		select {
		case <-ctx.Done():
			listener.Close()
		case <-done:
		}
	}()
	capacity := make(chan struct{}, 8)
	for {
		conn, err := listener.Accept()
		if err != nil {
			if ctx.Err() != nil {
				return nil
			}
			return err
		}
		select {
		case capacity <- struct{}{}:
			workers.Add(1)
			go func() { defer workers.Done(); defer func() { <-capacity }(); handle(conn, service) }()
		default:
			conn.Close()
		}
	}
}

func handle(conn net.Conn, service *update.Service) {
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(12 * time.Second))
	data, err := bufio.NewReaderSize(conn, 8192).ReadSlice('\n')
	var request Request
	response := Response{Version: 1}
	if err != nil || len(data) > 8192 || json.Unmarshal(data, &request) != nil || len(request.ID) < 1 || len(request.ID) > 64 {
		response.Error = &Failure{"INVALID_MESSAGE", "expected a bounded JSON request with request_id"}
	} else {
		response.ID = request.ID
		if request.Version != 1 {
			response.Error = &Failure{"PROTOCOL_MISMATCH", "only updater protocol 1 is supported"}
		} else {
			ctx, cancel := context.WithTimeout(context.Background(), 8*time.Second)
			defer cancel()
			switch request.Type {
			case "check":
				response.Result = service.Check(ctx)
			case "status":
				response.Result = map[string]any{"task": service.Status()}
			case "start":
				response.Result, err = service.Start(ctx, request.TaskID, request.ReleaseVersion, request.SHA256)
			case "confirm":
				response.Result, err = service.Confirm(request.TaskID, request.ReleaseVersion)
			default:
				response.Error = &Failure{"UNSUPPORTED_REQUEST", "supported requests: check/status/start/confirm"}
			}
			if err != nil {
				response.Result = nil
				response.Error = &Failure{"UPDATE_REJECTED", err.Error()}
			}
		}
	}
	_ = json.NewEncoder(conn).Encode(response)
}
