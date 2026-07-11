import { useState } from 'react'
import { motion } from 'framer-motion'
import { KeyRound, User } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import './LoginModal.css'

interface LoginModalProps {
  onClose: () => void
}

export function LoginModal({ onClose }: LoginModalProps) {
  const { loginMicrosoft, addOffline } = useAccounts()
  const [username, setUsername] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleMicrosoft() {
    setBusy(true)
    setError(null)
    const result = await loginMicrosoft()
    setBusy(false)
    if (result.ok) {
      onClose()
    } else {
      setError(result.error ?? 'Login failed.')
    }
  }

  async function handleOffline() {
    setBusy(true)
    setError(null)
    const result = await addOffline(username)
    setBusy(false)
    if (result.ok) {
      onClose()
    } else {
      setError(result.error ?? 'Could not create offline account.')
    }
  }

  return (
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="modal"
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">Sign in to Prime</h2>
        <p className="modal__subtitle">
          Use your Microsoft account for official Minecraft, or play offline with a custom username.
        </p>

        <div className="modal__actions">
          <Button
            variant="primary"
            size="lg"
            block
            icon={<KeyRound size={18} />}
            disabled={busy}
            onClick={() => void handleMicrosoft()}
          >
            Sign in with Microsoft
          </Button>
        </div>

        <p className="text-caption" style={{ textAlign: 'center', margin: '20px 0 12px' }}>
          — or offline —
        </p>

        <input
          className="modal__field"
          placeholder="Offline username"
          value={username}
          maxLength={16}
          disabled={busy}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && void handleOffline()}
        />
        <Button
          variant="secondary"
          size="md"
          block
          icon={<User size={16} />}
          disabled={busy || username.trim().length < 3}
          onClick={() => void handleOffline()}
        >
          Continue Offline
        </Button>

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" size="sm" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
