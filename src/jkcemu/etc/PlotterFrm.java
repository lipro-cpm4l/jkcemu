/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer den Plotter
 */

package jkcemu.etc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.UserInputException;
import jkcemu.image.AbstractImageFrm;
import jkcemu.print.PrintUtil;


public class PlotterFrm extends AbstractImageFrm
{
  private static final String ACTION_COLOR_PAPER   = "color.paper";
  private static final String HELP_PAGE            = "/help/plotter.htm";
  private static final String PROP_CONFIRM_NEWPAGE = "confirm.newpage";

  private static PlotterFrm instance = null;

  private Plotter              plotter;
  private BufferedImage        image;
  private File                 file;
  private javax.swing.Timer    refreshTimer;
  private volatile boolean     dirty;
  private JMenuItem            mnuNewPage;
  private JMenuItem            mnuPrint;
  private JMenuItem            mnuSaveAs;
  private JMenuItem            mnuClose;
  private JMenuItem            mnuCopy;
  private JMenuItem            mnuHelpContent;
  private JRadioButtonMenuItem mnuPenThk1;
  private JRadioButtonMenuItem mnuPenThk2;
  private JRadioButtonMenuItem mnuPenThk3;
  private JRadioButtonMenuItem mnuPenThk4;
  private JRadioButtonMenuItem mnuPenThk5;
  private JMenuItem            mnuPenColor;
  private JMenuItem            mnuPaperColor;
  private JCheckBoxMenuItem    mnuConfirmNewPage;
  private JButton              btnNewPage;
  private JButton              btnPrint;
  private JButton              btnSaveAs;
  private JButton              btnRotateLeft;
  private JButton              btnRotateRight;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public void imageChanged( BufferedImage image )
  {
    this.image = image;
    this.imageFld.setImage( image );
    this.imageFld.repaint();
  }


  public static void open( Plotter plotter )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new PlotterFrm( plotter );
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public void setDirty()
  {
    this.dirty = true;
  }


  public void setPenThickness( int value )
  {
    switch( value ) {
      case 2:
	this.mnuPenThk2.setSelected( true );
	break;
      case 3:
	this.mnuPenThk3.setSelected( true );
	break;
      case 4:
	this.mnuPenThk4.setSelected( true );
	break;
      case 5:
	this.mnuPenThk5.setSelected( true );
	break;
      default:
	this.mnuPenThk1.setSelected( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    this.mnuConfirmNewPage.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			getSettingsPrefix() + PROP_CONFIRM_NEWPAGE,
			true ) );
    return super.applySettings( props );
  }


