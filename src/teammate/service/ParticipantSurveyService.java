package teammate.service;

import teammate.model.Participant;

import java.util.*;
import teammate.service.LoggerService;


public class ParticipantSurveyService {

    public static final String[] ROLE_OPTIONS = {
            "Strategist", "Attacker", "Defender", "Supporter", "Coordinator"
    };

    public static final String[] GAME_OPTIONS = {
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

    // Survey for an EXISTING logged-in participant (p2, etc.)
    public void runSurveyForExistingParticipant(Scanner sc,
                                                Participant p,
                                                AuthService authService) {

        LoggerService logger = LoggerService.getInstance();

        if (p.getPersonalityType() != null &&
                !p.getPersonalityType().equalsIgnoreCase("Not selected")) {

            System.out.println("\nYou already completed the survey earlier.");
            System.out.println("Your saved profile will be used for team formation:");
            System.out.println("Name       : " + p.getName());
            System.out.println("Game       : " + p.getPreferredGame());
            System.out.println("Role       : " + p.getRole());
            System.out.println("Skill      : " + p.getSkillLevel());
            System.out.println("Personality: " + p.getPersonalityType());
            System.out.println("Score      : " + p.getPersonalityScore());

            logger.info("Survey skipped (already completed) for participant: " + p.getName());
            return;
        }

        // Preferred game
        String game = chooseFromOptions(sc, "Select Preferred Game:", GAME_OPTIONS);
        System.out.println();

        // Skill
        int skillLevel = askIntInRange(sc,
                "Enter Skill Level (1–10): ", 1, 10);
        System.out.println();
        // Role
        String role = chooseFromOptions(sc, "Select Preferred Role:", ROLE_OPTIONS);
        System.out.println();
        // Questions Q1–Q5
        int[] answers = new int[5];
        for (int q = 0; q < QUESTIONS.length; q++) {
            System.out.println(QUESTIONS[q]);
            answers[q] = askIntInRange(sc,
                    "Rate 1 (Strongly Disagree) to 5 (Strongly Agree): ", 1, 5);
            System.out.println();
        }

        int q1 = answers[0];
        int q2 = answers[1];
        int q3 = answers[2];
        int q4 = answers[3];
        int q5 = answers[4];

        int totalScore = q1 + q2 + q3 + q4 + q5;
        int personalityScore = totalScore * 4;
        String personalityType = calculatePersonality(q1, q2, q3, q4, q5);

        // update participant object in memory
        p.setPreferredGame(game);
        p.setSkillLevel(skillLevel);
        p.setRole(role);
        p.setPersonalityScore(personalityScore);
        p.setPersonalityType(personalityType);
        System.out.println("Updated profile: " + p.getName()
                + " | " + game + " | Skill " + skillLevel
                + " | Role " + role
                + " | Type " + personalityType);

        // NOW MAKE IT PERMANENT
        try {
            authService.saveAllAccountsToFile();
            logger.info("Survey completed and saved for participant: " + p.getName() +
                    " | Game=" + game +
                    " | Role=" + role +
                    " | Score=" + personalityScore +
                    " | Type=" + personalityType);
            System.out.println("Survey completed and saved for participant: " + p.getName());
        } catch (Exception e) {
            LoggerService.getInstance().error("Failed to save survey data for " + p.getName(), e);
            System.out.println("Survey failed to save for " + p.getName());
        }
    }
}
