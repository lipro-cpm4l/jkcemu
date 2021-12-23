/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Textdatei speichern
 */

package jkcemu.text;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileUtil;


public class SaveTextDlg extends BaseDlg
{
  private JCheckBox         cbTrailing1A;
  private JCheckBox         cbTrimLines;
  private JComboBox<Object> comboEncoding;
  private JComboBox<Object> comboLineEnd;
  private JButton           btnSave;
  private JButton           btnCancel;
  private File              file;
  private EditText          editText;
  private boolean           fileSaved;
  private boolean           notified;


  public SaveTextDlg( File file, EditText editText )
  {
    super(
	editText.getTextEditFrm(),
	"Textdatei speichern: " + file.getName() );

    this.file      = file;
    this.editText  = editText;
    this.fileSaved = false;
    this.notified  = false;


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
    JPanel panelFileName = GUIFactory.createPanel(
		new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelFileName.setBorder( GUIFactory.createTitledBorder( "Dateiname" ) );
    panelFileName.add( GUIFactory.createLabel(
					fileName != null ? fileName : "" ) );
    add( panelFileName, gbc );


    // Bereich Eigenschaften
    JPanel panelProp = GUIFactory.createPanel( new GridBagLayout() );
    panelProp.setBorder( GUIFactory.createTitledBorder( "Eigenschaften" ) );
    gbc.gridy++;
    add( panelProp, gbc );

    GridBagConstraints gbcProp = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panelProp.add( GUIFactory.createLabel( "Zeichensatz:" ), gbcProp );

    this.comboEncoding = GUIFactory.createComboBox();
    this.comboEncoding.addItem( "Systemzeichensatz" );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.ASCII ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.ISO646DE ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP437 ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP850 ) );
    this.comboEncoding.addItem( new CharConverter(
		CharConverter.Encoding.LATIN1 ) );
    this.comboEncoding.addItem( "UTF-8" );
    this.comboEncoding.addItem( "UTF-8" + EditText.TEXT_WITH_BOM );
    this.comboEncoding.addItem( "UTF-16" + EditText.TEXT_WITH_BOM );
    this.comboEncoding.addItem( "UTF-16BE" );
    this.comboEncoding.addItem( "UTF-16BE" + EditText.TEXT_WITH_BOM );
    this.comboEncoding.addItem( "UTF-16LE" );
    this.comboEncoding.addItem( "UTF-16LE" + EditText.TEXT_WITH_BOM );
    this.comboEncoding.setEditable( false );
    gbcProp.anchor = GridBagConstraints.WEST;
    gbcProp.gridx++;
    panelProp.add( this.comboEncoding, gbcProp );

    gbcProp.anchor = GridBagConstraints.EAST;
    gbcProp.gridx  = 0;
    gbcProp.gridy++;
    panelProp.add( GUIFactory.createLabel( "Zeilenende:" ), gbcProp );

    this.comboLineEnd = GUIFactory.createComboBox();
    this.comboLineEnd.addItem( new TextLineSeparator( "\r\n" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\n" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\r" ) );
    this.comboLineEnd.addItem( new TextLineSeparator( "\u001E" ) );
    gbcProp.anchor = GridBagConstraints.WEST;
    gbcProp.gridx++;
    panelProp.add( this.comboLineEnd, gbcProp );

    this.cbTrimLines = GUIFactory.createCheckBox(
		"Unsichtbare Zeichen am Zeilenende entfernen" );
    gbcProp.gridy++;
    panelProp.add( this.cbTrimLines, gbcProp );

    this.cbTrailing1A = GUIFactory.createCheckBox(
		"Datei mit Byte 1Ah abschlie\u00DFen" );
    gbcProp.insets.top    = 0;
    gbcProp.insets.bottom = 5;
    gbcProp.gridy++;
    panelProp.add( this.cbTrailing1A, gbcProp );


    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 2, 1, 5, 5 ) );

    this.btnSave = GUIFactory.createButton( EmuUtil.TEXT_SAVE );
    panelBtn.add( this.btnSave );

    this.btnCancel = GUIFactory.createButtonCancel();
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
    this.cbTrailing1A.setSelected( this.editText.getEofByte() == 0x1A );
    this.cbTrimLines.setSelected( this.editText.getTrimLines() );

    Object encObj = editText.getCharConverter();
    if( encObj == null ) {
      encObj = editText.getEncodingDescription();
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
			editText.getTextEditFrm(),
			file,
			editText.getCharConverter(),
			editText.getEncodingName(),
			editText.getEncodingDescription(),
			editText.hasByteOrderMark(),
			editText.getEofByte(),
			editText.getTrimLines(),
			editText.getLineEnd() );
      saved = true;
      Main.setLastFile( file, Main.FILE_GROUP_TEXT );
    } else {
      File preSelection = editText.getFile();
      if( preSelection == null ) {
	String fileName = editText.getFileName();
	if( fileName != null ) {
	  preSelection = new File( fileName );
	} else {
	  preSelection = Main.getLastDirFile( Main.FILE_GROUP_TEXT );
	}
      }
      file = FileUtil.showFileSaveDlg(
				editText.getTextEditFrm(),
				"Textdatei speichern",
				preSelection,
				TextEditFrm.getTextFileFilters() );
      if( file != null ) {

	/*
         * pruefen, ob im Editor bereits eine andere Datei
	 * mit dem Namen geoeffnet ist
	 */
	if( allTexts != null ) {
	  for( EditText tmpTxt : allTexts ) {
	    if( (tmpTxt != editText) && tmpTxt.isSameFile( file ) ) {
	      showInfoDlg(
		editText.getTextEditFrm(),
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
	Main.setLastFile( file, Main.FILE_GROUP_TEXT );
      }
    }
    return saved;
  }


  public boolean fileSaved()
  {
    return this.fileSaved;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.btnSave.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
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


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.btnSave.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
  }


	/* --- private Methoden --- */

  private void doSave()
  {
    try {

      // Zeichensatz ermitteln
      CharConverter charConverter = null;
      String        encodingName  = null;
      String        encodingDesc  = null;
      boolean       byteOrderMark = false;
      Object        encodingObj   = this.comboEncoding.getSelectedItem();
      if( encodingObj != null ) {
	encodingDesc = encodingObj.toString();
	if( encodingObj instanceof CharConverter ) {
	  charConverter = (CharConverter) encodingObj;
	  encodingName  = charConverter.getEncodingName();
	} else {
	  if( encodingDesc != null ) {
	    if( encodingDesc.startsWith( "ISO" )
		|| encodingDesc.startsWith( "UTF" ) )
	    {
	      int posSpace = encodingDesc.indexOf( '\u0020' );
	      encodingName = (posSpace > 0 ?
				encodingDesc.substring( 0, posSpace )
				: encodingDesc);
	    }
	    if( encodingDesc.indexOf( EditText.TEXT_WITH_BOM ) >= 0 ) {
	      byteOrderMark = true;
	    }
	  }
	}
      }

      // Zeilenendebytes
      String lineEnd    = "\r\n";
      Object lineEndObj = this.comboLineEnd.getSelectedItem();
      if( lineEndObj != null ) {
	if( lineEndObj instanceof TextLineSeparator ) {
	  lineEnd = ((TextLineSeparator) lineEndObj).getLineEnd();
	}
      }

      // Datei schreiben
      this.editText.saveFile(
			this,
			this.file,
			charConverter,
			encodingName,
			encodingDesc,
			byteOrderMark,
			this.cbTrailing1A.isSelected() ? 0x1A : -1,
			this.cbTrimLines.isSelected(),
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
