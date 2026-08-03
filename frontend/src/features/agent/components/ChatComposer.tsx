import { useState, type KeyboardEvent } from 'react'
import { SendHorizontal } from 'lucide-react'

import { Button } from '@/components/ui/button'

interface ChatComposerProps {
  disabled: boolean
  onSend: (text: string) => void
}

export function ChatComposer({ disabled, onSend }: ChatComposerProps) {
  const [value, setValue] = useState('')

  const trimmed = value.trim()

  const submit = () => {
    if (!trimmed || disabled) {
      return
    }
    onSend(trimmed)
    setValue('')
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  return (
    <div className="flex items-end gap-2 border-t border-border bg-background p-3 sm:p-4">
      <textarea
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={1}
        placeholder="Pergunte sobre seus gastos ou descreva uma transação..."
        aria-label="Mensagem para o assistente"
        className="max-h-40 min-h-10 flex-1 resize-none rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm outline-none placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
      />
      <Button type="button" size="default" disabled={disabled || !trimmed} onClick={submit} aria-label="Enviar mensagem">
        <SendHorizontal className="size-4" />
      </Button>
    </div>
  )
}
