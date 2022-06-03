
/**
 * Abstraction of a chat room that users connect to in order to share secure messages
 * 
 * @author David Court, CRTDAV015
 * @author Bradley Culligan, CLLBRA005
 * @version June 2022
 */

import java.util.HashSet;
import java.util.Set;

public class Room {

    private String roomID;
    // private String password;
    private Set<ServerThread> sThreads;

    // Initialise a new, empty room
    public Room(String roomID) {
        this.roomID = roomID;
        sThreads = new HashSet<>();
    }

    // Method to add user to room
    // Should maybe make this boolean for checks
    public void addUser(ServerThread sThread) {
        sThreads.add(sThread);
    }

    // Method to remove user from room
    public void removeUser(ServerThread sThread) {
        sThreads.remove(sThread);
    }

    public String getRoomID() {
        return roomID;
    }

    public Set<String> getUsernames() {
        Set<String> usernames = new HashSet<>();
        for (ServerThread sThread : sThreads) {
            usernames.add(sThread.getUsername());
        }
        return usernames;
    }

    public void broadcastMessage(String msg) {
        for (ServerThread sThread : sThreads) {
            sThread.sendMsg(msg);
        }
    }

    public String toString() {
        return roomID + getUsernames().toString();
    }

    // public String getPass(){
    // return password;
    // }
}
