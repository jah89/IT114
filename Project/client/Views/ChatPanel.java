package Project.client.Views;

import Project.client.CardView;
import Project.client.Client;
import Project.client.Interfaces.ICardControls;
import Project.common.LoggerUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * ChatPanel represents the main chat interface where messages can be sent and
 * received.
 */
public class ChatPanel extends JPanel {
    private JPanel chatArea = null;
    private UserListPanel userListPanel;
    private final float CHAT_SPLIT_PERCENT = 0.7f;
    private JTextArea chatHistory; // jah89 07-20-2024
    private JTextField messageInput; // jah89 
    private JButton sendButton; // jah89 
    private JButton exportButton; // jah89 07-26-2024

    /**
     * Constructor to create the ChatPanel UI.
     * 
     * @param controls The controls to manage card transitions.
     */
    public ChatPanel(ICardControls controls) {
        super(new BorderLayout(10, 10));

        JPanel chatContent = new JPanel(new GridBagLayout());
        chatContent.setAlignmentY(Component.TOP_ALIGNMENT);

        // Wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(chatContent);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        chatArea = chatContent;

        userListPanel = new UserListPanel();

        // JSplitPane setup with chat on the left and user list on the right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, userListPanel);
        splitPane.setResizeWeight(CHAT_SPLIT_PERCENT); // Allocate % space to the chat panel initially

        // Enforce splitPane split
        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(CHAT_SPLIT_PERCENT));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(CHAT_SPLIT_PERCENT));
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        chatHistory = new JTextArea(); // jah89 07-20-2024
        chatHistory.setEditable(false); 
        JScrollPane chatScrollPane = new JScrollPane(chatHistory); 

        JPanel input = new JPanel();
        input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
        input.setBorder(new EmptyBorder(5, 5, 5, 5));

        messageInput = new JTextField(); // jah89 07-20-2024
        input.add(messageInput); 

        sendButton = new JButton("Send"); 
        messageInput.addKeyListener(new KeyListener() { 
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendButton.doClick(); 
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        sendButton.addActionListener((event) -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    String text = messageInput.getText().trim(); //JAH89 07-20-2024
                    if (!text.isEmpty()) {
                        long clientId = Client.INSTANCE.getClientId(); // Get the client ID - jah89 07-26-2024
                        if (text.startsWith("@")) {
                            int spaceIndex = text.indexOf(" ");
                            if (spaceIndex != -1) {
                                String targetName = text.substring(1, spaceIndex);
                                String privateMessage = text.substring(spaceIndex + 1);
                                long targetId = Client.INSTANCE.getClientIdByName(targetName);  
                                if (targetId == -1) {
                                    chatHistory.append("User " + targetName + " not found.\n");
                                } else {
                                    Client.INSTANCE.sendPrivateMessage(targetId, privateMessage);
                                    chatHistory.append("To " + targetName + ": " + privateMessage + "\n");
                                    updateUserStatus(clientId, false, true); // jah89 07-26-2024
                                }
                            }
                        } else {
                            Client.INSTANCE.sendMessage(text);
                            chatHistory.append("Me: " + text + "\n");
                            updateUserStatus(clientId, false, true); // jah89 07-26-2024
                        }
                        messageInput.setText(""); // Clear the original text
                    }
                } catch (NullPointerException | IOException e) {
                    LoggerUtil.INSTANCE.severe("Error sending message", e);
                }
            });
        });

        input.add(sendButton); // jah89 07-20-2024

        exportButton = new JButton("Export Chat");
        exportButton.addActionListener((event) -> {  // jah89 07-26-2024
            SwingUtilities.invokeLater(() -> {
                try {
                    exportChatHistory();
                } catch (IOException e) {
                    LoggerUtil.INSTANCE.severe("Error exporting chat history", e);
                }
            });
        });

        input.add(exportButton); // jah89 07-26-2024

        this.add(splitPane, BorderLayout.CENTER);
        this.add(input, BorderLayout.SOUTH);

        this.setName(CardView.CHAT.name());
        controls.addPanel(CardView.CHAT.name(), this);

        chatArea.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (chatArea.isVisible()) {
                        chatArea.revalidate();
                        chatArea.repaint();
                    }
                });
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (chatArea.isVisible()) {
                        chatArea.revalidate();
                        chatArea.repaint();
                    }
                });
            }
        });

        // Add vertical glue to push messages to the top
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; // Column index 0
        gbc.gridy = GridBagConstraints.RELATIVE; // Automatically move to the next row
        gbc.weighty = 1.0; // Give extra space vertically to this component
        gbc.fill = GridBagConstraints.BOTH; // Fill both horizontally and vertically
        chatArea.add(Box.createVerticalGlue(), gbc);
    }

    private void exportChatHistory() throws IOException { // jah89 07-24-2024
        StringBuilder chatContent = new StringBuilder();
        chatContent.append(chatHistory.getText());  

        String fileName = String.format("chat_history_%s.txt", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(chatContent.toString());
        }
        chatHistory.append("Chat history exported to " + fileName + "\n");
    }

    public void updateUserStatus(long clientId, boolean isMuted, boolean isActive) { // jah89 07-26-2024
        SwingUtilities.invokeLater(() -> {
            UserListItem userItem = userListPanel.getUserItem(clientId);
            if (userItem != null) {
                if (isMuted) {
                    userItem.setMuted(true);
                } else {
                    userItem.setMuted(false);
                }
                if (isActive) {
                    userItem.setActive(true);
                } else {
                    userItem.setActive(false);
                }
            }
        });
    }

    public void addUserListItem(long clientId, String clientName) { // jah89 07-26-2024
        userListPanel.addUserListItem(clientId, clientName);
    }

    public void removeUserListItem(long clientId) { // jah89 07-26-2024
        userListPanel.removeUserListItem(clientId);
    }

    public void clearUserList() { // jah89 07-26-2024
        userListPanel.clearUserList();
    }

    public void addText(String text) { // jah89 07-26-2024
        SwingUtilities.invokeLater(() -> {
            JEditorPane textContainer = new JEditorPane("text/plain", text);
            textContainer.setEditable(false);
            textContainer.setBorder(BorderFactory.createEmptyBorder());

            // Account for the width of the vertical scrollbar
            JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatArea);
            int scrollBarWidth = parentScrollPane.getVerticalScrollBar().getPreferredSize().width;

            // Adjust the width of the text container
            int availableWidth = chatArea.getWidth() - scrollBarWidth - 10; // Subtract an additional padding
            textContainer.setSize(new Dimension(availableWidth, Integer.MAX_VALUE));
            Dimension d = textContainer.getPreferredSize();
            textContainer.setPreferredSize(new Dimension(availableWidth, d.height));
            // Remove background and border
            textContainer.setOpaque(false);
            textContainer.setBorder(BorderFactory.createEmptyBorder());
            textContainer.setBackground(new Color(0, 0, 0, 0));

            // GridBagConstraints settings for each message
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; // Column index 0
            gbc.gridy = GridBagConstraints.RELATIVE; // Automatically move to the next row
            gbc.weightx = 1; // Let the component grow horizontally to fill the space
            gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
            gbc.insets = new Insets(0, 0, 5, 0); // Add spacing between messages

            chatArea.add(textContainer, gbc);
            chatArea.revalidate();
            chatArea.repaint();

            // Scroll down on new message
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = parentScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }
}
