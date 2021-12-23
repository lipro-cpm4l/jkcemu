/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KC85/5-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Nutzung der ab CAOS 4.8 zur Verfuegung stehenden
 * treiberspezifischen Funktionen fuer den Zugriff auf USB-Speicher
 */

package jkcemu.programming.basic.target;

import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.KC85;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;
import jkcemu.programming.basic.BasicUtil;


public class KC85Caos48Target extends KC854Target
{
  public static final String BASIC_TARGET_NAME   = "TARGET_KC85_CAOS48";
  public static final String DISPLAY_TARGET_NAME = "KC85/5 mit CAOS >= 4.8";

  /*
   * IOCTB_M_FLG_OFFS: Flags im Kanalzeigerfeld
   *   Bit 0: Lesen:     Zeichen im Puffer
   *   Bit 1: Lesen:     Ende der Datei erreicht (EOF)
   *   Bit 6: Lesen:     Binaermodus
   *   Bit 7: Schreiben: Datei geoeffnet
   */
  private static final int IOCTB_M_FLG_OFFS = BasicLibrary.IOCTB_DRIVER_OFFS;
  private static final int IOCTB_M_FNAME_OFFS = IOCTB_M_FLG_OFFS + 2;
  private static final int IOCTB_CHANNEL_SIZE = IOCTB_M_FNAME_OFFS + 12;


  private static final String CODE_CHECK_OS_LABEL = "X_CAOS48_CHECK_OS";
  private static final String CODE_CHECK_OS_CALL  = "\tCALL\t"
					+ CODE_CHECK_OS_LABEL
					+ "\n"
					+ "\tRET\tC\n";

  private boolean usesBegFillChannel;
  private boolean usesDiskDriver;
  private boolean usesVdipDriver;
  private boolean usesVdipFBytes;


