/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Textdatei speichern
 */

package jkcemu.text;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class SaveTextDlg extends BasicDlg
{
  private JCheckBox    btnTrimLines;
  private JComboBox    comboEncoding;
  private JComboBox    comboLineEnd;
  private JButton      btnSave;
  private JButton      btnCancel;
  private File         file;
  private EditText     editText;
  private boolean      fileSaved;


  public SaveTextDlg( File file, EditText editText )
  {
    super( editText.getEditFrm(), "Textdatei speichern: " + file.getName() );

    this.file      = file;
    this.editText  = editText;
    this.fileSaved = false;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Bereich Dateiname
    String fileName = file.getName();
    JPanel panelFileName = new JPanel(
		new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelFileName.setBorder( BorderFactory.createTitledBorder( "Dateiname" ) );
    panelFileName.add( new JLabel( fileName != null ? fileName : "" ) );
    add( panelFileName, gbc );


    // Bereich Eigenschaften
    JPanel panelProp = new JPanel( new GridBagLayout() );
    panelProp.setBorder( BorderFactory.createTitledBorder( "Eigenschaften" ) );
    gbc.gridy++;
    add( panelProp, gbc );

    GridBagConstraints gbcProp = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    panelProp.add( new JLabel( "Zeichensatz:" ), gbcProp );

    this.comboEncoding = new JComboBox();
    this.comboEncoding.addItem( "Systemzeichensatz" );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.ASCII_7BIT ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.ISO646DE ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP437 ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP850 ) );
    this.comboEncoding.addItem( new CharConverter(
		CharConverter.Encoding.LATIN1 ) );
    this.comboEncoding.addItem( "UTF-8" );
    this.comboEncoding.addItem( "UTF-16 (mit Byte-Order-Markierung)" );
    this.comboEncoding.addItem( "UTF-16BE (Big-Endian)" );
    this.comboEncoding.addItem( "UTF-16LE (Little-Endian)" );
    this.comboEncoding.setEditable( false );
    gbcProp.anchor = GridBagConstraints.WEST;
    gbcProp.gridx++;
    panelProp.add( this.comboEncoding, gbcProp );

    gbcProp.anchor     = GridBagConstraints.EAST;
    gbcProp.insets.top = 2;
    gbcProp.gridx      = 0;
    gbcProp.gridy++;
    panelProp.add( new JLabel( "Zeilenende:" ), gbcProp );

    this.comboLineEnd = new JComboBox();
    this.comboLineEnd.addItem( new TextLineSeparator( "\r\n" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\n" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\r" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\u001E" ) );
    gbcProp.anchor = GridBagConstraints.WEST;
    gbcProp.gridx++;
    panelProp.add( this.comboLineEnd, gbcProp );

    this.btnTrimLines = new JCheckBox(
		"Unsichtbare Zeichen am Zeilenende entfernen" );
    gbcProp.insets.bottom = 5;
    gbcProp.gridy++;
    panelProp.add( this.btnTrimLines, gbcProp );


    // Bereich Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );

    this.btnSave = new JButton( "Speichern" );
    this.btnSave.addActionListener( this );
    this.btnSave.addKeyListener( this );
    panelBtn.add( this.btnSave );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Vorbelegungen
    this.btnTrimLines.setSelected( this.editText.getTrimLines() );

    Object encObj = editText.getCharConverter();
    if( encObj == null ) {
      encObj = editText.getEncodingDisplayText();
    }
    if( encObj != null ) {
      this.comboEncoding.setSelectedItem( encObj );
    }

    String lineEnd = this.editText.getLineEnd();
    if( lineEnd == null ) {
      lineEnd = System.getProperty( "line.separator" );
    }
    if( lineEnd != null ) {
      this.comboLineEnd.setSelectedItem( new TextLineSeparator( lineEnd ) );
    }
  }


  public static boolean saveFile(
				EditText             editText,
				Collection<EditText> allTexts,
				boolean              askFileName )
							throws IOException
  {
    boolean saved = false;

    File file = editText.getFile();
    if( (file != null) && !askFileName ) {
      editText.saveFile(
			editText.getEditFrm(),
			file,
			editText.getCharConverter(),
			editText.getEncodingName(),
			editText.getEncodingDisplayText(),
			editText.getTrimLines(),
			editText.getLineEnd() );
      saved = true;
      Main.setLastFile( file, "text" );
    } else {
      File preSelection = editText.getFile();

      file = EmuUtil.showFileSaveDlg(
		editText.getEditFrm(),
		"Textdatei speichern",
		preSelection != null ?
			 preSelection : Main.getLastPathFile( "text" ),
		EditFrm.getTextFileFilters() );
      if( file != null ) {

	/*
         * pruefen, ob im Editor bereits eine andere Datei
	 * mit dem Namen geoeffnet ist
	 */
	if( allTexts != null ) {
	  for( EditText tmpTxt : allTexts ) {
	    if( (tmpTxt != editText) && tmpTxt.isSameFile( file ) ) {
	      BasicDlg.showInfoDlg(
		editText.getEditFrm(),
		"Diese Datei ist bereits ge\u00F6ffnet.\n"
			+ "Bitte w\u00E4hlen Sie einen anderen Dateinamen.",
		"Hinweis" );
	      return false;
	    }
	  }
	}

	// eigentliches Speichern
	SaveTextDlg dlg = new SaveTextDlg( file, editText );
	dlg.setVisible( true );
	saved = dlg.fileSaved();
	Main.setLastFile( file, "text" );
      }
    }
    return saved;
  }


  public boolean fileSaved()
  {
    return this.fileSaved;
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnSave ) {
	rv = true;
	doSave();
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doSave()
  {
    try {

      // Zeichendsatz ermitteln
      CharConverter charConverter       = null;
      String        encodingName        = null;
      String        encodingDisplayText = null;
      Object        encodingObj         = this.comboEncoding.getSelectedItem();
      if( encodingObj != null ) {
	encodingDisplayText = encodingObj.toString();
	if( encodingObj instanceof CharConverter ) {
	  charConverter = (CharConverter) encodingObj;
	  encodingName  = charConverter.getEncodingName();
	} else {
	  String s = encodingObj.toString();
	  if( s != null ) {
	    if( s.startsWith( "ISO" ) || s.startsWith( "UTF" ) ) {
	      int posSpace = s.indexOf( '\u0020' );
	      encodingName = (posSpace > 0 ? s.substring( 0, posSpace ) : s);
	    }
	  }
	}
      }

      // Zeilenendebytes
      String lineEnd    = "\r\n";
      Object lineEndObj = this.comboLineEnd.getSelectedItem();
      if( lineEndObj != null ) {
	if( lineEndObj instanceof TextLineSeparator )
	  lineEnd = ((TextLineSeparator) lineEndObj).getLineEnd();
      }

      // Datei schreiben
      this.editText.saveFile(
			this,
			this.file,
			charConverter,
			encodingName,
			encodingDisplayText,
			this.btnTrimLines.isSelected(),
			lineEnd );
      this.fileSaved = true;
      doClose();
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
    catch( IOException ex ) {
      showErrorDlg(
	this,
	"Datei \'" + this.file.getPath()
		+ "\'\nkann nicht gespeichert werden.\n\n"
		+ ex.getMessage() );
    }
  }
}

