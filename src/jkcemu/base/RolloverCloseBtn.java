/*
 * (c) 2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schaltflaeche mit einem diagonalen Kreuz und Rollover-Effekt
 */

package jkcemu.base;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.plaf.basic.BasicButtonUI;


public class RolloverCloseBtn extends JButton
{
  public static Stroke crossStroke = new BasicStroke(
						2F,
						BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND );


  public RolloverCloseBtn()
  {
    setBorderPainted( false );
    setContentAreaFilled( false );
    setFocusable( false );
    setRolloverEnabled( true );
    setUI( new BasicButtonUI() );

    final RolloverCloseBtn btn = this;
    addMouseListener(
		new MouseAdapter()
		{
		  @Override
		  public void mouseEntered( MouseEvent e )
		  {
		    if( btn.isEnabled() && btn.isRolloverEnabled() ) {
		      btn.setBorderPainted( true );
		    }
		  }

		  @Override
		  public void mouseExited( MouseEvent e )
		  {
		    if( btn.isEnabled() && btn.isRolloverEnabled() ) {
		      btn.setBorderPainted( false );
		    }
		  }
		} );
  }


  public static void paintBorder( Graphics g, int x, int y, int w, int h )
  {
    if( (w > 3) && (h > 3) ) {
      g.setColor( Color.LIGHT_GRAY );
      g.drawLine( x + 1,     y,         x + w - 2, y );
      g.drawLine( x + 1,     y + h - 2, x + w - 2, y + h - 2 );
      g.drawLine( x,         y + 1,     x,         y + h - 2 );
      g.drawLine( x + w - 2, y + 1,     x + w - 2, y + h - 2 );
    }
  }


  public static void paintComponent(
				Graphics g,
				int      x,
				int      y,
				int      w,
				int      h,
				boolean  enabled,
				boolean  pressed )
  {
    int m = Math.min( w, h ) - 6;
    if( m > 2 ) {
      x += ((w - m) / 2);
      y += ((h - m) / 2);
      if( enabled && pressed ) {
	x++;
	y++;
      }
      Graphics g1 = g.create();
      if( g1 instanceof Graphics2D ) {
	((Graphics2D) g1).setStroke( crossStroke );
      }
      g1.setColor( enabled ? Color.BLACK : Color.GRAY );
      g1.drawLine( x, y, x + m - 2, y + m - 2 );
      g1.drawLine( x, y + m - 2, x + m - 2, y );
      g1.dispose();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void paintBorder( Graphics g )
  {
    if( isBorderPainted() ) {
      paintBorder( g, 0, 0, getWidth(), getHeight() );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    super.paintComponent( g );
    paintComponent(
		g,
		0,
		0,
		getWidth(),
		getHeight(),
		isEnabled(),
		getModel().isPressed() && isBorderPainted() );
  }


  @Override
  public void updateUI()
  {
    // Schaltflaeche soll unabhaengig vom Erscheinungsbild sein
  }
}
