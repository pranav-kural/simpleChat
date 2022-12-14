// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.*;
import utils.SCUtilities;

import java.io.*;
import java.util.HashMap;

/**
 * Concrete class implementing AbstractClient of the OCSF framework
 * @author Pranav Kural
 * Student number: 300241227
 */
public class ChatClient extends AbstractClient {
  //Instance variables **********************************************

  /**
   * The interface type variable.  It allows the implementation of
   * the display method in the client.
   */
  ChatIF clientUI;

  /**
   * ClientConsole's id
   */
  String loginId;

  /**
   * Specify signature of the command sent by client to set login id
   */
  public static final String CLIENT_SET_LOGIN_ID_COMMAND = "#login";

  /**
   * Specify delimiter used to separate command from arguments provided with it
   */
  private static final String COMMAND_ARGUMENT_SEPARATOR = "\\s+";

  // define a structure for all commands
  // to avoid using hard-coded strings in multiple places
  private enum COMMANDS {
    quit,
    logoff,
    sethost,
    setport,
    login,
    gethost,
    getport
  }

  // list of accepted commands (common to all clients)
  // storing in a HashMap for quickly checking if a given string value if a valid command (in O(1))
  private final static HashMap<String, Boolean> ACCEPTED_COMMANDS = new HashMap<>() {{
    put(COMMANDS.quit.name(), true);
    put(COMMANDS.logoff.name(), true);
    put(COMMANDS.sethost.name(), true);
    put(COMMANDS.setport.name(), true);
    put(COMMANDS.login.name(), true);
    put(COMMANDS.gethost.name(), true);
    put(COMMANDS.getport.name(), true);
  }};

  //Constructors ****************************************************

  /**
   * Constructs an instance of the chat client.
   *
   * @param host     The server to connect to.
   * @param port     The port number to connect on.
   * @param clientUI The interface type variable.
   */

  public ChatClient(String loginId, String host, int port, ChatIF clientUI) throws IllegalArgumentException {
    super(host, port); //Call the superclass constructor
    this.clientUI = clientUI;
    setLoginId(loginId);
  }


  //Instance methods ************************************************

  /**
   * Set the loginId
   * @param loginId login id of the client
   */
  public void setLoginId(String loginId) throws IllegalArgumentException {
    if (loginId == null || loginId.isEmpty()) {
      throw new IllegalArgumentException("ERROR - No login ID specified.  Connection aborted.");
    }
    this.loginId = loginId;
  }

