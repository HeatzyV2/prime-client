# GitHub — Releases & mises à jour

Repo public : **[github.com/HeatzyV2/prime-client](https://github.com/HeatzyV2/prime-client)**

Aucun serveur Prime requis — GitHub héberge le code et les binaires.

## Publier une version

```powershell
# 1. Commit tes changements
git add .
git commit -m "Describe your changes"

# 2. Tag semver (déclenche la CI Release)
git tag v0.8.1
git push origin main
git push origin v0.8.1
```

La workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml) build automatiquement :

- **Prime Client** mod jar (`mc-1.21.11`)
- **Prime Launcher** installeur Windows (`.exe`)

Assets attachés à la GitHub Release du tag.

## Site web (GitHub Pages)

Le site marketing vit dans [`website/`](../website/). Déployé automatiquement via [`.github/workflows/pages.yml`](../.github/workflows/pages.yml).

| | |
|--|--|
| URL | `https://heatzyv2.github.io/prime-client/` |
| Dev local | `cd website && npm install && npm run dev` |
| Téléchargement | Bouton relié à `releases/latest` → `.exe` du launcher |

Active **Settings → Pages → GitHub Actions** sur le repo une première fois.

## Vérifier les màj in-game

Settings → **Check for updates** interroge l’API publique :

`GET api.github.com/repos/HeatzyV2/prime-client/releases/latest`

## Build local de l’installeur

```powershell
cd launcher
npm install
npm run dist
# Sortie : launcher/release/Prime-Launcher-Setup-0.8.0.exe
```

## Nom du repo

| GitHub | Valeur |
|--------|--------|
| Slug URL | `prime-client` |
| Nom affiché | Prime Client |
| Owner | `HeatzyV2` |
