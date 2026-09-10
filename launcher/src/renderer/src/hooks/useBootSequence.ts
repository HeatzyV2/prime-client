import { useEffect, useState } from 'react'
import { BOOT_STEPS } from '@shared/types'

/** Boot splash driven by real BootService progress events. */
export function useBootSequence() {
  const [booting, setBooting] = useState(true)
  const [stepIndex, setStepIndex] = useState(0)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    let cancelled = false
    let finishTimer: ReturnType<typeof setTimeout> | null = null

    const unsubscribe = window.primeLauncher.boot.onProgress?.((payload) => {
      if (cancelled) return
      const pct = Math.round((payload.step / Math.max(1, payload.total)) * 100)
      setProgress(pct)
      // Map store steps onto the 4 splash labels.
      const mapped = Math.min(BOOT_STEPS.length - 1, Math.floor(((payload.step - 1) / payload.total) * BOOT_STEPS.length))
      setStepIndex(Math.max(0, mapped))
    })

    void (async () => {
      try {
        setStepIndex(0)
        setProgress(8)
        await window.primeLauncher.boot.initialize()
        if (!cancelled) {
          setStepIndex(BOOT_STEPS.length - 1)
          setProgress(100)
        }
      } catch {
        if (!cancelled) {
          setStepIndex(BOOT_STEPS.length - 1)
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
      unsubscribe?.()
      if (finishTimer) clearTimeout(finishTimer)
    }
  }, [])

  return { booting, stepIndex, progress }
}
