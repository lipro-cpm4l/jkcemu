/*
 * (c) 2008-2012 Jens Mueller
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
  private FileNameFld     fldFileName;
  private JLabel          labelFileName;
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
    updCodeToEmuFieldsVisible();
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
    options.setCodeToFile(
			this.btnCodeToFile.isSelected(),
			this.fldFileName.getFile() );
    options.setCodeToEmu(
		this.btnCodeToEmu != null ?
				this.btnCodeToEmu.isSelected()
				: false );
    options.setCodeToSecondSystem(
		this.btnCodeToSecondSys != null ?
				this.btnCodeToSecondSys.isSelected()
				: false );
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

    if( this.emuThread != null ) {
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

      gbc.gridy++;
    } else {
      this.btnCodeToEmu        = null;
      this.btnCodeToPrimarySys = null;
      this.btnCodeToSecondSys  = null;
    }

    this.btnCodeToFile = new JCheckBox( "Programmcode in Datei speichern" );
    this.btnCodeToFile.addActionListener( this );
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    panel.add( this.btnCodeToFile, gbc );

    JPanel panelFile = new JPanel( new GridBagLayout() );
    gbc.insets.left   = 50;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
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

    this.labelFileName = new JLabel( "Dateiname:" );
    panelFile.add( this.labelFileName, gbcFile );

    this.fldFileName = new FileNameFld();
    this.fldFileName.setEditable( false );
    gbcFile.fill    = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx = 1.0;
    gbcFile.gridx++;
    panelFile.add( this.fldFileName, gbcFile );

    this.btnFileSelect = createImageButton(
				"/images/file/open.png",
				"\u00D6ffnen..." );
    gbcFile.fill    = GridBagConstraints.NONE;
    gbcFile.weightx = 0.0;
    gbcFile.gridx++;
    panelFile.add( this.btnFileSelect, gbcFile );

    return panel;
  }


  protected abstract void doApply();


  protected void setCodeToEmu( boolean state )
  {
    if( this.btnCodeToEmu != null )
      this.btnCodeToEmu.setSelected( state );
  }


  protected void updCodeDestFields(
				PrgOptions options,
				boolean    forceCodeToEmu )
  {
    updCodeToEmuFieldsVisible();
    if( options != null ) {
      if( this.btnCodeToEmu != null ) {
	this.btnCodeToEmu.setSelected( options.getCodeToEmu() );
      }
      if( this.btnCodeToSecondSys != null ) {
	this.btnCodeToSecondSys.setSelected(
				options.getCodeToSecondSystem() );
      }
      this.btnCodeToFile.setSelected( options.getCodeToFile() );
      this.fldFileName.setFile( options.getCodeFile() );
    } else {
      if( this.btnCodeToEmu != null ) {
	this.btnCodeToEmu.setSelected( forceCodeToEmu );
      }
      this.btnCodeToFile.setSelected( false );
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
      else if( src == this.btnCodeToFile ) {
	rv = true;
	updCodeToFileFieldsEnabled();
      }
      else if( (src == this.fldFileName) || (src == this.btnFileSelect) ) {
	rv = true;
	doSelectFile();
      }
      else if( src == this.btnApply ) {
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
    }
  }


  private void updCodeToEmuFieldsVisible()
  {
    if( (this.btnCodeToEmu != null)
	&& (this.btnCodeToSecondSys != null)
	&& (this.btnCodeToPrimarySys != null) )
    {
      String secondarySysName = null;
      if( this.emuThread != null ) {
	EmuSys emuSys = this.emuThread.getEmuSys();
	if( emuSys != null ) {
	  secondarySysName = emuSys.getSecondSystemName();
	}
      }
      if( secondarySysName != null ) {
	this.btnCodeToEmu.setText( "Programmmcode in Emulator laden:" );
	this.btnCodeToSecondSys.setText( secondarySysName );
	this.btnCodeToSecondSys.setVisible( true );
	this.btnCodeToPrimarySys.setVisible( true );
      } else {
	this.btnCodeToEmu.setText( "Programmmcode in Emulator laden" );
	this.btnCodeToSecondSys.setVisible( false );
	this.btnCodeToPrimarySys.setVisible( false );
      }
    }
  }


  private void updCodeToEmuFieldsEnabled()
  {
    if( (this.btnCodeToEmu != null)
	&& (this.btnCodeToSecondSys != null)
	&& (this.btnCodeToPrimarySys != null) )
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
  }


  private void updCodeToFileFieldsEnabled()
  {
    boolean state = this.btnCodeToFile.isSelected();
    this.labelFileName.setEnabled( state );
    this.fldFileName.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
  }
}

