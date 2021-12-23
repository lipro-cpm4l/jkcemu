/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z80-Assembler
 */

package jkcemu.programming.assembler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import jkcemu.base.EmuUtil;
import jkcemu.etc.ReadableByteArrayOutputStream;
import jkcemu.file.FileFormat;
import jkcemu.file.FileSaver;
import jkcemu.file.FileUtil;
import jkcemu.file.LoadData;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgLogger;
import jkcemu.programming.PrgOptions;
import jkcemu.programming.PrgSource;
import jkcemu.programming.TooManyErrorsException;


public class Z80Assembler
{
  public static enum Syntax { ALL, ZILOG_ONLY, ROBOTRON_ONLY };

  private static final String[] sortedReservedWords = {
	"ADC", "ADD", "ALIGN", "AND", "BINCLUDE", "BIT",
	"CALL", "CCF", "CPD", "CPDR", "CPI", "CPIR", "CPL", "CPU",
	"DA", "DAA", "DB", "DEC", "DEFA", "DEFB", "DEFH",
	"DEFM", "DEFS", "DEFW",
	"DFB", "DFH", "DFS", "DFW", "DI", "DJNZ", "DW",
	"EI", "ELSE", "END", "ENDIF", "ENT", "ENTRY", "EQU",
	"ERROR", "EVEN", "EX", "EXX",
	"HALT", "HEX",
	"IFDEF", "IFE", "IFF", "IFNDEF", "IM",
	"IN", "INC", "INCLUDE", "IND", "INDR", "INI", "INIR",
	"JP", "JR",
	"LD", "LDD", "LDDR", "LDI", "LDIR", "LISTOFF", "LISTON",
	"NAME", "NEG", "NEWPAGE", "NOP",
	"OR", "ORG", "OTDR", "OTIR", "OUT", "OUTD", "OUTI",
	"PAGE", "POP", "PUSH",
	"RES", "RET", "RETI", "RETN", "RL", "RLA", "RLC", "RLCA", "RLD",
	"RR", "RRA", "RRC", "RRCA", "RRD", "RST",
	"SBC", "SCF", "SET", "SLA", "SRA", "SRL", "SUB", "TITLE",
	"U880", "U880UNDOC", "XOR", "Z80", "Z80UNDOC" };

  private static final String[] sortedReservedRobotronWords = {
	"CAC", "CAM", "CANC", "CANZ", "CAP", "CAPE", "CAPO", "CAZ", "CMP",
	"EXAF", "INF", "JMP", "JPC", "JPM", "JPNC", "JPNZ", "JPP",
	"JPPE", "JPPO", "JPZ", "JRC", "JRNC", "JRNZ", "JRZ",
	"RC", "RM", "RNC", "RNZ", "RP", "RPE", "RPO", "RZ" };

  private static final String BUILT_IN_LABEL = "__JKCEMU__";

  private PrgSource                     curSource;
  private PrgSource                     mainSource;
  private PrgOptions                    options;
  private PrgLogger                     logger;
  private Stack<AsmStackEntry>          stack;
  private Map<File,byte[]>              file2Bytes;
  private Map<File,PrgSource>           file2Source;
  private Map<String,AsmLabel>          labels;
  private AsmLabel[]                    sortedLabels;
  private StringBuilder                 srcOut;
  private StringBuilder                 listOut;
  private ReadableByteArrayOutputStream codeBuf;
  private byte[]                        codeOut;
  private String                        appName;
  private boolean                       addrOverflow;
  private boolean                       cpuDone;
  private boolean                       interactive;
  private boolean                       orgOverlapped;
  private boolean                       relJumpsTooLong;
  private boolean                       restartAsm;
  private boolean                       suppressLineAddr;
  private boolean                       status;
  private boolean                       listEnabled;
  private volatile boolean              execEnabled;
  private Integer                       entryAddr;
  private int                           begAddr;
  private int                           endAddr;
  private int                           curAddr;
  private int                           instBegAddr;
  private int                           passNum;
  private int                           listLineNum;
  private int                           errCnt;


