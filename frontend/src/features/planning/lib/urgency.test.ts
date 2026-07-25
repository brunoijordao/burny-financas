import { describe, expect, it } from 'vitest'

import { getPlanningUrgency } from './urgency'

function isoDateOffset(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

describe('getPlanningUrgency', () => {
  it('flags a settled entry as settled regardless of due date', () => {
    expect(getPlanningUrgency({ status: 'SETTLED', dueDate: isoDateOffset(-30) })).toBe('settled')
  })

  it('flags an overdue entry as overdue', () => {
    expect(getPlanningUrgency({ status: 'OVERDUE', dueDate: isoDateOffset(-1) })).toBe('overdue')
  })

  it('flags a pending entry due within the window as near-due', () => {
    expect(getPlanningUrgency({ status: 'PENDING', dueDate: isoDateOffset(2) })).toBe('near-due')
  })

  it('flags a pending entry due today as near-due', () => {
    expect(getPlanningUrgency({ status: 'PENDING', dueDate: isoDateOffset(0) })).toBe('near-due')
  })

  it('flags a pending entry due beyond the window as normal', () => {
    expect(getPlanningUrgency({ status: 'PENDING', dueDate: isoDateOffset(10) })).toBe('normal')
  })

  it('respects a custom near-due window', () => {
    expect(getPlanningUrgency({ status: 'PENDING', dueDate: isoDateOffset(5) }, 7)).toBe('near-due')
  })
})
