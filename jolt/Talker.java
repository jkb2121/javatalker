/* Comments
Update Weblog With:

TO DO:
1. 3/5/05 - Caesar Cipher on Talker Mail (fixed)
2. Online Syslog (fixed)
3. ? modspeech cut out the color being added to the buffer
4. Private Areas
5. Wisecrack Channel
6. Color Preferences
7. 3/6/05 - desc escaped into database, de-escaped out of it...
8. Return values for some methods..
9. Error handling?  Sure try and catch, but what then?
10. Setting to highlight outgoing tells with ~BP instead of ~FP?
11. 3/6/05 - Levels and Sites on the Login/Logout
12. Prompts
13. Fixed User Feedback on .quit
14. 3/6/05 - Time and Date Displays
15. The Locu Authentication Bypass (fixed)
16. Added .site
18, 3/6/05 - Added Shout and Semote
17. 3/6/05 - Fixed UserConnection finally
18. 1/3/05 - Fixed the \0 in .smail Bug
19. 1/3/05 - Fixed the "Message has been Sent" to User Online bug.
20. 3/6/05 - Dead Threads and Memory Wasting due to ResultSets being left open.
21. 7/6/05 - Added a Moe .people alias (and did some work to try to kill off some of the thread problem)
22. 7/7/2005 - Added .greet, and maybe succeeded in killing off the thread bug?
**/

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;
import java.sql.Connection;
import java.text.*;
import com.jkbworld.CaesarCipher;

/** UserConnection Object
	@author Jeffrey K. Brown
	<p>The UserConnection is a threaded object that continually monitors an input
	stream of a socket from the Talker and prints to an output stream of a socket.
	Input to the UserConnection from the user is checked for command names which
	determine what type of communication is taking place.  The different commands
	execute different methods in the Talker object.
	</p>
*/

class UserConnection extends Thread {
	/** This is the Socket on which the connection to the Talker is made. */
	public Socket socket;

	/** This is a LineNumberReader that reads the InputStream from socket and passes it to the Talker. */
	public LineNumberReader in;

	/** This is a PrintWriter used to print messages from the Talker to the user's socket. */
	public PrintWriter out;

	/** This is used by the Talker to identify the position in the UserConnection array the current UserConnection has. */
	int index=0;

	/** This serves as a link back to the main Talker object, so the individual users can share the set of data in Talker */
	static Talker t;

	/** This String is used to store SQL queries prior to passing them to the database. */
	String sql;

	/** This variable is used by the Talker to identify which virtual area or room the user is in. */
	int area;

	/** This is used by the UserConnection to determine whether or not to terminate its internal thread. */
	boolean quit = false;

	/** This is a boolean used to designate a connection that has been dropped because the user has logged on again */
	boolean replaced = false;


	/** This is a boolean which helps the system determine if the PrintWriter out is ready to be written. */
	boolean outset=false;

	/** This is a boolean that helps determine whether or not to break out of the
	login loop and into the normal Talker loop. */
	boolean authenticated=false;

	/** This is the User Name */
	private String name = "Unnamed";

	/** This is the User's description */
	private String desc = "A Generic Desc";

	/** This is the User's password */
	private String password = "";

	/** This is the number of minutes of the current connection. */
	private int minutes = 0;

	/** This is the number of minutes since the last input from the user. */
	private int idle = 0;

	/** The level is used to designate what commands the user has access to. */
	private int level = 0;

	/** The profile is a place where the users can put a few paragraphs describing themselves. */
	private String profile = "This user has not yet set a profile";

	/** This is used to keep a running count of the total number of minutes on the system. */
	private int totaltime = 0;

	/** This is used to tell males from females.*/
	private String gender = "Unknown";

	/** This is used to show the user's age */
	private int age = 18;

	/** This stores the user's email address. */
	private String email = "me@noneofyourbusiness.com";

	/** This stores the URL of the user's homepage */
	private String homepage = "darkages.darkfantastic.net/";

	/** This stores the URL of the user's picture */
	private String photo = "darkages.darkfantastic.net/none.jpg";

	/** This is used by the system to determine whether or not to print colors */
	private int colorpref = 2;

	/** This is used to store the last location a user signed on from. */
	private String lastsite = "This user has never been here before";

	/** This variable is used to store the date the user last signed on. */
	private String laston = "Never";

	/** This is used to determine whether or not the user is connected through the Chatlet. */
	private boolean chatlet=false;

	/** The tell[] array stores the last 20 online private messages for reviewing. */
	String[] tell = new String[20];

	/** This boolean shows the user is away from the keyboard if true */
	boolean afk=false;

	/** If a user is away from keyboard, and this is true, an online private message was received. */
	boolean afktell=false;

	/** A user can specify a reason why they are going AFK if they desire.  It's stored here. */
	String afkmsg = "ZZ... Z... z...";

	/** The quit() method sets the quit boolean, which is used to terminate the UserConnection Thread. */
	public void quit() { quit = true; }

	/** addMinute() increments the running count of minutes and idle minutes for timing purposes. */
	public void addMinute() { ++minutes; ++idle; }

	/** The getIdle method returns the number of idle minutes.
		@return integer number of idle minutes */
	public int getIdle() { return idle; }

	/** The getTime() method returns the numbef of minutes the user has been connected.
		@return integer number of minutes connected. */
	public int getTime() { return minutes; }


	/** isAuthenticated() returns a boolean that lets the calling method know whether
	or not a UserConnection is authenticated or not.  Often used by methods printing output
	to multiple UserConnections.
		@return boolean*/
	public boolean isAuthenticated() { return authenticated; }

	/** UserConnection Constructor.  In the constructor, we initialize a lot of the variables used by the UserConnection.
		@param t The UserConnection constructor takes a link back to the calling Talker object.
	*/
	public UserConnection(Talker t) {
		int i=0;
		this.t = t;
		area = 0;

		for (i=0; i<20; ++i) tell[i]="";
	}

	/** setArea is called to change the virtual area of a user to a new one.
		@param area is an integer, which stores the index in the Area array of the new virtual area. */
	public void setArea (int area) {
		this.area = area;
	}
	/** getArea is called by methods in Talker that need to determine where the user is located.
		@return area, an integer index of the virtual room in the Area array in which the user is located. */
	public int getArea () {
		return area;
	}

	/** getIndex is called by methods in Talker that need to know the Index of this particular UserConnection.
		@return index, an integer index of this UserConnection in the UserConnection array. */
	public int getIndex() {
		return index;
	}

	/** setIndex is used by the Talker to set the UserConnection's index. It is called when resizing the UserConnection array, or when the user initially connects.
		@param index an integer index for the UserConnection to take on. */
	public void setIndex(int index) {
		this.index = index;
	}

	/** getPort() is used to determine which port the UserConnection is connected from.  This is used along with GetSite and GetInetAddress.
		@return integer representing the port. */
	public int getPort() {
		return socket.getPort();
	}

