package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class GuitarString {

    private final Queue<Double> ringBuffer;
    private final int capacity;
    private boolean active = true;
    private int tickCount = 0;

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
            active = false;
            return 0.0;
        }

        double first = ringBuffer.poll();
        double second = ringBuffer.peek() != null ? ringBuffer.peek() : 0.0;

        // Karplus-Strong update: average the first two samples
        double newSample = (first + second) * 0.5;
        ringBuffer.add(newSample);

        tickCount++;
        return newSample;
    }

    public boolean isActive() {
        // The sound naturally decays, but we can consider it inactive after a certain time
        return active && tickCount < AudioConstants.SAMPLE_RATE * 2; // 2 seconds lifetime
    }

    public double getVibrationAmplitude() {
        // Simple decay function for visual amplitude
        double time = tickCount / AudioConstants.SAMPLE_RATE;
        return Math.max(0, 1.0 - time / 1.5); // Decays over 1.5s
    }
}