  public void connectToServer() throws IOException {
    if (this.loginId == null || this.loginId.isEmpty()) {
      throw new IOException("ERROR - No login ID specified.  Connection aborted.");
    }
    openConnection();
  }

  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) {
    String msgFromServer = msg.toString();
    // if server has sent a command
    if (msgFromServer.startsWith("#")) {
      try {
        clientUI.display("Server sent command to disconnect");
        handleClientCommand(msgFromServer);
      } catch (IOException e) {
        clientUI.display("Unable to process server's request to disconnect");
      }
    } else {
      clientUI.display(msgFromServer);
    }
  }

  /**
   * This method handles all data coming from the UI
   *
   * @param message The message from the UI.
   */
  public void handleMessageFromClientUI(String message) {
    // guard-clause
    if (message == null) {
      this.clientUI.display("Invalid message received from Client UI");
      return;
    }

    try {
      // if message is a command
      if (message.startsWith("#")) {
        handleClientCommand(message);
      } else {
        if (isConnected()) {
          sendToServer(message);
        } else {
          this.clientUI.display("Client is not connected to server. Please open connection and try again!");
        }
      }
    } catch (IOException e) {
      String msg = "Could not send message to server. " + e.getMessage();
      if (isConnected()) {
        quit();
        msg += "Terminating client";
      }
      clientUI.display(msg);
    }
  }

  /**
   * Handle command input from Client Console
   * @param clientCommand command entered by the client
   * @throws IOException throws IOException is I/O error occurs while sending to server
   */
  private void handleClientCommand(String clientCommand) throws IOException {
    // get command and command args separately
    String[] commandAndArgs = SCUtilities.extractCommandAndArgs(clientCommand, COMMAND_ARGUMENT_SEPARATOR);
    // guard-clause, check if processed input is not null
    if (!SCUtilities.isValidArray(commandAndArgs)) {
      throw new NullPointerException("Invalid value provided for command & arguments (clientCommand)");
    }
    // extract command and args from processed input
    String command = commandAndArgs[0].substring(1); // get command without the command identifier (ex: '#')
    String commandArgs = commandAndArgs[1]; // will be null if no args supplied

    // check if the command is valid (not empty, or unhandled)
    if (!SCUtilities.isValidCommand(command, ACCEPTED_COMMANDS)) {
      this.clientUI.display("not a valid command: #" + command);
      return;
    }

    if (command.equals(COMMANDS.quit.name())) {
      // close connection to server and quit
      quit();
    } else if (command.equals(COMMANDS.logoff.name())) {
      // close connection (but not quit)
      if (isConnected()) {
        // closeConnection();
        // not using AbstractClient's method because doing so does not trigger the "connectionClosed()" method
        // implemented in the AbstractServer, so we don't get message on serverUI indicating a client has disconnected
        // Therefore, sending the request to server
//        sendToServer("#logoff");
        closeConnection();
        this.clientUI.display("Connection terminated with the server.");
      } else {
        this.clientUI.display("Invalid command! No active connection.");
      }

    } else if (command.equals(COMMANDS.sethost.name())) {
      // setting a new hostname
      // make sure client is not already connected
      if (isConnected()) {
        this.clientUI.display("Can not change hostname while connection is active. Please disconnect first (#logoff), then try again.");
      }
      // validate command arguments
      if (SCUtilities.isValidString(commandArgs)) {
        setHost(commandArgs);
      } else {
        this.clientUI.display("Invalid command argument, no host provided.");
      }
    } else if (command.equals(COMMANDS.setport.name())) {
      // setting a new port
      // make sure client is not already connected
      if (isConnected()) {
        this.clientUI.display("Can not change port while connection is active. Please disconnect first (#logoff), then try again.");
      }
      // validate command arguments
      if (!SCUtilities.isValidString(commandArgs)) {
        this.clientUI.display("Invalid command argument, no port number provided.");
      } else {
        int port;
        // validate input
        try {
          port = Integer.parseInt(commandArgs);
          // set the new port if parsing was successful
          setPort(port);
        } catch (NumberFormatException e) {
          this.clientUI.display("Invalid value provided for port number: " + commandArgs);
        }
      }
    } else if (command.equals(COMMANDS.login.name())) {
      // established a connection to the server; displays error is already connected
      if (isConnected()) {
        this.clientUI.display("Invalid command! Client is already connected to host " + getHost() + " on port " + getPort());
      } else {
        // open connection
        openConnection();
      }
    } else if (command.equals(COMMANDS.gethost.name())) {
      // display current host name
      this.clientUI.display("Current client's host set to: " + getHost());
    } else if (command.equals(COMMANDS.getport.name())) {
      // display current port
      this.clientUI.display("Current client's port set to: " + getPort());
    }

    // not one of the yet implemented accepted commands
    else {
      this.clientUI.display("Command not available yet");
    }
  }

  /**
   * This method terminates the client.
   */
  public void quit() {
    // try to disconnect is currently connected
    if (isConnected()) {
      try {
        closeConnection();
      } catch (IOException e) {
        this.clientUI.display("Unable to close connection");
      }
    }
    System.exit(0);
  }

  /**
   * Hook method called after the connection has been closed
   */
  @Override
  protected void connectionClosed() {
    this.clientUI.display("Server connection closed.");
  }

  /**
   * Hook method called each time an exception is thrown by the client's
   * thread that is waiting for messages from the server
   *
   * @param exception the exception raised.
   */
  @Override
  protected void connectionException(Exception exception) {
//    this.clientUI.display("The server has shut down");
    quit();
  }

  /**
   * Hook method called after a connection has been established.
   */
  @Override
  protected void connectionEstablished() {
    // send server command to set loginId
    try {
      sendToServer(CLIENT_SET_LOGIN_ID_COMMAND + " " + loginId);
    } catch (IOException e) {
      System.out.println("Unable to send login command to server");
      System.exit(1);
    }
  }
}
//End of ChatClient class
