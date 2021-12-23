/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Uebertragung einer Dateiliste
 */

package jkcemu.file;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


public class TransferableFileList implements ClipboardOwner, Transferable
{
  private java.util.List<File> files;


  public TransferableFileList( File file ) throws IOException
  {
    this( Collections.singleton( file ) );
  }


  public TransferableFileList( Collection<File> files ) throws IOException
  {
    int n = files.size();
    this.files = new ArrayList<>( n > 0 ? n : 1 );
    for( File file : files ) {
      boolean failed = false;
      try {
	if( file.toPath() == null ) {
	  failed = true;
	}
	this.files.add( file );
      }
      catch( InvalidPathException ex ) {
	failed = true;
      }
      finally {
	if( failed ) {
	  throw new IOException(
		"Kopieren/Verschieben von virtuellen Dateien"
			+ " bzw. Verzeichnisse nicht m\u00F6glich" );
	}
      }
    }
  }


	/* --- ClipboardOwner --- */

  @Override
  public void lostOwnership( Clipboard clipboard, Transferable contents )
  {
    // leer
  }


	/* --- Transferable --- */

  @Override
  public Object getTransferData( DataFlavor flavor )
					throws UnsupportedFlavorException
  {
    if( !isDataFlavorSupported( flavor ) ) {
      throw new UnsupportedFlavorException( flavor );
    }
    return this.files;
  }


  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return new DataFlavor[] { DataFlavor.javaFileListFlavor };
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.javaFileListFlavor );
  }
}
