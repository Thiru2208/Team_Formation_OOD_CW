package teammate.service;

import teammate.model.Participant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthService {

    // You can keep this path or change to "data/participant_accounts.csv"
    private static final String ACCOUNTS_FILE =
            "src/teammate/auth/participant_accounts.csv";

    // username -> plain password (in memory)
    private final Map<String, String> participantCredentials = new HashMap<>();
    // username -> Participant profile
    private final Map<String, Participant> participantProfiles = new HashMap<>();

    public AuthService() {
        loadParticipantAccounts();
    }

    // ================= ORGANIZER LOGIN =================
    public boolean organizerLogin(Scanner sc) {
        System.out.println("\n--- Organizer Login ---");
        System.out.print("Username: ");
        String user = sc.nextLine().trim();
        System.out.print("Password: ");
        String pass = sc.nextLine().trim();

        // hard-coded organizer account
        if (user.equals("organizer") && pass.equals("org123")) {
            System.out.println("✅ Organizer login successful.\n");
            return true;
        } else {
            System.out.println("❌ Invalid organizer credentials.\n");
            return false;
        }
    }

    // ================= PARTICIPANT SIGNUP =================
    public Participant participantSignup(Scanner sc) {
        System.out.println("\n--- Participant Sign Up ---");

        // 1) Username (unique + not empty)
        String username;
        while (true) {
            System.out.print("Choose a username: ");
            username = sc.nextLine().trim();

            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                continue;
            }
            String key = username.toLowerCase();
            if (participantCredentials.containsKey(key)) {
                System.out.println("This username is already taken. Try another one.");
                continue;
            }
            break;
        }

        // 2) Password + confirm (4 chars, letters + digits only)
        String password;
        while (true) {
            System.out.print("Choose a 4-character password (letters & digits only): ");
            password = sc.nextLine().trim();

            if (!isValidPassword(password)) {
                System.out.println("❌ Invalid password. It must be EXACTLY 4 characters, only letters (A–Z, a–z) and digits (0–9).");
                continue;
            }

            System.out.print("Confirm password: ");
            String confirm = sc.nextLine().trim();

            if (!password.equals(confirm)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            break;
        }

        // 3) Full name
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            name = username; // fallback
        }

        String key = username.toLowerCase();

        // save in memory (plain in RAM, encrypted only in file)
        participantCredentials.put(key, password);
        Participant p = new Participant(name, "Not selected",
                "Not selected", 0, "Not selected");
        participantProfiles.put(key, p);

        // append to file with ENCRYPTED password
        appendAccountToFile(username, password, name);

        System.out.println("✅ Signup successful. You can now log in.\n");
        return p;
    }

    // ================= PARTICIPANT LOGIN =================
    public Participant participantLogin(Scanner sc) {
        System.out.println("\n--- Participant Login ---");
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        String key = username.toLowerCase();
        String stored = participantCredentials.get(key); // loaded from file (decrypted)

        if (stored == null || !stored.equals(password)) {
            System.out.println("❌ Invalid username or password.\n");
            return null;
        }

        System.out.println("✅ Login success. Welcome " + username + "!\n");

        Participant profile = participantProfiles.get(key);
        if (profile == null) {
            // fallback: in case account existed in file but not in map (edge case)
            profile = new Participant(username, username + "@example.com",
                    "Not selected", 0, "Not selected");
            participantProfiles.put(key, profile);
        }
        return profile;
    }

    // ================= FILE LOAD / SAVE =================
    private void loadParticipantAccounts() {
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            System.out.println("No existing participant accounts file found.");
            return;
        }

        int loadedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // maybe header
            if (line != null) {
                // remove BOM if present
                line = line.replace("\uFEFF", "");
            }
            if (line != null && line.startsWith("username,")) {
                // header line, read next
                line = br.readLine();
            }

            while (line != null) {
                // IMPORTANT: keep empty last column
                String[] parts = line.split(",", -1);

                if (parts.length >= 4) {
                    String username          = parts[0].trim();
                    String encryptedPassword = parts[1].trim();
                    String fullName          = parts[2].trim();
                    String email             = parts[3].trim();

                    String key = username.toLowerCase();

                    String plainPassword = "";
                    if (!encryptedPassword.isEmpty()) {
                        plainPassword = decryptPassword(encryptedPassword);
                    }

                    participantCredentials.put(key, plainPassword);

                    Participant p = new Participant(
                            fullName,
                            email.isEmpty() ? (username + "@example.com") : email,
                            "Not selected",
                            0,
                            "Not selected"
                    );
                    participantProfiles.put(key, p);
                    loadedCount++;
                }

                line = br.readLine();
            }

            System.out.println("Loaded " + loadedCount + " participant accounts from file.");
        } catch (IOException e) {
            System.out.println("Error loading participant accounts: " + e.getMessage());
        }
    }

    private void appendAccountToFile(String username,
                                     String plainPassword,
                                     String fullName) {
        try {
            File file = new File(ACCOUNTS_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();   // create folder if missing
            }

            boolean newFile = !file.exists();

            try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
                if (newFile) {
                    pw.println("username,password,fullName,email");
                }

                // 🔐 encrypt before writing
                String encrypted = encryptPassword(plainPassword);

                // you can add email later; for now keep it empty
                pw.println(username + "," + encrypted + "," + fullName + ",");
            }
        } catch (IOException e) {
            System.out.println("Error saving account to file: " + e.getMessage());
        }
    }

    // ================= HELPERS =================

    // Password must be exactly 4 chars, letters & digits only
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        // regex: 4 chars, each [A-Za-z0-9]
        return password.matches("[A-Za-z0-9]{4}");
    }

    // Simple "encryption" using Base64 (for coursework only, not real security)
    private String encryptPassword(String raw) {
        return Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String decryptPassword(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // if corrupted file, avoid crash
            return "";
        }
    }
}
