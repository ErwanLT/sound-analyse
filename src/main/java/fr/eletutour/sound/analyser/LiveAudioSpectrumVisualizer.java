package fr.eletutour.sound.analyser;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveAudioSpectrumVisualizer extends JPanel {

    /**
     * Définit le mode de visualisation à utiliser.
     */
    public enum VisualizationMode {
        BARS, CIRCLE, WAVE;

        /**
         * Convertit une chaîne de caractères en mode de visualisation.
         * @param s La chaîne (ex: "bars", "circle"). Insensible à la casse.
         * @return Le VisualizationMode correspondant, ou BARS par défaut.
         */
        public static VisualizationMode fromString(String s) {
            if (s == null) return BARS;
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return BARS; // Mode par défaut si la chaîne est invalide
            }
        }
    }

    private final VisualizationMode mode;
    private volatile double[] magnitudes = new double[AudioConstants.SAMPLE_COUNT / 2];
    private volatile double[] samples = new double[AudioConstants.SAMPLE_COUNT];
    private final double[] smoothedMagnitudes = new double[AudioConstants.SAMPLE_COUNT / 2];
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Constructeur principal qui initialise le panneau avec un mode de visualisation.
     * @param mode Le mode à utiliser pour le rendu.
     */
    public LiveAudioSpectrumVisualizer(VisualizationMode mode) {
        this.mode = mode;
        setBackground(Color.BLACK);
    }

    /**
     * Point d'entrée de l'application.
     * @param args Accepte un argument optionnel pour définir le mode de visualisation (bars, circle, wave).
     */
     static void main(String[] args) {
        VisualizationMode mode = (args.length > 0) ? VisualizationMode.fromString(args[0]) : VisualizationMode.BARS;

        JFrame frame = new JFrame("🌈 Spectre Audio Vivant — " + mode);
        LiveAudioSpectrumVisualizer panel = new LiveAudioSpectrumVisualizer(mode);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.add(panel);
        frame.setVisible(true);

        panel.startCapture();
    }

    public void startCapture() {
        Thread captureThread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format, AudioConstants.SAMPLE_COUNT * AudioConstants.BYTES_PER_SAMPLE * 2);
                line.start();

                byte[] buffer = new byte[AudioConstants.SAMPLE_COUNT * AudioConstants.BYTES_PER_SAMPLE];
                double[] currentSamples = new double[AudioConstants.SAMPLE_COUNT];

                IO.println("🎙️ Capture en cours... ferme la fenêtre pour arrêter.");

                while (running.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) continue;

                    int samplesRead = Math.min(AudioConstants.SAMPLE_COUNT, bytesRead / AudioConstants.BYTES_PER_SAMPLE);
                    for (int i = 0, s = 0; s < samplesRead; i += 2, s++) {
                        int low = buffer[i] & 0xFF;
                        int high = buffer[i + 1];
                        int value = (high << 8) | low;
                        currentSamples[s] = value / 32768.0;
                    }
                    if (samplesRead < AudioConstants.SAMPLE_COUNT) {
                        Arrays.fill(currentSamples, samplesRead, AudioConstants.SAMPLE_COUNT, 0.0);
                    }

                    // Copie thread-safe pour le rendu de la forme d'onde
                    this.samples = Arrays.copyOf(currentSamples, currentSamples.length);

                    double[] newMagnitudes = computeFFT(currentSamples);

                    // Lissage exponentiel pour les magnitudes
                    for (int i = 0; i < newMagnitudes.length; i++) {
                        smoothedMagnitudes[i] = 0.8 * smoothedMagnitudes[i] + 0.2 * newMagnitudes[i];
                    }

                    // Copie thread-safe pour le rendu du spectre
                    this.magnitudes = Arrays.copyOf(smoothedMagnitudes, smoothedMagnitudes.length);
                    Thread.sleep(10);
                }

                line.stop();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        captureThread.start();
        Timer timer = new Timer(33, e -> repaint());
        timer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running.set(false)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        switch (mode) {
            case CIRCLE:
                drawCircular(g2);
                break;
            case WAVE:
                drawWaveform(g2);
                break;
            case BARS:
            default:
                drawBarChart(g2);
                break;
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Mode: " + mode, 10, 20);
    }

    private void drawBarChart(Graphics2D g2) {
        if (magnitudes == null) return;
        int w = getWidth();
        int h = getHeight();
        int len = magnitudes.length;
        double max = Arrays.stream(magnitudes).max().orElse(1);
        double logBase = Math.log(len);

        for (int i = 1; i < len; i++) {
            double norm = magnitudes[i] / max;
            int barHeight = (int) (norm * h);
            double log_i = Math.log(i);
            int x = (int) (w * log_i / logBase);
            float hue = (float) (0.33 - norm * 0.33);
            g2.setColor(Color.getHSBColor(hue, 1.0f, 1.0f));
            g2.drawLine(x, h, x, h - barHeight);
        }
    }

    private void drawCircular(Graphics2D g2) {
        if (magnitudes == null) return;
        int w = getWidth();
        int h = getHeight();
        int centerX = w / 2;
        int centerY = h / 2;
        int baseRadius = Math.min(w, h) / 8;
        double maxMagnitude = Arrays.stream(magnitudes).max().orElse(1.0);

        int numMagnitudes = magnitudes.length;
        for (int i = 1; i < numMagnitudes / 2; i++) { // On ne dessine que la moitié pour éviter la symétrie
            double angle = 2 * Math.PI * i / (numMagnitudes / 2.0);
            double norm = magnitudes[i] / maxMagnitude;
            double lineLength = norm * (Math.min(w, h) / 3.0);

            int x1 = centerX + (int) (baseRadius * Math.cos(angle));
            int y1 = centerY + (int) (baseRadius * Math.sin(angle));
            int x2 = centerX + (int) ((baseRadius + lineLength) * Math.cos(angle));
            int y2 = centerY + (int) ((baseRadius + lineLength) * Math.sin(angle));

            float hue = (float) (0.33 - norm * 0.33);
            g2.setColor(Color.getHSBColor(hue, 1.0f, 1.0f));
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawWaveform(Graphics2D g2) {
        if (samples == null) return;
        int w = getWidth();
        int h = getHeight();
        int halfH = h / 2;
        double amplitudeMultiplier = 2.5; // Facteur d'amplification pour une meilleure visibilité

        for (int i = 0; i < samples.length - 1; i++) {
            // On calcule l'amplitude visuelle, qui peut dépasser 1.0
            double visualAmplitude = Math.abs(samples[i] * amplitudeMultiplier);
            // On la limite à 1.0 pour le calcul de la couleur, afin que la teinte soit correcte
            double clampedAmplitude = Math.min(1.0, visualAmplitude);

            // La couleur varie du vert (0.33) au rouge (0.0) en fonction de l'amplitude visuelle
            float hue = (float) (0.33 - (clampedAmplitude * 0.33));
            g2.setColor(Color.getHSBColor(hue, 1.0f, 1.0f));

            int x1 = (int) ((double) i / samples.length * w);
            double y1_raw = halfH - (samples[i] * halfH * amplitudeMultiplier);
            // On s'assure que la ligne ne dépasse pas les bords du panneau
            int y1 = Math.max(0, Math.min(h - 1, (int) y1_raw));

            int x2 = (int) ((double) (i + 1) / samples.length * w);
            double y2_raw = halfH - (samples[i + 1] * halfH * amplitudeMultiplier);
            int y2 = Math.max(0, Math.min(h - 1, (int) y2_raw));

            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private static double[] computeFFT(double[] input) {
        int n = input.length;
        double[] real = Arrays.copyOf(input, n);
        double[] imag = new double[n];

        int levels = 31 - Integer.numberOfLeadingZeros(n);
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - levels);
            if (j > i) {
                double tmpR = real[i];
                real[i] = real[j];
                real[j] = tmpR;
            }
        }

        for (int size = 2; size <= n; size <<= 1) {
            int half = size >> 1;
            double angle = -2 * Math.PI / size;
            double wStepR = Math.cos(angle);
            double wStepI = Math.sin(angle);

            for (int i = 0; i < n; i += size) {
                double wR = 1.0;
                double wI = 0.0;
                for (int j = 0; j < half; j++) {
                    int even = i + j;
                    int odd = i + j + half;
                    double rOdd = real[odd];
                    double iOdd = imag[odd];
                    double tR = rOdd * wR - iOdd * wI;
                    double tI = rOdd * wI + iOdd * wR;
                    real[odd] = real[even] - tR;
                    imag[odd] = imag[even] - tI;
                    real[even] += tR;
                    imag[even] += tI;

                    double newWR = wR * wStepR - wI * wStepI;
                    wI = wR * wStepI + wI * wStepR;
                    wR = newWR;
                }
            }
        }

        double[] mags = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            mags[i] = Math.hypot(real[i], imag[i]);
        }
        return mags;
    }
}