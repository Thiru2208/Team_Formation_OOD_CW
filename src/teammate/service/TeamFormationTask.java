package teammate.service;

import teammate.model.Participant;
import teammate.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Background task to form teams in a separate thread.
 * Useful when datasets are large.
 */
public class TeamFormationTask implements Callable<ArrayList<Team>> {

    private final ArrayList<Participant> participants;
    private final int teamSize;
    private final TeamBuilder teamBuilder;
    private final LoggerService logger;

    public TeamFormationTask(List<Participant> participants,
                             int teamSize,
                             TeamBuilder teamBuilder,
                             LoggerService logger) {
        // copy into new list to avoid concurrent modification
        this.participants = new ArrayList<>(participants);
        this.teamSize = teamSize;
        this.teamBuilder = teamBuilder;
        this.logger = logger;
    }

    @Override
    public ArrayList<Team> call() {
        logger.info("TeamFormationTask started. Participants = "
                + participants.size() + ", teamSize = " + teamSize);
        ArrayList<Team> formed = teamBuilder.buildTeams(participants, teamSize, logger);
        logger.info("TeamFormationTask finished. Teams formed = " + formed.size());
        return formed;
    }
}
