<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHPlayerInteract

*Shift + clic droit sur un joueur exécute des commandes (ex. /infos), hors régions bloquées.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Shift + clic droit sur un joueur exécute des commandes (ex. /infos), hors régions bloquées.

Utilisé sur : **Opale**.

## Dépendances

- Obligatoires : `WorldGuard`
- Optionnelles : `Vault`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/shplayerinteract` | `/shpi` | Main command for SHPlayerInteract plugin | — |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shplayerinteract.use` | Allows users to interact with players using shift+right click | `op` |
| `shplayerinteract.reload` | Allows reloading the plugin configuration | `op` |

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHPlayerInteract/
└── <a href="src/main/resources/config.yml">config.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
