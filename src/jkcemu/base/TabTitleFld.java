/*
 * (c) 2019-2022 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Reiter einer JTabbedPane mit Schaltflaeche zum Schliessen
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;


public class TabTitleFld extends JPanel implements ActionListener
{
  public static class TabCloseEvent extends ActionEvent
  {
    private int  tabIdx;

    public TabCloseEvent(
			JTabbedPane source,
			int    id,
			int    tabIdx,
			long   when,
			int    modifiers )
    {
      super( source, id, null, when, modifiers );
      this.tabIdx = tabIdx;
    }

    public int getTabIndex()
    {
      return this.tabIdx;
    }
  };


  ActionListener           actionListener;
  private JTabbedPane      tabbedPane;
  private JLabel           label;
  private RolloverCloseBtn closeBtn;


  public static TabTitleFld addTabTo(
			JTabbedPane    tabbedPane,
			String         title,
			Component      tab,
			ActionListener listener )
  {
    int idx = tabbedPane.getTabCount();
    tabbedPane.insertTab( title, null, tab, null, idx );
    TabTitleFld titleFld = new TabTitleFld( tabbedPane, title, listener );
    tabbedPane.setTabComponentAt( idx, titleFld );
    return titleFld;
  }


  public RolloverCloseBtn getCloseBtn()
  {
    return this.closeBtn;
  }


  public JTabbedPane getTabbedPane()
  {
    return this.tabbedPane;
  }


  public static void setTitleAt(
			JTabbedPane    tabbedPane,
			int            idx,
			String         title )
  {
    boolean   done   = false;
    Component tabFld = tabbedPane.getTabComponentAt( idx );
    if( tabFld != null ) {
      if( tabFld instanceof TabTitleFld ) {
	((TabTitleFld) tabFld).label.setText( title );
	done = true;
      }
    }
    if( !done ) {
      tabbedPane.setTitleAt( idx, title );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    Dimension d = this.label.getPreferredSize();
    if( d != null ) {
      if( d.height > 0 ) {
	this.closeBtn.setPreferredSize(
			new Dimension( d.height, d.height ) );
      }
    }
    if( this.actionListener != null ) {
      this.closeBtn.addActionListener( this );
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.actionListener != null ) {
      this.closeBtn.removeActionListener(  this );
    }
    super.removeNotify();
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    if( (this.actionListener != null)
	&& (e.getSource() == this.closeBtn) )
    {
      this.actionListener.actionPerformed(
		new TabCloseEvent(
			this.tabbedPane,
			e.getID(),
			this.tabbedPane.indexOfTabComponent( this ),
			e.getWhen(),
			e.getModifiers() ) );
    }
  }


	/* --- Konstruktor --- */

  private TabTitleFld(
		JTabbedPane    tabbedPane,
		String         title,
		ActionListener actionListener )
  {
    this.tabbedPane     = tabbedPane;
    this.actionListener = actionListener;
    setOpaque( false );
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
				0, 0,
				1, 1,
				1.0, 0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL,
				new Insets( 0, 0, 0, 0 ),
				0, 0 );
				
    this.label = GUIFactory.createLabel( title );
    this.label.setOpaque( false );
    add( this.label, gbc );
    this.closeBtn = new RolloverCloseBtn();
    this.closeBtn.setOpaque( false );
    this.closeBtn.setToolTipText( EmuUtil.TEXT_CLOSE );
    gbc.anchor      = GridBagConstraints.EAST;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.top  = 1;
    gbc.insets.left = 8;
    gbc.gridx++;
    add( this.closeBtn, gbc );
  }
}
