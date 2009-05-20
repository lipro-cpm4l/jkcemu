/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Anzeige des Speicherinhaltes
 */

package jkcemu.tools;

import java.awt.Frame;
import java.awt.event.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.BasicDlg;
import z80emu.Z80MemView;


public class MemViewFrm extends AbstractMemAreaFrm
{
  public MemViewFrm( Z80MemView memory )
  {
    super(
	memory,
	"JKCEMU Speicher",
	"Aktualisieren",
	KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK ),
	76 );
  }


	/* --- ueberschriebene Methoden --- */

  protected void doAction()
  {
    try {
      int addr    = getBegAddr();
      int endAddr = getEndAddr();

      int bufSize = (((endAddr - addr) + 15) / 16) * 74;
      StringBuilder buf = new StringBuilder( bufSize > 0 ? bufSize : 16 );

      while( addr <= endAddr ) {
	buf.append( String.format( "%04X  ", addr ) );
	int rowAddr = addr;
	for( int i = 0; i < 16; i++ ) {
	  if( addr <= endAddr ) {
	    buf.append( String.format(
				" %02X",
				this.memory.getMemByte( addr++, false ) ) );
	  } else {
	    buf.append( "   " );
	  }
	}
	buf.append( "   " );
	addr = rowAddr;
	for( int i = 0; i < 16; i++ ) {
	  if( addr <= endAddr ) {
	    int ch = this.memory.getMemByte( addr++, false );
	    if( (ch >= '\u0020') && (ch < 0x7F) ) {
	      buf.append( (char) ch );
	    } else {
	      buf.append( (char) '.' );
	    }
	  }
	}
	buf.append( (char) '\n' );
      }
      setResult( buf.toString() );
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }
}

