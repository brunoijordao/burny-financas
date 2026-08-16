import { ChevronRight, Tags } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PreferencesForm } from '@/features/settings/components/PreferencesForm'

export function SettingsPage() {
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Configurações</h1>
        <p className="text-muted-foreground">Ajuste como o sistema exibe seus dados e organize suas regras de categorização.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Preferências de exibição</CardTitle>
          <CardDescription>Moeda e formato de data usados em todo o sistema.</CardDescription>
        </CardHeader>
        <CardContent>
          <PreferencesForm />
        </CardContent>
      </Card>

      <Link
        to="/settings/auto-categorization"
        className="flex items-center gap-4 rounded-card border border-border bg-card p-6 text-card-foreground shadow-card outline-none transition-colors hover:bg-secondary focus-visible:ring-1 focus-visible:ring-ring"
      >
        <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
          <Tags className="size-5" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="font-medium">Regras de auto-categorização</p>
          <p className="text-sm text-muted-foreground">Veja e gerencie as palavras-chave de todas as suas categorias em um só lugar.</p>
        </div>
        <ChevronRight className="size-5 shrink-0 text-muted-foreground" aria-hidden="true" />
      </Link>
    </div>
  )
}
