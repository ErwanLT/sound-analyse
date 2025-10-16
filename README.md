# Analyseur de Son

Ce projet est une collection d'outils d'analyse audio en temps réel écrits en Java. Il utilise l'API `javax.sound.sampled` pour capturer l'audio du microphone et effectue une Transformation de Fourier Rapide (FFT) pour analyser les fréquences.

Le projet contient deux applications principales :
1.  Un analyseur de fréquence en ligne de commande.
2.  Un visualiseur de spectre audio avec une interface graphique (GUI).

## Fonctionnalités

*   **Analyseur de Fréquence (CLI) :**
    *   Capture l'audio en temps réel depuis le microphone.
    *   Calcule la fréquence dominante et son intensité.
    *   Affiche les résultats en continu dans la console.
    *   Permet d'arrêter la capture et de sauvegarder l'enregistrement dans un fichier `enregistrement.wav`.

*   **Visualiseur de Spectre (GUI) :**
    *   Ouvre une fenêtre affichant le spectre de fréquences en direct.
    *   Utilise une échelle logarithmique pour une meilleure représentation des fréquences audibles.
    *   Les barres de fréquence changent de couleur (vert → jaune → rouge) en fonction de leur intensité.
    *   L'affichage est lissé pour une meilleure expérience visuelle.

## Technologies

*   Java (compilé avec la version 25)
*   Maven pour la gestion du projet
*   Java Swing pour l'interface graphique

## Prérequis

*   JDK 21 ou supérieur
*   Apache Maven

## Installation

Pour compiler le projet et installer les dépendances, exécutez la commande suivante à la racine du projet :

```bash
mvn clean install
```

## Utilisation

Vous pouvez lancer l'une des deux applications à l'aide de Maven.

### 1. Analyseur de Fréquence (Console)

Cette application affiche la fréquence sonore dominante détectée par votre micro.

**Lancement :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveFrequencyAnalyzerInterruptible"
```
L'analyse démarre immédiatement. Pour l'arrêter, retournez dans la console et appuyez sur la touche **Entrée**. Un fichier `enregistrement.wav` contenant la session audio sera créé à la racine du projet.

### 2. Visualiseur de Spectre (GUI)

Cette application ouvre une fenêtre qui affiche une visualisation en direct de l'audio capté par votre micro. Vous pouvez choisir le mode de visualisation en passant un argument au démarrage.

Les modes disponibles sont : `bars` (par défaut), `circle`, et `wave`.

**Lancement (Barres - par défaut) :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveAudioSpectrumVisualizer"
```
*Note : l'argument `bars` est optionnel (`-Dexec.args="bars"`) car c'est le mode par défaut.*

**Lancement (Cercle) :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveAudioSpectrumVisualizer" -Dexec.args="circle"
```

**Lancement (Onde) :**
```bash
mvn exec:java -Dexec.mainClass="fr.eletutour.sound.analyser.LiveAudioSpectrumVisualizer" -Dexec.args="wave"
```
Pour arrêter l'application, il suffit de fermer la fenêtre.
