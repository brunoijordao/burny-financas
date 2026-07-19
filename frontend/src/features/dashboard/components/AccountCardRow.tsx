import type { Account } from '@/features/accounts/api/accountsApi'
import { AccountIcon } from '@/features/accounts/icons'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface AccountCardRowProps {
  accounts: Account[]
}

/** Horizontal-scroll row of per-account cards, reusing each account's own icon/color (design.md Decision 4). */
export function AccountCardRow({ accounts }: AccountCardRowProps) {
  const activeAccounts = accounts.filter((account) => account.active)

  if (activeAccounts.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma conta cadastrada ainda.</p>
  }

  return (
    <div className="flex gap-3 overflow-x-auto pb-1">
      {activeAccounts.map((account) => {
        const isCreditCard = account.type === 'CREDIT_CARD'
        return (
          <div
            key={account.id}
            className="flex min-w-52 shrink-0 flex-col gap-2 rounded-lg border border-border bg-card p-4"
          >
            <div className="flex items-center gap-2">
              <span
                className="flex size-8 shrink-0 items-center justify-center rounded-full"
                style={{ backgroundColor: account.color + '33', color: account.color }}
              >
                <AccountIcon icon={account.icon} className="size-4" />
              </span>
              <span className="truncate text-sm font-medium">{account.name}</span>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">{isCreditCard ? 'Fatura atual' : 'Saldo'}</p>
              <p className="text-lg font-semibold">
                {formatCurrency((isCreditCard ? account.currentInvoice : account.balance) ?? 0)}
              </p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
