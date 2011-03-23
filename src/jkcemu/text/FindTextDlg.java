/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe eines Suchtextes
 */

package jkcemu.text;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class FindTextDlg extends BasicDlg
{
  public enum Action { NO_ACTION, FIND_NEXT, REPLACE_ALL };

  private Action     action;
  private JTextField textFieldFind;
  private JTextField textFieldReplace;
  private JCheckBox  tglNoticeCase;
  private JButton    btnFindNext;
  private JButton    btnReplaceAll;
  private JButton    btnCancel;


  public FindTextDlg(
		Frame   parent,
		String  textFind,
		String  textReplace,
		boolean ignoreCase )
  {
    super( parent, "Suchen und Ersetzen" );

    // Initialisierungen
    this.action = Action.NO_ACTION;

    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Labels
    add( new JLabel( "Suchen nach:" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Ersetzen durch:" ), gbc );

    // Eingabefelder
    this.textFieldFind = new JTextField();
    this.textFieldFind.setEditable( true );
    this.textFieldFind.addActionListener( this );
    if( textFind != null ) {
      this.textFieldFind.setText( getFirstLine( textFind ) );
    }
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy   = 0;
    gbc.gridx++;
    add( this.textFieldFind, gbc );

    this.textFieldReplace = new JTextField();
    this.textFieldReplace.setEditable( true );
    this.textFieldReplace.addActionListener( this );
    if( textReplace != null ) {
      this.textFieldReplace.setText( getFirstLine( textReplace ) );
    }
    gbc.gridy++;
    add( this.textFieldReplace, gbc );

    // Schaltknopf
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridy++;
    this.tglNoticeCase = new JCheckBox(
				" Gro\u00DF-/Kleinschreibung beachten",
				!ignoreCase );
    add( this.tglNoticeCase, gbc );

    // Knoepfe
    JPanel panelBtn = new JPanel();
    panelBtn.setLayout( new GridLayout( 3, 1, 5, 5 ) );

    this.btnFindNext = new JButton( "Suchen" );
    this.btnFindNext.addActionListener( this );
    this.btnFindNext.addKeyListener( this );
    panelBtn.add( this.btnFindNext );

    this.btnReplaceAll = new JButton( "Alle ersetzen" );
    this.btnReplaceAll.addActionListener( this );
    this.btnReplaceAll.addKeyListener( this );
    panelBtn.add( this.btnReplaceAll );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor     = GridBagConstraints.NORTHEAST;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );

    // Tab-Sequenz
    setFocusTraversalPolicy(
	new ListFocusTraversalPolicy(
			this.textFieldFind,
			this.textFieldReplace,
			this.tglNoticeCase,
			this.btnFindNext,
			this.btnReplaceAll,
			this.btnCancel ) );

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  public Action getAction()
  {
    return this.action;
  }


  public String getTextFind()
  {
    String rv = this.textFieldFind.getText();
    return rv != null ? rv : "";
  }


  public String getTextReplace()
  {
    String rv = this.textFieldReplace.getText();
    return rv != null ? rv : "";
  }


  public boolean getIgnoreCase()
  {
    return !this.tglNoticeCase.isSelected();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( src.equals( this.btnFindNext )   ||
	    src.equals( this.textFieldFind ) ||
	    src.equals( this.textFieldReplace ) )
	{
	  rv          = true;
	  this.action = Action.FIND_NEXT;
	  doClose();
	}
	else if( src.equals( this.btnReplaceAll ) ) {
	  rv          = true;
	  this.action = Action.REPLACE_ALL;
	  doClose();
	}
	else if( src.equals( this.btnCancel ) ) {
	  rv          = true;
	  this.action = Action.NO_ACTION;
	  doClose();
	}
      }
    }
    return rv;
  }


  @Override
  public void windowClosing( WindowEvent e )
  {
    this.action = Action.NO_ACTION;
    super.windowClosing( e );
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.textFieldFind != null) ) {
      this.textFieldFind.requestFocus();
      this.textFieldFind.selectAll();
    }
  }


	/* --- private Methoden --- */

  private String getFirstLine( String text )
  {
    int eol = text.indexOf( (char) '\n' );
    return eol >= 0 ? text.substring( 0, eol ) : text;
  }
}

