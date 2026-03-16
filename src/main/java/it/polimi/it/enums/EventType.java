package it.polimi.it.enums;

import it.polimi.mesos.model.Player;
import it.polimi.mesos.model.Tribe;

public enum EventType {

    HUNT {

        @Override
        public void apply(Player player, int valueEra) {

            /*    Evento caccia.  Il giocatore ottiene cibo in base al valore dell'era*/

            player.addFood(valueEra);

        }
    },

    SUSTENANCE {

        @Override
        public void apply(Player player, int valueEra) {

            /* Evento sostentamento.  Il giocatore deve pagare cibo per mantenere la tribù.    */

            player.addFood(-valueEra);

        }
    },

    SHAMANIC_RITUAL {

        @Override
        public void apply(Player player, int valueEra) {

            /*Evento rituale sciamanico. Il giocatore guadagna punti prestigio. */

            player.addPrestigePoints(valueEra);

        }
    },

    CAVE_PAINTINGS {

        @Override
        public void apply(Player player, int valueEra) {

            /*  Evento pitture rupestri.I punti dipendono dagli artisti nella tribù.   */

            Tribe tribe = player.getTribe();

            int artists = tribe.countCharacterType(CharacterType.ARTIST);

            player.addPrestigePoints(artists * valueEra);

        }
    };

    /**
     * Metodo astratto che ogni tipo di evento deve implementare.
     */
    public abstract void apply(Player player, int valueEra);

}