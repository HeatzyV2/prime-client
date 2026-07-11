import type { LocaleCatalog } from './en'

export const fr: LocaleCatalog = {
  app: {
    name: 'Prime Launcher'
  },
  nav: {
    dashboard: 'Accueil',
    accounts: 'Comptes',
    instances: 'Instances',
    mods: 'Mods',
    resources: 'Packs de ressources',
    shaders: 'Shaders',
    store: 'Boutique Prime',
    cosmetics: 'Cosmétiques',
    servers: 'Serveurs',
    friends: 'Amis',
    news: 'Actualités',
    media: 'Médias',
    performance: 'Performances',
    downloads: 'Téléchargements',
    settings: 'Paramètres'
  },
  boot: {
    core: 'Initialisation du cœur Prime…',
    updates: 'Vérification des mises à jour…',
    minecraft: 'Chargement de Minecraft…',
    ready: 'Prêt.'
  },
  common: {
    play: 'Jouer',
    launching: 'Lancement…',
    join: 'Rejoindre',
    saved: 'Enregistré',
    guest: 'Invité',
    never: 'Jamais',
    free: 'Gratuit',
    prime: 'Prime',
    primePlus: 'Prime Plus',
    active: 'Actif',
    use: 'Utiliser',
    add: 'Ajouter',
    manage: 'Gérer',
    signIn: 'Se connecter',
    checkNow: 'Vérifier',
    downloadUpdate: 'Télécharger la mise à jour',
    automatic: 'Automatique'
  },
  newsTag: {
    update: 'Mise à jour',
    event: 'Événement',
    announcement: 'Annonce'
  },
  dashboard: {
    signInTitle: 'Connectez-vous pour jouer',
    signInHint: 'Connectez Microsoft ou utilisez le mode hors ligne pour lancer Prime Client.',
    addAccount: 'Ajouter un compte',
    welcomeBack: 'Bon retour',
    quickLaunch: 'Lancement rapide',
    profile: 'Profil',
    instance: 'Instance',
    primeAccount: 'Compte Prime',
    lastSession: 'Dernière session',
    news: 'Actualités',
    favoriteServers: 'Serveurs favoris',
    timePlayed: 'Temps de jeu',
    mods: 'Mods',
    ram: 'RAM',
    version: 'Version'
  },
  settings: {
    title: 'Paramètres',
    subtitle: 'Enregistré localement — aucun compte ou cloud requis.',
    sections: {
      general: 'Général',
      appearance: 'Apparence',
      minecraft: 'Minecraft',
      performance: 'Performances',
      accounts: 'Comptes',
      privacy: 'Confidentialité',
      downloads: 'Téléchargements',
      advanced: 'Avancé'
    },
    language: {
      label: 'Langue',
      hint: "Langue d'affichage du launcher",
      en: 'English',
      fr: 'Français'
    },
    closeOnLaunch: {
      label: 'Fermer au lancement du jeu',
      hint: 'Réduire le launcher quand Minecraft démarre',
      toggle: 'Fermer au lancement'
    },
    checkUpdates: {
      label: 'Rechercher des mises à jour',
      hint: 'Comparer avec la dernière release GitHub'
    },
    autoUpdate: {
      label: 'Mise à jour automatique',
      toggle: 'Mise à jour auto'
    },
    theme: {
      label: 'Thème',
      dark: 'Prime Sombre',
      crimson: 'Crimson (débloqué dans la boutique)'
    },
    hardwareAccel: {
      label: 'Accélération matérielle',
      toggle: 'Accélération matérielle'
    },
    backgroundNebula: {
      label: 'Fond nébuleuse',
      hint: 'Fond animé débloqué dans la Boutique',
      toggle: 'Fond nébuleuse'
    },
    restartRequired: 'Redémarrage requis pour l\'accélération matérielle.',
    restartNow: 'Redémarrer',
    javaPath: {
      label: 'Chemin Java par défaut',
      hint: 'JDK 21+ détecté automatiquement au lancement'
    },
    defaultRam: {
      label: 'Allocation RAM par défaut'
    },
    performancePreset: {
      label: 'Profil de performance au lancement',
      low: 'PC faible',
      balanced: 'Équilibré',
      performance: 'Performance',
      ultra: 'Ultra'
    },
    activeAccount: {
      label: 'Compte actif',
      none: 'Aucun compte — ajoutez-en un pour jouer'
    },
    microsoftAccount: {
      label: 'Compte Microsoft'
    },
    analytics: {
      label: 'Analytiques',
      hint: 'Désactivé par défaut — rien n\'est envoyé sans serveur',
      toggle: 'Analytiques'
    },
    discordRpc: {
      label: 'Discord Rich Presence',
      hint: 'Géré par le mod Prime Client en jeu',
      toggle: 'Discord RPC'
    },
    concurrentDownloads: {
      label: 'Téléchargements simultanés'
    },
    jvmArgs: {
      label: 'Arguments JVM',
      hint: 'Un par ligne — aussi appliqués via les profils Performance'
    },
    developerMode: {
      label: 'Mode développeur',
      toggle: 'Mode développeur'
    },
    updateNotes: 'v{current} → dernière v{latest} — {notes}'
  },
  pages: {
    accounts: {
      title: 'Comptes',
      subtitle: 'Authentification Microsoft, profils hors ligne et sync Compte Prime.'
    },
    instances: {
      title: 'Instances',
      subtitle: 'Créez et gérez vos installations Minecraft avec Prime Client.'
    },
    mods: {
      title: 'Mods',
      subtitle: "Gérez les mods de l'instance active — fichiers locaux et Modrinth."
    },
    resources: {
      title: 'Packs de ressources',
      subtitle: "Importez ou installez des packs pour l'instance active."
    },
    shaders: {
      title: 'Shaders',
      subtitle: "Importez ou installez des shaders pour l'instance active."
    },
    store: {
      title: 'Boutique Prime',
      subtitle: 'Débloquez cosmétiques et thèmes avec des Prime Coins — tout en local.'
    },
    cosmetics: {
      title: 'Cosmétiques',
      subtitle: 'Objets possédés — équipez localement (sync profil Prime).'
    },
    servers: {
      title: 'Hub Serveurs',
      subtitle: 'Serveurs favoris avec joueurs, ping et connexion rapide.'
    },
    friends: {
      title: 'Amis',
      subtitle: 'Liste locale — notes et scrims restent sur ce PC.'
    },
    news: {
      title: 'Actualités',
      subtitle: 'Annonces intégrées — pas de serveur de news distant.'
    },
    media: {
      title: 'Médias',
      subtitle: 'Captures et enregistrements du dossier instance.'
    },
    performance: {
      title: 'Performances',
      subtitle: 'Infos matériel et profils — RAM, distance de rendu, flags JVM.'
    },
    downloads: {
      title: 'Téléchargements',
      subtitle: 'Installations Modrinth et tâches du launcher.'
    }
  },
  modals: {
    login: {
      title: 'Connexion à Prime',
      subtitle: 'Utilisez votre compte Microsoft pour Minecraft officiel, ou jouez hors ligne.',
      microsoft: 'Se connecter avec Microsoft',
      offlineDivider: '— ou hors ligne —',
      offlinePlaceholder: 'Pseudo hors ligne',
      offlineContinue: 'Continuer hors ligne',
      cancel: 'Annuler',
      loginFailed: 'Échec de la connexion.',
      offlineFailed: 'Impossible de créer le compte hors ligne.'
    },
    instance: {
      createTitle: 'Nouvelle instance',
      editTitle: 'Configurer l\'instance',
      subtitle: 'Stockée localement dans AppData — Vanilla et Fabric, sans serveur.',
      name: 'Nom',
      minecraftVersion: 'Version Minecraft',
      loader: 'Loader',
      fabricLoader: 'Loader Fabric',
      includePrimeMod: 'Inclure le mod Prime Client (+ Fabric API)',
      ram: 'RAM (Mo)',
      jvmArgs: 'Arguments JVM (un par ligne)',
      createFailed: 'Impossible de créer l\'instance.',
      saveFailed: 'Impossible d\'enregistrer l\'instance.',
      saving: 'Enregistrement…',
      create: 'Créer'
    },
    modrinth: {
      modsTitle: 'Modrinth — Mods',
      resourcePacksTitle: 'Modrinth — Packs de ressources',
      shadersTitle: 'Modrinth — Shaders',
      subtitle: 'API Modrinth publique — téléchargements vers le dossier de l\'instance active.',
      searching: 'Recherche…',
      searchFailed: 'Échec de la recherche.',
      installFailed: 'Échec de l\'installation.',
      close: 'Fermer'
    }
  },
  accounts: {
    addAccount: 'Ajouter un compte',
    primeAccount: 'Compte Prime',
    quickAdd: 'Ajout rapide',
    quickAddHint: 'Microsoft ouvre une fenêtre sécurisée (Xbox Live → Minecraft).',
    signInMicrosoft: 'Se connecter avec Microsoft',
    offlinePlaceholder: 'Pseudo hors ligne',
    addOffline: 'Ajouter compte hors ligne',
    minecraftAccounts: 'Comptes Minecraft',
    noAccounts: 'Aucun compte.',
    addOne: 'Ajoutez-en un',
    toPlay: 'pour jouer.',
    level: 'Niveau {level}',
    syncDescription: 'Sync configs, HUD, cosmétiques et stats en local.',
    syncButton: 'Sync profil Prime',
    refreshMicrosoft: 'Rafraîchir le token',
    refreshSuccess: 'Token Microsoft rafraîchi.',
    refreshFailed: 'Échec du rafraîchissement du token.',
    microsoft: 'Microsoft',
    offline: 'Hors ligne'
  },
  servers: {
    addServer: 'Ajouter un serveur',
    serverName: 'Nom du serveur',
    serverAddress: 'Adresse (host ou host:port)',
    refresh: 'Actualiser le statut',
    remove: 'Supprimer',
    empty: 'Aucun serveur favori.',
    joinNeedsAccount: 'Connectez-vous pour rejoindre un serveur.',
    added: 'Serveur ajouté.',
    addFailed: 'Impossible d\'ajouter le serveur.'
  },
  friends: {
    addFriend: 'Ajouter un ami',
    usernamePrompt: 'Pseudo Minecraft',
    notePrompt: 'Note (optionnel)',
    notePlaceholder: 'Modifier la note…',
    saveNote: 'Enregistrer',
    remove: 'Supprimer',
    empty: 'Aucun ami. Ajoutez des pseudos avec lesquels vous jouez.',
    offline: 'Hors ligne',
    addFailed: 'Impossible d\'ajouter l\'ami.',
    refreshStatus: 'Actualiser le statut'
  },
  actions: {
    import: 'Importer',
    browseModrinth: 'Parcourir Modrinth',
    openFolder: 'Ouvrir le dossier',
    configure: 'Configurer',
    folder: 'Dossier',
    duplicate: 'Dupliquer',
    delete: 'Supprimer',
    equip: 'Équiper',
    unequip: 'Retirer',
    owned: 'Possédé',
    buy: 'Acheter',
    claim: 'Obtenir',
    free: 'Gratuit',
    applyPreset: 'Appliquer le profil',
    applying: 'Application…',
    clearCompleted: 'Effacer terminés',
    refresh: 'Actualiser',
    cancel: 'Annuler',
    save: 'Enregistrer',
    searchMods: 'Rechercher des mods…',
    searchModrinth: 'Rechercher sur Modrinth…',
    install: 'Installer',
    installing: 'Installation…',
    importJar: 'Importer .jar',
    close: 'Fermer',
    create: 'Créer'
  },
  mods: {
    all: 'Tout',
    enabled: 'Activés',
    disabled: 'Désactivés'
  },
  empty: {
    noMods: 'Aucun mod dans cette instance.',
    noResourcePacks: 'Aucun pack de ressources. Importez un .zip ou installez depuis Modrinth.',
    noShaders: 'Aucun shader. Installez Iris (mod) d\'abord, puis ajoutez des packs ici.',
    noDownloads: 'Aucun téléchargement.',
    noDownloadsHint: 'Aucun téléchargement récent. Lancez Minecraft pour voir la progression.',
    noScreenshots: 'Aucun média. F2 pour une capture, ou enregistrez un clip via Prime Client (Créateur → Clip Recorder).',
    loadingInstance: 'Chargement de l\'instance…'
  },
  resources: {
    importZip: 'Importer .zip',
    active: 'Actif'
  },
  confirm: {
    deleteInstance: 'Supprimer « {name} » ? Les fichiers peuvent être conservés ou supprimés.',
    deleteFiles: 'Supprimer aussi saves et mods sur le disque ?',
    removeMod: 'Supprimer {name} ?',
    removePack: 'Supprimer {name} ?',
    removeShader: 'Supprimer {name} ?'
  },
  errors: {
    deleteInstance: 'Impossible de supprimer l\'instance.'
  },
  performance: {
    detectedHardware: 'Matériel détecté',
    cpu: 'CPU',
    gpu: 'GPU',
    ram: 'RAM',
    applySuccess: 'Profil appliqué à l\'instance active (RAM + options.txt).',
    applyFailed: 'Échec de l\'application du profil.',
    detecting: 'Détection du matériel…',
    systemRam: 'RAM système',
    chunks: '{count} chunks'
  },
  store: {
    coins: '{balance} Prime Coins',
    unlocked: '{name} débloqué ! Équipez depuis Cosmétiques.',
    purchaseFailed: 'Achat échoué.',
    coinsPrice: '{price} coins',
    categories: {
      all: 'Tout',
      cosmetic: 'Cosmétiques',
      theme: 'Thèmes',
      background: 'Fonds',
      badge: 'Badges'
    }
  },
  cosmetics: {
    characterPreview: 'Aperçu personnage',
    previewHint: 'Aperçu tête 2D — viewer complet dans le mod Prime Client',
    all: 'Tout',
    capes: 'Capes',
    wings: 'Ailes',
    pets: 'Animaux',
    emotes: 'Emotes',
    badges: 'Badges',
    emptyOwned: 'Aucun cosmétique — visitez la Boutique.'
  },
  instances: {
    vanilla: 'Vanilla',
    fabric: 'Fabric',
    primeClient: 'Prime Client',
    play: 'Jouer',
    default: 'Par défaut',
    loading: 'Chargement des instances…',
    signInToPlay: 'Connectez-vous pour jouer.',
    lastPlayed: 'Dernière session {date}',
    ramBadge: '{mb} Mo RAM',
    modsBadge: '{count} mods'
  },
  media: {
    openFolder: 'Ouvrir le dossier clips',
    refresh: 'Actualiser',
    replaysNote: 'Replays et clips apparaissent ici via Prime Client (config/primeclient/clips).'
  }
}
