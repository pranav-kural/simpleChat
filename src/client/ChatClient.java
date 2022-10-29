// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.*;
import java.io.*;
import java.util.HashMap;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000
 */
public class ChatClient extends AbstractClient
{
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
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   */
  
  public ChatClient(String loginId, String host, int port, ChatIF clientUI)
    throws IOException 
  {
    super(host, port); //Call the superclass constructor
    this.clientUI = clientUI;
    this.loginId = loginId;
    openConnection();
    // send login id to the server
    sendToServer("#login " + loginId);
  }

  
  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
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
  public void handleMessageFromClientUI(String message)
  {
    // guard-clause
    if (message == null) {
      this.clientUI.display("Invalid message received from Client UI");
      return;
    }

    try
    {
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
    }
    catch(IOException e)
    {
      String msg = "Could not send message to server. " + e.getMessage();
      if (isConnected()) {
        quit();
        msg += "Terminating client";
      }
      clientUI.display(msg);
    }
  }

  private void handleClientCommand(String clientCommand) throws IOException {
    // get command and command args separately
    String[] commandAndArgs = extractCommandAndArgs(clientCommand);
    String command = commandAndArgs[0];
    String commandArgs = commandAndArgs[1]; // will be null if no args supplied

    // check if the command is valid (not empty, or unhandled)
    if (!isValidCommand(command)) {
      this.clientUI.display("not a valid command: #" + command);
      return;
    }

    if (command.equals(COMMANDS.quit.name())) {
      // close connection to server and quit
      quit();
    }

    else if (command.equals(COMMANDS.logoff.name())) {
      // close connection (but not quit)
      if (isConnected()) {
        closeConnection();
      } else {
        this.clientUI.display("Invalid command! No active connection.");
      }

    }

    else if (command.equals(COMMANDS.sethost.name())) {
      // setting a new hostname
      // make sure client is not already connected
      if (isConnected()) {
        this.clientUI.display("Can not change hostname while connection is active. Please disconnect first (#logoff), then try again.");
      }
      // validate command arguments
      if (isValidCommandArgs(commandArgs)) {
        setHost(commandArgs);
      } else {
        this.clientUI.display("Invalid command argument, no host provided.");
      }
    }

    else if (command.equals(COMMANDS.setport.name())) {
      // setting a new port
      // make sure client is not already connected
      if (isConnected()) {
        this.clientUI.display("Can not change port while connection is active. Please disconnect first (#logoff), then try again.");
      }
      // validate command arguments
      if (!isValidCommandArgs(commandArgs)) {
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
    }

    else if (command.equals(COMMANDS.login.name())) {
      // established a connection to the server; displays error is already connected
      if (isConnected()) {
        this.clientUI.display("Invalid command! Client is already connected to host " + getHost() + " on port " + getPort());
      } else {
        // open connection
        openConnection();
      }
    }

    else if (command.equals(COMMANDS.gethost.name())) {
      // display current host name
      this.clientUI.display("Current client's host set to: " + getHost());
    }

    else if (command.equals(COMMANDS.getport.name())) {
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
  public void quit()
  {
    // try to disconnect is currently connected
    if (isConnected()) {
      try
      {
        closeConnection();
      }
      catch(IOException e) {
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
   * @param exception the exception raised.
   */
  @Override
  protected void connectionException(Exception exception) {
    this.clientUI.display("The server has shut down");
    quit();
  }

  /**
   * Checks is a provided command value is one of the accepted commands
   * @param command string value of the command to be validated
   * @return true if valid command, else false
   */
  private static boolean isValidCommand(String command) {
    return command != null && !command.isEmpty() && ACCEPTED_COMMANDS.containsKey(command);
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
//End of ChatClient class
