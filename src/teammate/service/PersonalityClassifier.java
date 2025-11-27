package teammate.service;

import teammate.model.Participant;

public class PersonalityClassifier {

    public static void classify(Participant p, int score) {

        if (score >= 80) {
            p.setPersonalityType("Leader");
        } else if (score >= 50) {
            p.setPersonalityType("Balanced");
        } else {
            p.setPersonalityType("Thinker");
        }
    }
}
