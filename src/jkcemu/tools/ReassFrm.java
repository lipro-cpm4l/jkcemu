/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Reassembler
 */

package jkcemu.tools;

import java.awt.Frame;
import java.awt.event.*;
import java.lang.*;
import javax.swing.*;
import jkcemu.base.BasicDlg;
import z80emu.*;


public class ReassFrm extends AbstractMemAreaFrm
{
  private static final int COL_NAME = 19;
  private static final int COL_ARG  = 27;


  public ReassFrm( Z80MemView memory )
  {
    super(
	memory,
	"JKCEMU Reassembler",
	"Reassemblieren",
	KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK ),
	40 );
    
  }


	/* --- ueberschriebene Methoden --- */

  protected void doAction()
  {
    try {
      int    addr    = getBegAddr();
      int    endAddr = addr;
      String text    = getEndAddrText();
      if( text != null ) {
	if( text.length() > 0 )
	  endAddr = getEndAddr();
      }
      StringBuilder buf = new StringBuilder( 0x4000 );
      while( addr <= endAddr ) {
	int begOfLine = buf.length();
	buf.append( String.format( "%04X", addr ) );
	addr = reassInstr( buf, begOfLine, addr );
	buf.append( (char) '\n' );
      }
      setResult( buf.toString() );
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


	/* --- private Methoden --- */

  private void appendCode( StringBuilder buf, int addr, int len )
  {
    buf.append( (char) '\u0020' );
    for( int i = 0; i < len; i++ ) {
      buf.append( (char) '\u0020' );
      buf.append( String.format( "%02X", this.memory.getMemByte( addr++ ) ) );
    }
  }


  private static void appendSpaces( StringBuilder buf, int endPos )
  {
    for( int i = buf.length(); i < endPos; i++ )
      buf.append( (char) '\u0020' );
  }


  private int reassInstr( StringBuilder buf, int begOfLine, int addr )
  {
    Z80ReassInstr instr = Z80Reassembler.reassInstruction(
				addr,
				this.memory.getMemByte( addr ),
				this.memory.getMemByte( addr + 1 ),
				this.memory.getMemByte( addr + 2 ),
				this.memory.getMemByte( addr + 3 ) );
    if( instr != null ) {
      int len = instr.getLength();
      appendCode( buf, addr, len );

      String s = instr.getName();
      if( s != null ) {
	appendSpaces( buf, begOfLine + COL_NAME );
	buf.append( s );

	s = instr.getArg1();
	if( s != null ) {
	  appendSpaces( buf, begOfLine + COL_ARG );
	  buf.append( s );

	  s = instr.getArg2();
	  if( s != null ) {
	    buf.append( (char) ',' );
	    buf.append( s );
	  }
	}
      }
      addr += len;
    } else {
      appendCode( buf, addr, 1 );
      addr++;
    }
    return addr;
  }
}

