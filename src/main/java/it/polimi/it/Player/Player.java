package it.polimi.it.Player;

private String name;
private int food;
private int prestigePoints;
private Tribe tribe;

public void addFood(int amount) {
    food += amount;
}

public void spendFood(int amount) {
    if(food - amount >= 0) {
        food -= amount;
    }
    else food = 0;
}

public void addPrestige(int points) {
    prestigePoints += points;
}

public Tribe getTribe() {
    return tribe;
}

public int getFood() {
    return food;
}
}