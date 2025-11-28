package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;

public class TeamFormationTask implements Runnable {

    private final ArrayList<Participant> participants;
    private final int teamSize;
    private final TeamBuilder teamBuilder;
    private final LoggerService logger = LoggerService.getInstance();

    // new: which mode to use (true = strict, false = smart)
    private final boolean strictMode;

    private ArrayList<Team> result = new ArrayList<>();

    public TeamFormationTask(ArrayList<Participant> participants,
                             int teamSize,
                             TeamBuilder teamBuilder,
                             boolean strictMode) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.teamBuilder = teamBuilder;
        this.strictMode = strictMode;
    }

    @Override
    public void run() {
        try {
            System.out.println("[Thread] Team formation thread running: " + Thread.currentThread().getName());
            logger.info("TeamFormationTask START. participants=" +
                    participants.size() + ", teamSize=" + teamSize +
                    ", strictMode=" + strictMode);

            if (strictMode) {
                // Mode 1: strict equal teams
                result = teamBuilder.buildTeamsStrictEqual(participants, teamSize, logger);
            } else {
                // Mode 2: intelligent balanced teams
                result = teamBuilder.buildTeams(participants, teamSize, logger);
            }

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
