import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { selectFieldClassName as selectClassName } from '@/components/ui/select-field'
import { cn } from '@/lib/utils'
import * as investmentsApi from '@/features/investments/api/investmentsApi'
import type { BenchmarkComparison } from '@/features/investments/api/investmentsApi'
import { benchmarkComparisonSchema, type BenchmarkComparisonFormValues } from '@/features/investments/schemas'

const benchmarkLabels: Record<BenchmarkComparisonFormValues['benchmarkType'], string> = {
  CDI: 'CDI',
  IBOVESPA: 'IBOVESPA',
  IPCA: 'IPCA',
}

function formatPercentage(value: number) {
  return `${value.toFixed(2)}%`
}

/** The benchmark percentage is typed in by the user for each comparison — nothing is fetched from an external index or persisted (design.md Decision 3). */
export function BenchmarkComparisonCard() {
  const [result, setResult] = useState<BenchmarkComparison | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BenchmarkComparisonFormValues>({
    resolver: zodResolver(benchmarkComparisonSchema),
    defaultValues: {
      benchmarkType: 'CDI',
      benchmarkPercentage: '',
      periodStart: new Date(new Date().getFullYear(), 0, 1).toISOString().slice(0, 10),
      periodEnd: new Date().toISOString().slice(0, 10),
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const comparison = await investmentsApi.fetchBenchmarkComparison(
        values.benchmarkType,
        Number(values.benchmarkPercentage),
        values.periodStart,
        values.periodEnd,
      )
      setResult(comparison)
    } catch {
      setFormError('Não foi possível calcular a comparação. Tente novamente em instantes.')
    }
  })

  return (
    <div className="flex flex-col gap-4">
      <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
        <div className="flex flex-wrap gap-4">
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor="benchmarkType">Benchmark</Label>
            <select id="benchmarkType" className={selectClassName} {...register('benchmarkType')}>
              {(Object.keys(benchmarkLabels) as Array<BenchmarkComparisonFormValues['benchmarkType']>).map((type) => (
                <option key={type} value={type}>
                  {benchmarkLabels[type]}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor="benchmarkPercentage">Percentual no período</Label>
            <Input
              id="benchmarkPercentage"
              type="number"
              step="0.01"
              placeholder="0,00"
              aria-invalid={Boolean(errors.benchmarkPercentage)}
              {...register('benchmarkPercentage')}
            />
            {errors.benchmarkPercentage ? (
              <p className="text-sm text-destructive">{errors.benchmarkPercentage.message}</p>
            ) : null}
          </div>
        </div>

        <div className="flex flex-wrap gap-4">
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor="periodStart">Início do período</Label>
            <Input id="periodStart" type="date" {...register('periodStart')} />
          </div>
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor="periodEnd">Fim do período</Label>
            <Input id="periodEnd" type="date" {...register('periodEnd')} />
          </div>
        </div>

        {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Calculando...' : 'Comparar'}
          </Button>
        </div>
      </form>

      {result ? (
        <div className="grid gap-4 sm:grid-cols-2 rounded-card border border-border p-4">
          <div className="flex flex-col gap-1">
            <span className="text-xs text-muted-foreground">{benchmarkLabels[result.benchmarkType]} no período</span>
            <span className="text-xl font-semibold">{formatPercentage(result.benchmarkPercentage)}</span>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs text-muted-foreground">Sua carteira no período</span>
            <span
              className={cn(
                'text-xl font-semibold',
                result.portfolioReturnPercentage !== null && result.portfolioReturnPercentage >= 0
                  ? 'text-foreground'
                  : 'text-destructive',
              )}
            >
              {result.portfolioReturnPercentage !== null ? formatPercentage(result.portfolioReturnPercentage) : 'Sem dados suficientes'}
            </span>
          </div>
        </div>
      ) : null}
    </div>
  )
}
