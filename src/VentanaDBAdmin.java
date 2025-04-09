import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
@SuppressWarnings("unused")
public class VentanaDBAdmin {
    private Stage ventana = new Stage();
    private TextField tfId = new TextField();
    private TextField tfNombre = new TextField();
    private ComboBox<String> cbCampus = new ComboBox<>();
    private ListView<String> lvCampus = new ListView<>();
    private Connection conn;
    
    private ToggleGroup tgOperacion = new ToggleGroup();
    private RadioButton rbInsertar = new RadioButton("Insertar");
    private RadioButton rbActualizar = new RadioButton("Actualizar");
    private RadioButton rbEliminar = new RadioButton("Eliminar");

    public void mostrar() {
        configurarUI();
        ventana.setScene(new Scene(crearLayoutPrincipal(), 600, 500));
        ventana.setTitle("Gestión de Campus");
        ventana.show();
        cargarCampus();
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

        cbCampus.setDisable(insertar);
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
        gpFormulario.addRow(0, new Label("Seleccionar Campus:"), cbCampus);
        gpFormulario.addRow(1, new Label("ID Campus:"), tfId);
        gpFormulario.addRow(2, new Label("Nombre:"), tfNombre);
        
        cbCampus.setOnAction(e -> rellenarCampos());
        lvCampus.setOnMouseClicked(e -> rellenarCamposDesdeLista());
        
        Button btnEjecutar = new Button("Ejecutar");
        btnEjecutar.setOnAction(e -> ejecutarOperacion());
        
        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> limpiarFormulario());
        
        HBox panelBotones = new HBox(10, btnEjecutar, btnLimpiar);
        
        Label lblLista = new Label("Lista de Campus:");
        lvCampus.setPrefHeight(150);
        
        layout.getChildren().addAll(
            new Label("GESTIÓN DE CAMPUS (DBA)"),
            panelOperaciones,
            gpFormulario,
            panelBotones,
            lblLista,
            lvCampus
        );
        
        return layout;
    }

    private void ejecutarOperacion() {
        try {
            conn = conexion.conectar();
            
            if (rbInsertar.isSelected()) {
                insertarCampus();
            } else if (rbActualizar.isSelected()) {
                actualizarCampus();
            } else if (rbEliminar.isSelected()) {
                eliminarCampus();
            }
            
            cargarCampus();
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void cargarCampus() {
        cbCampus.getItems().clear();
        lvCampus.getItems().clear();
        try {
            conn = conexion.conectar();
            String query = "SELECT idCampus, nombre FROM Campus";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String campus = rs.getInt("idCampus") + " - " + rs.getString("nombre");
                cbCampus.getItems().add(campus);
                lvCampus.getItems().add(campus);
            }
        } catch (SQLException e) {
            mostrarError("Error al cargar campus: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void rellenarCampos() {
        if (cbCampus.getValue() == null) return;
        String[] partes = cbCampus.getValue().split(" - ", 2);
        if (partes.length == 2) {
            tfId.setText(partes[0]);
            tfNombre.setText(partes[1]);
        }
    }
    
    private void rellenarCamposDesdeLista() {
        String seleccion = lvCampus.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;
        String[] partes = seleccion.split(" - ", 2);
        if (partes.length == 2) {
            tfId.setText(partes[0]);
            tfNombre.setText(partes[1]);
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
    }

    private void eliminarCampus() throws SQLException {
        if (tfId.getText().isEmpty()) {
            mostrarError("Debe especificar un ID");
            return;
        }
        try {
            String query = "DELETE FROM Campus WHERE idCampus = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(tfId.getText()));
            int affectedRows = stmt.executeUpdate();
    
            if (affectedRows == 0) {
                mostrarError("No se pudo eliminar el campus. Verifique que el ID sea correcto.");
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
    
            if (sqlState != null) {
                switch (sqlState) {
                    case "23000": // Código genérico de violación de integridad
                        if (e.getMessage().contains("foreign key constraint")) {
                            mostrarError("No se puede eliminar el campus porque existen registros dependientes.");
                        } else if (e.getMessage().contains("PRIMARY")) {
                            mostrarError("Error: ID duplicado. Ya existe un campus con este ID.");
                        } else {
                            mostrarError("Error de integridad de datos: " + e.getMessage());
                        }
                        break;
                    default:
                        throw e; // Lanza el error si no es un caso controlado
                }
            } else {
                throw e;
            }
        }
    }
    

    private void limpiarFormulario() {
        tfId.clear();
        tfNombre.clear();
        cbCampus.getSelectionModel().clearSelection();
        lvCampus.getSelectionModel().clearSelection();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
