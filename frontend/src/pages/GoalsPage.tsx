import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as goalsApi from '@/features/goals/api/goalsApi'
import type { Goal } from '@/features/goals/api/goalsApi'
import { AddContributionForm } from '@/features/goals/components/AddContributionForm'
import { CreateGoalForm } from '@/features/goals/components/CreateGoalForm'
import { GoalHistorySection } from '@/features/goals/components/GoalHistorySection'

type Panel = 'none' | 'create' | { edit: Goal } | { contribute: Goal }

export function GoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

  const reload = useCallback(async () => {
    try {
      const goalList = await goalsApi.fetchGoals()
      setGoals(goalList)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar suas metas. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const handleDelete = async (goal: Goal) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir a meta "${goal.name}"?`)
    if (!confirmed) {
      return
    }
    await goalsApi.deleteGoal(goal.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Metas de economia</h1>
        <p className="text-muted-foreground">Acompanhe o progresso das suas metas e registre aportes.</p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Nova meta</Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Nova meta</CardTitle>
            <CardDescription>Defina um nome, valor alvo e prazo para a sua meta de economia.</CardDescription>
          </CardHeader>
          <CardContent>
            <CreateGoalForm
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await goalsApi.createGoal({
                  name: values.name,
                  targetAmount: Number(values.targetAmount),
                  deadline: values.deadline,
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
            <CardTitle>Editar meta</CardTitle>
          </CardHeader>
          <CardContent>
            <CreateGoalForm
              initialValues={{
                name: panel.edit.name,
                targetAmount: String(panel.edit.targetAmount),
                deadline: panel.edit.deadline,
              }}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await goalsApi.updateGoal(panel.edit.id, {
                  name: values.name,
                  targetAmount: Number(values.targetAmount),
                  deadline: values.deadline,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'contribute' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Registrar aporte em {panel.contribute.name}</CardTitle>
          </CardHeader>
          <CardContent>
            <AddContributionForm
              onCancel={() => setPanel('none')}
              onSubmit={async (amount, contributionDate) => {
                await goalsApi.addContribution(panel.contribute.id, amount, contributionDate)
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      <GoalHistorySection
        goals={goals}
        onAddContribution={(goal) => setPanel({ contribute: goal })}
        onEdit={(goal) => setPanel({ edit: goal })}
        onDelete={handleDelete}
      />
    </div>
  )
}
