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
}
