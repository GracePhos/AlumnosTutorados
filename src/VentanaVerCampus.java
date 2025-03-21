import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
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

        TableView<Campus> tabla = new TableView<>();

        // Columnas de la tabla
        TableColumn<Campus, String> colCampus = new TableColumn<>("Campus");
        colCampus.setCellValueFactory(new PropertyValueFactory<>("nombreCampus"));

        TableColumn<Campus, String> colEdificios = new TableColumn<>("Edificios");
        colEdificios.setCellValueFactory(new PropertyValueFactory<>("edificios"));

        tabla.getColumns().addAll(colCampus, colEdificios);

        // Cargar datos de los campus y edificios
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT c.nombre AS nombreCampus, GROUP_CONCAT(e.nombre SEPARATOR ', ') AS edificios
                FROM Campus c
                LEFT JOIN Edificio e ON c.idCampus = e.idCampus
                GROUP BY c.idCampus
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tabla.getItems().add(new Campus(
                    rs.getString("nombreCampus"),
                    rs.getString("edificios")
                ));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar campus: " + e.getMessage());
            alert.showAndWait();
        }

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(tabla);

        ventana.setScene(new Scene(layout, 600, 400));
        ventana.show();
    }
}