/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die PC/M-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.emusys.PCM;


public class PCMSettingsFld extends AbstractSettingsFld
{
  private static final int DEFAULT_AUTO_ACTION_WAIT_MILLIS = 500;

  private JTabbedPane          tabbedPane;
  private JPanel               tabModel;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JCheckBox            btnAutoLoadBDOS;
  private JRadioButton         btnRF64x16;
  private JRadioButton         btnFDC64x16;
  private JRadioButton         btnFDC80x24;
  private ROMFileSettingsFld   fldAltROM;
  private ROMFileSettingsFld   fldAltFont;


  public PCMSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
    this.tabModel = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Modell", this.tabModel );

    GridBagConstraints gbcModel = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.btnRF64x16 = new JRadioButton(
				"RAM-Floppy-System, 64x16 Zeichen",
				true );
    grpSys.add( this.btnRF64x16 );
    this.btnRF64x16.addActionListener( this );
    this.tabModel.add( this.btnRF64x16, gbcModel );

    this.btnAutoLoadBDOS = new JCheckBox(
				"Bei RESET automatisch BDOS laden",
				true );
    this.btnAutoLoadBDOS.addActionListener( this );
    gbcModel.insets.top  = 0;
    gbcModel.insets.left = 50;
    gbcModel.gridy++;
    this.tabModel.add( this.btnAutoLoadBDOS, gbcModel );

    this.btnFDC64x16 = new JRadioButton(
				"Floppy-Disk-System, 64x16 Zeichen",
				false );
    grpSys.add( this.btnFDC64x16 );
    this.btnFDC64x16.addActionListener( this );
    gbcModel.insets.left = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnFDC64x16, gbcModel );

    this.btnFDC80x24 = new JRadioButton(
				"Floppy-Disk-System, 80x24 Zeichen",
				false );
    grpSys.add( this.btnFDC80x24 );
    this.btnFDC80x24.addActionListener( this );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnFDC80x24, gbcModel );

    gbcModel.fill          = GridBagConstraints.HORIZONTAL;
    gbcModel.weightx       = 1.0;
    gbcModel.insets.top    = 10;
    gbcModel.insets.bottom = 10;
    gbcModel.gridy++;
    this.tabModel.add( new JSeparator(), gbcModel );

    this.fldAltROM = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "rom.",
		"Alternativer ROM-Inhalt (Grundbetriebssystem):" );
    gbcModel.insets.top    = 5;
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.fldAltROM, gbcModel );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "font.",
				"Alternativer Zeichensatz:" );
    gbcModel.gridy++;
    this.tabModel.add( this.fldAltFont, gbcModel );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
					settingsFrm,
					propPrefix,
					DEFAULT_AUTO_ACTION_WAIT_MILLIS,
					true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
					settingsFrm,
					propPrefix,
					PCM.getDefaultSwapKeyCharCase(),
					DEFAULT_AUTO_ACTION_WAIT_MILLIS );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    // Tab Modell
    boolean fdc64x16 = this.btnFDC64x16.isSelected();
    boolean fdc80x24 = this.btnFDC80x24.isSelected();
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_FDC_ENABLDED,
		fdc64x16 || fdc80x24 );
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_GRAPHIC_KEY,
		fdc80x24 ?
			PCM.PROP_GRAPHIC_VALUE_80X24
			: PCM.PROP_GRAPHIC_VALUE_64X32 );
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_AUTO_LOAD_BDOS,
		this.btnAutoLoadBDOS.isSelected() );
    this.fldAltROM.applyInput( props, selected );
    this.fldAltFont.applyInput( props, selected );

    // Tab AutoLoad
    this.tabAutoLoad.applyInput( props, selected );

    // Tab AutoInput
    this.tabAutoInput.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      rv = this.tabAutoLoad.doAction( e );
      if( !rv ) {
	rv = this.tabAutoInput.doAction( e );
      }
      if( !rv ) {
	if( src instanceof JRadioButton ) {
	  updAutoLoadBDOSFieldEnabled();
	  fireDataChanged();
	  rv = true;
	}
	else if( src instanceof AbstractButton ) {
	  fireDataChanged();
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    if( EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + PCM.PROP_FDC_ENABLDED,
		false ) )
    {
      if( EmuUtil.getProperty(
		props,
		this.propPrefix + PCM.PROP_GRAPHIC_KEY ).equals(
					PCM.PROP_GRAPHIC_VALUE_80X24 ) )
      {
	this.btnFDC80x24.setSelected( true );
      } else {
	this.btnFDC64x16.setSelected( true );
      }
    } else {
      this.btnRF64x16.setSelected( true );
    }
    this.btnAutoLoadBDOS.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PCM.PROP_AUTO_LOAD_BDOS,
				true ) );
    this.fldAltROM.updFields( props );
    this.fldAltFont.updFields( props );
    updAutoLoadBDOSFieldEnabled();

    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private void updAutoLoadBDOSFieldEnabled()
  {
    this.btnAutoLoadBDOS.setEnabled( this.btnRF64x16.isSelected() );
  }
}
