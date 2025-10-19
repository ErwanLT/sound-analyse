package fr.eletutour.sound.analyser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Synthesiser extends JFrame {

    private final Map<Character, Double> keyToFreq;
    {
        keyToFreq = new HashMap<>();
        // Octave 4 - White Keys
        keyToFreq.put('q', AudioConstants.noteFrequencies.get("C4"));
        keyToFreq.put('s', AudioConstants.noteFrequencies.get("D4"));
        keyToFreq.put('d', AudioConstants.noteFrequencies.get("E4"));
        keyToFreq.put('f', AudioConstants.noteFrequencies.get("F4"));
        keyToFreq.put('g', AudioConstants.noteFrequencies.get("G4"));
        keyToFreq.put('h', AudioConstants.noteFrequencies.get("A4"));
        keyToFreq.put('j', AudioConstants.noteFrequencies.get("B4"));

        // Octave 4 - Black Keys
        keyToFreq.put('z', AudioConstants.noteFrequencies.get("C#4"));
        keyToFreq.put('e', AudioConstants.noteFrequencies.get("D#4"));
        keyToFreq.put('t', AudioConstants.noteFrequencies.get("F#4"));
        keyToFreq.put('y', AudioConstants.noteFrequencies.get("G#4"));
        keyToFreq.put('u', AudioConstants.noteFrequencies.get("A#4"));

        // Octave 5 - White Keys
        keyToFreq.put('k', AudioConstants.noteFrequencies.get("C5"));
        keyToFreq.put('l', AudioConstants.noteFrequencies.get("D5"));
        keyToFreq.put('m', AudioConstants.noteFrequencies.get("E5"));

        // Octave 5 - Black Keys
        keyToFreq.put('i', AudioConstants.noteFrequencies.get("C#5"));
        keyToFreq.put('o', AudioConstants.noteFrequencies.get("D#5"));
    }

    private final Set<Character> pressedKeys = new HashSet<>();
    private final PianoKeyboardPanel pianoKeyboard;
    private volatile AudioConstants.Waveform selectedWaveform = AudioConstants.Waveform.SINE;

    public Synthesiser() {
        setTitle("Mini Synthétiseur");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        pianoKeyboard = new PianoKeyboardPanel(pressedKeys);

        JPanel waveformPanel = new JPanel();
        waveformPanel.setBorder(BorderFactory.createTitledBorder("Waveform"));
        ButtonGroup waveformGroup = new ButtonGroup();
        for (AudioConstants.Waveform w : AudioConstants.Waveform.values()) {
            JRadioButton waveButton = new JRadioButton(w.name());
            waveButton.setActionCommand(w.name());
            if (w == selectedWaveform) {
                waveButton.setSelected(true);
            }
            waveButton.addActionListener(e -> selectedWaveform = AudioConstants.Waveform.valueOf(e.getActionCommand()));
            waveformGroup.add(waveButton);
            waveformPanel.add(waveButton);
        }

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pianoKeyboard, BorderLayout.CENTER);
        getContentPane().add(waveformPanel, BorderLayout.SOUTH);

        setupKeyBindings();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        setVisible(true);

        new Thread(this::soundLoop).start();
    }

    private void setupKeyBindings() {
        InputMap im = pianoKeyboard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = pianoKeyboard.getActionMap();

        for (char c : keyToFreq.keySet()) {
            char upperC = Character.toUpperCase(c);
            String pressedAction = "press_" + c;
            String releasedAction = "release_" + c;

            im.put(KeyStroke.getKeyStroke("pressed " + upperC), pressedAction);
            am.put(pressedAction, new KeyAction(c, true));

            im.put(KeyStroke.getKeyStroke("released " + upperC), releasedAction);
            am.put(releasedAction, new KeyAction(c, false));
        }
    }


    private class KeyAction extends AbstractAction {
        private final char keyChar;
        private final boolean isPress;

        public KeyAction(char keyChar, boolean isPress) {
            this.keyChar = keyChar;
            this.isPress = isPress;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (pressedKeys) {
                if (isPress) {
                    pressedKeys.add(keyChar);
                } else {
                    pressedKeys.remove(keyChar);
                }
            }
            pianoKeyboard.repaint();
        }
    }

    private void soundLoop() {
        try {
            AudioFormat af = new AudioFormat(AudioConstants.SAMPLE_RATE, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af);
            line.start();
            byte[] buffer = new byte[1];
            double t = 0;
            while (true) {
                double sample = 0;
                synchronized (pressedKeys) {
                    for (char k : pressedKeys) {
                        Double f = keyToFreq.get(k);
                        if (f != null) {
                            double angle = 2 * Math.PI * f * t / AudioConstants.SAMPLE_RATE;
                            double sampleValue = 0;
                            switch (selectedWaveform) {
                                case SINE -> sampleValue = Math.sin(angle);
                                case SQUARE -> sampleValue = Math.signum(Math.sin(angle));
                                case TRIANGLE -> sampleValue = (2.0 / Math.PI) * Math.asin(Math.sin(angle));
                                case SAWTOOTH -> {
                                    double normalizedAngle = angle % (2.0 * Math.PI);
                                    sampleValue = (normalizedAngle / Math.PI) - 1.0;
                                }
                            }
                            sample += sampleValue;
                        }
                    }
                }
                sample /= Math.max(1, pressedKeys.size()); // Avoid saturation
                buffer[0] = (byte) (sample * 127);
                line.write(buffer, 0, 1);
                t++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Synthesiser::new);
    }
}