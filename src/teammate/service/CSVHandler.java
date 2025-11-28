package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.LoggerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CSVHandler {

    /**
     * Loads participants from a CSV file with full validation.
     * CSV Format expected:
     *
     * ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType
     */
    public ArrayList<Participant> loadParticipants(String filePath, LoggerService logger) {

        ArrayList<Participant> participants = new ArrayList<>();
        File file = new File(filePath);

        // ----------- 1. Validate File Existence -----------
        if (!file.exists() || !file.isFile()) {
            System.out.println("Error: File not found: " + filePath);
            logger.error("CSV load failed – file not found: " + filePath);
            return participants;
        }

        // ----------- 2. Validate Extension -----------
        if (!filePath.toLowerCase().endsWith(".csv")) {
            System.out.println("Error: File is not a .csv file.");
            logger.error("CSV load failed – not a CSV: " + filePath);
            return participants;
        }

        // ----------- 3. OPEN AND READ CSV -----------
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine(); // Header

            if (line == null) {
                System.out.println("Error: CSV file is empty.");
                logger.error("CSV load failed – empty file: " + filePath);
                return participants;
            }

            int lineNo = 1;

            while ((line = br.readLine()) != null) {
                lineNo++;

                if (line.trim().isEmpty()) continue;

                // ----------- CSV Columns -----------
                // Expecting 8 columns:
                // 0: ID
                // 1: Name
                // 2: Email
                // 3: PreferredGame
                // 4: SkillLevel
                // 5: PreferredRole
                // 6: PersonalityScore
                // 7: PersonalityType

                String[] data = line.split(",");

                if (data.length < 8) {
                    System.out.println("Warning: Line " + lineNo + " missing columns. Skipping.");
                    logger.info("CSV load warning: line " + lineNo + " missing columns in " + filePath);
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
                int skill;
                try {
                    skill = Integer.parseInt(skillStr);
                } catch (NumberFormatException ex) {
                    System.out.println("Warning: Invalid skill value on line " +
                            lineNo + " (" + skillStr + "). Skipping.");
                    logger.info("CSV load warning: invalid skill '" + skillStr +
                            "' on line " + lineNo + " in file " + filePath);
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
                Participant p = new Participant(name, email, game, skill, role);

                p.setPersonalityScore(personalityScore);
                p.setPersonalityType(personalityType);

                participants.add(p);
            }

        } catch (IOException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }

        // ----------- SUMMARY -----------
        if (participants.isEmpty()) {
            System.out.println("No valid participants found.");
            logger.info("CSV load finished with 0 valid participants from " + filePath);
        } else {
            System.out.println("Loaded participants successfully.");
            logger.info("CSV load finished with " + participants.size() +
                    " participants from " + filePath);
        }

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
            File folder = new File("src/teammate/TeamMembers");
            if (!folder.exists()) folder.mkdirs();

            // Proper timestamp: yyyyMMdd_HHmmss
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String fileName = "teams_" + timestamp + ".csv";
            File output = new File(folder, fileName);

            try (PrintWriter pw = new PrintWriter(output)) {

                pw.println("TeamName,ID,Name,Email,Game,Skill,Role,Personality");

                for (Team t : teams) {
                    for (Participant p : t.getMembers()) {
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
                    pw.println(); // blank line between teams
                }

            }

            logger.info("Teams saved to CSV: " + output.getPath() +
                    " (teams=" + teams.size() + ")");
            return output.getPath();


        } catch (Exception e) {
            logger.error("Error saving teams to CSV", e);
            System.out.println("Error saving teams: " + e.getMessage());
            return null;
        }

    }
}
