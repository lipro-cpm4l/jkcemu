/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KCNet-Bibliothek
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Set;
import jkcemu.net.W5100;


public class KCNetLibrary
{
  public static final int IOCTB_SOCKET_OFFS  = BasicLibrary.IOCTB_DRIVER_OFFS;
  public static final int IOCTB_PROTO_OFFS   = IOCTB_SOCKET_OFFS + 1;
  public static final int IOCTB_RX_CNT_OFFS  = IOCTB_PROTO_OFFS + 1;
  public static final int IOCTB_RX_RMN_OFFS  = IOCTB_RX_CNT_OFFS + 2;
  public static final int IOCTB_RX_POS_OFFS  = IOCTB_RX_RMN_OFFS + 2;
  public static final int IOCTB_TX_FSR_OFFS  = IOCTB_RX_POS_OFFS + 2;
  public static final int IOCTB_TX_WR_OFFS   = IOCTB_TX_FSR_OFFS + 2;
  public static final int IOCTB_REMOTE_OFFS  = IOCTB_TX_WR_OFFS + 2;
  public static final int IOCTB_CHANNEL_SIZE = IOCTB_REMOTE_OFFS + 6;


  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf                buf      = compiler.getCodeBuf();
    Set<BasicLibrary.LibItem> libItems = compiler.getLibItems();
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SET_DNSSERVER ) ) {
      /*
       * Setzen des DNS-Servers,
       * Fehlervariablen werden gesetzt
       * Parameter:
       *   HL:   Zeiger auf die textuelle IP-Adresse
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler
       */
      buf.append( "KCNET_SET_DNSSERVER:\n" );
      BasicLibrary.appendResetErrorUseBC( compiler );
      buf.append( "\tCALL\tKCNET_PARSE_IPADDR\n"
		+ "\tJR\tC,KCNET_SET_ERR\n"
		+ "\tCALL\tKCNET_INIT_HW\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,06H\n"		// IP-Adresse schreiben
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tJR\tKCNET_SET_IPADDR1\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_PARSE_IPADDR );
      libItems.add( BasicLibrary.LibItem.KCNET_SET_BASE );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SET_GATEWAY ) ) {
      /*
       * Setzen des Gateways
       * Fehlervariablen werden gesetzt
       * Parameter:
       *   HL:   Zeiger auf die textuelle IP-Adresse
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler
       */
      buf.append( "KCNET_SET_GATEWAY:\n"
		+ "\tLD\tDE,8001H\n"
		+ "\tJR\tKCNET_SET_IPADDR\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_SET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SET_LOCALADDR ) ) {
      /*
       * Setzen der lokalen IP-Adresse
       * Fehlervariablen werden gesetzt
       * Parameter:
       *   HL:   Zeiger auf die textuelle IP-Adresse
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler
       */
      buf.append( "KCNET_SET_LOCALADDR:\n"
		+ "\tLD\tDE,800FH\n"
		+ "\tJR\tKCNET_SET_IPADDR\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_SET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SET_NETMASK ) ) {
      /*
       * Setzen der Netzwerkmaske
       * Fehlervariablen werden gesetzt
       * Parameter:
       *   HL:   Zeiger auf die textuelle Netzwerkmaske
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler
       */
      buf.append( "KCNET_SET_NETMASK:\n"
		+ "\tLD\tDE,8005H\n" );
		// direkt weiter mit KCNET_SET_IPADDR
      libItems.add( BasicLibrary.LibItem.KCNET_SET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SET_BASE ) ) {
      /*
       * Schreiben einer IP-Adresse in den W5100-Adressraum
       * Parameter:
       *   DE: Zieladresse im KCNet
       *   HL: Zeiger auf die textuelle IP-Adresse
       */
      buf.append( "KCNET_SET_IPADDR:\n" );
      BasicLibrary.appendResetErrorUseBC( compiler );
      buf.append( "\tPUSH\tDE\n"
		+ "\tCALL\tKCNET_PARSE_IPADDR\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,KCNET_SET_ERR\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tKCNET_INIT_HW\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,02H\n"		// Adresszeiger schreiben
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,E\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"			// Bytes schreiben
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,04H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "KCNET_SET_IPADDR1:\n"
		+ "\tLD\tDE,KCNET_M_IPADDR\n"
		+ "\tLD\tB,04H\n"
		+ "KCNET_SET_IPADDR2:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tDJNZ\tKCNET_SET_IPADDR2\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
		+ "KCNET_SET_ERR:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_INVALID,
			"Ungueltige IP-Adresse/Maske",
			"Invalid ip address / mask" );
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_PARSE_IPADDR );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_HOSTBYNAME ) ) {
      /*
       * Parsen einer IP-Adresse oder eines Hostnamens
       * und Eintragen der IP-Adresse in KCNET_M_IPADDR
       * Parameter:
       *   HL:   Zeiger auf den Namen
       * Rueckgabewert:
       *   CY=0: OK, IP-Adresse steht in (KCNET_M_IPADDR)
       *   CY=1: Fehler
       */
      buf.append( "KCNET_HOSTBYNAME:\n"
		+ "\tCALL\tKCNET_PARSE_IPADDR\n"
		+ "\tRET\tNC\n"
      // IP-Adresse konnte nicht geparst werden -> DNS-Anfrage
		+ "\tLD\tA,03\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tCALL\tKCNET_INIT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tE,02H\n"			// UDP
		+ "\tCALL\tKCNET_SOCK_GET_PORT_AND_OPEN\n"
		+ "\tLD\tL,24H\n"			// Sn_TX_WR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\t(KCNET_M_DNS_TX_WR),HL\n"
      // ID (2 Bytes)
		+ "\tLD\tHL,KCNET_M_DNS_ID\n"
		+ "\tLD\tA,R\n"
		+ "\tRLD\n"
		+ "\tLD\tL,(HL)\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\t(KCNET_M_DNS_ID),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,H\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,L\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
      // Rest des Headers (10 Bytes)
		+ "\tLD\tHL,KCNET_DATA_DNSQ\n"
		+ "\tLD\tB,0AH\n"
		+ "KCNET_HOSTBYNAME1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,A\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tDJNZ\tKCNET_HOSTBYNAME1\n"
      // Hostname auswerten und an das Datenpaket anhaengen
		+ "\tEXX\n"
		+ "\tLD\tBC,0000H\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,(KCNET_M_HOST)\n"
		+ "\tLD\tC,00H\n"	// Zaehler fuer alle Zeichen
		+ "KCNET_HOSTBYNAME2:\n"	// neues Label
		+ "\tLD\tB,00H\n"	// Zaehler fuer Zeichen eines Labels
		+ "KCNET_HOSTBYNAME3:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME5\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME6\n"
		+ "KCNET_HOSTBYNAME4:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME4\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME8\n"
		+ "KCNET_HOSTBYNAME5:\n"
		+ "\tCALL\tKCNET_HOSTBYNAME9\n"
		+ "\tJR\tKCNET_HOSTBYNAME11\n"
		+ "KCNET_HOSTBYNAME6:\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME8\n"
		+ "\tCP\t7FH\n"
		+ "\tJR\tNC,KCNET_HOSTBYNAME8\n"
		+ "\tINC\tC\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME8\n"
		+ "\tCP\t2EH\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME7\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME8\n"
		+ "\tCALL\tKCNET_HOSTBYNAME9\n"
		+ "\tINC\tHL\n"
		+ "\tJR\tKCNET_HOSTBYNAME2\n"
		+ "KCNET_HOSTBYNAME7:\n"
		+ "\tINC\tB\n"
		+ "\tLD\tA,B\n"
		+ "\tCP\t41H\n"		// max. 64 Zeichen pro Label
		+ "\tJR\tC,KCNET_HOSTBYNAME3\n"
		+ "KCNET_HOSTBYNAME8:\n"
		+ "\tCALL\tKCNET_SOCK_CLOSE\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_INVALID,
			"Ungueltiger Hostname",
			"Invalid hostname" );
      buf.append( "\tSCF\n"
		+ "\tRET\n"
		+ "KCNET_HOSTBYNAME9:\n"
      // Anzahl Zeichen schreiben
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tC,B\n"
		+ "\tLD\tB,00H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tDEC\tHL\n"
		+ "\tPOP\tBC\n"
      // Zeichen schreiben
		+ "KCNET_HOSTBYNAME10:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,A\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tDJNZ\tKCNET_HOSTBYNAME10\n"
		+ "\tRET\n"
		+ "KCNET_HOSTBYNAME11:\n"
      // Label-Endezeichen schreiben
		+ "\tLD\tB,00H\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
      // DNS-Anfrage abschliessen (QTYPE und QCLASS, jeweils 1)
		+ "\tLD\tB,02H\n"
		+ "KCNET_HOSTBYNAME12:\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tB,00H\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tLD\tB,01H\n"
		+ "\tCALL\tKCNET_DNS_WRITE\n"
		+ "\tPOP\tBC\n"
		+ "\tDJNZ\tKCNET_HOSTBYNAME12\n"
		+ "\tEXX\n"
		+ "\tLD\t(KCNET_M_DNS_QDLEN),BC\n"
		+ "\tEXX\n"
      // IP-Adresse und Portnummer setzen
		+ "\tLD\tA,07H\n"		// IP-Addresse lesen
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tBC,0400H\n"
		+ "KCNET_HOSTBYNAME13:\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tPUSH\tAF\n"
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tDJNZ\tKCNET_HOSTBYNAME13\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME14\n"
		+ "\tPOP\tAF\n"
		+ "\tPOP\tAF\n"
		+ "\tPOP\tAF\n"
		+ "\tPOP\tAF\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_DNS_NOT_CONFIGURED,
			"DNS nicht konfiguriert",
			"DNS not configured" );
      buf.append( "\tJP\tKCNET_HOSTBYNAME24\n"
		+ "KCNET_HOSTBYNAME14:\n"
		+ "\tLD\tDE,040FH\n"	// 4 Durchlaeufe und Sn_DIPR+3
		+ "KCNET_HOSTBYNAME15:\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tC,E\n"
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tDEC\tE\n"
		+ "\tDEC\tD\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME15\n"
		+ "\tLD\tBC,0035H\n"			// Port 53
		+ "\tLD\tL,10H\n"			// Sn_DPORT
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tLD\tBC,(KCNET_M_DNS_TX_WR)\n"
		+ "\tCALL\tKCNET_SOCK_SEND\n"
		+ "\tJP\tC,KCNET_HOSTBYNAME24\n"
		+ "\tLD\tDE,0000H\n"			// Timeout-Zaehler
		+ "\tJR\tKCNET_HOSTBYNAME17\n"
      // neuer Empfang aktivieren
		+ "KCNET_HOSTBYNAME16:\n"
		+ "\tLD\tL,26H\n"			// Sn_RX_RSR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tPOP\tBC\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tLD\tBC,4001H\n"			// Sn_CR=RECV
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
      // auf Antwort warten
		+ "KCNET_HOSTBYNAME17:\n"
		+ "\tLD\tL,26H\n"			// Sn_RX_RSR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tH,A\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME18\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME17\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_TIMEOUT,
			"Keine DNS-Antwort",
			"No DNS response" );
      buf.append( "\tJP\tKCNET_HOSTBYNAME24\n"
		+ "KCNET_HOSTBYNAME18:\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RMN),HL\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RD),HL\n"
      // UDP-Header ueberlesen
		+ "\tLD\tBC,0008H\n"
		+ "\tCALL\tKCNET_DNS_SKIP\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME16\n"
      // ID lesen und vergleichen
		+ "\tCALL\tKCNET_DNS_READ_WORD\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME16\n"
		+ "\tLD\tBC,(KCNET_M_DNS_ID)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME16\n"
      // DNS-Antwort?
		+ "\tCALL\tKCNET_DNS_READ_BYTE\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME16\n"
		+ "\tAND\t80H\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME16\n"
      // DNS-Fehler?
		+ "\tCALL\tKCNET_DNS_READ_BYTE\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
		+ "\tAND\t0FH\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME20\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME23\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME23\n"
		+ "KCNET_HOSTBYNAME19:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_DNS_ERROR,
			"DNS-Anfrage fehlgeschlagen",
			"DNS error" );
      buf.append( "\tJR\tKCNET_HOSTBYNAME24\n"
		+ "KCNET_HOSTBYNAME20:\n"
      // QDCOUNT auswerten
		+ "\tCALL\tKCNET_DNS_READ_WORD\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME21\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tHL,0000H\n"			// kein QD-Feld
		+ "\tLD\t(KCNET_M_DNS_QDLEN),HL\n"
		+ "KCNET_HOSTBYNAME21:\n"
      // ANCOUNT auswerten
		+ "\tCALL\tKCNET_DNS_READ_WORD\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,KCNET_HOSTBYNAME19\n"
      // NSCOUNT und ARCOUNT ueberlesen
		+ "\tLD\tBC,0004H\n"
		+ "\tCALL\tKCNET_DNS_SKIP\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
      // QD-Feld ueberlesen
		+ "\tLD\tBC,(KCNET_M_DNS_QDLEN)\n"
		+ "\tCALL\tKCNET_DNS_SKIP\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
      // Antwortfeld
		+ "\tLD\tBC,000AH\n"
		+ "\tCALL\tKCNET_DNS_SKIP\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
		+ "\tCALL\tKCNET_DNS_READ_WORD\n"
		+ "\tJR\tC,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tBC,0004H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tNZ,KCNET_HOSTBYNAME19\n"
		+ "\tLD\tHL,KCNET_M_IPADDR\n"
		+ "\tLD\tB,04H\n"
		+ "KCNET_HOSTBYNAME22:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_DNS_READ_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tKCNET_HOSTBYNAME22\n"
		+ "\tCALL\tKCNET_SOCK_CLOSE\n"
		+ "\tOR\tA\n"				// CY=0
		+ "\tRET\n"
		+ "KCNET_HOSTBYNAME23:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_UNKNOWN_HOST,
			"Host nicht gefunden",
			"Host not found" );
      buf.append( "KCNET_HOSTBYNAME24:\n"
		+ "\tCALL\tKCNET_SOCK_CLOSE\n"
		+ "\tSCF\n"
		+ "\tRET\n"
      /*
       * Ueberspringen von empfangenen Bytes
       * Parameter:
       *   BC: Anzahl der zu ueberspringenen Bytes
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Ende uebersprungen
       */
		+ "KCNET_DNS_SKIP:\n"
		+ "\tLD\tHL,(KCNET_M_DNS_RX_RMN)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\tC\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RMN),HL\n"
		+ "\tLD\tHL,(KCNET_M_DNS_RX_RD)\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RD),HL\n"
		+ "\tRET\n"
      /*
       * Lesen eines Bytes
       * Rueckgabewert:
       *   CY=0: OK, gelesenes Byte in A
       *   CY=1: kein Byte mehr verfuegen
       */
		+ "KCNET_DNS_READ_BYTE:\n"
		+ "\tLD\tHL,(KCNET_M_DNS_RX_RMN)\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tSCF\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RMN),HL\n"
		+ "\tLD\tHL,(KCNET_M_DNS_RX_RD)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tOR\t0F8H\n"
		+ "\tLD\tH,A\n"
		+ "\tCALL\tKCNET_GET_BYTE\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(KCNET_M_DNS_RX_RD),HL\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
      /*
       * Lesen eines Wortes
       * Rueckgabewert:
       *   CY=0: OK, gelesenes in HL
       *   CY=1: kein Byte mehr verfuegen
       */
		+ "KCNET_DNS_READ_WORD:\n"
		+ "\tCALL\tKCNET_DNS_READ_BYTE\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tAF\n"
		+ "\tCALL\tKCNET_DNS_READ_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n"
      /*
       * Schreiben eines Bytes in DNS-Socket
       * Parameter:
       *   B: Byte
       */
		+ "KCNET_DNS_WRITE:\n"
		+ "\tLD\tHL,(KCNET_M_DNS_TX_WR)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tOR\t0D8H\n"
		+ "\tLD\tH,A\n"
		+ "\tCALL\tKCNET_SET_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\t(KCNET_M_DNS_TX_WR),HL\n"
		+ "\tEXX\n"
		+ "\tINC\tBC\n"
		+ "\tEXX\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_PARSE_IPADDR );
      libItems.add( BasicLibrary.LibItem.KCNET_SOCK );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_PARSE_IPADDR ) ) {
      /*
       * Parsen einer IP-Adresse und Eintragen dieser in KCNET_M_IPADDR
       * Parameter:
       *   HL:   Zeiger auf den Namen
       * Rueckgabewert:
       *   CY=0: OK, IP-Adresse steht in (KCNET_M_IPADDR)
       *   CY=1: Fehler
       */
      buf.append( "KCNET_PARSE_IPADDR:\n"
      // Leerzeichen uebergehen
		+ "\tDEC\tHL\n"
		+ "KCNET_PARSE_IPADDR1:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR1\n"
		+ "\tLD\t(KCNET_M_HOST),HL\n"
		+ "\tLD\tDE,KCNET_M_IPADDR\n"
		+ "KCNET_PARSE_IPADDR2:\n"
      // einzelne Zahl parsen
		// B: Wert der Zahl
		// C: Zaehler fuer Anzahl Ziffern
		+ "\tLD\tBC,0000H\n"
		+ "KCNET_PARSE_IPADDR3:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR4\n"
		+ "\tCP\t2EH\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR4\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR5\n"
		+ "\tSUB\t30H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,KCNET_PARSE_IPADDR6\n"
		+ "\tEX\tAF,AF\'\n"			// D=D*10
		+ "\tLD\tA,B\n"
		+ "\tADD\tA,A\n"
		+ "\tRET\tC\n"
		+ "\tADD\tA,A\n"
		+ "\tRET\tC\n"
		+ "\tADD\tA,B\n"
		+ "\tRET\tC\n"
		+ "\tADD\tA,A\n"
		+ "\tRET\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tADD\tA,B\n"
		+ "\tRET\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tINC\tC\n"
		+ "\tJR\tKCNET_PARSE_IPADDR3\n"
		+ "KCNET_PARSE_IPADDR4:\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\tC\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR6\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,LOW(KCNET_M_IPADDR+4)\n"
		+ "\tCP\tE\n"
		+ "\tJR\tNZ,KCNET_PARSE_IPADDR2\n"
      // IP-Adresse geparst, dahinter darf nichts mehr kommen
		+ "\tDEC\tHL\n"
		+ "KCNET_PARSE_IPADDR5:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,KCNET_PARSE_IPADDR5\n"
      // IP-Adresse konnte nicht geparst werden
		+ "KCNET_PARSE_IPADDR6:\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_MACADDR ) ) {
      /*
       * MAC-Adresse als String zurueckliefern
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette mit der MAC-Adresse
       */
      buf.append( "KCNET_S_MACADDR:\n" );
      BasicLibrary.appendResetErrorUseHL( compiler );
      buf.append( "\tCALL\tKCNET_INIT_HW\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,02H\n"		// Adresszeiger schreiben
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,09H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,80H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,01H\n"		// Bytes lesen
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,06H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tLD\tB,06H\n"
		+ "\tJR\tKCNET_S_MACADDR2\n"
		+ "KCNET_S_MACADDR1:\n"
		+ "\tLD\tA,3AH\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "KCNET_S_MACADDR2:\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tCALL\tS_HXA\n"
		+ "\tDJNZ\tKCNET_S_MACADDR1\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.S_HXA );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
      libItems.add( BasicLibrary.LibItem.D_EMPT );
      libItems.add( BasicLibrary.LibItem.M_STMP );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_DNSSERVER ) ) {
      /*
       * IP-Adresse des DNS-Servers als String zurueckliefern
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette mit der IP-Adresse
       */
      buf.append( "KCNET_S_DNSSERVER:\n" );
      BasicLibrary.appendResetErrorUseHL( compiler );
      buf.append( "\tCALL\tKCNET_INIT_HW\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,07H\n"
		+ "\tJR\tKCNET_S_GET_IPADDR1\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_S_GET_IPADDR );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
      libItems.add( BasicLibrary.LibItem.D_EMPT );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_GATEWAY ) ) {
      /*
       * Gateway-Adresse als String zurueckliefern
       * Rueckgabewert:
       *   HL: Zeiger auf die Netzwerkmaske mit der IP-Adresse
       */
      buf.append( "KCNET_S_GATEWAY:\n"
		+ "\tLD\tDE,8001H\n"
		+ "\tJR\tKCNET_S_GET_IPADDR\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_S_GET_IPADDR );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_NETMASK ) ) {
      /*
       * Netzwerkmaske als String zurueckliefern
       * Rueckgabewert:
       *   HL: Zeiger auf die Netzwerkmaske mit der IP-Adresse
       */
      buf.append( "KCNET_S_NETMASK:\n"
		+ "\tLD\tDE,8005H\n"
		+ "\tJR\tKCNET_S_GET_IPADDR\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_S_GET_IPADDR );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_LOCALADDR ) ) {
      /*
       * Eigene IP-Adresse als String zurueckliefern
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette mit der IP-Adresse
       */
      buf.append( "KCNET_S_LOCALADDR:\n"
		+ "\tLD\tDE,800FH\n" );
		// direkt weiter mit KCNET_S_GET_IPADDR
      libItems.add( BasicLibrary.LibItem.KCNET_S_GET_IPADDR );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_GET_IPADDR ) ) {
      /*
       * IP-Adresse lesen und als String zurueckliefern
       * Parameter:
       *   DE: Adresse im KCNet-Adressraum
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette mit der IP-Adresse
       */
      buf.append( "KCNET_S_GET_IPADDR:\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tKCNET_INIT_HW\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,D_EMPT\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,02H\n"		// Adresszeiger schreiben
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,E\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,D\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,01H\n"		// Bytes lesen
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,04H\n"
		+ "KCNET_S_GET_IPADDR1:\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tHL,KCNET_M_IPADDR\n"
		+ "\tLD\tB,04H\n"
		+ "KCNET_S_GET_IPADDR2:\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tKCNET_S_GET_IPADDR2\n"
		+ "\tLD\tHL,KCNET_M_IPADDR\n" );
		// direkt weiter mit KCNET_S_IPADDR
      libItems.add( BasicLibrary.LibItem.KCNET_S_IPADDR );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
      libItems.add( BasicLibrary.LibItem.D_EMPT );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_S_IPADDR ) ) {
      /*
       * Umwandlung einer IP-Adresse in eine Zeichenkette
       * Parameter:
       *   HL: Zeiger auf IP-Adresse (4 Bytes)
       * Rueckgabewert:
       *   HL: Zeiger auf die Zeichenkette
       */
      buf.append( "KCNET_S_IPADDR:\n"
		+ "\tLD\tDE,M_STMP\n"
		+ "\tLD\tB,04H\n"
		+ "\tJR\tKCNET_S_IPADDR2\n"
		+ "KCNET_S_IPADDR1:\n"
		+ "\tLD\tA,2EH\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "KCNET_S_IPADDR2:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,0064H\n"			// 100
		+ "\tCALL\tKCNET_S_IPADDR3\n"
		+ "\tLD\tL,0AH\n"			// 10
		+ "\tCALL\tKCNET_S_IPADDR3\n"
		+ "\tLD\tHL,0FF01H\n"			// 1
		+ "\tCALL\tKCNET_S_IPADDR3\n"
		+ "\tPOP\tHL\n"
		+ "\tDJNZ\tKCNET_S_IPADDR1\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"
		+ "\tLD\tHL,M_STMP\n"
		+ "\tRET\n"
		+ "KCNET_S_IPADDR3:\n"
		+ "\tLD\tC,0FFH\n"
		+ "KCNET_S_IPADDR4:\n"
		+ "\tINC\tC\n"
		+ "\tSUB\tL\n"
		+ "\tJR\tNC,KCNET_S_IPADDR4\n"
		+ "\tADD\tA,L\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\tH\n"
		+ "\tLD\tA,L\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tH,C\n"
		+ "\tLD\tA,C\n"
		+ "\tADD\tA,30H\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,L\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.M_STMP );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
      /*
       * Setzen des Kanalzeigerfeldes
       * Parameter:
       *   (M_IOCA): Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "KCNET_SET_IOPTRS_TCP:\n"
		+ "\tLD\tBC,000CH\n"
		+ "\tLD\tDE,(M_IOCA)\n"
		+ "\tLD\tHL,KCNET_DATA_IOPTRS\n"
		+ "\tLDIR\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(DE),A\n"		// Status Zeichenpuffer
		+ "\tINC\tDE\n"
		+ "\tLD\t(DE),A\n"		// Zeichen im Puffer
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tLD\t(DE),A\n"		// IOCTB_SOCKET_OFFS
		+ "\tINC\tDE\n"
		+ "\tLD\tA,\'T\'\n"
		+ "\tLD\t(DE),A\n"		// IOCTB_PROTO_OFFS
		+ "\tINC\tDE\n"
		+ "\tLD\tB," );
      buf.appendHex2( IOCTB_CHANNEL_SIZE - IOCTB_RX_CNT_OFFS );
      buf.append( "\n"
		+ "\tXOR\tA\n"
		+ "KCNET_SET_IOPTRS_TCP1:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDJNZ\tKCNET_SET_IOPTRS_TCP1\n"
		+ "\tRET\n" );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP )
	|| libItems.contains( BasicLibrary.LibItem.KCNET_UDP ) )
    {
      /*
       * IP-Adresse und Portnummer der Gegenstelle lesen
       * und im Kanalzeigerfeld eintragen
       * Parameter:
       *   (M_IOCA): Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "KCNET_IOCTB_FILL_REMOTE:\n"
		+ "\tLD\tDE,(M_IOCA)\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n" );
      buf.append_LD_HL_nn( IOCTB_REMOTE_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,040CH\n"	//  4 Durchlaeufe und Sn_DIPR
		+ "KCNET_IOCTB_FILL_REMOTE1:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_SOCK_GET_BYTE\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tL\n"
		+ "\tDEC\tH\n"
		+ "\tJR\tNZ,KCNET_IOCTB_FILL_REMOTE1\n"
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
		+ "\tRET\n"
      /*
       * Felder fuer Lese- und Schreiboperationen initialisieren
       * Parameter:
       *   (M_IOCA):         Anfangsadresse Kanalzeigerfeld
       *   (KCNET_M_SOCKET): Socket
       */
		+ "KCNET_IOCTB_INIT_TXRX:\n"
		+ "\tLD\tDE,(M_IOCA)\n" );
      buf.append_LD_HL_nn( IOCTB_RX_POS_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,H\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"				// DE: IOCTB_TX_WR_OFFS
		+ "\tLD\tL,24H\n"			// Sn_TX_WR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,H\n"
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n" );
      /*
       * CLOSE-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       */
      buf.append( "KCNET_CLOSE:\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n" );
      if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tCP\t17H\n"			// SOCK_ESTABLISHED
		+ "\tCALL\tZ,KCNET_FLUSH1\n" );
      }
      buf.append( "\tJP\tKCNET_SOCK_CLOSE\n"
      /*
       * EOF-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   HL: 0:  Socket offen (TCP- oder UDP-Mode)
       *       -1: Socket geschlossen
       */
		+ "KCNET_EOF:\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tNC\n"
		+ "\tDEC\tHL\n"
		+ "\tRET\n"
      /*
       * AVAILABLE-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   HL: Anzahl der empfangenen und noch nicht gelesenen Bytes
       */
		+ "KCNET_AVAILABLE:\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tCALL\tKCNET_SOCK_CHECK_STATUS\n" );
      if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP )
	  && libItems.contains( BasicLibrary.LibItem.KCNET_UDP ) )
      {
	buf.append( "\tEX\tAF,AF\'\n" );
      }
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tRET\tC\n" );
      buf.append_LD_HL_nn( IOCTB_RX_RMN_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tL,26H\n" );			// Sn_RX_RSR
      if( libItems.contains( BasicLibrary.LibItem.KCNET_UDP ) ) {
	if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	  // TCP und UDP
	  buf.append( "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tCP\t17H\n"				// SOCK_ESTABLISHED
		+ "\tRET\tZ\n"
		+ "\tLD\tBC,0008H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
	} else {
	  // nur UDP
	  buf.append( "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tBC,0008H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
	}
      } else {
	buf.append( "\tJP\tKCNET_SOCK_GET_WORD\n" );
      }
      /*
       * READ-Routine
       * Parameter:
       *   DE: Anfangsadresse Kanalzeigerfeld
       * Rueckgabewert:
       *   A: gelesenes Zeichen
       */
      buf.append( "KCNET_READ:\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tRET\tC\n" );
      // noch Bytes zu lesen?
      buf.append_LD_HL_nn( IOCTB_RX_RMN_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,KCNET_READ5\n"
      // keine Bytes mehr zu lesen -> auf neue Bytes warten
		+ "KCNET_READ1:\n" );
      if( compiler.getBasicOptions().canBreakAlways() ) {
	buf.append( "\tPUSH\tDE\n"
		+ "\tCALL\tXCKBRK\n"
		+ "\tPOP\tDE\n" );
	libItems.add( BasicLibrary.LibItem.XCKBRK );
      }
      buf.append( "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tRET\tC\n" );
      if( libItems.contains( BasicLibrary.LibItem.KCNET_UDP ) ) {
	buf.append( "\tEX\tAF,AF\'\n" );
      }
      buf.append( "\tLD\tL,26H\n"			// Sn_RX_RSR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,KCNET_READ1\n"
      // neue Bytes empfangen -> Anzahl in IOCTB_RREM_OFFS eintragen
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
      buf.append_LD_HL_nn( IOCTB_RX_RMN_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
      // Leseposition in IOCTB_RPTR_OFFS eintragen
		+ "\tPUSH\tHL\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tPOP\tBC\n" );			// BC: Anzahl Bytes
      if( libItems.contains( BasicLibrary.LibItem.KCNET_UDP ) ) {
	// bei UDP Header beachten
	buf.append( "\tEX\tAF,AF\'\n"
		+ "\tCP\t22H\n"				// SOCK_UDP
		+ "\tJR\tNZ,KCNET_READ5\n"
		+ "\tLD\tHL,0008H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tC,KCNET_READ3\n"
	// nur 8 oder weniger Bytes -> keine Nutzbytes -> Bytes ueberlesen
	// duerfte nie vorkommen
		+ "\tLD\tB,C\n"
		+ "KCNET_READ2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tKCNET_READ5\n"
		+ "\tPOP\tBC\n"
		+ "\tDJNZ\tKCNET_READ2\n"
		+ "\tJR\tKCNET_READ1\n"
	// UDP-Header lesen und auswerten
		+ "KCNET_READ3:\n" );
	buf.append_LD_HL_nn( IOCTB_REMOTE_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tB,04H\n"			// IP-Adresse lesen
		+ "KCNET_READ4:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_READ5\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tKCNET_READ4\n"
		+ "\tCALL\tKCNET_READ6\n" );		// Portnummer lesen
	buf.append_LD_HL_nn( IOCTB_RX_RMN_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tCALL\tKCNET_READ6\n" );		// Restlaenge lesen
      }
      // naechstes Byte vom W5100-Lesepuffer holen
      buf.append( "KCNET_READ5:\n" );
      buf.append_LD_HL_nn( IOCTB_RX_POS_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t07H\n"	// Maske fuer 2K Puffergroesse
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,C\n"		// HL: logischer Leseposition
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tOR\t0E0H\n"			// 8000h | 6000h
		+ "\tOR\tH\n"				// Leseadresse
		+ "\tLD\tH,A\n"
		+ "\tCALL\tKCNET_GET_BYTE\n"
		+ "\tLD\tB,A\n"
		+ "\tPOP\tHL\n"
      // logische Leseposition inkrementieren und in IOCTB_RPTR_OFFS merken
		+ "\tINC\tHL\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
      // Anzahl der Bytes verringern
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tSUB\t01H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tC,A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tSBC\tA,00H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tOR\tC\n"
		+ "\tLD\tA,B\n"
		+ "\tRET\tNZ\n"
      // letztes Byte gelesen
		+ "\tPUSH\tAF\n"
      // Leseoperation abschliessen: (Sn_RX_RD) += (Sn_RX_RSR)
		+ "\tLD\tL,26H\n"			// Sn_RX_RSR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tL,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tPOP\tBC\n"
		+ "\tADD\tHL,BC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,28H\n"			// Sn_RX_RD
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tB,C\n"
		+ "\tLD\tC,29H\n"			// Sn_RX_RD+1
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
      // Empfang freigeben
		+ "\tLD\tBC,4001H\n"			// (Sn_CR)=RECV
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
      // gelesenes Byte zurueckgeben
		+ "\tPOP\tAF\n"
		+ "\tRET\n"
      // Lesen eines Wortes und nach (HL) schreiben
		+ "KCNET_READ6:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_READ5\n"		// High(Portnummer)
		+ "\tPUSH\tAF\n"
		+ "\tCALL\tKCNET_READ5\n"		// Low(Portnummer)
		+ "\tPOP\tBC\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tRET\n"
      /*
       * WRITE-Routine
       * Parameter:
       *   (M_IOCA): Anfangsadresse Kanalzeigerfeld
       *   A:        zu schreibendes Byte
       */
		+ "KCNET_WRITE:\n"
		+ "\tLD\t(KCNET_M_BBUF),A\n"
		+ "\tLD\tDE,(M_IOCA)\n" );
      buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "KCNET_WRITE1:\n"
		+ "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tRET\tC\n" );
      if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "\tEX\tAF,AF\'\n" );	// Status merken
      }
      // noch Platz im Puffer?
      buf.append_LD_HL_nn( IOCTB_TX_FSR_OFFS );
      buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tINC\tHL\n"			// HL: IOCTB_TX_WR_OFFS
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,KCNET_WRITE4\n"
      // Groesse des Schreibpuffers pruefen
		+ "\tPUSH\tHL\n"
		+ "\tLD\tL,20H\n"		// Sn_TX_FSR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tJR\tNZ,KCNET_WRITE3\n" );
      /*
       * Schreibpuffer zu klein,
       * Bei TCP wird gewartet, bis wieder Platz ist
       * (vorheriges SEND noch nicht abgeschlossen,
       * duerft aber nicht vorkommen).
       * Bei UDP fuehrt das zu einem Fehler.
       */
      if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "\tEX\tAF,AF\'\n"
		+ "\tCP\t17H\n"			// SOCK_ESTABLISHED
		+ "\tJR\tZ,KCNET_WRITE2\n" );
      }
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_MTU_EXCEEDED,
			"MTU exceeded",
			"MTU ueberschritten" );
      buf.append( "\tRET\n" );
      if( libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "KCNET_WRITE2:\n"
		+ "\tEX\tAF,AF\'\n" );
	if( compiler.getBasicOptions().canBreakAlways() ) {
	  buf.append( "\tCALL\tXCKBRK\n" );
	  libItems.add( BasicLibrary.LibItem.XCKBRK );
	}
	buf.append( "\tJR\tKCNET_WRITE1\n" );
      }
      buf.append( "KCNET_WRITE3:\n"
      // Groesse Schreibpuffer nach IOCTB_TX_FSR_OFFS schreiben
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tHL\n"			// HL: IOCTB_TX_WR_OFFS
      // logische Schreibposition holen
		+ "\tPUSH\tHL\n"
		+ "\tLD\tL,24H\n"		// Sn_TX_WR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n"
		+ "\tDEC\tHL\n"
		+ "KCNET_WRITE4:\n"		// HL: IOCTB_TX_WR_OFFS
      // Schreibadresse berechnen
		+ "\tPUSH\tHL\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t07H\n"		// Maske fuer 2K Puffergroesse
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,C\n"
		+ "\tPUSH\tHL\n"		// logische Schreibposition
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tOR\t0C0H\n"		// 8000h | 4000h
		+ "\tOR\tH\n"
		+ "\tLD\tH,A\n"			// HL: Schreibadresse
      // Byte in den W5100-Adressraum schreiben
		+ "\tLD\tA,(KCNET_M_BBUF)\n"
		+ "\tLD\tB,A\n"
		+ "\tCALL\tKCNET_SET_BYTE\n"
      // logische Schreibposition inkrementieren
		+ "\tPOP\tBC\n"
		+ "\tINC\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t07H\n"
      // neue Schreibposition zurueckschreiben
		+ "\tPOP\tHL\n"			// HL: IOCTB_TX_WR_OFFS
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),A\n"
      // Anzahl freie Bytes dekrementieren
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tDEC\tHL\n"			// HL: IOCTB_FSR_OFFS
		+ "\tLD\tC,(HL)\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\t(HL),C\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),B\n" );
      if( !libItems.contains( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "\tRET\n" );
      } else {
	// Puffer voll?
	buf.append( "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tRET\tNZ\n"
	// Puffer voll, bei TCP Senden
		+ "\tEX\tAF,AF\'\n"
		+ "\tCP\t17H\n"			// SOCK_ESTABLISHED
		+ "\tRET\tNZ\n"
		+ "\tJR\tKCNET_FLUSH2\n"
	/*
	 * FLUSH-Routine
	 * Parameter:
	 *   DE: Anfangsadresse Kanalzeigerfeld
	 */
		+ "KCNET_FLUSH:\n" );
	buf.append_LD_HL_nn( IOCTB_SOCKET_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(KCNET_M_SOCKET),A\n"
		+ "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tRET\tC\n"
		+ "\tCP\t17H\n"			// SOCK_ESTABLISHED
		+ "\tRET\tNZ\n" );
	libItems.add( BasicLibrary.LibItem.KCNET_FLUSH1 );
      }
      if( libItems.contains( BasicLibrary.LibItem.KCNET_FLUSH1 ) ) {
	// Pruefen, ob ueberhaupt Bytes zu senden sind
	buf.append( "KCNET_FLUSH1:\n"
		+ "\tLD\tL,24H\n"		// Sn_TX_WR
		+ "\tCALL\tKCNET_SOCK_GET_WORD\n"
		+ "\tPUSH\tHL\n" );
	buf.append_LD_HL_nn( IOCTB_TX_WR_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tPOP\tHL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\tZ\n"
	// eigentliches Senden, HL: IOCTB_TX_WR_OFFS
		+ "KCNET_FLUSH2:\n" );
	buf.append_LD_HL_nn( IOCTB_TX_WR_OFFS );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tB,(HL)\n" );
		// direkt weiter mit KCNET_SOCK_SEND
      }
      libItems.add( BasicLibrary.LibItem.KCNET_SOCK );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_SOCK ) ) {
      /*
       * Senden der Bytes im Socket
       * Parameter:
       *   BC: TX_WR
       */
      buf.append( "KCNET_SOCK_SEND:\n"
		+ "\tLD\tL,24H\n"			// Sn_TX_WR
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tLD\tBC,2001H\n"			// (Sn_CR)=SEND
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
      // auf Ergebnis warten
		+ "KCNET_SOCK_SEND1:\n"
		+ "\tLD\tL,02H\n"			// Sn_IR
		+ "\tCALL\tKCNET_SOCK_GET_BYTE\n"
		+ "\tLD\tB,A\n"
		+ "\tAND\t18H\n"			// SEND+TIMEOUT
		+ "\tJR\tNZ,KCNET_SOCK_SEND2\n" );
      if( compiler.getBasicOptions().canBreakAlways() ) {
	buf.append( "\tPUSH\tDE\n"
		+ "\tCALL\tXCKBRK\n"
		+ "\tPOP\tDE\n" );
	libItems.add( BasicLibrary.LibItem.XCKBRK );
      }
      buf.append( "\tCALL\tKCNET_SOCK_CHECK_STATUS\n"
		+ "\tRET\tC\n"
		+ "\tJR\tKCNET_SOCK_SEND1\n"
		+ "KCNET_SOCK_SEND2:\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tLD\tC,02H\n"			// Sn_IR
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"	// zuruecksetzen
		+ "\tEX\tAF,AF\'\n"
		+ "\tAND\t08H\n"			// SEND/TIMEOUT
		+ "\tRET\tZ\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_TIMEOUT,
			"Senden fehlgeschlagen",
			"Send failed" );
      buf.append( "\tRET\n"
      /*
       * Holen der Portnummer und Oeffnen eines Sockets
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   E:                Betriebsart (1: TCP, 2: UDP)
       */
		+ "KCNET_SOCK_GET_PORT_AND_OPEN:\n"
		+ "\tLD\tA,08H\n"			// Portnummer holen
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tH,A\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tL,A\n"
      /*
       * Oeffnen eines Sockets
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   E:                Betriebsart (1: TCP, 2: UDP)
       *   HL:               Portnummer
       */
		+ "KCNET_SOCK_OPEN:\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_SOCK_CLOSE\n"
		+ "KCNET_SOCK_OPEN1:\n"
		+ "\tLD\tB,E\n"
		+ "\tLD\tC,00\n"			// Sn_MR
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tPOP\tBC\n"
		+ "\tPUSH\tBC\n"
		+ "\tLD\tL,04H\n"			// Sn_PORT
		+ "\tCALL\tKCNET_SOCK_SET_WORD\n"
		+ "\tLD\tBC,0101H\n"                    // Sn_CR=OPEN
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "KCNET_SOCK_OPEN2:\n"
		+ "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tOR\tA\n"				// SOCK_CLOSED?
		+ "\tJR\tZ,KCNET_SOCK_OPEN2\n"
		+ "\tCP\t13H\n"				// SOCK_INIT?
		+ "\tJR\tZ,KCNET_SOCK_OPEN3\n"
		+ "\tCP\t22H\n"				// SOCK_UDP?
		+ "\tJR\tZ,KCNET_SOCK_OPEN3\n"
		+ "\tLD\tBC,1001H\n"			// Sn_CR=CLOSE
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tJR\tKCNET_SOCK_OPEN1\n"
		+ "KCNET_SOCK_OPEN3:\n"
		+ "\tPOP\tHL\n"
		+ "\tRET\n"
      /*
       * Socket schliessen
       * Parameter:
       *   (M_SOCKET): Socketnummer
       */
		+ "KCNET_SOCK_CLOSE:\n"
		+ "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tOR\tA\n"				// SOCK_CLOSED?
		+ "\tRET\tZ\n"
		+ "\tLD\tBC,0801H\n"			// (Sn_CR)=DISCON
		+ "\tCP\t17H\n"				// SOCK_ESTABLISHED?
		+ "\tJP\tZ,KCNET_SOCK_CLOSE1\n"
		+ "\tLD\tB,10H\n"			// (Sn_CR)=CLOSE
		+ "KCNET_SOCK_CLOSE1:\n"
		+ "\tCALL\tKCNET_SOCK_SET_BYTE\n"
		+ "\tLD\tL,02H\n"			// Sn_IR
		+ "\tCALL\tKCNET_SOCK_GET_BYTE\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,02H\n"			// Sn_IR
		+ "\tJR\tKCNET_SOCK_SET_BYTE\n"		// zuruecksetzen
      /*
       * Lesen eines 16-Bit-Wertes vom Socket
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   L:                Offset
       * Rueckgabewert:
       *   HL: gelesenes Wort
       */
		+ "KCNET_SOCK_GET_WORD:\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tADD\tA,84H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,01H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tH,A\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tL,A\n"
		+ "\tRET\n"
      /*
       * Pruefen, ob der Socket-Status dem eines offenen IO-Kanals entspricht
       * (SOCK_ESTABLISHED oder SOCK_UPD). Wenn ja, wird CY=0 zurueckgegeben.
       * Bei Status CLOSED oder wenn der Socket gerade geschlossen wird
       * (Verbindungsabbau durch die Gegenstelle),
       * wird die Fehlervariable auf E_EOF gesetzt mit CY=1 zurueckgekehrt.
       * Bei allen anderen Stati, die eigentich nicht auftreten duerften,
       * wird die Fehlervariable auf E_SOCKET_STATUS gesetzt und
       * mit CY=1 zurueckgekehrt.
       *
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       * Rueckgabewert:
       *   CY=0: OK, Socketstatus in A
       *   CY=1: falscher Status -> Aktion abbrechen
       */
		+ "KCNET_SOCK_CHECK_STATUS:\n"
		+ "\tCALL\tKCNET_SOCK_GET_STATUS\n"
		+ "\tOR\tA\n"				// SOCK_CLOSED?
		+ "\tJR\tZ,KCNET_SOCK_CHECK_STATUS2\n"
		+ "\tCP\t17H\n"				// SOCK_ESTABLISHED?
		+ "\tRET\tZ\n"
		+ "\tCP\t22H\n"				// SOCK_UDP?
		+ "\tRET\tZ\n"
      /*
       * Stati 0x18 bis 0x1D signalisieren den Verbindungsabbau.
       *   0x18: SOCK_FIN_WAIT_1
       *   0x19: SOCK_FIN_WAIT_2
       *   0x1A: SOCK_CLOSING
       *   0x1B: SOCK_TIME_WAIT
       *   0x1C: SOCK_CLOSE_WAIT
       *   0x1D: SOCK_LAST_ACK
       */
		+ "\tCP\t18H\n"
		+ "\tJR\tC,KCNET_SOCK_CHECK_STATUS1\n"
		+ "\tCP\t1EH\n"
		+ "\tJR\tC,KCNET_SOCK_CHECK_STATUS2\n"
		+ "KCNET_SOCK_CHECK_STATUS1:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_SOCKET_STATUS,
			"Ungueltiger Socket-Status",
			"Invalid socket status" );
      buf.append( "\tJR\tKCNET_SOCK_CHECK_STATUS3\n"
		+ "KCNET_SOCK_CHECK_STATUS2:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_EOF,
			"Socket geschlossen",
			"Socket closed" );
      buf.append( "KCNET_SOCK_CHECK_STATUS3:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
      /*
       * Lesen Socket-Status
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       * Rueckgabewert:
       *   A: gelesenes Byte
       */
		+ "KCNET_SOCK_GET_STATUS:\n"
		+ "\tLD\tL,03H\n"
		// direkt weiter mit KCNET_SOCK_GET_BYTE
      /*
       * Lesen eines Bytes vom Socket
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   L:                Offset
       * Rueckgabewert:
       *   A: gelesenes Byte
       */
		+ "KCNET_SOCK_GET_BYTE:\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tADD\tA,84H\n"
		+ "\tLD\tH,A\n"
		+ "\tJP\tKCNET_GET_BYTE\n"
      /*
       * Setzen eines Wortes im Socket
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   BC:               zu setzendes Wort
       *   L:                Offset
       */
		+ "KCNET_SOCK_SET_WORD:\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tADD\tA,84H\n"
		+ "\tLD\tH,A\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tKCNET_SET_BYTE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tBC\n"
		+ "\tINC\tL\n"
		+ "\tLD\tB,C\n"
		+ "\tJP\tKCNET_SET_BYTE\n"
      /*
       * Setzen eines Bytes im Socket
       * Parameter:
       *   (KCNET_M_SOCKET): Socket-Nummer
       *   B:                zu setzendes Byte
       *   C:                Offset
       */
		+ "KCNET_SOCK_SET_BYTE:\n"
		+ "\tLD\tA,(KCNET_M_SOCKET)\n"
		+ "\tADD\tA,84H\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tL,C\n"
		+ "\tJP\tKCNET_SET_BYTE\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_BASE ) ) {
      /*
       * KCNet initialisieren
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler, Fehlervariablen gesetzt
       */
      buf.append( "KCNET_INIT:\n"
		+ "\tCALL\tKCNET_INIT_HW\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,(KCNET_M_INIT)\n"
		+ "\tAND\t02H\n"
		+ "\tRET\tNZ\n"
      // Subnetzmaske pruefen
		+ "\tLD\tHL,8005H\n"
		+ "\tCALL\tKCNET_CHECK_IPADDR\n"
		+ "\tRET\tC\n"
      // IP-Adresse pruefen
		+ "\tLD\tHL,800FH\n"
		+ "\tCALL\tKCNET_CHECK_IPADDR\n"
		+ "\tRET\tC\n"
      // W5100 initialisieren
		+ "\tLD\tHL,KCNET_DATA_MODULE_INIT\n"
		+ "\tLD\tB,10H\n"
		+ "KCNET_INIT1:\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tDJNZ\tKCNET_INIT1\n"
      // Initialisierung abgeschlossen
		+ "\tLD\tA,03H\n"			// HW+CFG
		+ "\tLD\t(KCNET_M_INIT),A\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
      /*
       * Vorhandensein einer IP-Adresse pruefen,
       * Parameter:
       *   HL: Adresse im KCNet-Modul
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: IP-Adresse nicht vorhanden
       */
		+ "KCNET_CHECK_IPADDR:\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,01H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,04H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tXOR\tA\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,03H\n"
		+ "KCNET_CHECK_IPADDR1:\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tDJNZ\tKCNET_CHECK_IPADDR1\n"
		+ "\tRET\tNZ\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_DEVICE_NOT_CONFIGURED,
			"KCNet nicht konfiguriert",
			"KCNet not configured" );
      buf.append( "\tSCF\n"
		+ "\tRET\n" );
      libItems.add( BasicLibrary.LibItem.KCNET_BASE_HW );
    }
    if( libItems.contains( BasicLibrary.LibItem.KCNET_BASE_HW ) ) {
      /*
       * KCNet-Hardware initialisieren
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Fehler, Fehlervariablen gesetzt
       */
      AbstractTarget target     = compiler.getTarget();
      int            baseIOAddr = target.getKCNetBaseIOAddr();
      buf.append( "KCNET_INIT_HW:\n"
		+ "\tLD\tA,(KCNET_M_INIT)\n"
		+ "\tAND\t01H\n"
		+ "\tRET\tNZ\n" );
      if( target.needsEnableKCNet() ) {
	target.appendEnableKCNet( buf );
	buf.append( "\tJR\tC,KCNET_INIT_ERR\n" );
      }
      buf.append( "\tLD\tHL,KCNET_DATA_PIO_INIT\n" );
      buf.append_LD_BC_nn( (2 << 8) | (baseIOAddr + 2) );
      buf.append( "\tOTIR\n" );
      buf.append_LD_BC_nn( (4 << 8) | (baseIOAddr + 3) );
      buf.append( "\tOTIR\n"
		+ "\tIN\tA,(" );
      buf.appendHex2( baseIOAddr );
      buf.append( ")\n"
		+ "\tIN\tA,(" );
      buf.appendHex2( baseIOAddr + 1 );
      buf.append( ")\n"
		+ "\tRLCA\n"
		+ "\tJR\tC,KCNET_INIT_ERR\n"
      // Hardware-Version pruefen
		+ "\tLD\tA,0AH\n"
		+ "\tCALL\tKCNET_CHECK_VERSION\n"
		+ "\tRET\tC\n"
      // Software-Version pruefen
		+ "\tLD\tA,09H\n"
		+ "\tCALL\tKCNET_CHECK_VERSION\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,01H\n"
		+ "\tLD\t(KCNET_M_INIT),A\n"
		+ "\tRET\n"
      /*
       * KCNet-Version pruefen
       * Parameter:
       *   A: KCNet-Kommando (9: Software-Version, 10: Hardware-Version)
       * Rueckgabewert:
       *   CY=0: OK
       *   CY=1: Version zu alt
       */
		+ "KCNET_CHECK_VERSION:\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tL,A\n"
		+ "\tCALL\tKCNET_RD_BYTE\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tBC,0102H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\tNC\n"
      // Fehler
		+ "KCNET_INIT_ERR:\n" );
      BasicLibrary.appendSetError(
			compiler,
			BasicLibrary.E_DEVICE_NOT_FOUND,
			"KCNet nicht gefunden",
			"KCNet not found" );
      buf.append( "\tSCF\n"
		+ "\tRET\n"
      /*
       * Setzen eines Bytes im KCNet-Adressraum
       * Parameter:
       *   HL: Adresse
       *   B:  zu setzendes Byte
       */
		+ "KCNET_SET_BYTE:\n"
		+ "\tLD\tA,04H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,B\n"
		// direkt weiter mit KCNET_WR_BYTE
      /*
       * Schreiben eines Bytes in das KCNet-Modul:
       * Parameter:
       *   A: zu schreibendes Byte
       */
		+ "KCNET_WR_BYTE:\n"
		+ "\tOUT\t(" );
      buf.appendHex2( baseIOAddr );
      buf.append( "),A\n"
		+ "KCNET_WR_BYTE1:\n"
		+ "\tIN\tA,(" );
      buf.appendHex2( baseIOAddr + 1 );
      buf.append( ")\n"
		+ "\tRRCA\n"
		+ "\tJR\tC,KCNET_WR_BYTE1\n"
		+ "\tRET\n"
      /*
       * Lesen eines Bytes vom KCNet-Adressraum
       * Parameter:
       *   HL: Adresse
       * Rueckgabewert:
       *   A: gelesenes Byte
       */
		+ "KCNET_GET_BYTE:\n"
		+ "\tLD\tA,05H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,L\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		+ "\tLD\tA,H\n"
		+ "\tCALL\tKCNET_WR_BYTE\n"
		// direkt weiter mit KCNET_RD_BYTE
      /*
       * Lesen eines Bytes vom KCNet-Modul:
       * Rueckgabewert:
       *   A: gelesenes Byte
       */
		+ "KCNET_RD_BYTE:\n"
		+ "\tIN\tA,(" );
      buf.appendHex2( baseIOAddr + 1 );
      buf.append( ")\n"
		+ "\tRLCA\n"
		+ "\tJR\tNC,KCNET_RD_BYTE\n"
		+ "\tIN\tA,(" );
      buf.appendHex2( baseIOAddr );
      buf.append( ")\n"
		+ "\tRET\n" );
    }
  }


  public static void appendDataTo( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_HOSTBYNAME ) ) {
      buf.append( "KCNET_DATA_DNSQ:\n"
		+ "\tDB\t01H,00H,00H,01H,00H,00H,00H,00H,00H,00H\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_TCP )
	|| compiler.usesLibItem( BasicLibrary.LibItem.KCNET_UDP ) )
    {
      buf.append( "KCNET_DATA_IOPTRS:\n"
		+ "\tDW\tKCNET_CLOSE\n"
		+ "\tDW\tKCNET_EOF\n"
		+ "\tDW\tKCNET_AVAILABLE\n"
		+ "\tDW\tKCNET_READ\n"
		+ "\tDW\tKCNET_WRITE\n" );
      if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_TCP ) ) {
	buf.append( "\tDW\tKCNET_FLUSH\n" );
      }
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_BASE ) ) {
      buf.append( "KCNET_DATA_MODULE_INIT:\n"
		+ "\tDB\t04H,00H,80H,00H\n"		// MR=0
		+ "\tDB\t04H,16H,80H,00H\n"		// IMR=0
		+ "\tDB\t04H,1AH,80H,55H\n"		// RMSR=55h
		+ "\tDB\t04H,1BH,80H,55H\n" );		// TMSR=55h
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_BASE_HW ) ) {
      buf.append( "KCNET_DATA_PIO_INIT:\n"
		+ "\tDB\t8FH,07H,0CFH,0FFH,17H,0FFH\n" );
    }
  }


  public static void appendBssTo( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_HOSTBYNAME ) ) {
      compiler.getCodeBuf().append( "KCNET_M_DNS_ID:\n"
				+ "\tDS\t2\n"
				+ "KCNET_M_DNS_QDLEN:\n"
				+ "\tDS\t2\n"
				+ "KCNET_M_DNS_TX_WR:\n"
				+ "\tDS\t2\n"
				+ "KCNET_M_DNS_RX_RMN:\n"
				+ "\tDS\t2\n"
				+ "KCNET_M_DNS_RX_RD:\n"
				+ "\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_PARSE_IPADDR ) ) {
      compiler.getCodeBuf().append( "KCNET_M_HOST:\n"
				+ "\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_TCP )
	|| compiler.usesLibItem( BasicLibrary.LibItem.KCNET_UDP ) )
    {
      compiler.getCodeBuf().append( "KCNET_M_BBUF:\n"
				+ "\tDS\t1\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_SOCK ) ) {
      compiler.getCodeBuf().append( "KCNET_M_SOCKET:\n"
				+ "\tDS\t1\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_BASE_HW ) ) {
      compiler.getCodeBuf().append( "KCNET_M_INIT:\n"
				+ "\tDS\t1\n"	// Bit 0: HW, Bit 1: CFG
				+ "KCNET_M_IPADDR:\n"
				+ "\tDS\t4\n" );
    }
  }


  public static void appendInitTo( BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.KCNET_BASE_HW ) ) {
      compiler.getCodeBuf().append( "\tXOR\tA\n"
				+ "\tLD\t(KCNET_M_INIT),A\n" );
    }
  }
}

