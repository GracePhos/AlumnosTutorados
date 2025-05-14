import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@SuppressWarnings("unused")
public class VentanaDBAdmin {
    private Stage ventana = new Stage();
    private Connection conn;

    // Componentes para Campus
    private TextField tfId = new TextField();
    private TextField tfNombre = new TextField();
    private ComboBox<String> cbCampus = new ComboBox<>();
    private ListView<String> lvCampus = new ListView<>();
    private ToggleGroup tgOperacion = new ToggleGroup();
    private RadioButton rbInsertar = new RadioButton("Insertar");
    private RadioButton rbActualizar = new RadioButton("Actualizar");
    private RadioButton rbEliminar = new RadioButton("Eliminar");

    // Control de paneles
    private ComboBox<String> cbOperacionPrincipal = new ComboBox<>();
    private VBox panelCampus = new VBox(10);
    private VBox panelTutorados = new VBox(10);

    // Componentes para Tutorados
    private ComboBox<String> cbProfesor = new ComboBox<>();
    private TextField tfNumeroControlAlumno = new TextField();
    private TextField tfNombreAlumno = new TextField();
    private Button btnAsignarProfesor = new Button("Asignar Profesor");
    private TableView<Alumno> tableAlumnosDisponibles = new TableView<>();
    private TableView<Alumno> tableAlumnosAsignados = new TableView<>();
    private SplitPane splitPaneTablas = new SplitPane();
    private Button btnDesasignar = new Button("Desasignar Alumno");
    private Button btnImprimir = new Button("Imprimir PDF");
    
    // Label de fecha
    private Label lblFechaActual = new Label();

    /**
     * Muestra la ventana de gestión de campus y tutorados.
     * Configura la interfaz de usuario, crea el layout principal y
     * muestra la ventana con el título que incluye la fecha actual.
     * Luego carga los campus y profesores en sus respectivos
     * componentes.
     */
    public void mostrar() {
        configurarUI();
        ventana.setScene(new Scene(crearLayoutPrincipal(), 1200, 800)); // Aumenté el ancho para acomodar las dos tablas
        ventana.setTitle("Gestión de Campus y Tutorados - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ventana.show();
        cargarCampus();
        cargarProfesores();
    }

    /**
     * Configura la interfaz de usuario para la ventana de gestión de campus y tutorados.
     * Inicializa los componentes de la interfaz, configura sus propiedades y establece
     * los eventos de clic y de cambio de selección en los componentes.
     */

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

        // Configurar tablas
        configurarTabla(tableAlumnosDisponibles, "Disponibles para Asignar");
        configurarTabla(tableAlumnosAsignados, "Alumnos Asignados");

        cbProfesor.setOnAction(e -> {
            cargarAlumnosAsignados();
        });

        // Configurar SplitPane
        splitPaneTablas.getItems().addAll(
            new VBox(new Label("Alumnos Disponibles para Asignar"), tableAlumnosDisponibles),
            new VBox(new Label("Alumnos Ya Asignados"), tableAlumnosAsignados)
        );
        splitPaneTablas.setDividerPositions(0.5);

        // Configurar eventos de clic
        tableAlumnosDisponibles.setOnMouseClicked(e -> rellenarCamposDesdeTabla(tableAlumnosDisponibles));
        tableAlumnosAsignados.setOnMouseClicked(e -> rellenarCamposDesdeTabla(tableAlumnosAsignados));

        btnDesasignar.setOnAction(e -> desasignarAlumno());
        btnImprimir.setOnAction(e -> generarPDFAlumnosAsignados());


        lblFechaActual.setText("Fecha actual: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private void desasignarAlumno() {
        Alumno alumnoSeleccionado = tableAlumnosAsignados.getSelectionModel().getSelectedItem();
        
        if (alumnoSeleccionado == null) {
            mostrarError("Debes seleccionar un alumno asignado para desasignar.");
            return;
        }
        
        try (Connection conn = conexion.conectar()) {
            String query = "UPDATE Alumno SET idProfesor = NULL WHERE numero_control = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, alumnoSeleccionado.getNumeroControl());
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                Alert alerta = new Alert(Alert.AlertType.INFORMATION,
                    "Alumno desasignado exitosamente:\n" +
                    "Alumno: " + alumnoSeleccionado.getNombres() + " " + 
                    alumnoSeleccionado.getApellidoPaterno() + "\n" +
                    "No. Control: " + alumnoSeleccionado.getNumeroControl()
                );
                alerta.showAndWait();
                cargarAlumnos(); // Recargar ambas listas
            } else {
                mostrarError("No se pudo desasignar el alumno.");
            }
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        }
    }

