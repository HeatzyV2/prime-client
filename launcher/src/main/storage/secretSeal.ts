import { safeStorage } from 'electron'

const ENC_PREFIX = 'enc:'

/** Encrypt a secret with OS keychain-backed safeStorage when available. */
export function sealSecret(plain: string): string {
  if (!plain) {
    return plain
  }
  try {
    if (safeStorage.isEncryptionAvailable()) {
      return ENC_PREFIX + safeStorage.encryptString(plain).toString('base64')
    }
  } catch {
    // fall through to plaintext (dev / unsupported platforms)
  }
  return plain
}

/** Decrypt a value previously sealed with {@link sealSecret}; plaintext passthrough. */
export function openSecret(value: string | undefined): string | undefined {
  if (!value) {
    return value
  }
  if (!value.startsWith(ENC_PREFIX)) {
    return value
  }
  try {
    if (safeStorage.isEncryptionAvailable()) {
      return safeStorage.decryptString(Buffer.from(value.slice(ENC_PREFIX.length), 'base64'))
    }
  } catch {
    return undefined
  }
  return undefined
}
