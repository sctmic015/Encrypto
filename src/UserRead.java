/**
 * The user reader thread which reads a server input and appropriately handles that data
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;

public class UserRead extends Thread{
    private Socket socket;
    private User user;
    private BufferedReader input;
    private AtomicBoolean running = new AtomicBoolean(true);

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
     * Sends final message to server and closes socket connection
     */
    public synchronized void shutdown() {
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
        while (socket.isConnected() && running.get()) {
            // Receive the text from server
            try {
                user.setReceivedMessage(input.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
