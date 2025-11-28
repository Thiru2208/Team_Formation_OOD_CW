package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;

public class TeamFormationTask implements Runnable {

    private final ArrayList<Participant> participants;
    private final int teamSize;
    private final TeamBuilder teamBuilder;
    private final LoggerService logger = LoggerService.getInstance();

    private ArrayList<Team> result = new ArrayList<>();

    public TeamFormationTask(ArrayList<Participant> participants,
                             int teamSize,
                             TeamBuilder teamBuilder) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.teamBuilder = teamBuilder;
    }

    @Override
    public void run() {
        try {
            logger.info("TeamFormationTask START. participants=" +
                    participants.size() + ", teamSize=" + teamSize);
            result = teamBuilder.buildTeams(participants, teamSize, logger);
            logger.info("TeamFormationTask END. teamsFormed=" + result.size());
        } catch (Exception e) {
            logger.error("TeamFormationTask FAILED", e);
            System.out.println("⚠ An error occurred while forming teams.");
        }
    }

    public ArrayList<Team> getResult() {
        return result;
    }
}
