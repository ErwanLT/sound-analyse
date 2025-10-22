package fr.eletutour.sound.generation.drum;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class VirtualDrumkit extends JFrame {

    private final List<DrumVoice> voices = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public VirtualDrumkit() {
        setTitle("Batterie Virtuelle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.DARK_GRAY);

        mainPanel.add(createLabel("B - Grosse Caisse (Kick)"));
        mainPanel.add(createLabel("N - Caisse Claire (Snare)"));
        mainPanel.add(createLabel("H - Hi-Hat Fermé"));
        mainPanel.add(createLabel("C - Cymbale"));

        add(mainPanel);
        setupKeyBindings(mainPanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(this::soundLoop).start();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        return label;
    }

    private void setupKeyBindings(JPanel panel) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        im.put(KeyStroke.getKeyStroke("pressed B"), "press_kick");
        am.put("press_kick", new DrumAction(DrumSound.KICK));

        im.put(KeyStroke.getKeyStroke("pressed N"), "press_snare");
        am.put("press_snare", new DrumAction(DrumSound.SNARE));

        im.put(KeyStroke.getKeyStroke("pressed H"), "press_hihat");
        am.put("press_hihat", new DrumAction(DrumSound.HIHAT_CLOSED));

        im.put(KeyStroke.getKeyStroke("pressed C"), "press_cymbal");
        am.put("press_cymbal", new DrumAction(DrumSound.CYMBAL));
    }

    private class DrumAction extends AbstractAction {
        private final DrumSound sound;
        DrumAction(DrumSound sound) { this.sound = sound; }

        @Override
        public void actionPerformed(ActionEvent e) {
            DrumVoice voice = new DrumVoice(sound, random);
            voices.add(voice);
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
                    for (DrumVoice voice : voices) {
                        mixedSample += voice.getNextSample();
                        if (!voice.isActive()) {
                            voices.remove(voice);
                        }
                    }
                    mixedSample = Math.max(-1.0, Math.min(1.0, mixedSample)); // Clipping
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
        SwingUtilities.invokeLater(VirtualDrumkit::new);
    }
}

enum DrumSound {
    KICK, SNARE, HIHAT_CLOSED, CYMBAL
}

class DrumVoice {
    private final DrumSound sound;
    private boolean active = true;
    private double position = 0;
    private double pitch;

    // For snare noise
    private double[] noiseBuffer;
    private int noisePosition = 0;

    // For cymbal
    private double[] cymbalFrequencies;
    private double[] cymbalPositions;

    DrumVoice(DrumSound sound, Random random) {
        this.sound = sound;
        this.pitch = 1.0;

        if (sound == DrumSound.SNARE || sound == DrumSound.HIHAT_CLOSED) {
            int bufferSize = (int) (AudioConstants.SAMPLE_RATE * 0.2); // 200ms of noise
            noiseBuffer = new double[bufferSize];
            for (int i = 0; i < bufferSize; i++) {
                noiseBuffer[i] = random.nextDouble() * 2 - 1;
            }
        } else if (sound == DrumSound.CYMBAL) {
            cymbalFrequencies = new double[6];
            cymbalPositions = new double[6];
            // Non-harmonic frequencies for a metallic sound
            cymbalFrequencies[0] = 220.5;
            cymbalFrequencies[1] = 340.1;
            cymbalFrequencies[2] = 410.3;
            cymbalFrequencies[3] = 550.6;
            cymbalFrequencies[4] = 680.8;
            cymbalFrequencies[5] = 815.2;
        }
    }

    boolean isActive() {
        return active;
    }

    double getNextSample() {
        if (!active) return 0.0;

        double sample = 0;
        double envelope = 0;
        double noise = 0;
        double duration = 0;

        switch (sound) {
            case KICK:
                duration = 0.15; // 150ms
                if (position > duration) {
                    active = false;
                    return 0.0;
                }
                envelope = Math.pow(1.0 - (position / duration), 2);
                pitch = 150.0 * Math.exp(-position * 35.0); // Fast pitch drop
                sample = Math.sin(position * 2 * Math.PI * pitch);
                break;

            case SNARE:
                duration = 0.15; // 150ms
                if (position > duration) {
                    active = false;
                    return 0.0;
                }
                envelope = Math.pow(1.0 - (position / duration), 3);
                double tone = Math.sin(position * 2 * Math.PI * 180.0);
                noise = noiseBuffer[noisePosition++];
                sample = (tone * 0.4) + (noise * 0.6);
                break;

            case HIHAT_CLOSED:
                duration = 0.05; // 50ms
                if (position > duration) {
                    active = false;
                    return 0.0;
                }
                envelope = Math.pow(1.0 - (position / duration), 2);
                noise = noiseBuffer[noisePosition++];
                // Simple high-pass filter on noise
                double previousNoise = (noisePosition > 1) ? noiseBuffer[noisePosition - 2] : 0.0;
                sample = noise - previousNoise;
                break;

            case CYMBAL:
                duration = 1.5; // 1.5 seconds
                if (position > duration) {
                    active = false;
                    return 0.0;
                }
                // Fast attack, long decay
                envelope = Math.pow(1.0 - (position / duration), 2);

                double cymbalSample = 0;
                for (int i = 0; i < cymbalFrequencies.length; i++) {
                    // Square wave for metallic sound
                    cymbalSample += Math.signum(Math.sin(cymbalPositions[i] * 2 * Math.PI));
                    cymbalPositions[i] += cymbalFrequencies[i] / AudioConstants.SAMPLE_RATE;
                    if (cymbalPositions[i] > 1.0) cymbalPositions[i] -= 1.0;
                }
                sample = (cymbalSample / cymbalFrequencies.length) * 0.5; // Reduce volume
                break;
        }

        position += 1.0 / AudioConstants.SAMPLE_RATE;
        return sample * envelope;
    }
}
