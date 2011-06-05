// The purpose of this class is to model the state of the hashi board.
// It can initialize the state given on size and difficulty level
// and provides functionality to check whether the state is valid or not.

package de.fastray;

import de.fastray.BoardElement;
import de.fastray.Connection;

import android.util.Log;
import java.util.Random;
import java.util.StringTokenizer;
import static junit.framework.Assert.*;


public class BoardState {
  // This class member is used for random initialization purposes.
  static private final Random random = new Random();

  // The difficulty levels.
  static public final int EASY = 0;
  static public final int MEDIUM = 1;
  static public final int HARD = 2;

  static public final int EMPTY = 0;

  static int ConnectionFingerprint(BoardElement start, BoardElement end) {
    int x = start.row * 100 + start.col;
    int y = end.row * 100 + end.col;
    // Swap to get always the same fingerprint independent whether we are called
    // start-end or end-start
    if (x > y ) {
      int temp = x;
      x = y;
      y = temp;
    }
    Log.d("", String.format("%d %d" , x ,y));
    return x ^ y;
  }

  public class State {
    // The elements of the board are stored in this array.
    // A value defined by "EMPTY" means that its not set yet.
    // The board layout is given by
    // board_elements[0][0] = top left corner
    // board_elements[0][board_width - 1] = top right corner
    // board_elements[board_width - 1][0] = lower left corner
    // board_elements[board_width - 1][board_width - 1] = lower right corner
    public BoardElement [][] board_elements = null;
    
    public int [][] cell_occupied = null;

    // The width of the board. We only assume squared boards.
    public int board_width=0;


    public State(int width) {
      board_width = width;
      board_elements = new BoardElement[width][width];
      cell_occupied = new int[width][width];
    }

    public State CloneWithoutConnections() {
      State newstate = new State(board_width);
      if (board_elements != null) {
	newstate.board_elements = new BoardElement[board_elements.length][board_elements.length];
	for (int i = 0; i < board_elements.length; ++i) {
	  for (int j = 0; j < board_elements.length; ++j) {
	    if (board_elements[i][j] == null)
              continue;
	    newstate.board_elements[i][j] = board_elements[i][j].clone();
	  }
	}
      }
      if (cell_occupied != null) {
	newstate.cell_occupied = new int[board_elements.length][board_elements.length];
	for (int i = 0; i < board_elements.length; ++i) {
	  for (int j = 0; j < board_elements.length; ++j) {
	    newstate.cell_occupied[i][j] = cell_occupied[i][j];
	  }
	}
      }
      return newstate;
    }

    public void AddToBridgeCache(BoardElement first, BoardElement second) {
      if (first == null || second == null) { return; }
      final int fingerprint = ConnectionFingerprint(first, second);
      Log.d(getClass().getName(),
          String.format("Fingerprint of this bridge %d", fingerprint));
      // mark the end points as occupied.
      cell_occupied[first.row][first.col] = fingerprint;
      cell_occupied[second.row][second.col] = fingerprint;

      int dcol = second.col - first.col;
      int drow = second.row - first.row;

      if (first.row == second.row) {
	for (int i = (int) (first.col + Math.signum(dcol)); i != second.col; i += Math.signum(dcol)) {
	  cell_occupied[first.row][i] = fingerprint;
	}
      } else {
	assert first.col == second.col;
	for (int i = (int) (first.row + Math.signum(drow)); i != second.row; i+= Math.signum(drow)) {
	  cell_occupied[i][first.col] = fingerprint;
	}
      }
    }
  }; // end of state

  private State current_state, old_state;

  static private final int WIDTH_EASY = 7;

  public void NewGame(int hardness) {
    switch(hardness) {
      case EASY:
	Log.d(getClass().getName(), "Initializing new easy game");
	InitializeEasy();
	old_state = getCurrentState().CloneWithoutConnections();
	break;
    }
  }

  public void ResetGame() {
    if (old_state != null) {
      Log.d(getClass().getName(), "Setting board_elements to old_elements");
      setCurrentState(old_state.CloneWithoutConnections());
    } else {
      Log.d(getClass().getName(), "old_lements are zero");
    }
  }

  public BoardState(int hardness) {
    NewGame(hardness);
  }

