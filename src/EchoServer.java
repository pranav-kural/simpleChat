// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 


import common.ChatIF;
import  ocsf.server.*;

import java.io.IOException;

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
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT = 5555;
  
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

    if (!isListening()) {
      this.serverUI.display("Server is currently not listening!");
      return;
    }

    try
    {
      // if message is a command
      if (message.startsWith("#")) {
        handleCommandFromServerUI(message);
      } else {
        // print the message to all connected clients
        this.sendToAllClients("SERVER MSG> " + message);
        // print message on serverUI as well
        this.serverUI.display(message);
      }
    }
    catch(IOException e)
    {
      this.serverUI.display("Unable to handle the message");
    }
  }

  private void handleCommandFromServerUI(String serverCommand) throws IOException {}

  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of 
   * the server instance (there is no UI in this phase).
   *
   * @param args The port number to listen on.  Defaults to 5555
   *          if no argument is entered.
   */
//  public static void main(String[] args)
//  {
//    int port = 0; //Port to listen on
//
//    try
//    {
//      port = Integer.parseInt(args[0]); //Get port from command line
//    }
//    catch(Throwable t)
//    {
//      port = DEFAULT_PORT; //Set port to 5555
//    }
//
//    EchoServer sv = new EchoServer(port);
//
//
//  }
}
//End of EchoServer class
