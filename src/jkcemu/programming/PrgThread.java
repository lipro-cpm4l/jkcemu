/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Thread fuer einen Compiler/Assembler
 */

package jkcemu.programming;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.text.*;


public abstract class PrgThread extends Thread
{
  protected EmuThread  emuThread;
  protected EditText   editText;
  protected PrgOptions options;

  private Appendable logOut;
  private boolean    execEnabled;


  public PrgThread(
		String     threadName,
		EmuThread  emuThread,
		EditText   editText,
		PrgOptions options,
		Appendable logOut )
  {
    super( Main.getThreadGroup(), threadName );
    this.emuThread   = emuThread;
    this.editText    = editText;
    this.options     = options;
    this.logOut      = logOut;
    this.execEnabled = true;
  }


  protected void appendToLog( String text )
  {
    if( text != null ) {
      try {
	this.logOut.append( text );
      }
      catch( IOException ex ) {}
    }
  }


  public void cancel()
  {
    this.execEnabled = false;
  }


  protected abstract boolean execute() throws IOException;


  protected void fireReplaceSourceText( final String text )
  {
    final EditText editText = this.editText;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    editText.replaceText( text );
		  }
		} );
  }


  public EditText getEditText()
  {
    return this.editText;
  }


  protected void writeCodeToEmu( Z80Assembler assembler, boolean logAddrs )
  {
    boolean forceRun  = this.options.getForceRun();
    byte[]  codeBytes = assembler.getCreatedCode();
    if( codeBytes != null ) {
      int     begAddr   = assembler.getBegAddr();
      int     startAddr = begAddr;
      Integer entryAddr = assembler.getEntryAddr();
      if( entryAddr != null ) {
	startAddr = entryAddr.intValue();
      }
      if( (this.emuThread != null)
	  && (begAddr >= 0) && (begAddr <= 0xFFFF) )
      {
	String secondSysName = null;
	EmuSys emuSys        = null;
	if( this.options.getCodeToSecondSystem() ) {
	  emuSys = this.emuThread.getEmuSys();
	  if( emuSys != null ) {
	    secondSysName = emuSys.getSecondSystemName();
	  }
	}
	appendToLog( "Lade Programmcode in Arbeitsspeicher" );
	if( logAddrs || (secondSysName != null) ) {
	  appendToLog( " (" );
	  if( secondSysName != null ) {
	    appendToLog( secondSysName );
	    appendToLog( ", " );
	  }
	  appendToLog(
		String.format(
			"Bereich %04X-%04X)",
			begAddr,
			begAddr + codeBytes.length -  1) );
	}
	if( forceRun && (startAddr >= 0) ) {
	  appendToLog(
		String.format(
			"\nund starte Programm auf Adresse %04X",
			startAddr ) );
	} else {
	  if( forceRun ) {
	    appendToLog( "\nStart des Programms nicht m\u00F6glich,\n"
		+ "da Quelltext keine ENT-Anweisung (Programmeintrittspunkt)"
		+ " enth\u00E4lt" );
	  }
	}
	appendToLog( "...\n" );
	if( (emuSys != null) && (secondSysName != null) ) {
	  emuSys.loadIntoSecondSystem(
			codeBytes,
			begAddr,
			forceRun ? startAddr : -1 );
	} else {
	  this.emuThread.loadIntoMemory(
		new LoadData(
			codeBytes,
			0,
			codeBytes.length,
			begAddr,
			forceRun ? startAddr : -1,
			null ) );
	}
      }
    } else {
      appendToLog( "Programmcode kann nicht in Emulator geladen" );
      if( forceRun ) {
	appendToLog( " und dort gestartet" );
      }
      appendToLog( " werden,\nda kein einziges Byte erzeugt wurde.\n" );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    TextEditFrm textEditFrm = this.editText.getTextEditFrm();
    try {
      if( execute() ) {
	appendToLog( "Fertig\n" );
      }
    }
    catch( IOException ex ) {
      if( this.execEnabled ) {
	boolean done = false;
	String  msg  = ex.getMessage();
	if( msg != null ) {
	  if( !msg.isEmpty() ) {
	    appendToLog( "Fehler: " );
	    appendToLog( msg );
	    if( !msg.endsWith( "\n" ) ) {
	      appendToLog( "\n" );
	    }
	    done = true;
	  }
	}
	if( !done ) {
	  appendToLog( "Ein-/Ausgabefehler\n" );
	}
      } else {
	appendToLog( "Abgebrochen\n" );
      }
    }
    catch( Exception ex ) {
      if( this.execEnabled ) {
	EventQueue.invokeLater( new ErrorMsg( textEditFrm, ex ) );
      }
    }
    if( textEditFrm != null ) {
      textEditFrm.threadTerminated( this );
    }
  }
}
