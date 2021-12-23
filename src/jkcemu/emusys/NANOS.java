/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des NANOS-Systems
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.audio.AbstractSoundDevice;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.disk.GIDE;
import jkcemu.etc.GraphicPoppe;
import jkcemu.etc.K1520Sound;
import jkcemu.net.KCNet;
import jkcemu.text.TextUtil;
import jkcemu.usb.VDIP;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;
import z80emu.Z80SIO;
import z80emu.Z80SIOChannelListener;


public class NANOS extends EmuSys implements
					FDC8272.DriveSelector,
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80PIOPortListener,
					Z80SIOChannelListener
{
  public static final String SYSNAME     = "NANOS";
  public static final String PROP_PREFIX = "jkcemu.nanos.";

  public static final String PROP_KEYBOARD            = "keyboard";
  public static final String PROP_FONT_8X6_PREFIX     = "font.8x6.";
  public static final String PROP_FONT_8X8_PREFIX     = "font.8x8.";
  public static final String PROP_ROM                 = "rom";
  public static final String PROP_KEYBOARD_SWAP_CASE  = "swap_case";
  public static final String PROP_GRAPHIC             = "graphic";

  public static final String VALUE_EPOS                 = "epos";
  public static final String VALUE_NANOS                = "nanos";
  public static final String VALUE_GRAPHIC_64X32        = "64x32";
  public static final String VALUE_GRAPHIC_80X24        = "80x24";
  public static final String VALUE_GRAPHIC_80X25        = "80x25";
  public static final String VALUE_GRAPHIC_POPPE        = "poppe";
  public static final String VALUE_KEYBOARD_PIO00A_HS   = "pio00a_hs";
  public static final String VALUE_KEYBOARD_PIO00A_BIT7 = "pio00a_bit7";
  public static final String VALUE_KEYBOARD_SIO84A      = "sio84a";

  public static final int DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 2000;


  private enum GraphicHW {
			Video2_64x32,
			Video3_80x24,
			Video3_80x25,
			Poppe };
  private enum KeyboardHW { PIO00A_HS, PIO00A_BIT7, SIO84A };

  private static FloppyDiskInfo epos20Disk64x32 =
		new FloppyDiskInfo(
			"/disks/nanos/epos21_64x32.dump.gz",
			"EPOS 2.1 Boot-Diskette (64x32 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo epos20Disk80x24 =
		new FloppyDiskInfo(
			"/disks/nanos/epos21_80x24.dump.gz",
			"EPOS 2.1 Boot-Diskette (80x24 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo nanos22Disk80x25 =
		new FloppyDiskInfo(
			"/disks/nanos/nanos22_80x25.dump.gz",
			"NANOS 2.2 Boot-Diskette",
			2, 2048, true );

  private static byte[] romEpos   = null;
  private static byte[] romNanos  = null;
  private static byte[] fontNanos = null;

  private byte[]            fontBytes;
  private byte[]            ram1000;
  private byte[]            romBytes;
  private String            romProp;
  private byte[]            ram256k;
  private Z80PIO            pio00;
  private Z80PIO            pio80;
  private Z80SIO            sio84;
  private Z80PIO            pio88;
  private Z80CTC            ctc8C;
  private K1520Sound        k1520Sound;
  private KCNet             kcNet;
  private VDIP              vdip;
  private GIDE              gide;
  private FDC8272           fdc;
  private FloppyDiskDrive[] fdDrives;
  private boolean           fdcTC;
  private boolean           bootMemEnabled;
  private boolean           swapKeyCharCase;
  private boolean           ram256kEnabled;
  private boolean           ram256kReadable;
  private int               ram256kMemBaseAddr;
  private int               ram256kRFBaseAddr;
  private long              pasteTStates;
  private GraphicPoppe      graphicPoppe;
  private GraphicHW         graphicHW;
  private KeyboardHW        keyboardHW;


  public NANOS( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.graphicHW = getGraphicHW( props );
    if( this.graphicHW == GraphicHW.Poppe ) {
      this.graphicPoppe = new GraphicPoppe( this, false, props );
      this.graphicPoppe.setScreenFrm( emuThread.getScreenFrm() );
      this.graphicPoppe.setFixedScreenSize( isFixedScreenSize( props ) );
    } else {
      this.graphicPoppe = null;
    }
    this.keyboardHW      = getKeyboardHW( props );
    this.swapKeyCharCase = false;
    this.fontBytes       = null;
    this.romBytes        = null;
    this.romProp         = null;
    this.ram1000         = new byte[ 0x0400 ];
    this.ram256k         = new byte[ 0x40000 ];
    this.fdc             = new FDC8272( this, 4 );
    this.fdDrives        = new FloppyDiskDrive[ 4 ];
    Arrays.fill( this.fdDrives, null );
    setFDCSpeed( false );

    if( emulatesK1520Sound( props ) ) {
      this.k1520Sound = new K1520Sound( this, 0xE0 );
    } else {
      this.k1520Sound = null;
    }

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (E/A-Adressen 80h-83h)" );
    }

    this.vdip = null;
    if( emulatesVDIP( props ) ) {
      this.vdip = new VDIP(
			0,
			this.emuThread.getZ80CPU(),
			"USB-PIO (E/A-Adressen 88h-8Bh)" );
      this.vdip.applySettings( props );
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, this.propPrefix );

    this.pio00 = new Z80PIO( "ZRE-PIO (E/A-Adressen 00h-03h)" );
    this.pio80 = null;
    if( this.kcNet == null ) {
      this.pio80 = new Z80PIO( "PIO (E/A-Adressen 80h-83h)" );
    }
    this.sio84 = new Z80SIO( "SIO (E/A-Adressen 84h-87h)" );
    this.pio88 = null;
    if( this.vdip == null ) {
      this.pio88 = new Z80PIO( "PIO (E/A-Adressen 88h-8Bh)" );
    }
    this.ctc8C = new Z80CTC( "CTC (E/A-Adressen 8Ch-8Fh)" );

    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
    iSources.add( this.pio00 );
    if( this.k1520Sound != null ) {
      iSources.add( this.k1520Sound );
    }
    if( this.kcNet != null ) {
      iSources.add( this.kcNet );
    } else if( this.pio80 != null ) {
      iSources.add( this.pio80 );
    }
    iSources.add( this.sio84 );
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    } else if( this.pio88 != null ) {
      iSources.add( this.pio88 );
    }
    iSources.add( this.ctc8C );

    Z80CPU cpu = emuThread.getZ80CPU();
    try {
      cpu.setInterruptSources(
	iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    this.pio00.addPIOPortListener( this, Z80PIO.PortInfo.A );
    this.pio00.addPIOPortListener( this, Z80PIO.PortInfo.B );
    this.sio84.addChannelListener( this, 1 );
    this.ctc8C.addCTCListener( this );

    applyKeyboardSettings( props );
    z80MaxSpeedChanged( cpu );
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return new FloppyDiskInfo[] {
			epos20Disk64x32,
			epos20Disk80x24,
			nanos22Disk80x25 };
  }


  public static int getDefaultSpeedKHz()
  {
    return 2457;
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.fdDrives != null ) {
      if( (driveNum >= 0) && (driveNum < this.fdDrives.length) ) {
	rv = this.fdDrives[ driveNum ];
      }
    }
    return rv;
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc8C ) {
      if( timerNum == 0 ) {
	this.sio84.clockPulseSenderA();
	this.sio84.clockPulseReceiverA();
      } else if( timerNum == 1 ) {
	this.sio84.clockPulseSenderB();
	this.sio84.clockPulseReceiverB();
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80MaxSpeedChanged( cpu );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( pio == this.pio00 ) {
      if( (port == Z80PIO.PortInfo.A)
	  && (status == Z80PIO.Status.READY_FOR_INPUT)
	  && (this.keyboardHW == KeyboardHW.PIO00A_HS)
	  && (this.pasteIter != null) )
      {
	this.pasteTStates = 50000;
      }
      else if( (port == Z80PIO.PortInfo.B)
	       && ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
		   || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
      {
	int outValue = this.pio00.fetchOutValuePortB( 0xFF );
	this.bootMemEnabled = ((outValue & 0x80) != 0);
	this.tapeOutPhase   = ((outValue & 0x40) != 0);
      }
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOByteSent( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio84) && (channel == 1) ) {
      this.emuThread.getPrintMngr().putByte( value );
      this.sio84.setClearToSendB( false );
      this.sio84.setClearToSendB( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>NANOS Konfiguration</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>ZRE-ROM/RAM:</td><td>" );
    EmuUtil.appendOnOffText( buf, this.bootMemEnabled );
    buf.append( "</td></tr>\n"
	+ "<tr><td>256K RAM:</td><td>" );
    if( this.ram256kEnabled ) {
      buf.append( this.ram256kReadable ?
				"Lesen und Schreiben"
				: "Nur Schreiben" );
    } else {
      buf.append( "aus" );
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>RAM-Bank f&uuml;r Hauptspeicher:</td><td>" );
    buf.append( this.ram256kMemBaseAddr >> 16 );
    buf.append( "</td></tr>\n"
	+ "<tr><td>RAM-Floppy Sektor-Adresse:</td><td>" );
    buf.append( String.format( "%05Xh", this.ram256kRFBaseAddr ) );
    buf.append( "</td></tr>\n" );
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.appendStatusHTMLTo( buf );
    }
    buf.append( "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    applyKeyboardSettings( props );
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.setFixedScreenSize( isFixedScreenSize( props ) );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    loadFonts( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = TextUtil.equals(
		this.romProp,
		EmuUtil.getProperty( props, this.propPrefix + PROP_ROM ) );
    }
    if( rv && (this.graphicHW != getGraphicHW( props )) ) {
      rv = false;
    }
    if( rv && (this.keyboardHW != getKeyboardHW( props )) ) {
      rv = false;
    }
    if( rv && (emulatesK1520Sound( props ) != (this.k1520Sound != null)) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesVDIP( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, this.propPrefix );
    }
    return rv;
  }


  @Override
  public synchronized void cancelPastingText()
  {
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      if( this.pasteIter != null ) {
	this.pasteIter = null;
	informPastingTextStatusChanged( false );
      }
    } else {
      super.cancelPastingText();
    }
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public void die()
  {
    this.pio00.removePIOPortListener( this, Z80PIO.PortInfo.A );
    this.pio00.removePIOPortListener( this, Z80PIO.PortInfo.B );
    this.ctc8C.removeCTCListener( this );
    this.sio84.removeChannelListener( this, 0 );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    cpu.removeMaxSpeedListener( this );
    cpu.removeTStatesListener( this );
    this.fdc.die();
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.die();
    }
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
    super.die();
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0xE600;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.BLACK;
    if( this.graphicPoppe != null ) {
      color = this.graphicPoppe.getColor( colorIdx );
    } else {
      if( colorIdx > 0 ) {
	color = this.colorWhite;
      }
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return this.graphicPoppe != null ?
			this.graphicPoppe.getColorCount()
			: 2;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.graphicPoppe != null ) {
      rv = this.graphicPoppe.getColorIndex( x, y );
    } else {
      if( this.fontBytes != null ) {
	int pixPerRow     = 10;
	int colsPerRow    = 80;
	int charsOnScreen = 1920;		// 80x24
	switch( this.graphicHW ) {
	  case Video2_64x32:
	    pixPerRow     = 8;
	    colsPerRow    = 64;
	    charsOnScreen = 2048;
	    break;
	  case Video3_80x25:
	    charsOnScreen = 2000;
	    break;
	}
	int rPix = y % pixPerRow;
	int row  = y / pixPerRow;
	int col  = x / 8;
	if( rPix < 8 ) {
	  int mIdx = (row * colsPerRow) + col;
	  if( (mIdx >= 0) && (mIdx < charsOnScreen) ) {
	    int ch   = this.emuThread.getRAMByte( mIdx + 0xF800 );
	    int fIdx = (ch * 8) + rPix;
	    if( (fIdx >= 0) && (fIdx < this.fontBytes.length ) ) {
	      int m = 0x80;
	      int n = x % 8;
	      if( n > 0 ) {
		m >>= n;
	      }
	      if( (this.fontBytes[ fIdx ] & m) != 0 ) {
		rv = WHITE;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster rv = null;
    switch( this.graphicHW ) {
      case Video2_64x32:
	rv = new CharRaster( 64, 32, 8, 8, 8 );
	break;
      case Video3_80x24:
	rv = new CharRaster( 80, 24, 10, 8, 8 );
	break;
      case Video3_80x25:
	rv = new CharRaster( 80, 25, 10, 8, 8 );
	break;
      case Poppe:
	if( this.graphicPoppe != null ) {
	  rv = this.graphicPoppe.getCurScreenCharRaster();
	}
	break;
    }
    return rv;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_780K;
  }


  @Override
  public int getDefaultPromptAfterResetMillisMax()
  {
    return DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX;
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
    return "/help/nanos.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( this.bootMemEnabled && (addr < 0x1400) ) {
      if( (addr < 0x1000) && (this.romBytes != null) ) {
	if( addr < this.romBytes.length ) {
	  rv = (int) this.romBytes[ addr ] & 0xFF;
	}
      }
      if( (addr >= 0x1000) && (addr < 0x1400) ) {
	int idx = addr - 0x1000;
	if( idx < this.ram1000.length ) {
	  rv = (int) this.ram1000[ idx ] & 0xFF;
	}
      }
    } else if( (addr >= 0xF800) && (this.graphicPoppe != null) ) {
      rv = this.graphicPoppe.readMemByte( addr );
    } else {
      if( this.ram256kEnabled && this.ram256kReadable ) {
	if( (addr >= 0xF700) && (addr < 0xF800) ) {
	  int idx = this.ram256kRFBaseAddr | ((addr - 0xF700) & 0x000FF);
	  if( idx < this.ram256k.length ) {
	    rv = (int) this.ram256k[ idx ] & 0xFF;
	  }
	} else {
	  int idx = this.ram256kMemBaseAddr | addr;
	  if( idx < this.ram256k.length ) {
	    rv = this.ram256k[ idx ] & 0xFF;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int    ch        = -1;
    byte[] fontBytes = null;
    if( this.graphicPoppe != null ) {
      ch        = this.graphicPoppe.getScreenChar( chRaster, chX, chY );
      fontBytes = this.graphicPoppe.getCurFontBytes();
    } else {
      int nCols = 0;
      int nRows = 0;
      switch( this.graphicHW ) {
	case Video2_64x32:
	  nCols = 64;
	  nRows = 32;
	  break;
	case Video3_80x24:
	  nCols = 80;
	  nRows = 24;
	  break;
	case Video3_80x25:
	  nCols = 80;
	  nRows = 25;
	  break;
      }
      fontBytes = this.fontBytes;
      if( (chX >= 0) && (chX < nCols) && (chY >= 0) && (chY < nRows) ) {
	ch = this.emuThread.getRAMByte( 0xF800 + ((chY * nCols) + chX) );
      }
    }
    if( fontBytes == fontNanos ) {
      switch( ch ) {
	case 0x5C:
	  ch = '\u0278';
	  break;
	case 0x5E:
	  ch = '\u00AC';
	  break;
	case 0x60:
	  ch = '\\';
	  break;
	default:
	  if( (ch < 0x20) && (ch > 0x7E) ) {
	    ch = -1;
	  }
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    int rv = 0;
    switch( this.graphicHW ) {
      case Video2_64x32:
	rv = 256;
	break;
      case Video3_80x24:
	rv = 240;
	break;
      case Video3_80x25:
	rv = 250;
	break;
      case Poppe:
	if( this.graphicPoppe != null ) {
	  rv = this.graphicPoppe.getScreenHeight();
	}
	break;
    }
    return rv;
  }


  @Override
  public int getScreenWidth()
  {
    int rv = 640;
    switch( this.graphicHW ) {
      case Video2_64x32:
	rv = 512;
	break;
      case Poppe:
	if( this.graphicPoppe != null ) {
	  rv = this.graphicPoppe.getScreenWidth();
	}
	break;
    }
    return rv;
  }


  @Override
  public AbstractSoundDevice[] getSoundDevices()
  {
    return this.k1520Sound != null ?
	new AbstractSoundDevice[] { this.k1520Sound.getSoundDevice() }
	: super.getSoundDevices();
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    FloppyDiskInfo[] rv = null;
    if( (this.romBytes == romEpos)
	&& (this.keyboardHW == KeyboardHW.PIO00A_BIT7) )
    {
      if( this.graphicHW == GraphicHW.Video2_64x32 ) {
	rv = new FloppyDiskInfo[] { epos20Disk64x32 };
      }
      else if( this.graphicHW == GraphicHW.Video3_80x24 ) {
	rv = new FloppyDiskInfo[] { epos20Disk80x24 };
      }
      else if( this.graphicHW == GraphicHW.Poppe ) {
	rv = new FloppyDiskInfo[] { epos20Disk64x32, epos20Disk80x24 };

      }
    }
    else if( (this.romBytes == romNanos)
	     && (this.keyboardHW == KeyboardHW.PIO00A_HS)
	     && (this.graphicHW == GraphicHW.Video3_80x25) )
    {
      rv = new FloppyDiskInfo[] { nanos22Disk80x25 };
    }
    return rv;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives.length;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return this.swapKeyCharCase;
  }


  @Override
  public String getTitle()
  {
    return SYSNAME;
  }


  @Override
  public VDIP[] getVDIPs()
  {
    return this.vdip != null ?
			new VDIP[] { this.vdip }
			: super.getVDIPs();
  }


  @Override
  public boolean isPastingText()
  {
    return this.keyboardHW == KeyboardHW.PIO00A_HS ?
					(this.pasteIter != null)
					: super.isPastingText();
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
      rv = keyTyped( (char) ch );
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    putKeyChar( '\u0000' );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    return putKeyChar( ch );
  }


  @Override
  public void loadROMs( Properties props )
  {
    this.romProp = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM );
    String lowerProp = this.romProp.toLowerCase();
    if( (this.romProp.length() > VALUE_PREFIX_FILE.length())
	&& lowerProp.startsWith( VALUE_PREFIX_FILE ) )
    {
      this.romBytes = readROMFile(
				this.romProp.substring( 5 ),
				0x1000,
				"ROM-Inhalt" );
    }
    if( (this.romBytes == null)
	&& lowerProp.equalsIgnoreCase( VALUE_EPOS ) )
    {
      if( romEpos == null ) {
	romEpos = readResource( "/rom/nanos/eposrom.bin" );
      }
      this.romBytes = romEpos;
    }
    if( this.romBytes == null ) {
      if( romNanos == null ) {
	romNanos = readResource( "/rom/nanos/nanosrom.bin" );
      }
      this.romBytes = romNanos;
    }
    loadFonts( props );
  }


  @Override
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      if( ch == '\n' ) {
	ch = '\r';
      }
      if( (ch > 0) && (ch < 0x7F) ) {
	while( !this.pio00.isReadyPortA() ) {
	  Thread.sleep( 50 );
	}
	rv = putKeyChar( ch );
      }
    } else if( this.keyboardHW == KeyboardHW.SIO84A ) {
      if( ch == '\n' ) {
	ch = '\r';
      }
      if( (ch > 0) && (ch < 0xFF) ) {
	while( !this.sio84.isReadyReceiverA() ) {
	  Thread.sleep( 50 );
	}
	rv = putKeyChar( ch );
      }
    } else {
      rv = super.pasteChar( ch );
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (this.kcNet != null) && ((port & 0xFC) == 0x80) ) {
      rv = this.kcNet.read( port );
    } else if( (this.vdip != null) && ((port & 0xFC) == 0x88) ) {
      rv = this.vdip.read( port );
    } else if( (this.gide != null) && ((port & 0xF0) == 0xD0) ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    } else if( (this.k1520Sound != null) && ((port & 0xF8) == 0xE0) ) {
      rv = this.k1520Sound.read( port, tStates );
    } else {
      port &= 0xFF;
      if( port < 0x80 ) {
	// PIO auf ZRE-Karte
	switch( port & 0x03 ) {
	  case 0:
	    rv = this.pio00.readDataA();
	    break;
	  case 1:
	    rv = this.pio00.readDataB();
	    break;
	}
      } else {
	switch( port ) {
	  // PIO 0 auf IO-Karte
	  case 0x80:
	    if( this.pio80 != null ) {
	      rv = this.pio80.readDataA();
	    }
	    break;
	  case 0x81:
	    if( this.pio80 != null ) {
	      rv = this.pio80.readDataB();
	    }
	    break;

	  // SIO auf IO-Karte
	  case 0x84:
	    rv = this.sio84.readDataA();
	    break;
	  case 0x85:
	    rv = this.sio84.readDataB();
	    break;
	  case 0x86:
	    rv = this.sio84.readControlA();
	    break;
	  case 0x87:
	    rv = this.sio84.readControlB();
	    break;

	  // PIO 1 auf IO-Karte
	  case 0x88:
	    if( this.pio88 != null ) {
	      rv = this.pio88.readDataA();
	    }
	    break;
	  case 0x89:
	    if( this.pio88 != null ) {
	      rv = this.pio88.readDataB();
	    }
	    break;

	  // CTC auf IO-Karte
	  case 0x8C:
	  case 0x8D:
	  case 0x8E:
	  case 0x8F:
	    rv = this.ctc8C.read( port & 0x03, tStates );
	    break;

	  // FDC-Karte
	  case 0x94:
	    rv = this.fdc.readMainStatusReg();
	    break;
	  case 0x95:
	    rv = this.fdc.readData();
	    break;

	  // Farbgrafikkarte
	  case 0xF2:
	    if( this.graphicPoppe != null ) {
	      rv = this.graphicPoppe.readIOByte( port );
	    }
	    break;
	}
      }
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    if( powerOn ) {
      initDRAM();
      initSRAM( this.ram1000, props );
      EmuUtil.initDRAM( this.ram256k );
    }
    this.fdc.reset( powerOn );
    this.pio00.reset( powerOn );
    if( this.pio80 != null ) {
      this.pio80.reset( powerOn );
    }
    this.sio84.reset( powerOn );
    this.sio84.setClearToSendA( true );
    this.sio84.setClearToSendB( true );
    if( this.pio88 != null ) {
      this.pio88.reset( powerOn );
    }
    this.ctc8C.reset( powerOn );
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.reset( powerOn );
    }
    if( this.kcNet != null ) {
      this.kcNet.reset( powerOn );
    }
    if( this.vdip != null ) {
      this.vdip.reset( powerOn );
    }
    setFDCSpeed( false );
    this.fdcTC = false;
    for( int i = 0; i < this.fdDrives.length; i++ ) {
      FloppyDiskDrive drive = this.fdDrives[ i ];
      if( drive != null ) {
	drive.reset();
      }
    }
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.reset( powerOn, props );
    }
    this.bootMemEnabled     = true;
    this.ram256kEnabled     = false;
    this.ram256kReadable    = false;
    this.ram256kMemBaseAddr = 0;
    this.ram256kRFBaseAddr  = 0;
    this.pasteTStates       = 0;

    // Initialzustand fuer Tastatur
    this.pio00.putInValuePortA( 0x00, 0xFF );
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
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( this.bootMemEnabled && (addr >= 0x1000) && (addr < 0x1400) ) {
      int idx = addr - 0x1000;
      if( idx < this.ram1000.length ) {
	this.ram1000[ idx ] = (byte) value;
      }
      rv = true;
    }
    if( this.ram256kEnabled ) {
      if( (addr >= 0xF700) && (addr < 0xF800) ) {
	int idx = this.ram256kRFBaseAddr | ((addr - 0xF700) & 0x000FF);
	if( idx < this.ram256k.length ) {
	  this.ram256k[ idx ] = (byte) value;
	}
      } else {
	int idx = this.ram256kMemBaseAddr | addr;
	if( idx < this.ram256k.length ) {
	  this.ram256k[ idx ] = (byte) value;
	}
      }
      rv = true;
    }
    if( addr >= 0xF800 ) {
      boolean done = false;
      if( this.graphicPoppe != null ) {
	this.graphicPoppe.writeMemByte( addr, value );
      }
      if( !done ) {
	this.emuThread.setRAMByte( addr, value );
      }
      this.screenFrm.setScreenDirty( true );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return (this.fontBytes != fontNanos);
  }


  public synchronized void startPastingText( String text )
  {
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      boolean done = false;
      if( text != null ) {
	if( !text.isEmpty() ) {
	  cancelPastingText();
	  informPastingTextStatusChanged( true );
	  CharacterIterator iter = new StringCharacterIterator( text );
	  if( this.pio00.isReadyPortA() ) {
	    if( putKeyChar( iter.first() ) ) {
	      this.pasteIter = iter;
	      done           = true;
	    } else {
	      fireShowCharNotPasted( iter );
	    }
	  } else {
	    done = true;
	  }
	}
      }
      if( !done ) {
	informPastingTextStatusChanged( false );
      }
    } else {
      super.startPastingText( text );
    }
  }


  @Override
  public boolean supportsPrinter()
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
  public void tapeInPhaseChanged()
  {
    this.pio00.putInValuePortB( this.tapeInPhase ? 0x20 : 0, 0x20 );
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (this.kcNet != null) && ((port & 0xFC) == 0x80) ) {
      this.kcNet.write( port, value );
    } else if( (this.vdip != null) && ((port & 0xFC) == 0x88) ) {
      this.vdip.write( port, value );
    } else if( (this.gide != null) && ((port & 0xF0) == 0xD0) ) {
      this.gide.write( port, value );
    } else if( (this.k1520Sound != null) && ((port & 0xF8) == 0xE0) ) {
      this.k1520Sound.write( port, value, tStates );
    } else {
      port &= 0xFF;
      if( port < 0x80 ) {
	// PIO auf ZRE-Karte
	switch( port & 0x03 ) {
	  case 0:
	    this.pio00.writeDataA( value );
	    break;
	  case 1:
	    this.pio00.writeDataB( value );
	    break;
	  case 2:
	    this.pio00.writeControlA( value );
	    break;
	  case 3:
	    this.pio00.writeControlB( value );
	    break;
	}
      } else {
	switch( port ) {
	  // 256K RAM Karte
	  case 0xC0:
	    this.ram256kRFBaseAddr = (this.ram256kRFBaseAddr & 0x30000)
					| ((value << 8) & 0x0FF00);
	    break;
	  case 0xC2:
	    this.ram256kMemBaseAddr = ((value << 12) & 0x30000);
	    this.ram256kRFBaseAddr  = (this.ram256kRFBaseAddr & 0x0FF00)
					| ((value << 10) & 0x30000);
	    break;
	  case 0xC4:
	    this.ram256kEnabled = false;
	    break;
	  case 0xC5:
	    this.ram256kEnabled = true;
	    break;
	  case 0xC6:
	    this.ram256kReadable = false;
	    break;
	  case 0xC7:
	    this.ram256kReadable = true;
	    break;

	  // PIO 0 auf IO-Karte
	  case 0x80:
	    if( this.pio80 != null ) {
	      this.pio80.writeDataA( value );
	    }
	    break;
	  case 0x81:
	    if( this.pio80 != null ) {
	      this.pio80.writeDataB( value );
	    }
	    break;
	  case 0x82:
	    if( this.pio80 != null ) {
	      this.pio80.writeControlA( value );
	    }
	    break;
	  case 0x83:
	    if( this.pio80 != null ) {
	      this.pio80.writeControlB( value );
	    }
	    break;

	  // SIO auf IO-Karte
	  case 0x84:
	    this.sio84.writeDataA( value );
	    break;
	  case 0x85:
	    this.sio84.writeDataB( value );
	    break;
	  case 0x86:
	    this.sio84.writeControlA( value );
	    break;
	  case 0x87:
	    this.sio84.writeControlB( value );
	    break;

	  // PIO 1 auf IO-Karte
	  case 0x88:
	    if( this.pio88 != null ) {
	      this.pio88.writeDataA( value );
	    }
	    break;
	  case 0x89:
	    if( this.pio88 != null ) {
	      this.pio88.writeDataB( value );
	    }
	    break;
	  case 0x8A:
	    if( this.pio88 != null ) {
	      this.pio88.writeControlA( value );
	    }
	    break;
	  case 0x8B:
	    if( this.pio88 != null ) {
	      this.pio88.writeControlB( value );
	    }
	    break;

	  // CTC auf IO-Karte
	  case 0x8C:
	  case 0x8D:
	  case 0x8E:
	  case 0x8F:
	    this.ctc8C.write( port & 0x03, value, tStates );
	    break;

	  // FDC-Karte
	  case 0x92:
	    {
	      setFDCSpeed( (value & 0x01) != 0 );
	      boolean tc = ((value & 0x02) != 0);
	      if( tc && (tc != this.fdcTC) ) {
		this.fdc.fireTC();
	      }
	      this.fdcTC = tc;
	    }
	    break;
	  case 0x95:
	    this.fdc.write( value );
	    break;

	  // Farbgrafikkarte
	  case 0xF1:
	  case 0xF2:
	    if( this.graphicPoppe != null ) {
	      this.graphicPoppe.writeIOByte( port, value );
	    }
	    break;
	}
      }
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.ctc8C.z80TStatesProcessed( cpu, tStates );
    this.fdc.z80TStatesProcessed( cpu, tStates );
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    if( (this.keyboardHW == KeyboardHW.PIO00A_HS)
	&& (this.pasteTStates > 0) )
    {
      this.pasteTStates -= tStates;
      if( this.pasteTStates <= 0 ) {
	CharacterIterator iter = this.pasteIter;
	if( iter != null ) {
	  char ch = iter.next();
	  if( ch == CharacterIterator.DONE ) {
	    cancelPastingText();
	  } else {
	    if( !putKeyChar( ch ) ) {
	      cancelPastingText();
	      fireShowCharNotPasted( iter );
	    }
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void applyKeyboardSettings( Properties props )
  {
    switch( EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_KEYBOARD ) )
    {
      case VALUE_KEYBOARD_PIO00A_BIT7:
	this.keyboardHW = KeyboardHW.PIO00A_BIT7;
	break;
      case VALUE_KEYBOARD_SIO84A:
	this.keyboardHW = KeyboardHW.SIO84A;
	break;
      default:
	this.keyboardHW = KeyboardHW.PIO00A_HS;
    }
    this.swapKeyCharCase = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_KEYBOARD_SWAP_CASE,
				false );
  }


  private GraphicHW getGraphicHW( Properties props )
  {
    GraphicHW rv = GraphicHW.Video3_80x25;
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_GRAPHIC ) )
    {
      case VALUE_GRAPHIC_64X32:
	rv = GraphicHW.Video2_64x32;
	break;
      case VALUE_GRAPHIC_80X24:
	rv = GraphicHW.Video3_80x24;
	break;
      case VALUE_GRAPHIC_POPPE:
	rv = GraphicHW.Poppe;
	break;
    }
    return rv;
  }


  private KeyboardHW getKeyboardHW( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_KEYBOARD ).equals(
					VALUE_KEYBOARD_PIO00A_BIT7 ) ?
			KeyboardHW.PIO00A_BIT7 : KeyboardHW.PIO00A_HS;
  }


  private byte[] getNanosFontBytes()
  {
    if( fontNanos == null ) {
      fontNanos = readResource( "/rom/nanos/nanosfont.bin" );
    }
    return fontNanos;
  }


  private boolean isFixedScreenSize( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_FIXED_SCREEN_SIZE,
			false );
  }


  private void loadFonts( Properties props )
  {
    this.fontBytes = readFontByProperty(
		props,
		this.propPrefix + PROP_FONT_8X8_PREFIX + PROP_FILE,
		0x1000 );
    if( this.fontBytes == null ) {
      this.fontBytes = getNanosFontBytes();
    }
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.load8x6FontByProperty(
		props,
		this.propPrefix + PROP_FONT_8X6_PREFIX + PROP_FILE );
      this.graphicPoppe.set8x8FontBytes( this.fontBytes );
    }
  }


  private boolean putKeyChar( char ch )
  {
    boolean rv = false;
    if( this.fontBytes == fontNanos ) {
      switch( ch ) {
	case '\u0278':
	  ch = '|';
	  break;
	case '\u00AC':
	  ch = '~';
	  break;
	case '\\':
	  ch = '\u0060';
	  break;
      }
    }
    switch( this.keyboardHW ) {
      case PIO00A_HS:
	if( (ch > 0) && (ch <= 0xFF) ) {
	  this.pio00.putInValuePortA( TextUtil.toReverseCase( ch ), true );
	  rv = true;
	}
	break;
      case PIO00A_BIT7:
	if( ch == 0 ) {
	  this.pio00.putInValuePortA( 0, 0xFF );
	  rv = true;
	}
	else if( (ch > 0) && (ch <= 0x7F) ) {
	  this.pio00.putInValuePortA( ch | 0x80, 0xFF );
	  rv = true;
	}
	break;
      case SIO84A:
	if( (ch > 0) && (ch <= 0xFF) ) {
	  this.sio84.putToReceiverA( TextUtil.toReverseCase( ch ) );
	  rv = true;
	}
	break;
    }
    return rv;
  }


  private void setFDCSpeed( boolean mini )
  {
    this.fdc.setTStatesPerMilli( mini ? 4000 : 8000 );
  }
}
