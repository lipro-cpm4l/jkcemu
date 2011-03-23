/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente zur Anzeige eines Dateinamens
 *
 * In Abhaengigkeit von der Laenge des Dateinamens
 * und der aktuellen Groesse der Kompenente wird der Dateiname
 * entweder vollstaendig oder gekuerzt angezeigt.
 */

package jkcemu.base;

import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;


public class FileNameFld extends javax.swing.JTextField
{
  private File     file;
  private String[] filePathElems;


  public FileNameFld()
  {
    this.filePathElems = null;
    setEditable( false );
    addComponentListener(
		new ComponentAdapter()
		{
		  @Override
		  public void componentResized( ComponentEvent e )
		  {
		    updView();
		  }
		} );
  }


  public File getFile()
  {
    return this.file;
  }


  public void setFile( File file )
  {
    this.file          = file;
    this.filePathElems = null;
    updView();
  }


  public void setFileName( String fileName )
  {
    File file = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	file = new File( fileName );
      }
    }
    setFile( file );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void setText( String text )
  {
    this.file          = null;
    this.filePathElems = null;
    super.setText( text );
    setToolTipText( null );
  }


  @Override
  public void updateUI()
  {
    super.updateUI();
    updView();
  }


	/* --- private Methoden --- */

  private void updView()
  {
    setToolTipText( null );

    String fileName = null;
    if( this.file != null ) {
      fileName = this.file.getPath();
    }
    super.setText( fileName );
    if( fileName != null ) {
      int width = getWidth();
      if( getPrefWidth() > width ) {
	if( this.filePathElems == null ) {
	  java.util.List<String> elems = new ArrayList<String>();
	  File                   file  = this.file;
	  while( file != null ) {
	    String lastElem = file.getName();
	    if( lastElem != null ) {
	      if( lastElem.isEmpty() ) {
		elems.add( file.getPath() );
		file = null;
	      } else {
		elems.add( lastElem );
		file = file.getParentFile();
	      }
	    } else {
	      elems.add( file.getPath() );
	      file = null;
	    }
	  }
	  int n = elems.size();
	  /*
	   * Es muessen mindestens 3 Elemente sein,
	   * um in der Mitte etwas wegzulassen.
	   */
	  if( n > 2 ) {
	    this.filePathElems = new String[ n ];
	    for( int i = 0; i < n; i++ ) {
	      this.filePathElems[ i ] = elems.get( n - i - 1 );
	    }
	  }
	}
	if( this.filePathElems != null ) {
	  int n = this.filePathElems.length / 2;
	  if( (n * 2) == this.filePathElems.length ) {
	    --n;
	  }
	  String separator = File.separator;
	  if( separator == null ) {
	    separator = Character.toString( File.separatorChar );
	  }
	  StringBuilder buf = new StringBuilder( fileName.length() + 2 );
	  while( n > 0 ) {
	    buf.setLength( 0 );
	    for( int i = 0; i < n; i++ ) {
	      String s = this.filePathElems[ i ];
	      buf.append( s );
	      if( !s.endsWith( separator ) ) {
		buf.append( separator );
	      }
	    }
	    buf.append( "..." );
	    for( int i = this.filePathElems.length - n;
		 i < this.filePathElems.length;
		 i++ )
	    {
	      buf.append( File.separatorChar );
	      buf.append( this.filePathElems[ i ] );
	    }
	    super.setText( buf.toString() );
	    if( getPrefWidth() <= width ) {
	      break;
	    }
	    --n;
	  }
	  setToolTipText( fileName );
	}
      }
    }
  }


  private int getPrefWidth()
  {
    Dimension prefSize = getPreferredSize();
    return prefSize != null ? prefSize.width : -1;
  }
}
