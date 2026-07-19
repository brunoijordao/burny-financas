import { describe, expect, it } from 'vitest'

import { computeBudgetProgress } from './progress'

describe('computeBudgetProgress', () => {
  it('computes percent under the limit as not over', () => {
    const progress = computeBudgetProgress(800, 400)
    expect(progress.percent).toBe(50)
    expect(progress.barWidth).toBe(50)
    expect(progress.overLimit).toBe(false)
  })

  it('flags exactly 100% as over limit', () => {
    const progress = computeBudgetProgress(800, 800)
    expect(progress.percent).toBe(100)
    expect(progress.overLimit).toBe(true)
  })

  it('flags spend beyond the limit as over limit and caps the bar width at 100', () => {
    const progress = computeBudgetProgress(800, 1200)
    expect(progress.percent).toBe(150)
    expect(progress.barWidth).toBe(100)
    expect(progress.overLimit).toBe(true)
  })

  it('returns zero progress when there is no limit set', () => {
    const progress = computeBudgetProgress(null, 400)
    expect(progress.percent).toBe(0)
    expect(progress.overLimit).toBe(false)
  })
})
