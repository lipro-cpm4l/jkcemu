/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KC85-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.KC85;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;
import jkcemu.programming.basic.VdipLibrary;


public class KC85Target extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_KC85";

  private static final int[] vdipBaseIOAddrs = { 0x2C };

  protected boolean needsFullWindow;
  protected boolean pixUtilAppended;
  protected boolean xpsetAppended;

  private boolean usesJoystick;
  private boolean usesM052;
  private boolean usesXCOLOR;
  private boolean usesXINK;
  private boolean usesXPAPER;
  private boolean usesX_M_CLOCK;
  private boolean xCheckClockAppended;
  private boolean xDepTimeAppended;


  public KC85Target()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
    setNamedValue( "BLACK", 0 );
    setNamedValue( "BLINKING", 0x10 );
    setNamedValue( "BLUE", 0x01 );
    setNamedValue( "CYAN", 0x05 );
    setNamedValue( "GREEN", 0x04 );
    setNamedValue( "MAGENTA", 0x03 );
    setNamedValue( "RED", 0x02 );
    setNamedValue( "WHITE", 0x07 );
    setNamedValue( "YELLOW", 0x06 );
    setNamedValue( "JOYST_LEFT", 0x04 );
    setNamedValue( "JOYST_RIGHT", 0x08 );
    setNamedValue( "JOYST_DOWN", 0x02 );
    setNamedValue( "JOYST_UP", 0x01 );
    setNamedValue( "JOYST_BUTTON1", 0x20 );
    setNamedValue( "JOYST_BUTTON2", 0x10 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    buf.append( "X_M_IO88H:\tDS\t1\n" );
    if( this.usesM052 ) {
      buf.append( "X_M_M052DRVN:\tDS\t1\n"
		+ "X_M_M052SLOT:\tDS\t1\n"
		+ "X_M_M052STAT:\tDS\t1\n"
		+ "X_M_M052USED:\tDS\t1\n" );
    }
    if( this.usesX_M_CLOCK ) {
      buf.append( "X_M_CLOCK:\tDS\t1\n" );
    }
  }


  @Override
  public void appendDisableVdipTo( AsmCodeBuf buf )
  {
    if( getVdipBaseIOAddresses() != null ) {
      buf.append( "\tCALL\tX_M052_DISABLE\n" );
      this.usesM052 = true;
    }
  }


  // Rueckgabe: CY=1 bedeutdet Fehler
  @Override
  public void appendEnableVdipTo( AsmCodeBuf buf )
  {
    if( getVdipBaseIOAddresses() != null ) {
      buf.append( "\tCALL\tX_M052_ENABLE\n" );
      this.usesM052 = true;
    }
  }


  @Override
  public void appendEtcPreXOutTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( this.usesXINK ) {
      buf.append( "XINK:\tLD\tA,01H\n" );
      if( this.usesXCOLOR || this.usesXPAPER ) {
	buf.append( "\tJR\tX_COLOR_1\n" );
      } else {
	buf.append( "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
      }
    }
    if( this.usesXPAPER ) {
      buf.append( "XPAPER:\tLD\tE,L\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tLD\tL,A\n" );
      this.usesXCOLOR = true;
      // unmittelbar weiter mit XCOLOR!
    }
    if( this.usesXCOLOR ) {
      buf.append( "XCOLOR:\tLD\tA,02H\n"
		+ "X_COLOR_1:\n"
		+ "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
    }
    if( (getVdipBaseIOAddresses() != null) && this.usesM052 ) {
      /*
       * Aktivieren des Moduls M052
       * Rueckgabewert:
       *   CY=0: M052 aktiv
       *   CY=1: M052 nicht gefunden
       */
      buf.append( "X_M052_ENABLE:\n"
		+ "\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
      // auf CAOS-Version gleich oder groesser 4.8 testen
		+ "\tLD\tA,(0E011H)\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tNZ,X_M052_ENABLE_5\n"		// CAOS < 4.x
		+ "\tLD\tA,(0EDFFH)\n"
		+ "\tCP\t48H\n"
		+ "\tJR\tC,X_M052_ENABLE_5\n"		// CAOS < 4.8
      // CAOS >= 4.8: aktiven Treiber ermitteln
		+ "\tLD\tA,0FDH\n"			// Treiber anfragen
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t49H\n"				// SETDEV
		+ "\tRET\tC\n"
		+ "\tLD\t(X_M_M052DRVN),A\n"
      // ist der aktive Treiber ein USB-Treiber?
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t55H\n"
		+ "\tJR\tZ,X_M052_ENABLE_4\n"
      // kein USB-Treiber aktiv -> ersten USB-Treiber suchen
		+ "\tPUSH\tIY\n"
		+ "\tLD\tIY,0A900H\n"
		+ "\tLD\tDE,0020H\n"			// Groesse Eintrag
		+ "\tLD\tB,08H\n"
		+ "X_M052_ENABLE_1:\n"
		+ "\tLD\tA,(IY+00H)\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tA\n"
		+ "\tJR\tZ,X_M052_ENABLE_2\n"		// kein Treiber
		+ "\tLD\tA,(IY+04H)\n"
		+ "\tCP\t55H\n"
		+ "\tJR\tZ,X_M052_ENABLE_3\n"		// "U" fuer USB
		+ "X_M052_ENABLE_2:\n"
		+ "\tADD\tIY,DE\n"
		+ "\tDJNZ\tX_M052_ENABLE_1\n"
      // Ende der Suche in Treibertabelle
		+ "\tLD\tC,0FFH\n"			// kein USB gefunden
		+ "X_M052_ENABLE_3:\n"
		+ "\tPOP\tIY\n"
		+ "\tLD\tA,C\n"
		+ "\tINC\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
      // Treiber aktivieren
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t49H\n"			// SETDEV
		+ "\tRET\tC\n"
      // Device-Treiber in USER-Mode schalten
		+ "X_M052_ENABLE_4:\n"
		+ "\tLD\tE,00H\n"		// Unterfunktion 0
		+ "\tCALL\t0F021H\n"		// PV 7
		+ "\tDB\t07H\n"			// Funktion 7
		+ "\tRET\tC\n"			// Fehler
		+ "\tLD\tA,01H\n"		// M052 verwaltet durch CAOS
		+ "\tJR\tX_M052_ENABLE_8\n"
      /*
       * kein nutzbarer Treiber gefunden
       * -> M052 selbst suchen und initalisieren
       */
		+ "X_M052_ENABLE_5:\n"
		+ "\tLD\tBC,0880H\n"
		+ "X_M052_ENABLE_6:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tCP\t0FDH\n"
		+ "\tJR\tZ,X_M052_ENABLE_7\n"
		+ "\tINC\tB\n"
		+ "\tJR\tNZ,X_M052_ENABLE_6\n"
		+ "\tSCF\n"
		+ "\tRET\n"			// M052 nicht gefunden, CY=1
      // M052 gefunden
		+ "X_M052_ENABLE_7:\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\tH,0B8H\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
      // M052 einblenden und initialisieren
		+ "\tLD\tD,0C5H\n"		// C000H, IO ein
		+ "\tLD\tL,B\n"			// Slot
		+ "\tLD\tA,02H\n"		// 2 Argumente
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n" );
      VdipLibrary.appendCodeInitPIOTo( buf, getVdipBaseIOAddresses()[ 0 ] );
      buf.append( "\tLD\tA,0FFH\n"		// M052 selbst verwaltet
      /*
       * Initialisierungszustand merken
       */
		+ "X_M052_ENABLE_8:\n"
		+ "\tLD\t(X_M_M052USED),A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
      /*
       * Deaktivieren des Moduls M052
       */
		+ "X_M052_DISABLE:\n"
		+ "\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tJP\tM,X_M052_DISABLE_1\n"
      // Device-Treiber in CAOS-Mode schalten
		+ "\tLD\tE,01H\n"			// Unterfunktion 1
		+ "\tCALL\t0F021H\n"			// PV 7
		+ "\tDB\t07H\n"				// Funktion 7
		+ "\tJR\tX_M052_DISABLE_2\n"
      // selbst verwaltetes M052 auf den urspruenglichen Wert schalten
		+ "X_M052_DISABLE_1:\n"
		+ "\tLD\tA,(X_M_M052STAT)\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,(X_M_M052SLOT)\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n"
		+ "X_M052_DISABLE_2:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_M052USED),A\n"
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendPreExitTo( AsmCodeBuf buf )
  {
    if( this.usesM052 ) {
      buf.append( "\tCALL\tX_M052_DISABLE\n" );
    }
    buf.append( "\tLD\tA,(X_M_IO88H)\n"
		+ "\tOUT\t(88H),A\n" );
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf, boolean appTypeSub )
  {
    buf.append( "\tRET\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    /*
     * IRM-Status merken und IRM einschalten
     */
    buf.append( "\tIN\tA,(88H)\n"
		+ "\tLD\t(X_M_IO88H),A\n"
		+ "\tOR\t04H\n"
		+ "\tOUT\t(88H),A\n" );
    if( this.usesJoystick ) {
      /*
       * Joystick-PIO im Modul M008/M021 programmieren,
       * und zwar so, wie es auch der Treiber im CAOS 4.5 tut
       */
      buf.append( "\tLD\tA,0CFH\n"
		+ "\tOUT\t(92H),A\n"
		+ "\tLD\tA,7FH\n"
		+ "\tOUT\t(92H),A\n" );
    }
    if( this.usesM052 ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
		+ "\tLD\t(X_M_M052USED),A\n" );
    }
    if( this.usesX_M_CLOCK ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_CLOCK),A\n" );
    }
    if( this.needsFullWindow ) {
      // Fenstergroesse pruefen und ggf. auf Maximalewerte setzen
      buf.append( "\tLD\tHL,(0B79CH)\n"		// Fensterposition
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,X_INIT_WIN_1\n"
		+ "\tLD\tHL,(0B79EH)\n"		// Fenstergroesse
		+ "\tLD\tA,H\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,X_INIT_WIN_1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t28H\n"
		+ "\tJR\tNC,X_INIT_WIN_2\n"
		+ "X_INIT_WIN_1:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\t(0B79CH),HL\n"		// Fensteranfang
		+ "\tLD\tHL,2028H\n"
		+ "\tLD\t(0B79EH),HL\n"		// Fenstergroesse
		+ "\tLD\tA,(0B79BH)\n"		// Fensternummer
		+ "\tLD\tL,A\n"			// Position im
		+ "\tLD\tH,00H\n"		// Fenstervektorspeicher
		+ "\tLD\tC,L\n"			// berechnen
		+ "\tLD\tB,H\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tDE,0B99CH\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),00H\n"		// Fensteranfang
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),28H\n"		// Fenstergroesse
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),20H\n"		// Fenstergroesse
		+ "\tINC\tHL\n"
		+ "\tLD\tA,0CH\n"		// Fenster loeschen
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t00H\n"
		+ "X_INIT_WIN_2:\n" );
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
      buf.append( "XCHECK_BREAK:\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t2AH\n"
		+ "\tRET\tNC\n"
		+ "\tJR\tXBREAK\n" );
    }
    if( xInkey ) {
      buf.append( "XINKEY:\tCALL\t0F003H\n"
		+ "\tDB\t0EH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tJR\tC,X_INKEY_1\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n"
		+ "X_INKEY_1:\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tRET\tC\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n" );
      }
    }
    if( xInch ) {
      buf.append( "XINCH:\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t16H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    boolean done = false;
    if( appName != null ) {
      if( !appName.isEmpty() ) {
	buf.append( "\tDB\t7FH,7FH\n" );
	buf.appendStringLiteral( appName, "01H" );
	done = true;
      }
    }
    if( !done ) {
      compiler.putWarning(
		"Programm kann auf dem Zielsystem nicht aufgerufen werden,"
				+ " da der Programmname leer ist." );
    }
    buf.append( "\tENT\n" );
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0028H\n" );
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0140H\n" );
  }


  @Override
  public void appendXClsTo( AsmCodeBuf buf )
  {
    buf.append( "XCLS:\tLD\tA,0CH\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n" );
    this.needsFullWindow = true;
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  @Override
  public void appendXColorTo( AsmCodeBuf buf )
  {
    this.usesXCOLOR = true;
  }


  /*
   * Abfrage der Zeile der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsLinTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSLIN:\n"
		+ "\tLD\tA,(0B7A1H)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNC,X_CRSLIN_1\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_CRSLIN_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Abfrage der Spalte der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsPosTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSPOS:\n"
		+ "\tLD\tA,(0B7A0H)\n"
		+ "\tCP\t28H\n"
		+ "\tJR\tNC,X_CRSPOS_1\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_CRSPOS_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Lesen des Datums und der Uhrzeit
   * Rueckgabe:
   *   X_M_DATETIME:    Jahr Tausender
   *   X_M_DATETIME+1:  Jahr Hunderter
   *   X_M_DATETIME+2:  Jahr Zehner
   *   X_M_DATETIME+3:  Jahr Einser
   *   X_M_DATETIME+4:  Monat Zehner
   *   X_M_DATETIME+5:  Monat Einser
   *   X_M_DATETIME+6:  Tag Zehner
   *   X_M_DATETIME+7:  Tag Einser
   *   X_M_DATETIME+8:  Stunde Zehner (0...23)
   *   X_M_DATETIME+9:  Stunde Einser
   *   X_M_DATETIME+10: Minute Zehner
   *   X_M_DATETIME+11: Minute Einser
   *   X_M_DATETIME+12: Sekunde Zehner
   *   X_M_DATETIME+13: Sekunde Einser
   *   CY=1:            Fehler
   */
  @Override
  public void appendXDateTimeTo( AsmCodeBuf buf )
  {
    if( !this.xDateTimeAppended ) {
      buf.append( "XDATETIME:\n"
		+ "\tCALL\tX_CHECK_CLOCK\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_DATETIME_1\n"	// D004 + DEP >= 3.0
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_DATETIME_4\n"	// 000Ch-000Fh
		+ "\tSCF\n"
		+ "\tRET\n"
      /*
       * Lesen von 6 aufeinander folgende Bytes aus dem D004/D008-RAM
       * ab Adresse 0FD84h und diese nibble-weise (also 12 Bytes)
       * nach M_X_DATETIME schreiben
       */
		+ "X_DATETIME_1:\n"
		+ "\tCALL\tX_DEPTIME\n"
      /*
       * Die Uhrzeit muss nicht unbedingt aus einer Real Time Clock stammen,
       * sondern kann u.U. auch manuell eingegeben worden sein.
       * Dadurch ist auch ein Datum in der Vergangenheit moeglich,
       * d.h. auch eins vor dem Jahr 2000.
       * Aus diesem Grund werden hier abhaengig von der Zehnerziffer
       * der Jahreszahl die Hunderter und Tausender ermittelt.
       * Die Zehnerziffer steht bereits in A.
       */
		+ "\tLD\tHL,0002H\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tC,X_DATETIME_3\n"
		+ "\tLD\tHL,0901H\n"
		+ "X_DATETIME_3:\n"
		+ "\tLD\t(X_M_DATETIME),HL\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
      /*
       * Zeit aus den Speicherzellen 000Ch-000Fh lesen,
       * konvertieren und nach X_M_DATETIME schreiben
       */
		+ "X_DATETIME_4:\n"
		+ "\tLD\tHL,X_M_DATETIME\n"
		+ "\tLD\tDE,000CH\n"
      // Jahr
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tC,A\n"
		+ "\tSRL\tA\n"			// Bit 0..6: Jahr ab 1980
		+ "\tSUB\t14H\n"
		+ "\tJR\tNC,X_DATETIME_5\n"
		+ "\tLD\t(HL),01H\n"		// 1980..1999
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),09H\n"
		+ "\tADD\tA,64H\n"
		+ "\tJR\tX_DATETIME_7\n"
		+ "X_DATETIME_5:\n"
		+ "\tLD\t(HL),02H\n"		// 2000...2107
		+ "\tINC\tHL\n"
		+ "\tSUB\t64H\n"
		+ "\tJR\tNC,X_DATETIME_6\n"	// 2100..2107
		+ "\tADD\tA,64H\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tJR\tX_DATETIME_7\n"
		+ "X_DATETIME_6:\n"
		+ "\tLD\t(HL),01H\n"
		+ "X_DATETIME_7:\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tX_DATETIME_10\n"
      // Monat
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tB,03H\n"
		+ "X_DATETIME_8:\n"
		+ "\tRLCA\n"
		+ "\tRL\tC\n"
		+ "\tDJNZ\tX_DATETIME_8\n"
		+ "\tLD\tA,C\n"
		+ "\tAND\t0FH\n"
		+ "\tCALL\tX_DATETIME_10\n"
      // Tag
		+ "\tLD\tA,(DE)\n"
		+ "\tAND\t1FH\n"
		+ "\tCALL\tX_DATETIME_10\n"
      // Stunde
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tAND\t1FH\n"
		+ "\tCALL\tX_DATETIME_10\n"
      // Minute
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tB,03H\n"
		+ "X_DATETIME_9:\n"
		+ "\tRLCA\n"
		+ "\tRL\tC\n"
		+ "\tDJNZ\tX_DATETIME_9\n"
		+ "\tLD\tA,C\n"
		+ "\tAND\t3FH\n"
		+ "\tCALL\tX_DATETIME_10\n"
      // Sekunde
		+ "\tLD\tA,(DE)\n"
		+ "\tRLCA\n"
		+ "\tAND\t3EH\n"
      // zwei Ziffern ausgeben
		+ "X_DATETIME_10:\n"
		+ "\tLD\tB,0FFH\n"
		+ "X_DATETIME_11:\n"
		+ "\tINC\tB\n"
		+ "\tSUB\t0AH\n"
		+ "\tJR\tNC,X_DATETIME_11\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tADD\tA,0AH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      appendXCheckClockTo( buf );
      appendXDepTimeTo( buf );
      this.usesX_M_DATETIME  = true;
      this.xDateTimeAppended = true;
    }
  }


  /*
   * Lesen des Datums und der Uhrzeit und
   * und Rueckgabe in einer DOSTIME-Struktur
   *
   * Rueckgabe:
   *   HL:   Zeiger auf eine DOSTIME-Struktur
   *   CY=1: Fehler
   */
  @Override
  public void appendXDosTimeTo( AsmCodeBuf buf )
  {
    if( !this.xDosTimeAppended ) {
      buf.append( "XDOSTIME:\n"
		+ "\tCALL\tX_CHECK_CLOCK\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_DOSTIME_1\n"	// D004 + DEP >= 3.0
		+ "\tDEC\tA\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tLD\tHL,000CH\n"
		+ "\tRET\tZ\n"			// 000Ch-000Fh
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_DOSTIME_1:\n"
		+ "\tCALL\tX_DEPTIME\n"
		+ "\tJP\tX_TO_DOSTIME\n" );
      appendXToDosTimeTo( buf );
      appendXCheckClockTo( buf );
      appendXDepTimeTo( buf );
      this.xDosTimeAppended = true;
    }
  }


  /*
   * Zeichnen einer horizontalen Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate, nicht kleiner 0
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXHLineTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XHLINE:\tBIT\t7,B\n"
		+ "\tRET\tNZ\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\tC\n"
		+ "\tLD\tH,00H\n"
		+ "X_HLINE_1:\n"
		+ "\tOR\tH\n"
		+ "\tLD\tH,A\n"
		+ "\tSRL\tA\n"
		+ "\tJR\tC,X_HLINE_2\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tZ,X_HLINE_1\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tJR\tX_PSET_A\n"
		+ "X_HLINE_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tCALL\tX_PSET_A\n"
		+ "\tEXX\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tH,00H\n"
		+ "\tJR\tXHLINE\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXInkTo( AsmCodeBuf buf )
  {
    this.usesXINK = true;
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   */
  @Override
  public void appendXJoyTo( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,X_JOY_1\n"
		+ "\tIN\tA,(90H)\n"
		+ "\tCPL\n"
		+ "\tAND\t3FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_JOY_1:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    this.usesJoystick = true;
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tH,E\n"
		+ "\tLD\t(0B7A0H),HL\n"
		+ "\tRET\n" );
    this.needsFullWindow = true;
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tCALL\t0F003H\n"
		+ "\tDB\t02H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      buf.append( "XOUTCH:\tCALL\t0F003H\n"
		+ "\tDB\t24H\n"
		+ "\tRET\n" );
      this.xOutchAppended = true;
    }
  }


  /*
   * XPAINT_LEFT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach links,
   *   Der Startpunkt selbst wird nicht geprueft
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     (PAINT_M_X1): X-Endkoordinate der gefuellten Linie,
   *                   kleiner oder gleich X-Startpunkt
   *
   * XPAINT_RIGHT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach rechts
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     CY=1:         Pixel im Startpunkt bereits gesetzt (gefuellt)
   *                   oder ausserhalb des sichtbaren Bereichs
   *     (PAINT_M_X2): X-Endkoordinate der gefuellten Linie, nur bei CY=0
   */
  @Override
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPAINT_LEFT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,X_PAINT_LEFT_5\n"
		+ "\tDEC\tDE\n"
		+ "X_PAINT_LEFT_1:\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,X_PAINT_LEFT_4\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tA,B\n"
		+ "X_PAINT_LEFT_2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,X_PAINT_LEFT_3\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tDEC\tDE\n"
		+ "\tSLA\tC\n"
		+ "\tJR\tNC,X_PAINT_LEFT_2\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "\tJR\tX_PAINT_LEFT_1\n"
		+ "X_PAINT_LEFT_3:\n"
                + "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "X_PAINT_LEFT_4:\n"
		+ "\tINC\tDE\n"
		+ "X_PAINT_LEFT_5:\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tRET\n"
		+ "XPAINT_RIGHT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tAND\tB\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tX_PAINT_RIGHT_2\n"
		+ "X_PAINT_RIGHT_1:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,X_PAINT_RIGHT_3\n"
		+ "X_PAINT_RIGHT_2:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tINC\tDE\n"
		+ "\tSRL\tC\n"
		+ "\tJR\tNC,X_PAINT_RIGHT_1\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,X_PAINT_RIGHT_4\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tAND\tB\n"
		+ "\tJR\tNZ,X_PAINT_RIGHT_4\n"
		+ "\tJR\tX_PAINT_RIGHT_2\n"
		+ "X_PAINT_RIGHT_3:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "X_PAINT_RIGHT_4:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    this.usesXPAPER = true;
  }


  /*
   * Farbe eines Pixels ermitteln
   * Parameter:
   *   DE: X-Koordinate (0...319)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  @Override
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_POINT_5\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,X_POINT_1\n"
    // Farbbyte lesen bei KC85/4..5
		+ "\tDB\t0DDH,0CBH,01H,0CFH\t;SET 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tDB\t0DDH,0CBH,01H,8FH\t;RES 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tJR\tX_POINT_2\n"
    // Farbbyte lesen bei KC85/2..3
		+ "X_POINT_1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tC,A\n"
    // Pixel auswerten
		+ "X_POINT_2:\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tA,C\n"
		+ "\tJR\tZ,X_POINT_3\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t1FH\n"
		+ "\tJR\tX_POINT_4\n"
		+ "X_POINT_3:\n"
		+ "\tAND\t07H\n"
		+ "X_POINT_4:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_POINT_5:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPResTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPRES:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n" );
    appendXPSetTo( buf, compiler );
    appendPixUtilTo( buf );
  }


  /*
   * Setzen eines Pixels unter Beruecksichtigung des eingestellten Stiftes
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPSetTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpsetAppended ) {
      buf.append( "XPSET:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n"
		+ "X_PSET_A:\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tB,A\n"
		+ "\tLD\tA,(X_M_PEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PSET_2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PSET_1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(HL)\n"		// Stift 3 (XOR-Mode)
		+ "\tXOR\tB\n"
		+ "\tJR\tX_PSET_WR_A\n"
		+ "X_PSET_1:\n"			// Pixel loeschen
		+ "\tLD\tA,B\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n"
		+ "X_PSET_2:\n"			// Pixel setzen
		+ "\tLD\tA,B\n" );
      }
      buf.append( "X_PSET_OR_A:\n"
		+ "\tOR\t(HL)\n"
		+ "X_PSET_WR_A:\n"
		+ "\tLD\t(HL),A\n"
      // Farbbyte schreiben
		+ "X_PSET_WR_COLOR:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,X_PSET_3\n"
      /*
       * bei KC85/4..5 Farbebene einschalten, Farbe setzen
       * und wieder Pixelebene einschalten
       */
		+ "\tDB\t0DDH,0CBH,01H,0CFH\t;SET 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDB\t0DDH,0CBH,01H,8FH\t;RES 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tRET\n"
      /*
       * KC85/2..3: Zur Sicherheit pruefen,
       * ob DE eine Adresse im Farbspeicher enthaelt
       */
		+ "X_PSET_3:\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t0A8H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t0B2H\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf );
      this.needsFullWindow = true;
      this.xpsetAppended   = true;
    }
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel exisistiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_PTEST_1\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
  }


  @Override
  public int get100msLoopCount()
  {
    return 50;
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
      if( emuSys instanceof KC85 ) {
	rv = 3;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0300;
  }


  @Override
  public int getMaxAppNameLen()
  {
    return 38;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAdAdr )
  {
    String rv = null;
    if( emuSys != null ) {
      if( emuSys instanceof KC85 ) {
	rv = appName;
      }
    }
    return appName;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return vdipBaseIOAddrs;
  }


  @Override
  public boolean needsEnableDisableVdip()
  {
    return (getVdipBaseIOAddresses() != null);
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsFullWindow     = false;
    this.pixUtilAppended     = false;
    this.usesJoystick        = false;
    this.usesM052            = false;
    this.usesXCOLOR          = false;
    this.usesXINK            = false;
    this.usesXPAPER          = false;
    this.usesX_M_CLOCK       = false;
    this.xCheckClockAppended = false;
    this.xDepTimeAppended    = false;
    this.xpsetAppended       = false;
  }


  @Override
  public boolean supportsColors()
  {
    return true;
  }


  @Override
  public boolean supportsGraphics()
  {
    return true;
  }


  @Override
  public boolean supportsXCLS()
  {
    return true;
  }


  @Override
  public boolean supportsXHLINE()
  {
    return true;
  }


  @Override
  public boolean supportsXJOY()
  {
    return true;
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
  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return true;
  }


  @Override
  public boolean supportsXTIME()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "KC85/2..5, HC900";
  }


	/* --- Hilfsfunktionen --- */

  protected void appendPixUtilTo( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      /*
       * Pruefen der Parameter, ermitteln von Informationen zu einem Pixel
       * und bei KC85/4..5 einschalten der Pixelebene
       *
       * Parameter:
       *   DE: X-Koordinate (0...319)
       *   HL: Y-Koordinate (0...255)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   A:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   C:    84h (nur bei DE=0)
       *   DE:   Adresse im Farbspeicher (KC85/2..3) oder 0 (KC85/4..5)
       *   HL:   Adresse im Pixelspeicher
       */
      buf.append( "X_PINFO:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PINFO_1\n"
		+ "\tCP\t02H\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "X_PINFO_1:\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tZ,X_PINFO_3\n"
		+ "X_PINFO_2:\n"
		+ "\tSRL\tC\n"
		+ "\tDJNZ\tX_PINFO_2\n"
		+ "X_PINFO_3:\n"
		+ "\tPUSH\tBC\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tL,E\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t34H\n"
		+ "\tPOP\tBC\n"
      // bei KC85/4..5 Pixelebene einschalten
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"			// CY=0
		+ "\tLD\tA,C\n"
		+ "\tRET\tNZ\n"			// KC85/2..3
		+ "\tDB\t0DDH,0CBH,01H,88H\t;RES 1,(IX+01H),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tOUT\t(C),B\n"
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }


  /*
   * Pruefen auf Vorhandensein einer Systemuhr
   *
   * Rueckgabe:
   *   A und X_M_CLOCK:
   *     -1: keine Systemuhr vorhanden
   *      1: D004 mit DEP >= 3.0
   *      2: Systemzellen 000Ch-000Fh
   */
  protected void appendXCheckClockTo( AsmCodeBuf buf )
  {
    if( !this.xCheckClockAppended ) {
      buf.append( "X_CHECK_CLOCK:\n"
		+ "\tLD\tA,(X_M_CLOCK)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
      // Test auf D004
		+ "\tLD\tBC,0FC80H\n"
		+ "\tIN\tA,(C)\n"
		+ "\tCP\t0A7H\n"
		+ "\tJR\tNZ,X_CHECK_CLOCK_1\n"
      // Kopplung einschalten
		+ "\tLD\tA,(0B8FCH)\n"		// Modulsteuerbyte lesen
		+ "\tOR\t05H\n"			// Modul und Kopplung ein
		+ "\tLD\tD,A\n"			// Modulsteuerbyte setzen
		+ "\tLD\tL,0FCH\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n"
      // Kontrolle auf DEP Version 3.0 oder hoeher
		+ "\tLD\tBC,083F1H\n"
		+ "\tIN\tA,(C)\n"
		+ "\tCP\t30H\n"
		+ "\tJR\tC,X_CHECK_CLOCK_1\n"
		+ "\tLD\tA,01H\n"
		+ "\tJR\tX_CHECK_CLOCK_4\n"
      /*
       * kein D004 mit DEP >= 3.0
       * -> Systemzellen 000Ch-000Fh pruefen
       */
		+ "X_CHECK_CLOCK_1:\n"
		+ "\tLD\tHL,000CH\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tRRCA\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tRR\tA\n"			// Bit 4..7: Monat
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,X_CHECK_CLOCK_3\n"
		+ "\tCP\t0D0H\n"
		+ "\tJR\tNC,X_CHECK_CLOCK_3\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"		// Bit 0..3: Tag
		+ "\tJR\tZ,X_CHECK_CLOCK_3\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNC,X_CHECK_CLOCK_3\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,A\n"
		+ "\tAND\t0F8H\n"		// Bit 3..7: Stunde
		+ "\tCP\t0C0H\n"
		+ "\tJR\tNC,X_CHECK_CLOCK_3\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
      /*
       * Wenn der Zeitstempel 0 ist,
       * wird von einem statischen Zeitstempel ausgegangen,
       * der von keinem Zeitgeber aktuslisiert wird,
       * auch wenn das fuer die ersten zwei Sekunden nach Mitternacht
       * nicht zutreffen muss.
       */
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tB\n"
		+ "\tJR\tZ,X_CHECK_CLOCK_3\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0E0H\n"		// Bit 0..4: Sekunden / 2
		+ "\tLD\tB,03H\n"
		+ "X_CHECK_CLOCK_2:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tA\n"
		+ "\tDJNZ\tX_CHECK_CLOCK_2\n"
		+ "\tAND\t0FCH\n"		// Bit 2..7: Minute
		+ "\tCP\t0F0H\n"
		+ "\tJR\tNC,X_CHECK_CLOCK_3\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t1FH\n"		// Bit 0..4: Sekunden / 2
		+ "\tCP\t1EH\n"
		+ "\tJR\tNC,X_CHECK_CLOCK_3\n"
		+ "\tLD\tA,02H\n"
		+ "\tJR\tX_CHECK_CLOCK_4\n"
		+ "X_CHECK_CLOCK_3:\n"
		+ "\tLD\tA,0FFH\n"
		+ "X_CHECK_CLOCK_4:\n"
		+ "\tLD\t(X_M_CLOCK),A\n"
		+ "\tRET\n" );
      this.usesX_M_CLOCK       = true;
      this.xCheckClockAppended = true;
    }
  }


  /*
   * Lesen des Datums und der Uhrzeit aus D004 / DEP 3.x
   *
   * Rueckgabe:
   *   X_M_DATETIME:    Jahr Tausender
   *   X_M_DATETIME+1:  Jahr Hunderter
   *   X_M_DATETIME+2:  Jahr Zehner
   *   X_M_DATETIME+3:  Jahr Einser
   *   X_M_DATETIME+4:  Monat Zehner
   *   X_M_DATETIME+5:  Monat Einser
   *   X_M_DATETIME+6:  Tag Zehner
   *   X_M_DATETIME+7:  Tag Einser
   *   X_M_DATETIME+8:  Stunde Zehner (0...23)
   *   X_M_DATETIME+9:  Stunde Einser
   *   X_M_DATETIME+10: Minute Zehner
   *   X_M_DATETIME+11: Minute Einser
   *   X_M_DATETIME+12: Sekunde Zehner
   *   X_M_DATETIME+13: Sekunde Einser
   */
  protected void appendXDepTimeTo( AsmCodeBuf buf )
  {
    if( !this.xDepTimeAppended ) {
      buf.append( "X_DEPTIME:\n"
		+ "\tLD\tHL,X_M_DATETIME+0DH\n"
		+ "\tLD\tBC,89F1H\n"
		+ "\tLD\tD,06H\n"
		+ "X_DEPTIME_1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tPUSH\tAF\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDEC\tHL\n"
		+ "\tPOP\tAF\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tB\n"
		+ "\tDEC\tD\n"
		+ "\tJR\tNZ,X_DEPTIME_1\n"
		+ "\tRET\n" );
      this.usesX_M_DATETIME = true;
      this.xDepTimeAppended = true;
    }
  }
}
