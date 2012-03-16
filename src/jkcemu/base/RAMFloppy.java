/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer RAM-Floppy
 */

package jkcemu.base;

import java.awt.EventQueue;
import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.text.TextUtil;


public class RAMFloppy
{
  public enum RFType { ADW, MP_3_1988, OTHER };

  private RFType           rfType;
  private int              size;
  private int              addr;
  private int              endOfData;
  private int              prevEndOfData;
  private boolean          prevDataChanged;
  private boolean          readOnly;
  private volatile boolean dataChanged;
  private byte[]           dataBuf;
  private String           infoText;
  private String           sysName;
  private File             file;
  private RAMFloppyFld     ramFloppyFld;


  public RAMFloppy()
  {
    initRF();
    this.dataBuf      = null;
    this.ramFloppyFld = null;
  }


  public void clear()
  {
    this.endOfData   = 0;
    this.dataChanged = false;
    if( this.dataBuf != null ) {
      Arrays.fill( this.dataBuf, (byte) 0xE5 );
    }
    fireRAMFloppyChanged();
  }


  public static boolean complies(
				RAMFloppy        rf,
				String           sysName,
				RAMFloppy.RFType rfType,
				Properties       props,
				String           propPrefix )
  {
    boolean rv    = false;
    boolean state = false;
    int size      = 0;
    if( rfType == RFType.MP_3_1988 ) {
      size  = 256 * 1024;
      state = EmuUtil.getBooleanProperty(
				props,
				propPrefix + "enabled",
				false );
    } else {
      size = getRAMFloppySize( props, propPrefix );
      if( size > 0 ) {
	state = true;
      }
    }
    if( rf != null ) {
      if( state ) {
	rv = TextUtil.equals( sysName, rf.sysName )
		&& (rfType == rf.rfType)
		&& (size == rf.size);
      }
    } else {
      if( !state ) {
	rv = true;
      }
    }
    return rv;
  }


  public void deinstall()
  {
    initRF();
    fireRAMFloppyChanged();
  }


  public int getByte( int idx )
  {
    int rv = 0xFF;
    if( (idx >= 0) && (idx < this.size) ) {
      rv = 0;
      if( this.dataBuf != null ) {
	if( idx < this.dataBuf.length ) {
	  rv = (int) this.dataBuf[ idx ] & 0xFF;
	}
      }
      fireRAMFloppyAccess();
    }
    return rv;
  }


  public File getFile()
  {
    return this.file;
  }


  public String getInfoText()
  {
    return this.infoText != null ?
		this.infoText
		: "RAM-Floppy nicht emuliert";
  }


  public int getSize()
  {
    return this.size;
  }


  public int getUsedSize()
  {
    return this.endOfData;
  }


  public boolean hasDataChanged()
  {
    return this.dataChanged;
  }


