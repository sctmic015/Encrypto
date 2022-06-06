
/**
 * Abstraction of a chat room that users connect to in order to share secure messages
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA;

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

    /**
     * Add user's server thread to room (i.e. user added to room)
     */
    public void addUser(ServerThread sThread) {
        sThreads.add(sThread);
    }

    /**
     * Remove user's server thread from room (i.e. user removed from room)
     */
    public void removeUser(ServerThread sThread) {
        sThreads.remove(sThread);
    }

    /**
     * Getter for room's ID
     */
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

    /**
     * Message user with supplied public key
     */
    public void msgUser(PublicKey pubKey, String cipherText) {
        for (ServerThread sThread : sThreads) {
            if (sThread.getPublicKey().equals(pubKey)) {
                sThread.sendMsg(cipherText);
            }
        }
    }

    public String getPass() {
        return password;
    }
}
