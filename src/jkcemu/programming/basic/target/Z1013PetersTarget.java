/*
 * (c) 2013-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z1013-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Unterstuetzung des 64x16-Modus der Peters-Platine
 */

package jkcemu.programming.basic.target;

import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;


public class Z1013PetersTarget extends Z1013Target
{
  public static final String BASIC_TARGET_NAME = "TARGET_Z1013_64X16";

  private boolean needsScreenSizeHChar;
  private boolean needsScreenSizeWChar;
  private boolean needsScreenSizePixel;
  private boolean usesScreens;


  public Z1013PetersTarget()
  {
    setNamedValue( "LASTSCREEN", 1 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesScreens ) {
      buf.append( "X_M_SCREEN:\tDS\t1\n" );
    }
  }


  @Override
  protected void appendCheckGraphicScreenTo(
				AsmCodeBuf    buf,
				BasicCompiler compiler )
  {
    if( this.usesScreens ) {
      buf.append( "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PINFO_1\n" );
      appendExitNoGraphicsScreenTo( buf, compiler );
    }
  }


  @Override
  public void appendEtcPastXOutTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "X_SCREEN0:\n"
		+ "\tIN\tA,(04H)\n"
		+ "\tAND\t7FH\n"
		+ "\tOUT\t(04H),A\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_SCREEN),A\n"
		+ "\tRET\n" );
    }
    if( this.needsScreenSizeHChar || this.needsScreenSizeWChar ) {
      if( this.usesScreens ) {
	if( this.needsScreenSizeHChar ) {
	  buf.append( "X_HCHR:\tLD\tHL,0010H\n" );
	  if( this.needsScreenSizeWChar ) {
	    buf.append( "\tJR\tX_HWCHR\n" );
	  }
	}
	if( this.needsScreenSizeWChar ) {
	  buf.append( "X_WCHR:\tLD\tHL,0040H\n" );
	}
	buf.append( "X_HWCHR:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tDEC\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,0020H\n"
		+ "\tRET\n" );
      } else {
	buf.append( "X_HCHR:\n"
		+ "X_WCHR:\tLD\tHL,0020H\n"
		+ "\tRET\n" );
      }
    }
    if( this.needsScreenSizePixel ) {
      buf.append( "X_HPIX:\n"
		+ "X_WPIX:\tLD\tHL,0040H\n" );
      if( this.usesScreens ) {
	buf.append( "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,00H\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendPreExitTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "\tCALL\tX_SCREEN0\n" );
    }
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HCHR\n" );
    this.needsScreenSizeHChar = true;
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HPIX\n" );
    this.needsScreenSizePixel = true;
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesScreens ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_SCREEN),A\n"
		+ "\tCALL\tX_SCREEN0\n" );
    }
  }


  @Override
  public void appendSwitchToTextScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "\tCALL\tX_SCREEN0\n" );
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WCHR\n" );
    this.needsScreenSizeWChar = true;
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WPIX\n" );
    this.needsScreenSizePixel = true;
  }


  /*
   * Abfrage der Zeile der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsLinTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSLIN:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
    /*
     * M_SCREEN=0: B=3
     * M_SCREEN=1: B=2
     */
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tSUB\t03H\n"
		+ "\tNEG\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t03H\n"
		+ "X_CRSLIN_1:\n"
		+ "\tSLA\tL\n"
		+ "\tRLA\n"
		+ "\tDJNZ\tX_CRSLIN_1\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  /*
   * Abfrage der Spalte der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsPosTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSPOS:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
		+ "\tLD\tC,3FH\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_CRSPOS_1\n"
		+ "\tLD\tC,1FH\n"
		+ "X_CRSPOS_1:\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\tB\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "XLOCATE:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_LOCATE_1\n"
		+ "\tLD\tA,1FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_LOCATE_3\n"
		+ "\tJR\tX_LOCATE_2\n"
		+ "X_LOCATE_1:\n"
		+ "\tLD\tA,0FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_LOCATE_3\n"
		+ "\tADD\tHL,HL\n"
		+ "X_LOCATE_2:\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tBC,0EC00H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tLD\t(002BH),HL\n"
		+ "\tRET\n"
		+ "X_LOCATE_3:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
    } else {
      super.appendXLocateTo( buf );
    }
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      if( !this.xOutchAppended ) {
	buf.append( "XOUTCH:\tCP\t0AH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_OUTCH_1\n"
		+ "\tLD\tA,D\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n"
	// Bildschirmroutine fuer 64x16-Modus
		+ "X_OUTCH_1:\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,X_OUTCH_7\n"
		+ "\tCP\t09H\n"
		+ "\tJR\tZ,X_OUTCH_2\n"
		+ "\tCP\t0CH\n"
		+ "\tJP\tZ,X_OUTCH_8\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,X_OUTCH_9\n"
		+ "\tLD\t(HL),D\n"
	// Cursor eins weiter
		+ "X_OUTCH_2:\n"
		+ "\tINC\tHL\n"
	// ggf. scrollen
		+ "X_OUTCH_3:\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t0F0H\n"
		+ "\tCALL\tNC,X_OUTCH_5\n"
	// Cursor setzen
		+ "X_OUTCH_4:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(001FH),A\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tLD\t(002BH),HL\n"
		+ "\tRET\n"
	// Bildschirm scrollen
		+ "X_OUTCH_5:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,0EC40H\n"
		+ "\tLD\tDE,0EC00H\n"
		+ "\tLD\tBC,03C0H\n"
		+ "\tLDIR\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\tB,40H\n"
		+ "X_OUTCH_6:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tX_OUTCH_6\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\n"
	// Cursor links
		+ "X_OUTCH_7:\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t0ECH\n"
		+ "\tJR\tNC,X_OUTCH_4\n"
		+ "\tLD\tHL,0EFFFH\n"
		+ "\tJR\tX_OUTCH_4\n"
	// Bildschirm loeschen
		+ "X_OUTCH_8:\n"
		+ "\tLD\tHL,0EC00H\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,0EC01H\n"
		+ "\tLD\tBC,03FFH\n"
		+ "\tLDIR\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tX_OUTCH_4\n"
	// Cursor auf neue Zeile
		+ "X_OUTCH_9:\n"
		+ "\tLD\tB,20H\n"
		+ "X_OUTCH_10:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t03FH\n"
		+ "\tJR\tNZ,X_OUTCH_10\n"
		+ "\tJR\tX_OUTCH_3\n" );
	this.xOutchAppended = true;
      }
    } else {
      super.appendXOutchTo( buf );
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
   *   HL=-1: Pixel existiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( this.usesScreens ) {
      if( !this.xptestAppended ) {
	buf.append( "XPOINT:\n"
		+ "XPTEST:\tLD\tA,(X_M_SCREEN)\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
		+ "\tCALL\tX_PINFO_1\n"
		+ "\tJR\tC,X_PTEST_1\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
	appendPixUtilTo( buf, compiler );
	this.xptestAppended = true;
      }
    } else {
      super.appendXPTestTo( buf, compiler );
    }
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer
   *        0: Text mit 32x32 Zeichen und Grafik mit 64x64 Pixel
   *        1: Text mit 64x16 Zeichen
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  @Override
  public void appendXScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "XSCREEN:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_SCREEN_1\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tCP\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tC,04H\n"
		+ "\tIN\tB,(C)\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_SCREEN_2\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_SCREEN_3\n"
		+ "X_SCREEN_1:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_SCREEN_2:\n"
		+ "\tRES\t7,B\n"		// SCREEN 0
		+ "\tJR\tX_SCREEN_4\n"
		+ "X_SCREEN_3:\n"
		+ "\tSET\t7,B\n"		// SCREEN 1
		+ "X_SCREEN_4:\n"
		+ "\tOUT\t(C),B\n"
		+ "\tLD\t(X_M_SCREEN),A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    }
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return add( super.getBasicTargetNames(), BASIC_TARGET_NAME );
  }


  @Override
  public void preAppendLibraryCode( BasicCompiler compiler )
  {
    super.preAppendLibraryCode( compiler );
    if( compiler.usesLibItem( BasicLibrary.LibItem.SCREEN )
	|| compiler.usesLibItem( BasicLibrary.LibItem.XSCREEN ) )
    {
      this.usesScreens = true;
    }
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsScreenSizeHChar = false;
    this.needsScreenSizeWChar = false;
    this.needsScreenSizePixel = false;
    this.usesScreens          = false;
  }


  @Override
  public String toString()
  {
    return "Z1013 mit Peters-Platine (64x16 Zeichen)";
  }
}
