import java.sql.Connection;
import java.sql.DriverManager;

public class conexion {
    // Método para establecer la conexión con la base de datos MariaDB
    public static Connection conectar() {
        try {
            // URL de conexión con los parámetros necesarios
            String url = "jdbc:mariadb://localhost:3306/My_Tutorados"
           + "?useGssapiAuth=false"
           + "&useMysqlMetadata=true";
            
            // Retorna la conexión a la base de datos usando credenciales de root
            return DriverManager.getConnection(url, "root", "suis");
            
        } catch (Exception e) {
            // Manejo de errores en caso de fallo en la conexión
            System.err.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}