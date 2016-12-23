/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M011 (64 KByte RAM)
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M011 extends AbstractKC85Module
{
  private int     negMask;	// 1-Bits muessen in der Adresse negiert werden
  private boolean readWrite;
  private byte[]  ram;


  public M011( int slot )
  {
    super( slot );
    this.negMask   = 0;
    this.readWrite = false;
    this.ram       = new byte[ 0x10000 ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    buf.append( "Blockreihung: " );
    switch( this.negMask ) {
      case 0x0000:
	buf.append( "1 2 3 4" );
	break;
      case 0x4000:
	buf.append( "2 1 4 3" );
	break;
      case 0x8000:
	buf.append( "3 4 1 2" );
	break;
      case 0xC000:
	buf.append( "4 3 2 1" );
	break;
    }
  }


  @Override
  public void clearRAM()
  {
    Arrays.fill( this.ram, (byte) 0 );
  }


  @Override
  public String getModuleName()
  {
    return "M011";
  }


  @Override
  public Boolean getReadWrite()
  {
    return this.readWrite;
  }


  @Override
  public int getTypeByte()
  {
    return 0xF6;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled ) {
      int idx = addr ^ this.negMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.negMask   = (value << 8) & 0xC000;
    this.readWrite = ((value & 0x02) != 0);
  }


  @Override
  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled ) {
      int idx = addr ^ this.negMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	if( this.readWrite ) {
	  this.ram[ idx ] = (byte) value;
	  rv = 2;
	} else {
	  rv = 1;
	}
      }
    }
    return rv;
  }
}
