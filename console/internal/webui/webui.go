// Package webui serves the single web build shared by the desktop and HTTP hosts.
// It owns static assets only; machine operations belong to controller APIs.
package webui

import (
	"embed"
	"io/fs"
	"net/http"
	"strings"
)

// Build with console/build-panel.sh, which stages console/web/dist here.
//
//go:embed all:dist
var assets embed.FS

// Handler returns a read-only static UI handler. Unknown API and asset paths
// return 404, never the SPA HTML (which could mask a disconnected backend).
func Handler() http.Handler {
	root, err := fs.Sub(assets, "dist")
	if err != nil {
		panic(err)
	}
	files := http.FileServerFS(root)
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("Cache-Control", "no-cache")
		if r.Method != http.MethodGet && r.Method != http.MethodHead {
			w.Header().Set("Allow", "GET, HEAD")
			http.Error(w, "read-only panel host", http.StatusMethodNotAllowed)
			return
		}
		if r.URL.Path == "/healthz" {
			w.Header().Set("Content-Type", "application/json")
			if r.Method != http.MethodHead {
				_, _ = w.Write([]byte(`{"service":"smt-panel","mode":"simulation","machine_connected":false}`))
			}
			return
		}
		name := strings.TrimPrefix(r.URL.Path, "/")
		if name == "" {
			name = "index.html"
		}
		info, err := fs.Stat(root, name)
		if err != nil || info.IsDir() || name == "api" || strings.HasPrefix(name, "api/") {
			http.NotFound(w, r)
			return
		}
		files.ServeHTTP(w, r)
	})
}
