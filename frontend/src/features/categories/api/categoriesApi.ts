import { httpClient } from '@/lib/httpClient'

export interface Category {
  id: number
  name: string
  icon: string
  color: string
  parentCategoryId: number | null
  defaultCategory: boolean
  active: boolean
  subcategories: Category[]
  createdAt: string
  updatedAt: string
}

export interface CategoryKeyword {
  id: number
  categoryId: number
  keyword: string
  createdAt: string
}

export interface CreateCategoryPayload {
  name: string
  icon: string
  color: string
  parentCategoryId?: number
}

export interface UpdateCategoryPayload {
  name: string
  icon: string
  color: string
}

/** POST /categories */
export async function createCategory(payload: CreateCategoryPayload): Promise<Category> {
  const response = await httpClient.post<Category>('/categories', payload)
  return response.data
}

/** GET /categories -> the caller's active categories, with active subcategories nested */
export async function fetchCategories(): Promise<Category[]> {
  const response = await httpClient.get<Category[]>('/categories')
  return response.data
}

/** PUT /categories/{id} */
export async function updateCategory(id: number, payload: UpdateCategoryPayload): Promise<Category> {
  const response = await httpClient.put<Category>(`/categories/${id}`, payload)
  return response.data
}

/** DELETE /categories/{id}, soft-deleted; cascades to subcategories server-side if top-level */
export async function deleteCategory(id: number): Promise<void> {
  await httpClient.delete(`/categories/${id}`)
}

/** GET /categories/{id}/keywords */
export async function fetchCategoryKeywords(categoryId: number): Promise<CategoryKeyword[]> {
  const response = await httpClient.get<CategoryKeyword[]>(`/categories/${categoryId}/keywords`)
  return response.data
}

/** POST /categories/{id}/keywords */
export async function createCategoryKeyword(categoryId: number, keyword: string): Promise<CategoryKeyword> {
  const response = await httpClient.post<CategoryKeyword>(`/categories/${categoryId}/keywords`, { keyword })
  return response.data
}

/** DELETE /categories/{id}/keywords/{keywordId} */
export async function deleteCategoryKeyword(categoryId: number, keywordId: number): Promise<void> {
  await httpClient.delete(`/categories/${categoryId}/keywords/${keywordId}`)
}
