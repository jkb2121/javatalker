-- MySQL dump 8.21
--
-- Host: localhost    Database: javatalker
---------------------------------------------------------
-- Server version	3.23.48-log

--
-- Table structure for table 'area'
--

CREATE TABLE area (
  id int(11) NOT NULL auto_increment,
  name varchar(50) default NULL,
  description text,
  idx int(11) NOT NULL default '0',
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Dumping data for table 'area'
--


INSERT INTO area VALUES (1,'Main','This is the Main Room, Welcome to it',0);
INSERT INTO area VALUES (2,'Other','This is just another room.  Nothing special',1);
INSERT INTO area VALUES (3,'Pirate Ship','You see a big pirate ship.  Walk the plank, matey!',2);

--
-- Table structure for table 'mail'
--

CREATE TABLE mail (
  id int(11) NOT NULL auto_increment,
  sender varchar(25) default NULL,
  mdate varchar(50) default NULL,
  mtime varchar(50) default NULL,
  message text,
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Dumping data for table 'mail'
--


INSERT INTO mail VALUES (2,'Jkb','Tue 08/12/2003','23:20','okay here\'s a nother');

--
-- Table structure for table 'messages'
--

CREATE TABLE messages (
  id int(11) NOT NULL auto_increment,
  name varchar(25) default NULL,
  messdate varchar(30) default NULL,
  messtime varchar(30) default NULL,
  message text,
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Dumping data for table 'messages'
--


INSERT INTO messages VALUES (27,'Jkb','Mon 08/11/2003','23:53','okay... what if we did this\"');
INSERT INTO messages VALUES (7,'Jkb','Mon 08/11/2003','22:41','This is a new message from Jkb');
INSERT INTO messages VALUES (28,'Jkb','Mon 08/11/2003','23:56','okay, here we go again');
INSERT INTO messages VALUES (26,'Jkb','Mon 08/11/2003','23:52','I\'d like to know\"');
INSERT INTO messages VALUES (25,'Jkb','Mon 08/11/2003','23:52','okay\" how about \"doubles?');
INSERT INTO messages VALUES (24,'Jkb','Mon 08/11/2003','23:51','here\'s another\'');
INSERT INTO messages VALUES (15,'Jkb','Mon 08/11/2003','23:35','\"');
INSERT INTO messages VALUES (17,'Jkb','Mon 08/11/2003','23:36','\'');
INSERT INTO messages VALUES (29,'Nubia','Mon 08/11/2003','23:58','test now');
INSERT INTO messages VALUES (32,'Nubia','Tue 08/12/2003','24:01','okay now let\'s test the dereference \\ in the middle');
INSERT INTO messages VALUES (33,'Nubia','Tue 08/12/2003','24:02','lets try to escape the closing quote with a \\');
INSERT INTO messages VALUES (34,'Nubia','Tue 08/12/2003','24:02','let\'s try again');
INSERT INTO messages VALUES (35,'Jkb','Tue 08/12/2003','23:03','okay, does write still work?');
INSERT INTO messages VALUES (36,'Jkb','Tue 08/12/2003','23:03','sweet..it does');

--
-- Table structure for table 'messarea'
--

CREATE TABLE messarea (
  id int(11) NOT NULL auto_increment,
  messid int(11) NOT NULL default '0',
  areaid varchar(25) default NULL,
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Dumping data for table 'messarea'
--


INSERT INTO messarea VALUES (7,7,'Other');
INSERT INTO messarea VALUES (29,29,'Main');
INSERT INTO messarea VALUES (28,28,'Main');
INSERT INTO messarea VALUES (27,27,'Main');
INSERT INTO messarea VALUES (26,26,'Main');
INSERT INTO messarea VALUES (25,25,'Main');
INSERT INTO messarea VALUES (24,24,'Main');
INSERT INTO messarea VALUES (32,32,'Main');
INSERT INTO messarea VALUES (33,33,'Main');
INSERT INTO messarea VALUES (34,34,'Main');
INSERT INTO messarea VALUES (35,35,'Main');
INSERT INTO messarea VALUES (36,36,'Main');

--
-- Table structure for table 'user'
--

CREATE TABLE user (
  username varchar(20) NOT NULL default '',
  password varchar(20) NOT NULL default '',
  description varchar(40) default 'a new user',
  level int(11) NOT NULL default '0',
  profile text,
  totaltime int(11) default NULL,
  gender varchar(10) NOT NULL default 'Unknown',
  age int(11) default '0',
  email varchar(60) default NULL,
  homepage varchar(60) default NULL,
  photo varchar(60) default NULL,
  colorpref int(11) default '2',
  lastsite varchar(60) default NULL,
  laston date default NULL,
  PRIMARY KEY  (username)
) TYPE=MyISAM;

--
-- Dumping data for table 'user'
--


INSERT INTO user VALUES ('Jkb','PW90HPE2GiS56','a new user',0,NULL,NULL,'Unknown',0,NULL,NULL,NULL,2,NULL,NULL);
INSERT INTO user VALUES ('Summoner','PWuA8j9GF3uhE','a new user',0,NULL,NULL,'Unknown',0,NULL,NULL,NULL,2,NULL,NULL);
INSERT INTO user VALUES ('Nubia','PWoYmNoH9guK6','a new user',0,NULL,NULL,'Unknown',0,NULL,NULL,NULL,2,NULL,NULL);
INSERT INTO user VALUES ('Penguin','PWco/SKgZkpQs','a new user',0,NULL,NULL,'Unknown',0,NULL,NULL,NULL,2,NULL,NULL);
INSERT INTO user VALUES ('Webuser','PWm2nLamJbpyo','a new user',0,NULL,NULL,'Unknown',0,NULL,NULL,NULL,2,NULL,NULL);

--
-- Table structure for table 'usermail'
--

CREATE TABLE usermail (
  id int(11) NOT NULL auto_increment,
  username varchar(25) NOT NULL default '',
  mailid int(11) NOT NULL default '0',
  PRIMARY KEY  (id)
) TYPE=MyISAM;

--
-- Dumping data for table 'usermail'
--


INSERT INTO usermail VALUES (2,'Jkb',2);

