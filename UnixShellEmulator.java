package KU_1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class UnixShellEmulator {
    private JFrame frame;
    private JTextArea outputArea;
    private JTextField inputField;

    public static void main(String[] args) {
        UnixShellEmulator emulator = new UnixShellEmulator();
        emulator.start();
    }

    public void start() {
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("VFS Emulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.WHITE);
        outputArea.setFont(new Font("Courier", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setBackground(Color.DARK_GRAY);
        inputField.setForeground(Color.WHITE);
        inputField.setFont(new Font("Courier", Font.PLAIN, 14));

        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executeCommand();
            }
        });

        JLabel promptLabel = new JLabel("$ ");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Courier", Font.BOLD, 14));

        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.setBackground(Color.DARK_GRAY);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
        inputField.requestFocus();
    }

    private void executeCommand() {
        String commandLine = inputField.getText().trim();
        inputField.setText("");

        if (commandLine.isEmpty()) return;

        print("$ " + commandLine);

        String[] parts = parseCommand(commandLine);
        if (parts.length == 0) return;

        String command = parts[0];
        String[] args = new String[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i];
        }

        String result = processCommand(command, args);

        if (result != null && !result.isEmpty()) {
            print(result);
        }

        if ("exit".equals(command)) {
            System.exit(0);
        }
    }

    private String[] parseCommand(String line) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                    if (current.length() > 0) {
                        parts.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = true;
                    quoteChar = c;
                    if (current.length() > 0) {
                        parts.add(current.toString());
                        current = new StringBuilder();
                    }
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        parts.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        String[] result = new String[parts.size()];
        return parts.toArray(result);
    }

    private String processCommand(String command, String[] args) {
        if ("ls".equals(command)) {
            String argsStr = "";
            for (String arg : args) {
                argsStr += arg + " ";
            }
            return "ls called with args: " + argsStr.trim();
        } else if ("cd".equals(command)) {
            if (args.length > 1) {
                return "Error: cd takes 0 or 1 arguments";
            }
            String argsStr = "";
            for (String arg : args) {
                argsStr += arg + " ";
            }
            return "cd called with args: " + argsStr.trim();
        } else if ("exit".equals(command)) {
            return "Goodbye!";
        } else {
            return "Error: Unknown command '" + command + "'";
        }
    }

    private void print(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}