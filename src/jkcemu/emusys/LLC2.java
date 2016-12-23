/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC2
 */

package jkcemu.emusys;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileFormat;
import jkcemu.base.RAMFloppy;
import jkcemu.base.SaveDlg;
import jkcemu.base.SourceUtil;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.GIDE;
import jkcemu.emusys.ac1_llc2.AbstractSCCHSys;
import jkcemu.etc.VDIP;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;


public class LLC2
		extends AbstractSCCHSys
		implements
			FDC8272.DriveSelector,
			Z80CTCListener
{
  public static final String SYSNAME           = "LLC2";
  public static final String PROP_PREFIX       = "jkcemu.llc2.";
  public static final String PROP_SCREEN_RATIO = "screen.ratio";

  public static final String VALUE_SCREEN_RATIO_43       = "4:3";
  public static final String VALUE_SCREEN_RATIO_UNSCALED = "unscaled";

  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE = true;

  /*
   * Der im Emulator integrierte SCCH-Monitor V9.1 enthaelt eine Routine
   * zur seriellen Ausgabe von Zeichen ueber Bit 2 des IO-Ports E4h (PIO 2).
   * In der Standardeinstellung soll diese Ausabe mit 9600 Bit/s erfolgen,
   * was bei einer Taktfrequenz von 3 MHz 312,5 Takte pro Bit entspricht.
   * Tatsaechlich werden aber in der Standardeinstellung pro Bit
   * 4 Ausgabe-Befehle auf dem Port getaetigt, die zusammen
   * ca. 336 bis 339 Takte benoetigen, was etwa 8900 Bit/s entspricht.
   * Damit im Emulator die Ausgabe auf dem emulierten Drucker ueber diese
   * serielle Schnittstelle funktioniert,
   * wird somit der Drucker ebenfalls mit dieser Bitrate emuliert.
   * Ist allerdings eine externe ROM-Datei als Monitorprogramm eingebunden,
   * werden 9600 Baud emuliert.
   */
  private static int V24_TSTATES_PER_BIT_INTERN = 337;
  private static int V24_TSTATES_PER_BIT_EXTERN = 312;

  private static byte[] llc2Font  = null;
  private static byte[] scchMon91 = null;

  private Z80CTC            ctc;
  private Z80PIO            pio2;
  private GIDE              gide;
  private FDC8272           fdc;
  private RAMFloppy         ramFloppy1;
  private RAMFloppy         ramFloppy2;
  private BufferedImage     screenImg;
  private byte[]            screenBuf;
  private byte[]            fontBytes;
  private byte[]            osBytes;
  private String            osFile;
  private boolean           osRomEnabled;
  private boolean           bit7InverseMode;
  private boolean           screenInverseMode;
  private boolean           loudspeakerEnabled;
  private boolean           tapeInPhase;
  private boolean           keyboardUsed;
  private volatile boolean  graphicKeyState;
  private boolean           hiRes;
  private int               fontOffset;
  private int               videoPixelAddr;
  private int               videoTextAddr;
  private volatile int      tStatesPerLine;
  private int               lineTStateCounter;
  private int               lineCounter;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private KCNet             kcNet;
  private VDIP              vdip;


  public LLC2( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.screenImg  = null;
    this.screenBuf  = new byte[ 64 * 256 ];
    this.osBytes    = null;
    this.osFile     = null;
    this.pasteFast  = false;
    this.ramModule3 = this.emuThread.getExtendedRAM( 0x100000 );  // 1MByte
    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"LLC2",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an E/A-Adressen D0h-D7h",
				props,
				this.propPrefix + PROP_RF1_PREFIX );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				"LLC2",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an E/A-Adressen B0h-B7h",
				props,
				this.propPrefix + PROP_RF2_PREFIX );

    this.curFDDrive = null;
    this.fdDrives   = null;
    this.fdc        = null;
    if( emulatesFloppyDisk( props ) ) {
      this.fdDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.fdDrives, null );
      this.fdc = new FDC8272( this, 4 );
    }

    this.ctc  = new Z80CTC( "CTC (E/A-Adressen F8-FB)" );
    this.pio1 = new Z80PIO( "PIO (E/A-Adressen E8-EB)" );
    this.pio2 = new Z80PIO( "V24-PIO (E/A-Adressen E4-E7)" );

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (E/A-Adressen C0-C3)" );
    }

    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP(
			this.emuThread.getFileTimesViewFactory(),
			"USB-PIO (E/A-Adressen DC-DF, FC-FF)" );
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, this.propPrefix );

    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
    iSources.add( this.ctc );
    iSources.add( this.pio1 );
    iSources.add( this.pio2 );
    if( this.kcNet != null ) {
      iSources.add( this.kcNet );
    }
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    }
    Z80CPU cpu = emuThread.getZ80CPU();
    try {
      cpu.setInterruptSources(
	iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}

    this.ctc.setTimerConnection( 1, 3 );
    this.ctc.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    z80MaxSpeedChanged( cpu );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    checkAddPCListener( props );
    updScreenRatio( props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 3000;
  }


  protected void loadROMs( Properties props )
  {
    super.loadROMs( props, "/rom/llc2/gsbasic.bin" );

    // OS-ROM
    this.osFile  = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_FILE );
    this.osBytes = readROMFile( this.osFile, 0x1000, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( scchMon91 == null ) {
	scchMon91 = readResource( "/rom/llc2/scchmon_91g.bin" );
      }
      this.osBytes = scchMon91;
    }

    // Zeichensatz
    loadFont( props );
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    return this.curFDDrive;
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( (ctc == this.ctc) && (timerNum == 0) ) {
      if( this.loudspeakerEnabled ) {
	this.soundOutPhase = !this.soundOutPhase;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>LLC2 Status</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>Monitor-ROM:</td><td>" );
    buf.append( this.osRomEnabled ? "ein" : "aus" );
    buf.append( "</td></tr>\n"
	+ "<tr><td>ROM-Disk:</td><td>" );
    buf.append( this.scchRomdiskEnabled ? "ein" : "aus" );
    buf.append( "</td></tr>\n"
	+ "<tr><td>ROM-Disk Bank:</td><td>" );
    buf.append( (this.scchRomdiskBankAddr >> 14) & 0x0F );
    buf.append( "</td></tr>\n"
	+ "<tr><td>Programmpaket X ROM:</td><td>" );
    buf.append( this.scchPrgXRomEnabled ? "ein" : "aus" );
    buf.append( "</td></tr>\n"
	+ "<tr><td>BASIC-ROM:</td><td>" );
    buf.append( this.scchBasicRomEnabled ? "ein" : "aus" );
    buf.append( "</td></tr>\n"
	+ "<tr><td>Grafikmodus:</td><td>" );
    buf.append( this.hiRes ? "HIRES-Vollgrafik" : "Text" );
    buf.append( "</td></tr>\n"
	+ "<tr><td>Bildwiederholspeicher:</td><td>" );
    if( this.hiRes ) {
      buf.append( String.format(
			"%04Xh-%04Xh",
			this.videoPixelAddr,
			this.videoPixelAddr + 0x3FFF ) );
    } else {
      buf.append(
		String.format(
			"%04Xh-%04Xh",
			this.videoTextAddr,
			this.videoTextAddr + 0x07FF ) );
    }
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    loadFont( props );
    checkAddPCListener( props );
    if( updScreenRatio( props ) ) {
      this.screenFrm.fireScreenSizeChanged();
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = super.canApplySettings( props );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_OS_FILE ) );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			"LLC2",
			RAMFloppy.RFType.MP_3_1988,
			props,
			this.propPrefix + PROP_RF1_PREFIX );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			"LLC2",
			RAMFloppy.RFType.MP_3_1988,
			props,
			this.propPrefix + PROP_RF2_PREFIX );
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, this.propPrefix );
    }
    if( rv && emulatesFloppyDisk( props ) != (this.fdc != null) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesUSB( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return !this.hiRes;
  }


  @Override
  public void die()
  {
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.ramFloppy1 != null ) {
      this.ramFloppy1.deinstall();
    }
    if( this.ramFloppy2 != null ) {
      this.ramFloppy2.deinstall();
    }
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pasteFast ) {
      cpu.removePCListener( this );
      this.pasteFast = false;
    }

    if( this.fdc != null ) {
      this.fdc.die();
    }
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x1856;
  }


  @Override
  public int getBorderColorIndex()
  {
    return this.screenInverseMode ? WHITE : BLACK;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( (x == 0) && (this.tStatesPerLine <= 1) ) {
      fillScreenBufLine( y );
    }
    int b   = 0;
    int idx = (y * 64) + (x / 8);
    if( (idx >= 0) && (idx < this.screenBuf.length) ) {
      b = this.screenBuf[ idx ];
    }
    int m = 0x80;
    int n = x % 8;
    if( n > 0 ) {
      m >>= n;
    }
    if( (b & m) != 0 ) {
      rv = WHITE;
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster raster = null;
    if( !this.hiRes ) {
      if( this.screenImg != null ) {
	raster = new CharRaster( 64, 32, 12, 12, 8, 0 );
      } else {
	raster = new CharRaster( 64, 32, 8, 8, 8, 0 );
      }
    }
    return raster;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_400K;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 80;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 200;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 80;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/llc2.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = getScchMemByte( addr, m1 );
    if( rv < 0 ) {
      rv = 0xFF;
      if( this.osRomEnabled && (addr < 0xC000) ) {
	if( this.osBytes != null ) {
	  if( addr < this.osBytes.length ) {
	    rv = (int) this.osBytes[ addr ] & 0xFF;
	  }
	}
      } else {
	rv = this.emuThread.getRAMByte( addr );
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    if( !this.hiRes ) {
      int addr = this.videoTextAddr + (chY * 64) + chX;
      if( (addr >= this.videoTextAddr)
	  && (addr < (this.videoTextAddr + 0x0800)) )
      {
	int b = this.emuThread.getRAMByte( addr );
	if( b < scchCharToUnicode.length ) {
	  ch = scchCharToUnicode[ b ];
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return this.screenImg != null ? 384 : 256;
  }


  @Override
  public int getScreenWidth()
  {
    return 512;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives != null ? this.fdDrives.length : 0;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public String getTitle()
  {
    return SYSNAME;
  }


  @Override
  protected VDIP getVDIP()
  {
    return this.vdip;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    switch( keyCode ) {
      case KeyEvent.VK_F1:
	this.screenInverseMode = !this.screenInverseMode;
	this.screenFrm.setScreenDirty( true );
	rv = true;
	break;

      case KeyEvent.VK_F2:
        this.graphicKeyState = !this.graphicKeyState;
        rv = true;
        break;

      default:
	rv = super.keyPressed( keyCode, ctrlDown, shiftDown );
    }
    return rv;
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    boolean       rv  = false;
    BufferedImage img = this.screenImg;
    if( img != null ) {
      for( int ix = 0; ix < 512; ix++ ) {
	for( int iy = 0; iy < 256; iy++ ) {
	  img.setRGB(
		ix,
		iy,
		getColorIndex( ix, iy ) > 0 ? 0xFFFFFFFF : 0xFF000000 );
	}
      }
      g.drawImage(
		img,
		x,
		y,
		getScreenWidth() * screenScale,
		getScreenHeight() * screenScale,
		this );
      rv = true;
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    } else if( ((port & 0xF8) == 0xD0) && (this.ramFloppy1 != null) ) {
      rv = this.ramFloppy1.readByte( port & 0x07 );
    } else if( ((port & 0xF8) == 0xB0) && (this.ramFloppy2 != null) ) {
      rv = this.ramFloppy2.readByte( port & 0x07 );
    } else {
      switch( port & 0xFF ) {
	case 0xA0:
	  if( this.fdc != null ) {
	    rv = this.fdc.readMainStatusReg();
	  }
	  break;

	case 0xA1:
	  if( this.fdc != null ) {
	    rv = this.fdc.readData();
	  }
	  break;

	case 0xA2:
	case 0xA3:
	  if( this.fdc != null ) {
	    rv = this.fdc.readDMA();
	  }
	  break;

	case 0xA4:
	case 0xA5:
	  if( this.fdc != null ) {
	    rv = 0xDF;		// Bit 0..3 nicht benutzt, 4..7 invertiert
	    FloppyDiskDrive fdd = this.curFDDrive;
	    if( fdd != null ) {
	      if( fdd.isReady() ) {
		rv |= 0x20;
		if( this.fdc.getIndexHoleState() ) {
		  rv &= ~0x10;
		}
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
	  if( this.fdc != null ) {
	    this.fdc.fireTC();
	  }
	  break;

	case 0xC0:
	case 0xC1:
	case 0xC2:
	case 0xC3:
	  if( this.kcNet != null ) {
	    rv = this.kcNet.read( port );
	  }
	  break;

	case 0xDC:
	case 0xDD:
	case 0xDE:
	case 0xDF:
	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    rv = this.vdip.read( port );
	  }
	  break;

	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.osRomEnabled = false;
	  break;

	case 0xE4:
	  // V24: CTS=L (empfangsbereit)
	  this.pio2.putInValuePortA( 0, 0x04 );
	  rv = this.pio2.readDataA();
	  break;

	case 0xE5:
	  rv = this.pio2.readDataB();
	  break;

	case 0xE6:
	  rv = this.pio2.readControlA();
	  break;

	case 0xE7:
	  rv = this.pio2.readControlB();
	  break;

	case 0xE8:
	  synchronized( this.pio1 ) {
	    if( !this.keyboardUsed
		&& !(this.joystickEnabled && this.joystickSelected) )
	    {
	      this.pio1.putInValuePortA( 0, 0xFF );
	      this.keyboardUsed = true;
	    }
	  }
	  rv = this.pio1.readDataA();
	  break;

	case 0xE9:
	  this.pio1.putInValuePortB( this.graphicKeyState ? 0 : 0x04, 0x04 );
	  rv = this.pio1.readDataB();
	  break;

	case 0xEA:
	  rv = this.pio1.readControlA();
	  break;

	case 0xEB:
	  rv = this.pio1.readControlB();
	  break;

	case 0xF8:
	case 0xF9:
	case 0xFA:
	case 0xFB:
	  rv = this.ctc.read( port & 0x03, tStates );
	  break;
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio1.reset( true );
      this.pio2.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio1.reset( false );
      this.pio2.reset( false );
    }
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.fdc != null ) {
      this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    }
    if( this.fdDrives != null ) {
      for( int i = 0; i < this.fdDrives.length; i++ ) {
	FloppyDiskDrive drive = this.fdDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    this.curFDDrive         = null;
    this.osRomEnabled       = true;
    this.bit7InverseMode    = false;
    this.screenInverseMode  = false;
    this.loudspeakerEnabled = false;
    this.tapeInPhase        = this.emuThread.readTapeInPhase();
    this.keyboardUsed       = false;
    this.joystickSelected   = false;
    this.graphicKeyState    = false;
    this.hiRes              = false;
    this.joystickValue      = 0x1F;
    this.keyboardValue      = 0;
    this.fontOffset         = 0;
    this.lineCounter        = 0;
    this.lineTStateCounter  = 0;
    this.videoPixelAddr     = 0xC000;
    this.videoTextAddr      = 0xC000;
    if( this.osBytes == scchMon91 ) {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_INTERN;
    } else {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_EXTERN;
    }
    this.screenFrm.fireUpdScreenTextActionsEnabled();
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      (new SaveDlg(
		this.screenFrm,
		0x60F7,
		endAddr,
		"LLC2-BASIC-Programm speichern",
		SaveDlg.BasicType.MS_DERIVED_BASIC,
		EmuUtil.getBasicFileFilter() )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.fdDrives != null ) {
      if( (idx >= 0) && (idx < this.fdDrives.length) ) {
	this.fdDrives[ idx ] = drive;
      }
    }
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( this.joystickEnabled && (joyNum == 0) ) {
      int value = 0x1F;
      if( (actionMask & JoystickThread.UP_MASK) != 0 ) {
	value &= ~0x01;
      }
      if( (actionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value &= ~0x02;
      }
      if( (actionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value &= ~0x04;
      }
      if( (actionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value &= ~0x08;
      }
      if( (actionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	value &= ~0x10;
      }
      this.joystickValue = value;
      synchronized( this.pio1 ) {
	if( this.joystickSelected ) {
	  this.pio1.putInValuePortA( this.joystickValue, 0xFF );
	}
      }
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    int     status = setScchMemByte( addr, value );
    boolean done   = (status >= 0);
    boolean rv     = (status > 0);
    if( !done && (!this.osRomEnabled || (addr >= 0xC000)) ) {
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


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != llc2Font;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPrinter()
  {
    return true;
  }


  @Override
  public boolean supportsRAMFloppy1()
  {
    return this.ramFloppy1 != null;
  }


  @Override
  public boolean supportsRAMFloppy2()
  {
    return this.ramFloppy2 != null;
  }


  @Override
  public boolean supportsSaveBasic()
  {
    return true;
  }


  @Override
  public boolean supportsSoundOutMono()
  {
    return true;
  }


  @Override
  public boolean supportsTapeIn()
  {
    return true;
  }


  @Override
  public boolean supportsTapeOut()
  {
    return true;
  }


  @Override
  public void updDebugScreen()
  {
    for( int y = 0; y < 0x100; y++ ) {
      fillScreenBufLine( y );
    }
  }


  @Override
  public void updSysCells(
			int        begAddr,
			int        len,
			FileFormat fileFmt,
			int        fileType )
  {
    if( fileFmt != null ) {
      if( (fileFmt.equals( FileFormat.BASIC_PRG )
		&& (begAddr == 0x60F7)
		&& (len > 7))
	  || (fileFmt.equals( FileFormat.HEADERSAVE )
		&& (fileType == 'B')
		&& (begAddr <= 0x60F7)
		&& ((begAddr + len) > 0x60FE)) )
      {
	int topAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x60F7 );
	this.emuThread.setMemWord( 0x60D2, topAddr );
	this.emuThread.setMemWord( 0x60D4, topAddr );
	this.emuThread.setMemWord( 0x60D6, topAddr );
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      this.gide.write( port, value );
    } else if( ((port & 0xF8) == 0xD0) && (this.ramFloppy1 != null) ) {
      this.ramFloppy1.writeByte( port & 0x07, value );
    } else if( ((port & 0xF8) == 0xB0) && (this.ramFloppy2 != null) ) {
      this.ramFloppy2.writeByte( port & 0x07, value );
    } else {
      boolean dirty = false;
      switch( port & 0xFF ) {
	case 0xA1:
	  if( this.fdc != null ) {
	    this.fdc.write( value );
	  }
	  break;

	case 0xA2:
	case 0xA3:
	  if( this.fdc != null ) {
	    this.fdc.writeDMA( value );
	  }
	  break;

	case 0xA6:
	case 0xA7:
	  if( this.fdDrives != null ) {
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
	  if( this.fdc != null ) {
	    this.fdc.fireTC();
	  }
	  break;

	case 0xC0:
	case 0xC1:
	case 0xC2:
	case 0xC3:
	  if( this.kcNet != null ) {
	    this.kcNet.write( port, value );
	  }
	  break;

	case 0xDC:
	case 0xDD:
	case 0xDE:
	case 0xDF:
	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    this.vdip.write( port, value );
	  }
	  break;

	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.osRomEnabled = false;
	  break;

	case 0xE4:
	  this.pio2.writeDataA( value );
	  synchronized( this ) {
	    boolean state = ((this.pio2.fetchOutValuePortA( false )
							& 0x02) != 0);
	    /*
	     * fallende Flanke: Wenn gerade keine Ausgabe laeuft,
	     * dann beginnt jetzt eine.
	     */
	    if( !state && this.v24BitOut && (this.v24BitNum == 0) ) {
	      this.v24ShiftBuf      = 0;
	      this.v24TStateCounter = 3 * this.v24TStatesPerBit / 2;
	      this.v24BitNum++;
	    }
	    this.v24BitOut = state;
	  }
	  break;

	case 0xE5:
	  this.pio2.writeDataB( value );
	  break;

	case 0xE6:
	  this.pio2.writeControlA( value );
	  break;

	case 0xE7:
	  this.pio2.writeControlB( value );
	  break;

	case 0xE8:
	  this.pio1.writeDataA( value );
	  break;

	case 0xE9:
	  this.pio1.writeDataB( value );
	  synchronized( this.pio1 ) {
	    int v             = this.pio1.fetchOutValuePortB( false );
	    this.tapeOutPhase = ((v & 0x01) != 0);
	    if( this.fontBytes != llc2Font ) {
	      if( this.fontBytes.length > 0x0800 ) {
		if( (v & 0x08) != 0 ) {
		  if( this.fontBytes.length > 0x1000 ) {
		    this.fontOffset = 0x1000;
		  } else {
		    this.fontOffset = 0x0800;
		  }
		} else {
		  this.fontOffset = 0;
		}
	      }
	    }
	    boolean b = ((v & 0x20) != 0);
            if( b != this.bit7InverseMode ) {
              this.bit7InverseMode = b;
              dirty                = true;
            }
	    b = ((v & 0x40) != 0);
	    if( b != this.loudspeakerEnabled ) {
	      this.loudspeakerEnabled = b;
	      this.soundOutPhase      = !this.soundOutPhase;
	    }
	    if( this.joystickEnabled ) {
	      b = ((v & 0x10) == 0);
	      if( b != this.joystickSelected ) {
		this.joystickSelected = b;
		this.pio1.putInValuePortA(
			b ? this.joystickValue : this.keyboardValue,
			0xFF );
	      }
	    }
	  }
	  break;

	case 0xEA:
	  this.pio1.writeControlA( value );
	  break;

	case 0xEB:
	  this.pio1.writeControlB( value );
	  break;

	case 0xEC:
	  this.scchPrgXRomEnabled  = ((value & 0x01) != 0);
	  this.scchBasicRomEnabled = ((value & 0x02) != 0);
	  this.scchRomdiskEnabled  = ((value & 0x08) != 0);
	  if( this.scchRomdiskEnabled ) {
	    this.scchRomdiskBankAddr =
			(((value & 0x01) | ((value >> 3) & 0x0E)) << 14);
	    this.scchPrgXRomEnabled = false;
	  } else {
	    this.scchPrgXRomEnabled = ((value & 0x01) != 0);
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
	    if( vAddr != this.videoPixelAddr ) {
	      this.videoPixelAddr = vAddr;
	      dirty               = true;
	    }
	    boolean hiRes = ((value & 0x40) != 0);
	    if( hiRes != this.hiRes ) {
	      this.hiRes = hiRes;
	      dirty      = true;
	      this.screenFrm.fireUpdScreenTextActionsEnabled();
	    }
	  }
	  break;

	case 0xF8:
	case 0xF9:
	case 0xFA:
	case 0xFB:
	  this.ctc.write( port & 0x03, value, tStates );
	  break;
      }
      if( dirty ) {
	this.screenFrm.setScreenDirty( true );
      }
    }
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );

    // KHz * 1000 / 50 / 312
    this.tStatesPerLine = cpu.getMaxSpeedKHz() * 20 / 312;
    if( this.fdc != null ) {
      this.fdc.z80MaxSpeedChanged( cpu );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
  }


  @Override
  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    if( this.tStatesPerLine > 0 ) {
      this.lineTStateCounter += tStates;
      if( this.lineTStateCounter >= this.tStatesPerLine ) {
	this.lineTStateCounter -= this.tStatesPerLine;
	fillScreenBufLine( this.lineCounter - 31 );
	this.lineCounter++;
	if( this.lineCounter >= 312 ) {
	  this.lineCounter = 0;
	  this.ctc.externalUpdate( 2, 1 );
	  this.screenFrm.setScreenDirty( true );
	}
      }
    }
    if( this.v24BitNum > 0 ) {
      synchronized( this ) {
	this.v24TStateCounter -= tStates;
	if( this.v24TStateCounter < 0 ) {
	  if( this.v24BitNum > 8 ) {
	    this.emuThread.getPrintMngr().putByte( this.v24ShiftBuf );
	    this.v24BitNum = 0;
	  } else {
	    this.v24ShiftBuf >>= 1;
	    if( this.v24BitOut ) {
	      this.v24ShiftBuf |= 0x80;
	    }
	    this.v24TStateCounter = this.v24TStatesPerBit;
	    this.v24BitNum++;
	  }
	}
      }
    }
    boolean phase = this.emuThread.readTapeInPhase();
    if( phase != this.tapeInPhase ) {
      this.tapeInPhase = phase;
      this.pio1.putInValuePortB( this.tapeInPhase ? 0x02 : 0, 0x02 );
    }
  }


	/* --- private Methoden --- */

  private void fillScreenBufLine( int y )
  {
    if( (y >= 0) && (y < 256) ) {
      int row  = y / 8;
      int rPix = y % 8;
      if( this.hiRes ) {
	for( int col = 0; col < 64; col++ ) {
	  int b = this.emuThread.getRAMByte( this.videoPixelAddr
						+ (rPix * 0x0800)
						+ (row * 64) + col );
	  if( this.screenInverseMode ) {
	    b = ~b;
	  }
	  this.screenBuf[ (y * 64) + col ] = (byte) b;
	}
      } else {
	boolean inverse   = false;
	byte[]  fontBytes = null;
	if( !this.hiRes ) {
	  fontBytes = this.fontBytes;
	}
	for( int col = 0; col < 64; col++ ) {
	  int b   = 0;
	  int ch  = this.emuThread.getRAMByte(
			this.videoTextAddr + (row * 64) + col );
	  if( ch == 0x10 ) {
	    inverse = false;
	  } else if( ch == 0x11 ) {
	    inverse = true;
	  }
	  if( fontBytes != null ) {
	    int idx = this.fontOffset
			+ ((ch & (this.bit7InverseMode ? 0x7F : 0xFF)) * 8)
			+ rPix;
	    if( (idx >= 0) && (idx < fontBytes.length) ) {
	      int m = 0x80;
	      int f = fontBytes[ idx ];
	      for( int i = 0; i < 8; i++ ) {
		if( (f & m) != 0 ) {
		  b |= m;
		}
		m >>= 1;
	      }
	    }
	  }
	  if( (inverse || (this.bit7InverseMode && ((ch & 0x80) != 0)))
					  != this.screenInverseMode )
	  {
	    b = ~b;
	  }
	  this.screenBuf[ (y * 64) + col ] = (byte) b;
	}
      }
    }
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE, 0x2000 );
    if( this.fontBytes == null ) {
      if( llc2Font == null ) {
	llc2Font = readResource( "/rom/llc2/llc2font.bin" );
      }
      this.fontBytes = llc2Font;
    }
  }


  private boolean updScreenRatio( Properties props )
  {
    boolean changed = false;
    boolean mode43  = EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_SCREEN_RATIO ).equals(
					VALUE_SCREEN_RATIO_43 );
    if( mode43 != (this.screenImg != null) ) {
      if( mode43 ) {
	this.screenImg = new BufferedImage(
				512,
				256,
				BufferedImage.TYPE_INT_RGB );
      } else {
	this.screenImg = null;
      }
      changed = true;
    }
    return changed;
  }
}
