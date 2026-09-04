import com.dental.server.HttpServer;

public class Main {
    public static void main(String[] args) {
        try {
            HttpServer.main(args);
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}