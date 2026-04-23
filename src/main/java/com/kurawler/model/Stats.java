public class Stats {
    private int hp;
    private int energy;
    private final int MAX_HP = 100;
    private final int MAX_ENERGY = 100;

    public Stats() {
        this.hp = 17;     
        this.energy = MAX_ENERGY;
    }

    // Energy decreasing by walking
    public void drainEnergy(int amount) {
        this.energy = Math.max(0, this.energy - amount);
    }

    // HP increasing by consuming potion
    public void heal(int amount) {
        this.hp = Math.min(MAX_HP, this.hp + amount);
    }

    public int getHp() { return hp; }
    public int getEnergy() { return energy; }
    
    public String getStatusString() {
        return "HP: " + hp + " | Energy: " + energy;
    }

}
