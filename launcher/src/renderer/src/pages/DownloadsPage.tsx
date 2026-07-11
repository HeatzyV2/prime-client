import { useCallback, useEffect, useState } from 'react'
import { Trash2, X } from 'lucide-react'
import type { DownloadTask } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, ProgressBar } from '@renderer/design-system/components'

const STATUS_VARIANT: Record<string, 'default' | 'red' | 'success' | 'prime'> = {
  downloading: 'red',
  paused: 'default',
  completed: 'success',
  queued: 'default'
}

export function DownloadsPage() {
  const [tasks, setTasks] = useState<DownloadTask[]>([])

  const refresh = useCallback(async () => {
    setTasks(await window.primeLauncher.downloads.list())
  }, [])

  useEffect(() => {
    void refresh()
    const unsubscribe = window.primeLauncher.launch.onProgress(() => {
      void refresh()
    })
    const timer = setInterval(() => void refresh(), 3000)
    return () => {
      unsubscribe()
      clearInterval(timer)
    }
  }, [refresh])

  return (
    <PageShell
      title="Download Center"
      subtitle="Launch and CDN progress saved locally during Minecraft startup."
      actions={
        <Button variant="secondary" size="sm" icon={<Trash2 size={14} />} onClick={() => void window.primeLauncher.downloads.clearCompleted().then(refresh)}>
          Clear completed
        </Button>
      }
    >
      {tasks.length === 0 ? (
        <p className="text-caption">No recent downloads. Launch Minecraft to see progress here.</p>
      ) : (
        <div className="page-list">
          {tasks.map((task) => (
            <div key={task.id} className="list-row" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div className="list-row__body">
                  <div className="list-row__title">{task.name}</div>
                  <div className="list-row__desc">{task.size}</div>
                </div>
                <Badge variant={STATUS_VARIANT[task.status] ?? 'default'}>{task.status}</Badge>
                <span className="text-mono" style={{ color: 'var(--prime-muted)' }}>
                  {task.speed}
                </span>
                <span className="text-mono" style={{ color: 'var(--prime-muted-2)' }}>
                  {task.eta}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="prime-btn--icon"
                  onClick={() => void window.primeLauncher.downloads.remove(task.id).then(refresh)}
                >
                  <X size={14} />
                </Button>
              </div>
              <ProgressBar value={task.progress} large />
            </div>
          ))}
        </div>
      )}
    </PageShell>
  )
}
