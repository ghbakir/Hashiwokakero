package de.fastray;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.os.Vibrator;

import static junit.framework.Assert.*;

import de.fastray.BoardState;

public class BoardView extends View {
  private BoardState board_state;
  final private Paint background = new Paint();
  final private Paint cell_lines = new Paint();
  final private Paint text_paint = new Paint();
  final private Paint text_paint_done = new Paint();
  final private Paint bridge_paint = new Paint();

  private PixelPosition[][] positions;

  private boolean drawing_bridge = false;
  private boolean live_bridge_legal = false;
  private PixelPosition start_candidate = null;
  private PixelPosition end_candidate = null;

  private float end_point_x = 0;
  private float end_point_y = 0;

  private final Bitmap default_icon = BitmapFactory.decodeResource(this.getResources(),R.drawable.yellowicon);
  private final Bitmap selected_icon = BitmapFactory.decodeResource(this.getResources(),R.drawable.greenicon);
  private final Bitmap done_icon = BitmapFactory.decodeResource(this.getResources(),R.drawable.greenicon);

  private final float MAX_DISTANCE_FOR_SELECTION = 20;


  class PixelPosition {
    public float x;
    public float y;
    public PixelPosition(float x, float y) {
      this.x = x;
      this.y = y;
    }
    public BoardElement element_reference = null;
    public boolean is_selected = false;
  };

  public BoardView(Context context, BoardState state) {
    super(context);
    setFocusable(true);
    setFocusableInTouchMode(true);
    board_state = state;
    cell_lines.setColor(0xff444444);
    text_paint.setColor(0xff00003f);
    text_paint.setTextSize(20);
    text_paint_done.setColor(0xffff003f);
    text_paint_done.setTextSize(20);
    bridge_paint.setStrokeWidth(5);
    bridge_paint.setColor(0xff000000);
    Reset();
  }

  public void Reset() {
    int board_width = board_state.getCurrentState().board_width;
    positions  = new PixelPosition[board_width][board_width];
    for (int row = 0; row < board_width; ++row) {
      for (int col= 0; col < board_width; ++col) {
	positions[row][col] = new PixelPosition(0, 0);
	GetCellCenter(row, col,positions[row][col]);
	positions[row][col].element_reference =
          board_state.getCurrentState().board_elements[row][col];
      }
    }
  }

  @Override
  protected void onSizeChanged(int newx, int newy, int oldx, int oldy) {
    Reset();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    final int board_width = board_state.getCurrentState().board_width;
    // Draw the background...
    background.setColor(getResources().getColor(
          R.color.background));
    canvas.drawRect(0, 0, getWidth(), getHeight(), background);

    // We draw a grid of the size of the board.
    for (int i = 0; i < board_width; ++i) {
      PixelPosition p0 = positions[i][0];
      PixelPosition p1 = positions[i][board_width - 1];
      canvas.drawLine(p0.x, p0.y, p1.x,p1.y, cell_lines);
      p0 = positions[0][i];
      p1 = positions[board_width - 1][i];
      canvas.drawLine(p0.x, p0.y, p1.x,p1.y, cell_lines);
    }

    // Draw accepted bridges.
    // From top to bottom and left to right.
    // We exploit symmetry.
    for (int row = 0; row < board_width; ++row) {
      for (int col= 0; col < board_width; ++col) {
        final String logmarker = getClass().getName() + " " + row + " " + col;;
        BoardElement elt = null;
        elt = board_state.getCurrentState().board_elements[row][col];
        assertNotNull(logmarker + ": null element", elt);
        if (elt.is_island) {
          if (elt.connecting_east != null) {
            assertNotNull(logmarker + ": connecting east destination is null", elt.connecting_east.destination);
            PaintBridge(canvas, elt, elt.connecting_east.destination,elt.connecting_east.second);
          }
          if (elt.connecting_south!= null) {
            assertNotNull(logmarker + ": connecting south destination is null", elt.connecting_south.destination);
            PaintBridge(canvas, elt, elt.connecting_south.destination,elt.connecting_south.second);
          }
        }
      }
    }

    // We draw live bridge lines if needed.
    if (drawing_bridge) {
      canvas.drawLine(start_candidate.x, start_candidate.y,
          end_point_x, end_point_y, bridge_paint);
    }

    // On top of this we draw possibly two sets of icons.
    // The not selected numbers and the selected ones.
    for (int row = 0; row < board_width; ++row) {
      for (int col= 0; col < board_width; ++col) {
        if (board_state.getCurrentState().board_elements[row][col].is_island) {
          PixelPosition p = positions[row][col];
          Bitmap icon = null;
          if (p.is_selected) {
            icon = selected_icon;
          } else {
            icon = default_icon;
          }
          int max_connecting_bridges =
            board_state.getCurrentState().board_elements[row][col].max_connecting_bridges;
          int count =
            board_state.getCurrentState().board_elements[row][col].GetCurrentCount();
          PaintNumber(canvas, p, max_connecting_bridges, count == max_connecting_bridges, icon);
        }
      }
    }

  }

  private Rect paint_source = new Rect();
  private Rect paint_destination = new Rect();

