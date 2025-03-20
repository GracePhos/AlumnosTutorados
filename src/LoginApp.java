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

public class LoginApp extends Application {

    // Longitudes máximas según la estructura de la base de datos
    private static final int MAX_LONGITUD_CORREO = 25;
    private static final int MAX_LONGITUD_CONTROL = 8;
    private static final int MAX_LONGITUD_APELLIDO = 20;

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        // Componentes de la interfaz de inicio de sesión
        TextField tfUsuario = new TextField();
        tfUsuario.setPromptText("Número de control o correo");
        PasswordField pfContraseña = new PasswordField();
        pfContraseña.setPromptText("Apellido paterno");
        Button btnLogin = new Button("Iniciar sesión");

        // Evento de inicio de sesión
        btnLogin.setOnAction(e -> {
            if (tfUsuario.getText().isEmpty() || pfContraseña.getText().isEmpty()) {
                mostrarAlerta("Error", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                return;
            }

            if (!validarLongitudes(tfUsuario.getText(), pfContraseña.getText())) return;

            try (Connection conn = conexion.conectar()) {
                String usuario = tfUsuario.getText().trim();
                String contraseña = pfContraseña.getText().toLowerCase().trim();

                if (esNumero(usuario)) {
                    if (usuario.length() != MAX_LONGITUD_CONTROL) {
                        mostrarAlerta("Error", "Número de control inválido", Alert.AlertType.ERROR);
                        return;
                    }

                    String[] datosAlumno = obtenerDatosAlumno(conn, usuario, contraseña);
                    if (datosAlumno != null) {
                        mostrarVentanaUsuario("Alumno", datosAlumno);
                        stage.close();
                    } else {
                        mostrarAlerta("Error", "Credenciales incorrectas", Alert.AlertType.ERROR);
                    }
                } else {
                    String[] datosProfesor = obtenerDatosProfesor(conn, usuario, contraseña);
                    if (datosProfesor != null) {
                        mostrarVentanaUsuario("Profesor", datosProfesor);
                        stage.close();
                    } else {
                        mostrarAlerta("Error", "Credenciales incorrectas", Alert.AlertType.ERROR);
                    }
                }

            } catch (Exception ex) {
                mostrarAlerta("Error", "Error de conexión: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // Diseño de la interfaz de inicio de sesión
        grid.add(new Label("Usuario:"), 0, 0);
        grid.add(tfUsuario, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(pfContraseña, 1, 1);
        grid.add(btnLogin, 1, 2);

        stage.setScene(new Scene(grid, 300, 150));
        stage.setTitle("Login Tutorados");
        stage.show();
    }

    // ================== MÉTODOS PARA OBTENER DATOS ==================
    private String[] obtenerDatosProfesor(Connection conn, String correo, String contraseña) throws SQLException {
        String query = "SELECT idProfesor, nombres FROM Profesor WHERE correo = ? AND apellido_paterno = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, correo);
            stmt.setString(2, contraseña);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("idProfesor"),  // Índice 0: ID
                    rs.getString("nombres")      // Índice 1: Nombre
                };
            }
        }
        return null;
    }

    private String[] obtenerDatosAlumno(Connection conn, String numeroControl, String contraseña) throws SQLException {
        String query = "SELECT numero_control, nombres FROM Alumno WHERE numero_control = ? AND apellido_paterno = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, Long.parseLong(numeroControl));
            stmt.setString(2, contraseña);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("numero_control"),  // Índice 0: Número de control
                    rs.getString("nombres")           // Índice 1: Nombre
                };
            }
        }
        return null;
    }

    // ================== VENTANAS DE USUARIO ==================
    private void mostrarVentanaUsuario(String tipoUsuario, String[] datos) {
        if (tipoUsuario.equals("Profesor")) {
            mostrarVentanaProfesor(datos);
        } else {
            mostrarVentanaAlumno(datos);
        }
    }

    private void mostrarVentanaProfesor(String[] datosProfesor) {
        Stage ventana = new Stage();
        ventana.setTitle("Panel del Profesor - " + datosProfesor[1]);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Lista de alumnos asignados
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
            mostrarAlerta("Error", "Error al cargar alumnos", Alert.AlertType.ERROR);
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
        
            if (alumnoSeleccionado == null || sala.equals("Ninguna sala seleccionada")) {
                mostrarAlerta("Error", "¡Selecciona un alumno y una sala!", Alert.AlertType.ERROR);
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
                    mostrarAlerta("Error", "Ya existe una sesión programada en esta sala a la misma hora.", Alert.AlertType.ERROR);
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
        
                mostrarAlerta("Éxito", "Sesión creada correctamente", Alert.AlertType.INFORMATION);
        
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Código de error para violación de restricción única
                    mostrarAlerta("Error", "Ya existe una sesión programada en esta sala a la misma hora.", Alert.AlertType.ERROR);
                } else {
                    mostrarAlerta("Error", "Error al crear sesión: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                mostrarAlerta("Error", "Error inesperado: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // Diseño de la ventana del profesor
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

    private void mostrarVentanaAlumno(String[] datosAlumno) {
        Stage ventana = new Stage();
        ventana.setTitle("Sesiones del Alumno - " + datosAlumno[1]);

        TableView<String[]> tabla = new TableView<>();

        // Columnas
        TableColumn<String[], String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> colSala = new TableColumn<>("Sala");
        colSala.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));

        tabla.getColumns().addAll(colFecha, colHora, colSala);

        // Cargar sesiones del alumno
        try (Connection conn = conexion.conectar()) {
            String query = """
                SELECT s.fecha, s.hora, sa.nombre 
                FROM Sesion s 
                JOIN Sala sa ON s.idSala = sa.idSala 
                WHERE s.numero_control = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(datosAlumno[0]));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tabla.getItems().add(new String[]{
                    rs.getString("fecha"),
                    rs.getString("hora"),
                    rs.getString("nombre")
                });
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "Error al cargar sesiones", Alert.AlertType.ERROR);
        }

        // Diseño de la ventana del alumno
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(new Label("Tus sesiones:"), tabla);

        ventana.setScene(new Scene(layout, 600, 400));
        ventana.show();
    }

    // ================== MÉTODOS AUXILIARES ==================
    private boolean esNumero(String texto) {
        try {
            Long.parseLong(texto);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validarLongitudes(String usuario, String contraseña) {
        if (esNumero(usuario) && usuario.length() > MAX_LONGITUD_CONTROL) {
            mostrarAlerta("Error", "Número de control inválido", Alert.AlertType.ERROR);
            return false;
        } else if (!esNumero(usuario) && usuario.length() > MAX_LONGITUD_CORREO) {
            mostrarAlerta("Error", "Correo demasiado largo", Alert.AlertType.ERROR);
            return false;
        }

        if (contraseña.length() > MAX_LONGITUD_APELLIDO) {
            mostrarAlerta("Error", "Contraseña inválida", Alert.AlertType.ERROR);
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}