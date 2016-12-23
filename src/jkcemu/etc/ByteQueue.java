/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Queue mit einzelnen Bytes als Elemente
 */

package jkcemu.etc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.*;


public class ByteQueue
{
  private byte[]  buf;
  private Object  modifyLock;
  private Object  readLock;
  private Object  writeLock;
  private boolean empty;
  private int     front;
  private int     rear;


  public ByteQueue( int initialCapacity )
  {
    this.buf        = new byte[ initialCapacity > 0 ? initialCapacity : 1 ];
    this.modifyLock = new Object();
    this.readLock   = new Object();
    this.writeLock  = new Object();
    clear();
  }


  /*
   * Die Methode haengt ein Byte an das Ende der Queue an.
   * Ist der interne Puffer zu klein, wird er vergroessert.
   */
  public void add( byte b )
  {
    synchronized( this.readLock ) {
      synchronized( this.modifyLock ) {
	if( !this.empty && (this.front == this.rear) ) {
	  // Queue ist voll -> Puffer vergroessern
	  byte[] a = new byte[ this.buf.length + 1024 ];

	  // vom Anfang der Queue bis zum Pufferende kopieren,
	  // Anzahl der kopierenten Bytes als neue Endposition vormerken
	  this.rear = this.buf.length - this.front;
	  System.arraycopy( this.buf, this.front, a, 0, this.rear );

	  // vom Pufferanfang bis zum Zeiger kopieren,
	  // neue Endposition um Anzahl der kopierten Bytes erhoehen
	  if( this.front > 0 ) {
	    System.arraycopy( this.buf, 0, a, this.rear, this.front );
	    this.rear += this.front;
	  }
	  this.buf   = a;
	  this.front = 0;
	}
	this.buf[ this.rear++ ] = b;
	if( this.rear >= this.buf.length ) {
	  this.rear = 0;
	}
	this.empty = false;
      }
      try {
	this.readLock.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  public void clear()
  {
    synchronized( this.writeLock ) {
      synchronized( this.modifyLock ) {
	this.empty = true;
	this.front = 0;
	this.rear  = 0;
      }
      try {
	this.writeLock.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  public boolean isEmpty()
  {
    boolean rv = false;
    synchronized( this.modifyLock ) {
      rv =this.empty;
    }
    return rv;
  }


  /*
   * Die Methode liefert das erste Byte vom Anfang der Queue.
   * Ist die Queue leer, wird -1 zurueckgeliefert.
   */
  public int poll()
  {
    int rv = -1;
    synchronized( this.writeLock ) {
      synchronized( this.modifyLock ) {
	if( !this.empty ) {
	  rv = (int) this.buf[ this.front++ ] & 0xFF;
	  if( this.front >= this.buf.length ) {
	    this.front = 0;
	  }
	  if( this.front == this.rear ) {
	    this.empty = true;
	  }
	}
      }
      try {
	this.writeLock.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
    return rv;
  }


  /*
   * Die Methode liefert das erste Byte vom Anfang der Queue.
   * Ist die Queue leer, wird gewartet, bis ein Byte verfuegbar ist.
   */
  public byte read() throws IOException
  {
    int b = 0;
    synchronized( this.readLock ) {
      b = poll();
      while( b < 0 ) {
	try {
	  this.readLock.wait();
	}
	catch( IllegalMonitorStateException ex ) {}
	catch( InterruptedException ex ) {
	  throw new InterruptedIOException();
	}
	b = poll();
      }
    }
    return (byte) (b & 0xFF);
  }


  /*
   * Die Methode haengt ein Byte an das Ende der Queue an.
   * Ist der interne Puffer voll, wird gewartet,
   * bis ein Byte von der Queue entnommen wird.
   */
  public void write( byte b ) throws IOException
  {
    synchronized( this.writeLock ) {
      if( !this.empty && (this.front == this.rear) ) {
	try {
	  this.writeLock.wait();
	}
	catch( IllegalMonitorStateException ex ) {}
	catch( InterruptedException ex ) {
	  throw new InterruptedIOException();
	}
      }
    }
    add( b );
  }
}
