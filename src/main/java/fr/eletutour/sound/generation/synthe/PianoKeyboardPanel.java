package fr.eletutour.sound.generation.synthe;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class PianoKeyboardPanel extends JPanel {

    private final Set<Character> pressedKeys;
    private final Map<Character, Rectangle> whiteKeyRects = new HashMap<>();
    private final Map<Character, Rectangle> blackKeyRects = new HashMap<>();

    private static final int WHITE_KEY_WIDTH = 60;
    private static final int WHITE_KEY_HEIGHT = 200;
    private static final int BLACK_KEY_WIDTH = 40;
    private static final int BLACK_KEY_HEIGHT = 120;

    public PianoKeyboardPanel(Set<Character> pressedKeys) {
        this.pressedKeys = pressedKeys;

        char[] whiteKeys = {'q', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm'};
        for (int i = 0; i < whiteKeys.length; i++) {
            whiteKeyRects.put(whiteKeys[i], new Rectangle(i * WHITE_KEY_WIDTH, 0, WHITE_KEY_WIDTH, WHITE_KEY_HEIGHT));
        }

        blackKeyRects.put('z', new Rectangle(WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('e', new Rectangle(2 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('t', new Rectangle(4 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('y', new Rectangle(5 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('u', new Rectangle(6 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('i', new Rectangle(8 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));
        blackKeyRects.put('o', new Rectangle(9 * WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2, 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT));

        int panelWidth = whiteKeys.length * WHITE_KEY_WIDTH;
        setPreferredSize(new Dimension(panelWidth, WHITE_KEY_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw white keys
        for (Map.Entry<Character, Rectangle> entry : whiteKeyRects.entrySet()) {
            if (pressedKeys.contains(entry.getKey())) {
                g2d.setColor(Color.LIGHT_GRAY);
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fill(entry.getValue());
            g2d.setColor(Color.BLACK);
            g2d.draw(entry.getValue());
        }

        // Draw black keys
        for (Map.Entry<Character, Rectangle> entry : blackKeyRects.entrySet()) {
            if (pressedKeys.contains(entry.getKey())) {
                g2d.setColor(Color.DARK_GRAY);
            } else {
                g2d.setColor(Color.BLACK);
            }
            g2d.fill(entry.getValue());
        }
    }
}
