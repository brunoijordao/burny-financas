import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Category } from '@/features/categories/api/categoriesApi'
import { CategoryIcon } from '@/features/categories/icons'

interface CategoryListProps {
  categories: Category[]
  onEdit: (category: Category) => void
  onDelete: (category: Category) => void
  onManageKeywords: (category: Category) => void
}

export function CategoryList({ categories, onEdit, onDelete, onManageKeywords }: CategoryListProps) {
  if (categories.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma categoria cadastrada ainda.</p>
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {categories.map((category) => (
        <Card key={category.id}>
          <CardHeader className="flex flex-row items-center gap-3 space-y-0">
            <span
              className="flex size-9 shrink-0 items-center justify-center rounded-full"
              style={{ backgroundColor: category.color + '33', color: category.color }}
            >
              <CategoryIcon icon={category.icon} className="size-5" />
            </span>
            <div className="flex flex-col">
              <CardTitle>{category.name}</CardTitle>
              {category.defaultCategory ? (
                <span className="text-xs text-muted-foreground">Categoria padrão</span>
              ) : null}
            </div>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {category.subcategories.length > 0 ? (
              <div className="flex flex-col gap-1">
                {category.subcategories.map((subcategory) => (
                  <div key={subcategory.id} className="flex items-center justify-between gap-2 pl-2 text-sm">
                    <span className="flex items-center gap-2">
                      <CategoryIcon icon={subcategory.icon} className="size-4" />
                      {subcategory.name}
                    </span>
                    <div className="flex gap-1">
                      <Button variant="ghost" size="sm" onClick={() => onEdit(subcategory)}>
                        Editar
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => onDelete(subcategory)}>
                        Excluir
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}

            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => onManageKeywords(category)}>
                Palavras-chave
              </Button>
              <Button variant="outline" size="sm" onClick={() => onEdit(category)}>
                Editar
              </Button>
              <Button variant="ghost" size="sm" onClick={() => onDelete(category)}>
                Excluir
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
