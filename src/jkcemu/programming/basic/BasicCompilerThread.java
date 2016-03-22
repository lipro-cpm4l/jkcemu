/*
 * (c) 2012-2015 Jens Mueller
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
				editText.getFile(),
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
						null,
						this.options,
						this.logger,
						true );
	status = assembler.assemble(
			target.getMaxAppNameLen() > 0 ?
				this.basicOptions.getAppName() : null,
			(target instanceof Z9001Target)
				|| (target instanceof Z9001KRTTarget) );
	if( (this.basicOptions.getBssBegAddr() >= 0)
	    && assembler.getOrgOverlapped() )
	{
	  appendToLog( "\nProgrammcode und Bereich f\u00FCr"
		+ " Variablen/Speicherzellen \u00FCberschneiden sich.\n"
		+ "Bitte w\u00E4hlen Sie eine andere Anfangsadresse f\u00FCr"
		+ " Variablen/Speicherzellen!" );
	}
	if( assembler.getRelJumpsTooLong() ) {
	  appendToLog( "\nCompilieren Sie bitte mit ausgeschalteter Option"
				+ " \'Relative Spr\u00FCnge bevorzugen\'!" );
	}
	if( status ) {
	  byte[] code = assembler.getCreatedCode();
	  if( code != null ) {
	    if( code.length > 0 ) {
	      int codeBegAddr = this.basicOptions.getCodeBegAddr();
	      appendToLog( "Speicherbelegung:\n" );
	      appendToLog( String.format(
				"  %04X-%04X: Programmcode\n",
				codeBegAddr,
				codeBegAddr + code.length - 1 ) );
	      Integer topAddr = assembler.getLabelValue(
						BasicCompiler.TOP_LABEL );
	      if( topAddr != null ) {
		int bssBegAddr = this.basicOptions.getBssBegAddr();
		if( bssBegAddr < 0 ) {
		  bssBegAddr = this.basicOptions.getCodeBegAddr() + code.length;
		}
		if( (bssBegAddr >= 0) && (bssBegAddr < topAddr.intValue()) ) {
		  appendToLog( String.format(
				"  %04X-%04X: Variablen, Speicherzellen%s\n",
				bssBegAddr,
				topAddr.intValue() - 1,
				this.basicOptions.getStackSize() > 0 ?
					", Stack" : "" ) );
		}
	      }
	      if( this.options.getCodeToEmu()
		  || this.options.getForceRun() )
	      {
		writeCodeToEmu( assembler, false );
		if( !this.options.getForceRun() && (this.emuThread != null) ) {
		  EmuSys emuSys = this.emuThread.getEmuSys();
		  if( emuSys != null ) {
		    String startCmd = target.getStartCmd(
					emuSys,
					this.basicOptions.getAppName(),
					this.basicOptions.getCodeBegAddr() );
		    if( startCmd != null ) {
		      if( !startCmd.isEmpty() ) {
			appendToLog( "Kommando zum Starten des Programms: " );
			appendToLog( startCmd );
			appendToLog( "\n" );
		      }
		    }
		  }
		}
	      }
	    }
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

