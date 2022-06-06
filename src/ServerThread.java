
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

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
            //input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //userPublicKeyInput = new ObjectInputStream(socket.getInputStream());
            //userCertificateOutput = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            // ServerThread is created on login attempt so username will be sent. Ask the
            // server to add this username
            username = (String) input.readObject();
            System.out.println(username);
            userPublicKey = (PublicKey) input.readObject();
            //server.addUserCertificates(username, "SHA256WithRSA", userPublicKey);
            X509Certificate userCertificate = server.createEndEntity(username, "SHA256WithRSA", userPublicKey);
            this.userCertificate = userCertificate;
            server.addUserCertificate(userCertificate);
            // --- DEBUG STATEMENT ---
            server.inform(userCertificate.toString());
            //server.printUserCertificates();
            //output.write("Message");
            //output.write(userCertificate.toString());
            System.out.println("Eish1");
            System.out.println(userCertificate instanceof X509Certificate);
            output.writeObject(userCertificate);
            System.out.println("Eish2");
            X509Certificate serverCertificate = server.getServerCertificate();
            output.writeObject(serverCertificate);
            //output.flush();
            //System.out.println(userPublicKey);
            // --- DEBUG STATEMENT ---
            server.inform(userPublicKey.toString());
            if (server.addUser(username)) {
                // User was successfully added and is connected in this thread; let the server
                // output this news to console
                server.inform(username + " is connected!");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get username of user
     */
    public String getUsername() {
        return username;
    }

    public X509Certificate getUserCertificate() {
        return userCertificate;
    }

    /**
     * Returns the public key of the user connected to this server thread
     */
    public PublicKey getPublicKey() {
        return userPublicKey;
    }

    /**
     * Send message from server to user connected on this server thread
     */
    public void sendMsg(String msg) {
        try {
            output.writeObject(msg + "\n");
            //output.writeObject("\n");
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(X509Certificate userCertificate){
        try{
            output.writeObject(userCertificate);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ArrayList<X509Certificate> keyRing){
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
                server.inform(receivedText);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Check for control commands, and handles command approriately
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

            switch (command) {
                case "LOGOUT":
                    // Logout command received, remove user
                    if (server.removeUser(username)) {
                        leaveCurRoom();
                        // Send message to user to shut down connection
                        try {
                            output.writeObject(":SHUTDOWN:" + "\n");
                            //output.writeObject("\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        server.inform(username + " has disconnected");
                        running = false;
                        break;
                    }
                case "START":
                    if (!server.containsRoom(roomID)) {
                        try {
                            output.writeObject(":VALID:" + "\n");
                            //output.writeObject("\n");
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
                        }
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.writeObject(":INVALID:" + "\n");
                            //output.writeObject("\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        server.inform(username + " tried to start room with ID = " + roomID
                                + ". ID is already in use so room was not started...");
                    }
                    break;
                case "JOIN":
                    if (server.containsRoom(roomID) && server.validRoomPassCombo(roomID, pass)) {
                        try {
                            output.writeObject(":VALID:" + "\n");
                            //output.writeObject("\n");
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
                        }
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.writeObject(":INVALID:" + "\n");
                            //output.writeObject("\n");
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        server.inform(username + " tried to join room with ID = " + roomID
                                + ". Either the room does not exist or the password was incorrect, so the room was not joined...");
                    }
                    break;
                case "MESSAGE":
                    if (server.containsRoom(roomID)) {
                        msgRoom(roomID, ":MESSAGE:" + message); // TODO: Use public key object being sent to message end-to-end 
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
    public void startRoom(String roomID, String password) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        server.addRoom(new Room(roomID, password));
        joinRoom(roomID);
        curRoomID = roomID;
    }

    /**
     * Add the user to the given room and inform all users in that room of the event
     */
    public void joinRoom(String roomID) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        // If a user is already in a room, remove them from the old room
        leaveCurRoom();
        Room newRoom = server.getRoom(roomID);
        newRoom.addUser(this);  // Adds server thread to room
        System.out.println("UserNames: " + newRoom.getUsernames());
        msgRoom(roomID, ":UPDATE:" + newRoom.getUsernames()); // Get all usernames in room. BroadCast Message
        msgRoom(roomID, newRoom.getUserKeys());    // Get userkeys gets all keys in room. Get all keys in room broadcast message
        System.out.println(newRoom.getUserKeys().size());
        curRoomID = roomID;
    }

    /**
     * Remove user from their current room and inform all users in that room of the
     * event
     */
    public void leaveCurRoom() {
        if (!(curRoomID == null)) {
            Room room = server.getRoom(curRoomID);
            room.removeUser(this);
            msgRoom(curRoomID, ":UPDATE:" + room.getUsernames());
        }
    }

    /**
     * Broadcast message to all users in a room
     */
    public void msgRoom(String roomID, String msg) {
        server.getRoom(roomID).broadcastMessage(msg);
    }

    public void msgRoom(String roomID, X509Certificate userCertificate){
        server.getRoom(roomID).broadcastMessage(userCertificate);
        System.out.println("Certificate Broadcasted");
    }

    public void msgRoom(String roomID, ArrayList<X509Certificate> keyRing){
        server.getRoom(roomID).broadcastMessage(keyRing);
    }
    /**
     * Send message to user with specific public key
     */
    public void msgUser(String roomID, PublicKey pubKey, String cipherText) {
        server.getRoom(roomID).msgUser(pubKey, cipherText);
    }
}
