/*
 * (c) 2008-2021 Jens Mueller
 * (c) 2014-2022 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Emulation des PC/M (Mugler/Mathes-PC)
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.audio.AbstractSoundDevice;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.RAMFloppy;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.etc.CPUSynchronSoundDevice;
import jkcemu.etc.K1520Sound;
import jkcemu.text.TextUtil;
import jkcemu.usb.VDIP;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;
import z80emu.Z80SIO;
import z80emu.Z80SIOChannelListener;


public class PCM extends EmuSys implements
					FDC8272.DriveSelector,
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80PIOPortListener,
					Z80SIOChannelListener
{
  public static final String SYSNAME     = "PCM";
  public static final String SYSTEXT     = "PC/M";
  public static final String PROP_PREFIX = "jkcemu.pcm.";

  public static final String PROP_BDOS_PREFIX    = "bdos.";
  public static final String PROP_AUTOLOAD       = "autoload";
  public static final String PROP_GRAPHIC        = "graphic";
  public static final String VALUE_GRAPHIC_80X24 = "80x24";
  public static final String VALUE_GRAPHIC_64X32 = "64x32";

  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE = true;

  private static FloppyDiskInfo disk64x16v330 = new FloppyDiskInfo(
			"/disks/pcm/pcmsys330_64x16.dump.gz",
			"PC/M v3.30 Boot-Diskette (64x16 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo disk80x24v330 = new FloppyDiskInfo(
			"/disks/pcm/pcmsys330_80x24.dump.gz",
			"PC/M v3.30 Boot-Diskette (80x24 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo disk64x16v331 = new FloppyDiskInfo(
			"/disks/pcm/pcmsys331_64x16.dump.gz",
			"PC/M v3.31 Boot-Diskette (64x16 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo disk80x24v331 = new FloppyDiskInfo(
			"/disks/pcm/pcmsys331_80x24.dump.gz",
			"PC/M v3.31 Boot-Diskette (80x24 Zeichen)",
			2, 2048, true );

  private static FloppyDiskInfo disk80x24ssrc = new FloppyDiskInfo(
			"/disks/pcm/pcmsys_src.dump.gz",
			"PC/M Systemquellen (Boot-Diskette f\u00FCr 80x24)",
			2, 2048, true );

  private static final FloppyDiskInfo[] availableFloppyDisks = {
							disk64x16v330,
							disk80x24v330,
							disk64x16v331,
							disk80x24v331,
							disk80x24ssrc };

  private static byte[] bdos              = null;
  private static byte[] romRF64x16        = null;
  private static byte[] romFDC64x16       = null;
  private static byte[] romFDC80x24       = null;
  private static byte[] pcmFontBytes64x16 = null;
  private static byte[] pcmFontBytes80x24 = null;

  private byte[]                 bdosBytes;
  private String                 bdosFile;
  private byte[]                 fontBytes;
  private byte[]                 romBytes;
  private String                 romFile;
  private byte[]                 ramVideo;
  private RAMFloppy              ramFloppy;
  private CPUSynchronSoundDevice loudspeaker;
  private K1520Sound             k1520Sound;
  private VDIP                   vdip;
  private Z80CTC                 ctc;
  private Z80PIO                 pio;
  private Z80SIO                 sio;
  private FDC8272                fdc;
  private FloppyDiskDrive        curFDDrive;
  private FloppyDiskDrive[]      fdDrives;
  private boolean                fdcTC;
  private boolean                keyboardUsed;
  private boolean                mode80x24;
  private boolean                soundPhase;
  private boolean                romEnabled;
  private boolean                upperBank0Enabled;
  private int                    ramBank;
  private int                    romSize;
  private int                    nmiCounter;


  public PCM( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.bdosBytes  = null;
    this.bdosFile   = null;
    this.romSize    = 0x2000;
    this.fontBytes  = null;
    this.romBytes   = null;
    this.romFile    = null;
    this.ramVideo   = new byte[ 0x0800 ];
    this.fdcTC      = false;
    this.mode80x24  = emulates80x24( props );
    this.curFDDrive = null;
    this.fdc        = null;
    this.fdDrives   = null;
    if( emulatesFloppyDisk( props ) ) {
      this.fdc      = new FDC8272( this, 4 );
      this.fdDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.fdDrives, null );
    } else {
      this.fdc      = null;
      this.fdDrives = null;
    }
    this.loudspeaker = new CPUSynchronSoundDevice( "Lautsprecher" );

    if( emulatesK1520Sound( props ) ) {
      this.k1520Sound = new K1520Sound( this, 0x38 );
    } else {
      this.k1520Sound = null;
    }

    if( emulatesVDIP( props ) ) {
      this.vdip = new VDIP(
			0,
			this.emuThread.getZ80CPU(),
			"USB-PIO (E/A-Adressen DCh-DFh und FCh-FFh)" );
    } else {
      this.vdip = null;
    }

    this.ramFloppy = this.emuThread.getRAMFloppy1();
    if( this.ramFloppy != null ) {
      this.ramFloppy.install(
		"PC/M",
		RAMFloppy.RFType.OTHER,
		124 * 1024,
		"PC/M RAM-B\u00E4nke 1 und 2",
		EmuUtil.getProperty(
			props,
			this.propPrefix
				+ PCM.PROP_RF_PREFIX
				+ RAMFloppy.PROP_FILE ) );
    }

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC (E/A-Adressen 80h-83h)" );
    this.pio   = new Z80PIO( "PIO (E/A-Adressen 84h-87h)" );
    this.sio   = new Z80SIO( "SIO (E/A-Adressen 88h-8Bh)" );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    this.ctc.addCTCListener( this );
    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.B );
    this.sio.addChannelListener( this, 0 );

    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
    iSources.add( this.ctc );
    iSources.add( this.pio );
    iSources.add( this.sio );
    if( this.k1520Sound != null ) {
      iSources.add( this.k1520Sound );
    }
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    }
    try {
      cpu.setInterruptSources(
        iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}

    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    z80MaxSpeedChanged( cpu );
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
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
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  this.sio.clockPulseSenderA();
	  this.sio.clockPulseReceiverA();
	  break;
	case 1:
	  this.sio.clockPulseSenderB();
	  this.sio.clockPulseReceiverB();
	  break;
	case 2:
	  this.soundPhase = !this.soundPhase;
	  this.loudspeaker.setCurPhase( this.soundPhase );
	  break;
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.loudspeaker.z80MaxSpeedChanged( cpu );
    if( this.fdc != null ) {
      this.fdc.z80MaxSpeedChanged( cpu );
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80MaxSpeedChanged( cpu );
    }
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio)
	&& (port == Z80PIO.PortInfo.B)
	&& ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
	    || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
    {
      this.tapeOutPhase = ((this.pio.fetchOutValuePortB( 0xFF ) & 0x40) != 0);
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOByteSent( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio) && (channel == 0) ) {
      this.emuThread.getPrintMngr().putByte( value );
      this.sio.setClearToSendA( false );
      this.sio.setClearToSendA( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>PC/M Speicherkonfiguration</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>F800h-FFFFh:</td><td>BWS</td></tr>\n"
	+ "<tr><td>C000h-F7FFh:</td><td>RAM Bank " );
    if( this.upperBank0Enabled ) {
      buf.append( "0" );
    } else {
      buf.append( this.ramBank );
    }
    int ramBegAddr = (this.romEnabled ? this.romSize : 0x0000);
    buf.append(
	String.format(
		"</td></tr>\n"
    			+ "<tr><td>%04Xh-BFFFh:</td>"
			+ "<td>RAM Bank %d</td></tr>\n",
		ramBegAddr,
		this.ramBank ) );
    if( ramBegAddr > 0 ) {
      buf.append(
	String.format(
		"<tr><td>0000h-%04Xh:</td><td>ROM</td></tr>\n",
		ramBegAddr - 1) );
    }
    buf.append( "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    loadFont( props );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = TextUtil.equals(
		this.bdosFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_BDOS_PREFIX + PROP_FILE ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.romFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_PREFIX + PROP_FILE ) );
    }
    if( rv && emulatesFloppyDisk( props ) != (this.fdc != null) ) {
      rv = false;
    }
    if( rv && emulates80x24( props ) != this.mode80x24 ) {
      rv = false;
    }
    if( rv && (emulatesK1520Sound( props ) != (this.k1520Sound != null)) ) {
      rv = false;
    }
    if( rv && (emulatesVDIP( props ) != (this.vdip != null)) ) {
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
    if( this.ramFloppy != null ) {
      this.ramFloppy.deinstall();
    }
    this.sio.removeChannelListener( this, 0 );
    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.B );
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    cpu.removeMaxSpeedListener( this );
    cpu.removeTStatesListener( this );
    if( this.fdc != null ) {
      this.fdc.die();
    }
    this.loudspeaker.fireStop();
    if( this.k1520Sound != null ) {
      this.k1520Sound.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
    super.die();
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0xC800;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.fontBytes != null ) {
      if( this.mode80x24 ) {
	int rPix = y % 10;
	int row  = y / 10;
	int col  = x / 6;
	int mIdx = (row * 80) + col;
	if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	  int ch = (int) this.ramVideo[ mIdx ] & 0xFF;
	  if( (rPix == 8) && ((ch & 0x80) != 0) ) {
	    rv = WHITE;					// Cursor
	  } else {
	    int fIdx = ((ch & 0x7F) * 16) + rPix;
	    if( (fIdx >= 0) && (fIdx < this.fontBytes.length ) ) {
	      int m = 0x80;
	      int n = x % 6;
	      if( n > 0 ) {
		m >>= n;
	      }
	      if( (this.fontBytes[ fIdx ] & m) == 0 ) {
		rv = WHITE;
	      }
	    }
	  }
	}
      } else {
	int rPix = y % 16;
	int row  = y / 16;
	int col  = x / 7;
	int offs = 0x0400;
	if( rPix >= 8 ) {
	  offs = 0;		// Zwischenzeile
	  rPix -= 8;
	}
	int mIdx = offs + (row * 64) + col;
	if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	  int fIdx = (((int) this.ramVideo[ mIdx ] & 0xFF) * 8) + rPix;
	  if( (fIdx >= 0) && (fIdx < this.fontBytes.length ) ) {
	    int m = 0x80;
	    int n = x % 7;
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
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return this.mode80x24 ?
		new CharRaster( 80, 24, 10, 6, 10 )
		: new CharRaster( 64, 32, 8, 7, 8 );
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_624K;
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
    return "/help/pcm.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr < this.romSize) && this.romEnabled ) {
      if( this.romBytes != null ) {
	if( addr < this.romBytes.length ) {
	  rv = (int) this.romBytes[ addr ] & 0xFF;
	}
      }
    } else if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	rv = (int) this.ramVideo[ idx ] & 0xFF;
      }
    } else {
      if( (this.ramBank == 0)
	  || ((addr >= 0xC000) && this.upperBank0Enabled) )
      {
	rv = this.emuThread.getRAMByte( addr );
      }
      else if( (this.ramBank == 1) && (this.ramFloppy != null) ) {
	rv = this.ramFloppy.getByte( addr );
      }
      else if( (this.ramBank == 2) && (this.ramFloppy != null) ) {
	rv = this.ramFloppy.getByte( addr + 0xF800 );
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch = -1;
    if( this.mode80x24 ) {
      int idx = (chY * 80) + chX;
      if( (idx >= 0) && (idx < this.ramVideo.length) ) {
	int b = (int) this.ramVideo[ idx ];
	switch( b ) {
	  case 0x01:
	    ch = '\u00C4';		// Ae
	    break;
	  case 0x02:
	    ch = '\u00D6';		// Oe
	    break;
	  case 0x03:
	    ch = '\u00DC';		// Ue
	    break;
	  case 0x04:
	    ch = '\u00E4';		// ae
	    break;
	  case 0x05:
	    ch = '\u00F6';		// oe
	    break;
	  case 0x06:
	    ch = '\u00FC';		// ue
	    break;
	  case 0x07:
	    ch = '\u00DF';		// sz 
	    break;
	  default:
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      ch = b;
	    }
	}
      }
    } else {
      int idx = -1;
      if( (chY & 0x01) == 0 ) {
	chY /= 2;
	idx = 0x0400 + (chY * 64) + chX;
      } else {
	chY /= 2;
	idx = (chY * 64) + chX;
      }
      if( (idx >= 0) && (idx < this.ramVideo.length) ) {
	int b = (int) this.ramVideo[ idx ];
	switch( b ) {
	  case 0x18:
	    ch = '\u00C4';		// Ae
	    break;
	  case 0x19:
	    ch = '\u00E4';		// ae
	    break;
	  case 0x1A:
	    ch = '\u00D6';		// Oe
	    break;
	  case 0x1B:
	    ch = '\u00F6';		// oe
	    break;
	  case 0x1C:
	    ch = '\u00DC';		// Ue
	    break;
	  case 0x1D:
	    ch = '\u00FC';		// ue
	    break;
	  case 0x1E:
	    ch = '\u00DF';		// sz 
	    break;
	  default:
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      ch = b;
	    }
	}
	if( (b >= 0x20) && (b < 0x7F) ) {
	  ch = b;
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return this.mode80x24 ? 240 : 256;
  }


  @Override
  public int getScreenWidth()
  {
    return this.mode80x24 ? 480 : 448;
  }


  @Override
  public AbstractSoundDevice[] getSoundDevices()
  {
    return this.k1520Sound != null ?
		new AbstractSoundDevice[] {
			this.loudspeaker,
			this.k1520Sound.getSoundDevice() }
		: new AbstractSoundDevice[] { this.loudspeaker };
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    FloppyDiskInfo[] rv = null;
    if( this.fdc != null ) {
      if( this.mode80x24 ) {
	rv = new FloppyDiskInfo[] { disk80x24v330 };
      } else {
	rv = new FloppyDiskInfo[] { disk64x16v330 };
      }
    }
    return rv;
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
    return SYSTEXT;
  }


  @Override
  public VDIP[] getVDIPs()
  {
    return this.vdip != null ?
			new VDIP[] { this.vdip }
			: super.getVDIPs();
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
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    this.pio.putInValuePortA( 0, 0xFF );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  @Override
  public void loadROMs( Properties props )
  {
    this.romFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_PREFIX + PROP_FILE );
    this.romBytes = readROMFile(
			this.romFile,
			0x8000,
			"ROM-Inhalt (Grundbetriebssystem)" );
    if( this.romBytes != null ) {
      // ROM-Groesse ermitteln
      int n8k = (this.romBytes.length + 0x1FFF) / 0x2000;
      if( n8k < 1 ) {
	n8k = 1;
      } else if( n8k > 4 ) {
	n8k = 4;
      }
      this.romSize = n8k * 0x2000;
    } else {
      if( this.fdc != null ) {
	if( this.mode80x24 ) {
	  if( romFDC80x24 == null ) {
	    romFDC80x24 = readResource( "/rom/pcm/pcmsys330_80x24.bin" );
	  }
	  this.romBytes = romFDC80x24;
	} else {
	  if( romFDC64x16 == null ) {
	    romFDC64x16 = readResource( "/rom/pcm/pcmsys330_64x16.bin" );
	  }
	  this.romBytes = romFDC64x16;
	}
      } else {
	if( romRF64x16 == null ) {
	  romRF64x16 = readResource( "/rom/pcm/pcmsys211_64x16.bin" );
	}
	this.romBytes = romRF64x16;
      }
    }
    loadFont( props );
  }


  @Override
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    if( ch == '\n' ) {
      ch = '\r';
    }
    if( (ch > 0) && (ch < 0x7F) ) {
      if( Character.isUpperCase( ch ) ) {
	ch = Character.toLowerCase( ch );
      } else if( Character.isLowerCase( ch ) ) {
	ch = Character.toUpperCase( ch );
      }
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      Thread.sleep( 100 );
      this.pio.putInValuePortA( 0, 0xFF );
      rv = true;
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	rv = this.ctc.read( port & 0x03, tStates );
	break;

      case 0x84:
	if( !this.keyboardUsed ) {
	  this.pio.putInValuePortA( 0, 0xFF );
	  this.keyboardUsed = true;
	}
	rv = this.pio.readDataA();
	break;

      case 0x85:
	rv = this.pio.readDataB();
	break;

      case 0x88:
	rv = this.sio.readDataA();
	break;

      case 0x89:
	rv = this.sio.readDataB();
	break;

      case 0x8A:
	rv = this.sio.readControlA();
	break;

      case 0x8B:
	rv = this.sio.readControlB();
	break;

      case 0x98:
      case 0x99:
      case 0x9A:
      case 0x9B:
	this.nmiCounter = 3;
	break;

      case 0xC0:
	if( this.fdc != null ) {
	  rv = this.fdc.readMainStatusReg();
	}
	break;

      case 0xC1:
	if( this.fdc != null ) {
	  rv = this.fdc.readData();
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

      default:
	if( (this.k1520Sound != null) && ((port & 0xF8) == 0x38) ) {
	  rv = this.k1520Sound.read( port, tStates );
	}
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    if( m1 && (this.nmiCounter > 0) ) {
      --this.nmiCounter;
      if( this.nmiCounter == 0 ) {
	this.emuThread.getZ80CPU().fireNMI();
      }
    }
    return getMemByte( addr, m1 );
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    if( powerOn ) {
      initDRAM();
      fillRandom( this.ramVideo );
    }
    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_BDOS_PREFIX + PROP_AUTOLOAD,
			true ) )
    {
      loadBDOS( props );
    }
    if( this.fdc != null ) {
      this.fdc.reset( powerOn );
    }
    this.ctc.reset( powerOn );
    this.pio.reset( powerOn );
    this.sio.reset( powerOn );
    this.sio.setClearToSendA( true );
    this.sio.setClearToSendB( true );
    if( this.fdDrives != null ) {
      for( int i = 0; i < this.fdDrives.length; i++ ) {
	FloppyDiskDrive drive = this.fdDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    this.loudspeaker.reset();
    if( this.k1520Sound != null ) {
      this.k1520Sound.reset( powerOn );
    }
    if( this.vdip != null ) {
      this.vdip.reset( powerOn );
    }
    this.curFDDrive        = null;
    this.fdcTC             = false;
    this.keyboardUsed      = false;
    this.soundPhase        = false;
    this.romEnabled        = true;
    this.upperBank0Enabled = true;
    this.ramBank           = 0;
    this.nmiCounter        = 0;
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
    if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    } else {
      if( (addr >= this.romSize) || !this.romEnabled ) {
	if( (this.ramBank == 0)
	    || ((addr >= 0xC000) && this.upperBank0Enabled) )
	{
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
	else if( (this.ramBank == 1) && (this.ramFloppy != null) ) {
	  this.ramFloppy.setByte( addr, value );
	  rv = true;
	}
	else if( (this.ramBank == 2) && (this.ramFloppy != null) ) {
	  this.ramFloppy.setByte( addr + 0xF800, value );
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return (this.fontBytes != pcmFontBytes64x16)
		&& (this.fontBytes != pcmFontBytes80x24);
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
  public boolean supportsRAMFloppy1()
  {
    return this.ramFloppy != null;
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
    this.pio.putInValuePortB( this.tapeInPhase ? 0x80 : 0, 0x80 );
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	this.ctc.write( port & 0x03, value, tStates );
	break;

      case 0x84:
	this.pio.writeDataA( value );
	break;

      case 0x85:
	this.pio.writeDataB( value );
	break;

      case 0x86:
	this.pio.writeControlA( value );
	break;

      case 0x87:
	this.pio.writeControlB( value );
	break;

      case 0x88:
	this.sio.writeDataA( value );
	break;

      case 0x89:
	this.sio.writeDataB( value );
	break;

      case 0x8A:
	this.sio.writeControlA( value );
	break;

      case 0x8B:
	this.sio.writeControlB( value );
	break;

      case 0x94:
      case 0x95:
      case 0x96:
      case 0x97:
	this.ramBank           = value & 0x03;
	this.upperBank0Enabled = ((value & 0x40) != 0);
	this.romEnabled        = ((value & 0x80) == 0);
	break;

      case 0x98:
      case 0x99:
      case 0x9A:
      case 0x9B:
	this.nmiCounter = 3;
	break;

      case 0xC1:
	if( this.fdc != null ) {
	  this.fdc.write( value );
	}
	break;

      case 0xC2:
      case 0xC3:
	if( this.fdc != null ) {
	  if( this.fdDrives != null ) {
	    FloppyDiskDrive fdd  = null;
	    int v    = ((value & 0x0f) >> 1) | ((value & 0x01) << 3);
	    int mask = 0x01;
	    for( int i = 0; i < this.fdDrives.length; i++ ) {
	      if( (v & mask) != 0 ) {
		fdd = this.fdDrives[ i ];
		break;
	      }
	      mask <<= 1;
	    }
	    this.curFDDrive = fdd;
	  }
	  boolean tc = ((value & 0x80) != 0);
	  if( tc && (tc != this.fdcTC) ) {
	    this.fdc.fireTC();
	  }
	  this.fdcTC = tc;
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

      default:
	if( (this.k1520Sound != null) && ((port & 0xF8) == 0x38) ) {
	  this.k1520Sound.write( port, value, tStates );
	}
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.loudspeaker.z80TStatesProcessed( cpu, tStates );
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- private Methoden --- */

  private boolean emulates80x24( Properties props )
  {
    return EmuUtil.getProperty(
	props,
	this.propPrefix + PROP_GRAPHIC ).equals( VALUE_GRAPHIC_80X24 );
  }


  private void loadBDOS( Properties props )
  {
    this.bdosFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_BDOS_PREFIX + PROP_FILE );
    this.bdosBytes = readRAMFile(
			this.bdosFile,
			0x0E00,
			"RAM-Datei f\u00FCr BDOS" );
    if( this.bdosBytes == null ) {
      if( bdos == null ) {
	bdos = readResource( "/rom/pcm/bdos.bin" );
      }
      this.bdosBytes = bdos;
    }
    if( this.bdosBytes != null ) {
      int addr = 0xD000;
      for( int i = 0; (addr < 0x10000) &&
		      (i < this.bdosBytes.length); i++ ) {
	this.emuThread.setRAMByte( addr++, this.bdosBytes[ i ] );
      }
    }
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE,
				0x0800 );
    if( this.fontBytes == null ) {
      if( this.mode80x24 ) {
	if( pcmFontBytes80x24 == null ) {
	  pcmFontBytes80x24 = readResource( "/rom/pcm/pcmfont_80x24.bin" );
	}
	this.fontBytes = pcmFontBytes80x24;
      } else {
	if( pcmFontBytes64x16 == null ) {
	  pcmFontBytes64x16 = readResource( "/rom/pcm/pcmfont_64x16.bin" );
	}
	this.fontBytes = pcmFontBytes64x16;
      }
    }
  }
}
