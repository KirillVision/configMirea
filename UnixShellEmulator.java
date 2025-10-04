package KU_1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

public class UnixShellEmulator {
    private JFrame frame;
    private JTextArea outputArea;
    private JTextField inputField;
    private Config config;
    private VirtualFileSystem vfs;

    public static void main(String[] args) {
        UnixShellEmulator emulator = new UnixShellEmulator();
        emulator.config = new Config();
        emulator.config.parseArgs(args);
        emulator.start();
    }

    public void start() {
        vfs = new VirtualFileSystem(config.vfsPath);
        createGUI();

        if (config.startupScript != null) {
            runStartupScript(config.startupScript);
        }
    }

    private void createGUI() {
        String title = "VFS Emulator";
        if (config.vfsPath != null) {
            title += " - " + config.vfsPath;
        }
        frame = new JFrame(title);
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
            return ls(args);
        } else if ("cd".equals(command)) {
            return cd(args);
        } else if ("pwd".equals(command)) {
            return pwd(args);
        } else if ("echo".equals(command)) {
            return echo(args);
        } else if ("cal".equals(command)) {
            return cal(args);
        } else if ("rmdir".equals(command)) {
            return rmdir(args);
        } else if ("chown".equals(command)) {
            return chown(args);
        } else if ("help".equals(command)) {
            return help(args);
        } else if ("conf-dump".equals(command)) {
            return confDump(args);
        } else if ("exit".equals(command)) {
            return "Goodbye!";
        } else {
            return "Error: Unknown command '" + command + "'";
        }
    }

    private String ls(String[] args) {
        String path = args.length > 0 ? args[0] : null;
        ArrayList<String> files = vfs.list(path);
        if (files == null) return "Error: Directory not found";

        String result = "";
        for (String file : files) {
            result += file + "  ";
        }
        return result.trim();
    }

    private String cd(String[] args) {
        if (args.length > 1) return "Error: cd takes 0 or 1 arguments";
        String path = args.length == 0 ? "/" : args[0];
        if (vfs.cd(path)) return "";
        return "Error: Directory not found";
    }

    private String pwd(String[] args) {
        if (args.length > 0) return "Error: pwd takes no arguments";
        return vfs.pwd();
    }

    private String echo(String[] args) {
        String result = "";
        for (String arg : args) {
            result += arg + " ";
        }
        return result.trim();
    }

    private String cal(String[] args) {
        if (args.length > 0) {
            return "Error: cal takes no arguments";
        }

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        String[] monthNames = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        StringBuilder sb = new StringBuilder();
        sb.append("    ").append(monthNames[month - 1]).append(" ").append(year).append("\n");
        sb.append("Su Mo Tu We Th Fr Sa\n");

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i < firstDayOfWeek; i++) {
            sb.append("   ");
        }

        for (int day = 1; day <= daysInMonth; day++) {
            sb.append(String.format("%2d ", day));
            if ((day + firstDayOfWeek - 1) % 7 == 0) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String rmdir(String[] args) {
        if (args.length != 1) return "Error: rmdir takes 1 argument";
        if (vfs.rmdir(args[0])) return "";
        return "Error: Cannot remove directory";
    }

    private String chown(String[] args) {
        if (args.length != 2) return "Error: chown takes 2 arguments";
        if (vfs.chown(args[1], args[0])) return "";
        return "Error: Cannot change owner";
    }

    private String help(String[] args) {
        return "Available commands:\n" +
                "ls      - List directory contents\n" +
                "cd      - Change directory\n" +
                "pwd     - Print working directory\n" +
                "echo    - Display message\n" +
                "cal     - Display calendar\n" +
                "rmdir   - Remove directory\n" +
                "chown   - Change file owner\n" +
                "help    - Show this help\n" +
                "conf-dump - Show configuration\n" +
                "exit    - Exit emulator";
    }

    private String confDump(String[] args) {
        String result = "Configuration:\n";
        result += "  VFS Path: " + config.vfsPath + "\n";
        result += "  Startup Script: " + config.startupScript;
        return result;
    }

    private void runStartupScript(String scriptPath) {
        print("=== Running startup script: " + scriptPath + " ===");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(scriptPath));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                print("$ " + line);

                String[] parts = parseCommand(line);
                if (parts.length == 0) continue;

                String command = parts[0];
                String[] args = new String[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    args[i - 1] = parts[i];
                }

                String result = processCommand(command, args);
                if (result != null && !result.isEmpty()) {
                    print(result);
                }

                if (result != null && result.startsWith("Error:")) {
                    print("Startup script stopped due to error");
                    break;
                }

                if ("exit".equals(command)) {
                    break;
                }
            }
            reader.close();
            print("=== Startup script execution completed ===");
        } catch (Exception e) {
            print("Error reading startup script: " + e.getMessage());
        }
    }

    private void print(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}

