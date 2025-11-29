package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeamBuilder {

    // === CONFIGURABLE CONSTRAINTS for SMART MODE ===
    private static final int MAX_PER_GAME_PER_TEAM   = 2;  // cap: same game per team
    private static final int MIN_DISTINCT_ROLES      = 3;  // at least 3 different roles per team
    private static final int MAX_LEADERS_PER_TEAM    = 2;  // soft max
    private static final int MAX_THINKERS_PER_TEAM   = 3;  // soft max

    // ================== MODE: SMART / BALANCED TEAMS ==================

    public ArrayList<Team> buildTeams(ArrayList<Participant> participants,
                                      int teamSize,
                                      LoggerService logger) {

        ArrayList<Team> teams = new ArrayList<>();

        if (participants == null || participants.isEmpty()) {
            logger.info("TeamBuilder: no participants available to build teams.");
            System.out.println("TeamBuilder: no participants available to build teams.");
            return teams;
        }

        logger.info("TeamBuilder: building teams. participants=" + participants.size()
                + ", teamSize=" + teamSize);
        System.out.println("TeamBuilder: building teams. participants=" + participants.size() + ", teamSize=" + teamSize);

        // ----- how many teams? -----
        int teamCount = (int) Math.ceil((double) participants.size() / teamSize);
        for (int i = 1; i <= teamCount; i++) {
            teams.add(new Team("Team " + i));
        }

        // ----- prepare participants list -----
        Collections.shuffle(participants);
        participants.sort((a, b) -> Integer.compare(b.getSkillLevel(), a.getSkillLevel()));

        // global average skill
        int totalSkill = 0;
        for (Participant p : participants) {
            totalSkill += p.getSkillLevel();
        }
        double globalAvgSkill = (participants.isEmpty()) ? 0.0 :
                (double) totalSkill / participants.size();

        logger.info("TeamBuilder: global average skill=" + globalAvgSkill);
        System.out.println("TeamBuilder: global average skill=" + globalAvgSkill);

        // ----- assign each participant to best team (heuristic scoring) -----
        for (Participant p : participants) {

            Team bestTeam = null;
            int bestScore = Integer.MIN_VALUE;

            List<Team> shuffledTeams = new ArrayList<>(teams);
            Collections.shuffle(shuffledTeams);

            for (Team t : shuffledTeams) {
                if (t.getMembers().size() >= teamSize) {
                    continue; // team already full
                }

                int score = evaluatePlacementScore(t, p, globalAvgSkill);

                if (score > bestScore) {
                    bestScore = score;
                    bestTeam = t;
                }
            }

            if (bestTeam == null) {
                bestTeam = findSmallestTeam(teams, teamSize);
            }

            bestTeam.addMember(p);
        }

        // EXTRA STEP: ensure all teams have at least 3 members
        ensureMinTeamSize(teams, 3, logger);

        // ----- logging summary -----
        logger.info("TeamBuilder: created " + teams.size() + " teams.");
        System.out.println("TeamBuilder: created " + teams.size() + " teams.");

        for (Team t : teams) {
            int size = t.getMembers().size();
            int leaders = countTypeInTeam(t, "Leader");
            int thinkers = countTypeInTeam(t, "Thinker");
            int balanced = size - leaders - thinkers;

            Set<String> games = distinctGamesInTeam(t);
            Set<String> roles = distinctRolesInTeam(t);

            double avgSkill = averageSkill(t);

            logger.info("Team summary: " + t.getTeamName()
                    + " | size=" + size
                    + " | avgSkill=" + avgSkill
                    + " | games=" + games.size()
                    + " | roles=" + roles.size()
                    + " | leaders=" + leaders
                    + " | thinkers=" + thinkers
                    + " | balanced=" + balanced);

            System.out.println("Team summary: " + t.getTeamName()
                    + " | size=" + size
                    + " | avgSkill=" + avgSkill
                    + " | games=" + games.size()
                    + " | roles=" + roles.size()
                    + " | leaders=" + leaders
                    + " | thinkers=" + thinkers
                    + " | balanced=" + balanced);
        }

        return teams;
    }

    // ==============================================================
    //                SCORING HEURISTIC FOR PLACEMENT
    // ==============================================================

    private int evaluatePlacementScore(Team t, Participant p, double globalAvgSkill) {

        int score = 0;

        // ---------- 1. Game diversity ----------
        String game = safeLower(p.getPreferredGame());
        int sameGameCount = countGameInTeam(t, game);

        if (sameGameCount >= MAX_PER_GAME_PER_TEAM) {
            score -= 1000;
        } else {
            score += (MAX_PER_GAME_PER_TEAM - sameGameCount) * 5;
        }

        // ---------- 2. Role variety ----------
        String role = safeLower(p.getRole());
        Set<String> currentRoles = distinctRolesInTeam(t);
        boolean roleAlreadyExists = currentRoles.contains(role);

        int effectiveMinRoles = Math.min(MIN_DISTINCT_ROLES, t.getMembers().size() + 1);

        if (!roleAlreadyExists && currentRoles.size() < effectiveMinRoles) {
            score += 15;
        } else if (!roleAlreadyExists) {
            score += 5;
        } else {
            score += 1;
        }

        // ---------- 3. Personality mix ----------
        String type = p.getPersonalityType() == null ? "" : p.getPersonalityType();
        int leaders = countTypeInTeam(t, "Leader");
        int thinkers = countTypeInTeam(t, "Thinker");

        if (type.equalsIgnoreCase("Leader")) {
            if (leaders >= MAX_LEADERS_PER_TEAM) {
                score -= 400;
            } else {
                score += (MAX_LEADERS_PER_TEAM - leaders) * 10;
            }
        } else if (type.equalsIgnoreCase("Thinker")) {
            if (thinkers >= MAX_THINKERS_PER_TEAM) {
                score -= 250;
            } else {
                score += (MAX_THINKERS_PER_TEAM - thinkers) * 6;
            }
        } else {
            score += 4; // Balanced
        }

        // ---------- 4. Skill balancing ----------
        double teamSkillSum = 0;
        for (Participant member : t.getMembers()) {
            teamSkillSum += member.getSkillLevel();
        }
        int futureSize = t.getMembers().size() + 1;
        double newAvg = (futureSize == 0) ? globalAvgSkill
                : (teamSkillSum + p.getSkillLevel()) / futureSize;

        double diff = Math.abs(globalAvgSkill - newAvg);
        score -= (int) (diff * 2);

        return score;
    }

    // ==============================================================
    //                    HELPER METHODS
    // ==============================================================

    private Team findSmallestTeam(List<Team> teams, int teamSize) {
        Team best = null;
        int minSize = Integer.MAX_VALUE;
        for (Team t : teams) {
            int size = t.getMembers().size();
            if (size < teamSize && size < minSize) {
                minSize = size;
                best = t;
            }
        }
        if (best == null && !teams.isEmpty()) {
            best = teams.get(0);
        }
        return best;
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.trim().toLowerCase();
    }

    private int countGameInTeam(Team team, String gameLower) {
        if (gameLower == null) return 0;
        int count = 0;
        for (Participant p : team.getMembers()) {
            if (p.getPreferredGame() != null &&
                    p.getPreferredGame().trim().equalsIgnoreCase(gameLower)) {
                count++;
            }
        }
        return count;
    }

    private Set<String> distinctGamesInTeam(Team team) {
        Set<String> games = new HashSet<>();
        for (Participant p : team.getMembers()) {
            if (p.getPreferredGame() != null && !p.getPreferredGame().trim().isEmpty()) {
                games.add(safeLower(p.getPreferredGame()));
            }
        }
        return games;
    }

    private Set<String> distinctRolesInTeam(Team team) {
        Set<String> roles = new HashSet<>();
        for (Participant p : team.getMembers()) {
            if (p.getRole() != null && !p.getRole().trim().isEmpty()) {
                roles.add(safeLower(p.getRole()));
            }
        }
        return roles;
    }

    private int countTypeInTeam(Team team, String type) {
        int count = 0;
        for (Participant p : team.getMembers()) {
            if (p.getPersonalityType() != null &&
                    p.getPersonalityType().equalsIgnoreCase(type)) {
                count++;
            }
        }
        return count;
    }

    private double averageSkill(Team t) {
        if (t.getMembers().isEmpty()) return 0.0;
        int sum = 0;
        for (Participant p : t.getMembers()) {
            sum += p.getSkillLevel();
        }
        return (double) sum / t.getMembers().size();
    }

    // ✅ NEW: ensure all teams have at least minSize members
    private void ensureMinTeamSize(ArrayList<Team> teams, int minSize, LoggerService logger) {
        List<Team> smallTeams = new ArrayList<>();

        // 1) identify small teams
        for (Team t : teams) {
            if (t.getMembers().size() > 0 && t.getMembers().size() < minSize) {
                smallTeams.add(t);
            }
        }

        if (smallTeams.isEmpty()) {
            logger.info("TeamBuilder: no small teams to fix (minSize=" + minSize + ").");
            System.out.println("TeamBuilder: no small teams to fix (minSize=" + minSize + ").");
            return; // already ok
        }

        // 🔍 log which teams are small
        for (Team st : smallTeams) {
            logger.info("Small team detected: " + st.getTeamName()
                    + " | size=" + st.getMembers().size());
            System.out.println("Small team detected: " + st.getTeamName()
                    + " | size=" + st.getMembers().size());
        }

        logger.info("TeamBuilder: fixing small teams (<" + minSize + "). Small teams=" + smallTeams.size());
        System.out.println("TeamBuilder: fixing small teams (<" + minSize + "). Small teams=" + smallTeams.size());

        // 2) redistribute members from small teams into other teams
        for (Team small : smallTeams) {
            logger.info("Redistributing members from small team: " + small.getTeamName());
            System.out.println("Redistributing members from small team: " + small.getTeamName());

            List<Participant> toMove = new ArrayList<>(small.getMembers());
            small.getMembers().clear(); // clear members in this small team

            for (Participant p : toMove) {
                // here exclude = 'small'
                Team target = findTeamWithLowestSize(teams, small);

                if (target != null) {
                    logger.info("Moving participant " + p.getName()
                            + " from " + small.getTeamName()
                            + " to " + target.getTeamName());
                    System.out.println("Moving participant " + p.getName()
                            + " from " + small.getTeamName()
                            + " to " + target.getTeamName());
                    target.addMember(p);
                } else {
                    logger.error("No target team found for " + p.getName()
                            + " from " + small.getTeamName());
                    System.out.println("No target team found for " + p.getName()
                            + " from " + small.getTeamName());
                }
            }
        }

        // 3) remove teams that ended up empty
        teams.removeIf(t -> t.getMembers().isEmpty());
        logger.info("TeamBuilder: after fixing, totalTeams=" + teams.size());
        System.out.println("TeamBuilder: after fixing, totalTeams=" + teams.size());
    }

    // find team (excluding `exclude`) with the smallest size
    private Team findTeamWithLowestSize(List<Team> teams, Team exclude) {
        Team best = null;
        int minSize = Integer.MAX_VALUE;
        for (Team t : teams) {
            if (t == exclude) {
                System.out.println("TeamBuilder: found excluding team " + t.getTeamName());
                LoggerService.getInstance().info("Excluding team from candidate list: " + t.getTeamName());
                continue;}
            int size = t.getMembers().size();
            if (size < minSize) {
                minSize = size;
                best = t;
            }
        }
        return best;
    }
}