	/** getInetAddress() returns the name or IP address from which the UserConnection originated.
		@return InetAddress of the site's DNS name or IP address */
	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}

	/** getSite() returns the port and address from which the user has connected.
		@return String containing the site and port. */
	public String getSite() {
		String output = ""+getInetAddress().getHostAddress() +", Port: "+getPort();
		
		return output;
	}

	/** getSocket() returns the Socket on which the UserConnection is made.
		@return Socket of the user. */
	public Socket getSocket() {
		return socket;
	}

	/** getTotalTime() returns the Total number of minutes the user has logged on the system, including the minutes of the current UserConnection.
		@return integer containing the number of minutes. */
	public int getTotalTime() {
		return totaltime + minutes;
	}

	/** getAfk() returns a boolean showing whether or not the user is AFK.
		@return boolean designating the AFK status, true or false. */
	boolean getAfk() {
		return afk;
	}

	/** setAfk() is a toggle that either marks a user AFK or not.  This is called by the .afk command.
		@param afk boolean whose value replaces the current afk boolean value. */
	void setAfk(boolean afk) {
		this.afk = afk;
	}

	/** setAfkMsg() sets the message that is displayed when a user sends an online private message to an AFK user.
		@param msg is a String containing the message */
	void setAfkMsg(String msg) {
		afkmsg = msg;
	}

	/** getAfkMsg() returns the message to be displayed while the user is AFK.
		@return afkmsg is the String containing the AFK message. */
	String getAfkMsg() {
		return afkmsg;
	}

	/** setAfkTells() marks the UserConnection as having received online private messages while AFK.
		@param afktell is the boolean with the afktell status. */
	void setAfkTells(boolean afktell) {
		this.afktell=afktell;
	}

	/** getAfkTells() returns the boolean value with the afktell status, either the user has received tells while AFK or not.
		@return afktell which is the status. */
	boolean getAfkTells() {
		return afktell;
	}

	/** setEmail() sets the UserConnection email value to the string passed as a parameter.  If the parameter string is longer than 60, it is truncated.
		@param email a string containing the new value for the email address. */
	void setEmail(String email) {
		if (email.length()>60) {
			this.email = email.substring(0,60);
		}
		else {
			this.email = email;
		}
	}

	/** getEmail() returns the value set for the UserConnection email address.
		@return email which is the the String containing the email address. */
	String getEmail() {
		return t.unescapeUserInput(email);
	}
	/** setHomepage() sets the UserConnection homepage value to the value passed as a parameter.  If the new value is longer than 60 characters, it is truncated.
		@param homepage String containing the homepage value. */
	void setHomepage(String homepage) {
		if (homepage.length()>60) {
			this.homepage = homepage.substring(0,60);
		}
		else this.homepage = homepage;
	}

	/** getHomepage() simply returns the UserConnection homepage value.
		@return homepage which is a String containing the homepage value. */
	String getHomepage() {
		return t.unescapeUserInput(homepage);
	}

	/** setPhoto() sets the UserConnection photo value to the one in the parameter.  If the new value is longer than 60 characters, it is truncated.
		@param photo contains the new value for the photo */
	void setPhoto(String photo) {
		if (photo.length()>60) {
			this.photo = photo.substring(0,60);
		}
		else this.photo = photo;
	}
	/** getPhoto() simply returns the UserConnection photo value.
		@return photo, the String containing the value for photo. */
	String getPhoto() {
		return t.unescapeUserInput(photo);
	}

	/** setDesc() sets the value of the parameter to the UserConnection desc.  If it is longer than 40, it is truncated.
		@param desc which is a string containing the new description value. */
	void setDesc(String desc) {
		if (desc.length()>40) {
			this.desc = desc.substring(0,40);
		}
		else this.desc = desc;
	}

	/** getDesc() returns the value of the UserConnection desc.
		@return desc which is the UserConnection description value. */
	String getDesc() {
		return desc;
	}

	/** setAge() sets the UserConnection age to the parameter value.
		@param age the integer representing the new age value. */
	void setAge(int age) {
		this.age = age;
	}

	/** getAge() returns the UserConnection age value.
		@return integer containing the age. */
	int getAge() {
		return age;
	}

	/** setLevel() is called by the .promote and .demote commands to change the UserConnection's level to the one in the parameter.
		@param level is an integer containing the new value for level. */
	void setLevel(int level) {
		this.level = level;
	}

	/** getLevel() returns the value of the UserConnection's level.
		@return level which contains the value of the UserConnection level. */
	int getLevel() {
		return level;
	}

	/** getUserName() returns the value of the UserConnection's username.
		@return name, which is a String containing the username. */
	public String getUserName() {
		return name;
	}

	/** setUsername() sets the UserConnection's username value.
		@param name is the new value for UserConnection name.*/
	public void setUserName(String name) {
		this.name = t.escapeUserInput(name);
	}

	/** getUserDesc() returns the UserConnection desc value.
		@return desc which is a String containing the UserConnections description. */
	public String getUserDesc() {
		return t.unescapeUserInput(desc);
	}

	/** setPassword() sets the UserConnection password to the value passed.
		@param password String containing the new password. */
	void setPassword(String password) {
		this.password = password;
	}

	/** getPassword() returns the UserConnection password.
		@return password is the UserConnection password. */
	String getPassword() {
		return password;
	}

	/** setProfile() sets the UserConnection profile to the value passed to the method.
		@param profile is a String containing the new Profile. */
	void setProfile(String profile) {
		this.profile = profile;
	}

	/** getProfile() returns the String value of the user profile.
		@return profile is a String containing the profile. */
	String getProfile() {
		return t.unescapeUserInput(profile);
	}

	/** setGender() sets the UserConnection gender to the value passed as a parameter.  This is called from the .set command, which only permits "Male", "Female", or "Unknown".
		@param gender containing the new gender to be set. */
	void setGender(String gender) {
		this.gender = gender;
	}

	/** getGender() returns the value set for gender in the UserConnection object.
		@return gender is a String containing the gender. */
	String getGender() {
		return gender;
	}

	/** addTell() adds online private messages to the tell[] array.  The tell array stores 20 messages.  If there are more than 20, the oldest ones are dropped off the end to make room for the new.
		@return boolean indicating whether or not there was an error.
		@param line which is a String containing the online private message to add to the tell buffer. */
	public boolean addTell (String line) {
		int i=0;

		for (i=0; i<20; ++i) {
			if (tell[i].equals("")) {
				tell[i] = line;
				return false;
			}
		}

		for (i=0; i<19; ++i) {
			tell[i] = tell[i+1];
		}
		tell[19] = line;
		return false;
	}

	/** showTell() displays the contents of the tell buffer.
		@return boolean indicating whether or not there was an error. */
	public boolean showTell () {
		int i=0;
		println("~FYYour Private Messages:~N\n");

		for (i=0; i<20; ++i) {
			if (tell[i].equals("")) break;
			println(tell[i]);
		}
		println("");

		return false;
	}

	/** clearTell() clears the tell buffer by replacing the online private messages with empty strings.
		@return boolean indicating whether or not an error has occurred. */
	public boolean clearTell () {
		int i=0;
		println("~FYTell Buffer cleared.~N");

		for (i=0; i<20; ++i) {
			tell[i] = "";
		}
		return false;
	}

	/** getLevelName returns a string based on the level and gender of the UserConnection.
		@return String containing the value of the Level Name. */
	String getLevelName() {
		if (getLevel()==0) {
			if (getGender().equals("Male")) return "Peasant";
			else if (getGender().equals("Female")) return "Peasant";
			else return "Visitor";
		}
		if (getLevel()==1) {
			if (getGender().equals("Male")) return "Knight";
			else if (getGender().equals("Female")) return "Lady";
			else return "Subject";
		}
		if (getLevel()==2) {
			if (getGender().equals("Male")) return "Cleric";
			else if (getGender().equals("Female")) return "Mystic";
			else return "Mage";
		}
		if (getLevel()==3) {
			if (getGender().equals("Male")) return "Wizard";
			else if (getGender().equals("Female")) return "Sorceress";
			else return "Chanter";
		}
		if (getLevel()==4) {
			if (getGender().equals("Male")) return "Prince";
			else if (getGender().equals("Female")) return "Princess";
			else return "Heir";
		}
		if (getLevel()==5) {
			if (getGender().equals("Male")) return "King";
			else if (getGender().equals("Female")) return "Queen";
			else return "Royal";
		}
		return "Visitor";
	}

	/** getLevelName() returns a String containg the Level Name based on the specificed level and gender.
		@param level integer containing the level.
		@param gender String containing the value of the gender.
		@return String containing the Level Name */
	String getLevelName(int level, String gender) {
		if (level==0) {
			if (gender.equals("Male")) return "Peasant";
			else if (gender.equals("Female")) return "Peasant";
			else return "Visitor";
		}
		if (level==1) {
			if (gender.equals("Male")) return "Knight";
			else if (gender.equals("Female")) return "Lady";
			else return "Subject";
		}
		if (level==2) {
			if (gender.equals("Male")) return "Cleric";
			else if (gender.equals("Female")) return "Mystic";
			else return "Mage";
		}
		if (level==3) {
			if (gender.equals("Male")) return "Wizard";
			else if (gender.equals("Female")) return "Sorceress";
			else return "Chanter";
		}
		if (level==4) {
			if (gender.equals("Male")) return "Prince";
			else if (gender.equals("Female")) return "Princess";
			else return "Heir";
		}
		if (level==5) {
			if (gender.equals("Male")) return "King";
			else if (gender.equals("Female")) return "Queen";
			else return "Royal";
		}
		return "Visitor";
	}

	/** parseColors() is used to put color codes into the Java Talker.  Parsecolors is hard coded with the ASCII color codes.  When a user puts a color code, for example ~FR for foreground red, parseColors does a string replace of ~FR with the ASCII color code for red.  This is called by the print and println methods of UserConnection.
		@param line is a String containing the line to be parsed.
		@return line is a String containing the parsed line. */
	public String parseColors(String line) {

		String RED    = "\033[31m\033[1m";
		String GREEN  = "\033[32m\033[1m";
		String YELLOW = "\033[33m\033[1m";
		String BLUE   = "\033[34m\033[1m";
		String PURPLE = "\033[35m\033[1m";
		String SKY    = "\033[36m\033[1m";
		String WHITE  = "\033[37m\033[1m";
		String BRED   = "\033[41m\033[1m";
		String BGREEN = "\033[42m\033[1m";
		String BYELLOW= "\033[43m\033[1m";
		String BBLUE  = "\033[44m\033[1m";
		String BPURPLE= "\033[45m\033[1m";
		String BSKY   = "\033[46m\033[1m";
		String BWHITE = "\033[47m\033[1m";
		String NORMAL = "\033[0m";
		String BLINKY = "\033[5m";
		String UNDER  = "\033[4m";

		
		line = line.replaceAll("~FR", RED);
		line = line.replaceAll("~FG", GREEN);
		line = line.replaceAll("~FY", YELLOW);
		line = line.replaceAll("~FB", BLUE);
		line = line.replaceAll("~FP", PURPLE);
		line = line.replaceAll("~FM", PURPLE);
		line = line.replaceAll("~FS", SKY);
		line = line.replaceAll("~FW", WHITE);
		line = line.replaceAll("~BR", BRED);
		line = line.replaceAll("~BG", BGREEN);
		line = line.replaceAll("~BY", BYELLOW);
		line = line.replaceAll("~BB", BBLUE);
		line = line.replaceAll("~BP", BPURPLE);
		line = line.replaceAll("~BM", BPURPLE);
		line = line.replaceAll("~BS", BSKY);
		line = line.replaceAll("~BY", BWHITE);

		line = line.replaceAll("~UL", UNDER);
		line = line.replaceAll("~LI", BLINKY);
		line = line.replaceAll("~N", NORMAL);
		line = line.replaceAll("~BE", "\007");

		return line;
	}

	/** clearColors() is used to remove color codes from input into the Java Talker.  For clients who don't support colors, this is a means of stopping the ASCII codes from being printed.  Color codes are replaced with empty strings, so the default text color is used.  This is called by the print and println methods of UserConnection.
		@param line is a String containing the line to be parsed.
		@return line is a String containing the parsed line. */
	public String clearColors(String line) {

		line = line.replaceAll("~FR", "");
		line = line.replaceAll("~FG", "");
		line = line.replaceAll("~FY", "");
		line = line.replaceAll("~FB", "");
		line = line.replaceAll("~FP", "");
		line = line.replaceAll("~FM", "");
		line = line.replaceAll("~FS", "");
		line = line.replaceAll("~FW", "");
		line = line.replaceAll("~BR", "");
		line = line.replaceAll("~BG", "");
		line = line.replaceAll("~BY", "");
		line = line.replaceAll("~BB", "");
		line = line.replaceAll("~BP", "");
		line = line.replaceAll("~BM", "");
		line = line.replaceAll("~BS", "");
		line = line.replaceAll("~BY", "");
		line = line.replaceAll("~BE", "");

		line = line.replaceAll("~UL", "");
		line = line.replaceAll("~LI", "");
		line = line.replaceAll("~N", "");

		return line;
	}

	/** println() prints lines using println to the PrintWriter (user output) that has either been passed through the parseColors, enabling ASCII text, clearColors, stripping out ASCII text, or nothing, enabling client-side color parsing.
		@return boolean indicating whether or not there has been an error.
		@param line which is the input line needing to be parsed. */
	public boolean println(String line) {
		if (colorpref==2) {
			line = parseColors(line);
		}
		if (colorpref==1) {
			// Do nothing...keep color codes...
		}
		if (colorpref==0) {
			line = clearColors(line);
		}
		out.println(line+"\r");
		return false;
	}

	/** print() prints lines using print (no newline) to the PrintWriter (user output) that has either been passed through the parseColors, enabling ASCII text, clearColors, stripping out ASCII text, or nothing, enabling client-side color parsing.
		@return boolean indicating whether or not there has been an error.
		@param line which is the input line needing to be parsed. */
	public boolean print (String line) {

		if (colorpref==2) {
			line = parseColors(line);
		}
		if (colorpref==1) {
			// Do nothing...keep color codes...
		}
		if (colorpref==0) {
			line = clearColors(line);
		}

		if (chatlet==true) {
			// Chatlet doesn't like the manual buffer flush...
			out.println(line);
		}
		else {
			out.print(line);
		}
		return false;
	}


	public String unControlString(String str) {
		StringBuffer sb = null;
		String retval="";
		int i=0;

		try {
			sb = new StringBuffer(str);

			for (i=0; i<sb.length(); ++i) {
				if (Character.isISOControl(sb.charAt(i))) {
					if (sb.charAt(i)=='\b') continue;					

					sb = sb.deleteCharAt(i);
					--i;
				}
			}
			retval = sb.toString();
		}
		catch (Exception e) {
			retval = "Exception: "+e.getMessage();
			t.printSyslog("unControlString Error("+getUserName()+"): "+e.getMessage());	
		}

		return retval;
	}



	/** loadUserData() makes a database access and reads a user record.  The values from the user record populate the UserConnection object based on the authenticated user name.
		@return boolean indicating whether or not the loadUserData() method was successful */
	boolean loadUserData() {
		t.connectDB();
		String sql="";
		try {
			sql = "SELECT description,level,profile,totaltime,gender,age,email,homepage,photo,colorpref,lastsite,laston FROM user WHERE username='"+name+"'";

			ResultSet rs = t.stmt.executeQuery(sql);
			if (rs.next()) {
				desc = t.unescapeUserInput(rs.getString(1));
				if (desc==null) { desc = "A Generic Desc"; }
				level = rs.getInt(2);
				profile = t.unescapeUserInput(rs.getString(3));
				if (profile==null) { profile = "This user has not yet set a profile"; }
				totaltime = rs.getInt(4);
				gender = rs.getString(5);
				if (gender==null) { gender = "Unknown"; }
				age = rs.getInt(6);
				email = t.unescapeUserInput(rs.getString(7));
				if (email==null) { email = "me@noneofyourbusiness.com"; }
				homepage = t.unescapeUserInput(rs.getString(8));
				if (homepage==null) { homepage = "darkages.darkfantastic.net/"; }
				photo = t.unescapeUserInput(rs.getString(9));
				if (photo==null) { photo = "darkages.darkfantastic.net/none.jpg"; }
				if (chatlet==false) colorpref = rs.getInt(10);
				lastsite = rs.getString(11);
				if (lastsite==null) { lastsite = "Who knows..."; }
				laston = rs.getString(12);
				if (laston==null) { laston = "The other day"; }
				rs.close();
				return true;
			}
			else {
				t.printSyslog("Unable to loadUserData for User: "+name);
				return false;
			}
		}
		catch (Exception e) {
			t.printSyslog("loadUserData(): Exception caught trying to Load User Data");
			e.printStackTrace();
			return false;
		}
	}

	/** saveUserData() saves the UserConnection data to the database.  This method is called by the quit commands so information is saved when the user logs out.
		@return boolean indicating whether or not the saveUserData() call was successful. */
	boolean saveUserData() {

		t.connectDB();
		String sql="";
		String passstring;
		
		String db_desc = "";
		String db_profile = "";
		String db_email = "";
		String db_homepage = "";
		String db_photo = "";
		
		try {
			if (t.getEncryptPasswords()) { passstring = "encrypt('"+password+"','PW')"; }
			else { passstring = "'"+password+"'"; }
			
			db_desc = t.escapeUserInput(desc);
			db_profile = t.escapeUserInput(profile);
			db_email = t.escapeUserInput(email);
			db_homepage = t.escapeUserInput(homepage);
			db_photo = t.escapeUserInput(photo);

			sql = "UPDATE user SET "+
				"description='"+db_desc+
				"',level="+level+
				",password="+passstring+
				",profile='"+db_profile+
				"',gender='"+gender+
				"',age="+age+
				",totaltime="+getTotalTime()+
				",email='"+db_email+
				"',homepage='"+db_homepage+
				"',photo='"+db_photo+
				"',lastsite='"+getSite()+
				"',laston='"+t.datestamp+", "+t.timestamp+
				"' WHERE username='"+name+"'";

			int sqlrows = t.stmt.executeUpdate(sql);
			//System.out.println("saveUserData(): Executed: "+sql+"; "+sqlrows+" Rows Effected.");

			return true;
		}
		catch (Exception e) {
			t.printSyslog("saveUserData(): Exception while saving data!\n"+e.getMessage()+"\nSQL:"+sql);
			return false;
		}


	}

	/** addConnection() is called when a new UserConnection is created and a new Socket it linked to it.  This initializes the LineNumberReader and PrintWriter, starts the thread, and connects to the Database.
		@param s is the Socket over which the UserConnection communicates with the rest of the system. */
	public void addConnection(Socket s) {
		socket = s;
		try {
			in = new LineNumberReader (
			new InputStreamReader (socket.getInputStream()),1);
		} catch (Exception e) {
			t.printSyslog("Exception Creating LineNumberReader:"+e.getMessage());
		}
		try {
			out = new PrintWriter (
				new BufferedWriter (
					new OutputStreamWriter (
						socket.getOutputStream())), true);
			println("\033]0;Dark Ages JavaTalker\007");
			
			println("~FGWelcome to Dark Ages JavaTalker~N");
			println("~FGRunning Java Talker Version "+t.version+"~N");
			
			BufferedReader bin = new BufferedReader(new FileReader("titlepage.txt"));
			String fileline="";
			while (bin.ready()) {
				fileline = bin.readLine();
				
				try {
					println(fileline);
				}
				catch (NullPointerException npe) {
					break;
				}
			}
			bin.close();
			
		} catch (Exception e) {
			t.printSyslog("Exception Creating PrintWriter:"+e.getMessage());
		}

		s = null;
		outset = true;
		start();
		t.connectDB();
	}

	/** checklevel() takes a parameter that is the minimum level for a command.  Checklevel is put in an if-block, and if checklevel returns a true, the command is executed.  If the level of the user is less than the parameter, checklevel returns false and the user may not execute the command.
		@param test is an integer representing the minimum level for a command's execution.
		@return boolean representing whether or not the user's level compares to test and if true, the command is executed. */
	public boolean checklevel (int test) {
		if (level < test) {
			println("\n~FRYou are not authorized to use that command~N");
			return false;
		}
		return true;
	}

	
	public String cleanTelnet (String in) {
		String clean ="";
		int i=0;
		
		try {
			for (i=in.length()-1; i>=0; --i) {  
				if (in.charAt(i)=='?') break;		
			}		
			clean = in.substring(i+1);
		}
		catch (Exception e) {
			clean = "";
		}
		return clean;
	}
	
	/** run() is the main method of the UserConnection.  In it, there are loops that continuously read the socket and act based on the data read.
		As long as the user is connected to the system, the UserConnection thread is running.  The run() method is divided into two main sections, the login section
		and the talker section.  The login section simply reads the username, reads the password, checks them against a database, and if they match, the user is let onto the system and is put into the talker section.
		Additionally, in the logon section, we take care of creation of new accounts.  A user may put "New" as the username which triggers the system to assist in creation of the new user account.
		When completed, the system creates the new account in the database and passes the new user into the Talker section.
		The Talker section continuously checks the socket for input.  If it finds some, it reads the line of input looking for command keywords.
		If a keyword is found, the run method calls the method responsible for that command with its parameters and goes back to reading the socket. */
	public void run() {
		ResultSet rs = null;
		StringTokenizer st;
		String cmd = new String("");
		String loginname=null;

		String cmd_who = new String(".who");
		String cmd_tell = new String(".tell");
		String cmd_go = new String(".go");
		String cmd_read = new String(".read");
		String cmd_write = new String(".write");
		String cmd_wipe = new String(".wipe");
		String cmd_to = new String (".to");
		String cmd_topic = new String(".topic");
		String cmd_quit = new String(".quit");
		String cmd_shutdown = new String(".shutdown");
		String cmd_look = new String(".look");
		String cmd_emote = new String(".emote");
		String cmd_remote = new String(".remote");
		String cmd_review = new String(".review");
		String cmd_cbuffer = new String(".cbuffer");
		String cmd_rmail = new String(".rmail");
		String cmd_smail = new String(".smail");
		String cmd_dmail = new String(".dmail");
		String cmd_description = new String(".description");
		String cmd_set = new String(".set");
		String cmd_idle = new String(".idle");
		String cmd_examine = new String(".examine");
		String cmd_promote = new String(".promote");
		String cmd_demote = new String(".demote");
		String cmd_afk = new String(".afk");
		String cmd_remove = new String(".remove");
		String cmd_revtells = new String(".revtells");
		String cmd_ctells = new String(".ctells");
		String cmd_delete = new String(".delete");
		String cmd_nuke = new String(".nuke");
		String cmd_version = new String(".version");
		String cmd_help = new String(".help");
		String cmd_colors = new String(".colors");
		String cmd_think = new String(".think");
		String cmd_sing = new String(".sing");
		String cmd_echo = new String(".echo");
		String cmd_syslog = new String(".syslog");
		String cmd_last = new String(".last");
		String cmd_beep = new String(".beep");
		String cmd_wake = new String(".wake");
		String cmd_site = new String(".site");
		String cmd_shout = new String(".shout");
		String cmd_semote= new String(".semote");
		String cmd_people= new String(".people");	
		String cmd_greet= new String(".greet");	
		String cmd_main = new String(".main");
		String cmd_mroom = new String(".mroom");
		String cmd_clearscreen = new String(".cls");

		boolean cleantelnet=false;
				
		// These booleans are responsible for the script taking place in the login section.
		// At each step, the booleans are changed so that the next step may take place.
		boolean newuser = false;
		boolean printednameheader = false;
		boolean readloginname = false;
		boolean printedpassheader = false;

		//*****************************************************************
		//  Start of Login Section
		//*****************************************************************
		try {
			// If the user has not authenticated, keep the user in the
			// Login section.  If the user asks to quit, let him.

			while ((authenticated==false) && (quit==false)) {

				// This sleep call keeps us from using up all CPU
				try { 
					Thread.sleep(100); 
					if (in.getLineNumber() > 200) {
						
						try {
							in.close();
						}
						catch (Exception x) {
							t.printSyslog("Exception closing LineNumberReader: "+x.getMessage());
						}
						try {
							in = null;
						}
						catch (Exception x) {
							t.printSyslog("Exception nulling LineNumberReader: "+x.getMessage());
						}
						try {
							in = new LineNumberReader (new InputStreamReader (socket.getInputStream()),1);

						}
						catch (Exception x) {
							t.printSyslog("Exception recreating LineNumberReader: "+x.getMessage());
						}
					}
				}
				catch (InterruptedException ie) { 
					t.printSyslog("Interrupted Exception!"+ie.getMessage());
					continue; 
				}

				// If the PrintWriter is not initialized, continue.
				if (outset==false) {
					continue;
				}


				// First, we check to see if this is the New User section, if not,
				// use this main section, which is for normal logins.
				if (newuser == false) {

					if ((printednameheader==false) && (readloginname==false) && (printedpassheader==false)) {

						println("");
						println("~FGNew Users, Enter: New~N");
						print("~FGWhat is your Name?~N ");
						out.flush();
						printednameheader = true;
					}
					
					boolean ready=false;
					try {
						ready = in.ready();
					}
					catch (Exception e) {
						// t.printSyslog("Exception Reading Ready in Login");
						ready=false;
						break;
					}


					if (ready && (printednameheader==true) && (readloginname==false) && (printedpassheader==false)) {
						try {
							// Read the user Login Name, since we're passing it to the database, escape it.
							loginname = t.escapeUserInput((in.readLine()).trim());
						}
						catch (Exception x) {
							t.printSyslog("Exception reading Username:"+x.getMessage());
						}

						// Telnet Protocol Includes some Negotiation
						// We want to clear it out if it's there...
						if (cleantelnet==false) {	
							try {
								loginname = cleanTelnet(loginname);
							}
							catch (Exception e) {
								t.printSyslog("UserConnection.run cleanTelnet() Exception: "+e.getMessage());
								quit=true;
								break;
							}
						}
						
						readloginname = true;
						idle = 0;

						loginname = loginname.trim();
						if ((loginname.length() >= 3)) {

							// Properly format the username.
							String fl = loginname.substring(0,1);
							fl = fl.toUpperCase();
							String el = loginname.substring(1,loginname.length());
							el = el.toLowerCase();
							loginname = fl.concat(el);
							loginname = loginname.replace(' ','x');

							// If the login name is Shutmedown, kill the system
							if (loginname.equals("Shutmedown")) {
								t.shutdown();
								break;
							}

							// If the login name is New, we set the booleans to use
							// the New User Setup Mode.
							if (loginname.equals("New")) {
								println("~FYEntering New User Setup Mode...~N");
								newuser = true;
								printednameheader = false;
								readloginname = false;
								printedpassheader = false;
								continue;
							}

							// This will restart the login section, but disable the
							// colors for the Chatlet.
							if (loginname.equals("Chatlet_coloroff")) {
        							printednameheader = false;
								readloginname = false;
								printedpassheader = false;
								chatlet=true;
								colorpref=0;
								continue;
							}

							// This restarts the login section and then changes
							// the color parsing to leave the color tags intact
							// so that client-side color parsing may take place.
							if (loginname.equals("Chatlet_coloron")) {
        							printednameheader = false;
								readloginname = false;
								printedpassheader = false;
								colorpref=1;
								chatlet=true;
								continue;
							}

							// If the user types quit, we'll just quit them.
							if (loginname.equals("Quit")) {
								println("~FYNow Quitting...~N");
								quit=true;
								break;
							}

							if (loginname.equals("Who")) {
								printednameheader = false;
								readloginname = false;
								printedpassheader = false;
								t.who (index);		
								continue;
							}
							
							println("Your name is now "+loginname);
						}
						else {
							println("~FRInvalid Username~N");
							printednameheader = false;
							readloginname = false;
							printedpassheader = false;

							continue;
						}
					}

					if ((printedpassheader==false) && (readloginname==true) && (printednameheader==true)) {
						print("~FGWhat is your Password?~N ");
						out.flush();
						printedpassheader=true;
					}

					if ((in.ready()) && (printedpassheader==true) && (readloginname==true) && (printednameheader==true)) {

						// Read Password, and Escape Input, since it will go to the database.
						password = t.escapeUserInput(in.readLine());
						idle = 0;
						password = password.trim();
						if (password.length() >= 3) {
							println("Your password is "+password);

							// Depending on the system, (Windows 2000 or not), encrypt the password or not.
							if (t.getEncryptPasswords()==true) {
								sql = "SELECT username,password FROM user WHERE username='"+loginname+"' AND password=encrypt('"+password+"', 'PW')";
							}
							else {
								sql = "SELECT username,password FROM user WHERE username='"+loginname+"' AND password='"+password+"'";
							}
							try {
								String tname = "";
								rs = t.stmt.executeQuery(sql);

								// If a username / password combination is found in the database, we have authenticated.
								if (rs.next()) { 
									tname = rs.getString(1);
								}

								// If not, we have failed authentication, and we'll just start over.
								else {
									//System.out.println("SQL: "+sql);
									//System.out.println("rs.next() failed");
									t.printSyslog("Username and Password Failure by '"+loginname+"' from "+getSite());
									tname = "nothing";
									println("~FRInvalid Username and Password Combination.  Try again.~N");
									printednameheader = false;
									readloginname = false;
									printedpassheader = false;
									continue;
								}
								
								rs.close();

								// Okay, suppose we do properly authenticate.  Lets now check the system
								// to see if this user is already signed in.  If so, the other is treated
								// like a connection timeout, and the present UserConnection is authenticated.

								if (t.getUserIndex(tname)!=-1) {
									t.printSyslog("Multiple Instances of same User Logged In");
									println("~FRYou are already signed on!~N");
									println("~FRDropping Old Connection...~N");
									t.conn[t.getUserIndex(tname)].replaced=true;
									t.conn[t.getUserIndex(tname)].quit();
									name = loginname;
									authenticated=true;
									loadUserData();
									replaced=true;
								}

								// Authenticate this UserConnection.

								else {
									if (tname.equals(loginname)) {
										name = loginname;
										authenticated = true;
										loadUserData();
									}
								}
							}
							catch (Exception e) {
								t.printSyslog("Invalid Username and Password Exception");
								t.printSyslog("Probably means SQLException, and DB Connection is dropped");
								e.printStackTrace();

								try {
									rs.close();
									t.printSyslog("Successfully Closed ResultSet after Exception: "+e.getMessage());
								}
								catch (Exception ex) { 
									; // Exception is expected here. If we get one, it's cool. 
								}
								
								
								t.printSyslog("Attempting to Reconnect the Database...");
								if (t.connectDB()==false) {
									t.printSyslog("Failed to Reconnect the Database...");
								}
								else {
									t.printSyslog("Database Successfully Reconnected.");
									println("Please try again.");
								}
								printednameheader = false;
								readloginname = false;
								printedpassheader = false;

								continue;
							}

						}
						else {
							println("~FRInvalid Username and Password.~N");
							printednameheader = false;
							readloginname = false;
							printedpassheader = false;
						}
					}
				}

				// This is the New User Setup section.  If the user has
				// authenticated in the login section, this section is skipped.

				else {
					try {
						if ((printednameheader==false) && (readloginname==false) && (printedpassheader==false))  {
							println("~FYNew User Setup has been reached!~N");

							print("\n~FGWhat username would you like to have?~N ");
							out.flush();
							printednameheader=true;
						}

						if ((in.ready()) && (printednameheader==true) && (readloginname==false) && (printedpassheader==false)) {

							// Read the Loginname, and Escape it, since it goes into the database.
							loginname = t.escapeUserInput(in.readLine());
							idle = 0;

							if ((loginname.length() > 0) && (loginname.length() < 13))  {

								// Properly format the Username.
								loginname = loginname.trim();
								String fl = loginname.substring(0,1);
								fl = fl.toUpperCase();
								String el = loginname.substring(1,loginname.length());
								el = el.toLowerCase();
								loginname = fl.concat(el);
								loginname = loginname.replace(' ','x');

								// If the user wants to quit, let him.
								if (loginname.equals("Quit")) {
									println("~FYNow Quitting...~N");
									quit=true;
									break;
								}

								// Users can't have "New" as a username.
								if (loginname.equals("New")) {
									println("~FRYou can't use that for a Username!~N");
									printednameheader = false;
									readloginname = false;
									printedpassheader = false;
									continue;
								}

								// Next, check to see if that username has been uesd already.
								if (t.isUser(loginname)) {
									println("~FRThat username is already in use.  Please pick another.~N");
									println("~FRIf you really are ~FY\""+loginname+"\"~FR, type Quit and log in the normal way.~N");
									printednameheader = false;
									readloginname = false;
									printedpassheader = false;

									continue;
								}
								readloginname = true;
							}
							else {
								println("~FRIllegal Username.~N");
								printednameheader = false;
								readloginname = false;
								printedpassheader = false;
								continue;
							}
						}

						if ((printednameheader==true) && (printedpassheader==false) && (readloginname==true)) {
							println("Your name is now "+loginname);
							print("\n~FGWhat Password would you like to use?~N ");
							out.flush();
							printedpassheader=true;
						}
						if ((in.ready()) &&  (printedpassheader=true) && (readloginname==true) && (printednameheader==true)) {

							// Read and Escape the password.
							password = t.escapeUserInput(in.readLine());
							idle = 0;
							if (password.length() > 0) {
								out.println("Your password is "+password);
								try {

									// If the system does not support encryption (Win2000), don't encrypt the passwords.
									if (t.getEncryptPasswords()==true) {
										sql = "INSERT INTO user (username, password) VALUES (\""+loginname+"\",encrypt('"+password+"','PW'))";
									}
									else {
										sql = "INSERT INTO user (username, password) VALUES (\""+loginname+"\",'"+password+"')";
									}
									int sqlrows = t.stmt.executeUpdate(sql);

									if (sqlrows == 1) {
										authenticated = true;
										name = loginname;
										saveUserData();
									}
								}
								catch (Exception e) {
									println("Please try again!");
									t.printSyslog("Invalid New User and Password Exception");
									t.printSyslog("Probably means SQLException, and DB Connection is dropped");
									e.printStackTrace();

									t.printSyslog("Attempting to Reconnect the Database...");
									if (t.connectDB()==false) {
										t.printSyslog("Failed to Reconnect the Database...");
									}
									else {
										t.printSyslog("Database Successfully Reconnected.");
										out.println("Please try again.");
									}
									printednameheader = false;
									readloginname = false;
									printedpassheader = false;

									continue;
								}
							}

							else {
								println("~FRInvalid Username and/or Password?~N");
								printednameheader = false;
								readloginname = false;
								printedpassheader = false;

								continue;
							}
						}
					}
					catch (Exception e) {
						t.printSyslog("Exception Caught in New User Setup!");
						//e.printStackTrace();
					}
				}
			}
		}
		catch (Exception e) {
			t.printSyslog("Error Authenticating:  Caught Exception");
			e.printStackTrace();
		}

		// This should work around the Locu Authentication Bypass
		if (name.equals("Unnamed")) {
			quit=true;
		}
		
		// Print announcement to system that another user has just signed on or reconnected

		if ((quit==false) && (replaced==false)) {
			saveUserData();
			println("\n\n");
			t.printLoginLogoutMessage("JOINING US:",index);
			//t.printSystemMessage("~FY~BEJOINING US: "+name+" "+desc+"~N", index);
			t.printSyslog(""+name+" signed on from "+getInetAddress());
			t.area[0].look(this);
			t.checkNewMail(this);
		}

		if ((quit==false) && (replaced==true)) {
			saveUserData();
			println("\n\n");
			t.printLoginLogoutMessage("REJOINING US:", index);
			//t.printSystemMessage("~FY~BEREJOINING US: "+name+" "+desc+"~N", index);
			t.printSyslog(""+name+" signed on again from "+getSite());
			t.area[0].look(this);
			t.checkNewMail(this);
			replaced=false;
		}

		//*****************************************************************
		//  End of Login Section, Start of Talker Section
		//*****************************************************************

		try {
			while (quit==false) {

				// First Sleep so we don't eat all the CPU.
				try { 
		
					Thread.sleep(100); 
					if (in.getLineNumber() > 200) {
						try {
							in.close();
						}
						catch (Exception x) {
							t.printSyslog("Exception closing LineNumberReader: "+x.getMessage());
						}
						try {
							in = null;
						}
						catch (Exception x) {
							t.printSyslog("Exception nulling LineNumberReader: "+x.getMessage());
						}
						try {
							in = new LineNumberReader (new InputStreamReader (socket.getInputStream()),1);
						}
						catch (Exception x) {
							t.printSyslog("Exception recreating LineNumberReader: "+x.getMessage());
						}

					}				
				}

				catch (InterruptedException ie) { 
					t.printSyslog("Interrupted Exception!"+ie.getMessage());
					continue; 
				}


				try {
					if (!socket.isBound()) t.printSyslog("Socket is NOT Bound");
					if (socket.isInputShutdown()) t.printSyslog("Socket Input Shutdown");
					if (socket.isOutputShutdown()) t.printSyslog("Socket Output Shutdown");
						
				}
				catch (Exception x) {
					t.printSyslog("Exception Testing Socket: "+x.getMessage());
				}


				if (socket.isClosed()) {
					t.printSyslog("Socket IsClosed is True- Name: "+loginname);						
					break;
				}
				try {
					// If there is Input waiting to be read, read it.
					if (in.ready()) {
						if (quit==true) break;

						String str = in.readLine();

						str = unControlString(str);

						String rest = "";
						if (str.length()<=0) continue;
						st = new StringTokenizer(str);

						// Parse through the tokens and build two variables:
						// cmd - the first token (or command) on the line
						// rest - everything else on the line.
						try {
							if (!st.hasMoreTokens()) continue;
							cmd = (st.nextToken()).toLowerCase();
							rest = "";
							while (st.hasMoreTokens()) {
								rest = rest.concat(" ");
								rest = rest.concat(st.nextToken());
							}
							rest = rest.trim();
						}
						catch (Exception e) {
							t.printSyslog("UserConnection Run: String Tokenizer Exception");
							e.printStackTrace();
						}

						// Any input from the user is enough to set the idle count to 0.
						idle = 0;

						// If a user was AFK, any input brings them back... Also print AFK info.
						if (getAfk()) {
							println("You return to the keyboard.");
							if (getAfkTells()) {
								println("There were .tells while you were away:");
								setAfkTells(false);
								showTell();
							}

							setAfk(false);
							t.area[getArea()].printArea(this, name+" is back.", false);
						}


						// Next, we check for NUTS talker shortcuts and convert them when necessary.
						if (cmd.startsWith(";;")) { rest = (cmd.substring(2,cmd.length()).concat(" "+rest)).trim(); cmd=".remote"; }
						if (cmd.startsWith(";")) { rest = (cmd.substring(1,cmd.length()).concat(" "+rest)).trim(); cmd=".emote"; }
						if (cmd.startsWith(":")) { rest = (cmd.substring(1,cmd.length()).concat(" "+rest)).trim(); cmd=".emote"; }
						if (cmd.startsWith("/")) { rest = (cmd.substring(1,cmd.length()).concat(" "+rest)).trim(); cmd=".remote"; }
						if (cmd.startsWith("+")) { rest = (cmd.substring(1,cmd.length()).concat(" "+rest)).trim(); cmd=".echo"; }


						// If we don't start with a ".", why even bother comparing to commands?
						if (cmd.startsWith(".")==false) { t.say(index, str); continue; }

						// Finally, we compare cmd to the command list for matches.
						// We use startsWith() instead of equals() to permit us to use command shortcuts,
						// for example, .w instead of .who.  Additionally, we implement checklevel to
						// make sure commands are not executed unless users are past a certain level.

						if (cmd_emote.startsWith(cmd)) { t.emote(index, rest); continue; }
						if (cmd_who.startsWith(cmd)) { t.who(index); continue; }
						if (cmd_tell.startsWith(cmd)) { t.tell(index, rest); continue; }
						if (cmd_go.startsWith(cmd)) { t.goArea(index, rest); continue; }
						if (cmd_read.startsWith(cmd)) { t.area[area].read(this); continue; }
						if (cmd_write.startsWith(cmd)) { t.area[area].write(this, rest); continue; }
						if (cmd_wipe.startsWith(cmd)) { if (checklevel(1)) { t.area[area].wipe(this, rest); } continue; }
						if (cmd_to.startsWith(cmd)) { t.modspeech(this, rest, ".to"); continue; }
						if (cmd_topic.startsWith(cmd)) { t.area[area].setTopic(this, rest); continue; }
						if (cmd_quit.startsWith(cmd)) { saveUserData(); quit=true; printQuitMessage(); break; }
						if (cmd_shout.startsWith(cmd)) { t.shout(index, rest); continue; }
						if (cmd_semote.startsWith(cmd)) { t.semote(index,rest); continue; }
						if (cmd_shutdown.startsWith(cmd)) { if (checklevel(4)) { t.shutdown(); break; } continue; }
						if (cmd_look.startsWith(cmd)) { t.area[area].look(this); continue; }
						if (cmd_remote.startsWith(cmd)) { t.remote(index, rest); continue; }
						if (cmd_review.startsWith(cmd)) { t.area[area].showRev(this); continue; }
						if (cmd_cbuffer.startsWith(cmd)) { t.area[area].clearRev(this); continue; }
						if (cmd_rmail.startsWith(cmd)) { t.rmail(this); continue;}
						if (cmd_smail.startsWith(cmd)) { t.smail(this, rest); continue; }
						if (cmd_dmail.startsWith(cmd)) { t.dmail(this, rest); continue; }
						if (cmd_description.startsWith(cmd)) { rest = new String("desc ").concat(rest); t.setUserProperty(this, rest); saveUserData(); continue; }
						if (cmd_set.startsWith(cmd)) { t.setUserProperty(this, rest); saveUserData(); continue; }
						if (cmd_idle.startsWith(cmd)) { t.idle(this); continue; }
						if (cmd_examine.startsWith(cmd)) { t.examine(this, rest); continue; }
						if (cmd_promote.startsWith(cmd)) { if (checklevel(3)) { t.promote(this, rest); } continue; }
						if (cmd_demote.startsWith(cmd)) { if (checklevel(3)) { t.demote(this, rest); } continue; }
						if (cmd_afk.startsWith(cmd)) {  t.setAfk(this,rest); continue; }
						if (cmd_remove.startsWith(cmd)) { if (checklevel(3)) { t.remove(this,rest); } continue; }
						if (cmd_revtells.startsWith(cmd)) { showTell(); continue; }
						if (cmd_ctells.startsWith(cmd)) { clearTell(); continue; }
						if (cmd_delete.startsWith(cmd)) { if (checklevel(3)) { t.deleteUser(this,rest); } continue; }
						if (cmd_nuke.startsWith(cmd)) { if (checklevel(3)) { t.deleteUser(this,rest); } continue; }
						if (cmd_version.startsWith(cmd)) { t.version(this); continue; }
						if (cmd_help.startsWith(cmd)) { t.help(this, rest); continue; }
						if (cmd_colors.startsWith(cmd)) { t.displayColors(this); continue; }
						if (cmd_think.startsWith(cmd)) { t.modspeech(this, rest, ".think"); continue; }
						if (cmd_sing.startsWith(cmd)) { t.modspeech(this, rest, ".sing"); continue; }
						if (cmd_echo.startsWith(cmd)) { t.modspeech(this, rest, ".echo"); continue; }

						if (cmd_syslog.startsWith(cmd)) { if (checklevel(3)) { t.syslog(this); } continue; }
						if (cmd_last.startsWith(cmd)) { if (checklevel(3)) { t.syslog(this,true); } continue; }
						if (cmd_beep.startsWith(cmd)) { t.beep(index, rest, 0); continue; }
						if (cmd_wake.startsWith(cmd)) { t.beep(index, rest, 1); continue; }
						if (cmd_site.startsWith(cmd)) { if (checklevel(3)) { t.site(index, rest); } continue; }
						if (cmd_people.startsWith(cmd)) {if (checklevel(2)) { t.who(index); } continue; }	
						if (cmd_greet.startsWith(cmd)) { t.greet(this, rest); continue; }
						if (cmd_main.startsWith(cmd)) { t.goArea(index, "Great Hall"); continue; }
						if (cmd_mroom.startsWith(cmd)) { t.goArea(index, "Great Hall"); continue; }
						if (cmd_clearscreen.startsWith(cmd)) { t.clearscreen(this); continue; }	
						
						// If nothing matches, it is treated as a say().
						t.say(index, str);
					}
					//else {
					//	System.out.println("UserConnection "+index+" Not Ready!");
					//	Thread.sleep(100);
					//	continue;
					//}
				}
				catch (NullPointerException npe) {
					t.printSyslog("UserConnection Run: Null Pointer Exception...");
					// I found that sometimes, all the socket setup
					// that takes place in Talker takes longer than
					// it takes for the Connection class to start its
					// thread.  This results in the in.readLine() up
					// there still not being initialized.  We just
					// try again if we get a N.P.E.
					continue;
				}
				catch (Exception e) {
					//Socket Exception and Array Out of Bounds
					t.printSyslog("Time out or Connection drop: "+e.getMessage());
					break;
				}
			}
		}
		catch (Exception e) {
			t.printSyslog("UserConnection Run Exception: "+e.getMessage());
		}
		finally {
			try {
				// First, announce a user is leaving (if the user wasn't replaced)
				if (replaced==false) {
					desc = t.unescapeUserInput(desc);
					
					if (!getUserName().equals("Unnamed")) {
						t.printLoginLogoutMessage("LEAVING US:", index);
					}
				}

				// Close Input and Output Streams and the Socket.
				try {
					rs.close();
					//t.printSyslog("Database Connection Not Closed! UserConnection");
				} catch (Exception e) { 
					//t.printSyslog("OK Error, right? Exception: "+e.getMessage());
					; // Exceptions are OK here...  
				}
				
				try {	
					in.close();
				} catch (Exception e) {
					t.printSyslog("Exception Closing 'in': "+e.getMessage());
				}		
		
				try {
					out.flush();
				}
				catch (Exception e) {
					t.printSyslog("Exception Flushing 'out': "+e.getMessage());
				}
				try {
					out.close();
				}
				catch (Exception e) {
					t.printSyslog("Exception closing 'out':"+e.getMessage());
				}				

				try {
					socket.close();
				}
				catch (Exception e) {
					t.printSyslog("Exception closing userconnection 'socket':"+e.getMessage());
				}				

				t.printSyslog(""+getUserName()+" is disconnected.");

				
				// Next, t.quitUser() resizes the UserConnection array.
				try {
					//t.printSyslog("Calling QuitUser in UserConnection::run for "+getUserName());
					t.quitUser(index);
					//t.printSyslog("QuitUser Call in UserConnection::run Successful");
				}
				catch (Exception ex) {
					t.printSyslog("Failed to Call QuitUser: "+ex.getMessage());
				} 

			}
			catch (Exception e) {
				t.printSyslog("Exception in trying to close a connection");
				e.printStackTrace();
			}
		}
	}

	public void finalize() {
		try {
			in.close();
		}
		catch (Exception e) {
			t.printSyslog("Finalize - Exception closing BufferedReader"+e.getMessage());
		}
		
		try {
			out.close();
		}
		catch (Exception e) {
			t.printSyslog("Finalize - Exception closing PrintWriter"+e.getMessage());
		}
		
		try {
			socket.close();
		}
		catch (Exception e) { 
			t.printSyslog("Finalize - Exception closing Socket: "+e.getMessage());
		}
		
	}
	
	
	void printQuitMessage() {
	
		println("");
		println("");
		println("~FYThank you for visiting Dark Ages JavaTalker!~N");
		println("~FY           Please back soon!~N");
		println("");
		println("Disconnecting you...");
		println("");
	}
	
}  // End of Connection Class
//***************************************************************************
/** Talker is the Main JavaTalker class that links everything else together.
	<p>The Talker Class performs the connection handling portion of the Java Talker.
	The Main method of Talker opens a ServerSocket and then sits in a loop, accepting connections and then creating
	UserConnection Objects and spinning the connections off on their own thread.
	Most of the user commands reside in Talker, mostly because they need to communicate
	messages among other users on the system.  Talker also creates the connections to the
	database, builds the Area array and manages the UserConnection array. */

