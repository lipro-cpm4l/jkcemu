/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiformate fuer Speicherabbilddateien
 */

package jkcemu.base;

import java.lang.*;


public class FileFormat
{
  public static FileFormat BIN
	= new FileFormat( "BIN-Datei (Speicherabbild ohne Kopfdaten)", 0 );

  public static FileFormat BASIC_PRG
	= new FileFormat( "BASIC-Programmdatei", 0 );

  public static FileFormat CDT
	= new FileFormat( "CPC-Tape-Datei", 0 );

  public static FileFormat CSW
	= new FileFormat( "CSW-Datei", 0 );

  public static FileFormat HEADERSAVE
	= new FileFormat( "Headersave-Datei", 16 );

  public static FileFormat INTELHEX
	= new FileFormat( "Intel-HEX-Datei", 0 );

  public static FileFormat KCB
	= new FileFormat( "KCB-Datei (BASIC-Programm)", 11 );

  public static FileFormat KCC
	= new FileFormat( "KCC/JTC-Datei", 11 );

  public static FileFormat KCTAP_SYS
	= new FileFormat( "KC-TAP-Datei", 11 );

  public static FileFormat KCTAP_KC85
	= new FileFormat( "KC-TAP-Datei (KC85)", 11 );

  public static FileFormat KCTAP_Z9001
	= new FileFormat( "KC-TAP-Datei (Z9001)", 11 );

  public static FileFormat KCTAP_BASIC_PRG
	= new FileFormat( "KC-TAP-BASIC-Programmdatei", 11 );

  public static FileFormat KCTAP_BASIC_DATA
	= new FileFormat( "KC-TAP-BASIC-Datenfeld", 8 );

  public static FileFormat KCTAP_BASIC_ASC
	= new FileFormat( "KC-TAP-BASIC-ASCII-Listing", 8 );

  public static FileFormat KCBASIC_HEAD_PRG
	= new FileFormat( "KC-BASIC-Programmdatei mit Kopfdaten", 8 );

  public static FileFormat KCBASIC_HEAD_DATA
	= new FileFormat( "KC-BASIC-Datenfeld mit Kopfdaten", 8 );

  public static FileFormat KCBASIC_HEAD_ASC
	= new FileFormat( "KC-BASIC-ASCII-Listing mit Kopfdaten", 8 );

  public static FileFormat KCBASIC_PRG
	= new FileFormat( "KC-BASIC-Programmdatei", 0 );

  public static FileFormat RBASIC_PRG
	= new FileFormat( "RBASIC-Programmdatei", 0 );

  public static FileFormat RMC
	= new FileFormat( "RBASIC-Maschinencodedatei", 0 );

  public static FileFormat TZX
	= new FileFormat( "ZX-Spectrum-Tape-Datei", 0 );

  public static FileFormat ZXTAP
	= new FileFormat( "ZX-TAP-Datei", 0 );


  private String text;
  private int    maxFileDescLen;


  public int getMaxFileDescLength()
  {
    return this.maxFileDescLen;
  }


  public static int getTotalMaxFileDescLength()
  {
    return 16;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.text;
  }


	/* --- Konstruktor --- */

  private FileFormat( String text, int maxFileDescLen )
  {
    this.text           = text;
    this.maxFileDescLen = maxFileDescLen;
  }
}
