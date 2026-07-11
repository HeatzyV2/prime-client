import { useEffect, useState } from 'react'
import { BOOT_STEPS } from '@shared/types'

const STEP_DURATION_MS = 900

export function useBootSequence() {
  const [booting, setBooting] = useState(true)
  const [stepIndex, setStepIndex] = useState(0)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    let step = 0
    let frame: number
    let stepStart = performance.now()

    const tick = (now: number) => {
      const elapsed = now - stepStart
      const stepProgress = Math.min(1, elapsed / STEP_DURATION_MS)
      const base = (step / BOOT_STEPS.length) * 100
      const current = base + stepProgress * (100 / BOOT_STEPS.length)
      setProgress(current)
      setStepIndex(step)

      if (stepProgress >= 1) {
        step++
        stepStart = now
        if (step >= BOOT_STEPS.length) {
          setProgress(100)
          setTimeout(() => setBooting(false), 400)
          return
        }
      }
      frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [])

  return { booting, stepIndex, progress }
}
