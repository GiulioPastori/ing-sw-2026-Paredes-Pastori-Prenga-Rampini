package it.polimi.it.network.message;

/**
 * Client → Server: player wants to join a game session.
 *
 * @param nickname    chosen nickname (must be unique on the server)
 * @param numPlayers  desired number of players — used only if this player
 *                    creates a new session; ignored by subsequent joiners
 */
record JoinMessage(String nickname, int numPlayers) implements Message {}

/**
 * Client → Server: player places their totem on an offer tile.
 *
 * @param playerName name of the acting player
 * @param tileLetter letter of the chosen tile (e.g. 'B')
 */
record PlaceTotemMessage(String playerName, char tileLetter) implements Message {}

/**
 * Client → Server: player takes a card from the board.
 *
 * @param playerName name of the acting player
 * @param cardId     unique ID of the chosen card
 * @param fromTopRow true if the card is in the top row
 */
record TakeCardMessage(String playerName, int cardId, boolean fromTopRow)
        implements Message {}

/**
 * Client → Server: player passes their card slot.
 *
 * @param playerName name of the acting player
 */
record PassMessage(String playerName) implements Message {}

/**
 * Client → Server: player uses or declines the Watchtower post-round action.
 *
 * @param playerName name of the acting player
 * @param cardId     ID of the chosen card, or -1 to decline
 */
record PostRoundMessage(String playerName, int cardId) implements Message {}