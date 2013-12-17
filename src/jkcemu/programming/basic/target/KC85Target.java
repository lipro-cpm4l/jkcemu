/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KC85-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.KC85;
import jkcemu.programming.basic.*;


public class KC85Target extends AbstractTarget
{
  private boolean needsFullWindow;
  private boolean usesColors;
  private boolean usesJoystick;
  private boolean usesOSVersion;
  private boolean usesM052;
  private boolean xpresAppended;
  private boolean xpsetAppended;


  public KC85Target()
  {
    reset();
  }


  @Override
  public void appendDataTo( AsmCodeBuf buf )
  {
    super.appendDataTo( buf );
    if( this.usesOSVersion ) {
      buf.append( "X_CAOS:\tDB\t'CAOS '\n"
		+ "\tDB\t00H\n" );
    }
  }


  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesColors ) {
      buf.append( "X_M_INK:\n"
		+ "\tDS\t1\n" );
    }
    if( this.usesOSVersion ) {
      buf.append( "X_M_OS:\tDS\t1\n" );
    }
    if( this.usesM052 ) {
      buf.append( "X_M_M052SLOT:\n"
		+ "\tDS\t1\n"
		+ "X_M_M052STAT:\n"
		+ "\tDS\t1\n"
		+ "X_M_M052USED:\n"
		+ "\tDS\t1\n" );
    }
  }


  @Override
  public void appendEnableKCNet( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_M052\n" );
    this.usesM052 = true;
  }


  @Override
  public void appendEnableVdip( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_M052\n" );
    this.usesM052 = true;
  }


  @Override
  public void appendEtc( AsmCodeBuf buf )
  {
    super.appendEtc( buf );
    if( this.usesM052 ) {
      /*
       * Aktivieren des Moduls M052
       * Rueckgabewert:
       *   CY=0: M052 aktiv
       *   CY=1: M052 nicht gefunden
       */
      buf.append( "X_M052:\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tBC,0880H\n"
		+ "X_M052_1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tCP\t0FDH\n"
		+ "\tJR\tZ,X_M052_2\n"
		+ "\tINC\tB\n"
		+ "\tJR\tNZ,X_M052_1\n"
		+ "\tSCF\n"
		+ "\tRET\n"			// M052 nicht gefunden, CY=1
		+ "X_M052_2:\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\tH,0B8H\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
		+ "\tOR\t04H\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tLD\t(X_M_M052USED),A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    if( this.usesM052 ) {
      buf.append( "\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,(X_M_M052STAT)\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,(X_M_M052SLOT)\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n" );
    }
    buf.append( "X_EXIT1:\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
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
    if( this.usesOSVersion ) {
      /*
       * Zeichenkette "CAOS " ab F000h suchen und das naechste
       * ACSII-Zeichen als Hauptversion interpretieren,
       * wenn es eine Ziffer ist.
       */
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_OS),A\n"
		+ "\tLD\tBC,0EFFFH\n"
		+ "X_IOS1:\tINC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_IOS4\n"		// nicht gefunden
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tLD\tDE,X_CAOS\n"
		+ "X_IOS2:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_IOS3\n"		// gefunden
		+ "\tCP\t(HL)\n"
		+ "\tJR\tNZ,X_IOS1\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tX_IOS2\n"
		+ "X_IOS3:\tLD\tA,(HL)\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,X_IOS4\n"
		+ "\tCP\t0A0H\n"
		+ "\tJR\tNC,X_IOS4\n"
		+ "\tLD\t(X_M_OS),A\n"
		+ "X_IOS4:\n" );
    }
    if( this.usesColors ) {
      buf.append( "\tLD\tA,(0B7A3H)\n"
		+ "\tAND\t0F8H\n"
		+ "\tLD\t(X_M_INK),A\n" );
    }
    if( this.needsFullWindow ) {
      // Fenstergroesse pruefen und ggf. auf Maximalewerte setzen
      buf.append( "\tLD\tHL,(0B79CH)\n"		// Fensterposition
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,X_IWN1\n"
		+ "\tLD\tHL,(0B79EH)\n"		// Fenstergroesse
		+ "\tLD\tA,H\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,X_IWN1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t28H\n"
		+ "\tJR\tNC,X_IWN2\n"
		+ "X_IWN1:\tLD\tHL,0000H\n"
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
		+ "X_IWN2:\n" );
    }
    if( this.usesM052 ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
		+ "\tLD\t(X_M_M052USED),A\n" );
    }
  }


  @Override
  public void appendInput(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xckbrk ) {
      buf.append( "XCKBRK:\tCALL\t0F003H\n"
		+ "\tDB\t2AH\n"
		+ "\tRET\tNC\n"
		+ "\tJR\tXBREAK\n" );
    }
    if( xinkey ) {
      buf.append( "XINKEY:\tCALL\t0F003H\n"
		+ "\tDB\t0EH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tJR\tC,XINKE1\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n"
		+ "XINKE1:\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tRET\tC\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n" );
      }
    }
    if( xinch ) {
      buf.append( "XINCH:\tCALL\t0F003H\n"
		+ "\tDB\t16H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendProlog(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
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
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0028H\n" );
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0140H\n" );
  }


  @Override
  public void appendXCLS( AsmCodeBuf buf )
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
  public void appendXCOLOR( AsmCodeBuf buf )
  {
    buf.append( "XCOLOR:\tLD\tA,L\n"
		+ "\tAND\t1FH\n"
		+ "\tLD\tL,A\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tLD\t(X_M_INK),A\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Zeichnen einer horizontaler Linie,
   *   Bei CAOS 4 oder hoeher wird die LINE-Funktion von CAOS verwendet,
   *   da diese etwas schneller als die JKCEMU-Routine ist.
   *   Die LINE-Funktion im CAOS 3 ist dagegen wesentlich langsamer
   *   und wird deshalb nicht verwendet.
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXHLINE( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XHLINE:" );
    if( this.usesX_MPEN ) {
      buf.append( "\tLD\tA,(X_MPEN)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tEX\tAF,AF\'\n" );
    }
    buf.append( "\tBIT\t7,B\n"			// Laenge pruefen
		+ "\tRET\tNZ\n"
		+ "\tBIT\t7,H\n"		// Y pruefen
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(X_M_OS)\n"
		+ "\tCP\t04H\n"
		+ "\tJR\tC,XHLIN2\n"
		+ "\tLD\t(0B782H),DE\n"
		+ "\tLD\t(0B784H),HL\n"
		+ "\tLD\t(0B788H),HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\t(0B786H),HL\n"
		+ "\tEXX\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
    if( this.usesX_MPEN ) {
      buf.append( "\tLD\tB,02H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,XHLIN1\n"
		+ "\tDEC\tB\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XHLIN1\n"
		+ "\tDEC\tB\n"
		+ "XHLIN1:\tLD\tA,(X_M_INK)\n"
		+ "\tOR\tB\n" );
    } else {
      buf.append( "\tLD\tA,(X_M_INK)\n" );
    }
    buf.append( "\tLD\t(0B7D6H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t3EH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tEXX\n"
		+ "\tRET\n"
		+ "XHLIN2:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tBC\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tXHLIN2\n" );
    appendXPSET( buf, compiler );
    this.needsFullWindow = true;
    this.usesOSVersion   = true;
  }


  @Override
  public void appendXINK( AsmCodeBuf buf )
  {
    buf.append( "XINK:\tLD\tA,L\n"
		+ "\tAND\t1FH\n"
		+ "\tLD\tL,A\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tLD\t(X_M_INK),A\n"
		+ "\tLD\tA,01H\n"
		+ "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   *         Bit 0: links
   *         Bit 1: rechts
   *         Bit 2: runter
   *         Bit 3: hoch
   *         Bit 4: Aktionsknopf
   */
  public void appendXJOY( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,XJOY1\n"
		+ "\tIN\tA,(90H)\n"
		+ "\tCPL\n"
		+ "\tLD\tB,A\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t03H\n"	// links und rechts maskieren
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t04H\n"	// runter maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t08H\n"	// hoch maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t10H\n"	// Aktionsknopf 1 maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t20H\n"	// Aktionsknopf 2 maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY1:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    this.usesJoystick = true;
  }


  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tH,E\n"
		+ "\tLD\t(0B7A0H),HL\n"
		+ "\tRET\n" );
    this.needsFullWindow = true;
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tCALL\t0F003H\n"
		+ "\tDB\t02H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tCALL\t0F003H\n"
		+ "\tDB\t24H\n"
		+ "\tRET\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXPAPER( AsmCodeBuf buf )
  {
    buf.append( "XPAPER:\tLD\tA,L\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,(X_M_INK)\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPRES( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpresAppended ) {
      buf.append( "XPRES:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "XPRES1:\tLD\tA,L\n"
		+ "\tLD\t(0B7D5H),A\n"
		+ "\tLD\t(0B7D3H),DE\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t2FH\n"
		+ "\tRET\n" );
      this.needsFullWindow = true;
      this.xpresAppended   = true;
    }
  }


  /*
   * Setzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPSET( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpsetAppended ) {
      buf.append( "XPSET:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n" );
      if( this.usesX_MPEN ) {
	buf.append( "\tLD\tB,00H\n"
		+ "\tLD\tA,(X_MPEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET1\n"
		+ "\tRET\tM\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPRES1\n"
		+ "\tINC\tB\n"
		+ "\tLD\tA,(X_M_OS)\n"
		+ "\tCP\t04H\n"
		+ "\tJR\tC,XPSET3\n"
		+ "XPSET1:" );
	this.usesOSVersion = true;
      }
      buf.append( "\tLD\tA,L\n"
		+ "\tLD\t(0B7D5H),A\n"
		+ "\tLD\t(0B7D3H),DE\n"
		+ "\tLD\tA,(X_M_INK)\n" );
      if( this.usesX_MPEN ) {
	buf.append( "\tOR\tB\n"
		+ "XPSET2:" );
      }
      buf.append( "\tLD\t(0B7D6H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t30H\n"
		+ "\tRET\n" );
      if( this.usesX_MPEN ) {
	buf.append( "XPSET3:\tCALL\tXPRES1\n"
		+ "\tRET\tC\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(X_M_INK)\n"
		+ "\tJR\tXPSET2\n" );
	appendXPRES( buf, compiler );
      }
      this.needsFullWindow = true;
      this.usesColors      = true;
      this.xpsetAppended   = true;
    }
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL=0: Pixel nicht gesetzt
   *   HL=1: Pixel gesetzt
   */
  @Override
  public void appendXPTEST( AsmCodeBuf buf, BasicCompiler compiler )
  {
    /*
     * Da es keine Systemfunktion zum Testen eines Pixels gibt,
     * aber die benoetigte Information mit der Funktion zum Loeschen
     * eines Pixels gewonnen werden kann,
     * wird das Pixel geloescht und, sofern es gesetzt war,
     * wieder mit der gleichen Vordergrundfarbe neu gesetzt.
     */
    buf.append( "XPTEST:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XPTST1\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\t(0B7D5H),A\n"
		+ "\tLD\t(0B7D3H),DE\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t2FH\n"
		+ "\tJR\tC,XPTST1\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tAND\t0F8H\n"
		+ "\tLD\t(0B7D6H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t30H\n"
		+ "\tLD\tHL,0001H\n"
		+ "\tRET\n"
		+ "XPTST1:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'KC85\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof KC85) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 50;
  }


  @Override
  public int getColorBlack()
  {
    return 0;
  }


  @Override
  public int getColorBlinking()
  {
    return 0x10;
  }


  @Override
  public int getColorBlue()
  {
    return 0x01;
  }


  @Override
  public int getColorCyan()
  {
    return 0x05;
  }


  @Override
  public int getColorGreen()
  {
    return 0x04;
  }


  @Override
  public int getColorMagenta()
  {
    return 0x03;
  }


  @Override
  public int getColorRed()
  {
    return 0x02;
  }


  @Override
  public int getColorWhite()
  {
    return 0x07;
  }


  @Override
  public int getColorYellow()
  {
    return 0x06;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0300;
  }


  @Override
  public int getGraphicScreenNum()
  {
    return 0;
  }


  @Override
  public int getKCNetBaseIOAddr()
  {
    return 0x28;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0x2C };
  }


  @Override
  public boolean needsEnableKCNet()
  {
    return true;
  }


  @Override
  public boolean needsEnableVdip()
  {
    return true;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsFullWindow = false;
    this.usesColors      = false;
    this.usesJoystick    = false;
    this.usesOSVersion   = false;
    this.usesM052        = false;
    this.xpresAppended   = false;
    this.xpsetAppended   = false;
  }


  @Override
  public boolean supportsAppName()
  {
    return true;
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
  public boolean supportsXLOCAT()
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
    return "KC85/2..5, HC900";
  }
}
