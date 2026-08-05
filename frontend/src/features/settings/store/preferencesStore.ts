import axios from 'axios'
import { create } from 'zustand'

import * as settingsApi from '@/features/settings/api/settingsApi'
import type { UserPreferences } from '@/features/settings/api/settingsApi'

/** Matches the backend's defaults (see design.md Decision 1) so a session that never loads or
 * saves preferences still formats currency/dates exactly as before this feature existed. */
export const DEFAULT_PREFERENCES: UserPreferences = { currency: 'BRL', dateFormat: 'DD/MM/YYYY' }

interface PreferencesState extends UserPreferences {
  /** Fetches the authenticated user's saved preferences; falls back silently to the current
   * (default) state on failure instead of blocking the UI (see design.md Decision 4).
   *
   * Accepts an optional `AbortSignal` so the caller (`AppLayout`) can cancel a stale in-flight
   * request — e.g. React StrictMode's dev-only mount→cleanup→mount double-invoke, which would
   * otherwise fire this GET twice on every session bootstrap. */
  loadPreferences: (signal?: AbortSignal) => Promise<void>
  setPreferences: (preferences: UserPreferences) => void
}

/**
 * Holds the current user's display preferences, hydrated once per session from `AppLayout`.
 * Not persisted to localStorage (unlike `sidebarStore`) since the backend is the source of truth.
 */
export const usePreferencesStore = create<PreferencesState>((set) => ({
  ...DEFAULT_PREFERENCES,
  loadPreferences: async (signal) => {
    try {
      const preferences = await settingsApi.fetchPreferences(signal)
      set(preferences)
    } catch (error) {
      // A cancelled request (StrictMode's double-invoke aborting the stale first call) isn't a
      // real failure — the second call already superseded it. Don't stomp on whatever it sets.
      if (axios.isCancel(error)) {
        return
      }
      set(DEFAULT_PREFERENCES)
    }
  },
  setPreferences: (preferences) => set(preferences),
}))
