
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
import java.security.cert.X509Certificate;

public class UserRead extends Thread {
    private Socket socket;
    private User user;
    private BufferedReader input;
    private X509Certificate userCertificate;
    private ObjectInput CertInput;

    /**
     * Constructor
     */
    public UserRead(Socket socket, User user) throws ClassNotFoundException {
        this.socket = socket;
        this.user = user;

        // Setup the input handler
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("LOl");
            System.out.println("Lol2");
            //X509Certificate userCertificate = (X509Certificate) CertInput.readObject();


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
                if (count == 0) {
                    count ++;
                    CertInput = new ObjectInputStream(socket.getInputStream());
                    if ((certificate = (X509Certificate) CertInput.readObject()) != null) {
                        user.addCertificate(certificate);
                        System.out.println(certificate);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } 
            // Receive the text from server
            try {
                if ((line = input.readLine()) != null) {
                    //System.out.println("Test");
                    System.out.print(line);
                    // Using control commands, handle incoming message appropriately
                    String[] controlCommands = line.split(":", 3);
                    // Assign the split variables appropriately
                    String command = "";
                    String contents = ""; // either msg or username set
                    command = controlCommands[1]; // Item zero is throwaway
                    contents = controlCommands[2];

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
                        user.setReceivedMessage(contents);
                        user.addNewMessage();
                    }

                }

            } catch (IOException e) {
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
}
