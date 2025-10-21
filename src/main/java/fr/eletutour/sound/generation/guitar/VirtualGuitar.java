package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualGuitar extends JFrame {

    private final GuitarPanel guitarPanel;
    private final Map<Integer, GuitarString> activeStrings = new ConcurrentHashMap<>();
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

    private String[] tuningNames = tuningPresets.keySet().toArray(new String[0]);
    private int currentTuningIndex = 0; // Index for tuningNames
    private float distortionLevel = 0.0f; // 0.0 to 1.0

    public VirtualGuitar() {
        setTitle("Guitare Virtuelle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        guitarPanel = new GuitarPanel();
        add(guitarPanel);

        setupKeyBindings();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(this::soundLoop).start();
    }

    private void setupKeyBindings() {
        JPanel contentPane = (JPanel) getContentPane();
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
                if (capoFret < 12) { // Max 12 frets for capo
                    capoFret++;
                    guitarPanel.setCapoFret(capoFret);
                }
            }
        });

        im.put(KeyStroke.getKeyStroke("LEFT"), "capo_down");
        am.put("capo_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (capoFret > 0) {
                    capoFret--;
                    guitarPanel.setCapoFret(capoFret);
                }
            }
        });

        // Chord definitions: { {stringIndex, fretNumber}, ... }
        // Fret number -1 means muted string
        Map<Character, int[][]> chordDefinitions = Map.of(
            'A', new int[][]{{0, -1}, {1, 3}, {2, 2}, {3, 0}, {4, 1}, {5, 0}}, // C Major
            'Z', new int[][]{{0, 3}, {1, 2}, {2, 0}, {3, 0}, {4, 0}, {5, 3}}, // G Major
            'E', new int[][]{{0, -1}, {1, -1}, {2, 0}, {3, 2}, {4, 3}, {5, 2}}, // D Major
            'R', new int[][]{{0, 0}, {1, 2}, {2, 2}, {3, 0}, {4, 0}, {5, 0}}  // E Minor
        );

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
                currentTuningIndex = (currentTuningIndex + 1) % tuningNames.length;
                currentStringFrequencies = tuningPresets.get(tuningNames[currentTuningIndex]);
                // Optionally, clear active strings to prevent old frequencies from lingering
                activeStrings.clear();
                System.out.println("Tuning changed to: " + tuningNames[currentTuningIndex]);
            }
        });

        // Distortion controls
        im.put(KeyStroke.getKeyStroke("pressed U"), "distortion_up");
        am.put("distortion_up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                distortionLevel = Math.min(1.0f, distortionLevel + 0.1f);
                System.out.println("Distortion: " + String.format("%.1f", distortionLevel));
            }
        });

        im.put(KeyStroke.getKeyStroke("pressed J"), "distortion_down");
        am.put("distortion_down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                distortionLevel = Math.max(0.0f, distortionLevel - 0.1f);
                System.out.println("Distortion: " + String.format("%.1f", distortionLevel));
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
            double originalFrequency = VirtualGuitar.this.currentStringFrequencies[stringIndex];
            double adjustedFrequency = originalFrequency * Math.pow(2, capoFret / 12.0);

            // Pluck the main string with full amplitude
            activeStrings.put(stringIndex, new GuitarString(adjustedFrequency));
            guitarPanel.pluckString(stringIndex);

            // Trigger sympathetic resonance in other strings
            for (int i = 0; i < NUM_STRINGS; i++) {
                if (i != stringIndex) {
                    double sympatheticOriginalFrequency = VirtualGuitar.this.currentStringFrequencies[i];
                    double sympatheticAdjustedFrequency = sympatheticOriginalFrequency * Math.pow(2, capoFret / 12.0);

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
                    double originalFrequency = VirtualGuitar.this.currentStringFrequencies[stringIndex];
                    // Adjust frequency based on fret number and capo
                    double adjustedFrequency = originalFrequency * Math.pow(2, (fretNumber + capoFret) / 12.0);

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
                        double sympatheticOriginalFrequency = VirtualGuitar.this.currentStringFrequencies[i];
                        double sympatheticAdjustedFrequency = sympatheticOriginalFrequency * Math.pow(2, capoFret / 12.0); // Sympathetic strings are affected by capo only
                        activeStrings.put(i, new GuitarString(sympatheticAdjustedFrequency, SYMPATHETIC_RESONANCE_FACTOR));
                    }
                }
            }
        }
    }

    private void soundLoop() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, true))) {
            line.open();
            line.start();
            byte[] buffer = new byte[1024];

            while (true) {
                for (int i = 0; i < buffer.length / 2; i++) {
                    double mixedSample = 0;

                    for (Map.Entry<Integer, GuitarString> entry : activeStrings.entrySet()) {
                        GuitarString string = entry.getValue();
                        mixedSample += string.getNextSample();
                        if (!string.isActive()) {
                            activeStrings.remove(entry.getKey());
                        }
                    }

                    // Apply distortion
                    if (distortionLevel > 0.0f) {
                        // Simple soft clipping using tanh
                        // The 'gain' factor amplifies the signal before tanh, increasing distortion
                        double gain = 1.0 + (distortionLevel * 5.0); // Adjust gain for desired distortion intensity
                        mixedSample = Math.tanh(mixedSample * gain) / Math.tanh(gain);
                    }

                    mixedSample = Math.max(-1.0, Math.min(1.0, mixedSample * 0.5)); // Reduce volume to prevent clipping
                    short pcmValue = (short) (mixedSample * Short.MAX_VALUE);
                    buffer[i * 2] = (byte) (pcmValue >> 8);
                    buffer[i * 2 + 1] = (byte) pcmValue;
                }
                line.write(buffer, 0, buffer.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VirtualGuitar::new);
    }
}
