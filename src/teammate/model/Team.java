package teammate.model;

import java.util.ArrayList;

public class Team {
    private String teamName;
    private ArrayList<Participant> members = new ArrayList<>();

    public Team(String teamName) {
        this.teamName = teamName;
    }

    public void addMember(Participant p) {
        members.add(p);
    }

    public String getTeamName() { return teamName; }
    public ArrayList<Participant> getMembers() { return members; }
}
