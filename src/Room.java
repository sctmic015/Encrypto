
/**
 * Abstraction of a chat room that users connect to in order to share secure messages
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

//import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA;

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

    public ArrayList<X509Certificate> getUserKeys() {
        ArrayList<X509Certificate> userKeys = new ArrayList<>();
        for (ServerThread sThread : sThreads){
            userKeys.add(sThread.getUserCertificate());
        }
        return userKeys;
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
     * Broadcast the given certificate to all users the room
     */
    public void broadcastMessage(X509Certificate userCertificate){
        for (ServerThread sThread : sThreads){
            sThread.sendMsg(userCertificate);
            System.out.println("Certificate broadcasted 2");
        }
    }

    /**
     * Broadcast the given keyRing to all users the room
     */
    public void broadcastMessage(ArrayList<X509Certificate> keyRing){
        for (ServerThread sThread : sThreads){
            sThread.sendMsg(keyRing);
        }
    }

    /**
     * Message user with supplied public key
     */
    public void msgUser(String pubKey, String cipherText, String senderPubKey) {
        for (ServerThread sThread : sThreads) {
            String roomPubKeyEncoded="";
            try {
                roomPubKeyEncoded = Base64.getEncoder().encodeToString(sThread.getPublicKey().toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                System.out.println(new String(Base64.getDecoder().decode(pubKey), "UTF-8")); // incorrect
                System.out.println(new String(Base64.getDecoder().decode(roomPubKeyEncoded), "UTF-8")); // correct
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (roomPubKeyEncoded.equals(pubKey)) {
                sThread.sendMsg(cipherText + ":" + senderPubKey + ":");
            }
        }
    }

    /**
     * Return the (hashed) room password 
     */
    public String getPass() {
        return password;
    }
}
