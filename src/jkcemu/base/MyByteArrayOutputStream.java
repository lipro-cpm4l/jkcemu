/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterung von ByteArrayOutputStream
 *
 * Die Klasse ermoeglicht es, die gespeicherten Daten wieder zu lesen,
 * ohne dass die Daten wie bei der Methode toByteArray() dupliziert werden.
 * Des Weiteren kann der interene Puffer freigegeben werden.
 */

package jkcemu.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.*;


public class MyByteArrayOutputStream extends ByteArrayOutputStream
{
  public MyByteArrayOutputStream( int size )
  {
    super( size );
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
