/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Halte-/Log-Punkt auf eine Ausgabeadresse oder
 * auf einen Bereich von Ausgabeadressen
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class OutputBreakpoint extends AbstractBreakpoint
{
  private boolean is8Bit;
  private int     begPort;
  private int     endPort;
  private int     mask;
  private String  cond;
  private int     value;


  public OutputBreakpoint(
		DebugFrm debugFrm,
		boolean  is8Bit,
		int      begPort,
		int      endPort,
		String   cond,
		int      mask,
		int      value )
  {
    super( debugFrm );
    this.is8Bit  = is8Bit;
    this.begPort = begPort & 0xFFFF;
    this.endPort = (endPort >= 0 ? (endPort & 0xFFFF) : -1);
    this.mask    = mask & 0xFF;
    this.cond    = cond;
    this.value   = value & 0xFF;

    StringBuilder buf = new StringBuilder();
    if( this.is8Bit ) {
      this.begPort &= 0xFF;
      buf.append( String.format( "%02X", this.begPort ) );
      if( this.endPort >= 0 ) {
	this.endPort &= 0xFF;
	buf.append( String.format( "-%02X", this.endPort ) );
      }
    } else {
      buf.append( String.format( "%04X", this.begPort ) );
      if( this.endPort >= 0 ) {
	buf.append( String.format( "-%04X", this.endPort ) );
      }
    }
    if( this.cond != null ) {
      if( this.mask != 0xFF ) {
	buf.append(
		String.format(
			":(Wert&%02X)%s%02X",
			this.mask,
			this.cond,
			this.value ) );
      } else {
	buf.append(
		String.format(
			":Wert%s%02X",
			this.cond,
			this.value ) );
      }
    }
    setText( buf.toString() );
  }


  public boolean get8Bit()
  {
    return this.is8Bit;
  }


  public int getBegPort()
  {
    return this.begPort;
  }


  public String getCondition()
  {
    return this.cond;
  }


  public int getEndPort()
  {
    return this.endPort;
  }


  public int getMask()
  {
    return this.mask;
  }


  public int getValue()
  {
    return this.value;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean matchesImpl( Z80CPU cpu, Z80InterruptSource iSource )
  {
    boolean rv    = false;
    int     pc    = cpu.getRegPC();
    int     op0   = cpu.getMemByte( pc, false );
    int     op1   = cpu.getMemByte( pc + 1, false );
    int     port  = -1;
    int     value = -1;
    if( op0 == 0xD3 ) {			// OUT A
      int regA = cpu.getRegA();
      port     = (regA << 8) | op1;
      value    = regA;
    }
    else if( op0 == 0xED ) {
      switch( op1 ) {
	case 0x41:			// OUT (C),B
	  port  = cpu.getRegBC();
	  value = cpu.getRegB();
	  break;
	case 0x49:			// OUT (C),C
	  port  = cpu.getRegBC();
	  value = cpu.getRegC();
	  break;
	case 0x51:			// OUT (C),D
	  port  = cpu.getRegBC();
	  value = cpu.getRegD();
	  break;
	case 0x59:			// OUT (C),E
	  port  = cpu.getRegBC();
	  value = cpu.getRegE();
	  break;
	case 0x61:			// OUT (C),H
	  port  = cpu.getRegBC();
	  value = cpu.getRegH();
	  break;
	case 0x69:			// OUT (C),L
	  port  = cpu.getRegBC();
	  value = cpu.getRegL();
	  break;
	case 0x71:			// OUT (C),?
	  port  = cpu.getRegBC();
	  value = 0;
	  break;
	case 0x79:			// OUT (C),A
	  port  = cpu.getRegBC();
	  value = cpu.getRegA();
	  break;
	case 0xA3:			// OUTI
	case 0xAB:			// OUTD
	case 0xB3:			// OTIR
	case 0xBB:			// OTDR
	  port = cpu.getRegBC();
	  if( !this.is8Bit ) {
	    /*
	     * Bei den Blockausgabebefehlen wird das B-Register
	     * dekrementiert, bevor es auf den Adressbus gelegt wird.
	     */
	    port = ((port - 0x0100) & 0xFF00) | (port & 0x00FF);
	  }
	  value = cpu.getMemByte( cpu.getRegHL(), false );
	  break;
      }
    }
    if( port >= 0 ) {
      if( matchesPort( port ) ) {
	if( (this.cond != null) && (value >= 0) ) {
	  rv = checkValues( value & mask, this.cond, this.value );
	} else {
	  rv = true;
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean matchesPort( int port )
  {
    if( this.is8Bit ) {
      port &= 0x00FF;
    } else {
      port &= 0xFFFF;
    }
    return (port == this.begPort)
	   || ((port >= this.begPort) && (port <= this.endPort));
  }
}

