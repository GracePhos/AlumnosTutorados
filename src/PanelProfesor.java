import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
@SuppressWarnings("unused")
public class PanelProfesor {
    private String[] datosProfesor;

    public PanelProfesor(String[] datosProfesor) {
        this.datosProfesor = datosProfesor;
    }

    public void mostrar() {
        Stage ventana = new Stage();
        ventana.setTitle("Panel del Profesor - " + datosProfesor[1]);

        // Crear un BorderPane para organizar el layout
        BorderPane root = new BorderPane();

        // Título del panel
        Label titulo = new Label("Bienvenido, " + datosProfesor[1]);
        titulo.setFont(new Font("Arial", 20));
        BorderPane.setAlignment(titulo, Pos.CENTER);
        root.setTop(titulo);

        // Menú lateral con botones
        VBox menuLateral = new VBox(10);
        menuLateral.setPadding(new Insets(10));
        menuLateral.setStyle("-fx-background-color:rgb(168, 168, 168);");

        // Botones con íconos
        Button btnCrearSesion = crearBotonConIcono("Crear Sesión", "");
        Button btnEditarSesion = crearBotonConIcono("Editar Sesión", "");
        Button btnEliminarSesion = crearBotonConIcono("Eliminar Sesión", "");
        Button btnVerSesiones = crearBotonConIcono("Ver Sesiones", "");
        Button btnVerCampus = crearBotonConIcono("Ver Campus", "");
        Button btnVerSemestres = crearBotonConIcono("Ver Semestres", "");
        Button btnVerCarreras = crearBotonConIcono("Ver Carreras", "");

        // Acciones de los botones
        btnCrearSesion.setOnAction(e -> new VentanaCrearSesion(datosProfesor).mostrar());
        btnEditarSesion.setOnAction(e -> new VentanaEditarSesion(datosProfesor).mostrar());
        btnEliminarSesion.setOnAction(e -> new VentanaEliminarSesion(datosProfesor).mostrar());
        btnVerSesiones.setOnAction(e -> new VentanaVerSesiones(datosProfesor).mostrar());
        btnVerCampus.setOnAction(e -> new VentanaVerCampus().mostrar());
        btnVerSemestres.setOnAction(e -> new VentanaVerSemestres().mostrar());
        btnVerCarreras.setOnAction(e -> new VentanaVerCarreras().mostrar());

        // Agregar botones al menú lateral
        menuLateral.getChildren().addAll(
            btnCrearSesion, btnEditarSesion, btnEliminarSesion,
            btnVerSesiones, btnVerCampus, btnVerSemestres, btnVerCarreras
        );

        // Colocar el menú lateral en el BorderPane
        root.setLeft(menuLateral);

        // Configurar la escena y mostrar la ventana
        Scene scene = new Scene(root, 800, 600);
        ventana.setScene(scene);
        ventana.show();
    }

    // Método para crear botones con íconos
    private Button crearBotonConIcono(String texto, String nombreIcono) {
        Button boton = new Button(texto);
        try {
            Image icono = new Image(getClass().getResourceAsStream("/icons/" + nombreIcono));
            boton.setGraphic(new ImageView(icono));
        } catch (Exception e) {
            System.out.println("No se pudo cargar el ícono: " + nombreIcono);
        }
        boton.setStyle("-fx-background-color:rgb(66, 179, 145); -fx-text-fill: white; -fx-font-size: 14px;");
        boton.setMaxWidth(Double.MAX_VALUE);
        return boton;
    }
}