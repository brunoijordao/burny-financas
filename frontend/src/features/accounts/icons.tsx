import { Banknote, Building2, CreditCard, Landmark, PiggyBank, Smartphone, Wallet } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export const iconOptions: { value: string; label: string; Icon: LucideIcon }[] = [
  { value: 'wallet', label: 'Carteira', Icon: Wallet },
  { value: 'landmark', label: 'Banco', Icon: Landmark },
  { value: 'piggy-bank', label: 'Poupança', Icon: PiggyBank },
  { value: 'credit-card', label: 'Cartão', Icon: CreditCard },
  { value: 'banknote', label: 'Dinheiro', Icon: Banknote },
  { value: 'smartphone', label: 'Digital', Icon: Smartphone },
  { value: 'building', label: 'Corretora', Icon: Building2 },
]

export function AccountIcon({ icon, className }: { icon: string; className?: string }) {
  const Icon = iconOptions.find((option) => option.value === icon)?.Icon ?? Wallet
  return <Icon className={className} />
}
