import { motion } from 'framer-motion'
import { PrimeLogo, ProgressBar } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { BOOT_STEPS } from '@shared/types'
import './SplashScreen.css'

interface SplashScreenProps {
  progress: number
  stepIndex: number
  version: string
}

export function SplashScreen({ progress, stepIndex, version }: SplashScreenProps) {
  const { t } = useI18n()
  const { reduceMotion } = useTheme()
  const step = BOOT_STEPS[stepIndex]
  const label = step ? t(`boot.${step.id}`) : ''

  return (
    <motion.div
      className="splash"
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label={label || 'Prime'}
      exit={reduceMotion ? { opacity: 0 } : { opacity: 0 }}
      transition={{ duration: reduceMotion ? 0.08 : 0.22, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="splash__bg" aria-hidden />

      <div className="splash__content">
        <div className="splash__brand">
          <PrimeLogo size={72} />
          <p className="splash__mark">Prime</p>
        </div>

        <div className="splash__status">
          <p className="splash__step">{label}</p>
          <ProgressBar value={progress} large />
        </div>
      </div>

      <span className="splash__version">v{version}</span>
    </motion.div>
  )
}
