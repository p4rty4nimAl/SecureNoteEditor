package com.xtraea;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import static com.xtraea.FileHandler.*;
import static com.xtraea.FileHandler.Config.*;
import static com.xtraea.FileHandler.Config.ConfigIndexes.*;
import static com.xtraea.ObfuscationHandler.deobfuscate;

public class Main {
    public static JFileChooser fc = new JFileChooser("/");
    public static void main(String[] args) {
        initialiseConfig();
        updateConfigValue(getConfigValue(LASTFILE), LASTFILE);
        JFrame root = createRoot(new JComponent[0]);

        fc.setFileFilter(new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) return true;
                return file.getName().toLowerCase().endsWith(".snt");
            }
            public String getDescription() {
                return "Secure Note Text (*.snt)";
            }
        });

        JTextArea mainTextArea = new JTextArea();
        mainTextArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        mainTextArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(mainTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBounds(2 + 2 * 18, 1, root.getWidth() - (2 + 2 * (18 + 8)), root.getContentPane().getHeight());
        root.add(scrollPane);

        if (args.length > 0) {
            File argsFile = new File(System.getProperty("user.dir") + "/" + args[0]);
            if (argsFile.exists()) {
                mainTextArea.selectAll();
                mainTextArea.replaceSelection(deobfuscate(readFile(argsFile)));
                currentlyOpenFile = argsFile;
            }
        }

        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                scrollPane.setBounds(2 + 2 * 18, 1, root.getWidth() - (2 + 2 * (18 + 7)), root.getContentPane().getHeight());
            }
        });

        JMenuBar rootMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");

        // populate fileMenu
        fileMenu.add(createMenuItem("New", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (!checkBeforeCloseActive(root, mainTextArea, fc)) return;
                mainTextArea.selectAll();
                mainTextArea.replaceSelection("");
                FileHandler.currentlyOpenFile = null;
            }
        }, KeyEvent.VK_N));

        fileMenu.add(createMenuItem("Open", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (getConfigValue(LASTFILE) != null) {
                    fc.setCurrentDirectory(new File(getConfigValue(LASTFILE)));
                }
                fc.showOpenDialog(root);
                if (fc.getSelectedFile() != null) {
                    updateConfigValue(String.valueOf(fc.getSelectedFile()), LASTFILE);
                } else return;
                if (!checkBeforeCloseActive(root, mainTextArea, fc)) return;
                String deobfuscatedText = deobfuscate(readFile(fc.getSelectedFile()));
                mainTextArea.selectAll();
                mainTextArea.replaceSelection(deobfuscatedText);
                FileHandler.currentlyOpenFile = fc.getSelectedFile();
            }
        }, KeyEvent.VK_O));

        fileMenu.add(createMenuItem("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (FileHandler.currentlyOpenFile != null) {
                    checkingSave(currentlyOpenFile, mainTextArea.getText());
                } else {
                    fc.showOpenDialog(root);
                    checkingSave(fc.getSelectedFile(), mainTextArea.getText());
                }
            }
        }, KeyEvent.VK_S));

        fileMenu.add(createMenuItem("Save As", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                fc.showOpenDialog(root);
                checkingSave(fc.getSelectedFile(), mainTextArea.getText());
                currentlyOpenFile = fc.getSelectedFile();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK)));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (!checkBeforeCloseActive(root, mainTextArea, fc)) return;
                saveConfig();

                root.setVisible(false);
                root.dispose();
            }
        }, KeyEvent.VK_W));

        // populate editMenu
        editMenu.add(createMenuItem("Cut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainTextArea.cut();
            }
        }, KeyEvent.VK_X));

        editMenu.add(createMenuItem("Copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainTextArea.copy();
            }
        }, KeyEvent.VK_C));

        editMenu.add(createMenuItem("Paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mainTextArea.paste();
            }
        }, KeyEvent.VK_V));

        rootMenuBar.add(fileMenu);
        rootMenuBar.add(editMenu);
        rootMenuBar.setVisible(true);
        root.setJMenuBar(rootMenuBar);

        root.setVisible(true);
    }
    private static JMenuItem createMenuItem(String label, AbstractAction runOnClick, int key) {
        JMenuItem tMI = new JMenuItem(label);
        tMI.addActionListener(runOnClick);
        tMI.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        tMI.setVisible(true);
        return tMI;
    }
    private static JMenuItem createMenuItem(String label, AbstractAction runOnClick, KeyStroke key) {
        JMenuItem tMI = new JMenuItem(label);
        tMI.addActionListener(runOnClick);
        tMI.setAccelerator(key);
        tMI.setVisible(true);
        return tMI;
    }
    public static JFrame createRoot(JComponent[] componentsToAdd) {
        JFrame root = new JFrame("Secure Note Editor");
        for (JComponent component : componentsToAdd) {
            component.setVisible(true);
            root.add(component);
        }
        root.setLayout(null);
        root.setSize(1280, 720);
        root.setLocationRelativeTo(null);
        root.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        root.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                saveConfig();

                root.setVisible(false);
                root.dispose();
            }

        });
        return root;
    }
}