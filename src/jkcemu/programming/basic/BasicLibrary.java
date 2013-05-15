/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Bibliothek
 */

package jkcemu.programming.basic;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.programming.*;


public class BasicLibrary
{
  public static enum LibItem {
			INIV, INSV, INRSV, INLNB, INLNR, INPWV,
			P_I, P_IF,
			PS_I, PS_IF, PS_S, PS_ST, PS_SP, PS_NL,
			ACCEPT, CIRCLE, CONNECT,
			DATA, DATAGRAM, DREADI, DREADS,
			DRAW, DRAWR, DRBOX, DRBOXF, H_BOX, DRHLIN, DRLINE,
			DRLBL, DRLBLT, MOVER, LOCATE,
			PAUSE, PEN, ONGOAD, PLOTR, SCREEN,
			IOCLOSE, IOOPEN, IOEOF, IOAVAILABLE,
			IOINL, IOINX, IORDB, IOFLUSH, IO_SET_COUT,
			IO_CRT_HANDLER, IO_LPT_HANDLER,
			IO_SIMPLE_OUT_HANDLER, IO_VDIP_HANDLER,
			IO_COUT, IOCADR, IOCTB1, IOCTB2,
			CHECK_OPEN_NET_CHANNEL,
			KCNET_S_MACADDR, KCNET_S_DNSSERVER, KCNET_S_GATEWAY,
			KCNET_S_LOCALADDR, KCNET_S_NETMASK,
			KCNET_S_GET_IPADDR, KCNET_S_IPADDR,
			KCNET_SET_DNSSERVER, KCNET_SET_GATEWAY,
			KCNET_SET_LOCALADDR, KCNET_SET_NETMASK,
			KCNET_SET_BASE, KCNET_BASE_HW,
			KCNET_HOSTBYNAME, KCNET_PARSE_IPADDR,
			KCNET_TCP, KCNET_UDP, KCNET_SOCK, KCNET_BASE,
			VDIP_M_IOADDR,
			F_ISTR, F_JOY, F_LEN, F_LOCALPORT, F_REMOTEPORT,
			F_RND, F_SGN, F_SQR,
			F_VAL, F_VLB, F_VLH, F_VLI,
			S_BIN, S_CHR, S_HEX, S_HEXN, S_HXHL, S_HXA,
			S_HOSTBYNAME, S_INP, S_INCH, S_INKY,
			S_LEFT, S_LWR, S_LTRIM,
			S_MID, S_MIDN, S_MIRR, S_NETMASK, S_REMOTEADDR,
			S_RIGHT, S_RTRIM,
			S_STC, S_STS, S_STR, S_TRIM, S_UPR, SEND,
			ARYADR, CKIDX,
			O_AND, O_NOT, O_OR, O_XOR,
			O_LT, O_LE, O_GT, O_GE, O_EQ, O_NE,
			O_ADD, O_SUB, O_MUL, O_MOD, O_DIV, O_INC, O_DEC,
			O_SHL, O_SHR,
			O_STEQ, O_STNE, O_STGE, O_STGT, O_STLE, O_STLT,
			ASGSM, ASGSL, ASGSV,
			MFIND, MALLOC, MMGC, MRGC, MFREE, HEAP, ABS_NEG_HL,
			CPHLDE, CKSTK, JP_HL, SMACP, SVDUP, STCMP, STNCP,
			C_UPR, D_EMPT,
			E_DATA, E_IDX, E_PARM, E_NOV, E_NXWF, E_REWG, E_USR,
			E_TYPE, E_EXIT, OUTSP, FNT5P7,
			M_ERN, M_ERT, M_FRET, M_INKB, M_IO_COUT,
			M_HOST, M_PORT,
			M_STMP, M_TOP, M_XYPO,
			XCKBRK, XINCH, XINKEY, XBREAK,
			XBORDER, XCLS, XCOLOR, XCURS, XHLINE, XINK, XPAPER,
			XOUTST, XOUTS, XOUTNL, XOUTCH, XLOCATE, XLPTCH,
			XPAUSE, XPEN, XPSET, XPRES, XPTEST,
			XSCRS, XTARID, XJOY };

  public static final int E_OK                    = 0;
  public static final int E_ERROR                 = -1;
  public static final int E_INVALID               = -2;
  public static final int E_OVERFLOW              = -3;
  public static final int E_CHANNEL_ALREADY_OPEN  = -11;
  public static final int E_CHANNEL_CLOSED        = -12;
  public static final int E_DEVICE_NOT_FOUND      = -21;
  public static final int E_DEVICE_NOT_CONFIGURED = -22;
  public static final int E_DEVICE_LOCKED         = -23;
  public static final int E_NO_DISK               = -24;
  public static final int E_FILE_NOT_FOUND        = -25;
  public static final int E_IO_MODE               = -26;
  public static final int E_IO_ERROR              = -27;
  public static final int E_EOF                   = -28;
  public static final int E_READ_ONLY             = -29;
  public static final int E_DISK_FULL             = -30;
  public static final int E_MTU_EXCEEDED          = -31;
  public static final int E_DNS_NOT_CONFIGURED    = -32;
  public static final int E_DNS_ERROR             = -33;
  public static final int E_UNKNOWN_HOST          = -34;
  public static final int E_CONNECT_FAILED        = -35;
  public static final int E_SOCKET_STATUS         = -36;
  public static final int E_TIMEOUT               = -37;

  public static final int IOCTB_EOF_OFFS       = 2;
  public static final int IOCTB_AVAILABLE_OFFS = 4;
  public static final int IOCTB_READ_OFFS      = 6;
  public static final int IOCTB_WRITE_OFFS     = 8;
  public static final int IOCTB_FLUSH_OFFS     = 10;
  public static final int IOCTB_BBUF_OFFS      = 12;
  public static final int IOCTB_DRIVER_OFFS    = 14;

