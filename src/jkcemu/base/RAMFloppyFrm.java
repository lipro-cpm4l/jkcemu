/*
 * (c) 2010-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die RAM-Floppies
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.text.TextUtil;


public class RAMFloppyFrm extends BasicFrm
{
  private static RAMFloppyFrm instance = null;

  private EmuThread    emuThread;
  private RAMFloppyFld rfFld1;
  private RAMFloppyFld rfFld2;
  private String       rfInfo1;
  private String       rfInfo2;
  private int          rfSize1;
  private int          rfSize2;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( EmuThread emuThread )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new RAMFloppyFrm( emuThread );
    }
    instance.toFront();
    instance.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean   rv        = super.applySettings( props, resizable );
    boolean   different = true;
    if( this.emuThread != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	RAMFloppy rf1 = null;
	RAMFloppy rf2 = null;
	if( emuSys.supportsRAMFloppy1() ) {
	  rf1 = this.emuThread.getRAMFloppy1();
	}
	if( emuSys.supportsRAMFloppy2() ) {
	  rf2 = this.emuThread.getRAMFloppy2();
	}
	if( equalsRF( rf1, this.rfSize1, this.rfInfo1 )
	    && equalsRF( rf2, this.rfSize2, this.rfInfo2 ) )
	{
	  different = false;
	}
      }
    }
    if( different ) {
      doClose();
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      if( e instanceof ActionEvent ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.equals( "close" ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( "help" ) ) {
	    rv = true;
	    HelpFrm.open( "/help/ramfloppy.htm" );
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void resetFired()
  {
    if( this.rfFld1 != null ) {
      this.rfFld1.fireRAMFloppyChanged();
    }
    if( this.rfFld2 != null ) {
      this.rfFld2.fireRAMFloppyChanged();
    }
  }


	/* --- Konstruktor --- */

  private RAMFloppyFrm( EmuThread emuThread )
  {
    this.emuThread = emuThread;
    this.rfFld1    = null;
    this.rfFld2    = null;
    this.rfInfo1   = null;
    this.rfInfo2   = null;
    this.rfSize1   = -1;
    this.rfSize2   = -1;
    setTitle( "JKCEMU RAM-Floppies" );
    Main.updIcon( this );

    int       nRFs      = 0;
    RAMFloppy rf1       = null;
    RAMFloppy rf2       = null;
    if( emuThread != null ) {
      EmuSys emuSys = emuThread.getEmuSys();
      if( emuSys != null ) {
	if( emuSys.supportsRAMFloppy1() ) {
	  rf1 = emuThread.getRAMFloppy1();
	  if( rf1 != null ) {
	    nRFs++;
	  }
	}
	if( emuSys.supportsRAMFloppy2() ) {
	  rf2 = emuThread.getRAMFloppy2();
	  if( rf2 != null ) {
	    nRFs++;
	  }
	}
      }
    }


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuFile.add( createJMenuItem( "Schlie\u00DFen", "close" ) );
    mnuBar.add( mnuFile );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", "help" ) );
    mnuBar.add( mnuHelp );


    // Fensterinhalt
    if( nRFs > 0 ) {
      setLayout( new GridLayout( nRFs, 1, 5, 5 ) );

      if( rf1 != null ) {
	this.rfInfo1 = rf1.getInfoText();
	this.rfSize1 = rf1.getSize();
	this.rfFld1  = new RAMFloppyFld( this, rf1 );
	this.rfFld1.setBorder(
		BorderFactory.createTitledBorder( this.rfInfo1 ) );
	add( this.rfFld1 );
	--nRFs;
      }

      if( (nRFs > 0) && (rf2 != null) ) {
	this.rfInfo2 = rf2.getInfoText();
	this.rfSize2 = rf2.getSize();
	this.rfFld2  = new RAMFloppyFld( this, rf2 );
	this.rfFld2.setBorder(
		BorderFactory.createTitledBorder( this.rfInfo2 ) );
	add( this.rfFld2 );
      }
    }


    // Fenstergroesse
    setLocationByPlatform( true );
    /*
     * Es soll immer die pack()-Methode aufgerufen werden.
     * Aus diesem Grund soll in der Vater-Klasse die Fenstergroesse
     * nicht gesetzt werden, weshalb als resizable=false uebergeben wird.
     */
    applySettings( Main.getProperties(), false );
    pack();
    setResizable( true );


    // Timer
    (new javax.swing.Timer(
		100,
		new ActionListener()
		{
		  @Override
		  public void actionPerformed( ActionEvent e )
		  {
		    checkLEDState();
		  }
		} )).start();

  }


	/* --- private Methoden --- */

  private void checkLEDState()
  {
    if( this.rfFld1 != null ) {
      this.rfFld1.checkLEDState();
    }
    if( this.rfFld2 != null ) {
      this.rfFld2.checkLEDState();
    }
  }


  private boolean equalsRF( RAMFloppy rf, int size, String info )
  {
    boolean rv = false;
    if( rf != null ) {
      if( (rf.getSize() == size)
	  && TextUtil.equals( rf.getInfoText(), info ) )
      {
	rv = true;
      }
    } else {
      if( size < 0 ) {
	rv = true;
      }
    }
    return rv;
  }
}

