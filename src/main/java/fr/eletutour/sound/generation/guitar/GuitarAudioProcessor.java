package fr.eletutour.sound.generation.guitar;

import fr.eletutour.sound.constant.AudioConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Map;

public class GuitarAudioProcessor implements Runnable {

    private final Map<Integer, GuitarString> activeStrings;
    private final VirtualGuitar virtualGuitar;

    public GuitarAudioProcessor(VirtualGuitar virtualGuitar, Map<Integer, GuitarString> activeStrings) {
        this.virtualGuitar = virtualGuitar;
        this.activeStrings = activeStrings;
    }

    @Override
    public void run() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(AudioConstants.SAMPLE_RATE, 16, 1, true, true))) {
            line.open();
            line.start();
            byte[] buffer = new byte[1024];

            while (true) {
                for (int i = 0; i < buffer.length / 2; i++) {
                    double mixedSample = 0;

                    for (Map.Entry<Integer, GuitarString> entry : activeStrings.entrySet()) {
                        GuitarString string = entry.getValue();
                        mixedSample += string.getNextSample();
                        if (!string.isActive()) {
                            activeStrings.remove(entry.getKey());
                        }
                    }

                    // Apply distortion
                    if (virtualGuitar.getDistortionLevel() > 0.0f) {
                        // Simple soft clipping using tanh
                        // The 'gain' factor amplifies the signal before tanh, increasing distortion
                        double gain = 1.0 + (virtualGuitar.getDistortionLevel() * 5.0); // Adjust gain for desired distortion intensity
                        mixedSample = Math.tanh(mixedSample * gain) / Math.tanh(gain);
                    }

                    mixedSample = Math.max(-1.0, Math.min(1.0, mixedSample * 0.5)); // Reduce volume to prevent clipping
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
}
