import { Auth } from 'msmc'
import { Authenticator } from 'minecraft-launcher-core'
import type { StoredMinecraftAccount } from '../storage/account-types'
import { offlineUuid } from '../auth/offline'

type MclcUser = Awaited<ReturnType<typeof Authenticator.getAuth>>

export async function resolveLaunchAuthorization(
  account: StoredMinecraftAccount
): Promise<MclcUser> {
  if (account.type === 'offline') {
    const uuid = account.uuid || offlineUuid(account.username)
    return {
      access_token: 'null',
      client_token: 'null',
      uuid: uuid.replace(/-/g, ''),
      name: account.username,
      user_properties: {}
    } as MclcUser
  }

  if (!account.msRefreshToken) {
    throw new Error('Microsoft session missing. Sign in again from Accounts.')
  }

  const authManager = new Auth()
  const xbox = await authManager.refresh(account.msRefreshToken)
  const mc = await xbox.getMinecraft()
  return mc.mclc(true) as MclcUser
}