  public static final int IOMODE_INPUT  = 1;
  public static final int IOMODE_OUTPUT = 2;
  public static final int IOMODE_APPEND = 4;


  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf     buf       = compiler.getCodeBuf();
    Set<LibItem>   libItems  = compiler.getLibItems();
    BasicOptions   options   = compiler.getBasicOptions();
    AbstractTarget target    = compiler.getTarget();
    int            stackSize = options.getStackSize();
    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Bibliotheksfunktionen\n" );
    }
    if( libItems.contains( LibItem.INIV ) ) {
      /*
       * Eingabe einer Zahl in eine Integer-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die Integer-Variable
       * Rueckgabewert:
       *   CY=1: Fehlerhafte Eingabe
       */
      buf.append( "INIV:\tPUSH\tHL\n"
		+ "\tCALL\tINLNB\n"
		+ "\tCALL\tF_VLI\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,(M_ERN)\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,INIV1\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n"
		+ "INIV1:" );
      buf.append_LD_HL_nn( E_OVERFLOW );
      buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNZ,INIV2\n"
		+ "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Tut mir leid!\'\n" );
      } else {
	buf.append( "\tDB\t\'Sorry!\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJR\tINIV3\n"
		+ "INIV2:\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Wie bitte?\'\n" );
      } else {
	buf.append( "\tDB\t\'What?\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "INIV3:\tCALL\tXOUTNL\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      libItems.add( LibItem.INLNB );
      libItems.add( LibItem.F_VLI );
      libItems.add( LibItem.XOUTST );
      libItems.add( LibItem.XOUTNL );
      libItems.add( LibItem.M_ERN );
    }
    if( libItems.contains( LibItem.INSV ) ) {
      /*
       * Eingabe einer Zeile in eine String-Variable
       * mit Abschneiden der fuehrenden Leerzeichen
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INSV:\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tINLNB\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,INSV1\n"
		+ "\tLD\tDE,D_EMPT\n"
		+ "\tJR\tINSV3\n"
		+ "INSV1:\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "INSV2:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,INSV2\n"
		+ "\tDEC\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "INSV3:\tPOP\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tMFREE\n" );
      libItems.add( LibItem.INLNB );
      libItems.add( LibItem.MFIND );
      libItems.add( LibItem.MALLOC );
      libItems.add( LibItem.MFREE );
      libItems.add( LibItem.D_EMPT );
    }
    if( libItems.contains( LibItem.INRSV ) ) {
      /*
       * Eingabe einer Zeile in eine String-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INRSV:\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPUSH\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tC,0FFH\n"
		+ "\tCALL\tINLNR\n"
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
      libItems.add( LibItem.INLNR );
      libItems.add( LibItem.MFIND );
      libItems.add( LibItem.MALLOC );
      libItems.add( LibItem.MFREE );
    }
    if( libItems.contains( LibItem.INLNB ) ) {
      /*
       * Eingabe einer Zeile in den temporaeren String-Puffer
       * mit Abschneiden fuehrender Leerzeichen
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die eingegebene Zeichenkette
       */
      buf.append( "INLNB:\tLD\tHL,M_STMP\n"
		+ "\tLD\tC," );
      buf.appendHex2( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tCALL\tINLNR\n"
		+ "\tLD\tHL,M_STMP-1\n"
		+ "INLNB1:\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,INLNB1\n"
		+ "\tRET\n" );
      libItems.add( LibItem.INLNR );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.INLNR ) ) {
      /*
       * Eingabe einer Zeile, die mit ENTER abgeschlossen wird
       *
       * Parameter:
       *   HL: Anfangsadresse des Puffers
       *   C:  max. Anzahl einzugebender Zeichen (Pufferlaenge - 1)
       * Rueckgabewert:
       *   HL: Zeiger auf das terminierende Null-Byte
       */
      buf.append( "INLNR:\tPUSH\tHL\n"
		+ "\tLD\tB,C\n"
		+ "INLNR1:\tLD\t(HL),20H\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tINLNR1\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,00H\n"
		+ "\tJR\tINLNR3\n"
		+ "INLNR2:\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "INLNR3:\tPUSH\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tD,A\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,INLNR5\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tZ,INLNR5\n"
		+ "\tCP\t09H,\n"
		+ "\tJR\tZ,INLNR6\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,INLNR7\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,INLNR3\n"
		+ "INLNR4:\tLD\tA,B\n"
		+ "\tCP\tC\n"
		+ "\tJR\tNC,INLNR3\n"
		+ "\tLD\t(HL),D\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tB\n"
		+ "\tJR\tINLNR2\n"
		+ "INLNR5:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,INLNR3\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tB\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tINLNR2\n"
		+ "INLNR6:\tLD\tA,B\n"
		+ "\tCP\tC\n"
		+ "\tJR\tNC,INLNR3\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tB\n"
		+ "\tJR\tINLNR2\n"
		+ "INLNR7:\tLD\t(HL),00H\n"
		+ "\tJP\tXOUTNL\n" );
      libItems.add( LibItem.XINCH );
      libItems.add( LibItem.XOUTNL );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.INPWV ) ) {
      /*
       * Eingabe eines Kennwortes in eine String-Variable
       *
       * Parameter:
       *   HL: Zeiger auf die String-Variable
       */
      buf.append( "INPWV:\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPUSH\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tB,00H\n"
		+ "INPWV1:\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,INPWV3\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tZ,INPWV3\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,INPWV4\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,INPWV1\n"
		+ "\tINC\tB\n"
		+ "\tJR\tZ,INPWV2\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,2AH\n"		// Echo-Zeichen
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tINPWV1\n"
		+ "INPWV2:\tDEC\tB\n"
		+ "\tJR\tINPWV1\n"
		+ "INPWV3:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,INPWV1\n"
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
		+ "\tJR\tINPWV1\n"
		+ "INPWV4:\tXOR\tA\n"
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
      libItems.add( LibItem.MFIND );
      libItems.add( LibItem.MALLOC );
      libItems.add( LibItem.MFREE );
      libItems.add( LibItem.XINCH );
      libItems.add( LibItem.XOUTNL );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.P_I ) ) {
      /*
       * Unformatierte Ausgabe einer Integer-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "P_I:\tCALL\tS_STR\n"
		+ "\tJP\tXOUTS\n" );
      libItems.add( LibItem.S_STR );
      libItems.add( LibItem.XOUTS );
    }
    if( libItems.contains( LibItem.P_IF ) ) {
      /*
       * Formatierte Ausgabe einer Integer-Zahl auf dem Bildschirm
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "P_IF:\tCALL\tS_STR\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_LEN\n"
		+ "\tLD\tA,0EH\n"
		+ "\tSUB\tL\n"
		+ "\tJR\tC,P_IF2\n"
		+ "\tJR\tZ,P_IF2\n"
		+ "P_IF1:\tPUSH\tAF\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tAF\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,P_IF1\n"
		+ "P_IF2:\tPOP\tHL\n"
		+ "\tJP\tXOUTS\n" );
      libItems.add( LibItem.S_STR );
      libItems.add( LibItem.F_LEN );
      libItems.add( LibItem.XOUTCH );
      libItems.add( LibItem.XOUTS );
    }
    if( libItems.contains( LibItem.PS_I ) ) {
      /*
       * Unformatierte Ausgabe einer Integer-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "PS_I:\tCALL\tS_STR\n"
		+ "\tJP\tPS_S\n" );
      libItems.add( LibItem.S_STR );
      libItems.add( LibItem.PS_S );
    }
    if( libItems.contains( LibItem.PS_IF ) ) {
      /*
       * Formatierte Ausgabe einer Integer-Zahl auf einem Ausgabekanal
       *
       * Parameter:
       *   HL: auszugebende Zahl
       */
      buf.append( "PS_IF:\tCALL\tS_STR\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_LEN\n"
		+ "\tLD\tA,0EH\n"
		+ "\tSUB\tL\n"
		+ "\tJR\tC,PS_IF2\n"
		+ "\tJR\tZ,PS_IF2\n"
		+ "PS_IF1:\tPUSH\tAF\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tPOP\tAF\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,PS_IF1\n"
		+ "PS_IF2:\tPOP\tHL\n"
		+ "\tJP\tPS_S\n" );
      libItems.add( LibItem.S_STR );
      libItems.add( LibItem.F_LEN );
      libItems.add( LibItem.PS_S );
      libItems.add( LibItem.IO_COUT );
    }
    if( libItems.contains( LibItem.PS_ST ) ) {
      buf.append( "PS_ST:\tEX\t(SP),HL\n"
		+ "\tCALL\tPS_S\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.PS_S );
    }
    if( libItems.contains( LibItem.PS_S ) ) {
      buf.append( "PS_S:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tPS_S\n" );
      libItems.add( LibItem.IO_COUT );
    }
    if( libItems.contains( LibItem.PS_SP ) ) {
      buf.append( "PS_SP:\tLD\tA,20H\n"
		+ "\tJR\tIO_COUT\n" );
      libItems.add( LibItem.IO_COUT );
    }
    if( libItems.contains( LibItem.PS_NL ) ) {
      buf.append( "PS_NL:\tLD\tA,0DH\n"
		+ "\tCALL\tIO_COUT\n"
		+ "\tLD\tA,0AH\n" );
      libItems.add( LibItem.IO_COUT );
      // direkt weiter mit IO_COUT!
    }
    if( libItems.contains( LibItem.IO_COUT ) ) {
      buf.append( "IO_COUT:\n"
		+ "\tLD\tHL,(M_IO_COUT)\n"
		+ "\tJP\t(HL)\n" );
      libItems.add( LibItem.M_IO_COUT );
    }
    if( libItems.contains( LibItem.IO_SET_COUT ) ) {
      /*
       * Die Methode setzt die Adresse der Ausgaberoutine.
       * Die Fehlervariablen werden initialisiert bzw. gesetzt.
       *
       * Parameter:
       *   HL: Adresse der Ausgaberoutine
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Adresse ist Null
       */
      buf.append( "IO_SET_COUT:\n"
		+ "\tLD\t(M_IO_COUT),HL\n" );
      appendResetErrorUseBC( compiler );
      buf.append( "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_IO_COUT );
    }
    if( libItems.contains( LibItem.ACCEPT ) ) {
      /*
       * Socket in TCP-Server-Mode schalten
       *
       * Parameter:
       *   (M_PORT): Portnummer
       *   A:        Socketnummer (entspricht Kanalnummer-1)
       *   HL:       Anfangsadresse des Kanalzeigerfeldes
       */
      buf.append( "ACCEPT:\tLD\t(M_IOCA),HL\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,ACCEPT1\n" );
      appendSetErrorChannelAlreadyOpen( compiler );
      buf.append( "\tRET\n"
		+ "ACCEPT1:\n"
		+ "\tDEC\tHL\n" );
      buf.append_LD_DE_nn( KCNetLibrary.IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tLD\t(HL),A\n" );
      appendResetErrorUseHL( compiler );
      buf.append( "\tCALL\tKCNET_INIT\n"
		+ "\tRET\tC\n"
		+ "ACCEPT2:\n"
		+ "\tLD\tE,01H\n"			// TCP
		+ "\tLD\tHL,(M_PORT)\n"
		+ "\tCALL\tKCNET_SOCK_OPEN\n"
		+ "\tLD\tBC,0201H\n"			// Sn_CR=LISTEN
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tCP\t14H\n"				// SOCK_LISTEN?
		+ "\tJR\tNZ,ACCEPT2\n"
      // Kanalzeigerfeld fuellen
		+ "\tCALL\tKCNET_SET_IOPTRS_TCP\n"
      // auf Verbindung warten
		+ "ACCEPT3:\n" );
      if( options.canBreakAlways() ) {
	buf.append( "\tCALL\tXCKBRK\n" );
	libItems.add( LibItem.XCKBRK );
      }
      buf.append( "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tCP\t14H\n"				// SOCK_LISTEN?
		+ "\tJR\tZ,ACCEPT3\n"
		+ "\tCP\t01H\n"				// SOCK_ARP?
		+ "\tJR\tZ,ACCEPT3\n"
		+ "\tCP\t15H\n"				// SOCK_SYNSENT?
		+ "\tJR\tZ,ACCEPT3\n"
		+ "\tCP\t16H\n"				// SOCK_SYNRECV?
		+ "\tJR\tZ,ACCEPT3\n"
		+ "\tCP\t17H\n"				// SOCK_ESTABLISHED?
		+ "\tJR\tNZ,ACCEPT2\n"
		+ "\tCALL\tKCNET_IOCTB_INIT_TXRX\n"
		+ "\tJP\tKCNET_IOCTB_FILL_REMOTE\n" );
      libItems.add( LibItem.KCNET_TCP );
      libItems.add( LibItem.KCNET_BASE );
    }
    if( libItems.contains( LibItem.CONNECT ) ) {
      /*
       * TCP-Verbindung aufbauen
       *
       * Parameter:
       *   (M_HOST): Hostname oder IP-Adresse (textuell)
       *   (M_PORT): Portnummer
       *   A:        Socketnummer (entspricht Kanalnummer-1)
       *   HL:       Anfangsadresse des Kanalzeigerfeldes
       */
      buf.append( "CONNECT:\n"
		+ "\tLD\t(M_IOCA),HL\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,CONNECT1\n" );
      appendSetErrorChannelAlreadyOpen( compiler );
      buf.append( "\tRET\n"
		+ "CONNECT1:\n"
		+ "\tDEC\tHL\n" );
      buf.append_LD_DE_nn( KCNetLibrary.IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tLD\t(HL),A\n" );
      appendResetErrorUseHL( compiler );
      buf.append( "\tCALL\tKCNET_INIT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tHL,(M_HOST)\n"
		+ "\tCALL\tKCNET_HOSTBYNAME\n"
		+ "\tRET\tC\n"
		+ "\tLD\tE,01H\n"			// TCP
		+ "\tCALL\tKCNET_SOCK_GET_PORT_AND_OPEN\n"
		+ "\tLD\tHL,KCNET_M_IPADDR\n"
		+ "\tLD\tBC,040CH\n"		// 4 Durchlaeufe, Sn_DIPR
		+ "CONNECT2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tC\n"
		+ "\tDJNZ\tCONNECT2\n"
		+ "\tLD\tBC,(M_PORT)\n"
		+ "\tLD\tL,10H\n"			// Sn_DPORT
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tLD\tBC,0401H\n"			// Sn_CR=CONNECT
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "CONNECT3:\n"
		+ "\tLD\tL,03H\n"			// Sn_SR
		+ "\tCALL\tKCNET_SOCK_GET_BYTE\n"
		+ "\tCP\t17H\n"				// SOCK_ESTABLISHED
		+ "\tJP\tZ,CONNECT5\n"
		+ "\tOR\tA\n" );			// SOCK_CLOSED?
      if( options.canBreakAlways() ) {
	buf.append( "\tJR\tZ,CONNECT4\n"
		+ "\tCALL\tXCKBRK\n"
		+ "\tJR\tCONNECT3\n"
		+ "CONNECT4:\n" );
	libItems.add( LibItem.XCKBRK );
      } else {
	buf.append( "\tJR\tNZ,CONNECT3\n" );
      }
      appendSetError(
		compiler,
		E_CONNECT_FAILED,
		"Verbindungsaufbau fehlgeschlagen",
		"Connect failed" );
      buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "CONNECT5:\n"
		+ "\tCALL\tKCNET_SET_IOPTRS_TCP\n"
		+ "\tCALL\tKCNET_IOCTB_INIT_TXRX\n"
		+ "\tJP\tKCNET_IOCTB_FILL_REMOTE\n" );
      libItems.add( LibItem.KCNET_HOSTBYNAME );
      libItems.add( LibItem.KCNET_TCP );
      libItems.add( LibItem.KCNET_BASE );
    }
    if( libItems.contains( LibItem.DATAGRAM ) ) {
      /*
       * Socket in UDP-Mode oeffnen
       *
       * Parameter:
       *   (M_PORT): Portnummer
       *   A:        Socketnummer (entspricht Kanalnummer-1)
       *   HL:       Anfangsadresse des Kanalzeigerfeldes
       */
      buf.append( "DATAGRAM:\n"
		+ "\tLD\t(M_IOCA),HL\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,DATAGRAM1\n" );
      appendSetErrorChannelAlreadyOpen( compiler );
      buf.append( "\tRET\n"
		+ "DATAGRAM1:\n"
		+ "\tDEC\tHL\n" );
      buf.append_LD_DE_nn( KCNetLibrary.IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tLD\t(HL),A\n" );
      appendResetErrorUseHL( compiler );
      buf.append( "\tCALL\tKCNET_INIT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tE,02H\n"		// UDP
		+ "\tLD\tHL,(M_PORT)\n"
		+ "\tCALL\tKCNET_SOCK_OPEN\n"
      // Kanalzeigerfeld fuellen
		+ "\tLD\tBC,000AH\n"		// ohne FLUSH-Routine
		+ "\tLD\tDE,(M_IOCA)\n"
		+ "\tLD\tHL,KCNET_DATA_IOPTRS\n"
		+ "\tLDIR\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tB,04H\n"		// FLUSH- und Zeichenpuffer
		+ "DATAGRAM2:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tDATAGRAM2\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tLD\t(DE),A\n"		// IOCTB_SOCKET_OFFS
		+ "\tINC\tDE\n"
		+ "\tLD\tA,\'U\'\n"
		+ "\tLD\t(DE),A\n"		// IOCTB_PROTO_OFFS
		+ "\tINC\tDE\n"
		+ "\tLD\tB," );
      buf.appendHex2( KCNetLibrary.IOCTB_CHANNEL_SIZE
				- KCNetLibrary.IOCTB_RX_CNT_OFFS );
      buf.append( "\n"
		+ "\tXOR\tA\n"
		+ "DATAGRAM3:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tDATAGRAM3\n"
		+ "\tJP\tKCNET_IOCTB_INIT_TXRX\n" );
      libItems.add( LibItem.KCNET_UDP );
      libItems.add( LibItem.KCNET_BASE );
    }
    if( libItems.contains( LibItem.SEND ) ) {
      /*
       * Senden eines UDP-Pakets
       *
       * Parameter:
       *   (KCNET_M_SOCKET): Socketnummer
       *   (M_IOCA):         Anfangsadresse Kanalzeigerfeld
       *   (M_HOST):         Hostname oder IP-Adresse (textuell)
       *   (M_PORT):         Portnummer
       */
      buf.append( "SEND:\tLD\tHL,(M_IOCA)\n"
		+ "\tCALL\tCHECK_OPEN_NET_CHANNEL\n"
		+ "\tRET\tC\n"
		+ "\tCP\t\'U\'\n"
		+ "\tJR\tZ,SEND1\n" );
      appendSetErrorIOMode( compiler );
      buf.append( "\tRET\n"
		+ "SEND1:\tLD\tBC,040CH\n"	// 4 Durchlaeufe und Sn_DIPR
		+ "\tLD\tHL,KCNET_M_IPADDR\n"
		+ "SEND2:\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tINC\tC\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tSEND2\n"
		+ "\tLD\tL,C\n"			// L nun Sn_DPORT
		+ "\tLD\tBC,(M_PORT)\n"
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tJP\tKCNET_FLUSH1\n" );
      libItems.add( LibItem.CHECK_OPEN_NET_CHANNEL );
      libItems.add( LibItem.KCNET_HOSTBYNAME );
      libItems.add( LibItem.KCNET_UDP );
      libItems.add( LibItem.KCNET_BASE );
    }
    if( libItems.contains( LibItem.CIRCLE ) ) {
      /*
       * Zeichnen eines Kreises
       *
       * Parameter:
       *   M_CIMX: X-Koordinate Kreismittelpunkt (Xm)
       *   M_CIMY: Y-Koordinate Kreismittelpunkt (Ym)
       *   M_CIRA: Radius
       * Hilfszellen:
       *   M_CIER: Fehlerglied
       *   M_CIRX: relative X-Koordinate (Xr)
       *   M_CIRY: relative Y-Koordinate (Yr)
       *   M_CXMX: Koordinate Xm - Xr
       *   M_CXPX: Koordinate Xm + Xr
       *   M_CXMY: Koordinate Xm - Yr
       *   M_CXPY: Koordinate Xm + Yr
       *   M_CYMX: Koordinate Ym - Xr
       *   M_CYPX: Koordinate Ym + Xr
       *   M_CYMY: Koordinate Ym - Yr
       *   M_CYPY: Koordinate Ym + Yr
       */
      buf.append( "CIRCLE:\tLD\tDE,(M_CIRA)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"		// bei R<0
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"			// bei R=0
		+ "\tLD\tHL,0001\n"		// Fehlerglied: 1-R
		+ "\tSBC\tHL,DE\n"		// CY ist 0
		+ "\tLD\t(M_CIER),HL\n"
		+ "\tLD\tHL,0000H\n"		// (Xr,Yr) = (0,R)
		+ "\tLD\t(M_CIRX),HL\n"
		+ "\tLD\t(M_CIRY),DE\n"
		+ "\tLD\tHL,(M_CIMX)\n"
		+ "\tLD\t(M_CXMX),HL\n"
		+ "\tLD\t(M_CXPX),HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\t(M_CXMY),HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_CXPY),HL\n"
		+ "\tLD\tHL,(M_CIMY)\n"
		+ "\tLD\t(M_CYMX),HL\n"
		+ "\tLD\t(M_CYPX),HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\t(M_CYMY),HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_CYPY),HL\n"
		+ "CIRC2:\tLD\tHL,(M_CIRY)\n"	// Ende erreicht?
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tDE,(M_CIRX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tAF\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tCIRC4\n"		// Punkte setzen
		+ "\tPOP\tDE\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tDE\n"			// Xr = Xr + 1
		+ "\tLD\t(M_CIRX),DE\n"
		+ "\tLD\tHL,(M_CXMX)\n"		// Xm-Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CXMX),HL\n"
		+ "\tLD\tHL,(M_CXPX)\n"		// Xm+Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_CXPX),HL\n"
		+ "\tLD\tHL,(M_CYMX)\n"		// Xm-Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CYMX),HL\n"
		+ "\tLD\tHL,(M_CYPX)\n"		// Xm+Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_CYPX),HL\n"
		+ "\tLD\tHL,(M_CIER)\n"		// F >= 0 -> CIRC3
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,CIRC3\n"
		+ "\tSLA\tE\n"			// F = F + 2*X - 1
		+ "\tRL\tD\n"
		+ "\tADD\tHL,DE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CIER),HL\n"
		+ "\tJR\tCIRC2\n"
		+ "CIRC3:\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,(M_CIRY)\n"		// F = F + 2*(X-Y)
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\t(M_CIER),HL\n"
		+ "\tLD\tHL,(M_CIRY)\n"		// Yr = Yr - 1
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CIRY),HL\n"
		+ "\tLD\tHL,(M_CXMY)\n"		// Xm-Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_CXMY),HL\n"
		+ "\tLD\tHL,(M_CXPY)\n"		// Xm+Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CXPY),HL\n"
		+ "\tLD\tHL,(M_CYMY)\n"		// Xm-Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_CYMY),HL\n"
		+ "\tLD\tHL,(M_CYPY)\n"		// Xm+Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(M_CYPY),HL\n"
		+ "\tJP\tCIRC2\n"
      /*
       * Setzen eines Punktes in allen 8 Oktanden
       *
       * DE:  relative X-Koordinate
       * BC:  relative Y-Koordinate
       * Z=1: Xr == Yr
       */
		+ "CIRC4:\tJR\tZ,CIRC6\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRC5\n"
		+ "\tLD\tDE,(M_CXMX)\n"
		+ "\tCALL\tCIRC10\n"
		+ "CIRC5:\tLD\tDE,(M_CXPX)\n"
		+ "\tCALL\tCIRC10\n"
		+ "\tEXX\n"
		+ "CIRC6:\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRC7\n"
		+ "\tLD\tDE,(M_CXMY)\n"
		+ "\tCALL\tCIRC8\n"
		+ "CIRC7:\tLD\tDE,(M_CXPY)\n"
		+ "CIRC8:\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRC9\n"
		+ "\tLD\tHL,(M_CYMX)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"
		+ "CIRC9:\tLD\tHL,(M_CYPX)\n"
		+ "\tJP\tXPSET\n"
		+ "CIRC10:\tEXX\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRC11\n"
		+ "\tLD\tHL,(M_CYMY)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"
		+ "CIRC11:\tLD\tHL,(M_CYPY)\n"
		+ "\tJP\tXPSET\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.ONGOAD ) ) {
      /*
       * Ermittlung der Sprungadresse bei ON GOTO/GOSUB.
       *
       * Parameter:
       *   HL: Auswahl des Sprungziels
       * Rueckgabewert:
       *   HL: Adresse des Sprungziels oder 0
       */
      buf.append( "ONGOAD:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
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
		+ "\tJR\tNZ,ONGOA1\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,ONGOA1\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tJR\tZ,ONGOA1\n"
		+ "\tJR\tC,ONGOA1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tONGOA2\n"
		+ "ONGOA1:\tLD\tHL,0000H\n"
		+ "ONGOA2:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.LOCATE ) ) {
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
		+ "\tJP\tE_PARM\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.XLOCATE );
    }
    if( libItems.contains( LibItem.MOVER ) ) {
      /*
       * Grafikkursor relativ positionieren
       *
       * Parameter:
       *   DE: X-Koordinate relativ
       *   HL: Y-Koordinate relativ
       */
      buf.append( "MOVER:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.O_ADD );
      libItems.add( LibItem.M_XYPO );
    }
    if( libItems.contains( LibItem.DRLBLT ) ) {
      buf.append( "DRLBLT:\tEX\t(SP),HL\n"
		+ "\tCALL\tDRLBL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.DRLBL );
    }
    if( libItems.contains( LibItem.DRLBL ) ) {
      buf.append( "DRLBL:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,DRLBL5\n"
		+ "\tCP\t7FH\n"	
		+ "\tJR\tNC,DRLBL\n"
		+ "\tSUB\t21H\n"
		+ "\tJR\tC,DRLBL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,00H\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tBC,FNT5P7\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tDE,(M_XPOS)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tC,A\n"
		+ "DRLBL1:\tLD\tA,(HL)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\tB,08H\n"
		+ "DRLBL2:\tSRL\tA\n"
		+ "\tJR\tNC,DRLBL3\n"
		+ "\tPUSH\tAF\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tAF\n"
		+ "DRLBL3:\tINC\tHL\n"
		+ "\tDJNZ\tDRLBL2\n"
		+ "\tINC\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,DRLBL1\n"
		+ "\tPOP\tHL\n"
		+ "DRLBL4:\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(M_XPOS),DE\n"
		+ "\tJR\tDRLBL\n"
		+ "DRLBL5:\tLD\tDE,(M_XPOS)\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tDRLBL4\n" );
      libItems.add( LibItem.FNT5P7 );
      libItems.add( LibItem.M_XYPO );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.DRAWR ) ) {
      /*
       * Linie zum relativ angegebenen Punkt zeichnen,
       * Das Pixel auf der aktuellen Position wird nicht gesetzt.
       *
       * Parameter:
       *   DE: rekative X-Koordinate
       *   HL: rekative Y-Koordinate
       */
      buf.append( "DRAWR:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tLD\t(M_LNBX),HL\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_LNEX),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\t(M_LNBY),HL\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_LNEY),HL\n"
		+ "\tJR\tDRAW1\n" );
      libItems.add( LibItem.M_XYPO );
      libItems.add( LibItem.O_ADD );
      libItems.add( LibItem.DRAW );
    }
    if( libItems.contains( LibItem.DRAW ) ) {
      /*
       * Linie zum Punkt (M_LNXE,M_LNYE) zeichnen,
       * Das Pixel auf der aktuellen Position wird nicht gesetzt.
       *
       * Parameter:
       *   DE: rekative X-Koordinate
       *   HL: rekative Y-Koordinate
       */
      buf.append( "DRAW:\tLD\tHL,(M_XPOS)\n"
		+ "\tLD\t(M_LNBX),HL\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\t(M_LNBY),HL\n"
		+ "DRAW1:\tCALL\tDRLIN1\n"
		+ "\tLD\tHL,(M_LNEX)\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tLD\tHL,(M_LNEY)\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_XYPO );
      libItems.add( LibItem.DRLINE );
    }
    if( libItems.contains( LibItem.DRBOX ) ) {
      /*
       * Rechteck zeichnen
       *
       * Parameter:
       *   (M_LNBX): X-Koordinate Anfang
       *   (M_LNBY): Y-Koordinate Anfang
       *   (M_LNEX): X-Koordinate Ende
       *   (M_LNEY): Y-Koordinate Ende
       * Hilfszellen:
       *   (M_LNSX): X-Schrittweite
       *   (M_LNSY): Y-Schrittweite
       */
      buf.append( "DRBOX:\tCALL\tH_BOX\n"
		+ "\tPUSH\tBC\n"	// oberste Line zeichen
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
    if( target.supportsXHLINE() ) {
      buf.append( "\tCALL\tXHLINE\n" );
      libItems.add( LibItem.XHLINE );
    } else {
      buf.append( "\tCALL\tDRHLIN\n" );
      libItems.add( LibItem.DRHLIN );
    }
    buf.append( "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,(M_LNEY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRBOX1\n"
		+ "\tLD\tA,(M_LNEY+1)\n"
		+ "\tCP\tH\n"
		+ "\tRET\tZ\n"
		+ "DRBOX1:\tINC\tHL\n"
		+ "\tLD\tA,(M_LNEY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRBOX2\n"
		+ "\tLD\tA,(M_LNEY+1)\n"
		+ "\tCP\tH\n" );
    if( target.supportsXHLINE() ) {
      buf.append( "\tJP\tZ,XHLINE\n" );
    } else {
      buf.append( "\tJP\tZ,DRHLIN\n" );
    }
    buf.append( "DRBOX2:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tDE,(M_LNEX)\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tDRBOX1\n" );
      libItems.add( LibItem.H_BOX );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.DRBOXF ) ) {
      /*
       * ausgefuelltes Rechteck zeichnen
       *
       * Parameter:
       *   (M_LNBX): X-Koordinate Anfang
       *   (M_LNBY): Y-Koordinate Anfang
       *   (M_LNEX): X-Koordinate Ende
       *   (M_LNEY): Y-Koordinate Ende
       * Hilfszellen:
       *   (M_LNSX): X-Schrittweite
       *   (M_LNSY): Y-Schrittweite
       */
      buf.append( "DRBOXF:CALL\tH_BOX\n"
		+ "DRBXF1:\tPUSH\tBC\n"	// oberste Line zeichen
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
      if( target.supportsXHLINE() ) {
	buf.append( "\tCALL\tXHLINE\n" );
	libItems.add( LibItem.XHLINE );
      } else {
	buf.append( "\tCALL\tDRHLIN\n" );
	libItems.add( LibItem.DRHLIN );
      }
      buf.append( "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,(M_LNEY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRFBX2\n"
		+ "\tLD\tA,(M_LNEY+1)\n"
		+ "\tCP\tH\n"
		+ "\tRET\tZ\n"
		+ "DRFBX2:\tINC\tHL\n"
		+ "\tJR\tDRBXF1\n" );
      libItems.add( LibItem.H_BOX );
    }
    if( libItems.contains( LibItem.H_BOX ) ) {
      /*
       * Anpassung der Koordinaten fuer das Zeichen eines Rechtecks
       *
       * Parameter:
       *   (M_LNBX): X-Koordinate Anfang
       *   (M_LNBY): Y-Koordinate Anfang
       *   (M_LNEX): X-Koordinate Ende
       *   (M_LNEY): Y-Koordinate Ende
       * Rueckgabewerte:
       *   (M_LNBX): linke X-Koordinate
       *   (M_LNBY): untere Y-Koordinate
       *   (M_LNEX): rechte X-Koordinate
       *   (M_LNEY): ober Y-Koordinate
       *   DE: linke X-Koordinate
       *   HL: untere Y-Koordinate
       *   BC: Breite des Rechtecks - 1
       */
      buf.append( "H_BOX:\tLD\tHL,(M_LNEY)\n"
		+ "\tLD\tDE,(M_LNBY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,H_BOX1\n"
		+ "\tADD\tHL,DE\n"		// Y-Koordinaten tauschen
		+ "\tLD\t(M_LNBY),HL\n"
		+ "\tLD\t(M_LNEY),DE\n"
		+ "H_BOX1:\tLD\tHL,(M_LNEX)\n"
		+ "\tLD\tDE,(M_LNBX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tB,H\n"			// BC = X2 - X1
		+ "\tLD\tC,L\n"
		+ "\tJR\tNC,H_BOX2\n"
		+ "\tADD\tHL,DE\n"		// X-Koordinaten tauschen
		+ "\tLD\t(M_LNBX),HL\n"
		+ "\tLD\t(M_LNEX),DE\n"
		+ "\tEX\tDE,HL\n"		// DE = X1
		+ "\tLD\tHL,0000H\n"		// BC anpassen
		+ "\tSBC\tHL,BC\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "H_BOX2:\tLD\tHL,(M_LNBY)\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.DRHLIN ) ) {
      /*
       * Horizontale Linie zeichnen
       *
       * Parameter:
       *   BC: Laenge - 1
       *   DE: linke X-Koordinate
       *   HL: Y-Koordinate
       */
      buf.append( "DRHLIN:\tBIT\t7,B\n"		// Laenge pruefen
		+ "\tRET\tNZ\n"
		+ "\tBIT\t7,H\n"		// Y pruefen
		+ "\tRET\tNZ\n"
		+ "DRHLN1:\tPUSH\tBC\n"
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
		+ "\tJR\tDRHLN1\n" );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.DRLINE ) ) {
      /*
       * Linie zeichnen
       *
       * Parameter:
       *   (M_LNBX): X-Koordinate Anfang
       *   (M_LNBY): Y-Koordinate Anfang
       *   (M_LNEX): X-Koordinate Ende
       *   (M_LNEY): Y-Koordinate Ende
       * Hilfszellen:
       *   (M_LNSX): X-Schrittweite
       *   (M_LNSY): Y-Schrittweite
       */
      buf.append( "DRLINE:\tLD\tDE,(M_LNBX)\n"	// Anfangspixel setzen
		+ "\tLD\tHL,(M_LNBY)\n"
		+ "\tCALL\tXPSET\n"
		+ "DRLIN1:\tLD\tBC,0001H\n"	// sx=1
		+ "\tLD\tHL,(M_LNEX)\n"		// dx=xe-xa
		+ "\tLD\tDE,(M_LNBX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,DRLIN2\n"		// if dx<0 then dx=-dx:sx=-1
		+ "\tCALL\tNEGHL\n"
		+ "\tLD\tBC,0FFFFH\n"
		+ "DRLIN2:\tLD\t(M_LNSX),BC\n"
		+ "\tPUSH\tHL\n"		// dx
		+ "\tPUSH\tHL\n"		// dx
		+ "\tLD\tBC,0001H\n"		// sy=1
		+ "\tLD\tHL,(M_LNEY)\n"		// dy=ye-ya
		+ "\tLD\tDE,(M_LNBY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,DRLIN3\n"		// if dy<0 then dy=-dy:sy=-1
		+ "\tCALL\tNEGHL\n"
		+ "\tLD\tBC,0FFFFH\n"
		+ "DRLIN3:\tLD\t(M_LNSY),BC\n"
		+ "\tPUSH\tHL\n"		// dx
		+ "\tEXX\n"
		+ "\tPOP\tBC\n"			// BC': dy
		+ "\tPOP\tDE\n"			// DE': dx
		+ "\tEXX\n"
		+ "\tEX\tDE,HL\n"		// dy -> DE
		+ "\tPOP\tHL\n"			// dx
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"		// if dx<dy then DRLIN5
		+ "\tJR\tC,DRLIN6\n"
      /*
       * ABS(dx) >= ABS(dy)
       * 2. Registersatz:
       *     BC: dy
       *     DE: dx
       *     HL: Fehlerglied
       */
		+ "\tEXX\n"
		+ "\tLD\tH,D\n"			// Fehlerglied mit
		+ "\tLD\tL,E\n"			// dx/2 initialisieren
		+ "\tSRA\tH\n"
		+ "\tRR\tL\n"
		+ "\tEXX\n"
		+ "DRLIN4:\tEXX\n"		// Test auf Y-Schritt
		+ "\tOR\tA\n"			// Fehler = Fehler - dy
		+ "\tSBC\tHL,BC\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,DRLIN5\n"		// wenn Fehler < 0
		+ "\tEXX\n"
		+ "\tADD\tHL,DE\n"		// Fehler = Fehler + dx
		+ "\tEXX\n"
		+ "\tLD\tHL,(M_LNBY)\n"		// Y-Schritt
		+ "\tLD\tDE,(M_LNSY)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_LNBY),HL\n"
		+ "DRLIN5:\tLD\tHL,(M_LNBX)\n"	// X-Schritt
		+ "\tLD\tDE,(M_LNSX)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_LNBX),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"		// Pixel setzen
		+ "\tLD\tHL,(M_LNBY)\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"			// Ende erreicht?
		+ "\tLD\tHL,(M_LNEX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,DRLIN4\n"
		+ "\tRET\n"
      /*
       * ABS(dx) < ABS(dy)
       * 2. Registersatz:
       *     BC: dy
       *     DE: dx
       *     HL: Fehlerglied
       */
		+ "DRLIN6:\tEXX\n"
		+ "\tLD\tH,B\n"			// Fehlerglied mit
		+ "\tLD\tL,C\n"			// dy/2 initialisieren
		+ "\tSRA\tH\n"
		+ "\tRR\tL\n"
		+ "\tEXX\n"
		+ "DRLIN7:\tEXX\n"		// Test auf X-Schritt
		+ "\tOR\tA\n"			// Fehler = Fehler - dx
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,DRLIN8\n"		// wenn Fehler < 0
		+ "\tEXX\n"
		+ "\tADD\tHL,BC\n"		// Fehler = Fehler + dy
		+ "\tEXX\n"
		+ "\tLD\tHL,(M_LNBX)\n"		// X-Schritt
		+ "\tLD\tDE,(M_LNSX)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_LNBX),HL\n"
		+ "DRLIN8:\tLD\tHL,(M_LNBY)\n"	// Y-Schritt
		+ "\tLD\tDE,(M_LNSY)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(M_LNBY),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tDE,(M_LNBX)\n"
		+ "\tCALL\tXPSET\n"		// Pixel setzen
		+ "\tPOP\tDE\n"			// Ende erreicht?
		+ "\tLD\tHL,(M_LNEY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,DRLIN7\n"
		+ "\tRET\n" );
      libItems.add( LibItem.ABS_NEG_HL );
      libItems.add( LibItem.M_XYPO );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.PAUSE ) ) {
      /*
       * Warten
       *
       * Parameter:
       *   HL: Wartezeit in 1/10 Sekunen
       */
      buf.append( "PAUSE:\tBIT\t7,H\n"
		+ "\tRET\tNZ\n"
		+ "PAUSE1:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPAUSE\n"
		+ "\tCALL\tXINKEY\n"
		+ "\tPOP\tHL\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,PAUSE1\n"
		+ "\tRET\n" );
      libItems.add( LibItem.XINKEY );
      libItems.add( LibItem.XPAUSE );
    }
    if( libItems.contains( LibItem.PEN ) ) {
      /*
       * Einstellen des Stiftes
       *
       * Parameter:
       *   HL: Mode: 0=Loeschen, 1=Zeichnen, 2=XOR
       */
      buf.append( "PEN:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t04H\n"
		+ "\tJP\tNC,E_PARM\n"
		+ "\tLD\tA,L\n"
		+ "\tJP\tXPEN\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.XPEN );
    }
    if( libItems.contains( LibItem.PLOTR ) ) {
      /*
       * Grafikkursor relativ positionieren und Punkt setzen
       *
       * Parameter:
       *   DE: X-Koordinate relativ
       *   HL: Y-Koordinate relativ
       */
      buf.append( "PLOTR:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_LNEX),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tCALL\tO_ADD\n"
		+ "\tLD\t(M_LNEY),HL\n"
		+ "\tLD\tDE,(M_XPOS)\n"
		+ "\tJP\tXPSET\n" );
      libItems.add( LibItem.M_XYPO );
      libItems.add( LibItem.O_ADD );
      libItems.add( LibItem.XPSET );
    }
    if( libItems.contains( LibItem.CKSTK ) ) {
      /*
       * Pruefen, ob der Stack ueberlaeuft
       *
       * Parameter bei CKSTKN:
       *   DE: benoetigte Stack-Tiefe
       */
      buf.append( "CKSTK:\tLD\tDE,0000\n"
		+ "CKSTKN:" );
      if( stackSize <= 0 ) {
	buf.append( "\tRET\n" );
      } else {
	buf.append( "\tLD\tHL,M_TOP-" );
	buf.appendHex4( stackSize );
	buf.append( "+10H" );
	libItems.add( LibItem.M_TOP );
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
	libItems.add( LibItem.E_EXIT );
	libItems.add( LibItem.XOUTST );
      }
    }
    if( libItems.contains( LibItem.S_BIN ) ) {
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
      buf.append( "S_BIN:\tLD\tDE,M_STMP\n"
		+ "\tLD\tB,10H\n"
		+ "\tLD\tA,31H\n"
		+ "S_BIN1:\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tJR\tC,S_BIN7\n"
		+ "\tDJNZ\tS_BIN1\n"
		+ "\tDEC\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tS_BIN8\n"
		+ "S_BINN:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,S_BIN\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,0010H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNC,S_BIN3\n"
		+ "S_BIN2:\tLD\tA,\'0\'\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,S_BIN2\n"
		+ "S_BIN3:\tPOP\tHL\n"
		+ "\tLD\tA,10H\n"
		+ "\tSUB\tC\n"
		+ "\tJR\tZ,S_BIN5\n"
		+ "\tLD\tB,A\n"
		+ "S_BIN4:\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tDJNZ\tS_BIN4\n"
		+ "S_BIN5:\tLD\tB,C\n"
		+ "S_BIN6:\tLD\tA,30H\n"
		+ "\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tJR\tNC,S_BIN7\n"
		+ "\tINC\tA\n"
		+ "S_BIN7:\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tS_BIN6\n"
		+ "S_BIN8:\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_HEXN ) ) {
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
      buf.append( "S_HEXN:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,S_HEX\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNC,S_HXN1\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "S_HXN1:\tLD\tHL,0004H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNC,S_HXN3\n"
		+ "S_HXN2:\tLD\tA,\'0\'\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,S_HXN2\n"
		+ "S_HXN3:\tPOP\tHL\n"
		+ "\tLD\tA,03H\n"
		+ "\tCP\tC\n"
		+ "\tJR\tC,S_HEX4\n"
		+ "\tDEC\tA\n"
		+ "\tCP\tC\n"
		+ "\tJR\tC,S_HEX3\n"
		+ "\tDEC\tA\n"
		+ "\tCP\tC\n"
		+ "\tJR\tC,S_HEX2\n"
		+ "\tJR\tS_HEX1\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.S_HEX );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_HEX ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       *
       * Parameter:
       *   HL: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_HEX:\tLD\tDE,M_STMP\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,S_HXC1\n"
		+ "S_HEX4:\tCALL\tS_HXHL\n"
		+ "\tJR\tS_HEXE\n"
		+ "S_HXC1:\tLD\tA,H\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tZ,S_HXC2\n"
		+ "S_HEX3:\tLD\tA,H\n"
		+ "\tCALL\tS_HXAL\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tS_HXA\n"
		+ "\tJR\tS_HEXE\n"
		+ "S_HXC2:\tLD\tA,L\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,S_HEX1\n"
		+ "S_HEX2:\tLD\tA,L\n"
		+ "\tCALL\tS_HXA\n"
		+ "\tJR\tS_HEXE\n"
		+ "S_HEX1:\tLD\tA,L\n"
		+ "\tCALL\tS_HXAL\n"
		+ "S_HEXE:\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.S_HXHL );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_HOSTBYNAME ) ) {
      /*
       * Ermittlung der IP-Adresse eines Rechners
       *
       * Parameter:
       *   HL: Zeiger auf den Rechnernamen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette mit der IP-Adresse
       */
      buf.append( "S_HOSTBYNAME:\n" );
      appendResetErrorUseBC( compiler );
      buf.append( "\tCALL\tKCNET_HOSTBYNAME\n"
		+ "\tLD\tHL,KCNET_M_IPADDR\n"
		+ "\tJP\tNC,KCNET_S_IPADDR\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_HOSTBYNAME );
      libItems.add( BasicLibrary.LibItem.KCNET_S_IPADDR );
      libItems.add( BasicLibrary.LibItem.D_EMPT );
    }
    if( libItems.contains( LibItem.S_INP ) ) {
      /*
       * Lesen einer bestimmten Anzahl von Zeichen von der Tastatur
       *
       * Parameter:
       *   HL: Anzahl Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INP:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,S_INP2\n"
		+ "\tLD\tB,L\n"
		+ "S_INP1:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXINCH\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tS_INP1\n"
		+ "S_INP2:\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.XINCH );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_INCH ) ) {
      /*
       * Warten auf einen Tastendruck und Rueckgabe der gedrueckten Taste
       * als Zeichenkette
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INCH:\tCALL\tXINCH\n"
		+ "\tLD\tHL,M_INKB\n"
		+ "\tJR\tS_CHRX\n" );
      libItems.add( LibItem.XINCH );
      libItems.add( LibItem.S_CHR );
      libItems.add( LibItem.M_INKB );
    }
    if( libItems.contains( LibItem.S_INKY ) ) {
      /*
       * Lesen der aktuell gedrueckten Taste und Rueckgabe als Zeichenkette
       *
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_INKY:\tCALL\tXINKEY\n"
		+ "\tLD\tHL,M_INKB\n"
		+ "\tJR\tS_CHRX\n" );
      libItems.add( LibItem.XINKEY );
      libItems.add( LibItem.S_CHR );
      libItems.add( LibItem.M_INKB );
    }
    if( libItems.contains( LibItem.S_CHR ) ) {
      /*
       * Umwandlung eines Zeichencodes in eine Zeichenkette
       *
       * Parameter:
       *   L: Zeichencode
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_CHRL:\tLD\tA,L\n"
		+ "S_CHRA:\tLD\tHL,M_STMP\n"
		+ "S_CHRX:\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_LEFT ) ) {
      /*
       * Anfang (linker Teil) einer Zeichenkette zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl der zurueckzuliefernden Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_LEFT:\tLD\tA,B\n"
			+ "\tOR\tA\n"
			+ "\tJP\tM,E_PARM\n"
			+ "\tLD\tDE,M_STMP\n"
			+ "\tPUSH\tDE\n"
			+ "\tCALL\tSTNCP\n"
			+ "\tPOP\tHL\n"
			+ "\tRET\n" );
      libItems.add( LibItem.F_LEN );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.M_STMP );
      libItems.add( LibItem.STNCP );
    }
    if( libItems.contains( LibItem.S_LWR ) ) {
      /*
       * Zeichenkette in Kleinbuchstaben wandeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   HL: Zeiger auf die umgewandelte Zeichenkette
       */
      buf.append( "S_LWR:\tLD\tDE,M_STMP-1\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "S_LWR1:\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t41H\n"
		+ "\tJR\tC,S_LWR2\n"
		+ "\tCP\t5BH\n"
		+ "\tJR\tNC,S_LWR2\n"
		+ "\tADD\tA,20H\n"
		+ "S_LWR2:\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,S_LWR3\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_LWR1\n"
		+ "\tLD\t(DE),A\n"
		+ "S_LWR3:\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_TRIM ) ) {
      /*
       * Weisse Leerzeichen am Anfang und Ende einer Zeichenkette abschneiden
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "S_TRIM:\tCALL\tS_LTRIM\n" );
      // weiter mit S_RTRM
      libItems.add( LibItem.S_LTRIM );
      libItems.add( LibItem.S_RTRIM );
    }
    if( libItems.contains( LibItem.S_RTRIM ) ) {
      /*
       * Weisse Leerzeichen am Ende einer Zeichenkette abschneiden
       * S_RTRM muss inmittelbar auf S_TRIM folgen!
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "S_RTRIM:\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tDEC\tDE\n"		// Zeiger auf letztes Zeichen
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "S_RTRIM1:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,S_RTRIM3\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,S_RTRIM2\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "S_RTRIM2:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_RTRIM1\n"
		+ "S_RTRIM3:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.F_LOCALPORT ) ) {
      /*
       * Ermitteln der lokalen Portnummer
       *
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabe:
       *   HL: Portnummer
       */
      buf.append( "F_LOCALPORT:\n"
		+ "\tCALL\tCHECK_OPEN_NET_CHANNEL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tKCNET_INIT\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tLD\tL,04H\n"			// Sn_PORT
		+ "\tJP\tKCNET_SOCK_GET_WORD\n" );
      libItems.add( LibItem.CHECK_OPEN_NET_CHANNEL );
      libItems.add( LibItem.KCNET_SOCK );
    }
    if( libItems.contains( LibItem.S_LTRIM ) ) {
      /*
       * Weisse Leerzeichen am Anfang einer Zeichenkette abschneiden
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die neue Zeichenkette
       */
      buf.append( "S_LTRIM:\n"
		+ "\tDEC\tHL\n"
		+ "S_LTRIM1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t21H\n"
		+ "\tJR\tC,S_LTRIM1\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.S_MIDN ) ) {
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
      buf.append( "S_MIDN:\tCALL\tS_MID\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tSTNCP\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.M_STMP );
      libItems.add( LibItem.S_MID );
      libItems.add( LibItem.STNCP );
    }
    if( libItems.contains( LibItem.S_MID ) ) {
      /*
       * Teil einer Zeichenkette ab einer gegebenen Position zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   DE: Position, ab der die Zeichenkette geliefert wird
       * Rueckgabewert:
       *   HL: Zeiger auf die Teilzeichenkette
       */
      buf.append( "S_MID:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tE\n"
		+ "\tJP\tZ,E_PARM\n"
		+ "S_MID1:\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tS_MID1\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.S_MIRR ) ) {
      /*
       * Zeichenkette spiegeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   HL: Zeiger auf die gespiegelte Zeichenkette
       */
      buf.append( "S_MIRR:\tLD\tBC,0FFFFH\n"
		+ "\tDEC\tHL\n"
		+ "S_MIR1:\tINC\tHL\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,S_MIR1\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,S_MIR2\n"
		+ "\tLD\tB,0FFH\n"	// Zeichenkette zu lang
		+ "\tJR\tS_MIR3\n"
		+ "S_MIR2:\tOR\tC\n"
		+ "\tRET\tZ\n"		// leere Zeichenkette
		+ "\tLD\tB,C\n"
		+ "S_MIR3:\tLD\tDE,M_STMP-1\n"
		+ "S_MIR4:\tINC\tDE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tS_MIR4\n"
		+ "\tINC\tDE\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_REMOTEADDR ) ) {
      /*
       * Ermitteln der IP-Adresse der Gegenstelle
       *
       * Parameter:
       *   HL: Anfangsadresse Kanalzeigerfeld
       * Rueckgabe:
       *   HL: Zeiger auf die Zeichenkette mit der IP-Adresse
       */
      buf.append( "S_REMOTEADDR:\n"
		+ "\tCALL\tCHECK_OPEN_NET_CHANNEL\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\tC\n" );
      buf.append_LD_HL_nn( KCNetLibrary.IOCTB_REMOTE_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tJP\tKCNET_S_IPADDR\n" );
      libItems.add( LibItem.CHECK_OPEN_NET_CHANNEL );
      libItems.add( LibItem.KCNET_S_IPADDR );
      libItems.add( LibItem.D_EMPT );
    }
    if( libItems.contains( LibItem.S_RIGHT ) ) {
      /*
       * Ende (rechter Teil) einer Zeichenkette zurueckliefern
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl der zurueckzuliefernden Zeichen
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_RIGHT:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tF_LEN\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "S_RIGHT1:\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,S_RIGHT1\n"
		+ "\tRET\n" );
      libItems.add( LibItem.F_LEN );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.S_STC ) ) {
      /*
       * Vervielfaeltigen eines Zeichens
       *
       * Parameter:
       *   L:  Zeichen
       *   BC: Anzahl
       * Rueckgabewert:
       *   HL: Zeiger auf die erzeugte Zeichenkette
       */
      buf.append( "S_STC:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_STC1\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\n"
		+ "S_STC1:\tLD\tE,L\n"
		+ "\tLD\tHL," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNC,S_STC2\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "S_STC2:\tLD\tHL,M_STMP\n"
		+ "\tPUSH\tHL\n"
		+ "S_STC3:\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_STC3\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.D_EMPT );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_STS ) ) {
      /*
       * Vervielfaeltigen einer Zeichenkette
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   BC: Anzahl
       * Rueckgabewert:
       *   HL: Zeiger auf die erzeugte Zeichenkette
       */
      buf.append( "S_STS:\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_STS1\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\n"
		+ "S_STS1:\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tEXX\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "S_STS2:\tCALL\tSTNCP\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,S_STS3\n"
		+ "\tEXX\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tNZ,S_STS2\n"
		+ "S_STS3:\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STNCP );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.D_EMPT );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.S_UPR ) ) {
      /*
       * Zeichenkette in Grossbuchstaben wandeln
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   HL: Zeiger auf die umgewandelte Zeichenkette
       */
      buf.append( "S_UPR:\tLD\tDE,M_STMP-1\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "S_UPR1:\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tC_UPR\n"
		+ "\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,S_UPR2\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,S_UPR1\n"
		+ "\tLD\t(DE),A\n"
		+ "S_UPR2:\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.C_UPR );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.F_JOY ) ) {
      buf.append( "F_JOY:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n" );
      if( target.supportsXJOY() ) {
	buf.append( "\tJP\tXJOY\n" );
	libItems.add( LibItem.XJOY );
      } else {
	buf.append( "\tHL,0000H\n"
		+ "\tRET\n" );
      }
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.F_ISTR ) ) {
      /*
       * Laenge einer Zeichenkette ermitteln
       *
       * Parameter:
       *   DE: Zeiger auf die gesuchte Zeichenkette
       *   HL: Zeiger auf die zu durchsuchende Zeichenkette
       * Rueckgabe:
       *   HL: Position, in der die erste Zeichenktte
       *       in der zweiten gefunden wurde bzw. 0,
       *       wenn nicht gefunden wurde
       */
      buf.append( "F_ISTR:\tLD\tBC,0000H\n"
		+ "\tDEC\tHL\n"
		+ "F_IST1:\tINC\tHL\n"
		+ "\tINC\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "F_IST2:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_IST3\n"		// gefunden
		+ "\tCP\t(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tZ,F_IST2\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_IST1\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_IST3:\tPOP\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.F_LEN ) ) {
      /*
       * Laenge einer Zeichenkette ermitteln
       *
       * Parameter:
       *   HL: Zeiger auf Zeichenkette
       * Rueckgabe:
       *   HL: Laenge der Zeichenkette
       */
      buf.append( "F_LEN:\tLD\tDE,0FFFFH\n"
		+ "\tDEC\tHL\n"
		+ "F_LEN1:\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_LEN1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.F_REMOTEPORT ) ) {
      /*
       * Ermitteln der Portnummer der Gegenstelle
       *
       * Parameter:
       *   HL: Anfangsadresse Kanalzeigerfeld
       * Rueckgabe:
       *   HL: Portnummer der Gegenstelle oder 0 im Fehlerfall
       */
      buf.append( "F_REMOTEPORT:\n"
		+ "\tCALL\tCHECK_OPEN_NET_CHANNEL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n" );
      buf.append_LD_HL_nn( KCNetLibrary.IOCTB_REMOTE_OFFS + 4 );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
      libItems.add( LibItem.CHECK_OPEN_NET_CHANNEL );
    }
    if( libItems.contains( LibItem.F_RND ) ) {
      /*
       * Ermitteln einer Zufallszahl
       *
       * Parameter:
       *   HL: maximaler Wert (groesser 0)
       * Rueckgabe:
       *   HL: Zufallszahl zwischen 1 und maximaler Wert
       */
      buf.append( "F_RND:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,F_RND1\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,M_RND\n"
		+ "\tLD\tA,R\n"
		+ "\tRLD\n"
		+ "\tINC\tHL\n"
		+ "\tRLD\n"
		+ "\tPOP\tDE\n"
		+ "F_RND1:\tLD\tHL,(M_RND)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_RND2\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t03H\n"		// damit die Schleife
		+ "\tLD\tH,A\n"			// nicht zu lange laeuft
		+ "F_RND2:\tSBC\tHL,DE\n"
		+ "\tJR\tNC,F_RND2\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.F_SGN ) ) {
      /*
       * Ermitteln des Vorzeichens
       *
       * Parameter:
       *   HL: zu pruefender Wert
       * Rueckgabe:
       *   HL: -1, 0 oder +1
       */
      buf.append( "F_SGN:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tLD\tHL,0001H\n"
		+ "\tRET\tP\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.F_SQR ) ) {
      /*
       * Ermitteln des ganzzahligen Anteils einer Quadratwurzel
       *
       * Parameter:
       *   HL: Wert, aus der die Quadratwurzel gezogen wird
       * Rueckgabe:
       *   HL: Quadratwurzel (nur ganzzahliger Anteil)
       */
      buf.append( "F_SQR:\tBIT\t7,H\n"
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tB,1\n"
		+ "\tLD\tDE,0001H\n"
		+ "F_SQR1:\tLD\tA,L\n"
		+ "\tSUB\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,H\n"
		+ "\tSBC\tA,D\n"
		+ "\tLD\tH,A\n"
		+ "\tJR\tC,F_SQR2\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,F_SQR3\n"
		+ "\tLD\tA,E\n"
		+ "\tADD\tA,2\n"
		+ "\tLD\tE,A\n"
		+ "\tLD\tA,D\n"
		+ "\tADC\tA,0\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,B\n"
		+ "\tINC\tA\n"
		+ "\tLD\tB,A\n"
		+ "\tJR\tF_SQR1\n"
		+ "F_SQR2:\tLD\tA,B\n"
		+ "\tDEC\tA\n"
		+ "\tLD\tB,A\n"
		+ "F_SQR3:\tLD\tL,B\n"
		+ "\tLD\tH,0\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.F_VAL ) ) {
      /*
       * Lesen einer Zahl aus einer Zeichenkette
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       *   DE: Zahlenbasis (2, 10 oder 16)
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_VAL:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,F_VLB\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tZ,F_VLI\n"
		+ "\tCP\t10H\n"
		+ "\tJP\tZ,F_VLH\n"
		+ "\tJP\tE_PARM\n" );
      libItems.add( LibItem.F_VLB );
      libItems.add( LibItem.F_VLI );
      libItems.add( LibItem.F_VLH );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.F_VLB ) ) {
      /*
       * Lesen einer Binaerzahl aus einer Zeichenkette
       * Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_VLB:\tLD\tD,H\n"
		+ "\tLD\tE,L\n" );
      BasicLibrary.appendResetErrorUseHL( compiler );
      buf.append( "\tDEC\tDE\n"
		+ "F_VLB1:\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_VLB1\n"
		+ "\tLD\tC,00H\n"
		+ "\tLD\tHL,0000H\n"
		+ "F_VLB2:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_VLB3\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,F_VLB7\n"
		+ "\tRET\n"
		+ "F_VLB3:\tCP\t20H\n"
		+ "\tJR\tNZ,F_VLB5\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_VLB7\n"
		+ "F_VLB4:\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,F_VLB5\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tF_VLB4\n"
		+ "F_VLB5:\tSUB\t30H\n"
		+ "\tJR\tC,F_VLB7\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tNC,F_VLB7\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,F_VLB6\n" );
      BasicLibrary.appendSetErrorNumericOverflow( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_VLB6:\tINC\tC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tOR\tL\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tF_VLB2\n"
		+ "F_VLB7:" );
      BasicLibrary.appendSetErrorInvalidChars( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.F_VLH ) ) {
      /*
       * Lesen einer Integer-Zahl aus einer Zeichenkette
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_VLH:\tLD\tD,H\n"
		+ "\tLD\tE,L\n" );
      BasicLibrary.appendResetErrorUseHL( compiler );
      buf.append( "\tDEC\tDE\n"
		+ "F_VLH1:\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_VLH1\n"
		+ "\tLD\tC,00H\n"
		+ "\tLD\tHL,0000H\n"
		+ "F_VLH2:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,F_VLH3\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,F_VLH8\n"
		+ "\tRET\n"
		+ "F_VLH3:\tCP\t20H\n"
		+ "\tJR\tNZ,F_VLH5\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,F_VLH8\n"
		+ "F_VLH4:\tLD\tA,(DE)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,F_VLH5\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tF_VLH4\n"
		+ "F_VLH5:\tSUB\t30H\n"
		+ "\tJR\tC,F_VLH8\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tC,F_VLH6\n"
		+ "\tSUB\t07H\n"
		+ "\tJR\tC,F_VLH8\n"
		+ "\tCP\t10H\n"
		+ "\tJR\tC,F_VLH6\n"
		+ "\tSUB\t20H\n"
		+ "\tJR\tC,F_VLH8\n"
		+ "\tCP\t10H\n"
		+ "\tJR\tNC,F_VLH8\n"
		+ "F_VLH6:\tLD\tB,A\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,F_VLH7\n" );
      BasicLibrary.appendSetErrorNumericOverflow( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_VLH7:\tINC\tC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tB\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tF_VLH2\n"
		+ "F_VLH8:" );
      BasicLibrary.appendSetErrorInvalidChars( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.F_VLI ) ) {
      /*
       * Lesen einer Integer-Zahl aus einer Zeichenkette
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabe:
       *   HL: gelesene Zahl oder 0 bei Fehler
       */
      buf.append( "F_VLI:\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
      BasicLibrary.appendResetErrorUseHL( compiler );
      buf.append( "\tDEC\tBC\n" 
		+ "F_VLI1:\tINC\tBC\n"
		+ "\tLD\tA,(BC)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_VLI1\n"
		+ "\tCP\t2DH\n"
		+ "\tJR\tNZ,F_VLI2\n"
		+ "\tINC\tBC\n"
		+ "\tCALL\tF_VLI2\n"
		+ "\tRET\tC\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
		+ "F_VLI2:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,F_VLI5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,F_VLI5\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "F_VLI3:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_VLI4\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,F_VLI5\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,F_VLI5\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,0\n"
		+ "\tADC\tA,H\n"
		+ "\tLD\tH,A\n"
		+ "\tJP\tP,F_VLI3\n" );
      BasicLibrary.appendSetErrorNumericOverflow( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "F_VLI4:\tLD\tA,(BC)\n"
		+ "\tINC\tBC\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,F_VLI4\n"
		+ "F_VLI5:" );
      BasicLibrary.appendSetErrorInvalidChars( compiler );
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.ARYADR ) ) {
      /*
       * Adresse eines Elements eines Arrays berechnen
       *
       * Parameter:
       *   HL: Index
       *   DE: Basisadresse der Variablen
       *   BC: Array-Groesse + 1
       * Rueckgabewert:
       *   HL: Adresse des Elements im Array
       */
      buf.append( "ARYADR:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_IDX\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJP\tNC,E_IDX\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_IDX );
    }
    if( libItems.contains( LibItem.CKIDX ) ) {
      /*
       * Index einer Feldvariablen pruefen
       *
       * Parameter:
       *   HL: Index
       *   BC: Groesse der Dimension + 1
       * Rueckgabewert:
       *   HL: Index
       */
      buf.append( "CKIDX:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_IDX\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJP\tNC,E_IDX\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_IDX );
    }
    if( libItems.contains( LibItem.O_AND ) ) {
      /*
       * Bitweises UND
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL AND DE
       */
      buf.append( "O_AND:\tLD\tA,H\n"
		+ "\tAND\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_OR ) ) {
      /*
       * Bitweises ODER
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL OR DE
       */
      buf.append( "O_OR:\tLD\tA,H\n"
		+ "\tOR\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_XOR ) ) {
      /*
       * Bitweises Exklusiv-ODER
       *
       * Parameter:
       *   DE, HL: Operanden
       * Rueckgabe:
       *   HL: HL XOR DE
       */
      buf.append( "O_XOR:\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tXOR\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_NOT ) ) {
      /*
       * Logisches NICHT
       *
       * Parameter:
       *   HL: Operand
       * Rueckgabe:
       *   HL: ~HL
       */
      buf.append( "O_NOT:\tLD\tA,H\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_LT ) ) {
      /*
       * Vergleich HL < DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL < DE, sonst 0
       */
      buf.append( "O_LT:\tCALL\tCPHLDE\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.CPHLDE );
    }
    if( libItems.contains( LibItem.O_LE ) ) {
      /*
       * Vergleich HL <= DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL <= DE, sonst 0
       */
      buf.append( "O_LE:\tCALL\tCPHLDE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.CPHLDE );
    }
    if( libItems.contains( LibItem.O_GT ) ) {
      /*
       * Vergleich HL > DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL > DE, sonst 0
       */
      buf.append( "O_GT:\tCALL\tCPHLDE\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.CPHLDE );
    }
    if( libItems.contains( LibItem.O_GE ) ) {
      /*
       * Vergleich HL >= DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL > DE, sonst 0
       */
      buf.append( "O_GE:\tCALL\tCPHLDE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.CPHLDE );
    }
    if( libItems.contains( LibItem.O_EQ ) ) {
      /*
       * Vergleich HL == DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL == DE, sonst 0
       */
      buf.append( "O_EQ:\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_NE ) ) {
      /*
       * Vergleich HL != DE ?
       *
       * Parameter:
       *   HL, DE: Operanden
       * Rueckgabe:
       *   HL: -1 wenn HL != DE, sonst 0
       */
      buf.append( "O_NE:\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.CPHLDE ) ) {
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
      buf.append( "CPHLDE:\tLD\tA,H\n"
                + "\tXOR\tD\n"
                + "\tJP\tP,CPHD1\n"
                + "\tEX\tDE,HL\n"
                + "\tCALL\tCPHD1\n"
                + "\tEX\tDE,HL\n"
                + "\tRET\n"
                + "CPHD1:\tLD\tA,H\n"
                + "\tCP\tD\n"
                + "\tRET\tNZ\n"
                + "\tLD\tA,L\n"
                + "\tCP\tE\n"
                + "\tRET\n" );
    }
    if( libItems.contains( LibItem.O_INC ) ) {
      /*
       * Addition: HL = HL + 1
       */
      buf.append( "O_INC:\tINC\tHL\n"
		+ "\tLD\tA,80H\n"
		+ "\tXOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n"
		+ "\tJP\tE_NOV\n" );
      libItems.add( LibItem.E_NOV );
    }
    if( libItems.contains( LibItem.O_DEC ) ) {
      /*
       * Addition: HL = HL - 1
       */
      buf.append( "O_DEC:\tLD\tA,80H\n"
		+ "\tXOR\tH\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_NOV\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_NOV );
    }
    if( libItems.contains( LibItem.O_SUB ) ) {
      /*
       * Subtraktion: HL = HL - DE
       */
      buf.append( "O_SUB:\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tNEGHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n" );	// kein RET -> weiter mit O_ADD
      libItems.add( LibItem.O_ADD );
      libItems.add( LibItem.ABS_NEG_HL );
    }
    if( libItems.contains( LibItem.O_ADD ) ) {
      /*
       * Addition: HL = HL + DE
       * O_ADD muss direkt hinter O_SUB folgen!
       */
      buf.append( "O_ADD:\tLD\tA,H\n"
		+ "\tXOR\tD\n"
		+ "\tLD\tA,D\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\tM\n"
		+ "\tXOR\tH\n"
		+ "\tRET\tP\n"
		+ "\tJP\tE_NOV\n" );
      libItems.add( LibItem.E_NOV );
    }
    if( libItems.contains( LibItem.O_MUL ) ) {
      /*
       * Multiplikation: HL = HL * DE
       */
      buf.append( "O_MUL:\tLD\tB,0\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,O_MUL1\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJP\tNZ,E_NOV\n"
		+ "O_MUL1:\tLD\tA,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,O_MUL3\n"
		+ "O_MUL2:\tADD\tHL,DE\n"
		+ "\tJP\tC,E_NOV\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,O_MUL2\n"
		+ "O_MUL3:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_NOV\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tM,NEGHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.ABS_NEG_HL );
      libItems.add( LibItem.E_NOV );
    }
    if( libItems.contains( LibItem.O_MOD ) ) {
      /*
       * Modulo: HL = HL % DE
       */
      buf.append( "O_MOD:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,O_MOD2\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,E_DIV0\n"
		+ "O_MOD1:\tLD\tB,00H\n"
		+ "\tCALL\tABSHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tO_DIV2\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tO_DIV1\n"
		+ "O_MOD2:\tEX\tDE,HL\n"
		+ "\tCALL\tNEGHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJR\tO_MOD1\n" );
      libItems.add( LibItem.ABS_NEG_HL );
      libItems.add( LibItem.O_DIV );
    }
    if( libItems.contains( LibItem.O_DIV ) ) {
      /*
       * Division: HL = HL / DE
       */
      buf.append( "O_DIV:\tLD\tB,00H\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tABSHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,E_DIV0\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tO_DIV2\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tBC\n"
		+ "O_DIV1:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_NOV\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tCALL\tM,NEGHL\n"
		+ "\tRET\n"
	// Vorzeichenlose Division DE = HL/DE, Rest in HL
		+ "O_DIV2:\tLD\tA,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tLD\tB,10H\n"
		+ "O_DIV3:\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tADC\tHL,HL\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,O_DIV4\n"
		+ "\tADD\tHL,DE\n"
		+ "O_DIV4:\tDJNZ\tO_DIV3\n"
		+ "\tRL\tC\n"
		+ "\tRLA\n"
		+ "\tCPL\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tLD\tE,A\n"
		+ "\tRET\n"
		+ "E_DIV0:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Division durch Null\'\n" );
      } else {
	buf.append( "\tDB\t\'Division by zero\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.ABS_NEG_HL );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.O_SHL ) ) {
      /*
       * links schieben: HL = HL << DE
       */
      buf.append( "O_SHL:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tJR\tNZ,O_SHL2\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tB,E\n"
		+ "O_SHL1:\tSLA\tL\n"
		+ "\tRL\tH\n"
		+ "\tDJNZ\tO_SHL1\n"
		+ "\tRET\n"
		+ "O_SHL2:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.O_SHR ) ) {
      /*
       * rechts schieben: HL = HL >> DE
       */
      buf.append( "O_SHR:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_PARM\n"
		+ "\tJR\tNZ,O_SHR2\n"
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tB,E\n"
		+ "O_SHR1:\tSRL\tH\n"
		+ "\tRR\tL\n"
		+ "\tDJNZ\tO_SHR1\n"
		+ "\tRET\n"
		+ "O_SHR2:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.ABS_NEG_HL ) ) {
      /*
       * Absolutwert und Negieren von Werten
       *
       * Parameter:
       *   HL: zu veraendernder Wert
       * Rueckgabe:
       *   HL: Ergebnis
       */
      buf.append( "ABSHL:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tP\n"
		+ "NEGHL:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,H\n"
		+ "\tPUSH\tAF\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tL,A\n"
		+ "\tINC\tHL\n"
		+ "\tPOP\tAF\n"
		+ "\tXOR\tH\n"
		+ "\tJP\tP,E_NOV\n"
		+ "\tLD\tA,B\n"
		+ "\tXOR\t80H\n"
		+ "\tLD\tB,A\n"
		+ "\tRET\n" );
      libItems.add( LibItem.E_NOV );
    }
    if( libItems.contains( LibItem.O_STEQ ) ) {
      /*
       * Zeichenkette (DE) == (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenketten gleich sind, sonst 0
       */
      buf.append( "O_STEQ:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.O_STGE ) ) {
      /*
       * Zeichenkette (DE) >= (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) >= (HL), sonst 0
       */
      buf.append( "O_STGE:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.O_STGT ) ) {
      /*
       * Zeichenkette (DE) > (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) > (HL), sonst 0
       */
      buf.append( "O_STGT:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.O_STLE ) ) {
      /*
       * Zeichenkette (DE) <= (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) < (HL), sonst 0
       */
      buf.append( "O_STLE:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.O_STLT ) ) {
      /*
       * Zeichenkette (DE) < (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenkette (DE) < (HL), sonst 0
       */
      buf.append( "O_STLT:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.O_STNE ) ) {
      /*
       * Zeichenkette (DE) != (HL)?
       *
       * Parameter:
       *   DE, HL: Zeiger auf die beiden Zeichenketten
       * Rueckgabe:
       *   HL: -1 wenn Zeichenketten ungleich sind, sonst 0
       */
      buf.append( "O_STNE:\tCALL\tSTCMP\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tNZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.STCMP );
    }
    if( libItems.contains( LibItem.DREADI ) ) {
      /*
       * Lesen eines 1- oder 2-Byte-Integer-Wertes aus dem Datenbereich
       * und einer Integer-Variablen zuweisen
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DREADI:\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tOR\tA\n"
		+ "\tJP\tZ,E_DATA\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_INT1 );
      buf.append( "\n"
		+ "\tJR\tZ,DRDI1\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_INT2 );
      buf.append( "\n"
		+ "\tJP\tNZ,E_TYPE\n"
		+ "\tINC\tDE\n"			// 2-Byte Integer
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\t(M_READ),DE\n"
		+ "\tRET\n"
		+ "DRDI1:\tINC\tDE\n"		// 1-Byte Integer
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(M_READ),DE\n"
		+ "\tRET\n" );
      libItems.add( LibItem.DATA );
      libItems.add( LibItem.E_DATA );
      libItems.add( LibItem.E_TYPE );
    }
    if( libItems.contains( LibItem.DREADS ) ) {
      /*
       * Lesen einer Zeichenkette aus dem Datenbereich
       * und einer String-Variablen zuweisen
       *
       * Parameter:
       *   HL:       Zeiger auf die Variable
       *   (M_READ): aktuelle Leseposition
       */
      buf.append( "DREADS:\tLD\tDE,(M_READ)\n"
		+ "\tLD\tA,(DE)\n"		// Typ-Byte
		+ "\tOR\tA\n"
		+ "\tJP\tZ,E_DATA\n"
		+ "\tCP\t" );
      buf.appendHex2( BasicCompiler.DATA_STRING );
      buf.append( "\n"
		+ "\tJP\tNZ,E_TYPE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\t(HL),D\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),E\n"
		+ "DRDS1:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,DRDS1\n"
		+ "\tLD\t(M_READ),DE\n"
		+ "\tLD\tD,B\n"
		+ "\tLD\tE,C\n"
		+ "\tJP\tMFREE\n" );
      libItems.add( LibItem.DATA );
      libItems.add( LibItem.MFREE );
      libItems.add( LibItem.E_DATA );
      libItems.add( LibItem.E_TYPE );
    }
    if( libItems.contains( LibItem.SCREEN ) ) {
      buf.append( "SCREEN:\tCALL\tXSCRS\n"
		+ "\tJP\tC,E_PARM\n"
		+ "\tRET\n" );
      libItems.add( LibItem.XSCRS );
      libItems.add( LibItem.E_PARM );
    }
    if( libItems.contains( LibItem.IOEOF ) ) {
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
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tJR\tIOEOF2\n"
		+ "IOEOF1:" );
      appendResetErrorUseBC( compiler );
      for( int i = 1; i < IOCTB_EOF_OFFS; i++ ) {
	buf.append( "\tINC\tHL\n" );
      }
      buf.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,IOEOF3\n" );
      appendSetErrorIOMode( compiler );
      buf.append( "IOEOF2:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n"
		+ "IOEOF3:\tJP\t(HL)\n" );
    }
    if( libItems.contains( LibItem.IOAVAILABLE ) ) {
      /*
       * Anzahl der verfuegbaren Bytes ermitteln
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       */
      buf.append( "IOAVAILABLE:\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOAVAILABLE1\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tJR\tIOAVAILABLE2\n"
		+ "IOAVAILABLE1:\n" );
      appendResetErrorUseBC( compiler );
      for( int i = 1; i < IOCTB_AVAILABLE_OFFS; i++ ) {
	buf.append( "\tINC\tHL\n" );
      }
      buf.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,IOAVAILABLE3\n" );
      appendSetErrorIOMode( compiler );
      buf.append( "IOAVAILABLE2:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n"
		+ "IOAVAILABLE3:\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tJP_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tPOP\tBC\n" );
      buf.append_LD_HL_nn( IOCTB_BBUF_OFFS );
      buf.append( "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.JP_HL );
    }
    if( libItems.contains( LibItem.IOINL ) ) {
      /*
       * Lesen einer Zeile aus einem Eingabekanal
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       * Rueckgabewert:
       *   HL: Zeiger auf die gelesene Zeichenkette
       */
      buf.append( "IOINL:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOINL1\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tLD\tHL,D_EMPT\n"
		+ "\tRET\n"
		+ "IOINL1:\tDEC\tHL\n" );
      appendResetErrorUseBC( compiler );
      buf.append( "\tLD\tB," );
      buf.appendHex2( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "IOINL2:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tIORDB\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,IOINL3\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,IOINL4\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tIOINL2\n"
		+ "IOINL3:\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
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
      libItems.add( LibItem.IORDB );
      libItems.add( LibItem.D_EMPT );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.IOINX ) ) {
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
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOINX1\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tLD\tHL,D_EMPT\n"
		+ "\tRET\n"
		+ "IOINX1:\tDEC\tHL\n" );
      appendResetErrorUseDE( compiler );
      buf.append( "\tLD\tDE,M_STMP\n"
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
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( LibItem.IORDB );
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.D_EMPT );
      libItems.add( LibItem.M_STMP );
    }
    if( libItems.contains( LibItem.IORDB ) ) {
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
      appendSetErrorIOMode( compiler );
      buf.append( "\tRET\n"
		+ "IORDB2:\tJP\t(HL)\n" );
    }
    if( libItems.contains( LibItem.IOFLUSH ) ) {
      /*
       * Gepufferte Daten schreiben
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       */
      buf.append( "IOFLUSH:\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,IOFLUSH1\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tRET\n"
		+ "IOFLUSH1:\n" );
      appendResetErrorUseBC( compiler );
      buf.append_LD_BC_nn( IOCTB_FLUSH_OFFS - 1 );
      buf.append( "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tZ,IOFLUSH2\n"
		+ "\tJP\t(HL)\n"
		+ "IOFLUSH2:\n" );
      buf.append_LD_HL_nn( IOCTB_WRITE_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tRET\tNZ\n" );
      appendSetErrorIOMode( compiler );
      buf.append( "\tRET\n" );
    }
    if( libItems.contains( LibItem.IOCLOSE ) ) {
      /*
       * Schliessen eines IO-Kanals
       *
       * Parameter:
       *   HL: Adresse des Kanalzeigerfeldes
       */
      buf.append( "IOCLOSE:\n" );
      appendResetErrorUseBC( compiler );
      buf.append( "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,IOCLOSE1\n"
		+ "\tCALL\tJP_HL\n"
		+ "\tJR\tIOCLOSE2\n"
		+ "IOCLOSE1:\n" );
      appendSetErrorChannelClosed( compiler );
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
      libItems.add( LibItem.JP_HL );
    }
    if( libItems.contains( LibItem.IOOPEN ) ) {
      /*
       * Oeffnen eines IO-Kanals
       *
       * Parameter:
       *   (M_IONM): Zeiger auf Geraete-/Dateiname
       *   (M_IOAC): Zugriffsmode
       *   HL:       Anfangsadresse des Kanalzeigerfeldes
       */
      buf.append( "IOOPEN:\n"
		+ "\tLD\t(M_IOCA),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tZ,IOOPEN1\n" );
      appendSetErrorChannelAlreadyOpen( compiler );
      buf.append( "\tRET\n"
		+ "IOOPEN1:\n" );
      appendResetErrorUseHL( compiler );
      buf.append( "\tLD\tHL,IOHNTB\n"
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
      if( libItems.contains( LibItem.M_ERN )
	  || libItems.contains( LibItem.M_ERT ) )
      {
	/*
	 * Der Handler hat sich fuer den Geraete- bzw. Dateinamen
	 * verantwortlich gefuehlt.
	 * Pruefen, ob der Kanal geoeffnet wurde.
	 */
	buf.append( "\tLD\tHL,(M_IOCA)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tRET\tNZ\n" );	// Kanal offen -> OK
	if( libItems.contains( LibItem.M_ERN ) ) {
	  buf.append( "\tLD\tHL,(M_ERN)\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tNZ\n" );	// Fehler eingetragen -> OK
	} else if( libItems.contains( LibItem.M_ERT ) ) {
	  buf.append( "\tLD\tHL,(M_ERT)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n" );	// Fehler eingetragen -> OK
	}
	appendSetError( compiler );	// Fehler eintragen
      }
      buf.append( "\tRET\n"
		+ "IOOPEN3:\n" );
      // Keine passenden Handler gefunden
      appendSetErrorDeviceNotFound( compiler );
      buf.append( "\tRET\n" );
      libItems.add( LibItem.IO_CRT_HANDLER );
      if( target.supportsXLPTCH() ) {
	libItems.add( LibItem.IO_LPT_HANDLER );
      }
      int[] ioAddrs = target.getVdipBaseIOAddresses();
      if( ioAddrs != null ) {
	if( ioAddrs.length > 0 ) {
	  libItems.add( LibItem.IO_VDIP_HANDLER );
	}
      }
      libItems.add( LibItem.JP_HL );
    }
    if( libItems.contains( LibItem.IO_VDIP_HANDLER ) ) {
      VdipLibrary.appendCodeTo( compiler );
    }
    if( libItems.contains( LibItem.IO_CRT_HANDLER ) ) {
      buf.append( "IO_CRT_HANDLER:\n"
		+ "\tLD\tHL,D_IOCRT\n"
		+ "\tLD\tDE,XOUTCH\n"
		+ "\tJR\tIO_SIMPLE_OUT_HANDLER\n" );
      libItems.add( LibItem.IO_SIMPLE_OUT_HANDLER );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.IO_LPT_HANDLER ) ) {
      buf.append( "IO_LPT_HANDLER:\n"
		+ "\tLD\tHL,D_IOLPT\n"
		+ "\tLD\tDE,XLPTCH\n"
		+ "\tJR\tIO_SIMPLE_OUT_HANDLER\n" );
      libItems.add( LibItem.IO_SIMPLE_OUT_HANDLER );
      libItems.add( LibItem.XLPTCH );
    }
    if( libItems.contains( LibItem.IO_SIMPLE_OUT_HANDLER ) ) {
      /*
       * Handler fuer einfachen Ausgabekanal
       *
       * Parameter:
       *   DE:       Adresse der Ausgaberoutine
       *   HL:       Geraetename, auf den der Handler reagiert
       *   (M_IONM): Zeiger auf Geraetename (wird mit dem in HL verglichen)
       *   (M_IOCA): Anfangsadresse des Kanalzeigerfeldes
       *   (M_IOAC): Zugriffsmode (muss 0 oder 2 sein)
       * Rueckgabewert:
       *   CY=1:     Handler ist nicht zustaendig
       *   CY=0:     Handler ist zustaendig
       *               -> keine weiteren Handler testen
       */
      buf.append( "IO_SIMPLE_OUT_HANDLER:\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tDE,(M_IONM)\n"
		+ "IO_SIMPLE_OUT_H1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCALL\tC_UPR\n"
		+ "\tCP\t(HL)\n"
		+ "\tJR\tNZ,IO_SIMPLE_OUT_H5\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t3AH\n"
		+ "\tJR\tNZ,IO_SIMPLE_OUT_H1\n"
		+ "\tLD\tA,(M_IOAC)\n"		// Betriebsart pruefen
		+ "\tOR\tA\n"
		+ "\tJR\tZ,IO_SIMPLE_OUT_H2\n"
		+ "\tCP\t" );
      buf.appendHex2( IOMODE_OUTPUT );
      buf.append( "\n"
		+ "\tJR\tZ,IO_SIMPLE_OUT_H2\n" );
      appendSetErrorIOMode( compiler );		// Betriebsart nicht moeglich
      buf.append( "\tPOP\tDE\n"
		+ "\tOR\tA\n"			// C=0
		+ "\tRET\n"
		+ "IO_SIMPLE_OUT_H2:\n"
		+ "\tLD\tHL,(M_IOCA)\n"
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
      libItems.add( LibItem.C_UPR );
    }
    if( libItems.contains( LibItem.IOCADR ) ) {
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
		+ "\tJP\tNZ,E_PARM\n"
		+ "\tOR\tL\n"
		+ "\tJP\tZ,E_PARM\n"
		+ "\tDEC\tA\n"
		+ "\tCP\t04H\n"
		+ "\tJP\tNC,E_PARM\n"
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
      libItems.add( LibItem.E_PARM );
      libItems.add( LibItem.IOCTB1 );
      libItems.add( LibItem.IOCTB2 );
    }
    if( libItems.contains( LibItem.CHECK_OPEN_NET_CHANNEL ) ) {
      /*
       * Pruefen, ob der Kanal mit KCNet (TCP oder UDP) geoeffnet ist
       * Die Fehlervariablen werden gesetzt.
       *
       * Parameter:
       *   HL: Anfangsadresse Kanalzeigerfeld
       * Rueckgabe:
       *   CY=0: OK, A: Protokoll, DE: Anfangsadresse Kanalzeigerfeld
       *   CY=1: nicht als KCNet-Kanal geoeffnet
       */
      buf.append( "CHECK_OPEN_NET_CHANNEL:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\t(HL)\n"
		+ "\tJR\tNZ,CHECK_OPEN_NET_CHANNEL1\n" );
      appendSetErrorChannelClosed( compiler );
      buf.append( "\tJR\tCHECK_OPEN_NET_CHANNEL2\n"
		+ "CHECK_OPEN_NET_CHANNEL1:\n"
		+ "\tDEC\tHL\n"
		+ "\tEX\tDE,HL\n" );
      buf.append_LD_HL_nn( KCNetLibrary.IOCTB_PROTO_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t\'T\'\n"
		+ "\tJR\tZ,CHECK_OPEN_NET_CHANNEL3\n"
		+ "\tCP\t\'U\'\n"
		+ "\tJR\tZ,CHECK_OPEN_NET_CHANNEL3\n" );
      appendSetErrorIOMode( compiler );
      buf.append( "CHECK_OPEN_NET_CHANNEL2:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "CHECK_OPEN_NET_CHANNEL3:\n" );
      appendResetErrorUseHL( compiler );
      buf.append( "\tOR\tA\n"				// CY=0
		+ "\tRET\n" );
    }
    KCNetLibrary.appendCodeTo( compiler );
    if( libItems.contains( LibItem.S_HXHL ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       * Es wird kein terminierendes Null-Byte geschrieben!
       *
       * Parameter:
       *   HL: numerischer Wert (Ausgabe 4-stellig)
       * Rueckgabewert:
       *   DE: Zeiger auf das erste Zeichen hinter der Zeichenkette
       */
      buf.append( "S_HXHL:\tLD\tA,H\n"
		+ "\tCALL\tS_HXA\n"
		+ "\tLD\tA,L\n" );
		// direkt weiter mit S_HXA
      libItems.add( LibItem.S_HXA );
    }
    if( libItems.contains( LibItem.S_HXA ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Hexadezimalzahl.
       * Es wird kein terminierendes Null-Byte geschrieben!
       *
       * Parameter:
       *   S_HXA:  A: numerischer Wert (Ausgabe 2-stellig)
       *   S_HXAL: A: numerischer Wert (Ausgabe 1-stellig)
       * Rueckgabewert:
       *   DE: Zeiger auf das erste Zeichen hinter der Zeichenkette
       */
      buf.append( "S_HXA:\tPUSH\tAF\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tRRCA\n"
		+ "\tCALL\tS_HXAL\n"
		+ "\tPOP\tAF\n"
		+ "S_HXAL:\tAND\t0FH\n"
		+ "\tADD\tA,90H\n"
		+ "\tDAA\n"
		+ "\tADC\tA,40H\n"
		+ "\tDAA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.C_UPR ) ) {
      // Zeichen in A gross wandeln
      buf.append( "C_UPR:\tCP\t61H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t7BH\n"
		+ "\tRET\tNC\n"
		+ "\tSUB\t20H\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.E_DATA ) ) {
      buf.append( "E_DATA:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Keine Daten mehr\'\n" );
      } else {
	buf.append( "\tDB\t\'Out of data\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_IDX ) ) {
      buf.append( "E_IDX:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Index ausserhalb des Bereichs\'\n" );
      } else {
	buf.append( "\tDB\t\'Index out of range\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_NOV ) ) {
      buf.append( "E_NOV:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Numerischer Ueberlauf\'\n" );
      } else {
	buf.append( "\tDB\t\'Numeric overflow\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_NXWF ) ) {
      buf.append( "E_NXWF:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'NEXT ohne FOR\'\n" );
      } else {
	buf.append( "\tDB\t\'NEXT without FOR\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_PARM ) ) {
      buf.append( "E_PARM:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Parameter ausserhalb des Wertebereichs\'\n" );
      } else {
	buf.append( "\tDB\t\'Parameter out of range\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_REWG ) ) {
      buf.append( "E_REWG:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'RETURN ohne GOSUB\'\n" );
      } else {
	buf.append( "\tDB\t\'RETURN without GOSUB\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_TYPE ) ) {
      buf.append( "E_TYPE:" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Falscher Datentyp\'\n" );
      } else {
	buf.append( "\tDB\t\'Type mismatch\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.ASGSM ) ) {
      /*
       * Eine Zeichenkette einer String-Variable zuweisen,
       * wobei diese in einen neu allokierten Speicherberich kopiert wird
       *
       * Parameter:
       *   DE: Zeiger auf die String-Variable
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "ASGSM:\tPUSH\tDE\n"
		+ "\tCALL\tSMACP\n"
		+ "\tPOP\tDE\n" );
      libItems.add( LibItem.SMACP );
      libItems.add( LibItem.ASGSL );
      // direkt weiter mit ASGSL
    }
    if( libItems.contains( LibItem.ASGSL ) ) {
      /*
       * Eine Zeichenkette einer String-Variable zuweisen
       *
       * Parameter:
       *   DE: Zeiger auf die String-Variable
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "ASGSL:\tPUSH\tDE\n"
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
      libItems.add( LibItem.MFREE );
    }
    if( libItems.contains( LibItem.ASGSV ) ) {
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
      buf.append( "ASGSV:\tLD\tA,H\n"
		+ "\tCP\tD\n"
		+ "\tJR\tNZ,ASGSV1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\tE\n"
		+ "\tRET\tZ\n"
		+ "ASGSV1:\tPUSH\tDE\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tCALL\tSVDUP\n"
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
      libItems.add( LibItem.SVDUP );
      libItems.add( LibItem.MFREE );
    }
    if( libItems.contains( LibItem.SMACP ) ) {
      /*
       * Die Routine allokiert Speicher und kopiert die in HL
       * uebergebene Zeichenkette hinein.
       *
       * Parameter:
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewert:
       *   DE: Zeiger auf die kopierte Zeichenkette
       */
      buf.append( "SMACP:\tLD\tA,(HL)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,SMACP1\n"
		+ "\tLD\tDE,D_EMPT\n"
		+ "\tRET\n"
		+ "SMACP1:\tPUSH\tHL\n"
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tSTNCP\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tRET\n" );
      libItems.add( LibItem.MFIND );
      libItems.add( LibItem.MALLOC );
      libItems.add( LibItem.STNCP );
      libItems.add( LibItem.D_EMPT );
    }
    if( libItems.contains( LibItem.SVDUP ) ) {
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
      buf.append( "SVDUP:\tLD\tHL,HEAPB\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tNC\n"
		+ "\tADD\tHL,DE\n"
		+ "\tJR\tSVDUP2\n"
		+ "SVDUP1:\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "SVDUP2:\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,SVDUP1\n"
		+ "\tADD\tHL,DE\n"
		+ "\tDEC\tHL\n"			// Block gefunden
		+ "\tBIT\t7,(HL)\n"
		+ "\tJR\tNZ,SVDUP3\n"		// Bit 7 -> neuer Speicher
		+ "\tINC\t(HL)\n"
		+ "\tRET\tP\n"
		+ "\tDEC\t(HL)\n"		// Zaehlerueberlauf
		+ "SVDUP3:\tPUSH\tDE\n"		// neuer Speicher
		+ "\tCALL\tMFIND\n"
		+ "\tPOP\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tBC," );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN );
      buf.append( "\n"
		+ "\tCALL\tSTNCP\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tMALLOC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.MFIND );
      libItems.add( LibItem.MALLOC );
      libItems.add( LibItem.HEAP );
      libItems.add( LibItem.STNCP );
    }
    if( libItems.contains( LibItem.STCMP ) ) {
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
      buf.append( "STCMP:\tLD\tA,(DE)\n"
		+ "\tCP\t(HL)\n"
		+ "\tRET\tNZ\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tSTCMP\n" );
    }
    if( libItems.contains( LibItem.STNCP ) ) {
      /*
       * Kopieren einer Zeichenkette mit Laengenbegrenzung
       *
       * Parameter:
       *   BC: max. Anzahl der Zeichen (Groesse des Zielpuffers - 1)
       *   DE: Zeiger auf den Zielpuffer
       *   HL: Zeiger auf die Zeichenkette
       * Rueckgabewerte:
       *   DE: Zeiger auf das Ende der kopierten Zeichenkette
       *       (terminierendes Null-Byte) im Zielpuffer
       *   BC: Restgroesse des Zielpuffers - 1
       */
      buf.append( "STNCP:\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,STNCP2\n"
		+ "\tDEC\tDE\n"
		+ "STNCP1:\tINC\tDE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(DE),A\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,STNCP1\n"
		+ "\tINC\tDE\n"
		+ "STNCP2:\tLD\t(DE),A\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.MFIND ) ) {
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
       *   |                        muss am Ende der Abweisung     |    \
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
		+ "\tJR\tNZ,MFIND1\n" );
      target.appendSwitchToTextScreen( buf );
      buf.append( "\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'Zeichenkettenspeicher voll\'\n" );
      } else {
	buf.append( "\tDB\t\'Out of string memory\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.HEAP );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.MALLOC ) ) {
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
		+ "\tJR\tC,MALLO1\n"
		+ "\tJR\tZ,MALLO1\n"
		+ "\tLD\tB,H\n"			// BC: neue Nutzgroesse
		+ "\tLD\tC,L\n"
		+ "\tLD\tIY,0FFFBH\n"		// -5
		+ "\tADD\tIY,DE\n"		// IY: Blockanfang
		+ "\tLD\tL,(IY+2)\n"
		+ "\tLD\tH,(IY+3)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"		// HL: freier Bereich
		+ "\tJR\tC,MALLO1\n"
		+ "\tJR\tZ,MALLO1\n"
		+ "\tLD\tDE,0010H\n"		// Mindestgroesse fuer
		+ "\tSBC\tHL,DE\n"		// Blocksplittung
		+ "\tJR\tC,MALLO1\n"
		+ "\tJR\tZ,MALLO1\n"
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
		+ "MALLO1:\tPOP\tHL\n"
		+ "\tPOP\tIY\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.MMGC ) ) {
      /*
       * Speicherbereich fuer spaetere Freigabe mit MRGC markieren
       * (Bit 7 im Referenzaehler setzen, Mark for Garbage Collector)
       * 
       * Parameter:
       *   DE: Zeiger auf den allokierten Speicherbereich
       */
      buf.append( "MMGC:\tLD\tHL,HEAPB\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tNC\n"			// Blockadresse < Heap
		+ "\tPUSH\tIY\n"
		+ "\tADD\tHL,DE\n"		// HL: Heap-Anfang
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "MMGC1:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MMGC3\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tZ,MMGC2\n"		// Block gefunden
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tJR\tMMGC1\n"
		+ "MMGC2:\tEX\tDE,HL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tSET\t7,(HL)\n"		// Block markieren
		+ "MMGC3:\tPOP\tIY\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( LibItem.MRGC ) ) {
      /*
       * Freigeben aller Speicherbereiche,
       * bei denen im Refenerenzzaehler Bit 7 gesetzt ist
       * (Run Garbage Collector)
       */
      buf.append( "MRGC:\tPUSH\tIY\n"
		+ "\tLD\tHL,0000H\n"		// vorheriger Block
		+ "\tLD\tIY,HEAPB\n"		// aktueller Block
		+ "MRGC1:\tLD\tA,(IY+5)\n"
		+ "\tOR\tA\n"
		+ "\tJP\tP,MRGC3\n"
		+ "\tAND\t7FH\n"
		+ "\tJR\tZ,MRGC2\n"
		+ "\tDEC\tA\n"
		+ "MRGC2:\tLD\t(IY+5),A\n"	// Referenzzaehler dekremtiert
		+ "\tJR\tNZ,MRGC3\n"		// trotzdem noch nicht frei
		+ "\tPUSH\tHL\n"		// Block wurde freigegeben
		+ "\tCALL\tMFREE4\n"		// ggf. verschmelzen
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MRGC3\n"
		+ "\tPUSH\tHL\n"		// zurueck zu vorherigen Block
		+ "\tPOP\tIY\n"
		+ "\tCALL\tMFREE4\n"		// ggf. verschmelzen
		+ "MRGC3:\tPUSH\tIY\n"		// naechster Block
		+ "\tPOP\tHL\n"
		+ "\tLD\tE,(IY+0)\n"
		+ "\tLD\tD,(IY+1)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,MRGC4\n"
		+ "\tPUSH\tDE\n"
		+ "\tPOP\tIY\n"
		+ "\tJR\tMRGC1\n"
		+ "MRGC4:\tPOP\tIY\n"
		+ "\tRET\n" );
      libItems.add( LibItem.HEAP );
      libItems.add( LibItem.MFREE );
    }
    if( libItems.contains( LibItem.MFREE ) ) {
      /*
       * Freigeben eines allokierten Speicherbereichs
       *
       * Parameter:
       *   DE: Zeiger auf den allokierten Speicherbereich
       */
      buf.append( "MFREE:\tLD\tHL,HEAPB\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tNC\n"			// Blockadresse < Heap
		+ "\tPUSH\tIY\n"
		+ "\tADD\tHL,DE\n"		// HL: Heap-Anfang
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tBC,0000H\n"
		+ "\tJR\tMFREE2\n"
		+ "MFREE1:\tADD\tHL,DE\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"			// BC: vorheriger Block
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "MFREE2:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,MFREE3\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,MFREE1\n"
		+ "\tADD\tHL,DE\n"		// Block gefunden
		+ "\tPUSH\tHL\n"
		+ "\tPOP\tIY\n"	
		+ "\tLD\tA,(IY+4)\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\t(IY+4),A\n"		// Bit 7 loeschen
		+ "\tJR\tZ,MFREE3\n"		// Block war nicht belegt
		+ "\tDEC\t(IY+4)\n"		// Referenzzaehler verringern
		+ "\tJR\tNZ,MFREE3\n"		// Block immer noch nicht frei
		+ "\tPUSH\tBC\n"		// vorheriger Block
		+ "\tCALL\tMFREE4\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,MFREE3\n"
		+ "\tPUSH\tBC\n"
		+ "\tPOP\tIY\n"
		+ "\tCALL\tMFREE4\n"
		+ "MFREE3:\tPOP\tIY\n"
		+ "\tRET\n"
      /*
       * Block in IY mit dem nachfolgenden Block verschmelzen,
       * sofern moeglich,
       * IY wird nicht zerstoert.
       * Parameter:
       *   IY: Blockadresse
       */
		+ "MFREE4:\tLD\tA,(IY+4)\n"	// Block frei?
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
      libItems.add( LibItem.HEAP );
    }
    if( libItems.contains( LibItem.E_USR ) ) {
      target.appendSwitchToTextScreen( buf );
      buf.append( "E_USR:\tCALL\tXOUTST\n" );
      if( compiler.isLangCode( "DE" ) ) {
	buf.append( "\tDB\t\'USR-Funktion nicht definiert\'\n" );
      } else {
	buf.append( "\tDB\t\'USR function not defined\'\n" );
      }
      buf.append( "\tDB\t00H\n"
		+ "\tJP\tE_EXIT\n" );
      libItems.add( LibItem.E_EXIT );
      libItems.add( LibItem.XOUTST );
    }
    if( libItems.contains( LibItem.E_EXIT ) ) {
      /*
       * Programmbeendigung aufgrund eines schweren Fehlers
       */
      buf.append( "E_EXIT:" );
      if( options.getPrintLineNumOnAbort() ) {
	buf.append( "\tLD\tHL,(M_SRLN)\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tNZ,E_EXI1\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTST\n" );
	if( compiler.isLangCode( "DE" ) ) {
	  buf.append( "\tDB\t\' in Zeile\'\n" );
	} else {
	  buf.append( "\tDB\t\' in line\'\n" );
	}
	buf.append( "\tDB\t00H\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tS_STR\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tLD\tHL,(M_BALN)\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tNZ,E_EXI1\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTST\n" );
	if( compiler.isLangCode( "DE" ) ) {
	  buf.append( "\tDB\t\' (BASIC-Zeile\'\n" );
	} else {
	  buf.append( "\tDB\t\' (BASIC line\'\n" );
	}
	buf.append( "\tDB\t00H\n"
		+ "\tPOP\tHL\n"
		+ "\tCALL\tS_STR\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tLD\tA,29H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "E_EXI1:" );
	libItems.add( LibItem.S_STR );
	libItems.add( LibItem.XOUTCH );
	libItems.add( LibItem.XOUTS );
	libItems.add( LibItem.XOUTST );
      }
      buf.append( "\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      libItems.add( LibItem.XOUTNL );
    }
    if( libItems.contains( LibItem.S_STR ) ) {
      /*
       * Umwandlung eines numerischen Wertes in eine Zeichenkette
       * mit einer Dezimalzahl.
       * Das erste Zeichen enthaelt entweder das Vorzeichen
       * oder ein Leerzeichen.
       *
       * Parameter:
       *   HL: numerischer Wert
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "S_STR:\tLD\tA,H\n"
		+ "\tXOR\t80H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,S_STR1\n"
		+ "\tLD\tHL,S_STR9\n"
		+ "\tRET\n"
		+ "S_STR1:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tLD\tA,20H\n"
		+ "\tJP\tP,S_STR2\n"
		+ "\tEX\tDE,HL\n"
		+ "\tXOR\tA\n"			// CY=0
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,A\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tA,'-'\n"
		+ "S_STR2:\tEXX\n"
		+ "\tLD\tHL,M_STR\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tB,00H\n"		// keine Nullen
		+ "\tLD\tDE,2710H\n"		// 10000
		+ "\tCALL\tS_STR3\n"
		+ "\tLD\tDE,03E8H\n"		// 1000
		+ "\tCALL\tS_STR3\n"
		+ "\tLD\tDE,0064H\n"		// 100
		+ "\tCALL\tS_STR3\n"
		+ "\tLD\tDE,000AH\n"		// 10
		+ "\tCALL\tS_STR3\n"
		+ "\tLD\tDE,0001H\n"		// 1
		+ "\tLD\tB,01H\n"		// Null ausgeben
		+ "\tCALL\tS_STR3\n"
		+ "\tXOR\tA\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tHL,M_STR\n"
		+ "\tRET\n"
		+ "S_STR3:\tLD\tA,0FFH\n"
		+ "\tOR\tA\n"			// CY=0
		+ "S_STR4:\tINC\tA\n"		// CY unveraendert
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,S_STR4\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tC,A\n"
		+ "\tOR\tB\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,'0'\n"
		+ "\tLD\tB,A\n"
		+ "\tADD\tA,C\n"
		+ "\tEXX\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tRET\n"
		+ "S_STR9:\tDB\t'-32768'\n"
		+ "\tDB\t00H\n" );
    }
    if( libItems.contains( LibItem.JP_HL ) ) {
      /*
       * Sprung zu der Adresse, die im HL-Register steht
       *
       * Parameter:
       *   HL: Adresse
       */
      buf.append( "JP_HL:\tJP\t(HL)\n" );
    }
    if( libItems.contains( LibItem.OUTSP ) ) {
      /*
       * Ausgabe eines Leerzeichens auf dem Bildschirm
       */
      buf.append( "OUTSP:\tLD\tA,20H\n"
		+ "\tJP\tXOUTCH\n" );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.DATA ) ) {
      /*
       * Leseposition fuer Daten auf den Anfang des Datenbereichs setzen,
       */
      buf.append( "DINIT:\tLD\tHL,DBEG\n"
		+ "\tLD\t(M_READ),HL\n"
		+ "\tRET\n" );
    }
    buf.append( "XEXIT:" );
    if( libItems.contains( LibItem.IOCTB1 ) ) {
      buf.append( "\tLD\tHL,IOCTB1\n"
		+ "\tCALL\tCKCLOS\n" );
    }
    if( libItems.contains( LibItem.IOCTB2 ) ) {
      buf.append( "\tLD\tHL,IOCTB2\n"
		+ "\tCALL\tCKCLOS\n" );
    }
    buf.append( "\tLD\tSP,(M_STCK)\n" );
    target.appendExit( buf );
    if( libItems.contains( LibItem.IOCTB1 )
	|| libItems.contains( LibItem.IOCTB2 ) )
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
    target.appendInput(
		buf,
		libItems.contains( LibItem.XCKBRK ),
		libItems.contains( LibItem.XINKEY ),
		libItems.contains( LibItem.XINCH ),
		options.canBreakOnInput() );
    if( libItems.contains( LibItem.XBREAK )
	|| options.canBreakOnInput() )
    {
      // Zeilenumbruch und Programm beenden
      buf.append( "XBREAK:\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      libItems.add( LibItem.XOUTST );
      libItems.add( LibItem.XOUTNL );
    }
    if( libItems.contains( LibItem.XBORDER ) ) {
      target.appendXBORDER( buf );
    }
    if( libItems.contains( LibItem.XCOLOR ) ) {
      target.appendXCOLOR( buf );
    }
    if( libItems.contains( LibItem.XINK ) ) {
      target.appendXINK( buf );
    }
    if( libItems.contains( LibItem.XPAPER ) ) {
      target.appendXPAPER( buf );
    }
    if( libItems.contains( LibItem.XCLS ) ) {
      target.appendXCLS( buf );
    }
    if( libItems.contains( LibItem.XLOCATE ) ) {
      target.appendXLOCATE( buf );
    }
    if( libItems.contains( LibItem.XCURS ) ) {
      target.appendXCURS( buf );
    }
    if( libItems.contains( LibItem.XPAUSE ) ) {
      buf.append( "XPAUSE:\tLD\tDE," );
      buf.appendHex4( target.get100msLoopCount() );
      buf.append( "\n"
		+ "XPAUS1:\tLD\tB,0\n"
		+ "XPAUS2:\tDJNZ\tXPAUS2\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,XPAUS1\n"
		+ "\tRET\n" );
    }
    // XPEN vor den anderen Grafikroutinen hinzufuegen!
    if( libItems.contains( LibItem.XPEN ) ) {
      target.appendXPEN( buf );
    }
    if( libItems.contains( LibItem.XHLINE ) ) {
      target.appendXHLINE( buf, compiler );
    }
    if( libItems.contains( LibItem.XPRES ) ) {
      target.appendXPRES( buf, compiler );
    }
    if( libItems.contains( LibItem.XPSET ) ) {
      target.appendXPSET( buf, compiler );
    }
    if( libItems.contains( LibItem.XPTEST ) ) {
      target.appendXPTEST( buf, compiler );
    }
    if( libItems.contains( LibItem.XSCRS ) ) {
      target.appendXSCRS( buf );
    }
    if( libItems.contains( LibItem.XJOY ) ) {
      target.appendXJOY( buf );
    }
    if( libItems.contains( LibItem.XOUTST ) || target.needsXOUTST() ) {
      /*
       * Ausgabe eines mit einem Null-Byte abgeschlossenen Textes
       * auf dem Bildschirm,
       * der sich unmittelbar an den Funktionsaufruf anschliesst.
       */
      buf.append( "XOUTST:\tEX\t(SP),HL\n"
		+ "\tCALL\tXOUTS\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      libItems.add( LibItem.XOUTS );
    }
    if( libItems.contains( LibItem.XOUTS ) ) {
      /*
       * Ausgabe eines mit einem Null-Byte abgeschlossenen Textes
       * auf dem Bildschirm
       *
       * Parameter:
       *   HL: Adresse des Textes
       * Rueckgabe:
       *   HL: erstes Byte hinter dem abschliessenden Null-Byte
       */
      buf.append( "XOUTS:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tXOUTS\n" );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.XOUTNL ) ) {
      target.appendXOUTNL( buf );
      libItems.add( LibItem.XOUTCH );
    }
    if( libItems.contains( LibItem.XOUTCH ) ) {
      target.appendXOUTCH( buf );
    }
    if( libItems.contains( LibItem.XLPTCH ) ) {
      target.appendXLPTCH( buf );
    }
    target.appendEtc( buf );
  }


  public static void appendDataTo(
			BasicCompiler      compiler,
			Map<String,String> strLitLabelMap,
			StringBuilder      userData )
  {
    AsmCodeBuf                buf      = compiler.getCodeBuf();
    Set<BasicLibrary.LibItem> libItems = compiler.getLibItems();
    BasicOptions              options  = compiler.getBasicOptions();
    AbstractTarget            target   = compiler.getTarget();
    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Datenbereich\n" );
    }
    if( libItems.contains( LibItem.IOOPEN ) ) {
      buf.append( "IOHNTB:\n" );
      if( libItems.contains( LibItem.IO_VDIP_HANDLER ) ) {
	buf.append( "\tDW\tIO_VDIP_HANDLER\n" );
      }
      if( libItems.contains( LibItem.IO_CRT_HANDLER ) ) {
	buf.append( "\tDW\tIO_CRT_HANDLER\n" );
      }
      if( libItems.contains( LibItem.IO_LPT_HANDLER ) ) {
	buf.append( "\tDW\tIO_LPT_HANDLER\n" );
      }
      buf.append( "\tDW\t0000H\n" );
    }
    Set<Map.Entry<String,String>> entries = strLitLabelMap.entrySet();
    if( entries != null ) {
      for( Map.Entry<String,String> e : entries ) {
	String label = e.getValue();
	if( label != null ) {
	  if( !label.isEmpty() ) {
	    buf.append( label );
	    buf.append( (char) ':' );
	    if( label.length() > 6 ) {
	      buf.append( (char) '\n' );
	    }
	    buf.appendStringLiteral( e.getKey() );
	  }
	}
      }
    }
    if( libItems.contains( LibItem.XTARID ) ) {
      target.appendXTARID( buf );
    }
    if( libItems.contains( LibItem.IO_CRT_HANDLER ) ) {
      buf.append( "D_IOCRT:\n"
			+ "\tDB\t\'CRT:\'\n" );
    }
    if( libItems.contains( LibItem.IO_LPT_HANDLER ) ) {
      buf.append( "D_IOLPT:\n"
			+ "\tDB\t\'LPT:\'\n" );
    }
    if( libItems.contains( LibItem.IO_VDIP_HANDLER ) ) {
      VdipLibrary.appendDataTo( compiler );
    }
    KCNetLibrary.appendDataTo( compiler );
    if( libItems.contains( LibItem.D_EMPT ) ) {
      buf.append( "D_EMPT:\tDB\t00H\n" );
    }
    if( libItems.contains( LibItem.FNT5P7 ) ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;Zeichensatz\n" );
      }
      buf.append( "FNT5P7:\tDB\t01H,0FAH,00H,00H,00H,00H\n"	// !
		+ "\tDB\t03H,0E0H,00H,0E0H,00H,00H\n"		// "
		+ "\tDB\t05H,28H,0FEH,28H,0FEH,28H\n"		// #
		+ "\tDB\t05H,24H,54H,0FEH,54H,48H\n"		// $
		+ "\tDB\t05H,0C4H,0C8H,10H,26H,46H\n"		// %
		+ "\tDB\t05H,0CH,72H,9AH,64H,0AH\n"		// &
		+ "\tDB\t02H,20H,0C0H,00H,00H,00H\n"		// '
		+ "\tDB\t03H,38H,44H,82H,00H,00H\n"		// (
		+ "\tDB\t03H,82H,44H,38H,00H,00H\n"		// )
		+ "\tDB\t05H,28H,10H,7CH,10H,28H\n"		// *
		+ "\tDB\t05H,10H,10H,7CH,10H,10H\n"		// +
		+ "\tDB\t02H,02H,0CH,00H,00H,00H\n"		// ,
		+ "\tDB\t05H,10H,10H,10H,10H,10H\n"		// -
		+ "\tDB\t01H,02H,00H,00H,00H,00H\n"		// .
		+ "\tDB\t05H,04H,08H,10H,20H,40H\n"		// /
		+ "\tDB\t05H,7CH,8AH,92H,0A2H,7CH\n"		// 0
		+ "\tDB\t03H,42H,0FEH,02H,00H,00H\n"		// 1
		+ "\tDB\t05H,46H,8AH,92H,92H,62H\n"		// 2
		+ "\tDB\t05H,84H,82H,92H,0B2H,0CCH\n"		// 3
		+ "\tDB\t05H,18H,28H,48H,0FEH,08H\n"		// 4
		+ "\tDB\t05H,0E4H,0A2H,0A2H,0A2H,9CH\n"		// 5
		+ "\tDB\t05H,0CH,32H,52H,92H,0CH\n"		// 6
		+ "\tDB\t05H,80H,8EH,90H,0A0H,0C0H\n"		// 7
		+ "\tDB\t05H,6CH,92H,92H,92H,6CH\n"		// 8
		+ "\tDB\t05H,60H,92H,94H,98H,60H\n"		// 9
		+ "\tDB\t01H,24H,00H,00H,00H,00H\n"		// :
		+ "\tDB\t02H,02H,2CH,00H,00H,00H\n"		// ;
		+ "\tDB\t04H,10H,28H,44H,82H,00H\n"		// <
		+ "\tDB\t05H,28H,28H,28H,28H,28H\n"		// =
		+ "\tDB\t04H,82H,44H,28H,10H,00H\n"		// >
		+ "\tDB\t05H,40H,80H,8AH,90H,60H\n"		// ?
		+ "\tDB\t05H,7CH,82H,9AH,0AAH,78H\n"		// @
		+ "\tDB\t05H,3EH,48H,88H,48H,3EH\n"		// A
		+ "\tDB\t05H,82H,0FEH,92H,92H,6CH\n"		// B
		+ "\tDB\t05H,7CH,82H,82H,82H,44H\n"		// C
		+ "\tDB\t05H,82H,0FEH,82H,82H,7CH\n"		// D
		+ "\tDB\t04H,0FEH,92H,92H,82H,00H\n"		// E
		+ "\tDB\t04H,0FEH,90H,90H,80H,00H\n"		// F
		+ "\tDB\t05H,7CH,82H,82H,92H,5CH\n"		// G
		+ "\tDB\t05H,0FEH,10H,10H,10H,0FEH\n"		// H
		+ "\tDB\t03H,82H,0FEH,82H,00H,00H\n"		// I
		+ "\tDB\t05H,04H,02H,82H,0FCH,80H\n"		// J
		+ "\tDB\t05H,0FEH,10H,28H,44H,82H\n"		// K
		+ "\tDB\t04H,0FEH,02H,02H,02H,00H\n"		// L
		+ "\tDB\t05H,0FEH,40H,30H,40H,0FEH\n"		// M
		+ "\tDB\t05H,0FEH,60H,10H,0CH,0FEH\n"		// N
		+ "\tDB\t05H,7CH,82H,82H,82H,7CH\n"		// O
		+ "\tDB\t05H,0FEH,90H,90H,90H,60H\n"		// P
		+ "\tDB\t05H,7CH,82H,8AH,84H,7AH\n"		// Q
		+ "\tDB\t05H,0FEH,90H,98H,94H,62H\n"		// R
		+ "\tDB\t05H,64H,92H,92H,92H,4CH\n"		// S
		+ "\tDB\t05H,80H,80H,0FEH,80H,80H\n"		// T
		+ "\tDB\t05H,0FCH,02H,02H,02H,0FCH\n"		// U
		+ "\tDB\t05H,0E0H,18H,06H,18H,0E0H\n"		// V
		+ "\tDB\t05H,0FCH,02H,1CH,02H,0FCH\n"		// W
		+ "\tDB\t05H,0C6H,28H,10H,28H,0C6H\n"		// X
		+ "\tDB\t05H,0E0H,10H,0EH,10H,0E0H\n"		// Y
		+ "\tDB\t05H,86H,8AH,92H,0A2H,0C2H\n"		// Z
		+ "\tDB\t03H,0FEH,82H,82H,00H,00H\n"		// [
		+ "\tDB\t05H,40H,20H,10H,08H,04H\n"		// \
		+ "\tDB\t03H,82H,82H,0FEH,00H,00H\n"		// ]
		+ "\tDB\t05H,20H,40H,80H,40H,20H\n"		// ^
		+ "\tDB\t05H,02H,02H,02H,02H,02H\n"		// _
		+ "\tDB\t02H,80H,40H,00H,00H,00H\n"		// `
		+ "\tDB\t05H,04H,2AH,2AH,2AH,1EH\n"		// a
		+ "\tDB\t05H,0FEH,22H,22H,22H,1CH\n"		// b
		+ "\tDB\t04H,1CH,22H,22H,22H,00H\n"		// c
		+ "\tDB\t05H,1CH,22H,22H,22H,0FEH\n"		// d
		+ "\tDB\t05H,1CH,2AH,2AH,2AH,18H\n"		// e
		+ "\tDB\t04H,20H,7EH,0A0H,80H,00H\n"		// f
		+ "\tDB\t05H,18H,25H,25H,25H,3EH\n"		// g
		+ "\tDB\t04H,0FEH,20H,20H,1EH,00H\n"		// h
		+ "\tDB\t03H,22H,0BEH,02H,00H,00H\n"		// i
		+ "\tDB\t04H,02H,01H,21H,0BEH,00H\n"		// j
		+ "\tDB\t05H,0FEH,04H,08H,14H,22H\n"		// k
		+ "\tDB\t03H,0FCH,02H,00H,00H,00H\n"		// l
		+ "\tDB\t05H,3EH,20H,1EH,20H,1EH\n"		// m
		+ "\tDB\t05H,3EH,10H,20H,20H,1EH\n"		// n
		+ "\tDB\t05H,1CH,22H,22H,22H,1CH\n"		// o
		+ "\tDB\t05H,3FH,24H,24H,24H,18H\n"		// p
		+ "\tDB\t05H,18H,24H,24H,24H,3FH\n"		// q
		+ "\tDB\t04H,3EH,10H,20H,20H,00H\n"		// r
		+ "\tDB\t05H,12H,2AH,2AH,2AH,24H\n"		// s
		+ "\tDB\t03H,20H,0FCH,22H,00H,00H\n"		// t
		+ "\tDB\t05H,3CH,02H,02H,04H,3EH\n"		// u
		+ "\tDB\t05H,38H,04H,02H,04H,38H\n"		// v
		+ "\tDB\t05H,3CH,02H,0CH,02H,3CH\n"		// w
		+ "\tDB\t05H,22H,14H,08H,14H,22H\n"		// x
		+ "\tDB\t05H,31H,0AH,04H,08H,30H\n"		// y
		+ "\tDB\t05H,22H,26H,2AH,32H,22H\n"		// z
		+ "\tDB\t03H,10H,6CH,82H,00H,00H\n"		// {
		+ "\tDB\t01H,0FEH,00H,00H,00H,00H\n"		// |
		+ "\tDB\t03H,82H,6CH,10H,00H,00H\n"		// }
		+ "\tDB\t05H,10H,20H,10H,08H,10H\n"		// ~
		+ "\n" );
    }
    target.appendDataTo( buf );
    if( userData != null ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;User DATA\n" );
      }
      buf.append( userData );
    }
  }


  public static void appendBssTo(
			BasicCompiler       compiler,
			Map<String,VarDecl> varDecls,
			Set<String>         usrLabels,
			StringBuilder       userBSS )
  {
    AsmCodeBuf                buf      = compiler.getCodeBuf();
    Set<BasicLibrary.LibItem> libItems = compiler.getLibItems();
    BasicOptions              options  = compiler.getBasicOptions();
    AbstractTarget            target   = compiler.getTarget();
    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Speicherzellen\n" );
    }
    if( options.getPrintLineNumOnAbort() ) {
      buf.append( "M_SRLN:\tDS\t2\n"
		+ "M_BALN:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.DATA ) ) {
      /*
       * Speicherzelle fuer Leseposition anlegen
       */
      buf.append( "M_READ:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.F_RND ) ) {
      /*
       * Speicherzelle fuer den Zufallsgenerator
       */
      buf.append( "M_RND:\tDS\t2\n" );
    }
    KCNetLibrary.appendBssTo( compiler );
    if( libItems.contains( LibItem.IO_VDIP_HANDLER ) ) {
      VdipLibrary.appendBssTo( compiler );
    }
    if( libItems.contains( LibItem.KCNET_BASE )
	|| libItems.contains( LibItem.IOOPEN )
	|| libItems.contains( LibItem.IOCTB1 )
	|| libItems.contains( LibItem.IOCTB2 ) )
    {
      buf.append( "M_IONM:\tDS\t02H\n"
		+ "M_IOCA:\tDS\t02H\n"
		+ "M_IOAC:\tDS\t01H\n" );
    }
    if( libItems.contains( LibItem.IOCTB1 ) ) {
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
       *   +4:  AVAILABLE-Routine
       *          In:  DE: Anfangsadresse Kanalzeigerfeld
       *          Out: HL: Anzahl der verfuegbaren Bytes
       *   +6:  READ-Routine
       *          In:  DE: Anfangsadresse Kanalzeigerfeld
       *          Out: A:  gelesenes Zeichen bzw. 00h ab EOF
       *   +8:  WRITE-Routine
       *          In:  (M_IOCA): Anfangsadresse Kanalzeigerfeld
       *               A:        zu schreibendes Byte
       *   +10: FLUSH-Routine
       *          In:  DE: Anfangsadresse Kanalzeigerfeld
       *   +12: Status Zeichenpuffer
       *   +13: Zeichenpuffer
       *   +14: Treiber-spezifisches Feld
       */
      buf.append( "IOCTB1:\tDS\t" );
      buf.appendHex2( compiler.getIOChannelSize() );
      buf.newLine();
    }
    if( libItems.contains( LibItem.IOCTB2 ) ) {
      /*
       * Zeigerfeld fuer Kanal 2,
       * muss unmittelbar hinter Kanal 1 folgen
       */
      buf.append( "IOCTB2:\tDS\t" );
      buf.appendHex2( compiler.getIOChannelSize() );
      buf.newLine();
    }
    if( libItems.contains( LibItem.M_XYPO ) ) {
      buf.append( "M_XPOS:\tDS\t2\n"
		+ "M_YPOS:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.DRBOX )
	|| libItems.contains( LibItem.DRBOXF )
	|| libItems.contains( LibItem.DRLINE ) )
    {
      buf.append( "M_LNBX:\tDS\t2\n"
		+ "M_LNBY:\tDS\t2\n"
		+ "M_LNEX:\tDS\t2\n"
		+ "M_LNEY:\tDS\t2\n"
		+ "M_LNSX:\tDS\t2\n"
		+ "M_LNSY:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.CIRCLE ) ) {
      buf.append( "M_CIMX:\tDS\t2\n"
		+ "M_CIMY:\tDS\t2\n"
		+ "M_CIRA:\tDS\t2\n"
		+ "M_CIER:\tDS\t2\n"
		+ "M_CIRX:\tDS\t2\n"
		+ "M_CIRY:\tDS\t2\n"
		+ "M_CXMX:\tDS\t2\n"
		+ "M_CXPX:\tDS\t2\n"
		+ "M_CXMY:\tDS\t2\n"
		+ "M_CXPY:\tDS\t2\n"
		+ "M_CYMX:\tDS\t2\n"
		+ "M_CYPX:\tDS\t2\n"
		+ "M_CYMY:\tDS\t2\n"
		+ "M_CYPY:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_FRET ) ) {
      buf.append( "M_FRET:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_INKB ) ) {
      buf.append( "M_INKB:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_IO_COUT ) ) {
      buf.append( "M_IO_COUT:\n"
		+ "\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_HOST ) ) {
      buf.append( "M_HOST:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_PORT ) ) {
      buf.append( "M_PORT:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_STMP ) ) {
      // temporaerer String-Puffer
      buf.append( "M_STMP:\tDS\t" );
      buf.appendHex4( BasicCompiler.MAX_STR_LEN + 1 );
      buf.append( (char) '\n' );
    }
    if( libItems.contains( LibItem.S_STR ) ) {
      // Puffer fuer S_STR
      buf.append( "M_STR:\tDS\t08H\n" );
    }
    if( libItems.contains( LibItem.M_ERN ) ) {
      // Fehlernummer
      buf.append( "M_ERN:\tDS\t2\n" );
    }
    if( libItems.contains( LibItem.M_ERT ) ) {
      // Fehlertext
      buf.append( "M_ERT:\tDS\t2\n" );
    }
    // Stack Pointer bei Programmstart
    buf.append( "M_STCK:\tDS\t2\n" );
    target.appendBssTo( buf );
    if( options.getShowAssemblerText() && !usrLabels.isEmpty() ) {
      buf.append( "\n;USR-Funktionen\n" );
    }
    for( String usrLabel : usrLabels ) {
      buf.append( usrLabel );
      buf.append( ":\tDS\t2\n" );
    }
    Collection<String> varNames = varDecls.keySet();
    if( varNames != null ) {
      int nVars = varNames.size();
      if( nVars > 0 ) {
	try {
	  String[] a = varNames.toArray( new String[ nVars ] );
	  if( a != null ) {
	    try {
	      Arrays.sort( a );
	    }
	    catch( ClassCastException ex ) {}
	    if( a.length > 0 ) {
	      if( options.getShowAssemblerText() ) {
		buf.append( "\n;BASIC-Variablen\n" );
	      }
	      for( String varName : a ) {
		VarDecl varDecl = varDecls.get( varName );
		if( varDecl != null ) {
		  String varLabel = varDecl.getLabel();
		  buf.append( varLabel );
		  buf.append( (char) ':' );
		  if( varLabel.length() > 6 ) {
		    buf.append( (char) '\n' );
		  }
		  buf.append( "\tDS\t" );
		  int varSize = varDecl.getSize();
		  if( varSize > 2 ) {
		    if( varSize < 0x100 ) {
		      buf.appendHex2( varSize );
		    } else {
		      buf.appendHex4( varSize );
		    }
		  } else {
		    buf.append( (char) '2' );
		  }
		  buf.append( (char) '\n' );
		}
	      }
	    }
	  }
	}
	catch( ArrayStoreException ex ) {}
      }
    }
    if( userBSS != null ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;User BSS\n" );
      }
      buf.append( userBSS );
    }
    if( libItems.contains( LibItem.HEAP ) ) {
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
      buf.append( "\n"
		+ "M_TOP:\n" );
    } else {
      if( libItems.contains( LibItem.M_TOP ) ) {
	buf.append( "M_TOP:\n" );
      }
    }
  }



  public static void appendInitTo(
			BasicCompiler       compiler,
			Map<String,VarDecl> varDecls,
			Set<String>         usrLabels )
  {
    AsmCodeBuf   buf      = compiler.getCodeBuf();
    Set<LibItem> libItems = compiler.getLibItems();
    BasicOptions options  = compiler.getBasicOptions();
    if( options.getShowAssemblerText() ) {
      buf.append( "\n;Initialisierungen\n" );
    }
    buf.append( "MINIT:\tLD\t(M_STCK),SP\n" );
    if( options.getStackSize() > 0 ) {
      buf.append( "\tLD\tSP,M_TOP\n" );
      libItems.add( LibItem.M_TOP );
    }
    if( options.getPrintLineNumOnAbort() ) {
      buf.append( "\tLD\tHL,0FFFFH\n"
		+ "\tLD\t(M_SRLN),HL\n"
		+ "\tLD\t(M_BALN),HL\n" );
    }
    if( libItems.contains( LibItem.F_RND ) ) {
      buf.append( "\tLD\tA,R\n"
		+ "\tLD\t(M_RND),A\n" );
    }
    if( libItems.contains( LibItem.DATA ) ) {
      buf.append( "\tCALL\tDINIT\n" );
    }
    KCNetLibrary.appendInitTo( compiler );
    if( libItems.contains( LibItem.IO_VDIP_HANDLER ) ) {
      VdipLibrary.appendInitTo( compiler );
    }
    /*
     * Zeigerfelder der einzelnen Kanaele initialisieren
     */
    int    nChannels         = 0;
    String firstChannelLabel = null;
    if( libItems.contains( LibItem.IOCTB2 ) ) {
      firstChannelLabel = "IOCTB2";
      nChannels++;
    }
    if( libItems.contains( LibItem.IOCTB1 ) ) {
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
    if( libItems.contains( LibItem.M_XYPO ) ) {
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tLD\t(M_YPOS),HL\n" );
    }
    if( libItems.contains( LibItem.M_ERN ) ) {
      if( !libItems.contains( LibItem.M_XYPO ) ) {
	buf.append( "\tLD\tHL,0000H\n" );
      }
      buf.append( "\tLD\t(M_ERN),HL\n" );
    }
    if( libItems.contains( LibItem.M_ERT ) ) {
      buf.append( "\tLD\tHL,D_EMPT\n"
		+ "\tLD\t(M_ERT),HL\n" );
      libItems.add( LibItem.D_EMPT );
    }
    Collection<String> varNames = varDecls.keySet();
    if( varNames != null ) {
      int nVars = varNames.size();
      if( nVars > 0 ) {
	try {
	  String[] a = varNames.toArray( new String[ nVars ] );
	  if( a != null ) {
	    try {
	      Arrays.sort( a );
	    }
	    catch( ClassCastException ex ) {}
	    String firstStrVarLabel = null;
	    int    nStrVars         = 0;
	    for( int i = 0; i< a.length; i++ ) {
	      if( a[ i ].endsWith( "$" ) ) {
		VarDecl var = varDecls.get( a[ i ] );
		if( var != null ) {
		  nStrVars += (var.getSize() / 2);
		  if( firstStrVarLabel == null ) {
		    firstStrVarLabel = var.getLabel();
		  }
		}
	      }
	    }
	    if( nStrVars > 0 ) {
	      if( nStrVars > 3 ) {
		if( firstStrVarLabel != null ) {
		  if( nStrVars > 0x0100 ) {
		    buf.append( "\tLD\tBC," );
		    buf.appendHex4( nStrVars );
		  } else {
		    buf.append( "\tLD\tB," );
		    buf.appendHex2( nStrVars );
		  }
		  buf.append( "\n"
			+ "\tLD\tDE,D_EMPT\n"
			+ "\tLD\tHL," );
		  buf.append( firstStrVarLabel );
		  buf.append( "\n"
			+ "MINIST:\tLD\t(HL),E\n"
			+ "\tINC\tHL\n"
			+ "\tLD\t(HL),D\n"
			+ "\tINC\tHL\n" );
		  if( nStrVars > 0x0100 ) {
		    buf.append( "\tDEC\tBC\n"
			+ "\tLD\tA,B\n"
			+ "\tOR\tC\n"
			+ "\tJR\tNZ,MINIST\n" );
		  } else {
		    buf.append( "\tDJNZ\tMINIST\n" );
		  }
		  libItems.add( LibItem.D_EMPT );
		}
	      } else {
		buf.append( "\tLD\tHL,D_EMPT\n" );
		for( int i = 0; i< a.length; i++ ) {
		  if( a[ i ].endsWith( "$" ) ) {
		    VarDecl var = varDecls.get( a[ i ] );
		    if( var != null ) {
		      buf.append( "\tLD\t(" );
		      buf.append( var.getLabel() );
		      buf.append( "),HL\n" );
		    }
		  }
		}
		libItems.add( LibItem.D_EMPT );
	      }
	    }
	  }
	}
	catch( ArrayStoreException ex ) {}
      }
    }
    if( libItems.contains( LibItem.HEAP ) ) {
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


  public static void appendResetErrorUseBC( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERN ) ) {
      compiler.getCodeBuf().append( "\tLD\tBC,0000H\n"
				+ "\tLD\t(M_ERN),BC\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERT ) ) {
      compiler.getCodeBuf().append( "\tLD\tBC,D_EMPT\n"
				+ "\tLD\t(M_ERT),BC\n" );
      compiler.getLibItems().add( BasicLibrary.LibItem.D_EMPT );
    }
  }


  public static void appendResetErrorUseDE( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERN ) ) {
      compiler.getCodeBuf().append( "\tLD\tDE,0000H\n"
				+ "\tLD\t(M_ERN),DE\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERT ) ) {
      compiler.getCodeBuf().append( "\tLD\tDE,D_EMPT\n"
				+ "\tLD\t(M_ERT),DE\n" );
      compiler.getLibItems().add( BasicLibrary.LibItem.D_EMPT );
    }
  }


  public static void appendResetErrorUseHL( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERN ) ) {
      compiler.getCodeBuf().append( "\tLD\tHL,0000H\n"
				+ "\tLD\t(M_ERN),HL\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERT ) ) {
      compiler.getCodeBuf().append( "\tLD\tHL,D_EMPT\n"
				+ "\tLD\t(M_ERT),HL\n" );
      compiler.getLibItems().add( BasicLibrary.LibItem.D_EMPT );
    }
  }


  public static void appendSetError(
			BasicCompiler compiler,
			int           errCode,
			String        errTextDE,
			String        errTextEN )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERN ) ) {
      buf.append_LD_HL_nn( errCode );
      buf.append( "\tLD\t(M_ERN),HL\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERT ) ) {
      buf.append_LD_HL_xx(
	compiler.getStringLiteralLabel(
		compiler.isLangCode( "DE" ) ? errTextDE : errTextEN ) );
      buf.append( "\tLD\t(M_ERT),HL\n" );
    }
  }


  public static void appendSetError( BasicCompiler compiler )
  {
    appendSetError( compiler, E_ERROR, "Fehler", "Error" );
  }


  public static void appendSetErrorChannelAlreadyOpen( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_CHANNEL_CLOSED,
		"Kanal bereits geoeffnet",
		"Channel already open" );
  }


  public static void appendSetErrorChannelClosed( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_CHANNEL_CLOSED,
		"Kanal geschlossen",
		"Channel closed" );
  }


  public static void appendSetErrorDiskFull( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_DISK_FULL,
		"Speichermedium voll",
		"Disk full" );
  }


  public static void appendSetErrorDeviceLocked( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_DEVICE_LOCKED,
		"Geraet bereits in Benutzung",
		"Device locked" );
  }


  public static void appendSetErrorDeviceNotFound( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_DEVICE_NOT_FOUND,
		"Geraet nicht gefunden",
		"Device not found" );
  }


  public static void appendSetErrorEOF( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_EOF,
		"Dateiende erreicht",
		"End of file reached" );
  }


  public static void appendSetErrorFileNotFound( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_FILE_NOT_FOUND,
		"Datei nicht gefunden",
		"File not found" );
  }


  public static void appendSetErrorFileReadOnly( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_READ_ONLY,
		"Datei schreibgeschuetzt",
		"File write protected" );
  }


  public static void appendSetErrorIOError( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_IO_ERROR,
		"Ein-/Ausgabefehler",
		"IO error" );
  }


  public static void appendSetErrorInvalidChars( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_INVALID,
		"Ungueltige Zeichen",
		"Invalid characters" );
  }


  public static void appendSetErrorIOMode( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_IO_MODE,
		"Ungueltige Betriebsart",
		"Invalid IO mode" );
  }


  public static void appendSetErrorNoDisk( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_NO_DISK,
		"Kein Speichermedium vorhanden",
		"No disk" );
  }


  public static void appendSetErrorNumericOverflow( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		E_OVERFLOW,
		"Numerischer Ueberlauf",
		"Numeric overflow" );
  }
}

