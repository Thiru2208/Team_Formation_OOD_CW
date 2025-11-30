package teammate.app;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void askTeamSize_acceptsValidValueFirstTime() {
        // user types: 5 <Enter>
        String userInput = "5\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(5, result);
    }

    @Test
    void askTeamSize_repromptsUntilValidNumber() {
        // user types: "abc" (invalid) → "20" (out of range) → "4" (valid)
        String userInput = "abc\n20\n4\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        // Expected: keeps asking until "4" and returns 4
        assertEquals(4, result);
    }

    @Test
    void askTeamSize_respectsLowerBound() {
        // user types: "2" (below 3) → "3" (valid min)
        String userInput = "2\n3\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(3, result);  // min allowed
    }

    @Test
    void askTeamSize_respectsUpperBound() {
        // user types: "16" (> 15) → "10" (valid)
        String userInput = "16\n10\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(10, result);
    }

    @Test
    void askTeamSize_acceptsExactlyMinimumAllowed() {
        // user types: "3" (exact lower bound)
        String userInput = "3\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(3, result, "Should accept team size at the minimum bound (3)");
    }

    @Test
    void askTeamSize_acceptsExactlyMaximumAllowed() {
        // user types: "15" (exact upper bound)
        String userInput = "15\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(15, result, "Should accept team size at the maximum bound (15)");
    }

    @Test
    void askTeamSize_handlesEmptyInputThenValid() {
        // user types: "" (empty) → "7" (valid)
        String userInput = "\n7\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(7, result, "Should ignore empty input and then accept valid size");
    }

    @Test
    void askTeamSize_handlesMultipleSpacesThenValid() {
        // user types: "   " → "   10   " → valid (trim should work)
        String userInput = "   \n   10   \n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(10, result, "Should trim spaces and accept value");
    }

    @Test
    void askTeamSize_rejectsNegativeNumbers() {
        // user types: -5 → 6
        String userInput = "-5\n6\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(6, result, "Negative numbers must be rejected");
    }

    @Test
    void askTeamSize_rejectsZero() {
        // user types: 0 → 5
        String userInput = "0\n5\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(5, result, "Zero must not be accepted");
    }

    @Test
    void askTeamSize_rejectsFloatingPointValue() {
        // user types "4.5" → then "6"
        String userInput = "4.5\n6\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(6, result, "Floating-point inputs should be rejected");
    }

    @Test
    void askTeamSize_rejectsSpecialCharacters() {
        // user types: "@$%" → then "9"
        String userInput = "@$%\n9\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(9, result, "Special characters should be rejected");
    }

    @Test
    void askTeamSize_continuesAfterLargeNumber() {
        // user types: 99999 → then 8
        String userInput = "99999\n8\n";
        Scanner sc = new Scanner(
                new ByteArrayInputStream(userInput.getBytes(StandardCharsets.UTF_8))
        );

        int result = Main.askTeamSize(sc, 15);

        assertEquals(8, result, "Numbers above max must be rejected");
    }
}
