export const en = {
  app: {
    name: 'Prime Launcher'
  },
  nav: {
    dashboard: 'Home',
    accounts: 'Accounts',
    instances: 'Instances',
    mods: 'Mods',
    resources: 'Resource Packs',
    shaders: 'Shaders',
    store: 'Prime Store',
    cosmetics: 'Cosmetics',
    servers: 'Servers',
    friends: 'Friends',
    chat: 'Chat',
    news: 'News',
    media: 'Media',
    performance: 'Performance',
    downloads: 'Downloads',
    console: 'Console',
    settings: 'Settings'
  },
  boot: {
    core: 'Initializing Prime Core…',
    updates: 'Checking for updates…',
    minecraft: 'Loading Minecraft…',
    ready: 'Ready.'
  },
  common: {
    play: 'Play',
    launching: 'Launching…',
    join: 'Join',
    saved: 'Saved',
    guest: 'Guest',
    never: 'Never',
    free: 'Free',
    prime: 'Prime',
    primePlus: 'Prime Plus',
    active: 'Active',
    use: 'Use',
    add: 'Add',
    manage: 'Manage',
    signIn: 'Sign in',
    checkNow: 'Check now',
    downloadUpdate: 'Download update',
    automatic: 'Automatic',
    updateNow: 'Update now'
  },
  newsTag: {
    update: 'Update',
    event: 'Event',
    announcement: 'Announcement'
  },
  dashboard: {
    signInTitle: 'Sign in to play',
    signInHint: 'Connect Microsoft or use offline mode to launch Prime Client.',
    addAccount: 'Add Account',
    welcomeBack: 'Welcome back',
    quickLaunch: 'Quick Launch',
    profile: 'Profile',
    instance: 'Instance',
    primeAccount: 'Prime Account',
    lastSession: 'Last Session',
    news: 'News',
    favoriteServers: 'Favorite Servers',
    timePlayed: 'Time played',
    mods: 'Mods',
    ram: 'RAM',
    version: 'Version'
  },
  settings: {
    title: 'Settings',
    subtitle: 'Saved locally — no account or cloud required.',
    sections: {
      general: 'General',
      appearance: 'Appearance',
      minecraft: 'Minecraft',
      performance: 'Performance',
      accounts: 'Accounts',
      privacy: 'Privacy',
      downloads: 'Downloads',
      updates: 'Updates',
      advanced: 'Advanced'
    },
    language: {
      label: 'Language',
      hint: 'Launcher display language',
      en: 'English',
      fr: 'Français'
    },
    closeOnLaunch: {
      label: 'Minimize on game launch',
      hint: 'Keeps the launcher running — only minimizes the window when Minecraft starts',
      toggle: 'Minimize on launch'
    },
    checkUpdates: {
      label: 'Check for updates',
      hint: 'Compare with latest GitHub Release'
    },
    autoUpdate: {
      label: 'Auto-update launcher',
      hint: 'Check GitHub for updates when the launcher starts',
      toggle: 'Auto update'
    },
    theme: {
      label: 'Theme',
      dark: 'Crimson',
      crimson: 'Crimson',
      midnight: 'Midnight',
      aurora: 'Aurora'
    },
    hardwareAccel: {
      label: 'Hardware acceleration',
      toggle: 'Hardware acceleration'
    },
    backgroundNebula: {
      label: 'Nebula background',
      hint: 'Animated space background from the Store',
      toggle: 'Nebula background'
    },
    restartRequired: 'Restart required to apply hardware acceleration.',
    restartNow: 'Restart now',
    javaPath: {
      label: 'Default Java path',
      hint: 'Auto-detected JDK 21+ at launch',
      addPath: 'Add path',
      browseFailed: 'Could not add Java path.'
    },
    defaultRam: {
      label: 'Default RAM allocation'
    },
    gameResolution: {
      label: 'Game resolution',
      hint: 'Window size at launch (width × height in pixels)'
    },
    gameDisplayMode: {
      label: 'Display mode',
      hint: 'How Minecraft starts — borderless is windowed at your chosen resolution',
      windowed: 'Windowed',
      borderless: 'Borderless fullscreen',
      fullscreen: 'Fullscreen'
    },
    performancePreset: {
      label: 'Performance preset on launch',
      low: 'Low PC',
      balanced: 'Balanced',
      performance: 'Performance',
      ultra: 'Ultra'
    },
    activeAccount: {
      label: 'Active account',
      none: 'No account — add one to play'
    },
    microsoftAccount: {
      label: 'Microsoft account'
    },
    analytics: {
      label: 'Analytics',
      hint: 'Disabled by default — nothing is sent without a server',
      toggle: 'Analytics'
    },
    discordRpc: {
      label: 'Discord Rich Presence',
      hint: 'Shows Prime Launcher on Discord (hands off to the mod in-game)',
      toggle: 'Discord RPC'
    },
    concurrentDownloads: {
      label: 'Concurrent downloads'
    },
    jvmArgs: {
      label: 'JVM arguments',
      hint: 'One per line — applied via Performance presets too'
    },
    developerMode: {
      label: 'Developer mode',
      toggle: 'Developer mode'
    },
    updateNotes: 'v{current} → latest v{latest} — {notes}'
  },
  updates: {
    modal: {
      title: 'Update available',
      subtitle: 'A new version of Prime Client is ready. Install without leaving the launcher.'
    },
    launcher: {
      label: 'Prime Launcher'
    },
    mod: {
      label: 'Prime Client mod'
    },
    versionLine: 'v{current} → v{latest}',
    installLauncher: 'Install launcher',
    installMod: 'Install mod',
    installing: 'Installing…',
    checking: 'Checking…',
    later: 'Later',
    upToDate: 'Up to date',
    phase: {
      downloading: 'Downloading…',
      installing: 'Installing…',
      done: 'Done',
      error: 'Failed'
    },
    errors: {
      dev_mode: 'Launcher self-update only works in the installed app, not dev mode.',
      unsupported_platform: 'In-app launcher update is only supported on Windows.',
      no_update: 'No update available to install.',
      game_running: 'Close Minecraft before updating the mod.',
      no_instance: 'No instance found — create one first.',
      prime_mod_disabled: 'Prime mod is disabled on the default instance.',
      unknown: 'Update failed. Try again or download from GitHub.'
    }
  },
  pages: {
    accounts: {
      title: 'Accounts',
      subtitle: 'Microsoft authentication, offline profiles, and Prime Account sync.'
    },
    instances: {
      title: 'Instances',
      subtitle: 'Create and manage Minecraft installations with Prime Client.'
    },
    mods: {
      title: 'Mods',
      subtitle: 'Manage mods for the active instance — local files and Modrinth.'
    },
    resources: {
      title: 'Resource Packs',
      subtitle: 'Import or install resource packs for the active instance.'
    },
    shaders: {
      title: 'Shaders',
      subtitle: 'Import or install shader packs for the active instance.'
    },
    store: {
      title: 'Prime Store',
      subtitle: 'Unlock cosmetics and themes with Prime Coins — all local, no payment server.'
    },
    cosmetics: {
      title: 'Cosmetics',
      subtitle: 'Owned items from the Store — equip locally (syncs to Prime profile file).'
    },
    servers: {
      title: 'Server Hub',
      subtitle: 'Favorite servers with live player count, ping, and quick join.'
    },
    friends: {
      title: 'Friends',
      subtitle: 'Prime friends synced launcher ↔ game — live presence and requests.'
    },
    chat: {
      title: 'Chat',
      subtitle: 'Private messages with friends — live, synced with the game.'
    },
    news: {
      title: 'News',
      subtitle: 'Bundled announcements — no remote news server.'
    },
    media: {
      title: 'Media',
      subtitle: 'Screenshots and recordings from your instance folder.'
    },
    performance: {
      title: 'Performance',
      subtitle: 'Hardware info and launch presets — RAM, render distance, JVM flags.'
    },
    downloads: {
      title: 'Downloads',
      subtitle: 'Modrinth installs and launcher tasks.'
    },
    console: {
      title: 'Console',
      subtitle: 'Launch output, downloads, warnings and errors in real time.'
    }
  },
  modals: {
    login: {
      title: 'Sign in to Prime',
      subtitle: 'Use your Microsoft account for official Minecraft, or play offline with a custom username.',
      microsoft: 'Sign in with Microsoft',
      offlineDivider: '— or offline —',
      offlinePlaceholder: 'Offline username',
      offlineContinue: 'Continue Offline',
      cancel: 'Cancel',
      loginFailed: 'Login failed.',
      offlineFailed: 'Could not create offline account.'
    },
    instance: {
      createTitle: 'New Instance',
      editTitle: 'Configure Instance',
      subtitle: 'Stored locally in AppData — Vanilla and Fabric supported, no server required.',
      name: 'Name',
      minecraftVersion: 'Minecraft version',
      loader: 'Loader',
      fabricLoader: 'Fabric loader',
      includePrimeMod: 'Include Prime Client mod (+ Fabric API)',
      ram: 'RAM (MB)',
      jvmArgs: 'JVM args (one per line)',
      javaPath: 'Java path (optional)',
      javaPathHint: 'Overrides global default for this instance',
      createFailed: 'Could not create instance.',
      saveFailed: 'Could not save instance.',
      saving: 'Saving…',
      create: 'Create'
    },
    browse: {
      modsTitle: 'Browse — Mods',
      resourcePacksTitle: 'Browse — Resource Packs',
      shadersTitle: 'Browse — Shaders',
      subtitle: 'Search Modrinth or CurseForge — downloads go to your active instance folder.',
      searching: 'Searching…',
      searchFailed: 'Search failed.',
      installFailed: 'Install failed.',
      close: 'Close',
      chooseVersion: 'Choose version',
      chooseVersionHint: 'Select which release to install for this instance.',
      versionLabel: 'Version',
      gameVersions: 'MC versions',
      loaders: 'Loaders',
      recommended: 'Recommended',
      back: 'Back',
      noVersions: 'No compatible versions found.',
      versionsFailed: 'Could not load versions.'
    },
    modrinth: {
      modsTitle: 'Browse Modrinth — Mods',
      resourcePacksTitle: 'Browse Modrinth — Resource Packs',
      shadersTitle: 'Browse Modrinth — Shaders',
      subtitle: 'Public Modrinth API — downloads go directly to your active instance folder.',
      searching: 'Searching…',
      searchFailed: 'Search failed.',
      installFailed: 'Install failed.',
      close: 'Close'
    }
  },
  accounts: {
    addAccount: 'Add Account',
    primeAccount: 'Prime Account',
    quickAdd: 'Quick Add',
    quickAddHint: 'Microsoft opens a secure login window (Xbox Live → Minecraft Services).',
    signInMicrosoft: 'Sign in with Microsoft',
    offlinePlaceholder: 'Offline username',
    addOffline: 'Add Offline Account',
    minecraftAccounts: 'Minecraft Accounts',
    noAccounts: 'No accounts yet.',
    addOne: 'Add one',
    toPlay: 'to play.',
    level: 'Level {level}',
    syncDescription: 'Sync configs, HUD, cosmetics, and stats locally.',
    syncButton: 'Sync Prime Profile',
    refreshMicrosoft: 'Refresh token',
    refreshSuccess: 'Microsoft token refreshed.',
    refreshFailed: 'Token refresh failed.',
    microsoft: 'Microsoft',
    offline: 'Offline'
  },
  servers: {
    addServer: 'Add Server',
    serverName: 'Server name',
    serverAddress: 'Address (host or host:port)',
    refresh: 'Refresh status',
    remove: 'Remove',
    empty: 'No favorite servers yet.',
    joinNeedsAccount: 'Sign in to join a server.',
    added: 'Server added.',
    addFailed: 'Could not add server.'
  },
  friends: {
    addFriend: 'Add Friend',
    usernamePrompt: 'Minecraft username',
    notePrompt: 'Note (optional)',
    notePlaceholder: 'Edit note…',
    saveNote: 'Save',
    remove: 'Remove',
    empty: 'No friends yet. Both players must open Prime once.',
    offline: 'Offline',
    addFailed: 'Could not add friend.',
    refreshStatus: 'Refresh status',
    message: 'Message',
    inviteParty: 'Invite to party',
    accept: 'Accept',
    partyServer: 'Party server',
    joinPartyServer: 'Join party server'
  },
  chat: {
    refresh: 'Refresh',
    conversations: 'Conversations',
    empty: 'No conversations yet. Open a DM from Friends to start chatting.',
    selectConversation: 'Pick a conversation',
    selectHint: 'Choose someone on the left to open your private thread.',
    messagePlaceholder: 'Write a message…',
    send: 'Send',
    image: 'Image',
    imagePathPrompt: 'Image file path',
    live: 'Live',
    directMessage: 'Direct message',
    startTitle: 'Say hello',
    startBody: 'This is the beginning of your conversation with {name}.',
    composerHint: 'Enter to send · images supported',
    today: 'Today',
    yesterday: 'Yesterday'
  },
  actions: {
    import: 'Import',
    browseModrinth: 'Browse Modrinth',
    browseContent: 'Browse online',
    searchCurseForge: 'Search CurseForge…',
    openFolder: 'Open Folder',
    configure: 'Configure',
    folder: 'Folder',
    duplicate: 'Duplicate',
    delete: 'Delete',
    equip: 'Equip',
    unequip: 'Unequip',
    owned: 'Owned',
    buy: 'Buy',
    claim: 'Claim',
    free: 'Free',
    applyPreset: 'Apply Preset',
    applying: 'Applying…',
    clearCompleted: 'Clear completed',
    refresh: 'Refresh',
    cancel: 'Cancel',
    save: 'Save',
    searchMods: 'Search mods…',
    searchModrinth: 'Search Modrinth…',
    install: 'Install',
    installing: 'Installing…',
    importJar: 'Import .jar',
    close: 'Close',
    create: 'Create'
  },
  mods: {
    all: 'All',
    enabled: 'Enabled',
    disabled: 'Disabled'
  },
  empty: {
    noMods: 'No mods in this instance.',
    noResourcePacks: 'No resource packs yet. Import a .zip or install from Modrinth.',
    noShaders: 'No shader packs yet. Install Iris (mod) first, then add packs here.',
    noDownloads: 'No downloads yet.',
    noDownloadsHint: 'No recent downloads. Launch Minecraft to see progress here.',
    noScreenshots: 'No media yet. Press F2 for screenshots, or record clips with Prime Client (Creator → Clip Recorder).',
    loadingInstance: 'Loading instance…'
  },
  resources: {
    importZip: 'Import .zip',
    active: 'Active'
  },
  confirm: {
    deleteInstance: 'Delete "{name}"? Game files can be kept or removed.',
    deleteFiles: 'Also delete saves and mods on disk?',
    removeMod: 'Remove {name}?',
    removePack: 'Remove {name}?',
    removeShader: 'Remove {name}?'
  },
  errors: {
    deleteInstance: 'Could not delete instance.'
  },
  performance: {
    detectedHardware: 'Detected Hardware',
    cpu: 'CPU',
    gpu: 'GPU',
    ram: 'RAM',
    applySuccess: 'Preset applied to active instance (RAM + options.txt).',
    applyFailed: 'Failed to apply preset.',
    detecting: 'Detecting hardware…',
    systemRam: 'System RAM',
    chunks: '{count} chunks'
  },
  store: {
    coins: '{balance} Prime Coins',
    unlocked: '{name} unlocked! Equip cosmetics from the Cosmetics page.',
    purchaseFailed: 'Purchase failed.',
    coinsPrice: '{price} coins',
    categories: {
      all: 'All',
      cosmetic: 'Cosmetics',
      theme: 'Themes',
      background: 'Backgrounds',
      badge: 'Badges'
    }
  },
  cosmetics: {
    characterPreview: 'Character Preview',
    previewHint: '2D head preview — full viewer in Prime Client mod',
    all: 'All',
    capes: 'Capes',
    wings: 'Wings',
    pets: 'Pets',
    emotes: 'Emotes',
    badges: 'Badges',
    emptyOwned: 'No cosmetics owned yet — visit the Store.'
  },
  instances: {
    vanilla: 'Vanilla',
    fabric: 'Fabric',
    primeClient: 'Prime Client',
    play: 'Play',
    default: 'Default',
    loading: 'Loading instances…',
    signInToPlay: 'Sign in to play.',
    lastPlayed: 'Last played {date}',
    ramBadge: '{mb} MB RAM',
    modsBadge: '{count} mods'
  },
  media: {
    openFolder: 'Open clips folder',
    refresh: 'Refresh',
    replaysNote: 'Replays and clips appear here when exported by Prime Client (config/primeclient/clips).'
  },
  logs: {
    title: 'Launch logs',
    tab: 'Console',
    empty: 'No logs yet. Press Play to see download and launch output here.',
    openFolder: 'Open log folder',
    clear: 'Clear',
    copy: 'Copy all',
    copied: 'Copied!',
    filterAll: 'All',
    filterInfo: 'Info',
    filterWarn: 'Warn',
    filterError: 'Errors',
    filterDebug: 'Debug'
  },
  crash: {
    title: 'Minecraft crashed',
    sessionDuration: 'Session lasted {duration}',
    context: 'Context:',
    screen: 'Screen:',
    primeInvolved: 'Prime Client involved',
    suggestion: 'Suggested fix',
    openReport: 'Open crash report',
    openLogs: 'Open logs',
    sendReport: 'Send to Prime',
    dismiss: 'Dismiss',
    fix: {
      blurOnce:
        'A GUI rendering conflict occurred (blur limit). Update Prime Client to the latest version — this issue is fixed in recent builds.',
      outOfMemory:
        'Java ran out of memory. Increase RAM in Instance settings or close other heavy applications.',
      primeMod:
        'The crash originates from Prime Client ({location}). Try updating the mod or disabling recently changed modules.',
      modConflict:
        'A mod conflict is likely. Disable recently added mods and test again.',
      modConflictNamed:
        'Mods involved in the stack trace: {mods}. Try disabling them one by one.',
      loaderError:
        'Fabric loader or a mod failed to load. Check mod versions match your Minecraft version.',
      unknown:
        'Open the crash report for full details. If this keeps happening, share the report with support.'
    }
  }
} 

export type LocaleCatalog = typeof en
