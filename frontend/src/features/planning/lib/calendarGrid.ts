import type { PlanningEntry } from '@/features/planning/api/planningApi'

export interface CalendarDay {
  /** yyyy-MM-dd, null for leading/trailing padding cells outside the month */
  date: string | null
  dayOfMonth: number | null
  entries: PlanningEntry[]
  isToday: boolean
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

/** Splits a `yyyy-MM` month plus its entries into a 7-column grid, padded to full weeks. */
export function buildCalendarGrid(month: string, entries: PlanningEntry[], today = new Date()): CalendarDay[] {
  const [year, monthNum] = month.split('-').map(Number)
  const daysInMonth = new Date(year, monthNum, 0).getDate()
  const leadingBlanks = new Date(year, monthNum - 1, 1).getDay()
  const todayIso = today.toISOString().slice(0, 10)

  const entriesByDay = new Map<string, PlanningEntry[]>()
  entries.forEach((entry) => {
    const list = entriesByDay.get(entry.dueDate) ?? []
    list.push(entry)
    entriesByDay.set(entry.dueDate, list)
  })

  const days: CalendarDay[] = []
  for (let i = 0; i < leadingBlanks; i++) {
    days.push({ date: null, dayOfMonth: null, entries: [], isToday: false })
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const iso = `${year}-${pad(monthNum)}-${pad(day)}`
    days.push({ date: iso, dayOfMonth: day, entries: entriesByDay.get(iso) ?? [], isToday: iso === todayIso })
  }
  while (days.length % 7 !== 0) {
    days.push({ date: null, dayOfMonth: null, entries: [], isToday: false })
  }
  return days
}
