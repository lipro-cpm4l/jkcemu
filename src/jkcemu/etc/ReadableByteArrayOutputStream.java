/*
 * (c) 2019-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterung von ByteArrayOutputStream
 *
 * Die Klasse ermoeglicht es, die gespeicherten Daten wieder zu lesen,
 * ohne dass die Daten wie bei der Methode toByteArray() dupliziert werden.
 * Des Weiteren kann der interene Puffer freigegeben werden.
 */

package jkcemu.etc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class ReadableByteArrayOutputStream extends ByteArrayOutputStream
{
  public ReadableByteArrayOutputStream( int size )
  {
    super( size );
  }


  /*
   * Die Methode liefert ein Bytes vom Ende aus gesehen, d.h.,
   * idx=0 liefert das letzte und idx=-1 das vorletzte Byte usw.
   *
   * Rueckgabe:
   *   Byte oder -1 bei Zugriff auserhalb des gueltigen Bereichs
   */
  public synchronized int getFromEnd( int pos )
  {
    int rv  = -1;
    int idx = this.count - 1 - pos;
    if( (idx >= 0) && (idx < this.buf.length) ) {
      rv = (int) this.buf[ idx ] & 0xFF;
    }
    return rv;
  }


  public InputStream newInputStream()
  {
    return new ByteArrayInputStream( this.buf, 0, this.count );
  }


  public synchronized void resetAndFreeMem()
  {
    reset();

    // Puffer freigeben durch zuweisen eines neuen sehr kleinen Puffers
    this.buf = new byte[ 4 ];
  }
}
