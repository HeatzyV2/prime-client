/** Lightweight first paint while settings IPC is deferred. */
export function SettingsSkeleton() {
  return (
    <div className="settings settings--skeleton" aria-busy="true">
      <nav className="settings__nav">
        {Array.from({ length: 9 }, (_, i) => (
          <div key={i} className="settings__skel-nav" />
        ))}
      </nav>
      <div className="settings__panel">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="settings__skel-row" />
        ))}
      </div>
    </div>
  )
}