public class Talker {
	/** the Talker version */
	String version = "JT 1.0.34";
	/** the date last updated */
	String updated = "5/08/2006";
	/** the person last doing the updates */
	String updater = "Jkb";

	/** This TalkerTimer instance links to the Talker and starts performing its time and connection management functions */
	TalkerTimer timer = new TalkerTimer(this);

	/** this is used to specify on which port we want Talker to run. */
	static final int PORT = 2121;

	/** this ServerSocket is used to accept connections on the port listed above. */
	static ServerSocket s;

	/** this Socket is a temporary spot for a connection until it is spun off onto its own UserConnection. */
	static Socket socket;

	/**  the UserConnection array, conn stores the UserConnections. */
	public UserConnection[] conn;

	/** the UserConnection array, ctemp is used to temporarily hold the conn array data during resizing. */
	public UserConnection[] ctemp;

	/** This is used to create new UserConnections and link them into the UserConnection Array. */
	UserConnection oneconn;

	/** This is a connection to the Database. */
	static Connection DBConnection = null;

	/** This statement is used to execute SQL commands in the database and return data. */
	static Statement stmt = null;

	/** This is used to store the current System Date. */
	StringBuffer datestamp = new StringBuffer();

	/** This is used to store the current System Time. */
	StringBuffer timestamp = new StringBuffer();

	/** This is used to describe how we want Dates formatted when we print them. */
	SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd/yyyy");

	/** this is used to describe how we want Times formatted when we print them. */
	SimpleDateFormat stf = new SimpleDateFormat("kk:mm");

	/** This Array is used to store all of the Areas available to the users on the Java Talker. */
	public Area[] area;

	/** This boolean tells the system whether or not to encrypt the passwords when reading and writing to the User table in the database. */
	private boolean encryptpasswords=true;

	/** main is the static method of Talker, which simply creates a new Talker instance. */
	public static void main (String[] args) {
		Talker c = new Talker();
	}

	public String centerText(String text, int pad) { 
		String ret = text.trim();
		boolean left = false;
		
		while (ret.length() < pad) {
			
			if (left) {
				ret = " "+ret;
				left = false;
			}
			else {
				ret = ret + " ";
				left = true;
			}
		}
				
		return ret;
	}
	
