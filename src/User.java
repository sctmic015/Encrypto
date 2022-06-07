
/**
 * Represents a user and handles logic for UI and server access control
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

//import org.bouncycastle.crypto.ec.ECNewPublicKeyTransform;
//import org.bouncycastle.jcajce.provider.asymmetric.X509;
//import org.bouncycastle.openpgp.PGPKeyRing;
//import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

import java.awt.EventQueue;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.crypto.params.KeyParameter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
    private X509Certificate userCertificate;
    private X509Certificate serverCertificate;
    public KeyStore keyStore;
    public ArrayList<X509Certificate> keyRing;

    /**
     * Constructor to connect user to server
     */
    public User(String host, int port) throws GeneralSecurityException, IOException {
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
        connectedList = connectedList.substring(1, connectedList.length() - 2);

        // Split the list and store in a list data structure
        connectedUsers = new ArrayList<>(Arrays.asList(connectedList.split(", ")));

        // Pass the updated list to the GUI
        updateRoomListOfConnectedUsers();
    }

    /**
     * Updates the list of connected users public key certificates
     */
    public void updateConnectedUsersKeys(ArrayList<X509Certificate> keys) throws KeyStoreException {
        this.keyRing = keys;
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
     */
    public boolean validUsername() throws ClassNotFoundException {
        if (username.length() > 0 && username.length() < 18) {
            begin(); // Execute the connection which will set 'connected' based on this connection
        }
        return connected;
    }

    /**
     * Send message to user that message sent to server is deemed invalid
     */
    public void warnFailure() {
        chatWindow.warnFailure();
    }

    /**
     * Sets up the GUI for chatting with new instance
     */
    public void setupChat() {
        chatWindow.setupChat();
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
        // --- DEBUG STATEMENT ---
        inform("Disconnected from server");
        return !connected;
    }

    /**
     * User inform method for testing
     */
    public void inform(String text) {
        System.out.println("> " + text);
    }

    /**
     * For each user connected on the keyRing, encrypt the message and send tell the server to send the encrypted message
     * @throws Exception
     */
    public synchronized void sendMessage(String messageHeader, String messageContents) throws Exception {
        for (X509Certificate receiverCertificate : keyRing) {
            PublicKey pubKey = receiverCertificate.getPublicKey();

            String cipherText = PGPUtil.sender(messageContents, keyPair, pubKey); // Cipher the message contents by PGP
            String encodedPubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());

            setTextMessage(messageHeader + encodedPubKey + ":" + cipherText);
        }
    }

    /**
     * Returns the plaintext from a PGP received ciphertext
     * @throws Exception
     */
    public String getPlainText(String cipherText) throws Exception {
        // Separate content strings receievd for ciphertext msg and sender pub key
        String msg = "";
        String senderPublicKey = "";
        cipherText = cipherText.substring(0, cipherText.lastIndexOf(":"));
        int sep = cipherText.lastIndexOf(":");
        msg = cipherText.substring(0, sep);
        senderPublicKey = cipherText.substring(sep+1, cipherText.length());

        // Go through the list of keys on the keyring and choose the matching certificate to be used as the sender's certificate
        PublicKey senderpubKey = userCertificate.getPublicKey(); // Initialised to itself
        for (X509Certificate cert : keyRing) {
            String encodedPubKey = Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
            if (senderPublicKey.equals(encodedPubKey)) {
                senderpubKey = cert.getPublicKey();
            }
        }

        return PGPUtil.receiver(cipherText, senderpubKey, keyPair.getPublic(), keyPair.getPrivate());
    }

    /**
     * Hashes and encrypts the password then returns this version in string form
     */
    public String createHiddenPassword(String text) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        // 1: Create a hash of the password
        String hashPassword= PGPUtil.hashSHA(text);
        // 2: Encrypt the hashsed password with the CA's public key
        String encryptPassword = PGPUtil.asymmetricEncrypt(serverCertificate.getPublicKey(), null, hashPassword, 1);

        return encryptPassword;
    }

    /**
     * Generates and returns the user's key pair
     */
    private KeyPair userKey() throws GeneralSecurityException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

        return kpGen.generateKeyPair();
    }

    /**
     * Get the user key pair
     */
    public KeyPair getKeyPair(){
        return this.keyPair;
    }

    /**
     * Get the user's public key
     */
    public PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    /**
     * Sets the user certificate
     */
    public void setCertificate(X509Certificate userCertificate){
        this.userCertificate = userCertificate;
    }

    /**
     * Sets and stores the servers certificate
     */
    public void setServerCertificate(X509Certificate certificate){
        this.serverCertificate = certificate;
    }

    /**
     * Begin user execution socket and launch read/write threads
     */
    public void begin() throws ClassNotFoundException {
        try {
            socket = new Socket(host, port);
            connected = true;
            // --- DEBUG STATEMENT ---
            inform("Connected to server!");

            // Offload the read and write threads
            userWrite = new UserWrite(socket, this);
            userWrite.start();
            userRead = new UserRead(socket, this);
            userRead.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        String wantedHost = "localhost";
        int wantedPort = 4444;

        // Check arguments passed, then use those instead
        if (args.length == 1) {
            wantedPort = Integer.parseInt(args[0]);
        } else if (args.length == 2) {
            wantedHost = args[0];
            wantedPort = Integer.parseInt((args[0]));
        } else if (args.length > 2) {
            // --- DEBUG STATEMENT ---
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

    /**
     * Display an incoming message to the GUI chat area
     */
    public void addNewMessage() {
        chatWindow.updateTxtChat(receivedMessage);
    }

    /**
     * Update GUI to show new list of room users
     */
    public void updateRoomListOfConnectedUsers() {
        chatWindow.updateRoomWith(connectedUsers);
    }
}
