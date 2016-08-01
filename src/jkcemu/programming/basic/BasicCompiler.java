/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jkcemu.base.*;
import jkcemu.programming.*;


public class BasicCompiler
{
  public static final int    DATA_INT1         = 0x01;
  public static final int    DATA_INT2         = 0x02;
  public static final int    DATA_STRING       = 0x11;
  public static final int    MAGIC_GOSUB       = 'G';
  public static final int    MAX_INT_VALUE     = 32767;
  public static final int    MAX_STR_LEN       = 255;
  public static final String ERR_POS_TEXT      = " ??? ";
  public static final String DATA_LABEL_PREFIX = "D_";
  public static final String LINE_LABEL_PREFIX = "L_";
  public static final String START_LABEL       = "MSTART";
  public static final String TOP_LABEL         = "MTOP";

  public static enum DataType { INTEGER, STRING };
  public static enum IODriver { CRT, LPT, FILE, VDIP, ALL };

  /*
   * sortierte Liste der reservierten Schluesselwoeter
   * ohne zielsystemabhaengigen Konstanten
   */
  private static final String[] sortedReservedWords = {
	"ABS", "ADD", "AND", "APPEND", "AS", "ASC", "ASM", "AT",
	"BIN$", "BINARY", "BORDER", "BSS",
	"CALL", "CASE", "CHR$", "CIRCLE", "CLOSE", "CLS",
	"CODE", "COLOR", "CURSOR",
	"DATA", "DECLARE", "DEEK", "DEF", "DEFUSR",
	"DEFUSR0", "DEFUSR1", "DEFUSR2", "DEFUSR3", "DEFUSR4",
	"DEFUSR5", "DEFUSR6", "DEFUSR7", "DEFUSR8", "DEFUSR9",
	"DIM", "DO", "DOKE", "DRAW", "DRAWR",
	"ELSE", "ELSEIF", "END", "ENDIF", "EOF", "ERR", "ERR$", "EXIT",
	"E_CHANNEL_ALREADY_OPEN", "E_CHANNEL_CLOSED",
	"E_DEVICE_LOCKED", "E_DEVICE_NOT_FOUND",
	"E_DISK_FULL",
	"E_EOF", "E_ERROR", "E_FILE_NOT_FOUND", "E_INVALID",
	"E_IO_ERROR", "E_IO_MODE", "E_NO_DISK", "E_OK", "E_OVERFLOW",
	"E_PATH_NOT_FOUND",
	"E_READ_ONLY", "E_SOCKET_STATUS", "E_UNKNOWN_HOST",
	"FALSE", "FOR", "FUNCTION",
	"GOSUB", "GOTO",
	"HEX$", "HIBYTE", "H_CHAR", "H_PIXEL",
	"IF", "IN", "INCLUDE", "INK", "INKEY$",
	"INP", "INPUT", "INPUT$", "INSTR", "JOYST", "JOYST_BUTTONS",
	"LABEL", "LCASE$", "LEFT$", "LEN", "LET", "LINE",
	"LOBYTE", "LOCAL", "LOCATE", "LOOP", "LOWER$",
	"LPRINT", "LTRIM$",
	"MAX", "MEMSTR$", "MID$", "MIN", "MIRROR$",
	"MOD", "MOVE", "MOVER",
	"NEXT", "NOT",
	"ON", "OPEN", "OR", "OUT", "OUTPUT",
	"PAINT", "PAPER", "PASSWORD", "PAUSE", "PEEK",
	"PEN", "PEN_NONE", "PEN_NORMAL", "PEN_RUBBER", "PEN_XOR",
	"PLOT", "PLOTR", "POINT", "POKE",
	"PRESET", "PRINT", "PSET", "PTEST",
	"READ", "REM", "RESTORE", "RETURN", "RIGHT$", "RND", "RTRIM$",
	"SCREEN", "SELECT", "SGN", "SHL", "SHR", "SPACE$",
	"SQR", "STEP", "STOP", "STR$", "STRING$", "SUB",
	"TEXT", "THEN", "TO", "TOP", "TRIM$", "TRUE",
	"UCASE$", "UNTIL", "UPPER$", "USING", "USR",
	"USR0", "USR1", "USR2", "USR3", "USR4",
	"USR5", "USR6", "USR7", "USR8", "USR9",
	"VAL", "VDIP",
	"WAIT", "WEND", "WHILE", "WRITE", "W_CHAR", "W_PIXEL",
	"XOR", "XPOS", "YPOS" };

  private PrgSource                  curSource;
  private PrgSource                  mainSource;
  private AbstractTarget             target;
  private BasicOptions               options;
  private PrgLogger                  logger;
  private Map<IODriver,Set<Integer>> ioDriverModes;
  private Set<BasicLibrary.LibItem>  libItems;
  private SortedSet<String>          basicLines;
  private Collection<BasicLineExpr>  destBasicLines;
  private Set<String>                dataBasicLines;
  private Collection<BasicLineExpr>  restoreBasicLines;
  private Stack<BasicSourcePos>      structureStack;
  private Map<String,CallableEntry>  name2Callable;
  private Map<String,VarDecl>        name2GlobalVar;
  private Map<String,String>         str2Label;
  private SortedSet<String>          usrLabels;
  private AsmCodeBuf                 asmOut;
  private AsmCodeBuf                 dataOut;
  private StringBuilder              userData;
  private StringBuilder              userBSS;
  private boolean                    gcRequired;
  private boolean                    execEnabled;
  private boolean                    mainPrg;
  private boolean                    separatorChecked;
  private boolean                    tmpStrBufUsed;
  private int                        errCnt;
  private int                        labelNum;
  private int                        codeCreationDisabledLevel;
  private int                        codePosAtBegOfSrcLine;
  private long                       curBasicLineNum;
  private long                       lastBasicLineNum;
  private String                     lastBasicLineExpr;


