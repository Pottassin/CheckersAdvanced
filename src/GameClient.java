import java.io.*;
import java.net.*;

public class GameClient {
    public static void main(String[] args) throws IOException {
        String serverIP = "192.168.137.1"; // replace with server laptop’s IP
        int port = 12345;

        Socket socket = new Socket(serverIP, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // send a message
        out.println("Hello from client!");
        System.out.println("Server replied: " + in.readLine());

        socket.close();
    }
}
