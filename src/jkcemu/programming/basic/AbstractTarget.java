/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die System-spezifische Code-Erzeugung des BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.util.HashMap;
import java.util.Map;
import jkcemu.base.EmuSys;
import jkcemu.file.FileFormat;
import jkcemu.programming.basic.target.AC1Target;
import jkcemu.programming.basic.target.CPMTarget;
import jkcemu.programming.basic.target.HueblerGraphicsMCTarget;
import jkcemu.programming.basic.target.KC854Target;
import jkcemu.programming.basic.target.KC85Caos48Target;
import jkcemu.programming.basic.target.KC85Target;
import jkcemu.programming.basic.target.KramerMCTarget;
import jkcemu.programming.basic.target.LLC2HIRESTarget;
import jkcemu.programming.basic.target.SCCHTarget;
import jkcemu.programming.basic.target.Z1013PetersTarget;
import jkcemu.programming.basic.target.Z1013KRTTarget;
import jkcemu.programming.basic.target.Z1013Target;
import jkcemu.programming.basic.target.Z1013ZXTarget;
import jkcemu.programming.basic.target.Z9001KRTTarget;
import jkcemu.programming.basic.target.Z9001Target;


public abstract class AbstractTarget
{
  protected boolean usesX_M_DATETIME;
  protected boolean usesX_M_DOSTIME;
  protected boolean usesX_M_PEN;
  protected boolean xDateTimeAppended;
  protected boolean xDosTimeAppended;
  protected boolean xOutchAppended;

  private boolean             xCrsLinAppened;
  private boolean             xCheckDateTimeAppended;
  private boolean             xToDosTimeAppended;
  private Map<String,Integer> namedValues;


  protected AbstractTarget()
  {
    this.namedValues = new HashMap<>();
    setNamedValue( CPMTarget.BASIC_TARGET_NAME, 100 );
    setNamedValue( HueblerGraphicsMCTarget.BASIC_TARGET_NAME, 300 );
    setNamedValue( KC85Target.BASIC_TARGET_NAME, 400 );
    setNamedValue( KC854Target.BASIC_TARGET_NAME, 410 );
    setNamedValue( KC85Caos48Target.BASIC_TARGET_NAME, 420 );
    setNamedValue( KramerMCTarget.BASIC_TARGET_NAME, 600 );
    setNamedValue( SCCHTarget.BASIC_TARGET_NAME, 700 );
    setNamedValue( AC1Target.BASIC_TARGET_NAME, 710 );
    setNamedValue( LLC2HIRESTarget.BASIC_TARGET_NAME, 720 );
    setNamedValue( Z1013Target.BASIC_TARGET_NAME, 800 );
    setNamedValue( Z1013PetersTarget.BASIC_TARGET_NAME, 810 );
    setNamedValue( Z1013KRTTarget.BASIC_TARGET_NAME, 820 );
    setNamedValue( Z1013ZXTarget.BASIC_TARGET_NAME, 830 );
    setNamedValue( Z9001Target.BASIC_TARGET_NAME, 900 );
    setNamedValue( Z9001KRTTarget.BASIC_TARGET_NAME, 910 );
    setNamedValue( "GRAPHICSCREEN", -1 );
    for( String name : new String[] {
				"LASTSCREEN", "BLACK",
				"JOYST_LEFT", "JOYST_RIGHT",
				"JOYST_UP", "JOYST_DOWN",
				"JOYST_BUTTON1", "JOYST_BUTTON2" } )
    {
      setNamedValue( name, 0 );
    }
    for( String name : new String[] {
				"BLINKING", "BLUE", "CYAN", "GREEN",
				"MAGENTA", "RED", "WHITE", "YELLOW" } )
    {
      setNamedValue( name, 1 );
    }
    reset();
  }


  protected static String[] add( String[] a, String s )
  {
    String[] rv = null;
    if( a != null ) {
      rv      = new String[ a.length + 1 ];
      rv[ 0 ] = s;
      System.arraycopy( a, 0, rv, 1, a.length );
    } else {
      rv = new String[] { s };
    }
    return rv;
  }


  public void appendBssTo( AsmCodeBuf buf )
  {
    if( this.usesX_M_DATETIME ) {
      buf.append( "X_M_DATETIME:\tDS\t14\n" );
    }
    if( this.usesX_M_DOSTIME ) {
      buf.append( "X_M_DOSTIME:\tDS\t4\n" );
    }
    if( this.usesX_M_PEN ) {
      buf.append( "X_M_PEN:\tDS\t1\n" );
    }
  }


