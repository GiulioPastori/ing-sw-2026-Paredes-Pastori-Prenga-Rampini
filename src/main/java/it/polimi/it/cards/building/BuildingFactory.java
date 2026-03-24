package it.polimi.it.cards.building;


import it.polimi.it.cards.events.ShamanicRitual;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import java.util.Arrays; //


public class BuildingFactory {

    public static Building create(int id, Era era) {
        return switch (era) {
            case ERA_I -> createEra1(id);
            case ERA_II -> createEra2(id);
            case ERA_III -> createEra3(id);
        };
    }

    // --- ERA I (6 Carte) ---
    private static Building createEra1(int id) {
        return switch (id) {
            case 1 -> new Building("Edificio I-1", Era.ERA_I, 2, 2, "Sconto Cibo Artista", Arrays.<BuildingStrategy>asList(new SustenanceDiscountStrategy(CharacterType.ARTIST, 1))); // giusto
            case 2 -> new Building("Edificio I-2", Era.ERA_I, 3, 2, "Sconto Cibo Inventor", Arrays.<BuildingStrategy>asList(new SustenanceDiscountStrategy(CharacterType.INVENTOR, 1))); // era 2
            case 3 -> new Building("Edificio I-3", Era.ERA_I, 2, 2, "Sconto Cibo Gatherer", Arrays.<BuildingStrategy>asList(new SustenanceDiscountStrategy(CharacterType.GATHERER, 1))); // giusto
            case 4 -> new Building("Edificio I-4", Era.ERA_I, 4, 3, "+5 Cibo per set completo", Arrays.<BuildingStrategy>asList(new SetCollectionStrategy(5, true))); // giusto
            case 5 -> new Building("Edificio I-5", Era.ERA_I, 3, 2, "+3 Icone Sciamano", Arrays.<BuildingStrategy>asList(new ExtraShamanIconsStrategy(3))); // era 2
            case 6 -> new Building("Edificio I-6", Era.ERA_I, 2, 5, "Punti Prestigio fissi", Arrays.<BuildingStrategy>asList(new FixedPointsStrategy(5))); // non esiste
            default -> throw new IllegalArgumentException("ID Era I non valido");
        };
    }

    // --- ERA II (7 Carte) ---
    private static Building createEra2(int id) {
        return switch (id) {
            case 1 -> new Building("Edificio II-1", Era.ERA_II, 3, 2, "Bonus Caccia Hunter", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.HUNTER, 1, 1, 0))); // +1 cibo e +1 pp per cacciatore
            case 2 -> new Building("Edificio II-2", Era.ERA_II, 3, 1, "Bonus Pittura Artista", Arrays.<BuildingStrategy>asList(new CavePaintingBonusStrategy())); // +1 cibo
            case 3 -> new Building("Edificio II-3", Era.ERA_II, 4, 3, "Immunità penale rituale", Arrays.<BuildingStrategy>asList(new NoRitualPenaltyStrategy())); // era 1
            case 4 -> new Building("Edificio II-4", Era.ERA_II, 5, 4, "Raddoppio premio rituale", Arrays.<BuildingStrategy>asList(new DoubleRitualRewardStrategy()));  // Shaman
            case 5 -> new Building("Edificio II-5", Era.ERA_II, 2, 1, "Bonus Cibo Totem", Arrays.<BuildingStrategy>asList(new TotemFoodBonusStrategy())); // è dell'era 1
            case 6 -> new Building("Edificio II-6", Era.ERA_II, 4, 2, "Azione acquisto extra", Arrays.<BuildingStrategy>asList(new ExtraActionStrategy())); // è quello che manca in era 3
            case 7 -> new Building("Edificio II-7", Era.ERA_II, 3, 3, "Bonus coppie Inventor", Arrays.<BuildingStrategy>asList(new InventorPairStrategy(3))); // è dell'era 1
            // manca raddoppio punti presitigio dei builder
            default -> throw new IllegalArgumentException("ID Era II non valido");
        };
    }

    // --- ERA III (8 Carte) ---
    private static Building createEra3(int id) {
        return switch (id) {
            case 1 -> new Building("Edificio III-1", Era.ERA_III, 4, 0, "x3 PP Hunter", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.HUNTER, 0, 0, 3))); // giusto
            case 2 -> new Building("Edificio III-2", Era.ERA_III, 4, 0, "x4 PP Gatherer", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.GATHERER, 0, 0, 4))); // giusto
            case 3 -> new Building("Edificio III-3", Era.ERA_III, 4, 0, "x4 PP Artista", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.ARTIST, 0, 0, 4))); // giusto
            case 4 -> new Building("Edificio III-4", Era.ERA_III, 4, 0, "x2 PP Inventor", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.INVENTOR, 0, 0, 2))); // giusto
            case 5 -> new Building("Edificio III-5", Era.ERA_III, 4, 0, "x4 PP Shaman", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.SHAMAN, 0, 0, 4))); // giusto
            case 6 -> new Building("Edificio III-6", Era.ERA_III, 4, 0, "x4 PP Builder", Arrays.<BuildingStrategy>asList(new PerTypeStrategy(CharacterType.BUILDER, 0, 0, 4))); // giusto
            case 7 -> new Building("Edificio III-7", Era.ERA_III, 5, 0, "+6 PP per set completo", Arrays.<BuildingStrategy>asList(new SetCollectionStrategy(6, false))); // era 2
            case 8 -> new Building("Edificio III-8", Era.ERA_III, 6, 25, "Grande Totem", Arrays.<BuildingStrategy>asList(new FixedPointsStrategy(25))); // giusto

            // Manca edificio che fa pescare una carta dalla fila sopra a fine turno
            default -> throw new IllegalArgumentException("ID Era III non valido");
        };
    }
}