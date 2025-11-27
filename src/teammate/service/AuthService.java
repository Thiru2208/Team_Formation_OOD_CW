package teammate.service;

import teammate.model.Participant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthService {

    // one organizer only (hard-coded for coursework)
    private static final String ORG_USERNAME = "organizer";
    private static final String ORG_PASSWORD = "org123";

    // all participant accounts (username -> Participant object)
    private Map<String, Participant> participantAccounts = new HashMap<>();

    public boolean organizerLogin(Scanner sc) {
        System.out.print("Organizer username: ");
        String u = sc.nextLine().trim();
        System.out.print("Organizer password: ");
        String p = sc.nextLine().trim();

        if (ORG_USERNAME.equals(u) && ORG_PASSWORD.equals(p)) {
            System.out.println("✅ Organizer login successful.\n");
            return true;
        }
        System.out.println("❌ Invalid organizer credentials.\n");
        return false;
    }

    // Participant sign-up: create username/password for an existing or new participant
    public Participant participantSignup(Scanner sc) {
        System.out.println("\n--- Participant Sign Up ---");
        System.out.print("Choose username: ");
        String username = sc.nextLine().trim();

        if (participantAccounts.containsKey(username)) {
            System.out.println("❌ Username already taken.");
            return null;
        }

        System.out.print("Choose password: ");
        String password = sc.nextLine().trim();

        System.out.print("Enter display name: ");
        String name = sc.nextLine().trim();

        // simple dummy email – you can change rule
        String email = username + "@gamehub.com";

        System.out.println("Account created for " + name + " (" + username + ")");

        Participant p = new Participant(name, email, null, 0, null);
        p.setUsername(username);
        p.setPassword(password);

        participantAccounts.put(username, p);
        return p;
    }

    public Participant participantLogin(Scanner sc) {
        System.out.println("\n--- Participant Login ---");
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        Participant p = participantAccounts.get(username);
        if (p != null && password.equals(p.getPassword())) {
            System.out.println("✅ Login success. Welcome " + p.getName() + "!\n");
            return p;
        }
        System.out.println("❌ Invalid username or password.\n");
        return null;
    }

    public Map<String, Participant> getParticipantAccounts() {
        return participantAccounts;
    }
}
