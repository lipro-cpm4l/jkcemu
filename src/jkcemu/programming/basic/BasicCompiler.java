/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import jkcemu.base.EmuUtil;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgLogger;
import jkcemu.programming.PrgSource;
import jkcemu.programming.TooManyErrorsException;


public class BasicCompiler
{
  public static final int    DATA_INT1          = 0x01;
  public static final int    DATA_INT2          = 0x02;
  public static final int    DATA_INT4          = 0x04;
  public static final int    DATA_DEC6          = 0x16;
  public static final int    DATA_STRING        = 0x31;
  public static final int    MAGIC_GOSUB        = 'G';
  public static final int    MAX_STR_LEN        = 255;
  public static final int    MAX_INT_VALUE      = 0x7FFF;
  public static final long   MAX_LONG_VALUE     = 0x7FFFFFFFL;
  public static final char   TYPE_STRING_SUFFIX = '$';
  public static final String ERR_POS_TEXT       = " ??? ";
  public static final String DATA_LABEL_PREFIX  = "DATA_POOL_";
  public static final String LINE_LABEL_PREFIX  = "L_";
  public static final String START_LABEL        = "APPSTART";
  public static final String TOP_LABEL          = "MTOP";

  public static enum AccessMode { READ, WRITE, READ_WRITE };
  public static enum DataType { INT2, INT4, FLOAT4, DEC6, STRING };
  public static enum IODriver { CRT, LPT, DISK, VDIP, FILE_ALL, ALL };

  /*
   * sortierte Liste der reservierten Schluesselwoerter
   * ohne zielsystemabhaengige Konstanten
   */
  private static final String[] sortedReservedWords = {
	"ABS", "ASIN", "ACOS", "ADD", "AND", "APPEND",
	"AS", "ASC", "ASM", "AT", "ATAN",
	"BIN$", "BINARY", "BORDER", "BSS",
	"CALL", "CASE", "CDEC", "CHR$", "CINT", "CIRCLE",
	"CLNG", "CLOSE", "CLS", "CRSLIN", "CRSPOS",
	"CODE", "COLOR", "CURSOR", "COS",
	"DATA", "DATETIME$", "DECIMAL", "DECLARE", "DECVAL",
	"DEEK", "DEF", "DEFUSR",
	"DEFUSR0", "DEFUSR1", "DEFUSR2", "DEFUSR3", "DEFUSR4",
	"DEFUSR5", "DEFUSR6", "DEFUSR7", "DEFUSR8", "DEFUSR9",
	"DIM", "DO", "DOKE", "DOUBLE", "DRAW", "DRAWR",
	"ELSE", "ELSEIF", "END", "ENDIF", "EOF", "ERR", "ERR$", "EXIT",
	"E_CHANNEL_ALREADY_OPEN", "E_CHANNEL_CLOSED",
	"E_DEVICE_LOCKED", "E_DEVICE_NOT_FOUND",
	"E_DIGITS_TRUNCATED", "E_DISK_FULL",
	"E_EOF", "E_ERROR", "E_FILE_NOT_FOUND", "E_INVALID",
	"E_IO_ERROR", "E_IO_MODE", "E_NO_DISK", "E_OK", "E_OVERFLOW",
	"E_PATH_NOT_FOUND",
	"E_READ_ONLY", "E_SOCKET_STATUS", "E_UNKNOWN_HOST",
	"FALSE", "FLOAT", "FOR", "FRAC", "FUNCTION",
	"GOSUB", "GOTO",
	"HEX$", "HIBYTE", "HIWORD", "H_CHAR", "H_PIXEL",
	"IF", "IN", "INCLUDE", "INK", "INKEY$",
	"INP", "INPUT", "INPUT$", "INSTR", "INT", "INTEGER", "INTVAL",
	"JOYST", "JOYST_BUTTONS",
	"LABEL", "LCASE$", "LEFT$", "LEN", "LET", "LINE", "LNGVAL",
	"LOBYTE", "LOCAL", "LOCATE", "LOG", "LONG", "LOOP",
	"LOWER$", "LOWORD", "LPRINT", "LTRIM$",
	"MAX", "MEMSTR$", "MID$", "MIN", "MIRROR$",
	"MOD", "MOVE", "MOVER",
	"NEXT", "NOT",
	"ON", "OPEN", "OR", "OUT", "OUTPUT",
	"PAINT", "PAPER", "PASSWORD", "PAUSE", "PEEK",
	"PEN", "PEN_ERASER", "PEN_NONE", "PEN_NORMAL", "PEN_XOR",
	"PLOT", "PLOTR", "POINT", "POKE", "POS",
	"PRESET", "PRINT", "PSET", "PTEST",
	"READ", "REM", "RESTORE", "RETURN", "RIGHT$", "RND", "ROUND",
	"ROUND_HALF_DOWN", "ROUND_HALF_EVEN", "ROUND_HALF_UP", "RTRIM$",
	"SCALE", "SCREEN", "SELECT", "SGN", "SHL", "SHR", "SIN", "SINGLE",
	"SPACE$", "SQR", "STEP", "STOP", "STR$", "STRING", "STRING$",
	"STRPTR", "SUB", "SWAP",
	"TAN", "TEXT", "THEN", "TO", "TOP", "TRIM$", "TRUE", "TRUNC",
	"UCASE$", "UNTIL", "UPPER$", "USING", "USR",
	"USR0", "USR1", "USR2", "USR3", "USR4",
	"USR5", "USR6", "USR7", "USR8", "USR9",
	"VAL", "VDIP",
	"WAIT", "WEND", "WHILE", "WRITE", "W_CHAR", "W_PIXEL",
	"XOR", "XPOS", "YPOS" };

  private static final String CODE_LD_SRC_LINE_HL = "\tLD\t(M_SRC_LINE),HL\n";

  private static final String MSG_VAR_NOT_WRITTEN
				= "Variable %s wird gelesen aber nirgends"
					+ " ein Wert zugewiesen";

  private static final String MSG_VAR_NOT_READ
				= "Variable %s wird nirgends gelesen";

  private static final String MSG_VAR_NOT_USED
				= "Variable %s wird nicht verwendet";

