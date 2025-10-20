# Sound Tools - Analyse et Synthèse Audio

Ce projet est une collection d'outils audio en Java, permettant à la fois l'analyse de son en temps réel et la synthèse sonore interactive.

## Applications

Le projet contient quatre applications principales :

1.  **Analyseur de Fréquence (CLI) :** Capture l'audio du microphone et affiche la fréquence dominante en temps réel dans la console.
2.  **Visualiseur de Spectre (GUI) :** Affiche une visualisation graphique du spectre de fréquences de l'audio capté par le microphone.
3.  **Mini Synthétiseur (GUI) :** Un synthétiseur polyphonique complet avec des contrôles de mise en forme du son.
4.  **Générateur de Son (GUI) :** Un autre synthétiseur avancé pour générer des sons et jouer des partitions.

## Technologies

*   Java (compilé avec la version 21)
*   Maven pour la gestion du projet
*   Java Swing pour les interfaces graphiques

## Prérequis

*   JDK 21 ou supérieur
*   Apache Maven

## Installation

Pour compiler le projet et installer les dépendances, exécutez la commande suivante à la racine du projet :

```bash
mvn clean install
```

## Lancement des Applications

Vous pouvez lancer chacune des applications à l'aide de Maven.

### 1. Analyseur de Fréquence (Console)

Cette application affiche la fréquence sonore dominante détectée par votre micro.

**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveFrequencyAnalyzerInterruptible"
```
Pour l'arrêter, retournez dans la console et appuyez sur **Entrée**.

### 2. Visualiseur de Spectre (GUI)

Cette application affiche une visualisation en direct de l'audio. Plusieurs modes sont disponibles (`bars`, `circle`, `wave`).

**Lancement (mode "barres") :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveAudioSpectrumVisualizer" -Dexec.args="bars"
```

### 3. Mini Synthétiseur (GUI)

Un synthétiseur polyphonique soustractif doté d'un clavier de piano virtuel.

**Fonctionnalités :**
*   **Polyphonie :** 8 voix simultanées.
*   **Clavier :** Jouable avec un clavier AZERTY (`q,s,d...` pour les touches blanches, `z,e,t...` pour les noires).
*   **Oscillateurs :** Choix entre 4 formes d'onde (SINE, SQUARE, TRIANGLE, SAWTOOTH).
*   **Enveloppe ADSR :** Des curseurs permettent de régler les temps d'**Attaque** et de **Relâchement (Release)**.
*   **Filtre :** Un filtre passe-bas avec contrôle de la **Fréquence de coupure (Cutoff)** et de la **Résonance**.
*   **Pitch Control :** Un curseur pour transposer la hauteur des notes.

**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.Synthesiser"
```

<img width="613" height="423" alt="image" src="https://github.com/user-attachments/assets/28f10090-afd6-4998-ae4e-4ff3bd915ac6" />


### 4. Générateur de Son (GUI)

Un synthétiseur avancé pour générer des sons et jouer des partitions.

**Fonctionnalités :**
*   Fréquence réglable via un curseur.
*   Choix de la forme d'onde (SINUS, CARRÉ, etc.).
*   Lecteur de partitions depuis les fichiers `.txt`.

**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.SoundGenerator"
```
