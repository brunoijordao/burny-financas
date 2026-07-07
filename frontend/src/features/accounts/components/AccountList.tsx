import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Account } from '@/features/accounts/api/accountsApi'
import { AccountIcon } from '@/features/accounts/icons'
import { accountTypeLabels } from '@/features/accounts/schemas'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface AccountListProps {
  accounts: Account[]
  onEdit: (account: Account) => void
  onDelete: (account: Account) => void
}

export function AccountList({ accounts, onEdit, onDelete }: AccountListProps) {
  if (accounts.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma conta cadastrada ainda.</p>
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {accounts.map((account) => {
        const isCreditCard = account.type === 'CREDIT_CARD'
        return (
          <Card key={account.id}>
            <CardHeader className="flex flex-row items-center gap-3 space-y-0">
              <span
                className="flex size-9 shrink-0 items-center justify-center rounded-full"
                style={{ backgroundColor: account.color + '33', color: account.color }}
              >
                <AccountIcon icon={account.icon} className="size-5" />
              </span>
              <div className="flex flex-col">
                <CardTitle>{account.name}</CardTitle>
                <span className="text-xs text-muted-foreground">{accountTypeLabels[account.type]}</span>
              </div>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              {isCreditCard ? (
                <div className="flex flex-col gap-0.5">
                  <span className="text-xs text-muted-foreground">Fatura atual</span>
                  <span className="text-lg font-semibold">{formatCurrency(account.currentInvoice ?? 0)}</span>
                  <span className="text-xs text-muted-foreground">
                    Limite: {formatCurrency(account.creditLimit ?? 0)}
                  </span>
                </div>
              ) : (
                <div className="flex flex-col gap-0.5">
                  <span className="text-xs text-muted-foreground">Saldo</span>
                  <span className="text-lg font-semibold">{formatCurrency(account.balance ?? 0)}</span>
                </div>
              )}

              <div className="flex justify-end gap-2">
                <Button variant="outline" size="sm" onClick={() => onEdit(account)}>
                  Editar
                </Button>
                <Button variant="ghost" size="sm" onClick={() => onDelete(account)}>
                  Excluir
                </Button>
              </div>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
