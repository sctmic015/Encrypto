
/**
 * Contains all code to create and handle the chat window for a user
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;

public class ChatWindow extends JFrame {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String PATH_TO_IMAGE = "res/chatLogo.png";

    // Components
    private ImageIcon imgIcon = new ImageIcon(PATH_TO_IMAGE);
    private JButton btnLogout = new JButton("Logout");
    private JButton btnStartRoom = new JButton("Start");
    private JButton btnJoinRoom = new JButton("Join");
    private JTable tblConnectedUsers;
    private JPanel pnlHeader;
    private JPanel pnlPersonalUsername;
    private JPanel pnlChattingToUsername;
    private JPanel pnlChatArea;
    private JPanel pnlConnectedUsers;
    private JPanel pnlRoomButtons;
    private JPanel pnlChatHistory;
    private JPanel pnlTypeAndSendMessage;
    private JLabel lblLogoImage = new JLabel(
            new ImageIcon(imgIcon.getImage().getScaledInstance(610, 435, Image.SCALE_DEFAULT)));
    private JLabel lblWelcomeText = new JLabel(
            "Welcome! Please click on 'Start' to start your own room or 'Join' to join a room...");
    private JLabel lblPersonalUsername;
    private JLabel lblChattingToUsername;
    private JTextArea txtareaChatHistory;
    private JTextField txtMessage;
    private JButton btnSend = new JButton("Send");

    // Fields
    private String chosenChatName = "Welcome!";
    private String personalUsername;
    private String hint = "Type a message...";
    // private boolean splashOpen = true;
    private User user;
    private String curRoomID = "";

    /**
     * Default frame constructor
     */
    public ChatWindow(User user) {
        // Standard requirements to make and format chat window
        super("Encrypto");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // User must quit via 'Disconnect' button
        setResizable(false); // Prefer to have a default window size to avoid moving components unnecessarily
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout());

        // Store the user
        this.user = user;

        // Create and set image icone for personalisation
        setIconImage(imgIcon.getImage());

        // Confirm with user for server disconnect and close application
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Application can only quit if user agrees
        addWindowListener(new WindowAdapter() {
            /**
             * If a user chooses to close the application, they must first confirm it in
             * order to tell the server that they have disconnected
             */
            @Override
            public void windowClosing(WindowEvent e) {
                if ((JOptionPane.showConfirmDialog(null, "Are you sure you want to disconnect and quit?",
                        "Disconnect and Quit", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)) {
                    user.setTextMessage(":LOGOUT:");
                    user.disconnect();
                    System.exit(0);
                }
            }
        });

        // Set up panels
        pnlConnectedUsers = new JPanel();
        pnlConnectedUsers.setPreferredSize(new Dimension(180, 485));
        pnlRoomButtons = new JPanel();
        pnlRoomButtons.setPreferredSize(new Dimension(180, 75));
        pnlHeader = new JPanel();
        pnlHeader.setPreferredSize(new Dimension(800, 40));
        pnlPersonalUsername = new JPanel();
        pnlChattingToUsername = new JPanel();
        pnlHeader.add(pnlPersonalUsername);
        pnlHeader.add(pnlChattingToUsername);
        pnlChatHistory = new JPanel();
        pnlTypeAndSendMessage = new JPanel();
        pnlChatArea = new JPanel();
        pnlChatArea.add(pnlChatHistory);
        pnlChatArea.add(pnlTypeAndSendMessage);

        // Set up 'connected users' panel
        pnlConnectedUsers.setLayout(new BorderLayout());
        pnlConnectedUsers.add(pnlRoomButtons, BorderLayout.SOUTH);

        String[] colName = { "Encrypto" };
        Object[][] connectedUsers = {};

        // Create table without editing permissions
        tblConnectedUsers = new JTable(connectedUsers, colName) {
            /**
             * If a user attempts to edit a cell, it will return false
             */
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tblConnectedUsers.setCellSelectionEnabled(false);

        // Add mouse event to get selected user from table
        /*
         * tblConnectedUsers.addMouseListener(new MouseAdapter() {
         * /**
         * Get the selected user and setup a chat with them
         *
         * @Override
         * public void mousePressed(MouseEvent e) {
         * String selectedUser;
         * int row = tblConnectedUsers.getSelectedRow();
         * int col = tblConnectedUsers.getSelectedColumn();
         * selectedUser = (String) tblConnectedUsers.getValueAt(row, col);
         * chosenChatName = selectedUser;
         * setupChat();
         * splashOpen = false; // Set the splash option off (Chat currently open)
         * }
         * });
         */

        // Get the default table cell renderer for this class and adjust its horizontal
        // alignment to centre (horizontally) text items in cells
        DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) tblConnectedUsers
                .getDefaultRenderer(this.getClass());
        cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // Similar performance as above but for headers done separately
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) tblConnectedUsers.getTableHeader()
                .getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblConnectedUsers.setFont(new Font("", Font.PLAIN, 13));
        // Column header font must be separately set
        tblConnectedUsers.getTableHeader().setFont(new Font("", Font.BOLD, 13));
        tblConnectedUsers.setRowHeight(25);
        pnlConnectedUsers.add(new JScrollPane(tblConnectedUsers)); // ScrollPane needed to show header

        // Setup room buttons panel
        // Buttons for starting/joining a room and logging out
        pnlRoomButtons.add(btnStartRoom);
        pnlRoomButtons.add(btnJoinRoom);
        pnlRoomButtons.add(btnLogout);

        // Start room functionality
        btnStartRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnStartRoom) {

                    // Popup design
                    JTextField roomID = new JTextField(10);
                    JTextField password = new JTextField(10);
                    JPanel myPanel = new JPanel();
                    myPanel.add(new JLabel("Enter new room name:"));
                    myPanel.add(roomID);
                    myPanel.add(Box.createHorizontalStrut(5)); // a spacer
                    myPanel.add(new JLabel("Create new room passord:"));
                    myPanel.add(password);

                    // Open popup
                    int result = JOptionPane.showConfirmDialog(null, myPanel,
                            "Start new room", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        // TODO: Write check to ensure room ID is unique and id/password are not null
                        curRoomID = roomID.getText();
                        user.setTextMessage(":START:" + curRoomID + ":");
                        setupChat();
                    }
                }
            }
        });

        // Join room functionality
        btnJoinRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnJoinRoom) {
                    JTextField roomID = new JTextField(10);
                    JTextField password = new JTextField(10);

                    JPanel myPanel = new JPanel();
                    myPanel.add(new JLabel("Enter room name:"));
                    myPanel.add(roomID);
                    myPanel.add(Box.createHorizontalStrut(5)); // a spacer
                    myPanel.add(new JLabel("Enter room passord:"));
                    myPanel.add(password);

                    int result = JOptionPane.showConfirmDialog(null, myPanel,
                            "Join room", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        // TODO: Write check to ensure room ID is unique and id/password are not null
                        curRoomID = roomID.getText();
                        user.setTextMessage(":JOIN:" + curRoomID + ":");
                        setupChat();
                    }
                }
            }
        });

        // Disconnect button functionality
        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnLogout) {
                    // Pass message to server to logout user
                    user.setTextMessage(":LOGOUT:");
                    dispose();
                    if (user.disconnect()) {
                        new LoginWindow(new User(user.getHost(), user.getPort()));
                    }
                }
            }
        });

        // Set up header
        pnlHeader.setLayout(null);
        pnlPersonalUsername.setLayout(new BorderLayout());
        pnlChattingToUsername.setLayout(new BorderLayout());
        lblPersonalUsername = new JLabel(personalUsername);
        lblPersonalUsername.setFont(new Font("", Font.BOLD, 15));
        lblPersonalUsername.setHorizontalAlignment(SwingConstants.CENTER);
        pnlPersonalUsername.setBounds(0, 0, 180, 40);
        pnlPersonalUsername.setBackground(new Color(0x21827e));
        lblChattingToUsername = new JLabel(chosenChatName); // Default header is not to a user
        lblChattingToUsername.setFont(new Font("", Font.BOLD, 14));
        lblChattingToUsername.setHorizontalAlignment(SwingConstants.CENTER);
        pnlChattingToUsername.setBounds(180, 0, 620, 40);
        pnlChattingToUsername.setBackground(new Color(0x7F7F7F));
        pnlPersonalUsername.add(lblPersonalUsername, BorderLayout.CENTER);
        pnlChattingToUsername.add(lblChattingToUsername, BorderLayout.CENTER);

        // Set up chat area
        pnlChatHistory.add(lblLogoImage);
        pnlTypeAndSendMessage.add(lblWelcomeText);

        // Add panels to chat window frame
        add(pnlHeader, BorderLayout.NORTH);
        add(pnlChatArea, BorderLayout.CENTER);
        add(pnlConnectedUsers, BorderLayout.WEST);

        setVisible(true);
    }

    /**
     * Show chatting area instead of splash
     */
    public void setupChat() {
        // if (splashOpen) {
        // splashOpen = false; // Set the splash option off (Chat currently opening)

        // Set chatting roomID and clear splash components
        lblChattingToUsername.setText(curRoomID);
        pnlChatHistory.remove(lblLogoImage);
        pnlTypeAndSendMessage.remove(lblWelcomeText);
        pnlChatArea.setLayout(new BorderLayout());

        // Add chat history box
        txtareaChatHistory = new JTextArea(30, 48);
        txtareaChatHistory.setLineWrap(true);
        txtareaChatHistory.setEditable(false);
        txtareaChatHistory.setAutoscrolls(true);
        pnlChatHistory.add(new JScrollPane(txtareaChatHistory, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        pnlChatArea.add(pnlChatHistory, BorderLayout.CENTER);

        // Add message typing and sending area
        txtMessage = new JTextField(hint);
        txtMessage.setColumns(42);

        // Give text message input area hint functionality
        txtMessage.addFocusListener(new FocusAdapter() {
            /**
             * When the textfield for sending a message gains focus and contains the hint
             * message, clear the contents for a smoother typing experience
             */
            @Override
            public void focusGained(FocusEvent e) {
                if (txtMessage.getText().equals(hint)) {
                    txtMessage.setText(""); // Clear contents
                }
            }

            /**
             * If the user causes focus on the message dialog to be lost, then the hint must
             * return as the text
             */
            @Override
            public void focusLost(FocusEvent e) {
                if (txtMessage.getText().equals(hint) || txtMessage.getText().length() == 0) {
                    txtMessage.setText(hint);
                }
            }
        });

        pnlTypeAndSendMessage.add(txtMessage);
        pnlTypeAndSendMessage.add(btnSend);

        // Send button functionality
        btnSend.addActionListener(new ActionListener() {
            /**
             * Send the typed contents for messaging to the server to send to each connected
             * user in room
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                String messageContents = txtMessage.getText();
                txtMessage.setText(hint);

                // Set the user chat contents for sending to server
                user.setTextMessage(
                        ":MESSAGE:" + curRoomID + ":" + "[" + user.getUsername() + "] " + messageContents);
            }
        });

        // Update heading of connected users table
        tblConnectedUsers.getColumnModel().getColumn(0).setHeaderValue("In room with:");
        tblConnectedUsers.getTableHeader().repaint();

        pnlChatArea.add(pnlTypeAndSendMessage, BorderLayout.SOUTH);
        // } else {
        // Messaging screen is already setup so just clear the contents
        // clearChatArea();
        // }
    }

    /**
     * Clear the components used in the chat area
     */
    public void clearChatArea() {
        txtareaChatHistory.setText("");
        txtMessage.setText(hint);
    }
}
