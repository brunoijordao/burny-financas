import { useCallback, useEffect, useMemo, useState } from 'react'
import { ChevronDown } from 'lucide-react'

import { cn } from '@/lib/utils'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import { KeywordManager } from '@/features/categories/components/KeywordManager'

interface CategoryRow {
  category: Category
  isSubcategory: boolean
}

function flattenCategories(categories: Category[]): CategoryRow[] {
  return categories.flatMap((category) => [
    { category, isSubcategory: false },
    ...category.subcategories.map((subcategory) => ({ category: subcategory, isSubcategory: true })),
  ])
}

/**
 * Consolidated view over every category's auto-categorization keywords, reusing the existing
 * `KeywordManager` (built against `GET/POST/DELETE /categories/{id}/keywords`) per row instead of
 * only surfacing it one category at a time via the `/categories` modal (see design.md Decision 5).
 */
export function AutoCategorizationRules() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const reload = useCallback(async () => {
    try {
      const list = await categoriesApi.fetchCategories()
      setCategories(list)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar suas categorias. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const rows = useMemo(() => flattenCategories(categories), [categories])

  if (loadError) {
    return <p className="text-sm text-destructive">{loadError}</p>
  }

  if (rows.length === 0) {
    return <p className="text-sm text-muted-foreground">Você ainda não tem categorias cadastradas.</p>
  }

  return (
    <ul className="flex flex-col divide-y divide-border rounded-lg border border-border">
      {rows.map(({ category, isSubcategory }) => {
        const isExpanded = expandedId === category.id
        return (
          <li key={category.id}>
            <button
              type="button"
              onClick={() => setExpandedId(isExpanded ? null : category.id)}
              aria-expanded={isExpanded}
              className={cn(
                'flex w-full items-center justify-between gap-3 px-4 py-3 text-left outline-none transition-colors hover:bg-secondary focus-visible:ring-2 focus-visible:ring-ring',
                isSubcategory && 'pl-10',
              )}
            >
              <span className="flex min-w-0 items-center gap-2 font-medium">
                <span
                  aria-hidden="true"
                  className="inline-block size-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: category.color }}
                />
                <span className="truncate">{category.name}</span>
              </span>
              <ChevronDown
                aria-hidden="true"
                className={cn('size-4 shrink-0 text-muted-foreground transition-transform', isExpanded && 'rotate-180')}
              />
            </button>

            {isExpanded ? (
              <div className={cn('border-t border-border bg-muted/30 px-4 py-4', isSubcategory && 'pl-10')}>
                <KeywordManager category={category} onClose={() => setExpandedId(null)} />
              </div>
            ) : null}
          </li>
        )
      })}
    </ul>
  )
}
