
/**
 * Threaded server instance handling a client to allow multiple user connection on server
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

//import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.bouncycastle.operator.OperatorCreationException;

import java.net.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerThread extends Thread {
    private Socket socket;
    private Server server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private ObjectInput userPublicKeyInput;
    private ObjectOutput userCertificateOutput;
    private String username;
    private String curRoomID;
    private PublicKey userPublicKey;
    private X509Certificate userCertificate;
    private KeyRingObject keyRingObject;

    /**
     * Constructs a server thread, setting up the input and output mechanisms.
     * User is also attempted to be added and connection is closed if this fails.
     */
    public ServerThread(Socket socket, Server server) throws CertificateException, OperatorCreationException {
        this.socket = socket;
        this.server = server;
        curRoomID = null;

        // Setup input/output handlers
        try {
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            // Retrieve username and public key sent by user upon login and corresponding certificate
            username = (String) input.readObject();
            userPublicKey = (PublicKey) input.readObject();
            X509Certificate userCertificate = server.createEndEntity(username, "SHA256WithRSA", userPublicKey);
            this.userCertificate = userCertificate;
            server.addUserCertificate(userCertificate);
            this.keyRingObject = new KeyRingObject(username, this.userCertificate);

            // Send user and server certificates back to user
            output.writeObject(userCertificate);
            X509Certificate serverCertificate = server.getServerCertificate();
            output.writeObject(serverCertificate);

            // Add username to server and print debug statement
            if (server.addUser(username)) {
                // --- DEBUG STATEMENT ---
                server.inform(username + " is connected!");
            }
            
            // --- DEBUG STATEMENT ---
            server.inform("The user's certificate has been created as follows:");
            server.inform(userCertificate.toString());
            server.inform("The user's public key is as follows:");
            server.inform(userPublicKey.toString());
            
        } catch (IOException | ClassNotFoundException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get username of user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get certificate of user
     */
    public X509Certificate getUserCertificate() {
        return userCertificate;
    }

    /**
     * Returns the public key of the user connected to this server thread
     */
    public PublicKey getPublicKey() {
        return userPublicKey;
    }

    public KeyRingObject getKeyRingObject(){
        return this.keyRingObject;
    }
    /**
     * Send message from server to user connected on this server thread
     */
    public void sendMsg(String msg) {
        try {
            output.writeObject(msg + "\n");
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send certifiacte from server to user connected on this server thread
     */
    public void sendMsg(X509Certificate userCertificate){
        try{
            output.writeObject(userCertificate);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send ArrayList of Certificates of users connected to room from server to user connected on this server thread
     */
    /**
    public void sendMsg(ArrayList<X509Certificate> keyRing){
        try{
            output.writeObject(keyRing);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */
    public void sendMsg(ArrayList<KeyRingObject> keyRing){
        try{
            output.writeObject(keyRing);
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
                receivedText = (String )input.readObject();

                // --- DEBUG STATEMENT ---
                server.inform("The server received the following message from the user:");
                server.inform(receivedText);

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Tokenise incoming message
            String[] controlCommands = receivedText.split(":", 5);
            
            // Assign the split variables appropriately
            String message = "";
            String command = "";
            String roomID = "";
            String pass = "";
            if (controlCommands.length > 1) {
                command = controlCommands[1]; // Item zero is throwaway
                if (controlCommands.length > 3) {
                    roomID = controlCommands[2];
                    pass = controlCommands[3];
                }
                message = controlCommands[controlCommands.length - 1];
            }

            // Handle incoming command approriately
            switch (command) {
                case "LOGOUT":
                    // Logout command received, remove user
                    if (server.removeUser(username)) {
                        try {
                            leaveCurRoom();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Send message to user to shut down connection
                        try {
                            output.writeObject(":SHUTDOWN:" + "\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        running = false;
                        
                        // --- DEBUG STATEMENT ---
                        server.inform(username + " has disconnected");

                        break;
                    }
                case "START":
                    if (!server.containsRoom(roomID)) {
                        try {
                            output.writeObject(":VALID:" + "\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            startRoom(roomID, pass);
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (KeyStoreException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.writeObject(":INVALID:" + "\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // --- DEBUG STATEMENT ---
                        server.inform(username + " tried to start room with ID = " + roomID
                                + ". ID is already in use so room was not started...");
                    }
                    break;
                case "JOIN":
                    if (server.containsRoom(roomID) && server.validRoomPassCombo(roomID, pass)) {
                        try {
                            output.writeObject(":VALID:" + "\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            joinRoom(roomID);
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (KeyStoreException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.writeObject(":INVALID:" + "\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // --- DEBUG STATEMENT ---
                        server.inform(username + " tried to join room with ID = " + roomID
                                + ". Either the room does not exist or the password was incorrect, so the room was not joined...");
                    }
                    break;
                case "MESSAGE":
                    if (server.containsRoom(roomID)) {
                        //System.out.println("Message Received: " + message);
                        try {
                            msgRoom(roomID, ":MESSAGE:" + message); // TODO: Use public key object being sent to message end-to-end
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // TODO: msgUser(roomID, pubKey, ":MESSAGE:" + message);
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
     * Add a new room to the server and add the creator
     */
    public void startRoom(String roomID, String password) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InterruptedException {
        server.addRoom(new Room(roomID, password));
        joinRoom(roomID);
        curRoomID = roomID;
    }

    /**
     * Add the user to the given room and inform all users in that room of the event
     */
    public void joinRoom(String roomID) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InterruptedException {
        // If a user is already in a room, remove them from the old room
        leaveCurRoom();

        // Adds server thread to room
        Room newRoom = server.getRoom(roomID);
        newRoom.addUser(this);
        curRoomID = roomID;

        // Broadcast all usernames and keys in the room to present users
        msgRoom(roomID, ":UPDATE:" + newRoom.getUsernames()); 
        //msgRoom(roomID, newRoom.getUserKeys());
        msgRoom(roomID, newRoom.getUserKeyRingObject());
    }

    /**
     * Remove user from their current room and inform all users in that room of the
     * event
     */
    public void leaveCurRoom() throws InterruptedException {
        if (!(curRoomID == null)) {
            Room room = server.getRoom(curRoomID);
            room.removeUser(this);
            msgRoom(curRoomID, ":UPDATE:" + room.getUsernames());
            msgRoom(curRoomID, room.getUserKeyRingObject());
        }
    }

    /**
     * Broadcast a message to all users in a room
     */
    public void msgRoom(String roomID, String msg) throws InterruptedException {
        server.getRoom(roomID).broadcastMessage(msg);
    }

    public void msgRoom2(String roomID, String msg){
        String[] incomingSplit = msg.split("]");
        String header = incomingSplit[0] + " ";
        String initialMessage = incomingSplit[1].trim();
        String[] messageSplit = initialMessage.split(">");
        ArrayList<String> out = new ArrayList<>();
        out.add(header);
        for (int i = 1; i < messageSplit.length; i ++){
            String[] userSplit = messageSplit[i].split("<");
            String userName = userSplit[0];
            String encryptedMessage = userSplit[1];
            out.add(userName);
            out.add(encryptedMessage);
        }
        server.getRoom(roomID).broadcastMessageArray(out);
    }

    /**
     * Broadcast a certificate to all users in a room
     */
    /**
    public void msgRoom(String roomID, X509Certificate userCertificate){
        server.getRoom(roomID).broadcastMessage(userCertificate);
        System.out.println("Certificate Broadcasted");
    }
     */

    /**
     * Broadcast a keyRing to all users in a room
     */
    /**
    public void msgRoom(String roomID, ArrayList<X509Certificate> keyRing){
        server.getRoom(roomID).broadcastMessage(keyRing);
    }
    */

    public void msgRoom(String roomID, ArrayList<KeyRingObject> keyRing){
        server.getRoom(roomID).broadcastMessage(keyRing);
    }
    /**
     * Send message to user with specific public key
     */
    public void msgUser(String roomID, PublicKey pubKey, String cipherText) {
        server.getRoom(roomID).msgUser(pubKey, cipherText);
    }
}
