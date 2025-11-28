package teammate.model;

public class Participant extends Human {
    private String id;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private String role;
    private String personalityType;
    private int personalityScore;

    public Participant(String name, String email,
                       String preferredGame, int skillLevel, String role) {
        // username/password are handled by AuthService; not needed here
        super(name, null, null);
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.role = role;
    }
    // getters/setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}

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

    public void setEmail(String email) { this.email = email; }
    public void setPreferredGame(String preferredGame) { this.preferredGame = preferredGame; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public void setRole(String role) { this.role = role; }

}
