package teammate.app;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.*;

import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static ArrayList<Participant> participants = new ArrayList<>();
    private static ArrayList<Team> teams = new ArrayList<>();

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        TeamBuilder teamBuilder = new TeamBuilder();
        AuthService authService = new AuthService();
        LoggerService logger = new LoggerService();
        ParticipantSurveyService surveyService = new ParticipantSurveyService();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        logger.info("Application started");
        System.out.println();
        System.out.println("==== TeamMate: Intelligent Team Formation System ====\n");

        boolean running = true;
        while (running) {
            System.out.println("Main Menu");
            System.out.println("1. Organizer login");
            System.out.println("2. Participant sign up");
            System.out.println("3. Participant login");
            System.out.println("4. Exit");
            System.out.println();
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    if (authService.organizerLogin(sc)) {
                        logger.info("Organizer logged in");
                        organizerMenu(sc, csvHandler, teamBuilder, logger, executor); // pass logger
                    } else {
                        logger.info("Organizer login failed");
                    }
                    break;

                case "2": {
                    Participant newP = authService.participantSignup(sc);
                    if (newP != null) {
                        participants.add(newP);
                        logger.info("New participant signed up and added to system: " + newP.getName());
                        System.out.println("✅ New participant added to system: " + newP.getName());
                    } else {
                        logger.info("Participant signup failed");
                        System.out.println("⚠ Signup failed.");
                    }
                    break;
                }

                case "3": {
                    Participant logged = authService.participantLogin(sc);
                    if (logged != null) {
                        logger.info("Participant logged in: " + logged.getName());
                        participantMenu(sc, surveyService, logged, logger, authService, executor);
                    } else {
                        logger.info("Participant login failed");
                    }
                    break;
                }

                case "4":
                    logger.info("Application exiting");
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
                                      TeamBuilder teamBuilder,
                                      LoggerService logger, ExecutorService executor) {


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
            System.out.println();
            System.out.print("Enter choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": {
                    System.out.println();
                    System.out.println("Upload member details");
                    System.out.print("Enter CSV path: ");
                    String path = sc.nextLine().trim();
                    ArrayList<Participant> loaded = csvHandler.loadParticipants(path, logger);
                    if (!loaded.isEmpty()) {
                        participants.addAll(loaded);
                        logger.info("Organizer loaded " + loaded.size() + " participants from CSV: " + path);
                        // Merge with existing (keep sign-ups also)
                        System.out.println("Loaded " + loaded.size() + " participants. Current Total: " + participants.size());
                        // ✅ process survey data in a background thread
                        executor.submit(new SurveyProcessingTask(loaded, logger));
                    } else {
                        logger.info("Organizer attempted to load participants but file was empty/invalid: " + path);
                        System.out.println("No valid participants loaded.");
                    }
                    break;
                }

                case "2":
                    if (participants.isEmpty()) {
                        System.out.println();
                        System.out.println("Please upload/add participants first.");
                        break;
                    }
                    int teamSize = askTeamSize(sc, participants.size());
                    try {
                        // ✅ run team formation in a separate thread
                        TeamFormationTask task =
                                new TeamFormationTask(participants, teamSize, teamBuilder, logger);
                        Future<ArrayList<Team>> future = executor.submit(task);
                        // wait until background thread finishes team formation
                        teams = future.get();
                        logger.info("Teams formed: " + teams.size() + " with team size " + teamSize);
                        System.out.println("Teams formed: " + teams.size() + " with team size " + teamSize);
                    } catch (Exception e) {
                        System.out.println("Error forming teams. See logs for details.");
                        logger.error("Exception while forming teams: " + e.getMessage());
                    }
                    break;

                case "3":
                    showTeams();
                    break;

                case "4":
                    if (teams.isEmpty()) {
                        System.out.println("⚠ No teams to export.");
                        break;
                    }
                    String outPath = csvHandler.saveTeamsAuto(teams, logger);
                    logger.info("Teams exported to CSV: " + outPath);
                    System.out.println("Exported to: " + outPath + "successfully.");
                    break;

                case "5":
                    updateParticipantByNumber(sc, logger);
                    break;

                case "6":
                    deleteParticipantByNumber(sc, logger);
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
            System.out.println();
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

    private static void updateParticipantByNumber(Scanner sc, LoggerService logger) {

        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        System.out.println();
        System.out.println("\n===== ALL PARTICIPANTS =====");
        printParticipantsWithNumbers();
        System.out.println();
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
        logger.info("Participant updated: " + target.getName());
    }

    private static void deleteParticipantByNumber(Scanner sc, LoggerService logger) {
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        System.out.println();
        System.out.println("\n===== ALL PARTICIPANTS =====");
        printParticipantsWithNumbers();
        System.out.println();
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
        logger.info("Participant deleted: " + removed.getName());
    }

    // ================= PARTICIPANT MENU ===================
    private static void participantMenu(Scanner sc,
                                        ParticipantSurveyService surveyService,
                                        Participant account,
                                        LoggerService logger,
                                        AuthService authService, ExecutorService executor) {

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
                    // Ensure this account is inside global participants list
                    if (!participants.contains(account)) {
                        participants.add(account);
                    }

                    // run survey (this also saves to CSV via AuthService)
                    surveyService.runSurveyForExistingParticipant(sc, account, authService, logger);

                    // background "survey processing" on this single participant too
                    executor.submit(new SurveyProcessingTask(
                            Collections.singletonList(account), logger));

                    logger.info("Survey accessed by participant: " + account.getName());
                    break;
                }

                case "2": {
                    Team myTeam = null;

                    for (Team t : teams) {
                        for (Participant p : t.getMembers()) {

                            boolean sameEmail = (p.getEmail() != null && account.getEmail() != null &&
                                    p.getEmail().equalsIgnoreCase(account.getEmail()));
                            boolean sameName = p.getName().equalsIgnoreCase(account.getName());

                            if (sameEmail || sameName) {
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
}
