# Discord Rich Presence — Configuration

## Application ID (obligatoire)

| Champ | Valeur |
|-------|--------|
| **Application ID** | `1525574680994648174` |
| Portail | [Discord Developer Portal](https://discord.com/developers/applications/1525574680994648174) |

C’est le **seul ID** nécessaire. Pas de bot token, pas de client secret pour la Rich Presence IPC.

Le launcher et le mod in-game utilisent **le même Application ID** (défini dans `launcher/src/main/discord/types.ts` et `DiscordRpcService.java`).

> Si tu n’es pas propriétaire de cette application Discord, crée la tienne sur [discord.com/developers](https://discord.com/developers/applications) et remplace l’ID dans ces deux fichiers.

## Asset image (portail Discord)

Dans **Rich Presence → Art Assets**, upload le logo avec cette clé exacte :

| Asset key | Fichier |
|-----------|---------|
| `prime_logo` | `mc-1.21.11/src/main/resources/assets/primeclient/textures/gui/logo.png` |

Sans cet asset, la présence peut s’afficher sans image ou être rejetée.

## Prérequis côté utilisateur

1. **Discord Desktop** ouvert (pas le navigateur seul)
2. **Paramètres utilisateur Discord** → Activité de jeu → *Afficher l’activité actuelle* activé
3. **Launcher** → Paramètres → **Discord RPC** activé (par défaut : oui)
4. Relancer le launcher **après** avoir ouvert Discord si la RPC n’apparaît pas (retry auto toutes les 10 s)

## Comportement

| Contexte | Affichage |
|----------|-----------|
| Launcher ouvert (sans jeu) | `Prime Launcher` · `Joueur • Ready to play` |
| Téléchargement / lancement | `Launching Minecraft` |
| Jeu lancé | Launcher efface sa présence → le **mod** prend le relais |
| Jeu fermé | Retour `Prime Launcher` |

## Activer in-game

**Right Shift** → **Prime** → module **Discord RPC**

## Dépannage

- Console launcher (**Console** dans la sidebar) : cherche `Discord Rich Presence active` ou `Discord RPC unavailable`
- Vérifie l’App ID dans le message de log
- Les **boutons** RPC peuvent être refusés par Discord sur une app non vérifiée — le launcher réessaie sans boutons automatiquement

Application ID : **1525574680994648174**
