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
            System.out.println("[Thread] Team formation thread running: " + Thread.currentThread().getName());
            // [FORM 1.2.6] TeamFormationTask.run() starts in ExecutorService thread
            logger.info("TeamFormationTask START. participants=" +
                    participants.size() + ", teamSize=" + teamSize);
            // [FORM 2.1] Call TeamBuilder.buildTeams(participants, teamSize, logger)
            result = teamBuilder.buildTeams(participants, teamSize, logger);
            // [FORM 2.3] Log number of teams formed
            logger.info("TeamFormationTask END. teamsFormed=" + result.size());
        } catch (Exception e) {
            // [FORM 3.1] Handle any unexpected error during team formation
            logger.error("TeamFormationTask FAILED", e);
            System.out.println("⚠ An error occurred while forming teams.");
        }
    }

    public ArrayList<Team> getResult() {
        return result;
    }
}
