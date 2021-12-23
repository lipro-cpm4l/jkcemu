/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Componente zur Anzeige einer ausgeweaehlten Schrift
 */

package jkcemu.settings;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.base.EmuUtil;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;


public class FontSelectionFld extends JPanel implements ActionListener
{
  private SettingsFrm        settingsFrm;
  private FontMngr.FontUsage fontUsage;
  private Font               oldFont;
  private Font               selectedFont;
  private JButton            btnDefault;
  private JButton            btnSelect;
  private JTextField         fldInfo;


  public FontSelectionFld(
			SettingsFrm        settingsFrm,
			String             labelText,
			FontMngr.FontUsage fontUsage )
  {
    this.settingsFrm  = settingsFrm;
    this.fontUsage    = fontUsage;
    this.oldFont      = null;
    this.selectedFont = null;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( labelText ), gbc );


    this.fldInfo = GUIFactory.createTextField();
    this.fldInfo.setEditable( false );
    gbc.weightx    = 1.0;
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.fldInfo, gbc );

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnSelect = GUIFactory.createButton( EmuUtil.TEXT_SELECT );
    panelBtn.add( this.btnSelect );

    this.btnDefault = GUIFactory.createButton( "Standard" );
    panelBtn.add( this.btnDefault );

    updFields();
    this.btnSelect.addActionListener( this );
    this.btnDefault.addActionListener( this );
  }


  public void applySelectedFont( Properties props )
  {
    if( props != null ) {
      String family = "";
      String bold   = "";
      String italic = "";
      String size   = "";
      if( this.selectedFont != null ) {
	family = String.valueOf( this.selectedFont.getFamily() );
	bold   = String.valueOf( this.selectedFont.isBold() );
	italic = String.valueOf( this.selectedFont.isItalic() );
	size   = String.valueOf( this.selectedFont.getSize() );
      }
      props.setProperty(
		FontMngr.getPropNameFontFamily( this.fontUsage ),
		family );
      props.setProperty(
		FontMngr.getPropNameFontBold( this.fontUsage ),
		bold );
      props.setProperty(
		FontMngr.getPropNameFontItalic( this.fontUsage ),
		italic );
      props.setProperty(
		FontMngr.getPropNameFontSize( this.fontUsage ),
		size );
    }
  }


  public boolean hasFontChanged()
  {
    return !EmuUtil.equals( this.oldFont, this.selectedFont );
  }


  public void resetChanged()
  {
    this.oldFont = this.selectedFont;
  }


  public void updFields( Properties props )
  {
    String family = EmuUtil.getProperty(
			props,
			FontMngr.getPropNameFontFamily( fontUsage ) );
    int size = EmuUtil.getIntProperty(
			props,
			FontMngr.getPropNameFontSize( fontUsage ),
			0 );
    boolean bold = EmuUtil.getBooleanProperty(
			props,
			FontMngr.getPropNameFontBold( fontUsage ),
			false );
    boolean italic = EmuUtil.getBooleanProperty(
			props,
			FontMngr.getPropNameFontItalic( fontUsage ),
			false );
    if( !family.isEmpty() && (size > 0) ) {
      int style = 0;
      if( bold ) {
	style = Font.BOLD;
      }
      if( italic ) {
	style = Font.ITALIC;
      }
      this.oldFont = new Font(
			family,
			style != 0 ? style : Font.PLAIN,
			size );
    } else {
      this.oldFont = null;
    }
    this.selectedFont = this.oldFont;
    updFields();
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.btnSelect ) {
      Font font = FontSelectDlg.showDlg(
				this.settingsFrm,
				this.fontUsage,
				this.selectedFont );
      if( font != null ) {
	this.selectedFont = font;
	updFields();
	this.settingsFrm.fireDataChanged( true );
      }
    }
    else if( src == this.btnDefault ) {
      this.selectedFont = null;
      updFields();
      this.settingsFrm.fireDataChanged( true );
    }
  }


	/* --- private Methoden --- */

  private void updFields()
  {
    if( this.selectedFont != null ) {
      StringBuilder buf = new StringBuilder( 64 );
      buf.append( this.selectedFont.getName() );
      buf.append( String.format( ", %d pt", this.selectedFont.getSize() ) );
      if( this.selectedFont.isBold() ) {
	buf.append( ", fett" );
	if( this.selectedFont.isItalic() ) {
	  buf.append( " kursiv" );
	}
      } else if( this.selectedFont.isItalic() ) {
	buf.append( ", kursiv" );
      }
      this.fldInfo.setText( buf.toString() );
      this.btnDefault.setEnabled( true );
    } else {
      this.fldInfo.setText( "Standard" );
      this.btnDefault.setEnabled( false );
    }
  }
}
