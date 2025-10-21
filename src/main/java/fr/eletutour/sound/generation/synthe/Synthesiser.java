package fr.eletutour.sound.generation.synthe;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Synthesiser extends JFrame {

    private final Map<Character, Double> keyToFreq;
    {
        keyToFreq = new HashMap<>();
        keyToFreq.put('q', AudioConstants.noteFrequencies.get("C4"));
        keyToFreq.put('s', AudioConstants.noteFrequencies.get("D4"));
        keyToFreq.put('d', AudioConstants.noteFrequencies.get("E4"));
        keyToFreq.put('f', AudioConstants.noteFrequencies.get("F4"));
        keyToFreq.put('g', AudioConstants.noteFrequencies.get("G4"));
        keyToFreq.put('h', AudioConstants.noteFrequencies.get("A4"));
        keyToFreq.put('j', AudioConstants.noteFrequencies.get("B4"));
        keyToFreq.put('z', AudioConstants.noteFrequencies.get("C#4"));
        keyToFreq.put('e', AudioConstants.noteFrequencies.get("D#4"));
        keyToFreq.put('t', AudioConstants.noteFrequencies.get("F#4"));
        keyToFreq.put('y', AudioConstants.noteFrequencies.get("G#4"));
        keyToFreq.put('u', AudioConstants.noteFrequencies.get("A#4"));
        keyToFreq.put('k', AudioConstants.noteFrequencies.get("C5"));
        keyToFreq.put('l', AudioConstants.noteFrequencies.get("D5"));
        keyToFreq.put('m', AudioConstants.noteFrequencies.get("E5"));
        keyToFreq.put('i', AudioConstants.noteFrequencies.get("C#5"));
        keyToFreq.put('o', AudioConstants.noteFrequencies.get("D#5"));
    }

    private final Set<Character> pressedKeys = new HashSet<>();
    private final PianoKeyboardPanel pianoKeyboard;
    private final SynthControlsPanel controlsPanel;

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
        controlsPanel = new SynthControlsPanel();

        getContentPane().add(pianoKeyboard, BorderLayout.CENTER);
        getContentPane().add(controlsPanel, BorderLayout.SOUTH);

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
            im.put(KeyStroke.getKeyStroke("pressed " + upperC), "press_" + c);
            am.put("press_" + c, new KeyAction(c, true));
            im.put(KeyStroke.getKeyStroke("released " + upperC), "release_" + c);
            am.put("release_" + c, new KeyAction(c, false));
        }
    }

    private Voice findAvailableVoice() {
        Voice oldestReleased = null;
        for (Voice voice : voices) {
            if (voice.state == Voice.State.INACTIVE) return voice;
            if (voice.state == Voice.State.RELEASE) {
                if (oldestReleased == null || voice.stateChangeTime < oldestReleased.stateChangeTime) {
                    oldestReleased = voice;
                }
            }
        }
        return oldestReleased;
    }

    private class KeyAction extends AbstractAction {
        private final char keyChar;
        private final boolean isPress;
        public KeyAction(char keyChar, boolean isPress) { this.keyChar = keyChar; this.isPress = isPress; }
        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (pressedKeys) {
                if (isPress) {
                    if (!pressedKeys.contains(keyChar)) {
                        Voice voice = findAvailableVoice();
                        if (voice != null) {
                            double baseFreq = keyToFreq.get(keyChar);
                            double finalFreq = baseFreq * Math.pow(2, controlsPanel.getPitchOffset() / 12.0);
                            voice.press(keyChar, finalFreq);
                            pressedKeys.add(keyChar);
                        }
                    }
                } else {
                    pressedKeys.remove(keyChar);
                    for (Voice voice : voices) {
                        if (voice.key == keyChar) voice.release();
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
                    mixedSample *= 0.25;
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
        private double frequency, position = 0.0, currentAmplitude = 0.0, releaseStartAmplitude = 0.0;
        private char key = 0;
        private long stateChangeTime = 0;
        private State state = State.INACTIVE;
        private enum State { INACTIVE, ATTACK, DECAY, SUSTAIN, RELEASE }
        private double low = 0.0, band = 0.0;

        void press(char key, double frequency) {
            this.key = key; this.frequency = frequency; this.state = State.ATTACK;
            this.position = 0; this.stateChangeTime = System.nanoTime();
            this.low = 0.0; this.band = 0.0;
        }

        void release() {
            if (state != State.INACTIVE) {
                this.state = State.RELEASE;
                this.releaseStartAmplitude = currentAmplitude;
                this.stateChangeTime = System.nanoTime();
            }
        }

        double getNextSample() {
            if (state == State.INACTIVE) return 0.0;

            double timeInState = (System.nanoTime() - stateChangeTime) / 1_000_000_000.0;

            // ADSR
            double attackTime = controlsPanel.getAttackTime();
            double releaseTime = controlsPanel.getReleaseTime();
            double sustainLevel = 0.7; // Hardcoded for now
            double decayTime = 0.1; // Hardcoded for now

            switch (state) {
                case ATTACK:
                    if (attackTime <= 0) { currentAmplitude = 1.0; state = State.DECAY; stateChangeTime = System.nanoTime(); break; }
                    if (timeInState >= attackTime) { currentAmplitude = 1.0; state = State.DECAY; stateChangeTime = System.nanoTime(); } 
                    else { currentAmplitude = timeInState / attackTime; }
                    break;
                case DECAY:
                    if (decayTime <= 0) { currentAmplitude = sustainLevel; state = State.SUSTAIN; break; }
                    if (timeInState >= decayTime) { currentAmplitude = sustainLevel; state = State.SUSTAIN; } 
                    else { currentAmplitude = 1.0 - (1.0 - sustainLevel) * (timeInState / decayTime); }
                    break;
                case SUSTAIN:
                    currentAmplitude = sustainLevel;
                    break;
                case RELEASE:
                    if (releaseTime <= 0) { currentAmplitude = 0.0; state = State.INACTIVE; key = 0; break; }
                    if (timeInState >= releaseTime) { currentAmplitude = 0.0; state = State.INACTIVE; key = 0; } 
                    else { currentAmplitude = releaseStartAmplitude * (1.0 - (timeInState / releaseTime)); }
                    break;
            }

            // Waveform
            double sampleValue = switch (controlsPanel.getSelectedWaveform()) {
                case SINE -> Math.sin(position * 2 * Math.PI);
                case SQUARE -> Math.signum(Math.sin(position * 2 * Math.PI));
                case TRIANGLE -> (2.0 / Math.PI) * Math.asin(Math.sin(position * 2 * Math.PI));
                case SAWTOOTH -> (position * 2.0) - 1.0;
            };

            // Filter
            double cutoff = 20000.0 * Math.pow(controlsPanel.getFilterCutoff(), 3);
            double f = 2 * Math.sin(Math.PI * Math.min(0.25, cutoff / (AudioConstants.SAMPLE_RATE * 2)));
            double q = 1.0 - controlsPanel.getFilterResonance();

            low = low + f * band;
            double high = sampleValue - low - q * band;
            band = f * high + band;

            double filteredSample = low;

            position += frequency / AudioConstants.SAMPLE_RATE;
            if (position > 1.0) position -= 1.0;

            return filteredSample * currentAmplitude;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Synthesiser::new);
    }
}