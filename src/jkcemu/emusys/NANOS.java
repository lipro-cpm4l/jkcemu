/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des NANOS-Systems
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.text.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.etc.VDIP;
import jkcemu.net.KCNet;
import jkcemu.text.TextUtil;
import z80emu.*;


public class NANOS extends EmuSys implements
					FDC8272.DriveSelector,
					Z80PIOPortListener,
					Z80SIOChannelListener,
					Z80TStatesListener
{
  public static final String PROP_KEYBOARD_KEY               = "keyboard";
  public static final String PROP_KEYBOARD_VALUE_PIO00A_HS   = "pio00a_hs";
  public static final String PROP_KEYBOARD_VALUE_PIO00A_BIT7 = "pio00a_bit7";
  public static final String PROP_KEYBOARD_SWAP_CASE         = "swap_case";

  public static final String PROP_GRAPHIC_KEY         = "graphic";
  public static final String PROP_GRAPHIC_VALUE_64X32 = "64x32";
  public static final String PROP_GRAPHIC_VALUE_80X24 = "80x24";
  public static final String PROP_GRAPHIC_VALUE_80X25 = "80x25";
  public static final String PROP_GRAPHIC_VALUE_POPPE = "poppe";

  private enum GraphicHW {
			Video2_64x32,
			Video3_80x24,
			Video3_80x25,
			Poppe_64x32_80x24 };
  private enum KeyboardHW { PIO00A_HS, PIO00A_BIT7 };

  private static FloppyDiskInfo epos20Disk64x32 =
		new FloppyDiskInfo(
			"/disks/nanos/epos20_64x32.dump.gz",
			"EPOS 2.0 Boot-Diskette (64x32 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo epos20Disk80x24 =
		new FloppyDiskInfo(
			"/disks/nanos/epos20_80x24.dump.gz",
			"EPOS 2.0 Boot-Diskette (80x24 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo nanos22Disk80x25 =
		new FloppyDiskInfo(
			"/disks/nanos/nanos22_80x25.dump.gz",
			"NANOS 2.2 Boot-Diskette",
			2, 2048, true );

  private static byte[] romEpos   = null;
  private static byte[] romNanos  = null;
  private static byte[] fontNanos = null;

  private byte[]            fontBytes8x6;
  private byte[]            fontBytes8x8;
  private byte[]            ram1000;
  private byte[]            ramVideoText;
  private byte[]            ramVideoColor;
  private byte[]            romBytes;
  private String            romProp;
  private byte[]            ram256k;
  private Z80PIO            pio00;
  private Z80PIO            pio80;
  private Z80SIO            sio84;
  private Z80PIO            pio88;
  private Z80CTC            ctc8C;
  private KCNet             kcNet;
  private VDIP              vdip;
  private GIDE              gide;
  private FDC8272           fdc;
  private FloppyDiskDrive[] fdDrives;
  private boolean           fdcTC;
  private boolean           audioInPhase;
  private boolean           altFontSelected;
  private boolean           colorRamSelected;
  private boolean           mode64x32;
  private boolean           fillPixLines8And9;
  private boolean           bootMemEnabled;
  private boolean           swapKeyCharCase;
  private boolean           ram256kEnabled;
  private boolean           ram256kReadable;
  private int               ram256kMemBaseAddr;
  private int               ram256kRFBaseAddr;
  private long              pasteTStates;
  private Color[]           colors;
  private GraphicHW         graphicHW;
  private KeyboardHW        keyboardHW;


  public NANOS( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, "jkcemu.nanos." );
    this.graphicHW = getGraphicHW( props );
    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
      this.ramVideoText  = new byte[ 0x0800 ];
      this.ramVideoColor = new byte[ 0x0800 ];
      this.colors        = new Color[ 16 ];
      float f = getBrightness( props );
      if( (this.colors != null) && (f >= 0F) && (f <= 1F) ) {
	for( int i = 0; i < this.colors.length; i++ ) {
	  int v = Math.round( ((i & 0x08) != 0 ? 0xFF : 0xBF) * f );
	  this.colors[ i ] = new Color(
		(i & 0x01) != 0 ? v : 0,
		(i & 0x02) != 0 ? v : 0,
		(i & 0x04) != 0 ? v : 0 );
	}
      }
    } else {
      this.ramVideoText  = null;
      this.ramVideoColor = null;
      this.colors        = null;
    }
    this.keyboardHW      = getKeyboardHW( props );
    this.swapKeyCharCase = false;
    this.fontBytes8x6    = null;
    this.fontBytes8x8    = null;
    this.romBytes        = null;
    this.romProp         = null;
    this.ram1000         = new byte[ 0x0400 ];
    this.ram256k         = new byte[ 0x40000 ];
    this.fdc             = new FDC8272( this, 4 );
    this.fdDrives        = new FloppyDiskDrive[ 4 ];
    Arrays.fill( this.fdDrives, null );
    setFDCSpeed( false );

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (E/A-Adressen 80h-83h)" );
    }

    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP(
			this.emuThread.getFileTimesViewFactory(),
			"USB-PIO (E/A-Adressen 88h-8Bh)" );
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, this.propPrefix );

    this.pio00 = new Z80PIO( "ZRE-PIO (00-03)" );
    this.pio80 = null;
    if( this.kcNet == null ) {
      this.pio80 = new Z80PIO( "PIO (80-83)" );
    }
    this.sio84 = new Z80SIO( "SIO (84-87)" );
    this.pio88 = null;
    if( this.vdip == null ) {
      this.pio88 = new Z80PIO( "PIO (88-8B)" );
    }
    this.ctc8C = new Z80CTC( "CTC (8C-8F)" );

    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
    iSources.add( this.pio00 );
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
    cpu.addTStatesListener( this );
    this.pio00.addPIOPortListener( this, Z80PIO.PortInfo.A );
    this.sio84.addChannelListener( this, 0 );

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


  public static boolean getDefaultSwapKeyCharCase()
  {
    return false;
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


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio00)
	&& (port == Z80PIO.PortInfo.A)
	&& (status == Z80PIO.Status.READY_FOR_INPUT)
	&& (this.keyboardHW == KeyboardHW.PIO00A_HS)
	&& (this.pasteIter != null) )
    {
      this.pasteTStates = 50000;
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOChannelByteAvailable( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio84) && (channel == 1) )
      this.emuThread.getPrintMngr().putByte( value );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    boolean phase = this.emuThread.readAudioPhase();
    if( phase != this.audioInPhase ) {
      this.audioInPhase = phase;
      this.pio00.putInValuePortB( this.audioInPhase ? 0x20 : 0, 0x20 );
    }
    this.ctc8C.z80TStatesProcessed( cpu, tStates );
    this.fdc.z80TStatesProcessed( cpu, tStates );
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    if( (this.keyboardHW == KeyboardHW.PIO00A_HS)
	&& (this.pasteTStates > 0) )
    {
      this.pasteTStates -= tStates;
      synchronized( this ) {
	if( this.pasteTStates <= 0 ) {
	  if( this.pasteIter != null ) {
	    char ch = this.pasteIter.next();
	    if( ch == CharacterIterator.DONE ) {
	      cancelPastingText();
	    } else {
	      putKeyChar( ch );
	    }
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>NANOS Konfiguration</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>ZRE-ROM/RAM:</td><td>" );
    buf.append( this.bootMemEnabled ? "ein" : "aus" );
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
    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
      buf.append( "<tr><td>Bildschirmformat:</td><td>" );
      buf.append( this.mode64x32 ? "64x32" : "80x24" );
      buf.append( "</td></tr>\n" );
      if( this.mode64x32 ) {
	buf.append( "<tr><td>Zeichensatz:</td><td>" );
	buf.append( this.altFontSelected ? "2" : "1 (Standard)" );
	buf.append( "</td></tr>\n" );
      } else {
	buf.append( "<tr><td>Zeilenzwischenraum:</td><td>" );
	buf.append(
		this.fillPixLines8And9 ? "Blockgrafik" : "Hintergrundfarbe" );
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>F800h-FFFFh:</td><td>" );
      if( this.colorRamSelected ) {
	buf.append( "Farb" );
      } else {
	buf.append( "Text" );
      }
      buf.append( "-RAM</td></tr>\n" );
    }
    buf.append( "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    if( EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_KEYBOARD_KEY ).equals(
					PROP_KEYBOARD_VALUE_PIO00A_BIT7 ) )
    {
      this.keyboardHW = KeyboardHW.PIO00A_BIT7;
    } else {
      this.keyboardHW = KeyboardHW.PIO00A_HS;
    }
    this.swapKeyCharCase = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_KEYBOARD_SWAP_CASE,
				false );
    loadFonts( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( "NANOS" );
    if( rv ) {
      rv = TextUtil.equals(
		this.romProp,
		EmuUtil.getProperty( props, this.propPrefix + "rom" ) );
    }
    if( rv && (this.graphicHW != getGraphicHW( props )) ) {
      rv = false;
    }
    if( rv && (this.keyboardHW != getKeyboardHW( props )) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesUSB( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, this.propPrefix );
    }
    return rv;
  }


  public synchronized void cancelPastingText()
  {
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      if( this.pasteIter != null ) {
	this.pasteIter = null;
	this.screenFrm.firePastingTextFinished();
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
    this.sio84.removeChannelListener( this, 0 );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    cpu.removeTStatesListener( this );
    this.fdc.die();
    if( this.kcNet != null ) {
      cpu.removeMaxSpeedListener( this.kcNet );
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
    if( this.gide != null ) {
      this.gide.die();
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0xE600;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    if( (this.ramVideoColor != null) && (this.colors != null) ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
	color = this.colors[ colorIdx ];
      }
    } else {
      if( colorIdx > 0 ) {
	color = Color.white;
      }
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return this.ramVideoColor != null ? 16 : 2;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( (this.graphicHW == GraphicHW.Poppe_64x32_80x24)
	&& (this.ramVideoText != null) && (this.ramVideoColor != null) )
    {
      byte[] fontBytes     = this.fontBytes8x8;
      int    fontOffs      = 0;
      int    pixPerCol     = 8;
      int    pixPerRow     = 10;
      int    colsPerRow    = 80;
      int    charsOnScreen = 1920;		// 80x24
      if( this.mode64x32 ) {
	fontBytes     = this.fontBytes8x6;
	pixPerCol     = 6;
	pixPerRow     = 8;
	colsPerRow    = 64;
	charsOnScreen = 2048;
	if( this.altFontSelected && (fontBytes != null) ) {
	  if( fontBytes.length > 0x0800 ) {
	    fontOffs = 0x0800;
	  }
	}
      } else {
	y -= 8;
      }
      if( (y >= 0) && (fontBytes != null) ) {
	int rPix = y % pixPerRow;
	int row  = y / pixPerRow;
	int col  = x / pixPerCol;
	if( (rPix >= 8) && this.fillPixLines8And9 ) {
	  rPix -= 8;
	  fontOffs = 0x0800;
	}
	int mIdx = (row * colsPerRow) + col;
	if( (mIdx >= 0) && (mIdx < charsOnScreen) ) {
	  if( mIdx < this.ramVideoText.length ) {
	    rv = (int) this.ramVideoColor[ mIdx ] & 0xFF;
	  }
	  if( rPix < 8 ) {
	    int ch = 0;
	    if( mIdx < this.ramVideoText.length ) {
	      ch = (int) this.ramVideoText[ mIdx ] & 0xFF;
	    }
	    int fIdx = (ch * 8) + rPix;
	    if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	      int m = 0x80;
	      int n = x % pixPerCol;
	      if( n > 0 ) {
		m >>= n;
	      }
	      if( (fontBytes[ fIdx ] & m) == 0 ) {
		rv >>= 4;
	      }
	    }
	  } else {
	    rv >>= 4;
	  }
	  rv &= 0x0F;
	}
      }
    } else {
      if( this.fontBytes8x8 != null ) {
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
	    if( (fIdx >= 0) && (fIdx < this.fontBytes8x8.length ) ) {
	      int m = 0x80;
	      int n = x % 8;
	      if( n > 0 ) {
		m >>= n;
	      }
	      if( (this.fontBytes8x8[ fIdx ] & m) != 0 ) {
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
	rv = new CharRaster( 64, 32, 8, 8, 8, 0 );
	break;
      case Video3_80x24:
	rv = new CharRaster( 80, 24, 10, 8, 8, 0 );
	break;
      case Poppe_64x32_80x24:
	if( this.mode64x32 ) {
	  rv = new CharRaster( 64, 32, 8, 8, 6, 0 );
	} else {
	  rv = new CharRaster( 80, 24, 10, 8, 8, 8 );
	}
	break;
    }
    return rv != null ? rv : new CharRaster( 80, 25, 10, 8, 8, 0 );
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_780K;
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
    }
    else if( (addr >= 0xF800)
	     && (this.ramVideoText != null)
	     && (this.ramVideoColor != null) )
    {
      int idx = addr - 0xF800;
      if( this.colorRamSelected ) {
	if( idx < this.ramVideoColor.length ) {
	  rv = (int) this.ramVideoColor[ idx ] & 0xFF;
	}
      } else {
	if( idx < this.ramVideoText.length ) {
	  rv = (int) this.ramVideoText[ idx ] & 0xFF;
	}
      }
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
    int ch    = -1;
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
      case Poppe_64x32_80x24:
	if( this.mode64x32 ) {
	  nCols = 64;
	  nRows = 32;
	} else {
	  nCols = 80;
	  nRows = 24;
	}
	break;
    }
    if( (chX >= 0) && (chX < nCols) && (chY >= 0) && (chY < nRows) ) {
      int b   = 0;
      int idx = (chY * nCols) + chX;
      if( this.ramVideoText != null ) {
	if( idx < this.ramVideoText.length ) {
	  b = (int) this.ramVideoText[ idx ] & 0xFF;
	}
      } else {
        b = this.emuThread.getRAMByte( 0xF800 + idx );
      }
      if( this.fontBytes8x8 == fontNanos ) {
	switch( b ) {
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
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      ch = b;
	    }
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
      case Poppe_64x32_80x24:
	rv = 256;
	break;
      case Video3_80x24:
	rv = 240;
	break;
      case Video3_80x25:
	rv = 250;
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
      case Poppe_64x32_80x24:
	if( this.mode64x32 ) {
	  rv = 384;
	}
	break;
    }
    return rv;
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
      else if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
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
    return "NANOS";
  }


  @Override
  protected VDIP getVDIP()
  {
    return this.vdip;
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
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      if( this.pio00.isReadyPortA() ) {
	putKeyChar( ch );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  protected boolean pasteChar( char ch )
  {
    boolean rv = false;
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      if( ch == '\n' ) {
	ch = '\r';
      }
      if( (ch > 0) && (ch < 0x7F) ) {
	try {
	  while( !keyTyped( ch ) ) {
	    Thread.sleep( 100 );
	  }
	  rv = true;
	}
	catch( InterruptedException ex ) {}
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

    port &= 0xFF;
    if( (this.kcNet != null) && ((port & 0xFC) == 0x80) ) {
      rv = this.kcNet.read( port );
    } else if( (this.vdip != null) && ((port & 0xFC) == 0x88) ) {
      rv = this.vdip.read( port );
    } else if( (this.gide != null) && ((port & 0xD0) == 0x80) ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    } else {
      if( port < 0x80 ) {
	// PIO auf ZRE-Karte
	switch( port & 0x03 ) {
	  case 0:
	    rv = this.pio00.readDataA();
	    break;
	  case 1:
	    rv = this.pio00.readDataB();
	    break;
	  case 2:
	    rv = this.pio00.readControlA();
	    break;
	  case 0x3:
	    rv = this.pio00.readControlB();
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
	  case 0x82:
	    if( this.pio80 != null ) {
	      rv = this.pio80.readControlA();
	    }
	    break;
	  case 0x83:
	    if( this.pio80 != null ) {
	      rv = this.pio80.readControlB();
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
	  case 0x8A:
	    if( this.pio88 != null ) {
	      rv = this.pio88.readControlA();
	    }
	    break;
	  case 0x8B:
	    if( this.pio88 != null ) {
	      rv = this.pio88.readControlB();
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
	    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
	      rv = 0xF0;
	      if( this.colorRamSelected ) {
		rv |= 0x01;
	      }
	      if( !this.mode64x32 ) {
		rv |= 0x02;
	      }
	      if( this.fillPixLines8And9 ) {
		rv |= 0x04;
	      }
	      if( this.altFontSelected ) {
		rv |= 0x08;
	      }
	    }
	    break;
	}
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      initSRAM( this.ram1000, props );
      Arrays.fill( this.ram256k, (byte) 0xFF );
      if( this.ramVideoText != null ) {
	fillRandom( this.ramVideoText );
      }
      if( this.ramVideoColor != null ) {
	fillRandom( this.ramVideoColor );
      }
    }
    this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    boolean coldReset = ((resetLevel == EmuThread.ResetLevel.POWER_ON)
		|| (resetLevel == EmuThread.ResetLevel.COLD_RESET));
    this.pio00.reset( coldReset );
    if( this.pio80 != null ) {
      this.pio80.reset( coldReset );
    }
    this.sio84.reset( coldReset );
    if( this.pio88 != null ) {
      this.pio88.reset( coldReset );
    }
    this.ctc8C.reset( coldReset );
    if( this.gide != null ) {
      this.gide.reset();
    }
    setFDCSpeed( false );
    this.fdcTC = false;
    for( int i = 0; i < this.fdDrives.length; i++ ) {
      FloppyDiskDrive drive = this.fdDrives[ i ];
      if( drive != null ) {
	drive.reset();
      }
    }
    switch( this.graphicHW ) {
      case Video2_64x32:
      case Poppe_64x32_80x24:
	setMode64x32( true );
	break;
      case Video3_80x24:
      case Video3_80x25:
	setMode64x32( false );
	break;
    }
    this.altFontSelected    = false;
    this.colorRamSelected   = false;
    this.fillPixLines8And9  = false;
    this.audioInPhase       = this.emuThread.readAudioPhase();
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
      if( (this.graphicHW == GraphicHW.Poppe_64x32_80x24)
	  && (this.ramVideoText != null) && (this.ramVideoColor != null) )
      {
	int idx = addr - 0xF800;
	if( this.colorRamSelected ) {
	  if( idx < this.ramVideoColor.length ) {
	    this.ramVideoColor[ idx ] = (byte) value;
	    done = true;
	  }
	} else {
	  if( idx < this.ramVideoText.length ) {
	    this.ramVideoText[ idx ] = (byte) value;
	    done = true;
	  }
	}
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
    return (this.fontBytes8x8 != fontNanos);
  }


  public synchronized void startPastingText( String text )
  {
    if( this.keyboardHW == KeyboardHW.PIO00A_HS ) {
      boolean done = false;
      if( text != null ) {
	if( !text.isEmpty() ) {
	  cancelPastingText();
	  this.pasteIter = new StringCharacterIterator( text );
	  if( this.pio00.isReadyPortA() ) {
	    putKeyChar( this.pasteIter.first() );
	  }
	  done = true;
	}
      }
      if( !done ) {
	this.screenFrm.firePastingTextFinished();
      }
    } else {
      super.startPastingText( text );
    }
  }


  @Override
  public boolean supportsAudio()
  {
    return true;
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
  public void writeIOByte( int port, int value, int tStates )
  {
    port &= 0xFF;
    if( (this.kcNet != null) && ((port & 0xFC) == 0x80) ) {
      this.kcNet.write( port, value );
    } else if( (this.vdip != null) && ((port & 0xFC) == 0x88) ) {
      this.vdip.write( port, value );
    } else if( (this.gide != null) && ((port & 0xF0) == 0xD0) ) {
      this.gide.write( port, value );
    } else {
      if( port < 0x80 ) {
	// PIO auf ZRE-Karte
	switch( port & 0x03 ) {
	  case 0:
	    this.pio00.writeDataA( value );
	    break;
	  case 1:
	    {
	      this.pio00.writeDataB( value );
	      int outValue = this.pio00.fetchOutValuePortB( false );
	      this.bootMemEnabled = ((outValue & 0x80) != 0);
	      this.emuThread.writeAudioPhase( (outValue & 0x40) != 0 );
	    }
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
	    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
	      this.altFontSelected   = ((value & 0x08) != 0);
	      this.fillPixLines8And9 = ((value & 0x04) != 0);
	      setMode64x32( (value & 0x02) == 0 );
	    }
	    break;
	  case 0xF2:
	    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
	      this.colorRamSelected = ((value & 0x01) != 0);
	    }
	    break;
	}
      }
    }
  }


	/* --- private Methoden --- */

  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "kcnet.enabled",
			false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "vdip.enabled",
			false );
  }


  private GraphicHW getGraphicHW( Properties props )
  {
    GraphicHW rv = GraphicHW.Video3_80x25;
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_GRAPHIC_KEY ) )
    {
      case PROP_GRAPHIC_VALUE_64X32:
	rv = GraphicHW.Video2_64x32;
	break;
      case PROP_GRAPHIC_VALUE_80X24:
	rv = GraphicHW.Video3_80x24;
	break;
      case PROP_GRAPHIC_VALUE_POPPE:
	rv = GraphicHW.Poppe_64x32_80x24;
	break;
    }
    return rv;
  }


  private KeyboardHW getKeyboardHW( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_KEYBOARD_KEY ).equals(
					PROP_KEYBOARD_VALUE_PIO00A_BIT7 ) ?
			KeyboardHW.PIO00A_BIT7 : KeyboardHW.PIO00A_HS;
  }


  private byte[] getNanosFontBytes()
  {
    if( fontNanos == null ) {
      fontNanos = readResource( "/rom/nanos/nanosfont.bin" );
    }
    return fontNanos;
  }


  private void loadFonts( Properties props )
  {
    this.fontBytes8x8 = readFontByProperty(
				props,
				this.propPrefix + "font.8x8.file",
				0x1000 );
    if( this.fontBytes8x8 == null ) {
      this.fontBytes8x8 = getNanosFontBytes();
    }
    if( this.graphicHW == GraphicHW.Poppe_64x32_80x24 ) {
      this.fontBytes8x6 = readFontByProperty(
				props,
				this.propPrefix + "font.8x6.file",
				0x1000 );
      if( this.fontBytes8x6 == null ) {
	this.fontBytes8x6 = getNanosFontBytes();
      }
    }
  }


  private void loadROMs( Properties props )
  {
    this.romProp = EmuUtil.getProperty(
			props,
			this.propPrefix + "rom" );
    String upperProp = this.romProp.toUpperCase();
    if( (this.romProp.length() > 5) && upperProp.startsWith( "FILE:" ) ) {
      this.romBytes = readROMFile(
				this.romProp.substring( 5 ),
				0x1000,
				"ROM-Inhalt" );
    }
    if( (this.romBytes == null) && upperProp.startsWith( "EPOS" ) ) {
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


  private void putKeyChar( char ch )
  {
    if( this.fontBytes8x8 == fontNanos ) {
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
	if( ch > '\u0000' ) {
	  this.pio00.putInValuePortA( TextUtil.toReverseCase( ch ), true );
	}
	break;
      case PIO00A_BIT7:
	if( ch > '\u0000' ) {
	  this.pio00.putInValuePortA( ch | 0x80, 0xFF );
	} else {
	  this.pio00.putInValuePortA( 0, 0xFF );
	}
	break;
    }
  }


  private void setFDCSpeed( boolean mini )
  {
    this.fdc.setTStatesPerMilli( mini ? 4000 : 8000 );
  }


  private void setMode64x32( boolean state )
  {
    boolean oldMode64x32 = this.mode64x32;
    this.mode64x32       = state;
    if( this.mode64x32 != oldMode64x32 ) {
      this.screenFrm.fireScreenSizeChanged();
    }
  }
}
