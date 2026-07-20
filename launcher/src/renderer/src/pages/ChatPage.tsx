import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { RefreshCw, ImagePlus, Send } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'

interface Conversation {
  id: string
  participants: { uuid: string; username: string }[]
  updatedAt: string
}

interface ChatMessage {
  id: string
  conversationId?: string
  senderUuid: string
  senderUsername?: string | null
  text: string
  imageUrl: string | null
  createdAt: string
}

function displayName(
  message: ChatMessage,
  participants: { uuid: string; username: string }[] | undefined
): string {
  if (message.senderUsername && message.senderUsername.trim()) {
    return message.senderUsername.trim()
  }
  const fromParticipants = participants?.find((p) => p.uuid === message.senderUuid)?.username
  if (fromParticipants && fromParticipants.trim()) {
    return fromParticipants.trim()
  }
  return message.senderUuid.slice(0, 8)
}

export function ChatPage() {
  const { t } = useI18n()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const activeIdRef = useRef<string | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    activeIdRef.current = activeId
  }, [activeId])

  const reload = useCallback(async () => {
    try {
      await window.primeLauncher.social.connect()
      const list = (await window.primeLauncher.chat.conversations()) as Conversation[]
      setConversations(list)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chat unavailable')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  useEffect(() => {
    if (!activeId) {
      setMessages([])
      return
    }
    void window.primeLauncher.chat.messages(activeId).then((list) => {
      setMessages(list as ChatMessage[])
    })
  }, [activeId])

  // Live messages via social WebSocket
  useEffect(() => {
    const unsub = window.primeLauncher.social.onEvent((event) => {
      if (event.t !== 'message' || !event.message || typeof event.message !== 'object') {
        return
      }
      const incoming = event.message as ChatMessage
      if (!incoming.id || !incoming.conversationId) {
        return
      }
      // Refresh conversation list ordering when any DM arrives
      void window.primeLauncher.chat.conversations().then((list) => {
        setConversations(list as Conversation[])
      })
      if (incoming.conversationId !== activeIdRef.current) {
        return
      }
      setMessages((prev) => {
        if (prev.some((m) => m.id === incoming.id)) {
          return prev
        }
        return [...prev, incoming]
      })
    })
    return unsub
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length])

  const active = useMemo(
    () => conversations.find((c) => c.id === activeId) ?? null,
    [conversations, activeId]
  )

  async function send(): Promise<void> {
    if (!activeId || !text.trim()) return
    setBusy(true)
    try {
      const sent = (await window.primeLauncher.chat.send(activeId, text.trim())) as ChatMessage
      setText('')
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) {
          return prev
        }
        return [...prev, sent]
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Send failed')
    } finally {
      setBusy(false)
    }
  }

  async function attachImage(): Promise<void> {
    if (!activeId) return
    const result = await window.primeLauncher.dialog?.openFile?.({
      filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp', 'gif'] }]
    })
    const filePath =
      typeof result === 'string'
        ? result
        : (result as { filePaths?: string[] } | null)?.filePaths?.[0]
    const path =
      filePath ||
      (() => {
        const manual = window.prompt(t('chat.imagePathPrompt'))
        return manual || null
      })()
    if (!path) return
    setBusy(true)
    try {
      const url = await window.primeLauncher.chat.upload(path)
      const sent = (await window.primeLauncher.chat.send(
        activeId,
        text.trim() || '',
        url
      )) as ChatMessage
      setText('')
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) {
          return prev
        }
        return [...prev, sent]
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <PageShell
      title={t('pages.chat.title')}
      subtitle={t('pages.chat.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} onClick={() => void reload()}>
          {t('chat.refresh')}
        </Button>
      }
    >
      {error ? (
        <p className="text-caption" style={{ color: 'var(--prime-error)', marginBottom: 12 }}>
          {error}
        </p>
      ) : null}

      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: 16, minHeight: 420 }}>
        <div className="card" style={{ padding: 12, overflow: 'auto' }}>
          <div className="text-caption" style={{ marginBottom: 8 }}>
            {t('chat.conversations')}
          </div>
          {conversations.length === 0 ? (
            <p className="text-caption">{t('chat.empty')}</p>
          ) : (
            conversations.map((c) => {
              const name = c.participants?.map((p) => p.username).join(', ') || c.id
              return (
                <Button
                  key={c.id}
                  variant="ghost"
                  size="sm"
                  style={{
                    display: 'block',
                    width: '100%',
                    textAlign: 'left',
                    marginBottom: 4,
                    opacity: activeId === c.id ? 1 : 0.75
                  }}
                  onClick={() => setActiveId(c.id)}
                >
                  {name}
                </Button>
              )
            })
          )}
        </div>

        <div className="card" style={{ padding: 12, display: 'flex', flexDirection: 'column', minHeight: 420 }}>
          <div className="text-caption" style={{ marginBottom: 8 }}>
            {active
              ? active.participants?.map((p) => p.username).join(', ')
              : t('chat.selectConversation')}
          </div>
          <div style={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {messages.map((m) => (
              <div key={m.id} style={{ maxWidth: '80%' }}>
                <div className="text-caption">
                  {displayName(m, active?.participants)} ·{' '}
                  {new Date(m.createdAt).toLocaleString()}
                </div>
                {m.text ? <div>{m.text}</div> : null}
                {m.imageUrl ? (
                  <img
                    src={m.imageUrl}
                    alt=""
                    style={{ maxWidth: 280, maxHeight: 200, borderRadius: 8, marginTop: 4 }}
                  />
                ) : null}
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <input
              className="modal__field"
              style={{ flex: 1 }}
              value={text}
              disabled={!activeId || busy}
              placeholder={t('chat.messagePlaceholder')}
              onChange={(e) => setText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') void send()
              }}
            />
            <Button
              variant="ghost"
              size="sm"
              icon={<ImagePlus size={14} />}
              disabled={!activeId || busy}
              onClick={() => void attachImage()}
            >
              {t('chat.image')}
            </Button>
            <Button
              variant="primary"
              size="sm"
              icon={<Send size={14} />}
              disabled={!activeId || busy}
              onClick={() => void send()}
            >
              {t('chat.send')}
            </Button>
          </div>
        </div>
      </div>
    </PageShell>
  )
}
