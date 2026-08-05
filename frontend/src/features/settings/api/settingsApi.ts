import { httpClient } from '@/lib/httpClient'

export type CurrencyCode = 'BRL' | 'USD' | 'EUR'
export type DateFormatCode = 'DD/MM/YYYY' | 'MM/DD/YYYY' | 'YYYY-MM-DD'

export interface UserPreferences {
  currency: CurrencyCode
  dateFormat: DateFormatCode
}

/** GET /settings/preferences -> the caller's saved preferences, or the defaults (BRL / DD/MM/YYYY) if never saved */
export async function fetchPreferences(signal?: AbortSignal): Promise<UserPreferences> {
  const response = await httpClient.get<UserPreferences>('/settings/preferences', { signal })
  return response.data
}

/** PUT /settings/preferences, upserted server-side */
export async function updatePreferences(payload: UserPreferences): Promise<UserPreferences> {
  const response = await httpClient.put<UserPreferences>('/settings/preferences', payload)
  return response.data
}
