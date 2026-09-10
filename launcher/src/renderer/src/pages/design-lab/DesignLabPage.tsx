import { useEffect, useState } from 'react'
import { PRIME_THEMES, type PrimeThemeId } from '@shared/theme'
import { PlayButton } from '@renderer/components/launcher/PlayButton'
import { PrimeLogo } from '@renderer/design-system/components'
import { useTheme } from '@renderer/context/ThemeProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import './DesignLabPage.css'

type Composition = 'A' | 'B' | 'C' | 'D'
type Panel = 'none' | 'instance' | 'create' | 'server'

function themeKey(id: PrimeThemeId): string {
  return id.replace('prime-', '')
}

function WorldArt({ dense = false }: { dense?: boolean }) {
  return (
    <div className={`lab-world${dense ? ' lab-world--dense' : ''}`} aria-hidden>
      <div className="lab-world__sky" />
      <div className="lab-world__haze" />
      <div className="lab-world__mid" />
      <div className="lab-world__blocks">
        <span />
        <span />
        <span />
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="lab-world__terrain" />
      <div className="lab-world__vignette" />
      <div className="lab-world__scrim" />
    </div>
  )
}

export function DesignLabPage() {
  const { t } = useI18n()
  const { applyThemeId } = useTheme()
  const [comp, setComp] = useState<Composition>('D')
  const [theme, setTheme] = useState<PrimeThemeId>('prime-crimson')
  const [panel, setPanel] = useState<Panel>('none')
  const [advanced, setAdvanced] = useState(false)

  useEffect(() => {
    applyThemeId(theme)
  }, [theme, applyThemeId])

  return (
    <div className="lab" data-lab-theme={theme}>
      <aside className="lab__controls">
        <p className="text-label">{t('designLab.title')}</p>
        <h1 className="lab__title">{t('designLab.subtitle')}</h1>
        <p className="lab__hint">{t('designLab.hint')}</p>

        <p className="text-label">{t('designLab.composition')}</p>
        <div className="lab__seg">
          {(['A', 'B', 'C', 'D'] as Composition[]).map((id) => (
            <button
              key={id}
              type="button"
              className={`lab__seg-btn${comp === id ? ' is-active' : ''}`}
              onClick={() => setComp(id)}
            >
              {id}
            </button>
          ))}
        </div>
        <p className="lab__meta">
          <strong>{t(`designLab.compositions.${comp}.name`)}</strong>
          <span>{t(`designLab.compositions.${comp}.blurb`)}</span>
        </p>

        <p className="text-label">{t('designLab.theme')}</p>
        <div className="lab__themes">
          {PRIME_THEMES.map((id) => (
            <button
              key={id}
              type="button"
              className={`lab__theme${theme === id ? ' is-active' : ''}`}
              onClick={() => setTheme(id)}
            >
              {t(`settings.theme.${themeKey(id)}`)}
            </button>
          ))}
        </div>

        <p className="text-label">{t('designLab.mocks')}</p>
        <div className="lab__actions">
          <button type="button" onClick={() => setPanel('instance')}>
            {t('designLab.openInstance')}
          </button>
          <button type="button" onClick={() => setPanel('create')}>
            {t('designLab.openCreate')}
          </button>
          <button type="button" onClick={() => setPanel('server')}>
            {t('designLab.openServer')}
          </button>
        </div>
      </aside>

      <div className={`lab__stage lab__stage--${comp}`}>
        {comp === 'A' && (
          <div className="lab-scene lab-scene--a">
            <WorldArt />
            <div className="lab-scene__brand">
              <PrimeLogo size={36} compact />
              <span>Prime</span>
            </div>
            <div className="lab-scene__hero lab-scene__hero--low">
              <PlayButton label={t('common.play')} />
              <div className="lab-scene__bar">
                <button type="button" onClick={() => setPanel('instance')}>
                  {t('designLab.mockInstance')}
                </button>
                <span>·</span>
                <button type="button" onClick={() => setPanel('server')}>
                  {t('designLab.mockServerShort')}
                </button>
                <span>·</span>
                <span>{t('designLab.mockVersionBar')}</span>
              </div>
            </div>
            <nav className="lab-scene__dock" aria-hidden>
              <i />
              <i />
              <i />
              <i />
            </nav>
          </div>
        )}

        {comp === 'B' && (
          <div className="lab-scene lab-scene--b">
            <div className="lab-scene__side">
              <WorldArt dense />
            </div>
            <div className="lab-scene__panel">
              <div className="lab-scene__brand">
                <PrimeLogo size={32} compact />
                <span>Prime Client</span>
              </div>
              <p className="lab-scene__version">{t('designLab.mockVersionFull')}</p>
              <PlayButton label={t('common.play')} />
              <button type="button" className="lab-scene__pill" onClick={() => setPanel('instance')}>
                <span className="text-label">{t('dashboard.playHub.instance')}</span>
                <strong>{t('designLab.mockInstance')}</strong>
              </button>
              <div className="lab-scene__cluster">
                <button type="button" onClick={() => setPanel('server')}>
                  {t('designLab.mockServer')}
                </button>
                <span>{t('designLab.mockRam')}</span>
                <span>{t('designLab.mockPlayer')}</span>
              </div>
              <button type="button" className="lab-scene__link" onClick={() => setPanel('instance')}>
                {t('designLab.manageInstances')}
              </button>
            </div>
          </div>
        )}

        {comp === 'C' && (
          <div className="lab-scene lab-scene--c">
            <div className="lab-scene__layer lab-scene__layer--far">
              <WorldArt />
            </div>
            <div className="lab-scene__layer lab-scene__layer--near" aria-hidden />
            <div className="lab-scene__play-light" aria-hidden />
            <div className="lab-scene__brand lab-scene__brand--center">
              <PrimeLogo size={48} />
            </div>
            <div className="lab-scene__hero">
              <PlayButton label={t('common.play')} />
              <p className="lab-scene__caption">{t('designLab.mockCaption')}</p>
            </div>
            <aside className="lab-scene__ghost-rail" aria-hidden>
              <i />
              <i />
              <i />
              <i />
            </aside>
          </div>
        )}

        {comp === 'D' && (
          <div className="lab-scene lab-scene--d">
            <WorldArt />
            <aside className="lab-scene__rail" aria-hidden>
              <i className="is-active" />
              <i />
              <i />
              <i />
            </aside>
            <div className="lab-scene__brand lab-scene__brand--d">
              <PrimeLogo size={28} compact />
              <div>
                <span className="lab-scene__wordmark">Prime</span>
                <span className="lab-scene__sub">Client</span>
              </div>
            </div>
            <div className="lab-scene__center">
              <p className="lab-scene__mc">{t('designLab.mockVersionFull')}</p>
              <div className="lab-scene__play-wrap">
                <div className="lab-scene__play-light" aria-hidden />
                <PlayButton label={t('common.play')} />
              </div>
              <button type="button" className="lab-scene__instance" onClick={() => setPanel('instance')}>
                <span className="text-label">{t('dashboard.playHub.instance')}</span>
                <strong>{t('designLab.mockInstance')}</strong>
                <span>{t('dashboard.playHub.change')}</span>
              </button>
              <div className="lab-scene__cluster">
                <button type="button" onClick={() => setPanel('server')}>
                  {t('designLab.mockServer')}
                </button>
                <span>{t('designLab.mockRamFull')}</span>
                <span>{t('designLab.mockPlayer')}</span>
              </div>
            </div>
          </div>
        )}

        {panel !== 'none' && (
          <div className="lab-sheet" role="dialog" onClick={() => setPanel('none')}>
            <div className="lab-sheet__panel v3-fade-in" onClick={(e) => e.stopPropagation()}>
              {panel === 'instance' && (
                <>
                  <p className="text-label">{t('dashboard.playHub.instance')}</p>
                  <h2>{t('designLab.selectInstance')}</h2>
                  <button type="button" className="lab-sheet__item is-active">
                    <span>
                      {t('designLab.mockInstance')}
                      <strong>{t('designLab.mockVersionFull')}</strong>
                    </span>
                    <em>✓</em>
                  </button>
                  <button type="button" className="lab-sheet__item">
                    <span>
                      {t('instances.vanilla')}
                      <strong>{t('designLab.mockVanillaVersion')}</strong>
                    </span>
                  </button>
                  <button type="button" className="lab-sheet__item">
                    <span>
                      {t('designLab.mockAltInstance')}
                      <strong>{t('designLab.mockVersionFull')}</strong>
                    </span>
                  </button>
                  <button
                    type="button"
                    className="lab-sheet__cta"
                    onClick={() => setPanel('create')}
                  >
                    + {t('dashboard.playHub.createInstance')}
                  </button>
                  <button type="button" className="lab-sheet__link">
                    {t('dashboard.playHub.manageAll')}
                  </button>
                </>
              )}
              {panel === 'create' && (
                <>
                  <p className="text-label">{t('designLab.createLabel')}</p>
                  <h2>{t('dashboard.playHub.createInstance')}</h2>
                  <label>
                    {t('modals.instance.name')}
                    <input defaultValue={t('designLab.defaultName')} key={`name-${t('designLab.defaultName')}`} />
                  </label>
                  <label>
                    {t('modals.instance.minecraftVersion')}
                    <select defaultValue="1.21.1">
                      <option>1.21.1</option>
                      <option>1.20.4</option>
                    </select>
                  </label>
                  <label>
                    {t('modals.instance.loader')}
                    <select defaultValue="fabric">
                      <option value="prime">{t('instances.primeClient')}</option>
                      <option value="fabric">{t('instances.fabric')}</option>
                      <option value="vanilla">{t('instances.vanilla')}</option>
                    </select>
                  </label>
                  <button type="button" className="lab-sheet__link" onClick={() => setAdvanced((v) => !v)}>
                    {advanced ? t('modals.instance.hideAdvanced') : t('modals.instance.showAdvanced')}
                  </button>
                  {advanced && (
                    <div className="lab-sheet__adv">
                      <label>
                        {t('modals.instance.ram')}
                        <input defaultValue="4096" />
                      </label>
                      <label>
                        {t('modals.instance.javaPath')}
                        <input
                          defaultValue={t('designLab.autoJava')}
                          key={`java-${t('designLab.autoJava')}`}
                        />
                      </label>
                    </div>
                  )}
                  <button type="button" className="lab-sheet__cta">
                    {t('modals.instance.create')}
                  </button>
                </>
              )}
              {panel === 'server' && (
                <>
                  <p className="text-label">{t('dashboard.playHub.server')}</p>
                  <h2>{t('designLab.selectServer')}</h2>
                  <button type="button" className="lab-sheet__item">
                    <span>
                      {t('dashboard.playHub.singleplayer')}
                      <strong>{t('dashboard.playHub.noServer')}</strong>
                    </span>
                  </button>
                  <button type="button" className="lab-sheet__item is-active">
                    <span>
                      {t('designLab.mockServer')}
                      <strong>{t('designLab.onlineMs', { ms: 42 })}</strong>
                    </span>
                  </button>
                  <button type="button" className="lab-sheet__item">
                    <span>
                      Hypixel
                      <strong>{t('designLab.onlineMs', { ms: 28 })}</strong>
                    </span>
                  </button>
                </>
              )}
              <button type="button" className="lab-sheet__close" onClick={() => setPanel('none')}>
                {t('common.close')}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
