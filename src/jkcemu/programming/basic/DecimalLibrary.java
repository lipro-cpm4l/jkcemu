/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Bibliothek fuer Funktionen mit dem Datentyp DECIMAL
 */

package jkcemu.programming.basic;

import java.util.Set;


public class DecimalLibrary
{
  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();

    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_ABS_D6 ) ) {
      /*
       * Betrag eines Dec6-Wertes ermitteln
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   M_ACCU: nicht negativer Dec6-Wert
       */
      buf.append( "F_D6_ABS_D6:\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t7H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 ) ) {
      /*
       * Umwandlung eines Int4-Wertes in einen Dec6-Wert.
       *
       * Parameter:
       *   DEHL:  Int4-Wert
       * Rueckgabewert:
       *   M_ACCU: Dec6-Wert
       */
      buf.append( "F_D6_CDEC_I4:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tD6_CLEAR_ACCU\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tAND\t80H\n"		// nur Vorzeichen
		+ "\tJP\tP,F_D6_CDEC_I4_1\n"
		+ "\tCALL\tI4_NEG_DEHL_RAW\n"
		+ "\tJR\tC,F_D6_CDEC_I4_4\n"
		+ "\tCALL\tF_D6_CDEC_I4_1\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tSET\t7,(HL)\n"
		+ "\tRET\n"
		+ "F_D6_CDEC_I4_1:\n"
		+ "\tPUSH\tIY\n"
		+ "\tPUSH\tDE\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"			// obere 16 Bit in HL'
		+ "\tEXX\n"
		+ "\tPUSH\tHL\n"
		+ "\tPOP\tIY\n"			// untere 16 Bit in IY
		+ "\tLD\tC,20H\n"		// C=32
		+ "F_D6_CDEC_I4_2:\n"
		+ "\tADD\tIY,IY\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"		// oberstes Bit -> CY
		+ "\tEXX\n"
      // (M_ACCU * 2) + CY
		+ "\tLD\tHL,M_ACCU+5\n"
		+ "\tLD\tB,06H\n"
		+ "F_D6_CDEC_I4_3:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tADC\tA,A\n"
		+ "\tDAA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDEC\tHL\n"
		+ "\tDJNZ\tF_D6_CDEC_I4_3\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,F_D6_CDEC_I4_2\n"
		+ "\tPOP\tIY\n"
		+ "\tRET\n"
		+ "F_D6_CDEC_I4_4:\n"		// -2147483648
		+ "\tCALL\tD6_LD_ACCU_NNNNNN\n"
		+ "\tDB\t80H,21H,47H,48H,36H,48H\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I4_NEG_DEHL_RAW );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_NNNNNN );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 ) ) {
      /*
       * Umwandlung eines Int2-Wertes in einen Dec6-Wert.
       *
       * Parameter:
       *   HL:     Int2-Wert
       * Rueckgabewert:
       *   M_ACCU: Dec6-Wert
       */
      buf.append( "F_D6_CDEC_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t80H\n"		// nur Vorzeichen
		+ "\tLD\t(M_ACCU),A\n"		// 1. Accu-Byte
		+ "\tJP\tP,F_D6_CDEC_I2_1\n"
		+ "\tCALL\tI2_NEG_HL_RAW\n"
		+ "\tJR\tC,F_D6_CDEC_I2_3\n"
		+ "F_D6_CDEC_I2_1:\n"
		+ "\tLD\tBC,1000H\n"		// B=16, C=0
		+ "\tLD\tD,C\n"			// D=0
		+ "\tLD\tE,C\n"			// E=0
		+ "\tLD\t(M_ACCU+1),DE\n"	// 2. und 3. Accu-Byte
		+ "F_D6_CDEC_I2_2:\n"
		+ "\tADD\tHL,HL\n"		// oberstes Bit -> CY
      /*
       * BCD-Multiplikation mit 2 mit Addition von CY
       *  D: Einer und Zehner
       *  E: Hunderter und Tausender
       *  C: Zehntausender
       */
		+ "\tLD\tA,D\n"
		+ "\tADC\tA,A\n"
		+ "\tDAA\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,E\n"
		+ "\tADC\tA,A\n"
		+ "\tDAA\n"
		+ "\tLD\tE,A\n"
      // C=(C*2)+CY, da max. Wert 3, ist DAA nicht notwendig
		+ "\tRL\tC\n"
		+ "\tDJNZ\tF_D6_CDEC_I2_2\n"
		+ "\tLD\tA,C\n"			// Zehntausender
		+ "\tLD\t(M_ACCU+3),A\n"	// 4. Accu-Byte
		+ "\tLD\t(M_ACCU+4),DE\n"	// 5. und 6. Accu-Byte
		+ "\tRET\n"
		+ "F_D6_CDEC_I2_3:\n"		// -32768
		+ "\tCALL\tD6_LD_ACCU_NNNNNN\n"
		+ "\tDB\t80H,00H,00H,03H,27H,68H\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I2_NEG_HL_RAW );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_NNNNNN );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_CINT_D6 ) ) {
      /*
       * Umwandlung eines Dec6-Wertes in einen Int2-Wert
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   HL:     Int2-Wert
       */
      buf.append( "F_I2_CINT_D6:\n" );
      if( compiler.usesLibItem( BasicLibrary.LibItem.F_I4_CLNG_D6 ) ) {
	buf.append( "\tCALL\tF_I4_CLNG_D6\n"
		+ "\tCALL\tCHECK_DEHL_FOR_I2\n"
		+ "\tRET\tNC\n"
                + "\tJP\tE_NUMERIC_OVERFLOW\n" );
	compiler.addLibItem( BasicLibrary.LibItem.CHECK_DEHL_FOR_I2 );
	compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
      } else {
	buf.append( "\tEXX\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tP,F_I2_CINT_D6_1\n"
		+ "\tCALL\tF_I2_CINT_D6_1\n"
		+ "\tJP\tI2_NEG_HL_RAW\n"
		+ "F_I2_CINT_D6_1:\n"
		+ "\tAND\t070H\n"
		+ "\tSUB\t0B0H\n"
		+ "\tCPL\n"
		+ "\tINC\tA\n"
		+ "\tLD\tC,A\n"		// Anzahl der zu lesenden Stellen
		+ "\tLD\tD,00H\n"
		+ "F_I2_CINT_D6_2:\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tLD\tB,06H\n"
		+ "F_I2_CINT_D6_3:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tF_I2_CINT_D6_3\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tEXX\n"
		+ "\tCALL\tI2_APPEND_DIGIT_TO_HL\n"
                + "\tJP\tC,E_NUMERIC_OVERFLOW\n"
		+ "\tEXX\n"
		+ "\tLD\tA,C\n"
		+ "\tSUB\t10H\n"
		+ "\tJR\tNZ,F_I2_CINT_D6_2\n"
		+ "\tRET\n" );
	compiler.addLibItem( BasicLibrary.LibItem.I2_APPEND_DIGIT_TO_HL );
	compiler.addLibItem( BasicLibrary.LibItem.I2_NEG_HL_RAW );
	compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
      }
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I4_CLNG_D6 ) ) {
      /*
       * Umwandlung eines Dec6-Wertes in einen Int4-Wert
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   DEHL:  Int4-Wert
       */
      buf.append( "F_I4_CLNG_D6:\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tLD\tH,D\n"
		+ "\tLD\tL,D\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tP,F_I4_CLNG_D6_1\n"
		+ "\tCALL\tF_I4_CLNG_D6_1\n"
		+ "\tJP\tI4_NEG_DEHL_RAW\n"
		+ "F_I4_CLNG_D6_1:\n"
		+ "\tAND\t070H\n"
		+ "\tSUB\t0B0H\n"
		+ "\tCPL\n"
		+ "\tINC\tA\n"
		+ "\tLD\tC,A\n"		// Anzahl der zu lesenden Stellen
		+ "\tLD\tD,00H\n"
		+ "F_I4_CLNG_D6_2:\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tLD\tB,06H\n"
		+ "F_I4_CLNG_D6_3:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tF_I4_CLNG_D6_3\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tEXX\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tI4_DEHL_MUL4\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,L\n"
		+ "\tADD\tC\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tADC\tB\n"
		+ "\tLD\tH,A\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,E\n"
		+ "\tADD\tC\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,D\n"
		+ "\tADC\tB\n"
		+ "\tLD\tD,A\n"
		+ "\tCALL\tI4_DEHL_MUL2\n"
                + "\tJP\tM,E_NUMERIC_OVERFLOW\n"
		+ "\tEXX\n"
		+ "\tLD\tA,C\n"
		+ "\tSUB\t10H\n"
		+ "\tJR\tNZ,F_I4_CLNG_D6_2\n"
		+ "\tEXX\n"
		+ "\tRET\n"
		+ "I4_DEHL_MUL4:\n"
		+ "\tCALL\tI4_DEHL_MUL2\n"
		+ "I4_DEHL_MUL2:\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I4_NEG_DEHL_RAW );
      compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_FRAC_D6 ) ) {
      /*
       * Ermitteln des Nachkommaanteils eines Dec6-Wertes
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   M_ACCU: Dec6-Wert ohne Vorkommaanteil
       */
      buf.append( "F_D6_FRAC_D6:\n"
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\t(HL),70H\n"	// neu: 7 Nachkommastellen
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"	// 2. Byte Null
		+ "\tSUB\t07H\n"
		+ "\tNEG\n"		// wie oft schieben
		+ "\tLD\tC,A\n"
		+ "\tJR\tZ,F_D6_FRAC_D6_3\n"
		+ "F_D6_FRAC_D6_1:\n"
      // nur die letzten 4 Bytes mit den Nachkommastellen schieben
		+ "\tLD\tHL,M_ACCU+5\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,04H\n"
		+ "F_D6_FRAC_D6_2:\n"
		+ "\tRLD\n"
		+ "\tDEC\tHL\n"
		+ "\tDJNZ\tF_D6_FRAC_D6_2\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,F_D6_FRAC_D6_1\n"
		+ "F_D6_FRAC_D6_3:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_SCALE_D6 ) ) {
      /*
       * Ermitteln der Anzahl der dezimalen Nachkommastellen
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   HL:     Anzahl Nachkommastellen
       */
      buf.append( "F_I2_SCALE_D6:\n"
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tC,A\n"
		+ "F_I2_SCALE_D6_1:\n"
      /*
       * Da nur max. 7 Nachkommastellen moeglich sind,
       * reicht es hier, nur die letzten 4 Bytes zu schieben.
       */
		+ "\tXOR\tA\n"
		+ "\tLD\tB,04H\n"
		+ "\tLD\tHL,M_ACCU+2\n"
		+ "F_I2_SCALE_D6_2:\n"
		+ "\tRRD\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tF_I2_SCALE_D6_2\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tNZ,F_I2_SCALE_D6_3\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,F_I2_SCALE_D6_1\n"
		+ "F_I2_SCALE_D6_3:\n"
		+ "\tLD\tL,C\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_I2_SGN_D6 ) ) {
      /*
       * Ermitteln des Vorzeichens eines Dec6-Wertes
       *
       * Parameter:
       *   M_ACCU: Dec6-Wert
       * Rueckgabewert:
       *   HL:     -1: Dec6-Wert war kleiner Null
       *            0: Dec6-Wert war Null
       *            1: Dec6-Wert war groesser Null
       */
      buf.append( "F_I2_SGN_D6:\n"
		+ "\tCALL\tD6_IS_ACCU_ZERO\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tJP\tF_I2_SGN_I2_2\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_IS_ACCU_ZERO );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_SGN_I2 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_TRUNC_D6_I2 ) ) {
      /*
       * Abschneiden einer Dec6-Zahl
       *
       * Parameter:
       *   M_ACCU: abzuschneidender Wert
       *   HL:     Anzahl der Nachkommastellen,
       *           hinter der abgeschnitten werden soll (>= 0)
       * Rueckgabe:
       *   M_ACCU: abgeschnittener Wert
       */
      buf.append( "F_D6_TRUNC_D6_I2:\n"
		+ "\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tJR\tD6_TRUNC_UTIL\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_TRUNC_UTIL );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_TRUNC_D6 ) ) {
      /*
       * Abschneiden einer Dec6-Zahl hinter dem Komma
       *
       * Parameter:
       *   M_ACCU: abzuschneidender Wert
       * Rueckgabe:
       *   M_ACCU: abgeschnittener Wert
       */
      buf.append( "F_D6_TRUNC_D6:\n"
		+ "\tLD\tHL,0000H\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_TRUNC_UTIL );
      // direkt weiter mit D6_TRUNC_UTIL
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_TRUNC_UTIL ) ) {
      /*
       * Abschneiden einer Dec6-Zahl
       *
       * Parameter:
       *   M_ACCU: abzuschneidender Wert
       *   HL:     Anzahl der Nachkommastellen,
       *           hinter der abgeschnitten werden soll (>= 0)
       * Rueckgabe:
       *   M_ACCU: abgeschnittener Wert
       */
      buf.append( "D6_TRUNC_UTIL:\n" );
      if( compiler.usesLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL ) ) {
	buf.append( "\tLD\tA,0FFH\n"
		+ "\tJR\tD6_ROUND_UTIL_HL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL );
	compiler.addLibItem( BasicLibrary.LibItem.M_MODE_SCALE );
      } else {
	buf.append( "\tEX\tDE,HL\n"
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,A\n"
		+ "\tAND\t70H\n"
		+ "\tEX\tAF,AF\'\n"	// orig. oberstes Byte in AF'
		+ "\tLD\tL,C\n"		// akt. Anzahl Nachkommastellen
		+ "\tLD\tH,00H\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tSBC\tHL,DE\n"	// Anz. zu verschiebende Stellen
		+ "\tRET\tZ\n"
		+ "\tRET\tM\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,D6_CLEAR_ACCU\n"
		+ "\tLD\tA,0AH\n"
		+ "\tCP\tL\n"
		+ "\tJP\tC,D6_CLEAR_ACCU\n"
		+ "\tLD\tE,L\n"		// Anz. Stellen zu verschieben
		+ "\tLD\tD,B\n"		// Vorzeichen merken
	// oberstes Nibble leeren
		+ "\tLD\tA,B\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(M_ACCU),A\n"
	// Byte hinter Accu leeren
		+ "\tXOR\tA\n"
		+ "\tLD\t(M_ACCU+6),A\n"
		+ "D6_TRUNC_UTIL_1:\n"
	// Accu um D Stellen nach rechts schieben
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tB,06H\n"
		+ "\tXOR\tA\n"
		+ "D6_TRUNC_UTIL_2:\n"
		+ "\tRRD\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_TRUNC_UTIL_2\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tSUB\t10H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tDEC\tE\n"
		+ "\tJR\tNZ,D6_TRUNC_UTIL_1\n"
	// ggf. zurueckschieben
		+ "\tEX\tAF,AF\'\n"
		+ "D6_TRUNC_UTIL_3:\n"
		+ "\tBIT\t7,A\n"
		+ "\tJR\tZ,F_D6_TRUNC_D6_I2_5\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tLD\tB,06H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tXOR\tA\n"
		+ "D6_TRUNC_UTIL_4:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_TRUNC_UTIL_4\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tADD\tA,10H\n"
		+ "\tJR\tD6_TRUNC_UTIL_3\n"
		+ "F_D6_TRUNC_D6_I2_5:\n"
	// oberstes Nibble schreiben
		+ "\tLD\tB,A\n"			// Anz. Nachkommastellen
		+ "\tLD\tA,D\n"			// Vorzeichen
		+ "\tAND\t80H\n"
		+ "\tOR\tB\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
	compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
	compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      }
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6 ) ) {
      /*
       * Runden einer Dec6-Zahl auf eine ganze Zahl
       *
       * Parameter:
       *   M_ACCU: zu rundender Wert
       * Rueckgabe:
       *   M_ACCU: gerundeter Wert
       */
      buf.append( "F_D6_ROUND_D6:\n"
		+ "\tXOR\tA\n"
		+ "\tJR\tD6_ROUND_UTIL_00\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_00 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6_I2 ) ) {
      /*
       * Runden einer Dec6-Zahl
       *
       * Parameter:
       *   M_ACCU: zu rundender Wert
       *   HL:     Rundungsmodus:
       *             0: Betrag abrunden (zur Null hin)
       *             1: auf gerade Zahl runden
       *             2: Betrag aufrunden (von der Null weg)
       * Rueckgabe:
       *   M_ACCU: gerundeter Wert
       */
      buf.append( "F_D6_ROUND_D6_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t03H\n"
		+ "\tJP\tNC,E_INVALID_PARAM\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tD6_ROUND_UTIL_00\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_00 );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6_I2_I2 ) ) {
      /*
       * Runden einer Dec6-Zahl
       *
       * Parameter:
       *   M_ACCU: zu rundender Wert
       *   DE:     Rundungsmodus:
       *             0: Betrag abrunden (zur Null hin)
       *             1: auf gerade Zahl runden
       *             2: Betrag aufrunden (von der Null weg)
       *   HL:     Anzahl der Nachkommastellen,
       *           auf die gerundet werden soll,
       *           kann auch kleiner 0 sein
       * Rueckgabe:
       *   M_ACCU: gerundeter Wert
       */
      buf.append( "F_D6_ROUND_D6_I2_I2:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t03H\n"
		+ "\tJP\tNC,E_INVALID_PARAM\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tD6_ROUND_UTIL_HL\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_00 ) ) {
      /*
       * Runden einer Dec6-Zahl auf eine ganze Zahl
       *
       * Parameter:
       *   M_ACCU: zu rundender Wert
       *   A:      Rundungsmodus
       *             -1: Betrag abrunden (zur Null hin)
       *              0: auf gerade Zahl runden
       *              1: Betrag aufrunden (von der Null weg)
       * Rueckgabe:
       *   M_ACCU: gerundeter Wert
       */
      buf.append( "D6_ROUND_UTIL_00:\n"
		+ "\tLD\tHL,0000H\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL );
      // direkt weiter mit D6_ROUND_UTIL_HL
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL ) ) {
      /*
       * Runden einer Dec6-Zahl
       *
       * Parameter:
       *   M_ACCU: zu rundender Wert
       *   A:      Rundungsmodus
       *             -1: Betrag abrunden (zur Null hin)
       *              0: auf gerade Zahl runden
       *              1: Betrag aufrunden (von der Null weg)
       *   HL:     Anzahl der Nachkommastellen,
       *           auf die gerundet werden soll,
       *           kann auch kleiner 0 sein
       * Rueckgabe:
       *   M_ACCU: gerundeter Wert
       */
      buf.append( "D6_ROUND_UTIL_HL:\n"
		+ "\tLD\t(M_MODE_SCALE),A\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,A\n"
		+ "\tAND\t70H\n"	// Bit 4-7: Anz. Nachkommastellen
		+ "\tEX\tAF,AF\'\n"	// in AF' merken
		+ "\tLD\tL,C\n"		// akt. Anzahl Nachkommastellen
		+ "\tLD\tH,00H\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tSBC\tHL,DE\n"	// Anz. zu verschiebende Stellen
		+ "\tRET\tZ\n"
		+ "\tRET\tM\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,D6_CLEAR_ACCU\n"
		+ "\tLD\tA,0AH\n"
		+ "\tCP\tL\n"
		+ "\tJP\tC,D6_CLEAR_ACCU\n"
		+ "\tLD\tE,L\n"		// Anz. Stellen zu verschieben
		+ "\tLD\tD,B\n"		// Vorzeichen merken
      // oberstes Nibble leeren
		+ "\tLD\tA,B\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(M_ACCU),A\n"
      // Byte hinter Accu leeren
		+ "\tXOR\tA\n"
		+ "\tLD\t(M_ACCU+6),A\n"
		+ "D6_ROUND_UTIL_1:\n"
      // Accu um D Stellen nach rechts schieben
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tB,06H\n"
		+ "\tXOR\tA\n"
		+ "D6_ROUND_UTIL_2:\n"
		+ "\tRRD\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_ROUND_UTIL_2\n"
      /*
       * letzte herausgeschobene Ziffer OR-verknupefen,
       * um bei einer 5 in der vorletzten Stelle feststellen zu koennen,
       * ob es dahinter noch weitere Stellen gab
       */
		+ "\tLD\tB,(HL)\n"
		+ "\tRRD\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
      // Anzahl Nachkommastellen anpassen
		+ "\tEX\tAF,AF\'\n"
		+ "\tSUB\t10H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tDEC\tE\n"
		+ "\tJR\tNZ,D6_ROUND_UTIL_1\n"
      // Betrag auf- oder abrunden?
		+ "\tLD\tA,(M_MODE_SCALE)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,D6_ROUND_UTIL_5\n"	// Betrag abrunden
		+ "\tJR\tNZ,D6_ROUND_UTIL_3\n"	// Betrag aufrunden
      // Gerade-Zahl-Regel
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t50H\n"
		+ "\tJR\tZ,D6_ROUND_UTIL_9\n"	// in der Mitte
		+ "\tJR\tC,D6_ROUND_UTIL_5\n"	// Betrag abrunden
      /*
       * Betrag aufrunden,
       * Wenn es keine weiteren Nachkommastellen gab,
       * dann entspricht der Wert bereits dem gerundeten
       * und muss nicht mehr aufgerundet werden.
       */
		+ "D6_ROUND_UTIL_3:\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tXOR\tA\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,D6_ROUND_UTIL_5\n"
		+ "\tLD\tB,06H\n"
		+ "\tSCF\n"			// 1 addieren
		+ "D6_ROUND_UTIL_4:\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tADC\tA,00H\n"
		+ "\tDAA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDJNZ\tD6_ROUND_UTIL_4\n"
		+ "D6_ROUND_UTIL_5:\n"
      /*
       * bei A7=1 (Ueberlauf) solange zurueckschieben,
       * bis A7 wiedder 0 ist
       */
		+ "\tEX\tAF,AF\'\n"
		+ "D6_ROUND_UTIL_6:\n"
		+ "\tBIT\t7,A\n"
		+ "\tJR\tZ,D6_ROUND_UTIL_8\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tLD\tB,06H\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tXOR\tA\n"
		+ "D6_ROUND_UTIL_7:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_ROUND_UTIL_7\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tADD\tA,10H\n"
		+ "\tJR\tD6_ROUND_UTIL_6\n"
		+ "D6_ROUND_UTIL_8:\n"
      // oberstes Nibble schreiben
		+ "\tLD\tB,A\n"			// Anz. Nachkommastellen
		+ "\tLD\tA,D\n"			// Vorzeichen
		+ "\tAND\t80H\n"
		+ "\tOR\tB\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
      // auf gerade Zahl runden
		+ "D6_ROUND_UTIL_9:\n"
		+ "\tDEC\tHL\n"
		+ "\tBIT\t0,(HL)\n"
		+ "\tJR\tNZ,D6_ROUND_UTIL_3\n"	// ungerade -> aufrunden
		+ "\tJR\tD6_ROUND_UTIL_5\n" );	// gerade -> abrunden
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_7 );
      compiler.addLibItem( BasicLibrary.LibItem.M_MODE_SCALE );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.F_D6_VAL_S ) ) {
      /*
       * Lesen einer Dec6-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   M_ACCU: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_D6_VAL_S:\n"
		+ "\tDEC\tHL\n" 
		+ "F_D6_VAL_S_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_D6_VAL_S_1\n"	// Leerzeichen uebergehen
		+ "\tCP\t2BH\n"			// Plus
		+ "\tJR\tZ,F_D6_VAL_S_2\n"
		+ "\tCP\t2DH\n"			// Minus
		+ "\tJR\tNZ,F_D6_VAL_S_2\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tF_D6_VAL_S_2\n"
		+ "\tRET\tC\n"
		+ "\tJP\tD6_NEG_ACCU\n"
		+ "F_D6_VAL_S_2:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tD6_CLEAR_ACCU\n"
		+ "\tLD\tBC,0000H\n"
      /*
       * DE:       Zeiger auf Zeichenkette
       * B0-B2:    Anzahl der Nachkommastellen
       * B7=1:     Flag fuer gueltige Zahl
       * C:        Anzahl der gesamten Stellen
       * (M_ACCU): Dezimalzahl
       */
		+ "F_D6_VAL_S_3:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t30H\n"
		+ "\tJR\tNZ,F_D6_VAL_S_5\n"
		+ "\tSET\t7,B\n"		// gueltige Zahl
		+ "\tJR\tF_D6_VAL_S_3\n"	// Vornull uebergehen
		+ "F_D6_VAL_S_4:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "F_D6_VAL_S_5:\n"
		+ "\tCP\t2EH\n"			// Dezimalpunkt
		+ "\tJR\tZ,F_D6_VAL_S_6\n"
		+ "\tCALL\tF_D6_VAL_S_13\n"	// num. Wert ermitteln
		+ "\tJR\tC,F_D6_VAL_S_8\n"	// keine Ziffer -> Ende
		+ "\tCALL\tF_D6_VAL_S_14\n"	// Ziffer anhaengen
		+ "\tJR\tNC,F_D6_VAL_S_4\n" );
      BasicUtil.appendSetErrorNumericOverflow( compiler );
      buf.append( "\tJR\tF_D6_VAL_S_10\n"
		+ "F_D6_VAL_S_6:\n"		// Nachkommastellen lesen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCALL\tF_D6_VAL_S_13\n"	// num. Wert ermitteln
		+ "\tJR\tC,F_D6_VAL_S_8\n"	// keine Ziffer -> Ende
      // Nachkommaziffer aufgrund zu vieler Stellen ignorieren?
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t07H\n"
		+ "\tCP\t07H\n"
		+ "\tLD\tA,L\n"
		+ "\tJR\tNZ,F_D6_VAL_S_7\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_D6_VAL_S_6\n"	// 0 abgeschnitten
		+ "\tCALL\tD6_SET_DIGITS_TRUNCATED\n"
		+ "\tJR\tF_D6_VAL_S_6\n"
		+ "F_D6_VAL_S_7:\n"
		+ "\tCALL\tF_D6_VAL_S_14\n"	// Ziffer anhaengen
		+ "\tINC\tB\n"			// Nachkommastellen++
		+ "\tJR\tF_D6_VAL_S_6\n"
		+ "F_D6_VAL_S_8:\n"		// keine Ziffer mehr
		+ "\tBIT\t7,B\n"		// gueltige Zahl?
		+ "\tJR\tNZ,F_D6_VAL_S_11\n"	// ja -> Ende
		+ "F_D6_VAL_S_9:\n" );
      BasicUtil.appendSetErrorInvalidChars( compiler );
      buf.append( "F_D6_VAL_S_10:\n"
		+ "\tCALL\tD6_CLEAR_ACCU\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "F_D6_VAL_S_11:\n"		// Ende Zeichenkette pruefen
		+ "\tOR\tA\n"			// Ende Zeichenkette?
		+ "\tJR\tZ,F_D6_VAL_S_12\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,F_D6_VAL_S_9\n"	// kein Leerzeichen -> Fehler
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tF_D6_VAL_S_11\n"
		+ "F_D6_VAL_S_12:\n"
      // Anzahl Nachhkommastellen eintragen
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,B\n"
		+ "\tRLA\n"
		+ "\tRLA\n"
		+ "\tRLA\n"
		+ "\tRLA\n"
		+ "\tAND\t70H\n"
		+ "\tOR\t(HL)\n"		// CY=0
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
      /*
       * numerischer Wert der Zeichen in A zurueckliefern
       * CY=1: keine Ziffer, A unveraendert
       */
		+ "F_D6_VAL_S_13:\n"
		+ "\tCP\t30H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t3AH\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tSUB\t30H\n"
		+ "\tRET\n"
      // Ziffer in A an M_ACCU anhaengen, CY=1: Ueberlauf
		+ "F_D6_VAL_S_14:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,0AH\n"		// max. Stellenanzahl
		+ "\tCP\tC\n"			// bereits erreicht?
		+ "\tRET\tC\n"			// Ueberlauf
		+ "\tINC\tC\n"			// Gesamtstellen++
		+ "\tSET\t7,B\n"		// gueltige Zahl
		+ "\tLD\tA,L\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tD6_APPEND_DIGIT_TO_ACCU\n"
		+ "\tPOP\tBC\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_NEG_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_APPEND_DIGIT_TO_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_SET_DIGITS_TRUNCATED );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.S_STR_D6MEM ) ) {
      /*
       * Umwandlung Dec6-Wertes
       * in eine Zeichenkette mit einer Dezimalzahl.
       * Das erste Zeichen enthaelt entweder das Vorzeichen
       * oder ein Leerzeichen.
       *
       * Parameter:
       *   HL: Zeiger auf den numerischen Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_STR_D6MEM:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tD6_IS_ZERO\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tZ,S_STR_D6_13\n"
		+ "\tJR\tS_STR_D6_1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_IS_ACCU_ZERO );
      compiler.addLibItem( BasicLibrary.LibItem.S_STR_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.S_STR_D6 ) ) {
      /*
       * Umwandlung Dec6-Wertes
       * in eine Zeichenkette mit einer Dezimalzahl.
       * Das erste Zeichen enthaelt entweder das Vorzeichen
       * oder ein Leerzeichen.
       *
       * Parameter:
       *   M_ACCU: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_STR_D6:\n"
		+ "\tCALL\tD6_IS_ACCU_ZERO\n"
		+ "\tJR\tZ,S_STR_D6_13\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "S_STR_D6_1:\n"
		+ "\tLD\tA,20H\n"		// Leerzeichen
		+ "\tBIT\t7,(HL)\n"
		+ "\tJR\tZ,S_STR_D6_2\n"
		+ "\tLD\tA,2DH\n"		// Minuszeichen
		+ "S_STR_D6_2:\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_CVTBUF\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tAND\t07H\n"
		+ "\tSUB\t0BH\n"		// max. Stellenanzahl
		+ "\tCPL\n"
		+ "\tINC\tA\n"
		+ "\tLD\tC,A\n"			// Anzahl Vorkommastellen
		+ "\tLD\tD,00H\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCALL\tS_STR_D6_8\n"
		+ "\tLD\tB,05H\n"
		+ "S_STR_D6_3:\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tS_STR_D6_7\n"
		+ "\tDJNZ\tS_STR_D6_3\n"
		+ "\tBIT\t1,D\n"		// Komma ausgegeben?
		+ "\tEXX\n"
		+ "\tLD\t(HL),00H\n"		// String terminieren
      // gff. Nachkomma-Nullen entfernen
		+ "\tJR\tZ,S_STR_D6_6\n"	// kein Komma -> Ende
		+ "S_STR_D6_4:\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t2EH\n"
		+ "\tJR\tZ,S_STR_D6_5\n"
		+ "\tCP\t30H\n"			// 0?
		+ "\tJR\tNZ,S_STR_D6_6\n"	// nein -> Ende
		+ "\tLD\t(HL),00H\n"
		+ "\tJR\tS_STR_D6_4\n"
		+ "S_STR_D6_5:\n"
		+ "\tLD\t(HL),00H\n"		// Dezimalpunkt abschneiden
		+ "S_STR_D6_6\n"
		+ "\tLD\tHL,M_CVTBUF\n"
		+ "\tRET\n"
      // (HL) mit zwei Ziffern ausgeben
		+ "S_STR_D6_7:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tCALL\tS_STR_D6_8\n"
		+ "\tLD\tA,(HL)\n"
      // A mit einer Ziffer ausgeben
		+ "S_STR_D6_8:\n"
		+ "\tINC\tC\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,S_STR_D6_10\n"
		+ "\tBIT\t0,D\n"		// Ziffern ausgegeben?
		+ "\tEXX\n"
		+ "\tJR\tNZ,S_STR_D6_9\n"
		+ "\tLD\t(HL),30H\n"		// nein -> Vorkomma-Null
		+ "\tINC\tHL\n"
		+ "S_STR_D6_9:\n"
		+ "\tLD\t(HL),2EH\n"		// Dezimalpunkt
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tD,03H\n"		// Ziffer+Dezimalpunkt
		+ "S_STR_D6_10:\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tNZ,S_STR_D6_11\n"
		+ "\tBIT\t0,D\n"		// Null unterdruecken?
		+ "\tJR\tZ,S_STR_D6_12\n"
		+ "S_STR_D6_11:\n"
		+ "\tADD\tA,30H\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tSET\t0,D\n"		// Ziffer ausgegeben
		+ "S_STR_D6_12:\n"
		+ "\tDEC\tC\n"
		+ "\tRET\n"
		+ "S_STR_D6_13:\n"
		+ "\tLD\tHL,S_STR_D6_14\n"
		+ "\tRET\n"
		+ "S_STR_D6_14:\n"
		+ "\tDB\t20H,30H,00H\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_IS_ACCU_ZERO );
      compiler.addLibItem( BasicLibrary.LibItem.M_CVTBUF );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_MAX_D6_D6 ) ) {
      /*
       * Den groesseren von zwei Dec6-Werte ermitteln
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Operand 1 wenn dieser groesser Operand 2, sonst Operand 2
       */
      buf.append( "D6_MAX_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tC\n"
		+ "\tJR\tD6_LD_ACCU_OP1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_OP1 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_MIN_D6_D6 ) ) {
      /*
       * Den kleineren von zwei Dec6-Werte ermitteln
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Operand 1 wenn dieser kleiner Operand 2, sonst Operand 2
       */
      buf.append( "D6_MIN_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tNC\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_OP1 );
      // direkt weiter mit D6_LD_ACCU_OP1
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_ACCU_OP1 ) ) {
      /*
       * M_OP1 nach M_ACU kopieren
       *
       * Parameter:
       *   M_OP1:  Wert
       * Rueckgabe:
       *   M_ACCU: Wert
       */
      buf.append( "D6_LD_ACCU_OP1:\n"
		+ "\tLD\tBC,0006H\n"
		+ "\tLD\tDE,M_ACCU\n"
		+ "\tLD\tHL,M_OP1\n"
		+ "\tLDIR\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_ACCU_IYOFFS ) ) {
      /*
       * (IY+Offs) nach M_ACU kopieren
       *
       * Parameter:
       *   IY:     Basisadresse der Quelle
       *   (SP):   Offset (Byte hinter Return-Adresse)
       *   M_ACCU: Ziel
       */
      buf.append( "D6_LD_ACCU_IYOFFS:\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tE,A\n"
		+ "\tRLCA\n"			// CY: Vorzeichen
		+ "\tSBC\tA,A\n"
		+ "\tLD\tD,A\n"
		+ "\tPUSH\tIY\n"
		+ "\tPOP\tHL\n"
		+ "\tADD\tHL,DE\n"		// Quelladresse
		+ "\tLD\tDE,M_ACCU\n"		// Zieladresse
		+ "\tLD\tBC,0006H\n"
		+ "\tLDIR\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_EQ_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU == M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_EQ_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_GE_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU >= M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_GE_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tNC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_GT_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU > M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_GT_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_LE_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU <= M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_LE_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_LT_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU <= M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_LT_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tNC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.I2_NE_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU <> M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   HL=0FFFFH: wahr
       *   HL=0000H:  falsch
       */
      buf.append( "I2_NE_D6_D6:\n"
		+ "\tCALL\tD6_CMP_D6_D6\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_CMP_D6_D6 ) ) {
      /*
       * Dec6-Vergleich M_ACCU <> M_OP1
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   Z=1: M_OP1 == M_ACCU
       *   C=1: M_OP1 < M_ACCU
       */
      buf.append( "D6_CMP_D6_D6:\n"
      // beide Operanden Null?
		+ "\tCALL\tD6_IS_ACCU_ZERO\n"
		+ "\tJR\tNZ,D6_CMP_D6_D6_1\n"
		+ "\tLD\tHL,M_OP1\n"
		+ "\tCALL\tD6_IS_ZERO\n"
		+ "\tRET\tZ\n"			// Accu == Op1 == 0
		+ "D6_CMP_D6_D6_1:\n"
      // Vorzeichen vergleichen
		+ "\tLD\tDE,M_OP1\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tC,B\n"			// Vorzeichen merken
		+ "\tXOR\tB\n"
		+ "\tJP\tM,D6_CMP_D6_D6_4\n"	// Vorzeichen ungleich
		+ "\tEXX\n"
		+ "\tCALL\tD6_EQUALIZE_SCALES\n"
		+ "\tEXX\n"
      /*
       * Vorzeichen und Anzahl Nachkommastellen sind gleich
       * -> Die Bytes koennen direkt verglichen werden.
       */
		+ "\tLD\tB,06H\n"
		+ "D6_CMP_D6_D6_2:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t(HL)\n"
		+ "\tJR\tNZ,D6_CMP_D6_D6_3\n"	// ungleich: Z=0, CY relevant
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_CMP_D6_D6_2\n"
		+ "\tXOR\tA\n"			// Z=1, CY=0
		+ "\tRET\n"
		+ "D6_CMP_D6_D6_3:\n"
      // wenn negativ (C7=1), dann CY umkehren
		+ "\tRRA\n"			// CY -> A7
		+ "\tXOR\tC\n"			// bei C7=1 -> A7 negieren
		+ "\tOR\t7FH\n"			// Z=0
		+ "\tRLCA\n"			// A7 -> CY
		+ "\tRET\n"
      // Vorzeichen ungleich, A7=1 -> Z=0
		+ "D6_CMP_D6_D6_4:\n"
		+ "\tLD\tA,B\n"
		+ "\tRLCA\n"			// Vorzeichen Accu -> CY
		+ "\tCCF\n"			// CY umkehren
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_EQUALIZE_SCALES );
      compiler.addLibItem( BasicLibrary.LibItem.D6_IS_ACCU_ZERO );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_SUB_D6_D6 ) ) {
      /*
       * Dec6-Subtraktion M_ACCU = M_OP1 - M_ACCU
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Ergebnis
       */
      buf.append( "D6_SUB_D6_D6:\n"
		+ "\tCALL\tD6_NEG_ACCU\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_NEG_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_ADD_D6_D6 );
      // direkt weiter mit D6_ADD_D6_D6
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_ADD_D6_D6 ) ) {
      /*
       * Dec6-Addition M_ACCU = M_OP1 + M_ACCU
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Ergebnis
       */
      buf.append( "D6_ADD_D6_D6:\n"
      /*
       * Anzahl der Nachkommastellen (Scale)
       * muss bei beiden Operanden gleich sein.
       */
		+ "\tCALL\tD6_EQUALIZE_SCALES\n"
      // Vorzeichen vergleichen
		+ "\tLD\tA,(DE)\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tC,A\n"			// Vorzeichen+Scale
		+ "\tXOR\t(HL)\n"
		+ "\tJP\tM,D6_ADD_D6_D6_7\n"	// unterschiedliche Vorz.
      /*
       * Vorzeichen gleich -> Addition
       * Vorzeichen und Scale in C.
       *
       * Oberstes Nibble in beiden Operanden auf 0 setzen
       * zwecks Nutzung als Ueberlaufstelle
       */
		+ "\tCALL\tD6_CLEAR_UPPER_NIBBLES\n"
		+ "\tLD\tDE,M_OP1+6\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tXOR\tA\n"			// CY=0
		+ "\tLD\tB,06H\n"
		+ "D6_ADD_D6_D6_6:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tADC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDJNZ\tD6_ADD_D6_D6_6\n"
		+ "\tJR\tD6_ADD_D6_D6_11\n"
		+ "D6_ADD_D6_D6_7:\n"
      /*
       * Vorzeichen ungleich -> Subtraktion
       * Scale in C.
       * Operanden muessen noch in die richtige Reihenfolge gebracht werden.
       */
		+ "\tRES\t7,C\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,D6_ADD_D6_D6_8\n"
		+ "\tEX\tDE,HL\n"
		+ "D6_ADD_D6_D6_8:\n"
      /*
       * Oberstes Nibble in beiden Operanden auf 0 setzen
       * zwecks Nutzung als Ueberlaufstelle
       */
		+ "\tCALL\tD6_CLEAR_UPPER_NIBBLES\n"
		+ "\tLD\tDE,M_OP1+6\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tXOR\tA\n"			// CY=0
		+ "\tLD\tB,06H\n"
		+ "D6_ADD_D6_D6_9:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSBC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDJNZ\tD6_ADD_D6_D6_9\n"
		+ "\tJR\tNC,D6_ADD_D6_D6_11\n"
      // Ergebnis negativ -> Vorzeichen setzen und Ergebnis negieren
		+ "\tSET\t7,C\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tLD\tB,06H\n"		
		+ "\tOR\tA\n"			// CY=0
		+ "D6_ADD_D6_D6_10:\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,00H\n"
		+ "\tSBC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDJNZ\tD6_ADD_D6_D6_10\n"
		+ "D6_ADD_D6_D6_11:\n"
      /*
       * Oberes Nibble eintragen
       * Ggf. muss die Nachkommaanzahl reduziert werden.
       */
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,D6_ADD_D6_D6_12\n"
      // Ueberlaufstelle vorhanden -> Anzahl Nachkommastellen reduzieren
		+ "\tLD\tA,C\n"
		+ "\tAND\t70H\n"
		+ "\tJP\tZ,E_NUMERIC_OVERFLOW\n"
		+ "\tXOR\tA\n"
		+ "\tRRD\n"
		+ "\tCALL\tD6_DEC_SCALE_1\n"
		+ "\tLD\tA,C\n"
		+ "\tSUB\t10H\n"
		+ "\tLD\tC,A\n"
		+ "D6_ADD_D6_D6_12:\n"
      // oberes Nibble eintragen
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\tC\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_UPPER_NIBBLES );
      compiler.addLibItem( BasicLibrary.LibItem.D6_DEC_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.D6_EQUALIZE_SCALES );
      compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_EQUALIZE_SCALES ) ) {
      /*
       * Sicherstellen, dass die Anzahl der Nachkommastellen
       * in M_OP1 und M_ACCU gleich sind
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   DE: Zeiger auf einen Operanden
       *   HL: Zeiger auf den anderen Operanden
       */
      buf.append( "D6_EQUALIZE_SCALES:\n"
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\tC,A\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,M_OP1\n"
		+ "\tCALL\tD6_GET_SCALE\n"
		+ "\tSUB\tC\n"			// Anz. Op1 - Accu
		+ "\tRET\tZ\n"			// Scales sind gleich
		+ "\tJR\tNC,D6_EQUALIZE_SCALES_1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCPL\n"
		+ "\tINC\tA\n"
		+ "D6_EQUALIZE_SCALES_1:\n"
      /*
       * HL zeigt auf den Wert mit dem groesseren Scale,
       * DE auf den anderen.
       * A: Scale-Unterschied
       *
       * Zuerst wird versucht,
       * bei dem Operand mit dem kleineren Scale diesen zu erhoehen,
       * um moeglichst keine Stellen abschneiden zu muessen.
       * Wenn das nicht moeglich ist oder nicht ausreicht,
       * wird der Scale des anderen Operanden entsprechend reduziert.
       */
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tB,A\n"
		+ "D6_EQUALIZE_SCALES_2:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tNZ,D6_EQUALIZE_SCALES_3\n"	// nicht moeglich
		+ "\tCALL\tD6_INC_SCALE\n"
		+ "\tDJNZ\tD6_EQUALIZE_SCALES_2\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"				// Scales sind gleich
		+ "D6_EQUALIZE_SCALES_3:\n"
		+ "\tEX\tDE,HL\n"
		+ "D6_EQUALIZE_SCALES_4:\n"
		+ "\tCALL\tD6_DEC_SCALE\n"
		+ "\tDJNZ\tD6_EQUALIZE_SCALES_4\n"
		+ "\tRET\n"
      // Anzahl der Nachkommastellen um eins erhoehen
		+ "D6_INC_SCALE:\n"
      // herausfallende Stelle auf Null setzen
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\t(HL),A\n"
      // in den restlichen Bytes um eine Stelle verschieben
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tXOR\tA\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tB,05H\n"
		+ "D6_INC_SCALE_1:\n"
		+ "\tRLD\n"
		+ "\tDEC\tHL\n"
		+ "\tDJNZ\tD6_INC_SCALE_1\n"
		+ "\tPOP\tBC\n"
      // hoechstwertiges Byte anpassen
		+ "\tOR\t(HL)\n"
		+ "\tADD\tA,10H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.D6_DEC_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_DEC_SCALE ) ) {
      /*
       * Reduzierung der Anzahl der Nachkommastellen um eins
       *
       * Parameter:
       *   HL: Zeiger auf die Dec6-Zahl
       */
      buf.append( "D6_DEC_SCALE:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tCALL\tD6_DEC_SCALE_1\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0F0H\n"
		+ "\tSUB\t10H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "D6_DEC_SCALE_1:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,05H\n"
		+ "D6_DEC_SCALE_2:\n"
		+ "\tINC\tHL\n"
		+ "\tRRD\n"
		+ "\tDJNZ\tD6_DEC_SCALE_2\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tZ,D6_DEC_SCALE_3\n"
      // herausgefallene Stelle ist ungleich Null -> Fehlervariable setzen
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tD6_SET_DIGITS_TRUNCATED\n"
		+ "\tPOP\tHL\n"
		+ "D6_DEC_SCALE_3:\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_SET_DIGITS_TRUNCATED );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_DIV_D6_D6 ) ) {
      /*
       * Dec6-Division M_ACCU = M_OP1 / M_ACCU
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Ergebnis
       */
      buf.append( "D6_DIV_D6_D6:\n" );
      // Vorzeichen des Ergebnisses ermitteln und in M_SIGN merken
      buf.append( "\tLD\tA,(M_OP1)\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,(M_ACCU)\n"
		+ "\tXOR\tB\n"
		+ "\tLD\t(M_SIGN),A\n"
      /*
       * Operanden nach links schieben sowie Differenz
       * der Nachkommastellen ermitteln und in M_MODE_SCALE merken
       */
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tCALL\tD6_DIV_PREPARE_OP\n"
		+ "\tJP\tZ,E_DIV_BY_0\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tPUSH\tAF\n"
		+ "\tLD\tHL,M_OP1+6\n"
		+ "\tCALL\tD6_DIV_PREPARE_OP\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tPOP\tBC\n"
		+ "\tSUB\tB\n"
		+ "\tLD\t(M_MODE_SCALE),A\n"
      // M_REG2 leeren
		+ "\tLD\tHL,M_REG2\n"
		+ "\tCALL\tD6_CLEAR_1\n"
      // eigentliche Division
		+ "\tEXX\n"
		+ "\tLD\tC,0CH\n"		// Op2 um 12 Stellen schieben
		+ "\tEXX\n"
		+ "D6_DIV_D6_D6_2:\n"
      /*
       * OP2 solange von OP1 subtrahieren wie moeglich,
       * dabei Durchlaeufe in C (Ergebnis) zaehlen
       */
		+ "\tLD\tC,0FFH\n"		// Startwert Ergebniszaehler
		+ "D6_DIV_D6_D6_3:\n"
		+ "\tINC\tC\n"			// Ergebniszaehler
		+ "\tLD\tDE,M_OP1+6\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tLD\tB,06H\n"		// 12 Bytes
		+ "D6_DIV_D6_D6_4:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSBC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tD6_DIV_D6_D6_4\n"
		+ "\tJR\tNC,D6_DIV_D6_D6_3\n"
      // wieder einmal adieren -> positiver Rest
		+ "\tLD\tDE,M_OP1+6\n"
		+ "\tLD\tHL,M_ACCU+6\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tLD\tB,06H\n"		// 12 Bytes
		+ "D6_DIV_D6_D6_5:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tADC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tD6_DIV_D6_D6_5\n"
      // M_REG2 nach links schieben und Ergebnisziffer eintragen
		+ "\tLD\tHL,M_REG2+6\n"
		+ "\tLD\tA,C\n"			// Ergebnisziffer
		+ "\tLD\tB,06H\n"
		+ "D6_DIV_D6_D6_6:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_DIV_D6_D6_6\n"
      // M_ACCU nach rechts schieben
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,06H\n"
		+ "D6_DIV_D6_D6_7:\n"
		+ "\tRRD\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_DIV_D6_D6_7\n"
      // noch ein Durchlauf?
		+ "\tEXX\n"
		+ "\tDEC\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tNZ,D6_DIV_D6_D6_2\n"
      // Rest vorhanden?
		+ "\tLD\tHL,M_OP1\n"
		+ "\tCALL\tD6_CHECK_DIGITS_TRUNCATED\n"
      /*
       * Anzahl Nachkommastellen setzen,
       * dabei ggf. das Ergebnis nach rechts schieben
       */
		+ "\tLD\tA,(M_MODE_SCALE)\n"
		+ "\tADD\tA,0BH\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(M_REG2)\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,D6_DIV_D6_D6_8\n"
		+ "\tCALL\tD6_DIV_D6_D6_9\n"
		+ "D6_DIV_D6_D6_8:\n"
		+ "\tLD\tA,07H\n"
		+ "\tCP\tC\n"
		+ "\tJP\tNC,D6_MUL_DIV_COPY_RESULT\n"
		+ "\tCALL\tD6_DIV_D6_D6_9\n"
		+ "\tJR\tD6_DIV_D6_D6_8\n"
		+ "D6_DIV_D6_D6_9:\n"
		+ "\tDEC\tC\n"
		+ "\tLD\tHL,M_REG2\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,06H\n"
		+ "D6_DIV_D6_D6_10:\n"
		+ "\tRRD\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_DIV_D6_D6_10\n"
      // Nicht-Null-Ziffer herausgefallen?
		+ "\tAND\t0FH\n"
		+ "\tRET\tZ\n"
		+ "\tJP\tD6_SET_DIGITS_TRUNCATED\n"
      /*
       * Operand aufbereiten:
       *   Zahl soweit nach links schieben,
       *   bis eine Nicht-Null-Ziffer im obersten Nibble steht,
       *
       * Paramter:
       *   HL: Zeiger auf das erste Byte hinter der Zahl
       * Rueckgabe:
       *   A:   angepasste Anzahl der Nachkommastellen
       *   Z=1: Zahl ist Null
       */
		+ "D6_DIV_PREPARE_OP:\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tCALL\tD6_DIV_PREPARE_OP_3\n"
		+ "\tAND\t07H\n"
		+ "\tINC\tA\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tLD\tC,0BH\n"	// max. 11 weitere Verschiebungen
		+ "D6_DIV_PREPARE_OP_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0F0H\n"
		+ "\tRET\tNZ\n"
		+ "\tCALL\tD6_DIV_PREPARE_OP_2\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tINC\tA\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,D6_DIV_PREPARE_OP_1\n"
		+ "\tRET\n"
		+ "D6_DIV_PREPARE_OP_2:\n"
		+ "\tLD\tH,D\n"
		+ "\tLD\tL,E\n"
		+ "D6_DIV_PREPARE_OP_3:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,06H\n"
		+ "D6_DIV_PREPARE_OP_4:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_DIV_PREPARE_OP_4\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_MUL_DIV_UTIL );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_SET_DIGITS_TRUNCATED );
      compiler.addLibItem( BasicLibrary.LibItem.E_DIV_BY_0 );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_MODE_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_SIGN );
      compiler.addLibItem( BasicLibrary.LibItem.M_REG2_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_MUL_D6_D6 ) ) {
      /*
       * Dec6-Multiplikation M_ACCU = M_OP1 * M_ACCU
       *
       * Parameter:
       *   M_OP1:  Operand 1
       *   M_ACCU: Operand 2
       * Rueckgabe:
       *   M_ACCU: Ergebnis
       */
      buf.append( "D6_MUL_D6_D6:\n" );
      // Register 2 loeschen
      buf.append( "\tLD\tHL,M_REG2\n"
		+ "\tLD\tB,0CH\n"
		+ "\tCALL\tD6_CLEAR_2\n"
      /*
       * Multiplikation vorbereiten:
       *   - DE mit der Anfangsadresse von M_ACCU laden
       *   - HL mit der Anfangsadresse von M_OP1 laden
       *   - Vorzeichen des Ergebnisses ermitteln und in M_SIGN merken
       *   - Summe der Nachkommastellen beider Operanden ermitteln
       *     und in M_MODE_SCALE merken
       *   - oberes Nibble in beiden Operanden auf Null setzen
       */
		+ "\tCALL\tD6_GET_ACCU_SCALE\n"
		+ "\tLD\tC,A\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,M_OP1\n"
		+ "\tCALL\tD6_GET_SCALE\n"
		+ "\tLD\tB,A\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tXOR\t(HL)\n"
		+ "\tLD\t(M_SIGN),A\n"
		+ "\tCALL\tD6_CLEAR_UPPER_NIBBLES\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tADD\tA,C\n"
		+ "\tLD\t(M_MODE_SCALE),A\n"
		+ "\tEXX\n"
      /*
       * B: von Op2 bereits verarbeitete Ziffern + 1
       *    dient zur Steuerung, mit wieviel Bytes Reg2 verarbeitet wird
       * C: Op2 um 11 Stellen schieben
       */
		+ "\tLD\tBC,010BH\n"		// Op2 um 11 Stellen schieben
		+ "\tEXX\n"
		+ "\tLD\tA,(M_ACCU)\n"
		+ "D6_MUL_D6_D6_1:\n"
      // Zwischenregister M_REG1 leeren
		+ "\tLD\tHL,M_REG1\n"
		+ "\tLD\tB,0CH\n"
		+ "\tCALL\tD6_CLEAR_2\n"
		+ "\tAND\t0FH\n"		// Ziffer=0?, CY=0
		+ "\tJR\tZ,D6_MUL_D6_D6_4\n"
      // Multiplikation M_REG1 = M_OP1 * HiDigit(M_ACCU)
		+ "\tLD\tC,A\n"
		+ "D6_MUL_D6_D6_2:\n"
		+ "\tLD\tDE,M_REG1+12\n"
		+ "\tLD\tHL,M_OP1+6\n"
		+ "\tLD\tB,06H\n"
		+ "D6_MUL_D6_D6_3:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tADC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tD6_MUL_D6_D6_3\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,D6_MUL_D6_D6_2\n"
		+ "D6_MUL_D6_D6_4:\n"
      // Berechnen, mit wievel Bytes Reg2 verarbeitet werden muss
		+ "\tEXX\n"
		+ "\tINC\tB\n"
		+ "\tLD\tA,B\n"
		+ "\tEXX\n"
		+ "\tSRL\tA\n"
		+ "\tADD\tA,06H\n"
		+ "\tLD\tC,A\n"			// relevante Bytes in Reg2
      // M_REG2 = M_REG2 * 10
		+ "\tLD\tHL,M_REG2+12\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,C\n"		
		+ "D6_MUL_D6_D6_5:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_MUL_D6_D6_5\n"
      // M_REG2 = M_REG2 + M_REG1
		+ "\tLD\tDE,M_REG2+12\n"
		+ "\tLD\tHL,M_REG1+12\n"
		+ "\tLD\tB,C\n"
		+ "\tOR\tA\n"			// CY=0
		+ "D6_MUL_D6_D6_6:\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tADC\tA,(HL)\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tD6_MUL_D6_D6_6\n"
      // noch eine Op2-Ziffer?
		+ "\tEXX\n"
		+ "\tDEC\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,D6_MUL_D6_D6_8\n"
      // Op2 nach links schieben und naechste Ziffer in A liefern
		+ "\tLD\tHL,M_ACCU+5\n"
		+ "\tLD\tB,05H\n"
		+ "D6_MUL_D6_D6_7:\n"
		+ "\tRLD\n"
		+ "\tDEC\tHL\n"
		+ "\tDJNZ\tD6_MUL_D6_D6_7\n"
		+ "\tJR\tD6_MUL_D6_D6_1\n"
		+ "D6_MUL_D6_D6_8:\n"
      /*
       * Ergebnis soweit nach links schieben wie moeglich,
       * maximal jedoch bis Scale=19
       */
		+ "\tLD\tA,(M_MODE_SCALE)\n"
		+ "\tLD\tC,A\n"
		+ "D6_MUL_D6_D6_9:\n"
		+ "\tLD\tA,(M_REG2)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,D6_MUL_D6_D6_11\n"
		+ "\tLD\tHL,M_REG2+12\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,0CH\n"
		+ "D6_MUL_D6_D6_10:\n"
		+ "\tDEC\tHL\n"
		+ "\tRLD\n"
		+ "\tDJNZ\tD6_MUL_D6_D6_10\n"
		+ "\tINC\tC\n"
		+ "\tLD\tA,C\n"
		+ "\tCP\t13H\n"			// 19
		+ "\tJR\tC,D6_MUL_D6_D6_9\n"
		+ "D6_MUL_D6_D6_11:\n"
      /*
       * Da die letzten 12 Ziffern abgeschnitten werden,
       * muss Scale mindestens 12 sein
       */
		+ "\tLD\tA,C\n"
		+ "\tSUB\t0CH\n"		// 12 abziehen
		+ "\tJP\tC,E_NUMERIC_OVERFLOW\n"
		+ "\tLD\tC,A\n"
      // Ergebnis nach M_ACCU kopieren,
		+ "\tCALL\tD6_MUL_DIV_COPY_RESULT\n" );
      // Ergebnis aufbereiten -> direkt weiter mit D6_MUL_DIV_UTIL
      compiler.addLibItem( BasicLibrary.LibItem.D6_MUL_DIV_UTIL );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_UPPER_NIBBLES );
      compiler.addLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU );
      compiler.addLibItem( BasicLibrary.LibItem.D6_GET_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_MODE_SCALE );
      compiler.addLibItem( BasicLibrary.LibItem.M_SIGN );
      compiler.addLibItem( BasicLibrary.LibItem.M_REG1_12 );
      compiler.addLibItem( BasicLibrary.LibItem.M_REG2_12 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_MUL_DIV_UTIL ) ) {
      // Nicht-Null-Ziffern abgeschnitten?
      buf.append( "D6_CHECK_DIGITS_TRUNCATED:\n"
		+ "\tLD\tB,06H\n"
		+ "\tXOR\tA\n"
		+ "D6_CHECK_DIGITS_TRUNCATED_1:\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJP\tNZ,D6_SET_DIGITS_TRUNCATED\n"
		+ "\tDJNZ\tD6_CHECK_DIGITS_TRUNCATED_1\n"
		+ "\tRET\n"
      /*
       * Ergebnis von M_REG2 nach M_ACCU kopieren
       *
       * Parameter:
       *   M_REG2: Quelle, oberstes Nibble muss 0 sein!
       *   M_SIGN: Vorzeichen in Bit 7
       *   C:      Anzahl Nachkommastallen
       */
		+ "D6_MUL_DIV_COPY_RESULT:\n"
		+ "\tLD\tA,(M_SIGN)\n"
		+ "\tAND\t80H\n"		// Vorzeichen, CY=0
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,C\n"			// Scale
		+ "\tRLCA\n"
		+ "\tRLCA\n"
		+ "\tRLCA\n"
		+ "\tRLCA\n"
		+ "\tOR\tB\n"
		+ "\tLD\tHL,M_REG2\n"
		+ "\tOR\t(HL)\n"
		+ "\tLD\tDE,M_ACCU\n"
		+ "\tLD\t(DE),A\n"		// 1. Byte
		+ "\tLD\tB,05H\n"
		+ "D6_MUL_DIV_COPY_RESULT_1:\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tD6_MUL_DIV_COPY_RESULT_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_SET_DIGITS_TRUNCATED );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_REG2_6 );
      compiler.addLibItem( BasicLibrary.LibItem.M_SIGN );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_CLEAR_UPPER_NIBBLES ) ) {
      /*
       * in (DE) und (HL) oberstes Halbbyte auf 0 setzen
       */
      buf.append( "D6_CLEAR_UPPER_NIBBLES:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tD6_CLEAR_UPPER_NIBBLES_1\n"
		+ "\tEX\tDE,HL\n"
		+ "D6_CLEAR_UPPER_NIBBLES_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_CLEAR_ACCU ) ) {
      /*
       * M_ACCU auf numerisch 0 setzen
       */
      buf.append( "D6_CLEAR_ACCU:\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "D6_CLEAR_1:\n"
		+ "\tLD\tB,06H\n"
		+ "D6_CLEAR_2:\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_CLEAR_2\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_NEG_ACCU ) ) {
      /*
       * Dec6-Wert in M_ACCU mathematisch negieren
       *
       * Parameter:
       *   M_ACCU: Eingangswert
       * Rueckgabe:
       *   M_ACCU: Ergebnis
       */
      buf.append( "D6_NEG_ACCU:\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tXOR\t80H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_APPEND_DIGIT_TO_ACCU ) ) {
      /*
       * Anhaengen einer Ziffer an den in M_ACCU stehenden Wert,
       * Dabei wird dieser Wert um eine Dezimalstelle nach links verschoben.
       *
       * Parameter:
       *   A0-A3:  anzuhaengende Ziffer      
       *   M_ACCU: Dec6-Wert, an den die Ziffer angehaengt wird
       * Rueckgabewert:
       *   M_ACCU: neuer Dec6-Wert
       */
      buf.append( "D6_APPEND_DIGIT_TO_ACCU:\n"
		+ "\tLD\tB,06H\n"
		+ "\tLD\tHL,M_ACCU+5\n"
		+ "D6_APPEND_DIGIT_TO_ACCU_1:\n"
		+ "\tRLD\n"
		+ "\tDEC\tHL\n"
		+ "\tDJNZ\tD6_APPEND_DIGIT_TO_ACCU_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_GET_SCALE ) ) {
      /*
       * Lesen der Anzahl der Nachkommestellen
       *
       * Parameter:
       *   HL: Zeiger auf den Dec6-Wert
       * Rueckgabewert:
       *   A0...2: Anzahl der Stellen
       *   A3...7: Null
       *   Z=0:    keine Nachkommastellen
       */
      buf.append( "D6_GET_ACCU_SCALE:\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "D6_GET_SCALE:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tAND\t07H\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_IS_ACCU_ZERO ) ) {
      /*
       * Testen des in M_ACCU stehenden Dec6-Wertes
       * auf numerisch 0.
       *
       * Parameter:
       *   M_ACCU: numerischer Wert
       * Rueckgabewert:
       *   Z=1: Wert ist 0
       */
      buf.append( "D6_IS_ACCU_ZERO:\n"
		+ "\tLD\tHL,M_ACCU\n"
		+ "D6_IS_ZERO:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\tB,05H\n"
		+ "D6_IS_ZERO_1:\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tDJNZ\tD6_IS_ZERO_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU ) ) {
      /*
       * Die Funktion kopiert 6 Bytes aus M_ACCU in den Speicherbereich,
       * auf den HL zeigt.
       *
       * Parameter:
       *   M_ACCU: Wert
       *   HL:     Zeiger auf den Speicherplatz
       */
      buf.append( "D6_LD_MEM_ACCU:\n"
		+ "\tLD\tDE,M_ACCU\n"
		+ "\tLD\tB,6\n"
		+ "D6_LD_MEM_ACCU_1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tD6_LD_MEM_ACCU_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_OP1_MEM ) ) {
      /*
       * Die Funktion kopiert 6 Bytes aus dem Speicherbereich,
       * auf den HL zeigt, nach M_OP1.
       *
       * Parameter:
       *   HL: Zeiger auf den Speicherplatz
       */
      buf.append( "D6_LD_OP1_MEM:\n"
		+ "\tLD\tDE,M_OP1\n"
		+ "\tJR\tD6_LD_MEM_MEM_1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_OP1_MEM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_ACCU_MEM ) ) {
      /*
       * Die Funktion kopiert 6 Bytes aus dem Speicherbereich,
       * auf den HL zeigt, nach M_ACCU.
       *
       * Parameter:
       *   HL: Zeiger auf den Speicherplatz
       */
      buf.append( "D6_LD_ACCU_MEM:\n"
		+ "\tLD\tDE,M_ACCU\n"
		+ "D6_LD_MEM_MEM_1:\n"
		+ "\tLD\tB,6\n"
		+ "D6_LD_MEM_MEM_2:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tD6_LD_MEM_MEM_2\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_OP1_NNNNNN ) ) {
      /*
       * Die Routine kopiert die nach dem Aufruf folgenden 6 Bytes
       * nach M_OP1.
       *
       * Parameter:
       *   (SP): Direktwert
       */
      buf.append( "D6_LD_OP1_NNNNNN:\n"
		+ "\tLD\tDE,M_OP1\n"
		+ "\tJR\tD6_LD_MEM_NNNNNN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_NNNNNN );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_LD_ACCU_NNNNNN ) ) {
      /*
       * Die Routine kopiert die nach dem Aufruf folgenden 6 Bytes
       * nach M_ACCU.
       *
       * Parameter:
       *   (SP): Direktwert
       */
      buf.append( "D6_LD_ACCU_NNNNNN:\n"
		+ "\tLD\tDE,M_ACCU\n"
		+ "D6_LD_MEM_NNNNNN:\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tB,06H\n"
		+ "D6_LD_ACCU_NNNNNN_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tD6_LD_ACCU_NNNNNN_1\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_PUSH_ACCU ) ) {
      /*
       * Die Routine schreibt die 6 Bytes vom M_ACCU auf den Stack.
       *
       * Parameter:
       *   M_ACCU: zu kellernder Wert
       * Rueckgabe:
       *   Stack:  Wert von M_ACCU
       */
      buf.append( "D6_PUSH_ACCU:\n"
		+ "\tPOP\tDE\n"			// Return-Adresse in DE
		+ "\tLD\tHL,(M_ACCU+4)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_ACCU+2)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_ACCU)\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"		// Return-Adresse in HL
		+ "\tJP\t(HL)\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_PUSH_MEM ) ) {
      /*
       * Die Routine schreibt die 6 Bytes, auf die HL zeigt, auf den Stack.
       *
       * Parameter:
       *   HL: Zeiger auf den zu kellernden Wert
       * Rueckgabe:
       *   Stack:  Wert von (HL), 6 Bytes
       */
      buf.append( "D6_PUSH_MEM:\n"
		+ "\tPOP\tDE\n"			// Return-Adresse in DE
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,03H\n"
		+ "D6_PUSH_MEM_1:\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,D6_PUSH_MEM_1\n"
		+ "\tEX\tDE,HL\n"		// Return-Adresse in HL
		+ "\tJP\t(HL)\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_POP_ACCU ) ) {
      /*
       * Die Routine liest vom Stack 6 Bytes und schreibt sie in M_ACCU.
       *
       * Parameter:
       *   Stack:  zu lesender Wert
       * Rueckgabe:
       *   M_ACCU: gelesener Wert
       */
      buf.append( "D6_POP_ACCU:\n"
		+ "\tPOP\tDE\n"			// Return-Adresse in DE
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_ACCU),HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_ACCU+2),HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_ACCU+4),HL\n"
		+ "\tEX\tDE,HL\n"		// Return-Adresse in HL
		+ "\tJP\t(HL)\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_ACCU_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_POP_OP1 ) ) {
      /*
       * Die Routine liest vom Stack 6 Bytes und schreibt sie in M_OP1.
       *
       * Parameter:
       *   Stack: zu lesender Wert
       * Rueckgabe:
       *   M_OP1: gelesener Wert
       */
      buf.append( "D6_POP_OP1:\n"
		+ "\tPOP\tDE\n"			// Return-Adresse in DE
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_OP1),HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_OP1+2),HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(M_OP1+4),HL\n"
		+ "\tEX\tDE,HL\n"		// Return-Adresse in HL
		+ "\tJP\t(HL)\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_OP1_6 );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.D6_SET_DIGITS_TRUNCATED ) ) {
      /*
       * Die Routine setzt den Fehlerstatus auf "Ziffern abgeschintten".
       */
      buf.append( "D6_SET_DIGITS_TRUNCATED:\n" );
      BasicUtil.appendSetErrorDigitsTruncated( compiler );
      buf.append( "\tRET\n" );
    }
  }


  public static void appendBssTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ACCU_7 ) ) {
      buf.append( "M_ACCU:\tDS\t7\n" );
    } else if( compiler.usesLibItem( BasicLibrary.LibItem.M_ACCU_6 ) ) {
      buf.append( "M_ACCU:\tDS\t6\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_REG1_12 ) ) {
      buf.append( "M_REG1:\tDS\t12\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_REG2_12 ) ) {
      buf.append( "M_REG2:\tDS\t12\n" );
    } else if( compiler.usesLibItem( BasicLibrary.LibItem.M_REG2_6 ) ) {
      buf.append( "M_REG2:\tDS\t6\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_MODE_SCALE ) ) {
      buf.append( "M_MODE_SCALE:\tDS\t1\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_SIGN ) ) {
      buf.append( "M_SIGN:\tDS\t1\n" );
    }
  }


	/* --- Konstruktor --- */

  private DecimalLibrary()
  {
    // nicht intstanziierbar
  }
}
