// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import java.util.Scanner;

import client.*;
import common.*;

/**
 * ClientConsole class helps to create a user interface for a ChatClient
 * @author Pranav Kural
 * Student number: 300241227
 */
public class ClientConsole implements ChatIF 
{
  //Class variables *************************************************
  
  /**
   * The default port to connect on.
   */
  final public static int DEFAULT_PORT = 5555;
  final private static String DEFAULT_HOST = "localhost";
  
  //Instance variables **********************************************
  
  /**
   * The instance of the client that created this ConsoleChat.
   */
  ChatClient client;

  /**
   * Scanner to read from the console
   */
  Scanner fromConsole; 

  
  //Constructors ****************************************************

  /**
   * Constructs an instance of the ClientConsole UI.
   *
   * @param host The host to connect to.
   * @param port The port to connect on.
   */
  public ClientConsole(String loginId, String host, int port)
  {
    // login id must be provided
    if (loginId == null) {
      throw new IllegalArgumentException("Invalid login id");
    }

    try 
    {
      client= new ChatClient(loginId, host, port, this);
    }
    catch (IllegalArgumentException e) {
      display(e.getMessage());
    }
    
    // Create scanner object to read from console
    fromConsole = new Scanner(System.in); 
  }

  
  //Instance methods ************************************************
  
  /**
   * This method waits for input from the console.  Once it is 
   * received, it sends it to the client's message handler.
   */
  public void accept() 
  {
    try
    {

      String message;

      while (true) 
      {
        message = fromConsole.nextLine();
        client.handleMessageFromClientUI(message);
      }
    } 
    catch (Exception ex) 
    {
      display("Unexpected error while reading from console! " + ex.getMessage());
    }
  }

  /**
   * This method overrides the method in the ChatIF interface.  It
   * displays a message onto the screen.
   *
   * @param message The string to be displayed.
   */
  public void display(String message) 
  {
    // if it's a server message, no need for ">" prefix
    System.out.println((message.startsWith("SERVER MSG>") ? message : "> " + message));
  }

  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of the Client UI.
   *
   * @param args arguments in order: hostname port-number
   */
  public static void main(String[] args) 
  {
    // initialize using default values
    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    // loginId must be provided
    if (args.length < 1) {
      System.out.println("Unable to create ClientConsole UI because no loginID was provided");
      return;
    }
    String loginId = null;

    // over-write host and/or port if we have valid arguments
    try {
      loginId = args[0];
      host = args[1];
      port = Integer.parseInt(args[2]);
      System.out.println("Connecting on host: " + host + ", port: " + port);
    }
    catch(ArrayIndexOutOfBoundsException e) {
      System.out.println("No host or port provided. Connecting on host: " + host + ", port: " + port);
    }
    catch (NumberFormatException e) {
      System.out.println("No port number provided, connecting on host: " + host + ", port: " + port);
    }

    ClientConsole chat = new ClientConsole(loginId, host, port);
    // below implemented for Testcase 2003 (generally, ClientConsole must have a login id)
    try{
      chat.client.connectToServer();
    }
    catch(IOException exception)
    {
      chat.display("Error: Can't setup connection! Terminating client.");
      // terminate the client
      chat.client.quit();
      System.exit(1);
    }
    chat.accept();  //Wait for console data

  }
}
//End of ConsoleChat class
