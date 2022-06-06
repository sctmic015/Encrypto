
/**
 * Threaded server instance handling a client to allow multiple user connection on server
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.bouncycastle.operator.OperatorCreationException;

import java.net.*;
import java.io.*;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ServerThread extends Thread {
    private Socket socket;
    private Server server;
    private BufferedReader input;
    private BufferedWriter output;
    private ObjectInput userPublicKeyInput;
    private ObjectOutput userCertificateOutput;
    private String username;
    private String curRoomID;
    private PublicKey userPublicKey;

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
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            userPublicKeyInput = new ObjectInputStream(socket.getInputStream());
            userCertificateOutput = new ObjectOutputStream(socket.getOutputStream());

            // ServerThread is created on login attempt so username will be sent. Ask the
            // server to add this username
            username = input.readLine();
            // System.out.println(username);
            userPublicKey = (PublicKey) userPublicKeyInput.readObject();
            //server.addUserCertificates(username, "SHA256WithRSA", userPublicKey);
            X509Certificate userCertificate = server.createEndEntity(username, "SHA256WithRSA", userPublicKey);
            server.addUserCertificate(userCertificate);
            // --- DEBUG STATEMENT ---
            server.inform(userCertificate.toString());
            //server.printUserCertificates();
            //output.write("Message");
            //output.write(userCertificate.toString());
            userCertificateOutput.writeObject(userCertificate);
            userCertificateOutput.flush();
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
                    if (!server.containsRoom(roomID)) {
                        try {
                            output.write(":VALID:");
                            output.newLine();
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        startRoom(roomID, pass);
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.write(":INVALID:");
                            output.newLine();
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
                            output.write(":VALID:");
                            output.newLine();
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        joinRoom(roomID);
                    } else {
                        // Error setting up room, tell user there was an invalid operation and inform
                        // the server of the mishap
                        try {
                            output.write(":INVALID:");
                            output.newLine();
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        server.inform(username + " tried to join room with ID = " + roomID
                                + ". Either the room does not exist or the password was incorrect, so room was not joined...");
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
    public void startRoom(String roomID, String password) {
        server.addRoom(new Room(roomID, password));
        joinRoom(roomID);
        curRoomID = roomID;
    }

    /**
     * Add the user to the given room and inform all users in that room of the event
     */
    public void joinRoom(String roomID) {
        // If a user is already in a room, remove them from the old room
        leaveCurRoom();
        Room newRoom = server.getRoom(roomID);
        newRoom.addUser(this);
        msgRoom(roomID, ":UPDATE:" + newRoom.getUsernames());
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

    /**
     * Send message to user with specific public key
     */
    public void msgUser(String roomID, PublicKey pubKey, String cipherText) {
        server.getRoom(roomID).msgUser(pubKey, cipherText);
    }
}