  @Override
  public void componentHidden( ComponentEvent e )
  {
    if( e.getComponent() == this ) {
      this.refreshTimer.stop();
    }
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    if( e.getComponent() == this ) {
      this.refreshTimer.start();
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnRotateLeft ) {
	rv = true;
	doRotateLeft();
      }
      else if( src == this.btnRotateRight ) {
	rv = true;
	doRotateRight();
      }
      if( (src == this.mnuNewPage) || (src == this.btnNewPage) ) {
	rv = true;
	doNewPage();
      }
      else if( (src == this.mnuPrint) || (src == this.btnPrint) ) {
	rv = true;
	if( getImage() != null ) {
	  PrintUtil.doPrint( this, this.imageFld, "Plotter" );
	}
      }
      else if( (src == this.mnuSaveAs) || (src == this.btnSaveAs) ) {
	rv = true;
	doSaveAs();
      }
      else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuCopy ) {
	rv = true;
	doCopy();
      }
      else if( (src == this.mnuPenThk1)
	       || (src == this.mnuPenThk2)
	       || (src == this.mnuPenThk3)
	       || (src == this.mnuPenThk4)
	       || (src == this.mnuPenThk5) )
      {
	rv = true;
	if( this.plotter != null ) {
	  int thk = 1;
	  if( this.mnuPenThk2.isSelected() ) {
	    thk = 2;
	  }
	  else if( this.mnuPenThk3.isSelected() ) {
	    thk = 3;
	  }
	  else if( this.mnuPenThk4.isSelected() ) {
	    thk = 4;
	  }
	  else if( this.mnuPenThk5.isSelected() ) {
	    thk = 5;
	  }
	  this.plotter.setPenThickness( thk );
	  Main.setProperty(
			Plotter.PROP_PEN_THICKNESS,
			Integer.toString( thk ) );
	}
      }
      else if( src == this.mnuPenColor ) {
	rv = true;
	doColorPen();
      }
      else if( src == this.mnuPaperColor ) {
	rv = true;
	doColorPaper();
      }
      else if( src == this.mnuConfirmNewPage ) {
	rv = true;
	Main.setProperty(
		getSettingsPrefix() + PROP_CONFIRM_NEWPAGE,
		Boolean.toString( this.mnuConfirmNewPage.isSelected() ) );
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  public BufferedImage getImage()
  {
    return this.image;
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {
      Plotter plotter = newEmuSys.getPlotter();
      if( plotter != null ) {
	if( plotter != this.plotter ) {
	  this.plotter = plotter;
	  this.plotter.setPlotterFrm( this );
	}
      } else {
	if( this.plotter != null ) {
	  this.plotter.setPlotterFrm( null );
	  this.plotter = null;
	}
	PlotterFrm.close();
      }
    }
  }


	/* --- Konstruktor --- */

  private PlotterFrm( Plotter plotter )
  {
    this.plotter = plotter;
    this.image   = null;
    this.file    = null;
    this.dirty   = false;
    setTitle( "JKCEMU Plotter" );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuNewPage = createMenuItemWithStandardAccelerator(
						"Neue Seite",
						KeyEvent.VK_N );
    mnuFile.add( this.mnuNewPage );

    this.mnuPrint = createMenuItemOpenPrint( true );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createMenuItemSaveAs( true );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();
    this.mnuCopy  = createMenuItemCopy( true );
    mnuEdit.add( this.mnuCopy );


    // Menu Einstellungen
    JMenu mnuSettings = createMenuSettings();

    JMenu mnuPenThk = GUIFactory.createMenu( "Stiftbreite" );
    mnuSettings.add( mnuPenThk );

    ButtonGroup grpPenThk = new ButtonGroup();

    this.mnuPenThk1 = GUIFactory.createRadioButtonMenuItem( "1 Pixel" );
    grpPenThk.add( this.mnuPenThk1 );
    mnuPenThk.add( this.mnuPenThk1 );

    this.mnuPenThk2 = GUIFactory.createRadioButtonMenuItem( "2 Pixel" );
    grpPenThk.add( this.mnuPenThk2 );
    mnuPenThk.add( this.mnuPenThk2 );

    this.mnuPenThk3 = GUIFactory.createRadioButtonMenuItem( "3 Pixel" );
    grpPenThk.add( this.mnuPenThk3 );
    mnuPenThk.add( this.mnuPenThk3 );

    this.mnuPenThk4 = GUIFactory.createRadioButtonMenuItem( "4 Pixel" );
    grpPenThk.add( this.mnuPenThk4 );
    mnuPenThk.add( this.mnuPenThk4 );

    this.mnuPenThk5 = GUIFactory.createRadioButtonMenuItem( "5 Pixel" );
    grpPenThk.add( this.mnuPenThk5 );
    mnuPenThk.add( this.mnuPenThk5 );

    this.mnuPenColor = createMenuItem( "Stiftfarbe..." );
    mnuSettings.add( this.mnuPenColor );

    this.mnuPaperColor = createMenuItem( "Papierfarbe..." );
    mnuSettings.add( this.mnuPaperColor );
    mnuSettings.addSeparator();

    this.mnuConfirmNewPage = GUIFactory.createCheckBoxMenuItem(
			"Best\u00E4tigung f\u00FCr neue Seite",
			true );
    mnuSettings.add( this.mnuConfirmNewPage );


    // Menu Hilfe
    JMenu mnuHelp       = createMenuHelp();
    this.mnuHelpContent = createMenuItem( "Hilfe zum Plotter..." );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar(
					mnuFile,
					mnuEdit,
					mnuSettings,
					mnuHelp ) );


    // Werkzeugleiste
    JPanel panelToolBar = GUIFactory.createPanel(
		new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelToolBar, BorderLayout.NORTH );

    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelToolBar.add( toolBar );

    this.btnNewPage = GUIFactory.createRelImageResourceButton(
					this,
					"file/new.png",
					this.mnuNewPage.getText() );
    toolBar.add( this.btnNewPage );
    toolBar.addSeparator();

    this.btnSaveAs = GUIFactory.createRelImageResourceButton(
					this,
					"file/save_as.png",
					this.mnuSaveAs.getText() );
    toolBar.add( this.btnSaveAs );

    this.btnPrint = GUIFactory.createRelImageResourceButton(
					this,
					"file/print.png",
					this.mnuPrint.getText() );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnRotateLeft = GUIFactory.createRelImageResourceButton(
					this,
					"edit/rotate_left.png",
					"Nach links drehen" );
    toolBar.add( this.btnRotateLeft );

    this.btnRotateRight = GUIFactory.createRelImageResourceButton(
					this,
					"edit/rotate_right.png",
					"Nach rechts drehen" );
    toolBar.add( this.btnRotateRight );
    toolBar.addSeparator();

    toolBar.add( createScaleComboBox( 25, 25, 33, 50, 75, 100 ) );
    doScaleView();


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }


