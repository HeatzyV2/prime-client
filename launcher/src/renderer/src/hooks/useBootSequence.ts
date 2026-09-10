import { useEffect, useState } from 'react'

/** Boot splash driven by the real initialize IPC (no fake progress timer). */
export function useBootSequence() {
  const [booting, setBooting] = useState(true)
  const [stepIndex, setStepIndex] = useState(0)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    let cancelled = false
    let finishTimer: ReturnType<typeof setTimeout> | null = null

    void (async () => {
      try {
        setStepIndex(0)
        setProgress(15)
        await window.primeLauncher.boot.initialize()
        if (!cancelled) {
          setStepIndex(3)
          setProgress(100)
        }
      } catch {
        if (!cancelled) {
          setStepIndex(3)
          setProgress(100)
        }
      } finally {
        if (!cancelled) {
          finishTimer = setTimeout(() => {
            if (!cancelled) setBooting(false)
          }, 200)
        }
      }
    })()

    return () => {
      cancelled = true
      if (finishTimer) clearTimeout(finishTimer)
    }
  }, [])

  return { booting, stepIndex, progress }
}
