/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Compiler-Thread
 */

package jkcemu.programming.basic;

import java.awt.*;
import java.io.*;
import java.lang.*;
import jkcemu.base.*;
import jkcemu.emusys.*;
import jkcemu.programming.*;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.programming.basic.target.*;
import jkcemu.text.*;


public class BasicCompilerThread extends PrgThread
{
  private String        sysTitle;
  private PrgLogger     logger;
  private BasicCompiler compiler;
  private BasicOptions  basicOptions;


  public BasicCompilerThread(
			EmuThread    emuThread,
			EditText     editText,
			BasicOptions options,
			Appendable   logOut )
  {
    super( "JKCEMU basic compiler", emuThread, editText, options, logOut );

    EmuSys emuSys = (emuThread != null ? emuThread.getEmuSys() : null);
    if( emuSys != null ) {
      this.sysTitle = emuSys.getTitle();
    } else {
      this.sysTitle = null;
    }
    this.basicOptions = options;
    this.logger       = PrgLogger.createLogger( logOut );
    this.compiler     = new BasicCompiler(
				editText.getText(),
				options,
				this.logger );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean execute() throws IOException
  {
    boolean status = false;
    appendToLog( "Compiliere...\n" );

    AbstractTarget target = this.basicOptions.getTarget();
    if( target != null ) {
      String text = target.toString();
      if( text != null ) {
	appendToLog( "Zielsystem: " );
	appendToLog( text );
	appendToLog( "\n" );
      }
      final String asmText = this.compiler.compile();
      if( asmText != null ) {
	if( this.basicOptions.getShowAssemblerText() ) {
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    openAsmText( asmText );
			  }
			} );
	}
	appendToLog( "Assembliere...\n" );
	Z80Assembler assembler = new Z80Assembler(
						asmText,
						"Assembler-Quelltext",
						this.options,
						this.logger,
						true );
	status = assembler.assemble(
			target.supportsAppName() ?
				this.basicOptions.getAppName() : null,
			(target instanceof Z9001Target)
				|| (target instanceof Z9001KRTTarget) );
	if( assembler.getRelJumpsTooLong() ) {
	  appendToLog( "Compilieren Sie bitte mit ausgeschalteter Option"
				+ " \'Relative Spr\u00FCnge bevorzugen\'" );
	}
	if( status
	    && (this.options.getCodeToEmu() || this.options.getForceRun()) )
	{
	  byte[] code = assembler.getCreatedCode();
	  if( code != null ) {
	    writeCodeToEmu( assembler );
	  }
	}
      }
    } else {
      if( this.sysTitle != null ) {
	appendToLog( "Fehler: Zielsystem \'" );
	appendToLog( this.sysTitle );
	appendToLog( "\' nicht unterst\u00FCtzt.\n" );
      } else {
	appendToLog( "Fehler: Zielsystem unbekannt" );
      }
    }
    return status;
  }


  @Override
  public void cancel()
  {
    super.cancel();
    this.compiler.cancel();
  }


	/* --- private Methoden --- */

  private void openAsmText( String text )
  {
    TextEditFrm textEditFrm = this.editText.getTextEditFrm();
    if( textEditFrm != null ) {
      EditText asmEditText = this.editText.getResultEditText();
      if( asmEditText != null ) {
	if( asmEditText.hasDataChanged()
	    || !textEditFrm.contains( asmEditText ) )
	{
	  asmEditText = null;
	}
      }
      if( asmEditText != null ) {
	asmEditText.setText( text );
	Component tabComponent = asmEditText.getTabComponent();
	if( tabComponent != null ) {
	  textEditFrm.setSelectedTabComponent( tabComponent );
	}
      } else {
	asmEditText = textEditFrm.openText( text );
	this.editText.setResultEditText( asmEditText );
      }
    }
  }
}

