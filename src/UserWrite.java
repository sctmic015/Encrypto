
/**
 * The user writer thread which writes user input to a server and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.net.*;
import java.io.*;

public class UserWrite extends Thread {
    private Socket socket;
    private User user;
    private BufferedWriter output;

    /**
     * Constructor
     */
    public UserWrite(Socket socket, User user) {
        this.socket = socket;
        this.user = user;

        // Setup the output handler
        try {
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // Send the user's username first
            output.write(user.getUsername());
            output.newLine();
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded run method
     */
    @Override
    public void run() {
        while (socket.isConnected() && user.isConnected()) { 
            try {
                String text = user.getTextMessage();
                // Only send text if there is something meaningful to send
                if (text != "") {
                    output.write(text);
                    output.newLine();
                    output.flush();

                    // Message has been flushed, reset the text message
                    user.setTextMessage("");
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