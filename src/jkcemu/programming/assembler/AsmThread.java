/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Assembler-Thread
 */

package jkcemu.programming.assembler;

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.Z9001;
import jkcemu.programming.*;
import jkcemu.text.*;
import jkcemu.tools.ReassFrm;
import jkcemu.tools.debugger.*;


public class AsmThread extends PrgThread
{
  private Z80Assembler assembler;


  public AsmThread(
		EmuThread  emuThread,
		EditText   editText,
		PrgOptions options,
		Appendable logOut )
  {
    super( "JKCEMU assembler", emuThread, editText, options, logOut );
    this.assembler = new Z80Assembler(
				editText.getText(),
				null,
				options,
				PrgLogger.createLogger( logOut ),
				true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean execute() throws IOException
  {
    appendToLog( "Assembliere...\n" );
    boolean forZ9001 = false;
    if( this.emuThread != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	forZ9001 = (emuSys instanceof Z9001 );
      }
    }
    boolean status = this.assembler.assemble( null, forZ9001 );
    if( status ) {
      if( this.options.getFormatSource() ) {
	String srcOut = this.assembler.getFormattedSourceText();
	if( srcOut != null ) {
	  fireReplaceSourceText( srcOut );
	}
      }
      if( this.options.getCodeToEmu() || this.options.getForceRun() ) {
	byte[] code = this.assembler.getCreatedCode();
	if( code != null ) {
	  writeCodeToEmu( this.assembler );
	  if( this.options.getLabelsToDebugger() ) {
	    labelsToDebugger( this.options.getCodeToSecondSystem() );
	  }
	  if( this.options.getLabelsToReassembler() ) {
	    labelsToReass( this.options.getCodeToSecondSystem() );
	  }
	}
      }
    }
    return status;
  }


  @Override
  public void cancel()
  {
    super.cancel();
    this.assembler.cancel();
  }


	/* --- private Methoden --- */

  private void labelsToDebugger( boolean secondSys )
  {
    AsmLabel[] labels = this.assembler.getSortedLabels();
    if( labels != null ) {
      if( labels.length > 0 ) {
	ScreenFrm screenFrm = Main.getScreenFrm();
	if( screenFrm != null ) {
	  boolean  done     = false;
	  DebugFrm debugFrm = null;
	  if( secondSys && (this.emuThread != null) ) {
	    EmuSys emuSys = this.emuThread.getEmuSys();
	    if( emuSys != null ) {
	      if( emuSys.getSecondSystemName() != null ) {
		debugFrm = screenFrm.openSecondDebugger();
		done     = true;
	      }
	    }
	  }
	  if( !done ) {
	    debugFrm = screenFrm.openPrimaryDebugger();
	  }
	  if( debugFrm != null ) {
	    debugFrm.setLabels( labels );
	  }
	}
      }
    }
  }


  private void labelsToReass( boolean secondSys )
  {
    AsmLabel[] labels = this.assembler.getSortedLabels();
    if( labels != null ) {
      if( labels.length > 0 ) {
	ScreenFrm screenFrm = Main.getScreenFrm();
	if( screenFrm != null ) {
	  boolean  done     = false;
	  ReassFrm reassFrm = null;
	  if( secondSys && (this.emuThread != null) ) {
	    EmuSys emuSys = this.emuThread.getEmuSys();
	    if( emuSys != null ) {
	      if( emuSys.getSecondSystemName() != null ) {
		reassFrm = screenFrm.openSecondReassembler();
		done     = true;
	      }
	    }
	  }
	  if( !done ) {
	    reassFrm = screenFrm.openPrimaryReassembler();
	  }
	  if( reassFrm != null ) {
	    reassFrm.setLabels(
			labels,
			this.assembler.getBegAddr(),
			this.assembler.getEndAddr() );
	  }
	}
      }
    }
  }
}

