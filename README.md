# Prime Client

Client Minecraft All-in-One : PvP, Performance, QoL, Création de contenu, Personnalisation.
Alternative professionnelle à Lunar / Badlion / Feather — 100 % légitime (visuel, confort, performance ; aucun cheat).

## Versions supportées

| Minecraft | Module Gradle | Java | Loom | Mappings |
|-----------|---------------|------|------|----------|
| 1.21.4    | `mc-1.21.4`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.5    | `mc-1.21.5`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.6    | `mc-1.21.6`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.7    | `mc-1.21.7`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.8    | `mc-1.21.8`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.9    | `mc-1.21.9`   | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.10   | `mc-1.21.10`  | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 1.21.11   | `mc-1.21.11`  | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 26.1      | `mc-26.1`     | 25   | `fabric-loom`       | Mojang (runtime natif) |
| 26.2      | `mc-26.2`     | 25   | `fabric-loom`       | Mojang (runtime natif) |

Un jar par version : `prime-client-<mc>-x.y.z.jar` (ex. `prime-client-1.21.8-2.6.0.jar`).
Le core commun est embarqué en Jar-in-Jar.

> Yarn s'arrête à 1.21.11. Toutes les couches utilisent mojmap : mêmes noms de
> classes dans le code source, adapters versionnés pour les diffs d'API.

## Structure

```
prime-client/
├── core/          Common Core — Java pur, zéro dépendance Minecraft
├── mc-1.21.*/     Couches Fabric 1.21.4 → 1.21.11
├── mc-26.*/       Couches Fabric 26.1 / 26.2
└── launcher/      Prime Launcher (Electron)
```

Règle d'or : le core ne touche jamais une classe Minecraft. Tout passe par les
interfaces `dev.primeclient.core.adapter.*`, implémentées dans chaque couche.
Détails : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

API serveurs partenaires (plugins) : [docs/SERVER_API.md](docs/SERVER_API.md).

## Build & run

```bash
# Toutes les couches 1.21.x (Java 21)
./gradlew :mc-1.21.4:build :mc-1.21.11:build

# Couches 26.x (toolchain Java 25 requis)
./gradlew :mc-26.1:build :mc-26.2:build

# Une seule version
./gradlew :mc-1.21.8:runClient
./gradlew :mc-26.2:runClient
```

Jars finaux dans `mc-*/build/libs/`.
Prérequis : JDK 21 minimum ; JDK 25 pour builder les cibles 26.x.

## Prime Launcher

Le launcher officiel Electron vit dans [`launcher/`](launcher/). Releases :
[GitHub](https://github.com/HeatzyV2/prime-client) · [docs/GITHUB.md](docs/GITHUB.md)

```bash
cd launcher
npm install
npm run dev
```

## Feuille de route

- [x] Phase 1–11 — Architecture, core, HUD, ClickGUI, modules, launcher, cosmetics
- [x] Multi-version — jars Fabric pour Minecraft 1.21.4 → 26.2
