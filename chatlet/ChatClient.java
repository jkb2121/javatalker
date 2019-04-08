//***************************************************************************
/*
Jeffrey K. Brown
CS670Y Internet Applications in Java
Multithreaded Socket Server's Interactive Client
1/12/2003

The ChatClient connects to the Chat Server and sends messages.  The Client
is interactive, getting keystrokes from Stdin and writing them out to the
socket.  ChatClient also listens to the socket's input buffer for messages
from the server and prints those out to Stdout.  There are two classes in
this file, ChatClient, which sets up the connections and listens for input
from the keyboard, and SocketPrinter, which listens to the socket.  I
needed to have a multithreaded system here because the SocketPrinter
would wait forever for socket input, and the ChatClient's listening on the
keyboard would wait forever, so we would have one or the other blocking
the other.
*/
//***************************************************************************
import java.net.*;
import java.io.*;

// SocketPrinter listens for anything being sent on the socket's InputStream
// and passes it back out to Stdout.  It is using a separate Thread as the
// main ChatClient class because the program's flow would stay on the socket
// part, keeping the keyboard part from working and vice versa, so a separate
// thread seemed to be the only reasonable way to go.

class SocketPrinter extends Thread {
	BufferedReader in;

	// The constructor starts the thread and initializes the BufferedReader
	SocketPrinter(Socket s) {
		start();
		try {
			in = new BufferedReader(new InputStreamReader (s.getInputStream()));;
		}
		catch (IOException ioe) {
			 ioe.printStackTrace();
		}
	}

	// The method of the thread.  It reads from the socket and prints to Stdout.
	public void run () {
		String str="";;
		try {
			while (true) {
				try {
					if ((in.ready())) {

						str = in.readLine();
						if (!str.equals("")) {
							System.out.println("Input from Socket: "+str);
						}
					}
				}
				catch (Exception e) { continue; }

			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
//***************************************************************************

// ChatClient connects the client to a Chat server or any other service
// running on port 2121.  ChatClient listens for keyboard input and then
// sends that to the Chat Server through the PrintWriter.  If a .q is read,
// (it's sent to the Chat Server to quit that connection, too), the
// ChatClient closes its socket and terminates.


public class ChatClient {
	public static void main (String[] args) {
		InetAddress addr = null;
		Socket socket = null;

		try {
			 addr = InetAddress.getByName(null);
		}
		catch (UnknownHostException uhe) {
			System.out.println("Could not get the Hostname, even though it's Localhost");
			System.exit(1);
		}

		try {
			socket = new Socket (addr, 2122);
		}
		catch (IOException ioe) {
			System.out.println("Error Creating the Socket");
			System.exit(1);
		}

		SocketPrinter sp;

		try {
			String str = "";
			String inbuf ="";

			// Create the Printwriter and BufferedReader on the Output
			// Stream of the socket and Stdin.

			PrintWriter out = new PrintWriter (
				new BufferedWriter (
					new OutputStreamWriter(
						socket.getOutputStream())),true);
			BufferedReader stdin = new BufferedReader(
				new InputStreamReader (System.in));

			// Create a new SocketPrinter.

			sp = new SocketPrinter(socket);

			// Read lines from Stdin and echo them to Stdout and to the
			// Socket, too.
			while (true) {
				if (stdin.ready()) {
					inbuf = stdin.readLine();
					System.out.println("Input from Keyboard: "+inbuf);

					out.println(inbuf);

					if (inbuf.equals(".q")) {
						System.out.println("Closing ChatClient...");
						break;
					}

					inbuf="";
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exiting with Error...");
		}
		finally {
			System.out.println("Closing...");
			try { socket.close(); }
			catch (Exception e) { ; /* We're already closing... */ }
			System.exit(0);
		}
	}
}

//***************************************************************************
