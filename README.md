# Encrypto

2022 CS Honours NIS project for encrypted group chat. Developed by David Court (CRTDAV015), Bradley Culligan (CLLBRA005), and Michael Scott (SCTMIC015).

## Getting started

In a terminal instance, first run `make server` to start the Encrypto server on port 4444.
In a new terminal instance, run `make user` to create a user and launch its GUI. This can be repeated any number of times to create more users and their GUIs.

The GUI is used to interact with the application. The login window is the first window that opens when a user is started. At the bottom, there is a text field for a username to be entered - this must be between 1 and 17 characters in length. Click the login button to connect your user to the server.

The following window displayed is the chat window. In the top left, your chosen username will be displayed. Next to that, it will say "Welcome!" - this welcome text will display the room ID when you join a room. In the middle of the screen, on the right, the Encrypto logo is displayed and a helpful hint text on how to proceed. Use the buttons on the bottom left to start a room, join a room, or logout from the application.

When starting or joining a room, the user will be prompted to enter a room ID and password. These have a maximum length of 10 characters and the room ID must have at least one character. Note that the password can be empty for a public room.

Note that when attempting to join a room, the room must exist (i.e. a user must have started a room with the room ID and password required) and the correct room ID and password must be entered, else the attempt to join the room will be denied.

If successfully entered into a room, the GUI will be updated. The left middle displays a list of all users whom are in the room you are chatting with. The right middle is the text chat area which will display all messages being passed in the room. Underneath this, there is a field for you to type a message and click send to share it with all room participants.

Use the logout button to return to the login window and remove your user from the server. Closing the application will also prompt you to disconnect from the server and logout.

Note that the required crypto jars have been included in the lib directory and the Makefile will automatically associate those jars when trying to run the application. The Makefile is intended for Unix machine usage. If you are attempting to run the application through your IDE or via some other method not using the Makefile, then please ensure that the jars in lib are appropriately setup on your device.