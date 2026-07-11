import { useCallback, useEffect, useState } from 'react'
import { UserPlus, Users, RefreshCw } from 'lucide-react'
import type { FriendEntry } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'

function statusDot(status: FriendEntry['status']): string {
  switch (status) {
    case 'online':
      return 'status-dot--online'
    case 'away':
      return 'status-dot--away'
    case 'in-game':
      return 'status-dot--ingame'
    default:
      return 'status-dot--offline'
  }
}

export function FriendsPage() {
  const { t } = useI18n()
  const [friends, setFriends] = useState<FriendEntry[]>([])
  const [error, setError] = useState<string | null>(null)
  const [username, setUsername] = useState('')
  const [note, setNote] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editNote, setEditNote] = useState('')

  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    setFriends(await window.primeLauncher.friends.list())
  }, [])

  async function handleRefreshAll() {
    setBusy(true)
    setFriends(await window.primeLauncher.friends.refreshAll())
    setBusy(false)
  }

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handleAdd() {
    if (username.trim().length < 3) {
      return
    }
    const result = await window.primeLauncher.friends.add(username, note || undefined)
    if (result.ok) {
      setError(null)
      setUsername('')
      setNote('')
      await refresh()
    } else {
      setError(result.error ?? t('friends.addFailed'))
    }
  }

  async function handleRemove(friendId: string) {
    await window.primeLauncher.friends.remove(friendId)
    await refresh()
  }

  async function handleSaveNote(friendId: string) {
    await window.primeLauncher.friends.updateNote(friendId, editNote)
    setEditingId(null)
    await refresh()
  }

  return (
    <PageShell
      title={t('pages.friends.title')}
      subtitle={t('pages.friends.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} disabled={busy} onClick={() => void handleRefreshAll()}>
          {t('friends.refreshStatus')}
        </Button>
      }
    >
      <div className="page-grid page-grid--2" style={{ marginBottom: 16 }}>
        <input
          className="modal__field"
          placeholder={t('friends.usernamePrompt')}
          value={username}
          maxLength={16}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          className="modal__field"
          placeholder={t('friends.notePrompt')}
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
      </div>
      <Button
        variant="primary"
        icon={<UserPlus size={16} />}
        style={{ marginBottom: 16 }}
        disabled={username.trim().length < 3}
        onClick={() => void handleAdd()}
      >
        {t('friends.addFriend')}
      </Button>

      {error && (
        <p className="text-caption" style={{ color: 'var(--prime-error)', marginBottom: 12 }}>
          {error}
        </p>
      )}

      {friends.length === 0 ? (
        <p className="text-caption">{t('friends.empty')}</p>
      ) : (
        <div className="page-list">
          {friends.map((friend) => (
            <div key={friend.id} className="list-row">
              <Avatar alt={friend.username} size="sm" />
              <div className={`status-dot ${statusDot(friend.status)}`} style={{ marginLeft: -8 }} />
              <div className="list-row__body">
                <div className="list-row__title">{friend.username}</div>
                {editingId === friend.id ? (
                  <input
                    className="modal__field"
                    style={{ marginTop: 4 }}
                    value={editNote}
                    onChange={(e) => setEditNote(e.target.value)}
                    placeholder={t('friends.notePlaceholder')}
                  />
                ) : (
                  <div className="list-row__desc">{friend.activity ?? t('friends.offline')}</div>
                )}
              </div>
              <div className="list-row__meta">
                {editingId === friend.id ? (
                  <Button variant="secondary" size="sm" onClick={() => void handleSaveNote(friend.id)}>
                    {t('friends.saveNote')}
                  </Button>
                ) : (
                  <Button variant="ghost" size="sm" onClick={() => { setEditingId(friend.id); setEditNote(friend.activity ?? '') }}>
                    {t('actions.save')}
                  </Button>
                )}
                <Button variant="ghost" size="sm" onClick={() => void handleRemove(friend.id)}>
                  {t('friends.remove')}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </PageShell>
  )
}
