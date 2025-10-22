package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualGuitar extends JFrame {

    private int capoFret = 0; // 0 means no capo, 1 means capo on 1st fret, etc.

    // Standard Tuning EADGBe
    private double[] currentStringFrequencies = { // Changed to non-static and non-final
        AudioConstants.noteFrequencies.get("E2"), // E String (thickest)
        AudioConstants.noteFrequencies.get("A2"), // A String
        AudioConstants.noteFrequencies.get("D3"), // D String
        AudioConstants.noteFrequencies.get("G3"), // G String
        AudioConstants.noteFrequencies.get("B3"), // B String
        AudioConstants.noteFrequencies.get("E4")  // e string (thinnest)
    };

    private int currentTuningIndex = 0; // Index for tuningNames
    private float distortionLevel = 0.0f; // 0.0 to 1.0

    public VirtualGuitar() {
        setTitle("Guitare Virtuelle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        GuitarPanel guitarPanel = new GuitarPanel();
        add(guitarPanel);

        Map<Integer, GuitarString> activeStrings = new ConcurrentHashMap<>();
        GuitarKeyBindings guitarKeyBindings = new GuitarKeyBindings(this, guitarPanel, activeStrings);
        guitarKeyBindings.setupBindings((JPanel) getContentPane());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(new GuitarAudioProcessor(this, activeStrings)).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VirtualGuitar::new);
    }

    // Getters for GuitarKeyBindings to access VirtualGuitar's state
    public int getCapoFret() {
        return capoFret;
    }

    public void setCapoFret(int capoFret) {
        this.capoFret = capoFret;
    }

    public double[] getCurrentStringFrequencies() {
        return currentStringFrequencies;
    }

    public void setCurrentStringFrequencies(double[] currentStringFrequencies) {
        this.currentStringFrequencies = currentStringFrequencies;
    }

    public int getCurrentTuningIndex() {
        return currentTuningIndex;
    }

    public void setCurrentTuningIndex(int currentTuningIndex) {
        this.currentTuningIndex = currentTuningIndex;
    }

    public float getDistortionLevel() {
        return distortionLevel;
    }

    public void setDistortionLevel(float distortionLevel) {
        this.distortionLevel = distortionLevel;
    }
}

