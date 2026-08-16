import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { formatCurrency, formatDate } from '@/lib/formatters'
import type { PlanningEntry } from '@/features/planning/api/planningApi'
import { PlanningStatusBadge } from '@/features/planning/components/PlanningStatusBadge'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface PlanningEntryListProps {
  entries: PlanningEntry[]
  onEdit: (entry: PlanningEntry) => void
  onDelete: (entry: PlanningEntry) => void
  onSettle: (entry: PlanningEntry) => void
  onUndoSettlement: (entry: PlanningEntry) => void
}

export function PlanningEntryList({ entries, onEdit, onDelete, onSettle, onUndoSettlement }: PlanningEntryListProps) {
  const currency = usePreferencesStore((state) => state.currency)
  const dateFormat = usePreferencesStore((state) => state.dateFormat)

  if (entries.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum lançamento cadastrado ainda.</p>
  }

  return (
    <div className="flex flex-col gap-3">
      {entries.map((entry) => (
        <Card key={entry.id}>
          <CardContent className="flex flex-wrap items-center justify-between gap-3 pt-6">
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-2">
                <span className="font-medium">{entry.description}</span>
                <PlanningStatusBadge entry={entry} />
              </div>
              <span className="text-xs text-muted-foreground">
                Vence em {formatDate(entry.dueDate, dateFormat)} · {entry.accountName}
                {entry.categoryName ? ` · ${entry.categoryName}` : ''}
              </span>
            </div>

            <div className="flex items-center gap-3">
              <span
                className={
                  entry.type === 'PAYABLE'
                    ? 'font-semibold text-destructive'
                    : 'font-semibold text-foreground'
                }
              >
                {entry.type === 'PAYABLE' ? '-' : '+'}
                {formatCurrency(entry.amount, currency)}
              </span>
              <div className="flex gap-2">
                {entry.status === 'SETTLED' ? (
                  <Button variant="outline" size="sm" onClick={() => onUndoSettlement(entry)}>
                    Desfazer baixa
                  </Button>
                ) : (
                  <Button size="sm" onClick={() => onSettle(entry)}>
                    {entry.type === 'PAYABLE' ? 'Marcar como pago' : 'Marcar como recebido'}
                  </Button>
                )}
                <Button variant="outline" size="sm" onClick={() => onEdit(entry)}>
                  Editar
                </Button>
                <Button variant="ghost" size="sm" onClick={() => onDelete(entry)}>
                  Excluir
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
