/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z80-Assembler
 */

package jkcemu.programming.assembler;

import java.awt.EventQueue;
import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.*;
import jkcemu.text.*;
import jkcemu.tools.ReassFrm;
import jkcemu.tools.debugger.*;


public class Z80Assembler extends PrgThread
{
  private StringBuilder         sourceOut;
  private ByteArrayOutputStream codeOut;
  private Map<String,AsmLabel>  labels;
  private AsmLabel[]            sortedLabels;
  private Integer               entryAddr;
  private boolean               status;
  private boolean               addrOverflow;
  private boolean               endReached;
  private int                   passNum;
  private int                   begAddr;
  private int                   endAddr;
  private int                   curAddr;


  public Z80Assembler(
		EmuThread  emuThread,
		EditText   editText,
		String     sourceText,
		Appendable logOut,
		PrgOptions options,
		boolean    forceRun )
  {
    super(
	"JKCEMU assembler",
	emuThread,
	editText,
	sourceText,
	logOut,
	options,
	forceRun );
    this.sourceOut       = null;
    this.codeOut         = null;
    this.labels          = new Hashtable<String,AsmLabel>();
    this.sortedLabels    = null;
    this.entryAddr       = null;
    this.status          = true;
    this.addrOverflow    = false;
    this.endReached      = false;
    this.passNum         = 0;
    this.begAddr         = -1;
    this.endAddr         = -1;
    this.curAddr         = 0;
  }


