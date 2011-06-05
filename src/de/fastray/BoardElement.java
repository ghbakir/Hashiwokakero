package de.fastray;

import java.util.Vector;
import android.util.Log;
import de.fastray.Connection;

// This class holds information about a grid point.
// Every grid point does have
//   -- a coordinate = (row , col).
//   -- a state "is_island" whether it is an island or empty.
// If the grid point is an island then it furthermore contains
//   -- number of possible other islands it connects to.
//   -- possible connections to its neighbours N,S,E and W. 
public class BoardElement {
  // *********************************************************
  // *********************************************************
  // General members
  // *********************************************************
  // *********************************************************

  public int row = 0;
  public int col = 0;

  // Is this an island?
  public boolean is_island = false;

  // *********************************************************
  // *********************************************************
  // Island specific members.
  // *********************************************************
  // *********************************************************
  public int max_connecting_bridges = 0;

  // It is easier to refer to neighbours via directions.
  public enum Direction {
    EAST, SOUTH, WEST, NORTH;
  }

  // Pairs of a BoardElement and the number of connecting bridges
  public Connection connecting_north = null;
  public Connection connecting_south = null;
  public Connection connecting_east = null;
  public Connection connecting_west = null;

  public BoardElement clone() {
    BoardElement elt = new BoardElement();
    elt.row = row;
    elt.col = col;
    Log.i("BoardElemnet", "Cloning" + elt.row + " " + elt.col);

    elt.max_connecting_bridges = max_connecting_bridges;
    elt.is_island = is_island;
    if (connecting_east != null)
      elt.connecting_east = new Connection();
    else 
      elt.connecting_east = null;

    if (connecting_north!= null)
      elt.connecting_north = new Connection();
    else
      elt.connecting_north = null;

    if (connecting_south!= null)
      elt.connecting_south = new Connection();
    else
      elt.connecting_south = null;

    if (connecting_west != null)
      elt.connecting_west = new Connection();
    else 
      elt.connecting_west = null;

    return elt;
  }

  private int GetConnectionCount(Connection connection){
    if (connection == null) {
      return 0;
    } else {
      return connection.second;
    }
  }

  // Return the current count of connections connected
  // to this island.
  public int GetCurrentCount() {
    if (!is_island) {
      return 0;
    }
    int s = GetConnectionCount(connecting_east);
    s += GetConnectionCount(connecting_south);
    s += GetConnectionCount(connecting_north);
    s += GetConnectionCount(connecting_west);
    return s;
  }

  void AddConnection(Direction dir, BoardElement dest, int value) {
    Connection connection = null;
    switch (dir) {
      case EAST:
        connecting_east = new Connection(dest, value);
        connection = connecting_east;
        break;
      case WEST:
        connecting_west = new Connection(dest, value);
        connection = connecting_west;
        break;
      case SOUTH:
        connecting_south = new Connection(dest, value);
        connection = connecting_south;
        break;
      case NORTH:
        connecting_north = new Connection(dest, value);
        connection = connecting_north;
        break;
    }
  }
}; // BoardElement
