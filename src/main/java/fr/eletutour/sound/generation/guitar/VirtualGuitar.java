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

    // Standard Tuning EADGBe
    private static final double[] STRING_FREQUENCIES = {
        AudioConstants.noteFrequencies.get("E2"), // E String (thickest)
        AudioConstants.noteFrequencies.get("A2"), // A String
        AudioConstants.noteFrequencies.get("D3"), // D String
        AudioConstants.noteFrequencies.get("G3"), // G String
        AudioConstants.noteFrequencies.get("B3"), // B String
        AudioConstants.noteFrequencies.get("E4")  // e string (thinnest)
    };

    public VirtualGuitar() {
        setTitle("Guitare Virtuelle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        guitarPanel = new GuitarPanel();
        add(guitarPanel);

        setupKeyBindings();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(this::soundLoop).start();
    }

    private void setupKeyBindings() {
        JPanel contentPane = (JPanel) getContentPane();
        InputMap im = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = contentPane.getActionMap();

        char[] keys = {'H', 'G', 'F', 'D', 'S', 'Q'}; // Mapped from thin to thick string
        for (int i = 0; i < keys.length; i++) {
            im.put(KeyStroke.getKeyStroke("pressed " + keys[i]), "press_" + i);
            am.put("press_" + i, new StringAction(i));
        }
    }

    private class StringAction extends AbstractAction {
        private final int stringIndex;

        StringAction(int stringIndex) {
            this.stringIndex = stringIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Replace the old string sound with the new one if played again
            activeStrings.put(stringIndex, new GuitarString(STRING_FREQUENCIES[stringIndex]));
            guitarPanel.pluckString(stringIndex);
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
