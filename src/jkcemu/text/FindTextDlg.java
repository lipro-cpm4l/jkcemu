/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe eines Suchtextes
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.ListFocusTraversalPolicy;


public class FindTextDlg extends BaseDlg
{
  public enum Action { NO_ACTION, FIND_NEXT, REPLACE_ALL };

  private static java.util.List<String> findHistory = new ArrayList<>();

  private Action            action;
  private String            findText;
  private JComboBox<String> comboFind;
  private JTextField        fldFind;
  private JTextField        fldReplace;
  private JCheckBox         tglNoticeCase;
  private JButton           btnFindNext;
  private JButton           btnReplaceAll;
  private JButton           btnCancel;


  public FindTextDlg(
		Frame   parent,
		String  textFind,
		String  textReplace,
		boolean ignoreCase )
  {
    super( parent, "Suchen und Ersetzen" );

    // Initialisierungen
    this.action   = Action.NO_ACTION;
    this.findText = null;

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
    this.comboFind = new JComboBox<>();
    for( String item : findHistory ) {
      this.comboFind.addItem( item );
    }
    this.comboFind.setEditable( true );
    if( textFind != null ) {
      this.comboFind.setSelectedItem( getFirstLine( textFind ) );
    }
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy   = 0;
    gbc.gridx++;
    add( this.comboFind, gbc );

    this.fldFind = null;
    ComboBoxEditor editor = this.comboFind.getEditor();
    if( editor != null ) {
      Component c = editor.getEditorComponent();
      if( c != null ) {
	if( c instanceof JTextField ) {
	  this.fldFind = (JTextField) c;
	  this.fldFind.addActionListener( this );
	}
      }
    }

    this.fldReplace = new JTextField();
    this.fldReplace.setEditable( true );
    this.fldReplace.addActionListener( this );
    if( textReplace != null ) {
      this.fldReplace.setText( getFirstLine( textReplace ) );
    }
    gbc.gridy++;
    add( this.fldReplace, gbc );

    Font font = this.fldReplace.getFont();
    if( font != null ) {
      this.comboFind.setFont( font );
    }

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
			this.comboFind,
			this.fldReplace,
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


  public String getFindText()
  {
    return this.findText != null ? this.findText : "";
  }


  public boolean getIgnoreCase()
  {
    return !this.tglNoticeCase.isSelected();
  }


  public String getReplaceText()
  {
    String rv = this.fldReplace.getText();
    return rv != null ? rv : "";
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( (src == this.fldFind) && (this.fldFind != null) ) {
	  rv          = true;
	  this.action = Action.FIND_NEXT;
	  applyFindText( this.fldFind.getText() );
	  doClose();
	}
	if( (src == this.fldReplace) || (src == this.btnFindNext) ) {
	  rv          = true;
	  this.action = Action.FIND_NEXT;
	  applyFindText( this.comboFind.getSelectedItem() );
	  doClose();
	}
	else if( src.equals( this.btnReplaceAll ) ) {
	  rv          = true;
	  this.action = Action.REPLACE_ALL;
	  applyFindText( this.comboFind.getSelectedItem() );
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
    if( (e.getWindow() == this) && (this.comboFind != null) ) {
      this.comboFind.requestFocus();
      if( this.fldFind != null ) {
	this.fldFind.selectAll();
      }
    }
  }


	/* --- private Methoden --- */

  private void applyFindText( Object text )
  {
    this.findText = (text != null ? text.toString() : null);
    if( this.findText != null ) {
      if( !this.findText.isEmpty() ) {
	findHistory.remove( this.findText );
	int n = findHistory.size();
	while( n >= 10 ) {
	  this.findHistory.remove( n - 1 );
	  --n;
	}
	findHistory.add( 0, this.findText );
      }
    }
  }


  private String getFirstLine( String text )
  {
    int eol = text.indexOf( (char) '\n' );
    return eol >= 0 ? text.substring( 0, eol ) : text;
  }
}
