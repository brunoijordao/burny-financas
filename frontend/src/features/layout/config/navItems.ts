import {
  ArrowLeftRight,
  Bot,
  CalendarClock,
  FileText,
  FileUp,
  LayoutDashboard,
  PiggyBank,
  Settings,
  Tags,
  Target,
  TrendingUp,
  Wallet,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
}

/** Single source of truth for the sidebar menu, used by both the desktop rail and the mobile overlay. */
export const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: LayoutDashboard },
  { label: 'Contas', path: '/accounts', icon: Wallet },
  { label: 'Categorias', path: '/categories', icon: Tags },
  { label: 'Transações', path: '/transactions', icon: ArrowLeftRight },
  { label: 'Importação de PDF', path: '/pdf-imports', icon: FileUp },
  { label: 'Orçamentos', path: '/budgets', icon: PiggyBank },
  { label: 'Metas', path: '/goals', icon: Target },
  { label: 'Planejamento', path: '/planning', icon: CalendarClock },
  { label: 'Investimentos', path: '/investments', icon: TrendingUp },
  { label: 'Relatórios', path: '/reports', icon: FileText },
  { label: 'Assistente', path: '/assistant', icon: Bot },
  { label: 'Configurações', path: '/settings', icon: Settings },
]
