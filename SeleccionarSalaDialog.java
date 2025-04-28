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
class SeleccionarSalaDialog extends Stage {
    private String salaSeleccionada;
    private ComboBox<String> cbCampus = new ComboBox<>();
    private ComboBox<String> cbEdificios = new ComboBox<>();
    private ComboBox<String> cbSalas = new ComboBox<>();

    public SeleccionarSalaDialog() {
        this.setTitle("Seleccionar Sala");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Cargar campus
        cargarCampus();

        // Listeners para cambios de selección
        cbCampus.setOnAction(e -> cargarEdificios());
        cbEdificios.setOnAction(e -> cargarSalas());

        Button btnConfirmar = new Button("Seleccionar");
        btnConfirmar.setOnAction(e -> {
            if (cbSalas.getSelectionModel().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "¡Selecciona una sala!").show();
                return;
            }
            salaSeleccionada = cbSalas.getValue();
            this.close();
        });

        layout.getChildren().addAll(
            new Label("Campus:"), cbCampus,
            new Label("Edificio:"), cbEdificios,
            new Label("Sala:"), cbSalas,
            btnConfirmar
        );

        this.setScene(new Scene(layout, 300, 250));
    }

    private void cargarCampus() {
        try (Connection conn = conexion.conectar()) {
            cbCampus.getItems().clear();
            String query = "SELECT idCampus, nombre FROM Campus";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                cbCampus.getItems().add(rs.getString("idCampus") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarEdificios() {
        if (cbCampus.getSelectionModel().isEmpty()) return;
        int idCampus = Integer.parseInt(cbCampus.getValue().split(" - ")[0]);

        try (Connection conn = conexion.conectar()) {
            cbEdificios.getItems().clear();
            String query = "SELECT idEdificio, nombre FROM Edificio WHERE idCampus = " + idCampus;
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                cbEdificios.getItems().add(rs.getString("idEdificio") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarSalas() {
        if (cbEdificios.getSelectionModel().isEmpty()) return;
        int idEdificio = Integer.parseInt(cbEdificios.getValue().split(" - ")[0]);

        try (Connection conn = conexion.conectar()) {
            cbSalas.getItems().clear();
            String query = "SELECT idSala, nombre FROM Sala WHERE idEdificio = " + idEdificio;
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                cbSalas.getItems().add(rs.getString("idSala") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getSalaSeleccionada() {
        return salaSeleccionada;
    }
}