  public void appendDataTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Wenn das Zielsystem den Standard-VDIP-Treiber verwendet,
   * aber das VDIP-Model aktiviert und wieder deaktiviert werden muss,
   * wird hier der Aufruf der Routine zum Deaktivieren implementiert.
   */
  public void appendDisableVdipTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendDiskHandlerTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Wenn das Zielsystem den Standard-VDIP-Treiber verwendet,
   * aber das VDIP-Model aktiviert und wieder deaktiviert werden muss,
   * wird hier der Aufruf der Routine zum Aktivieren implementiert.
   *
   * Rueckgabe: CY=1 bedeutdet Fehler
   */
  public void appendEnableVdipTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendEtcPreXOutTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  public void appendEtcPastXOutTo( AsmCodeBuf buf )
  {
    // leer
  }


  public abstract void appendExitTo( AsmCodeBuf buf, boolean appTypeSub );


  public void appendPreExitTo( AsmCodeBuf buf )
  {
    // leer
  }


  protected void appendExitNoGraphicsScreenTo(
					AsmCodeBuf    buf,
					BasicCompiler compiler )
  {
    appendSwitchToTextScreenTo( buf );
    buf.append( "\tCALL\tXOUTST\n" );
    if( compiler.isLangCode( "DE" ) ) {
      buf.append( "\tDB\t\'Kein Grafik-SCREEN\'\n" );
    } else {
      buf.append( "\tDB\t\'No graphics screen\'\n" );
    }
    buf.append( "\tDB\t00H\n" );
    if( compiler.getBasicOptions().getPrintLineNumOnAbort()
	&& compiler.usesLibItem( BasicLibrary.LibItem.E_EXIT ) )
    {
      buf.append( "\tJP\tE_EXIT\n" );
    } else {
      buf.append( "\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      compiler.addLibItem( BasicLibrary.LibItem.XOUTNL );
    }
    compiler.addLibItem( BasicLibrary.LibItem.XOUTST );
  }


  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Zeilen unbekannt
  }


  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  public void appendInitTo( AsmCodeBuf buf )
  {
    if( this.usesX_M_PEN ) {
      buf.append( "\tLD\tA,01H\n"
		+ "\tLD\t(X_M_PEN),A\n" );
    }
  }


