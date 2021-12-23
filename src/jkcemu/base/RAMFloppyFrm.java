/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die RAM-Floppies
 */

package jkcemu.base;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JMenu;
import jkcemu.Main;
import jkcemu.text.TextUtil;


public class RAMFloppyFrm extends BaseFrm
{
  private static final String ACTION_CLOSE = "close";
  private static final String ACTION_HELP  = "help";
  private static final String HELP_PAGE    = "/help/ramfloppy.htm";

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
    if( instance != null ) {
      instance.doClose();
      instance = null;
    }
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
  public boolean applySettings( Properties props )
  {
    boolean   rv        = super.applySettings( props );
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
      close();
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
	  if( cmd.equals( ACTION_CLOSE ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( ACTION_HELP ) ) {
	    rv = true;
	    HelpFrm.openPage( HELP_PAGE );
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
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

    int       nRFs = 0;
    RAMFloppy rf1  = null;
    RAMFloppy rf2  = null;
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


    // Menu Datei
    JMenu mnuFile = createMenuFile();
    mnuFile.add( createMenuItemClose( ACTION_CLOSE ) );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();
    mnuHelp.add(
	createMenuItem( "Hilfe zu RAM-Floppies...", ACTION_HELP ) );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    if( nRFs > 0 ) {
      setLayout( new GridLayout( nRFs, 1, 5, 5 ) );

      if( rf1 != null ) {
	this.rfInfo1 = rf1.getInfoText();
	this.rfSize1 = rf1.getSize();
	this.rfFld1  = new RAMFloppyFld( this, rf1 );
	this.rfFld1.setBorder(
		GUIFactory.createTitledBorder( this.rfInfo1 ) );
	add( this.rfFld1 );
	--nRFs;
      }

      if( (nRFs > 0) && (rf2 != null) ) {
	this.rfInfo2 = rf2.getInfoText();
	this.rfSize2 = rf2.getSize();
	this.rfFld2  = new RAMFloppyFld( this, rf2 );
	this.rfFld2.setBorder(
		GUIFactory.createTitledBorder( this.rfInfo2 ) );
	add( this.rfFld2 );
      }
    }


    // Fenstergroesse
    setResizable( true );
    if( !super.applySettings( Main.getProperties() ) ) {
      setLocationByPlatform( true );
      pack();
    }


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