    // sonstiges
    if( this.plotter != null ) {
      this.plotter.setPlotterFrm( this );
    } else {
      setPenThickness( Plotter.DEFAULT_PEN_THICKNESS );
    }
    updActionBtns();
    this.refreshTimer = new javax.swing.Timer(
		150,
		new ActionListener()
		{
		  public void actionPerformed( ActionEvent e )
		  {
		    repaintImage();
		  }
		} );

    // Listener
    this.mnuPenThk1.addActionListener( this );
    this.mnuPenThk2.addActionListener( this );
    this.mnuPenThk3.addActionListener( this );
    this.mnuPenThk4.addActionListener( this );
    this.mnuPenThk5.addActionListener( this );
    this.mnuConfirmNewPage.addActionListener( this );
    this.btnNewPage.addActionListener( this );
    this.btnSaveAs.addActionListener( this );
    this.btnPrint.addActionListener( this );
    this.btnRotateLeft.addActionListener( this );
    this.btnRotateRight.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doColorPaper()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      Color color = JColorChooser.showDialog(
				this,
				"Auswahl Papierfarbe",
				this.plotter.getPaperColor() );
      if( color != null ) {
	try {
	  plotter.setPaperColor( color );
	}
	catch( UserInputException ex ) {
	  BaseDlg.showInfoDlg( this, ex.getMessage() );
	}
	Main.setProperty(
		Plotter.PROP_PAPER_COLOR,
		String.format( "%08X", color.getRGB() ) );
      }
    }
  }


  private void doColorPen()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      Color color = JColorChooser.showDialog(
				this,
				"Auswahl Stiftfarbe",
				this.plotter.getPenColor() );
      if( color != null ) {
	try {
	  plotter.setPenColor( color );
	}
	catch( UserInputException ex ) {
	  BaseDlg.showInfoDlg( this, ex.getMessage() );
	}
	Main.setProperty(
		Plotter.PROP_PEN_COLOR,
		String.format( "%08X", color.getRGB() ) );
      }
    }
  }


  private void doNewPage()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      if( this.mnuConfirmNewPage.isSelected() ) {
	if( BaseDlg.showYesNoDlg( this, "Neue Seite?" ) ) {
	  plotter.newPage();
	}
      } else {
	plotter.newPage();
      }
    }
  }


  private void doSaveAs()
  {
    File file = saveAs( this.file );
    if( file != null ) {
      this.file = file;
    }
  }


  private boolean isClean()
  {
    Plotter plotter = this.plotter;
    return plotter != null ? plotter.isClean() : true;
  }


  private void repaintImage()
  {
    boolean dirty = this.dirty;
    this.dirty    = false;
    if( dirty ) {
      updActionBtns();
      this.imageFld.setImage( this.image );
      this.imageFld.repaint();
    }
  }


  private void updActionBtns()
  {
    boolean state = false;
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      state = !plotter.isClean();
    }
    this.mnuSaveAs.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuCopy.setEnabled( state );
    this.btnSaveAs.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnRotateLeft.setEnabled( state );
    this.btnRotateRight.setEnabled( state );
  }
}
