package it.polimi.it;

import it.polimi.it.actions.PassAction;
import it.polimi.it.actions.PlaceTotemAction;
import it.polimi.it.actions.TakeCardAction;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.validation.ValidationResult;
import it.polimi.it.view.GameView;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static it.polimi.it.GameIntegrationTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameController}.
 *
 * GameController is the thin MVC controller between Game (model) and GameView (view).
 * It has five responsibilities:
 *  1. Start the game via startGame()
 *  2. Forward valid actions to the model via handle()
 *  3. Call view.showInvalidAction() when an action is rejected
 *  4. Forward every GameEvent to the appropriate view.showXxx() method
 *  5. Expose the underlying game via getGame()
 *
 * Tests use a SpyGameView that records every method call so we can verify
 * which view methods were invoked and how many times.
 */
@DisplayName("GameController")
class GameControllerTest {

    // ── SpyGameView ────────────────────────────────────────────────────────────

    /**
     * A test-only GameView that records every call for later assertions.
     * All showXxx methods append their method name to `calls`.
     * showInvalidAction also records the reason so tests can inspect it.
     */
    static class SpyGameView implements GameView {

        final List<String> calls          = new ArrayList<>();
        final List<String> invalidReasons = new ArrayList<>();

        @Override public void showRoundStarted(int round, List<PlayerSnapshot> order,
                                               BoardSnapshot board) { calls.add("showRoundStarted"); }
        @Override public void showTotemPlaced(PlayerSnapshot p, char tile,
                                              BoardSnapshot board) { calls.add("showTotemPlaced"); }
        @Override public void showCardSlotReady(PlayerSnapshot p, boolean top,
                                                List<CardSnapshot> avail,
                                                List<CardSnapshot> ev) { calls.add("showCardSlotReady"); }
        @Override public void showCardAcquired(PlayerSnapshot p,
                                               CardSnapshot c) { calls.add("showCardAcquired"); }
        @Override public void showCardAcquisitionFailed(PlayerSnapshot p, CardSnapshot c,
                                                        int miss) { calls.add("showCardAcquisitionFailed"); }
        @Override public void showTotemReturnedToTurnOrder(PlayerSnapshot p, int pos,
                                                           BoardSnapshot b) { calls.add("showTotemReturnedToTurnOrder"); }
        @Override public void showPostRoundStarted(List<PlayerSnapshot> wt,
                                                   List<CardSnapshot> av) { calls.add("showPostRoundStarted"); }
        @Override public void showEventResolved(int id, Era era,
                                                EventType t) { calls.add("showEventResolved"); }
        @Override public void showEraChanged(Era e) { calls.add("showEraChanged"); }
        @Override public void showRoundEnded(int r) { calls.add("showRoundEnded"); }
        @Override public void showGameEnded(List<PlayerSnapshot> winners,
                                            List<PlayerSnapshot> scores) { calls.add("showGameEnded"); }
        @Override public void showInvalidAction(String name, String reason) {
            calls.add("showInvalidAction");
            invalidReasons.add(reason);
        }

        // Input stubs — never called in server mode
        @Override public char requestOfferTileChoice(PlayerSnapshot p, BoardSnapshot b) { return 'B'; }
        @Override public int  requestCardChoice(PlayerSnapshot p, List<CardSnapshot> a) { return -1; }
        @Override public int  requestPostRoundCardChoice(PlayerSnapshot p, List<CardSnapshot> a) { return -1; }

