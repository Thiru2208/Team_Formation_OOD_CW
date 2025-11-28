package teammate.app;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.*;

import java.util.*;
import java.util.concurrent.*;

import static teammate.service.ParticipantSurveyService.GAME_OPTIONS;
import static teammate.service.ParticipantSurveyService.ROLE_OPTIONS;

public class Main {

    private static ArrayList<Participant> participants = new ArrayList<>();
    private static ArrayList<Team> teams = new ArrayList<>();
    private static final LoggerService logger = LoggerService.getInstance();

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
        System.out.println("========= TeamMate: Intelligent Team Formation System ========= ");

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Main Menu ---");
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
                        organizerMenu(sc, csvHandler, teamBuilder, authService, logger, executor); // pass logger
                    } else {
                        logger.info("Organizer login failed");
                    }
                    break;

                case "2": {
                    Participant newP = authService.participantSignup(sc);
                    if (newP != null) {
                        participants.add(newP);
                        logger.info("New participant signed up and added to system: " + newP.getName());
                        System.out.println("New participant added to system: " + newP.getName());
                    } else {
                        logger.info("Participant signup failed");
                        System.out.println("Signup failed.");
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
                    System.out.println();
                    System.out.println("Exiting system...");
                    executor.shutdown();
                    logger.info("Executor service shut down. Application exiting.");
                    break;

                default:
                    System.out.println("Invalid choice.\n");
            }
        }
    }

    // ================= ORGANIZER MENU ===================
    private static void organizerMenu(Scanner sc,
                                      CSVHandler csvHandler,
                                      TeamBuilder teamBuilder, AuthService authService,
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
            System.out.println("7. Change organizer password");
            System.out.println("8. Logout");
            System.out.println();
            System.out.print("Enter choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": {
                    System.out.println();
                    System.out.println("--- Upload Member Details ---");
                    System.out.print("Enter CSV path: ");
                    String path = sc.nextLine().trim();
                    ArrayList<Participant> loaded = csvHandler.loadParticipants(path, logger);
                    if (!loaded.isEmpty()) {
                        participants.addAll(loaded);
                        logger.info("Organizer loaded " + loaded.size() + " participants from CSV: " + path);
                        // Merge with existing (keep sign-ups also)
                        System.out.println("Loaded " + loaded.size() + " participants. Current Total: " + participants.size());
                        // process survey data in a background thread
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
                    System.out.println();
                    System.out.println("--- Team Formation ---");
                    int teamSize = askTeamSize(sc, participants.size());
                    TeamFormationTask tfTask = new TeamFormationTask(participants, teamSize, teamBuilder);
                    Thread tfThread = new Thread(tfTask, "TeamFormationThread");

                    try {
                        logger.info("Starting TeamFormationThread. teamSize=" + teamSize);
                        tfThread.start();
                        tfThread.join();  // wait till building finishes
                        teams = tfTask.getResult();
                        logger.info("Teams formed: " + teams.size() + " with team size " + teamSize);
                        System.out.println("Teams formed: " + teams.size() + " with team size " + teamSize);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Team formation was interrupted. Error forming teams. See logs for details.");
                        logger.error("Team formation thread interrupted: " + e.getMessage());
                    }

                    break;

                case "3":
                    showTeams();
                    break;

                case "4":
                    if (teams.isEmpty()) {
                        System.out.println("No teams to export.");
                        break;
                    }
                    String outPath = csvHandler.saveTeamsAuto(teams, logger);
                    logger.info("Teams exported to CSV: " + outPath);
                    System.out.println();
                    System.out.println("--- Export teams ---");
                    System.out.println("Teams exported to: " + outPath + "successfully.");
                    break;

                case "5":
                    updateParticipantByNumber(sc, logger, authService);
                    break;

                case "6":
                    deleteParticipantByNumber(sc, logger);
                    break;

                case "7":
                    authService.changeOrganizerPassword(sc);
                    break;

                case "8":
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
        System.out.println();
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

    private static void updateParticipantByNumber(Scanner sc,
                                                  LoggerService logger,
                                                  AuthService authService) {

        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }

        printParticipantsWithNumbers();
        System.out.println();
        System.out.println("--- Update Participant Details ---");
        System.out.print("Enter participant number to UPDATE: ");
        String updStr = sc.nextLine().trim();
        int updIndex;

        try {
            updIndex = Integer.parseInt(updStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            logger.error("Organizer entered invalid participant index for update: " + updStr, e);
            return;
        }

        if (updIndex < 1 || updIndex > participants.size()) {
            System.out.println("Index out of range.");
            logger.error("Organizer entered out-of-range index for update: " + updIndex);
            return;
        }

        Participant target = participants.get(updIndex - 1);
        System.out.println("Updating: " + target.getName());

        // ---- backup old values (for cancel) ----
        String oldGame  = target.getPreferredGame();
        String oldRole  = target.getRole();
        int    oldSkill = target.getSkillLevel();

        // ================== UPDATE GAME (with options) ==================
        System.out.println("\nSelect NEW Game (or 0 to keep current)");
        System.out.println("Current game: " + target.getPreferredGame());
        System.out.println("0. Keep current");
        for (int i = 0; i < GAME_OPTIONS.length; i++) {
            System.out.println((i + 1) + ". " + GAME_OPTIONS[i]);
        }
        while (true) {
            System.out.print("Enter choice: ");
            String gameChoice = sc.nextLine().trim();
            try {
                int gc = Integer.parseInt(gameChoice);
                if (gc == 0) {
                    // keep old game
                    break;
                } else if (gc >= 1 && gc <= GAME_OPTIONS.length) {
                    target.setPreferredGame(GAME_OPTIONS[gc - 1]);
                    break;
                } else {
                    System.out.println("Please enter 0-" + GAME_OPTIONS.length);
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        // ================== UPDATE ROLE (with options) ==================
        System.out.println("\nSelect NEW Role (or 0 to keep current)");
        System.out.println("Current role: " + target.getRole());
        System.out.println("0. Keep current");
        for (int i = 0; i < ROLE_OPTIONS.length; i++) {
            System.out.println((i + 1) + ". " + ROLE_OPTIONS[i]);
        }
        while (true) {
            System.out.print("Enter choice: ");
            String roleChoice = sc.nextLine().trim();
            try {
                int rc = Integer.parseInt(roleChoice);
                if (rc == 0) {
                    break;
                } else if (rc >= 1 && rc <= ROLE_OPTIONS.length) {
                    target.setRole(ROLE_OPTIONS[rc - 1]);
                    break;
                } else {
                    System.out.println("Please enter 0-" + ROLE_OPTIONS.length);
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        // ================== UPDATE SKILL (menu style) ==================
        System.out.println("\nSelect NEW Skill Level (1–10) or 0 to keep current");
        System.out.println("Current skill: " + oldSkill);
        while (true) {
            System.out.print("Enter skill (0 = keep): ");
            String skillStr = sc.nextLine().trim();
            try {
                int sv = Integer.parseInt(skillStr);
                if (sv == 0) {
                    break; // keep old skill
                } else if (sv >= 1 && sv <= 10) {
                    target.setSkillLevel(sv);
                    break;
                } else {
                    System.out.println("Please enter 0 or a value from 1 to 10.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }

        // ---- show summary of new values ----
        System.out.println("\nReview updated details:");
        System.out.println("Name : " + target.getName());
        System.out.println("Game : " + target.getPreferredGame());
        System.out.println("Role : " + target.getRole());
        System.out.println("Skill: " + target.getSkillLevel());

        // ---- confirm permanent save ----
        while (true) {
            System.out.print("Save these changes permanently? (Y/N): ");
            String ans = sc.nextLine().trim();

            if (ans.equalsIgnoreCase("Y")) {
                try {
                    authService.saveAllAccountsToFile();
                    logger.info("Participant permanently updated: " + target.getName());
                    System.out.println("Successfully updated & saved: " + target.getName());
                } catch (Exception e) {
                    logger.error("Failed to save updated participant to file: " + target.getName(), e);
                    System.out.println("Failed to save changes. Please try again.");
                }
                break;

            } else if (ans.equalsIgnoreCase("N")) {
                // revert to old values
                target.setPreferredGame(oldGame);
                target.setRole(oldRole);
                target.setSkillLevel(oldSkill);

                logger.info("Update cancelled for participant: " + target.getName());
                System.out.println("Changes discarded. Original values kept.");
                break;

            } else {
                System.out.println("Please enter Y or N.");
            }
        }
    }

    private static void deleteParticipantByNumber(Scanner sc, LoggerService logger) {
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");
            return;
        }
        System.out.println();
        printParticipantsWithNumbers();
        System.out.println();
        System.out.println("--- Delete Participant Details ---");
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
        System.out.println("Successfully Deleted: " + removed.getName());
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
            System.out.println("3. Change my password");
            System.out.println("4. Logout");
            System.out.println();
            System.out.print("Enter choice: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": {
                    // Ensure this account is inside global participants list
                    if (!participants.contains(account)) {
                        participants.add(account);
                    }

                    // run survey in its own thread
                    SurveyProcessingTask task =
                            new SurveyProcessingTask(surveyService, sc, account, authService);
                    Thread surveyThread = new Thread(task, "SurveyProcessingThread");

                    try {
                        logger.info("Starting SurveyProcessingThread for " + account.getName());
                        surveyThread.start();
                        surveyThread.join(); // wait until survey completes
                        logger.info("SurveyProcessingThread finished for " + account.getName());
                    } catch (InterruptedException e) {
                        logger.error("Survey thread interrupted for user " + account.getName(), e);
                        Thread.currentThread().interrupt();
                        System.out.println("Survey processing was interrupted.");
                    }
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
                        System.out.println();
                        System.out.println("You are not assigned to any team yet.");
                    } else {
                        System.out.println();
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
                    authService.changeParticipantPassword(sc, account);
                    break;

                case "4":
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
