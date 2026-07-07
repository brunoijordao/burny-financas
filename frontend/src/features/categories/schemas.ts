import { z } from 'zod'

export const createCategorySchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório'),
  icon: z.string().min(1, 'Ícone é obrigatório'),
  color: z.string().min(1, 'Cor é obrigatória'),
  parentCategoryId: z.string().optional(),
})

export type CreateCategoryFormValues = z.infer<typeof createCategorySchema>

export const editCategorySchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório'),
  icon: z.string().min(1, 'Ícone é obrigatório'),
  color: z.string().min(1, 'Cor é obrigatória'),
})

export type EditCategoryFormValues = z.infer<typeof editCategorySchema>

export const addKeywordSchema = z.object({
  keyword: z.string().min(1, 'Palavra-chave é obrigatória'),
})

export type AddKeywordFormValues = z.infer<typeof addKeywordSchema>
