/*
 * (c) 2013-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * VDIP-Bibliothek
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Set;


public class VdipLibrary
{
  public static void appendCodeTo( BasicCompiler compiler )
  {
    AbstractTarget            target   = compiler.getTarget();
    AsmCodeBuf                buf      = compiler.getCodeBuf();
    Set<BasicLibrary.LibItem> libItems = compiler.getLibItems();

    /*
     * VDIP-Handler testen
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
    buf.append( "IO_VDIP_HANDLER:\n" );
    boolean      done        = false;
    int[]        baseIOAddrs = target.getVdipBaseIOAddresses();
    Set<Integer> modes       = compiler.getIODriverModes(
						BasicCompiler.IODriver.VDIP );
    if( (baseIOAddrs != null) && (modes != null) ) {
      if( (baseIOAddrs.length > 0) && !modes.isEmpty() ) {
	boolean needsVDIP_RD_FSIZE = false;
	int     baseIOAddr         = -1;
	if( baseIOAddrs.length == 1 ) {
	  baseIOAddr = baseIOAddrs[ 0 ];
	} else {
	  compiler.getLibItems().add( BasicLibrary.LibItem.VDIP_M_IOADDR );
	}
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
			+ "\tCALL\tC_UPR\n"
			+ "\tCP\t56H\n"
			+ "\tJR\tNZ,VDIP_HANDLER2\n"
			+ "\tINC\tDE\n"
			+ "\tLD\tA,(DE)\n"
			+ "\tINC\tDE\n"
			+ "\tCP\t3AH\n"
			+ "\tJR\tZ,VDIP_HANDLER4\n"
			+ "\tDEC\tDE\n"
			+ "\tDEC\tDE\n"
	/*
	 * Am Anfang des Dateinamens steht nicht "V:".
	 * Pruefen, ob ein anderer Geraetename dort steht.
	 */
			+ "\tDEC\tDE\n"
			+ "VDIP_HANDLER1:\n"
			+ "\tINC\tDE\n"
			+ "\tLD\tA,(DE)\n"
			+ "VDIP_HANDLER2:\n"
			+ "\tCP\t3AH\n"
			+ "\tJR\tNZ,VDIP_HANDLER3\n"
			+ "\tSCF\n"			// anderes Geraet
			+ "\tRET\n"
			+ "VDIP_HANDLER3:\n"
			+ "\tOR\tA\n"
			+ "\tJR\tNZ,VDIP_HANDLER1\n"
			+ "\tLD\tDE,(IO_M_NAME)\n"
	// Handler zustaendig, DE zeigt auf den Pfad-/Dateinamen
			+ "VDIP_HANDLER4:\n"
			+ "\tLD\t(VDIP_M_FNAME),DE\n"
	// Geraet bereits in Benutzung?
			+ "\tLD\tA,(VDIP_M_LOCKED)\n"
			+ "\tOR\tA\n"
			+ "\tJR\tZ,VDIP_HANDLER5\n" );
	BasicLibrary.appendSetErrorDeviceLocked( compiler );
	buf.append( "\tOR\tA\n"		// RET mit CY=0!
			+ "\tRET\n"
	// VDIP initialisieren
			+ "VDIP_HANDLER5:\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tCALL\tVDIP_INIT\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n" );	// RET mit CY=0!
	} else {
	  for( int i = 0; i < (baseIOAddrs.length - 1); i++ ) {
	    buf.append_LD_A_n( baseIOAddrs[ i ] );
	    buf.append( "\tLD\t(VDIP_M_IOADDR),A\n"
			+ "\tCALL\tVDIP_INIT\n"
			+ "\tJR\tNC,VDIP_HANDLER6\n" );
	  }
	  buf.append_LD_A_n( baseIOAddrs[ baseIOAddrs.length - 1 ] );
	  buf.append( "\tLD\t(VDIP_M_IOADDR),A\n"
			+ "\tCALL\tVDIP_INIT\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n"		// RET mit CY=0!
			+ "VDIP_HANDLER6:\n" );
	}
	// Speichermedium vorhanden?
	buf.append( "\tCALL\tVDIP_CHECK_DISK\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n"		// RET mit CY=0!
	// ggf. Pfad auswerten und Verzeichnis einstellen
			+ "\tCALL\tVDIP_SET_DIR\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n"		// RET mit CY=0!
			+ "\tLD\tHL,IO_M_ACCESS\n" );
	// beim Lesen und Anhaengen muss die Dateilaenge bekannt sein
	if( txtInput || txtAppend || binInput || binAppend ) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_DEFAULT_MASK
				| BasicLibrary.IOMODE_INPUT_MASK
				| BasicLibrary.IOMODE_APPEND_MASK );
	  buf.append( "\tAND\t(HL)\n"
			+ "\tJR\tZ,VDIP_HANDLER7\n"
			+ "\tPUSH\tHL\n"
			+ "\tCALL\tVDIP_RD_FSIZE\n"
			+ "\tPOP\tHL\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n" );	// RET mit CY=0!
	  needsVDIP_RD_FSIZE = true;
	}

	// Datei oeffnen
	buf.append( "VDIP_HANDLER7:\n" );
	if( txtInput || binInput ) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_DEFAULT_MASK
				| BasicLibrary.IOMODE_INPUT_MASK );
	  buf.append( "\tAND\t(HL)\n"
			+ "\tJR\tNZ,VDIP_HANDLER8\n" );
	}
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  buf.append_LD_A_n( BasicLibrary.IOMODE_OUTPUT_MASK
				| BasicLibrary.IOMODE_APPEND_MASK );
	  buf.append( "\tAND\t(HL)\n"
			+ "\tJR\tNZ,VDIP_HANDLER11\n" );
	}
	BasicLibrary.appendSetErrorIOMode( compiler );
	buf.append( "\tOR\tA\n"			// CY=0
			+ "\tRET\n" );

	// Datei zum Lesen oeffnen
	if( txtInput || binInput ) {
	  buf.append( "VDIP_HANDLER8:\n"
			+ "\tLD\tA,0EH\n"
			+ "\tCALL\tVDIP_WR_FNAME_CMD\n" );
	  buf.append( "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tJR\tNC,VDIP_HANDLER9\n"
			+ "\tLD\tHL,(M_ERN)\n" );
	  buf.append_LD_DE_nn( BasicLibrary.E_ERROR );
	  buf.append( "\tOR\tA\n"
			+ "\tSBC\tHL,DE\n"
			+ "\tSCF\n"
			+ "\tCCF\n"		// CY=0, Z beibehalten
			+ "\tRET\tNZ\n" );
	  BasicLibrary.appendSetErrorFileNotFound( compiler );
	  buf.append( "\tRET\n"
	  // Datei zum Lesen geoeffnet
			+ "VDIP_HANDLER9:\n"
			+ "\tCALL\tVDIP_LOCK_INIT_BUF\n"
	  // Kanalzeigerfeld fuellen
			+ "\tLD\tDE,(IO_M_CADDR)\n"
			+ "\tLD\tHL,VDIP_DATA_RDPTRS\n" );
	  buf.append_LD_BC_nn( BasicLibrary.IOCTB_WRITE_OFFS );
	  buf.append( "\tLDIR\n"
			+ "\tXOR\tA\n"
			+ "\tLD\tB," );
	  buf.appendHex2( compiler.getIOChannelSize()
					- BasicLibrary.IOCTB_WRITE_OFFS );
	  buf.append( "\n"
			+ "VDIP_HANDLER10:\n"
			+ "\tLD\t(DE),A\n"
			+ "\tINC\tDE\n"
			+ "\tDJNZ\tVDIP_HANDLER10\n"
			+ "\tOR\tA\n"
			+ "\tRET\n" );
	  libItems.add( BasicLibrary.LibItem.VDIP_DATA_RDPTRS );
	}

	// Datei zum Schreiben oeffnen
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  buf.append( "VDIP_HANDLER11:\n"
			+ "\tLD\tA,09H\n"
			+ "\tCALL\tVDIP_WR_FNAME_CMD\n"
			+ "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n" );		// RET mit CY=0!
	  if( (txtOutput || binOutput) && (txtAppend || binAppend) ) {
	    buf.append( "\tLD\tA,(IO_M_ACCESS)\n"
			+ "\tAND\t" );
	    buf.appendHex2( BasicLibrary.IOMODE_TXT_APPEND
				| BasicLibrary.IOMODE_BIN_APPEND );
	    buf.append( "\n"
			+ "\tJR\tNZ,VDIP_HANDLER12\n" );
	  }
	  if( txtOutput || binOutput ) {
	    // Dateizeiger auf den Dateianfang setzen
	    buf.append( "\tCALL\tVDIP_WR_CMD\n"
			+ "\tDB\t28H,20H,00H,00H,00H,00H,0DH\n" );
	  }
	  if( (txtOutput || binOutput) && (txtAppend || binAppend) ) {
	    buf.append( "\tJR\tVDIP_HANDLER14\n"
			+ "VDIP_HANDLER12:\n" );
	  }
	  if( txtAppend || binAppend ) {
	    /*
	     * Dateizeiger auf das Dateiende setzen
	     * hoechstwertiges Byte zuerst!!!
	     */
	    buf.append( "\tLD\tA,28H\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tA,20H\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tHL,VDIP_M_FBYTES+4\n"
			+ "\tLD\tB,04H\n"
			+ "VDIP_HANDLER13:\n"
			+ "\tDEC\tHL\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tPUSH\tBC\n"
			+ "\tPUSH\tHL\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tPOP\tHL\n"
			+ "\tPOP\tBC\n"
			+ "\tDJNZ\tVDIP_HANDLER13\n"
			+ "\tLD\tA,0DH\n"
			+ "\tCALL\tVDIP_WR_BYTE\n" );
	  }

	  // Dateizeiger gesetzt -> Ergebnis pruefen
	  buf.append( "VDIP_HANDLER14:\n"
			+ "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tCCF\n"
			+ "\tRET\tNC\n"		// RET mit CY=0!
	  // Datei zum Schreiben geoeffnet
			+ "\tCALL\tVDIP_LOCK_INIT_BUF\n"
	  // Kanalzeigerfeld fuellen
			+ "\tLD\tHL,(IO_M_CADDR)\n"
			+ "\tLD\tDE,VDIP_CLOSE\n"
			+ "\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n"
			+ "\tINC\tHL\n"
			+ "\tXOR\tA\n"
			+ "\tLD\tB," );
	  buf.appendHex2( BasicLibrary.IOCTB_WRITE_OFFS - 2 );
	  buf.append( "\n"
			+ "VDIP_HANDLER15:\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\tVDIP_HANDLER15\n"
			+ "\tLD\tDE,VDIP_WRITE\n"
			+ "\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),A\n"
			+ "\tOR\tA\n"		// CY=0
			+ "\tRET\n" );
	}
	/*
	 * VDIP initialisieren
	 * Rueckgabewert:
	 *  CY=0: OK
	 *  CY=1: Fehler, Fehlervariablen auf E_DEVICE_NOT_FOUND gesetzt
	 */
	buf.append( "VDIP_INIT:\n"
			+ "\tLD\tA,(VDIP_M_INIT)\n"
			+ "\tOR\tA\n"
			+ "\tRET\tNZ\n" );
	// VDIP aktiviern
	if( target.needsEnableVdip() ) {
	  target.appendEnableVdipTo( buf );
	  buf.append( "\tJR\tC,VDIP_INIT_ERR\n" );
	}
	// PIO initialisieren
	buf.append( "\tLD\tHL,VDIP_DATA_PIO_INIT\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append_LD_BC_nn( 0x0500 | (baseIOAddr + 3) );
	} else {
	  buf.append( "\tLD\tA,(VDIP_M_IOADDR)\n"
			+ "\tADD\tA,03H\n"
			+ "\tLD\tC,A\n"
			+ "\tLD\tB,05H\n" );
	}
	buf.append( "\tOTIR\n"
			+ "\tDEC\tC\n"
			+ "\tDEC\tC\n"
			+ "\tLD\tB,01H\n"
			+ "\tOTIR\n"
			+ "\tINC\tC\n"
			+ "\tLD\tB,02H\n"
			+ "\tOTIR\n"
	// VDIP-Anfangsmeldung ueberlesen
			+ "VDIP_INIT1:\n"
			+ "\tCALL\tVDIP_RD_BYTE_T\n"
			+ "\tJR\tNC,VDIP_INIT1\n"
	// Echo testen
			+ "\tLD\tA,\'E\'\n"
			+ "\tCALL\tVDIP_WR_BYTE_T\n"
			+ "\tJR\tC,VDIP_INIT_ERR\n"
			+ "\tLD\tA,0DH\n"
			+ "\tCALL\tVDIP_WR_BYTE_T\n"
			+ "\tJR\tC,VDIP_INIT_ERR\n"
			+ "\tCALL\tVDIP_RD_BYTE_T\n"
			+ "\tJR\tC,VDIP_INIT_ERR\n"
			+ "\tCP\t\'E\'\n"
			+ "\tJR\tNZ,VDIP_INIT_ERR\n"
			+ "\tCALL\tVDIP_RD_BYTE_T\n"
			+ "\tJR\tC,VDIP_INIT_ERR\n"
			+ "\tCP\t0DH\n"
			+ "\tJR\tNZ,VDIP_INIT_ERR\n"
	// Short Command Set einstellen
			+ "\tCALL\tVDIP_WR_CMD\n"
			+ "\tDB\t10H,0DH\n"
			+ "\tCALL\tVDIP_INIT2\n"
			+ "\tRET\tC\n"
	// Binaer-Mode einstellen
			+ "\tCALL\tVDIP_WR_CMD\n"
			+ "\tDB\t91H,0DH\n"
	// Prompt pruefen
			+ "VDIP_INIT2:\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCP\t3EH\n"
			+ "\tJR\tNZ,VDIP_INIT_ERR\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCP\t0DH\n"
			+ "\tJR\tNZ,VDIP_INIT_ERR\n"
			+ "\tLD\tA,01H\n"
			+ "\tLD\t(VDIP_M_INIT),A\n"
			+ "\tRET\n"
			+ "VDIP_INIT_ERR:\n" );
	BasicLibrary.appendSetErrorDeviceNotFound( compiler );
	buf.append( "\tSCF\n"
			+ "\tRET\n"
	// VDIP sperren und Puffer initialisieren
			+ "VDIP_LOCK_INIT_BUF:\n"
			+ "\tLD\tHL,VDIP_BUF\n"
			+ "\tLD\t(VDIP_M_BPTR),HL\n"
			+ "\tXOR\tA\n"
			+ "\tLD\t(VDIP_M_BBYTES),A\n"
			+ "\tINC\tA\n"
			+ "\tLD\t(VDIP_M_LOCKED),A\n"
			+ "\tRET\n"
	/*
	 * CLOSE-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 */
			+ "VDIP_CLOSE:\n"
			+ "\tLD\tA,(VDIP_M_LOCKED)\n"
			+ "\tOR\tA\n"
			+ "\tRET\tZ\n" );
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  if( txtInput || binInput ) {
	    buf.append_LD_HL_nn( BasicLibrary.IOCTB_WRITE_OFFS );
	    buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,VDIP_CLOSE1\n" );
	  }
	  buf.append( "\tCALL\tVDIP_FLUSH\n"
		+ "VDIP_CLOSE1:\n" );
	}
	buf.append( "\tLD\tA,0AH\n"
			+ "\tCALL\tVDIP_WR_FNAME_CMD\n"
			+ "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tRET\tC\n"
			+ "\tXOR\tA\n"
			+ "\tLD\t(VDIP_M_LOCKED),A\n"
			+ "\tRET\n"
	/*
	 * EOF-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   HL: -1: Dateiende erreicht
	 */
			+ "VDIP_EOF:\n" );
	if( txtInput || binInput ) {
	  buf.append( "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tOR\tA\n"
			+ "\tJR\tNZ,VDIP_EOF2\n"
			+ "\tLD\tHL,VDIP_M_FBYTES\n"
			+ "\tLD\tB,04H\n"
			+ "VDIP_EOF1:\n"
			+ "\tOR\t(HL)\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\tVDIP_EOF1\n"
			+ "VDIP_EOF2:\n"
			+ "\tLD\tHL,0000H\n"
			+ "\tOR\tA\n"
			+ "\tRET\tNZ\n"
			+ "\tDEC\tHL\n" );
	} else {
	  buf.append( "\tLD\tHL,0FFFFH\n" );
	}
	buf.append( "\tRET\n"
	/*
	 * READ-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   A: gelesenes Zeichen
	 */
			+ "VDIP_READ:\n" );
	if( txtInput || binInput ) {
	  // noch Bytes im Puffer?
	  buf.append( "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tOR\tA\n"
			+ "\tJR\tNZ,VDIP_READ7\n"
	  /*
	   * keine Bytes mehr im Puffer
	   * 128 von der Anzahl der restlichen Dateibytes subtrahieren
	   */
			+ "\tLD\tHL,VDIP_M_FBYTES\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tLD\tD,A\n"
			+ "\tSUB\t80H\n"
			+ "\tLD\t(HL),A\n"
			+ "\tJR\tNC,VDIP_READ3\n"	// kein Ueberlauf
			+ "\tLD\tBC,0300H\n"
			+ "VDIP_READ1:\n"
			+ "\tINC\tHL\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tSBC\tA,C\n"
			+ "\tLD\t(HL),A\n"
			+ "\tDJNZ\tVDIP_READ1\n"
			+ "\tJR\tNC,VDIP_READ3\n"	// kein Ueberlauf
	  /*
	   * weniger als 128 Bytes verfuegbar
	   * Anzahl in D
	   */
			+ "\tLD\tHL,VDIP_M_FBYTES\n"
			+ "\tXOR\tA\n"
			+ "\tLD\tB,04H\n"
			+ "VDIP_READ2:\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\tVDIP_READ2\n"
			+ "\tLD\tA,D\n"
			+ "\tOR\tA\n"
			+ "\tJR\tNZ,VDIP_READ4\n"
	  // keine restlichen Bytes mehr
			+ "\tLD\t(VDIP_M_BBYTES),A\n" );
	  BasicLibrary.appendSetErrorEOF( compiler );
	  buf.append( "\tRET\n"
	  // 128 Bytes lesen
			+ "VDIP_READ3:\n"
			+ "\tLD\tA,80H\n"
	  // Bytes lesen, Anzahl in A
			+ "VDIP_READ4:\n"
			+ "\tLD\t(VDIP_M_BBYTES),A\n"
			+ "\tEX\tAF,AF\'\n"
			+ "\tLD\tA,0BH\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tA,20H\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tB,03H\n"
			+ "VDIP_READ5:\n"
			+ "\tPUSH\tBC\n"
			+ "\tXOR\tA\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tPOP\tBC\n"
			+ "\tDJNZ\tVDIP_READ5\n"
			+ "\tEX\tAF,AF\'\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tA,0DH\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tHL,VDIP_BUF\n"
			+ "\tLD\t(VDIP_M_BPTR),HL\n"
			+ "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tLD\tB,A\n"
			+ "VDIP_READ6:\n"
			+ "\tPUSH\tBC\n"
			+ "\tPUSH\tHL\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tPOP\tHL\n"
			+ "\tPOP\tBC\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\tVDIP_READ6\n"
			+ "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "VDIP_READ7:\n"
			+ "\tDEC\tA\n"
			+ "\tLD\t(VDIP_M_BBYTES),A\n"
			+ "\tLD\tHL,(VDIP_M_BPTR)\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(VDIP_M_BPTR),HL\n" );
	} else {
	  buf.append( "\tXOR\tA\n" );
	}
	buf.append( "\tRET\n"
	/*
	 * WRITE-Routine
	 * Parameter:
	 *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
	 *   A:            zu schreibendes Byte
	 */
			+ "VDIP_WRITE:\n" );
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  buf.append( "\tLD\tHL,(VDIP_M_BPTR)\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(VDIP_M_BPTR),HL\n"
			+ "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tINC\tA\n"
			+ "\tLD\t(VDIP_M_BBYTES),A\n"
			+ "\tRET\tP\n" );
	  // direkt weiter mit der FLUSH-Routine!
	}
	/*
	 * FLUSH-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 */
	buf.append( "VDIP_FLUSH:\n" );
	if( txtOutput || txtAppend || binOutput || binAppend ) {
	  buf.append( "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tOR\tA\n"
			+ "\tRET\tZ\n"
			+ "\tPUSH\tAF\n"
			+ "\tLD\tA,08H\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tA,20H\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tB,03H\n"
			+ "VDIP_FLUSH1:\n"
			+ "\tPUSH\tBC\n"
			+ "\tXOR\tA\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tPOP\tBC\n"
			+ "\tDJNZ\tVDIP_FLUSH1\n"
			+ "\tPOP\tAF\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tA,0DH\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tLD\tHL,VDIP_BUF\n"
			+ "\tLD\tA,(VDIP_M_BBYTES)\n"
			+ "\tLD\tB,A\n"
			+ "VDIP_FLUSH2:\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tINC\tHL\n"
			+ "\tPUSH\tBC\n"
			+ "\tPUSH\tHL\n"
			+ "\tCALL\tVDIP_WR_BYTE\n"
			+ "\tPOP\tHL\n"
			+ "\tPOP\tBC\n"
			+ "\tDJNZ\tVDIP_FLUSH2\n"
			+ "\tCALL\tVDIP_CHECK_RESULT\n"
			+ "\tRET\tC\n"
			+ "\tXOR\tA\n"
			+ "\tLD\t(VDIP_M_BBYTES),A\n"
			+ "\tLD\tHL,VDIP_BUF\n"
			+ "\tLD\t(VDIP_M_BPTR),HL\n" );
	}
	buf.append( "\tRET\n" );
	/*
	 * Lesen der Dateigroesse
	 * Paramater:
	 *   (VDIP_M_FNAME): Dateiname
	 * Rueckgabewerte:
	 *   CY=0: OK, Dateigroesse steht in (VDIP_M_FBYTES)
	 *   CY=1: Fehler
	 */
	if( needsVDIP_RD_FSIZE ) {
	  buf.append( "VDIP_RD_FSIZE:\n"
			+ "\tLD\tA,01H\n"	// Kommando DIR
			+ "\tCALL\tVDIP_WR_FNAME_CMD\n"
	  // Leerzeile pruefen
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCP\t0DH\n"
			+ "\tJR\tNZ,VDIP_RD_FSIZE4\n"
	  // vom DIR-Kommando zurueckgelieferten Dateinamen vergleichen
			+ "\tLD\tHL,(VDIP_M_FNAME)\n"
			+ "VDIP_RD_FSIZE1:\n"
			+ "\tLD\tA,(HL)\n"
			+ "\tINC\tHL\n"
			+ "\tOR\tA\n"
			+ "\tJR\tZ,VDIP_RD_FSIZE2\n"
			+ "\tCALL\tC_UPR\n"
			+ "\tPUSH\tAF\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCALL\tC_UPR\n"
			+ "\tLD\tB,A\n"
			+ "\tPOP\tAF\n"
			+ "\tCP\tB\n"
			+ "\tJR\tZ,VDIP_RD_FSIZE1\n" );
	  // Dateiname stimmt nicht ueberein
	  BasicLibrary.appendSetErrorFileNotFound( compiler );
	  buf.append( "\tJR\tVDIP_RD_FSIZE5\n"
	  // Leerzeichen hinter Dateinamen pruefen
			+ "VDIP_RD_FSIZE2:\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCP\t20H\n"
			+ "\tJR\tNZ,VDIP_RD_FSIZE4\n"
	  // Dateigroesse lesen
			+ "\tLD\tHL,VDIP_M_FBYTES\n"
			+ "\tLD\tB,04H\n"
			+ "VDIP_RD_FSIZE3:\n"
			+ "\tPUSH\tBC\n"
			+ "\tPUSH\tHL\n"
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tPOP\tHL\n"
			+ "\tPOP\tBC\n"
			+ "\tLD\t(HL),A\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\tVDIP_RD_FSIZE3\n"
	  // Zeilenendezeichen lesen
			+ "\tCALL\tVDIP_RD_BYTE\n"
			+ "\tCP\t0DH\n"
			+ "\tJR\tNZ,VDIP_RD_FSIZE4\n"
	  // Kommando erfolgreich?
			+ "\tJR\tVDIP_CHECK_RESULT\n"
	  // Fehler beim DIR-Kommando -> restliche Bytes lesen und Abbruch
			+ "VDIP_RD_FSIZE4:\n" );
	  BasicLibrary.appendSetErrorIOError( compiler );
	  buf.append( "VDIP_RD_FSIZE5:\n"
			+ "\tCALL\tVDIP_RD_BYTE_T\n"
			+ "\tRET\tC\n"
			+ "\tCP\t0DH\n"
			+ "\tJR\tNZ,VDIP_RD_FSIZE5\n"
			+ "\tSCF\n"
			+ "\tRET\n" );
	}
	/*
	 * Vorhandensein des Speichermediums pruefen
	 * Rueckgabewert:
	 *   CY=0: OK
	 *   CY=1: Fehler, Fehlervariablen gesetzt
	 */
	buf.append( "VDIP_CHECK_DISK:\n"
		+ "\tCALL\tVDIP_WR_CMD\n"
		+ "\tDB\t0DH\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,VDIP_CHECK_DISK2\n"
		+ "\tEX\tAF,AF\'\n"
		+ "VDIP_CHECK_DISK1:\n"
		+ "\tCALL\tVDIP_RD_BYTE_T\n"
		+ "\tJR\tC,VDIP_CHECK_DISK2\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tNZ,VDIP_CHECK_DISK1\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCP\t3EH\n"
		+ "\tRET\tZ\n"		// Disk vorhanden
		+ "\tCP\t4EH\n"
		+ "\tJR\tNZ,VDIP_CHECK_DISK2\n" );
	BasicLibrary.appendSetErrorNoDisk( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_CHECK_DISK2:\n" );
	BasicLibrary.appendSetError( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
	/*
	 * Ergebnis auswerten
	 * Rueckgabewerte:
	 *   CY=0: keine Fehler
	 *   CY=1: Fehler, Fehlervariablen gesetzt
	 */
		+ "VDIP_CHECK_RESULT:\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,VDIP_CHECK_RESULT2\n"
		+ "\tLD\tD,A\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tE,A\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,VDIP_CHECK_RESULT2\n"
		+ "\tPUSH\tDE\n"
		+ "VDIP_CHECK_RESULT1:\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tNZ,VDIP_CHECK_RESULT1\n"
		+ "\tPOP\tDE\n"
		+ "VDIP_CHECK_RESULT2:\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t3EH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,4446H\n"		// DF: Disk Full
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,VDIP_CHECK_RESULT4\n" );
	BasicLibrary.appendSetErrorDiskFull( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_CHECK_RESULT4:\n"
		+ "\tLD\tHL,4649H\n"		// FI: Invalid
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,VDIP_CHECK_RESULT5\n" );
	BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_INVALID,
			"Ungueltiger Parameter",
			"Invalid parameter" );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_CHECK_RESULT5:\n"
		+ "\tLD\tHL,524FH\n"		// RO: Read Only
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,VDIP_CHECK_RESULT6\n" );
	BasicLibrary.appendSetErrorFileReadOnly( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_CHECK_RESULT6:\n"
		+ "\tLD\tHL,464FH\n"		// FO: File Open
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,VDIP_CHECK_RESULT7\n" );
	BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_DEVICE_LOCKED,
			"Datei offen",
			"File open" );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_CHECK_RESULT7:\n" );
	BasicLibrary.appendSetError( compiler );
	buf.append( "\tSCF\n"
		+ "\tRET\n"
	/*
	 * Pfad des vollstaendigen Dateinamens einstellen
	 * Parameter:
	 *   (VDIP_M_FNAME): Dateiname inkl. optionalen Pfad
	 * Rueckgabewerte:
	 *   (VDIP_M_FNAME): Dateiname ohne Pfad
	 *   CY=1: Fehler
	 */
		+ "VDIP_SET_DIR:\n"
		+ "\tLD\tDE,(VDIP_M_FNAME)\n"
	// absolute Pfadangabe?
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t2FH\n"			// Slash
		+ "\tJR\tZ,VDIP_SET_DIR1\n"
		+ "\tCP\t5CH\n"			// Backslash
		+ "\tJR\tNZ,VDIP_SET_DIR4\n"
	// in das oberste Verzeichnis wechseln
		+ "VDIP_SET_DIR1:\n"
		+ "\tINC\tDE\n"
		+ "VDIP_SET_DIR2:\n"
		+ "\tCALL\tVDIP_WR_CMD\n"	// CD ..
		+ "\tDB\t02H,20H,2EH,2EH,0DH\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"	// Ergebnis auswerten
		+ "\tCP\t3EH\n"
		+ "\tJR\tNZ,VDIP_SET_DIR3\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"	// Ergebnis auswerten
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,VDIP_SET_DIR2\n"
		+ "VDIP_SET_DIR3:\n"		// Fehler -> Bytes lesen
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,VDIP_SET_DIR4\n"
		+ "\tCALL\tVDIP_RD_BYTE\n"
		+ "\tJR\tVDIP_SET_DIR3\n"
	// in Verzeichnis wechseln
		+ "VDIP_SET_DIR4:\n"
		+ "\tLD\tB,0FFH\n"
		+ "\tLD\tH,D\n"
		+ "\tLD\tL,E\n"
		+ "VDIP_SET_DIR5:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tB\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,VDIP_SET_DIR10\n"	// kein Pfadelement
		+ "\tCP\t0DH\n"			// Zeilenendezeichen
		+ "\tJR\tZ,VDIP_SET_DIR8\n"	// nicht erlaubt
		+ "\tCP\t2FH\n"			// Slash
		+ "\tJR\tZ,VDIP_SET_DIR6\n"
		+ "\tCP\t5CH\n"			// Backslash
		+ "\tJR\tNZ,VDIP_SET_DIR5\n"
		+ "VDIP_SET_DIR6:\n"
		+ "\tXOR\tA\n"
		+ "\tOR\tB\n"
		+ "\tJR\tZ,VDIP_SET_DIR8\n"	// leeres Pfadelement
		+ "\tPUSH\tBC\n"
		+ "\tLD\tA,02H\n"		// Kommando CD
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tPOP\tBC\n"
		+ "VDIP_SET_DIR7:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tPOP\tBC\n"
		+ "\tDJNZ\tVDIP_SET_DIR7\n"
		+ "\tINC\tDE\n"			// Trennzeichen uebergehen
		+ "\tLD\tA,0DH\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tVDIP_CHECK_RESULT\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tNC,VDIP_SET_DIR4\n" );
	// bei Command Failed (E_ERROR) wurde Pfad nicht gefunden
	if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERN ) ) {
	  buf.append_LD_DE_nn( BasicLibrary.E_ERROR );
	  buf.append( "\tLD\tHL,(M_ERN)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,VDIP_SET_DIR9\n" );
	}
	buf.append( "VDIP_SET_DIR8:\n" );	// Pfad nicht gefunden
	BasicLibrary.appendSetError(
		compiler,
		BasicLibrary.E_PATH_NOT_FOUND,
		"Pfad nicht gefunden",
		"Path not found" );
	buf.append( "VDIP_SET_DIR9:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "VDIP_SET_DIR10:\n"
		+ "\tLD\t(VDIP_M_FNAME),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
	/*
	 * Kommando mit angehaengtem Dateinamen senden
	 * Parameter:
	 *   A:              Kommando
	 *   (VDIP_M_FNAME): Dateiname
	 */
		+ "VDIP_WR_FNAME_CMD:\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tLD\tHL,(VDIP_M_FNAME)\n"
		+ "\tLD\tA,20H\n"
		+ "VDIP_WR_FNAME_CMD1:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,VDIP_WR_FNAME_CMD1\n"
		+ "\tLD\tA,0DH\n"
		+ "\tJR\tVDIP_WR_BYTE\n"
	/*
	 * Kommando senden
	 * Die Kommandobytes stehen hinter dem CALL-Befehl
	 */
		+ "VDIP_WR_CMD:\n"
		+ "\tEX\t(SP),HL\n"
		+ "VDIP_WR_CMD1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tAF\n"
		+ "\tCALL\tVDIP_WR_BYTE\n"
		+ "\tPOP\tAF\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tNZ,VDIP_WR_CMD1\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n"
	/*
	 * Lesen eines Bytes vom VDIP-Modul
	 * Rueckgabewerte:
	 *  A: gelesenes Byte
	 */
		+ "VDIP_RD_BYTE:\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tLD\tC," );
	  buf.appendHex2( baseIOAddr + 1 );
	  buf.append( "\n" );
	} else {
	  buf.append( "\tLD\tA,(VDIP_M_IOADDR)\n"
		+ "\tINC\tA\n"
		+ "\tLD\tC,A\n" );
	}
	buf.append( "VDIP_RD_BYTE1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tRRCA\n"
		+ "\tJR\tC,VDIP_RD_BYTE1\n"
		+ "VDIP_RD_BYTE2:\n"
		+ "\tLD\tA,0F3H\n"
		+ "\tOUT\t(C),A\n"
		+ "\tDEC\tC\n"
		+ "\tIN\tA,(C)\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,0F7H\n"
		+ "\tINC\tC\n"
		+ "\tOUT\t(C),A\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"			// CY=0: OK
		+ "\tRET\n"
	/*
	 * Lesen eines Bytes vom VDIP-Modul mit Timeout-Check
	 * Rueckgabewerte:
	 *  CY=0: OK, gelesenes Byte in A
	 *  CY=1: Fehler (Timeout)
	 */
		+ "VDIP_RD_BYTE_T:\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tLD\tC," );
	  buf.appendHex2( baseIOAddr + 1 );
	  buf.append( "\n" );
	} else {
	  buf.append( "\tLD\tA,(VDIP_M_IOADDR)\n"
		+ "\tINC\tA\n"
		+ "\tLD\tC,A\n" );
	}
	buf.append( "\tLD\tDE,0000H\n"
		+ "VDIP_RD_BYTE_T1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tRRCA\n"
		+ "\tJR\tNC,VDIP_RD_BYTE2\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,VDIP_RD_BYTE_T1\n"
		+ "\tSCF\n"			// CY=1: Timeout
		+ "\tRET\n"
	/*
	 * Schreiben eines Bytes in das VDIP-Modul:
	 * Parameter:
	 *  A: Byte
	 */
		+ "VDIP_WR_BYTE:\n"
		+ "\tLD\tB,A\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tLD\tC," );
	  buf.appendHex2( baseIOAddr );
	  buf.append( "\n" );
	} else {
	  buf.append( "\tLD\tA,(VDIP_M_IOADDR)\n"
			+ "\tLD\tC,A\n" );
	}
	buf.append( "\tOUT\t(C),B\n"
		+ "\tINC\tC\n"
		+ "VDIP_WR_BYTE1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tJR\tC,VDIP_WR_BYTE1\n"
		+ "VDIP_WR_BYTE2:\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tOUT\t(C),A\n"
		+ "\tLD\tA,0F7H\n"
		+ "\tOUT\t(C),A\n"
		+ "\tOR\tA\n"			// CY=0: OK
		+ "\tRET\n"
	/*
	 * Schreiben eines Bytes in das VDIP-Modul mit Timeout-Check
	 * Parameter:
	 *  A: Byte
	 * Rueckgabewerte:
	 *  CY=0: OK, gelesenes Byte in A
	 *  CY=1: Fehler (Timeout)
	 */
		+ "VDIP_WR_BYTE_T:\n"
		+ "\tLD\tB,A\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tLD\tC," );
	  buf.appendHex2( baseIOAddr );
	  buf.append( "\n" );
	} else {
	  buf.append( "\tLD\tA,(VDIP_M_IOADDR)\n"
		+ "\tLD\tC,A\n" );
	}
	buf.append( "\tOUT\t(C),B\n"
		+ "\tINC\tC\n"
		+ "\tLD\tDE,0000H\n"
		+ "VDIP_WR_BYTE_T1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tJR\tNC,VDIP_WR_BYTE2\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,VDIP_WR_BYTE_T1\n"
		+ "\tSCF\n"			// CY=1: Timeout
		+ "\tRET\n" );
	libItems.add( BasicLibrary.LibItem.VDIP_INIT );
	libItems.add( BasicLibrary.LibItem.C_UPR );
	libItems.add( BasicLibrary.LibItem.M_ERN );
	libItems.add( BasicLibrary.LibItem.M_ERN );
	done = true;
      }
    }
    if( !done ) {
      buf.append( "\tSCF\n"		// CY=1: Handler nicht zustaendig
		+ "\tRET\n" );
    }
  }


  public static void appendDataTo( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.VDIP_INIT ) ) {
      compiler.getCodeBuf().append( "VDIP_DATA_PIO_INIT:\n"
		+ "\tDB\t0CFH,23H,0D0H,17H,0FFH\n"	// PIO B Ctrl
		+ "\tDB\t0D4H\n"			// PIO B Data
		+ "\tDB\t8FH,07H\n" );			// PIO A Ctrl
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.VDIP_INIT ) ) {
      compiler.getCodeBuf().append( "VDIP_DATA_RDPTRS:\n"
		+ "\tDW\tVDIP_CLOSE\n"
		+ "\tDW\tVDIP_EOF\n"
		+ "\tDW\tVDIP_READ\n" );
    }
  }


  public static void appendBssTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.VDIP_M_IOADDR ) ) {
      buf.append( "VDIP_M_IOADDR:\n"
		+ "\tDS\t1\n" );
    }
    buf.append( "VDIP_M_INIT:\tDS\t1\n"
		+ "VDIP_M_LOCKED:\tDS\t1\n"
		+ "VDIP_M_FNAME:\tDS\t2\n"
		+ "VDIP_M_FBYTES:\tDS\t4\n"
		+ "VDIP_M_BBYTES:\tDS\t1\n"
		+ "VDIP_M_BPTR:\tDS\t2\n"
		+ "VDIP_BUF:\tDS\t80H\n" );
  }


  public static void appendInitTo( AsmCodeBuf buf )
  {
    buf.append( "\tXOR\tA\n"
		+ "\tLD\t(VDIP_M_INIT),A\n"
		+ "\tLD\t(VDIP_M_LOCKED),A\n" );
  }
}
