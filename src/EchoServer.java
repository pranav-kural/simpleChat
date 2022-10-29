// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 


import client.ChatClient;
import common.ChatIF;
import  ocsf.server.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class EchoServer extends AbstractServer
{
  //Class variables *************************************************

  /**
   * The interface type variable.  It allows the implementation of
   * the display method in the server.
   */
  ChatIF serverUI;

  // define a structure for all commands
  // to avoid using hard-coded strings in multiple places
  private enum COMMANDS {
    quit, // quit gracefully
    stop, // stop listening
    close, // stop listening and disconnect all clients
    setport, // set new port (only when not listening)
    start, // start listening
    getport // get the port server is listening on
  }

  // list of accepted commands (common to all clients)
  // storing in a HashMap for quickly checking if a given string value if a valid command (in O(1))
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
  public EchoServer(int port, ServerConsole serverUI)
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
  public void handleMessageFromClient
    (Object msg, ConnectionToClient client)
  {
    System.out.println("Message received: " + msg + " from " + client);
    this.sendToAllClients(msg);
  }
    
  /**
   * This method overrides the one in the superclass.  Called
   * when the server starts listening for connections.
   */
  protected void serverStarted()
  {
    System.out.println
      ("Server listening for connections on port " + getPort());
  }
  
  /**
   * This method overrides the one in the superclass.  Called
   * when the server stops listening for connections.
   */
  protected void serverStopped()
  {
    System.out.println
      ("Server has stopped listening for connections.");
  }

  /**
   * Hook method called each time a new client connection is accepted
   * @param client the connection connected to the client
   */
  @Override
  protected void clientConnected(ConnectionToClient client) {
    System.out.println("Connection established with a new client. Number of connected clients: " + getNumberOfClients());
  }

  /**
   * Hook method called each time a client disconnects.
   * @param client the connection with the client.
   */
  @Override
  synchronized protected void clientDisconnected(ConnectionToClient client) {
    System.out.println("Connection disconnected with a client. Number of connected clients: " + getNumberOfClients());
  }

  public void handleMessageFromServerUI(String message) {
    // guard-clause
    if (message == null) {
      this.serverUI.display("Invalid message received from Server UI");
      return;
    }

    try
    {
      // if message is a command
      if (message.startsWith("#")) {
        handleCommandFromServerUI(message);
      } else {
        if (getNumberOfClients() == 0) {
          this.serverUI.display("No clients connected");
        } else {
          // print the message to all connected clients
          this.sendToAllClients("SERVER MSG> " + message);
        }

        // print message on serverUI as well
        this.serverUI.display(message);
      }
    }
    catch(IOException e)
    {
      this.serverUI.display("Unable to handle the message");
    }
  }

  private void handleCommandFromServerUI(String serverCommand) throws IOException {

    // get command and command args separately
    String[] commandAndArgs = extractCommandAndArgs(serverCommand);
    String command = commandAndArgs[0];
    String commandArgs = commandAndArgs[1]; // will be null if no args supplied

    // check if the command is valid (not empty, or unhandled)
    if (!isValidCommand(command)) {
      this.serverUI.display("not a valid command: #" + command);
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
        this.serverUI.display("Can not change port while server is listening for connections. Please close the server first (#close), then try again.");
      }
      // validate command arguments
      if (!isValidCommandArgs(commandArgs)) {
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
      client.sendToClient("#logoff");
    }
  }

  //Class methods ***************************************************

  /**
   * Checks is a provided command value is one of the accepted commands
   * @param command string value of the command to be validated
   * @return true if valid command, else false
   */
  private static boolean isValidCommand(String command) {
    return !command.isEmpty() && ACCEPTED_COMMANDS.containsKey(command);
  }

  private static boolean isValidCommandArgs(String commandArgs) {
    return commandArgs != null && !commandArgs.isEmpty();
  }

  private String[] extractCommandAndArgs(String userInput) throws ArrayIndexOutOfBoundsException {
    // process user input, split on space
    String[] result = userInput.split(" ");
    // extract the command (without '#')
    String command = result[0].substring(1);
    String commandArgs = (result.length > 2) ? result[1] : null;
    return new String[]{ command, commandArgs };
  }

}
//End of EchoServer class