	/** connectDB() tests the connection to the Database.  If the connection is not made,
		connectDB reopens it using the MySQL Connector-J libraries.
		@return boolean that describes whether or not the database connection was successful.*/
	public boolean connectDB() {
		if (DBConnection != null) {
			try {
				if (DBConnection.isClosed()==false) {
					//printSyslog("Database Connection is already Open.");
					return true;
				}
			}
			catch (Exception e) {
				printSyslog("Exception caught while checking to see if database was open");
				e.printStackTrace();
			}
		}

		// Initiate Connection to the Database using the Connector-J JDBC drivers.
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			DBConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/javatalker", "talker", "password");
			stmt = DBConnection.createStatement();
			stmt.setEscapeProcessing(true);
		}
		catch (Exception e) {
			printSyslog("ConnectDB: Error Opening Database: "+e);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/** getEncryptPasswords() simply returns the boolean value of whether or not the
		database supports encrypted passwords.
		@return boolean that says whether or not the database supports the encrypt() function.*/
	public boolean getEncryptPasswords() {
		return encryptpasswords;
	}

	/** setEncryptPasswords() runs a test on the database to determine whether or not the database will support encrypted passwords.
	If the test passes an encrypted string back to the Java Talker, encryptpasswords is set to true and the system will use encrypted
	passwords in the future.  Otherwise, the system will not use encrypted passwords ini the database. */
	public void setEncryptPasswords() {

		// Connect to Database.
		connectDB();
		printSyslog("Entered setEncryptPasswords()");

		// We're going to ask for an encrypted version of the word 'PASSWORD'.
		String sql = "SELECT encrypt('PASSWORD', 'PW')";
		String answer="";
		try {
			// Run the Query...
			ResultSet rs = stmt.executeQuery(sql);

			// See if we get an answer...
			if (rs.next()) {

				answer = rs.getString(1);
				printSyslog("setEncryptPasswords(): "+answer);

				// If we get an answer, the encrypt method is supported.
				if (answer!=null) {
					printSyslog("Answer is probably an encrypted string. True");
					encryptpasswords = true;
				}

				// If we don't, there is no encrypt method, so the system assumes we're
				// asking for a user-defined method that doesn't exist, so we get
				// a null answer.  In which case, we turn off encrypting of passwords.
				else {
					printSyslog("Answer is probably NULL.");
					encryptpasswords = false;
				}
			}
			else {
				// Additionally, if we are unable to get even a null answer
				// we don't encrypt the passwords.
				printSyslog("Answer is probably NULL.");
				encryptpasswords = false;
			}
			rs.close();
		}
		catch (Exception e) {
			printSyslog("Exception in setEncryptPasswords()");
			e.printStackTrace();
			encryptpasswords=false;
		}
		if (encryptpasswords) answer = "True";
		else answer = "False";

		// Display to the Syslog the encryption status
		printSyslog("Exited setEncryptPasswords() with answer of: "+answer);
	}

	/** initDates() Reads the date and time from the system and stores it in some StringBuffers
		for later use in TalkerTimer, Syslog printout and the offline messaging. */
	public void initDates() {
		datestamp = new StringBuffer();
		timestamp = new StringBuffer();
		sdf.format(new java.util.Date(), datestamp, new FieldPosition(0));
		stf.format(new java.util.Date(), timestamp, new FieldPosition(0));
	}

	/** printSyslog() Prints a message along with the date and time stamp.  At present, this prints
		to System.out since the syslog is simply captured to a text file along with the stacktraces of
		any Exceptions that may be thrown.  In the future, we may want to write this data
		to a more formal file or database table for reading so certain users on the system can review the
		logs while online.
		@param message this is the message that gets printed along with the date and time. */
	public void printSyslog(String message, int priority) {
		String sql;
		message = escapeUserInput(message);
		System.out.println(datestamp+"; "+timestamp+":  "+message);
		if (connectDB()==true) {
			sql = "INSERT INTO syslog(timestamp, message, priority) VALUES ('"+datestamp+"; "+timestamp+"', '"+message+"', "+priority+")";
			try {
				stmt.executeUpdate(sql);			
			}
			catch (Exception e) {
				System.out.println(datestamp+"; "+timestamp+":   "+e);
				System.out.println(""+datestamp+";"+timestamp+": "+message);
			}
		}
	}

	public void printSyslog(String message) {
		printSyslog(message,0);
	}
	
	/** the Talker() constructor does most of the work in the Java Talker.  It initializes the dates and times.  It connects the database.
		it performs tests to see if the database will support encryption.  Finally, it listens for and manages the connections and the
		resizing of the UserConnection array. */
	public Talker() {
		// First, we initialize the time and date stamps.
		initDates();

		// Connect to the database.
		if (connectDB()==false) {
			printSyslog("Unable to Open the Database, System Exiting...");
			System.exit(1);
		}
		else {
			// Refresh the System Log
			String sql = "DELETE FROM syslog";
			try {
				stmt.executeUpdate(sql);
			}
			catch (Exception e) {
				printSyslog("Error Clearing the Syslog!",3);
			}
		}

		printSyslog("Booting Dark Ages on Port: "+PORT,3);
		printSyslog("JavaTalker "+version,3);
		printSyslog("Updated "+updated+" by "+updater,3);
		
		// Test whether or not we can encrypt passwords, and set variables accordingly.
		setEncryptPasswords();

		// Create UserConnection Array and Temporary UserConnection Array.
		conn = new UserConnection[0];
		ctemp = new UserConnection[1];

		// Create a temporary Area and initialize the area array.
		//Area ax = new Area(this);
		//area = ax.initArea(this);
		area = Area.initArea(this);

		int i=0;
		try {
			// Start ServerSocket
			s = new ServerSocket(PORT);
			printSyslog("Server Socket Started");
		}
		catch (Exception e) {
			printSyslog("Unable to Start Server Socket");
			e.printStackTrace();
			System.exit(1);
		}

		try {

			while (true) {
				// Sleep the thread for a little bit so we're not constantly hitting the CPU.
				try { Thread.sleep(100); }
				catch (InterruptedException ie) { continue; }

				try {
					// Listen to accept a socket, then block until we do.
					socket = s.accept();
					//printSyslog("Accepted a Socket Connection from: "+socket.getInetAddress().getHostName());
				}

				// If we get an exception here, it's because the ServerSocket is closed
				// since the system is shutting down.  Because the accept() method blocks,
				// the only way to shut down the system is to manually kill the ServerSocket.
				catch (Exception e) {
					printSyslog("System Shutting down, No Socket to Accept Connection");
					//e.printStackTrace();
					break;
				}

				// At this point, the system has stopped blocking and has a
				// connection in socket.

				try {
					// Assign the socket to a UserConnection.
					oneconn = null;
					oneconn = new UserConnection(this);
					oneconn.addConnection(socket);

					// Resize the Array.
					ctemp = new UserConnection[conn.length+1];
					for (i=0; i<conn.length; ++i) {
						ctemp[i] = conn[i];
					}

					// Add the new UserConnection to the end of it.
					ctemp[conn.length] = oneconn;
					conn = ctemp;

					// Delete the temporary array.
					ctemp = null;

					// Test: Delete oneconn?
					try {
						oneconn = null;
						//socket.close();
						socket = null;
					}
					catch (Exception x) {
						printSyslog("ServerSocket: Exception Closing accept socket: "+x.getMessage());
					}

					// set the index for the new UserConnection.
					conn[conn.length-1].setIndex(conn.length-1);

				}
				catch (Exception e) {
					printSyslog("Caught Exception while adding of a user to the conn array");
					e.printStackTrace();
					try {
						socket.close();
					}
					catch (Exception ee) {
						printSyslog("Error Closing Connection Socket");
						ee.printStackTrace();
					}
				}
			}
		}
		finally {
			try {
				s.close();
			}
			catch (Exception e) {
				printSyslog("Error Closing ServerSocket");
				e.printStackTrace();
			}
		}
	}

	
	
	boolean printLoginLogoutMessage(String onoff, int index) {
		for (int i=0; i<conn.length; ++i) {
			if (index==i) continue;

			try {
				// If a user is not authenticated, they don't get to see the message.
				if (!conn[i].isAuthenticated()) continue;
			} 
			catch (Exception e) { 
				printSyslog("Error Checking if conn is authenticated"); 
				printSyslog("Calling QuitUser in printLoginLogoutMessage()");
				quitUser(i); 
				printSyslog("Call to QuitUser successful");
				--i; 
				continue;
			}


			try {
				if (conn[i].getLevel() > 1) {
					conn[i].println("~FY~BE" + onoff +" "+ conn[index].getUserName() +" "+ conn[index].getUserDesc() +"~N ~FR["+ conn[index].getInetAddress().getHostName() + "]~N  ~FY["+ conn[index].getLevelName() +"]~N");
				}
				else {
					conn[i].println("~FY~BE" + onoff +" "+ conn[index].getUserName() +" "+ conn[index].getUserDesc() +"~N  ~FY["+ conn[index].getLevelName() +"]~N");
				}
			} catch (Exception ee) { printSyslog("Error Printing message to conn"); }
		}
		return false;
	}
	
	
	/** printSystemMessage() is used to print messages to all authenticated users across the system
		regardless of which room they are in.
		@return boolean indicating whether the command failed.
		@param message String containing the message we want to print to the users
		@param ignoreuser integer index of the any user we may want to skip. */
	boolean printSystemMessage(String message, int ignoreuser) {
		for (int i=0; i<conn.length; ++i) {
			if (ignoreuser==i) continue;

			try {
				// If a user is not authenticated, they don't get to see the message.
				if (!conn[i].isAuthenticated()) continue;
			} 
			catch (Exception e) { 
				printSyslog("Error Checking if conn is authenticated"); 
				printSyslog("About to call QuitUser in PrintSystemMessage()");
				quitUser(i); 
				printSyslog("Successfully called QuitUser in PrintSystemMessage()");
				--i; 
				continue;
			}


			try {
			conn[i].println(message);
			} catch (Exception ee) { printSyslog("Error Printing message to conn"); }
		}
		return false;
	}

	/** tell() is the means of sending online private messages from one user to another.
		in addition to getting the message, the private message gets added to the UserConnections
		personal tell buffer.
		@return boolean indicating whether or not an error occurred.
		@param from integer indicating the index of the user sending the .tell
		@param messagein is the String containing the recipient's name and the message. */
	boolean tell (int from, String messagein) {
		StringTokenizer st = new StringTokenizer(messagein);
		String to;
		String message;
		int toindex=-1;

		// First, parse off the recipient's name.
		if (st.hasMoreTokens()) to = st.nextToken();
		else {
			conn[from].println("~FR Usage:  .tell username message~N");
			return true;
		}

		// Next, pull off at least one more token to be the message.
		if (st.hasMoreTokens()) message = st.nextToken();
		else {
			conn[from].println("~FR Usage:  .tell username message~N");
			return true;
		}

		// If there are any more tokens, concatenate them onto the message.
		while (st.hasMoreTokens()) {
			message = message.concat(" ");
			message = message.concat(st.nextToken());
		}

		// get the index of the recipient in the UserConnection array.
		toindex = getUserIndex(to);

		// If the user is not signed on, report this.
		if (toindex==-1) {
			conn[from].println("\""+to+"\" is not signed on.");
			return true;
		}

		// If the user is AFK, print the AFK messages.
		if (conn[toindex].getAfk()) {
			conn[from].println(conn[toindex].getUserName()+" is AFK: "+conn[toindex].getAfkMsg());
			conn[toindex].setAfkTells(true);
		}

		// Print the message to each user's socket and tell buffer.
		conn[from].println("~FP-> You say to "+conn[toindex].getUserName()+": "+message+"~N");
		conn[from].addTell("~FP-> You say to "+conn[toindex].getUserName()+": "+message+"~N");

		conn[toindex].println("~FP-> "+conn[from].getUserName()+" tells you: "+message+"~N");
		conn[toindex].addTell("~FP-> "+conn[from].getUserName()+" tells you: "+message+"~N");

		return false;
	}
		
	boolean site (int from, String messagein) {
		StringTokenizer st = new StringTokenizer(messagein);
		String target;
		String message;
		int targetindex=-1;

		// First, parse off the recipient's name.
		if (st.hasMoreTokens()) target = st.nextToken();
		else {
			conn[from].println("~FR Usage:  .sitee username~N");
			return true;
		}
		
		// get the index of the recipient in the UserConnection array.
		targetindex = getUserIndex(target);

		// If the user is not signed on, report this.
		if (targetindex==-1) {
			conn[from].println("\""+target+"\" is not signed on.");
			return true;
		}
		else {
			conn[from].println("~FY\""+target+"\"~FR is signed on from:");
			conn[from].println("~FRSite: "+(conn[targetindex].getInetAddress()).getHostName());
			conn[from].println("~FRIP:   "+(conn[targetindex].getInetAddress()).getHostAddress()+" Port: "+conn[targetindex].getPort()+"~N");
			return false;
		}
	}
	
		
	
	boolean beep (int from, String messagein, int wake) {
		StringTokenizer st = new StringTokenizer(messagein);
		String to;
		String message;
		int toindex=-1;

		// First, parse off the recipient's name.
		if (st.hasMoreTokens()) to = st.nextToken();
		else {
			if (wake==1) {
				conn[from].println("~FR Usage:  .wake username [message]~N");
			}
			if (wake==0) {
				conn[from].println("~FR Usage:  .beep username [message]~N");
			}
			return true;
		}

		// Next, pull off at least one more token to be the message.
		if (st.hasMoreTokens()) {
			message = st.nextToken();
			
			// If there are any more tokens, concatenate them onto the message.
			while (st.hasMoreTokens()) {
				message = message.concat(" ");
				message = message.concat(st.nextToken());
			}
		}
		else {
			message="";
		}

		// get the index of the recipient in the UserConnection array.
		toindex = getUserIndex(to);

		// If the user is not signed on, report this.
		if (toindex==-1) {
			conn[from].println("\""+to+"\" is not signed on.");
			return true;
		}

		// If the user is AFK, print the AFK messages.
		if (conn[toindex].getAfk()) {
			conn[from].println(conn[toindex].getUserName()+" is AFK: "+conn[toindex].getAfkMsg());
			conn[toindex].setAfkTells(true);
		}

		// Print the message to each user's socket and tell buffer.
		conn[from].println("~FYYou send to "+conn[toindex].getUserName()+":~N");
		conn[from].println("~FR~BEAttention!  "+conn[from].getUserName()+" wants to talk to you!~N");
		if (!message.equals("")) conn[from].println("~FRMessage: "+message+"~N\n\r");
		
		conn[toindex].println("~FR~BEAttention!  "+conn[from].getUserName()+" wants to talk to you!~N");
		if (!message.equals("")) conn[toindex].println("~FRMessage: "+message+"~N\n\r");
		
		conn[toindex].addTell("~FP-> "+conn[from].getUserName()+" beeped you: "+message+"~N");

		return false;
	}

	/** remote() sends a private online message in the form of an emote to another user.  In addition to
		just printing the message, this will also put the message in the UserConnection tell buffer.
		@return boolean indicating whether or not an error has taken place.
		@param from is the index in the UserConnection Array of the user sending the remote message.
		@param messagein is the String containing the username of the recipient and the actual message to send. */
	boolean remote (int from, String messagein) {
		StringTokenizer st = new StringTokenizer(messagein);
		String to;
		String message;
		int toindex=-1;

		// Pull off the first token to be the username
		if (st.hasMoreTokens()) to = st.nextToken();
		else {
			conn[from].println("~FR Usage:  .remote username message~N");
			return true;
		}

		// Pull off the second token to be the message.
		if (st.hasMoreTokens()) message = st.nextToken();
		else {
			conn[from].println("~FR Usage:  .remote username message~N");
			return true;
		}

		// Concatenate all the rest of the tokens to the message.
		while (st.hasMoreTokens()) {
			message = message.concat(" ");
			message = message.concat(st.nextToken());
		}

		// Get the Index of the Recipient
		toindex = getUserIndex(to);

		// If the recipient is not signed on, report this and exit.
		if (toindex==-1) {
			conn[from].println("\""+to+"\" is not signed on.");
			return true;
		}

		// If the recipient is AFK, report this fact, but proceed with the remote.
		if (conn[toindex].getAfk()) {
			conn[from].println(conn[toindex].getUserName()+" is AFK: "+conn[toindex].getAfkMsg());
			conn[toindex].setAfkTells(true);
		}

		// Print the message to both sockets and add to the tell buffers
		conn[from].println("~FP-> You remote to "+conn[toindex].getUserName()+": "+conn[from].getUserName()+" "+message+"~N");
		conn[from].addTell("-> You remote to "+conn[toindex].getUserName()+": "+conn[from].getUserName()+" "+message+"~N");

		conn[toindex].println("~FP-> "+conn[from].getUserName()+" "+message+"~N");
		conn[toindex].addTell("-> "+conn[from].getUserName()+" "+message+"~N");
		return false;
	}

	/** promote() increments the user level of the recipient user to allow access to more commands.
		@return boolean indicating whether the promote was successful
		@param conn UserConnection of the user calling the .promote command
		@param other String containing the name of the user to be promoted.*/
	boolean promote (UserConnection conn, String other) {
		if (other.equals("")) {
			conn.println("~FR Usage:  .promote username~N");
			return false;
		}

		// Get the Index of the other user.
		int i = getUserIndex(other);

		if (i!=-1) {
			int level = this.conn[i].getLevel();

			// Check to see if target user is less than level 5.
			if (level < 5) {
				level++;
			}
			else {
				conn.println("~FRCan't promote past level 5~N");
				return false;
			}


			// The user can't be promoted to an equal or higher level than the person doing the promoting.
			if ((conn.getLevel()!=5) && (level>=conn.getLevel())) {
				conn.println("~FRCan't promote to an equal or higher level than yours.~N");
				return false;
			}

			// Set the level of the target user and then print the messages informing them of the level change.
			this.conn[i].setLevel(level);
			conn.println("You promote "+this.conn[i].getUserName()+" to "+this.conn[i].getLevelName());
			this.conn[i].println(conn.getUserName()+" promotes you to "+this.conn[i].getLevelName());
			printSyslog(""+conn.getUserName()+" promotes "+this.conn[i].getUserName()+" to "+this.conn[i].getLevelName());
		}
		else {
			conn.println("That user is not signed on.");
			return false;
		}

		// Save the UserData of the user promoted.
		this.conn[i].saveUserData();
		return true;
	}

	/** demote() decrements the user level of the recipient user to restrict access to commands.
		@return boolean indicating whether or not the demote was successful.
		@param conn UserConnection of the user calling the .demote command
		@param other String containing the name of the user to be demoted.*/

	boolean demote (UserConnection conn, String other) {
		if (other.equals("")) {
			conn.println("~FR Usage:  .demote username~N");
			return false;
		}

		// Get User Index.
		int i = getUserIndex(other);

		if (i!=-1) {
			int level = this.conn[i].getLevel();

			// Check to make sure we're not demoting past level 0.
			if (level >= 1) {
				level--;
			}
			else {
				conn.println("~FRCan't demote past level 0~N");
				return false;
			}

			// Can't demote users on equal or higher level than you.
			if (level>=conn.getLevel()) {
				conn.println("~FRCan't demote an equal or higher level than yours.~N");
				return false;
			}

			// Change the level and report to the other users.
			this.conn[i].setLevel(level);
			conn.println("You demote "+this.conn[i].getUserName()+" to "+this.conn[i].getLevelName());
			this.conn[i].println(conn.getUserName()+" demotes you to "+this.conn[i].getLevelName());
			printSyslog(""+conn.getUserName()+" demotes "+this.conn[i].getUserName()+" to "+this.conn[i].getLevelName());
		}
		else {
			conn.println("That user is not signed on.");
			return false;
		}

		// Save the user settings.
		this.conn[i].saveUserData();
		return true;
	}

	/** version() displays version information about the JavaTalker.
		@return boolean indicating whether or not errors have occurred.
		@param conn UserConnection to which the version information gets printed. */
	boolean version (UserConnection conn) {
		conn.println("");
		conn.println("~FG+====================================================+~N");
		conn.println("~FG+  Jkb JavaTalker    Version: ~FY"+version+"~N");
		conn.println("~FG+  Last Updated: ~FY"+updated+"~FG  By: ~FY"+updater+"~N");
		conn.println("~FG+====================================================+~N");
		conn.println("~FG+ The Jkb JavaTalker was designed and developed by   +~N");
		conn.println("~FG+ Jeffrey K. Brown for a graduate project at the     +~N");
		conn.println("~FG+ University of New Haven, CT as part of his pursuit +~N");
		conn.println("~FG+ of a Master's Degree in Computer Science. Enjoy!   +~N");
		conn.println("~FG+====================================================+~N");
		conn.println(" ");
		return false;
	}

	/** examine() looks up information about another user and provides this to the inquirer.
		@return boolean indicating whether or not an error has occurred.
		@param conn UserConnection of the user that the examine information gets printed to.
		@param otheruser Name of user inquiring about. */
	boolean examine (UserConnection conn, String otheruser) {
		int i=0;
		boolean online=true;

		// Get index of user.
		if (otheruser.equals("")) {
			i = conn.getIndex();
		}
		else {
			i = getUserIndex(otheruser);
			if (i==-1) {
				online = false;
				if (isUser(otheruser)==false) {
					conn.println("There is no such user "+otheruser);
					return true;
				}
				else {
					// Check to get the proper name format from the database.
					otheruser = getUserDBName(otheruser);
				}
			}
			else online = true;
		}

		// If the user is already online, we can simply pull the data from the UserConnection object.
		if (online==true) {
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY "+this.conn[i].getUserName()+" "+this.conn[i].getUserDesc()+"~N");
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY+ Level:    "+this.conn[i].getLevelName()+"~N");
			conn.println("~FY+ Age:      "+this.conn[i].getAge()+"~N");
			conn.println("~FY+ Photo:    "+this.conn[i].getPhoto()+"~N");
			conn.println("~FY+ Homepage: "+this.conn[i].getHomepage()+"~N");
			conn.println("~FY+ Email:    "+this.conn[i].getEmail()+"~N");
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY+ User is in area: "+area[this.conn[i].getArea()].getName()+"~N");
			conn.println("~FY+ Online: "+this.conn[i].getTime()+" Min;  Idle: "+this.conn[i].getIdle()+" Min."+"~N");
			conn.println("~FY+ Total Time: "+this.conn[i].getTotalTime()+"~N");

			// If the user is at a high enough level, we can view the user's site.
			if (conn.getLevel()>1) {
				conn.println("~FY+ Connected from: "+this.conn[i].getInetAddress().getHostName()+"~N");
			}
			conn.println("~BB~FY+======================================================================+~N");
			conn.println(""+this.conn[i].getProfile());
			conn.println("~BB~FY+======================================================================+~N");
			conn.println(" ");
			return false;
		}

		// If the user is not online, we must connect to the Database and pull the user record.
		else {
			connectDB();
			String sql="";
			String desc="";
			String profile="";
			String gender="";
			String email="";
			String homepage="";
			String photo="";
			String lastsite="";
			String laston="";
			int level=0;
			int totaltime=0;
			int age=0;

			try {
				// Create SQL statement.
				sql = "SELECT description,level,profile,totaltime,gender,age,email,homepage,photo,colorpref,lastsite,laston FROM user WHERE username='"+otheruser+"'";

				ResultSet rs = stmt.executeQuery(sql);
				if (rs.next()) {
					desc = rs.getString(1);
					if (desc==null) { desc = "A Generic Desc"; }
					level = rs.getInt(2);
					profile = rs.getString(3);
					if (profile==null) { profile = "This user has not yet set a profile"; }
					totaltime = rs.getInt(4);
					gender = rs.getString(5);
					if (gender==null) { gender = "Unknown"; }
					age = rs.getInt(6);
					email = rs.getString(7);
					if (email==null) { email = "me@noneofyourbusiness.com"; }
					homepage = rs.getString(8);
					if (homepage==null) { homepage = "darkages.darkfantastic.net/"; }
					photo = rs.getString(9);
					if (photo==null) { photo = "darkages.darkfantastic.net/none.jpg"; }
					lastsite = rs.getString(11);
					if (lastsite==null) { lastsite = "Who knows..."; }
					laston = rs.getString(12);
					if (laston==null) { laston = "The other day"; }
				}
				else {
					printSyslog("Unable to load UserData in .examine");
					return false;
				}
				rs.close();
			}
			catch (Exception e) {
				printSyslog("examine(): Exception caught trying to Load User Data");
				e.printStackTrace();
				return false;
			}

			// Print the data from the database.
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY "+otheruser+" "+desc+"~N");
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY+ Level:    "+conn.getLevelName(level,gender)+"~N");
			conn.println("~FY+ Age:      "+age+"~N");
			conn.println("~FY+ Photo:    "+photo+"~N");
			conn.println("~FY+ Homepage: "+homepage+"~N");
			conn.println("~FY+ Email:    "+email+"~N");
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY+ Last Signed on: "+laston+"~N");
			conn.println("~FY+ Total Time: "+totaltime+"~N");
			if (conn.getLevel()>1) {
				conn.println("~FY+ Last Connected from: "+lastsite+"~N");
			}
			conn.println("~BB~FY+======================================================================+~N");
			conn.println("~FY"+profile+"~N");
			conn.println("~BB~FY+======================================================================+~N");
			conn.println(" ");

			return false;

		}
	}

	boolean syslog (UserConnection conn) {
		syslog(conn, false);
		return false;
	}
	
	boolean syslog (UserConnection conn, boolean last) {
		ResultSet rs=null;
		String sql="SELECT timestamp,message,priority,id FROM syslog";
		
		String stamp ="";
		String message = "";
		int priority =0;
		String highlight="";
		int id=0;
		int lines=0;
		
		conn.println("~FG============================================================================~N");
		if (last) conn.println("~FY                       Dark Ages System Log (Last)                             ~N");
		else conn.println("~FY                          Dark Ages System Log                              ~N");
		conn.println("~FG============================================================================~N");
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				stamp ="";
				message = "";
				priority =0;
				highlight = "~N";
			
				stamp = rs.getString(1);
				message = rs.getString(2);
				priority = rs.getInt(3);
				id= rs.getInt(4);
				
				if (priority > 0) {
					highlight="~FY";
				}
				
				if (last==false) {
					conn.println(" ~FG"+stamp+":~N"+highlight+" "+message+"~N");
					lines++;
				}
			}
			rs.close();
			
			if (last==true) {
				lines=0;
				sql="SELECT timestamp,message,priority,id FROM syslog WHERE id>"+ (id-30);
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					stamp ="";
					message = "";
					priority =0;
					highlight = "~N";
			
					stamp = rs.getString(1);
					message = rs.getString(2);
					priority = rs.getInt(3);
					id= rs.getInt(4);
				
					if (priority > 0) {
						highlight="~FY";
					}
				
					
					conn.println(" ~FG"+stamp+":~N"+highlight+" "+message+"~N");
					lines++;
				}
				rs.close();
			
			}
		}
		catch (Exception e) {
			printSyslog("Exception in Syslog Printing: "+e);
			return true;
		}
		conn.println("~FG============================================================================~N");
		conn.println("~FY                       Total Lines: "+lines+"~N");
		conn.println("~FG============================================================================~N");
		return false;
	}
	
	
	/** help() pulls the help information from the database.  If a user specifies a topic,
		a database access is made to find the help topic.  If found, it returns the particular
		help entry.  If there is no topic specified, it will produce a list of all available
		help topics.
		@return boolean reporting whether or not the call was successful
		@param conn UserConnection of the user calling the .help command
		@param topic String with the topic of the help.*/
	boolean help (UserConnection conn, String topic) {
		String sql = "";
		ResultSet rs=null;

		// These Strings and column are used to
		// display the Help in three neat columns.
		String col1="";
		String col2="";
		String col3="";
		int column=0;

		// If no topic is specified, just print the list.
		if (topic.equals("")) {
			sql = "SELECT topic FROM help ORDER BY topic";

			try {
				rs = stmt.executeQuery(sql);

				conn.println("~FYAvailable Help Topics:~N");

				// Print the Results in columns.
				while(rs.next()) {

					if (column==0) {
						col1 = rs.getString(1);
						while (col1.length() < 20) {
							col1 = col1.concat(" ");
						}

						column=1;
						continue;
					}
					if (column==1) {
						col2 = rs.getString(1);
						while (col2.length() < 20) {
							col2 = col2.concat(" ");
						}
						column=2;
						continue;
					}
					if (column==2) {
						col3 = rs.getString(1);
						while (col3.length() < 20) {
							col3 = col3.concat(" ");
						}

						conn.println("" + col1 + col2 + col3);
						col1 = col2 = col3 = "";
						column=0;
						continue;
					}
				}
				if (column!=2) conn.println("" + col1 + col2 + col3);
				conn.println(" ");
				rs.close();
				return true;
			}
			catch (Exception e) {
				try { rs.close(); } catch (Exception ex) { printSyslog("help() exception! SQL:"+sql+" "+e.getMessage()); }
				printSyslog("help() exception! SQL:"+sql+" "+e.getMessage());
				return false;
			}
			
		}

		// If there is a topic specified, search the database for the
		// topic and print the results.
		else {

			try {
				sql = "SELECT topic,description FROM help WHERE LCASE(topic)='"+topic.toLowerCase()+"'";
				rs = stmt.executeQuery(sql);

				if (rs.next()) {
					String top = rs.getString(1);
					String desc = rs.getString(2);
					conn.println("\n~FYTopic: "+top+"~N\n\n"+desc+"\n\n");
					rs.close();
					return true;
				}
				else {
					conn.println("No Results Found for "+topic);
					rs.close();
					return false;
				}
			}
			catch (Exception e) {
				printSyslog("help() exception! SQL:"+sql);
				return false;
			}
		}
	}

	/** sendMessage() is used to display a message to users on the system
		regardless of the area they are in.  This is used to print the Joining/Leaving
		messages.
		@param from integer index of user sending the message
		@param message String message to be printed.
		@param toself integer determining whether or not to print the message to "from" user */
	void sendMessage(int from, String message, int toself) {
		int i;

		for (i=0; i<conn.length; ++i) {

			if ((toself==0) && (from==i)) {
				try {
					//System.out.println("Msg From "+from+", To "+i+"(ignored):"+message);
					if (conn[i].isAuthenticated()) conn[i].println("You say: "+ message);
					continue;
				}
				catch (Exception e) {
					printSyslog("SendMessage(): Exception Printing Back to User From:"+from+" i:"+i+"toself:"+toself);
					e.printStackTrace();
					continue;
				}
			}
			else {
				try {
					;
					//System.out.println("Msg From "+from+", To "+i+":"+message);
				}
				catch (Exception e) {
					printSyslog("SendMessage(): Exception Printing to System.out From:"+from+" i:"+i+"toself:"+toself);
					e.printStackTrace();
					continue;
				}
			}

			try {
				if (conn[i].isAuthenticated()) conn[i].println(""+from+" says: "+message);
			}
			catch (Exception e) {
				printSyslog("SendMessage(): Exception Printing to User:"+i+" From:"+from+" toself:"+toself);
				e.printStackTrace();
				continue;
			}
		}
	}

	public void clearscreen (UserConnection user) {
		for (int i=0; i<52; ++i) user.println("");
	}


	/** who() produces a list of users on the Java Talker.
		@param index integer index of the user making the call to .who */
	public void who (int index) {
		int i;

		String location="";
		String timestr="";
		String levelstr="";
		int locsize=0;
		int timesize=0;
		int levsize=0;
		int countedusers =0;

		conn[index].println("~BB~FY+----------------------------------------------------------------------------+~N");
		conn[index].println("~FY+" + centerText("Users Currently On Dark Ages",76) + "+~N");
		
		conn[index].println("~BB~FY+----------------------------------------------------------------------------+~N\n");

		// To make the columns neat, we get a count of the longest String in each column and
		// size the entire column to match.  We pad the smaller strings with spaces to do this.
		for (i=0; i<conn.length; ++i) {

			if (conn[i].isAuthenticated()==false) continue;
			countedusers++;
			
			location = area[conn[i].getArea()].getName();
			if (conn[i].getAfk()) location = "<AFK>";
			if (location.length() > locsize) {
				locsize = location.length();
			}

			timestr = new Integer(conn[i].getTime()).toString();
			if (timestr.length() > timesize) {
				timesize = timestr.length();
			}

			levelstr = conn[i].getLevelName();
			if (levelstr.length() > levsize) {
				levsize = levelstr.length();
			}
		}

		// Print the lines of the users connected, padding the shorter names with spaces.
		for (i=0; i<conn.length; ++i) {
			String stat = "  ";
			// Don't list unauthenticated users.
			if (conn[i].isAuthenticated()==false) continue;

			location = area[conn[i].getArea()].getName();
			if (conn[i].getAfk()) location = "<AFK>";
			while (location.length() < locsize) {
				location = location.concat(" ");
			}

			timestr = new Integer(conn[i].getTime()).toString();
			while (timestr.length() < timesize) {
				timestr = timestr.concat(" ");
			}

			levelstr = conn[i].getLevelName();
			while (levelstr.length() < levsize) {
				levelstr = levelstr.concat(" ");
			}

			conn[index].println("~FY"+stat+
				location+"~N | "+
				timestr+" mins. | "+
				"~FG"+levelstr+"~N | "+
				"~FY"+conn[i].getUserName()+"~N "+
				unescapeUserInput(conn[i].getUserDesc())+"~N");

		}
		if (countedusers==0) {
			conn[index].println("\n~FY                          There are no users online!~N\n");
		}
		
		conn[index].println("\n~BB~FY+----------------------------------------------------------------------------+~N");
		if (countedusers==1) conn[index].println("~FY+" + centerText("Total of 1 User Online",76) + "+~N");
		else conn[index].println("~FY+" + centerText("Total of "+countedusers+" Users Online",76) +"+~N");
		conn[index].println("~FY+" + centerText(""+datestamp+" "+timestamp,76) + "+~N");
		conn[index].println("~BB~FY+----------------------------------------------------------------------------+~N");
		conn[index].println(" ");
	}

	/** idle() prints a list of the idle times for each of the users on the system.
		@param conn UserConnection of the user making the call to .idle */
	public void idle (UserConnection conn) {
		int i;

		String nameblock="";
		int nameblocksize=0;

		conn.println("~FYUsers Idle Times:~N");
		conn.println("~FY+===============+~N");

		for (i=0; i<this.conn.length; ++i) {
			if (this.conn[i].isAuthenticated()==false) continue;
			nameblock = this.conn[i].getUserName();

			if (nameblock.length() > nameblocksize) {
				nameblocksize=nameblock.length();
			}
		}

		for (i=0; i<this.conn.length; ++i) {
			if (this.conn[i].isAuthenticated()==false) continue;

			nameblock = this.conn[i].getUserName();
			while (nameblock.length() < nameblocksize) {
				nameblock = nameblock.concat(" ");
			}

			conn.println(" "+nameblock+"    "+this.conn[i].getIdle()+" minutes.");
		}
		conn.println(" ");
	}

	/** displayColors() displays the supported Color Codes and an example of what the output looks like.
		@param conn UserConnection of the user making the call to .colors.*/
	void displayColors(UserConnection conn) {

		conn.println("~FGThis is the supported list of colors~N");
		conn.println("You can embed the color tags in a regular line of text to add color to it");

		conn.out.print("~FR ");
		conn.println("~FR- prints text in Red~N");
		conn.out.print("~FG ");
		conn.println("~FG- prints text in Green~N");
		conn.out.print("~FY ");
		conn.println("~FY- prints text in Yellow~N");
		conn.out.print("~FB ");
		conn.println("~FB- prints text in Blue~N");
		conn.out.print("~FP or ~FM");
		conn.println("~FP- prints text in Purple~N");
		conn.out.print("~FS ");
		conn.println("~FS- prints text in Sky Blue~N");
		conn.out.print("~FW ");
		conn.println("~FW- prints text in White~N");
		conn.out.print("~BR ");
		conn.println("~BR- prints background in Red~N");
		conn.out.print("~BG ");
		conn.println("~BG- prints background in Green~N");
		conn.out.print("~BY ");
		conn.println("~BY- prints background in Yellow~N");
		conn.out.print("~BB ");
		conn.println("~BB- prints background in Blue~N");
		conn.out.print("~BP or ~BM");
		conn.println("~BP- prints background in Purple~N");
		conn.out.print("~BS ");
		conn.println("~BS- prints background in Sky Blue~N");
		conn.out.print("~BW ");
		conn.println("~BW- prints background in White~N");
		conn.out.print("~UL ");
		conn.println("~UL- prints text in Underline~N");
		conn.out.print("~LI ");
		conn.println("~LI- prints text in Blinky~N");
		conn.out.print("~N ");
		conn.println("~N- prints text in Normal, or Removes Color~N");
		conn.println(" ");
	}

	/** say() simply displays text received from the user to the users in the area.  Text is
		logged into the area's conversation buffer.
		@param from integer index of the user calling the say() method.
		@param message String containing the message the user is saying. */
	synchronized void say(int from, String message) {
		int i;

		for (i=0; i<conn.length; ++i) {
			if (from==i) {
				try {
					// Don't print to the unauthenticated users.
					if (conn[i].isAuthenticated()) conn[i].println("You say: "+ message+"~N");
					continue;
				}
				catch (Exception e) {
					printSyslog("Say(): Exception Printing Back to User From:"+from+" i:"+i);
					e.printStackTrace();
					continue;
				}
			}
			else {
				try {
					// Don't print to the unauthenticated users.
					if (conn[i].isAuthenticated()==false) {
						continue;
					}
					else if (conn[i].getArea()!=conn[from].getArea()) {
						continue;
					}
					else {
						conn[i].println(""+conn[from].getUserName()+" says: "+message+"~N");
					}
				}
				catch (Exception e) {
					printSyslog("Say(): Exception Printing to User:"+i+" From:"+from);
					e.printStackTrace();
					continue;
				}
			}
		}

		// Add this say to the area's conversation buffer.
		area[conn[from].getArea()].addRev(""+conn[from].getUserName()+" says: "+message);
	}

	synchronized void shout(int from, String message) {
		int i;

		for (i=0; i<conn.length; ++i) {
			if (from==i) {
				try {
					// Don't print to the unauthenticated users.
					if (conn[i].isAuthenticated()) conn[i].println("! You shout: "+ message+"~N");
					continue;
				}
				catch (Exception e) {
					printSyslog("Shout(): Exception Printing Back to User From:"+from+" i:"+i);
					e.printStackTrace();
					continue;
				}
			}
			else {
				try {
					// Don't print to the unauthenticated users.
					if (conn[i].isAuthenticated()==false) {
						continue;
					}
					else {
						conn[i].println("! "+conn[from].getUserName()+" shouts: "+message+"~N");
					}
				}
				catch (Exception e) {
					printSyslog("Shout(): Exception Printing to User:"+i+" From:"+from);
					e.printStackTrace();
					continue;
				}
			}
		}
		// Add this shout to the area's conversation buffer.
		area[conn[from].getArea()].addRev(""+conn[from].getUserName()+" shouts: "+message);

	}

	/** emote() displays the username followed by a string of text with the intent of a third-person
		view of the conversation so emotion like "Jkb Smiles" can be printed. Emotes are
		printed to the area conversation buffer.
		@param from integer index of user performing the .emote
		@param message String containing the message */
	void emote(int from, String message) {
		int i;

		for (i=0; i<conn.length; ++i) {
			if (from==i) {
				try {
					if (conn[i].isAuthenticated()) conn[i].println(conn[i].getUserName()+" "+ message+"~N");
					continue;
				}
				catch (Exception e) {
					printSyslog("Emote(): Exception Printing Back to User From:"+from+" i:"+i);
					e.printStackTrace();
					continue;
				}
			}
			else {
				try {
					if (conn[i].isAuthenticated()==false) {
						//System.out.println("Say from "+conn[from].getUserName()+", To(unauth): "+conn[i].getUserName()+" "+message);
					}
					else if (conn[i].getArea()!=conn[from].getArea()) {
						//System.out.println("Say from "+conn[from].getUserName()+", To(!area): "+conn[i].getUserName()+" "+message);
						continue;
					}
					else {
						conn[i].println(conn[from].getUserName()+" "+message+"~N");
						//System.out.println("Say From "+conn[from].getUserName()+", To: "+conn[i].getUserName()+" "+message);
					}
				}
				catch (Exception e) {
					printSyslog("Emote(): Exception Printing to User:"+i+" From:"+from);
					e.printStackTrace();
					continue;
				}
			}
		}

		// Write this emote to the area's conversation buffer.
		area[conn[from].getArea()].addRev(""+conn[from].getUserName()+" "+message);
	}

	void semote(int from, String message) {
		int i;

		for (i=0; i<conn.length; ++i) {
			if (from==i) {
				try {
					if (conn[i].isAuthenticated()) conn[i].println("! " + conn[i].getUserName()+" "+ message+"~N");
					continue;
				}
				catch (Exception e) {
					printSyslog("Semote(): Exception Printing Back to User From:"+from+" i:"+i);
					e.printStackTrace();
					continue;
				}
			}
			else {
				try {
					if (conn[i].isAuthenticated()==false) {
						//System.out.println("Say from "+conn[from].getUserName()+", To(unauth): "+conn[i].getUserName()+" "+message);
					}
					else {
						conn[i].println("! " + conn[from].getUserName()+" "+message+"~N");
						//System.out.println("Say From "+conn[from].getUserName()+", To: "+conn[i].getUserName()+" "+message);
					}
				}
				catch (Exception e) {
					printSyslog("Semote(): Exception Printing to User:"+i+" From:"+from);
					e.printStackTrace();
					continue;
				}
			}
		}

		// Write this emote to the area's conversation buffer.
		area[conn[from].getArea()].addRev("! "+conn[from].getUserName()+" "+message);
	}
	
	/** quitUser() deletes a UserConnection from the UserConnection array.  In addition to deleting the
		UserConnection, it also resizes the array to attempt to conserve resources.
		@param index integer index of the user being deleted.*/
	synchronized void quitUser (int index) {
		int i=0;
		int j=0;
		int size = conn.length - 1;

		if (size<0) size=0;

		if (index >= conn.length) {
			printSyslog("Warning: Attempting to Delete User index: "+index+" but it is larger than the array!");
		}

		//printSyslog("Preparing to Quit conn["+index+"]:  conn[].length: "+conn.length);
		printSyslog(""+conn[index].getUserName()+" signed off."); // 3009

		// First, create a temporary array to hold the new contents of the UserConnection array.
		try {
			ctemp = new UserConnection[size];
		}
		catch (Exception e) {
			printSyslog("QuitUser() Exception in creating a new temporary UserConnection array: "+e.getMessage());

		}

		try {
			// If this is the last user online signing off, just reinitialize the whole thing.
			if (size==0) {
				conn=null;
				ctemp=null;				

				conn = new UserConnection[0];
				ctemp = new UserConnection[1];
			}

			// Otherwise, start copying the array from the old to the new, skipping the one to be deleted.
			else {
				for (i=0; i<=size; ++i,++j) {

					if (i==index) {
						
						try {
							//printSyslog("Attempting to force-close a socket...");
							conn[i].in.close();
							conn[i].out.close();
							conn[i].socket.close();
							//printSyslog("Force-closed the Socket!");
						}
						catch (Exception ex) {
							printSyslog("Error Force-closing the Socket: "+ex.getMessage());
						}
						
						conn[i]=null;
						++i;
					}
					if (i<=size) {
						ctemp[j] = conn[i];
						ctemp[j].setIndex(j);
					}
					else {
						//System.out.println("Last User in Array Quitting...");
					}
				}

				// Next, move the conn array to point at the memory and delete the ctemp.
				conn = ctemp;
				ctemp = null;
			}
		}
		catch (Exception ee) {
			printSyslog("QuitUser() Exception Caught while shuffling arrays around: "+ee.getMessage());

		}

		// print a note to the system log.
		// printSyslog("Successfully Deleted "+index+" from the array. Conn:"+conn.length);
	}


	/** shutdown() is called to close all UserConnections and the ServerSocket. The user sockets are
		manually closed.*/
	void shutdown () {
		int i=0;
		int num = 0;
		num = conn.length;

		while (i < num) {
			try {
				//System.out.println("i="+i+", num="+num);
				conn[i].println("\n~FRSystem Shutting Down, Disconnecting You!~N\n");
				conn[i].saveUserData();
				conn[i].quit();
				//conn[i].in.close();
				//(conn[i].socket).close();

			}
			catch (Exception e) {
				printSyslog("Shutdown() Exception caught while closing individual UserConnections' sockets.");
				e.printStackTrace();
			}
			i++;
		}

		printSyslog("System Shut Down",3);
		
		//try {
		//	(oneconn.socket).close();
		//	oneconn=null;
		//	s.close();
		//}
		//catch (Exception e) {
		//	printSyslog("Shutdown() Exception caught while closing the ServerSocket and Temporary socket");
		//	e.printStackTrace();
		//}

		try {
			Thread.sleep(2000);
		}
		catch (Exception e) {
			printSyslog("Shutdown(): Exception Sleeping to wait for UserConn's to close");
		}

		conn = null;
		System.exit(0);
	}

	/** getUserIndex() is used to take a name or partial name and return the index of
		the UserConnection in the array associated with the name.
		@return integer index of the UserConnection
		@param name String containing the name of the user. */
	int getUserIndex(String name) {
		name = name.toLowerCase();
		int i;
		// read through conn structure looking for exact matches.
		for (i=0; i<conn.length; ++i) {
			String compare = conn[i].getUserName();
			compare = compare.toLowerCase();
			//System.out.println("Comparing "+name+" to "+compare);
			if (name.equals(compare)) return i;
		}

		// read through conn structure looking for partial matches.
		for (i=0; i<conn.length; ++i) {
			String compare = conn[i].getUserName();
			compare = compare.toLowerCase();
			//System.out.println("Comparing "+name+" to "+compare);
			if (compare.startsWith(name)) return i;
		}

		// If no matches, return -1.
		return -1;
	}

	/** getAreaIndex is used to take a name or partial name and return the array index of the
		Area object associated with the name.
		@return integer index of the Area object.
		@param name String name containing the name of the area. */
	int getAreaIndex(String name) {
		name = name.toLowerCase();
		int i;

		// read through area structure looking for exact matches.
		for (i=0; i<area.length; ++i) {
			//System.out.println("Comparing "+name+" to "+area[i].getName());
			if (name.equals(area[i].getName().toLowerCase())) return i;
		}

		// read through area structure looking for partial matches.
		for (i=0; i<area.length; ++i) {
			//System.out.println("Comparing "+name+" to "+area[i].getName());
			if ((area[i].getName().toLowerCase()).startsWith(name)) return i;
		}
		return -1;
	}

	/** isUser() is used to check through the list of usernames to find out if the
		user actually exists or not.  This is used in the .smail and user creation
		methods.
		@param name String containing the name we are querying for.
		@return boolean containing the value of whether or not the user exists. */
	boolean isUser (String name) {
		name = name.toLowerCase();
		boolean retval=false;
		String sql = "SELECT username FROM user WHERE username=LCASE(\""+name+"\")";
		try {
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				retval= true;
			}
			else {
				retval= false;
			}
			rs.close();
		}
		catch (Exception e) {
			printSyslog("Error in isUser("+name+"): "+e.getMessage());
			e.printStackTrace();
			retval=false;
		}
		return retval;
	}

	/** getUserDBName() returns the correctly spelled name (in the event of a partial search) in the
		database so that when we create a link in the usermail table, we are correctly linking to
		the right key.
		@return String containing correctly spelled and capitalized name.
		@param name String containing the test value. */
	String getUserDBName (String name) {
		name = name.toLowerCase();
		String sql = "SELECT username FROM user WHERE username=LCASE(\""+name+"\")";
		try {
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				String uname = rs.getString(1);
				rs.close();
				return uname;
			}
			else {
				rs.close();
				return "";
			}
		}
		catch (Exception e) {
			printSyslog("Error in getUserDBName("+name+")");
			e.printStackTrace();
		}
		return "";
	}

	/** goArea() is called by the .go method, which changes the area a user is currently in.
		If no parameters are specified, the list of areas is displayed.
		@return boolean determining wheter or not an error has occurred
		@param index integer index of the user making the goArea() call.
		@param str String containing the area name. */
	public boolean goArea (int index, String str) {
		String sql="";
		if (str.equals("")) {
			conn[index].println("~FR Usage: .go areaname~N");
			try {
				sql="SELECT name FROM area ORDER BY name";
				ResultSet rs = stmt.executeQuery(sql);

				conn[index].println("~FY  Areas available:~N");
				while (rs.next()) {
					conn[index].out.println("   "+rs.getString(1));
				}
				rs.close();
				conn[index].println(" ");
			}
			catch (Exception e) {
				printSyslog("goArea() Exception: SQL: "+sql);
				e.printStackTrace();
			}
			return false;
		}

		// Retrieve the Index of the Destination Area
		int room = getAreaIndex(str);

		if (room==-1) {
			conn[index].println("~FYNo Such Area \""+str+"\" Exists~N");
		}
		else {

			// Change the area and display the room information.
			conn[index].setArea(room);
			area[room].look(conn[index]);
		}

		return false;
	}

	public void checkNewMail (UserConnection conn) {
		try {
			connectDB();
			String sql = "SELECT mail.id,sender,mdate,mtime,message,unread FROM mail,usermail WHERE mail.id=usermail.mailid AND usermail.username=\""+conn.getUserName()+"\" ORDER BY mail.id";

			ResultSet rs = stmt.executeQuery(sql);
			int number = 0;
			int newmessages =0;

			while (rs.next()) {
				number++;
				try {
					// Pull the Sender, Date, Time and Message from the Database
					int id = rs.getInt(1);
					String sender = rs.getString(2);
					String mdate = rs.getString(3);
					String mtime = rs.getString(4);
					String message = rs.getString(5);
					int unread = rs.getInt(6);

					if (unread==1) newmessages++;
				}
				catch (Exception ex) {
					printSyslog("Exception Caught in Rmail");
					ex.printStackTrace();
				}
			}
			rs.close();
			if (newmessages>0) {
				conn.println("~FYYou Have "+newmessages+" New Mail Messages out of "+number+" total!~N");			
			}
			else { 
				conn.println("~FPYou have "+number+" old mail messages.~N");
			}
		}
		catch (Exception e) {
			printSyslog("Caught Exception in CheckNewMail()");
			e.printStackTrace();
		}
	
	}
	
	
	/** rmail() is used to read all of a user's offline private messages, or smail.
		@param conn UserConnection of the user making the call to the .rmail command. */
	public void rmail (UserConnection conn) {
		CaesarCipher cc = new CaesarCipher();
		String codedmessage="";
		String message="";
		String sql ="";
		try {
			conn.println("~FYHere is your mail:~N\n");

			connectDB();
			
			sql = "UPDATE mail,usermail SET mail.unread=0 WHERE mail.id=usermail.mailid AND usermail.username=\""+conn.getUserName()+"\"";
			stmt.executeUpdate(sql);
			
			sql = "SELECT mail.id,sender,mdate,mtime,message FROM mail,usermail WHERE mail.id=usermail.mailid AND usermail.username=\""+conn.getUserName()+"\" ORDER BY mail.id";

			ResultSet rs = stmt.executeQuery(sql);
			int number = 0;

			while (rs.next()) {
				number++;
				try {
					// Pull the Sender, Date, Time and Message from the Database
					int id = rs.getInt(1);
					String sender = rs.getString(2);
					String mdate = rs.getString(3);
					String mtime = rs.getString(4);
					codedmessage = rs.getString(5);

					codedmessage = unescapeUserInput(codedmessage);
					
					String rawmessage = cc.decodeMessage(codedmessage);
					message = rawmessage.replaceAll("\\\\0","'");					
					// Display them to the User.
					conn.println(" ~FB"+mdate+": "+mtime+": ~FRFrom "+sender+":~N  "+message);
				}
				catch (Exception ex) {
					printSyslog("Exception Caught in Rmail");
					ex.printStackTrace();
				}
			}
			rs.close();
			conn.println("\n~FYTotal of "+number+" Mail Messages~N");
			conn.println(" ");
		}
		catch (Exception e) {
			printSyslog("Caught Exception in Rmail()");
			e.printStackTrace();
		}
	}

	/** smail() is used to create offline private messages.  Using the time and date
		stamps, smail() provides information on when the message was sent, the actual
		message and the user sending it.  smail() creates the row in the mail table
		and the link record in the usermail table.
		@return boolean designating whether or not an error occurred
		@param conn UserConnection of user sending the message
		@param message String containing both the intended recipient and the message. */
	public boolean smail (UserConnection conn, String message) {
		CaesarCipher cc = new CaesarCipher();
		String encodedmessage = "";
		StringTokenizer st = new StringTokenizer(message);
		String to;
		int toindex=-1;
		try {
			// First, pull off the recipient's name.
			if (st.hasMoreTokens()) to = st.nextToken();
			else {
				conn.println("~FR Usage: .smail recipient message~N");
				return true;
			}

			// Check to see if that user is a real existing user.
			if (isUser(to)==false) {
				conn.println("~FR\""+to+"\" is not an existing username.~N");
				return true;
			}
			else {
				to = getUserDBName(to);
				toindex = getUserIndex(to);
			}

			// Pull off the first token in the message, and concatenate the rest onto it.
			if (st.hasMoreTokens()) message = st.nextToken();
			else {
				conn.println("~FR Usage: .smail recipient message~N");
				return true;
			}

			while (st.hasMoreTokens()) {
				message = message.concat(" ");
				message = message.concat(st.nextToken());
			}


			connectDB();
			ResultSet rs;
			int mailid;
			String sql="";
			int sqlrows=0;

			// Since this message goes into the database, escape it.
			message = escapeUserInput(message);

			encodedmessage = cc.encodeMessage(message);
			encodedmessage = escapeUserInput(encodedmessage);
			
			//conn.println("Your Encoded Message: "+encodedmessage);
			
			// Write the message to the mail table.
			try {
				sql = "INSERT INTO mail (sender,mdate,mtime,message) VALUES (\""+conn.getUserName()+"\",\""+datestamp+"\", \""+timestamp+"\",\""+encodedmessage+"\")";
				sqlrows = stmt.executeUpdate(sql);

				// Removed due to incompatibilities with the Windows Java virtual machines
				//rs = stmt.getGeneratedKeys();
				//rs.next();
				//mailid = rs.getInt(1);

				//System.out.println("Executed: "+sql+"; "+sqlrows+" Rows Effected.");

				sql = "SELECT id FROM mail WHERE sender='"+conn.getUserName()+"' AND mdate='"+datestamp+"' AND mtime='"+timestamp+"' AND message='"+encodedmessage+"'";
				rs = stmt.executeQuery(sql);
				rs.next();
				mailid = rs.getInt(1);
				rs.close();


			}
			catch (Exception e) {
				conn.println("Error Writing the Mail Message, please try again");
				printSyslog("Write(): Exception Inserting Mail into Mail Table\n\rSQL: "+sql);
				e.printStackTrace();
				return true;
			}

			// Write the User to Message link in the UserMail table.
			try {
				sql = "INSERT INTO usermail (username, mailid) VALUES (\""+to+"\","+mailid+")";
				sqlrows = stmt.executeUpdate(sql);
				//System.out.println("Executed: "+sql+"; "+sqlrows+" Rows Effected.");
			}
			catch (Exception e) {
				conn.println("Error Writing the Mail Message, please try again");
				printSyslog("Write(): Exception Inserting Mail and User into UserMail");
				e.printStackTrace();
				return true;
			}
		}
		catch (Exception e) {
			printSyslog("Caught Exception in Smail():"+e);
			e.printStackTrace();
			return true;
		}

		// Provide a little feedback.
		if (toindex!=-1) this.conn[toindex].println("You Receive a Mail Message");
		else if (toindex==conn.getIndex()) conn.println("You send yourself a mail message.");
		conn.println("Your message has been sent.");

		printSyslog(""+conn.getUserName()+" smailed "+to);
		
		return false;
	}

	/** dmail() provides a means of deleting messages from a user's Offline Private Message "box".
		If a user specifies a number, that number of messages is deleted.  If a user specifies the word
		all, all of the messages are deleted.
		@return boolean indicating whether or not an error has occurred.
		@param conn UserConnection of user deleting messages
		@param in String containing number of messages to delete or word all. */
	public boolean dmail (UserConnection conn, String in) {
		if (in.equals("")) {
			conn.println("~FR Usage:  .dmail all/number of msgs~N");
			return true;
		}

		int todelete = 0;
		try {

			// First, connect to the database and get a list of all the user mail.
			connectDB();
			String sql = "SELECT mail.id FROM mail, usermail WHERE usermail.username=\""+conn.getUserName()+"\" AND usermail.mailid=mail.id ORDER BY mail.id";
			ResultSet rs = stmt.executeQuery(sql);
			int number = 0;

			if (in.equals("all")) {
				todelete = -1;
				conn.println("Erasing all of your Mail...");
			}
			else {
				try {
					todelete = new Integer(in).intValue();
				}
				catch (Exception e) {
					return true;
				}
				if (todelete <= 0) return true;
				conn.println("Erasing "+todelete+" of your Mail Messages...");
			}


			while (rs.next()) {
				number++;
				if (todelete != -1) {
					if (number > todelete) {
						--number;
						break;
					}
				}

				try {
					// Next, continually read the message index of each message.
					int id = rs.getInt(1);
					int sqlrows = 0;

					// Form the SQL statement to delete the message and execute it.
					String sql2 = "DELETE FROM mail WHERE mail.id="+id;
					sqlrows = stmt.executeUpdate(sql2);
					//System.out.println("Executed: "+sql2+"; "+sqlrows+" Rows Effected.");

					// Form the SQL statement to delete the link from usermail and execute it.
					String sql3 = "DELETE FROM usermail WHERE mailid="+id;
					sqlrows = stmt.executeUpdate(sql3);
					//System.out.println("Executed: "+sql3+"; "+sqlrows+" Rows Effected.");
				}
				catch (Exception ex) {
					printSyslog("Exception Caught in Dmail()");
					ex.printStackTrace();
				}
			}
			rs.close();
			conn.println("Total of "+number+" Mail Messages Deleted.");
		}
		catch (Exception e) {
			printSyslog("Caught Exception in Dmail()");
			e.printStackTrace();
		}
		return false;
	}

	/** setUserProperty() is used by a user to set different attributes of the UserConnection.
		Some of the attributes are description, gender, password, age, email, homepage, photo and profile.
		@param conn UserConnection of the user calling .set command
		@param value String containing the attribute and the new value
		@return boolean indicating whether or not the .set was successful. */
	public boolean setUserProperty(UserConnection conn, String value) {

		// If no parameters, print the usage information.
		if (value.equals("")) {
			conn.println("~FR Usage:  .set property value~N");
			conn.println("   Valid Properties:");
			conn.println("   desc - User Description");
			conn.println("   gender - (male/female/unknown)");
			conn.println("   password - oldpass newpass");
			conn.println("   age - Number of years old");
			conn.println("   email - Email Address");
			conn.println("   homepage - URL of Homepage");
			conn.println("   photo - URL of Photo");
			conn.println("   profile - User Profile");
			conn.println(" ");
			return false;
		}

		String value2 = "";
		String property="";

		// Separate the property from the values.
		try {
			StringTokenizer st = new StringTokenizer(value);
			property = st.nextToken();
			while (st.hasMoreTokens()) {
				value2 = value2.concat(" ");
				value2 = value2.concat(st.nextToken());
			}
			value = value2;
		}
		catch (Exception e) {
			conn.println("~FRUnable to Set That!~N");
			return false;
		}

		// Handle the desc property.
		if (property.equals("desc")) {
			conn.setDesc(value.trim());
			conn.println("Your description is now: "+value);
			return true;
		}

		// Handle the gender property.
		if (property.equals("gender")) {
			String gender="";
			value = (value.toLowerCase()).trim();
			if (new String("male").startsWith(value)) gender="Male";
			else if (new String("female").startsWith(value)) gender="Female";
			else gender="Unknown";

			conn.setGender(gender);
			conn.println("You change your gender to: "+gender);
			return true;
		}

		// Handle the age property.
		if (property.equals("age")) {
			int oldage = conn.getAge();
			int newage = -1;
			try {
				newage =new Integer(value.trim()).intValue();
			}
			catch (Exception e) {
				printSyslog("Error Setting Age to: "+value+" by "+conn.getUserName());
				conn.setAge(oldage);
				conn.println("~FRIllegal Age, Try again!~N");
				return false;
			}
			conn.setAge(newage);
			conn.println("You set your age to: "+newage);

			return true;
		}

		// Handle the password property.
		if (property.equals("password")) {
			String oldpass="";
			String newpass="";
			try {
				StringTokenizer st = new StringTokenizer(value);
				oldpass = st.nextToken();
				newpass = st.nextToken();
			}
			catch (Exception e) {
				conn.println("~FRUsage: .set password oldpassword newpassword~N");
				return false;
			}

			if (oldpass.equals(conn.getPassword())) {
				conn.setPassword(newpass);
				conn.println("Your password has been changed.");
				return true;
			}
			else {
				conn.println("Your old password did not match.  Your password has not been changed.");
				return false;
			}
		}

		// Handle the email property.
		if (property.equals("email")) {
			conn.setEmail(value.trim());
			conn.println("Your email address is now set to: "+value);
			return true;
		}

		// Handle the photo property.
		if (property.equals("photo")) {
			conn.setPhoto(value.trim());
			conn.println("Your photo URL is now set to: "+value);
			return true;
		}

		// Handling for the homepage attribute.
		if (property.equals("homepage")) {
			conn.setHomepage(value.trim());
			conn.println("Your homepage URL is now set to: "+value);
			return true;
		}

		// Handling for the profile attribute.
		if (property.equals("profile")) {
			conn.setProfile(value.trim());
			conn.println("Your profile is now set to: "+value);
			return true;
		}

		// If we haven't found a way to handle the property, print an error.
		conn.println("~FRYou cannot set "+property+"!~n");
		return false;
	}

	public String unescapeUserInput (String input) {
		int i=0;
		int length=0;
		
		StringBuffer in = new StringBuffer(input);
		
		for (i=0; i<in.length(); ++i) {
			
			if (in.charAt(i) == '\\') {
				try {
					if (in.charAt(i+1) == '"') {
						in.deleteCharAt(i);
						i--;
						continue;
					}
					if (in.charAt(i+1) == '\'') {
						in.deleteCharAt(i);
						i--;
						continue;
					}
				}
				catch (Exception e) {
					continue;
				}
			}
			
		}
				
		return in.toString();
	}

	/** escapeUserInput() replaces characters that would confuse or potentially
	create security vulnerabilities in the database access portions of the program
	with their escaped equivalents.
	@return String containing the escaped string.
	@param input String containing the unescaped input. */

	public String escapeUserInput (String input) {
		int i=0;
		int length=0;
		StringBuffer in = new StringBuffer(input);
		length = in.length();
		for (i=0; i<length; ++i) {

			// replace double quotes.
			if (in.charAt(i)=='"') {
				if (i==0) { in.insert(0,'\\'); length = in.length(); ++i; continue;}
				if (in.charAt(i-1)=='\\') continue;
				else {in.insert(i, '\\'); length = in.length(); ++i; continue; }
			}

			// replace single quotes.
			if (in.charAt(i)=='\'') {
				if (i==0) { in.insert(0,'\\'); length = in.length(); ++i; continue;}
				if (in.charAt(i-1)=='\\') continue;
				else {in.insert(i, '\\'); length = in.length(); ++i; continue; }
			}

			// replace escape characters.
			if (in.charAt(i)=='\\') {
				if (i==0) { in.insert(0,'\\'); length = in.length(); ++i; continue;}
				if (in.charAt(i-1)=='\\') continue;
				else {in.insert(i, '\\'); length = in.length(); ++i; continue; }
			}
		}
		return in.toString();
	}

	
	
	/** setAfk() marks a user as being away from keyboard.
		@param conn UserConnection of user going away.
		@param afkmsg String containing the reason why user is going afk. */
	void setAfk(UserConnection conn, String afkmsg) {
		if (afkmsg.equals("")) afkmsg = "ZZz... Zz... z...";

		conn.println("You go AFK: "+afkmsg);
		area[conn.getArea()].printArea(conn, conn.getUserName()+" goes AFK: "+afkmsg, false);

		conn.setAfk(true);
		conn.setAfkMsg(afkmsg);
	}

	/** remove() is a way for a high-leveled user to manually disconnect users from the Java Talker.
		@return boolean indicating whether or not the .remove was successful
		@param conn UserConnection of the user performing the .remove.
		@param other String containing the name of the user to be .removed. */
	boolean remove (UserConnection conn, String other) {
		if (other.equals("")) {
			conn.println("~FR Usage:  .remove username~N");
			return false;
		}


		int i = getUserIndex(other);
		if (i!=-1) {
			// Can't remove a higher level user.
			if (conn.getLevel() < this.conn[i].getLevel()) {
				conn.println("~FR"+other+" is at a higher level than you. Unable to remove.~N");
				return false;
			}

			try {
				this.conn[i].socket.close();
			}
			catch (Exception e) {
				conn.println("~FRFailed to Remove "+other+"~N");
				return false;
			}
			printLoginLogoutMessage("LEAVING US:",i);
			printSyslog(""+conn.getUserName()+" removed "+other);
			
			try {
				this.conn[i].in.close();
			}
			catch (Exception e) {
				printSyslog("Failed to close BufferedReader for "+this.conn[i].getUserName());
			}
			return true;
		}
		else {
			conn.println("That user is not signed on.");
			return false;
		}
	}


	/** deleteUser() deletes a user account from the database.
		@return boolean indicating whether or not the .delete was successful
		@param conn UserConnection of the user calling the .delete command.
		@param other String containing name of user to delete. */
	boolean deleteUser (UserConnection conn, String other) {
		if (other.equals("")) {
			conn.println("~FR Usage:  .delete username~N");
			return false;
		}

		// First, find out if the user is signed on or not and remove them...
		int i = getUserIndex(other);
		if (i!=-1) {
			if (conn.getLevel() < this.conn[i].getLevel()) {
				conn.println("~FR"+other+" is at a higher than you. Unable to remove.~N");
				return false;
			}

			try {
				this.conn[i].socket.close();
			}
			catch (Exception e) {
				conn.println("~FRFailed to Remove "+other+"~N");
				return false;
			}
			printLoginLogoutMessage("LEAVING US:", i);

		}

		// Otherwise, check the user file and see what level the user is at.
		else {
				UserConnection delete = new UserConnection(this);
				delete.setUserName(getUserDBName(other));
				delete.loadUserData();
				delete.loadUserData();
				if (conn.getLevel() < delete.getLevel()) {
					conn.println("~FR"+other+" is at a higher than you. Unable to remove.~N");
					return false;
				}
				delete = null;
		}

		// If at any point prior, if there is not enough reason to delete
		// the user, the flow stops.
		String deletename = getUserDBName(other);

		String sql="";
		int sqlrows=0;

		try {

			// Delete all references to the user in the database.

			sql = "SELECT id FROM usermail WHERE username='"+deletename+"'";
			ResultSet rs = stmt.executeQuery(sql);
			//System.out.println("deleteUser(): Executed: "+sql);

			while (rs.next()) {

				sql = "DELETE FROM mail WHERE id="+rs.getInt(1);
				sqlrows = stmt.executeUpdate(sql);
				//System.out.println("deleteUser(): Executed: "+sql+"; "+sqlrows+" Rows Effected.");

			}
			rs.close();
			sql = "DELETE FROM usermail WHERE username='"+deletename+"'";
			sqlrows = stmt.executeUpdate(sql);
			//System.out.println("deleteUser(): Executed: "+sql+"; "+sqlrows+" Rows Effected.");

			sql = "DELETE FROM user WHERE username='"+deletename+"'";
			sqlrows = stmt.executeUpdate(sql);
			//System.out.println("deleteUser(): Executed: "+sql+"; "+sqlrows+" Rows Effected.");

		}
		catch (Exception e) {
			printSyslog("deleteUser(): Exception, probably SQL:"+sql);
			e.printStackTrace();
		}

		// Report the deletion.
		conn.println("~FY"+deletename+" has been deleted.~N");
		printSyslog(conn.getUserName()+" deleted "+deletename);
		return true;
	}


	/** modspeech() simply displays text received from the user to the users in the area.  Text is
	logged into the area's conversation buffer.
	@param user UserConnection object of the user calling the sing() method.
	@param message String containing the message the user is saying. */
	void modspeech(UserConnection user, String message, String style) {
		int i;
		int userindex = user.getIndex();
		int stylenum=-1;
		String output = null;
		String firsttoken = null;
		String rest = "";
		StringTokenizer st = null;
		
		if (style.equals(".sing")) stylenum = 0;
		if (style.equals(".think")) stylenum = 1;
		if (style.equals(".echo")) stylenum = 2;
		if (style.equals(".to")) stylenum = 3;

		if ((stylenum==2) || (stylenum==3)) {
			try {
				st = new StringTokenizer(message);
				if (st.countTokens() > 0) {
					firsttoken = st.nextToken();
					if (stylenum==3) {
						rest = st.nextToken();
						while (st.hasMoreTokens()) {
							rest = rest + " " + st.nextToken();
						}
					}
				}
				else {
					message = "";
				}
			}
			catch (Exception e) {
				message = "";
			}
			
		}

		if (stylenum==3) {
			if (rest.equals("")) {
				message = "";
			}
		}
								
		if (message.equals("")) {
			if (stylenum==0) user.println("~FRUsage: .sing song~N");
			if (stylenum==1) user.println("~FRUsage: .think thoughts~N");
			if (stylenum==2) user.println("~FRUsage: .echo echoable~N");
			if (stylenum==3) user.println("~FRUsage: .to username messagetouser~N");
		}
		else {
			for (i=0; i<conn.length; ++i) {
				try {
					// Don't print to the unauthenticated users.
					if (conn[i].isAuthenticated()==false) {
						continue;
					}
					else if (conn[i].getArea()!=conn[userindex].getArea()) {
						continue;
					}
					else {
						if (stylenum==0) {
							output = ""+conn[userindex].getUserName()+" sings: o/~ ~N"+message+"~FP o/~";
							conn[i].println("~FP"+output+"~N");
						}
						if (stylenum==1) {
							output = ""+conn[userindex].getUserName()+" thinks . o O ( ~N"+message+"~FP )";
							conn[i].println("~FP"+output+"~N");
						}
						if (stylenum==2) {
							if (getUserIndex(firsttoken)!=-1) {
								output= "["+user.getUserName()+"]: "+message;
							}
							else {
								output = ""+message;
							}
							conn[i].println(output);
						}
						if (stylenum==3) {
							if (getUserIndex(firsttoken)!=-1) firsttoken = conn[getUserIndex(firsttoken)].getUserName();
							output = ""+conn[userindex].getUserName()+" says [to "+firsttoken+"]: "+rest;
							if (i==userindex) conn[i].println("You say [to "+firsttoken+"]: "+rest);
							else {
								conn[i].println(output);
							}
						}
					}
				}
				catch (Exception e) {
					printSyslog("Modspeech(): Exception Printing '"+style+"' to User:"+i+" From:"+userindex+"="+e);
					continue;
				}
				
			}

			// Add this say to the area's conversation buffer.
			area[conn[userindex].getArea()].addRev(""+output+"~N");
		}
	}

	
	
	String[] greetmap = {
		"0,1,1,1,0 1,0,0,0,1 1,1,1,1,1 1,0,0,0,1 1,0,0,0,1",
		"1,1,1,1,0 1,0,0,0,1 1,1,1,1,0 1,0,0,0,1 1,1,1,1,0",
		"0,1,1,1,1 1,0,0,0,0 1,0,0,0,0 1,0,0,0,0 0,1,1,1,1",
		"1,1,1,1,0 1,0,0,0,1 1,0,0,0,1 1,0,0,0,1 1,1,1,1,0",
		"1,1,1,1,1 1,0,0,0,0 1,1,1,1,0 1,0,0,0,0 1,1,1,1,1",
		"1,1,1,1,1 1,0,0,0,0 1,1,1,1,0 1,0,0,0,0 1,0,0,0,0",
		"0,1,1,1,0 1,0,0,0,0 1,0,1,1,0 1,0,0,0,1 0,1,1,1,0",
		"1,0,0,0,1 1,0,0,0,1 1,1,1,1,1 1,0,0,0,1 1,0,0,0,1",
		"0,1,1,1,0 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0 0,1,1,1,0",
		"0,0,0,0,1 0,0,0,0,1 0,0,0,0,1 1,0,0,0,1 0,1,1,1,0",
		"1,0,0,0,1 1,0,0,1,0 1,0,1,0,0 1,0,0,1,0 1,0,0,0,1",
		"1,0,0,0,0 1,0,0,0,0 1,0,0,0,0 1,0,0,0,0 1,1,1,1,1",
		"1,0,0,0,1 1,1,0,1,1 1,0,1,0,1 1,0,0,0,1 1,0,0,0,1",
		"1,0,0,0,1 1,1,0,0,1 1,0,1,0,1 1,0,0,1,1 1,0,0,0,1",
		"0,1,1,1,0 1,0,0,0,1 1,0,0,0,1 1,0,0,0,1 0,1,1,1,0",
		"1,1,1,1,0 1,0,0,0,1 1,1,1,1,0 1,0,0,0,0 1,0,0,0,0",
		"0,1,1,1,0 1,0,0,0,1 1,0,1,0,1 1,0,0,1,1 0,1,1,1,0",
		"1,1,1,1,0 1,0,0,0,1 1,1,1,1,0 1,0,0,1,0 1,0,0,0,1",
		"0,1,1,1,1 1,0,0,0,0 0,1,1,1,0 0,0,0,0,1 1,1,1,1,0",
		"1,1,1,1,1 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0",
		"1,0,0,0,1 1,0,0,0,1 1,0,0,0,1 1,0,0,0,1 1,1,1,1,1",
		"1,0,0,0,1 1,0,0,0,1 0,1,0,1,0 0,1,0,1,0 0,0,1,0,0",
		"1,0,0,0,1 1,0,0,0,1 1,0,1,0,1 1,1,0,1,1 1,0,0,0,1",
		"1,0,0,0,1 0,1,0,1,0 0,0,1,0,0 0,1,0,1,0 1,0,0,0,1",
		"1,0,0,0,1 0,1,0,1,0 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0",
		"1,1,1,1,1 0,0,0,1,0 0,0,1,0,0 0,1,0,0,0 1,1,1,1,1",
		"0,1,1,1,0 1,0,0,0,1 1,0,0,0,1 1,0,0,0,1 0,1,1,1,0", // Locu's 0
		"0,0,1,0,0 0,1,1,0,0 0,0,1,0,0 0,0,1,0,0 0,1,1,1,0", // 1
		"0,1,1,1,0 1,0,0,0,1 0,0,0,1,0 0,1,0,0,0 1,1,1,1,1", // Locu's 2
		"0,1,1,1,0 0,0,0,0,1 0,0,1,1,0 0,0,0,0,1 0,1,1,1,0", // Locu's 3
		"0,0,1,1,0 0,1,0,1,0 1,1,1,1,1 0,0,0,1,0 0,0,0,1,0", // Locu's 4
		"1,1,1,1,1 1,0,0,0,0 1,1,1,1,0 0,0,0,0,1 1,1,1,1,0", // Locu's 5
		"0,1,1,1,0 1,0,0,0,0 1,1,1,1,0 1,0,0,0,1 0,1,1,1,0", // Locu's 6
		"1,1,1,1,1 0,0,0,0,1 0,0,0,1,0 0,0,1,0,0 0,1,0,0,0", // Locu's 7
		"0,1,1,1,0 1,0,0,0,1 0,1,1,1,0 1,0,0,0,1 0,1,1,1,0", // Locu's 8
		"0,1,1,1,0 1,0,0,0,1 0,1,1,1,0 0,0,1,0,0 0,1,0,0,0", // Locu's 9
		"0,1,1,1,0 0,1,1,1,0 0,0,1,0,0 0,0,0,0,0 0,0,1,0,0", // !
		"0,1,1,1,0 0,0,0,0,1 0,0,1,0,0 0,0,0,0,0 0,0,1,0,0", // ?
		"0,0,1,0,0 0,0,0,1,0 0,0,0,1,0 0,0,0,1,0 0,0,1,0,0", // )
		"0,0,1,0,0 0,1,0,0,0 0,1,0,0,0 0,1,0,0,0 0,0,1,0,0", // (
		"0,0,0,0,0 0,0,1,0,0 0,0,0,0,0 0,0,1,0,0 0,0,0,0,0", // :
		"0,0,0,0,0 0,0,1,0,0 0,0,0,0,0 0,0,1,0,0 0,1,0,0,0", // ;
		"0,0,0,0,0 0,1,1,1,0 0,0,0,0,0 0,1,1,1,0 0,0,0,0,0", // =
		"0,1,1,0,0 0,0,1,0,0 0,0,0,0,0 0,0,0,0,0 0,0,0,0,0", // '
		"0,0,0,0,0 0,0,0,0,0 1,1,1,1,1 0,0,0,0,0 0,0,0,0,0", // -
        	"0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 0,0,0,0,0", // space
        	"0,0,1,0,0 0,0,1,0,0 1,1,1,1,1 0,0,1,0,0 0,0,1,0,0", // +
		"1,1,1,1,1 1,0,1,0,0 1,1,1,1,1 0,0,1,0,1 1,1,1,1,1", // $
		"0,0,1,0,0 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0 0,0,1,0,0", // |
		"0,1,0,0,0 1,0,1,0,1 0,0,0,1,0 0,0,0,0,0 0,0,0,0,0", // ~
		"0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 0,0,1,1,0 0,0,1,1,0", // .
		"0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 0,0,1,1,0 0,0,0,1,0", // ,
		"0,1,1,1,1 1,0,0,0,1 1,0,1,1,1 1,0,0,0,0 0,1,1,1,0", // @
		"0,1,0,1,0 1,1,1,1,1 0,1,0,1,0 1,1,1,1,1 0,1,0,1,0", // #
		"0,1,0,0,1 0,0,0,1,0 0,0,1,0,0 0,1,0,0,0,1,0,0,1,0", // %
		"0,0,1,0,0 0,1,0,1,0 1,0,0,0,1 0,0,0,0,0 0,0,0,0,0", // ^
		"0,0,1,0,0 0,1,0,1,0 0,0,1,0,0 0,1,0,1,1 0,0,1,0,1", // &
		"1,0,1,0,1 0,1,1,1,0 1,1,1,1,1 0,1,1,1,0 1,0,1,0,1", // *
		"0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 0,0,0,0,0 1,1,1,1,1", // _
		"0,1,1,1,0 0,1,0,0,0 0,1,0,0,0 0,1,0,0,0 0,1,1,1,0", // [
		"0,1,1,1,0 0,0,0,1,0 0,0,0,1,0 0,0,0,1,0 0,1,1,1,0", // ]
		"0,0,1,0,0 0,1,0,0,0 1,0,0,0,0 0,1,0,0,0 0,0,1,0,0", // <
		"0,0,1,0,0 0,0,0,1,0 0,0,0,0,1 0,0,0,1,0 0,0,1,0,0", // >
		"0,0,0,0,1 0,0,0,1,0 0,0,1,0,0 0,1,0,0,0 1,0,0,0,0", // /
		"0,1,1,1,0 0,1,0,0,0 1,0,0,0,0 0,1,0,0,0 0,1,1,1,0", // /* {  */
		"0,1,1,1,0 0,0,0,1,0 0,0,0,0,1 0,0,0,1,0 0,1,1,1,0", // /* } */
		"1,0,0,0,0 0,1,0,0,0 0,0,1,0,0 0,0,0,1,0 0,0,0,0,1"  // /* \ */
	};

	
	public void greet (UserConnection user, String input) {
		int slen,lc,i,j;
		int randint;
		
		input = input.toLowerCase();
		
		slen = input.length();
		if (slen>12) slen=12;
		
		if (slen==0) {
			user.println("~FRWhat would you like to Greet?~N\n\r");
			return;
		}

		int lineindex=0;
		String[] line = new String[5];
		for (i=0;i<5;++i) {
			line[i] = "";
		}
		
		area[user.getArea()].printArea(user, "\n\r~FY"+user.getUserName()+" yells:~N\n\r", false);
		user.println("\n\r~FYYou yell:~N\n\r");
			
		for (i=0;i<slen;++i) {
			
			lc = (int) ((char) (input.charAt(i)) - 'a');
			if (input.charAt(i) == '0') lc = 26;
			if (input.charAt(i) == '1') lc = 27;
			if (input.charAt(i) == '2') lc = 28;
			if (input.charAt(i) == '3') lc = 29;
			if (input.charAt(i) == '4') lc = 30;
			if (input.charAt(i) == '5') lc = 31;
			if (input.charAt(i) == '6') lc = 32;
			if (input.charAt(i) == '7') lc = 33;
			if (input.charAt(i) == '8') lc = 34;
			if (input.charAt(i) == '9') lc = 35;
			if (input.charAt(i) == '!') lc = 36;
			if (input.charAt(i) == '?') lc = 37;
			if (input.charAt(i) == ')') lc = 38;
			if (input.charAt(i) == '(') lc = 39;
			if (input.charAt(i) == ':') lc = 40;
			if (input.charAt(i) == ';') lc = 41;
			if (input.charAt(i) == '=') lc = 42;
			if (input.charAt(i) == '\'') lc = 43;
			if (input.charAt(i) == '-') lc = 44;
			if (input.charAt(i) == ' ') lc = 45;
			
			if (input.charAt(i) == '+') lc = 46;
			if (input.charAt(i) == '$') lc = 47;
			if (input.charAt(i) == '|') lc = 48;
			if (input.charAt(i) == '~') lc = 49;
			if (input.charAt(i) == '.') lc = 50;
			if (input.charAt(i) == ',') lc = 51;
			if (input.charAt(i) == '@') lc = 52;
			if (input.charAt(i) == '#') lc = 53;
			if (input.charAt(i) == '%') lc = 54;
			if (input.charAt(i) == '^') lc = 55;
			if (input.charAt(i) == '&') lc = 56;
			if (input.charAt(i) == '*') lc = 57;
			if (input.charAt(i) == '_') lc = 58;
			if (input.charAt(i) == '[') lc = 59;
			if (input.charAt(i) == ']') lc = 60;
			if (input.charAt(i) == '<') lc = 61;
			if (input.charAt(i) == '>') lc = 62;
	                if (input.charAt(i) == '/') lc = 63;
                	if (input.charAt(i) == '{') lc = 64;
	               	if (input.charAt(i) == '}') lc = 65;
			if (input.charAt(i) == '\\') lc = 66;
				
			if (lc >= 0 && lc < 67) {
				StringTokenizer st1 = new StringTokenizer(greetmap[lc]," ");
				
				lineindex = 0;
				while (st1.hasMoreTokens()) {
					String charline = st1.nextToken();
					StringTokenizer st2 = new StringTokenizer(charline,",");
					while (st2.hasMoreTokens()) {
						String character = st2.nextToken();
					
						if (character.equals("1")) {
							String add="";
							randint = (int) (Math.random() * 5.0);
							if (randint == 0) add = "~FR#~N";
							else if (randint == 1) add = "~FG#~N";
							else if (randint == 2) add = "~FY#~N";
							else if (randint == 3) add = "~FP#~N";
							else if (randint == 4) add = "~FS#~N";
							else add = "~FB#~N";
						
							line[lineindex] = line[lineindex].concat(add);
						}
						else {
							line[lineindex] = line[lineindex].concat(" ");
						}
						
					}					
					line[lineindex] = line[lineindex].concat("  ");
					
					lineindex++;
				}
			}
		}
		
		for (i=0;i<5;++i) {
			//System.out.println(""+line[i]);
			area[user.getArea()].printArea(user, line[i], true);
		}
		area[user.getArea()].printArea(user, "\n\r", true);
	}

	
}  // End of Talker Class
//***************************************************************************

