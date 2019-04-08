/*

To Do:


*/
import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import java.awt.event.*;

class SocketPrinter extends Thread {
	BufferedReader in;
	Chatlet c;

	// The constructor starts the thread and initializes the BufferedReader
	SocketPrinter(Socket s, Chatlet c) {
		this.c = c;

		try {
			in = new BufferedReader(new InputStreamReader (s.getInputStream()));;
		}
		catch (IOException ioe) {
			 ioe.printStackTrace();
		}

		int i=6;
		while (i>0) {
			try {
				if ((in.ready())) {
					in.readLine();
					--i;
				}
			}
			catch (IOException ie) {
				--i;
			}
		}

		i=6;
		while (i>0) {
			try {
				if ((in.ready())) {
					in.readLine();
					--i;
				}
			}
			catch (IOException ie) {
				--i;
			}
		}

		start();
	}

	// The method of the thread.  It reads from the socket and prints to Stdout.
	public void run () {
		int count=0;
		String str="";;
		try {
			while (true) {

				c.jsp.setMaximum();

				try {
					if ((in.ready())) {
						str = in.readLine();
						if (!str.equals("")) {
							c.printText("\n"+str);

							//c.jsp.setMaximum();
							//c.jsp.setMaximum();
							//c.jta.updateUI();
							//c.jsp.setMaximum();
							//c.jsp.setMaximum();
						}
					}
					Thread.sleep(100);
					count ++;

					if (count==2000) {
						//c.jsp.setMaximum();
						//c.jta.updateUI();
						//c.jsp.setMaximum();
					}
				}
				catch (Exception e) { continue; }

			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}

}


class JkbScrollPane extends JScrollPane {


	public JkbScrollPane(JTextArea jta) {
		super(jta,JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//super(jta,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		//	JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

	}

	public void setMaximum() {

		verticalScrollBar.setValue(verticalScrollBar.getMaximum()+10);

	}

}


public class Chatlet extends JApplet implements ActionListener {
	JTextField jtf;
	public JTextArea jta;
	JkbScrollPane jsp;
	Socket sock;

	SocketPrinter sp;
	PrintWriter out;


	public void runChatlet() {
		try {

			String javahost = getParameter("javahost");
			sock = new Socket (javahost, 2121);

			out = new PrintWriter(sock.getOutputStream(), true);

			out.println("CHATLET_COLOROFF");
			out.println(getParameter("username").trim());
			out.println(getParameter("password").trim());



			sp = new SocketPrinter(sock, this);
		}
		catch (Exception e) {
			printText(e.getMessage());
		}
	}


	public void init () {
		Container c = getContentPane();
		c.setLayout(new FlowLayout());
		jtf = new JTextField(80);
		jta = new JTextArea(20,80);
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);
		jta.setEditable(false);
		Font f = new Font ("Courier",Font.PLAIN,12);
		jta.setFont (f);
		jtf.setFont (f);
		jsp = new JkbScrollPane(jta);
		//jta,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		//	JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		jtf.addActionListener(this);
		c.setBackground(new Color(0,0,140));

		c.add(jsp);
		c.add(jtf);

		runChatlet();
	}

	public void printText (String s) {
		jta.append(s);

	}

	public void actionPerformed (ActionEvent ae) {
		String message = jtf.getText();
		//System.out.println("ActionPerformed");
		jtf.setText("");
		out.println(message);
	}
}
