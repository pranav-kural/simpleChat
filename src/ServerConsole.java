import common.ChatIF;
import server.EchoServer;

import java.util.Scanner;

/**
 * ServerConsole class helps to create a user interface for a EchoServer
 * @author Pranav Kural
 * Student number: 300241227
 */
public class ServerConsole implements ChatIF {
    //Class variables *************************************************

    /**
     * The default port to connect on.
     */
    final public static int DEFAULT_PORT = 5555;

    //Instance variables **********************************************

    /**
     * The instance of the echo server that created this ServerConsole
     */
    EchoServer server;


    /**
     * Scanner to read from the console
     */
    Scanner fromConsole;


    //Constructors ****************************************************

    /**
     * Constructs an instance of the ServerConsole UI.
     *
     * @param port The port to connect on.
     */
    public ServerConsole(int port)
    {
        // instantiate echo server
        server = new EchoServer(port, this);

        // attempt to start listening for connections
        try
        {
            server.listen();
        }
        catch (Exception ex)
        {
            System.out.println("ERROR - Could not listen for clients!");
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
                server.handleMessageFromServerUI(message);
            }
        }
        catch (Exception ex)
        {
            System.out.println("Unexpected error while reading from console! " + ex.getMessage());
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
        System.out.println("SERVER MSG> " + message);
    }

    //Class methods ***************************************************

    /**
     * This method is responsible for the creation of the ServerConsole
     *
     * @param args first argument: port number
     */
    public static void main(String[] args)
    {
        // initialize with default value(s)
        int port = DEFAULT_PORT;

        // try to parse user input for port number
        try
        {
            port = Integer.parseInt(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            System.out.println("No port provided. Server going to listen on default port");
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number provided");
        }

        // instantiate a new echo server
        ServerConsole serverUI = new ServerConsole(port);
        // accept input from server user
        serverUI.accept();
    }
}
