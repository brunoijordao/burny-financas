import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/formatters'
import type { AssetType, InvestmentAsset } from '@/features/investments/api/investmentsApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

const typeLabels: Record<AssetType, string> = {
  STOCK: 'Ação',
  FII: 'FII',
  CDB: 'CDB',
  TREASURY_DIRECT: 'Tesouro Direto',
  CRYPTO: 'Criptomoeda',
}

function formatQuantity(value: number) {
  return value.toLocaleString('pt-BR', { maximumFractionDigits: 8 })
}

interface AssetListProps {
  assets: InvestmentAsset[]
  onAddOperation: (asset: InvestmentAsset) => void
  onAddValuation: (asset: InvestmentAsset) => void
  onEdit: (asset: InvestmentAsset) => void
  onDelete: (asset: InvestmentAsset) => void
}

export function AssetList({ assets, onAddOperation, onAddValuation, onEdit, onDelete }: AssetListProps) {
  const currency = usePreferencesStore((state) => state.currency)

  if (assets.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum ativo cadastrado ainda.</p>
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {assets.map((asset) => (
        <Card key={asset.id}>
          <CardHeader className="flex flex-row items-start justify-between gap-2 space-y-0">
            <div>
              <CardTitle>{asset.name}</CardTitle>
              <span className="text-xs text-muted-foreground">
                {typeLabels[asset.type]}
                {asset.accountName ? ` · ${asset.accountName}` : ''}
              </span>
            </div>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="text-muted-foreground">Quantidade</span>
                <p className="font-medium">{formatQuantity(asset.quantity)}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Preço médio</span>
                <p className="font-medium">{formatCurrency(asset.averagePrice, currency)}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Valor investido</span>
                <p className="font-medium">{formatCurrency(asset.investedAmount, currency)}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Valor atual</span>
                <p className="font-medium">
                  {asset.currentValue !== null ? formatCurrency(asset.currentValue, currency) : 'Não informado'}
                </p>
              </div>
            </div>

            {asset.profitabilityAmount !== null && asset.profitabilityPercentage !== null ? (
              <p
                className={cn(
                  'text-sm font-semibold',
                  asset.profitabilityAmount >= 0 ? 'text-foreground' : 'text-destructive',
                )}
              >
                {asset.profitabilityAmount >= 0 ? '+' : ''}
                {formatCurrency(asset.profitabilityAmount, currency)} ({asset.profitabilityPercentage.toFixed(2)}%)
              </p>
            ) : (
              <p className="text-xs text-muted-foreground">Registre um valor atual para ver a rentabilidade.</p>
            )}

            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => onAddValuation(asset)}>
                Atualizar valor
              </Button>
              <Button variant="outline" size="sm" onClick={() => onAddOperation(asset)}>
                Registrar operação
              </Button>
              <Button variant="outline" size="sm" onClick={() => onEdit(asset)}>
                Editar
              </Button>
              <Button variant="ghost" size="sm" onClick={() => onDelete(asset)}>
                Excluir
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
