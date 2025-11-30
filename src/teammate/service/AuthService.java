package teammate.service;

import teammate.model.Organizer;
import teammate.model.Participant;
import teammate.service.LoggerService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthService {

    private static final LoggerService logger = LoggerService.getInstance();
    private static Organizer organizerAccount;

    // username (lowercase) -> plain password
    private static final Map<String, String> participantCredentials = new HashMap<>();
    // username (lowercase) -> Participant profile
    private static final Map<String, Participant> participantProfiles = new HashMap<>();
    // username (lowercase) -> generated ID (P101, P102, ...)
    private static final Map<String, String> participantIds = new HashMap<>();

    // next numeric ID to generate (101 => P101)
    private static int nextGeneratedNumericId = 101;

    private static final String ACCOUNTS_FILE =
            "src/teammate/auth/participant_accounts.csv";
    private static final String ORGANIZER_FILE =
            "src/teammate/auth/organizer_account.csv";

    public AuthService() {
        loadOrganizerAccount(ORGANIZER_FILE);
        loadParticipantAccounts(ACCOUNTS_FILE);
    }

    // ================= ORGANIZER LOGIN =================
    public boolean organizerLogin(Scanner sc) {
        System.out.println("\n--- Organizer Login ---");
        System.out.print("Username: ");
        String user = sc.nextLine().trim();
        System.out.print("Password: ");
        String pass = sc.nextLine().trim();

        if (organizerAccount == null) {
            System.out.println("Organizer account not loaded!");
            return false;
        }

        if (user.equalsIgnoreCase(organizerAccount.getUsername())
                && pass.equals(organizerAccount.getPassword())) {
            System.out.println("Login success. Welcome " + organizerAccount.getName() + "!");
            logger.info("Organizer login success for username=" + organizerAccount.getUsername());
            return true;
        }
        logger.info("Organizer login FAILED for username=" + user);
        System.out.println("Invalid organizer credentials.");
        return false;
    }

    public void changeOrganizerPassword(Scanner sc, String ORGANIZER_FILE) {
        if (organizerAccount == null) {
            System.out.println("Organizer account not loaded – cannot change password.");
            return;
        }

        System.out.println("\n--- Change Organizer Password ---");

        System.out.print("Enter current password: ");
        String current = sc.nextLine().trim();

        if (!current.equals(organizerAccount.getPassword())) {
            System.out.println("Current password is incorrect.");
            return;
        }

        String newPass;
        while (true) {
            System.out.print("Enter new 4/6-character password (letters & digits only): ");
            newPass = sc.nextLine().trim();

            if (!isValidPassword(newPass)) {
                System.out.println("Invalid password. It must be EXACTLY 4/6 characters, only A–Z, a–z, 0–9.");
                continue;
            }

            System.out.print("Confirm new password: ");
            String confirm = sc.nextLine().trim();

            if (!newPass.equals(confirm)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            break;
        }

        organizerAccount.setPassword(newPass);
        saveOrganizerAccount(ORGANIZER_FILE);

        System.out.println("Organizer password updated successfully.");
    }

    public void changeParticipantPassword(Scanner sc, Participant participant, String ACCOUNTS_FILE) {

        if (participant == null) {
            System.out.println("No participant is logged in.");
            return;
        }

        System.out.println("\n--- Change Participant Password ---");

        System.out.print("Enter current password: ");
        String current = sc.nextLine().trim();

        // Stored password is inside participantCredentials map
        String key = participant.getUsername().toLowerCase();
        String realPassword = participantCredentials.get(key);

        if (!current.equals(realPassword)) {
            System.out.println("Incorrect current password.");
            return;
        }

        String newPass;
        while (true) {
            System.out.print("Enter new 4/6-character password (letters & digits only): ");
            newPass = sc.nextLine().trim();

            if (!isValidPassword(newPass)) {
                System.out.println("Invalid password. Use EXACTLY 4/6 characters A–Z a–z 0–9.");
                continue;
            }
            System.out.print("Confirm new password: ");
            String confirm = sc.nextLine().trim();

            if (!newPass.equals(confirm)) {
                System.out.println("Passwords do not match.");
                continue;
            }
            break;
        }

        // Update in memory
        participantCredentials.put(key, newPass);
        participant.setPassword(newPass);

        // Save permanently
        saveAllAccountsToFile(ACCOUNTS_FILE);

        System.out.println("Your password has been updated successfully!");
    }


    // ================= PARTICIPANT SIGNUP =================
    public Participant participantSignup(Scanner sc, String ACCOUNTS_FILE) {
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

        // 2) Password + confirm (4 chars, letters & digits only)
        String password;
        while (true) {
            System.out.print("Choose a 4/6-character password (letters & digits only): ");
            password = sc.nextLine().trim();

            if (!isValidPassword(password)) {
                System.out.println("Invalid password. It must be EXACTLY 4/6 characters, only letters (A–Z, a–z) and digits (0–9).");
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

        // 3) Auto-generate ID, Name, Email in signup order
        int currentIdNumber = nextGeneratedNumericId;       // e.g. 101 for first signup
        String id        = "P" + currentIdNumber;           // P101
        String fullName  = "Participant_" + currentIdNumber; // Participant_101
        String email     = "user" + currentIdNumber + "@university.edu"; // user101@...

        System.out.println("\nYour system details:");
        System.out.println("ID       : " + id);
        System.out.println("Name     : " + fullName);
        System.out.println("Email    : " + email);
        System.out.println();
        System.out.println("(These will be used for team formation.)");

        String key = username.toLowerCase();

        // save password in memory
        participantCredentials.put(key, password);

        // create participant profile with default survey data
        Participant p = new Participant(fullName, email,
                "Not selected", 0, "Not selected");
        p.setPersonalityType("Not selected");
        p.setPersonalityScore(0);
        // make sure profile knows its login credentials
        p.setUsername(username);
        p.setPassword(password);

        participantProfiles.put(key, p);
        participantIds.put(key, id);

        // append this account to file (with all columns)
        appendAccountToFile(username, password, id, fullName, email,
                p.getPreferredGame(), p.getSkillLevel(), p.getRole(),
                p.getPersonalityScore(), p.getPersonalityType(), ACCOUNTS_FILE);

        // increase ID counter for next signup
        nextGeneratedNumericId++;
        logger.info("Participant signup success: username=" + username +
                ", id=" + id + ", email=" + email);
        System.out.println("Signup successful. You can now log in. username=" + username +
                ", id=" + id + ", email=" + email);
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
            logger.info("Participant login FAILED for username=" + username);
            System.out.println("Invalid username or password.");
            return null;
        }

        logger.info("Participant login success: username=" + username);
        System.out.println("Login success. Welcome " + username + "!");

        Participant profile = participantProfiles.get(key);
        if (profile == null) {
            // fallback – should not normally happen if file is consistent
            profile = new Participant(username,
                    username + "@example.com",
                    "Not selected", 0, "Not selected");
            profile.setPersonalityType("Not selected");
            profile.setPersonalityScore(0);
            participantProfiles.put(key, profile);
        }
        profile.setUsername(username);
        profile.setPassword(stored);
        return profile;
    }

    public static void loadOrganizerAccount(String ORGANIZER_FILE) {
        File file = new File(ORGANIZER_FILE);
        if (!file.exists()) {
            System.out.println("Organizer account file missing: " + ORGANIZER_FILE);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // skip header line
            String line = br.readLine();   // we expect only one organizer row

            if (line == null || line.trim().isEmpty()) {
                System.out.println("Organizer file is empty.");
                return;
            }

            String[] parts = line.split(",", -1);
            // Format: username,password,name   (3 columns)
            if (parts.length < 3) {
                System.out.println("Organizer file has invalid format.");
                return;
            }

            String username      = parts[0].trim();
            String encryptedPass = parts[1].trim();
            String name          = parts[2].trim();

            String plainPass = decryptPassword(encryptedPass);

            organizerAccount = new Organizer(name, username, plainPass);
            System.out.println("Organizer account loaded.");
        } catch (IOException e) {
            System.out.println("Error loading organizer account: " + e.getMessage());
        }
    }

    // ================= FILE LOAD / SAVE =================
    public static void loadParticipantAccounts(String ACCOUNTS_FILE) {
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            logger.info("No existing participant accounts file found.");
            System.out.println("No existing participant accounts file found.");
            return;
        }

        int loadedCount = 0;
        int maxNumericIdFound = 100; // so first new becomes 101

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // maybe header
            if (line != null) {
                // remove BOM if present
                line = line.replace("\uFEFF", "");
            }
            if (line != null && line.toLowerCase().startsWith("username,")) {
                // header line, read next
                line = br.readLine();
            }

            while (line != null) {
                // keep empty last columns
                String[] parts = line.split(",", -1);

                // expecting:
                // 0=username,1=password,2=ID,3=fullName,4=email,
                // 5=preferredGame,6=skillLevel,7=role,8=personalityScore,9=personalityType
                if (parts.length >= 5) {
                    String username          = parts[0].trim();
                    String encryptedPassword = parts[1].trim();
                    String id                = parts[2].trim();
                    String fullName          = parts[3].trim();
                    String email             = parts[4].trim();

                    String preferredGame     = "Not selected";
                    int    skillLevel        = 0;
                    String role              = "Not selected";
                    int    personalityScore  = 0;
                    String personalityType   = "Not selected";

                    if (parts.length >= 10) {
                        if (!parts[5].trim().isEmpty())
                            preferredGame = parts[5].trim();

                        if (!parts[6].trim().isEmpty()) {
                            try { skillLevel = Integer.parseInt(parts[6].trim()); }
                            catch (NumberFormatException ignored) {}
                        }

                        if (!parts[7].trim().isEmpty())
                            role = parts[7].trim();

                        if (!parts[8].trim().isEmpty()) {
                            try { personalityScore = Integer.parseInt(parts[8].trim()); }
                            catch (NumberFormatException ignored) {}
                        }

                        if (!parts[9].trim().isEmpty())
                            personalityType = parts[9].trim();
                    }

                    String key = username.toLowerCase();

                    String plainPassword = "";
                    if (!encryptedPassword.isEmpty()) {
                        plainPassword = decryptPassword(encryptedPassword);
                    }

                    participantCredentials.put(key, plainPassword);

                    Participant p = new Participant(
                            fullName,
                            email.isEmpty() ? (username + "@example.com") : email,
                            preferredGame,
                            skillLevel,
                            role
                    );
                    p.setPersonalityScore(personalityScore);
                    p.setPersonalityType(personalityType);

                    // 🔹 link credentials into the profile
                    p.setUsername(username);
                    p.setPassword(plainPassword);

                    participantProfiles.put(key, p);
                    participantIds.put(key, id);

                    // track max ID number, if in format P###
                    if (id != null && id.startsWith("P")) {
                        try {
                            int numeric = Integer.parseInt(id.substring(1));
                            if (numeric > maxNumericIdFound) {
                                maxNumericIdFound = numeric;
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    loadedCount++;
                }

                line = br.readLine();
            }

            // next ID to assign
            nextGeneratedNumericId = maxNumericIdFound + 1;
            logger.info("Loaded " + loadedCount + " participant accounts from file.");
            System.out.println("Loaded " + loadedCount + " participant accounts from file.");
            System.out.println("Next generated ID will be: P" + nextGeneratedNumericId);

        } catch (IOException e) {
            logger.error("Error loading participant accounts from file: " + ACCOUNTS_FILE, e);
            System.out.println("Error loading participant accounts: " + e.getMessage());
        }
    }

    // used when signing up a new user (append one row)
    private void appendAccountToFile(String username,
                                     String plainPassword,
                                     String id,
                                     String fullName,
                                     String email,
                                     String preferredGame,
                                     int skillLevel,
                                     String role,
                                     int personalityScore,
                                     String personalityType, String ACCOUNTS_FILE) {
        try {
            File file = new File(ACCOUNTS_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();   // create folder if missing
            }

            boolean newFile = !file.exists();

            try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
                if (newFile) {
                    pw.println("username,password,ID,fullName,email,preferredGame,skillLevel,role,personalityScore,personalityType");
                }

                String encrypted = encryptPassword(plainPassword);

                pw.println(username + "," + encrypted + "," + id + "," +
                        fullName + "," + email + "," +
                        preferredGame + "," + skillLevel + "," +
                        role + "," + personalityScore + "," + personalityType);
            }
        } catch (IOException e) {
            logger.error("Error saving single account to file: " + ACCOUNTS_FILE, e);
            System.out.println("Error saving account to file: " + e.getMessage());
        }
    }

    // 🔹 called after survey updates so everything (including survey) is saved permanently
    public void saveAllAccountsToFile(String ACCOUNTS_FILE) {
        try {
            File file = new File(ACCOUNTS_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("username,password,ID,fullName,email,preferredGame,skillLevel,role,personalityScore,personalityType");

                for (Map.Entry<String, String> entry : participantCredentials.entrySet()) {
                    String keyUsernameLower = entry.getKey();      // stored in lower case
                    String plainPassword    = entry.getValue();
                    String encrypted        = encryptPassword(plainPassword);

                    Participant p = participantProfiles.get(keyUsernameLower);
                    String id = participantIds.getOrDefault(keyUsernameLower, "P000");

                    String fullName = (p != null && p.getName() != null && !p.getName().isEmpty())
                            ? p.getName() : keyUsernameLower;

                    String email = (p != null && p.getEmail() != null && !p.getEmail().isEmpty())
                            ? p.getEmail() : (keyUsernameLower + "@example.com");

                    String preferredGame = (p != null && p.getPreferredGame() != null)
                            ? p.getPreferredGame() : "Not selected";

                    int skillLevel = (p != null) ? p.getSkillLevel() : 0;

                    String role = (p != null && p.getRole() != null)
                            ? p.getRole() : "Not selected";

                    int personalityScore = (p != null) ? p.getPersonalityScore() : 0;

                    String personalityType = (p != null && p.getPersonalityType() != null)
                            ? p.getPersonalityType() : "Not selected";

                    pw.println(keyUsernameLower + "," + encrypted + "," + id + "," +
                            fullName + "," + email + "," +
                            preferredGame + "," + skillLevel + "," +
                            role + "," + personalityScore + "," + personalityType);
                }
            }

            logger.info("All participant accounts saved to file (with IDs and survey data).");
            System.out.println("All participant accounts saved to file.");

        } catch (IOException e) {
            logger.error("Error saving all accounts to file: " + ACCOUNTS_FILE, e);
            System.out.println("Error saving all accounts: " + e.getMessage());
        }
    }

    private void saveOrganizerAccount(String ORGANIZER_FILE) {
        if (organizerAccount == null) return;

        try {
            File file = new File(ORGANIZER_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                // header
                pw.println("username,password,name");

                String encrypted = encryptPassword(organizerAccount.getPassword());
                pw.println(
                        organizerAccount.getUsername() + "," +
                                encrypted + "," +
                                organizerAccount.getName()
                );
            }
            System.out.println("Organizer account saved.");
        } catch (IOException e) {
            System.out.println("Error saving organizer account: " + e.getMessage());
        }
    }

    // ================= HELPERS =================

    // Password must be exactly 4 chars, letters & digits only
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("[A-Za-z0-9]{4,6}");
    }

    // Simple Base64 "encryption" for coursework
    private String encryptPassword(String raw) {
        return Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decryptPassword(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}