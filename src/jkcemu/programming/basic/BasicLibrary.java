/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Bibliothek
 */

package jkcemu.programming.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import jkcemu.programming.PrgLogger;


public class BasicLibrary
{
  public static enum LibItem {
			INPUT_V_I2, INPUT_V_I4, INPUT_V_D6,
			INPUT_V_S, INPUT_LINE_V_S,
			INPUT_PASSWORD_V_S,
			INPUT_ERROR, INPUT_LINE, INPUT_TEXT,
			PRINT_D6, PRINT_D6MEM,
			PRINTF_D6, PRINTF_D6MEM,
			PRINT_I2, PRINTF_I2, PRINT_I4, PRINTF_I4,
			PRINTF_S,
			IO_PRINT_D6, IO_PRINT_D6MEM,
			IO_PRINTF_D6, IO_PRINTF_D6MEM,
			IO_PRINT_I2, IO_PRINTF_I2, IO_PRINT_I4, IO_PRINTF_I4,
			IO_PRINTF_S,
			IO_PRINT_S, IO_PRINT_TRAILING_S,
			IO_PRINT_SP, IO_PRINT_NL,
			CIRCLE,
			DATA, DATA_READ_I2, DATA_READ_I4,
			DATA_READ_D6, DATA_READ_S,
			DRAW,  DRAWR, DRAW_LINE, DRAWS, DRAWST,
			DRBOX, DRBOXF, H_BOX, DRAW_HLINE,
			DRLBL, DRLBLT, MOVER, LOCATE,
			GET_ON_GO_ADDR,
			NEXT_N, NEXT_UP, NEXT_INC, NEXT_DOWN, NEXT_DEC,
			PAINT, PAINT_M_HPIX, PAINT_M_WPIX,
			PAUSE, PAUSE_N, PEN, PLOTR,
			PRINT_SPC, SCREEN, SWAP6, SWAP4, SWAP2, SWAP,
			IO_PRINT_SPC,
			IOCLOSE, IOOPEN, IOEOF,
			IOINL, IOINX, IORDB, IO_SET_COUT,
			IO_CRT_HANDLER, IO_LPT_HANDLER,
			IO_SIMPLE_OUT_HANDLER,
			IO_DISK_HANDLER, IO_VDIP_HANDLER,
			IO_COUT, IO_M_COUT, IOCADR, IOCTB1, IOCTB2,
			VDIP_DATA_RDPTRS, VDIP_INIT, VDIP_M_IOADDR,
			F_D6_ABS_D6, F_D6_CDEC_I2, F_D6_CDEC_I4,
			F_D6_FRAC_D6, F_D6_ROUND_D6,
			F_D6_ROUND_D6_I2, F_D6_ROUND_D6_I2_I2,
			F_D6_TRUNC_D6, F_D6_TRUNC_D6_I2,
			F_D6_TRUNC_D6_I2_I2,
			F_D6_VAL_S,
			F_I2_CINT_I4,
			F_I2_INSTR, F_I2_INSTR_N, F_I2_LEN,
			F_I2_CINT_D6, F_I2_IS_TARGET_I2, F_I2_JOY_I2,
			F_I2_RND_I2,
			F_I2_SCALE_D6,
			F_I2_SGN_I2, F_I2_SGN_I4, F_I2_SGN_D6,
			F_I2_SQR_I2, F_I4_SQR_I4,
			F_I2_VAL_S_I2, F_I2_VAL_BIN_S,
			F_I2_VAL_DEC_S, F_I2_VAL_HEX_S,
			F_I4_CLNG_D6,
			F_I4_HIWORD_I4, F_I4_LOWORD_I4,
			F_I4_VAL_S_I2, F_I4_VAL_BIN_S,
			F_I4_VAL_DEC_S, F_I4_VAL_HEX_S,
			F_S_BIN_I2, F_S_BIN_I2_I2, F_S_BIN_I4, F_S_BIN_I4_I2,
			F_S_BIN_HL,
			F_S_HEX_I2, F_S_HEX_I2_I2, F_S_HEX_I4, F_S_HEX_I4_I2,
			F_S_HEX_HL,
			F_S_OCT_I2, F_S_OCT_I2_I2, F_S_OCT_I4, F_S_OCT_I4_I2,
			F_S_CHR, F_S_LEFT, F_S_LOWER,
			F_S_LTRIM, F_S_RTRIM, F_S_TRIM,
			F_S_MID, F_S_MID_N, F_S_MIRROR,
			F_S_RIGHT, F_S_STRING_I2_C, F_S_STRING_I2_S,
			F_S_DATETIME_S,
			F_S_UPPER,
			S_INPUT_N, S_INCHAR, S_INKEY,
			S_STR_I2, S_STR_I4, S_STR_D6, S_STR_D6MEM,
			SKIP_LEADING_CHARS, SKIP_LEADING_ZEROS,
			TERMINATE_TMP_STR_AT_DE,
			ARRAY2_ELEM_ADDR, ARRAY4_ELEM_ADDR,
			ARRAY6_ELEM_ADDR, CHECK_ARRAY_IDX,
			D6_ADD_D6_D6, D6_SUB_D6_D6, D6_CMP_D6_D6,
			D6_MUL_D6_D6, D6_DIV_D6_D6, D6_MUL_DIV_UTIL,
			D6_MAX_D6_D6, D6_MIN_D6_D6,
			D6_NEG_ACCU, D6_IS_ACCU_ZERO,
			D6_EQUALIZE_SCALES, D6_DEC_SCALE, D6_GET_SCALE,
			D6_ROUND_UTIL_00, D6_ROUND_UTIL_HL, D6_TRUNC_UTIL,
			D6_APPEND_DIGIT_TO_ACCU,
			D6_CLEAR_ACCU, D6_CLEAR_UPPER_NIBBLES,
			D6_LD_ACCU_NNNNNN, D6_LD_ACCU_IYOFFS,
			D6_LD_ACCU_OP1, D6_LD_ACCU_MEM, D6_LD_MEM_ACCU,
			D6_LD_OP1_MEM, D6_LD_OP1_NNNNNN,
			D6_PUSH_ACCU, D6_PUSH_MEM, D6_POP_ACCU, D6_POP_OP1,
			D6_SET_DIGITS_TRUNCATED,
			I4_AND_I4_I4, I4_NOT_I4, I4_OR_I4_I4, I4_XOR_I4_I4,
			I4_ADD_I4_I4, I4_SUB_I4_I4,
			I4_MUL_I4_I4, I4_MOD_I4_I4,
			I4_DIV_I4_I4, I4_DIV_UTIL,
			I4_SHL_I4_I2, I4_SHR_I4_I2, I4_SHX_UTIL,
			I4_MAX_I4_I4, I4_MIN_I4_I4,
			I4_DEC_MEM, I4_INC_MEM,
			I4_ABS_DEHL, I4_NEG_DEHL, I4_NEG_DEHL_RAW,
			I4_ERR, I4_VAL_UTIL, I4_HL_TO_DEHL,
			I4_HL2HL_TO_DEHL, I4_POP_DE2HL2,
			LD_DEHL_MEM,
			LD_HL_MEM, LD_HL2HL_NULL,
			LD_MEM_NNNN, LD_MEM_DEBC, LD_MEM_DEHL,
			LD_MEM_DE2HL2, CHECK_DEHL_FOR_I2,
			I2_AND_I2_I2, I2_NOT_I2, I2_OR_I2_I2, I2_XOR_I2_I2,
			I2_ADD_I2_I2, I2_SUB_I2_I2,
			I2_MUL_I2_I2, I2_MOD_I2_I2, I2_DIV_I2_I2,
			I2_INC_I2, I2_DEC_I2, I2_SHL_I2_I2, I2_SHR_I2_I2,
			I2_MAX_I2_I2, I2_MIN_I2_I2,
			I2_EQ_I2_I2, I2_EQ_I4_I4, I2_EQ_D6_D6, I2_EQ_S_S,
			I2_GE_I2_I2, I2_GE_I4_I4, I2_GE_D6_D6, I2_GE_S_S,
			I2_GT_I2_I2, I2_GT_I4_I4, I2_GT_D6_D6, I2_GT_S_S,
			I2_LE_I2_I2, I2_LE_I4_I4, I2_LE_D6_D6, I2_LE_S_S,
			I2_LT_I2_I2, I2_LT_I4_I4, I2_LT_D6_D6, I2_LT_S_S,
			I2_NE_I2_I2, I2_NE_I4_I4, I2_NE_D6_D6, I2_NE_S_S,
			I2_CONTAINS, I2_DEC_MEM, I2_INC_MEM,
			I2_ABS_HL, I2_NEG_HL, I2_NEG_HL_RAW,
			I2_STR_CMP, STR_N_COPY,
			I2_APPEND_DIGIT_TO_HL, I2_ERR, I2_VAL_UTIL,
			CMP_HL_DE, CMP_DE2HL2_DEHL,
			ASSIGN_STR_TO_NEW_MEM_VS, ASSIGN_STR_TO_VS,
			ASSIGN_VS_TO_VS,
			MFIND, MALLOC, MMGC, MRGC, MFREE,
			CHECK_DE_WITHIN_HEAP, HEAP,
			CHECK_STACK, ADD_SP_N, JP_HL,
			MALLOC_AND_STR_COPY, STR_VAR_DUP,
			C_UPPER_C,
			CLEAR_MEM, RESET_ERROR,
			EMPTY_STRING,
			E_DIV_BY_0, E_IDX_OUT_OF_RANGE, E_INVALID_PARAM,
			E_NEXT_WITHOUT_FOR, E_NUMERIC_OVERFLOW,
			E_OUT_OF_DATA, E_RET_WITHOUT_GOSUB,
			E_TYPE_MISMATCH, E_EXIT, E_USR_FUNCTION_NOT_DEFINED,
			OUTSP, FONT_5X7,
			M_OP1_4, M_OP1_6, M_ACCU_6, M_ACCU_7,
			M_MODE_SCALE, M_SIGN,
			M_RETVAL_2, M_RETVAL_4, M_RETVAL_6,
			M_REG1_12, M_REG2_12, M_REG2_6,
			M_CVTBUF,
			M_BASIC_LINE_NUM, M_SOURCE_NAME,
			M_ERROR_NUM, M_ERROR_TEXT,
			M_KEY_BUF, M_TMP_STR_BUF, M_XYPOS, MTOP,
			XCHECK_BREAK, XINCH, XINKEY, XBREAK,
			XBORDER, XCLS, XCOLOR, XCRSLIN, XCRSPOS, XCURSOR,
			XDATETIME, XDOSTIME, XHLINE, XINK, XPAPER,
			XOUTST, XOUTS, XOUTNL, XOUTCH, XLOCATE, XLPTCH,
			XPEN, XPAINT, XPOINT, XPSET, XPRES, XPTEST,
			XSCREEN, XJOY };

  // Fehlercodes
  public static final int E_OK                   = 0;
  public static final int E_ERROR                = -1;
  public static final int E_INVALID              = -2;
  public static final int E_OVERFLOW             = -3;
  public static final int E_DIGITS_TRUNCATED     = -4;
  public static final int E_CHANNEL_ALREADY_OPEN = -11;
  public static final int E_CHANNEL_CLOSED       = -12;
  public static final int E_DEVICE_NOT_FOUND     = -21;
  public static final int E_DEVICE_LOCKED        = -22;
  public static final int E_HARDWARE             = -23;
  public static final int E_NO_DISK              = -24;
  public static final int E_FILE_NOT_FOUND       = -25;
  public static final int E_PATH_NOT_FOUND       = -26;
  public static final int E_IO_MODE              = -27;
  public static final int E_IO_ERROR             = -28;
  public static final int E_EOF                  = -29;
  public static final int E_READ_ONLY            = -30;
  public static final int E_DIR_FULL             = -31;
  public static final int E_DISK_FULL            = -32;
  public static final int E_MEDIA_CHANGED        = -33;

  /*
   * Offsets innerhalb des Kanalzeigerfelds
   *
   * An erster Stelle (Offset 0) steht der Zeiger zur CLOSE-Routine.
   * Anhand des Vorhandensein dieses Zeigers wird erkannt,
   * ob der Kanal offen oder geschlossen ist.
   */
  public static final int IOCTB_EOF_OFFS    = 2;
  public static final int IOCTB_READ_OFFS   = 4;
  public static final int IOCTB_WRITE_OFFS  = 6;
  public static final int IOCTB_BBUF_OFFS   = 8;
  public static final int IOCTB_DRIVER_OFFS = 10;

  /*
   * Betriebsmodi eines Kanals:
   *   Bit 0: Default
   *   Bit 1: Input
   *   Bit 2: Output
   *   Bit 3: Append
   *   Bit 4: Text
   *   Bit 5: Binary
   */
  public static final int IOMODE_DEFAULT_MASK = 0x01;
  public static final int IOMODE_INPUT_MASK   = 0x02;
  public static final int IOMODE_OUTPUT_MASK  = 0x04;
  public static final int IOMODE_APPEND_MASK  = 0x08;
  public static final int IOMODE_TXT_MASK     = 0x10;
  public static final int IOMODE_BIN_MASK     = 0x20;

  public static final int IOMODE_TXT_DEFAULT = IOMODE_TXT_MASK
						| IOMODE_DEFAULT_MASK;

  public static final int IOMODE_TXT_INPUT = IOMODE_TXT_MASK
						| IOMODE_INPUT_MASK;

  public static final int IOMODE_TXT_OUTPUT = IOMODE_TXT_MASK
						| IOMODE_OUTPUT_MASK;

  public static final int IOMODE_TXT_APPEND = IOMODE_TXT_MASK
						| IOMODE_APPEND_MASK;

  public static final int IOMODE_BIN_DEFAULT = IOMODE_BIN_MASK
						| IOMODE_DEFAULT_MASK;

  public static final int IOMODE_BIN_INPUT = IOMODE_BIN_MASK
						| IOMODE_INPUT_MASK;

  public static final int IOMODE_BIN_OUTPUT = IOMODE_BIN_MASK
						| IOMODE_OUTPUT_MASK;

  public static final int IOMODE_BIN_APPEND = IOMODE_BIN_MASK
						| IOMODE_APPEND_MASK;

  // Rundungsmodi
  public static final int ROUND_HALF_DOWN = 0;
  public static final int ROUND_HALF_EVEN = 1;
  public static final int ROUND_HALF_UP   = 2;

  // sonstige Konstanten
  public static final String EMPTY_STRING_LABEL = "EMPTY_STRING";

  public static final String CALL_CHECK_STACK    = "\tCALL\tCHECK_STACK\n";
  public static final String CALL_CHECK_STACK_N  = "\tCALL\tCHECK_STACK_N\n";
  public static final String SUB_CHECK_STACK_BEG = "CHECK_STACK:\n"
						+ "\tLD\tDE,0000H\n"
						+ "CHECK_STACK_N:\n";
  public static final String SUB_CHECK_STACK_END = "\tRET\n";

  public static final String CALL_RESET_ERROR    = "\tCALL\tRESET_ERROR\n";
  public static final String SUB_RESET_ERROR_BEG = "RESET_ERROR:\n"
						+ "\tPUSH\tHL\n";
  public static final String SUB_RESET_ERROR_END = "\tPOP\tHL\n"
						+ "\tRET\n";

