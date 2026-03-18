package it.polimi.it.cards;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.Era;

public class Building extends Card {

        private int foodCost;
        private int finalPoints;
        private int buildingId;
        private String effectDescription;

        public Building(String name, Era era, int foodCost, int finalPoints, int buildingId, String effectDescription) {
            super(name, era);
            this.foodCost = foodCost;
            this.finalPoints = finalPoints;
            this.buildingId = buildingId;
            this.effectDescription = effectDescription;
        }


        // * Applica l'effetto dell'edificio al giocatore */
        public void applyEffect(Player player, Board board) {
            if (player== null) {
            }
            player.addprestigePoints(1);
        }

        //**Restituisce il costo in cibo dell'edificio*//
        public int getCost() {

            return this.foodCost;
        }


        public int calculateFinalPoints(Tribe tribe) {
            int     FinalPoints = 0;
            for (Player p : tribe.getPlayers()) {
                finalPoints += p.getPoints();
            }
            // Applica bonus dell'effetto
            finalPoints += bonusPoints;


            return finalPoints;

        }

        public int getBuildingId() {

            return buildingId;
        }

        public String getEffectDescription() {

            return effectDescription;
        }

    }


}