    /**
     * Configura una TableView para mostrar la información de los Alumnos. La configuración
     * incluye establecer las columnas para mostrar los siguientes atributos de los Alumnos:
     * número de control, nombres, apellido paterno, apellido materno y carrera.
     * 
     */
    @SuppressWarnings("unchecked")
    private void configurarTabla(TableView<Alumno> tabla, String titulo) {
        TableColumn<Alumno, String> colNumeroControl = new TableColumn<>("Número Control");
        colNumeroControl.setCellValueFactory(cellData -> cellData.getValue().numeroControlProperty());
        
        TableColumn<Alumno, String> colNombres = new TableColumn<>("Nombres");
        colNombres.setCellValueFactory(cellData -> cellData.getValue().nombresProperty());
        
        TableColumn<Alumno, String> colApellidoPaterno = new TableColumn<>("Apellido Paterno");
        colApellidoPaterno.setCellValueFactory(cellData -> cellData.getValue().apellidoPaternoProperty());
        
        TableColumn<Alumno, String> colApellidoMaterno = new TableColumn<>("Apellido Materno");
        colApellidoMaterno.setCellValueFactory(cellData -> cellData.getValue().apellidoMaternoProperty());

        TableColumn<Alumno, String> colCarrera = new TableColumn<>("Carrera");
        colCarrera.setCellValueFactory(cellData -> cellData.getValue().carreraProperty());

        tabla.getColumns().clear();
        tabla.getColumns().addAll(colNumeroControl, colNombres, colApellidoPaterno, colApellidoMaterno, colCarrera);
    }

    /**
     * Crea el layout principal de la ventana para el administrador de base de datos.
     * El layout consta de una sección para la gestión de campus y otra sección para
     * el registro de tutorados. La sección de gestión de campus permite insertar,
     * actualizar o eliminar campus, mientras que la sección de registro de tutorados
     * permite asignar profesores a los alumnos. La sección de gestión de campus
     * se muestra por defecto, y se puede cambiar a la sección de registro de
     * tutorados seleccionando la opción correspondiente en el combobox de la parte
     * superior de la ventana.
     * @return El layout principal de la ventana.
     */
    @SuppressWarnings("static-access")
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
    
        // Configuración de la sección de Tutorados
        GridPane gpTutorados = new GridPane();
        gpTutorados.setVgap(10);
        gpTutorados.setHgap(10);
        gpTutorados.addRow(0, new Label("Profesor Asignado:"), cbProfesor);
        gpTutorados.addRow(1, new Label("Nombre del Alumno:"), tfNombreAlumno);
        gpTutorados.addRow(2, new Label("N. Control:"), tfNumeroControlAlumno);
    
        tfNombreAlumno.setEditable(false);
        tfNumeroControlAlumno.setEditable(false);
    
        btnAsignarProfesor.setOnAction(e -> asignarProfesor());
        
        Button btnLimpiarTutorados = new Button("Limpiar");
        btnLimpiarTutorados.setOnAction(e -> {
            cbProfesor.getSelectionModel().clearSelection();
            tfNumeroControlAlumno.clear();
            tfNombreAlumno.clear();
            tableAlumnosDisponibles.getSelectionModel().clearSelection();
            tableAlumnosAsignados.getSelectionModel().clearSelection();
        });
    
                HBox panelBotonesTutorados = new HBox(10, btnAsignarProfesor, btnDesasignar, btnLimpiarTutorados, btnImprimir);
    
        panelTutorados.getChildren().addAll(
            new Label("REGISTRO DE TUTORADOS"),
            gpTutorados,
            splitPaneTablas,
            panelBotonesTutorados
        );

    
        StackPane paneles = new StackPane(panelCampus, panelTutorados);
        panelTutorados.setVisible(false);
    
