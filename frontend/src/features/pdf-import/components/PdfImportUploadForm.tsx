import { useCallback, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'

const ACCEPTED_TYPES = { 'application/pdf': ['.pdf'] }

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

interface PdfImportUploadFormProps {
  accounts: Account[]
  onUpload: (accountId: number, file: File) => Promise<void>
  onCancel: () => void
}

export function PdfImportUploadForm({ accounts, onUpload, onCancel }: PdfImportUploadFormProps) {
  const [accountId, setAccountId] = useState(accounts[0] ? String(accounts[0].id) : '')
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      const file = acceptedFiles[0]
      if (!file || !accountId) {
        return
      }
      setUploading(true)
      setError(null)
      try {
        await onUpload(Number(accountId), file)
      } catch (uploadError) {
        if (isAxiosError(uploadError) && uploadError.response?.status === 429) {
          setError('Limite de envios de extrato atingido (10 por hora). Tente novamente mais tarde.')
        } else if (isAxiosError(uploadError) && uploadError.response?.status === 404) {
          setError('Conta não encontrada.')
        } else {
          setError('Não foi possível enviar o extrato. Verifique se o arquivo é um PDF válido.')
        }
      } finally {
        setUploading(false)
      }
    },
    [accountId, onUpload],
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: ACCEPTED_TYPES,
    maxFiles: 1,
    disabled: uploading || !accountId,
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="pdf-import-account">Conta</Label>
        <select
          id="pdf-import-account"
          className={selectClassName}
          value={accountId}
          onChange={(event) => setAccountId(event.target.value)}
        >
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </select>
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
          <span>Solte o extrato aqui</span>
        ) : (
          <span>Arraste o extrato em PDF do Itaú ou clique para selecionar</span>
        )}
      </div>

      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={uploading}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}
