package scratch;

import java.util.ArrayList;

class Pupil {
    private String firstName;
    private String secondName;
    ArrayList<AwardedPoints> awardedPoints = new ArrayList<>();

    public Pupil(String firstName, String secondName) {
        this.firstName = firstName;
        this.secondName = secondName;
    }

    public void add(AwardedPoints newPoints) {
        awardedPoints.add(newPoints);
    }

    @Override
    public String toString() {
        return "Pupil{" +
                "firstName='" + firstName + '\'' +
                ", secondName='" + secondName + '\'' +
                ", awardedPoints=" + awardedPoints +
                '}';
    }
}