/** Area object
	@author Jeffrey K. Brown
	<p>Area is the object that represents a virtual area or room.  Each Area
	has a message board, a topic and a room description.  Additionally, each
	Area has a conversation buffer.  Users in a virtual area may carry on
	public online conversation between each other.  Offline public communication
	takes place on a room to room basis as well.</p> */
class Area {
	/** t is a link back to the Talker object so we can reference methods in Talker. */
	static Talker t;

	/** This is the Area's index in the Talker Area array. */
	int index;

	/** This is the name of the Area. */
	String name;

	/** This stores the area's topic.  Sometimes, it is necessary to set a topic to direct
		the flow of conversation in a room. */
	String topic;

	/** This is the Area description.  This is used to provide atmosphere and environment
		to this made up virtual room. */
	String description;

	/** This is the Area's conversation buffer.  All online public communication taking
		place in a particular Area is stored here until cleared. */
	String[] rev = new String[20];


	/** Area() is the Area Object Constructor.  We simply link back to the Talker
		object here and initialize some of the variables.
		@param t This is a link back to the Talker Object. */
	public Area (Talker t) {
		this.t = t;
		topic = "none";

		for (int i=0; i<20; ++i) {
			rev[i] = "";
		}
	}

	/** addRev adds a line of online public communication to the area's conversation buffer.
		@param line is the line of text to be added to the conversation buffer.
		@return boolean representing whether or not an error has occurred. */
	public boolean addRev (String line) {
		int i=0;

		for (i=0; i<20; ++i) {

			// Either put the line in the first empty line we find...
			if (rev[i].equals("")) {
				rev[i] = line;
				return false;
			}
		}

		// ... or move all the lines up, and add the new line to the last object.
		for (i=0; i<19; ++i) {
			rev[i] = rev[i+1];
		}
		rev[19] = line;
		return false;
	}

