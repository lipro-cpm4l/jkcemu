/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.Color;
import java.lang.*;
import java.util.*;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.audio.*;
import jkcemu.ac1.AC1;
import jkcemu.z1013.Z1013;
import jkcemu.z9001.Z9001;
import z80emu.*;


public class EmuThread extends Thread implements
					Z80IOSystem,
					Z80Memory
{
  private ScreenFrm         screenFrm;
  private Z80CPU            z80cpu;
  private Object            monitor;
  private byte[]            ram;
  private volatile ExtROM[] extROMs;
  private volatile AudioIn  audioIn;
  private volatile AudioOut audioOut;
  private volatile LoadData loadData;
  private volatile boolean  powerOn;
  private volatile boolean  emuRunning;
  private volatile EmuSys   emuSys;
  private volatile EmuSys   nextEmuSys;
  private volatile ExtROM[] nextExtROMs;


  public EmuThread( ScreenFrm screenFrm )
  {
    this.screenFrm   = screenFrm;
    this.z80cpu      = new Z80CPU( this, this );
    this.monitor     = "a monitor object for synchronization";
    this.ram         = new byte[ 0x10000 ];
    this.extROMs     = null;
    this.audioIn     = null;
    this.audioOut    = null;
    this.loadData    = null;
    this.powerOn     = true;
    this.emuRunning  = false;
    this.emuSys      = null;
    this.nextEmuSys  = null;
    this.nextExtROMs = null;
    applySettings( Main.getProperties() );
    fireReset( true );
  }


  public void applySettings( Properties props )
  {
    applySettings(
		props,
		EmuUtil.readExtROMs( this.screenFrm, props ), false );
  }


  public synchronized void applySettings(
				Properties props,
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
      if( sysName.startsWith( "Z1013" ) ) {
	emuSys = new Z1013( this, props );
      } else if( sysName.startsWith( "AC1" ) ) {
	emuSys = new AC1( this );
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
    if( reqReset || (this.emuSys == null) ) {
      fireReset( false );
    }

    // CPU-Geschwindigkeit
    int    maxSpeedKHz  = getDefaultSpeedKHz( emuSys.getSystemName() );
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


  public static int getDefaultSpeedKHz( String sysName )
  {
    int rv = 2000;	// AC1, Z1013
    if( sysName != null ) {
      if( sysName.startsWith( "KC85/1" )
	  || sysName.startsWith( "KC87" )
	  || sysName.startsWith( "Z9001" ) )
      {
	rv = 2458;	// eigentlich 2,4576 MHz
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


  public ExtROM[] getExtROMs()
  {
    return this.extROMs;
  }


  public int getRAMByte( int addr )
  {
    return (int) this.ram[ addr & 0xFFFF ] & 0xFF;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public Z80CPU getZ80CPU()
  {
    return this.z80cpu;
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


  public void updAudioOutPhase( boolean phase )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null )
      audioOut.updPhase( phase );
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
  public void fireReset( boolean powerOn )
  {
    synchronized( this.monitor ) {
      this.powerOn  = powerOn;
      this.loadData = null;
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
	    if( this.powerOn ) {
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
	  this.z80cpu.resetCPU( this.powerOn );
	  this.emuSys.reset( this.powerOn );
	  this.z80cpu.setRegPC( this.emuSys.getResetStartAddress() );
	}
	this.powerOn = false;

	// in die Z80-Emulation verzweigen
	this.z80cpu.run();
      }
      catch( Z80ExternalException ex ) {}
      catch( Exception ex ) {
	SwingUtilities.invokeLater( new ErrorMsg( this.screenFrm, ex ) );
      }
    }
  }
}

