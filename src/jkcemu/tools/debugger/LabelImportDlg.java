/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Importieren von Marken in den Debugger
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class LabelImportDlg extends BaseDlg
{
  private DebugFrm     debugFrm;
  private JRadioButton btnSourceClipboard;
  private JRadioButton btnSourceFile;
  private FileNameFld  fileNameFld;
  private JCheckBox    btnSuppressRecreateRemovedLabels;
  private JCheckBox    btnRemoveObsoleteLabels;
  private JCheckBox    btnCaseSensitive;
  private JButton      btnFileSelect;
  private JButton      btnImport;
  private JButton      btnCancel;


  public static void showDlg(
			DebugFrm           debugFrm,
			LabelImportOptions options )
  {
    (new LabelImportDlg( debugFrm, options )).setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnSourceClipboard)
	  || (src == this.btnSourceFile) )
      {
	updFieldsEnabled();
	rv = true;
      }
      else if( src == this.btnFileSelect ) {
	doFileSelect();
	rv = true;
      }
      else if( src == this.btnImport ) {
	doImport();
	rv = true;
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private LabelImportDlg(
		DebugFrm           debugFrm,
		LabelImportOptions options )
  {
    super( debugFrm, "Halte-/Log-Punkte importieren" );
    this.debugFrm = debugFrm;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Bereich Quelle
    JPanel panelSource = new JPanel( new GridBagLayout() );
    panelSource.setBorder( BorderFactory.createTitledBorder( "Quelle" ) );
    add( panelSource, gbc );

    GridBagConstraints gbcSource = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    ButtonGroup grpSource = new ButtonGroup();

    this.btnSourceClipboard = new JRadioButton(
			"Halte-/Log-Punkte aus Zwischenablage importieren",
			true );
    grpSource.add( this.btnSourceClipboard );
    panelSource.add( this.btnSourceClipboard, gbcSource );

    this.btnSourceFile = new JRadioButton(
			"Halte-/Log-Punkte aus Datei importieren",
			false );
    grpSource.add( this.btnSourceFile );
    gbcSource.insets.bottom = 0;
    gbcSource.gridy++;
    panelSource.add( this.btnSourceFile, gbcSource );

    this.fileNameFld        = new FileNameFld();
    gbcSource.weightx       = 1.0;
    gbcSource.fill          = GridBagConstraints.HORIZONTAL;
    gbcSource.insets.top    = 0;
    gbcSource.insets.left   = 50;
    gbcSource.insets.bottom = 5;
    gbcSource.gridwidth     = 1;
    gbcSource.gridy++;
    panelSource.add( this.fileNameFld, gbcSource );

    this.btnFileSelect = createImageButton(
				"/images/file/open.png",
				"Datei ausw\u00E4hlen" );
    gbcSource.weightx     = 0.0;
    gbcSource.fill        = GridBagConstraints.NONE;
    gbcSource.insets.left = 5;
    gbcSource.gridx++;
    panelSource.add( this.btnFileSelect, gbcSource );


    // Bereich Optionen
    JPanel panelOptions = new JPanel();
    panelOptions.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
    add( panelOptions, gbc );

    panelOptions.setLayout(
		new BoxLayout( panelOptions, BoxLayout.Y_AXIS ) );

    this.btnSuppressRecreateRemovedLabels = new JCheckBox(
		"Manuell entfernte Halte-/Log-Punkte nicht erneut anlegen",
		true );
    this.btnSuppressRecreateRemovedLabels.setAlignmentX(
						Component.LEFT_ALIGNMENT );
    panelOptions.add( this.btnSuppressRecreateRemovedLabels );

    this.btnRemoveObsoleteLabels = new JCheckBox(
		"Bereits vorher importierte Halte-/Log-Punkte entfernen",
		true );
    this.btnRemoveObsoleteLabels.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.btnRemoveObsoleteLabels );

    this.btnCaseSensitive = new JCheckBox(
		"Gro\u00DF/-Kleinschreibung bei Namen beachten",
		false );
    this.btnCaseSensitive.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.btnCaseSensitive );


    // Bereich Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnImport = new JButton( "Importieren" );
    panelBtn.add( this.btnImport );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Vorbelegungen
    if( options != null ) {
      switch( options.getLabelSource() ) {
	case CLIPBOARD:
	  this.btnSourceClipboard.setSelected( true );
	  break;
	case FILE:
	  this.btnSourceFile.setSelected( true );
	  break;
      }
      this.fileNameFld.setFile( options.getFile() );
      this.btnSuppressRecreateRemovedLabels.setSelected(
			options.getSuppressRecreateRemovedLabels() );
      this.btnRemoveObsoleteLabels.setSelected(
			options.getRemoveObsoleteLabels() );
      this.btnCaseSensitive.setSelected(
			options.getCaseSensitive() );
    }
    updFieldsEnabled();


    // Listener
    this.btnSourceClipboard.addActionListener( this );
    this.btnSourceFile.addActionListener( this );
    this.btnImport.addActionListener( this );
    this.btnImport.addKeyListener( this );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
  }



	/* --- private Methoden --- */

  private void doFileSelect()
  {
    File oldFile = this.fileNameFld.getFile();
    File file    = EmuUtil.showFileOpenDlg(
		this,
		"Datei ausw\u00E4hlen",
		oldFile != null ?
			oldFile
			: Main.getLastDirFile( Main.FILE_GROUP_LABEL ) );
    if( file != null ) {
      String msg = null;
      if( file.exists() ) {
	if( file.canRead() ) {
	  this.fileNameFld.setFile( file );
	  Main.setLastFile( file, Main.FILE_GROUP_LABEL );
	} else {
	  msg = "Datei nicht lesbar";
	}
      } else {
	msg = "Datei nicht gefunden";
      }
      if( msg != null ) {
	showErrorDlg( this, msg );
      }
    }
  }


  private void doImport()
  {
    boolean isFileSource = this.btnSourceFile.isSelected();
    File    file         = this.fileNameFld.getFile();
    if( isFileSource && (file == null) ) {
      showErrorDlg(
		this,
		"Bei einem Import aus einer Datei m\u00FCssen\n"
			+ "Sie auch eine Datei ausw\u00E4hlen." );
    } else {
      if( this.debugFrm.importLabels(
		this,
		new LabelImportOptions(
			isFileSource ?
				LabelImportOptions.LabelSource.FILE
				: LabelImportOptions.LabelSource.CLIPBOARD,
			file,
			this.btnSuppressRecreateRemovedLabels.isSelected(),
			this.btnRemoveObsoleteLabels.isSelected(),
			this.btnCaseSensitive.isSelected() ) ) )
      {
	doClose();
      }
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = this.btnSourceFile.isSelected();
    this.fileNameFld.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
  }
}
