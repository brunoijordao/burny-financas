import {
  Car,
  HeartPulse,
  Home,
  PartyPopper,
  ShoppingBag,
  Sparkles,
  Utensils,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

/** Values must match the icons seeded by DefaultCategoryProvisioningService on the backend. */
export const iconOptions: { value: string; label: string; Icon: LucideIcon }[] = [
  { value: 'utensils', label: 'Alimentação', Icon: Utensils },
  { value: 'car', label: 'Transporte', Icon: Car },
  { value: 'home', label: 'Moradia', Icon: Home },
  { value: 'party-popper', label: 'Lazer', Icon: PartyPopper },
  { value: 'heart-pulse', label: 'Saúde', Icon: HeartPulse },
  { value: 'shopping-bag', label: 'Compras', Icon: ShoppingBag },
  { value: 'sparkles', label: 'Outros', Icon: Sparkles },
]

export function CategoryIcon({ icon, className }: { icon: string; className?: string }) {
  const Icon = iconOptions.find((option) => option.value === icon)?.Icon ?? Sparkles
  return <Icon className={className} />
}
