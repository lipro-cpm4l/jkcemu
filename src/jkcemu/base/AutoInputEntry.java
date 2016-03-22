/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Element einer automatischen Tastatureingabe
 */

package jkcemu.base;

import java.io.UnsupportedEncodingException;
import java.lang.*;
import java.net.URLDecoder;
import java.util.*;


public class AutoInputEntry
{
  private int    millisToWait;
  private String inputText;
  private String remark;


  public AutoInputEntry( int millisToWait, String inputText, String remark )
  {
    this.millisToWait = millisToWait;
    this.inputText    = inputText;
    this.remark       = remark;
  }


  public int getMillisToWait()
  {
    return this.millisToWait;
  }


  public String getInputText()
  {
    return this.inputText;
  }


  public String getRemark()
  {
    return this.remark;
  }


  public static java.util.List<AutoInputEntry> readEntries(
						Properties props,
						String     propPrefix )
  {
    java.util.List<AutoInputEntry> rv = null;
    if( (props != null) && (propPrefix != null) ) {
      int n = EmuUtil.getIntProperty( props, propPrefix + "count", 0 );
      if( n > 0 ) {
	rv = new ArrayList<>();
	for( int i = 0; i < n; i++ ) {
	  String prefix    = propPrefix + String.valueOf( i );
	  String inputText = props.getProperty( prefix + ".input_text" );
	  if( inputText != null ) {
	    if( !inputText.isEmpty() ) {
	      try {
		rv.add( new AutoInputEntry(
				EmuUtil.getIntProperty(
					props,
					prefix + ".wait.millis",
					0 ),
				URLDecoder.decode( inputText, "UTF-8" ),
				props.getProperty( prefix + ".remark" ) ) );
	      }
	      catch( UnsupportedEncodingException ex ) {}
	    }
	  }
	}
      }
    }
    return rv;
  }
}
