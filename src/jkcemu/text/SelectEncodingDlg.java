/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Zeichensatzes
 */

package jkcemu.text;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class SelectEncodingDlg extends BasicDlg
{
  private boolean       applied;
  private CharConverter charConverter;
  private String        encodingName;
  private String        encodingDisplayText;
  private JComboBox     comboEncoding;
  private JButton       btnOK;
  private JButton       btnCancel;


  public SelectEncodingDlg( Frame parent )
  {
    super( parent, "Zeichensatz ausw\u00E4hlen" );


    // Initialisierungen
    this.applied             = false;
    this.charConverter       = null;
    this.encodingName        = null;
    this.encodingDisplayText = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Auswahlfeld
    this.comboEncoding = new JComboBox();
    this.comboEncoding.addItem( "Systemzeichensatz" );
    this.comboEncoding.addItem(
                new CharConverter( CharConverter.Encoding.ASCII_7BIT ) );
    this.comboEncoding.addItem(
                new CharConverter( CharConverter.Encoding.ISO646DE ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP437 ) );
    this.comboEncoding.addItem(
		new CharConverter( CharConverter.Encoding.CP850 ) );
    this.comboEncoding.addItem( new CharConverter(
		CharConverter.Encoding.LATIN1 ) );
    this.comboEncoding.addItem( "UTF-8" );
    this.comboEncoding.addItem( "UTF-16 (mit Byte-Order-Markierung)" );
    this.comboEncoding.addItem( "UTF-16BE (Big-Endian)" );
    this.comboEncoding.addItem( "UTF-16LE (Little-Endian)" );
    this.comboEncoding.setEditable( false );
    add( this.comboEncoding, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel();
    panelBtn.setLayout( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.gridy++;
    add( panelBtn, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  public boolean encodingChoosen()
  {
    return this.applied;
  }


  public CharConverter getCharConverter()
  {
    return this.charConverter;
  }


  public String getEncodingName()
  {
    return this.encodingName;
  }


  public String getEncodingDisplayText()
  {
    return this.encodingDisplayText;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnOK ) {
	  doApply();
	}
	else if( src == this.btnCancel ) {
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    Object encodingObj = this.comboEncoding.getSelectedItem();
    if( encodingObj != null ) {
      this.encodingDisplayText = encodingObj.toString();
      if( encodingObj instanceof CharConverter ) {
	this.charConverter = (CharConverter) encodingObj;
	this.encodingName  = charConverter.getEncodingName();
      } else {
	String s = encodingObj.toString();
	if( s != null ) {
	  if( s.startsWith( "ISO" ) || s.startsWith( "UTF" ) ) {
	    int posSpace = s.indexOf( '\u0020' );
	    this.encodingName = (posSpace > 0 ?
					s.substring( 0, posSpace ) : s);
	  }
	}
      }
      this.applied = true;
      doClose();
    }
  }
}

