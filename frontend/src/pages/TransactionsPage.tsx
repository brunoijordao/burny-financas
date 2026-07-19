import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import { AttachmentManager } from '@/features/transactions/components/AttachmentManager'
import { CreateTransactionForm } from '@/features/transactions/components/CreateTransactionForm'
import { EditTransactionForm } from '@/features/transactions/components/EditTransactionForm'
import { TransactionFiltersBar, type TransactionFiltersValue } from '@/features/transactions/components/TransactionFiltersBar'
import { TransactionList } from '@/features/transactions/components/TransactionList'
import * as transactionsApi from '@/features/transactions/api/transactionsApi'
import type { PagedResult, Transaction } from '@/features/transactions/api/transactionsApi'

type Panel = 'none' | 'create' | { edit: Transaction } | { attachments: Transaction }

const EMPTY_FILTERS: TransactionFiltersValue = {
  accountId: '',
  categoryId: '',
  type: '',
  startDate: '',
  endDate: '',
}

const EMPTY_PAGE: PagedResult<Transaction> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function TransactionsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [page, setPage] = useState<PagedResult<Transaction>>(EMPTY_PAGE)
  const [filters, setFilters] = useState<TransactionFiltersValue>(EMPTY_FILTERS)
  const [pageNumber, setPageNumber] = useState(0)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

  const reloadLookups = useCallback(async () => {
    const [accountList, categoryList] = await Promise.all([accountsApi.fetchAccounts(), categoriesApi.fetchCategories()])
    setAccounts(accountList)
    setCategories(categoryList)
  }, [])

  const reloadTransactions = useCallback(async () => {
    try {
      const result = await transactionsApi.fetchTransactions({
        accountId: filters.accountId ? Number(filters.accountId) : undefined,
        categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
        type: filters.type || undefined,
        startDate: filters.startDate || undefined,
        endDate: filters.endDate || undefined,
        page: pageNumber,
      })
      setPage(result)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar suas transações. Tente novamente em instantes.')
    }
  }, [filters, pageNumber])

  useEffect(() => {
    void reloadLookups()
  }, [reloadLookups])

  useEffect(() => {
    void reloadTransactions()
  }, [reloadTransactions])

  const handleFiltersChange = (value: TransactionFiltersValue) => {
    setFilters(value)
    setPageNumber(0)
  }

  const handleDelete = async (transaction: Transaction) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir "${transaction.description}"?`)
    if (!confirmed) {
      return
    }
    await transactionsApi.deleteTransaction(transaction.id)
    await reloadTransactions()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Suas transações</h1>
        <p className="text-muted-foreground">Receitas e despesas lançadas nas suas contas.</p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')} disabled={accounts.length === 0}>
          Nova transação
        </Button>
      </div>

      {accounts.length === 0 ? (
        <p className="text-sm text-muted-foreground">Cadastre uma conta antes de lançar transações.</p>
      ) : null}

      <TransactionFiltersBar accounts={accounts} categories={categories} value={filters} onChange={handleFiltersChange} />

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Nova transação</CardTitle>
            <CardDescription>Registre uma receita ou despesa.</CardDescription>
          </CardHeader>
          <CardContent>
            <CreateTransactionForm
              accounts={accounts}
              categories={categories}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await transactionsApi.createTransaction({
                  description: values.description,
                  amount: Number(values.amount),
                  type: values.type,
                  transactionDate: values.transactionDate,
                  accountId: Number(values.accountId),
                  categoryId: values.categoryId ? Number(values.categoryId) : undefined,
                  note: values.note || undefined,
                  recurring: values.recurring,
                  frequency: values.recurring ? values.frequency : undefined,
                  startDate: values.recurring ? values.startDate : undefined,
                  endDate: values.recurring && values.endDate ? values.endDate : undefined,
                })
                setPanel('none')
                await reloadTransactions()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'edit' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Editar transação</CardTitle>
          </CardHeader>
          <CardContent>
            <EditTransactionForm
              transaction={panel.edit}
              accounts={accounts}
              categories={categories}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await transactionsApi.updateTransaction(panel.edit.id, {
                  description: values.description,
                  amount: Number(values.amount),
                  type: values.type,
                  transactionDate: values.transactionDate,
                  accountId: Number(values.accountId),
                  categoryId: values.categoryId ? Number(values.categoryId) : undefined,
                  note: values.note || undefined,
                })
                setPanel('none')
                await reloadTransactions()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'attachments' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Anexos de {panel.attachments.description}</CardTitle>
            <CardDescription>Comprovantes e notas fiscais desta transação.</CardDescription>
          </CardHeader>
          <CardContent>
            <AttachmentManager transaction={panel.attachments} onClose={() => setPanel('none')} />
          </CardContent>
        </Card>
      ) : null}

      <TransactionList
        page={page}
        accounts={accounts}
        categories={categories}
        onEdit={(transaction) => setPanel({ edit: transaction })}
        onDelete={handleDelete}
        onManageAttachments={(transaction) => setPanel({ attachments: transaction })}
        onPageChange={setPageNumber}
      />
    </div>
  )
}
