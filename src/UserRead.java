
/**
 * The user reader thread which reads a server input and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.net.*;
import java.io.*;

public class UserRead extends Thread {
    private Socket socket;
    private User user;
    private BufferedReader input;

    /**
     * Constructor
     */
    public UserRead(Socket socket, User user) {
        this.socket = socket;
        this.user = user;

        // Setup the input handler
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
                    String[] controlCommands = line.split(":", 4);
                    // Assign the split variables appropriately
                    String command = "";
                    String contents = "";           //either msg or username set
                    command = controlCommands[1];   // Item zero is throwaway
                    contents = controlCommands[2];

                    switch (command) {
                        // If shutdown message has been called, then break
                        // Because readline is a blocking call, we need to receive a message to allow
                        // this thread to safely close
                        case "SHUTDOWN":
                            break;
                        case "UPDATE":
                            user.updateConnectedUsers(contents);
                            break;
                        case "MESSAGE":
                            user.setReceivedMessage(contents);
                            user.addNewMessage();
                            break;
                        default:
                            break;
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
