package server;
// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 


import common.ChatIF;
import  ocsf.server.*;
import utils.SCUtilities;
import java.io.IOException;
import java.util.HashMap;

/**
 * Concrete class implementing AbstractServer of the OCSF framework
 * @author Pranav Kural
 * Student number: 300241227
 */
public class EchoServer extends AbstractServer
{
  //Class variables *************************************************

  /**
   * The interface type variable.  It allows the implementation of
   * the display method in the server.
   */
  ChatIF serverUI;

  /**
   * Specify the prefix character used to identify commands
   */
  private static final String COMMAND_PREFIX = "#";

  /**
   * Specify signature of the key value used for storing client's login id
   */
  private static final String CLIENT_LOGIN_ID_KEY = "loginID";

  /**
   * Specify signature of the command sent by client to set login id
   */
  public static final String CLIENT_SET_LOGIN_ID_COMMAND = "#login";

  /**
   * Specify signature of the command used to logoff (disconnect) a client
   */
  private static final String CLIENT_LOGOFF_COMMAND = "#logoff";

  /**
   * Specify delimiter used to separate command from arguments provided with it
   */
  private static final String COMMAND_ARGUMENT_SEPARATOR = "\\s+";


  // define a structure for all commands
  // to avoid using hard-coded strings in multiple places
  private enum COMMANDS {
    quit, // quit gracefully
    stop, // stop listening
    close, // stop listening and disconnect all clients
    setport, // set new port (only when not listening)
    start, // start listening
    getport, // get the port server is listening on
  }

