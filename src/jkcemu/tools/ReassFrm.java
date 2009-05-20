/*
 * (c) 2008-2009 Jens Mueller
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
import jkcemu.base.*;
import z80emu.*;


public class ReassFrm extends AbstractMemAreaFrm
{
  private static final int COL_MNEMONIC = 23;
  private static final int COL_ARGS     = 31;
  private static final int COL_REMARK   = 49;

  private EmuThread emuThread;


  public ReassFrm( EmuThread emuThread )
  {
    super(
	emuThread,
	"JKCEMU Reassembler",
	"Reassemblieren",
	KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK ),
	58 );
    this.emuThread = emuThread;
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
	int len = this.emuThread.getEmuSys().reassembleSysCall(
					addr,
					buf,
					COL_MNEMONIC,
					COL_ARGS,
					COL_REMARK );
	if( len > 0 ) {
	  addr += len;
	} else {
	  int begOfLine = buf.length();
	  buf.append( String.format( "%04X", addr ) );

	  int b0        = this.memory.getMemByte( addr, true );
	  boolean b1_m1 = ((b0 == 0xED) || (b0 == 0xDD) || (b0 == 0xFD));

	  Z80ReassInstr instruction = Z80Reassembler.reassInstruction(
				addr,
				b0,
				this.memory.getMemByte( addr + 1, b1_m1 ),
				this.memory.getMemByte( addr + 2, false ),
				this.memory.getMemByte( addr + 3, false ) );
	  if( instruction != null ) {
	    buf.append( (char) '\u0020' );
	    len = instruction.getLength();
	    for( int i = 0; i < len; i++ ) {
	      buf.append( (char) '\u0020' );
	      buf.append( String.format(
				"%02X",
				this.emuThread.getMemByte(
					addr++,
					(i == 0) || ((i == 1) && b1_m1) ) ) );
	    }

	    String s = instruction.getName();
	    if( s != null ) {
	      appendSpaces( buf, begOfLine + COL_MNEMONIC );
	      buf.append( s );

	      s = instruction.getArg1();
	      if( s != null ) {
		appendSpaces( buf, begOfLine + COL_ARGS );
		buf.append( s );

		s = instruction.getArg2();
		if( s != null ) {
		  buf.append( (char) ',' );
		  buf.append( s );
		}
	      }
	    }
	  } else {
	    buf.append( String.format(
				"  %02X",
				this.emuThread.getMemByte( addr++, true ) ) );
	  }
	  buf.append( (char) '\n' );
	}
      }
      setResult( buf.toString() );
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


	/* --- private Methoden --- */

  private static void appendSpaces( StringBuilder buf, int endPos )
  {
    for( int i = buf.length(); i < endPos; i++ )
      buf.append( (char) '\u0020' );
  }
}

