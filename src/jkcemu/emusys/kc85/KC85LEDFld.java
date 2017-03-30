/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige der KC85-LEDs
 */

package jkcemu.emusys.kc85;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.lang.*;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.EmuThread;
import jkcemu.emusys.KC85;


public class KC85LEDFld extends AbstractScreenDevice
{
  private static final String TEXT_SYSTEM = "SYSTEM";

  private KC85  kc85;
  private int   kcTypeNum;
  private int   pioAValue;
  private Color greenOn;
  private Color greenOff;
  private Color yellowOn;
  private Color yellowOff;
  private Font  fontTitle;
  private Font  fontMem;


  public KC85LEDFld( KC85 kc85, Properties props )
  {
    super( props );
    this.kc85      = kc85;
    this.kcTypeNum = kc85.getKCTypeNum();
    this.pioAValue = 0;
    this.greenOn   = Color.GREEN;
    this.greenOff  = new Color( 60, 120, 60 );
    this.yellowOn  = Color.YELLOW;
    this.yellowOff = new Color( 120, 120, 0 );
    this.fontTitle = new Font( Font.SANS_SERIF, Font.PLAIN, 12 );
    this.fontMem   = new Font( Font.SANS_SERIF, Font.PLAIN, 10 );
  }


  public void setPioAValue( int value )
  {
    value &= (this.kcTypeNum >= 4 ? 0x20 : 0x27);
    if( value != this.pioAValue ) {
      this.pioAValue = value;
      setScreenDirty( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public EmuThread getEmuThread()
  {
    return this.kc85.getEmuThread();
  }


  @Override
  public int getScreenHeight()
  {
    return 75;
  }


  @Override
  public int getScreenWidth()
  {
    return this.kcTypeNum >= 4 ? 135 : 215;
  }


  @Override
  public String getTitle()
  {
    return String.format(
		"%s: %s LED%s",
		Main.APPNAME,
		this.kc85.getTitle(),
		this.kcTypeNum >= 4 ? "" : "s" );
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    int w = getScreenWidth();
    int h = getScreenHeight();
    g.translate( x, y );
    if( screenScale > 1 ) {
      if( g instanceof Graphics2D ) {
	((Graphics2D) g).scale( (double) screenScale, (double) screenScale );
      }
    }
    g.setColor( new Color( 225, 225, 225 ) );
    g.fillRect( 0, 0, w, h );
    g.setColor( Color.BLACK );
    g.setFont( this.fontTitle );
    if( this.kcTypeNum >= 4 ) {
      int         wText = 50;
      FontMetrics fm    = g.getFontMetrics();
      if( fm != null ) {
	wText = fm.stringWidth( TEXT_SYSTEM );
      }
      g.drawString( TEXT_SYSTEM, 65, 58 );
      g.drawLine( 20, 20, 60, 60 );
      g.drawLine( 60, 60, 65 + wText, 60 );
      paintLED(
	g,
	20,
	20,
	(this.pioAValue & 0x20) != 0 ? this.yellowOn : this.yellowOff );
    } else {
      g.fillRect( 0, 20, w, 5 );
      g.fillRect( 50, 0, 5, h );
      g.fillRect( 105, 25, 5, h );
      g.fillRect( 160, 25, 5, h );
      drawCenter( g, 0, 15, 50, "TAPE" );
      drawCenter( g, 55, 15, 160, "MEMORYSELECTION" );
      g.setFont( this.fontMem );
      drawCenter( g, 55, 72, 50, "ROM" );
      drawCenter( g, 110, 72, 50, "RAM" );
      drawCenter( g, 165, 72, 50, "IRM" );
      paintLED(
	g,
	25,
	50,
	(this.pioAValue & 0x20) != 0 ? this.yellowOn : this.yellowOff );
      paintLED(
	g,
	80,
	50,
	(this.pioAValue & 0x01) != 0 ? this.greenOn : this.greenOff );
      paintLED(
	g,
	135,
	50,
	(this.pioAValue & 0x02) != 0 ? this.greenOn : this.greenOff );
      paintLED(
	g,
	190,
	50,
	(this.pioAValue & 0x04) != 0 ? this.greenOn : this.greenOff );
    }
    return true;
  }


	/* --- private Methoden --- */

  private void drawCenter( Graphics g, int x, int y, int w, String text )
  {
    FontMetrics fm = g.getFontMetrics();
    if( fm != null ) {
      g.drawString( text, x + ((w - fm.stringWidth( text )) / 2), y );
    }
  }


  private static void paintLED( Graphics g, int x, int y, Color color )
  {
    g.setColor( Color.BLACK );
    g.fillOval( x - 8, y - 8, 16, 16 );
    g.setColor( color );
    g.fillOval( x - 5, y - 5, 10, 10 );
  }
}