        HBox panelSuperior = new HBox();
        panelSuperior.setPadding(new Insets(0, 0, 10, 0));
        panelSuperior.setSpacing(10);
        panelSuperior.setHgrow(lblFechaActual, Priority.ALWAYS);
        Label lblAdmin = new Label("Editando como admin");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        panelSuperior.getChildren().addAll(lblFechaActual, espacio, lblAdmin);
    
        layout.getChildren().addAll(
            panelSuperior,
            new Label("Operación Principal:"), cbOperacionPrincipal,
            paneles
        );
    
        return layout;
    }

private void generarPDFAlumnosAsignados() {
    String profesorSeleccionado = cbProfesor.getValue();
    if (profesorSeleccionado == null || profesorSeleccionado.isEmpty()) {
        mostrarError("Debes seleccionar un profesor para generar el PDF.");
        return;
    }

    try {
        // Obtener datos del profesor
        String[] partesProfesor = profesorSeleccionado.split(" - ", 2);
        String nombreProfesor = partesProfesor.length > 1 ? partesProfesor[1].trim() : "Desconocido";

        // Obtener la lista de alumnos asignados
        ObservableList<Alumno> alumnos = tableAlumnosAsignados.getItems();
        if (alumnos.isEmpty()) {
            mostrarError("El profesor seleccionado no tiene alumnos asignados.");
            return;
        }

        // Crear el documento PDF
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Configuración inicial - usar solo fuentes estándar
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("Reporte de Alumnos Asignados");
            contentStream.endText();

            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 720);
            contentStream.showText("Profesor: " + nombreProfesor);
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText("Fecha: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            contentStream.endText();

            // Crear tabla
            drawTable(contentStream, 50, 650, alumnos);
        }

        // Guardar el documento
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("alumnos_asignados_" + nombreProfesor.replace(" ", "_") + ".pdf");
        File file = fileChooser.showSaveDialog(ventana);

        if (file != null) {
            document.save(file);
            document.close();
            
            Alert alerta = new Alert(Alert.AlertType.INFORMATION, 
                "PDF generado exitosamente en:\n" + file.getAbsolutePath());
            alerta.showAndWait();
        } else {
            document.close();
        }
    } catch (IOException e) {
        mostrarError("Error al generar el PDF: " + e.getMessage());
        e.printStackTrace();
    }
}

    /**
     * Retorna una cadena vacia si el parametro es nulo, de lo contrario retorna el parametro.
     * @param s Cadena a evaluar.
     * @return Cadena vacia si s es nulo, de lo contrario s.
     */
private String safeText(String s) {
    return s == null ? "" : s;
}


