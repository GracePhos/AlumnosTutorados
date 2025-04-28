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
public class VentanaEliminarSesion {
    private String[] datosProfesor;

    public VentanaEliminarSesion(String[] datosProfesor) {
        this.datosProfesor = datosProfesor;
    }

    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Eliminar Sesión");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        ComboBox<String> cbSesiones = new ComboBox<>();
        Button btnEliminar = new Button("Eliminar Sesión");

        // Cargar sesiones del profesor
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT s.idSesion, a.nombres, s.fecha, s.hora 
                FROM Sesion s
                JOIN Alumno a ON s.numero_control = a.numero_control
                WHERE s.idProfesor = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(datosProfesor[0]));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                cbSesiones.getItems().add(
                    rs.getString("idSesion") + " - " + rs.getString("nombres") + " (" + rs.getString("fecha") + ")"
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar sesiones: " + e.getMessage());
            alert.showAndWait();
        }

        // Acción para eliminar sesión
        btnEliminar.setOnAction(e -> {
            String sesionSeleccionada = cbSesiones.getSelectionModel().getSelectedItem();
            if (sesionSeleccionada == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("¡Selecciona una sesión!");
                alert.showAndWait();
                return;
            }

            int idSesion = Integer.parseInt(sesionSeleccionada.split(" - ")[0]);

            try (Connection conn = conexion.conectar()) {
                String query = "DELETE FROM Sesion WHERE idSesion = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, idSesion);
                stmt.executeUpdate();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Sesión eliminada correctamente.");
                alert.showAndWait();
                ventana.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al eliminar la sesión: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        // Diseño
        layout.getChildren().addAll(new Label("Selecciona una sesión:"), cbSesiones, btnEliminar);

        ventana.setScene(new Scene(layout, 400, 200));
        ventana.show();
    }
}