import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import type { PagedResult, Transaction } from '@/features/transactions/api/transactionsApi'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function flattenCategories(categories: Category[]): Map<number, string> {
  const map = new Map<number, string>()
  categories.forEach((category) => {
    map.set(category.id, category.name)
    category.subcategories.forEach((sub) => map.set(sub.id, sub.name))
  })
  return map
}

interface TransactionListProps {
  page: PagedResult<Transaction>
  accounts: Account[]
  categories: Category[]
  onEdit: (transaction: Transaction) => void
  onDelete: (transaction: Transaction) => void
  onManageAttachments: (transaction: Transaction) => void
  onPageChange: (page: number) => void
}

export function TransactionList({
  page,
  accounts,
  categories,
  onEdit,
  onDelete,
  onManageAttachments,
  onPageChange,
}: TransactionListProps) {
  const accountNames = new Map(accounts.map((account) => [account.id, account.name]))
  const categoryNames = flattenCategories(categories)

  if (page.content.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma transação encontrada.</p>
  }

  return (
    <div className="flex flex-col gap-3">
      {page.content.map((transaction) => (
        <Card key={transaction.id}>
          <CardContent className="flex flex-wrap items-center justify-between gap-3 py-4">
            <div className="flex flex-col gap-0.5">
              <span className="font-medium">{transaction.description}</span>
              <span className="text-xs text-muted-foreground">
                {transaction.transactionDate} · {accountNames.get(transaction.accountId) ?? 'Conta removida'}
                {transaction.categoryId != null ? ` · ${categoryNames.get(transaction.categoryId) ?? 'Categoria'}` : ''}
                {transaction.recurrenceId != null ? ' · Recorrente' : ''}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <span
                className={
                  transaction.type === 'EXPENSE'
                    ? 'font-semibold text-destructive'
                    : 'font-semibold text-emerald-600 dark:text-emerald-400'
                }
              >
                {transaction.type === 'EXPENSE' ? '-' : '+'}
                {formatCurrency(transaction.amount)}
              </span>
              <div className="flex gap-1">
                <Button variant="outline" size="sm" onClick={() => onManageAttachments(transaction)}>
                  Anexos
                </Button>
                <Button variant="outline" size="sm" onClick={() => onEdit(transaction)}>
                  Editar
                </Button>
                <Button variant="ghost" size="sm" onClick={() => onDelete(transaction)}>
                  Excluir
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ))}

      {page.totalPages > 1 ? (
        <div className="flex items-center justify-between pt-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page.page === 0}
            onClick={() => onPageChange(page.page - 1)}
          >
            Anterior
          </Button>
          <span className="text-sm text-muted-foreground">
            Página {page.page + 1} de {page.totalPages}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page.page + 1 >= page.totalPages}
            onClick={() => onPageChange(page.page + 1)}
          >
            Próxima
          </Button>
        </div>
      ) : null}
    </div>
  )
}
