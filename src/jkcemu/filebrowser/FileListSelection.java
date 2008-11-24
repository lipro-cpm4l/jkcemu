/*
 * (c) 2008 Jens Mueller
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


public class FileListSelection implements Transferable
{
  private java.util.List<File> files;


  public FileListSelection( Collection<File> files )
  {
    this.files = new Vector<File>( files.size() );
    this.files.addAll( files );
  }


  public Object getTransferData( DataFlavor flavor )
					throws UnsupportedFlavorException
  {
    if( !isDataFlavorSupported( flavor ) ) {
      throw new UnsupportedFlavorException( flavor );
    }
    return this.files;
  }


  public DataFlavor[] getTransferDataFlavors()
  {
    DataFlavor[] rv = { DataFlavor.javaFileListFlavor };
    return rv;
  }


  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.javaFileListFlavor );
  }
}