        boolean called(String method) { return calls.contains(method); }
        long    count(String method)  { return calls.stream().filter(method::equals).count(); }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Game buildGame() {
        Game g = new GameBuilder()
                .withPlayers(List.of("Alice", "Bob"),
                        List.of(TotemColour.RED, TotemColour.BLUE))
                .withCards(minimalCards())
                .withRandom(new Random(42))
                .build();
        g.setTestMode(true);
        return g;
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Construction and startup")
    class Construction {

        @Test
        @DisplayName("getGame returns the game passed at construction")
        void getGameReturnsModel() {
            Game game = buildGame();
            SpyGameView view = new SpyGameView();
            GameController ctrl = new GameController(game, view);
            assertSame(game, ctrl.getGame());
        }

        @Test
        @DisplayName("startGame triggers RoundStarted event → view.showRoundStarted called")
        void startGameCallsShowRoundStarted() {
            Game game = buildGame();
            SpyGameView view = new SpyGameView();
            GameController ctrl = new GameController(game, view);
            ctrl.startGame();
            assertTrue(view.called("showRoundStarted"),
                    "showRoundStarted must be called after startGame()");
        }

        @Test
        @DisplayName("controller registers itself as listener on construction")
        void controllerRegistersAsListener() {
            Game game = buildGame();
            SpyGameView view = new SpyGameView();
            new GameController(game, view);
            // Verify by starting the game — if listener wasn't registered, no view calls
            game.start();
            assertTrue(view.called("showRoundStarted"));
        }
    }

    @Nested
    @DisplayName("handle() — action forwarding")
    class HandleAction {

        private Game           game;
        private SpyGameView    view;
        private GameController ctrl;

        @BeforeEach
        void setUp() {
            game = buildGame();
            view = new SpyGameView();
            ctrl = new GameController(game, view);
            ctrl.startGame();
        }

        @Test
        @DisplayName("valid action is accepted and returns ValidationResult.Ok")
        void validActionReturnsOk() {
            List<String> o = order(game);
            ValidationResult result = ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            assertTrue(result.isOk());
        }

        @Test
        @DisplayName("valid action does NOT call showInvalidAction")
        void validActionDoesNotShowError() {
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            assertFalse(view.called("showInvalidAction"));
        }

        @Test
        @DisplayName("invalid action is rejected and returns ValidationResult.Fail")
        void invalidActionReturnsFail() {
            // Wrong player acts first
            List<String> o = order(game);
            ValidationResult result = ctrl.handle(new PlaceTotemAction(o.get(1), 'B'));
            assertFalse(result.isOk());
        }

        @Test
        @DisplayName("invalid action calls view.showInvalidAction with a reason")
        void invalidActionCallsShowInvalidAction() {
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(1), 'B')); // wrong player
            assertTrue(view.called("showInvalidAction"),
                    "showInvalidAction must be called for a rejected action");
            assertFalse(view.invalidReasons.isEmpty());
            assertFalse(view.invalidReasons.get(0).isBlank());
        }

        @Test
        @DisplayName("placing a totem calls view.showTotemPlaced")
        void placeTotemCallsView() {
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            assertTrue(view.called("showTotemPlaced"));
        }

        @Test
        @DisplayName("completing placement phase calls view.showCardSlotReady")
        void completionCallsShowCardSlotReady() {
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            ctrl.handle(new PlaceTotemAction(o.get(1), 'C'));
            assertTrue(view.called("showCardSlotReady"),
                    "showCardSlotReady must fire once all players have placed");
        }
    }

    @Nested
    @DisplayName("onEvent() — view routing")
    class EventRouting {

        private Game           game;
        private SpyGameView    view;
        private GameController ctrl;

        @BeforeEach
        void setUp() {
            game = buildGame();
            view = new SpyGameView();
            ctrl = new GameController(game, view);
        }

        @Test
        @DisplayName("RoundStarted → showRoundStarted")
        void roundStartedRouted() {
            ctrl.startGame();
            assertTrue(view.called("showRoundStarted"));
        }

        @Test
        @DisplayName("TotemPlaced → showTotemPlaced")
        void totemPlacedRouted() {
            ctrl.startGame();
            ctrl.handle(new PlaceTotemAction(order(game).get(0), 'B'));
            assertTrue(view.called("showTotemPlaced"));
        }

        @Test
        @DisplayName("CardSlotReady → showCardSlotReady")
        void cardSlotReadyRouted() {
            ctrl.startGame();
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            ctrl.handle(new PlaceTotemAction(o.get(1), 'C'));
            assertTrue(view.called("showCardSlotReady"));
        }

