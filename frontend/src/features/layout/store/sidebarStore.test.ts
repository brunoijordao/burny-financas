import { afterEach, describe, expect, it } from 'vitest'

import { useSidebarStore } from './sidebarStore'

describe('useSidebarStore', () => {
  afterEach(() => {
    useSidebarStore.setState({ isCollapsed: false })
    window.localStorage.clear()
  })

  it('starts expanded by default', () => {
    expect(useSidebarStore.getState().isCollapsed).toBe(false)
  })

  it('toggleCollapsed flips the state', () => {
    useSidebarStore.getState().toggleCollapsed()
    expect(useSidebarStore.getState().isCollapsed).toBe(true)

    useSidebarStore.getState().toggleCollapsed()
    expect(useSidebarStore.getState().isCollapsed).toBe(false)
  })

  it('persists the collapsed state to localStorage', () => {
    useSidebarStore.getState().toggleCollapsed()
    expect(useSidebarStore.getState().isCollapsed).toBe(true)

    const raw = window.localStorage.getItem('sidebar-ui')
    expect(raw).not.toBeNull()
    expect(JSON.parse(raw as string).state.isCollapsed).toBe(true)
  })

  it('rehydrates state written to storage by another tab/session', async () => {
    // Simulate a write that didn't go through this store instance's `setState`
    // (which would otherwise immediately re-persist and mask the read-back).
    window.localStorage.setItem('sidebar-ui', JSON.stringify({ state: { isCollapsed: true }, version: 0 }))

    await useSidebarStore.persist.rehydrate()

    expect(useSidebarStore.getState().isCollapsed).toBe(true)
  })
})