  private PrgSource                         curSource;
  private PrgSource                         mainSource;
  private AbstractTarget                    target;
  private BasicOptions                      options;
  private PrgLogger                         logger;
  private Map<IODriver,Set<Integer>>        ioDriverModes;
  private Map<BasicLibrary.LibItem,Integer> libItems;
  private SortedSet<String>                 basicLines;
  private Collection<BasicLineExpr>         destBasicLines;
  private Set<String>                       dataBasicLines;
  private Collection<BasicLineExpr>         restoreBasicLines;
  private Stack<BasicSourcePos>             structureStack;
  private Map<String,CallableEntry>         name2Callable;
  private Map<String,VarDecl>               name2GlobalVar;
  private Map<String,String>                str2Label;
  private SortedSet<String>                 usrLabels;
  private AsmCodeBuf                        asmOut;
  private AsmCodeBuf                        dataOut;
  private AsmCodeBuf                        sysDataOut;
  private StringBuilder                     userData;
  private StringBuilder                     userBSS;
  private boolean                           caseExpected;
  private boolean                           gcRequired;
  private boolean                           execEnabled;
  private boolean                           mainPrg;
  private boolean                           separatorChecked;
  private boolean                           tmpStrBufUsed;
  private boolean                           errVarsSet;
  private int                               errCnt;
  private int                               resetErrCodePos;
  private int                               labelNum;
  private int                               codeCreationDisabledLevel;
  private int                               codePosAtBegOfSrcLine;
  private long                              curBasicLineNum;
  private long                              lastBasicLineNum;
  private String                            lastBasicLineExpr;
  private String                            stackFrameCode;
  private int                               stackFramePos;


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
    this.libItems                  = new HashMap<>();
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
    this.sysDataOut                = new AsmCodeBuf( 0x0400 );
    this.userBSS                   = null;
    this.userData                  = null;
    this.execEnabled               = true;
    this.mainPrg                   = true;
    this.caseExpected              = false;
    this.gcRequired                = false;
    this.separatorChecked          = false;
    this.tmpStrBufUsed             = false;
    this.errVarsSet                = false;
    this.codeCreationDisabledLevel = 0;
    this.errCnt                    = 0;
    this.resetErrCodePos           = -1;
    this.labelNum                  = 1;
    this.curBasicLineNum           = -1;
    this.lastBasicLineNum          = -1;
    this.lastBasicLineExpr         = null;
    this.stackFrameCode            = null;
    this.stackFramePos             = -1;

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
    addLibItem( libItem, 1 );
  }


  public void cancel()
  {
    this.execEnabled = false;
  }


  /*
   * Die Methode prueft, ob der uebergebene Name eine Variable ist
   * bzw. als einfache Variable gewertet werden kann (implizite Deklaration).
   * Wenn ja, wird die Variable vollstaendig geparst (inkl. Indexe)
   * und ein VarInfo-Objekt zurueckgeliefert.
   * Ist in dem Objekt das Attribute "addrExpr" gefuellt,
   * bescheibt dieses die Adresse der Variable.
   * Im dem Fall wurde kein Code erzeugt.
   * Ist dagegen "addrExpr" null, wurde Code erzeugt,
   * der im HL-Register die Adresse der Variable enthaelt.
   */
  public SimpleVarInfo checkVariable(
			CharacterIterator iter,
			ParseContext      context,
			String            varName,
			AccessMode        accessMode ) throws PrgException
  {
    SimpleVarInfo varInfo = null;
    if( varName != null ) {
      if( !varName.isEmpty() ) {
	if( !isReservedWord( varName ) ) {

	  // auf lokale Variable prufen
	  CallableEntry callableEntry = getEnclosingCallableEntry();
	  if( callableEntry != null ) {
	    if( !varName.equals( callableEntry.getName() ) ) {
	      Integer iyOffs = callableEntry.getIYOffs( varName );
	      if( iyOffs != null ) {
		callableEntry.updVarUsage( varName, accessMode );
		varInfo = new SimpleVarInfo(
				callableEntry.getArgOrVarType( varName ),
				null,
				iyOffs );
	      }
	    }
	  }

	  /*
	   * Wenn es keine benutzerdefinierte Funktion/Prozedur ist,
	   * muss es eine globale Variable sein.
	   */
	  if( (varInfo == null)
	      && !this.name2Callable.containsKey( varName ) )
	  {
	    VarDecl varDecl = this.name2GlobalVar.get( varName );
	    if( varDecl != null ) {
	      updVarUsage( varDecl, accessMode );
	      String addrExpr = null;
	      if( varDecl.getDimCount() > 0 ) {
		BasicUtil.parseToken( iter, '(' );
		if( varDecl.getDimCount() == 1 ) {
		  int pos = this.asmOut.length();
		  BasicExprParser.parseInt2Expr( this, iter, context );
		  Integer idx = BasicUtil.removeLastCodeIfConstExpr(
								this.asmOut,
								pos );
		  if( idx != null ) {
		    if( (idx.intValue() < 0) || (idx > varDecl.getDim1()) ) {
		      BasicUtil.throwIndexOutOfRange();
		    }
		    int elemOffs = idx.intValue() * varDecl.getElemSize();
		    if( idx.intValue() > 0 ) {
		      if( idx.intValue() >= 0xA000 ) {
			addrExpr = String.format(
						"%s+0%04XH",
						varDecl.getLabel(),
						elemOffs );
		      } else {
			addrExpr = String.format(
						"%s+%04XH",
						varDecl.getLabel(),
						elemOffs );
		      }
		    } else {
		      addrExpr = varDecl.getLabel();
		    }
		  } else {
		    appendCodeIndexToElemAddr( varDecl );
		  }
		} else if( varDecl.getDimCount() == 2 ) {
		  /*
		   * Offset-Berechnung:
		   * offs = ((idx1 * (dim2+1)) + idx2) * elemSize
		   */
		  int pos = this.asmOut.length();
		  BasicExprParser.parseInt2Expr( this, iter, context );
		  Integer idx1 = BasicUtil.removeLastCodeIfConstExpr(
								this.asmOut,
								pos );
		  if( idx1 != null ) {
		    if( (idx1.intValue() < 0)
			|| (idx1.intValue() > varDecl.getDim1()) )
		    {
		      BasicUtil.throwIndexOutOfRange();
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim1() + 1 );
		      this.asmOut.append( "\tCALL\tCHECK_ARRAY_IDX\n" );
		      addLibItem( BasicLibrary.LibItem.CHECK_ARRAY_IDX );
		    }
		  }
		  BasicUtil.parseToken( iter, ',' );
		  pos = this.asmOut.length();
		  BasicExprParser.parseInt2Expr( this, iter, context );
		  Integer idx2 = BasicUtil.removeLastCodeIfConstExpr(
								this.asmOut,
								pos );
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
						+ idx2.intValue())
					* varDecl.getElemSize();
		      if( offs > 0 ) {
			this.asmOut.append( '+' );
			this.asmOut.appendHex4( offs );
		      }
		      this.asmOut.newLine();
		    } else {
		      /*
		       * Berechnung: idx1 * (dim2+1)
		       * HL: Idx1
		       */
		      if( varDecl.getDim2() == 1 ) {
			this.asmOut.append( "\tADD\tHL,HL\n" );
		      } else {
			this.asmOut.append_LD_DE_nn( varDecl.getDim2() + 1 );
			this.asmOut.append( "\tCALL\tI2_MUL_I2_I2\n" );
			addLibItem( BasicLibrary.LibItem.I2_MUL_I2_I2 );
		      }
		      // Idx2 addieren
		      if( idx2.intValue() > 0 ) {
			if( idx2.intValue() > 3 ) {
			  this.asmOut.append_LD_DE_nn( idx2.intValue() );
			  this.asmOut.append( "\tADD\tHL,DE\n" );
			} else {
			  for( int i = 0; i < idx2.intValue(); i++ ) {
			    this.asmOut.append( "\tINC\tHL\n" );
			  }
			}
		      }
		      // aus Index Elementadresse ermitteln
		      appendCodeIndexToElemAddr( varDecl );
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim2() + 1 );
		      this.asmOut.append( "\tCALL\tCHECK_ARRAY_IDX\n" );
		      addLibItem( BasicLibrary.LibItem.CHECK_ARRAY_IDX );
		    }
		    if( idx1 != null ) {
		      if( idx1.intValue() > 0 ) {
			this.asmOut.append_LD_DE_nn(
				  idx1.intValue() * (varDecl.getDim2() + 1) );
			this.asmOut.append( "\tADD\tHL,DE\n" );
		      }
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
						+ "\tCALL\tI2_MUL_I2_I2\n"
						+ "\tPUSH\tHL\n",
				varDecl.getDim2() + 1 ) );
			addLibItem( BasicLibrary.LibItem.I2_MUL_I2_I2 );
		      }
		      this.asmOut.append( "\tPOP\tDE\n"
					+ "\tADD\tHL,DE\n" );
		    }
		    // aus Index Elementadresse ermitteln
		    appendCodeIndexToElemAddr( varDecl );
		  }
		}
		BasicUtil.parseToken( iter, ')' );
	      } else {
		addrExpr = varDecl.getLabel();
	      }
	      varInfo = new SimpleVarInfo(
				varDecl.getDataType(),
				addrExpr,
				null );
	    } else {
	      if( callableEntry != null ) {
		throw new PrgException( "Implizite Variablendeklaration"
			+ " in einer Funktion/Prozedur nicht erlaubt" );
	      }
	      // implizite Variablendeklaration, aber nur im Hauptprogramm
	      if( this.options.getWarnImplicitDecls() ) {
		putWarning( "Implizite Deklaration der Variable \'"
						+ varName + "\'" );
	      }
	      DataType dataType = BasicUtil.getDefaultTypeBySuffix( varName );
	      varDecl           = new VarDecl(
					this.curSource,
					this.curBasicLineNum,
					varName,
					dataType );
	      updVarUsage( varDecl, accessMode );
	      this.name2GlobalVar.put( varName, varDecl );
	      varInfo = new SimpleVarInfo(
					dataType,
					varDecl.getLabel(),
					null );
	    }
	  }
	}
      }
    }
    return varInfo;
  }


  public String compile() throws IOException
  {
    String asmText       = null;
    this.execEnabled     = true;
    this.errCnt          = 0;
    this.resetErrCodePos = -1;
    this.stackFrameCode  = null;
    this.stackFramePos   = -1;
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
	AsmCodeBuf varBssBuf  = new AsmCodeBuf( 0x0400 );
	int        bssBegAddr = this.options.getBssBegAddr();
	if( (bssBegAddr < 0)
	    || (bssBegAddr > this.options.getCodeBegAddr()) )
	{
	  appendHeadTo( initBuf );
	}
	initBuf.append( "\n"
			+ "\tORG\t" );
	initBuf.appendHex4( this.options.getCodeBegAddr() );
	initBuf.newLine();
	if( !this.options.isAppTypeSubroutine() ) {
	  this.target.appendPrologTo(
				initBuf,
				this,
				this.options.getAppName() );
	}
	BasicLibrary.appendInitTo(
				initBuf,
				this,
				this.name2GlobalVar,
				this.usrLabels,
				varBssBuf );
	initBuf.append( this.asmOut );
	this.asmOut = initBuf;
	AsmCodeOptimizer.optimize2( this );
	if( libItems.containsKey( BasicLibrary.LibItem.DATA ) ) {
	  if( this.options.getShowAssemblerText() ) {
	    this.asmOut.append( "\n;DATA-Zeilen\n" );
	  }
	  this.asmOut.append( "DATA_POOL:\n" );
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
	    appendHeadTo( bssBuf );
	  }
	  bssBuf.append( "\n"
			+ "\tORG\t" );
	  bssBuf.appendHex4( bssBegAddr );
	  bssBuf.append( "\n" );
	}
	BasicLibrary.appendBssTo(
			bssBuf,
			this,
			varBssBuf,
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
    if( usesLibItem( BasicLibrary.LibItem.IO_DISK_HANDLER ) ) {
      rv = Math.max( rv, this.target.getDiskIOChannelSize() );
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
      if( (driver == IODriver.DISK) || (driver == IODriver.VDIP) ) {
	Set<Integer> modes2 = this.ioDriverModes.get( IODriver.FILE_ALL );
	if( modes2 != null ) {
	  rv.addAll( modes2 );
	}
      }
      if( driver != IODriver.ALL ) {
	Set<Integer> modes3 = this.ioDriverModes.get( IODriver.ALL );
	if( modes3 != null ) {
	  rv.addAll( modes3 );
	}
      }
    }
    return rv;
  }


  public PrgLogger getLogger()
  {
    return this.logger;
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


  public AsmCodeBuf getSysDataOut()
  {
    return this.sysDataOut;
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
    boolean rv = false;
    if( driver != null ) {
      rv = this.ioDriverModes.containsKey( driver )
		|| this.ioDriverModes.containsKey( IODriver.ALL );
      if( !rv ) {
	if( (driver == IODriver.DISK) || (driver == IODriver.VDIP) ) {
	  rv = this.ioDriverModes.containsKey( IODriver.FILE_ALL );
	}
      }
    }
    return rv;
  }


  public String lazyGetStringLiteralLabel( String text )
  {
    return this.str2Label.get( text );
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
	this.userData.append( '\n' );
      } else if( bss ) {
	if( this.userBSS == null ) {
	  this.userBSS = new StringBuilder( 0x0800 );
	}
	this.userBSS.append( text );
	this.userBSS.append( '\n' );
      } else {
	this.asmOut.append( text );
	this.asmOut.newLine();
      }
    } while( BasicUtil.checkComma( iter ) );
    if( isFunc ) {
      BasicUtil.parseToken( iter, ')' );
    }
  }


  public void parseCallableCall(
			CharacterIterator iter,
			ParseContext      context,
			CallableEntry     entry ) throws PrgException
  {
    if( this.options.getCheckStack()
	&& (this.options.getStackSize() > 0) )
    {
      this.asmOut.append_LD_DE_nn( entry.getTotalArgSize()
				+ entry.getTotalVarSize() );
      this.asmOut.append( BasicLibrary.CALL_CHECK_STACK_N );
      addLibItem( BasicLibrary.LibItem.CHECK_STACK );
    }
    int nArgs = entry.getArgCount();
    if( nArgs > 0 ) {
      BasicUtil.parseToken( iter, '(' );
      for( int i = 0; i < nArgs; i++ ) {
	if( i > 0 ) {
	  BasicUtil.parseToken( iter, ',' );
	}
	switch( entry.getArgType( i ) ) {
	  case INT2:
	    BasicExprParser.parseInt2Expr( this, iter, context );
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    break;
	  case INT4:
	    BasicExprParser.parseInt4Expr( this, iter, context );
	    this.asmOut.append_PUSH_DEHL();
	    break;
	  case DEC6:
	    BasicExprParser.parseDec6Expr( this, iter, context );
	    this.asmOut.append_PUSH_D6Accu( this );
	    break;
	  case STRING:
	    {
	      String text = BasicUtil.checkStringLiteral( this, iter );
	      if( text != null ) {
		this.asmOut.append( "\tLD\tHL," );
		this.asmOut.append( getStringLiteralLabel( text ) );
		this.asmOut.append( "\n"
				    + "\tPUSH\tHL\n" );
	      } else {
		SimpleVarInfo srcVar = checkVariable(
						iter,
						context,
						AccessMode.READ );
		if( srcVar != null ) {
		  srcVar.writeCode_LD_DE_VarValue( this );
		  this.asmOut.append( "\tCALL\tSTR_VAR_DUP\n"
					+ "\tPUSH\tDE\n" );
		  addLibItem( BasicLibrary.LibItem.STR_VAR_DUP );
		} else {
		  BasicExprParser.parseStringPrimExpr( this, iter, context );
		  this.asmOut.append( "\tCALL\tMALLOC_AND_STR_COPY\n"
					+ "\tPUSH\tHL\n" );
		  addLibItem( BasicLibrary.LibItem.MALLOC_AND_STR_COPY );
		}
	      }
	    }
	    break;
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

    int nStackWords = 0;
    for( int i = nArgs - 1; i >= 0; --i ) {
      switch( entry.getArgType( i ) ) {
	case INT2:
	  nStackWords++;
	  break;
	case INT4:
	  nStackWords += 2;
	  break;
	case DEC6:
	  nStackWords += 3;
	  break;
	case STRING:
	  appendCorrectStack( nStackWords );
	  nStackWords = 0;
	  this.asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tMFREE\n" );
	  addLibItem( BasicLibrary.LibItem.MFREE );
	  break;
      }
    }
    appendCorrectStack( nStackWords );
    if( entry instanceof FunctionEntry ) {
      switch( ((FunctionEntry) entry).getReturnType() ) {
	case INT2:
	  this.asmOut.append( "\tLD\tHL,(M_RETVAL)\n" );
	  addLibItem( BasicLibrary.LibItem.M_RETVAL_2 );
	  break;
	case INT4:
	  this.asmOut.append( "\tLD\tHL,(M_RETVAL)\n"
			+ "\tLD\tDE,(M_RETVAL+02H)\n" );
	  addLibItem( BasicLibrary.LibItem.M_RETVAL_4 );
	  break;
	case STRING:
	  this.asmOut.append( "\tLD\tHL,(M_RETVAL)\n" );
	  addLibItem( BasicLibrary.LibItem.M_RETVAL_2 );
	  this.gcRequired = true;
	  break;
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
			ParseContext      context,
			AtomicBoolean     constOut ) throws PrgException
  {
    Number channel = BasicUtil.readNumber( iter );
    if( channel != null ) {
      if( !(channel instanceof Integer) ) {
	BasicUtil.throwInt2ExprExpected();
      }
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
	  BasicUtil.throwIOChannelNumOutOfRange();
      }
      if( constOut != null ) {
	constOut.set( true );
      }
    } else {
      BasicExprParser.parseInt2Expr( this, iter, context );
      this.asmOut.append( "\tCALL\tIOCADR\n" );
      addLibItem( BasicLibrary.LibItem.IOCADR );
      addLibItem( BasicLibrary.LibItem.IOCTB1 );
      addLibItem( BasicLibrary.LibItem.IOCTB2 );
      if( constOut != null ) {
	constOut.set( false );
      }
    }
    setErrVarsSet();
  }


  public void putWarningLastDigitsIgnored( int nIgnoredDigits )
  {
    StringBuilder buf = new StringBuilder( 80 );
    buf.append( "Letzte" );
    if( nIgnoredDigits > 1 ) {
      buf.append( '\u0020' );
      buf.append( nIgnoredDigits );
    }
    buf.append( " Ziffer" );
    if( nIgnoredDigits != 1 ) {
      buf.append( 'n' );
    }
    buf.append( " der Zahl ignoriert, da der Datentyp"
		+ " keine so hohe Genauigkeit bietet." );
    putWarning( buf.toString() );
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


  public void removeLibItemComplete( BasicLibrary.LibItem libItem )
  {
    if( (libItem != null) && this.asmOut.isEnabled() )
      this.libItems.remove( libItem );
  }


  public void removeLibItemOnce( BasicLibrary.LibItem libItem )
  {
    if( (libItem != null) && this.asmOut.isEnabled() ) {
      Integer cnt = this.libItems.get( libItem );
      if( cnt != null ) {
	if( cnt.intValue() > 1 ) {
	  cnt = Integer.valueOf( cnt.intValue() - 1 );
	  this.libItems.put( libItem, cnt );
	} else {
	  this.libItems.remove( libItem );
	}
      }
    }
  }


  public void setErrVarsSet()
  {
    this.errVarsSet = true;
  }


  public boolean usesErrorVars()
  {
    return this.libItems.containsKey( BasicLibrary.LibItem.M_ERROR_NUM )
	   || this.libItems.containsKey( BasicLibrary.LibItem.M_ERROR_TEXT );
  }


  public boolean usesLibItem( BasicLibrary.LibItem libItem )
  {
    return this.libItems.containsKey( libItem );
  }


	/* --- private Methoden --- */

  private void addLibItem( BasicLibrary.LibItem libItem, int cnt )
  {
    if( (libItem != null) && this.asmOut.isEnabled() ) {
      Integer cntObj = this.libItems.get( libItem );
      if( cntObj != null ) {
	cntObj = Integer.valueOf( cntObj.intValue() + cnt );
      } else {
	cntObj = Integer.valueOf( cnt );
      }
      this.libItems.put( libItem, cntObj );
    }
  }


  private void addLibItems( Map<BasicLibrary.LibItem,Integer> srcItems )
  {
    if( srcItems != null ) {
      for( BasicLibrary.LibItem item : srcItems.keySet() ) {
	Integer cnt = srcItems.get( item );
	if( cnt != null ) {
	  addLibItem( item, cnt.intValue() );
	}
      }
    }
  }


  /*
   * Die Methode erzeugt den Code fuer die Berechnung der Elementadresse.
   *
   * Parameter:
   *   varDecl: Variablendeklaration
   *   HL:      absoluter Index
   * Rueckgabe:
   *   HL:      Adresse des duch HL angegebenen Elements
   */
  private void appendCodeIndexToElemAddr( VarDecl varDecl ) throws PrgException
  {
    int elemSize = varDecl.getElemSize();
    if( this.options.getCheckBounds() ) {
      int dimCount = varDecl.getDimCount();
      switch( dimCount ) {
	case 1:
	  this.asmOut.append_LD_BC_nn( varDecl.getDim1() + 1 );
	  break;
	case 2:
	  this.asmOut.append_LD_BC_nn(
			(varDecl.getDim1() + 1) * (varDecl.getDim2() + 1) );
	  break;
	default:
	  // sollte nie vorkommen
	  throw new PrgException(
		String.format( "Ung\u00FCltige Dimension: %d", dimCount ) );
      }
      this.asmOut.append_LD_DE_xx( varDecl.getLabel() );
      switch( elemSize ) {
	case 2:
	  this.asmOut.append( "\tCALL\tARRAY2_ELEM_ADDR\n" );
	  addLibItem( BasicLibrary.LibItem.ARRAY2_ELEM_ADDR );
	  break;
	case 4:
	  this.asmOut.append( "\tCALL\tARRAY4_ELEM_ADDR\n" );
	  addLibItem( BasicLibrary.LibItem.ARRAY4_ELEM_ADDR );
	  break;
	case 6:
	  this.asmOut.append( "\tCALL\tARRAY6_ELEM_ADDR\n" );
	  addLibItem( BasicLibrary.LibItem.ARRAY6_ELEM_ADDR );
	  break;
	default:
	  // sollte nie vorkommen
	  BasicUtil.throwInvalidElemSize( elemSize );
      }
    } else {
      this.asmOut.append_LD_DE_xx( varDecl.getLabel() );
      switch( elemSize ) {
	case 2:
	  this.asmOut.append( "\tADD\tHL,HL\n"
				+ "\tADD\tHL,DE\n" );
	  break;
	case 4:
	  this.asmOut.append( "\tADD\tHL,HL\n"
				+ "\tADD\tHL,HL\n"
				+ "\tADD\tHL,DE\n" );
	  break;
	case 6:
	  this.asmOut.append( "\tCALL\tARRAY6_ELEM_ADDR\n" );
	  addLibItem( BasicLibrary.LibItem.ARRAY6_ELEM_ADDR );
	  break;
	default:
	  // sollte nie vorkommen
	  BasicUtil.throwInvalidElemSize( elemSize );
      }
    }
  }


  private void appendCorrectStack( int nStackWords )
  {
    if( nStackWords > 0 ) {
      if( nStackWords > 4 ) {
	this.asmOut.append( "\tCALL\tADD_SP_N\n"
			+ "\tDB\t" );
	this.asmOut.appendHex2( nStackWords * 2 );
	this.asmOut.newLine();
	addLibItem( BasicLibrary.LibItem.ADD_SP_N );
      } else {
	for( int i = 0; i < nStackWords; i++ ) {
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
      }
    }
  }


  private void appendHeadTo( AsmCodeBuf buf )
  {
    if( this.options.getShowAssemblerText() ) {
      buf.append( ";\n"
		+ ";Dieser Quelltext wurde vom"
		+ " JKCEMU-BASIC-Compiler erzeugt.\n"
		+ ";http://www.jens-mueller.org/jkcemu/\n"
		+ ";\n" );
    }
    String appName = this.options.getAppName();
    if( appName != null ) {
      buf.append( "\n\tNAME\t\'" );
      buf.append( appName );
      buf.append( "\'\n" );
    }
  }


  private void appendLineNotFoundToErrLog(
		BasicLineExpr lineExpr,
		String        trailingMsg ) throws TooManyErrorsException
  {
    if( lineExpr != null ) {
      StringBuilder buf = new StringBuilder( 128 );
      if( lineExpr.appendMsgPrefixTo( EmuUtil.TEXT_ERROR, buf ) ) {
	buf.append( ": " );
      }
      if( lineExpr.isLabel() ) {
	buf.append( "Marke \'" );
	buf.append( lineExpr.getExprText() );
	buf.append( '\'' );
      } else {
	buf.append( "BASIC-Zeilennummer " );
	buf.append( lineExpr.getExprText() );
      }
      buf.append( " nicht gefunden" );
      if( trailingMsg != null ) {
	buf.append( trailingMsg );
      }
      buf.append( '\n' );
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
      buf.append( '\n' );
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


  private NumericValue checkSignedNumericLiteral( CharacterIterator iter )
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
    NumericValue rv = NumericValue.checkLiteral( this, iter, null, true );
    if( (rv != null) && neg ) {
      rv = rv.negate();
    }
    if( rv == null ) {
      iter.setIndex( pos );
    }
    return rv;
  }


  private SimpleVarInfo checkVariable(
				CharacterIterator iter,
				ParseContext      context,
				AccessMode        accessMode )
							throws PrgException
  {
    int           begPos  = iter.getIndex();
    SimpleVarInfo varInfo = checkVariable(
				iter,
				context,
				BasicUtil.checkIdentifier( iter ),
				accessMode );
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
	      && this.libItems.containsKey(
			BasicLibrary.LibItem.M_SOURCE_NAME ) )
	  {
	    this.asmOut.append( "\tLD\tHL,0000H\n"
				+ "\tLD\t(M_SOURCE_NAME),HL\n" );
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
		EmuUtil.TEXT_ERROR );
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


    // globale Variablen pruefen
    if( this.options.getWarnUnusedItems() ) {
      Collection<VarDecl> globalVars = name2GlobalVar.values();
      if( globalVars != null ) {
	for( VarDecl varDecl : globalVars ) {
	  String msg = null;
	  if( varDecl.isRead() ) {
	    if( !varDecl.isWritten() ) {
	      msg = MSG_VAR_NOT_WRITTEN;
	    }
	  } else if ( varDecl.isWritten() ) {
	    if( !varDecl.isRead() ) {
	      msg = MSG_VAR_NOT_READ;
	    }
	  } else {
	    msg = MSG_VAR_NOT_USED;
	  }
	  if( msg != null ) {
	    putWarning(
		varDecl,
		String.format( msg, varDecl.toString() ) );
	  }
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
		String msg = null;
		if( entry.isRead( varName ) ) {
		  if( !entry.isWritten( varName ) ) {
		    msg = MSG_VAR_NOT_WRITTEN;
		  }
		} else if ( entry.isWritten( varName ) ) {
		  if( !entry.isRead( varName ) ) {
		    msg = MSG_VAR_NOT_READ;
		  }
		} else {
		  msg = MSG_VAR_NOT_USED;
		}
		if( msg != null ) {
		  putWarning(
			entry.getVarSourcePos( varName ),
			String.format( msg, varName ) );
		}
	      }
	    }
	  }
	} else {
	  BasicSourcePos callSourcePos = entry.getFirstCallSourcePos();
	  appendLineNumMsgToErrLog(
			callSourcePos != null ? callSourcePos : entry,
			entry.toString() + " nicht implementiert",
			EmuUtil.TEXT_ERROR );
	  incErrorCount();
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
	      buf.append( iter.current() );
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
			EmuUtil.TEXT_ERROR );
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
		EmuUtil.TEXT_ERROR );
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
	      this.asmOut.append( "\tLD\t(M_SRC_LINE),HL\n" );
	    }
	  }
	  if( (this.curBasicLineNum >= 0)
	      && (this.curBasicLineNum < MAX_INT_VALUE) )
	  {
	    this.asmOut.append_LD_HL_nn( (int) this.curBasicLineNum );
	    this.asmOut.append( "\tLD\t(M_BASIC_LINE_NUM),HL\n" );
	    addLibItem( BasicLibrary.LibItem.M_BASIC_LINE_NUM );
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
      appendLineNumMsgToErrLog( msg, EmuUtil.TEXT_ERROR );
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
	buf.append( '\n' );
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
    this.errVarsSet      = false;
    this.tmpStrBufUsed   = false;
    this.resetErrCodePos = this.asmOut.length();;

    char ch = BasicUtil.skipSpaces( iter );
    if( (ch == '!') || (ch == '\'') ) {		// Kommentar
      iter.next();
      parseREM( iter );
    } else {
      if( this.caseExpected ) {
	int     idx       = iter.getIndex();
	boolean caseState = BasicUtil.checkKeyword( iter, "CASE" );
	iter.setIndex( idx );
	if( !caseState ) {
	  throw new PrgException( "CASE erwartet" );
	}
      }
      if( ch == '?' ) {
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
	    } else if( name.equals( "CASE" ) ) {
	      parseCASE( iter );
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
	    } else if( name.equals( "SELECT" ) ) {
	      parseSELECT( iter );
	    } else if( name.equals( "SWAP" ) ) {
	      parseSWAP( iter );
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
		  parseCallableCall( iter, new ParseContext(), entry );
		} else {
		  SimpleVarInfo varInfo = checkVariable(
						iter,
						new ParseContext(),
						name,
						AccessMode.WRITE );
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
    if( this.errVarsSet && (this.resetErrCodePos >= 0) ) {
      this.asmOut.insert(
			this.resetErrCodePos,
			BasicLibrary.CALL_RESET_ERROR );
      addLibItem( BasicLibrary.LibItem.RESET_ERROR );
    }
    try {
      if( (this.stackFrameCode != null) && (this.stackFramePos >= 0) ) {
	try {
	  this.asmOut.insert( this.stackFramePos, this.stackFrameCode );
	}
	catch( StringIndexOutOfBoundsException ex ) {
	  this.asmOut.append( this.stackFrameCode );
	}
      }
    }
    finally {
      this.stackFrameCode = null;
      this.stackFramePos  = -1;
    }
  }


  private void parseBORDER( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsBorderColor() ) {
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tCALL\tXBORDER\n" );
      addLibItem( BasicLibrary.LibItem.XBORDER );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseInt2Expr( this, iter );
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
      Number value = BasicUtil.readHex( iter );
      if( value == null ) {
	BasicUtil.throwHexDigitExpected();
      }
      if( value instanceof Long ) {
	BasicUtil.throwNumberTooBig();
      }
      if( value instanceof Integer ) {
	this.asmOut.append_LD_HL_nn( value.intValue() );
      } else {
	BasicUtil.throwHexDigitExpected();
      }
    } else {
      BasicExprParser.parseInt2Expr( this, iter );
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


  private void parseCASE( CharacterIterator iter ) throws PrgException
  {
    SelectEntry selectEntry = null;
    if( !this.structureStack.isEmpty() ) {
      BasicSourcePos entry = this.structureStack.peek();
      if( entry instanceof SelectEntry ) {
	selectEntry = (SelectEntry) entry;
      }
    }
    if( selectEntry == null ) {
      throw new PrgException( "CASE au\u00DFerhalb einer SELECT-Anweisung" );
    }
    if( this.caseExpected ) {
      this.caseExpected = false;
    } else {
      // naechstes CASE -> vorheriges abschliessen
      this.asmOut.append( "\tJP\t" );
      this.asmOut.append( selectEntry.getEndLabel() );
      this.asmOut.newLine();
      this.asmOut.append( selectEntry.getCaseLabel() );
      this.asmOut.append( ":\n" );
      selectEntry.setCaseLabel( nextLabel() );
    }
    if( BasicUtil.checkKeyword( iter, "ELSE" ) ) {
      if( selectEntry.isElseDone() ) {
	throw new PrgException( "Mehrfaches CASE ELSE nicht erlaubt" );
      }
      selectEntry.setElseDone();
    } else {
      if( selectEntry.isElseDone() ) {
	throw new PrgException(
			"CASE-Ausdruck nach CASE ELSE nicht erlaubt" );
      }
      java.util.List<Integer> values = new ArrayList<>();
      do {
	String       name  = null;
	NumericValue value = NumericValue.checkLiteral(
						this,
						iter,
						BasicCompiler.DataType.INT2,
						false );
	if( value == null ) {
	  name = BasicUtil.checkIdentifier( iter );
	  if( name != null ) {
	    value = BasicExprParser.checkConstant( this, name );
	  }
	}
	if( value == null ) {
	  BasicUtil.throwNoConstExpr();
	}
	if( value.getDataType() != BasicCompiler.DataType.INT2 ) {
	  BasicUtil.throwInt2ExprExpected();
	}
	if( selectEntry.checkUniqueCaseValue(
			  this,
			  name != null ? name : value.longValue() ) )
	{
	  values.add( value.intValue() );
	}
      } while( BasicUtil.checkComma( iter ) );
      int n = values.size();
      if( n == 1 ) {
	int v = values.get( 0 ).intValue();
	if( v == 0 ) {
	  this.asmOut.append( "\tLD\tA,D\n"
				+ "\tOR\tE\n" );
	} else {
	  this.asmOut.append_LD_HL_nn( v );
	  this.asmOut.append( "\tOR\tA\n"
				+ "\tSBC\tHL,DE\n" );
	}
	this.asmOut.append( "\tJP\tNZ," );
	this.asmOut.append( selectEntry.getCaseLabel() );
	this.asmOut.newLine();
      } else if( n > 1 ) {
	if( n > 255 ) {
	  throw new PrgException(
			"Zu viele Eintr\u00E4ge in der Konstantenliste" );
	}
	this.asmOut.append( "\tCALL\tI2_CONTAINS\n" );
	addLibItem( BasicLibrary.LibItem.I2_CONTAINS );
	String label = nextLabel();
	this.asmOut.append( "\tDW\t" );
	this.asmOut.append( label );
	this.asmOut.append( "\n"
			+ "\tJP\tC," );
	this.asmOut.append( selectEntry.getCaseLabel() );
	this.asmOut.newLine();
	this.sysDataOut.append( label );
	this.sysDataOut.append( ":\tDB\t" );
	this.sysDataOut.appendHex2( n );
	for( int i = 0; i < n; i++ ) {
	  if( (i & 0x03) == 0 ) {
	    this.sysDataOut.append( "\n"
			+ "\tDW\t" );
	  } else {
	    this.sysDataOut.append( ',' );
	  }
	  this.sysDataOut.appendHex4( values.get( i ).intValue() );
	}
	this.sysDataOut.newLine();
      } else {
	this.asmOut.append( "\tJR\t" );
	this.asmOut.append( selectEntry.getCaseLabel() );
	this.asmOut.newLine();
      }
    }
    this.separatorChecked = true;
  }


  private void parseCIRCLE( CharacterIterator iter ) throws PrgException
  {
    checkGraphicsSupported();
    parsePointToMem( iter, "CIRCLE_M_X", "CIRCLE_M_Y" );
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseInt2Expr( this, iter );
    this.asmOut.append( "\tLD\t(CIRCLE_M_R),HL\n"
		+ "\tCALL\tCIRCLE\n" );
    addLibItem( BasicLibrary.LibItem.CIRCLE );
  }


  private void parseCLOSE( CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '#' );
    parseIOChannelNumToPtrFldAddrInHL( iter, new ParseContext(), null );
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
      BasicExprParser.parseInt2Expr( this, iter );
      if( BasicUtil.checkComma( iter ) ) {
	int pos = this.asmOut.length();
	BasicExprParser.parseInt2Expr( this, iter );
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
	BasicExprParser.parseInt2Expr( this, iter );
	if( BasicUtil.checkComma( iter ) ) {
	  BasicExprParser.parseInt2Expr( this, iter );
	}
      }
      finally {
	popCodeCreationDisabled();
      }
    }
    if( BasicUtil.checkComma( iter ) ) {
      parseBORDER( iter );
    }
  }


  private void parseCURSOR( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsXCURSOR() ) {
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tCALL\tXCURSOR\n" );
      addLibItem( BasicLibrary.LibItem.XCURSOR );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseInt2Expr( this, iter );
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
	NumericValue value = checkSignedNumericLiteral( iter );
	if( value == null ) {
	  throw new PrgException( "Zahl oder String-Literal erwartet" );
	}
	this.dataOut.append( "\tDB\t" );
	if( value.getDataType().equals( BasicCompiler.DataType.DEC6 ) ) {
	  this.dataOut.appendHex2( DATA_DEC6 );
	  long d6Bits = value.dec6Bits();
	  for( int i = 0; i < 6; i++ ) {
	    this.dataOut.append( ',' );
	    this.dataOut.appendHex2( (int) (d6Bits >> 40) );
	    d6Bits <<= 8;
	  }
	} else {
	  int  t = DATA_INT4;
	  int  n = 4;
	  long v = value.longValue();
	  if( (v >= -128) && (v < 127) ) {
	    t = DATA_INT1;
	    n = 1;
	  } else if( (v >= -32768) && (v <= 32767) ) {
	    t = DATA_INT2;
	    n = 2;
	  }
	  this.dataOut.appendHex2( t );
	  for( int i = 0; i < n; i++ ) {
	    this.dataOut.append( ',' );
	    this.dataOut.appendHex2( (int) v );
	    v >>= 8;
	  }
	}
	this.dataOut.newLine();
      }
    } while( BasicUtil.checkComma( iter ) );
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
    boolean status = false;
    if( Character.toUpperCase( BasicUtil.skipSpaces( iter ) ) == 'U' ) {
      if( Character.toUpperCase( iter.next() ) == 'S' ) {
	if( Character.toUpperCase( iter.next() ) == 'R' ) {
	  iter.next();
	  status = true;
	}
      }
    }
    if( !status ) {
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
    BasicExprParser.parseInt2Expr( this, iter );
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
	BasicUtil.throwVarNameExpected();
      }
      checkVarName( varName );
      if( this.name2GlobalVar.get( varName ) != null ) {
	throw new PrgException(
		"Variable mit dem Namen bereits deklariert" );
      }
      if( this.name2Callable.get( varName ) != null ) {
	throw new PrgException(
		"Funktion/Prozedur mit dem Namen bereits deklariert" );
      }
      int dim1 = 0;
      int dim2 = 0;
      if( BasicUtil.checkToken( iter, '(' ) ) {
	dim1 = BasicUtil.parseInt2Number( iter );
	if( dim1 < 1 ) {
	  BasicUtil.throwDimTooSmall();
	}
	if( BasicUtil.checkComma( iter ) ) {
	  dim2 = BasicUtil.parseInt2Number( iter );
	  if( dim2 < 1 ) {
	    BasicUtil.throwDimTooSmall();
	  }
	}
	BasicUtil.parseToken( iter, ')' );
      }
      this.name2GlobalVar.put(
		varName,
		new VarDecl(
			this.curSource,
			this.curBasicLineNum,
			varName,
			BasicUtil.parseTypeDecl( varName, iter ),
			dim1,
			dim2 ) );
    } while( BasicUtil.checkComma( iter ) );
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
    BasicExprParser.parseInt2Expr( this, iter );
    BasicUtil.parseToken( iter, ',' );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
    if( addr != null ) {
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\t(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( "),HL\n" );
    } else {
      pos = this.asmOut.length();
      BasicExprParser.parseInt2Expr( this, iter );
      Integer value = BasicUtil.removeLastCodeIfConstExpr(
							this.asmOut,
							pos );
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
      } else if( BasicExprParser.checkParseStringPrimVarExpr(
						this,
						iter,
						new ParseContext() ) )
      {
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
	  this.resetErrCodePos = -1;
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
	  this.resetErrCodePos = this.asmOut.length();
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
	this.resetErrCodePos = this.asmOut.length();
	ifEntry.setElseLabel( nextLabel() );	// neue ELSE-Marke erzeugen
      }
    }
    parseIForELSEIF( iter, ifEntry );
  }


  private void parseEND( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "SELECT" ) ) {
      SelectEntry selectEntry = null;
      if( !this.structureStack.isEmpty() ) {
	BasicSourcePos entry = this.structureStack.pop();
	if( entry instanceof SelectEntry ) {
	  selectEntry = (SelectEntry) entry;
	} else {
	  this.structureStack.push( entry );
	}
      }
      if( selectEntry == null ) {
	throw new PrgException(
		"END SELECT au\u00DFerhalb einer SELECT-Anweisung" );
      }
      this.asmOut.append( selectEntry.getCaseLabel() );
      this.asmOut.append( ":\n" );
      this.asmOut.append( selectEntry.getEndLabel() );
      this.asmOut.append( ":\n" );
    } else {
      CallableEntry callableEntry = getEnclosingCallableEntry();
      if( BasicUtil.checkKeyword( iter, "FUNCTION" ) ) {
	boolean ok = false;
	if( callableEntry != null ) {
	  if( callableEntry instanceof FunctionEntry ) {

	    // Rueckgabewert
	    int iyOffs = ((FunctionEntry) callableEntry).getReturnVarIYOffs();
	    switch( ((FunctionEntry) callableEntry).getReturnType() ) {
	      case INT2:
	      case STRING:
		this.asmOut.append_LD_HL_IndirectIY( iyOffs );
		this.asmOut.append( "\tLD\t(M_RETVAL),HL\n" );
		addLibItem( BasicLibrary.LibItem.M_RETVAL_2 );
		break;
	      case INT4:
		this.asmOut.append_LD_HL_IndirectIY( iyOffs );
		this.asmOut.append( "\tLD\t(M_RETVAL),HL\n" );
		this.asmOut.append_LD_HL_IndirectIY( iyOffs + 2 );
		this.asmOut.append( "\tLD\t(M_RETVAL+02H),HL\n" );
		addLibItem( BasicLibrary.LibItem.M_RETVAL_4 );
		break;
	      case DEC6:
		this.asmOut.append_LD_D6Accu_IndirectIY( iyOffs, this );
		break;
	    }
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

	// alle Strukturen abgeschlossen?
	while( !this.structureStack.isEmpty() ) {
	  BasicSourcePos entry = this.structureStack.pop();
	  if( entry != callableEntry ) {
	    appendLineNumMsgToErrLog(
			entry,
			entry.toString() + " nicht abgeschlossen",
			EmuUtil.TEXT_ERROR );
	    this.errCnt++;
	  }
	}
	if( callableEntry.hasStackFrame() ) {
	  int nVars = callableEntry.getVarCount();

	  // lokale String-Variablen freigeben
	  int nArgs = callableEntry.getArgCount();
	  if( nArgs > 0 ) {
	    for( int i = 0; i < nArgs; i++ ) {
	      if( callableEntry.getArgType( i ).equals( DataType.STRING ) ) {
		this.asmOut.append_LD_DE_IndirectIY(
				callableEntry.getArgIYOffs( i ) );
		this.asmOut.append( "\tCALL\tMFREE\n" );
		addLibItem( BasicLibrary.LibItem.MFREE );
	      }
	    }
	  }
	  if( nVars > 0 ) {
	    for( int i = 0; i < nVars; i++ ) {
	      if( callableEntry.getVarType( i ).equals( DataType.STRING ) ) {
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
    SimpleVarInfo varInfo = checkVariable(
				iter,
				new ParseContext(),
				AccessMode.READ_WRITE );
    if( varInfo != null ) {
      if( !varInfo.getDataType().equals( DataType.INT2 ) ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      throw new PrgException( "Integer-Variable erwartet" );
    }
    if( !varInfo.hasStaticAddr() ) {
      throw new PrgException(
		"Laufvariable darf keine Feldvariable mit variabler"
						+ " Indexangabe sein." );
    }
    parseAssignment( iter, varInfo );
    if( !BasicUtil.checkKeyword( iter, "TO" ) ) {
      throw new PrgException( "TO erwartet" );
    }
    int toPos = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
    Integer toValue = BasicUtil.removeLastCodeIfConstExpr(
						this.asmOut,
						toPos );
    if( toValue == null ) {
      this.asmOut.append( "\tPUSH\tHL\n" );
    }
    Integer stepValue = null;
    if( BasicUtil.checkKeyword( iter, "STEP" ) ) {
      int stepPos = this.asmOut.length();
      BasicExprParser.parseInt2Expr( this, iter );
      stepValue = BasicUtil.removeLastCodeIfConstExpr(
						this.asmOut,
						stepPos );
      if( stepValue == null ) {
	this.asmOut.append( "\tPUSH\tHL\n" );
      }
    } else {
      stepValue = 1;
    }
    String loopLabel = nextLabel();
    this.asmOut.append( loopLabel );
    this.asmOut.append( ':' );
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
      this.asmOut.append( BasicLibrary.CALL_CHECK_STACK );
      addLibItem( BasicLibrary.LibItem.CHECK_STACK );
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
    int                               srcBegPos     = iter.getIndex();
    int                               condBegPos    = this.asmOut.length();
    Map<BasicLibrary.LibItem,Integer> condLibItems  = null;
    Map<BasicLibrary.LibItem,Integer> tmpLibItems   = this.libItems;
    try {
      this.libItems = new HashMap<>();
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\tA,H\n"
			+ "\tOR\tL\n" );
    }
    finally {
      condLibItems  = this.libItems;
      this.libItems = tmpLibItems;
    }
    BasicUtil.skipSpaces( iter );
    int srcNextPos = iter.getIndex();

    // Pruefen, ob die Bedingung einen konstanten Wert hat
    Integer condValue = null;
    try {
      iter.setIndex( srcBegPos );
      this.libItems = new HashMap<>();
      condValue     = BasicExprParser.checkParseInt2ConstExpr( this, iter );
      BasicUtil.skipSpaces( iter );
      if( iter.getIndex() != srcNextPos ) {
	// Ausdruck ist laenger als der fuer konstant erkannte Teil
	condValue = null;
      }
    }
    finally {
      this.libItems = tmpLibItems;
      iter.setIndex( srcNextPos );
    }
    if( condValue != null ) {
      this.asmOut.setLength( condBegPos );
    } else {
      addLibItems( condLibItems );
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
	  BasicUtil.throwBasicLineExprExpected();
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
	  boolean                optimized = false;
	  java.util.List<String> lines     = this.asmOut.getLinesAsList(
								condBegPos );
	  if( lines.size() == 5 ) {
	    String line0 = lines.get( 0 );
	    String line1 = lines.get( 1 );
	    String line2 = lines.get( 2 );
	    if( line0.startsWith( "\tLD\tHL," )
		&& line1.startsWith( "\tLD\tDE," ) )
	    {
	      if( line2.equals( "\tCALL\tI2_LT_I2_I2\n" ) ) {
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.cut( condBegPos );
		  this.asmOut.append( line0 );
		  this.asmOut.append( "\tBIT\t7,H\n" );
		  this.asmOut.append( jmpInstr );
		  this.asmOut.append( "Z," );
		  this.asmOut.append( elseLabel );
		  this.asmOut.newLine();
		  removeLibItemOnce( BasicLibrary.LibItem.I2_LT_I2_I2 );
		  optimized = true;
		}
	      }
	      else if( line2.equals( "\tCALL\tI2_GE_I2_I2\n" ) ) {
		if( line1.equals( "\tLD\tDE,0000H\n" ) ) {
		  this.asmOut.cut( condBegPos );
		  this.asmOut.append( line0 );
		  this.asmOut.append( "\tBIT\t7,H\n" );
		  this.asmOut.append( jmpInstr );
		  this.asmOut.append( "NZ," );
		  this.asmOut.append( elseLabel );
		  this.asmOut.newLine();
		  removeLibItemOnce( BasicLibrary.LibItem.I2_GE_I2_I2 );
		  optimized = true;
		}
	      }
	      else if( line2.equals( "\tCALL\tI2_EQ_I2_I2\n" ) ) {
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
		removeLibItemOnce( BasicLibrary.LibItem.I2_EQ_I2_I2 );
		optimized = true;
	      }
	      else if( line2.equals( "\tCALL\tI2_NE_I2_I2\n" ) ) {
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
		removeLibItemOnce( BasicLibrary.LibItem.I2_NE_I2_I2 );
		optimized = true;
	      }
	    }
	  }
	  if( !optimized ) {
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
	  this.asmOut.append( "\tLD\t(M_SOURCE_NAME),HL\n" );
	  addLibItem( BasicLibrary.LibItem.M_SOURCE_NAME );
	}
      }
    }
  }


  private void parseINK( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tCALL\tXINK\n" );
      addLibItem( BasicLibrary.LibItem.XINK );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseInt2Expr( this, iter );
      }
      finally {
	popCodeCreationDisabled();
      }
    }
  }


  private void parseINPUT( CharacterIterator iter ) throws PrgException
  {
    if( BasicUtil.checkToken( iter, '#' ) ) {
      parseIOChannelNumToPtrFldAddrInHL( iter, new ParseContext(), null );
      int begPos_IO_M_CADDR = this.asmOut.length();
      this.asmOut.append( "\tLD\t(IO_M_CADDR),HL\n" );
      int endPos_IO_M_CADDR = this.asmOut.length();
      BasicUtil.parseToken( iter, ',' );
      this.asmOut.append_CALL_RESET_ERROR( this );
      boolean multiVars = false;
      for(;;) {
	this.asmOut.append( "\tLD\tC,2CH\n"	// Komma als Trennzeichen
			+ "\tCALL\tIOINL\n" );
	addLibItem( BasicLibrary.LibItem.IOINL );
	int           pos     = this.asmOut.length();
	SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.WRITE );
	if( varInfo == null ) {
	  BasicUtil.throwVarExpected();
	}
	varInfo.ensureAddrInHL( this.asmOut );
	String varAddrCode = this.asmOut.cut( pos );
	switch( varInfo.getDataType() ) {
	  case INT2:
	    this.asmOut.append( "\tCALL\tF_I2_VAL_DEC_S\n" );
	    addLibItem( BasicLibrary.LibItem.F_I2_VAL_DEC_S );
	    if( BasicUtil.isSingleInst_LD_HL_xx( varAddrCode ) ) {
	      this.asmOut.append( "\tEX\tDE,HL\n" );
	      this.asmOut.append( varAddrCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tHL\n" );
	      this.asmOut.append( varAddrCode );
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tLD\t(HL),E\n"
				+ "\tINC\tHL\n"
				+ "\tLD\t(HL),D\n" );
	    break;
	  case INT4:
	    this.asmOut.append( "\tCALL\tF_I4_VAL_DEC_S\n" );
	    if( BasicUtil.isSingleInst_LD_HL_xx( varAddrCode ) ) {
	      this.asmOut.append( "\tLD\tB,H\\n"
				+ "\tLD\tC,L\n" );
	      this.asmOut.append( varAddrCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tDE\n"
				+ "\tPUSH\tHL\n" );
	      this.asmOut.append( varAddrCode );
	      this.asmOut.append( "\tPOP\tBC\n"
				+ "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tLD_MEM_DEBC\n" );
	    addLibItem( BasicLibrary.LibItem.LD_MEM_DEBC );
	    break;
	  case DEC6:
	    this.asmOut.append( "\tCALL\tF_D6_VAL_S\n" );
	    addLibItem( BasicLibrary.LibItem.F_D6_VAL_S );
	    this.asmOut.append( varAddrCode );
	    this.asmOut.append( "\tCALL\tD6_LD_MEM_ACCU\n" );
	    addLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU );
	    break;
	  case STRING:
	    {
	      String newVarAddrCode = BasicUtil.convertCodeToValueInDE(
							varAddrCode );
	      if( newVarAddrCode != null ) {
		this.asmOut.append( newVarAddrCode );
	      } else {
		this.asmOut.append( "\tPUSH\tHL\n" );
		this.asmOut.append( varAddrCode );
		this.asmOut.append( "\tEX\tDE,HL\n"
					+ "\tPOP\tHL\n" );
	      }
	      this.asmOut.append( "\tCALL\tASSIGN_STR_TO_NEW_MEM_VS\n" );
	      addLibItem( BasicLibrary.LibItem.ASSIGN_STR_TO_NEW_MEM_VS );
	    }
	    break;
	}
	if( !BasicUtil.checkComma( iter ) ) {
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
	SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.WRITE );
	if( varInfo == null ) {
	  if( text != null ) {
	    BasicUtil.throwVarExpected();
	  }
	  BasicUtil.throwStringLitOrVarExpected();
	}
	switch( varInfo.getDataType() ) {
	  case INT2:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tINPUT_V_I2\n" );
	    if( this.options.getPreferRelativeJumps() ) {
	      this.asmOut.append( "\tJR\tC," );
	    } else {
	      this.asmOut.append( "\tJP\tC," );
	    }
	    this.asmOut.append( retryLabel );
	    this.asmOut.newLine();
	    addLibItem( BasicLibrary.LibItem.INPUT_V_I2 );
	    break;
	  case INT4:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tINPUT_V_I4\n" );
	    if( this.options.getPreferRelativeJumps() ) {
	      this.asmOut.append( "\tJR\tC," );
	    } else {
	      this.asmOut.append( "\tJP\tC," );
	    }
	    this.asmOut.append( retryLabel );
	    this.asmOut.newLine();
	    addLibItem( BasicLibrary.LibItem.INPUT_V_I4 );
	    break;
	  case DEC6:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tINPUT_V_D6\n" );
	    if( this.options.getPreferRelativeJumps() ) {
	      this.asmOut.append( "\tJR\tC," );
	    } else {
	      this.asmOut.append( "\tJP\tC," );
	    }
	    this.asmOut.append( retryLabel );
	    this.asmOut.newLine();
	    addLibItem( BasicLibrary.LibItem.INPUT_V_D6 );
	    break;
	  case STRING:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tINPUT_V_S\n" );
	    addLibItem( BasicLibrary.LibItem.INPUT_V_S );
	    break;
	}
      } while( BasicUtil.checkToken( iter, ';' ) );
    }

    /*
     * Da ein evtl. notwendiger Aufruf von RESET_ERROR bereits erfolgt ist
     * bzw. in den Routinen zum Einlesen einer Variable erfolgt,
     * muss das nicht noch am Anfang der Anweisung geschehen.
     */
    this.errVarsSet = false;
  }


  private void parseLET( CharacterIterator iter ) throws PrgException
  {
    String varName = BasicUtil.checkIdentifier( iter );
    if( varName == null ) {
      BasicUtil.throwVarExpected();
    }
    if( !checkReturnValueAssignment( iter, varName ) ) {
      SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					varName,
					AccessMode.WRITE );
      if( varInfo != null ) {
	parseAssignment( iter, varInfo );
      } else {
	BasicUtil.throwVarExpected();
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
	if( !BasicExprParser.checkParseStringPrimVarExpr(
						this,
						iter,
						new ParseContext() ) )
	{
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
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_BX),HL\n" );
      BasicUtil.parseToken( iter, ',' );
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_BY),HL\n" );
      if( enclosed ) {
	BasicUtil.parseToken( iter, ')' );
	BasicUtil.parseToken( iter, '-' );
	BasicUtil.parseToken( iter, '(' );
      } else {
	BasicUtil.parseToken( iter, ',' );
      }
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_EX),HL\n" );
      BasicUtil.parseToken( iter, ',' );
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\t(LINE_M_EY),HL\n" );
      if( enclosed ) {
	BasicUtil.parseToken( iter, ')' );
      }
      if( BasicUtil.checkComma( iter ) ) {
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
	BasicUtil.throwVarNameExpected();
      }
      checkVarName( varName );
      if( varName.equals( entry.getName() ) ) {
	throw new PrgException( "Name der Funktion/Prozedur"
			+ " als Variablenname nicht zul\u00E4ssig" );
      }
      entry.addVar(
		this.curSource,
		this.curBasicLineNum,
		varName,
		BasicUtil.parseTypeDecl( varName, iter ) );
    } while( BasicUtil.checkComma( iter ) );
  }


  private void parseLOCATE( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parse2Int2ArgsTo_DE_HL( this, iter, new ParseContext() );
    if( this.target.supportsXLOCATE() ) {
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
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ," + doEntry.getLoopLabel() + "\n" );
    } else if( BasicUtil.checkKeyword( iter, "WHILE" ) ) {
      BasicExprParser.parseInt2Expr( this, iter );
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
      addLibItem( BasicLibrary.LibItem.M_XYPOS );
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
      SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.READ_WRITE );
      this.asmOut.setLength( len );
      if( varInfo == null ) {
	throw new PrgException( "Variable oder Ende der Anweisung erwartet" );
      }
      if( !varInfo.equals( forEntry.getSimpleVarInfo() ) ) {
	throw new PrgException( "Variable stimmt nicht mit der bei der"
			+ " FOR-Anweisung angegebenen \u00FCberein." );
      }
    }
    forEntry.getSimpleVarInfo().ensureAddrInHL( this.asmOut );
    int     stackItems = 0;
    Integer stepValue  = forEntry.getStepValue();
    if( stepValue == null ) {
      this.asmOut.append( "\tPOP\tBC\n" );
    }
    Integer toValue = forEntry.getToValue();
    if( toValue != null ) {
      this.asmOut.append_LD_DE_nn( toValue.intValue() );
    } else {
      this.asmOut.append( "\tPOP\tDE\n" );
      this.asmOut.append( "\tPUSH\tDE\n" );
      stackItems++;
    }
    if( stepValue != null ) {
      if( stepValue.intValue() < 0 ) {
	if( stepValue.intValue() == -1 ) {
	  this.asmOut.append( "\tCALL\tNEXT_DEC\n" );
	  addLibItem( BasicLibrary.LibItem.NEXT_DEC );
	} else {
	  this.asmOut.append_LD_BC_nn( stepValue.intValue() );
	  this.asmOut.append( "\tCALL\tNEXT_DOWN\n" );
	  addLibItem( BasicLibrary.LibItem.NEXT_DOWN );
	}
      } else {
	if( stepValue.intValue() == 1 ) {
	  this.asmOut.append( "\tCALL\tNEXT_INC\n" );
	  addLibItem( BasicLibrary.LibItem.NEXT_INC );
	} else {
	  this.asmOut.append_LD_BC_nn( stepValue.intValue() );
	  this.asmOut.append( "\tCALL\tNEXT_UP\n" );
	  addLibItem( BasicLibrary.LibItem.NEXT_UP );
	}
      }
    } else {
      this.asmOut.append( "\tPUSH\tBC\n"
			+ "\tCALL\tNEXT_N\n" );
      addLibItem( BasicLibrary.LibItem.NEXT_N );
      stackItems++;
    }
    this.asmOut.append( "\tJP\tC," );
    this.asmOut.append( forEntry.getLoopLabel() );
    this.asmOut.newLine();
    this.asmOut.append( forEntry.getExitLabel() );
    this.asmOut.append( ":\n" );
    while( stackItems > 0 ) {
      this.asmOut.append( "\tPOP\tHL\n" );
      --stackItems;
    }
  }


  private void parseON( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseInt2Expr( this, iter );
    if( BasicUtil.checkKeyword( iter, "GOSUB" ) ) {
      this.asmOut.append( "\tCALL\tGET_ON_GO_ADDR\n" );
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
      addLibItem( BasicLibrary.LibItem.GET_ON_GO_ADDR );
    } else if( BasicUtil.checkKeyword( iter, "GOTO" ) ) {
      this.asmOut.append( "\tCALL\tGET_ON_GO_ADDR\n" );
      parseLineExprList( iter );
      String label = nextLabel();
      this.asmOut.append( "\tJR\tZ," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
		+ "\tJP\t(HL)\n" );
      this.asmOut.append( label );
      this.asmOut.append( ":\n" );
      addLibItem( BasicLibrary.LibItem.GET_ON_GO_ADDR );
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
      } else if( text.startsWith( "LPT:" ) ) {
	driver = IODriver.LPT;
      } else if( text.startsWith( "V:" ) ) {
	driver = IODriver.VDIP;
      } else if( this.target.startsWithDiskDevice( text ) ) {
	driver = IODriver.DISK;
      } else {
	int pos = text.indexOf( ':' );
	if( pos >= 0 ) {
	  driver = null;
	  putWarning( "Unbekanntes Ger\u00E4t" );
	} else {
	  // Dateizugriff ohne Laufwerksbuchstabe
	  if( this.target.getDiskHandlerLabel() != null ) {
	    if( this.options.isOpenDiskEnabled()
		&& (this.target.getVdipHandlerLabel() == null) )
	    {
	      /*
	       * Wenn das Zielsystem keinen eigenen VDIP-Treiber bietet,
	       * dafuer aber einen DISK-Treiber hat, der auch aktiviert ist,
	       * gehen Dateizugriff ohne Laufwerksbuchstabe
	       * immer auf den DISK-Treiber.
	       */
	      driver = IODriver.DISK;
	    } else {
	      driver = IODriver.FILE_ALL;
	    }
	  } else {
	    driver = IODriver.VDIP;
	  }
	}
      }
      if( driver != null ) {
	switch( driver ) {
	  case CRT:
	    if( !this.options.isOpenCrtEnabled() ) {
	      putWarning( "CRT-Treiber in den Compiler-Optionen"
				+ " deaktiviert" );
	    }
	    break;
	  case LPT:
	    if( !this.options.isOpenLptEnabled() ) {
	      putWarning( "LPT-Treiber in den Compiler-Optionen"
				+ " deaktiviert" );
	    }
	    break;
	  case DISK:
	    if( !this.options.isOpenDiskEnabled() ) {
	      putWarning( "DISK-Treiber in den Compiler-Optionen"
				+ " deaktiviert" );
	    }
	    break;
	  case VDIP:
	    if( !this.options.isOpenVdipEnabled() ) {
	      putWarning( "VDIP-Treiber in den Compiler-Optionen"
				+ " deaktiviert" );
	    }
	    break;
	  case FILE_ALL:
	    if( !this.options.isOpenDiskEnabled()
		&& !this.options.isOpenVdipEnabled() )
	    {
	      putWarning( "Alle Treiber f\u00FCr Dateizugriffe"
			+ " (DISK und VDIP) in den Compiler-Optionen"
			+ " deaktiviert" );
	    }
	    break;
	  case ALL:
	    if( !this.options.isOpenCrtEnabled()
		&& !this.options.isOpenLptEnabled()
		&& !this.options.isOpenDiskEnabled()
		&& !this.options.isOpenVdipEnabled() )
	    {
	      putWarning( "Alle Treiber in den Compiler-Optionen"
			+ " deaktiviert" );
	    }
	    break;
	}
      }
    } else {
      if( !BasicExprParser.checkParseStringPrimVarExpr(
						this,
						iter,
						new ParseContext() ) )
      {
	BasicUtil.throwStringExprExpected();
      }
    }
    if( (driver == IODriver.ALL)
	&& !this.options.isOpenCrtEnabled()
	&& !this.options.isOpenLptEnabled()
	&& !this.options.isOpenDiskEnabled()
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
    parseIOChannelNumToPtrFldAddrInHL( iter, new ParseContext(), null );
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
    if( driver != null ) {
      Set<Integer> driverModes = this.ioDriverModes.get( driver );
      if( driverModes == null ) {
	driverModes = new TreeSet<>();
      }
      driverModes.add( ioMode );
      this.ioDriverModes.put( driver, driverModes );
    }
    addLibItem( BasicLibrary.LibItem.IOOPEN );
  }


  private void parseOUT( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = BasicUtil.checkToken( iter, '(' );
    int     pos      = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
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
    BasicExprParser.parseInt2Expr( this, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
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
      BasicExprParser.parseInt2Expr( this, iter );
      this.asmOut.append( "\tCALL\tXPAPER\n" );
      addLibItem( BasicLibrary.LibItem.XPAPER );
    } else {
      try {
	pushCodeCreationDisabled();
	BasicExprParser.parseInt2Expr( this, iter );
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
      BasicExprParser.parseInt2Expr( this, iter );
      Integer value = BasicUtil.removeLastCodeIfConstExpr(
							this.asmOut,
							pos );
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
    BasicExprParser.parseInt2Expr( this, iter );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
    BasicUtil.parseToken( iter, ',' );
    pos = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
    if( addr != null ) {
      if( value != null ) {
	this.asmOut.append_LD_A_n( value.intValue() );
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInA( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tLD\tA,L\n" );
	}
      }
      this.asmOut.append( "\tLD\t(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( "),A\n" );
    } else {
      if( value != null ) {
	this.asmOut.append( "\tLD\t(HL)," );
	this.asmOut.appendHex2( value.intValue() );
	this.asmOut.newLine();
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInA( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tLD\tA,L\n"
			+ "\tPOP\tHL\n" );
	}
	this.asmOut.append( "\tLD\t(HL),A\n" );
      }
    }
  }


  private void parsePEN( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
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
	this.asmOut.append( "\tCALL\tIO_PRINT_NL\n" );
	addLibItem( BasicLibrary.LibItem.IO_PRINT_NL );
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
      SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.WRITE );
      if( varInfo != null ) {
	switch( varInfo.getDataType() ) {
	  case INT2:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDATA_READ_I2\n" );
	    addLibItem( BasicLibrary.LibItem.DATA_READ_I2 );
	    break;
	  case INT4:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDATA_READ_I4\n" );
	    addLibItem( BasicLibrary.LibItem.DATA_READ_I4 );
	    break;
	  case DEC6:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDATA_READ_D6\n" );
	    addLibItem( BasicLibrary.LibItem.DATA_READ_D6 );
	    break;
	  case STRING:
	    varInfo.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tDATA_READ_S\n" );
	    addLibItem( BasicLibrary.LibItem.DATA_READ_S );
	    break;
	}
      } else {
	BasicUtil.throwVarExpected();
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
	BasicUtil.throwBasicLineExprExpected();
      }
      this.asmOut.append( "\tCALL\tDATA_INIT\n" );
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
		+ "\tJP\tNZ,E_RET_WITHOUT_GOSUB\n" );
      addLibItem( BasicLibrary.LibItem.E_RET_WITHOUT_GOSUB );
    }
    this.asmOut.append( "\tRET\n" );
  }


  private void parseSCREEN( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseInt2Expr( this, iter );
    this.asmOut.append( "\tCALL\tSCREEN\n" );
    addLibItem( BasicLibrary.LibItem.SCREEN );
  }


  private void parseSELECT( CharacterIterator iter ) throws PrgException
  {
    if( !BasicUtil.checkKeyword( iter, "CASE" ) ) {
      throw new PrgException( "CASE erwartet" );
    }
    int pos = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
    String oldCode = this.asmOut.cut( pos );
    String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
    if( newCode != null ) {
      this.asmOut.append( newCode );
    } else {
      this.asmOut.append( oldCode );
      this.asmOut.append( "\tEX\tDE,HL\n" );
    }
    this.caseExpected = true;
    this.structureStack.push(
		new SelectEntry(
			this.curSource,
			this.curBasicLineNum,
			nextLabel(),
			nextLabel() ) );
  }


  private void parseSWAP( CharacterIterator iter ) throws PrgException
  {
    String varName1 = BasicUtil.checkIdentifier( iter );
    if( varName1 == null ) {
      BasicUtil.throwVarExpected();
    }
    SimpleVarInfo varInfo1 = checkVariable(
					iter,
					new ParseContext(),
					varName1,
					AccessMode.READ_WRITE );
    if( varInfo1 == null ) {
      BasicUtil.throwVarExpected();
    }
    BasicUtil.parseToken( iter, ',' );
    int pos = this.asmOut.length();
    String varName2 = BasicUtil.checkIdentifier( iter );
    if( varName2 == null ) {
      BasicUtil.throwVarExpected();
    }
    SimpleVarInfo varInfo2 = checkVariable(
					iter,
					new ParseContext(),
					varName2,
					AccessMode.READ_WRITE );
    if( varInfo2 == null ) {
      BasicUtil.throwVarExpected();
    }
    if( !varInfo1.getDataType().equals( varInfo2.getDataType() ) ) {
      throw new PrgException( "Datentypen stimmen nicht \u00FCberein" );
    }
    int    typeSize  = BasicUtil.getDataTypeSize( varInfo1.getDataType() );
    String addrExpr1 = varInfo1.getStaticAddrExpr();
    String addrExpr2 = varInfo2.getStaticAddrExpr();
    if( (addrExpr1 != null) && (addrExpr2 != null) && (typeSize == 2) ) {
      // ohne Unterprogramm direkt tauschen
      this.asmOut.append( "\tLD\tDE,(" );
      this.asmOut.append( addrExpr1 );
      this.asmOut.append( ")\n"
			+ "\tLD\tHL,(" );
      this.asmOut.append( addrExpr2 );
      this.asmOut.append( ")\n"
			+ "\tLD\t(" );
      this.asmOut.append( addrExpr1 );
      this.asmOut.append( "),HL\n"
			+ "\tLD\t(" );
      this.asmOut.append( addrExpr2 );
      this.asmOut.append( "),DE\n" );
    } else {
      if( varInfo1.hasStaticAddr() ) {
	varInfo2.ensureAddrInHL( this.asmOut );
	varInfo1.ensureStaticAddrInDE( this.asmOut, true );
      } else {
	if( varInfo2.hasStaticAddr() ) {
	  varInfo2.ensureStaticAddrInDE( this.asmOut, true );
	} else {
	  this.asmOut.insert( pos, "\tPUSH\tHL\n" );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
      }
      this.asmOut.replaceEnd(
			"\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n",
			"\tPOP\tDE\n" );
      switch( typeSize ) {
	case 2:
	  this.asmOut.append( "\tCALL\tSWAP2\n" );
	  addLibItem( BasicLibrary.LibItem.SWAP2 );
	  break;
	case 4:
	  this.asmOut.append( "\tCALL\tSWAP4\n" );
	  addLibItem( BasicLibrary.LibItem.SWAP4 );
	  break;
	case 6:
	  this.asmOut.append( "\tCALL\tSWAP6\n" );
	  addLibItem( BasicLibrary.LibItem.SWAP6 );
	  break;
      }
    }
  }


  private void parseWAIT( CharacterIterator iter ) throws PrgException
  {
    // Code fuer erstes Argument erzeugen (in BC)
    int pos = this.asmOut.length();
    BasicExprParser.parseInt2Expr( this, iter );
    String oldCode1 = this.asmOut.cut( pos );
    String newCode1 = BasicUtil.convertCodeToValueInBC( oldCode1 );

    // Code fuer zweites Argument erzeugen (in E)
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseInt2Expr( this, iter );
    String  oldCode2 = this.asmOut.substring( pos );
    String  newCode2 = null;
    Integer mask     = BasicUtil.removeLastCodeIfConstExpr(
							this.asmOut,
							pos );
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
    if( BasicUtil.checkComma( iter ) ) {
      BasicExprParser.parseInt2Expr( this, iter );
      oldCode3    = this.asmOut.substring( pos );
      Integer inv = BasicUtil.removeLastCodeIfConstExpr( this.asmOut, pos );
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
			+ "\tCALL\tXCHECK_BREAK\n"
			+ "\tJR\t" );
      this.asmOut.append( loopLabel );
      this.asmOut.newLine();
      this.asmOut.append( exitLabel );
      this.asmOut.append( ":\n" );
      addLibItem( BasicLibrary.LibItem.XCHECK_BREAK );
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
    BasicExprParser.parseInt2Expr( this, iter );
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

  private void parseInt2Expr( CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseInt2Expr( this, iter );
  }


  private void parseLineExprList( CharacterIterator iter ) throws PrgException
  {
    java.util.List<String> labels = new ArrayList<>( 32 );
    labels.add( parseDestLineExpr( iter ) );
    while( BasicUtil.checkComma( iter ) ) {
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
      rv = lineNum;
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
    ParseContext context = new ParseContext();
    BasicUtil.parseToken( iter, '=' );
    switch( dstVar.getDataType() ) {
      case INT2:
	if( dstVar.hasStaticAddr() ) {
	  BasicExprParser.parseInt2Expr( this, iter, context );
	  dstVar.writeCode_LD_Var_Reg( this );
	} else {
	  int pos = this.asmOut.length();
	  BasicExprParser.parseInt2Expr( this, iter, context );
	  Integer value = BasicUtil.removeLastCodeIfConstExpr(
							this.asmOut,
							pos );
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
	break;
      case INT4:
	if( dstVar.hasStaticAddr() ) {
	  BasicExprParser.parseInt4Expr( this, iter, context );
	  dstVar.writeCode_LD_Var_Reg( this );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  BasicExprParser.parseInt4Expr( this, iter, context );
	  this.asmOut.append( "\tPOP\tBC\n" );
	  this.asmOut.append( "\tCALL\tLD_MEM_DEHL\n" );
	  addLibItem( BasicLibrary.LibItem.LD_MEM_DEHL );
	}
	break;
      case DEC6:
	if( dstVar.hasStaticAddr() ) {
	  BasicExprParser.parseDec6Expr( this, iter, context );
	  boolean done     = false;
	  String  preLine  = "";
	  String  lastLine = this.asmOut.cutLastLine();
	  int     pos      = this.asmOut.length();
	  try {
	    if( lastLine.equals( "\tCALL\tD6_LD_ACCU_MEM\n" ) ) {
	      dstVar.ensureStaticAddrInDE( this.asmOut, false );
	      this.asmOut.append( "\tLD\tBC,0006H\n"
				+ "\tLDIR\n" );
	      done = true;
	    } else if( lastLine.startsWith( "\tDB\t" ) ) {
	      preLine = this.asmOut.cutLastLine();
	      pos = this.asmOut.length();
	      if( preLine.equals( "\tCALL\tD6_LD_ACCU_NNNNNN\n" ) ) {
		dstVar.ensureStaticAddrInDE( this.asmOut, false );
		this.asmOut.append( "\tCALL\tD6_LD_MEM_NNNNNN\n" );
		// D6_LD_MEM_NNNNNN ist in D6_LD_ACCU_NNNNNN enthalten
		this.asmOut.append( lastLine );
		done = true;
	      }
	    }
	  }
	  catch( PrgException ex ) {
	    // sollte nie vorkommen
	    done = false;
	  }
	  if( !done ) {
	    this.asmOut.setLength( pos );
	    this.asmOut.append( preLine );
	    this.asmOut.append( lastLine );
	    dstVar.ensureAddrInHL( this.asmOut );
	    this.asmOut.append( "\tCALL\tD6_LD_MEM_ACCU\n" );
	    addLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU );
	  }
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  BasicExprParser.parseDec6Expr( this, iter, context );
	  this.asmOut.append( "\tPOP\tHL\n" );
	  this.asmOut.append( "\tCALL\tD6_LD_MEM_ACCU\n" );
	  addLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU );
	}
	context.setMAccuDirty();
	break;
      case STRING:
	parseStringAssignment( iter, dstVar );
	break;
    }
  }


	/* --- Hilfsfunktionen --- */

  private void checkAppendBreakCheck()
  {
    if( this.options.canBreakAlways() ) {
      this.asmOut.append( "\tCALL\tXCHECK_BREAK\n" );
      addLibItem( BasicLibrary.LibItem.XCHECK_BREAK );
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
	AsmCodeBuf frameBuf = new AsmCodeBuf( 1024 );
	frameBuf.append( "\tPUSH\tIY\n"
			+ "\tLD\tIY,0000H\n"
			+ "\tADD\tIY,SP\n" );
	int totalVarSize = entry.getTotalVarSize();
	if( totalVarSize > 0 ) {

	  // auf dem Stack Platz schaffen fuer lokale Variablen,
	  frameBuf.append_LD_HL_nn( -totalVarSize );
	  frameBuf.append( "\tADD\tHL,SP\n"
			+ "\tLD\tSP,HL\n" );

	  /*
	   * numerische Variablen initialisieren,
	   * Der Einfachheit halber wird hier der gesamte Bereich
	   * fuer die lokalen mit Nullbytes beschrieben,
	   * auch wenn weiter unten die String-Variablen separat
	   * initialisiert werden.
	   */
	  if( this.options.getInitVars() ) {
	    for( int i = 0; i < nVars; i++ ) {
	      if( !entry.getVarType( i ).equals( DataType.STRING ) ) {
		frameBuf.append_LD_B_n( totalVarSize );
		frameBuf.append( "\tCALL\tCLEAR_MEM\n" );
		addLibItem( BasicLibrary.LibItem.CLEAR_MEM );
		break;
	      }
	    }
	  }
	}

	// String-Variablen initialisieren
	boolean hlLoaded = false;
	for( int i = 0; i < nVars; i++ ) {
	  if( entry.getVarType( i ).equals( DataType.STRING ) ) {
	    if( !hlLoaded ) {
	      frameBuf.append_LD_HL_xx( BasicLibrary.EMPTY_STRING_LABEL );
	      addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
	      hlLoaded = true;
	    }
	    frameBuf.append_LD_IndirectIY_HL( entry.getVarIYOffs( i ) );
	  }
	}

	/*
	 * StackFrame-Code vor einem evtl. Kommentar
	 * mit der BASIC-Zeile einfuegen
	 */
	String lastText = null;
	if( this.options.getPrintLineNumOnAbort() ) {
	  int pos = this.asmOut.getLastLinePos();
	  if( pos > 0 ) {
	    String lastLine = this.asmOut.cut( pos );
	    if( lastLine.equals( CODE_LD_SRC_LINE_HL ) ) {
	      pos = this.asmOut.getLastLinePos();
	      if( pos >= 0 ) {
		String preLine = this.asmOut.cut( pos );
		if( preLine.startsWith( "\tLD\tHL," ) ) {
		  lastText = preLine + lastLine;
		} else {
		  this.asmOut.append( preLine );
		}
	      }
	    }
	    if( lastLine == null ) {
	      this.asmOut.append( lastLine );
	    }
	  }
	}
	if( this.options.getIncludeBasicLines() ) {
	  int pos = this.asmOut.getLastLinePos();
	  if( pos > 0 ) {
	    String comment = this.asmOut.cut( pos - 1 );
	    if( comment.startsWith( "\n;" ) ) {
	      if( lastText != null ) {
		lastText = comment + lastText;
	      } else {
		lastText = comment;
	      }
	    } else {
	      this.asmOut.append( comment );
	    }
	  }
	}
	this.stackFrameCode = frameBuf.toString();
	this.stackFramePos  = this.asmOut.length();
	entry.setStackFrameCreated();
	if( lastText != null ) {
	  this.asmOut.append( lastText );
	}
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
    if( !isFunc && BasicUtil.endsWithStringSuffix( name ) ) {
      throw new PrgException(
		String.format(
			"Name einer Prozedur darf nicht auf \'%c\' enden",
			TYPE_STRING_SUFFIX ) );
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
    Set<String>              argSet   = new TreeSet<>();
    java.util.List<String>   argNames = new ArrayList<>();
    java.util.List<DataType> argTypes = new ArrayList<>();
    if( BasicUtil.checkToken( iter, '(' ) ) {
      if( !BasicUtil.checkToken( iter, ')' ) ) {
	int argIdx = 0;
	do {
	  String argName = BasicUtil.checkIdentifier( iter );
	  if( argName == null ) {
	    BasicUtil.throwVarExpected();
	  }
	  checkVarName( argName );
	  if( argName.equals( name ) ) {
	    throw new PrgException( "Name der Funktion/Prozedur"
				+ " als Variablenname nicht erlaubt" );
	  }
	  DataType argType = BasicUtil.parseTypeDecl( argName, iter );
	  if( entry != null ) {
	    // Uebereinstimmung mit vorheriger Deklaration?
	    if( argIdx >= entry.getArgCount() ) {
	      mismatch = true;
	    } else {
	      if( entry.getArgType( argIdx ) != argType ) {
		mismatch = true;
	      } else {
		if( forImplementation ) {
		  /*
		   * Die Variablennamen koennen sich zwischen Deklaration
		   * und Implementierung unterscheiden.
		   * Entscheidend sind die bei der Implementierung.
		   */
		  entry.updArgName( argIdx, argName );
		}
	      }
	    }
	  }
	  if( !argSet.add( argName ) ) {
	    throw new PrgException( "Lokale Variable bereits vorhanden" );
	  }
	  argNames.add( argName );
	  argTypes.add( argType );
	  argIdx++;
	} while( BasicUtil.checkComma( iter ) );
	BasicUtil.parseToken( iter, ')' );
      }
    }

    // ggf. Rueckgabetype parsen
    BasicCompiler.DataType dataType = BasicUtil.parseTypeDecl( name, iter );

    // Uebereinstimmung mit vorheriger Deklaration?
    if( entry != null ) {
      if( entry.getArgCount() != argNames.size() ) {
	mismatch = true;
      }
      if( entry instanceof FunctionEntry ) {
	if( !dataType.equals( ((FunctionEntry) entry).getReturnType() ) ) {
	  mismatch = true;
	}
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
			name,
			dataType );
      } else {
	entry = new SubEntry( this.curSource, this.curBasicLineNum, name );
      }
      entry.setArgs( argNames, argTypes );
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
    entry.setAsmCodePos( this.asmOut.length() );
  }


  /*
   * Die Methode parst eine Kanalnummer.
   * Das Doppelkreuz ist zu dem Zeitpunkt bereits geparst.
   * Als Ergebnis steht die Adresse der IO-WRITE-Routine in HL.
   */
  private void parseIOChannelNumToWriteRoutineInHL(
				CharacterIterator iter ) throws PrgException
  {
    Number channel = BasicUtil.readNumber( iter );
    if( channel != null ) {
      if( !(channel instanceof Integer) ) {
	BasicUtil.throwInt2ExprExpected();
      }
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
	  BasicUtil.throwIOChannelNumOutOfRange();
      }
    } else {
      BasicExprParser.parseInt2Expr( this, iter );
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
    setErrVarsSet();
  }


  private String parseDestLineExpr( CharacterIterator iter )
						throws PrgException
  {
    BasicLineExpr lineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSource,
						this.curBasicLineNum );
    if( lineExpr == null ) {
      BasicUtil.throwBasicLineExprExpected();
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
    SimpleVarInfo varInfo = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.WRITE );
    if( varInfo != null ) {
      if( varInfo.getDataType() != DataType.STRING ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      if( text != null ) {
	BasicUtil.throwStringVarExpected();
      }
      BasicUtil.throwStringLitOrVarExpected();
    }
    varInfo.ensureAddrInHL( this.asmOut );
    if( password ) {
      this.asmOut.append( "\tCALL\tINPUT_PASSWORD_V_S\n" );
      addLibItem( BasicLibrary.LibItem.INPUT_PASSWORD_V_S );
    } else {
      this.asmOut.append( "\tCALL\tINPUT_LINE_V_S\n" );
      addLibItem( BasicLibrary.LibItem.INPUT_LINE_V_S );
    }
  }


  private void parseInputLineIO( CharacterIterator iter ) throws PrgException
  {
    ParseContext context = new ParseContext();
    parseIOChannelNumToPtrFldAddrInHL( iter, context, null );
    BasicUtil.parseToken( iter, ',' );
    this.asmOut.append( "\tLD\tC,00H\n"
			+ "\tCALL\tIOINL\n" );
    int           pos     = this.asmOut.length();
    SimpleVarInfo varInfo = checkVariable( iter, context, AccessMode.WRITE );
    if( varInfo == null ) {
      BasicUtil.throwStringVarExpected();
    }
    if( varInfo.getDataType() != DataType.STRING ) {
      BasicUtil.throwStringVarExpected();
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
    BasicExprParser.parse2Int2ArgsTo_DE_HL( this, iter, new ParseContext() );
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
    BasicExprParser.parseInt2Expr( this, iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( labelX );
    this.asmOut.append( "),HL\n" );
    BasicUtil.parseToken( iter, ',' );
    BasicExprParser.parseInt2Expr( this, iter );
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
         * auch wenn es sich um ein Nullbyte handelt.
         */
	boolean done   = false;
	int     srcPos = iter.getIndex();
	if( BasicUtil.checkKeyword( iter, "CHR$" ) ) {
	  int dstPos = this.asmOut.length();
	  BasicExprParser.parseEnclosedInt2Expr(
					this,
					iter,
					new ParseContext() );
	  String oldCode = asmOut.cut( dstPos );
	  String newCode = BasicUtil.convertCodeToValueInA( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	  } else {
	    asmOut.append( oldCode );
	    this.asmOut.append( "\tLD\tA,L\n" );
	  }
	  if( toScreen ) {
	    this.asmOut.append( "\tCALL\tXOUTCH\n" );
	    addLibItem( BasicLibrary.LibItem.XOUTCH );
	  } else {
	    this.asmOut.append( "\tCALL\tIO_COUT\n" );
	    addLibItem( BasicLibrary.LibItem.IO_COUT );
	  }
	  hasSeg = true;
	  done   = true;
	} else if( BasicUtil.checkKeyword( iter, "SPC" ) ) {
	  BasicExprParser.parseEnclosedInt2Expr(
					this,
					iter,
					new ParseContext() );
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
		this.asmOut.append( "\tCALL\tIO_PRINT_S\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINT_S );
	      }
	    } else {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tXOUTST\n" );
		addLibItem( BasicLibrary.LibItem.XOUTST );
	      } else {
		this.asmOut.append( "\tCALL\tIO_PRINT_TRAILING_S\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINT_TRAILING_S );
	      }
	      this.asmOut.appendStringLiteral( text );
	    }
	    hasSeg = true;
	  }
	  else if( BasicExprParser.checkParseStringPrimVarExpr(
						this,
						iter,
						new ParseContext() ) )
	  {
	    if( toScreen ) {
	      this.asmOut.append( "\tCALL\tXOUTS\n" );
	      addLibItem( BasicLibrary.LibItem.XOUTS );
	    } else {
	      this.asmOut.append( "\tCALL\tIO_PRINT_S\n" );
	      addLibItem( BasicLibrary.LibItem.IO_PRINT_S );
	    }
	    hasSeg = true;
	  } else {
	    if( mandatory ) {
	      BasicUtil.throwStringExprExpected();
	    }
	  }
	}
	if( !hasSeg ) {
	  break;
	}
	if( BasicUtil.skipSpaces( iter ) != '+' ) {
	  break;
	}
	iter.next();
	mandatory = true;
      }
      if( !hasSeg ) {

	// kann nur noch numerisches Segment sein
	switch( BasicExprParser.parseNumericExpr(
					this,
					iter,
					new ParseContext(),
					null ) )
	{
	  case INT2:
	    if( formatted ) {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tPRINTF_I2\n" );
		addLibItem( BasicLibrary.LibItem.PRINTF_I2 );
	      } else {
		this.asmOut.append( "\tCALL\tIO_PRINTF_I2\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINTF_I2 );
	      }
	    } else {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tPRINT_I2\n" );
		addLibItem( BasicLibrary.LibItem.PRINT_I2 );
	      } else {
		this.asmOut.append( "\tCALL\tIO_PRINT_I2\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINT_I2 );
	      }
	    }
	    space = true;
	    break;
	  case INT4:
	    if( formatted ) {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tPRINTF_I4\n" );
		addLibItem( BasicLibrary.LibItem.PRINTF_I4 );
	      } else {
		this.asmOut.append( "\tCALL\tIO_PRINTF_I4\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINTF_I4 );
	      }
	    } else {
	      if( toScreen ) {
		this.asmOut.append( "\tCALL\tPRINT_I4\n" );
		addLibItem( BasicLibrary.LibItem.PRINT_I4 );
	      } else {
		this.asmOut.append( "\tCALL\tIO_PRINT_I4\n" );
		addLibItem( BasicLibrary.LibItem.IO_PRINT_I4 );
	      }
	    }
	    space = true;
	    break;
	  case DEC6:
	    if( this.asmOut.cutIfEndsWith( "\tCALL\tD6_LD_ACCU_MEM\n" ) ) {
	      if( formatted ) {
		if( toScreen ) {
		  this.asmOut.append( "\tCALL\tPRINTF_D6MEM\n" );
		  addLibItem( BasicLibrary.LibItem.PRINTF_D6MEM );
		} else {
		  this.asmOut.append( "\tCALL\tIO_PRINTF_D6MEM\n" );
		  addLibItem( BasicLibrary.LibItem.IO_PRINTF_D6MEM );
		}
	      } else {
		if( toScreen ) {
		  this.asmOut.append( "\tCALL\tPRINT_D6MEM\n" );
		  addLibItem( BasicLibrary.LibItem.PRINT_D6MEM );
		} else {
		  this.asmOut.append( "\tCALL\tIO_PRINT_D6MEM\n" );
		  addLibItem( BasicLibrary.LibItem.IO_PRINT_D6MEM );
		}
	      }
	    } else {
	      if( formatted ) {
		if( toScreen ) {
		  this.asmOut.append( "\tCALL\tPRINTF_D6\n" );
		  addLibItem( BasicLibrary.LibItem.PRINTF_D6 );
		} else {
		  this.asmOut.append( "\tCALL\tIO_PRINTF_D6\n" );
		  addLibItem( BasicLibrary.LibItem.IO_PRINTF_D6 );
		}
	      } else {
		if( toScreen ) {
		  this.asmOut.append( "\tCALL\tPRINT_D6\n" );
		  addLibItem( BasicLibrary.LibItem.PRINT_D6 );
		} else {
		  this.asmOut.append( "\tCALL\tIO_PRINT_D6\n" );
		  addLibItem( BasicLibrary.LibItem.IO_PRINT_D6 );
		}
	      }
	    }
	    space = true;
	    break;
	  default:
	    BasicUtil.throwNumericExprExpected();
	}
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
	  this.asmOut.append( "\tCALL\tIO_PRINT_SP\n" );
	  addLibItem( BasicLibrary.LibItem.IO_PRINT_SP );
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
	this.asmOut.append( "\tCALL\tIO_PRINT_NL\n" );
	addLibItem( BasicLibrary.LibItem.IO_PRINT_NL );
      }
    }
  }


  /*
   * Zuweisung zu einer String-Variablen paren
   *
   * Das '='-Zeichen wurde bereits geparst.
   */
  private void parseStringAssignment(
			CharacterIterator iter,
			SimpleVarInfo     dstVar ) throws PrgException
  {
    boolean done   = false;
    int     srcPos = iter.getIndex();
    int     dstPos = this.asmOut.length();

    // pruefen, ob nur ein Literal zugewiesen wird
    String  text = BasicUtil.checkStringLiteral( this, iter );
    if( text != null ) {
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
      SimpleVarInfo srcVar = checkVariable(
					iter,
					new ParseContext(),
					AccessMode.READ );
      if( srcVar != null ) {
	if( isEndOfInstr( iter ) ) {
	  if( dstVar.hasStaticAddr() && srcVar.hasStaticAddr() ) {
	    srcVar.ensureAddrInHL( this.asmOut );
	    dstVar.ensureStaticAddrInDE( this.asmOut, true );
	  } else {
	    // wird durch Optimizer optimiert
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
      if( BasicExprParser.checkParseStringPrimExpr(
						this,
						iter,
						new ParseContext() ) )
      {
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
	  if( !BasicExprParser.checkParseStringPrimVarExpr(
						this,
						iter,
						new ParseContext() ) )
	  {
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
	this.asmOut.append( "\tCALL\tSTR_N_COPY\n" );
	addLibItem( BasicLibrary.LibItem.STR_N_COPY );
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


  private static void updVarUsage( VarDecl varDecl, AccessMode accessMode )
  {
    switch( accessMode ) {
      case READ:
	varDecl.setRead();
	break;
      case WRITE:
	varDecl.setWritten();
	break;
      case READ_WRITE:
	varDecl.setRead();
	varDecl.setWritten();
	break;
    }
  }
}
