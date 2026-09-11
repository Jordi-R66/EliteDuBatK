# EliteDuBatK

## Politique de conception

IA : On s'en balec, tant que c'est manuellement vérifié et compris de tous

### Events

Chaque Event est dans sa propre classe.
Par exemple, Une classe pour la suppression de messages n'écoutera aucun autre événement que les suppressions de messages.

## Configuration

La configuration est lue dans `config.json` (dossier courant), ou dans le fichier indiqué par la variable d'environnement `CONFIG_PATH`. Voir `example.config.json`.

```json
{
	"token": "TOKEN_DU_BOT",
	"databasePath": "data.db",
	"adminRole": "ID_DU_ROLE",
	"groups": {
		"nom-du-groupe": ["ID_ROLE_1", "ID_ROLE_2"]
	},
	"channels": {
		"voice_channel": "ID_DU_SALON",
		"message_delete_channel": "ID_DU_SALON",
		"lock_channel": "ID_DU_SALON"
	}
}
```

| Clé | Description |
|---|---|
| `token` | Token du bot (Discord Developer Portal). Ne jamais le commiter. |
| `databasePath` | Chemin du fichier SQLite. Un chemin relatif part du dossier courant du bot (voir [Docker](#docker)). |
| `adminRole` | ID du rôle autorisé à utiliser `/lock`. Les membres avec la permission Administrateur y ont aussi accès. Pendant un verrouillage, ce rôle garde le droit d'écrire. |
| `groups` | Groupes de rôles pour la commande de ping : nom du groupe → liste d'IDs de rôles. |
| `channels` | Salons de logs. Plusieurs clés peuvent pointer vers le même salon ; une clé absente ou vide désactive ce log (avertissement au démarrage). |

Salons de logs (`channels`) :

| Clé | Contenu |
|---|---|
| `voice_channel` | Connexions, déconnexions et changements de salon vocal |
| `message_delete_channel` | Messages supprimés (un par un ou en masse) |
| `lock_channel` | Verrouillages et déverrouillages de salon (`/lock`) |

Pour copier un ID : activer le mode développeur (Paramètres → Avancés), puis clic droit → « Copier l'identifiant ».

Le bot a besoin de l'intent privilégié **Message Content** (Discord Developer Portal → Bot), sinon il ne peut pas se connecter.

## Docker

```sh
docker compose up -d
```

- `./config.json` est monté en lecture sur `/app/config.json`.
- `./data` est monté sur `/app/data`, qui est aussi le dossier courant du bot : avec `"databasePath": "data.db"`, la base est donc `./data/data.db` sur l'hôte et survit à la recréation du conteneur.
- Il faut monter le **dossier** et pas seulement le fichier `data.db` : SQLite crée et supprime des fichiers temporaires (`data.db-journal`) à côté de la base, ce qui est impossible sur un fichier monté seul. De plus, si le fichier n'existe pas sur l'hôte, Docker crée un dossier à sa place.
- Garder un `databasePath` relatif. Un chemin absolu hors de `/app/data` placerait la base hors du volume.

## Documentation Libs

[JDA](https://docs.jda.wiki/net/dv8tion/jda/api/hooks/ListenerAdapter.html#onMessageDelete(net.dv8tion.jda.api.events.message.MessageDeleteEvent))
