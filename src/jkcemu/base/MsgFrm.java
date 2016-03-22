/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Ausgabe von Meldungen
 */

package jkcemu.base;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class MsgFrm extends BasicFrm
{
  private static final int DEFAULT_WIDHTH = 500;
  private static final int DEFAULT_HEIGHT = 120;

  private boolean   frameEmpty;
  private JTextArea fldText;


  public MsgFrm( Window owner )
  {
    setTitle( "JKCEMU Meldungen" );
    Main.updIcon( this );
    this.frameEmpty = true;

    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.fldText = new JTextArea();
    this.fldText.setEditable( false );
    this.fldText.setMargin( new Insets( 5, 5, 5, 5 ) );
    add( new JScrollPane( this.fldText ), BorderLayout.CENTER );

    Font font = this.fldText.getFont();
    if( font != null ) {
      this.fldText.setFont( font.deriveFont( Font.PLAIN ) );
    }


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      setSize( DEFAULT_WIDHTH, DEFAULT_HEIGHT );
      boolean done = false;
      if( owner != null ) {
	try {
	  Dimension pSize     = owner.getSize();
	  Point     pLocation = owner.getLocationOnScreen();
	  setLocation(
		pLocation.x + ((pSize.width - DEFAULT_WIDHTH) / 2),
		pLocation.y + ((pSize.height - DEFAULT_HEIGHT) / 2) );
	  done = true;
	}
	catch( Exception ex ) {}
      }
      if( !done ) {
	setLocationByPlatform( true );
      }
    }
    setResizable( true );
  }


  public void appendMsg( String text )
  {
    if( text != null ) {
      if( !text.isEmpty() ) {
	if( this.frameEmpty ) {
	  this.frameEmpty = false;
	} else {
	  this.fldText.append( "\n\n" );
	}
	this.fldText.append( text );
	this.fldText.append( "\n" );
      }
    }
  }
}
