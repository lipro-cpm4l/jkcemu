/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Grundgeruest fuer einen Optionen-Dialog
 */

package jkcemu.programming;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.*;
import jkcemu.programming.PrgOptions;


public abstract class AbstractOptionsDlg extends BasicDlg
{
  protected PrgOptions appliedOptions;
  protected EmuThread  emuThread;

  private static String[] fileFmtItems = {
				FileSaver.KCC,
				FileSaver.KCTAP_0,
				FileSaver.KCTAP_1,
				FileSaver.HEADERSAVE,
				FileSaver.INTELHEX,
				FileSaver.BIN };

  private Frame owner;

  /*
   * Attribute sind nur gesetzt,
   * wenn die entsprechenden add...-Methoden aufgerufen werden
   */
  private JCheckBox       btnCodeToEmu;
  private JCheckBox       btnCodeToFile;
  private JRadioButton    btnCodeToPrimarySys;
  private JRadioButton    btnCodeToSecondSys;
  private JComboBox       comboFileFmt;
  private FileNameFld     fldFileName;
  private JTextField      fldFileType;
  private JTextField      fldFileDesc;
  private JLabel          labelFileFmt;
  private JLabel          labelFileName;
  private JLabel          labelFileType;
  private JLabel          labelFileDesc;
  private JButton         btnFileSelect;
  private JButton         btnApply;
  private JButton         btnCancel;
  private LimitedDocument docFileDesc;


  public PrgOptions getAppliedOptions()
  {
    return this.appliedOptions;
  }


  public void settingsChanged()
  {
    updCodeToEmuFields();
    updCodeToEmuFieldsEnabled();
  }


	/* --- geschuetzte Konstruktoren und Methoden --- */

  protected AbstractOptionsDlg(
			Frame     owner,
			EmuThread emuThread,
			String    title )
  {
    super( owner, title );
    this.owner          = owner;
    this.emuThread      = emuThread;
    this.appliedOptions = null;
  }


  protected void applyCodeDestOptionsTo( PrgOptions options )
						throws UserInputException
  {
    char fileType = '\u0020';
    String text = this.fldFileType.getText();
    if( text != null ) {
      if( text.length() > 0 ) {
	fileType = text.charAt( 0 );
	if( (fileType < '\u0020') || (fileType >= '\u007E') ) {
	  fileType = '\u0020';
	}
      }
    }
    FileSaver.checkFileDesc(
			getSelectedFileFmt(),
			false,
			this.fldFileDesc.getText() );
    options.setCodeToFile(
			this.btnCodeToFile.isSelected(),
			this.fldFileName.getFile(),
			getSelectedFileFmt(),
			fileType,
			this.fldFileDesc.getText() );
    options.setCodeToEmu( this.btnCodeToEmu.isSelected() );
    options.setCodeToSecondSystem( this.btnCodeToSecondSys.isSelected() );
  }


  protected void codeToEmuChanged( boolean state )
  {
    // leer
  }


  protected JPanel createButtons( String applyText )
  {
    JPanel panel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = new JButton( applyText );
    this.btnApply.addActionListener( this );
    this.btnApply.addKeyListener( this );
    panel.add( this.btnApply );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panel.add( this.btnCancel );

    return panel;
  }


  protected JPanel createCodeDestOptions()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnCodeToEmu = new JCheckBox();
    this.btnCodeToEmu.addActionListener( this );
    panel.add( this.btnCodeToEmu, gbc );

    ButtonGroup grpCodeToEmu = new ButtonGroup();

    this.btnCodeToPrimarySys = new JRadioButton( "Grundger\u00E4t", true );
    grpCodeToEmu.add( this.btnCodeToPrimarySys );
    gbc.insets.left = 20;
    gbc.gridx++;
    panel.add( this.btnCodeToPrimarySys, gbc );

    this.btnCodeToSecondSys = new JRadioButton();
    grpCodeToEmu.add( this.btnCodeToSecondSys );
    gbc.insets.left = 5;
    gbc.gridx++;
    panel.add( this.btnCodeToSecondSys, gbc );

    this.btnCodeToFile = new JCheckBox( "Programmcode in Datei speichern" );
    this.btnCodeToFile.addActionListener( this );
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    panel.add( this.btnCodeToFile, gbc );

