import {
  ArrowLeftRight,
  FileUp,
  LayoutDashboard,
  PiggyBank,
  Tags,
  Target,
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
]
