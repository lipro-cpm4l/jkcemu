/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Haltepunkt auf eine Programmadresse
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class PCBreakpoint extends AbstractBreakpoint
{
  private int    addr;
  private String reg;
  private int    mask;
  private String cond;
  private int    value;


  public PCBreakpoint(
		int    addr,
		String reg,
		int    mask,
		String cond,
		int    value )
  {
    this.addr  = addr & 0xFFFF;
    this.reg   = reg;
    this.mask  = mask & 0xFFFF;
    this.cond  = cond;
    this.value = value & 0xFFFF;

    String text = String.format( "%04X", this.addr );
    if( (this.reg != null) && (this.cond != null) ) {
      if( !this.reg.isEmpty() && !this.cond.isEmpty() ) {
	StringBuilder buf = new StringBuilder( 32 );
	buf.append( text );
	buf.append( (char) ':' );
	if( reg.length() == 2 ) {
	  if( (this.mask & 0xFFFF) != 0xFFFF ) {
	    buf.append( String.format( "(%s&%04X)", this.reg, this.mask ) );
	  } else {
	    buf.append( reg );
	  }
	  buf.append( this.cond );
	  buf.append( String.format( "%04X", this.value ) );
	} else {
	  if( (this.mask & 0xFF) != 0xFF ) {
	    buf.append( String.format( "(%s&%02X)", this.reg, this.mask ) );
	  } else {
	    buf.append( reg );
	  }
	  buf.append( this.cond );
	  buf.append( String.format( "%02X", this.value ) );
	}
	text = buf.toString();
      }
    }
    setText( text );
  }


  public int getAddress()
  {
    return this.addr;
  }


  public String getCondition()
  {
    return this.cond;
  }


  public int getMask()
  {
    return this.mask;
  }


  public String getRegister()
  {
    return this.reg;
  }


  public int getValue()
  {
    return this.value;
  }


	/* --- Z80Breakpoint --- */

  @Override
  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource )
  {
    boolean rv = (cpu.getRegPC() == this.addr);
    if( rv && (this.reg != null) && (this.cond != null) ) {
      int r8  = -1;
      int r16 = -1;
      if( this.reg.equals( "A" ) ) {
	r8 = cpu.getRegA();
      } else if( this.reg.equals( "B" ) ) {
	r8 = cpu.getRegB();
      } else if( this.reg.equals( "C" ) ) {
	r8 = cpu.getRegC();
      } else if( this.reg.equals( "D" ) ) {
	r8 = cpu.getRegD();
      } else if( this.reg.equals( "E" ) ) {
	r8 = cpu.getRegE();
      } else if( this.reg.equals( "H" ) ) {
	r8 = cpu.getRegH();
      } else if( this.reg.equals( "L" ) ) {
	r8 = cpu.getRegL();
      } else if( this.reg.equals( "BC" ) ) {
	r16 = cpu.getRegBC();
      } else if( this.reg.equals( "DE" ) ) {
	r16 = cpu.getRegDE();
      } else if( this.reg.equals( "HL" ) ) {
	r16 = cpu.getRegHL();
      } else if( this.reg.equals( "IX" ) ) {
	r16 = cpu.getRegIX();
      } else if( this.reg.equals( "IXH" ) ) {
	r8 = (cpu.getRegIX() >> 8);
      } else if( this.reg.equals( "IXL" ) ) {
	r8 = cpu.getRegIX() & 0xFF;
      } else if( this.reg.equals( "IY" ) ) {
	r16 = cpu.getRegIY();
      } else if( this.reg.equals( "IYH" ) ) {
	r8 = (cpu.getRegIY() >> 8);
      } else if( this.reg.equals( "IYL" ) ) {
	r8 = cpu.getRegIY() & 0xFF;
      } else if( this.reg.equals( "SP" ) ) {
	r16 = cpu.getRegSP();
      }
      if( (r8 >= 0) || (r16 >= 0) ) {
	int v1 = 0;
	int v2 = 0;
	if( r8 >= 0 ) {
	  v1 = (r8 & this.mask & 0xFF);
	  v2 = (this.value & 0xFF);
	} else {
	  v1 = (r16 & this.mask & 0xFFFF);
	  v2 = (this.value & 0xFFFF);
	}
	rv = checkValues( v1, this.cond, v2 );
      }
    }
    return rv;
  }
}

