package fr.eletutour.sound.analyser;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class SoundGenerator extends JPanel {

    // Enum for waveform types
    private enum Waveform {
        SINE, SQUARE, TRIANGLE, SAWTOOTH
    }

    private final JSlider frequencySlider;
    private final JToggleButton onOffButton;
    private JLabel frequencyLabel;
    private final ButtonGroup waveformGroup;

    // Colors inspired by the image
    private static final Color BG_COLOR = new Color(242, 183, 117);
    private static final Color COMPONENT_BG_COLOR = new Color(227, 207, 178);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);
    private static final Color BORDER_COLOR = new Color(150, 150, 150);

    private volatile boolean isPlaying = false;

    public SoundGenerator() {
        super(new GridBagLayout());
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // --- Row 0: Play Button ---
        onOffButton = new JToggleButton("PLAY");
        styleBigButton(onOffButton);
        onOffButton.addActionListener(e -> {
            if (onOffButton.isSelected()) {
                onOffButton.setText("STOP");
                startPlayback();
            } else {
                onOffButton.setText("PLAY");
                stopPlayback();
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; // Don't stretch the button
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        add(onOffButton, gbc);

        // --- Row 1: Frequency Slider ---
        frequencySlider = new JSlider(JSlider.HORIZONTAL, 20, 8000, 440);
        frequencySlider.setBackground(BG_COLOR);
        frequencySlider.addChangeListener(e -> frequencyLabel.setText(frequencySlider.getValue() + " Hz"));
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        add(frequencySlider, gbc);

        // --- Row 2: Frequency Display and Controls ---
        JPanel frequencyDisplayPanel = new JPanel(new BorderLayout(10, 0));
        frequencyDisplayPanel.setBackground(BG_COLOR);

        JButton halfFreqButton = new JButton("x/2");
        styleMiniButton(halfFreqButton);
        halfFreqButton.addActionListener(e -> frequencySlider.setValue(Math.max(frequencySlider.getMinimum(), frequencySlider.getValue() / 2)));
        frequencyDisplayPanel.add(halfFreqButton, BorderLayout.WEST);

        frequencyLabel = new JLabel(frequencySlider.getValue() + " Hz", SwingConstants.CENTER);
        frequencyLabel.setForeground(TEXT_COLOR);
        frequencyLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        frequencyDisplayPanel.add(frequencyLabel, BorderLayout.CENTER);

        JButton doubleFreqButton = new JButton("x2");
        styleMiniButton(doubleFreqButton);
        doubleFreqButton.addActionListener(e -> frequencySlider.setValue(Math.min(frequencySlider.getMaximum(), frequencySlider.getValue() * 2)));
        frequencyDisplayPanel.add(doubleFreqButton, BorderLayout.EAST);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        add(frequencyDisplayPanel, gbc);

        // --- Row 3: Waveform Selection ---
        JPanel waveformPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        waveformPanel.setBackground(BG_COLOR);
        waveformGroup = new ButtonGroup();
        for (Waveform w : Waveform.values()) {
            JToggleButton waveButton = new JToggleButton(w.name());
            styleWaveButton(waveButton);
            waveButton.setActionCommand(w.name());
            if (w == Waveform.SINE) waveButton.setSelected(true);
            waveformGroup.add(waveButton);
            waveformPanel.add(waveButton);
        }

        // Wrapper panel to keep the waveform buttons centered and at their preferred size
        JPanel waveWrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        waveWrapperPanel.setBackground(BG_COLOR);
        waveWrapperPanel.add(waveformPanel);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(waveWrapperPanel, gbc);
    }

    private void styleBigButton(JToggleButton button) {
        button.setBackground(COMPONENT_BG_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1));
        button.setPreferredSize(new Dimension(120, 40));
    }

    private void styleMiniButton(JButton button) {
        button.setBackground(COMPONENT_BG_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1));
    }

    private void styleWaveButton(JToggleButton button) {
        button.setBackground(COMPONENT_BG_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(BORDER_COLOR, 1));
        button.setPreferredSize(new Dimension(90, 35));
    }

    private void startPlayback() {
        if (isPlaying) return;
        isPlaying = true;

        Thread playbackThread = new Thread(() -> {
            SourceDataLine line = null;
            try {
                AudioFormat audioFormat = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, true);
                line = AudioSystem.getSourceDataLine(audioFormat);
                line.open(audioFormat);
                line.start();

                double currentAngle = 0.0;
                byte[] buffer = new byte[1024];

                while (isPlaying) {
                    Waveform selectedWaveform = Waveform.valueOf(waveformGroup.getSelection().getActionCommand());
                    int frequency = frequencySlider.getValue();
                    double angleIncrement = (2.0 * Math.PI * frequency) / audioFormat.getSampleRate();

                    for (int i = 0; i < buffer.length; i += 2) {
                        short pcmValue = getPcmValue(currentAngle, selectedWaveform);
                        buffer[i] = (byte) (pcmValue >> 8);
                        buffer[i + 1] = (byte) pcmValue;
                        currentAngle += angleIncrement;
                    }
                    line.write(buffer, 0, buffer.length);
                }
                line.drain();
            } catch (LineUnavailableException ex) {
                SwingUtilities.invokeLater(() -> {
                    onOffButton.setSelected(false);
                    onOffButton.setText("PLAY");
                    JOptionPane.showMessageDialog(this, "Audio line unavailable.", "Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                if (line != null) line.close();
            }
        });
        playbackThread.start();
    }

    private static short getPcmValue(double currentAngle, Waveform selectedWaveform) {
        double normalizedAngle = currentAngle % (2.0 * Math.PI);

        double sampleValue = switch (selectedWaveform) {
            case SINE -> Math.sin(currentAngle);
            case SQUARE -> Math.signum(Math.sin(currentAngle));
            case TRIANGLE -> (2.0 / Math.PI) * Math.asin(Math.sin(currentAngle));
            case SAWTOOTH -> (normalizedAngle / Math.PI) - 1.0;
        };

        // Volume is at max (Short.MAX_VALUE)
        return (short) (sampleValue * Short.MAX_VALUE);
    }

    private void stopPlayback() {
        isPlaying = false;
    }

    static void main() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JFrame frame = new JFrame("Sound Synthesizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new SoundGenerator());
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}