# Flight Database Management App
I developed this 2-tiered database application for a datamanagement course that covered SQL, Relational Databases, ER-Diagrams, DBMS, NoSql DBs, Transactions etc.

The client side is written in java and communicates to a microsoft azure sql server using JDBC. All data is stored in a relational sql server db from azure. Data query and retrieval using sql (ofc) The app supports the following functions:
  - create <username> <password> <balance>

  creates a new user with the given username, password, and initial balance. Stores this in the RDMS, with the password as a salted hash.

  - login <username> <password>

  logs in the given user if the salted hash of the given password matches that in the database.

  - search <origin city> <dest city> <isDirect> <day> <num_results>

  searches and returns list of flight itineraries given the critera

  - book <iternary id>

  given itinerary id (returned from a search) reserved this itinerary for the logged in user, storing it in the db.

  - pay <reservation id>

  given a reservations id associated with the logged in user, pays for this reservation if the user has the funds, and updates the db accordingly.

  - reservations

  lists all reservations for the logged in user.

  - quit

  quits the app

  ## build and run
this app uses maven for the build system, to build and run:
$mvn clean compile assembly:single
$java -jar target/FlightApp-1.0-jar-with-dependencies.jar

but note, it wont actually build and run because the file "dbconn.properties" containing info and auth to connect to the azure database has been removed for obvious reasons since this repo is public.