  // list of accepted commands (common to all clients)
  // storing in a HashMap for quickly checking if a given string value is a valid command (in O(1))
  private final static HashMap<String, Boolean> ACCEPTED_COMMANDS = new HashMap<>() {{
    put(COMMANDS.quit.name(), true);
    put(COMMANDS.stop.name(), true);
    put(COMMANDS.close.name(), true);
    put(COMMANDS.setport.name(), true);
    put(COMMANDS.start.name(), true);
    put(COMMANDS.getport.name(), true);
  }};
  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) 
  {
    super(port);
  }

  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port, ChatIF serverUI)
  {
    super(port);
    this.serverUI = serverUI;
  }
  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient(Object msg, ConnectionToClient client)
  {
    // guard-clause
    if (client == null) {
      System.out.println("Invalid call to handle message from client without providing a client instance");
      return;
    } else if (msg == null || msg.toString().isEmpty()) {
      System.out.println("Invalid message received from client: " + client);
      return;
    }

    String msgStr = (String) msg;

    if (msgStr.startsWith(COMMAND_PREFIX)) {
      // if msg received from client is "#login" command
      if (msgStr.startsWith(CLIENT_SET_LOGIN_ID_COMMAND)) {
        try {
          // display a message
          serverUI.display("Message received: #login <loginID> from null.");
          // set client's loginId
          setClientLoginId(msgStr, client);
          // print server message indicating success in logging in client
          serverUI.display(client.getInfo(CLIENT_LOGIN_ID_KEY) + " has logged on.");
          // let client know connecting was successful
          sendMessageToClient(client.getInfo(CLIENT_LOGIN_ID_KEY) + " has logged on.", client);
        } catch (Exception e) {
          sendMessageToClient("Failed to set login id. Error: " + e.getMessage(), client);
        }
      }

      // if msg received from client is "#logoff" command
      else if (msgStr.startsWith(CLIENT_LOGOFF_COMMAND)) {
        try {
          // disconnect the client
          client.close();
        } catch (Exception e) {
          sendMessageToClient("Failed to logoff client. Error: " + e.getMessage(), client);
        }
      }

      else {
        sendMessageToClient("Invalid command: " + msgStr, client);
      }

    }

    // else if message received from the client is not a command
    else {
      // make sure client has set the login id before sending any messages
      // this also ensures, client sent login command as first thing after establishing connection
      if (client.getInfo(CLIENT_LOGIN_ID_KEY) != null) {
        // send message to the server
        System.out.println("Message received: " + msg + " from " + client.getInfo(CLIENT_LOGIN_ID_KEY));
        this.sendToAllClients(client.getInfo(CLIENT_LOGIN_ID_KEY) + ": " + msg);
      } else {
        sendMessageToClient("Invalid request received. " + CLIENT_LOGIN_ID_KEY + " must be the first command after connection has established. Terminating connection.", client);
        try {
          // close client connection
          client.close();
        } catch (IOException e) {
          System.out.println("Unable to close client connection");
        }
      }

    }
  }
    
  /**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
    System.out.println("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
    System.out.println("Server has stopped listening for connections.");
  }

  /**
   * Hook method called each time a new client connection is accepted
   * @param client the connection connected to the client
   */
  @Override
  protected void clientConnected(ConnectionToClient client) {
    System.out.println("A new client has connected to the server.");
  }

  /**
   * Hook method called each time a client disconnects.
   * @param client the connection with the client.
   */
  @Override
  synchronized protected void clientDisconnected(ConnectionToClient client) {
    System.out.println("Connection disconnected with a client");
  }

  /**
   *
   * @param client the client that raised the exception.
   * @param exception
   */
  @Override
  protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
    System.out.println("Connection disconnected with client: " + client);
  }

  /**
   * Method to handle input from user from the Server UI console
   * @param userInput the user's input on server UI
   */
  public void handleMessageFromServerUI(String userInput) {
    // guard-clause
    if (userInput == null) {
      this.serverUI.display("Invalid userInput received from Server UI");
      return;
    }

    try
    {
      // if userInput is a command
      if (userInput.startsWith(COMMAND_PREFIX)) {
        handleCommandFromServerUI(userInput);
      } else {
        if (getNumberOfClients() == 0) {
          this.serverUI.display("No clients connected");
        } else {
          // print the userInput to all connected clients
          this.sendToAllClients("SERVER MSG> " + userInput);
        }

        // print userInput on serverUI as well
        this.serverUI.display(userInput);
      }
    }
    catch(IOException e)
    {
      this.serverUI.display("Unable to handle the userInput");
    }
  }

  /**
   * Method to handle commands input by user on the Server UI console
   * @param serverCommand command for server to handle
   * @throws IOException throws IOException for invalid command or other errors
   */
  private void handleCommandFromServerUI(String serverCommand) throws IOException {

    // guard-clause
    if (serverCommand == null || serverCommand.isEmpty()) {
      throw new IOException("No command provided!");
    }

    // get command and command args separately
    String[] commandAndArgs = SCUtilities.extractCommandAndArgs(serverCommand, COMMAND_ARGUMENT_SEPARATOR);
    // guard-clause, check if processed input is not null
    if (!SCUtilities.isValidArray(commandAndArgs)) {
      throw new NullPointerException("Invalid value provided for command & arguments (msgStr)");
    }
    // extract command and args separately
    String command = commandAndArgs[0].substring(1); // get command name without the COMMAND_PREFIX
    String commandArgs = commandAndArgs[1]; // will be null if no args supplied

    // check if the command is valid (not empty, or unaccepted)
    if (!SCUtilities.isValidCommand(command, ACCEPTED_COMMANDS)) {
      this.serverUI.display("not a valid command: " + COMMAND_PREFIX + command);
      return;
    }

    // stop listening and shut down the server
    if (command.equals(COMMANDS.quit.name())) {
      close();
      // close ServerConsole UI
      System.exit(0);
    }

    // stop listening for connections
    else if (command.equals(COMMANDS.stop.name())) {
      if (isListening()) {
        stopListening();
      } else {
        // if already not listening
        this.serverUI.display("Invalid command! Server is already not listening for connections!");
      }
    }

    // disconnect all clients, then stop server gracefully
    else if (command.equals(COMMANDS.close.name())) {
      // make sure client is currently active
      if (isListening()) {
        // first stop listening for new connections, so no new clients get added while we disconnect existing ones
        stopListening();
        // disconnect all connected clients
        disconnectAllClients();
        // finally, close connection
        close();
      } else {
        // if already not listening
        this.serverUI.display("Invalid command! Server is already not listening for connections!");
      }
    }

    // setting a new port
    else if (command.equals(COMMANDS.setport.name())) {
      // make sure client is not already connected
      if (isListening()) {
        this.serverUI.display("Can not change port while server is listening for connections. Please close the server first (" + COMMAND_PREFIX + "close), then try again.");
      }
      // validate command arguments
      if (!SCUtilities.isValidString(commandArgs)) {
        this.serverUI.display("Invalid command argument, no port number provided.");
      } else {
        int port;
        // validate input
        try {
          port = Integer.parseInt(commandArgs);
          // set the new port if parsing was successful
          setPort(port);
        } catch (NumberFormatException e) {
          this.serverUI.display("Invalid value provided for port number: " + commandArgs);
        }
      }
    }

    // start server to listen for connections
    else if (command.equals(COMMANDS.start.name())) {
      // make sure server is not already running
      if (isListening()) {
        this.serverUI.display("Invalid command! Server is already listening on port " + getPort());
      } else {
        // start listening for connections
        listen();
      }
    }

    // get server's port
    else if (command.equals(COMMANDS.getport.name())) {
      // display port
      this.serverUI.display("Current server port: " + getPort());
    }

    // not one of the yet implemented accepted commands
    else {
      this.serverUI.display("Command not available yet");
    }
  }

  private void disconnectAllClients() throws IOException {
    Thread[] clientThreads = getClientConnections();
    ConnectionToClient client;
    for (Thread clientThread : clientThreads) {
      client = (ConnectionToClient) clientThread;
      client.close();
      // print a message - Testcase 2008
      serverUI.display(client.getInfo(CLIENT_LOGIN_ID_KEY) + " has disconnected.");
    }
  }

  private void setClientLoginId(String msgStr, ConnectionToClient client) {
    // guard-clause
    if (msgStr == null || msgStr.isEmpty() || client == null) {
      throw new NullPointerException("Method setClientLoginId called with invalid arguments");
    }
    // process input to get command and args separately
    String[] loginInput = SCUtilities.extractCommandAndArgs(msgStr, COMMAND_ARGUMENT_SEPARATOR);

    // guard-clause, check if processed input is not null
    if (!SCUtilities.isValidArray(loginInput)) {
      throw new NullPointerException("Invalid value provided for command & arguments (msgStr)");
    }

    // validate command
    if (!loginInput[0].equals(CLIENT_SET_LOGIN_ID_COMMAND)) {
      throw new IllegalArgumentException("Method setClientLoginId called with invalid command value: " + loginInput[0]);
    }
    // validate existence of argument (loginId value)
    else if (loginInput.length < 2 || loginInput[1] == null) {
      throw new IllegalArgumentException("No value provided for login id");
    }

    // set client's login id
    client.setInfo(CLIENT_LOGIN_ID_KEY, loginInput[1]);
  }

  //Class methods ***************************************************

  /**
   * A helper method to send a message to a specific client
   * Used for sending error or other informative messages to specific client
   * @param message message to be sent
   * @param client client to which message needs to be sent
   */
  private void sendMessageToClient(String message, ConnectionToClient client) {
    try {
      client.sendToClient(message);
    } catch (IOException e) {
      System.out.println("Failed to send message to the client. Error: " + e.getMessage());
    }
  }

}
//End of server.EchoServer class
