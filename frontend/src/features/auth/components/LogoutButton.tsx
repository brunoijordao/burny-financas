import { Button } from '@/components/ui/button'
import { useAuthActions } from '@/features/auth/hooks/useAuthActions'

export function LogoutButton() {
  const { logout } = useAuthActions()

  return (
    <Button variant="outline" onClick={() => void logout()}>
      Sair
    </Button>
  )
}
