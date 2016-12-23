/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige des Zustandes eines Joysticks
 */

package jkcemu.joystick;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.lang.*;


public class JoystickActionFld extends Component
{
  private static Polygon pgLeft  = null;
  private static Polygon pgRight = null;
  private static Polygon pgUp    = null;
  private static Polygon pgDown  = null;

  private boolean stateLeft;
  private boolean stateRight;
  private boolean stateUp;
  private boolean stateDown;
  private boolean stateButton1;
  private boolean stateButton2;


  public JoystickActionFld()
  {
    if( pgLeft == null ) {
      int[] x = { 0, 10, 10, 25, 25, 10, 10, 0 };
      int[] y = { 35, 25, 31, 31, 39, 39, 45, 35 };
      pgLeft = new Polygon( x, y, Math.min( x.length, y.length ) );
    }
    if( pgRight == null ) {
      int[] x = { 60, 70, 60, 60, 45, 45, 60, 60 };
      int[] y = { 25, 35, 45, 39, 39, 31, 31, 25 };
      pgRight = new Polygon( x, y, Math.min( x.length, y.length ) );
    }
    if( pgUp == null ) {
      int[] x = { 35, 45, 39, 39, 31, 31, 25, 35 };
      int[] y = { 0, 10, 10, 25, 25, 10, 10, 0 };
      pgUp = new Polygon( x, y, Math.min( x.length, y.length ) );
    }
    if( pgDown == null ) {
      int[] x = { 31, 39, 39, 45, 35, 25, 31, 31 };
      int[] y = { 45, 45, 60, 60, 70, 60, 60, 45 };
      pgDown = new Polygon( x, y, Math.min( x.length, y.length ) );
    }
    this.stateLeft    = false;
    this.stateRight   = false;
    this.stateUp      = false;
    this.stateDown    = false;
    this.stateButton1 = false;
    this.stateButton2 = false;
    setPreferredSize( new Dimension( 71, 106 ) );
  }


  public int getJoystickAction( int x, int y )
  {
    int rv = 0;
    if( pgLeft.contains( x, y ) ) {
      rv |= JoystickThread.LEFT_MASK;
    } else if( pgRight.contains( x, y ) ) {
      rv |= JoystickThread.RIGHT_MASK;
    } else if( pgUp.contains( x, y ) ) {
      rv |= JoystickThread.UP_MASK;
    } else if( pgDown.contains( x, y ) ) {
      rv |= JoystickThread.DOWN_MASK;
    } else if( isInCircle( 20, 90, 10, x, y ) ) {
      rv |= JoystickThread.BUTTON1_MASK;
    } else if( isInCircle( 50, 90, 10, x, y ) ) {
      rv |= JoystickThread.BUTTON2_MASK;
    }
    return rv;
  }


  public void setJoystickAction( int actionMask )
  {
    this.stateLeft    = ((actionMask & JoystickThread.LEFT_MASK) != 0);
    this.stateRight   = ((actionMask & JoystickThread.RIGHT_MASK) != 0);
    this.stateUp      = ((actionMask & JoystickThread.UP_MASK) != 0);
    this.stateDown    = ((actionMask & JoystickThread.DOWN_MASK) != 0);
    this.stateButton1 = ((actionMask & JoystickThread.BUTTON1_MASK) != 0);
    this.stateButton2 = ((actionMask & JoystickThread.BUTTON2_MASK) != 0);
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void paint( Graphics g )
  {
    boolean enabled     = isEnabled();
    Color   colorBorder = (enabled ? Color.black : Color.gray);

    g = g.create();

    // Hoch
    g.setColor( enabled && this.stateUp ?  Color.red : Color.lightGray );
    g.fillPolygon( pgUp );
    g.setColor( colorBorder );
    g.drawPolygon( pgUp );

    // Links
    g.setColor( enabled && this.stateLeft ?  Color.red : Color.lightGray );
    g.fillPolygon( pgLeft );
    g.setColor( colorBorder );
    g.drawPolygon( pgLeft );

    // Rechts
    g.setColor( enabled && this.stateRight ?  Color.red : Color.lightGray );
    g.fillPolygon( pgRight );
    g.setColor( colorBorder );
    g.drawPolygon( pgRight );

    // Runter
    g.setColor( enabled && this.stateDown ?  Color.red : Color.lightGray );
    g.fillPolygon( pgDown );
    g.setColor( colorBorder );
    g.drawPolygon( pgDown );

    // Aktionsknopf 1
    g.setColor( enabled && this.stateButton1 ?  Color.red : Color.lightGray );
    g.fillOval( 10, 80, 20, 20 );
    g.setColor( colorBorder );
    g.drawOval( 10, 80, 20, 20 );

    // Aktionsknopf 2
    g.setColor( enabled && this.stateButton2 ?  Color.red : Color.lightGray );
    g.fillOval( 40, 80, 20, 20 );
    g.setColor( colorBorder );
    g.drawOval( 40, 80, 20, 20 );

    g.dispose();
  }


	/* --- private Methoden --- */

  private static boolean isInCircle( int xm, int ym, int r, int xp, int yp )
  {
    // Abstand vom Kreismittelpunkt berechnen und mit Radius vergleichen
    int dx = Math.abs( xm - xp );
    int dy = Math.abs( ym - yp );
    return Math.sqrt( (double) ((dx * dx) + (dy * dy)) ) <= (double) r;
  }
}
