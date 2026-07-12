# Prime Client — Site web

Landing page marketing pour [Prime Client](https://github.com/HeatzyV2/prime-client).

**URL (GitHub Pages)** : `https://heatzyv2.github.io/prime-client/`

## Développement

```bash
cd website
npm install
npm run dev
# → http://localhost:4321/prime-client/
```

## Build

```bash
npm run build
# Sortie : website/dist/
```

## Déploiement

Le workflow `.github/workflows/pages.yml` déploie automatiquement sur **GitHub Pages** à chaque push sur `main` (dossier `website/`).

Dans les paramètres du repo GitHub : **Settings → Pages → Source : GitHub Actions**.

## Téléchargement .exe

Le bouton **Télécharger** interroge l'API GitHub :

`GET api.github.com/repos/HeatzyV2/prime-client/releases/latest`

Il cible le fichier `Prime-Launcher-Setup-*.exe` attaché à la release.

Pour publier une version téléchargeable :

```powershell
git tag v0.9.3
git push origin main
git push origin v0.9.3
```

La CI `release.yml` build le mod + le launcher `.exe` et les attache à la release.

## Thème

Design tokens alignés sur le launcher (`--prime-red: #e11d2e`, `--prime-black: #060608`).
