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
public class VentanaVerCarreras {
    @SuppressWarnings("unchecked")
    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Carreras Disponibles");

        TableView<String[]> tabla = new TableView<>();

        TableColumn<String[], String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));

        tabla.getColumns().addAll(colId, colNombre);

        try (Connection conn = conexion.conectar()) {
            String query = "SELECT idCarrera, nombre FROM Carrera";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tabla.getItems().add(new String[]{
                    rs.getString("idCarrera"),
                    rs.getString("nombre")
                });
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar carreras: " + e.getMessage());
            alert.showAndWait();
        }

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(tabla);

        ventana.setScene(new Scene(layout, 400, 300));
        ventana.show();
    }
}