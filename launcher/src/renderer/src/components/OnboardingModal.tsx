import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Check, Shirt, UserCircle, Box, Sparkles } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import './OnboardingModal.css'

interface OnboardingModalProps {
  onDone: () => void
}

const STEPS = ['account', 'instance', 'skin', 'play'] as const

export function OnboardingModal({ onDone }: OnboardingModalProps) {
  const { t } = useI18n()
  const [step, setStep] = useState(0)

  useEffect(() => {
    playUiSound('click')
  }, [step])

  async function finish() {
    await window.primeLauncher.settings.update({ onboardingDone: true })
    playUiSound('success')
    onDone()
  }

  const id = STEPS[step]!
  const icons = {
    account: <UserCircle size={28} />,
    instance: <Box size={28} />,
    skin: <Shirt size={28} />,
    play: <Sparkles size={28} />
  }

  return (
    <div className="onboard-modal">
      <motion.div
        className="onboard-modal__card"
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <p className="onboard-modal__eyebrow">{t('onboarding.eyebrow')}</p>
        <div className="onboard-modal__icon">{icons[id]}</div>
        <h2>{t(`onboarding.steps.${id}.title`)}</h2>
        <p className="onboard-modal__desc">{t(`onboarding.steps.${id}.body`)}</p>

        <div className="onboard-modal__dots">
          {STEPS.map((_, i) => (
            <span key={i} className={i === step ? 'is-active' : i < step ? 'is-done' : ''} />
          ))}
        </div>

        <div className="onboard-modal__actions">
          {step < STEPS.length - 1 ? (
            <Button variant="primary" onClick={() => setStep((s) => s + 1)}>
              {t('onboarding.next')}
            </Button>
          ) : (
            <Button variant="primary" icon={<Check size={16} />} onClick={() => void finish()}>
              {t('onboarding.finish')}
            </Button>
          )}
          <button type="button" className="onboard-modal__skip" onClick={() => void finish()}>
            {t('onboarding.skip')}
          </button>
        </div>
      </motion.div>
    </div>
  )
}
