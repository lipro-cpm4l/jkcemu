/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC2
 */

package jkcemu.system;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import z80emu.*;


public class LLC2 extends EmuSys implements Z80CTCListener
{
  private static byte[] fontBytes = null;
  private static byte[] scchMon91 = null;
  private static byte[] gsbasic   = null;

  private Z80CTC  ctc;
  private Z80PIO  pio;
  private byte[]  ramExtended;
  private byte[]  prgX;
  private byte[]  romdisk;
  private boolean romEnabled;
  private boolean romdiskEnabled;
  private boolean romdiskA15;
  private boolean prgXEnabled;
  private boolean gsbasicEnabled;
  private boolean inverseMode;
  private boolean loudspeakerEnabled;
  private boolean loudspeakerPhase;
  private boolean audioOutPhase;
  private boolean keyboardUsed;
  private boolean hiRes;
  private boolean rf32KActive;
  private boolean rf32NegA15;
  private boolean rfReadEnabled;
  private boolean rfWriteEnabled;
  private int     rfAddr16to19;
  private int     fontOffset;
  private int     videoPixelAddr;
  private int     videoTextAddr;


  public LLC2( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( fontBytes == null ) {
      fontBytes = readResource( "/rom/llc2/llc2font.bin" );
    }
    if( scchMon91 == null ) {
      scchMon91 = readResource( "/rom/llc2/scchmon_91.bin" );
    }
    if( gsbasic == null ) {
      gsbasic = readResource( "/rom/llc2/gsbasic.bin" );
    }
    this.ramExtended = this.emuThread.getExtendedRAM( 0x100000 );
    this.prgX        = readProgramX( props );
    this.romdisk     = readROMDisk( props );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc, this.pio );

    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this.ctc );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 3000;
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:

	  // Lautsprecher
	  if( this.loudspeakerEnabled ) {
	    this.loudspeakerPhase = !this.loudspeakerPhase;
	    if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
	      this.emuThread.writeAudioPhase( this.loudspeakerPhase );
	    }
	  }
	  break;

	case 1:
	  // Verbindung von Ausgang 1 auf Eingang 3
	  ctc.externalUpdate( 3, 1 );
	  break;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this.ctc );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x1856;
  }


  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.hiRes ) {
      int g = y / 8;
      int p = y % 8;
      int b = this.emuThread.getRAMByte(
		this.videoPixelAddr + (p * 0x0800) + (g * 64) + (x / 8) );
      int m = 0x80;
      int n = x % 8;
      if( n > 0 ) {
	m >>= n;
      }
      if( (b & m) != 0 ) {
	rv = WHITE;
      }
    } else {
      byte[] fBytes = this.emuThread.getExtFontBytes();
      if( fBytes == null ) {
	fBytes = fontBytes;
      }
      if( fBytes != null ) {
	int row = y / 8;
	int col = x / 8;
	int ch  = this.emuThread.getRAMByte(
				this.videoTextAddr + (row * 64) + col );
	int idx = this.fontOffset
			+ ((ch & (this.inverseMode ? 0x7F : 0xFF)) * 8)
			+ (y % 8);
	if( (idx >= 0) && (idx < fBytes.length) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  if( (fBytes[ idx ] & m) != 0 ) {
	    rv = WHITE;
	  }
	}
	if( this.inverseMode && ((ch & 0x80) != 0) ) {
	  rv = (rv == WHITE ? BLACK : WHITE);
	}
      }
    }
    return rv;
  }


  public int getCharColCount()
  {
    return 64;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return 32;
  }


  public int getCharRowHeight()
  {
    return 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public String getHelpPage()
  {
    return "/help/llc2.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( !m1 && this.rfReadEnabled ) {
      if( this.ramExtended != null ) {
	int idx = this.rfAddr16to19 | addr;
	if( idx < this.ramExtended.length ) {
	  rv = (int) this.ramExtended[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.rf32KActive && (addr >= 0x4000) && (addr < 0xC000) ) {
      if( this.ramExtended != null ) {
	int idx = this.rfAddr16to19 | addr;
	if( this.rf32NegA15 ) {
	  if( (idx & 0x8000) != 0 ) {
	    idx &= 0xF7FFF;
	  } else {
	    idx |= 0x8000;
	  }
	}
	if( idx < this.ramExtended.length ) {
	  rv = (byte) this.ramExtended[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.gsbasicEnabled
	&& (addr >= 0x4000) && (addr < 0x6000) )
    {
      if( gsbasic != null ) {
	int idx = addr - 0x4000;
	if( idx < gsbasic.length ) {
	  rv = (int) gsbasic[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.romdiskEnabled && (addr >= 0xC000) ) {
      if( this.romdisk != null ) {
	int idx = addr - 0xC000;
	if( this.prgXEnabled ) {
	  idx |= 0x4000;
	}
	if( this.romdiskA15 ) {
	  idx |= 0x8000;
	}
	if( idx < this.romdisk.length ) {
	  rv = (int) this.romdisk[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.prgXEnabled && (addr >= 0xE000) ) {
      if( this.prgX != null ) {
	int idx = addr - 0xE000;
	if( idx < this.prgX.length ) {
	  rv = (int) this.prgX[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.romEnabled && (addr < 0xC000) ) {
      if( scchMon91 != null ) {
	if( addr < scchMon91.length ) {
	  rv = (int) this.scchMon91[ addr ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int addr = this.videoTextAddr + (chY * 64) + chX;
    if( (addr >= this.videoTextAddr)
	&& (addr < (this.videoTextAddr + 0x0800)) )
    {
      int b = this.emuThread.getRAMByte( addr );
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return 256;
  }


  public int getScreenWidth()
  {
    return 512;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return "LLC2";
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    boolean rv = false;
    if( !this.rfReadEnabled ) {
      if( addr < 0xC000 ) {
	rv = this.romEnabled;
      } else if( (addr >= 0xC000) && (addr < 0xE000) ) {
	if( !this.romdiskEnabled ) {
	  rv = true;
	}
      } else {
	if( !this.romdiskEnabled && !this.prgXEnabled ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  public boolean isISO646DE()
  {
    return true;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 8;
	break;

      case KeyEvent.VK_RIGHT:
	ch = 9;
	break;

      case KeyEvent.VK_DOWN:
	ch = 0x0A;
	break;

      case KeyEvent.VK_UP:
	ch = 0x0B;
	break;

      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.pio.putInValuePortA( ch | 0x80, false );
      rv = true;
    }
    return rv;
  }


  public void keyReleased()
  {
    this.pio.putInValuePortA( 0, false );
  }


  public void openBasicProgram()
  {
    String text = AC1LLC2Util.getSCCHBasicProgram( this.emuThread );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return AC1LLC2Util.reassembleSysCall(
				this.emuThread,
				addr,
				buf,
				colMnemonic,
				colArgs,
				colRemark );
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0xF8) == 0xD0 ) {
      rv = this.emuThread.getRAMFloppy1().readByte( port & 0x07 );
    }
    else if( (port & 0xF8) == 0xB0 ) {
      rv = this.emuThread.getRAMFloppy2().readByte( port & 0x07 );
    } else {
      switch( port & 0xFF ) {
	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.romEnabled = false;
	  break;

	case 0xE8:
	  if( !this.keyboardUsed ) {
	    this.pio.putInValuePortA( 0, 0xFF );
	    this.keyboardUsed = true;
	  }
	  rv = this.pio.readPortA();
	  break;

	case 0xE9:
	  this.pio.putInValuePortB(
			  this.emuThread.readAudioPhase() ? 0xFE : 0xFC,
			  0x22 );
	  rv = this.pio.readPortB();
	  break;

	case 0xEA:
	  rv = this.pio.readControlA();
	  break;

	case 0xEB:
	  rv = this.pio.readControlB();
	  break;

	case 0xF8:
	case 0xF9:
	case 0xFA:
	case 0xFB:
	  rv = this.ctc.read( port & 0x03 );
	  break;
      }
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert.
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "LLC2" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
    }
    this.romEnabled         = true;
    this.romdiskEnabled     = false;
    this.romdiskA15         = false;
    this.prgXEnabled        = false;
    this.gsbasicEnabled     = false;
    this.inverseMode        = false;
    this.loudspeakerEnabled = false;
    this.loudspeakerPhase   = false;
    this.audioOutPhase      = false;
    this.keyboardUsed       = false;
    this.hiRes              = false;
    this.rf32KActive        = false;
    this.rf32NegA15         = false;
    this.rfReadEnabled      = false;
    this.rfWriteEnabled     = false;
    this.rfAddr16to19       = 0;
    this.fontOffset         = 0;
    this.videoPixelAddr     = 0xC000;
    this.videoTextAddr      = 0xC000;
  }


  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCStyleBasicEndAddr( this.emuThread, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      (new SaveDlg(
		this.screenFrm,
		0x60F7,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		"LLC2-BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( this.rfWriteEnabled ) {
      if( this.ramExtended != null ) {
	int idx = this.rfAddr16to19 | addr;
	if( idx < this.ramExtended.length ) {
	  this.ramExtended[ idx ] = (byte) value;
	  rv = true;
	}
      }
      done = true;
    }
    if( !done && this.rf32KActive && (addr >= 0x4000) && (addr < 0xC000) ) {
      if( this.ramExtended != null ) {
	int idx = this.rfAddr16to19 | addr;
	if( this.rf32NegA15 ) {
	  if( (idx & 0x8000) != 0 ) {
	    idx &= 0xF7FFF;
	  } else {
	    idx |= 0x8000;
	  }
	}
	if( idx < this.ramExtended.length ) {
	  this.ramExtended[ idx ] = (byte) value;
	  rv = true;
	}
      }
      done = true;
    }
    if( !done && this.gsbasicEnabled
          && (addr >= 0x4000) && (addr < 0x6000) )
    {
      done = true;
    }
    if( !done && this.romdiskEnabled && (addr >= 0xC000) ) {
      done = true;
    }
    if( !done && this.prgXEnabled && (addr >= 0xE000) ) {
      done = true;
    }
    if( !done && (!this.romEnabled || (addr >= 0xC000)) ) {
      this.emuThread.setRAMByte( addr, value );
      if( this.hiRes ) {
	if( (addr >= this.videoPixelAddr)
	    && (addr < (this.videoPixelAddr + 0x4000)) )
	{
	  this.screenFrm.setScreenDirty( true );
	}
      } else {
	if( (addr >= this.videoTextAddr)
	    && (addr < (this.videoTextAddr + 0x0800)) )
	{
	  this.screenFrm.setScreenDirty( true );
	}
      }
      rv = true;
    }
    return rv;
  }


  public boolean supportsRAMFloppy1()
  {
    return true;
  }


  public boolean supportsRAMFloppy2()
  {
    return true;
  }


  public void updSysCells(
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    if( (begAddr == 0x60F7) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	if( fileType == 'B' ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x60D2, topAddr );
	  this.emuThread.setMemWord( 0x60D4, topAddr );
	  this.emuThread.setMemWord( 0x60D6, topAddr );
	}
      }
    }
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xF8) == 0xD0 ) {
      this.emuThread.getRAMFloppy1().writeByte( port & 0x07, value );
    } else if( (port & 0xF8) == 0xB0 ) {
      this.emuThread.getRAMFloppy2().writeByte( port & 0x07, value );
    } else {
      boolean dirty = false;
      switch( port & 0xFF ) {
	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.romEnabled = false;
	  break;

	case 0xE8:
	  this.pio.writePortA( value );
	  break;

	case 0xE9:
	  this.pio.writePortB( value );
	  {
	    int     v = this.pio.fetchOutValuePortB( false );
	    boolean b = ((v & 0x01) != 0);
	    if( b != this.audioOutPhase ) {
	      this.audioOutPhase = b;
	      if( !this.emuThread.isLoudspeakerEmulationEnabled() ) {
		this.emuThread.writeAudioPhase( b );
	      }
	    }
	    b = ((v & 0x20) != 0);
	    if( b != this.inverseMode ) {
	      this.inverseMode = b;
	      dirty            = true;
	    }
	    b = ((v & 0x40) != 0);
	    if( b != this.inverseMode ) {
	      this.loudspeakerEnabled = b;
	      if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
		this.loudspeakerPhase = !this.loudspeakerPhase;
		this.emuThread.writeAudioPhase( this.loudspeakerPhase );
	      }
	    }
	  }
	  break;

	case 0xEA:
	  this.pio.writeControlA( value );
	  break;

	case 0xEB:
	  this.pio.writeControlB( value );
	  break;

	case 0xEC:
	  this.prgXEnabled    = ((value & 0x01) != 0);
	  this.gsbasicEnabled = ((value & 0x02) != 0);
	  this.romdiskEnabled = ((value & 0x08) != 0);
	  this.romdiskA15     = ((value & 0x20) != 0);
	  if( (value & 0x04) != 0 ) {
	    this.romEnabled = false;
	  }
	  break;

	case 0xED:
	  this.rfAddr16to19   = (value << 16) & 0xF0000;
	  this.rf32NegA15     = ((value & 0x10) != 0);
	  this.rf32KActive    = ((value & 0x20) != 0);
	  this.rfReadEnabled  = ((value & 0x40) != 0);
	  this.rfWriteEnabled = ((value & 0x80) != 0);
	  break;

	case 0xEE:
	  {
	    int vAddr = ((value & 0x44) == 0x04 ? 0xF800 : 0xC000);
	    if( vAddr != this.videoTextAddr ) {
	      this.videoTextAddr = vAddr;
	      dirty              = true;
	    }
	    switch( value & 0x30 ) {
	      case 0:
		vAddr = 0xC000;
		break;
	      case 0x10:
		vAddr = 0x8000;
		break;
	      case 0x20:
		vAddr = 0x4000;
		break;
	      case 030:
		vAddr = 0x0000;
		break;
	    }
	    if( vAddr != videoPixelAddr ) {
	      this.videoPixelAddr = vAddr;
	      dirty               = true;
	    }
	    boolean hiRes = ((value & 0x40) != 0);
	    if( hiRes != this.hiRes ) {
	      this.hiRes = hiRes;
	      dirty      = true;
	    }
	  }
	  break;

	case 0xF8:
	case 0xF9:
	case 0xFA:
	case 0xFB:
	  this.ctc.write( port & 0x03, value );
	  break;
      }
      if( dirty ) {
	this.screenFrm.setScreenDirty( true );
      }
    }
  }
}

