package Project.common;

public class RollPayload extends Payload {
    private int sides;
    private int rolls;

    public int getSides() {
        return sides;
    }

    public void setSides(int sides) {
        this.sides = sides;
    }

    public int getRolls() {
        return rolls;
    }

    public void setRolls(int rolls) {
        this.rolls = rolls;
    }

    @Override
    public String toString() {
        return String.format("RollPayload[sides=%d, rolls=%d]", sides, rolls);
    }
}
