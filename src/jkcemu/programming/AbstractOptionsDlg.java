/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Grundgeruest fuer einen Optionen-Dialog
 */

package jkcemu.programming;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.programming.PrgOptions;


public abstract class AbstractOptionsDlg extends BaseDlg
{
  protected PrgOptions oldOptions;
  protected PrgOptions appliedOptions;
  protected EmuThread  emuThread;

  private static final String TEXT_LOAD_INTO_EMU_WITH
				= "Programmmcode in Emulator laden:";

  private static final String TEXT_LOAD_INTO_EMU
				= "Programmmcode in Emulator laden";

  private Frame   owner;
  private boolean notified;

  /*
   * Attribute sind nur gesetzt,
   * wenn die entsprechenden create...-Methoden aufgerufen werden
   */
  private JCheckBox    cbCodeToEmu;
  private JCheckBox    cbCodeToFile;
  private JRadioButton rbCodeToPrimarySys;
  private JRadioButton rbCodeToSecondSys;
  private FileNameFld  fldFileName;
  private JLabel       labelFileName;
  private JButton      btnFileSelect;
  private JButton      btnApply;
  private JButton      btnCancel;


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
			Frame      owner,
			EmuThread  emuThread,
			PrgOptions options,
			String     title )
  {
    super( owner, title );
    this.owner              = owner;
    this.emuThread          = emuThread;
    this.oldOptions         = options;
    this.notified           = false;
    this.appliedOptions     = null;
    this.cbCodeToEmu        = null;
    this.cbCodeToFile       = null;
    this.rbCodeToPrimarySys = null;
    this.rbCodeToSecondSys  = null;
    this.fldFileName        = null;
    this.labelFileName      = null;
    this.btnFileSelect      = null;
    this.btnApply           = null;
    this.btnCancel          = null;
  }


  protected void applyCodeDestOptionsTo( PrgOptions options )
						throws UserInputException
  {
    options.setCodeToFile(
			this.cbCodeToFile.isSelected(),
			this.fldFileName.getFile() );
    if( this.cbCodeToEmu != null ) {
      options.setCodeToEmu( this.cbCodeToEmu.isSelected() );
    }
    if( this.rbCodeToSecondSys != null ) {
      options.setCodeToSecondSystem( this.rbCodeToSecondSys.isSelected() );
    }
  }


  protected void codeToEmuChanged( boolean state )
  {
    // leer
  }


  protected JPanel createButtons( String applyText )
  {
    JPanel panel = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = GUIFactory.createButton( applyText );
    panel.add( this.btnApply );

    this.btnCancel = GUIFactory.createButtonCancel();
    panel.add( this.btnCancel );

    return panel;
  }


  protected JPanel createCodeDestOptions( boolean enableSecondSysBtns )
  {
    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    if( this.emuThread != null ) {
      this.cbCodeToEmu = GUIFactory.createCheckBox( TEXT_LOAD_INTO_EMU );
      panel.add( this.cbCodeToEmu, gbc );

      if( enableSecondSysBtns ) {
	this.cbCodeToEmu.setText( TEXT_LOAD_INTO_EMU_WITH );

	ButtonGroup grpCodeToEmu = new ButtonGroup();

	this.rbCodeToPrimarySys = GUIFactory.createRadioButton(
						"Grundger\u00E4t",
						true );
	grpCodeToEmu.add( this.rbCodeToPrimarySys );
	gbc.insets.left = 20;
	gbc.gridx++;
	panel.add( this.rbCodeToPrimarySys, gbc );

	this.rbCodeToSecondSys = GUIFactory.createRadioButton();
	grpCodeToEmu.add( this.rbCodeToSecondSys );
	gbc.insets.left = 5;
	gbc.gridx++;
	panel.add( this.rbCodeToSecondSys, gbc );
      }
      gbc.gridy++;
    } else {
      this.cbCodeToEmu        = null;
      this.rbCodeToPrimarySys = null;
      this.rbCodeToSecondSys  = null;
    }

    this.cbCodeToFile = GUIFactory.createCheckBox(
					"Programmcode in Datei speichern" );
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    panel.add( this.cbCodeToFile, gbc );

    JPanel panelFile = GUIFactory.createPanel( new GridBagLayout() );
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

    this.labelFileName = GUIFactory.createLabel( "Dateiname:" );
    panelFile.add( this.labelFileName, gbcFile );

    this.fldFileName = new FileNameFld();
    this.fldFileName.setEditable( false );
    gbcFile.fill    = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx = 1.0;
    gbcFile.gridx++;
    panelFile.add( this.fldFileName, gbcFile );

    this.btnFileSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_OPEN_OPEN );
    gbcFile.fill    = GridBagConstraints.NONE;
    gbcFile.weightx = 0.0;
    gbcFile.gridx++;
    panelFile.add( this.btnFileSelect, gbcFile );

    return panel;
  }


  protected abstract void doApply();


  protected void setCodeToEmu( boolean state )
  {
    if( this.cbCodeToEmu != null )
      this.cbCodeToEmu.setSelected( state );
  }


  protected void updCodeDestFields(
				PrgOptions options,
				boolean    forceCodeToEmu )
  {
    updCodeToEmuFieldsVisible();
    if( options != null ) {
      if( this.cbCodeToEmu != null ) {
	this.cbCodeToEmu.setSelected( options.getCodeToEmu() );
      }
      if( this.rbCodeToSecondSys != null ) {
	this.rbCodeToSecondSys.setSelected(
				options.getCodeToSecondSystem() );
      }
      this.cbCodeToFile.setSelected( options.getCodeToFile() );
      this.fldFileName.setFile( options.getCodeFile() );
    } else {
      if( this.cbCodeToEmu != null ) {
	this.cbCodeToEmu.setSelected( forceCodeToEmu );
      }
      this.cbCodeToFile.setSelected( false );
    }
    updCodeToEmuFieldsEnabled();
    updCodeToFileFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      if( this.cbCodeToEmu != null ) {
	this.cbCodeToEmu.addActionListener( this );
      }
      if( this.cbCodeToFile != null ) {
	this.cbCodeToFile.addActionListener( this );
      }
      if( this.btnFileSelect != null ) {
	this.btnFileSelect.addActionListener( this );
      }
      if( this.btnApply != null ) {
	this.btnApply.addActionListener( this );
      }
      if( this.btnCancel != null ) {
	this.btnCancel.addActionListener( this );
      }
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    Object src = e.getSource();
    if( src == this.cbCodeToEmu ) {
      rv = true;
      updCodeToEmuFieldsEnabled();
    }
    else if( src == this.cbCodeToFile ) {
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
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      if( this.cbCodeToEmu != null ) {
	this.cbCodeToEmu.removeActionListener( this );
      }
      if( this.cbCodeToFile != null ) {
	this.cbCodeToFile.removeActionListener( this );
      }
      if( this.btnFileSelect != null ) {
	this.btnFileSelect.removeActionListener( this );
      }
      if( this.btnApply != null ) {
	this.btnApply.removeActionListener( this );
      }
      if( this.btnCancel != null ) {
	this.btnCancel.removeActionListener( this );
      }
    }
  }


	/* --- private Methoden --- */

  private void doSelectFile()
  {
    File file = FileUtil.showFileSaveDlg(
				this.owner,
				"Programmcode speichern",
				this.fldFileName.getFile(),
				FileUtil.getKCSystemFileFilter(),
				FileUtil.getKCTapFileFilter(),
				FileUtil.getHeadersaveFileFilter(),
				FileUtil.getHexFileFilter(),
				FileUtil.getBinaryFileFilter(),
				FileUtil.getComFileFilter() );
    if( file != null ) {
      this.fldFileName.setFile( file );
    }
  }


  private void updCodeToEmuFieldsVisible()
  {
    if( (this.cbCodeToEmu != null)
	&& (this.rbCodeToSecondSys != null)
	&& (this.rbCodeToPrimarySys != null) )
    {
      String secondarySysName = null;
      if( this.emuThread != null ) {
	EmuSys emuSys = this.emuThread.getEmuSys();
	if( emuSys != null ) {
	  secondarySysName = emuSys.getSecondSystemName();
	}
      }
      if( secondarySysName != null ) {
	this.cbCodeToEmu.setText( TEXT_LOAD_INTO_EMU_WITH );
	this.rbCodeToSecondSys.setText( secondarySysName );
	this.rbCodeToSecondSys.setVisible( true );
	this.rbCodeToPrimarySys.setVisible( true );
      } else {
	this.cbCodeToEmu.setText( TEXT_LOAD_INTO_EMU );
	this.rbCodeToSecondSys.setVisible( false );
	this.rbCodeToPrimarySys.setVisible( false );
      }
    }
  }


  private void updCodeToEmuFieldsEnabled()
  {
    if( (this.cbCodeToEmu != null)
	&& (this.rbCodeToSecondSys != null)
	&& (this.rbCodeToPrimarySys != null) )
    {
      boolean state = this.cbCodeToEmu.isSelected();
      if( this.rbCodeToPrimarySys.isVisible() ) {
	this.rbCodeToPrimarySys.setEnabled( state );
      }
      if( this.rbCodeToSecondSys.isVisible() ) {
	this.rbCodeToSecondSys.setEnabled( state );
      }
      codeToEmuChanged( this.cbCodeToEmu.isSelected() );
    }
  }


  private void updCodeToFileFieldsEnabled()
  {
    boolean state = this.cbCodeToFile.isSelected();
    this.labelFileName.setEnabled( state );
    this.fldFileName.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
  }
}
