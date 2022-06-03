import java.util.HashSet;
import java.util.Set;

// Abstraction of a chat room that users connect to in order to share secure messages

public class Room {
    
    private String roomID;
    //private String password;
    private Set<String> usernames; //changeto SET

    // Initialise a new, empty room
    public Room(String roomID){
        this.roomID = roomID;
        usernames = new HashSet<>();
    }

    // Method to add user to room
    //Should maybe make this boolean for checks
    public void addUser(String newUser){
        usernames.add(newUser);
    }

    // Method to remove user from room 
    public void removeUser(String username){
        usernames.remove(username);
    }

    public String getRoomID(){
        return roomID;
    }

    public Set<String> getUsernames(){
        return usernames;
    }

    // public String getPass(){
    //     return password;
    // }
}
