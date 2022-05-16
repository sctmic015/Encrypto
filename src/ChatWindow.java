
/**
 * Contains all code to create and handle the chat window for a user
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

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
    private JButton btnDisconnect = new JButton("Disconnect");
    private JButton btnStartRoom = new JButton("Start Room");
    private JButton btnJoinRoom = new JButton("Join Room");
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
            new ImageIcon(imgIcon.getImage().getScaledInstance(670, 448, Image.SCALE_DEFAULT)));
    private JLabel lblWelcomeText = new JLabel("Welcome! Please click on a room to join or start your own room...");
    private JLabel lblPersonalUsername;
    private JLabel lblChattingToUsername;
    private JTextArea txtareaChatHistory;
    private JTextField txtMessage;
    private JButton btnSend = new JButton("Send");

    // Fields
    private String chosenChatName = "Welcome!";
    private String personalUsername;
    private String hint = "Type a message...";
    private boolean splashOpen = true;

    /**
     * Default frame constructor
     */
    public ChatWindow(String username) {
        // Standard requirements to make and format chat window
        super("Encrypto");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // User must quit via 'Disconnect' button
        setResizable(false); // Prefer to have a default window size to avoid moving components unnecessarily
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout());

        // Set connected user's username
        this.personalUsername = username;

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
                if (JOptionPane.showConfirmDialog(null, "Are you sure you want to disconnect and quit?",
                        "Disconnect and Quit", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    // TODO: <-- SEND DISCONNECT MESSAGE TO SERVER -->
                    System.exit(0);
                }
            }
        });

        // Set up panels
        pnlConnectedUsers = new JPanel();
        pnlConnectedUsers.setPreferredSize(new Dimension(120, 460));
        pnlRoomButtons = new JPanel();
        pnlRoomButtons.setPreferredSize(new Dimension(120, 100));
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

        // **** FAKE TABLE SETUP *****
        String[] colName = { "Rooms:" };
        Object[][] connectedUsers = { { "2" }, { "32" }, { "123" } };

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
        tblConnectedUsers.setCellSelectionEnabled(true);

        // Add mouse event to get selected user from table
        tblConnectedUsers.addMouseListener(new MouseAdapter() {
            /**
             * Get the selected user and setup a chat with them
             */
            @Override
            public void mousePressed(MouseEvent e) {
                String selectedUser;
                int row = tblConnectedUsers.getSelectedRow();
                int col = tblConnectedUsers.getSelectedColumn();
                selectedUser = (String) tblConnectedUsers.getValueAt(row, col);
                chosenChatName = selectedUser;
                setupChat();
                splashOpen = false; // Set the splash option off (Chat currently open)
            }
        });

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
        pnlRoomButtons.add(btnDisconnect);

        // Start room functionality
        btnStartRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Room starting logic to be implemented
                if (e.getSource() == btnStartRoom) {
                    JOptionPane.showMessageDialog(null, "Starting Room...");
                    // TODO: SEND START ROOM MESSAGE TO SERVER
                }
            }
        });

        // Join room functionality
        btnJoinRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Room joining logic to be implemented
                if (e.getSource() == btnJoinRoom) {
                    JOptionPane.showMessageDialog(null, "Joining Room...");
                    // TODO: SEND START ROOM MESSAGE TO SERVER
                }
            }
        });

        // Disconnect button functionality
        btnDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnDisconnect) {
                    dispose();
                    // TODO: SEND DISCONNECTED MESSAGE TO SERVER
                    new LoginWindow();
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
        pnlPersonalUsername.setBounds(0, 0, 120, 40);
        pnlPersonalUsername.setBackground(new Color(0x21827e));
        lblChattingToUsername = new JLabel(chosenChatName); // Default header is not to a user
        lblChattingToUsername.setFont(new Font("", Font.BOLD, 14));
        lblChattingToUsername.setHorizontalAlignment(SwingConstants.CENTER);
        pnlChattingToUsername.setBounds(120, 0, 680, 40);
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
        if (splashOpen) {
            // Set chatting username and clear splash components
            lblChattingToUsername.setText(chosenChatName);
            pnlChatHistory.remove(lblLogoImage);
            pnlTypeAndSendMessage.remove(lblWelcomeText);
            pnlChatArea.setLayout(new BorderLayout());

            // Add chat history box
            txtareaChatHistory = new JTextArea(30, 54);
            txtareaChatHistory.setLineWrap(true);
            txtareaChatHistory.setEditable(false);
            txtareaChatHistory.setAutoscrolls(true);
            pnlChatHistory.add(new JScrollPane(txtareaChatHistory, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
            pnlChatArea.add(pnlChatHistory, BorderLayout.CENTER);

            // Add message typing and sending area
            txtMessage = new JTextField(hint);
            txtMessage.setColumns(48);

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
                 * Send the typed contents for messaging to the chat history area.
                 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    String messageContents = txtMessage.getText();
                    txtMessage.setText(hint);

                    txtareaChatHistory.append("[You]\n" + messageContents + "\n\n");
                }
            });

            pnlChatArea.add(pnlTypeAndSendMessage, BorderLayout.SOUTH);
        } else {
            // Messaging screen is already setup so just clear the contents
            clearChatArea();
        }
    }

    /**
     * Clear the components used in the chat area
     */
    public void clearChatArea() {
        txtareaChatHistory.setText("");
        txtMessage.setText(hint);
    }
}
