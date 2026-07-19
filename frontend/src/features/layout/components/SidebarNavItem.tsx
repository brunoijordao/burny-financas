import { NavLink } from 'react-router-dom'

import { cn } from '@/lib/utils'
import type { NavItem } from '@/features/layout/config/navItems'

interface SidebarNavItemProps {
  item: NavItem
  collapsed: boolean
  onNavigate?: () => void
}

export function SidebarNavItem({ item, collapsed, onNavigate }: SidebarNavItemProps) {
  const Icon = item.icon

  return (
    <NavLink
      to={item.path}
      end={item.path === '/'}
      onClick={onNavigate}
      title={collapsed ? item.label : undefined}
      aria-label={item.label}
      className={({ isActive }) =>
        cn(
          'group relative flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium outline-none transition-colors',
          'text-sidebar-muted-foreground hover:bg-sidebar-active-bg hover:text-sidebar-foreground',
          'focus-visible:ring-2 focus-visible:ring-sidebar-accent-bar',
          collapsed && 'justify-center px-0',
          isActive && 'bg-sidebar-active-bg text-sidebar-foreground',
        )
      }
    >
      {({ isActive }) => (
        <>
          <span
            aria-hidden="true"
            className={cn(
              'absolute left-0 h-5 w-[3px] rounded-full bg-sidebar-accent-bar transition-opacity',
              isActive ? 'opacity-100' : 'opacity-0',
            )}
          />
          <Icon className="size-5 shrink-0" strokeWidth={isActive ? 2.25 : 1.75} />
          {!collapsed && <span className="truncate">{item.label}</span>}
        </>
      )}
    </NavLink>
  )
}
