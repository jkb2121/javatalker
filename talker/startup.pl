#!/usr/bin/perl 
#####################################################################################
# Java Talker Startup Script
# CS690 Project
# Jeffrey K. Brown
#
# This Java Talker Startup Script is for Unix and Linux systems. It uses two
# different utilities, the AT scheduler and the Perl Interpreter to pull the system
# time and then use it to schedule the Java Talker to start.  It is necessary to
# start Java servers on Linux in this manner because Java does not allow processes to
# fork, or spawn new processes.  If you start the Java Talker server and then attempt
# to disconnect from the server, you will either disconnect the Java Talker Server,
# or your server connection will not terminate.  We get around this by scheduling the
# server process to start $delay minutes in the future with the AT scheduler.  The AT
# scheduler creates a new shell and launches the Java Talker Server in it.  Shutting
# down the Talker Server is done by the Talker command .shutdown.  If necessary, the
# Java Talker Server can also be forcibly killed by the Unix command kill.
#
#####################################################################################
# These variables can be customized to your Java Talker installation and server.
#
$delay = 1;				
$jtpath = "/home/jkb/jolt/";     	
$jtfile = "Talker";			
$javapath= "/usr/lib/java/bin/java";	
$atpath = "/usr/bin/at";		
$logfile = "jolt.log";
$jkbjar = "/home/jkb/jolt/jkbworld.jar";			
#
#####################################################################################
# Java Talker Startup Script Methodology:
#
# To start the Java Talker, we need to make a call looking like this:
# java Talker
#
# Because AT emails all the output of the call to you upon process termination,
# I have all of the output going into a log file:
# java Talker > JavaTalker.log
#
# The AT schedule, however, will only take one parameter for the file to be executed.
# Additionally, what if the user is calling the script from a different directory
# than the Java Talker directory.  For this reason, we generate a script file to
# connect to the Java Talker directory, execute the Talker Server process and then
# capture the output to a log file.
#
# Using the $delay variable, we add the number of minutes to the system time and
# schedule the new script to run at that time.  Finally, we delete the script.
#####################################################################################

# Assign a name to a temporary file based on a random number.  We also put the
# temporary file in the Java Talker Path.
$random = rand 10000000;
$tempfile = "$jtpath$random";

# Next, we open the temporary file and then add commands to change to the Java Talker
# Path and to run the Java Talker Server, capturing all output to the logfile.  Last,
# we close the file.

open (FILE, ">$tempfile") or die "Unable to Create and Open Temp File: $!\n";
print FILE "\ncd $jtpath";
print FILE "\n$javapath -classpath .:$jtpath:$jkbjar $jtfile > $jtpath$logfile";
print FILE "\n";
close (FILE);

# Next, we make the new script executable, so AT will be able to run the commands
# stored inside and start the Java Talker.
chmod 0755, "$jtpath/$random";

# At this stage, we get the system time and separate the individual attributes
# using Perl's localtime function.

($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);

# Here, we add the number of seconds to wait ($delay) to the system minutes and
# check the new time, making changes if needed.  These checks work best if $delay
# is 1 minute.  If you wanted to increase the delay, you might want to verify that
# these checks still provide acceptable performance.

$min += $delay;
if ($min<10) {
	$min = "0$min";
}
if ($hour<10) {
	$hour= "0$hour";
}
if ($min==60) {
	$min="00";
	$hour=$hour+1;
	if ($hour==24) {
		$hour="00";
	}
}

print "String: ", $atpath, " -f ", $tempfile, " ", $hour, $min;

# Now we generate the system call to the AT Scheduler.  $atpath contains the path
# to the AT scheduler executable.  The "-f" tells AT to execute the file whose name
# is the next token.  The $tempfile is the name of our temporary file.  $hour$min is
# the new time we generated at which time the Java Talker will be started.  Finally,
# we run the command.

@startup = ("$atpath", "-f", "$tempfile", "$hour$min");
system(@startup)==0 or die "Error Running At! $!";

# Last, we delete the temporary file.

unlink $tempfile;

# The End.  The Java Talker executable should be scheduled and will start in 1 minute

#####################################################################################
