package com.xtraea;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static com.xtraea.ObfuscationHandler.deobfuscate;
import static com.xtraea.ObfuscationHandler.obfuscate;

public class FileHandler {
    static class Config {
        private static final File CONFIG_FILE = new File(System.getProperty("user.home") + "/SNE.conf");
        private static String[] config = new String[1]; //increase as add more to

        public static void initialiseConfig() {
            try {
                if (CONFIG_FILE.exists()) return;
                //noinspection ResultOfMethodCallIgnored
                CONFIG_FILE.createNewFile();
                System.out.println("Config file did not exist; created at " + CONFIG_FILE.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("WARNING: could not create config file; this will cause errors later.");
                e.printStackTrace();
            }
        }
        public static void saveConfig() {
            saveConfigFromArray(config);
        }

        private static void saveConfigFromArray(String[] array) {
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                StringBuilder toWrite = new StringBuilder();
                for (String s : array) {
                    toWrite.append(s).append("\n");
                }
                writer.write(new String(toWrite));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public static abstract class ConfigIndexes {
            public static final int LASTFILE = 0;
        }
        private static boolean isCacheValid = false;
        public static void invalidateCache() {
            isCacheValid = false;
        }
        public static String getConfigValue(int index) {
            if (isCacheValid) return config[index];
            try (Scanner configFile = new Scanner(CONFIG_FILE)) {
                for (int i = 0; configFile.hasNextLine() && i < config.length; i++) {
                    config[i] = configFile.nextLine();
                }
                isCacheValid = true;
                return config[index];
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        public static void updateConfigValue(String value, int index) {
            config[index] = value;
            saveConfig();
            invalidateCache();
        }
    }

    public static File currentlyOpenFile = null;
    public static void saveFile(File file, String string) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(obfuscate(string));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String readFile(File file) {
        StringBuilder fileContents = new StringBuilder();
        try (Scanner reader = new Scanner(file).useDelimiter("(\\b|\\B)")) {
            while (reader.hasNext()) fileContents.append(reader.next());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContents.toString();
    }
    //checks if exists - creates if not; then saves
    //also checks if null
    public static void checkingSave(File file, String toSave) {
        if (file == null) return;
        if (file.exists()) {
            saveFile(file, toSave);
        } else {
            try {
                if (file.createNewFile()) {
                    saveFile(file, toSave);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    //returns 0 if no changes, 1 if changes, and 2 if no file active
    public static int checkForChanges(String current, File active) {
        if (active == null) return 2;
        if (deobfuscate(readFile(active)).equals(current)) return 0;
        return 1;
    }
    public static boolean checkBeforeCloseActive(JFrame root, JTextArea mainTextArea, JFileChooser fc) {
        if (checkForChanges(mainTextArea.getText(), currentlyOpenFile) == 0) return true;
        Object[] options = {"Save",
                "Don't Save",
                "Cancel"};
        int choice = JOptionPane.showOptionDialog(root,
                "Would you like to save changes?",
                "Secure Note Editor",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        if (choice == 2 || choice == -1) return false;
        if (choice != 1) switch (checkForChanges(mainTextArea.getText(), currentlyOpenFile)) {
            case 2 -> {
                fc.showOpenDialog(root);
                if (fc.getSelectedFile() != null) {
                    currentlyOpenFile = fc.getSelectedFile();
                } else break;
                saveFile(currentlyOpenFile, mainTextArea.getText());
            }
            case 1 -> {
                saveFile(currentlyOpenFile, mainTextArea.getText());
            }
        }
        return true;
    }
}
