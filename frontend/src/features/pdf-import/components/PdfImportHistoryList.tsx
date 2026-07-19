import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import type { PdfImport } from '@/features/pdf-import/api/pdfImportApi'

const STATUS_LABELS: Record<PdfImport['status'], string> = {
  PROCESSING: 'Processando',
  READY_FOR_REVIEW: 'Pronto para revisão',
  FAILED: 'Falhou',
}

const STATUS_CLASSNAMES: Record<PdfImport['status'], string> = {
  PROCESSING: 'text-muted-foreground',
  READY_FOR_REVIEW: 'text-emerald-600 dark:text-emerald-400',
  FAILED: 'text-destructive',
}

interface PdfImportHistoryListProps {
  imports: PdfImport[]
  selectedId: number | null
  onSelect: (pdfImport: PdfImport) => void
}

export function PdfImportHistoryList({ imports, selectedId, onSelect }: PdfImportHistoryListProps) {
  if (imports.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum extrato importado ainda.</p>
  }

  return (
    <div className="flex flex-col gap-2">
      {imports.map((pdfImport) => (
        <Card key={pdfImport.id} className={selectedId === pdfImport.id ? 'border-ring' : undefined}>
          <CardContent className="flex items-center justify-between gap-3 py-3">
            <div className="flex flex-col">
              <span className="text-sm font-medium">{pdfImport.originalFilename}</span>
              <span className={`text-xs ${STATUS_CLASSNAMES[pdfImport.status]}`}>
                {STATUS_LABELS[pdfImport.status]}
              </span>
            </div>
            <Button type="button" variant="outline" size="sm" onClick={() => onSelect(pdfImport)}>
              Ver
            </Button>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
