/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC2
 */

package jkcemu.system;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import z80emu.*;


public class LLC2
		extends EmuSys
		implements
			FDC8272.DriveSelector,
			Z80CTCListener,
			Z80TStatesListener
{
  private static byte[] fontBytes = null;
  private static byte[] scchMon91 = null;
  private static byte[] gsbasic   = null;

  private Z80CTC            ctc;
  private Z80PIO            pio;
  private FDC8272           fdc;
  private byte[]            ramExtended;
  private byte[]            prgXBytes;
  private byte[]            romdiskBytes;
  private String            prgXFileName;
  private String            romdiskFileName;
  private boolean           romEnabled;
  private boolean           romdiskEnabled;
  private boolean           romdiskA15;
  private boolean           prgXEnabled;
  private boolean           gsbasicEnabled;
  private boolean           inverseMode;
  private boolean           loudspeakerEnabled;
  private boolean           loudspeakerPhase;
  private boolean           audioOutPhase;
  private boolean           keyboardUsed;
  private boolean           hiRes;
  private boolean           rf32KActive;
  private boolean           rf32NegA15;
  private boolean           rfReadEnabled;
  private boolean           rfWriteEnabled;
  private int               rfAddr16to19;
  private int               fontOffset;
  private int               videoPixelAddr;
  private int               videoTextAddr;
  private int               romdiskBankAddr;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;


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
    this.curFDDrive = null;
    this.fdDrives   = new FloppyDiskDrive[ 4 ];
    Arrays.fill( this.fdDrives, null );

    this.fdc             = new FDC8272( this, 4 );
    this.ramExtended     = this.emuThread.getExtendedRAM( 0x100000 );
    this.prgXBytes       = null;
    this.prgXFileName    = null;
    this.romdiskBytes    = null;
    this.romdiskFileName = null;
    this.romdiskBankAddr = 0;
    lazyReadPrgX( props );
    lazyReadRomdisk( props );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc, this.pio );

    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this );
    cpu.addMaxSpeedListener( this.fdc );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 3000;
  }


	/* --- FDC8272.DriveSelector --- */

  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    return this.curFDDrive;
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


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );
    this.fdc.z80TStatesProcessed( cpu, tStates );
  }


	/* --- ueberschriebene Methoden --- */


  public void applySettings( Properties props )
  {
    super.applySettings( props );
    lazyReadPrgX( props );
    lazyReadRomdisk( props );
  }


  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeMaxSpeedListener( this.fdc );
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x1856;
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


  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return false;
  }


  public int getDefaultFloppyDiskBlockSize()
  {
    return 2048;
  }


  public int getDefaultFloppyDiskDirBlocks()
  {
    return 1;
  }


  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.getFormat( 1, 80, 5, 1024 );
  }


  public int getDefaultFloppyDiskSystemTracks()
  {
    return 0;
  }


  public long getDelayMillisAfterPasteChar()
  {
    return 50;
  }


  public long getDelayMillisAfterPasteEnter()
  {
    return 150;
  }


  public long getHoldMillisPasteChar()
  {
    return 50;
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
      if( this.romdiskBytes != null ) {
	int idx = this.romdiskBankAddr | (addr - 0xC000);
	if( idx < this.romdiskBytes.length ) {
	  rv = (int) this.romdiskBytes[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && this.prgXEnabled && (addr >= 0xE000) ) {
      if( this.prgXBytes != null ) {
	int idx = addr - 0xE000;
	if( idx < this.prgXBytes.length ) {
	  rv = (int) this.prgXBytes[ idx ] & 0xFF;
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


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives.length;
  }


  public int getSupportedRAMFloppyCount()
  {
    return 2;
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


  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    byte[] fontBytes = null;
    if( !this.hiRes ) {
      fontBytes = this.emuThread.getExtFontBytes();
      if( fontBytes == null ) {
	fontBytes = this.fontBytes;
      }
    }
    if( this.hiRes || (fontBytes != null) ) {
      int bgColorIdx = getBorderColorIndex();
      int wBase      = getScreenWidth();
      int hBase      = getScreenHeight();

      if( (xOffs > 0) || (yOffs > 0) ) {
	g.translate( xOffs, yOffs );
      }

      /*
       * Aus Gruenden der Performance werden nebeneinander liegende
       * weisse Punkte zusammengefasst und als Linie gezeichnet.
       */
      for( int y = 0; y < hBase; y++ ) {
	int     lastColorIdx = -1;
	int     xColorBeg    = -1;
	boolean inverse      = false;
	for( int x = 0; x < wBase; x++ ) {
	  int     col   = x / 8;
	  int     row   = y / 8;
	  int     rPix  = y % 8;
	  boolean pixel = false;
	  if( this.hiRes ) {
	    int b = this.emuThread.getRAMByte(
				this.videoPixelAddr + (rPix * 0x0800)
						+ (row * 64) + (x / 8) );
	    int m = 0x80;
	    int n = x % 8;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    pixel = ((b & m) != 0);
	  } else {
	    int ch = this.emuThread.getRAMByte(
				this.videoTextAddr + (row * 64) + col );
	    if( ch == 0x10 ) {
	      inverse = false;
	    } else if( ch == 0x11 ) {
	      inverse = true;
	    }
	    int idx = this.fontOffset
			+ ((ch & (this.inverseMode ? 0x7F : 0xFF)) * 8)
			+ (y % 8);
	    if( (idx >= 0) && (idx < fontBytes.length) ) {
	      int m = 0x80;
	      int n = x % 8;
	      if( n > 0 ) {
		m >>= n;
	      }
	      pixel = ((fontBytes[ idx ] & m) != 0);
	      if( inverse || this.inverseMode ) {
		pixel = !pixel;
	      }
	    }
	  }
	  int curColorIdx = (pixel ? 1 : 0);
	  if( curColorIdx != lastColorIdx ) {
	    if( (lastColorIdx >= 0)
		&& (lastColorIdx != bgColorIdx)
		&& (xColorBeg >= 0) )
	    {
	      g.setColor( getColor( lastColorIdx ) );
	      g.fillRect(
			xColorBeg * screenScale,
			y * screenScale,
			(x - xColorBeg) * screenScale,
			screenScale );
	    }
	    xColorBeg    = x;
	    lastColorIdx = curColorIdx;
	  }
	}
	if( (lastColorIdx >= 0)
	    && (lastColorIdx != bgColorIdx)
	    && (xColorBeg >= 0) )
	{
	  g.setColor( getColor( lastColorIdx ) );
	  g.fillRect(
		xColorBeg * screenScale,
		y * screenScale,
		(wBase - xColorBeg) * screenScale,
		screenScale );
	}
      }
      if( (xOffs > 0) || (yOffs > 0) ) {
	g.translate( -xOffs, -yOffs );
      }
    }
    return true;
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
	case 0xA0:
	  rv = this.fdc.readMainStatusReg();
	  break;

	case 0xA1:
	  rv = this.fdc.readData();
	  break;

	case 0xA2:
	case 0xA3:
	  rv = this.fdc.readDMA();
	  break;

	case 0xA4:
	case 0xA5:
	  {
	    rv = 0xDF;		// Bit 0..3 nicht benutzt, 4..7 invertiert
	    if( this.fdc.getIndexHoleState() ) {
	      rv &= ~0x10;
	    }
	    FloppyDiskDrive fdd = this.curFDDrive;
	    if( fdd != null ) {
	      if( fdd.isReady() ) {
		rv |= 0x20;
	      }
	    }
	    if( this.fdc.isInterruptRequest() ) {
	      rv &= ~0x40;
	    }
	    if( this.fdc.isDMARequest() ) {
	      rv &= ~0x80;
	    }
	  }
	  break;

	case 0xA8:
	case 0xA9:
	  this.fdc.fireTC();
	  break;

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
      this.fdc.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
      this.fdc.reset( false );
    }
    for( int i = 0; i < this.fdDrives.length; i++ ) {
      FloppyDiskDrive drive = this.fdDrives[ i ];
      if( drive != null ) {
	drive.reset();
      }
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
    this.curFDDrive         = null;
  }


  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x60F7 );
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


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( (idx >= 0) && (idx < this.fdDrives.length) ) {
      this.fdDrives[ idx ] = drive;
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


  public boolean supportsAudio()
  {
    return true;
  }


  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  public boolean supportsPasteFromClipboard()
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
	case 0xA1:
	  this.fdc.write( value );
	  break;

	case 0xA2:
	case 0xA3:
	  this.fdc.writeDMA( value );
	  break;

	case 0xA6:
	case 0xA7:
	  {
	    FloppyDiskDrive fdd  = null;
	    int             mask = 0x01;
	    for( int i = 0; i < this.fdDrives.length; i++ ) {
	      if( (value & mask) != 0 ) {
		fdd = this.fdDrives[ i ];
		break;
	      }
	      mask <<= 1;
	    }
	    this.curFDDrive = fdd;
	  }
	  break;

	case 0xA8:
	case 0xA9:
	  this.fdc.fireTC();
	  break;

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
	    if( b != this.loudspeakerEnabled ) {
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
	  if( this.romdiskEnabled ) {
	    int bank = (value & 0x01) | ((value >> 3) & 0x0E);
	    this.romdiskBankAddr = (((value & 0x01) | ((value >> 3) & 0x0E))
								 << 14);
	    this.prgXEnabled = false;
	  } else {
	    this.prgXEnabled = ((value & 0x01) != 0);
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


	/* --- private Methoden --- */

  private void lazyReadPrgX( Properties props )
  {
    String fName = EmuUtil.getProperty( props, "jkcemu.program_x.file.name" );
    if( EmuUtil.differs( fName, this.prgXFileName ) ) {
      this.prgXFileName = fName;
      this.prgXBytes    = readFile( fName, 0x2000, "Programmpaket X" );
    }
  }


  private void lazyReadRomdisk( Properties props )
  {
    String fName = EmuUtil.getProperty( props, "jkcemu.romdisk.file.name" );
    if( EmuUtil.differs( fName, this.romdiskFileName ) ) {
      this.romdiskFileName = fName;
      this.romdiskBytes    = readFile( fName, 0x40000, "ROM-Disk" );
    }
  }
}

