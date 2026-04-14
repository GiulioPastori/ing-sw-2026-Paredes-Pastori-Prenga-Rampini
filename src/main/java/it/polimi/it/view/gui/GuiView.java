package it.polimi.it.view.gui;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Swing-based graphical user interface.
 *
 * All Swing updates run on the EDT via SwingUtilities.invokeLater().
 * Input requests (requestXxx) block the calling thread using a
 * BlockingQueue until the user makes a choice in the GUI.
 *
 * Layout:
 * ┌──────────────────────────────────────┐
 * │  TOP ROW          │  PLAYER PANELS   │
 * ├──────────────────────────────────────┤
 * │  OFFER TRACK                         │
 * ├──────────────────────────────────────┤
 * │  BOTTOM ROW                          │
 * ├──────────────────────────────────────┤
 * │  LOG                                 │
 * └──────────────────────────────────────┘
 */
public class GuiView implements GameView {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(40, 25, 15);
    private static final Color PANEL_BG    = new Color(58, 38, 22);
    private static final Color BORDER      = new Color(180, 140, 80);
    private static final Color TEXT        = new Color(220, 195, 150);
    private static final Color FOOD_COLOR  = new Color(100, 200, 100);
    private static final Color PP_COLOR    = new Color(220, 180, 60);

    // ── Swing components ──────────────────────────────────────────────────────
    private JFrame      mainFrame;
    private JTextArea   logArea;
    private JPanel      topRowPanel;
    private JPanel      bottomRowPanel;
    private JPanel      offerTrackPanel;
    private JPanel      playerPanel;

    // ── Input queues ──────────────────────────────────────────────────────────
    private final BlockingQueue<Integer>   cardChoiceQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Character> tileChoiceQueue = new ArrayBlockingQueue<>(1);

    public GuiView() {
        SwingUtilities.invokeLater(this::buildFrame);
    }

    // ── Frame construction ────────────────────────────────────────────────────

    private void buildFrame() {
        mainFrame = new JFrame("MESOS");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1200, 800);
        mainFrame.setLayout(new BorderLayout(6, 6));
        mainFrame.getContentPane().setBackground(BG);

