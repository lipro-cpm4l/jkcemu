/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Thread fuer einen Compiler/Assembler
 */

package jkcemu.programming;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.IOException;
import jkcemu.Main;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.ErrorMsg;
import jkcemu.file.FileFormat;
import jkcemu.file.LoadData;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.text.EditText;
import jkcemu.text.TextEditFrm;


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


  protected void fireOpenResultText( final String text )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openResultText( text );
		  }
		} );
  }


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


  protected void writeCodeToEmu(
			Z80Assembler assembler,
			FileFormat   fileFmt,
			boolean      logAddrs )
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
	appendToLog( "Lade Programmcode in Arbeitsspeicher...\n" );
	try {
	  if( (emuSys != null) && (secondSysName != null) ) {
	    emuSys.loadIntoSecondSystem( codeBytes, begAddr );
	    appendToLog(
		String.format(
			"Programmcode in %s nach %04X-%04X geladen\n",
			secondSysName,
			begAddr,
			begAddr + codeBytes.length -  1) );
	    if( forceRun ) {
	      appendToLog( "\n" + secondSysName
		+ ": automatischer Programmstart nicht unterst\u00FCtzt\n" );
	    }
	  } else {
	    StringBuilder rvStatusMsg = new StringBuilder();
	    this.emuThread.loadIntoMemory(
		new LoadData(
			codeBytes,
			0,
			codeBytes.length,
			begAddr,
			forceRun ? startAddr : -1,
			fileFmt ),
		rvStatusMsg );
	    if( rvStatusMsg.length() > 0 ) {
	      String msg     = rvStatusMsg.toString();
	      String pattern = "Datei ";
	      if( msg.startsWith( pattern ) ) {
		appendToLog( "Programmcode "
				+ msg.substring( pattern.length() ) );
	      } else {
		appendToLog( msg );
	      }
	    } else if( logAddrs ) {
	      appendToLog(
		String.format(
			"Programmcode nach %04X-%04X geladen",
			begAddr,
			begAddr + codeBytes.length -  1) );
	    }
	    appendToLog( "\n" );
	    if( forceRun ) {
	      if( startAddr >= 0 ) {
		appendToLog(
			String.format(
				"Starte Programm auf Adresse %04X...\n",
				startAddr ) );
	      } else {
		appendToLog( "\nStart des Programms nicht m\u00F6glich,\n"
			+ "da Quelltext keine ENT-Anweisung"
			+ " (Programmeintrittspunkt) enth\u00E4lt\n" );
	      }
	    }
	  }
	}
	catch( IOException ex ) {
	  appendToLog( "Laden des Programmcodes fehlgeschlagen\n" );
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( !msg.isEmpty() ) {
	      appendToLog( msg + "\n" );
	    }
	  }
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
	ErrorMsg.showLater( textEditFrm, ex );
      }
    }
    if( textEditFrm != null ) {
      textEditFrm.threadTerminated( this );
    }
  }


	/* --- private Methoden --- */

  private void openResultText( String text )
  {
    TextEditFrm textEditFrm = this.editText.getTextEditFrm();
    if( textEditFrm != null ) {
      EditText editText = this.editText.getResultEditText();
      if( editText != null ) {
	if( editText.hasDataChanged()
	    || !textEditFrm.contains( editText ) )
	{
	  editText = null;
	}
      }
      if( editText != null ) {
	editText.setText( text );
	Component tab = editText.getTab();
	if( tab != null ) {
	  textEditFrm.setSelectedTab( tab );
	}
      } else {
	editText = textEditFrm.openText( text );
	this.editText.setResultEditText( editText );
      }
    }
  }
}
