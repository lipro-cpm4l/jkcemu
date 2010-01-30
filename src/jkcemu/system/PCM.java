/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des PC/M (Mugler/Mathes-PC)
 */

package jkcemu.system;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import jkcemu.disk.*;
import z80emu.*;


public class PCM extends EmuSys implements
					FDC8272.DriveSelector,
					Z80CTCListener,
					Z80SIOChannelListener
{
  private static byte[] bdos         = null;
  private static byte[] rom1RF       = null;
  private static byte[] pcmFontBytes = null;

  private byte[]            ramVideo;
  private Z80CTC            ctc;
  private Z80PIO            pio;
  private Z80SIO            sio;
  private FDC8272           fdc;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private boolean           fdcTC;
  private boolean           audioOutPhase;
  private boolean           keyboardUsed;
  private boolean           romEnabled;
  private boolean           upperBank0Enabled;
  private int               ramBank;
  private int               nmiCounter;


  public PCM( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );

    if( rom1RF == null ) {
      rom1RF = readResource( "/rom/pcm/pcm_1rf.bin" );
    }
    if( pcmFontBytes == null ) {
      pcmFontBytes = readResource( "/rom/pcm/pcmfont.bin" );
    }
    this.ramVideo      = new byte[ 0x0800 ];
    this.audioOutPhase = false;
    this.fdcTC         = false;
    this.curFDDrive    = null;
    this.fdc           = new FDC8272( this, 4 );
    this.fdDrives      = new FloppyDiskDrive[ 4 ];
    Arrays.fill( this.fdDrives, null );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    this.sio   = new Z80SIO();
    cpu.setInterruptSources( this.ctc, this.pio, this.sio );
    if( this.fdc != null ) {
      cpu.addMaxSpeedListener( this.fdc );
      cpu.addTStatesListener( this.fdc );
    }
    this.ctc.addCTCListener( this );
    this.sio.addChannelListener( this, 0 );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- FDC8272.DriveSelector --- */

  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    return this.curFDDrive;
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( (ctc == this.ctc) && (timerNum == 2) ) {
      if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
	this.audioOutPhase = !this.audioOutPhase;
	this.emuThread.writeAudioPhase( this.audioOutPhase );
      }
    }
  }


	/* --- Z80SIOChannelListener --- */

  public void z80SIOChannelByteAvailable( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio) && (channel == 0) )
      this.emuThread.getPrintMngr().putByte( value );
  }


	/* --- ueberschriebene Methoden --- */

  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.sio.removeChannelListener( this, 0 );
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.fdc != null ) {
      cpu.removeTStatesListener( this.fdc );
      cpu.removeMaxSpeedListener( this.fdc );
    }
  }


  public int getAppStartStackInitValue()
  {
    return 0xC800;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = BLACK;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = pcmFontBytes;
    }
    if( fontBytes != null ) {
      int rPix = y % 16;
      int row  = y / 16;
      int col  = x / 8;
      int offs = 0x0400;
      if( rPix >= 8 ) {
	offs = 0;		// Zwischenzeile
	rPix -= 8;
      }
      int mIdx = offs + (row * 64) + col;
      if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	int fIdx = (((int) this.ramVideo[ mIdx ] & 0xFF) * 8) + rPix;
	if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  if( (fontBytes[ fIdx ] & m) != 0 ) {
	    rv = WHITE;
	  }
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
    return "/help/pcm.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr < 0x2000) && this.romEnabled ) {
      if( rom1RF != null ) {
	if( addr < rom1RF.length ) {
	  rv = (int) rom1RF[ addr ] & 0xFF;
	}
      }
    } else if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	rv = (int) this.ramVideo[ idx ] & 0xFF;
      }
    } else {
      if( (this.ramBank == 0)
	  || ((addr >= 0xC00) && this.upperBank0Enabled) )
      {
	rv = this.emuThread.getRAMByte( addr );
      }
      else if( this.ramBank == 1 ) {
	rv = this.emuThread.getRAMFloppy1().getByte( addr );
      }
      else if( this.ramBank == 2 ) {
	rv = this.emuThread.getRAMFloppy1().getByte( addr + 0xF800 );
      }
    }
    return rv;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
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
    return this.fdDrives != null ? this.fdDrives.length : 0;
  }


  public int getSupportedRAMFloppyCount()
  {
    return 1;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return "PC/M (Mugler/Mathes-PC)";
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    return ((addr < 0x2000) && this.romEnabled)
		|| ((addr >= 0x2000) && (addr < 0xC000) && (this.ramBank == 0))
		|| ((addr >= 0xC000) && (addr < 0xF800)
			&& ((this.ramBank == 0) || this.upperBank0Enabled))
		|| (addr >= 0xF800);
  }


  public boolean isISO646DE()
  {
    return false;
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


  public void keyReleased()
  {
    this.pio.putInValuePortA( 0, 0xFF );
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public boolean pasteChar( char ch )
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
      try {
	Thread.sleep( 100 );
      }
      catch( InterruptedException ex ) {}
      this.pio.putInValuePortA( 0, 0xFF );
      rv = true;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	rv = this.ctc.read( port & 0x03 );
	break;

      case 0x84:
	if( !this.keyboardUsed ) {
	  this.pio.putInValuePortA( 0, 0xFF );
	  this.keyboardUsed = true;
	}
	rv = this.pio.readPortA();
	break;

      case 0x85:
	this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x80 : 0, 0x80 );
	rv = this.pio.readPortB();
	break;

      case 0x86:
	rv = this.pio.readControlA();
	break;

      case 0x87:
	rv = this.pio.readControlB();
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
    }
    return rv;
  }


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


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System
   * oder der ROM-Inhalt aendert
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "PCM" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramVideo );
    }
    if( EmuUtil.getBooleanProperty(
			props,
			"jkcemu.pcm.auto_load_bdos",
			true ) )
    {
      if( bdos == null ) {
	bdos = readResource( "/rom/pcm/bdos.bin" );
    }
    if( bdos != null ) {
	int addr = 0xD000;
	for( int i = 0; (addr < 0x10000) && (i < bdos.length); i++ ) {
	  this.emuThread.setRAMByte( addr++, bdos[ i ] );
	}
      }
    }
    if( this.fdc != null ) {
      this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
    }
    if( this.fdDrives != null ) {
      for( int i = 0; i < this.fdDrives.length; i++ ) {
	FloppyDiskDrive drive = this.fdDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    this.fdcTC             = false;
    this.keyboardUsed      = false;
    this.romEnabled        = true;
    this.upperBank0Enabled = true;
    this.ramBank           = 0;
    this.nmiCounter        = 0;
  }


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.fdDrives != null ) {
      if( (idx >= 0) && (idx < this.fdDrives.length) ) {
	this.fdDrives[ idx ] = drive;
      }
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    } else {
      if( (addr >= 0x2000) || !this.romEnabled ) {
	if( (this.ramBank == 0)
	    || ((addr >= 0xC00) && this.upperBank0Enabled) )
	{
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
	else if( this.ramBank == 1 ) {
	  this.emuThread.getRAMFloppy1().setByte( addr, value );
	  rv = true;
	}
	else if( this.ramBank == 2 ) {
	  this.emuThread.getRAMFloppy1().setByte( addr + 0xF800, value );
	  rv = true;
	}
      }
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


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	this.ctc.write( port & 0x03, value );
	break;

      case 0x84:
	this.pio.writePortA( value );
	break;

      case 0x85:
	this.pio.writePortB( value );
	if( !this.emuThread.isLoudspeakerEmulationEnabled() ) {
	  this.audioOutPhase = ((this.pio.fetchOutValuePortB( false ) & 0x40)
									!= 0);
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
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
	  boolean         tc   = ((value & 0x80) != 0);
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
	  if( tc && (tc != this.fdcTC) ) {
	    this.fdc.fireTC();
	  }
	  this.fdcTC    = tc;
	}
	break;
    }
  }
}

