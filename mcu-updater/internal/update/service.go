// Package update owns durable firmware tasks, independent of API connection lifetimes.
package update

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sync"
	"time"

	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/release"
	"github.com/lavamilkTeam/LavamilkSMT/mcu-updater/internal/storage"
)

type Source interface {
	Ready() error
	Latest(context.Context) (release.Manifest, error)
	Download(context.Context, release.Manifest, string) error
}
type Programmer interface {
	Ready() error
	Program(context.Context, string, release.Manifest) error
}
type Maintenance interface {
	Ready() error
	Acquire(string, release.Manifest) error
	Release(string) error
}

type Task struct {
	ID        string           `json:"id"`
	State     string           `json:"state"`
	Release   release.Manifest `json:"release"`
	Error     string           `json:"error,omitempty"`
	UpdatedAt time.Time        `json:"updated_at"`
}
type Check struct {
	CanUpdate bool              `json:"can_update"`
	Blockers  []string          `json:"blockers"`
	Release   *release.Manifest `json:"release,omitempty"`
	Task      *Task             `json:"task"`
}

type Service struct {
	mu          sync.Mutex
	wg          sync.WaitGroup
	directory   string
	source      Source
	programmer  Programmer
	maintenance Maintenance
	task        *Task
}

func New(directory string, source Source, programmer Programmer, maintenance Maintenance) (*Service, error) {
	if err := os.MkdirAll(filepath.Join(directory, "tasks"), 0700); err != nil {
		return nil, err
	}
	s := &Service{directory: directory, source: source, programmer: programmer, maintenance: maintenance}
	data, err := os.ReadFile(s.statePath())
	if err == nil {
		if err := json.Unmarshal(data, &s.task); err != nil || s.task == nil || !validID(s.task.ID) {
			return nil, errors.New("invalid saved update state; manual recovery required")
		}
		switch s.task.State {
		case "downloading", "flashing":
			if err := s.save("interrupted", "updater restarted before completion; maintenance remains locked"); err != nil {
				return nil, err
			}
		case "awaiting_confirmation", "succeeded", "failed", "interrupted":
		default:
			return nil, errors.New("unknown saved update state; manual recovery required")
		}
	} else if !os.IsNotExist(err) {
		return nil, err
	}
	return s, nil
}

func validID(id string) bool         { return regexp.MustCompile(`^[a-f0-9]{32}$`).MatchString(id) }
func (s *Service) statePath() string { return filepath.Join(s.directory, "current.json") }
func (s *Service) snapshot() *Task {
	if s.task == nil {
		return nil
	}
	copy := *s.task
	return &copy
}
func (s *Service) Status() *Task { s.mu.Lock(); defer s.mu.Unlock(); return s.snapshot() }

func (s *Service) Check(ctx context.Context) Check {
	result := Check{Blockers: []string{}, Task: s.Status()}
	for _, err := range []error{s.source.Ready(), s.programmer.Ready(), s.maintenance.Ready()} {
		if err != nil {
			result.Blockers = append(result.Blockers, err.Error())
		}
	}
	if s.source.Ready() == nil {
		m, err := s.source.Latest(ctx)
		if err != nil {
			result.Blockers = append(result.Blockers, err.Error())
		} else {
			result.Release = &m
		}
	}
	if result.Task != nil && result.Task.State != "succeeded" {
		result.Blockers = append(result.Blockers, "an existing update requires completion or recovery")
	}
	// Readiness is not a maintenance grant; only Start can consume a controller-issued permit.
	result.CanUpdate = false
	result.Blockers = append(result.Blockers, "controller maintenance authorization required")
	return result
}

func (s *Service) save(state, detail string) error {
	s.task.State, s.task.Error, s.task.UpdatedAt = state, detail, time.Now().UTC()
	if err := storage.Save(s.statePath(), s.task); err != nil {
		return err
	}
	return storage.Save(filepath.Join(s.directory, "tasks", s.task.ID, "task.json"), s.task)
}

func (s *Service) Start(ctx context.Context, id, version, digest string) (*Task, error) {
	if !validID(id) {
		return nil, errors.New("task_id must contain 32 lowercase hex characters")
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.task != nil {
		if s.task.ID == id {
			if s.task.Release.Version != version || s.task.Release.SHA256 != digest {
				return nil, errors.New("task_id reused for a different release")
			}
			return s.snapshot(), nil
		}
		if s.task.State != "succeeded" {
			return nil, errors.New("another update is active or requires recovery")
		}
	}
	if _, err := os.Stat(filepath.Join(s.directory, "tasks", id)); !os.IsNotExist(err) {
		return nil, errors.New("task ID already used or task storage unavailable")
	}
	for _, err := range []error{s.source.Ready(), s.programmer.Ready(), s.maintenance.Ready()} {
		if err != nil {
			return nil, err
		}
	}
	m, err := s.source.Latest(ctx)
	if err != nil {
		return nil, err
	}
	if m.Version != version || m.SHA256 != digest {
		return nil, errors.New("release changed; check and select the version again")
	}
	if err := s.maintenance.Acquire(id, m); err != nil {
		return nil, err
	}
	directory := filepath.Join(s.directory, "tasks", id)
	if err := os.Mkdir(directory, 0700); err != nil {
		return nil, err
	}
	s.task = &Task{ID: id, Release: m}
	if err := s.save("downloading", ""); err != nil {
		return nil, err
	}
	s.wg.Add(1)
	go s.run(directory, m)
	return s.snapshot(), nil
}

func (s *Service) transition(state, detail string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.save(state, detail)
}

func (s *Service) run(directory string, m release.Manifest) {
	defer s.wg.Done()
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Minute)
	defer cancel()
	err := s.source.Download(ctx, m, directory)
	if err == nil {
		err = s.transition("flashing", "")
	}
	if err == nil {
		err = s.programmer.Program(ctx, directory, m)
	}
	if err != nil {
		_ = s.transition("failed", err.Error()) // Keep the maintenance record on every failure.
		return
	}
	if err := s.transition("awaiting_confirmation", ""); err != nil {
		_ = s.transition("failed", fmt.Sprintf("cannot persist completion: %v", err))
	}
}

// Confirm is called only after the controller reconnects to real hardware and reads its version.
func (s *Service) Confirm(id, actualVersion string) (*Task, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.task == nil || s.task.ID != id || s.task.Release.Version != actualVersion {
		return nil, errors.New("task or running firmware version mismatch")
	}
	if s.task.State == "succeeded" {
		return s.snapshot(), nil
	}
	if s.task.State != "awaiting_confirmation" {
		return nil, errors.New("firmware has not reached runtime confirmation")
	}
	if err := s.save("succeeded", ""); err != nil {
		return nil, err
	}
	if err := s.maintenance.Release(id); err != nil {
		_ = s.save("failed", "maintenance release failed; manual recovery required")
		return nil, err
	}
	return s.snapshot(), nil
}

// Shutdown drains a running flash operation instead of cancelling it with a UI/API request.
func (s *Service) Wait() { s.wg.Wait() }
