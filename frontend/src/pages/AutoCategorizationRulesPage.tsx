import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'

import { AutoCategorizationRules } from '@/features/settings/components/AutoCategorizationRules'

export function AutoCategorizationRulesPage() {
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 p-4 py-8">
      <div>
        <Link
          to="/settings"
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground outline-none transition-colors hover:text-foreground focus-visible:ring-1 focus-visible:ring-ring"
        >
          <ArrowLeft className="size-4" />
          Configurações
        </Link>
        <h1 className="mt-2 text-2xl font-semibold">Regras de auto-categorização</h1>
        <p className="text-muted-foreground">
          Palavras-chave de todas as suas categorias em um só lugar. Transações cuja descrição contiver uma delas
          são categorizadas automaticamente.
        </p>
      </div>

      <AutoCategorizationRules />
    </div>
  )
}
