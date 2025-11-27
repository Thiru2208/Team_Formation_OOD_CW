/*package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;

public class TeamFormationTask implements Runnable {

    private final ArrayList<Participant> participants;
    private final int teamSize;
    private final TeamBuilder builder;
    private ArrayList<Team> result;

    public TeamFormationTask(ArrayList<Participant> participants, int teamSize, TeamBuilder builder) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.builder = builder;
    }

    @Override
    /public void run() {
        System.out.println("▶ Team formation started in thread: " +
                Thread.currentThread().getName());
        result = builder.buildTeams(participants, teamSize);
        System.out.println("✔ Team formation finished in thread: " +
                Thread.currentThread().getName());
    }

    public ArrayList<Team> getResult() {
        return result;
    }
}
*/