  public void assemble() throws IOException
  {
    clearErrorCount();
    this.passNum    = 1;
    this.endReached = false;
    try {
      appendToLog( "Assembliere...\nLauf 1:\n" );
      parseAsm();
      if( this.running && this.status ) {
	this.passNum    = 2;
	this.endReached = false;
	if( this.options.getCodeToEmu() || this.options.getCodeToFile() ) {
	  this.codeOut = new ByteArrayOutputStream( 0x1000 );

	}
	appendToLog( "Lauf 2:\n" );
	if( this.options.getFormatSource() ) {
	  this.sourceOut = new StringBuilder(
				Math.max( getSourceLength(), 16 ) );
	}
	resetSource();
	parseAsm();
	if( this.codeOut != null ) {
	  this.codeOut.close();
	  if( this.running && this.status ) {
	    byte[] aCode = this.codeOut.toByteArray();
	    if( aCode != null ) {
	      if( this.options.getCodeToEmu() ) {
		writeCodeToEmu( aCode, this.options.getCodeToSecondSystem() );
	      }
	      if( this.options.getCodeToFile() ) {
		writeCodeToFile(
			this.options.getCodeFile(),
			this.options.getCodeFileFormat(),
			this.options.getCodeFileType(),
			this.options.getCodeFileDesc(),
			aCode );
	      }
	    }
	  }
	}
	if( this.running && this.status ) {
	  if( this.options.getPrintLabels() ) {
	    printLabels();
	  }
	  if( this.options.getCodeToEmu() ) {
	    if( this.options.getLabelsToDebugger() ) {
	      labelsToDebugger( this.options.getCodeToSecondSystem() );
	    }
	    if( this.options.getLabelsToReassembler() ) {
	      labelsToReass( this.options.getCodeToSecondSystem() );
	    }
	  }
	}
      }
    }
    catch( TooManyErrorsException ex ) {
      appendToLog( "\nAbgebrochen aufgrund zu vieler Fehler\n" );
    }
    if( getErrorCount() > 0 ) {
      appendErrorCountToLog();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    EditFrm editFrm = this.editText.getEditFrm();
    try {
      assemble();
      if( this.running && this.status ) {
	appendToLog( "Fertig\n" );
	if( this.sourceOut != null )
	  fireReplaceSourceText( this.sourceOut.toString() );
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
      EventQueue.invokeLater( new ErrorMsg( editFrm, ex ) );
    }
    if( editFrm != null )
      editFrm.threadTerminated( this );
  }


	/* --- private Methoden --- */

  private void checkAddr() throws PrgException
  {
    if( !this.addrOverflow && ((this.curAddr & ~0xFFFF) != 0) ) {
      this.addrOverflow = true;
      throw new PrgException( "\u00DCberlauf: Adressz\u00E4hler > 0FFFFh" );
    }
  }


  private void parseAsm() throws IOException, TooManyErrorsException
  {
    this.begAddr = -1;
    this.endAddr = -1;
    this.curAddr = 0;
    String line = readLine();
    while( this.running && !this.endReached && (line != null) ) {
      parseLine( line );
      line = readLine();
    }
  }


  private void parseLine( String line )
				throws IOException, TooManyErrorsException
  {
    try {
      AsmLine asmLine = AsmLine.scanLine(
				this,
				line,
				this.options.getLabelsCaseSensitive() );
      if( asmLine != null ) {
	String labelName = asmLine.getLabel();
	if( labelName != null ) {
	  if( this.passNum == 1 ) {
	    if( this.labels.containsKey( labelName ) ) {
	      throw new PrgException(
			"Marke " + labelName + " bereits vergeben" );
	    }
	    this.labels.put(
			labelName,
			new AsmLabel( labelName, this.curAddr ) );
	    this.sortedLabels = null;
	  }
	}
	String instruction = asmLine.getInstruction();
	if( instruction != null ) {
	  if( instruction.length() > 0 ) {
	    if( instruction.equals( "ADD" ) ) {
	      parseADD( asmLine );
	    }
	    else if( instruction.equals( "ADC" ) ) {
	      parseADC_SBC( asmLine, 0x88, 0x4A );
	    }
	    else if( instruction.equals( "AND" ) ) {
	      parseBiOp8( asmLine, 0xA0 );
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
		     || instruction.equals( "DB" )
		     || instruction.equals( ".DB" ) )
	    {
	      parseDEFB( asmLine );
	    }
	    else if( instruction.equals( "DEFH" )
		     || instruction.equals( ".DEFH" )
		     || instruction.equals( "HEX" )
		     || instruction.equals( ".HEX" ) )
	    {
	      parseDEFH( asmLine );
	    }
	    else if( instruction.equals( "DEFS" )
		     || instruction.equals( ".DEFS" )
		     || instruction.equals( "DS" )
		     || instruction.equals( ".DS" ) )
	    {
	      parseDEFS( asmLine );
	    }
	    else if( instruction.equals( "DEFW" )
		     || instruction.equals( ".DEFW" )
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
	      int d = getAddrDiff( asmLine.nextArg() );
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
		     || instruction.equals( ".ENT" ) )
	    {
	      parseENT( asmLine );
	    }
	    else if( instruction.equals( "EQU" )
		     || instruction.equals( ".EQU" ) )
	    {
	      parseEQU( asmLine );
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
	      int d = getAddrDiff( asmLine.nextArg() );
	      asmLine.checkEOL();
	      putCode( 0x38 );
	      putCode( d );
	      robotronMnemonic();
	    }
	    else if( instruction.equals( "JRNC" ) ) {
	      int d = getAddrDiff( asmLine.nextArg() );
	      asmLine.checkEOL();
	      putCode( 0x30 );
	      putCode( d );
	      robotronMnemonic();
	    }
	    else if( instruction.equals( "JRNZ" ) ) {
	      int d = getAddrDiff( asmLine.nextArg() );
	      asmLine.checkEOL();
	      putCode( 0x20 );
	      putCode( d );
	      robotronMnemonic();
	    }
	    else if( instruction.equals( "JRZ" ) ) {
	      int d = getAddrDiff( asmLine.nextArg() );
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
	    else if( instruction.equals( "NEG" ) ) {
	      asmLine.checkEOL();
	      putCode( 0xED );
	      putCode( 0x44 );
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
	    } else {
	      throw new PrgException(
			"\'" + instruction + "\': Unbekannte Mnemonik" );
	    }
	  }
	}
	if( this.sourceOut != null ) {
	  asmLine.appendFormattedTo( this.sourceOut );
	  this.sourceOut.append( (char) '\n' );
	}
      }
    }
    catch( PrgException ex ) {
      String msg = ex.getMessage();
      if( msg == null ) {
        msg = "Unbekannter Fehler";
      }
      appendLineNumMsgToLog( msg, "Fehler" );
      this.status = false;
      incErrorCount();
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
    String  text = asmLine.nextArg().toString();
    if( text != null ) {
      text = text.toUpperCase();
      if( text.equals( "U880" ) ) {
	robotronSyntax();
	done = true;
      }
      else if( text.equals( "Z80" ) ) {
	zilogSyntax();
	done = true;
      }
    }
    if( !done ) {
      throw new PrgException( "\'Z80\' oder \'U880\' erwartet" );
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
      skipCode( nextWordArg( asmLine ) );
    } while( asmLine.hasMoreArgs() );
  }


  private void parseDEFW( AsmLine asmLine ) throws PrgException
  {
    do {
      putWord( nextWordArg( asmLine ) );
    } while( asmLine.hasMoreArgs() );
  }


  private void parseEND( AsmLine asmLine ) throws PrgException
  {
    this.endReached = true;
    asmLine.checkEOL();
  }


  private void parseENT( AsmLine asmLine ) throws PrgException
  {
    if( this.passNum == 1 ) {
      if( this.entryAddr != null ) {
	throw new PrgException( "Mehrfache ENT-Anweisungen nicht erlaubt" );
      }
      this.entryAddr = this.curAddr;
    }
    asmLine.checkEOL();
  }


  private void parseEQU( AsmLine asmLine ) throws PrgException
  {
    String labelName = asmLine.getLabel();
    if( labelName != null ) {
      /*
       * Die Marke wurde bereits beim Zerlegen der Zeile erkannt und
       * der Markentabelle mit dem Wert der aktuellen Adresse hinzugefuegt.
       * Hier muss nur der Wert korrigiert werden.
       */
      AsmLabel label = this.labels.get( labelName );
      if( label != null ) {
	label.setLabelValue( nextWordArg( asmLine ) );
      }
      asmLine.checkEOL();
    } else {
      throw new PrgException( "EQU ohne Marke" );
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


  private void parseJMP( AsmLine asmLine ) throws PrgException
  {
    AsmArg a = asmLine.nextArg();
    asmLine.checkEOL();
    robotronMnemonic();
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
	int d = getAddrDiff( a2 );
	asmLine.checkEOL();
	putCode( 0x20 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "Z" ) ) {
	int d = getAddrDiff( a2 );
	asmLine.checkEOL();
	putCode( 0x28 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "NC" ) ) {
	int d = getAddrDiff( a2 );
	asmLine.checkEOL();
	putCode( 0x30 );
	putCode( d );
	zilogSyntax();
      }
      else if( a1.equalsUpper( "C" ) ) {
	int d = getAddrDiff( a2 );
	asmLine.checkEOL();
	putCode( 0x38 );
	putCode( d );
	zilogSyntax();
      } else {
	throwNoSuchInstArgs();
      }
    } else {
      int d = getAddrDiff( a1 );
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
	putCode( 0xDD );
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
		&& (a2.equalsUpper( "IYH" ) || a2.equalsUpper( "YX" ))) )
    {
      putCode( preCode );
      putCode( 0x64 + baseCode );
    }
    else if( ((preCode == 0xDD)
		&& (a2.equalsUpper( "IXL" ) || a2.equalsUpper( "HL" )))
	     || ((preCode == 0xFD)
		&& (a2.equalsUpper( "IYL" ) || a2.equalsUpper( "YL" ))) )
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
    int a = nextWordArg( asmLine );
    asmLine.checkEOL();
    if( a < this.curAddr ) {
      throw new PrgException( "Zur\u00FCcksetzen des"
			+ " Addressz\u00E4hlers nicht erlaubt" );
    }
    if( (this.curAddr > 0) && (a > this.curAddr) ) {
      skipCode( a - this.curAddr );
    }
    this.curAddr = a;
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
    boolean isHex = false;
    String  text  = asmLine.nextArg().toUpperString();
    asmLine.checkEOL();
    if( text.endsWith( "H" ) ) {
      isHex = true;
      text  = text.substring( 0, text.length() - 1 );
    }
    try {
      int v = Integer.parseInt( text, 16 );
      if( (v & ~0x38) != 0 ) {
	throwNoSuchInstArgs();
      }
      putCode( 0xC7 | v );
      if( isHex ) {
	zilogSyntax();
      } else {
	if( (this.options.getSyntax() == PrgOptions.Syntax.ZILOG_ONLY)
	    && (v > 9) )
	{
	  putWarning( "Hexadezimalkonstante endet nicht mit \'H\'" );
	}
      }
    }
    catch( NumberFormatException ex ) {
      throw new PrgException( "Hexadezimalkonstante erwartet" );
    }
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


  private void parseBiOp8Internal( AsmArg a, int baseCode ) throws PrgException
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
    char   ch      = '\u0020';
    String bitText = a1.toString();
    if( bitText.length() == 1 ) {
      ch = bitText.charAt( 0 );
    }
    if( (ch < '0') || (ch > '7') ) {
      throw new PrgException( "\'0\' bis \'7\' als Bitabgabe erwartet" );
    }
    int bitCode = (ch - '0') << 3;
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


  private int nextByteArg( AsmLine asmLine ) throws PrgException
  {
    return getByte( asmLine.nextArg() );
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
  private int getAddrDiff( AsmArg asmArg ) throws PrgException
  {
    int v = 0;
    if( this.passNum == 2 ) {
      v = getWord( asmArg ) - ((this.curAddr + 2) & 0xFFFF);
      if( (v < ~0x7F) || (v > 0x7F) ) {
	throw new PrgException( "Relative Sprungdistanz zu gro\u00DF" );
      }
    }
    return v;
  }


  private int getIndirectIXYDist( AsmArg a ) throws PrgException
  {
    int    v    = 0;
    String text = a.getIndirectIXYDist();
    if( text != null ) {
      if( text.length() > 0 ) {
	v = ExprParser.parse(
			text,
			this.labels,
			this.passNum == 2,
			this.options.getLabelsCaseSensitive() );
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
    int v = ExprParser.parse(
			text,
			this.labels,
			this.passNum == 2,
			this.options.getLabelsCaseSensitive() );
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
    int v = ExprParser.parse(
			text,
			this.labels,
			this.passNum == 2,
			this.options.getLabelsCaseSensitive() );
    if( (v < ~0x7FFF) || (v > 0xFFFF) ) {
      putWarning( "Numerischer Wert au\u00DFerhalb 16-Bit-Bereich:"
					+ "Bits gehen verloren" );
    }
    return v;
  }


  private AsmLabel[] getSortedLabels()
  {
    AsmLabel[] rv = this.sortedLabels;
    if( rv == null ) {
      try {
	Collection<AsmLabel> c = this.labels.values();
	if( c != null ) {
	  int n = c.size();
	  if( n > 0 ) {
	    rv = c.toArray( new AsmLabel[ n ] );
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
    return rv;
  }


  private void putChar( char ch ) throws PrgException
  {
    if( ch > 0x7F ) {
      if( ch <= 0xFF ) {
	putWarning(
		String.format(
			"\'%c\' ist kein ASCII-Zeichen, entsprechend Unicode"
				+ " mit %02XH \u00FCbersetzt",
			ch,
			(int) ch ) );
      } else {
	throw new PrgException(
		String.format(
			"\'%c\': 16-Bit-Unicodezeichen nicht erlaubt",
			ch ) );
      }
    }
    putCode( ch );
  }


  private void putCode( int b ) throws PrgException
  {
    if( this.codeOut != null ) {
      if( this.begAddr < 0 ) {
	this.begAddr = this.curAddr;
      } else {
	if( this.endAddr + 1 < this.curAddr ) {
	  int n = this.curAddr - this.endAddr - 1;
	  for( int i = 0; i < n; i++ ) {
	    this.codeOut.write( 0 );
	  }
	}
      }
      this.endAddr = this.curAddr;
      this.codeOut.write( b );
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


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Robotron-Mnemonik verwendet wird,
   * die nicht mit der Zilog-Mnemonik uebereinstimmt.
   */
  private void robotronMnemonic()
  {
    if( this.options.getSyntax() == PrgOptions.Syntax.ZILOG_ONLY )
      putWarning( "Robotron-Mnemonik" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Robotron-Syntax verwendet wird,
   * die nicht mit der Zilog-Syntax uebereinstimmt.
   */
  private void robotronSyntax()
  {
    if( this.options.getSyntax() == PrgOptions.Syntax.ZILOG_ONLY )
      putWarning( "Robotron-Syntax" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Zilog-Syntax verwendet wird,
   * die nicht mit der Robotron-Syntax uebereinstimmt.
   */
  private void zilogSyntax()
  {
    if( this.options.getSyntax() == PrgOptions.Syntax.ROBOTRON_ONLY )
      putWarning( "Zilog-Syntax" );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn eine Zilog-Mnemonik verwendet wird,
   * die nicht mit der Robotron-Mnemonik uebereinstimmt.
   */
  private void zilogMnemonic()
  {
    if( this.options.getSyntax() == PrgOptions.Syntax.ROBOTRON_ONLY )
      putWarning( "Zilog-Mnemonik" );
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


  private void writeCodeToEmu( byte[] aCode, boolean secondSys )
  {
    if( aCode.length > 0 ) {
      int begAddr   = this.begAddr;
      int startAddr = (this.entryAddr != null ?
				this.entryAddr.intValue() : begAddr);
      if( (this.emuThread != null) && (begAddr >= 0) && (begAddr <= 0xFFFF) ) {
	String secondSysName = null;
	EmuSys emuSys        = null;
	if( secondSys ) {
	  emuSys = this.emuThread.getEmuSys();
	  if( emuSys != null ) {
	    secondSysName = emuSys.getSecondSystemName();
	  }
	}
	StringBuilder buf = new StringBuilder( 256 );
	buf.append( "Lade Programmcode in Arbeitsspeicher (" );
	if( secondSysName != null ) {
	  buf.append( secondSysName );
	  buf.append( ", " );
	}
	if( begAddr != this.begAddr ) {
	  buf.append( "alternativer " );
	}
	buf.append(
		String.format(
			"Bereich %04X-%04X)",
			begAddr,
			begAddr + aCode.length -  1) );
	if( this.forceRun && (startAddr >= 0) ) {
	  buf.append(
		String.format(
			"\nund starte Programm auf Adresse %04X",
			startAddr ) );
	} else {
	  if( this.forceRun ) {
	    buf.append( "\nStart des Programms nicht m\u00F6glich,\n"
		+ "da Quelltext keine ENT-Anweisung (Programmeintrittspunkt)"
		+ " enth\u00E4lt" );
	  }
	}
	buf.append( (char) '\n' );
	appendToLog( buf.toString() );
	if( (emuSys != null) && (secondSysName != null) ) {
	  emuSys.loadIntoSecondSystem(
			aCode,
			begAddr,
			this.forceRun ? startAddr : -1 );
	} else {
	  this.emuThread.loadIntoMemory(
		new LoadData(
			aCode,
			0,
			aCode.length,
			begAddr,
			this.forceRun ? startAddr : -1,
			null ) );
	}
      }
    } else {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Programmcode kann nicht in Emulator geladen" );
      if( this.forceRun ) {
	buf.append( " und dort gestartet" );
      }
      buf.append( " werden,\nda kein einziges Byte erzeugt wurde.\n" );
      appendToLog( buf.toString() );
    }
  }


  private void writeCodeToFile(
			File   file,
			String fileFmt,
			char   fileType,
			String fileDesc,
			byte[] aCode )
  {
    if( aCode.length > 0 ) {
      if( file != null ) {
	if( (fileType < '\u0020') || (fileType > 0x7E) ) {
	  fileType = '\u0020';
	}
	int     begAddr = this.begAddr;
	Integer sAddr   = null;
	if( fileType == 'C' ) {
	  if( this.entryAddr != null ) {
	    sAddr = this.entryAddr;
	  } else {
	    sAddr = new Integer( begAddr );
	  }
	}
	try {
	  FileSaver.saveFile(
		file,
		fileFmt,
		new LoadData(
			aCode,
			0,
			aCode.length,
			this.begAddr,
			-1,
			fileFmt ),
		this.begAddr,
		begAddr + aCode.length - 1,
		false,
		false,
		this.begAddr,
		sAddr,
		fileType,
		fileDesc,
		null );
	}
	catch( IOException ex ) {
	  StringBuilder buf = new StringBuilder( 256 );
	  buf.append( "Ein-/Ausgabefehler" );
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( msg.length() > 0 ) {
	      buf.append( ":\n" );
	      buf.append( msg );
	    }
	  }
	  buf.append( (char) '\n' );
	  appendToLog( buf.toString() );
	}
      } else {
	appendToLog( "Programmcode kann nicht gespeichert werden,\n"
			+ "da kein Dateiname ausgew\u00E4hlt wurde.\n" );
      }
    } else {
      appendToLog( "Programmcode kann nicht gespeichert werden,\n"
			+ "da kein einziges Byte erzeugt wurde.\n" );
    }
  }


  private void labelsToDebugger( boolean secondSys )
  {
    AsmLabel[] labels = getSortedLabels();
    if( labels != null ) {
      if( labels.length > 0 ) {
	boolean  done     = false;
	DebugFrm debugFrm = null;
	if( secondSys && (this.emuThread != null) ) {
	  EmuSys emuSys = this.emuThread.getEmuSys();
	  if( emuSys != null ) {
	    if( emuSys.getSecondSystemName() != null ) {
	      debugFrm = Main.getScreenFrm().openSecondDebugger();
	      done     = true;
	    }
	  }
	}
	if( !done ) {
	  debugFrm = Main.getScreenFrm().openPrimaryDebugger();
	}
	if( debugFrm != null ) {
	  debugFrm.setLabels( labels );
	}
      }
    }
  }


  private void labelsToReass( boolean secondSys )
  {
    AsmLabel[] labels = getSortedLabels();
    if( labels != null ) {
      if( labels.length > 0 ) {
	boolean  done     = false;
	ReassFrm reassFrm = null;
	if( secondSys && (this.emuThread != null) ) {
	  EmuSys emuSys = this.emuThread.getEmuSys();
	  if( emuSys != null ) {
	    if( emuSys.getSecondSystemName() != null ) {
	      reassFrm = Main.getScreenFrm().openSecondReassembler();
	      done     = true;
	    }
	  }
	}
	if( !done ) {
	  reassFrm = Main.getScreenFrm().openPrimaryReassembler();
	}
	if( reassFrm != null ) {
	  reassFrm.setLabels( labels );
	}
      }
    }
  }


  private void printLabels()
  {
    boolean    firstLabel = true;
    AsmLabel[] labels     = getSortedLabels();
    if( labels != null ) {
      for( int i = 0; i < labels.length; i++ ) {
	if( firstLabel ) {
	  firstLabel = false;
	  appendToLog( "\nMarkentabelle:\n" );
	}
	String        name = labels[ i ].getLabelName();
	StringBuilder buf  = new StringBuilder( 13 + name.length() );
	buf.append( "    " );
	buf.append( labels[ i ].toHex16String() );
	buf.append( "h   " );
	buf.append( name );
	buf.append( (char) '\n' );
	appendToLog( buf.toString() );
      }
    }
    if( firstLabel ) {
      appendToLog( "Markentabelle ist leer.\n" );
    } else {
      appendToLog( "\n" );
    }
  }


  private void putWarningOutOf8Bits()
  {
    putWarning( "Numerischer Wert au\u00DFerhalb 8-Bit-Bereich:"
					+ "Bits gehen verloren" );
  }
}

