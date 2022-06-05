
/**
 * Abstraction of a chat room that users connect to in order to share secure messages
 * 
 * @author David Court, CRTDAV015
 * @author Bradley Culligan, CLLBRA005
 * @version June 2022
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Room {

    private String roomID;
    private String password;
    private Set<ServerThread> sThreads;

    /**
     * Room constructor for initialising new, empty room
     */
    public Room(String roomID, String password) {
        this.roomID = roomID;
        this.password = password;
        sThreads = new HashSet<>();
    }

    // Add user to room
    public void addUser(ServerThread sThread) {
        sThreads.add(sThread);
    }

    // Remove a user from the room
    public void removeUser(ServerThread sThread) {
        sThreads.remove(sThread);
    }

    // Get roomID
    public String getRoomID() {
        return roomID;
    }

    /**
     * Retrieve usernames of all users in a room
     */
    public ArrayList<String> getUsernames() {
        ArrayList<String> usernames = new ArrayList<>();
        for (ServerThread sThread : sThreads) {
            usernames.add(sThread.getUsername());
        }
        return usernames;
    }

    /**
     * Broadcast the given message to all users the room
     */
    public void broadcastMessage(String msg) {
        for (ServerThread sThread : sThreads) {
            sThread.sendMsg(msg);
        }
    }

    public String getPass() {
        return password;
    }

    // public String getPass(){
    // return password;
    // }
}
