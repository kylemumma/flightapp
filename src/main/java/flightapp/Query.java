package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.util.ArrayList;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  // clearTables
  private static final String DELETE_USERS_SQL = "DELETE FROM USERS_kmumma1";
  private static final String DELETE_RESERVATIONS_SQL = "DELETE FROM RESERVATIONS_kmumma1";
  private PreparedStatement deleteUsersStmt;
  private PreparedStatement deleteReservationsStmt;

  // create
  private static final String CREATE_USER_SQL = "INSERT INTO USERS_kmumma1 VALUES (?, ?, ?)";
  private PreparedStatement createUserStmt;

  // login
  private static final String GET_CREDENTIALS_SQL = "SELECT password FROM USERS_kmumma1 WHERE username = ?";
  private PreparedStatement getCredentialsStmt;

  // search
  private static final String GET_DIRECT_FLIGHTS_SQL = "SELECT TOP (?) " 
  + "fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
  + "FROM Flights " 
  + "WHERE origin_city = ? "
  + "AND dest_city = ? "
  + "AND day_of_month = ? "
  + "AND canceled = 0 "
  + "ORDER BY actual_time, fid ASC";
  private static final String GET_INDIRECT_FLIGHTS_SQL = "SELECT TOP (?) "
  + "f1.day_of_month,f1.fid as fid1,f1.carrier_id as car1,f1.flight_num as fnum1,f1.origin_city as origin1,f1.dest_city as dest1,f1.actual_time as time1,f1.capacity as cap1,f1.price as pri1, "
  + "f2.fid as fid2,f2.carrier_id as car2,f2.flight_num as fnum2,f2.origin_city as origin2,f2.dest_city as dest2,f2.actual_time as time2,f2.capacity as cap2,f2.price as pri2 "
  + "FROM FLIGHTS f1 "
  + "JOIN FLIGHTS f2 "
  + "ON f1.dest_city = f2.origin_city AND f1.day_of_month = f2.day_of_month "
  + "WHERE f1.origin_city = ? "
  + "AND f2.dest_city = ? "
  + "AND f1.day_of_month = ? "
  + "AND f1.canceled = 0 "
  + "AND f2.canceled = 0 "
  + "ORDER BY f1.actual_time + f2.actual_time, f1.fid, f2.fid ASC";
  private PreparedStatement getIndirectFlightsStmt;
  private PreparedStatement getDirectFlightsStmt;

  // book
  private static final String GET_MAX_RID_SQL = "SELECT MAX(rid) AS mrid FROM RESERVATIONS_kmumma1";
  private static final String INSERT_NEW_BOOKING_SQL = "INSERT INTO RESERVATIONS_kmumma1 VALUES (?,?,?,?,?)";
  private PreparedStatement getMaxRidStmt;
  private PreparedStatement insertNewBookingStmt;

  private static final String GET_NUM_SEATS_BOOKED_SQL = "SELECT COUNT(*) AS cnt FROM RESERVATIONS_kmumma1 WHERE fid1 = ? OR fid2 = ?";
  private PreparedStatement getNumSeatsBookedStmt;

  private static final String GET_USER_RES_CT_FOR_DAY_SQL = "SELECT COUNT(*) AS cnt FROM RESERVATIONS_kmumma1 r JOIN FLIGHTS f ON f.fid = r.fid1 WHERE r.username = ? AND f.day_of_month = ?";
  private PreparedStatement getUserResCtForDayStmt;

  // pay
  private static final String GET_RESERVATION_SQL = "SELECT * FROM RESERVATIONS_kmumma1 WHERE rid = ? AND username = ? AND paid = 0";
  private PreparedStatement getReservationStmt;
  private static final String PAY_RESERVATION_SQL = "UPDATE RESERVATIONS_kmumma1 SET paid = 1 WHERE rid = ?";
  private PreparedStatement payReservationStmt;
  private static final String GET_USER_BALANCE_SQL = "SELECT balance FROM USERS_kmumma1 WHERE username = ?";
  private PreparedStatement getUserBalanceStmt;
  private static final String UPDATE_USER_BALANCE_SQL = "UPDATE USERS_kmumma1 SET balance = ? WHERE username = ?";
  private PreparedStatement updateUserBalanceStmt;
  private static final String GET_FLIGHT_PRICE_SQL = "SELECT price FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement getFlightPriceStmt;
  
  // reservations
  private static final String GET_RESERVATIONS_FOR_USER_SQL = "SELECT * FROM RESERVATIONS_kmumma1 WHERE username = ?";
  private PreparedStatement getReservationsForUserStmt;
  private static final String GET_RESERVATIONS_FOR_USER_SQL2 = "SELECT "
  + "r.rid, r.paid, "
  + "f1.day_of_month,f1.fid as fid1,f1.carrier_id as car1,f1.flight_num as fnum1,f1.origin_city as origin1,f1.dest_city as dest1,f1.actual_time as time1,f1.capacity as cap1,f1.price as pri1, "
  + "f2.fid as fid2,f2.carrier_id as car2,f2.flight_num as fnum2,f2.origin_city as origin2,f2.dest_city as dest2,f2.actual_time as time2,f2.capacity as cap2,f2.price as pri2 " 
  + "FROM RESERVATIONS_kmumma1 r JOIN FLIGHTS f1 ON r.fid1 = f1.fid LEFT OUTER JOIN FLIGHTS f2 ON r.fid2 = f2.fid WHERE r.username = ? "
  + "ORDER BY r.rid ASC";
  private PreparedStatement getReservationsForUserStmt2;

  // Instance variables
  //
  String user = "";
  List<Flight[]> searchResults;

  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      deleteReservationsStmt.executeUpdate();
      deleteUsersStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    
    // clearTables
    deleteUsersStmt = conn.prepareStatement(DELETE_USERS_SQL);
    deleteReservationsStmt = conn.prepareStatement(DELETE_RESERVATIONS_SQL);
  
    // create
    createUserStmt = conn.prepareStatement(CREATE_USER_SQL);

    // login
    getCredentialsStmt = conn.prepareStatement(GET_CREDENTIALS_SQL);

    // search
    getDirectFlightsStmt = conn.prepareStatement(GET_DIRECT_FLIGHTS_SQL);
    getIndirectFlightsStmt = conn.prepareStatement(GET_INDIRECT_FLIGHTS_SQL);
  
    // book
    getMaxRidStmt = conn.prepareStatement(GET_MAX_RID_SQL);
    insertNewBookingStmt = conn.prepareStatement(INSERT_NEW_BOOKING_SQL);
    getNumSeatsBookedStmt = conn.prepareStatement(GET_NUM_SEATS_BOOKED_SQL);
    getUserResCtForDayStmt = conn.prepareStatement(GET_USER_RES_CT_FOR_DAY_SQL);
    
    // pay
    getReservationStmt = conn.prepareStatement(GET_RESERVATION_SQL);
    payReservationStmt = conn.prepareStatement(PAY_RESERVATION_SQL);
    getUserBalanceStmt = conn.prepareStatement(GET_USER_BALANCE_SQL);
    updateUserBalanceStmt = conn.prepareStatement(UPDATE_USER_BALANCE_SQL);
    getFlightPriceStmt = conn.prepareStatement(GET_FLIGHT_PRICE_SQL);

    // reservations
    getReservationsForUserStmt = conn.prepareStatement(GET_RESERVATIONS_FOR_USER_SQL);
    getReservationsForUserStmt2 = conn.prepareStatement(GET_RESERVATIONS_FOR_USER_SQL2);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    if (!this.user.equals("")) return "User already logged in\n";
    try {
      getCredentialsStmt.setString(1, username.toLowerCase());
      ResultSet r = getCredentialsStmt.executeQuery();
      r.next();
      byte[] storedPwrd = r.getBytes("password");
      if (PasswordUtils.plaintextMatchesSaltedHash(password, storedPwrd)) {
        this.user = username.toLowerCase();
        this.searchResults = null;
        return String.format("Logged in as %s\n", username);
      } else {
        return "Login failed\n";
      }
    } catch (Exception e) {
      return "Login failed\n";
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) return "Failed to create user\n";
    try {
      createUserStmt.setString(1, username.toLowerCase());
      createUserStmt.setBytes(2, PasswordUtils.saltAndHashPassword(password));
      createUserStmt.setInt(3, initAmount);
      createUserStmt.executeUpdate();
      return String.format("Created user %s\n", username);
    } catch (Exception e) {
      //e.printStackTrace();
      return "Failed to create user\n";
    }    
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return, must be positive
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    if (numberOfItineraries <= 0) return "No flights match your selection\n";
    StringBuffer sb = new StringBuffer();
    try {
      // get the direct flights
      List<Flight> directFlights = new ArrayList<>();
      getDirectFlightsStmt.setInt(1, numberOfItineraries);
      getDirectFlightsStmt.setString(2, originCity);
      getDirectFlightsStmt.setString(3, destinationCity);
      getDirectFlightsStmt.setInt(4, dayOfMonth);
      ResultSet directResults = getDirectFlightsStmt.executeQuery();
      while (directResults.next()) {
        int id = directResults.getInt("fid");
        int day = directResults.getInt("day_of_month");
        String carrier = directResults.getString("carrier_id");
        String fnum = directResults.getString("flight_num");
        String origin = directResults.getString("origin_city");
        String dest = directResults.getString("dest_city");
        int tm = directResults.getInt("actual_time");
        int cap = directResults.getInt("capacity");
        int pri = directResults.getInt("price");
        Flight curr = new Flight(id, day, carrier, fnum, origin, dest, tm, cap, pri);
        directFlights.add(curr);
      }
      directResults.close();

      // get the indirect flights
      List<Flight[]> indirectFlights = new ArrayList<>();
      if (!directFlight) {
        getIndirectFlightsStmt.setInt(1, numberOfItineraries-directFlights.size());
        getIndirectFlightsStmt.setString(2, originCity);
        getIndirectFlightsStmt.setString(3, destinationCity);
        getIndirectFlightsStmt.setInt(4, dayOfMonth);
        ResultSet indirectResults = getIndirectFlightsStmt.executeQuery();
        while (indirectResults.next()) {
          int id = indirectResults.getInt("fid1");
          int day = indirectResults.getInt("day_of_month");
          String carrier = indirectResults.getString("car1");
          String fnum = indirectResults.getString("fnum1");
          String origin = indirectResults.getString("origin1");
          String dest = indirectResults.getString("dest1");
          int tm = indirectResults.getInt("time1");
          int cap = indirectResults.getInt("cap1");
          int pri = indirectResults.getInt("pri1");
          Flight f1 = new Flight(id, day, carrier, fnum, origin, dest, tm, cap, pri);

          
          id = indirectResults.getInt("fid2");
          day = indirectResults.getInt("day_of_month");
          carrier = indirectResults.getString("car2");
          fnum = indirectResults.getString("fnum2");
          origin = indirectResults.getString("origin2");
          dest = indirectResults.getString("dest2");
          tm = indirectResults.getInt("time2");
          cap = indirectResults.getInt("cap2");
          pri = indirectResults.getInt("pri2");
          Flight f2 = new Flight(id, day, carrier, fnum, origin, dest, tm, cap, pri);


          indirectFlights.add(new Flight[] {f1,f2});
        }
        indirectResults.close();
      }

      // merge sorted directFlights and indirectFlights
      List<Flight[]> itineraries = new ArrayList<Flight[]>();
      int dp = 0;
      int ip = 0;
      while(dp < directFlights.size() || ip < indirectFlights.size()) {
        Flight[] toAdd;
        if (dp < directFlights.size() && ip < indirectFlights.size()) {
          Flight i1 = directFlights.get(dp);
          Flight[] i2 = indirectFlights.get(ip);
          if (i1.time < (i2[0].time + i2[1].time)) {
            toAdd = new Flight[] {i1, null};
            dp++;
          } else if (i1.time > i2[0].time + i2[1].time) {
            toAdd = i2;
            ip++;
          } else {  // tie
            if (i1.fid > i2[0].fid) {
              toAdd = new Flight[] {i1, null};
              dp++;
            } else {
              toAdd = i2;
              ip++;
            }
          }
        } else if (dp < directFlights.size()) {
          toAdd = new Flight[] {directFlights.get(dp), null};
          dp++;
        } else {  // ip < indirectFlights.size()
          toAdd = indirectFlights.get(ip);
          ip++;
        }
        itineraries.add(toAdd);
      }

      // store latest search if logged in
      if (!user.equals("")) searchResults = itineraries;

      // print itineraries
      if (itineraries.size() == 0) return "No flights match your selection\n";
      for (int i = 0; i < itineraries.size(); i++) {
        Flight[] curr = itineraries.get(i);
        if (curr[1] == null) {  // direct
          String fStr = String.format("Itinerary %d: 1 flight(s), %d minutes\n", i, curr[0].time);
          sb.append(fStr + curr[0].toString() + "\n");
        } else {  // indirect
          String fStr = String.format("Itinerary %d: 2 flight(s), %d minutes\n", i, curr[0].time + curr[1].time);
          sb.append(fStr + curr[0].toString() + "\n" + curr[1].toString() + "\n");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    if (user.equals("")) return "Cannot book reservations, not logged in\n";
    try {
      if(searchResults == null || itineraryId < 0 || itineraryId >= searchResults.size())
        return String.format("No such itinerary %s\n", itineraryId);
      
      Flight[] itn = searchResults.get(itineraryId);

      conn.setAutoCommit(false);
      /* does user already have a reservation on this day? */
      getUserResCtForDayStmt.setString(1, user);
      getUserResCtForDayStmt.setInt(2, itn[0].dayOfMonth);
      ResultSet r = getUserResCtForDayStmt.executeQuery();
      r.next();
      if (r.getInt("cnt") != 0) {
        conn.rollback();
        return "You cannot book two flights in the same day\n";
      }

      /* see if flights have space left */
      // flight 1
      getNumSeatsBookedStmt.setInt(1, itn[0].fid);  // fid1
      getNumSeatsBookedStmt.setInt(2, itn[0].fid);  // fid2
      r = getNumSeatsBookedStmt.executeQuery();
      r.next();
      if (r.getInt("cnt") >= itn[0].capacity) {
        conn.rollback();
        return "Booking failed\n";  // fully booked
      }
      // flight 2
      if (itn[1] != null) {
        getNumSeatsBookedStmt.setInt(1, itn[1].fid);  // fid1
        getNumSeatsBookedStmt.setInt(2, itn[1].fid);  // fid2
        r = getNumSeatsBookedStmt.executeQuery();
        r.next();
        if (r.getInt("cnt") >= itn[1].capacity) {
          conn.rollback();
          return "Booking failed\n";  // fully booked
        }
      }

      /*  create new reservation */
      // rid
      r = getMaxRidStmt.executeQuery();
      int nextRid;
      if (!r.next()) nextRid = 0;
      else nextRid = r.getInt("mrid")+1;
      insertNewBookingStmt.setInt(1, nextRid);

      insertNewBookingStmt.setInt(2, itn[0].fid);  // fid1

      // fid2
      if (itn[1] == null) insertNewBookingStmt.setNull(3, java.sql.Types.INTEGER);
      else insertNewBookingStmt.setInt(3, itn[1].fid);

      insertNewBookingStmt.setString(4, user);  // username
      insertNewBookingStmt.setInt(5, 0);  // unpaid
      insertNewBookingStmt.executeUpdate();

      conn.commit();
      return String.format("Booked flight(s), reservation ID: %d\n", nextRid);
    } catch (Exception e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
      } catch(Exception e2) {System.err.println("oh no");}

      if(e instanceof SQLServerException && e.getMessage().toLowerCase().contains("deadlock")) {
        try{Thread.sleep(500);} catch(Exception e2){}
        return transaction_book(itineraryId);
      } else {
        e.printStackTrace();
        return "Booking failed\n";
      }
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    if (user.equals("")) return "Cannot pay, not logged in\n";
    try {
      /* find the unpaid reservation for this user and given rid */
      conn.setAutoCommit(false);
      getReservationStmt.setInt(1, reservationId);  // rid
      getReservationStmt.setString(2, user);  // username
      ResultSet resv = getReservationStmt.executeQuery();
      if (!resv.next()) {
        conn.rollback();
        return String.format("Cannot find unpaid reservation %d under user: %s\n", reservationId, user);
      }

      /* verify funds */
      getUserBalanceStmt.setString(1, user);
      ResultSet r = getUserBalanceStmt.executeQuery();
      r.next();
      int userBalance = r.getInt("balance");
      
      getFlightPriceStmt.setInt(1, resv.getInt("fid1"));
      r = getFlightPriceStmt.executeQuery();
      r.next();
      int resvprice = r.getInt("price");

      getFlightPriceStmt.setInt(1, resv.getInt("fid2"));
      if (!resv.wasNull()) {
        r = getFlightPriceStmt.executeQuery();
        r.next();
        resvprice += r.getInt("price");
      }

      if (userBalance < resvprice) {
        conn.rollback();
        return String.format("User has only %d in account but itinerary costs %d\n", userBalance, resvprice);
      }

      /* update funds */
      payReservationStmt.setInt(1, reservationId);
      payReservationStmt.executeUpdate();
      updateUserBalanceStmt.setInt(1, userBalance - resvprice);
      updateUserBalanceStmt.setString(2, user);
      updateUserBalanceStmt.executeUpdate();
      conn.commit();
      return String.format("Paid reservation: %d remaining balance: %d\n", reservationId, userBalance-resvprice);
    } catch (Exception e) {
      try {
      conn.rollback();
      conn.setAutoCommit(true);
      } catch (Exception e2) {System.err.println("uh oh");}
      return "Failed to pay for reservation " + reservationId + "\n";
    }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    if (user.equals("")) {
      return "Cannot view reservations, not logged in\n";
    }
    try {
      getReservationsForUserStmt2.setString(1, user);
      ResultSet r = getReservationsForUserStmt2.executeQuery();
      if (!r.next()) return "No reservations found\n"; 
      StringBuffer sb = new StringBuffer();
      do {
        Flight f1 = new Flight(
          r.getInt("fid1"), 
          r.getInt("day_of_month"), 
          r.getString("car1"), 
          r.getString("fnum1"), 
          r.getString("origin1"), 
          r.getString("dest1"), 
          r.getInt("time1"),
          r.getInt("cap1"), 
          r.getInt("pri1"));

        Flight f2 = null;
        int fid2 = r.getInt("fid2");
        if (!r.wasNull()) {
          f2 = new Flight(
          fid2, 
          r.getInt("day_of_month"), 
          r.getString("car2"), 
          r.getString("fnum2"), 
          r.getString("origin2"), 
          r.getString("dest2"), 
          r.getInt("time2"),
          r.getInt("cap2"), 
          r.getInt("pri2"));
        }

        sb.append(String.format("Reservation %d paid: %s:\n", r.getInt("rid"), r.getInt("paid") == 0 ? "false" : "true"));
        sb.append(f1.toString() + "\n");
        if (f2 != null) {
          sb.append(f2.toString() + "\n");
        }
      } while (r.next());
      return sb.toString();
    } catch (Exception e) {
      return "Failed to retrieve reservations\n";
    }
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
