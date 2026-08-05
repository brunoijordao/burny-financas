import MockAdapter from 'axios-mock-adapter'
import { afterEach, describe, expect, it } from 'vitest'

import { httpClient } from '@/lib/httpClient'
import { DEFAULT_PREFERENCES, usePreferencesStore } from './preferencesStore'

const mockHttp = new MockAdapter(httpClient)

describe('usePreferencesStore', () => {
  afterEach(() => {
    mockHttp.reset()
    usePreferencesStore.setState(DEFAULT_PREFERENCES)
  })

  it('starts with the default currency and date format', () => {
    expect(usePreferencesStore.getState()).toMatchObject(DEFAULT_PREFERENCES)
  })

  it('loadPreferences applies the fetched values on success', async () => {
    mockHttp.onGet('/settings/preferences').reply(200, { currency: 'USD', dateFormat: 'MM/DD/YYYY' })

    await usePreferencesStore.getState().loadPreferences()

    expect(usePreferencesStore.getState()).toMatchObject({ currency: 'USD', dateFormat: 'MM/DD/YYYY' })
  })

  it('loadPreferences keeps the defaults when the fetch fails', async () => {
    mockHttp.onGet('/settings/preferences').networkError()

    await usePreferencesStore.getState().loadPreferences()

    expect(usePreferencesStore.getState()).toMatchObject(DEFAULT_PREFERENCES)
  })

  it('loadPreferences does not reset to defaults when the request was cancelled', async () => {
    // Simulates React StrictMode's dev-only double-invoke: the first (stale) call is aborted, and
    // must not stomp on whatever a superseding call already set — see preferencesStore.ts.
    usePreferencesStore.getState().setPreferences({ currency: 'EUR', dateFormat: 'YYYY-MM-DD' })
    mockHttp.onGet('/settings/preferences').reply(200, { currency: 'USD', dateFormat: 'MM/DD/YYYY' })

    const controller = new AbortController()
    controller.abort()
    await usePreferencesStore.getState().loadPreferences(controller.signal)

    expect(usePreferencesStore.getState()).toMatchObject({ currency: 'EUR', dateFormat: 'YYYY-MM-DD' })
  })

  it('setPreferences updates the state directly', () => {
    usePreferencesStore.getState().setPreferences({ currency: 'EUR', dateFormat: 'YYYY-MM-DD' })

    expect(usePreferencesStore.getState()).toMatchObject({ currency: 'EUR', dateFormat: 'YYYY-MM-DD' })
  })
})
