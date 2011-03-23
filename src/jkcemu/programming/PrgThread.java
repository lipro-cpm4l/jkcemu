/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Thread fuer einen Compiler/Assembler
 */

package jkcemu.programming;

import java.awt.EventQueue;
import java.io.*;
import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.text.EditText;


public class PrgThread extends Thread
{
  protected EmuThread  emuThread;
  protected EditText   editText;
  protected Appendable logOut;
  protected PrgOptions options;
  protected boolean    forceRun;
  protected boolean    running;

  private String sourceText;
  private int    sourceLen;
  private int    sourcePos;
  private int    sourceLineNum;
  private int    errorCount;


  public PrgThread(
		String     threadName,
		EmuThread  emuThread,
		EditText   editText,
		String     sourceText,
		Appendable logOut,
		PrgOptions options,
		boolean    forceRun )
  {
    super( threadName );
    this.emuThread     = emuThread;
    this.editText      = editText;
    this.logOut        = logOut;
    this.options       = options;
    this.forceRun      = forceRun;
    this.running       = true;
    this.sourceText    = (sourceText != null ? sourceText : "");
    this.sourceLen     = this.sourceText.length();
    this.sourcePos     = 0;
    this.sourceLineNum = 0;
    this.errorCount    = 0;
  }


  public void appendErrorCountToLog()
  {
    if( this.logOut != null ) {
      try {
	this.logOut.append( (char) '\n' );
	this.logOut.append( Integer.toString( this.errorCount ) );
	this.logOut.append( " Fehler\n" );
      }
      catch( IOException ex ) {}
    }
  }


  public void appendLineNumMsgToLog( String msg, String msgType )
  {
    StringBuilder buf = new StringBuilder( 128 );
    if( this.sourceLineNum > 0 ) {
      if( msgType != null ) {
        buf.append( msgType );
        buf.append( " in " );
      }
      buf.append( "Zeile " );
      buf.append( this.sourceLineNum );
      buf.append( ": " );
    }
    buf.append( msg );
    if( !msg.endsWith( "\n" ) ) {
      buf.append( (char) '\n' );
    }
    appendToLog( buf.toString() );
  }


  public void appendToLog( String text )
  {
    if( (text != null) && (this.logOut != null) ) {
      try {
	this.logOut.append( text );
      }
      catch( IOException ex ) {}
    }
  }


  public void clearErrorCount()
  {
    this.errorCount = 0;
  }


  public void fireStop()
  {
    this.running = false;
  }


  protected void fireReplaceSourceText( final String text )
  {
    final EditText editText = this.editText;
    EventQueue.invokeLater(
		new Runnable()
		{
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


  public int getErrorCount()
  {
    return this.errorCount;
  }


  public int getSourceLength()
  {
    return this.sourceLen;
  }


  public int getSourceLineNum()
  {
    return this.sourceLineNum;
  }


  public void incErrorCount() throws TooManyErrorsException
  {
    this.errorCount++;
    if( this.errorCount >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  public void putWarning( String msg )
  {
    if( msg != null )
      appendLineNumMsgToLog( msg, "Warnung" );
  }


  public String readLine()
  {
    String line = null;
    if( this.sourcePos < this.sourceLen ) {
      int eol = this.sourceText.indexOf( '\n', this.sourcePos );
      if( eol >= this.sourcePos ) {
	line           = this.sourceText.substring( this.sourcePos, eol );
	this.sourcePos = eol + 1;
      } else {
	line           = this.sourceText.substring( this.sourcePos );
	this.sourcePos = this.sourceLen;
      }
      this.sourceLineNum++;
    }
    return line;
  }


  public void resetSource()
  {
    this.sourcePos     = 0;
    this.sourceLineNum = 0;
  }
}

