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
public class VentanaCrearSesion {
    private String[] datosProfesor;

    public VentanaCrearSesion(String[] datosProfesor) {
        this.datosProfesor = datosProfesor;
    }

    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Crear Sesión");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Componentes para crear sesión
        ListView<String> listaAlumnos = new ListView<>();
        Button btnSeleccionarSala = new Button("Seleccionar Sala");
        Label lblSalaSeleccionada = new Label("Ninguna sala seleccionada");
        DatePicker datePicker = new DatePicker();
        TextField tfHora = new TextField("HH:MM");
        TextField tfResumen = new TextField();
        Button btnCrear = new Button("Crear Sesión");

        // Cargar alumnos asignados
        try (Connection conn = conexion.conectar()) {
            String query = "SELECT numero_control, nombres FROM Alumno WHERE idProfesor = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(datosProfesor[0]));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                listaAlumnos.getItems().add(
                    rs.getString("numero_control") + " - " + rs.getString("nombres")
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar alumnos: " + e.getMessage());
            alert.showAndWait();
        }

        // Acción para seleccionar sala
        btnSeleccionarSala.setOnAction(e -> {
            SeleccionarSalaDialog dialogo = new SeleccionarSalaDialog();
            dialogo.showAndWait();
            if (dialogo.getSalaSeleccionada() != null) {
                lblSalaSeleccionada.setText(dialogo.getSalaSeleccionada());
            }
        });

        // Acción para crear sesión
        btnCrear.setOnAction(e -> {
            String alumnoSeleccionado = listaAlumnos.getSelectionModel().getSelectedItem();
            String sala = lblSalaSeleccionada.getText();

            if (alumnoSeleccionado == null || sala.equals("Ninguna sala seleccionada") || datePicker.getValue() == null || tfHora.getText().isEmpty() || tfResumen.getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Por favor, complete todos los campos.");
                alert.showAndWait();
                return;
            }

            try (Connection conn = conexion.conectar()) {
                // Extraer el número de control del alumno y el ID de la sala
                int numeroControl = Integer.parseInt(alumnoSeleccionado.split(" - ")[0]);
                int idSala = Integer.parseInt(sala.split(" - ")[0]);

                // Verificar si ya existe una sesión en la misma sala, fecha y hora
                String queryVerificar = "SELECT COUNT(*) FROM Sesion WHERE idSala = ? AND fecha = ? AND hora = ?";
                PreparedStatement stmtVerificar = conn.prepareStatement(queryVerificar);
                stmtVerificar.setInt(1, idSala);
                stmtVerificar.setDate(2, Date.valueOf(datePicker.getValue()));
                stmtVerificar.setString(3, tfHora.getText());
                ResultSet rs = stmtVerificar.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Ya existe una sesión programada en esta sala a la misma hora.");
                    alert.showAndWait();
                    return;
                }

                // Insertar la nueva sesión
                String queryInsertar = "INSERT INTO Sesion (numero_control, idSala, idProfesor, fecha, hora, resumen) VALUES (?,?,?,?,?,?)";
                PreparedStatement stmtInsertar = conn.prepareStatement(queryInsertar);
                stmtInsertar.setInt(1, numeroControl);
                stmtInsertar.setInt(2, idSala);
                stmtInsertar.setInt(3, Integer.parseInt(datosProfesor[0]));
                stmtInsertar.setDate(4, Date.valueOf(datePicker.getValue()));
                stmtInsertar.setString(5, tfHora.getText());
                stmtInsertar.setString(6, tfResumen.getText());
                stmtInsertar.executeUpdate();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Sesión creada correctamente.");
                alert.showAndWait();

            } catch (SQLException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error al crear sesión: " + ex.getMessage());
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Error inesperado: " + ex.getMessage());
                alert.showAndWait();
            } 
        });

        // Diseño de la ventana
        layout.getChildren().addAll(
            new Label("Alumnos asignados:"), listaAlumnos,
            new Label("Sala seleccionada:"), lblSalaSeleccionada,
            btnSeleccionarSala,
            new Label("Fecha:"), datePicker,
            new Label("Hora (HH:MM):"), tfHora,
            new Label("Resumen:"), tfResumen,
            btnCrear
        );

        ventana.setScene(new Scene(layout, 500, 500));
        ventana.show();
    }
}
