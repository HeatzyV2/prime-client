import { Link } from 'react-router-dom'
import { useI18n } from '@renderer/context/I18nProvider'
import { useAccounts } from '@renderer/context/AccountProvider'

export function AccountsPanel() {
  const { t } = useI18n()
  const { accounts, activeAccount, loginMicrosoft } = useAccounts()

  return (
    <>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.activeAccount.label')}</div>
          <div className="settings__hint">
            {activeAccount
              ? `${activeAccount.username} (${activeAccount.type})`
              : t('settings.activeAccount.none')}
          </div>
        </div>
        <Link to="/accounts">
          <button className="settings__input" style={{ cursor: 'pointer' }}>
            {t('common.manage')} ({accounts.length})
          </button>
        </Link>
      </div>
      <div className="settings__row">
        <div>
          <div className="settings__label">{t('settings.microsoftAccount.label')}</div>
        </div>
        <button
          className="settings__input"
          style={{ cursor: 'pointer' }}
          onClick={() => void loginMicrosoft()}
        >
          {t('common.signIn')}
        </button>
      </div>
    </>
  )
}
