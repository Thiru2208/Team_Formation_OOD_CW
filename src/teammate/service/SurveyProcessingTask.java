package teammate.service;

import teammate.model.Participant;

import java.util.Scanner;

/**
 * Background task: "processing survey data" using a separate thread.
 * You can imagine this as validating / analysing survey results.
 */
// Runs the participant survey in a background thread and saves the updated survey data.

public class SurveyProcessingTask implements Runnable {

    private final ParticipantSurveyService surveyService;
    private final Scanner scanner;
    private final Participant participant;
    private final AuthService authService;
    private final LoggerService logger = LoggerService.getInstance();
    private static final String ACCOUNTS_FILE =
            "src/teammate/auth/participant_accounts.csv";

    public SurveyProcessingTask(ParticipantSurveyService surveyService,
                                Scanner scanner,
                                Participant participant,
                                AuthService authService) {
        this.surveyService = surveyService;
        this.scanner = scanner;
        this.participant = participant;
        this.authService = authService;
    }

    @Override
    public void run() {
        try {
            System.out.println();
            System.out.println("--- Adding " + participant.getName() + " ---");
            System.out.println("[Thread] Survey thread running: " + Thread.currentThread().getName());

            // [SURV 1.4.1] SurveyProcessingTask.run() begins for selected participant
            logger.info("SurveyProcessingTask START for participant=" + participant.getName());
            // [SURV 1.4.2] runSurveyForExistingParticipant(scanner, participant, authService, ACCOUNTS_FILE)
            surveyService.runSurveyForExistingParticipant(scanner, participant, authService, ACCOUNTS_FILE);
            // [SURV 3.1.1] log "SurveyProcessingTask END for participant"
            logger.info("SurveyProcessingTask END for participant=" + participant.getName());
            // [SURV 3.1.2] return from thread
        } catch (Exception e) {  // [SURV 3.2]
            // [SURV 3.2.1] catch any unhandled exception from survey
            logger.error("SurveyProcessingTask FAILED for participant=" + participant.getName(), e);
            // [SURV 3.2.2] Display("Error while processing survey")
            System.out.println("An error occurred while processing the survey. Please try again.");
            // [SURV 3.2.3] return with error
        }
    }
}
