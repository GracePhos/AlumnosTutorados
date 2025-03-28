import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;

public class VentanaDBAdmin {
    private Stage ventana = new Stage();
    private TextField tfId = new TextField();
    private TextField tfNombre = new TextField();
    private TextArea taHistorial = new TextArea();
    private Connection conn;
    
    private ToggleGroup tgOperacion = new ToggleGroup();
    private RadioButton rbInsertar = new RadioButton("Insertar");
    private RadioButton rbActualizar = new RadioButton("Actualizar");
    private RadioButton rbEliminar = new RadioButton("Eliminar");

    public void mostrar() {
        configurarUI();
        ventana.setScene(new Scene(crearLayoutPrincipal(), 600, 400));
        ventana.setTitle("Gestión de Campus");
        ventana.show();
        cargarCampusAlInicio();
    }

    private void configurarUI() {
        rbInsertar.setToggleGroup(tgOperacion);
        rbActualizar.setToggleGroup(tgOperacion);
        rbEliminar.setToggleGroup(tgOperacion);
        rbInsertar.setSelected(true);
        
        tgOperacion.selectedToggleProperty().addListener((obs, oldVal, newVal) -> actualizarVisibilidadCampos());
        actualizarVisibilidadCampos();
    }

    private void actualizarVisibilidadCampos() {
        boolean insertar = rbInsertar.isSelected();
        boolean actualizar = rbActualizar.isSelected();
        boolean eliminar = rbEliminar.isSelected();

        tfId.setDisable(insertar);
        tfId.setOpacity(insertar ? 0.3 : 1);
        
        tfNombre.setDisable(eliminar);
        tfNombre.setOpacity(eliminar ? 0.3 : 1);
    }

    private VBox crearLayoutPrincipal() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        
        HBox panelOperaciones = new HBox(10, 
            new Label("Operación:"), rbInsertar, rbActualizar, rbEliminar);
        
        GridPane gpFormulario = new GridPane();
        gpFormulario.setVgap(10);
        gpFormulario.setHgap(10);
        gpFormulario.addRow(0, new Label("ID Campus:"), tfId);
        gpFormulario.addRow(1, new Label("Nombre:"), tfNombre);
        
        Button btnEjecutar = new Button("Ejecutar");
        btnEjecutar.setOnAction(e -> ejecutarOperacion());
        
        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> limpiarFormulario());
        
        HBox panelBotones = new HBox(10, btnEjecutar, btnLimpiar);
        
        taHistorial.setEditable(false);
        taHistorial.setPrefHeight(200);
        
        layout.getChildren().addAll(
            new Label("GESTIÓN DE CAMPUS (DBA)"),
            panelOperaciones,
            gpFormulario,
            panelBotones,
            new Label("Historial:"),
            taHistorial
        );
        
        return layout;
    }

    private void ejecutarOperacion() {
        taHistorial.clear();
        try {
            conn = conexion.conectar();
            
            if (rbInsertar.isSelected()) {
                insertarCampus();
            } else if (rbActualizar.isSelected()) {
                actualizarCampus();
            } else if (rbEliminar.isSelected()) {
                eliminarCampus();
            }
            
            cargarCampusAlInicio();
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void cargarCampusAlInicio() {
        taHistorial.clear();
        try {
            conn = conexion.conectar();
            consultarCampus();
        } catch (SQLException e) {
            mostrarError("Error al cargar campus: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void consultarCampus() throws SQLException {
        String query = "SELECT idCampus, nombre FROM Campus";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        taHistorial.appendText("\n=== TODOS LOS CAMPUS ===\n");
        while (rs.next()) {
            taHistorial.appendText(rs.getInt("idCampus") + ": " + rs.getString("nombre") + "\n");
        }
    }

    private void insertarCampus() throws SQLException {
        if (tfNombre.getText().isEmpty()) {
            mostrarError("El nombre es obligatorio");
            return;
        }
        String query = "INSERT INTO Campus (nombre) VALUES (?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, tfNombre.getText());
        stmt.executeUpdate();
        taHistorial.appendText("Campus insertado exitosamente.\n");
    }

    private void actualizarCampus() throws SQLException {
        if (tfId.getText().isEmpty() || tfNombre.getText().isEmpty()) {
            mostrarError("ID y Nombre son obligatorios");
            return;
        }
        String query = "UPDATE Campus SET nombre = ? WHERE idCampus = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, tfNombre.getText());
        stmt.setInt(2, Integer.parseInt(tfId.getText()));
        stmt.executeUpdate();
        taHistorial.appendText("Campus actualizado exitosamente.\n");
    }

    private void eliminarCampus() throws SQLException {
        if (tfId.getText().isEmpty()) {
            mostrarError("Debe especificar un ID");
            return;
        }
        String query = "DELETE FROM Campus WHERE idCampus = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, Integer.parseInt(tfId.getText()));
        stmt.executeUpdate();
        taHistorial.appendText("Campus eliminado exitosamente.\n");
    }

    private void limpiarFormulario() {
        tfId.clear();
        tfNombre.clear();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
        taHistorial.appendText("ERROR: " + mensaje + "\n");
    }
}
