import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { AuthResultDto, LaunchProgressDto, LaunchResultDto, SyncResultDto } from '@shared/ipc'
import type { LauncherProfile, MinecraftAccount, PrimeAccount } from '@shared/types'

interface AccountContextValue {
  loading: boolean
  prime: PrimeAccount | null
  accounts: MinecraftAccount[]
  activeAccount: MinecraftAccount | null
  profile: LauncherProfile | null
  launchMessage: string | null
  launchProgress: LaunchProgressDto | null
  refresh: () => Promise<void>
  loginMicrosoft: () => Promise<AuthResultDto>
  addOffline: (username: string) => Promise<AuthResultDto>
  removeAccount: (id: string) => Promise<AuthResultDto>
  setActive: (id: string) => Promise<void>
  syncPrime: () => Promise<SyncResultDto>
  launch: (instanceId: string) => Promise<LaunchResultDto>
  clearLaunchMessage: () => void
  clearLaunchProgress: () => void
}

const AccountContext = createContext<AccountContextValue | null>(null)

export function AccountProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true)
  const [prime, setPrime] = useState<PrimeAccount | null>(null)
  const [accounts, setAccounts] = useState<MinecraftAccount[]>([])
  const [activeAccount, setActiveAccount] = useState<MinecraftAccount | null>(null)
  const [profile, setProfile] = useState<LauncherProfile | null>(null)
  const [launchMessage, setLaunchMessage] = useState<string | null>(null)
  const [launchProgress, setLaunchProgress] = useState<LaunchProgressDto | null>(null)

  useEffect(() => {
    const unsubscribe = window.primeLauncher.launch.onProgress((payload) => {
      setLaunchProgress(payload)
    })
    return unsubscribe
  }, [])

  const refresh = useCallback(async () => {
    const [p, list, active, prof] = await Promise.all([
      window.primeLauncher.account.getPrime(),
      window.primeLauncher.account.getMinecraft(),
      window.primeLauncher.account.getActive(),
      window.primeLauncher.profile.getActive()
    ])
    setPrime(p)
    setAccounts(list)
    setActiveAccount(active)
    setProfile(prof)
  }, [])

  useEffect(() => {
    void (async () => {
      try {
        await refresh()
      } finally {
        setLoading(false)
      }
    })()
  }, [refresh])

  const loginMicrosoft = useCallback(async () => {
    const result = await window.primeLauncher.account.loginMicrosoft()
    if (result.ok) {
      await refresh()
    }
    return result
  }, [refresh])

  const addOffline = useCallback(
    async (username: string) => {
      const result = await window.primeLauncher.account.addOffline(username)
      if (result.ok) {
        await refresh()
      }
      return result
    },
    [refresh]
  )

  const removeAccount = useCallback(
    async (id: string) => {
      const result = await window.primeLauncher.account.remove(id)
      if (result.ok) {
        await refresh()
      }
      return result
    },
    [refresh]
  )

  const setActive = useCallback(
    async (id: string) => {
      await window.primeLauncher.account.setActive(id)
      await refresh()
    },
    [refresh]
  )

  const syncPrime = useCallback(async () => {
    const result = await window.primeLauncher.account.syncPrime()
    if (result.ok) {
      await refresh()
    }
    return result
  }, [refresh])

  const launch = useCallback(
    async (instanceId: string) => {
      const result = await window.primeLauncher.launch.game(instanceId)
      setLaunchMessage(result.message)
      if (result.ok) {
        await refresh()
      }
      return result
    },
    [refresh]
  )

  const value = useMemo(
    () => ({
      loading,
      prime,
      accounts,
      activeAccount,
      profile,
      launchMessage,
      launchProgress,
      refresh,
      loginMicrosoft,
      addOffline,
      removeAccount,
      setActive,
      syncPrime,
      launch,
      clearLaunchMessage: () => setLaunchMessage(null),
      clearLaunchProgress: () => setLaunchProgress(null)
    }),
    [
      loading,
      prime,
      accounts,
      activeAccount,
      profile,
      launchMessage,
      launchProgress,
      refresh,
      loginMicrosoft,
      addOffline,
      removeAccount,
      setActive,
      syncPrime,
      launch
    ]
  )

  return <AccountContext.Provider value={value}>{children}</AccountContext.Provider>
}

export function useAccounts(): AccountContextValue {
  const ctx = useContext(AccountContext)
  if (!ctx) {
    throw new Error('useAccounts must be used within AccountProvider')
  }
  return ctx
}
