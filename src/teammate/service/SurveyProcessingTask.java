package teammate.service;

import teammate.model.Participant;

import java.util.Scanner;

/**
 * Background task: "processing survey data" using a separate thread.
 * You can imagine this as validating / analysing survey results.
 */
public class SurveyProcessingTask implements Runnable {

    private final ParticipantSurveyService surveyService;
    private final Scanner scanner;
    private final Participant participant;
    private final AuthService authService;
    private final LoggerService logger = LoggerService.getInstance();

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
            logger.info("SurveyProcessingTask START for participant=" + participant.getName());
            surveyService.runSurveyForExistingParticipant(scanner, participant, authService);
            logger.info("SurveyProcessingTask END for participant=" + participant.getName());
        } catch (Exception e) {
            logger.error("SurveyProcessingTask FAILED for participant=" + participant.getName(), e);
            System.out.println("An error occurred while processing the survey. Please try again.");
        }
    }
}
