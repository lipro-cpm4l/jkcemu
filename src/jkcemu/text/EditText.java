/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verwaltung eines im Editor befindlichen Textes
 */

package jkcemu.text;

import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.print.*;
import java.io.*;
import java.lang.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.PrgOptions;
import jkcemu.print.*;
import jkcemu.ac1.AC1;
import jkcemu.z1013.Z1013;
import z80emu.Z80MemView;


public class EditText implements
				CaretListener,
				DocumentListener,
				Printable,
				UndoableEditListener
{
  private boolean       used;
  private boolean       dataChanged;
  private boolean       prjChanged;
  private boolean       saved;
  private boolean       trimLines;
  private EmuThread     emuThread;
  private PrgOptions    prgOptions;
  private File          file;
  private File          prjFile;
  private CharConverter charConverter;
  private String        encodingName;
  private String        encodingDisplayText;
  private String        lineEnd;
  private String        textName;
  private String        textValue;
  private JTextArea     textArea;
  private Component     tabComponent;
  private DropTarget    dropTarget1;
  private DropTarget    dropTarget2;
  private UndoManager   undoMngr;
  private EditText      resultEditText;
  private EditFrm       editFrm;


  public EditText(
		EditFrm   editFrm,
		EmuThread emuThread,
		Component tabComponent,
		JTextArea textArea,
		boolean   used )
  {
    init( editFrm, emuThread );
    this.used     = used;
    this.textName = "Neuer Text";

    int n = this.editFrm.getNewTextNum();
    if( n > 1 ) {
      this.textName += " <" + String.valueOf( n ) + ">";
    }
    setComponents( tabComponent, textArea );
  }


  public EditText(
		EditFrm       editFrm,
		EmuThread     emuThread,
		File          file,
		CharConverter charConverter,
		String        encodingName,
		String        encodingDisplayText ) throws IOException
  {
    init( editFrm, emuThread );
    loadFile( file, charConverter, encodingName, encodingDisplayText );
  }


  public boolean canUndo()
  {
    return this.undoMngr.canUndo();
  }


  public void die()
  {
    disableDropTargets();
    if( this.textArea != null ) {
      this.textArea.removeCaretListener( this );
      Document doc = this.textArea.getDocument();
      if( doc != null ) {
	doc.removeDocumentListener( this );
	doc.removeUndoableEditListener( this );
      }
    }
    this.editFrm             = null;
    this.file                = null;
    this.charConverter       = null;
    this.encodingDisplayText = null;
    this.encodingName        = null;
    this.lineEnd             = null;
    this.textName            = null;
    this.textValue           = null;
    this.textArea            = null;
    this.tabComponent        = null;
    this.undoMngr.discardAllEdits();
  }


  public int getCaretPosition()
  {
    return this.textArea != null ? this.textArea.getCaretPosition() : 0;
  }


  public CharConverter getCharConverter()
  {
    return this.charConverter;
  }


  public EditFrm getEditFrm()
  {
    return this.editFrm;
  }


  public String getEncodingDisplayText()
  {
    return this.encodingDisplayText;
  }


  public String getEncodingName()
  {
    return this.encodingName;
  }


  public File getFile()
  {
    return this.file;
  }


  public JTextArea getJTextArea()
  {
    return this.textArea;
  }


  public String getLineEnd()
  {
    return this.lineEnd;
  }


  public String getName()
  {
    return this.textName;
  }


  public PrgOptions getPrgOptions()
  {
    return this.prgOptions;
  }


  public File getProjectFile()
  {
    return this.prjFile;
  }


  public EditText getResultEditText()
  {
    return this.resultEditText;
  }


  public Component getTabComponent()
  {
    return this.tabComponent;
  }


  public int getTabSize()
  {
    int rv = 8;
    if( this.textArea != null ) {
      rv = this.textArea.getTabSize();
      if( rv < 1 )
	rv = 8;
    }
    return rv;
  }


  public String getText()
  {
    String text = null;
    if( this.textArea != null ) {
      text = this.textArea.getText();
    }
    else if( this.textValue != null ) {
      text = this.textValue;
    }
    return text != null ? text : "";
  }


  public int getTextLength()
  {
    if( this.textArea != null ) {
      return this.textArea.getDocument().getLength();
    }
    if( this.textValue != null ) {
      return this.textValue.length();
    }
    return 0;
  }


  public boolean getTrimLines()
  {
    return this.trimLines;
  }


  public void gotoLine( int lineNum )
  {
    this.editFrm.setState( Frame.NORMAL );
    this.editFrm.toFront();
    this.editFrm.setSelectedTabComponent( this.tabComponent );
    this.editFrm.gotoLine( this.textArea, lineNum );
  }


  public boolean hasDataChanged()
  {
    return this.dataChanged;
  }


  public boolean hasProjectChanged()
  {
    return this.prjChanged || (this.prgOptions == null);
  }


  public boolean isSameFile( File file )
  {
    return ((this.file != null) && (file != null)) ?
				this.file.equals( file ) : false;
  }


  public boolean isSaved()
  {
    return this.saved;
  }


  public boolean isUsed()
  {
    return this.used;
  }


  public void loadFile(
		File          file,
		CharConverter charConverter,
		String        encodingName,
		String        encodingDisplayText ) throws IOException
  {
    PushbackInputStream inStream  = null;
    PushbackReader      reader    = null;
    boolean             charsLost = false;
    boolean             hasCR     = false;
    boolean             hasNL     = false;
    boolean             has1E     = false;

    try {
      boolean  filtered = false;
      FileInfo fileInfo = null;
      String   text     = null;
      byte[]  fileBytes = EmuUtil.readFile( file, Integer.MAX_VALUE );
      if( fileBytes != null ) {
	fileInfo = FileInfo.analyzeFile( fileBytes, fileBytes.length, file );
	if( fileInfo != null ) {
	  String fileFmt = fileInfo.getFileFormat();
	  if( fileFmt != null ) {
	    try {
	      LoadData loadData = null;
	      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
		switch( fileInfo.getFileType() ) {
		  case 'A':
		    loadData = FileInfo.createLoadData( fileBytes, fileFmt );
		    if( loadData != null ) {
		      text = SourceUtil.getAssemblerText(
						loadData,
						fileInfo.getBegAddr() );
		    }
		    break;

		  case 'B':
		    loadData = FileInfo.createLoadData( fileBytes, fileFmt );
		    if( loadData != null ) {
		      int addr = fileInfo.getBegAddr();
		      switch( addr >> 8 ) {
			case 0x03:
			case 0x04:
			  addr = 0x0401;
			  break;

			case 0x2B:
			case 0x2C:
			  addr = 0x2C01;
			  break;
		      }
		      text = SourceUtil.getKCBasicProgram( loadData, addr );
		    }
		    break;

		  case 'b':
		    loadData = FileInfo.createLoadData( fileBytes, fileFmt );
		    if( loadData != null ) {
		      switch( loadData.getBegAddr() ) {
			case 0x1000:
			  text = Z1013.getTinyBasicProgram( loadData );
			  break;

			case 0x18C0:
			  text = AC1.getTinyBasicProgram( loadData );
			  break;
		      }
		    }
		    break;

		  case 'I':
		  case 'T':
		    if( fileBytes.length > 32 ) {
		      StringBuilder buf = new StringBuilder(
						fileBytes.length - 32 );
		      boolean cr = false;
		      for( int i = 32; i < fileBytes.length; i++ ) {
			int b = fileBytes[ i ] & 0xFF;
			if( b == '\r' ) {
			  cr = true;
			  buf.append( (char) '\n' );
			}
			else if( b == '\n' ) {
			  if( cr ) {
			    cr = false;
			  } else {
			    buf.append( (char) '\n' );
			  }
			}
			else if( b == 0x1E ) {
			  buf.append( (char) '\n' );
			}
			else if( (b == '\t') || (b >= 0x20) ) {
			  buf.append( (char) b );
			}
		      }
		      text = buf.toString();
		    }
		    break;
		}
	      }
	      else if( fileFmt.equals( FileInfo.KCB )
		       || fileFmt.equals( FileInfo.KCTAP_BASIC )
		       || fileFmt.equals( FileInfo.KCBASIC_HEAD )
		       || fileFmt.equals( FileInfo.KCBASIC_PURE ) )
	      {
		loadData = FileInfo.createLoadData( fileBytes, fileFmt );
		if( loadData != null ) {
		  text = SourceUtil.getKCBasicProgram(
						loadData,
						fileInfo.getBegAddr() );
		}
	      }
	    }
	    catch( IOException ex ) {
	      text = null;
	    }
	  }
	}
      }
      if( text != null ) {
	filtered      = true;
	charConverter = null;
      } else {
	if( fileBytes != null ) {
	  if( fileBytes.length > 0 ) {
	    StringBuilder textBuf = new StringBuilder( fileBytes.length );

	    inStream = new PushbackInputStream(
				new ByteArrayInputStream( fileBytes ) );

	    // ggf. Zeichensatzumwandlung aktivieren
	    if( charConverter == null ) {
	      reader = new PushbackReader( encodingName != null ?
				new InputStreamReader( inStream, encodingName )
				: new InputStreamReader( inStream ) );
	    }

	    // erste Zeile bis zum Zeilenende lesen
	    int ch = readChar( reader, inStream, charConverter );
	    while( (ch != -1)
		   && (ch != '\n') && (ch != '\r')
		   && (ch != '\u001E') )
	    {
	      if( ch == 0 ) {
		if( charConverter != null )
		  charsLost = true;
	      } else {
		textBuf.append( (char) ch );
	      }
	      ch = readChar( reader, inStream, charConverter );
	    }

	    // Zeilenende pruefen
	    if( ch == '\r' ) {

	      // Zeilenende ist entweder CR oder CRLF
	      hasCR = true;
	      textBuf.append( (char) '\n' );

	      // kommt noch ein LF?
	      ch = readChar( reader, inStream, charConverter );
	      if( (ch == 0) && (charConverter != null) ) {
		charsLost = true;
	      }
	      else if( ch == '\n' ) {
		// Zeilenende ist CRLF
		hasNL = true;
	      }
	      else if( ch != -1 ) {
		// Zeilenende ist nur CR
		// gelesenes Zeichen zurueck
		if( reader != null ) {
		  reader.unread( ch );
		} else {
		  inStream.unread( ch );
		}
	      }
	    }
	    else if( ch == '\n' ) {
	      textBuf.append( (char) '\n' );
	      hasNL = true;
	    }
	    else if( ch == '\u001E' ) {
	      textBuf.append( (char) '\n' );
	      has1E = true;
	    }

	    // restliche Datei einlesen, aber nur,
	    // wenn das zuletzt gelesene Zeichen nicht EOF war
	    if( ch != -1 ) {
	      boolean wasCR = false;
	      ch = readChar( reader, inStream, charConverter );
	      while( ch >= 0 ) {
		if( (ch == 0) && (charConverter != null) ) {
		  charsLost = true;
		}
		else if( ch == '\r' ) {
		  textBuf.append( (char) '\n' );
		  wasCR = true;
		} else {
		  if( ch == '\n' ) {
		    if( !wasCR )
		      textBuf.append( (char) ch );
		  }
		  else if( (ch == '\u001E') || (ch == '\f') ) {
		    textBuf.append( (char) '\n' );
		  }
		  // Null-Bytes und Textendezeichen herausfiltern
		  else if( (ch != 0)
			   && (ch != '\u0003') && (ch != '\u0004')
			   && (ch != '\u001A') )
		  {
		    textBuf.append( (char) ch );
		  }
		  wasCR = false;
		}
		ch = readChar( reader, inStream, charConverter );
	      }
	    }
	    if( reader != null ) {
	      reader.close();
	      reader = null;
	    } else {
	      inStream.close();
	    }
	    inStream = null;

	    // alles OK -> Text und Werte uebernehmen
	    if( hasCR ) {
	      if( hasNL ) {
		this.lineEnd = "\r\n";
	      } else {
		this.lineEnd = "\r";
	      }
	    } else {
	      if( hasNL ) {
		this.lineEnd = "\n";
	      } else if( has1E ) {
		this.lineEnd = "\u001E";
	      } else {
		this.lineEnd = null;
	      }
	    }
	    text = textBuf.toString();
	  }
	}
      }
      this.used                = true;
      this.charConverter       = charConverter;
      this.encodingName        = encodingName;
      this.encodingDisplayText = encodingDisplayText;
      if( filtered ) {
	this.file     = null;
	this.textName = "Neuer Text (" + file.getName() + ")";
	setDataUnchanged( false );
      } else {
	this.file     = file;
	this.textName = file.getName();
	setDataUnchanged( true );
      }
      if( this.textArea != null ) {
	this.textArea.setText( text );
	this.textArea.setCaretPosition( 0 );
	this.textValue = null;
      } else {
	this.textValue = text;
      }
      this.undoMngr.discardAllEdits();
      this.editFrm.updUndoButtons();
      this.editFrm.updCaretButtons();
      this.editFrm.updTitle();

      if( charsLost ) {
	BasicDlg.showWarningDlg(
		this.editFrm,
		"Die Datei enth\u00E4lt Zeichen, die in dem gew\u00FCnschten\n"
			+ "Zeichensatz nicht existieren und somit auch nicht\n"
			+ "geladen werden k\u00F6nnen, d.h.,\n"
			+ "diese Zeichen fehlen in dem angezeigten Text.\n"
			+ "Sie sollten versuchen, die Datei mit einem\n"
			+ "anderen Zeichensatz zu laden." );
      }
    }
    catch( UnsupportedEncodingException ex ) {
      throw new IOException(
		encodingName + ": Der Zeichensatz wird auf dieser Plattform"
			+ " nicht unterst\u00FCtzt." );
    }
    finally {
      EmuUtil.doClose( reader );
      EmuUtil.doClose( inStream );
    }
  }


  public void replaceText( String text )
  {
    boolean docChanged = false;
    if( this.textArea != null ) {
      Document doc = this.textArea.getDocument();
      if( doc != null ) {
	try {
	  int len = doc.getLength();
	  if( doc instanceof AbstractDocument ) {
	    ((AbstractDocument) doc).replace( 0, len, text, null );
	  } else {
	    if( len > 0 ) {
	      doc.remove( 0, len );
	    }
	    if( text != null ) {
	      doc.insertString( 0, text, null );
	    }
	  }
	  docChanged = true;
	}
	catch( BadLocationException ex ) {}
      }
      if( !docChanged ) {
	this.textArea.setText( text );
      }
      this.textArea.setCaretPosition( 0 );
    }
    else if( this.textValue != null ) {
      this.textValue = text;
    }
    if( !docChanged ) {
      this.undoMngr.discardAllEdits();
    }
    this.editFrm.updUndoButtons();
    this.editFrm.updCaretButtons();
  }


  public void saveFile(
		Component     owner,
		File          file,
		CharConverter charConverter,
		String        encodingName,
		String        encodingDisplayText,
		boolean       trimLines,
		String        lineEnd ) throws IOException
  {
    boolean charsLost = false;
    String  text      = (this.textArea != null ?
				this.textArea.getText() : this.textValue);
    if( text == null )
      text = "";

    // Zeilenende
    if( lineEnd == null ) {
      lineEnd = System.getProperty( "line.separator" );
    }
    byte[] lineEndBytes = null;
    if( lineEnd != null ) {
      lineEndBytes = lineEnd.getBytes();
      if( lineEndBytes != null ) {
	if( lineEndBytes.length < 1 )
	  lineEndBytes = null;
      }
    }
    if( lineEnd == null ) {
      lineEndBytes      = new byte[ 2 ];
      lineEndBytes[ 0 ] = (byte) '\r';
      lineEndBytes[ 1 ] = (byte) '\n';
    }

    FileOutputStream outStream = null;
    try {
      outStream  = new FileOutputStream( file );

      BufferedWriter outWriter = null;
      if( charConverter == null ) {
	outWriter = new BufferedWriter( encodingName != null ?
			new OutputStreamWriter( outStream, encodingName )
			: new OutputStreamWriter( outStream ) );
      }

      int len = text.length();
      int ch;

      if( trimLines ) {
	int spaceBegPos = -1;
	for( int i = 0; i < len; i++ ) {
	  ch = text.charAt( i );
	  switch( ch ) {
	    case '\n':
	      if( outWriter != null ) {
		writeLineEnd( outWriter, lineEnd );
	      } else {
		outStream.write( lineEndBytes );
	      }
	      spaceBegPos = -1;
	      break;

	    case '\t':
	    case '\u0020':
	      if( spaceBegPos < 0 ) {
		spaceBegPos = i;
	      }
	      break;

	    default:
	      // Pruefen, ob vor dem Zeichen Leerzeichen waren,
	      // die ausgegeben werden muessen
	      if( spaceBegPos >= 0 ) {
		for( int k = spaceBegPos; k < i; k++ ) {
		  char chSpace = text.charAt( k );
		  if( outWriter != null ) {
		    outWriter.write( chSpace );
		  } else {
		    outStream.write( (int) chSpace & 0xFF );
		  }
		}
		spaceBegPos = -1;
	      }
	      if( outWriter != null ) {
		outWriter.write( ch );
	      } else {
		if( !writeChar( outStream, charConverter, ch ) )
		  charsLost = true;
	      }
	  }
	}

      } else {

	if( outWriter != null ) {
	  for( int i = 0; i < len; i++ ) {
	    ch = text.charAt( i );
	    if( ch == '\n' ) {
	      writeLineEnd( outWriter, lineEnd );
	    } else {
	      outWriter.write( ch );
	    }
	  }
	} else {
	  for( int i = 0; i < len; i++ ) {
	    ch = text.charAt( i );
	    if( ch == '\n' ) {
	      outStream.write( lineEndBytes );
	    } else {
	      if( !writeChar( outStream, charConverter, ch ) )
		charsLost = true;
	    }
	  }
	}
      }

      if( outWriter != null ) {
	outWriter.flush();		// schliesst auch "this.outStream"
	outWriter.close();
      } else {
	outStream.close();
      }
      outStream = null;

      if( !isSameFile( file ) ) {
	setProjectChanged( true );
      }

      this.file                = file;
      this.charConverter       = charConverter;
      this.encodingName        = encodingName;
      this.encodingDisplayText = encodingDisplayText;
      this.trimLines           = trimLines;
      this.lineEnd             = lineEnd;
      this.textName            = this.file.getName();
      setDataUnchanged( true );
      this.editFrm.updTitle();

      if( charsLost ) {
	BasicDlg.showWarningDlg(
		owner,
		"Der Text enth\u00E4lt Zeichen, die in dem gew\u00FCnschten\n"
			+ "Zeichensatz nicht existieren und somit auch nicht\n"
			+ "gespeichert werden k\u00F6nnen, d.h.,\n"
			+ "diese Zeichen fehlen in der gespeicherten Datei." );
      }
    }
    catch( UnsupportedEncodingException ex ) {
      throw new IOException(
		encodingName + ": Der Zeichensatz wird auf dieser Plattform"
			+ " nicht unterst\u00FCtzt." );
    }
    finally {
      EmuUtil.doClose( outStream );
    }
  }


  public boolean saveProject( Frame frame, boolean askFileName )
  {
    boolean rv = false;
    if( this.file != null ) {
      File prjFile = this.prjFile;
      if( (prjFile == null) || askFileName ) {
	File preSelection = prjFile;
	if( preSelection == null ) {
	  File   srcParent = this.file.getParentFile();
	  String srcName   = this.file.getName();
	  if( srcName != null ) {
	    String prjName = srcName;
	    int    pos     = srcName.lastIndexOf( '.' );
	    if( (pos >= 0) && (pos < srcName.length() ) ) {
	      prjName = srcName.substring( 0, pos );
	    }
	    prjName += ".prj";
	    if( srcParent != null ) {
	      preSelection = new File( srcParent, prjName );
	    } else {
	      preSelection = new File( prjName );
	    }
	  }
	}
	prjFile = EmuUtil.showFileSaveDlg(
				frame,
				"Projekt speichern",
				preSelection,
				EmuUtil.getProjectFileFilter() );
      }
      if( prjFile != null ) {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( prjFile );

	  Properties props = new Properties();
	  props.setProperty( "jkcemu.properties.type", "project" );
	  props.setProperty(
			"jkcemu.programming.source.file.name",
			this.file.getPath() );
	  if( this.prgOptions != null ) {
	    this.prgOptions.putOptionsTo( props );
	  }
	  props.storeToXML( out, "Programming Options" );
	  out.close();
	  out             = null;
	  this.prjFile    = prjFile;
	  rv              = true;
	  setProjectChanged( false );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg(
		frame,
		"Speichern des Projektes fehlgeschlagen:\n"
			+ ex.getMessage() );
	}
	finally {
	  if( out != null ) {
	    try {
	      out.close();
	    }
	    catch( IOException ex ) {}
	  }
	}
      }
    } else {
      BasicDlg.showErrorDlg(
		frame,
		"Das Projekt kann nicht gespeichert werden,\n"
			+ "da kein Dateiname f\u00FCr den Quelltext"
			+ " bekannt ist.\n"
			+ "Speichern Sie bitte zuerst den Quelltext\n"
			+ "und dann das Projekt." );
    }
    return rv;
  }


  public void setCaretPosition( int pos )
  {
    if( this.textArea != null ) {
      try {
	this.textArea.setCaretPosition( pos );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  public void setComponents( Component tabComponent, JTextArea textArea )
  {
    this.tabComponent = tabComponent;
    this.textArea     = textArea;

    // Text anzeigen
    if( this.textArea != null ) {
      this.textArea.setText( this.textValue );
      this.textArea.setCaretPosition( 0 );
      this.textArea.addCaretListener( this );
      this.textValue = null;

      // unbedingt hinter Text anzeigen
      Document doc = this.textArea.getDocument();
      if( doc != null ) {
	doc.addDocumentListener( this );
	doc.addUndoableEditListener( this );
      }
    }

    // Komponenten als Drop-Ziele aktivieren
    disableDropTargets();
    if( tabComponent != null ) {
      this.dropTarget1 = new DropTarget( tabComponent, this.editFrm );
      this.dropTarget1.setActive( true );
    }
    if( textArea != null ) {
      this.dropTarget2 = new DropTarget( textArea, this.editFrm );
      this.dropTarget2.setActive( true );
    }
  }


  public void setDataChanged()
  {
    boolean wasChanged = this.dataChanged;
    this.dataChanged   = true;
    this.saved         = false;
    if( this.dataChanged != wasChanged )
      this.editFrm.dataChangedStateChanged( this );
  }


  public void setPrgOptions( PrgOptions options )
  {
    this.prgOptions = options;
    setProjectChanged( true );
  }


  public void setProject( File file, Properties props )
  {
    this.prgOptions = PrgOptions.getPrgOptions( this.emuThread, props );
    this.prjFile    = file;
    this.prjChanged = false;
  }


  public void setProjectChanged( boolean state )
  {
    boolean wasChanged = this.prjChanged;
    this.prjChanged    = state;
    if( state && !wasChanged )
      this.editFrm.dataChangedStateChanged( this );
  }


  public void setText( String text )
  {
    if( this.textArea != null ) {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( 0 );
    }
    else if( this.textValue != null ) {
      this.textValue = text;
    }
    this.undoMngr.discardAllEdits();
    this.editFrm.updUndoButtons();
    this.editFrm.updCaretButtons();
    setDataUnchanged( true );
  }


  public void setResultEditText( EditText editText )
  {
    this.resultEditText = editText;
  }


  public void undo()
  {
    if( this.undoMngr.canUndo() ) {
      this.undoMngr.undo();
    }
    this.editFrm.updUndoButtons();
  }


	/* --- CaretListener --- */
 
  public void caretUpdate( CaretEvent e )
  {
    this.editFrm.caretPositionChanged();
  }


	/* --- DocumentListener --- */
 
  public void changedUpdate( DocumentEvent e )
  {
    setDataChanged();
    this.used = true;
  }
 
 
  public void insertUpdate( DocumentEvent e )
  {
    setDataChanged();
    this.used = true;
  }
 
 
  public void removeUpdate( DocumentEvent e )
  {
    setDataChanged();
    this.used = true;
  }


	/* --- Printable --- */

  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    String text    = getText();
    int    tabSize = getTabSize();

    // Anzahl Zeilen pro Seite
    int fontSize     = Main.getPrintFontSize();
    int linesPerPage = (int) pf.getImageableHeight() / fontSize;
    if( Main.getPrintFileName() || Main.getPrintPageNum() ) {
      linesPerPage -= 2;
    }
    if( (linesPerPage < 1) || (pf.getImageableWidth() < 1.0) ) {
      throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
    }

    // vorherige Seiten ueberspringen
    int linesToSkip = pageNum * linesPerPage;
    int len         = text.length();
    int pos         = 0;
    while( (linesToSkip > 0) && (pos < len) ) {
      int i = text.indexOf( '\n', pos );
      if( i < pos )
	return NO_SUCH_PAGE;

      pos = i + 1;
      --linesToSkip;
    }
    if( (linesToSkip > 0) || (pos >= len) )
      return NO_SUCH_PAGE;

    // Seite drucken
    g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );
    g.setColor( Color.black );

    int x = (int) pf.getImageableX();
    int y = (int) pf.getImageableY() + fontSize;
    while( (linesPerPage > 0) && (pos < len) ) {
      int i = text.indexOf( '\n', pos );
      if( i >= pos ) {
	g.drawString(
		PrintUtil.expandTabs( text.substring( pos, i ), tabSize ),
		x,
		y );
	pos = i + 1;
      } else {
	g.drawString(
		PrintUtil.expandTabs( text.substring( pos ), tabSize ),
		x,
		y );
	pos = len;
      }
      y += fontSize;
      --linesPerPage;
    }

    // Fusszeile
    fontSize -= 2;
    if( fontSize < 6 ) {
      fontSize = 6;
    }
    y = (int) (pf.getImageableY() + pf.getImageableHeight());
    String fileName = (this.file != null ? this.file.getName() : null);
    if( Main.getPrintFileName() && (fileName != null) ) {
      g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );
      g.drawString( fileName, x, y );
      if( Main.getPrintPageNum() ) {
	String s = String.valueOf( pageNum + 1 );
	g.drawString(
		s,
		(int) (pf.getImageableX() + pf.getImageableWidth()
				- g.getFontMetrics().stringWidth( s )),
		y );
      }
    } else {
      if( Main.getPrintPageNum() )
	PrintUtil.printCenteredPageNum( g, pf, fontSize, pageNum + 1 );
    }

    return PAGE_EXISTS;
  }


	/* --- UndoableEditListener --- */
 
  public void undoableEditHappened( UndoableEditEvent e )
  {
    this.undoMngr.undoableEditHappened( e );
    this.editFrm.updUndoButtons();
  }


	/* --- private Methoden --- */

  private void init( EditFrm editFrm, EmuThread emuThread )
  {
    this.editFrm             = editFrm;
    this.emuThread           = emuThread;
    this.undoMngr            = new UndoManager();
    this.prgOptions          = null;
    this.file                = null;
    this.charConverter       = null;
    this.encodingName        = null;
    this.encodingDisplayText = null;
    this.lineEnd             = null;
    this.resultEditText      = null;
    this.textName            = null;
    this.textValue           = null;
    this.textArea            = null;
    this.tabComponent        = null;
    this.dropTarget1         = null;
    this.dropTarget2         = null;
    this.used                = false;
    this.dataChanged         = false;
    this.prjChanged          = false;
    this.saved               = true;
    this.trimLines           = false;
  }


  private void disableDropTargets()
  {
    DropTarget dt = this.dropTarget1;
    if( dt != null ) {
      this.dropTarget1 = null;
      dt.setActive( false );
    }
    dt = this.dropTarget2;
    if( dt != null ) {
      this.dropTarget2 = null;
      dt.setActive( false );
    }
  }


  private int readChar(
		Reader        reader,
		InputStream   inStream,
		CharConverter charConverter ) throws IOException
  {
    int ch = -1;
    if( reader != null ) {
      ch = reader.read();
    } else {
      ch = inStream.read();
      if( (ch != -1) && (charConverter != null) )
	ch = charConverter.toUnicode( ch );
    }
    return ch;
  }


  private void setDataUnchanged( final boolean saved )
  {
    final EditText editText   = this;
    editText.dataChanged      = false;
    editText.saved            = saved;
    SwingUtilities.invokeLater(
	new Runnable() {
	  public void run()
	  {
	    editText.dataChanged = false;
	    editText.saved       = saved;
	    if( editText.editFrm != null )
	      editText.editFrm.dataChangedStateChanged( editText );
	  }
	} );
  }


  private boolean writeChar(
			OutputStream  out,
			CharConverter charConverter,
			int           ch ) throws IOException
  {
    boolean rv = false;
    if( (ch > 0) && (charConverter != null) ) {
      ch = charConverter.toCharsetByte( (char) ch );
    }
    if( (ch > 0) && (ch <= 0xFF) ) {
      out.write( ch );
      rv = true;
    }
    return rv;
  }


  private void writeLineEnd(
			BufferedWriter outWriter,
			String         lineEnd ) throws IOException
  {
    if( lineEnd != null )
      outWriter.write( lineEnd );
    else
      outWriter.newLine();
  }
}

