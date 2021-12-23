/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anzeigen von Detailinformationen eines Bildes
 */

package jkcemu.image;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.UnsupportedEncodingException;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;
import jkcemu.text.CharConverter;


public class ExifEditDlg extends BaseDlg
{
  public static class Latin1Document extends PlainDocument
  {
    private CharConverter converter;

    public Latin1Document()
    {
      this.converter = new CharConverter( CharConverter.Encoding.LATIN1 );
    }

    @Override
    public void insertString(
			int          offs,
			String       s,
			AttributeSet a ) throws BadLocationException
    {
      if( s != null ) {
	char[] buf = s.toCharArray();
	int    pos = 0;
	for( char ch : buf ) {
	  if( this.converter.toCharsetByte( ch ) > 0 ) {
	    buf[ pos++ ] = ch;
	  }
	}
	if( pos > 0 ) {
	  super.insertString( offs, String.valueOf( buf, 0, pos ), a );
	}
      }
    }
  };


  private ExifData   approvedExifData;
  private ExifData   oldExifData;
  private JLabel     labelAuthor;
  private JLabel     labelCopyright;
  private JLabel     labelName;
  private JLabel     labelDesc;
  private JTextField fldAuthor;
  private JTextField fldCopyright;
  private JTextField fldComment;
  private JTextField fldName;
  private JTextField fldDesc;
  private JButton    btnOK;
  private JButton    btnCancel;


  public static ExifData showDlg( Window owner, ExifData exifData )
  {
    ExifEditDlg dlg = new ExifEditDlg( owner, exifData );
    dlg.setVisible( true );
    return dlg.approvedExifData;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnOK) || (src == this.fldComment) ) {
	rv = true;
	doApprove();
      } else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      } else if( src instanceof JTextField ) {
	rv = true;
	((JTextField) src).transferFocus();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldName.removeActionListener( this );
      this.fldDesc.removeActionListener( this );
      this.fldAuthor.removeActionListener( this );
      this.fldCopyright.removeActionListener( this );
      this.fldComment.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ExifEditDlg( Window owner, ExifData exifData )
  {
    super( owner, "Zusatzinformationen bearbeiten" );
    this.oldExifData      = exifData;
    this.approvedExifData = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.labelName = GUIFactory.createLabel( "Name / Titel:" );
    add( this.labelName, gbc );

    this.labelDesc = GUIFactory.createLabel( "Beschreibung / Bemerkung:" );
    gbc.gridy++;
    add( this.labelDesc, gbc );

    this.labelAuthor = GUIFactory.createLabel( "Autor / Fotograf:" );
    gbc.gridy++;
    add( this.labelAuthor, gbc );

    this.labelCopyright = GUIFactory.createLabel( "Copyright:" );
    gbc.gridy++;
    add( this.labelCopyright, gbc );

    /*
     * Kommentar kann alle Unicode-Zeichen enthalten,
     * d.h., deas Label wird nicht fuer eine Fehlemeldung benoetigt.
     */
    gbc.gridy++;
    add( GUIFactory.createLabel( "Kommentar:" ), gbc );

    this.fldName = createLatin1TextField();
    gbc.anchor   = GridBagConstraints.WEST;
    gbc.fill     = GridBagConstraints.HORIZONTAL;
    gbc.weightx  = 1.0;
    gbc.gridy    = 0;
    gbc.gridx++;
    add( this.fldName, gbc );

    this.fldDesc = createLatin1TextField();
    gbc.gridy++;
    add( this.fldDesc, gbc );

    this.fldAuthor = createLatin1TextField();
    gbc.gridy++;
    add( this.fldAuthor, gbc );

    this.fldCopyright = createLatin1TextField();
    gbc.gridy++;
    add( this.fldCopyright, gbc );

    // Kommentar kann alle Unicode-Zeichen enthalten
    this.fldComment = GUIFactory.createTextField();
    gbc.gridy++;
    add( this.fldComment, gbc );

    JPanel panelBtn = GUIFactory.createPanel(
				new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );
    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
    this.fldName.setColumns( 0 );
    this.fldDesc.setColumns( 0 );
    this.fldAuthor.setColumns( 0 );
    this.fldCopyright.setColumns( 0 );


    // Vorbelegungen
    if( exifData != null ) {
      setText( this.fldName, exifData.getDocumentName() );
      setText( this.fldDesc, exifData.getImageDesc() );
      setText( this.fldAuthor, exifData.getAuthor() );
      setText( this.fldCopyright, exifData.getCopyright() );
      setText( this.fldComment, exifData.getComment() );
    }


    // Listener
    this.fldName.addActionListener( this );
    this.fldDesc.addActionListener( this );
    this.fldAuthor.addActionListener( this );
    this.fldCopyright.addActionListener( this );
    this.fldComment.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private static JTextField createLatin1TextField()
  {
    return GUIFactory.createTextField( new Latin1Document(), 20 );
  }


  private void doApprove()
  {
    JLabel label = this.labelName;
    try {
      ExifData exifData = this.oldExifData;
      if( exifData != null ) {
	exifData = exifData.copy();
      } else {
	exifData = new ExifData();
      }
      exifData.setDocumentName( getLatin1Bytes( this.fldName ) );
      label = this.labelDesc;
      exifData.setImageDesc( getLatin1Bytes( this.fldDesc ) );
      label = this.labelAuthor;
      exifData.setAuthor( getLatin1Bytes( this.fldAuthor ) );
      label = this.labelCopyright;
      exifData.setCopyright( getLatin1Bytes( this.fldCopyright ) );
      exifData.setComment( this.fldComment.getText() );
      this.approvedExifData = exifData;
      doClose();
    }
    catch( UnsupportedEncodingException ex ) {
      showErrorDlg(
		this,
		label.getText() + "Der Text enth\u00E4lt Zeichen"
			+ " aus einem Zeichensatz,\n"
			+ "der hier nicht unterst\u00FCtzt wird." );
    }
  }


  private static byte[] getLatin1Bytes( JTextField fld )
					throws UnsupportedEncodingException
  {
    byte[] rv = null;
    String s  = fld.getText();
    if( s != null ) {
      if( !s.isEmpty() ) {
	rv = s.getBytes( "ISO-8859-1" );
      }
    }
    return rv;
  }


  private static void setText( JTextField fld, String text )
  {
    if( text != null )
      fld.setText( text );
  }
}
