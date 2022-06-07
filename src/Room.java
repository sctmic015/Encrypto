
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

    public ArrayList<KeyRingObject> getUserKeyRingObject(){
        ArrayList<KeyRingObject> userKeyRingObjects = new ArrayList<>();
        for (ServerThread sThread : sThreads){
            userKeyRingObjects.add(sThread.getKeyRingObject());
        }
        return userKeyRingObjects;
    }

    /**
     * Broadcast the given message to all users the room
     */
    public void broadcastMessage(String msg) {
        System.out.println("Messaged Received: " + msg);
        for (ServerThread sThread : sThreads) {
            System.out.println(sThread.getUsername());
            sThread.sendMsg(msg);

        }
    }
    public void broadcastMessageArray(ArrayList<String> msg){
        String msg2 = "[Mike] Hi";
        for (ServerThread sThread : sThreads){
            sThread.sendMsg(msg2);
        }
    }
    public void broadcastMessage2(String msg) throws InterruptedException {
        String[] incomingSplit = msg.split("]");
        String header = incomingSplit[0] + " ";
        String initialMessage = incomingSplit[1].trim();
        String[] messageSplit = initialMessage.split(">");
        for (int i = 1; i < messageSplit.length; i ++){
            String[] userSplit = messageSplit[i].split("<");
            String userName = userSplit[0];
            String encryptedMessage = userSplit[1];
            for (ServerThread sThread : sThreads){
                System.out.println("In broadcast Message");
                sThread.wait(20000);
                if (sThread.getKeyRingObject().getUsername().equals(userName)){
                    sThread.sendMsg(encryptedMessage);
                }
            }
        }
    }
    public void broadcastMessage3(String msg) throws InterruptedException {
        System.out.println("Messaged Received: " + msg);
        for (ServerThread sThread : sThreads) {
            System.out.println(sThread.getUsername());
            sThread.wait(2000000000);

            String[] incomingSplit = msg.split("]");
            String header = incomingSplit[0] + " ";
            String initialMessage = incomingSplit[1].trim();
            String[] messageSplit = initialMessage.split(">");
            for (int i = 1; i < messageSplit.length; i ++){
                String[] userSplit = messageSplit[i].split("<");
                String userName = userSplit[0];
                String encryptedMessage = userSplit[1];
                if (sThread.getKeyRingObject().getUsername().equals(userName)){
                    System.out.println(header + encryptedMessage);
                }
            }
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
    /**
    public void broadcastMessage(ArrayList<X509Certificate> keyRing){
        for (ServerThread sThread : sThreads){
            sThread.sendMsg(keyRing);
        }
    }
    */
    public void broadcastMessage(ArrayList<KeyRingObject> keyRing){
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
                //System.out.println(pubKey);
                System.out.println(new String(Base64.getDecoder().decode(pubKey), "UTF-8")); // incorrect
                //System.out.println(new String(Base64.getDecoder().decode(roomPubKeyEncoded), "UTF-8")); // correct
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
