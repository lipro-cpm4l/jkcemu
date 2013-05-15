/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kommandozeilenschnittstelle des Assemblers
 */

package jkcemu.programming.assembler;

import java.io.*;
import java.lang.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.*;


public class CmdLineAssembler
{
  private static final String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jkcemu.jar --as [Optionen] <Datei>",
	"  java -jar jkcemu.jar --assembler [Optionen] <Datei>",
	"",
	"Optionen:",
	"  -h            diese Hilfe anzeigen",
	"  -l            Markentabelle ausgeben",
	"  -o <Datei>    Ausgabedatei festlegen",
	"  -C            Gro\u00DF-/Kleinschreibung bei Marken beachten",
	"  -U            undokumentierte Befehle erlauben",
	"  -R            nur Robotron-Syntax erlauben",
	"  -Z            nur Zilog-Syntax erlauben",
	"" };


  public static boolean execute( String[] args, int argIdx )
  {
    boolean status       = false;
    boolean caseFlag     = false;
    boolean helpFlag     = false;
    boolean robotronFlag = false;
    boolean zilogFlag    = false;
    boolean undocFlag    = false;
    boolean listFlag     = false;
    String  outFileName  = null;
    String  srcFileName  = null;
    try {
      while( argIdx < args.length ) {
	String arg = args[ argIdx++ ];
	if( arg != null ) {
	  if( !arg.isEmpty() ) {
	    if( arg.charAt( 0 ) == '-' ) {
	      int len = arg.length();
	      if( len < 2 ) {
		throwWrongCmdLine();
	      }
	      int pos = 1;
	      while( pos < len ) {
		char ch = arg.charAt( pos++ );
		switch( ch ) {
		  case 'h':
		  case 'H':
		    helpFlag = true;
		    break;
		  case 'C':
		    caseFlag = true;
		    break;
		  case 'R':
		    robotronFlag = true;
		    break;
		  case 'Z':
		    zilogFlag = true;
		    break;
		  case 'U':
		    undocFlag = true;
		    break;
		  case 'l':
		    listFlag = true;
		    break;
		  case 'o':
		    if( outFileName != null ) {
		      throwWrongCmdLine();
		    }
		    if( pos < len ) {
		      outFileName = arg.substring( pos );
		    } else {
		      if( argIdx < args.length ) {
			outFileName = args[ argIdx++ ];
		      } else {
			throwWrongCmdLine();
		      }
		    }
		    pos = len;		// Schleife verlassen
		    break;
		  default:
		    throw new IOException(
			String.format( "Unbekannte Option \'%c\'", ch ) );
		}
	      }
	    } else {
	      if( srcFileName != null ) {
		throwWrongCmdLine();
	      }
	      srcFileName = arg;
	    }
	  }
	}
      }
      if( helpFlag ) {
	EmuUtil.printlnOut();
	EmuUtil.printlnOut( Main.VERSION + " Assembler" );
	for( String s : usageLines ) {
	  EmuUtil.printlnOut( s );
	}
      } else {

	// Quelltextdatei
	if( robotronFlag && zilogFlag ) {
	  throw new IOException( "Optionen \'R\' und \'Z\'"
			+ " schlie\u00DFen sich gegenseitig aus" );
	}
	if( srcFileName == null ) {
	  throw new IOException( "Quelltextdatei nicht angegeben" );
	}
	File srcFile = new File( srcFileName );

	// Optionen auswerten
	PrgOptions options = new PrgOptions();
	if( robotronFlag ) {
	  options.setAsmSyntax( Z80Assembler.Syntax.ROBOTRON_ONLY );
	} else if( zilogFlag ) {
	  options.setAsmSyntax( Z80Assembler.Syntax.ZILOG_ONLY );
	} else {
	  options.setAsmSyntax( Z80Assembler.Syntax.ALL );
	}
	options.setLabelsCaseSensitive( caseFlag );
	options.setAllowUndocInst( undocFlag );
	options.setPrintLabels( listFlag );

	// Assembler starten
	status = assemble( srcFile, outFileName, options );
      }
    }
    catch( IOException ex ) {
      EmuUtil.printlnErr();
      EmuUtil.printlnErr( Main.VERSION + " Assembler:" );
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  EmuUtil.printlnErr( msg );
	}
      }
      for( String s : usageLines ) {
	EmuUtil.printlnErr( s );
      }
      status = false;
    }
    return status;
  }


	/* --- private Methoden --- */

  private static boolean assemble(
				File       srcFile,
				String     outFileName,
				PrgOptions options )
  {
    boolean status = false;
    try {
      File outFile = null;
      if( outFileName != null ) {
	outFile = new File( outFileName );
      } else {
	String fName = srcFile.getName();
	if( fName != null ) {
	  int pos = fName.lastIndexOf( '.' );
	  if( (pos >= 0) && (pos < fName.length()) ) {
	    fName = fName.substring( 0, pos );
	  }
	  fName += ".bin";
	} else {
	  fName = "out.bin";
	}
	File dirFile = srcFile.getParentFile();
        if( dirFile != null ) {
          outFile = new File( dirFile, fName );
        } else {
          outFile = new File( fName );
        }
      }
      if( outFile.equals( srcFile ) ) {
	throw new IOException( "Quelltext- und Ausgabedatei sind identisch" );
      }
      options.setCodeToFile( true, outFile );
      status = (new Z80Assembler(
			EmuUtil.readTextFile( srcFile ),
			srcFile.getName(),
			options,
			PrgLogger.createStandardLogger(),
			false )).assemble( null, false );
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  EmuUtil.printlnErr( msg );
	}
      }
    }
    return status;
  }


  private static void throwWrongCmdLine() throws IOException
  {
    throw new IOException( "Kommandozeile fehlerhaft" );
  }
}

