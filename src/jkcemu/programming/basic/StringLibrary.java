/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Bibliothek fuer String-Funktionen
 */

package jkcemu.programming.basic;

import java.util.Set;


public class StringLibrary
{
  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();

    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_CHR ) ) {
      /*
       * Umwandlung eines Zeichencodes in eine Zeichenkette
       *
       * Parameter:
       *   L: Zeichencode
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_CHR_L:\n"
		+ "\tLD\tA,L\n"
		+ "F_S_CHR_A:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "F_S_CHR_X:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_LEFT ) ) {
      /*
       * Anfang (linker Teil) einer Zeichenkette zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl der zurueckzuliefernden Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_LEFT:\n"
			+ "\tLD\tA,B\n"
			+ "\tOR\tA\n"
			+ "\tJP\tM,E_INVALID_PARAM\n"
			+ "\tLD\tDE,M_TMP_STR_BUF\n"
			+ "\tPUSH\tDE\n"
			+ "\tCALL\tSTR_N_COPY\n"
			+ "\tPOP\tHL\n"
			+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_LEN );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( BasicLibrary.LibItem.STR_N_COPY );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_LOWER ) ) {
      /*
       * Zeichenkette in Kleinbuchstaben wandeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   HL: Zeiger auf die umgewandelte Zeichenkette
       */
      buf.append( "F_S_LOWER:\n"
		+ "\tLD\tDE,M_TMP_STR_BUF-1\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "F_S_LOWER_1:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t41H\n"
		+ "\tJR\tC,F_S_LOWER_2\n"
		+ "\tCP\t5BH\n"
		+ "\tJR\tNC,F_S_LOWER_2\n"
		+ "\tADD\tA,20H\n"
		+ "F_S_LOWER_2:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_S_LOWER_3\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_LOWER_1\n"
		+ "\tLD\t(DE),A\n"
		+ "F_S_LOWER_3:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_TRIM ) ) {
      /*
       * Weisse Leerzeichen am Anfang und Ende einer Zeichenkette abschneiden
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "F_S_TRIM:\n"
		+ "\tCALL\tF_S_LTRIM\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_LTRIM );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_RTRIM );
      // direkt weiter mit F_S_RTRIM !!!
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_RTRIM ) ) {
      /*
       * Weisse Leerzeichen am Ende einer Zeichenkette abschneiden
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "F_S_RTRIM:\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tDEC\tDE\n"		// Zeiger auf letztes Zeichen
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "F_S_RTRIM_1:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_S_RTRIM_3\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,F_S_RTRIM_2\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "F_S_RTRIM_2:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_RTRIM_1\n"
		+ "F_S_RTRIM_3:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_LTRIM ) ) {
      /*
       * Weisse Leerzeichen am Anfang einer Zeichenkette abschneiden
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "F_S_LTRIM:\n"
		+ "\tDEC\tHL\n"
		+ "F_S_LTRIM_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,F_S_LTRIM_1\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_MID_N ) ) {
      /*
       * Teil einer Zeichenkette ab einer gegebenen Position
       * mit einer gegebenen Laenge zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   DE: Position, ab der die Zeichenkette geliefert wird
       *   BC: Laenge der Teilzeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die Teilzeichenkette
       */
      buf.append( "F_S_MID_N:\n"
		+ "\tCALL\tF_S_MID\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tSTR_N_COPY\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_MID );
      compiler.addLibItem( BasicLibrary.LibItem.STR_N_COPY );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_MID ) ) {
      /*
       * Teil einer Zeichenkette ab einer gegebenen Position zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   DE: Position, ab der die Zeichenkette geliefert wird
       * Rueckgabewert:
       *   HL: Zeiger auf die Teilzeichenkette
       */
      buf.append( "F_S_MID:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tE\n"
		+ "\tJP\tZ,E_INVALID_PARAM\n"
		+ "F_S_MID_1:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tF_S_MID_1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_MIRROR ) ) {
      /*
       * Zeichenkette spiegeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die gespiegelte Zeichenkette
       */
      buf.append( "F_S_MIRROR:\n"
		+ "\tLD\tBC,0FFFFH\n"
		+ "\tDEC\tHL\n"
		+ "F_S_MIRROR_1:\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_S_MIRROR_1\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_S_MIRROR_2\n"
		+ "\tLD\tB,0FFH\n"	// Zeichenkette zu lang
		+ "\tJR\tF_S_MIRROR_3\n"
		+ "F_S_MIRROR_2:\n"
		+ "\tOR\tC\n"
		+ "\tRET\tZ\n"		// leere Zeichenkette
		+ "\tLD\tB,C\n"
		+ "F_S_MIRROR_3:\n"
		+ "\tLD\tDE,M_TMP_STR_BUF-1\n"
		+ "F_S_MIRROR_4:\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tF_S_MIRROR_4\n"
		+ "\tINC\tDE\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_RIGHT ) ) {
      /*
       * Ende (rechter Teil) einer Zeichenkette zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl der zurueckzuliefernden Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_RIGHT:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_I2_LEN\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "F_S_RIGHT_1:\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,F_S_RIGHT_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_LEN );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_STRING_I2_C ) ) {
      /*
       * Vervielfaeltigen eines Zeichens
       *
       * Parameter:
       *   L:  Zeichen
       *   BC: Anzahl
       * Rueckgabewert:
       *   HL: Zeiger auf die erzeugte Zeichenkette
       */
      buf.append( "F_S_STRING_I2_C:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_STRING_I2_C_1\n" );
      buf.append_LD_HL_xx( BasicLibrary.EMPTY_STRING_LABEL );
      buf.append( "\tRET\n"
		+ "F_S_STRING_I2_C_1:\n"
		+ "\tLD\tE,L\n"
		+ "\tLD\tHL," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNC,F_S_STRING_I2_C_2\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "F_S_STRING_I2_C_2:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tPUSH\tHL\n"
		+ "F_S_STRING_I2_C_3:\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_STRING_I2_C_3\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_STRING_I2_S ) ) {
      /*
       * Vervielfaeltigen einer Zeichenkette
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl
       * Rueckgabewert:
       *   HL: Zeiger auf die erzeugte Zeichenkette
       */
      buf.append( "F_S_STRING_I2_S:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_STRING_I2_S_1\n" );
      buf.append_LD_HL_xx( BasicLibrary.EMPTY_STRING_LABEL );
      buf.append( "\tRET\n"
		+ "F_S_STRING_I2_S_1:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tEXX\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "F_S_STRING_I2_S_2:\n"
		+ "\tCALL\tSTR_N_COPY\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,F_S_STRING_I2_S_3\n"
		+ "\tEXX\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tNZ,F_S_STRING_I2_S_2\n"
		+ "F_S_STRING_I2_S_3:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.STR_N_COPY );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_DATETIME_S ) ) {
      /*
       * Erzeugen einer Zeichenkette mit dem aktuellen Datum
       * und/oder der aktuellem Uhrzeit
       *
       * Parameter:
       *   HL: Zeiger auf den Formatstring
       * Rueckgabewert:
       *   HL: Zeiger auf die erzeugte Zeichenkette
       */
      buf.append( "F_S_DATETIME_S:\n" );
      if( compiler.getTarget().supportsXTIME() ) {
	buf.append( "\tPUSH\tHL\n"
		+ "\tCALL\tXDATETIME\n"
		+ "\tLD\tC,0FFH\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tNC,F_S_DATETIME_S_5\n"
		+ "\tLD\tHL,EMPTY_STRING\n"
		+ "\tRET\n"
		+ "F_S_DATETIME_S_1:\n"
		+ "\tLD\tC,00H\n"			// keine Vornullen
		+ "F_S_DATETIME_S_2:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,X_M_DATETIME\n"
		+ "\tLD\tB,D\n"
		+ "\tLD\tD,00H\n"
		+ "\tADD\tHL,DE\n"
		+ "F_S_DATETIME_S_3:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\tD,A\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,F_S_DATETIME_S_4\n"
		+ "\tLD\tA,30H\n"
		+ "\tADD\tA,D\n"
		+ "\tCALL\tF_S_DATETIME_S_12\n"
		+ "F_S_DATETIME_S_4:\n"
		+ "\tLD\tC,0FFH\n"			// Nullen ausgeben
		+ "\tDJNZ\tF_S_DATETIME_S_3\n"
		+ "\tPOP\tHL\n"
		+ "F_S_DATETIME_S_5:\n"
		+ "\tLD\tC,59H\n"			// Y
		+ "\tCALL\tF_S_DATETIME_S_11\n"		// Y?
		+ "\tJR\tNZ,F_S_DATETIME_S_6\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"		// YY?
		+ "\tLD\tDE,0103H\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_2\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"		// YYY?
		+ "\tLD\tDE,0202H\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_2\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"		// YYYY?
		+ "\tLD\tDE,0301H\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_2\n"
		+ "\tLD\tDE,0400H\n"
		+ "\tJR\tF_S_DATETIME_S_2\n"
		+ "F_S_DATETIME_S_6:\n"
		+ "\tLD\tC,4DH\n"			// M
		+ "\tLD\tDE,0204H\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_7\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tZ,F_S_DATETIME_S_2\n"		// Ausgabe MM
		+ "\tLD\tC,49H\n"			// I
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tLD\tE,0AH\n"
		+ "\tJR\tZ,F_S_DATETIME_S_2\n"		// Ausgabe MI
		+ "\tLD\tE,04H\n"
		+ "\tJR\tF_S_DATETIME_S_1\n"		// Ausgabe (M)M
		+ "F_S_DATETIME_S_7:\n"
		+ "\tLD\tC,44H\n"			// D
		+ "\tLD\tE,06H\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_9\n"
		+ "F_S_DATETIME_S_8:\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tNZ,F_S_DATETIME_S_1\n"		// ohne Vornullen
		+ "\tJR\tF_S_DATETIME_S_2\n"		// mit Vornullen
		+ "F_S_DATETIME_S_9:\n"
		+ "\tLD\tC,48H\n"			// H
		+ "\tLD\tE,08H\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tZ,F_S_DATETIME_S_8\n"
		+ "\tLD\tC,53H\n"			// S
		+ "\tLD\tE,0CH\n"
		+ "\tCALL\tF_S_DATETIME_S_11\n"
		+ "\tJR\tZ,F_S_DATETIME_S_8\n"
		+ "\tLD\tA,(HL)\n"			// kein Platzhalter
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_S_DATETIME_S_10\n"
		+ "\tCALL\tF_S_DATETIME_S_12\n"		// Zeichen kopieren
		+ "\tJR\tF_S_DATETIME_S_5\n"
		+ "F_S_DATETIME_S_10:\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n"
	// CP C,(HL) ohne Beachtung der Gross-/Kleinschreibung
		+ "F_S_DATETIME_S_11:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\tC\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
	// Schreiben des Zeichens in A inkl. Ueberlaufpruefung
		+ "F_S_DATETIME_S_12:\n"
		+ "\tEXX\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tC,F_S_DATETIME_S_13\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tF_S_DATETIME_S_14\n"
		+ "F_S_DATETIME_S_13:\n"
		+ "\tINC\tC\n"
		+ "F_S_DATETIME_S_14:\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
	compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
	compiler.addLibItem( BasicLibrary.LibItem.XDATETIME );
	compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
      } else {
	buf.append( "\tLD\tHL,EMPTY_STRING\n"
		+ "\tRET\n" );
      }
      compiler.addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_S_UPPER ) ) {
      /*
       * Zeichenkette in Grossbuchstaben wandeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   HL: Zeiger auf die umgewandelte Zeichenkette
       */
      buf.append( "F_S_UPPER:\n"
		+ "\tLD\tDE,M_TMP_STR_BUF-1\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "F_S_UPPER_1:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_S_UPPER_2\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,F_S_UPPER_1\n"
		+ "\tLD\t(DE),A\n"
		+ "F_S_UPPER_2:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
      compiler.addLibItem( BasicLibrary.LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_INSTR_N ) ) {
      /*
       * Laenge einer Zeichenkette ermitteln
       *
       * Parameter:
       *   BC: Anfangsposition in der durchsuchten Zeichenkette
       *   DE: Zeiger auf die gesuchte Zeichenkette
       *   HL: Zeiger auf die zu durchsuchende Zeichenkette
       * Rueckgabe:
       *   HL: Position, in der die erste Zeichenkette
       *       in der zweiten gefunden wurde bzw. 0,
       *       wenn nicht gefunden wurde
       */
      buf.append( "F_I2_INSTR_N:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,E_INVALID_PARAM\n"
		+ "\tPUSH\tBC\n"
		+ "F_I2_INSTR_N_1:\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,F_I2_INSTR_N_2\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_I2_INSTR_N_1\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tF_I2_INSTR_5\n"
		+ "F_I2_INSTR_N_2:\n"
		+ "\tPOP\tBC\n"
		+ "\tDEC\tBC\n"
		+ "\tJR\tF_I2_INSTR_1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_INSTR );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_INSTR ) ) {
      /*
       * Laenge einer Zeichenkette ermitteln
       *
       * Parameter:
       *   DE: Zeiger auf die gesuchte Zeichenkette
       *   HL: Zeiger auf die zu durchsuchende Zeichenkette
       * Rueckgabe:
       *   HL: Position, in der die erste Zeichenkette
       *       in der zweiten gefunden wurde bzw. 0,
       *       wenn nicht gefunden wurde
       */
      buf.append( "F_I2_INSTR:\n"
		+ "\tLD\tBC,0000H\n"
		+ "F_I2_INSTR_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_I2_INSTR_5\n"
		+ "\tDEC\tHL\n"
		+ "F_I2_INSTR_2:\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "F_I2_INSTR_3:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_I2_INSTR_4\n"	// gefunden
		+ "\tCP\t(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tZ,F_I2_INSTR_3\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_I2_INSTR_2\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_I2_INSTR_4:\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tRET\n"
		+ "F_I2_INSTR_5:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_LEN ) ) {
      /*
       * Laenge einer Zeichenkette ermitteln
       *
       * Parameter:
       *   HL: Zeiger auf Zeichenkette
       * Rueckgabe:
       *   HL: Laenge der Zeichenkette
       */

      buf.append( "F_I2_LEN:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tDEC\tHL\n"
		+ "F_I2_LEN_1:\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t(HL)\n"
		+ "\tJR\tNZ,F_I2_LEN_1\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_STR_CMP ) ) {
      /*
       * Vergleich zweier Zeichenketten
       *
       * Parameter:
       *   DE: Zeiger auf die linke Zeichenkette
       *   HL: Zeiger auf die rechte Zeichenkette
       * Rueckgabewerte:
       *   Z=1:  Zeichenketten sind gleich
       *   CY=1: linke Zeichenkette (DE) ist kleiner
       */
      buf.append( "I2_STR_CMP:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t(HL)\n"
		+ "\tRET\tNZ\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tI2_STR_CMP\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.STR_N_COPY ) ) {
      /*
       * Kopieren einer Zeichenkette mit Laengenbegrenzung
       *
       * Parameter:
       *   BC: max. Anzahl der Zeichen (Groesse des Zielpuffers - 1)
       *   DE: Zeiger auf den Zielpuffer
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   DE: Zeiger auf das Ende der kopierten Zeichenkette
       *       (terminierendes Nullbyte) im Zielpuffer
       *   BC: Restgroesse des Zielpuffers - 1
       */
      buf.append( "STR_N_COPY:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,STR_N_COPY_2\n"
		+ "\tDEC\tDE\n"
		+ "STR_N_COPY_1:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,STR_N_COPY_1\n"
		+ "\tINC\tDE\n"
		+ "STR_N_COPY_2:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n" );
    }
  }


	/* --- Konstruktor --- */

  private StringLibrary()
  {
    // nicht intstanziierbar
  }
}
