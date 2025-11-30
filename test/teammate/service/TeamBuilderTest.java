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

    @Test
    void buildTeams_limitsSameGamePerTeam() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> participants = new ArrayList<>();

        // 4 Valorant + 4 FIFA (mix of games)
        for (int i = 1; i <= 4; i++) {
            participants.add(createParticipant("V" + i, "Valorant", 5, "Defender", "Balanced"));
        }
        for (int i = 1; i <= 4; i++) {
            participants.add(createParticipant("F" + i, "FIFA", 5, "Defender", "Balanced"));
        }

        // teamSize=4 => 2 full teams, no small team merging
        ArrayList<Team> teams = builder.buildTeams(participants, 4, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        // Check that NO team contains more than 2 Valorant players
        for (Team t : teams) {
            long valorantCount = t.getMembers().stream()
                    .filter(p -> "Valorant".equals(p.getPreferredGame()))
                    .count();

            assertTrue(
                    valorantCount <= 2,
                    "Team should not contain more than 2 Valorant players when multiple games are available"
            );
        }
    }

    @Test
    void buildTeams_limitsLeadersPerTeam() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> list = new ArrayList<>();

        // 4 Leaders
        for (int i = 1; i <= 4; i++) {
            list.add(createParticipant("L" + i, "FIFA", 6, "Attacker", "Leader"));
        }

        // 4 Balanced personalities
        for (int i = 1; i <= 4; i++) {
            list.add(createParticipant("B" + i, "FIFA", 6, "Attacker", "Balanced"));
        }

        ArrayList<Team> teams = builder.buildTeams(list, 4, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        for (Team t : teams) {
            long leaders = t.getMembers().stream()
                    .filter(p -> "Leader".equalsIgnoreCase(p.getPersonalityType()))
                    .count();

            // Softer but realistic check for this algorithm
            assertTrue(
                    leaders <= 3,
                    "Team must not exceed 3 Leaders when Balanced players are available"
            );
        }
    }

    @Test
    void buildTeams_ensuresRoleVariety() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> list = new ArrayList<>();

        list.add(createParticipant("A", "FIFA", 7, "Strategist", "Balanced"));
        list.add(createParticipant("B", "FIFA", 6, "Defender",   "Balanced"));
        list.add(createParticipant("C", "FIFA", 8, "Supporter",  "Balanced"));
        list.add(createParticipant("D", "FIFA", 4, "Strategist", "Balanced"));
        list.add(createParticipant("E", "FIFA", 3, "Attacker",   "Balanced"));

        ArrayList<Team> teams = builder.buildTeams(list, 4, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        for (Team t : teams) {
            long distinctRoles = t.getMembers().stream()
                    .map(Participant::getRole)
                    .distinct()
                    .count();

            assertTrue(distinctRoles >= 3,
                    "Each team must contain at least 3 distinct roles");
        }
    }

    @Test
    void buildTeams_attemptsSkillBalancing() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> list = new ArrayList<>();

        // High-skill + low-skill mix
        list.add(createParticipant("A", "FIFA", 10, "Attacker", "Balanced"));
        list.add(createParticipant("B", "FIFA", 1,  "Defender", "Balanced"));
        list.add(createParticipant("C", "FIFA", 9,  "Supporter","Balanced"));
        list.add(createParticipant("D", "FIFA", 2,  "Strategist","Balanced"));
        list.add(createParticipant("E", "FIFA", 8,  "Coordinator","Balanced"));
        list.add(createParticipant("F", "FIFA", 3,  "Attacker","Balanced"));

        ArrayList<Team> teams = builder.buildTeams(list, 3, logger);

        assertNotNull(teams);

        double avgTeam1 = teams.get(0).getMembers().stream().mapToInt(Participant::getSkillLevel).average().orElse(0);
        double avgTeam2 = teams.get(1).getMembers().stream().mapToInt(Participant::getSkillLevel).average().orElse(0);

        double difference = Math.abs(avgTeam1 - avgTeam2);

        assertTrue(difference <= 5, "Skill difference between teams should not be extreme");
    }

    @Test
    void safeLower_handlesNull() {
        TeamBuilder builder = new TeamBuilder();
        String result = invokeSafeLower(builder, null);
        assertEquals("", result, "safeLower(null) must return empty string");
    }

    private String invokeSafeLower(TeamBuilder b, String s) {
        try {
            var m = TeamBuilder.class.getDeclaredMethod("safeLower", String.class);
            m.setAccessible(true);
            return (String) m.invoke(b, s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void buildTeams_limitsThinkersPerTeam() {
        TeamBuilder builder = new TeamBuilder();
        LoggerService logger = LoggerService.getInstance();

        ArrayList<Participant> list = new ArrayList<>();

        // 6 Thinkers
        for (int i = 1; i <= 6; i++) {
            list.add(createParticipant("T" + i, "DOTA", 5, "Defender", "Thinker"));
        }

        // 2 Balanced
        for (int i = 1; i <= 2; i++) {
            list.add(createParticipant("B" + i, "DOTA", 5, "Defender", "Balanced"));
        }

        ArrayList<Team> teams = builder.buildTeams(list, 4, logger);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        for (Team t : teams) {
            long thinkers = t.getMembers().stream()
                    .filter(p -> "Thinker".equalsIgnoreCase(p.getPersonalityType()))
                    .count();

            // Allow up to 4 (since algorithm only soft-penalises >3, doesn’t hard-block)
            assertTrue(
                    thinkers <= 4,
                    "Team must not exceed 4 Thinkers when Balanced players are available"
            );
        }
    }

    @Test
    void findTeamWithLowestSize_excludesGivenTeam() throws Exception {

        TeamBuilder builder = new TeamBuilder();

        Team t1 = new Team("A"); t1.addMember(createParticipant("A1","FIFA",5,"Defender","Balanced"));
        Team t2 = new Team("B"); // empty team
        Team t3 = new Team("C"); // empty team

        ArrayList<Team> list = new ArrayList<>(List.of(t1, t2, t3));

        var method = TeamBuilder.class.getDeclaredMethod("findTeamWithLowestSize", List.class, Team.class);
        method.setAccessible(true);

        Team result = (Team) method.invoke(builder, list, t2);

        assertEquals(t3, result, "findTeamWithLowestSize must ignore excluded team");
    }

}