  public boolean TryAddNewBridge(BoardElement start, BoardElement end, int count) {
    assertEquals(count, 1);
    assert (start != null);
    assert (end != null);
    final int fingerprint = ConnectionFingerprint(start, end);

    Log.d(getClass().getName(),
	String.format("considering (%d,%d) and (%d,%d)", start.row,start.col, end.row,end.col));
    if (start.row == end.row && start.col == end.col) {
      Log.d(getClass().getName(), "Same nodes selected!");
      return false;
    }
    assert count > 0;

    int dcol = end.col - start.col;
    int drow = end.row - start.row;

    // It must be a vertical or horizontal bridge:
    if (Math.abs(dcol) > 0 && Math.abs(drow) > 0) {
      Log.d(getClass().getName(), "Not a pure horizontal or vertical bridge.");
      return false;
    }

    // First we check whether start and end elements can take the specified bridge counts.
    int count_start = start.GetCurrentCount();
    int count_end = end.GetCurrentCount();

    if (count_start  + count > start.max_connecting_bridges ||
	count_end + count > end.max_connecting_bridges) {
      Log.d(getClass().getName(), "Sums on start or end would be too large.");
      return false;
    }

    Log.d(getClass().getName(),
     String.format("Sums:%d @ (%d,%d)  and %d @ (%d,%d)",
       count_start, start.row, start.col,
       count_end, end.row, end.col));

    Connection start_connection = null;
    Connection end_connection = null;

    // Next we check whether we are crossing any lines.
    if (start.row == end.row) {
      for (int i = (int) (start.col + Math.signum(dcol)); i != end.col; i += Math.signum(dcol)) {
	if (getCurrentState().cell_occupied[start.row][i] > 0 &&
            getCurrentState().cell_occupied[start.row][i] != fingerprint) {
	  Log.d(getClass().getName(), "Crossing an occupied cell.");
	  return false;
	}
      }
      assert start.col != end.col;
      if (start.col > end.col) {
	start.connecting_east = GetOrCreateConnection(end, start.connecting_east);
	end.connecting_west = GetOrCreateConnection(start, end.connecting_west);
	start_connection = start.connecting_east;
	end_connection = end.connecting_west;
      } else {
	start.connecting_west = GetOrCreateConnection(end, start.connecting_west);
	end.connecting_east = GetOrCreateConnection(start, end.connecting_east);
	start_connection = start.connecting_west;
	end_connection = end.connecting_east;
      }
    } else {
      assert start.col == end.col;
      for (int i = (int) (start.row + Math.signum(drow)); i != end.row ; i += Math.signum(drow)) {
	if (getCurrentState().cell_occupied[i][start.col] > 0 &&
            getCurrentState().cell_occupied[i][start.col] != fingerprint) {
	  Log.d(getClass().getName(), "Crossing an occupied cell.");
	  return false;
	}
      }
      if (start.row > end.row ) {
	start.connecting_north = GetOrCreateConnection(end, start.connecting_north);
	end.connecting_south = GetOrCreateConnection(start, end.connecting_south);
	start_connection = start.connecting_north;
	end_connection = end.connecting_south;
      } else {
	start.connecting_south= GetOrCreateConnection(end, start.connecting_south);
	end.connecting_north = GetOrCreateConnection(start, end.connecting_north);
	start_connection = start.connecting_south;
	end_connection = end.connecting_north;
      }
    }
    start_connection.destination = end;
    end_connection.destination = start;
    start_connection.second += count;
    end_connection.second += count;

    getCurrentState().AddToBridgeCache(start, end);

    Log.d(getClass().getName(),
        String.format("New bridge added. Sums:%d @ (%d,%d)  and %d @ (%d,%d)",
         count_start, start.row,start.col,
         count_end, end.row,end.col));
    return true;
  }

  private Connection GetOrCreateConnection(
      BoardElement end,
      Connection connection) {
    if (connection!= null) { return connection; }
    return new Connection(end, 0);
  }

  protected void InitializeEasy() {
    setCurrentState(new State(WIDTH_EASY));
    //    FillWithRandomPath();
    // Load some default
    String[] debug_board_state  = new String[7];
    debug_board_state[0] = "0,3,0,0,3,0,2";
    debug_board_state[1] = "1,0,2,0,0,1,2";
    debug_board_state[2] = "0,2,0,0,0,0,0";
    debug_board_state[3] = "0,0,3,0,3,0,4";
    debug_board_state[4] = "2,0,0,0,0,1,0";
    debug_board_state[5] = "0,1,0,0,1,0,2";
    debug_board_state[6] = "2,0,3,0,0,2,0";

    for (int i = 0; i < WIDTH_EASY; ++i) {
      StringTokenizer tokenizer = new StringTokenizer (debug_board_state[i], ",");
      int column = 0;
      while(tokenizer.hasMoreTokens()) {
	String token = tokenizer.nextToken();
	getCurrentState().board_elements[i][column] = new BoardElement();
	getCurrentState().board_elements[i][column].max_connecting_bridges = Integer.parseInt(token);
	getCurrentState().board_elements[i][column].row = i;
	getCurrentState().board_elements[i][column].col = column;

	if (getCurrentState().board_elements[i][column].max_connecting_bridges > 0) {
	  getCurrentState().board_elements[i][column].is_island = true;
	}
	++column;
      }
    }
  }

  public void setCurrentState(State new_state) {
    this.current_state = new_state;
  }

  public State getCurrentState() {
    return current_state;
  }
}

