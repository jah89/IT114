package Project.client.Views;

import Project.client.CardView;
import Project.client.Interfaces.ICardControls;
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * ConnectionPanel is a JPanel that allows the user to input the host and port
 * for a connection. It uses a BorderLayout with a BoxLayout for the content
 * panel. It validates the port input and displays error messages if necessary.
 */
public class ConnectionPanel extends JPanel {
    private String host;
    private int port;
    private String username; // Declare the username variable

    /**
     * Constructs a ConnectionPanel with the specified controls.
     * 
     * @param controls the ICardControls to handle card navigation.
     */
    public ConnectionPanel(ICardControls controls) {
        super(new BorderLayout(10, 10)); // Set BorderLayout with gaps

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding

        // Add username - jah89 07/19/2024
        JLabel userLabel = new JLabel("Username:");
        JTextField userValue = new JTextField();
        userValue.setToolTipText("Enter your username (no spaces)"); 
        JLabel userError = new JLabel();
        userError.setVisible(false); 
        content.add(userLabel);
        content.add(userValue);
        content.add(userError);

        // Add host input field
        JLabel hostLabel = new JLabel("Host:");
        JTextField hostValue = new JTextField("127.0.0.1");
        hostValue.setToolTipText("Enter the host address"); // Add tooltip
        JLabel hostError = new JLabel();
        hostError.setVisible(false); // Initially hide the error label
        content.add(hostLabel);
        content.add(hostValue);
        content.add(hostError);

        // Add port input field
        JLabel portLabel = new JLabel("Port:");
        JTextField portValue = new JTextField("3000");
        portValue.setToolTipText("Enter the port number"); // Add tooltip
        JLabel portError = new JLabel();
        portError.setVisible(false); // Initially hide the error label
        content.add(portLabel);
        content.add(portValue);
        content.add(portError);

        // Add Next button
        JButton button = new JButton("Next");
        button.setAlignmentX(JButton.CENTER_ALIGNMENT); // Center the button
        button.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
                boolean isValid = true;
                try {
                    port = Integer.parseInt(portValue.getText());
                    portError.setVisible(false); // Hide error label if valid
                } catch (NumberFormatException e) {
                    portError.setText("Invalid port value, must be a number");
                    portError.setVisible(true); // Show error label if invalid
                    isValid = false;
                }

                // Validate username - jah89 07/19/2024
                username = userValue.getText().trim();
                if (username.isEmpty() || username.contains(" ")) {
                    userError.setText("Invalid username, must not be empty or contain spaces");
                    userError.setVisible(true); 
                    isValid = false;
                } else {
                    userError.setVisible(false); 
                }

                if (isValid) {
                    host = hostValue.getText();
                    controls.next(); 
                }
            });
        });
        content.add(Box.createVerticalStrut(10)); // Add vertical spacing
        content.add(button);

        // Add the content panel to the center of the BorderLayout
        this.add(content, BorderLayout.CENTER);
        this.setName(CardView.CONNECT.name()); // Set the name of the panel
        controls.addPanel(CardView.CONNECT.name(), this); // Add panel to controls
    }

    /**
     * Gets the host value entered by the user.
     * 
     * @return the host value.
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port value entered by the user.
     * 
     * @return the port value.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the username entered by the user.
     * 
     * @return the username.
     * jah89 07/19/2024
     */
    public String getUsername() {
        return username;
    }
}
