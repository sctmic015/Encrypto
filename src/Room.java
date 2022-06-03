import java.util.HashSet;
import java.util.Set;

// Abstraction of a chat room that users connect to in order to share secure messages

public class Room {

    private String roomID;
    //private String password;
    private Set<String> usernames; //changeto SET
    private Set<ServerThread> sThreads;

    // Initialise a new, empty room
    public Room(String roomID) {
        this.roomID = roomID;
        usernames = new HashSet<>();
        sThreads = new HashSet<>();
    }

    // Method to add user to room
    //Should maybe make this boolean for checks
    public void addUser(String newUser, ServerThread sThread){
        usernames.add(newUser);
        sThreads.add(sThread);
    }

    // Method to remove user from room
    public void removeUser(String username) {
        usernames.remove(username);
    }

    public void removeThread(ServerThread sThread){
        sThreads.remove(sThread);
    }

    public String getRoomID(){
        return roomID;
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    public void broadcastMessage(String msg){
        for (ServerThread sThread : sThreads) {
            sThread.sendMsg(msg);
        }
    }

    public String toString(){
        return roomID + getUsernames().toString();
    }

    // public String getPass(){
    // return password;
    // }
}