  public KC85Caos48Target()
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesDiskDriver || this.usesVdipDriver ) {
      buf.append( "X_M_CAOS48_FNAME:\tDS\t2\n"
		+ "X_M_CAOS48_DRIVER_NUM:\tDS\t1\n" );
    }
    if( this.usesDiskDriver ) {
      buf.append( "X_M_CAOS48_DISK_LOCKED:\tDS\t1\n"
		+ "X_M_CAOS48_CHDIR_BUF:\tDS\t3\n" );
    }
    if( this.usesVdipDriver ) {
      buf.append( "X_M_CAOS48_VDIP_LOCKED:\tDS\t1\n" );
      if( this.usesVdipFBytes ) {
	buf.append( "X_M_CAOS48_VDIP_FBYTES:\tDS\t4\n" );
      }
      buf.append( "X_M_CAOS48_VDIP_BBYTES:\tDS\t1\n"
		+ "X_M_CAOS48_VDIP_BPTR:\tDS\t2\n"
		+ "X_M_CAOS48_VDIP_BUF:\tDS\t80H\n" );
    }
    if( this.usesDiskDriver || this.usesVdipDriver ) {
      buf.append( "X_M_CAOS48_BSS_END:\n" );
    }
  }


  @Override
  public void appendDiskHandlerTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    this.usesDiskDriver = true;

    /*
     * Datei-Handler fuer CAOS-DISK-Treiber
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
    boolean txtAppend = modes.contains( BasicLibrary.IOMODE_TXT_APPEND );
    boolean txtOutput = modes.contains( BasicLibrary.IOMODE_TXT_OUTPUT );
    boolean txtInput  = (modes.contains( BasicLibrary.IOMODE_TXT_INPUT )
		|| modes.contains( BasicLibrary.IOMODE_TXT_DEFAULT ));
    boolean binAppend = modes.contains( BasicLibrary.IOMODE_BIN_APPEND );
    boolean binOutput = modes.contains( BasicLibrary.IOMODE_BIN_OUTPUT );
    boolean binInput  = (modes.contains( BasicLibrary.IOMODE_BIN_INPUT )
		|| modes.contains( BasicLibrary.IOMODE_BIN_DEFAULT ));

    // auf CAOS-Version gleich oder groesser 4.8 testen
    buf.append( CODE_CHECK_OS_CALL );

    // Puffer fuer CD-Aufruf in DE
    buf.append( "\tLD\tDE,X_M_CAOS48_CHDIR_BUF\n"
    // Laufwerk am Dateianfang testen
		+ "\tLD\tHL,(IO_M_NAME)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\t41H\n"				// A
		+ "\tJR\tC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tCP\t51H\n"				// P
		+ "\tJR\tNC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
    // User-Bereich (0..15 oder 0..F) testen und ermitteln
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_1\n"		// war ':'
		+ "\tJR\tC,X_CAOS48_DISK_HANDLER_2\n"
		+ "\tSUB\t07H\n"
		+ "\tJR\tC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tCP\t10H\n"
		+ "\tJR\tNC,X_CAOS48_DISK_HANDLER_5\n"
    // User-Bereich als Hexadezimalziffer
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tNZ,X_CAOS48_DISK_HANDLER_5\n"
    // Doppelpunkt gefunden
		+ "X_CAOS48_DISK_HANDLER_1:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tX_CAOS48_DISK_HANDLER_7\n"
    // User-Bereich als Dezimalzahl
		+ "X_CAOS48_DISK_HANDLER_2:\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_3\n"
		+ "\tADD\tA,30H\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tX_CAOS48_DISK_HANDLER_4\n"
    // erste Ziffer ist eine eins -> weitere Ziffer moeglich
		+ "X_CAOS48_DISK_HANDLER_3:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_1\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tADD\tA,41H\n"
		+ "\tCP\t47H\n"
		+ "\tJR\tNC,X_CAOS48_DISK_HANDLER_5\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "X_CAOS48_DISK_HANDLER_4:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_1\n"
    // Testen, ob ein Doppelpunkt (anderer Geraetename)  enthalten ist
		+ "X_CAOS48_DISK_HANDLER_5:\n"
		+ "\tLD\tHL,(IO_M_NAME)\n"
		+ "X_CAOS48_DISK_HANDLER_6:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t3AH\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_CAOS48_DISK_HANDLER_6\n"
    // kein Laufwerk angegeben
		+ "\tLD\t(X_M_CAOS48_CHDIR_BUF),A\n"	// A=0
		+ "\tLD\tHL,(IO_M_NAME)\n"
		+ "X_CAOS48_DISK_HANDLER_7:\n"
    /*
     * Handler ist zustaendig, HL zeigt auf den Pfad-/Dateinamen,
     * Die Funktion darf ab nun nur noch dann mit CY=1 beendet werden,
     * wenn es im CAOS keinen DISK-Treiber findet.
     * In allen anderen Faellen, auch bei Fehler,
     * muss mit CY=0 beendet werden, da dieser Handler zustaendig ist
     * und deshalb kein anderer Handler mehr aufgerufen werden darf.
     */
		+ "\tLD\t(X_M_CAOS48_FNAME),HL\n"
    // Pruefen, ob das Programm den DISK-Treiber schon verwendet
		+ "\tLD\tA,(X_M_CAOS48_DISK_LOCKED)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,X_CAOS48_DEVICE_LOCKED\n"
    // DISK-Treiber aktivieren
		+ "\tLD\t(X_M_CAOS48_FNAME),HL\n"
		+ "\tLD\tD,44H\n"			// 'D' fuer DISK
		+ "\tCALL\tX_CAOS48_ACTIVATE_DRIVER\n"
		+ "\tRET\tC\n" );
    // Datei oeffnen
    if( txtAppend || binAppend ) {
      buf.append( "\tLD\tA,(IO_M_ACCESS)\n"
		+ "\tAND\t" );
      buf.appendHex2( BasicLibrary.IOMODE_APPEND_MASK );
      buf.append( "\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_8\n" );
      // Betriebsart APPEND durch den CAOS-DISK-Treiber nicht unterstuetzt
      BasicUtil.appendSetErrorIOMode( compiler );
      buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_DISK_HANDLER_8:\n" );
    }
    if( txtInput || binInput || txtOutput || binOutput ) {
      buf.append( "\tLD\tDE,X_M_CAOS48_CHDIR_BUF\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_9\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t09H\n"				// CD
		+ "\tJP\tC,X_CAOS48_DISK_SET_ERR\n"
		+ "X_CAOS48_DISK_HANDLER_9:\n"
      /*
       * Dateiname aufbereiten:
       * zuerst kommt die Endung, dann der Basisname,
       * jeweils aufgefuellt mit Leerzeichen
       */
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
      buf.append_LD_DE_nn( IOCTB_M_FNAME_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tBC,0B20H\n"
		+ "X_CAOS48_DISK_HANDLER_10:\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CAOS48_DISK_HANDLER_10\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tPOP\tDE\n"
		+ "\tPUSH\tDE\n"
      // Basisname in Puffer uebertragen
		+ "\tLD\tHL,(X_M_CAOS48_FNAME)\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tB,08H\n"
		+ "X_CAOS48_DISK_HANDLER_11:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_15\n"
		+ "\tCP\t2EH\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_13\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tX_CAOS48_DISK_HANDLER_11\n"
      // Zeichen bis zum Punkt uebergehen
		+ "X_CAOS48_DISK_HANDLER_12:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_13\n"
		+ "\tCP\t2EH\n"
		+ "\tJR\tNZ,X_CAOS48_DISK_HANDLER_12\n"
      // Dateiendung uebertragen
		+ "X_CAOS48_DISK_HANDLER_13:\n"
		+ "\tPOP\tDE\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tB,03H\n"
		+ "X_CAOS48_DISK_HANDLER_14:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_15\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tX_CAOS48_DISK_HANDLER_14\n"
		+ "X_CAOS48_DISK_HANDLER_15:\n"
		+ "\tPOP\tDE\n"			// aufbereiteter Dateiname
		+ "\tLD\tHL,IO_M_ACCESS\n" );
      if( txtInput || binInput ) {
	buf.append_LD_A_n( BasicLibrary.IOMODE_DEFAULT_MASK
					| BasicLibrary.IOMODE_INPUT_MASK );
	buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_18\n"
	// Datei zum Lesen oeffnen und erstes Byte lesen
		+ "\tEX\tDE,HL\n"			// HL: Dateiname
		+ "\tLD\tD,08H\n"			// Bit 3 = 1: OPEN
		+ "\tCALL\tX_CAOS48_DISK_MBIN\n"
		+ "\tJR\tNC,X_CAOS48_DISK_HANDLER_16\n"
		+ "\tCALL\tX_CAOS48_DISK_CLOSE_RD\n"
		+ "\tJR\tX_CAOS48_DISK_SET_ERR\n"
	// Kanalzeigerfeld fuellen
		+ "X_CAOS48_DISK_HANDLER_16:\n"
		+ "\tPUSH\tAF\n"
		+ "\tLD\tDE,X_CAOS48_DISK_CLOSE_RD\n"
		+ "\tCALL\tX_CAOS48_BEG_FILL_CHANNEL\n"
		+ "\tLD\tDE,X_CAOS48_DISK_EOF\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tDE,X_CAOS48_DISK_READ\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
	buf.append_LD_DE_nn( IOCTB_M_FLG_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),01H\n" );		// Bit 0: Puffer gefuellt
	if( txtInput && binInput ) {
	  buf.append( "\tLD\tA,(IO_M_ACCESS)\n"
		+ "\tAND\t" );
	  buf.appendHex2( BasicLibrary.IOMODE_BIN_MASK );
	  buf.append( "\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_17\n"
		+ "\tLD\t(HL),41H\n"		// Bit 6: Binaermodus
		+ "X_CAOS48_DISK_HANDLER_17:\n" );
	}
	buf.append( "\tINC\tHL\n"
		+ "\tPOP\tAF\n"
		+ "\tLD\t(HL),A\n"		// erstes gelesene Byte
		+ "\tJR\tX_CAOS48_DISK_HANDLER_20\n"
		+ "X_CAOS48_DISK_HANDLER_18:\n" );
	this.usesBegFillChannel = true;
      }
      if( txtOutput || binOutput ) {
	buf.append_LD_A_n( BasicLibrary.IOMODE_OUTPUT_MASK );
	buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tZ,X_CAOS48_DISK_SET_ERR\n" );
	/*
         * Die Datei wird mit dem ersten Aufruf von MBOUT geoeffnet.
	 * Deshalb wird hier nur das Kanalzeigerfeld gefuellt.
	 */
	if( txtOutput || !binOutput ) {
	  buf.append( "\tLD\tDE,X_CAOS48_DISK_CLOSE_WR_ASC\n" );
	} else if( !txtOutput || binOutput ) {
	  buf.append( "\tLD\tDE,X_CAOS48_DISK_CLOSE_WR_BIN\n" );
	} else {
	  buf.append( "\tLD\tDE,X_CAOS48_DISK_CLOSE_WR_ASC\n" );
	  buf.append_LD_A_n( BasicLibrary.IOMODE_BIN_MASK );
	  buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tZ,X_CAOS48_DISK_HANDLER_19\n"
		+ "\tLD\tDE,X_CAOS48_DISK_CLOSE_WR_BIN\n"
		+ "X_CAOS48_DISK_HANDLER_19:\n" );
	}
	buf.append( "\tCALL\tX_CAOS48_BEG_FILL_CHANNEL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tDE,X_CAOS48_DISK_WRITE\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
	this.usesBegFillChannel = true;
      }
      buf.append( "X_CAOS48_DISK_HANDLER_20:\n"
		+ "\tLD\tA,01H\n"
		+ "\tLD\t(X_M_CAOS48_DISK_LOCKED),A\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
    }
    buf.append( "X_CAOS48_DISK_SET_ERR:\n" );
    if( this.usesVdipDriver
	&& (compiler.usesLibItem( BasicLibrary.LibItem.M_ERROR_NUM )
	    || compiler.usesLibItem( BasicLibrary.LibItem.M_ERROR_TEXT )) )
    {
      buf.append( "\tJP\tX_CAOS48_VDIP_SET_ERR\n" );
    } else {
      BasicUtil.appendSetError( compiler );
      buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
    }
    if( txtInput || binInput ) {

      /*
       * CLOSE-Routine fuer Lesen
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "X_CAOS48_DISK_CLOSE_RD:\n"
      // Datei schliessen
		+ "\tLD\tD,40H\n"			// CLOSE
		+ "\tCALL\tX_CAOS48_DISK_MBIN\n" );
      if( txtOutput || binOutput ) {
	buf.append( "\tJR\tX_CAOS48_DISK_CLOSE_WR_4\n" );
      } else {
	buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_CAOS48_DISK_LOCKED),A\n"
		+ "\tJP\tX_CAOS48_RESTORE_DRIVER\n" );
      }

      /*
       * EOF-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   HL:  0: Dateiende noch nicht erreicht
       *       -1: Dateiende erreicht
       */
      buf.append( "X_CAOS48_DISK_EOF:\n" );
      buf.append_LD_HL_nn( IOCTB_M_FLG_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tBIT\t1,(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n"

      /*
       * READ-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   A: gelesenes Zeichen
       */
		+ "X_CAOS48_DISK_READ:\n" );
      buf.append_LD_HL_nn( IOCTB_M_FLG_OFFS + 1 );	// HL -> Puffer
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"			// Zeichen im Puffer
		+ "\tDEC\tHL\n"				// HL -> Flags
		+ "\tBIT\t1,(HL)\n"			// EOF?
		+ "\tJR\tNZ,X_CAOS48_DISK_READ_2\n"
		+ "\tBIT\t0,(HL)\n"			// Puffer gefuellt?
		+ "\tJR\tNZ,X_CAOS48_DISK_READ_3\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tD,00H\n"			// Byte lesen
		+ "\tCALL\tX_CAOS48_DISK_MBIN\n"
		+ "\tPOP\tHL\n" );
      if( !txtInput && binInput ) {
	buf.append( "\tRET\tNC\n" );
      } else {
	buf.append( "\tJR\tNC,X_CAOS48_DISK_READ_4\n" );
      }
      buf.append( "X_CAOS48_DISK_READ_1:\n"
		+ "\tSET\t1,(HL)\n"			// Fehler -> EOF
		+ "X_CAOS48_DISK_READ_2:\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n"
		+ "X_CAOS48_DISK_READ_3:\n"
		+ "\tRES\t0,(HL)\n"			// Puffer leer
		+ "X_CAOS48_DISK_READ_4:\n" );
      if( txtInput && binInput ) {
	buf.append( "\tBIT\t6,(HL)\n"
		+ "\tRET\tNZ\n" );			// Binaermodus
      }
      if( txtInput ) {
	buf.append( "\tCP\t1AH\n"			// Dateiende?
		+ "\tRET\tNZ\n"
		+ "\tJR\tX_CAOS48_DISK_READ_1\n" );
      } else {
	buf.append( "\tRET\n" );
      }
      buf.append( "X_CAOS48_DISK_MBIN:\n"
		+ "\tSET\t5,(IX+07H)\n"			// keine Anzeige
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t37H\n"				// MBIN
		+ "\tRET\n" );
    }
    if( binOutput ) {
      /*
       * CLOSE-Routine fuer Schreiben im Binaermodus
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "X_CAOS48_DISK_CLOSE_WR_BIN:\n"
		+ "\tXOR\tA\n"
		+ "\tJR\tX_CAOS48_DISK_CLOSE_WR\n" );
    }
    if( txtOutput ) {
      /*
       * CLOSE-Routine fuer Schreiben im Textmodus
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "X_CAOS48_DISK_CLOSE_WR_ASC:\n"
		+ "\tLD\tA,1AH\n" );
      // direkt weiter mit X_CAOS48_DISK_CLOSE_WR
    }
    if( txtOutput || binOutput ) {
      buf.append( "X_CAOS48_DISK_CLOSE_WR:\n" );
      buf.append_LD_HL_nn( IOCTB_M_FLG_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tBIT\t7,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tNZ,X_CAOS48_DISK_CLOSE_WR_1\n"
      // Datei noch nicht geoeffnet -> oeffnen
		+ "\tLD\tD,08H\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tX_CAOS48_DISK_MBOUT\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,00H\n"
		+ "\tJR\tC,X_CAOS48_DISK_CLOSE_WR_3\n"
		+ "\tDEC\tHL\n"
		+ "\tSET\t7,(HL)\n"			// Datei geoeffnet
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),01H\n"			// 1 Byte geschrieben
      // pruefen, ob es das letzte Byte im Block ist
		+ "X_CAOS48_DISK_CLOSE_WR_1:\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tC\n"
		+ "\tJR\tZ,X_CAOS48_DISK_CLOSE_WR_3\n" );
      // Block mit NUllbytes auffuellen
      if( txtOutput ) {
	buf.append( "\tPUSH\tAF\n" );
      }
      buf.append( "\tXOR\tA\n"
		+ "\tSUB\tC\n"
		+ "\tLD\tB,A\n" );
      if( txtOutput ) {
	buf.append( "\tPOP\tAF\n" );
      } else {
	buf.append( "\tXOR\tA\n" );
      }
      buf.append( "X_CAOS48_DISK_CLOSE_WR_2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tD,00H\n"
		+ "\tCALL\tX_CAOS48_DISK_MBOUT\n" );
      if( txtOutput ) {
	buf.append( "\tLD\tA,00H\n" );
      }
      buf.append( "\tPOP\tBC\n"
		+ "\tJR\tC,X_CAOS48_DISK_CLOSE_WR_3\n"
		+ "\tDJNZ\tX_CAOS48_DISK_CLOSE_WR_2\n"
      // letztes Byte mit Kennung fuer CLOSE schreiben
		+ "X_CAOS48_DISK_CLOSE_WR_3:\n"
    		+ "\tLD\tD,40H\n"			// CLOSE
		+ "\tCALL\tX_CAOS48_DISK_MBOUT\n"
		+ "X_CAOS48_DISK_CLOSE_WR_4:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_CAOS48_DISK_LOCKED),A\n"
		+ "\tJP\tX_CAOS48_RESTORE_DRIVER\n"
      /*
       * WRITE-Routine
       * Parameter:
       *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
       *   A:            zu schreibendes Byte
       */
		+ "X_CAOS48_DISK_WRITE:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n" );
      buf.append_LD_DE_nn( IOCTB_M_FLG_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tD,00H\n"
		+ "\tPUSH\tHL\n"
		+ "\tBIT\t7,(HL)\n"
		+ "\tJR\tNZ,X_CAOS48_DISK_WRITE_1\n"
      // Datei oeffnen
		+ "\tSET\t7,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"				// HL: Dateiname
		+ "\tLD\tD,08H\n"			// OPEN
		+ "X_CAOS48_DISK_WRITE_1:\n"
		+ "\tCALL\tX_CAOS48_DISK_MBOUT\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\t(HL)\n"
		+ "\tRET\n"
		+ "X_CAOS48_DISK_MBOUT:\n"
		+ "\tSET\t5,(IX+07H)\n"			// keine Anzeige
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t38H\n"				// MBOUT
		+ "\tRET\tNC\n"
		+ "\tCALL\tX_CAOS48_DISK_SET_ERR\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
    }
    compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
  }


  @Override
  public void appendEtcPreXOutTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler )
  {
    if( buf.getNumOccurences( CODE_CHECK_OS_CALL ) == 1 ) {
      buf.replace(
		CODE_CHECK_OS_CALL,
		"\tLD\tA,(0E011H)\n"
			+ "\tCP\t7FH\n"
			+ "\tSCF\n"
			+ "\tRET\tNZ\n"		// CAOS < 4.x
			+ "\tLD\tA,(0EDFFH)\n"
			+ "\tCP\t48H\n"
			+ "\tRET\tC\n" );	// CAOS < 4.8
    } else {
      buf.append( CODE_CHECK_OS_LABEL );
      buf.append( ":\n"
			+ "\tLD\tA,(0E011H)\n"
			+ "\tCP\t7FH\n"
			+ "\tSCF\n"
			+ "\tRET\tNZ\n"		// CAOS < 4.x
			+ "\tLD\tA,(0EDFFH)\n"
			+ "\tCP\t48H\n"
			+ "\tRET\n" );		// CY=1: CAOS < 4.8
    }
    if( this.usesDiskDriver || this.usesVdipDriver ) {
      /*
       * Die Funktion prueft, ob der gewuenschte Treiber (Register D)
       * aktiv ist.
       * Wenn nicht, wird der erste Treiber dieses Typs gesucht
       * und aktiviert.
       * Die Nummer des bisher aktiven Treibers wird in
       * (X_M_CAOS48_DRIVER_NUM) geschrieben.
       *
       * Parameter:
       *   D: Type des Treibers 44H: DISK, 55H: USB
       *
       * Rueckgabe:
       *   CY=1: Treiber nicht gefunden
       */
      buf.append( "X_CAOS48_ACTIVATE_DRIVER:\n"
		+ "\tPUSH\tDE\n"
      // aktiven Treiber ermitteln
		+ "\tLD\tA,0FDH\n"			// Treiber abfragen
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t49H\n"				// SETDEV
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\t(X_M_CAOS48_DRIVER_NUM),A\n"
      // Pruefen, ob es der gewuenschte Treiber ist
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\tD\n"
		+ "\tRET\tZ\n"				// Z=1 -> CY=0
      // gewuenschter Treiber nicht aktiv -> Treiber suchen
		+ "\tLD\tL,D\n"
		+ "\tPUSH\tIY\n"
		+ "\tLD\tIY,0A900H\n"
		+ "\tLD\tDE,0020H\n"			// Groesse Eintrag
		+ "\tLD\tB,08H\n"
		+ "X_CAOS48_ACTIVATE_DRIVER_1:\n"
		+ "\tLD\tA,(IY+00H)\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tA\n"
		+ "\tJR\tZ,X_CAOS48_ACTIVATE_DRIVER_2\n"
		+ "\tLD\tA,(IY+04H)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tZ,X_CAOS48_ACTIVATE_DRIVER_3\n"
		+ "X_CAOS48_ACTIVATE_DRIVER_2:\n"
		+ "\tADD\tIY,DE\n"
		+ "\tDJNZ\tX_CAOS48_ACTIVATE_DRIVER_1\n"
      // Ende der Suche in Treibertabelle
		+ "\tLD\tC,0FFH\n"			// nicht gefunden
		+ "X_CAOS48_ACTIVATE_DRIVER_3:\n"
		+ "\tPOP\tIY\n"
		+ "\tLD\tA,C\n"
		+ "\tINC\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
      // Treiber aktivieren, Treiber-Nr. in A
		+ "\tJR\tX_CAOS48_ACTIVATE_DRIVER_4\n"
		+ "X_CAOS48_RESTORE_DRIVER:\n"
		+ "\tLD\tA,(X_M_CAOS48_DRIVER_NUM)\n"
		+ "X_CAOS48_ACTIVATE_DRIVER_4:\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t49H\n"				// SETDEV
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
      // Fehlermeldung Geraet gesperrt
		+ "X_CAOS48_DEVICE_LOCKED:\n" );
      BasicUtil.appendSetErrorDeviceLocked( compiler );
      buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
    }
    if( this.usesBegFillChannel ) {
      /*
       * Die Funktion traegt im Kanal den Zeiger fuer die CLOSE-Routine
       * ein und setzt die anderen Bytes auf 00h.
       *
       * Parameter:
       *   (IO_M_CADDR):  Adresse auf den Kanal
       *   DE:            Zeiger auf CLOSE-Funktion
       * Rueckgabe:
       *   HL:            Zeiger auf den EOF-Eintrag
       */
      buf.append( "X_CAOS48_BEG_FILL_CHANNEL:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tHL\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB," );
      /*
       * Bis einschliesslich Flag-Byte und Zeichen-Puffer loeschen.
       * HL steht bereits auf dem 2. Byte des Kanalzeigerfeldes.
       */
      buf.appendHex2( IOCTB_M_FLG_OFFS );
      buf.append( "\n"
		+ "X_CAOS48_BEG_FILL_CHANNEL_1:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CAOS48_BEG_FILL_CHANNEL_1\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesDiskDriver || this.usesVdipDriver ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\tB,X_M_CAOS48_BSS_END-X_M_CAOS48_FNAME\n"
		+ "\tLD\tHL,X_M_CAOS48_FNAME\n"
		+ "\tXOR\tA\n"
		+ "X_CAOS48_VDIP_INIT_1:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CAOS48_VDIP_INIT_1\n" );
    }
  }


  @Override
  public void appendVdipHandlerTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    this.usesVdipDriver = true;

    /*
     * Datei-Handler fuer CAOS-USB-Treiber
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
    buf.append( getVdipHandlerLabel() );
    buf.append( ":\n" );

    Set<Integer> modes = compiler.getIODriverModes(
					BasicCompiler.IODriver.VDIP );
    boolean txtAppend = modes.contains( BasicLibrary.IOMODE_TXT_APPEND );
    boolean txtOutput = modes.contains( BasicLibrary.IOMODE_TXT_OUTPUT );
    boolean txtInput  = (modes.contains( BasicLibrary.IOMODE_TXT_INPUT )
		|| modes.contains( BasicLibrary.IOMODE_TXT_DEFAULT ));
    boolean binAppend = modes.contains( BasicLibrary.IOMODE_BIN_APPEND );
    boolean binOutput = modes.contains( BasicLibrary.IOMODE_BIN_OUTPUT );
    boolean binInput  = (modes.contains( BasicLibrary.IOMODE_BIN_INPUT )
		|| modes.contains( BasicLibrary.IOMODE_BIN_DEFAULT ));

    // auf CAOS-Version gleich oder groesser 4.8 testen
    buf.append( CODE_CHECK_OS_CALL );

    // Geraetename am Dateianfang testen
    buf.append( "\tLD\tHL,(IO_M_NAME)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\t56H\n"				// V
		+ "\tJR\tNZ,X_CAOS48_VDIP_HANDLER_2\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tZ,X_CAOS48_VDIP_HANDLER_4\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
    /*
     * Am Anfang des Dateinamens steht nicht "V:".
     * Pruefen, ob ein anderer Geraetename dort steht.
     */
		+ "\tDEC\tHL\n"
		+ "X_CAOS48_VDIP_HANDLER_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "X_CAOS48_VDIP_HANDLER_2:\n"
		+ "\tCP\t3AH\n"				// Doppelpunkt
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t2FH\n"				// Slash
		+ "\tJR\tZ,X_CAOS48_VDIP_HANDLER_3\n"
		+ "\tCP\t5CH\n"				// Backslash
		+ "\tJR\tZ,X_CAOS48_VDIP_HANDLER_3\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tJR\tNZ,X_CAOS48_VDIP_HANDLER_1\n"
		+ "X_CAOS48_VDIP_HANDLER_3:\n"
		+ "\tLD\tHL,(IO_M_NAME)\n"
		+ "X_CAOS48_VDIP_HANDLER_4:\n"
    /*
     * Handler ist zustaendig, HL zeigt auf den Pfad-/Dateinamen,
     * Die Funktion darf ab nun nur noch dann mit CY=1 beendet werden,
     * wenn es im CAOS keinen USB-Treiber findet.
     * In allen anderen Faellen, auch bei Fehler,
     * muss mit CY=0 beendet werden, da dieser Handler zustaendig ist
     * und deshalb kein anderer Handler mehr aufgerufen werden darf.
     */
		+ "\tLD\t(X_M_CAOS48_FNAME),HL\n"
    // Pruefen, ob das Programm USB schon verwendet
		+ "\tLD\tA,(X_M_CAOS48_VDIP_LOCKED)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,X_CAOS48_DEVICE_LOCKED\n"
    // USB-Treiber aktivieren
		+ "\tLD\tD,55H\n"			// 'U' fuer USB
		+ "\tCALL\tX_CAOS48_ACTIVATE_DRIVER\n"
		+ "\tRET\tC\n"
    // Datei oeffnen
		+ "\tLD\tHL,IO_M_ACCESS\n" );
    if( txtInput || binInput ) {
      buf.append_LD_A_n( BasicLibrary.IOMODE_DEFAULT_MASK
					| BasicLibrary.IOMODE_INPUT_MASK );
      buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tZ,X_CAOS48_VDIP_HANDLER_6\n"
      // Datei zum Lesen oeffnen
		+ "\tLD\tHL,(X_M_CAOS48_FNAME)\n"
		+ "\tLD\tE,05H\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
		+ "\tJR\tC,X_CAOS48_VDIP_HANDLER_5\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BBYTES),A\n"
      // Dateilaenge merken
		+ "\tLD\t(X_M_CAOS48_VDIP_FBYTES),BC\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_FBYTES+2),DE\n"
      // Kanalzeigerfeld fuellen
		+ "\tLD\tDE,X_CAOS48_VDIP_CLOSE\n"
		+ "\tCALL\tX_CAOS48_BEG_FILL_CHANNEL\n"
		+ "\tLD\tDE,X_CAOS48_VDIP_EOF\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tDE,X_CAOS48_VDIP_READ\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tJR\tX_CAOS48_VDIP_HANDLER_9\n"
		+ "X_CAOS48_VDIP_HANDLER_5:\n"
		+ "\tLD\tHL,4346H\n"			// CF: Command Failed
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJP\tNZ,X_CAOS48_VDIP_CHECK_ERR\n" );
      BasicUtil.appendSetErrorFileNotFound( compiler );
      buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
      this.usesBegFillChannel = true;
      this.usesVdipFBytes     = true;
    }
    buf.append( "X_CAOS48_VDIP_HANDLER_6:\n" );
    if( txtOutput || binOutput ) {
      buf.append_LD_A_n( BasicLibrary.IOMODE_OUTPUT_MASK );
      buf.append( "\tAND\t(HL)\n"
		+ "\tJR\tZ,X_CAOS48_VDIP_HANDLER_7\n"
		+ "\tLD\tHL,(X_M_CAOS48_FNAME)\n"
		+ "\tLD\tE,08H\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
		+ "\tJR\tX_CAOS48_VDIP_HANDLER_8\n" );
    }
    buf.append( "X_CAOS48_VDIP_HANDLER_7:\n" );
    if( txtAppend || binAppend ) {
      buf.append_LD_A_n( BasicLibrary.IOMODE_APPEND_MASK );
      buf.append( "\tAND\t(HL)\n"
		+ "\tJP\tZ,X_CAOS48_VDIP_SET_ERR\n"
		+ "\tLD\tHL,(X_M_CAOS48_FNAME)\n"
		+ "\tLD\tE,09H\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
		+ "\tJR\tX_CAOS48_VDIP_HANDLER_8\n" );
    }
    if( txtOutput || binOutput || txtAppend || binAppend ) {
      buf.append( "X_CAOS48_VDIP_HANDLER_8:\n"
		+ "\tJP\tC,X_CAOS48_VDIP_CHECK_ERR\n"
      // Kanalzeigerfeld fuellen
		+ "\tLD\tDE,X_CAOS48_VDIP_CLOSE\n"
		+ "\tCALL\tX_CAOS48_BEG_FILL_CHANNEL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tDE,X_CAOS48_VDIP_WRITE\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n" );
      this.usesBegFillChannel = true;
    }
    if( txtInput || binInput
	|| txtOutput || binOutput
	|| txtAppend || binAppend )
    {
      buf.append( "X_CAOS48_VDIP_HANDLER_9:\n"
		+ "\tCALL\tX_CAOS48_VDIP_CLEAR_BUF\n"
		+ "\tINC\tA\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_LOCKED),A\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_CLEAR_BUF:\n"
		+ "\tLD\tHL,X_M_CAOS48_VDIP_BUF\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BPTR),HL\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BBYTES),A\n"
		+ "\tRET\n" );
    }
    /*
     * CLOSE-Routine
     * Parameter:
     *   DE: Anfangsadresse Kanalzeigerfeld
     */
    buf.append( "X_CAOS48_VDIP_CLOSE:\n"
		+ "\tLD\tA,(X_M_CAOS48_VDIP_LOCKED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
    if( txtOutput || binOutput || txtAppend || binAppend ) {
      // ggf. Puffer schreiben
      buf.append_LD_HL_nn( BasicLibrary.IOCTB_WRITE_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tLD\tA,(X_M_CAOS48_VDIP_BBYTES)\n"
		+ "\tCALL\tNZ,X_CAOS48_VDIP_WRITE_1\n" );
    }
    // Datei schliessen
    buf.append( "\tLD\tE,0CH\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
    // VDIP entsperren und urspruenglichen Treiber aktivieren
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_LOCKED),A\n"
		+ "\tJP\tX_CAOS48_RESTORE_DRIVER\n" );
    if( txtInput || binInput ) {
      /*
       * EOF-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   HL: -1: Dateiende erreicht
       */
      buf.append( "X_CAOS48_VDIP_EOF:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\tA,(X_M_CAOS48_VDIP_BBYTES)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,X_M_CAOS48_VDIP_FBYTES\n"
		+ "\tLD\tB,04H\n"
		+ "X_CAOS48_VDIP_EOF_1:\n"
		+ "\tOR\t(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_CAOS48_VDIP_EOF_1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n"
      /*
       * READ-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   A: gelesenes Zeichen
       */
		+ "X_CAOS48_VDIP_READ:\n"
      // noch Bytes im Puffer?
		+ "\tLD\tA,(X_M_CAOS48_VDIP_BBYTES)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_READ_5\n"
      /*
       * keine Bytes mehr im Puffer,
       * 128 von der Anzahl der restlichen Dateibytes subtrahieren
       */
		+ "\tLD\tHL,X_M_CAOS48_VDIP_FBYTES\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tD,A\n"
		+ "\tSUB\t80H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tBC,0300H\n"
		+ "X_CAOS48_VDIP_READ_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tSBC\tA,C\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDJNZ\tX_CAOS48_VDIP_READ_1\n"
		+ "\tJR\tNC,X_CAOS48_VDIP_READ_3\n"	// kein Ueberlauf
      /*
       * weniger als 128 Bytes verfuegbar,
       * Anzahl in D
       */
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_FBYTES),HL\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_FBYTES+2),HL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_READ_4\n"
	  // keine restlichen Bytes mehr
		+ "X_CAOS48_VDIP_READ_2:\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BBYTES),A\n"
		+ "\tRET\n"
	  // 128 Bytes lesen
		+ "X_CAOS48_VDIP_READ_3:\n"
		+ "\tLD\tA,80H\n"
	  // Bytes lesen, Anzahl in A
		+ "X_CAOS48_VDIP_READ_4:\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,00H\n"
		+ "\tLD\tHL,X_M_CAOS48_VDIP_BUF\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BPTR),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tE,07H\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tC,X_CAOS48_VDIP_READ_6\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CAOS48_VDIP_READ_2\n"
		+ "X_CAOS48_VDIP_READ_5:\n"
		+ "\tDEC\tA\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BBYTES),A\n"
		+ "\tLD\tHL,(X_M_CAOS48_VDIP_BPTR)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BPTR),HL\n"
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_READ_6:\n"
		+ "\tCALL\tX_CAOS48_VDIP_CHECK_ERR\n"
		+ "\tXOR\tA\n"
		+ "\tJR\tX_CAOS48_VDIP_READ_2\n" );
      this.usesVdipFBytes = true;
    }
    if( txtOutput || binOutput || txtAppend || binAppend ) {
      /*
       * WRITE-Routine
       * Parameter:
       *   (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
       *   A:            zu schreibendes Byte
       */
      buf.append( "X_CAOS48_VDIP_WRITE:\n"
		+ "\tLD\tHL,(X_M_CAOS48_VDIP_BPTR)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(X_M_CAOS48_VDIP_BPTR),HL\n"
		+ "\tLD\tHL,X_M_CAOS48_VDIP_BBYTES\n"
		+ "\tINC\t(HL)\n"
		+ "\tRET\tP\n"
		+ "\tLD\tA,(HL)\n"
      // Puffer schreiben, A: Anzahl Bytes
		+ "X_CAOS48_VDIP_WRITE_1:\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,X_M_CAOS48_VDIP_BUF\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,00H\n"
		+ "\tLD\tE,0BH\n"
		+ "\tCALL\t0F021H\n"
		+ "\tDB\t07H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tX_CAOS48_VDIP_CLEAR_BUF\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tRET\tNC\n" );
      // weiter mit X_CAOS48_VDIP_CHECK_ERR
    }
    buf.append( "X_CAOS48_VDIP_CHECK_ERR:\n"
		+ "\tLD\tHL,4446H\n"			// DF: Disk Full
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_CHECK_ERR_1\n" );
    BasicUtil.appendSetErrorDiskFull( compiler );
    buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_CHECK_ERR_1:\n"
		+ "\tLD\tHL,4649H\n"			// FI: Invalid
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_CHECK_ERR_2\n" );
    BasicUtil.appendSetError(
		compiler,
		BasicLibrary.E_INVALID,
		BasicLibrary.TEXT_INVALID_PARAM_DE,
		BasicLibrary.TEXT_INVALID_PARAM_EN );
    buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_CHECK_ERR_2:\n"
		+ "\tLD\tHL,524FH\n"			// RO: Read Only
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_CHECK_ERR_3\n" );
    BasicUtil.appendSetErrorReadOnly( compiler );
    buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_CHECK_ERR_3:\n"
		+ "\tLD\tHL,464FH\n"			// FO: File Open
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,X_CAOS48_VDIP_SET_ERR\n" );
    BasicUtil.appendSetError(
		compiler,
		BasicLibrary.E_DEVICE_LOCKED,
		"Datei offen",
		"File open" );
    buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "X_CAOS48_VDIP_SET_ERR:\n" );
    BasicUtil.appendSetError( compiler );
    buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
    compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return add( super.getBasicTargetNames(), BASIC_TARGET_NAME );
  }


  @Override
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    int rv = 0;
    if( emuSys != null ) {
      if( emuSys instanceof KC85 ) {
	rv = 1;
	if( ((KC85) emuSys).getKCTypeNum() >= 5 ) {
	  rv = 3;
	}
      }
    }
    return rv;
  }


  @Override
  public String getDiskHandlerLabel()
  {
    return "X_CAOS48_DISK_HANDLER";
  }


  @Override
  public int getDiskIOChannelSize()
  {
    return IOCTB_CHANNEL_SIZE;
  }


  @Override
  public String getVdipHandlerLabel()
  {
    return "X_CAOS48_VDIP_HANDLER";
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return null;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.usesBegFillChannel = false;
    this.usesDiskDriver     = false;
    this.usesVdipDriver     = false;
    this.usesVdipFBytes     = false;
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
	  boolean hex = false;
	  int     pos = 1;
	  while( pos < len ) {
	    ch = fileName.charAt( pos++ );
	    if( ch == ':' ) {
	      rv = true;
	      break;
	    }
	    if( hex ) {
	      break;		// weiteres Zeichen hinter Hex-Ziffer
	    }
	    if( (ch < '0') || (ch > '9') ) {
	      if( ((ch >= 'A') && (ch <= 'F'))
		  || ((ch >= 'a') && (ch <= 'f')) )
	      {
		hex = true;
	      } else {
		break;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public String toString()
  {
    return DISPLAY_TARGET_NAME;
  }
}
