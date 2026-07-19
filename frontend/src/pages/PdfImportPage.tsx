import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import * as pdfImportApi from '@/features/pdf-import/api/pdfImportApi'
import type { PdfImport, PdfImportDetail as PdfImportDetailData } from '@/features/pdf-import/api/pdfImportApi'
import { PdfImportDetail } from '@/features/pdf-import/components/PdfImportDetail'
import { PdfImportHistoryList } from '@/features/pdf-import/components/PdfImportHistoryList'
import { PdfImportUploadForm } from '@/features/pdf-import/components/PdfImportUploadForm'
import type { EditPdfImportItemFormValues } from '@/features/pdf-import/schemas'

const POLL_INTERVAL_MS = 2000

export function PdfImportPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [imports, setImports] = useState<PdfImport[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<PdfImportDetailData | null>(null)
  const [showUpload, setShowUpload] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)

  const reloadLookups = useCallback(async () => {
    const [accountList, categoryList] = await Promise.all([accountsApi.fetchAccounts(), categoriesApi.fetchCategories()])
    setAccounts(accountList)
    setCategories(categoryList)
  }, [])

  const reloadImports = useCallback(async () => {
    try {
      const list = await pdfImportApi.fetchPdfImports()
      setImports(list)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar suas importações. Tente novamente em instantes.')
    }
  }, [])

  const reloadDetail = useCallback(async (id: number) => {
    const result = await pdfImportApi.fetchPdfImportDetail(id)
    setDetail(result)
    return result
  }, [])

  useEffect(() => {
    void reloadLookups()
    void reloadImports()
  }, [reloadLookups, reloadImports])

  // Poll while the selected import is still processing (extraction + Gemma interpretation run
  // asynchronously on the backend), stopping as soon as it reaches a terminal state.
  useEffect(() => {
    if (selectedId == null) {
      return
    }
    void reloadDetail(selectedId)

    const interval = window.setInterval(() => {
      void reloadDetail(selectedId).then((result) => {
        if (result.pdfImport.status !== 'PROCESSING') {
          window.clearInterval(interval)
          void reloadImports()
        }
      })
    }, POLL_INTERVAL_MS)

    return () => window.clearInterval(interval)
  }, [selectedId, reloadDetail, reloadImports])

  const handleUpload = async (accountId: number, file: File) => {
    const created = await pdfImportApi.uploadPdfImport(accountId, file)
    setShowUpload(false)
    setSelectedId(created.id)
    await reloadImports()
  }

  const handleRetry = async () => {
    if (selectedId == null) {
      return
    }
    await pdfImportApi.retryPdfImport(selectedId)
    await reloadDetail(selectedId)
    await reloadImports()
  }

  const handleSaveItem = async (itemId: number, values: EditPdfImportItemFormValues) => {
    if (selectedId == null) {
      return
    }
    await pdfImportApi.updatePdfImportItem(selectedId, itemId, {
      transactionDate: values.transactionDate,
      description: values.description,
      amount: Number(values.amount),
      type: values.type,
      categoryId: values.categoryId ? Number(values.categoryId) : undefined,
    })
    await reloadDetail(selectedId)
  }

  const handleDiscardItem = async (itemId: number) => {
    if (selectedId == null) {
      return
    }
    await pdfImportApi.discardPdfImportItem(selectedId, itemId)
    await reloadDetail(selectedId)
  }

  const handleConfirmItem = async (itemId: number) => {
    if (selectedId == null) {
      return
    }
    await pdfImportApi.confirmPdfImportItem(selectedId, itemId)
    await reloadDetail(selectedId)
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Importar extrato do Itaú</h1>
        <p className="text-muted-foreground">
          Envie um extrato em PDF para que suas transações sejam sugeridas automaticamente.
        </p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setShowUpload(true)} disabled={accounts.length === 0}>
          Nova importação
        </Button>
      </div>

      {accounts.length === 0 ? (
        <p className="text-sm text-muted-foreground">Cadastre uma conta antes de importar um extrato.</p>
      ) : null}

      {showUpload ? (
        <Card>
          <CardHeader>
            <CardTitle>Enviar extrato</CardTitle>
            <CardDescription>Apenas extratos do Itaú em PDF são suportados nesta versão.</CardDescription>
          </CardHeader>
          <CardContent>
            <PdfImportUploadForm accounts={accounts} onUpload={handleUpload} onCancel={() => setShowUpload(false)} />
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-6 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <div>
          <h2 className="mb-2 text-sm font-medium text-muted-foreground">Importações</h2>
          <PdfImportHistoryList imports={imports} selectedId={selectedId} onSelect={(pdfImport) => setSelectedId(pdfImport.id)} />
        </div>

        <div>
          {detail && selectedId != null ? (
            <Card>
              <CardHeader>
                <CardTitle>{detail.pdfImport.originalFilename}</CardTitle>
              </CardHeader>
              <CardContent>
                <PdfImportDetail
                  detail={detail}
                  categories={categories}
                  onRetry={handleRetry}
                  onSaveItem={handleSaveItem}
                  onDiscardItem={handleDiscardItem}
                  onConfirmItem={handleConfirmItem}
                />
              </CardContent>
            </Card>
          ) : (
            <p className="text-sm text-muted-foreground">Selecione uma importação para revisar suas transações.</p>
          )}
        </div>
      </div>
    </div>
  )
}
