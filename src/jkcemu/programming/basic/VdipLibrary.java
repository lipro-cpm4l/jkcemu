/*
 * (c) 2013 Jens Mueller
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
    AsmCodeBuf                buf      = compiler.getCodeBuf();
    Set<BasicLibrary.LibItem> libItems = compiler.getLibItems();
    AbstractTarget            target   = compiler.getTarget();

    /*
     * VDIP-Handler testen
     *
     * Parameter:
     *   (M_IONM): Zeiger auf Geraete-/Dateiname
     *   (M_IOCA): Anfangsadresse des Kanalzeigerfeldes
     *   (M_IOAC): Zugriffsmode
     * Rueckgabewert:
     *   CY=1:     Handler ist nicht zustaendig
     *   CY=0:     Handler ist zustaendig
     *               -> keine weiteren Handler testen
     */
    buf.append( "IO_VDIP_HANDLER:\n" );
    boolean done        = false;
    int[]   baseIOAddrs = target.getVdipBaseIOAddresses();
    if( baseIOAddrs != null ) {
      if( baseIOAddrs.length > 0 ) {
	int baseIOAddr = -1;
	if( baseIOAddrs.length == 1 ) {
	  baseIOAddr = baseIOAddrs[ 0 ];
	} else {
	  compiler.getLibItems().add( BasicLibrary.LibItem.VDIP_M_IOADDR );
	}
	// Geraetename am Dateianfang testen
	buf.append( "\tLD\tDE,(M_IONM)\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCALL\tC_UPR\n"
		+ "\tCP\t\'V\'\n"
		+ "\tJR\tNZ,VDIP_HANDLER1\n"
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
		+ "VDIP_HANDLER1:\n"
		+ "\tDEC\tDE\n"
		+ "VDIP_HANDLER2:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tNZ,VDIP_HANDLER3\n"
		+ "\tSCF\n"			// anderes Geraet
		+ "\tRET\n"
		+ "VDIP_HANDLER3:\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,VDIP_HANDLER2\n"
		+ "\tLD\tDE,(M_IONM)\n"
	// Handler zustaendig, DE zeigt auf den Pfad-/Dateinamen
		+ "VDIP_HANDLER4:\n"
		+ "\tLD\t(VDIP_M_FNAME),DE\n"
	// Geraet bereits in Benutzung?
		+ "\tLD\tA,(VDIP_M_LOCKED)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,VDIP_HANDLER5\n" );
	BasicLibrary.appendSetErrorDeviceLocked( compiler );
	buf.append( "\tOR\tA\n"			// RET mit CY=0!
		+ "\tRET\n"
	// VDIP initialisieren
		+ "VDIP_HANDLER5:\n" );
	if( baseIOAddr >= 0 ) {
	  buf.append( "\tCALL\tVDIP_INIT\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n" );		// RET mit CY=0!
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
		+ "\tRET\tNC\n"			// RET mit CY=0!
		+ "VDIP_HANDLER6:\n" );
	}
	// Speichermedium vorhanden?
	buf.append( "\tCALL\tVDIP_CHECK_DISK\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"			// RET mit CY=0!
// TODO: Pfad auswerten und aktuelles Verzeichnis einstellen
	// beim Lesen und Anhaengen muss die Dateilaenge bekannt sein
		+ "\tLD\tA,(M_IOAC)\n"
		+ "\tCP\t" );
	buf.appendHex2( BasicLibrary.IOMODE_OUTPUT );
	buf.append( "\n"
		+ "\tJR\tZ,VDIP_HANDLER7\n"
		+ "\tCALL\tVDIP_RD_FSIZE\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"			// RET mit CY=0!
	// Datei oeffnen
		+ "VDIP_HANDLER7:\n"
		+ "\tLD\tA,(M_IOAC)\n"
		+ "\tCP\t" );
	buf.appendHex2( BasicLibrary.IOMODE_OUTPUT );
	buf.append( "\n"
		+ "\tJR\tNC,VDIP_HANDLER10\n"	// Ausgabe
	// Datei im VDIP oeffnen
		+ "\tLD\tA,0EH\n"
		+ "\tCALL\tVDIP_WR_FNAME_CMD\n" );
	buf.append( "\tCALL\tVDIP_CHECK_RESULT\n"
		+ "\tJR\tNC,VDIP_HANDLER8\n"
		+ "\tLD\tHL,(M_ERN)\n" );
	buf.append_LD_DE_nn( BasicLibrary.E_ERROR );
	buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tSCF\n"
		+ "\tCCF\n"			// CY=0, Z beibehalten
		+ "\tRET\tNZ\n" );
	BasicLibrary.appendSetErrorFileNotFound( compiler );
	buf.append( "\tRET\n"
	// Datei zum Lesen geoeffnet
		+ "VDIP_HANDLER8:\n"
		+ "\tCALL\tVDIP_LOCK_INIT_BUF\n"
	// Kanalzeigerfeld fuellen
		+ "\tLD\tDE,(M_IOCA)\n"
		+ "\tLD\tHL,VDIP_DATA_RDPTRS\n"
		+ "\tLD\tBC,0008H\n"
		+ "\tLDIR\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB," );
	buf.appendHex2( compiler.getIOChannelSize()
				- BasicLibrary.IOCTB_READ_OFFS
				- 2 );
	buf.append( "\n"
		+ "VDIP_HANDLER9:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tVDIP_HANDLER9\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
	// Datei zum Schreiben oeffnen
		+ "VDIP_HANDLER10:\n"
		+ "\tJR\tZ,VDIP_HANDLER11\n"
		+ "\tCP\t" );
	buf.appendHex2( BasicLibrary.IOMODE_APPEND );
	buf.append( "\n"
		+ "\tJR\tZ,VDIP_HANDLER11\n" );
	BasicLibrary.appendSetErrorIOMode( compiler );
	buf.append( "\tOR\tA\n"			// RET mit CY=0!
		+ "\tRET\n"
		+ "VDIP_HANDLER11:\n"
	// Datei im VDIP oeffnen
		+ "\tLD\tA,09H\n"
		+ "\tCALL\tVDIP_WR_FNAME_CMD\n"
		+ "\tCALL\tVDIP_CHECK_RESULT\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"			// RET mit CY=0!
		+ "\tLD\tA,(M_IOAC)\n"
		+ "\tCP\t" );
	buf.appendHex2( BasicLibrary.IOMODE_APPEND );
	buf.append( "\n"
		+ "\tJR\tZ,VDIP_HANDLER12\n"
	/*
	 * Betriebsart OUTPUT
	 * Dateizeiger auf den Dateianfang setzen
	 */
		+ "\tCALL\tVDIP_WR_CMD\n"
		+ "\tDB\t28H,20H,00H,00H,00H,00H,0DH\n"
		+ "\tJR\tVDIP_HANDLER14\n"
	/*
	 * Betriebsart APPEND
	 * Dateizeiger auf das Dateiende setzen,
	 * hoechstwertiges Byte zuerst!!!
	 */
		+ "VDIP_HANDLER12:\n"
		+ "\tLD\tA,28H\n"
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
		+ "\tCALL\tVDIP_WR_BYTE\n"
	// Dateizeiger gesetzt -> Ergebnis pruefen
		+ "VDIP_HANDLER14:\n"
		+ "\tCALL\tVDIP_CHECK_RESULT\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"			// RET mit CY=0!
	// Datei zum Schreiben geoeffnet
		+ "\tCALL\tVDIP_LOCK_INIT_BUF\n"
	// Kanalzeigerfeld fuellen
		+ "\tXOR\tA\n"
		+ "\tLD\tHL,(M_IOCA)\n"
		+ "\tLD\tDE,VDIP_CLOSE\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
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
		+ "\tLD\tDE,VDIP_FLUSH\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
	/*
	 * VDIP initialisieren
	 * Rueckgabewert:
	 *  CY=0: OK
	 *  CY=1: Fehler, Fehlervariablen auf E_DEVICE_NOT_FOUND gesetzt
	 */
		+ "VDIP_INIT:\n"
		+ "\tLD\tA,(VDIP_M_INIT)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n" );
	// VDIP aktiviern
	if( target.needsEnableVdip() ) {
	  target.appendEnableVdip( buf );
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
		+ "\tRET\tZ\n"
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
		+ "\tRET\tZ\n"
		+ "\tCALL\tVDIP_FLUSH\n"
		+ "\tLD\tA,0AH\n"
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
		+ "VDIP_EOF:\n"
		+ "\tLD\tA,(VDIP_M_BBYTES)\n"
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
		+ "\tDEC\tHL\n"
		+ "\tRET\n"
	/*
	 * AVAILABLE-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   HL: Anzahl der Bytes im Puffer
	 */
		+ "VDIP_AVAILABLE:\n"
		+ "\tLD\tA,(VDIP_M_BBYTES)\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
	/*
	 * READ-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 * Rueckgabewert:
	 *   A: gelesenes Zeichen
	 */
		+ "VDIP_READ:\n"
	// noch Bytes im Puffer?
		+ "\tLD\tA,(VDIP_M_BBYTES)\n"
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
		+ "\tLD\t(VDIP_M_BPTR),HL\n"
		+ "\tRET\n"
	/*
	 * WRITE-Routine
	 * Parameter:
	 *   (M_IOCA): Anfangsadresse Kanalzeigerfeld
	 *   A:        zu schreibendes Byte
	 */
		+ "VDIP_WRITE:\n"
		+ "\tLD\tHL,(VDIP_M_BPTR)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(VDIP_M_BPTR),HL\n"
		+ "\tLD\tA,(VDIP_M_BBYTES)\n"
		+ "\tINC\tA\n"
		+ "\tLD\t(VDIP_M_BBYTES),A\n"
		+ "\tRET\tP\n"
		// direkt weiter mit der FLUSH-Routine!
	/*
	 * FLUSH-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 */
		+ "VDIP_FLUSH:\n"
		+ "\tLD\tA,(VDIP_M_BBYTES)\n"
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
		+ "\tLD\t(VDIP_M_BPTR),HL\n"
		+ "\tRET\n"
	/*
	 * Lesen der Dateigroesse
	 * Paramater:
	 *   (VDIP_M_FNAME): Dateiname
	 * Rueckgabewerte:
	 *   CY=0: OK, Dateigroesse steht in (VDIP_M_FBYTES)
	 *   CY=1: Fehler
	 */
		+ "VDIP_RD_FSIZE:\n"
		+ "\tLD\tA,01H\n"		// Kommando DIR
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
		+ "\tRET\n"
	/*
	 * Vorhandensein des Speichermediums pruefen
	 * Rueckgabewert:
	 *   CY=0: OK
	 *   CY=1: Fehler, Fehlervariablen gesetzt
	 */
		+ "VDIP_CHECK_DISK:\n"
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
		+ "\tRET\tZ\n"				// Disk vorhanden
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
	 *  CY=1: Fehler (Timeout ca. 100 ms bei " Mhz Taktfrequenz)
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
	buf.append( "\tLD\tDE,1000H\n"
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
	 *  CY=1: Fehler (Timeout ca. 100 ms bei 2 MHz Taktfrequenz)
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
	libItems.add( BasicLibrary.LibItem.C_UPR );
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
    compiler.getCodeBuf().append( "VDIP_DATA_PIO_INIT:\n"
		+ "\tDB\t0CFH,23H,0D0H,17H,0FFH\n"	// PIO B Ctrl
		+ "\tDB\t0D4H\n"			// PIO B Data
		+ "\tDB\t8FH,07H\n"			// PIO A Ctrl
		+ "VDIP_DATA_RDPTRS:\n"
		+ "\tDW\tVDIP_CLOSE\n"
		+ "\tDW\tVDIP_EOF\n"
		+ "\tDW\tVDIP_AVAILABLE\n"
		+ "\tDW\tVDIP_READ\n" );
  }


  public static void appendBssTo( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( compiler.usesLibItem( BasicLibrary.LibItem.VDIP_M_IOADDR ) ) {
      buf.append( "VDIP_M_IOADDR:\n"
		+ "\tDS\t1\n" );
    }
    buf.append( "VDIP_M_INIT:\n"
		+ "\tDS\t1\n"
		+ "VDIP_M_LOCKED:\n"
		+ "\tDS\t1\n"
		+ "VDIP_M_FNAME:\n"
		+ "\tDS\t2\n"
		+ "VDIP_M_FBYTES:\n"
		+ "\tDS\t4\n"
		+ "VDIP_M_BBYTES:\n"
		+ "\tDS\t1\n"
		+ "VDIP_M_BPTR:\n"
		+ "\tDS\t2\n"
		+ "VDIP_BUF:\n"
		+ "\tDS\t80H\n" );
  }


  public static void appendInitTo( BasicCompiler compiler )
  {
    compiler.getCodeBuf().append( "\tXOR\tA\n"
		+ "\tLD\t(VDIP_M_INIT),A\n"
		+ "\tLD\t(VDIP_M_LOCKED),A\n" );
  }
}

