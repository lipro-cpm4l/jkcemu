/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kommandozeilenschnittstelle des Assemblers
 */

package jkcemu.programming.assembler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.programming.CmdLineArgIterator;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgLogger;
import jkcemu.programming.PrgOptions;


public class CmdLineAssembler
{
  private static final String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jkcemu.jar --as [Optionen] <Datei>",
	"  java -jar jkcemu.jar --assembler [Optionen] <Datei>",
	"",
	"Optionen:",
	"  -h              diese Hilfe anzeigen",
	"  -f <Datei>      Kommandozeile aus Datei lesen",
	"  -l              Markentabelle ausgeben",
	"  -o <Datei>      Ausgabedatei festlegen",
	"  -9              Ausgabedatei f\u00FCr Z9001, KC85/1 und KC87"
							+ " erzeugen",
	"  -C              Gro\u00DF-/Kleinschreibung bei Marken beachten",
	"  -D <Marke>      Marke mit dem Wert -1 (alle Bits gesetzt)"
							+ " definieren",
	"  -D <Marke=Wert> Marke mit angegebenen Wert definieren",
	"  -U              undokumentierte Befehle erlauben",
	"  -R              nur Robotron-Syntax erlauben",
	"  -Z              nur Zilog-Syntax erlauben",
	"" };


  public static boolean execute( String[] args, int argIdx )
  {
    java.util.List<Map.Entry<String,Integer>> labels = new ArrayList<>();

    boolean status       = false;
    boolean caseFlag     = false;
    boolean helpFlag     = false;
    boolean robotronFlag = false;
    boolean zilogFlag    = false;
    boolean undocFlag    = false;
    boolean listFlag     = false;
    boolean forZ9001     = false;
    String  outFileName  = null;
    String  srcFileName  = null;

    CmdLineArgIterator backIter = null;
    CmdLineArgIterator iter     = CmdLineArgIterator.createFromStringArray(
								args,
								argIdx );
    try {
      String arg = iter.next();
      while( arg != null ) {
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
		case 'f':
		  {
		    if( backIter != null ) {
		      throw new IOException(
			"Option -f in der Datei nicht erlaubt" );
		    }
		    String fileName = null;
		    if( pos < len ) {
		      fileName = arg.substring( pos );
		      pos      = len;		// Schleife verlassen
		    } else {
		      fileName = iter.next();
		    }
		    if( fileName == null ) {
		      throwWrongCmdLine();
		    }
		    try {
		      backIter = iter;
		      iter     = CmdLineArgIterator.createFromReader(
					new FileReader( fileName ) );
		    }
		    catch( IOException ex ) {
		      iter     = backIter;
		      backIter = null;
		    }
		  }
		  break;
		case 'h':
		case 'H':
		  helpFlag = true;
		  break;
		case 'C':
		  caseFlag = true;
		  break;
		case 'D':
		  {
		    String labelText = null;
		    if( pos < len ) {
		      labelText = arg.substring( pos );
		      pos       = len;		// Schleife verlassen
		    } else {
		      labelText = iter.next();
		    }
		    parseLabel( labels, labelText );
		  }
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
		    pos         = len;		// Schleife verlassen
		  } else {
		    outFileName = iter.next();
		  }
		  if( outFileName == null ) {
		    throwWrongCmdLine();
		  }
		  break;
		case '9':
		  forZ9001 = true;
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
	arg = iter.next();
	if( (arg == null) && (backIter != null) ) {
	  EmuUtil.closeSilent( iter );
	  iter     = backIter;
	  backIter = null;
	  arg      = iter.next();
	}
      }
      if( helpFlag ) {
	EmuUtil.printlnOut();
	EmuUtil.printlnOut( Main.APPINFO + " Assembler" );
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
	status = assemble(
			srcFile,
			outFileName,
			forZ9001,
			labels,
			options );
      }
    }
    catch( IOException ex ) {
      EmuUtil.printlnErr();
      EmuUtil.printlnErr( Main.APPINFO + " Assembler:" );
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
    finally {
      EmuUtil.closeSilent( iter );
    }
    return status;
  }


	/* --- private Methoden --- */

  private static boolean assemble(
		File                                      srcFile,
		String                                    outFileName,
		boolean                                   forZ9001,
		java.util.List<Map.Entry<String,Integer>> labels,
		PrgOptions                                options )
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
      Z80Assembler asm = new Z80Assembler(
					null,
					null,
					srcFile,
					options,
					PrgLogger.createStandardLogger(),
					false );
      for( Map.Entry<String,Integer> label : labels ) {
	String  s = label.getKey();
	Integer v = label.getValue();
	if( !asm.addLabel( s, v != null ? v.intValue() : -1 ) ) {
	  throw new IOException( "Marke " + s + " bereits vorhanden" );
	}
      }
      status = asm.assemble( null, forZ9001 );
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


  private static void parseLabel(
	java.util.List<Map.Entry<String,Integer>> labels,
	String                                    text ) throws IOException
  {
    if( text == null ) {
      throwWrongCmdLine();
    }
    String labelName = text;
    String valueText = null;
    int    pos       = text.indexOf( '=' );
    if( pos >= 0 ) {
      labelName = text.substring( 0, pos );
      valueText = text.substring( pos + 1 );
    }
    if( labelName.isEmpty() ) {
      throwWrongCmdLine();
    }
    boolean status = AsmLabel.isIdentifierStart( text.charAt( 0 ) );
    if( status ) {
      int len = labelName.length();
      if( len > 1 ) {
	for( int i = 1; i < len; i++ ) {
	  if( !AsmLabel.isIdentifierPart( text.charAt( i ) ) ) {
	    status = false;
	    break;
	  }
	}
      }
    }
    if( !status ) {
      throw new IOException(
	labelName + ": Marke enth\u00E4lht ung\u00FCltige Zeichen" );
    }
    int labelValue = -1;
    if( valueText != null ) {
      CharacterIterator iter = new StringCharacterIterator( valueText );
      if( iter.first() == CharacterIterator.DONE ) {
	throw new IOException(
		"Marke " + labelName + ": Wert fehlt" );
      }
      try {
	labelValue = ExprParser.parseNumber( iter );
      }
      catch( PrgException ex ) {
	throw new IOException(
		"Marke " + labelName + ": " + ex.getMessage() );
      }
      if( ExprParser.skipSpaces( iter ) != CharacterIterator.DONE ) {
	throw new IOException( "Ung\u00FCltige Zahl bei Marke " + labelName );
      }
    }
    labels.add( new AbstractMap.SimpleImmutableEntry<>(
						labelName,
						labelValue ) );
  }


  private static void throwWrongCmdLine() throws IOException
  {
    throw new IOException( "Kommandozeile fehlerhaft" );
  }
}
