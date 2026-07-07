import { useCallback, useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category, CategoryKeyword } from '@/features/categories/api/categoriesApi'
import { addKeywordSchema, type AddKeywordFormValues } from '@/features/categories/schemas'

interface KeywordManagerProps {
  category: Category
  onClose: () => void
}

export function KeywordManager({ category, onClose }: KeywordManagerProps) {
  const [keywords, setKeywords] = useState<CategoryKeyword[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AddKeywordFormValues>({
    resolver: zodResolver(addKeywordSchema),
    defaultValues: { keyword: '' },
  })

  const reload = useCallback(async () => {
    try {
      const list = await categoriesApi.fetchCategoryKeywords(category.id)
      setKeywords(list)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar as palavras-chave.')
    }
  }, [category.id])

  useEffect(() => {
    void reload()
  }, [reload])

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await categoriesApi.createCategoryKeyword(category.id, values.keyword)
      reset({ keyword: '' })
      await reload()
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        setFormError('Essa palavra-chave já está associada a outra categoria sua.')
      } else {
        setFormError('Não foi possível adicionar a palavra-chave.')
      }
    }
  })

  const handleRemove = async (keyword: CategoryKeyword) => {
    await categoriesApi.deleteCategoryKeyword(category.id, keyword.id)
    await reload()
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Transações cuja descrição contiver uma dessas palavras serão categorizadas automaticamente como{' '}
        <span className="font-medium text-foreground">{category.name}</span> em changes futuras.
      </p>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex flex-wrap gap-2">
        {keywords.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhuma palavra-chave cadastrada.</p>
        ) : (
          keywords.map((keyword) => (
            <span
              key={keyword.id}
              className="flex items-center gap-2 rounded-full border border-input px-3 py-1 text-sm"
            >
              {keyword.keyword}
              <button
                type="button"
                className="text-muted-foreground hover:text-destructive"
                onClick={() => void handleRemove(keyword)}
                aria-label={`Remover ${keyword.keyword}`}
              >
                ×
              </button>
            </span>
          ))
        )}
      </div>

      <form className="flex items-end gap-2" onSubmit={submit} noValidate>
        <div className="flex flex-1 flex-col gap-1.5">
          <Input
            placeholder="Ex: IFOOD"
            aria-invalid={Boolean(errors.keyword)}
            {...register('keyword')}
          />
          {errors.keyword ? <p className="text-sm text-destructive">{errors.keyword.message}</p> : null}
        </div>
        <Button type="submit" disabled={isSubmitting}>
          Adicionar
        </Button>
      </form>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end">
        <Button type="button" variant="outline" onClick={onClose}>
          Fechar
        </Button>
      </div>
    </div>
  )
}
