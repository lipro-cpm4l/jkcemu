/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die ZXSpectrum-Einstellungen
 */

package jkcemu.emusys.zxspectrum;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.ZXSpectrum;


public class ZXSpectrumSettingsFld extends AbstractSettingsFld
{
  private JRadioButton       btn48K;
  private JRadioButton       btn128K;
  private ROMFileSettingsFld fldAltROM;


  public ZXSpectrumSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpModel = new ButtonGroup();

    this.btn48K = new JRadioButton( "ZX Spectrum 48K", true );
    grpModel.add( this.btn48K );
    add( this.btn48K, gbc );

    this.btn128K = new JRadioButton( "ZX Spectrum+ 128K", false );
    grpModel.add( this.btn128K );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btn128K, gbc );

    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.fldAltROM = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + ZXSpectrum.PROP_ROM_PREFIX,
		"Alternativer Betriebssystem-ROM:" );
    gbc.insets.top    = 5;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.fldAltROM, gbc );


    // Listener
    this.btn48K.addActionListener( this );
    this.btn128K.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + ZXSpectrum.PROP_MODEL,
		this.btn128K.isSelected() ? "128k" : "48k" );
    this.fldAltROM.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
        rv = true;
        fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.fldAltROM.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    if( EmuUtil.getProperty(
		props,
		this.propPrefix + ZXSpectrum.PROP_MODEL ).equals(
						ZXSpectrum.VALUE_128K ) )
    {
      this.btn128K.setSelected( true );
    } else {
      this.btn48K.setSelected( true );
    }
    this.fldAltROM.updFields( props );
  }
}
