package webui_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"regexp"
	"strings"
	"testing"

	"github.com/lavamilkTeam/LavamilkSMT/console/internal/webui"
)

func TestBuiltUIAndAssets(t *testing.T) {
	h := webui.Handler()
	w := httptest.NewRecorder()
	h.ServeHTTP(w, httptest.NewRequest("GET", "/", nil))
	if w.Code != 200 || !strings.Contains(w.Body.String(), `id="root"`) {
		t.Fatalf("missing built React UI: %d %s", w.Code, w.Body.String())
	}
	refs := regexp.MustCompile(`(?:src|href)="(/assets/[^" ]+)"`).FindAllStringSubmatch(w.Body.String(), -1)
	if len(refs) < 2 {
		t.Fatal("expected built JavaScript and CSS")
	}
	for _, ref := range refs {
		w = httptest.NewRecorder()
		h.ServeHTTP(w, httptest.NewRequest("GET", ref[1], nil))
		if w.Code != 200 || w.Body.Len() == 0 {
			t.Fatalf("unavailable asset %s: %d", ref[1], w.Code)
		}
	}
}

func TestReadOnlyAndMissingPaths(t *testing.T) {
	h := webui.Handler()
	for _, tc := range []struct {
		method, path string
		status       int
	}{
		{"POST", "/", 405}, {"POST", "/healthz", 405},
		{"GET", "/api/machine", 404}, {"GET", "/assets/missing.js", 404},
		{"GET", "/assets/", 404}, {"GET", "/../go.mod", 404}, {"HEAD", "/", 200},
	} {
		t.Run(tc.method+tc.path, func(t *testing.T) {
			w := httptest.NewRecorder()
			h.ServeHTTP(w, httptest.NewRequest(tc.method, tc.path, nil))
			if w.Code != tc.status {
				t.Fatalf("status %d, want %d", w.Code, tc.status)
			}
			if tc.method == "HEAD" && w.Body.Len() != 0 {
				t.Fatal("HEAD has a body")
			}
		})
	}
}

func TestHealthDoesNotClaimMachineReadiness(t *testing.T) {
	w := httptest.NewRecorder()
	webui.Handler().ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	var result struct {
		Mode      string `json:"mode"`
		Connected bool   `json:"machine_connected"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &result); err != nil {
		t.Fatal(err)
	}
	if result.Mode != "simulation" || result.Connected {
		t.Fatalf("unexpected health: %+v", result)
	}
}