	/** showRev() displays the Conversation Buffer to the user.
		@param conn is the link to the UserConnection of the user calling the .rev command.
		@return boolean indicating whether or not an error took place. */
	public boolean showRev (UserConnection conn) {
		int i=0;
		conn.println("~FYThe "+name+" Conversation Buffer:~N\n");

		for (i=0; i<20; ++i) {
			if (rev[i].equals("")) break;
			conn.println("- "+rev[i]+"~N");
		}
		conn.println(" ");

		return false;
	}

	/** clearRev() clears the Conversation Buffer.  This is done by overwriting the
		conversation buffer by empty strings.
		@param conn is a UserConnection of the user calling .cbuffer
		@return boolean indicating whether an error took place in the method. */
	public boolean clearRev (UserConnection conn) {
		int i=0;
		conn.println("Conversation Buffer cleared.");

		for (i=0; i<20; ++i) {
			rev[i] = "";
		}

		return false;
	}

	/** setDescription() simply sets the Area description of the room.
		@param description is the new description that we want to set.
		@return boolean indicating whether or not an error took place. */
	public boolean setDescription(String description) {
		this.description = description;

		return false;
	}

	/** initArea() builds the Area Array for Talker.  This is accomplished by reading the Area
		table and creating Area objects with the data.  The Area objects are then linked in
		an array and passed back to Talker.
		@param t is a link back to the Talker object.
		@return Area[] which is an array of Area Objects.*/
	public static Area[] initArea (Talker t) {
		Area[] areas = new Area[0];
		Area[] atemp = new Area[1];
		Area onearea;

		// Connect to the Database and run the Query.
		String sql = "SELECT name,description FROM area";
		t.connectDB();

		try {
			ResultSet rs = t.stmt.executeQuery(sql);
			int i=0;
			int j=0;

			// For each Area record, create a new Area Object.
			while (rs.next()) {
				String aname = "";
				String adesc ="";

				atemp = new Area[areas.length+1];

				try {
					aname = rs.getString(1);
					adesc = rs.getString(2);
				}
				catch (Exception e) {
					t.printSyslog("Exception Getting Area Info");
					e.printStackTrace();
				}

				// Set the data from the Area record to the onearea
				// temporary Area object.
				onearea = null;
				onearea = new Area(t);
				onearea.setName(aname);
				onearea.setDescription(adesc);
				onearea.setIndex(i);

				// Resize the Area array to fit the new Area object.
				for (j=0; j<areas.length; ++j) {
					try {
						atemp[j] = areas[j];
					}
					catch (Exception e) {
						t.printSyslog("Area Exception");
						e.printStackTrace();
					}
				}

				// Add it.
				atemp[j] = onearea;

				// Reset.
				areas = atemp;
				atemp = null;
				++i;
			}
			rs.close();
		}
		catch (Exception e) {
			t.printSyslog("Exception Reading Data from Area Table");
			e.printStackTrace();
		}
		return areas;
	}

