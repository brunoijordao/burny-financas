import { LogOut, PanelLeftClose, PanelLeftOpen } from 'lucide-react'

import { cn } from '@/lib/utils'
import { useAuthActions } from '@/features/auth/hooks/useAuthActions'
import { useSessionStore } from '@/features/auth/store/sessionStore'
import { navItems } from '@/features/layout/config/navItems'
import { SidebarNavItem } from '@/features/layout/components/SidebarNavItem'

interface SidebarProps {
  collapsed: boolean
  /** Omit to hide the collapse/expand control (used by the mobile overlay, which has its own close affordance). */
  onToggleCollapse?: () => void
  onNavigate?: () => void
  className?: string
}

export function Sidebar({ collapsed, onToggleCollapse, onNavigate, className }: SidebarProps) {
  const email = useSessionStore((state) => state.email)
  const { logout } = useAuthActions()

  return (
    <div
      className={cn(
        'flex h-full flex-col border-r border-sidebar-border bg-sidebar-background text-sidebar-foreground',
        className,
      )}
    >
      <div className={cn('flex items-center gap-3 border-b border-sidebar-border px-4 py-4', collapsed && 'justify-center px-2')}>
        <div
          aria-hidden="true"
          className="flex size-9 shrink-0 items-center justify-center rounded-full bg-sidebar-active-bg text-sm font-semibold text-sidebar-foreground"
        >
          {email ? email.charAt(0).toUpperCase() : '?'}
        </div>
        {!collapsed && (
          <p className="min-w-0 flex-1 truncate text-sm font-medium text-sidebar-foreground" title={email ?? undefined}>
            {email ?? 'Usuário'}
          </p>
        )}
      </div>

      <div className={cn('px-2 pt-2', collapsed && 'px-2')}>
        <button
          type="button"
          onClick={() => void logout()}
          aria-label="Sair"
          title={collapsed ? 'Sair' : undefined}
          className={cn(
            'flex h-9 w-full items-center gap-2 rounded-md px-3 text-sm text-sidebar-muted-foreground outline-none transition-colors hover:bg-sidebar-active-bg hover:text-sidebar-foreground focus-visible:ring-2 focus-visible:ring-sidebar-accent-bar',
            collapsed && 'justify-center px-0',
          )}
        >
          <LogOut className="size-5 shrink-0" />
          {!collapsed && <span>Sair</span>}
        </button>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-2 py-3" aria-label="Navegação principal">
        {navItems.map((item) => (
          <SidebarNavItem key={item.path} item={item} collapsed={collapsed} onNavigate={onNavigate} />
        ))}
      </nav>

      {onToggleCollapse && (
        <div className="border-t border-sidebar-border p-2">
          <button
            type="button"
            onClick={onToggleCollapse}
            aria-label={collapsed ? 'Expandir menu' : 'Recolher menu'}
            title={collapsed ? 'Expandir menu' : 'Recolher menu'}
            className={cn(
              'flex h-9 w-full items-center gap-2 rounded-md px-3 text-sm text-sidebar-muted-foreground outline-none transition-colors hover:bg-sidebar-active-bg hover:text-sidebar-foreground focus-visible:ring-2 focus-visible:ring-sidebar-accent-bar',
              collapsed && 'justify-center px-0',
            )}
          >
            {collapsed ? <PanelLeftOpen className="size-5" /> : <PanelLeftClose className="size-5" />}
            {!collapsed && <span>Recolher</span>}
          </button>
        </div>
      )}
    </div>
  )
}