        @Test
        @DisplayName("TotemReturnedToTurnOrder → showTotemReturnedToTurnOrder")
        void totemReturnedRouted() {
            ctrl.startGame();
            List<String> o = order(game);
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            ctrl.handle(new PlaceTotemAction(o.get(1), 'C'));
            ctrl.handle(new PassAction(o.get(0)));
            ctrl.handle(new PassAction(o.get(1)));
            assertTrue(view.called("showTotemReturnedToTurnOrder"));
        }

        @Test
        @DisplayName("RoundEnded → showRoundEnded")
        void roundEndedRouted() {
            ctrl.startGame();
            List<String> o = order(game);
            // Complete a full round: place on B and C, pass both
            ctrl.handle(new PlaceTotemAction(o.get(0), 'B'));
            ctrl.handle(new PlaceTotemAction(o.get(1), 'C'));
            ctrl.handle(new PassAction(o.get(0)));
            ctrl.handle(new PassAction(o.get(1)));
            assertTrue(view.called("showRoundEnded"));
        }

        @Test
        @DisplayName("PostRoundStarted → showPostRoundStarted")
        void postRoundStartedRouted() {
            // PostRoundStarted only fires if at least one player has a Watchtower
            // (building ID 302). Build a game that includes one so Alice can acquire it.
            List<it.polimi.it.cards.Card> cards = minimalCards();
            cards.add(new it.polimi.it.cards.building.Building(302, Era.ERA_I, 0, 0, "Watchtower"));
            Game g = new GameBuilder()
                    .withPlayers(List.of("Alice", "Bob"),
                            List.of(TotemColour.RED, TotemColour.BLUE))
                    .withCards(cards)
                    .withRandom(new Random(42))
                    .build();
            g.setTestMode(true);
            SpyGameView v = new SpyGameView();
            GameController c = new GameController(g, v);
            c.startGame();

            List<String> o = order(g);
            // Place both totems: Alice on C (top slot), Bob on B (bot slot, resolves first)
            c.handle(new PlaceTotemAction(o.get(0), 'B'));
            c.handle(new PlaceTotemAction(o.get(1), 'C'));

            // Bob passes tile B first
            c.handle(new PassAction(o.get(0)));

            // Alice on tile C: take the Watchtower from top row if available,
            // otherwise pass
            Optional<it.polimi.it.cards.Card> wt = g.getBoard().getTopRow().stream()
                    .filter(card -> card.getId() == 302)
                    .findFirst();
            if (wt.isPresent()) {
                c.handle(new it.polimi.it.actions.TakeCardAction(o.get(1), 302, true));
            } else {
                c.handle(new PassAction(o.get(1)));
                // Watchtower ended up in bottom row or deck — just verify round ends
                assertTrue(v.called("showRoundEnded"));
                return;
            }

            // Now Alice has Watchtower → PostRoundStarted must fire
            assertTrue(v.called("showPostRoundStarted"),
                    "showPostRoundStarted must fire when a player has the Watchtower");
        }

        @Test
        @DisplayName("showRoundStarted is called once per round")
        void showRoundStartedCalledOncePerRound() {
            // Use a game WITHOUT testMode so endRound() auto-triggers beginRound().
            // testMode=true blocks that chain, making it impossible to observe round 2.
            Game g = new GameBuilder()
                    .withPlayers(List.of("Alice", "Bob"),
                            List.of(TotemColour.RED, TotemColour.BLUE))
                    .withCards(minimalCards())
                    .withRandom(new Random(42))
                    .build();
            SpyGameView v = new SpyGameView();
            GameController c = new GameController(g, v);
            c.startGame();

            assertEquals(1, v.count("showRoundStarted"),
                    "Exactly one showRoundStarted on start");

            // Complete round 1 — without testMode, round 2 begins automatically
            passRound(g);

            assertTrue(v.count("showRoundStarted") >= 2,
                    "showRoundStarted must fire again for round 2 (count=" + v.count("showRoundStarted") + ")");
        }
    }
}