package com.jkbworld;


/**
 * @author jkb
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CaesarCipher {
	private String version = "0.0.1";
	
	private char[] array = new char[89];	
	
	public CaesarCipher() {
		array[0] = '0';
		array[1] = '1';
		array[2] = '2';
		array[3] = '3';
		array[4] = '4';
		array[5] = '5';
		array[6] = '6';
		array[7] = '7';
		array[8] = '8';
		array[9] = '9';
		array[10] = 'a';
		array[11] = 'b';
		array[12] = 'c';
		array[13] = 'd';
		array[14] = 'e';
		array[15] = 'f';
		array[16] = 'g';
		array[17] = 'h';
		array[18] = 'i';
		array[19] = 'j';
		array[20] = 'k';
		array[21] = 'l';
		array[22] = 'm';
		array[23] = 'n';
		array[24] = 'o';
		array[25] = 'p';
		array[26] = 'q';
		array[27] = 'r';
		array[28] = 's';
		array[29] = 't';
		array[30] = 'u';
		array[31] = 'v';
		array[32] = 'w';
		array[33] = 'x';
		array[34] = 'y';
		array[35] = 'z';
		array[36] = 'A';
		array[37] = 'B';
		array[38] = 'C';
		array[39] = 'D';
		array[40] = 'E';
		array[41] = 'F';
		array[42] = 'G';
		array[43] = 'H';
		array[44] = 'I';
		array[45] = 'J';
		array[46] = 'K';
		array[47] = 'L';
		array[48] = 'M';
		array[49] = 'N';
		array[50] = 'O';
		array[51] = 'P';
		array[52] = 'Q';
		array[53] = 'R';
		array[54] = 'S';
		array[55] = 'T';
		array[56] = 'U';
		array[57] = 'V';
		array[58] = 'W';
		array[59] = 'X';
		array[60] = 'Y';
		array[61] = 'Z';
		array[62] = '`';
		array[63] = '~';
		array[64] = '!';
		array[65] = '@';
		array[66] = '#';
		array[67] = '$';
		array[68] = '%';
		array[69] = '^';
		array[70] = '&';
		array[71] = '*';
		array[72] = '(';
		array[73] = ')';
		array[74] = '-';
		array[75] = '_';
		array[76] = '=';
		array[77] = '+';
		array[78] = '\\';
		array[79] = '|';
		array[80] = ',';
		array[81] = '<';
		array[82] = '.';
		array[83] = '>';
		array[84] = '?';
		array[85] = '/';
		array[86] = ' ';
		array[87] = ';';
		array[88] = ':';
		
		
	}
	
	private int getCharIndex(char c) {
		int i=0;
		for (i=0; i<array.length; ++i) {
			if (array[i]==c) return i;
		}
		System.out.println("Character '"+c+"' Not Found!");
		return array.length;
	}
	
	private char getIndexChar(int i) {
		try{
			return array[i];
		}
		catch (Exception e) {
			System.out.println("Index: "+i);
			e.printStackTrace();
			try {
				return array[array.length-1];
			}
			catch (Exception ex) {
				return '?';
			}
		}
	}
	
	
	public String getVersion() {
		return version;
	}
	
	public String encodeMessage(String messagein) {
		String encoded="";
		char[] chararray = new char[messagein.length()];
		char[] encarray = new char[messagein.length()];
		
		long shift = 0;
		int i=0;		
		
		shift = Math.abs(Math.round(Math.random() * array.length-2));
		
		chararray = messagein.toCharArray();
		int currentindex=0;
		int newindex=0;
		for (i=0; i<messagein.length(); ++i) {
			newindex = 0;
			currentindex = getCharIndex(chararray[i]);
			
			newindex = currentindex + (int)shift;
			if (newindex > (array.length-1)) {
				newindex -= array.length;
			}
			
			//System.out.println(""+chararray[i]+" - "+currentindex+" - "+newindex+" - "+getIndexChar(newindex));
		
			encarray[i] = getIndexChar(newindex);
		}
		
		
		String retstr = new String(encarray);
		
		encoded = ""+shift + "<space>" + retstr;
		return encoded;
	}
	
	public String decodeMessage(String messagein) {
		String decoded = "";
		decoded = messagein;
		//String mxmessage = "";
		String mixedmessage = "";
		int shift = 0;
		
		
		int index = messagein.indexOf("<space>");
				
		shift = new Integer(messagein.substring(0,index)).intValue();

		mixedmessage = messagein.substring(index+7);
			
	
		char[] encarray = mixedmessage.toCharArray();
		char[] chararray = new char[mixedmessage.length()];
		
		int newindex=0;
		int currentindex=0;
		int i=0;
		try {
		
			for (i=0; i<mixedmessage.length(); ++i) {
				currentindex=0;
				newindex = 0;
			
				currentindex = getCharIndex(encarray[i]);
				newindex = currentindex - shift;

				if (newindex < 0) {
					newindex += array.length;
				}	
			
				//System.out.println(""+encarray[i]+" - "+currentindex+" - "+newindex+" - "+getIndexChar(newindex));
			
		
				try {
					chararray[i] = getIndexChar(newindex);
				}
				catch (Exception ex) {
					chararray[i] = '?';
				}
			}
			//System.out.println("Shift="+shift);
			//System.out.println("Array Length: "+array.length);
		
			decoded = new String(chararray);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return decoded;
	}
	
	public static void main(String[] args) {
		System.out.println("This is CaesarCipher...");
		CaesarCipher cc = new CaesarCipher();
		
		System.out.println("Version: "+cc.getVersion());
		
		String message = "@18:00 Hours, 12/31/2004: Fire all of the missiles at Russia! -EL PRESIDENTE:;";
		String working = "";
		
		
		System.out.println ("Message: "+message);
		
		working = cc.encodeMessage(message);
		
		System.out.println ("Encoded: "+working);
		
		message = cc.decodeMessage(working);
		
		System.out.println ("Decoded: "+message);		
		
		System.out.println("That was CaesarCipher...");
	}
	
	
	
}
