/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kapselung eines Quelltextes
 */

package jkcemu.programming;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import jkcemu.base.EmuUtil;


public class PrgSource
{
  private java.util.List<String> lines;
  private String                 text;
  private String                 name;
  private File                   file;
  private int                    pos;
  private int                    lineNum;
  private Map<Integer,Integer>   lineNum2Addr;


  public File getFile()
  {
    return this.file;
  }


  public static File getIncludeFile(
				PrgSource baseSource,
				String    fileName )
  {
    File file    = null;
    File dirFile = null;
    if( baseSource != null ) {
      File baseFile = baseSource.getFile();
      if( baseFile != null ) {
	dirFile = baseFile.getParentFile();
      }
    }
    if( !fileName.startsWith( "/" )
	&& (fileName.indexOf( '/' ) >= 0)
	&& ((File.separatorChar == '/')
		|| (fileName.indexOf( File.separatorChar ) < 0)) )
    {
      // plattformunabhaengige relative Pfadangabe
      try {
	String[] items = fileName.split( "/" );
	if( items != null ) {
	  if( items.length > 0 ) {
	    file = dirFile;
	    for( String s : items ) {
	      if( file != null ) {
		file = new File( file, s );
	      } else {
		file = new File( s );
	      }
	    }
	  }
	}
      }
      catch( PatternSyntaxException ex ) {}
    }
    if( file == null ) {
      file = new File( fileName );
      if( (dirFile != null) && !file.isAbsolute() ) {
	file = new File( dirFile, fileName );
      }
    }
    return file;
  }


  public Map<Integer,Integer> getLineAddrMap()
  {
    return this.lineNum2Addr;
  }


  public int getLineNum()
  {
    return this.lineNum;
  }


  public String getName()
  {
    return this.name;
  }


  public String getText()
  {
    return this.text;
  }


  public static PrgSource readFile( File file ) throws IOException
  {
    return new PrgSource(
			readLines( new FileReader( file ) ),
			null,
			file.getName(),
			file );
  }


  public static PrgSource readText(
				String text,
				String name,
				File   file )
  {
    if( (name == null) && (file != null) ) {
      name = file.getName();
    }
    return new PrgSource( null, text, name, file );
  }


  public String readLine() throws IOException
  {
    String rv = null;
    if( this.lines != null ) {
      if( (this.lineNum >= 0) && (this.lineNum < this.lines.size()) ) {
	rv = this.lines.get( this.lineNum++ );
      }
    } else if( this.text != null ) {
      int len = this.text.length();
      if( this.pos < len ) {
	int eol = this.text.indexOf( '\n', this.pos );
	if( eol >= this.pos ) {
	  rv       = this.text.substring( this.pos, eol );
	  this.pos = eol + 1;
	} else {
	  rv       = this.text.substring( this.pos );
	  this.pos = len;
	}
	this.lineNum++;
      }
    }
    return rv;
  }


  public void reset()
  {
    this.lineNum = 0;
    this.pos     = 0;
    if( this.lineNum2Addr != null ) {
      this.lineNum2Addr.clear();
    }
  }


  public void setLineAddr( int addr )
  {
    if( this.lineNum2Addr == null ) {
      this.lineNum2Addr = new HashMap<>();
    }
    this.lineNum2Addr.put( this.lineNum, addr );
  }


	/* --- Konstruktor --- */

  private PrgSource(
		java.util.List<String> lines,
		String                 text,
		String                 name,
		File                   file )
  {
    this.lines        = lines;
    this.file         = file;
    this.text         = text;
    this.name         = name;
    this.pos          = 0;
    this.lineNum      = 0;
    this.lineNum2Addr = null;
  }


	/* --- private Methoden --- */

  private static java.util.List<String> readLines( Reader in )
						throws IOException
  {
    java.util.List<String> lines  = new ArrayList<>( 0x1000 );
    BufferedReader         reader = null;
    try {
      reader      = new BufferedReader( in );
      String line = reader.readLine();
      while( line != null ) {
	lines.add( line );
	line = reader.readLine();
      }
    }
    finally {
      EmuUtil.doClose( reader );
    }
    return lines;
  }
}