  /*
   * Die Methode fuegt die Routinen zur Abfrage der Tastatur hinzu.
   *
   * Parameter:
   *   xCheckBreak: Routine XCHECK_BREAK (Test auf CTRL-C)
   *                muss hinzugefuegt werden.
   *                kein Rueckgabewert
   *   XINKEY:      Routine XINKEY (Tastaturabfrage ohne warten)
   *                muss hinzugefuegt werden.
   *                Rueckgabewert: Tastencode in A, 0: keine Taste gedrueckt
   *   XINCH:       Routine XINCH (Tastaturabfrage mit warten)
   *                muss hinzugefuegt werden.
   *                Rueckgabewert: Tastencode in A
   *
   * Allgemein gilt:
   * Wenn die Parameter xCheckBreak oder canBreakOnInput gesetzt sind,
   * muss beim Druecken von CTRL-C das Programm abgebrochen werden.
   */
  public abstract void appendInputTo(
				AsmCodeBuf buf,
				boolean    xCheckBreak,
				boolean    xInkey,
				boolean    xInch,
				boolean    canBreakOnInput );


  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    // leer
  }


  public void appendSwitchToTextScreenTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendVdipHandlerTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Spalten unbekannt
  }


  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  /*
   * Einstellen der Randfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXBorderTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendXClsTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  public void appendXColorTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Abfrage der Zeile der aktuellen Cursor-Position
   */
  public void appendXCrsLinTo( AsmCodeBuf buf )
  {
    if( !this.xCrsLinAppened ) {
      buf.append( "XCRSLIN:\n"
		+ "XCRSPOS:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
      this.xCrsLinAppened = true;
    }
  }


  /*
   * Abfrage der Spalte der aktuellen Cursor-Position
   */
  public void appendXCrsPosTo( AsmCodeBuf buf )
  {
    appendXCrsLinTo( buf );
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0: Cursor ausschalten
   *     <>0: Cursor einschalten
   */
  public void appendXCursorTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Pruefen, ob in X_M_DATETIME gueltige Werte stehen
   * Parameter:
   *   X_M_DATETIME: Datum und Uhrzeit
   * Rueckgabe:
   *   CY=0: Werte in X_M_DATETIME gueltig
   *   CY=1: kein Zeitstempel in X_M_DATETIME
   */
  protected void appendXCheckDateTimeTo( AsmCodeBuf buf )
  {
    if( !this.xCheckDateTimeAppended ) {
      buf.append( "X_CHECK_DATETIME:\n"
		+ "\tCALL\tX_CHECK_DATETIME_1\n"	// Monat
		+ "\tDB\t04H,0DH\n"
		+ "\tJR\tZ,X_CHECK_DATETIME_2\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_CHECK_DATETIME_1\n"	// Tag
		+ "\tDB\t06H,20H\n"
		+ "\tJR\tZ,X_CHECK_DATETIME_2\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_CHECK_DATETIME_1\n"	// Stunde
		+ "\tDB\t08H,18H\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_CHECK_DATETIME_1\n"	// Minute
		+ "\tDB\t0AH,3CH\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_CHECK_DATETIME_1\n"	// Sekunde
		+ "\tDB\t0CH,3CH\n"
		+ "\tRET\n"
      /*
       * Parameter:
       *   1. nachfolgende Byte: Offset innerhalb X_M_DATETIME
       *   2. nachfolgende Byte: ober Grenze (exklusiv)
       * Rueckgabe:
       *   CY=1: obere Grenze erreicht oder ueberschritten
       */
		+ "X_CHECK_DATETIME_1:\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tE,(HL)\n"		// Offset
		+ "\tINC\tHL\n"
		+ "\tLD\tC,(HL)\n"		// obere Grenze
		+ "\tINC\tHL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tHL,X_M_DATETIME\n"
		+ "\tLD\tD,00H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
      // Zehner: A = A * 10
		+ "\tLD\tB,A\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,B\n"
		+ "\tADD\tA,A\n"
      // Einser addieren
		+ "\tADD\tA,(HL)\n"
		+ "\tRET\tZ\n"
      // obere Grenze testen
		+ "\tSUB\tC\n"
		+ "\tCCF\n"
		+ "\tRET\n"
		+ "X_CHECK_DATETIME_2:\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      this.usesX_M_DATETIME       = true;
      this.xCheckDateTimeAppended = true;
    }
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
  public void appendXDateTimeTo( AsmCodeBuf buf )
  {
    if( !this.xDateTimeAppended ) {
      buf.append( "XDATETIME:\n" );
      int ioBaseAddr = getGideIOBaseAddr();
      if( ioBaseAddr >= 0 ) {
	buf.append_LD_BC_nn( 0x0F00 | (ioBaseAddr + 5) );
	buf.append( "\tLD\tA,04H\n"
		+ "\tOUT\t(C),A\n"		// 24-Stunden-Modus
		+ "\tLD\tHL,X_M_DATETIME\n"
		+ "\tLD\t(HL),02H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,0BH\n"		// Register fuer Zehner Jahr
		+ "X_DATETIME_1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_DATETIME_1\n"
		+ "\tIN\tA,(C)\n"
		+ "\tAND\t0FH\n"		// CY=0
		+ "\tLD\t(HL),A\n" );
	/*
	 * Noch ist nicht sicher, ob das GIDE auch angeschlossen ist,
	 * und somit korrekte Werte gelesen wurden.
	 * Aus diesem Grund wird noch die Plausibilitaet grprueft.
	 */
	appendXCheckDateTimeTo( buf );
	this.usesX_M_DATETIME = true;
      }
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
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
  public void appendXDosTimeTo( AsmCodeBuf buf )
  {
    if( !this.xDosTimeAppended ) {
      buf.append( "XDOSTIME:\n" );
      int ioBaseAddr = getGideIOBaseAddr();
      if( ioBaseAddr >= 0 ) {
	buf.append( "\tCALL\tXDATETIME\n"
		+ "\tRET\tC\n"
		+ "\tJP\tX_TO_DOSTIME\n" );
	appendXToDosTimeTo( buf );
	appendXDateTimeTo( buf );
      } else {
	buf.append( "\tSCF\n"
		+ "\tRET\n" );
      }
      this.xDosTimeAppended = true;
    }
  }


  /*
   * Wenn die Methode supportsXHLINE() true liefert,
   * muss diese Methode hier die Routine XHLINE implementieren.
   *
   * XHLINE: Zeichnen einer horizontalen Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate, nicht kleiner 0
   *   HL: Y-Koordinate
   */
  public void appendXHLineTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Einstellen der Vordergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXInkTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   */
  public void appendXJoyTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Ausgabe eines Zeichens auf dem Drucker
   */
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tRET\n" );
  }


  /*
   * Ausgabe des Zeichens in A auf dem Bildschirm
   */
  public abstract void appendXOutchTo( AsmCodeBuf buf );


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,0AH\n" );
    if( this.xOutchAppended ) {
      buf.append( "\tJP\tXOUTCH\n" );
    } else {
      appendXOutchTo( buf );
    }
  }


  /*
   * Einstellen der Hintergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Einstellen des Stiftes
   * Parameter:
   *   A: Stift (0: Ignorieren, 1: Normal, 2: Loeschen, 3: XOR-Mode)
   */
  public void appendXPenTo( AsmCodeBuf buf )
  {
    buf.append( "XPEN:\tLD\t(X_M_PEN),A\n"
		+ "\tRET\n" );
    this.usesX_M_PEN = true;
  }


  /*
   * Wenn die Methode supportsXPAINT_LEFT_RIGHT() true liefert,
   * muss diese Methode hier die Routinen XPAINT_LEFT und XPAINT_RIGHT
   * implementieren, anderenfalls XPAINT,
   * sofern das Zielsystem Grafik unterstuetzt.
   *
   * XPAINT:
   *   Setzen eines Pixels ohne Beruecksichtigung des eingestellten Stiftes
   *   bei gleichzeitigem Test, ob dieser schon gesetzt ist
   *   Parameter:
   *     DE: X-Koordinate
   *     HL: Y-Koordinate
   *   Rueckgabe:
   *     CY=1: Pixel bereits gesetzt oder ausserhalb des sichtbaren Bereichs
   *
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
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Farbe eines Pixels ermitteln
   * Parameter:
   *   DE: X-Koordinate (0...255)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPResTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Setzen eines Pixels unter Beruecksichtigung des eingestellten Stiftes
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPSetTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate (0...255)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel exisitiert nicht
   */
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer (0: Standard)
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  public void appendXScreenTo( AsmCodeBuf buf )
  {
    buf.append( "XSCREEN:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"			// CY=0
		+ "\tRET\tZ\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
  }


  protected void appendXToDosTimeTo( AsmCodeBuf buf )
  {
    if( !this.xToDosTimeAppended ) {
      buf.append( "X_TO_DOSTIME:\n"
		+ "\tLD\tHL,X_M_DATETIME+02H\n"
		+ "\tLD\tB,06H\n"
      // BCD in binaere Werte umrechnen
		+ "X_TO_DOSTIME_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tC,A\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,C\n"
		+ "\tADD\tA,A\n"
		+ "\tADD\tA,(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_TO_DOSTIME_1\n"
      // 32-Bit-Zeitstempel zusammenbauen
		+ "\tLD\tHL,X_M_DOSTIME\n"
		+ "\tLD\tDE,X_M_DATETIME+03H\n"
		+ "\tLD\tA,(DE)\n"		// Jahr, zweistellig
      // 80..99 -> 00..19, 00..79 -> 20..99
		+ "\tSUB\t50H\n"		// 80
		+ "\tJR\tNC,X_TO_DOSTIME_2\n"
		+ "\tADD\tA,64H\n"
		+ "X_TO_DOSTIME_2:\n"
		+ "\tSLA\tA\n"
		+ "\tLD\tC,A\n"			// Bit 7..1: Jahr
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"		// Monat
		+ "\tLD\tB,05H\n"
		+ "\tCALL\tX_TO_DOSTIME_4\n"
		+ "\tJR\tNC,X_TO_DOSTIME_3\n"
		+ "\tINC\tC\n"			// Bit 0: Monat
		+ "X_TO_DOSTIME_3:\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tC,A\n"			// Bit 7..5: Monat
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"		// Tag
		+ "\tAND\t1FH\n"
		+ "\tOR\tC\n"			// Bit 4..0: Tag
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"		// Stunde
		+ "\tLD\tB,03H\n"
		+ "\tCALL\tX_TO_DOSTIME_4\n"
		+ "\tLD\tC,A\n"			// Bit 7..3: Tag
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"		// Minute
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tAND\t07H\n"
		+ "\tOR\tC\n"			// Bit 2..0: Minute
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(DE)\n"		// Minute
		+ "\tLD\tB,05H\n"
		+ "\tCALL\tX_TO_DOSTIME_4\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"		// Sekunde
		+ "\tRRCA\n"
		+ "\tAND\t1FH\n"
		+ "\tOR\tC\n"			// Bit 4..0: Sekunde, CY=0
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tHL,X_M_DOSTIME\n"
		+ "\tRET\n"
      // Wert in A um B Stellen nach links schieben
		+ "X_TO_DOSTIME_4:\n"
		+ "\tSLA\tA\n"
		+ "\tDJNZ\tX_TO_DOSTIME_4\n"
		+ "\tRET\n" );
      this.usesX_M_DATETIME   = true;
      this.usesX_M_DOSTIME    = true;
      this.xToDosTimeAppended = true;
    }
  }


  public abstract int      get100msLoopCount();
  public abstract String[] getBasicTargetNames();
  public abstract int      getDefaultBegAddr();


  /*
   * Die Methode besagt, inwieweit der von dem Zielsystem erzeugte
   * Programmcode auf dem uebergebenen emulierten System lauffaehig ist:
   *   0: nicht lauffaehig
   *   1: nur Basisfunktionen lauffaehig
   *   2: Standardfunktionen lauffaehig
   *   3: voll lauffaehig inkl. Unterstuetzung fuer spezielle Hardware
   */
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    return 0;
  }


  public FileFormat getDefaultFileFormat()
  {
    return FileFormat.BIN;
  }


  public String getDiskHandlerLabel()
  {
    return null;
  }


  public int getDiskIOChannelSize()
  {
    return 0;
  }


  public int getGideIOBaseAddr()
  {
    return -1;
  }


  public int getMaxAppNameLen()
  {
    return 0;
  }


  public int getNamedValue( String name )
  {
    Integer value = this.namedValues.get( name );
    return value != null ? value.intValue() : 0;
  }


  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    return null;
  }


  public int[] getTargetIDs()
  {
    int[]    rv          = null;
    String[] targetNames = getBasicTargetNames();
    if( targetNames != null ) {
      rv = new int[ targetNames.length ];
      for( int i = 0; i < targetNames.length; i++ ) {
	rv[ i ] = getNamedValue( targetNames[ i ] );
      }
    } else {
      rv = new int[ 0 ];
    }
    return rv;
  }


  /*
   * Wenn das Zielsystem den Standard-VDIP-Treiber verwendet moechte,
   * muss diese Methode die IO-Adresse bzw. IO-Adressen der VDIP-PIO
   * zuruecklieferen.
   */
  public int[] getVdipBaseIOAddresses()
  {
    return null;
  }


  /*
   * Wenn das Zielsystem den eigenen VDIP-Treiber hat,
   * muss diese Methode die Assembler-Marke der Handler-Routine
   * zuruecklieferen.
   */
  public String getVdipHandlerLabel()
  {
    return null;
  }


  public boolean isReservedWord( String name )
  {
    return this.namedValues.containsKey( name.toUpperCase() );
  }


  public boolean needsEnableDisableVdip()
  {
    return false;
  }


  public void preAppendLibraryCode( BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Diese Methode wird vor dem Compilieren aufgerufen,
   * um das Object in einen definierten Ausgangszustand zu versetzen.
   */
  public void reset()
  {
    this.usesX_M_DATETIME       = false;
    this.usesX_M_DOSTIME        = false;
    this.usesX_M_PEN            = false;
    this.xCrsLinAppened         = false;
    this.xCheckDateTimeAppended = false;
    this.xDateTimeAppended      = false;
    this.xDosTimeAppended       = false;
    this.xOutchAppended         = false;
    this.xToDosTimeAppended     = false;
  }


  protected void setNamedValue( String name, int value )
  {
    this.namedValues.put( name, value );
  }


  public boolean startsWithDiskDevice( String fileName )
  {
    return false;
  }


  public boolean supportsColors()
  {
    return false;
  }


  public boolean supportsBorderColor()
  {
    return false;
  }


  /*
   * Wenn die Methode true zurueckliefert,
   * muessen die Methoden appendXPRES(...), appendXPSET(...)
   * und appendXPTST(...) die entsprechenden Routinen anhaengen.
   */
  public boolean supportsGraphics()
  {
    return false;
  }


  public boolean supportsVdip()
  {
    boolean rv = (getDiskHandlerLabel() != null);
    if( !rv ) {
      int[] vdipIOAddrs = getVdipBaseIOAddresses();
      if( vdipIOAddrs != null ) {
	if( vdipIOAddrs.length > 0 ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  public boolean supportsXCLS()
  {
    return false;
  }


  public boolean supportsXCURSOR()
  {
    return false;
  }


  public boolean supportsXHLINE()
  {
    return false;
  }


  public boolean supportsXJOY()
  {
    return false;
  }


  public boolean supportsXLOCATE()
  {
    return false;
  }


  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return false;
  }


  public boolean supportsXLPTCH()
  {
    return false;
  }


  /*
   * Wenn diese Methode true liefert,
   * muessen die Methoden appendXDateTimeTo und appendXDosTimeTo
   * sinnvollen Programmcode erzeugen.
   */
  public boolean supportsXTIME()
  {
    return (getGideIOBaseAddr() >= 0);
  }
}
