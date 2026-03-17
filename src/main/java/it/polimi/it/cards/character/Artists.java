package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

 public class Artists extends Character {
    public int cavePaintingScores;
    public int endGameSetBonus;

    public Artists(String name, Era era, CharacterType type, int icon, int cavePaintingScores, int endGameSetBonus) {
        super(name, era, type);
        this.cavePaintingScores = cavePaintingScores;
        this.endGameSetBonus = endGameSetBonus;
    }

    @Override
    public void applyEffect(Player player) {
        Tribe tribe = player.getTribe();
        tribe.addCavePaintingScore(this.cavePaintingScores);
        tribe.addEndGameSetBonus(this.endGameSetBonus);
    }

    @Override
    public CharacterType getTypeOfCharacter() {
        return CharacterType.ARTIST;

    }

    @Override
    public int getIcon() {
        return this.getIcon();
    }

    public void setCavePaintingScores(int cavePaintingScores) {
        this.cavePaintingScores = cavePaintingScores;
    }

     public int getEndGameSetBonus() {
        this.endGameSetBonus=endGameSetBonus;
        return endGameSetBonus;
     }
 }