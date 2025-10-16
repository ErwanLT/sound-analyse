package fr.eletutour.sound.analyser;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveAudioSpectrumVisualizer extends JPanel {

    private double[] magnitudes = new double[AudioConstants.SAMPLE_COUNT / 2];
    private final double[] smoothedMagnitudes = new double[AudioConstants.SAMPLE_COUNT / 2];
    private final AtomicBoolean running = new AtomicBoolean(true);

    static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("🌈 Spectre Audio Vivant — Java");
        LiveAudioSpectrumVisualizer panel = new LiveAudioSpectrumVisualizer();
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
                double[] samples = new double[AudioConstants.SAMPLE_COUNT];

                IO.println("🎙️ Capture en cours... ferme la fenêtre pour arrêter.");

                while (running.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead <= 0) continue;

                    int samplesRead = Math.min(AudioConstants.SAMPLE_COUNT, bytesRead / AudioConstants.BYTES_PER_SAMPLE);
                    for (int i = 0, s = 0; s < samplesRead; i += 2, s++) {
                        int low = buffer[i] & 0xFF;
                        int high = buffer[i + 1];
                        int value = (high << 8) | low;
                        samples[s] = value / 32768.0;
                    }
                    if (samplesRead < AudioConstants.SAMPLE_COUNT) {
                        Arrays.fill(samples, samplesRead, AudioConstants.SAMPLE_COUNT, 0.0);
                    }

                    double[] newMagnitudes = computeFFT(samples);

                    // Lissage exponentiel
                    for (int i = 0; i < newMagnitudes.length; i++) {
                        smoothedMagnitudes[i] =
                                0.8 * smoothedMagnitudes[i] + 0.2 * newMagnitudes[i];
                    }

                    magnitudes = Arrays.copyOf(smoothedMagnitudes, smoothedMagnitudes.length);
                    Thread.sleep(10);
                }

                line.stop();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        captureThread.start();

        // Redessin régulier à 30 FPS
        Timer timer = new Timer(33, e -> repaint());
        timer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> running.set(false)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (magnitudes == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        int len = magnitudes.length;
        double max = Arrays.stream(magnitudes).max().orElse(1);

        // Using a logarithmic scale for the x-axis
        double logBase = Math.log(len);

        for (int i = 1; i < len; i++) { // Start from 1 to avoid log(0)
            double norm = magnitudes[i] / max;
            int barHeight = (int) (norm * h);

            // Calculate x position on a logarithmic scale
            double log_i = Math.log(i);
            int x = (int) (w * log_i / logBase);

            // Couleur dynamique (vert → jaune → rouge)
            float hue = (float) (0.33 - norm * 0.33); // 0.33=vert, 0.0=rouge
            g2.setColor(Color.getHSBColor(hue, 1.0f, 1.0f));

            g2.drawLine(x, h, x, h - barHeight);
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Spectre Audio (Log) — " + (int) (AudioConstants.SAMPLE_RATE / 2) + " Hz", 10, 20);
    }

    // FFT identique à avant
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
                double tmpI = imag[i];
                imag[i] = imag[j];
                imag[j] = tmpI;
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

                    double rEven = real[even];
                    double iEven = imag[even];
                    double rOdd = real[odd];
                    double iOdd = imag[odd];

                    double tR = rOdd * wR - iOdd * wI;
                    double tI = rOdd * wI + iOdd * wR;

                    real[even] = rEven + tR;
                    imag[even] = iEven + tI;
                    real[odd] = rEven - tR;
                    imag[odd] = iEven - tI;

                    double newWR = wR * wStepR - wI * wStepI;
                    double newWI = wR * wStepI + wI * wStepR;
                    wR = newWR;
                    wI = newWI;
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
