package Project.common;

public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passing of initialization data [name])
    CLIENT_ID,  // server sending client id
    SYNC_CLIENT,  // silent syncing of clients in room
    DISCONNECT,  // distinct disconnect action
    ROOM_CREATE,
    ROOM_JOIN, // join/leave room based on boolean
    MESSAGE,    // sender and message
    ROOM_LIST, 
    ROLL, //added 07/03/2024 to handle dice roll and flip 
    FLIP,
    MUTE, //jah89 07-20-2024
    UNMUTE,
    PRIVATE_MESSAGE //jah89 07-20-2024
}