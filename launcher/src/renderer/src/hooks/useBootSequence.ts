import { useEffect, useState } from 'react'

export function useBootSequence() {
  const [booting, setBooting] = useState(true)
  const [stepIndex, setStepIndex] = useState(0)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    let cancelled = false
    let step = 0
    let finishTimer: ReturnType<typeof setTimeout> | null = null
    const timer = setInterval(() => {
      if (step < 3) {
        step += 1
        setStepIndex(step)
        setProgress(Math.round((step / 3) * 90))
      }
    }, 500)

    void (async () => {
      try {
        await window.primeLauncher.boot.initialize()
      } finally {
        clearInterval(timer)
        if (!cancelled) {
          setStepIndex(3)
          setProgress(100)
          finishTimer = setTimeout(() => {
            if (!cancelled) setBooting(false)
          }, 300)
        }
      }
    })()

    return () => {
      cancelled = true
      clearInterval(timer)
      if (finishTimer) clearTimeout(finishTimer)
    }
  }, [])

  return { booting, stepIndex, progress }
}
