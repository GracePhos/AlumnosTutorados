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
public class VentanaEditarSesion {
    private String[] datosProfesor;

    public VentanaEditarSesion(String[] datosProfesor) {
        this.datosProfesor = datosProfesor;
    }

    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Editar Sesión");

        GridPane layout = new GridPane();
        layout.setPadding(new Insets(10));
        layout.setVgap(10);
        layout.setHgap(10);

        // Componentes
        ComboBox<String> cbSesiones = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        TextField tfHora = new TextField("HH:MM");
        TextField tfResumen = new TextField();
        Button btnGuardar = new Button("Guardar Cambios");

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

        // Acción para guardar cambios
        btnGuardar.setOnAction(e -> {
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
                String query = """
                    UPDATE Sesion 
                    SET fecha = ?, hora = ?, resumen = ?
                    WHERE idSesion = ?
                    """;
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setDate(1, Date.valueOf(datePicker.getValue()));
                stmt.setString(2, tfHora.getText());
                stmt.setString(3, tfResumen.getText());
                stmt.setInt(4, idSesion);
                stmt.executeUpdate();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Sesión actualizada correctamente.");
                alert.showAndWait();
                ventana.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al actualizar la sesión: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        // Diseño
        layout.add(new Label("Sesión:"), 0, 0);
        layout.add(cbSesiones, 1, 0);
        layout.add(new Label("Fecha:"), 0, 1);
        layout.add(datePicker, 1, 1);
        layout.add(new Label("Hora (HH:MM):"), 0, 2);
        layout.add(tfHora, 1, 2);
        layout.add(new Label("Resumen:"), 0, 3);
        layout.add(tfResumen, 1, 3);
        layout.add(btnGuardar, 1, 4);

        ventana.setScene(new Scene(layout, 400, 250));
        ventana.show();
    }
}