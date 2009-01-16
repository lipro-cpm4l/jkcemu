/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.audio.*;
import jkcemu.ac1.AC1;
import jkcemu.bcs3.BCS3;
import jkcemu.kc85.KC85;
import jkcemu.z1013.Z1013;
import jkcemu.z9001.Z9001;
import z80emu.*;


public class EmuThread extends Thread implements
					Z80IOSystem,
					Z80Memory
{
  public enum ResetLevel { NO_RESET, WARM_RESET, COLD_RESET, POWER_ON };

  private ScreenFrm           screenFrm;
  private Z80CPU              z80cpu;
  private Object              monitor;
  private byte[]              ram;
  private RAMFloppy           ramFloppyA;
  private RAMFloppy           ramFloppyB;
  private volatile ExtFile    extFont;
  private volatile ExtROM[]   extROMs;
  private volatile AudioIn    audioIn;
  private volatile AudioOut   audioOut;
  private volatile LoadData   loadData;
  private volatile ResetLevel resetLevel;
  private volatile boolean    emuRunning;
  private volatile EmuSys     emuSys;
  private volatile EmuSys     nextEmuSys;
  private volatile ExtROM[]   nextExtROMs;


  public EmuThread( ScreenFrm screenFrm )
  {
    this.screenFrm   = screenFrm;
    this.z80cpu      = new Z80CPU( this, this );
    this.monitor     = "a monitor object for synchronization";
    this.ram         = new byte[ 0x10000 ];
    this.ramFloppyA  = new RAMFloppy();
    this.ramFloppyB  = new RAMFloppy();
    this.extFont     = null;
    this.extROMs     = null;
    this.audioIn     = null;
    this.audioOut    = null;
    this.loadData    = null;
    this.resetLevel  = ResetLevel.POWER_ON;
    this.emuRunning  = false;
    this.emuSys      = null;
    this.nextEmuSys  = null;
    this.nextExtROMs = null;
    applySettings( Main.getProperties() );
    fireReset( ResetLevel.POWER_ON );
  }


  public void applySettings( Properties props )
  {
    applySettings(
		props,
		EmuUtil.readExtFont( this.screenFrm, props ),
		EmuUtil.readExtROMs( this.screenFrm, props ),
		false );
  }


  public synchronized void applySettings(
				Properties props,
				ExtFile    extFont,
				ExtROM[]   extROMs,
				boolean    forceReset )
  {
    // zu emulierendes System ermitteln
    boolean reqReset = false;
    EmuSys  emuSys   = this.emuSys;
    if( emuSys != null ) {
      reqReset = emuSys.requiresReset( props );
    }
    if( (emuSys == null) || reqReset ) {
      if( emuSys != null ) {
	emuSys.die();
      }
      String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
      if( sysName.startsWith( "AC1" ) ) {
	emuSys = new AC1( this, props );
      } else if( sysName.startsWith( "BCS3" ) ) {
	emuSys = new BCS3( this, props );
      } else if( sysName.startsWith( "Z1013" ) ) {
	emuSys = new Z1013( this, props );
      } else if( sysName.startsWith( "KC85/2" )
		 || sysName.startsWith( "KC85/3" )
		 || sysName.startsWith( "KC85/4" ) )
      {
	emuSys = new KC85( this, props );
      } else {
	emuSys = new Z9001( this, props );
      }
    }
    if( reqReset && (this.emuSys != null) ) {
      synchronized( this.monitor ) {
	this.nextEmuSys  = emuSys;
	this.nextExtROMs = extROMs;
      }
    } else {
      this.emuSys  = emuSys;
      this.extROMs = extROMs;
    }
    this.extFont = extFont;
    if( reqReset || (this.emuSys == null) ) {
      fireReset( ResetLevel.COLD_RESET );
    }

    // CPU-Geschwindigkeit
    updCPUSpeed( props, emuSys.getSystemName() );
  }


  public static int getDefaultSpeedKHz(
				String     sysName,
				Properties props )
  {
    int rv = 1750;			// KC85/2..4
    if( sysName != null ) {
      if( sysName.startsWith( "KC85/1" )
	  || sysName.startsWith( "KC87" )
	  || sysName.startsWith( "Z9001" ) )
      {
	rv = 2458;			// eigentlich 2,4576 MHz
      }
      else if( sysName.startsWith( "AC1" )
	       || sysName.startsWith( "Z1013" ) )
      {
	rv = 2000;
      }
      else if( sysName.startsWith( "BCS3" ) ) {
	rv = BCS3.getDefaultSpeedKHz( props );
      }
    }
    return rv;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
  }


  public EmuSys getNextEmuSys()
  {
    return this.nextEmuSys;
  }


  public ExtFile getExtFont()
  {
    return this.extFont;
  }


  public byte[] getExtFontBytes()
  {
    ExtFile extFont = this.extFont;
    return extFont != null ? extFont.getBytes() : null;
  }


  public ExtROM[] getExtROMs()
  {
    return this.extROMs;
  }


  public int getRAMByte( int addr )
  {
    return (int) this.ram[ addr & 0xFFFF ] & 0xFF;
  }


  public RAMFloppy getRAMFloppyA()
  {
    return this.ramFloppyA;
  }


  public RAMFloppy getRAMFloppyB()
  {
    return this.ramFloppyB;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public Z80CPU getZ80CPU()
  {
    return this.z80cpu;
  }


  /*
   * Mit diese Methode wird ermittelt,
   * ob die Tonausgabe auf dem Kassettenrecorderanschluss
   * oder einen evtl. vorhandenen Lautsprecheranschluss
   * emuliert werden soll.
   */
  public boolean isLoudspeakerEmulationEnabled()
  {
    return audioOut != null ? audioOut.isLoudspeakerEmulationEnabled() : false;
  }


  public boolean keyPressed( KeyEvent e )
  {
    return this.emuSys.keyPressed( e );
  }


  public void keyReleased( int keyCode )
  {
    this.emuSys.keyReleased( keyCode );
  }


  public void keyTyped( char ch )
  {
    if( this.emuSys.getSwapKeyCharCase() ) {
      if( Character.isUpperCase( ch ) ) {
        ch = Character.toLowerCase( ch );
      } else if( Character.isLowerCase( ch ) ) {
        ch = Character.toUpperCase( ch );
      }
    }
    switch( ch ) {
      case '\u00A7':  // Paragraf-Zeichen
	ch = '@';
	break;

      case '\u00C4':  // Ae
	ch = '[';
	break;

      case '\u00D6':  // Oe
	ch = '\\';
	break;

      case '\u00DC':  // Ue
	ch = ']';
	break;

      case '\u00E4':  // ae
	ch = '{';
	break;

      case '\u00F6':  // oe
	ch = '|';
	break;

      case '\u00FC':  // ue
	ch = '}';
	break;

      case '\u00DF':  // sz
	ch = '~';
	break;
    }
    this.emuSys.keyTyped( ch );
  }


  public boolean readAudioPhase()
  {
    boolean phase   = false;
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      phase = audioIn.readPhase();
    }
    return phase;
  }


  public void setAudioIn( AudioIn audioIn )
  {
    this.audioIn = audioIn;
  }


  public void setAudioOut( AudioOut audioOut )
  {
    this.audioOut = audioOut;
  }


  public void setRAMByte( int addr, int value )
  {
    addr &= 0xFFFF;
    this.ram[ addr ] = (byte) value;
  }


  public void updCPUSpeed( Properties props, String sysName )
  {
    int    maxSpeedKHz  = getDefaultSpeedKHz( sysName, props );
    String maxSpeedText = EmuUtil.getProperty( props, "jkcemu.maxspeed.khz" );
    if( maxSpeedText.equals( "unlimited" ) ) {
      maxSpeedKHz = 0;
    } else {
      if( !maxSpeedText.equals( "default" ) ) {
	if( maxSpeedText.length() > 0 ) {
	  try {
	    int value = Integer.parseInt( maxSpeedText );
	    if( value > 0 )
	      maxSpeedKHz = value;
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    this.z80cpu.setMaxSpeedKHz( maxSpeedKHz );
  }


  public void writeAudioPhase( boolean phase )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null )
      audioOut.writePhase( phase );
  }


  public void writeAudioValue( byte value )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null )
      audioOut.writeValue( value );
  }


	/* --- Empfang von Signalen aus einen anderen Thread --- */

  /*
   * Die Methode fireReset besagt, dass der Emulations-Thread
   * zurueckgesetzt werden soll.
   * Dazu muss dieser ggf. aufgeweckt werden,
   * was durch Z80CPU.fireReset() geschieht.
   * Das eigentliche Zuruecksetzen der CPU und der Peripherie geschieht
   * im Emulations-Thread.
   */
  public void fireReset( ResetLevel resetLevel )
  {
    synchronized( this.monitor ) {
      this.resetLevel = resetLevel;
      this.loadData   = null;
      this.z80cpu.fireExit();
    }
  }


  /*
   * Diese Methode ladet Daten in den Arbeitsspeicher und startet
   * diese bei Bedarf.
   * Sind die Datenbytes als Programm zu starten, so werden sie
   * in den Emulations-Thread ueberfuehrt und dort geladen und gestartet.
   * Anderenfalls erfolgt das Laden sofort.
   *
   * Um die Programmausfuehrung an einer bestimmten Stelle fortzusetzen
   * (Programmstart), wird die CPU-Emulation zurueckgesetzt.
   * Dadurch wird ein definierter Startzustand und ggf. das Aufwecken
   * des CPU-Emulations-Threads aus dem Wartezustand sichergestellt.
   */
  public void loadIntoMemory( LoadData loadData )
  {
    if( loadData.getStartAddr() >= 0 ) {
      synchronized( this.monitor ) {
	this.loadData = loadData;
	this.z80cpu.firePause( false );
	this.z80cpu.fireExit();
      }
    } else {
      loadData.loadIntoMemory( this );
    }
  }


  public void stopEmulator()
  {
    this.emuRunning = false;
    this.z80cpu.fireExit();
  }


	/* --- Z80IOSystem --- */

  public int readIOByte( int port )
  {
    return this.emuSys.readIOByte( port );
  }


  public void writeIOByte( int port, int value )
  {
    this.emuSys.writeIOByte( port, value );
  }


	/* --- Z80Memory --- */

  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int      rv      = 0xFF;
    boolean  done    = false;
    ExtROM[] extROMs = this.extROMs;
    if( extROMs != null ) {
      for( int i = 0; !done && (i < extROMs.length); i++ ) {
	ExtROM rom = this.extROMs[ i ];
	if( (addr >= rom.getBegAddress()) && (addr <= rom.getEndAddress()) ) {
	  rv = rom.getByte( addr );
	  done = true;
	}
      }
    }
    if( !done ) {
      rv = this.emuSys.getMemByte( addr );
    }
    return rv;
  }


  public int getMemWord( int addr )
  {
    return (this.emuSys.getMemByte( addr + 1 ) << 8)
				| this.emuSys.getMemByte( addr );
  }


  public void setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean  done    = false;
    ExtROM[] extROMs = this.extROMs;
    if( extROMs != null ) {
      for( int i = 0; !done && (i < extROMs.length); i++ ) {
	ExtROM rom = this.extROMs[ i ];
	if( (addr >= rom.getBegAddress()) && (addr <= rom.getEndAddress()) )
	  done = true;
      }
    }
    if( !done )
      this.emuSys.setMemByte( addr, value );
  }


  public void setMemWord( int addr, int value )
  {
    this.emuSys.setMemByte( addr, value & 0xFF );
    this.emuSys.setMemByte( addr + 1, (value >> 8) & 0xFF );
  }


	/* --- Methoden fuer Thread --- */

  public void run()
  {
    this.emuRunning = true;
    while( this.emuRunning ) {
      try {

	/*
	 * Pruefen, ob ein Programm geladen oder der Emulator
	 * tatsaechlich zurueckgesetzt werden soll
	 */
	LoadData loadData = null;
	synchronized( this.monitor ) {
	  loadData = this.loadData;
	  if( loadData != null ) {
	    this.loadData = null;
	  } else {
	    if( this.nextEmuSys != null ) {
	      this.emuSys      = this.nextEmuSys;
	      this.extROMs     = this.nextExtROMs;
	      this.nextEmuSys  = null;
	      this.nextExtROMs = null;
	    }
	    if( this.resetLevel == ResetLevel.POWER_ON ) {
	      for( int i = 0; i < this.ram.length; i++ ) {
		this.ram[ i ] = (byte) 0;
	      }
	    }
	  }
	}
	if( loadData != null ) {
	  loadData.loadIntoMemory( this );
	  this.z80cpu.setRegPC( loadData.getStartAddr() );
	  int spInitValue = this.emuSys.getAppStartStackInitValue();
	  if( spInitValue > 0 ) {
	    this.z80cpu.setRegSP( spInitValue );
	  }
	} else {
	  if( (this.resetLevel == ResetLevel.COLD_RESET)
	      || (this.resetLevel == ResetLevel.POWER_ON) )
	  {
	    checkLoadRF( this.ramFloppyA, 'A', "jkcemu.ramfloppy.a" );
	    checkLoadRF( this.ramFloppyB, 'B', "jkcemu.ramfloppy.b" );
	    this.z80cpu.resetCPU( true );
	  } else {
	    this.z80cpu.resetCPU( false );
	  }
	  this.emuSys.reset( this.resetLevel );
	  this.z80cpu.setRegPC(
			this.emuSys.getResetStartAddress( this.resetLevel ) );
	}
	this.resetLevel = ResetLevel.NO_RESET;

	// in die Z80-Emulation verzweigen
	this.z80cpu.run();
      }
      catch( Z80ExternalException ex ) {}
      catch( Exception ex ) {
	SwingUtilities.invokeLater( new ErrorMsg( this.screenFrm, ex ) );
      }
    }
  }


	/* --- private Methoden --- */

  private void checkLoadRF(
			RAMFloppy ramFloppy,
			char      rfChar,
			String    propsPrefix )
  {
    String fileName = Main.getProperty( propsPrefix + ".file.name" );
    if( fileName != null ) {
      if( fileName.length() > 0 ) {
	try {
	  ramFloppy.load( new File( fileName ) );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  this.screenFrm.fireShowErrorDlg(
			String.format(
				"RAM-Floppy %c kann nicht geladen werden\n%s",
				rfChar,
				msg != null ? msg : "" ) );
	}
      }
    }
  }
}