  private void PaintNumber(Canvas canvas, PixelPosition p, int number, boolean done, Bitmap icon) {
    float x0 = p.x;
    float y0 = p.y;
    float x = p.x - text_paint.getTextSize() / 2 + 4;
    float y = p.y + text_paint.getTextSize() / 2 - 2;

    paint_source.left = 0;
    paint_source.right = icon.getWidth();
    paint_source.top = 0;
    paint_source.bottom = icon.getHeight();
    paint_destination.left = (int) (x0 - 12);
    paint_destination.top = (int) (y0 - 12);
    paint_destination.right= (int) (x0 + 12);
    paint_destination.bottom= (int) (y0 + 12);
    if (!done)
      canvas.drawBitmap(icon, paint_source, paint_destination, text_paint);
    else
      canvas.drawBitmap(done_icon, paint_source, paint_destination, text_paint);
    canvas.drawText(String.format("%d", number), x, y, text_paint);
  }

  private void PaintBridge(Canvas canvas, BoardElement start, BoardElement end, int bridges) {
    float startX = positions[start.row][start.col].x;
    float startY = positions[start.row][start.col].y;

    float endX = positions[end.row][end.col].x;
    float endY = positions[end.row][end.col].y;
    float linewidth = bridge_paint.getStrokeWidth();
    if (bridges == 1) {
      canvas.drawLine(startX, startY, endX, endY, bridge_paint);
    } else if (bridges == 2) {
      if (startY == endY) {
	canvas.drawLine(startX, startY - linewidth - 2, endX, endY - linewidth -2, bridge_paint);
	canvas.drawLine(startX, startY + linewidth + 2, endX, endY + linewidth + 2, bridge_paint);
      } else {
	canvas.drawLine(startX - linewidth - 2, startY, endX - linewidth - 2, endY, bridge_paint);
	canvas.drawLine(startX + linewidth + 2, startY, endX + linewidth +2, endY, bridge_paint);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN ||
        event.getAction() == MotionEvent.ACTION_MOVE ||
        event.getAction() == MotionEvent.ACTION_UP)  {
      float x =  event.getX();
      float y = event.getY();
      end_point_x = x;
      end_point_y = y;
      PixelPosition candidate = null;
      switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          Log.d(getClass().getName(), "ACTION DOWN");
          candidate =  TrySelect(x, y);
          if (candidate != null) {
            drawing_bridge = true;
            candidate.is_selected = true;
            start_candidate = candidate;
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(50L);
            return true;
          }

          start_candidate = null;

          break;
        case MotionEvent.ACTION_MOVE:
          if (drawing_bridge) {
            float dx = start_candidate.x - x;
            float dy = start_candidate.y - y;
            candidate = TrySelect(x,y);
            if (candidate != null && candidate != start_candidate) {
              // Allow only non-diagonal neighbors.
              if (candidate.x == start_candidate.x ||
                  candidate.y == start_candidate.y) {
                candidate.is_selected = true;
              }
              if (candidate != end_candidate && end_candidate != null) {
                end_candidate.is_selected = false;
              }
              if (end_candidate != candidate) {
                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50L);
              }
              end_candidate = candidate;
            } else {
              if (end_candidate != null) {
                end_candidate.is_selected = false;
              }

              end_candidate = null;
            }
          }
          break;
        case MotionEvent.ACTION_UP:
          Log.d(getClass().getName(), "ACTION UP");

          if (start_candidate == null)
            return false;

          drawing_bridge = false;
          candidate = TrySelect(x, y);
          boolean result = false;
          if (candidate != null) {
            BoardElement start_elt = start_candidate.element_reference;
            BoardElement end_elt = candidate.element_reference;
            result = board_state.TryAddNewBridge(start_elt, end_elt, 1);
          }

          // Cleanup
          start_candidate.is_selected = false;
          start_candidate = null;
          if (end_candidate != null) {
            end_candidate.is_selected = false;
            end_candidate = null;
          }

          if (result) {
            CheckEndCondition();
          }
      }
      invalidate();
      return true;
    }
    return super.onTouchEvent(event);
  }

  void CheckEndCondition() {
  }

  private PixelPosition TrySelect(float x, float y) {
    for (int i = 0; i < positions.length; ++i) {
      for (int j = 0; j < positions[i].length; ++j) {
	double sq_distance = Math.sqrt((positions[i][j].x - x) * (positions[i][j].x - x) +
	    (positions[i][j].y - y) * (positions[i][j].y - y));
	if (sq_distance < MAX_DISTANCE_FOR_SELECTION &&
            positions[i][j].element_reference != null &&
            positions[i][j].element_reference.is_island) {
          Log.d(getClass().getName(),
              String.format("Choosing %d %d",
                positions[i][j].element_reference.row,
                positions[i][j].element_reference.col));
          return positions[i][j];
	}
      }
    }
    return null;
  }

  private final void GetCellCenter(int board_row, int board_column, PixelPosition result) {
    final int board_width = board_state.getCurrentState().board_width;
    final int grid_start = 5;
    final int cell_width = (getWidth() - 2 * grid_start) / board_width;
    final int cell_height = (getHeight() - 2 * grid_start) / board_width;
    float x = grid_start + board_column * cell_width + cell_width / 2;
    float y = grid_start + board_row * cell_height  + cell_height / 2;
    result.x = x;
    result.y = y;
  }

}
