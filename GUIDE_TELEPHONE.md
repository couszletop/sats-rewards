# Compiler l'appli sans ordinateur (100% sur téléphone)

Pas d'ordinateur ? Pas de problème. GitHub compile l'appli pour toi,
gratuitement, et tu télécharges l'APK directement sur ton téléphone.

## Étape 1 : récupérer le projet

Télécharge le fichier zip du projet (demande le lien à Charlie) et
décompresse-le sur ton téléphone avec ton gestionnaire de fichiers.

## Étape 2 : créer un compte GitHub

1. Va sur **github.com** et crée un compte (gratuit).
2. Crée un nouveau dépôt : **+ > New repository**, nomme-le `sats-rewards`,
   en **public**, puis **Create repository**.

## Étape 3 : envoyer les fichiers

1. Dans ton dépôt, clique **Add file > Upload files**.
2. Envoie tout le contenu du dossier décompressé (les dossiers `android/`,
   `backend/`, `.github/` et les README). Astuce : passe ton navigateur en
   « version pour ordinateur » pour glisser-déposer des dossiers entiers.
3. Clique **Commit changes**.

Le fichier `.github/workflows/build-apk.yml` est déjà inclus : dès l'envoi,
GitHub lance la compilation automatiquement.

## Étape 4 : télécharger l'APK

1. Dans ton dépôt, va dans l'onglet **Actions**.
2. Clique sur le workflow en cours (pastille jaune) puis attends qu'il passe
   au vert (environ 5 à 10 minutes la première fois).
3. En bas de la page du run, section **Artifacts**, télécharge
   `sats-rewards-debug-apk`.
4. Ouvre le fichier téléchargé pour installer l'appli (autorise
   « installer des applications inconnues » si Android le demande).

## Étape 5 (optionnel) : tester avec le vrai backend

L'APK seule affiche l'interface avec des pubs de test. Pour tester tout le
circuit (pub → sats → retrait) :

1. Installe **Termux** depuis le Play Store (ou F-Droid).
2. Dans Termux :
   ```
   pkg install nodejs
   cd <dossier-du-projet>/backend
   npm install
   npm start
   ```
3. Avant d'envoyer les fichiers sur GitHub, modifie dans
   `android/app/src/main/java/com/satsrewards/app/Config.kt` :
   `BACKEND_URL = "http://127.0.0.1:3000"` (le backend tourne sur le
   téléphone lui-même).

## Et pour le Play Store ?

La version du Play Store demande une étape en plus : générer un **App Bundle
(.aab) signé**. C'est faisable aussi via GitHub Actions, mais il faut d'abord
créer une clé de signature. On fera ça ensemble quand l'APK de test te plaira.
