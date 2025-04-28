public class App {
    // La clase App solo sirve como punto de entrada para la aplicación,
    // ya que por alguna razón, el main de LoginApp no se ejecuta sin este main.
    public static void main(String[] args) {
        // Lanza la aplicación LoginApp
        LoginApp.launch(LoginApp.class, args);
    }
}
