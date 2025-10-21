package fr.eletutour.sound.generation.guitar;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class GuitarPanel extends JPanel {

    private static final int NUM_STRINGS = 6;
    private final long[] pluckTimes = new long[NUM_STRINGS];
    private int capoFret = 0; // Added capoFret field

    public GuitarPanel() {
        setBackground(new Color(30, 30, 30));
        setPreferredSize(new Dimension(1200, 400));

        Timer timer = new Timer(16, e -> repaint()); // ~60 FPS
        timer.start();
    }

    public void pluckString(int stringIndex) {
        if (stringIndex >= 0 && stringIndex < NUM_STRINGS) {
            pluckTimes[stringIndex] = System.currentTimeMillis();
        }
    }

    // Added setCapoFret method
    public void setCapoFret(int capoFret) {
        this.capoFret = capoFret;
        repaint(); // Repaint to show the capo
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // Draw frets
        g2d.setColor(new Color(100, 100, 100)); // Fret color
        int fretStart = 50; // Same as string start
        int fretEnd = panelWidth - 50; // Same as string end
        int numFrets = 12; // Number of frets to draw
        int fretSpacing = (fretEnd - fretStart) / (numFrets + 1); // Approximate spacing

        for (int i = 0; i <= numFrets; i++) {
            int x = fretStart + i * fretSpacing;
            g2d.setStroke(new BasicStroke(i == 0 ? 5 : 2)); // Thicker for the nut (fret 0)
            g2d.drawLine(x, panelHeight / (NUM_STRINGS + 1), x, panelHeight - panelHeight / (NUM_STRINGS + 1));
        }

        // Draw capo if active
        if (capoFret > 0 && capoFret <= numFrets) {
            g2d.setColor(new Color(50, 83, 200, 150)); // Capo color (semi-transparent red)
            int capoX = fretStart + capoFret * fretSpacing - (fretSpacing / 2); // Position capo in the middle of the fret
            g2d.fillRect(capoX, panelHeight / (NUM_STRINGS + 1) - 5, fretSpacing, panelHeight - panelHeight / (NUM_STRINGS + 1) + 10);
        }


        for (int i = 0; i < NUM_STRINGS; i++) {
            long timeSincePluck = System.currentTimeMillis() - pluckTimes[i];
            double currentAmplitude = 0.0;
            if (timeSincePluck < 2000) { // Vibrate for 2 seconds
                currentAmplitude = Math.exp(-timeSincePluck / 300.0); // Exponential decay
            }

            int y = panelHeight / (NUM_STRINGS + 1) * (i + 1);
            int maxAmplitude = 20 + i * 2; // Thicker strings vibrate more

            g2d.setColor(new Color(200, 200, 200));

            if (currentAmplitude > 0.01) {
                drawVibratingString(g2d, y, panelWidth, maxAmplitude * currentAmplitude, i);
            } else {
                g2d.setStroke(new BasicStroke(2 + i)); // Thicker stroke for lower strings
                g2d.drawLine(50, y, panelWidth - 50, y);
            }
        }
    }

    private void drawVibratingString(Graphics2D g2d, int y, int width, double amplitude, int stringIndex) {
        g2d.setStroke(new BasicStroke(2 + stringIndex));
        Path2D.Double path = new Path2D.Double();
        path.moveTo(50, y);

        int segments = 100;
        for (int i = 0; i <= segments; i++) {
            double px = (double) i / segments;
            double waveX = 50 + px * (width - 100);
            // A simple sine wave for vibration, with more complexity for lower strings
            double waveY = y + amplitude * Math.sin(px * Math.PI) * Math.sin(px * (10 + stringIndex * 2));
            path.lineTo(waveX, waveY);
        }
        g2d.draw(path);
    }
}