  public static final String TEXT_INVALID_PARAM_DE = "Ungueltiger Parameter";
  public static final String TEXT_INVALID_PARAM_EN = "Invalid parameter";


  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf     buf       = compiler.getCodeBuf();
    BasicOptions   options   = compiler.getBasicOptions();
    AbstractTarget target    = compiler.getTarget();
    int            stackSize = options.getStackSize();

    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Bibliotheksfunktionen\n" );
    }
    if( compiler.usesLibItem( LibItem.INPUT_V_I2 ) ) {
      /*
       * Eingabe einer Zahl in eine Integer-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die Integer-Variable
       * Rueckgabewert:
       *   CY=1: Fehlerhafte Eingabe
       */
      buf.append( "INPUT_V_I2:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tINPUT_TEXT\n" );
      buf.append_CALL_RESET_ERROR( compiler );
      buf.append( "\tCALL\tF_I2_VAL_DEC_S\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,(M_ERROR_NUM)\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,INPUT_ERROR\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.INPUT_TEXT );
      compiler.addLibItem( LibItem.INPUT_ERROR );
      compiler.addLibItem( LibItem.F_I2_VAL_DEC_S );
      compiler.addLibItem( LibItem.M_ERROR_NUM );
    }
    if( compiler.usesLibItem( LibItem.INPUT_V_I4 ) ) {
      /*
       * Eingabe einer Zahl in eine Long-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die Long-Variable
       * Rueckgabewert:
       *   CY=1: Fehlerhafte Eingabe
       */
      buf.append( "INPUT_V_I4:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tINPUT_TEXT\n" );
      buf.append_CALL_RESET_ERROR( compiler );
      buf.append( "\tCALL\tF_I4_VAL_DEC_S\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,(M_ERROR_NUM)\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,INPUT_ERROR\n"
		+ "\tJP\tLD_MEM_DE2HL2\n" );
      compiler.addLibItem( LibItem.INPUT_TEXT );
      compiler.addLibItem( LibItem.INPUT_ERROR );
      compiler.addLibItem( LibItem.F_I4_VAL_DEC_S );
      compiler.addLibItem( LibItem.LD_MEM_DE2HL2 );
      compiler.addLibItem( LibItem.M_ERROR_NUM );
    }
    if( compiler.usesLibItem( LibItem.INPUT_V_D6 ) ) {
      /*
       * Eingabe einer Zahl in eine Decimal-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die Decimal-Variable
       * Rueckgabewert:
       *   CY=1: Fehlerhafte Eingabe
       */
      buf.append( "INPUT_V_D6:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tINPUT_TEXT\n" );
      buf.append_CALL_RESET_ERROR( compiler );
      buf.append( "\tCALL\tF_D6_VAL_S\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,(M_ERROR_NUM)\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,D6_LD_MEM_ACCU\n"
		+ "\tJR\tINPUT_ERROR\n" );
      compiler.addLibItem( LibItem.INPUT_TEXT );
      compiler.addLibItem( LibItem.INPUT_ERROR );
      compiler.addLibItem( LibItem.F_D6_VAL_S );
      compiler.addLibItem( LibItem.D6_LD_MEM_ACCU );
      compiler.addLibItem( LibItem.M_ERROR_NUM );
    }
    if( compiler.usesLibItem( LibItem.INPUT_ERROR ) ) {
      /*
       * Pruefen des Fehlercodes nach einer Eingabe
       * und Ausgabe des Fehlertextes
       *
       * Parameter:
       *   BC: Fehlercode
       * Rueckgabewert:
       *   CY=1
       */
      buf.append( "INPUT_ERROR:\n" );
      buf.append_LD_HL_nn( E_OVERFLOW );
      buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNZ,INPUT_ERROR_1\n"
		+ "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Betrag zu gross!\'\n" );
      } else {
	buf.append( "\tDB\t\'Amount too big!\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJR\tINPUT_ERROR_3\n"
		+ "INPUT_ERROR_1:\n" );
      if( compiler.usesLibItem( LibItem.INPUT_V_D6 ) ) {
	buf.append_LD_HL_nn( E_DIGITS_TRUNCATED );
	buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNZ,INPUT_ERROR_2\n"
		+ "\tCALL\tXOUTST\n" );
	if( compiler.isLangCode( "DE" ) ) {
	  buf.append( "\tDB\t\'Zu viele Nachkommastellen!\'\n" );
	} else {
	  buf.append( "\tDB\t\'Too many fractional digits!\'\n" );
	}
	buf.append( "\tDB\t00H\n"
		+ "\tJR\tINPUT_ERROR_3\n"
		+ "INPUT_ERROR_2:\n" );
      }
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Wie bitte?\'\n" );
      } else {
	buf.append( "\tDB\t\'What?\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "INPUT_ERROR_3:\n"
		+ "\tCALL\tXOUTNL\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.XOUTST );
      compiler.addLibItem( LibItem.XOUTNL );
    }
    if( compiler.usesLibItem( LibItem.INPUT_V_S ) ) {
      /*
       * Eingabe einer Zeile in eine String-Variable
       * mit Abschneiden der fuehrenden Leerzeichen
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INPUT_V_S:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tINPUT_TEXT\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,INPUT_V_S_1\n" );
      buf.append_LD_DE_xx( EMPTY_STRING_LABEL );
      buf.append( "\tJR\tINPUT_V_S_3\n"
		+ "INPUT_V_S_1:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "INPUT_V_S_2:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,INPUT_V_S_2\n"
		+ "\tDEC\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "INPUT_V_S_3:\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tMFREE\n" );
      compiler.addLibItem( LibItem.INPUT_TEXT );
      compiler.addLibItem( LibItem.MFIND );
      compiler.addLibItem( LibItem.MALLOC );
      compiler.addLibItem( LibItem.MFREE );
      compiler.addLibItem( LibItem.EMPTY_STRING );
    }
    if( compiler.usesLibItem( LibItem.INPUT_LINE_V_S ) ) {
      /*
       * Eingabe einer ganzen Zeile in eine String-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INPUT_LINE_V_S:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPUSH\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tC,0FFH\n"
		+ "\tCALL\tINPUT_LINE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tMFREE\n" );
      compiler.addLibItem( LibItem.INPUT_LINE );
      compiler.addLibItem( LibItem.MFIND );
      compiler.addLibItem( LibItem.MALLOC );
      compiler.addLibItem( LibItem.MFREE );
    }
    if( compiler.usesLibItem( LibItem.INPUT_TEXT ) ) {
      /*
       * Eingabe einer Zeile in den temporaeren String-Puffer
       * mit Abschneiden fuehrender Leerzeichen
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die eingegebene Zeichenkette
       */
      buf.append( "INPUT_TEXT:\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tLD\tC," );
      buf.appendHex2( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tCALL\tINPUT_LINE\n"
		+ "\tLD\tHL,M_TMP_STR_BUF-1\n"
		+ "INPUT_TEXT_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,INPUT_TEXT_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.INPUT_LINE );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.INPUT_LINE ) ) {
      /*
       * Eingabe einer Zeile, die mit ENTER abgeschlossen wird
       *
       * Parameter:
       *   HL: Anfangsadresse des Puffers
       *   C:  max. Anzahl einzugebender Zeichen (Pufferlaenge - 1)
       * Rueckgabewert:
       *   HL: Zeiger auf das terminierende Nullbyte
       */
      buf.append( "INPUT_LINE:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,C\n"
		+ "INPUT_LINE_1:\n"
		+ "\tLD\t(HL),20H\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tINPUT_LINE_1\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,00H\n"
		+ "\tJR\tINPUT_LINE_3\n"
		+ "INPUT_LINE_2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "INPUT_LINE_3:\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tD,A\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,INPUT_LINE_5\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tZ,INPUT_LINE_5\n"
		+ "\tCP\t09H,\n"
		+ "\tJR\tZ,INPUT_LINE_6\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,INPUT_LINE_7\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,INPUT_LINE_3\n"
		+ "INPUT_LINE_4:\n"
		+ "\tLD\tA,B\n"
		+ "\tCP\tC\n"
		+ "\tJR\tNC,INPUT_LINE_3\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tB\n"
		+ "\tJR\tINPUT_LINE_2\n"
		+ "INPUT_LINE_5:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,INPUT_LINE_3\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tB\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tINPUT_LINE_2\n"
		+ "INPUT_LINE_6:\n"
		+ "\tLD\tA,B\n"
		+ "\tCP\tC\n"
		+ "\tJR\tNC,INPUT_LINE_3\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tB\n"
		+ "\tJR\tINPUT_LINE_2\n"
		+ "INPUT_LINE_7:\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tJP\tXOUTNL\n" );
      compiler.addLibItem( LibItem.XINCH );
      compiler.addLibItem( LibItem.XOUTNL );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.INPUT_PASSWORD_V_S ) ) {
      /*
       * Eingabe eines Kennwortes in eine String-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INPUT_PASSWORD_V_S:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPUSH\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tB,00H\n"
		+ "INPUT_PASSWORD_V_S_1:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,INPUT_PASSWORD_V_S_3\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tZ,INPUT_PASSWORD_V_S_3\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,INPUT_PASSWORD_V_S_4\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,INPUT_PASSWORD_V_S_1\n"
		+ "\tINC\tB\n"
		+ "\tJR\tZ,INPUT_PASSWORD_V_S_2\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,2AH\n"		// Echo-Zeichen
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tINPUT_PASSWORD_V_S_1\n"
		+ "INPUT_PASSWORD_V_S_2:\n"
		+ "\tDEC\tB\n"
		+ "\tJR\tINPUT_PASSWORD_V_S_1\n"
		+ "INPUT_PASSWORD_V_S_3:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,INPUT_PASSWORD_V_S_1\n"
		+ "\tDEC\tB\n"
		+ "\tDEC\tHL\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,08H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,08H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tINPUT_PASSWORD_V_S_1\n"
		+ "INPUT_PASSWORD_V_S_4:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTNL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tMFREE\n" );
      compiler.addLibItem( LibItem.MFIND );
      compiler.addLibItem( LibItem.MALLOC );
      compiler.addLibItem( LibItem.MFREE );
      compiler.addLibItem( LibItem.XINCH );
      compiler.addLibItem( LibItem.XOUTNL );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.PRINT_D6 ) ) {
      /*
       * Unformatierte Ausgabe einer Dec6-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   M_ACCU: auszugebende Zahl
       */
      buf.append( "PRINT_D6:\n"
		+ "\tCALL\tS_STR_D6\n"
		+ "\tJP\tXOUTS\n" );
      compiler.addLibItem( LibItem.S_STR_D6 );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.PRINT_D6MEM ) ) {
      /*
       * Unformatierte Ausgabe einer Dec6-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: Zeiger auf die auszugebende Zahl
       */
      buf.append( "PRINT_D6MEM:\n"
		+ "\tCALL\tS_STR_D6MEM\n"
		+ "\tJP\tXOUTS\n" );
      compiler.addLibItem( LibItem.S_STR_D6MEM );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.PRINT_I4 ) ) {
      /*
       * Unformatierte Ausgabe einer Long-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   DEHL: auszugebende Zahl
       */
      buf.append( "PRINT_I4:\n"
		+ "\tCALL\tS_STR_I4\n"
		+ "\tJP\tXOUTS\n" );
      compiler.addLibItem( LibItem.S_STR_I4 );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.PRINT_I2 ) ) {
      /*
       * Unformatierte Ausgabe einer Integer-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "PRINT_I2:\n"
		+ "\tCALL\tS_STR_I2\n"
		+ "\tJP\tXOUTS\n" );
      compiler.addLibItem( LibItem.S_STR_I2 );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.PRINTF_D6 ) ) {
      /*
       * Formatierte Ausgabe einer Dec6-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   M_ACCU: auszugebende Zahl
       */
      buf.append( "PRINTF_D6:\n"
		+ "\tCALL\tS_STR_D6\n"
		+ "\tJR\tPRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6 );
      compiler.addLibItem( LibItem.PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.PRINTF_D6MEM ) ) {
      /*
       * Formatierte Ausgabe einer Dec6-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: Zeiger auf die auszugebende Zahl
       */
      buf.append( "PRINTF_D6MEM:\n"
		+ "\tCALL\tS_STR_D6MEM\n"
		+ "\tJR\tPRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6MEM );
      compiler.addLibItem( LibItem.PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.PRINTF_I4 ) ) {
      /*
       * Formatierte Ausgabe einer Long-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   DEHL: auszugebende Zahl
       */
      buf.append( "PRINTF_I4:\n"
		+ "\tCALL\tS_STR_I4\n"
		+ "\tJR\tPRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_I4 );
      compiler.addLibItem( LibItem.PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.PRINTF_I2 ) ) {
      /*
       * Formatierte Ausgabe einer Integer-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "PRINTF_I2:\n"
		+ "\tCALL\tS_STR_I2\n" );
      compiler.addLibItem( LibItem.S_STR_I2 );
      compiler.addLibItem( LibItem.PRINTF_S );
      // direkt weiter mit PRINTF_S!
    }
    if( compiler.usesLibItem( LibItem.PRINTF_S ) ) {
      /*
       * Formatierte Ausgabe einer Zeichenkette auf dem Bildschirm
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "PRINTF_S:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_I2_LEN\n"
		+ "\tLD\tA,0EH\n"
		+ "\tSUB\tL\n"
		+ "\tJR\tC,PRINTF_S_2\n"
		+ "\tJR\tZ,PRINTF_S_2\n"
		+ "PRINTF_S_1:\n"
		+ "\tPUSH\tAF\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tAF\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,PRINTF_S_1\n"
		+ "PRINTF_S_2:\n"
		+ "\tPOP\tHL\n"
		+ "\tJP\tXOUTS\n" );
      compiler.addLibItem( LibItem.F_I2_LEN );
      compiler.addLibItem( LibItem.XOUTCH );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.PRINT_SPC ) ) {
      /*
       * Ausgabe von Leerzeichen auf dem Bildschirm
       *
       * Parameter:
       *   HL: Anzahl Leerzeichen
       */
      buf.append( "PRINT_SPC:\n"
		+ "\tBIT\t7,H\n"
		+ "\tRET\tNZ\n"
		+ "PRINT_SPC1:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tJR\tPRINT_SPC1\n" );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_D6 ) ) {
      /*
       * Unformatierte Ausgabe einer Dec6-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   M_ACCU: auszugebende Zahl
       */
      buf.append( "IO_PRINT_D6:\n"
		+ "\tCALL\tS_STR_D6\n"
		+ "\tJP\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6 );
      compiler.addLibItem( LibItem.IO_PRINT_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_D6MEM ) ) {
      /*
       * Unformatierte Ausgabe einer Dec6-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: Zeiger auf die auszugebende Zahl
       */
      buf.append( "IO_PRINT_D6MEM:\n"
		+ "\tCALL\tS_STR_D6MEM\n"
		+ "\tJP\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6MEM );
      compiler.addLibItem( LibItem.IO_PRINT_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_I4 ) ) {
      /*
       * Unformatierte Ausgabe einer Long-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   DEHL: auszugebende Zahl
       */
      buf.append( "IO_PRINT_I4:\n"
		+ "\tCALL\tS_STR_I4\n"
		+ "\tJP\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.S_STR_I4 );
      compiler.addLibItem( LibItem.IO_PRINT_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_I2 ) ) {
      /*
       * Unformatierte Ausgabe einer Integer-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "IO_PRINT_I2:\n"
		+ "\tCALL\tS_STR_I2\n"
		+ "\tJP\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.S_STR_I2 );
      compiler.addLibItem( LibItem.IO_PRINT_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINTF_D6 ) ) {
      /*
       * Formatierte Ausgabe einer Dec6-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   M_ACCU: auszugebende Zahl
       */
      buf.append( "IO_PRINTF_D6:\n"
		+ "\tCALL\tS_STR_D6\n"
		+ "\tJR\tIO_PRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6 );
      compiler.addLibItem( LibItem.IO_PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINTF_D6MEM ) ) {
      /*
       * Formatierte Ausgabe einer Dec6-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: Zeiger auf die auszugebende Zahl
       */
      buf.append( "IO_PRINTF_D6MEM:\n"
		+ "\tCALL\tS_STR_D6MEM\n"
		+ "\tJR\tIO_PRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_D6MEM );
      compiler.addLibItem( LibItem.IO_PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINTF_I4 ) ) {
      /*
       * Formatierte Ausgabe einer Long-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   DEHL: auszugebende Zahl
       */
      buf.append( "IO_PRINTF_I4:\n"
		+ "\tCALL\tS_STR_I4\n"
		+ "\tJR\tIO_PRINTF_S\n" );
      compiler.addLibItem( LibItem.S_STR_I4 );
      compiler.addLibItem( LibItem.IO_PRINTF_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINTF_I2 ) ) {
      /*
       * Formatierte Ausgabe einer Integer-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "IO_PRINTF_I2:\n"
		+ "\tCALL\tS_STR_I2\n" );
      compiler.addLibItem( LibItem.IO_PRINTF_S );
      // direkt weiter mit IO_PRINTF_S!
    }
    if( compiler.usesLibItem( LibItem.IO_PRINTF_S ) ) {
      /*
       * Formatierte Ausgabe einer Zeichenkette auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "IO_PRINTF_S:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_I2_LEN\n"
		+ "\tLD\tA,0EH\n"
		+ "\tSUB\tL\n"
		+ "\tJR\tC,IO_PRINTF_S_2\n"
		+ "\tJR\tZ,IO_PRINTF_S_2\n"
		+ "IO_PRINTF_S_1:\n"
		+ "\tPUSH\tAF\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tPOP\tAF\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,IO_PRINTF_S_1\n"
		+ "IO_PRINTF_S_2:\n"
		+ "\tPOP\tHL\n"
		+ "\tJP\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.S_STR_I2 );
      compiler.addLibItem( LibItem.F_I2_LEN );
      compiler.addLibItem( LibItem.IO_PRINT_S );
      compiler.addLibItem( LibItem.IO_COUT );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_SPC ) ) {
      /*
       * Ausgabe von Leerzeichen auf dem Ausgabekanal
       *
       * Parameter:
       *   HL: Anzahl Leerzeichen
       */
      buf.append( "IO_PRINT_SPC:\n"
		+ "\tBIT\t7,H\n"
		+ "\tRET\tNZ\n"
		+ "IO_PRINT_SPC1:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tPOP\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tJR\tIO_PRINT_SPC1\n" );
      compiler.addLibItem( LibItem.IO_COUT );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_TRAILING_S ) ) {
      buf.append( "IO_PRINT_TRAILING_S:\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tCALL\tIO_PRINT_S\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.IO_PRINT_S );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_S ) ) {
      buf.append( "IO_PRINT_S:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tIO_PRINT_S\n" );
      compiler.addLibItem( LibItem.IO_COUT );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_SP ) ) {
      buf.append( "IO_PRINT_SP:\n"
		+ "\tLD\tA,20H\n"
		+ "\tJR\tIO_COUT\n" );
      compiler.addLibItem( LibItem.IO_COUT );
    }
    if( compiler.usesLibItem( LibItem.IO_PRINT_NL ) ) {
      buf.append( "IO_PRINT_NL:\n"
		+ "\tLD\tA,0DH\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tLD\tA,0AH\n" );
      compiler.addLibItem( LibItem.IO_COUT );
      // direkt weiter mit IO_COUT !!!
    }
    if( compiler.usesLibItem( LibItem.IO_COUT ) ) {
      buf.append( "IO_COUT:\n"
		+ "\tLD\tHL,(IO_M_COUT)\n"
		+ "\tJP\t(HL)\n" );
      compiler.addLibItem( LibItem.IO_M_COUT );
    }
    if( compiler.usesLibItem( LibItem.IO_SET_COUT ) ) {
      /*
       * Die Methode setzt die Adresse der Ausgaberoutine.
       * Die Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Adresse der Ausgaberoutine
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Adresse ist Null
       */
      buf.append( "IO_SET_COUT:\n"
		+ "\tLD\t(IO_M_COUT),HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n" );
      BasicUtil.appendSetErrorChannelClosed( compiler );
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.IO_M_COUT );
    }
    if( compiler.usesLibItem( LibItem.DATA_READ_D6 ) ) {
      /*
       * Lesen eines Int1-, Int2-, Int4- oder Dec6-Wertes
       * aus dem Datenbereich und einer Dec6-Variablen zuweisen
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DATA_READ_D6:\n"
		+ "\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tINC\tDE\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_DEC6 );
      buf.append( "\n"
		+ "\tJR\tZ,DATA_READ_D6_1\n"
		+ "\tPUSH\tHL\n"		// Variablenadresse sichern
		+ "\tPUSH\tHL\n"		// 4 Bytes auf dem Stack
		+ "\tPUSH\tHL\n"		// reservieren
		+ "\tLD\tHL,0000H\n"
		+ "\tADD\tHL,SP\n"		// Adresse auf dem Stack
      // Wert als I4 lesen und auf den Stack schreiben
		+ "\tCALL\tDATA_READ_I4_1\n"
      // I4-Wert vom Stack nach DEHL lesen
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
      // I4-Wert in Dec6 wandeln
		+ "\tCALL\tF_D6_CDEC_I4\n"
      // Dec6-Wert in Variable schreiben
		+ "\tPOP\tHL\n"			// Variablenadresse
		+ "\tJP\tD6_LD_MEM_ACCU\n"
		+ "DATA_READ_D6_1:\n"		// Dec6-Wert
		+ "\tLD\tBC,0006H\n"
		+ "\tJR\tDATA_READ_I4_3\n" );
      compiler.addLibItem( LibItem.DATA );
      compiler.addLibItem( LibItem.DATA_READ_I4 );
      compiler.addLibItem( LibItem.F_D6_CDEC_I4 );
      compiler.addLibItem( LibItem.D6_LD_MEM_ACCU );
    }
    if( compiler.usesLibItem( LibItem.DATA_READ_I4 ) ) {
      /*
       * Lesen eines Int1-, Int2- oder Int4-Wertes
       * aus dem Datenbereich und einer Long-Variablen zuweisen
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DATA_READ_I4:\n"
		+ "\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tINC\tDE\n"
		+ "DATA_READ_I4_1:\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_INT4 );
      buf.append( "\n"
		+ "\tJR\tZ,DATA_READ_I4_2\n"
		+ "\tCALL\tDATA_READ_I2_1\n"
		+ "\tRLA\n"			// CY=Vorzeichen
		+ "\tSBC\tA,A\n"		// alle Bits=CY setzen
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "DATA_READ_I4_2:\n"		// 4-Byte-Integer
		+ "\tLD\tBC,0004H\n"
		+ "DATA_READ_I4_3:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLDIR\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJR\tDATA_READ_I2_4\n" );
      compiler.addLibItem( LibItem.DATA );
      compiler.addLibItem( LibItem.DATA_READ_I2 );
    }
    if( compiler.usesLibItem( LibItem.DATA_READ_I2 ) ) {
      /*
       * Lesen eines Int1- oder Int2-Wertes
       * aus dem Datenbereich und einer Integer-Variablen zuweisen
       *
       * Nach Rueckkehr zeigt HL auf das hoeherwertige Byte
       * der Variable und A enthaelt dieses Byte.
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DATA_READ_I2:\n"
		+ "\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tINC\tDE\n"
		+ "DATA_READ_I2_1:\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,E_OUT_OF_DATA\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_INT1 );
      buf.append( "\n"
		+ "\tJR\tZ,DATA_READ_I2_2\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_INT2 );
      buf.append( "\n"
		+ "\tJP\tNZ,E_TYPE_MISMATCH\n"
		+ "\tLD\tA,(DE)\n"		// 2-Byte Integer
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tDATA_READ_I2_3\n"
		+ "DATA_READ_I2_2:\n"		// 1-Byte Integer
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tRLA\n"			// CY=Vorzeichen
		+ "\tSBC\tA,A\n"		// alle Bits=CY setzen
		+ "DATA_READ_I2_3:\n"
		+ "\tLD\t(HL),A\n"
		+ "DATA_READ_I2_4:\n"
		+ "\tLD\t(M_READ),DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.DATA );
      compiler.addLibItem( LibItem.E_OUT_OF_DATA );
      compiler.addLibItem( LibItem.E_TYPE_MISMATCH );
    }
    if( compiler.usesLibItem( LibItem.DATA_READ_S ) ) {
      /*
       * Lesen einer Zeichenkette aus dem Datenbereich
       * und einer String-Variablen zuweisen
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DATA_READ_S:\n"
		+ "\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tOR\tA\n"
		+ "\tJP\tZ,E_OUT_OF_DATA\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_STRING );
      buf.append( "\n"
		+ "\tJP\tNZ,E_TYPE_MISMATCH\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\t(HL),D\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "DATA_READ_S_1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,DATA_READ_S_1\n"
		+ "\tLD\t(M_READ),DE\n"
		+ "\tLD\tD,B\n"
		+ "\tLD\tE,C\n"
		+ "\tJP\tMFREE\n" );
      compiler.addLibItem( LibItem.DATA );
      compiler.addLibItem( LibItem.MFREE );
      compiler.addLibItem( LibItem.E_OUT_OF_DATA );
      compiler.addLibItem( LibItem.E_TYPE_MISMATCH );
    }
    /*
     * Programmcode fuer den Datentypen Decimal
     */
    DecimalLibrary.appendCodeTo( compiler );
    if( compiler.usesLibItem( LibItem.GET_ON_GO_ADDR ) ) {
      /*
       * Ermittlung der Sprungadresse bei ON GOTO/GOSUB.
       *
       * Parameter:
       *   HL: Auswahl des Sprungziels
       * Rueckgabewert:
       *   HL: Adresse des Sprungziels oder 0
       */
      buf.append( "GET_ON_GO_ADDR:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tEX\tDE,HL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,0\n"
		+ "\tLD\tC,A\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,BC\n"
		+ "\tPOP\tBC\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tH,0\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,GET_ON_GO_ADDR_1\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,GET_ON_GO_ADDR_1\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tJR\tZ,GET_ON_GO_ADDR_1\n"
		+ "\tJR\tC,GET_ON_GO_ADDR_1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tGET_ON_GO_ADDR_2\n"
		+ "GET_ON_GO_ADDR_1:\n"
		+ "\tLD\tHL,0000H\n"
		+ "GET_ON_GO_ADDR_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.LOCATE ) ) {
      /*
       * Setzen der Cursor-Position auf dem Bildschirm
       *
       * Parameter:
       *   DE: Zeile, >= 0
       *   HL: Spalte, >= 0
       */
      buf.append( "LOCATE:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tAND\t80H\n"
		+ "\tJP\tZ,XLOCATE\n"
		+ "\tJP\tE_INVALID_PARAM\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.XLOCATE );
    }
    if( compiler.usesLibItem( LibItem.PAUSE ) ) {
      /*
       * Warten auf Druecken der Leertaste
       */
      buf.append( "PAUSE:\tCALL\tXINCH\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,PAUSE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.XINCH );
    }
    if( compiler.usesLibItem( LibItem.PAUSE_N ) ) {
      /*
       * Warten
       *
       * Parameter:
       *   HL: Wartezeit in 1/10 Sekunen
       */
      buf.append( "PAUSE_N:\n"
		+ "\tBIT\t7,H\n"
		+ "\tRET\tNZ\n"
		+ "PAUSE_N1:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tHL\n" );
      buf.append_LD_DE_nn( target.get100msLoopCount() );
      buf.append( "PAUSE_N2:\n"
		+ "\tLD\tB,0\n"
		+ "PAUSE_N3:\n"
		+ "\tDJNZ\tPAUSE_N3\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,PAUSE_N2\n"
		+ "\tCALL\tXINKEY\n"
		+ "\tPOP\tHL\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,PAUSE_N1\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.XINKEY );
    }
    GraphicsLibrary.appendCodeTo( compiler );
    if( compiler.usesLibItem( LibItem.SWAP2 ) ) {
      /*
       * Vertauschen von 2 Bytes im Speicher
       *
       * Parameter:
       *   DE: Zeiger auf Bytefolge 1
       *   HL: Zeiger auf Bytefolge 2
       */
      buf.append( "SWAP2:\n"
		+ "\tLD\tB,02H\n"
		+ "\tJR\tSWAP\n" );
      compiler.addLibItem( LibItem.SWAP );
    }
    if( compiler.usesLibItem( LibItem.SWAP4 ) ) {
      /*
       * Vertauschen von 4 Bytes im Speicher
       *
       * Parameter:
       *   DE: Zeiger auf Bytefolge 1
       *   HL: Zeiger auf Bytefolge 2
       */
      buf.append( "SWAP4:\n"
		+ "\tLD\tB,04H\n"
		+ "\tJR\tSWAP\n" );
      compiler.addLibItem( LibItem.SWAP );
    }
    if( compiler.usesLibItem( LibItem.SWAP6 ) ) {
      /*
       * Vertauschen von 6 Bytes im Speicher
       *
       * Parameter:
       *   DE: Zeiger auf Bytefolge 1
       *   HL: Zeiger auf Bytefolge 2
       */
      buf.append( "SWAP6:\n"
		+ "\tLD\tB,06H\n" );
      compiler.addLibItem( LibItem.SWAP );
      // direkt weiter mit SWAP!
    }
    if( compiler.usesLibItem( LibItem.SWAP ) ) {
      /*
       * Vertauschen von Bytes im Speicher
       *
       * Parameter:
       *   B:  Anzahl der zu vertauschenden Bytes
       *   DE: Zeiger auf Bytefolge 1
       *   HL: Zeiger auf Bytefolge 2
       */
      buf.append( "SWAP:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tA,C\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tSWAP\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.CHECK_STACK ) ) {
      /*
       * Pruefen, ob der Stack ueberlaeuft
       *
       * Parameter bei CHECK_STACK_N:
       *   DE: benoetigte Stack-Tiefe
       */
      buf.append( SUB_CHECK_STACK_BEG );
      if( !options.isAppTypeSubroutine() && (stackSize > 0) ) {
	buf.append( "\tLD\tHL," );
	buf.append( BasicCompiler.TOP_LABEL );
	buf.append( '-' );
	buf.appendHex4( stackSize );
	buf.append( "+10H" );
	compiler.addLibItem( LibItem.MTOP );
	buf.append( "\n"
		+ "\tADD\tHL,DE\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,SP\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tXOUTST\n" );
	if( compiler.isLangCode( "DE" ) ) {
	  buf.append( "\tDB\t\'Stack-Ueberlauf\'\n" );
	} else {
	  buf.append( "\tDB\t\'Stack overflow\'\n" );
	}
	buf.append( "\tDB\t00H\n"
		  + "\tJP\tE_EXIT\n" );
	compiler.addLibItem( LibItem.E_EXIT );
	compiler.addLibItem( LibItem.XOUTST );
      } else {
	buf.append( SUB_CHECK_STACK_END );
      }
    }
    if( compiler.usesLibItem( LibItem.F_S_BIN_I4_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Binaerzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       *   BC:   Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_BIN_I4_I2:\n"
		+ "\tCALL\tF_S_BIN_I4\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,20H\n"
		+ "\tJP\tSKIP_LEADING_CHARS\n" );
      compiler.addLibItem( LibItem.F_S_BIN_I4 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
    }
    if( compiler.usesLibItem( LibItem.F_S_BIN_I2_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Binaerzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       *   BC: Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_BIN_I2_I2:\n"
		+ "\tCALL\tF_S_BIN_I2\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,10H\n"
		+ "\tJP\tSKIP_LEADING_CHARS\n" );
      compiler.addLibItem( LibItem.F_S_BIN_I2 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
    }
    if( compiler.usesLibItem( LibItem.F_S_OCT_I4_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Oktalzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       *   BC:   Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL:    Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_OCT_I4_I2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tF_S_OCT_I4\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,0BH\n"
		+ "\tJP\tSKIP_LEADING_CHARS\n" );
      compiler.addLibItem( LibItem.F_S_OCT_I4 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
    }
    if( compiler.usesLibItem( LibItem.F_S_OCT_I2_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Oktalzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       *   BC: Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_OCT_I2_I2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tF_S_OCT_I2\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,06H\n"
		+ "\tJP\tSKIP_LEADING_CHARS\n" );
      compiler.addLibItem( LibItem.F_S_OCT_I2 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
    }
    if( compiler.usesLibItem( LibItem.F_S_HEX_I4_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       *   BC:   Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_HEX_I4_I2:\n"
		+ "\tCALL\tF_S_HEX_I4\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,08H\n"
		+ "\tJP\tSKIP_LEADING_CHARS\n" );
      compiler.addLibItem( LibItem.F_S_HEX_I4 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
    }
    if( compiler.usesLibItem( LibItem.F_S_HEX_I2_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       *   BC: Anzahl der auszugebenden Ziffern (0=variabel)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_HEX_I2_I2:\n"
		+ "\tCALL\tF_S_HEX_I2\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tC\n"
		+ "\tJP\tZ,SKIP_LEADING_ZEROS\n"
		+ "\tLD\tA,04H\n" );
      compiler.addLibItem( LibItem.F_S_HEX_I2 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.SKIP_LEADING_ZEROS );
      compiler.addLibItem( LibItem.SKIP_LEADING_CHARS );
      // direkt weiter mit SKIP_LEADING_CHARS !!!
    }
    if( compiler.usesLibItem( LibItem.SKIP_LEADING_CHARS ) ) {
      /*
       * Die Routine schneidet von einer Zeichenkette
       * links eine gewisse Anzahl von Zeichen ab.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   A:  Ist-Laenge der Zeichenkette
       *   C:  Soll-Laenge der Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die evtl. gekuerzte Zeichenkette
       */
      buf.append( "SKIP_LEADING_CHARS:\n"
		+ "\tSUB\tC\n"
		+ "\tJP\tC,E_INVALID_PARAM\n"
		+ "SKIP_LEADING_CHARS_1:\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tSKIP_LEADING_CHARS_1\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_S_BIN_I4 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Binaerzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       * Rueckgabewert:
       *   HL:   Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_BIN_I4:\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tF_S_BIN_HL\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tF_S_BIN_HL\n"
		+ "\tJP\tTERMINATE_TMP_STR_AT_DE\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( LibItem.F_S_BIN_HL );
      compiler.addLibItem( LibItem.TERMINATE_TMP_STR_AT_DE );
    }
    if( compiler.usesLibItem( LibItem.F_S_BIN_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Binaerzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_BIN_I2:\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tCALL\tF_S_BIN_HL\n"
		+ "\tJP\tTERMINATE_TMP_STR_AT_DE\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( LibItem.F_S_BIN_HL );
      compiler.addLibItem( LibItem.TERMINATE_TMP_STR_AT_DE );
    }
    if( compiler.usesLibItem( LibItem.F_S_BIN_HL ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Binaerzahl.
       * Es wird kein terminierendes Nullbyte geschrieben!
       *
       * Parameter:
       *   HL: numerischer Wert (Ausgabe max. 16-stellig)
       *   DE: Zeiger auf den Ausgabepuffer
       * Rueckgabewert:
       *   DE: Zeiger auf das erste Zeichen hinter der Zeichenkette
       */
      buf.append( "F_S_BIN_HL:\n"
		+ "\tLD\tB,10H\n"
		+ "F_S_BIN_HL_1:\n"
		+ "\tXOR\tA\n"
		+ "\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tADC\tA,30H\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tF_S_BIN_HL_1\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.F_S_HEX_I4 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       *   DE:   Zeiger auf den Ausgabepuffer
       * Rueckgabewert:
       *   HL:    Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_HEX_I4:\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tF_S_HEX_HL\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tF_S_HEX_HL\n"
		+ "\tJP\tTERMINATE_TMP_STR_AT_DE\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( LibItem.F_S_HEX_HL );
      compiler.addLibItem( LibItem.TERMINATE_TMP_STR_AT_DE );
    }
    if( compiler.usesLibItem( LibItem.F_S_HEX_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       *   DE: Zeiger auf den Ausgabepuffer
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_HEX_I2:\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tCALL\tF_S_HEX_HL\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
      compiler.addLibItem( LibItem.F_S_HEX_HL );
      compiler.addLibItem( LibItem.TERMINATE_TMP_STR_AT_DE );
      // direkt weiter mit TERMINATE_TMP_STR_AT_DE !!!
    }
    if( compiler.usesLibItem( LibItem.TERMINATE_TMP_STR_AT_DE ) ) {
      /*
       * Die Routine schreibt ein Nullbyte auf die Speicherzelle,
       * auf die DE zeigt und liefert in HL
       * die Adresse des String-Puffers zurueck.
       *
       * Parameter:
       *   DE: Zeiger auf das erste Byte hinter der im Puffer stehenden
       *       und abzuschliessende Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "TERMINATE_TMP_STR_AT_DE:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.F_S_HEX_HL ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       * Es wird kein terminierendes Nullbyte geschrieben!
       *
       * Parameter:
       *   HL: numerischer Wert (Ausgabe max. 4-stellig)
       *   DE: Zeiger auf den Ausgabepuffer
       * Rueckgabewert:
       *   DE: Zeiger auf das erste Zeichen hinter der Zeichenkette
       */
      buf.append( "F_S_HEX_HL:\n"
		+ "\tLD\tA,H\n"
		+ "\tCALL\tF_S_HEX_HL_1\n"
		+ "\tLD\tA,L\n"
		+ "F_S_HEX_HL_1:\n"
		+ "\tPUSH\tAF\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tCALL\tF_S_HEX_HL_2\n"
		+ "\tPOP\tAF\n"
		+ "F_S_HEX_HL_2:\n"
		+ "\tAND\t0FH\n"
		+ "\tADD\tA,90H\n"
		+ "\tDAA\n"
		+ "\tADC\tA,40H\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.F_S_OCT_I4 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Oktalzahl.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       * Rueckgabewert:
       *   HL:   Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_OCT_I4:\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_TMP_STR_BUF+0CH\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tEXX\n"
		+ "\tLD\tC,0BH\n"
		+ "F_S_OCT_I4_1:\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t07H\n"
		+ "\tADD\tA,30H\n"
		+ "\tEXX\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tEXX\n"
		+ "\tLD\tB,03H\n"
		+ "F_S_OCT_I4_2:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tRR\tH\n"
		+ "\tRR\tL\n"
		+ "\tDJNZ\tF_S_OCT_I4_2\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,F_S_OCT_I4_1\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.F_S_OCT_I2 ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Oktalzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "F_S_OCT_I2:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,M_TMP_STR_BUF+08H\n"
		+ "\tLD\t(HL),00H\n"
		+ "F_S_OCT_I2_1:\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tADD\tA,30H\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tB,03H\n"
		+ "F_S_OCT_I2_2:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tDJNZ\tF_S_OCT_I2_2\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,F_S_OCT_I2_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.SKIP_LEADING_ZEROS ) ) {
      /*
       * Die Methode ueberliest fuehrende Null.
       * Besteht der String nur aus Nullen, bleibt die letzte Null stehen.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette ohne fuehrende Nullen
       */
      buf.append( "SKIP_LEADING_ZEROS:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t30H\n"
		+ "\tRET\tNZ\n"
		+ "SKIP_LEADING_ZEROS_1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,SKIP_LEADING_ZEROS_2\n"
		+ "\tCP\t30H\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tSKIP_LEADING_ZEROS_1\n"
		+ "SKIP_LEADING_ZEROS_2:\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.S_INPUT_N ) ) {
      /*
       * Lesen einer bestimmten Anzahl von Zeichen von der Tastatur
       *
       * Parameter:
       *   HL: Anzahl Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INPUT_N:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,S_INPUT_N_2\n"
		+ "\tLD\tB,L\n"
		+ "S_INPUT_N_1:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tS_INPUT_N_1\n"
		+ "S_INPUT_N_2:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.XINCH );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.S_INCHAR ) ) {
      /*
       * Warten auf einen Tastendruck und Rueckgabe der gedrueckten Taste
       * als Zeichenkette
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INCHAR:\n"
		+ "\tCALL\tXINCH\n"
		+ "\tLD\tHL,M_KEY_BUF\n"
		+ "\tJP\tF_S_CHR_X\n" );
      compiler.addLibItem( LibItem.XINCH );
      compiler.addLibItem( LibItem.F_S_CHR );
      compiler.addLibItem( LibItem.M_KEY_BUF );
    }
    if( compiler.usesLibItem( LibItem.S_INKEY ) ) {
      /*
       * Lesen der aktuell gedrueckten Taste und Rueckgabe als Zeichenkette
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INKEY:\n"
		+ "\tCALL\tXINKEY\n"
		+ "\tLD\tHL,M_KEY_BUF\n"
		+ "\tJP\tF_S_CHR_X\n" );
      compiler.addLibItem( LibItem.XINKEY );
      compiler.addLibItem( LibItem.F_S_CHR );
      compiler.addLibItem( LibItem.M_KEY_BUF );
    }
    if( compiler.usesLibItem( LibItem.F_I2_CINT_I4 ) ) {
      /*
       * Umwandlung eines Long-Wertes in einen Integer-Wert
       *
       * Die Funktion meldet einen Fehler,
       * wenn der Betrag des uebergebenen Wertes zu gross ist.
       *
       * Parameter:
       *   DEHL: Long-Wert
       * Rueckgabe:
       *   HL:   Integer-Wert
       */
      buf.append( "F_I2_CINT_I4:\n"
		+ "\tCALL\tCHECK_DEHL_FOR_I2\n"
		+ "\tRET\tNC\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n" );
      compiler.addLibItem( LibItem.CHECK_DEHL_FOR_I2 );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.F_I2_IS_TARGET_I2 ) ) {
      /*
       * Pruefen, ob der Wert in HL dem aktuellen Zielsystem entspricht.
       * Dabei werden auch uebergeordnete Zielsysteme erkannt.
       *
       * Parameter:
       *   HL: Wert des zu testendes Zielsystems
       * Rueckgabe:
       *   HL: 0:     Wert entspricht nicht dem Zielsystem
       *       FFFFh: Wert entspricht dem Zielsystem
       */
      buf.append( "F_I2_IS_TARGET_I2:\n"
		+ "\tEX\tDE,HL\n" );
      int[] targetIDs = compiler.getTarget().getTargetIDs();
      if( targetIDs != null ) {
	for( int targetID : targetIDs ) {
	  buf.append_LD_HL_nn( targetID );
	  buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tZ,F_I2_IS_TARGET_I2_1\n" );
	}
      }
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_I2_IS_TARGET_I2_1:\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.F_I2_JOY_I2 ) ) {
      buf.append( "F_I2_JOY_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n" );
      if( target.supportsXJOY() ) {
	buf.append( "\tJP\tXJOY\n" );
	compiler.addLibItem( LibItem.XJOY );
      } else {
	buf.append( "\tHL,0000H\n"
		+ "\tRET\n" );
      }
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_I2_RND_I2 ) ) {
      /*
       * Ermitteln einer Zufallszahl
       *
       * Die RND-Funktion verknuepft mehrere Algorithmen miteinander,
       * um die jeweilgen Nachteile gegenseitig zu kompensieren:
       *   1. Lesen der Programmcodebytes
       *   2. XOR-Verknuepfung mit dem Refresh-Register
       *   3. XOR-Verknuepfung mit dem aktuellen Wert eines
       *      rueckgekoppelten Schieberegister-Pseudozufallsgenerators,
       *      der bei jedem Zurueckstellen von Algorithmus 1
       *      um eins weitergestellt wird.
       *
       * Parameter:
       *   HL: maximaler Wert (groesser 0)
       * Rueckgabe:
       *   HL: Zufallszahl zwischen 0 und dem maximalen Wert (exklusive)
       */
      buf.append( "F_I2_RND_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_INVALID_PARAM\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tDE,(M_RNDA)\n"		// aktuelle Leseadresse
		+ "\tLD\tHL,XEXIT\n"		// XEXIT erreicht?
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,F_I2_RND_I2_2\n" );
      // auf Anfang zurueckstellen
      buf.append_LD_DE_xx( BasicCompiler.START_LABEL );
      buf.append( "\tLD\tA,(M_RNDX)\n"		// rueckgekoppelten
		+ "\tLD\tB,A\n"			// Schieberegister-
		+ "\tAND\t8EH\n"		// Pseudozufallsgenerator
		+ "\tJP\tPE,F_I2_RND_I2_1\n"	// (M_RNDX) weiterstellen
		+ "\tCCF\n"
		+ "F_I2_RND_I2_1:\n"
		+ "\tRL\tB\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(M_RNDX),A\n"
		+ "F_I2_RND_I2_2:\n"		// 2 Programmcodebytes lesen
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,(M_RNDX)\n"
		+ "\tXOR\t(HL)\n"
		+ "\tLD\tE,A\n"
		+ "\tINC\tHL\n"			// Adresse inkrementieren
		+ "\tLD\t(M_RNDA),HL\n"
		+ "\tLD\tA,R\n"
		+ "\tXOR\t(HL)\n"
		+ "\tJP\tP,F_I2_RND_I2_3\n"
		+ "\tCPL\n"
		+ "F_I2_RND_I2_3:\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,E\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tI2_DIV_I2_I2_2\n" );
      compiler.addLibItem( LibItem.I2_DIV_I2_I2 );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_I2_SQR_I2 ) ) {
      /*
       * Ermitteln des ganzzahligen Anteils der Quadratwurzel
       * einer 16-Bit-Zahl
       *
       * Parameter:
       *   HL: Eingangswert
       * Rueckgabe:
       *   HL: ganzzahliger Anteil der Quadratwurzel
       */
      buf.append( "F_I2_SQR_I2:\n"
		+ "\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tB,01H\n"
		+ "\tLD\tDE,0001H\n"
		+ "F_I2_SQR_I2_1:\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tC,F_I2_SQR_I2_2\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,F_I2_SQR_I2_3\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tB\n"
		+ "\tJR\tF_I2_SQR_I2_1\n"
		+ "F_I2_SQR_I2_2:\n"
		+ "\tDEC\tB\n"
		+ "F_I2_SQR_I2_3:\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_I4_SQR_I4 ) ) {
      /*
       * Ermitteln des ganzzahligen Anteils der Quadratwurzel
       * einer 32-Bit-Zahl nach dem Algorithmus:
       *   Startwert: X1=X/2
       *   Iteration: X2=X1+((X2-X1)/2)
       *
       * Parameter:
       *   DEHL: Eingangswert
       * Rueckgabe:
       *   DEHL: ganzzahliger Anteil der Quadratwurzel
       */
      buf.append( "F_I4_SQR_I4:\n"
		+ "\tBIT\t7,D\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,F_I4_SQR_I4_1\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"			// SQR(0)=0
		+ "\tLD\tA,L\n"
		+ "\tDEC\tA\n"
		+ "\tRET\tZ\n"			// SQR(1)=1
		+ "F_I4_SQR_I4_1:\n"
		+ "\tLD\t(M_OP1),HL\n"
		+ "\tLD\t(M_OP1+2),DE\n"
		+ "\tCALL\tF_I4_SQR_I4_6\n"	// Startwert X1=X/2
		+ "F_I4_SQR_I4_2:\n"
      // X1 -> Stack
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
      // DEHL -> DE'DE, M_OP1 -> HL'HL
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_OP1+2)\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,(M_OP1)\n"
		+ "\tPOP\tDE\n"
      // X2=X/X1, DE'DE = HL'HL / DE'DE
		+ "\tCALL\tI4_DIV_UTIL_1\n"
		+ "\tPUSH\tDE\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"			// DEHL: X1
      // Wenn X1 > X2, dann X1 und X2 tauschen
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tJR\tNC,F_I4_SQR_I4_3\n"
		+ "\tEXX\n"
		+ "F_I4_SQR_I4_3:\n"
      // DEHL=X2-X1 wobei X2 immer >= X1, DE'HL'=X1
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tI4_SUB_I4_I4\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tEXX\n"
      // Wenn Diff=0 oder Diff=1, dann Ergebnis=X1
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,F_I4_SQR_I4_4\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,F_I4_SQR_I4_5\n"	// X1=X2
		+ "\tLD\tA,L\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,F_I4_SQR_I4_5\n"	// X1=X2-1
		+ "F_I4_SQR_I4_4:\n"
      // X1=X1+(Diff/2)
		+ "\tCALL\tF_I4_SQR_I4_6\n"
		+ "\tCALL\tI4_ADD_I4_I4\n"
		+ "\tJR\tF_I4_SQR_I4_2\n"
		+ "F_I4_SQR_I4_5:\n"
		+ "\tEXX\n"
		+ "\tRET\n"
      // DEHL = DEHL / 2
		+ "F_I4_SQR_I4_6;\n"
		+ "\tSRL\tD\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tH\n"
		+ "\tSRL\tL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
      compiler.addLibItem( LibItem.I4_ADD_I4_I4 );
      compiler.addLibItem( LibItem.I4_SUB_I4_I4 );
      compiler.addLibItem( LibItem.I4_DIV_UTIL );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.M_OP1_4 );
    }
    if( compiler.usesLibItem( LibItem.F_I2_VAL_S_I2 ) ) {
      /*
       * Lesen einer Integer-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   DE: Zahlenbasis (2, 10 oder 16)
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I2_VAL_S_I2:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,F_I2_VAL_BIN_S\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tZ,F_I2_VAL_DEC_S\n"
		+ "\tCP\t10H\n"
		+ "\tJP\tZ,F_I2_VAL_HEX_S\n"
		+ "\tJP\tE_INVALID_PARAM\n" );
      compiler.addLibItem( LibItem.F_I2_VAL_BIN_S );
      compiler.addLibItem( LibItem.F_I2_VAL_DEC_S );
      compiler.addLibItem( LibItem.F_I2_VAL_HEX_S );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_I4_VAL_S_I2 ) ) {
      /*
       * Lesen einer Long-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL:   Zeiger auf die Zeichenkette
       *   DE:   Zahlenbasis (2, 10 oder 16)
       * Rueckgabe:
       *   DEHL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I4_VAL_S_I2:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,F_I4_VAL_BIN_S\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tZ,F_I4_VAL_DEC_S\n"
		+ "\tCP\t10H\n"
		+ "\tJP\tZ,F_I4_VAL_HEX_S\n"
		+ "\tJP\tE_INVALID_PARAM\n" );
      compiler.addLibItem( LibItem.F_I4_VAL_BIN_S );
      compiler.addLibItem( LibItem.F_I4_VAL_DEC_S );
      compiler.addLibItem( LibItem.F_I4_VAL_HEX_S );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.F_I2_VAL_BIN_S ) ) {
      /*
       * Lesen einer Binaerzahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I2_VAL_BIN_S:\n" );
      if( compiler.usesLibItem( LibItem.F_I4_VAL_BIN_S ) ) {
	buf.append( "CALL\tF_I4_VAL_BIN_S\n"
		+ "\tCALL\tCHECK_DEHL_FOR_I2\n"
		+ "\tRET\tNC\n"
		+ "\tJP\tI2_ERR_SET_NUMERIC_OVERFLOW\n" );
	compiler.addLibItem( LibItem.CHECK_DEHL_FOR_I2 );
      } else {
	buf.append( "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tLD\tHL,0000H\n"
		+ "F_I2_VAL_BIN_S_1:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t02H\n"
		+ "\tJP\tNC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tADD\tHL,HL\n"
		+ "\tJP\tC,I2_ERR_SET_NUMERIC_OVERFLOW\n"
		+ "\tOR\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tJR\tF_I2_VAL_BIN_S_1\n" );
	compiler.addLibItem( LibItem.I2_VAL_UTIL );
      }
      compiler.addLibItem( LibItem.I2_ERR );
    }
    if( compiler.usesLibItem( LibItem.F_I4_VAL_BIN_S ) ) {
      /*
       * Lesen einer Binaerzahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL:   Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   DEHL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I4_VAL_BIN_S:\n" );
      buf.append( "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tCALL\tLD_HL2HL_NULL\n"
		+ "F_I4_VAL_BIN_S_1:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t02H\n"
		+ "\tJP\tNC,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tJP\tNZ,I4_ERR_SET_NUMERIC_OVERFLOW\n"
		+ "\tOR\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tJR\tF_I4_VAL_BIN_S_1\n" );
      compiler.addLibItem( LibItem.I4_VAL_UTIL );
      compiler.addLibItem( LibItem.I4_ERR );
      compiler.addLibItem( LibItem.LD_HL2HL_NULL );
    }
    if( compiler.usesLibItem( LibItem.F_I2_VAL_DEC_S ) ) {
      /*
       * Lesen einer Integer-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I2_VAL_DEC_S:\n" );
      if( compiler.usesLibItem( LibItem.F_I4_VAL_DEC_S ) ) {
	buf.append( "\tCALL\tF_I4_VAL_DEC_S\n"
		+ "\tCALL\tCHECK_DEHL_FOR_I2\n"
		+ "\tRET\tNC\n"
		+ "\tJP\tI2_ERR_SET_NUMERIC_OVERFLOW\n" );
	compiler.addLibItem( LibItem.CHECK_DEHL_FOR_I2 );
      } else {
	buf.append( "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tCP\t2BH\n"				// Plus
		+ "\tJR\tZ,F_I2_VAL_DEC_S_1\n"
		+ "\tCP\t2DH\n"				// Minus
		+ "\tJR\tNZ,F_I2_VAL_DEC_S_2\n"
		+ "\tCALL\tF_I2_VAL_DEC_S_1\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tRET\n"
		+ "F_I2_VAL_DEC_S_1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "F_I2_VAL_DEC_S_2:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tNC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tCALL\tI2_APPEND_DIGIT_TO_HL\n"
		+ "\tJP\tC,I2_ERR_SET_NUMERIC_OVERFLOW\n"
	// naechstes Zeichen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tJR\tF_I2_VAL_DEC_S_2\n" );
	compiler.addLibItem( LibItem.I2_VAL_UTIL );
	compiler.addLibItem( LibItem.I2_APPEND_DIGIT_TO_HL );
	compiler.addLibItem( LibItem.I2_NEG_HL );
      }
      compiler.addLibItem( LibItem.I2_ERR );
    }
    if( compiler.usesLibItem( LibItem.F_I4_VAL_DEC_S ) ) {
      /*
       * Lesen einer Long-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL:   Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   DEHL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I4_VAL_DEC_S:\n" );
      buf.append( "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tCALL\tLD_HL2HL_NULL\n"
		+ "\tCP\t2BH\n"				// Plus
		+ "\tJR\tZ,F_I4_VAL_DEC_S_1\n"
		+ "\tCP\t2DH\n"				// Minus
		+ "\tJR\tNZ,F_I4_VAL_DEC_S_2\n"
		+ "\tCALL\tF_I4_VAL_DEC_S_1\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tI4_NEG_DEHL\n"
		+ "\tRET\n"
		+ "F_I4_VAL_DEC_S_1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "F_I4_VAL_DEC_S_2:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tNC,I4_ERR_SET_INVALID_CHARS\n"
      // HL'HL = HL'HL * 10
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADD\tHL,BC\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,BC\n"
		+ "\tEXX\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tADC\tA,00H\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tADC\tA,00H\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tADC\tA,00H\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tJP\tM,I4_ERR_SET_NUMERIC_OVERFLOW\n"
      // naechstes Zeichen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tJR\tF_I4_VAL_DEC_S_2\n" );
      compiler.addLibItem( LibItem.I4_VAL_UTIL );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
      compiler.addLibItem( LibItem.LD_HL2HL_NULL );
    }
    if( compiler.usesLibItem( LibItem.F_I2_VAL_HEX_S ) ) {
      /*
       * Lesen einer Integer-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I2_VAL_HEX_S:\n" );
      if( compiler.usesLibItem( LibItem.F_I4_VAL_HEX_S ) ) {
	buf.append( "\tCALL\tF_I4_VAL_HEX_S\n"
		+ "\tCALL\tCHECK_DEHL_FOR_I2\n"
		+ "\tRET\tNC\n"
		+ "\tJP\tI2_ERR_SET_NUMERIC_OVERFLOW\n" );
	compiler.addLibItem( LibItem.CHECK_DEHL_FOR_I2 );
      } else {
	buf.append( "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tLD\tHL,0000H\n"
		+ "F_I2_VAL_HEX_S_1:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tC,F_I2_VAL_HEX_S_2\n"
		+ "\tCP\t17H\n"
		+ "\tJP\tNC,I2_ERR_SET_INVALID_CHARS\n"
		+ "\tSUB\t07H\n"
		+ "F_I2_VAL_HEX_S_2:\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0F0H\n"
		+ "\tJP\tNZ,I2_ERR_SET_NUMERIC_OVERFLOW\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tB\n"
		+ "\tLD\tL,A\n"
	// naechstes Zeichen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I2_VAL_END\n"
		+ "\tJR\tF_I2_VAL_HEX_S_1\n" );
	compiler.addLibItem( LibItem.I2_VAL_UTIL );
      }
      compiler.addLibItem( LibItem.I2_ERR );
    }
    if( compiler.usesLibItem( LibItem.F_I4_VAL_HEX_S ) ) {
      /*
       * Lesen einer Long-Zahl aus einer Zeichenkette,
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL:   Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   DEHL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_I4_VAL_HEX_S:\n"
		+ "\tCALL\tVAL_SKIP_SPACES\n"
		+ "\tLD\tHL,0000H\n"
		+ "F_I4_VAL_HEX_S_1:\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tC,F_I4_VAL_HEX_S_2\n"
		+ "\tCP\t17H\n"
		+ "\tJP\tNC,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tSUB\t07H\n"
		+ "F_I4_VAL_HEX_S_2:\n"
		+ "\tLD\tB,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tAND\t0F0H\n"
		+ "\tJP\tNZ,I4_ERR_SET_NUMERIC_OVERFLOW\n"
		+ "\tLD\tB,04H\n"
		+ "F_I4_VAL_HEX_S_3:\n"
		+ "\tEXX\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tDJNZ\tF_I4_VAL_HEX_S_3\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tB\n"
		+ "\tLD\tL,A\n"
      // naechstes Zeichen
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I4_VAL_END\n"
		+ "\tJR\tF_I4_VAL_HEX_S_1\n" );
      compiler.addLibItem( LibItem.I4_VAL_UTIL );
      compiler.addLibItem( LibItem.I4_ERR );
    }
    if( compiler.usesLibItem( LibItem.I2_VAL_UTIL )
	|| compiler.usesLibItem( LibItem.I4_VAL_UTIL ) )
    {
      /*
       * Ueberlesen von Leerzeichen,
       * Wird dabei das Ende der Zeichenkette erreicht,
       * wird die Fehlervariable gesetzt.
       *
       * Parameter:
       *   HL:   Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   A:  erstes Zeichen, welches kein Leerzeichen ist
       *   DE: Zeiger auf das erste Zeichen hinter dem in A gelesene
       */
      buf.append( "VAL_SKIP_SPACES:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,VAL_SKIP_SPACES\n"
		+ "\tOR\tA\n" );		// CY=0
      if( compiler.usesLibItem( LibItem.I4_VAL_UTIL ) ) {
	buf.append( "\tJP\tZ,I4_ERR_SET_INVALID_CHARS\n" );
	compiler.addLibItem( LibItem.I4_ERR );
      } else {
	buf.append( "\tJP\tZ,I2_ERR_SET_INVALID_CHARS\n" );
	compiler.addLibItem( LibItem.I2_ERR );
      }
      buf.append( "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      /*
       * Zeilenende pruefen
       *
       * Parameter:
       *   A:    aktuelles Zeichen
       *   DE:   Zeiger auf das naechste Zeichen
       * Rueckgabe:
       *   DEHL bzw. HL: gelesener Wert
       */
      if( compiler.usesLibItem( LibItem.I4_VAL_UTIL ) ) {
	buf.append( "I2_VAL_END:\n"
		+ "I4_VAL_END:\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,I4_HL2HL_TO_DEHL\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tNZ,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tI4_VAL_END\n" );
	compiler.addLibItem( LibItem.I4_HL2HL_TO_DEHL );
	compiler.addLibItem( LibItem.I4_ERR );
      } else {
	buf.append( "I2_VAL_END:\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJP\tNZ,I4_ERR_SET_INVALID_CHARS\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tI2_VAL_END\n" );
	compiler.addLibItem( LibItem.I2_ERR );
      }
    }
    if( compiler.usesLibItem( LibItem.F_I4_HIWORD_I4 ) ) {
      /*
       * Obere 16 Bit einer 32-Bit-Zahl als 32-Bit-Zahl liefern
       *
       * Parameter:
       *   DEHL: uebergebener Wert
       * Rueckgabewert:
       *   DEHL: Rueckgabewert
       */
      buf.append( "F_I4_HIWORD_I4:\n"
		+ "\tEX\tDE,HL\n" );
      compiler.addLibItem( LibItem.F_I4_LOWORD_I4 );
      // direkt weiter mit F_I4_LOWORD
    }
    if( compiler.usesLibItem( LibItem.F_I4_LOWORD_I4 ) ) {
      /*
       * Untere 16 Bit einer 32-Bit-Zahl als 32-Bit-Zahl liefern,
       * d.h. die oberen 16 Bit auf 0 setzen
       *
       * Parameter:
       *   DEHL: uebergebener Wert
       * Rueckgabewert:
       *   DEHL: Rueckgabewert
       */
      buf.append( "F_I4_LOWORD_I4:\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.NEXT_N ) ) {
      /*
       * NEXT-Befehl mit beliebiger Schrittweite
       *
       * Parameter:
       *   BC: Schrittweite
       *   DE: Endwert
       *   HL: Zeiger auf die Laufvariable
       * Rueckgabewert:
       *   CY=1: Schleife erneut durchlaufen
       */
      buf.append( "NEXT_N:\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tZ,NEXT_UP\n"
		+ "\tJR\tNEXT_DOWN\n" );
      compiler.addLibItem( LibItem.NEXT_UP );
      compiler.addLibItem( LibItem.NEXT_DOWN );
    }
    if( compiler.usesLibItem( LibItem.NEXT_DEC ) ) {
      /*
       * NEXT-Befehl mit Schrittweite -1
       *
       * Parameter:
       *   DE: Endwert
       *   HL: Zeiger auf die Laufvariable
       * Rueckgabewert:
       *   CY=1: Schleife erneut durchlaufen
       */
      buf.append( "NEXT_DEC:\n"
		+ "\tLD\tBC,0FFFFH\n" );
      compiler.addLibItem( LibItem.NEXT_DOWN );
      // direkt weiter mit NEXT_DOWN
    }
    if( compiler.usesLibItem( LibItem.NEXT_DOWN ) ) {
      /*
       * NEXT-Befehl mit negativer Schrittweite
       *
       * Parameter:
       *   BC: Schrittweite
       *   DE: Endwert
       *   HL: Zeiger auf die Laufvariable
       * Rueckgabewert:
       *   CY=1: Schleife erneut durchlaufen
       */
      buf.append( "NEXT_DOWN:\n"
		+ "\tCALL\tNEXT_UP\n"
		+ "\tRET\tZ\n"
		+ "\tCCF\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.NEXT_UP );
    }
    if( compiler.usesLibItem( LibItem.NEXT_INC ) ) {
      /*
       * NEXT-Befehl mit Schrittweite 1
       *
       * Parameter:
       *   DE: Endwert
       *   HL: Zeiger auf die Laufvariable
       * Rueckgabewert:
       *   CY=1: Schleife erneut durchlaufen
       */
      buf.append( "NEXT_INC:\n"
		+ "\tLD\tBC,0001H\n" );
      compiler.addLibItem( LibItem.NEXT_UP );
      // direkt weiter mit NEXT_UP
    }
    if( compiler.usesLibItem( LibItem.NEXT_UP ) ) {
      /*
       * NEXT-Befehl mit positiver Schrittweite
       *
       * Parameter:
       *   BC: Schrittweite
       *   DE: Endwert
       *   HL: Zeiger auf die Laufvariable
       * Rueckgabewert:
       *   CY=1: Schleife erneut durchlaufen
       */
      buf.append( "NEXT_UP:\n"
		+ "\tLD\tA,C\n"
		+ "\tADD\tA,(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tC,A\n"
		+ "\tADC\tA,B\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tH,A\n"
      /*
       * Pruefung auf Ueberlauf:
       *   B:  alter H-Wert der Schrittweite
       *   C:  alter H-Wert der Laufvariable
       *   DE: Endwert
       *   HL: neuer Wert der Laufvariable
       */
		+ "\tLD\tA,C\n"
		+ "\tXOR\tB\n"
		+ "\tJP\tM,NEXT_UP_1\n"
		+ "\tLD\tA,B\n"
		+ "\tXOR\tH\n"
		+ "\tJP\tM,E_NUMERIC_OVERFLOW\n"
		+ "NEXT_UP_1:\n"
      // Vergleich mit Endwert
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tRET\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.ARRAY2_ELEM_ADDR ) ) {
      /*
       * Adresse eines Elements in einem Array berechnen
       * dessen einzelne Felder 2 Bytes gross sind
       *
       * Parameter:
       *   HL: Index
       *   DE: Basisadresse der Variablen
       *   BC: Array-Groesse + 1
       * Rueckgabewert:
       *   HL: Adresse des Elements im Array
       */
      buf.append( "ARRAY2_ELEM_ADDR:\n" );
      if( options.getCheckBounds() ) {
	buf.append( "\tCALL\tCHECK_ARRAY_IDX\n" );
      }
      buf.append( "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CHECK_ARRAY_IDX );
    }
    if( compiler.usesLibItem( LibItem.ARRAY4_ELEM_ADDR ) ) {
      /*
       * Adresse eines Elements in einem Array berechnen
       * dessen einzelne Felder 4 Bytes gross sind
       *
       * Parameter:
       *   HL: Index
       *   DE: Basisadresse der Variablen
       *   BC: Array-Groesse + 1
       * Rueckgabewert:
       *   HL: Adresse des Elements im Array
       */
      buf.append( "ARRAY4_ELEM_ADDR:\n" );
      if( options.getCheckBounds() ) {
	buf.append( "\tCALL\tCHECK_ARRAY_IDX\n" );
      }
      buf.append( "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CHECK_ARRAY_IDX );
    }
    if( compiler.usesLibItem( LibItem.ARRAY6_ELEM_ADDR ) ) {
      /*
       * Adresse eines Elements in einem Array berechnen
       * dessen einzelne Felder 6 Bytes gross sind
       *
       * Parameter:
       *   HL: Index
       *   DE: Basisadresse der Variablen
       *   BC: Array-Groesse + 1
       * Rueckgabewert:
       *   HL: Adresse des Elements im Array
       */
      buf.append( "ARRAY6_ELEM_ADDR:\n" );
      if( options.getCheckBounds() ) {
	buf.append( "\tCALL\tCHECK_ARRAY_IDX\n" );
      }
      buf.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CHECK_ARRAY_IDX );
    }
    if( compiler.usesLibItem( LibItem.CHECK_ARRAY_IDX ) ) {
      /*
       * Index einer Feldvariablen pruefen
       * A und DE werden nicht veraendert.
       *
       * Parameter:
       *   HL: Index
       *   BC: Groesse der Dimension + 1
       * Rueckgabewert:
       *   HL: Index
       */
      buf.append( "CHECK_ARRAY_IDX:\n"
		+ "\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_IDX_OUT_OF_RANGE\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJP\tNC,E_IDX_OUT_OF_RANGE\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_IDX_OUT_OF_RANGE );
    }
    if( compiler.usesLibItem( LibItem.I4_AND_I4_I4 ) ) {
      /*
       * Bitweises UND (32 Bit)
       *
       * Parameter:
       *   DE'HL', DEHL: Operanden
       * Rueckgabe:
       *   DEHL:  Ergebnis
       */
      buf.append( "I4_AND_I4_I4:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
		+ "\tAND\tD\n"
		+ "\tLD\tD,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,E\n"
		+ "\tEXX\n"
		+ "\tAND\tE\n"
		+ "\tLD\tE,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tAND\tH\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tEXX\n"
		+ "\tAND\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_OR_I4_I4 ) ) {
      /*
       * Bitweises ODER (32 Bit)
       *
       * Parameter:
       *   DE'HL', DEHL: Operanden
       * Rueckgabe:
       *   DEHL:  Ergebnis
       */
      buf.append( "I4_OR_I4_I4:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
		+ "\tOR\tD\n"
		+ "\tLD\tD,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,E\n"
		+ "\tEXX\n"
		+ "\tOR\tE\n"
		+ "\tLD\tE,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tOR\tH\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tEXX\n"
		+ "\tOR\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_XOR_I4_I4 ) ) {
      /*
       * Bitweises Exklusiv-ODER (32 Bit)
       *
       * Parameter:
       *   DE'HL', DEHL: Operanden
       * Rueckgabe:
       *   DEHL:  Ergebnis
       */
      buf.append( "I4_XOR_I4_I4:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
		+ "\tXOR\tD\n"
		+ "\tLD\tD,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,E\n"
		+ "\tEXX\n"
		+ "\tXOR\tE\n"
		+ "\tLD\tE,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tXOR\tH\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tEXX\n"
		+ "\tXOR\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_XOR_I2_I2 );
    }
    if( compiler.usesLibItem( LibItem.I4_NOT_I4 ) ) {
      /*
       * Bitweises NICHT (32 Bit)
       *
       * Parameter:
       *   DEHL: Operand
       * Rueckgabe:
       *   DEHL: Ergebnis
       */
      buf.append( "I4_NOT_I4:\n"
		+ "\tCALL\tI2_NOT_I2\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tI2_NOT_I2\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_NOT_I2 );
    }
    if( compiler.usesLibItem( LibItem.I4_SUB_I4_I4 ) ) {
      /*
       * Subtraktion: DEHL = DE'HL' - DEHL
       */
      buf.append( "I4_SUB_I4_I4:\n"
		+ "\tCALL\tI4_NEG_DEHL\n" );
      compiler.addLibItem( LibItem.I4_ADD_I4_I4 );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
      // direkt weiter mit I4_ADD_I4_I4 !!!
    }
    if( compiler.usesLibItem( LibItem.I4_ADD_I4_I4 ) ) {
      /*
       * Addition: DEHL = DE'HL' + DEHL
       */
      buf.append( "I4_ADD_I4_I4:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tEXX\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tADC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,E\n"
		+ "\tEXX\n"
		+ "\tADC\tA,E\n"
		+ "\tLD\tE,A\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
		+ "\tLD\tB,A\n"			// B: Vorzeichen Op1
		+ "\tLD\tC,D\n"			// C: Vorzeichen Op2
		+ "\tADC\tA,D\n"
		+ "\tLD\tD,A\n"
      // Ueberlauf pruefen
		+ "\tLD\tA,C\n"
		+ "\tXOR\tB\n"
		+ "\tRET\tM\n"
		+ "\tLD\tA,D\n"
		+ "\tXOR\tB\n"
		+ "\tRET\tP\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n" );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I4_MUL_I4_I4 ) ) {
      /*
       * Multiplikation: DEHL = DE'HL' * DEHL
       *
       * Wenn beide Operanden groesser 16 Bit sind,
       * kommt es zu einem Ueberlauf,
       * da das Ergebnis nur max. 32 Bit gross sein darf.
       * Umgekehrt heisst das, dass mindestens ein Operand
       * nur 16 Bit gross sein kann.
       * Die eigentliche Multiplikation wird deshalb
       * als 32x16 Bit ausgefuehrt.
       */
      buf.append( "I4_MUL_I4_I4:\n"
		+ "\tLD\tA,D\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tI4_ABS_DEHL\n"
		+ "\tEXX\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tXOR\tD\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tI4_ABS_DEHL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,I4_MUL_I4_I4_1\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJP\tNZ,E_NUMERIC_OVERFLOW\n"
		+ "I4_MUL_I4_I4_1:\n"
      // vorzeichenlose Multiplikation HL'HL = HL * DE'HL'
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
      // vorzeichenlose Multiplikation HL'HL = BC * DE'DE
		+ "\tOR\tA\n"			// CY=0
		+ "\tSBC\tHL,HL\n"		// HL'HL=0
		+ "\tEXX\n"
		+ "\tSBC\tHL,HL\n"		// HL'HL=0
		+ "\tLD\tA,B\n"
		+ "\tLD\tB,16\n"
		+ "I4_MUL_I4_I4_2:\n"
		+ "\tEXX\n"
		+ "\tSRA\tB\n"
		+ "\tRR\tC\n"			// Bit 0 -> CY
		+ "\tJR\tNC,I4_MUL_I4_I4_3\n"
		+ "\tADD\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,DE\n"
		+ "\tJP\tM,E_NUMERIC_OVERFLOW\n"
		+ "\tEXX\n"
		+ "I4_MUL_I4_I4_3:\n"
		+ "\tSLA\tE\n"
		+ "\tRL\tD\n"
		+ "\tEXX\n"
		+ "\tRL\tE\n"
		+ "\tRL\tD\n"
		+ "\tDJNZ\tI4_MUL_I4_I4_2\n"
		+ "\tEXX\n"
      // HL'HL -> DEHL und Vorzeichen anpassen
		+ "\tCALL\tI4_HL2HL_TO_DEHL\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,I4_NEG_DEHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I4_ABS_DEHL );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
      compiler.addLibItem( LibItem.I4_HL2HL_TO_DEHL );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I4_MOD_I4_I4 ) ) {
      /*
       * Modulo: DEHL = DE'HL' / DEHL
       */
      buf.append( "I4_MOD_I4_I4:\n"
		+ "\tCALL\tI4_DIV_UTIL\n"
		+ "\tCALL\tI4_HL2HL_TO_DEHL\n"
		+ "\tRET\tP\n"
		+ "\tJP\tI4_NEG_DEHL\n" );
      compiler.addLibItem( LibItem.I4_DIV_UTIL );
      compiler.addLibItem( LibItem.I4_HL2HL_TO_DEHL );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I4_DIV_I4_I4 ) ) {
      /*
       * Division: DEHL = DE'HL' / DEHL
       */
      buf.append( "I4_DIV_I4_I4:\n"
		+ "\tCALL\tI4_DIV_UTIL\n"
		+ "\tPUSH\tDE\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\tP\n"
		+ "\tJP\tI4_NEG_DEHL\n" );
      compiler.addLibItem( LibItem.I4_DIV_UTIL );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I4_DIV_UTIL ) ) {
      /*
       * Division: DE'DE = DE'HL' / DEHL, Rest in HL'HL
       *
       * Parameter:
       *   DE'HL': Dividend (vorzeichenbehaftet)
       *   DEHL:   Divisor (vorzeichenbehaftet)
       * Rueckgabe:
       *   S-Flag: Vorzeichen des Ergebnisses
       *   DE'DE:  Ergebis (vorzeichenlos)
       *   HL'HL:  Rest (vorzeichenlos)
       */
      buf.append( "I4_DIV_UTIL:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_DIV_BY_0\n"
		+ "\tLD\tA,D\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tI4_ABS_DEHL\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tXOR\tD\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCALL\tI4_ABS_DEHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tDE\n"
      // vorzeichenlose Division DE'DE = HL'HL / DE'DE, Rest in HL'HL
		+ "I4_DIV_UTIL_1:\n"
		+ "\tLD\tA,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tB,20H\n"
		+ "\tOR\tA\n"			// CY=0
		+ "I4_DIV_UTIL_2:\n"
		+ "\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tEXX\n"
		+ "\tRL\tC\n"
		+ "\tRL\tB\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,HL\n"
		+ "\tEXX\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,I4_DIV_UTIL_3\n"
      /*
       * Subtraktion rueckgaengig machen,
       * dabei wird CY wegen Ueberlauf immer gesetzt
       */
		+ "\tADD\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,DE\n"
		+ "\tEXX\n"
		+ "I4_DIV_UTIL_3:\n"
		+ "\tDJNZ\tI4_DIV_UTIL_2\n"
		+ "\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tCPL\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tLD\tE,A\n"
		+ "\tEXX\n"
		+ "\tRL\tC\n"
		+ "\tRL\tB\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,B\n"
		+ "\tCPL\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\tD,A\n"
		+ "\tEXX\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tOR\tA\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I4_ABS_DEHL );
      compiler.addLibItem( LibItem.E_DIV_BY_0 );
    }
    if( compiler.usesLibItem( LibItem.I4_SHL_I4_I2 ) ) {
      /*
       * links schieben: DEHL = DE'HL' << DEHL
       */
      buf.append( "I4_SHL_I4_I2:\n"
		+ "\tCALL\tI4_SHX_UTIL\n"
		+ "I4_SHL_I4_I2_1:\n"
		+ "\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tRL\tE\n"
		+ "\tRL\tD\n"
		+ "\tDJNZ\tI4_SHL_I4_I2_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I4_SHX_UTIL );
    }
    if( compiler.usesLibItem( LibItem.I4_SHR_I4_I2 ) ) {
      /*
       * rechts schieben: DEHL = DE'HL' >> DEHL
       */
      buf.append( "I4_SHR_I4_I2:\n"
		+ "\tCALL\tI4_SHX_UTIL\n"
		+ "\tRET\tZ\n"
		+ "I4_SHR_I4_I2_1:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tRR\tH\n"
		+ "\tRR\tL\n"
		+ "\tDJNZ\tI4_SHR_I4_I2_1\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I4_SHX_UTIL );
    }
    if( compiler.usesLibItem( LibItem.I4_SHX_UTIL ) ) {
      /*
       * Parameter fuer I4-Schiebeoperation pruefen und aufbereiten
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   DEHL:   Operand 1
       *   B:      Operand 2
       *   Z=1:    keine Schiebeoperation mehr notwendig
       */
      buf.append( "I4_SHX_UTIL:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tOR\tE\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,I4_SHX_UTIL_1\n"
		+ "\tLD\tB,L\n"
		+ "\tEXX\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"			// Z=1 -> nicht schieben
		+ "\tAND\t0E0H\n"
		+ "\tJR\tNZ,I4_SHX_UTIL_1\n"
		+ "\tOR\tB\n"			// B<>0 -> Z=0 -> schieben
		+ "\tRET\n"
		+ "I4_SHX_UTIL_1:\n"
		+ "\tXOR\tA\n"			// Z=1 -> nicht schieben
		+ "\tLD\tD,A\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.I4_MAX_I4_I4 ) ) {
      /*
       * Ermittlung des groesseren von zwei uebergebenen Werten
       *
       * Parameter:
       *   DE'HL': Wert 1
       *   DEHL:   Wert 2
       * Rueckgabe:
       *   DEHL:   der groesseren der beiden Werte
       */
      buf.append( "I4_MAX_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tRET\tNC\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I4_MIN_I4_I4 ) ) {
      /*
       * Ermittlung des kleineren von zwei uebergebenen Werten
       *
       * Parameter:
       *   DE'HL': Wert 1
       *   DEHL:   Wert 2
       * Rueckgabe:
       *   DEHL:   der kleinere der beiden Werte
       */
      buf.append( "I4_MIN_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tRET\tC\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I4_DEC_MEM ) ) {
      /*
       * Wert einer Long-Variable dekrementieren
       *
       * Parameter:
       *   HL: Zeiger auf Long-Variable
       */
      buf.append( "I4_DEC_MEM:\n"
		+ "\tLD\tBC,0400H\n"
		+ "\tSCF\n"
		+ "I4_DEC_MEM_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tSBC\tA,C\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tI4_DEC_MEM_1\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_INC_MEM ) ) {
      /*
       * Wert einer Long-Variable dekrementieren
       *
       * Parameter:
       *   HL: Zeiger auf Long-Variable
       */
      buf.append( "I4_INC_MEM:\n"
		+ "\tLD\tBC,0400H\n"
		+ "\tSCF\n"
		+ "I4_INC_MEM_1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tADC\tA,C\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tI4_INC_MEM_1\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_ABS_DEHL ) ) {
      /*
       * Absolutwert von DEHL bilden
       *
       * Parameter:
       *   DEHL: Eingangswert
       * Rueckgabe:
       *   DEHL: Ergebnis
       */
      buf.append( "I4_ABS_DEHL:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tRET\tP\n" );
      compiler.addLibItem( LibItem.I4_NEG_DEHL );
      // direkt weiter mit I4_NEG_DEHL !!!
    }
    if( compiler.usesLibItem( LibItem.I4_NEG_DEHL ) ) {
      /*
       * DEHL mathematisch negieren
       *
       * Parameter:
       *   DEHL: Eingangswert
       * Rueckgabe:
       *   DEHL: Ergebnis
       */
      buf.append("I4_NEG_DEHL:\n"
		+ "\tCALL\tI4_NEG_DEHL_RAW\n"
		+ "\tJP\tC,E_NUMERIC_OVERFLOW\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I4_NEG_DEHL_RAW );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I4_HL_TO_DEHL ) ) {
      /*
       * 16-Bit- zur 32-Bit-Zahl erweitern
       *
       * Parameter:
       *   HL: Operand
       * Rueckgabe:
       *   DEHL: Ergebnis
       */
      buf.append( "I4_HL_TO_DEHL:\n"
		+ "\tLD\tA,H\n"
		+ "\tRLCA\n"		// CY=Vorzeichen
		+ "\tEX\tDE,HL\n"
		+ "\tSBC\tHL,HL\n"	// alle Bits mit CY setzen
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_AND_I2_I2 ) ) {
      /*
       * Bitweises UND
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL AND DE
       */
      buf.append( "I2_AND_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_OR_I2_I2 ) ) {
      /*
       * Bitweises ODER
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL OR DE
       */
      buf.append( "I2_OR_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_XOR_I2_I2 ) ) {
      /*
       * Bitweises Exklusiv-ODER
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL XOR DE
       */
      buf.append( "I2_XOR_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tXOR\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_NOT_I2 ) ) {
      /*
       * Bitweises NICHT (16 Bit)
       *
       * Parameter:
       *   HL: Operand
       * Rueckgabe:
       *   HL: ~HL
       */
      buf.append( "I2_NOT_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_EQ_I4_I4 ) ) {
      /*
       * Vergleich HL'HL == DE'DE ?
       *
       * Parameter:
       *   HL'HL, DE'DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL'HL == DE'DE, sonst 0
       */
      buf.append( "I2_EQ_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_GE_I4_I4 ) ) {
      /*
       * Vergleich DE'HL' >= DEHL ?
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   HL: -1 wenn DE'HL' >= DEHL, sonst 0
       */
      buf.append( "I2_GE_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_GT_I4_I4 ) ) {
      /*
       * Vergleich DE'HL' > DEHL ?
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   HL: -1 wenn DE'HL' > DEHL, sonst 0
       */
      buf.append( "I2_GT_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_LE_I4_I4 ) ) {
      /*
       * Vergleich DE'HL' <= DEHL ?
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   HL: -1 wenn DE'HL' <= DEHL, sonst 0
       */
      buf.append( "I2_LE_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_LT_I4_I4 ) ) {
      /*
       * Vergleich DE'HL' < DEHL ?
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   HL: -1 wenn DE'HL' < DEHL, sonst 0
       */
      buf.append( "I2_LT_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_NE_I4_I4 ) ) {
      /*
       * Vergleich DE'HL' != DEHL ?
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   HL: -1 wenn DE'HL' != DEHL, sonst 0
       */
      buf.append( "I2_NE_I4_I4:\n"
		+ "\tCALL\tCMP_DE2HL2_DEHL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_DE2HL2_DEHL );
    }
    if( compiler.usesLibItem( LibItem.I2_EQ_I2_I2 ) ) {
      /*
       * Vergleich HL == DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL == DE, sonst 0
       */
      buf.append( "I2_EQ_I2_I2:\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_GE_I2_I2 ) ) {
      /*
       * Vergleich HL >= DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL >= DE, sonst 0
       */
      buf.append( "I2_GE_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_GT_I2_I2 ) ) {
      /*
       * Vergleich HL > DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL > DE, sonst 0
       */
      buf.append( "I2_GT_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_LE_I2_I2 ) ) {
      /*
       * Vergleich HL <= DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL <= DE, sonst 0
       */
      buf.append( "I2_LE_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_LT_I2_I2 ) ) {
      /*
       * Vergleich HL < DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL < DE, sonst 0
       */
      buf.append( "I2_LT_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_NE_I2_I2 ) ) {
      /*
       * Vergleich HL != DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL != DE, sonst 0
       */
      buf.append( "I2_NE_I2_I2:\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_INC_I2 ) ) {
      /*
       * Addition: HL = HL + 1
       */
      buf.append( "I2_INC_I2:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,80H\n"
		+ "\tXOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n" );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I2_DEC_I2 ) ) {
      /*
       * Addition: HL = HL - 1
       */
      buf.append( "I2_DEC_I2:\n"
		+ "\tLD\tA,80H\n"
		+ "\tXOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_NUMERIC_OVERFLOW\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I2_SUB_I2_I2 ) ) {
      /*
       * Subtraktion: HL = HL - DE
       */
      buf.append( "I2_SUB_I2_I2:\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n" );
      compiler.addLibItem( LibItem.I2_ADD_I2_I2 );
      compiler.addLibItem( LibItem.I2_NEG_HL );
      // weiter mit I2_ADD_I2_I2 !!!
    }
    if( compiler.usesLibItem( LibItem.I2_ADD_I2_I2 ) ) {
      /*
       * Addition: HL = HL + DE
       */
      buf.append( "I2_ADD_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tXOR\tD\n"
		+ "\tRET\tM\n"
		+ "\tLD\tA,D\n"
		+ "\tXOR\tH\n"
		+ "\tRET\tP\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n" );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I2_MOD_I2_I2 ) ) {
      /*
       * Modulo: HL = HL % DE
       */
      buf.append( "I2_MOD_I2_I2:\n"
		+ "\tPUSH\tHL\n"		// Vorzeichen des Ergebnisses
		+ "\tCALL\tI2_DIV_I2_I2_1\n"
		+ "\tPOP\tAF\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,I2_NEG_HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_NEG_HL );
      compiler.addLibItem( LibItem.I2_DIV_I2_I2 );
    }
    if( compiler.usesLibItem( LibItem.I2_DIV_I2_I2 ) ) {
      /*
       * Division: HL = HL / DE
       */
      buf.append( "I2_DIV_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tPUSH\tAF\n"		// Vorzeichen des Ergebnisses
		+ "\tCALL\tI2_DIV_I2_I2_1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tAF\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,I2_NEG_HL\n"
		+ "\tRET\n"
      /*
       * vorzeichenbehaftete Division DE = HL/DE, Rest in HL
       * Ergebnis und Rest immer positiv
       */
		+ "I2_DIV_I2_I2_1:\n"
		+ "\tCALL\tI2_ABS_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tI2_ABS_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJP\tZ,E_DIV_BY_0\n"
      // vorzeichenlose Division DE = HL/DE, Rest in HL
		+ "I2_DIV_I2_I2_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\tB,10H\n"
		+ "I2_DIV_I2_I2_3:\n"
		+ "\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tADC\tHL,HL\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,I2_DIV_I2_I2_4\n"
		+ "\tADD\tHL,DE\n"
		+ "I2_DIV_I2_I2_4:\n"
		+ "\tDJNZ\tI2_DIV_I2_I2_3\n"
		+ "\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tCPL\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tLD\tE,A\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_ABS_HL );
      compiler.addLibItem( LibItem.I2_NEG_HL );
      compiler.addLibItem( LibItem.E_DIV_BY_0 );
    }
    if( compiler.usesLibItem( LibItem.I2_MUL_I2_I2 ) ) {
      /*
       * Multiplikation: HL = HL * DE
       *
       * Wenn beide Operanden groesser 8 Bit sind,
       * kommt es zu einem Ueberlauf,
       * da das Ergebnis nur max. 16 Bit gross sein darf.
       * Umgekehrt heisst das, dass mindestens ein Operand
       * nur 8 Bit gross ist.
       * Die eigentliche Multiplikation wird deshalb
       * mit 16x8 Bit ausgefuehrt.
       */
      buf.append( "I2_MUL_I2_I2:\n"
		+ "\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tPUSH\tAF\n"
		+ "\tCALL\tI2_ABS_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tI2_ABS_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,I2_MUL_I2_I2_1\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJP\tNZ,E_NUMERIC_OVERFLOW\n"
      // vorzeichenlose Multiplikation HL = L * DE
		+ "I2_MUL_I2_I2_1:\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\tB,8\n"
		+ "I2_MUL_I2_I2_2:\n"
		+ "\tSRA\tA\n"
		+ "\tJR\tNC,I2_MUL_I2_I2_3\n"
		+ "\tADD\tHL,DE\n"
		+ "\tJP\tC,E_NUMERIC_OVERFLOW\n"
		+ "I2_MUL_I2_I2_3:\n"
		+ "\tSLA\tE\n"
		+ "\tRL\tD\n"
		+ "\tDJNZ\tI2_MUL_I2_I2_2\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_NUMERIC_OVERFLOW\n"
		+ "\tPOP\tAF\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,I2_NEG_HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_ABS_HL );
      compiler.addLibItem( LibItem.I2_NEG_HL );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.I2_SHL_I2_I2 ) ) {
      /*
       * links schieben: HL = HL << DE
       */
      buf.append( "I2_SHL_I2_I2:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tJR\tNZ,I2_SHL_I2_I2_2\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tB,E\n"
		+ "I2_SHL_I2_I2_1:\n"
		+ "\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tDJNZ\tI2_SHL_I2_I2_1\n"
		+ "\tRET\n"
		+ "I2_SHL_I2_I2_2:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.I2_SHR_I2_I2 ) ) {
      /*
       * rechts schieben: HL = HL >> DE
       */
      buf.append( "I2_SHR_I2_I2:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"
		+ "\tJR\tNZ,I2_SHR_I2_I2_2\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tB,E\n"
		+ "I2_SHR_I2_I2_1:\n"
		+ "\tSRL\tH\n"
		+ "\tRR\tL\n"
		+ "\tDJNZ\tI2_SHR_I2_I2_1\n"
		+ "\tRET\n"
		+ "I2_SHR_I2_I2_2:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.I2_MAX_I2_I2 ) ) {
      /*
       * Ermittlung des groesseren von zwei uebergebenen Werten
       *
       * Parameter:
       *   DE: Wert 1
       *   HL: Wert 2
       * Rueckgabe:
       *   HL: HL > DE ? HL : DE
       */
      buf.append( "I2_MAX_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tRET\tNC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_MIN_I2_I2 ) ) {
      /*
       * Ermittlung des kleineren von zwei uebergebenen Werten
       *
       * Parameter:
       *   DE: Wert 1
       *   HL: Wert 2
       * Rueckgabe:
       *   HL: HL < DE ? HL : DE
       */
      buf.append( "I2_MIN_I2_I2:\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tRET\tC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( LibItem.I2_EQ_S_S ) ) {
      /*
       * Zeichenkette (DE) == (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenketten gleich sind, sonst 0
       */
      buf.append( "I2_EQ_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_GE_S_S ) ) {
      /*
       * Zeichenkette (DE) >= (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) >= (HL), sonst 0
       */
      buf.append( "I2_GE_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_GT_S_S ) ) {
      /*
       * Zeichenkette (DE) > (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) > (HL), sonst 0
       */
      buf.append( "I2_GT_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_LE_S_S ) ) {
      /*
       * Zeichenkette (DE) <= (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) < (HL), sonst 0
       */
      buf.append( "I2_LE_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_LT_S_S ) ) {
      /*
       * Zeichenkette (DE) < (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) < (HL), sonst 0
       */
      buf.append( "I2_LT_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_NE_S_S ) ) {
      /*
       * Zeichenkette (DE) != (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenketten ungleich sind, sonst 0
       */
      buf.append( "I2_NE_S_S:\n"
		+ "\tCALL\tI2_STR_CMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.I2_STR_CMP );
    }
    if( compiler.usesLibItem( LibItem.I2_CONTAINS ) ) {
      /*
       * Pruefen, ob ein Wert in einer Liste von Werten enthalten ist
       *
       * Parameter:
       *   DE: zu pruefender Wert
       *   1. Byte hinter CALL:		Anzahl Eintraege in der Liste
       *   2. Byte hinter CALL:		L-Adresse Listenanfang
       *   3. Byte hinter CALL:		H-Adresse Listenanfang
       * Rueckgabe:
       *   CY=0: Wert von DE in der Liste enthalten
       *   CY=1: Wert von DE nicht enthalten
       */
      buf.append( "I2_CONTAINS:\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tC,(HL)\n"		// L-Adresse
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"		// H-Adresse
		+ "\tINC\tHL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tA,(BC)\n"		// Anzahl Eintraege
		+ "\tINC\tBC\n"
		+ "I2_CONTAINS_1:\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tH,A\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tZ\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,I2_CONTAINS_1\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_DEC_MEM ) ) {
      /*
       * Wert einer Integer-Variable inkrementieren
       *
       * Parameter:
       *   HL: Zeiger auf Integer-Variable
       */
      buf.append( "I2_DEC_MEM:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(HL),D\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_INC_MEM ) ) {
      /*
       * Wert einer Integer-Variable inkrementieren
       *
       * Parameter:
       *   HL: Zeiger auf Integer-Variable
       */
      buf.append( "I2_INC_MEM:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),D\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_ABS_HL ) ) {
      /*
       * Absolutwert von HL
       *
       * Parameter:
       *   HL: Eingangswert
       * Rueckgabe:
       *   HL: Ergebnis
       */
      buf.append( "I2_ABS_HL:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tP\n" );
      compiler.addLibItem( LibItem.I2_NEG_HL );
      // direkt weiter mit I2_NEG_HL !!!
    }
    if( compiler.usesLibItem( LibItem.I2_NEG_HL ) ) {
      /*
       * Wert in HL mathematisch negieren
       *
       * Parameter:
       *   HL: Eingangswert
       * Rueckgabe:
       *   HL: Ergebnis
       */
      buf.append( "I2_NEG_HL:\n"
		+ "\tCALL\tI2_NEG_HL_RAW\n"
		+ "\tRET\tNC\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n" );
      compiler.addLibItem( LibItem.I2_NEG_HL_RAW );
      compiler.addLibItem( LibItem.E_NUMERIC_OVERFLOW );
    }
    if( compiler.usesLibItem( LibItem.CMP_HL_DE ) ) {
      /*
       * Vorzeichenbehafteter Vergleich von HL und DE,
       * Register BC, DE und HL werden nicht veraendert.
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   C-Flag: HL < DE
       *   Z-Flag: HL == DE
       */
      buf.append( "CMP_HL_DE:\n"
		+ "\tLD\tA,H\n"
                + "\tXOR\tD\n"
                + "\tJP\tP,CMP_HL_DE_1\n"
                + "\tEX\tDE,HL\n"
                + "\tCALL\tCMP_HL_DE_1\n"
                + "\tEX\tDE,HL\n"
                + "\tRET\n"
                + "CMP_HL_DE_1:\n"
		+ "\tLD\tA,H\n"
                + "\tCP\tD\n"
                + "\tRET\tNZ\n"
                + "\tLD\tA,L\n"
                + "\tCP\tE\n"
                + "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.CMP_DE2HL2_DEHL ) ) {
      /*
       * Vorzeichenbehafteter Vergleich von DE'HL' und DEHL,
       * Register BC, DE und HL werden nicht veraendert.
       *
       * Parameter:
       *   DE'HL': Operand 1
       *   DEHL:   Operand 2
       * Rueckgabe:
       *   C-Flag: DE'HL' < DEHL
       *   Z-Flag: DE'HL' == DEHL
       */
      buf.append( "CMP_DE2HL2_DEHL:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
                + "\tXOR\tD\n"
                + "\tJP\tP,CMP_DE2HL2_DEHL_1\n"
		+ "\tEXX\n"
                + "\tCALL\tCMP_DE2HL2_DEHL_1\n"
		+ "\tEXX\n"
                + "\tRET\n"
                + "CMP_DE2HL2_DEHL_1:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
                + "\tCP\tD\n"
                + "\tRET\tNZ\n"
		+ "\tEXX\n"
		+ "\tLD\tA,E\n"
		+ "\tEXX\n"
                + "\tCP\tE\n"
                + "\tRET\tNZ\n"
		+ "\tEXX\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
                + "\tCP\tH\n"
                + "\tRET\tNZ\n"
		+ "\tEXX\n"
		+ "\tLD\tA,L\n"
		+ "\tEXX\n"
                + "\tCP\tL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_DEHL_MEM ) ) {
      /*
       * DEHL aus dem Speicher laden
       *
       * Parameter:
       *   HL: Zeiger auf den Speicherplatz
       */
      buf.append( "LD_DEHL_MEM:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_HL_MEM ) ) {
      /*
       * HL aus dem Speicher laden
       *
       * Parameter:
       *   HL: Zeiger auf den Speicherplatz
       */
      buf.append( "LD_HL_MEM:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_HL2HL_NULL ) ) {
      /*
       * HL2HL mit 0 laden
       */
      buf.append( "LD_HL2HL_NULL:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_MEM_NNNN ) ) {
      /*
       * Die Funktion kopiert die vier hinter dem Funktionsaufruf
       * stehenden Bytes in den Speicherbereich, auf den HL zeigt.
       *
       * Parameter:
       *   HL: Zeiger auf den Speicherplatz
       */
      buf.append( "LD_MEM_NNNN:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tBC,0004H\n"
		+ "\tLDIR\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_MEM_DEBC ) ) {
      /*
       * Die Funktion kopiert die vier in DEBC stehenden Bytes
       * in den Speicherbereich, auf den HL zeigt.
       *
       * Parameter:
       *   DEBC: zu schreibende Bytes
       *   HL:   Zeiger auf den Speicherplatz
       */
      buf.append( "LD_MEM_DEBC:\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_MEM_DEHL ) ) {
      /*
       * Die Funktion kopiert die vier in DEHL stehenden Bytes
       * in den Speicherbereich, auf den BC zeigt.
       *
       * Parameter:
       *   DEHL: zu schreibende Bytes
       *   BC:   Zeiger auf den Speicherplatz
       */
      buf.append( "LD_MEM_DEHL:\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,H\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,E\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,D\n"
		+ "\tLD\t(BC),A\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.LD_MEM_DE2HL2 ) ) {
      /*
       * Die Funktion schreibt die vier in DE'HL' stehenden Bytes
       * in den Speicherbereich, auf den HL zeigt.
       *
       * Parameter:
       *   DE'HL': zu schreibende Bytes
       *   HL:     Zeiger auf den Speicherplatz
       */
      buf.append( "LD_MEM_DE2HL2:\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.SCREEN ) ) {
      buf.append( "SCREEN:\tCALL\tXSCREEN\n"
		+ "\tJP\tC,E_INVALID_PARAM\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.XSCREEN );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( LibItem.IOEOF ) ) {
      /*
       * Pruefen, ob das Ende des Eingabekanals erreicht wurde.
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       */
      buf.append( "IOEOF:\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOEOF1\n" );
      BasicUtil.appendSetErrorChannelClosed( compiler );
      buf.append( "\tJR\tIOEOF2\n"
		+ "IOEOF1:\n" );
      for( int i = 1; i < IOCTB_EOF_OFFS; i++ ) {
	buf.append( "\tINC\tHL\n" );
      }
      buf.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,IOEOF3\n" );
      BasicUtil.appendSetErrorIOMode( compiler );
      buf.append( "IOEOF2:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n"
		+ "IOEOF3:\tJP\t(HL)\n" );
    }
    if( compiler.usesLibItem( LibItem.IOINL ) ) {
      /*
       * Lesen einer Zeile aus einem Eingabekanal
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       *   C:  Trennzeichen oder 0
       * Rueckgabewert:
       *   HL: Zeiger auf die gelesene Zeichenkette
       */
      buf.append( "IOINL:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOINL1\n" );
      BasicUtil.appendSetErrorChannelClosed( compiler );
      buf.append_LD_HL_xx( EMPTY_STRING_LABEL );
      buf.append( "\tRET\n"
		+ "IOINL1:\tDEC\tHL\n"
		+ "\tLD\tB," );
      buf.appendHex2( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "IOINL2:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIORDB\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,IOINL3\n"
		+ "\tCP\tC\n"
		+ "\tJR\tZ,IOINL3\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,IOINL3\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,IOINL4\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tIOINL2\n"
		+ "IOINL3:\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n"
		+ "IOINL4:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIORDB\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,IOINL3\n" );
      buf.append_LD_BC_nn( IOCTB_BBUF_OFFS + 1 );
      buf.append( "\tADD\tHL,BC\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tJR\tIOINL3\n" );
      compiler.addLibItem( LibItem.IORDB );
      compiler.addLibItem( LibItem.EMPTY_STRING );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.IOINX ) ) {
      /*
       * Lesen von Bytes aus einem Eingabekanal
       *
       * Parameter:
       *   BC: Anzahl der zu lesenden Bytes
       *   HL: Adresse des Kanalzeigerfeldes
       * Rueckgabewert:
       *   HL: Zeiger auf die gelesene Zeichenkette
       */
      buf.append( "IOINX:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOINX1\n" );
      BasicUtil.appendSetErrorChannelClosed( compiler );
      buf.append_LD_HL_xx( EMPTY_STRING_LABEL );
      buf.append( "\tRET\n"
		+ "IOINX1:\tDEC\tHL\n"
		+ "\tLD\tDE,M_TMP_STR_BUF\n"
		+ "IOINX2:\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,IOINX3\n"
		+ "\tDEC\tBC\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIORDB\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tIOINX2\n"
		+ "IOINX3:\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_TMP_STR_BUF\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.IORDB );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.EMPTY_STRING );
      compiler.addLibItem( LibItem.M_TMP_STR_BUF );
    }
    if( compiler.usesLibItem( LibItem.IORDB ) ) {
      /*
       * Lesen eines Bytes aus einem Eingabekanal
       * Der Kanal muss offen sein!
       * Im Fehlerfall werden die Fehlervariablen gesetzt.
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       * Rueckgabewert:
       *   A:  gelesenes Zeichen bzw. 00h ab EOF
       */
      buf.append( "IORDB:\tEX\tDE,HL\n" );
      buf.append_LD_HL_nn( IOCTB_BBUF_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,IORDB1\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tRET\n"
		+ "IORDB1:" );
      buf.append_LD_HL_nn( IOCTB_READ_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,IORDB2\n" );
      BasicUtil.appendSetErrorIOMode( compiler );
      buf.append( "\tRET\n"
		+ "IORDB2:\tJP\t(HL)\n" );
    }
    if( compiler.usesLibItem( LibItem.IOCLOSE ) ) {
      /*
       * Schliessen eines IO-Kanals
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       */
      buf.append( "IOCLOSE:\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,IOCLOSE1\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tJP_HL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tIOCLOSE2\n"
		+ "IOCLOSE1:\n" );
      BasicUtil.appendSetErrorChannelClosed( compiler );
      buf.append( "IOCLOSE2:\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB," );
      buf.appendHex2( compiler.getIOChannelSize() );
      buf.append( "\n"
		+ "IOCLOSE3:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tIOCLOSE3\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.JP_HL );
    }
    if( compiler.usesLibItem( LibItem.IOOPEN ) ) {
      /*
       * Oeffnen eines IO-Kanals
       *
       * Parameter:
       *   (IO_M_NAME):   Zeiger auf Geraete-/Dateiname
       *   (IO_M_ACCESS): Zugriffsmode
       *   HL:            Anfangsadresse des Kanalzeigerfeldes
       */
      buf.append( "IOOPEN:\n"
		+ "\tLD\t(IO_M_CADDR),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,IOOPEN1\n" );
      BasicUtil.appendSetErrorChannelAlreadyOpen( compiler );
      buf.append( "\tRET\n"
		+ "IOOPEN1:\n"
		+ "\tLD\tHL,IO_HANDLER_TAB\n"
		+ "IOOPEN2:\n"
		+ "\tLD\tE,(HL)\n"		// Handler-Adresse holen
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,IOOPEN3\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tJP_HL\n"		// Handler aufrufen
		+ "\tPOP\tHL\n"
		+ "\tJR\tC,IOOPEN2\n" );	// naechster Handler
      if( compiler.usesLibItem( LibItem.M_ERROR_NUM )
	  || compiler.usesLibItem( LibItem.M_ERROR_TEXT ) )
      {
	/*
	 * Der Handler hat sich fuer den Geraete- bzw. Dateinamen
	 * verantwortlich gefuehlt.
	 * Pruefen, ob der Kanal geoeffnet wurde.
	 */
	buf.append( "\tLD\tHL,(IO_M_CADDR)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tRET\tNZ\n" );	// Kanal offen -> OK
	if( compiler.usesLibItem( LibItem.M_ERROR_NUM ) ) {
	  buf.append( "\tLD\tHL,(M_ERROR_NUM)\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n" );	// Fehler eingetragen -> OK
	} else if( compiler.usesLibItem( LibItem.M_ERROR_TEXT ) ) {
	  buf.append( "\tLD\tHL,(M_ERROR_TEXT)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n" );	// Fehler eingetragen -> OK
	}
	BasicUtil.appendSetError( compiler );	// Fehler eintragen
      }
      buf.append( "\tRET\n"
		+ "IOOPEN3:\n" );
      // Keine passenden Handler gefunden
      BasicUtil.appendSetErrorDeviceNotFound( compiler );
      buf.append( "\tRET\n" );
      if( compiler.needsDriver( BasicCompiler.IODriver.CRT )
	  && options.isOpenCrtEnabled() )
      {
	buf.append( "IO_CRT_HANDLER:\n"
		+ "\tLD\tHL,D_IOCRT\n"
		+ "\tLD\tDE,XOUTCH\n"
		+ "\tJR\tIO_SIMPLE_OUT_HANDLER\n" );
	compiler.addLibItem( LibItem.IO_CRT_HANDLER );
	compiler.addLibItem( LibItem.IO_SIMPLE_OUT_HANDLER );
	compiler.addLibItem( LibItem.XOUTCH );
      }
      if( compiler.needsDriver( BasicCompiler.IODriver.LPT )
	  && options.isOpenLptEnabled()
	  && target.supportsXLPTCH() )
      {
	buf.append( "IO_LPT_HANDLER:\n"
		+ "\tLD\tHL,D_IOLPT\n"
		+ "\tLD\tDE,XLPTCH\n"
		+ "\tJR\tIO_SIMPLE_OUT_HANDLER\n" );
	compiler.addLibItem( LibItem.IO_LPT_HANDLER );
	compiler.addLibItem( LibItem.IO_SIMPLE_OUT_HANDLER );
	compiler.addLibItem( LibItem.XLPTCH );
      }
      if( compiler.usesLibItem( LibItem.IO_SIMPLE_OUT_HANDLER ) ) {
	/*
	 * Handler fuer einfachen Ausgabekanal
	 *
	 * Der Code steht direkt hinter
	 * IO_CRT_HANDLER bzw. IO_LPT_HANDLER,
	 * damit die relative Sprungdistanz nicht zu gross wird.
	 *
	 * Parameter:
	 *   DE:            Adresse der Ausgaberoutine
	 *   HL:            Geraetename, auf den der Handler reagiert
	 *   (IO_M_NAME):   Zeiger auf Geraetename
	 *                  (wird mit dem in HL verglichen)
	 *   (IO_M_CADDR):  Anfangsadresse des Kanalzeigerfeldes
	 *   (IO_M_ACCESS): Zugriffsmode (muss 0 oder 2 sein)
	 * Rueckgabewert:
	 *   CY=1:     Handler ist nicht zustaendig
	 *   CY=0:     Handler ist zustaendig
	 *               -> keine weiteren Handler testen
	 */
	buf.append( "IO_SIMPLE_OUT_HANDLER:\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tDE,(IO_M_NAME)\n"
		+ "IO_SIMPLE_OUT_H1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\t(HL)\n"
		+ "\tJR\tNZ,IO_SIMPLE_OUT_H5\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tNZ,IO_SIMPLE_OUT_H1\n"
		+ "\tLD\tA,(IO_M_ACCESS)\n"	// Betriebsart pruefen
		+ "\tAND\t" );
	buf.appendHex2( ~(IOMODE_DEFAULT_MASK
				| IOMODE_OUTPUT_MASK
				| IOMODE_TXT_MASK
				| IOMODE_BIN_MASK) );
	buf.append( "\n"
		+ "\tJR\tZ,IO_SIMPLE_OUT_H2\n" );
	// Betriebsart nicht moeglich
	BasicUtil.appendSetErrorIOMode( compiler );
	buf.append( "\tPOP\tDE\n"
		+ "\tOR\tA\n"			// C=0
		+ "\tRET\n"
		+ "IO_SIMPLE_OUT_H2:\n"
		+ "\tLD\tHL,(IO_M_CADDR)\n"
		+ "\tLD\tDE,IO_SIMPLE_CLOSE\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB," );
	buf.appendHex2( IOCTB_WRITE_OFFS - 2 );
	buf.append( "\n"
		+ "IO_SIMPLE_OUT_H3:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tIO_SIMPLE_OUT_H3\n"
		+ "\tPOP\tDE\n"			// Routine fuer Zeichenausgabe
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB," );
	buf.appendHex2( compiler.getIOChannelSize() - IOCTB_WRITE_OFFS - 2 );
	buf.append( "\n"
		+ "IO_SIMPLE_OUT_H4:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tIO_SIMPLE_OUT_H4\n"
		+ "\tRET\n"
		+ "IO_SIMPLE_OUT_H5:\n"
		+ "\tPOP\tDE\n"
		+ "\tSCF\n"
		+ "IO_SIMPLE_CLOSE:\n"
		+ "\tRET\n" );
	compiler.addLibItem( LibItem.C_UPPER_C );
      }
      if( compiler.needsDriver( BasicCompiler.IODriver.DISK )
	  && options.isOpenDiskEnabled() )
      {
	String diskHandlerLabel = target.getDiskHandlerLabel();
	if( diskHandlerLabel != null ) {
	  if( !diskHandlerLabel.isEmpty() ) {
	    compiler.getTarget().appendDiskHandlerTo( buf, compiler );
	    compiler.addLibItem( LibItem.IO_DISK_HANDLER );
	  }
	}
      }
      if( compiler.needsDriver( BasicCompiler.IODriver.VDIP )
	  && options.isOpenVdipEnabled() )
      {
	int[] ioAddrs = target.getVdipBaseIOAddresses();
	if( ioAddrs != null ) {
	  if( ioAddrs.length > 0 ) {
	    VdipLibrary.appendCodeTo( compiler );
	    compiler.addLibItem( LibItem.IO_VDIP_HANDLER );
	  }
	} else {
	  String vdipHandlerLabel = target.getVdipHandlerLabel();
	  if( vdipHandlerLabel != null ) {
	    if( !vdipHandlerLabel.isEmpty() ) {
	      target.appendVdipHandlerTo( buf, compiler );
	      compiler.addLibItem( LibItem.IO_VDIP_HANDLER );
	    }
	  }
	}
      }
      compiler.addLibItem( LibItem.JP_HL );
    }
    if( compiler.usesLibItem( LibItem.IOCADR ) ) {
      /*
       * Ermittlung der Kanalanfangsadresse in der IOPTAB
       * anhand der Kanalnummer
       *
       * Parameter:
       *   HL: Kanalnummer (>=1)
       * Rueckgabe:
       *   HL: Kanalanfangsadresse
       */
      buf.append( "IOCADR:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_INVALID_PARAM\n"
		+ "\tDEC\tA\n"
		+ "\tCP\t04H\n"
		+ "\tJP\tNC,E_INVALID_PARAM\n"
      // Multiplikation mit 12 (IOCTB_CHANNEL_SIZE)
		+ "\tADD\tA,A\n"			// x2
		+ "\tLD\tB,A\n"	
		+ "\tADD\tA,A\n"			// x4
		+ "\tADD\tA,B\n"			// +2 -> x6
		+ "\tADD\tA,A\n"			// x12
		+ "\tLD\tL,A\n"
		+ "\tLD\tDE,IOCTB1\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.E_INVALID_PARAM );
      compiler.addLibItem( LibItem.IOCTB1 );
      compiler.addLibItem( LibItem.IOCTB2 );
    }
    if( compiler.usesLibItem( LibItem.ASSIGN_STR_TO_NEW_MEM_VS ) ) {
      /*
       * Eine Zeichenkette einer String-Variable zuweisen,
       * wobei diese in einen neu allokierten Speicherberich kopiert wird
       *
       * Parameter:
       *   DE: Zeiger auf die String-Variable
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "ASSIGN_STR_TO_NEW_MEM_VS:\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tMALLOC_AND_STR_COPY\n"
		+ "\tPOP\tDE\n" );
      compiler.addLibItem( LibItem.MALLOC_AND_STR_COPY );
      compiler.addLibItem( LibItem.ASSIGN_STR_TO_VS );
      // direkt weiter mit ASSIGN_STR_TO_VS !!!
    }
    if( compiler.usesLibItem( LibItem.ASSIGN_STR_TO_VS ) ) {
      /*
       * Eine Zeichenkette einer String-Variable zuweisen
       *
       * Parameter:
       *   DE: Zeiger auf die String-Variable
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "ASSIGN_STR_TO_VS:\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tCALL\tMFREE\n"
		+ "\tPOP\tDE\n"			// Zeichenkette
		+ "\tPOP\tHL\n"			// Variable
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.MFREE );
    }
    if( compiler.usesLibItem( LibItem.ASSIGN_VS_TO_VS ) ) {
      /*
       * Eine String-Variable einer andern zuweisen,
       * Wenn die Zeichenkette der Quellvariablen im Heap liegt,
       * wird nur der Referenzzaehler erhoeht.
       * Sollte dieser dabei ueberlaufen,
       * muss ein neuer Speicherbereich allokiert werden.
       *
       * Parameter:
       *   DE: Zeiger auf die Zielvariable
       *   HL: Zeiger auf die Quellvariable
       */
      buf.append( "ASSIGN_VS_TO_VS:\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\tD\n"
		+ "\tJR\tNZ,ASSIGN_VS_TO_VS1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tRET\tZ\n"
		+ "ASSIGN_VS_TO_VS1:\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tCALL\tSTR_VAR_DUP\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\t(HL),D\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tLD\tD,B\n"
		+ "\tLD\tE,C\n"
		+ "\tJP\tMFREE\n" );
      compiler.addLibItem( LibItem.STR_VAR_DUP );
      compiler.addLibItem( LibItem.MFREE );
    }
    if( compiler.usesLibItem( LibItem.MALLOC_AND_STR_COPY ) ) {
      /*
       * Die Routine allokiert Speicher und kopiert die in HL
       * uebergebene Zeichenkette hinein.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   DE: Zeiger auf die kopierte Zeichenkette
       */
      buf.append( "MALLOC_AND_STR_COPY:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,MALLOC_AND_STR_COPY_1\n" );
      buf.append_LD_DE_xx( EMPTY_STRING_LABEL );
      buf.append( "\tRET\n"
		+ "MALLOC_AND_STR_COPY_1:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tSTR_N_COPY\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.MFIND );
      compiler.addLibItem( LibItem.MALLOC );
      compiler.addLibItem( LibItem.STR_N_COPY );
      compiler.addLibItem( LibItem.EMPTY_STRING );
    }
    if( compiler.usesLibItem( LibItem.STR_VAR_DUP ) ) {
      /*
       * Die Routine inkrementiert den Referenzzaehler in dem Speicherblock,
       * in dem die Zeichenkette gespeichert ist, auf die DE zeigt.
       * Wuerde der Referenzzaehler ueberlaufen,
       * wird ein neuer Speicherblock allokiert und in DE zurueckgeliefert.
       * Ist die Zeichenkette in keinem mit MFREE gefundenden Speicherblock
       * gespeichert, passiert nichts.
       *
       * Parameter:
       *   DE: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   DE: Zeiger auf evtl. kopierte Zeichenkette
       */
      buf.append( "STR_VAR_DUP:\n"
		+ "\tCALL\tCHECK_DE_WITHIN_HEAP\n"
		+ "\tRET\tC\n"
		+ "\tJR\tSTR_VAR_DUP_2\n"	// HL: Heap-Anfang
		+ "STR_VAR_DUP_1:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "STR_VAR_DUP_2:\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,STR_VAR_DUP_1\n"
		+ "\tADD\tHL,DE\n"
		+ "\tDEC\tHL\n"			// Block gefunden
		+ "\tBIT\t7,(HL)\n"
		+ "\tJR\tNZ,STR_VAR_DUP_3\n"	// Bit 7 -> neuer Speicher
		+ "\tINC\t(HL)\n"
		+ "\tRET\tP\n"
		+ "\tDEC\t(HL)\n"		// Zaehlerueberlauf
		+ "STR_VAR_DUP_3:\n"		// neuer Speicher
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tCALL\tSTR_N_COPY\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.MFIND );
      compiler.addLibItem( LibItem.MALLOC );
      compiler.addLibItem( LibItem.CHECK_DE_WITHIN_HEAP );
      compiler.addLibItem( LibItem.HEAP );
      compiler.addLibItem( LibItem.STR_N_COPY );
    }
    StringLibrary.appendCodeTo( compiler );
    if( compiler.usesLibItem( LibItem.C_UPPER_C ) ) {
      // Zeichen in A gross wandeln
      buf.append( "C_UPPER_C:\n"
		+ "\tCP\t61H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t7BH\n"
		+ "\tRET\tNC\n"
		+ "\tSUB\t20H\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.MFIND ) ) {
      /*
       * Suchen eines mindestens 256-Byte grossen Speicherbereichs,
       * welcher noch nicht belegt ist.
       *
       * Der Heap ist in dynamische Bloecke unterteilt,
       * die in einer einfach verketteten Liste organisiert sind.
       * Jeder Block beginnt mit einem 5 Byte grossen Kopf.
       *
       * Aufbau eines Blocks:
       *   +-------------------------------------------------------+
       *   |               nutzbarer Speicherbereich               |
       *   +-------------------------------------------------------+  
       *   | 1 Byte:  Verwaltung                                   | \
       *   |            Bit 0...6:  Referenzzaehler, 0: Block frei |  \
       *   |            Bit 7:      Rueckgabewert einer Funktion,  |   \
       *   |                        muss am Ende der Anweisung     |    \
       *   |                        einmal freigegeben werden      |     Kopf
       *   +-------------------------------------------------------+    /
       *   | 2 Bytes: Groesse des nutzbaren Speicherbereichs       |   /
       *   +-------------------------------------------------------+  /
       *   | 2 Bytes: Zeiger auf den naechsten Block oder 0000h    | /
       *   +-------------------------------------------------------+
       *
       * Die Routine sucht den Block, markiert ihn aber noch nicht als belegt.
       *
       * Rueckgabewert:
       *   DE: Anfangsadresse des gefundenen Speicherbereichs
       */
      buf.append( "MFIND:\tLD\tHL,HEAPB\n"
		+ "MFIND1:\tLD\tE,(HL)\n"	// DE: naechster Block
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"		// H-Anteil Blockgroesse
		+ "\tOR\tA\n"
		+ "\tJR\tZ,MFIND2\n"		// Block zu klein
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"		// Referenzzaehler
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,MFIND2\n"		// Block belegt
		+ "\tINC\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n"
		+ "MFIND2:\tEX\tDE,HL\n"	// naechster Block
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,MFIND1\n"
		+ "E_OUT_OF_MEM:\n" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Speicher voll\'\n" );
      } else {
	buf.append( "\tDB\t\'Out of memory\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.HEAP );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.MALLOC ) ) {
      /*
       * Allokieren eines Teils des mit MFIND gefundenen Speicherbereichs
       *
       * Der in HL uebergebene Speicherbereich wird bis einschliesslich
       * der in DE uebergebenen Adresse allokiert.
       * Der Speicherbereich muss vorher mit der MFIND-Funktion
       * ermittelt worden sein!
       * 
       * Parameter:
       *   HL: Zeiger auf den mit MFIND gefundenen Speicherbereich
       *   DE: letzte belegte Adresse im Speicherbereich
       * Rueckgabewert:
       *   HL: Zeiger auf den allokierten Speicherbereich
       */
      buf.append( "MALLOC:\tPUSH\tIY\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tC,MALLOC_1\n"
		+ "\tJR\tZ,MALLOC_1\n"
		+ "\tLD\tB,H\n"			// BC: neue Nutzgroesse
		+ "\tLD\tC,L\n"
		+ "\tLD\tIY,0FFFBH\n"		// -5
		+ "\tADD\tIY,DE\n"		// IY: Blockanfang
		+ "\tLD\tL,(IY+2)\n"
		+ "\tLD\tH,(IY+3)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"		// HL: freier Bereich
		+ "\tJR\tC,MALLOC_1\n"
		+ "\tJR\tZ,MALLOC_1\n"
		+ "\tLD\tDE,0010H\n"		// Mindestgroesse fuer
		+ "\tSBC\tHL,DE\n"		// Blocksplittung
		+ "\tJR\tC,MALLOC_1\n"
		+ "\tJR\tZ,MALLOC_1\n"
		+ "\tADD\tHL,DE\n"		// HL: Groesse neuer Block
		+ "\tLD\t(IY+2),C\n"		// Block verkleinern
		+ "\tLD\t(IY+3),B\n"		// und
		+ "\tLD\t(IY+4),01H\n"		// als belegt markieren
		+ "\tLD\tE,(IY+0)\n"
		+ "\tLD\tD,(IY+1)\n"		// DE: Adresse naechster Block
		+ "\tPUSH\tIY\n"		// Block splitten
		+ "\tADD\tIY,BC\n"
		+ "\tLD\tBC,0005H\n"
		+ "\tADD\tIY,BC\n"		// IY: neue Blockadresse
		+ "\tLD\t(IY+0),E\n"
		+ "\tLD\t(IY+1),D\n"
		+ "\tLD\tDE,0005H\n"		// freier Bereich minus
		+ "\tOR\tA\n"			// Kopfgroesse ist Nutzgroesse
		+ "\tSBC\tHL,DE\n"		// im neuen Block
		+ "\tLD\t(IY+2),L\n"
		+ "\tLD\t(IY+3),H\n"
		+ "\tLD\t(IY+4),00H\n"
		+ "\tPUSH\tIY\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tIY\n"
		+ "\tLD\t(IY+0),E\n"
		+ "\tLD\t(IY+1),D\n"
		+ "MALLOC_1:\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tIY\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.MMGC ) ) {
      /*
       * Speicherbereich fuer spaetere Freigabe mit MRGC markieren
       * (Bit 7 im Referenzaehler setzen, Mark for Garbage Collector)
       * 
       * Parameter:
       *   DE: Zeiger auf den allokierten Speicherbereich
       */
      buf.append( "MMGC:\tCALL\tCHECK_DE_WITHIN_HEAP\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tIY\n"		// HL: Heap-Anfang
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "MMGC_1:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MMGC_3\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tZ,MMGC_2\n"		// Block gefunden
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tMMGC_1\n"
		+ "MMGC_2:\tEX\tDE,HL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tSET\t7,(HL)\n"		// Block markieren
		+ "MMGC_3:\tPOP\tIY\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CHECK_DE_WITHIN_HEAP );
    }
    if( compiler.usesLibItem( LibItem.MRGC ) ) {
      /*
       * Freigeben aller Speicherbereiche,
       * bei denen im Refenerenzzaehler Bit 7 gesetzt ist
       * (Run Garbage Collector)
       */
      buf.append( "MRGC:\tPUSH\tIY\n"
		+ "\tLD\tHL,0000H\n"		// vorheriger Block
		+ "\tLD\tIY,HEAPB\n"		// aktueller Block
		+ "MRGC_1:\tLD\tA,(IY+5)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tP,MRGC_3\n"
		+ "\tAND\t7FH\n"
		+ "\tJR\tZ,MRGC_2\n"
		+ "\tDEC\tA\n"
		+ "MRGC_2:\tLD\t(IY+5),A\n"	// Referenzzaehler dekremtiert
		+ "\tJR\tNZ,MRGC_3\n"		// trotzdem noch nicht frei
		+ "\tPUSH\tHL\n"		// Block wurde freigegeben
		+ "\tCALL\tMFREE_4\n"		// ggf. verschmelzen
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MRGC_3\n"
		+ "\tPUSH\tHL\n"		// zurueck zu vorherigen Block
		+ "\tPOP\tIY\n"
		+ "\tCALL\tMFREE_4\n"		// ggf. verschmelzen
		+ "MRGC_3:\tPUSH\tIY\n"		// naechster Block
		+ "\tPOP\tHL\n"
		+ "\tLD\tE,(IY+0)\n"
		+ "\tLD\tD,(IY+1)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,MRGC_4\n"
		+ "\tPUSH\tDE\n"
		+ "\tPOP\tIY\n"
		+ "\tJR\tMRGC_1\n"
		+ "MRGC_4:\tPOP\tIY\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.HEAP );
      compiler.addLibItem( LibItem.MFREE );
    }
    if( compiler.usesLibItem( LibItem.MFREE ) ) {
      /*
       * Freigeben eines allokierten Speicherbereichs
       *
       * Parameter:
       *   DE: Zeiger auf den allokierten Speicherbereich
       */
      buf.append( "MFREE:\tCALL\tCHECK_DE_WITHIN_HEAP\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tIY\n"		// HL: Heap-Anfang
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tBC,0000H\n"
		+ "\tJR\tMFREE_2\n"
		+ "MFREE_1:\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"			// BC: vorheriger Block
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "MFREE_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MFREE_3\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,MFREE_1\n"
		+ "\tADD\tHL,DE\n"		// Block gefunden
		+ "\tPUSH\tHL\n"
		+ "\tPOP\tIY\n"	
		+ "\tLD\tA,(IY+4)\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\t(IY+4),A\n"		// Bit 7 loeschen
		+ "\tJR\tZ,MFREE_3\n"		// Block war nicht belegt
		+ "\tDEC\t(IY+4)\n"		// Referenzzaehler verringern
		+ "\tJR\tNZ,MFREE_3\n"		// Block immer noch nicht frei
		+ "\tPUSH\tBC\n"		// vorheriger Block
		+ "\tCALL\tMFREE_4\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,MFREE_3\n"
		+ "\tPUSH\tBC\n"
		+ "\tPOP\tIY\n"
		+ "\tCALL\tMFREE_4\n"
		+ "MFREE_3:\n"
		+ "\tPOP\tIY\n"
		+ "\tRET\n"
      /*
       * Block in IY mit dem nachfolgenden Block verschmelzen,
       * sofern moeglich,
       * IY wird nicht zerstoert.
       * Parameter:
       *   IY: Blockadresse
       */
		+ "MFREE_4:\n"
		+ "\tLD\tA,(IY+4)\n"		// Block frei?
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"			// Block nicht frei
		+ "\tLD\tL,(IY+0)\n"
		+ "\tLD\tH,(IY+1)\n"		// HL: naechster Block
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"			// kein naechster Block
		+ "\tLD\tE,(HL)\n"		// DE: uebernaechster Block
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tC,(HL)\n"		// BC: Nutzgroesse des
		+ "\tINC\tHL\n"			//     naechsten Blocks
		+ "\tLD\tB,(HL)\n"
		+ "\tINC\tHL\n"	
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"			// naechster Block belegt
		+ "\tLD\t(IY+0),E\n"		// verschmelzen
		+ "\tLD\t(IY+1),D\n"
		+ "\tLD\tL,(IY+2)\n"
		+ "\tLD\tH,(IY+3)\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tBC,0005\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\t(IY+2),L\n"
		+ "\tLD\t(IY+3),H\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.CHECK_DE_WITHIN_HEAP );
      compiler.addLibItem( LibItem.HEAP );
    }
    if( compiler.usesLibItem( LibItem.CHECK_DE_WITHIN_HEAP ) ) {
      /*
       * Pruefen, ob die in DE enthaltene Adresse im Heap liegt,
       * DE bleibt erhalten
       * 
       * Parameter:
       *   DE: Zeiger auf den zu pruefenden Speicherbereich
       *
       * Rueckgabewert:
       *
       *   CY=0: Adresse im Heap, HL: Heap-Anfang
       *   CY=1: Adresse ausserhalb des Heaps
       */
      buf.append( "CHECK_DE_WITHIN_HEAP:\n"
		+ "\tLD\tHL,HEAPB\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tZ,CHECK_DE_WITHIN_HEAP1\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tLD\tHL,HEAPE\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "CHECK_DE_WITHIN_HEAP1:\n"
		+ "\tLD\tHL,HEAPB\n"
		+ "\tRET\n" );
    }
    /*
     * Funktionen, die auch von der Decimal-Bibliothek
     * benoetigt werden
     */
    if( compiler.usesLibItem( LibItem.F_I2_SGN_I4 ) ) {
      /*
       * Ermitteln des Vorzeichens
       *
       * Parameter:
       *   HL: zu pruefender Wert
       * Rueckgabe:
       *   HL: -1, 0 oder +1
       */
      buf.append( "F_I2_SGN_I4:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tEXX\n"
		+ "\tOR\tH\n"
		+ "\tJR\tF_I2_SGN_I2_1\n" );
      compiler.addLibItem( LibItem.F_I2_SGN_I2 );
      // direkt weiter mit F_I2_SGN_I2 !!!
    }
    if( compiler.usesLibItem( LibItem.F_I2_SGN_I2 ) ) {
      /*
       * Ermitteln des Vorzeichens
       *
       * Parameter:
       *   HL: zu pruefender Wert
       * Rueckgabe:
       *   HL: -1, 0 oder +1
       */
      buf.append( "F_I2_SGN_I2:\n"
		+ "\tLD\tA,H\n"
		+ "F_I2_SGN_I2_1:\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,H\n"
		+ "F_I2_SGN_I2_2:\n"
		+ "\tOR\tA\n"
		+ "\tLD\tHL,0001H\n"
		+ "\tRET\tP\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.E_DIV_BY_0 ) ) {
      /*
       * Ausgabe der Fehlermeldung "Division durch 0"
       */
      buf.append( "E_DIV_BY_0:\n" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Division durch 0\'\n" );
      } else {
	buf.append( "\tDB\t\'Division by zero\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_IDX_OUT_OF_RANGE ) ) {
      buf.append( "E_IDX_OUT_OF_RANGE:" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Index ausserhalb des Bereichs\'\n" );
      } else {
	buf.append( "\tDB\t\'Index out of range\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_INVALID_PARAM ) ) {
      buf.append( "E_INVALID_PARAM:\n" );
      target.appendSwitchToTextScreenTo( buf );
      String msg = (compiler.isLangCode( "DE" ) ?
					TEXT_INVALID_PARAM_DE
					: TEXT_INVALID_PARAM_EN);
      String label = compiler.lazyGetStringLiteralLabel( msg );
      if( label != null ) {
	buf.append_LD_HL_xx( label );
	buf.append( "\tCALL\tXOUTS\n" );
	compiler.addLibItem( LibItem.XOUTS );
      } else {
	buf.append( "\tCALL\tXOUTST\n" );
	buf.appendStringLiteral( msg, "00H" );
	compiler.addLibItem( LibItem.XOUTST );
      }
      buf.append( "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
    }
    if( compiler.usesLibItem( LibItem.E_OUT_OF_DATA ) ) {
      buf.append( "E_OUT_OF_DATA:" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Keine Daten mehr\'\n" );
      } else {
	buf.append( "\tDB\t\'Out of data\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_NEXT_WITHOUT_FOR ) ) {
      buf.append( "E_NEXT_WITHOUT_FOR:" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'NEXT ohne FOR\'\n" );
      } else {
	buf.append( "\tDB\t\'NEXT without FOR\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_NUMERIC_OVERFLOW ) ) {
      buf.append( "E_NUMERIC_OVERFLOW:\n" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Numerischer Ueberlauf\'\n" );
      } else {
	buf.append( "\tDB\t\'Numeric overflow\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_RET_WITHOUT_GOSUB ) ) {
      buf.append( "E_RET_WITHOUT_GOSUB:" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'RETURN ohne GOSUB\'\n" );
      } else {
	buf.append( "\tDB\t\'RETURN without GOSUB\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_USR_FUNCTION_NOT_DEFINED ) ) {
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "E_USR_FUNCTION_NOT_DEFINED:\n"
		+ "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'USR-Funktion nicht definiert\'\n" );
      } else {
	buf.append( "\tDB\t\'USR function not defined\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_TYPE_MISMATCH ) ) {
      buf.append( "E_TYPE_MISMATCH:" );
      target.appendSwitchToTextScreenTo( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Falscher Datentyp\'\n" );
      } else {
	buf.append( "\tDB\t\'Type mismatch\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      compiler.addLibItem( LibItem.E_EXIT );
      compiler.addLibItem( LibItem.XOUTST );
    }
    if( compiler.usesLibItem( LibItem.E_EXIT ) ) {
      /*
       * Programmbeendigung aufgrund eines schweren Fehlers
       */
      buf.append( "E_EXIT:\n" );
      if( options.getPrintLineNumOnAbort() ) {
	buf.append( "\tLD\tHL,(M_SRC_LINE)\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tNZ,E_EXIT_2\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTST\n" );
	if( compiler.usesLibItem( LibItem.M_SOURCE_NAME ) ) {
	  buf.append( "\tDB\t\' in \'\n"
		+ "\tDB\t00H\n"
		+ "\tLD\tHL,(M_SOURCE_NAME)\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,E_EXIT_1\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tCALL\tXOUTST\n"
		+ "\tDB\t\', \'\n"
		+ "\tDB\t00H\n"
		+ "E_EXIT_1:\n"
		+ "\tCALL\tXOUTST\n" );
	  if( compiler.isLangCode( "DE" ) ) {
	    buf.append( "\tDB\t\'Zeile\'\n" );
	  } else {
	    buf.append( "\tDB\t\'line\'\n" );
	  }
	} else {
	  if( compiler.isLangCode( "DE" ) ) {
	    buf.append( "\tDB\t\' in Zeile\'\n" );
	  } else {
	    buf.append( "\tDB\t\' in line\'\n" );
	  }
	}
	buf.append( "\tDB\t00H\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tS_STR_I2\n"
		+ "\tCALL\tXOUTS\n" );
	if( compiler.usesLibItem( LibItem.M_BASIC_LINE_NUM ) ) {
	  buf.append( "\tLD\tHL,(M_BASIC_LINE_NUM)\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tNZ,E_EXIT_2\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTST\n" );
	  if( compiler.isLangCode( "DE" ) ) {
	    buf.append( "\tDB\t\' (BASIC-Zeile\'\n" );
	  } else {
	    buf.append( "\tDB\t\' (BASIC line\'\n" );
	  }
	  buf.append( "\tDB\t00H\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tS_STR_I2\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tLD\tA,29H\n"
		+ "\tCALL\tXOUTCH\n" );
	}
	buf.append( "E_EXIT_2:\n" );
	compiler.addLibItem( LibItem.S_STR_I2 );
	compiler.addLibItem( LibItem.XOUTCH );
	compiler.addLibItem( LibItem.XOUTS );
	compiler.addLibItem( LibItem.XOUTST );
      }
      buf.append( "\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      compiler.addLibItem( LibItem.XOUTNL );
    }
    if( compiler.usesLibItem( LibItem.S_STR_I4 ) ) {
      /*
       * Umwandlung eines numerischen 32-Bit-Wertes
       * in eine Zeichenkette mit einer ganzen Dezimalzahl.
       * Das erste Zeichen enthaelt entweder das Vorzeichen
       * oder ein Leerzeichen.
       *
       * Parameter:
       *   DEHL: numerischer Wert
       * Rueckgabewert:
       *   HL:   Zeiger auf die Zeichenkette
       */
      buf.append( "S_STR_I4:\n"
		+ "\tLD\tA,20H\n"		// Leerzeichen
		+ "\tBIT\t7,D\n"		// kleiner 0 ?
		+ "\tJR\tZ,S_STR_I4_1\n"
		+ "\tCALL\tI4_NEG_DEHL_RAW\n"
		+ "\tJP\tC,S_STR_I4_5\n"
		+ "\tLD\tA,2DH\n"		// Minuszeichen
		+ "S_STR_I4_1:\n"
		+ "\tLD\tB,00H\n"		// keine Nullen
		+ "\tPUSH\tDE\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,M_CVTBUF\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tDE,3B9AH\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0CA00H\n"		// DE'DE=1000000000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,05F5H\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0E100H\n"		// DE'DE=100000000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0098H\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,9680H\n"		// DE'DE=10000000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,000FH\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,4240H\n"		// DE'DE=1000000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0001H\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,86A0H\n"		// DE'DE=100000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "S_STR_I4_2:\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,2710H\n"		// DE'DE=10000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tLD\tDE,03E8H\n"		// DE'DE=1000
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tLD\tDE,0064H\n"		// DE'DE=100
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tLD\tDE,000AH\n"		// DE'DE=10
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tLD\tDE,0001H\n"		// DE'DE=1
		+ "\tLD\tB,01H\n"		// Null ausgeben
		+ "\tCALL\tS_STR_I4_3\n"
		+ "\tEXX\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(BC),A\n"
		+ "\tLD\tHL,M_CVTBUF\n"
		+ "\tRET\n"
		+ "S_STR_I4_3:\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tOR\tA\n"			// CY=0
		+ "S_STR_I4_4:\n"
		+ "\tINC\tA\n"			// CY unveraendert
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,S_STR_I4_4\n"
		+ "\tADD\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tADC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tLD\tC,A\n"
		+ "\tOR\tB\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,30H\n"		// '0'
		+ "\tLD\tB,A\n"
		+ "\tADD\tA,C\n"
		+ "\tEXX\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tEXX\n"
		+ "\tRET\n"
		+ "S_STR_I4_5:\n"
		+ "\tLD\tHL,S_STR_I4_6\n"
		+ "\tRET\n"
		+ "S_STR_I4_6:\n"
		+ "\tDB\t'-2147483648'\n"
		+ "\tDB\t00H\n" );
      compiler.addLibItem( LibItem.I4_NEG_DEHL_RAW );
      compiler.addLibItem( LibItem.M_CVTBUF );
    }
    if( compiler.usesLibItem( LibItem.S_STR_I2 ) ) {
      /*
       * Umwandlung eines numerischen 16-Bit-Wertes
       * in eine Zeichenkette mit einer ganzen Dezimalzahl.
       * Das erste Zeichen enthaelt entweder das Vorzeichen
       * oder ein Leerzeichen.
       *
       * Parameter:
       *   HL: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_STR_I2:\n"
		+ "\tLD\tA,20H\n"		// Leerzeichen
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,S_STR_I2_1\n"
		+ "\tCALL\tI2_NEG_HL_RAW\n"
		+ "\tJR\tC,S_STR_I2_4\n"
		+ "\tLD\tA,2DH\n"		// Minuszeichen
		+ "S_STR_I2_1:\n" );
      if( compiler.usesLibItem( LibItem.S_STR_I4 ) ) {
	buf.append( "\tEXX\n"
		+ "\tLD\tBC,M_CVTBUF\n"
		+ "\tLD\t(BC),A\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tEXX\n"			// HL'=0
		+ "\tLD\tB,00H\n"		// keine Nullen
		+ "\tJP\tS_STR_I4_2\n" );
      } else {
	buf.append( "\tEXX\n"
		+ "\tLD\tHL,M_CVTBUF\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tB,00H\n"		// keine Nullen
		+ "\tLD\tDE,2710H\n"		// 10000
		+ "\tCALL\tS_STR_I2_2\n"
		+ "\tLD\tDE,03E8H\n"		// 1000
		+ "\tCALL\tS_STR_I2_2\n"
		+ "\tLD\tDE,0064H\n"		// 100
		+ "\tCALL\tS_STR_I2_2\n"
		+ "\tLD\tDE,000AH\n"		// 10
		+ "\tCALL\tS_STR_I2_2\n"
		+ "\tLD\tDE,0001H\n"		// 1
		+ "\tLD\tB,01H\n"		// Null ausgeben
		+ "\tCALL\tS_STR_I2_2\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tLD\tHL,M_CVTBUF\n"
		+ "\tRET\n"
		+ "S_STR_I2_2:\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tOR\tA\n"			// CY=0
		+ "S_STR_I2_3:\n"
		+ "\tINC\tA\n"			// CY unveraendert
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,S_STR_I2_3\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tC,A\n"
		+ "\tOR\tB\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,30H\n"		// 0
		+ "\tLD\tB,A\n"
		+ "\tADD\tA,C\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
      }
      buf.append( "S_STR_I2_4:\n"
		+ "\tLD\tHL,S_STR_I2_5\n"
		+ "\tRET\n"
		+ "S_STR_I2_5:\n"
		+ "\tDB\t'-32768'\n"
		+ "\tDB\t00H\n" );
      compiler.addLibItem( LibItem.I2_NEG_HL_RAW );
      compiler.addLibItem( LibItem.M_CVTBUF );
    }
    if( compiler.usesLibItem( LibItem.I4_NEG_DEHL_RAW ) ) {
      /*
       * DEHL mathematisch negieren
       * ohne Ausgabe einer Fehlermeldung bei Ueberlaufpruefung
       *
       * Parameter:
       *   DEHL: Eingangswert
       * Rueckgabe:
       *   DEHL: Ergebnis
       *   CY=1: Ueberlauf (nur bei Wert 80000000h)
       */
      buf.append( "I4_NEG_DEHL_RAW:\n"
		+ "\tLD\tA,D\n"
		+ "\tXOR\t80H\n"
		+ "\tOR\tE\n"
		+ "\tOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"		// 80000000h -> Ueberlauf
		+ "\tXOR\tA\n"		// CY=0
		+ "\tSUB\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,00H\n"
		+ "\tSBC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,00H\n"
		+ "\tSBC\tA,E\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,00H\n"
		+ "\tSBC\tA,D\n"
		+ "\tLD\tD,A\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_NEG_HL_RAW ) ) {
      /*
       * HL mathematisch negieren
       * ohne Ausgabe einer Fehlermeldung bei Ueberlaufpruefung
       *
       * Parameter:
       *   HL: Eingangswert
       * Rueckgabe:
       *   HL:   Ergebnis
       *   CY=1: Ueberlauf (nur bei Wert 8000h)
       */
      buf.append( "I2_NEG_HL_RAW:\n"
		+ "\tLD\tA,H\n"
		+ "\tXOR\t80H\n"
		+ "\tOR\tL\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"		// 8000h -> Ueberlauf
		+ "\tXOR\tA\n"		// CY=0
		+ "\tSUB\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,00H\n"
		+ "\tSBC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_HL2HL_TO_DEHL ) ) {
      /*
       * HL'HL -> DEHL
       * Flags werden nicht geaendert.
       */
      buf.append( "I4_HL2HL_TO_DEHL:\n"
		+ "\tEXX\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I4_POP_DE2HL2 ) ) {
      /*
       * Die Routine liest vom Stack 4 Bytes und schreibt sie in DE'HL'.
       *
       * Parameter:
       *   Stack: zu lesender Wert
       * Rueckgabe:
       *   DE'HL': gelesener Wert
       */
      buf.append( "I4_POP_DE2HL2:\n"
		+ "\tEXX\n"
		+ "\tPOP\tBC\n"			// Return-Adresse
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPUSH\tBC\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_APPEND_DIGIT_TO_HL ) ) {
      /*
       * Dezimalziffer an HL anhaengen: HL = (HL * 10) + A
       *
       * Parameter:
       *   A:  anzuhaengende Ziffer
       *   HL: alter Wert
       * Rueckgabe:
       *   HL:   Ergebnis
       *   CY=1: Ueberlauf (Ergebnis negativ)
       */
      buf.append( "I2_APPEND_DIGIT_TO_HL:\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,00H\n"
		+ "\tADC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tRLCA\n"		// Vorzeichen in CY
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.I2_ERR )
	|| compiler.usesLibItem( LibItem.I4_ERR ) )
    {
      buf.append( "I2_ERR_SET_NUMERIC_OVERFLOW:\n"
		+ "I4_ERR_SET_NUMERIC_OVERFLOW:\n" );
      BasicUtil.appendSetErrorNumericOverflow( compiler );
      buf.append( "\tJR\tERR_SET_NULL\n"
		+ "I2_ERR_SET_INVALID_CHARS:\n"
		+ "I4_ERR_SET_INVALID_CHARS:\n" );
      BasicUtil.appendSetErrorInvalidChars( compiler );
      buf.append( "ERR_SET_NULL:\n" );
      if( compiler.usesLibItem( LibItem.I4_ERR ) ) {
	buf.append( "\tLD\tDE,0000H\n" );
      }
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.CHECK_DEHL_FOR_I2 ) ) {
      /*
       * Pruefen, ob die uebergebene Int4-Zahl auch als Int2 darstellbar ist
       *
       * Parameter:
       *   DEHL:    zu pruefender Wert
       * Rueckgabe:
       *   CY-Flag: 0=ja, 1=nein
       */
      buf.append( "CHECK_DEHL_FOR_I2:\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tNZ,CHECK_DEHL_FOR_I2_2\n"
      /*
       * Bit 15 nicht gesetzt,
       * demzufolge duerfen auch die Bits 16-31 nicht gesetzt sein.
       */
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"			// CY=0
		+ "\tRET\tZ\n"
      // zu gross fuer I2 -> CY=1
		+ "CHECK_DEHL_FOR_I2_1:\n"
		+ "\tSCF\n"			// CY=1
		+ "\tRET\n"
      /*
       * Bit 15 gesetzt,
       * demzufolge muessen auch die Bits 16-31 gesetzt sein.
       */
		+ "CHECK_HL2HL_FOR_I2_2:\n"
		+ "\tINC\tD\n"
		+ "\tJR\tNZ,CHECK_HL2HL_FOR_I2_1\n"
		+ "\tINC\tE\n"
		+ "\tJR\tNZ,CHECK_HL2HL_FOR_I2_1\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.ADD_SP_N ) ) {
      /*
       * Die Routine springt zu der Adresse, die im HL-Register steht
       *
       * Parameter:
       *   HL: Adresse
       */
      buf.append( "ADD_SP_N:\n"
		+ "\tEXX\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tPOP\tDE\n"			// Return-Adresse
		+ "\tLD\tL,A\n"
		+ "\tRLCA\n"
		+ "\tSBC\tA,A\n"
		+ "\tLD\tH,A\n"
		+ "\tADD\tHL,SP\n"
		+ "\tLD\tSP,HL\n"
		+ "\tPUSH\tDE\n"		// Return-Adresse
		+ "\tEXX\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.CLEAR_MEM ) ) {
      /*
       * Die Routine beschreibt den Speicherbereich, auf den HL zeigt,
       * mit sovielen Nullbytes, wie in BC angegeben ist.
       *
       * Parameter:
       *   B:  Anzahl der zu initialisierenden Bytes (>0)
       *   HL: Anfangsadresse des zu initialisierenden Speicherbereichs
       */
      buf.append( "CLEAR_MEM:\n"
		+ "\tXOR\tA\n"
		+ "CLEAR_MEM_1:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tCLEAR_MEM_1\n"
		+ "\tRET\n" );
    }
    if( compiler.usesLibItem( LibItem.JP_HL ) ) {
      /*
       * Sprung zu der Adresse, die im HL-Register steht
       *
       * Parameter:
       *   HL: Adresse
       */
      buf.append( "JP_HL:\tJP\t(HL)\n" );
    }
    if( compiler.usesLibItem( LibItem.OUTSP ) ) {
      /*
       * Ausgabe eines Leerzeichens auf dem Bildschirm
       */
      buf.append( "OUTSP:\tLD\tA,20H\n"
		+ "\tJP\tXOUTCH\n" );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.DATA ) ) {
      /*
       * Leseposition fuer Daten auf den Anfang des Datenbereichs setzen,
       */
      buf.append( "DATA_INIT:\n"
		+ "\tLD\tHL,DATA_POOL\n"
		+ "\tLD\t(M_READ),HL\n"
		+ "\tRET\n" );
    }
    buf.append( "XEXIT:" );
    if( compiler.usesLibItem( LibItem.IOCTB1 ) ) {
      buf.append( "\tLD\tHL,IOCTB1\n"
		+ "\tCALL\tCKCLOS\n" );
    }
    if( compiler.usesLibItem( LibItem.IOCTB2 ) ) {
      buf.append( "\tLD\tHL,IOCTB2\n"
		+ "\tCALL\tCKCLOS\n" );
    }
    target.appendPreExitTo( buf );
    buf.append( "\tLD\tSP,(M_OLD_STACK)\n" );
    target.appendExitTo( buf, options.isAppTypeSubroutine() );
    if( compiler.usesLibItem( LibItem.IOCTB1 )
	|| compiler.usesLibItem( LibItem.IOCTB2 ) )
    {
      buf.append( "CKCLOS:\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tH\n"
		+ "\tRET\tZ\n"
		+ "\tJP\t(HL)\n" );
    }
    target.appendInputTo(
		buf,
		compiler.usesLibItem( LibItem.XCHECK_BREAK ),
		compiler.usesLibItem( LibItem.XINKEY ),
		compiler.usesLibItem( LibItem.XINCH ),
		options.canBreakOnInput() );
    if( compiler.usesLibItem( LibItem.XBREAK )
	|| options.canBreakOnInput() )
    {
      // Zeilenumbruch und Programm beenden
      buf.append( "XBREAK:\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      compiler.addLibItem( LibItem.XOUTST );
      compiler.addLibItem( LibItem.XOUTNL );
    }
    if( compiler.usesLibItem( LibItem.XSCREEN ) ) {
      target.appendXScreenTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XBORDER ) ) {
      target.appendXBorderTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XCOLOR ) ) {
      target.appendXColorTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XINK ) ) {
      target.appendXInkTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XPAPER ) ) {
      target.appendXPaperTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XCLS ) ) {
      target.appendXClsTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XCRSLIN ) ) {
      target.appendXCrsLinTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XCRSPOS ) ) {
      target.appendXCrsPosTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XLOCATE ) ) {
      target.appendXLocateTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XCURSOR ) ) {
      target.appendXCursorTo( buf );
    }
    // XPEN vor den anderen Grafikroutinen hinzufuegen!
    if( compiler.usesLibItem( LibItem.XPEN ) ) {
      target.appendXPenTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XHLINE ) ) {
      target.appendXHLineTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XPAINT ) ) {
      target.appendXPaintTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XPRES ) ) {
      target.appendXPResTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XPSET ) ) {
      target.appendXPSetTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XPTEST ) ) {
      target.appendXPTestTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XPOINT ) ) {
      target.appendXPointTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.XJOY ) ) {
      target.appendXJoyTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XDATETIME ) ) {
      target.appendXDateTimeTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XDOSTIME ) ) {
      target.appendXDosTimeTo( buf );
    }
    target.appendEtcPreXOutTo( buf, compiler );
    if( compiler.usesLibItem( LibItem.XOUTST ) ) {
      /*
       * Ausgabe eines mit einem Nullbyte abgeschlossenen Textes
       * auf dem Bildschirm,
       * der sich unmittelbar an den Funktionsaufruf anschliesst.
       */
      buf.append( "XOUTST:\tEX\t(SP),HL\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( LibItem.XOUTS );
    }
    if( compiler.usesLibItem( LibItem.XOUTS ) ) {
      /*
       * Ausgabe eines mit einem Nullbyte abgeschlossenen Textes
       * auf dem Bildschirm
       *
       * Parameter:
       *   HL: Adresse des Textes
       * Rueckgabe:
       *   HL: erstes Byte hinter dem abschliessenden Nullbyte
       */
      buf.append( "XOUTS:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tXOUTS\n" );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.XOUTNL ) ) {
      target.appendXOutnlTo( buf );
      compiler.addLibItem( LibItem.XOUTCH );
    }
    if( compiler.usesLibItem( LibItem.XOUTCH ) ) {
      target.appendXOutchTo( buf );
    }
    if( compiler.usesLibItem( LibItem.XLPTCH ) ) {
      target.appendXLPtchTo( buf );
    }
    target.appendEtcPastXOutTo( buf );
    buf.append( SUB_RESET_ERROR_BEG );
    if( compiler.usesLibItem( LibItem.M_ERROR_NUM ) ) {
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tLD\t(M_ERROR_NUM),HL\n" );
    }
    if( compiler.usesLibItem( LibItem.M_ERROR_TEXT ) ) {
      buf.append_LD_HL_xx( EMPTY_STRING_LABEL );
      buf.append( "\tLD\t(M_ERROR_TEXT),HL\n" );
      compiler.addLibItem( LibItem.EMPTY_STRING );
    }
    buf.append( SUB_RESET_ERROR_END );
  }


  public static void appendDataTo(
			BasicCompiler      compiler,
			Map<String,String> strLitLabelMap,
			StringBuilder      userData )
  {
    AsmCodeBuf     buf     = compiler.getCodeBuf();
    BasicOptions   options = compiler.getBasicOptions();
    AbstractTarget target  = compiler.getTarget();

    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Datenbereich\n" );
    }
    buf.append( compiler.getSysDataOut() );
    if( compiler.usesLibItem( LibItem.IOOPEN ) ) {
      StringBuilder handlerLog = new StringBuilder( 32 );
      buf.append( "IO_HANDLER_TAB:\n" );
      if( compiler.usesLibItem( LibItem.IO_DISK_HANDLER ) ) {
	String diskHandlerLabel = target.getDiskHandlerLabel();
	if( diskHandlerLabel != null ) {
	  if( !diskHandlerLabel.isEmpty() ) {
	    buf.append( "\tDW\t" );
	    buf.append( diskHandlerLabel );
	    buf.newLine();
	    handlerLog.append( "DISK" );
	  }
	}
      }
      if( compiler.usesLibItem( LibItem.IO_VDIP_HANDLER ) ) {
	boolean vdipAppended = false;
	if( target.getVdipBaseIOAddresses() != null ) {
	  buf.append( "\tDW\tIO_VDIP_HANDLER\n" );
	  vdipAppended = true;
	} else {
	  String vdipHandlerLabel = target.getVdipHandlerLabel();
	  if( vdipHandlerLabel != null ) {
	    if( !vdipHandlerLabel.isEmpty() ) {
	      buf.append( "\tDW\t" );
	      buf.append( vdipHandlerLabel );
	      buf.newLine();
	      vdipAppended = true;
	    }
	  }
	}
	if( vdipAppended ) {
	  if( handlerLog.length() > 0 ) {
	    handlerLog.append( ", " );
	  }
	  handlerLog.append( "VDIP" );
	}
      }
      if( compiler.usesLibItem( LibItem.IO_CRT_HANDLER ) ) {
	buf.append( "\tDW\tIO_CRT_HANDLER\n" );
	if( handlerLog.length() > 0 ) {
	  handlerLog.append( ", " );
	}
	handlerLog.append( "CRT" );
      }
      if( compiler.usesLibItem( LibItem.IO_LPT_HANDLER ) ) {
	buf.append( "\tDW\tIO_LPT_HANDLER\n" );
	if( handlerLog.length() > 0 ) {
	  handlerLog.append( ", " );
	}
	handlerLog.append( "LPT" );
      }
      buf.append( "\tDW\t0000H\n" );
      PrgLogger logger = compiler.getLogger();
      if( logger != null ) {
	if( handlerLog.length() > 0 ) {
	  logger.appendToOutLog( "Eingebundene Treiber f\u00FCr OPEN: " );
	  logger.appendToOutLog( handlerLog.toString() );
	} else {
	  logger.appendToOutLog(
		"Warnung: Keine Treiber f\u00FCr OPEN eingebunden" );
	}
	logger.appendToOutLog( "\n" );
      }
    }
    Set<Map.Entry<String,String>> entries = strLitLabelMap.entrySet();
    if( entries != null ) {
      for( Map.Entry<String,String> e : entries ) {
	String label = e.getValue();
	if( label != null ) {
	  if( !label.isEmpty() ) {
	    buf.append( label );
	    buf.append( ':' );
	    if( label.length() > 6 ) {
	      buf.newLine();
	    }
	    buf.appendStringLiteral( e.getKey() );
	  }
	}
      }
    }
    if( compiler.usesLibItem( LibItem.IO_CRT_HANDLER ) ) {
      buf.append( "D_IOCRT:\n"
			+ "\tDB\t\'CRT:\'\n" );
    }
    if( compiler.usesLibItem( LibItem.IO_LPT_HANDLER ) ) {
      buf.append( "D_IOLPT:\n"
			+ "\tDB\t\'LPT:\'\n" );
    }
    if( compiler.usesLibItem( LibItem.IO_VDIP_HANDLER )
	&& (target.getVdipBaseIOAddresses() != null) )
    {
      VdipLibrary.appendDataTo( compiler );
    }
    if( compiler.usesLibItem( LibItem.EMPTY_STRING ) ) {
      buf.append( EMPTY_STRING_LABEL );
      buf.append( ":\n"
		+ "\tDB\t00H\n" );
    }
    GraphicsLibrary.appendDataTo( compiler );
    target.appendDataTo( buf );
    if( userData != null ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;User DATA\n" );
      }
      buf.append( userData );
    }
  }


  public static void appendBssTo(
			AsmCodeBuf          buf,
			BasicCompiler       compiler,
			AsmCodeBuf          varBssBuf,
			Set<String>         usrLabels,
			StringBuilder       userBSS )
  {
    BasicOptions   options = compiler.getBasicOptions();
    AbstractTarget target  = compiler.getTarget();

    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Speicherzellen\n" );
    }
    if( compiler.usesLibItem( LibItem.M_SOURCE_NAME ) ) {
      buf.append( "M_SOURCE_NAME:\tDS\t2\n" );
    }
    if( options.getPrintLineNumOnAbort() ) {
      buf.append( "M_SRC_LINE:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( LibItem.M_BASIC_LINE_NUM ) ) {
      buf.append( "M_BASIC_LINE_NUM:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_RETVAL_6 ) ) {
      buf.append( "M_RETVAL:\tDS\t6\n" );
    } else if( compiler.usesLibItem( BasicLibrary.LibItem.M_RETVAL_4 ) ) {
      buf.append( "M_RETVAL:\tDS\t4\n" );
    } else if( compiler.usesLibItem( BasicLibrary.LibItem.M_RETVAL_2 ) ) {
      buf.append( "M_RETVAL:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_OP1_6 ) ) {
      buf.append( "M_OP1:\tDS\t6\n" );
    } else if( compiler.usesLibItem( BasicLibrary.LibItem.M_OP1_4 ) ) {
      buf.append( "M_OP1:\tDS\t4\n" );
    }
    if( compiler.usesLibItem( LibItem.DATA ) ) {
      /*
       * Speicherzelle fuer Leseposition anlegen
       */
      buf.append( "M_READ:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( LibItem.F_I2_RND_I2 ) ) {
      /*
       * Speicherzellen fuer den Zufallsgenerator
       */
      buf.append( "M_RNDA:\tDS\t2\n"		// Leseposition
		+ "M_RNDX:\tDS\t1\n" );		// Schieberegister
    }
    if( compiler.usesLibItem( LibItem.IO_VDIP_HANDLER )
	&& (target.getVdipBaseIOAddresses() != null) )
    {
      VdipLibrary.appendBssTo( buf, compiler );
    }
    if( compiler.usesLibItem( LibItem.IOOPEN )
	|| compiler.usesLibItem( LibItem.IOCTB1 )
	|| compiler.usesLibItem( LibItem.IOCTB2 ) )
    {
      buf.append( "IO_M_NAME:\tDS\t2\n"
		+ "IO_M_CADDR:\tDS\t2\n"
		+ "IO_M_ACCESS:\tDS\t1\n" );
    }
    if( compiler.usesLibItem( LibItem.IO_M_COUT ) ) {
      buf.append( "IO_M_COUT:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( LibItem.IOCTB1 ) ) {
      /*
       * Zeigerfeld fuer Kanal 1:
       *   +0:  CLOSE-Routine
       *          In:   DE: Anfangsadresse Kanalzeigerfeld
       *          Info:
       *            Das Vorhandensein der CLOSE-Routine bestimmt,
       *            ob der Kanal offen oder geschlossen ist, d.h.,
       *            beim Oeffnen eines Kanals muss eine CLOSE-Routine
       *            eingetragen werden.
       *            Die CLOSE-Routine braucht das Zeigerfeld nicht aufraeumen.
       *            Das macht die allgemeine CLOSE-Anweisung.
       *   +2:  EOF-Routine
       *          In:  DE:    Anfangsadresse Kanalzeigerfeld
       *          Out: HL=-1: EOF erreicht
       *   +4:  READ-Routine
       *          In:  DE: Anfangsadresse Kanalzeigerfeld
       *          Out: A:  gelesenes Zeichen bzw. 00h ab EOF
       *   +6:  WRITE-Routine
       *          In:  (IO_M_CADDR): Anfangsadresse Kanalzeigerfeld
       *               A:            zu schreibendes Byte
       *   +8:  Status Zeichenpuffer
       *   +9:  Zeichenpuffer
       *   +10: Treiber-spezifisches Feld
       */
      buf.append( "IOCTB1:\tDS\t" );
      buf.appendHex2( compiler.getIOChannelSize() );
      buf.newLine();
    }
    if( compiler.usesLibItem( LibItem.IOCTB2 ) ) {
      /*
       * Zeigerfeld fuer Kanal 2,
       * muss unmittelbar hinter Kanal 1 folgen
       */
      buf.append( "IOCTB2:\tDS\t" );
      buf.appendHex2( compiler.getIOChannelSize() );
      buf.newLine();
    }
    DecimalLibrary.appendBssTo( buf, compiler );
    GraphicsLibrary.appendBssTo( buf, compiler );
    if( compiler.usesLibItem( LibItem.M_KEY_BUF ) ) {
      buf.append( "M_KEY_BUF:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( LibItem.M_TMP_STR_BUF ) ) {
      // temporaerer String-Puffer
      buf.append( "M_TMP_STR_BUF:\tDS\t" );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN + 1 );
      buf.newLine();
    }
    if( compiler.usesLibItem( LibItem.M_CVTBUF ) ) {
      // Puffer fuer S_STR_D6, S_STR_I2 und S_STR_I4
      if( compiler.usesLibItem( LibItem.S_STR_D6 )
	  || compiler.usesLibItem( LibItem.S_STR_D6MEM ) )
      {
	buf.append( "M_CVTBUF:\tDS\t14\n" );
      } else if( compiler.usesLibItem( LibItem.S_STR_I4 ) ) {
	buf.append( "M_CVTBUF:\tDS\t12\n" );
      } else {
	buf.append( "M_CVTBUF:\tDS\t7\n" );
      }
    }
    if( compiler.usesLibItem( LibItem.M_ERROR_NUM ) ) {
      // Fehlernummer
      buf.append( "M_ERROR_NUM:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( LibItem.M_ERROR_TEXT ) ) {
      // Fehlertext
      buf.append( "M_ERROR_TEXT:\tDS\t2\n" );
    }
    // Stack Pointer bei Programmstart
    buf.append( "M_OLD_STACK:\tDS\t2\n" );
    target.appendBssTo( buf );
    if( options.getShowAssemblerText() && !usrLabels.isEmpty() ) {
      buf.append( "\n;USR-Funktionen\n" );
    }
    for( String usrLabel : usrLabels ) {
      buf.append( usrLabel );
      buf.append( ":\tDS\t2\n" );
    }
    if( varBssBuf.length() > 0 ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;BASIC-Variablen\n" );
      }
      buf.append( varBssBuf );
    }
    if( userBSS != null ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;User BSS\n" );
      }
      buf.append( userBSS );
    }
    if( compiler.usesLibItem( LibItem.HEAP ) ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;Zeichenkettenspeicher\n" );
      }
      buf.append( "HEAPB:\tDS\t" );
      buf.appendHex4( options.getHeapSize() );
      buf.append( "\n"
		+ "HEAPE:\n" );
    }
    int stackSize = options.getStackSize();
    if( stackSize > 0 ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;Stack-Bereich\n" );
      }
      buf.append( "\tDS\t" );
      buf.appendHex4( stackSize );
      buf.newLine();
    }
    buf.append( BasicCompiler.TOP_LABEL );
    buf.append( ":\n" );
  }


  public static void appendInitTo(
			AsmCodeBuf          buf,
			BasicCompiler       compiler,
			Map<String,VarDecl> varDecls,
			SortedSet<String>   usrLabels,
			AsmCodeBuf          varBssBufOut )
  {
    BasicOptions options = compiler.getBasicOptions();

    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Initialisierungen\n" );
    }
    buf.append( BasicCompiler.START_LABEL );
    buf.append( ":\n"
		+ "\tLD\t(M_OLD_STACK),SP\n" );
    if( options.getStackSize() > 0 ) {
      buf.append( "\tLD\tSP," );
      buf.append( BasicCompiler.TOP_LABEL );
      buf.newLine();
      compiler.addLibItem( LibItem.MTOP );
    }
    if( options.getPrintLineNumOnAbort() ) {
      buf.append( "\tLD\tHL,0FFFFH\n"
		+ "\tLD\t(M_SRC_LINE),HL\n" );
      if( compiler.usesLibItem( LibItem.M_BASIC_LINE_NUM ) ) {
	buf.append( "\tLD\t(M_BASIC_LINE_NUM),HL\n" );
      }
    }
    if( compiler.usesLibItem( LibItem.M_SOURCE_NAME ) ) {
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tLD\t(M_SOURCE_NAME),HL\n" );
    }
    if( compiler.usesLibItem( LibItem.F_I2_RND_I2 ) ) {
      buf.append( "\tLD\tH,HIGH(" );
      buf.append( BasicCompiler.START_LABEL );
      if( (compiler.getBasicOptions().getCodeBegAddr() & 0x00FF) != 0 ) {
	buf.append( "+0001H" );
      }
      buf.append( ")\n"
		+ "\tLD\tA,R\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\t(M_RNDA),HL\n"
		+ "\tLD\tA,46H\n"
		+ "\tLD\t(M_RNDX),A\n" );
    }
    if( compiler.usesLibItem( LibItem.DATA ) ) {
      buf.append( "\tCALL\tDATA_INIT\n" );
    }
    if( compiler.usesLibItem( LibItem.E_USR_FUNCTION_NOT_DEFINED )
	&& (usrLabels != null) )
    {
      int nUsrLabels = usrLabels.size();
      if( nUsrLabels > 0 ) {
	if( nUsrLabels > 4 ) {
	  String firstUsrLabel = usrLabels.first();
	  String loopLabel     = compiler.nextLabel();
	  buf.append( "\tLD\tDE,E_USR_FUNCTION_NOT_DEFINED\n"
			+ "\tLD\tHL," );
	  buf.append( firstUsrLabel );
	  buf.append( "\n"
			+ "\tLD\tB," );
	  buf.appendHex2( nUsrLabels );
	  buf.newLine();
	  buf.append( loopLabel );
	  buf.append( ":\n"
			+ "\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n"
			+ "\tINC\tHL\n"
			+ "\tDJNZ\t" );
	  buf.append( loopLabel );
	  buf.newLine();
	} else {
	  buf.append( "\tLD\tHL,E_USR_FUNCTION_NOT_DEFINED\n" );
	  for( String label : usrLabels ) {
	    buf.append( "\tLD\t(" );
	    buf.append( label );
	    buf.append( "),HL\n" );
	  }
	}
      }
    }
    if( compiler.usesLibItem( LibItem.IO_VDIP_HANDLER )
	&& (compiler.getTarget().getVdipBaseIOAddresses() != null) )
    {
      VdipLibrary.appendInitTo( buf, compiler );
    }

    /*
     * Zeigerfelder der einzelnen Kanaele initialisieren
     */
    int    nChannels         = 0;
    String firstChannelLabel = null;
    if( compiler.usesLibItem( LibItem.IOCTB2 ) ) {
      firstChannelLabel = "IOCTB2";
      nChannels++;
    }
    if( compiler.usesLibItem( LibItem.IOCTB1 ) ) {
      firstChannelLabel = "IOCTB1";
      nChannels++;
    }
    if( (nChannels > 0) && (firstChannelLabel != null) ) {
      buf.append( "\tLD\tHL," );
      buf.append( firstChannelLabel );
      buf.append( "\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB," );
      buf.appendHex2( nChannels * compiler.getIOChannelSize() );
      buf.append( "\n"
		+ "IOINIL:\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tIOINIL\n" );
    }

    /*
     * weitere Initialisierungen
     */
    GraphicsLibrary.appendInitTo( buf, compiler );
    buf.append_CALL_RESET_ERROR( compiler );

    /*
     * Variablen initialisieren,
     * Dabei wird auch der BSS-Code fuer die Variablen erzeugt.
     */
    Map<String,Integer> numVarLabel2Size = new HashMap<>();
    Map<String,Integer> strVarLabel2Size = new HashMap<>();
    int                 nStrVarElems     = 0;
    for( VarDecl varDecl : varDecls.values() ) {
      int totalVarSize = varDecl.getTotalSize();
      if( varDecl.getDataType().equals( BasicCompiler.DataType.STRING ) ) {
	strVarLabel2Size.put( varDecl.getLabel(), totalVarSize );
	nStrVarElems += (totalVarSize / varDecl.getElemSize());
      } else {
	numVarLabel2Size.put( varDecl.getLabel(), totalVarSize );
      }
    }
    String firstNumVarLabel = null;
    int    numVarsTotalSize = 0;
    for( Map.Entry<String,Integer> varEntry : numVarLabel2Size.entrySet() ) {
      String  varLabel = varEntry.getKey();
      if( firstNumVarLabel == null ) {
	firstNumVarLabel = varLabel;
      }
      Integer varSize = varEntry.getValue();
      int     size    = (varSize != null ? varSize.intValue() : 2);
      appendBssLineTo( varBssBufOut, varLabel, size );
      numVarsTotalSize += size;
    }
    String firstStrVarLabel = null;
    for( Map.Entry<String,Integer> varEntry : strVarLabel2Size.entrySet() ) {
      String varLabel = varEntry.getKey();
      if( firstStrVarLabel == null ) {
	firstStrVarLabel = varLabel;
      }
      Integer varSize = varEntry.getValue();
      int     size    = (varSize != null ? varSize.intValue() : 2);
      appendBssLineTo( varBssBufOut, varLabel, size );
    }
    boolean regHLPreFirstStrVar = false;
    boolean regHLEqFirstStrVar  = false;
    if( options.getInitVars()
	&& (firstNumVarLabel != null)
	&& (numVarsTotalSize > 0) )
    {
      buf.append_LD_HL_xx( firstNumVarLabel );
      if( numVarsTotalSize > 0x100 ) {
	buf.append( "\tLD\t(HL),00H\n" );
	buf.append_LD_DE_xx( firstNumVarLabel + "+1" );
	buf.append_LD_BC_nn( numVarsTotalSize - 1 );
	buf.append( "\tLDIR\n" );
	regHLPreFirstStrVar = true;
      } else {
	buf.append_LD_B_n( numVarsTotalSize & 0xFF );
	buf.append( "INIT_VAR_1:\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tINIT_VAR_1\n" );
	regHLEqFirstStrVar = true;
      }
    }
    if( (firstStrVarLabel != null) && (nStrVarElems > 0) ) {
      if( nStrVarElems > 3 ) {
	if( regHLPreFirstStrVar ) {
	  buf.append( "\tINC\tHL\n" );
	} else if( !regHLEqFirstStrVar ) {
	  buf.append_LD_HL_xx( firstStrVarLabel );
	}
	if( nStrVarElems > 0x0100 ) {
	  buf.append_LD_BC_nn( nStrVarElems );
	} else {
	  buf.append_LD_B_n( nStrVarElems & 0xFF );
	}
	buf.append_LD_DE_xx( EMPTY_STRING_LABEL );
	buf.append( "INIT_VAR_2:\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n" );
	if( nStrVarElems > 0x0100 ) {
	  buf.append( "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,INIT_VAR_2\n" );
	} else {
	  buf.append( "\tDJNZ\tINIT_VAR_2\n" );
	}
      } else {
	buf.append_LD_HL_xx( EMPTY_STRING_LABEL );
	for( String varLabel : strVarLabel2Size.keySet() ) {
	  buf.append( "\tLD\t(" );
	  buf.append( varLabel );
	  buf.append( "),HL\n" );
	}
      }
      compiler.addLibItem( LibItem.EMPTY_STRING );
    }

    // Heap initialisieren
    if( compiler.usesLibItem( LibItem.HEAP ) ) {
      buf.append( "\tLD\tDE,HEAPE-HEAPB-5\n"
		+ "\tLD\tHL,HEAPB\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n" );
    }
    compiler.getTarget().appendInitTo( buf );
  }


	/* --- private Methoden --- */

  private static void appendBssLineTo(
				AsmCodeBuf buf,
				String     label,
				int        size )
  {
    buf.append( label );
    buf.append( ":\tDS\t" );
    if( size < 10 ) {
      buf.append( size );
    } else if( size < 0x100 ) {
      buf.appendHex2( size );
    } else {
      buf.appendHex4( size );
    }
    buf.newLine();
  }


	/* --- Konstruktor --- */

  private BasicLibrary()
  {
    // nicht instanziierbar
  }
}
