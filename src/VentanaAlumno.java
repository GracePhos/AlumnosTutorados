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

import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;

public class VentanaAlumno {
    private String[] datosAlumno;

    public VentanaAlumno(String[] datosAlumno) {
        this.datosAlumno = datosAlumno;
    }

    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Sesiones del Alumno - " + datosAlumno[1]);

        TableView<Sesion> tabla = new TableView<>();

        // Columnas de la tabla
        TableColumn<Sesion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        TableColumn<Sesion, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));

        TableColumn<Sesion, String> colSala = new TableColumn<>("Sala");
        colSala.setCellValueFactory(new PropertyValueFactory<>("nombreSala"));

        TableColumn<Sesion, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("resumen"));

        tabla.getColumns().addAll(colFecha, colHora, colSala, colDescripcion);

        // Cargar datos de las sesiones del alumno
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT s.fecha, s.hora, sa.nombre AS nombreSala, s.resumen 
                FROM Sesion s
                JOIN Sala sa ON s.idSala = sa.idSala
                WHERE s.numero_control = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(datosAlumno[0]));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tabla.getItems().add(new Sesion(
                    null, // No necesitamos el nombre del alumno aquí
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