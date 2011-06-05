package de.fastray;

import java.util.Vector;
import android.util.Log;

public class BoardElement {
  public int row = 0;
  public int col = 0;
  public int max_connecting_bridges = 0;
  public boolean is_island = false;

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

  public int GetCurrentCount() {
    int s = 0;
    if (connecting_east != null) {
      s += connecting_east.second;
    }
    if (connecting_south!= null) {
      s += connecting_south.second;
    }
    if (connecting_north != null) {
      s += connecting_north .second;
    }
    if (connecting_west != null) {
      s += connecting_west.second;
    }
    return s;
  }

  static class Connection {
    public BoardElement source;
    public BoardElement destination;
    public int second;
    public Connection() {
      source = null;
      second = 0;
    }
    public Connection(BoardElement elt, int val) {
      source = elt;
      second = val;
    }

    public Connection clone() {
      Connection c = new Connection();
      c.source = source;
      c.second = second;
      return c;
    }
  };

  public enum Direction {
    EAST, SOUTH, WEST, NORTH;
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
};