class Config {
    String vfsPath;
    String startupScript;

    void parseArgs(String[] args) {
        System.out.println("Configuration:");
        for (int i = 0; i < args.length; i++) {
            if ("--vfs-path".equals(args[i]) && i + 1 < args.length) {
                vfsPath = args[++i];
                System.out.println("  VFS Path: " + vfsPath);
            } else if ("--startup-script".equals(args[i]) && i + 1 < args.length) {
                startupScript = args[++i];
                System.out.println("  Startup Script: " + startupScript);
            }
        }

        if (vfsPath == null) {
            System.out.println("  VFS Path: (not specified)");
        }
        if (startupScript == null) {
            System.out.println("  Startup Script: (not specified)");
        }
    }
}

class VirtualFileSystem {
    private Node root;
    private Node current;
    private String physicalPath;

    class Node {
        String name;
        boolean isDir;
        HashMap<String, Node> children;
        Node parent;
        String owner;

        Node(String name, boolean isDir) {
            this.name = name;
            this.isDir = isDir;
            this.owner = "user";
            if (isDir) children = new HashMap<>();
        }
    }

    public VirtualFileSystem(String physicalPath) {
        this.physicalPath = physicalPath;
        root = new Node("/", true);
        current = root;
        setupBasicStructure();

        if (physicalPath != null) {
            System.out.println("Loading VFS from: " + physicalPath);
        }
    }

    private void setupBasicStructure() {
        // Создаем базовую структуру папок (3 уровня вложенности)
        Node home = addNode(root, "home", true);
        Node user = addNode(home, "user", true);
        Node documents = addNode(user, "documents", true);
        Node downloads = addNode(user, "downloads", true);
        Node temp = addNode(user, "temp", true);

        // Файлы
        addNode(user, "readme.txt", false);
        addNode(documents, "file1.doc", false);
        addNode(documents, "file2.doc", false);
        addNode(downloads, "archive.zip", false);
        addNode(temp, "to_delete", true);

        // Еще один уровень
        Node projects = addNode(user, "projects", true);
        addNode(projects, "project1", true);
        addNode(projects, "project2", true);
    }

    private Node addNode(Node parent, String name, boolean isDir) {
        Node node = new Node(name, isDir);
        node.parent = parent;
        if (parent.isDir) {
            parent.children.put(name, node);
        }
        return node;
    }

    public ArrayList<String> list(String path) {
        Node target = path == null ? current : resolvePath(path);
        if (target == null || !target.isDir) return null;

        ArrayList<String> result = new ArrayList<>();
        for (Node child : target.children.values()) {
            result.add(child.name + (child.isDir ? "/" : ""));
        }
        return result;
    }

    public boolean cd(String path) {
        Node target = resolvePath(path);
        if (target != null && target.isDir) {
            current = target;
            return true;
        }
        return false;
    }

    public String pwd() {
        ArrayList<String> path = new ArrayList<>();
        Node node = current;

        while (node != root) {
            path.add(0, node.name);
            node = node.parent;
        }

        if (path.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", path);
    }

    public boolean rmdir(String path) {
        Node target = resolvePath(path);
        if (target == null || !target.isDir || target == root) {
            return false;
        }

        // Проверяем что директория пуста
        if (target.children != null && !target.children.isEmpty()) {
            return false;
        }

        return target.parent.children.remove(target.name) != null;
    }

    public boolean chown(String path, String owner) {
        Node target = resolvePath(path);
        if (target == null) {
            return false;
        }
        target.owner = owner;
        return true;
    }

    private Node resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return current;
        }

        if (path.startsWith("/")) {
            return resolveAbsolute(path);
        } else {
            return resolveRelative(path);
        }
    }

    private Node resolveAbsolute(String path) {
        String[] parts = path.split("/");
        Node current = root;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if ("..".equals(part)) {
                if (current.parent != null) current = current.parent;
            } else if (!".".equals(part)) {
                if (current.children == null) return null;
                current = current.children.get(part);
                if (current == null) return null;
            }
        }
        return current;
    }

    private Node resolveRelative(String path) {
        String[] parts = path.split("/");
        Node current = this.current;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if ("..".equals(part)) {
                if (current.parent != null) current = current.parent;
            } else if (!".".equals(part)) {
                if (current.children == null) return null;
                current = current.children.get(part);
                if (current == null) return null;
            }
        }
        return current;
    }
}