/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe eines Suchtextes
 */

package jkcemu.text;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
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
import jkcemu.base.ComboBoxEnterActionMngr;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.ListFocusTraversalPolicy;


public class FindTextDlg
		extends BaseDlg
		implements ComboBoxEnterActionMngr.EnterListener
{
  public enum Action { NO_ACTION, FIND_NEXT, REPLACE_ALL };

  private static java.util.List<String> findHistory    = new ArrayList<>();
  private static java.util.List<String> replaceHistory = new ArrayList<>();

  private boolean           notified;
  private Action            action;
  private String            searchText;
  private String            replaceText;
  private JComboBox<String> comboFind;
  private JComboBox<String> comboReplace;
  private JCheckBox         cbNoticeCase;
  private JButton           btnFind;
  private JButton           btnReplaceAll;
  private JButton           btnCancel;


  public FindTextDlg(
		Window  owner,
		String  textSearch,
		String  textReplace,
		boolean ignoreCase )
  {
    super( owner, EmuUtil.TEXT_FIND_AND_REPLACE );
    this.notified    = false;
    this.action      = Action.NO_ACTION;
    this.searchText  = null;
    this.replaceText = null;

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
    add( GUIFactory.createLabel( EmuUtil.LABEL_SEARCH_FOR ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Ersetzen durch:" ), gbc );

    // Eingabefelder
    this.comboFind = GUIFactory.createComboBox();
    for( String item : findHistory ) {
      this.comboFind.addItem( item );
    }
    this.comboFind.setEditable( true );
    this.comboFind.setSelectedItem(
			textSearch != null ?
				TextUtil.getFirstLine( textSearch )
				: "" );
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy   = 0;
    gbc.gridx++;
    add( this.comboFind, gbc );

    this.comboReplace = GUIFactory.createComboBox();
    this.comboReplace.addItem( "" );
    for( String item : replaceHistory ) {
      this.comboReplace.addItem( item );
    }
    this.comboReplace.setEditable( true );
    this.comboReplace.setSelectedItem(
			textReplace != null ?
				TextUtil.getFirstLine( textReplace )
				: "" );
    gbc.gridy++;
    add( this.comboReplace, gbc );

    Font font = (GUIFactory.createTextField()).getFont();
    if( font != null ) {
      this.comboFind.setFont( font );
    }

    // Schaltknopf
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridy++;
    this.cbNoticeCase = GUIFactory.createCheckBox(
				" Gro\u00DF-/Kleinschreibung beachten",
				!ignoreCase );
    add( this.cbNoticeCase, gbc );

    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel();
    panelBtn.setLayout( new GridLayout( 3, 1, 5, 5 ) );

    this.btnFind = GUIFactory.createButton( EmuUtil.TEXT_FIND );
    panelBtn.add( this.btnFind );

    this.btnReplaceAll = GUIFactory.createButton( "Alle ersetzen" );
    panelBtn.add( this.btnReplaceAll );

    this.btnCancel = GUIFactory.createButtonCancel();
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
			this.comboReplace,
			this.cbNoticeCase,
			this.btnFind,
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


  public boolean getIgnoreCase()
  {
    return !this.cbNoticeCase.isSelected();
  }


  public String getReplaceText()
  {
    return this.replaceText != null ? this.replaceText : "";
  }


  public String getSearchText()
  {
    return this.searchText != null ? this.searchText : "";
  }


	/* --- ComboBoxEnterActionMngr.EnterListener --- */

  @Override
  public void comboBoxEnterAction( JComboBox<?> combo )
  {
    if( (combo == this.comboFind) || (combo == this.comboReplace) )
      doFind( Action.FIND_NEXT );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.btnFind.addActionListener( this );
      this.btnReplaceAll.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnFind ) {
      rv = true;
      doFind( Action.FIND_NEXT );
    }
    else if( src.equals( this.btnReplaceAll ) ) {
      rv = true;
      doFind( Action.REPLACE_ALL );
    }
    else if( src.equals( this.btnCancel ) ) {
      rv          = true;
      this.action = Action.NO_ACTION;
      doClose();
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.btnFind.removeActionListener( this );
      this.btnReplaceAll.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
  }


  /*
   * Da die Editorkomponente geshared genutzt werden koennte,
   * wird hiermit sichergestellt,
   * dass der daran registrierte Listener auch wieder entfernt wird.
   */
  @Override
  public void setVisible( boolean state )
  {
    if( state && !isVisible() ) {
      ComboBoxEnterActionMngr.addListener( this.comboFind, this );
      ComboBoxEnterActionMngr.addListener( this.comboReplace, this );
    }
    super.setVisible( state );
    if( !state && isVisible() ) {
      ComboBoxEnterActionMngr.removeListener( this.comboFind, this );
      ComboBoxEnterActionMngr.removeListener( this.comboReplace, this );
    }
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
      ComboBoxEditor editor = this.comboFind.getEditor();
      if( editor != null ) {
	editor.selectAll();
      }
    }
  }


	/* --- private Methoden --- */

  private void doFind( Action action )
  {
    this.searchText = getTextAndInsertToHistory(
					this.comboFind,
					findHistory );
    this.replaceText = getTextAndInsertToHistory(
					this.comboReplace,
					replaceHistory );
    if( this.searchText != null ) {
      this.action = action;
      doClose();
    }
  }


  private String getTextAndInsertToHistory(
					JComboBox<String>      combo,
					java.util.List<String> history )
  {
    String rv = null;
    Object o  = combo.getSelectedItem();
    if( o != null ) {
      String s = o.toString();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  rv = s;
	  history.remove( s );
	  int n = history.size();
	  while( n >= 10 ) {
	    history.remove( n - 1 );
	    --n;
	  }
	  history.add( 0, s );
	}
      }
    }
    return rv;
  }
}
