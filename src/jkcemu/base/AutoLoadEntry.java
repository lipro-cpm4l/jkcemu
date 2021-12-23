/*
 * (c) 2015-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Automatisch zu ladende Datei
 */

package jkcemu.base;

import java.util.ArrayList;
import java.util.Properties;
import jkcemu.Main;


public class AutoLoadEntry
{
  public static final String PROP_FILE        = "file";
  public static final String PROP_LOAD_ADDR   = "address.load";
  public static final String PROP_WAIT_MILLIS = "wait.millis";

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
      int n = EmuUtil.getIntProperty(
				props,
				propPrefix + Main.PROP_COUNT,
				0 );
      if( n > 0 ) {
	rv = new ArrayList<>();
	for( int i = 0; i < n; i++ ) {
	  String prefix   = String.format( "%s%d.", propPrefix, i );
	  String fileName = props.getProperty( prefix + PROP_FILE );
	  if( fileName != null ) {
	    fileName = fileName.trim();
	    if( !fileName.isEmpty() ) {
	      rv.add(
		new AutoLoadEntry(
			EmuUtil.getIntProperty(
				props,
				prefix + PROP_WAIT_MILLIS,
				0 ),
			fileName,
			getAddrProperty(
				props,
				prefix + PROP_LOAD_ADDR ) ) );
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
	      rv = value;
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }
}
