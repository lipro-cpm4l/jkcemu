/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.audio.*;
import jkcemu.disk.*;
import jkcemu.emusys.*;
import jkcemu.joystick.*;
import jkcemu.print.PrintMngr;
import jkcemu.text.TextUtil;
import z80emu.*;


public class EmuThread extends Thread implements
					Z80IOSystem,
					Z80Memory
{
  public enum ResetLevel { NO_RESET, WARM_RESET, COLD_RESET, POWER_ON };

  private ScreenFrm           screenFrm;
  private Z80CPU              z80cpu;
  private Object              monitor;
  private JoystickFrm         joyFrm;
  private JoystickThread[]    joyThreads;
  private byte[]              ram;
  private byte[]              ramExtended;
  private RAMFloppy           ramFloppy1;
  private RAMFloppy           ramFloppy2;
  private PrintMngr           printMngr;
  private volatile AudioIn    audioIn;
  private volatile AudioOut   audioOut;
  private volatile LoadData   loadData;
  private volatile ResetLevel resetLevel;
  private volatile boolean    emuRunning;
  private volatile EmuSys     emuSys;
  private volatile Boolean    iso646de;


  public EmuThread( ScreenFrm screenFrm, Properties props )
  {
    super( "JKCEMU CPU" );
    this.screenFrm   = screenFrm;
    this.z80cpu      = new Z80CPU( this, this );
    this.monitor     = "a monitor object for synchronization";
    this.joyFrm      = null;
    this.joyThreads  = new JoystickThread[ 2 ];
    this.ram         = new byte[ 0x10000 ];
    this.ramExtended = null;
    this.ramFloppy1  = new RAMFloppy();
    this.ramFloppy2  = new RAMFloppy();
    this.printMngr   = new PrintMngr();
    this.audioIn     = null;
    this.audioOut    = null;
    this.loadData    = null;
    this.resetLevel  = ResetLevel.POWER_ON;
    this.emuRunning  = false;
    this.emuSys      = null;
    Arrays.fill( this.joyThreads, null );
    applySettings( props );
  }


  public synchronized void applySettings( Properties props )
  {
    // zu emulierendes System ermitteln
    boolean done   = false;
    EmuSys  emuSys = this.emuSys;
    if( emuSys != null ) {
      if( emuSys.canApplySettings( props ) ) {
	done = true;
      }
    }
    if( (emuSys != null) && done ) {
      emuSys.applySettings( props );
    } else {
      if( emuSys != null ) {
	emuSys.cancelPastingText();
	emuSys.die();
      }
      String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
      if( sysName.startsWith( "AC1" ) ) {
	emuSys = new AC1( this, props );
      } else if( sysName.startsWith( "BCS3" ) ) {
	emuSys = new BCS3( this, props );
      } else if( sysName.startsWith( "C80" ) ) {
	emuSys = new C80( this, props );
      } else if( sysName.startsWith( "HueblerEvertMC" ) ) {
	emuSys = new HueblerEvertMC( this, props );
      } else if( sysName.startsWith( "HueblerGraphicsMC" ) ) {
	emuSys = new HueblerGraphicsMC( this, props );
      } else if( sysName.startsWith( "KC85/1" )
		 || sysName.startsWith( "KC87" )
		 || sysName.startsWith( "Z9001" ) )
      {
	emuSys = new Z9001( this, props );
      } else if( sysName.startsWith( "HC900" )
		 || sysName.startsWith( "KC85/2" )
		 || sysName.startsWith( "KC85/3" )
		 || sysName.startsWith( "KC85/4" )
		 || sysName.startsWith( "KC85/5" ) )
      {
	emuSys = new KC85( this, props );
      } else if( sysName.startsWith( "KCcompact" ) ) {
	emuSys = new KCcompact( this, props );
      } else if( sysName.startsWith( "KramerMC" ) ) {
	emuSys = new KramerMC( this, props );
      } else if( sysName.startsWith( "LC80" ) ) {
	emuSys = new LC80( this, props );
      } else if( sysName.startsWith( "LLC1" ) ) {
	emuSys = new LLC1( this, props );
      } else if( sysName.startsWith( "LLC2" ) ) {
	emuSys = new LLC2( this, props );
      } else if( sysName.startsWith( "PC/M" ) ) {
	emuSys = new PCM( this, props );
      } else if( sysName.startsWith( "Poly880" ) ) {
	emuSys = new Poly880( this, props );
      } else if( sysName.startsWith( "SC2" ) ) {
	emuSys = new SC2( this, props );
      } else if( sysName.startsWith( "SLC1" ) ) {
	emuSys = new SLC1( this, props );
      } else if( sysName.startsWith( "VCS80" ) ) {
	emuSys = new VCS80( this, props );
      } else if( sysName.startsWith( "Z1013" ) ) {
	emuSys = new Z1013( this, props );
      } else {
	emuSys = new A5105( this, props );
      }
      ResetLevel resetLevel = ResetLevel.POWER_ON;
      if( this.emuSys != null ) {
	String s1 = this.emuSys.getTitle();
	String s2 = emuSys.getTitle();
	if( (s1 != null) && (s2 != null) ) {
	  if( s1.equals( s2 ) ) {
	    resetLevel = ResetLevel.COLD_RESET;
	  }
	}
      }
      this.emuSys = emuSys;
      fireReset( resetLevel );
    }

    // Floppy Disks
    FloppyDiskStationFrm frm = FloppyDiskStationFrm.getSharedInstance(
							this.screenFrm );
    if( frm != null ) {
      int n = emuSys.getSupportedFloppyDiskDriveCount();
      frm.setDriveCount( n );
      for( int i = 0; i < n; i++ ) {
	emuSys.setFloppyDiskDrive( i, frm.getDrive( i ) );
      }
    }

    // Joysticks
    int nJoys = emuSys.getSupportedJoystickCount();
    for( int i = 0; i < this.joyThreads.length; i++ ) {
      JoystickThread jt = null;
      synchronized( this.joyThreads ) {
	jt = this.joyThreads[ i ];
	if( jt != null ) {
	  if( i >= nJoys ) {
	    jt.fireStop();
	    this.joyThreads[ i ] = null;
	  }
	  jt = null;
	} else {
	  if( i < nJoys ) {
	    jt = new JoystickThread( this, i, false );
	    this.joyThreads[ i ] = jt;
	  }
	}
      }
      if( jt != null ) {
	jt.start();
      }
      updJoystickFrm( i );
    }

    // CPU-Geschwindigkeit
    updCPUSpeed( props );

    // sonstiges
    this.iso646de = null;
  }


  public void changeJoystickConnectState( int joyNum )
  {
    JoystickThread jt = null;
    synchronized( this.joyThreads ) {
      if( (joyNum >= 0) && (joyNum < this.joyThreads.length) ) {
	jt = this.joyThreads[ joyNum ];
	if( jt != null ) {
	  try {
	    jt.fireStop();
	    jt.join( 500 );
	  }
	  catch( InterruptedException ex ) {}
	  this.joyThreads[ joyNum ] = null;
	} else {
	  jt = new JoystickThread( this, joyNum, true );
	  this.joyThreads[ joyNum ] = jt;
	  jt.start();
	}
	updJoystickFrm( joyNum );
      }
    }
  }


  public void fireShowJoystickError( final String msg )
  {
    final Component owner = this.joyFrm;
    if( (owner != null) && (msg != null) ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    BasicDlg.showErrorDlg( owner, msg );
		  }
		} );
    }
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    int    rv      = A5105.getDefaultSpeedKHz();
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName != null ) {
      if( sysName.startsWith( "AC1" ) ) {
	rv = AC1.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "BCS3" ) ) {
	rv = BCS3.getDefaultSpeedKHz( props );
      }
      else if( sysName.startsWith( "C80" ) ) {
	rv = C80.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "HC900" )
	       || sysName.startsWith( "KC85/2" )
	       || sysName.startsWith( "KC85/3" )
	       || sysName.startsWith( "KC85/4" )
	       || sysName.startsWith( "KC85/5" ) )
      {
	rv = KC85.getDefaultSpeedKHz( props );
      }
      else if( sysName.startsWith( "HueblerEvertMC" ) ) {
	rv = HueblerEvertMC.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "HueblerGraphicsMC" ) ) {
	rv = HueblerGraphicsMC.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "KC85/1" )
	       || sysName.startsWith( "KC87" )
	       || sysName.startsWith( "Z9001" ) )
      {
	rv = Z9001.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "KCcompact" ) ) {
	rv = KCcompact.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "KramerMC" ) ) {
	rv = KramerMC.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "LC80" ) ) {
	rv = LC80.getDefaultSpeedKHz( props );
      }
      else if( sysName.startsWith( "LLC1" ) ) {
	rv = LLC1.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "LLC2" ) ) {
	rv = LLC2.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "PC/M" ) ) {
	rv = PCM.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "Poly880" ) ) {
	rv = Poly880.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "SC2" ) ) {
	rv = SC2.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "SLC1" ) ) {
	rv = SLC1.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "VCS80" ) ) {
	rv = VCS80.getDefaultSpeedKHz();
      }
      else if( sysName.startsWith( "Z1013" ) ) {
	rv = Z1013.getDefaultSpeedKHz( props );
      }
    }
    return rv;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
  }


  public Boolean getISO646DE()
  {
    return this.iso646de;
  }


  public synchronized byte[] getExtendedRAM( int size )
  {
    byte[] rv = null;
    if( size > 0 ) {
      if( this.ramExtended != null ) {
	if( size > this.ramExtended.length ) {
	  rv = new byte[ size ];
	  Arrays.fill( rv, (byte) 0 );
	  System.arraycopy(
			this.ramExtended,
			0,
			rv,
			0,
			this.ramExtended.length );
	  this.ramExtended = rv;
	} else {
	  rv = this.ramExtended;
	}
      } else {
	rv = new byte[ size ];
	Arrays.fill( rv, (byte) 0 );
	this.ramExtended = rv;
      }
    }
    return rv;
  }


  public PrintMngr getPrintMngr()
  {
    return this.printMngr;
  }


  public int getRAMByte( int addr )
  {
    return (int) this.ram[ addr & 0xFFFF ] & 0xFF;
  }


  public RAMFloppy getRAMFloppy1()
  {
    return this.ramFloppy1;
  }


  public RAMFloppy getRAMFloppy2()
  {
    return this.ramFloppy2;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public Z80CPU getZ80CPU()
  {
    return this.z80cpu;
  }


  public static boolean isColdReset( EmuThread.ResetLevel resetLevel )
  {
    return (resetLevel == EmuThread.ResetLevel.POWER_ON)
		|| (resetLevel == EmuThread.ResetLevel.POWER_ON);
  }


  /*
   * Mit diese Methode wird ermittelt,
   * ob die Tonausgabe auf dem Kassettenrecorderanschluss
   * oder einen evtl. vorhandenen Lautsprecheranschluss
   * emuliert werden soll.
   */
  public boolean isSoundOutEnabled()
  {
    return audioOut != null ? audioOut.isSoundOutEnabled() : false;
  }


  public void joystickThreadTerminated( JoystickThread t )
  {
    synchronized( this.joyThreads ) {
      for( int i = 0; i < this.joyThreads.length; i++ ) {
	if( this.joyThreads[ i ] == t ) {
	  this.joyThreads[ i ] = null;
	  updJoystickFrm( i );
	}
      }
    }
  }


  public boolean keyPressed( KeyEvent e )
  {
    return this.emuSys != null ?
		this.emuSys.keyPressed(
				e.getKeyCode(),
				e.isControlDown(),
				e.isShiftDown() )
		: false;
  }


  public void keyReleased()
  {
    if( this.emuSys != null )
      this.emuSys.keyReleased();
  }


  public void keyTyped( char ch )
  {
    if( this.emuSys != null ) {
      if( this.emuSys.getSwapKeyCharCase() ) {
	if( Character.isUpperCase( ch ) ) {
	  ch = Character.toLowerCase( ch );
	} else if( Character.isLowerCase( ch ) ) {
	  ch = Character.toUpperCase( ch );
	}
      }
      if( this.emuSys.getConvertKeyCharToISO646DE() ) {
	this.emuSys.keyTyped( TextUtil.toISO646DE( ch ) );
      } else {
	this.emuSys.keyTyped( ch );
      }
    }
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
    EmuSys emuSys = this.emuSys;
    if( emuSys != null ) {
      emuSys.audioOutChanged( audioOut );
    }
  }


  public void setISO646DE( boolean state )
  {
    this.iso646de = state;
  }


  public void setJoystickAction( int joyNum, int actionMask )
  {
    EmuSys emuSys = this.emuSys;
    if( emuSys != null ) {
      emuSys.setJoystickAction( joyNum, actionMask );
    }
    JoystickFrm joyFrm = this.joyFrm;
    if( joyFrm != null ) {
      joyFrm.setJoystickAction( joyNum, actionMask );
    }
  }


  public void setJoystickFrm( JoystickFrm joyFrm )
  {
    this.joyFrm = joyFrm;
    synchronized( this.joyThreads ) {
      for( int i = 0; i < this.joyThreads.length; i++ ) {
	updJoystickFrm( i );
      }
    }
  }


  public void setMemWord( int addr, int value )
  {
    setMemByte( addr, value & 0xFF );
    setMemByte( addr + 1, (value >> 8) & 0xFF );
  }


  public void setRAMByte( int addr, int value )
  {
    addr &= 0xFFFF;
    this.ram[ addr ] = (byte) value;
  }


  public void updCPUSpeed( Properties props )
  {
    int    maxSpeedKHz  = getDefaultSpeedKHz( props );
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
    this.screenFrm.clearScreenSelection();
    EmuSys emuSys = this.emuSys;
    if( emuSys != null ) {
      emuSys.cancelPastingText();
    }
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
    for( int i = 0; i < this.joyThreads.length; i++ ) {
      JoystickThread jt = this.joyThreads[ i ];
      if( jt != null ) {
	jt.fireStop();
      }
    }
    this.emuRunning = false;
    this.z80cpu.fireExit();
  }


	/* --- Z80IOSystem --- */

  @Override
  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( this.emuSys != null ) {
      rv = this.emuSys.readIOByte( port );
    }
    return rv;
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    if( this.emuSys != null )
      this.emuSys.writeIOByte( port, value );
  }


	/* --- Z80Memory --- */

  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    return this.emuSys.getMemByte( addr & 0xFFFF, m1 );
  }


  @Override
  public int getMemWord( int addr )
  {
    return (getMemByte( addr + 1, false ) << 8) | getMemByte( addr, false );
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    return this.emuSys.readMemByte( addr & 0xFFFF, m1 );
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    return this.emuSys.setMemByte( addr & 0xFFFF, value );
  }


  @Override
  public void writeMemByte( int addr, int value )
  {
    this.emuSys.writeMemByte( addr & 0xFFFF, value );
  }


	/* --- ueberschriebene Methoden fuer Thread --- */

  @Override
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
	    if( this.resetLevel == ResetLevel.POWER_ON ) {
	      Arrays.fill( this.ram, (byte) 0 );
	    }
	  }
	}
	if( loadData != null ) {
	  loadData.loadIntoMemory( this );
	  this.z80cpu.setRegPC( loadData.getStartAddr() );
	  if( this.emuSys != null ) {
	    int spInitValue = this.emuSys.getAppStartStackInitValue();
	    if( spInitValue > 0 ) {
	      this.z80cpu.setRegSP( spInitValue );
	    }
	  }
	} else {
	  if( (this.resetLevel == ResetLevel.COLD_RESET)
	      || (this.resetLevel == ResetLevel.POWER_ON) )
	  {
	    this.z80cpu.resetCPU( true );
	  } else {
	    this.z80cpu.resetCPU( false );
	  }
	  if( this.emuSys != null ) {
	    this.emuSys.reset( this.resetLevel, Main.getProperties() );
	    this.z80cpu.setRegPC(
			this.emuSys.getResetStartAddress( this.resetLevel ) );
	  }
	}

	// RAM-Floppies und Druckmanager zuruecksetzen
	this.printMngr.reset();
	this.ramFloppy1.reset();
	this.ramFloppy2.reset();
	if( (this.emuSys != null)
	    && (this.resetLevel == ResetLevel.POWER_ON)
	    && Main.getBooleanProperty(
                        "jkcemu.ramfloppy.clear_on_power_on",
                        false ) )
	{
	  if( this.emuSys.supportsRAMFloppy1()
	      && (this.ramFloppy1.getUsedSize() > 0) )
	  {
	    this.ramFloppy1.clear();
	  }
	  if( this.emuSys.supportsRAMFloppy2()
	      && (this.ramFloppy2.getUsedSize() > 0) )
	  {
	    this.ramFloppy2.clear();
	  }
	}

	// Fenster informieren
	final Frame[] frms = Frame.getFrames();
	if( frms != null ) {
	  EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    for( Frame f : frms ) {
		      if( f instanceof BasicFrm ) {
			((BasicFrm) f).resetFired();
		      }
		    }
		  }
		} );
	}

	// in die Z80-Emulation verzweigen
	this.resetLevel = ResetLevel.NO_RESET;
	this.z80cpu.run();
      }
      catch( Z80ExternalException ex ) {}
      catch( Exception ex ) {
	this.emuRunning = false;
	EventQueue.invokeLater( new ErrorMsg( this.screenFrm, ex ) );
      }
    }
  }


	/* --- private Methoden --- */

  private void updJoystickFrm( final int joyNum )
  {
    final JoystickFrm joyFrm = this.joyFrm;
    if( (joyFrm != null)
	&& (joyNum >= 0)
	&& (joyNum < this.joyThreads.length) )
    {
      int    nJoys  = 0;
      EmuSys emuSys = this.emuSys;
      if( emuSys != null ) {
	nJoys = emuSys.getSupportedJoystickCount();
      }
      final boolean emulated  = (joyNum < nJoys);
      final boolean connected = (this.joyThreads[ joyNum ] != null);
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    joyFrm.setJoystickState( joyNum, emulated, connected );
		  }
		} );
    }
  }
}

