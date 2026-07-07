import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import { AccountList } from '@/features/accounts/components/AccountList'
import { CreateAccountForm } from '@/features/accounts/components/CreateAccountForm'
import { EditAccountForm } from '@/features/accounts/components/EditAccountForm'
import { TransferForm } from '@/features/accounts/components/TransferForm'
import { LogoutButton } from '@/features/auth/components/LogoutButton'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

type Panel = 'none' | 'create' | 'transfer' | { edit: Account }

export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [consolidatedBalance, setConsolidatedBalance] = useState<number | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [panel, setPanel] = useState<Panel>('none')

  const reload = useCallback(async () => {
    try {
      const [accountList, balance] = await Promise.all([
        accountsApi.fetchAccounts(),
        accountsApi.fetchConsolidatedBalance(),
      ])
      setAccounts(accountList)
      setConsolidatedBalance(balance.consolidatedBalance)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar suas contas. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const handleDelete = async (account: Account) => {
    const confirmed = window.confirm(`Tem certeza que deseja excluir a conta "${account.name}"?`)
    if (!confirmed) {
      return
    }
    await accountsApi.deleteAccount(account.id)
    await reload()
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Suas contas</h1>
          <p className="text-muted-foreground">
            Saldo consolidado:{' '}
            <span className="font-medium text-foreground">
              {consolidatedBalance !== null ? formatCurrency(consolidatedBalance) : '...'}
            </span>
          </p>
        </div>
        <LogoutButton />
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      <div className="flex gap-2">
        <Button onClick={() => setPanel('create')}>Nova conta</Button>
        <Button variant="outline" onClick={() => setPanel('transfer')} disabled={accounts.length < 2}>
          Transferir
        </Button>
      </div>

      {panel === 'create' ? (
        <Card>
          <CardHeader>
            <CardTitle>Nova conta</CardTitle>
            <CardDescription>Cadastre uma nova conta, carteira ou cartão de crédito.</CardDescription>
          </CardHeader>
          <CardContent>
            <CreateAccountForm
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await accountsApi.createAccount({
                  name: values.name,
                  icon: values.icon,
                  color: values.color,
                  type: values.type,
                  creditLimit: values.creditLimit ? Number(values.creditLimit) : undefined,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      {panel === 'transfer' ? (
        <Card>
          <CardHeader>
            <CardTitle>Transferir entre contas</CardTitle>
            <CardDescription>
              Uma conta de cartão de crédito só pode ser destino (pagamento de fatura).
            </CardDescription>
          </CardHeader>
          <CardContent>
            <TransferForm
              accounts={accounts}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await accountsApi.transferBetweenAccounts({
                  sourceAccountId: Number(values.sourceAccountId),
                  destinationAccountId: Number(values.destinationAccountId),
                  amount: Number(values.amount),
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
            <CardTitle>Editar conta</CardTitle>
          </CardHeader>
          <CardContent>
            <EditAccountForm
              account={panel.edit}
              onCancel={() => setPanel('none')}
              onSubmit={async (values) => {
                await accountsApi.updateAccount(panel.edit.id, {
                  name: values.name,
                  icon: values.icon,
                  color: values.color,
                  creditLimit: values.creditLimit ? Number(values.creditLimit) : undefined,
                })
                setPanel('none')
                await reload()
              }}
            />
          </CardContent>
        </Card>
      ) : null}

      <AccountList
        accounts={accounts}
        onEdit={(account) => setPanel({ edit: account })}
        onDelete={handleDelete}
      />
    </div>
  )
}
