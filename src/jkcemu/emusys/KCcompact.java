/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des KCcompact
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.AudioFormat;
import jkcemu.audio.AudioOut;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.kccompact.KCcompactKeyboardFld;
import jkcemu.etc.*;
import jkcemu.joystick.JoystickThread;
import jkcemu.print.PrintMngr;
import jkcemu.text.TextUtil;
import z80emu.*;


public class KCcompact extends EmuSys implements
					CRTC6845.SyncListener,
					FDC8272.DriveSelector,
					PPI8255.Callback,
					PSG8910.Callback,
					Z80InstrTStatesMngr,
					Z80InterruptSource,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static final FloppyDiskInfo[] availableFloppyDisks = {
		new FloppyDiskInfo(
			"/disks/kccompact/kccmicrodos.dump.gz",
			"KC compact MicroDOS Systemdiskette" ) };

  /*
   * Diese Tabelle mappt die unteren 4 Bits des Farbpalettenwertes
   * auf den Index in der Farbtabelle.
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

  private static byte[] romOS    = null;
  private static byte[] romBASIC = null;
  private static byte[] romFDC   = null;

  private Color[]              colors;
  private int[]                regColors;
  private int                  regColorNum;
  private int                  borderColorIdx;
  private int                  romSelect;
  private String               fdcROMFile;
  private byte[]               fdcROMBytes;
  private byte[]               screenBuf;
  private byte[]               ramExt;
  private int[]                ram16KOffs;
  private int[]                keyboardMatrix;
  private int                  keyboardIdx;
  private int                  keyboardValue;
  private int                  psgRegNum;
  private int                  psgValue;
  private int                  lineIrqCounter;
  private int                  joy0ActionMask;
  private int                  joy1ActionMask;
  private int                  screenMode;
  private volatile int         screenWidth;
  private boolean              screenDirty;
  private boolean              screenRefreshEnabled;
  private volatile boolean     fixedScreenSize;
  private boolean              interruptRequested;
  private boolean              centronicsStrobe;
  private boolean              basicROMEnabled;
  private boolean              osROMEnabled;
  private AudioOut             psgAudioOut;
  private PSG8910              psg;
  private PPI8255              ppi;
  private CRTC6845             crtc;
  private KCcompactKeyboardFld keyboardFld;
  private FDC8272              fdc;
  private FloppyDiskDrive[]    floppyDiskDrives;


  public KCcompact( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( romOS == null ) {
      romOS = readResource( "/rom/kccompact/kccos.bin" );
    }
    if( romBASIC == null ) {
      romBASIC = readResource( "/rom/kccompact/kccbasic.bin" );
    }
    this.colors = new Color[ 27 ];
    createColors( props );

    this.keyboardFld     = null;
    this.fixedScreenSize = isFixedScreenSize( props );
    this.screenMode      = 0;
    this.screenWidth     = 320;
    this.screenBuf       = new byte[ 640 * 200 ];  // max. interne Pixelanzahl
    this.keyboardMatrix  = new int[ 10 ];
    this.regColors       = new int[ 16 ];
    Arrays.fill( this.regColors, 0 );

    this.ram16KOffs  = new int[ 4 ];
    this.crtc        = new CRTC6845( 1000, this );
    this.ppi         = new PPI8255( this );
    this.psg         = new PSG8910( 1000000, AudioOut.MAX_VALUE, this );
    this.psgAudioOut = null;

    if( emulatesFloppyDisk( props ) ) {
      this.ramExt           = this.emuThread.getExtendedRAM( 0x10000 );
      this.fdc              = new FDC8272( this, 4 );
      this.floppyDiskDrives = new FloppyDiskDrive[ 2 ];
      Arrays.fill( this.floppyDiskDrives, null );
    } else {
      this.ramExt           = null;
      this.fdc              = null;
      this.floppyDiskDrives = null;
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( this );
    cpu.setInstrTStatesMngr( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    this.psg.start();
  }


  public static int getDefaultSpeedKHz()
  {
    return 4000;
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
      if( this.emuThread.readAudioPhase() ) {
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
      this.keyboardIdx = value & 0x0F;
      if( this.psgAudioOut == null ) {
	this.emuThread.writeAudioPhase( (value & 0x20) != 0 );
      }
      this.psgValue = this.psg.access(
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
  public void psgWriteSample( PSG8910 psg, int a, int b, int c )
  {
    AudioOut audioOut = this.psgAudioOut;
    if( audioOut != null ) {
      audioOut.writeSamples( 1, (byte) ((a + b + c) / 6) );
    }
  }


	/* --- Z80InstrTStatesMngr --- */

  @Override
  public int z80IntructionProcessed( Z80CPU cpu, int pc, int tStates )
  {
    int m = (tStates + 3) / 4;
    return m * 4;
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  /*
   * Die Hardware des KCcompact reagiert nicht auf RETI-Befehle,
   * weshalb diese speziellen Interrupt-Return-Befehle
   * auch nicht vewendet werden.
   * Damit wird aber auch "interruptFinished()" nie aufgerufen.
   * Aus diesem Grund muss der Interrupt-Zustand mit der
   * Interrupt-Annahme bereits zurueckgesetzt werden, was bedeutet,
   * dass "isInterruptAccepted()" nie true zurueckliefern kann.
   */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<table border=\"1\">\n"
	+ "<tr><td>Im vertiklaen Synchronimpuls:</td><td>" );
    buf.append( this.crtc.isVSync() ? "ja" : "nein" );
    buf.append( "</td></tr>\n"
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
  public void interruptFinish()
  {
    // leer
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


  @Override
  public void reset( boolean powerOn )
  {
    Arrays.fill( this.screenBuf, (byte) 0 );
    Arrays.fill( this.ram16KOffs, 0 );
    if( this.ramExt != null ) {
      Arrays.fill( this.ramExt, (byte) 0 );
    }
    Arrays.fill( this.keyboardMatrix, 0 );
    this.psgRegNum            = -1;
    this.psgRegNum            = 0xFF;
    this.regColorNum          = 0;
    this.borderColorIdx       = 0;
    this.keyboardIdx          = 0;
    this.keyboardValue        = 0xFF;
    this.romSelect            = 0;
    this.lineIrqCounter       = 0;
    this.interruptRequested   = false;
    this.centronicsStrobe     = false;
    this.basicROMEnabled      = true;
    this.osROMEnabled         = true;
    this.screenDirty          = true;
    this.screenRefreshEnabled = false;
    this.ppi.reset();
    this.psg.reset();
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


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.crtc.z80MaxSpeedChanged( cpu );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.crtc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- ueberschriebene Methoden --- */

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
  public void audioOutChanged( AudioOut audioOut )
  {
    int sampleRate = 0;
    if( audioOut != null ) {
      if( audioOut.isSoundOutEnabled() ) {
	AudioFormat fmt = audioOut.getAudioFormat();
	if( fmt != null ) {
	  sampleRate = Math.round( fmt.getSampleRate() );
	}
      }
    }
    this.psg.setSampleRate( sampleRate );
    this.psgAudioOut = (sampleRate > 0 ? audioOut : null);
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( "KCcompact" );
    if( rv && ((this.fdc != null) != emulatesFloppyDisk( props )) ) {
      rv = false;
    }
    if( rv && (this.fdc != null) ) {
      rv = TextUtil.equals(
		this.fdcROMFile,
		EmuUtil.getProperty(
				props, 
				"jkcemu.kccompact.fdc.rom.file" ) );
    }
    return rv;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new KCcompactKeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    cpu.setInstrTStatesMngr( null );
    this.psg.die();
    if( this.fdc != null ) {
      this.fdc.die();
    }
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
  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return true;
  }


  @Override
  public int getDefaultFloppyDiskBlockSize()
  {
    return this.fdc != null ? 2048 : -1;
  }


  @Override
  public int getDefaultFloppyDiskDirBlocks()
  {
    return this.fdc != null ? 3 : -1;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return this.fdc != null ?
                FloppyDiskFormat.getFormat( 2, 80, 9, 512 )
                : null;
  }


  @Override
  public int getDefaultFloppyDiskSystemTracks()
  {
    return this.fdc != null ? 2 : -1;
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
      if( romOS != null ) {
	if( addr < romOS.length ) {
	  rv = (int) romOS[ addr ] & 0xFF;
	}
      }
    } else if( (addr >= 0xC000) && this.basicROMEnabled ) {
      boolean done = false;
      byte[]  rom  = this.fdcROMBytes;
      if( this.fdc != null ) {
	if( this.romSelect == 6 ) {
	  int idx = addr - 0xC000 + 0x4000;
	  if( (idx >= 0x4000) && (idx < rom.length) ) {
	    rv   = (int) rom[ idx ] & 0xFF;
	    done = true;
	  }
	} else if( this.romSelect == 7 ) {
	  rom = this.fdcROMBytes;
	  int idx = addr - 0xC000;
	  if( (idx >= 0) && (idx < 0x4000) && (idx < rom.length) ) {
	    rv   = (int) rom[ idx ] & 0xFF;
	    done = true;
	  }
	}
      }
      if( !done ) {
	rom = romBASIC;
	if( rom != null ) {
	  int idx = addr - 0xC000;
	  if( (idx >= 0) && (idx < rom.length) ) {
	    rv = (int) rom[ idx ] & 0xFF;
	  }
	}
      }
    } else {
      int idx = addr + this.ram16KOffs[ (addr >> 14) & 0x03 ];
      if( idx >= 0x10000 ) {
	if( this.ramExt != null ) {
	  idx &= 0xFFFF;
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
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0x0000;
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
    return "KC compact";
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


  @Override
  public int readIOByte( int port )
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
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	&& isReloadExtROMsOnPowerOnEnabled( props ) )
    {
      loadROMs( props );
    }
    this.joy0ActionMask = 0;
    this.joy1ActionMask = 0;
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
    int     idx = (addr & 0xFFFF) + this.ram16KOffs[ (addr >> 14) & 0x03 ];
    if( idx >= 0x10000 ) {
      if( this.ramExt != null ) {
	idx &= 0xFFFF;
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
  public boolean supportsAudio()
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
  public String toString()
  {
    return "Zentrale Zustandssteuerung";
  }


  @Override
  public void writeIOByte( int port, int value )
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
	      this.borderColorIdx = this.colorPalette2Idx[ value & 0x1F ];
	    } else {
	     this.regColors[ this.regColorNum & 0x0F ]
				= this.colorPalette2Idx[ value & 0x1F ];
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
	  Arrays.fill( this.ram16KOffs, 0 );
	  switch( value & 0x07 ) {
	    case 0x01:
	      this.ram16KOffs[ 3 ] = 0x10000;
	      break;
	    case 0x02:
	      Arrays.fill( this.ram16KOffs, 0x10000 );
	      break;
	    case 0x04:
	      this.ram16KOffs[ 1 ] = 0xC0000;
	      break;
	    case 0x05:
	      this.ram16KOffs[ 1 ] = 0x100000;
	      break;
	    case 0x06:
	      this.ram16KOffs[ 1 ] = 0x140000;
	      break;
	    case 0x07:
	      this.ram16KOffs[ 1 ] = 0x180000;
	      break;
	  }
	  break;
      }
    }
  }


	/* --- private Methoden --- */

  private void createColors( Properties props )
  {
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      int mRed   = 0;
      int mGreen = 0;
      int mBlue  = 0;
      for( int i = 0; i < this.colors.length; i++ ) {
	int r = Math.round( mRed * brightness );
	int g = Math.round( mGreen * brightness );
	int b = Math.round( mBlue * brightness );

	this.colors[ i ] = new Color( (r << 16) | (g << 8) | b );

	if( mBlue == 0 ) {
	  mBlue = 128;
	} else if( mBlue == 128 ) {
	  mBlue = 255;
	} else {
	  mBlue = 0;
	  if( mRed == 0 ) {
	    mRed = 128;
	  } else if( mRed == 128 ) {
	    mRed = 255;
	  } else {
	    mRed = 0;
	    if( mGreen == 0 ) {
	      mGreen = 128;
	    } else {
	      mGreen = 255;
	    }
	  }
	}
      }
    }
  }


  private static boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.kccompact.floppydisk.enabled",
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


  private static boolean isFixedScreenSize( Properties props )
  {
    return EmuUtil.parseBooleanProperty(
			props,
			"jkcemu.kccompact.fixed_screen_size",
			false );
  }


  private void loadROMs( Properties props )
  {
    if( this.fdc != null ) {
      this.fdcROMFile = EmuUtil.getProperty(
				props,
				"jkcemu.kccompact.fdc.rom.file" );
      this.fdcROMBytes = readFile( this.fdcROMFile, 0x8000, "FDC-ROM" );
      if( this.fdcROMBytes == null ) {
	if( romFDC == null ) {
	  romFDC = readResource( "/rom/kccompact/basdos.bin" );
	}
	this.fdcROMBytes = romFDC;
      }
    } else {
      this.fdcROMFile  = null;
      this.fdcROMBytes = null;
    }
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
