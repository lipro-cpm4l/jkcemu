/*
 * (c) 2010-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BIC A5105 und des ALBA PC 1505
 *
 * Emulation des Sound-Generators:
 * Die Bedeutung der Sound-Register ist fast identisch zu dem Schaltkreis
 * AY-3-8910. Den einzigen Unterschied gibt es beim Register 7.
 * Dort sind die Bits beim A5105 Bits H-aktiv, beim AY-3-8910 dagegen L-aktiv.
 * Aus diesem Grund wird zur Sound-Emulation die Klasse PSG8910 verwendet
 * und das Register 7 entsprechend anders behandelt.
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.sound.sampled.AudioFormat;
import jkcemu.audio.AudioOut;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.a5105.*;
import jkcemu.etc.*;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.text.CharConverter;
import z80emu.*;


public class A5105 extends EmuSys implements
					FDC8272.DriveSelector,
					PSG8910.Callback,
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80TStatesListener
{
  /*
   * Das SCPX bringt einen Fehler, wenn die Diskette schreibgeschuetzt ist.
   * Das ist somit der Fall, wenn die in JKCEMU integrierte
   * SCPX-Systemdiskette eingelegt wird.
   * Um diese Unschoenheit zu umgehen, sollte die SCPX-Systemdiskette
   * exportiert und die so entstandene Abbilddatei ohne Schreibschutz
   * verwendet werden.
   * Aus diesem Grund wird die SCPX-Systemdiskette nur als "verfuegbare",
   * nicht aber als "direkt zum Einlegen geeignete" Diskette angeboten.
   */
  private static FloppyDiskInfo rbasicPrgDisk =
		new FloppyDiskInfo(
			"/disks/a5105/a5105rbasicprg.dump.gz",
                        "BIC A5105 RBASIC Programmdiskette" );

  private static FloppyDiskInfo rbasicSysDisk =
		new FloppyDiskInfo(
			"/disks/a5105/a5105rbasicsys.dump.gz",
                        "BIC A5105 RBASIC Systemdiskette" );

  private static final FloppyDiskInfo[] availableFloppyDisks = {
		rbasicPrgDisk,
		rbasicSysDisk,
		new FloppyDiskInfo(
			"/disks/a5105/a5105scpxsys.dump.gz",
                        "BIC A5105 SCPX Systemdiskette" ) };

  private static final FloppyDiskInfo[] suitableFloppyDisks = {
							rbasicPrgDisk,
							rbasicSysDisk };

  private static final int BIOS_ADDR_CONIN     = 0xFD09;
  private static final int V24_TSTATES_PER_BIT = 347;

  private static CharConverter cp437    = null;
  private static byte[]        romK1505 = null;
  private static byte[]        romK5651 = null;

  /*
   * Die Tastaturmatrix enthaelt die Spalten 0-7 und die Zeilen 0-8.
   * In den Zeilen 6 bis 8 sind nur Sondertasten zu finden,
   * die separat behandelt werden und somit in den drei nachfolgenden
   * Matrizen nicht enthalten sind, d.h., dort fehlen diese Zeilen.
   *
   * Fuer die Zeilen 6 bis 8 gilt folgendene Tastenbelegung:
   *      0       1         2       3         4     5      6       7
   *   6  Shift   Escape    Control Caps Lock Alt   F5/run F4/list F3/load
   *   7  F2/auto F1/screen Graph   -         Stop  -      -       Enter
   *   8  Space   Home      InsMode Delete    Links Hoch   Runter  Rechts
   */
  private static int[][] kbMatrixNormal = {
	{ '0',  '1', '2', '3', '4',      '5',      '6',      '7' },
	{ '8',  '9', '<', '+', '\u00F6', '\u00E4', '\u00FC', '#' },
	{ '\'', '?', ',', '.', '-',      -1,      'a',      'b' },
	{ 'c',  'd', 'e', 'f', 'g',      'h',      'i',      'j' },
	{ 'k',  'l', 'm', 'n', 'o',      'p',      'q',      'r' },
	{ 's',  't', 'u', 'v', 'w',      'x',      'y',      'z' } };

  private static int[][] kbMatrixShift = {
	{ '=',      '!',      '\"', '\\', '$',      '%',      '&',      '/' },
	{ '(',      ')',      '>',  '*',  '\u00D6', '\u00C4', '\u00DC', '^' },
	{ '\u0060', '\u00DF', ';',  ':',  '_',      '=',      'A',      'B' },
	{ 'C',      'D',      'E',  'F',  'G',      'H',      'I',      'J' },
	{ 'K',      'L',      'M',  'N',  'O',      'P',      'Q',      'R' },
	{ 'S',      'T',      'U',  'V',  'W',      'X',      'Y',      'Z' } };

  private static int[][] kbMatrixControl = {
	{ -1, -1, -1, -1, -1, -1, -1, -1 },
	{ -1, -1, -1, -1, -1, -1, -1, -1 },
	{ -1, -1, -1, -1, -1, -1,  1,  2 },
	{  3,  4,  5,  6,  7,  8,  9, 10 },
	{ 11, 12, 13, 14, 15, 16, 17, 18 },
	{ 19, 20, 21, 22, 23, 24, 25, 26 } };

  private Z80CTC            ctc80;
  private Z80PIO            pio90;
  private GDC82720          gdc;
  private VIS               vis;
  private PSG8910           psg;
  private AudioOut          psgAudioOut;
  private FDC8272           fdc;
  private FloppyDiskDrive[] floppyDiskDrives;
  private KCNet             kcNet;
  private VDIP              vdip;
  private RAMFloppy         ramFloppy1;
  private RAMFloppy         ramFloppy2;
  private boolean           fdcReset;
  private boolean           joyEnabled;
  private boolean           joy1Selected;
  private int               joy0ActionMask;
  private int               joy1ActionMask;
  private boolean           capsLockLED;
  private boolean           tapeLED;
  private boolean           v24BitOut;
  private int               v24BitNum;
  private int               v24ShiftBuf;
  private int               v24TStateCounter;
  private int               shiftTStateCounter;
  private int               memConfig;
  private int               ledValues;
  private int               psgRegNum;
  private int               keyboardCol;
  private int[]             keyboardMatrix;
  private A5105KeyboardFld  keyboardFld;
  private volatile boolean  pasteFast;


  public A5105( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( romK1505 == null ) {
      romK1505 = readResource( "/rom/a5105/k1505_0000.bin" );
    }
    this.pasteFast      = false;
    this.capsLockLED    = false;
    this.tapeLED        = false;
    this.keyboardFld    = null;
    this.keyboardCol    = -1;
    this.keyboardMatrix = new int[ 9 ];
    Arrays.fill( keyboardMatrix, 0 );

    if( emulatesFloppyDisk( props ) ) {
      if( romK5651 == null ) {
	romK5651 = readResource( "/rom/a5105/k5651_4000.bin" );
      }
      this.fdc              = new FDC8272( this, 4 );
      this.floppyDiskDrives = new FloppyDiskDrive[ 3 ];
      Arrays.fill( this.floppyDiskDrives, null );
    } else {
      this.fdc              = null;
      this.floppyDiskDrives = null;
    }
    this.gdc = new GDC82720();
    this.vis = new VIS( this.screenFrm, this.gdc );
    this.vis.setFixedScreenSize( isFixedScreenSize( props ) );
    this.gdc.setVRAM( this.vis );
    this.gdc.setGDCListener( this.vis );
    createColors( props );

    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"A5105",
				RAMFloppy.RFType.ADW,
				"RAM-Floppy an IO-Adressen 20h/21h",
				props,
				"jkcemu.a5105.ramfloppy.1." );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				"A5105",
				RAMFloppy.RFType.ADW,
				"RAM-Floppy an IO-Adressen 24h/25h",
				props,
				"jkcemu.a5105.ramfloppy.2." );

    this.psgAudioOut = null;
    this.psg         = new PSG8910(
				getDefaultSpeedKHz() * 1000 / 2,
				AudioOut.MAX_VALUE,
				this );
    this.psg.start();

    this.ctc80 = new Z80CTC( "CTC (IO-Adressen 80h-83h)" );
    this.pio90 = new Z80PIO( "PIO (IO-Adressen 90h-93h)" );

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (IO-Adressen C0h-C3h)" );
    }

    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP( "USB-PIO (IO-Adressen FCh-FFh)" );
    }

    java.util.List<Z80InterruptSource> iSources
                                = new ArrayList<Z80InterruptSource>();
    iSources.add( this.ctc80 );
    iSources.add( this.pio90 );
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

    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    this.ctc80.setTimerConnection( 0, 2 );
    this.ctc80.setTimerConnection( 2, 3 );
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
      cpu.addMaxSpeedListener( this.kcNet );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    z80MaxSpeedChanged( cpu );
    checkAddPCListener( props );
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public boolean getCapsLockLED()
  {
    return this.capsLockLED;
  }


  public static int getDefaultSpeedKHz()
  {
    return 3750;
  }


  public boolean getTapeLED()
  {
    return this.tapeLED;
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
      this.shiftTStateCounter = 0;
    }
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.floppyDiskDrives != null ) {
      if( (driveNum >= 0) && (driveNum < this.floppyDiskDrives.length) ) {
	rv = this.floppyDiskDrives[ driveNum ];
      }
    }
    return rv;
  }


	/* --- PSG8910.Callback --- */

  @Override
  public int psgReadPort( PSG8910 psg, int port )
  {
    return 0xFF;
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


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.gdc.z80MaxSpeedChanged( cpu );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
    }
  }


	/* --- Z80PCListener --- */

  @Override
  public synchronized void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( this.pasteFast && (pc == BIOS_ADDR_CONIN) ) {
      CharacterIterator iter = this.pasteIter;
      if( iter != null ) {
	char ch = iter.next();
	while( ch != CharacterIterator.DONE ) {
	  if( (ch > 0) && (ch < 0x7F) ) {
	    cpu.setRegA( ch == '\n' ? '\r' : ch );
	    cpu.setRegPC( cpu.doPop() );
	    break;
	  }
	  ch = iter.next();
	}
	if( ch == CharacterIterator.DONE ) {
	  keyReleased();
	  cancelPastingText();
	}
      }
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc80.z80TStatesProcessed( cpu, tStates );
    this.gdc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    synchronized( this.keyboardMatrix ) {
      if( this.shiftTStateCounter > 0 ) {
	this.shiftTStateCounter -= tStates;
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
	    this.v24TStateCounter = V24_TSTATES_PER_BIT;
	    this.v24BitNum++;
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>A5105 Speicherkonfiguration</h1>\n"
	+ "<table border=\"2\">\n"
	+ "<tr><th>Adressbereich</th><th>Eingeblendeter Speicher</th></tr>\n"
	+ "<tr><td>C000h - FFFFh</td><td>" );
    switch( this.memConfig & 0xC0 ) {
      case 0x00:
	buf.append( "ROM im Grundger&auml;t (bis 9FFFh)" );
	break;
      case 0x40:
	buf.append( "Modul in der Datenspeichereinheit (nicht emuliert)" );
	break;
      case 0x80:
	buf.append( "RAM" );
	break;
      case 0xC0:
	buf.append( "Modul im Grundger&auml;t (nicht emuliert)" );
	break;
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>8000h - BFFFh</td><td>" );
    switch( this.memConfig & 0x30 ) {
      case 0x00:
	buf.append( "ROM im Grundger&auml;t (bis 9FFFh)" );
	break;
      case 0x10:
	buf.append( "Modul in der Datenspeichereinheit (nicht emuliert)" );
	break;
      case 0x20:
	buf.append( "RAM" );
	break;
      case 0x30:
	buf.append( "Modul im Grundger&auml;t (nicht emuliert)" );
	break;
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>4000h - 7FFFh</td><td>" );
    switch( this.memConfig & 0x0C ) {
      case 0x00:
	buf.append( "ROM im Grundger&auml;t" );
	break;
      case 0x04:
	buf.append( "ROM in der Datenspeichereinheit" );
	break;
      case 0x08:
	buf.append( "RAM" );
	break;
      case 0x0C:
	buf.append( "Modul im Grundger&auml;t (nicht emuliert)" );
	break;
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>0000h - 3FFFh</td><td>" );
    switch( this.memConfig & 0x03 ) {
      case 0x00:
	buf.append( "ROM im Grundger&auml;t" );
	break;
      case 0x01:
	buf.append( "Modul in der Datenspeichereinheit (nicht emuliert)" );
	break;
      case 0x02:
	buf.append( "RAM" );
	break;
      case 0x03:
	buf.append( "Modul im Grundger&auml;t (nicht emuliert)" );
	break;
    }
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );

    boolean state = isFixedScreenSize( props );
    if( state != this.vis.isFixedScreenSize() ) {
      this.vis.setFixedScreenSize( state );
      this.screenFrm.fireScreenSizeChanged();
    }
    createColors( props );
    checkAddPCListener( props );
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
				"jkcemu.system" ).equals( "A5105" );
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			"A5105",
			RAMFloppy.RFType.ADW,
			props,
			"jkcemu.a5105.ramfloppy.1." );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			"A5105",
			RAMFloppy.RFType.ADW,
			props,
			"jkcemu.a5105.ramfloppy.2." );
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
    return this.vis.canExtractScreenText();
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new A5105KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public synchronized void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pasteFast ) {
      cpu.removePCListener( this );
      this.pasteFast = false;
    }
    this.psg.die();
    this.gdc.setGDCListener( null );
    this.gdc.setVRAM( null );
    if( this.ramFloppy1 != null ) {
      this.ramFloppy1.deinstall();
    }
    if( this.ramFloppy2 != null ) {
      this.ramFloppy2.deinstall();
    }
    if( this.fdc != null ) {
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
  public int getBorderColorIndex()
  {
    return this.vis.getBorderColorIndex();
  }


  @Override
  public Color getColor( int idx )
  {
    return this.vis.getColor( idx );
  }


  @Override
  public int getColorCount()
  {
    return this.vis.getColorCount();
  }


  @Override
  protected boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster rv = null;
    if( canExtractScreenText() ) {
      int charHeight = Math.min( this.gdc.getCharRowHeight(), 8 );
      int rowHeight  = this.gdc.getCharRowHeight();
      if( this.vis.isFixedScreenSize() || this.screenFrm.isFullScreenMode() ) {
	charHeight *= 2;
	rowHeight *= 2;
      }
      rv = new CharRaster(
			this.vis.getCharColCount(),
			this.vis.getCharRowCount(),
			rowHeight,
			charHeight,
			this.vis.getCharWidth(),
			this.vis.getCharTopLine() );
    }
    return rv;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_780K;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/a5105.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr >= 0) && (addr < 0x4000) ) {
      switch( this.memConfig & 0x03 ) {
	case 0x00:
	  if( romK1505 != null ) {
	    if( addr < romK1505.length ) {
	      rv = (int) romK1505[ addr ] & 0xFF;
	    }
	  }
	  break;

	case 0x02:
	  rv = this.emuThread.getRAMByte( addr );
	  break;
      }
    }
    else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      switch( this.memConfig & 0x0C ) {
	case 0x00:
	  if( romK1505 != null ) {
	    if( addr < romK1505.length ) {
	      rv = (int) romK1505[ addr ] & 0xFF;
	    }
	  }
	  break;

	case 0x04:
	  if( (this.fdc != null) && (romK5651 != null) ) {
	    int idx = addr - 0x4000;
	    if( (idx >= 0) && (idx < romK5651.length) ) {
	      rv = (int) romK5651[ idx ] & 0xFF;
	    }
	  }
	  break;

	case 0x08:
	  rv = this.emuThread.getRAMByte( addr );
	  break;
      }
    }
    else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      switch( this.memConfig & 0x30 ) {
	case 0x00:
	  if( romK1505 != null ) {
	    if( addr < romK1505.length ) {
	      rv = (int) romK1505[ addr ] & 0xFF;
	    }
	  }
	  break;

	case 0x20:
	  rv = this.emuThread.getRAMByte( addr );
	  break;
      }
    }
    else if( addr >= 0xC000 ) {
      if( (this.memConfig & 0xC0) == 0x80 ) {
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
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    if( cp437 == null ) {
      cp437 = new CharConverter( CharConverter.Encoding.CP437 );
    }
    return cp437.toUnicode( this.gdc.getScreenChar( chX, chY ) );
  }


  @Override
  public int getScreenHeight()
  {
    return this.vis.getScreenHeight();
  }


  @Override
  public int getScreenWidth()
  {
    return this.vis.getScreenWidth();
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return this.fdc != null ? suitableFloppyDisks : null;
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
    return "A5105";
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
    synchronized( this.keyboardMatrix ) {
      switch( keyCode ) {
	case KeyEvent.VK_BACK_SPACE:
	case KeyEvent.VK_LEFT:
	  this.keyboardMatrix[ 8 ] |= 0x10;
	  rv = true;
	  break;
	case KeyEvent.VK_RIGHT:
	  this.keyboardMatrix[ 8 ] |= 0x80;
	  rv = true;
	  break;
	case KeyEvent.VK_UP:
	  this.keyboardMatrix[ 8 ] |= 0x20;
	  rv = true;
	  break;
	case KeyEvent.VK_DOWN:
	  this.keyboardMatrix[ 8 ] |= 0x40;
	  rv = true;
	  break;
	case KeyEvent.VK_SPACE:
	  this.keyboardMatrix[ 8 ] |= 0x01;
	  rv = true;
	  break;
	case KeyEvent.VK_ENTER:
	  this.keyboardMatrix[ 7 ] |= 0x80;
	  rv = true;
	  break;
	case KeyEvent.VK_ESCAPE:
	  this.keyboardMatrix[ 6 ] |= 0x02;
	  rv = true;
	  break;
	case KeyEvent.VK_DELETE:
	  this.keyboardMatrix[ 8 ] |= 0x08;
	  rv = true;
	  break;
	case KeyEvent.VK_INSERT:		// INS MODE
	  this.keyboardMatrix[ 8 ] |= 0x04;
	  rv = true;
	  break;
	case KeyEvent.VK_F1:			// PF1
	  this.keyboardMatrix[ 7 ] |= 0x02;
	  rv = true;
	  break;
	case KeyEvent.VK_F2:			// PF2
	  this.keyboardMatrix[ 7 ] |= 0x01;
	  rv = true;
	  break;
	case KeyEvent.VK_F3:			// PF3
	  this.keyboardMatrix[ 6 ] |= 0x80;
	  rv = true;
	  break;
	case KeyEvent.VK_F4:			// PF4
	  this.keyboardMatrix[ 6 ] |= 0x40;
	  rv = true;
	  break;
	case KeyEvent.VK_F5:			// PF5
	  this.keyboardMatrix[ 6 ] |= 0x20;
	  rv = true;
	  break;
	case KeyEvent.VK_F6:			// HOME
	case KeyEvent.VK_HOME:
	  this.keyboardMatrix[ 8 ] |= 0x02;
	  rv = true;
	  break;
	case KeyEvent.VK_F7:			// STOP
	  this.keyboardMatrix[ 7 ] |= 0x10;
	  rv = true;
	  break;
	case KeyEvent.VK_SHIFT:			// rv=false!
	  this.keyboardMatrix[ 6 ] |= 0x01;
	  break;
	case KeyEvent.VK_CONTROL:		// rv=false!
	  this.keyboardMatrix[ 6 ] |= 0x04;
	  break;
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
      /*
       * Da die Control- und Shift-Taste direkt auf die emulierte
       * Tastaturmatrix abgebildet wird, kommt es zu fehlerhaften Eingaben,
       * wenn z.B. ein Zeichen auf der A5105-Tastaur nicht,
       * aber auf der Tastatur des Emulatorrechners in der Shift-Ebene liegt.
       * Aus diesem Grund wird geprueft,
       * ob sich der Control- oder Shift-Status aendert.
       * Ist das der Fall, werden eine Zeit lang erstmal nur die
       * Control- und Shift-Taste gelesen und spaeter dann
       * die anderen Tasten eingeblendet.
       */
      synchronized( this.keyboardMatrix ) {
	boolean oldControl = ((this.keyboardMatrix[ 6 ] & 0x04) != 0);
	boolean oldShift   = ((this.keyboardMatrix[ 6 ] & 0x01) != 0);
	Arrays.fill( this.keyboardMatrix, 0 );
	switch( ch ) {
	  case '@':
	    this.keyboardMatrix[ 6 ] = 0x10;	// Alt
	    this.keyboardMatrix[ 0 ] = 0x04;
	    rv = true;
	    break;

	  case '\u00A7':			// Paragraph
	    this.keyboardMatrix[ 6 ] = 0x10;	// Alt
	    this.keyboardMatrix[ 0 ] = 0x08;
	    rv = true;
	    break;

	  case '|':
	    this.keyboardMatrix[ 6 ] = 0x11;	// Alt+Shift
	    this.keyboardMatrix[ 1 ] = 0x20;
	    rv = true;
	    break;

	  case '[':
	    this.keyboardMatrix[ 6 ] = 0x10;	// Alt
	    this.keyboardMatrix[ 1 ] = 0x01;
	    rv = true;
	    break;

	  case ']':
	    this.keyboardMatrix[ 6 ] = 0x10;	// Alt
	    this.keyboardMatrix[ 1 ] = 0x02;
	    rv = true;
	    break;

	  case '{':
	    this.keyboardMatrix[ 6 ] = 0x11;	// Alt+Shift
	    this.keyboardMatrix[ 1 ] = 0x01;
	    rv = true;
	    break;

	  case '}':
	    this.keyboardMatrix[ 6 ] = 0x11;	// Alt+Shift
	    this.keyboardMatrix[ 1 ] = 0x02;
	    rv = true;
	    break;

	  case '~':
	    this.keyboardMatrix[ 7 ] = 0x04;	// Graph
	    this.keyboardMatrix[ 6 ] = 0x01;	// Shift
	    this.keyboardMatrix[ 1 ] = 0x80;
	    rv = true;
	    break;

	  default:
	    rv = setCharInKBMatrix( kbMatrixNormal, ch );
	    if( !rv ) {
	      rv = setCharInKBMatrix( kbMatrixShift, ch );
	      if( rv ) {
		this.keyboardMatrix[ 6 ] = 0x01;
	      } else {
		rv = setCharInKBMatrix( kbMatrixControl, ch );
		if( rv ) {
		  this.keyboardMatrix[ 6 ] |= 0x04;
		}
	      }
	    }
	}
	boolean newControl = ((this.keyboardMatrix[ 6 ] & 0x04) != 0);
	boolean newShift   = ((this.keyboardMatrix[ 6 ] & 0x01) != 0);
	if( (newControl != oldControl) || (newShift != oldShift) ) {
	  this.shiftTStateCounter = 100000;
	}
      }
      if( rv ) {
	updKeyboardFld();
      }
    }
    return rv;
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    this.vis.paintScreen( g, x, y, screenScale );
    return true;
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x20:
	if( this.ramFloppy1 != null ) {
	  rv = this.ramFloppy1.readByte( port );
	}
	break;

      case 0x24:
	if( this.ramFloppy2 != null ) {
	  rv = this.ramFloppy2.readByte( port );
	}
	break;

      case 0x40:
      case 0x42:
      case 0x44:
      case 0x46:
	if( this.fdc != null ) {
	  rv = this.fdc.readMainStatusReg();
	}
	break;

      case 0x41:
      case 0x43:
      case 0x45:
      case 0x47:
	if( this.fdc != null ) {
	  rv = this.fdc.readData();
	}
	break;

      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	rv = this.ctc80.read( port & 0x03 );
	break;

      case 0x90:
	rv = this.pio90.readPortA();
	break;

      case 0x91:
	/*
	 * Bit 4: V24-Status (auf 0 setzen)
	 * Bit 7: Kassettenrecordereingang
	 */
	this.pio90.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x80 : 0, 0x90 );
	rv = this.pio90.readPortB();
	break;

      case 0x92:
	rv = this.pio90.readControlA();
	break;

      case 0x93:
	rv = this.pio90.readControlB();
	break;

      case 0x98:
	rv = this.gdc.readStatus();
	break;

      case 0x99:
	rv = this.gdc.readData();
	break;

      case 0x9C:
	rv = this.vis.readFontByte();
	break;

      case 0xA1:				// Sound-Register lesen
	if( this.psgRegNum == 7 ) {
	  rv = ~this.psg.getRegister( this.psgRegNum ) & 0xFF;
	} else if( this.psgRegNum <= 13 ) {
	  rv = this.psg.getRegister( this.psgRegNum );
	}
	break;

      case 0xA8:				// SVG Port A
	rv = this.memConfig;
	break;

      case 0xA9:				// SVG Port B
	if( this.joyEnabled ) {
	  rv    = 0;
	  int m = (this.joy1Selected ?
				this.joy0ActionMask
				: this.joy1ActionMask);
	  if( (m & JoystickThread.LEFT_MASK) != 0 ) {
	    rv |= 0x08;
	  }
	  if( (m & JoystickThread.RIGHT_MASK) != 0 ) {
	    rv |= 0x04;
	  }
	  if( (m & JoystickThread.DOWN_MASK) != 0 ) {
	    rv |= 0x02;
	  }
	  if( (m & JoystickThread.UP_MASK) != 0 ) {
	    rv |= 0x01;
	  }
	  if( (this.joy0ActionMask & JoystickThread.BUTTON1_MASK) != 0 ) {
	    rv |= 0x20;
	  }
	  if( (this.joy0ActionMask & JoystickThread.BUTTON2_MASK) != 0 ) {
	    rv |= 0x10;
	  }
	  if( (this.joy1ActionMask & JoystickThread.BUTTON1_MASK) != 0 ) {
	    rv |= 0x80;
	  }
	  if( (this.joy1ActionMask & JoystickThread.BUTTON2_MASK) != 0 ) {
	    rv |= 0x40;
	  }
	  rv = ~rv & 0xFF;
	} else {
	  synchronized( this.keyboardMatrix ) {
	    int col = this.keyboardCol;
	    if( (col >= 0) && (col < this.keyboardMatrix.length) ) {
	      if( this.shiftTStateCounter > 0 ) {
		// nur Shift- und Control-Taste lesen
		if( col == 6 ) {
		  rv = ~(this.keyboardMatrix[ col ] & 0x05) & 0xFF;
		}
	      } else {
		rv = ~this.keyboardMatrix[ col ] & 0xFF;
	      }
	    }
	  }
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

      case 0xFC:
      case 0xFD:
      case 0xFE:
      case 0xFF:
	if( this.vdip != null ) {
	  rv = this.vdip.read( port );
	}
	break;
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc80.reset( true );
      this.pio90.reset( true );
    } else {
      this.ctc80.reset( false );
      this.pio90.reset( false );
    }
    this.vis.reset( resetLevel );
    if( this.fdc != null ) {
      this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    }
    if( this.floppyDiskDrives != null ) {
      for( int i = 0; i < this.floppyDiskDrives.length; i++ ) {
	FloppyDiskDrive drive = this.floppyDiskDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    this.fdcReset           = false;
    this.joyEnabled         = false;
    this.joy1Selected       = false;
    this.joy0ActionMask     = 0;
    this.joy1ActionMask     = 0;
    this.memConfig          = 0;
    this.psgRegNum          = 0;
    this.v24BitOut          = true;	// V24: H-Pegel
    this.v24BitNum          = 0;
    this.v24ShiftBuf        = 0;
    this.v24TStateCounter   = 0;
    this.shiftTStateCounter = 0;
    setCapsLockLED( false );
    setTapeLED( false );
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x8001 );
    if( endAddr >= 0x8001 ) {
      (new SaveDlg(
		this.screenFrm,
		0x8001,
		endAddr,
		-1,
		false,		// kein KC-BASIC
		true,		// RBASIC
		"RBASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
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
    if( joyNum == 0 ) {
      this.joy0ActionMask = actionMask;
    } else if( joyNum == 1 ) {
      this.joy1ActionMask = actionMask;
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( ((addr >= 0) && (addr < 0x4000)
				&& ((this.memConfig & 0x03) == 0x02))
	|| ((addr >= 0x4000) && (addr < 0x8000)
				&& ((this.memConfig & 0x0C) == 0x08))
	|| ((addr >= 0x8000) && (addr < 0xC000)
				&& ((this.memConfig & 0x30) == 0x20))
	|| ((addr >= 0xC000) && ((this.memConfig & 0xC0) == 0x80)) )
    {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	if( this.pasteFast ) {
	  cancelPastingText();
	  CharacterIterator iter = new StringCharacterIterator( text );
	  char              ch   = iter.first();
	  if( ch != CharacterIterator.DONE ) {
	    if( ch == '\n' ) {
	      ch = '\r';
	    }
	    /*
	     * Da sich die Programmausfuehrung i.d.R. bereits
	     * in der betreffenden Systemfunktion befindet,
	     * muss das erste Zeichen direkt an der Tastatur
	     * angelegt werden,
	     * damit der Systemaufruf beendet wird und somit
	     * der naechste Aufruf dann abgefangen werden kann.
	     */
	    keyTyped( ch );
	    this.pasteIter = iter;
	    done           = false;
	  }
	} else {
	  super.startPastingText( text );
	  done = false;
	}
      }
    }
    if( !done ) {
      this.screenFrm.firePastingTextFinished();
    }
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
    if( (begAddr == 0x8001) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.RBASIC ) ) {
	int endAddr = SourceUtil.getKCBasicStyleEndAddr(
						this.emuThread,
						0x8001 );
	if( endAddr > 0x8001 ) {
	  endAddr++;
	  this.emuThread.setMemByte( 0x8000, 0 );
	  this.emuThread.setMemWord( 0xF588, endAddr );
	  this.emuThread.setMemWord( 0xF58A, endAddr );
	  this.emuThread.setMemWord( 0xF58C, endAddr );
	  this.emuThread.setMemWord( 0xF58E, 0x8000 );
	  this.emuThread.setMemWord( 0xF67B, 0 );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0x20:
      case 0x21:
	if( this.ramFloppy1 != null ) {
	  this.ramFloppy1.writeByte( port, value );
	}
	break;

      case 0x24:
      case 0x25:
	if( this.ramFloppy2 != null ) {
	  this.ramFloppy2.writeByte( port, value );
	}
	break;

      case 0x40:
      case 0x41:
      case 0x42:
      case 0x43:
      case 0x44:
      case 0x45:
      case 0x46:
      case 0x47:
	if( this.fdc != null ) {
	  this.fdc.write( value );
	}
	break;

      case 0x48:
      case 0x49:
      case 0x4A:
      case 0x4B:
      case 0x4C:
      case 0x4D:
      case 0x4E:
      case 0x4F:
	if( this.fdc != null ) {
	  if( (value & 0x10) != 0 ) {
	    this.fdc.fireTC();
	  }
	  boolean b = ((value & 0x20) != 0);
	  if( b & !this.fdcReset ) {
	    this.fdc.reset( false );
	  }
	  this.fdcReset = b;
	}
	break;

      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	this.ctc80.write( port & 0x03, value );
	break;

      case 0x90:
	this.pio90.writePortA( value );
	break;

      case 0x91:
	synchronized( this ) {
	  this.pio90.writePortB( value );
	  int v = this.pio90.fetchOutValuePortB( false );
	  /*
	   * Bit 1: V24 TxD,
	   * Wenn bei fallender Flanke gerade keine Ausgabe laeuft,
	   * dann beginnt jetzt eine.
	   */
	  boolean state = ((v & 0x02) != 0);
	  if( !state && this.v24BitOut && (this.v24BitNum == 0) ) {
	    this.v24ShiftBuf      = 0;
	    this.v24TStateCounter = 3 * V24_TSTATES_PER_BIT / 2;
	    this.v24BitNum++;
	  }
	  this.v24BitOut = state;
	  // Bit 5 und 6: Joystick
	  this.joyEnabled   = ((v & 0x40) == 0);
	  this.joy1Selected = ((v & 0x20) == 0);
	}
	break;

      case 0x92:
	this.pio90.writeControlA( value );
	break;

      case 0x93:
	this.pio90.writeControlB( value );
	break;

      case 0x98:
	this.gdc.writeArg( value );
	break;

      case 0x99:
	this.gdc.writeCmd( value );
	break;

      case 0x9C:
	this.vis.writeFontByte( value );
	break;

      case 0x9D:
	this.vis.writeMode( value );
	break;

      case 0x9E:
	this.vis.writeFontAddr( value );
	break;

      case 0xA0:				// Auswahl Sound-Register
	this.psgRegNum = value & 0x0F;
	break;

      case 0xA1:				// Sound-Register schreiben
	if( this.psgRegNum <= 13 ) {
	  if( this.psgRegNum == 7 ) {
	    this.psg.setRegister( this.psgRegNum, ~value );
	  } else {
	    this.psg.setRegister( this.psgRegNum, value );
	  }
	}
	break;

      case 0xA8:				// SVG Port A
	this.memConfig = value;
	break;

      case 0xAA:				// SVG Port C
	this.keyboardCol = value & 0x0F;
	break;

      case 0xAB:
	switch( value & 0x0E ) {
	  case 0x08:
	    setTapeLED( (value & 0x01) == 0 );
	    break;
	  case 0x0A:
	    if( this.psgAudioOut == null ) {
	      this.emuThread.writeAudioPhase( (value & 0x01) != 0 );
	    }
	    break;
	  case 0x0C:
	    setCapsLockLED( (value & 0x01) == 0 );
	    break;
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

      case 0xFC:
      case 0xFD:
      case 0xFE:
      case 0xFF:
	if( this.vdip != null ) {
	  this.vdip.write( port, value );
	}
	break;
    }
  }


	/* --- private Methoden --- */

  private synchronized void checkAddPCListener( Properties props )
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    if( cpu != null ) {
      boolean pasteFast = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.a5105.paste.fast",
				true );
      if( pasteFast != this.pasteFast ) {
	this.pasteFast = pasteFast;
	if( pasteFast ) {
	  cpu.addPCListener( this, BIOS_ADDR_CONIN );
	} else {
	  cpu.removePCListener( this );
	}
      }
    }
  }


  private void createColors( Properties props )
  {
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      this.vis.createColors( brightness );
    }
  }


  private static boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.a5105.floppydisk.enabled",
			true );
  }


  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.a5105.kcnet.enabled",
				false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.a5105.vdip.enabled",
				false );
  }


  private static boolean isFixedScreenSize( Properties props )
  {
    return EmuUtil.parseBooleanProperty(
			props,
			"jkcemu.a5105.fixed_screen_size",
			false );
  }


  private void setCapsLockLED( boolean state )
  {
    if( state != this.capsLockLED ) {
      this.capsLockLED = state;
      if( this.keyboardFld != null ) {
	this.keyboardFld.repaint();
      }
    }
  }


  private boolean setCharInKBMatrix( int[][] kbMatrix, char ch )
  {
    boolean rv = false;
    synchronized( this.keyboardMatrix ) {
      int n = Math.min( this.keyboardMatrix.length, kbMatrix.length );
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
    }
    return rv;
  }


  private void setTapeLED( boolean state )
  {
    if( state != this.tapeLED ) {
      this.tapeLED = state;
      if( this.keyboardFld != null ) {
	this.keyboardFld.repaint();
      }
    }
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}

