package it.polimi.it.cards.building;



import it.polimi.it.Player.Player;
import it.polimi.it.cards.events.ShamanicRitual;

public class ExtraShamanIconsStrategy implements BuildingStrategy {
    private final int icons;

    public ExtraShamanIconsStrategy(int icons) { this.icons = icons; }


    @Override
    public void onRitual(Player p, ShamanicRitual e) { /* Aggiunge icone virtuali */ }

    public int getExtraIcons() {
        return icons;
    }

}


