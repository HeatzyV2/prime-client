import { useCallback, useEffect, useState } from 'react'
import { Camera, Film } from 'lucide-react'
import type { MediaItem } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'

export function MediaPage() {
  const { instance, instanceId } = useActiveInstance()
  const [items, setItems] = useState<MediaItem[]>([])

  const refresh = useCallback(async () => {
    setItems(await window.primeLauncher.media.list(instanceId ?? undefined))
  }, [instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return (
    <PageShell
      title="Media Center"
      subtitle={
        instance
          ? `Screenshots from ${instance.name}/screenshots — taken in-game with F2.`
          : 'Loading instance…'
      }
      actions={
        <Button variant="secondary" size="sm" onClick={() => void window.primeLauncher.media.openFolder(instanceId ?? undefined)}>
          Open Folder
        </Button>
      }
    >
      {items.length === 0 ? (
        <p className="text-caption">No screenshots yet. Press F2 in Minecraft to capture.</p>
      ) : (
        <div className="page-grid page-grid--3">
          {items.map((item) => (
            <div
              key={item.id}
              className="tile"
              role="button"
              tabIndex={0}
              onClick={() => item.filePath && void window.primeLauncher.media.openFile(item.filePath)}
            >
              <div className="tile__preview">
                <Camera size={28} />
              </div>
              <div className="tile__name">{item.title}</div>
              <div className="tile__desc">
                {item.date} · {item.size}
              </div>
              <div style={{ marginTop: 12 }}>
                <Badge variant="default">{item.type}</Badge>
              </div>
            </div>
          ))}
        </div>
      )}

      <p className="text-caption" style={{ marginTop: 24 }}>
        Replays and clips will appear here when Prime Client exports them locally.
        <Film size={14} style={{ verticalAlign: 'middle', marginLeft: 6 }} />
      </p>
    </PageShell>
  )
}
