import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import { transactionTypeLabels, transactionTypes, type TransactionTypeValue } from '@/features/transactions/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export interface TransactionFiltersValue {
  accountId: string
  categoryId: string
  type: TransactionTypeValue | ''
  startDate: string
  endDate: string
}

interface TransactionFiltersBarProps {
  accounts: Account[]
  categories: Category[]
  value: TransactionFiltersValue
  onChange: (value: TransactionFiltersValue) => void
}

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

export function TransactionFiltersBar({ accounts, categories, value, onChange }: TransactionFiltersBarProps) {
  const flatCategories = flattenCategories(categories)

  const update = (patch: Partial<TransactionFiltersValue>) => onChange({ ...value, ...patch })

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="filter-account">Conta</Label>
        <select
          id="filter-account"
          className={selectClassName}
          value={value.accountId}
          onChange={(event) => update({ accountId: event.target.value })}
        >
          <option value="">Todas</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="filter-category">Categoria</Label>
        <select
          id="filter-category"
          className={selectClassName}
          value={value.categoryId}
          onChange={(event) => update({ categoryId: event.target.value })}
        >
          <option value="">Todas</option>
          {flatCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="filter-type">Tipo</Label>
        <select
          id="filter-type"
          className={selectClassName}
          value={value.type}
          onChange={(event) => update({ type: event.target.value as TransactionTypeValue | '' })}
        >
          <option value="">Todos</option>
          {transactionTypes.map((type) => (
            <option key={type} value={type}>
              {transactionTypeLabels[type]}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="filter-start">De</Label>
        <Input
          id="filter-start"
          type="date"
          value={value.startDate}
          onChange={(event) => update({ startDate: event.target.value })}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="filter-end">Até</Label>
        <Input
          id="filter-end"
          type="date"
          value={value.endDate}
          onChange={(event) => update({ endDate: event.target.value })}
        />
      </div>

      <div className="flex items-end sm:col-span-2 lg:col-span-5">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange({ accountId: '', categoryId: '', type: '', startDate: '', endDate: '' })}
        >
          Limpar filtros
        </Button>
      </div>
    </div>
  )
}
