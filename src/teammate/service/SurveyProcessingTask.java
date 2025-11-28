package teammate.service;

import teammate.model.Participant;
import teammate.service.LoggerService;

import java.util.List;

/**
 * Background task: "processing survey data" using a separate thread.
 * You can imagine this as validating / analysing survey results.
 */
public class SurveyProcessingTask implements Runnable {

    private final List<Participant> participants;
    private final LoggerService logger;

    public SurveyProcessingTask(List<Participant> participants, LoggerService logger) {
        this.participants = participants;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            logger.info("SurveyProcessingTask started for " + participants.size() + " participant(s)");

            for (Participant p : participants) {
                // Example extra checks / processing
                if (p.getPersonalityType() == null || p.getPersonalityType().trim().isEmpty()) {
                    p.setPersonalityType("Balanced");
                }
                if (p.getPersonalityScore() < 0) {
                    logger.error("Negative personality score detected for: " + p.getName());
                    p.setPersonalityScore(0);
                }
            }

            logger.info("SurveyProcessingTask finished successfully.");
        } catch (Exception ex) {
            logger.error("Error inside SurveyProcessingTask: " + ex.getMessage());
        }
    }
}
