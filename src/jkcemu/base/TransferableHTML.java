/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Uebertragbarer HTML-Text
 */

package jkcemu.base;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;


public class TransferableHTML implements ClipboardOwner, Transferable
{
  private static DataFlavor[] htmlFlavors;

  private String htmlText;


  public TransferableHTML( String htmlText )
  {
    this.htmlText = htmlText;
    if( htmlFlavors == null ) {
      java.util.List<DataFlavor> flavors = new ArrayList<>();
      addFlavor( flavors, "text/html; class=java.lang.String" );
      addFlavor( flavors, "text/html; class=java.io.Reader" );
      addFlavor(
		flavors,
		"text/html; charset=utf-8; class=java.io.InputStream" );
      htmlFlavors = flavors.toArray( new DataFlavor[ flavors.size() ] );
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
    Object   rv = null;
    Class<?> cl = flavor.getRepresentationClass();
    if( cl != null ) {
      if( cl.equals( String.class ) ) {
	rv = this.htmlText;
      } else if( cl.equals( Reader.class ) ) {
	rv = new StringReader( this.htmlText );
      } else if( cl.equals( InputStream.class ) ) {
	rv = new ByteArrayInputStream( this.htmlText.getBytes() );
      }
    }
    if( rv == null ) {
      throw new UnsupportedFlavorException( flavor );
    }
    return rv;
  }


  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return htmlFlavors;
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    boolean rv = false;
    if( flavor != null ) {
      for( DataFlavor f : htmlFlavors ) {
	if( f.equals( flavor ) ) {
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static void addFlavor(
			Collection<DataFlavor> flavors,
			String                 mimeType )
  {
    try {
      flavors.add( new DataFlavor( mimeType, "HTML-formatierter Text" ) );
    }
    catch( IllegalArgumentException ex ) {}
  }
}
