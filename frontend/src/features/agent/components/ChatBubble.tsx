import { Bot, TriangleAlert } from 'lucide-react'

import { cn } from '@/lib/utils'

export interface DisplayMessage {
  id: string
  role: 'USER' | 'MODEL' | 'ERROR'
  text: string
}

interface ChatBubbleProps {
  message: DisplayMessage
}

export function ChatBubble({ message }: ChatBubbleProps) {
  if (message.role === 'USER') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] rounded-card rounded-br-sm bg-primary px-4 py-2.5 text-sm text-primary-foreground shadow-card sm:max-w-[70%]">
          {message.text}
        </div>
      </div>
    )
  }

  if (message.role === 'ERROR') {
    return (
      <div className="flex items-start gap-2.5">
        <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-destructive/10 text-destructive">
          <TriangleAlert className="size-4" />
        </div>
        <div className="max-w-[85%] rounded-card rounded-tl-sm border border-destructive/30 bg-destructive/5 px-4 py-2.5 text-sm text-destructive sm:max-w-[70%]">
          {message.text}
        </div>
      </div>
    )
  }

  return (
    <div className="flex items-start gap-2.5">
      <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-secondary text-foreground">
        <Bot className="size-4" />
      </div>
      <div className="max-w-[85%] rounded-card rounded-tl-sm bg-secondary px-4 py-2.5 text-sm text-secondary-foreground shadow-card sm:max-w-[70%]">
        <p className="whitespace-pre-wrap">{message.text}</p>
      </div>
    </div>
  )
}

export function TypingIndicator() {
  return (
    <div className="flex items-start gap-2.5">
      <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-secondary text-foreground">
        <Bot className="size-4" />
      </div>
      <div className={cn('flex items-center gap-1 rounded-card rounded-tl-sm bg-secondary px-4 py-3')} aria-label="Assistente digitando">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="size-1.5 animate-bounce rounded-full bg-muted-foreground"
            style={{ animationDelay: `${i * 120}ms` }}
          />
        ))}
      </div>
    </div>
  )
}
