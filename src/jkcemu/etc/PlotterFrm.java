/*
 * (c) 2011-2017 Jens Mueller
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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.UserInputException;
import jkcemu.image.AbstractImageFrm;
import jkcemu.print.PrintUtil;


public class PlotterFrm extends AbstractImageFrm
{
  private static final String ACTION_CLOSE         = "close";
  private static final String ACTION_COLOR_PAPER   = "color.paper";
  private static final String ACTION_COLOR_PEN     = "color.pen";
  private static final String ACTION_HELP          = "help";
  private static final String HELP_PAGE            = "/help/plotter.htm";
  private static final String PROP_CONFIRM_NEWPAGE = "confirm.newpage";

  private static PlotterFrm instance = null;

  private Plotter              plotter;
  private Image                image;
  private File                 file;
  private javax.swing.Timer    refreshTimer;
  private volatile boolean     dirty;
  private JMenuItem            mnuNewPage;
  private JMenuItem            mnuPrint;
  private JMenuItem            mnuSaveAs;
  private JMenuItem            mnuCopy;
  private JRadioButtonMenuItem mnuPenThk1;
  private JRadioButtonMenuItem mnuPenThk2;
  private JRadioButtonMenuItem mnuPenThk3;
  private JRadioButtonMenuItem mnuPenThk4;
  private JRadioButtonMenuItem mnuPenThk5;
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


  public static void lazySetPlotter( Plotter plotter )
  {
    if( instance != null ) {
      if( instance.plotter != null ) {
	instance.plotter.setPlotterFrm( null );
      }
      instance.plotter = plotter;
      if( instance.plotter != null ) {
	instance.plotter.setPlotterFrm( instance );
      }
    }
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
  public boolean applySettings( Properties props, boolean resizable )
  {
    this.mnuConfirmNewPage.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			getSettingsPrefix() + PROP_CONFIRM_NEWPAGE,
			true ) );
    return super.applySettings( props, resizable );
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
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
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
	    PrintUtil.doPrint( this, this.imgFld, "Plotter" );
	  }
	}
	else if( (src == this.mnuSaveAs) || (src == this.btnSaveAs) ) {
	  rv = true;
	  doSaveAs();
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
	else if( src == this.mnuConfirmNewPage ) {
	  rv = true;
	  Main.setProperty(
		getSettingsPrefix() + PROP_CONFIRM_NEWPAGE,
		Boolean.toString( this.mnuConfirmNewPage.isSelected() ) );
	}
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.equals( ACTION_CLOSE ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( ACTION_COLOR_PEN ) ) {
	    rv = true;
	    doColorPen();
          }
	  else if( cmd.equals( ACTION_COLOR_PAPER ) ) {
	    rv = true;
	    doColorPaper();
          }
	  else if( cmd.equals( ACTION_HELP ) ) {
	    rv = true;
	    HelpFrm.open( HELP_PAGE );
          }
	}
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
    return this.plotter.getBufferedImage();
  }


	/* --- Konstruktor --- */

  private PlotterFrm( Plotter plotter )
  {
    this.plotter = plotter;
    this.dirty   = false;
    this.image   = null;
    this.file    = null;
    setTitle( "JKCEMU Plotter" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNewPage = createJMenuItem( "Neue Seite" );
    mnuFile.add( this.mnuNewPage );

    this.mnuPrint = createJMenuItem( "Drucken..." );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createJMenuItem( "Speichern unter..." );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    mnuFile.add( createJMenuItem( "Schlie\u00DFen", ACTION_CLOSE ) );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuCopy = createJMenuItem( "Kopieren" );
    mnuEdit.add( this.mnuCopy );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuEdit.setMnemonic( KeyEvent.VK_E );
    mnuBar.add( mnuSettings );

    JMenu mnuPenThk = new JMenu( "Stiftbreite" );
    mnuSettings.add( mnuPenThk );

    ButtonGroup grpPenThk = new ButtonGroup();

    this.mnuPenThk1 = new JRadioButtonMenuItem( "1 Pixel" );
    this.mnuPenThk1.addActionListener( this );
    grpPenThk.add( this.mnuPenThk1 );
    mnuPenThk.add( this.mnuPenThk1 );

    this.mnuPenThk2 = new JRadioButtonMenuItem( "2 Pixel" );
    this.mnuPenThk2.addActionListener( this );
    grpPenThk.add( this.mnuPenThk2 );
    mnuPenThk.add( this.mnuPenThk2 );

    this.mnuPenThk3 = new JRadioButtonMenuItem( "3 Pixel" );
    this.mnuPenThk3.addActionListener( this );
    grpPenThk.add( this.mnuPenThk3 );
    mnuPenThk.add( this.mnuPenThk3 );

    this.mnuPenThk4 = new JRadioButtonMenuItem( "4 Pixel" );
    this.mnuPenThk4.addActionListener( this );
    grpPenThk.add( this.mnuPenThk4 );
    mnuPenThk.add( this.mnuPenThk4 );

    this.mnuPenThk5 = new JRadioButtonMenuItem( "5 Pixel" );
    this.mnuPenThk5.addActionListener( this );
    grpPenThk.add( this.mnuPenThk5 );
    mnuPenThk.add( this.mnuPenThk5 );

    mnuSettings.add( createJMenuItem( "Stiftfarbe...", ACTION_COLOR_PEN ) );
    mnuSettings.add( createJMenuItem(
			"Papierfarbe...",
			ACTION_COLOR_PAPER ) );
    mnuSettings.addSeparator();
    this.mnuConfirmNewPage = new JCheckBoxMenuItem(
			"Best\u00E4tigung f\u00FCr neue Seite",
			true );
    this.mnuConfirmNewPage.addActionListener( this );
    mnuSettings.add( this.mnuConfirmNewPage );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );
    mnuHelp.add( createJMenuItem( "Hilfe...", ACTION_HELP ) );


    // Werkzeugleiste
    JPanel panelToolBar = new JPanel(
		new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelToolBar, BorderLayout.NORTH );

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelToolBar.add( toolBar );

    this.btnNewPage = createImageButton(
			"/images/file/new.png", "Neue Seite" );
    toolBar.add( this.btnNewPage );
    toolBar.addSeparator();

    this.btnSaveAs = createImageButton(
			"/images/file/save_as.png", "Speichern unter" );
    toolBar.add( this.btnSaveAs );

    this.btnPrint = createImageButton( "/images/file/print.png", "Drucken" );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnRotateLeft = createImageButton(
				"/images/edit/rotate_left.png",
				"Nach links drehen" );
    toolBar.add( this.btnRotateLeft );

    this.btnRotateRight = createImageButton(
				"/images/edit/rotate_right.png",
				"Nach rechts drehen" );
    toolBar.add( this.btnRotateRight );
    toolBar.addSeparator();

    toolBar.add( createScaleComboBox( 25, 25, 33, 50, 75, 100 ) );
    doScaleView();


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    if( this.plotter != null ) {
      this.plotter.setPlotterFrm( this );
    } else {
      setPenThickness( Plotter.DEFAULT_PEN_THICKNESS );
    }
    updActionBtns();
    this.refreshTimer = new javax.swing.Timer(
				100,
				new ActionListener()
				{
				  public void actionPerformed( ActionEvent e )
				  {
				    repaintImage();
				  }
				} );
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
    this.dirty = false;
    updActionBtns();
    repaint();
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
