/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kommandozeilenschnittstelle des BASIC-Compilers
 */

package jkcemu.programming.basic;

import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.*;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.programming.basic.target.*;


public class CmdLineBasicCompiler
{
  private static final String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jkcemu.jar --bc [Optionen] <Datei>",
	"  java -jar jkcemu.jar --basiccompiler [Optionen] <Datei>",
	"",
	"Optionen:",
	"  -h              diese Hilfe anzeigen",
	"  -g              bei Abbruch aufgrund eines Fehlers"
						+ " Zeilennummer ausgeben",
	"  -o <Datei>      Ausgabedatei festlegen",
	"  -t <System>     Zielsystem festlegen (cpm, huebler, kc85, kramer,",
	"                  llc2_hires, scch, z1013, z9001, z9001_krt)",
	"  -A <Adresse>    Anfangsadresse festlegen (hexadezimal)",
	"  -L <Sprache>    Sprache der Laufzeitausschriften festlegen"
						+ " (de, en)",
	"  -M <Zahl>       Stack-Gr\u00F6\u00DFe festlegen"
					+ " (0: System-Stack verwenden)",
	"  -N <Name>       Programmname festlegen",
	"  -B...           Abbruchm\u00F6glichkeit festlegen",
	"  -O...           Programmcode optimieren",
	"  -S              nur Assembler-Quelltext erzeugen",
	"  -T <Zahl>       Gr\u00F6\u00DFe Zeichenkettenspeicher festlegen",
	"  -W <...>        Warnungen ein-/ausschalten",
	"",
	"Option zum Festlegen der Abbruchm\u00F6glichkeit:",
	"  -B0:            CTRL-C bricht Programm nicht ab",
	"  -B1:            CTRL-C bricht Programm nur bei Eingaben ab",
	"  -B2:            CTRL-C bricht Programm immer ab"
						+ " (Standard, langsam!)",
	"",
	"Option zur Programmcodeoptimierung (-O entspricht -O2):",
	"  -O1:            Stack-Pr\u00FCfungen ausschalten",
	"  -O2:            zus\u00E4tzlich Feldpr\u00FCfungen ausschalten",
	"  -O3:            zus\u00E4tzlich relative Spr\u00FCnge bevorzugen",
	"",
	"Option fuer Warnungen:",
	"  -W all:         alle Warnungen einschalten (Standard)",
	"  -W none:        alle Warnungen ausschalten",
	"  -W nonascii:    Bei Nicht-ASCII-Zeichen warnen",
	"  -W unused:      Bei nicht verwendeten Funktionen, Prozeduren und",
	"                  Variablen warnen",
	"" };


  public static boolean execute( String[] args, int argIdx )
  {
    boolean                  status         = false;
    boolean                  asmFlag        = false;
    boolean                  helpFlag       = false;
    boolean                  debugFlag      = false;
    int                      optimizerLevel = 0;
    String                   srcFileName    = null;
    Map<String,String>       optToArg       = new HashMap<String,String>();
    BasicOptions.BreakOption breakOption    = BasicOptions.BreakOption.ALWAYS;
    try {
      while( argIdx < args.length ) {
	String arg = args[ argIdx++ ];
	if( arg != null ) {
	  int len = arg.length();
	  if( len > 0 ) {
	    if( arg.charAt( 0 ) == '-' ) {
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
		  case 'g':
		    debugFlag = true;
		    break;
		  case 'S':
		    asmFlag = true;
		    break;
		  case 'B':
		    if( pos < len ) {
		      switch( arg.charAt( pos++ ) ) {
			case '0':
			  breakOption = BasicOptions.BreakOption.NEVER;
			  break;
			case '1':
			  breakOption = BasicOptions.BreakOption.INPUT;
			  break;
			case '2':
			  breakOption = BasicOptions.BreakOption.ALWAYS;
			  break;
			default:
			  throw new IOException(
				String.format(
				    "Option \'B%c\' nicht unterst\u00FCtzt",
				    ch ) );
		      }
		    } else {
		      throw new IOException(
				"Option \'B\' hat falsches Format" );
		    }
		    break;
		  case 'O':
		    if( pos < len ) {
		      char ch1 = arg.charAt( pos++ );
		      if( (ch1 < '0') || (ch1 > '4') ) {
			throw new IOException(
				String.format(
					"Option O%c nicht unterst\u00FCtzt",
					ch1 ) );
		      }
		      optimizerLevel = ch1 - '0';
		    } else {
		      optimizerLevel = 2;
		    }
		    break;
		  case 'o':
		  case 't':
		  case 'A':
		  case 'L':
		  case 'M':
		  case 'N':
		  case 'T':
		  case 'W':
		    String optArg = null;
		    if( pos < len ) {
		      optArg = arg.substring( pos );
		    } else {
		      while( (optArg == null) && (argIdx < args.length) ) {
			String s = args[ argIdx++ ];
			if( s != null ) {
			  if( !s.isEmpty() ) {
			    optArg = s;
			  }
			}
		      }
		    }
		    if( optArg == null ) {
		      throwWrongCmdLine();
		    }
		    String optKey = Character.toString( ch );
		    if( optToArg.containsKey( optKey ) ) {
		      throwWrongCmdLine();
		    }
		    optToArg.put( optKey, optArg );
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
	EmuUtil.printlnOut( Main.VERSION + " BASIC-Compiler" );
	for( String s : usageLines ) {
	  EmuUtil.printlnOut( s );
	}
      } else {

	// Quelltextdatei
	if( srcFileName == null ) {
	  throw new IOException( "Quelltextdatei nicht angegeben" );
	}
	File srcFile = new File( srcFileName );

	// Optionen auswerten
	BasicOptions options = new BasicOptions();

	// Zielsystem
	String sysName = optToArg.get( "t" );
	if( sysName == null ) {
	  throw new IOException( "Option \'t\' nicht angegeben" );
	}
	AbstractTarget target   = null;
	boolean        forZ9001 = false;
	if( sysName != null ) {
	  if( sysName.equalsIgnoreCase( "SCCH" ) ) {
	    target = new SCCHTarget();
	  }
	  else if( sysName.equalsIgnoreCase( "CPM" ) ) {
	    target = new CPMTarget();
	  }
	  else if( sysName.equalsIgnoreCase( "KC85" ) ) {
	    target = new KC85Target();
	  }
	  else if( sysName.equalsIgnoreCase( "HUEBLER" ) ) {
	    target = new HueblerGraphicsMCTarget();
	  }
	  else if( sysName.equalsIgnoreCase( "KRAMER" ) ) {
	    target = new KramerMCTarget();
	  }
	  else if( sysName.equalsIgnoreCase( "Z9001" ) ) {
	    target   = new Z9001Target();
	    forZ9001 = true;
	  }
	  else if( sysName.equalsIgnoreCase( "Z9001_KRT" ) ) {
	    target   = new Z9001KRTTarget();
	    forZ9001 = true;
	  }
	  else if( sysName.equalsIgnoreCase( "LLC2_HIRES" ) ) {
	    target = new LLC2HIRESTarget();
	  }
	  else if( sysName.equalsIgnoreCase( "Z1013" ) ) {
	    target = new Z1013Target();
	  }
	}
	if( target == null ) {
	  throw new IOException(
		String.format( "\'%s\': unbekanntes Zielsystem", sysName ) );
	}
	options.setTarget( target );

	// Ausgabedatei
	String outFileName = optToArg.get( "o" );

	// Sprache
	String langCode = optToArg.get( "L" );
	if( langCode != null ) {
	  if( !langCode.equalsIgnoreCase( "DE")
	      && !langCode.equalsIgnoreCase( "EN" ) )
	  {
	    throw new IOException(
			String.format(
				"\'%s\': Sprache nicht unterst\u00FCtzt",
				langCode ) );
	  }
	  options.setLangCode( langCode.toUpperCase() );
	} else {
	  options.setLangCode( "DE" );
	}

	// Anfangsadresse
	int begAddr = getHex4Arg( optToArg, "A" );
	if( begAddr >= 0 ) {
	  options.setBegAddr( begAddr );
	} else {
	  options.setBegAddr( target.getDefaultBegAddr() );
	}

	// Heap-Groesse
	int heapSize = getIntArg( optToArg, "T" );
	if( heapSize >= 0 ) {
	  if( heapSize < BasicOptions.MIN_HEAP_SIZE ) {
	    throw new IOException( "Option \'T\': Wert zu klein" );
	  }
	  if( heapSize > BasicCompiler.MAX_VALUE ) {
	    throw new IOException( "Option \'T\': Wert zu gro\u00DF" );
	  }
	  options.setHeapSize( heapSize );
	} else {
	  options.setHeapSize( BasicOptions.DEFAULT_HEAP_SIZE );
	}

	// Stack-Groesse
	int stackSize = getIntArg( optToArg, "M" );
	if( stackSize >= 0 ) {
	  if( (stackSize > 0)
	      && (stackSize < BasicOptions.MIN_STACK_SIZE) )
	  {
	    throw new IOException( "Option \'T\': Wert entweder 0 oder >= "
			+ Integer.toString( BasicOptions.MIN_STACK_SIZE ) );
	  }
	  if( heapSize > BasicCompiler.MAX_VALUE ) {
	    throw new IOException( "Option \'T\': Wert zu gro\u00DF" );
	  }
	  options.setStackSize( stackSize );
	} else {
	  options.setStackSize( BasicOptions.DEFAULT_STACK_SIZE );
	}

	// Programmname
	String appName = optToArg.get( "N" );
	if( appName == null ) {
	  String fName = srcFile.getName();
	  if( fName != null ) {
	    int pos = fName.indexOf( '.' );
	    if( pos > 0 ) {
	      appName = fName.substring( 0, pos ).toUpperCase();
	    }
	  }
	}
	if( appName != null ) {
	  options.setAppName( appName );
	}

	// Abbruchmoeglichkeit
	options.setBreakOption( breakOption );

	// Codeoptimierung
	options.setCheckStack( optimizerLevel == 0 );
	options.setCheckBounds( optimizerLevel <= 1 );
	options.setPreferRelativeJumps( optimizerLevel >= 3 );

	// Warnungen
	String warnText = optToArg.get( "W" );
	if( warnText != null ) {
	  options.setWarnNonAsciiChars( false );
	  options.setWarnUnusedItems( false );
	  if( warnText.equalsIgnoreCase( "ALL" ) ) {
	    options.setWarnNonAsciiChars( true );
	    options.setWarnUnusedItems( true );
	  } else if( warnText.equalsIgnoreCase( "NONASCII" ) ) {
	    options.setWarnNonAsciiChars( true );
	  } else if( warnText.equalsIgnoreCase( "UNUSED" ) ) {
	    options.setWarnUnusedItems( true );
	  } else if( !warnText.equalsIgnoreCase( "NONE" ) ) {
	    throw new IOException( "Option \'W\': Schl\u00FCsselwort "
				+ warnText + " nicht unterst\u00FCtzt" );
	  }
	} else {
	  options.setWarnNonAsciiChars( true );
	  options.setWarnUnusedItems( true );
	}

	// sonstige Optionen
	options.setPrintLineNumOnAbort( debugFlag );
	options.setShowAssemblerText( asmFlag );

	// Compiler starten
	status = compile(
			srcFile,
			outFileName,
			target.supportsAppName() ? appName : null,
			forZ9001,
			options,
			asmFlag );
      }
    }
    catch( IOException ex ) {
      EmuUtil.printlnErr();
      EmuUtil.printlnErr( Main.VERSION + " BASIC-Compiler:" );
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

  private static boolean compile(
				File         srcFile,
				String       outFileName,
				String       appName,
				boolean      forZ9001,
				BasicOptions options,
				boolean      suppressAssembler )
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
	} else {
	  fName = "out";
	}
	if( suppressAssembler ) {
	  fName += ".s";
	} else {
	  if( (options.getTarget() instanceof CPMTarget)
	      && (options.getBegAddr() == 0x0100) )
	  {
	    fName += "*.com";
	  } else {
	    fName += ".bin";
	  }
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
      if( suppressAssembler ) {
	options.setCodeToFile( false, null );
      } else {
	options.setCodeToFile( true, outFile );
      }
      PrgLogger     logger   = PrgLogger.createStandardLogger();
      BasicCompiler compiler = new BasicCompiler(
					EmuUtil.readTextFile( srcFile ),
					options,
					logger );
      String asmText = compiler.compile();
      if( asmText != null ) {
	if( suppressAssembler ) {
	  BufferedWriter out = null;
	  int            len = asmText.length();
	  try {
	    out = new BufferedWriter( new FileWriter( outFile ) );
	    for( int i = 0; i < len; i++ ) {
	      char ch = asmText.charAt( i );
	      if( ch == '\n' ) {
		out.newLine();
	      } else {
		out.write( ch );
	      }
	    }
	    out.close();
	    out    = null;
	    status = true;
	  }
	  finally {
	    EmuUtil.doClose( out );
	  }
	} else {
	  Z80Assembler assembler = new Z80Assembler(
						asmText,
						"Assembler-Quelltext",
						options,
						logger,
						false );
	  status = assembler.assemble( appName, forZ9001 );
	  if( assembler.getRelJumpsTooLong() ) {
	    EmuUtil.printlnErr( "Compilieren Sie bitte mit einer"
			+ " niedrigeren Optimierungsstufe (max. \'-O3\')." );
	  }
	}
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  EmuUtil.printlnErr( msg );
	}
      }
      status = false;
    }
    return status;
  }


  private static int getHex4Arg(
			Map<String,String> optToArg,
			String             opt ) throws IOException
  {
    int    rv  = -1;
    String arg = optToArg.get( opt );
    if( arg != null ) {
      try {
	int len = arg.length();
	if( (len > 1) && (arg.endsWith( "H" ) || arg.endsWith( "h" )) ) {
	  arg = arg.substring( 0, len - 1 );
	}
	rv = Integer.parseInt( arg, 16 );
	if( (rv & ~0xFFFF) != 0 ) {
	  throw new IOException(
		String.format(
			"Option \'%c\': %s Hexadezimalzahl zu gro\u00DF",
			opt,
			arg ) );
	}
      }
      catch( NumberFormatException ex ) {
	throw new IOException(
		String.format(
			"Option \'%c\': %s Ung\u00FCltige Hexadezimalzahl",
			opt,
			arg ) );
      }
    }
    return rv;
  }


  private static int getIntArg(
			Map<String,String> optToArg,
			String             opt ) throws IOException
  {
    int    rv  = -1;
    String arg = optToArg.get( opt );
    if( arg != null ) {
      try {
	rv = Integer.parseInt( arg );
      }
      catch( NumberFormatException ex ) {
	throw new IOException(
		String.format(
			"Option \'%c\': %s: Ung\u00FCltige Zahl",
			opt,
			arg ) );
      }
    }
    return rv;
  }


  private static void throwWrongCmdLine() throws IOException
  {
    throw new IOException( "Kommandozeile fehlerhaft" );
  }
}
