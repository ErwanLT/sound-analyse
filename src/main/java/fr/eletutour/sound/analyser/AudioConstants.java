package fr.eletutour.sound.analyser;

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
}
