/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Auswahl eines Unicode-Zeichens
 */

package jkcemu.text;

import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.CharPageFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class CharSelectDlg extends BaseDlg implements CharPageFld.Callback
{
  private TextEditFrm  textEditFrm;
  private boolean      notified;
  private int          codeBase;
  private CharPageFld  charPageFld;
  private JTextField   fldCodeBase;
  private JTextField   fldSelectedChar;
  private JTextField   fldCharInfo;
  private JRadioButton rbScale1;
  private JRadioButton rbScale2;
  private JButton      btnPrevPage;
  private JButton      btnNextPage;
  private JButton      btnCopy;
  private JButton      btnPaste;
  private JButton      btnClose;


  public CharSelectDlg( TextEditFrm textEditFrm )
  {
    super( textEditFrm, Dialog.ModalityType.MODELESS );
    setTitle( "Zeichenauswahl" );
    this.textEditFrm = textEditFrm;
    this.notified    = false;
    this.codeBase    = 0;


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Kopfbereich
    JPanel panelHeader = GUIFactory.createPanel( new GridBagLayout() );
    add( panelHeader, gbc );

    GridBagConstraints gbcHeader = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 0 ),
					0, 0 );

    panelHeader.add(
		GUIFactory.createLabel( "Unicode-Seite [hex]:" ),
		gbcHeader );

    this.btnPrevPage = GUIFactory.createRelImageResourceButton(
					this,
					"nav/back.png",
					"Vorherige Seite" );
    gbcHeader.gridx++;
    panelHeader.add( this.btnPrevPage, gbcHeader );

    this.fldCodeBase = GUIFactory.createTextField( 3 );
    gbcHeader.gridx++;
    panelHeader.add( this.fldCodeBase, gbcHeader );

    this.btnNextPage = GUIFactory.createRelImageResourceButton(
					this,
					"nav/next.png",
					"N\u00E4chste Seite" );
    gbcHeader.gridx++;
    panelHeader.add( this.btnNextPage, gbcHeader );

    gbcHeader.insets.left = 10;
    gbcHeader.gridx++;
    panelHeader.add(
		GUIFactory.createLabel( "Skalierung:" ),
		gbcHeader );

    ButtonGroup grpScale = new ButtonGroup();

    this.rbScale1 = GUIFactory.createRadioButton( "100%", true );
    grpScale.add( this.rbScale1 );
    gbcHeader.insets.left = 5;
    gbcHeader.gridx++;
    panelHeader.add( this.rbScale1, gbcHeader );

    this.rbScale2 = GUIFactory.createRadioButton( "200%" );
    grpScale.add( this.rbScale2 );
    gbcHeader.gridx++;
    panelHeader.add( this.rbScale2, gbcHeader );

    // Header-Felder nach links schieben
    gbcHeader.fill    = GridBagConstraints.HORIZONTAL;
    gbcHeader.weightx = 1.0;
    gbcHeader.gridx++;
    panelHeader.add( GUIFactory.createPanel(), gbcHeader );


    // Anzeige Unicode-Seite
    this.charPageFld = new CharPageFld( this );
    GUIFactory.initFont( this.charPageFld );
    gbc.fill       = GridBagConstraints.BOTH;
    gbc.weightx    = 1.0;
    gbc.weighty    = 1.0;
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.charPageFld, gbc );

    // Fussbereich
    JPanel panelFooter = GUIFactory.createPanel( new GridBagLayout() );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelFooter, gbc );

    GridBagConstraints gbcFooter = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 0 ),
					0, 0 );

    panelFooter.add(
	GUIFactory.createLabel( "Ausgew\u00E4hltes Zeichen:" ),
	gbcFooter );

    this.fldSelectedChar = GUIFactory.createTextField( 3 );
    this.fldSelectedChar.setEditable( false );
    gbcFooter.insets.left = 5;
    gbcFooter.gridx++;
    panelFooter.add( this.fldSelectedChar, gbcFooter );

    gbcFooter.gridx++;
    panelFooter.add( GUIFactory.createLabel( "Code:" ), gbcFooter );

    this.fldCharInfo = GUIFactory.createTextField();
    this.fldCharInfo.setEditable( false );
    gbcFooter.fill    = GridBagConstraints.HORIZONTAL;
    gbcFooter.weightx = 1.0;
    gbcFooter.gridx++;
    panelFooter.add( this.fldCharInfo, gbcFooter );

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbcFooter.anchor       = GridBagConstraints.EAST;
    gbcFooter.fill         = GridBagConstraints.NONE;
    gbcFooter.weightx      = 0.0;
    gbcFooter.insets.right = 0;
    gbcFooter.gridx++;
    panelFooter.add( panelBtn, gbcFooter );

    this.btnPaste = GUIFactory.createButton( "In Text einf\u00FCgen" );
    this.btnPaste.setEnabled( false );
    panelBtn.add( this.btnPaste );

    this.btnCopy = GUIFactory.createButton( EmuUtil.TEXT_COPY );
    this.btnCopy.setEnabled( false );
    panelBtn.add( this.btnCopy );

    this.btnClose = GUIFactory.createButton( EmuUtil.TEXT_CLOSE );
    panelBtn.add( this.btnClose );


    // Fenstergroesse und -position
    pack();
    setLocationByPlatform( true );
    setResizable( true );


    // Vorbelegung
    setCodePage( 0 );
  }


	/* --- CharPageFld.Callback --- */

  @Override
  public void charSelected( int ch )
  {
    if( ch >= 0 ) {
      this.fldSelectedChar.setText( String.format( " %c", (char) ch ) );
      this.fldCharInfo.setText(
		ch >= 0x0100 ?
			String.format( "%04X hex / %d dez", ch, ch )
			: String.format( "%02X hex / %d dez", ch, ch ) );
      this.btnPaste.setEnabled( true );
      this.btnCopy.setEnabled( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.fldCodeBase.addActionListener( this );
      this.btnPrevPage.addActionListener( this );
      this.btnNextPage.addActionListener( this );
      this.rbScale1.addActionListener( this );
      this.rbScale2.addActionListener( this );
      this.btnCopy.addActionListener( this );
      this.btnPaste.addActionListener( this );
      this.btnClose.addActionListener( this );
      this.charPageFld.addKeyListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.fldCodeBase ) {
      rv = true;
      doCodePage();
    }
    else if( src == this.btnPrevPage ) {
      rv = true;
      doPrevCodePage();
    }
    else if( src == this.btnNextPage ) {
      rv = true;
      doNextCodePage();
    }
    else if( src == this.rbScale1 ) {
      rv = true;
      this.charPageFld.setFontScale( 1 );
    }
    else if( src == this.rbScale2 ) {
      rv = true;
      this.charPageFld.setFontScale( 2 );
    }
    else if( src == this.btnPaste ) {
      rv = true;
      doPaste();
    }
    else if( src == this.btnCopy ) {
      rv = true;
      doCopy();
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    switch( e.getKeyCode() ) {
      case KeyEvent.VK_PAGE_DOWN:
	doNextCodePage();
	e.consume();
	break;
      case KeyEvent.VK_PAGE_UP:
	doPrevCodePage();
	e.consume();
    }
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.fldCodeBase.removeActionListener( this );
      this.btnPrevPage.removeActionListener( this );
      this.btnNextPage.removeActionListener( this );
      this.rbScale1.removeActionListener( this );
      this.rbScale2.removeActionListener( this );
      this.btnCopy.removeActionListener( this );
      this.btnPaste.removeActionListener( this );
      this.btnClose.removeActionListener( this );
      this.charPageFld.removeKeyListener( this );
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.charPageFld != null) )
      this.charPageFld.requestFocus();
  }


	/* --- private Methoden --- */

  private void doCodePage()
  {
    int codeBase = this.codeBase;
    try {
      String text = this.fldCodeBase.getText();
      if( text != null ) {
	codeBase = Integer.parseInt( text, 16 );
	if( text.length() <= 2 ) {
	  codeBase <<= 8;
	}
      }
    }
    catch( NumberFormatException ex ) {}
    setCodePage( codeBase );
  }


  private void doPrevCodePage()
  {
    setCodePage( this.codeBase - 0x100 );
  }


  private void doNextCodePage()
  {
    setCodePage( this.codeBase + 0x100 );
  }


  private void doCopy()
  {
    String text = getSelectedCharText();
    if( text != null ) {
      EmuUtil.copyToClipboard( this, text );
    }
  }


  private void doPaste()
  {
    String text = getSelectedCharText();
    if( text != null ) {
      this.textEditFrm.pasteText( text );
    }
  }


  private String getSelectedCharText()
  {
    String rv = null;
    String s  = this.fldSelectedChar.getText();
    if( s != null ) {
      if( s.length() > 1 ) {
	rv = s.substring( 1 );
      }
    }
    return rv;
  }


  private void setCodePage( int codeBase )
  {
    this.codeBase = codeBase & 0xFF00;
    this.fldCodeBase.setText(
		String.format( "%02X", (this.codeBase >> 8) ) );
    this.charPageFld.setCodeBase( this.codeBase );
  }
}
