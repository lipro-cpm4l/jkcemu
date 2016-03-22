/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Automatisch zu ladende Datei
 */

package jkcemu.base;

import java.lang.*;
import java.util.*;


public class AutoLoadEntry
{
  private int     millisToWait;
  private String  fileName;
  private Integer loadAddr;


  public AutoLoadEntry(
		int     millisToWait,
		String  fileName,
		Integer loadAddr )
  {
    this.millisToWait = millisToWait;
    this.fileName     = fileName;
    this.loadAddr     = loadAddr;
  }


  public String getFileName()
  {
    return this.fileName;
  }


  public int getMillisToWait()
  {
    return this.millisToWait;
  }


  public Integer getLoadAddr()
  {
    return this.loadAddr;
  }


  public static java.util.List<AutoLoadEntry> readEntries(
						Properties props,
						String     propPrefix )
  {
    java.util.List<AutoLoadEntry> rv = null;
    if( (props != null) && (propPrefix != null) ) {
      int n = EmuUtil.getIntProperty( props, propPrefix + "count", 0 );
      if( n > 0 ) {
	rv = new ArrayList<>();
	for( int i = 0; i < n; i++ ) {
	  String prefix   = propPrefix + String.valueOf( i );
	  String fileName = props.getProperty( prefix + ".file" );
	  if( fileName != null ) {
	    fileName = fileName.trim();
	    if( !fileName.isEmpty() ) {
	      rv.add(
		new AutoLoadEntry(
			EmuUtil.getIntProperty(
				props,
				prefix + ".wait.millis",
				0 ),
			fileName,
			getAddrProperty(
				props,
				prefix + ".address.load" ) ) );
	    }
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static Integer getAddrProperty(
				Properties props,
				String     keyword )
  {
    Integer rv = null;
    if( props != null ) {
      String s = props.getProperty( keyword );
      if( s != null ) {
	s = s.trim();
	if( !s.isEmpty() ) {
	  try {
	    int value = Integer.parseInt( s, 16 );
	    if( (value >= 0) && (value <= 0xFFFF) ) {
	      rv = new Integer( value );
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }
}
