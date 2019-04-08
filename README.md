# JavaTalker 
This is Jkb's JavaTalker project that he submitted for his graduate degree back in 2004.  This project used Java, JDBC 
and Sockets to replicate the functionality and experience of an old NUTS talker.  Part of the fun is that because it's 
Java, in addition to running on Linux and Unix like NUTS, it can also run on Windows without problems.  I've put it out 
onto github just for fun, and that's a better place for it than an old hard drive somewhere.

I know I made some later updates to JavaTalker, but I can't find the most recent updates for it.  I had a small project 
to boot it back up, so I booted an older version.  If I ever find the more recent updates, I'll refresh this project.

Lastly, this is my code from 2003 and 2004, and I've been writing code for a long time now, so while it was pretty 
exciting and new to me back then,  I definitely find it pretty cringeworthy these days. :)

Notes and Stuff
-----
Warning:  Nobody uses NUTS talkers (or JavaTalkers) anymore because they basically submit plain text over the Internet.  
It's all perfectly easy to intercept as I've demonstrated in the project-report/docs/tcpdump folder.

Warning:  Back in 2003 and 2004, I wasn't able to figure out a good way to encrypt the passwords in the database, so I 
wrote a quick and dirty CaesarCiper.java class with methods to 'encrypt' and 'decrypt' passwords.  It's 2019 now, and 
it's a best security practice to never write your own crypto.

To get the talker running again, I needed to boot up a javatalker database and populate it with data.  In the original
release, I used an old version of MySQL.  For the most recent relaunch, I loaded it with MariaDB.  I've accidentally 
(but now I'm leaving it there) included the MariaDB JDBC driver in the talker folder.

There's a startup.pl Perl script that has a few handy features to launch the talker using the at queue because that's 
how I knew how to start and run a script outside of the current shell.  There's almost certainly a better way to do it 
using nohup or an actual linux startup script.  It also needs a few tweaks for paths and stuff.

There's a subdirectory called Chatlet that has a Java Applet that lets you hit the JavaTalker from a web browser (with 
Java, of course).  Warning: Nobody really does this anymore and most browsers' Java support is dubious these days--ymmv.  

In case you were curious, under project-reports, there is my original project proposal and final submitted reports, plus
some supporting project docs (tcpdump, uml) that are also included in the final reports.

TODO Stuff 
---------
So, maybe someday I'll look into these TODO's, but maybe I never will.
* TODO: Include the command line to start up the JavaTalker.
* TODO: Update the connection string in the DBConnection class so we're not hard-coding the db connection info.
* TODO: Maybe include a screenshot or two
* TODO: Clean up the Database Creation scripts a little better.
* TODO: Import the Help Files?
* TODO: If I ever find the JavaBot code, I'll load that in here, too.
* TODO: I think I have the C socket program somewhere, too, that I can include.
* TODO: Maybe pick and put an OSS license in there?
