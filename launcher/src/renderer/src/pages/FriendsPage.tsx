import { useCallback, useEffect, useState } from 'react'
import { UserPlus, Users } from 'lucide-react'
import type { FriendEntry } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Button } from '@renderer/design-system/components'

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
  const [friends, setFriends] = useState<FriendEntry[]>([])
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setFriends(await window.primeLauncher.friends.list())
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handleAdd() {
    const username = prompt('Minecraft username (3–16 chars):')
    if (!username) {
      return
    }
    const note = prompt('Optional note:') ?? undefined
    const result = await window.primeLauncher.friends.add(username, note)
    if (result.ok) {
      setError(null)
      await refresh()
    } else {
      setError(result.error ?? 'Could not add friend.')
    }
  }

  async function handleRemove(friendId: string) {
    await window.primeLauncher.friends.remove(friendId)
    await refresh()
  }

  return (
    <PageShell
      title="Friends"
      subtitle="Local roster only — no live presence API. Track names and notes yourself."
      actions={
        <Button variant="primary" icon={<UserPlus size={16} />} onClick={() => void handleAdd()}>
          Add Friend
        </Button>
      }
    >
      {error && <p className="text-caption" style={{ color: 'var(--prime-error)', marginBottom: 12 }}>{error}</p>}

      {friends.length === 0 ? (
        <p className="text-caption">No friends yet. Add Minecraft usernames you play with.</p>
      ) : (
        <div className="page-list">
          {friends.map((friend) => (
            <div key={friend.id} className="list-row">
              <Avatar alt={friend.username} size="sm" />
              <div className={`status-dot ${statusDot(friend.status)}`} style={{ marginLeft: -8 }} />
              <div className="list-row__body">
                <div className="list-row__title">{friend.username}</div>
                <div className="list-row__desc">{friend.activity ?? 'Offline'}</div>
              </div>
              <div className="list-row__meta">
                <Button variant="ghost" size="sm" onClick={() => void handleRemove(friend.id)}>
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </PageShell>
  )
}
