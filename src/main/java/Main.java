import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws Exception {
        BooleanSearchEngine engine = new BooleanSearchEngine(new File("pdfs"));
        try (ServerSocket serverSocket = new ServerSocket(8989);) {
            System.out.println("Starting server at " + serverSocket.getLocalPort() + "...");
            while (true) {
                try (
                        Socket socket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                ) {
                    String word = in.readLine();
                    out.println(engine.search(word));
                }
            }
        } catch (IOException e) {
            System.out.println("Can't start the server");
            e.printStackTrace();
        }
    }
}