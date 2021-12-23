/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * CP/M-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.NANOS;
import jkcemu.emusys.PCM;
import jkcemu.file.FileFormat;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;
import jkcemu.programming.basic.BasicUtil;


public class CPMTarget extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME   = "TARGET_CPM";
  public static final String DISPLAY_TARGET_NAME = "CP/M-kompatibel";

  private static final int IOCTB_M_BIN_OFFS = BasicLibrary.IOCTB_DRIVER_OFFS;
  private static final int IOCTB_M_ERR_OFFS = IOCTB_M_BIN_OFFS + 1;
  private static final int IOCTB_M_POS_OFFS = IOCTB_M_ERR_OFFS + 1;
  private static final int IOCTB_FCB_OFFS   = IOCTB_M_POS_OFFS + 1;
  private static final int IOCTB_FCB_R0_OFFS  = IOCTB_FCB_OFFS + 33;
  private static final int IOCTB_FNAME_OFFS   = IOCTB_FCB_OFFS + 1;
  private static final int IOCTB_FTYPE_OFFS   = IOCTB_FNAME_OFFS + 8;
  private static final int IOCTB_DMA_OFFS     = IOCTB_FCB_OFFS + 36;
  private static final int IOCTB_CHANNEL_SIZE = IOCTB_DMA_OFFS + 128;

  private boolean usesFileRead;
  private boolean usesFileWrite;
  private boolean usesX_M_INKEY;


  public CPMTarget()
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesFileRead || this.usesFileWrite ) {
      buf.append( "X_CPM_FILE_M_DRIVE:\tDS\t1\n"
		+ "X_CPM_FILE_M_FNAME:\tDS\t2\n" );
    }
    if( this.usesX_M_INKEY ) {
      buf.append( "X_M_INKEY:\tDS\t1\n" );
    }
  }


  @Override
  public void appendDataTo( AsmCodeBuf buf )
  {
    if( this.usesFileRead || this.usesFileWrite ) {
      buf.append( "X_CPM_FILE_DATA_VALID_CHR:\n"
		+ "\tDB\t21H,23H,24H,25H,26H,27H,28H,29H,2BH\n"
		+ "\tDB\t2DH,3DH,5EH,5FH,60H,7BH,7DH,7EH,00H\n" );
    }
    if( this.usesFileRead ) {
      buf.append( "X_CPM_FILE_DATA_RDPTRS:\n"
		+ "\tDW\tX_CPM_FILE_CLOSE\n"
		+ "\tDW\tX_CPM_FILE_EOF\n"
		+ "\tDW\tX_CPM_FILE_READ\n" );
    }
  }


  @Override
  public void appendDiskHandlerTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    boolean done = false;

    /*
     * Datei-Handler
     *
     * Parameter:
     *   (IO_M_NAME):   Zeiger auf Geraete-/Dateiname
     *   (IO_M_CADDR):  Anfangsadresse des Kanalzeigerfeldes
     *   (IO_M_ACCESS): Zugriffsmode
     * Rueckgabewert:
     *   CY=1:          Handler ist nicht zustaendig
     *   CY=0:          Handler ist zustaendig
     *                    -> keine weiteren Handler testen
     */
    buf.append( getDiskHandlerLabel() );
    buf.append( ":\n" );

    Set<Integer> modes = compiler.getIODriverModes(
					BasicCompiler.IODriver.DISK );
    if( modes != null ) {
      if( !modes.isEmpty() ) {
	boolean txtAppend = modes.contains( BasicLibrary.IOMODE_TXT_APPEND );
	boolean txtOutput = modes.contains( BasicLibrary.IOMODE_TXT_OUTPUT );
	boolean txtInput  = (modes.contains( BasicLibrary.IOMODE_TXT_INPUT )
			|| modes.contains( BasicLibrary.IOMODE_TXT_DEFAULT ));
	boolean binAppend = modes.contains( BasicLibrary.IOMODE_BIN_APPEND );
	boolean binOutput = modes.contains( BasicLibrary.IOMODE_BIN_OUTPUT );
	boolean binInput  = (modes.contains( BasicLibrary.IOMODE_BIN_INPUT )
			|| modes.contains( BasicLibrary.IOMODE_BIN_DEFAULT ));

	// Geraetename am Dateianfang testen
	buf.append( "\tLD\tDE,(IO_M_NAME)\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\t41H\n"				// A
		+ "\tJR\tC,X_CPM_FILE_HANDLER_2\n"
		+ "\tCP\t51H\n"				// Q
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_2\n"
		+ "\tLD\tB,A\n"				// Laufwerksbuchstabe
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
	// Geraetename oder User-Nummer?
		+ "\tCP\t30H\n"				// 0
		+ "\tJR\tC,X_CPM_FILE_HANDLER_2\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_5\n" // Geraetename ohne User-Nr
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_2\n"
	// User-Nr parsen
		+ "\tSUB\t30H\n"
		+ "X_CPM_FILE_HANDLER_1:\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,X_CPM_FILE_HANDLER_2\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_6\n" // Geraetename mit User-Nr
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_2\n"
		+ "\tPUSH\tAF\n"		   // C=(C*10)+A
		+ "\tLD\tA,C\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,C\n"
		+ "\tADD\tA,A\n"
		+ "\tLD\tC,A\n"
		+ "\tPOP\tAF\n"
		+ "\tADD\tA,C\n"
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_1\n"	// naechste Ziffer
	// Ueberlauf in User-Nr. -> User-Nr. ungueltig
		+ "X_CPM_FILE_HANDLER_2:\n"
	/*
	 * kein passendes Laufwerk oder ungueltige User-Nr
	 * -> pruefen, ob ein Geraetename vorhanden ist
	 */
		+ "\tLD\tDE,(IO_M_NAME)\n"
		+ "X_CPM_FILE_HANDLER_3:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_4\n"	// nur Dateiname
		+ "\tINC\tDE\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_3\n"
		+ "\tSCF\n"				// anderes Geraet
		+ "\tRET\n"
	/*
	 * nur Dateiname gefunden
	 * -> auf dem aktuellen Laufwerk oeffnen
	 */
		+ "X_CPM_FILE_HANDLER_4:\n"
		+ "\tXOR\tA\n"				// aktuellen LW
		+ "\tLD\t(X_CPM_FILE_M_DRIVE),A\n"
		+ "\tLD\tHL,(IO_M_NAME)\n"
		+ "\tLD\t(X_CPM_FILE_M_FNAME),HL\n"
		+ "\tJR\tX_CPM_FILE_HANDLER_7\n"
	/*
	 * Geraetename ohne User-Nr. gefunden,
	 * DE: Dateiname - 1, B: Laufwerksbuchstabe
	 */
		+ "X_CPM_FILE_HANDLER_5:\n"
		+ "\tLD\tA,B\n"
		+ "\tSUB\t40H\n"		// LW A: 1 usw.
		+ "\tLD\t(X_CPM_FILE_M_DRIVE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(X_CPM_FILE_M_FNAME),DE\n"
		+ "\tJR\tX_CPM_FILE_HANDLER_7\n"
	/*
	 * Geraetename mit User-Nr. gefunden,
	 * DE: Dateiname - 1, B: Laufwerksbuchstabe, C: User-Nr
	 */
		+ "X_CPM_FILE_HANDLER_6:\n"
		+ "\tLD\tA,B\n"
		+ "\tSUB\t40H\n"		// LW A: 1 usw.
		+ "\tLD\t(X_CPM_FILE_M_DRIVE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(X_CPM_FILE_M_FNAME),DE\n"
	// User-Nr. setzen
		+ "\tLD\tE,C\n"
		+ "\tLD\tC,20H\n"		// F_USERNUM
		+ "\tCALL\t0005H\n"		// User-Nr. setzen
	// FCB fuellen
		+ "X_CPM_FILE_HANDLER_7:\n"
		+ "\tCALL\tX_CPM_FILE_FILL_FCB\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"			// unglueltiger Dateiname
	// Betriebsart pruefen
		+ "\tLD\tHL,IO_M_ACCESS\n" );
	if( txtInput || binInput) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_DEFAULT_MASK
				| BasicLibrary.IOMODE_INPUT_MASK );
	  buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_8\n" );
	}
	if( txtOutput || binOutput ) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_OUTPUT_MASK );
	  buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_10\n" );
	}
	if( txtAppend || binAppend ) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_APPEND_MASK );
	  buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_13\n" );
	}
	BasicUtil.appendSetErrorIOMode( compiler );
	buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
	/*
	 * Datei zum Lesen oeffnen
	 */
	if( txtInput || binInput ) {
	  buf.append( "X_CPM_FILE_HANDLER_8:\n"
		+ "\tLD\tC,0FH\n"			// F_OPEN
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tCP\t04H\n"
		+ "\tJR\tC,X_CPM_FILE_HANDLER_9\n" );
	  BasicUtil.appendSetErrorFileNotFound( compiler );
	  buf.append( "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
		+ "X_CPM_FILE_HANDLER_9:\n"
	  // Kanalzeigerfeld fuellen
		+ "\tCALL\tX_CPM_FILE_CLEAR_CHANNEL\n"
		+ "\tLD\tDE,(IO_M_CADDR)\n"
		+ "\tLD\tHL,X_CPM_FILE_DATA_RDPTRS\n"
		+ "\tLD\tBC,0008H\n"
		+ "\tLDIR\n"
	  // Position im Puffer auf FFh setzen
			+ "\tLD\tHL,(IO_M_CADDR)\n" );
	  buf.append_LD_DE_nn( IOCTB_M_POS_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
	  this.usesFileRead = true;
	}
	/*
	 * Datei zum Schreiben oeffnen
	 */
	if( txtOutput || binOutput ) {
	  buf.append( "X_CPM_FILE_HANDLER_10:\n"
		+ "\tLD\tC,13H\n"			// F_DELETE
		+ "\tCALL\tX_CPM_FILE_BDOS\n" );
	}
	if( txtOutput || binOutput || txtAppend || binAppend ) {
	  buf.append( "X_CPM_FILE_HANDLER_11:\n"
		+ "\tCALL\tX_CPM_FILE_FILL_FCB\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tC,16H\n"			// F_MAKE
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tCP\t04H\n"
		+ "\tJR\tC,X_CPM_FILE_HANDLER_12\n" );
	  BasicUtil.appendSetErrorDirFull( compiler );
	  buf.append( "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
	  // Kanalzeigerfeld fuellen
		+ "X_CPM_FILE_HANDLER_12:\n"
		+ "\tCALL\tX_CPM_FILE_CLEAR_CHANNEL\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\t(HL),LOW(X_CPM_FILE_CLOSE)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),HIGH(X_CPM_FILE_CLOSE)\n"
		+ "\tPOP\tHL\n" );
	  buf.append_LD_DE_nn( BasicLibrary.IOCTB_WRITE_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),LOW(X_CPM_FILE_WRITE)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),HIGH(X_CPM_FILE_WRITE)\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
	  this.usesFileWrite = true;
	}
	/*
	 * Datei zum Anhaengen oeffnen
	 */
	if( txtAppend || binAppend ) {
	  buf.append( "X_CPM_FILE_HANDLER_13:\n"
		+ "\tLD\tC,0FH\n"			// F_OPEN
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tCP\t04H\n"
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_11\n" );
	  if( txtAppend && binAppend ) {
	    buf.append( "\tLD\tDE,(IO_M_CADDR)\n" );
	    buf.append_LD_HL_nn( IOCTB_M_BIN_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_18\n" );
	  }
	  if( txtAppend ) {
	    /*
	     * Dateiende der Textdatei ermitteln,
	     * Dazu wird die Datei mit F_READRAND gelesen,
	     * da diese die Dateiposition fuer den sequenziellen Zugriff
	     * auf den mit F_READRAND gesetzten Record setzt
	     * und somit der naechste sequenzielle Schreibzugriff
	     * den gleichen Record betrifft.
	     */
	    buf.append( "X_CPM_FILE_HANDLER_14:\n"
		+ "\tCALL\tX_CPM_FILE_SET_DMA_ADDR\n"
		+ "\tLD\tC,21H\n"			// F_READRAND
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_15\n"	// Record gelesen
		+ "\tCP\t07H\n"
		+ "\tJR\tC,X_CPM_FILE_HANDLER_12\n"	// EOF erreicht
		+ "\tJR\tX_CPM_FILE_HANDLER_19\n"	// Fehler
	    // Byte 00 oder 1A suchen
		+ "X_CPM_FILE_HANDLER_15:\n"
		+ "\tLD\tDE,(IO_M_CADDR)\n" );
	    buf.append_LD_HL_nn( IOCTB_DMA_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tC,00H\n"
		+ "X_CPM_FILE_HANDLER_16:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_17\n"
		+ "\tCP\t1AH\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_17\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tC\n"
		+ "\tJP\tP,X_CPM_FILE_HANDLER_16\n"
	    // Record Counter inkremtieren
		+ "\tLD\tDE,(IO_M_CADDR)\n" );
	    buf.append_LD_HL_nn( IOCTB_FCB_R0_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,01H\n"
		+ "\tADD\tA,(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tJR\tNC,X_CPM_FILE_HANDLER_14\n"
		+ "\tINC\tHL\n"
		+ "\tINC\t(HL)\n"
		+ "\tJR\tX_CPM_FILE_HANDLER_14\n"
	  // Dateiendezeichen gefunden
		+ "X_CPM_FILE_HANDLER_17:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_CPM_FILE_HANDLER_12\n"
		+ "\tPOP\tDE\n" );
	    buf.append_LD_HL_nn( IOCTB_M_POS_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(HL),C\n"
		+ "\tRET\n" );
	  }
	  if( binAppend ) {
	    // Dateiende der Binaerdatei ermitteln
	    buf.append( "X_CPM_FILE_HANDLER_18:\n"
		+ "\tCALL\tX_CPM_FILE_SET_DMA_ADDR\n"
		+ "\tLD\tC,14H\n"			// F_READ
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_18\n"	// Record gelesen
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_CPM_FILE_HANDLER_12\n" );	// EOF erreicht
	  }
	  // Fehler
	  buf.append( "X_CPM_FILE_HANDLER_19:\n"
		+ "\tINC\tA\n"
		+ "\tJR\tNZ,X_CPM_FILE_HANDLER_20\n" );
	  BasicUtil.appendSetErrorHardware( compiler );
	  buf.append( "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
		+ "X_CPM_FILE_HANDLER_20:\n" );
	  BasicUtil.appendSetError( compiler );
	  buf.append( "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
	}

	/*
	 * CLOSE-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 */
	buf.append( "X_CPM_FILE_CLOSE:\n" );
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  if( txtInput || binInput ) {
	    // Datei zum Schreiben geoeffnet?
	    buf.append_LD_HL_nn( BasicLibrary.IOCTB_WRITE_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_CLOSE_2\n" );
	  }
	  if( txtOutput || txtAppend ) {
	    /*
	     * bei Textdateien, die zum Schreiben geoeffnet sind,
	     * Dateiendekennung 1Ah anhaengen
	     */
	    if( binOutput || binAppend ) {
	      buf.append_LD_HL_nn( IOCTB_M_BIN_OFFS );
	      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_CPM_FILE_CLOSE_1\n" );
	    }
	    buf.append( "\tPUSH\tDE\n"
		+ "\tLD\tA,1AH\n"
		+ "\tCALL\tX_CPM_FILE_WRITE\n"
		+ "\tPOP\tDE\n"
		+ "X_CPM_FILE_CLOSE_1:\n" );
	  }
	  buf.append_LD_HL_nn( IOCTB_M_POS_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tNZ,X_CPM_FILE_WRITE_2\n"
		+ "X_CPM_FILE_CLOSE_2:\n" );
	}
	buf.append( "\tLD\tC,10H\n"		// F_CLOSE
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tCP\t04H\n"
		+ "\tRET\tC\n" );
	BasicUtil.appendSetError( compiler );
	buf.append( "\tRET\n" );

	/*
	 * EOF-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   HL=-1: EOF erreicht
	 */
	if( txtInput || binInput ) {
	  buf.append( "X_CPM_FILE_EOF:\n" );
	  buf.append_LD_HL_nn( IOCTB_M_ERR_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
	}

	/*
	 * READ-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   A: gelesenes Zeichen oder 0 bei EOF bzw. vorherigem Fehler
	 */
	if( txtInput || binInput ) {
	  buf.append( "X_CPM_FILE_READ:\n" );
	  buf.append_LD_HL_nn( IOCTB_M_ERR_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tX_CPM_FILE_READ_CHECK_ERR\n"
		+ "\tLD\tA,00H\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"			// HL: IOCTB_M_POS
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tP,X_CPM_FILE_READ_1\n"
	  // naechstes Segment lesen
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tX_CPM_FILE_SET_DMA_ADDR\n"
		+ "\tLD\tC,14H\n"		// F_READ
		+ "\tCALL\tX_CPM_FILE_BDOS\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tDEC\tHL\n"			// HL: IOCTB_M_ERR
		+ "\tLD\t(HL),A\n"
		+ "\tCALL\tX_CPM_FILE_READ_CHECK_ERR\n"
		+ "\tLD\tA,00H\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"			// HL: IOCTB_M_POS
		+ "X_CPM_FILE_READ_1:\n"
		+ "\tINC\tA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDEC\tA\n" );
	  buf.append_LD_HL_nn( IOCTB_DMA_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tADC\tA,00H\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,(HL)\n" );
	  if( txtInput ) {
	    buf.append( "\tOR\tA\n"		// Dateiende-Byte?
 		+ "\tJR\tZ,X_CPM_FILE_READ_2\n"
		+ "\tCP\t1AH\n"	
		+ "\tRET\tNZ\n"
		+ "X_CPM_FILE_READ_2:\n" );
	    if( binInput ) {
	      buf.append( "\tLD\tB,A\n" );
	      buf.append_LD_HL_nn( IOCTB_M_BIN_OFFS );
	      buf.append( "\tADD\tHL,DE\n"
		+ "\tOR\tA\n"
		+ "\tLD\tA,B\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"	);		// HL: IOCTB_M_ERR_OFFS
	    } else {
	      buf.append_LD_HL_nn( IOCTB_M_ERR_OFFS );
	      buf.append( "\tADD\tHL,DE\n" );
	    }
	    buf.append( "\tLD\tA,01H\n"		// BDOS-Code fuer EOF
		+ "\tLD\t(HL),A\n"
		+ "\tCALL\tX_CPM_FILE_READ_CHECK_ERR\n"
		+ "\tXOR\tA\n" );
	  }
	  buf.append( "\tRET\n"
	  /*
	   * F_READ-Fehlercode auswerten und BASIC-Fehlervariablen setzen
	   * DE und HL bleiben erhalten
	   * Parameter:
	   *   A: Rueckgabewert der Funktion F_READ
	   * Rueckgabewert:
	   *   CY=0: OK
	   *   CY=1: Fehler, BASIC-Fehlervariablen gesetzt
	   */
		+ "X_CPM_FILE_READ_CHECK_ERR:\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );		// CY=0
	  if( compiler.usesErrorVars() ) {
	    buf.append( "\tPUSH\tHL\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_CPM_FILE_READ_CHECK_ERR_3\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,X_CPM_FILE_READ_CHECK_ERR_1\n"
		+ "\tCP\t0FFH\n"
		+ "\tJR\tZ,X_CPM_FILE_READ_CHECK_ERR_2\n" );
	    BasicUtil.appendSetError( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_READ_CHECK_ERR_3\n"
		+ "X_CPM_FILE_READ_CHECK_ERR_1:\n" );
	    BasicUtil.appendSetErrorMediaChanged( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_READ_CHECK_ERR_3\n"
		+ "X_CPM_FILE_READ_CHECK_ERR_2:\n" );
	    BasicUtil.appendSetErrorHardware( compiler );
	    buf.append( "X_CPM_FILE_READ_CHECK_ERR_3:\n"
		+ "\tPOP\tHL\n" );
	  }
	  buf.append( "\tSCF\n"
		+ "\tRET\n" );
	}

	/*
	 * WRITE-Routine
	 * Parameter:
	 *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
         *   A:            zu schreibendes Byte
	 */
	buf.append( "X_CPM_FILE_WRITE:\n" );
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  buf.append( "\tLD\tB,A\n"
		+ "\tLD\tDE,(IO_M_CADDR)\n" );
	  buf.append_LD_HL_nn( IOCTB_M_ERR_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tX_CPM_FILE_WRITE_CHECK_ERR\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"			// HL: IOCTB_M_POS_OFFS
		+ "\tLD\tC,(HL)\n"
		+ "\tPUSH\tHL\n" );
	  buf.append_LD_HL_nn( IOCTB_DMA_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,L\n"
		+ "\tADD\tA,C\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tADC\tA,00H\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\t(HL),B\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tC\n"
		+ "\tLD\t(HL),C\n"
		+ "\tRET\tP\n"
	  // Segment voll -> auf Datentraeger schreiben
		+ "\tLD\t(HL),00H\n"
		+ "X_CPM_FILE_WRITE_2:\n"
		+ "\tCALL\tX_CPM_FILE_SET_DMA_ADDR\n"
		+ "\tLD\tC,15H\n"		// F_WRITE
		+ "\tJP\tX_CPM_FILE_BDOS\n" );
	  buf.append_LD_HL_nn( IOCTB_M_ERR_OFFS );
	  buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),A\n"
		// direkt weiter mit X_CPM_FILE_WRITE_CHECK_ERR
	  /*
	   * F_WRITE-Fehlercode auswerten und BASIC-Fehlervariablen setzen
	   * DE und HL bleiben erhalten
	   * Parameter:
	   *   A: Rueckgabewert der Funktion F_WRITE
	   * Rueckgabewert:
	   *   CY=0: OK
	   *   CY=1: Fehler, BASIC-Fehlervariablen gesetzt
	   */
		+ "X_CPM_FILE_WRITE_CHECK_ERR:\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );		// CY=0
	  if( compiler.usesErrorVars() ) {
	    buf.append( "\tPUSH\tHL\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_CPM_FILE_WRITE_CHECK_ERR_1\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,X_CPM_FILE_WRITE_CHECK_ERR_2\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,X_CPM_FILE_WRITE_CHECK_ERR_3\n"
		+ "\tCP\t0FFH\n"
		+ "\tJR\tZ,X_CPM_FILE_WRITE_CHECK_ERR_4\n" );
	    BasicUtil.appendSetError( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_WRITE_CHECK_ERR_5\n"
		+ "X_CPM_FILE_WRITE_CHECK_ERR_1:\n" );
	    BasicUtil.appendSetErrorDirFull( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_WRITE_CHECK_ERR_5\n"
		+ "X_CPM_FILE_WRITE_CHECK_ERR_2:\n" );
	    BasicUtil.appendSetErrorDiskFull( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_WRITE_CHECK_ERR_5\n"
		+ "X_CPM_FILE_WRITE_CHECK_ERR_3:\n" );
	    BasicUtil.appendSetErrorMediaChanged( compiler );
	    buf.append( "\tJR\tX_CPM_FILE_WRITE_CHECK_ERR_5\n"
		+ "X_CPM_FILE_WRITE_CHECK_ERR_4:\n" );
	    BasicUtil.appendSetErrorHardware( compiler );
	    buf.append( "X_CPM_FILE_WRITE_CHECK_ERR_5:\n"
		+ "\tPOP\tHL\n" );
	  }
	  buf.append( "\tSCF\n"
		+ "\tRET\n" );
	}

	/*
	 * Kanalzeigerfeld leeren
	 * Der FCB ist zu dem Zeitpunkt schon gefuellt und wird nicht geleert.
	 *
	 * Parameter:
	 *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
	 */
	buf.append( "X_CPM_FILE_CLEAR_CHANNEL:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n"
		+ "\tLD\tB," );
	buf.appendHex2( IOCTB_FCB_OFFS );
	buf.append( "\n"
		+ "\tXOR\tA\n"
		+ "X_CPM_FILE_CLEAR_CHANNEL_1:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CPM_FILE_CLEAR_CHANNEL_1\n"
		+ "\tRET\n"

	/*
	 * FCB fuellen
	 * Parameter:
	 *   (X_CPM_FILE_M_FNAME): Zeiger auf Dateiname
	 *   (X_CPM_FILE_M_DRIVE): Laufwerk (0: aktuelles, 1: A, usw.)
	 * Rueckgabe:
	 *   CY=0: OK
	 *   CY=1: ungueltiger Name -> Fehlervariablen gesetzt
	 */
		+ "X_CPM_FILE_FILL_FCB:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
	buf.append_LD_BC_nn( IOCTB_FCB_OFFS );
	buf.append( "\tADD\tHL,BC\n"
		+ "\tLD\tA,(X_CPM_FILE_M_DRIVE)\n"
		+ "\tLD\t(HL),A\n"		// Laufwerk
		+ "\tINC\tHL\n"
	/*
	 * Dateinamesfeld mit Leerzeichen
	 * und der Rest mit Nullbytes vorbelegen
	 */
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\tB,0BH\n"
		+ "X_CPM_FILE_FILL_FCB_1:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CPM_FILE_FILL_FCB_1\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,18H\n"
		+ "X_CPM_FILE_FILL_FCB_2:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CPM_FILE_FILL_FCB_2\n"
		+ "\tPOP\tHL\n"
	// Dateiname muss mindestens ein Zeichen haben
		+ "\tLD\tDE,(X_CPM_FILE_M_FNAME)\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CPM_FILE_FILL_FCB_6\n"
	// Dateiname uebertragen
		+ "\tLD\tB,08H\n"
		+ "X_CPM_FILE_FILL_FCB_3:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"			// Dateiname ohne Endung
		+ "\tCP\t2EH\n"
		+ "\tJR\tZ,X_CPM_FILE_FILL_FCB_4\n"
		+ "\tCALL\tX_CPM_FILE_TO_VALID_CHR\n"
		+ "\tJR\tC,X_CPM_FILE_FILL_FCB_6\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CPM_FILE_FILL_FCB_3\n"
	// 8 Zeichen uebertragen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"			// Dateiname ohne Endung
		+ "\tCP\t2EH\n"
		+ "\tJR\tNZ,X_CPM_FILE_FILL_FCB_6\n"
		+ "X_CPM_FILE_FILL_FCB_4:\n"
	// Dateierweiterung uebertragen
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
	buf.append_LD_BC_nn( IOCTB_FTYPE_OFFS );
	buf.append( "\tADD\tHL,BC\n"
		+ "\tLD\tB,03H\n"
		+ "X_CPM_FILE_FILL_FCB_5:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCALL\tX_CPM_FILE_TO_VALID_CHR\n"
		+ "\tJR\tC,X_CPM_FILE_FILL_FCB_6\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CPM_FILE_FILL_FCB_5\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "X_CPM_FILE_FILL_FCB_6:\n" );
	BasicUtil.appendSetErrorInvalidFileName( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"

	/*
	 * Zeichen auf Gueltigkeit in einem Dateinamen pruefen
	 * und in Grossbuchstaben umwandeln
	 * Parameter:
	 *   A: Zeichen
	 * Rueckgabe:
	 *   CY=0: OK
	 *   CY=1: ungueltiges Zeichen
	 */
		+ "X_CPM_FILE_TO_VALID_CHR:\n"
	// auf Ziffern pruefen
		+ "\tCP\t30H\n"
		+ "\tJR\tC,X_CPM_FILE_TO_VALID_CHR_1\n"
		+ "\tCP\t3AH\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
	// auf @ und Grossbuchstaben pruefen
		+ "\tCP\t40H\n"
		+ "\tJR\tC,X_CPM_FILE_TO_VALID_CHR_1\n"
		+ "\tCP\t5BH\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
	// auf Kleinbuchstaben pruefen
		+ "\tCP\t61H\n"
		+ "\tJR\tC,X_CPM_FILE_TO_VALID_CHR_1\n"
		+ "\tCP\t7BH\n"
		+ "\tJR\tNC,X_CPM_FILE_TO_VALID_CHR_1\n"
		+ "\tSUB\t20H\n"
		+ "\tRET\n"
	// auf Sonderzeichen pruefen
		+ "X_CPM_FILE_TO_VALID_CHR_1:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,X_CPM_FILE_DATA_VALID_CHR\n"
		+ "\tLD\tB,A\n"
		+ "X_CPM_FILE_TO_VALID_CHR_2:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tJR\tZ,X_CPM_FILE_TO_VALID_CHR_3\n"
		+ "\tCP\tB\n"
		+ "\tJR\tNZ,X_CPM_FILE_TO_VALID_CHR_2\n"
		+ "X_CPM_FILE_TO_VALID_CHR_3:\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n"

	/*
	 * BDOS-Aufruf mit FCB-Adresse in DE
	 * Paramater:
	 *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
	 *   C:            Nummer der BDOS-Funktion
	 */
		+ "X_CPM_FILE_BDOS:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
	buf.append_LD_DE_nn( IOCTB_FCB_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJP\t0005H\n"
	/*
	 * Setzen der DMA-Adresse im BDOS
	 * Parameter:
	 *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
	 */
		+ "X_CPM_FILE_SET_DMA_ADDR:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
	buf.append_LD_DE_nn( IOCTB_DMA_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tC,1AH\n"
		+ "\tJP\t0005H\n" );
	compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
	done = true;
      }
    }
    if( !done ) {
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf, boolean isAppTypeSub )
  {
    if( isAppTypeSub ) {
      buf.append( "\tRET\n" );
    } else {
      buf.append( "\tJP\t0000H\n" );
    }
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesX_M_INKEY ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n" );
    }
  }


  @Override
  public void appendInputTo(
			AsmCodeBuf buf,
			boolean    xCheckBreak,
			boolean    xInkey,
			boolean    xInch,
			boolean    canBreakOnInput )
  {
    if( xCheckBreak ) {
      if( xInch ) {
	buf.append( "XINCH:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "X_INCHAR_1:\n"
		+ "\tCALL\tX_INKEY_1\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_INCHAR_1\n"
		+ "\tRET\n" );
	xInkey = true;
      }
      buf.append( "XCHECK_BREAK:\n"
		+ "\tLD\tC,06H\n"
		+ "\tLD\tE,0FFH\n"
		+ "\tCALL\t0005H\n"
		+ "\tCP\t03H\n" );
      if( xInkey ) {
	buf.append( "\tJR\tZ,XBREAK\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tRET\n"
		+ "XINKEY:\tLD\tA,(X_M_INKEY)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_INKEY_1\n"
		+ "\tPUSH\tAF\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\n"
		+ "X_INKEY_1:\n"
		+ "\tLD\tC,06H\n"
		+ "\tLD\tE,0FFH\n"
		+ "\tCALL\t0005H\n"
		+ "\tCP\t03H\n" );
	this.usesX_M_INKEY = true;
      }
      buf.append( "\tRET\tNZ\n"
		+ "\tJR\tXBREAK\n" );
    } else {
      if( xInch ) {
	buf.append( "XINCH:\n"
		+ "\tCALL\tXINKEY\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINCH\n"
		+ "\tRET\n" );
	xInkey = true;
      }
      if( xInkey ) {
	buf.append( "XINKEY:\tLD\tC,06H\n"
		+ "\tLD\tE,0FFH\n" );
	if( canBreakOnInput ) {
	  buf.append( "\tCALL\t0005H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
	}
	buf.append( "\tJP\t0005H\n" );
      }
    }
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tA,1BH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tA,E\n"
		+ "\tOR\t80H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\t80H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tRET\n" );
    appendXOutchTo( buf );
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,5\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,02H\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
      this.xOutchAppended = true;
    }
  }


  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,02H\n"
		+ "\tLD\tE,0DH\n"
		+ "\tCALL\t0005H\n"
		+ "\tLD\tC,02H\n"
		+ "\tLD\tE,0AH\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public int get100msLoopCount()
  {
    return 69;
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return new String[] { BASIC_TARGET_NAME };
  }


  @Override
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    int rv = 0;
    if( emuSys != null ) {
      if( (emuSys instanceof NANOS)
	  || (emuSys instanceof PCM) )
      {
	rv = 2;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0100;
  }


  @Override
  public FileFormat getDefaultFileFormat()
  {
    return FileFormat.COM;
  }


  @Override
  public String getDiskHandlerLabel()
  {
    return "X_CPM_FILE_HANDLER";
  }


  @Override
  public int getDiskIOChannelSize()
  {
    return IOCTB_CHANNEL_SIZE;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xFC };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.usesFileRead  = false;
    this.usesFileWrite = false;
    this.usesX_M_INKEY = false;
  }


  @Override
  public boolean startsWithDiskDevice( String fileName )
  {
    boolean rv = false;
    if( fileName != null ) {
      int len = fileName.length();
      if( len > 1 ) {
	char ch = fileName.charAt( 0 );
	if( ((ch >= 'A') && (ch <= 'P'))
	    || ((ch >= 'a') && (ch <= 'p')) )
	{
	  int pos = 1;
	  while( pos < len ) {
	    ch = fileName.charAt( pos++ );
	    if( ch == ':' ) {
	      rv = true;
	      break;
	    }
	    if( (ch < '0') || (ch > '9') ) {
	      break;
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public boolean supportsXLOCATE()
  {
    return true;
  }


  @Override
  public boolean supportsXLPTCH()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return DISPLAY_TARGET_NAME;
  }
}
