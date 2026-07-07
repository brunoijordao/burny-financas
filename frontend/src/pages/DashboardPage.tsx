import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { LogoutButton } from '@/features/auth/components/LogoutButton'

/** Placeholder protected view — the real dashboard is a future change; accounts is the first one. */
export function DashboardPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-4 p-4">
      <h1 className="text-2xl font-semibold">Você está autenticado</h1>
      <p className="text-muted-foreground">
        Esta é uma rota protegida. O dashboard completo será implementado em uma change futura.
      </p>
      <div className="flex gap-2">
        <Button asChild>
          <Link to="/accounts">Ver minhas contas</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/categories">Ver minhas categorias</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/transactions">Ver minhas transações</Link>
        </Button>
      </div>
      <LogoutButton />
    </div>
  )
}
