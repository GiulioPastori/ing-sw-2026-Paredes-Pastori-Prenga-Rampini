package it.polimi.it.cards.events;


import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import java.util.List;

public class CavePaintings implements EventStrategy {

    private final int threshold;        // Numero minimo di artisti richiesto
    private final int penalty;          // PP persi se sotto soglia
    private final int rewardPerArtist;  // PP guadagnati per ogni artista se sopra soglia

    /**
     * Costruttore basato sulla carta Evento di Mesos.
     * @param threshold Il numero minimo di artisti per non subire la penalità.
     * @param penalty I punti da sottrarre se non si raggiunge la soglia.
     * @param rewardPerArtist I punti da assegnare per OGNI artista se si è raggiunta la soglia.
     */
    public CavePaintings(int threshold, int penalty, int rewardPerArtist) {
        this.threshold = threshold;
        this.penalty = penalty;
        this.rewardPerArtist = rewardPerArtist;
    }

    @Override
    public void apply(List<Player> players) {
        // L'evento viene risolto per ogni giocatore nella lista
        for (Player p : players) {
            applyEffect(p);
        }
    }

    @Override
    public void applyEffect(Player player) {
        // 1. Contiamo quanti Artisti ha il giocatore nella sua tribù
        int artistCount = player.getTribe().countType(CharacterType.ARTIST);

        // 2. Controllo della soglia indicata sulla carta
        if (artistCount < threshold) {
            // CASO A: Sotto la soglia
            // Esempio: "se avete zero o un Artista perdete 3 PP"
            player.addPrestigePoint(-penalty);

        } else {
            // CASO B: Raggiunta o superata la soglia
            int totalBonus = artistCount * rewardPerArtist;
            player.addPrestigePoint(totalBonus);

        }
    }
}