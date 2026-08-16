import { useEffect, useRef, useState } from 'react'
import { isAxiosError } from 'axios'
import { Bot } from 'lucide-react'

import * as agentApi from '@/features/agent/api/agentApi'
import type { ChatMessagePayload, TransactionDraft } from '@/features/agent/api/agentApi'
import { ChatBubble, TypingIndicator, type DisplayMessage } from '@/features/agent/components/ChatBubble'
import { ChatComposer } from '@/features/agent/components/ChatComposer'
import { TransactionDraftCard } from '@/features/agent/components/TransactionDraftCard'

// Matches the "last N turns" default suggested in design.md Decision 8; the backend independently
// enforces its own (larger) cap via app.ai-agent.max-history-messages, so this is a cost/latency
// default, not the security boundary.
const HISTORY_LIMIT = 20

type TranscriptItem =
  | ({ id: string; kind: 'message' } & DisplayMessage)
  | { id: string; kind: 'draft'; draft: TransactionDraft }

function newId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function AssistantPage() {
  const [transcript, setTranscript] = useState<TranscriptItem[]>([])
  const [isSending, setIsSending] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [transcript, isSending])

  const historyForRequest = (): ChatMessagePayload[] =>
    transcript
      .filter((item): item is { id: string; kind: 'message'; role: 'USER' | 'MODEL'; text: string } =>
        item.kind === 'message' && item.role !== 'ERROR',
      )
      .slice(-HISTORY_LIMIT)
      .map((item) => ({ role: item.role, text: item.text }))

  const handleSend = async (text: string) => {
    const history = historyForRequest()
    setTranscript((prev) => [...prev, { id: newId(), kind: 'message', role: 'USER', text }])
    setIsSending(true)

    try {
      const response = await agentApi.sendChatMessage(text, history)
      setTranscript((prev) => {
        const next: TranscriptItem[] = [...prev, { id: newId(), kind: 'message', role: 'MODEL', text: response.reply }]
        if (response.draft) {
          next.push({ id: newId(), kind: 'draft', draft: response.draft })
        }
        return next
      })
    } catch (error) {
      const message =
        isAxiosError(error) && error.response?.status === 429
          ? 'Você atingiu o limite de mensagens por hora. Tente novamente mais tarde.'
          : 'Não consegui falar com o assistente agora. Tente novamente em instantes.'
      setTranscript((prev) => [...prev, { id: newId(), kind: 'message', role: 'ERROR', text: message }])
    } finally {
      setIsSending(false)
    }
  }

  const handleConfirmDraft = async (draft: TransactionDraft) => {
    await agentApi.confirmTransactionDraft({
      description: draft.description,
      amount: draft.amount,
      type: draft.type,
      date: draft.date,
      accountId: draft.accountId,
      categoryId: draft.categoryId,
    })
  }

  const isEmpty = transcript.length === 0

  return (
    <div className="mx-auto flex h-full max-w-3xl flex-col px-4 py-6 sm:py-8">
      <div className="mb-4 flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-full bg-primary text-primary-foreground">
          <Bot className="size-5" />
        </div>
        <div>
          <h1 className="text-xl font-semibold">Assistente financeiro</h1>
          <p className="text-sm text-muted-foreground">Pergunte sobre suas finanças ou descreva uma transação.</p>
        </div>
      </div>

      <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto rounded-card border border-border bg-card/40 p-4">
        {isEmpty ? (
          <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-muted-foreground">
            <Bot className="size-8" />
            <p className="text-sm">
              Experimente perguntar &ldquo;qual meu saldo?&rdquo; ou dizer &ldquo;gastei R$50 com mercado na conta corrente&rdquo;.
            </p>
          </div>
        ) : (
          transcript.map((item) =>
            item.kind === 'message' ? (
              <ChatBubble key={item.id} message={item} />
            ) : (
              <TransactionDraftCard
                key={item.id}
                draft={item.draft}
                onConfirm={() => handleConfirmDraft(item.draft)}
                onDecline={() => {}}
              />
            ),
          )
        )}
        {isSending ? <TypingIndicator /> : null}
      </div>

      <ChatComposer disabled={isSending} onSend={(text) => void handleSend(text)} />
    </div>
  )
}
