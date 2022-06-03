
/**
 * The user writer thread which writes user input to a server and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;

public class UserWrite extends Thread {
    private Socket socket;
    private User user;
    private BufferedWriter output;
    private AtomicBoolean running = new AtomicBoolean(true);

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
     * Sends final message to server and closes socket connection
     */
    public synchronized void shutdown(){
        try {
            running.set(false);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Threaded run method
     */
    @Override
    public void run() {
        try {
            while (socket.isConnected() && running.get()) {
                String text = user.getTextMessage();
                // Only send text if there is something meaningful to send
                if (text != "") {
                    output.write(text);
                    output.newLine();
                    output.flush();

                    // Message has been flushed, reset the text message
                    user.setTextMessage("");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}