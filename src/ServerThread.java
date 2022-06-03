
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
    private BufferedWriter output;
    private String username;

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
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // ServerThread is created on login attempt so username will be sent. Ask the
            // server to add this username
            username = input.readLine();
            if (!server.addUser(username)) {
                // socket.close(); // Close connection if user can't be added
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
        String receivedText = "";

        // Continuously read the inputted data and act accordingly
        while (true) {

            // Receive the text from user
            try {
                receivedText = input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Check for control command, if none of the control commands sent, then pass
            // message to members of chat room
            if (receivedText.equals(":LOGOUT:")) {
                // Logout command received, remove user
                if (server.removeUser(username)) {
                    server.inform(username + " has disconnected");
                    break;
                }
            } else if (receivedText.equals(":START:")) {
                // TODO: server.startRoom(id);
                // server.joinRoom(id, username);
            } else if (receivedText.equals(":JOIN:")) {
                // TODO: server.joinRoom(id, username);
            } else {
                // No control command, so pass message to room participants
                // TODO: server.msgRoom(receivedText);
                server.inform(receivedText);
            }
        }
    }
}
