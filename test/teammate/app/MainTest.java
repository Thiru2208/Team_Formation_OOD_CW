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
}
