# Sound Tools - Analyse et Synthèse Audio

Ce projet est une collection d'outils audio en Java, permettant à la fois l'analyse de son en temps réel et la synthèse sonore interactive.

## Applications

Le projet contient quatre applications principales :

1.  **Analyseur de Fréquence (CLI) :** Capture l'audio du microphone et affiche la fréquence dominante en temps réel dans la console.
2.  **Visualiseur de Spectre (GUI) :** Affiche une visualisation graphique du spectre de fréquences de l'audio capté par le microphone.
3.  **Mini Synthétiseur (GUI) :** Transforme le clavier de l'ordinateur en un piano simple avec une visualisation des touches.
4.  **Générateur de Son (GUI) :** Un synthétiseur plus avancé pour générer des sons, choisir des formes d'onde et jouer des partitions.

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

Transforme votre clavier en piano. La disposition des touches est optimisée pour un clavier AZERTY.

*   **Touches blanches :** `q`, `s`, `d`, `f`, `g`, `h`, `j`, `k`, `l`, `m`
*   **Touches noires :** `z`, `e`, `t`, `y`, `u`, `i`, `o`


**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.generation.synthe.Synthesiser"
```

### 4. Générateur de Son (GUI)

Un synthétiseur avancé pour générer des sons et jouer des partitions.

**Fonctionnalités :**
*   Fréquence réglable via un curseur.
*   Choix de la forme d'onde (SINUS, CARRÉ, etc.).
*   Lecteur de partitions depuis les fichiers `.txt`.

**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.generation.SoundGenerator"
```