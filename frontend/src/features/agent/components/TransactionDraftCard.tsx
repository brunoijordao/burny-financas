import { useState } from 'react'
import { Check, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import type { TransactionDraft } from '@/features/agent/api/agentApi'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDate(value: string) {
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

interface TransactionDraftCardProps {
  draft: TransactionDraft
  onConfirm: () => Promise<void>
  onDecline: () => void
}

/**
 * The one moment in this screen where the assistant is asking for a decision, not just answering —
 * given its own visual weight (accent border, its own card) rather than blending into the message
 * flow, so "this requires your action" reads at a glance.
 */
export function TransactionDraftCard({ draft, onConfirm, onDecline }: TransactionDraftCardProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resolved, setResolved] = useState<'confirmed' | 'declined' | null>(null)

  const isExpense = draft.type === 'EXPENSE'

  const handleConfirm = async () => {
    setBusy(true)
    setError(null)
    try {
      await onConfirm()
      setResolved('confirmed')
    } catch {
      setError('Não foi possível registrar essa transação. Tente novamente.')
    } finally {
      setBusy(false)
    }
  }

  const handleDecline = () => {
    setResolved('declined')
    onDecline()
  }

  if (resolved) {
    return (
      <div className="ml-9 rounded-xl border border-border bg-card px-4 py-3 text-sm text-muted-foreground">
        {resolved === 'confirmed' ? 'Transação registrada.' : 'Sugestão descartada.'}
      </div>
    )
  }

  return (
    <div className="ml-9 overflow-hidden rounded-xl border border-border bg-card shadow-sm">
      <div className="flex items-stretch">
        <div className={isExpense ? 'w-1 shrink-0 bg-destructive' : 'w-1 shrink-0 bg-emerald-500'} aria-hidden="true" />
        <div className="flex-1 p-4">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            Confirmar {isExpense ? 'despesa' : 'receita'}
          </p>
          <div className="mt-1.5 flex items-baseline justify-between gap-3">
            <span className="font-medium text-foreground">{draft.description}</span>
            <span className={isExpense ? 'font-semibold text-destructive' : 'font-semibold text-emerald-600 dark:text-emerald-400'}>
              {isExpense ? '-' : '+'}
              {formatCurrency(draft.amount)}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {draft.accountName}
            {draft.categoryName ? ` · ${draft.categoryName}` : ' · Sem categoria'} · {formatDate(draft.date)}
          </p>

          {error ? <p className="mt-2 text-sm text-destructive">{error}</p> : null}

          <div className="mt-3 flex gap-2">
            <Button type="button" size="sm" disabled={busy} onClick={() => void handleConfirm()}>
              <Check className="size-4" />
              {busy ? 'Registrando...' : 'Confirmar'}
            </Button>
            <Button type="button" variant="outline" size="sm" disabled={busy} onClick={handleDecline}>
              <X className="size-4" />
              Descartar
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
