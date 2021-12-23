/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die allgemeinen Einstellungen
 * des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Properties;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.text.ParseException;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.emusys.CustomSys;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class GeneralSettingslFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private static final String LABEL_CLOCK_FREQUENCY = "Taktfrequenz:";

  private NumberFormat fmtSpeed;
  private Document     docTitle;
  private Document     docSpeedMHz;
  private JTextField   fldTitle;
  private JTextField   fldSpeedMHz;


  public GeneralSettingslFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    this.fmtSpeed = NumberFormat.getNumberInstance();
    if( this.fmtSpeed instanceof DecimalFormat ) {
      ((DecimalFormat) this.fmtSpeed).applyPattern( "#0.0##" );
    }

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Bezeichnung des Computers:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( LABEL_CLOCK_FREQUENCY ), gbc );

    this.fldTitle = GUIFactory.createTextField( 5 );
    this.docTitle = this.fldTitle.getDocument();
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridy     = 0;
    gbc.gridx++;
    add( this.fldTitle, gbc );

    this.fldSpeedMHz = GUIFactory.createTextField( 5 );
    this.docSpeedMHz = this.fldSpeedMHz.getDocument();
    gbc.fill         = GridBagConstraints.NONE;
    gbc.weightx      = 0.0;
    gbc.gridwidth    = 1;
    gbc.gridy++;
    add( this.fldSpeedMHz, gbc );
    gbc.gridx++;
    add( GUIFactory.createLabel( "MHz" ), gbc );

    if( this.docTitle != null ) {
      this.docTitle.addDocumentListener( this );
    }
    if( this.docSpeedMHz != null ) {
      this.docSpeedMHz.addDocumentListener( this );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws
						UserCancelException,
						UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_TITLE,
		this.fldTitle.getText() );
    String mhzText = this.fldSpeedMHz.getText();
    if( mhzText == null ) {
      mhzText = "";
    }
    try {
      int    khz = 0;
      Number mhz = this.fmtSpeed.parse( mhzText );
      if( mhz != null ) {
	khz = Math.round( mhz.floatValue() * 1000F );
      }
      if( khz < 1 ) {
	throw new UserInputException( LABEL_CLOCK_FREQUENCY
			+ CustomSysSettingsFld.TEXT_INVALID_VALUE );
      }
      EmuUtil.setProperty(
			props,
			this.propPrefix + CustomSys.PROP_SPEED_KHZ,
			khz );
    }
    catch( ParseException ex ) {
      throw new UserInputException( LABEL_CLOCK_FREQUENCY
			+ CustomSysSettingsFld.TEXT_INVALID_VALUE );
    }
  }


  @Override
  public void updFields( Properties props )
  {
    this.fldTitle.setText( CustomSys.getTitle( props ) );
    this.fldSpeedMHz.setText(
	this.fmtSpeed.format(
		(float) CustomSys.getDefaultSpeedKHz( props ) / 1000F ) );
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if( (doc == this.docTitle) || (doc == this.docSpeedMHz) ) {
      fireDataChanged();
    }
  }
}
