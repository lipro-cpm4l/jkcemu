/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiname fuer Drag&Drop-Operationen
 */

package jkcemu.filebrowser;

import java.awt.datatransfer.*;
import java.io.File;
import java.lang.*;
import java.util.*;


public class FileListSelection implements ClipboardOwner, Transferable
{
  private java.util.List<File> files;


  public FileListSelection( Collection<File> files )
  {
    this.files = new Vector<File>( files.size() );
    this.files.addAll( files );
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
    DataFlavor[] rv = { DataFlavor.javaFileListFlavor };
    return rv;
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.javaFileListFlavor );
  }
}

