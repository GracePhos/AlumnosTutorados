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
public class VentanaVerSesiones {
    private String[] datosProfesor;

    public VentanaVerSesiones(String[] datosProfesor) {
        this.datosProfesor = datosProfesor;
    }

    @SuppressWarnings("unchecked")
    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Sesiones del Profesor");

        TableView<Sesion> tabla = new TableView<>();

        // Columnas de la tabla
        TableColumn<Sesion, String> colAlumno = new TableColumn<>("Alumno");
        colAlumno.setCellValueFactory(new PropertyValueFactory<>("nombreAlumno"));

        TableColumn<Sesion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        TableColumn<Sesion, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));

        TableColumn<Sesion, String> colSala = new TableColumn<>("Sala");
        colSala.setCellValueFactory(new PropertyValueFactory<>("nombreSala"));

        TableColumn<Sesion, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("resumen"));

        tabla.getColumns().addAll(colAlumno, colFecha, colHora, colSala, colDescripcion);

        // Cargar datos de las sesiones
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT a.nombres AS nombreAlumno, s.fecha, s.hora, sa.nombre AS nombreSala, s.resumen 
                FROM Sesion s
                JOIN Alumno a ON s.numero_control = a.numero_control
                JOIN Sala sa ON s.idSala = sa.idSala
                WHERE s.idProfesor = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(datosProfesor[0]));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tabla.getItems().add(new Sesion(
                    rs.getString("nombreAlumno"),
                    rs.getString("fecha"),
                    rs.getString("hora"),
                    rs.getString("nombreSala"),
                    rs.getString("resumen")
                ));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error al cargar sesiones: " + e.getMessage());
            alert.showAndWait();
        }

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(tabla);

        ventana.setScene(new Scene(layout, 800, 600));
        ventana.show();
    }
}