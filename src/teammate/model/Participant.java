package teammate.model;

public class Participant {
    private String id;
    private String name;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private String role;
    private String personalityType;
    private int personalityScore;
    private String username;
    private String password;

    public Participant(String name, String email, String preferredGame, int skillLevel, String role) {
        this.name = name;
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.role = role;
    }

    // getters/setters ...

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPreferredGame() { return preferredGame; }
    public int getSkillLevel() { return skillLevel; }
    public String getRole() { return role; }

    public String getPersonalityType() { return personalityType; }
    public void setPersonalityType(String personalityType) {
        this.personalityType = personalityType;
    }

    public int getPersonalityScore() { return personalityScore; }
    public void setPersonalityScore(int personalityScore) {
        this.personalityScore = personalityScore;
    }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPreferredGame(String preferredGame) { this.preferredGame = preferredGame; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public void setRole(String role) { this.role = role; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
