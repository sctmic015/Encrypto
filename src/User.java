
/**
 * Represents a user and handles logic for UI and server access control
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.awt.EventQueue;
import java.net.*;
import java.io.IOException;

public class User {
    private String username = "";
    private String host;
    private int port;
    private Socket socket;
    private volatile boolean connected = false;
    private UserRead userRead;
    private UserWrite userWrite;
    private String txtMessage = "";
    private String receivedMessage = "";

    /**
     * Constructor to connect user to server
     */
    public User(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Adjust username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the username of user
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Set text message
     */
    public synchronized void setTextMessage(String txtMessage) {
        this.txtMessage = txtMessage;
    }

    /**
     * Get the text message contents
     */
    public synchronized String getTextMessage() {
        return this.txtMessage;
    }

    /**
     * Set received message
     */
    public synchronized void setReceivedMessage(String txtMessage) {
        this.receivedMessage = txtMessage;
    }

    /**
     * Get the received message contents
     */
    public synchronized String getReceivedMessage() {
        return this.receivedMessage;
    }

    /**
     * Get host of user
     */
    public String getHost() {
        return host;
    }

    /**
     * Get port of user
     */
    public int getPort() {
        return port;
    }

    /**
     * Checks if text supplied as argument is a valid username
     * 
     * @param String: Username supplied to check if valid
     */
    public boolean validUsername() {
        if (username.length() > 0 && username.length() < 18)
            begin(); // Execute the connection which will set 'connected' based on this connection
        return connected;
    }

    /**
     * Start the main chat window with this user
     */
    public ChatWindow createChatWindow(User user) {
        return (new ChatWindow(user));
    }

    /**
     * Return connection status of user with server
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Attempts to disconnect the user from the server
     */
    public boolean disconnect() {
        connected = false;
        System.out.println("Disconnected from server");
        return !connected;
    }

    /**
     * User inform method for testing
     */
    public void inform(String text) {
        System.out.println(text);
    }

    /**
     * Begin user execution socket and launch read/write threads
     */
    public void begin() {
        try {
            socket = new Socket(host, port);
            connected = true;
            System.out.println("Connected to server!");

            // Offload the read and write threads
            userRead = new UserRead(socket, this);
            userWrite = new UserWrite(socket, this);
            userRead.start();
            userWrite.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String wantedHost = "localhost";
        int wantedPort = 4444;

        // Check arguments passed, then use those instead
        if (args.length == 1) {
            wantedPort = Integer.parseInt(args[0]);
        } else if (args.length == 2) {
            wantedHost = args[0];
            wantedPort = Integer.parseInt((args[0]));
        } else if (args.length > 2) {
            System.err.println(
                    "Please ensure the proper arguments have been passed. Start a user with no arguments to connect to server on localhost with port 4444. Alternatively, pass a single argument for different port number (still hosted on localhost), or pass two arguments server hostname and port number respectively.");
            System.exit(1);
        }

        User user = new User(wantedHost, wantedPort);

        // User workflow is further handled by the login window
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new LoginWindow(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Display an incoming message to the GUI chat area
    public void addNewMessage(){
        //TODO: Show the new message (stored in receivedMessage) on to GUI
    }

    // Update GUI to show new list of room users
    public void updateRoom(){
        //TODO: Show the new list of users in the room (stored in 
        //string array form in receivedMessage, e.g. "[Dave, Brad, Mike]"")

    }
}
