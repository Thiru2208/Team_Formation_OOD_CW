package teammate.service;

import org.junit.jupiter.api.Test;
import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamBuilderTest {

    private Participant createParticipant(String name,
                                          String game,
                                          int skill,
                                          String role,
                                          String personality) {
        Participant p = new Participant(
                name,
                name.toLowerCase() + "@uni.test",
                game,
                skill,
                role
        );
        p.setPersonalityType(personality);
        p.setPersonalityScore(0);
        return p;
    }

    @Test
    void buildTeams_withNoParticipants_returnsEmptyList() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> list = new ArrayList<>();

        ArrayList<Team> teams = builder.buildTeams(list, 4, logger);

        assertNotNull(teams);
        assertTrue(teams.isEmpty(), "No teams should be created when there are no participants");
    }

    @Test
    void buildTeams_assignsAllParticipants_andRespectsTeamSizeBounds() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> participants = new ArrayList<>();

        // 10 participants, mixed roles/games/types
        for (int i = 1; i <= 10; i++) {
            String name = "P" + i;
            String game = (i % 2 == 0) ? "Valorant" : "FIFA";
            String role = (i % 3 == 0) ? "Attacker" : "Defender";
            String type = (i % 4 == 0) ? "Leader" : "Balanced";
            int skill = (i % 10) + 1;

            participants.add(createParticipant(name, game, skill, role, type));
        }

        int requestedTeamSize = 4;

        ArrayList<Team> teams = builder.buildTeams(participants, requestedTeamSize, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty(), "Teams should be formed");

        int totalMembers = 0;
        for (Team t : teams) {
            List<Participant> members = t.getMembers();
            assertNotNull(members, "Team members list must not be null");

            // because ensureMinTeamSize uses minSize = 3
            assertTrue(
                    members.size() >= 3,
                    "Every team with members should have at least 3 members"
            );
            assertTrue(
                    members.size() <= requestedTeamSize || teams.size() == 1,
                    "Team size should not exceed requested size (except when everything ends up in one team)"
            );
            totalMembers += members.size();
        }

        assertEquals(
                participants.size(),
                totalMembers,
                "All participants must be assigned to exactly one team"
        );
    }

    @Test
    void buildTeams_avoidsSmallTeamsAfterBalancing() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> participants = new ArrayList<>();

        // 5 participants, teamSize = 3 will normally create 2 teams.
        // ensureMinTeamSize should merge so that no team has 1–2 members.
        for (int i = 1; i <= 5; i++) {
            participants.add(createParticipant(
                    "P" + i,
                    "Valorant",
                    5,
                    "Strategist",
                    "Balanced"
            ));
        }

        ArrayList<Team> teams = builder.buildTeams(participants, 3, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        int totalMembers = 0;
        for (Team t : teams) {
            int size = t.getMembers().size();
            assertTrue(size >= 3, "After balancing, no non-empty team should have less than 3 members");
            totalMembers += size;
        }

        assertEquals(5, totalMembers, "All 5 participants must still be in some team");
    }
}
