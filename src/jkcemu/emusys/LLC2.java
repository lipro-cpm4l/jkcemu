/*
 * (c) 2009-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC2
 */

package jkcemu.emusys;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.ac1_llc2.AbstractSCCHSys;
import jkcemu.etc.VDIP;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.text.TextUtil;
import z80emu.*;


public class LLC2
		extends AbstractSCCHSys
		implements
			FDC8272.DriveSelector,
			Z80CTCListener,
			Z80TStatesListener
{
  /*
   * Der im Emulator integrierte SCCH-Monitor V9.1 enthaelt eine Routine
   * zur seriellen Ausgabe von Zeichen ueber Bit 2 des IO-Ports E4h (PIO 2).
   * In der Standardeinstellung soll diese Ausabe mit 9600 Bit/s erfolgen,
   * was bei einer Taktfrequenz von 3 MHz 312,5 Takte pro Bit entspricht.
   * Tatsaechlich werden aber in der Standardeinstellung pro Bit
   * 4 Ausgabe-Befehle auf dem Port getaetigt, die zusammen
   * ca. 336 bis 339 Takte benoetigen, was etwa 8900 Bit/s entspricht.
   * Damit im Emulator die Ausgabe auf den emulierten Drucker ueber diese
   * serielle Schnittstelle funktioniert,
   * wird somit der Drucker ebenfalls mit dieser Bitrate emuliert.
   * Ist allerdings eine externe ROM-Datei als Monitorprogramm eingebunden,
   * werden 9600 Baud emuliert.
   */
  private static int V24_TSTATES_PER_BIT_INTERN = 337;
  private static int V24_TSTATES_PER_BIT_EXTERN = 312;

  private static byte[] llc2Font  = null;
  private static byte[] scchMon91 = null;
  private static byte[] gsbasic   = null;

  private Z80CTC            ctc;
  private Z80PIO            pio2;
  private GIDE              gide;
  private FDC8272           fdc;
  private RAMFloppy         ramFloppy1;
  private RAMFloppy         ramFloppy2;
  private byte[]            ramModule3;
  private byte[]            fontBytes;
  private byte[]            osBytes;
  private byte[]            basicBytes;
  private byte[]            prgXBytes;
  private byte[]            romdiskBytes;
  private String            osFile;
  private String            basicFile;
  private String            prgXFile;
  private String            romdiskFile;
  private boolean           romEnabled;
  private boolean           romdiskEnabled;
  private boolean           romdiskA15;
  private boolean           prgXEnabled;
  private boolean           basicEnabled;
  private boolean           bit7InverseMode;
  private boolean           screenInverseMode;
  private boolean           loudspeakerEnabled;
  private boolean           loudspeakerPhase;
  private boolean           audioOutPhase;
  private boolean           keyboardUsed;
  private volatile boolean  graphicKeyState;
  private boolean           hiRes;
  private boolean           rf32KActive;
  private boolean           rf32NegA15;
  private boolean           rfReadEnabled;
  private boolean           rfWriteEnabled;
  private int               rfAddr16to19;
  private int               fontOffset;
  private int               videoPixelAddr;
  private int               videoTextAddr;
  private boolean           v24BitOut;
  private int               v24BitNum;
  private int               v24ShiftBuf;
  private int               v24TStateCounter;
  private int               v24TStatesPerBit;
  private int               romdiskBankAddr;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private KCNet             kcNet;
  private VDIP              vdip;


  public LLC2( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.osBytes         = null;
    this.osFile          = null;
    this.basicBytes      = null;
    this.basicFile       = null;
    this.prgXBytes       = null;
    this.prgXFile        = null;
    this.romdiskBytes    = null;
    this.romdiskFile     = null;
    this.romdiskBankAddr = 0;

    this.ramModule3 = this.emuThread.getExtendedRAM( 0x100000 );  // 1MByte

    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"LLC2",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an IO-Adressen D0h-D7h",
				props,
				"jkcemu.llc2.ramfloppy.1." );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				"LLC2",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an IO-Adressen B0h-B7h",
				props,
				"jkcemu.llc2.ramfloppy.2." );

    this.curFDDrive = null;
    this.fdDrives   = null;
    this.fdc        = null;
    if( emulatesFloppyDisk( props ) ) {
      this.fdDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.fdDrives, null );
      this.fdc = new FDC8272( this, 4 );
    }

    this.ctc   = new Z80CTC( "CTC (IO-Adressen F8-FB)" );
    this.pio1  = new Z80PIO( "PIO (IO-Adressen E8-EB)" );
    this.pio2  = new Z80PIO( "V24-PIO (IO-Adressen E4-E7)" );

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (IO-Adressen C0-C3)" );
    }

    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP( "USB-PIO (IO-Adressen FC-FF)" );
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, "jkcemu.llc2." );

    this.joystickEnabled = emulatesJoystick( props );

    java.util.List<Z80InterruptSource> iSources
				= new ArrayList<Z80InterruptSource>();
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
    cpu.addTStatesListener( this );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
      cpu.addMaxSpeedListener( this.fdc );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
      cpu.addMaxSpeedListener( this.kcNet );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
  }


  public static int getDefaultSpeedKHz()
  {
    return 3000;
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
	this.loudspeakerPhase = !this.loudspeakerPhase;
	if( this.emuThread.isSoundOutEnabled() ) {
	  this.emuThread.writeAudioPhase( this.loudspeakerPhase );
	}
      }
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
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
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    loadFont( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
				props,
				"jkcemu.system" ).equals( "LLC2" );
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty( props,  "jkcemu.llc2.os.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.basicFile,
		EmuUtil.getProperty( props,  "jkcemu.llc2.basic.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.prgXFile,
		EmuUtil.getProperty( props,  "jkcemu.llc2.program_x.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.romdiskFile,
		EmuUtil.getProperty( props,  "jkcemu.llc2.romdisk.file" ) );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			"LLC2",
			RAMFloppy.RFType.MP_3_1988,
			props,
			"jkcemu.llc2.ramfloppy.1." );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			"LLC2",
			RAMFloppy.RFType.MP_3_1988,
			props,
			"jkcemu.llc2.ramfloppy.2." );
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, "jkcemu.llc2." );
    }
    if( rv && emulatesFloppyDisk( props ) != (this.fdc != null) ) {
      rv = false;
    }
    if( rv && (emulatesJoystick( props ) != this.joystickEnabled) ) {
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
    return true;
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
    cpu.setInterruptSources( (Z80InterruptSource[]) null );

    if( this.fdc != null ) {
      cpu.removeMaxSpeedListener( this.fdc );
      this.fdc.die();
    }
    if( this.kcNet != null ) {
      cpu.removeMaxSpeedListener( this.kcNet );
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
  public int getCharColCount()
  {
    return 64;
  }


  @Override
  public int getCharHeight()
  {
    return 8;
  }


  @Override
  public int getCharRowCount()
  {
    return 32;
  }


  @Override
  public int getCharRowHeight()
  {
    return 8;
  }


  @Override
  public int getCharWidth()
  {
    return 8;
  }


  @Override
  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return false;
  }


  @Override
  public int getDefaultFloppyDiskBlockSize()
  {
    return 2048;
  }


  @Override
  public int getDefaultFloppyDiskDirBlocks()
  {
    return 1;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.getFormat( 1, 80, 5, 1024 );
  }


  @Override
  public int getDefaultFloppyDiskSystemTracks()
  {
    return 0;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 50;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 150;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 50;
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

    int     rv   = 0xFF;
    boolean done = false;
    if( !m1 && this.rfReadEnabled && (this.ramModule3 != null) ) {
      int idx = this.rfAddr16to19 | addr;
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	rv = (int) this.ramModule3[ idx ] & 0xFF;
      }
      done = true;
    }
    if( !done && this.rf32KActive && (this.ramModule3 != null)
	&& (addr >= 0x4000) && (addr < 0xC000) )
    {
      int idx = this.rfAddr16to19 | addr;
      if( this.rf32NegA15 ) {
	idx ^= 0x8000;
      }
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	rv = (int) this.ramModule3[ idx ] & 0xFF;
      }
      done = true;
    }
    if( !done && this.basicEnabled
	&& (addr >= 0x4000) && (addr < 0x6000) )
    {
      if( this.basicBytes != null ) {
	int idx = addr - 0x4000;
	if( idx < this.basicBytes.length ) {
	  rv = (int) this.basicBytes[ idx ] & 0xFF;
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
      if( this.osBytes != null ) {
	if( addr < this.osBytes.length ) {
	  rv = (int) this.osBytes[ addr ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  @Override
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


  @Override
  public int getScreenHeight()
  {
    return 256;
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
    return true;
  }


  @Override
  public String getTitle()
  {
    return "LLC2";
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
    int     ch = 0;
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

      case KeyEvent.VK_LEFT:
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

      case KeyEvent.VK_BACK_SPACE:
	ch = (this.graphicKeyState ? 8 : 0x7F);
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    setKeyboardValue( 0 );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    byte[] fontBytes = null;
    if( !this.hiRes ) {
      fontBytes = this.fontBytes;
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
	  } else if( fontBytes != null ) {
	    int ch = this.emuThread.getRAMByte(
				this.videoTextAddr + (row * 64) + col );
	    if( ch == 0x10 ) {
	      inverse = false;
	    } else if( ch == 0x11 ) {
	      inverse = true;
	    }
	    int idx = this.fontOffset
			+ ((ch & (this.bit7InverseMode ? 0x7F : 0xFF)) * 8)
			+ (y % 8);
	    if( (idx >= 0) && (idx < fontBytes.length) ) {
	      int m = 0x80;
	      int n = x % 8;
	      if( n > 0 ) {
		m >>= n;
	      }
	      pixel = ((fontBytes[ idx ] & m) != 0);
	      if( (inverse || (this.bit7InverseMode && ((ch & 0x80) != 0)))
					!= this.screenInverseMode )
	      {
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


  @Override
  public int readIOByte( int port )
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

	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.romEnabled = false;
	  break;

	case 0xE4:
	  // V24: CTS=L (empfangsbereit)
	  this.pio2.putInValuePortA( 0, 0x04 );
	  rv = this.pio2.readPortA();
	  break;

	case 0xE5:
	  rv = this.pio2.readPortB();
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
	  rv = this.pio1.readPortA();
	  break;

	case 0xE9:
	  {
	    int v = 0x04;		// PIO B2: Grafiktaste, L-aktiv
	    if( this.graphicKeyState ) {
	      v = 0;
	    }
	    if( this.emuThread.readAudioPhase() ) {
	      v |= 0x02;
	    }
	    this.pio1.putInValuePortB( v, 0x06 );
	  }
	  rv = this.pio1.readPortB();
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
	  rv = this.ctc.read( port & 0x03 );
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    rv = this.vdip.read( port );
	  }
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
    this.romEnabled         = true;
    this.romdiskEnabled     = false;
    this.romdiskA15         = false;
    this.prgXEnabled        = false;
    this.basicEnabled       = false;
    this.bit7InverseMode    = false;
    this.screenInverseMode  = false;
    this.loudspeakerEnabled = false;
    this.loudspeakerPhase   = false;
    this.audioOutPhase      = false;
    this.keyboardUsed       = false;
    this.joystickSelected   = false;
    this.graphicKeyState    = false;
    this.hiRes              = false;
    this.rf32KActive        = false;
    this.rf32NegA15         = false;
    this.rfReadEnabled      = false;
    this.rfWriteEnabled     = false;
    this.rfAddr16to19       = 0;
    this.joystickValue      = 0x1F;
    this.keyboardValue      = 0;
    this.fontOffset         = 0;
    this.videoPixelAddr     = 0xC000;
    this.videoTextAddr      = 0xC000;
    this.v24BitOut          = true;	// V24: H-Pegel
    this.v24BitNum          = 0;
    this.v24ShiftBuf        = 0;
    this.v24TStateCounter   = 0;
    if( this.osBytes == scchMon91 ) {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_INTERN;
    } else {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_EXTERN;
    }
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      (new SaveDlg(
		this.screenFrm,
		0x6000,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		false,		// kein RBASIC
		"LLC2-BASIC-Programm speichern" )).setVisible( true );
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

    boolean rv   = false;
    boolean done = false;
    if( this.rfWriteEnabled && (this.ramModule3 != null) ) {
      int idx = this.rfAddr16to19 | addr;
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	this.ramModule3[ idx ] = (byte) value;
	rv = true;
      }
      done = true;
    }
    if( !done && this.rf32KActive && (this.ramModule3 != null)
	&& (addr >= 0x4000) && (addr < 0xC000) )
    {
      int idx = this.rfAddr16to19 | addr;
      if( this.rf32NegA15 ) {
	idx ^= 0x8000;
      }
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	this.ramModule3[ idx ] = (byte) value;
	rv = true;
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


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != llc2Font;
  }


  @Override
  public boolean supportsAudio()
  {
    return true;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
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


  @Override
  public void writeIOByte( int port, int value )
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

	case 0xE0:
	case 0xE1:
	case 0xE2:
	case 0xE3:
	  this.romEnabled = false;
	  break;

	case 0xE4:
	  this.pio2.writePortA( value );
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
	  this.pio2.writePortB( value );
	  break;

	case 0xE6:
	  this.pio2.writeControlA( value );
	  break;

	case 0xE7:
	  this.pio2.writeControlB( value );
	  break;

	case 0xE8:
	  this.pio1.writePortA( value );
	  break;

	case 0xE9:
	  this.pio1.writePortB( value );
	  synchronized( this.pio1 ) {
	    int     v = this.pio1.fetchOutValuePortB( false );
	    boolean b = ((v & 0x01) != 0);
	    if( b != this.audioOutPhase ) {
	      this.audioOutPhase = b;
	      if( !this.emuThread.isSoundOutEnabled() ) {
		this.emuThread.writeAudioPhase( b );
	      }
	    }
	    if( this.fontBytes != llc2Font ) {
	      if( this.fontBytes.length > 0x0800 ) {
		b = ((v & 0x08) != 0);
		this.fontOffset = (b ? 0x0800 : 0);
	      }
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
	    b = ((v & 0x20) != 0);
            if( b != this.bit7InverseMode ) {
              this.bit7InverseMode = b;
              dirty                = true;
            }
	    b = ((v & 0x40) != 0);
	    if( b != this.loudspeakerEnabled ) {
	      this.loudspeakerEnabled = b;
	      if( this.emuThread.isSoundOutEnabled() ) {
		this.loudspeakerPhase = !this.loudspeakerPhase;
		this.emuThread.writeAudioPhase( this.loudspeakerPhase );
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
	  this.prgXEnabled    = ((value & 0x01) != 0);
	  this.basicEnabled   = ((value & 0x02) != 0);
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
	    if( vAddr != this.videoPixelAddr ) {
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

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    this.vdip.write( port, value );
	  }
	  break;
      }
      if( dirty ) {
	this.screenFrm.setScreenDirty( true );
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.llc2.floppydisk.enabled",
			false );
  }


  private static boolean emulatesJoystick( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.llc2.joystick.enabled",
			false );
  }


  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.llc2.kcnet.enabled",
				false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.llc2.vdip.enabled",
				false );
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				"jkcemu.llc2.font.file", 0x1000 );
    if( this.fontBytes == null ) {
      if( llc2Font == null ) {
	llc2Font = readResource( "/rom/llc2/llc2font.bin" );
      }
      this.fontBytes = llc2Font;
    }
  }


  private void loadROMs( Properties props )
  {
    // OS-ROM
    this.osFile  = EmuUtil.getProperty( props, "jkcemu.llc2.os.file" );
    this.osBytes = readFile( this.osFile, 0x1000, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( scchMon91 == null ) {
	scchMon91 = readResource( "/rom/llc2/scchmon_91g.bin" );
      }
      this.osBytes = scchMon91;
    }

    // BASIC-ROM
    this.basicFile  = EmuUtil.getProperty( props, "jkcemu.llc2.basic.file" );
    this.basicBytes = readFile( this.basicFile, 0x2000, "BASIC" );
    if( this.basicBytes == null ) {
      if( gsbasic == null ) {
	gsbasic = readResource( "/rom/llc2/gsbasic.bin" );
      }
      this.basicBytes = gsbasic;
    }

    // Programmpaket X
    this.prgXFile = EmuUtil.getProperty(
				props,
				"jkcemu.llc2.program_x.file" );
    this.prgXBytes = readFile( this.prgXFile, 0x2000, "Programmpaket X" );

    // ROM-Disk
    this.romdiskFile = EmuUtil.getProperty(
				props,
				"jkcemu.llc2.romdisk.file" );
    this.romdiskBytes = readFile( this.romdiskFile, 0x40000, "ROM-Disk" );

    // Zeichensatz
    loadFont( props );
  }
}
