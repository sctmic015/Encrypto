
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
    private String curRoomID; 

    /**
     * Constructs a server thread, setting up the input and output mechanisms.
     * User is also attempted to be added and connection is closed if this fails.
     */
    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        curRoomID = null;

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
     * Get username of user
     */
    public String getUsername() {
        return username;
    }

    // PGP starts here
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
                        leaveRoom();
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
                    if (validID(roomID)) {
                        startRoom(roomID);
                    } else {
                        System.err.println("Invalid roomID for starting room...");
                    }
                    break;
                case "JOIN":
                    if (validID(roomID) && server.containsRoom(roomID)) {
                        joinRoom(roomID);
                    } else {
                        System.err.println("Invalid roomID for joining room...");
                    }
                    break;
                case "MESSAGE":
                    if (validID(roomID) && server.containsRoom(roomID)) {
                        msgRoom(roomID, ":MESSAGE:" + message);
                    } else {
                        System.err.println("Invalid roomID to send message...");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Verifies that the supplied ID is valid
     */
    public boolean validID(String ID) {
        return (ID.length() > 0);
    }

    /**
     * Add a new room to the server and add the creator
     */
    public void startRoom(String roomID) {
        // Make this boolean to check that no room with existing ID exists
        // Check that room name not taken
        server.addRoom(new Room(roomID));
        joinRoom(roomID);
    }

    /**
     * Add the user to the given room and inform all users in that room of the event
     */
    public void joinRoom(String roomID) {
        //If a user is already in a room, remove them from the old room
        if (!(curRoomID == null)){
            leaveRoom();
        }
        Room newRoom = server.getRoom(roomID);
        newRoom.addUser(this);
        msgRoom(roomID, ":UPDATE:" + newRoom.getUsernames());
    }

    /**
     * Remove user from their current room and inform all users in that room of the event
     */
    public void leaveRoom() {
        Room room = server.getRoom(curRoomID);
        room.removeUser(this);
        msgRoom(curRoomID, ":UPDATE:" + room.getUsernames());
    }

    /**
     * Broadcast message to all users in a room
     */
    public void msgRoom(String roomID, String msg) {
        server.getRoom(roomID).broadcastMessage(msg);
    }
}
