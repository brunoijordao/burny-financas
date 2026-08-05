import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/formatters'
import * as planningApi from '@/features/planning/api/planningApi'
import type { PlanningEntry } from '@/features/planning/api/planningApi'
import { buildCalendarGrid } from '@/features/planning/lib/calendarGrid'
import { getPlanningUrgency } from '@/features/planning/lib/urgency'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

const WEEKDAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
const MAX_VISIBLE_PER_DAY = 3

function monthLabel(month: string) {
  const [year, monthNum] = month.split('-').map(Number)
  return new Date(year, monthNum - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
}

function shiftMonth(month: string, delta: number) {
  const [year, monthNum] = month.split('-').map(Number)
  const shifted = new Date(year, monthNum - 1 + delta, 1)
  return `${shifted.getFullYear()}-${String(shifted.getMonth() + 1).padStart(2, '0')}`
}

function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function EntryChip({ entry }: { entry: PlanningEntry }) {
  const currency = usePreferencesStore((state) => state.currency)
  const urgency = getPlanningUrgency(entry)
  return (
    <div
      className={cn(
        'truncate rounded px-1.5 py-0.5 text-[11px] leading-tight',
        entry.type === 'PAYABLE' ? 'bg-destructive/15 text-destructive' : 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400',
        urgency === 'overdue' && 'ring-1 ring-destructive',
      )}
      title={`${entry.description} · ${formatCurrency(entry.amount, currency)}`}
    >
      {entry.type === 'PAYABLE' ? '−' : '+'} {entry.description}
    </div>
  )
}

interface PlanningCalendarProps {
  /** Bump this from the parent after a mutation (create/edit/settle/delete) to force a refetch. */
  refreshSignal?: number
}

/** Owns its month state; fetches GET /planning-entries/calendar on month change or when refreshSignal changes. */
export function PlanningCalendar({ refreshSignal }: PlanningCalendarProps) {
  const [month, setMonth] = useState(currentMonth())
  const [entries, setEntries] = useState<PlanningEntry[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    planningApi
      .fetchPlanningCalendar(month)
      .then((result) => {
        if (!cancelled) {
          setEntries(result)
          setError(null)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Não foi possível carregar o calendário deste mês.')
        }
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [month, refreshSignal])

  const days = buildCalendarGrid(month, entries)
  const weeks: (typeof days)[] = []
  for (let i = 0; i < days.length; i += 7) {
    weeks.push(days.slice(i, i + 7))
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <Button variant="outline" size="sm" onClick={() => setMonth((current) => shiftMonth(current, -1))} aria-label="Mês anterior">
          <ChevronLeft className="size-4" />
        </Button>
        <span className="text-sm font-medium capitalize">{monthLabel(month)}</span>
        <Button variant="outline" size="sm" onClick={() => setMonth((current) => shiftMonth(current, 1))} aria-label="Próximo mês">
          <ChevronRight className="size-4" />
        </Button>
      </div>

      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="grid grid-cols-7 gap-1 text-center text-xs text-muted-foreground">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>

      <div className="flex flex-col gap-1">
        {weeks.map((week, weekIndex) => (
          // eslint-disable-next-line react/no-array-index-key
          <div key={weekIndex} className="grid grid-cols-7 gap-1">
            {week.map((day, dayIndex) => (
              <div
                key={day.date ?? `blank-${weekIndex}-${dayIndex}`}
                className={cn(
                  'flex min-h-20 flex-col gap-1 rounded-md border border-border p-1',
                  day.date === null && 'border-transparent',
                  day.isToday && 'border-primary bg-primary/5',
                )}
              >
                {day.dayOfMonth !== null ? (
                  <>
                    <span className={cn('text-xs', day.isToday ? 'font-semibold text-primary' : 'text-muted-foreground')}>
                      {day.dayOfMonth}
                    </span>
                    <div className="flex flex-col gap-0.5">
                      {day.entries.slice(0, MAX_VISIBLE_PER_DAY).map((entry) => (
                        <EntryChip key={entry.id} entry={entry} />
                      ))}
                      {day.entries.length > MAX_VISIBLE_PER_DAY ? (
                        <span className="text-[11px] text-muted-foreground">
                          +{day.entries.length - MAX_VISIBLE_PER_DAY} mais
                        </span>
                      ) : null}
                    </div>
                  </>
                ) : null}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
