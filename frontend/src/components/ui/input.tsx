import * as React from 'react'

import { cn } from '@/lib/utils'

function Input({ className, type, ...props }: React.ComponentProps<'input'>) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        // Input Field spec: #f5f5f5 resting fill, no border at rest, 18px radius,
        // hairline ring replaces the fill on focus instead of an offset shadow.
        'flex h-10 w-full rounded-input border border-transparent bg-muted px-2.5 py-2 text-sm text-foreground transition-colors placeholder:text-muted-foreground focus-visible:border-input focus-visible:bg-transparent focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
      {...props}
    />
  )
}

export { Input }
