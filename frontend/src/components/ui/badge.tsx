import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

// DESIGN.md Badge spec: Solid / Soft / Outline, all pill-shaped at the shared
// 18px badge radius, 12px/500 type, 2px 8px padding.
const badgeVariants = cva(
  'inline-flex items-center rounded-badge px-2 py-0.5 text-xs font-medium',
  {
    variants: {
      variant: {
        solid: 'bg-ink-soft text-surface-alt',
        soft: 'bg-secondary text-secondary-foreground',
        outline: 'border border-border bg-transparent text-foreground',
        destructive: 'bg-destructive/10 text-destructive',
      },
    },
    defaultVariants: {
      variant: 'soft',
    },
  },
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <span data-slot="badge" className={cn(badgeVariants({ variant, className }))} {...props} />
}

export { Badge, badgeVariants }
