/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige der Eigenschaften eines Textes
 */

package jkcemu.text;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class TextPropDlg extends BasicDlg
{
  private JButton btnOK;


  public TextPropDlg( Frame parent, EditText editText )
  {
    super( parent, "Eigenschaften" );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    add( new JLabel( "Dateiname:" ), gbc );
    gbc.insets.top = 2;
    gbc.gridy++;
    add( new JLabel( "Zeichensatz:" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Tabulatorbreite:" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Zeilenende:" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Zeilen trimmen:" ), gbc );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( new JLabel( "Dateiendezeichen:" ), gbc );

    gbc.insets.top    = 5;
    gbc.insets.bottom = 2;
    gbc.gridy         = 0;
    gbc.gridx++;
    File file = editText.getFile();
    if( file != null ) {
      add( new JLabel( file.getAbsolutePath() ), gbc );
    }

    String displayText = editText.getEncodingDescription();
    gbc.insets.top = 2;
    gbc.gridy++;
    add( new JLabel( displayText != null ? displayText : "System" ), gbc );

    gbc.gridy++;
    JTextArea textArea = editText.getJTextArea();
    if( textArea != null ) {
      add( new JLabel( String.valueOf( textArea.getTabSize() ) ), gbc );
    }

    displayText    = null;
    String lineEnd = editText.getLineEnd();
    if( lineEnd != null ) {
      displayText = TextLineSeparator.getDisplayText( lineEnd );
    }
    gbc.gridy++;
    add( new JLabel( displayText != null ? displayText : "System" ), gbc );

    if( editText.getTrimLines() ) {
      displayText = "Ja, Leerzeichen am Zeilenende entfernen";
    } else {
      displayText = "Nein";
    }
    gbc.gridy++;
    add( new JLabel( displayText ), gbc );

    if( editText.getTrailing1A() ) {
      displayText = "1Ah";
    } else {
      displayText = "Nicht vorhanden";
    }
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( new JLabel( displayText ), gbc );


    // Knopf
    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.btnOK, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
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
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }
}

