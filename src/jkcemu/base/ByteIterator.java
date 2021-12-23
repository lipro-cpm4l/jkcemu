/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Iterator ueber ein Byte-Array
 *
 * Die next-Methoden werfen eine NoSuchElementException,
 * wenn der Index sich ausserhalb des gueltigen Bereichs liegt.
 * Die read-Methoden liefern in dem Fall -1.
 */

package jkcemu.base;

import java.util.NoSuchElementException;


public class ByteIterator
{
  private byte[] buf;
  private int    offs;
  private int    endIdx;
  private int    curIdx;


  public ByteIterator(
		byte[] buf,
		int    offs,
		int    len )
  {
    this.buf    = buf;
    this.offs   = offs;
    this.curIdx = offs;
    this.endIdx = offs + len;
    if( this.endIdx >= buf.length ) {
      this.endIdx = buf.length;
    }
  }


  public ByteIterator( byte[] buf )
  {
    this( buf, 0, buf.length );
  }


  public int available()
  {
    return hasNext() ? (this.endIdx - this.curIdx) : 0;
  }


  public int getIndex()
  {
    return this.curIdx;
  }


  public boolean hasNext()
  {
    return (this.curIdx >= this.offs) && (this.curIdx < this.endIdx);
  }


  public byte next() throws NoSuchElementException
  {
    if( !hasNext() ) {
      throw new NoSuchElementException();
    }
    return this.buf[ this.curIdx++ ];
  }


  public int nextByte() throws NoSuchElementException
  {
    return (int) next() & 0xFF;
  }


  public int nextInt3LE() throws NoSuchElementException
  {
    int l  = nextWord();
    int b2 = nextByte();
    return (b2 << 16) | l;
  }


  public int nextInt4LE() throws NoSuchElementException
  {
    int l = nextWord();
    int h = nextWord();
    return (h << 16) | l;
  }


  public int nextWord() throws NoSuchElementException
  {
    int b0 = nextByte();
    int b1 = nextByte();
    return (b1 << 8) | b0;
  }


  public int readByte()
  {
    return (this.curIdx >= this.offs) && (this.curIdx < this.endIdx) ?
				(int) this.buf[ this.curIdx++ ] & 0xFF
				: -1;
  }


  public int readWord()
  {
    int b0 = readByte();
    int b1 = readByte();
    return (b0 >= 0) && (b1 >= 0) ?
		(((b1 << 8) & 0xFF00) | (b0 & 0x00FF))
		: -1;
  }


  public void setIndex( int idx )
  {
    this.curIdx = idx;
  }


  public void skip( int n )
  {
    if( n > 0 ) {
      long newPos = (long) this.curIdx + (long) n;
      if( newPos > Integer.MAX_VALUE ) {
	newPos = Integer.MAX_VALUE;
      }
      this.curIdx = (int) newPos;
    }
  }
}