        topRowPanel    = buildRowPanel("Top Row");
        bottomRowPanel = buildRowPanel("Bottom Row");
        offerTrackPanel = buildRowPanel("Offer Track");
        playerPanel    = new JPanel(new GridLayout(1, 0, 6, 0));
        playerPanel.setBackground(BG);
        playerPanel.setPreferredSize(new Dimension(0, 100));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(25, 15, 8));
        logArea.setForeground(TEXT);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(320, 0));
        logScroll.setBorder(titledBorder("Log"));

        JPanel center = new JPanel(new GridLayout(3, 1, 4, 4));
        center.setBackground(BG);
        center.add(topRowPanel);
        center.add(offerTrackPanel);
        center.add(bottomRowPanel);

        mainFrame.add(center,      BorderLayout.CENTER);
        mainFrame.add(logScroll,   BorderLayout.EAST);
        mainFrame.add(playerPanel, BorderLayout.SOUTH);
        mainFrame.setVisible(true);
    }

    private JPanel buildRowPanel(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.setBackground(PANEL_BG);
        p.setBorder(titledBorder(title));
        return p;
    }

    private javax.swing.border.Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title,
                0, 0, null, TEXT);
    }

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder) {
        SwingUtilities.invokeLater(() -> {
            log("══ ROUND " + round + " STARTED ══");
            updatePlayerPanel(turnOrder);
        });
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter) {
        SwingUtilities.invokeLater(() ->
                log(player.name() + " placed totem on tile " + tileLetter));
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards) {
        SwingUtilities.invokeLater(() -> {
            log("[" + player.name() + "] " + (isTopRow ? "top" : "bottom")
                    + " row slot ready.");
            updateRowPanel(isTopRow ? topRowPanel : bottomRowPanel, availableCards, false);
        });
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        SwingUtilities.invokeLater(() ->
                log(player.name() + " acquired " + shortCard(card)));
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        SwingUtilities.invokeLater(() ->
                log(player.name() + " cannot afford " + shortCard(card)
                        + " (missing " + missingFood + " food)"));
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        SwingUtilities.invokeLater(() ->
                log(player.name() + " → Turn Order #" + position));
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        SwingUtilities.invokeLater(() -> log("[WATCHTOWER] Post-round action."));
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        SwingUtilities.invokeLater(() -> log("[EVENT] " + era + " " + type));
    }

    @Override
    public void showEraChanged(Era era) {
        SwingUtilities.invokeLater(() -> {
            log("★ ERA " + era + " BEGINS ★");
            JOptionPane.showMessageDialog(mainFrame, "Era " + era + " has begun!",
                    "New Era", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void showRoundEnded(int round) {
        SwingUtilities.invokeLater(() -> log("── Round " + round + " ended ──"));
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        SwingUtilities.invokeLater(() -> {
            String winnerNames = winners.stream()
                    .map(PlayerSnapshot::name)
                    .reduce((a, b) -> a + ", " + b).orElse("none");
            log("★ GAME OVER — Winner(s): " + winnerNames);
            StringBuilder sb = new StringBuilder("<html><b>Final Scores:</b><br>");
            finalScores.stream()
                    .sorted((a, b) -> b.prestigePoints() - a.prestigePoints())
                    .forEach(p -> sb.append(p.name()).append(": ")
                            .append(p.prestigePoints()).append(" PP<br>"));
            sb.append("</html>");
            JOptionPane.showMessageDialog(mainFrame, sb.toString(),
                    "Game Over — " + winnerNames, JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        SwingUtilities.invokeLater(() -> log("[!] " + playerName + ": " + reason));
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       List<OfferTileSnapshot> freeTiles) {
        SwingUtilities.invokeLater(() -> {
            offerTrackPanel.removeAll();
            log("[" + player.name() + "] Choose an offer tile.");
            for (OfferTileSnapshot t : freeTiles) {
                String label = t.isFoodOnly()
                        ? "Tile " + t.letter() + "<br>+3 Food"
                        : "Tile " + t.letter() + "<br>T:" + t.topRowCards()
                        + " B:" + t.bottomRowCards();
                JButton btn = tileButton(label, t.letter());
                offerTrackPanel.add(btn);
            }
            offerTrackPanel.revalidate();
            offerTrackPanel.repaint();
        });
        try { return tileChoiceQueue.take(); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return freeTiles.get(0).letter();
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        SwingUtilities.invokeLater(() -> {
            log("[" + player.name() + "] Choose a card.");
            updateRowPanel(topRowPanel, available, true);
        });
        try { return cardChoiceQueue.take(); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player,
                                          List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        SwingUtilities.invokeLater(() -> {
            log("[WATCHTOWER " + player.name() + "] Choose a card or close to decline.");
            updateRowPanel(topRowPanel, available, true);
        });
        try { return cardChoiceQueue.take(); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    // ── GUI helpers ───────────────────────────────────────────────────────────

    private void updateRowPanel(JPanel panel, List<CardSnapshot> cards, boolean clickable) {
        panel.removeAll();
        for (CardSnapshot c : cards) panel.add(cardButton(c, clickable));
        panel.revalidate();
        panel.repaint();
    }

    private JButton cardButton(CardSnapshot card, boolean clickable) {
        String html = "<html><center>" + card.era() + "<br>"
                + shortCard(card) + "</center></html>";
        JButton btn = new JButton(html);
        btn.setPreferredSize(new Dimension(110, 72));
        btn.setBackground(cardColor(card));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        if (clickable) btn.addActionListener(e -> cardChoiceQueue.offer(card.id()));
        else           btn.setEnabled(false);
        return btn;
    }

    private JButton tileButton(String htmlLabel, char letter) {
        JButton btn = new JButton("<html><center>" + htmlLabel + "</center></html>");
        btn.setPreferredSize(new Dimension(90, 56));
        btn.setBackground(new Color(90, 60, 35));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> tileChoiceQueue.offer(letter));
        return btn;
    }

    private void updatePlayerPanel(List<PlayerSnapshot> players) {
        playerPanel.removeAll();
        for (PlayerSnapshot p : players) {
            JPanel card = new JPanel(new GridLayout(3, 1));
            card.setBackground(PANEL_BG);
            card.setBorder(BorderFactory.createLineBorder(BORDER));
            JLabel name = new JLabel(p.name(), SwingConstants.CENTER);
            name.setForeground(TEXT);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            JLabel food = new JLabel("Food: " + p.food(), SwingConstants.CENTER);
            food.setForeground(FOOD_COLOR);
            JLabel pp = new JLabel("PP: " + p.prestigePoints(), SwingConstants.CENTER);
            pp.setForeground(PP_COLOR);
            card.add(name); card.add(food); card.add(pp);
            playerPanel.add(card);
        }
        playerPanel.revalidate();
        playerPanel.repaint();
    }

    private Color cardColor(CardSnapshot c) {
        if (c.type() == CardSnapshot.Type.BUILDING)   return new Color(75, 55, 110);
        if (c.characterType() == null)                return new Color(60, 60, 60);
        return switch (c.characterType()) {
            case HUNTER   -> new Color(130, 45, 45);
            case GATHERER -> new Color(45, 95, 45);
            case SHAMAN   -> new Color(75, 45, 115);
            case BUILDER  -> new Color(95, 75, 35);
            case INVENTOR -> new Color(35, 75, 115);
            case ARTIST   -> new Color(115, 55, 85);
        };
    }

    private String shortCard(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> c.characterType() != null ? c.characterType().toString() : "CHARACTER";
            case BUILDING  -> "BLDG +" + c.finalPoints() + "PP";
            case EVENT     -> "EVENT";
        };
    }

    private void log(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
