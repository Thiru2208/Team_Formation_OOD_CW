package teammate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import teammate.model.Participant;
import teammate.model.Team;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVHandlerTest {

    private CSVHandler csvHandler;
    private LoggerService logger;

    @BeforeEach
    void setUp() {
        csvHandler = new CSVHandler();
        logger = LoggerService.getInstance();
    }

    @Test
    void loadParticipants_validCsv_returnsParticipants() throws Exception {
        // --- create temp CSV file ---
        File temp = File.createTempFile("participants_test", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            pw.println("P101,Alice,alice@mail.com,Valorant,8,Strategist,20,Leader");
            pw.println("P102,Bob,bob@mail.com,FIFA,5,Defender,15,Thinker");
        }

        // --- call method ---
        ArrayList<Participant> result =
                csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        // --- assertions ---
        assertEquals(2, result.size());

        Participant p1 = result.get(0);
        assertEquals("Alice", p1.getName());
        assertEquals("alice@mail.com", p1.getEmail());
        assertEquals("Valorant", p1.getPreferredGame());
        assertEquals(8, p1.getSkillLevel());
        assertEquals("Strategist", p1.getRole());
        assertEquals(20, p1.getPersonalityScore());
        assertEquals("Leader", p1.getPersonalityType());
    }

    @Test
    void loadParticipants_missingFile_returnsEmptyList() {
        ArrayList<Participant> result =
                csvHandler.loadParticipants("this_file_does_not_exist.csv", logger);

        assertTrue(result.isEmpty(), "When file missing, list should be empty");
    }

    @Test
    void loadParticipants_wrongExtension_returnsEmptyList() throws Exception {
        File temp = File.createTempFile("participants_test", ".txt");
        temp.deleteOnExit();

        ArrayList<Participant> result =
                csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertTrue(result.isEmpty(), "When extension is not .csv, list should be empty");
    }

    @Test
    void saveTeamsAuto_createsCsvFileWithContent() throws Exception {
        // --- prepare one team with one participant ---
        Participant p = new Participant(
                "Test User",
                "test@example.com",
                "Valorant",
                7,
                "Strategist"
        );
        p.setPersonalityType("Leader");

        Team team = new Team("Team 1");
        team.addMember(p);

        List<Team> teams = new ArrayList<>();
        teams.add(team);

        // --- call method ---
        String path = csvHandler.saveTeamsAuto(teams, logger);

        assertNotNull(path, "Path should not be null");

        File outFile = new File(path);
        assertTrue(outFile.exists(), "Output CSV file should exist");

        // --- optional: check content ---
        String content = Files.readString(outFile.toPath());
        assertTrue(content.contains("Team 1"));
        assertTrue(content.contains("Test User"));
        assertTrue(content.contains("Valorant"));
    }

    @Test
    void loadParticipants_rowWithMissingColumns_isSkipped() throws Exception {
        File temp = File.createTempFile("participants_missing", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            pw.println("P101,Alice,alice@mail.com,Valorant,8"); // only 5 columns → skip
            pw.println("P102,Bob,bob@mail.com,FIFA,5,Defender,10,Thinker"); // valid
        }

        ArrayList<Participant> list = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertEquals(1, list.size(), "Only valid row must be loaded");
        assertEquals("Bob", list.get(0).getName());
    }

    @Test
    void loadParticipants_invalidSkill_skipsThatRow() throws Exception {
        File temp = File.createTempFile("participants_invalid_skill", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            pw.println("P101,Alice,alice@mail.com,Valorant,abc,Strategist,20,Leader"); // invalid skill
            pw.println("P102,Bob,bob@mail.com,FIFA,5,Defender,15,Thinker");
        }

        ArrayList<Participant> result = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).getName());
    }

    @Test
    void loadParticipants_emptyFile_returnsEmpty() throws Exception {
        File temp = File.createTempFile("participants_empty", ".csv");
        temp.deleteOnExit();

        // write NOTHING

        ArrayList<Participant> list = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertTrue(list.isEmpty(), "Empty CSV must return empty list");
    }

    @Test
    void loadParticipants_headerOnly_returnsEmptyList() throws Exception {
        File temp = File.createTempFile("participants_header_only", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
        }

        ArrayList<Participant> list = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertTrue(list.isEmpty(), "No data rows → empty list expected");
    }

    @Test
    void loadParticipants_skipsBlankLines() throws Exception {
        File temp = File.createTempFile("participants_blanklines", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            pw.println();
            pw.println("P101,Alice,alice@mail.com,Valorant,7,Strategist,10,Leader");
            pw.println();
        }

        ArrayList<Participant> list = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertEquals(1, list.size());
        assertEquals("Alice", list.get(0).getName());
    }

    @Test
    void loadParticipants_missingPersonalityScore_defaultsZero() throws Exception {
        File temp = File.createTempFile("participants_missing_score", ".csv");
        temp.deleteOnExit();

        try (PrintWriter pw = new PrintWriter(new FileWriter(temp))) {
            pw.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            pw.println("P101,Alice,alice@mail.com,Valorant,5,Strategist,,Leader");
        }

        ArrayList<Participant> list = csvHandler.loadParticipants(temp.getAbsolutePath(), logger);

        assertEquals(1, list.size());
        assertEquals(0, list.get(0).getPersonalityScore());
    }

    @Test
    void saveTeamsAuto_multipleTeams_allTeamsSaved() throws Exception {
        Participant p1 = new Participant("A", "a@mail.com", "Valorant", 8, "Strategist");
        Participant p2 = new Participant("B", "b@mail.com", "FIFA", 5, "Defender");

        Team t1 = new Team("Team Alpha");
        t1.addMember(p1);

        Team t2 = new Team("Team Beta");
        t2.addMember(p2);

        List<Team> teams = List.of(t1, t2);

        String path = csvHandler.saveTeamsAuto(teams, logger);

        assertNotNull(path);
        File file = new File(path);
        assertTrue(file.exists());

        String content = Files.readString(file.toPath());

        assertTrue(content.contains("Team Alpha"));
        assertTrue(content.contains("Team Beta"));
        assertTrue(content.contains("A"));
        assertTrue(content.contains("B"));
    }

    @Test
    void saveTeamsAuto_createsFolderIfMissing() {
        File folder = new File("src/teammate/TeamMembers");
        if (folder.exists()) {
            folder.delete();
        }

        Participant p = new Participant("User", "u@mail.com", "FIFA", 4, "Defender");
        Team team = new Team("TestTeam");
        team.addMember(p);

        String path = csvHandler.saveTeamsAuto(List.of(team), logger);

        assertNotNull(path);
        assertTrue(new File(path).exists(), "CSV should be created even if folder was missing");
    }




}
