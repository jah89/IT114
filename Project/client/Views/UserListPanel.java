package Project.client.Views;

import Project.common.LoggerUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.HashMap;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * UserListPanel represents a UI component that displays a list of users.
 */
public class UserListPanel extends JPanel {
    private JPanel userListArea;
    private GridBagConstraints lastConstraints; // Keep track of the last constraints for the glue
    private HashMap<Long, UserListItem> userItemsMap; // Maintain a map of client IDs to UserListItems

    /**
     * Constructor to create the UserListPanel UI.
     */
    public UserListPanel() {
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>(); // Initialize the map

        JPanel content = new JPanel(new GridBagLayout());
        userListArea = content;

        // Wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(userListArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0)); // Remove border

        this.add(scroll, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }
        });

        // Add vertical glue to push items to the top
        lastConstraints = new GridBagConstraints();
        lastConstraints.gridx = 0;
        lastConstraints.gridy = GridBagConstraints.RELATIVE;
        lastConstraints.weighty = 1.0;
        lastConstraints.fill = GridBagConstraints.VERTICAL;
        userListArea.add(Box.createVerticalGlue(), lastConstraints);

        // Listen for resize events to adjust user list items accordingly
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> adjustUserListItemsWidth());
            }
        });
    }

    /**
     * Adds a user to the list.
     *
     * @param clientId   The ID of the client. // jah89 07-20-2024
     * @param clientName The name of the client. // jah89 07-20-2024
     */
    protected void addUserListItem(long clientId, String clientName) { // jah89 07-20-2024
        SwingUtilities.invokeLater(() -> { // jah89 07-20-2024
            if (userItemsMap.containsKey(clientId)) { // jah89 07-20-2024
                LoggerUtil.INSTANCE.warning("User already in the list: " + clientName); // jah89 07-20-2024
                return; // jah89 07-20-2024
            }

            LoggerUtil.INSTANCE.info("Adding user to list: " + clientName); // jah89 07-20-2024

            UserListItem userItem = new UserListItem(clientId, clientName, userListArea); // jah89 07-20-2024

            // GridBagConstraints settings for each user // jah89 07-20-2024
            GridBagConstraints gbc = new GridBagConstraints(); // jah89 07-20-2024
            gbc.gridx = 0; // Column index 0 // jah89 07-20-2024
            gbc.gridy = userListArea.getComponentCount() - 1; // Place before the glue // jah89 07-20-2024
            gbc.weightx = 1; // Let the component grow horizontally to fill the space // jah89 07-20-2024
            gbc.anchor = GridBagConstraints.NORTH; // Anchor to the top // jah89 07-20-2024
            gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally // jah89 07-20-2024
            gbc.insets = new Insets(0, 0, 5, 0); // Add spacing between users // jah89 07-20-2024

            // Remove the last glue component if it exists // jah89 07-20-2024
            if (lastConstraints != null) { // jah89 07-20-2024
                int index = userListArea.getComponentCount() - 1; // jah89 07-20-2024
                if (index > -1) { // jah89 07-20-2024
                    userListArea.remove(index); // jah89 07-20-2024
                }
            }
            // Add user item // jah89 07-20-2024
            userListArea.add(userItem, gbc); // jah89 07-20-2024

            // Add vertical glue to push items to the top // jah89 07-20-2024
            userListArea.add(Box.createVerticalGlue(), lastConstraints); // jah89 07-20-2024

            userItemsMap.put(clientId, userItem); // Add to the map // jah89 07-20-2024

            userListArea.revalidate(); // jah89 07-20-2024
            userListArea.repaint(); // jah89 07-20-2024
        }); // jah89 07-20-2024
    }

    /**
     * Adjusts the width of all user list items. // jah89 07-20-2024
     */
    private void adjustUserListItemsWidth() { // jah89 07-20-2024
        SwingUtilities.invokeLater(() -> { // jah89 07-20-2024
            for (UserListItem item : userItemsMap.values()) { // jah89 07-20-2024
                item.setPreferredSize( // jah89 07-20-2024
                        new Dimension(userListArea.getWidth() - 20, item.getPreferredSize().height)); // jah89 07-20-2024
            }
            userListArea.revalidate(); // jah89 07-20-2024
            userListArea.repaint(); // jah89 07-20-2024
        }); // jah89 07-20-2024
    }

    /**
     * Removes a user from the list. // jah89 07-20-2024
     *
     * @param clientId The ID of the client to be removed. // jah89 07-20-2024
     */
    protected void removeUserListItem(long clientId) { // jah89 07-20-2024
        SwingUtilities.invokeLater(() -> { // jah89 07-20-2024
            LoggerUtil.INSTANCE.info("Removing user list item for id " + clientId); // jah89 07-20-2024
            UserListItem item = userItemsMap.remove(clientId); // Remove from the map // jah89 07-20-2024
            if (item != null) { // jah89 07-20-2024
                userListArea.remove(item); // jah89 07-20-2024
                userListArea.revalidate(); // jah89 07-20-2024
                userListArea.repaint(); // jah89 07-20-2024
            }
        }); // jah89 07-20-2024
    }

    /**
     * Clears the user list. // jah89 07-20-2024
     */
    protected void clearUserList() { // jah89 07-20-2024
        SwingUtilities.invokeLater(() -> { // jah89 07-20-2024
            LoggerUtil.INSTANCE.info("Clearing user list"); // jah89 07-20-2024
            userItemsMap.clear(); // Clear the map // jah89 07-20-2024
            userListArea.removeAll(); // jah89 07-20-2024
            userListArea.revalidate(); // jah89 07-20-2024
            userListArea.repaint(); // jah89 07-20-2024
        }); // jah89 07-20-2024
    }
}
