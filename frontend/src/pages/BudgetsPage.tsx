import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as budgetsApi from '@/features/budgets/api/budgetsApi'
import type { Budget } from '@/features/budgets/api/budgetsApi'
import { BudgetList } from '@/features/budgets/components/BudgetList'
import { SetBudgetForm } from '@/features/budgets/components/SetBudgetForm'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'

type Panel = 'none' | 'create' | { edit: Budget }

export function BudgetsPage() {
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

  const reload = useCallback(async () => {
    try {
      const [budgetList, categoryList] = await Promise.all([budgetsApi.fetchBudgets(), categoriesApi.fetchCategories()])
      setBudgets(budgetList)
      setCategories(categoryList)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar seus orçamentos. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const handleDelete = async (budget: Budget) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir o orçamento de "${budget.categoryName}"?`)
    if (!confirmed) {
      return
    }
    await budgetsApi.deleteBudget(budget.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Orçamentos do mês</h1>
        <p className="text-muted-foreground">Defina um limite de gasto por categoria e acompanhe o progresso.</p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Novo orçamento</Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Novo orçamento</CardTitle>
            <CardDescription>Escolha uma categoria e defina o limite de gasto para o mês corrente.</CardDescription>
          </CardHeader>
          <CardContent>
            <SetBudgetForm
              categories={categories}
              onCancel={() => setPanel('none')}
              onSubmit={async (categoryId, limitAmount) => {
                await budgetsApi.setBudget(categoryId, limitAmount)
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
            <CardTitle>Editar orçamento de {panel.edit.categoryName}</CardTitle>
          </CardHeader>
          <CardContent>
            <SetBudgetForm
              categories={categories}
              lockCategory
              initialCategoryId={panel.edit.categoryId}
              initialLimitAmount={panel.edit.limitAmount ?? undefined}
              onCancel={() => setPanel('none')}
              onSubmit={async (categoryId, limitAmount) => {
                await budgetsApi.setBudget(categoryId, limitAmount)
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      <BudgetList budgets={budgets} onEdit={(budget) => setPanel({ edit: budget })} onDelete={handleDelete} />
    </div>
  )
}
