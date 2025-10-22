package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class GuitarKeyBindings {

    private final VirtualGuitar virtualGuitar;
    private final GuitarPanel guitarPanel;
    private final Map<Integer, GuitarString> activeStrings;

    // Chord definitions: { {stringIndex, fretNumber}, ... }
    // Fret number -1 means muted string
    private final Map<Character, int[][]> chordDefinitions = Map.of(
        'A', new int[][]{{0, -1}, {1, 3}, {2, 2}, {3, 0}, {4, 1}, {5, 0}}, // C Major
        'Z', new int[][]{{0, 3}, {1, 2}, {2, 0}, {3, 0}, {4, 0}, {5, 3}}, // G Major
        'E', new int[][]{{0, -1}, {1, -1}, {2, 0}, {3, 2}, {4, 3}, {5, 2}}, // D Major
        'R', new int[][]{{0, 0}, {1, 2}, {2, 2}, {3, 0}, {4, 0}, {5, 0}}  // E Minor
    );

    // Define tuning presets
    private final Map<String, double[]> tuningPresets = Map.of(
        "Standard", new double[]{
            AudioConstants.noteFrequencies.get("E2"),
            AudioConstants.noteFrequencies.get("A2"),
            AudioConstants.noteFrequencies.get("D3"),
            AudioConstants.noteFrequencies.get("G3"),
            AudioConstants.noteFrequencies.get("B3"),
            AudioConstants.noteFrequencies.get("E4")
        },
        "Drop D", new double[]{
            AudioConstants.noteFrequencies.get("D2"), // E string dropped to D
            AudioConstants.noteFrequencies.get("A2"),
            AudioConstants.noteFrequencies.get("D3"),
            AudioConstants.noteFrequencies.get("G3"),
            AudioConstants.noteFrequencies.get("B3"),
            AudioConstants.noteFrequencies.get("E4")
        },
        "Open G", new double[]{
            AudioConstants.noteFrequencies.get("D2"),
            AudioConstants.noteFrequencies.get("G2"),
            AudioConstants.noteFrequencies.get("D3"),
            AudioConstants.noteFrequencies.get("G3"),
            AudioConstants.noteFrequencies.get("B3"),
            AudioConstants.noteFrequencies.get("D4")
        }
    );

    private final String[] tuningNames;

    public GuitarKeyBindings(VirtualGuitar virtualGuitar, GuitarPanel guitarPanel, Map<Integer, GuitarString> activeStrings) {
        this.virtualGuitar = virtualGuitar;
        this.guitarPanel = guitarPanel;
        this.activeStrings = activeStrings;
        this.tuningNames = tuningPresets.keySet().toArray(new String[0]);
    }

    public void setupBindings(JPanel contentPane) {
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = contentPane.getActionMap();

        // Ergonomic AZERTY mapping from key to string index (0=E2 thickest, 5=E4 thinnest)
        // Keys are mapped from high pitch (thinnest string) to low pitch (thickest string)
        Map<Character, Integer> keyToString = Map.of(
            'Q', 5, // E4 (aigu)
            'S', 4, // B3
            'D', 3, // G3
            'F', 2, // D3
            'G', 1, // A2
            'H', 0  // E2 (grave)
        );

        for (Map.Entry<Character, Integer> entry : keyToString.entrySet()) {
            char key = entry.getKey();
            int stringIndex = entry.getValue();
            im.put(KeyStroke.getKeyStroke("pressed " + key), "press_" + key);
            am.put("press_" + key, new StringAction(stringIndex));
        }

        // Capo controls
        im.put(KeyStroke.getKeyStroke("RIGHT"), "capo_up");
        am.put("capo_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (virtualGuitar.getCapoFret() < 12) { // Max 12 frets for capo
                    virtualGuitar.setCapoFret(virtualGuitar.getCapoFret() + 1);
                    guitarPanel.setCapoFret(virtualGuitar.getCapoFret());
                }
            }
        });

        im.put(KeyStroke.getKeyStroke("LEFT"), "capo_down");
        am.put("capo_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (virtualGuitar.getCapoFret() > 0) {
                    virtualGuitar.setCapoFret(virtualGuitar.getCapoFret() - 1);
                    guitarPanel.setCapoFret(virtualGuitar.getCapoFret());
                }
            }
        });

        for (Map.Entry<Character, int[][]> entry : chordDefinitions.entrySet()) {
            char key = entry.getKey();
            int[][] chordShape = entry.getValue();
            im.put(KeyStroke.getKeyStroke("pressed " + key), "press_chord_" + key);
            am.put("press_chord_" + key, new ChordAction(chordShape));
        }

        // Tuning controls
        im.put(KeyStroke.getKeyStroke("pressed T"), "cycle_tuning");
        am.put("cycle_tuning", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                virtualGuitar.setCurrentTuningIndex((virtualGuitar.getCurrentTuningIndex() + 1) % tuningNames.length);
                virtualGuitar.setCurrentStringFrequencies(tuningPresets.get(tuningNames[virtualGuitar.getCurrentTuningIndex()]));
                // Optionally, clear active strings to prevent old frequencies from lingering
                activeStrings.clear();
                System.out.println("Tuning changed to: " + tuningNames[virtualGuitar.getCurrentTuningIndex()]);
            }
        });

        // Distortion controls
        im.put(KeyStroke.getKeyStroke("pressed U"), "distortion_up");
        am.put("distortion_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                virtualGuitar.setDistortionLevel(Math.min(1.0f, virtualGuitar.getDistortionLevel() + 0.1f));
                System.out.println("Distortion: " + String.format("%.1f", virtualGuitar.getDistortionLevel()));
            }
        });

        im.put(KeyStroke.getKeyStroke("pressed J"), "distortion_down");
        am.put("distortion_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                virtualGuitar.setDistortionLevel(Math.max(0.0f, virtualGuitar.getDistortionLevel() - 0.1f));
                System.out.println("Distortion: " + String.format("%.1f", virtualGuitar.getDistortionLevel()));
            }
        });
    }

    private static final int NUM_STRINGS = 6;

    private class StringAction extends AbstractAction {
        private final int stringIndex;
        private static final double SYMPATHETIC_RESONANCE_FACTOR = 0.15; // 15% of the energy

        StringAction(int stringIndex) {
            this.stringIndex = stringIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            double originalFrequency = virtualGuitar.getCurrentStringFrequencies()[stringIndex];
            double adjustedFrequency = originalFrequency * Math.pow(2, virtualGuitar.getCapoFret() / 12.0);

            // Pluck the main string with full amplitude
            activeStrings.put(stringIndex, new GuitarString(adjustedFrequency));
            guitarPanel.pluckString(stringIndex);

            // Trigger sympathetic resonance in other strings
            for (int i = 0; i < NUM_STRINGS; i++) {
                if (i != stringIndex) {
                    double sympatheticOriginalFrequency = virtualGuitar.getCurrentStringFrequencies()[i];
                    double sympatheticAdjustedFrequency = sympatheticOriginalFrequency * Math.pow(2, virtualGuitar.getCapoFret() / 12.0);

                    // Only replace if the string is not already ringing loudly
                    if (!activeStrings.containsKey(i) || activeStrings.get(i).getVibrationAmplitude() < 0.1) {
                        activeStrings.put(i, new GuitarString(sympatheticAdjustedFrequency, SYMPATHETIC_RESONANCE_FACTOR));
                    }
                }
            }
        }
    }

    private class ChordAction extends AbstractAction {
        private final int[][] chordShape;
        private static final double SYMPATHETIC_RESONANCE_FACTOR = 0.15;

        ChordAction(int[][] chordShape) {
            this.chordShape = chordShape;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int[] stringFret : chordShape) {
                int stringIndex = stringFret[0];
                int fretNumber = stringFret[1];

                if (fretNumber != -1) { // If not muted
                    double originalFrequency = virtualGuitar.getCurrentStringFrequencies()[stringIndex];
                    // Adjust frequency based on fret number and capo
                    double adjustedFrequency = originalFrequency * Math.pow(2, (fretNumber + virtualGuitar.getCapoFret()) / 12.0);

                    activeStrings.put(stringIndex, new GuitarString(adjustedFrequency));
                    guitarPanel.pluckString(stringIndex);
                }
            }

            // Trigger sympathetic resonance for all strings not explicitly played or muted
            for (int i = 0; i < NUM_STRINGS; i++) {
                boolean isPlayedInChord = false;
                for (int[] stringFret : chordShape) {
                    if (stringFret[0] == i) {
                        isPlayedInChord = true;
                        break;
                    }
                }

                if (!isPlayedInChord) {
                    // Only replace if the string is not already ringing loudly
                    if (!activeStrings.containsKey(i) || activeStrings.get(i).getVibrationAmplitude() < 0.1) {
                        double sympatheticOriginalFrequency = virtualGuitar.getCurrentStringFrequencies()[i];
                        double sympatheticAdjustedFrequency = sympatheticOriginalFrequency * Math.pow(2, virtualGuitar.getCapoFret() / 12.0); // Sympathetic strings are affected by capo only
                        activeStrings.put(i, new GuitarString(sympatheticAdjustedFrequency, SYMPATHETIC_RESONANCE_FACTOR));
                    }
                }
            }
        }
    }
}