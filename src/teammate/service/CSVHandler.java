package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

// Handles loading participants from CSV files and exporting formed teams to CSV with logging.
public class CSVHandler {

    private final LoggerService logger = LoggerService.getInstance();
    /**
     * Loads participants from a CSV file with full validation.
     * CSV Format expected:
     * ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType
     */
    public ArrayList<Participant> loadParticipants(String filePath, LoggerService logger) {

        ArrayList<Participant> participants = new ArrayList<>();
        File file = new File(filePath);

        // ----------- 1. Validate File Existence -----------
        // [CSV 1.3.1] check file exists?
        if (!file.exists() || !file.isFile()) {
            // [CSV 1.3.1.1] File not found
            System.out.println("Error: File not found: " + filePath); // [CSV 1.3.1.1.1] Display("Error: File not found")
            logger.error("CSV load failed – file not found: " + filePath); // [CSV 1.3.1.1.2] log
            return participants; // [CSV 1.3.1.1.3] return empty list
        }

        // ----------- 2. Validate Extension -----------
        // [CSV 1.3.2] check extension .csv
        if (!filePath.toLowerCase().endsWith(".csv")) {
            // [CSV 1.3.2.1] Not CSV
            System.out.println("Error: File is not a .csv file."); // [CSV 1.3.3.1.1]
            logger.error("CSV load failed – not a CSV: " + filePath); // [CSV 1.3.3.1.2] log
            return participants; // [CSV 1.3.3.1.3] return empty list
        }

        // ----------- 3. OPEN AND READ CSV -----------
        // [CSV 1.3.3] Open reader and read header
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine(); // Header
            // [CSV 1.3.3.1] Header null empty full
            if (line == null) {
                System.out.println("Error: CSV file is empty."); // [CSV 1.3.3.1.1]
                logger.error("CSV load failed – empty file: " + filePath); // [CSV 1.3.3.1.2] log
                return participants; // [CSV 1.3.3.1.3] return empty list
            }

            int lineNo = 1;

            // [CSV 2] loop [For each data line in CSV]
            // [CSV 2.1] read and trim line
            while ((line = br.readLine()) != null) {
                lineNo++;

                // [CSV 2.1.1] Empty Line
                if (line.trim().isEmpty()) continue; // [CSV 2.1.1.1] skip line

                // ----------- CSV Columns -----------
                // Expecting 8 columns:
                // 0: ID // 1: Name // 2: Email // 3: PreferredGame // 4: SkillLevel
                // 5: PreferredRole // 6: PersonalityScore // 7: PersonalityType

                // [CSV 2.2]
                // [CSV 2.2.1] split into data 0..7
                String[] data = line.split(",");

                // [CSV 2.2.2] Missing columns (data.length < 8)
                if (data.length < 8) {
                    System.out.println("Warning: Line " + lineNo + " missing columns. Skipping."); // [CSV 2.2.2.1]
                    logger.info("CSV load warning: line " + lineNo + " missing columns in " + filePath); // [CSV 2.2.2.2] log
                    continue;
                }

                // Extract fields
                String name = data[1].trim();
                String email = data[2].trim();
                String game = data[3].trim();
                String skillStr = data[4].trim();
                String role = data[5].trim();
                String scoreStr = data[6].trim();
                String personalityType = data[7].trim();

                // ----------- Validate Skill (must be number) -----------
                // [CSV 2.3]
                // [CSV 2.3.1] parse skill value
                int skill;
                try {
                    skill = Integer.parseInt(skillStr);
                } catch (NumberFormatException ex) {
                    // [CSV 2.3.2] alt: skill not a number
                    System.out.println("Warning: Invalid skill value on line " +
                            lineNo + " (" + skillStr + "). Skipping."); // [CSV 2.3.2.1]
                    logger.info("CSV load warning: invalid skill '" + skillStr +
                            "' on line " + lineNo + " in file " + filePath); // [CSV 2.3.2.2] log
                    continue;
                }

                // ----------- Validate Personality Score (optional) -----------
                int personalityScore = 0;
                try {
                    personalityScore = Integer.parseInt(scoreStr);
                } catch (Exception ignore) {
                    // score optional – no skip
                }

                // ----------- CREATE Participant Object -----------
                // [CSV 2.4] Create Participant
                // [CSV 2.4.1] Add new participant
                Participant p = new Participant(name, email, game, skill, role);
                // [CSV 2.4.2] Set personality score and type
                p.setPersonalityScore(personalityScore);
                p.setPersonalityType(personalityType);

                participants.add(p);
            }

        } catch (IOException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
            logger.error("Error reading CSV file: " + filePath, e);
        }


        // ----------- SUMMARY -----------
        if (participants.isEmpty()) {
            // [CSV 3.1] list empty
            System.out.println("No valid participants found."); // [CSV 3.1.1]
            logger.info("CSV load finished with 0 valid participants from " + filePath); // [CSV 3.1.2] log
        } else {
            // [CSV 3.2] One or more participants loaded
            System.out.println("Loaded participants successfully.");
            logger.info("CSV load finished with " + participants.size() +
                    " participants from " + filePath);
        }
        // [CSV 3] Return Participants list
        return participants;
    }

    /**
     * Automatically saves formed teams into:
     *   src/teammate/TeamMembers/teams_yyyyMMdd_HHmmss.csv
     *
     * @param teams list of teams
     * @return full path of the created file
     */
    public String saveTeamsAuto(List<Team> teams, LoggerService logger) {
        try {
            // [EXP 1.2.2] ensure TeamMembers folder exists (mk dirs)
            File folder = new File("src/teammate/TeamMembers");
            if (!folder.exists()) folder.mkdirs();

            /* Proper timestamp: yyyyMMdd_HHmmss */
            // [EXP 1.2.3] generate timestamp & fileName
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String fileName = "teams_" + timestamp + ".csv";
            // [EXP 1.2.4] create file object for output
            File output = new File(folder, fileName);

            // [EXP 1.2.5] open print writer(output)
            try (PrintWriter pw = new PrintWriter(output)) {
                // [EXP 2] Write header and team data
                // [EXP 2.1] write header
                pw.println("TeamName,ID,Name,Email,Game,Skill,Role,Personality");

                // [EXP 2.2] loop
                for (Team t : teams) {
                    // [EXP 2.2.1] loop
                    for (Participant p : t.getMembers()) {
                        // [EXP 2.2.1.1]
                        pw.println(
                                t.getTeamName() + "," +
                                        p.getName() + "," +
                                        p.getEmail() + "," +
                                        p.getPreferredGame() + "," +
                                        p.getSkillLevel() + "," +
                                        p.getRole() + "," +
                                        p.getPersonalityType()
                        );
                    }
                    // [EXP 2.2.2] write blank line between teams
                    pw.println(); // blank line between teams
                }

            }


            logger.info("Teams saved to CSV: " + output.getPath() +
                    " (teams=" + teams.size() + ")");       // [EXP 3.1]
            // [EXP 3.2] return output path
            return output.getPath();


        } catch (Exception e) {
            // [EXP 1.2.5.1]
            logger.error("Error saving teams to CSV", e);    // [EXP 1.2.5.1.1]
            System.out.println("Error saving teams: " + e.getMessage());    // [EXP 1.2.5.1.2] & [EXP 1.2.5.1.4]
            return null;     // [EXP 1.2.5.1.3]
        }

    }
}
