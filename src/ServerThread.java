
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

    public void sendMsg(String msg) {
        try {
            output.write(msg);
            output.newLine();
            output.flush();
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
                server.inform(receivedText);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Check for control commands, and handles command approriately
            String[] input = receivedText.split(":");
            switch (input[1]) {
                case "LOGOUT":
                    // Logout command received, remove user
                    if (server.removeUser(username)) {
                        // Send message to user to shut down connection
                        try {
                            output.write(":SHUTDOWN:");
                            output.newLine();
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        server.inform(username + " has disconnected");
                        break;
                    }
                case "START":
                    startRoom(input[2], input[3]);
                    break;
                case "JOIN":
                    joinRoom(input[2], input[3]);
                    break;
                case "MESSAGE":
                    msgRoom(input[2], input[3]);
            }
            // if (receivedText.equals(":LOGOUT:")) {
            // // Logout command received, remove user
            // if (server.removeUser(username)) {
            // server.inform(username + " has disconnected");
            // break;
            // }
            // } else if (receivedText.equals(":START:")) {
            // // TODO: server.startRoom(id);
            // // server.joinRoom(id, username);
            // } else if (receivedText.equals(":JOIN:")) {
            // // TODO: server.joinRoom(id, username);
            // } else {
            // // No control command, so pass message to room participants
            // // TODO: server.msgRoom(receivedText);
            // server.inform(receivedText);
            // }
        }
    }

    // Method to start new Room
    public void startRoom(String roomID, String username) { // Make this boolean to check that no room with existing ID
                                                            // exists
        // Check that room name not taken
        server.addRoom(new Room(roomID));
        joinRoom(roomID, username);
        // server.inform(server.getRoom(roomID).toString());
    }

    // Method to join a Room
    public void joinRoom(String roomID, String username) {
        server.getRoom(roomID).addUser(username, this);
    }

    // Method to leave a room
    public void leaveRoom(String roomID, String username) {
        Room room = server.getRoom(roomID);
        room.removeUser(username);
        room.removeThread(this);
    }

    // Method to broadcast message to all users in a room
    public void msgRoom(String roomID, String msg) {
        server.getRoom(roomID).broadcastMessage(msg);
    }
}