	/** getName() returns the name of the Area.
		@return String name of the area. */
	public String getName () {
		return name;
	}

	/** setName() simply sets the name of the Area to the value being passed.
		@param name the new name to set the Area. */
	public void setName (String name) {
		this.name = name;
	}

	/** setIndex() sets the index of the Area in the Area array
		@param index is the new index of the Array. */
	public void setIndex (int index) {
		this.index = index;
	}

	/** setTopic sets the topic for the Area to the parameter.
		@param conn is a UserConnection to the user calling the .topic command.
		@param topic is a String containing the new topic for the room. */
	public void setTopic (UserConnection conn, String topic) {
		if (topic.equals("")) {
			topic = "none";
		}

		int length = t.conn.length;

		printArea(conn,""+conn.getUserName()+" sets the topic to: "+topic+"~N",false);
		conn.println("~FYYou set the topic to: "+topic+"~N");

		// Add this to the Conversation buffer
		addRev(""+conn.getUserName()+" sets the topic to: "+topic);
		this.topic = topic;
	}

	/** getIndex() returns the index of the room.
		@return integer index of the room in the Area array. */
	public int getIndex () {
		return index;
	}

	/** look() displays information about the current Area to a user.  It is called when the user
		executes the .look command or changes areas.
		@param conn is the UserConnection calling the .look command or changing areas. */
	public void look (UserConnection conn) {
		String sql="empty";
		int users=0;
		String uname = null;
		try {
			// Display the name of the Room and Room Description.
			conn.println("~FYWelcome to the "+name+" Room!~N");
			conn.println("\n"+description+"\n");

			// Display a list of users in the room.
			conn.println("~FY~ULUsers in the Room:~N\n  ");
			int length = t.conn.length;
			uname = conn.getUserName();

			for (int i=0; i<length; ++i) {
				if (t.conn[i].isAuthenticated()==false) continue;
				if (t.conn[i].getArea()==index) {
					if (uname.equals(t.conn[i].getUserName())) continue;
					conn.println("\t~FY"+t.conn[i].getUserName()+"~N "+t.conn[i].getUserDesc()+"~N");
					users++;
				}

			}
			if (users==0) conn.println("\tYou are alone here.");
			conn.println("   ");

			// Connect to the Database and get the number of messages on the board.
			t.connectDB();
			sql = "SELECT count(*) FROM messages, messarea WHERE messarea.areaid=\""+t.area[conn.getArea()].getName()+"\" AND messarea.messid=messages.id";
			ResultSet rs = t.stmt.executeQuery(sql);
			rs.next();
			int msgs = rs.getInt(1);
			rs.close();
			
			// Print the number of messages on the board.
			conn.println("~FPThere are ~FY"+msgs+"~FP Messages on the board.~N");

			// Display the Topic.
			conn.println("~FPThe current topic is: ~FY"+topic+"~N");
			conn.println(" ");

		}
		catch (Exception e) {
			t.printSyslog("Exception in Look()\n"+sql);
			e.printStackTrace();
		}
	}