private void drawTable(PDPageContentStream contentStream, float x, float y, 
                      ObservableList<Alumno> alumnos) throws IOException {
    final int rows = alumnos.size() + 1;
    final int cols = 5;
    final float rowHeight = 20f;
    final float tableWidth = 500f;
    final float colWidth = tableWidth / cols;

    // Encabezados
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
    String[] headers = {"No. Control", "Nombres", "Apellido Paterno", "Apellido Materno", "Carrera"};

    float nexty = y;
    for (int i = 0; i <= alumnos.size(); i++) {
        // Línea horizontal
        contentStream.moveTo(x, nexty);
        contentStream.lineTo(x + tableWidth, nexty);
        contentStream.stroke();

        float nextx = x;

        for (int j = 0; j < cols; j++) {
            contentStream.moveTo(nextx, y);
            contentStream.lineTo(nextx, y - rowHeight * rows);
            contentStream.stroke();

            contentStream.beginText();
            contentStream.newLineAtOffset(nextx + 5, nexty - 15);

            String text = "";
            if (i == 0) {
                text = headers[j];
            } else {
                Alumno alumno = alumnos.get(i - 1);
                switch (j) {
                    case 0: text = safeText(alumno.getNumeroControl()); break;
                    case 1: text = safeText(alumno.getNombres()); break;
                    case 2: text = safeText(alumno.getApellidoPaterno()); break;
                    case 3: text = safeText(alumno.getApellidoMaterno()); break;
                    case 4: text = safeText(alumno.getCarrera()); break;
                }
            }

            contentStream.showText(text);
            contentStream.endText();

            nextx += colWidth;
        }

        nexty -= rowHeight;
    }

    // Última línea vertical derecha
    contentStream.moveTo(x + tableWidth, y);
    contentStream.lineTo(x + tableWidth, y - rowHeight * rows);
    contentStream.stroke();
}

    /**
     * Cambia el panel mostrado en la interfaz de usuario.
     * Si se selecciona "Gestión de Campus", se muestra el panel de gestión de campus.
     * Si se selecciona "Registro de Tutorados", se muestra el panel de registro de tutorados.
     * Llama a cargarAlumnos() para cargar la lista de alumnos en el panel de registro de tutorados.
     */
    private void cambiarPanel() {
        boolean esCampus = cbOperacionPrincipal.getValue().equals("Gestión de Campus");
        panelCampus.setVisible(esCampus);
        panelTutorados.setVisible(!esCampus);
        if (!esCampus) cargarAlumnos();
    }

    /**
     * Actualiza la visibilidad de los campos de la interfaz de usuario.
     * Si se selecciona "Insertar", deshabilita el campo de selección de campus y
     * establece la opacidad del campo de ID en 0.3.
     * Si se selecciona "Eliminar", deshabilita el campo de ID y
     * establece la opacidad del campo de nombre en 0.3.
     */
    private void actualizarVisibilidadCampos() {
        boolean insertar = rbInsertar.isSelected();
        boolean eliminar = rbEliminar.isSelected();

        cbCampus.setDisable(insertar);
        tfId.setDisable(insertar);
        tfId.setOpacity(insertar ? 0.3 : 1);
        tfNombre.setDisable(eliminar);
        tfNombre.setOpacity(eliminar ? 0.3 : 1);
    }

    /**
     * Ejecuta la operación seleccionada en la interfaz de usuario.
     * Realiza la conexión a la base de datos, ejecuta la operación correspondiente
     * (insertar, actualizar o eliminar) y vuelve a cargar los campus en la interfaz
     * de usuario. Muestra un diálogo de error si ocurre un error de base de datos.
     * Cierra la conexión a la base de datos al finalizar.
     */
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

    /**
     * Carga los campus en la interfaz de usuario.
     * Limpia las listas de campus en el combobox y en la lista de campus,
     * conecta a la base de datos, ejecuta un query para obtener los campus,
     * y los agrega a las listas. Muestra un diálogo de error si ocurre
     * un error de base de datos. Cierra la conexión a la base de datos
     * al finalizar.
     */
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

/**
 * Rellena los campos de texto con el ID y el nombre del campus seleccionado
 * en el ComboBox de campus. Si no hay un campus seleccionado, no realiza 
 * ninguna acción. Se espera que los valores en el ComboBox estén en el 
 * formato "ID - Nombre".
 */

    private void rellenarCampos() {
        if (cbCampus.getValue() == null) return;
        String[] partes = cbCampus.getValue().split(" - ", 2);
        if (partes.length == 2) {
            tfId.setText(partes[0]);
            tfNombre.setText(partes[1]);
        }
    }

    /**
     * Rellena los campos de texto con el ID y el nombre del campus seleccionado
     * en la lista de campus. Si no hay un campus seleccionado, no realiza ninguna
     * accion. Se espera que los valores en la lista estan en el formato "ID - Nombre".
     */
    private void rellenarCamposDesdeLista() {
        String seleccion = lvCampus.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;
        String[] partes = seleccion.split(" - ", 2);
        if (partes.length == 2) {
            tfId.setText(partes[0]);
            tfNombre.setText(partes[1]);
        }
    }

