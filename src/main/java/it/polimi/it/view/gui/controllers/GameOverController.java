package it.polimi.it.view.gui.controllers;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * End-game screen controller.
 *
 * <p>Receives the list of winners and the final ranking, populates the
 * {@link TableView} with the scores, and highlights the rows corresponding
 * to the winners.
 */
public class GameOverController {

    @FXML private VBox  root;
    @FXML private Label winnersLabel;

    @FXML private TableView<Row>           scoreTable;
    @FXML private TableColumn<Row, Number> rankCol;
    @FXML private TableColumn<Row, String> nameCol;
    @FXML private TableColumn<Row, Number> ppCol;
    @FXML private TableColumn<Row, Number> foodCol;

    /** POJO/record-like row of the ranking. Exposes JavaFX properties. */
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

    @FXML
    private void initialize() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        ppCol  .setCellValueFactory(new PropertyValueFactory<>("pp"));
        foodCol.setCellValueFactory(new PropertyValueFactory<>("food"));

        // Highlight the winning row via CSS class
        scoreTable.setRowFactory(tv -> {
            TableRow<Row> tableRow = new TableRow<>() {
                @Override
                protected void updateItem(Row item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("winner-row");
                    if (!empty && item != null && item.isWinner()) {
                        getStyleClass().add("winner-row");
                    }
                }
            };
            return tableRow;
        });
    }

    /**
     * Popola la schermata con vincitori e classifica.
     */
    public void setResults(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
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
