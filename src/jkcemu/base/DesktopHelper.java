/*
 * (c) 2019-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Desktop-Integration
 *
 * Mit Java 9 hat sich die Desktop-Integration verbessert.
 * Dieser Quelltext soll jedoch mit einer niedrigeren Java-Version
 * compilierfaehig sein.
 * Um trotzdem eine bessere Dektop-Integration zu ermoeglichen,
 * sofern man eine Java-Laufzeitumgebung mit einer hoeheren Versionsnummer
 * verwendet, wird versucht, eine abgeleitete Klasse nachzuladen,
 * die moeglichereweise mit einer hoeheren Java-Version compiliert wurde
 * und eine bessere Desktop-Integration bietet.
 * Konkret wird folgende Klasse versucht nachzuladen:
 *
 *   jkcemu.base.jversion.DesktopHelper_X
 *
 * Dabei wird das X durch die Hauptversionsnummer der verwendeten
 * Java-Laufzeitumgebung ersetzt.
 * Gibt es diese Klasse nicht, wird das X um eins verringert
 * und erneut versucht, die Klasse zu laden.
 * Das wird solange fortgesetzt, bis entweder eine Klasse geladen
 * werde konnte oder X=7 ist.
 */

package jkcemu.base;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import jkcemu.Main;


public class DesktopHelper
{
  protected static DesktopHelper instance = null;
  protected Desktop              desktop  = null;


  protected DesktopHelper()
  {
    if( Desktop.isDesktopSupported() ) {
      try {
	this.desktop = Desktop.getDesktop();
      }
      catch( UnsupportedOperationException ex ) {}
    }
  }


  public static void install( BaseFrm topFrm )
  {
    if( instance == null ) {
      int v = Main.getMainJavaVersion();
      if( v > 0 ) {
	for( int i = v; i >= 7; --i ) {
	  try {
	    Class<?> cl = Class.forName(
			String.format(
				"jkcemu.base.jversion.DesktopHelper_%d",
				i ) );
	    if( DesktopHelper.class.isAssignableFrom( cl ) ) {
	      instance = (DesktopHelper) cl.getConstructor(
				BaseFrm.class ).newInstance( topFrm );
	      break;
	    }
	  }
	  catch( Exception ex ) {}
	  catch( LinkageError ex ) {}
	}
      }
      if( instance == null ) {
	instance = new DesktopHelper();
      }
    }
  }


  public static boolean isMoveToTrashSupported()
  {
    return instance != null ?
		instance.isMoveToTrashSupportedInternal()
		: false;
  }


  protected boolean isMoveToTrashSupportedInternal()
  {
    return false;
  }


  public static boolean isOpenSupported()
  {
    return instance != null ?
		instance.isOpenSupportedInternal()
		: false;
  }


  protected boolean isOpenSupportedInternal()
  {
    return this.desktop != null ?
		this.desktop.isSupported( Desktop.Action.OPEN )
		: false;
  }


  public static void moveToTrash( File file ) throws IOException
  {
    if( instance == null ) {
      throwMoveToTrashNotSupported();
    }
    instance.moveToTrashInternal( file );
  }


  protected void moveToTrashInternal( File file ) throws IOException
  {
    throwMoveToTrashNotSupported();
  }


  public static void open( File file ) throws IOException
  {
    if( instance == null ) {
      throwOpenNotSupported();
    }
    instance.openInternal( file );
  }


  protected void openInternal( File file ) throws IOException
  {
    if( this.desktop == null ) {
      throwOpenNotSupported();
    }
    try {
      this.desktop.open( file );
    }
    catch( UnsupportedOperationException ex ) {
      throwOpenNotSupported();
    }
  }


  protected static void throwMoveToTrashNotSupported() throws IOException
  {
    throw new IOException( "Das Verschieben von Dateien in den Papierkorb"
		+ "wird von der verwendeten\n"
		+ "Java-Laufzeitumgebung nicht unterst\u00FCtzt." );
  }


  protected static void throwOpenNotSupported() throws IOException
  {
    throw new IOException( "Das \u00D6ffnen von Dateien in einem"
		+ "externen Programm wird von der verwendeten\n"
		+ "Java-Laufzeitumgebung nicht unterst\u00FCtzt." );
  }
}