/**
 * Inserta un nuevo campus en la base de datos con el nombre especificado.
 * Si el campo de nombre está vacío, muestra un error y no realiza la inserción.
 * 
 * @throws SQLException Si ocurre un error al ejecutar la consulta SQL.
 */

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

    /**
     * Limpia los campos de texto y los ComboBox de la interfaz de usuario.
     * Limpia el campo de ID, el campo de nombre, el ComboBox de campus,
     * la lista de campus, el ComboBox de profesores, el campo de número de
     * control, el campo de nombre del alumno y las selecciones de las tablas
     * de alumnos disponibles y asignados.
     */
    private void limpiarFormulario() {
        tfId.clear();
        tfNombre.clear();
        cbCampus.getSelectionModel().clearSelection();
        lvCampus.getSelectionModel().clearSelection();
        
        cbProfesor.getSelectionModel().clearSelection();
        tfNumeroControlAlumno.clear();
        tfNombreAlumno.clear();
        tableAlumnosDisponibles.getSelectionModel().clearSelection();
        tableAlumnosAsignados.getSelectionModel().clearSelection();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.showAndWait();
    }

    /**
     * Carga la lista de profesores desde la base de datos y los muestra 
     * en el ComboBox de selección de profesores. 
     * Ejecuta una consulta para obtener los datos de los profesores incluyendo 
     * sus nombres y apellidos, y los agrega al ComboBox en el formato "ID - Nombre Completo".
     * Muestra un mensaje de error si ocurre un problema al realizar la consulta.
     */

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
        cargarAlumnosDisponibles();
        cargarAlumnosAsignados();
    }

    /**
     * Carga la lista de alumnos disponibles (sin asignar) desde la base de datos y los muestra 
     * en la tabla de alumnos disponibles. 
     * Ejecuta una consulta para obtener los datos de los alumnos incluyendo 
     * sus números de control, nombres, apellidos y carreras, y los agrega a la tabla en el formato "NC - Nombres Apellidos Carrera".
     * Muestra un mensaje de error si ocurre un problema al realizar la consulta.
     */
    private void cargarAlumnosDisponibles() {
        try {
            conn = conexion.conectar();
            String query = "SELECT a.numero_control, a.nombres, a.apellido_paterno, " +
                          "a.apellido_materno, c.nombre as carrera " +
                          "FROM Alumno a " +
                          "JOIN Carrera c ON a.idCarrera = c.idCarrera " +
                          "WHERE a.idProfesor = 0 OR a.idProfesor IS NULL";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            tableAlumnosDisponibles.getItems().clear();
            while (rs.next()) {
                String numeroControl = rs.getString("numero_control");
                String nombres = rs.getString("nombres");
                String apellidoPaterno = rs.getString("apellido_paterno");
                String apellidoMaterno = rs.getString("apellido_materno");
                String carrera = rs.getString("carrera");
                tableAlumnosDisponibles.getItems().add(new Alumno(numeroControl, nombres, apellidoPaterno, 
                                                         apellidoMaterno, carrera));
            }
        } catch (SQLException e) {
            mostrarError("Error al cargar alumnos disponibles: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Carga la lista de alumnos asignados al profesor seleccionado en el ComboBox
     * de selección de profesores y los muestra en la tabla de alumnos asignados.
     * Ejecuta una consulta para obtener los datos de los alumnos incluyendo 
     * sus números de control, nombres, apellidos y carreras, y los agrega a la tabla
     * en el formato "NC - Nombres Apellidos Carrera". Si no se selecciona un profesor,
     * se limpia la tabla. Muestra un mensaje de error si ocurre un problema al realizar
     * la consulta.
     */
private void cargarAlumnosAsignados() {
    String profesorSeleccionado = cbProfesor.getValue();
    if (profesorSeleccionado == null || profesorSeleccionado.isEmpty()) {
        tableAlumnosAsignados.getItems().clear();
        return;
    }
    
    try {
        conn = conexion.conectar();
        int idProfesor = Integer.parseInt(profesorSeleccionado.split(" - ")[0].trim());
        String nombreProfesor = profesorSeleccionado.split(" - ")[1].trim();
        
        // Crear etiqueta con estilo especial
        Label lblTitulo = new Label("ALUMNOS ASIGNADOS A PROFESOR " + nombreProfesor.toUpperCase());
        lblTitulo.setStyle("-fx-text-fill: red; -fx-background-color: yellow; -fx-font-size: 16px; -fx-font-weight: bold;");
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setPadding(new Insets(5));
        
        // Reemplazar el título en el SplitPane
        VBox panelAsignados = (VBox) splitPaneTablas.getItems().get(1);
        panelAsignados.getChildren().set(0, lblTitulo);
        
        // Resto del código para cargar alumnos...
        String query = "SELECT a.numero_control, a.nombres, a.apellido_paterno, " +
                      "a.apellido_materno, c.nombre as carrera " +
                      "FROM Alumno a " +
                      "JOIN Carrera c ON a.idCarrera = c.idCarrera " +
                      "WHERE a.idProfesor = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, idProfesor);
        ResultSet rs = stmt.executeQuery();
        
        tableAlumnosAsignados.getItems().clear();
        while (rs.next()) {
            String numeroControl = rs.getString("numero_control");
            String nombres = rs.getString("nombres");
            String apellidoPaterno = rs.getString("apellido_paterno");
            String apellidoMaterno = rs.getString("apellido_materno");
            String carrera = rs.getString("carrera");
            tableAlumnosAsignados.getItems().add(new Alumno(numeroControl, nombres, apellidoPaterno, 
                                                 apellidoMaterno, carrera));
        }
    } catch (SQLException e) {
        mostrarError("Error al cargar alumnos asignados: " + e.getMessage());
    } finally {
        if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
    }
}

    private void rellenarCamposDesdeTabla(TableView<Alumno> tabla) {
        Alumno seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null && tabla == tableAlumnosDisponibles) {
            tfNombreAlumno.setText(seleccionado.getNombres() + " " + seleccionado.getApellidoPaterno());
            tfNumeroControlAlumno.setText(seleccionado.getNumeroControl());
        } else {
            tfNombreAlumno.clear();
            tfNumeroControlAlumno.clear();
        }
    }

    /**
     * Asigna un profesor a un alumno.
     * 
     * Valida que se haya seleccionado un profesor y un alumno,
     * y que el alumno exista en la base de datos.
     * 
     * Si es exitoso, muestra un mensaje de confirmación con los
     * detalles de la asignación y limpia los campos de texto y
     * las selecciones de las tablas.
     * 
     * Si falla, muestra un mensaje de error con la descripción del
     * problema.
     */
    private void asignarProfesor() {
        String profesorSeleccionado = cbProfesor.getValue();
        String numeroControlStr = tfNumeroControlAlumno.getText();

        if (profesorSeleccionado == null || numeroControlStr.isEmpty()) {
            mostrarError("Debes seleccionar un profesor y un alumno.");
            return;
        }

        int idProfesor = Integer.parseInt(profesorSeleccionado.split(" - ")[0].trim());
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
                    "Alumno: " + tfNombreAlumno.getText() + "\n" +
                    "No. Control: " + numeroControl + "\n" +
                    "Profesor ID: " + idProfesor + "\n" +
                    "Fecha: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                );
                alerta.showAndWait();
                tableAlumnosDisponibles.getSelectionModel().clearSelection();
                tfNombreAlumno.clear();
                tfNumeroControlAlumno.clear();
                cargarAlumnosDisponibles(); // Solo recarga los disponibles
                cargarAlumnosAsignados();   // Recarga los asignados al profesor actual
            } else {
                mostrarError("No se encontró el alumno seleccionado.");
            }
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        }
    }


    /** Clase interna para representar un alumno en la tabla, para poder acceder a sus propiedades de manera mas facil xd */
    public class Alumno {
        private SimpleStringProperty numeroControl;
        private SimpleStringProperty nombres;
        private SimpleStringProperty apellidoPaterno;
        private SimpleStringProperty apellidoMaterno;
        private SimpleStringProperty carrera;
    
        public Alumno(String numeroControl, String nombres, String apellidoPaterno, 
                     String apellidoMaterno, String carrera) {
            this.numeroControl = new SimpleStringProperty(numeroControl);
            this.nombres = new SimpleStringProperty(nombres);
            this.apellidoPaterno = new SimpleStringProperty(apellidoPaterno);
            this.apellidoMaterno = new SimpleStringProperty(apellidoMaterno);
            this.carrera = new SimpleStringProperty(carrera);
        }
    
        public String getNumeroControl() { return numeroControl.get(); }
        public String getNombres() { return nombres.get(); }
        public String getApellidoPaterno() { return apellidoPaterno.get(); }
        public String getApellidoMaterno() { return apellidoMaterno.get(); }
        public String getCarrera() { return carrera.get(); }
    
        public SimpleStringProperty numeroControlProperty() { return numeroControl; }
        public SimpleStringProperty nombresProperty() { return nombres; }
        public SimpleStringProperty apellidoPaternoProperty() { return apellidoPaterno; }
        public SimpleStringProperty apellidoMaternoProperty() { return apellidoMaterno; }
        public SimpleStringProperty carreraProperty() { return carrera; }
    }
}