import { describe, expect, it } from 'vitest'

import { buildCalendarGrid } from './calendarGrid'
import type { PlanningEntry } from '@/features/planning/api/planningApi'

function entry(dueDate: string): PlanningEntry {
  return {
    id: 1,
    type: 'PAYABLE',
    accountId: 1,
    accountName: 'Conta',
    categoryId: null,
    categoryName: null,
    amount: 100,
    description: 'Conta de luz',
    dueDate,
    status: 'PENDING',
    settledAt: null,
    transactionId: null,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
  }
}

describe('buildCalendarGrid', () => {
  it('produces a grid padded to full weeks with the right day count for the month', () => {
    const days = buildCalendarGrid('2026-02', [])
    expect(days.length % 7).toBe(0)
    const realDays = days.filter((day) => day.dayOfMonth !== null)
    expect(realDays).toHaveLength(28)
    expect(realDays[0]?.dayOfMonth).toBe(1)
    expect(realDays.at(-1)?.dayOfMonth).toBe(28)
  })

  it('attaches entries to the matching day', () => {
    const days = buildCalendarGrid('2026-02', [entry('2026-02-15'), entry('2026-02-15'), entry('2026-02-20')])
    const day15 = days.find((day) => day.date === '2026-02-15')
    const day20 = days.find((day) => day.date === '2026-02-20')
    expect(day15?.entries).toHaveLength(2)
    expect(day20?.entries).toHaveLength(1)
  })

  it('marks the current date as today', () => {
    const today = new Date(2026, 1, 10)
    const days = buildCalendarGrid('2026-02', [], today)
    const day10 = days.find((day) => day.date === '2026-02-10')
    expect(day10?.isToday).toBe(true)
  })

  it('does not attach entries from other months', () => {
    const days = buildCalendarGrid('2026-02', [entry('2026-03-01')])
    const allEntries = days.flatMap((day) => day.entries)
    expect(allEntries).toHaveLength(0)
  })
})
