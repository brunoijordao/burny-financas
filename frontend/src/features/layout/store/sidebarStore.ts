import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface SidebarState {
  isCollapsed: boolean
  /** Toggles between the expanded (icons + labels) and collapsed (icons only) desktop rail. */
  toggleCollapsed: () => void
}

/**
 * Holds the desktop sidebar's collapsed/expanded preference. Persisted to
 * localStorage (unlike the auth session store) since this is a UI
 * preference, not a credential — see design.md Decision 2.
 */
export const useSidebarStore = create<SidebarState>()(
  persist(
    (set) => ({
      isCollapsed: false,
      toggleCollapsed: () => set((state) => ({ isCollapsed: !state.isCollapsed })),
    }),
    { name: 'sidebar-ui' },
  ),
)
