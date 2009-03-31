/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.awt.Component;
import java.io.IOException;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.swing.SwingUtilities;
import jkcemu.base.*;
import jkcemu.text.*;
import jkcemu.programming.*;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.system.*;


public class BasicCompiler extends PrgThread
{
  private enum Platform {
			AC1_LLC2, HC900, HUEBLERMC, KRAMERMC,
			Z1013, Z9001, UNKNOWN };

  private enum LibItem { INLN, R_INT, P_HEXA, P_INT, P_LTXT, P_TEXT, P_TAB,
			DATA, DREAD,
			F_RND, F_SGN, F_SIZE, F_SQR,
			O_OR, O_AND, O_NOT,
			O_LT, O_LE, O_GT, O_GE, O_EQ, O_NE,
			O_ADD, O_SUB, O_MUL, O_DIV,
			ABS_NEG_HL,
			H_CP, H_DIV, H_NEXT, CKSTCK, JP_HL,
			INCHR, INKEY, CKBRK, ARYADR,
			E_ARG, E_ARIT, E_BRK, E_NXWF, E_REWG,
			OUTNL, M_RND, M_LEN, M_WINT, M_ARY, M_TOP,
			XOUTCH, XINCH, XINKEY, XCKBRK };

  private static final int MAX_VALUE   = 32767;
  private static final int MIN_VALUE   = -32768;
  private static final int MAGIC_FOR   = 'F';
  private static final int MAGIC_GOSUB = 'G';

  private BasicOptions         basicOptions;
  private BasicSourceFormatter sourceFormatter;
  private StringBuilder        asmOut;
  private StringBuilder        dataOut;
  private String               sysTitle;
  private String               endOfLineLabel;
  private Stack<String>        elseLabels;
  private Platform             platform;
  private Set<LibItem>         libItems;
  private Set<String>          varLabels;
  private Set<Long>            basicLineNums;
  private Map<Long,LineInfo>   destBasicLineNums;
  private Map<Long,LineInfo>   restoreBasicLineNums;
  private Collection<Long>     dataBasicLineNums;
  private Stack<ForNextEntry>  forNextEntries;
  private boolean              ac1CompatibilityChecked;
  private boolean              z1013CompatibilityChecked;
  private boolean              printStringOpen;
  private boolean              defmStringOpen;
  private boolean              suppressExit;
  private int                  labelNum;
  private long                 curBasicLineNum;
  private long                 lastBasicLineNum;


  public BasicCompiler(
		EmuThread    emuThread,
		EditText     editText,
		String       sourceText,
		Appendable   logOut,
		BasicOptions options,
		boolean      forceRunProgram )
  {
    super( emuThread, editText, sourceText, logOut, options, forceRunProgram );
    this.basicOptions    = options;
    this.sourceFormatter = null;
    if( this.basicOptions.getFormatSource() ) {
      int capacity = 1024;
      if( sourceText != null ) {
	capacity += sourceText.length();
      }
      this.sourceFormatter = new BasicSourceFormatter( capacity );
    }
    EmuSys emuSys = emuThread.getEmuSys();
    this.sysTitle = emuSys.getTitle();
    this.platform = Platform.UNKNOWN;
    if( (emuSys instanceof AC1)
	|| (emuSys instanceof LLC2) )
    {
      this.platform = Platform.AC1_LLC2;
    }
    else if( emuSys instanceof KC85 ) {
      this.platform = Platform.HC900;
    }
    else if( (emuSys instanceof HueblerEvertMC)
	     || (emuSys instanceof HueblerGraphicsMC) )
    {
      this.platform = Platform.HUEBLERMC;
    }
    else if( emuSys instanceof KramerMC ) {
      this.platform = Platform.KRAMERMC;
    }
    else if( emuSys instanceof Z1013 ) {
      this.platform  = Platform.Z1013;
    }
    else if( emuSys instanceof Z9001 ) {
      this.platform = Platform.Z9001;
    }
    this.libItems                  = new HashSet<LibItem>();
    this.varLabels                 = new HashSet<String>();
    this.basicLineNums             = new HashSet<Long>();
    this.destBasicLineNums         = new HashMap<Long,LineInfo>();
    this.dataBasicLineNums         = null;
    this.restoreBasicLineNums      = null;
    this.forNextEntries            = null;
    this.asmOut                    = null;
    this.dataOut                   = null;
    this.endOfLineLabel            = null;
    this.elseLabels                = null;
    this.ac1CompatibilityChecked   = false;
    this.z1013CompatibilityChecked = false;
    this.printStringOpen           = false;
    this.defmStringOpen            = false;
    this.suppressExit              = false;
    this.labelNum                  = 1;
    this.curBasicLineNum           = -1;
    this.lastBasicLineNum          = -1;
  }


  public void appendLineNumMsgToLog(
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
    appendToLog( buf.toString() );
  }


  public void appendLineNumMsgToLog( String msg, String msgType )
  {
    appendLineNumMsgToLog(
		getSourceLineNum(),
		this.curBasicLineNum,
		msg,
		msgType );
  }


	/* --- ueberschriebene Methoden --- */

