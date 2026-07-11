import { accountService } from './AccountService'

/** Local profile sync only — no hosted Prime backend. */
export class CloudService {
  async getSyncStatus(): Promise<{ lastSync: string | null; pending: boolean }> {
    return { lastSync: null, pending: false }
  }

  async sync(): Promise<{ ok: boolean; lastSync: string; message: string }> {
    return accountService.syncPrimeCloud()
  }
}

export const cloudService = new CloudService()
