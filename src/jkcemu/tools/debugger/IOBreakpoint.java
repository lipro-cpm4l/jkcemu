/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Haltepunkte auf eine Programmadresse bzw. Adressbereich
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class IOBreakpoint extends AbstractBreakpoint
{
  private boolean is8Bit;
  private int     begPort;
  private int     endPort;
  private int     value;
  private int     mask;


  public IOBreakpoint(
		AccessMode accessMode,
		boolean    is8Bit,
		int        begPort,
		int        endPort,
		int        value,
		int        mask )
  {
    super( accessMode );
    this.is8Bit  = is8Bit;
    this.begPort = begPort & 0xFFFF;
    this.endPort = (endPort >= 0 ? (endPort & 0xFFFF) : -1);
    this.value   = (value >= 0 ? (value & 0xFF) : -1);
    this.mask    = mask & 0xFF;
    if( this.is8Bit ) {
      this.begPort &= 0xFF;
      if( this.endPort >= 0 ) {
	this.endPort &= 0xFF;
      }
    }
    createAndSetText(
		this.begPort,
		this.endPort,
		this.is8Bit,
		this.value,
		this.mask );
  }


  public int getBegPort()
  {
    return this.begPort;
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


  public boolean is8Bit()
  {
    return this.is8Bit;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void completeAccessMode( AccessMode accessMode )
  {
    super.completeAccessMode( accessMode );
    createAndSetText(
		this.begPort,
		this.endPort,
		this.is8Bit,
		this.value,
		this.mask );
  }


	/* --- Z80Breakpoint --- */

  @Override
  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource )
  {
    boolean rv  = false;
    int     pc  = cpu.getRegPC();
    int     op0 = cpu.getMemByte( pc, false );
    int     op1 = cpu.getMemByte( pc + 1, false );
    switch( getAccessMode() ) {
      case READ:
	rv = matchesRead( cpu, op0, op1 );
	break;
      case WRITE:
	rv = matchesWrite( cpu, op0, op1 );
	break;
      case READ_WRITE:
	rv = matchesRead( cpu, op0, op1 );
	if( !rv ) {
	  rv = matchesWrite( cpu, op0, op1 );
	}
	break;
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


  private boolean matchesRead( Z80CPU cpu, int op0, int op1 )
  {
    boolean rv = false;
    if( op0 == 0xDB ) {			// IN A
      rv = matchesPort( (cpu.getRegA() << 8) | op1 );
    }
    else if( op0 == 0xED ) {
      if( (op1 == 0x40)			// IN B,(C)
	  || (op1 == 0x48)		// IN C,(C)
	  || (op1 == 0x50)		// IN D,(C)
	  || (op1 == 0x58)		// IN E,(C)
	  || (op1 == 0x60)		// IN H,(C)
	  || (op1 == 0x68)		// IN L,(C)
	  || (op1 == 0x70)		// IN F,(C)
	  || (op1 == 0x78)		// IN A,(C)
	  || (op1 == 0xA2)		// INI
	  || (op1 == 0xAA)		// IND
	  || (op1 == 0xB2)		// INIR
	  || (op1 == 0xBA) )		// INDR
      {
	rv = matchesPort( cpu.getRegBC() );
      }
    }
    return rv;
  }


  private boolean matchesWrite( Z80CPU cpu, int op0, int op1 )
  {
    boolean rv    = false;
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
	     * Bei den Blockausgabebefehlen wird Das B-Register
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
	if( this.value >= 0 ) {
	  if( (this.value & this.mask) == (value & this.mask) ) {
	    rv = true;
	  }
	} else {
	  rv = true;
	}
      }
    }
    return rv;
  }
}
