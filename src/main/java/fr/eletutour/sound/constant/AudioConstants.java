package fr.eletutour.sound.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines shared audio constants for the sound analysis application.
 * This class cannot be instantiated.
 */
public final class AudioConstants {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AudioConstants() {}

    /**
     * The sample rate in Hz. This is the number of samples of audio carried per second.
     * 96000 Hz is a common high-quality sample rate.
     */
    public static final float SAMPLE_RATE = 96000f;

    /**
     * The number of samples to process in each FFT window. Must be a power of 2.
     */
    public static final int SAMPLE_COUNT = 1024;

    /**
     * The number of bytes per audio sample. For 16-bit audio, this is 2 bytes.
     */
    public static final int BYTES_PER_SAMPLE = 2;

    /**
     * Enum for the different types of waveforms that can be generated.
     */
    public enum Waveform {
        SINE, SQUARE, TRIANGLE, SAWTOOTH
    }

    /**
     * A map of musical note names to their corresponding frequencies in Hertz.
     */
    public static final Map<String, Double> noteFrequencies = new HashMap<>();

    static {
        // Pre-populate note frequencies for a standard 88-key piano (A0 to C8)
        noteFrequencies.put("A0", 27.50);
        noteFrequencies.put("A#0", 29.14);
        noteFrequencies.put("Bb0", 29.14);
        noteFrequencies.put("B0", 30.87);
        noteFrequencies.put("C1", 32.70);
        noteFrequencies.put("C#1", 34.65);
        noteFrequencies.put("Db1", 34.65);
        noteFrequencies.put("D1", 36.71);
        noteFrequencies.put("D#1", 38.89);
        noteFrequencies.put("Eb1", 38.89);
        noteFrequencies.put("E1", 41.20);
        noteFrequencies.put("F1", 43.65);
        noteFrequencies.put("F#1", 46.25);
        noteFrequencies.put("Gb1", 46.25);
        noteFrequencies.put("G1", 49.00);
        noteFrequencies.put("G#1", 51.91);
        noteFrequencies.put("Ab1", 51.91);
        noteFrequencies.put("A1", 55.00);
        noteFrequencies.put("A#1", 58.27);
        noteFrequencies.put("Bb1", 58.27);
        noteFrequencies.put("B1", 61.74);
        noteFrequencies.put("C2", 65.41);
        noteFrequencies.put("C#2", 69.30);
        noteFrequencies.put("Db2", 69.30);
        noteFrequencies.put("D2", 73.42);
        noteFrequencies.put("D#2", 77.78);
        noteFrequencies.put("Eb2", 77.78);
        noteFrequencies.put("E2", 82.41);
        noteFrequencies.put("F2", 87.31);
        noteFrequencies.put("F#2", 92.50);
        noteFrequencies.put("Gb2", 92.50);
        noteFrequencies.put("G2", 98.00);
        noteFrequencies.put("G#2", 103.83);
        noteFrequencies.put("Ab2", 103.83);
        noteFrequencies.put("A2", 110.00);
        noteFrequencies.put("A#2", 116.54);
        noteFrequencies.put("Bb2", 116.54);
        noteFrequencies.put("B2", 123.47);
        noteFrequencies.put("C3", 130.81);
        noteFrequencies.put("C#3", 138.59);
        noteFrequencies.put("Db3", 138.59);
        noteFrequencies.put("D3", 146.83);
        noteFrequencies.put("D#3", 155.56);
        noteFrequencies.put("Eb3", 155.56);
        noteFrequencies.put("E3", 164.81);
        noteFrequencies.put("F3", 174.61);
        noteFrequencies.put("F#3", 185.00);
        noteFrequencies.put("Gb3", 185.00);
        noteFrequencies.put("G3", 196.00);
        noteFrequencies.put("G#3", 207.65);
        noteFrequencies.put("Ab3", 207.65);
        noteFrequencies.put("A3", 220.00);
        noteFrequencies.put("A#3", 233.08);
        noteFrequencies.put("Bb3", 233.08);
        noteFrequencies.put("B3", 246.94);
        noteFrequencies.put("C4", 261.63);
        noteFrequencies.put("C#4", 277.18);
        noteFrequencies.put("Db4", 277.18);
        noteFrequencies.put("D4", 293.66);
        noteFrequencies.put("D#4", 311.13);
        noteFrequencies.put("Eb4", 311.13);
        noteFrequencies.put("E4", 329.63);
        noteFrequencies.put("F4", 349.23);
        noteFrequencies.put("F#4", 369.99);
        noteFrequencies.put("Gb4", 369.99);
        noteFrequencies.put("G4", 392.00);
        noteFrequencies.put("G#4", 415.30);
        noteFrequencies.put("Ab4", 415.30);
        noteFrequencies.put("A4", 440.00);
        noteFrequencies.put("A#4", 466.16);
        noteFrequencies.put("Bb4", 466.16);
        noteFrequencies.put("B4", 493.88);
        noteFrequencies.put("C5", 523.25);
        noteFrequencies.put("C#5", 554.37);
        noteFrequencies.put("Db5", 554.37);
        noteFrequencies.put("D5", 587.33);
        noteFrequencies.put("D#5", 622.25);
        noteFrequencies.put("Eb5", 622.25);
        noteFrequencies.put("E5", 659.26);
        noteFrequencies.put("F5", 698.46);
        noteFrequencies.put("F#5", 739.99);
        noteFrequencies.put("Gb5", 739.99);
        noteFrequencies.put("G5", 783.99);
        noteFrequencies.put("G#5", 830.61);
        noteFrequencies.put("Ab5", 830.61);
        noteFrequencies.put("A5", 880.00);
        noteFrequencies.put("A#5", 932.33);
        noteFrequencies.put("Bb5", 932.33);
        noteFrequencies.put("B5", 987.77);
        noteFrequencies.put("C6", 1046.50);
        noteFrequencies.put("C#6", 1108.73);
        noteFrequencies.put("Db6", 1108.73);
        noteFrequencies.put("D6", 1174.66);
        noteFrequencies.put("D#6", 1244.51);
        noteFrequencies.put("Eb6", 1244.51);
        noteFrequencies.put("E6", 1318.51);
        noteFrequencies.put("F6", 1396.91);
        noteFrequencies.put("F#6", 1479.98);
        noteFrequencies.put("Gb6", 1479.98);
        noteFrequencies.put("G6", 1567.98);
        noteFrequencies.put("G#6", 1661.22);
        noteFrequencies.put("Ab6", 1661.22);
        noteFrequencies.put("A6", 1760.00);
        noteFrequencies.put("A#6", 1864.66);
        noteFrequencies.put("Bb6", 1864.66);
        noteFrequencies.put("B6", 1975.53);
        noteFrequencies.put("C7", 2093.00);
        noteFrequencies.put("C#7", 2217.46);
        noteFrequencies.put("Db7", 2217.46);
        noteFrequencies.put("D7", 2349.32);
        noteFrequencies.put("D#7", 2489.02);
        noteFrequencies.put("Eb7", 2489.02);
        noteFrequencies.put("E7", 2637.02);
        noteFrequencies.put("F7", 2793.83);
        noteFrequencies.put("F#7", 2959.96);
        noteFrequencies.put("Gb7", 2959.96);
        noteFrequencies.put("G7", 3135.96);
        noteFrequencies.put("G#7", 3322.44);
        noteFrequencies.put("Ab7", 3322.44);
        noteFrequencies.put("A7", 3520.00);
        noteFrequencies.put("A#7", 3729.31);
        noteFrequencies.put("Bb7", 3729.31);
        noteFrequencies.put("B7", 3951.07);
        noteFrequencies.put("C8", 4186.01);
    }
}