package teammate.service;

import org.junit.jupiter.api.Test;
import teammate.model.Participant;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantSurveyServiceTest {

    // ---------- simple personality logic tests ----------

    @Test
    void testCalculatePersonalityLeader() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        // q1 and q5 high → leaderScore biggest
        String type = service.calculatePersonality(5, 2, 2, 1, 5);

        assertEquals("Leader", type);
    }

    @Test
    void testCalculatePersonalityThinker() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        // q2 and q4 high → thinkerScore biggest
        String type = service.calculatePersonality(1, 5, 2, 5, 1);

        assertEquals("Thinker", type);
    }

    @Test
    void testCalculatePersonalityBalanced() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        // teamwork (q3) high → balanced
        String type = service.calculatePersonality(2, 2, 5, 2, 2);

        assertEquals("Balanced", type);
    }

    // ---------- helper fake AuthService for survey ----------

    /** Small stub that only tracks if saveAllAccountsToFile() was called. */
    static class FakeAuthService extends AuthService {
        boolean saved = false;

        @Override
        public void saveAllAccountsToFile() {
            saved = true;   // do NOT write anything in tests
        }
    }

    // ---------- runSurveyForExistingParticipant tests ----------

    @Test
    void testRunSurveyUpdatesParticipantAndSaves() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        Participant p = new Participant(
                "Test User",
                "test@uni.edu",
                "Not selected",
                0,
                "Not selected"
        );
        p.setPersonalityType("Not selected");
        p.setPersonalityScore(0);

        // Input script for Scanner:
        // 1  -> game: Valorant
        // 7  -> skill level
        // 2  -> role: Attacker
        // 5,4,3,4,5 -> answers to Q1–Q5
        String input =
                "1\n" +   // game choice
                        "7\n" +   // skill
                        "2\n" +   // role
                        "5\n" +
                        "4\n" +
                        "3\n" +
                        "4\n" +
                        "5\n";

        Scanner sc = new Scanner(input);
        FakeAuthService fakeAuth = new FakeAuthService();

        service.runSurveyForExistingParticipant(sc, p, fakeAuth);

        // check participant updated correctly
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals(7, p.getSkillLevel());
        assertEquals("Attacker", p.getRole());

        int expectedTotal = 5 + 4 + 3 + 4 + 5;
        int expectedScore = expectedTotal * 4;
        assertEquals(expectedScore, p.getPersonalityScore());

        // personality type should match your calculatePersonality logic
        assertEquals(
                service.calculatePersonality(5, 4, 3, 4, 5),
                p.getPersonalityType()
        );

        // and saveAllAccountsToFile() must be called
        assertTrue(fakeAuth.saved);
    }

    @Test
    void testRunSurveySkippedWhenAlreadyCompleted() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        // already has a personality
        Participant p = new Participant(
                "Existing User",
                "exist@uni.edu",
                "Valorant",
                8,
                "Strategist"
        );
        p.setPersonalityType("Leader");
        p.setPersonalityScore(80);

        FakeAuthService fakeAuth = new FakeAuthService();
        Scanner sc = new Scanner(""); // no input needed, should skip immediately

        service.runSurveyForExistingParticipant(sc, p, fakeAuth);

        // nothing should change
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals(8, p.getSkillLevel());
        assertEquals("Strategist", p.getRole());
        assertEquals("Leader", p.getPersonalityType());
        assertEquals(80, p.getPersonalityScore());

        // and no save call
        assertFalse(fakeAuth.saved);
    }
}
