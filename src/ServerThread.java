
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

    public String getUsername() {
        return username;
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
        boolean running = true;

        // Continuously read the inputted data and act accordingly
        while (running) {
            // Receive the text from user
            try {
                receivedText = input.readLine();
                server.inform(receivedText);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Check for control commands, and handles command approriately
            String[] controlCommands = receivedText.split(":", 4);
            // Assign the split variables appropriately
            String message = "";
            String command = "";
            String roomID = "";
            if (controlCommands.length > 0) {
                command = controlCommands[1]; // Item zero is throwaway
                if (controlCommands.length > 2) {
                    roomID = controlCommands[2];
                }
                message = controlCommands[controlCommands.length - 1];
            }

            switch (command) {
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
                        running = false;
                        break;
                    }
                case "START":
                    startRoom(roomID);
                    break;
                case "JOIN":
                    joinRoom(roomID);
                    break;
                case "MESSAGE":
                    msgRoom(roomID, message);
                    break;
                default:
                    break;
            }
        }
    }

    // Method to start new Room
    public void startRoom(String roomID) {
        // Make this boolean to check that no room with existing ID exists
        // Check that room name not taken
        server.addRoom(new Room(roomID));
        joinRoom(roomID);
        // server.inform(server.getRoom(roomID).toString());
    }

    // Method to join a Room
    public void joinRoom(String roomID) {
        server.getRoom(roomID).addUser(this);
    }

    // Method to leave a room
    public void leaveRoom(String roomID) {
        server.getRoom(roomID).removeUser(this);
    }

    // Method to broadcast message to all users in a room
    public void msgRoom(String roomID, String msg) {
        server.getRoom(roomID).broadcastMessage(msg);
    }
}
