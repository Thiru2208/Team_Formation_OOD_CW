package teammate.service;

import org.junit.jupiter.api.*;
import teammate.model.Participant;
import teammate.model.Organizer;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    // Before each test → create clean files
    @BeforeEach
    void setup() throws Exception {
        // Clean participant and organizer files
        Files.deleteIfExists(new File("src/teammate/auth/participant_accounts.csv").toPath());
        Files.deleteIfExists(new File("src/teammate/auth/organizer_account.csv").toPath());

        // Create default organizer file
        try (PrintWriter pw = new PrintWriter("src/teammate/auth/organizer_account.csv")) {
            pw.println("username,password,name");
            pw.println("admin," +
                    Base64.getEncoder().encodeToString("abcd".getBytes()) +
                    ",MainOrganizer");
        }

        authService = new AuthService();
    }

    // ------------------------------------------------------------
    // TEST PASSWORD VALIDATION
    // ------------------------------------------------------------
    @Test
    void testPasswordValidation() throws Exception {
        var method = AuthService.class.getDeclaredMethod("isValidPassword", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(authService, "abcd"));  // valid
        assertTrue((boolean) method.invoke(authService, "A1b2"));  // valid
        assertTrue((boolean) method.invoke(authService, "abc123")); // valid (6 chars)

        assertFalse((boolean) method.invoke(authService, "abc"));      // too short
        assertFalse((boolean) method.invoke(authService, "abcd!"));    // illegal char
        assertFalse((boolean) method.invoke(authService, "abcdefg"));  // too long
    }

    // ------------------------------------------------------------
    // TEST PASSWORD ENCRYPTION / DECRYPTION
    // ------------------------------------------------------------
    @Test
    void testPasswordEncryptDecrypt() throws Exception {
        var enc = AuthService.class.getDeclaredMethod("encryptPassword", String.class);
        var dec = AuthService.class.getDeclaredMethod("decryptPassword", String.class);
        enc.setAccessible(true);
        dec.setAccessible(true);

        String raw = "abcd";
        String encrypted = (String) enc.invoke(authService, raw);
        String decrypted = (String) dec.invoke(authService, encrypted);

        assertEquals(raw, decrypted);
        assertNotEquals(raw, encrypted);  // Base64 encoded
    }

    // ------------------------------------------------------------
    // TEST PARTICIPANT SIGNUP
    // ------------------------------------------------------------
    @Test
    void testParticipantSignup() {
        Scanner sc = new Scanner("john\nabcd\nabcd\n");
        Participant p = authService.participantSignup(sc);

        assertNotNull(p);
        assertEquals("Participant_101", p.getName());
        assertEquals("user101@university.edu", p.getEmail());
        assertEquals("john", p.getUsername());
        assertEquals("abcd", p.getPassword());
    }

    // ------------------------------------------------------------
    // TEST PARTICIPANT LOGIN
    // ------------------------------------------------------------
    @Test
    void testParticipantLoginSuccess() {
        Scanner sc = new Scanner("john\nabcd\nabcd\n"); // signup input
        authService.participantSignup(sc);

        Scanner login = new Scanner("john\nabcd\n");
        Participant logged = authService.participantLogin(login);

        assertNotNull(logged);
        assertEquals("john", logged.getUsername());
    }

    @Test
    void testParticipantLoginFail() {
        Scanner sc = new Scanner("john\nabcd\nabcd\n");
        authService.participantSignup(sc);

        Scanner wrong = new Scanner("john\nwrongpass\n");
        Participant logged = authService.participantLogin(wrong);

        assertNull(logged);
    }

    // ------------------------------------------------------------
    // TEST SAVE ALL ACCOUNTS TO FILE
    // ------------------------------------------------------------
    @Test
    void testSaveAllAccounts() throws Exception {
        // signup
        Scanner sc = new Scanner("john\nabcd\nabcd\n");
        authService.participantSignup(sc);

        // save to file
        authService.saveAllAccountsToFile();

        File f = new File("src/teammate/auth/participant_accounts.csv");
        assertTrue(f.exists());

        String txt = Files.readString(f.toPath());
        assertTrue(txt.contains("john"));
        assertTrue(txt.contains("Participant_101"));
    }

    // ------------------------------------------------------------
    // TEST ORGANIZER LOGIN
    // ------------------------------------------------------------
    @Test
    void testOrganizerLoginSuccess() {
        Scanner sc = new Scanner("admin\nabcd\n");
        assertTrue(authService.organizerLogin(sc));
    }

    @Test
    void testOrganizerLoginFail() {
        Scanner sc = new Scanner("admin\nwrong\n");
        assertFalse(authService.organizerLogin(sc));
    }
}
