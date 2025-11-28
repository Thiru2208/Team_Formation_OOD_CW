package teammate.model;

/**
 * Organizer is a type of Human.
 * Right now it only uses the common fields from Human.
 * Later you can add organizer-specific fields (e.g. staffId, role, etc.).
 */
public class Organizer extends Human {

    public Organizer(String name, String username, String password) {
        super(name, username, password);
    }
}
