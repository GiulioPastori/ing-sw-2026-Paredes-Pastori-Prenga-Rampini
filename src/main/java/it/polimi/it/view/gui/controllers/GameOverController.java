package it.polimi.it.view.gui.controllers;

import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.FinalScoreBreakdown;
import it.polimi.it.observer.PlayerSnapshot;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller della schermata di fine partita.
 *
 * <p>Riceve la lista dei vincitori e la classifica finale, popola la
 * {@link TableView} con i punteggi e mette in risalto le righe corrispondenti
 * ai vincitori.
 *
 * <p>Espone inoltre il metodo {@link #setLeaderboard} che, quando arriva il
 * messaggio {@code LEADERBOARD} dal server (FA "Classifica partite su DB"),
 * popola la tabella inferiore con la classifica globale per le partite con
 * lo stesso numero di giocatori, evidenziando la riga del giocatore corrente.
 */
public class GameOverController {

    @FXML private VBox  root;
    @FXML private Label winnersLabel;
    @FXML private TextArea finalBreakdownArea;

    @FXML private TableView<Row>           scoreTable;
    @FXML private TableColumn<Row, Number> rankCol;
    @FXML private TableColumn<Row, String> nameCol;
    @FXML private TableColumn<Row, Number> ppCol;
    @FXML private TableColumn<Row, Number> foodCol;

    // ── Leaderboard widgets (FA "Classifica partite su DB") ──────────────────
    @FXML private Label leaderboardHeader;
    @FXML private Label leaderboardSummary;
    @FXML private TableView<LbRow>           leaderboardTable;
    @FXML private TableColumn<LbRow, Number> lbRankCol;
    @FXML private TableColumn<LbRow, String> lbNameCol;
    @FXML private TableColumn<LbRow, Number> lbScoreCol;
    @FXML private TableColumn<LbRow, String> lbDateCol;

    private static final DateTimeFormatter LB_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** POJO/record-like riga della classifica della partita corrente. */
    public static class Row {
        private final SimpleIntegerProperty rank = new SimpleIntegerProperty();
        private final SimpleStringProperty  name = new SimpleStringProperty();
        private final SimpleIntegerProperty pp   = new SimpleIntegerProperty();
        private final SimpleIntegerProperty food = new SimpleIntegerProperty();
        private final boolean               winner;

        Row(int rank, PlayerSnapshot p, boolean winner) {
            this.rank.set(rank);
            this.name.set(p.name());
            this.pp.set(p.prestigePoints());
            this.food.set(p.food());
            this.winner = winner;
        }

        public int     getRank() { return rank.get(); }
        public String  getName() { return name.get(); }
        public int     getPp()   { return pp.get(); }
        public int     getFood() { return food.get(); }
        public boolean isWinner(){ return winner; }
    }

    /** Riga della tabella inferiore (classifica globale storica). */
    public static class LbRow {
        private final SimpleIntegerProperty rank  = new SimpleIntegerProperty();
        private final SimpleStringProperty  name  = new SimpleStringProperty();
        private final SimpleIntegerProperty score = new SimpleIntegerProperty();
        private final SimpleStringProperty  date  = new SimpleStringProperty();
        private final boolean               highlight;

        LbRow(LeaderboardEntry e, boolean highlight) {
            this.rank.set(e.rank());
            this.name.set(e.nickname());
            this.score.set(e.finalScore());
            this.date.set(e.playedAt() == null ? "—" : LB_DATE_FMT.format(e.playedAt()));
            this.highlight = highlight;
        }

        public int     getRank()     { return rank.get(); }
        public String  getName()     { return name.get(); }
        public int     getScore()    { return score.get(); }
        public String  getDate()     { return date.get(); }
        public boolean isHighlight() { return highlight; }
    }

    @FXML
    private void initialize() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        ppCol  .setCellValueFactory(new PropertyValueFactory<>("pp"));
        foodCol.setCellValueFactory(new PropertyValueFactory<>("food"));

        // Evidenzia la riga vincitrice via classe CSS
        scoreTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Row item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("winner-row");
                if (!empty && item != null && item.isWinner()) {
                    getStyleClass().add("winner-row");
                }
            }
        });

        // ── Leaderboard table setup ───────────────────────────────────────────
        if (leaderboardTable != null) {
            lbRankCol .setCellValueFactory(new PropertyValueFactory<>("rank"));
            lbNameCol .setCellValueFactory(new PropertyValueFactory<>("name"));
            lbScoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
            lbDateCol .setCellValueFactory(new PropertyValueFactory<>("date"));

            leaderboardTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(LbRow item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("highlight-row");
                    if (!empty && item != null && item.isHighlight()) {
                        getStyleClass().add("highlight-row");
                    }
                }
            });
            // Until a LEADERBOARD message arrives, show a placeholder.
            if (leaderboardHeader != null)
                leaderboardHeader.setText("Classifica");
            if (leaderboardSummary != null)
                leaderboardSummary.setText("In attesa della classifica…");
        }
    }

    /**
     * Popola la schermata con vincitori e classifica della partita.
     */
    public void setResults(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        setResults(winners, finalScores, List.of());
    }

    /**
     * Popola la schermata con vincitori, resoconto del calcolo punti e
     * classifica della partita.
     */
    public void setResults(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores,
                           List<FinalScoreBreakdown> scoreBreakdowns) {
        runFx(() -> {
            String winnerNames = winners.stream()
                    .map(PlayerSnapshot::name)
                    .reduce((a, b) -> a + ", " + b).orElse("nessuno");
            winnersLabel.setText("Vincitore" + (winners.size() > 1 ? "i: " : ": ") + winnerNames);

            Set<String> winSet = new HashSet<>();
            for (PlayerSnapshot w : winners) winSet.add(w.name());

            // Ordina per PP desc, poi per food desc (come da regolamento)
            List<PlayerSnapshot> sorted = finalScores.stream()
                    .sorted(Comparator
                            .comparingInt(PlayerSnapshot::prestigePoints).reversed()
                            .thenComparing(Comparator
                                    .comparingInt(PlayerSnapshot::food).reversed()))
                    .toList();

            var rows = FXCollections.<Row>observableArrayList();
            int rank = 1;
            for (PlayerSnapshot p : sorted) {
                rows.add(new Row(rank++, p, winSet.contains(p.name())));
            }
            scoreTable.setItems(rows);
            fillFinalBreakdown(sorted, scoreBreakdowns);
        });
    }

    private void fillFinalBreakdown(List<PlayerSnapshot> sortedScores,
                                    List<FinalScoreBreakdown> scoreBreakdowns) {
        if (finalBreakdownArea == null) return;
        if (scoreBreakdowns == null || scoreBreakdowns.isEmpty()) {
            finalBreakdownArea.setText("Resoconto non disponibile.");
            return;
        }

        Map<String, FinalScoreBreakdown> byPlayer = scoreBreakdowns.stream()
                .collect(Collectors.toMap(FinalScoreBreakdown::playerName,
                        Function.identity(), (a, b) -> a));
        StringBuilder sb = new StringBuilder();
        for (PlayerSnapshot p : sortedScores) {
            FinalScoreBreakdown b = byPlayer.get(p.name());
            if (b == null) continue;
            sb.append(b.playerName()).append(": ")
                    .append(b.inGamePrestige()).append(" PP in partita")
                    .append(" + ").append(b.builderPoints()).append(" costruttori")
                    .append(" + ").append(b.inventorPoints()).append(" inventori")
                    .append(" + ").append(b.artistPoints()).append(" artisti")
                    .append(" + ").append(b.buildingPoints()).append(" edifici")
                    .append(" = ").append(b.totalPrestige()).append(" PP")
                    .append(" | cibo ").append(b.food())
                    .append('\n');
        }
        finalBreakdownArea.setText(sb.toString());
    }

    /**
     * Popola la tabella inferiore con la classifica globale ricevuta dal
     * server.
     *
     * @param recipient    nickname del giocatore corrente (riga evidenziata)
     * @param myScore      punteggio finale del giocatore corrente
     * @param myRank       posizione 1-based del giocatore corrente
     * @param totalEntries numero totale di risultati registrati per quel
     *                     numero di giocatori
     * @param numPlayers   numero di giocatori delle partite considerate
     * @param leaderboard  classifica globale (già ordinata, con rank
     *                     impostato)
     */
    public void setLeaderboard(String recipient,
                               int myScore,
                               int myRank,
                               int totalEntries,
                               int numPlayers,
                               List<LeaderboardEntry> leaderboard) {
        runFx(() -> {
            if (leaderboardTable == null) return; // FXML predates this feature

            if (leaderboardHeader != null) {
                leaderboardHeader.setText(
                        "Classifica — Partite a " + numPlayers + " giocatori");
            }
            if (leaderboardSummary != null) {
                String rankText = myRank > 0
                        ? "#" + myRank + " su " + totalEntries
                        : "posizione non disponibile";
                leaderboardSummary.setText(
                        recipient + " — punteggio: " + myScore + " — " + rankText);
            }

            var rows = FXCollections.<LbRow>observableArrayList();
            for (LeaderboardEntry e : leaderboard) {
                boolean highlight = recipient != null
                        && recipient.equals(e.nickname())
                        && e.finalScore() == myScore;
                rows.add(new LbRow(e, highlight));
            }
            leaderboardTable.setItems(rows);

            // Auto-scroll to the player's row, if found
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).isHighlight()) {
                    leaderboardTable.scrollTo(i);
                    break;
                }
            }
        });
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
        Platform.exit();
    }

    public VBox getRoot() { return root; }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
