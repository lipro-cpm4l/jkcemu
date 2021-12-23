/*
 * (c) 2010-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente zur Anzeige eines Dateinamens
 *
 * In Abhaengigkeit von der Laenge des Dateinamens
 * und der aktuellen Groesse der Kompenente wird der Dateiname
 * entweder vollstaendig oder gekuerzt angezeigt.
 *
 * Beginnt unter Windows der Dateiname mit der Kennung
 * fuer einen Geraetenamen (\\.\),
 * wird diese Kennung in der Anzeige weggelassen.
 */

package jkcemu.file;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;


public class FileNameFld extends javax.swing.JTextField
{
  private static final String READONLY_APPENDIX = " (nur lesend)";

  private boolean  readOnly;
  private File     file;
  private String[] filePathElems;


  public FileNameFld()
  {
    this.readOnly      = false;
    this.file          = null;
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


  public boolean isFileReadOnly()
  {
    return this.readOnly;
  }


  public boolean setFile( File file )
  {
    return setFile( file, false );
  }


  public boolean setFile( File file, boolean readOnly )
  {
    boolean differs = (readOnly != this.readOnly);
    if( !differs ) {
      differs = true;
      if( (file != null) && (this.file != null) ) {
	if( file.getPath().equals( this.file.getPath() ) ) {
	  differs = false;
	}
      } else {
	if( (file == null) && (this.file == null) ) {
	  differs = false;
	}
      }
    }
    if( differs ) {
      this.file          = file;
      this.filePathElems = null;
      this.readOnly      = readOnly;
      updView();
    }
    return differs;
  }


  public void setFileName( String fileName )
  {
    File file = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	file = new File( fileName );
      }
    }
    setFile( file, false );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void setText( String text )
  {
    this.file          = null;
    this.filePathElems = null;
    this.readOnly      = false;
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

    String text = null;
    if( this.file != null ) {
      text = this.file.getPath();
    }
    if( text != null ) {
      if( (File.separatorChar == '\\') && text.startsWith( "\\\\.\\" ) ) {
	text = text.substring( 4 );
      }
      if( this.readOnly ) {
	text += READONLY_APPENDIX;
      }
      super.setText( text );
      int width = getWidth();
      if( getPrefWidth() > width ) {
	if( this.filePathElems == null ) {
	  java.util.List<String> elems = new ArrayList<>();
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
	  StringBuilder buf = new StringBuilder( text.length() + 2 );
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
	    if( this.readOnly ) {
	      buf.append( READONLY_APPENDIX );
	    }
	    super.setText( buf.toString() );
	    if( getPrefWidth() <= width ) {
	      break;
	    }
	    --n;
	  }
	  setToolTipText( text );
	}
      }
    } else {
      super.setText( "" );
    }
  }


  private int getPrefWidth()
  {
    Dimension prefSize = getPreferredSize();
    return prefSize != null ? prefSize.width : -1;
  }
}
