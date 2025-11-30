package teammate.service;

import org.junit.jupiter.api.*;
import teammate.model.Participant;
import teammate.model.Organizer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    // only use a test file for participants
    private static final String ACCOUNTS_FILE =
            "test/teammate/auth/test_participant_accounts.csv";

    @BeforeEach
    void setup() throws Exception {
        // 1) delete any previous test participant file
        Path accPath = Paths.get(ACCOUNTS_FILE);
        Files.deleteIfExists(accPath);

        // 2) create service (this will load REAL csv)
        authService = new AuthService();

        // 3) now override all static in-memory state so tests are clean and predictable

        // clear participant maps
        clearStaticMap("participantCredentials");
        clearStaticMap("participantProfiles");
        clearStaticMap("participantIds");

        // reset nextGeneratedNumericId = 101
        Field idField = AuthService.class.getDeclaredField("nextGeneratedNumericId");
        idField.setAccessible(true);
        idField.setInt(null, 101);

        // set a known organizer account: admin / abcd
        Field orgField = AuthService.class.getDeclaredField("organizerAccount");
        orgField.setAccessible(true);
        Organizer testOrg = new Organizer("MainOrganizer", "admin", "abcd");
        orgField.set(null, testOrg);
    }

    // helper to clear private static final maps
    @SuppressWarnings("unchecked")
    private void clearStaticMap(String fieldName) throws Exception {
        Field f = AuthService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) f.get(null); // static → null
        map.clear();
    }

    // ------------------------------------------------------------
    // TEST PASSWORD VALIDATION
    // ------------------------------------------------------------
    @Test
    void testPasswordValidation() throws Exception {
        var method = AuthService.class.getDeclaredMethod("isValidPassword", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(authService, "abcd"));     // valid
        assertTrue((boolean) method.invoke(authService, "A1b2"));     // valid
        assertTrue((boolean) method.invoke(authService, "abc123"));   // valid (6 chars)

        assertFalse((boolean) method.invoke(authService, "abc"));     // too short
        assertFalse((boolean) method.invoke(authService, "abcd!"));   // illegal char
        assertFalse((boolean) method.invoke(authService, "abcdefg")); // too long
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
        // with clean static state, "john" is not taken and first ID = 101
        Scanner sc = new Scanner("john\nabcd\nabcd\n");
        Participant p = authService.participantSignup(sc, ACCOUNTS_FILE);

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
        authService.participantSignup(sc, ACCOUNTS_FILE);

        Scanner login = new Scanner("john\nabcd\n");
        Participant logged = authService.participantLogin(login);

        assertNotNull(logged);
        assertEquals("john", logged.getUsername());
    }

    @Test
    void testParticipantLoginFail() {
        Scanner sc = new Scanner("john\nabcd\nabcd\n");
        authService.participantSignup(sc, ACCOUNTS_FILE);

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
        authService.participantSignup(sc, ACCOUNTS_FILE);

        // save to file
        authService.saveAllAccountsToFile(ACCOUNTS_FILE);

        File f = new File(ACCOUNTS_FILE);
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
        // organizerAccount was set to admin/abcd in @BeforeEach
        Scanner sc = new Scanner("admin\nabcd\n");
        assertTrue(authService.organizerLogin(sc));
    }

    @Test
    void testOrganizerLoginFail() {
        Scanner sc = new Scanner("admin\nwrong\n");
        assertFalse(authService.organizerLogin(sc));
    }
}
