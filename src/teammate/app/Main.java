package teammate.app;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.*;

import java.util.*;

public class Main {

    private static ArrayList<Participant> participants = new ArrayList<>();
    private static ArrayList<Team> teams = new ArrayList<>();

    // (Optional) you can reuse these later for nicer update menus
    private static final String[] ROLE_OPTIONS = {
            "Strategist", "Attacker", "Defender", "Supporter", "Coordinator"
    };

    private static final String[] GAME_OPTIONS = {
            "Valorant", "DOTA 2", "FIFA", "Basketball", "Badminton", "Chess", "CS:GP"
    };

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        TeamBuilder teamBuilder = new TeamBuilder();
        AuthService authService = new AuthService();
        ParticipantSurveyService surveyService = new ParticipantSurveyService();

        System.out.println("==== TeamMate: Intelligent Team Formation System ====\n");

        boolean running = true;
        while (running) {
            System.out.println("Main Menu");
            System.out.println("1. Organizer login");
            System.out.println("2. Participant sign up");
            System.out.println("3. Participant login");
            System.out.println("4. Exit");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    if (authService.organizerLogin(sc)) {
                        organizerMenu(sc, csvHandler, teamBuilder);
                    }
                    break;

                case "2": {
                    // ⚠️ CHANGE AuthService.participantSignup TO RETURN Participant
                    Participant newP = authService.participantSignup(sc);
                    if (newP != null) {
                        participants.add(newP);
                        System.out.println("✅ New participant added to system: " + newP.getName());
                    } else {
                        System.out.println("⚠ Signup failed.");
                    }
                    break;
                }

                case "3": {
                    Participant logged = authService.participantLogin(sc);
                    if (logged != null) {
                        participantMenu(sc, surveyService, logged);
                    }
                    break;
                }

                case "4":
                    running = false;
                    System.out.println("Exiting system...");
                    break;

                default:
                    System.out.println("Invalid choice.\n");
            }
        }
    }

    // ================= ORGANIZER MENU ===================
    private static void organizerMenu(Scanner sc,
                                      CSVHandler csvHandler,
                                      TeamBuilder teamBuilder) {

        boolean back = false;
        while (!back) {
            System.out.println("\n--- Organizer Menu ---");
            System.out.println("1. Upload member details (CSV)");
            System.out.println("2. Define team size and form teams");
            System.out.println("3. View formed teams");
            System.out.println("4. Export teams as CSV");
            System.out.println("5. Update participant data");
            System.out.println("6. Delete participant");
            System.out.println("7. Logout");
            System.out.print("Enter choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": {
                    System.out.print("Enter CSV path: ");
                    String path = sc.nextLine().trim();
                    ArrayList<Participant> loaded = csvHandler.loadParticipants(path);
                    if (!loaded.isEmpty()) {
                        // 👉 Merge with existing (keep sign-ups also)
                        participants.addAll(loaded);
                        System.out.println("✅ Loaded " + loaded.size()
                                + " participants. Total now: " + participants.size());
                    } else {
                        System.out.println("⚠ No valid participants loaded.");
                    }
                    break;
                }

                case "2":
                    if (participants.isEmpty()) {
                        System.out.println("⚠ Please upload/add participants first.");
                        break;
                    }
                    int teamSize = askTeamSize(sc, participants.size());
                    teams = teamBuilder.buildTeams(participants, teamSize);
                    System.out.println("✅ Teams formed: " + teams.size());
                    break;

                case "3":
                    showTeams();
                    break;

                case "4":
                    if (teams.isEmpty()) {
                        System.out.println("⚠ No teams to export.");
                        break;
                    }
                    String outPath = csvHandler.saveTeamsAuto(teams);
                    System.out.println("📁 Exported to: " + outPath);
                    break;

                case "5":
                    updateParticipantByNumber(sc);
                    break;

                case "6":
                    deleteParticipantByNumber(sc);
                    break;

                case "7":
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static int askTeamSize(Scanner sc, int max) {
        while (true) {
            System.out.print("Enter team size (min 3): ");
            String in = sc.nextLine().trim();
            try {
                int val = Integer.parseInt(in);
                if (val < 3 || val > max) {
                    System.out.println("Team size must be between 3 and " + max);
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Enter a number.");
            }
        }
    }

    private static void showTeams() {
        if (teams.isEmpty()) {
            System.out.println("No teams formed yet.");
            return;
        }
        System.out.println("\n===== TEAMS FORMED =====");
        for (Team t : teams) {
            System.out.println("\n" + t.getTeamName() + ":");
            for (Participant p : t.getMembers()) {
                System.out.println(" - " + p.getName() + " | " + p.getRole()
                        + " | Skill " + p.getSkillLevel()
                        + " | Personality " + p.getPersonalityType());
            }
        }
    }

    // ---------- UPDATE / DELETE USING NUMBER (INDEX) ----------
    private static void printParticipantsWithNumbers() {
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        System.out.println("\n===== ALL PARTICIPANTS =====");
        int idx = 1;
        for (Participant p : participants) {
            System.out.println(idx++ + ". " +
                    p.getName() +
                    " | " + p.getEmail() +
                    " | Game: " + p.getPreferredGame() +
                    " | Role: " + p.getRole() +
                    " | Skill: " + p.getSkillLevel() +
                    " | Personality: " + p.getPersonalityType());
        }
    }

    private static void updateParticipantByNumber(Scanner sc) {
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        printParticipantsWithNumbers();
        System.out.print("Enter participant number to UPDATE: ");
        String updStr = sc.nextLine().trim();
        int updIndex;
        try {
            updIndex = Integer.parseInt(updStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }
        if (updIndex < 1 || updIndex > participants.size()) {
            System.out.println("Index out of range.");
            return;
        }

        Participant target = participants.get(updIndex - 1);
        System.out.println("Updating: " + target.getName());

        // Game
        System.out.print("New game (" + target.getPreferredGame() +
                "), press Enter to keep: ");
        String g = sc.nextLine().trim();
        if (!g.isEmpty()) target.setPreferredGame(g);

        // Role
        System.out.print("New role (" + target.getRole() +
                "), press Enter to keep: ");
        String r = sc.nextLine().trim();
        if (!r.isEmpty()) target.setRole(r);

        // Skill
        System.out.print("New skill (" + target.getSkillLevel() +
                "), press Enter to keep: ");
        String s = sc.nextLine().trim();
        if (!s.isEmpty()) {
            try {
                target.setSkillLevel(Integer.parseInt(s));
            } catch (NumberFormatException ignore) {}
        }
        System.out.println("✅ Participant updated.");
    }

    private static void deleteParticipantByNumber(Scanner sc) {
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        printParticipantsWithNumbers();
        System.out.print("Enter participant number to DELETE: ");
        String delStr = sc.nextLine().trim();
        int delIndex;
        try {
            delIndex = Integer.parseInt(delStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }
        if (delIndex < 1 || delIndex > participants.size()) {
            System.out.println("Index out of range.");
            return;
        }
        Participant removed = participants.remove(delIndex - 1);
        System.out.println("✅ Deleted: " + removed.getName());
    }

    // ================= PARTICIPANT MENU ===================
    private static void participantMenu(Scanner sc,
                                        ParticipantSurveyService surveyService,
                                        Participant account) {

        boolean back = false;
        while (!back) {
            System.out.println("\n--- Participant Menu ---");
            System.out.println("1. Complete survey");
            System.out.println("2. View my team details");
            System.out.println("3. Logout");
            System.out.print("Enter choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": {
                    // add more new members (survey)
                    List<Participant> extra =
                            surveyService.collectNewParticipants(sc, participants.size());
                    participants.addAll(extra);
                    System.out.println("Participants after adding: " + participants.size());
                    break;

                }

                case "2": {
                    Team myTeam = null;
                    for (Team t : teams) {
                        for (Participant p : t.getMembers()) {
                            if (p.getEmail() != null &&
                                    p.getEmail().equalsIgnoreCase(account.getEmail())) {
                                myTeam = t;
                                break;
                            }
                        }
                        if (myTeam != null) break;
                    }
                    if (myTeam == null) {
                        System.out.println("You are not assigned to any team yet.");
                    } else {
                        System.out.println("\nYour Team: " + myTeam.getTeamName());
                        for (Participant p : myTeam.getMembers()) {
                            System.out.println(" - " + p.getName()
                                    + " | " + p.getRole()
                                    + " | Skill " + p.getSkillLevel()
                                    + " | Personality " + p.getPersonalityType());
                        }
                    }
                    break;
                }

                case "3":
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // (Optional for future: you can reuse these if needed)
    private static int askIntInRange(Scanner sc, String msg, int min, int max) {
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

    private static String chooseFromOptions(Scanner sc, String title, String[] options) {
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
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid choice. Try again.");
        }
    }
}
