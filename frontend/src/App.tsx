import { Navigate, Route, Routes } from 'react-router-dom'

import { RouteGuard } from '@/features/auth/components/RouteGuard'
import { AppLayout } from '@/features/layout/components/AppLayout'
import { AccountsPage } from '@/pages/AccountsPage'
import { BudgetsPage } from '@/pages/BudgetsPage'
import { CategoriesPage } from '@/pages/CategoriesPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { GoalsPage } from '@/pages/GoalsPage'
import { LoginPage } from '@/pages/LoginPage'
import { PdfImportPage } from '@/pages/PdfImportPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { TransactionsPage } from '@/pages/TransactionsPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<RouteGuard />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/accounts" element={<AccountsPage />} />
          <Route path="/categories" element={<CategoriesPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/pdf-imports" element={<PdfImportPage />} />
          <Route path="/budgets" element={<BudgetsPage />} />
          <Route path="/goals" element={<GoalsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