  public void install(
		String     sysName,
		RFType     rfType,
		int        size,
		String     infoText,
		String     fileName )
  {
    if( !TextUtil.equals( sysName, this.sysName )
	|| (rfType != this.rfType)
	|| (size != this.size)
	|| !TextUtil.equals( infoText, this.infoText ) )
    {
      this.rfType      = rfType;
      this.size        = size;
      this.addr        = 0;
      this.endOfData   = 0;
      this.dataChanged = false;
      this.readOnly    = false;
      this.infoText    = infoText;
      this.sysName     = sysName;
      this.file        = null;
      if( this.dataBuf != null ) {
	Arrays.fill( this.dataBuf, (byte) 0 );
      }
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  boolean state = true;
	  if( this.file != null ) {
	    if( TextUtil.equals( fileName, this.file.getPath() ) ) {
	      state = false;
	    }
	  }
	  if( state ) {
	    try {
	      load( new File( fileName ) );
	    }
	    catch( IOException ex ) {
	      EmuUtil.fireShowError(
			Main.getScreenFrm(),
			infoText + " konnte nicht geladen werden.",
			ex );
	    }
	  }
	}
      }
      fireRAMFloppyChanged();
    }
  }


  public void load( File file ) throws IOException
  {
    ensureBufferSize();
    if( this.dataBuf != null ) {
      InputStream in = null;
      try {
	in = new FileInputStream( file );

	this.endOfData   = EmuUtil.read( in, this.dataBuf );
	this.file        = file;
	this.dataChanged = false;
	for( int i = this.endOfData; i < this.dataBuf.length; i++ ) {
	  this.dataBuf[ i ] = (byte) 0;
	}
	fireRAMFloppyChanged();
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
  }


  public static RAMFloppy prepare(
				RAMFloppy rf,
				String     sysName,
				RFType     rfType,
				String     infoText,
				Properties props,
				String     propPrefix )
  {
    RAMFloppy rv = null;
    if( rf != null ) {
      if( rfType == RFType.MP_3_1988 ) {
	if( EmuUtil.getBooleanProperty(
				props,
				propPrefix + "enabled",
				false ) )
	{
	  rf.install(
		sysName,
		rfType,
		256 * 1024,
		infoText,
		EmuUtil.getProperty( props, propPrefix + "file" ) );
	  rv = rf;
	}
      } else {
	int size = getRAMFloppySize( props, propPrefix );
	if( size > 0 ) {
	  rf.install(
		sysName,
		rfType,
		size,
		infoText,
		EmuUtil.getProperty( props, propPrefix + "file" ) );
	  rv = rf;
	}
      }
    }
    return rv;
  }


  public int readByte( int port )
  {
    int rv = 0xFF;
    switch( this.rfType ) {
      case ADW:
	if( (port & 0x01) == 0 ) {
	  rv = getByte( this.addr | ((port >> 8) & 0x7F) );
	}
	break;

      case MP_3_1988:
	port &= 0x07;
	if( (port >= 0) && (port <= 3) ) {
	  rv = getByte( ((port << 16) & 0x30000) | (this.addr & 0x0FFFF) );
	  this.addr = (this.addr & 0x3FF00) | (this.addr + 1) & 0x000FF;
	  fireRAMFloppyAccess();
	}
	else if( port == 6 ) {
	  rv = (this.addr >> 8) & 0xFF;
	  fireRAMFloppyAccess();
	}
	else if( port == 7 ) {
	  rv = this.addr & 0xFF;
	  fireRAMFloppyAccess();
	}
	break;
    }
    return rv;
  }


  public void reset()
  {
    this.readOnly = false;
    this.addr     = 0;
  }


  public void save( File file ) throws IOException
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );
      if( this.dataBuf != null ) {
	out.write(
		this.dataBuf,
		0,
		Math.min( this.endOfData, this.dataBuf.length ) );
      }
      out.close();
      out = null;

      this.file        = file;
      this.dataChanged = false;
      fireRAMFloppyChanged();
    }
    finally {
      EmuUtil.doClose( out );
    }
  }


  public boolean setByte( int idx, int value )
  {
    boolean rv = false;
    if( (idx >= 0) && (idx < this.size) ) {
      ensureBufferSize();
      if( this.dataBuf != null ) {
	if( idx < this.dataBuf.length ) {
	  this.dataBuf[ idx ] = (byte) value;
	  if( idx >= this.endOfData ) {
	    this.endOfData = idx + 1;
	  }
	  this.dataChanged = true;
	  fireRAMFloppyChanged();
	  rv = true;
	}
      }
      fireRAMFloppyAccess();
    }
    return rv;
  }


  public void setRAMFloppyFld( RAMFloppyFld ramFloppyFld )
  {
    this.ramFloppyFld = ramFloppyFld;
  }


  public void writeByte( int port, int value )
  {
    switch( this.rfType ) {
      case ADW:
	if( (port & 0x01) == 0 ) {
	  if( !this.readOnly ) {
	    setByte( this.addr | ((port >> 8) & 0x7F), value );
	  } else {
	    fireRAMFloppyAccess();
	  }
	} else {
	  this.readOnly = ((port & 0x8000) != 0);
	  this.addr     = ((port << 7) & 0x3F8000) | ((value << 7) & 0x7F80);
	  fireRAMFloppyAccess();
	}
	break;

      case MP_3_1988:
	port &= 0x07;
	if( (port >= 0) && (port <= 3) ) {
	  setByte( ((port << 16) & 0x30000) | (this.addr & 0x0FFFF), value );
	  this.addr = (this.addr & 0x3FF00) | (this.addr + 1) & 0x000FF;
	}
	else if( port == 6 ) {
	  this.addr = (this.addr & 0x300FF) | ((value << 8) & 0x0FF00);
	  fireRAMFloppyAccess();
	}
	else if( port == 7 ) {
	  this.addr = (this.addr & 0x3FF00) | (value & 0x000FF);
	  fireRAMFloppyAccess();
	}
	break;
    }
  }


	/* --- private Methoden --- */

  public void ensureBufferSize()
  {
    if( this.size > 0 ) {
      if( this.dataBuf != null ) {
	if( this.dataBuf.length < this.size ) {
	  byte[] a = new byte[ this.size ];
	  System.arraycopy( this.dataBuf, 0, a, 0, this.dataBuf.length );
	  Arrays.fill( a, this.dataBuf.length, a.length, (byte) 0 );
	  this.dataBuf = a;
	}
      } else {
	this.dataBuf = new byte[ this.size ];
	Arrays.fill( this.dataBuf, (byte) 0 );
      }
    }
  }


  private void fireRAMFloppyAccess()
  {
    RAMFloppyFld fld = this.ramFloppyFld;
    if( fld != null )
      fld.fireRAMFloppyAccess();
  }


  private void fireRAMFloppyChanged()
  {
    RAMFloppyFld fld = this.ramFloppyFld;
    if( fld != null ) {
      if( (this.dataChanged != this.prevDataChanged)
	  || (this.endOfData != this.prevEndOfData) )
      {
	this.prevDataChanged = this.dataChanged;
	this.prevEndOfData   = this.endOfData;
	fld.fireRAMFloppyChanged();
      }
    }
  }


  private static int getRAMFloppySize( Properties props, String propPrefix )
  {
    int kb = EmuUtil.getIntProperty( props, propPrefix + "kbyte", 0 );
    return (kb == 128) || (kb == 512) || (kb == 2048) ? (kb * 1024) : 0;
  }


  private void initRF()
  {
    this.rfType          = RFType.OTHER;
    this.size            = 0;
    this.addr            = 0;
    this.endOfData       = 0;
    this.prevEndOfData   = 0;
    this.prevDataChanged = false;
    this.dataChanged     = false;
    this.readOnly        = false;
    this.infoText        = null;
    this.sysName         = null;
    this.file            = null;
  }
}