	/** read() reads the Area's message board and displays it to a user.
		@param conn is the UserConnection making the .read request. */
	public void read (UserConnection conn) {
		try {
			// Display the name of the board.
			conn.println("~FY~ULThe "+name+" Message Board:~N\n");

			// Connect to the Database and read the messages information about this room.
			t.connectDB();
			String sql = "SELECT messages.id, messdate, messtime, name, message FROM messages, messarea WHERE messarea.areaid=\""+t.area[conn.getArea()].getName()+"\" AND messarea.messid=messages.id ORDER BY messages.id";
			ResultSet rs = t.stmt.executeQuery(sql);
			int number = 0;

			while (rs.next()) {
				number++;
				try {
					int id = rs.getInt(1);
					String messdate = rs.getString(2);
					String messtime = rs.getString(3);
					String messname = rs.getString(4);
					String message = rs.getString(5);

					// Print each message.
					conn.println("~FB"+messdate+": "+messtime+": ~FRFrom "+messname+":~N  "+message+"~N");
				}
				catch (Exception ex) {
					t.printSyslog("Exception Caught in Message");
					ex.printStackTrace();
				}
			}
			rs.close();

			// Print the total number of messages.
			conn.println("\n~FYTotal of "+number+" Messages~N");
			conn.println(" ");
		}
		catch (Exception e) {
			t.printSyslog("Caught Exception in Read(): "+e.getMessage());
		}

		// Tell everyone that someone is reading the board.
		printArea(conn, ""+conn.getUserName()+" reads the message board", false);
	}

	/** wipe() erases messages from the Area message board.  Wipe can either erase a number of messages or all of the messages, depending on the parameter "in"
		@param conn is the UserConnection making the request.
		@param in is a String containing the arguments that determine the type of wipe. */
	public boolean wipe (UserConnection conn, String in) {

		if (in.equals("")) {
			conn.println("~FR Usage:  .wipe all/numberofmessages~N");
			return true;
		}

		int todelete = 0;
		try {
			// Get the number of messages on the board that can be deleted.
			t.connectDB();
			String sql = "SELECT messages.id FROM messages, messarea WHERE messarea.areaid=\""+t.area[conn.getArea()].getName()+"\" AND messarea.messid=messages.id ORDER BY messages.id";
			ResultSet rs = t.stmt.executeQuery(sql);
			int number = 0;

			// If the string is "all", all messages will be erased, otherwise, read the number.
			if (in.equals("all")) {
				todelete = -1;
				conn.println("Erasing all Messages from the "+name+" Message Board...");
			}
			else {
				try {
					todelete = new Integer(in).intValue();
				}
				catch (Exception e) {
					return true;
				}
				if (todelete <= 0) return true;
				conn.println("Erasing "+todelete+" Messages from the "+name+" Message Board...");
			}

			// Run through the list of messages on the message board, reading the message
			// ID number of each message.  Then, delete the Message ID from both the Messages
			// table and the mess-area table.

			while (rs.next()) {
				number++;
				if (todelete != -1) {
					if (number > todelete) {
						--number;
						break;
					}
				}

				try {
					int id = rs.getInt(1);
					int sqlrows = 0;

					String sql2 = "DELETE FROM messages WHERE messages.id="+id;
					sqlrows = t.stmt.executeUpdate(sql2);
					//System.out.println("Executed: "+sql2+"; "+sqlrows+" Rows Effected.");

					String sql3 = "DELETE FROM messarea WHERE messarea.messid="+id;
					sqlrows = t.stmt.executeUpdate(sql3);
					//System.out.println("Executed: "+sql3+"; "+sqlrows+" Rows Effected.");


				}
				catch (Exception ex) {
					t.printSyslog("Exception Caught in Message");
					ex.printStackTrace();
				}
			}
			rs.close();
			conn.println("~FYTotal of "+number+" Messages Deleted.~N");
		}
		catch (Exception e) {
			t.printSyslog("Caught Exception in Wipe()");
			e.printStackTrace();
		}

		// Display to the room that someone has erased messages from the board.
		printArea(conn, ""+conn.getUserName()+" wipes some messsages from the board.", true);
		return false;
	}

	/** write() allows users to write Offline Public messages in an Area.
		@return boolean indicating whether or not an error has occurred.
		@param conn is a UserConnection linking back to the user calling the .write command.
		@param message is the message the User would like to write on the board. */
	public boolean write (UserConnection conn, String message) {
		try {
			if (message.equals("")) {
				conn.println("~FR Usage:  .write message~N");
				return true;
			}

			t.connectDB();
			ResultSet rs;
			int messid;
			int areaid;
			String sql;
			String areaname;
			int sqlrows=0;

			// Because this message is going into the database, we need to escape it.
			message = t.escapeUserInput(message);

			try {
				// Insert the Message into the Messages table.
				sql = "INSERT INTO messages (messdate,messtime,name,message) VALUES (\""+t.datestamp+"\", \""+t.timestamp+"\",\""+conn.getUserName()+"\",\""+message+"\")";
				sqlrows = t.stmt.executeUpdate(sql);

				// Removed Due to Windows Java Incompatibility
				//rs = t.stmt.getGeneratedKeys();
				//rs.next();
				//messid = rs.getInt(1);
				//System.out.println("Executed: "+sql+"; "+sqlrows+" Rows Effected.");

				// Select the message ID so we can insert this into the Message-Area table.
				sql = "SELECT id FROM messages WHERE name='"+conn.getUserName()+"' AND messdate='"+t.datestamp+"' AND messtime='"+t.timestamp+"' AND message='"+message+"'";
				rs = t.stmt.executeQuery(sql);
				rs.next();
				messid = rs.getInt(1);
				rs.close();
			}
			catch (Exception e) {
				t.printSyslog("Write(): Exception Inserting Message into Messages Table");
				e.printStackTrace();
				return true;
			}

			try {
                                areaname = t.area[conn.getArea()].getName();
			}
			catch (Exception e) {
				t.printSyslog("Write(): Exception Selecting Area ID From Area Table\n"+sql);
				e.printStackTrace();
				return true;
			}

			try {
				// Insert Message ID and Area ID into messarea table.
				sql = "INSERT INTO messarea (areaid, messid) VALUES (\""+areaname+"\","+messid+")";
				sqlrows = t.stmt.executeUpdate(sql);

			}
			catch (Exception e) {
				t.printSyslog("Write(): Exception Inserting Message and ID into MessArea");
				e.printStackTrace();
				return true;
			}
		}
		catch (Exception e) {
			t.printSyslog("Caught Exception in Write()");
			e.printStackTrace();
		}

		// Displays to the room a message informing them that someone wrote a new message.
		printArea(conn, ""+conn.getUserName()+" writes a message on the board.", false);
		conn.println("You write a message on the board.");

		return false;
	}


	/** printArea() displays a line of text to all of the users in a particular room.
		@return boolean indicating whether an error has taken place.
		@param conn is a link back to the UserConnection to which we compare the other users to when we determine which area to print to.
		@param message is a String with the message that we want to display.
		@param touser is a boolean indicating whether or not we print the message to the user "conn". */
	public boolean printArea(UserConnection conn, String message, boolean touser) {
		int areanum = conn.getArea();
		int usernum = conn.getIndex();
		int i = 0;

		for (i=0; i<t.conn.length; ++i) {
			if ((i==usernum) && (touser==false)) continue;

			if (t.conn[i].getArea()==areanum) {
				if (t.conn[i].isAuthenticated()==false) continue;
				t.conn[i].println(message+"~N");
			}
		}
		return false;
	}

}

//********************************************************************************************88
/** TalkerTimer System Timer Object
	@author Jeffrey K. Brown
	<p>The TalkerTimer is a Threaded object that continually montiors the UserConnection
	array every 60 seconds.  At each 60 second mark, TalkerTimer gets the time and date
	from the system (to make sure we are reading each minute).  Next, it increments the idle
	and time counters for each UserConnection.  Finally, it disconnects users who have been
	idle past the thresholds for authenticated and unauthenticated users.  The purpose of
	TalkerTimer was to save system resources by not keeping UserConnections open and reading.
	</p>
*/

class TalkerTimer extends Thread {

	/** This is a link back to the main Talker object.  We use it to refer back to
		the array of UserConnections and to call InitDate() and other methods. */
	static Talker t;

	/** TalkerTimer() Constructor simply links back to the calling Talker and starts the thread.
		@param t is a link back to the calling Talker object. */
	public TalkerTimer (Talker t) {
		this.t = t;
		start();
	}

	/** run() does all the work of the TalkerTimer.
		<p>First, the Thread is told to sleep for 60 seconds.  After waking, the
		date and time are read from the System.  Next, the TalkerTimer reads through
		the entire UserConnection array in Talker and increments the online and idle
		times by 1 minute.  Finally, the TalkerTimer run() compares the idle times
		the thresholds for disconnection and if the UserConnection is beyond the
		threshold, the user is disconnected from the system. */
	public void run () {
		int length=0;
		int idle=0;
		while (true) {

			try {
				try {
					// First, sleep.
					Thread.sleep(60000);  // 60 seconds
				}
				catch (Exception x) {
					t.printSyslog("TalkerTimer: Thead.sleep, probably interruptedException: "+x.getMessage());
				}
				


				try {
					// Read the System Date and Time
					t.initDates();
				}
				catch (Exception e) {
					t.printSyslog("TalkerTimer: Error with the time stuff: "+e.getMessage());
					continue;
				}

				length = t.conn.length;

				if (length > 0) {

					// Run through each UserConnection in the array.
					for (int i=0; i<length; ++i) {
						
						try {
							// Increment the Idle and Online Times
							t.conn[i].addMinute();
						}
						catch (Exception x) {
							t.printSyslog("TalkerTimer::run, Exception Adding Minute: "+x.getMessage());
							t.printSyslog("Testing what would happen if we kill the connection:");
							try {
								t.printSyslog("Attempting to Terminate User's Stuff:");
								t.conn[i].in.close();
								t.conn[i].out.close();
								t.conn[i].socket.close();
								t.printSyslog("User's Stuff Terminated Successfully!");
								continue;
							} catch (Exception xx) {
								t.printSyslog("TalkerTimer Exception Quitting the addminute offender:"+xx.getMessage());
								continue;
							}
						}

						// Read the number of minutes idle.
						idle = t.conn[i].getIdle();
						
						// Check the Authenticated User Thresholds
						if (t.conn[i].isAuthenticated()==true) {

							// Give a Warning
							if (idle == 55) {
								try {
									//t.printSyslog("Preparing to idle-warn a user...");
									t.conn[i].println("~FPYou have been idle 55 minutes.\nYou will be autoremoved in 5 \nminutes unless you respond.~N");
									//t.printSyslog("User has been idle-warned.");
								}
								catch (Exception x) {
									t.printSyslog("Exception Warning Idle User: "+x.getMessage());
								}
								continue;
							}

							// Actually Disconnect the user.
							if (idle >= 60) {  // 60 minutes when Thread Sleep is 60000
								try {
									//t.printSyslog("Preparing to AutoRemove idle user...");
									t.conn[i].println("~FRAssuming you are timed out.  Disconnecting you.~N");
									t.printSyslog("Disconnected "+t.conn[i].getUserName()+" after 60 minutes of inactivity");
									t.conn[i].quit();
									//t.printSyslog("Successfully Called Quit Routine for Idle user.");
								}
								catch (Exception x) {
									t.printSyslog("TalkerTimer::run Exception disconnecting idle user: "+x.getMessage());
								}
								continue;
							}
						}

						// Check the Unauthenticated User Thresholds
						if (t.conn[i].isAuthenticated()==false) {
							if (idle >= 5) {  // 5 minutes when Thread Sleep is 60000
								t.conn[i].println("\n~FRAssuming you are timed out.\nDisconnecting you.~N");
								try {
									//t.printSyslog("TalkerTimer: Attempting to disconnect unauthenticated idle socket...");
									t.conn[i].socket.close();
									//t.printSyslog("TalkerTimer: Successfully closed unauthenticated idle socket!");
								}
								catch (Exception x) {
									t.printSyslog("Error closing t.conn[i].socket.close(): "+x.getMessage());
								}									
								try {
									//t.printSyslog("TalkerTimer: Attempting to Delete unauthenticated idle user's stuff");
									//t.quitUser(i);
									t.conn[i].in.close();
									t.conn[i].out.close();

									//t.printSyslog("TalkerTimer: Successfully Deleted unauthenticated idle user's stuff.");
								}
								catch (Exception x) {
									t.printSyslog("Error disconnecting Unauthenticated idle socket: "+x.getMessage());
								}
								continue;
							}
						}
					}
				}
			}
			catch (NullPointerException npe) {
				t.printSyslog("TalkerTimer::run() NullPointerException:"+npe.getMessage());
				continue;
			}
			catch (Exception e) {
				t.printSyslog("TalkerTimer::run() Exception caught..."+e.getMessage());
				//e.printStackTrace();
				continue;
			}
		}
	}
}
