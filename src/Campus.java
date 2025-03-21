public class Campus {
    private String nombreCampus;
    private String edificios;

    // Constructor
    public Campus(String nombreCampus, String edificios) {
        this.nombreCampus = nombreCampus;
        this.edificios = edificios;
    }

    // Getters (necesarios para PropertyValueFactory)
    public String getNombreCampus() {
        return nombreCampus;
    }

    public String getEdificios() {
        return edificios;
    }
}