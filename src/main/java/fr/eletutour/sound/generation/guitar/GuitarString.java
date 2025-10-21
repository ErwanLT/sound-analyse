package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class GuitarString {

    private final Queue<Double> ringBuffer;
    private final int capacity;
    private int tickCount = 0;
    private double envelope = 1.0;
    private double lastFilterOutput = 0.0; // For the improved filter

    public GuitarString(double frequency) {
        this.capacity = (int) (AudioConstants.SAMPLE_RATE / frequency);
        this.ringBuffer = new LinkedList<>();
        Random random = new Random();

        for (int i = 0; i < capacity; i++) {
            ringBuffer.add(random.nextDouble() - 0.5); // Initial burst of noise
        }
    }

    public double getNextSample() {
        if (ringBuffer.isEmpty()) {
            return 0.0;
        }

        double first = ringBuffer.poll();

        // A simple IIR low-pass filter for a warmer tone
        double newSample = (first + lastFilterOutput) * 0.5;
        lastFilterOutput = newSample;
        ringBuffer.add(newSample);

        // Apply a smooth exponential decay envelope, similar to the visual decay time constant
        envelope = Math.exp(-tickCount / (AudioConstants.SAMPLE_RATE * 0.4));

        tickCount++;
        return newSample * envelope;
    }

    public boolean isActive() {
        // The string is inactive when the envelope has faded out
        return envelope > 0.005;
    }

    public double getVibrationAmplitude() {
        // This method can be used by a visualizer if needed
        return envelope;
    }
}
