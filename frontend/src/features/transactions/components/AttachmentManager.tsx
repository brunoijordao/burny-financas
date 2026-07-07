import { useCallback, useEffect, useState } from 'react'
import { useDropzone } from 'react-dropzone'

import { Button } from '@/components/ui/button'
import * as transactionsApi from '@/features/transactions/api/transactionsApi'
import type { Transaction, TransactionAttachment } from '@/features/transactions/api/transactionsApi'

const ACCEPTED_TYPES = {
  'image/png': ['.png'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'application/pdf': ['.pdf'],
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

interface AttachmentManagerProps {
  transaction: Transaction
  onClose: () => void
}

export function AttachmentManager({ transaction, onClose }: AttachmentManagerProps) {
  const [attachments, setAttachments] = useState<TransactionAttachment[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  const reload = useCallback(async () => {
    try {
      const list = await transactionsApi.fetchAttachments(transaction.id)
      setAttachments(list)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar os anexos.')
    }
  }, [transaction.id])

  useEffect(() => {
    void reload()
  }, [reload])

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      if (acceptedFiles.length === 0) {
        return
      }
      setUploading(true)
      setUploadError(null)
      try {
        for (const file of acceptedFiles) {
          await transactionsApi.uploadAttachment(transaction.id, file)
        }
        await reload()
      } catch {
        setUploadError('Não foi possível enviar o arquivo. Verifique o tipo (PNG, JPEG ou PDF).')
      } finally {
        setUploading(false)
      }
    },
    [transaction.id, reload],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop, accept: ACCEPTED_TYPES })

  const handleDownload = async (attachment: TransactionAttachment) => {
    const blob = await transactionsApi.downloadAttachment(transaction.id, attachment.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFilename
    link.click()
    URL.revokeObjectURL(url)
  }

  const handleRemove = async (attachment: TransactionAttachment) => {
    await transactionsApi.deleteAttachment(transaction.id, attachment.id)
    await reload()
  }

  return (
    <div className="flex flex-col gap-4">
      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex flex-col gap-2">
        {attachments.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhum anexo ainda.</p>
        ) : (
          attachments.map((attachment) => (
            <div
              key={attachment.id}
              className="flex items-center justify-between gap-2 rounded-md border border-input px-3 py-2 text-sm"
            >
              <div className="flex flex-col">
                <span>{attachment.originalFilename}</span>
                <span className="text-xs text-muted-foreground">{formatSize(attachment.sizeBytes)}</span>
              </div>
              <div className="flex gap-2">
                <Button type="button" variant="outline" size="sm" onClick={() => void handleDownload(attachment)}>
                  Baixar
                </Button>
                <Button type="button" variant="ghost" size="sm" onClick={() => void handleRemove(attachment)}>
                  Remover
                </Button>
              </div>
            </div>
          ))
        )}
      </div>

      <div
        {...getRootProps()}
        className={`flex cursor-pointer flex-col items-center justify-center rounded-md border-2 border-dashed p-6 text-sm text-muted-foreground ${
          isDragActive ? 'border-ring bg-accent' : 'border-input'
        }`}
      >
        <input {...getInputProps()} />
        {uploading ? (
          <span>Enviando...</span>
        ) : isDragActive ? (
          <span>Solte o arquivo aqui</span>
        ) : (
          <span>Arraste um comprovante (PNG, JPEG ou PDF) ou clique para selecionar</span>
        )}
      </div>

      {uploadError ? <p className="text-sm text-destructive">{uploadError}</p> : null}

      <div className="flex justify-end">
        <Button type="button" variant="outline" onClick={onClose}>
          Fechar
        </Button>
      </div>
    </div>
  )
}
