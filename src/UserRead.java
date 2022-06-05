
/**
 * The user reader thread which reads a server input and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
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
            CertInput = new ObjectInputStream(socket.getInputStream());
            X509Certificate userCertificate = (X509Certificate) CertInput.readObject();


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

        while (socket.isConnected() && user.isConnected()) {
            // Receive the text from server
            try {
                if ((line = input.readLine()) != null) {

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
