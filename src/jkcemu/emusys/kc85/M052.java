/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines USB- und Netzwerk-Moduls M052
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.etc.VDIP;
import jkcemu.net.KCNet;
import z80emu.*;


public class M052 extends AbstractKC85Module
				implements
					Z80InterruptSource,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static byte[] m052 = null;

  private Component owner;
  private String    fileName;
  private String    title;
  private KCNet     kcNet;
  private VDIP      vdip;
  private boolean   ioEnabled;
  private int       baseAddr;
  private int       romOffs;
  private byte[]    rom;


  public M052( int slot, Component owner, String fileName )
  {
    super( slot );
    this.owner     = owner;
    this.fileName  = fileName;
    this.title     = String.format( "M052 im Schacht %02X", slot );
    this.kcNet     = new KCNet( "Netzwerk-PIO" );
    this.vdip      = new VDIP( "USB-PIO" );
    this.ioEnabled = false;
    this.romOffs   = 0;
    this.baseAddr  = 0;
    this.rom       = null;
    reload( owner );
    if( this.rom == null ) {
      if( m052 == null ) {
	m052 = EmuUtil.readResource( owner, "/rom/kc85/m052.bin" );
      }
      this.rom = m052;
    }
  }


  public VDIP getVDIP()
  {
    return this.vdip;
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<h2>Netzwerk-PIO (IO-Adressen 28-2B)</h2>\n" );
    this.kcNet.appendStatusHTMLTo( buf );

    buf.append( "<h2>USB-PIO (IO-Adressen 2C-2F)</h2>\n" );
    this.vdip.appendStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int rv = 0;
    if( this.kcNet.isInterruptRequested() ) {
      rv = this.kcNet.interruptAccept();
    }
    else if( this.vdip.isInterruptRequested() ) {
      rv = this.vdip.interruptAccept();
    }
    return rv;
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( this.kcNet.isInterruptAccepted() ) {
      this.kcNet.interruptFinish();
    }
    else if( this.vdip.isInterruptAccepted() ) {
      this.vdip.interruptFinish();
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.kcNet.isInterruptAccepted()
			|| this.vdip.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    boolean rv = this.kcNet.isInterruptRequested();
    if( !rv && !this.kcNet.isInterruptAccepted() ) {
      rv = this.vdip.isInterruptRequested();
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn )
  {
    this.kcNet.reset( powerOn );
    this.vdip.reset( powerOn );
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.kcNet.z80MaxSpeedChanged( cpu );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.kcNet.z80TStatesProcessed( cpu, tStates );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void die()
  {
    this.kcNet.die();
    this.vdip.die();
  }


  @Override
  public String getFileName()
  {
    return this.fileName;
  }


  @Override
  public String getModuleName()
  {
    return "M052";
  }


  @Override
  public int getTypeByte()
  {
    return 0xFD;
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = -1;
    if( this.ioEnabled ) {
      switch( port & 0xFF ) {
	case 0x28:
	case 0x29:
	case 0x2A:
	case 0x2B:
	  rv = this.kcNet.read( port );
	  break;

	case 0x2C:
	case 0x2D:
	case 0x2E:
	case 0x2F:
	  rv = this.vdip.read( port );
	  break;
      }
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.baseAddr)
	&& (addr < (this.baseAddr + 0x2000))
	&& (this.rom != null) )
    {
      int idx = addr - this.baseAddr + this.romOffs;
      if( idx < this.rom.length ) {
	rv = (int) this.rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void reload( Component owner )
  {
    if( this.fileName != null ) {
      if( !this.fileName.isEmpty() ) {
	this.rom = EmuUtil.readFile(
			owner,
			this.fileName,
			0x8000,
			"M052 ROM-Datei" );
      }
    }
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.ioEnabled = ((value & 0x04) != 0);
    this.baseAddr  = (value << 8) & 0xC000;
    this.romOffs   = (value << 10) & 0x6000;
  }


  @Override
  public String toString()
  {
    return this.title;
  }


  @Override
  public boolean writeIOByte( int port, int value )
  {
    boolean rv = false;
    if( this.ioEnabled ) {
      switch( port & 0xFF ) {
	case 0x28:
	case 0x29:
	case 0x2A:
	case 0x2B:
	case 0x08:
	  this.kcNet.write( port, value );
	  rv = true;
	  break;

	case 0x2C:
	case 0x2D:
	case 0x2E:
	case 0x2F:
	  this.vdip.write( port, value );
	  rv = true;
	  break;
      }
    }
    return rv;
  }
}
