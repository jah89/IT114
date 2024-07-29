package Project.client;

import Project.client.Interfaces.ICardControls;
import Project.client.Interfaces.IConnectionEvents;
import Project.client.Interfaces.IMessageEvents;
import Project.client.Interfaces.IRoomEvents;
import Project.client.Views.ChatPanel;
import Project.client.Views.ConnectionPanel;
import Project.client.Views.Menu;
import Project.client.Views.RoomsPanel;
import Project.client.Views.UserDetailsPanel;
import Project.common.LoggerUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel; 
import javax.swing.SwingUtilities; 

public class ClientUI extends JFrame implements IConnectionEvents, IMessageEvents, IRoomEvents, ICardControls {
    private static ClientUI instance; // Singleton instance

    private CardLayout card = new CardLayout();
    private Container container;
    private JPanel cardContainer;
    private String originalTitle;
    private JPanel currentCardPanel;
    private CardView currentCard = CardView.CONNECT;
    private JMenuBar menu;
    private ConnectionPanel connectionPanel;
    private UserDetailsPanel userDetailsPanel;
    private ChatPanel chatPanel;
    private RoomsPanel roomsPanel;
    private JLabel roomLabel = new JLabel();

    {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        LoggerUtil.INSTANCE.setConfig(config);
    }

    public static ClientUI getInstance() {
        if (instance == null) {
            instance = new ClientUI("JAH89-Client");
        }
        return instance;
    }

    private ClientUI(String title) {
        super(title);
        originalTitle = title;
        container = getContentPane();
        cardContainer = new JPanel();
        cardContainer.setLayout(card);
        container.add(roomLabel, BorderLayout.NORTH);
        container.add(cardContainer, BorderLayout.CENTER);

        cardContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                cardContainer.setPreferredSize(e.getComponent().getSize());
                cardContainer.revalidate();
                cardContainer.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }
        });

        setMinimumSize(new Dimension(400, 400));
        setLocationRelativeTo(null);
        menu = new Menu(this);
        this.setJMenuBar(menu);

        connectionPanel = new ConnectionPanel(this);
        userDetailsPanel = new UserDetailsPanel(this);
        chatPanel = new ChatPanel(this);
        roomsPanel = new RoomsPanel(this);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                int response = JOptionPane.showConfirmDialog(cardContainer,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        Client.INSTANCE.sendDisconnect();
                    } catch (NullPointerException | IOException e) {
                        LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }
        });

        pack();
        setVisible(true);
    }

    private void findAndSetCurrentPanel() {
        for (Component c : cardContainer.getComponents()) {
            if (c.isVisible()) {
                currentCardPanel = (JPanel) c;
                currentCard = Enum.valueOf(CardView.class, currentCardPanel.getName());
                if (Client.INSTANCE.getMyClientId() == ClientData.DEFAULT_CLIENT_ID
                        && currentCard.ordinal() >= CardView.CHAT.ordinal()) {
                    show(CardView.CONNECT.name());
                }
                break;
            }
        }
        LoggerUtil.INSTANCE.fine("Current panel: " + currentCardPanel.getName());
    }

    @Override
    public void next() {
        card.next(cardContainer);
        findAndSetCurrentPanel();
    }

    @Override
    public void previous() {
        card.previous(cardContainer);
        findAndSetCurrentPanel();
    }

    @Override
    public void show(String cardName) {
        card.show(cardContainer, cardName);
        findAndSetCurrentPanel();
    }

    @Override
    public void addPanel(String cardName, JPanel panel) {
        cardContainer.add(panel, cardName);
    }

    @Override
    public void connect() {
        String username = userDetailsPanel.getUsername();
        String host = connectionPanel.getHost();
        int port = connectionPanel.getPort();
        setTitle(originalTitle + " - " + username);
        Client.INSTANCE.connect(host, port, username, this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> getInstance());
    }

    @Override
    public void onClientDisconnect(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatPanel.removeUserListItem(clientId);
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s disconnected*",
                    isMe ? "You" : String.format("%s[%s]", clientName, clientId));
            chatPanel.addText(message);
            if (isMe) {
                LoggerUtil.INSTANCE.info("I disconnected");
                previous();
            }
        }
    }

    @Override
    public void onMessageReceive(long clientId, String message) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            String clientName = Client.INSTANCE.getClientNameFromId(clientId);
            chatPanel.addText(String.format("%s[%s]: %s", clientName, clientId, message));
        }
    }

    @Override
    public void onReceiveClientId(long id) {
        show(CardView.CHAT.name());
        chatPanel.addText("*You connected*");
    }

    @Override
    public void onResetUserList() {
        chatPanel.clearUserList();
    }

    @Override
    public void onSyncClient(long clientId, String clientName) {
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            chatPanel.addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        roomsPanel.removeAllRooms();
        if (message != null && !message.isEmpty()) {
            roomsPanel.setMessage(message);
        }
        if (rooms != null) {
            for (String room : rooms) {
                roomsPanel.addRoom(room);
            }
        }
    }

    @Override
    public void onRoomAction(long clientId, String clientName, String roomName, boolean isJoin) {
        LoggerUtil.INSTANCE.info("Current card: " + currentCard.name());
        if (currentCard.ordinal() >= CardView.CHAT.ordinal()) {
            boolean isMe = clientId == Client.INSTANCE.getMyClientId();
            String message = String.format("*%s %s the Room %s*",
                    isMe ? "You" : String.format("%s[%s]", clientName, clientId),
                    isJoin ? "joined" : "left",
                    roomName == null ? "" : roomName); 
            chatPanel.addText(message);
            if (isJoin) {
                roomLabel.setText("Room: " + roomName);
                chatPanel.addUserListItem(clientId, String.format("%s (%s)", clientName, clientId));
            } else {
                chatPanel.removeUserListItem(clientId);
            }
        }
    }

    public void updateUserStatus(long clientId, boolean isMuted, boolean isActive) {
        chatPanel.updateUserStatus(clientId, isMuted, isActive);
    }
}
