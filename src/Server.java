import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.*;

/**
 * Server for communication to Encrypto clients
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

public class Server {
    private int port;
    // Store a set of unique connected usernames and rooms for the server to track
    private ArrayList<String> usernames = new ArrayList<>();
    private Set<Room> rooms = new HashSet<>();

    /**
     * Server constructor with port number supplied
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * Check if room with ID supplied exists
     */
    public boolean containsRoom(String roomID) {
        for (Room r : rooms) {
            if (roomID.equals(r.getRoomID()))
                return true;
        }

        return false;
    }

    /**
     * Attempts to add a username to the server if it's available.
     * The method returns whether a user was successfully added.
     */
    public synchronized boolean addUser(String username) {
        /* if (!usernames.contains(username)) {
            usernames.add(username);
            return true;
        }
        return false; */
        return usernames.add(username);
    }

    /**
     * Attempts to remove a username from the server.
     * The method returns whether a user was successfully removed.
     */
    public synchronized boolean removeUser(String username) {
        return usernames.remove(username);
    }

    //Allows users to create rooms
    // Should check that the given room doesn't already exist
    public void addRoom(Room newRoom){
        rooms.add(newRoom);
    }

    //gets the room associated with the given ID. If this doesn't exist, it will return null. Might need handeling
    public Room getRoom(String roomID){
        Room returnRoom = null;
        for (Room r : rooms) {
            if (roomID.equals(r.getRoomID()))
                returnRoom = r;
        }
        return returnRoom;
    }

    /**
     * Starts server listening for connections
     */
    private void beginServer() {
        try (ServerSocket ss = new ServerSocket(port)) {
            // Inform console that server successfully started
            System.out.println("Encrypto server started on port: " + port);

            // Accept client connections and serve forever
            while (true) {
                Socket socket = ss.accept();

                ServerThread serverThread = new ServerThread(socket, this);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Allows outputing messages to console for server logging
     */
    public void inform(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        // Error checking for abnormal arguments
        if (args.length > 1) {
            System.err.println(
                    "Start server with no arguments to use default port (4444) else please supply one argument for the port number required.");
            System.exit(1);
        }

        // Wanted port defaults to 4444, check if user supplied port and construct
        // server
        int wantedPort = 4444;
        if (args.length == 1) {
            wantedPort = Integer.parseInt(args[0]);
        }

        Server server = new Server(wantedPort);
        server.beginServer();
    }
}