  public BasicCompiler(
		String       srcText,
		File         srcFile,
		BasicOptions options,
		PrgLogger    logger )
  {
    this.mainSource                = null;
    this.target                    = options.getTarget();
    this.options                   = options;
    this.logger                    = logger;
    this.ioDriverModes             = new HashMap<>();
    this.libItems                  = new HashSet<>();
    this.basicLines                = new TreeSet<>();
    this.destBasicLines            = new ArrayList<>( 1024 );
    this.dataBasicLines            = null;
    this.restoreBasicLines         = null;
    this.structureStack            = new Stack<>();
    this.name2Callable             = new HashMap<>();
    this.str2Label                 = new HashMap<>();
    this.name2GlobalVar            = new HashMap<>();
    this.usrLabels                 = new TreeSet<>();
    this.asmOut                    = new AsmCodeBuf( 0x8000 );
    this.dataOut                   = null;
    this.userData                  = null;
    this.userBSS                   = null;
    this.execEnabled               = true;
    this.mainPrg                   = true;
    this.gcRequired                = false;
    this.separatorChecked          = false;
    this.tmpStrBufUsed             = false;
    this.codeCreationDisabledLevel = 0;
    this.errCnt                    = 0;
    this.labelNum                  = 1;
    this.curBasicLineNum           = -1;
    this.lastBasicLineNum          = -1;
    this.lastBasicLineExpr         = null;

    // Quelltext oeffnen
    if( srcText != null ) {
      this.mainSource = PrgSource.readText( srcText, null, srcFile );
    } else {
      if( srcFile != null ) {
	try {
	  this.mainSource = PrgSource.readFile( srcFile );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    msg = msg.trim();
	    if( msg.isEmpty() ) {
	      msg = null;
	    }
	  }
	  if( msg == null ) {
	    msg = "Datei kann nicht ge\u00F6ffnet werden";
	  }
	  appendToErrLog( srcFile.getPath() + ": " + msg );
	}
      }
    }
    this.curSource = this.mainSource;
  }


  public void addLibItem( BasicLibrary.LibItem libItem )
  {
    if( this.asmOut.isEnabled() )
      this.libItems.add( libItem );
  }


  public void cancel()
  {
    this.execEnabled = false;
  }


  /*
   * Die Methode prueft, ob der uebergebene Name eine Variable ist
   * bzw. als einfache Variable gewertet werden kann (implizite Deklaration).
   * Wenn ja, wird die Variable vollstaendig geparst (evtl. Indexe)
   * und ein VarInfo-Objekt zurueckgeliefert.
   * Ist in dem Objekt das Attribute "addrExpr" gefuellt,
   * bescheibt dieses die Adresse der Variable.
   * Im dem Fall wurde keine Code erzeugt.
   * Ist dagegen "addrExpr" null, wurde Code erzeugt,
   * der im HL-Register die Adresse der Variable enthaelt.
   */
  public SimpleVarInfo checkVariable(
			CharacterIterator iter,
			String            varName ) throws PrgException
  {
    SimpleVarInfo varInfo = null;
    if( varName != null ) {
      if( !varName.isEmpty() ) {
	if( !isReservedWord( varName ) ) {
	  boolean done     = false;
	  String  addrExpr = null;
	  Integer iyOffs   = null;

	  // auf lokale Variable prufen
	  CallableEntry callableEntry = getEnclosingCallableEntry();
	  if( callableEntry != null ) {
	    if( !varName.equals( callableEntry.getName() ) ) {
	      iyOffs = callableEntry.getIYOffs( varName );
	      if( iyOffs != null ) {
		callableEntry.setVarUsed( varName );
		done = true;
	      }
	    }
	  }

	  /*
	   * Wenn es keine benutzerdefinierte Funktion/Prozedur ist,
	   * muss es eine globale Variable sein.
	   */
	  if( !done && !this.name2Callable.containsKey( varName ) ) {
	    VarDecl varDecl  = this.name2GlobalVar.get( varName );
	    if( varDecl != null ) {
	      varDecl.setUsed();
	      if( varDecl.getDimCount() > 0 ) {
		BasicUtil.parseToken( iter, '(' );
		if( varDecl.getDimCount() == 1 ) {
		  int pos = this.asmOut.length();
		  BasicExprParser.parseExpr( this, iter );
		  Integer idx = BasicUtil.removeLastCodeIfConstExpr(
								this, pos );
		  if( idx != null ) {
		    if( (idx.intValue() < 0) || (idx > varDecl.getDim1()) ) {
		      BasicUtil.throwIndexOutOfRange();
		    }
		    if( idx.intValue() > 0 ) {
		      if( idx.intValue() >= 0xA000 ) {
			addrExpr = String.format(
					"%s+0%04XH",
					varDecl.getLabel(),
					idx.intValue() * 2 );
		      } else {
			addrExpr = String.format(
					"%s+%04XH",
					varDecl.getLabel(),
					idx.intValue() * 2 );
		      }
		    } else {
		      addrExpr = varDecl.getLabel();
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim1() + 1 );
		      this.asmOut.append_LD_DE_xx( varDecl.getLabel() );
		      this.asmOut.append( "\tCALL\tARYADR\n" );
		      addLibItem( BasicLibrary.LibItem.ARYADR );
		    } else {
		      this.asmOut.append( "\tADD\tHL,HL\n" );
		      this.asmOut.append_LD_DE_xx( varDecl.getLabel() );
		      this.asmOut.append( "\tADD\tHL,DE\n" );
		    }
		  }
		} else if( varDecl.getDimCount() == 2 ) {
		  int pos = this.asmOut.length();
		  BasicExprParser.parseExpr( this, iter );
		  Integer idx1 = BasicUtil.removeLastCodeIfConstExpr(
								this, pos );
		  if( idx1 != null ) {
		    if( (idx1.intValue() < 0)
			|| (idx1.intValue() > varDecl.getDim1()) )
		    {
		      BasicUtil.throwIndexOutOfRange();
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim1() + 1 );
		      this.asmOut.append( "\tCALL\tCKIDX\n" );
		      addLibItem( BasicLibrary.LibItem.CKIDX );
		    }
		  }
		  BasicUtil.parseToken( iter, ',' );
		  pos = this.asmOut.length();
		  BasicExprParser.parseExpr( this, iter );
		  Integer idx2 = BasicUtil.removeLastCodeIfConstExpr(
								this, pos );
		  if( idx2 != null ) {
		    if( (idx2.intValue() < 0)
			|| (idx2.intValue() > varDecl.getDim2()) )
		    {
		      BasicUtil.throwIndexOutOfRange();
		    }
		    if( idx1 != null ) {
		      this.asmOut.append( "\tLD\tHL," );
		      this.asmOut.append( varDecl.getLabel() );
		      int offs = ((idx1.intValue() * (varDecl.getDim2() + 1))
						+ idx2.intValue()) * 2;
		      if( offs > 0 ) {
			this.asmOut.append( '+' );
			this.asmOut.appendHex4( offs );
		      }
		      this.asmOut.newLine();
		    } else {
		      if( varDecl.getDim2() == 1 ) {
			this.asmOut.append( "\tADD\tHL,HL\n" );
		      } else {
			this.asmOut.append_LD_DE_nn( varDecl.getDim2() + 1 );
			this.asmOut.append( "\tCALL\tO_MUL\n" );
			addLibItem( BasicLibrary.LibItem.O_MUL );
		      }
		      if( idx2.intValue() == 1 ) {
			this.asmOut.append( "\tINC\tHL\n" );
		      } else if( idx2.intValue() > 1 ) {
			this.asmOut.append_LD_DE_nn( idx2.intValue() );
			this.asmOut.append( "\tADD\tHL,DE\n" );
		      }
		      this.asmOut.append( "\tADD\tHL,HL\n"
					  + "\tLD\tDE," );
		      this.asmOut.append( varDecl.getLabel() );
		      this.asmOut.append( "\n"
					  + "\tADD\tHL,DE\n" );
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim2() + 1 );
		      this.asmOut.append( "\tCALL\tCKIDX\n" );
		      addLibItem( BasicLibrary.LibItem.CKIDX );
		    }
		    if( idx1 != null ) {
		      this.asmOut.append_LD_DE_nn(
				  idx1.intValue() * (varDecl.getDim2() + 1) );
		    } else {
		      if( varDecl.getDim2() == 1 ) {
			this.asmOut.insert(
				pos,
				"\tADD\tHL,HL\n"
					+ "\tPUSH\tHL\n" );
		      } else {
			this.asmOut.insert(
				pos,
				String.format(
					"\tLD\tDE,%04XH\n"
						+ "\tCALL\tO_MUL\n"
						+ "\tPUSH\tHL\n",
				varDecl.getDim2() + 1 ) );
			addLibItem( BasicLibrary.LibItem.O_MUL );
		      }
		      this.asmOut.append( "\tPOP\tDE\n" );
		    }
		    this.asmOut.append( "\tADD\tHL,DE\n"
					+ "\tADD\tHL,HL\n"
					+ "\tLD\tDE," );
		    this.asmOut.append( varDecl.getLabel() );
		    this.asmOut.append( "\n"
					+ "\tADD\tHL,DE\n" );
		  }
		}
		BasicUtil.parseToken( iter, ')' );
	      } else {
		addrExpr = varDecl.getLabel();
	      }
	    } else {
	      if( callableEntry != null ) {
		throw new PrgException( "Implizite Variablendeklaration"
			+ " in einer Funktion/Prozedur nicht erlaubt" );
	      }
	      // implizite Variablendeklaration, aber nur im Hauptprogramm
	      varDecl = new VarDecl(
				this.curSource,
				this.curBasicLineNum,
				varName );
	      varDecl.setUsed();
	      this.name2GlobalVar.put( varName, varDecl );
	      addrExpr = varDecl.getLabel();
	    }
	    done = true;
	  }
	  if( done ) {
	    if( varName.endsWith( "$" ) ) {
	      varInfo = new SimpleVarInfo(
					DataType.STRING,
					addrExpr,
					iyOffs );
	    } else {
	      varInfo = new SimpleVarInfo(
					DataType.INTEGER,
					addrExpr,
					iyOffs );
	    }
	  }
	}
      }
    }
    return varInfo;
  }


  public String compile() throws IOException
  {
    String asmText   = null;
    this.execEnabled = true;
    this.errCnt      = 0;
    setCodeCreationDisabledLevel( 0 );
    this.target.reset();
    try {
      if( this.options.getShowAssemblerText() ) {
	this.asmOut.append( "\n;Eigentliches Programm\n" );
      }
      parseSourceText();
      if( this.execEnabled ) {
	AsmCodeOptimizer.optimize1( this );
	this.target.preAppendLibraryCode( this );
	BasicLibrary.appendCodeTo( this );

	// Initialisierungen
	AsmCodeBuf initBuf    = new AsmCodeBuf( 0x0400 );
	int        bssBegAddr = this.options.getBssBegAddr();
	if( (bssBegAddr < 0)
	    || (bssBegAddr > this.options.getCodeBegAddr()) )
	{
	  appendHeadCommentTo( initBuf );
	}
	initBuf.append( "\n"
			+ "\tORG\t" );
	initBuf.appendHex4( this.options.getCodeBegAddr() );
	initBuf.newLine();
	this.target.appendPrologTo(
				initBuf,
				this,
				this.options.getAppName() );
	BasicLibrary.appendInitTo(
				initBuf,
				this,
				this.name2GlobalVar,
				this.usrLabels );
	initBuf.append( this.asmOut );
	this.asmOut = initBuf;
	AsmCodeOptimizer.optimize2( this );
	if( libItems.contains( BasicLibrary.LibItem.DATA ) ) {
	  if( this.options.getShowAssemblerText() ) {
	    this.asmOut.append( "\n;DATA-Zeilen\n" );
	  }
	  this.asmOut.append( "DBEG:\n" );
	  if( this.dataOut != null ) {
	    this.asmOut.append( this.dataOut );
	    this.asmOut.append( "\tDB\t00H\n" );
	  }
	}
	BasicLibrary.appendDataTo(
			this,
			this.str2Label,
			this.userData );
	AsmCodeBuf bssBuf = this.asmOut;
	if( bssBegAddr >= 0 ) {
	  if( bssBegAddr < this.options.getCodeBegAddr() ) {
	    bssBuf = new AsmCodeBuf( 0x0400 );
	    appendHeadCommentTo( bssBuf );
	  }
	  bssBuf.append( "\n"
			+ "\tORG\t" );
	  bssBuf.appendHex4( bssBegAddr );
	  bssBuf.append( "\n" );
	}
	BasicLibrary.appendBssTo(
			bssBuf,
			this,
			this.name2GlobalVar,
			this.usrLabels,
			this.userBSS );
	if( bssBuf != this.asmOut ) {
	  bssBuf.append( "\n" );
	  bssBuf.append( this.asmOut );
	  this.asmOut = bssBuf;
	}
	if( this.errCnt == 0 ) {
	  asmText = this.asmOut.toString();
	}
      }
    }
    catch( TooManyErrorsException ex ) {
      appendToErrLog( "\nAbgebrochen aufgrund zu vieler Fehler\n" );
    }
    if( this.execEnabled && (this.errCnt > 0) ) {
      appendToErrLog( String.format( "%d Fehler\n", this.errCnt ) );
    }
    return asmText;
  }


  public SortedSet<String> getAllLineLabels()
  {
    return this.basicLines;
  }


  public BasicOptions getBasicOptions()
  {
    return this.options;
  }


  public CallableEntry getCallableEntry( String name )
  {
    return name != null ? this.name2Callable.get( name ) : null;
  }


  public AsmCodeBuf getCodeBuf()
  {
    return this.asmOut;
  }


  /*
   * Ermittlung der umgebenden Funktion/Prozedur
   *
   * Diese kann nur der unterste Eintrag auf dem Stack sein.
   */
  public CallableEntry getEnclosingCallableEntry()
  {
    CallableEntry rv = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.get( 0 );
      if( entry instanceof CallableEntry ) {
	rv = (CallableEntry) entry;
      }
    }
    return rv;
  }


  public int getIOChannelSize()
  {
    int rv = BasicLibrary.IOCTB_DRIVER_OFFS;
    if( usesLibItem( BasicLibrary.LibItem.IO_FILE_HANDLER ) ) {
      rv = Math.max( rv, this.target.getFileIOChannelSize() );
    }
    return rv;
  }


  /*
   * Die Methode gibt Auskunft, welche Betriebsarten
   * ein konkreter Treiber zur Verfuegung stellen muss.
   * Dabei muessen auch die Betriebsarten zurueckgeliefert werden
   * waehrend der Compilier-Zeit keinem konkreten Treiber
   * zugeorndet werden koennen.
   */
  public Set<Integer> getIODriverModes( IODriver driver )
  {
    Set<Integer> rv = new TreeSet<>();
    if( driver != null ) {
      Set<Integer> modes1 = this.ioDriverModes.get( driver );
      if( modes1 != null ) {
	rv.addAll( modes1 );
      }
      if( driver != IODriver.ALL ) {
	Set<Integer> modes2 = this.ioDriverModes.get( IODriver.ALL );
	if( modes2 != null ) {
	  rv.addAll( modes2 );
	}
      }
    }
    return rv;
  }


  public Set<BasicLibrary.LibItem> getLibItems()
  {
    return this.libItems;
  }


  public String getStringLiteralLabel( String text )
  {
    String label = this.str2Label.get( text );
    if( label == null ) {
      label = nextLabel();
      this.str2Label.put( text, label );
    }
    return label;
  }


  public AbstractTarget getTarget()
  {
    return this.target;
  }


  public SortedSet<String> getUsedLineLabels()
  {
    SortedSet<String> rv = new TreeSet<>();
    if( this.destBasicLines != null ) {
      for( BasicLineExpr lineExpr : this.destBasicLines ) {
	rv.add( lineExpr.getExprText().toUpperCase() );
      }
    }
    if( this.restoreBasicLines != null ) {
      for( BasicLineExpr lineExpr : this.restoreBasicLines ) {
	rv.add( lineExpr.getExprText().toUpperCase() );
      }
    }
    return rv;
  }


  public String getUsrLabel( int usrNum )
  {
    String rv = String.format( "M_USR%d", usrNum );
    this.usrLabels.add( rv );
    return rv;
  }


  public boolean isLangCode( String langCode )
  {
    boolean rv = false;
    if( langCode != null ) {
      String curLangCode = this.options.getLangCode();
      if( curLangCode != null ) {
	rv = langCode.equalsIgnoreCase( curLangCode );
      }
    }
    return rv;
  }


  public boolean needsDriver( IODriver driver )
  {
    return this.ioDriverModes.containsKey( driver )
		|| this.ioDriverModes.containsKey( IODriver.ALL );
  }


  public void lockTmpStrBuf() throws PrgException
  {
    if( this.tmpStrBufUsed ) {
      throw new PrgException( "String-Funktion hier nicht erlaubt,"
		+ " da der interne String-Puffer bereits durch"
		+ " eine andere String-Funktion belegt ist\n"
		+ "Weisen Sie bitte den String-Ausdruck einer Variablen zu"
		+ " und verwenden Sie diese hier." );
    }
    this.tmpStrBufUsed = true;
  }


  public String nextLabel()
  {
    return String.format( "M%d", this.labelNum++ );
  }


  public void parseASM(
		CharacterIterator iter,
		boolean           isFunc ) throws PrgException
  {
    if( isFunc ) {
      BasicUtil.parseToken( iter, '(' );
    }
    do {
      boolean data = false;
      boolean bss  = false;
      if( !isFunc && !BasicUtil.checkKeyword( iter, "CODE" ) ) {
	if( BasicUtil.checkKeyword( iter, "DATA" ) ) {
	  data = true;
	} else if( BasicUtil.checkKeyword( iter, "BSS" ) ) {
	  bss = true;
	}
      }
      String text = parseStringLiteral( iter );
      if( data ) {
	if( this.userData == null ) {
	  this.userData = new StringBuilder( 0x0800 );
	}
	this.userData.append( text );
	this.userData.append( (char) '\n' );
      } else if( bss ) {
	if( this.userBSS == null ) {
	  this.userBSS = new StringBuilder( 0x0800 );
	}
	this.userBSS.append( text );
	this.userBSS.append( (char) '\n' );
      } else {
	this.asmOut.append( text );
	this.asmOut.newLine();
      }
    } while( BasicUtil.checkToken( iter, ',' ) );
    if( isFunc ) {
      BasicUtil.parseToken( iter, ')' );
    }
  }


  public void parseCallableCall(
			CharacterIterator iter,
			CallableEntry     entry ) throws PrgException
  {
    if( this.options.getCheckStack()
	&& (this.options.getStackSize() > 0) )
    {
      this.asmOut.append_LD_DE_nn( entry.getTotalArgSize()
				+ entry.getTotalVarSize() );
      this.asmOut.append( "\tCALL\tCKSTKN\n" );
      addLibItem( BasicLibrary.LibItem.CKSTK );
    }
    int nArgs = entry.getArgCount();
    if( nArgs > 0 ) {
      BasicUtil.parseToken( iter, '(' );
      for( int i = 0; i < nArgs; i++ ) {
	if( i > 0 ) {
	  BasicUtil.parseToken( iter, ',' );
	}
	if( entry.getArgType( i ) == DataType.STRING ) {
	  String text = BasicUtil.checkStringLiteral( this, iter );
	  if( text != null ) {
	    this.asmOut.append( "\tLD\tHL," );
	    this.asmOut.append( getStringLiteralLabel( text ) );
	    this.asmOut.append( "\n"
				+ "\tPUSH\tHL\n" );
	  } else {
	    SimpleVarInfo srcVar = checkVariable( iter );
	    if( srcVar != null ) {
	      srcVar.ensureValueInDE( this.asmOut );
	      this.asmOut.append( "\tCALL\tSVDUP\n"
				+ "\tPUSH\tDE\n" );
	      addLibItem( BasicLibrary.LibItem.SVDUP );
	    } else {
	      BasicExprParser.parseStringPrimExpr( this, iter );
	      this.asmOut.append( "\tCALL\tSMACP\n"
				+ "\tPUSH\tHL\n" );
	      addLibItem( BasicLibrary.LibItem.SMACP );
	    }
	  }
	} else {
	  BasicExprParser.parseExpr( this, iter );
	  this.asmOut.append( "\tPUSH\tHL\n" );
	}
      }
      BasicUtil.parseToken( iter, ')' );
    } else {
      if( BasicUtil.checkToken( iter, '(' ) ) {
	BasicUtil.parseToken( iter, ')' );
      }
    }
    this.asmOut.append( "\tCALL\t" );
    this.asmOut.append( entry.getLabel() );
    this.asmOut.newLine();
    if( nArgs > 0 ) {
      if( nArgs > 5 ) {
	this.asmOut.append_LD_HL_nn( -nArgs );
	this.asmOut.append( "\tADD\tHL,SP\n"
			+ "\tLD\tSP,HL\n" );
      } else {
	for( int i = 0; i < nArgs; i++ ) {
	  this.asmOut.append( "\tPOP\tHL\n" );
	}
      }
    }
    if( entry instanceof FunctionEntry ) {
      this.asmOut.append( "\tLD\tHL,(M_FRET)\n" );
      addLibItem( BasicLibrary.LibItem.M_FRET );
      if( ((FunctionEntry) entry).getReturnType() == DataType.STRING ) {
	this.gcRequired = true;
      }
    }
    entry.putCallPos( this.curSource, this.curBasicLineNum );
  }


  /*
   * Die Methode parst eine Kanalnummer.
   * Die Anfangsadresse des Kanalzeigerfeldes steht dann in HL.
   * Das Doppelkreuz ist zu dem Zeitpunkt bereits geparst.
   */
  public void parseIOChannelNumToPtrFldAddrInHL(
			CharacterIterator iter,
			AtomicBoolean     constOut ) throws PrgException
  {
    Integer channel = BasicUtil.readNumber( iter );
    if( channel != null ) {
      switch( channel.intValue() ) {
	case 1:
	  this.asmOut.append( "\tLD\tHL,IOCTB1\n" );
	  addLibItem( BasicLibrary.LibItem.IOCTB1 );
	  break;
	case 2:
	  this.asmOut.append( "\tLD\tHL,IOCTB2\n" );
	  addLibItem( BasicLibrary.LibItem.IOCTB2 );
	  break;
	default:
	  throwIOChannelNumOutOfRange();
      }
      if( constOut != null ) {
	constOut.set( true );
      }
    } else {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tIOCADR\n" );
      addLibItem( BasicLibrary.LibItem.IOCADR );
      addLibItem( BasicLibrary.LibItem.IOCTB1 );
      addLibItem( BasicLibrary.LibItem.IOCTB2 );
      if( constOut != null ) {
	constOut.set( false );
      }
    }
  }


  public void putWarningNonAsciiChar( char ch )
  {
    
    putWarning(
	String.format(
		"\'%c\': kein ASCII-Zeichen,"
			+ " kann im Zielsystem ein anderes Zeichen sein",
		ch ) );
  }


  public void putWarningOutOfRange()
  {
    putWarning( "Wert au\u00DFerhalb des Wertebereiches" );
  }


  public boolean usesErrorVars()
  {
    return this.libItems.contains( BasicLibrary.LibItem.M_ERN )
		|| this.libItems.contains( BasicLibrary.LibItem.M_ERT );
  }


  public boolean usesLibItem( BasicLibrary.LibItem libItem )
  {
    return this.libItems.contains( libItem );
  }


	/* --- private Methoden --- */

  private void appendHeadCommentTo( AsmCodeBuf buf )
  {
    if( this.options.getShowAssemblerText() ) {
      buf.append( ";\n"
		+ ";Dieser Quelltext wurde vom"
		+ " JKCEMU-BASIC-Compiler erzeugt.\n"
		+ ";http://www.jens-mueller.org/jkcemu/\n"
		+ ";\n" );
    }
  }


  private void appendLineNotFoundToErrLog(
		BasicLineExpr lineExpr,
		String        trailingMsg ) throws TooManyErrorsException
  {
    if( lineExpr != null ) {
      StringBuilder buf = new StringBuilder( 128 );
      if( lineExpr.appendMsgPrefixTo( "Fehler", buf ) ) {
	buf.append( ": " );
      }
      if( lineExpr.isLabel() ) {
	buf.append( "Marke \'" );
	buf.append( lineExpr.getExprText() );
	buf.append( (char) '\'' );
      } else {
	buf.append( "BASIC-Zeilennummer " );
	buf.append( lineExpr.getExprText() );
      }
      buf.append( " nicht gefunden" );
      if( trailingMsg != null ) {
	buf.append( trailingMsg );
      }
      buf.append( (char) '\n' );
      appendToErrLog( buf.toString() );
      incErrorCount();
    }
  }


  private void appendLineNumMsgToErrLog(
			BasicSourcePos sourcePos,
			String         msg,
			String         msgType )
  {
    StringBuilder buf = new StringBuilder( 128 );
    if( sourcePos != null ) {
      if( sourcePos.appendMsgPrefixTo( msgType, buf ) ) {
	buf.append( ": " );
      }
    }
    buf.append( msg );
    if( !msg.endsWith( "\n" ) ) {
      buf.append( (char) '\n' );
    }
    appendToErrLog( buf.toString() );
  }


  private void appendLineNumMsgToErrLog( String msg, String msgType )
  {
    appendLineNumMsgToErrLog(
		new BasicSourcePos( this.curSource, this.curBasicLineNum ),
		msg,
		msgType );
  }


  private void appendToErrLog( String text )
  {
    if( this.logger != null )
      this.logger.appendToErrLog( text );
  }


  private boolean checkInstruction( CharacterIterator iter, String keyword )
  {
    return BasicUtil.checkKeyword( iter, keyword, true, true );
  }


  private void checkPutWarningFileDriverDisabled()
  {
    if( !this.options.isOpenFileEnabled() ) {
      putWarning( "Dateisystemtreiber in den Compiler-Optionen deaktiviert" );
    }
  }


  private void checkPutWarningVdipDriverDisabled()
  {
    if( !this.options.isOpenVdipEnabled() ) {
      putWarning( "VDIP-Treiber in den Compiler-Optionen deaktiviert" );
    }
  }


  private Integer checkSignedNumber( CharacterIterator iter )
							throws PrgException
  {
    boolean neg = false;
    char    ch  = BasicUtil.skipSpaces( iter );
    int     pos = iter.getIndex();
    if( ch == '+' ) {
      iter.next();
    } else if( ch == '-' ) {
      iter.next();
      neg = true;
    }
    Integer rv = BasicUtil.readNumber( iter );
    if( (rv != null) && neg ) {
      rv = new Integer( -rv.intValue() );
    }
    if( rv == null ) {
      iter.setIndex( pos );
    }
    return rv;
  }


  private SimpleVarInfo checkVariable(
				CharacterIterator iter ) throws PrgException
  {
    int           begPos  = iter.getIndex();
    SimpleVarInfo varInfo = checkVariable(
				iter,
				BasicUtil.checkIdentifier( iter ) );
    if( varInfo == null ) {
      iter.setIndex( begPos );
    }
    return varInfo;
  }


  private static String createBasicLineLabel( String lineExprText )
  {
    return LINE_LABEL_PREFIX + lineExprText.toUpperCase();
  }


  private static String createDataLineLabel( String lineExprText )
  {
    return DATA_LABEL_PREFIX + lineExprText.toUpperCase();
  }


  private void incErrorCount() throws TooManyErrorsException
  {
    this.errCnt++;
    if( this.errCnt >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  private void parseSourceText()
			throws IOException, TooManyErrorsException
  {
    this.structureStack.clear();
    this.mainPrg    = true;
    this.gcRequired = false;

    while( this.curSource != null ) {
      String line = this.curSource.readLine();
      if( line != null ) {
	parseSourceLine( line );
      } else {
	if( this.curSource != this.mainSource ) {
	  this.curSource = this.mainSource;
	  if( (this.curSource != null)
	      && this.libItems.contains( BasicLibrary.LibItem.M_SRNM ) )
	  {
	    this.asmOut.append( "\tLD\tHL,0000H\n"
				+ "\tLD\t(M_SRNM),HL\n" );
	  }
	} else {
	  this.curSource = null;
	}
      }
    }
    if( this.mainPrg ) {
      if( this.options.getShowAssemblerText() ) {
	this.asmOut.append( "\n;Programmende\n" );
      }
      this.asmOut.append( "\tJP\tXEXIT\n" );
    }

    // Pruefen, ob alle Strukturen geschlossen wurden
    for( BasicSourcePos entry : this.structureStack ) {
      appendLineNumMsgToErrLog(
		entry,
		entry.toString() + " nicht abgeschlossen",
		"Fehler" );
      this.errCnt++;
    }

    // Vorhandensein der Sprungziele und RESTORE-Positionen pruefen
    if( this.destBasicLines != null ) {
      for( BasicLineExpr lineExpr : this.destBasicLines ) {
	if( !this.basicLines.contains(
			lineExpr.getExprText().toUpperCase() ) )
	{
	  appendLineNotFoundToErrLog( lineExpr, null );
	}
      }
    }
    if( this.restoreBasicLines != null ) {
      for( BasicLineExpr lineExpr : this.restoreBasicLines ) {
	boolean found = false;
	if( this.dataBasicLines != null ) {
	  found = this.dataBasicLines.contains(
			lineExpr.getExprText().toUpperCase() );
	}
	if( !found ) {
	  appendLineNotFoundToErrLog(
		lineExpr,
		" oder enth\u00E4lt keine DATA-Anweisung" );
	}
      }
    }

    // benutzerdefinierte Funktionen und Prozeduren pruefen
    Collection<CallableEntry> callables = this.name2Callable.values();
    if( callables != null ) {
      for( CallableEntry entry : callables ) {
	if( !entry.isCalled()
	    && (this.errCnt == 0)
	    && this.options.getWarnUnusedItems() )
	{
	  putWarning( entry, entry.toString() + " wird nicht aufgerufen" );
	}
	if( entry.isImplemented() ) {
	  if( this.options.getWarnUnusedItems() ) {
	    int nVars = entry.getVarCount();
	    for( int i = 0; i < nVars; i++ ) {
	      String varName = entry.getVarName( i );
	      if( varName != null ) {
		if( !entry.isVarUsed( varName ) ) {
		  putWarning(
			entry.getVarSourcePos( varName ),
			"Lokale Variable " + varName
				+ " wird nicht verwendet" );
		}
	      }
	    }
	  }
	} else {
	  BasicSourcePos callSourcePos = entry.getFirstCallSourcePos();
	  appendLineNumMsgToErrLog(
			callSourcePos != null ? callSourcePos : entry,
			entry.toString() + " nicht implementiert",
			"Fehler" );
	  incErrorCount();
	}
      }
    }

    // globale Variablen pruefen
    if( this.options.getWarnUnusedItems() ) {
      Collection<VarDecl> globalVars = name2GlobalVar.values();
      if( globalVars != null ) {
	for( VarDecl varDecl : globalVars ) {
	  if( !varDecl.isUsed() ) {
	    putWarning(
		varDecl,
		varDecl.toString() + " wird nicht verwendet" );
	  }
	}
      }
    }
  }


  private void parseSourceLine( String lineText ) throws
						IOException,
						TooManyErrorsException
  {
    // Zeile verarbeiten
    CharacterIterator iter = null;
    try {
      iter = new StringCharacterIterator( lineText );
      this.curBasicLineNum = -1;

      // Quelltextzeile als Kommentar
      if( this.options.getShowAssemblerText()
	  && this.options.getIncludeBasicLines() )
      {
	char ch = BasicUtil.skipSpaces( iter );
	if( ch != CharacterIterator.DONE ) {
	  boolean codeCreationEnabled = this.asmOut.isEnabled();
	  this.asmOut.setEnabled( true );
	  this.asmOut.append( "\n;" );
	  while( ch != CharacterIterator.DONE ) {
	    this.asmOut.append( ch );
	    ch = iter.next();
	  }
	  this.asmOut.newLine();
	  this.asmOut.setEnabled( codeCreationEnabled );
	}
      }
      iter.setIndex( 0 );

      /*
       * auf Marke prufen,
       * Eine Marke wird mit einem Doppelpunkt abgeschlossen
       * und muss einzeln auf einer Zeile stehen, damit sie
       * syntaktisch von einer Anweisung unterschieden werden kann.
       */
      String labelText = null;
      char ch          = BasicUtil.skipSpaces( iter );
      int  begPos      = iter.getIndex();
      int  destLinePos = this.asmOut.length();
      if( ((ch >= 'A') && (ch <= 'Z'))
	  || ((ch >= 'a') && (ch <= 'z')) )
      {
	ch = iter.next();
	while( ((ch >= 'A') && (ch <= 'Z'))
	       || ((ch >= 'a') && (ch <= 'z'))
	       || ((ch >= '0') && (ch <= '9'))
	       || (ch == '_') )
	{
	  ch = iter.next();
	}
	if( ch == ':' ) {
	  iter.next();
	  if( BasicUtil.skipSpaces( iter ) == CharacterIterator.DONE ) {
	    int           endPos = iter.getIndex() - 1;
	    StringBuilder buf    = new StringBuilder( endPos - begPos );
	    iter.setIndex( begPos );
	    while( iter.getIndex() < endPos ) {
	      buf.append( (char) iter.current() );
	      iter.next();
	    }
	    labelText         = buf.toString();
	    String upperLabel = labelText.toUpperCase();
	    if( isReservedWord( upperLabel ) ) {
	      throw new PrgException( "Reserviertes Schl\u00FCsselwort"
					+ " als Marke nicht erlaubt" );
	    }
	    if( this.basicLines.contains( upperLabel ) ) {
	      appendLineNumMsgToErrLog(
			"\'" + labelText + "\': Marke bereits vorhanden",
			"Fehler" );
	      incErrorCount();
	    }
	    this.basicLines.add( upperLabel );
	    this.lastBasicLineExpr = upperLabel;
	    this.asmOut.append( createBasicLineLabel( upperLabel ) );
	    this.asmOut.append( ":\n" );
	    destLinePos = this.asmOut.length();
	  }
	}
      }
      if( labelText == null ) {

	// Zeilenummer
	iter.setIndex( 0 );
	ch = BasicUtil.skipSpaces( iter );
	Long lineNum = readLineNum( iter );
	if( lineNum != null ) {
	  this.lastBasicLineExpr = lineNum.toString();
	  if( this.basicLines.contains( this.lastBasicLineExpr ) ) {
	    appendLineNumMsgToErrLog(
		String.format(
			"BASIC-Zeilennummer %s bereits vorhanden",
			this.lastBasicLineExpr ),
		"Fehler" );
	    incErrorCount();
	  }
	  this.curBasicLineNum = lineNum.longValue();
	  if( this.curBasicLineNum <= this.lastBasicLineNum ) {
	    putWarning(
		"BASIC-Zeilennummer nicht in aufsteigender Reihenfolge" );
	  }
	  this.basicLines.add( this.lastBasicLineExpr );
	  this.lastBasicLineNum = this.curBasicLineNum;
	  this.asmOut.append( createBasicLineLabel( this.lastBasicLineExpr ) );
	  this.asmOut.append( ":\n" );
	  destLinePos = this.asmOut.length();
	}

	// Zeilennummer ggf. im Programm vermerken,
	int begLineDebugDestPos = -1;
	if( this.options.getPrintLineNumOnAbort() ) {
	  begLineDebugDestPos = this.asmOut.length();
	  PrgSource curSource = this.curSource;
	  if( curSource != null ) {
	    int srcLineNum = curSource.getLineNum();
	    if( srcLineNum < MAX_INT_VALUE ) {
	      this.asmOut.append_LD_HL_nn( srcLineNum );
	      this.asmOut.append( "\tLD\t(M_SRLN),HL\n" );
	    }
	  }
	  if( (this.curBasicLineNum >= 0)
	      && (this.curBasicLineNum < MAX_INT_VALUE) )
	  {
	    this.asmOut.append_LD_HL_nn( (int) this.curBasicLineNum );
	    this.asmOut.append( "\tLD\t(M_BALN),HL\n" );
	    this.libItems.add( BasicLibrary.LibItem.M_BALN );
	  }
	}

	// Anweisungen
	int destInstPos = this.asmOut.length();
	parseInstructions( iter, begLineDebugDestPos );
	if( this.gcRequired ) {
	  this.gcRequired = false;
	  this.asmOut.append( "\tCALL\tMRGC\n" );
	  addLibItem( BasicLibrary.LibItem.MRGC );
	}

	/*
	 * Wenn kein Code erzeugt wurde,
	 * muss auch die Zeilennummer nicht gemerkt werden.
	 */
	if( (this.asmOut.length() == destInstPos)
	    && (destInstPos > destLinePos) )
	{
	  this.asmOut.setLength( destLinePos );
	}
      }
    }
    catch( PrgException ex ) {
      String msg = ex.getMessage();
      if( msg == null ) {
	msg = "Unbekannter Fehler";
      }
      appendLineNumMsgToErrLog( msg, "Fehler" );
      if( iter != null ) {
	StringBuilder buf = new StringBuilder( iter.getEndIndex() + 16 );
	buf.append( "    " );
	int     pos  = iter.getIndex();
	char    ch   = iter.first();
	boolean done = false;
	while( ch != CharacterIterator.DONE ) {
	  buf.append( ch );
	  if( iter.getIndex() == pos ) {
	    buf.append( ERR_POS_TEXT );
	    done = true;
	  }
	  ch = iter.next();
	}
	if( !done ) {
	  buf.append( ERR_POS_TEXT );
	}
	buf.append( (char) '\n' );
	appendToErrLog( buf.toString() );
      }
      incErrorCount();
    }
    finally {
      // einzeilige IF-Anweisungen schliessen
      while( !this.structureStack.isEmpty() ) {
	BasicSourcePos entry = this.structureStack.peek();
	if( !(entry instanceof IfEntry) ) {
	  break;
	}
	IfEntry ifEntry = (IfEntry) entry;
	if( ifEntry.isMultiLine() ) {
	  break;
	}
	this.structureStack.pop();
	setCodeCreationDisabledLevel(
			ifEntry.getCodeCreationDisabledLevel() );
	String elseLabel = ifEntry.getElseLabel();
	if( elseLabel != null ) {
	  this.asmOut.append( elseLabel );
	  this.asmOut.append( ":\n" );
	}
	this.asmOut.append( ifEntry.getEndifLabel() );
	this.asmOut.append( ":\n" );
      }
    }
  }


  private void parseInstructions(
		CharacterIterator iter,
		int               begLineDebugDestPos ) throws PrgException
  {
    if( BasicUtil.skipSpaces( iter ) != CharacterIterator.DONE ) {
      for(;;) {
	this.separatorChecked = false;
	parseInstruction( iter, begLineDebugDestPos );
	char ch = BasicUtil.skipSpaces( iter );
	if( ch == CharacterIterator.DONE ) {
	  break;
	}
	if( !this.separatorChecked ) {
	  if( ch != ':' ) {
	    BasicUtil.throwUnexpectedChar( ch );
	  }
	  iter.next();
	}
	begLineDebugDestPos = -1;
      }
    }
  }


  private void parseInstruction(
		CharacterIterator iter,
		int               begLineDebugDestPos ) throws PrgException
  {
    this.tmpStrBufUsed = false;

    char ch     = BasicUtil.skipSpaces( iter );
    int  begPos = iter.getIndex();
    if( (ch == '!') || (ch == '\'') ) {		// Kommentar
      iter.next();
      parseREM( iter );
    }
    else if( ch == '?' ) {
      iter.next();
      checkMainPrgScope();
      checkCreateStackFrame();
      parsePRINT( iter );
    } else {
      boolean done = false;
      String  name = BasicUtil.checkIdentifier( iter );
      if( name != null ) {
	done = true;
	if( name.equals( "ASM" ) ) {
	  checkCreateStackFrame();
	  if( (begLineDebugDestPos >= 0)
	      && (begLineDebugDestPos < this.asmOut.length()) )
	  {
	    this.asmOut.setLength( begLineDebugDestPos );
	    begLineDebugDestPos = -1;
	  }
	  parseASM( iter, false );
	} else if( name.equals( "DECLARE" ) ) {
	  parseDECLARE( iter );
	} else if( name.equals( "REM" ) ) {
	  parseREM( iter );
	} else if( name.equals( "FUNCTION" ) ) {
	  parseCallableImpl( iter, true );
	} else if( name.equals( "LOCAL" ) ) {
	  parseLOCAL( iter );
	} else if( name.equals( "SUB" ) ) {
	  parseCallableImpl( iter, false );
	} else {
	  /*
	   * Alle weiteren Anweisungen duerfen nur im Hauptprogramm
	   * oder in einer Funktion bzw. Prozedur vorkommen.
	   * Ausserdem muss im Fall einer Funktion bzw. Prozedur
	   * der Stack-Rahmen angelegt sein.
	   */
	  checkMainPrgScope();
	  checkCreateStackFrame();
	  if( name.equals( "BORDER" ) ) {
	    parseBORDER( iter );
	  } else if( name.equals( "CALL" ) ) {
	    parseCALL( iter );
	  } else if( name.equals( "CIRCLE" ) ) {
	    parseCIRCLE( iter );
	  } else if( name.equals( "CLOSE" ) ) {
	    parseCLOSE( iter );
	  } else if( name.equals( "CLS" ) ) {
	    parseCLS();
	  } else if( name.equals( "COLOR" ) ) {
	    parseCOLOR( iter );
	  } else if( name.equals( "CURSOR" ) ) {
	    parseCURSOR( iter );
	  } else if( name.equals( "DATA" ) ) {
	    parseDATA( iter );
	  } else if( name.equals( "DEF" ) ) {
	    parseDEF( iter );
	  } else if( name.equals( "DEFUSR" ) ) {
	    parseDEFUSR( iter );
	  } else if( name.equals( "DEFUSR0" ) ) {
	    parseDEFUSR( iter, 0 );
	  } else if( name.equals( "DEFUSR1" ) ) {
	    parseDEFUSR( iter, 1 );
	  } else if( name.equals( "DEFUSR2" ) ) {
	    parseDEFUSR( iter, 2 );
	  } else if( name.equals( "DEFUSR3" ) ) {
	    parseDEFUSR( iter, 3 );
	  } else if( name.equals( "DEFUSR4" ) ) {
	    parseDEFUSR( iter, 4 );
	  } else if( name.equals( "DEFUSR5" ) ) {
	    parseDEFUSR( iter, 5 );
	  } else if( name.equals( "DEFUSR6" ) ) {
	    parseDEFUSR( iter, 6 );
	  } else if( name.equals( "DEFUSR7" ) ) {
	    parseDEFUSR( iter, 7 );
	  } else if( name.equals( "DEFUSR8" ) ) {
	    parseDEFUSR( iter, 8 );
	  } else if( name.equals( "DEFUSR9" ) ) {
	    parseDEFUSR( iter, 9 );
	  } else if( name.equals( "DIM" ) ) {
	    parseDIM( iter );
	  } else if( name.equals( "DOKE" ) ) {
	    parseDOKE( iter );
	  } else if( name.equals( "DO" ) ) {
	    parseDO( iter );
	  } else if( name.equals( "DRAW" ) ) {
	    parseDRAW( iter );
	  } else if( name.equals( "DRAWR" ) ) {
	    parseDRAWR( iter );
	  } else if( name.equals( "ELSE" ) ) {
	    parseELSE( iter );
	  } else if( name.equals( "ELSEIF" ) ) {
	    parseELSEIF( iter );
	  } else if( name.equals( "END" ) ) {
	    parseEND( iter );
	  } else if( name.equals( "ENDIF" ) ) {
	    parseENDIF( iter );
	  } else if( name.equals( "EXIT" ) ) {
	    parseEXIT( iter );
	  } else if( name.equals( "FOR" ) ) {
	    parseFOR( iter );
	  } else if( name.equals( "GOSUB" ) ) {
	    parseGOSUB( iter );
	  } else if( name.equals( "GOTO" ) ) {
	    parseGOTO( iter );
	  } else if( name.equals( "IF" ) ) {
	    parseIForELSEIF( iter, null );
	  } else if( name.equals( "INCLUDE" ) ) {
	    parseINCLUDE( iter );
	  } else if( name.equals( "INK" ) ) {
	    parseINK( iter );
	  } else if( name.equals( "INPUT" ) ) {
	    parseINPUT( iter );
	  } else if( name.equals( "LABEL" ) ) {
	    parseLABEL( iter );
	  } else if( name.equals( "LET" ) ) {
	    parseLET( iter );
	  } else if( name.equals( "LINE" ) ) {
	    parseLINE( iter );
	  } else if( name.equals( "LOCATE" ) ) {
	    parseLOCATE( iter );
	  } else if( name.equals( "LOOP" ) ) {
	    parseLOOP( iter );
	  } else if( name.equals( "LPRINT" ) ) {
	    parseLPRINT( iter );
	  } else if( name.equals( "MOVE" ) ) {
	    parseMOVE( iter );
	  } else if( name.equals( "MOVER" ) ) {
	    parseMOVER( iter );
	  } else if( name.equals( "NEXT" ) ) {
	    parseNEXT( iter );
	  } else if( name.equals( "ON" ) ) {
	    parseON( iter );
	  } else if( name.equals( "OPEN" ) ) {
	    parseOPEN( iter );
	  } else if( name.equals( "OUT" ) ) {
	    parseOUT( iter );
	  } else if( name.equals( "PAINT" ) ) {
	    parsePAINT( iter );
	  } else if( name.equals( "PAPER" ) ) {
	    parsePAPER( iter );
	  } else if( name.equals( "PASSWORD" ) ) {
	    parsePASSWORD( iter );
	  } else if( name.equals( "PAUSE" ) ) {
	    parsePAUSE( iter );
	  } else if( name.equals( "PEN" ) ) {
	    parsePEN( iter );
	  } else if( name.equals( "PLOT" ) ) {
	    parsePLOT( iter );
	  } else if( name.equals( "PLOTR" ) ) {
	    parsePLOTR( iter );
	  } else if( name.equals( "POKE" ) ) {
	    parsePOKE( iter );
	  } else if( name.equals( "PRESET" ) ) {
	    parsePRESET( iter );
	  } else if( name.equals( "PRINT" ) ) {
	    parsePRINT( iter );
	  } else if( name.equals( "PSET" ) ) {
	    parsePSET( iter );
	  } else if( name.equals( "READ" ) ) {
	    parseREAD( iter );
	  } else if( name.equals( "REM" ) ) {
	    parseREM( iter );
	  } else if( name.equals( "RESTORE" ) ) {
	    parseRESTORE( iter );
	  } else if( name.equals( "RETURN" ) ) {
	    parseRETURN();
	  } else if( name.equals( "SCREEN" ) ) {
	    parseSCREEN( iter );
	  } else if( name.equals( "WAIT" ) ) {
	    parseWAIT( iter );
	  } else if( name.equals( "WEND" ) ) {
	    parseWEND( iter );
	  } else if( name.equals( "WHILE" ) ) {
	    parseWHILE( iter );
	  } else {
	    if( !checkReturnValueAssignment( iter, name ) ) {
	      CallableEntry entry = this.name2Callable.get( name );
	      if( entry != null ) {
		parseCallableCall( iter, entry );
	      } else {
		SimpleVarInfo varInfo = checkVariable( iter, name );
		if( varInfo != null ) {
		  parseAssignment( iter, varInfo );
		} else {
		  done = false;
		}
	      }
	    }
	  }
	}
      }
      if( !done ) {
	throw new PrgException(
			"Anweisung, Prozedur oder Variable erwartet" );
      }
    }
  }


  private void parseBORDER( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsBorderColor() ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tXBORDER\n" );
      addLibItem( BasicLibrary.LibItem.XBORDER );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseExpr( this, iter );
      }
      finally {
	popCodeCreationDisabled();
      }
    }
  }


  private void parseCALL( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkToken( iter, '*' ) ) {
      BasicUtil.skipSpaces( iter );
      Integer value = BasicUtil.readHex( iter );
      if( value == null ) {
	BasicUtil.throwHexDigitExpected();
      }
      this.asmOut.append_LD_HL_nn( value.intValue() );
    } else {
      BasicExprParser.parseExpr( this, iter );
    }
    boolean insideCallable = (getEnclosingCallableEntry() != null);
    if( insideCallable ) {
      this.asmOut.append( "\tPUSH\tIY\n" );
    }
    this.asmOut.append( "\tCALL\tJP_HL\n" );
    if( insideCallable ) {
      this.asmOut.append( "\tPOP\tIY\n" );
    }
    addLibItem( BasicLibrary.LibItem.JP_HL );
  }


  private void parseCIRCLE( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointToMem( iter, "CIRCLE_M_X", "CIRCLE_M_Y" );
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseExpr( this, iter );
    this.asmOut.append( "\tLD\t(CIRCLE_M_R),HL\n"
		+ "\tCALL\tCIRCLE\n" );
    addLibItem( BasicLibrary.LibItem.CIRCLE );
  }


  private void parseCLOSE( CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '#' );
    parseIOChannelNumToPtrFldAddrInHL( iter, null );
    this.asmOut.append( "\tCALL\tIOCLOSE\n" );
    addLibItem( BasicLibrary.LibItem.IOCLOSE );
  }


  private void parseCLS()
  {
    if( this.target.supportsXCLS() ) {
      this.asmOut.append( "\tCALL\tXCLS\n" );
      addLibItem( BasicLibrary.LibItem.XCLS );
    } else {
      this.asmOut.append( "\tLD\tA,0CH\n"
		+ "\tCALL\tXOUTCH\n" );
      addLibItem( BasicLibrary.LibItem.XOUTCH );
    }
  }


  private void parseCOLOR( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      BasicExprParser.parseExpr( this, iter );
      if( BasicUtil.checkToken( iter, ',' ) ) {
	int pos = this.asmOut.length();
	BasicExprParser.parseExpr( this, iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tEX\tHL,DE\n"
			+ "\tPOP\tHL\n" );
	}
	this.asmOut.append( "\tCALL\tXCOLOR\n" );
	addLibItem( BasicLibrary.LibItem.XCOLOR );
      } else {
	this.asmOut.append( "\tCALL\tXINK\n" );
	addLibItem( BasicLibrary.LibItem.XINK );
      }
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseExpr( this, iter );
	if( BasicUtil.checkToken( iter, ',' ) ) {
	  BasicExprParser.parseExpr( this, iter );
	}
      }
      finally {
	popCodeCreationDisabled();
      }
    }
    if( BasicUtil.checkToken( iter, ',' ) ) {
      parseBORDER( iter );
    }
  }


  private void parseCURSOR( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsXCURS() ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tXCURS\n" );
      addLibItem( BasicLibrary.LibItem.XCURS );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseExpr( this, iter );
      }
      finally {
	popCodeCreationDisabled();
      }
    }
  }


  private void parseDATA( CharacterIterator iter ) throws PrgException
  {
    if( this.dataOut == null ) {
      this.dataOut = new AsmCodeBuf( 0x400 );
    }
    if( this.lastBasicLineExpr != null ) {
      if( this.dataBasicLines == null ) {
	this.dataBasicLines = new TreeSet<>();
      }
      if( this.dataBasicLines.add( this.lastBasicLineExpr ) ) {
	this.dataOut.append( createDataLineLabel( this.lastBasicLineExpr ) );
	this.dataOut.append( ":\n" );
      }
    }
    do {
      String text = BasicUtil.checkStringLiteral( this, iter );
      if( text != null ) {
	this.dataOut.append( "\tDB\t" );
	this.dataOut.appendHex2( DATA_STRING );
	this.dataOut.newLine();
	this.dataOut.appendStringLiteral( text );
      } else {
	Integer value = checkSignedNumber( iter );
	if( value == null ) {
	  throw new PrgException( "Integer- oder String-Literal erwartet" );
	}
	if( (value.intValue() >= 0) && (value.intValue() < 0x0100) ) {
	  this.dataOut.append( "\tDB\t" );
	  this.dataOut.appendHex2( DATA_INT1 );
	  this.dataOut.append( (char) ',' );
	  this.dataOut.appendHex2( value.intValue() );
	  this.dataOut.newLine();
	} else {
	  this.dataOut.append( "\tDB\t" );
	  this.dataOut.appendHex2( DATA_INT2 );
	  this.dataOut.newLine();
	  this.dataOut.append( "\tDW\t" );
	  this.dataOut.appendHex4( value.intValue() );
	  this.dataOut.newLine();
	}
      }
    } while( BasicUtil.checkToken( iter, ',' ) );
    addLibItem( BasicLibrary.LibItem.DATA );
  }


  private void parseDECLARE( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = null;
    if( BasicUtil.checkKeyword( iter, "FUNCTION" ) ) {
      parseCallableDecl( iter, true, false );
    } else if( BasicUtil.checkKeyword( iter, "SUB" ) ) {
      parseCallableDecl( iter, false, false );
    } else {
      throw new PrgException( "FUNCTION oder SUB erwartet" );
    }
  }


  private void parseDEF( CharacterIterator iter ) throws PrgException
  {
    if( !BasicUtil.checkKeyword( iter, "USR" ) ) {
      throw new PrgException( "USR erwartet" );
    }
    int usrNum = BasicUtil.parseUsrNum( iter );
    parseDEFUSR( iter, usrNum );
  }


  private void parseDEFUSR( CharacterIterator iter ) throws PrgException
  {
    int usrNum = BasicUtil.parseUsrNum( iter );
    parseDEFUSR( iter, usrNum );
  }


  private void parseDEFUSR(
			CharacterIterator iter,
			int               usrNum ) throws PrgException
  {
    BasicUtil.parseToken( iter, '=' );
    BasicExprParser.parseExpr( this, iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( getUsrLabel( usrNum ) );
    this.asmOut.append( "),HL\n" );
  }


  private void parseDIM( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = getEnclosingCallableEntry();
    if( entry != null ) {
      throw new PrgException( "Anweisung in einer Funktion/Prozedur"
			+ " nicht zul\u00E4ssig" );
    }
    do {
      String varName = BasicUtil.checkIdentifier( iter );
      if( varName == null ) {
	throwVarNameExpected();
      }
      checkVarName( varName );
      BasicUtil.parseToken( iter, '(' );
      VarDecl var  = null;
      int     dim1 = BasicUtil.parseNumber( iter );
      if( dim1 < 1 ) {
	throwDimTooSmall();
      }
      if( BasicUtil.checkToken( iter, ',' ) ) {
	int dim2 = BasicUtil.parseNumber( iter );
	if( dim2 < 1 ) {
	  throwDimTooSmall();
	}
	var = new VarDecl(
			this.curSource,
			this.curBasicLineNum,
			varName,
			dim1,
			dim2 );
      } else {
	var = new VarDecl(
			this.curSource,
			this.curBasicLineNum,
			varName,
			dim1 );
      }
      VarDecl tmpVar = this.name2GlobalVar.get( varName );
      if( tmpVar != null ) {
	if( tmpVar.getDimCount() > 0 ) {
	  throw new PrgException( "Feldvariable bereits deklariert" );
	} else {
	  throw new PrgException( "Variable implizit als einfache"
					+ " Variable bereits deklariert" );
	}
      }
      BasicUtil.parseToken( iter, ')' );
      this.name2GlobalVar.put( varName, var );
    } while( BasicUtil.checkToken( iter, ',' ) );
  }


  private void parseDO( CharacterIterator iter ) throws PrgException
  {
    String loopLabel = nextLabel();
    this.asmOut.append( loopLabel );
    this.asmOut.append( ":\n" );
    checkAppendBreakCheck();
    this.structureStack.push(
		new DoEntry(
			this.curSource,
			this.curBasicLineNum,
			loopLabel,
			nextLabel() ) );
  }


  private void parseDOKE( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    BasicUtil.parseToken( iter, ',' );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( this, pos );
    if( addr != null ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\t(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( "),HL\n" );
    } else {
      pos = this.asmOut.length();
      BasicExprParser.parseExpr( this, iter );
      Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
      if( value != null ) {
	this.asmOut.append( "\tLD\t(HL)," );
	this.asmOut.appendHex2( value.intValue() );
	this.asmOut.append( "\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL)," );
	this.asmOut.appendHex2( value.intValue() >> 8 );
	this.asmOut.newLine();
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tEX\tDE,HL\n"
			  + "\tPOP\tHL\n" );
	}
	this.asmOut.append( "\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n" );
      }
    }
  }


  private void parseDRAW( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "STEP" ) ) {
      parseDRAWR( iter );
    } else {
      checkGraphicsSupported();
      String text = BasicUtil.checkStringLiteral( this, iter );
      if( text != null ) {
	this.asmOut.append( "\tCALL\tDRAWST\n" );
	this.asmOut.appendStringLiteral( text );
	addLibItem( BasicLibrary.LibItem.DRAWST );
      } else if( BasicExprParser.checkParseStringPrimVarExpr( this, iter ) ) {
	this.asmOut.append( "\tCALL\tDRAWS\n" );
	addLibItem( BasicLibrary.LibItem.DRAWS );
      } else {
	BasicUtil.checkKeyword( iter, "TO" );
	parsePointToMem( iter, "LINE_M_EX", "LINE_M_EY" );
	this.asmOut.append( "\tCALL\tDRAW\n" );
	addLibItem( BasicLibrary.LibItem.DRAW );
      }
    }
  }


  private void parseDRAWR( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointTo_DE_HL( iter );
    this.asmOut.append( "\tCALL\tDRAWR\n" );
    addLibItem( BasicLibrary.LibItem.DRAWR );
  }


  private void parseELSE( CharacterIterator iter ) throws PrgException
  {
    for(;;) {
      IfEntry ifEntry = null;
      if( !this.structureStack.isEmpty() ) {
	BasicSourcePos entry = this.structureStack.peek();
	if( entry instanceof IfEntry ) {
	  ifEntry = (IfEntry) entry;
	}
      }
      if( ifEntry == null ) {
	throw new PrgException( "ELSE ohne IF" );
      }
      boolean ifCodeCreationDisabled = ifEntry.isIfCodeCreationDisabled();
      if( ifCodeCreationDisabled ) {
	ifEntry.setIfCodeCreationDisabled( false );
	popCodeCreationDisabled();
      }
      String elseLabel = ifEntry.getElseLabel();
      if( elseLabel != null ) {
	if( ifEntry.isElseCodeCreationDisabled() ) {
	  pushCodeCreationDisabled();
	} else {
	  if( !ifCodeCreationDisabled ) {
	    if( !ifEntry.isMultiLine()
		&& this.options.getPreferRelativeJumps() )
	    {
	      this.asmOut.append( "\tJR\t" );
	    } else {
	      this.asmOut.append( "\tJP\t" );
	    }
	    this.asmOut.append( ifEntry.getEndifLabel() );
	    this.asmOut.newLine();
	  }
	  this.asmOut.append( elseLabel );
	  this.asmOut.append( ":\n" );
	}
	ifEntry.setElseLabel( null );		// ELSE als geparst markieren
	break;
      } else {
	/*
	 * ELSE wurde schon geparst.
	 * Demzufolge muss sich das ELSE auf das vorherige IF beziehen.
	 * Deshalb wird hier der ELSE-Zweig der aktuellen IF-Anweisung
	 * geschlossen und im naechsten Schleifendurchlauf
	 * das ELSE der vorherigen IF-Anweisung verarbeitet.
	 */
	this.asmOut.append( ifEntry.getEndifLabel() );
	this.asmOut.append( ":\n" );
	this.structureStack.pop();
      }
    }
  }


  private void parseELSEIF( CharacterIterator iter ) throws PrgException
  {
    IfEntry ifEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof IfEntry ) {
	ifEntry = (IfEntry) entry;
      }
    }
    if( ifEntry == null ) {
      throw new PrgException( "ELSE ohne IF" );
    }
    String elseLabel = ifEntry.getElseLabel();
    if( elseLabel == null ) {
      throw new PrgException( "ELSEIF hinter ELSE nicht zul\u00E4ssig" );
    }
    setCodeCreationDisabledLevel( ifEntry.getCodeCreationDisabledLevel() );
    if( ifEntry.isIfCodeCreationDisabled() ) {
      ifEntry.setIfCodeCreationDisabled( false );
    } else {
      if( ifEntry.isElseCodeCreationDisabled() ) {
	pushCodeCreationDisabled();
      } else {
	if( !ifEntry.isMultiLine() && this.options.getPreferRelativeJumps() ) {
	  this.asmOut.append( "\tJR\t" );
	} else {
	  this.asmOut.append( "\tJP\t" );
	}
	this.asmOut.append( ifEntry.getEndifLabel() );
	this.asmOut.newLine();
	this.asmOut.append( elseLabel );
	this.asmOut.append( ":\n" );
	ifEntry.setElseLabel( nextLabel() );	// neue ELSE-Marke erzeugen
      }
    }
    parseIForELSEIF( iter, ifEntry );
  }


  private void parseEND( CharacterIterator iter ) throws PrgException
  {
    CallableEntry callableEntry = getEnclosingCallableEntry();
    if( BasicUtil.checkKeyword( iter, "FUNCTION" ) ) {
      boolean ok = false;
      if( callableEntry != null ) {
	if( callableEntry instanceof FunctionEntry ) {
	  ok = true;
	}
      }
      if( !ok ) {
	throw new PrgException(
			"Anweisung nur am Ende einer Funktion erlaubt" );
      }
    } else if( BasicUtil.checkKeyword( iter, "SUB" ) ) {
      boolean ok = false;
      if( callableEntry != null ) {
	if( callableEntry instanceof SubEntry ) {
	  ok = true;
	}
      }
      if( !ok ) {
	throw new PrgException(
			"Anweisung nur am Ende einer Prozedur erlaubt" );
      }
    }
    if( callableEntry != null ) {
      if( callableEntry instanceof FunctionEntry ) {
	this.asmOut.append_LD_HL_IndirectIY(
		((FunctionEntry) callableEntry).getReturnVarIYOffs() );
	this.asmOut.append( "\tLD\t(M_FRET),HL\n" );
	addLibItem( BasicLibrary.LibItem.M_FRET );
      }

      // alle Strukturen abgeschlossen?
      while( !this.structureStack.isEmpty() ) {
	BasicSourcePos entry = this.structureStack.pop();
	if( entry != callableEntry ) {
	  appendLineNumMsgToErrLog(
			entry,
			entry.toString() + " nicht abgeschlossen",
			"Fehler" );
	  this.errCnt++;
	}
      }
      if( callableEntry.hasStackFrame() ) {
	int nVars = callableEntry.getVarCount();
	// lokale String-Variablen freigeben
	int nArgs = callableEntry.getArgCount();
	if( nArgs > 0 ) {
	  for( int i = 0; i < nArgs; i++ ) {
	    if( callableEntry.getArgType( i ) == DataType.STRING ) {
	      this.asmOut.append_LD_DE_IndirectIY(
				callableEntry.getArgIYOffs( i, nArgs ) );
	      this.asmOut.append( "\tCALL\tMFREE\n" );
	      addLibItem( BasicLibrary.LibItem.MFREE );
	    }
	  }
	}
	if( nVars > 0 ) {
	  for( int i = 0; i < nVars; i++ ) {
	    if( callableEntry.getVarType( i ) == DataType.STRING ) {
	      boolean done   = false;
	      int     iyOffs = callableEntry.getVarIYOffs( i );
	      if( callableEntry instanceof FunctionEntry ) {
		if( ((FunctionEntry) callableEntry).getReturnVarIYOffs()
							== iyOffs )
		{
		  this.asmOut.append_LD_DE_IndirectIY( iyOffs );
		  this.asmOut.append( "\tCALL\tMMGC\n" );
		  addLibItem( BasicLibrary.LibItem.MMGC );
		  done = true;
		}
	      }
	      if( !done ) {
		this.asmOut.append_LD_DE_IndirectIY( iyOffs );
		this.asmOut.append( "\tCALL\tMFREE\n" );
		addLibItem( BasicLibrary.LibItem.MFREE );
	      }
	    }
	  }
	  this.asmOut.append( "\tLD\tSP,IY\n" );
	}
	this.asmOut.append( "\tPOP\tIY\n" );
      }
      this.asmOut.append( "\tRET\n" );
    } else {
      this.asmOut.append( "\tJP\tXEXIT\n" );
    }
  }


  private void parseENDIF( CharacterIterator iter ) throws PrgException
  {
    IfEntry ifEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof IfEntry ) {
	ifEntry = (IfEntry) entry;
	this.structureStack.pop();
      }
    }
    if( ifEntry == null ) {
      throw new PrgException( "ENDIF ohne IF" );
    }
    setCodeCreationDisabledLevel( ifEntry.getCodeCreationDisabledLevel() );
    String elseLabel = ifEntry.getElseLabel();
    if( elseLabel != null ) {
      this.asmOut.append( elseLabel );
      this.asmOut.append( ":\n" );
    }
    this.asmOut.append( ifEntry.getEndifLabel() );
    this.asmOut.append( ":\n" );
  }


  private void parseEXIT( CharacterIterator iter ) throws PrgException
  {
    LoopEntry loopEntry = null;
    int       idx       = this.structureStack.size() - 1;
    while( idx >= 0 ) {
      BasicSourcePos entry = this.structureStack.get( idx );
      if( entry instanceof LoopEntry ) {
	loopEntry = (LoopEntry) entry;
	break;
      }
      if( !(entry instanceof IfEntry) ) {
	break;
      }
      --idx;
    }
    if( loopEntry == null ) {
      throw new PrgException( "EXIT aus\u00DFerhalb einer Schleife" );
    }
    if( !isEndOfInstr( iter ) ) {
      if( !BasicUtil.checkKeyword( iter, loopEntry.getLoopBegKeyword() ) ) {
	throw new PrgException( loopEntry.getLoopBegKeyword()
				+ " oder Ende der Anweisung erwartet" );
      }
    }
    this.asmOut.append( "\tJP\t" );
    this.asmOut.append( loopEntry.getExitLabel() );
    this.asmOut.newLine();
  }


  private void parseFOR( CharacterIterator iter ) throws PrgException
  {
    SimpleVarInfo varInfo = checkVariable( iter );
    if( varInfo != null ) {
      if( varInfo.getDataType() != DataType.INTEGER ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      throw new PrgException( "Integer-Variable erwartet" );
    }
    if( !varInfo.hasStaticAddr() ) {
      throw new PrgException(
		"Laufvariable darf keine Feldvariable mit variablen"
						+ " Indexangaben sein." );
    }
    parseAssignment( iter, varInfo );
    if( !checkInstruction( iter, "TO" ) ) {
      throw new PrgException( "TO erwartet" );
    }
    int toPos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    Integer toValue = BasicUtil.removeLastCodeIfConstExpr( this, toPos );
    if( toValue == null ) {
      this.asmOut.append( "\tPUSH\tHL\n" );
    }
    Integer stepValue = null;
    if( checkInstruction( iter, "STEP" ) ) {
      int stepPos = this.asmOut.length();
      BasicExprParser.parseExpr( this, iter );
      stepValue = BasicUtil.removeLastCodeIfConstExpr( this, stepPos );
      if( stepValue == null ) {
	this.asmOut.append( "\tPUSH\tHL\n" );
      }
    } else {
      stepValue = new Integer( 1 );
    }
    String loopLabel = nextLabel();
    this.asmOut.append( loopLabel );
    this.asmOut.append( (char) ':' );
    checkAppendBreakCheck();
    this.asmOut.newLine();
    this.structureStack.push(
			new ForEntry(
				this.curSource,
				this.curBasicLineNum,
				loopLabel,
				nextLabel(),
				varInfo,
				toValue,
				stepValue ) );
  }


  private void parseGOSUB( CharacterIterator iter ) throws PrgException
  {
    checkAppendBreakCheck();
    if( this.options.getCheckStack()
	&& (this.options.getStackSize() > 0) )
    {
      this.asmOut.append( "\tCALL\tCKSTK\n" );
      addLibItem( BasicLibrary.LibItem.CKSTK );
    }
    String destLabel = parseDestLineExpr( iter );
    if( this.options.getCheckStack() ) {
      String endOfInstLabel = nextLabel();
      this.asmOut.append( "\tLD\tHL," );
      this.asmOut.append( endOfInstLabel );
      this.asmOut.append( "\n"
		+ "\tPUSH\tHL\n" );
      this.asmOut.append_LD_A_n( MAGIC_GOSUB );
      this.asmOut.append( "\tPUSH\tAF\n"
		+ "\tJP\t" );
      this.asmOut.append( destLabel );
      this.asmOut.newLine();
      this.asmOut.append( endOfInstLabel );
      this.asmOut.append( ":\n" );
    } else {
      this.asmOut.append( "\tCALL\t" );
      this.asmOut.append( destLabel );
      this.asmOut.newLine();
    }
  }


  private void parseGOTO( CharacterIterator iter ) throws PrgException
  {
    checkAppendBreakCheck();
    String destLabel = parseDestLineExpr( iter );
    this.asmOut.append( "\tJP\t" );
    this.asmOut.append( destLabel );
    this.asmOut.newLine();
  }


  private void parseIForELSEIF(
			CharacterIterator iter,
			IfEntry           ifEntry ) throws PrgException
  {
    boolean codeCreationDisabled = false;
    if( ifEntry != null ) {
      codeCreationDisabled = ifEntry.isElseCodeCreationDisabled();
    }
    Integer condValue = null;
    if( !codeCreationDisabled ) {
      condValue = BasicExprParser.checkParseConstExpr( this, iter );
    }
    Set<BasicLibrary.LibItem> condLibItems  = null;
    int                       condBegPos    = this.asmOut.length();
    boolean                   condOptimized = false;
    if( condValue == null ) {
      Set<BasicLibrary.LibItem> tmpLibItems = this.libItems;
      this.libItems = new HashSet<>();
      BasicExprParser.parseExpr( this, iter );
      condLibItems  = this.libItems;
      this.libItems = tmpLibItems;
      this.asmOut.append( "\tLD\tA,H\n"
			+ "\tOR\tL\n" );
    }

    // auf GOTO und mehrzeiliges IF pruefen
    BasicLineExpr gotoLineExpr = null;
    boolean       multiLine    = false;
    if( BasicUtil.checkKeyword( iter, "THEN" ) ) {
      if( isEndOfInstr( iter ) ) {
	multiLine = true;
      } else {
	char ch = BasicUtil.skipSpaces( iter );
	if( (ch >= '0') && (ch <= '9') ) {
	  gotoLineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSource,
						this.curBasicLineNum );
	}
      }
    } else {
      if( isEndOfInstr( iter ) ) {
	multiLine = true;
      }
    }
    if( !multiLine && (gotoLineExpr == null) ) {
      if( BasicUtil.checkKeyword( iter, "GOTO" ) ) {
	gotoLineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSource,
						this.curBasicLineNum );
	if( gotoLineExpr == null ) {
	  throwBasicLineExprExpected();
	}
      }
    }

    /*
     * Wenn hinter der Bedingung nur noch ein GOTO folgt
     * und die Tastatur auch nicht auf Programmabbruch abgefragt werden muss,
     * kann der Programmcode optimiert werden.
     */
    if( (condValue == null)
	&& (gotoLineExpr != null)
	&& !this.options.canBreakAlways()
	&& isEndOfInstr( iter ) )
    {
      this.asmOut.append( "\tJP\tNZ," );
      this.asmOut.append(
		createBasicLineLabel( gotoLineExpr.getExprText() ) );
      this.asmOut.newLine();
      this.destBasicLines.add( gotoLineExpr );
    } else {
      boolean ifCodeCreationDisabled   = false;
      boolean elseCodeCreationDisabled = codeCreationDisabled;
      if( condValue != null ) {
	if( condValue.intValue() == 0 ) {
	  ifCodeCreationDisabled = true;
	} else {
	  elseCodeCreationDisabled = true;
	}
      }
      if( ifEntry != null ) {
	ifEntry.setIfCodeCreationDisabled( ifCodeCreationDisabled );
	if( elseCodeCreationDisabled ) {
	  ifEntry.setElseCodeCreationDisabled( true );
	}
      } else {
	ifEntry = new IfEntry(
			this.curSource,
			this.curBasicLineNum,
			multiLine,
			nextLabel(),
			nextLabel(),
			this.codeCreationDisabledLevel,
			ifCodeCreationDisabled,
			elseCodeCreationDisabled );
	this.structureStack.push( ifEntry );
      }
      if( !codeCreationDisabled && ifCodeCreationDisabled ) {
	pushCodeCreationDisabled();
      } else {
	String elseLabel = ifEntry.getElseLabel();
	String jmpInstr  = "\tJP\t";
	if( !multiLine && this.options.getPreferRelativeJumps() ) {
	  jmpInstr = "\tJR\t";
	}
	if( !elseCodeCreationDisabled ) {
	  java.util.List<String> lines = this.asmOut.getLinesAsList(
								condBegPos );
	  if( lines.size() == 5 ) {
	    String line0 = lines.get( 0 );
	    String line1 = lines.get( 1 );
	    String line2 = lines.get( 2 );
	    if( line0.startsWith( "\tLD\tHL," )
		&& line1.startsWith( "\tLD\tDE," ) )
	    {
	      if( line2.equals( "\tCALL\tO_LT\n" ) ) {
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.cut( condBegPos );
		  this.asmOut.append( line0 );
		  this.asmOut.append( "\tBIT\t7,H\n" );
		  this.asmOut.append( jmpInstr );
		  this.asmOut.append( "Z," );
		  this.asmOut.append( elseLabel );
		  this.asmOut.newLine();
		  condOptimized = true;
		}
	      }
	      else if( line2.equals( "\tCALL\tO_GE\n" ) ) {
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.cut( condBegPos );
		  this.asmOut.append( line0 );
		  this.asmOut.append( "\tBIT\t7,H\n" );
		  this.asmOut.append( jmpInstr );
		  this.asmOut.append( "NZ," );
		  this.asmOut.append( elseLabel );
		  this.asmOut.newLine();
		  condOptimized = true;
		}
	      }
	      else if( line2.equals( "\tCALL\tO_EQ\n" ) ) {
		this.asmOut.cut( condBegPos );
		this.asmOut.append( line0 );
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.append( "\tLD\tA,H\n"
					+ "\tOR\tL\n" );
		} else {
		  this.asmOut.append( line1 );
		  this.asmOut.append( "\tOR\tA\n"
					+ "\tSBC\tHL,DE\n" );
		}
		this.asmOut.append( jmpInstr );
		this.asmOut.append( "NZ," );
		this.asmOut.append( elseLabel );
		this.asmOut.newLine();
		condOptimized = true;
	      }
	      else if( line2.equals( "\tCALL\tO_NE\n" ) ) {
		this.asmOut.cut( condBegPos );
		this.asmOut.append( line0 );
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.append( "\tLD\tA,H\n"
					+ "\tOR\tL\n" );
		} else {
		  this.asmOut.cut( condBegPos );
		  this.asmOut.append( line0 );
		  this.asmOut.append( line1 );
		  this.asmOut.append( "\tOR\tA\n"
					+ "\tSBC\tHL,DE\n" );
		}
		this.asmOut.append( jmpInstr );
		this.asmOut.append( "Z," );
		this.asmOut.append( elseLabel );
		this.asmOut.newLine();
		condOptimized = true;
	      }
	    }
	  }
	  if( !condOptimized ) {
	    this.asmOut.append( jmpInstr );
	    this.asmOut.append( "Z," );
	    this.asmOut.append( ifEntry.getElseLabel() );
	    this.asmOut.newLine();
	  }
	}
	if( gotoLineExpr != null ) {
	  checkAppendBreakCheck();
	  this.asmOut.append( "\tJP\t" );
	  this.asmOut.append(
		createBasicLineLabel( gotoLineExpr.getExprText() ) );
	  this.asmOut.newLine();
	  this.destBasicLines.add( gotoLineExpr );
	}
      }
      if( gotoLineExpr == null ) {
	this.separatorChecked = true;
      }
    }
    if( !condOptimized && (condLibItems != null) ) {
      this.libItems.addAll( condLibItems );
    }
  }


  private void parseINCLUDE( CharacterIterator iter ) throws PrgException
  {
    File file = PrgSource.getIncludeFile(
				this.curSource,
				parseStringLiteral( iter ) );
    /*
     * Hinter INCLUDE darf in der Zeile keine weitere BASIC-Anweisung
     * mehr folgen.
     */
    char ch = BasicUtil.skipSpaces( iter );
    if( ch != CharacterIterator.DONE ) {
      BasicUtil.throwUnexpectedChar( ch );
    }
    if( this.curSource != this.mainSource ) {
      throw new PrgException(
		"In sich geschachtelte INCLUDE-Anweisungen nicht erlaubt" );
    }
    try {
      this.curSource = PrgSource.readFile( file );
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	msg = msg.trim();
	if( msg.isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = "Datei kann nicht ge\u00F6ffnet werden.";
      }
      throw new PrgException( msg );
    }
    if( this.options.getPrintLineNumOnAbort() ) {
      String srcName = this.curSource.getName();
      if( srcName != null ) {
	if( !srcName.isEmpty() ) {
	  this.asmOut.append_LD_HL_xx( getStringLiteralLabel( srcName ) );
	  this.asmOut.append( "\tLD\t(M_SRNM),HL\n" );
	  this.libItems.add( BasicLibrary.LibItem.M_SRNM );
	}
      }
    }
  }


  private void parseINK( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tXINK\n" );
      addLibItem( BasicLibrary.LibItem.XINK );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseExpr( this, iter );
      }
      finally {
	popCodeCreationDisabled();
      }
    }
  }


  private void parseINPUT( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkToken( iter, '#' ) ) {
      parseIOChannelNumToPtrFldAddrInHL( iter, null );
      int begPos_IO_M_CADDR = this.asmOut.length();
      this.asmOut.append( "\tLD\t(IO_M_CADDR),HL\n" );
      int endPos_IO_M_CADDR = this.asmOut.length();
      BasicUtil.parseToken( iter, ',' );
      BasicLibrary.appendResetErrorUseBC( this );
      boolean multiVars = false;
      for(;;) {
	this.asmOut.append( "\tLD\tC,2CH\n"	// Komma als Trennzeichen
			+ "\tCALL\tIOINL\n" );
	addLibItem( BasicLibrary.LibItem.IOINL );
	int           pos     = this.asmOut.length();
	SimpleVarInfo varInfo = checkVariable( iter );
	if( varInfo == null ) {
	  throwVarExpected();
	}
	varInfo.ensureAddrInHL( this.asmOut );
	String oldVarCode = this.asmOut.cut( pos );
	if( varInfo.getDataType() == DataType.INTEGER ) {
	  this.asmOut.append( "\tCALL\tF_VLI1\n" );
	  if( BasicUtil.isSingleInst_LD_HL_xx( oldVarCode ) ) {
	    this.asmOut.append( "\tEX\tDE,HL\n" );
	    this.asmOut.append( oldVarCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldVarCode );
	    this.asmOut.append( "\tPOP\tDE\n" );
	  }
	  this.asmOut.append( "\tLD\t(HL),E\n"
				+ "\tINC\tHL\n"
				+ "\tLD\t(HL),D\n" );
	  addLibItem( BasicLibrary.LibItem.F_VLI );
	} else if( varInfo.getDataType() == DataType.STRING ) {
	  String newVarCode = BasicUtil.convertCodeToValueInDE( oldVarCode );
	  if( newVarCode != null ) {
	    this.asmOut.append( newVarCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldVarCode );
	    this.asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	  }
	  this.asmOut.append( "\tCALL\tASSIGN_STR_TO_NEW_MEM_VS\n" );
	  addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_NEW_MEM_VS );
	}
	if( !BasicUtil.checkToken( iter, ',' ) ) {
	  break;
	}
	this.asmOut.append( "\tLD\tHL,(IO_M_CADDR)\n" );
	multiVars = true;
      }
      if( !multiVars ) {
	this.asmOut.delete( begPos_IO_M_CADDR, endPos_IO_M_CADDR );
      }

    } else {

      // Eingabe von Tastatur
      do {
	String retryLabel = nextLabel();
	this.asmOut.append( retryLabel );
	this.asmOut.append( ":\n" );

	// Prompt
	String text = BasicUtil.checkStringLiteral( this, iter );
	if( text != null ) {
	  // String-Literal evtl. bereits vorhanden?
	  String label = this.str2Label.get( text );
	  if( label != null ) {
	    this.asmOut.append( "\tLD\tHL," );
	    this.asmOut.append( label );
	    this.asmOut.append( "\n"
			+ "\tCALL\tXOUTS\n" );
	    addLibItem( BasicLibrary.LibItem.XOUTS );
	  } else {
	    this.asmOut.append( "\tCALL\tXOUTST\n" );
	    this.asmOut.appendStringLiteral( text );
	    addLibItem( BasicLibrary.LibItem.XOUTST );
	  }
	  BasicUtil.checkToken( iter, ';' );
	} else {
	  this.asmOut.append_LD_A_n( '?' );
	  this.asmOut.append( "\tCALL\tXOUTCH\n" );
	  addLibItem( BasicLibrary.LibItem.XOUTCH );
	}

	// Variable
	SimpleVarInfo varInfo = checkVariable( iter );
	if( varInfo == null ) {
	  if( text != null ) {
	    throwVarExpected();
	  }
	  throwStringLitOrVarExpected();
	}
	if( varInfo.getDataType() == DataType.INTEGER ) {
	  varInfo.ensureAddrInHL( this.asmOut );
	  this.asmOut.append( "\tCALL\tINIV\n" );
	  if( this.options.getPreferRelativeJumps() ) {
	    this.asmOut.append( "\tJR\tC," );
	  } else {
	    this.asmOut.append( "\tJP\tC," );
	  }
	  this.asmOut.append( retryLabel );
	  this.asmOut.newLine();
	  addLibItem( BasicLibrary.LibItem.INIV );
	} else if( varInfo.getDataType() == DataType.STRING ) {
	  varInfo.ensureAddrInHL( this.asmOut );
	  this.asmOut.append( "\tCALL\tINSV\n" );
	  addLibItem( BasicLibrary.LibItem.INSV );
	}
      } while( BasicUtil.checkToken( iter, ';' ) );
    }
  }


  private void parseLET( CharacterIterator iter ) throws PrgException
  {
    String varName = BasicUtil.checkIdentifier( iter );
    if( varName == null ) {
      throwVarExpected();
    }
    if( !checkReturnValueAssignment( iter, varName ) ) {
      SimpleVarInfo varInfo = checkVariable( iter, varName );
      if( varInfo != null ) {
	parseAssignment( iter, varInfo );
      } else {
	throwVarExpected();
      }
    }
  }


  private void parseLABEL( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    for(;;) {
      String text = BasicUtil.checkStringLiteral( this, iter );
      if( text != null ) {
	if( !text.isEmpty() ) {
	  // String-Literal evtl. bereits vorhanden?
	  String label = this.str2Label.get( text );
	  if( label != null ) {
	    this.asmOut.append( "\tLD\tHL," );
	    this.asmOut.append( label );
	    this.asmOut.append( "\n"
		+ "\tCALL\tDRLBL\n" );
	    addLibItem( BasicLibrary.LibItem.DRLBL );
	  } else {
	    this.asmOut.append( "\tCALL\tDRLBLT\n" );
	    this.asmOut.appendStringLiteral( text );
	    addLibItem( BasicLibrary.LibItem.DRLBLT );
	  }
	}
      } else {
	if( !BasicExprParser.checkParseStringPrimVarExpr( this, iter ) ) {
	  BasicUtil.throwStringExprExpected();
	}
	this.asmOut.append( "\tCALL\tDRLBL\n" );
	addLibItem( BasicLibrary.LibItem.DRLBL );
      }
      if( BasicUtil.skipSpaces( iter ) != '+' ) {
	break;
      }
      iter.next();
    }
  }


  private void parseLINE( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "INPUT" ) ) {

      // Eingabe einer ganzen Zeile
      if( BasicUtil.checkToken( iter, '#' ) ) {
	parseInputLineIO( iter );
      } else {
	parseInputLine( iter, false );
      }

    } else {

      // Zeichnen einer Linie oder eines Rechtecks
      checkGraphicsSupported();
      boolean enclosed = BasicUtil.checkToken( iter, '(' );
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_BX),HL\n" );
      BasicUtil.parseToken( iter, ',' );
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_BY),HL\n" );
      if( enclosed ) {
	BasicUtil.parseToken( iter, ')' );
	BasicUtil.parseToken( iter, '-' );
	BasicUtil.parseToken( iter, '(' );
      } else {
	BasicUtil.parseToken( iter, ',' );
      }
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_EX),HL\n" );
      BasicUtil.parseToken( iter, ',' );
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_EY),HL\n" );
      if( enclosed ) {
	BasicUtil.parseToken( iter, ')' );
      }
      if( BasicUtil.checkToken( iter, ',' ) ) {
	if( BasicUtil.checkKeyword( iter, "BF" ) ) {
	  this.asmOut.append( "\tCALL\tDRBOXF\n" );
	  addLibItem( BasicLibrary.LibItem.DRBOXF );
	} else {
	  if( !BasicUtil.checkKeyword( iter, "B" ) ) {
	    throw new PrgException( "B oder BF erwartet" );
	  }
	  this.asmOut.append( "\tCALL\tDRBOX\n" );
	  addLibItem( BasicLibrary.LibItem.DRBOX );
	}
      } else {
	this.asmOut.append( "\tCALL\tDRAW_LINE\n" );
	addLibItem( BasicLibrary.LibItem.DRAW_LINE );
      }
    }
  }


  private void parseLOCAL( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = getEnclosingCallableEntry();
    if( entry == null ) {
	throw new PrgException(
		"Anweisung nur in einer Funktion/Prozedur erlaubt" );
    }
    do {
      String varName = BasicUtil.checkIdentifier( iter );
      if( varName == null ) {
	throwVarNameExpected();
      }
      checkVarName( varName );
      if( varName.equals( entry.getName() ) ) {
	throw new PrgException( "Name der Funktion/Prozedur"
			+ " als Variablenname nicht zul\u00E4ssig" );
      }
      entry.addVar( this.curSource, this.curBasicLineNum, varName );
    } while( BasicUtil.checkToken( iter, ',' ) );
  }


  private void parseLOCATE( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parse2ArgsTo_DE_HL( this, iter );
    if( this.target.supportsXLOCAT() ) {
      this.asmOut.append( "\tCALL\tLOCATE\n" );
      addLibItem( BasicLibrary.LibItem.LOCATE );
    } else {
      throw new PrgException( "LOCATE-Anweisung f\u00FCr das"
				+ " Zielsystem nicht unterst\u00FCtzt" );
    }
  }


  private void parseLOOP( CharacterIterator iter ) throws PrgException
  {
    DoEntry doEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof DoEntry ) {
	doEntry = (DoEntry) entry;
	this.structureStack.pop();
      }
    }
    if( doEntry == null ) {
      throw new PrgException( "LOOP ohne DO" );
    }
    if( BasicUtil.checkKeyword( iter, "UNTIL" ) ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ," + doEntry.getLoopLabel() + "\n" );
    } else if( BasicUtil.checkKeyword( iter, "WHILE" ) ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tNZ," + doEntry.getLoopLabel() + "\n" );
    } else {
      if( !isEndOfInstr( iter ) ) {
	throw new PrgException(
		"UNTIL, WHILE oder Ende der Anweisung erwartet" );
      }
      this.asmOut.append( "\tJP\t" );
      this.asmOut.append( doEntry.getLoopLabel() );
      this.asmOut.newLine();
    }
    this.asmOut.append( doEntry.getExitLabel() );
    this.asmOut.append( ":\n" );
  }


  private void parseLPRINT( CharacterIterator iter ) throws PrgException
  {
    if( !this.target.supportsXLPTCH() ) {
      throw new PrgException(
		"Druckerausgaben f\u00FCr das Zielsystem"
					+ " nicht unterst\u00FCtzt" );
    }
    this.asmOut.append( "\tLD\tHL,XLPTCH\n"
		+ "\tLD\t(IO_M_COUT),HL\n" );
    addLibItem( BasicLibrary.LibItem.XLPTCH );
    addLibItem( BasicLibrary.LibItem.IO_M_COUT );
    parsePrint( iter, false );
  }


  private void parseMOVE( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "STEP" ) ) {
      parseMOVER( iter );
    } else {
      checkGraphicsSupported();
      BasicUtil.checkKeyword( iter, "TO" );
      parsePointToMem( iter, "M_XPOS", "M_YPOS" );
      addLibItem( BasicLibrary.LibItem.M_XYPO );
    }
  }


  private void parseMOVER( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointTo_DE_HL( iter );
    this.asmOut.append( "\tCALL\tMOVER\n" );
    addLibItem( BasicLibrary.LibItem.MOVER );
  }


  private void parseNEXT( CharacterIterator iter ) throws PrgException
  {
    ForEntry forEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof ForEntry ) {
	forEntry = (ForEntry) entry;
	this.structureStack.pop();
      }
    }
    if( forEntry == null ) {
      throw new PrgException( "NEXT ohne FOR" );
    }
    if( !isEndOfInstr( iter ) ) {
      int len = this.asmOut.length();
      SimpleVarInfo varInfo = checkVariable( iter );
      this.asmOut.setLength( len );
      if( varInfo == null ) {
	throw new PrgException( "Variable oder Ende der Anweisung erwartet" );
      }
      if( !varInfo.equals( forEntry.getSimpleVarInfo() ) ) {
	throw new PrgException( "Variable stimmt nicht mit der bei der"
			+ " FOR-Anweisung angegebenen \u00FCberein." );
      }
    }
    forEntry.getSimpleVarInfo().ensureValueInHL( this.asmOut );
    Integer stepValue = forEntry.getStepValue();
    if( stepValue != null ) {
      if( stepValue.intValue() == -1 ) {
	this.asmOut.append( "\tCALL\tO_DEC\n" );
	addLibItem( BasicLibrary.LibItem.O_DEC );
      } else if( stepValue.intValue() == 1 ) {
	this.asmOut.append( "\tCALL\tO_INC\n" );
	addLibItem( BasicLibrary.LibItem.O_INC );
      } else {
	this.asmOut.append_LD_DE_nn( stepValue );
	this.asmOut.append( "\tCALL\tO_ADD\n" );
	addLibItem( BasicLibrary.LibItem.O_ADD );
      }
    } else {
      this.asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tO_ADD\n" );
      addLibItem( BasicLibrary.LibItem.O_ADD );
    }
    forEntry.getSimpleVarInfo().writeCode_LD_Var_HL( this.asmOut );
    Integer toValue = forEntry.getToValue();
    if( stepValue != null ) {
      if( toValue != null ) {
	/*
	 * Diese FOR-Schleife legt nichts auf den Stack.
	 * Es kann somit direkt zum Anfang der Schleife gesprungen werden.
	 */
	this.asmOut.append_LD_DE_nn( toValue );
	this.asmOut.append( "\tCALL\tCPHLDE\n" );
	addLibItem( BasicLibrary.LibItem.CPHLDE );
	if( stepValue.intValue() < 0 ) {
	  this.asmOut.append( "\tJP\tNC," );
	} else {
	  this.asmOut.append( "\tJP\tZ," );
	  this.asmOut.append( forEntry.getLoopLabel() );
	  this.asmOut.append( "\n"
				+ "\tJP\tC," );
	}
	this.asmOut.append( forEntry.getLoopLabel() );
	this.asmOut.newLine();
      } else {
	this.asmOut.append( "\tPOP\tDE\n" );
	this.asmOut.append( "\tCALL\tCPHLDE\n" );
	addLibItem( BasicLibrary.LibItem.CPHLDE );
	String exitLabel = nextLabel();
	if( stepValue.intValue() < 0 ) {
	  this.asmOut.append( "\tJR\tC," );
	  this.asmOut.append( exitLabel );
	  this.asmOut.newLine();
	} else {
	  String tmpLabel = nextLabel();
	  this.asmOut.append( "\tJR\tZ," );
	  this.asmOut.append( tmpLabel );
	  this.asmOut.append( "\n"
				+ "\tJR\tNC," );
	  this.asmOut.append( exitLabel );
	  this.asmOut.newLine();
	  this.asmOut.append( tmpLabel );
	  this.asmOut.append( (char) ':' );
	}
	this.asmOut.append( "\tPUSH\tDE\n"
				+ "\tJP\t" );
	this.asmOut.append( forEntry.getLoopLabel() );
	this.asmOut.newLine();
	this.asmOut.append( exitLabel );
	this.asmOut.append( ":\n" );
      }
    } else {
      this.asmOut.append( "\tLD\tB,D\n"			// Schrittweite
			+ "\tLD\tC,E\n" );
      if( toValue != null ) {
	this.asmOut.append_LD_DE_nn( toValue.intValue() );
      } else {
	this.asmOut.append( "\tPOP\tDE\n" );
      }
      this.asmOut.append( "\tLD\tA,B\n"
			+ "\tOR\tA\n"
			+ "\tJP\tM," );
      String subLabel = nextLabel();
      this.asmOut.append( subLabel );
      this.asmOut.append( "\n"
			+ "\tCALL\tCPHLDE\n" );
      addLibItem( BasicLibrary.LibItem.CPHLDE );
      String tmpLabel = nextLabel();
      this.asmOut.append( "\tJR\tZ," );
      this.asmOut.append( tmpLabel );
      this.asmOut.append( "\n"
			+ "\tJR\tC," );
      this.asmOut.append( tmpLabel );
      this.asmOut.append( "\n"
			+ "\tJR\t" );
      this.asmOut.append( forEntry.getExitLabel() );
      this.asmOut.newLine();
      this.asmOut.append( subLabel );
      this.asmOut.append( ":\n"
			+ "\tCALL\tCPHLDE\n"
			+ "\tJR\tC," );
      this.asmOut.append( forEntry.getExitLabel() );
      this.asmOut.newLine();
      this.asmOut.append( tmpLabel );
      this.asmOut.append( ":\n" );
      if( toValue == null ) {
	this.asmOut.append( "\tPUSH\tDE\n" );
      }
      this.asmOut.append( "\tPUSH\tBC\n"
				+ "\tJP\t" );
      this.asmOut.append( forEntry.getLoopLabel() );
      this.asmOut.newLine();
    }
    this.asmOut.append( forEntry.getExitLabel() );
    this.asmOut.append( ":\n" );
  }


  private void parseON( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseExpr( this, iter );
    if( BasicUtil.checkKeyword( iter, "GOSUB" ) ) {
      this.asmOut.append( "\tCALL\tONGOAD\n" );
      parseLineExprList( iter );
      String label = nextLabel();
      this.asmOut.append( "\tJR\tZ," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
		+ "\tLD\tDE," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
		+ "\tPUSH\tDE\n" );
      if( this.options.getCheckStack() ) {
        this.asmOut.append_LD_A_n( MAGIC_GOSUB );
        this.asmOut.append( "\tPUSH\tAF\n" );
      }
      this.asmOut.append( "\tJP\t(HL)\n" );
      this.asmOut.append( label );
      this.asmOut.append( ":\n" );
      addLibItem( BasicLibrary.LibItem.ONGOAD );
    } else if( BasicUtil.checkKeyword( iter, "GOTO" ) ) {
      this.asmOut.append( "\tCALL\tONGOAD\n" );
      parseLineExprList( iter );
      String label = nextLabel();
      this.asmOut.append( "\tJR\tZ," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
		+ "\tJP\t(HL)\n" );
      this.asmOut.append( label );
      this.asmOut.append( ":\n" );
      addLibItem( BasicLibrary.LibItem.ONGOAD );
    } else {
      throw new PrgException( "GOSUB oder GOTO erwartet" );
    }
  }


  private void parseOPEN( CharacterIterator iter ) throws PrgException
  {
    int      ioMode   = BasicLibrary.IOMODE_TXT_DEFAULT;
    IODriver driver   = IODriver.ALL;
    boolean  asParsed = false;
    String   text     = BasicUtil.checkStringLiteral( this, iter );
    if( text != null ) {
      String label = getStringLiteralLabel( text );
      asmOut.append( "\tLD\tHL," );
      asmOut.append( label );
      asmOut.newLine();
      text = text.trim().toUpperCase();
      if( text.startsWith( "CRT:" ) ) {
	driver = IODriver.CRT;
	if( !this.options.isOpenCrtEnabled() ) {
	  putWarning( "CRT-Treiber in den Compiler-Optionen deaktiviert" );
	}
      } else if( text.startsWith( "LPT:" ) ) {
	driver = IODriver.LPT;
	if( !this.options.isOpenLptEnabled() ) {
	  putWarning( "LPT-Treiber in den Compiler-Optionen deaktiviert" );
	}
      } else if( text.startsWith( "V:" ) ) {
	driver = IODriver.VDIP;
	checkPutWarningVdipDriverDisabled();
      } else if( this.target.startsWithFileDevice( text ) ) {
	driver = IODriver.FILE;
	checkPutWarningFileDriverDisabled();
      } else {
	int pos = text.indexOf( ':' );
	if( pos >= 0 ) {
	  putWarning( "Unbekanntes Ger\u00E4t" );
	} else {
	  /*
	   * nur Dateiname:
	   *   wenn vorhanden dann den Dateisystemtreiber nehmen,
	   *   ansonsten den VDIP-Treiber
	   */
	  if( this.target.getFileHandlerLabel() != null ) {
	    driver = IODriver.FILE;
	    checkPutWarningFileDriverDisabled();
	  } else {
	    int[] vdipIOAddrs = this.target.getVdipBaseIOAddresses();
	    if( vdipIOAddrs != null ) {
	      if( vdipIOAddrs.length > 0 ) {
		driver = IODriver.VDIP;
		checkPutWarningVdipDriverDisabled();
	      }
	    }
	  }
	}
      }
    } else {
      if( !BasicExprParser.checkParseStringPrimVarExpr( this, iter ) ) {
	BasicUtil.throwStringExprExpected();
      }
    }
    if( (driver == IODriver.ALL)
	&& !this.options.isOpenCrtEnabled()
	&& !this.options.isOpenLptEnabled()
	&& !this.options.isOpenFileEnabled()
	&& !this.options.isOpenVdipEnabled() )
    {
      putWarning( "Alle betreffenden Treiber"
			+ " in den Compiler-Optionen deaktiviert" );
    }
    this.asmOut.append( "\tLD\t(IO_M_NAME),HL\n" );
    if( BasicUtil.checkKeyword( iter, "FOR" ) ) {
      boolean binMode = BasicUtil.checkKeyword( iter, "BINARY" );
      if( BasicUtil.checkKeyword( iter, "INPUT" ) ) {
	ioMode = (binMode ?
			BasicLibrary.IOMODE_BIN_INPUT
			: BasicLibrary.IOMODE_TXT_INPUT);
      } else if( BasicUtil.checkKeyword( iter, "OUTPUT" ) ) {
	ioMode = (binMode ?
			BasicLibrary.IOMODE_BIN_OUTPUT
			: BasicLibrary.IOMODE_TXT_OUTPUT);
      } else if( BasicUtil.checkKeyword( iter, "APPEND" ) ) {
	ioMode = (binMode ?
			BasicLibrary.IOMODE_BIN_APPEND
			: BasicLibrary.IOMODE_TXT_APPEND);
      } else if( BasicUtil.checkKeyword( iter, "AS" ) ) {
	asParsed = true;
	ioMode   = (binMode ?
			BasicLibrary.IOMODE_BIN_DEFAULT
			: BasicLibrary.IOMODE_TXT_DEFAULT);
      } else {
	throw new PrgException( "INPUT, OUTPUT oder APPEND erwartet" );
      }
    }
    if( !asParsed ) {
      parseKeywordAS( iter );
    }
    this.asmOut.append_LD_A_n( ioMode );
    this.asmOut.append( "\tLD\t(IO_M_ACCESS),A\n" );
    BasicUtil.checkToken( iter, '#' );
    parseIOChannelNumToPtrFldAddrInHL( iter, null );
    this.asmOut.append( "\tCALL\tIOOPEN\n" );
    if( ((driver == IODriver.CRT) || (driver == IODriver.LPT))
	&& ((ioMode == BasicLibrary.IOMODE_TXT_OUTPUT)
	    || (ioMode == BasicLibrary.IOMODE_TXT_APPEND)
	    || (ioMode == BasicLibrary.IOMODE_BIN_OUTPUT)
	    || (ioMode == BasicLibrary.IOMODE_BIN_APPEND)) )
    {
      putWarning( "Ger\u00E4t kann nicht mit der angegebenen Betriebsart"
					+ " ge\u00F6ffnet werden" );
    }
    Set<Integer> driverModes = this.ioDriverModes.get( driver );
    if( driverModes == null ) {
      driverModes = new TreeSet<>();
    }
    driverModes.add( ioMode );
    this.ioDriverModes.put( driver, driverModes );
    addLibItem( BasicLibrary.LibItem.IOOPEN );
  }


  private void parseOUT( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = BasicUtil.checkToken( iter, '(' );
    int     pos      = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    String tmpPortCode = this.asmOut.cut( pos );
    String newPortCode = BasicUtil.convertCodeToValueInBC( tmpPortCode );
    if( enclosed ) {
      BasicUtil.parseToken( iter, ')' );
    }
    if( enclosed ) {
      if( !BasicUtil.checkToken( iter, '=' ) ) {
	BasicUtil.parseToken( iter, ',' );
      }
    } else {
      BasicUtil.parseToken( iter, ',' );
    }
    BasicExprParser.parseExpr( this, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
    if( value != null ) {
      if( newPortCode != null ) {
	this.asmOut.append( newPortCode );
      } else {
	this.asmOut.append( tmpPortCode );
	this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n" );
      }
      this.asmOut.append_LD_A_n( value.intValue() );
      this.asmOut.append( "\tOUT\t(C),A\n" );
    } else {
      if( newPortCode != null ) {
	this.asmOut.append( newPortCode );
      } else {
	String valueCode = this.asmOut.cut( pos );
	if( BasicUtil.isOnly_LD_HL_xx( valueCode ) ) {
	  if( newPortCode != null ) {
	    this.asmOut.append( newPortCode );
	  } else {
	    this.asmOut.append( tmpPortCode );
	    this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n" );
	  }
	  this.asmOut.append( valueCode );
	} else {
	  this.asmOut.append( tmpPortCode );
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( valueCode );
	  this.asmOut.append( "\tPOP\tBC\n" );
	}
      }
      this.asmOut.append( "\tOUT\t(C),L\n" );
    }
  }


  private void parsePAINT( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointToMem( iter, "PAINT_M_X", "PAINT_M_Y" );
    this.asmOut.append( "\tCALL\tPAINT\n" );
    addLibItem( BasicLibrary.LibItem.PAINT );
  }


  private void parsePAPER( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tXPAPER\n" );
      addLibItem( BasicLibrary.LibItem.XPAPER );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseExpr( this, iter );
      }
      finally {
	popCodeCreationDisabled();
      }
    }
  }


  private void parsePASSWORD( CharacterIterator iter ) throws PrgException
  {
    if( !BasicUtil.checkKeyword( iter, "INPUT" ) ) {
      throw new PrgException( "INPUT erwartet" );
    }
    parseInputLine( iter, true );
  }


  private void parsePAUSE( CharacterIterator iter ) throws PrgException
  {
    if( isEndOfInstr( iter ) ) {
      this.asmOut.append( "\tCALL\tPAUSE\n" );
      addLibItem( BasicLibrary.LibItem.PAUSE );
    } else {
      int pos = this.asmOut.length();
      BasicExprParser.parseExpr( this, iter );
      Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
      if( value != null ) {
	if( value.intValue() < 0 ) {
	  putWarningOutOfRange();
	}
	this.asmOut.append_LD_HL_nn( value.intValue() );
      }
      this.asmOut.append( "\tCALL\tPAUSE_N\n" );
      addLibItem( BasicLibrary.LibItem.PAUSE_N );
    }
  }


  private void parsePOKE( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( this, pos );
    BasicUtil.parseToken( iter, ',' );
    pos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    if( addr != null ) {
      Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
      if( value != null ) {
	if( (value & 0xFF) == 0 ) {
	  this.asmOut.append( "\tXOR\tA\n" );
	} else {
	  this.asmOut.append( "\tLD\tA," );
	  this.asmOut.appendHex2( value.intValue() );
	  this.asmOut.newLine();
	}
      } else {
	this.asmOut.append( "\tLD\tA,L\n" );
      }
      this.asmOut.append( "\tLD\t(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( "),A\n" );
    } else {
      Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
      if( value != null ) {
	this.asmOut.append( "\tLD\t(HL)," );
	this.asmOut.appendHex2( value.intValue() );
	this.asmOut.newLine();
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	  this.asmOut.append( "\tLD\t(HL),E\n" );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tLD\tA,L\n"
			+ "\tPOP\tHL\n"
			+ "\tLD\t(HL),A\n" );
	}
      }
    }
  }


  private void parsePEN( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
    if( value != null ) {
      if( (value.intValue() < 0) || (value.intValue() > 3) ) {
	throw new PrgException( "Ung\u00FCltiger Wert bei PEN" );
      }
      this.asmOut.append_LD_A_n( value.intValue() );
      this.asmOut.append( "\tCALL\tXPEN\n" );
      addLibItem( BasicLibrary.LibItem.XPEN );
    } else {
      this.asmOut.append( "\tCALL\tPEN\n" );
      addLibItem( BasicLibrary.LibItem.PEN );
    }
  }


  private void parsePLOT( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "STEP" ) ) {
      parsePLOTR( iter );
    } else {
      checkGraphicsSupported();
      BasicUtil.checkKeyword( iter, "TO" );
      parsePointTo_DE_HL( iter );
      this.asmOut.append( "\tLD\t(M_XPOS),DE\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tCALL\tXPSET\n" );
      addLibItem( BasicLibrary.LibItem.XPSET );
    }
  }


  private void parsePLOTR( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointTo_DE_HL( iter );
    this.asmOut.append( "\tCALL\tPLOTR\n" );
    addLibItem( BasicLibrary.LibItem.PLOTR );
  }


  private void parsePRESET( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointTo_DE_HL( iter );
    this.asmOut.append( "\tCALL\tXPRES\n" );
    addLibItem( BasicLibrary.LibItem.XPRES );
  }


  private void parsePRINT( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkToken( iter, '#' ) ) {
      parseIOChannelNumToWriteRoutineInHL( iter );
      this.asmOut.append( "\tCALL\tIO_SET_COUT\n" );
      addLibItem( BasicLibrary.LibItem.IO_SET_COUT );
      if( this.options.getPreferRelativeJumps() ) {
	this.asmOut.append( "\tJR\tC," );
      } else {
	this.asmOut.append( "\tJP\tC," );
      }
      String endOfInstLabel = nextLabel();
      this.asmOut.append( endOfInstLabel );
      this.asmOut.newLine();
      if( isEndOfInstr( iter ) ) {
	this.asmOut.append( "\tCALL\tPS_NL\n" );
	addLibItem( BasicLibrary.LibItem.PS_NL );
      } else {
	BasicUtil.parseToken( iter, ',' );
	parsePrint( iter, false );
      }
      this.asmOut.append( endOfInstLabel );
      this.asmOut.append( ":\n" );
    } else {
      parsePrint( iter, true );
    }
  }


  private void parsePSET( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointTo_DE_HL( iter );
    this.asmOut.append( "\tCALL\tXPSET\n" );
    addLibItem( BasicLibrary.LibItem.XPSET );
  }


  private void parseREM( CharacterIterator iter )
  {
    iter.setIndex( iter.getEndIndex() );
  }


  private void parseREAD( CharacterIterator iter ) throws PrgException
  {
    iter.previous();
    do {
      iter.next();
      SimpleVarInfo varInfo = checkVariable( iter );
      if( varInfo != null ) {
	switch( varInfo.getDataType() ) {
	  case INTEGER:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDREADI\n" );
	    addLibItem( BasicLibrary.LibItem.DREADI );
	    break;
	  case STRING:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDREADS\n" );
	    addLibItem( BasicLibrary.LibItem.DREADS );
	    break;
	}
      } else {
	throwVarExpected();
      }
    } while( BasicUtil.skipSpaces( iter ) == ',' );
  }


  private void parseRESTORE( CharacterIterator iter ) throws PrgException
  {
    BasicLineExpr lineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSource,
						this.curBasicLineNum );
    if( lineExpr != null ) {
      if( this.restoreBasicLines == null ) {
	this.restoreBasicLines = new ArrayList<>( 128 );
      }
      this.restoreBasicLines.add( lineExpr );
      this.asmOut.append_LD_HL_xx(
			createDataLineLabel( lineExpr.getExprText() ) );
      this.asmOut.append( "\tLD\t(M_READ),HL\n" );
    } else {
      if( !isEndOfInstr( iter ) ) {
	throwBasicLineExprExpected();
      }
      this.asmOut.append( "\tCALL\tDINIT\n" );
    }
    addLibItem( BasicLibrary.LibItem.DATA );
  }


  private void parseRETURN()
  {
    if( this.options.getCheckStack() ) {
      this.asmOut.append( "\tPOP\tAF\n"
		+ "\tCP\t" );
      this.asmOut.appendHex2( MAGIC_GOSUB );
      this.asmOut.append( "\n"
		+ "\tJP\tNZ,E_REWG\n" );
      addLibItem( BasicLibrary.LibItem.E_REWG );
    }
    this.asmOut.append( "\tRET\n" );
  }


  private void parseSCREEN( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseExpr( this, iter );
    this.asmOut.append( "\tCALL\tSCREEN\n" );
    addLibItem( BasicLibrary.LibItem.SCREEN );
  }


  private void parseWAIT( CharacterIterator iter ) throws PrgException
  {
    // Code fuer erstes Argument erzeugen (in BC)
    int pos = this.asmOut.length();
    BasicExprParser.parseExpr( this, iter );
    String oldCode1 = this.asmOut.cut( pos );
    String newCode1 = BasicUtil.convertCodeToValueInBC( oldCode1 );

    // Code fuer zweites Argument erzeugen (in E)
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseExpr( this, iter );
    String  oldCode2 = this.asmOut.substring( pos );
    String  newCode2 = null;
    Integer mask     = BasicUtil.removeLastCodeIfConstExpr( this, pos );
    this.asmOut.setLength( pos );
    if( mask != null ) {
      int v    = mask.intValue() & 0xFF;
      newCode2 = String.format(
			"\tLD\tE,%s%02XH\n",
			v >= 0xA0 ? "0" : "",
			v );
    } else {
      if( BasicUtil.isOnly_LD_HL_xx( oldCode2 ) ) {
	newCode2 = oldCode2 + "\tLD\tE,L\n";
      }
    }

    // Code fuer optionales drittes Argument erzeugen (in L)
    String oldCode3 = null;
    String newCode3 = null;
    if( BasicUtil.checkToken( iter, ',' ) ) {
      BasicExprParser.parseExpr( this, iter );
      oldCode3    = this.asmOut.substring( pos );
      Integer inv = BasicUtil.removeLastCodeIfConstExpr( this, pos );
      this.asmOut.setLength( pos );
      if( inv != null ) {
	int v    = inv.intValue() & 0xFF;
	newCode3 = String.format(
			"\tLD\tL,%s%02XH\n",
			v >= 0xA0 ? "0" : "",
			v );
      } else {
	if( BasicUtil.isOnly_LD_HL_xx( oldCode3 ) ) {
	  newCode3 = oldCode3;
	}
      }
    }

    /*
     * Gesamtcode erzeugen
     *   BC: Port
     *   E:  Maske
     *   L:  Inversion
     */
    if( (newCode1 != null)
	&& (newCode2 != null)
	&& ((oldCode3 == null) || (newCode3 != null)) )
    {
      this.asmOut.append( newCode1 );
      this.asmOut.append( newCode2 );
      if( newCode3 != null ) {
	this.asmOut.append( newCode3 );
      }
    } else {
      this.asmOut.append( oldCode1 );
      this.asmOut.append( "\tPUSH\tHL\n" );
      if( oldCode3 != null ) {
	if( newCode3 != null ) {
	  if( newCode2 != null ) {
	    this.asmOut.append( newCode2 );
	  } else {
	    this.asmOut.append( oldCode2 );
	    this.asmOut.append( "\tLD\tE,L\n" );
	  }
	  this.asmOut.append( newCode3 );
	} else {
	  this.asmOut.append( oldCode2 );
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode3 );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
      } else {
	if( newCode2 != null ) {
	  this.asmOut.append( newCode2 );
	} else {
	  this.asmOut.append( oldCode2 );
	  this.asmOut.append( "\tLD\tE,L\n" );
	}
      }
      this.asmOut.append( "\tPOP\tBC\n" );
    }
    String loopLabel = nextLabel();
    this.asmOut.append( loopLabel );
    this.asmOut.append( ":\tIN\tA,(C)\n" );
    if( oldCode3 != null ) {
      this.asmOut.append( "\tXOR\tL\n" );
    }
    this.asmOut.append( "\tAND\tE\n" );
    if( this.options.canBreakAlways() ) {
      String exitLabel = nextLabel();
      this.asmOut.append( "\tJR\tNZ," );
      this.asmOut.append( exitLabel );
      this.asmOut.append( "\n"
			+ "\tCALL\tXCKBRK\n"
			+ "\tJR\t" );
      this.asmOut.append( loopLabel );
      this.asmOut.newLine();
      this.asmOut.append( exitLabel );
      this.asmOut.append( ":\n" );
      addLibItem( BasicLibrary.LibItem.XCKBRK );
    } else {
      this.asmOut.append( "\tJR\tZ," );
      this.asmOut.append( loopLabel );
      this.asmOut.newLine();
    }
  }


  private void parseWEND( CharacterIterator iter ) throws PrgException
  {
    WhileEntry whileEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof WhileEntry ) {
	whileEntry = (WhileEntry) entry;
	this.structureStack.pop();
      }
    }
    if( whileEntry == null ) {
      throw new PrgException( "WEND ohne WHILE" );
    }
    this.asmOut.append( "\tJP\t" );
    this.asmOut.append( whileEntry.getLoopLabel() );
    this.asmOut.newLine();
    this.asmOut.append( whileEntry.getExitLabel() );
    this.asmOut.append( ":\n" );
  }


  private void parseWHILE( CharacterIterator iter ) throws PrgException
  {
    String loopLabel = nextLabel();
    String exitLabel = nextLabel();

    this.asmOut.append( loopLabel );
    this.asmOut.append( ":\n" );
    BasicExprParser.parseExpr( this, iter );
    BasicUtil.checkKeyword( iter, "DO" );
    this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ," + exitLabel + "\n" );
    checkAppendBreakCheck();
    this.structureStack.push(
		new WhileEntry(
			this.curSource,
			this.curBasicLineNum,
			loopLabel,
			exitLabel ) );
  }


	/* --- Parsen von Ausdruecke --- */

  private void parseExpr( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseExpr( this, iter );
  }


  private void parseLineExprList( CharacterIterator iter ) throws PrgException
  {
    java.util.List<String> labels = new ArrayList<>( 32 );
    labels.add( parseDestLineExpr( iter ) );
    while( BasicUtil.checkToken( iter, ',' ) ) {
      labels.add( parseDestLineExpr( iter ) );
    }
    int n = labels.size();
    if( n >= 0xFF ) {
      throw new PrgException( "Liste der Zeilennummern zu lang" );
    }
    this.asmOut.append( "\tDB\t" );
    this.asmOut.appendHex2( n );
    this.asmOut.newLine();
    for( String label : labels ) {
      this.asmOut.append( "\tDW\t" );
      this.asmOut.append( label );
      this.asmOut.newLine();
    }
  }


  private static Long readLineNum( CharacterIterator iter )
						throws PrgException
  {
    Long rv = null;
    char ch = BasicUtil.skipSpaces( iter );
    if( (ch >= '0') && (ch <= '9') ) {
      long lineNum = ch - '0';
      ch = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	lineNum = (lineNum * 10L) + (ch - '0');
	if( lineNum > Integer.MAX_VALUE ) {
	  throw new PrgException( "BASIC-Zeilennummer zu gro\u00DF" );
	}
	ch = iter.next();
      }
      rv = new Long( lineNum );
    }
    return rv;
  }


  /*
   * Die Methode parst eine Zuweisung.
   * Die Variable, der der Wert zugewiesen wird,
   * ist zu dem Zeitpunkt bereits geparst.
   */
  private void parseAssignment(
			CharacterIterator iter,
			SimpleVarInfo     dstVar ) throws PrgException
  {
    BasicUtil.parseToken( iter, '=' );
    if( dstVar.getDataType() == DataType.INTEGER ) {
      if( dstVar.hasStaticAddr() ) {
	BasicExprParser.parseExpr( this, iter );
	dstVar.writeCode_LD_Var_HL( this.asmOut );
      } else {
	int pos = this.asmOut.length();
	BasicExprParser.parseExpr( this, iter );
	Integer value = BasicUtil.removeLastCodeIfConstExpr( this, pos );
	if( value != null ) {
	  this.asmOut.append( "\tLD\t(HL)," );
	  this.asmOut.appendHex2( value.intValue() );
	  this.asmOut.append( "\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL)," );
	  this.asmOut.appendHex2( value.intValue() >> 8 );
	  this.asmOut.newLine();
	} else {
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tEX\tDE,HL\n"
			+ "\tPOP\tHL\n" );
	  }
	  this.asmOut.append( "\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n" );
	}
      }

    } else if( dstVar.getDataType() == DataType.STRING ) {

      boolean done   = false;
      int     srcPos = iter.getIndex();
      int     dstPos = this.asmOut.length();

      // pruefen, ob nur ein Literal zugewiesen wird
      String  text = BasicUtil.checkStringLiteral( this, iter );
      if( text != null ) {
	char ch = BasicUtil.skipSpaces( iter );
	if( isEndOfInstr( iter ) ) {
	  if( dstVar.hasStaticAddr() ) {
	    dstVar.ensureStaticAddrInDE( this.asmOut, false );
	  } else {
	    if( !BasicUtil.replaceLastCodeFrom_LD_HL_To_DE( this ) ) {
	      this.asmOut.append( "\tEX\tDE,HL\n" );
	    }
	  }
	  this.asmOut.append( "\tLD\tHL," );
	  this.asmOut.append( getStringLiteralLabel( text ) );
	  this.asmOut.append( "\n"
		+ "\tCALL\tASSIGN_STR_TO_VS\n" );
	  addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_VS );
	  done = true;
	}
      }

      // pruefen, ob nur eine String-Variable zugewiesen wird
      if( !done ) {
	iter.setIndex( srcPos );
	this.asmOut.setLength( dstPos );
	SimpleVarInfo srcVar = checkVariable( iter );
	if( srcVar != null ) {
	  char ch = BasicUtil.skipSpaces( iter );
	  if( isEndOfInstr( iter ) ) {
	    if( srcVar.hasStaticAddr() ) {
	      srcVar.ensureAddrInHL( this.asmOut );
	      dstVar.ensureStaticAddrInDE( this.asmOut, true );
	    } else {
	      this.asmOut.insert( dstPos, "\tPUSH\tHL\n" );
	      srcVar.ensureAddrInHL( this.asmOut );
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tASSIGN_VS_TO_VS\n" );
	    addLibItem( BasicLibrary.LibItem.ASSIGN_VS_TO_VS );
	    done = true;
	  }
	}
      }

      // pruefen, ob ein einfacher String-Ausdruck zugewiesen wird
      if( !done ) {
	iter.setIndex( srcPos );
	this.asmOut.setLength( dstPos );
	if( !dstVar.hasStaticAddr() ) {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	}
	if( BasicExprParser.checkParseStringPrimExpr( this, iter ) ) {
	  if( isEndOfInstr( iter ) ) {
	    if( dstVar.hasStaticAddr() ) {
	      dstVar.ensureStaticAddrInDE( this.asmOut, true );
	    } else {
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tASSIGN_STR_TO_NEW_MEM_VS\n" );
	    addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_NEW_MEM_VS );
	    done = true;
	  }
	}
      }

      // String in einem neuen Speicherbereich zusammenbauen
      if( !done ) {
	iter.setIndex( srcPos );
	this.asmOut.setLength( dstPos );
	if( !dstVar.hasStaticAddr() ) {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	}
	this.asmOut.append( "\tCALL\tMFIND\n"		// DE: Zielpuffer
		+ "\tPUSH\tDE\n" );
	this.asmOut.append_LD_BC_nn( MAX_STR_LEN );
	addLibItem( BasicLibrary.LibItem.MFIND );
	for(;;) {
	  text = BasicUtil.checkStringLiteral( this, iter );
	  if( text != null ) {
	    this.asmOut.append( "\tLD\tHL," );
	    this.asmOut.append( getStringLiteralLabel( text ) );
	    this.asmOut.newLine();
	  } else {
	    int pos = this.asmOut.length();
	    if( !BasicExprParser.checkParseStringPrimVarExpr( this, iter ) ) {
	      BasicUtil.throwStringExprExpected();
	    }
	    String tmpCode = this.asmOut.cut( pos );
	    if( BasicUtil.isOnly_LD_HL_xx( tmpCode ) ) {
	      this.asmOut.append( tmpCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tBC\n"
			+ "\tPUSH\tDE\n" );
	      this.asmOut.append( tmpCode );
	      this.asmOut.append( "\tPOP\tDE\n"
			+ "\tPOP\tBC\n" );
	    }
	  }
	  this.asmOut.append( "\tCALL\tSTNCP\n" );
	  addLibItem( BasicLibrary.LibItem.STNCP );
	  if( BasicUtil.skipSpaces( iter ) != '+' ) {
	    break;
	  }
	  iter.next();
	}
	this.asmOut.append( "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n" );
	addLibItem( BasicLibrary.LibItem.MALLOC );
	if( dstVar.hasStaticAddr() ) {
	  dstVar.ensureStaticAddrInDE( this.asmOut, true );
	} else {
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
	this.asmOut.append( "\tCALL\tASSIGN_STR_TO_VS\n" );
	addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_VS );
      }
    }
  }


	/* --- Hilfsfunktionen --- */

  private void checkAppendBreakCheck()
  {
    if( this.options.canBreakAlways() ) {
      this.asmOut.append( "\tCALL\tXCKBRK\n" );
      addLibItem( BasicLibrary.LibItem.XCKBRK );
    }
  }


  private void checkCreateStackFrame()
  {
    CallableEntry entry = getEnclosingCallableEntry();
    if( entry != null ) {
      int nVars = entry.getVarCount();
      if( !entry.hasStackFrame()
	  && ((entry.getArgCount() > 0) || (nVars > 0)) )
      {
	// Stack-Rahmen anlegen
	this.asmOut.append( "\tPUSH\tIY\n"
			+ "\tLD\tIY,0000H\n"
			+ "\tADD\tIY,SP\n" );
	int totalVarSize = entry.getTotalVarSize();
	if( totalVarSize > 0 ) {
	  this.asmOut.append_LD_HL_nn( -totalVarSize );
	  this.asmOut.append( "\tADD\tHL,SP\n"
			+ "\tLD\tSP,HL\n" );
	}

	// String-Variablen initialisieren
	boolean hlLoaded = false;
	for( int i = 0; i < nVars; i++ ) {
	  if( entry.getVarType( i ) == DataType.STRING ) {
	    if( !hlLoaded ) {
	      this.asmOut.append_LD_HL_xx( BasicLibrary.EMPTY_STRING_LABEL );
	      addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
	      hlLoaded = true;
	    }
	    this.asmOut.append_LD_IndirectIY_HL( entry.getVarIYOffs( i ) );
	  }
	}
	entry.setStackFrameCreated();
      }
    }
  }


  private void checkGraphicsSupported() throws PrgException
  {
    if( !this.target.supportsGraphics() ) {
      throw new PrgException(
		"Grafikanweisungen und -funktionen f\u00FCr"
			+ " das Zielsystem nicht unterst\u00FCtzt" );
    }
  }


  private void checkMainPrgScope() throws PrgException
  {
    if( !this.mainPrg ) {
      CallableEntry entry = getEnclosingCallableEntry();
      if( entry == null ) {
	throw new PrgException( "Anweisung nur im Hauptprogramm"
		+ " oder in einer Funktion/Prozedur zul\u00E4ssig" );
      }
    }
  }


  private boolean checkReturnValueAssignment(
				CharacterIterator iter,
				String            name ) throws PrgException
  {
    boolean       rv    = false;
    CallableEntry entry = getEnclosingCallableEntry();
    if( entry != null ) {
      if( entry.getName().equals( name ) ) {
	if( entry instanceof FunctionEntry ) {
	  Integer iyOffs = ((FunctionEntry) entry).getIYOffs( name );
	  if( iyOffs == null ) {
	    new PrgException( "R\u00FCckgabewert f\u00FCr eine"
				+ " Prozedur nicht m\u00F6glich" );
	  }
	  parseAssignment( iter, ((FunctionEntry) entry).getReturnVarInfo() );
	  rv = true;
	}
      }
    }
    return rv;
  }


  private void checkVarName( String varName ) throws PrgException
  {
    if( isReservedWord( varName ) ) {
      throw new PrgException( "Reserviertes Schl\u00FCsselwort"
			+ " als Variablenname nicht erlaubt" );
    }
  }


  private boolean isEndOfInstr( CharacterIterator iter )
  {
    char ch = BasicUtil.skipSpaces( iter );
    return (ch == CharacterIterator.DONE) || (ch == ':');
  }


  private boolean isReservedWord( String name )
  {
    return (Arrays.binarySearch( sortedReservedWords, name ) >= 0)
		|| this.target.isReservedWord( name );
  }


  private CallableEntry parseCallableDecl(
		CharacterIterator iter,
		boolean           isFunc,
		boolean           forImplementation ) throws PrgException
  {
    if( !this.structureStack.isEmpty() ) {
      throw new PrgException( "Anweisung nur in der obersten Ebene erlaubt" );
    }

    // Name parsen
    String name = BasicUtil.checkIdentifier( iter );
    if( name == null ) {
      throw new PrgException( "Name der Funktion/Prozedur erwartet" );
    }
    if( isReservedWord( name ) ) {
      throw new PrgException( "Reserviertes Schl\u00FCsselwort als"
			+ " Name einer Funktion/Prozedur nicht erlaubt" );
    }
    if( !isFunc && (name.indexOf( '$' ) >= 0) ) {
      throw new PrgException( "\'$\' im Namen einer Prozedur nicht erlaubt" );
    }

    // Funktion/Prozedur bereits vorhanden?
    boolean       mismatch = false;
    CallableEntry entry    = this.name2Callable.get( name );
    if( entry != null ) {
      if( (isFunc && !(entry instanceof FunctionEntry))
	  || (!isFunc && !(entry instanceof SubEntry)) )
      {
	mismatch = true;
      }
    }

    // Argumente parsen
    Set<String>            argSet  = new TreeSet<>();
    java.util.List<String> argList = new ArrayList<>();
    if( BasicUtil.checkToken( iter, '(' ) ) {
      if( !BasicUtil.checkToken( iter, ')' ) ) {
	do {
	  String argName = BasicUtil.checkIdentifier( iter );
	  if( argName == null ) {
	    throwVarExpected();
	  }
	  checkVarName( argName );
	  if( argName.equals( name ) ) {
	    throw new PrgException( "Name der Funktion/Prozedur"
				+ " als Variablenname nicht erlaubt" );
	  }
	  if( entry != null ) {
	    int idx = argList.size();
	    if( entry.equalsArgType( idx, argName ) ) {
	      if( forImplementation ) {
		/*
		 * Die Variablennamen koennen sich zwischen Deklaration
		 * und Implementierung unterscheiden.
		 * Entscheidend sind die bei der Implementierung.
		 */
		entry.setArg( idx, argName );
	      }
	    } else {
	      mismatch = true;
	    }
	  }
	  if( !argSet.add( argName ) ) {
	    throw new PrgException( "Lokale Variable bereits vorhanden" );
	  }
	  argList.add( argName );
	  if( argList.size() > 60 ) {
	    throw new PrgException( "Anzahl der maximal m\u00F6glichen"
				+ " Argumente \u00FCberschritten" );
	  }
	} while( BasicUtil.checkToken( iter, ',' ) );
	BasicUtil.parseToken( iter, ')' );
      }
    }

    // Uebereinstimmung mit vorheriger Deklaration?
    if( entry != null ) {
      int nArgs = 0;
      if( argList != null ) {
	nArgs = argList.size();
      }
      if( entry.getArgCount() != nArgs ) {
	mismatch = true;
      }
    }
    if( mismatch ) {
      throw new PrgException( "Funktion/Prozedur stimmt nicht"
			+ " mit vorheriger Deklaration \u00FCberein." );
    }
    if( entry == null ) {
      if( isFunc ) {
	entry = new FunctionEntry(
			this.curSource,
			this.curBasicLineNum,
			name );
      } else {
	entry = new SubEntry( this.curSource, this.curBasicLineNum, name );
      }
      if( argList != null ) {
	entry.setArgs( argList );
      }
      this.name2Callable.put( name, entry );
    }
    return entry;
  }


  private void parseCallableImpl(
			CharacterIterator iter,
			boolean           isFunc ) throws PrgException
  {
    CallableEntry entry = parseCallableDecl( iter, isFunc, true );
    if( entry.isImplemented() ) {
      throw new PrgException( "Funktion/Prozedur bereits vorhanden" );
    }
    entry.setImplemented();
    this.structureStack.push( entry );

    // ggf. Haupprogramm beenden
    if( this.mainPrg ) {
      this.asmOut.append( "\tJP\tXEXIT\n" );
      this.mainPrg = false;
    }

    // eigentliche Implementierung
    this.asmOut.append( entry.getLabel() );
    this.asmOut.append( ":\n" );
  }


  /*
   * Die Methode parst eine Kanalnummer.
   * Das Doppelkreuz ist zu dem Zeitpunkt bereits geparst.
   * Als Ergebnis steht die Adresse der IO-WRITE-Routine in HL.
   */
  private void parseIOChannelNumToWriteRoutineInHL(
				CharacterIterator iter ) throws PrgException
  {
    Integer channel = BasicUtil.readNumber( iter );
    if( channel != null ) {
      switch( channel.intValue() ) {
	case 1:
	  this.asmOut.append( "\tLD\tHL,IOCTB1\n"
			+ "\tLD\t(IO_M_CADDR),HL\n"
			+ "\tLD\tHL,(IOCTB1+" );
	  this.asmOut.appendHex2( BasicLibrary.IOCTB_WRITE_OFFS );
	  this.asmOut.append( ")\n" );
	  addLibItem( BasicLibrary.LibItem.IOCTB1 );
	  break;
	case 2:
	  this.asmOut.append( "\tLD\tHL,IOCTB2\n"
			+ "\tLD\t(IO_M_CADDR),HL\n"
			+ "\tLD\tHL,(IOCTB2+" );
	  this.asmOut.appendHex2( BasicLibrary.IOCTB_WRITE_OFFS );
	  this.asmOut.append( ")\n" );
	  addLibItem( BasicLibrary.LibItem.IOCTB2 );
	  break;
	default:
	  throwIOChannelNumOutOfRange();
      }
    } else {
      BasicExprParser.parseExpr( this, iter );
      this.asmOut.append( "\tCALL\tIOCADR\n"
			+ "\tLD\t(IO_M_CADDR),HL\n" );
      this.asmOut.append_LD_DE_nn( BasicLibrary.IOCTB_WRITE_OFFS );
      this.asmOut.append( "\tADD\tHL,DE\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tINC\tHL\n"
			+ "\tLD\tH,(HL)\n"
			+ "\tLD\tL,A\n" );
      addLibItem( BasicLibrary.LibItem.IOCADR );
      addLibItem( BasicLibrary.LibItem.IOCTB1 );
      addLibItem( BasicLibrary.LibItem.IOCTB2 );
    }
  }


  private String parseDestLineExpr( CharacterIterator iter )
						throws PrgException
  {
    BasicLineExpr lineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSource,
						this.curBasicLineNum );
    if( lineExpr == null ) {
      throwBasicLineExprExpected();
    }
    this.destBasicLines.add( lineExpr );
    return createBasicLineLabel( lineExpr.getExprText() );
  }


  private void parseInputLine(
			CharacterIterator iter,
			boolean           password ) throws PrgException
  {
    String text = BasicUtil.checkStringLiteral( this, iter );
    if( text != null ) {
      // String-Literal evtl. bereits vorhanden?
      String label = this.str2Label.get( text );
      if( label != null ) {
	this.asmOut.append( "\tLD\tHL," );
	this.asmOut.append( label );
	this.asmOut.append( "\n"
		+ "\tCALL\tXOUTS\n" );
	addLibItem( BasicLibrary.LibItem.XOUTS );
      } else {
	this.asmOut.append( "\tCALL\tXOUTST\n" );
	this.asmOut.appendStringLiteral( text );
	addLibItem( BasicLibrary.LibItem.XOUTST );
      }
      BasicUtil.parseToken( iter, ';' );
    }
    SimpleVarInfo varInfo = checkVariable( iter );
    if( varInfo != null ) {
      if( varInfo.getDataType() != DataType.STRING ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      if( text != null ) {
	throwStringVarExpected();
      }
      throwStringLitOrVarExpected();
    }
    varInfo.ensureAddrInHL( this.asmOut );
    if( password ) {
      this.asmOut.append( "\tCALL\tINPWV\n" );
      addLibItem( BasicLibrary.LibItem.INPWV );
    } else {
      this.asmOut.append( "\tCALL\tINRSV\n" );
      addLibItem( BasicLibrary.LibItem.INRSV );
    }
  }


  private void parseInputLineIO( CharacterIterator iter ) throws PrgException
  {
    parseIOChannelNumToPtrFldAddrInHL( iter, null );
    BasicUtil.parseToken( iter, ',' );
    this.asmOut.append( "\tLD\tC,00H\n"
			+ "\tCALL\tIOINL\n" );
    int           pos     = this.asmOut.length();
    SimpleVarInfo varInfo = checkVariable( iter );
    if( varInfo == null ) {
      throwStringVarExpected();
    }
    if( varInfo.getDataType() != DataType.STRING ) {
      throwStringVarExpected();
    }
    varInfo.ensureAddrInHL( this.asmOut );
    String oldVarCode = this.asmOut.cut( pos );
    String newVarCode = BasicUtil.convertCodeToValueInDE( oldVarCode );
    if( newVarCode != null ) {
      this.asmOut.append( newVarCode );
    } else {
      this.asmOut.append( "\tPUSH\tHL\n" );
      this.asmOut.append( oldVarCode );
      this.asmOut.append( "\tEX\tDE,HL\n"
			+ "\tPOP\tHL\n" );
    }
    this.asmOut.append( "\tCALL\tASSIGN_STR_TO_NEW_MEM_VS\n" );
    addLibItem( BasicLibrary.LibItem.IOINL );
    addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_NEW_MEM_VS );
  }


  private void parseKeywordAS( CharacterIterator iter ) throws PrgException
  {
    if( !BasicUtil.checkKeyword( iter, "AS" ) ) {
      throw new PrgException( "AS erwartet" );
    }
  }


  private void parsePointTo_DE_HL( CharacterIterator iter )
							throws PrgException
  {
    boolean enclosed = BasicUtil.checkToken( iter, '(' );
    BasicExprParser.parse2ArgsTo_DE_HL( this, iter );
    if( enclosed ) {
      BasicUtil.parseToken( iter, ')' );
    }
  }


  private void parsePointToMem(
			CharacterIterator iter,
			String            labelX,
			String            labelY ) throws PrgException
  {
    boolean enclosed = BasicUtil.checkToken( iter, '(' );
    BasicExprParser.parseExpr( this, iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( labelX );
    this.asmOut.append( "),HL\n" );
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseExpr( this, iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( labelY );
    this.asmOut.append( "),HL\n" );
    if( enclosed ) {
      BasicUtil.parseToken( iter, ')' );
    }
  }


  private void parsePrint(
			CharacterIterator iter,
			boolean           toScreen ) throws PrgException
  {
    boolean newLine   = true;
    boolean space     = false;
    boolean formatted = false;
    char    ch        = BasicUtil.skipSpaces( iter );
    while( !isEndOfInstr( iter ) ) {
      this.tmpStrBufUsed = false;
      newLine            = true;
      space              = false;
      boolean hasSeg     = false;

      // String-Segment pruefen
      boolean mandatory = false;
      for(;;) {
        /*
	 * CHR$ pruefen
         * Im Gegensatz zur gewoehnlichen String-Behandlung,
         * bei der CHR$(0) eine leere Zeichenkette liefert,
         * wird bei PRINT jedes mit CHR$(...) angegebene Byte ausgegeben,
         * auch wenn es sich um ein Null-Byte handelt.
         */
	boolean done   = false;
	int     srcPos = iter.getIndex();
	if( BasicUtil.checkKeyword( iter, "CHR$" ) ) {
	  if( BasicUtil.checkToken( iter, '(' ) ) {
	    int dstPos = this.asmOut.length();
	    BasicExprParser.parseExpr( this, iter );
	    Integer value = BasicUtil.removeLastCodeIfConstExpr(
							this, dstPos );
	    if( value != null ) {
	      this.asmOut.append_LD_A_n( value.intValue() );
	    } else {
	      this.asmOut.append( "\tLD\tA,L\n" );
	    }
	    if( toScreen ) {
	      this.asmOut.append( "\tCALL\tXOUTCH\n" );
	      addLibItem( BasicLibrary.LibItem.XOUTCH );
	    } else {
	      this.asmOut.append( "\tCALL\tIO_COUT\n" );
	      addLibItem( BasicLibrary.LibItem.IO_COUT );
	    }
	    BasicUtil.parseToken( iter, ')' );
	    hasSeg = true;
	    done   = true;
	  }
	} else if( BasicUtil.checkKeyword( iter, "SPC" ) ) {
	  BasicExprParser.parseEnclosedExpr( this, iter );
	  if( toScreen ) {
	    this.asmOut.append( "\tCALL\tPRINT_SPC\n" );
	    addLibItem( BasicLibrary.LibItem.PRINT_SPC );
	  } else {
	    this.asmOut.append( "\tCALL\tIO_PRINT_SPC\n" );
	    addLibItem( BasicLibrary.LibItem.IO_PRINT_SPC );
	  }
	  hasSeg = true;
	  done   = true;
	}
	if( !done ) {
	  iter.setIndex( srcPos );
	  String text = BasicUtil.checkStringLiteral( this, iter );
	  if( text != null ) {
	    // String-Literal evtl. bereits vorhanden?
	    String label = this.str2Label.get( text );
	    if( label != null ) {
	      this.asmOut.append( "\tLD\tHL," );
	      this.asmOut.append( label );
	      this.asmOut.newLine();
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tXOUTS\n" );
		addLibItem( BasicLibrary.LibItem.XOUTS );
	      } else {
		this.asmOut.append( "\tCALL\tPS_S\n" );
		addLibItem( BasicLibrary.LibItem.PS_S );
	      }
	    } else {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tXOUTST\n" );
		addLibItem( BasicLibrary.LibItem.XOUTST );
	      } else {
		this.asmOut.append( "\tCALL\tPS_ST\n" );
		addLibItem( BasicLibrary.LibItem.PS_ST );
	      }
	      this.asmOut.appendStringLiteral( text );
	    }
	    hasSeg = true;
	  } else if( BasicExprParser.checkParseStringPrimVarExpr(
							this, iter ) )
	  {
	    if( toScreen ) {
	      this.asmOut.append( "\tCALL\tXOUTS\n" );
	      addLibItem( BasicLibrary.LibItem.XOUTS );
	    } else {
	      this.asmOut.append( "\tCALL\tPS_S\n" );
	      addLibItem( BasicLibrary.LibItem.PS_S );
	    }
	    hasSeg = true;
	  } else {
	    if( mandatory ) {
	      BasicUtil.throwStringExprExpected();
	    }
	  }
	}
	if( BasicUtil.skipSpaces( iter ) != '+' ) {
	  break;
	}
	iter.next();
	mandatory = true;
      }
      if( !hasSeg ) {

	// kann nur noch numerisches Segment sein
	BasicExprParser.parseExpr( this, iter );
	if( formatted ) {
	  if( toScreen ) {
	    this.asmOut.append( "\tCALL\tP_IF\n" );
	    addLibItem( BasicLibrary.LibItem.P_IF );
	  } else {
	    this.asmOut.append( "\tCALL\tPS_IF\n" );
	    addLibItem( BasicLibrary.LibItem.PS_IF );
	  }
	} else {
	  if( toScreen ) {
	    this.asmOut.append( "\tCALL\tP_I\n" );
	    addLibItem( BasicLibrary.LibItem.P_I );
	  } else {
	    this.asmOut.append( "\tCALL\tPS_I\n" );
	    addLibItem( BasicLibrary.LibItem.PS_I );
	  }
	}
	space = true;
      }

      // weiteres Segment?
      ch        = BasicUtil.skipSpaces( iter );
      formatted = (ch == ',');
      if( (ch != ';') && (ch != ',') ) {
	break;
      }
      if( space ) {
	if( toScreen ) {
	  this.asmOut.append( "\tCALL\tOUTSP\n" );
	  addLibItem( BasicLibrary.LibItem.OUTSP );
	} else {
	  this.asmOut.append( "\tCALL\tPS_SP\n" );
	  addLibItem( BasicLibrary.LibItem.PS_SP );
	}
      }
      newLine = false;
      iter.next();
      ch = BasicUtil.skipSpaces( iter );
    }
    if( newLine ) {
      if( toScreen ) {
	this.asmOut.append( "\tCALL\tXOUTNL\n" );
	addLibItem( BasicLibrary.LibItem.XOUTNL );
      } else {
	this.asmOut.append( "\tCALL\tPS_NL\n" );
	addLibItem( BasicLibrary.LibItem.PS_NL );
      }
    }
  }


  private String parseStringLiteral( CharacterIterator iter )
							throws PrgException
  {
    String text = BasicUtil.checkStringLiteral( this, iter );
    if( text == null ) {
      throw new PrgException( "String-Literal erwartet" );
    }
    return text;
  }


  private void popCodeCreationDisabled()
  {
    setCodeCreationDisabledLevel( this.codeCreationDisabledLevel - 1 );
  }


  private void pushCodeCreationDisabled()
  {
    setCodeCreationDisabledLevel( this.codeCreationDisabledLevel + 1 );
  }


  public void putWarning( String msg )
  {
    appendLineNumMsgToErrLog( msg, "Warnung" );
  }


  public void putWarning( BasicSourcePos sourcePos, String msg )
  {
    appendLineNumMsgToErrLog( sourcePos, msg, "Warnung" );
  }


  private void setCodeCreationDisabledLevel( int level )
  {
    this.codeCreationDisabledLevel = (level > 0 ? level : 0);
    this.asmOut.setEnabled( this.codeCreationDisabledLevel == 0 );
  }


  private static void throwBasicLineExprExpected() throws PrgException
  {
    throw new PrgException( "BASIC-Zeilennummer oder Marke erwartet" );
  }


  private static void throwDimTooSmall() throws PrgException
  {
    throw new PrgException( "Dimension zu klein" );
  }


  private static void throwDivisionByZero() throws PrgException
  {
    throw new PrgException( "Division durch 0" );
  }


  private static void throwIOChannelNumOutOfRange() throws PrgException
  {
    throw new PrgException(
		"Kanalnummer au\u00DFerhalb des g\u00FCltigen Bereichs" );
  }


  private static void throwStringLitOrVarExpected() throws PrgException
  {
    throw new PrgException( "String-Literal oder String-Variable erwartet" );
  }


  private static void throwStringVarExpected() throws PrgException
  {
    throw new PrgException( "String-Variable erwartet" );
  }


  private static void throwVarExpected() throws PrgException
  {
    throw new PrgException( "Variable erwartet" );
  }


  private static void throwVarNameExpected() throws PrgException
  {
    throw new PrgException( "Name einer Variable erwartet" );
  }
}
