package fr.eletutour.sound.generation.guitar;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class GuitarPanel extends JPanel {

    private static final int NUM_STRINGS = 6;
    private final long[] pluckTimes = new long[NUM_STRINGS];

    public GuitarPanel() {
        setBackground(new Color(30, 30, 30));
        setPreferredSize(new Dimension(800, 400));

        Timer timer = new Timer(16, e -> repaint()); // ~60 FPS
        timer.start();
    }

    public void pluckString(int stringIndex) {
        if (stringIndex >= 0 && stringIndex < NUM_STRINGS) {
            pluckTimes[stringIndex] = System.currentTimeMillis();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

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
