package fr.eletutour.sound;

import fr.eletutour.sound.generation.SoundGenerator;
import fr.eletutour.sound.generation.drum.VirtualDrumkit;
import fr.eletutour.sound.generation.guitar.VirtualGuitar;
import fr.eletutour.sound.generation.synthe.Synthesiser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Main {

    private static final Color BG_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_COLOR = new Color(187, 187, 187);
    private static final Color BUTTON_BG_COLOR = new Color(75, 110, 175);
    private static final Color BUTTON_FG_COLOR = Color.WHITE;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use a modern look and feel
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                // Fallback to system L&F
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            JFrame frame = new JFrame("Sound Generation - Main Menu");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(BG_COLOR);
            mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel titleLabel = new JLabel("Sound Generation Tools", SwingConstants.CENTER);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleLabel.setForeground(TEXT_COLOR);
            titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 15, 15));
            buttonPanel.setBackground(BG_COLOR);
            buttonPanel.setBorder(new EmptyBorder(10, 40, 10, 40));

            addButton(buttonPanel, "Sound Generator", SoundGenerator::main);
            addButton(buttonPanel, "Virtual Drumkit", VirtualDrumkit::main);
            addButton(buttonPanel, "Virtual Guitar", VirtualGuitar::main);
            addButton(buttonPanel, "Synthesiser", Synthesiser::main);

            mainPanel.add(buttonPanel, BorderLayout.CENTER);

            frame.setContentPane(mainPanel);
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void addButton(JPanel panel, String text, RunnableWithStringArray action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setBackground(BUTTON_BG_COLOR);
        button.setForeground(BUTTON_FG_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 45, 45)),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));
        button.addActionListener(e -> action.run(new String[0]));
        panel.add(button);
    }

    @FunctionalInterface
    interface RunnableWithStringArray {
        void run(String[] args);
    }
}
