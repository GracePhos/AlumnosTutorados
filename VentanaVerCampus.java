import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;
import javafx.scene.control.cell.PropertyValueFactory;
@SuppressWarnings("unused")
public class VentanaVerCampus {
    @SuppressWarnings("unchecked")
    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Campus y Edificios");

        ListView<Campus> lista = new ListView<>();
        TableView<Campus> tabla = new TableView<>();

        lista.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Campus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIdCampus() + " - " + item.getNombreCampus() + " - " + item.getEdificios());
                }
            }
        });
        // Columnas de la tabla
        TableColumn<Campus, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("idCampus"));

        TableColumn<Campus, String> colCampus = new TableColumn<>("Campus");
        colCampus.setCellValueFactory(new PropertyValueFactory<>("nombreCampus"));

        TableColumn<Campus, String> colEdificios = new TableColumn<>("Edificios");
        colEdificios.setCellValueFactory(new PropertyValueFactory<>("edificios"));

        tabla.getColumns().addAll(colId, colCampus, colEdificios);

        // Cargar datos de los campus y edificios
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT c.idCampus, c.nombre AS nombreCampus, GROUP_CONCAT(e.nombre SEPARATOR ', ') AS edificios
                FROM Campus c
                LEFT JOIN Edificio e ON c.idCampus = e.idCampus
                GROUP BY c.idCampus
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            ObservableList<Campus> items = FXCollections.observableArrayList();
            while (rs.next()) {
                items.add(new Campus(
                    rs.getString("idCampus"),
                    rs.getString("nombreCampus"),
                    rs.getString("edificios")
                ));
            }
            tabla.setItems(items);
            lista.setItems(items);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar campus: " + e.getMessage());
            alert.showAndWait();
        }

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(new Label("Campus y Edificios"), lista);
        layout.getChildren().addAll(tabla);

        ventana.setScene(new Scene(layout, 600, 400));
        ventana.show();
    }
}


