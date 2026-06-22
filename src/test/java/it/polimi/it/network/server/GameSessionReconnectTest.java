package it.polimi.it.network.server;

import it.polimi.it.CardLoader;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.PlayerSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionReconnectTest {

    @Test
    @DisplayName("resume after reconnect replays the current prompt")
    void resumeAfterReconnectReplaysCurrentPrompt() throws Exception {
        List<Card> previousCards = ServerApp.getCardList();
        try {
            ServerApp.setCardList(CardLoader.loadAll(Path.of("src/main/resources/cards.json")));

            GameSession session = new GameSession(2);
            FakeConnection alice = new FakeConnection("Alice", TotemColour.RED);
            FakeConnection bob = new FakeConnection("Bob", TotemColour.BLUE);

            assertTrue(session.addClient(alice));
            assertTrue(session.addClient(bob));

            List<String> order = lastMessage(bob, Message.ROUND_STARTED)
                    .playerList("turnOrder").stream()
                    .map(PlayerSnapshot::name)
                    .toList();
            FakeConnection first = order.get(0).equals("Alice") ? alice : bob;
            FakeConnection second = first == alice ? bob : alice;

            session.handleMessage(Message.placeTotem(first.nickname, 'B'), first);
            session.handleMessage(Message.placeTotem(second.nickname, 'C'), second);

            session.onPlayerDisconnected(first.nickname, first);
            assertTrue(session.isSuspended(), "Two-player game must suspend with one survivor.");

            long returnedBeforeIgnoredPass = countTotemReturnedFor(second, second.nickname);
            long promptsBeforeReconnect = countMessages(second, Message.CARD_SLOT_READY);
            session.handleMessage(Message.pass(second.nickname), second);
            assertTrue(session.isSuspended());
            assertTrue(countTotemReturnedFor(second, second.nickname) == returnedBeforeIgnoredPass,
                    "Actions sent while suspended must not advance the model.");

            FakeConnection reconnectedFirst = new FakeConnection(first.nickname, first.colour);
            session.reconnectClient(reconnectedFirst);
            assertFalse(session.isSuspended(), "Reconnect must resume the session.");
            assertTrue(countMessages(second, Message.CARD_SLOT_READY) > promptsBeforeReconnect,
                    "The survivor must receive the current card prompt again.");

            session.handleMessage(Message.pass(second.nickname), second);
            assertTrue(countTotemReturnedFor(second, second.nickname) > returnedBeforeIgnoredPass,
                    "After resume, the survivor's pass must be accepted.");
        } finally {
            ServerApp.setCardList(previousCards);
        }
    }

    private static Message lastMessage(FakeConnection connection, String type) {
        for (int i = connection.sent.size() - 1; i >= 0; i--) {
            Message message = connection.sent.get(i);
            if (type.equals(message.getType())) return message;
        }
        throw new AssertionError("Missing message " + type + " for " + connection.nickname);
    }

    private static long countMessages(FakeConnection connection, String type) {
        return connection.sent.stream()
                .filter(message -> type.equals(message.getType()))
                .count();
    }

    private static long countTotemReturnedFor(FakeConnection connection, String nickname) {
        return connection.sent.stream()
                .filter(message -> Message.TOTEM_RETURNED.equals(message.getType()))
                .filter(message -> nickname.equals(message.playerObj("player").name()))
                .count();
    }

    private static final class FakeConnection implements ClientConnection {
        private final String nickname;
        private final TotemColour colour;
        private final List<Message> sent = new CopyOnWriteArrayList<>();

        private FakeConnection(String nickname, TotemColour colour) {
            this.nickname = nickname;
            this.colour = colour;
        }

        @Override
        public String getNickname() {
            return nickname;
        }

        @Override
        public TotemColour getTotemColour() {
            return colour;
        }

        @Override
        public void send(Message message) {
            sent.add(message);
        }

        @Override
        public void close() {
        }
    }
}
