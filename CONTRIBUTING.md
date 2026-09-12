# Guide de contribution — EliteDuBatK 🤖

Bienvenue sur le dépôt du bot Discord des étudiants du **BUT Informatique de l'IUT de Montpellier** !

Ce projet a pour vocation d'administrer et d'animer le serveur Discord communautaire des étudiants, tout en constituant un projet formateur et collaboratif capable de **traverser les promotions et d'évoluer avec elles**.

Que tu sois en BUT 1, BUT 2 ou BUT 3, ta contribution est la bienvenue ! Ce document regroupe toutes les règles, conventions et bonnes pratiques pour que le développement reste propre, pérenne et sécurisé.

---

## Sommaire

1. [Philosophie & Gouvernance](#1-philosophie--gouvernance)
2. [Code de conduite & Éthique](#2-code-de-conduite--éthique)
3. [Sécurité & Confidentialité des données](#3-sécurité--confidentialité-des-données)
4. [Stack technique](#4-stack-technique)
5. [Workflow de contribution Git](#5-workflow-de-contribution-git)
6. [Normes de développement & Qualité](#6-normes-de-développement--qualité)
7. [Guide des labels d'Issues](#7-guide-des-labels-dissues)

---

## 1. Philosophie & Gouvernance

* **Pérennité inter-promotions :** Le bot appartient à la communauté étudiante du BUT Informatique. Il doit rester maintenable par les futures promotions : architecture claire, dépendances modernes et documentation soignée.
* **Prise de décision :** Le pouvoir de décision final (validation des issues, revue et fusion des Pull Requests) appartient exclusivement aux **membres de l'organisation GitHub** (administrateurs et mainteneurs de confiance).
* **Rôle d'administration :** Le bot assure des tâches critiques de modération et d'administration du serveur. Aucune modification ne doit dégrader ou compromettre ces fonctions vitales.

---

## 2. Code de conduite & Éthique

Toute contribution ou interaction sur ce dépôt doit respecter un cadre strict. Sont **formellement interdits** :

* Toute fonctionnalité facilitant ou incitant à la **triche scolaire**, au plagiat ou au contournement des évaluations.
* L'utilisation du bot pour du **spam**, des raids, des nuisances sonores ou textuelles.
* Le contournement des rôles et permissions Discord ou du système de modération.
* Tout comportement contraire au **règlement intérieur de l'IUT de Montpellier / Université de Montpellier**, aux **Conditions d'utilisation de Discord** ou à la **législation en vigueur**.

---

## 3. Sécurité & Confidentialité des données

### Signalement de vulnérabilités (Responsible Disclosure)

* **Failles CRITIQUES (`priority: critical`) et ÉLEVÉES (`priority: high`) :** ⚠️ **NE JAMAIS ouvrir d'issue publique !** Contacte immédiatement un administrateur ou membre de l'organisation GitHub en **message privé** (sur GitHub ou Discord) afin qu'un correctif soit déployé avant toute divulgation.
* **Bugs mineurs ou faiblesses faibles :** Peuvent être signalés via une Issue GitHub classique avec le label `type: security`.

### Données sensibles & Vie privée (RGPD)

Il est **strictement prohibé** de committer ou publier dans les issues, PRs ou logs :
* **Tokens et accès secrets :** Jeton de bot Discord (`BOT_TOKEN`), webhooks privés, identifiants API ou certificats de production.
* **Données personnelles d'étudiants :** Adresses e-mail (universitaires `@etu.umontpellier.fr` ou personnelles), identifiants ENT, numéros étudiants universitaires, numéros **INE**, ou toute donnée permettant d'identifier ou de tracer un étudiant sans son consentement explicite.
* **Base de données de production :** Le fichier réel `data.db` ne doit jamais être committé ni partagé publiquement.

---

## 4. Stack technique

Le projet s'appuie sur des technologies abordées dès les premières années du BUT Informatique :

* **Langage & Runtime :** Java 24 (compilation `source` / `target` 24) / Java 25 (Temurin) sous Docker.
* **Gestionnaire de build :** Apache Maven.
* **Conteneurisation :** Docker & Docker Compose (build multi-stage léger).
* **Persistance :** SQLite (base de données embarquée stockée en volume local).

---

## 5. Workflow de contribution Git

### Étape 1 : Passer par une Issue (Obligatoire)

Avant d'écrire la moindre ligne de code, **ouvre une Issue** (ou échange sur une Issue existante) pour exposer ton idée ou le bug rencontré. Une contribution sans issue validée au préalable par l'équipe risque d'être refusée.

### Étape 2 : Créer sa branche

Toutes les contributions externes se font depuis un fork ou une branche dédiée issue de la branche `dev` :

* `main` : **Code de production uniquement.** Ne jamais soumettre de PR directement vers `main`.
* `dev` : **Branche d'intégration principale.** Toutes les PRs doivent cibler `dev`.

**Convention de nommage des branches :**
* Fonctionnalité : `feat/nom-fonctionnalite`
* Correction de bug : `fix/nom-du-bug`
* Refactoring : `refactor/nom-amelioration`
* Documentation : `docs/sujet-traite`

### Étape 3 : Rédiger ses commits

Nous suivons la convention des **Conventional Commits**, avec les messages rédigés **en français** :

* `feat: ajout de la commande de gestion des groupes de TP`
* `fix: correction du parsing des options de la commande /edt`
* `docs: mise à jour des étapes d'installation locale`
* `refactor: simplification de l'accès à la base sqlite`
* `test: ajout des tests unitaires pour le parser de configuration`

*(Le rebasage interactif est facultatif, mais veillez à garder un historique propre et compréhensible).*

### Étape 4 : Ouvrir une Pull Request (PR)

* Cible obligatoirement la branche **`dev`**.
* Lie l'issue associée dans la description (ex. : `Closes #12`).
* Décris précisément tes changements et les tests réalisés.
* Attends la revue et la validation par un membre de l'organisation GitHub.

---

## 6. Normes de développement & Qualité

### Langues et Documentation

* **Langue du code :** Tout le code source est **intégralement écrit en anglais** (noms de classes, interfaces, méthodes, attributs, variables).
* **Commentaires :** L'objectif premier est d'écrire un code source tellement clair et explicite qu'il se passe de commentaires. Si des explications sont nécessaires, les commentaires Doxygen pour documenter les classes et méthodes sont acceptés. Ils doivent alors être rédigés en **français**.
* **Langue des commits et PRs :** Rédigés en **français**.

### Utilisation de l'Intelligence Artificielle

L'utilisation d'assistants IA (Copilot, ChatGPT, Claude, etc.) pour générer du code est **tolérée**. Cependant, le code produit doit strictement respecter nos principes architecturaux, être lisible, relu de manière critique et couvert par des tests. Nous nous réservons le droit de refuser une Pull Request si la qualité du code généré n'est pas au niveau d'exigence attendu.

### Modélisation & UML

Un petit diagramme UML (diagramme de classes, d'états, de cas d'usage) pour appuyer la conception d'une PR ou d'une Issue n'est pas obligatoire mais reste **toujours très apprécié**. L'utilisation du format PlantUML est d'ailleurs recommandée pour une intégration native au format texte.

### Architecture & Robustesse (SESE & Fail-safe)

* **Single-Entry, Single-Exit (SESE) :** Il est impératif d'implémenter ce principe pour limiter les points de sortie multiples (seule exception : la levée d'Exceptions).
* **Approche Fail-Safe (pessimiste) :** Les variables de retour doivent être initialisées avec leurs valeurs d'échec ou de blocage, et ne doivent être modifiées vers le cas nominal que si toutes les conditions sont réunies.
* **Code Branchless :** Limitez au maximum la multiplication des blocs `else` et des sauts conditionnels.

**Exemple d'implémentation attendue en Java :**
```java
public InteractionResponse handleStudentCommand(CommandEvent event) {
    // 1. Initialisation pessimiste (fail-safe)
    InteractionResponse finalResponse = InteractionResponse.error("Accès refusé ou requête invalide.");

    // 2. Évaluation des conditions de passage (branchless-like)
    boolean isEventValid = (event != null && event.hasArgs());
    boolean isUserAuthorized = isEventValid && event.getUser().hasRole("ETUDIANT");

    // 3. Cas nominal unique : on ne modifie le retour que si tout est validé
    if (isUserAuthorized) {
        finalResponse = processCommandData(event.getArgs());
    }

    // 4. Point de sortie unique (SESE)
    return finalResponse;
}
```

* **Logs & Débogage :** Utiliser exclusivement le logger (Logback via SLF4J). Aucun `System.out.println` en production. Ne pas logger de données sensibles.
* **Tests unitaires :** Tout nouveau module logique ou utilitaire doit être accompagné de tests JUnit 5.

---

## 7. Guide des labels d'Issues

### Types

* `type: bug` : Dysfonctionnement avéré ou comportement inattendu du bot.
* `type: feature` : Nouvelle commande ou ajout fonctionnel majeur.
* `type: enhancement` : Amélioration d'une fonctionnalité existante ou optimisation.
* `type: security` : Problème de sécurité non critique (les failles critiques/high se traitent en MP).
* `type: documentation` : Amélioration ou mise à jour de la documentation technique.
* `type: question` : Demande d'information ou d'aide sur l'architecture.

### Priorités

* `priority: critical` : Crash du bot ou blocage complet (traitement immédiat).
* `priority: high` : Commande majeure défaillante touchant de nombreux utilisateurs.
* `priority: medium` : Bug standard avec contournement ou fonctionnalité utile prévue.
* `priority: low` : Retouche mineure, coquille ou détail cosmétique.
