import { Auth } from 'msmc'
import type { StoredMinecraftAccount } from '../storage/account-types'
import { newAccountId, skinUrlForUuid } from '../auth/offline'

export class MicrosoftAuth {
  async login(): Promise<StoredMinecraftAccount> {
    const authManager = new Auth('select_account')
    const xbox = await authManager.launch('electron')
    const mc = await xbox.getMinecraft()
    const profile = mc.profile

    if (!profile?.id || !profile.name) {
      throw new Error('Microsoft account has no Minecraft profile. Purchase Minecraft or use offline mode.')
    }

    const activeSkin = profile.skins?.find((s) => s.state === 'ACTIVE')
    const activeCape = profile.capes?.find((c) => c.state === 'ACTIVE')

    return {
      id: newAccountId(),
      type: 'microsoft',
      username: profile.name,
      uuid: profile.id,
      skinUrl: activeSkin?.url ?? skinUrlForUuid(profile.id),
      capeUrl: activeCape?.url,
      msRefreshToken: xbox.save(),
      addedAt: new Date().toISOString(),
      lastUsedAt: new Date().toISOString()
    }
  }

  async refresh(account: StoredMinecraftAccount): Promise<StoredMinecraftAccount> {
    if (!account.msRefreshToken) {
      throw new Error('Missing Microsoft refresh token.')
    }
    const authManager = new Auth()
    const xbox = await authManager.refresh(account.msRefreshToken)
    const mc = await xbox.getMinecraft()
    const profile = mc.profile

    if (!profile?.id || !profile.name) {
      throw new Error('Could not refresh Minecraft profile.')
    }

    const activeSkin = profile.skins?.find((s) => s.state === 'ACTIVE')
    const activeCape = profile.capes?.find((c) => c.state === 'ACTIVE')

    return {
      ...account,
      username: profile.name,
      uuid: profile.id,
      skinUrl: activeSkin?.url ?? skinUrlForUuid(profile.id),
      capeUrl: activeCape?.url,
      msRefreshToken: xbox.save(),
      lastUsedAt: new Date().toISOString()
    }
  }
}

export const microsoftAuth = new MicrosoftAuth()
