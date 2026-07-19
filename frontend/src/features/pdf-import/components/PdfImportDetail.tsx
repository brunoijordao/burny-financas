import { Button } from '@/components/ui/button'
import type { Category } from '@/features/categories/api/categoriesApi'
import type { EditPdfImportItemFormValues } from '@/features/pdf-import/schemas'
import type { PdfImportDetail as PdfImportDetailData } from '@/features/pdf-import/api/pdfImportApi'
import { PdfImportItemRow } from '@/features/pdf-import/components/PdfImportItemRow'

interface PdfImportDetailProps {
  detail: PdfImportDetailData
  categories: Category[]
  onRetry: () => Promise<void>
  onSaveItem: (itemId: number, values: EditPdfImportItemFormValues) => Promise<void>
  onDiscardItem: (itemId: number) => Promise<void>
  onConfirmItem: (itemId: number) => Promise<void>
}

export function PdfImportDetail({
  detail,
  categories,
  onRetry,
  onSaveItem,
  onDiscardItem,
  onConfirmItem,
}: PdfImportDetailProps) {
  const { pdfImport, items } = detail

  if (pdfImport.status === 'PROCESSING') {
    return (
      <p className="text-sm text-muted-foreground">
        Extraindo e interpretando o extrato "{pdfImport.originalFilename}"... isso pode levar alguns instantes.
      </p>
    )
  }

  if (pdfImport.status === 'FAILED') {
    return (
      <div className="flex flex-col gap-3">
        <p className="text-sm text-destructive">
          {pdfImport.failureReason ?? 'Não foi possível processar este extrato.'}
        </p>
        <div>
          <Button type="button" variant="outline" size="sm" onClick={() => void onRetry()}>
            Tentar novamente
          </Button>
        </div>
      </div>
    )
  }

  const pendingItems = items.filter((item) => item.status === 'PENDING')
  const resolvedItems = items.filter((item) => item.status !== 'PENDING')

  return (
    <div className="flex flex-col gap-4">
      {items.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Nenhuma transação foi identificada neste extrato.
        </p>
      ) : (
        <>
          {pendingItems.length > 0 ? (
            <div className="flex flex-col gap-2">
              <p className="text-sm text-muted-foreground">
                Revise cada transação identificada antes de confirmá-la.
              </p>
              {pendingItems.map((item) => (
                <PdfImportItemRow
                  key={item.id}
                  item={item}
                  categories={categories}
                  onSave={(values) => onSaveItem(item.id, values)}
                  onDiscard={() => onDiscardItem(item.id)}
                  onConfirm={() => onConfirmItem(item.id)}
                />
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Todos os itens já foram revisados.</p>
          )}

          {resolvedItems.length > 0 ? (
            <div className="flex flex-col gap-2">
              {resolvedItems.map((item) => (
                <PdfImportItemRow
                  key={item.id}
                  item={item}
                  categories={categories}
                  onSave={(values) => onSaveItem(item.id, values)}
                  onDiscard={() => onDiscardItem(item.id)}
                  onConfirm={() => onConfirmItem(item.id)}
                />
              ))}
            </div>
          ) : null}
        </>
      )}
    </div>
  )
}
