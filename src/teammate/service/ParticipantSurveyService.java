package teammate.service;

import teammate.model.Participant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class ParticipantSurveyService {

    private static final String[] ROLE_OPTIONS = {
            "Strategist", "Attacker", "Defender", "Supporter", "Coordinator"
    };

    private static final String[] GAME_OPTIONS = {
            "Valorant", "DOTA 2", "FIFA", "Basketball", "Badminton", "Chess", "CS:GP"
    };

    // 5 questions text
    private static final String[] QUESTIONS = {
            "Q1: I enjoy taking the lead and guiding others during group activities.",
            "Q2: I prefer analyzing situations and coming up with strategic solutions.",
            "Q3: I work well with others and enjoy collaborative teamwork.",
            "Q4: I am calm under pressure and can help maintain team morale.",
            "Q5: I like making quick decisions and adapting in dynamic situations."
    };

    /**
     * Ask user how many new members to add, run survey, and return list.
     * IDs/Names/Emails auto-generated based on existing count.
     */
    public List<Participant> collectNewParticipants(Scanner sc, int existingCount) {
        List<Participant> newParticipants = new ArrayList<>();

        System.out.print("\nHow many NEW participants do you want to add? ");
        String countStr = sc.nextLine().trim();
        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            System.out.println("Not a number. Skipping adding new participants.");
            return newParticipants;
        }

        for (int i = 0; i < count; i++) {
            int index = existingCount + i + 1; // e.g., after 100 → new = 101

            System.out.println("\n--- New Participant " + index + " Survey ---");

            // Auto-generate ID/Name/Email using same style
            String id = String.format("P%03d", index);
            String name = "Participant_" + index;
            String email = "user" + index + "@university.edu";

            // Choose PreferredGame
            String game = chooseFromOptions(sc, "Select Preferred Game:", GAME_OPTIONS);

            // SkillLevel 1–10
            int skillLevel = askIntInRange(sc,
                    "Enter Skill Level (1–10): ", 1, 10);

            // PreferredRole
            String role = chooseFromOptions(sc, "Select Preferred Role:", ROLE_OPTIONS);

            // Survey Q1–Q5 (1–5)
            int[] answers = new int[5];
            for (int q = 0; q < QUESTIONS.length; q++) {
                System.out.println(QUESTIONS[q]);
                answers[q] = askIntInRange(sc,
                        "Rate 1 (Strongly Disagree) to 5 (Strongly Agree): ", 1, 5);
            }

            // ---- NEW: use Q1..Q5 with calculatePersonality ----
            int q1 = answers[0];
            int q2 = answers[1];
            int q3 = answers[2];
            int q4 = answers[3];
            int q5 = answers[4];

            // total score exactly like spec: (Q1+..+Q5) * 4
            int totalScore = q1 + q2 + q3 + q4 + q5;     // range 5–25
            int personalityScore = totalScore * 4;       // range 20–100

            String personalityType = calculatePersonality(q1, q2, q3, q4, q5);
            // ---------------------------------------------------

            Participant p = new Participant(name, email, game, skillLevel, role);
            p.setPersonalityScore(personalityScore);
            p.setPersonalityType(personalityType);

            newParticipants.add(p);

            System.out.println("Created: " + id + " | " + name + " | " +
                    email + " | " + game + " | Skill " + skillLevel +
                    " | Role " + role + " | Type " + personalityType);
        }

        // Save new participants in a temporary CSV (not permanent main file)
        if (!newParticipants.isEmpty()) {
            saveTemporaryParticipants(newParticipants);
        }

        return newParticipants;
    }

    /**
     * Save temporary participants into:
     *   src/teammate/TempParticipants/new_participants_yyyyMMdd_HHmmss.csv
     * Format:
     *   ID,Name,Email,PreferredGame,SkillLevel,PreferredRole
     */
    private void saveTemporaryParticipants(List<Participant> participants) {
        try {
            String dirPath = "src/teammate/TempParticipants";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "new_participants_" + timeStamp + ".csv";
            File file = new File(dir, fileName);

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole");

                int index = 1;
                for (Participant p : participants) {
                    // Reconstruct ID/Name/Email based on index if needed
                    String id = String.format("P%03d", index);
                    String name = p.getName();
                    String email = p.getEmail();

                    pw.println(id + "," +
                            name + "," +
                            email + "," +
                            p.getPreferredGame() + "," +
                            p.getSkillLevel() + "," +
                            p.getRole());
                    index++;
                }
            }

            System.out.println("Temporary participants saved (for this run only).");

        } catch (IOException e) {
            System.out.println("Error saving temporary participants: " + e.getMessage());
        }
    }

    // -------- Helper methods --------

    private int askIntInRange(Scanner sc, String msg, int min, int max) {
        while (true) {
            System.out.print(msg);
            String input = sc.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val < min || val > max) {
                    System.out.println("Please enter a value between " + min + " and " + max + ".");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private String chooseFromOptions(Scanner sc, String title, String[] options) {
        while (true) {
            System.out.println(title);
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.print("Enter choice (1-" + options.length + "): ");
            String input = sc.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= options.length) {
                    return options[choice - 1];
                }
            } catch (NumberFormatException ignored) { }
            System.out.println("Invalid choice. Try again.");
        }
    }

    // ---- Personality classification helper ----
    public String calculatePersonality(int q1, int q2, int q3, int q4, int q5) {

        int leaderScore   = q1 + q5;   // leadership & quick decisions
        int thinkerScore  = q2 + q4;   // analysis & calm under pressure
        int balancedScore = q3 * 2;    // teamwork

        if (leaderScore > thinkerScore && leaderScore > balancedScore)
            return "Leader";

        if (thinkerScore > leaderScore && thinkerScore > balancedScore)
            return "Thinker";

        return "Balanced";
    }

}
