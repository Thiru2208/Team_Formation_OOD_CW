package teammate.model;

/**
 * Base class for all humans in the system.
 * Organizer and Participant both extend this.
 */
// Abstract base class storing common fields for both Organizer and Participant.

public abstract class Human {

    protected String name;
    protected String username;
    protected String password;

    public Human() {
    }

    public Human(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }

    // ---------- Getters / Setters ----------
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
