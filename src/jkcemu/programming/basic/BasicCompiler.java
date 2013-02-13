/*
 * (c) 2008-2013 Jens Mueller
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
import jkcemu.base.*;
import jkcemu.programming.*;


public class BasicCompiler
{
  public static final int    DATA_INT1   = 0x01;
  public static final int    DATA_INT2   = 0x02;
  public static final int    DATA_STRING = 0x11;
  public static final int    MAGIC_GOSUB = 'G';
  public static final int    MAX_VALUE   = 32767;
  public static final int    MAX_STR_LEN = 255;
  public static final String ERR_POS_TEXT = " ??? ";

  public static enum DataType { INTEGER, STRING };


  // sortierte Liste der reservierten Schluesselwoeter
  private static final String[] sortedReservedWords = {
	"ABS", "ADD", "AND", "APPEND", "AS", "ASC", "ASM", "AT",
	"BIN$", "BLACK", "BLINKING", "BLUE", "BORDER", "BSS",
	"CALL", "CHR$", "CIRCLE", "CLOSE", "CLS",
	"CODE", "COLOR", "CURSOR", "CYAN",
	"DATA", "DECLARE", "DEEK", "DEF", "DEFUSR",
	"DEFUSR0", "DEFUSR1", "DEFUSR2", "DEFUSR3", "DEFUSR4",
	"DEFUSR5", "DEFUSR6", "DEFUSR7", "DEFUSR8", "DEFUSR9",
	"DIM", "DO", "DOKE", "DRAW", "DRAWR",
	"ELSE", "ELSEIF", "END", "ENDIF", "EOF", "ERR", "ERR$", "EXIT",
	"FALSE", "FOR", "FUNCTION",
	"GOSUB", "GOTO", "GREEN",
	"HEX$", "H_CHAR", "H_PIXEL",
	"IF", "IN", "INCHAR$", "INCLUDE", "INK", "INKEY$",
	"INP", "INPUT", "INSTR",
	"JOYST",
	"LABEL", "LASTSCREEN", "LEFT$", "LEN", "LET", "LINE",
	"LOCAL", "LOCATE", "LOOP", "LOWER$", "LTRIM$",
	"MAGENTA", "MAX", "MEMSTR$", "MID$", "MIN", "MIRROR$",
	"MOD", "MOVE", "MOVER",
	"NEXT", "NOT",
	"ON", "OPEN", "OR", "OUT", "OUTPUT",
	"PAPER", "PASSWORD", "PAUSE", "PEEK", "PEN",
	"PLOT", "PLOTR", "POINT", "POKE",
	"PRESET", "PRINT", "PSET", "PTEST",
	"READ", "RED", "REM", "RESTORE", "RETURN", "RIGHT$",
	"RND", "RTRIM$",
	"SCREEN", "SGN", "SHL", "SHR", "SPACE$",
	"SQR", "STEP", "STOP", "STR$", "STRING$", "SUB",
	"TARGET_ADDR", "TARGET_ID$", "TEXT", "THEN", "TO", "TOP",
	"TRIM$", "TRUE",
	"UNTIL", "UPPER$", "USING", "USR",
	"USR0", "USR1", "USR2", "USR3", "USR4",
	"USR5", "USR6", "USR7", "USR8", "USR9",
	"VAL",
	"WAIT", "WEND", "WHILE", "WHITE", "WRITE", "W_CHAR", "W_PIXEL",
	"XOR", "XPOS",
	"YELLOW", "YPOS" };

  private String                    srcText;
  private AbstractTarget            target;
  private BasicOptions              options;
  private PrgLogger                 logger;
  private Set<BasicLibrary.LibItem> libItems;
  private Set<String>               basicLines;
  private Collection<BasicLineExpr> destBasicLines;
  private Set<String>               dataBasicLines;
  private Collection<BasicLineExpr> restoreBasicLines;
  private Stack<StructureEntry>     structureStack;
  private Map<String,CallableEntry> name2Callable;
  private Map<String,VarDecl>       name2GlobalVar;
  private Map<String,String>        str2Label;
  private Set<String>               usrLabels;
  private AsmCodeBuf                asmOut;
  private AsmCodeBuf                dataOut;
  private StringBuilder             userData;
  private StringBuilder             userBSS;
  private boolean                   codeGenEnabled;
  private boolean                   gcRequired;
  private boolean                   execEnabled;
  private boolean                   mainPrg;
  private boolean                   separatorChecked;
  private boolean                   suppressExit;
  private boolean                   tmpStrBufUsed;
  private int                       errCnt;
  private int                       labelNum;
  private int                       codePosAtBegOfSrcLine;
  private int                       curSourceLineNum;
  private long                      curBasicLineNum;
  private long                      lastBasicLineNum;
  private String                    lastBasicLineExpr;


  public BasicCompiler(
		String       srcText,
		BasicOptions options,
		PrgLogger    logger )
  {
    this.srcText           = srcText;
    this.target            = options.getTarget();
    this.options           = options;
    this.logger            = logger;
    this.libItems          = new HashSet<BasicLibrary.LibItem>();
    this.basicLines        = new TreeSet<String>();
    this.destBasicLines    = new ArrayList<BasicLineExpr>( 1024 );
    this.dataBasicLines    = null;
    this.restoreBasicLines = null;
    this.structureStack    = new Stack<StructureEntry>();
    this.name2Callable     = new HashMap<String,CallableEntry>();
    this.str2Label         = new HashMap<String,String>();
    this.name2GlobalVar    = new HashMap<String,VarDecl>();
    this.usrLabels         = new TreeSet<String>();
    this.asmOut            = new AsmCodeBuf( 4 * this.srcText.length() );
    this.dataOut           = null;
    this.userData          = null;
    this.userBSS           = null;
    this.codeGenEnabled    = true;
    this.execEnabled       = true;
    this.mainPrg           = true;
    this.gcRequired        = false;
    this.separatorChecked  = false;
    this.suppressExit      = false;
    this.tmpStrBufUsed     = false;
    this.errCnt            = 0;
    this.labelNum          = 1;
    this.curSourceLineNum  = 0;
    this.curBasicLineNum   = -1;
    this.lastBasicLineNum  = -1;
    this.lastBasicLineExpr = null;
  }


  public void cancel()
  {
    this.execEnabled = false;
  }


  public String compile() throws IOException
  {
    String asmText      = null;
    this.codeGenEnabled = true;
    this.execEnabled    = true;
    this.errCnt         = 0;
    this.target.reset();
    try {
      if( this.options.getShowAssemblerText() ) {
	this.asmOut.append( ";\n"
		+ ";Dieser Quelltext wurde vom JKCEMU-BASIC-Compiler"
		+ " erzeugt.\n"
		+ ";http://www.jens-mueller.org/jkcemu/\n"
		+ ";\n"
		+ "\n" );
      }
      this.asmOut.append( "\tORG\t" );
      this.asmOut.appendHex4( this.options.getBegAddr() );
      this.asmOut.newLine();
      this.target.appendProlog(
			this,
			this.asmOut,
			this.options.getAppName() );
      this.asmOut.append( "\tJP\tMINIT\n" );
      this.target.appendMenuItem(
			this,
			this.asmOut,
			this.options.getAppName() );
      if( this.options.getShowAssemblerText() ) {
	this.asmOut.append( "\n;Programmstart\n" );
      }
      this.asmOut.append( "MSTART:\n" );
      parseSourceText();
      if( this.execEnabled ) {
	BasicLibrary.appendCodeTo( this.asmOut, this.target, this );
	BasicLibrary.appendInitTo(
				this.asmOut,
				this.target,
				this.options,
				this.libItems,
				this.name2GlobalVar,
				this.usrLabels );
	this.asmOut.append( "\tJP\tMSTART\n" );
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
	BasicLibrary.appendData(
			this.asmOut,
			this.target,
			this.options ,
			this.libItems,
			this.str2Label,
			this.userData );
	BasicLibrary.appendBSS(
			this.asmOut,
			this.target,
			this.options,
			this.libItems,
			this.name2GlobalVar,
			this.usrLabels,
			this.userBSS );
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


  public BasicOptions getBasicOptions()
  {
    return this.options;
  }


  public Set<BasicLibrary.LibItem> getLibItems()
  {
    return this.libItems;
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


  public boolean usesLibItem( BasicLibrary.LibItem libItem )
  {
    return this.libItems.contains( libItem );
  }


	/* --- private Methoden --- */

  private void appendLineNotFoundToErrLog(
		BasicLineExpr lineExpr,
		String        trailingMsg ) throws TooManyErrorsException
  {
    if( lineExpr != null ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append( "Fehler" );
      if( lineExpr.getSourceLineNum() > 0 ) {
	buf.append( " in Zeile " );
	buf.append( lineExpr.getSourceLineNum() );
	if( lineExpr.getSourceBasicLineNum() >= 0 ) {
	  buf.append( " (BASIC-Zeilennummer " );
	  buf.append( lineExpr.getSourceBasicLineNum() );
	  buf.append( (char) ')' );
	}
      }
      buf.append( ": " );
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
			int    sourceLineNum,
			long   basicLineNum,
			String msg,
			String msgType )
  {
    StringBuilder buf = new StringBuilder( 128 );
    if( (sourceLineNum > 0) || (basicLineNum >= 0) ) {
      if( msgType != null ) {
	buf.append( msgType );
	buf.append( " in " );
      }
      if( sourceLineNum > 0 ) {
	buf.append( "Zeile " );
	buf.append( sourceLineNum );
	if( basicLineNum >= 0 ) {
	  buf.append( " (BASIC-Zeilennummer " );
	  buf.append( basicLineNum );
	  buf.append( (char) ')' );
	}
      }
      else if( basicLineNum >= 0 ) {
	buf.append( "BASIC-Zeilennummer " );
	buf.append( basicLineNum );
      }
      buf.append( ": " );
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
		this.curSourceLineNum,
		this.curBasicLineNum,
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
    return checkKeyword( iter, keyword, true, true );
  }


  private boolean checkKeyword( CharacterIterator iter, String keyword )
  {
    return checkKeyword( iter, keyword, true, false );
  }


  private boolean checkKeyword(
			CharacterIterator iter,
			String            keyword,
			boolean           fmtSource,
			boolean           space )
  {
    boolean rv = true;
    char    ch     = skipSpaces( iter );
    int     begPos = iter.getIndex();
    int     len    = keyword.length();
    for( int i = 0; i < len; i++ ) {
      if( keyword.charAt( i ) != Character.toUpperCase( ch ) ) {
	iter.setIndex( begPos );
	rv = false;
	break;
      }
      ch = iter.next();
    }
    if( rv ) {
      if( ((ch >= 'A') && (ch <= 'Z'))
	  || ((ch >= 'a') && (ch <= 'z'))
	  || (ch == '$') )
      {
	rv = false;
      }
    }
    return rv;
  }


  private Integer checkSignedNumber( CharacterIterator iter )
						throws PrgException
  {
    boolean neg = false;
    char    ch  = skipSpaces( iter );
    int     pos = iter.getIndex();
    if( ch == '+' ) {
      iter.next();
    } else if( ch == '-' ) {
      iter.next();
      neg = true;
    }
    Integer rv = readNumber( iter );
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
				checkIdentifier( iter ) );
    if( varInfo == null ) {
      iter.setIndex( begPos );
    }
    return varInfo;
  }


  /*
   * Die Methode prueft, ob der uebergebene Name eine Variable ist
   * bzw. als einfach Variable gewertet werden kann (implizite Deklaration).
   * Wenn ja, wird die Variable vollstaendig geparst (evtl. Indexe)
   * und ein VarInfo-Objekt zurueckgeliefert.
   * Ist in dem Objekt das Attribute "addrExpr" gefuellt,
   * bescheibt dieses die Adresse der Variablen.
   * Im dem Fall wurde keine Code erzeugt.
   * Ist dagegen "addrExpr" null, wurde Code erzeugt,
   * der im HL-Register die Adresse der Variablen enthaelt.
   */
  private SimpleVarInfo checkVariable(
			CharacterIterator iter,
			String            varName ) throws PrgException
  {
    SimpleVarInfo varInfo = null;
    if( varName != null ) {
      if( !varName.isEmpty() ) {
	if( Arrays.binarySearch( sortedReservedWords, varName ) < 0 ) {
	  boolean done     = false;
	  String  addrExpr = null;
	  Integer iyOffs   = null;

	  // auf lokale Variable prufen
	  CallableEntry callableEntry = getCallableEntry();
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
		parseToken( iter, '(' );
		if( varDecl.getDimCount() == 1 ) {
		  int pos = this.asmOut.length();
		  parseExpr( iter );
		  Integer idx = removeLastCodeIfConstExpr( pos );
		  if( idx != null ) {
		    if( (idx.intValue() < 0) || (idx > varDecl.getDim1()) ) {
		      throwIndexOutOfRange();
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
		  parseExpr( iter );
		  Integer idx1 = removeLastCodeIfConstExpr( pos );
		  if( idx1 != null ) {
		    if( (idx1.intValue() < 0)
			|| (idx1.intValue() > varDecl.getDim1()) )
		    {
		      throwIndexOutOfRange();
		    }
		  } else {
		    if( this.options.getCheckBounds() ) {
		      this.asmOut.append_LD_BC_nn( varDecl.getDim1() + 1 );
		      this.asmOut.append( "\tCALL\tCKIDX\n" );
		      addLibItem( BasicLibrary.LibItem.CKIDX );
		    }
		  }
		  parseToken( iter, ',' );
		  pos = this.asmOut.length();
		  parseExpr( iter );
		  Integer idx2 = removeLastCodeIfConstExpr( pos );
		  if( idx2 != null ) {
		    if( (idx2.intValue() < 0)
			|| (idx2.intValue() > varDecl.getDim2()) )
		    {
		      throwIndexOutOfRange();
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
		parseToken( iter, ')' );
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
				this.curSourceLineNum,
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


  private static String createBasicLineLabel( String lineExprText )
  {
    return "L_" + lineExprText.toUpperCase();
  }


  private static String createDataLineLabel( String lineExprText )
  {
    return "D_" + lineExprText.toUpperCase();
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
    this.mainPrg          = true;
    this.gcRequired       = false;
    this.curSourceLineNum = 0;
    if( this.srcText != null ) {
      BufferedReader reader = new BufferedReader(
					new StringReader( this.srcText ) );
      String line = reader.readLine();
      while( this.execEnabled && (line != null) ) {
        this.curSourceLineNum++;
        parseSourceLine( line );
        line = reader.readLine();
      }
    }
    if( this.mainPrg && !this.suppressExit ) {
      if( this.options.getShowAssemblerText() ) {
	this.asmOut.append( "\n;Programmende\n" );
      }
      this.asmOut.append( "\tJP\tXEXIT\n" );
    }

    // Pruefen, ob alle Strukturen geschlossen wurden
    for( StructureEntry entry : this.structureStack ) {
      appendLineNumMsgToErrLog(
		entry.getSourceLineNum(),
		entry.getBasicLineNum(),
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
	  putWarning(
		entry.getSourceLineNum(),
		entry.getBasicLineNum(),
		entry.toString() + " wird nicht aufgerufen" );
	}
	if( entry.isImplemented() ) {
	  if( this.options.getWarnUnusedItems() ) {
	    int nVars = entry.getVarCount();
	    for( int i = 0; i < nVars; i++ ) {
	      String varName = entry.getVarName( i );
	      if( varName != null ) {
		if( !entry.isVarUsed( varName ) ) {
		  putWarning(
			entry.getVarSourceLineNum( varName ),
			entry.getVarBasicLineNum( varName ),
			"Lokale Variable " + varName
				+ " wird nicht verwendet" );
		}
	      }
	    }
	  }
	} else {
	  long basicLineNum      = entry.getBasicLineNum();
	  int  sourceLineNum     = entry.getSourceLineNum();
	  int  callSourceLineNum = entry.getFirstCallSourceLineNum();
	  if( callSourceLineNum > 0 ) {
	    sourceLineNum = callSourceLineNum;
	    basicLineNum  = entry.getFirstCallBasicLineNum();
	  }
	  appendLineNumMsgToErrLog(
			sourceLineNum,
			basicLineNum,
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
		varDecl.getSourceLineNum(),
		varDecl.getBasicLineNum(),
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
    /*
     * Pruefen, ob im Fall eines impliziten Endes des Hauptprogramms
     * auf Code zur Beendigung des Programms verzichtet werden kann.
     */
    this.suppressExit   = false;
    int     lastLinePos = this.asmOut.getLastLinePos();
    String  lastInst    = this.asmOut.substring( lastLinePos );
    int     tabPos      = lastInst.indexOf( (char) '\t' );
    if( tabPos > 0 ) {
      lastInst = lastInst.substring( lastLinePos );
    }
    if( lastInst.startsWith( "\tJP" ) || lastInst.startsWith( "\tJR" ) ) {
      if( lastInst.indexOf( (char) ',' ) < 0 ) {
	// unbedingter Sprungbefehl
	this.suppressExit = true;
      }
    } else if( lastInst.equals( "\tRET\n" ) ) {
      this.suppressExit = true;
    }

    // Zeile verarbeiten
    CharacterIterator iter = null;
    try {
      iter = new StringCharacterIterator( lineText );
      this.curBasicLineNum = -1;

      // Quelltextzeile als Kommentar
      if( this.options.getShowAssemblerText() ) {
	char ch = skipSpaces( iter );
	if( ch != CharacterIterator.DONE ) {
	  this.asmOut.append( "\n;" );
	  while( ch != CharacterIterator.DONE ) {
	    this.asmOut.append( ch );
	    ch = iter.next();
	  }
	  this.asmOut.newLine();
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
      char ch          = skipSpaces( iter );
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
	  if( skipSpaces( iter ) == CharacterIterator.DONE ) {
	    int           endPos = iter.getIndex() - 1;
	    StringBuilder buf    = new StringBuilder( endPos - begPos );
	    iter.setIndex( begPos );
	    while( iter.getIndex() < endPos ) {
	      buf.append( (char) iter.current() );
	      iter.next();
	    }
	    labelText         = buf.toString();
	    String upperLabel = labelText.toUpperCase();
	    if( Arrays.binarySearch( sortedReservedWords, upperLabel ) >= 0 ) {
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
	ch = skipSpaces( iter );
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

	/*
	 * Zeilennummer ggf. im Programm vermerken,
	 * aber nur, wenn dieser Code auch durchlaufen wuerde
	 */
	if( !this.suppressExit ) {
	  if( this.options.getPrintLineNumOnAbort()
	      && (this.curSourceLineNum < MAX_VALUE) )
	  {
	    this.asmOut.append_LD_HL_nn( this.curSourceLineNum );
	    this.asmOut.append( "\tLD\t(M_SRLN),HL\n" );
	    if( (this.curBasicLineNum >= 0)
		&& (this.curBasicLineNum < MAX_VALUE) )
	    {
	      this.asmOut.append_LD_HL_nn( (int) this.curBasicLineNum );
	      this.asmOut.append( "\tLD\t(M_BALN),HL\n" );
	    }
	  }
	}

	// Anweisungen
	int destInstPos = this.asmOut.length();
	parseInstructions( iter );
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
	StructureEntry entry = this.structureStack.peek();
	if( !(entry instanceof IfEntry) ) {
	  break;
	}
	IfEntry ifEntry = (IfEntry) entry;
	if( ifEntry.isMultiLine() ) {
	  break;
	}
	this.structureStack.pop();
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


  private void parseInstructions( CharacterIterator iter ) throws PrgException
  {
    if( skipSpaces( iter ) != CharacterIterator.DONE ) {
      for(;;) {
	this.separatorChecked = false;
	parseInstruction( iter );
	char ch = skipSpaces( iter );
	if( ch == CharacterIterator.DONE ) {
	  break;
	}
	if( !this.separatorChecked ) {
	  if( ch != ':' ) {
	    throwUnexpectedChar( ch );
	  }
	  iter.next();
	}
      }
    }
  }


  private void parseInstruction( CharacterIterator iter ) throws PrgException
  {
    this.tmpStrBufUsed = false;

    char ch     = skipSpaces( iter );
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
      String  name = checkIdentifier( iter );
      if( name != null ) {
	done = true;
	if( name.equals( "ASM" ) ) {
	  checkCreateStackFrame();
	  parseASM( iter );
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
	    parseIF( iter );
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
	  } else if( name.equals( "MOVE" ) ) {
	    parseMOVE( iter );
	  } else if( name.equals( "MOVER" ) ) {
	    parseMOVER( iter );
	  } else if( name.equals( "NEXT" ) ) {
	    parseNEXT( iter );
	  } else if( name.equals( "ON" ) ) {
	    parseON( iter );
	  } else if( name.equals( "OUT" ) ) {
	    parseOUT( iter );
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


  private void parseASM( CharacterIterator iter ) throws PrgException
  {
    do {
      boolean data = false;
      boolean bss  = false;
      if( !checkKeyword( iter, "CODE" ) ) {
	if( checkKeyword( iter, "DATA" ) ) {
	  data = true;
	} else if( checkKeyword( iter, "BSS" ) ) {
	  bss = true;
	}
      }
      String text = checkStringLiteral( iter );
      if( text == null ) {
	throw new PrgException( "String-Literal erwartet" );
      }
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
    } while( checkToken( iter, ',' ) );
  }


  private void parseBORDER( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsBorderColor() ) {
      parseExpr( iter );
      this.asmOut.append( "\tCALL\tXBORDER\n" );
      addLibItem( BasicLibrary.LibItem.XBORDER );
    } else {
      try {
	setCodeGenEnabled( false );
	parseExpr( iter );
      }
      finally {
	setCodeGenEnabled( true );
      }
    }
  }


  private void parseCALL( CharacterIterator iter ) throws PrgException
  {
    if( checkToken( iter, '*' ) ) {
      skipSpaces( iter );
      Integer value = readHex( iter );
      if( value == null ) {
	throwHexDigitExpected();
      }
      this.asmOut.append_LD_HL_nn( value.intValue() );
    } else {
      parseExpr( iter );
    }
    boolean insideCallable = (getCallableEntry() != null);
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
    parsePointToMem( iter, "M_CIMX", "M_CIMY" );
    parseToken( iter, ',' );
    parseExpr( iter );
    this.asmOut.append( "\tLD\t(M_CIRA),HL\n"
		+ "\tCALL\tCIRCLE\n" );
    addLibItem( BasicLibrary.LibItem.CIRCLE );
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
      parseExpr( iter );
      if( checkToken( iter, ',' ) ) {
	int pos = this.asmOut.length();
	parseExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
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
	setCodeGenEnabled( false );
	parseExpr( iter );
	if( checkToken( iter, ',' ) ) {
	  parseExpr( iter );
	}
      }
      finally {
	setCodeGenEnabled( true );
      }
    }
    if( checkToken( iter, ',' ) ) {
      parseBORDER( iter );
    }
  }


  private void parseCURSOR( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsXCURS() ) {
      parseExpr( iter );
      this.asmOut.append( "\tCALL\tXCURS\n" );
      addLibItem( BasicLibrary.LibItem.XCURS );
    } else {
      try {
	setCodeGenEnabled( false );
	parseExpr( iter );
      }
      finally {
	setCodeGenEnabled( true );
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
	this.dataBasicLines = new TreeSet<String>();
      }
      if( this.dataBasicLines.add( this.lastBasicLineExpr ) ) {
	this.dataOut.append( createDataLineLabel( this.lastBasicLineExpr ) );
	this.dataOut.append( ":\n" );
      }
    }
    do {
      String text = checkStringLiteral( iter );
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
    } while( checkToken( iter, ',' ) );
    addLibItem( BasicLibrary.LibItem.DATA );
  }


  private void parseDECLARE( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = null;
    if( checkKeyword( iter, "FUNCTION" ) ) {
      parseCallableDecl( iter, true, false );
    } else if( checkKeyword( iter, "SUB" ) ) {
      parseCallableDecl( iter, false, false );
    } else {
      throw new PrgException( "FUNCTION oder SUB erwartet" );
    }
  }


  private void parseDEF( CharacterIterator iter ) throws PrgException
  {
    if( !checkKeyword( iter, "USR" ) ) {
      throw new PrgException( "USR erwartet" );
    }
    int usrNum = parseUsrNum( iter );
    parseDEFUSR( iter, usrNum );
  }


  private void parseDEFUSR( CharacterIterator iter ) throws PrgException
  {
    int usrNum = parseUsrNum( iter );
    parseDEFUSR( iter, usrNum );
  }


  private void parseDEFUSR(
			CharacterIterator iter,
			int               usrNum ) throws PrgException
  {
    parseToken( iter, '=' );
    parseExpr( iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( getUsrLabel( usrNum ) );
    this.asmOut.append( "),HL\n" );
    addLibItem( BasicLibrary.LibItem.E_USR );
  }


  private void parseDIM( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = getCallableEntry();
    if( entry != null ) {
      throw new PrgException( "Anweisung in einer Funktion/Prozedur"
			+ " nicht zul\u00E4ssig" );
    }
    do {
      String varName = checkIdentifier( iter );
      if( varName == null ) {
	throwVarNameExpected();
      }
      checkVarName( varName );
      parseToken( iter, '(' );
      VarDecl var  = null;
      int     dim1 = parseNumber( iter );
      if( dim1 < 1 ) {
	throwDimTooSmall();
      }
      if( checkToken( iter, ',' ) ) {
	int dim2 = parseNumber( iter );
	if( dim2 < 1 ) {
	  throwDimTooSmall();
	}
	var = new VarDecl(
			this.curSourceLineNum,
			this.curBasicLineNum,
			varName,
			dim1,
			dim2 );
      } else {
	var = new VarDecl(
			this.curSourceLineNum,
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
      parseToken( iter, ')' );
      this.name2GlobalVar.put( varName, var );
    } while( checkToken( iter, ',' ) );
  }


  private void parseDO( CharacterIterator iter ) throws PrgException
  {
    String loopLabel = nextLabel();
    this.asmOut.append( loopLabel );
    this.asmOut.append( ":\n" );
    checkAppendBreakCheck();
    this.structureStack.push(
		new DoEntry(
			this.curSourceLineNum,
			this.curBasicLineNum,
			loopLabel,
			nextLabel() ) );
  }


  private void parseDOKE( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    parseExpr( iter );
    parseToken( iter, ',' );
    Integer addr = removeLastCodeIfConstExpr( pos );
    if( addr != null ) {
      parseExpr( iter );
      this.asmOut.append( "\tLD\t(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( "),HL\n" );
    } else {
      pos = this.asmOut.length();
      parseExpr( iter );
      Integer value = removeLastCodeIfConstExpr( pos );
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
	String newCode = convertCodeToValueInDE( oldCode );
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
    if( checkKeyword( iter, "STEP" ) ) {
      parseDRAWR( iter );
    } else {
      checkGraphicsSupported();
      parsePointToMem( iter, "M_LNEX", "M_LNEY" );
      this.asmOut.append( "\tCALL\tDRAW\n" );
      addLibItem( BasicLibrary.LibItem.DRLINE );
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
	StructureEntry entry = this.structureStack.peek();
	if( entry instanceof IfEntry ) {
	  ifEntry = (IfEntry) entry;
	}
      }
      if( ifEntry == null ) {
	throw new PrgException( "ELSE ohne IF" );
      }
      String elseLabel = ifEntry.getElseLabel();
      if( elseLabel != null ) {
	if( this.options.getPreferRelativeJumps() ) {
	  this.asmOut.append( "\tJR\t" );
	} else {
	  this.asmOut.append( "\tJP\t" );
	}
	this.asmOut.append( ifEntry.getEndifLabel() );
	this.asmOut.newLine();
	this.asmOut.append( elseLabel );
	this.asmOut.append( ":\n" );
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
      StructureEntry entry = this.structureStack.peek();
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
    if( this.options.getPreferRelativeJumps() ) {
      this.asmOut.append( "\tJR\t" );
    } else {
      this.asmOut.append( "\tJP\t" );
    }
    this.asmOut.append( ifEntry.getEndifLabel() );
    this.asmOut.newLine();
    this.asmOut.append( elseLabel );
    this.asmOut.append( ":\n" );
    ifEntry.setElseLabel( nextLabel() );	// neue ELSE-Marke erzeugen
    parseExpr( iter );
    this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n" );
    checkKeyword( iter, "THEN" );
    if( !ifEntry.isMultiLine() && this.options.getPreferRelativeJumps() ) {
      this.asmOut.append( "\tJR\tZ," );
    } else {
      this.asmOut.append( "\tJP\tZ," );
    }
    this.asmOut.append( ifEntry.getElseLabel() );
    this.asmOut.newLine();
    if( isEndOfInstr( iter ) ) {
      this.separatorChecked = true;
    }
  }


  private void parseEND( CharacterIterator iter ) throws PrgException
  {
    CallableEntry callableEntry = getCallableEntry();
    if( checkKeyword( iter, "FUNCTION" ) ) {
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
    } else if( checkKeyword( iter, "SUB" ) ) {
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
	StructureEntry entry = this.structureStack.pop();
	if( entry != callableEntry ) {
	  appendLineNumMsgToErrLog(
			entry.getSourceLineNum(),
			entry.getBasicLineNum(),
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
					callableEntry.getArgIYOffs( i ) );
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
      this.suppressExit = true;
    }
  }


  private void parseENDIF( CharacterIterator iter ) throws PrgException
  {
    IfEntry ifEntry = null;
    if( !this.structureStack.isEmpty() ) {
      StructureEntry entry = this.structureStack.peek();
      if( entry instanceof IfEntry ) {
	ifEntry = (IfEntry) entry;
	this.structureStack.pop();
      }
    }
    if( ifEntry == null ) {
      throw new PrgException( "ENDIF ohne IF" );
    }
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
      StructureEntry entry = this.structureStack.get( idx );
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
      if( !checkKeyword( iter, loopEntry.getLoopBegKeyword() ) ) {
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
    parseExpr( iter );
    Integer toValue = removeLastCodeIfConstExpr( toPos );
    if( toValue == null ) {
      this.asmOut.append( "\tPUSH\tHL\n" );
    }
    Integer stepValue = null;
    if( checkInstruction( iter, "STEP" ) ) {
      int stepPos = this.asmOut.length();
      parseExpr( iter );
      stepValue = removeLastCodeIfConstExpr( stepPos );
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
				this.curSourceLineNum,
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


  private void parseIF( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n" );

    // auf GOTO und mehrzeiliges IF pruefen
    BasicLineExpr gotoLineExpr = null;
    boolean       multiLine    = false;
    if( checkKeyword( iter, "THEN" ) ) {
      if( isEndOfInstr( iter ) ) {
	multiLine = true;
      } else {
	char ch = skipSpaces( iter );
	if( (ch >= '0') && (ch <= '9') ) {
	  gotoLineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSourceLineNum,
						this.curBasicLineNum );
	}
      }
    } else {
      if( isEndOfInstr( iter ) ) {
	multiLine = true;
      }
    }
    if( !multiLine && (gotoLineExpr == null) ) {
      if( checkKeyword( iter, "GOTO" ) ) {
	gotoLineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSourceLineNum,
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
    if( (gotoLineExpr != null)
	&& !this.options.canBreakAlways()
	&& isEndOfInstr( iter ) )
    {
      this.asmOut.append( "\tJP\tNZ," );
      this.asmOut.append(
		createBasicLineLabel( gotoLineExpr.getExprText() ) );
      this.asmOut.newLine();
      this.destBasicLines.add( gotoLineExpr );
    } else {
      IfEntry ifEntry = new IfEntry(
				this.curSourceLineNum,
				this.curBasicLineNum,
				multiLine,
				nextLabel(),
				nextLabel() );
      this.structureStack.push( ifEntry );
      if( !multiLine && this.options.getPreferRelativeJumps() ) {
	this.asmOut.append( "\tJR\tZ," );
      } else {
	this.asmOut.append( "\tJP\tZ," );
      }
      this.asmOut.append( ifEntry.getElseLabel() );
      this.asmOut.newLine();
      if( gotoLineExpr != null ) {
	checkAppendBreakCheck();
	this.asmOut.append( "\tJP\t" );
	this.asmOut.append(
		createBasicLineLabel( gotoLineExpr.getExprText() ) );
	this.asmOut.newLine();
	this.destBasicLines.add( gotoLineExpr );
      } else {
	this.separatorChecked = true;
      }
    }
  }


  private void parseINK( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      parseExpr( iter );
      this.asmOut.append( "\tCALL\tXINK\n" );
      addLibItem( BasicLibrary.LibItem.XINK );
    } else {
      try {
	setCodeGenEnabled( false );
	parseExpr( iter );
      }
      finally {
	setCodeGenEnabled( true );
      }
    }
  }


  private void parseINPUT( CharacterIterator iter ) throws PrgException
  {
    do {
      String retryLabel = nextLabel();
      this.asmOut.append( retryLabel );
      this.asmOut.append( ":\n" );

      // Prompt
      String text = checkStringLiteral( iter );
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
	checkToken( iter, ';' );
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
    } while( checkToken( iter, ';' ) );
  }


  private void parseLET( CharacterIterator iter ) throws PrgException
  {
    String varName = checkIdentifier( iter );
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
      String text = checkStringLiteral( iter );
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
	if( !checkParseStringPrimVarExpr( iter ) ) {
	  throwStringExprExpected();
	}
	this.asmOut.append( "\tCALL\tDRLBL\n" );
	addLibItem( BasicLibrary.LibItem.DRLBL );
      }
      if( skipSpaces( iter ) != '+' ) {
	break;
      }
      iter.next();
    }
  }


  private void parseLINE( CharacterIterator iter ) throws PrgException
  {
    if( checkKeyword( iter, "INPUT" ) ) {

      // Eingabe einer ganzen Zeile
      parseInputLine( iter, false );

    } else {

      // Zeichnen einer Linie oder eines Rechtecks
      checkGraphicsSupported();
      boolean enclosed = checkToken( iter, '(' );
      parseExpr( iter );
      this.asmOut.append( "\tLD\t(M_LNBX),HL\n" );
      parseToken( iter, ',' );
      parseExpr( iter );
      this.asmOut.append( "\tLD\t(M_LNBY),HL\n" );
      if( enclosed ) {
	parseToken( iter, ')' );
	parseToken( iter, '-' );
	parseToken( iter, '(' );
      } else {
	parseToken( iter, ',' );
      }
      parseExpr( iter );
      this.asmOut.append( "\tLD\t(M_LNEX),HL\n" );
      parseToken( iter, ',' );
      parseExpr( iter );
      this.asmOut.append( "\tLD\t(M_LNEY),HL\n" );
      if( enclosed ) {
	parseToken( iter, ')' );
      }
      if( checkToken( iter, ',' ) ) {
	if( checkKeyword( iter, "BF" ) ) {
	  this.asmOut.append( "\tCALL\tDRBOXF\n" );
	  addLibItem( BasicLibrary.LibItem.DRBOXF );
	} else {
	  if( !checkKeyword( iter, "B" ) ) {
	    throw new PrgException( "B oder BF erwartet" );
	  }
	  this.asmOut.append( "\tCALL\tDRBOX\n" );
	  addLibItem( BasicLibrary.LibItem.DRBOX );
	}
      } else {
	this.asmOut.append( "\tCALL\tDRLINE\n" );
	addLibItem( BasicLibrary.LibItem.DRLINE );
      }
    }
  }


  private void parseLOCAL( CharacterIterator iter ) throws PrgException
  {
    CallableEntry entry = getCallableEntry();
    if( entry == null ) {
	throw new PrgException(
		"Anweisung nur in einer Funktion/Prozedur erlaubt" );
    }
    do {
      String varName = checkIdentifier( iter );
      if( varName == null ) {
	throwVarNameExpected();
      }
      checkVarName( varName );
      if( varName.equals( entry.getName() ) ) {
	throw new PrgException( "Name der Funktion/Prozedur"
			+ " als Variablenname nicht zul\u00E4ssig" );
      }
      entry.addVar( this.curSourceLineNum, this.curBasicLineNum, varName );
    } while( checkToken( iter, ',' ) );
  }


  private void parseLOCATE( CharacterIterator iter ) throws PrgException
  {
    parse2ArgsTo_DE_HL( iter );
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
      StructureEntry entry = this.structureStack.peek();
      if( entry instanceof DoEntry ) {
	doEntry = (DoEntry) entry;
	this.structureStack.pop();
      }
    }
    if( doEntry == null ) {
      throw new PrgException( "LOOP ohne DO" );
    }
    if( checkKeyword( iter, "UNTIL" ) ) {
      parseExpr( iter );
      this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ," + doEntry.getLoopLabel() + "\n" );
    } else if( checkKeyword( iter, "WHILE" ) ) {
      parseExpr( iter );
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


  private void parseMOVE( CharacterIterator iter ) throws PrgException
  {
    if( checkKeyword( iter, "STEP" ) ) {
      parseMOVER( iter );
    } else {
      checkGraphicsSupported();
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
      StructureEntry entry = this.structureStack.peek();
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


  private void parseOUT( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = checkToken( iter, '(' );
    int     pos      = this.asmOut.length();
    parseExpr( iter );
    String tmpPortCode = this.asmOut.cut( pos );
    String newPortCode = convertCodeToValueInBC( tmpPortCode );
    if( enclosed ) {
      parseToken( iter, ')' );
    }
    if( enclosed ) {
      if( !checkToken( iter, '=' ) ) {
	parseToken( iter, ',' );
      }
    } else {
      parseToken( iter, ',' );
    }
    parseExpr( iter );
    Integer value = removeLastCodeIfConstExpr( pos );
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
	if( isOnly_LD_HL_xx( valueCode ) ) {
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


  private void parseON( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    if( checkKeyword( iter, "GOSUB" ) ) {
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
    } else if( checkKeyword( iter, "GOTO" ) ) {
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


  private void parsePAPER( CharacterIterator iter ) throws PrgException
  {
    if( this.target.supportsColors() ) {
      parseExpr( iter );
      this.asmOut.append( "\tCALL\tXPAPER\n" );
      addLibItem( BasicLibrary.LibItem.XPAPER );
    } else {
      try {
	setCodeGenEnabled( false );
	parseExpr( iter );
      }
      finally {
	setCodeGenEnabled( true );
      }
    }
  }


  private void parsePASSWORD( CharacterIterator iter ) throws PrgException
  {
    if( !checkKeyword( iter, "INPUT" ) ) {
      throw new PrgException( "INPUT erwartet" );
    }
    parseInputLine( iter, true );
  }


  private void parsePAUSE( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    parseExpr( iter );
    Integer value = removeLastCodeIfConstExpr( pos );
    if( value != null ) {
      if( (value.intValue() < 0) || (value.intValue() > 0x7FFF) ) {
	putWarningOutOfRange();
      }
      this.asmOut.append_LD_HL_nn( value.intValue() );
    } else {
      parseExpr( iter );
    }
    this.asmOut.append( "\tCALL\tPAUSE\n" );
    addLibItem( BasicLibrary.LibItem.PAUSE );
  }


  private void parsePOKE( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    parseExpr( iter );
    Integer addr = removeLastCodeIfConstExpr( pos );
    parseToken( iter, ',' );
    pos = this.asmOut.length();
    parseExpr( iter );
    if( addr != null ) {
      Integer value = removeLastCodeIfConstExpr( pos );
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
      Integer value = removeLastCodeIfConstExpr( pos );
      if( value != null ) {
	this.asmOut.append( "\tLD\t(HL)," );
	this.asmOut.appendHex2( value.intValue() );
	this.asmOut.newLine();
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
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
    parseExpr( iter );
    Integer value = removeLastCodeIfConstExpr( pos );
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
    if( checkKeyword( iter, "STEP" ) ) {
      parsePLOTR( iter );
    } else {
      checkGraphicsSupported();
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
    boolean newLine   = true;
    boolean space     = false;
    boolean formatted = false;
    char    ch        = skipSpaces( iter );
    while( !isEndOfInstr( iter ) ) {
      this.tmpStrBufUsed = false;
      newLine            = true;
      space              = false;
      boolean hasSeg     = false;

      // String-Segment pruefen
      boolean mandatory = false;
      for(;;) {
	String text = checkStringLiteral( iter );
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
	  hasSeg = true;
	} else if( checkParseStringPrimVarExpr( iter ) ) {
	  this.asmOut.append( "\tCALL\tXOUTS\n" );
	  addLibItem( BasicLibrary.LibItem.XOUTS );
	  hasSeg = true;
	} else {
	  if( mandatory ) {
	    throwStringExprExpected();
	  }
	}
	if( skipSpaces( iter ) != '+' ) {
	  break;
	}
	iter.next();
	mandatory = true;
      }
      if( !hasSeg ) {

	// kann nur noch numerisches Segment sein
	parseExpr( iter );
	if( formatted ) {
	  this.asmOut.append( "\tCALL\tP_INTF\n" );
	  addLibItem( BasicLibrary.LibItem.P_INTF );
	} else {
	  this.asmOut.append( "\tCALL\tP_INT\n" );
	  addLibItem( BasicLibrary.LibItem.P_INT );
	}
	space = true;
      }

      // weiteres Segment?
      ch        = skipSpaces( iter );
      formatted = (ch == ',');
      if( (ch != ';') && (ch != ',') ) {
	break;
      }
      if( space ) {
	this.asmOut.append( "\tCALL\tOUTSP\n" );
	addLibItem( BasicLibrary.LibItem.OUTSP );
      }
      newLine = false;
      iter.next();
      ch = skipSpaces( iter );
    }
    if( newLine ) {
      this.asmOut.append( "\tCALL\tXOUTNL\n" );
      addLibItem( BasicLibrary.LibItem.XOUTNL );
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
    } while( skipSpaces( iter ) == ',' );
  }


  private void parseRESTORE( CharacterIterator iter ) throws PrgException
  {
    BasicLineExpr lineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSourceLineNum,
						this.curBasicLineNum );
    if( lineExpr != null ) {
      if( this.restoreBasicLines == null ) {
	this.restoreBasicLines = new ArrayList<BasicLineExpr>( 128 );
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
    this.suppressExit = true;
  }


  private void parseSCREEN( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    this.asmOut.append( "\tCALL\tSCREEN\n" );
    addLibItem( BasicLibrary.LibItem.SCREEN );
  }


  private void parseWAIT( CharacterIterator iter ) throws PrgException
  {
    // Code fuer erstes Argument erzeugen (in BC)
    int pos = this.asmOut.length();
    parseExpr( iter );
    String oldCode1 = this.asmOut.cut( pos );
    String newCode1 = convertCodeToValueInBC( oldCode1 );

    // Code fuer zweites Argument erzeugen (in E)
    parseToken( iter, ',' );
    parseExpr( iter );
    String  oldCode2 = this.asmOut.substring( pos );
    String  newCode2 = null;
    Integer mask     = removeLastCodeIfConstExpr( pos );
    this.asmOut.setLength( pos );
    if( mask != null ) {
      int v    = mask.intValue() & 0xFF;
      newCode2 = String.format(
			"\tLD\tE,%s%02XH\n",
			v >= 0xA0 ? "0" : "",
			v );
    } else {
      if( isOnly_LD_HL_xx( oldCode2 ) ) {
	newCode2 = oldCode2 + "\tLD\tE,L\n";
      }
    }

    // Code fuer optionales drittes Argument erzeugen (in L)
    String oldCode3 = null;
    String newCode3 = null;
    if( checkToken( iter, ',' ) ) {
      parseExpr( iter );
      oldCode3    = this.asmOut.substring( pos );
      Integer inv = removeLastCodeIfConstExpr( pos );
      this.asmOut.setLength( pos );
      if( inv != null ) {
	int v    = inv.intValue() & 0xFF;
	newCode3 = String.format(
			"\tLD\tL,%s%02XH\n",
			v >= 0xA0 ? "0" : "",
			v );
      } else {
	if( isOnly_LD_HL_xx( oldCode3 ) ) {
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
      StructureEntry entry = this.structureStack.peek();
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
    parseExpr( iter );
    checkKeyword( iter, "DO" );
    this.asmOut.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ," + exitLabel + "\n" );
    checkAppendBreakCheck();
    this.structureStack.push(
		new WhileEntry(
			this.curSourceLineNum,
			this.curBasicLineNum,
			loopLabel,
			exitLabel ) );
  }


	/* --- Parsen der BASIC-Funktionen --- */

  private void parseABS( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tCALL\tABSHL\n" );
    addLibItem( BasicLibrary.LibItem.ABS_NEG_HL );
  }


  private void parseASC( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ')' );
    this.asmOut.append( "\tLD\tL,(HL)\n"
		+ "\tLD\tH,00H\n" );
  }


  private void parseBLACK( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorBlack( this.asmOut );
  }


  private void parseBLINKING( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorBlinking( this.asmOut );
  }


  private void parseBLUE( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorBlue( this.asmOut );
  }


  private void parseCYAN( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorCyan( this.asmOut );
  }


  private void parseDEEK( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    parseEnclosedExpr( iter );
    Integer addr = removeLastCodeIfConstExpr( pos );
    if( addr != null ) {
      this.asmOut.append( "\tLD\tHL,(" );
      this.asmOut.appendHex4( addr.intValue() );
      this.asmOut.append( ")\n" );
    } else {
      this.asmOut.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n" );
    }
  }


  private void parseERR( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,(M_ERR)\n" );
    addLibItem( BasicLibrary.LibItem.M_ERR );
  }


  private void parseFALSE( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,0000H\n" );
  }


  private void parseGREEN( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorGreen( this.asmOut );
  }


  private void parseH_CHAR( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendHChar( this.asmOut );
  }


  private void parseH_PIXEL( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendHPixel( this.asmOut );
  }


  private void parseIN( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseExpr( iter );
    parseToken( iter, ')' );
    if( !replaceLastCodeFrom_LD_HL_To_BC() ) {
      this.asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
    }
    this.asmOut.append( "\tIN\tL,(C)\n"
		+ "\tLD\tH,0\n" );
  }


  private void parseINSTR( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    String text = checkStringLiteral( iter );
    if( text == null ) {
      parseStringPrimExpr( iter );
    }
    parseToken( iter, ',' );
    String pattern = checkStringLiteral( iter );
    if( pattern != null ) {
      if( text != null ) {
	this.asmOut.append( "\tLD\tHL," );
	this.asmOut.append( getStringLiteralLabel( text ) );
	this.asmOut.newLine();
      }
      this.asmOut.append( "\tLD\tDE," );
      this.asmOut.append( getStringLiteralLabel( pattern ) );
      this.asmOut.newLine();
    } else {
      if( text == null ) {
	this.asmOut.append( "\tPUSH\tHL\n" );
      }
      parseStringPrimExpr( iter );
      this.asmOut.append( "\tEX\tDE,HL\n" );
      if( text != null ) {
	this.asmOut.append( "\tLD\tHL," );
	this.asmOut.append( getStringLiteralLabel( text ) );
	this.asmOut.newLine();
      } else {
	this.asmOut.append( "\tPOP\tHL\n" );
      }
    }
    this.asmOut.append( "\tCALL\tF_ISTR\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.F_ISTR );
  }


  private void parseJOYST( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tCALL\tF_JOY\n" );
    addLibItem( BasicLibrary.LibItem.F_JOY );
  }


  private void parseLASTSCREEN( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendLastScreenNum( this.asmOut );
  }


  private void parseLEN( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    String text = checkStringLiteral( iter );
    if( text != null ) {
      this.asmOut.append_LD_HL_nn( text.length() );
    } else {
      parseStringPrimExpr( iter );
      this.asmOut.append( "\tCALL\tF_LEN\n" );
      addLibItem( BasicLibrary.LibItem.F_LEN );
    }
    parseToken( iter, ')' );
  }


  private void parseMAX( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseExpr( iter );
    while( checkToken( iter, ',' ) ) {
      this.asmOut.append( "\tPUSH\tHL\n" );
      parseExpr( iter );
      String label = nextLabel();
      this.asmOut.append( "\tPOP\tDE\n"
	+ "\tCALL\tCPHLDE\n"
	+ "\tJR\tNC," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
	+ "\tEX\tDE,HL\n" );
      this.asmOut.append( label );
      this.asmOut.append( ":\n" );
    }
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.CPHLDE );
  }


  private void parseMAGENTA( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorMagenta( this.asmOut );
  }


  private void parseMIN( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseExpr( iter );
    while( checkToken( iter, ',' ) ) {
      this.asmOut.append( "\tPUSH\tHL\n" );
      parseExpr( iter );
      String label = nextLabel();
      this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tCPHLDE\n"
		+ "\tJR\tC," );
      this.asmOut.append( label );
      this.asmOut.append( "\n"
		+ "\tEX\tDE,HL\n" );
      this.asmOut.append( label );
      this.asmOut.append( ":\n" );
    }
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.CPHLDE );
  }


  private void parsePEEK( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tLD\tL,(HL)\n"
		+ "\tLD\tH,0\n" );
  }


  private void parseRED( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorRed( this.asmOut );
  }


  private void parseRND( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tCALL\tF_RND\n" );
    addLibItem( BasicLibrary.LibItem.F_RND );
  }


  private void parsePTEST( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parse2ArgsTo_DE_HL( iter );
    parseToken( iter, ')' );
    this.asmOut.append( "\tCALL\tXPTEST\n" );
    addLibItem( BasicLibrary.LibItem.XPTEST );
  }


  private void parseSGN( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tCALL\tF_SGN\n" );
    addLibItem( BasicLibrary.LibItem.F_SGN );
  }


  private void parseSQR( CharacterIterator iter ) throws PrgException
  {
    int pos = this.asmOut.length();
    parseEnclosedExpr( iter );
    Integer value = removeLastCodeIfConstExpr( pos );
    if( value != null ) {
      if( value.intValue() < 0 ) {
	throw new PrgException(
			"Wurzel aus negativer Zahl nicht m\u00F6glich" );
      }
      this.asmOut.append_LD_HL_nn(
		(int) Math.floor( Math.sqrt( value.doubleValue() ) ) );
    } else {
      this.asmOut.append( "\tCALL\tF_SQR\n" );
      addLibItem( BasicLibrary.LibItem.F_SQR );
    }
  }


  private void parseTARGET_ADDR( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append_LD_HL_nn( this.options.getBegAddr() );
  }


  private void parseTOP( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,M_TOP\n" );
    addLibItem( BasicLibrary.LibItem.M_TOP );
  }


  private void parseTRUE( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,0FFFFH\n" );
  }


  private void parseUSR( CharacterIterator iter ) throws PrgException
  {
    int usrNum = parseUsrNum( iter );
    parseUSR( iter, usrNum );
  }


  private void parseUSR(
			CharacterIterator iter,
			int               usrNum ) throws PrgException
  {
    parseEnclosedExpr( iter );
    if( !replaceLastCodeFrom_LD_HL_To_DE() ) {
      this.asmOut.append( "\tEX\tDE,HL\n" );
    }
    boolean insideCallable = (getCallableEntry() != null);
    if( insideCallable ) {
      this.asmOut.append( "\tPUSH\tIY\n" );
    }
    this.asmOut.append( "\tLD\tHL,(" );
    this.asmOut.append( getUsrLabel( usrNum ) );
    this.asmOut.append( ")\n"
		+ "\tCALL\tJP_HL\n" );
    if( insideCallable ) {
      this.asmOut.append( "\tPOP\tIY\n" );
    }
    addLibItem( BasicLibrary.LibItem.JP_HL );
    addLibItem( BasicLibrary.LibItem.E_USR );
  }


  private void parseVAL( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    if( checkToken( iter, ',' ) ) {
      int pos = this.asmOut.length();
      parseExpr( iter );
      Integer radix = removeLastCodeIfConstExpr( pos );
      if( radix != null ) {
	switch( radix.intValue() ) {
	  case 2:
	    this.asmOut.append( "\tCALL\tF_VLB\n" );
	    addLibItem( BasicLibrary.LibItem.F_VLB );
	    break;
	  case 10:
	    this.asmOut.append( "\tCALL\tF_VLI\n" );
	    addLibItem( BasicLibrary.LibItem.F_VLI );
	    break;
	  case 16:
	    this.asmOut.append( "\tCALL\tF_VLH\n" );
	    addLibItem( BasicLibrary.LibItem.F_VLH );
	    break;
	  default:
	    throw new PrgException(
		"Zahlenbasis in VAL-Funktion nicht unterst\u00FCtzt" );
	}
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	}
	this.asmOut.append( "\tCALL\tF_VAL\n" );
	addLibItem( BasicLibrary.LibItem.F_VAL );
      }
    } else {
      this.asmOut.append( "\tCALL\tF_VLI\n" );
      addLibItem( BasicLibrary.LibItem.F_VLI );
    }
    parseToken( iter, ')' );
  }


  private void parseWHITE( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorWhite( this.asmOut );
  }


  private void parseW_CHAR( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendWChar( this.asmOut );
  }


  private void parseW_PIXEL( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendWPixel( this.asmOut );
  }


  private void parseXPOS( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,(M_XPOS)\n" );
    addLibItem( BasicLibrary.LibItem.M_XYPO );
  }


  private void parseYELLOW( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.target.appendColorYellow( this.asmOut );
  }


  private void parseYPOS( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,(M_YPOS)\n" );
    addLibItem( BasicLibrary.LibItem.M_XYPO );
  }


  private void parseStrBIN( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseExpr( iter );
    if( checkToken( iter, ',' ) ) {
      int pos = this.asmOut.length();
      parseExpr( iter );
      Integer value = removeLastCodeIfConstExpr( pos );
      if( value != null ) {
	if( value.intValue() < 0 ) {
	  putWarningOutOfRange();
	}
	this.asmOut.append_LD_BC_nn( value.intValue() );
      } else {
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInBC( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
	}
      }
      this.asmOut.append( "\tCALL\tS_BINN\n" );
    } else {
      this.asmOut.append( "\tCALL\tS_BIN\n" );
    }
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_BIN );
  }


  private void parseStrCHR( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    int pos = this.asmOut.length();
    parseEnclosedExpr( iter );
    Integer value = removeLastCodeIfConstExpr( pos );
    if( value != null ) {
      this.asmOut.append_LD_A_n( value.intValue() );
      this.asmOut.append( "\tCALL\tS_CHRA\n" );
    } else {
      this.asmOut.append( "\tCALL\tS_CHRL\n" );
    }
    addLibItem( BasicLibrary.LibItem.S_CHR );
  }


  private void parseStrHEX( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseExpr( iter );
    if( checkToken( iter, ',' ) ) {
      int pos = this.asmOut.length();
      parseExpr( iter );
      String oldCode = this.asmOut.cut( pos );
      String newCode = convertCodeToValueInBC( oldCode );
      if( newCode != null ) {
	this.asmOut.append( newCode );
      } else {
	this.asmOut.append( "\tPUSH\tHL\n" );
	this.asmOut.append( oldCode );
	this.asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tPOP\tHL\n" );
      }
      this.asmOut.append( "\tCALL\tS_HEXN\n" );
      addLibItem( BasicLibrary.LibItem.S_HEXN );
    } else {
      this.asmOut.append( "\tCALL\tS_HEX\n" );
      addLibItem( BasicLibrary.LibItem.S_HEX );
    }
    parseToken( iter, ')' );
  }


  private void parseStrINCHAR( CharacterIterator iter )  throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tCALL\tS_INCH\n" );
    addLibItem( BasicLibrary.LibItem.S_INCH );
  }


  private void parseStrINKEY( CharacterIterator iter )  throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tCALL\tS_INKY\n" );
    addLibItem( BasicLibrary.LibItem.S_INKY );
  }


  private void parseStrLEFT( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ',' );
    int pos = this.asmOut.length();
    parseExpr( iter );
    String oldCode = this.asmOut.cut( pos );
    String newCode = convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      this.asmOut.append( newCode );
    } else {
      this.asmOut.append( "\tPUSH\tHL\n" );
      this.asmOut.append( oldCode );
      this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
    }
    this.asmOut.append( "\tCALL\tS_LEFT\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_LEFT );
  }


  private void parseStrLOWER( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ')' );
    this.asmOut.append( "\tCALL\tS_LWR\n" );
    addLibItem( BasicLibrary.LibItem.S_LWR );
  }


  private void parseStrLTRIM( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    this.asmOut.append( "\tCALL\tS_LTRM\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_LTRM );
  }


  private void parseStrMEMSTR( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
  }


  private void parseStrMIRROR( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    String text = checkStringLiteral( iter );
    if( text != null ) {
      if( text.isEmpty() ) {
	this.asmOut.append_LD_HL_xx( "M_EMPT" );
	addLibItem( BasicLibrary.LibItem.M_EMPT );
      } else {
	this.asmOut.appendStringLiteral(
		(new StringBuilder( text ).reverse()).toString(),
		"00H" );
      }
    } else {
      lockTmpStrBuf();
      parseStringPrimExpr( iter );
      this.asmOut.append( "\tCALL\tS_MIRR\n" );
      addLibItem( BasicLibrary.LibItem.S_MIRR );
    }
    parseToken( iter, ')' );
  }


  private void parseStrMID( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ',' );
    int pos = this.asmOut.length();
    parseExpr( iter );
    String oldCode2 = this.asmOut.cut( pos );
    String newCode2 = convertCodeToValueInDE( oldCode2 );
    if( checkToken( iter, ',' ) ) {
      parseExpr( iter );
      String oldCode3 = this.asmOut.cut( pos );
      String newCode3 = convertCodeToValueInBC( oldCode3 );
      if( (newCode2 != null) && (newCode3 != null) ) {
	this.asmOut.append( newCode2 );
	this.asmOut.append( newCode3 );
      } else {
	this.asmOut.append( "\tPUSH\tHL\n" );
	if( newCode2 != null ) {
	  this.asmOut.append( oldCode3 );
	  this.asmOut.append( "\tLD\tB,H\n"
				+ "\tLD\tC,L\n" );
	  this.asmOut.append( newCode2 );
	} else {
	  if( newCode3 != null ) {
	    this.asmOut.append( oldCode2 );
	    this.asmOut.append( "\tEX\tDE,HL\n" );
	    this.asmOut.append( newCode3 );
	  } else {
	    this.asmOut.append( oldCode2 );
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode3 );
	    this.asmOut.append( "\tLD\tB,H\n"
				+ "\tLD\tC,L\n"
				+ "\tPOP\tDE\n" );
	  }
	}
	this.asmOut.append( "\tPOP\tHL\n" );
      }
      this.asmOut.append( "\tCALL\tS_MIDN\n" );
      addLibItem( BasicLibrary.LibItem.S_MIDN );
    } else {
      if( newCode2 != null ) {
	this.asmOut.append( newCode2 );
      } else {
	this.asmOut.append( "\tPUSH\tHL\n" );
	this.asmOut.append( oldCode2 );
	this.asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
      }
      this.asmOut.append( "\tCALL\tS_MID\n" );
      addLibItem( BasicLibrary.LibItem.S_MID );
    }
    parseToken( iter, ')' );
  }


  private void parseStrRIGHT( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ',' );
    int pos = this.asmOut.length();
    parseExpr( iter );
    String oldCode = this.asmOut.cut( pos );
    String newCode = convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      this.asmOut.append( newCode );
    } else {
      this.asmOut.append( "\tPUSH\tHL\n" );
      this.asmOut.append( oldCode );
      this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
    }
    this.asmOut.append( "\tCALL\tS_RGHT\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_RGHT );
  }


  private void parseStrRTRIM( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    this.asmOut.append( "\tCALL\tS_RTRM\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_RTRM );
  }


  private void parseStrSPACE( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    int pos = this.asmOut.length();
    parseEnclosedExpr( iter );
    String oldCode = this.asmOut.cut( pos );
    String newCode = convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      this.asmOut.append( newCode );
    } else {
      this.asmOut.append( oldCode );
      this.asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
    }
    this.asmOut.append( "\tLD\tL,20H\n"
		+ "\tCALL\tS_STC\n" );
    addLibItem( BasicLibrary.LibItem.S_STC );
  }


  private void parseStrSTR( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    this.asmOut.append( "\tCALL\tS_STR\n" );
    addLibItem( BasicLibrary.LibItem.S_STR );
  }


  private void parseStrSTRING( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    int pos = this.asmOut.length();
    parseExpr( iter );
    parseToken( iter, ',' );
    Integer cnt = removeLastCodeIfConstExpr( pos );
    if( cnt != null ) {
      if( checkParseStringPrimExpr( iter ) ) {
	this.asmOut.append_LD_BC_nn( cnt.intValue() );
	this.asmOut.append( "\tCALL\tS_STS\n" );
	addLibItem( BasicLibrary.LibItem.S_STS );
      } else {
	parseExpr( iter );
	this.asmOut.append_LD_BC_nn( cnt.intValue() );
	this.asmOut.append( "\tCALL\tS_STC\n" );
	addLibItem( BasicLibrary.LibItem.S_STC );
      }
    } else {
      String  oldCode1 = this.asmOut.cut( pos );
      String  newCode1 = convertCodeToValueInBC( oldCode1 );
      boolean isStr    = false;
      if( checkParseStringPrimExpr( iter ) ) {
	isStr = true;
      } else {
	parseExpr( iter );
      }
      String code2 = this.asmOut.cut( pos );
      if( isOnly_LD_HL_xx( code2 ) ) {
	if( newCode1 != null ) {
	  this.asmOut.append( newCode1 );
	} else {
	  this.asmOut.append( oldCode1 );
	  this.asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n" );
	}
	this.asmOut.append( code2 );
      } else {
	if( newCode1 != null ) {
	  this.asmOut.append( code2 );
	  this.asmOut.append( newCode1 );
	} else {
	  this.asmOut.append( oldCode1 );
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( code2 );
	  this.asmOut.append( "\tPOP\tBC\n" );
	}
      }
      if( isStr ) {
	this.asmOut.append( "\tCALL\tS_STS\n" );
	addLibItem( BasicLibrary.LibItem.S_STS );
      } else {
	this.asmOut.append( "\tCALL\tS_STC\n" );
	addLibItem( BasicLibrary.LibItem.S_STC );
      }
    }
    parseToken( iter, ')' );
  }


  private void parseStrTARGET_ID( CharacterIterator iter ) throws PrgException
  {
    checkParseDummyArg( iter );
    this.asmOut.append( "\tLD\tHL,XTARID\n" );
    addLibItem( BasicLibrary.LibItem.XTARID );
  }


  private void parseStrTRIM( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    this.asmOut.append( "\tCALL\tS_TRIM\n" );
    parseToken( iter, ')' );
    addLibItem( BasicLibrary.LibItem.S_TRIM );
  }


  private void parseStrUPPER( CharacterIterator iter ) throws PrgException
  {
    lockTmpStrBuf();
    parseToken( iter, '(' );
    parseStringPrimExpr( iter );
    parseToken( iter, ')' );
    this.asmOut.append( "\tCALL\tS_UPR\n" );
    addLibItem( BasicLibrary.LibItem.S_UPR );
  }


	/* --- Parsen von Ausdruecke --- */

  private boolean checkParseStringPrimExpr( CharacterIterator iter )
							throws PrgException
  {
    boolean rv   = false;
    String  text = checkStringLiteral( iter );
    if( text != null ) {
      this.asmOut.append( "\tLD\tHL," );
      this.asmOut.append( getStringLiteralLabel( text ) );
      this.asmOut.newLine();
      rv = true;
    } else {
      rv = checkParseStringPrimVarExpr( iter );
    }
    return rv;
  }


  private boolean checkParseStringPrimVarExpr( CharacterIterator iter )
							throws PrgException
  {
    boolean rv     = false;
    int     begPos = iter.getIndex();
    String  name   = checkIdentifier( iter );
    if( name != null ) {
      if( name.endsWith( "$" ) ) {
	if( name.equals( "BIN$" ) ) {
	  parseStrBIN( iter );
	} else if( name.equals( "CHR$" ) ) {
	  parseStrCHR( iter );
	} else if( name.equals( "HEX$" ) ) {
	  parseStrHEX( iter );
	} else if( name.equals( "INCHAR$" ) ) {
	  parseStrINCHAR( iter );
	} else if( name.equals( "INKEY$" ) ) {
	  parseStrINKEY( iter );
	} else if( name.equals( "LEFT$" ) ) {
	  parseStrLEFT( iter );
	} else if( name.equals( "LOWER$" ) ) {
	  parseStrLOWER( iter );
	} else if( name.equals( "LTRIM$" ) ) {
	  parseStrLTRIM( iter );
	} else if( name.equals( "MEMSTR$" ) ) {
	  parseStrMEMSTR( iter );
	} else if( name.equals( "MID$" ) ) {
	  parseStrMID( iter );
	} else if( name.equals( "MIRROR$" ) ) {
	  parseStrMIRROR( iter );
	} else if( name.equals( "RIGHT$" ) ) {
	  parseStrRIGHT( iter );
	} else if( name.equals( "RTRIM$" ) ) {
	  parseStrRTRIM( iter );
	} else if( name.equals( "SPACE$" ) ) {
	  parseStrSPACE( iter );
	} else if( name.equals( "STR$" ) ) {
	  parseStrSTR( iter );
	} else if( name.equals( "STRING$" ) ) {
	  parseStrSTRING( iter );
	} else if( name.equals( "TARGET_ID$" ) ) {
	  parseStrTARGET_ID( iter );
	} else if( name.equals( "TRIM$" ) ) {
	  parseStrTRIM( iter );
	} else if( name.equals( "UPPER$" ) ) {
	  parseStrUPPER( iter );
	} else {
	  CallableEntry entry = this.name2Callable.get( name );
	  if( entry != null ) {
	    if( entry instanceof FunctionEntry ) {
	      if( ((FunctionEntry) entry).getReturnType()
						!= DataType.STRING )
	      {
		throw  new PrgException( "String-Ausdruck erwartet" );
	      }
	      parseCallableCall( iter, entry );
	    } else {
	      throw new PrgException(
		"Aufruf einer Prozedur an der Stelle nicht erlaubt" );
	    }
	  } else {
	    parseVariableExpr( iter, name, DataType.STRING );
	  }
	}
	rv = true;
      } else {
	iter.setIndex( begPos );
      }
    }
    return rv;
  }


  private String checkStringLiteral( CharacterIterator iter )
							throws PrgException
  {
    StringBuilder buf       = null;
    char          delimiter = skipSpaces( iter );
    if( delimiter == '\"' ) {
      buf        = new StringBuilder( 64 );
      char    ch = iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != delimiter) ) {
	if( ch > 0xFF ) {
	  throw16BitChar( ch );
	}
	if( this.options.getWarnNonAsciiChars()
	    && ((ch < '\u0020') || (ch > '\u007F')) )
	{
	  putWarningNonAsciiChar( ch );
	}
	buf.append( ch );
	ch = iter.next();
      }
      if( ch != delimiter ) {
	putWarning( "String-Literal nicht geschlossen" );
      }
      iter.next();
    }
    return buf != null ? buf.toString() : null;
  }


  private void parseEnclosedExpr( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseExpr( iter );
    parseToken( iter, ')' );
  }


  private void parseExpr( CharacterIterator iter ) throws PrgException
  {
    parseNotExpr( iter );
    for(;;) {
      if( checkKeyword( iter, "AND" ) ) {
	int pos = this.asmOut.length();
	parseNotExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
	this.asmOut.append( "\tCALL\tO_AND\n" );
	addLibItem( BasicLibrary.LibItem.O_AND );
      } else if( checkKeyword( iter, "OR" ) ) {
	int pos = this.asmOut.length();
	parseNotExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n" );
	  }
	  this.asmOut.append( "\tCALL\tO_OR\n" );
	  addLibItem( BasicLibrary.LibItem.O_OR );
	}
      } else if( checkKeyword( iter, "XOR" ) ) {
	int pos = this.asmOut.length();
	parseNotExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
	this.asmOut.append( "\tCALL\tO_XOR\n" );
	addLibItem( BasicLibrary.LibItem.O_XOR );
      } else {
	break;
      }
    }
  }


  private void parseNotExpr( CharacterIterator iter ) throws PrgException
  {
    if( checkKeyword( iter, "NOT" ) ) {
      parseCondExpr( iter );
      this.asmOut.append( "\tCALL\tO_NOT\n" );
      addLibItem( BasicLibrary.LibItem.O_NOT );
    } else {
      parseCondExpr( iter );
    }
  }


  private void parseCondExpr( CharacterIterator iter ) throws PrgException
  {
    if( checkParseStringPrimExpr( iter ) ) {

      // String-Vergleich
      char ch = skipSpaces( iter );
      if( ch == '<' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( iter );
	  this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STLE\n" );
	  addLibItem( BasicLibrary.LibItem.O_STLE );
	} else if( ch == '>' ) {
	  iter.next();
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( iter );
	  this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STNE\n" );
	  addLibItem( BasicLibrary.LibItem.O_STNE );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( iter );
	  this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STLT\n" );
	  addLibItem( BasicLibrary.LibItem.O_STLT );
	}
      } else if( ch == '=' ) {
	iter.next();
	this.asmOut.append( "\tPUSH\tHL\n" );
	parseStringPrimExpr( iter );
	this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STEQ\n" );
	addLibItem( BasicLibrary.LibItem.O_STEQ );
      } else if( ch == '>' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( iter );
	  this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STGE\n" );
	  addLibItem( BasicLibrary.LibItem.O_STGE );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( iter );
	  this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STGT\n" );
	  addLibItem( BasicLibrary.LibItem.O_STGT );
	}
      } else {
	throw new PrgException( "Vergleichsoperator erwartet" );
      }

    } else {

      // prufen auf numerischen Vergleich
      parseShiftExpr( iter );
      char ch = skipSpaces( iter );
      if( ch == '<' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  int pos = this.asmOut.length();
	  parseShiftExpr( iter );
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	    this.asmOut.append( "\tCALL\tO_LE\n" );
	    addLibItem( BasicLibrary.LibItem.O_LE );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tO_GE\n" );
	    addLibItem( BasicLibrary.LibItem.O_GE );
	  }
	} else if( ch == '>' ) {
	  iter.next();
	  int pos = this.asmOut.length();
	  parseShiftExpr( iter );
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n" );
	  }
	  this.asmOut.append( "\tCALL\tO_NE\n" );
	  addLibItem( BasicLibrary.LibItem.O_NE );
	} else {
	  int pos = this.asmOut.length();
	  parseShiftExpr( iter );
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	    this.asmOut.append( "\tCALL\tO_LT\n" );
	    addLibItem( BasicLibrary.LibItem.O_LT );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tO_GT\n" );
	    addLibItem( BasicLibrary.LibItem.O_GT );
	  }
	}
      }
      else if( ch == '=' ) {
	iter.next();
	int pos = this.asmOut.length();
	parseShiftExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
	this.asmOut.append( "\tCALL\tO_EQ\n" );
	addLibItem( BasicLibrary.LibItem.O_EQ );
      }
      else if( ch == '>' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  int pos = this.asmOut.length();
	  parseShiftExpr( iter );
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	    this.asmOut.append( "\tCALL\tO_GE\n" );
	    addLibItem( BasicLibrary.LibItem.O_GE );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n"
				+ "\tCALL\tO_LE\n" );
	    addLibItem( BasicLibrary.LibItem.O_LE );
	  }
	} else {
	  int pos = this.asmOut.length();
	  parseShiftExpr( iter );
	  String oldCode = this.asmOut.cut( pos );
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	    this.asmOut.append( "\tCALL\tO_GT\n" );
	    addLibItem( BasicLibrary.LibItem.O_GT );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_LT\n" );
	    addLibItem( BasicLibrary.LibItem.O_LT );
	  }
	}
      }
    }
  }


  private void parseShiftExpr( CharacterIterator iter ) throws PrgException
  {
    parseAddExpr( iter );
    for(;;) {
      if( checkKeyword( iter, "SHL" ) ) {
	int pos = this.asmOut.length();
	parseAddExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  this.asmOut.append( "\tSLA\tL\n"
		  + "\tRL\tH\n" );
	} else {
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	  }
	  this.asmOut.append( "\tCALL\tO_SHL\n" );
	  addLibItem( BasicLibrary.LibItem.O_SHL );
	}
      } else if( checkKeyword( iter, "SHR" ) ) {
	int pos = this.asmOut.length();
	parseAddExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  this.asmOut.append( "\tSRL\tH\n"
			+ "\tRR\tL\n" );
	} else {
	  String newCode = convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    this.asmOut.append( newCode );
	  } else {
	    this.asmOut.append( "\tPUSH\tHL\n" );
	    this.asmOut.append( oldCode );
	    this.asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	  }
	  this.asmOut.append( "\tCALL\tO_SHR\n" );
	  addLibItem( BasicLibrary.LibItem.O_SHR );
	}
      } else {
	break;
      }
    }
  }


  private void parseAddExpr( CharacterIterator iter ) throws PrgException
  {
    parseMulExpr( iter );
    for(;;) {
      char ch = skipSpaces( iter );
      if( ch == '+' ) {
	iter.next();
	int pos = this.asmOut.length();
	parseMulExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    this.asmOut.append( "\tCALL\tO_INC\n" );
	    addLibItem( BasicLibrary.LibItem.O_INC );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    this.asmOut.append( "\tCALL\tO_DEC\n" );
	    addLibItem( BasicLibrary.LibItem.O_DEC );
	  } else {
	    String newCode = convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      this.asmOut.append( newCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tHL\n" );
	      this.asmOut.append( oldCode );
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tO_ADD\n" );
	    addLibItem( BasicLibrary.LibItem.O_ADD );
	  }
	}
      } else if( ch == '-' ) {
	iter.next();
	int pos = this.asmOut.length();
	parseMulExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    this.asmOut.append( "\tCALL\tO_DEC\n" );
	    addLibItem( BasicLibrary.LibItem.O_DEC );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    this.asmOut.append( "\tCALL\tO_INC\n" );
	    addLibItem( BasicLibrary.LibItem.O_INC );
	  } else {
	    String newCode = convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      this.asmOut.append( newCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tHL\n" );
	      this.asmOut.append( oldCode );
	      this.asmOut.append( "\tPOP\tDE\n"
				+ "\tEX\tDE,HL\n" );
	    }
	    this.asmOut.append( "\tCALL\tO_SUB\n" );
	    addLibItem( BasicLibrary.LibItem.O_SUB );
	  }
	}
      } else if( checkKeyword( iter, "ADD" ) ) {
	int pos = this.asmOut.length();
	parseMulExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    this.asmOut.append( "\tINC\tHL\n" );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    this.asmOut.append( "\tDEC\tHL\n" );
	  } else {
	    String newCode = convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      this.asmOut.append( newCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tHL\n" );
	      this.asmOut.append( oldCode );
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tADD\tHL,DE\n" );
	  }
	}
      } else if( checkKeyword( iter, "SUB" ) ) {
	int pos = this.asmOut.length();
	parseMulExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    this.asmOut.append( "\tDEC\tHL\n" );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    this.asmOut.append( "\tINC\tHL\n" );
	  } else {
	    String newCode = convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      this.asmOut.append( newCode );
	    } else {
	      this.asmOut.append( "\tPUSH\tHL\n" );
	      this.asmOut.append( oldCode );
	      this.asmOut.append( "\tPOP\tDE\n"
				+ "\tEX\tDE,HL\n" );
	    }
	    this.asmOut.append( "\tOR\tA\n"
				+ "\tSBC\tHL,DE\n" );
	  }
	}
      } else {
	break;
      }
    }
  }


  private void parseMulExpr( CharacterIterator iter ) throws PrgException
  {
    parseUnaryExpr( iter );
    for(;;) {
      char ch = skipSpaces( iter );
      if( ch == '*' ) {
	iter.next();
	int pos = this.asmOut.length();
	parseUnaryExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n" );
	}
	this.asmOut.append( "\tCALL\tO_MUL\n" );
	addLibItem( BasicLibrary.LibItem.O_MUL );
      } else if( ch == '/' ) {
	iter.next();
	int pos = this.asmOut.length();
	parseUnaryExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n"
			+ "\tEX\tDE,HL\n" );
	}
	this.asmOut.append( "\tCALL\tO_DIV\n" );
	addLibItem( BasicLibrary.LibItem.O_DIV );
      } else if( checkKeyword( iter, "MOD" ) ) {
	int pos = this.asmOut.length();
	parseUnaryExpr( iter );
	String oldCode = this.asmOut.cut( pos );
	String newCode = convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  this.asmOut.append( newCode );
	} else {
	  this.asmOut.append( "\tPUSH\tHL\n" );
	  this.asmOut.append( oldCode );
	  this.asmOut.append( "\tPOP\tDE\n"
			+ "\tEX\tDE,HL\n" );
	}
	this.asmOut.append( "\tCALL\tO_MOD\n" );
	addLibItem( BasicLibrary.LibItem.O_MOD );
      } else {
	break;
      }
    }
  }


  private void parseUnaryExpr( CharacterIterator iter ) throws PrgException
  {
    char ch = skipSpaces( iter );
    if( ch == '-' ) {
      iter.next();
      Integer value = readNumber( iter );
      if( value != null ) {
	this.asmOut.append_LD_HL_nn( -value.intValue() );
      } else {
	parsePrimExpr( iter );
	this.asmOut.append( "\tCALL\tNEGHL\n" );
	addLibItem( BasicLibrary.LibItem.ABS_NEG_HL );
      }
    } else {
      if( ch == '+' ) {
	iter.next();
      }
      parsePrimExpr( iter );
    }
  }


  private void parsePrimExpr( CharacterIterator iter ) throws PrgException
  {
    char ch = skipSpaces( iter );
    if( ch == '(' ) {
      iter.next();
      parseExpr( iter );
      parseToken( iter, ')' );
    }
    else if( (ch >= '0') && (ch <= '9') ) {
      this.asmOut.append_LD_HL_nn( parseNumber( iter ) );
    }
    else if( ch == '&' ) {
      ch = iter.next();
      if( (ch == 'B') || (ch == 'b') ) {
	ch = iter.next();
	if( (ch == '0') || (ch == '1') ) {
	  int value = 0;
	  while( (ch == '0') || (ch == '1') ) {
	    value <<= 1;
	    if( ch == '1' ) {
	      value |= 1;
	    }
	    ch = iter.next();
	  }
	  this.asmOut.append_LD_HL_nn( value );
	} else {
	  throw new PrgException( "0 oder 1 erwartet" );
	}
      } else if( (ch == 'H') || (ch == 'h') ) {
	iter.next();
	Integer value = readHex( iter );
	if( value == null ) {
	  throwHexDigitExpected();
	}
	this.asmOut.append_LD_HL_nn( value );
      } else {
	throw new PrgException( "B oder H erwartet" );
      }
    }
    else if( ch == '\'' ) {
      ch = iter.next();
      iter.next();
      if( this.options.getWarnNonAsciiChars()
	  && ((ch < '\u0020') || (ch > '\u007F')) )
      {
	putWarningNonAsciiChar( ch );
      }
      parseToken( iter, '\'' );
      check8BitChar( ch );
      this.asmOut.append_LD_HL_nn( ch );
    } else {
      String name = checkIdentifier( iter );
      if( name != null ) {
	if( name.equals( "ABS" ) ) {
	  parseABS( iter );
	} else if( name.equals( "ASC" ) ) {
	  parseASC( iter );
	} else if( name.equals( "BLACK" ) ) {
	  parseBLACK( iter );
	} else if( name.equals( "BLINKING" ) ) {
	  parseBLINKING( iter );
	} else if( name.equals( "BLUE" ) ) {
	  parseBLUE( iter );
	} else if( name.equals( "CYAN" ) ) {
	  parseCYAN( iter );
	} else if( name.equals( "DEEK" ) ) {
	  parseDEEK( iter );
	} else if( name.equals( "ERR" ) ) {
	  parseERR( iter );
	} else if( name.equals( "FALSE" ) ) {
	  parseFALSE( iter );
	} else if( name.equals( "GREEN" ) ) {
	  parseGREEN( iter );
	} else if( name.equals( "H_CHAR" ) ) {
	  parseH_CHAR( iter );
	} else if( name.equals( "H_PIXEL" ) ) {
	  parseH_PIXEL( iter );
	} else if( name.equals( "IN" ) || name.equals( "INP" ) ) {
	  parseIN( iter );
	} else if( name.equals( "INSTR" ) ) {
	  parseINSTR( iter );
	} else if( name.equals( "JOYST" ) ) {
	  parseJOYST( iter );
	} else if( name.equals( "LASTSCREEN" ) ) {
	  parseLASTSCREEN( iter );
	} else if( name.equals( "LEN" ) ) {
	  parseLEN( iter );
	} else if( name.equals( "MAGENTA" ) ) {
	  parseMAGENTA( iter );
	} else if( name.equals( "MAX" ) ) {
	  parseMAX( iter );
	} else if( name.equals( "MIN" ) ) {
	  parseMIN( iter );
	} else if( name.equals( "PEEK" ) ) {
	  parsePEEK( iter );
	} else if( name.equals( "POINT" ) || name.equals( "PTEST" ) ) {
	  parsePTEST( iter );
	} else if( name.equals( "RED" ) ) {
	  parseRED( iter );
	} else if( name.equals( "RND" ) ) {
	  parseRND( iter );
	} else if( name.equals( "SGN" ) ) {
	  parseSGN( iter );
	} else if( name.equals( "SQR" ) ) {
	  parseSQR( iter );
	} else if( name.equals( "TARGET_ADDR" ) ) {
	  parseTARGET_ADDR( iter );
	} else if( name.equals( "TOP" ) ) {
	  parseTOP( iter );
	} else if( name.equals( "TRUE" ) ) {
	  parseTRUE( iter );
	} else if( name.equals( "USR" ) ) {
	  parseUSR( iter );
	} else if( name.equals( "USR0" ) ) {
	  parseUSR( iter, 0 );
	} else if( name.equals( "USR1" ) ) {
	  parseUSR( iter, 1 );
	} else if( name.equals( "USR2" ) ) {
	  parseUSR( iter, 2 );
	} else if( name.equals( "USR3" ) ) {
	  parseUSR( iter, 3 );
	} else if( name.equals( "USR4" ) ) {
	  parseUSR( iter, 4 );
	} else if( name.equals( "USR5" ) ) {
	  parseUSR( iter, 5 );
	} else if( name.equals( "USR6" ) ) {
	  parseUSR( iter, 6 );
	} else if( name.equals( "USR7" ) ) {
	  parseUSR( iter, 7 );
	} else if( name.equals( "USR8" ) ) {
	  parseUSR( iter, 8 );
	} else if( name.equals( "USR9" ) ) {
	  parseUSR( iter, 9 );
	} else if( name.equals( "VAL" ) ) {
	  parseVAL( iter );
	} else if( name.equals( "WHITE" ) ) {
	  parseWHITE( iter );
	} else if( name.equals( "W_CHAR" ) ) {
	  parseW_CHAR( iter );
	} else if( name.equals( "W_PIXEL" ) ) {
	  parseW_PIXEL( iter );
	} else if( name.equals( "XPOS" ) ) {
	  parseXPOS( iter );
	} else if( name.equals( "YELLOW" ) ) {
	  parseYELLOW( iter );
	} else if( name.equals( "YPOS" ) ) {
	  parseYPOS( iter );
	} else {
	  CallableEntry entry = this.name2Callable.get( name );
	  if( entry != null ) {
	    if( entry instanceof FunctionEntry ) {
	      if( ((FunctionEntry) entry).getReturnType()
						!= DataType.INTEGER )
	      {
		throwIntExprExpected();
	      }
	      parseCallableCall( iter, entry );
	    } else {
	      throw new PrgException(
		"Aufruf einer Prozedur an der Stelle nicht erlaubt" );
	    }
	  } else {
	    if( name.endsWith( "$" ) ) {
	      throwIntExprExpected();
	    }
	    parseVariableExpr( iter, name, DataType.INTEGER );
	  }
	}
      } else {
	throwUnexpectedChar( skipSpaces( iter ) );
      }
    }
  }


  private void parseLineExprList( CharacterIterator iter ) throws PrgException
  {
    java.util.List<String> labels = new ArrayList<String>( 32 );
    labels.add( parseDestLineExpr( iter ) );
    while( checkToken( iter, ',' ) ) {
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


  private int parseNumber( CharacterIterator iter ) throws PrgException
  {
    Integer value = readNumber( iter );
    if( value == null ) {
      throw new PrgException( "Zahl erwartet" );
    }
    return value.intValue();
  }


  private void parseStringPrimExpr( CharacterIterator iter )
							throws PrgException
  {
    String text = checkStringLiteral( iter );
    if( text != null ) {
      String label = getStringLiteralLabel( text );
      this.asmOut.append( "\tLD\tHL," );
      this.asmOut.append( label );
      this.asmOut.newLine();
    } else {
      if( !checkParseStringPrimVarExpr( iter ) ) {
	throwStringExprExpected();
      }
    }
  }


  private Integer readHex( CharacterIterator iter ) throws PrgException
  {
    Integer rv = null;
    char    ch = iter.current();
    if( ((ch >= '0') && (ch <= '9'))
	|| ((ch >= 'A') && (ch <= 'F'))
	|| ((ch >= 'a') && (ch <= 'f')) )
    {
      int value = 0;
      while( ((ch >= '0') && (ch <= '9'))
	     || ((ch >= 'A') && (ch <= 'F'))
	     || ((ch >= 'a') && (ch <= 'f')) )
      {
	value <<= 4;
	if( (ch >= '0') && (ch <= '9') ) {
	  value |= (ch - '0');
	}
	else if( (ch >= 'A') && (ch <= 'F') ) {
	  value |= (ch - 'A' + 10);
	}
	else if( (ch >= 'a') && (ch <= 'f') ) {
	  value |= (ch - 'a' + 10);
	}
	value &= 0xFFFF;
	ch = iter.next();
      }
      rv = new Integer( value );
    }
    return rv;
  }


  private Long readLineNum( CharacterIterator iter ) throws PrgException
  {
    Long rv = null;
    char ch = skipSpaces( iter );
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


  private Integer readNumber( CharacterIterator iter ) throws PrgException
  {
    Integer rv = null;
    char    ch = skipSpaces( iter );
    if( (ch >= '0') && (ch <= '9') ) {
      int value = ch - '0';
      ch        = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	value = (value * 10) + (ch - '0');
	if( value > MAX_VALUE ) {
	  throw new PrgException( "Zahl zu gro\u00DF" );
	}
	ch = iter.next();
      }
      rv = new Integer( value );
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
    parseToken( iter, '=' );
    if( dstVar.getDataType() == DataType.INTEGER ) {
      if( dstVar.hasStaticAddr() ) {
	parseExpr( iter );
	dstVar.writeCode_LD_Var_HL( this.asmOut );
      } else {
	int pos = this.asmOut.length();
	parseExpr( iter );
	Integer value = removeLastCodeIfConstExpr( pos );
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
	  String newCode = convertCodeToValueInDE( oldCode );
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
      String  text = checkStringLiteral( iter );
      if( text != null ) {
	char ch = skipSpaces( iter );
	if( isEndOfInstr( iter ) ) {
	  if( dstVar.hasStaticAddr() ) {
	    dstVar.ensureStaticAddrInDE( this.asmOut, false );
	  } else {
	    if( !replaceLastCodeFrom_LD_HL_To_DE() ) {
	      this.asmOut.append( "\tEX\tDE,HL\n" );
	    }
	  }
	  this.asmOut.append( "\tLD\tHL," );
	  this.asmOut.append( getStringLiteralLabel( text ) );
	  this.asmOut.append( "\n"
		+ "\tCALL\tASGSL\n" );
	  addLibItem( BasicLibrary.LibItem.ASGSL );
	  done = true;
	}
      }

      // pruefen, ob nur eine String-Variable zugewiesen wird
      if( !done ) {
	iter.setIndex( srcPos );
	this.asmOut.setLength( dstPos );
	SimpleVarInfo srcVar = checkVariable( iter );
	if( srcVar != null ) {
	  char ch = skipSpaces( iter );
	  if( isEndOfInstr( iter ) ) {
	    if( srcVar.hasStaticAddr() ) {
	      srcVar.ensureAddrInHL( this.asmOut );
	      dstVar.ensureStaticAddrInDE( this.asmOut, true );
	    } else {
	      this.asmOut.insert( dstPos, "\tPUSH\tHL\n" );
	      srcVar.ensureAddrInHL( this.asmOut );
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tASGSV\n" );
	    addLibItem( BasicLibrary.LibItem.ASGSV );
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
	if( checkParseStringPrimExpr( iter ) ) {
	  if( isEndOfInstr( iter ) ) {
	    if( dstVar.hasStaticAddr() ) {
	      dstVar.ensureStaticAddrInDE( this.asmOut, true );
	    } else {
	      this.asmOut.append( "\tPOP\tDE\n" );
	    }
	    this.asmOut.append( "\tCALL\tASGSM\n" );
	    addLibItem( BasicLibrary.LibItem.ASGSM );
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
	  text = checkStringLiteral( iter );
	  if( text != null ) {
	    this.asmOut.append( "\tLD\tHL," );
	    this.asmOut.append( getStringLiteralLabel( text ) );
	    this.asmOut.newLine();
	  } else {
	    int pos = this.asmOut.length();
	    if( !checkParseStringPrimVarExpr( iter ) ) {
	      throwStringExprExpected();
	    }
	    String tmpCode = this.asmOut.cut( pos );
	    if( isOnly_LD_HL_xx( tmpCode ) ) {
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
	  if( skipSpaces( iter ) != '+' ) {
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
	this.asmOut.append( "\tCALL\tASGSL\n" );
	addLibItem( BasicLibrary.LibItem.ASGSL );
      }
    }
  }


	/* --- Hilfsfunktionen --- */

  private void addLibItem( BasicLibrary.LibItem libItem )
  {
    if( this.codeGenEnabled )
      this.libItems.add( libItem );
  }


  private static void check8BitChar( char ch ) throws PrgException
  {
    if( (ch == 0) || (ch > 0xFF) ) {
      throw new PrgException( "Zeichen \'" + ch
		+ "\' au\u00DFerhalb des 8-Bit-Wertebereiches" );
    }
  }


  private void checkAppendBreakCheck()
  {
    if( this.options.canBreakAlways() ) {
      this.asmOut.append( "\tCALL\tXCKBRK\n" );
      addLibItem( BasicLibrary.LibItem.XCKBRK );
    }
  }


  private void checkCreateStackFrame()
  {
    CallableEntry entry = getCallableEntry();
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
	      this.asmOut.append_LD_HL_xx( "M_EMPT" );
	      addLibItem( BasicLibrary.LibItem.M_EMPT );
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


  private String checkIdentifier( CharacterIterator iter )
  {
    String rv = null;
    char   ch = skipSpaces( iter );
    if( ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      int pos = iter.getIndex();
      ch      = iter.next();
      while( ((ch >= 'A') && (ch <= 'Z'))
	     || ((ch >= 'a') && (ch <= 'z'))
	     || ((ch >= '0') && (ch <= '9'))
	     || (ch == '_') )
      {
	ch = iter.next();
      }
      int len = iter.getIndex() - pos;
      if( ch == '$' ) {
	len++;
      }
      StringBuilder buf = new StringBuilder( len );
      iter.setIndex( pos );
      ch = iter.current();
      do {
	buf.append( Character.toUpperCase( ch ) );
	ch = iter.next();
	--len;
      } while( len > 0 );
      rv = buf.toString();
    }
    return rv;
  }


  private void checkMainPrgScope() throws PrgException
  {
    if( !this.mainPrg ) {
      CallableEntry entry = getCallableEntry();
      if( entry == null ) {
	throw new PrgException( "Anweisung nur im Hauptprogramm"
		+ " oder in einer Funktion/Prozedur zul\u00E4ssig" );
      }
    }
  }


  private void checkParseDummyArg( CharacterIterator iter )
							throws PrgException
  {
    if( checkToken( iter, '(' ) ) {
      readNumber( iter );
      parseToken( iter,')' );
    }
  }


  private boolean checkReturnValueAssignment(
				CharacterIterator iter,
				String            name ) throws PrgException
  {
    boolean       rv    = false;
    CallableEntry entry = getCallableEntry();
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


  private static boolean checkToken( CharacterIterator iter, char ch )
  {
    boolean rv = false;
    if( skipSpaces( iter ) == ch ) {
      iter.next();
      rv = true;
    }
    return rv;
  }


  private static void checkVarName( String varName ) throws PrgException
  {
    if( Arrays.binarySearch( sortedReservedWords, varName ) >= 0 ) {
      throw new PrgException( "Reserviertes Schl\u00FCsselwort"
			+ " als Variablenname nicht erlaubt" );
    }
  }


  private String convertCodeToValueInBC( String text )
  {
    return convertCodeToValueInRR( text, "BC" );
  }


  private String convertCodeToValueInDE( String text )
  {
    return convertCodeToValueInRR( text, "DE" );
  }


  private String convertCodeToValueInRR( String text, String rr )
  {
    String rv = null;
    if( (text != null) && (rr.length() == 2) ) {
      String label  = "";
      int    tabPos = text.indexOf( '\t' );
      if( tabPos > 0 ) {
	label = text.substring( 0, tabPos );
	text  = text.substring( tabPos );
      }
      if( text.startsWith( "\tLD\tHL," ) ) {
	if( text.indexOf( '\n' ) == (text.length() - 1) ) {
	  rv = String.format(
			"%s\tLD\t%s%s",
			label,
			rr,
			text.substring( 6 ) );
	}
      }
      else if( text.startsWith( "\tLD\tH," )
	       || text.startsWith( "\tLD\tL," ) )
      {
	int len = text.length();
	int eol = text.indexOf( '\n' );
	if( (eol > 0) && ((eol + 1) < len) ) {
	  String line1 = text.substring( 0, eol + 1 );
	  String line2 = text.substring( eol + 1 );
	  if( line2.indexOf( '\n' ) == (line2.length() - 1) ) {
	    if( line1.startsWith( "\tLD\tH,(IY" )
		&& line2.startsWith( "\tLD\tL,(IY" ) )
	    {
	      rv = String.format(
			"%s\tLD\t%c%s\tLD\t%c%s",
			label,
			rr.charAt( 0 ),
			line1.substring( 5 ),
			rr.charAt( 1 ),
			line2.substring( 5 ) );
	    }
	    else if( line1.startsWith( "\tLD\tL,(IY" )
		     && line2.startsWith( "\tLD\tH,(IY" ) )
	    {
	      rv = String.format(
			"%s\tLD\t%c%s\tLD\t%c%s",
			label,
			rr.charAt( 1 ),
			line1.substring( 5 ),
			rr.charAt( 0 ),
			line2.substring( 5 ) );
	    }
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Ermittlung der umgebenden Funktion/Prozedur
   *
   * Diese kann nur der unterste Eintrag auf dem Stack sein.
   */
  private CallableEntry getCallableEntry()
  {
    CallableEntry rv = null;
    if( !this.structureStack.isEmpty() ) {
      StructureEntry entry = this.structureStack.get( 0 );
      if( entry instanceof CallableEntry ) {
	rv = (CallableEntry) entry;
      }
    }
    return rv;
  }


  private String getStringLiteralLabel( String text )
  {
    String label = this.str2Label.get( text );
    if( label == null ) {
      label = nextLabel();
      this.str2Label.put( text, label );
    }
    return label;
  }


  private String getUsrLabel( int usrNum )
  {
    String rv = String.format( "M_USR%d", usrNum );
    this.usrLabels.add( rv );
    return rv;
  }


  private boolean isEndOfInstr( CharacterIterator iter )
  {
    char ch = skipSpaces( iter );
    return (ch == CharacterIterator.DONE) || (ch == ':');
  }


  private static boolean isOnly_LD_HL_xx( String text )
  {
    boolean rv = false;
    if( text != null ) {
      int tabPos = text.indexOf( '\t' );
      if( tabPos > 0 ) {
	text = text.substring( tabPos );
      }
      if( text.startsWith( "\tLD\tHL," ) ) {
	if( text.indexOf( '\n' ) == (text.length() - 1) ) {
	  rv = true;
	}
      }
      else if( text.startsWith( "\tLD\tH," )
	       || text.startsWith( "\tLD\tL," ) )
      {
	int len = text.length();
	int eol = text.indexOf( '\n' );
	if( (eol > 0) && ((eol + 1) < len) ) {
	  String line1 = text.substring( 0, eol + 1 );
	  String line2 = text.substring( eol + 1 );
	  if( line2.indexOf( '\n' ) == (line2.length() - 1) ) {
	    if( line1.startsWith( "\tLD\tH,(IY" )
		&& line2.startsWith( "\tLD\tL,(IY" ) )
	    {
	      rv = true;
	    }
	    else if( line1.startsWith( "\tLD\tL,(IY" )
		     && line2.startsWith( "\tLD\tH,(IY" ) )
	    {
	      rv = true;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void lockTmpStrBuf() throws PrgException
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


  private String nextLabel()
  {
    return String.format( "M%d", this.labelNum++ );
  }


  private void parse2ArgsTo_DE_HL( CharacterIterator iter )
							throws PrgException
  {
    int pos = this.asmOut.length();
    parseExpr( iter );
    parseToken( iter, ',' );
    Integer v1 = removeLastCodeIfConstExpr( pos );
    if( v1 != null ) {
      parseExpr( iter );
      this.asmOut.append_LD_DE_nn( v1.intValue() );
    } else {
      String oldCode1 = this.asmOut.cut( pos );
      String newCode1 = convertCodeToValueInDE( oldCode1 );
      parseExpr( iter );
      String code2 = this.asmOut.cut( pos );
      if( isOnly_LD_HL_xx( code2 ) ) {
	if( newCode1 != null ) {
	  this.asmOut.append( newCode1 );
	} else {
	  this.asmOut.append( oldCode1 );
	  this.asmOut.append( "\tEX\tDE,HL\n" );
	}
	this.asmOut.append( code2 );
      } else {
	this.asmOut.append( oldCode1 );
	this.asmOut.append( "\tPUSH\tHL\n" );
	this.asmOut.append( code2 );
	this.asmOut.append( "\tPOP\tDE\n" );
      }
    }
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
    String name = checkIdentifier( iter );
    if( name == null ) {
      throw new PrgException( "Name der Funktion/Prozedur erwartet" );
    }
    if( Arrays.binarySearch( sortedReservedWords, name ) >= 0 ) {
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
    Set<String>            argSet  = new TreeSet<String>();
    java.util.List<String> argList = new ArrayList<String>();
    if( checkToken( iter, '(' ) ) {
      if( !checkToken( iter, ')' ) ) {
	do {
	  String argName = checkIdentifier( iter );
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
		 * Entscheident sind die bei der Implementierung.
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
	} while( checkToken( iter, ',' ) );
	parseToken( iter, ')' );
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
			this.curSourceLineNum,
			this.curBasicLineNum,
			name );
      } else {
	entry = new SubEntry(
			this.curSourceLineNum,
			this.curBasicLineNum,
			name );
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
      if( !this.suppressExit ) {
	this.asmOut.append( "\tJP\tXEXIT\n" );
	this.suppressExit = true;
      }
      this.mainPrg = false;
    }

    // eigentliche Implementierung
    this.asmOut.append( entry.getLabel() );
    this.asmOut.append( ":\n" );
  }


  private void parseCallableCall(
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
      parseToken( iter, '(' );
      for( int i = 0; i < nArgs; i++ ) {
	if( i > 0 ) {
	  parseToken( iter, ',' );
	}
	if( entry.getArgType( i ) == DataType.STRING ) {
	  String text = checkStringLiteral( iter );
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
	      parseStringPrimExpr( iter );
	      this.asmOut.append( "\tCALL\tSMACP\n"
				+ "\tPUSH\tHL\n" );
	      addLibItem( BasicLibrary.LibItem.SMACP );
	    }
	  }
	} else {
	  parseExpr( iter );
	  this.asmOut.append( "\tPUSH\tHL\n" );
	}
      }
      parseToken( iter, ')' );
    } else {
      if( checkToken( iter, '(' ) ) {
	parseToken( iter, ')' );
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
    entry.putCallLineNum( this.curSourceLineNum, this.curBasicLineNum );
  }


  private String parseDestLineExpr( CharacterIterator iter )
						throws PrgException
  {
    BasicLineExpr lineExpr = BasicLineExpr.checkBasicLineExpr(
						iter,
						this.curSourceLineNum,
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
    String text = checkStringLiteral( iter );
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
      parseToken( iter, ';' );
    }
    SimpleVarInfo varInfo = checkVariable( iter );
    if( varInfo != null ) {
      if( varInfo.getDataType() != DataType.STRING ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      if( text != null ) {
	throw new PrgException( "String-Variable erwartet" );
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


  private void parsePointTo_DE_HL( CharacterIterator iter )
							throws PrgException
  {
    boolean enclosed = checkToken( iter, '(' );
    parse2ArgsTo_DE_HL( iter );
    if( enclosed ) {
      parseToken( iter, ')' );
    }
  }


  private void parsePointToMem(
			CharacterIterator iter,
			String            labelX,
			String            labelY ) throws PrgException
  {
    boolean enclosed = checkToken( iter, '(' );
    parseExpr( iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( labelX );
    this.asmOut.append( "),HL\n" );
    parseToken( iter, ',' );
    parseExpr( iter );
    this.asmOut.append( "\tLD\t(" );
    this.asmOut.append( labelY );
    this.asmOut.append( "),HL\n" );
    if( enclosed ) {
      parseToken( iter, ')' );
    }
  }


  private void parseToken(
			CharacterIterator iter,
			char              ch ) throws PrgException
  {
    if( skipSpaces( iter ) != ch ) {
       throw new PrgException( String.format( "\'%c\' erwartet", ch ) );
    }
    iter.next();
  }


  private int parseUsrNum( CharacterIterator iter ) throws PrgException
  {
    Integer usrNum = readNumber( iter );
    if( usrNum == null ) {
      throw new PrgException( "Nummer der USR-Funktion erwartet" );
    }
    if( (usrNum.intValue() < 0) || (usrNum.intValue() > 9) ) {
      throw new PrgException(
		"Ung\u00FCltige USR-Funktionsnummer (0...9 erlaubt)" );
    }
    return usrNum.intValue();
  }


  /*
   * Die Methode parst eine Variable.
   * Der Variablenname wurde zu dem Zeitpunkt bereits gelesen,
   * weshalb er uebergeben wird.
   */
  private void parseVariableExpr(
			CharacterIterator iter,
			String            varName,
			DataType          dataType ) throws PrgException
  {
    SimpleVarInfo varInfo = checkVariable( iter, varName );
    if( varInfo != null ) {
      if( varInfo.getDataType() != dataType ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      if( dataType == DataType.STRING ) {
	throw new PrgException(
			"String-Variable oder String-Funktion erwartet" );
      } else {
	throw new PrgException(
			"Numerische Variable oder Funktion erwartet" );
      }
    }
    varInfo.ensureValueInHL( this.asmOut );
  }


  public void putWarning( String msg )
  {
    appendLineNumMsgToErrLog( msg, "Warnung" );
  }


  public void putWarning( int sourceLineNum, long basicLineNum, String msg )
  {
    appendLineNumMsgToErrLog( sourceLineNum, basicLineNum, msg, "Warnung" );
  }


  private void putWarningNonAsciiChar( char ch )
  {
    
    putWarning(
	String.format(
		"\'%c\': kein ASCII-Zeichen,"
			+ " kann im Zielsystem ein anderes Zeichen sein",
		ch ) );
  }


  private void putWarningOutOfRange()
  {
    putWarning( "Wert au\u00DFerhalb des Wertebereiches" );
  }


  /*
   * Wenn die letzte erzeugte Code-Zeile das Laden
   * des HL-Registers mit einem konstanten Wert darstellt,
   * wird die Code-Zeile geloescht und der Wert zurueckgeliefert.
   */
  private Integer removeLastCodeIfConstExpr( int pos )
  {
    Integer rv       = null;
    String  instText = this.asmOut.substring( pos );
    int     tabPos   = instText.indexOf( '\t' );
    if( tabPos > 0 ) {
      pos      = tabPos;
      instText = instText.substring( tabPos );
    }
    if( instText.startsWith( "\tLD\tHL," ) && instText.endsWith( "H\n" ) ) {
      try {
	int v = Integer.parseInt(
			instText.substring( 7, instText.length() - 2 ),
			16 );
	if( (v & 0x8000) != 0 ) {
	  // negativer Wert
	  v = -(0x10000 - (v & 0xFFFF));
	}
	rv = new Integer( v );
      }
      catch( NumberFormatException ex ) {}
    }
    if( rv != null ) {
      this.asmOut.setLength( pos );
    }
    return rv;
  }


  private boolean replaceLastCodeFrom_LD_HL_To_BC()
  {
    return replaceLastCodeFrom_LD_HL_To_RR( "BC" );
  }


  private boolean replaceLastCodeFrom_LD_HL_To_DE()
  {
    return replaceLastCodeFrom_LD_HL_To_RR( "DE" );
  }


  private boolean replaceLastCodeFrom_LD_HL_To_RR( String rr )
  {
    boolean rv       = false;
    int     pos      = this.asmOut.getLastLinePos();
    String  instText = this.asmOut.substring( pos );
    int     tabPos   = instText.indexOf( '\t' );
    if( tabPos > 0 ) {
      pos      = tabPos;
      instText = instText.substring( tabPos );
    }
    if( instText.startsWith( "\tLD\tHL," ) ) {
      this.asmOut.replace( pos + 4, pos + 6, rr );
      rv = true;
    } else {
      // Zugriff auf lokale Variable?
      if( (rr.length() == 2)
	  && (instText.startsWith( "\tLD\tH," )
	      || instText.startsWith( "\tLD\tL," )) )
      {
	String line2 = this.asmOut.cut( pos );
	String line1 = this.asmOut.cut( this.asmOut.getLastLinePos() );
	if( (line1.indexOf( "\tLD\tH,(IY" ) >= 0)
	    && (line2.indexOf( "\tLD\tL,(IY" ) >= 0) )
	{
	  line1 = line1.replace(
			"\tLD\tH,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 0 ) ) );
	  line2 = line2.replace(
			"\tLD\tL,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 1 ) ) );
	  rv = true;
	}
	else if( (line1.indexOf( "\tLD\tL,(IY" ) >= 0)
		 && (line2.indexOf( "\tLD\tH,(IY" ) >= 0) )
	{
	  line1 = line1.replace(
			"\tLD\tL,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 1 ) ) );
	  line2 = line2.replace(
			"\tLD\tH,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 0 ) ) );
	  rv = true;
	}
	this.asmOut.append( line1 );
	this.asmOut.append( line2 );
      }
    }
    return rv;
  }


  private void setCodeGenEnabled( boolean state )
  {
    this.codeGenEnabled = state;
    this.asmOut.setEnabled( state );
  }


  private static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE) && PrgUtil.isWhitespace( ch ) ) {
      ch = iter.next();
    }
    return ch;
  }


  private static void throw16BitChar( char ch ) throws PrgException
  {
    throw new PrgException(
		String.format(
			"\'%c\': 16-Bit-Unicodezeichen nicht erlaubt",
			ch ) );
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


  private static void throwHexDigitExpected() throws PrgException
  {
    throw new PrgException( "Hexadezimalziffer erwartet" );
  }


  private static void throwIndexOutOfRange() throws PrgException
  {
    throw new PrgException(
		"Index au\u00DFerhalb des g\u00FCltigen Bereichs" );
  }


  private static void throwIntExprExpected() throws PrgException
  {
    throw new PrgException( "Integer-Ausdruck erwartet" );
  }


  private static void throwStringExprExpected() throws PrgException
  {
    throw new PrgException( "String-Ausdruck erwartet" );
  }


  private static void throwStringLitOrVarExpected() throws PrgException
  {
    throw new PrgException( "String-Literal oder String-Variable erwartet" );
  }


  private static void throwUnexpectedChar( char ch ) throws PrgException
  {
    if( ch == CharacterIterator.DONE ) {
      throw new PrgException( "Unerwartetes Ende der Zeile" );
    }
    StringBuilder buf = new StringBuilder( 32 );
    if( ch >= '\u0020' ) {
      buf.append( (char) '\'' );
      buf.append( ch );
      buf.append( "\': " );
    }
    buf.append( "Unerwartetes Zeichen" );
    throw new PrgException( buf.toString() );
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

