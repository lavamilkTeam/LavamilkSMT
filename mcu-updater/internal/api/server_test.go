package api

import (
	"bufio"
	"context"
	"encoding/json"
	"net"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/flash"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/maintenance"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/update"
)

func TestLocalProtocolChecksAndRejectsUnconfiguredStart(t *testing.T) {
	s, err := update.New(t.TempDir(), release.New(release.Config{}), flash.OpenOCD{}, maintenance.Files{})
	if err != nil {
		t.Fatal(err)
	}
	socket := filepath.Join(t.TempDir(), "updater.sock")
	listener, err := net.Listen("unix", socket)
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan error, 1)
	go func() { done <- Serve(ctx, listener, s) }()
	defer func() {
		cancel()
		if err := <-done; err != nil {
			t.Error(err)
		}
	}()
	for _, tc := range []struct{ payload, code string }{
		{`{"protocol_version":1,"request_id":"1","type":"check"}`, ""},
		{`{"protocol_version":1,"request_id":"2","type":"start","task_id":"0123456789abcdef0123456789abcdef","version":"v1","sha256":"digest"}`, "UPDATE_REJECTED"},
		{`{"protocol_version":2,"request_id":"3","type":"check"}`, "PROTOCOL_MISMATCH"},
		{`{"protocol_version":1,"request_id":"4","type":"shell"}`, "UNSUPPORTED_REQUEST"},
		{strings.Repeat("x", 9000), "INVALID_MESSAGE"},
	} {
		conn, err := net.Dial("unix", socket)
		if err != nil {
			t.Fatal(err)
		}
		conn.SetDeadline(time.Now().Add(2 * time.Second))
		conn.Write([]byte(tc.payload + "\n"))
		var response Response
		err = json.NewDecoder(bufio.NewReader(conn)).Decode(&response)
		conn.Close()
		if err != nil {
			t.Fatal(err)
		}
		if tc.code == "" {
			if response.Error != nil {
				t.Fatal(response.Error)
			}
		} else if response.Error == nil || response.Error.Code != tc.code {
			t.Fatalf("%s: %+v", tc.code, response)
		}
	}
	if s.Status() != nil {
		t.Fatal("invalid requests started a task")
	}
}
