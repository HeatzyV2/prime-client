import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react'
import { createPortal } from 'react-dom'
import { Button } from './Button'
import { useI18n } from '@renderer/context/I18nProvider'
import './ConfirmDialog.css'

export type DialogVariant = 'default' | 'danger'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  /** Optional third action (e.g. “keep files”). Resolves to `'secondary'`. */
  secondaryLabel?: string
  variant?: DialogVariant
}

export interface AlertOptions {
  title?: string
  message: string
  confirmLabel?: string
  variant?: DialogVariant
}

type ConfirmResult = boolean | 'secondary'

type DialogState =
  | {
      mode: 'confirm'
      options: ConfirmOptions
      resolve: (value: ConfirmResult) => void
    }
  | {
      mode: 'alert'
      options: AlertOptions
      resolve: () => void
    }

interface ConfirmContextValue {
  confirm: (options: ConfirmOptions | string) => Promise<ConfirmResult>
  alert: (options: AlertOptions | string) => Promise<void>
}

const ConfirmContext = createContext<ConfirmContextValue | null>(null)

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [dialog, setDialog] = useState<DialogState | null>(null)

  const confirm = useCallback((options: ConfirmOptions | string) => {
    const opts: ConfirmOptions = typeof options === 'string' ? { message: options } : options
    return new Promise<ConfirmResult>((resolve) => {
      setDialog({ mode: 'confirm', options: opts, resolve })
    })
  }, [])

  const alertFn = useCallback((options: AlertOptions | string) => {
    const opts: AlertOptions = typeof options === 'string' ? { message: options } : options
    return new Promise<void>((resolve) => {
      setDialog({ mode: 'alert', options: opts, resolve })
    })
  }, [])

  const value = useMemo(() => ({ confirm, alert: alertFn }), [confirm, alertFn])

  useEffect(() => {
    if (!dialog) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return
      e.preventDefault()
      if (dialog.mode === 'confirm') dialog.resolve(false)
      else dialog.resolve()
      setDialog(null)
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [dialog])

  function closeConfirm(result: ConfirmResult) {
    if (dialog?.mode !== 'confirm') return
    dialog.resolve(result)
    setDialog(null)
  }

  function closeAlert() {
    if (dialog?.mode !== 'alert') return
    dialog.resolve()
    setDialog(null)
  }

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      {dialog &&
        createPortal(
          <ConfirmDialogView
            dialog={dialog}
            onConfirm={() => {
              if (dialog.mode === 'confirm') closeConfirm(true)
              else closeAlert()
            }}
            onSecondary={() => closeConfirm('secondary')}
            onCancel={() => {
              if (dialog.mode === 'confirm') closeConfirm(false)
              else closeAlert()
            }}
          />,
          document.body
        )}
    </ConfirmContext.Provider>
  )
}

export function useConfirm(): ConfirmContextValue {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider')
  return ctx
}

function ConfirmDialogView({
  dialog,
  onConfirm,
  onSecondary,
  onCancel
}: {
  dialog: DialogState
  onConfirm: () => void
  onSecondary: () => void
  onCancel: () => void
}) {
  const { t } = useI18n()
  const { options, mode } = dialog
  const variant = options.variant ?? (mode === 'confirm' ? 'danger' : 'default')
  const title =
    options.title ??
    (mode === 'confirm' ? t('dialog.confirmTitle') : t('dialog.alertTitle'))
  const confirmLabel =
    options.confirmLabel ?? (mode === 'alert' ? t('actions.close') : t('actions.delete'))
  const cancelLabel =
    mode === 'confirm' && options.cancelLabel ? options.cancelLabel : t('actions.cancel')
  const secondaryLabel = mode === 'confirm' ? options.secondaryLabel : undefined

  return (
    <div className="prime-dialog" role="presentation" onClick={onCancel}>
      <div
        className={`prime-dialog__panel prime-dialog__panel--${variant} v3-fade-in`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="prime-dialog-title"
        aria-describedby="prime-dialog-message"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="prime-dialog__accent" aria-hidden />
        <p id="prime-dialog-title" className="prime-dialog__title">
          {title}
        </p>
        <p id="prime-dialog-message" className="prime-dialog__message">
          {options.message}
        </p>
        <div className="prime-dialog__actions">
          {mode === 'confirm' && (
            <Button variant="ghost" size="md" onClick={onCancel}>
              {cancelLabel}
            </Button>
          )}
          {secondaryLabel ? (
            <Button variant="secondary" size="md" onClick={onSecondary}>
              {secondaryLabel}
            </Button>
          ) : null}
          <Button
            autoFocus
            variant={variant === 'danger' && mode === 'confirm' ? 'danger' : 'primary'}
            size="md"
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
