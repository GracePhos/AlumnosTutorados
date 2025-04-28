import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleIntegerProperty;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class VentanaDBAdmin {
    private Stage ventana = new Stage();
    private Connection conn;

    // campus
    private TextField tfId = new TextField();
    private TextField tfNombre = new TextField();
    private ComboBox<String> cbCampus = new ComboBox<>();
    private ListView<String> lvCampus = new ListView<>();
    private ToggleGroup tgOperacion = new ToggleGroup();
    private RadioButton rbInsertar = new RadioButton("Insertar");
    private RadioButton rbActualizar = new RadioButton("Actualizar");
    private RadioButton rbEliminar = new RadioButton("Eliminar");

    // control de paneles
    private ComboBox<String> cbOperacionPrincipal = new ComboBox<>();
    private VBox panelCampus = new VBox(10);
    private VBox panelTutorados = new VBox(10);

    // tutorados
    private ComboBox<String> cbProfesor = new ComboBox<>();
    private ComboBox<String> cbAlumno = new ComboBox<>();
    private Button btnAsignarProfesor = new Button("Asignar Profesor");
    private TableView<Alumno> tableAlumnos = new TableView<>();
    private TableColumn<Alumno, String> colNumeroControl = new TableColumn<>("Número Control");
    private TableColumn<Alumno, String> colNombre = new TableColumn<>("Nombre");

    // label de fecha
    private Label lblFechaActual = new Label();

    public void mostrar() {
        configurarUI();
        ventana.setScene(new Scene(crearLayoutPrincipal(), 600, 650));
        ventana.setTitle("Gestión de Campus y Tutorados - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ventana.show();
        cargarCampus();
        cargarProfesores();
    }

    private void configurarUI() {
        rbInsertar.setToggleGroup(tgOperacion);
        rbActualizar.setToggleGroup(tgOperacion);
        rbEliminar.setToggleGroup(tgOperacion);
        rbInsertar.setSelected(true);

        tgOperacion.selectedToggleProperty().addListener((obs, oldVal, newVal) -> actualizarVisibilidadCampos());
        actualizarVisibilidadCampos();

        cbOperacionPrincipal.getItems().addAll("Gestión de Campus", "Registro de Tutorados");
        cbOperacionPrincipal.setValue("Gestión de Campus");
        cbOperacionPrincipal.setOnAction(e -> cambiarPanel());

        colNumeroControl.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumeroControl()));
        colNombre.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNombre()));
        tableAlumnos.getColumns().addAll(colNumeroControl, colNombre);
        tableAlumnos.setOnMouseClicked(e -> rellenarCamposDesdeTabla());

        // Establecer la fecha actual en el label
        lblFechaActual.setText("Fecha actual: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private VBox crearLayoutPrincipal() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        HBox panelOperaciones = new HBox(10, new Label("Operación:"), rbInsertar, rbActualizar, rbEliminar);
        GridPane gpFormulario = new GridPane();
        gpFormulario.setVgap(10);
        gpFormulario.setHgap(10);
        gpFormulario.addRow(0, new Label("Seleccionar Campus:"), cbCampus);
        gpFormulario.addRow(1, new Label("ID Campus:"), tfId);
        gpFormulario.addRow(2, new Label("Nombre:"), tfNombre);
        Button btnEjecutar = new Button("Ejecutar");
        btnEjecutar.setOnAction(e -> ejecutarOperacion());
        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> limpiarFormulario());
        HBox panelBotones = new HBox(10, btnEjecutar, btnLimpiar);
        Label lblLista = new Label("Lista de Campus:");
        lvCampus.setPrefHeight(150);

        panelCampus.getChildren().addAll(
            new Label("GESTIÓN DE CAMPUS (DBA)"),
            panelOperaciones,
            gpFormulario,
            panelBotones,
            lblLista,
            lvCampus
        );

        cbCampus.setOnAction(e -> rellenarCampos());
        lvCampus.setOnMouseClicked(e -> rellenarCamposDesdeLista());

        GridPane gpTutorados = new GridPane();
        gpTutorados.setVgap(10);
        gpTutorados.setHgap(10);
        gpTutorados.addRow(0, new Label("Profesor Asignado:"), cbProfesor);
        gpTutorados.addRow(1, new Label("Alumno:"), cbAlumno);

        btnAsignarProfesor.setOnAction(e -> asignarProfesor());

        panelTutorados.getChildren().addAll(
            new Label("REGISTRO DE TUTORADOS"),
            gpTutorados,
            tableAlumnos,
            btnAsignarProfesor
        );

        StackPane paneles = new StackPane(panelCampus, panelTutorados);
        panelTutorados.setVisible(false);

        layout.getChildren().addAll(
            lblFechaActual,
            new Label("Operación Principal:"),
            cbOperacionPrincipal,
            paneles
        );

        return layout;
    }

    private void cambiarPanel() {
        boolean esCampus = cbOperacionPrincipal.getValue().equals("Gestión de Campus");
        panelCampus.setVisible(esCampus);
        panelTutorados.setVisible(!esCampus);
        if (!esCampus) cargarAlumnos();
    }

    private void actualizarVisibilidadCampos() {
        boolean insertar = rbInsertar.isSelected();
        boolean eliminar = rbEliminar.isSelected();

        cbCampus.setDisable(insertar);
        tfId.setDisable(insertar);
        tfId.setOpacity(insertar ? 0.3 : 1);
        tfNombre.setDisable(eliminar);
        tfNombre.setOpacity(eliminar ? 0.3 : 1);
    }

    private void ejecutarOperacion() {
        try {
            conn = conexion.conectar();

            if (rbInsertar.isSelected()) insertarCampus();
            else if (rbActualizar.isSelected()) actualizarCampus();
            else if (rbEliminar.isSelected()) eliminarCampus();

            cargarCampus();
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
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
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
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
            if ("23000".equals(e.getSQLState())) {
                if (e.getMessage().contains("foreign key constraint"))
                    mostrarError("No se puede eliminar el campus porque existen registros dependientes.");
                else
                    mostrarError("Error de integridad de datos: " + e.getMessage());
            } else throw e;
        }
    }

    private void limpiarFormulario() {
        tfId.clear();
        tfNombre.clear();
        cbCampus.getSelectionModel().clearSelection();
        lvCampus.getSelectionModel().clearSelection();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.showAndWait();
    }

    private void cargarProfesores() {
        try {
            conn = conexion.conectar();
            String query = "SELECT idProfesor, nombres, apellido_paterno, apellido_materno FROM Profesor";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            ObservableList<String> listaProfesores = FXCollections.observableArrayList();
            while (rs.next()) {
                int id = rs.getInt("idProfesor");
                String nombre = rs.getString("nombres") + " " + rs.getString("apellido_paterno") + " " + rs.getString("apellido_materno");
                listaProfesores.add(id + " - " + nombre);
            }
            cbProfesor.setItems(listaProfesores);
        } catch (SQLException e) {
            mostrarError("Error al cargar los profesores.");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    private void cargarAlumnos() {
        try {
            conn = conexion.conectar();
            String query = "SELECT numero_control, nombres, apellido_paterno FROM Alumno WHERE idProfesor = 0 OR idProfesor IS NULL";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            tableAlumnos.getItems().clear();
            ObservableList<String> listaAlumnos = FXCollections.observableArrayList();
            while (rs.next()) {
                String numeroControl = rs.getString("numero_control");
                String nombreCompleto = rs.getString("nombres") + " " + rs.getString("apellido_paterno");
                tableAlumnos.getItems().add(new Alumno(numeroControl, nombreCompleto));
                listaAlumnos.add(numeroControl + " - " + nombreCompleto);
            }
            cbAlumno.setItems(listaAlumnos);
        } catch (SQLException e) {
            mostrarError("Error al cargar alumnos: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    private void rellenarCamposDesdeTabla() {
        Alumno seleccionado = tableAlumnos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            cbAlumno.setValue(seleccionado.getNumeroControl() + " - " + seleccionado.getNombre());
        }
    }

    private void asignarProfesor() {
        Alumno alumnoSeleccionado = tableAlumnos.getSelectionModel().getSelectedItem();
        String profesorSeleccionado = cbProfesor.getValue();

        if (profesorSeleccionado == null || alumnoSeleccionado == null) {
            mostrarError("Debes seleccionar un profesor y un alumno.");
            return;
        }

        String[] profesorParts = profesorSeleccionado.split(" - ");
        int idProfesor = Integer.parseInt(profesorParts[0].trim());

        String numeroControlStr = alumnoSeleccionado.getNumeroControl();
        int numeroControl = Integer.parseInt(numeroControlStr);

        try (Connection conn = conexion.conectar()) {
            String query = "UPDATE Alumno SET idProfesor = ? WHERE numero_control = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, idProfesor);
            stmt.setInt(2, numeroControl);
            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas > 0) {
                Alert alerta = new Alert(Alert.AlertType.INFORMATION, 
                    "Asignación Exitosa:\n" +
                    "Alumno: " + alumnoSeleccionado.getNombre() + "\n" +
                    "No. Control: " + numeroControl + "\n" +
                    "Profesor ID: " + idProfesor + "\n" +
                    "Fecha: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
                alerta.showAndWait();
                tableAlumnos.getSelectionModel().clearSelection();
                cbProfesor.getSelectionModel().clearSelection();
                cargarAlumnos();
            } else {
                mostrarError("No se encontró el alumno seleccionado.");
            }
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        }
    }

    public class Alumno {
        private SimpleStringProperty numeroControl;
        private SimpleStringProperty nombre;

        public Alumno(String numeroControl, String nombre) {
            this.numeroControl = new SimpleStringProperty(numeroControl);
            this.nombre = new SimpleStringProperty(nombre);
        }

        public String getNumeroControl() {
            return numeroControl.get();
        }

        public String getNombre() {
            return nombre.get();
        }
    }
}
