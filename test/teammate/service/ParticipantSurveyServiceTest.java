package teammate.service;

import org.junit.jupiter.api.Test;
import teammate.model.Participant;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantSurveyServiceTest {
    String ACCOUNTS_FILE = "test/teammate/auth/test_participant_accounts.csv";

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
        public void saveAllAccountsToFile(String file) {
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

        service.runSurveyForExistingParticipant(sc, p, fakeAuth, ACCOUNTS_FILE);

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

        service.runSurveyForExistingParticipant(sc, p, fakeAuth, ACCOUNTS_FILE);

        // nothing should change
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals(8, p.getSkillLevel());
        assertEquals("Strategist", p.getRole());
        assertEquals("Leader", p.getPersonalityType());
        assertEquals(80, p.getPersonalityScore());

        // and no save call
        assertFalse(fakeAuth.saved);
    }

    @Test
    void testCalculatePersonalityTieDefaultsToBalanced() {
        ParticipantSurveyService service = new ParticipantSurveyService();

        // leaderScore = q1 + q5 = 4 + 2 = 6
        // thinkerScore = q2 + q4 = 3 + 3 = 6
        // balancedScore = q3 * 2 = 3 * 2 = 6
        // all equal → should hit default "Balanced"
        String type = service.calculatePersonality(4, 3, 3, 3, 2);

        assertEquals("Balanced", type);
    }

    @Test
    void testRunSurvey_handlesInvalidGameChoiceThenValid() {
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

        // 99 → invalid game, then 1 → Valorant
        String input =
                "99\n" +  // invalid game
                        "1\n"  +  // valid game (Valorant)
                        "7\n"  +  // skill
                        "2\n"  +  // role
                        "5\n" +   // Q1
                        "4\n" +   // Q2
                        "3\n" +   // Q3
                        "4\n" +   // Q4
                        "5\n";    // Q5

        Scanner sc = new Scanner(input);
        FakeAuthService fakeAuth = new FakeAuthService();

        service.runSurveyForExistingParticipant(sc, p, fakeAuth, ACCOUNTS_FILE);

        assertEquals("Valorant", p.getPreferredGame());
        assertEquals(7, p.getSkillLevel());
        assertEquals("Attacker", p.getRole());
        assertTrue(fakeAuth.saved, "saveAllAccountsToFile should be called after survey");
    }

    @Test
    void testRunSurvey_handlesNonNumericSkillThenValid() {
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

        // 1 → game
        // "abc" → invalid skill
        // 8 → valid skill
        String input =
                "1\n" +     // game (Valorant)
                        "abc\n" +   // invalid skill
                        "8\n" +     // valid skill
                        "3\n" +     // role (Defender)
                        "4\n" +     // Q1
                        "4\n" +     // Q2
                        "4\n" +     // Q3
                        "4\n" +     // Q4
                        "4\n";      // Q5

        Scanner sc = new Scanner(input);
        FakeAuthService fakeAuth = new FakeAuthService();

        service.runSurveyForExistingParticipant(sc, p, fakeAuth, ACCOUNTS_FILE);

        assertEquals(8, p.getSkillLevel(), "Skill should be set from the valid numeric input");
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals("Defender", p.getRole());
        assertTrue(fakeAuth.saved);
    }

    @Test
    void testRunSurvey_handlesOutOfRangeSkillThenValid() {
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

        // 1 → game
        // 0, 11 → invalid (out of 1–10)
        // 5 → valid
        String input =
                "1\n" +   // game (Valorant)
                        "0\n" +   // invalid (<1)
                        "11\n" +  // invalid (>10)
                        "5\n" +   // valid
                        "1\n" +   // role (Strategist)
                        "3\n" +   // Q1
                        "3\n" +   // Q2
                        "3\n" +   // Q3
                        "3\n" +   // Q4
                        "3\n";    // Q5

        Scanner sc = new Scanner(input);
        FakeAuthService fakeAuth = new FakeAuthService();

        service.runSurveyForExistingParticipant(sc, p, fakeAuth, ACCOUNTS_FILE);

        assertEquals(5, p.getSkillLevel());
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals("Strategist", p.getRole());
        assertTrue(fakeAuth.saved);
    }

    @Test
    void testRunSurvey_saveFailureDoesNotCrashAndParticipantUpdated() {
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

        // simple valid inputs
        String input =
                "1\n" +  // game (Valorant)
                        "6\n" +  // skill
                        "1\n" +  // role (Strategist)
                        "4\n" +  // Q1
                        "4\n" +  // Q2
                        "4\n" +  // Q3
                        "4\n" +  // Q4
                        "4\n";   // Q5

        Scanner sc = new Scanner(input);

        // AuthService stub that throws
        class FailingAuthService extends AuthService {
            @Override
            public void saveAllAccountsToFile(String file) {
                throw new RuntimeException("Simulated IO failure");
            }
        }

        AuthService failingAuth = new FailingAuthService();

        // should NOT throw out of the method (it catches internally)
        service.runSurveyForExistingParticipant(sc, p, failingAuth, ACCOUNTS_FILE);

        // Still updated in memory even if file save failed
        assertEquals("Valorant", p.getPreferredGame());
        assertEquals(6, p.getSkillLevel());
        assertEquals("Strategist", p.getRole());
        assertEquals(
                service.calculatePersonality(4, 4, 4, 4, 4),
                p.getPersonalityType()
        );
    }

    @Test
    void roleAndGameOptionsContainExpectedValues() {
        assertTrue(
                java.util.Arrays.asList(ParticipantSurveyService.ROLE_OPTIONS)
                        .contains("Strategist")
        );
        assertTrue(
                java.util.Arrays.asList(ParticipantSurveyService.GAME_OPTIONS)
                        .contains("Valorant")
        );
    }

}
