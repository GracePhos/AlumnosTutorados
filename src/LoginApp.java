import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.sql.*;
@SuppressWarnings("unused")
public class LoginApp extends Application {

    private static final int MAX_LONGITUD_CORREO = 25;
    private static final int MAX_LONGITUD_CONTROL = 8;
    private static final int MAX_LONGITUD_APELLIDO = 20;

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        TextField tfUsuario = new TextField();
        tfUsuario.setPromptText("Número de control o correo");
        PasswordField pfContraseña = new PasswordField();
        pfContraseña.setPromptText("Apellido paterno");
        Button btnLogin = new Button("Iniciar sesión");

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

        grid.add(new Label("Usuario:"), 0, 0);
        grid.add(tfUsuario, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(pfContraseña, 1, 1);
        grid.add(btnLogin, 1, 2);

        stage.setScene(new Scene(grid, 300, 150));
        stage.setTitle("Login Tutorados");
        stage.show();
    }

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

    private void mostrarVentanaUsuario(String tipoUsuario, String[] datos) {
        if (tipoUsuario.equals("Profesor")) {
            new PanelProfesor(datos).mostrar();
        } else {
            new VentanaAlumno(datos).mostrar(); // Abrir la ventana del alumno
        }
    }

    private void mostrarVentanaAlumno(String[] datosAlumno) {
        // Implementar la ventana para alumnos
    }

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