  public Z80Assembler(
		String     srcText,
		String     srcName,
		File       srcFile,
		PrgOptions options,
		PrgLogger  logger,
		boolean    interactive )
  {
    this.curSource   = null;
    this.mainSource  = null;
    this.options     = options;
    this.logger      = logger;
    this.interactive = interactive;
    this.cpuDone     = false;
    this.srcOut      = null;
    this.listOut     = null;
    this.codeBuf     = null;
    this.appName     = null;
    this.stack       = new Stack<>();
    this.file2Bytes  = new HashMap<>();
    this.file2Source = new HashMap<>();
    this.labels      = new HashMap<>();
    if( this.options.getFormatSource() && (srcText != null) ) {
      this.srcOut = new StringBuilder( Math.max( srcText.length(), 16 ) );
    }
    if( this.options.getCreateCode() ) {
      this.codeBuf = new ReadableByteArrayOutputStream( 0x8000 );
    }
    init();

    // Quelltext oeffnen
    if( srcText != null ) {
      this.mainSource = PrgSource.readText( srcText, srcName, srcFile );
    } else {
      if( srcFile != null ) {
	try {
	  this.mainSource = PrgSource.readFile( srcFile );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( msg.trim().isEmpty() ) {
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
  }


  public boolean addLabel( String labelName, int value, boolean addrLabel )
  {
    if( !options.getLabelsCaseSensitive() ) {
      labelName = labelName.toUpperCase();
    }
    return (this.labels.put(
		labelName,
		new AsmLabel( labelName, value, addrLabel ) ) == null);
  }


  public boolean assemble( boolean forZ9001 ) throws IOException
  {
    try {
      do {
	reset();
        this.passNum = 1;
	parseAsm();
	computeMissingLabelValues();
	if( this.execEnabled && this.status ) {
	  if( this.mainSource != null ) {
	    this.mainSource.reset();
	  }
	  this.passNum   = 2;
	  this.curSource = this.mainSource;
	  if( this.options.getCreateAsmListing() ) {
	    this.listEnabled = true;
	    this.listOut     = new StringBuilder( 0x4000 );
	    this.listOut.append( "Assembler-Listing erzeugt von JKCEMU\n" );
	    if( this.mainSource != null ) {
	      String srcName = this.mainSource.getName();
	      if( srcName != null ) {
		this.listOut.append( "Quelle: " );
		this.listOut.append( srcName );
		this.listOut.append( '\n' );
	      }
	    }
	    printListTableHeader();
	  }
	  parseAsm();
	  if( this.restartAsm ) {
	    appendToOutLog( "Assembliere erneut...\n" );
	  } else {
	    if( this.codeBuf != null ) {
	      this.codeBuf.close();
	      if( this.execEnabled && this.status ) {
		this.codeOut = this.codeBuf.toByteArray();
	      }
	    }
	    if( this.execEnabled && this.status ) {
	      if( this.options.getPrintLabels() ) {
		printLabels();
	      }
	      if( this.options.getCodeToFile() ) {
		writeCodeToFile( forZ9001 );
	      }
	    }
	  }
	}
      } while( this.restartAsm );
    }
    catch( TooManyErrorsException ex ) {
      appendToErrLog( "\nAbgebrochen aufgrund zu vieler Fehler\n" );
      this.codeOut = null;
    }
    if( this.execEnabled && (this.errCnt > 0) ) {
      appendToErrLog( String.format( "%d Fehler\n", this.errCnt ) );
      this.codeOut = null;
    }
    return this.status;
  }


  public void cancel()
  {
    this.execEnabled = false;
  }


  public int getBegAddr()
  {
    return this.begAddr;
  }


  public int getEndAddr()
  {
    return this.endAddr;
  }


  public byte[] getCreatedCode()
  {
    return this.codeOut;
  }


  public Integer getEntryAddr()
  {
    return this.entryAddr;
  }


  public String getFormattedSourceText()
  {
    return this.srcOut != null ? this.srcOut.toString() : null;
  }


  public Integer getLabelValue( String labelName )
  {
    Integer rv = null;
    if( labelName != null ) {
      AsmLabel label = this.labels.get( labelName );
      if( label != null ) {
	Object o = label.getLabelValue();
	if( o != null ) {
	  if( o instanceof Integer ) {
	    rv = (Integer) o;
	  }
	}
      }
    }
    return rv;
  }


  public StringBuilder getListing()
  {
    return this.listOut;
  }


  public boolean getOrgOverlapped()
  {
    return this.orgOverlapped;
  }


  public PrgOptions getOptions()
  {
    return this.options;
  }


  public Collection<PrgSource> getPrgSources()
  {
    java.util.List<PrgSource> sources
		= new ArrayList<>( this.file2Source.size() + 1 );
    if( this.mainSource != null ) {
      sources.add( this.mainSource );
    }
    sources.addAll( this.file2Source.values() );
    return sources;
  }


  public boolean getRelJumpsTooLong()
  {
    return this.relJumpsTooLong;
  }


  /*
   * Sortierte Ausgabe der Markentabelle,
   * Eingebaute Marken sind nicht enthalten.
   */
  public AsmLabel[] getSortedLabels()
  {
    AsmLabel[] rv = this.sortedLabels;
    if( rv == null ) {
      int nSrc = this.labels.size();
      if( nSrc > 0 ) {
	Map<String,AsmLabel> labelMap = this.labels;
	try {
	  labelMap = new HashMap<>( nSrc );
	  labelMap.putAll( this.labels );
	  labelMap.remove( BUILT_IN_LABEL );
	}
	catch( UnsupportedOperationException
		| ClassCastException
		| IllegalArgumentException ex )
	{
	  labelMap = this.labels;
	}
	try {
	  Collection<AsmLabel> c = labelMap.values();
	  if( c != null ) {
	    int nAry = c.size();
	    if( nAry > 0 ) {
	      rv = c.toArray( new AsmLabel[ nAry ] );
	      if( rv != null ) {
		Arrays.sort( rv );
	      }
	    } else {
	      rv = new AsmLabel[ 0 ];
	    }
	  }
	}
	catch( ArrayStoreException ex ) {}
	catch( ClassCastException ex ) {}
	finally {
	  this.sortedLabels = rv;
	}
      }
    }
    return rv;
  }


  public boolean isReservedWord( String upperText )
  {
    boolean rv = (Arrays.binarySearch(
				sortedReservedWords,
				upperText ) >= 0);
    if( !rv ) {
      Syntax syntax = this.options.getAsmSyntax();
      if( (syntax == Syntax.ALL) || (syntax == Syntax.ROBOTRON_ONLY) ) {
	if( Arrays.binarySearch(
			sortedReservedRobotronWords,
			upperText ) >= 0 )
	{
	  rv = true;
	}
      }
      if( (syntax == Syntax.ALL) || (syntax == Syntax.ZILOG_ONLY) ) {
	rv |= upperText.equals( "CP" );
      }
    }
    if( this.options.getAllowUndocInst() ) {
      rv |= upperText.equals( "SLL" );
    }
    return rv;
  }


  public void putWarning( String msg )
  {
    if( this.passNum == 2 )
      appendLineNumMsgToErrLog( msg, "Warnung" );
  }


	/* --- private Methoden --- */

  private void appendCharsToListing( char ch, int count )
  {
    if( this.listEnabled && (this.listOut != null) ) {
      for( int i = 0; i < count; i++ ) {
	this.listOut.append( ch );
      }
    }
  }


  private int appendCodeBytesToListing( int addr, int maxCnt )
  {
    int rv = 0;
    if( this.listEnabled
	&& (this.listOut != null)
	&& (this.codeBuf != null)
	&& (addr < this.curAddr) )
    {
      this.listOut.append( String.format( "   %04X  ", addr ) );
      while( (addr < this.curAddr) && (maxCnt > 0) ) {
	addr++;
	int b = this.codeBuf.getFromEnd( this.curAddr - addr );
	if( b < 0 ) {
	  break;
	}
	this.listOut.append( String.format( " %02X", b ) );
	--maxCnt;
	rv++;
      }
    }
    return rv;
  }


  private void appendEndOfIncludeToListing()
  {
    if( this.listEnabled && (this.listOut != null) ) {
      appendLineNumToListing();
      appendCharsToListing( '\u0020', 3 );
      appendCharsToListing( '-', 20 );
      this.listOut.append( " Ende der Include-Datei " );
      appendCharsToListing( '-', 20 );
      this.listOut.append( '\n' );
    }
  }


  private void appendLineNumMsgToErrLog( String msg, String msgType )
  {
    StringBuilder buf = new StringBuilder( 128 );
    if( this.curSource != null ) {
      int lineNum = this.curSource.getLineNum();
      if( lineNum > 0 ) {
	String srcName = this.curSource.getName();
	if( srcName != null ) {
	  if( !srcName.isEmpty() ) {
	    buf.append( srcName );
	    buf.append( ": " );
	  }
	}
	if( msgType != null ) {
	  buf.append( msgType );
	  buf.append( " in " );
	}
	buf.append( "Zeile " );
	buf.append( lineNum );
	buf.append( ": " );
      }
    }
    if( msg != null ) {
      buf.append( msg );
    }
    if( !msg.endsWith( "\n" ) ) {
      buf.append( '\n' );
    }
    appendToErrLog( buf.toString() );
  }


  private void appendLineNumToListing()
  {
    if( this.listEnabled && (this.listOut != null) )
      this.listOut.append( String.format( "%5d", this.listLineNum++ ) );
  }


  private void appendToErrLog( String text )
  {
    if( this.logger != null )
      this.logger.appendToErrLog( text );
  }


  private void appendToOutLog( String text )
  {
    if( this.logger != null )
      this.logger.appendToOutLog( text );
  }


  private void checkPrint16BitWarning( int value )
  {
    if( (value < ~0x7FFF) || (value > 0xFFFF) ) {
      putWarning( "Numerischer Wert au\u00DFerhalb 16-Bit-Bereich:"
					+ "Bits gehen verloren" );
    }
  }


  private void checkAddr() throws PrgException
  {
    /*
     * Wenn Code bis zur Adresse 0xFFFF erzeugt wurde,
     * steht this.curAddr auf 0x10000.
     * Wurde dieser Wert allerdings ueberschritten,
     * liegt ein Adressueberlauf vor.
     */
    if( !this.addrOverflow && (this.curAddr > 0x10000) ) {
      this.addrOverflow = true;
      throw new PrgException( "\u00DCberlauf: Adressz\u00E4hler > 0FFFFh" );
    }
  }


  private void computeMissingLabelValues()
  {
    boolean computed = false;
    boolean failed   = false;
    do {
      computed = false;
      failed   = false;
      for( AsmLabel label : this.labels.values() ) {
	Object o = label.getLabelValue();
	if( o != null ) {
	  if( !(o instanceof Integer) ) {
	    String text = o.toString();
	    if( text != null ) {
	      try {
	 	Integer v = ExprParser.parse(
				text,
				this.instBegAddr,
				this.labels,
				false,
				this.options.getLabelsCaseSensitive() );
		if( v != null ) {
		  label.setLabelValue( v, false );
		  computed = true;
		} else {
		  failed = true;
		}
	      }
	      catch( PrgException ex ) {}
	    }
	  }
	}
      }
    } while( this.execEnabled && computed && failed );
  }


  private void init()
  {
    this.sortedLabels     = null;
    this.codeOut          = null;
    this.addrOverflow     = false;
    this.orgOverlapped    = false;
    this.relJumpsTooLong  = false;
    this.restartAsm       = false;
    this.suppressLineAddr = false;
    this.status           = true;
    this.execEnabled      = true;
    this.entryAddr        = null;
    this.begAddr          = -1;
    this.endAddr          = -1;
    this.curAddr          = 0;
    this.instBegAddr      = 0;
    this.passNum          = 0;
    this.errCnt           = 0;
    this.listLineNum      = 1;
  }


  private boolean isAssemblingEnabled()
  {
    boolean rv = true;
    for( AsmStackEntry e : this.stack ) {
      if( !e.isAssemblingEnabled() ) {
	rv = false;
	break;
      }
    }
    return rv;
  }


  private void parseAsm() throws IOException, TooManyErrorsException
  {
    this.begAddr     = -1;
    this.endAddr     = -1;
    this.curAddr     = 0;
    this.instBegAddr = 0;
    this.stack.clear();
    while( this.execEnabled && (this.curSource != null) ) {
      String line = this.curSource.readLine();
      if( line != null ) {
	parseLine( line );
      } else {
	if( this.curSource != this.mainSource ) {
	  appendEndOfIncludeToListing();
	  this.curSource = this.mainSource;
	} else {
	  this.curSource = null;
	}
      }
    }
    if( !this.stack.isEmpty() ) {
      try {
	int lineNum = this.stack.peek().getLineNum();
	StringBuilder buf = new StringBuilder( 32 );
	buf.append( "Bedingung" );
	if( lineNum > 0 ) {
	  buf.append( " in Zeile " );
	  buf.append( lineNum );
	}
	buf.append( " nicht geschlossen (ENDIF fehlt)" );
	appendLineNumMsgToErrLog( buf.toString(), EmuUtil.TEXT_ERROR );
	this.status = false;
	this.errCnt++;
      }
      catch( EmptyStackException ex ) {}
    }
  }


  private int parseExpr( String text ) throws PrgException
  {
    int     rv    = 0;
    Integer value = ExprParser.parse(
			text,
			this.instBegAddr,
			this.labels,
			this.passNum == 2,
			this.options.getLabelsCaseSensitive() );
    if( value != null ) {
      rv = value.intValue();
    } else {
      if( this.passNum == 2 ) {
	throw new PrgException( "Wert nicht ermittelbar" );
      }
    }
    return rv;
  }


  private void parseLine( String line )
				throws IOException, TooManyErrorsException
  {
    this.instBegAddr      = this.curAddr;
    this.suppressLineAddr = false;
    boolean isBinIncl     = false;
    boolean isSrcIncl     = false;
    boolean listCode      = false;
    boolean listOff       = false;
    String  labelName     = null;
    try {
      AsmLine asmLine = AsmLine.scanLine(
				this,
				line,
				this.options.getLabelsCaseSensitive() );
      if( asmLine != null ) {
	labelName = asmLine.getLabel();
	if( labelName != null ) {
	  if( this.passNum == 1 ) {
	    if( this.labels.containsKey( labelName ) ) {
	      throw new PrgException(
			"Marke " + labelName + " bereits vergeben" );
	    }
	    this.labels.put(
			labelName,
			new AsmLabel( labelName, this.curAddr, true ) );
	    this.sortedLabels = null;
	  }
	}
	String instruction = asmLine.getInstruction();
	if( instruction != null ) {
	  if( !instruction.isEmpty() ) {
	    if( instruction.equals( "IF" )
		|| instruction.equals( "IFT" ) )
	    {
	      parseIF( asmLine, true );
	    }
	    else if( instruction.equals( "IFE" )
		     || instruction.equals( "IFF" ) )
	    {
	      parseIF( asmLine, false );
	    }
	    else if( instruction.equals( "IFDEF" ) ) {
	      parseIFDEF( asmLine );
	    }
	    else if( instruction.equals( "IFNDEF" ) ) {
	      parseIFNDEF( asmLine );
	    }
	    else if( instruction.equals( "ELSE" ) ) {
	      parseELSE( asmLine );
	    }
	    else if( instruction.equals( "ENDIF" ) ) {
	      parseENDIF( asmLine );
	    } else {
	      if( isAssemblingEnabled() ) {
		listCode = true;
		if( instruction.equals( "ADD" ) ) {
		  parseADD( asmLine );
		}
		else if( instruction.equals( "ADC" ) ) {
		  parseADC_SBC( asmLine, 0x88, 0x4A );
		}
		else if( instruction.equals( "ALIGN" )
			 || instruction.equals( ".ALIGN" ) )
		{
		  parseALIGN( asmLine );
		}
		else if( instruction.equals( "AND" ) ) {
		  parseBiOp8( asmLine, 0xA0 );
		}
		else if( instruction.equals( "BINCLUDE" )
			 || instruction.equals( ".BINCLUDE" ) )
		{
		  parseBINCLUDE( asmLine );
		  isBinIncl = true;
		}
		else if( instruction.equals( "BIT" ) ) {
		  parseSingleBit( asmLine, 0x40 );
		}
		else if( instruction.equals( "CAC" ) ) {
		  parseInstDirectAddr( asmLine, 0xDC );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CALL" ) ) {
		  parseCALL( asmLine );
		}
		else if( instruction.equals( "CAM" ) ) {
		  parseInstDirectAddr( asmLine, 0xFC );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CANC" ) ) {
		  parseInstDirectAddr( asmLine, 0xD4 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CANZ" ) ) {
		  parseInstDirectAddr( asmLine, 0xC4 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CAP" ) ) {
		  parseInstDirectAddr( asmLine, 0xF4 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CAPE" ) ) {
		  parseInstDirectAddr( asmLine, 0xEC );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CAPO" ) ) {
		  parseInstDirectAddr( asmLine, 0xE4 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CAZ" ) ) {
		  parseInstDirectAddr( asmLine, 0xCC );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CCF" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x3F );
		}
		else if( instruction.equals( "CMP" ) ) {
		  parseBiOp8( asmLine, 0xB8 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "CP" ) ) {
		  parseBiOp8( asmLine, 0xB8 );
		  zilogMnemonic();
		}
		else if( instruction.equals( "CPD" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA9 );
		}
		else if( instruction.equals( "CPDR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB9 );
		}
		else if( instruction.equals( "CPI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA1 );
		}
		else if( instruction.equals( "CPIR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB1 );
		}
		else if( instruction.equals( "CPL" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x2F );
		}
		else if( instruction.equals( "CPU" )
			 || instruction.equals( ".CPU" ) )
		{
		  parseCPU( asmLine );
		}
		else if( instruction.equals( "DAA" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x27 );
		}
		else if( instruction.equals( "DEC" ) ) {
		  parseINC_DEC( asmLine, 0x05, 0x0B );
		}
		else if( instruction.equals( "DEFB" )
			 || instruction.equals( ".DEFB" )
			 || instruction.equals( "DEFM" )
			 || instruction.equals( ".DEFM" )
			 || instruction.equals( "DFB" )
			 || instruction.equals( ".DFB" )
			 || instruction.equals( "DB" )
			 || instruction.equals( ".DB" ) )
		{
		  parseDEFB( asmLine );
		}
		else if( instruction.equals( "DEFH" )
			 || instruction.equals( ".DEFH" )
			 || instruction.equals( "DFH" )
			 || instruction.equals( ".DFH" )
			 || instruction.equals( "HEX" )
			 || instruction.equals( ".HEX" ) )
		{
		  parseDEFH( asmLine );
		}
		else if( instruction.equals( "DEFS" )
			 || instruction.equals( ".DEFS" )
			 || instruction.equals( "DFS" )
			 || instruction.equals( ".DFS" )
			 || instruction.equals( "DS" )
			 || instruction.equals( ".DS" ) )
		{
		  parseDEFS( asmLine );
		}
		else if( instruction.equals( "DEFW" )
			 || instruction.equals( ".DEFW" )
			 || instruction.equals( "DFW" )
			 || instruction.equals( ".DFW" )
			 || instruction.equals( "DA" )
			 || instruction.equals( ".DA" )
			 || instruction.equals( "DW" )
			 || instruction.equals( ".DW" ) )
		{
		  parseDEFW( asmLine );
		}
		else if( instruction.equals( "DI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xF3 );
		}
		else if( instruction.equals( "DJNZ" ) ) {
		  int d = getAddrDiff( asmLine, asmLine.nextArg() );
		  asmLine.checkEOL();
		  putCode( 0x10 );
		  putCode( d );
		}
		else if( instruction.equals( "EI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xFB );
		}
		else if( instruction.equals( "END" )
			 || instruction.equals( ".END" ) )
		{
		  parseEND( asmLine );
		}
		else if( instruction.equals( "ENT" )
			 || instruction.equals( ".ENT" )
			 || instruction.equals( "ENTRY" )
			 || instruction.equals( ".ENTRY" ) )
		{
		  parseENTRY( asmLine );
		}
		else if( instruction.equals( "EQU" )
			 || instruction.equals( ".EQU" ) )
		{
		  parseEQU( asmLine );
		  listCode = false;
		}
		else if( instruction.equals( "ERROR" )
			 || instruction.equals( ".ERROR" ) )
		{
		  parseERROR( asmLine );
		}
		else if( instruction.equals( "EVEN" )
			 || instruction.equals( ".EVEN" ) )
		{
		  parseEVEN( asmLine );
		}
		else if( instruction.equals( "EX" ) ) {
		  parseEX( asmLine );
		}
		else if( instruction.equals( "EXAF" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x08 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "EXX" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xD9 );
		}
		else if( instruction.equals( "HALT" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x76 );
		}
		else if( instruction.equals( "IM" ) ) {
		  parseIM( asmLine );
		}
		else if( instruction.equals( "IN" ) ) {
		  parseIN( asmLine );
		}
		else if( instruction.equals( "INC" ) ) {
		  parseINC_DEC( asmLine, 0x04, 0x03 );
		}
		else if( instruction.equals( "INCLUDE" )
			 || instruction.equals( ".INCLUDE" ) )
		{
		  parseINCLUDE( asmLine );
		  isSrcIncl = true;
		}
		else if( instruction.equals( "INF" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0x70 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "IND" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xAA );
		}
		else if( instruction.equals( "INDR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xBA );
		}
		else if( instruction.equals( "INI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA2 );
		}
		else if( instruction.equals( "INIR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB2 );
		}
		else if( instruction.equals( "JMP" ) ) {
		  parseJMP( asmLine );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JP" ) ) {
		  parseJP( asmLine );
		}
		else if( instruction.equals( "JPC" ) ) {
		  parseInstDirectAddr( asmLine, 0xDA );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPM" ) ) {
		  parseInstDirectAddr( asmLine, 0xFA );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPNC" ) ) {
		  parseInstDirectAddr( asmLine, 0xD2 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPNZ" ) ) {
		  parseInstDirectAddr( asmLine, 0xC2 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPP" ) ) {
		  parseInstDirectAddr( asmLine, 0xF2 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPPE" ) ) {
		  parseInstDirectAddr( asmLine, 0xEA );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPPO" ) ) {
		  parseInstDirectAddr( asmLine, 0xE2 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JPZ" ) ) {
		  parseInstDirectAddr( asmLine, 0xCA );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JR" ) ) {
		  parseJR( asmLine );
		}
		else if( instruction.equals( "JRC" ) ) {
		  int d = getAddrDiff( asmLine, asmLine.nextArg() );
		  asmLine.checkEOL();
		  putCode( 0x38 );
		  putCode( d );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JRNC" ) ) {
		  int d = getAddrDiff( asmLine, asmLine.nextArg() );
		  asmLine.checkEOL();
		  putCode( 0x30 );
		  putCode( d );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JRNZ" ) ) {
		  int d = getAddrDiff( asmLine, asmLine.nextArg() );
		  asmLine.checkEOL();
		  putCode( 0x20 );
		  putCode( d );
		  robotronMnemonic();
		}
		else if( instruction.equals( "JRZ" ) ) {
		  int d = getAddrDiff( asmLine, asmLine.nextArg() );
		  asmLine.checkEOL();
		  putCode( 0x28 );
		  putCode( d );
		  robotronMnemonic();
		}
		else if( instruction.equals( "LD" ) ) {
		  parseLD( asmLine );
		}
		else if( instruction.equals( "LDD" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA8 );
		}
		else if( instruction.equals( "LDDR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB8 );
		}
		else if( instruction.equals( "LDI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA0 );
		}
		else if( instruction.equals( "LDIR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB0 );
		}
		else if( instruction.equals( "LISTOFF" )
			 || instruction.equals( ".LISTOFF" ) )
		{
		  asmLine.checkEOL();
		  listOff = true;
		}
		else if( instruction.equals( "LISTON" )
			 || instruction.equals( ".LISTON" ) )
		{
		  asmLine.checkEOL();
		  this.listEnabled = true;
		}
		else if( instruction.equals( "NAME" )
			 || instruction.equals( ".NAME" )
			 || instruction.equals( "TITLE" )
			 || instruction.equals( ".TITLE" ) )
		{
		  parseNAME( asmLine );
		}
		else if( instruction.equals( "NEG" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0x44 );
		}
		else if( instruction.equals( "NEWPAGE" )
			 || instruction.equals( ".NEWPAGE" )
			 || instruction.equals( "PAGE" )
			 || instruction.equals( ".PAGE" ) )
		{
		  asmLine.checkEOL();
		  if( this.listEnabled && (this.listOut != null) ) {
		    this.listOut.append( '\f' );
		    printListTableHeader();
		  }
		}
		else if( instruction.equals( "NOP" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x00 );
		}
		else if( instruction.equals( "OR" ) ) {
		  parseBiOp8( asmLine, 0xB0 );
		}
		else if( instruction.equals( "ORG" )
			 || instruction.equals( ".ORG" ) )
		{
		  parseORG( asmLine );
		  listCode = false;
		}
		else if( instruction.equals( "OTDR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xBB );
		}
		else if( instruction.equals( "OTIR" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xB3 );
		}
		else if( instruction.equals( "OUT" ) ) {
		  parseOUT( asmLine );
		}
		else if( instruction.equals( "OUTD" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xAB );
		}
		else if( instruction.equals( "OUTI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0xA3 );
		}
		else if( instruction.equals( "POP" ) ) {
		  parsePUSH_POP( asmLine, 0xC1 );
		}
		else if( instruction.equals( "PUSH" ) ) {
		  parsePUSH_POP( asmLine, 0xC5 );
		}
		else if( instruction.equals( "RC" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xD8 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RES" ) ) {
		  parseSingleBit( asmLine, 0x80 );
		}
		else if( instruction.equals( "RET" ) ) {
		  parseRET( asmLine );
		}
		else if( instruction.equals( "RETI" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0x4D );
		}
		else if( instruction.equals( "RETN" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xED );
		  putCode( 0x45 );
		}
		else if( instruction.equals( "RM" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xF8 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RNC" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xD0 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RNZ" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xC0 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RL" ) ) {
		  parseRotShift( asmLine, 0x10 );
		}
		else if( instruction.equals( "RLA" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x17 );
		}
		else if( instruction.equals( "RLC" ) ) {
		  parseRotShift( asmLine, 0x00 );
		}
		else if( instruction.equals( "RLCA" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x07 );
		}
		else if( instruction.equals( "RLD" ) ) {
		  parseRXD( asmLine, 0x6F );
		}
		else if( instruction.equals( "RP" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xF0 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RPE" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xE8 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RPO" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xE0 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "RR" ) ) {
		  parseRotShift( asmLine, 0x18 );
		}
		else if( instruction.equals( "RRA" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x1F );
		}
		else if( instruction.equals( "RRC" ) ) {
		  parseRotShift( asmLine, 0x08 );
		}
		else if( instruction.equals( "RRCA" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x0F );
		}
		else if( instruction.equals( "RRD" ) ) {
		  parseRXD( asmLine, 0x67 );
		}
		else if( instruction.equals( "RST" ) ) {
		  parseRST( asmLine );
		}
		else if( instruction.equals( "RZ" ) ) {
		  asmLine.checkEOL();
		  putCode( 0xC8 );
		  robotronMnemonic();
		}
		else if( instruction.equals( "SBC" ) ) {
		  parseADC_SBC( asmLine, 0x98, 0x42 );
		}
		else if( instruction.equals( "SCF" ) ) {
		  asmLine.checkEOL();
		  putCode( 0x37 );
		}
		else if( instruction.equals( "SET" ) ) {
		  parseSingleBit( asmLine, 0xC0 );
		}
		else if( instruction.equals( "SLA" ) ) {
		  parseRotShift( asmLine, 0x20 );
		}
		else if( instruction.equals( "SLL" ) ) {
		  parseRotShift( asmLine, 0x30 );
		  undocInst();
		}
		else if( instruction.equals( "SRA" ) ) {
		  parseRotShift( asmLine, 0x28 );
		}
		else if( instruction.equals( "SRL" ) ) {
		  parseRotShift( asmLine, 0x38 );
		}
		else if( instruction.equals( "SUB" ) ) {
		  parseBiOp8( asmLine, 0x90 );
		}
		else if( instruction.equals( "XOR" ) ) {
		  parseBiOp8( asmLine, 0xA8 );
		}
		else if( instruction.equals( "U880" )
			 || instruction.equals( ".U880" ) )
		{
		  parseCPU( asmLine, Syntax.ROBOTRON_ONLY, false );
		}
		else if( instruction.equals( "U880UNDOC" )
			 || instruction.equals( ".U880UNDOC" ) )
		{
		  parseCPU( asmLine, Syntax.ROBOTRON_ONLY, true );
		}
		else if( instruction.equals( "Z80" )
			 || instruction.equals( ".Z80" ) )
		{
		  parseCPU( asmLine, Syntax.ZILOG_ONLY, false );
		}
		else if( instruction.equals( "Z80UNDOC" )
			 || instruction.equals( ".Z80UNDOC" ) )
		{
		  parseCPU( asmLine, Syntax.ZILOG_ONLY, true );
		} else {
		  throw new PrgException(
			    "\'" + instruction + "\': Unbekannte Mnemonik" );
		}
	      }
	    }
	  }
	}
	if( (this.srcOut != null) && (this.passNum == 2) ) {
	  asmLine.appendFormattedTo( this.srcOut );
	  this.srcOut.append( '\n' );
	}
      }
    }
    catch( PrgException ex ) {
      putError( ex.getMessage() );
    }
    finally {
      if( this.interactive
	  && !this.suppressLineAddr
	  && (this.curSource != null)
	  && (this.passNum == 2) )
      {
	if( this.instBegAddr < this.curAddr ) {
	  this.curSource.setLineAddr( this.instBegAddr );
	} else {
	  if( labelName != null ) {
	    AsmLabel label = this.labels.get( labelName );
	    if( label != null ) {
	      Object value = label.getLabelValue();
	      if( value != null ) {
		if( value instanceof Integer ) {
		  if( ((Integer) value).intValue() == this.instBegAddr ) {
		    this.curSource.setLineAddr( this.instBegAddr );
		  }
		}
	      }
	    }
	  }
	}
      }
    }

    // Zeile ggf. an Listing anhaengen
    if( this.listEnabled && (this.listOut != null) ) {
      int lineBegIdx = this.listOut.length();
      int addr       = this.instBegAddr;
      appendLineNumToListing();
      if( listCode && !isBinIncl && !isSrcIncl
	  && (this.codeBuf != null) && (addr < this.curAddr) )
      {
	addr += appendCodeBytesToListing( addr, 5 );
      }
      if( !line.isEmpty() ) {
	for( int i = this.listOut.length(); i < (lineBegIdx + 32); i++ ) {
	  this.listOut.append( '\u0020' );
	}
	this.listOut.append( line );
      }
      if( isBinIncl || isSrcIncl ) {
	this.listOut.append( '\n' );
	appendLineNumToListing();
	appendCharsToListing( '\u0020', 3 );
	appendCharsToListing( '-', 19 );
	this.listOut.append( " Anfang der Include-Datei " );
	appendCharsToListing( '-', 19 );
      }
      if( listCode && (this.codeBuf != null) ) {
	while( addr < this.curAddr ) {
	  lineBegIdx = this.listOut.length();
	  this.listOut.append( '\n' );
	  appendLineNumToListing();
	  int n = appendCodeBytesToListing( addr, isBinIncl ? 16 : 5 );
	  if( n <= 0 ) {
	    this.listOut.setLength( lineBegIdx );
	    break;
	  }
	  addr += n;
	}
      }
      this.listOut.append( '\n' );
      if( isBinIncl ) {
	appendEndOfIncludeToListing();
      }
    }
    if( listOff ) {
      this.listEnabled = false;
    }
  }


  private void parseADD( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.equalsUpper( "HL" ) ) {
	if( a2.equalsUpper( "BC" ) ) {
	  putCode( 0x09 );
	}
	else if( a2.equalsUpper( "DE" ) ) {
	  putCode( 0x19 );
	}
	else if( a2.equalsUpper( "HL" ) ) {
	  putCode( 0x29 );
	}
	else if( a2.equalsUpper( "SP" ) ) {
	  putCode( 0x39 );
	} else {
	  throwNoSuchInstArgs();
	}
      }
      else if( a1.equalsUpper( "IX" ) ) {
	if( a2.equalsUpper( "BC" ) ) {
	  putCode( 0xDD );
	  putCode( 0x09 );
	}
	else if( a2.equalsUpper( "DE" ) ) {
	  putCode( 0xDD );
	  putCode( 0x19 );
	}
	else if( a2.equalsUpper( "SP" ) ) {
	  putCode( 0xDD );
	  putCode( 0x39 );
	}
	else if( a2.equalsUpper( "IX" ) ) {
	  putCode( 0xDD );
	  putCode( 0x29 );
	} else {
	  throwNoSuchInstArgs();
	}
      }
      else if( a1.equalsUpper( "IY" ) ) {
	if( a2.equalsUpper( "BC" ) ) {
	  putCode( 0xFD );
	  putCode( 0x09 );
	}
	else if( a2.equalsUpper( "DE" ) ) {
	  putCode( 0xFD );
	  putCode( 0x19 );
	}
	else if( a2.equalsUpper( "SP" ) ) {
	  putCode( 0xFD );
	  putCode( 0x39 );
	}
	else if( a2.equalsUpper( "IY" ) ) {
	  putCode( 0xFD );
	  putCode( 0x29 );
	} else {
	  throwNoSuchInstArgs();
	}
      } else {
	parseBiOp8( a1, a2, 0x80 );
      }
    } else {
      parseBiOp8( a1, 0x80 );
    }
  }


  private void parseADC_SBC(
			AsmLine asmLine,
			int     baseCode8,
			int     baseCode16 ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.equalsUpper( "HL" ) ) {
	if( a2.equalsUpper( "BC" ) ) {
	  putCode( 0xED );
	  putCode( baseCode16 );
	}
	else if( a2.equalsUpper( "DE" ) ) {
	  putCode( 0xED );
	  putCode( baseCode16 + 0x10 );
	}
	else if( a2.equalsUpper( "HL" ) ) {
	  putCode( 0xED );
	  putCode( baseCode16 + 0x20 );
	}
	else if( a2.equalsUpper( "SP" ) ) {
	  putCode( 0xED );
	  putCode( baseCode16 + 0x30 );
	} else {
	  throwNoSuchInstArgs();
	}
      } else {
	parseBiOp8( a1, a2, baseCode8 );
      }
    } else {
      parseBiOp8( a1, baseCode8 );
    }
  }


  private void parseALIGN( AsmLine asmLine ) throws PrgException
  {
    this.suppressLineAddr = true;

    int v = nextWordArg( asmLine );
    if( (v < 1) || (Integer.bitCount( v ) != 1) ) {
      throw new PrgException( "Zweierpotenz (1, 2, 4, 8, %10, %20 usw.)"
				      + " als Argument erwartet" );
    }
    if( asmLine.hasMoreArgs() ) {
      int b = getByte( asmLine.nextArg() );
      if( v > 1 ) {
	--v;
	while( (this.curAddr & v) != 0 ) {
	  putCode( b );
	}
      }
    } else {
      int m = v - 1;
      this.curAddr = (this.curAddr + m) & ~m;
      checkAddr();
    }
    asmLine.checkEOL();
  }


  private void parseBINCLUDE( AsmLine asmLine ) throws PrgException
  {
    File file = getIncludeFile( asmLine );
    asmLine.checkEOL();
    byte[] fileBytes = this.file2Bytes.get( file );
    try {
      if( fileBytes == null ) {
	fileBytes = FileUtil.readFile( file, false, 0x10000 );
	this.file2Bytes.put( file, fileBytes );
      }
      if( fileBytes == null ) {
	throw new IOException();
      }
      for( byte b : fileBytes ) {
	putCode( b );
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.trim().isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = "Datei kann nicht ge\u00F6ffnet werden.";
      }
      throw new PrgException( msg );
    }
  }


  private void parseCALL( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.equalsUpper( "NZ" ) ) {
	zilogSyntax();
	putCode( 0xC4 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "Z" ) ) {
	zilogSyntax();
	putCode( 0xCC );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "NC" ) ) {
	zilogSyntax();
	putCode( 0xD4 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "C" ) ) {
	zilogSyntax();
	putCode( 0xDC );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "PO" ) ) {
	zilogSyntax();
	putCode( 0xE4 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "PE" ) ) {
	zilogSyntax();
	putCode( 0xEC );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "P" ) ) {
	zilogSyntax();
	putCode( 0xF4 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "M" ) ) {
	zilogSyntax();
	putCode( 0xFC );
	putWord( getWord( a2 ) );
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      putCode( 0xCD );
      putWord( getWord( a1 ) );
    }
  }


  private void parseCPU( AsmLine asmLine ) throws PrgException
  {
    boolean done = false;
    String  text = stripEnclosedText(
				asmLine.nextArg().toString(),
				"CPU-Typ" );
    if( text != null ) {
      text = text.toUpperCase();
      if( text.equals( "U880" ) ) {
	parseCPU( asmLine, Syntax.ROBOTRON_ONLY, false );
	done = true;
      }
      else if( text.equals( "U880UNDOC" ) ) {
	parseCPU( asmLine, Syntax.ROBOTRON_ONLY, true );
	done = true;
      }
      else if( text.equals( "Z80" ) ) {
	parseCPU( asmLine, Syntax.ZILOG_ONLY, false );
	done = true;
      }
      else if( text.equals( "Z80UNDOC" ) ) {
	parseCPU( asmLine, Syntax.ZILOG_ONLY, true );
	done = true;
      }
    }
    if( !done ) {
      throw new PrgException( "\'Z80\', \'Z80UNDOC\',"
			+ " \'U880\' oder \'U880UNDOC\' erwartet" );
    }
  }


  private void parseCPU(
		AsmLine asmLine,
		Syntax  syntax,
		boolean allowUndocInst ) throws PrgException
  {
    if( passNum == 1 ) {
      if( this.cpuDone ) {
	throw new PrgException(
		"Mehrfaches Festlegen des CPU-Modells nicht erlaubt" );
      }
      this.cpuDone = true;
      this.options = new PrgOptions( this.options );
      this.options.setAsmSyntax( syntax );
      this.options.setAllowUndocInst( allowUndocInst );
    }
    asmLine.checkEOL();
  }


  private void parseDEFB( AsmLine asmLine ) throws PrgException
  {
    do {
      String text = asmLine.nextArg().toString();
      if( text != null ) {
	int len = text.length();
	if( len > 0 ) {
	  char ch0 = text.charAt( 0 );
	  if( (ch0 == '\'') || (ch0 == '\"') ) {
	    if( text.indexOf( ch0, 1 ) == 2 ) {
	      putChar( text.charAt( 1 ) );
	    } else {
	      int  pos = 1;
	      char ch  = (char) 0;
	      while( pos < len ) {
		ch = text.charAt( pos++ );
		if( ch == ch0 ) {
		  break;
		}
		putChar( ch );
	      }
	      if( ch != ch0 ) {
		throw new PrgException( "Zeichenkette nicht geschlossen" );
	      }
	      if( pos < len ) {
		throw new PrgException( "\'" + text.charAt( pos )
			  + "\': Unerwartetes Zeichen hinter Zeichenkette" );
	      }
	    }
	  } else {
	    putCode( getByte( text ) );
	  }
	}
      }
    } while( asmLine.hasMoreArgs() );
  }


  private void parseDEFH( AsmLine asmLine ) throws PrgException
  {
    do {
      String text = asmLine.nextArg().toUpperString();
      if( text != null ) {
	if( text.endsWith( "H" ) ) {
	  text = text.substring( 0, text.length() - 1 );
	}
	try {
	  int v = Integer.parseInt( text, 16 );
	  if( (v < ~0x7F) || (v > 0xFF) ) {
	    putWarningOutOf8Bits();
	  }
	  putCode( v );
	}
	catch( NumberFormatException ex ) {
	  throw new PrgException( "\'" + text
			+ "\': Ung\u00FCltige Hexadezimalzahl" );
	}
      }
    } while( asmLine.hasMoreArgs() );
  }


  private void parseDEFS( AsmLine asmLine ) throws PrgException
  {
    do {
      int nBytes = nextWordArg( asmLine );
      if( nBytes > 0 ) {
	String labelName = asmLine.getLabel();
	if( labelName != null ) {
	  AsmLabel label = this.labels.get( labelName );
	  if( label != null ) {
	    if( label.getVarSize() <= 0 ) {
	      label.setVarSize( nBytes );
	    }
	  }
	}
      }
      skipCode( nBytes );
    } while( asmLine.hasMoreArgs() );
  }


  private void parseDEFW( AsmLine asmLine ) throws PrgException
  {
    do {
      putWord( nextWordArg( asmLine ) );
    } while( asmLine.hasMoreArgs() );
  }


  private void parseELSE( AsmLine asmLine ) throws PrgException
  {
    if( this.stack.isEmpty() ) {
      throw new PrgException( "ELSE ohne zugeh\u00F6riges IF..." );
    }
    try {
      this.stack.peek().processELSE();
    }
    catch( EmptyStackException ex ) {}
    asmLine.checkEOL();
  }


  private void parseEND( AsmLine asmLine ) throws PrgException
  {
    if( this.curSource != null ) {
      this.curSource.setEOF();
    }
    asmLine.checkEOL();
  }


  private void parseENDIF( AsmLine asmLine ) throws PrgException
  {
    if( this.stack.isEmpty() ) {
      throw new PrgException( "ENDIF ohne zugeh\u00F6riges IF..." );
    }
    try {
      this.stack.pop();
    }
    catch( EmptyStackException ex ) {}
    asmLine.checkEOL();
  }


  private void parseENTRY( AsmLine asmLine ) throws PrgException
  {
    if( this.passNum == 1 ) {
      if( this.entryAddr != null ) {
	throw new PrgException(
		"Mehrfache ENT- bzw. ENTRY-Anweisungen nicht erlaubt" );
      }
      this.entryAddr = this.curAddr;
    }
    asmLine.checkEOL();
  }


  private void parseEQU( AsmLine asmLine ) throws PrgException
  {
    this.suppressLineAddr = true;

    String labelName = asmLine.getLabel();
    if( labelName != null ) {
      /*
       * Die Marke wurde evtl. bereits beim Parsen der Zeile erkannt und
       * der Markentabelle mit dem Wert der aktuellen Adresse hinzugefuegt.
       * Hier muss nur der Wert korrigiert werden.
       * Waehrend der Berechnung des Werts wird die Marke
       * von der Markentabelle entfernt,
       * damit es zu keiner Falschberechnung kommt,
       * falls die Marke selbst im Ausdruck steht.
       */
      AsmLabel label = this.labels.remove( labelName );
      Object   value = null;
      try {
	String  argText = asmLine.nextArg().toString();

	value = ExprParser.parse(
				argText,
				this.instBegAddr,
				this.labels,
				this.passNum == 2,
				this.options.getLabelsCaseSensitive() );
	if( (value == null) && (this.passNum == 1) ) {
	  /*
	   * Wenn im Lauf 1 der Wert nicht errechnet werden kann,
	   * dann den Text des Ausdrucks speichern,
	   * um die Berechnung spaeter erneut zu versuchen.
	   */
	  value = argText;
	}
      }
      finally {
	switch( this.passNum ) {
	  case 1:
	    label.setLabelValue( value, false );
	    break;
	  case 2:
	    if( (value != null) && !label.hasIntValue() ) {
	      label.setLabelValue( value, false );
	    }
	    break;
	}
	this.labels.put( labelName, label );
      }
      asmLine.checkEOL();
    } else {
      throw new PrgException( "EQU ohne Marke" );
    }
  }


  private void parseERROR( AsmLine asmLine ) throws PrgException
  {
    String msg = stripEnclosedText(
				asmLine.nextArg().toString(),
				"Meldungstext" );
    if( msg == null ) {
      throw new PrgException( "Meldungstext erwartet" );
    }
    asmLine.checkEOL();
    throw new PrgException( msg );
  }


  private void parseEVEN( AsmLine asmLine ) throws PrgException
  {
    this.suppressLineAddr = true;
    asmLine.checkEOL();
    if( (this.curAddr & 0x0001) != 0 ) {
      skipCode( 1 );
    }
  }


  private void parseEX( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    AsmArg a2 = asmLine.nextArg();
    asmLine.checkEOL();
    if( a1.isIndirectSP() ) {
      if( a2.equalsUpper( "HL" ) ) {
	putCode( 0xE3 );
      }
      else if( a2.equalsUpper( "IX" ) ) {
	putCode( 0xDD );
	putCode( 0xE3 );
      }
      else if( a2.equalsUpper( "IY" ) ) {
	putCode( 0xFD );
	putCode( 0xE3 );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.equalsUpper( "AF" ) ) {
      if( a2.equalsUpper( "AF\'" ) ) {
	zilogSyntax();
	putCode( 0x08 );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.equalsUpper( "DE" ) ) {
      if( a2.equalsUpper( "HL" ) ) {
	putCode( 0xEB );
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseIF(
		AsmLine asmLine,
		boolean condValue ) throws
					PrgException,
					TooManyErrorsException
  {
    boolean state = false;
    Integer value = ExprParser.parse(
			asmLine.nextArg().toString(),
			this.instBegAddr,
			this.labels,
			true,
			this.options.getLabelsCaseSensitive() );
    if( value != null ) {
      if( condValue == (value.intValue() != 0) ) {
	state = true;
      }
    } else {
      /*
       * Fehler hier ausgeben und keine Exception werfen,
       * damit bei ELSEIF, ELSE oder ENDIF
       * kein Strukturfehler auftritt
       */
      putError( "Wert nicht ermittelbar"
			+ " (Vorw\u00E4rtsreferenzen"
			+ " in Marken an der Stelle nicht erlaubt)" );
    }
    this.stack.push(
		new AsmStackEntry( this.curSource.getLineNum(), state ) );
    asmLine.checkEOL();
  }


  private void parseIFDEF( AsmLine asmLine ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    this.stack.push(
	new AsmStackEntry(
		this.curSource.getLineNum(),
		this.labels.containsKey(
			this.options.getLabelsCaseSensitive() ?
						a.toString()
						: a.toUpperString() ) ) );
    asmLine.checkEOL();
  }


  private void parseIFNDEF( AsmLine asmLine ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    this.stack.push(
	new AsmStackEntry(
		this.curSource.getLineNum(),
		!this.labels.containsKey(
			this.options.getLabelsCaseSensitive() ?
						a.toString()
						: a.toUpperString() ) ) );
    asmLine.checkEOL();
  }


  private void parseIM( AsmLine asmLine ) throws PrgException
  {
    String s = asmLine.nextArg().toString();
    asmLine.checkEOL();
    if( s.equals( "0" ) ) {
      putCode( 0xED );
      putCode( 0x46 );
    }
    else if( s.equals( "1" ) ) {
      putCode( 0xED );
      putCode( 0x56 );
    }
    else if( s.equals( "2" ) ) {
      putCode( 0xED );
      putCode( 0x5E );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseIN( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( a1.equalsUpper( "A" ) ) {
      if( asmLine.hasMoreArgs() ) {
	AsmArg a2 = asmLine.nextArg();
	if( a2.isIndirectAddr() ) {
	  putCode( 0xDB );
	  putCode( getByte( a2.getIndirectText() ) );
	}
	else if( a2.equalsUpper( "(C)" ) ) {
	  putCode( 0xED );
	  putCode( 0x78 );
	} else {
	  throwNoSuchInstArgs();
	}
	asmLine.checkEOL();
	zilogSyntax();
      } else {
	putCode( 0xED );
	putCode( 0x78 );
	robotronSyntax();
      }
    }
    else if( a1.isRegBtoL() ) {
      if( asmLine.hasMoreArgs() ) {
	if( !asmLine.nextArg().equalsUpper( "(C)" ) ) {
	  throwNoSuchInstArgs();
	}
	asmLine.checkEOL();
	zilogSyntax();
      } else {
	robotronSyntax();
      }
      putCode( 0xED );
      putCode( 0x40 | (a1.getReg8Code() << 3) );
    }
    else if( a1.equalsUpper( "F" ) ) {
      if( asmLine.hasMoreArgs() ) {
	if( !asmLine.nextArg().equalsUpper( "(C)" ) ) {
	  throwNoSuchInstArgs();
	}
	asmLine.checkEOL();
      }
      putCode( 0xED );
      putCode( 0x70 );
      undocInst();
    }
    else if( a1.equalsUpper( "(C)" ) ) {
      if( asmLine.hasMoreArgs() ) {
	throwNoSuchInstArgs();
      }
      putCode( 0xED );
      putCode( 0x70 );
      undocInst();
    } else {
      if( asmLine.hasMoreArgs() ) {
	throwNoSuchInstArgs();
      }
      putCode( 0xDB );
      putCode( getByte( a1 ) );
      robotronSyntax();
    }
  }


  private void parseINC_DEC(
			AsmLine asmLine,
			int     baseCode8,
			int     baseCode16 ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    asmLine.checkEOL();
    if( a.equalsUpper( "BC" ) ) {
      putCode( baseCode16 );
    }
    else if( a.equalsUpper( "DE" ) ) {
      putCode( baseCode16 + 0x10 );
    }
    else if( a.equalsUpper( "HL" ) ) {
      putCode( baseCode16 + 0x20 );
    }
    else if( a.equalsUpper( "SP" ) ) {
      putCode( baseCode16 + 0x30 );
    }
    else if( a.equalsUpper( "IX" ) ) {
      putCode( 0xDD );
      putCode( baseCode16 + 0x20 );
    }
    else if( a.equalsUpper( "IY" ) ) {
      putCode( 0xFD );
      putCode( baseCode16 + 0x20 );
    }
    else if( a.isRegAtoL() ) {
      putCode( baseCode8 | (a.getReg8Code() << 3) );
    }
    else if( a.isIndirectHL() ) {
      putCode( baseCode8 + 0x30 );
    }
    else if( a.equalsUpper( "M" ) ) {
      putCode( baseCode8 + 0x30 );
      robotronSyntax();
    }
    else if( a.isIndirectIXDist() ) {
      putCode( 0xDD );
      putCode( baseCode8 + 0x30 );
      putCode( getIndirectIXYDist( a ) );
    }
    else if( a.isIndirectIYDist() ) {
      putCode( 0xFD );
      putCode( baseCode8 + 0x30 );
      putCode( getIndirectIXYDist( a ) );
    }
    else if( a.equalsUpper( "IXH" ) || a.equalsUpper( "HX" ) ) {
      putCode( 0xDD );
      putCode( baseCode8 + 0x20 );
      undocInst();
    }
    else if( a.equalsUpper( "IXL" ) || a.equalsUpper( "LX" ) ) {
      putCode( 0xDD );
      putCode( baseCode8 + 0x28 );
      undocInst();
    }
    else if( a.equalsUpper( "IYH" ) || a.equalsUpper( "HY" ) ) {
      putCode( 0xFD );
      putCode( baseCode8 + 0x20 );
      undocInst();
    }
    else if( a.equalsUpper( "IYL" ) || a.equalsUpper( "LY" ) ) {
      putCode( 0xFD );
      putCode( baseCode8 + 0x28 );
      undocInst();
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseINCLUDE( AsmLine asmLine ) throws PrgException
  {
    File file = getIncludeFile( asmLine );
    if( this.curSource != this.mainSource ) {
      throw new PrgException(
		"In sich geschachtelte INCLUDE-Befehle nicht erlaubt" );
    }
    asmLine.checkEOL();
    PrgSource source = this.file2Source.get( file );
    if( source != null ) {
      source.reset();
      this.curSource = source;
    } else {
      try {
	source = PrgSource.readFile( file );
	this.file2Source.put( file, source );
	this.curSource = source;
      }
      catch( IOException ex ) {
	String msg = ex.getMessage();
	if( msg != null ) {
	  if( msg.trim().isEmpty() ) {
	    msg = null;
	  }
	}
	if( msg == null ) {
	  msg = "Datei kann nicht ge\u00F6ffnet werden.";
	}
	throw new PrgException( msg );
      }
    }
  }


  private void parseJMP( AsmLine asmLine ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    asmLine.checkEOL();
    if( a.isIndirectHL() ) {
      putCode( 0xE9 );
    }
    else if( a.isIndirectIX() ) {
      putCode( 0xDD );
      putCode( 0xE9 );
    }
    else if( a.isIndirectIY() ) {
      putCode( 0xFD );
      putCode( 0xE9 );
    } else {
      putCode( 0xC3 );
      putWord( getWord( a ) );
    }
  }


  private void parseJP( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.equalsUpper( "NZ" ) ) {
	zilogSyntax();
	putCode( 0xC2 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "Z" ) ) {
	zilogSyntax();
	putCode( 0xCA );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "NC" ) ) {
	zilogSyntax();
	putCode( 0xD2 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "C" ) ) {
	zilogSyntax();
	putCode( 0xDA );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "PO" ) ) {
	zilogSyntax();
	putCode( 0xE2 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "PE" ) ) {
	zilogSyntax();
	putCode( 0xEA );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "P" ) ) {
	zilogSyntax();
	putCode( 0xF2 );
	putWord( getWord( a2 ) );
      }
      else if( a1.equalsUpper( "M" ) ) {
	zilogSyntax();
	putCode( 0xFA );
	putWord( getWord( a2 ) );
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      if( a1.isIndirectHL() ) {
	putCode( 0xE9 );
      }
      else if( a1.isIndirectIX() ) {
	putCode( 0xDD );
	putCode( 0xE9 );
      }
      else if( a1.isIndirectIY() ) {
	putCode( 0xFD );
	putCode( 0xE9 );
      } else {
	putCode( 0xC3 );
	putWord( getWord( a1 ) );
      }
    }
  }


  private void parseJR( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.equalsUpper( "NZ" ) ) {
	int d = getAddrDiff( asmLine, a2 );
	asmLine.checkEOL();
	putCode( 0x20 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "Z" ) ) {
	int d = getAddrDiff( asmLine, a2 );
	asmLine.checkEOL();
	putCode( 0x28 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "NC" ) ) {
	int d = getAddrDiff( asmLine, a2 );
	asmLine.checkEOL();
	putCode( 0x30 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "C" ) ) {
	int d = getAddrDiff( asmLine, a2 );
	asmLine.checkEOL();
	putCode( 0x38 );
	putCode( d );
	zilogSyntax();
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      int d = getAddrDiff( asmLine, a1 );
      putCode( 0x18 );
      putCode( d );
    }
  }


  private void parseLD( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    AsmArg a2 = asmLine.nextArg();
    asmLine.checkEOL();

    if( a1.equalsUpper( "A" ) ) {
      if( a2.isRegAtoL() ) {
	putCode( 0x78 | a2.getReg8Code() );
      }
      else if( a2.isIndirectHL() ) {
	putCode( 0x7E );
      }
      else if( a2.equalsUpper( "M" ) ) {
	robotronSyntax();
	putCode( 0x7E );
      }
      else if( a2.isIndirectBC() ) {
	putCode( 0x0A );
      }
      else if( a2.isIndirectDE() ) {
	putCode( 0x1A );
      }
      else if( a2.isIndirectAddr() ) {
	putCode( 0x3A );
	putWord( getWord( a2.getIndirectText() ) );
      }
      else if( a2.isIndirectIXDist() ) {
	putCode( 0xDD );
	putCode( 0x7E );
	putCode( getIndirectIXYDist( a2 ) );
      }
      else if( a2.isIndirectIYDist() ) {
	putCode( 0xFD );
	putCode( 0x7E );
	putCode( getIndirectIXYDist( a2 ) );
      }
      else if( a2.equalsUpper( "I" ) ){
	putCode( 0xED );
	putCode( 0x57 );
      }
      else if( a2.equalsUpper( "R" ) ){
	putCode( 0xED );
	putCode( 0x5F );
      }
      else if( a2.equalsUpper( "IXH" ) || a2.equalsUpper( "HX" ) ) {
	putCode( 0xDD );
	putCode( 0x7C );
      }
      else if( a2.equalsUpper( "IXL" ) || a2.equalsUpper( "LX" ) ) {
	putCode( 0xDD );
	putCode( 0x7D );
      }
      else if( a2.equalsUpper( "IYH" ) || a2.equalsUpper( "HY" ) ) {
	putCode( 0xFD );
	putCode( 0x7C );
      }
      else if( a2.equalsUpper( "IYL" ) || a2.equalsUpper( "LY" ) ) {
	putCode( 0xFD );
	putCode( 0x7D );
      } else {
	putCode( 0x3E );
	putCode( getByte( a2 ) );
      }
    }
    else if( a1.isRegBtoL() ) {
      if( a2.isRegAtoL() ) {
	putCode( 0x40 | (a1.getReg8Code() << 3) | a2.getReg8Code() );
      }
      else if( a2.isIndirectHL() ) {
	putCode( 0x46 | (a1.getReg8Code() << 3) );
      }
      else if( a2.equalsUpper( "M" ) ) {
	robotronSyntax();
	putCode( 0x46 | (a1.getReg8Code() << 3) );
      }
      else if( a2.isIndirectIXDist() ) {
	putCode( 0xDD );
	putCode( 0x46 | (a1.getReg8Code() << 3) );
	putCode( getIndirectIXYDist( a2 ) );
      }
      else if( a2.isIndirectIYDist() ) {
	putCode( 0xFD );
	putCode( 0x46 | (a1.getReg8Code() << 3) );
	putCode( getIndirectIXYDist( a2 ) );
      }
      else if( a2.equalsUpper( "IXH" ) || a2.equalsUpper( "HX" ) ) {
	putCode( 0xDD );
	putCode( 0x44 | (a1.getReg8Code() << 3) );
	undocInst();
      }
      else if( a2.equalsUpper( "IXL" ) || a2.equalsUpper( "LX" ) ) {
	putCode( 0xDD );
	putCode( 0x45 | (a1.getReg8Code() << 3) );
	undocInst();
      }
      else if( a2.equalsUpper( "IYH" ) || a2.equalsUpper( "HY" ) ) {
	putCode( 0xFD );
	putCode( 0x44 | (a1.getReg8Code() << 3) );
	undocInst();
      }
      else if( a2.equalsUpper( "IYL" ) || a2.equalsUpper( "LY" ) ) {
	putCode( 0xFD );
	putCode( 0x45 | (a1.getReg8Code() << 3) );
	undocInst();
      } else {
	putCode( 0x06 | a1.getReg8Code() << 3 );
	putCode( getByte( a2 ) );
      }
    }
    else if( a1.isIndirectHL() ) {
      if( a2.isRegAtoL() ) {
	putCode( 0x70 | a2.getReg8Code() );
      } else {
	putCode( 0x36 );
	putCode( getByte( a2 ) );
      }
    }
    else if( a1.equalsUpper( "M" ) ) {
      robotronSyntax();
      if( a2.isRegAtoL() ) {
	putCode( 0x70 | a2.getReg8Code() );
      } else {
	putCode( 0x36 );
	putCode( getByte( a2 ) );
      }
    }
    else if( a1.isIndirectBC() ) {
      if( a2.equalsUpper( "A" ) ) {
	putCode( 0x02 );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.isIndirectDE() ) {
      if( a2.equalsUpper( "A" ) ) {
	putCode( 0x12 );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.isIndirectAddr() ) {
      if( a2.equalsUpper( "A" ) ) {
	putCode( 0x32 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "BC" ) ) {
	putCode( 0xED );
	putCode( 0x43 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "DE" ) ) {
	putCode( 0xED );
	putCode( 0x53 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "HL" ) ) {
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "SP" ) ) {
	putCode( 0xED );
	putCode( 0x73 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "IX" ) ) {
	putCode( 0xDD );
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "IY" ) ) {
	putCode( 0xFD );
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.isIndirectIXDist() ) {
      parseLD8_IXYD_X( a1, a2, 0xDD );
    }
    else if( a1.isIndirectIYDist() ) {
      parseLD8_IXYD_X( a1, a2, 0xFD );
    }
    else if( a1.equalsUpper( "I" ) && a2.equalsUpper( "A" ) ) {
      putCode( 0xED );
      putCode( 0x47 );
    }
    else if( a1.equalsUpper( "R" ) && a2.equalsUpper( "A" ) ) {
      putCode( 0xED );
      putCode( 0x4F );
    }
    else if( a1.equalsUpper( "BC" ) ) {
      parseLD16_BCDE_X( a1, a2, 0x00 );
    }
    else if( a1.equalsUpper( "DE" ) ) {
      parseLD16_BCDE_X( a1, a2, 0x10 );
    }
    else if( a1.equalsUpper( "HL" ) ) {
      parseLD16_HL_X( a1, a2, -1 );
    }
    else if( a1.equalsUpper( "IX" ) ) {
      parseLD16_HL_X( a1, a2, 0xDD );
    }
    else if( a1.equalsUpper( "IY" ) ) {
      parseLD16_HL_X( a1, a2, 0xFD );
    }
    else if( a1.equalsUpper( "SP" ) ) {
      if( a2.equalsUpper( "HL" ) ) {
	putCode( 0xF9 );
      }
      else if( a2.equalsUpper( "IX" ) ) {
	putCode( 0xDD );
	putCode( 0xF9 );
      }
      else if( a2.equalsUpper( "IY" ) ) {
	putCode( 0xFD );
	putCode( 0xF9 );
      }
      else if( a2.isIndirectAddr() ) {
	putCode( 0xED );
	putCode( 0x7B );
	putWord( getWord( a2.getIndirectText() ) );
      } else {
	putCode( 0x31 );
	putWord( getWord( a2 ) );
      }
    }
    else if( a1.isIndirectAddr() ) {
      if( a2.equalsUpper( "BC" ) ) {
	putCode( 0xED );
	putCode( 0x43 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "DE" ) ) {
	putCode( 0xED );
	putCode( 0x53 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "HL" ) ) {
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "SP" ) ) {
	putCode( 0xED );
	putCode( 0x73 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "IX" ) ) {
	putCode( 0xDD );
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      }
      else if( a2.equalsUpper( "IY" ) ) {
	putCode( 0xFD );
	putCode( 0x22 );
	putWord( getWord( a1.getIndirectText() ) );
      } else {
	throwNoSuchInstArgs();
      }
    }
    else if( a1.equalsUpper( "IXH" ) || a1.equalsUpper( "HX" ) ) {
      parseLD_IXY8_X( a2, 0xDD, 0x00 );
    }
    else if( a1.equalsUpper( "IXL" ) || a1.equalsUpper( "LX" ) ) {
      parseLD_IXY8_X( a2, 0xDD, 0x08 );
    }
    else if( a1.equalsUpper( "IYH" ) || a1.equalsUpper( "HY" ) ) {
      parseLD_IXY8_X( a2, 0xFD, 0x00 );
    }
    else if( a1.equalsUpper( "IYL" ) || a1.equalsUpper( "LY" ) ) {
      parseLD_IXY8_X( a2, 0xFD, 0x08 );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseLD8_IXYD_X(
			AsmArg a1,
			AsmArg a2,
			int    preCode ) throws PrgException
  {
    putCode( preCode );
    if( a2.isRegAtoL() ) {
      putCode( 0x70 | a2.getReg8Code() );
      putCode( getIndirectIXYDist( a1 ) );
    } else {
      putCode( 0x36 );
      putCode( getIndirectIXYDist( a1 ) );
      putCode( getByte( a2 ) );
    }
  }


  private void parseLD16_BCDE_X(
			AsmArg a1,
			AsmArg a2,
			int    codeDiff ) throws PrgException
  {
    if( a2.isIndirectAddr() ) {
      int w = getWord( a2.getIndirectText() );
      putCode( 0xED );
      putCode( 0x4B + codeDiff );
      putWord( w );
    } else {
      putCode( 0x01 + codeDiff );
      putWord( getWord( a2 ) );
    }
  }


  private void parseLD16_HL_X(
			AsmArg a1,
			AsmArg a2,
			int    preCode ) throws PrgException
  {
    if( preCode >= 0 ) {
      putCode( preCode );
    }
    if( a2.isIndirectAddr() ) {
      putCode( 0x2A );
      putWord( getWord( a2.getIndirectText() ) );
    } else {
      putCode( 0x21 );
      putWord( getWord( a2 ) );
    }
  }


  private void parseLD_IXY8_X(
			AsmArg a2,
			int    preCode,
			int    baseCode ) throws PrgException
  {
    if( a2.equalsUpper( "A" )
	|| a2.equalsUpper( "B" )
	|| a2.equalsUpper( "C" )
	|| a2.equalsUpper( "D" )
	|| a2.equalsUpper( "E" ) )
    {
      putCode( preCode );
      putCode( 0x60 + baseCode + a2.getReg8Code() );
    }
    else if( ((preCode == 0xDD)
		&& (a2.equalsUpper( "IXH" ) || a2.equalsUpper( "HX" )))
	     || ((preCode == 0xFD)
		&& (a2.equalsUpper( "IYH" ) || a2.equalsUpper( "HY" ))) )
    {
      putCode( preCode );
      putCode( 0x64 + baseCode );
    }
    else if( ((preCode == 0xDD)
		&& (a2.equalsUpper( "IXL" ) || a2.equalsUpper( "LX" )))
	     || ((preCode == 0xFD)
		&& (a2.equalsUpper( "IYL" ) || a2.equalsUpper( "LY" ))) )
    {
      putCode( preCode );
      putCode( 0x65 + baseCode );
    } else {
      putCode( preCode );
      putCode( 0x26 + baseCode );
      putCode( getByte( a2 ) );
    }
    undocInst();
  }


  private void parseORG( AsmLine asmLine ) throws PrgException
  {
    this.suppressLineAddr = true;

    int a = nextWordArg( asmLine );
    asmLine.checkEOL();
    if( a < this.curAddr ) {
      this.orgOverlapped = true;
      throw new PrgException( "Zur\u00FCcksetzen des"
			+ " Adressz\u00E4hlers nicht erlaubt" );
    }
    if( (this.curAddr > 0) && (a > this.curAddr) ) {
      skipCode( a - this.curAddr );
    }
    this.curAddr = a;
  }


  private void parseNAME( AsmLine asmLine ) throws PrgException
  {
    String appName = stripEnclosedText(
				asmLine.nextArg().toString(),
				"Name/Titel" );
    if( appName == null ) {
      throw new PrgException( "Programmname bzw. Titel erwartet" );
    }
    if( this.passNum == 1 ) {
      if( this.appName != null ) {
	throw new PrgException( "Mehrfaches Festlegen"
			+ " des Programmnames bzw. Titels nicht erlaubt" );
      }
      this.appName = appName;
    }
    asmLine.checkEOL();
  }


  private void parseOUT( AsmLine asmLine ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( a1.equalsUpper( "(C)" ) ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a2.isRegAtoL() ) {
	putCode( 0xED );
	putCode( 0x41 | (a2.getReg8Code() << 3) );
      } else {
	throwNoSuchInstArgs();
      }
      zilogSyntax();
    }
    else if( a1.isRegAtoL() ) {
      asmLine.checkEOL();
      putCode( 0xED );
      putCode( 0x41 | (a1.getReg8Code() << 3) );
      robotronSyntax();
    }
    else if( a1.isIndirectAddr() ) {
      if( asmLine.hasMoreArgs() ) {
	AsmArg a2 = asmLine.nextArg();
	asmLine.checkEOL();
	if( !a2.equalsUpper( "A" ) ) {
	  throwNoSuchInstArgs();
	}
	putCode( 0xD3 );
	putCode( getByte( a1.getIndirectText() ) );
	zilogSyntax();
      } else {
	putCode( 0xD3 );
	putCode( getByte( a1.getIndirectText() ) );
	undocSyntax();
      }
    } else {
      asmLine.checkEOL();
      putCode( 0xD3 );
      putCode( getByte( a1 ) );
      robotronSyntax();
    }
  }


  private void parsePUSH_POP(
			AsmLine asmLine,
			int     baseCode ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    asmLine.checkEOL();
    if( a.equalsUpper( "BC" ) ) {
      putCode( baseCode );
    }
    else if( a.equalsUpper( "DE" ) ) {
      putCode( baseCode + 0x10 );
    }
    else if( a.equalsUpper( "HL" ) ) {
      putCode( baseCode + 0x20 );
    }
    else if( a.equalsUpper( "AF" ) ) {
      putCode( baseCode + 0x30 );
    }
    else if( a.equalsUpper( "IX" ) ) {
      putCode( 0xDD );
      putCode( baseCode + 0x20 );
    }
    else if( a.equalsUpper( "IY" ) ) {
      putCode( 0xFD );
      putCode( baseCode + 0x20 );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseRET( AsmLine asmLine ) throws PrgException
  {
    if( asmLine.hasMoreArgs() ) {
      AsmArg a = asmLine.nextArg();
      asmLine.checkEOL();
      if( a.equalsUpper( "NZ" ) ) {
	zilogSyntax();
	putCode( 0xC0 );
      }
      else if( a.equalsUpper( "Z" ) ) {
	zilogSyntax();
	putCode( 0xC8 );
      }
      else if( a.equalsUpper( "NC" ) ) {
	zilogSyntax();
	putCode( 0xD0 );
      }
      else if( a.equalsUpper( "C" ) ) {
	zilogSyntax();
	putCode( 0xD8 );
      }
      else if( a.equalsUpper( "PO" ) ) {
	zilogSyntax();
	putCode( 0xE0 );
      }
      else if( a.equalsUpper( "PE" ) ) {
	zilogSyntax();
	putCode( 0xE8 );
      }
      else if( a.equalsUpper( "P" ) ) {
	zilogSyntax();
	putCode( 0xF0 );
      }
      else if( a.equalsUpper( "M" ) ) {
	zilogSyntax();
	putCode( 0xF8 );
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      putCode( 0xC9 );
    }
  }


  private void parseRXD( AsmLine asmLine, int baseCode ) throws PrgException
  {
    if( asmLine.hasMoreArgs() ) {
      AsmArg a = asmLine.nextArg();
      asmLine.checkEOL();
      if( a.isIndirectHL() || a.equalsUpper( "M" ) ) {
	putCode( 0xED );
	putCode( baseCode );
	undocSyntax();
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      putCode( 0xED );
      putCode( baseCode );
    }
  }


  private void parseRST( AsmLine asmLine ) throws PrgException
  {
    int value = parseExpr( asmLine.nextArg().toString() );
    asmLine.checkEOL();
    if( (value & ~0x38) != 0 ) {
      throwNoSuchInstArgs();
    }
    putCode( 0xC7 | value );
  }


  private void parseBiOp8( AsmLine asmLine, int baseCode ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      parseBiOp8( a1, a2, baseCode );
    } else {
      parseBiOp8( a1, baseCode );
    }
  }


  private void parseBiOp8(
			AsmArg a1,
			AsmArg a2,
			int    baseCode ) throws PrgException
  {
    if( a1.equalsUpper( "A" ) ) {
      parseBiOp8Internal( a2, baseCode );
      if( (baseCode == 0x80)		// ADD
	  || (baseCode == 0x88)		// ADC
	  || (baseCode == 0x98) )	// SBC
      {
	zilogSyntax();
      } else {
	undocSyntax();
      }
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseBiOp8( AsmArg a, int baseCode ) throws PrgException
  {
    parseBiOp8Internal( a, baseCode );
    if( (baseCode == 0x80)		// ADD
	|| (baseCode == 0x88)		// ADC
	|| (baseCode == 0x98) )		// SBC
    {
      robotronSyntax();
    }
  }


  private void parseBiOp8Internal( AsmArg a, int baseCode )
						throws PrgException
  {
    if( a.isRegAtoL() ) {
      putCode( baseCode + a.getReg8Code() );
    }
    else if( a.isIndirectHL() ) {
      putCode( baseCode + 6 );
    }
    else if( a.equalsUpper( "M" ) ) {
      putCode( baseCode + 6 );
      robotronSyntax();
    }
    else if( a.isIndirectIXDist() ) {
      putCode( 0xDD );
      putCode( baseCode + 6 );
      putCode( getIndirectIXYDist( a ) );
    }
    else if( a.isIndirectIYDist() ) {
      putCode( 0xFD );
      putCode( baseCode + 6 );
      putCode( getIndirectIXYDist( a ) );
    }
    else if( a.equalsUpper( "IXH" ) || a.equalsUpper( "HX" ) ) {
      putCode( 0xDD );
      putCode( baseCode + 4 );
      undocInst();
    }
    else if( a.equalsUpper( "IXL" ) || a.equalsUpper( "LX" ) ) {
      putCode( 0xDD );
      putCode( baseCode + 5 );
      undocInst();
    }
    else if( a.equalsUpper( "IYH" ) || a.equalsUpper( "HY" ) ) {
      putCode( 0xFD );
      putCode( baseCode + 4 );
      undocInst();
    }
    else if( a.equalsUpper( "IYL" ) || a.equalsUpper( "LY" ) ) {
      putCode( 0xFD );
      putCode( baseCode + 5 );
      undocInst();
    } else {
      putCode( baseCode + 0x46 );
      putCode( getByte( a ) );
    }
  }


  private void parseInstDirectAddr(
			AsmLine asmLine,
			int     code ) throws PrgException
  {
    int w = nextWordArg( asmLine );
    asmLine.checkEOL();
    putCode( code );
    putWord( w );
  }


  private void parseRotShift(
			AsmLine asmLine,
			int     baseCode ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    if( asmLine.hasMoreArgs() ) {
      AsmArg a2 = asmLine.nextArg();
      asmLine.checkEOL();
      if( a1.isIndirectIXDist() && a2.isRegAtoL() ) {
	putCode( 0xDD );
	putCode( 0xCB );
	putCode( getIndirectIXYDist( a1 ) );
	putCode( baseCode | a2.getReg8Code() );

	// 0x30: SLL -> Warnung bereits an anderer Stelle
	if( baseCode != 0x30 ) {
	  undocInst();
	}
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      if( a1.isRegAtoL() ) {
	putCode( 0xCB );
	putCode( baseCode + a1.getReg8Code() );
      }
      else if( a1.isIndirectHL() ) {
	putCode( 0xCB );
	putCode( baseCode + 6 );
      }
      else if( a1.equalsUpper( "M" ) ) {
	putCode( 0xCB );
	putCode( baseCode + 6 );
	robotronSyntax();
      }
      else if( a1.isIndirectIXDist() ) {
	putCode( 0xDD );
	putCode( 0xCB );
	putCode( getIndirectIXYDist( a1 ) );
	putCode( baseCode + 6 );
      }
      else if( a1.isIndirectIYDist() ) {
	putCode( 0xFD );
	putCode( 0xCB );
	putCode( getIndirectIXYDist( a1 ) );
	putCode( baseCode + 6 );
      } else {
	throwNoSuchInstArgs();
      }
    }
  }


  private void parseSingleBit(
			AsmLine asmLine,
			int     baseCode ) throws PrgException
  {
    AsmArg a1 = asmLine.nextArg();
    AsmArg a2 = asmLine.nextArg();
    AsmArg a3 = null;
    if( asmLine.hasMoreArgs() ) {
      a3 = asmLine.nextArg();
      asmLine.checkEOL();
    }
    int bitCode = (parseExpr( a1.toString() ) << 3);
    if( a3 != null ) {

      // bei SET und RES sind drei Argumente moeglich
      if( ((baseCode == 0x80) || (baseCode == 0xC0)) && a3.isRegAtoL() ) {
	if( a2.isIndirectIXDist() ) {
	  putCode( 0xDD );
	  putCode( 0xCB );
	  putCode( getIndirectIXYDist( a2 ) );
	  putCode( baseCode + a3.getReg8Code() + bitCode );
	  undocInst();
	}
	else if( a2.isIndirectIYDist() ) {
	  putCode( 0xFD );
	  putCode( 0xCB );
	  putCode( getIndirectIXYDist( a2 ) );
	  putCode( baseCode + a3.getReg8Code() + bitCode );
	  undocInst();
	} else {
	  throwNoSuchInstArgs();
	}
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      if( a2.isRegAtoL() ) {
	putCode( 0xCB );
	putCode( baseCode + a2.getReg8Code() + bitCode);
      }
      else if( a2.isIndirectHL() ) {
	putCode( 0xCB );
	putCode( baseCode + 6 + bitCode );
      }
      else if( a2.equalsUpper( "M" ) ) {
	putCode( 0xCB );
	putCode( baseCode + 6 + bitCode );
	robotronSyntax();
      }
      else if( a2.isIndirectIXDist() ) {
	putCode( 0xDD );
	putCode( 0xCB );
	putCode( getIndirectIXYDist( a2 ) );
	putCode( baseCode + 6 + bitCode );
      }
      else if( a2.isIndirectIYDist() ) {
	putCode( 0xFD );
	putCode( 0xCB );
	putCode( getIndirectIXYDist( a2 ) );
	putCode( baseCode + 6 + bitCode );
      } else {
	throwNoSuchInstArgs();
      }
    }
  }


  private void putError( String msg ) throws TooManyErrorsException
  {
    if( msg == null ) {
      msg = "Unbekannter Fehler";
    }
    appendLineNumMsgToErrLog( msg, EmuUtil.TEXT_ERROR );
    this.status = false;
    this.errCnt++;
    if( this.errCnt >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  private int nextWordArg( AsmLine asmLine ) throws PrgException
  {
    return getWord( asmLine.nextArg() );
  }


  /*
   * Wenn die Methode aufgerufen wird,
   * ist der Adresszaehler noch nicht auf den naechsten Befehl gestellt,
   * d.h., der Wert muss um zwei erhoeht werden.
   */
  private int getAddrDiff(
			AsmLine asmLine,
			AsmArg  asmArg ) throws PrgException
  {
    int v = 0;
    if( this.passNum == 2 ) {
      String s = asmArg.toString();
      if( s.endsWith( "-#" ) ) {
	s = s.substring( 0, s.length() - 2 );
      }
      v = getWord( s ) - ((this.curAddr + 2) & 0xFFFF);
      if( (v < ~0x7F) || (v > 0x7F) ) {
	boolean done = false;
	if( (this.curSource != null)
	    && this.options.getReplaceTooLongRelJumps() )
	{
	  int begOfInst = asmLine.getBegOfInstruction();
	  if( begOfInst >= 0 ) {
	    String line = this.curSource.getCurLine();
	    if( line != null ) {
	      StringBuilder buf = new StringBuilder( line );
	      if( (begOfInst + 1) < buf.length() ) {
		if( (Character.toUpperCase( buf.charAt( begOfInst ) ) == 'J')
		    && (Character.toUpperCase(
				buf.charAt( begOfInst + 1 ) ) == 'R') )
		{
		  buf.setCharAt( begOfInst + 1, 'P' );
		  /*
		   * Wenn nur Robotron-Syntax erlaubt ist,
		   * muss bei einem unbedingen relativen Sprung
		   * JR in JMP geaendert werden.
		   * Bei JRC, JRNC, JRZ und JRNZ wird dagegen weiterhin
		   * nur das R durch ein P ersetzt.
		   */
		  if( this.options.getAsmSyntax() == Syntax.ROBOTRON_ONLY ) {
		    String instr = asmLine.getInstruction();
		    if( instr != null ) {
		      if( instr.length() == 2 ) {
			buf.replace( begOfInst, begOfInst + 2, "JMP" );
		      }
		    }
		  }
		  if( this.curSource.replaceCurLine( buf.toString() ) ) {
		    putWarning( "Relativer Sprung wird als absoluter"
			+ " \u00FCbersetzt, da Sprungdistanz zu gro\u00DF" );
		    this.restartAsm = true;
		    done            = true;
		  }
		}
	      }
	    }
	  }
	}
	if( !done ) {
	  this.relJumpsTooLong = true;
	  throw new PrgException( "Relative Sprungdistanz zu gro\u00DF" );
	}
      }
    }
    return v;
  }


  private File getIncludeFile( AsmLine asmLine ) throws PrgException
  {
    String fileName = null;
    String text     = asmLine.nextArg().toString();
    if( text != null ) {
      fileName = stripEnclosedText( text, "Dateiname" );
    }
    if( fileName == null ) {
      throw new PrgException( "Dateiname erwartet" );
    }
    return PrgSource.getIncludeFile( this.curSource, fileName );
  }


  private int getIndirectIXYDist( AsmArg a ) throws PrgException
  {
    int    v    = 0;
    String text = a.getIndirectIXYDist();
    if( text != null ) {
      if( !text.isEmpty() ) {
	v = parseExpr( text );
	if( (v < ~0x7F) || (v > 0xFF) ) {
	  throw new PrgException( "Distanzangabe zu gro\u00DF" );
	}
	if( text.startsWith( "+" ) && (v > 0x7F) ) {
	  putWarning( "Distanz ist negativ (r\u00FCchw\u00E4rts)"
					+ " obwohl positiv angegeben" );
	}
      }
    }
    return v;
  }


  private int getByte( AsmArg asmArg ) throws PrgException
  {
    return getByte( asmArg.toString() );
  }


  private int getByte( String text ) throws PrgException
  {
    int v = parseExpr( text );
    if( (v < ~0x7F) || (v > 0xFF) ) {
      putWarningOutOf8Bits();
    }
    return v;
  }


  private int getWord( AsmArg asmArg ) throws PrgException
  {
    return getWord( asmArg.toString() );
  }


  private int getWord( String text ) throws PrgException
  {
    int v = parseExpr( text );
    checkPrint16BitWarning( v );
    return v;
  }


  private void putChar( char ch ) throws PrgException
  {
    if( ch > 0xFF ) {
      throw new PrgException(
		String.format(
			"\'%c\': 16-Bit-Unicodezeichen nicht erlaubt",
			ch ) );
    }
    if( this.options.getWarnNonAsciiChars()
	&& ((ch < '\u0020') || (ch > '\u007F')) )
    {
      putWarning(
		String.format(
			"\'%c\': kein ASCII-Zeichen, entsprechend Unicode"
				+ " mit %02XH \u00FCbersetzt",
			ch,
			(int) ch ) );
    }
    putCode( ch );
  }


  private void putCode( int b ) throws PrgException
  {
    if( (this.codeBuf != null) && (this.passNum == 2) ) {
      if( this.begAddr < 0 ) {
	this.begAddr = this.curAddr;
      } else {
	if( this.endAddr + 1 < this.curAddr ) {
	  int n = this.curAddr - this.endAddr - 1;
	  for( int i = 0; i < n; i++ ) {
	    this.codeBuf.write( 0 );
	  }
	}
      }
      this.endAddr = this.curAddr;
      this.codeBuf.write( b );
    }
    this.curAddr++;
    checkAddr();
  }


  private void putWord( int w ) throws PrgException
  {
    putCode( w );
    putCode( w >> 8 );
  }


  private void skipCode( int n ) throws PrgException
  {
    this.curAddr += n;
    checkAddr();
  }


  private void reset()
  {
    init();
    this.curSource = this.mainSource;
    this.appName   = null;
    this.stack.clear();
    this.labels.clear();
    this.labels.put(
		BUILT_IN_LABEL,
		new AsmLabel( BUILT_IN_LABEL, 1, false ) );
    if( this.curSource != null ) {
      this.curSource.reset();
    }
    if( this.codeBuf != null ) {
      this.codeBuf.reset();
    }
    if( this.srcOut != null ) {
      this.srcOut.setLength( 0 );
    }
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Robotron-Mnemonik verwendet wird,
   * die nicht mit der Zilog-Mnemonik uebereinstimmt.
   */
  private void robotronMnemonic()
  {
    if( this.options.getAsmSyntax() == Syntax.ZILOG_ONLY )
      putWarning( "Robotron-Mnemonik" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Robotron-Syntax verwendet wird,
   * die nicht mit der Zilog-Syntax uebereinstimmt.
   */
  private void robotronSyntax()
  {
    if( this.options.getAsmSyntax() == Syntax.ZILOG_ONLY )
      putWarning( "Robotron-Syntax" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Zilog-Syntax verwendet wird,
   * die nicht mit der Robotron-Syntax uebereinstimmt.
   */
  private void zilogSyntax()
  {
    if( this.options.getAsmSyntax() == Syntax.ROBOTRON_ONLY )
      putWarning( "Zilog-Syntax" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Zilog-Mnemonik verwendet wird,
   * die nicht mit der Robotron-Mnemonik uebereinstimmt.
   */
  private void zilogMnemonic()
  {
    if( this.options.getAsmSyntax() == Syntax.ROBOTRON_ONLY )
      putWarning( "Zilog-Mnemonik" );
  }


  private static String stripEnclosedText(
				String text,
				String itemDesc ) throws PrgException
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char ch = text.charAt( 0 );
	if( (ch == '\'') || (ch == '\"') ) {
	  if( (len < 2) || (text.charAt( len - 1 ) != ch) ) {
	    throw new PrgException( itemDesc
			  + " nicht mit " + ch + " abgeschlossen" );
	  }
	  if( len > 2 ) {
	    text = text.substring( 1, len - 1 );
	  }
	}
      }
    }
    return text;
  }


  private void undocInst()
  {
    if( !this.options.getAllowUndocInst() )
      putWarning( "Undokumentierter Befehl" );
  }


  private void undocSyntax()
  {
    if( !this.options.getAllowUndocInst() )
      putWarning( "Undokumentierte Syntax" );
  }


  private void throwNoSuchInstArgs() throws PrgException
  {
    throw new PrgException( "Die Anweisung existiert nicht"
		+ " f\u00FCr die angegebenen Argumente." );
  }


  private void printLabels()
  {
    StringBuilder buf = this.listOut;
    if( buf != null ) {
      buf.append( '\n' );
    } else {
      buf = new StringBuilder( 0x1000 );
    }
    boolean    missingValue = false;
    boolean    firstLabel   = true;
    AsmLabel[] labels       = getSortedLabels();
    if( labels != null ) {
      for( int i = 0; i < labels.length; i++ ) {
	if( firstLabel ) {
	  firstLabel = false;
	  buf.append( "\nMarkentabelle:\n" );
	}
	boolean vMissing = true;
	String  vText    = "k.W.";
	Object  value    = labels[ i ].getLabelValue();
	if( value != null ) {
	  if( value instanceof Integer ) {
	    vMissing = false;
	    vText    = String.format(
			"%04X",
			((Integer) value).intValue() & 0xFFFF );
	  }
	}
	buf.append( String.format(
			"    %s   %s\n",
			vText,
			labels[ i ].getLabelName() ) );
	missingValue |= vMissing;
      }
    }
    if( firstLabel ) {
      buf.append( "Markentabelle ist leer." );
    } else if( missingValue ) {
      buf.append(
	"\n    k.W.: Numerischer Wert konnte nicht berechnet werden." );
    }
    buf.append( '\n' );
    if( this.listOut == null ) {
      appendToOutLog( buf.toString() );
    }
  }


  private void printListTableHeader()
  {
    this.listOut.append( "\nZeile   Adr    Maschinen-Code   Quelltext\n" );
    for( int i = 0; i < 72; i++ ) {
      this.listOut.append( '-' );
    }
    this.listOut.append( '\n' );
  }


  private void putWarningOutOf8Bits()
  {
    putWarning( "Numerischer Wert au\u00DFerhalb 8-Bit-Bereich:"
					+ " Bits gehen verloren" );
  }


  private boolean writeCodeToFile( boolean forZ9001 )
  {
    boolean status = false;
    byte[] codeBytes = this.codeOut;
    if( codeBytes != null ) {
      if( codeBytes.length == 0 ) {
	codeBytes = null;
      }
    }
    if( codeBytes != null ) {
      File file = this.options.getCodeFile();
      if( file != null ) {
	int     begAddr = this.begAddr;
	Integer sAddr   = this.entryAddr;
	if( sAddr == null ) {
	  sAddr = begAddr;
	}
	FileFormat fileFmt  = FileFormat.BIN;
	String     fileType = null;
	String     fileDesc = "";
	String     fileName = file.getName();
	if( fileName != null ) {
	  String upperName = fileName.toUpperCase();
	  if( fileDesc.isEmpty() ) {
	    int pos = fileName.lastIndexOf( '.' );
	    if( pos > 0 ) {
	      fileDesc = upperName.substring( 0, pos );
	    }
	  }
	  if( upperName.endsWith( ".HEX" ) ) {
	    fileFmt = FileFormat.INTELHEX;
	  } else if( upperName.endsWith( ".KCC" )
		     || upperName.endsWith( ".KCM" ) )
	  {
	    fileFmt = FileFormat.KCC;
	  } else if( upperName.endsWith( ".TAP" ) ) {
	    if( forZ9001 ) {
	      fileFmt = FileFormat.KCTAP_Z9001;
	      if( sAddr != null ) {
		fileType = "COM";
	      }
	    } else {
	      fileFmt = FileFormat.KCTAP_KC85;
	    }
	  } else if( upperName.endsWith( ".Z80" ) ) {
	    fileFmt  = FileFormat.HEADERSAVE;
	    fileType = (sAddr != null ? "C" : "M");
	  }
	  if( fileFmt.equals( FileFormat.KCC )
	      || fileFmt.equals( FileFormat.KCTAP_KC85 )
	      || fileFmt.equals( FileFormat.KCTAP_Z9001 ) )
	  {
	    /*
	     * Wenn ein Programmname bekannt ist,
	     * wird dieser als Dateiname in den Dateikopf eingetragen,
	     * damit beim Laden der Datei mittels der Kassettenfunktionen
	     * der dann sichtbare Dateiname gleich dem Programmnamen ist.
	     * Dies vermeidet nicht nur Verwirrung,
	     * sondern ist z.B. beim Z9001 auch zwingend notwendig.
	     */
	    if( this.appName != null ) {
	      if( !this.appName.isEmpty() ) {
		fileDesc = appName;
	      }
	    }
	  }
	}
	try {
	  if( this.interactive ) {
	    appendToOutLog( "Speichere Programmcode in Datei \'"
				+ file.getPath() + "\'...\n" );
	  }
	  FileSaver.saveFile(
		file,
		fileFmt,
		new LoadData(
			codeBytes,
			0,
			codeBytes.length,
			this.begAddr,
			-1,
			fileFmt ),
		this.begAddr,
		begAddr + codeBytes.length - 1,
		false,
		this.begAddr,
		sAddr,
		fileDesc,
		fileType );
	  status = true;
	}
	catch( IOException ex ) {
	  StringBuilder buf = new StringBuilder( 256 );
	  buf.append( "Ein-/Ausgabefehler" );
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( !msg.isEmpty() ) {
	      buf.append( ":\n" );
	      buf.append( msg );
	    }
	  }
	  buf.append( '\n' );
	  appendToErrLog( buf.toString() );
	}
      } else {
	appendToErrLog( "Programmcode kann nicht gespeichert werden,\n"
			+ "da kein Dateiname ausgew\u00E4hlt wurde.\n" );
      }
    } else {
      appendToErrLog( "Programmcode kann nicht gespeichert werden,\n"
			+ "da kein einziges Byte erzeugt wurde.\n" );
    }
    return status;
  }
}
