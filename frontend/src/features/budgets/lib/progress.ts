export interface BudgetProgress {
  percent: number
  /** Bar fill width, capped at 100 even when spend exceeds the limit. */
  barWidth: number
  overLimit: boolean
}

/** Percent-of-limit and the at/over-100% threshold used to color the progress bar (design.md Decision 5). */
export function computeBudgetProgress(limitAmount: number | null, spentAmount: number): BudgetProgress {
  if (limitAmount === null || limitAmount <= 0) {
    return { percent: 0, barWidth: 0, overLimit: false }
  }

  const percent = (spentAmount / limitAmount) * 100
  return {
    percent,
    barWidth: Math.min(percent, 100),
    overLimit: percent >= 100,
  }
}
