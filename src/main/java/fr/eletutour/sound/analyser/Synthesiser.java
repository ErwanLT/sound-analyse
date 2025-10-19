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

    private static final int NUM_VOICES = 8;
    private final Voice[] voices;

    public Synthesiser() {
        setTitle("Mini Synthétiseur");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        voices = new Voice[NUM_VOICES];
        for (int i = 0; i < NUM_VOICES; i++) {
            voices[i] = new Voice();
        }

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

    private Voice findAvailableVoice() {
        for (Voice voice : voices) {
            if (voice.state == Voice.State.INACTIVE) {
                return voice;
            }
        }
        return null;
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
                    if (!pressedKeys.contains(keyChar)) {
                        Voice voice = findAvailableVoice();
                        if (voice != null) {
                            voice.press(keyChar, keyToFreq.get(keyChar));
                            pressedKeys.add(keyChar);
                        }
                    }
                } else {
                    pressedKeys.remove(keyChar);
                    for (Voice voice : voices) {
                        if (voice.key == keyChar) {
                            voice.release();
                        }
                    }
                }
            }
            pianoKeyboard.repaint();
        }
    }

    private void soundLoop() {
        try {
            AudioFormat af = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, true);
            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af, 4096);
            line.start();
            byte[] buffer = new byte[1024];

            while (true) {
                for (int i = 0; i < buffer.length / 2; i++) {
                    double mixedSample = 0;
                    for (Voice voice : voices) {
                        mixedSample += voice.getNextSample();
                    }

                    // Apply master volume and soft clipping
                    mixedSample *= 0.25; // Reduce volume to provide headroom
                    mixedSample = Math.tanh(mixedSample);

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

    private class Voice {
        private double frequency;
        private double position = 0.0;
        private char key = 0;
        private double currentAmplitude = 0.0;

        private State state = State.INACTIVE;
        private enum State { INACTIVE, ATTACK, DECAY, SUSTAIN, RELEASE }

        // ADSR times in seconds
        private static final double ATTACK_TIME = 0.05;
        private static final double DECAY_TIME = 0.1;
        private static final double SUSTAIN_LEVEL = 0.7;
        private static final double RELEASE_TIME = 0.3;

        // Rates calculated from times
        private static final double ATTACK_RATE = 1.0 / (ATTACK_TIME * AudioConstants.SAMPLE_RATE);
        private static final double DECAY_RATE = (1.0 - SUSTAIN_LEVEL) / (DECAY_TIME * AudioConstants.SAMPLE_RATE);
        private static final double RELEASE_RATE = SUSTAIN_LEVEL / (RELEASE_TIME * AudioConstants.SAMPLE_RATE);

        void press(char key, double frequency) {
            this.key = key;
            this.frequency = frequency;
            this.state = State.ATTACK;
            this.position = 0;
        }

        void release() {
            if (state != State.INACTIVE) {
                this.state = State.RELEASE;
            }
        }

        double getNextSample() {
            if (state == State.INACTIVE) return 0.0;

            switch (state) {
                case ATTACK:
                    currentAmplitude += ATTACK_RATE;
                    if (currentAmplitude >= 1.0) {
                        currentAmplitude = 1.0;
                        state = State.DECAY;
                    }
                    break;
                case DECAY:
                    currentAmplitude -= DECAY_RATE;
                    if (currentAmplitude <= SUSTAIN_LEVEL) {
                        currentAmplitude = SUSTAIN_LEVEL;
                        state = State.SUSTAIN;
                    }
                    break;
                case SUSTAIN:
                    break;
                case RELEASE:
                    currentAmplitude -= RELEASE_RATE;
                    if (currentAmplitude <= 0.0) {
                        currentAmplitude = 0.0;
                        state = State.INACTIVE;
                        key = 0;
                    }
                    break;
            }

            double angle = position * 2 * Math.PI;
            double sampleValue = switch (selectedWaveform) {
                case SINE -> Math.sin(angle);
                case SQUARE -> Math.signum(Math.sin(angle));
                case TRIANGLE -> (2.0 / Math.PI) * Math.asin(Math.sin(angle));
                case SAWTOOTH -> {
                    double normalizedAngle = (position * 2 * Math.PI) % (2.0 * Math.PI);
                    yield (normalizedAngle / Math.PI) - 1.0;
                }
            };

            position += frequency / AudioConstants.SAMPLE_RATE;
            if (position > 1.0) position -= 1.0;

            return sampleValue * currentAmplitude;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Synthesiser::new);
    }
}
