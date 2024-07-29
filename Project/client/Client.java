package Project.client;

import Project.client.Interfaces.IClientEvents;
import Project.client.Interfaces.IConnectionEvents;
import Project.client.Interfaces.IMessageEvents;
import Project.client.Interfaces.IRoomEvents;
import Project.common.ConnectionPayload;
import Project.common.LoggerUtil;
import Project.common.Payload;
import Project.common.PayloadType;
import Project.common.RollPayload;
import Project.common.RoomResultsPayload;
import Project.common.TextFX;
import Project.common.TextFX.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    {
        // TODO moved to ClientUI (this repeat doesn't do anything since config is set
        // only once)
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }

    private Random random = new Random(); // Adding a random num generator jah89 07/03/2024
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private ConcurrentHashMap<Long, ClientData> knownClients = new ConcurrentHashMap<>();
    private ClientData myData;

    // constants (used to reduce potential types when using them in code)
    private final String COMMAND_CHARACTER = "/";
    private final String CREATE_ROOM = "createroom";
    private final String JOIN_ROOM = "joinroom";
    private final String LIST_ROOMS = "listrooms";
    private final String DISCONNECT = "disconnect";
    private final String LOGOFF = "logoff";
    private final String LOGOUT = "logout";
    private final String SINGLE_SPACE = " ";
    private long clientId;

    // callback that updates the UI
    private static IClientEvents events;

    // needs to be private now that the enum logic is handling this
    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
        myData = new ClientData();
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }
    public long getClientId() {
        return clientId;
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    @Deprecated
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            LoggerUtil.INSTANCE.warning("Unknown host", e);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("IOException", e);
        }
        return isConnected();
    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @param username
     * @param callback (for triggering UI events)
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        myData.setClientName(username);
        Client.events = callback;
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
            sendClientName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     * @throws IOException
     */
    private boolean processClientCommand(String text) throws IOException {
        if (text.startsWith("/mute")) { // jah89 07-20-2024
            String targetName = text.replace("/mute", "").trim();
            if (targetName.isEmpty() || !knownClients.values().stream().anyMatch(c -> c.getClientName().equals(targetName))) {
                System.out.println("User not found.");
            } else {
                sendMute(targetName);
            }
            return true;
        } else if (text.startsWith("/unmute")) { // jah89 07-20-2024
            String targetName = text.replace("/unmute", "").trim();
            if (targetName.isEmpty() || !knownClients.values().stream().anyMatch(c -> c.getClientName().equals(targetName))) {
                System.out.println("User not found.");
            } else {
                sendUnmute(targetName);
            }
            return true;
        }
        if (isConnection(text)) {
            if (myData.getClientName() == null || myData.getClientName().length() == 0) {
                System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
                return true;
            }
            // replaces multiple spaces with a single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            sendClientName();
            return true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            return true;
        } else if (text.startsWith("/name")) {
            myData.setClientName(text.replace("/name", "").trim());
            System.out.println(TextFX.colorize("Set client name to " + myData.getClientName(), Color.CYAN));
            return true;
        } else if (text.equalsIgnoreCase("/users")) {
            System.out.println(
                    String.join("\n", knownClients.values().stream()
                            .map(c -> String.format("%s(%s)", c.getClientName(), c.getClientId())).toList()));
            return true;
        } else if (text.startsWith("/roll")) { // Handle roll jah90 07/03/2024
            handleRollCommand(text);
            return true;
        } else if (text.equalsIgnoreCase("/flip")) { // Handle flip jah89
            handleFlipCommand();
            return true;
        } else { // logic previously from Room.java
            // decided to make this as separate block to separate the core client-side items
            // vs the ones that generally are used after connection and that send requests
            if (text.startsWith(COMMAND_CHARACTER)) {
                boolean wasCommand = false;
                String fullCommand = text.replace(COMMAND_CHARACTER, "");
                String part1 = fullCommand;
                String[] commandParts = part1.split(SINGLE_SPACE, 2);// using limit so spaces in the command value
                                                                     // aren't split
                final String command = commandParts[0];
                final String commandValue = commandParts.length >= 2 ? commandParts[1] : "";
                switch (command) {
                    case CREATE_ROOM:
                        sendCreateRoom(commandValue);
                        wasCommand = true;
                        break;
                    case JOIN_ROOM:
                        sendJoinRoom(commandValue);
                        wasCommand = true;
                        break;
                    case LIST_ROOMS:
                        sendListRooms(commandValue);
                        wasCommand = true;
                        break;
                    // Note: these are to disconnect, they're not for changing rooms
                    case DISCONNECT:
                    case LOGOFF:
                    case LOGOUT:
                        sendDisconnect();
                        wasCommand = true;
                        break;
                }
                return wasCommand;
            }
        }
        return false;
    }

    public long getMyClientId() {
        return myData.getClientId();
    }
    // send methods to pass data to the ServerThread

    /**
     * Sends a search to the server-side to get a list of potentially matching Rooms
     * 
     * @param roomQuery optional partial match search String
     * @throws IOException
     */
    public void sendListRooms(String roomQuery) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_LIST);
        p.setMessage(roomQuery);
        send(p);
    }

    /**
     * Sends the room name we intend to create
     * 
     * @param room
     * @throws IOException
     */
    public void sendCreateRoom(String room) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_CREATE);
        p.setMessage(room);
        send(p);
    }

    /**
     * Sends the room name we intend to join
     * 
     * @param room
     * @throws IOException
     */
    public void sendJoinRoom(String room) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_JOIN);
        p.setMessage(room);
        send(p);
    }

    /**
     * Tells the server-side we want to disconnect
     * 
     * @throws IOException
     */
    void sendDisconnect() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    /**
     * Sends desired message over the socket
     * 
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        if (processClientCommand(message)) {
            return;
        }
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        send(p);
    }

    /**
     * Sends chosen client name after socket handshake
     * 
     * @throws IOException
     */
    private void sendClientName() throws IOException {
        if (myData.getClientName() == null || myData.getClientName().length() == 0) {
            System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
            return;
        }
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientName(myData.getClientName());
        send(cp);
    }

    /**
     * Generic send that passes any Payload over the socket (to ServerThread)
     * 
     * @param p
     * @throws IOException
     */
    private void send(Payload p) throws IOException {
        try {
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Socket send exception", e);
            throw e;
        }

    }
    // end send methods

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    // System.out.println(fromServer);
                    processPayload(fromServer);
                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            LoggerUtil.INSTANCE.severe("Error reading object as specified type: ", cce);
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.INSTANCE.info("Connection dropped", e);
            }
        } finally {
            closeServerConnection();
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

    /**
     * Listens for keyboard input from the user
     */
    @Deprecated
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); // moved here to avoid console spam
            while (isRunning) { // Run until isRunning is false
                String line = si.nextLine();
                LoggerUtil.INSTANCE.severe(
                        "You shouldn't be using terminal input for Milestone 3. Interaction should be done through the UI");
                if (!processClientCommand(line)) {
                    if (isConnected()) {
                        sendMessage(line);
                    } else {
                        System.out.println(
                                "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error in listentToInput()", e);
        }
        LoggerUtil.INSTANCE.info("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        myData.reset();
        knownClients.clear();
        try {
            if (out != null) {
                LoggerUtil.INSTANCE.info("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing output stream", e);
        }
        try {
            if (in != null) {
                LoggerUtil.INSTANCE.info("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing input stream", e);
        }
        try {
            if (server != null) {
                LoggerUtil.INSTANCE.info("Closing connection");
                server.close();
                LoggerUtil.INSTANCE.info("Closed socket");
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Error closing socket", e);
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Exception from main()", e);
        }
    }

    /**
     * Handles received message from the ServerThread
     * 
     * @param payload
     */
    private void processPayload(Payload payload) {
        try {
            LoggerUtil.INSTANCE.info("Received Payload: " + payload);
            switch (payload.getPayloadType()) {
                case CLIENT_ID: // get id assigned
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    processClientData(cp.getClientId(), cp.getClientName());
                    break;
                case SYNC_CLIENT: // silent add
                    cp = (ConnectionPayload) payload;
                    processClientSync(cp.getClientId(), cp.getClientName());
                    break;
                case DISCONNECT: // remove a disconnected client (mostly for the specific message vs leaving a room)
                    cp = (ConnectionPayload) payload;
                    processDisconnect(cp.getClientId(), cp.getClientName());
                    // note: we want this to cascade
                case ROOM_JOIN: // add/remove client info from known clients
                    cp = (ConnectionPayload) payload;
                    processRoomAction(cp.getClientId(), cp.getClientName(), cp.getMessage(), cp.isConnect());
                    break;
                case ROOM_LIST:
                    RoomResultsPayload rrp = (RoomResultsPayload) payload;
                    processRoomsList(rrp.getRooms(), rrp.getMessage());
                    break;
                case MESSAGE: // displays a received message
                    processMessage(payload.getClientId(), payload.getMessage());
                    break;
                case ROLL:
                    RollPayload rollPayload = (RollPayload) payload;
                    processRollPayload(rollPayload);
                    break;
                case FLIP:
                    processFlipPayload(payload);
                    break;
                case MUTE_STATUS: // handle mute status update // jah89 07-27-2024
                    handleMuteStatus(payload);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);
        }
    }
    
    // jah89 07-27-2024
    private void handleMuteStatus(Payload payload) {
        long clientId = payload.getClientId();
        boolean isMuted = "MUTED".equals(payload.getMessage());
        ClientUI.getInstance().updateUserStatus(clientId, isMuted, false); 
    }
    /**
     * Processes a RollPayload object received from the server.
     * 
     * @param rollPayload The RollPayload object.
     */
    private void processRollPayload(RollPayload rollPayload) {
        // Extract roll details from rollPayload and display them
        String message = rollPayload.getMessage();
        System.out.println(TextFX.colorize(message, Color.BLUE));
        // invoke onMessageReceive callback
        ((IMessageEvents) events).onMessageReceive(rollPayload.getClientId(), message);
    }

    /**
     * Processes a flip payload received from the server.
     * 
     * @param flipPayload The flip payload object.
     */
    private void processFlipPayload(Payload flipPayload) {
        // Extract flip result from flipPayload and display it
        String message = flipPayload.getMessage();
        System.out.println(TextFX.colorize(message, Color.BLUE));
        // invoke onMessageReceive callback
        ((IMessageEvents) events).onMessageReceive(flipPayload.getClientId(), message);
    }

    /**
     * Returns the ClientName of a specific Client by ID.
     * 
     * @param id
     * @return the name, or Room if id is -1, or [Unknown] if failed to find
     */
    public String getClientNameFromId(long id) {
        if (id == ClientData.DEFAULT_CLIENT_ID) {
            return "Room";
        }
        if (knownClients.containsKey(id)) {
            return knownClients.get(id).getClientName();
        }
        return "[Unknown]";
    }

    // payload processors
    private void processRoomsList(List<String> rooms, String message) {
        // invoke onReceiveRoomList callback
        ((IRoomEvents) events).onReceiveRoomList(rooms, message);
        if (rooms == null || rooms.size() == 0) {
            System.out.println(
                    TextFX.colorize("No rooms found matching your query",
                            Color.RED));
            return;
        }
        System.out.println(TextFX.colorize("Room Results:", Color.PURPLE));
        System.out.println(
                String.join("\n", rooms));

    }

    private void processDisconnect(long clientId, String clientName) {
        // invoke onClientDisconnect callback
        ((IConnectionEvents) events).onClientDisconnect(clientId, clientName);
        System.out.println(
                TextFX.colorize(String.format("*%s disconnected*",
                        clientId == myData.getClientId() ? "You" : clientName),
                        Color.RED));
        if (clientId == myData.getClientId()) {
            closeServerConnection();
        }
    }

    private void processClientData(long clientId, String clientName) {

        if (myData.getClientId() == ClientData.DEFAULT_CLIENT_ID) {
            myData.setClientId(clientId);
            myData.setClientName(clientName);
            // invoke onReceiveClientId callback
            ((IConnectionEvents) events).onReceiveClientId(clientId);
            // knownClients.put(cp.getClientId(), myData);// <-- this is handled later
        }
    }

    private void processMessage(long clientId, String message) {
        String name = knownClients.containsKey(clientId) ? knownClients.get(clientId).getClientName() : "Room";
        System.out.println(TextFX.colorize(String.format("%s: %s", name, message), Color.BLUE));
        // invoke onMessageReceive callback
        ((IMessageEvents) events).onMessageReceive(clientId, message);
    }

    private void processClientSync(long clientId, String clientName) {

        if (!knownClients.containsKey(clientId)) {
            ClientData cd = new ClientData();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            // invoke onSyncClient callback
            ((IConnectionEvents) events).onSyncClient(clientId, clientName);
        }
    }

    private void processRoomAction(long clientId, String clientName, String message, boolean isJoin) {

        if (isJoin && !knownClients.containsKey(clientId)) {
            ClientData cd = new ClientData();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            System.out.println(TextFX
                    .colorize(String.format("*%s[%s] joined the Room %s*", clientName, clientId, message),
                            Color.GREEN));
            // invoke onRoomJoin callback
            ((IRoomEvents) events).onRoomAction(clientId, clientName, message, isJoin);
        } else if (!isJoin) {
            ClientData removed = knownClients.remove(clientId);
            if (removed != null) {
                System.out.println(
                        TextFX.colorize(String.format("*%s[%s] left the Room %s*", clientName, clientId, message),
                                Color.YELLOW));
                // invoke onRoomJoin callback
                ((IRoomEvents) events).onRoomAction(clientId, clientName, message, isJoin);
            }
            // clear our list
            if (clientId == myData.getClientId()) {
                knownClients.clear();
                // invoke onResetUserList()
                ((IConnectionEvents) events).onResetUserList();
            }
        }
    }

    private void handleRollCommand(String text) {   //jah89 07/03/2024
        try {
            RollPayload rollPayload = new RollPayload();
            String[] parts = text.split(" ");
            if (parts.length == 2) {
                String rollCommand = parts[1];
                if (rollCommand.matches("\\d+")) {
                    int max = Integer.parseInt(rollCommand);
                    int result = random.nextInt(max + 1);
                    rollPayload.setSides(max);
                    rollPayload.setRolls(1);
                    rollPayload.setMessage(String.format("%s rolled %d and got %d", myData.getClientName(), max, result));
                    System.out.println(rollPayload.getMessage());
                } else if (rollCommand.matches("\\d+d\\d+")) {  //jah89 07-07-2024
                    String[] diceParts = rollCommand.split("d");
                    int rolls = Integer.parseInt(diceParts[0]);
                    int sides = Integer.parseInt(diceParts[1]);
                    int result = 0;
                    for (int i = 0; i < rolls; i++) {
                        result += random.nextInt(sides) + 1;
                    }
                    rollPayload.setSides(sides);
                    rollPayload.setRolls(rolls);
                    rollPayload.setMessage(String.format("%s rolled %sd%d and got %d", myData.getClientName(), rolls, sides, result));
                    System.out.println(rollPayload.getMessage());
                }
                rollPayload.setPayloadType(PayloadType.ROLL);
                send(rollPayload);
            }
        } catch (IOException e) {
            e.printStackTrace();
            LoggerUtil.INSTANCE.severe("Error sending roll payload", e);
        }
    }
    
    private void handleFlipCommand() { 
        try {
            Payload flipPayload = new Payload();
            flipPayload.setPayloadType(PayloadType.FLIP);
            String result = random.nextBoolean() ? "heads" : "tails";
            flipPayload.setMessage(String.format("%s flipped a coin and got %s", myData.getClientName(), result));
            System.out.println(flipPayload.getMessage());
            send(flipPayload);
        } catch (IOException e) {
            e.printStackTrace();
            LoggerUtil.INSTANCE.severe("Error sending flip payload", e);
        }
    }
    public void sendMute(String targetName) throws IOException { // jah89 07-20-2024
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MUTE);
        p.setMessage(targetName);
        send(p);
    }
    public void sendUnmute(String targetName) throws IOException { // jah89 07-20-2024
        Payload p = new Payload();
        p.setPayloadType(PayloadType.UNMUTE);
        p.setMessage(targetName);
        send(p);
    }
    public void sendPrivateMessage(long targetId, String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PRIVATE_MESSAGE); // Add this new PayloadType
        p.setClientId(targetId);
        p.setMessage(message);
        send(p);
    }
    public long getClientIdByName(String name) {
        for (ClientData client : knownClients.values()) {
            if (client.getClientName().equalsIgnoreCase(name)) {
                return client.getClientId();
            }
        }
        return -1; // Client not found
    }
    // end payload processors
}
