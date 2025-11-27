package teammate.service;

public class ValidateInput {

    public static boolean isNumeric(String input) {
        if (input == null || input.isEmpty()) return false;
        return input.matches("\\d+");
    }

    // NEW: validate team size with min 3
    public static boolean isValidTeamSize(int teamSize, int totalParticipants) {
        if (teamSize < 3) {
            return false; // need at least 3 (1 Leader + 2 Thinkers)
        }
        if (teamSize > totalParticipants) {
            return false;
        }
        return true;
    }
}
