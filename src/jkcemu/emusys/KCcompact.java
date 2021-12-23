/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des KCcompact
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;
import jkcemu.audio.AbstractSoundDevice;
import jkcemu.audio.AudioOut;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.emusys.kccompact.KCcompactKeyboardFld;
import jkcemu.etc.CRTC6845;
import jkcemu.etc.PPI8255;
import jkcemu.etc.PSG8910;
import jkcemu.etc.PSGSoundDevice;
import jkcemu.file.FileFormat;
import jkcemu.joystick.JoystickThread;
import jkcemu.print.PrintMngr;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80InstrTStatesMngr;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;


public class KCcompact extends EmuSys implements
					CRTC6845.SyncListener,
					FDC8272.DriveSelector,
					PPI8255.Callback,
					PSG8910.Callback,
					Z80InstrTStatesMngr,
					Z80InterruptSource,
					Z80MaxSpeedListener
{
  public static final String SYSNAME             = "KCcompact";
  public static final String SYSTEXT             = "KC compact";
  public static final String PROP_PREFIX         = "jkcemu.kccompact.";
  public static final String PROP_FDC_ROM_PREFIX = "fdc.rom.";
  public static final String PROP_EXT_RAM_512K   = "ext_ram_512k";
  public static final String PROP_EXT_ROM_PREFIX = "ext_rom.";

  public static final int DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 1200;

  public static final int[] rawRGBValues = {
				0xFF000000,
				0xFF00007F,
				0xFF0000FF,
				0xFF7F0000,
				0xFF7F007F,
				0xFF7F00FF,
				0xFFFF0000,
				0xFFFF007F,
				0xFFFF00FF,
				0xFF007F00,
				0xFF007F7F,
				0xFF007FFF,
				0xFF7F7F00,
				0xFF7F7F7F,
				0xFF7F7FFF,
				0xFFFF7F00,
				0xFFFF7F7F,
				0xFFFF7FFF,
				0xFF00FF00,
				0xFF00FF7F,
				0xFF00FFFF,
				0xFF7FFF00,
				0xFF7FFF7F,
				0xFF7FFFFF,
				0xFFFFFF00,
				0xFFFFFF7F,
				0xFFFFFFFF };

  private static final FloppyDiskInfo[] availableFloppyDisks = {
		new FloppyDiskInfo(
			"/disks/kccompact/kccmicrodos.dump.gz",
			"KC compact MicroDOS Systemdiskette",
			2, 2048, true ) };

  /*
   * Diese Tabelle mappt die unteren 4 Bits des Farbpalettenwertes
   * auf den Index in der Farbpalette.
   */
  private static final int[] colorPalette2Idx = {
				13, 13, 19, 25, 1, 7, 10, 16,
				 7, 25, 24, 26, 6, 8, 15, 17,
				 1, 19, 18, 20, 0, 2,  9, 11,
				 4, 22, 21, 23, 3, 5, 12, 14 };

  private static final int[][] kbMatrixNormal = {
	{  -1,  -1,   -1,  -1,  -1,  -1,   -1,   -1 },
	{  -1,  -1,   -1,  -1,  -1,  -1,   -1,   -1 },
	{  -1, '[', 0x0D, ']',  -1,  -1, '\\',   -1 },
	{  -1, '-',  '@', 'p', ';', ':',  '/',  '.' },
	{ '0', '9',  'o', 'i', 'l', 'k',  'm',  ',' },
	{ '8', '7',  'u', 'y', 'h', 'j',  'n', 0x20 },
	{ '6', '5',  'r', 't', 'g', 'f',  'b',  'v' },
	{ '4', '3',  'e', 'w', 's', 'd',  'c',  'x' },
	{ '1', '2', 0x1B, 'q',   9, 'a',   -1,  'z' },
	{ -1,   -1,   -1,  -1,  -1,  -1,   -1,   -1 } };

  private static final int[][] kbMatrixShift = {
	{  -1,   -1,   -1,  -1,  -1,  -1,  -1,  -1 },
	{  -1,   -1,   -1,  -1,  -1,  -1,  -1,  -1 },
	{  -1,  '{',   -1, '}',  -1,  -1,  -1,  -1 },
	{  -1,  '=',  '|', 'P', '+', '*', '?', '>' },
	{ '_',  ')',  'O', 'I', 'L', 'K', 'M', '<' },
	{ '(', '\'',  'U', 'Y', 'H', 'J', 'N',  -1 },
	{ '&',  '%',  'R', 'T', 'G', 'F', 'B', 'V' },
	{ '$',  '#',  'E', 'W', 'S', 'D', 'C', 'X' },
	{ '!', '\"',   -1, 'Q',  -1, 'A',  -1, 'Z' },
	{ -1,    -1,   -1,  -1,  -1,  -1,  -1,  -1 } };

  private static final int[][] kbMatrixControl = {
	{ -1, -1, -1, -1, -1, -1, -1, -1 },
	{ -1, -1, -1, -1, -1, -1, -1, -1 },
	{ -1, -1, -1, -1, -1, -1, -1, -1 },
	{ -1, -1, -1, 16, -1, -1, -1, -1 },
	{ -1, -1, 15,  9, 12, 11, 13, -1 },
	{ -1, -1, 21, 25,  8, 10, 14, -1 },
	{ -1, -1, 18, 20,  7,  6,  2, 22 },
	{ -1, -1,  5, 23, 19,  4,  3, 24 },
	{ -1, -1, -1, 17, -1,  1, -1, 26 },
	{ -1, -1, -1, -1, -1, -1, -1, -1 } };

  // Mapping der Zeichen ab Code 128
  private static final int[] char128ToUnicode = {
		    -1, 0x2598, 0x259D, 0x2580,		// 128
		0x2596, 0x258C, 0x259E, 0x259B,		// 132
		0x2597, 0x259A, 0x2590, 0x259C,		// 136
		0x2584, 0x2599, 0x259F, 0x2588,		// 140
		    -1,     -1,     -1,     -1,		// 144
		    -1,     -1,     -1,     -1,		// 148
		    -1,     -1,     -1,     -1,		// 152
		    -1,     -1,     -1,     -1,		// 156
		0x005E, 0x00B4,     -1, 0x00A3,		// 160
		0x00A9, 0x00B6, 0x00A7, 0x0060,		// 164
		0x00BC, 0x00BD, 0x00BE,     -1,		// 168
		0x00F7, 0x00AC, 0x00BF, 0x00A1,		// 172
		0x03B1, 0x03B2, 0x03B3, 0x03B4,		// 176
		0x03B5, 0x03B8, 0x03BB, 0x03BC,		// 180
		0x03C0, 0x03C3, 0x03C6, 0x03A6,		// 184
		0x03C7, 0x03C9, 0x03A3, 0x03A9,		// 188
		    -1,     -1,     -1,     -1,		// 192
		    -1,     -1,     -1,     -1,		// 196
		    -1,     -1, 0x25C7,     -1,		// 200
		    -1,     -1,     -1,     -1,		// 204
		    -1,     -1,     -1,     -1,		// 208
		0x25E4, 0x25E5, 0x25E2, 0x25E3,		// 212
		    -1,     -1,     -1,     -1,		// 216
		    -1,     -1,     -1,     -1,		// 220
		    -1,     -1,     -1, 0x2666,		// 224
		0x2665, 0x2660,     -1,     -1,		// 228
		    -1,     -1, 0x2642, 0x2640,		// 232
		0x2669, 0x266A, 0x2600,     -1,		// 236
		    -1,     -1,     -1,     -1,		// 240
		    -1,     -1, 0x25B6, 0x25C0 };	// 244

  private static byte[]              romOS            = null;
  private static byte[]              romBASIC         = null;
  private static byte[]              romFDC           = null;
  private static Map<Long,Character> pixelCRC32ToChar = null;
  private static AutoInputCharSet    autoInputCharSet = null;

  private Color[]              colors;
  private int[]                regColors;
  private int                  regColorNum;
  private int                  borderColorIdx;
  private int                  romSelect;
  private String               osFile;
  private String               basicFile;
  private Map<Integer,byte[]>  extRomBytes;
  private Map<Integer,String>  extRomFileNames;
  private byte[]               osBytes;
  private byte[]               basicBytes;
  private byte[]               screenBuf;
  private byte[]               ramExt;
  private int[]                ram16KOffs;
  private int[]                keyboardMatrix;
  private int                  keyboardIdx;
  private int                  keyboardValue;
  private int                  psgValue;
  private int                  lineIrqCounter;
  private int                  joy0ActionMask;
  private int                  joy1ActionMask;
  private int                  ramExtBankAddr;
  private int                  screenMode;
  private volatile int         screenWidth;
  private boolean              screenDirty;
  private boolean              screenRefreshEnabled;
  private volatile boolean     fixedScreenSize;
  private boolean              interruptRequested;
  private boolean              centronicsStrobe;
  private boolean              basicROMEnabled;
  private boolean              osROMEnabled;
  private boolean              ramExt512K;
  private PSGSoundDevice       psgSoundDevice;
  private PSG8910              psg;
  private PPI8255              ppi;
  private CRTC6845             crtc;
  private KCcompactKeyboardFld keyboardFld;
  private FDC8272              fdc;
  private FloppyDiskDrive[]    floppyDiskDrives;


  public KCcompact( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.colors = new Color[ 27 ];
    createColors( props );

    /*
     * Auch bei einer extern eingebundenen Betriebssystem-ROM-Datei
     * muss der integrierte ROM-Inhalt geladen werden,
     * da daraus die Pixeldaten fuer die Zeichenerkennung gelesen werden.
     */
    if( romOS == null ) {
      romOS = readResource( "/rom/kccompact/kccos.bin" );
    }

    this.osBytes         = null;
    this.osFile          = null;
    this.basicBytes      = null;
    this.basicFile       = null;
    this.keyboardFld     = null;
    this.fixedScreenSize = isFixedScreenSize( props );
    this.screenMode      = 0;
    this.screenWidth     = 320;
    this.screenBuf       = new byte[ 640 * 200 ];  // max. interne Pixelanzahl
    this.keyboardMatrix  = new int[ 10 ];
    this.regColors       = new int[ 16 ];
    Arrays.fill( this.regColors, 0 );

    this.crtc           = new CRTC6845( 1000, this );
    this.ppi            = new PPI8255( this );
    this.psg            = new PSG8910( 1000000, this );
    this.psgSoundDevice = new PSGSoundDevice(
					"Sound-Generator",
					true,
					this.psg );

    this.extRomBytes     = new HashMap<>();
    this.extRomFileNames = getExtRomFileNames( props );

    this.ram16KOffs = new int[ 4 ];
    this.ramExt512K = emulatesRAMExt512K( props );
    if( this.ramExt512K ) {
      this.ramExt = new byte[ 0x80000 ];
    } else {
      this.ramExt = null;
    }

    if( emulatesFloppyDisk( props ) ) {
      if( this.ramExt == null ) {
	this.ramExt = new byte[ 0x10000 ];
      }
      this.fdc              = new FDC8272( this, 4 );
      this.floppyDiskDrives = new FloppyDiskDrive[ 2 ];
      Arrays.fill( this.floppyDiskDrives, null );
    } else {
      this.fdc              = null;
      this.floppyDiskDrives = null;
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( this );
    cpu.setInstrTStatesMngr( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );
    this.psg.start();
  }


  public static AutoInputCharSet getAutoInputCharSet()
  {
    if( autoInputCharSet == null ) {
      autoInputCharSet = new AutoInputCharSet();
      autoInputCharSet.addAsciiChars();
      autoInputCharSet.addEnterChar();
      autoInputCharSet.addEscChar();
      autoInputCharSet.addDelChar();
      autoInputCharSet.addKeyChar( 16, "CLR" );
      autoInputCharSet.addTabChar();
      autoInputCharSet.addSpecialChar(
			242,
			AutoInputCharSet.VIEW_LEFT,
			AutoInputCharSet.TEXT_LEFT );
      autoInputCharSet.addSpecialChar(
			243,
			AutoInputCharSet.VIEW_RIGHT,
			AutoInputCharSet.TEXT_RIGHT );
      autoInputCharSet.addSpecialChar(
			241,
			AutoInputCharSet.VIEW_DOWN,
			AutoInputCharSet.TEXT_DOWN );
      autoInputCharSet.addSpecialChar(
			240,
			AutoInputCharSet.VIEW_UP,
			AutoInputCharSet.TEXT_UP );
    }
    return autoInputCharSet;
  }


  public static int getDefaultSpeedKHz()
  {
    return 4000;
  }


  public static String getExtRomFilePropName( int romNum )
  {
    return String.format(
			"%s%s%d.file",
			PROP_PREFIX,
			PROP_EXT_ROM_PREFIX,
			romNum );
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.keyboardMatrix ) {
      int n = Math.min( kbMatrix.length, this.keyboardMatrix.length );
      int i = 0;
      while( i < n ) {
	this.keyboardMatrix[ i ] = kbMatrix[ i ];
	i++;
      }
      while( i < this.keyboardMatrix.length ) {
	this.keyboardMatrix[ i ] = 0;
	i++;
      }
    }
  }


	/* --- CRTC6845.SyncListener --- */

  @Override
  public void crtcHSyncBegin( int totalLine, int charLine, int lineBegAddr )
  {
    if( this.screenRefreshEnabled ) {
      int dstPos = totalLine * 640;
      if( (dstPos >= 0) && ((dstPos + 639) < this.screenBuf.length) ) {
	int endPos = dstPos + 640;
	int baseAddr = ((lineBegAddr << 2) & 0xC000)
				| ((charLine << 11) & 0x3800);
	int segAddr  = (lineBegAddr << 1) & 0x07FE;
	int nBytes   = Math.min( this.crtc.getHCharVisible(), 40 ) * 2;
	switch( this.screenMode ) {
	  case 0:
	    for( int i = 0; i < nBytes; i++ ) {
	      int  b = this.emuThread.getRAMByte( baseAddr | segAddr );
	      byte c = (byte) this.regColors[ ((b << 2) & 0x08)
						| ((b >> 3) & 0x04)
						| ((b >> 2) & 0x02)
						| ((b >> 7) & 0x01) ];
	      this.screenBuf[ dstPos++ ] = c;
	      this.screenBuf[ dstPos++ ] = c;
	      c = (byte) this.regColors[ ((b << 3) & 0x08)
						| ((b >> 2) & 0x04)
						| ((b >> 1) & 0x02)
						| ((b >> 6) & 0x01) ];
	      this.screenBuf[ dstPos++ ] = c;
	      this.screenBuf[ dstPos++ ] = c;
	      segAddr = (segAddr + 1) & 0x07FF;
	    }
	    break;

	  case 1:
	    for( int i = 0; i < nBytes; i++ ) {
	      int b = this.emuThread.getRAMByte( baseAddr | segAddr );
	      this.screenBuf[ dstPos++ ] = (byte) this.regColors[
				((b >> 2) & 0x02) | ((b >> 7) & 0x01) ];
	      this.screenBuf[ dstPos++ ] = (byte) this.regColors[
				((b >> 1) & 0x02) | ((b >> 6) & 0x01) ];
	      this.screenBuf[ dstPos++ ] = (byte) this.regColors[
				(b & 0x02) | ((b >> 5) & 0x01) ];
	      this.screenBuf[ dstPos++ ] = (byte) this.regColors[
				((b << 1) & 0x02) | ((b >> 4) & 0x01) ];
	      segAddr = (segAddr + 1) & 0x07FF;
	    }
	    break;

	  case 2:
	    for( int i = 0; i < nBytes; i++ ) {
	      int b = this.emuThread.getRAMByte( baseAddr | segAddr );
	      int m = 0x80;
	      for( int k = 0; k < 8; k++ ) {
		this.screenBuf[ dstPos++ ] = (byte) this.regColors[
						(b & m) == 0 ? 0 : 1 ];
		m >>= 1;
	      }
	      segAddr = (segAddr + 1) & 0x07FF;
	    }
	    break;

	  default:
	    nBytes = 0;
	}

	// Rest Zeile mit Randfarbe
	while( dstPos < endPos ) {
	  this.screenBuf[ dstPos++ ] = (byte) this.borderColorIdx;
	}
      }
      this.screenFrm.setScreenDirty( true );
      this.screenFrm.fireRepaint();
    }
  }


  @Override
  public void crtcHSyncEnd()
  {
    // Interruptzaehler mit der fallenden Flanke triggern
    if( this.lineIrqCounter > 0 ) {
      --this.lineIrqCounter;
      if( this.lineIrqCounter == 0 ) {
	this.interruptRequested = true;
	this.lineIrqCounter = 52;
      }
    }
  }


  @Override
  public void crtcVSyncBegin()
  {
    // Interrupt beim 2. Horizontalimpuls innerhalb des Vertikalimpulses
    this.lineIrqCounter = 2;
    if( this.screenDirty ) {
      this.screenDirty = false;
      this.screenRefreshEnabled = true;
    } else {
      this.screenRefreshEnabled = false;
    }
  }


  @Override
  public void crtcVSyncEnd()
  {
    // leer
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.floppyDiskDrives != null ) {
      // nur Bit 0 auswerten, da nur eine Leitung beschaltet ist
      rv = this.floppyDiskDrives[ driveNum & 0x01 ];
    }
    return rv;
  }


	/* --- PPI8255.Callback --- */

  @Override
  public int ppiReadPort( PPI8255 ppi, int port )
  {
    int rv = 0xFF;
    if( port == PPI8255.PORT_A ) {			// Port A
      rv = this.psgValue;
    } else if( port == PPI8255.PORT_B ) {		// Port B
      rv = 0x3E;
      if( this.fdc != null ) {
	rv &= 0xDF;					// EXP
      }
      if( this.crtc.isVSync() ) {
	rv |= 0x01;					// VSYNC
      }
      if( this.tapeInPhase ) {
	rv |= 0x80;
      }
    }
    return rv;
  }


  @Override
  public void ppiWritePort(
			PPI8255 ppi,
			int     port,
			int     value,
			boolean strobe )
  {
    if( port == PPI8255.PORT_C ) {
      this.keyboardIdx  = value & 0x0F;
      this.tapeOutPhase = ((value & 0x20) != 0);
      this.psgValue     = this.psg.access(
				(value & 0x80) != 0,		// BDIR
				(value & 0x40) != 0,		// BC1
				true,				// BC2
				this.ppi.fetchOutValueA( false ) );
    }
  }


	/* --- PSG8910.Callback --- */

  @Override
  public int psgReadPort( PSG8910 psg, int port )
  {
    int rv = 0xFF;
    if( port == PSG8910.PORT_A ) {
      synchronized( this.keyboardMatrix ) {
	int idx = this.keyboardIdx;
	if( idx < this.keyboardMatrix.length ) {
	  rv = ~this.keyboardMatrix[ idx ] & 0xFF;
	}
	if( idx == 6 ) {
	  rv &= ~getJoyKeyboardMask( this.joy1ActionMask );
	} else if( idx == 9 ) {
	  rv &= ~getJoyKeyboardMask( this.joy0ActionMask );
	}
      }
    }
    return rv;
  }


  @Override
  public void psgWritePort( PSG8910 psg, int port, int value )
  {
    // leer
  }


  @Override
  public void psgWriteFrame( PSG8910 psg, int a, int b, int c )
  {
    this.psgSoundDevice.writeFrames(
				1,
				(a + b + c) / 3,
				(a + (b / 2)) * 2 / 3,
				(c + (b / 2)) * 2 / 3 );
  }


	/* --- Z80InstrTStatesMngr --- */

  @Override
  public int z80IntructionProcessed( Z80CPU cpu, int pc, int tStates )
  {
    int m = (tStates + 3) / 4;
    return m * 4;
  }


	/* --- Z80InterruptSource --- */

  /*
   * Die Hardware des KCcompact reagiert nicht auf RETI-Befehle,
   * weshalb diese speziellen Interrupt-Return-Befehle
   * auch nicht vewendet werden.
   * Damit wird aber auch "interruptFinish(...)" nie aufgerufen.
   * Aus diesem Grund muss der Interrupt-Zustand mit der
   * Interrupt-Annahme bereits zurueckgesetzt werden, was bedeutet,
   * dass "isInterruptAccepted()" nie true zurueckliefern kann.
   */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<table border=\"1\">\n"
	+ "<tr><td>Interrupt angemeldet:</td><td>" );
    buf.append( this.interruptRequested ? "ja" : "nein" );
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  @Override
  public int interruptAccept()
  {
    if( (this.lineIrqCounter > 0) && (this.lineIrqCounter <= 26) ) {
      this.lineIrqCounter += 26;
    }
    this.interruptRequested = false;
    return 0xFF;
  }


  @Override
  public boolean interruptFinish( int addr )
  {
    return false;
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return false;
  }


  @Override
  public boolean isInterruptRequested()
  {
    return this.interruptRequested;
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.crtc.z80MaxSpeedChanged( cpu );
    if( this.fdc != null ) {
      this.fdc.z80MaxSpeedChanged( cpu );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>KC compact Status</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>Betriebssystem-ROM:</td><td>" );
    EmuUtil.appendOnOffText( buf, this.osROMEnabled );
    buf.append( "</td></tr>\n"
	+ "<tr><td>BASIC-ROM:</td><td>" );
    EmuUtil.appendOnOffText( buf, this.basicROMEnabled );
    buf.append( "</td></tr>\n"
	+ "<tr><td>ROM Select Port:</td><td>" );
    buf.append( this.romSelect );
    buf.append( "</td></tr>\n"
	+ "<td valign=\"top\">RAM-Konfiguration:</td>"
	+ "<td valign=\"top\">" );
    synchronized( this.ram16KOffs ) {
      int     addr       = 0;
      boolean usesExtRAM = false;
      for( int i = 0; i < this.ram16KOffs.length; i++ ) {
	int block = i + ((this.ram16KOffs[ i ] >> 14) & 0x07);
	buf.append(
		String.format(
			"%04Xh-%04Xh: Block %d",
			addr,
			addr + 0x3FFF,
			block ) );
	if( block >= 4 ) {
	  if( this.ramExt != null ) {
	    usesExtRAM = true;
	  } else {
	    buf.append( " (nicht vorhanden)" );
	  }
	}
	if( i < (this.ram16KOffs.length - 1) ) {
	  buf.append( "<br/>" );
	}
	addr += 0x4000;
      }
      if( usesExtRAM && (this.ramExt != null) ) {
	buf.append( "<br/>Bl\u00F6cke 4 bis 7 " );
	if( this.ramExt512K ) {
	  buf.append( "in RAM-Erweiterung Bank " );
	  buf.append( (this.ramExtBankAddr >> 16) & 0x07 );
	} else {
	  buf.append( "im FDC-RAM" );
	}
      }
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>Im vertikalen Synchronimpuls:</td><td>" );
    buf.append( this.crtc.isVSync() ? "ja" : "nein" );
    buf.append( "</td></tr>\n"
	+ "</table>\n"
	+ "<br/><br/>\n"
	+ "<h2>PSG</h2>\n" );
    this.psg.appendStatusHTMLTo( buf );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );

    boolean state = isFixedScreenSize( props );
    if( state != this.fixedScreenSize ) {
      this.fixedScreenSize = state;
      this.screenFrm.fireScreenSizeChanged();
    }
    createColors( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty(
				props, 
				this.propPrefix + PROP_OS_FILE ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.basicFile,
		EmuUtil.getProperty(
			props, 
			this.propPrefix + PROP_BASIC_PREFIX + PROP_FILE ) );
    }
    if( rv && ((this.fdc != null) != emulatesFloppyDisk( props )) ) {
      rv = false;
    }
    if( rv && (this.ramExt512K != emulatesRAMExt512K( props )) ) {
      rv = false;
    }
    if( rv ) {
      Map<Integer,String> extRomFileNames = getExtRomFileNames( props );
      if( extRomFileNames.size() == this.extRomFileNames.size() ) {
	for( Integer romNum : extRomFileNames.keySet() ) {
	  if( !TextUtil.equals(
			extRomFileNames.get( romNum ),
			this.extRomFileNames.get( romNum ) ) )
	  {
	    rv = false;
	    break;
	  }
	}
      } else {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public KCcompactKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new KCcompactKeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    this.psgSoundDevice.fireStop();

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    cpu.setInstrTStatesMngr( null );
    if( this.fdc != null ) {
      this.fdc.die();
    }
    super.die();
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  @Override
  public int getBorderColorIndex()
  {
    return this.borderColorIdx;
  }


  @Override
  public Color getColor( int idx )
  {
    return (idx >= 0) && (idx < this.colors.length) ?
					this.colors[ idx ]
					: Color.BLACK;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = this.borderColorIdx;
    if( this.fixedScreenSize || this.screenFrm.isFullScreenMode() ) {
      if( this.screenMode < 2 ) {
	x /= 2;
      }
      y /= 2;
    }
    int idx = (y * 640) + x;
    if( (idx >= 0) && (idx < this.screenBuf.length) ) {
      rv = (int) this.screenBuf[ idx ];
    }
    return rv;
  }


  @Override
  public int getColorCount()
  {
    return this.colors.length;
  }


  @Override
  protected boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster rv       = null;
    int        colCount = 0;
    switch( this.screenMode ) {
      case 0:
	colCount = 20;
	break;
      case 1:
	colCount = 40;
	break;
      case 2:
	colCount = 80;
	break;
    }
    if( colCount > 0 ) {
      int charHeight = getScreenHeight() / 25;
      rv = new CharRaster(
			colCount,
			25,
			charHeight,
			getScreenWidth() / colCount,
			charHeight );
    }
    return rv;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/kccompact.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr < 0x4000) && this.osROMEnabled ) {
      if( this.osBytes != null ) {
	if( addr < this.osBytes.length ) {
	  rv = (int) this.osBytes[ addr ] & 0xFF;
	}
      }
    } else if( (addr >= 0xC000) && this.basicROMEnabled ) {
      byte[] rom = null;
      if( this.romSelect > 0 ) {
	rom = this.extRomBytes.get( Integer.valueOf( this.romSelect ) );
	if( (rom == null)
	    && (this.fdc != null)
	    && (this.romSelect == 7) )
	{
	  rom = romFDC;
	}
      } else {
	rom = this.basicBytes;
      }
      if( rom != null ) {
	int idx = addr - 0xC000;
	if( (idx >= 0) && (idx < rom.length) ) {
	  rv = (int) rom[ idx ] & 0xFF;
	}
      }
    } else {
      int idx = addr + this.ram16KOffs[ (addr >> 14) & 0x07 ];
      if( idx >= 0x10000 ) {
	if( this.ramExt != null ) {
	  idx &= 0xFFFF;
	  if( this.ramExt512K ) {
	    idx |= this.ramExtBankAddr;
	  }
	  if( idx < this.ramExt.length ) {
	    rv = (int) this.ramExt[ idx ] & 0xFF;
	  }
	}
      } else {
	rv = this.emuThread.getRAMByte( addr );
      }
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( boolean powerOn )
  {
    return 0x0000;
  }


  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int rv = -1;
    if( (chX >= 0) && (chX < chRaster.getColCount())
	&& (chY >= 0) && (chY < chRaster.getRowCount()) )
    {
      Map<Long,Character> crc32ToChar = getPixelCRC32ToCharMap();
      if( crc32ToChar != null ) {
	CRC32 crc1 = new CRC32();
	CRC32 crc2 = new CRC32();
	int   wCh  = chRaster.getCharWidth();
	int   hCh  = chRaster.getCharHeight();
	int   sx   = wCh / 8;
	int   sy   = hCh / 8;
	int   x    = chX * wCh;
	int   y    = chY * hCh;
	for( int i = 0; i < hCh; i += sy ) {
	  int b = 0;
	  for( int k = 0; k < wCh; k += sx ) {
	    b <<= 1;
	    if( getColorIndex( x + k, y + i ) == this.regColors[ 0 ] ) {
	      b |= 0x01;
	    }
	  }
	  crc1.update( ~b & 0xFF );
	  crc2.update( b );
	}
	Character ch = crc32ToChar.get( crc1.getValue() );
	if( ch == null ) {
	  // Zeichen invers?
	  ch = crc32ToChar.get( crc2.getValue() );
	}
	if( ch != null ) {
	  rv = ch.charValue();
	}
      }
    }
    return rv;
  }


  @Override
  public int getScreenHeight()
  {
    return (this.fixedScreenSize || this.screenFrm.isFullScreenMode()) ?
								400 : 200;
  }


  @Override
  public int getScreenWidth()
  {
    return (this.fixedScreenSize || this.screenFrm.isFullScreenMode()) ?
						640 : this.screenWidth;
  }


  @Override
  public String getSecondSystemName()
  {
    String rv = null;
    if( this.ramExt != null ) {
      if( this.ramExt512K ) {
	rv = "RAM-Erweiterung";
      } else {
	rv = "FDC-RAM";
      }
    }
    return rv;
  }


  @Override
  public AbstractSoundDevice[] getSoundDevices()
  {
    return new AbstractSoundDevice[] { this.psgSoundDevice };
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return this.fdc != null ? availableFloppyDisks : null;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.floppyDiskDrives != null ? this.floppyDiskDrives.length : 0;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    return 2;
  }


  @Override
  public String getTitle()
  {
    return SYSTEXT;
  }


  @Override
  public boolean isAutoScreenRefresh()
  {
    return true;
  }


  @Override
  public boolean keyPressed(
                        int     keyCode,
                        boolean ctrlDown,
                        boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyboardMatrix ) {
      switch( keyCode ) {
        case KeyEvent.VK_LEFT:
          this.keyboardMatrix[ 1 ] |= 0x01;
          rv = true;
          break;
        case KeyEvent.VK_RIGHT:
          this.keyboardMatrix[ 0 ] |= 0x02;
          rv = true;
          break;
        case KeyEvent.VK_UP:
          this.keyboardMatrix[ 0 ] |= 0x01;
          rv = true;
          break;
        case KeyEvent.VK_DOWN:
          this.keyboardMatrix[ 0 ] |= 0x04;
          rv = true;
          break;
        case KeyEvent.VK_SPACE:
          this.keyboardMatrix[ 5 ] |= 0x80;
          rv = true;
          break;
        case KeyEvent.VK_ENTER:
	  this.keyboardMatrix[ 2 ] |= 0x04;		// RETURN
          rv = true;
          break;
        case KeyEvent.VK_ESCAPE:
          this.keyboardMatrix[ 8 ] |= 0x04;
          rv = true;
          break;
        case KeyEvent.VK_TAB:
          this.keyboardMatrix[ 8 ] |= 0x10;
          rv = true;
          break;
	case KeyEvent.VK_BACK_SPACE:
          this.keyboardMatrix[ 9 ] |= 0x80;		// DEL
          rv = true;
          break;
        case KeyEvent.VK_DELETE:
          this.keyboardMatrix[ 2 ] |= 0x01;		// CLR
          rv = true;
          break;
        case KeyEvent.VK_F1:
          this.keyboardMatrix[ 1 ] |= 0x20;
          rv = true;
          break;
        case KeyEvent.VK_F2:
          this.keyboardMatrix[ 1 ] |= 0x40;
          rv = true;
          break;
        case KeyEvent.VK_F3:
          this.keyboardMatrix[ 0 ] |= 0x20;
          rv = true;
          break;
        case KeyEvent.VK_F4:
          this.keyboardMatrix[ 2 ] |= 0x10;
          rv = true;
          break;
        case KeyEvent.VK_F5:
          this.keyboardMatrix[ 1 ] |= 0x80;		// F0
          rv = true;
          break;
        case KeyEvent.VK_F6:
          this.keyboardMatrix[ 0 ] |= 0x40;		//  F-ENTER
          rv = true;
          break;
        case KeyEvent.VK_F7:
          this.keyboardMatrix[ 0 ] |= 0x80;		// F-Punkt
          rv = true;
          break;
        case KeyEvent.VK_F8:
          this.keyboardMatrix[ 1 ] |= 0x02;		// COPY
          rv = true;
          break;
      }
      if( rv ) {
	if( shiftDown ) {
	  this.keyboardMatrix[ 2 ] |= 0x20;
	}
	if( ctrlDown ) {
	  this.keyboardMatrix[ 2 ] |= 0x80;
	}
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( ch > 0 ) {
      synchronized( this.keyboardMatrix ) {
	rv = setCharInKBMatrix( kbMatrixNormal, ch );
	if( !rv ) {
	  rv = setCharInKBMatrix( kbMatrixShift, ch );
	  if( rv ) {
	    this.keyboardMatrix[ 2 ] |= 0x20;
	  } else {
	    rv = setCharInKBMatrix( kbMatrixControl, ch );
	    if( rv ) {
	      this.keyboardMatrix[ 2 ] |= 0x80;
	    }
	  }
	}
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  /*
   * In der CP/M-Betriebsart wird der RAM der Diskettenstation
   * als TPA (CP/M-Arbeitsspeicher) verwendet.
   * Aus diesem Grund ist die Methode ueberschrieben,
   * um COM-Dateien in diesen RAM zu laden.
   */
  @Override
  public void loadIntoMem(
			int           begAddr,
			byte[]        data,
			int           idx,
			int           len,
			FileFormat    fileFmt,
			int           fileType,
			StringBuilder rvStatusMsg )
  {
    if( data != null ) {
      if( (fileFmt == FileFormat.COM) && (this.ramExt != null) ) {
	loadIntoSecondSystem( data, begAddr );
	if( rvStatusMsg != null ) {
	  int endAddr = begAddr + len - 1;
	  rvStatusMsg.append(
		String.format(
			"Datei in %s nach %04X-%04X geladen",
			this.ramExt512K ?
				"RAM-Erweiterung Bank 0"
				: "FDC-RAM",
			begAddr,
			endAddr > 0xFFFF ? 0xFFFF : endAddr ) );
	}
      } else {
	super.loadIntoMem(
			begAddr,
			data,
			idx,
			len,
			fileFmt,
			fileType,
			rvStatusMsg );
      }
    }
  }


  @Override
  public void loadIntoSecondSystem( byte[] data, int loadAddr )
  {
    if( this.ramExt != null ) {
      int idx = 0;
      while( (idx < data.length) && (loadAddr < this.ramExt.length) ) {
	this.ramExt[ loadAddr++ ] = data[ idx++ ];
      }
    }
  }


  @Override
  public void loadROMs( Properties props )
  {
    // OS-ROM
    this.osFile  = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_FILE );
    this.osBytes = readROMFile( this.osFile, 0x4000, "Betriebssystem-ROM" );
    if( this.osBytes == null ) {
      this.osBytes = romOS;
    }

    // BASIC-ROM
    this.basicFile = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_BASIC_PREFIX + PROP_FILE );
    this.basicBytes = readROMFile( this.basicFile, 0x4000, "BASIC-ROM" );
    if( this.basicBytes == null ) {
      if( romBASIC == null ) {
	romBASIC = readResource( "/rom/kccompact/kccbasic.bin" );
      }
      this.basicBytes = romBASIC;
    }

    // ROM-Erweiterungen
    boolean             hasFdcRom   = false;
    Map<Integer,byte[]> extRomBytes = new HashMap<>();
    Map<Integer,String> extRomFiles = getExtRomFileNames( props );
    for( Integer romNum : extRomFiles.keySet() ) {
      String fileName = extRomFiles.get( romNum );
      if( fileName != null ) {
	byte[] romBytes = readROMFile(
				fileName,
				0x4000,
				"ROM-Erweiterung " + romNum.toString() );
	if( romBytes != null ) {
	  extRomBytes.put( romNum, romBytes );
	  if( romNum.intValue() == 7 ) {
	    hasFdcRom = true;
	  }
	}
      }
    }
    this.extRomBytes = extRomBytes;

    // FDC-ROM
    if( (this.fdc != null) && !hasFdcRom ) {
      if( romFDC == null ) {
	romFDC = readResource( "/rom/kccompact/basdos.bin" );
      }
    }
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (this.fdc != null)
	&& (port & 0x0580) == 0x0100 )		// FDC: A7=A10=0, A8=1
    {
      if( (port & 0x01) == 0 ) {
	rv &= this.fdc.readMainStatusReg();
      } else {
	rv &= this.fdc.readData();
      }
    }
    if( (port & 0x0800) == 0 ) {		// PPI: A11=0
      rv &= this.ppi.read( port >> 8, 0xFF );
    }
    if( (port & 0x4000) == 0 ) {		// CRTC: A14=0
      if( (port & 0x0300) == 0x0300 ) {		// A8=A9=1
	rv &= this.crtc.read();
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
      if( this.ramExt != null ) {
	EmuUtil.initDRAM( this.ramExt );
      }
    }
    Arrays.fill( this.screenBuf, (byte) 0 );
    Arrays.fill( this.ram16KOffs, 0 );
    Arrays.fill( this.keyboardMatrix, 0 );
    this.joy0ActionMask       = 0;
    this.joy1ActionMask       = 0;
    this.regColorNum          = 0;
    this.borderColorIdx       = 0;
    this.keyboardIdx          = 0;
    this.keyboardValue        = 0xFF;
    this.ramExtBankAddr       = 0;
    this.romSelect            = 0;
    this.lineIrqCounter       = 0;
    this.interruptRequested   = false;
    this.centronicsStrobe     = false;
    this.basicROMEnabled      = true;
    this.osROMEnabled         = true;
    this.screenDirty          = true;
    this.screenRefreshEnabled = false;
    this.ppi.reset();
    this.psgSoundDevice.reset();
    if( this.fdc != null ) {
      this.fdc.reset( powerOn );
    }
    if( this.floppyDiskDrives != null ) {
      for( int i = 0; i < this.floppyDiskDrives.length; i++ ) {
	FloppyDiskDrive drive = this.floppyDiskDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    setScreenMode( 1 );
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.floppyDiskDrives != null ) {
      if( (idx >= 0) && (idx < this.floppyDiskDrives.length) ) {
	this.floppyDiskDrives[ idx ] = drive;
      }
    }
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    synchronized( this.keyboardMatrix ) {
      if( joyNum == 0 ) {
	this.joy0ActionMask = actionMask;
      } else if( joyNum == 1 ) {
	this.joy1ActionMask = actionMask;
      }
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    boolean rv  = false;
    int     idx = (addr & 0xFFFF) + this.ram16KOffs[ (addr >> 14) & 0x07 ];
    if( idx >= 0x10000 ) {
      if( this.ramExt != null ) {
	idx &= 0xFFFF;
	if( this.ramExt512K ) {
	  idx |= this.ramExtBankAddr;
	}
	if( idx < this.ramExt.length ) {
	  this.ramExt[ idx ] = (byte) value;
	  rv                 = true;
	}
      }
    } else {
      this.emuThread.setRAMByte( idx, value );
      if( (idx & 0xC000) == ((this.crtc.getStartAddr() << 2) & 0xC000) ) {
	this.screenDirty = true;
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsKeyboardFld()
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
  public String toString()
  {
    return "Zentrale Zustandssteuerung";
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    value &= 0xFF;
    if( (this.fdc != null)
	&& (port & 0x0580) == 0x0100 )		// A7=A10=0, A8=1: FDC
    {
      this.fdc.write( value );
    }
    if( (port & 0x0800) == 0 ) {		// A11=0: PPI
      this.ppi.write( port >> 8, value );
    }
    if( (port & 0x1000) == 0 ) {		// A12=0: CIO
      if( (port & 0x0300) == 0x0300 ) {		// Port A (Drucker): A8=A9=1
	PrintMngr pm = this.emuThread.getPrintMngr();
	if( pm != null ) {
	  boolean strobe = ((value & 0x80) != 0);
	  if( strobe != this.centronicsStrobe ) {
	    this.centronicsStrobe = strobe;
	    if( strobe ) {
	      pm.putByte( (this.ppi.fetchOutValueC() & 0x80)
						    | (value & 0x7F) );
	    }
	  }
	}
      }
    }
    if( (port & 0x2000) == 0 ) {		// A13=0: ROM Select
      this.romSelect = value;
    }
    if( (port & 0x4000) == 0 ) {		// A14=0: CRTC
      switch( port & 0x0300 ) {
	case 0x0000:				// A8=A9=0
	  this.crtc.setRegNum( value );
	  break;
	case 0x0100:				// A8=1, A9=0
	  this.crtc.write( value );
	  this.screenDirty = true;
	  break;
      }
    }
    if( (port & 0x8000) == 0 ) {		// A15=0
      switch( value & 0xC0 ) {
	case 0x00:				// Farbnummer
	  this.regColorNum = value;
	  break;
	case 0x40:				// Farbwert
	  if( (value & 0xC0) == 0x40 ) {
	    if( (this.regColorNum & 0x10) != 0 ) {
	      this.borderColorIdx = colorPalette2Idx[ value & 0x1F ];
	    } else {
	      this.regColors[ this.regColorNum & 0x0F ]
				= colorPalette2Idx[ value & 0x1F ];
	    }
	  } else {
	    if( (this.regColorNum & 0x10) != 0 ) {
	      this.borderColorIdx = 0;
	    } else {
	      this.regColors[ this.regColorNum & 0x0F ] = 0;
	    }
	  }
	  this.screenDirty = true;
	  break;
	case 0x80:				// Multifunktionsregister
	  this.osROMEnabled    = ((value & 0x04) == 0);
	  this.basicROMEnabled = ((value & 0x08) == 0);
	  if( (value & 0x10) != 0 ) {
	    this.lineIrqCounter = 52;
	  }
	  setScreenMode( value & 0x03 );
	  break;
	case 0xC0:				// RAM Konfiguration
	  synchronized( this.ram16KOffs ) {
	    Arrays.fill( this.ram16KOffs, 0 );
	    switch( value & 0x07 ) {
	      case 0x01:
		this.ram16KOffs[ 3 ] = 0x10000;	// 3 -> ext 3
		break;
	      case 0x02:
		this.ram16KOffs[ 0 ] = 0x10000;	// 0 -> ext 0
		this.ram16KOffs[ 1 ] = 0x10000;	// 1 -> ext 1
		this.ram16KOffs[ 2 ] = 0x10000;	// 2 -> ext 2
		this.ram16KOffs[ 3 ] = 0x10000;	// 3 -> ext 3
		break;
	      case 0x03:
		// laut Handbuch zu dk'tronic RAM-Erweiterung
		this.ram16KOffs[ 1 ] = 0x08000;	// 1 -> 3
		this.ram16KOffs[ 3 ] = 0x10000;	// 3 -> ext 3
		break;
	      case 0x04:
		this.ram16KOffs[ 1 ] = 0x0C000;
		break;
	      case 0x05:
		this.ram16KOffs[ 1 ] = 0x10000;
		break;
	      case 0x06:
		this.ram16KOffs[ 1 ] = 0x14000;
		break;
	      case 0x07:
		this.ram16KOffs[ 1 ] = 0x18000;
		break;
	    }
	    this.ramExtBankAddr = (value << 13) & 0x70000;
	  }
	  break;
      }
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.crtc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- private Methoden --- */

  private void addPixelCRC32ToMap(
			Map<Long,Character> map,
			int                 code,
			char                unicode )
  {
    CRC32 crc = new CRC32();
    crc.update( romOS, 0x3800 + (code * 8), 8 );
    map.put( crc.getValue(), Character.valueOf( unicode ) );
  }


  private void createColors( Properties props )
  {
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	int rgb = 0;
	if( i < rawRGBValues.length ) {
	  rgb = rawRGBValues[ i ];
	}
	int r = Math.round( ((rgb >> 16) & 0xFF) * brightness );
	int g = Math.round( ((rgb >> 8) & 0xFF) * brightness );
	int b = Math.round( (rgb & 0xFF) * brightness );

	this.colors[ i ] = new Color( (r << 16) | (g << 8) | b );
      }
    }
  }


  private static Map<Integer,String> getExtRomFileNames( Properties props )
  {
    Map<Integer,String> extRomFileNames = new HashMap<>();
    if( props != null ) {
      for( int romNum = 1; romNum < 16; romNum++ ) {
	String fileName = props.getProperty(
				getExtRomFilePropName( romNum ) );
	if( fileName != null ) {
	  if( !fileName.isEmpty() ) {
	    extRomFileNames.put( Integer.valueOf( romNum ), fileName );
	  }
	}
      }
    }
    return extRomFileNames;
  }


  private boolean emulatesRAMExt512K( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_EXT_RAM_512K,
			false );
  }


  private static int getJoyKeyboardMask( int joyActionMask )
  {
    int rv = 0;
    if( (joyActionMask & JoystickThread.LEFT_MASK) != 0 ) {
      rv |= 0x04;
    }
    if( (joyActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
      rv |= 0x08;
    }
    if( (joyActionMask & JoystickThread.DOWN_MASK) != 0 ) {
      rv |= 0x02;
    }
    if( (joyActionMask & JoystickThread.UP_MASK) != 0 ) {
      rv |= 0x01;
    }
    if( (joyActionMask & JoystickThread.BUTTON1_MASK) != 0 ) {
      rv |= 0x10;
    }
    if( (joyActionMask & JoystickThread.BUTTON2_MASK) != 0 ) {
      rv |= 0x20;
    }
    return rv;
  }


  private Map<Long,Character> getPixelCRC32ToCharMap()
  {
    if( pixelCRC32ToChar == null ) {
      if( romOS != null ) {
        if( romOS.length >= 0x4000 ) {
          Map<Long,Character> map = new HashMap<>();
          for( int c = 0x20; c <= 0x7E; c++ ) {
	    addPixelCRC32ToMap( map, c, (char) c );
	  }
	  for( int i = 0; i < char128ToUnicode.length; i++ ) {
	    int unicode = char128ToUnicode[ i ];
	    if( unicode >= 0 ) {
	      addPixelCRC32ToMap( map, i + 128, (char) unicode );
	    }
	  }
          pixelCRC32ToChar = map;
        }
      }
    }
    return pixelCRC32ToChar;
  }


  private boolean isFixedScreenSize( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_FIXED_SCREEN_SIZE,
			false );
  }


  private boolean setCharInKBMatrix( int[][] kbMatrix, char ch )
  {
    boolean rv = false;
    int     n  = Math.min( this.keyboardMatrix.length, kbMatrix.length );
    for( int i = 0; i < n; i++ ) {
      int   m = 0x01;
      int[] c = kbMatrix[ i ];
      for( int k = 0; k < c.length; k++ ) {
	if( ch == c[ k ] ) {
	  this.keyboardMatrix[ i ] |= m;
	  rv = true;
	  break;
	}
	m <<= 1;
      }
    }
    return rv;
  }


  private void setScreenMode( int mode )
  {
    if( (mode >= 0) && (mode <= 2) ) {
      if( this.screenMode != mode ) {
	int w = 320;
	if( mode == 2 ) {
	  w = 640;
	}
	this.screenMode = mode;
	this.screenWidth = w;
	if( !this.fixedScreenSize ) {
	  this.screenFrm.fireScreenSizeChanged();
	}
      }
    }
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}
