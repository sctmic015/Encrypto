
/**
 * Contains all code to create and handle the login window
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;

public class LoginWindow extends JFrame {

    // Constants
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String PATH_TO_IMAGE = "res/chatLogo.png";

    // Components
    private JPanel pnlLogo;
    private JPanel pnlBottomBar;
    private JButton btnJoinNetwork;
    private JLabel lblJoinText;
    private ImageIcon imgIcon = new ImageIcon(PATH_TO_IMAGE);
    private JLabel lblLogoImage = new JLabel(imgIcon);
    private JTextField txtJoinName;

    // Fields
    private User user;

    /**
     * Default frame constructor
     */
    public LoginWindow(User user) {
        // Standard requirements to make and format login window
        super("Encrypto");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); // Prefer to have a default window size to avoid moving components unnecessarily
        setLocationRelativeTo(null); // Center the window
        setLayout(new FlowLayout());

        // Store the user that created the login window
        this.user = user;

        // Create and set image icone for personalisation
        setIconImage(imgIcon.getImage());

        // Setup for logo panel in centre of window
        pnlLogo = new JPanel();
        pnlLogo.add(lblLogoImage);

        // Setup for bottom bar with user input
        pnlBottomBar = new JPanel();
        btnJoinNetwork = new JButton("Login");
        lblJoinText = new JLabel("Username: ");
        txtJoinName = new JTextField(18);
        pnlBottomBar.add(lblJoinText);
        pnlBottomBar.add(txtJoinName);
        pnlBottomBar.add(btnJoinNetwork);

        // Login button event call
        btnJoinNetwork.addActionListener(new ActionListener() {
            /**
             * Login the user and dispose of this login window whilst creating the main chat
             * window or send a warning dialog to the screen to notify the user that they've
             * entered an invalid username
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = txtJoinName.getText().trim();
                user.setUsername(username);
                try {
                    if (login()) {
                        dispose();
                        user.createChatWindow(user);
                    } else {
                        JOptionPane.showMessageDialog(null, "Please enter a different username", "Username Invalid",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Add panels to main window
        add(pnlLogo);
        add(pnlBottomBar);

        setVisible(true);
    }

    /**
     * Attempt to login the user by setting the username
     */
    private boolean login() throws ClassNotFoundException {
        // Prevent further user interaction until login check is complete
        pnlBottomBar.remove(btnJoinNetwork);
        pnlBottomBar.remove(txtJoinName);
        lblJoinText.setPreferredSize(new Dimension(800, 20)); // Set text space to fill width of window
        lblJoinText.setHorizontalAlignment(SwingConstants.CENTER); // Centre the joining text
        lblJoinText.setText("Joining...");

        // Check if username is valid
        if (user.validUsername()) {
            return true;
        } else {
            // Re-add all the components back to their previous condition
            pnlBottomBar.remove(lblJoinText);
            lblJoinText = new JLabel("Username: ");
            txtJoinName.setText("");
            pnlBottomBar.add(lblJoinText);
            pnlBottomBar.add(txtJoinName);
            pnlBottomBar.add(btnJoinNetwork);

            return false;
        }
    }
}