
/**
 * Threaded server instance handling a client to allow multiple user connection on server
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
    private Socket socket;
    private Server server;
    private BufferedReader input;
    private PrintWriter output;

    /**
     * Constructs a server thread, setting up the input and output mechanisms.
     * User is also attempted to be added and connection is closed if this fails.
     */
    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;

        // Setup input/output handlers
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // ServerThread is created on login attempt so username will be sent. Ask the
            // server to add this username
            String username = input.readLine();
            if (!server.addUser(username)) {
                socket.close(); // Close connection if user can't be added
            } else {
                // User was successfully added and is connected in this thread; let the server
                // output this news to console
                server.inform(username + " is connected!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread run method which handles input and output
     */
    @Override
    public void run() {
        // TODO: Implement the reading and writing
    }
}
