/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige der Eigenschaften eines Textes
 */

package jkcemu.text;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class TextPropDlg extends BaseDlg
{
  private JButton btnOK;


  public static void showDlg( Frame owner, EditText editText )
  {
    (new TextPropDlg( owner, editText )).setVisible( true );
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


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnOK.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private TextPropDlg( Frame owner, EditText editText )
  {
    super( owner, "Eigenschaften" );


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

    add( GUIFactory.createLabel( "Dateiname:" ), gbc );
    gbc.insets.top = 2;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Zeichensatz:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Tabulatorbreite:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Zeilenende:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Zeilen trimmen:" ), gbc );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Dateiendezeichen:" ), gbc );

    gbc.insets.top    = 5;
    gbc.insets.bottom = 2;
    gbc.gridy         = 0;
    gbc.gridx++;
    File file = editText.getFile();
    if( file != null ) {
      add( GUIFactory.createLabel( file.getAbsolutePath() ), gbc );
    }

    String displayText = editText.getEncodingDescription();
    gbc.insets.top = 2;
    gbc.gridy++;
    add(
	GUIFactory.createLabel( displayText != null ? displayText : "System" ),
	gbc );

    gbc.gridy++;
    JTextArea textArea = editText.getJTextArea();
    if( textArea != null ) {
      add(
	GUIFactory.createLabel( String.valueOf( textArea.getTabSize() ) ),
	gbc );
    }

    displayText    = null;
    String lineEnd = editText.getLineEnd();
    if( lineEnd != null ) {
      displayText = TextLineSeparator.getDisplayText( lineEnd );
    }
    gbc.gridy++;
    add(
	GUIFactory.createLabel( displayText != null ? displayText : "System" ),
	gbc );

    if( editText.getTrimLines() ) {
      displayText = "Ja, Leerzeichen am Zeilenende entfernen";
    } else {
      displayText = "Nein";
    }
    gbc.gridy++;
    add( GUIFactory.createLabel( displayText ), gbc );

    int eofByte = editText.getEofByte();
    if( eofByte >= 0 ) {
      displayText = String.format( "%02Xh", eofByte );
    } else {
      displayText = "Nicht vorhanden";
    }
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( GUIFactory.createLabel( displayText ), gbc );


    // Knopf
    this.btnOK    = GUIFactory.createButtonOK();
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.btnOK, gbc );


    // Listener
    this.btnOK.addActionListener( this );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }
}
