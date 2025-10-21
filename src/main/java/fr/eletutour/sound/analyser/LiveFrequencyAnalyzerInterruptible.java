package fr.eletutour.sound.analyser;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveFrequencyAnalyzerInterruptible {


    static void main(String[] args) throws Exception {
        final int bufferSize = AudioConstants.SAMPLE_COUNT * AudioConstants.BYTES_PER_SAMPLE; // octets lus à chaque lecture

        AudioFormat format = new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, false); // little-endian
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format, bufferSize * 4);
        microphone.start();

        AtomicBoolean running = new AtomicBoolean(true);
        ByteArrayOutputStream recorded = new ByteArrayOutputStream();

        Thread captureThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            double[] samples = new double[AudioConstants.SAMPLE_COUNT];

            IO.println("🎤 Analyse en cours... Parle, siffle ou tape. Appuie sur Entrée pour arrêter.");

            while (running.get()) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) continue;

                // Sauvegarde brute si on veut exporter plus tard
                recorded.write(buffer, 0, bytesRead);

                // Conversion bytes -> échantillons normalisés (-1.0 .. 1.0)
                int samplesRead = Math.min(AudioConstants.SAMPLE_COUNT, bytesRead / AudioConstants.BYTES_PER_SAMPLE);
                for (int i = 0, s = 0; s < samplesRead; i += 2, s++) {
                    // little-endian -> low byte first
                    int low = buffer[i] & 0xFF;
                    int high = buffer[i + 1]; // sign will be preserved
                    int value = (high << 8) | low;
                    samples[s] = value / 32768.0; // normalisation
                }
                // Si on a moins d'échantillons que sampleCount, zero-pad
                if (samplesRead < AudioConstants.SAMPLE_COUNT) {
                    Arrays.fill(samples, samplesRead, AudioConstants.SAMPLE_COUNT, 0.0);
                }

                // Calcul FFT
                double[] magnitudes = computeFFT(samples);

                // Trouver pic (éviter index 0 = DC)
                int maxIndex = 1;
                for (int i = 2; i < magnitudes.length; i++) {
                    if (magnitudes[i] > magnitudes[maxIndex]) maxIndex = i;
                }

                double frequency = maxIndex * AudioConstants.SAMPLE_RATE / AudioConstants.SAMPLE_COUNT;
                double magnitude = magnitudes[maxIndex];

                // Affichage console : fréquence + barre d'intensité
                System.out.printf("Fréquence dominante : %7.1f Hz %s%n",
                        frequency,
                        createBarGraph(magnitude));
            }
        });

        captureThread.start();

        // Attente d'Entrée pour arrêter proprement
        new Scanner(System.in).nextLine();
        running.set(false);

        captureThread.join();
        microphone.stop();
        microphone.close();

        IO.println("✅ Capture arrêtée. Octets enregistrés: " + recorded.size());

        // Optionnel : sauvegarder en WAV (décommenter si souhaité)

        byte[] audioData = recorded.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
        File outFile = new File("enregistrement.wav");
        AudioSystem.write(ais, Type.WAVE, outFile);
        IO.println("💾 Fichier enregistré : " + outFile.getAbsolutePath());

    }

    // FFT (Cooley–Tukey) — prend des échantillons réels, renvoie magnitudes[0..n/2-1]
    private static double[] computeFFT(double[] input) {
        int n = input.length;
        double[] real = Arrays.copyOf(input, n);
        double[] imag = new double[n];

        int levels = 31 - Integer.numberOfLeadingZeros(n);
        // bit-reversal permutation
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

    private static String createBarGraph(double magnitude) {
        // échelle simple, ajustable si nécessaire
        int len = (int) Math.min(60, magnitude * 50);
        if (len <= 0) return "";
        return " " + "█".repeat(len);
    }
}
