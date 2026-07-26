import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as investmentsApi from '@/features/investments/api/investmentsApi'
import type { AllocationItem, InvestmentAsset, NetWorthEvolutionPoint, PortfolioSummary } from '@/features/investments/api/investmentsApi'
import { AllocationChart } from '@/features/investments/components/AllocationChart'
import { AssetForm } from '@/features/investments/components/AssetForm'
import { AssetList } from '@/features/investments/components/AssetList'
import { BenchmarkComparisonCard } from '@/features/investments/components/BenchmarkComparisonCard'
import { NetWorthEvolutionChart } from '@/features/investments/components/NetWorthEvolutionChart'
import { OperationForm } from '@/features/investments/components/OperationForm'
import { PortfolioSummaryCard } from '@/features/investments/components/PortfolioSummaryCard'
import { ValuationForm } from '@/features/investments/components/ValuationForm'

type Panel =
  | 'none'
  | 'create'
  | { edit: InvestmentAsset }
  | { operation: InvestmentAsset }
  | { valuation: InvestmentAsset }

export function InvestmentsPage() {
  const [assets, setAssets] = useState<InvestmentAsset[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [summary, setSummary] = useState<PortfolioSummary | null>(null)
  const [allocation, setAllocation] = useState<AllocationItem[]>([])
  const [evolution, setEvolution] = useState<NetWorthEvolutionPoint[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

  const reload = useCallback(async () => {
    try {
      const [assetList, accountList, summaryData, allocationData, evolutionData] = await Promise.all([
        investmentsApi.fetchInvestmentAssets(),
        accountsApi.fetchAccounts(),
        investmentsApi.fetchPortfolioSummary(),
        investmentsApi.fetchPortfolioAllocation(),
        investmentsApi.fetchNetWorthEvolution(),
      ])
      setAssets(assetList)
      setAccounts(accountList)
      setSummary(summaryData)
      setAllocation(allocationData)
      setEvolution(evolutionData)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar seus investimentos. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const handleDelete = async (asset: InvestmentAsset) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir o ativo "${asset.name}"?`)
    if (!confirmed) {
      return
    }
    await investmentsApi.deleteInvestmentAsset(asset.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Investimentos</h1>
        <p className="text-muted-foreground">
          Carteira de ativos, aportes e resgates, e rentabilidade com valores de mercado informados manualmente.
        </p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      {summary ? (
        <Card>
          <CardHeader>
            <CardTitle>Resumo da carteira</CardTitle>
          </CardHeader>
          <CardContent>
            <PortfolioSummaryCard summary={summary} />
          </CardContent>
        </Card>
      ) : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Novo ativo</Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Novo ativo</CardTitle>
            <CardDescription>Cadastre uma ação, FII, CDB, Tesouro Direto ou criptomoeda.</CardDescription>
          </CardHeader>
          <CardContent>
            <AssetForm
              accounts={accounts}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await investmentsApi.createInvestmentAsset({
                  name: values.name,
                  ticker: values.ticker || undefined,
                  type: values.type,
                  accountId: values.accountId ? Number(values.accountId) : undefined,
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
            <CardTitle>Editar ativo</CardTitle>
          </CardHeader>
          <CardContent>
            <AssetForm
              accounts={accounts}
              initialValues={{
                name: panel.edit.name,
                ticker: panel.edit.ticker ?? '',
                type: panel.edit.type,
                accountId: panel.edit.accountId ? String(panel.edit.accountId) : '',
              }}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await investmentsApi.updateInvestmentAsset(panel.edit.id, {
                  name: values.name,
                  ticker: values.ticker || undefined,
                  type: values.type,
                  accountId: values.accountId ? Number(values.accountId) : undefined,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'operation' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Registrar operação em {panel.operation.name}</CardTitle>
            <CardDescription>Aportes e resgates não afetam o saldo de nenhuma conta.</CardDescription>
          </CardHeader>
          <CardContent>
            <OperationForm
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await investmentsApi.createInvestmentOperation(panel.operation.id, {
                  type: values.type,
                  quantity: Number(values.quantity),
                  unitPrice: Number(values.unitPrice),
                  operationDate: values.operationDate,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'valuation' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Atualizar valor atual de {panel.valuation.name}</CardTitle>
            <CardDescription>Informe o valor de mercado atual — não há integração de cotação em tempo real.</CardDescription>
          </CardHeader>
          <CardContent>
            <ValuationForm
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await investmentsApi.createInvestmentValuation(panel.valuation.id, {
                  valueDate: values.valueDate,
                  totalValue: Number(values.totalValue),
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      <AssetList
        assets={assets}
        onAddOperation={(asset) => setPanel({ operation: asset })}
        onAddValuation={(asset) => setPanel({ valuation: asset })}
        onEdit={(asset) => setPanel({ edit: asset })}
        onDelete={handleDelete}
      />

      <Card>
        <CardHeader>
          <CardTitle>Distribuição da carteira</CardTitle>
          <CardDescription>Percentual do valor atual por tipo de ativo.</CardDescription>
        </CardHeader>
        <CardContent>
          <AllocationChart items={allocation} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Evolução do patrimônio</CardTitle>
          <CardDescription>Soma do valor de todos os ativos ao longo do tempo, a partir dos valores informados.</CardDescription>
        </CardHeader>
        <CardContent>
          <NetWorthEvolutionChart points={evolution} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Comparativo com benchmarks</CardTitle>
          <CardDescription>Compare a rentabilidade da sua carteira com CDI, IBOVESPA ou IPCA informados manualmente.</CardDescription>
        </CardHeader>
        <CardContent>
          <BenchmarkComparisonCard />
        </CardContent>
      </Card>
    </div>
  )
}
