package fr.eletutour.sound.analyser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Synthesiser extends JFrame implements KeyListener {

    private final Map<Character, Double> keyToFreq;
    {
        keyToFreq = new java.util.HashMap<>();
        // Octave 4 - Touches blanches
        keyToFreq.put('q', AudioConstants.noteFrequencies.get("C4")); // Do
        keyToFreq.put('s', AudioConstants.noteFrequencies.get("D4")); // Ré
        keyToFreq.put('d', AudioConstants.noteFrequencies.get("E4")); // Mi
        keyToFreq.put('f', AudioConstants.noteFrequencies.get("F4")); // Fa
        keyToFreq.put('g', AudioConstants.noteFrequencies.get("G4")); // Sol
        keyToFreq.put('h', AudioConstants.noteFrequencies.get("A4")); // La
        keyToFreq.put('j', AudioConstants.noteFrequencies.get("B4")); // Si

        // Octave 4 - Touches noires
        keyToFreq.put('z', AudioConstants.noteFrequencies.get("C#4"));
        keyToFreq.put('e', AudioConstants.noteFrequencies.get("D#4"));
        keyToFreq.put('t', AudioConstants.noteFrequencies.get("F#4"));
        keyToFreq.put('y', AudioConstants.noteFrequencies.get("G#4"));
        keyToFreq.put('u', AudioConstants.noteFrequencies.get("A#4"));

        // Octave 5 - Touches blanches
        keyToFreq.put('k', AudioConstants.noteFrequencies.get("C5"));
        keyToFreq.put('l', AudioConstants.noteFrequencies.get("D5"));
        keyToFreq.put('m', AudioConstants.noteFrequencies.get("E5"));

        // Octave 5 - Touches noires
        keyToFreq.put('i', AudioConstants.noteFrequencies.get("C#5"));
        keyToFreq.put('o', AudioConstants.noteFrequencies.get("D#5"));
    }

    private final Set<Character> pressedKeys = new HashSet<>();
    private boolean running = true;

    public Synthesiser() {
        setTitle("Mini Synthétiseur");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addKeyListener(this);
        setVisible(true);

        new Thread(this::soundLoop).start();
    }


    private void soundLoop() {
        try {
            AudioFormat af = new AudioFormat(AudioConstants.SAMPLE_RATE, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af);
            line.start();
            byte[] buffer = new byte[1];
            double t = 0;
            while (running) {
                double sample = 0;
                synchronized (pressedKeys) {
                    for (char k : pressedKeys) {
                        Double f = keyToFreq.get(k);
                        if (f != null)
                            sample += Math.sin(2 * Math.PI * f * t / AudioConstants.SAMPLE_RATE);
                    }
                }
                sample /= Math.max(1, pressedKeys.size()); // éviter la saturation
                buffer[0] = (byte) (sample * 127);
                line.write(buffer, 0, 1);
                t++;
            }
            line.drain();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (pressedKeys) {
            pressedKeys.add(e.getKeyChar());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (pressedKeys) {
            pressedKeys.remove(e.getKeyChar());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        new Synthesiser();
    }
}
