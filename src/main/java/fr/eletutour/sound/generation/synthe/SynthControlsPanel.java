package fr.eletutour.sound.generation.synthe;

import fr.eletutour.sound.constant.AudioConstants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SynthControlsPanel extends JPanel {

    private final JSlider attackSlider, releaseSlider, pitchSlider, cutoffSlider, resonanceSlider;
    private final ButtonGroup waveformGroup;

    public SynthControlsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // --- Waveform Panel ---
        JPanel waveformPanel = new JPanel();
        waveformPanel.setBorder(BorderFactory.createTitledBorder("Waveform"));
        waveformGroup = new ButtonGroup();
        for (AudioConstants.Waveform w : AudioConstants.Waveform.values()) {
            JRadioButton waveButton = new JRadioButton(w.name());
            waveButton.setActionCommand(w.name());
            if (w == AudioConstants.Waveform.SINE) waveButton.setSelected(true);
            waveformGroup.add(waveButton);
            waveformPanel.add(waveButton);
        }

        // --- Sliders Panel ---
        JPanel sliderPanel = new JPanel(new GridLayout(0, 1));

        JPanel topSliderRow = new JPanel(new GridLayout(1, 0, 5, 5));
        attackSlider = createSlider("Attack", 1, 500, 10);
        releaseSlider = createSlider("Release", 1, 2000, 300);
        pitchSlider = createSlider("Pitch", -12, 12, 0);
        topSliderRow.add(attackSlider);
        topSliderRow.add(releaseSlider);
        topSliderRow.add(pitchSlider);

        JPanel bottomSliderRow = new JPanel(new GridLayout(1, 0, 5, 5));
        cutoffSlider = createSlider("Cutoff", 0, 100, 100);
        resonanceSlider = createSlider("Resonance", 0, 100, 20);
        bottomSliderRow.add(cutoffSlider);
        bottomSliderRow.add(resonanceSlider);

        sliderPanel.add(topSliderRow);
        sliderPanel.add(bottomSliderRow);

        add(waveformPanel);
        add(sliderPanel);
    }

    private JSlider createSlider(String name, int min, int max, int initial) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, initial);
        slider.setBorder(new TitledBorder(name));
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing((max - min) / 4);
        slider.setPaintLabels(true);
        return slider;
    }

    // --- Getters for synth parameters ---

    public double getAttackTime() { return Math.max(1, attackSlider.getValue()) / 1000.0; }
    public double getReleaseTime() { return Math.max(1, releaseSlider.getValue()) / 1000.0; }
    public int getPitchOffset() { return pitchSlider.getValue(); }
    public double getFilterCutoff() { return cutoffSlider.getValue() / 100.0; }
    public double getFilterResonance() { return resonanceSlider.getValue() / 100.0; }
    public AudioConstants.Waveform getSelectedWaveform() { return AudioConstants.Waveform.valueOf(waveformGroup.getSelection().getActionCommand()); }
}
