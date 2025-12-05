package teammate.app;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static teammate.service.ParticipantSurveyService.GAME_OPTIONS;
import static teammate.service.ParticipantSurveyService.ROLE_OPTIONS;

public class Main {
    //Stores all participant records in memory
    static ArrayList<Participant> participants = new ArrayList<>();
   //Holds all formed teams
    private static ArrayList<Team> teams = new ArrayList<>();
    //Logger instance for tracking system activities
    private static final LoggerService logger = LoggerService.getInstance();
    //File path fot participant data
    private static final String ACCOUNTS_FILE =
            "src/teammate/auth/participant_accounts.csv";
    //File path for organizer data
    private static final String ORGANIZER_FILE =
            "src/teammate/auth/organizer_account.csv";

    //Starts the system, shows main menu, and handles all user actions.
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        CSVHandler csvHandler = new CSVHandler();
        TeamBuilder teamBuilder = new TeamBuilder();
        AuthService authService = new AuthService();
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
                    Participant newP = authService.participantSignup(sc, ACCOUNTS_FILE);
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
    //Handles login, signup, survey ,team formation
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
                    // [CSV 1] Select "Upload member details (CSV)"
                    System.out.println();
                    System.out.println("--- Upload Member Details ---");
                    // [CSV 1.1] Prompt for CSV file path
                    System.out.print("Enter CSV path: ");
                    // [CSV 1.2] Enter file path
                    String path = sc.nextLine().trim();
                    // [CSV 1.3] loadParticipants(filePath)
                    ArrayList<Participant> loaded = csvHandler.loadParticipants(path, logger);
                    if (!loaded.isEmpty()) {
                        participants.addAll(loaded);
                        // [CSV 3.2.1] Merge list into global participants
                        logger.info("Organizer loaded " + loaded.size() + " participants from CSV: " + path); // [CSV 3.2.2] log
                        // Merge with existing (keep sign-ups also)
                        System.out.println("Loaded " + loaded.size() + " participants. Current Total: " + participants.size()); // [CSV 3.2.3]
                        // process survey data in a background thread
                    } else {
                        // [CSV 1.3.1.1.5] & [CSV 1.3.2.1.5] & [CSV 1.3.3.1.5] log
                        logger.info("Organizer attempted to load participants but file was empty/invalid: " + path);
                        System.out.println("No valid participants loaded."); // [CSV 1.3.1.1.4] & [CSV 1.3.2.1.4] & [CSV 1.3.3.1.4]
                    }
                    break;
                }

                case "2":
                    // [FORM 1]
                    // [FORM 1.1] check if participants list is empty
                    if (participants.isEmpty()) {
                        // [FORM 1.1.1] No participants
                        System.out.println();
                        System.out.println("Please upload/add participants first.");    // [FORM 1.1.1.1] show "Please upload and add participants first"
                        break;
                    }
                    // // [FORM 1.2] Participants Available
                    System.out.println();
                    System.out.println("--- Team Formation ---");
                    int teamSize = askTeamSize(sc, participants.size());

                    // create the task (same as before)
                    // [FORM 1.2.4]
                    TeamFormationTask tfTask =
                            new TeamFormationTask(participants, teamSize, teamBuilder);

                    try {
                        // [FORM 1.2.5]
                        logger.info("Starting TeamFormationTask via ExecutorService. teamSize=" + teamSize);

                        // submit task to executor – runs in background thread
                        Future<Void> future = executor.submit(() -> {   // [FORM 1.2.6]
                            System.out.println("[Executor] Team formation thread running: "
                                    + Thread.currentThread().getName());
                            tfTask.run();          // same logic inside your Runnable
                            return null;           // because we use Runnable-style task
                        });

                        // wait for completion (like join())
                        future.get();   // [FORM 1.2.7]

                        // [FORM 2.4] get result from task
                        teams = tfTask.getResult();
                        // [FORM 3.1]
                        // [FORM 3.3] One or more teams formed
                        logger.info("Teams formed: " + teams.size() + " with team size " + teamSize); // [FORM 3.3.2] log
                        System.out.println("Teams formed: " + teams.size() + " with team size " + teamSize); // [FORM 3.3.1]

                    } catch (Exception e) {
                        // [FORM 3.2] No teams formed empty results
                        System.out.println("Team formation failed. See logs for details.");  // [FORM 3.2.1]
                        logger.error("Team formation via ExecutorService failed", e);    // [FORM 3.2.2] log
                    }
                    break;

                case "3":
                    showTeams();    // [VIEW 1]
                    break;

                case "4":
                    // [EXP 1] Select "Export teams as CSV"
                    // [EXP 1.1] check if teams list is empty
                    if (teams.isEmpty()) {
                        // [EXP 1.1.1] No teams
                        // [EXP 1.1.1.1]
                        System.out.println("No teams to export.");
                        // [EXP 1.1.1.2]
                        break;
                    }
                    // [EXP 1.2] Teams available
                    // [EXP 1.2.1] saveTeamsAuto(teams, logger)
                    String outPath = csvHandler.saveTeamsAuto(teams, logger);
                    logger.info("Teams exported to CSV: " + outPath);
                    System.out.println();
                    System.out.println("--- Export teams ---");
                    // [EXP 3.3]
                    System.out.println("Teams exported to: " + outPath + "successfully.");
                    // [EXP 3.4]
                    break;

                case "5":
                    // [UPD 1]
                    updateParticipantByNumber(sc, logger, authService);
                    break;

                case "6":
                    // [DEL 1]
                    deleteParticipantByNumber(sc, logger);
                    break;

                case "7":
                    authService.changeOrganizerPassword(sc, ORGANIZER_FILE);
                    break;

                case "8":
                    back = true;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    //Gets a valid team size from the user, ensuring it is between 3 and 15
    static int askTeamSize(Scanner sc, int max) {
        while (true) {
            System.out.print("Enter team size (min 3): ");  // [FORM 1.2.1] ask for team size
            String in = sc.nextLine().trim();   // [FORM 1.2.2] enter team size
            try {
                int val = Integer.parseInt(in);
                if (val < 3 || val > 15) { // max   // [FORM 1.2.3] validate team size range
                    System.out.println("Team size must be between 3 and " + 15);
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Enter a number.");
            }
        }
    }

    //Displays all formed teams with each member's details
    private static void showTeams() {
        // [VIEW 1.1]
        if (teams.isEmpty()) {
            // [VIEW 1.1.1]
            // [VIEW 1.1.1.1]
            System.out.println("No teams formed yet.");
            return;
        }
        // [VIEW 1.2]
        // [VIEW 1.2.1]
        System.out.println("\n===== TEAMS FORMED =====");
        for (Team t : teams) {  // [VIEW 2]
            // [VIEW 2.1]
            System.out.println("\n" + t.getTeamName() + ":");
            for (Participant p : t.getMembers()) {  // [VIEW 2.2]
                // [VIEW 2.2.1]
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

    // Updates a selected participant’s details by number, with validation and optional file saving.
    // [UPD 1] Select "Update participant data"
    public static void updateParticipantByNumber(Scanner sc,
                                                  LoggerService logger,
                                                  AuthService authService) {

        // [UPD 1.1] check if participants list is empty
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");  // [UPD 1.1.1] show "No participants loaded"
            return;
        }

        printParticipantsWithNumbers(); // [UPD 1.2.1] printParticipantsWithNumber()
        System.out.println();
        System.out.println("--- Update Participant Details ---");
        System.out.print("Enter participant number to UPDATE: ");    // [UPD 1.2.2] ask "Enter participant number to UPDATE"
        String updStr = sc.nextLine().trim();     // [UPD 1.2.3] enter participant number (string)
        int updIndex;

        try {
            updIndex = Integer.parseInt(updStr); // [UPD 1.2.4] parse participant number (Integer.parseInt)
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");  // [UPD 1.2.4.1] show "Invalid number"
            logger.error("Organizer entered invalid participant index for update: " + updStr, e); // [UPD 1.2.4.2] error "Invalid number"
            return;
        }

        // [UPD 1.2.4.3] check index in range (1..size)
        if (updIndex < 1 || updIndex > participants.size()) {
            System.out.println("Index out of range.");
            logger.error("Organizer entered out-of-range index for update: " + updIndex);
            // [UPD 1.2.4.4] [UPD 1.2.4.5] error "out-of-range index for update"
            return;
        }

        Participant target = participants.get(updIndex - 1);    // [UPD 1.3.1] get participant by index
        System.out.println("Updating: " + target.getName());    // [UPD 1.3.2] show "Updating target name"

        // ---- backup old values (for cancel) ----
        // [UPD 1.3.3] backup oldGame, oldRole, oldSkill
        String oldGame  = target.getPreferredGame();
        String oldRole  = target.getRole();
        int    oldSkill = target.getSkillLevel();

        // ================== UPDATE GAME (with options) ==================
        System.out.println("\nSelect NEW Game (or 0 to keep current)");
        System.out.println("Current game: " + target.getPreferredGame());
        System.out.println("0. Keep current");   // [UPD 2.1] show current game
        // [UPD 2.2] Update fields
        for (int i = 0; i < GAME_OPTIONS.length; i++) {
            System.out.println((i + 1) + ". " + GAME_OPTIONS[i]);
        }
        while (true) {
            System.out.print("Enter choice: "); // [UPD 2.2.1] ask choices for game / role / skill
            String gameChoice = sc.nextLine().trim();   // [UPD 2.2.2] enter choices
            try {
                int gc = Integer.parseInt(gameChoice);
                if (gc == 0) {
                    // keep old game
                    break;
                } else if (gc >= 1 && gc <= GAME_OPTIONS.length) { // [UPD 2.2.3] Validate choices
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
                    // [UPD 2.2.3.1] all choices valid or kept
                } else if (sv >= 1 && sv <= 10) {   //[UPD 2.2.3.1.1] update target game / role / skill as needed
                    target.setSkillLevel(sv);
                    break;
                } else {
                    System.out.println("Please enter 0 or a value from 1 to 10.");  // [UPD 2.2.3.2] some choice invalid
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");   // [UPD 2.2.3.2.1] show "Please enter valid option"
                // loop continues
            }
        }

        // ---- show summary of new values ----
        // [UPD 3.1] show updated summary
        System.out.println("\nReview updated details:");
        System.out.println("Name : " + target.getName());
        System.out.println("Game : " + target.getPreferredGame());
        System.out.println("Role : " + target.getRole());
        System.out.println("Skill: " + target.getSkillLevel());

        // ---- confirm permanent save ----
        while (true) {
            // [UPD 3.2] Ask to save changes
            System.out.print("Save these changes permanently to file? (Y/N): ");     // [UPD 3.2.1] ask "Save these changes permanently? (Y/N)?"
            String ans = sc.nextLine().trim();    // [UPD 3.2.2] enter answer

            if (ans.equalsIgnoreCase("Y")) {    // [UPD 3.2.3] alt Answer Y
                try {
                    authService.saveAllAccountsToFile("src/teammate/auth/participant_accounts.csv");    // [UPD 3.2.3.1] saveAllAccountsToFile()
                    // [UPD 3.2.3.1.1] Save success
                    // [UPD 3.2.3.1.1.1] ok
                    logger.info("Participant permanently updated: " + target.getName());     // [UPD 3.2.3.1.1.2] info "Participant permanently updated"
                    System.out.println("Successfully updated & saved: " + target.getName());     // [UPD 3.2.3.1.3] show "Successful updated & saved"
                } catch (Exception e) {
                    // [UPD 3.2.3.1.2] save failed (exception)
                    // [UPD 3.2.3.1.2.1] error "Failed to save updated participant"
                    logger.error("Failed to save updated participant to file: " + target.getName(), e);
                    // [UPD 3.2.3.1.2.2] error "Failed to save updated participant"
                    // [UPD 3.2.3.1.2.3] show "Failed to save updated participant"
                    System.out.println("Failed to save changes. Please try again.");
                }
                break;

            } else if (ans.equalsIgnoreCase("N")) { //[UPD 3.2.4] Answer N
                // KEEP new values in memory, just don't write to file
                logger.info("Update kept in memory only (not saved to file) for participant: " + target.getName()); // [UPD 3.2.4.1] info "Update kept in memory only (not saved to file)"
                System.out.println("Changes kept for this session only (not saved to file).");  // [UPD 3.2.4.2] show "Changes kept for this session only"
                break;

            } else {
                // [UPD 3.2.5] Other input
                // [UPD 3.2.5.1] show "Please enter Y or N."
                System.out.println("Please enter Y or N.");
            }
        }
    }

    public static void deleteParticipantByNumber(Scanner sc, LoggerService logger) {
        // [DEL 1.1] check if participant list is empty
        // [DEL 1.1.1] "No participants loaded."
        if (participants.isEmpty()) {
            System.out.println("No participants loaded.");   // [DEL 1.1.1.1] show "No participants loaded."
            return;
        }
        System.out.println();
        printParticipantsWithNumbers(); // [DEL 1.2.1]
        System.out.println();
        System.out.println("--- Delete Participant Details ---");
        System.out.print("Enter participant number to DELETE: ");   // [DEL 1.2.2]
        String delStr = sc.nextLine().trim();   // [DEL 1.2.3]
        int delIndex;
        try {
            delIndex = Integer.parseInt(delStr);     // [DEL 1.2.4]
        } catch (NumberFormatException e) {
            // [DEL 1.2.4.1]
            System.out.println("Invalid number.");// [DEL 1.2.4.1.1]
            return;
        }
        // [DEL 1.2.4.2] Valid number
        // [DEL 1.2.4.2.1] check index range
        if (delIndex < 1 || delIndex > participants.size()) {
            // [DEL 1.2.4.2.2] Index out of range
            System.out.println("Index out of range.");   // [DEL 1.2.4.2.2.1]
            return;
        }
        // [DEL 1.3] Delete Participant
        // [DEL 1.3.1]
        Participant removed = participants.remove(delIndex - 1);    // [DEL 1.3.2]
        System.out.println("Successfully Deleted: " + removed.getName());   // [DEL 1.3.3]
        logger.info("Participant deleted: " + removed.getName());   // [DEL 1.3.4]
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
                    // [SURV 1] Select Complete survey
                    // [SURV 1.1] ensure participant in global list
                    if (!participants.contains(account)) {
                        participants.add(account);
                    }

                    // run survey in its own thread
                    // [SURV 1.2]
                    SurveyProcessingTask task =
                            new SurveyProcessingTask(surveyService, sc, account, authService);
                    Thread surveyThread = new Thread(task, "SurveyProcessingThread");

                    try {
                        // [SURV 1.3] log "Starting SurveyProcessingThread for user"
                        logger.info("Starting SurveyProcessingThread for " + account.getName());
                        // [SURV 1.4] start
                        surveyThread.start();
                        // [SURV 1.5] join
                        surveyThread.join(); // wait until survey completes
                        // [SURV 3.3] log "SurveyProcessingThread finished for user"
                        logger.info("SurveyProcessingThread finished for " + account.getName());
                    } catch (InterruptedException e) {
                        // [SURV 3.2] Exception in task / thread interrupted
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
                    authService.changeParticipantPassword(sc, account, ACCOUNTS_FILE);
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