  public void run()
  {
    EditFrm editFrm = this.editText.getEditFrm();
    this.asmOut     = new StringBuilder( 4 * getSourceLength() );
    try {
      try {
	clearErrorCount();
	appendToLog( "Compiliere...\n" );
	if( this.platform == Platform.UNKNOWN ) {
	  appendToLog(
		String.format(
			"Fehler: Das gerade emulierte System (%s)"
				+ " wird nicht unterst\u00FCtzt.\n",
			this.sysTitle ) );
	  incErrorCount();
	} else {
	  parseSource();
	  closePrintString();
	  if( this.running ) {
	    putLibrary();
	    writeInit();
	    if( (getErrorCount() == 0)
		&& (this.forceRun
		    || this.options.getCodeToEmu()
		    || this.options.getCodeToFile()) )
	    {
	      (new Z80Assembler(
			this.emuThread,
			this.editText,
			this.asmOut.toString(),
			this.logOut,
			this.options,
			this.forceRun )).assemble();
	    }
	  }
	}
      }
      catch( TooManyErrorsException ex ) {
	appendToLog( "\nAbgebrochen aufgrund zu vieler Fehler\n" );
      }
      if( this.running ) {
	if( getErrorCount() > 0 ) {
	  appendErrorCountToLog();
	} else {
	  if( this.sourceFormatter != null ) {
	    fireReplaceSourceText( this.sourceFormatter.toString() );
	  }
	  if( this.basicOptions.getShowAsm() && (this.asmOut != null) ) {
	    final String       text    = this.asmOut.toString();
	    final BasicOptions options = new BasicOptions( this.basicOptions );
	    SwingUtilities.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    openAsmText( text, options );
			  }
			} );
	  }
	  appendToLog( "Fertig\n" );
	}
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.endsWith( "\n" ) ) {
	  appendToLog( "Fehler: " + msg );
	} else {
	  appendToLog( "Fehler: " + msg + "\n" );
	}
      } else {
	appendToLog( "Ein-/Ausgabefehler\n" );
      }
    }
    catch( Exception ex ) {
      SwingUtilities.invokeLater( new ErrorMsg( editFrm, ex ) );
    }
    if( editFrm != null )
      editFrm.threadTerminated( this );
  }


	/* --- private Methoden --- */

  private void appendToLogLineNotFound( LineInfo lineInfo )
						throws TooManyErrorsException
  {
    if( lineInfo != null ) {
      Long missingBasicLineNum = lineInfo.getMissingBasicLineNum();
      if( missingBasicLineNum != null ) {
	StringBuilder buf = new StringBuilder( 128 );
	if( lineInfo.getSourceLineNum() > 0 ) {
	  buf.append( "Fehler in Zeile " );
	  buf.append( lineInfo.getSourceLineNum() );
	  if( lineInfo.getBasicLineNum() >= 0 ) {
	    buf.append( " (BASIC-Zeilennummer " );
	    buf.append( lineInfo.getBasicLineNum() );
	    buf.append( (char) ')' );
	  }
	  buf.append( ": " );
	}
	buf.append( "Zeile mit BASIC-Zeilennummer " );
	buf.append( missingBasicLineNum );
	buf.append( " nicht gefunden\n" );
	appendToLog( buf.toString() );
	incErrorCount();
      }
    }
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
    char ch     = skipSpaces( iter );
    int  begPos = iter.getIndex();
    int  len    = keyword.length();
    for( int i = 0; i < len; i++ ) {
      if( ch == '.' ) {
	iter.next();
	break;
      }
      if( keyword.charAt( i ) != Character.toUpperCase( ch ) ) {
	iter.setIndex( begPos );
	return false;
      }
      ch = iter.next();
    }
    if( fmtSource && (this.sourceFormatter != null) ) {
      this.sourceFormatter.replaceByKeyword( begPos, keyword, space );
    }
    return true;
  }


  private boolean checkPrintStringLiteral( CharacterIterator iter )
							throws PrgException
  {
    boolean rv = false;
    char    ch = skipSpaces( iter );
    if( (ch == '\'') || (ch == '\"') ) {
      if( this.sourceFormatter != null ) {
	this.sourceFormatter.formatLast();
      }
      rv           = true;
      char endChar = ch;
      ch           = iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != endChar) ) {
	putCharToPrintString( ch );
	ch = iter.next();
      }
      if( ch == endChar ) {
	iter.next();
	if( this.sourceFormatter != null ) {
	  this.sourceFormatter.copyLast();
	}
      } else {
	if( this.sourceFormatter != null ) {
	  this.sourceFormatter.copyLast();
	  this.sourceFormatter.append( endChar );
	}
	putWarning( "String-Literal nicht geschlossen" );
      }
    }
    return rv;
  }


  private String checkVarGetAddrExpr( CharacterIterator iter )
							throws PrgException
  {
    String rv = null;
    char   ch = skipSpaces( iter );
    if( ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      String varName = readSimpleVarName( iter );
      int    len     = varName.length();
      if( len > 0 ) {
	if( varName.equals( "ABS" )
	    || varName.equals( "DEEK" )
	    || varName.equals( "ELSE" )
	    || varName.equals( "HEX" )
	    || varName.equals( "INCHAR" )
	    || varName.equals( "INKEY" )
	    || varName.equals( "INP" )
	    || varName.equals( "IN" )
	    || varName.equals( "LEN" )
	    || varName.equals( "PEEK" )
	    || varName.equals( "RND" )
	    || varName.equals( "SIZE" )
	    || varName.equals( "SGN" )
	    || varName.equals( "SQR" )
	    || varName.equals( "TOP" ) )
	{
	  throw new PrgException( varName
			+ ": Der Name der Variable ist ein reserviertes"
			+ " Schl\u00FCsselwort und somit nicht erlaubt" );
	}
	if( len > 1 ) {
	  ac1BasicMismatch();
	  z1013BasicMismatch();
	}
	rv = "M_V" + varName;
	this.varLabels.add( rv );
      }
    }
    else if( ch == '@' ) {
      iter.next();
      Integer value = checkEnclosedConstExpr( iter );
      if( value != null ) {
	rv = getArrayVarAddrExpr( value.intValue() );
      } else {
	parseEnclosedExpr( iter );
	putCode( "\tCALL\tARYADR\n" );
	this.libItems.add( LibItem.ARYADR );
	rv = "HL";
      }
    }
    return rv;
  }


  private void closePrintString()
  {
    if( this.printStringOpen ) {
      if( this.defmStringOpen ) {
	writeCode( "\'\n" );
	this.defmStringOpen = false;
      }
      writeCode( "\tDEFB\t0\n" );
      this.printStringOpen = false;
    }
  }


  private static String createBasicLineLabel( long basicLineNum )
  {
    return "L" + String.valueOf( basicLineNum );
  }


  private String createLabelForDestBasicLine( long basicLineNum )
  {
    Long keyObj = new Long( basicLineNum );
    if( !this.destBasicLineNums.containsKey( keyObj ) ) {
      this.destBasicLineNums.put(
		keyObj,
		new LineInfo( getSourceLineNum(), this.curBasicLineNum ) );
    }
    return createBasicLineLabel( basicLineNum );
  }


  private String getArrayVarAddrExpr( int idx ) throws PrgException
  {
    StringBuilder buf = new StringBuilder();
    buf.append( "M_ARY" );
    this.libItems.add( LibItem.M_ARY );
    if( idx < 0 ) {
      throw new PrgException( "Negativer Index in Array nicht erlaubt" );
    }
    if( idx >= this.basicOptions.getArraySize() ) {
      appendLineNumMsgToLog(
		"@( " + String.valueOf( idx ) + " ) liegt au\u00DFerhalb"
			+ " der in den Optionen eingestellten Gr\u00F6\u00DFe"
			+ " des @-Variablen-Arrays",
		"Warnung" );
    }
    if( idx > 0 ) {
      buf.append( (char) '+' );
      buf.append( getHex4( idx * 2 ) );
    }
    return buf.toString();
  }


  private static String getHex2( int value )
  {
    value &= 0xFF;
    return value < 0xA0 ?
		String.format( "%02XH", value )
		: String.format( "0%02XH", value );
  }


  private static String getHex4( int value )
  {
    value &= 0xFFFF;
    return value < 0xA000 ?
		String.format( "%04XH", value )
		: String.format( "0%04XH", value );
  }


  private void parseSource() throws IOException, TooManyErrorsException
  {
    if( this.basicOptions.getStructuredForNext() ) {
      this.forNextEntries = new Stack<ForNextEntry>();
    } else {
      this.forNextEntries = null;
    }
    String line = readLine();
    while( this.running && (line != null) ) {
      parseLine( line.trim() );
      line = readLine();
    }
    if( this.basicOptions.getShowAsm() && !this.suppressExit ) {
      putCode( "\n;Programmende\n" );
    }
    putExit();

    // Pruefen, ob zu jeder FOR-Anweisung auch eine NEXT-Anweisung existiert
    if( this.forNextEntries != null ) {
      for( ForNextEntry entry : this.forNextEntries ) {
	appendLineNumMsgToLog(
			entry.getSourceLineNum(),
			entry.getBasicLineNum(),
			"FOR-Schleife nicht mit NEXT abgeschlossen",
			"Warnung" );
      }
    }

    // Vorhandensein der Sprungziele und der RESTORE-Zeilennummern pruefen
    java.util.List<LineInfo> lineInfos = null;

    Collection<Long> reqBasicLineNums = this.destBasicLineNums.keySet();
    if( reqBasicLineNums != null ) {
      for( Long reqBasicLineNum : reqBasicLineNums ) {
	if( !this.basicLineNums.contains( reqBasicLineNum ) ) {
	  LineInfo lineInfo = this.destBasicLineNums.get( reqBasicLineNum );
	  if( lineInfo == null ) {
	    lineInfo = new LineInfo( -1, -1 );
	  }
	  lineInfo.setMissingBasicLineNum( reqBasicLineNum );
	  if( lineInfos == null ) {
	    lineInfos = new ArrayList<LineInfo>();
	  }
	  lineInfos.add( lineInfo );
	}
      }
    }
    if( this.restoreBasicLineNums != null ) {
      reqBasicLineNums = this.restoreBasicLineNums.keySet();
      if( reqBasicLineNums != null ) {
	for( Long reqBasicLineNum : reqBasicLineNums ) {
	  boolean exists = false;
	  if( this.dataBasicLineNums != null ) {
	    exists = this.dataBasicLineNums.contains( reqBasicLineNum );
	  }
	  if( !exists ) {
	    LineInfo lineInfo = this.destBasicLineNums.get( reqBasicLineNum );
	    if( lineInfo == null ) {
	      lineInfo = new LineInfo( -1, -1 );
	    }
	    lineInfo.setMissingBasicLineNum( reqBasicLineNum );
	    if( lineInfos == null ) {
	      lineInfos = new ArrayList<LineInfo>();
	    }
	    lineInfos.add( lineInfo );
	  }
        }
      }
    }
    if( lineInfos != null ) {
      try {
	Collections.sort( lineInfos );
      }
      catch( ClassCastException ex ) {}
      for( LineInfo lineInfo : lineInfos ) {
	appendToLogLineNotFound( lineInfo );
      }
    }

    /*
     * Reservierung fuer Variablen-Array pruefen
     * Array-Zugriffe mit einem konstanten Index werden bereits
     * bei deren Parsen geprueft.
     * Dehalb erfolgt hier nur eine Warnung,
     * wenn es Array-Zugriffe mit variablen Index gibt,
     * jedoch fuer das Array kein Platz reserviert wurde.
     */
    if( this.libItems.contains( LibItem.ARYADR )
	&& (this.basicOptions.getArraySize() <= 0) )
    {
      appendToLog( "Warnung: Es werden @-Variablen verwendet,"
		+ " ohne dass in den Optionen daf\u00FCr Platz"
		+ " reserviert wurde.\n" );
    }
  }


  private void parseLine( String lineText ) throws
						IOException,
						TooManyErrorsException
  {
    this.ac1CompatibilityChecked   = false;
    this.z1013CompatibilityChecked = false;
    this.endOfLineLabel            = null;
    if( this.elseLabels != null ) {
      this.elseLabels.clear();
    }
    if( this.basicOptions.getShowAsm() && (lineText.length() > 0) ) {
      putCode( "\n;" );
      putCode( lineText );
      putCode( '\n' );
    }
    CharacterIterator iter = null;
    try {
      iter = new StringCharacterIterator( lineText );

      // Zeilenummer
      char ch = iter.first();
      while( (ch != CharacterIterator.DONE)
	     && Character.isWhitespace( ch ) )
      {
	ch = iter.next();
      }
      this.curBasicLineNum = -1;
      if( (ch >= '0') && (ch <= '9') ) {
	this.curBasicLineNum = ch - '0';
	ch = iter.next();
	while( (ch >= '0') && (ch <= '9') ) {
	  this.curBasicLineNum = (this.curBasicLineNum * 10L) + (ch - '0');
	  if( this.curBasicLineNum > Integer.MAX_VALUE ) {
	    throw new PrgException( "BASIC-Zeilennummer zu gro\u00DF" );
	  }
	  ch = iter.next();
	}
	if( this.basicOptions.getStrictAC1MiniBASIC() ) {
	  if( this.curBasicLineNum == 0 ) {
	    putWarning( "BASIC-Zeilennummer 0 nicht m\u00F6glich"
		+ " im originalen AC1-Mini-BASIC" );
	  }
	  else if( this.curBasicLineNum > MAX_VALUE ) {
	    putWarning( "BASIC-Zeilennummer "
		+ String.valueOf( this.curBasicLineNum )
		+ " zu gro\u00DF f\u00FCr originales AC1-Mini-BASIC" );
	  }
	}
	if( this.basicOptions.getStrictZ1013TinyBASIC() ) {
	  if( this.curBasicLineNum == 0 ) {
	    putWarning( "BASIC-Zeilennummer 0 nicht m\u00F6glich"
		+ " im originalen Z1013-Tiny-BASIC" );
	  }
	  else if( this.curBasicLineNum > MAX_VALUE ) {
	    putWarning( "BASIC-Zeilennummer "
		+ String.valueOf( this.curBasicLineNum )
		+ " zu gro\u00DF f\u00FCr originales Z1013-Tiny-BASIC" );
	  }
	}
	if( this.basicLineNums.contains( this.curBasicLineNum ) ) {
	  super.appendLineNumMsgToLog(
			"BASIC-Zeilennummer "
				+ String.valueOf( this.curBasicLineNum )
				+ " bereits vorhanden",
			"Fehler" );
	  incErrorCount();
	}
	if( this.curBasicLineNum < this.lastBasicLineNum ) {
	  putWarning(
		"BASIC-Zeilennummer nicht in aufsteigender Reihenfolge" );
	}
	this.basicLineNums.add( this.curBasicLineNum );
	this.lastBasicLineNum = this.curBasicLineNum;
	putCode( createBasicLineLabel( this.curBasicLineNum ) );
	putCode( ":\n" );
      }

      // ggf. Quelltext formatieren
      if( this.sourceFormatter != null ) {
	this.sourceFormatter.beginLine( iter, this.curBasicLineNum );
      }

      // Anweisungen
      parseInstructions( iter );

      // Labels, die auf das Zeilenende zeigen
      if( this.elseLabels != null ) {
	while( !this.elseLabels.isEmpty() ) {
	  putCode( this.elseLabels.pop() );
	  putCode( ":\n" );
	}
      }
      if( this.endOfLineLabel != null ) {
	putCode( this.endOfLineLabel );
	putCode( ":\n" );
      }
    }
    catch( PrgException ex ) {
      String msg = ex.getMessage();
      if( msg == null ) {
	msg = "Unbekannter Fehler";
      }
      appendLineNumMsgToLog( msg, "Fehler" );
      if( iter != null ) {
	StringBuilder buf = new StringBuilder( iter.getEndIndex() + 16 );
	buf.append( "    " );
	int  pos = iter.getIndex();
	char ch  = iter.first();
	while( ch != CharacterIterator.DONE ) {
	  buf.append( ch );
	  if( iter.getIndex() == pos ) {
	    buf.append( " ??? " );
	  }
	  ch = iter.next();
	}
	buf.append( (char) '\n' );
	appendToLog( buf.toString() );
      }
      incErrorCount();
    }

    // ggf. Zeile bis zum Ende formatieren
    if( this.sourceFormatter != null ) {
      this.sourceFormatter.finishLine();
    }
  }


  private void parseInstructions( CharacterIterator iter ) throws PrgException
  {
    if( skipSpaces( iter ) != CharacterIterator.DONE ) {
      boolean loop = true;
      while( loop ) {
	parseInstruction( iter );
	char ch = skipSpaces( iter );
	if( ch == ';' ) {
	  if( this.sourceFormatter != null ) {
	    this.sourceFormatter.formatLast();
	    this.sourceFormatter.setSpace( false );
	  }
	  ch = iter.next();
	}
	else if( ch == ':' ) {
	  if( this.sourceFormatter != null ) {
	    this.sourceFormatter.formatLast();
	    this.sourceFormatter.setSpace( false );
	  }
	  ch = iter.next();
	  ac1BasicMismatch();
	  z1013BasicMismatch();
	} else {

	  /*
	   * Das Schluesselwort ELSE kann ohne einen Doppelpunkt
	   * oder Semikolon hinter der vorherigen Anweisung stehen.
	   * Aus diesem Grund muss es hier bereits behandelt werden.
	   */
	  if( checkInstruction( iter, "ELSE" ) ) {
	    ac1BasicMismatch();
	    z1013BasicMismatch();
	    boolean elseDone = false;
	    if( this.endOfLineLabel == null ) {
	      this.endOfLineLabel = nextLabel();
	    }
	    if( this.basicOptions.getPreferRelativeJumps() ) {
	      putCode( "\tJR\t" );
	    } else {
	      putCode( "\tJP\t" );
	    }
	    putCode( this.endOfLineLabel );
	    putCode( '\n' );
	    if( this.elseLabels != null ) {
	      if( !this.elseLabels.isEmpty() ) {
		putCode( this.elseLabels.pop() );
		putCode( ":\n" );
		elseDone = true;
	      }
	    }
	    if( !elseDone ) {
	      throw new PrgException(
			"ELSE ohne zugeh\u00F6riger IF-Anweisung" );
	    }
	  } else {
	    loop = false;
	    if( ch != CharacterIterator.DONE )
	      throwUnexpectedChar( ch );
	  }
	}
      }
    }
  }


  /*
   * Die Methode parst eine BASIC-Anweisung.
   * Die Suchreihenfolge entspricht der des originalen Z1013-BASIC,
   * damit die Kompatibilitaet auch gegeben ist,
   * wenn die Anweisungsnamen mit einem Punkt abgekuerzt werden.
   * Anweisungen, die es im originalen Z1013-BASIC nicht gibt,
   * stehen am Ende der Suchreihenfolge,
   * um die Kompatibilitaet zu bewahren.
   */
  private void parseInstruction( CharacterIterator iter ) throws PrgException
  {
    char ch     = skipSpaces( iter );
    int  begPos = iter.getIndex();
    if( ch == '!' ) {
      iter.next();
      if( this.sourceFormatter != null ) {
	this.sourceFormatter.replaceByKeyword( begPos, "REM", true );
      }
      ac1BasicMismatch();
      z1013BasicMismatch();
      parseREM( iter );
    }
    else if( ch == '?' ) {
      iter.next();
      if( this.sourceFormatter != null ) {
	this.sourceFormatter.replaceByKeyword( begPos, "PRINT", true );
      }
      ac1BasicMismatch();
      z1013BasicMismatch();
      parsePRINT( iter );
    } else {
      if( checkInstruction( iter, "NEXT" ) ) {
	ac1BasicMismatch();
	parseNEXT( iter );
      }
      else if( checkInstruction( iter, "LET" ) ) {
	parseLET( iter );
      }
      else if( checkInstruction( iter, "IF" ) ) {
	parseIF( iter );
      }
      else if( checkInstruction( iter, "GOTO" ) ) {
	parseGOTO( iter );
      }
      else if( checkInstruction( iter, "GOSUB" ) ) {
	parseGOSUB( iter );
      }
      else if( checkInstruction( iter, "RETURN" ) ) {
	parseRETURN();
      }
      else if( checkInstruction( iter, "REM" ) ) {
	parseREM( iter );
      }
      else if( checkInstruction( iter, "FOR" ) ) {
	ac1BasicMismatch();
	parseFOR( iter );
      }
      else if( checkInstruction( iter, "INPUT" ) ) {
	parseINPUT( iter );
      }
      else if( checkInstruction( iter, "PRINT" ) ) {
	parsePRINT( iter );
      }
      else if( checkInstruction( iter, "STOP" ) ) {
	putExit();
      }
      else if( checkInstruction( iter, "CALL" ) ) {
	parseCALL( iter );
      }
      else if( checkInstruction( iter, "OUTCHAR" ) ) {
	parseOUTCHAR( iter );
      }
      else if( checkInstruction( iter, "OUT" ) ) {
	parseOUT( iter );
      }
      else if( checkInstruction( iter, "O$" ) ) {
	ac1BasicMismatch();
	parseOutString( iter );
      }
      else if( checkInstruction( iter, "I$" ) ) {
	ac1BasicMismatch();
	parseInString( iter );
      }
      else if( checkInstruction( iter, "POKE" ) ) {
	parsePOKE( iter );
      }
      else if( checkInstruction( iter, "TAB" ) ) {
	parseTAB( iter );
      }
      else if( checkInstruction( iter, "BYTE" ) ) {
	parseBYTE( iter );
      }
      else if( checkInstruction( iter, "WORD" ) ) {
	parseWORD( iter );
      }
      else if( checkInstruction( iter, "CLS" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseCLS();
      }
      else if( checkInstruction( iter, "DATA" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseDATA( iter );
      }
      else if( checkInstruction( iter, "DOKE" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseDOKE( iter );
      }
      else if( checkKeyword( iter, "ELSE", false, false ) ) {
	// ELSE wird hinter einer Anweisung behandelt
	iter.setIndex( begPos );
      }
      else if( checkInstruction( iter, "READ" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseREAD( iter );
      }
      else if( checkInstruction( iter, "RESTORE" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseRESTORE( iter );
      }
      else if( checkInstruction( iter, "END" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	putExit();
      } else {
	String varAddrExpr = checkVarGetAddrExpr( iter );
	if( varAddrExpr != null ) {
	  parseAssignment( iter, varAddrExpr );
	} else {
	  if( skipSpaces( iter ) != CharacterIterator.DONE ) {
	    throw new PrgException( "Variable oder Anweisung erwartet" );
	  }
	}
      }
    }
  }


  private void parseBYTE( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = false;
    if( skipSpaces( iter ) == '(' ) {
      iter.next();
      enclosed = true;
    }
    Integer value = checkConstExpr( iter );
    if( value != null ) {
      putCode( "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'" );
      putCode( String.format( "%02X", value.intValue() & 0xFF ) );
      putCode( "\'\n"
		+ "\tDEFB\t0\n" );
      this.libItems.add( LibItem.P_LTXT );
    } else {
      parseExpr( iter );
      putCode( "\tLD\tA,L\n" );
      putCode( "\tCALL\tP_HEXA\n" );
      this.libItems.add( LibItem.P_HEXA );
    }
    if( enclosed ) {
      parseToken( iter, ')' );
    } else {
      ac1BasicMismatch();
      z1013BasicMismatch();
    }
  }


  private void parseCALL( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    putCode( "\tCALL\tJP_HL\n" );
    this.libItems.add( LibItem.JP_HL );
    if( this.basicOptions.getPrintCalls() ) {
      appendLineNumMsgToLog(
		"Befindet sich an der aufgerufenen Adresse"
			+ " auch der gew\u00FCnschte Maschinencode?",
		"CALL-Anweisung" );
    }
  }


  private void parseCLS() throws PrgException
  {
    putCode( "\tLD\tA,0CH\n"
		+ "\tCALL\tXOUTCH\n" );
    this.libItems.add( LibItem.XOUTCH );
  }


  private void parseDATA( CharacterIterator iter ) throws PrgException
  {
    if( this.dataOut == null ) {
      this.dataOut = new StringBuilder( 0x400 );
    }
    if( this.curBasicLineNum >= 0 ) {
      if( this.dataBasicLineNums == null ) {
	this.dataBasicLineNums = new HashSet<Long>();
      }
      if( this.dataBasicLineNums.add( new Long( this.curBasicLineNum ) ) ) {
	this.dataOut.append( (char) 'D' );
	this.dataOut.append( this.curBasicLineNum );
	this.dataOut.append( ":\n" );
      }
    }
    boolean isFirst = true;
    iter.previous();
    do {
      iter.next();
      Integer value = checkConstExpr( iter );
      if( value == null ) {
	throw new PrgException( "Konstanter Ausdruck bei DATA erforderlich" );
      }
      if( isFirst ) {
	this.dataOut.append( "\tDEFW\t" );
	isFirst = false;
      } else {
	this.dataOut.append( (char) ',' );
      }
      this.dataOut.append( getHex4( value.intValue() ) );
    } while( skipSpaces( iter ) == ',' );
    this.dataOut.append( '\n' );
    this.libItems.add( LibItem.DATA );
  }


  private void parseDOKE( CharacterIterator iter ) throws PrgException
  {
    Integer addr = checkConstExpr( iter );
    if( addr != null ) {
      parseToken( iter, ',' );
      parseExpr( iter );
      putCode( "\tLD\t(" );
      putCode( getHex4( addr.intValue() ) );
      putCode( "),HL\n" );
    } else {
      parseExpr( iter );
      parseToken( iter, ',' );
      putCode( "\tPUSH\tHL\n" );
      parseExpr( iter );
      putCode( "\tPOP\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
    }
  }


  /*
   * Wenn die Adresse der Laufvariablen und die Schrittweite konstant sind,
   * kann abhaengig von den Optionen die Schleife als strukturierte Schleife
   * mit optimiertem Programmcode uebersetzt werden.
   */
  private void parseFOR( CharacterIterator iter ) throws PrgException
  {
    // Stack-Check
    if( this.basicOptions.getCheckStack() ) {
      putCode( "\tCALL\tCKSTCK\n" );
      this.libItems.add( LibItem.CKSTCK );
    }

    // Adresse der Laufvariablen parsen und Programmcode temporaer merken
    StringBuilder varAddrOut = new StringBuilder( 16 );
    StringBuilder asmOut     = this.asmOut;
    String varAddrExpr       = null;
    try {
      this.asmOut = varAddrOut;
      varAddrExpr = checkVarGetAddrExpr( iter );
    }
    finally {
      this.asmOut = asmOut;
    }
    if( varAddrExpr == null ) {
      throwVarExpected();
    }

    // Schleifengrenzen parsen und Programmcode temporaer merken
    StringBuilder boundsOut = new StringBuilder( 16 );
    try {
      this.asmOut = boundsOut;
      parseAssignment( iter, varAddrExpr );
    }
    finally {
      this.asmOut = asmOut;
    }
    if( !checkInstruction( iter, "TO" ) ) {
      throw new PrgException( "\'TO\' erwartet" );
    }
    Integer endValue = checkConstExpr( iter );
    if( endValue == null ) {
      try {
	this.asmOut = boundsOut;
	parseExpr( iter );
	putCode( "\tPUSH\tHL\n" );
      }
      finally {
	this.asmOut = asmOut;
      }
    }

    // Schrittweite parsen und Programmcode temporaer merken
    StringBuilder stepOut   = null;
    Integer       stepValue = null;
    if( checkInstruction( iter, "STEP" ) ) {
      stepValue = checkConstExpr( iter );
      if( stepValue == null ) {
	stepOut = new StringBuilder( 16 );
	try {
	  this.asmOut = stepOut;
	  parseExpr( iter );
	  putCode( "\tPUSH\tHL\n" );
	}
	finally {
	  this.asmOut = asmOut;
	}
      }
    } else {
      stepValue = new Integer( 1 );
    }

    // pruefen, ob die Schleife optimiert werden kann
    ForNextEntry forNextEntry = null;
    if( this.forNextEntries != null ) {
      forNextEntry = new ForNextEntry(
				getSourceLineNum(),
				this.curBasicLineNum );
      this.forNextEntries.push( forNextEntry );
      if( !varAddrExpr.equals( "HL" ) && (stepValue != null) ) {
	forNextEntry.setCounterVariableAddrExpr( varAddrExpr );
	forNextEntry.setStepValue( stepValue.intValue() );
      } else {
	forNextEntry = null;	// keine Optimierung
      }
    }

    // endgueltiger Programmcode erzeugen
    String label = nextLabel();
    if( forNextEntry != null ) {
      this.asmOut.append( varAddrOut );
      this.asmOut.append( boundsOut );
      if( stepOut != null ) {
	this.asmOut.append( stepOut );
      }
      forNextEntry.setEndValue( endValue );
      forNextEntry.setLoopLabel( label );
    } else {
      this.asmOut.append( varAddrOut );
      if( !varAddrExpr.equals( "HL" ) ) {
	putCode( "\tLD\tHL," );
	putCode( varAddrExpr );
	putCode( '\n' );
      }
      putCode( "\tPUSH\tHL\n" );
      this.asmOut.append( boundsOut );
      if( endValue != null ) {
	putCode_LD_HL_nn( endValue.intValue() );
	putCode( "\tPUSH\tHL\n" );
      }
      if( stepOut != null ) {
	this.asmOut.append( stepOut );
      }
      if( stepValue != null ) {
	putCode_LD_HL_nn( stepValue.intValue() );
	putCode( "\tPUSH\tHL\n" );
      }
      putCode( "\tLD\tHL," );
      putCode( label );
      putCode( "\n" );
      putCode( "\tPUSH\tHL\n" );
    }
    if( this.basicOptions.getCheckStack() ) {
      putCode_LD_A_n( MAGIC_FOR );
      putCode( "\tPUSH\tAF\n" );
    }
    putCode( label );
    putCode( ':' );
    if( this.basicOptions.isBreakAlwaysPossible() ) {
      putCode( "\tCALL\tCKBRK\n" );
      this.libItems.add( LibItem.CKBRK );
    }
    putCode( '\n' );
  }


  private void parseGOSUB( CharacterIterator iter ) throws PrgException
  {
    if( this.basicOptions.isBreakAlwaysPossible() ) {
      putCode( "\tCALL\tCKBRK\n" );
      this.libItems.add( LibItem.CKBRK );
    }
    if( this.basicOptions.getCheckStack() ) {
      putCode( "\tCALL\tCKSTCK\n" );
      this.libItems.add( LibItem.CKSTCK );
    }
    String label = createLabelForDestBasicLine( parseNumber( iter ) );
    if( this.basicOptions.getCheckStack() ) {
      String endOfInstLabel = nextLabel();
      putCode( "\tLD\tHL," );
      putCode( endOfInstLabel );
      putCode( "\n"
		+ "\tPUSH\tHL\n" );
      putCode_LD_A_n( MAGIC_GOSUB );
      putCode( "\tPUSH\tAF\n"
		+ "\tJP\t" );
      putCode( label );
      putCode( '\n' );
      putCode( endOfInstLabel );
      putCode( ":\n" );
    } else {
      putCode( "\tCALL\t" );
      putCode( label );
      putCode( '\n' );
    }
  }


  private void parseGOTO( CharacterIterator iter ) throws PrgException
  {
    if( this.basicOptions.isBreakAlwaysPossible() ) {
      putCode( "\tCALL\tCKBRK\n" );
      this.libItems.add( LibItem.CKBRK );
    }
    String label = createLabelForDestBasicLine( parseNumber( iter ) );
    putCode( "\tJP\t" );
    putCode( label );
    putCode( '\n' );
    this.suppressExit = true;
  }


  private void parseIF( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    putCode( "\tLD\tA,H\n"
		+ "\tOR\tL\n" );

    /*
     * Wenn hinter der Bedingung nur noch ein GOTO folgt
     * und die Tastatur auch nicht auf Programmabbruch abgefragt werden muss,
     * kann der Programmcode optimiert werden.
     */
    boolean orgBasicMismatch = false;
    String  keywordText      = null;
    Integer gotoLineNum      = null;
    int     parsePos         = iter.getIndex();
    if( !this.basicOptions.isBreakAlwaysPossible() ) {
      if( checkKeyword( iter, "THEN", false, false ) ) {
	keywordText = "THEN";
	orgBasicMismatch = true;
	gotoLineNum      = readNumber( iter );
	if( gotoLineNum == null ) {
	  if( checkKeyword( iter, "GOTO", false, false ) ) {
	    keywordText = "THEN GOTO";
	    gotoLineNum = readNumber( iter );
	  }
	}
      }
      else if( checkKeyword( iter, "GOTO", false, false ) ) {
	keywordText = "GOTO";
	gotoLineNum = readNumber( iter );
      }
      if( gotoLineNum != null ) {
	if( skipSpaces( iter ) != CharacterIterator.DONE )
	  gotoLineNum = null;
      }
    }
    if( gotoLineNum != null ) {
      if( this.sourceFormatter != null ) {
	if( keywordText != null ) {
	  this.sourceFormatter.replaceByKeyword( parsePos, keywordText, true );
	}
	this.sourceFormatter.append( (char) '\u0020' );
	this.sourceFormatter.append( gotoLineNum.toString() );
      }
      if( orgBasicMismatch ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
      }
      putCode( "\tJP\tNZ," );
      putCode( createLabelForDestBasicLine( gotoLineNum.intValue() ) );
      putCode( '\n' );
    } else {
      iter.setIndex( parsePos );
      String elseLabel = nextLabel();
      if( this.elseLabels == null ) {
	this.elseLabels = new Stack<String>();
      }
      this.elseLabels.push( elseLabel );
      if( this.basicOptions.getPreferRelativeJumps() ) {
	putCode( "\tJR\tZ," );
      } else {
	putCode( "\tJP\tZ," );
      }
      putCode( elseLabel );
      putCode( '\n' );
      if( checkInstruction( iter, "THEN" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	char ch = skipSpaces( iter );
	if( (ch >= '0') && (ch <= '9') ) {
	  parseGOTO( iter );
	} else {
	  parseInstructions( iter );
	}
      } else {
	parseInstructions( iter );
      }
      this.suppressExit = false;
    }
  }


  private void parseINPUT( CharacterIterator iter ) throws PrgException
  {
    boolean loop = true;
    while( loop ) {
      String retryLabel = nextLabel();
      putCode( retryLabel );
      putCode( ":\n" );

      boolean hasInfo = checkPrintStringLiteral( iter );

      skipSpaces( iter );
      int    begPos      = iter.getIndex();
      String varAddrExpr = checkVarGetAddrExpr( iter );
      if( varAddrExpr != null ) {
	if( varAddrExpr.equals( "HL" ) ) {
	  putCode( "\tPUSH\tHL\n" );
	}
	if( !hasInfo ) {
	  int curPos = iter.getIndex();
	  iter.setIndex( begPos );
	  char ch = iter.current();
	  while( (ch != CharacterIterator.DONE)
		 && (iter.getIndex() < curPos) )
	  {
	    putCharToPrintString( Character.toUpperCase( ch ) );
	    ch = iter.next();
	  }
	}
	putCharToPrintString( ':' );
      }
      if( varAddrExpr != null ) {
	putCode( "\tLD\tHL,M_IBUF\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tE,M_EBUF-M_IBUF\n"
		+ "\tCALL\tINLN\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tR_INT\n" );
	this.libItems.add( LibItem.INLN );
	this.libItems.add( LibItem.R_INT );
	if( varAddrExpr.equals( "HL" ) ) {
	  putCode( "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tC," );
	  putCode( retryLabel );
	  putCode( "\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
	} else {
	  putCode( "\tJR\tC," );
	  putCode( retryLabel );
	  putCode( "\n"
		+ "\tLD\t(" );
	  putCode( varAddrExpr );
	  putCode( "),HL\n" );
	}
      }
      if( (hasInfo || (varAddrExpr != null))
	  && skipSpaces( iter ) == ',' )
      {
	iter.next();
      } else {
	loop = false;
      }
    }
  }


  private void parseInString( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    putCode( "\tLD\tE,3FH\n"
		+ "\tCALL\tINLN\n"
		+ "\tLD\tA,D\n"
		+ "\tLD\t(M_LEN),A\n" );
    this.libItems.add( LibItem.INLN );
    this.libItems.add( LibItem.M_LEN );

  }


  private void parseLET( CharacterIterator iter ) throws PrgException
  {
    String varAddrExpr = checkVarGetAddrExpr( iter );
    if( varAddrExpr != null ) {
      parseAssignment( iter, varAddrExpr );
    } else {
      throwVarExpected();
    }
  }


  private void parseNEXT( CharacterIterator iter ) throws PrgException
  {
    /*
     * Zuerst die Adresse der Laufvariablen ermitteln
     * und den dazugehoerigen Programmcode temporaer merken
     */
    StringBuilder asmOut = this.asmOut;
    StringBuilder asmTmp = new StringBuilder( 32 );
    this.asmOut          = asmTmp;
    String varAddrExpr = checkVarGetAddrExpr( iter );
    if( varAddrExpr == null ) {
      ac1BasicMismatch();
      z1013BasicMismatch();
    }
    this.asmOut = asmOut;

    /*
     * Je nach Optionen und ob die Adresse der Laufvariablen konstant ist,
     * Stack pruefen und optimierten Programmcode erzeugen
     */
    String       orgVarAddrExpr = null;
    ForNextEntry forNextEntry   = null;
    if( this.forNextEntries != null ) {
      if( this.forNextEntries.isEmpty() ) {
	throw new PrgException( "Bei Strukturierter Programmierung"
		+ " unerwartetes NEXT\n"
		+ "Deaktivieren Sie bitte die Option"
		+ " \'FOR/NEXT als strukturierte Schleifen \u00FCbersetzen"
		+ " und optimieren\'" );
      }
      forNextEntry   = this.forNextEntries.pop();
      orgVarAddrExpr = forNextEntry.getCounterVariableAddrExpr();
    }
    if( orgVarAddrExpr != null ) {
      if( varAddrExpr != null ) {
	if( !varAddrExpr.equals( orgVarAddrExpr ) ) {
	  throw new PrgException( "Unterschiedliche Laufvariablen"
		+ " in FOR/NEXT-Schleife\n"
		+ "Deaktivieren Sie bitte die Option"
		+ " \'FOR/NEXT als strukturierte Schleifen \u00FCbersetzen"
		+ " und optimieren\'" );
	}
      }
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPOP\tAF\n"
		+ "\tCP\t" );
	putCode( getHex2( MAGIC_FOR ) );
	putCode( "\n"
		+ "\tJP\tNZ,E_NXWF\n" );
	this.libItems.add( LibItem.E_NXWF );
      }
      putCode( "\tLD\tHL,(" );
      putCode( orgVarAddrExpr );
      putCode( ")\n" );
      int stepValue = forNextEntry.getStepValue();
      if( stepValue == -1 ) {
	putCode( "\tDEC\tHL\n" );
      } else if( stepValue == 1 ) {
	putCode( "\tINC\tHL\n" );
      } else {
	putCode_LD_DE_nn( stepValue );
	putCode( "\tOR\tA\n"
		+ "\tADC\tHL,DE\n"
		+ "\tJP\tPE,E_ARIT\n" );
	this.libItems.add( LibItem.E_ARIT );
      }
      putCode( "\tLD\t(" );
      putCode( orgVarAddrExpr );
      putCode( "),HL\n" );
      Integer endValue = forNextEntry.getEndValue();
      if( endValue != null ) {
	putCode_LD_DE_nn( endValue );
      } else {
	putCode( "\tPOP\tDE\n" );
      }
      putCode( "\tCALL\tCPHLDE\n" );
      this.libItems.add( LibItem.H_CP );
      if( stepValue < 0 ) {
        if( this.basicOptions.getCheckStack() || (endValue == null) ) {
	  String label = nextLabel();
	  putCode( "\tJR\tC," );
	  putCode( label );
	  putCode( '\n' );
	  if( endValue == null ) {
	    putCode( "\tPUSH\tDE\n" );
	  }
	  if( this.basicOptions.getCheckStack() ) {
	    putCode_LD_A_n( MAGIC_FOR );
	    putCode( "\tPUSH\tAF\n" );
	  }
	  putCode( "\tJP\t" );
	  putCode( forNextEntry.getLoopLabel() );
	  putCode( '\n' );
	  putCode( label );
	  putCode( ":\n" );
	} else {
	  putCode( "\tJP\tNC," );
	  putCode( forNextEntry.getLoopLabel() );
	  putCode( '\n' );
	}
      } else {
        if( this.basicOptions.getCheckStack() || (endValue == null) ) {
	  String label = nextLabel();
	  putCode( "\tJR\tZ," );
	  putCode( label );
	  putCode( "\n"
		+ "\tJR\tC," );
	  putCode( label );
	  putCode( '\n' );
	  if( endValue == null ) {
	    putCode( "\tPUSH\tDE\n" );
	  }
	  if( this.basicOptions.getCheckStack() ) {
	    putCode( "\tPUSH\tAF\n" );
	  }
	  putCode( "\tJP\t" );
	  putCode( forNextEntry.getLoopLabel() );
	  putCode( '\n' );
	  putCode( label );
	  putCode( ":\n" );
	} else {
	  putCode( "\tJP\tZ," );
	  putCode( forNextEntry.getLoopLabel() );
	  putCode( "\n"
		+ "\tJP\tC," );
	  putCode( forNextEntry.getLoopLabel() );
	  putCode( '\n' );
	}
      }
    } else {
      if( this.basicOptions.getCheckStack() ) {
	if( this.asmOut != null ) {
	  this.asmOut.append( asmTmp );
	  if( varAddrExpr != null ) {
	    if( varAddrExpr.equals( "HL" ) ) {
	      putCode( "\tEX\tDE,HL\n" );
	    } else {
	      putCode( "\tLD\tDE," );
	      putCode( varAddrExpr );
	      putCode( '\n' );
	    }
	  } else {
	    putCode( "\tLD\tDE,0000H\n" );
	  }
	}
      }
      putCode( "\tCALL\tH_NEXT\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ," );
      this.libItems.add( LibItem.H_NEXT );
      String label = nextLabel();
      putCode( label );
      putCode( '\n' );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPOP\tAF\n" );
      }
      putCode( "\tPOP\tHL\n"
		+ "\tPUSH\tHL\n" );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPUSH\tAF\n" );
      }
      putCode( "\tJP\t(HL)\n" );
      putCode( label );
      putCode( ":\n" );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPOP\tAF\n" );
      }
      putCode( "\tPOP\tBC\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tBC\n" );
    }
  }


  private void parseOUT( CharacterIterator iter ) throws PrgException
  {
    if( skipSpaces( iter ) == '(' ) {
      iter.next();
      parseExpr( iter );
      parseToken( iter, ')' );
      if( skipSpaces( iter ) == ',' ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
      } else {
	parseToken( iter, '=' );
      }
    } else {
      ac1BasicMismatch();
      z1013BasicMismatch();
      parseExpr( iter );
      parseToken( iter, ',' );
    }
    putCode( "\tPUSH\tHL\n" );
    parseExpr( iter );
    putCode( "\tPOP\tBC\n"
		+ "\tOUT\t(C),L\n" );
  }


  private void parseOUTCHAR( CharacterIterator iter ) throws PrgException
  {
    Integer value = checkConstExpr( iter, 0, 0xFF );
    if( value != null ) {
      if( value.intValue() == 0x0D ) {
	putCode( "\tCALL\tOUTNL\n" );
	this.libItems.add( LibItem.OUTNL );
      } else {
	putCode( "\tLD\tA," );
	putCode( getHex2( value.intValue() ) );
	putCode( "\n"
		+ "\tCALL\tXOUTCH\n" );
	this.libItems.add( LibItem.XOUTCH );
      }
    } else {
      parseExpr( iter );
      putCode( "\tLD\tA,L\n"
		+ "\tCALL\tXOUTCH\n" );
      this.libItems.add( LibItem.XOUTCH );
    }
  }


  private void parseOutString( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    putCode( "\tCALL\tP_TEXT\n" );
    this.libItems.add( LibItem.P_TEXT );
  }


  private void parsePOKE( CharacterIterator iter ) throws PrgException
  {
    parseExpr( iter );
    parseToken( iter, ',' );
    Integer value = checkConstExpr( iter );
    if( value != null ) {
      putCode( "\tLD\t(HL)," );
      putCode( getHex2( value.intValue() ) );
      putCode( '\n' );
    } else {
      putCode( "\tPUSH\tHL\n" );
      parseExpr( iter );
      putCode( "\tLD\tA,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),A\n" );
    }
  }


  private void parsePRINT( CharacterIterator iter ) throws PrgException
  {
    boolean newLine   = true;
    boolean loop      = true;
    boolean widthDone = false;

    char ch = skipSpaces( iter );
    while( loop && (ch != CharacterIterator.DONE)
	   && (ch != ';') && (ch != ':') )
    {
      loop    = false;
      int pos = iter.getIndex();
      if( checkKeyword( iter, "ELSE", false, false ) ) {
	iter.setIndex( pos );
      } else {
	newLine = true;
	if( !checkPrintStringLiteral( iter ) ) {
	  ch = iter.current();
	  if( ch == '#' ) {
	    iter.next();
	    parseExpr( iter );
	    putCode( "\tLD\tA,L\n"
			+ "\tLD\t(M_WINT),A\n" );
	    this.libItems.add( LibItem.M_WINT );
	    widthDone = true;
	  } else {
	    if( !widthDone ) {
	      putCode( "\tLD\tA,6\n"
			+ "\tLD\t(M_WINT),A\n" );
	      this.libItems.add( LibItem.M_WINT );
	      widthDone = true;
	    }
	    parseExpr( iter );
	    putCode( "\tCALL\tP_INT\n" );
	    this.libItems.add( LibItem.P_INT );
	  }
	}
	ch = skipSpaces( iter );
	if( ch == ',' ) {
	  newLine = false;
	  loop    = true;
	  iter.next();
	  ch = skipSpaces( iter );
	}
      }
    }
    if( newLine ) {
      putCode( "\tCALL\tOUTNL\n" );
      this.libItems.add( LibItem.OUTNL );
    }
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
      String varAddrExpr = checkVarGetAddrExpr( iter );
      if( varAddrExpr != null ) {
	if( varAddrExpr.equals( "HL" ) ) {
	  putCode( "\tPUSH\tHL\n"
		+ "\tCALL\tDREAD\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
	} else {
	  putCode( "\tCALL\tDREAD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(" );
	  putCode( varAddrExpr );
	  putCode( "),HL\n" );
	}
	this.libItems.add( LibItem.DREAD );
      } else {
	throwVarExpected();
      }
    } while( skipSpaces( iter ) == ',' );
  }


  private void parseRESTORE( CharacterIterator iter ) throws PrgException
  {
    Integer value = readNumber( iter );
    if( value != null ) {
      if( this.restoreBasicLineNums == null ) {
	this.restoreBasicLineNums = new HashMap<Long,LineInfo>();
      }
      Long keyObj = new Long( value.longValue() );
      if( !this.restoreBasicLineNums.containsKey( keyObj ) ) {
	this.restoreBasicLineNums.put(
		keyObj,
		new LineInfo( getSourceLineNum(), this.curBasicLineNum ) );
      }
      if( value.intValue() >= 0 ) {
	putCode( "\tLD\tHL,D" );
	putCode( value.toString() );
	putCode( "\n"
		+ "\tLD\t(M_READ),HL\n" );
	this.libItems.add( LibItem.DATA );
      }
    } else {
      putCode( "\tCALL\tDINIT\n" );
      this.libItems.add( LibItem.DATA );
    }
  }


  private void parseRETURN()
  {
    if( this.basicOptions.getCheckStack() ) {
      putCode( "\tPOP\tAF\n"
		+ "\tCP\t" );
      putCode( getHex2( MAGIC_GOSUB ) );
      putCode( "\n"
		+ "\tJP\tNZ,E_REWG\n" );
      this.libItems.add( LibItem.E_REWG );
    }
    putCode( "\tRET\n" );
  }


  private void parseTAB( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = false;
    if( skipSpaces( iter ) == '(' ) {
      iter.next();
      enclosed = true;
    }
    parseExpr( iter );
    putCode( "\tCALL\tP_TAB\n" );
    this.libItems.add( LibItem.P_TAB );
    if( enclosed ) {
      parseToken( iter, ')' );
    } else {
      ac1BasicMismatch();
      z1013BasicMismatch();
    }
  }


  private void parseWORD( CharacterIterator iter ) throws PrgException
  {
    boolean enclosed = false;
    if( skipSpaces( iter ) == '(' ) {
      iter.next();
      enclosed = true;
    }
    Integer value = checkConstExpr( iter );
    if( value != null ) {
      putCode( "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'" );
      putCode( String.format( "%04X", value.intValue() & 0xFFFF ) );
      putCode( "\'\n"
		+ "\tDEFB\t0\n" );
      this.libItems.add( LibItem.P_LTXT );
    } else {
      parseExpr( iter );
      putCode( "\tLD\tA,H\n"
		+ "\tCALL\tP_HEXA\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tP_HEXA\n" );
      this.libItems.add( LibItem.P_HEXA );
    }
    if( enclosed ) {
      parseToken( iter, ')' );
    } else {
      ac1BasicMismatch();
      z1013BasicMismatch();
    }
  }


	/* --- Parsen der BASIC-Funktionen --- */

  private void parseABS( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    putCode( "\tCALL\tABSHL\n" );
    this.libItems.add( LibItem.ABS_NEG_HL );
  }


  private void parseDEEK( CharacterIterator iter ) throws PrgException
  {
    Integer addr = checkEnclosedConstExpr( iter );
    if( addr != null ) {
      putCode( "\tLD\tHL,(" );
      putCode( getHex4( addr.intValue() ) );
      putCode( ")\n" );
    } else {
      parseEnclosedExpr( iter );
      putCode( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n" );
    }
  }


  private void parseHEX( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    Integer value = readHex( iter );
    if( value == null ) {
      throw new PrgException( "Hexadezimalziffer erwartet" );
    }
    parseToken( iter, ')' );
    putCode_LD_HL_nn( value.intValue() );
  }


  private void parseIN( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    putCode( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tIN\tL,(C)\n"
		+ "\tLD\tH,0\n" );
  }


  private void parseINCHAR() throws PrgException
  {
    putCode( "\tCALL\tINCHR\n" );
    this.libItems.add( LibItem.INCHR );
  }


  private void parseINKEY() throws PrgException
  {
    if( this.basicOptions.isBreakOnInputPossible() ) {
      putCode( "\tCALL\tINKEY\n" );
      this.libItems.add( LibItem.INKEY );
    } else {
      putCode( "\tCALL\tXINKEY\n" );
      this.libItems.add( LibItem.XINKEY );
    }
    putCode( "\tLD\tL,A\n"
		+ "\tLD\tH,0\n" );
  }


  private void parseLEN() throws PrgException
  {
    putCode( "\tLD\tA,(M_LEN)\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,0\n" );
    this.libItems.add( LibItem.M_LEN );
  }


  private void parsePEEK( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    putCode( "\tLD\tL,(HL)\n"
		+ "\tLD\tH,0\n" );
  }


  private void parseRND( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    putCode( "\tCALL\tF_RND\n" );
    this.libItems.add( LibItem.F_RND );
  }


  private void parseSGN( CharacterIterator iter ) throws PrgException
  {
    parseEnclosedExpr( iter );
    putCode( "\tCALL\tF_SGN\n" );
    this.libItems.add( LibItem.F_SGN );
  }


  private void parseSIZE() throws PrgException
  {
    putCode( "\tCALL\tF_SIZE\n" );
    this.libItems.add( LibItem.F_SIZE );
  }


  private void parseSQR( CharacterIterator iter ) throws PrgException
  {
    Integer value = checkEnclosedConstExpr( iter );
    if( value != null ) {
      if( value < 0 ) {
	throw new PrgException(
			"Wurzel aus negativer Zahl nicht m\u00F6glich" );
      }
      putCode_LD_HL_nn( (int) Math.floor( Math.sqrt( value.doubleValue() ) ) );
    } else {
      parseEnclosedExpr( iter );
      putCode( "\tCALL\tF_SQR\n" );
      this.libItems.add( LibItem.F_SQR );
    }
  }


  private void parseTOP() throws PrgException
  {
    putCode( "\tLD\tHL,M_TOP\n" );
    this.libItems.add( LibItem.M_TOP );
  }


	/* --- Parsen arithmetischer Ausdruecke --- */

  private Integer checkConstExpr( CharacterIterator iter ) throws PrgException
  {
    int     pos   = iter.getIndex();
    Integer value = checkConstOrExpr( iter );
    if( value == null ) {
      iter.setIndex( pos );
    }
    return value;
  }


  private Integer checkConstExpr(
			CharacterIterator iter,
			int               minValue,
			int               maxValue ) throws PrgException
  {
    int     pos   = iter.getIndex();
    Integer value = checkConstOrExpr( iter );
    if( value != null ) {
      if( (value.intValue() < minValue) || (value.intValue() > maxValue) )
	putWarning( "Wert des Ausdrucks au\u00DFerhalb des Wertebereiches" );
    }
    if( value == null ) {
      iter.setIndex( pos );
    }
    return value;
  }


  private Integer checkEnclosedConstExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = null;
    char    ch    = skipSpaces( iter );
    int     pos   = iter.getIndex();
    if( ch == '(' ) {
      iter.next();
      Integer v = checkConstOrExpr( iter );
      if( v != null ) {
	if( skipSpaces( iter ) == ')' ) {
	  iter.next();
	  value = v;
	}
      }
    }
    if( value == null ) {
      iter.setIndex( pos );
    }
    return value;
  }


  private Integer checkConstOrExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = checkConstAndExpr( iter );
    while( (value != null) && (checkKeyword( iter, "OR" ) ) ) {
      Integer v2 = checkConstAndExpr( iter );
      if( v2 != null ) {
	if( (value.intValue() != 0) || (v2.intValue() != 0) ) {
	  value = new Integer( 1 );
	} else {
	  value = new Integer( 0 );
	}
      } else {
	value = null;
      }
    }
    return value;
  }


  private Integer checkConstAndExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = checkConstNotExpr( iter );
    while( (value != null) && (checkKeyword( iter, "AND" ) ) ) {
      Integer v2 = checkConstNotExpr( iter );
      if( v2 != null ) {
	if( (value.intValue() != 0) && (v2.intValue() != 0) ) {
	  value = new Integer( 1 );
	} else {
	  value = new Integer( 0 );
	}
      } else {
	value = null;
      }
    }
    return value;
  }


  private Integer checkConstNotExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = null;
    if( checkKeyword( iter, "NOT" ) ) {
      value = checkConstCondExpr( iter );
      if( value != null ) {
	value = new Integer( value.intValue() == 0 ? 1 : 0 );
      }
    } else {
      value = checkConstCondExpr( iter );
    }
    return value;
  }


  private Integer checkConstCondExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = checkConstArithmExpr( iter );
    if( value != null ) {
      char ch = skipSpaces( iter );
      if( ch == '<' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  Integer v2 = checkConstArithmExpr( iter );
	  if( v2 != null ) {
	    value = new Integer( value.intValue() <= v2.intValue() ? 1 : 0 );
	  } else {
	    value = null;
	  }
	}
	else if( ch == '>' ) {
	  iter.next();
	  z1013BasicMismatch();
	  Integer v2 = checkConstArithmExpr( iter );
	  if( v2 != null ) {
	    value = new Integer( value.intValue() != v2.intValue() ? 1 : 0 );
	  } else {
	    value = null;
	  }
	} else {
	  Integer v2 = checkConstArithmExpr( iter );
	  if( v2 != null ) {
	    value = new Integer( value.intValue() < v2.intValue() ? 1 : 0 );
	  } else {
	    value = null;
	  }
	}
      }
      else if( ch == '>' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  Integer v2 = checkConstArithmExpr( iter );
	  if( v2 != null ) {
	    value = new Integer( value.intValue() >= v2.intValue() ? 1 : 0 );
	  } else {
	    value = null;
	  }
	} else {
	  Integer v2 = checkConstArithmExpr( iter );
	  if( v2 != null ) {
	    value = new Integer( value.intValue() > v2.intValue() ? 1 : 0 );
	  } else {
	    value = null;
	  }
	}
      }
      else if( ch == '=' ) {
	iter.next();
	Integer v2 = checkConstArithmExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() == v2.intValue() ? 1 : 0 );
	} else {
	  value = null;
	}
      }
      else if( ch == '#' ) {
	iter.next();
	Integer v2 = checkConstArithmExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() != v2.intValue() ? 1 : 0 );
	} else {
	  value = null;
	}
      }
    }
    return value;
  }


  private Integer checkConstArithmExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = null;
    char    ch    = skipSpaces( iter );
    if( ch == '-' ) {
      iter.next();
      value = checkConstAddExpr( iter );
      if( value != null ) {
	value = new Integer( -value.intValue() );
      }
    } else {
      if( ch == '+' ) {
	ch = iter.next();
      }
      value = checkConstAddExpr( iter );
    }
    return value;
  }


  private Integer checkConstAddExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = checkConstMulExpr( iter );
    char ch = skipSpaces( iter );
    while( (value != null) && ((ch == '+') || (ch == '-')) ) {
      if( ch == '+' ) {
	iter.next();
	Integer v2 = checkConstMulExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() + v2.intValue() );
	} else {
	  value = null;
	}
      } else {
	iter.next();
	Integer v2 = checkConstMulExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() - v2.intValue() );
	} else {
	  value = null;
	}
      }
      ch = skipSpaces( iter );
    }
    return value;
  }


  private Integer checkConstMulExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = checkConstPrimExpr( iter );
    char ch = skipSpaces( iter );
    while( (value != null) && ((ch == '*') || (ch == '/')) ) {
      if( ch == '*' ) {
	iter.next();
	Integer v2 = checkConstPrimExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() * v2.intValue() );
	} else {
	  value = null;
	}
      } else {
	iter.next();
	Integer v2 = checkConstPrimExpr( iter );
	if( v2 != null ) {
	  value = new Integer( value.intValue() / v2.intValue() );
	} else {
	  value = null;
	}
      }
      ch = skipSpaces( iter );
    }
    return value;
  }


  private Integer checkConstPrimExpr( CharacterIterator iter )
							throws PrgException
  {
    Integer value = null;
    char    ch    = skipSpaces( iter );
    if( ch == '(' ) {
      value = checkEnclosedConstExpr( iter );
    }
    else if( (ch >= '0') && (ch <= '9') ) {
      value = readNumber( iter );
    }
    else if( ch == '\'' ) {
      ch = iter.next();
      if( (int) ch <= 0xFF ) {
	if( iter.next() == '\'' ) {
	  iter.next();
	  value = new Integer( (int) ch );
	}
      }
    } else {
      if( checkKeyword( iter, "ABS" ) ) {
	value = checkEnclosedConstExpr( iter );
	if( value != null ) {
	  if( value.intValue() < 0 ) {
	    value = new Integer( -value.intValue() );
	  }
	}
      }
      else if( checkKeyword( iter, "HEX" ) ) {
	if( skipSpaces( iter ) == '(' ) {
	  iter.next();
	  value = readHex( iter );
	  if( value != null ) {
	    if( skipSpaces( iter ) == ')' ) {
	      iter.next();
	    } else {
	      value = null;
	    }
	  }
	}
      }
      else if( checkKeyword( iter, "SGN" ) ) {
	z1013BasicMismatch();
	value = checkEnclosedConstExpr( iter );
	if( value != null ) {
	  if( value.intValue() < 0 ) {
	    value = new Integer( -1 );
	  }
	  else if( value.intValue() > 0 ) {
	    value = new Integer( 1 );
	  } else {
	    value = new Integer( 0 );
	  }
	}
      }
      else if( checkKeyword( iter, "SQR" ) ) {
	z1013BasicMismatch();
	value = checkEnclosedConstExpr( iter );
	if( value != null ) {
	  if( value.intValue() >= 0 ) {
	    value = new Integer( (int) Math.floor(
				Math.sqrt( value.doubleValue( )) ) );
	  }
	}
      }
    }
    return value;
  }


  private void parseEnclosedExpr( CharacterIterator iter ) throws PrgException
  {
    parseToken( iter, '(' );
    parseExpr( iter );
    parseToken( iter, ')' );
  }


  private void parseExpr( CharacterIterator iter ) throws PrgException
  {
    parseAndExpr( iter );
    while( checkKeyword( iter, "OR" ) ) {
      putCode( "\tPUSH\tHL\n" );
      parseAndExpr( iter );
      putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_OR\n" );
      this.libItems.add( LibItem.O_OR );
    }
  }


  private void parseAndExpr( CharacterIterator iter ) throws PrgException
  {
    parseNotExpr( iter );
    while( checkKeyword( iter, "AND" ) ) {
      putCode( "\tPUSH\tHL\n" );
      parseNotExpr( iter );
      putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_AND\n" );
      this.libItems.add( LibItem.O_AND );
    }
  }


  private void parseNotExpr( CharacterIterator iter ) throws PrgException
  {
    if( checkKeyword( iter, "NOT" ) ) {
      parseCondExpr( iter );
      putCode( "\tCALL\tO_NOT\n" );
      this.libItems.add( LibItem.O_NOT );
    } else {
      parseCondExpr( iter );
    }
  }


  private void parseCondExpr( CharacterIterator iter ) throws PrgException
  {
    parseArithmExpr( iter );
    char ch = skipSpaces( iter );
    if( ch == '<' ) {
      ch = iter.next();
      if( ch == '=' ) {
	iter.next();
	Integer value = checkConstArithmExpr( iter );
	if( value != null ) {
	  putCode_LD_DE_nn( value.intValue() );
	  putCode( "\tCALL\tO_LE\n" );
	  this.libItems.add( LibItem.O_LE );
	} else {
	  putCode( "\tPUSH\tHL\n" );
	  parseArithmExpr( iter );
	  putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_GE\n" );
	  this.libItems.add( LibItem.O_GE );
	}
      }
      else if( ch == '>' ) {
	iter.next();
	z1013BasicMismatch();
	Integer value = checkConstArithmExpr( iter );
	if( value != null ) {
	  putCode_LD_DE_nn( value.intValue() );
	} else {
	  putCode( "\tPUSH\tHL\n" );
	  parseArithmExpr( iter );
	  putCode( "\tPOP\tDE\n" );
	}
	putCode( "\tCALL\tO_NE\n" );
	this.libItems.add( LibItem.O_NE );
      } else {
	Integer value = checkConstArithmExpr( iter );
	if( value != null ) {
	  putCode_LD_DE_nn( value.intValue() );
	  putCode( "\tCALL\tO_LT\n" );
	  this.libItems.add( LibItem.O_LT );
	} else {
	  putCode( "\tPUSH\tHL\n" );
	  parseArithmExpr( iter );
	  putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_GT\n" );
	  this.libItems.add( LibItem.O_GT );
	}
      }
    }
    else if( ch == '>' ) {
      ch = iter.next();
      if( ch == '=' ) {
	iter.next();
	Integer value = checkConstArithmExpr( iter );
	if( value != null ) {
	  putCode_LD_DE_nn( value.intValue() );
	  putCode( "\tCALL\tO_GE\n" );
	  this.libItems.add( LibItem.O_GE );
	} else {
	  putCode( "\tPUSH\tHL\n" );
	  parseArithmExpr( iter );
	  putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_LE\n" );
	  this.libItems.add( LibItem.O_LE );
	}
      } else {
	Integer value = checkConstArithmExpr( iter );
	if( value != null ) {
	  putCode_LD_DE_nn( value.intValue() );
	  putCode( "\tCALL\tO_GT\n" );
	  this.libItems.add( LibItem.O_GT );
	} else {
	  putCode( "\tPUSH\tHL\n" );
	  parseArithmExpr( iter );
	  putCode( "\tPOP\tDE\n"
		+ "\tCALL\tO_LT\n" );
	  this.libItems.add( LibItem.O_LT );
	}
      }
    }
    else if( ch == '=' ) {
      iter.next();
      Integer value = checkConstArithmExpr( iter );
      if( value != null ) {
	putCode_LD_DE_nn( value.intValue() );
      } else {
	putCode( "\tPUSH\tHL\n" );
	parseArithmExpr( iter );
	putCode( "\tPOP\tDE\n" );
      }
      putCode( "\tCALL\tO_EQ\n" );
      this.libItems.add( LibItem.O_EQ );
    }
    else if( ch == '#' ) {
      iter.next();
      Integer value = checkConstArithmExpr( iter );
      if( value != null ) {
	putCode_LD_DE_nn( value.intValue() );
      } else {
	putCode( "\tPUSH\tHL\n" );
	parseArithmExpr( iter );
	putCode( "\tPOP\tDE\n" );
      }
      putCode( "\tCALL\tO_NE\n" );
      this.libItems.add( LibItem.O_NE );
    }
  }


  private void parseArithmExpr( CharacterIterator iter ) throws PrgException
  {
    char ch = skipSpaces( iter );
    if( ch == '-' ) {
      iter.next();
      parseAddExpr( iter );
      putCode( "\tCALL\tNEGHL\n" );
      this.libItems.add( LibItem.ABS_NEG_HL );
    } else {
      if( ch == '+' ) {
	ch = iter.next();
      }
      parseAddExpr( iter );
    }
  }


  private void parseAddExpr( CharacterIterator iter ) throws PrgException
  {
    parseMulExpr( iter );
    char ch = skipSpaces( iter );
    while( (ch == '+') || (ch == '-') ) {
      iter.next();
      Integer value = checkConstMulExpr( iter );
      if( value != null ) {
	putCode_LD_DE_nn( value.intValue() );
	if( ch == '+' ) {
	  putCode( "\tCALL\tO_ADD\n" );
	  this.libItems.add( LibItem.O_ADD );
	} else {
	  putCode( "\tCALL\tO_SUB\n" );
	  this.libItems.add( LibItem.O_SUB );
	}
      } else {
	putCode( "\tPUSH\tHL\n" );
	parseMulExpr( iter );
	putCode( "\tPOP\tDE\n" );
	if( ch == '+' ) {
	  putCode( "\tCALL\tO_ADD\n" );
	  this.libItems.add( LibItem.O_ADD );
	} else {
	  putCode( "\tEX\tDE,HL\n"
		+ "\tCALL\tO_SUB\n" );
	  this.libItems.add( LibItem.O_SUB );
	}
      }
      ch = skipSpaces( iter );
    }
  }


  private void parseMulExpr( CharacterIterator iter ) throws PrgException
  {
    parsePrimExpr( iter );
    char ch = skipSpaces( iter );
    while( (ch == '*') || (ch == '/') ) {
      iter.next();
      Integer value = checkConstPrimExpr( iter );
      if( value != null ) {
	putCode_LD_DE_nn( value.intValue() );
	if( ch == '*' ) {
	  putCode( "\tCALL\tO_MUL\n" );
	  this.libItems.add( LibItem.O_MUL );
	} else {
	  putCode( "\tCALL\tO_DIV\n" );
	  this.libItems.add( LibItem.O_DIV );
	}
      } else {
	putCode( "\tPUSH\tHL\n" );
	parsePrimExpr( iter );
	putCode( "\tPOP\tDE\n" );
	if( ch == '*' ) {
	  putCode( "\tCALL\tO_MUL\n" );
	  this.libItems.add( LibItem.O_MUL );
	} else {
	  putCode( "\tEX\tDE,HL\n"
		+ "\tCALL\tO_DIV\n" );
	  this.libItems.add( LibItem.O_DIV );
	}
      }
      ch = skipSpaces( iter );
    }
  }


  private void parsePrimExpr( CharacterIterator iter ) throws PrgException
  {
    char ch = skipSpaces( iter );
    if( ch == '(' ) {
      Integer value = checkEnclosedConstExpr( iter );
      if( value != null ) {
	putCode_LD_HL_nn( value.intValue() );
      } else {
	iter.next();
	parseExpr( iter );
	parseToken( iter, ')' );
      }
    }
    else if( (ch >= '0') && (ch <= '9') ) {
      putCode_LD_HL_nn( parseNumber( iter ) );
    }
    else if( ch == '\'' ) {
      ch = iter.next();
      iter.next();
      parseToken( iter, '\'' );
      check8BitChar( ch );
      putCode_LD_HL_nn( ch );
    }
    else if( ch == '@' ) {
      iter.next();
      Integer value = checkEnclosedConstExpr( iter );
      if( value != null ) {
	putCode( "\tLD\tHL,(" );
	putCode( getArrayVarAddrExpr( value.intValue() ) );
	putCode( ")\n" );
      } else {
	parseEnclosedExpr( iter );
	putCode( "\tCALL\tARYADR\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tEX\tDE,HL\n" );
	this.libItems.add( LibItem.ARYADR );
      }
    } else {
      if( checkKeyword( iter, "ABS" ) ) {
	parseABS( iter );
      }
      else if( checkKeyword( iter, "DEEK" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseDEEK( iter );
      }
      else if( checkKeyword( iter, "HEX" ) ) {
	parseHEX( iter );
      }
      else if( checkKeyword( iter, "INCHAR" ) ) {
	parseINCHAR();
      }
      else if( checkKeyword( iter, "INKEY" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseINKEY();
      }
      else if( checkKeyword( iter, "INP" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseIN( iter );
      }
      else if( checkKeyword( iter, "IN" ) ) {
	parseIN( iter );
      }
      else if( checkKeyword( iter, "LEN" ) ) {
	ac1BasicMismatch();
	parseLEN();
      }
      else if( checkKeyword( iter, "PEEK" ) ) {
	parsePEEK( iter );
      }
      else if( checkKeyword( iter, "RND" ) ) {
	parseRND( iter );
      }
      else if( checkKeyword( iter, "SIZE" ) ) {
	parseSIZE();
      }
      else if( checkKeyword( iter, "SGN" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseSGN( iter );
      }
      else if( checkKeyword( iter, "SQR" ) ) {
	ac1BasicMismatch();
	z1013BasicMismatch();
	parseSQR( iter );
      }
      else if( checkKeyword( iter, "TOP" ) ) {
	parseTOP();
      } else {
	String varName = readSimpleVarName( iter );
	int    len     = varName.length();
	if( len > 0 ) {
	  if( len > 1 ) {
	    z1013BasicMismatch();
	  }
	  String varLabel = "M_V" + varName;
	  this.varLabels.add( varLabel );
	  putCode( "\tLD\tHL,(" );
	  putCode( varLabel );
	  putCode( ")\n" );
	} else {
	  throwUnexpectedChar( skipSpaces( iter ) );
	}
      }
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


  private Integer readHex( CharacterIterator iter ) throws PrgException
  {
    Integer rv = null;
    char    ch = skipSpaces( iter );
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


  private String readSimpleVarName( CharacterIterator iter )
  {
    StringBuilder buf = new StringBuilder();
    char          ch  = skipSpaces( iter );
    if( ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      buf.append( Character.toUpperCase( ch ) );
      ch = iter.next();
      if( this.basicOptions.getAllowLongVarNames() ) {
	while( ((ch >= 'A') && (ch <= 'Z'))
	       || ((ch >= 'a') && (ch <= 'z'))
	       || ((ch >= '0') && (ch <= '9')) )
	{
	  buf.append( Character.toUpperCase( ch ) );
	  ch = iter.next();
	}
      }
    }
    return buf.toString();
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
			String            varAddrExpr ) throws PrgException
  {
    parseToken( iter, '=' );
    if( varAddrExpr.equals( "HL" ) ) {
      Integer value = checkConstExpr( iter );
      if( value != null ) {
	putCode( "\tLD\t(HL)," );
	putCode( getHex2( value.intValue() ) );
	putCode( "\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL)," );
	putCode( getHex2( value.intValue() >> 8 ) );
	putCode( '\n' );
      } else {
	putCode( "\tPUSH\tHL\n" );
	parseExpr( iter );
	putCode( "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
      }
    } else {
      parseExpr( iter );
      putCode( "\tLD\t(" );
      putCode( varAddrExpr );
      putCode( "),HL\n" );
    }
  }


	/* --- Bibliothek mit den benoetigten Funktionalitaeten --- */

  private void putLibrary()
  {
    int stackSize = this.basicOptions.getStackSize();
    if( this.basicOptions.getShowAsm() ) {
      putCode( "\n;Bibliotheksfunktionen\n" );
    }
    if( this.libItems.contains( LibItem.INLN ) ) {
      /*
       * Eingabe einer Zeile, die mit ENTER abgeschlossen wird
       * Paramter:
       *   HL: Anfangsadresse des Puffers
       *   E:  max. Anzahl einzugebender Zeichen (Pufferlaenge - 1)
       */
      putCode( "INLN:\tPUSH\tHL\n"
		+ "\tLD\tB,E\n"
		+ "INLN1:\tLD\t(HL),20H\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tINLN1\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tD,0\n"
		+ "\tJR\tINLN3\n"
		+ "INLN2:\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,B\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "INLN3:\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,A\n" );
      this.libItems.add( LibItem.XINCH );
      this.libItems.add( LibItem.XOUTCH );
      if( this.basicOptions.isBreakOnInputPossible() ) {
	putCode( "\tCP\t3\n"
		+ "\tJP\tZ,E_BRK\n" );
	this.libItems.add( LibItem.E_BRK );
      }
      putCode( "\tCP\t8\n"
		+ "\tJR\tZ,INLN5\n"
		+ "\tCP\t9,\n"
		+ "\tJR\tZ,INLN6\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,INLN7\n"
		+ "\tCP\t0\n"
		+ "\tJR\tZ,INLN3\n"
		+ "INLN4:\tLD\tA,D\n"
		+ "\tCP\tE\n"
		+ "\tJR\tNC,INLN3\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tD\n"
		+ "\tJR\tINLN2\n"
		+ "INLN5:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,INLN3\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tD\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,B\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tINLN2\n"
		+ "INLN6:\tLD\tA,D\n"
		+ "\tCP\tE\n"
		+ "\tJR\tNC,INLN3\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tD\n"
		+ "\tJR\tINLN2\n"
		+ "INLN7:\tLD\t(HL),0\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.P_INT ) ) {
      /*
       * Ausgabe einer Integer-Zahl auf dem Bildschirm
       * Parameter:
       *   HL:       auszugebende Zahl
       *   (M_WINT): Feldbreite
       */
      putCode( "P_INT:\tLD\tA,(M_WINT)\n"
		+ "\tLD\tB,0\n"
		+ "\tLD\tC,A\n"
		+ "\tCALL\tABSHL\n"
		+ "\tJP\tP,P_INT1\n"
		+ "\tLD\tB,'-'\n"
		+ "\tDEC\tC\n"
		+ "P_INT1:\tPUSH DE\n"
		+ "\tLD\tDE,000AH\n"
		+ "\tPUSH\tDE\n"
		+ "\tDEC\tC\n"
		+ "\tPUSH\tBC\n"
		+ "P_INT2:\tCALL\tH_DIV\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,P_INT3\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tDEC\tL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tJR\tP_INT2\n"
		+ "P_INT3:\tPOP\tBC\n"
		+ "P_INT4:\tDEC\tC\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,P_INT5\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tJR\tP_INT4\n"
		+ "P_INT5:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tNZ,XOUTCH\n"
		+ "\tLD\tE,L\n"
		+ "P_INT6:\tLD\tA,E\n"
		+ "\tCP\t0AH\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tZ\n"
		+ "\tADD\tA,'0'\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tJR\tP_INT6\n" );
      this.libItems.add( LibItem.M_WINT );
      this.libItems.add( LibItem.ABS_NEG_HL );
      this.libItems.add( LibItem.H_DIV );
      this.libItems.add( LibItem.XOUTCH );
    }
    if( this.libItems.contains( LibItem.R_INT ) ) {
      /*
       * Lesen und einer Integer-Zahl aus dem Eingabepuffer
       * Rueckgabe:
       *   HL:     gelesene Zahl
       *   M_IBUF: Anfangsadresse des Eingabepuffers
       */
      putCode( "R_INT:\tLD\tBC,M_IBUF-1\n"
		+ "R_INT1:\tINC\tBC\n"
		+ "\tLD\tA,(BC)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,R_INT1\n"
		+ "\tCP\t2DH\n"
		+ "\tJR\tNZ,R_INT2\n"
		+ "\tINC\tBC\n"
		+ "\tCALL\tR_INT2\n"
		+ "\tRET\tC\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
		+ "R_INT2:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,R_INT5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,R_INT5\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,0\n"
		+ "R_INT3:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tCP\t0\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,R_INT4\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,R_INT5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,R_INT5\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,0\n"
		+ "\tADC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tJP\tP,R_INT3\n"
		+ "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'SORRY!\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "R_INT4:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tCP\t0\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,R_INT4\n"
		+ "R_INT5:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'WHAT?\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.CKSTCK ) ) {
      /*
       * Pruefen, ob der Stack ueberlaeuft
       */
      putCode( "CKSTCK:\tLD\tHL," );
      if( stackSize > 0 ) {
	putCode( "M_TOP-" );
	putCode( getHex4( stackSize ) );
	putCode( "+10H" );
	this.libItems.add( LibItem.M_TOP );
      } else {
	putCode( "0070H" );
      }
      putCode( "\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,SP\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'STACK OVERFLOW\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.F_RND ) ) {
      /*
       * Ermitteln einer Zufallszahl
       * Parameter:
       *   HL: maximaler Wert (groesser 0)
       * Rueckgabe:
       *   HL: Zufallszahl zwischen 1 und maximaler Wert
       */
      putCode( "F_RND:\tPUSH\tHL\n"
		+ "\tLD\tHL,M_RND\n"
		+ "\tLD\tA,R\n"
		+ "\tRLD\n"
		+ "\tINC\tHL\n"
		+ "\tRLD\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_ARG\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_ARG\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tBC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,(M_RND)\n"
		+ "F_RND1:\tSBC\tHL,DE\n"
		+ "\tJR\tNC,F_RND1\n"
		+ "\tADD\tHL,DE\n"
		+ "\tINC\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.E_ARG );
      this.libItems.add( LibItem.M_RND );
    }
    if( this.libItems.contains( LibItem.F_SGN ) ) {
      /*
       * Ermitteln des Vorzeichens
       * Parameter:
       *   HL: zu pruefender Wert
       * Rueckgabe:
       *   HL: -1, 0 oder +1
       */
      putCode( "F_SGN:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tLD\tHL,0001H\n"
		+ "\tRET\tP\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.F_SIZE ) ) {
      /*
       * Ermitteln der Groesse des hinter dem Programms
       * folgenden Speicherbereichs
       * Rueckgabe:
       *   HL: Groesse des Speicherbereichs, max. jedoch 32767
       */
      putCode( "F_SIZE:" );
      putCode_LD_HL_nn( this.basicOptions.getEndOfMemory() );
      putCode( "\tLD\tDE,M_TOP\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tC,F_SIZ1\n"
		+ "\tBIT\t7,H\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,7FFFH\n"
		+ "\tRET\n"
		+ "F_SIZ1:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.M_TOP );
    }
    if( this.libItems.contains( LibItem.F_SQR ) ) {
      /*
       * Ermitteln des ganzzahligen Anteils einer Quadratwurzel
       * Parameter:
       *   HL: Wert, aus der die Quadratwurzel gezogen wird
       * Rueckgabe:
       *   HL: Quadratwurzel (nur ganzzahliger Anteil)
       */
      putCode( "F_SQR:\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_ARG\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tB,1\n"
		+ "\tLD\tDE,0001H\n"
		+ "F_SQR1:\tLD\tA,L\n"
		+ "\tSUB\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tSBC\tA,D\n"
		+ "\tLD\tH,A\n"
		+ "\tJR\tC,F_SQR2\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,F_SQR3\n"
		+ "\tLD\tA,E\n"
		+ "\tADD\tA,2\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,D\n"
		+ "\tADC\tA,0\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,B\n"
		+ "\tINC\tA\n"
		+ "\tLD\tB,A\n"
		+ "\tJR\tF_SQR1\n"
		+ "F_SQR2:\tLD\tA,B\n"
		+ "\tDEC\tA\n"
		+ "\tLD\tB,A\n"
		+ "F_SQR3:\tLD\tL,B\n"
		+ "\tLD\tH,0\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.E_ARG );
    }
    if( this.libItems.contains( LibItem.H_NEXT ) ) {
      /*
       * Ende einer FOR-Schleife
       * Die Funktion liest vom Stack die Werte,
       * die bei der FOR-Anweisung auf den Stack geschrieben wurden,
       * und prueft, ob zum Schleifenanfang gesprungen wird.
       * Wenn nein, wir der Stack bereinigt.
       *
       * Parameter:
       *   Stack:
       *     Adresse der Schleifenvariable
       *     Endwert
       *     Schrittweite
       *     Adresse des Schleifenanfangs
       *     Kennung 'F' fuer FOR-Schleife (nur bei aktiver Stack-Pruefung)
       *   DE:
       *     Adresse der bei der NEXT-Anweisung angegebenen Variable
       *     oder 0, wenn dort keine Variable angegeben wurde
       *     (nur zu Pruefzwecken).
       */
      putCode( "H_NEXT:\tPOP\tBC\n" );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPOP\tAF\n"
		+ "\tCP\t" );
	putCode( getHex2( MAGIC_FOR ) );
	putCode( "\n"
		+ "\tJP\tNZ,E_NXWF\n" );
	this.libItems.add( LibItem.E_NXWF );
      }
      putCode( "\tLD\tIY,0\n"
		+ "\tADD\tIY,SP\n" );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tPUSH\tAF\n" );
      }
      putCode( "\tPUSH\tBC\n"
		+ "\tLD\tL,(IY+6)\n"
		+ "\tLD\tH,(IY+7)\n" );
      if( this.basicOptions.getCheckStack() ) {
	putCode( "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,H_NXT2\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\tD\n"
		+ "\tJR\tNZ,H_NXT1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tJR\tZ,H_NXT2\n"
		+ "H_NXT1:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'NEXT WITH WRONG VARIABLE\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
	putExit();
	putCode( "H_NXT2:" );
	this.libItems.add( LibItem.P_LTXT );
      }
      putCode( "\tPUSH\tHL\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tLD\tL,(IY+2)\n"
		+ "\tLD\tH,(IY+3)\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tLD\tL,(IY+4)\n"
		+ "\tLD\tH,(IY+5)\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tJP\tM,O_GT\n"
		+ "\tJR\tO_LT\n" );
      this.libItems.add( LibItem.O_ADD );
      this.libItems.add( LibItem.O_LT );
      this.libItems.add( LibItem.O_GT );
    }
    if( this.libItems.contains( LibItem.O_OR ) ) {
      /*
       * Logisches ODER
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_OR:\tLD\tA,L\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,O_OR1\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tD\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "O_OR1:\tLD\tHL,0001H\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.O_AND ) ) {
      /*
       * Logisches UND
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_AND:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tL\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.O_NOT ) ) {
      /*
       * Logisches NICHT
       * Parameter:
       *   HL: Operand
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_NOT:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tL\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.O_LT ) ) {
      /*
       * Vergleich HL < DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_LT:\tCALL\tH_CP\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.O_LE ) ) {
      /*
       * Vergleich HL <= DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_LE:\tCALL\tH_CP\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,H\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.O_GT ) ) {
      /*
       * Vergleich HL > DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_GT:\tCALL\tH_CP\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.O_GE ) ) {
      /*
       * Vergleich HL >= DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_GE:\tCALL\tH_CP\n"
		+ "\tRET\tC\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.O_EQ ) ) {
      /*
       * Vergleich HL == DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_EQ:\tCALL\tH_CP\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.O_NE ) ) {
      /*
       * Vergleich HL != DE ?
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: 0 oder 1
       */
      putCode( "O_NE:\tCALL\tH_CP\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_CP );
    }
    if( this.libItems.contains( LibItem.H_CP ) ) {
      /*
       * Vergleich von HL und DE
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   C-Flag: HL < DE
       *   Z-Flag: HL == DE
       *   A:  1  (nicht bei CPHLDE)
       *   HL: 0  (nicht bei CPHLDE)
       */
      putCode( "H_CP:\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tJP\tP,H_CP1\n"
		+ "\tEX\tDE,HL\n"
		+ "H_CP1:\tCALL\tH_CP2\n"
		+ "\tLD\tA,1\n"
		+ "\tLD\tHL,0000\n"
		+ "\tRET\n"
		+ "CPHLDE:\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tJP\tP,H_CP2\n"
		+ "\tEX\tDE,HL\n"
		+ "H_CP2:\tLD\tA,H\n"
		+ "\tCP\tD\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.O_SUB ) ) {
      /*
       * Subtraktion: HL = HL - DE
       */
      putCode( "O_SUB:\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tNEGHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n" );	// kein RET -> weiter mit O_ADD
      this.libItems.add( LibItem.O_ADD );
      this.libItems.add( LibItem.ABS_NEG_HL );
    }
    if( this.libItems.contains( LibItem.O_ADD ) ) {
      /*
       * Addition: HL = HL + DE
       * O_ADD muss direkt hinter O_SUB folgen!
       */
      putCode( "O_ADD:\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tLD\tA,D\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\tM\n"
		+ "\tXOR\tH\n"
		+ "\tRET\tP\n"
		+ "\tJP\tE_ARIT\n" );
      this.libItems.add( LibItem.E_ARIT );
    }
    if( this.libItems.contains( LibItem.O_MUL ) ) {
      /*
       * Multiplikation: HL = HL * DE
       */
      putCode( "O_MUL:\tLD\tB,0\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,O_MUL1\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJP\tNZ,E_ARIT\n"
		+ "O_MUL1:\tLD\tA,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,O_MUL3\n"
		+ "O_MUL2:\tADD\tHL,DE\n"
		+ "\tJP\tC,E_ARIT\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,O_MUL2\n"
		+ "O_MUL3:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_ARIT\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tM,NEGHL\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.ABS_NEG_HL );
      this.libItems.add( LibItem.E_ARIT );
    }
    if( this.libItems.contains( LibItem.O_DIV ) ) {
      /*
       * Division: HL = HL / DE
       */
      putCode( "O_DIV:\tLD\tB,0\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,O_DIV1\n"
		+ "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'DIVISION BY ZERO\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
	putExit();
	putCode( "O_DIV1:\tPUSH\tBC\n"
		+ "\tCALL\tH_DIV\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_ARIT\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tM,NEGHL\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.H_DIV );
      this.libItems.add( LibItem.ABS_NEG_HL );
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.H_DIV ) ) {
      /*
       * Vorzeichenlose Division: HL = HL / DE
       */
      putCode( "H_DIV:\tPUSH\tHL\n"
		+ "\tLD\tL,H\n"
		+ "\tLD\tH,0\n"
		+ "\tCALL\tH_DIV1\n"
		+ "\tLD\tB,C\n"
		+ "\tLD\tA,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tH,A\n"
		+ "H_DIV1:\tLD\tC,0FFH\n"
		+ "H_DIV2:\tINC\tC\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,H_DIV2\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
    }
    if( this.libItems.contains( LibItem.ABS_NEG_HL ) ) {
      /*
       * Absolutwert und Negieren von Werten
       * Parameter:
       *   HL: zu veraendernder Wert
       * Rueckgabe:
       *   HL: Ergebnis
       */
      putCode( "ABSHL:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tP\n"
		+ "NEGHL:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,H\n"
		+ "\tPUSH\tAF\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tL,A\n"
		+ "\tINC\tHL\n"
		+ "\tPOP\tAF\n"
		+ "\tXOR\tH\n"
		+ "\tJP\tP,E_ARIT\n"
		+ "\tLD\tA,B\n"
		+ "\tXOR\t80H\n"
		+ "\tLD\tB,A\n"
		+ "\tRET\n" );
	this.libItems.add( LibItem.E_ARIT );
    }
    if( this.libItems.contains( LibItem.ARYADR ) ) {
      /*
       * Ermitteln der Adresse einer Variabeln im Variablen-Array
       * Parameter:
       *   HL: Index
       * Rueckgabewert:
       *   HL: Adresse der Variablen
       */
      putCode( "ARYADR:\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_ARG\n" );
      if( this.basicOptions.getCheckArray() ) {
	putCode( "\tEX\tDE,HL\n" );
	putCode_LD_HL_nn( this.basicOptions.getArraySize() );
	putCode( "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJP\tC,E_ARG\n"
		+ "\tJP\tZ,E_ARG\n"
		+ "\tEX\tDE,HL\n" );
      }
      putCode( "\tADD\tHL,HL\n"
		+ "\tLD\tBC,M_ARY\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.E_ARG );
      this.libItems.add( LibItem.M_ARY );
    }
    if( this.libItems.contains( LibItem.DREAD ) ) {
      /*
       * Lesen eines Wertes aus dem Datenbereich
       * Parameter:
       *   (M_READ): aktuelle Leseposition
       *   DEND:     erste Adresse hinter dem Datenbereich
       * Rueckgabe:
       *   HL: gelesener Wert
       */
      putCode( "DREAD:\tLD\tHL,(M_READ)\n"
		+ "\tLD\tDE,DEND\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tC,DREAD1\n"
		+ "\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'READ POSITION EXCEEDED\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
	putExit();
	putCode( "DREAD1:"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_READ),HL\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.DATA );
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.INCHR ) ) {
      putCode( "INCHR:\tCALL\tXINCH\n" );
      if( this.basicOptions.isBreakOnInputPossible() ) {
	putCode( "\tCP\t3\n"
		+ "\tJP\tZ,E_BRK\n" );
	this.libItems.add( LibItem.E_BRK );
      }
      putCode( "\tLD\tL,A\n"
		+ "\tLD\tH,0\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.XINCH );
    }
    if( this.libItems.contains( LibItem.INKEY ) ) {
      /*
       * Tastaturstatus abfragen und Wert des Zeichens zurueckliefern,
       * welches durch die gerade gedrueckte(n) Taste(n) gebildet wird.
       * Dabei wird auch auf Abbruch geprueft
       *
       * Rueckgabe:
       *   A: Wert des Zeichens oder 0
       */
      if( this.platform == Platform.Z9001 ) {
	putCode( "INKEY:\tCALL\tXINKEY\n"
		+ "\tCP\t3\n"
		+ "\tJR\tZ,E_BRK\n"
		+ "\tRET\n" );
	this.libItems.add( LibItem.XINKEY );
	this.libItems.add( LibItem.E_BRK );
      } else {
	putCode( "INKEY:\n" );		// weiter mit CKBRK
	this.libItems.add( LibItem.CKBRK );
      }
    }
    if( this.libItems.contains( LibItem.CKBRK ) ) {
      /*
       * Pruefen auf Abbruch
       * Diese Funktion muss unmittelbar hinter LibItem.INKEY folgen!
       */
      if( this.platform == Platform.Z9001 ) {
	putCode( "CKBRK:\tCALL\tXCKBRK\n" );
	this.libItems.add( LibItem.XCKBRK );
      } else {
	putCode( "CKBRK:\tCALL\tXINKEY\n" );
	this.libItems.add( LibItem.XINKEY );
      }
      putCode( "\tCP\t3\n"
		+ "\tRET\tNZ\n" );
      this.libItems.add( LibItem.E_BRK );
    }
    if( this.libItems.contains( LibItem.E_BRK ) ) {
      /*
       * Abbruch-Ausschrift erzeugen
       * Diese Funktion muss unmittelbar hinter LibItem.CKBRK folgen!
       */
      putCode( "E_BRK:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'BREAK\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.P_TAB ) ) {
      /*
       * Ausgabe von Leerzeichen auf dem Bildschirm
       * Parameter:
       *   HL: Anzahl der Leerzeichen (groesser oder gleich 0)
       */
      putCode( "P_TAB:\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_ARG\n"
		+ "P_TAB1:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tJR\tP_TAB1\n" );
      this.libItems.add( LibItem.E_ARG );
      this.libItems.add( LibItem.XOUTCH );
    }
    if( this.libItems.contains( LibItem.E_ARG ) ) {
      putCode( "E_ARG:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'ARGUMENT OUT OF RANGE\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.E_ARIT ) ) {
      putCode( "E_ARIT:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'ARITHMETIC OVERFLOW\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.E_NXWF ) ) {
      putCode( "E_NXWF:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'NEXT WITHOUT FOR\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.E_REWG ) ) {
      putCode( "E_REWG:\tCALL\tP_LTXT\n"
		+ "\tDEFM\t\'RETURN WITHOUT GOSUB\'\n"
		+ "\tDEFB\t0DH\n"
		+ "\tDEFB\t0\n" );
      putExit();
      this.libItems.add( LibItem.P_LTXT );
    }
    if( this.libItems.contains( LibItem.P_LTXT ) ) {
      /*
       * Ausgabe eines mit einem Null-Byte abgeschlossenen Textes
       * auf dem Bildschirm,
       * der sich unmittelbar an den Funktionsaufruf anschliesst.
       */
      putCode( "P_LTXT:\tEX\t(SP),HL\n"
		+ "\tCALL\tP_TEXT\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      this.libItems.add( LibItem.P_TEXT );
    }
    if( this.libItems.contains( LibItem.P_TEXT ) ) {
      /*
       * Ausgabe eines mit einem Null-Byte abgeschlossenen Textes
       * auf dem Bildschirm
       *
       * Parameter:
       *   HL: Adresse des Textes
       * Rueckgabe:
       *   HL: erstes Byte hinter dem abschliessenden Null-Byte
       */
      putCode( "P_TEXT:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tP_TEXT\n" );
      this.libItems.add( LibItem.XOUTCH );
    }
    if( this.libItems.contains( LibItem.P_HEXA ) ) {
      /*
       * Hexadezimale Ausgabe eines Bytes auf dem Bildschirm
       * Parameter:
       *   A: auszugebender Wert
       */
      putCode( "P_HEXA:\tPUSH\tAF\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tCALL\tP_HXA1\n"
		+ "\tPOP\tAF\n"
		+ "P_HXA1:\tAND\t0FH\n"
		+ "\tADD\tA,90H\n"
		+ "\tDAA\n"
		+ "\tADC\tA,40H\n"
		+ "\tDAA\n"
		+ "\tJP\tXOUTCH\n" );
      this.libItems.add( LibItem.XOUTCH );
    }
    if( this.libItems.contains( LibItem.JP_HL ) ) {
      /*
       * Sprung zu der Adresse, die im HL-Register steht
       * Parameter:
       *   HL: Adresse
       */
      putCode( "JP_HL:\tJP\t(HL)\n" );
    }
    if( this.libItems.contains( LibItem.OUTNL ) ) {
      /*
       * Ausgabe eines Zeilenumbruchs auf dem Bildschirm
       */
      putCode( "OUTNL:\tLD\tA,0DH\n"
		+ "\tJP\tXOUTCH\n" );
      this.libItems.add( LibItem.XOUTCH );
    }
    if( this.libItems.contains( LibItem.DATA ) ) {
      /*
       * Leseposition fuer Daten auf den Anfang des Datenbereichs setzen,
       * Speicherzelle fuer Leseposition anlegen
       */
      putCode( "DINIT:\tLD\tHL,DBEG\n"
		+ "\tLD\t(M_READ),HL\n"
		+ "\tRET\n"
		+ "DBEG:\n" );
      if( this.dataOut != null ) {
	putCode( this.dataOut );
      }
      putCode( "DEND:\n"
		+ "M_READ:\tDEFS\t2\n" );
    }
    if( this.libItems.contains( LibItem.M_LEN ) ) {
      /*
       * Speicherzelle fuer die Laenge der letzten Zeichenketteneingabe
       */
      putCode( "M_LEN:\tDEFS\t1\n" );
    }
    if( this.libItems.contains( LibItem.M_WINT ) ) {
      /*
       * Speicherzelle fuer die Feldbreite bei Integer-Ausgaben
       */
      putCode( "M_WINT:\tDEFS\t1\n" );
    }
    if( this.libItems.contains( LibItem.M_RND ) ) {
      // Speicherzelle fuer den Zufallsgenerator
      putCode( "M_RND:\tDEFS\t2\n" );
    }
    if( this.libItems.contains( LibItem.R_INT ) ) {
      if( this.basicOptions.getShowAsm() ) {
	putCode( "\n;Eingabepuffer fuer INPUT\n" );
      }
      putCode( "M_IBUF:\tDEFS\t7\n"
		+ "M_EBUF:\tDEFS\t1\n" );
    }
    if( this.basicOptions.getShowAsm()
	&& (!this.varLabels.isEmpty()
		|| this.libItems.contains( LibItem.M_ARY )) )
    {
      putCode( "\n;BASIC-Variablen\n" );
    }
    for( String varLabel : this.varLabels ) {
      putCode( varLabel );
      putCode( ":\tDEFS\t2\n" );
    }
    if( this.libItems.contains( LibItem.M_ARY ) ) {
      putCode( "M_ARY:" );
      int arraySize = this.basicOptions.getArraySize();
      if( arraySize > 0 ) {
	putCode( "\tDEFS\t" );
	putCode( getHex4( arraySize * 2 ) );
      }
      putCode( '\n' );
    }
    putCode( "M_STCK:\tDEFS\t2\n" );
    if( stackSize > 0 ) {
      if( this.basicOptions.getShowAsm() ) {
	putCode( "\n;Stack-Bereich\n" );
      }
      putCode( "\tDEFS\t" );
      putCode( getHex4( stackSize ) );
      putCode( "\n"
		+ "M_TOP:\n" );
    } else {
      if( this.libItems.contains( LibItem.M_TOP ) )
	putCode( "M_TOP:\n" );
    }
  }


	/* --- Hilfsfunktionen --- */

  private void ac1BasicMismatch()
  {
    if( !this.ac1CompatibilityChecked
	&& this.basicOptions.getStrictAC1MiniBASIC() )
    {
      putWarning( "Syntax entspricht nicht dem originalen AC1-Mini-BASIC" );
      this.ac1CompatibilityChecked = true;
    }
  }


  private static void check8BitChar( char ch ) throws PrgException
  {
    if( (ch == 0) || (ch > 0xFF) ) {
      throw new PrgException( "Zeichen \'" + ch
		+ "\' au\u00DFerhalb des 8-Bit-Wertebereiches" );
    }
  }


  private String nextLabel()
  {
    return "M" + String.valueOf( this.labelNum++ );
  }


  private void openAsmText( String text, BasicOptions basicOptions )
  {
    EditFrm editFrm = this.editText.getEditFrm();
    if( editFrm != null ) {
      EditText asmEditText = this.editText.getResultEditText();
      if( asmEditText != null ) {
	if( asmEditText.hasDataChanged() || !editFrm.contains( asmEditText ) )
	  asmEditText = null;
      }
      if( asmEditText != null ) {
	asmEditText.setText( text );
	Component tabComponent = asmEditText.getTabComponent();
	if( tabComponent != null ) {
	  editFrm.setSelectedTabComponent( tabComponent );
	}
      } else {
	asmEditText = editFrm.openText( text );
	this.editText.setResultEditText( asmEditText );
      }
      asmEditText.setPrgOptions( options );
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


  private void putCode( CharSequence text )
  {
    closePrintString();
    writeCode( text );
    this.suppressExit = false;
  }


  private void putCode( char ch )
  {
    closePrintString();
    writeCode( ch );
    this.suppressExit = false;
  }


  private void putCode_LD_A_n( int value )
  {
    putCode( "\tLD\tA," );
    putCode( getHex2( value ) );
    putCode( '\n' );
  }


  private void putCode_LD_DE_nn( int value )
  {
    putCode( "\tLD\tDE," );
    putCode( getHex4( value ) );
    putCode( '\n' );
  }


  private void putCode_LD_HL_nn( int value )
  {
    putCode( "\tLD\tHL," );
    putCode( getHex4( value ) );
    putCode( '\n' );
  }


  private void putCharToPrintString( char ch ) throws PrgException
  {
    if( ch > 0 ) {
      check8BitChar( ch );
      if( !this.printStringOpen ) {
	writeCode( "\tCALL\tP_LTXT\n" );
	this.libItems.add( LibItem.P_LTXT );
	this.defmStringOpen  = false;
	this.printStringOpen = true;
      }
      if( (ch >= '\u0020') && (ch != '\'') ) {
	if( !this.defmStringOpen ) {
	  writeCode( "\tDEFM\t\'" );
	  this.defmStringOpen = true;
	}
	writeCode( ch );
      } else {
	if( this.defmStringOpen ) {
	  writeCode( "\'\n" );
	  this.defmStringOpen = false;
	}
	writeCode( "\tDEFB\t" );
	writeCode( getHex2( ch ) );
	writeCode( '\n' );
      }
    }
  }


  private void putExit()
  {
    closePrintString();
    if( !this.suppressExit ) {
      writeCode( "\tJP\tXEXIT\n" );
      this.suppressExit = true;
    }
  }


  private static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE) && Character.isWhitespace( ch ) ) {
      ch = iter.next();
    }
    return ch;
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


  private void writeCode( CharSequence text )
  {
    if( this.asmOut != null )
      this.asmOut.append( text );
  }


  private void writeCode( char ch )
  {
    if( this.asmOut != null )
      this.asmOut.append( ch );
  }


  private void writeInit()
  {
    if( this.asmOut != null ) {
      StringBuilder buf = new StringBuilder( 0x400 );
      if( this.basicOptions.getShowAsm() ) {
	buf.append( ";\n"
		+ ";Dieser Quelltext wurde vom JKCEMU-BASIC-Compiler"
		+ " erzeugt.\n"
		+ ";Weitere Informationen unter www.jens-mueller.org/jkcemu/\n"
		+ ";\n"
		+ "\n" );
      }
      buf.append( "\tORG\t" );
      buf.append( getHex4( this.basicOptions.getBegAddr() ) );
      buf.append( (char) '\n' );
      if( this.platform == Platform.HC900 ) {
	buf.append( "\tDEFB\t7FH,7FH\n" );
	boolean invalidChars = false;
	int     nChars       = 0;
	String  appName      = this.basicOptions.getAppName();
	if( appName != null ) {
	  int len = appName.length();
	  for( int i = 0; i < len; i++ ) {
	    char ch = appName.charAt( i );
	    if( (ch >= '\u0020') && (ch < 0x7F) ) {
	      if( nChars == 0 ) {
		buf.append( "\tDEFM\t\'" );
	      }
	      buf.append( ch );
	      nChars++;
	    } else {
	      invalidChars = true;
	    }
	  }
	}
	if( invalidChars ) {
	  putWarning( "Nicht erlaubte Zeichen aus Programmnamen entfernt" );
	}
	if( nChars > 0 ) {
	  buf.append( "\'\n"
		+ "\tDEFB\t1\n"
		+ "\tENT\n" );
	} else {
	  putWarning( "Programm kann im Betriebssystem nicht aufgerufen"
				+ " werden, da der Programmnane leer ist." );
	}
      }
      buf.append( "\tJP\tMSTART\n" );
      if( this.platform == Platform.Z9001 ) {
	buf.append( "\tDEFM\t\'" );
	boolean invalidChars = false;
	int     nChars       = 0;
	String  appName      = this.basicOptions.getAppName();
	if( appName != null ) {
	  int len = appName.length();
	  for( int i = 0; (i < len) && (nChars < 8); i++ ) {
	    char ch = appName.charAt( i );
	    if( (ch >= '\u0020') && (ch < 0x7F) ) {
	      buf.append( ch );
	      nChars++;
	    } else {
	      invalidChars = true;
	    }
	  }
	}
	if( invalidChars ) {
	  putWarning( "Nicht erlaubte Zeichen aus Programmnamen entfernt" );
	}
	if( nChars == 0 ) {
	  putWarning( "Programm kann im Betriebssystem nicht aufgerufen"
				+ " werden, da der Programmnane leer ist." );
	}
	while( nChars < 8 ) {
	  buf.append( (char) '\u0020');
	  nChars++;
	}
	buf.append( "\'\n"
		+ "\tDEFB\t0\n" );
      }
      if( this.basicOptions.getShowAsm() ) {
	buf.append( "\n;Aufruf der Systemfunktionen\n" );
      }
      buf.append( "XEXIT:\tLD\tSP,(M_STCK)\n" );
      switch( this.platform ) {
	case AC1_LLC2:
	  buf.append( "\tJP\t07FDH\n" );
	  break;

	case HC900:
	  buf.append( "\tRET\n" );
	  break;

	case HUEBLERMC:
	  buf.append( "\tJP\t0F01EH\n" );
	  break;

	case Z1013:
	  buf.append( "\tJP\t0038H\n" );
	  break;

	case KRAMERMC:
	case Z9001:
	  buf.append( "\tJP\t0000H\n" );
	  break;
      }
      if( this.libItems.contains( LibItem.XOUTCH ) ) {
	buf.append( "XOUTCH:\tPUSH\tAF\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
	switch( this.platform ) {
	  case AC1_LLC2:
	    buf.append( "\tRST\t10H\n" );
	    break;

	  case HC900:
	    buf.append( "\tCP\t0DH\n"
		+ "\tJR\tNZ,XOUTC1\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDEFB\t24H\n"
		+ "\tLD\tA,0AH\n"
		+ "XOUTC1:\tCALL\t0F003H\n"
		+ "\tDEFB\t24H\n" );
	    break;

	  case HUEBLERMC:
	    buf.append( "\tCP\t0DH\n"
		+ "\tJR\tNZ,XOUTC1\n"
		+ "\tLD\tC,A\n"
		+ "\tCALL\t0F009H\n"
		+ "\tLD\tA,0AH\n"
		+ "XOUTC1:\tLD\tC,A\n"
		+ "\tCALL\t0F009H\n" );
	    break;

	  case KRAMERMC:
	    buf.append( "\tCP\t0DH\n"
		+ "\tJR\tNZ,XOUTC1\n"
		+ "\tLD\tC,A\n"
		+ "\tCALL\t00E6H\n"
		+ "\tLD\tA,0AH\n"
		+ "XOUTC1:\tLD\tC,A\n"
		+ "\tCALL\t00E6H\n" );
	    break;

	  case Z1013:
	    buf.append( "\tRST\t20H\n"
		+ "\tDEFB\t0\n" );
	    break;

	  case Z9001:
	    buf.append( "\tLD\tC,2\n"
		+ "\tLD\tE,A\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tNZ,XOUTC1\n"
		+ "\tCALL\t0005H\n"
		+ "\tLD\tE,0AH\n"
		+ "XOUTC1:\tCALL\t0005H\n" );
	}
	buf.append( "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\n" );
      }
      if( this.libItems.contains( LibItem.XINCH ) ) {
	switch( this.platform ) {
	  case AC1_LLC2:
	    buf.append( "XINCH:\tJP\t0008H\n" );
	    break;

	  case HC900:
	    buf.append( "XINCH:\tCALL\t0F003H\n"
		+ "\tDEFB\t16h\n"
		+ "\tRET\n" );
	    break;

	  case HUEBLERMC:
	    buf.append( "XINCH:\tJP\t0F003H\n" );
	    break;

	  case KRAMERMC:
	    buf.append( "XINCH:\tJP\t00E0H\n" );
	    break;

	  case Z1013:
	    buf.append( "XINCH:\tRST\t20H\n"
		+ "\tDEFB\t1\n"
		+ "\tRET\n" );
	    break;

	  case Z9001:
	    buf.append( "XINCH:\tPUSH\tBC\n"
		+ "\tLD\tC,1\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tNC,XINCH1\n"
		+ "\tXOR\tA\n"
		+ "XINCH1:\tPOP\tBC\n"
		+ "\tRET\n" );
	}
      }
      if( this.libItems.contains( LibItem.XINKEY ) ) {
	switch( this.platform ) {
	  case AC1_LLC2:
	    buf.append( "XINKEY:\tCALL\t07FAH\n"
		+ "\tAND\t7FH\n"
		+ "\tRET\n" );
	    break;

	  case HC900:
	    buf.append( "XINKEY:\tCALL\t0F003H\n"
		+ "\tDB\t0CH\n"
		+ "\tJR\tC,XINKE1\n"
		+ "\tXOR\tA\n"
		+ "\tJR\tXINKE2\n"
		+ "XINKE1:\tCALL\t0F003H\n"
		+ "\tDB\t0EH\n"
		+ "XINKE2:\tRET\n" );
	    break;

	  case HUEBLERMC:
	    buf.append( "XINKEY:\tCALL\t0F012H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tJP\t0F003H\n" );
	    break;

	  case KRAMERMC:
	    buf.append( "XINKEY:\tCALL\t00EFH\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tJP\t00E0H\n" );
	    break;

	  case Z1013:
	    buf.append( "XINKEY:\tXOR\tA\n"
		+ "\tLD\t(0004H),A\n"
		+ "\tRST\t20H\n"
		+ "\tDEFB\t4\n"
		+ "\tRET\n" );
	    break;

	  case Z9001:
	    buf.append( "XINKEY:\tPUSH\tBC\n"
		+ "\tLD\tC,11\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINKE1\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINKE2\n"
		+ "\tLD\tC,1\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tNC,XINKE2\n"
		+ "XINKE1:\tXOR\tA\n"
		+ "XINKE2:\tPOP\tBC\n"
		+ "\tRET\n" );
	}
      }
      if( this.libItems.contains( LibItem.XCKBRK ) ) {
	if( this.platform == Platform.Z9001 ) {
	  buf.append( "XCKBRK:\tPUSH\tBC\n"
		+ "\tLD\tC,11\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tNC,XBRK1\n"
		+ "\tXOR\tA\n"
		+ "XBRK1:\tPOP\tBC\n"
		+ "\tRET\n" );
	}
      }
      if( this.basicOptions.getShowAsm() ) {
	buf.append( "\n;Programmstart\n" );
      }
      buf.append( "MSTART:\tLD\t(M_STCK),SP\n" );
      if( this.basicOptions.getStackSize() > 0 ) {
	buf.append( "\tLD\tSP,M_TOP" );
	this.libItems.add( LibItem.M_TOP );
      }
      buf.append( '\n' );
      if( this.libItems.contains( LibItem.DATA ) ) {
	buf.append( "\tCALL\tDINIT\n" );
      }
      this.asmOut.insert( 0, buf );
    }
  }


  private void z1013BasicMismatch()
  {
    if( !this.z1013CompatibilityChecked
	&& this.basicOptions.getStrictZ1013TinyBASIC() )
    {
      putWarning( "Syntax entspricht nicht dem originalen Z1013-Tiny-BASIC" );
      this.z1013CompatibilityChecked = true;
    }
  }
}

