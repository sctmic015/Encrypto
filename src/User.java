
/**
 * Represents a user and handles logic for UI and server access control
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.awt.EventQueue;
import java.net.*;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

public class User {
    private String username = "";
    private String host;
    private int port;
    private Socket socket;
    private volatile boolean connected = false;
    private UserRead userRead;
    private UserWrite userWrite;
    private volatile String txtMessage = "";
    private volatile String receivedMessage = "";
    private ChatWindow chatWindow;
    private volatile ArrayList<String> connectedUsers;
    private KeyPair keyPair;

    /**
     * Constructor to connect user to server
     */
    public User(String host, int port) throws GeneralSecurityException {
        this.host = host;
        this.port = port;
        this.keyPair = userKey();
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
     * Updates the list of connected users 
     */
    // TODO: and their associated public keys
    public void updateConnectedUsers(String connectedList) {
        // Remove the opening and closing brace
        connectedList = connectedList.substring(1, connectedList.length()-1);

        // Split the list and store in a list data structure
        connectedUsers = new ArrayList<>(Arrays.asList(connectedList.split(", ")));

        // Pass the updated list to the GUI
        updateRoomListOfConnectedUsers();
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
    public void createChatWindow(User user) {
        chatWindow = new ChatWindow(user);
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

    private KeyPair userKey() throws GeneralSecurityException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

        return kpGen.generateKeyPair();
    }

    public PublicKey getPublicKey(){
        return this.keyPair.getPublic();
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

    public static void main(String[] args) throws GeneralSecurityException {
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
        chatWindow.updateTxtChat(receivedMessage);
    }

    // Update GUI to show new list of room users
    public void updateRoomListOfConnectedUsers(){
        chatWindow.updateRoomWith(connectedUsers);
    }
}
