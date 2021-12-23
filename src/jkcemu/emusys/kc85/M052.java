/*
 * (c) 2011-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines USB- und Netzwerk-Moduls M052
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;
import jkcemu.net.KCNet;
import jkcemu.usb.VDIP;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class M052 extends AbstractKC85Module
				implements
					Z80InterruptSource,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  public static final String DESCRIPTION          = "USB/Netzwerk";
  public static final String USB_ONLY_DESCRIPTION = "Nur USB";

  private static byte[] m052 = null;

  private Component owner;
  private String    fileName;
  private KCNet     kcNet;
  private VDIP      vdip;
  private boolean   ioEnabled;
  private int       begAddr;
  private int       romOffs;
  private byte[]    rom;


  public M052(
	    int       slot,
	    Component owner,
	    Z80CPU    z80cpu,
	    int       vdipNum,
	    boolean   usbOnly,
	    String    fileName )
  {
    super( slot );
    this.owner    = owner;
    this.fileName = fileName;
    if( usbOnly ) {
      this.kcNet = null;
    } else {
      this.kcNet = new KCNet( "Netzwerk-PIO" );
    }
    this.vdip = new VDIP( vdipNum, z80cpu, "USB-PIO" );
    this.vdip.setModuleTitle(
		String.format( "M052 in Schacht %02X", slot ) );
    this.ioEnabled = false;
    this.romOffs   = 0;
    this.begAddr   = 0;
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
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    if( this.kcNet != null ) {
      buf.append( "<h2>Netzwerk-PIO (E/A-Adressen 28h-2Bh)</h2>\n" );
      this.kcNet.appendInterruptStatusHTMLTo( buf );
    }
    buf.append( "<h2>USB-PIO (E/A-Adressen 2Ch-2Fh)</h2>\n" );
    this.vdip.appendInterruptStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int     rv   = 0;
    boolean done = false;
    if( this.kcNet != null ) {
      if( this.kcNet.isInterruptRequested() ) {
	rv   = this.kcNet.interruptAccept();
	done = true;
      }
    }
    if( !done ) {
      if( this.vdip.isInterruptRequested() ) {
	rv = this.vdip.interruptAccept();
      }
    }
    return rv;
  }


  @Override
  public synchronized boolean interruptFinish( int addr )
  {
    boolean rv = false;
    if( this.kcNet != null ) {
      rv = this.kcNet.interruptFinish( addr );
    }
    if( !rv ) {
      rv = this.vdip.interruptFinish( addr );
    }
    return rv;
  }


  @Override
  public boolean isInterruptAccepted()
  {
    boolean rv = false;
    if( this.kcNet != null ) {
      rv = this.kcNet.isInterruptAccepted();
    }
    return rv | this.vdip.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    boolean rv = false;
    if( this.kcNet != null ) {
      rv = this.kcNet.isInterruptRequested();
    }
    return rv | this.vdip.isInterruptRequested();
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    if( this.kcNet != null )
      this.kcNet.z80MaxSpeedChanged( cpu );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.kcNet != null )
      this.kcNet.z80TStatesProcessed( cpu, tStates );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    if( this.kcNet == null ) {
      buf.append( USB_ONLY_DESCRIPTION );
    }
    String fileName = getFileName();
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	if( this.kcNet == null ) {
	  buf.append( ", " );
	}
	buf.append( "ROM-Datei: " );
	EmuUtil.appendHTML( buf, fileName );
      }
    }
  }


  @Override
  public void die()
  {
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    this.vdip.die();
  }


  @Override
  public int getBegAddr()
  {
    return this.begAddr;
  }


  @Override
  public String getFileName()
  {
    return this.fileName;
  }


  @Override
  public int getLEDrgb()
  {
    int rv = KC85FrontFld.RGB_LED_DARK;
    if( this.enabled && this.ioEnabled ) {
      rv = KC85FrontFld.RGB_LED_YELLOW;
    } else {
      if( this.enabled ) {
	rv = KC85FrontFld.RGB_LED_GREEN;
      } else if( this.ioEnabled ) {
	rv = KC85FrontFld.RGB_LED_RED;
      }
    }
    return rv;
  }


  @Override
  public String getModuleName()
  {
    return "M052";
  }


  @Override
  public int getSegmentNum()
  {
    return (this.romOffs >> 13) & 0x03;
  }


  @Override
  public int getTypeByte()
  {
    return 0xFD;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = -1;
    if( this.ioEnabled ) {
      switch( port & 0xFF ) {
	case 0x28:
	case 0x29:
	case 0x2A:
	case 0x2B:
	  if( this.kcNet != null ) {
	    rv = this.kcNet.read( port );
	  } else {
	    rv = 0xFF;
	  }
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
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + 0x2000))
	&& (this.rom != null) )
    {
      int idx = addr - this.begAddr + this.romOffs;
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
	this.rom = FileUtil.readFile(
			owner,
			this.fileName,
			true,
			0x8000,
			"M052 ROM-Datei" );
      }
    }
  }


  @Override
  public void reset( boolean powerOn )
  {
    if( this.kcNet != null ) {
      this.kcNet.reset( powerOn );
    }
    this.vdip.reset( powerOn );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.ioEnabled = ((value & 0x04) != 0);
    this.begAddr   = (value << 8) & 0xC000;
    this.romOffs   = (value << 10) & 0x6000;
  }


  @Override
  public boolean writeIOByte( int port, int value, int tStates )
  {
    boolean rv = false;
    if( this.ioEnabled ) {
      switch( port & 0xFF ) {
	case 0x28:
	case 0x29:
	case 0x2A:
	case 0x2B:
	case 0x08:
	  if( this.kcNet != null ) {
	    this.kcNet.write( port, value );
	  }
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
