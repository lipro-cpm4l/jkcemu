/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Importieren von Marken in den Debugger
 */

package jkcemu.tools.debugger;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class LabelImportDlg extends BaseDlg
{
  private DebugFrm     debugFrm;
  private JRadioButton rbSourceClipboard;
  private JRadioButton rbSourceFile;
  private JRadioButton rbCreateOrUpdateBPs;
  private JRadioButton rbUpdateBPsOnly;
  private FileNameFld  fileNameFld;
  private JCheckBox    cbRemoveObsoleteLabels;
  private JCheckBox    cbCaseSensitive;
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
    if( (src == this.rbSourceClipboard)
	|| (src == this.rbSourceFile) )
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
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.rbSourceClipboard.removeActionListener( this );
      this.rbSourceFile.removeActionListener( this );
      this.btnFileSelect.removeActionListener( this );
      this.btnImport.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
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
    JPanel panelSource = GUIFactory.createPanel( new GridBagLayout() );
    panelSource.setBorder( GUIFactory.createTitledBorder( "Quelle" ) );
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

    this.rbSourceClipboard = GUIFactory.createRadioButton(
			"Halte-/Log-Punkte aus Zwischenablage importieren" );
    grpSource.add( this.rbSourceClipboard );
    panelSource.add( this.rbSourceClipboard, gbcSource );

    this.rbSourceFile = GUIFactory.createRadioButton(
			"Halte-/Log-Punkte aus Datei importieren" );
    grpSource.add( this.rbSourceFile );
    gbcSource.insets.bottom = 0;
    gbcSource.gridy++;
    panelSource.add( this.rbSourceFile, gbcSource );

    this.fileNameFld        = new FileNameFld();
    gbcSource.weightx       = 1.0;
    gbcSource.fill          = GridBagConstraints.HORIZONTAL;
    gbcSource.insets.top    = 0;
    gbcSource.insets.left   = 50;
    gbcSource.insets.bottom = 5;
    gbcSource.gridwidth     = 1;
    gbcSource.gridy++;
    panelSource.add( this.fileNameFld, gbcSource );

    this.btnFileSelect = GUIFactory.createRelImageResourceButton(
						this,
						"file/open.png",
						EmuUtil.TEXT_SELECT_FILE );
    gbcSource.weightx     = 0.0;
    gbcSource.fill        = GridBagConstraints.NONE;
    gbcSource.insets.left = 5;
    gbcSource.gridx++;
    panelSource.add( this.btnFileSelect, gbcSource );


    // Bereich Optionen
    JPanel panelOptions = GUIFactory.createPanel();
    panelOptions.setBorder( GUIFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
    add( panelOptions, gbc );

    panelOptions.setLayout(
		new BoxLayout( panelOptions, BoxLayout.Y_AXIS ) );

    ButtonGroup grpCreateBPs = new ButtonGroup();

    this.rbCreateOrUpdateBPs = GUIFactory.createRadioButton(
		"Halte-/Log-Punkte anlegen oder aktualisieren" );
    grpCreateBPs.add( this.rbCreateOrUpdateBPs );
    this.rbCreateOrUpdateBPs.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.rbCreateOrUpdateBPs );

    this.rbUpdateBPsOnly = GUIFactory.createRadioButton(
		"Nur vorhandene Halte-/Log-Punkte aktualisieren" );
    grpCreateBPs.add( this.rbUpdateBPsOnly );
    this.rbUpdateBPsOnly.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.rbUpdateBPsOnly );

    this.cbRemoveObsoleteLabels = GUIFactory.createCheckBox(
		"Bereits vorher importierte Halte-/Log-Punkte entfernen",
		true );
    this.cbRemoveObsoleteLabels.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.cbRemoveObsoleteLabels );

    this.cbCaseSensitive = GUIFactory.createCheckBox(
		"Gro\u00DF/-Kleinschreibung bei Namen beachten" );
    this.cbCaseSensitive.setAlignmentX( Component.LEFT_ALIGNMENT );
    panelOptions.add( this.cbCaseSensitive );


    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.bottom = 10;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnImport = GUIFactory.createButton( "Importieren" );
    panelBtn.add( this.btnImport );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Vorbelegungen
    if( options != null ) {
      switch( options.getLabelSource() ) {
	case CLIPBOARD:
	  this.rbSourceClipboard.setSelected( true );
	  break;
	case FILE:
	  this.rbSourceFile.setSelected( true );
	  break;
      }
      this.fileNameFld.setFile( options.getFile() );
      if( options.getUpdateBreakpointsOnly() ) {
	this.rbUpdateBPsOnly.setSelected( true );
      } else {
	this.rbCreateOrUpdateBPs.setSelected( true );
      }
      this.cbRemoveObsoleteLabels.setSelected(
			options.getRemoveObsoleteLabels() );
      this.cbCaseSensitive.setSelected(
			options.getCaseSensitive() );
    } else {
      this.rbSourceClipboard.setSelected( true );
      this.rbCreateOrUpdateBPs.setSelected( true );
    }
    updFieldsEnabled();


    // Listener
    this.rbSourceClipboard.addActionListener( this );
    this.rbSourceFile.addActionListener( this );
    this.btnFileSelect.addActionListener( this );
    this.btnImport.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }



	/* --- private Methoden --- */

  private void doFileSelect()
  {
    File file = this.fileNameFld.getFile();
    if( file == null ) {
      file = Main.getLastDirFile( Main.FILE_GROUP_LABEL );
    }
    file = FileUtil.showFileOpenDlg(
			this,
			EmuUtil.TEXT_SELECT_FILE,
			FileUtil.getDirectory( file ) );
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
    boolean isFileSource = this.rbSourceFile.isSelected();
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
			this.cbCaseSensitive.isSelected(),
			this.rbUpdateBPsOnly.isSelected(),
			this.cbRemoveObsoleteLabels.isSelected() ) ) )
      {
	doClose();
      }
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = this.rbSourceFile.isSelected();
    this.fileNameFld.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
  }
}
