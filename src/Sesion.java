public class Sesion {
    private String nombreAlumno;
    private String fecha;
    private String hora;
    private String nombreSala;
    private String resumen;

    // Constructor
    public Sesion(String nombreAlumno, String fecha, String hora, String nombreSala, String resumen) {
        this.nombreAlumno = nombreAlumno;
        this.fecha = fecha;
        this.hora = hora;
        this.nombreSala = nombreSala;
        this.resumen = resumen;
    }

    // Getters (necesarios para PropertyValueFactory)
    public String getNombreAlumno() {
        return nombreAlumno;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getNombreSala() {
        return nombreSala;
    }

    public String getResumen() {
        return resumen;
    }
}