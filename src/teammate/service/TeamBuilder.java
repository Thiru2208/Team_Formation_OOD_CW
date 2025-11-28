package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;
import teammate.service.LoggerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamBuilder {

    public ArrayList<Team> buildTeams(ArrayList<Participant> participants,
                                      int teamSize,
                                      LoggerService logger) {

        ArrayList<Team> teams = new ArrayList<>();

        if (participants == null || participants.isEmpty()) {
            logger.info("TeamBuilder: no participants available to build teams.");
            return teams;
        }

        logger.info("TeamBuilder: building teams. participants=" + participants.size() +
                ", teamSize=" + teamSize);

        // ----- how many teams? -----
        int teamCount = (int) Math.ceil((double) participants.size() / teamSize);
        for (int i = 1; i <= teamCount; i++) {
            teams.add(new Team("Team " + i));
        }

        // ----- split by personality type -----
        List<Participant> leaders = new ArrayList<>();
        List<Participant> thinkers = new ArrayList<>();
        List<Participant> balanced = new ArrayList<>();

        for (Participant p : participants) {
            String type = p.getPersonalityType();
            if (type == null) type = "";

            if (type.equalsIgnoreCase("Leader")) {
                leaders.add(p);
            } else if (type.equalsIgnoreCase("Thinker")) {
                thinkers.add(p);
            } else {
                balanced.add(p);  // includes "Balanced" or anything else
            }
        }

        // randomize each group
        Collections.shuffle(leaders);
        Collections.shuffle(thinkers);
        Collections.shuffle(balanced);

        // ----- limit leaders: max 1 per team, extras -> Balanced -----
        List<Participant> usedLeaders = new ArrayList<>();
        int maxLeaders = Math.min(leaders.size(), teamCount);   // 1 per team

        for (int i = 0; i < leaders.size(); i++) {
            Participant p = leaders.get(i);
            if (i < maxLeaders) {
                usedLeaders.add(p);
            } else {
                // convert extra leaders to Balanced
                p.setPersonalityType("Balanced");
                balanced.add(p);
            }
        }

        // ----- limit thinkers: max 2 per team, extras -> Balanced -----
        List<Participant> usedThinkers = new ArrayList<>();
        int maxThinkers = Math.min(thinkers.size(), teamCount * 2);   // 2 per team

        for (int i = 0; i < thinkers.size(); i++) {
            Participant p = thinkers.get(i);
            if (i < maxThinkers) {
                usedThinkers.add(p);
            } else {
                // convert extra thinkers to Balanced
                p.setPersonalityType("Balanced");
                balanced.add(p);
            }
        }

        Collections.shuffle(usedLeaders);
        Collections.shuffle(usedThinkers);
        Collections.shuffle(balanced);

        // ----- STEP 1: give each team 1 Leader (if available) -----
        int li = 0;
        for (Team t : teams) {
            if (li < usedLeaders.size() && t.getMembers().size() < teamSize) {
                t.addMember(usedLeaders.get(li++));
            }
        }

        // ----- STEP 2: give each team at least 1 Thinker (if available) -----
        int ti = 0;
        for (Team t : teams) {
            if (ti < usedThinkers.size() && t.getMembers().size() < teamSize) {
                t.addMember(usedThinkers.get(ti++));
            }
        }

        // ----- STEP 3: optional 2nd Thinker (max 2 per team) -----
        for (Team t : teams) {
            if (ti >= usedThinkers.size()) break;
            if (t.getMembers().size() >= teamSize) continue;

            int thinkersInTeam = countTypeInTeam(t, "Thinker");
            if (thinkersInTeam < 2) {
                t.addMember(usedThinkers.get(ti++));
            }
        }

        // ----- STEP 4: fill remaining slots with Balanced (incl. converted extras) -----
        int bi = 0;
        boolean added = true;

        while (added) {
            added = false;
            for (Team t : teams) {
                if (t.getMembers().size() >= teamSize) {
                    continue;
                }
                if (bi < balanced.size()) {
                    t.addMember(balanced.get(bi++));
                    added = true;
                }
            }
        }
        logger.info("TeamBuilder split: Leaders=" + leaders.size() +
                ", Thinkers=" + thinkers.size() +
                ", Balanced=" + balanced.size());

        System.out.println("TeamBuilder split: Leaders=" + leaders.size() +
                ", Thinkers=" + thinkers.size() +
                ", Balanced=" + balanced.size());

        logger.info("TeamBuilder: created " + teams.size() + " teams.");
        return teams;
    }

    // helper: count people of a personality type inside one team
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
}
