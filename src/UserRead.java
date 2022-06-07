
/**
 * The user reader thread which reads a server input and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

import java.net.*;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

public class UserRead extends Thread {
    private Socket socket;
    private User user;
    private ObjectInputStream input;
    private X509Certificate userCertificate;
    private X509Certificate serverCertificate;

    /**
     * Constructor
     */
    public UserRead(Socket socket, User user) throws ClassNotFoundException {
        this.socket = socket;
        this.user = user;

        // Setup the input handler
        try {
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded run method
     */
    @Override
    public void run() {
        String line;
        X509Certificate certificate;
        int count = 0;
        while (socket.isConnected() && user.isConnected()) {
            try {
                // Reads in and stores users certificate signed by the server
                Object tempInput = input.readObject();
                if (tempInput instanceof X509Certificate && count == 0){
                    X509Certificate tempCertificate = (X509Certificate) tempInput;
                    user.setCertificate(tempCertificate);
                    count ++;
                }
                // Reads in and stores the servers certificate
                else if (tempInput instanceof X509Certificate && count == 1){
                    X509Certificate tempCertificate = (X509Certificate) tempInput;
                    serverCertificate = tempCertificate;
                    user.setServerCertificate(serverCertificate);
                    count ++;
                }
                // Reads in Certificates of all Users in Current Room. Acts as a public key ring
                else if (tempInput instanceof ArrayList<?>){
                    ArrayList<KeyRingObject> tempKeyStore = (ArrayList<KeyRingObject>) tempInput;
                    user.updateConnectedUsersKeys(tempKeyStore);
                }
                // Handles all message passing
                else {
                    line = (String) tempInput;
                    // Using control commands, handle incoming message appropriately
                    String[] controlCommands = line.split(":", 3);

                    // --- DEBUG STATEMENT ---
                    user.inform("Message received from server: " + line);

                    // Assign the split variables appropriately
                    String command = "";
                    String contents = ""; // either msg or username set
                    command = controlCommands[1]; // Item zero is throwaway
                    contents = controlCommands[2];
                    System.out.println("Contents are: " + contents);

                    if (command.equals("SHUTDOWN")) {
                        // If shutdown message has been called, then break
                        // Because readline is a blocking call, we need to receive a message to allow
                        // this thread to safely close
                        break;
                    } else if (command.equals("UPDATE")) {
                        user.updateConnectedUsers(contents);
                    } else if (command.equals("VALID")) {
                        // Server accepts message from user, setup chat instance
                        user.setupChat();
                    } else if (command.equals("INVALID")) {
                        user.warnFailure();
                    } else if (command.equals("MESSAGE")) {
                        String[] fromUSer = getUserMessage(contents, user.getUsername());
                        String senderUserName = fromUSer[0].trim();
                        for (int i = 0; i < user.getKeyRing().size(); i ++){
                            if (user.getKeyRing().get(i).matchAlias(senderUserName)) {
                                PublicKey senderPublicKey = user.getKeyRing().get(i).getPublicKey();
                                String userMessage = fromUSer[1];
                                System.out.println("UserMessage: " + userMessage);
                                userMessage = PGPUtil.receiver(userMessage, senderPublicKey, user.getKeyPair().getPublic(), user.getKeyPair().getPrivate());
                                System.out.println("Decrypted UserMessage: " + userMessage);
                                user.setReceivedMessage(userMessage);
                                user.addNewMessage();
                            }
                        }
                    }

                }

            } catch (IOException | ClassNotFoundException | KeyStoreException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // User no longer connected so close socket
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getUserMessage(String inputMessage, String Username){
        System.out.println(inputMessage);
        String[] incomingSplit = inputMessage.split("]");
        String header = incomingSplit[0].replaceFirst("\\[", "");
        System.out.println(header);
        String initialMessage = incomingSplit[1].trim();
        String[] messageSplit = initialMessage.split(">");
        String[] outString = new String[2];
        outString[0] = header;
        for (int i = 1; i < messageSplit.length; i ++) {
            String[] userSplit = messageSplit[i].split("<");
            String userName = userSplit[0];
            String encryptedMessage = userSplit[1];
            if (userName.equals(Username)){
                outString[1] = encryptedMessage;
                return outString;
            }
        }
        outString[1] = "Error";
        return outString;
    }
}