    JPanel panelFile = new JPanel( new GridBagLayout() );
    gbc.insets.left   = 50;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    panel.add( panelFile, gbc );

    GridBagConstraints gbcFile = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 2, 2, 2, 2 ),
					0, 0 );

    this.labelFileFmt = new JLabel( "Dateiformat:" );
    gbcFile.gridwidth = 1;
    gbcFile.gridy++;
    panelFile.add( this.labelFileFmt, gbcFile );

    this.comboFileFmt = new JComboBox();
    this.comboFileFmt.setEditable( false );
    for( int i = 0; i < fileFmtItems.length; i++ ) {
      this.comboFileFmt.addItem(
		FileSaver.getFormatText( fileFmtItems[ i ] ) );
    }
    this.comboFileFmt.addItem( "CP/M-Programmdatei" );
    this.comboFileFmt.addActionListener( this );
    gbcFile.gridwidth = 3;
    gbcFile.gridx++;
    panelFile.add( this.comboFileFmt, gbcFile );

    this.labelFileName = new JLabel( "Dateiname:" );
    gbcFile.gridwidth  = 1;
    gbcFile.gridx      = 0;
    gbcFile.gridy++;
    panelFile.add( this.labelFileName, gbcFile );

    this.fldFileName = new FileNameFld();
    this.fldFileName.setEditable( false );
    gbcFile.fill      = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx   = 1.0;
    gbcFile.gridwidth = 3;
    gbcFile.gridx++;
    panelFile.add( this.fldFileName, gbcFile );

    this.btnFileSelect = createImageButton(
				"/images/file/open.png",
				"\u00D6ffnen..." );
    gbcFile.fill      = GridBagConstraints.NONE;
    gbcFile.weightx   = 0.0;
    gbcFile.gridwidth = 1;
    gbcFile.gridx += 3;
    panelFile.add( this.btnFileSelect, gbcFile );

    this.labelFileType = new JLabel( "Dateityp:" );
    gbcFile.gridx = 0;
    gbcFile.gridy++;
    panelFile.add( this.labelFileType, gbcFile );

    this.fldFileType = new JTextField( new LimitedDocument( 1 ), "", 2 );
    gbcFile.gridx ++;
    panelFile.add( this.fldFileType, gbcFile );

    this.labelFileDesc = new JLabel( "Beschreibung:" );
    gbcFile.gridx++;
    panelFile.add( this.labelFileDesc, gbcFile );

    this.docFileDesc = new LimitedDocument( FileSaver.getMaxFileDescLength() );
    this.fldFileDesc = new JTextField( this.docFileDesc, "", 0 );
    gbcFile.fill    = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx = 1.0;
    gbcFile.gridx++;
    panelFile.add( this.fldFileDesc, gbcFile );

    return panel;
  }


  protected abstract void doApply();


  protected void updCodeDestFields( PrgOptions options )
  {
    updCodeToEmuFields();
    if( options != null ) {
      this.btnCodeToEmu.setSelected( options.getCodeToEmu() );
      this.btnCodeToSecondSys.setSelected( options.getCodeToSecondSystem() );
      this.btnCodeToFile.setSelected( options.getCodeToFile() );
      this.fldFileName.setFile( options.getCodeFile() );

      String fileFmt = options.getCodeFileFormat();
      if( fileFmt != null ) {
	String text = FileSaver.getFormatText( fileFmt );
	if( text != null ) {
	  this.comboFileFmt.setSelectedItem( text );
	}
      }
      char fileType = options.getCodeFileType();
      if( (fileType > '\u0020') && (fileType <= '\u007E') ) {
	this.fldFileType.setText( String.valueOf( fileType ) );
      }
      String fileDesc = options.getCodeFileDesc();
      if( fileDesc != null ) {
        this.fldFileDesc.setText( fileDesc );
      }
    } else {
      this.btnCodeToEmu.setSelected( false );
      this.btnCodeToFile.setSelected( false );

      EmuSys emuSys = this.emuThread.getEmuSys();
      if( (emuSys instanceof AC1) || (emuSys instanceof Z1013) ) {
	this.comboFileFmt.setSelectedItem(
			FileSaver.getFormatText( FileSaver.HEADERSAVE ) );
      } else {
	this.comboFileFmt.setSelectedItem(
			FileSaver.getFormatText( FileSaver.KCC ) );
      }
      this.fldFileType.setText( "C" );
    }
    updCodeToEmuFieldsEnabled();
    updCodeToFileFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnCodeToEmu ) {
	rv = true;
	updCodeToEmuFieldsEnabled();
      }
      else if( (src == this.btnCodeToFile) || (src == this.comboFileFmt) ) {
	rv = true;
	updCodeToFileFieldsEnabled();
      }
      else if( (src == this.fldFileName) || (src == this.btnFileSelect) ) {
	rv = true;
	doSelectFile();
      }
      else if( src == this.fldFileType ) {
	rv = true;
	this.fldFileDesc.requestFocus();
      }
      else if( (src == this.fldFileDesc) || (src == this.btnApply) ) {
	rv = true;
	doApply();
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doSelectFile()
  {
    File file = EmuUtil.showFileSaveDlg(
				this.owner,
				"Programmcode speichern",
				this.fldFileName.getFile(),
				EmuUtil.getKCSystemFileFilter(),
				EmuUtil.getTapFileFilter(),
				EmuUtil.getHeadersaveFileFilter(),
				EmuUtil.getHexFileFilter(),
				EmuUtil.getBinaryFileFilter(),
				EmuUtil.getComFileFilter() );
    if( file != null ) {
      this.fldFileName.setFile( file );
      this.fldFileType.requestFocus();
    }
  }


  private String getSelectedFileFmt()
  {
    int idx = this.comboFileFmt.getSelectedIndex();
    return (idx >= 0) && (idx < fileFmtItems.length) ?
					fileFmtItems[ idx ]
					: FileSaver.BIN;
  }


  public void updCodeToEmuFields()
  {
    String secondarySysName = null;
    EmuSys emuSys           = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      secondarySysName = emuSys.getSecondSystemName();
    }
    if( secondarySysName != null ) {
      this.btnCodeToEmu.setText( "Programmmcode in Emulator laden:" );
      btnCodeToSecondSys.setText( secondarySysName );
      btnCodeToSecondSys.setVisible( true );
      btnCodeToPrimarySys.setVisible( true );
    } else {
      this.btnCodeToEmu.setText( "Programmmcode in Emulator laden" );
      btnCodeToSecondSys.setVisible( false );
      btnCodeToPrimarySys.setVisible( false );
    }
  }


  private void updCodeToEmuFieldsEnabled()
  {
    boolean state = this.btnCodeToEmu.isSelected();
    if( btnCodeToPrimarySys.isVisible() ) {
      btnCodeToPrimarySys.setEnabled( state );
    }
    if( btnCodeToSecondSys.isVisible() ) {
      btnCodeToSecondSys.setEnabled( state );
    }
    codeToEmuChanged( this.btnCodeToEmu.isSelected() );
  }


  private void updCodeToFileFieldsEnabled()
  {
    boolean state = this.btnCodeToFile.isSelected();
    this.labelFileFmt.setEnabled( state );
    this.comboFileFmt.setEnabled( state );
    this.labelFileName.setEnabled( state );
    this.fldFileName.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
    if( state ) {
      String fileFmt = getSelectedFileFmt();

      state = fileFmt.equals( FileSaver.HEADERSAVE );
      this.labelFileType.setEnabled( state );
      this.fldFileType.setEnabled( state );

      state = state
		|| fileFmt.equals( FileSaver.KCC )
		|| fileFmt.equals( FileSaver.KCTAP_0 )
		|| fileFmt.equals( FileSaver.KCTAP_1 );
      this.labelFileDesc.setEnabled( state );
      this.fldFileDesc.setEnabled( state );
      this.docFileDesc.setMaxLength(
		FileSaver.getMaxFileDescLength( fileFmt, false ) );
    } else {
      this.labelFileType.setEnabled( state );
      this.fldFileType.setEnabled( state );
      this.labelFileDesc.setEnabled( state );
      this.fldFileDesc.setEnabled( state );
    }
  }
}

