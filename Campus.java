public class Campus {
    private String nombreCampus;
    private String edificios;
    private String idCampus;

    // Constructor
    public Campus(String idCampus, String nombreCampus, String edificios) {
        this.idCampus = idCampus;
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

    public String getIdCampus() {
        return idCampus;
    }

}