import { useEffect, useState } from 'react'
import { Menu, X } from 'lucide-react'
import { Outlet, useLocation } from 'react-router-dom'

import { cn } from '@/lib/utils'
import { useSidebarStore } from '@/features/layout/store/sidebarStore'
import { Sidebar } from '@/features/layout/components/Sidebar'

export function AppLayout() {
  const isCollapsed = useSidebarStore((state) => state.isCollapsed)
  const toggleCollapsed = useSidebarStore((state) => state.toggleCollapsed)
  const [isMobileOpen, setIsMobileOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    setIsMobileOpen(false)
  }, [location.pathname])

  return (
    <div className="flex h-svh overflow-hidden bg-background text-foreground">
      <Sidebar
        collapsed={isCollapsed}
        onToggleCollapse={toggleCollapsed}
        className={cn('hidden shrink-0 transition-[width] duration-200 md:flex', isCollapsed ? 'w-18' : 'w-64')}
      />

      {isMobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <button
            type="button"
            aria-label="Fechar menu"
            onClick={() => setIsMobileOpen(false)}
            className="absolute inset-0 bg-black/40"
          />
          <Sidebar
            collapsed={false}
            onNavigate={() => setIsMobileOpen(false)}
            className="relative z-50 w-72 max-w-[80vw] shadow-xl"
          />
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center gap-3 border-b border-border px-4 py-3 md:hidden">
          <button
            type="button"
            aria-label={isMobileOpen ? 'Fechar menu' : 'Abrir menu'}
            onClick={() => setIsMobileOpen((open) => !open)}
            className="flex size-9 items-center justify-center rounded-md text-foreground outline-none transition-colors hover:bg-secondary focus-visible:ring-2 focus-visible:ring-ring"
          >
            {isMobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
          </button>
          <span className="text-sm font-semibold">Burny Finanças</span>
        </header>

        <main className="min-w-0 flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
