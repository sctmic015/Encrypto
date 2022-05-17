/**
 * Represents a user and handles logic that UI controls
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

public class User {
    private String username = "";
    public LoginWindow loginWindow;
    private ChatWindow chatWindow;

    /**
     * Adjust username
     */
    public void setUsername(String username){
        this.username = username;
    }
    
    /**
     * Get the username of user
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Checks if text supplied as argument is a valid username
     * 
     * @param String: Username supplied to check if valid
     */
    public boolean validUsername() {
        // TODO: First need to connect to server
        return true;// (server.addUser(username));
    }

    /**
     * Start the main chat window with this user
     */
    public void createChatWindow(User user){
        chatWindow = new ChatWindow(user);
    }

    /**
     * Attempts to disconnect the user from the server
     */
    public boolean disconnect(){
        // TODO: Logic to be implemented sending disconnect to server
        return true;
    }

    public static void main(String[] args) {
        User user = new User();
        user.loginWindow = new LoginWindow(user);
    }
}
