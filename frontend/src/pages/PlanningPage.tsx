import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import * as planningApi from '@/features/planning/api/planningApi'
import type { PlanningEntry } from '@/features/planning/api/planningApi'
import { PlanningCalendar } from '@/features/planning/components/PlanningCalendar'
import { PlanningEntryForm } from '@/features/planning/components/PlanningEntryForm'
import { PlanningEntryList } from '@/features/planning/components/PlanningEntryList'
import { ProjectedCashFlowSection } from '@/features/planning/components/ProjectedCashFlowSection'

type Panel = 'none' | 'create' | { edit: PlanningEntry }

export function PlanningPage() {
  const [entries, setEntries] = useState<PlanningEntry[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')
  // Bumped after every mutation so the calendar/cash-flow sections (which fetch independently) refetch too.
  const [refreshSignal, setRefreshSignal] = useState(0)

  const reload = useCallback(async () => {
    try {
      const [entryList, accountList, categoryList] = await Promise.all([
        planningApi.fetchPlanningEntries(),
        accountsApi.fetchAccounts(),
        categoriesApi.fetchCategories(),
      ])
      setEntries(entryList)
      setAccounts(accountList)
      setCategories(categoryList)
      setLoadError(null)
      setRefreshSignal((current) => current + 1)
    } catch {
      setLoadError('Não foi possível carregar o planejamento. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const handleDelete = async (entry: PlanningEntry) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir "${entry.description}"?`)
    if (!confirmed) {
      return
    }
    await planningApi.deletePlanningEntry(entry.id)
    await reload()
  }

  const handleSettle = async (entry: PlanningEntry) => {
    const label = entry.type === 'PAYABLE' ? 'pago' : 'recebido'
    const confirmed = window.confirm(`Marcar "${entry.description}" como ${label}? Isso vai gerar uma transação real.`)
    if (!confirmed) {
      return
    }
    await planningApi.settlePlanningEntry(entry.id)
    await reload()
  }

  const handleUndoSettlement = async (entry: PlanningEntry) => {
    const confirmed = window.confirm(`Desfazer a baixa de "${entry.description}"? A transação vinculada será removida.`)
    if (!confirmed) {
      return
    }
    await planningApi.undoPlanningEntrySettlement(entry.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Planejamento financeiro</h1>
        <p className="text-muted-foreground">Contas a pagar e a receber, calendário de vencimentos e fluxo de caixa projetado.</p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Novo lançamento</Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Novo lançamento</CardTitle>
            <CardDescription>Registre uma conta a pagar ou a receber. Ela só afeta o saldo quando for baixada.</CardDescription>
          </CardHeader>
          <CardContent>
            <PlanningEntryForm
              accounts={accounts}
              categories={categories}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await planningApi.createPlanningEntry({
                  type: values.type,
                  accountId: Number(values.accountId),
                  categoryId: values.categoryId ? Number(values.categoryId) : undefined,
                  amount: Number(values.amount),
                  description: values.description,
                  dueDate: values.dueDate,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'edit' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Editar lançamento</CardTitle>
          </CardHeader>
          <CardContent>
            <PlanningEntryForm
              accounts={accounts}
              categories={categories}
              initialValues={{
                type: panel.edit.type,
                accountId: String(panel.edit.accountId),
                categoryId: panel.edit.categoryId ? String(panel.edit.categoryId) : '',
                amount: String(panel.edit.amount),
                description: panel.edit.description,
                dueDate: panel.edit.dueDate,
              }}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await planningApi.updatePlanningEntry(panel.edit.id, {
                  type: values.type,
                  accountId: Number(values.accountId),
                  categoryId: values.categoryId ? Number(values.categoryId) : undefined,
                  amount: Number(values.amount),
                  description: values.description,
                  dueDate: values.dueDate,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      <PlanningEntryList
        entries={entries}
        onEdit={(entry) => setPanel({ edit: entry })}
        onDelete={handleDelete}
        onSettle={handleSettle}
        onUndoSettlement={handleUndoSettlement}
      />

      <Card>
        <CardHeader>
          <CardTitle>Calendário de vencimentos</CardTitle>
          <CardDescription>Contas a pagar e a receber distribuídas pelos dias do mês.</CardDescription>
        </CardHeader>
        <CardContent>
          <PlanningCalendar refreshSignal={refreshSignal} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Fluxo de caixa projetado</CardTitle>
          <CardDescription>
            Saldo atual mais os lançamentos ainda pendentes — diferente da projeção do mês atual mostrada no dashboard.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ProjectedCashFlowSection refreshSignal={refreshSignal} />
        </CardContent>
      </Card>
    </div>
  )
}
