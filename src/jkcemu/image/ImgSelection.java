/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Uebertragbares Bild
 */

package jkcemu.image;

import java.awt.Image;
import java.awt.datatransfer.*;
import java.lang.*;


public class ImgSelection implements ClipboardOwner, Transferable
{
  private Image img;


  public ImgSelection( Image img )
  {
    this.img = img;
  }


	/* --- ClipboardOwner --- */

  @Override
  public void lostOwnership( Clipboard clp, Transferable t )
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
    return this.img;
  }


  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    DataFlavor[] rv = { DataFlavor.imageFlavor };
    return rv;
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.imageFlavor );
  }
}

