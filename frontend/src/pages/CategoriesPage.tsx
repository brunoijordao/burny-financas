import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import { CategoryList } from '@/features/categories/components/CategoryList'
import { CreateCategoryForm } from '@/features/categories/components/CreateCategoryForm'
import { EditCategoryForm } from '@/features/categories/components/EditCategoryForm'
import { KeywordManager } from '@/features/categories/components/KeywordManager'

type Panel = 'none' | 'create' | { edit: Category } | { keywords: Category }

export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

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

  const handleDelete = async (category: Category) => {
    const warning =
      category.subcategories.length > 0
        ? `Excluir "${category.name}" também desativará suas subcategorias. Deseja continuar?`
        : `Tem certeza que deseja excluir a categoria "${category.name}"?`
    const confirmed = window.confirm(warning)
    if (!confirmed) {
      return
    }
    await categoriesApi.deleteCategory(category.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Suas categorias</h1>
        <p className="text-muted-foreground">Organize suas transações por categoria e subcategoria.</p>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Nova categoria</Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Nova categoria</CardTitle>
            <CardDescription>Cadastre uma categoria ou subcategoria personalizada.</CardDescription>
          </CardHeader>
          <CardContent>
            <CreateCategoryForm
              topLevelCategories={categories}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await categoriesApi.createCategory({
                  name: values.name,
                  icon: values.icon,
                  color: values.color,
                  parentCategoryId: values.parentCategoryId ? Number(values.parentCategoryId) : undefined,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'edit' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Editar categoria</CardTitle>
          </CardHeader>
          <CardContent>
            <EditCategoryForm
              category={panel.edit}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await categoriesApi.updateCategory(panel.edit.id, values)
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {typeof panel === 'object' && 'keywords' in panel ? (
        <Card>
          <CardHeader>
            <CardTitle>Palavras-chave de {panel.keywords.name}</CardTitle>
            <CardDescription>Usadas para categorização automática de transações.</CardDescription>
          </CardHeader>
          <CardContent>
            <KeywordManager category={panel.keywords} onClose={() => setPanel('none')} />
          </CardContent>
        </Card>
      ) : null}

      <CategoryList
        categories={categories}
        onEdit={(category) => setPanel({ edit: category })}
        onDelete={handleDelete}
        onManageKeywords={(category) => setPanel({ keywords: category })}
      />
    </div>
  )
}
