/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige von Bildern
 */

package jkcemu.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.*;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import java.util.Stack;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileComparator;
import jkcemu.base.HelpFrm;
import jkcemu.print.PrintUtil;


public class ImageFrm extends AbstractImageFrm implements
						DropTargetListener,
						FlavorListener,
						MouseMotionListener
{
  private static final String PROP_AUTORESIZE = "auto_resize";
  private static final String PROP_BACKGROUND = "background";
  private static final String VALUE_BLACK     = "black";
  private static final String VALUE_GRAY      = "gray";
  private static final String VALUE_WHITE     = "white";
  private static final String VALUE_SYSTEM    = "system";
  private static final String HELP_PAGE       = "/help/tools/imageviewer.htm";

  private static final String ACTION_HISTORY_PREFIX = "history.";
  private static final String DEFAULT_STATUS_TEXT   = "Bereit";
  private static final int MAX_CONTENT_SIZE_PERCENT = 75;

  private static final String TEXT_A5105_FMT =
			"A5105-Format (320x200, 16 Farben)";
  private static final String TEXT_AC1_ACC_FMT =
			"AC1-ACC-Blockgrafik (384x256, Monochrom)";
  private static final String TEXT_AC1_SCCH_FMT =
			"AC1-SCCH-Blockgrafik (384x256, Monochrom)";
  private static final String TEXT_AC1_2010_FMT =
			"AC1-2010-Blockgrafik (384x256, Monochrom)";
  private static final String TEXT_KC854HIRES_FMT =
			"KC85/4-HIRES-Format (320x256, 4 Farben)";
  private static final String TEXT_LLC2HIRES_FMT =
			"LLC2-HIRES-Format (512x256, Monochrom)";
  private static final String TEXT_Z1013_FMT =
			"Z1013-Blockgrafik (256x256, Monochrom)";
  private static final String TEXT_Z9001_FMT =
			"Z9001-Blockgrafik (320x192, Monochrom)";


  private enum ExpFmt {
		IMG_A5105,
		IMG_LLC2HIRES,
		APP_AC1,
		APP_KC854HIRES,
		APP_LLC2HIRES,
		APP_Z1013,
		APP_Z9001,
		MEM_AC1,
		MEM_Z1013,
		MEM_Z9001 };


  private static ImageFrm instance = null;

  private int               lastRoundTopPixels;
  private int               lastRoundBottomPixels;
  private boolean           autoFitImage;
  private File[]            files;
  private Object            savedViewScale;
  private Point             selectionStart;
  private CropDlg           cropDlg;
  private Stack<ImgEntry>   imgStack;
  private JButton           btnOpen;
  private JButton           btnSaveAs;
  private JButton           btnPrint;
  private JButton           btnRotateLeft;
  private JButton           btnRotateRight;
  private JButton           btnFitImage;
  private JButton           btnPrev;
  private JButton           btnNext;
  private JMenuItem         mnuOpen;
  private JMenuItem         mnuSaveAs;
  private JMenuItem         mnuExpImgA5105;
  private JMenuItem         mnuExpImgLLC2Hires;
  private JMenuItem         mnuExpMemAC1;
  private JMenuItem         mnuExpMemZ1013;
  private JMenuItem         mnuExpMemZ9001;
  private JMenuItem         mnuExpAppAC1;
  private JMenuItem         mnuExpAppKC854Hires;
  private JMenuItem         mnuExpAppLLC2Hires;
  private JMenuItem         mnuExpAppZ1013;
  private JMenuItem         mnuExpAppZ9001;
  private JMenuItem         mnuExpColorTab;
  private JMenuItem         mnuImgProps;
  private JMenuItem         mnuPrint;
  private JMenuItem         mnuPrev;
  private JMenuItem         mnuNext;
  private JMenuItem         mnuClose;
  private JMenu             mnuEdit;
  private JMenu             mnuHistory;
  private JMenuItem         mnuCopy;
  private JMenuItem         mnuUndo;
  private JMenuItem         mnuPaste;
  private JMenuItem         mnuSelectArea;
  private JMenuItem         mnuScaleImage;
  private JMenuItem         mnuRotateImage;
  private JMenuItem         mnuFlipHorizontal;
  private JMenuItem         mnuRoundCorners;
  private JMenuItem         mnuAdjustImage;
  private JMenuItem         mnuIndexColors;
  private JMenuItem         mnuToGray;
  private JMenuItem         mnuToMonoFloydSteinberg;
  private JMenuItem         mnuToMonoSierra3;
  private JMenuItem         mnuToMonoAtkinson;
  private JMenuItem         mnuThreshold;
  private JMenuItem         mnuInvertImage;
  private JMenuItem         mnuRemoveTransparency;
  private JMenu             mnuConvert;
  private JMenuItem         mnuToA5105;
  private JMenuItem         mnuToAC1_ACC;
  private JMenuItem         mnuToAC1_SCCH;
  private JMenuItem         mnuToAC1_2010;
  private JMenuItem         mnuToKC854Hires;
  private JMenuItem         mnuToLLC2Hires21;
  private JMenuItem         mnuToLLC2Hires43;
  private JMenuItem         mnuToZ1013;
  private JMenuItem         mnuToZ9001;
  private JMenuItem         mnuBgSystem;
  private JMenuItem         mnuBgBlack;
  private JMenuItem         mnuBgGray;
  private JMenuItem         mnuBgWhite;
  private JMenuItem         mnuAutoResize;
  private JMenuItem         mnuHelpContent;
  private JLabel            labelStatus;
  private JComboBox<String> comboViewScale;


  public void fireFitImage()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    doFitImage( false );
		  }
		} );
  }


  public ImgFld getImgFld()
  {
    return this.imgFld;
  }


  public String getMenuPathTextReduceColors()
  {
    return String.format(
		"\'%s\' \u2192 \'%s\'",
		this.mnuEdit.getText(),
		this.mnuIndexColors.getText() );
  }


  public static void open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new ImageFrm();
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static void open( BufferedImage image, String title )
  {
    open();
    if( image != null ) {
      if( instance.confirmImageSaved() ) {
	ImgEntry.Mode   mode = ImgEntry.Mode.UNSPECIFIED;
	IndexColorModel icm  = ImgUtil.getIndexColorModel( image );
	if( icm != null ) {
	  boolean mono = true;
	  boolean gray = true;
	  int     n    = icm.getMapSize();
	  for( int i = 0; i < n; i++ ) {
	    int v = icm.getRGB( i );
	    int c = v & 0xFF;
	    if( (((v >> 16) & 0xFF) != c) || (((v >> 8) & 0xFF) != c) ) {
	      mono = false;
	      gray = false;
	      break;
	    }
	    if( (c != 0) && (c != 0xFF) ) {
	      mono = false;
	    }
	  }
	  if( mono ) {
	    mode = ImgEntry.Mode.MONOCHROME;
	  } else if( gray ) {
	    mode = ImgEntry.Mode.GRAY;
	  }
	}
	instance.showImageInternal(
				image,
				mode,
				ImgFld.Rotation.NONE,
				null,
				title,
				true,
				null,
				null );
	instance.updFileList();
      }
    }
  }


  public static void open( File file )
  {
    open();
    if( file != null ) {
      if( instance.confirmImageSaved() ) {
	instance.showImageFile( file );
      }
    }
  }


  public void setSelection( int x, int y, int w, int h )
  {
    this.imgFld.setSelection( x, y, w, h );
  }


  public void setSelectionColor( Color color )
  {
    this.imgFld.setSelectionColor( color );
  }


  public void showDerivatedImage( BufferedImage image, String title )
  {
    ImgEntry entry = getCurImgEntry();
    if( (entry != null) && (image != null) ) {
      showImageInternal(
		image,
		entry.getMode(),
		this.imgFld.getRotation(),
		null,
		title,
		false,
		null,
		null );
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      if( confirmImageSaved() ) {
	showImageFile( file );
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    if( e.getSource() == this.clipboard )
      updPasteBtn();
  }


	/* --- MouseMotionListener --- */

  @Override
  public void mouseDragged( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld ) {
      String text     = DEFAULT_STATUS_TEXT;
      Point  curPos   = toUnscaledPoint( e );
      Point  startPos = this.selectionStart;
      if( startPos != null ) {
	int x = startPos.x;
	int y = startPos.y;
	int w = curPos.x - startPos.x + 1;
	int h = curPos.y - startPos.y + 1;
	// Seitenverhaeltnis vorgegeben?
	Rectangle r = null;
	if( this.cropDlg != null ) {
	  if( this.cropDlg.isVisible() ) {
	    r = this.cropDlg.toRatio(
				startPos.x,
				startPos.y,
				curPos.x - startPos.x + 1,
				curPos.y - startPos.y + 1 );
	  }
	}
	if( r != null ) {
	  // Startpunkt soll erhalten bleiben
	  this.imgFld.setSelection( r );
	} else {
	  this.imgFld.setSelection( x, y, w, h );
	}
	r = this.imgFld.getSelectedArea();
	if( r != null ) {
	  if( this.cropDlg != null ) {
	    if( this.cropDlg.isVisible() ) {
	      this.cropDlg.setSelectedArea( r );
	    }
	  }
	  text = String.format(
			"Ausgew\u00E4hlter Bereich: X,Y=%d,%d  B,H=%d,%d",
			r.x,
			r.y,
			r.width,
			r.height );
	}
      } else {
	this.selectionStart = new Point( curPos.x, curPos.y );
      }
      this.labelStatus.setText( text );
      e.consume();
    }
  }


  @Override
  public void mouseMoved( MouseEvent e )
  {
    if( (e.getComponent() == this.imgFld)
	&& (this.imgFld.getSelection() == null) )
    {
      String        text  = DEFAULT_STATUS_TEXT;
      BufferedImage image = getImage();
      if( image != null ) {
	int w = image.getWidth();
	int h = image.getHeight();
	if( (w > 0) && (h > 0) ) {
	  Point p = toUnscaledPoint( e );
	  if( (p.x >= 0) && (p.x < w) && (p.y >= 0) && (p.y < h) ) {
	    if( image instanceof BufferedImage ) {
	      int pixelValue = image.getRGB( p.x, p.y );
	      if( image.getTransparency() == Transparency.OPAQUE ) {
		text = String.format(
				"X,Y=%d,%d  RGB=%d,%d,%d",
				p.x,
				p.y,
				(pixelValue >> 16) & 0xFF,
				(pixelValue >> 8) & 0xFF,
				pixelValue & 0xFF );
	      } else {
		text = String.format(
				"X,Y=%d,%d  RGB=%d,%d,%d  Alpha=%d",
				p.x,
				p.y,
				(pixelValue >> 16) & 0xFF,
				(pixelValue >> 8) & 0xFF,
				pixelValue & 0xFF,
				(pixelValue >> 24) & 0xFF );
	      }
	    } else {
	      text = String.format( "X=%d Y=%d", p.x, p.y );
	    }
	  }
	}
      }
      this.labelStatus.setText( text );
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = false;
    if( props != null ) {
      rv = super.applySettings( props, resizable );

      String prefix = getSettingsPrefix();

      this.mnuAutoResize.setSelected(
		EmuUtil.parseBoolean(
			props.getProperty( prefix + PROP_AUTORESIZE ),
			false ) );

      AbstractButton btn   = this.mnuBgSystem;
      Color          color = SystemColor.window;
      String         text  = props.getProperty( prefix + PROP_BACKGROUND );
      if( text != null ) {
	text = text.trim().toLowerCase();
	if( text.equals( VALUE_BLACK ) ) {
	  color = Color.black;
	  btn   = this.mnuBgBlack;
	}
	else if( text.equals( VALUE_GRAY ) ) {
	  color = Color.gray;
	  btn   = this.mnuBgGray;
	}
	else if( text.equals( VALUE_WHITE ) ) {
	  color = Color.white;
	  btn   = this.mnuBgWhite;
	}
      }
      btn.setSelected( true );
      this.imgFld.setBackground( color );
      this.imgFld.repaint();
    }
    return rv;
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    super.componentResized( e );
    if( this.autoFitImage ) {
      fitImage( false );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object src = e.getSource();
    if( src == this.btnRotateLeft ) {
      rv = true;
      doRotateLeft();
    }
    else if( src == this.btnRotateRight ) {
      rv = true;
      doRotateRight();
    }
    else if( src == this.btnFitImage ) {
      rv = true;
      doFitImage( true );
    }
    else if( (src == this.btnOpen) || (src == this.mnuOpen) ) {
      rv = true;
      doOpen();
    }
    else if( src == this.mnuClose ) {
      rv = true;
      doClose();
    }
    else if( (src == this.btnSaveAs) || (src == this.mnuSaveAs) ) {
      rv = true;
      doSaveAs();
    }
    else if( src == this.mnuExpImgA5105 ) {
      rv = true;
      doExport( ExpFmt.IMG_A5105 );
    }
    else if( src == this.mnuExpImgLLC2Hires ) {
      rv = true;
      doExport( ExpFmt.IMG_LLC2HIRES );
    }
    else if( src == this.mnuExpAppAC1 ) {
      rv = true;
      doExport( ExpFmt.APP_AC1 );
    }
    else if( src == this.mnuExpAppKC854Hires ) {
      rv = true;
      doExport( ExpFmt.APP_KC854HIRES );
    }
    else if( src == this.mnuExpAppLLC2Hires ) {
      rv = true;
      doExport( ExpFmt.APP_LLC2HIRES );
    }
    else if( src == this.mnuExpAppZ1013 ) {
      rv = true;
      doExport( ExpFmt.APP_Z1013 );
    }
    else if( src == this.mnuExpAppZ9001 ) {
      rv = true;
      doExport( ExpFmt.APP_Z9001 );
    }
    else if( src == this.mnuExpMemAC1 ) {
      rv = true;
      doExport( ExpFmt.MEM_AC1 );
    }
    else if( src == this.mnuExpMemZ1013 ) {
      rv = true;
      doExport( ExpFmt.MEM_Z1013 );
    }
    else if( src == this.mnuExpMemZ9001 ) {
      rv = true;
      doExport( ExpFmt.MEM_Z9001 );
    }
    else if( src == this.mnuExpColorTab ) {
      rv = true;
      doExportColorTab();
    }
    else if( src == this.mnuImgProps ) {
      rv = true;
      doImgProps();
    }
    else if( (src == this.btnPrint) || (src == this.mnuPrint) ) {
      rv = true;
      if( getImage() != null ) {
	PrintUtil.doPrint( this, this.imgFld, "Bildbetrachter" );
      }
    }
    else if( (src == this.btnPrev) || (src == this.mnuPrev) ) {
      rv = true;
      doPrev();
    }
    else if( (src == this.btnNext) || (src == this.mnuNext) ) {
      rv = true;
      doNext();
    }
    else if( src == this.mnuCopy ) {
      rv = true;
      doCopy();
    }
    else if( src == this.mnuPaste ) {
      rv = true;
      doPaste();
    }
    else if( src == this.mnuRoundCorners ) {
      rv = true;
      doRoundCorners();
    }
    else if( src == this.mnuSelectArea ) {
      rv = true;
      doSelectArea();
    }
    else if( src == this.mnuScaleImage ) {
      rv = true;
      doScaleImage();
    }
    else if( src == this.mnuRotateImage ) {
      rv = true;
      doRotateImage();
    }
    else if( src == this.mnuFlipHorizontal ) {
      rv = true;
      doFlipHorizontal();
    }
    else if( src == this.mnuAdjustImage ) {
      rv = true;
      doAdjustImage();
    }
    else if( src == this.mnuIndexColors ) {
      rv = true;
      doIndexColors();
    }
    else if( src == this.mnuToGray ) {
      rv = true;
      doToGray();
    }
    else if( src == this.mnuToMonoFloydSteinberg ) {
      rv = true;
      doToMonochrome( Dithering.Algorithm.FLOYD_STEINBERG );
    }
    else if( src == this.mnuToMonoSierra3 ) {
      rv = true;
      doToMonochrome( Dithering.Algorithm.SIERRA3 );
    }
    else if( src == this.mnuToMonoAtkinson ) {
      rv = true;
      doToMonochrome( Dithering.Algorithm.ATKINSON );
    }
    else if( src == this.mnuThreshold ) {
      rv = true;
      doThreshold();
    }
    else if( src == this.mnuInvertImage ) {
      rv = true;
      doInvertImage();
    }
    else if( src == this.mnuRemoveTransparency ) {
      rv = true;
      doRemoveTransparency();
    }
    else if( src == this.mnuToA5105 ) {
      rv = true;
      doToA5105();
    }
    else if( src == this.mnuToAC1_ACC ) {
      rv = true;
      doToAC1(
	"AC1-ACC-Blockgrafik",
	"/rom/ac1/accfont.bin",
	ImgEntry.Mode.AC1_ACC );
    }
    else if( src == this.mnuToAC1_SCCH ) {
      rv = true;
      doToAC1(
	"AC1-SCCH-Blockgrafik",
	"/rom/ac1/scchfont.bin",
	ImgEntry.Mode.AC1_SCCH );
    }
    else if( src == this.mnuToAC1_2010 ) {
      rv = true;
      doToAC1(
	"AC1-2010-Blockgrafik",
	"/rom/ac1/font2010.bin",
	ImgEntry.Mode.AC1_2010 );
    }
    else if( src == this.mnuToKC854Hires ) {
      rv = true;
      doToKC854Hires();
    }
    else if( src == this.mnuToLLC2Hires21 ) {
      rv = true;
      doToLLC2Hires( false );
    }
    else if( src == this.mnuToLLC2Hires43 ) {
      rv = true;
      doToLLC2Hires( true );
    }
    else if( src == this.mnuToZ1013 ) {
      rv = true;
      doToZ1013();
    }
    else if( src == this.mnuToZ9001 ) {
      rv = true;
      doToZ9001();
    }
    else if( src == this.mnuUndo ) {
      rv = true;
      doUndo();
    }
    else if( src == this.mnuBgSystem ) {
      rv = true;
      this.imgFld.setBackground( SystemColor.window );
      this.imgFld.repaint();
    }
    else if( src == this.mnuBgBlack ) {
      rv = true;
      this.imgFld.setBackground( Color.black );
      this.imgFld.repaint();
    }
    else if( src == this.mnuBgGray ) {
      rv = true;
      this.imgFld.setBackground( Color.gray );
      this.imgFld.repaint();
    }
    else if( src == this.mnuBgWhite ) {
      rv = true;
      this.imgFld.setBackground( Color.white );
      this.imgFld.repaint();
    }
    else if( src == this.mnuHelpContent ) {
      rv = true;
      HelpFrm.open( HELP_PAGE );
    }
    if( !rv && (e instanceof ActionEvent) ) {
      String cmd = ((ActionEvent) e).getActionCommand();
      if( cmd != null ) {
	int len = ACTION_HISTORY_PREFIX.length();
	if( (cmd.length() > len)
	    && cmd.startsWith( ACTION_HISTORY_PREFIX ) )
	{
	  try {
	    doRollbackTo( Integer.parseInt( cmd.substring( len ) ) );
	    rv = true;
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( confirmImageSaved() ) {
      rv = super.doClose();
      if( rv ) {
	if( !Main.checkQuit( this ) ) {
	  // damit beim erneuten Oeffnen das Fenster leer ist
	  clearContent();
	}
      }
    }
    return rv;
  }


  @Override
  protected void doRotateLeft()
  {
    super.doRotateLeft();
    if( this.autoFitImage ) {
      fitImage( false );
    }
    updCropDlg();
  }


  @Override
  protected void doRotateRight()
  {
    super.doRotateRight();
    if( this.autoFitImage ) {
      fitImage( false );
    }
    updCropDlg();
  }


  @Override
  protected void doScaleView()
  {
    super.doScaleView();
    this.autoFitImage = false;
    this.btnFitImage.setEnabled( true );
  }


  @Override
  protected BufferedImage getImage()
  {
    return this.imgStack.isEmpty() ? null : this.imgStack.peek().getImage();
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld )
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld ) {
      this.imgFld.setSelection( null );
      this.imgFld.requestFocus();
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld )
      this.selectionStart = null;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );

      String prefix = getSettingsPrefix();

      props.setProperty(
		prefix + PROP_AUTORESIZE,
		String.valueOf( this.mnuAutoResize.isSelected() ) );

      String colorText = VALUE_SYSTEM;
      if( this.mnuBgBlack.isSelected() ) {
	colorText = VALUE_BLACK;
      }
      else if( this.mnuBgGray.isSelected() ) {
	colorText = VALUE_GRAY;
      }
      else if( this.mnuBgWhite.isSelected() ) {
	colorText = VALUE_WHITE;
      }
      props.setProperty( prefix + PROP_BACKGROUND, colorText );
    }
  }


  @Override
  protected void updWindowSize()
  {
    if( this.mnuAutoResize.isSelected() ) {
      Dimension imgSize = this.imgFld.getRotatedImageSize();
      if( imgSize != null ) {
	int wImg = imgSize.width;
	int hImg = imgSize.height;
	if( (wImg > 0) && (hImg > 0) ) {
	  removeComponentListener( this );
	  double    scale   = this.imgFld.getScale();
	  wImg              = (int) Math.round( (double) wImg * scale );
	  hImg              = (int) Math.round( (double) hImg * scale );
	  Dimension maxSize = getMaxContentSize();
	  if( (wImg > maxSize.width) || (hImg > imgSize.width) ) {
	    setPreferredSize(
		new Dimension(
			Math.min( wImg, maxSize.width ),
			Math.min( hImg, maxSize.height ) ) );
	  }
	  pack();
	  setPreferredSize( null );
	  addComponentListener( this );
	}
      }
    }
  }


	/* --- Aktionen --- */

  private void doExport( ExpFmt expFmt )
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      javax.swing.filechooser.FileFilter fileFilter  = null;
      String                             fileExt     = "";
      String                             formatText  = "";
      boolean                            softwareDir = false;
      BufferedImage                      image       = entry.getImage();
      boolean                            status      = false;
      switch( expFmt ) {
	case IMG_A5105:
	  formatText = "A5105-";
	  if( entry.isA5105Format() ) {
	    fileFilter = ImgUtil.createA5105ImageFileFilter();
	    fileExt    = ".scr";
	    status     = true;
	  }
	  break;
	case APP_AC1:
	case MEM_AC1:
	  formatText  = "AC1-";
	  softwareDir = true;
	  if( entry.isAC1Format()) {
	    if( expFmt.equals( ExpFmt.APP_AC1 ) ) {
	      fileFilter = EmuUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = EmuUtil.getBinaryFileFilter();
	      fileExt    = "_1000_17FF.bin";
	    }
	    status = true;
	  }
	  break;
	case APP_KC854HIRES:
	  formatText  = "KC85/4-HIRES-";
	  softwareDir = true;
	  if( entry.isKC854HiresFormat() ) {
	    fileFilter = EmuUtil.getKCSystemFileFilter();
	    fileExt    = ".kcc";
	    status     = true;
	  }
	  break;
	case APP_LLC2HIRES:
	case IMG_LLC2HIRES:
	  formatText  = "LLC2-HIRES-";
	  softwareDir = true;
	  if( entry.isLLC2HiresFormat() ) {
	    if( entry.getMemBytes() == null ) {
	      entry.setMemBytes( ImgUtil.createLLC2HiresMemBytes( image ) );
	    }
	    if( expFmt.equals( ExpFmt.APP_LLC2HIRES ) ) {
	      fileFilter = EmuUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = ImgUtil.createLLC2HiresImageFileFilter();
	      fileExt    = ".pix";
	    }
	    status = true;
	  }
	  break;
	case APP_Z1013:
	case MEM_Z1013:
	  formatText  = "Z1013-";
	  softwareDir = true;
	  if( entry.isZ1013Format() ) {
	    if( expFmt.equals( ExpFmt.APP_Z1013 ) ) {
	      fileFilter = EmuUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = EmuUtil.getBinaryFileFilter();
	      fileExt    = "_EC00_EFFF.bin";
	    }
	    status = true;
	  }
	  break;
	case APP_Z9001:
	case MEM_Z9001:
	  formatText  = "Z9001-";
	  softwareDir = true;
	  if( entry.isZ9001Format()) {
	    if( expFmt.equals( ExpFmt.APP_Z9001 ) ) {
	      fileFilter = EmuUtil.getKCSystemFileFilter();
	      fileExt    = ".kcc";
	    } else {
	      fileFilter = EmuUtil.getBinaryFileFilter();
	      fileExt    = "_EC00_EFBF.bin";
	    }
	    status = true;
	  }
	  break;
      }
      if( status ) {
	if( !isUnrotated() ) {
	  BaseDlg.showInfoDlg(
		this,
		"Das Bild wird ohne die gerade angezeigte Drehung"
			+ " exportiert,\n"
			+ "da bei dem Dateiformat Breite und H\u00F6he"
			+ " nicht \u00E4nderbar sind.\n"
			+ "Wenn Sie das Bild gedreht exportieren"
			+ " m\u00F6chten,\n"
			+ "m\u00FCssen Sie es zuerst drehen,"
			+ " und dann mit der entsprechenden\n"
			+ "Funktion im Men\u00FC \""
			+ this.mnuEdit.getText()
			+ "\" \u2192 \""
			+ this.mnuConvert.getText() + "\"\n"
			+ "in das gew\u00FCnschte Format umwandeln." );
	}
	String fileName = null;
	File   dirFile  = null;
	File   lastFile = entry.getFile();
	if( lastFile != null ) {
	  fileName = lastFile.getName();
	  dirFile  = lastFile.getParentFile();
	} else {
	  int idx = this.imgStack.size() - 1;
	  while( (lastFile == null) && (idx >= 0) ) {
	    lastFile = this.imgStack.get( idx ).getFile();
	    --idx;
	  }
	  if( lastFile != null ) {
	    fileName = lastFile.getName();
	  }
	  dirFile = Main.getLastDirFile(
				softwareDir ?
					Main.FILE_GROUP_SOFTWARE
					: Main.FILE_GROUP_IMAGE );
	}
	if( fileName != null ) {
	  int pos = fileName.lastIndexOf( '.' );
	  if( pos >= 0 ) {
	    fileName = fileName.substring( 0, pos );
	  }
	}
	if( fileName != null ) {
	  if( fileName.isEmpty() ) {
	    fileName = null;
	  }
	}
	if( fileName == null ) {
	  fileName = "noname";
	}
	fileName  = fileName + fileExt;
	File file = EmuUtil.showFileSaveDlg(
			this,
			formatText + "Bild exportieren",
			dirFile != null ?
				new File( dirFile, fileName )
				: new File( fileName ),
			fileFilter );
	if( file != null ) {
	  try {
	    switch( expFmt ) {
	      case IMG_A5105:
		ImgSaver.saveImageA5105( this, image, file );
		break;
	      case IMG_LLC2HIRES:
		ImgUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case APP_AC1:
		if( entry.equalsMode( ImgEntry.Mode.AC1_ACC ) ) {
		  ImgSaver.saveAppAC1ACC( entry.getMemBytes(), file );
		} else {
		  ImgSaver.saveAppAC1SCCH( entry.getMemBytes(), file );
		}
		break;
	      case APP_KC854HIRES:
		ImgSaver.saveAppKC854Hires( image, file );
		break;
	      case APP_LLC2HIRES:
		ImgSaver.saveAppLLC2Hires( entry.getMemBytes(), file );
		break;
	      case APP_Z1013:
		ImgSaver.saveAppZ1013( entry.getMemBytes(), file );
		break;
	      case APP_Z9001:
		ImgSaver.saveAppZ9001( entry.getMemBytes(), file );
		break;
	      case MEM_AC1:
		ImgUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case MEM_Z1013:
		ImgUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case MEM_Z9001:
		ImgUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      default:
		status = false;
	    }
	    if( status ) {
	      imageSaved(
			file,
			softwareDir ?
				Main.FILE_GROUP_SOFTWARE
				: Main.FILE_GROUP_IMAGE );
	    }
	  }
	  catch( IOException ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	}
      } else {
	BaseDlg.showErrorDlg(
		this,
		"Sie m\u00FCssen das Bild zuerst mit der entsprechenden"
			+ " Funktion\n"
			+ "im Men\u00FC \""
			+ this.mnuEdit.getText()
			+ "\" \u2192 \""
			+ this.mnuConvert.getText() + "\"\n"
			+ "in das " + formatText + "Format umwandeln,\n"
			+ "bevor Sie es in dem Format exportieren"
			+ " k\u00F6nnen.\n"
			+ "Lesen Sie dazu bitte auch die Hinweise"
			+ " in der Hilfe!",
		"Exportieren" );
      }
    }
  }


  private void doExportColorTab()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      int             colorCnt = 0;
      IndexColorModel icm      = ImgUtil.getIndexColorModel( image );
      if( icm != null ) {
	colorCnt = icm.getMapSize();
	if( colorCnt > 0 ) {
	  File file = EmuUtil.showFileSaveDlg(
				this,
				"Farbpalette exportieren",
				Main.getLastDirFile( Main.FILE_GROUP_IMAGE ),
				IFFFile.getPaletteFileFilter(),
				JASCPaletteFile.getFileFilter() );
	  if( file != null ) {
	    try {
	      if( JASCPaletteFile.accept( file ) ) {
		JASCPaletteFile.write( file, icm );
	      } else if( IFFFile.accept( file ) ) {
		IFFFile.writePalette( file, icm );
	      } else {
		throw new IOException(
			ImgUtil.createFileSuffixNotSupportedMsg(
					JASCPaletteFile.getFileSuffixes(),
					IFFFile.getFileSuffixes() ) );
	      }
	      Main.setLastFile( file, Main.FILE_GROUP_IMAGE );
	    }
	    catch( IOException ex ) {
	      BaseDlg.showErrorDlg( this, ex );
	    }
	  }
	}
      }
      if( colorCnt == 0 ) {
	BaseDlg.showErrorDlg(
		this,
		"Das Bild hat keine indexierten Farben,\n"
			+ "die als Farbpalette exportiert"
			+ " werden k\u00F6nnten." );
      }
    }
  }


  private void doOpen()
  {
    if( instance.confirmImageSaved() ) {
      File file = EmuUtil.showFileOpenDlg(
			this,
			"Bilddatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_IMAGE ),
			ImgLoader.createFileFilter() );
      if( file != null ) {
	if( showImageFile( file ) ) {
	  Main.setLastFile( file, Main.FILE_GROUP_IMAGE );
	}
      }
    }
  }


  private void doPrev()
  {
    if( confirmImageSaved() ) {
      boolean done = false;
      if( !this.imgStack.isEmpty() ) {
	File    file  = this.imgStack.get( 0 ).getFile();  // vom Basisbild!
	File[]  files = this.files;
	if( (file != null) && (files != null) ) {
	  File dirFile = file.getParentFile();
	  if( (dirFile != null) && (files.length > 0) ) {
	    try {
	      int idx = Arrays.binarySearch( files, file );
	      if( idx < 0 ) {
		idx = -(idx + 1);
	      }
	      --idx;
	      if( idx < 0 ) {
		idx = files.length - 1;
	      }
	      for( int i = idx; i >= 0; --i ) {
		File f = files[ i ];
		if( !f.equals( file ) ) {
		  if( ImgLoader.accept( f ) ) {
		    showImageFile( f );
		    done = true;
		    break;
		  }
		}
	      }
	    }
	    catch( ClassCastException ex ) {}
	  }
	}
      }
      if( !done ) {
	showNoMoreImageFileFound();
      }
    }
  }


  private void doNext()
  {
    if( confirmImageSaved() ) {
      boolean done = false;
      if( !this.imgStack.isEmpty() ) {
	File    file  = this.imgStack.get( 0 ).getFile();  // vom Basisbild!
	File[]  files = this.files;
	if( (file != null) && (files != null) ) {
	  File dirFile = file.getParentFile();
	  if( (dirFile != null) && (files.length > 0) ) {
	    try {
	      int idx = Arrays.binarySearch( files, file );
	      if( idx >= 0 ) {
		idx++;
	      } else {
		idx = -(idx + 1);
	      }
	      if( idx >= files.length ) {
		idx = 0;
	      }
	      for( int i = idx; i < files.length; i++ ) {
		File f = files[ i ];
		if( !f.equals( file ) ) {
		  if( ImgLoader.accept( f ) ) {
		    showImageFile( f );
		    done = true;
		    break;
		  }
		}
	      }
	    }
	    catch( ClassCastException ex ) {}
	  }
	}
      }
      if( !done ) {
	showNoMoreImageFileFound();
      }
    }
  }


  private void doImgProps()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      ImgPropsDlg.showDlg( this, entry.getImage(), entry.getFile() );
    }
  }


  private void doFitImage( boolean allowScaleUp )
  {
    this.autoFitImage = true;
    fitImage( allowScaleUp );
  }


  private void doPaste()
  {
    if( this.clipboard != null ) {
      try {
	if( this.clipboard.isDataFlavorAvailable( DataFlavor.imageFlavor ) ) {
	  Object o = this.clipboard.getData( DataFlavor.imageFlavor );
	  if( o != null ) {
	    if( o instanceof Image ) {
	      if( confirmImageSaved() ) {
		BufferedImgBuilder builder = new BufferedImgBuilder( this );
		BufferedImage      image   = builder.buildFrom(
						(Image) o,
						"Bild einf\u00FCgen..." );
		if( image != null ) {
		  showImageInternal(
				image,
				builder.getMode(),
				ImgFld.Rotation.NONE,
				null,
				"Eingef\u00FCgtes Bild",
				true,
				null,
				null );
		  updFileList();
		}
	      }
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
  }


  private void doSaveAs()
  {
    int nEntries = this.imgStack.size();
    if( nEntries > 0 ) {
      ImgEntry entry = this.imgStack.peek();
      File     file  = null;
      int      idx   = nEntries - 1;
      while( (file == null) && (idx >= 0) ) {
	file = this.imgStack.get( idx ).getFile();
	--idx;
      }
      if( file != null ) {
	String fileName = file.getName();
	if( fileName != null ) {
	  if( fileName.isEmpty() ) {
	    fileName = null;
	  }
	}
	if( fileName != null ) {
	  int fileNum = this.imgStack.size() - 1;
	  if( fileNum > 0 ) {
	    String baseExt  = String.format( "_(%d)", fileNum );
	    String baseName = fileName;
	    String fileExt  = "";
	    int    pos      = fileName.lastIndexOf( '.' );
	    if( pos >= 0 ) {
	      baseName = fileName.substring( 0, pos );
	      fileExt  = fileName.substring( pos );
	    }
	    if( !baseName.endsWith( baseExt ) ) {
	      baseName += baseExt;
	    }
	    fileName = baseName + fileExt;
	  }
	  File parent = file.getParentFile();
	  if( parent != null ) {
	    file = new File( parent, fileName );
	  } else {
	    file = new File( fileName );
	  }
	}
      }
      file = saveAs( file );
      if( file != null ) {
	imageSaved( file, Main.FILE_GROUP_IMAGE );
      }
    }
  }


  private void doFlipHorizontal()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      int w = entry.getWidth();
      int h = entry.getHeight();
      if( (w > 0) && (h > 0) ) {
	BufferedImage newImg = ImgUtil.createCompatibleImage(
							entry.getImage(),
							w,
							h );
	if( newImg != null ) {
	  Graphics g = newImg.createGraphics();
	  g.drawImage( entry.getImage(), w, 0, -w, h, this );
	  g.dispose();
	  double viewScale = getViewScale();
	  showImageInternal(
		newImg,
		entry.getMode(),
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Horizontal gespiegelt",
		false,
		null,
		null );
	}
      }
    }
  }


  private void doRotateImage()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      saveView();
      BufferedImage image = RotateDlg.showDlg( this );
      if( image != null ) {
	boolean autoFitImage = this.autoFitImage;
	double  viewScale    = getViewScale();
	showImageInternal(
		image,
		entry.getMode(),
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Gedreht",
		false,
		null,
		null );
	doFitImage( false );
	this.autoFitImage = autoFitImage;
      } else {
	restoreView();
      }
    }
  }


  private void doRoundCorners()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      RoundCornersDlg dlg = new RoundCornersDlg(
					this,
					this.lastRoundTopPixels,
					this.lastRoundBottomPixels );
      dlg.setVisible( true );
      int nTopPixels    = dlg.getNumTopPixels();
      int nBottomPixels = dlg.getNumBottomPixels();
      if( (nTopPixels > 0) || (nBottomPixels > 0) ) {
	BufferedImage newImg = ImgUtil.roundCorners(
						this,
						entry.getImage(),
						nTopPixels,
						nBottomPixels );
	if( newImg != null ) {
	  showImageInternal(
			newImg,
			entry.getMode(),
			this.imgFld.getRotation(),
			getViewScale(),
			"Ecken abgerundet",
			false,
			null,
			null );
	}
	this.lastRoundTopPixels    = nTopPixels;
	this.lastRoundBottomPixels = nBottomPixels;
      }
    }
  }


  private void doScaleImage()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      BufferedImage newImg = ScaleDlg.showDlg( this, entry.getImage() );
      if( newImg != null ) {
	showImageInternal(
			newImg,
			ImgEntry.probeMode( newImg ),
			this.imgFld.getRotation(),
			null,
			"Skaliert",
			false,
			null,
			null );
      }
    }
  }


  private void doSelectArea()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      int w = image.getWidth();
      int h = image.getHeight();
      if( (w > 0) && (h > 0) ) {
	Rectangle r = this.imgFld.getSelectedArea();
	if( this.cropDlg == null ) {
	  this.cropDlg = new CropDlg( this );
	}
	this.cropDlg.setImageSize( w, h );
	this.cropDlg.setSelectedArea( r );
	this.cropDlg.setVisible( true );
	this.cropDlg.toFront();
	this.imgFld.setSelection( r );
      }
    }
  }


  private void doAdjustImage()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      saveView();
      BufferedImage image = ImgAdjustDlg.showDlg( this );
      if( image != null ) {
	double viewScale = getViewScale();
	showImageInternal(
		image,
		entry.getMode() == ImgEntry.Mode.INDEXED_COLORS ?
			ImgEntry.Mode.INDEXED_COLORS 
			: ImgEntry.Mode.UNSPECIFIED,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Helligkeit,  Kontrast, Farben eingestellt",
		false,
		null,
		null );
      } else {
	restoreView();
      }
    }
  }


  private void doIndexColors()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      BufferedImage newImg = IndexColorsDlg.showDlg( this, image );
      if( newImg != null ) {
	String          title = "Indexierte Farben";
	IndexColorModel icm   = ImgUtil.getIndexColorModel( newImg );
	if( icm != null ) {
	  int nColors = icm.getMapSize();
	  if( nColors > 0 ) {
	    title = String.format( "%d indexierte Farben", nColors );
	  }
	}
	if( newImg != null ) {
	  double viewScale = getViewScale();
	  showImageInternal(
		newImg,
		ImgEntry.Mode.INDEXED_COLORS,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		title,
		false,
		null,
		null );
	}
      }
    }
  }


  private void doToGray()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isAGrayMode() || entry.isAMonochromeMode() ) {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits in Graustufen" );
      } else {
	BufferedImage newImg = GrayScaler.toGray( this, entry.getImage() );
	if( newImg != null ) {
	  double viewScale = getViewScale();
	  showImageInternal(
		newImg,
		ImgEntry.Mode.GRAY,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Graustufen",
		false,
		null,
		null );
	}
      }
    }
  }


  private void doToMonochrome( Dithering.Algorithm algorithm )
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isAMonochromeMode() ) {
	showImgAlreadyMonochrome();
      } else {
	BufferedImage newImg = Dithering.work(
					this,
					entry.getImage(),
					ImgUtil.getColorModelBW(),
					algorithm );
	if( newImg != null ) {
	  double viewScale = getViewScale();
	  showImageInternal(
		newImg,
		ImgEntry.Mode.MONOCHROME,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Monochrom",
		false,
		null,
		null );
	}
      }
    }
  }


  private void doThreshold()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.equalsMode( ImgEntry.Mode.MONOCHROME ) ) {
	showImgAlreadyMonochrome();
      } else {
	saveView();
	BufferedImage newImg = ThresholdDlg.showDlg( this );
	if( newImg != null ) {
	  double viewScale = getViewScale();
	  showImageInternal(
		newImg,
		ImgEntry.Mode.MONOCHROME,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Monochrom",
		false,
		null,
		null );
	} else {
	  restoreView();
	}
      }
    }
  }


  private void doInvertImage()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isAInversionMode() ) {
	BaseDlg.showErrorDlg(
		this,
		"Das Bild wurde bereits invertiert." );
      } else {
	BufferedImage retImg = ColorInverter.work( this, entry.getImage() );
	if( retImg != null ) {
	  ImgEntry.Mode newMode = ImgEntry.Mode.INVERTED;
	  switch( entry.getMode() ) {
	    case INDEXED_COLORS:
	      newMode = ImgEntry.Mode.INVERTED_INDEXED_COLORS;
	      break;
	    case GRAY:
	      newMode = ImgEntry.Mode.INVERTED_GRAY;
	      break;
	    case MONOCHROME:
	      newMode = ImgEntry.Mode.INVERTED_MONOCHROME;
	      break;
	  }
	  double viewScale = getViewScale();
	  showImageInternal(
		retImg,
		newMode,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Invertiert",
		false,
		null,
		null );
	}
      }
    }
  }


  private void doRemoveTransparency()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      if( image.getTransparency() != Transparency.OPAQUE ) {
	BufferedImage retImg = RemoveTransparencyDlg.showDlg(
							this,
							image );
	if( retImg != null ) {
	  double viewScale = getViewScale();
	  showImageInternal(
		retImg,
		ImgEntry.Mode.UNSPECIFIED,
		this.imgFld.getRotation(),
		!this.autoFitImage && viewScale > 0.0 ? viewScale : null,
		"Transparenz entfernt",
		false,
		null,
		null );
	}
      }
    }
  }


  private void doToA5105()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isA5105Format() && isUnrotated() ) {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits im A5105-Format." );
      } else {
	showImageInternal(
		drawImageTo(
			entry.getImage(),
			null,
			new BufferedImage(
				ImgUtil.A5105_W,
				ImgUtil.A5105_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelA5105() ) ),
		ImgEntry.Mode.A5105,
		ImgFld.Rotation.NONE,
		null,
		TEXT_A5105_FMT,
		false,
		null,
		null );
      }
    }
  }


  private void doToAC1(
		String        title,
		String        fontResource,
		ImgEntry.Mode mode )
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.equalsMode( mode )
	  && entry.isAC1Format()
	  && isUnrotated() )
      {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits in dem AC1-Format." );
      } else {
	toMonochromCharImage(
			entry.getImage(),
			title,
			fontResource,
			ImgUtil.AC1_W,
			ImgUtil.AC1_H,
			mode,
			6,
			true );
      }
    }
  }


  private void doToKC854Hires()
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isKC854HiresFormat() && isUnrotated() ) {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits im KC85/4-HIRES-Format." );
      } else {
	showImageInternal(
		drawImageTo(
			entry.getImage(),
			null,
			new BufferedImage(
				ImgUtil.KC85_W,
				ImgUtil.KC85_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelKC854Hires() ) ),
		ImgEntry.Mode.KC854HIRES,
		ImgFld.Rotation.NONE,
		null,
		TEXT_KC854HIRES_FMT,
		false,
		null,
		null );
      }
    }
  }


  private void doToLLC2Hires( boolean for43 )
  {
    ImgEntry entry = getCurImgEntry();
    if( entry != null ) {
      if( entry.isLLC2HiresFormat() && isUnrotated() ) {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits im LLC2-HIRES-Format." );
      } else {
	Float ratioCorrection = null;
	if( for43 ) {
	  ratioCorrection = 3F/2F;
	}
	showImageInternal(
		drawImageTo(
			entry.getImage(),
			ratioCorrection,
			new BufferedImage(
				ImgUtil.LLC2_W,
				ImgUtil.LLC2_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelBW() ) ),
		ImgEntry.Mode.MONOCHROME,
		ImgFld.Rotation.NONE,
		null,
		TEXT_LLC2HIRES_FMT,
		false,
		null,
		null );
      }
    }
  }


  private void doToZ1013()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      toMonochromCharImage(
			image,
			"Z1013-Blockgrafik",
			"/rom/z1013/z1013font.bin",
			ImgUtil.Z1013_W,
			ImgUtil.Z1013_H,
			ImgEntry.Mode.Z1013,
			8,
			false );
    }
  }


  private void doToZ9001()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      toMonochromCharImage(
			image,
			"Z9001-Blockgrafik",
			"/rom/z9001/z9001font.bin",
			ImgUtil.Z9001_W,
			ImgUtil.Z9001_H,
			ImgEntry.Mode.Z9001,
			8,
			false );
    }
  }


  private void doRollbackTo( int idx )
  {
    if( (idx >= 0) && (idx < this.imgStack.size()) ) {
      boolean state = true;
      if( (idx + 2) < this.imgStack.size() ) {
	state = BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die letzten "
			+ String.valueOf( this.imgStack.size() - idx - 1 )
			+ " Bildbearbeitungsschritte verwerfen?" );
      }
      if( state ) {
	// zu verwerfende Eintraege uebergeben
	while( (idx + 1) < this.imgStack.size() ) {
	  this.imgStack.pop();
	}
	ImgEntry e = this.imgStack.pop();	// anzuzeigendes Bild
	rebuildHistoryMenu();
	showImageInternal(
		e.getImage(),
		e.getMode(),
		e.getRotation(),
		null,
		e.getTitle(),
		this.imgStack.isEmpty(),
		e.getFile(),
		e.getMemBytes() );
      }
    }
  }


  private void doUndo()
  {
    if( this.imgStack.size() > 1 )
      doRollbackTo( this.imgStack.size() - 2 );
  }


	/* --- Konstruktor --- */

  private ImageFrm()
  {
    this.lastRoundTopPixels    = 0;
    this.lastRoundBottomPixels = 0;
    this.autoFitImage          = true;
    this.files                 = null;
    this.savedViewScale        = null;
    this.selectionStart        = null;
    this.cropDlg               = null;
    this.imgStack              = new Stack<>();
    setTitleInternal( null );
    Main.updIcon( this );


    // Menu
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    // Menu Datei
    this.mnuOpen = createJMenuItem(
			"\u00D6ffnen...",
			KeyEvent.VK_O,
			InputEvent.CTRL_MASK );
    mnuFile.add( this.mnuOpen );

    this.mnuSaveAs = createJMenuItem(
			"Speichern unter...",
			KeyEvent.VK_P,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK );
    mnuFile.add( this.mnuSaveAs );

    JMenu mnuExp = new JMenu( "Exportieren als" );
    mnuFile.add( mnuExp );
    mnuFile.addSeparator();

    this.mnuExpImgA5105 = createJMenuItem( "A5105-Bilddatei..." );
    mnuExp.add( this.mnuExpImgA5105 );

    this.mnuExpImgLLC2Hires = createJMenuItem(
					"LLC2-HIRES-Bilddatei..." );
    mnuExp.add( this.mnuExpImgLLC2Hires );

    JMenu mnuExpMem = new JMenu(
		"Abbilddatei f\u00FCr Bildwiederholspeicher f\u00FCr" );
    mnuExp.add( mnuExpMem );

    this.mnuExpMemAC1 = createJMenuItem( "AC1..." );
    mnuExpMem.add( this.mnuExpMemAC1 );

    this.mnuExpMemZ1013 = createJMenuItem( "Z1013..." );
    mnuExpMem.add( this.mnuExpMemZ1013 );

    this.mnuExpMemZ9001 = createJMenuItem( "Z9001..." );
    mnuExpMem.add( this.mnuExpMemZ9001 );

    JMenu mnuExpApp = new JMenu( "Programm zur Anzeige des Bildes im" );
    mnuExp.add( mnuExpApp );

    this.mnuExpAppAC1 = createJMenuItem( "AC1..." );
    mnuExpApp.add( this.mnuExpAppAC1 );

    this.mnuExpAppKC854Hires = createJMenuItem( "KC85/4..." );
    mnuExpApp.add( this.mnuExpAppKC854Hires );

    this.mnuExpAppLLC2Hires = createJMenuItem( "LLC2..." );
    mnuExpApp.add( this.mnuExpAppLLC2Hires );

    this.mnuExpAppZ1013 = createJMenuItem( "Z1013..." );
    mnuExpApp.add( this.mnuExpAppZ1013 );

    this.mnuExpAppZ9001 = createJMenuItem( "Z9001..." );
    mnuExpApp.add( this.mnuExpAppZ9001 );

    this.mnuExpColorTab = createJMenuItem( "Farbpalette exportieren..." );
    mnuFile.add( this.mnuExpColorTab );
    mnuFile.addSeparator();

    this.mnuImgProps = createJMenuItem( "Bildeigenschaften..." );
    mnuFile.add( this.mnuImgProps );

    this.mnuPrint = createJMenuItem(
			"Drucken...",
			KeyEvent.VK_P,
			InputEvent.CTRL_MASK );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuPrev = createJMenuItem(
			"Vorheriges Bild",
			KeyEvent.VK_LEFT,
			InputEvent.CTRL_MASK );
    mnuFile.add( this.mnuPrev );

    this.mnuNext = createJMenuItem(
			"N\u00E4chstes Bild",
			KeyEvent.VK_RIGHT,
			InputEvent.CTRL_MASK );
    mnuFile.add( this.mnuNext );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    this.mnuEdit = new JMenu( "Bearbeiten" );
    this.mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuUndo = createJMenuItem(
			"R\u00FCckg\u00E4ngig",
			KeyEvent.VK_Z,
			InputEvent.CTRL_MASK );
    this.mnuEdit.add( this.mnuUndo );

    this.mnuHistory = new JMenu( "Historie" );
    this.mnuEdit.add( this.mnuHistory );
    this.mnuEdit.addSeparator();

    this.mnuCopy = createJMenuItem( "Bild kopieren" );
    this.mnuEdit.add( this.mnuCopy );

    this.mnuPaste = createJMenuItem( "Bild einf\u00FCgen" );
    this.mnuEdit.add( this.mnuPaste );
    this.mnuEdit.addSeparator();

    this.mnuRotateImage = createJMenuItem( "Drehen..." );
    this.mnuEdit.add( this.mnuRotateImage );

    this.mnuFlipHorizontal = createJMenuItem( "Spiegeln..." );
    this.mnuEdit.add( this.mnuFlipHorizontal );

    this.mnuSelectArea = createJMenuItem( "Zuschneiden..." );
    this.mnuEdit.add( this.mnuSelectArea );

    this.mnuScaleImage = createJMenuItem( "Skalieren..." );
    this.mnuEdit.add( this.mnuScaleImage );

    this.mnuRoundCorners = createJMenuItem( "Ecken abrunden..." );
    this.mnuEdit.add( this.mnuRoundCorners );
    this.mnuEdit.addSeparator();

    this.mnuAdjustImage = createJMenuItem(
				"Helligkeit, Kontrast, Farben..." );
    this.mnuEdit.add( this.mnuAdjustImage );

    this.mnuIndexColors = createJMenuItem(
				"Farben reduzieren und indexieren..." );
    this.mnuEdit.add( this.mnuIndexColors );

    JMenu mnuToBW = new JMenu( "Schwarz/Wei\u00DF wandeln" );
    this.mnuEdit.add( mnuToBW );

    this.mnuToGray = createJMenuItem( "Graustufen" );
    mnuToBW.add( this.mnuToGray );

    this.mnuThreshold = createJMenuItem( "Monochrom mittels Schwellwert" );
    mnuToBW.add( this.mnuThreshold );

    JMenu mnuToMonochrome = new JMenu( "Monochrom mittels Dithering" );
    mnuToBW.add( mnuToMonochrome );

    this.mnuToMonoFloydSteinberg = createJMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.FLOYD_STEINBERG ) );
    mnuToMonochrome.add( this.mnuToMonoFloydSteinberg );

    this.mnuToMonoSierra3 = createJMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.SIERRA3 ) );
    mnuToMonochrome.add( this.mnuToMonoSierra3 );

    this.mnuToMonoAtkinson = createJMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.ATKINSON ) );
    mnuToMonochrome.add( this.mnuToMonoAtkinson );

    this.mnuInvertImage = createJMenuItem( "Invertieren" );
    this.mnuEdit.add( this.mnuInvertImage );

    this.mnuRemoveTransparency = createJMenuItem(
					"Transparenz entfernen..." );
    this.mnuEdit.add( this.mnuRemoveTransparency );
    this.mnuEdit.addSeparator();

    this.mnuConvert = new JMenu( "Konvertieren in" );
    this.mnuEdit.add( mnuConvert );

    this.mnuToA5105 = createJMenuItem( TEXT_A5105_FMT );
    this.mnuConvert.add( this.mnuToA5105 );

    this.mnuToAC1_ACC = createJMenuItem( TEXT_AC1_ACC_FMT );
    this.mnuConvert.add( this.mnuToAC1_ACC );

    this.mnuToAC1_SCCH = createJMenuItem( TEXT_AC1_SCCH_FMT );
    this.mnuConvert.add( this.mnuToAC1_SCCH );

    this.mnuToAC1_2010 = createJMenuItem( TEXT_AC1_2010_FMT );
    this.mnuConvert.add( this.mnuToAC1_2010 );

    this.mnuToKC854Hires = createJMenuItem( TEXT_KC854HIRES_FMT );
    this.mnuConvert.add( this.mnuToKC854Hires );

    this.mnuToLLC2Hires21 = createJMenuItem(
		TEXT_LLC2HIRES_FMT + " ohne Anpassung f\u00FCr 4:3-Anzeige" );
    this.mnuConvert.add( this.mnuToLLC2Hires21 );

    this.mnuToLLC2Hires43 = createJMenuItem(
		TEXT_LLC2HIRES_FMT + " mit Anpassung f\u00FCr 4:3-Anzeige" );
    this.mnuConvert.add( this.mnuToLLC2Hires43 );

    this.mnuToZ1013 = createJMenuItem( TEXT_Z1013_FMT );
    this.mnuConvert.add( this.mnuToZ1013 );

    this.mnuToZ9001 = createJMenuItem( TEXT_Z9001_FMT );
    this.mnuConvert.add( this.mnuToZ9001 );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuAutoResize = new JCheckBoxMenuItem(
				"Fenster an Bildgr\u00F6\u00DFe anpassen",
				false );
    mnuSettings.add( this.mnuAutoResize );
    mnuSettings.addSeparator();

    JMenu mnuBgColor = new JMenu( "Hintergrundfarbe" );
    mnuSettings.add( mnuBgColor );

    ButtonGroup grpBgColor = new ButtonGroup();

    this.mnuBgSystem = new JRadioButtonMenuItem( "System", true );
    this.mnuBgSystem.addActionListener( this );
    grpBgColor.add( this.mnuBgSystem );
    mnuBgColor.add( this.mnuBgSystem );
    mnuBgColor.addSeparator();

    this.mnuBgBlack = new JRadioButtonMenuItem( "schwarz", false );
    this.mnuBgBlack.addActionListener( this );
    grpBgColor.add( this.mnuBgBlack );
    mnuBgColor.add( this.mnuBgBlack );

    this.mnuBgGray = new JRadioButtonMenuItem( "grau", false );
    this.mnuBgGray.addActionListener( this );
    grpBgColor.add( this.mnuBgGray );
    mnuBgColor.add( this.mnuBgGray );

    this.mnuBgWhite = new JRadioButtonMenuItem( "wei\u00DF", false );
    this.mnuBgWhite.addActionListener( this );
    grpBgColor.add( this.mnuBgWhite );
    mnuBgColor.add( this.mnuBgWhite );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( this.mnuEdit );
    mnuBar.add( mnuSettings );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


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

    this.btnOpen = createImageButton(
				"/images/file/open.png",
				this.mnuOpen.getText() );
    toolBar.add( this.btnOpen );

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

    this.comboViewScale = createScaleComboBox(
		100, 10, 15, 25, 33, 50, 75, 100, 200, 300, 400 );
    toolBar.add( this.comboViewScale );

    this.btnFitImage = createImageButton(
				"/images/edit/fit.png",
				"Bild an Fenstergr\u00F6\u00DFe anpassen" );
    toolBar.add( this.btnFitImage );
    toolBar.addSeparator();

    this.btnPrev = createImageButton(
				"/images/nav/back.png",
				"Vorheriges Bild" );
    toolBar.add( this.btnPrev );
    toolBar.addSeparator();

    this.btnNext = createImageButton(
				"/images/nav/next.png",
				"N\u00E4chstes Bild" );
    toolBar.add( this.btnNext );
    toolBar.addSeparator();


    // Statuszeile
    this.labelStatus = new JLabel( DEFAULT_STATUS_TEXT );
    JPanel panelStatus = new JPanel(
		new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelStatus.add( this.labelStatus );
    add( panelStatus, BorderLayout.SOUTH );


    // Drop-Ziele
    (new DropTarget( this.imgFld, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    this.imgFld.addMouseListener( this );
    this.imgFld.addMouseMotionListener( this );
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }
    clearContent();
  }


	/* --- private Methoden --- */

  private void clearContent()
  {
    showImageInternal(
		null,
		ImgEntry.Mode.UNSPECIFIED,
		ImgFld.Rotation.NONE,
		null,
		null,
		true,
		null,
		null );
    updFileList();
    updPasteBtn();
    this.labelStatus.setText( DEFAULT_STATUS_TEXT );
  }


  private void clearHistoryMenu()
  {
    try {
      int itemCnt = this.mnuHistory.getItemCount();
      for( int i = 0; i < itemCnt; i++ ) {
	this.mnuHistory.getItem( i ).removeActionListener( this );
      }
    }
    catch( IllegalArgumentException ex ) {}
    this.mnuHistory.removeAll();
  }


  private boolean confirmImageSaved()
  {
    boolean rv = true;
    int     n  = this.imgStack.size();
    if( n > 1 ) {		// Basisbild gilt immer als gespeichert
      if( this.imgStack.peek().getFile() == null ) {
	rv = BaseDlg.showYesNoWarningDlg(
		this,
		"Das angezeigte Bild wurde nicht gespeichert.\n"
			+ "M\u00F6chten Sie es verwerfen?",
		"Bild nicht gespeichert" );
      }
    }
    return rv;
  }

  private BufferedImage drawImageTo(
				BufferedImage srcImg,
				Float         ratioCorrection,
				BufferedImage dstImg )
  {
    if( (srcImg != null) && (dstImg != null) ) {
      float           srcW     = (float) srcImg.getWidth();
      float           srcH     = (float) srcImg.getHeight();
      ImgFld.Rotation rotation = this.imgFld.getRotation();
      if( rotation.equals( ImgFld.Rotation.LEFT )
	  || rotation.equals( ImgFld.Rotation.RIGHT ) )
      {
	srcW = (float) srcImg.getHeight();
	srcH = (float) srcImg.getWidth();
      }
      if( ratioCorrection != null ) {
	srcW *= ratioCorrection.floatValue();
      }
      float srcRatio = srcW / srcH;

      int   dstW     = dstImg.getWidth();
      int   dstH     = dstImg.getHeight();
      float dstRatio = (float) dstW / (float) dstH;

      int x = 0;
      int y = 0;
      int w = dstW;
      int h = dstH;
      if( dstRatio < srcRatio ) {
	// schwarze Balken oben und unten
	w = dstW;
	h = Math.round( (float) dstW / srcW * srcH );
	y = (dstH - h) / 2;
      } else if( dstRatio > srcRatio ) {
	// schwarze Balken links und rechts
	h = dstH;
	w = Math.round( (float) dstH / srcH * srcW );
	x = (dstW - w) / 2;
      }

      Graphics2D g = dstImg.createGraphics();
      g.setColor( Color.black );
      g.fillRect( 0, 0, dstW, dstH );
      if( !rotation.equals( ImgFld.Rotation.NONE ) ) {
	int m = 0;
	switch( rotation ) {
	  case LEFT:
	    g.rotate( -(Math.PI / 2.0) );
	    x = -((dstH - h) / 2) - h;
	    y = (dstW - w) / 2;
	    m = w;
	    w = h;
	    h = m;
	    break;

	  case RIGHT:
	    g.rotate( Math.PI / 2.0 );
	    x = (dstH - h) / 2;
	    y = -((dstW - w) / 2) - w;
	    m = w;
	    w = h;
	    h = m;
	    break;

	  case DOWN:
	    g.rotate( Math.PI );
	    x = -((dstW - w) / 2) - w;
	    y = -((dstH - h) / 2) - h;
	    break;
	}
      }
      g.drawImage( srcImg, x, y, w, h, this );
      g.dispose();
    }
    return dstImg;
  }


  private void fitImage( boolean allowScaleUp )
  {
    boolean   fitBtnState = false;
    Dimension imgSize     = this.imgFld.getRotatedImageSize();
    if( imgSize != null ) {
      fitBtnState = true;

      // Viewport-Groesse ermitteln
      JViewport vp = this.scrollPane.getViewport();
      if( vp != null ) {
	int vpW = vp.getWidth();
	int vpH = vp.getHeight();
	if( (vpW > 0) && (vpH > 0) ) {

	  // Skalieren
	  if( (imgSize.width > 0) && (imgSize.height > 0)
	      && (allowScaleUp
		  || (vpW < imgSize.width) || (vpH < imgSize.height)) )
	  {
	    this.scrollPane.invalidate();
	    this.imgFld.setScale(
			Math.min(
				(double) vpW / (double) imgSize.width,
				(double) vpH / (double) imgSize.height ) );
	    this.scrollPane.validate();
	    this.scrollPane.repaint();
	    updViewScaleFld();
	    fitBtnState = false;
	  }
	}
      }
    }
    this.btnFitImage.setEnabled( fitBtnState );
  }


  private ImgEntry getCurImgEntry()
  {
    return this.imgStack.isEmpty() ? null : this.imgStack.peek();
  }


  private Dimension getMaxContentSize()
  {
    Dimension rv = null;
    Toolkit   tk = getToolkit();
    if( tk != null ) {
      Dimension screenSize = tk.getScreenSize();
      if( screenSize != null ) {
	if( (screenSize.width > 0) && (screenSize.height > 0) ) {
	  rv = new Dimension(
		(screenSize.width * MAX_CONTENT_SIZE_PERCENT) / 100,
		(screenSize.height * MAX_CONTENT_SIZE_PERCENT) / 100 );
	}
      }
    }
    return rv != null ? rv : new Dimension( 0, 0 );
  }


  private static int getNumMatchingPixels(
					byte[] buf,
					int    offs,
					byte[] pattern,
					int    chXPixels )
  {
    int rv = 0;
    for( int i = 0; i < pattern.length; i++ ) {
      int b1 = 0;
      if( buf != null ) {
	if( offs < buf.length ) {
	  b1 = (int) buf[ offs++ ] & 0xFF;
	}
      }
      int b2 = (int) pattern[ i ] & 0xFF;
      int m  = 0x01;
      for( int k = 0; k < chXPixels; k++ ) {
	if( (b1 & m) == (b2 & m) ) {
	  rv++;
	}
	m <<= 1;
      }
    }
    return rv;
  }


  private void imageSaved( File file, String category )
  {
    if( file != null ) {
      Main.setLastFile( file, category );
      if( this.imgStack.size() == 1 ) {
	/*
	 * Wenn das Basisbild gespeichert wurde und der neue
	 * Dateiname nicht mehr in der Dateiliste enthalten ist,
	 * wird die Dateiliste geloescht,
	 * damit es bei "vorheriges Bild" und "naechstes Bild"
	 * nicht zu Inkonsistenzen kommt.
	 */
	if( this.files != null ) {
	  try {
	    if( Arrays.binarySearch( this.files, file ) < 0 ) {
	      this.files = null;
	    }
	  }
	  catch( ClassCastException ex ) {
	    this.files = null;
	  }
	  finally {
	    if( this.files == null ) {
	      updFileListBtnsEnabled();
	    }
	  }
	}
      } else {
	ImgEntry entry = getCurImgEntry();
	if( entry != null ) {
	  entry.setFile( file );
	}
      }
    }
  }


  private boolean isUnrotated()
  {
    return this.imgFld.getRotation().equals( ImgFld.Rotation.NONE );
  }


  private void rebuildHistoryMenu()
  {
    clearHistoryMenu();
    int n = this.imgStack.size() - 1;
    for( int i = 0; i < n; i++ ) {
      String title = this.imgStack.get( i ).getTitle();
      if( title == null ) {
	title = "kein Titel";
      }
      JMenuItem item = new JMenuItem( title );
      item.setActionCommand( ACTION_HISTORY_PREFIX + String.valueOf( i ) );
      item.addActionListener( this );
      this.mnuHistory.add( item );
    }
  }


  private void saveView()
  {
    if( this.autoFitImage ) {
      this.savedViewScale = null;
    } else {
      this.savedViewScale = this.comboViewScale.getSelectedItem();
    }
    this.imgFld.save();
  }


  private void restoreView()
  {
    this.imgFld.restore();

    String viewScale = null;
    if( this.savedViewScale != null ) {
      viewScale = this.savedViewScale.toString();
    }
    if( viewScale != null ) {
      this.comboViewScale.setSelectedItem( viewScale );
    } else {
      doFitImage( false );
    }
  }


  private void setTitleInternal( String title )
  {
    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "JKCEMU Bildbetrachter" );
    if( title != null ) {
      if( !title.isEmpty() ) {
	buf.append( ": " );
	buf.append( title );
      }
      BufferedImage image = getImage();
      if( image != null) {
	String resolution = String.format(
				"%dx%d",
				image.getWidth(),
				image.getHeight() );
	if( !title.contains( resolution ) ) {
	  buf.append( " (" );
	  buf.append( resolution );
	  buf.append( (char) ')' );
	}
      }
    }
    setTitle( buf.toString() );
  }


  private boolean showImageFile( File file )
  {
    boolean rv = false;
    if( file != null ) {
      ImgEntry entry  = null;
      String   errMsg = null;
      try {
	entry = ImgLoader.load( file );
	if( entry != null ) {
	  String title = file.getName();
	  if( title != null ) {
	    if( title.isEmpty() ) {
	      title = null;
	    }
	  }
	  if( title == null ) {
	    title = file.getPath();
	  }
	  BufferedImage image = entry.getImage();
	  showImageInternal(
			image,
			entry.getMode(),
			ImgFld.Rotation.NONE,
			null,
			title,
			true,
			file,
			null );
	  updFileList();
	  rv = true;
	} else {
	  errMsg = "Dateiformat nicht unterst\u00FCtzt";
	}
      }
      catch( Exception ex ) {
	errMsg = ex.getMessage();
      }
      if( entry == null ) {
	StringBuilder buf = new StringBuilder( 64 );
	buf.append( "Bilddatei kann nicht geladen werden" );
	if( errMsg != null ) {
	  buf.append( ":\n" );
	  buf.append( errMsg );
	} else {
	  buf.append( (char) '.' );
	}
	BaseDlg.showErrorDlg( this, buf.toString() );
      }
    }
    return rv;
  }


  private void showImageInternal(
			BufferedImage   image,
			ImgEntry.Mode   mode,
			ImgFld.Rotation rotation,
			Double          scale,
			String          title,
			boolean         baseImg,
			File            file,
			byte[]          videoMemBytes )
  {
    boolean autoResize = false;
    boolean state      = false;
    int     wImg       = 0;
    int     hImg       = 0;
    double  myScale    = 1.0;
    if( image != null ) {

      // Bildgroesse ermitteln
      wImg = image.getWidth();
      hImg = image.getHeight();
      if( (wImg > 0) && (hImg > 0) ) {
	state = true;

	/*
	 * Bei AutoResize nur den Skalierungfaktor ermitteln
	 * Die Skalierung selbst erfolgt weiter unten.
	 */
	if( (scale == null) && this.mnuAutoResize.isSelected() ) {

	  // Groesse Viewport ermitteln
	  int       vpW = 0;
	  int       vpH = 0;
	  JViewport vp  = this.scrollPane.getViewport();
	  if( vp != null ) {
	    vpW = vp.getWidth();
	    vpH = vp.getHeight();
	  }

	  /*
	   * Die Fenstergroesse und die Skalierung muss nur angepasst werden,
	   * wenn das Bild nicht in den aktuellen Viewport passt.
	   */
	  if( (wImg > vpW) || (hImg > vpH) ) {
	    Dimension maxContentSize = getMaxContentSize();
	    int       maxW           = maxContentSize.width;
	    int       maxH           = maxContentSize.height;
	    if( (maxW < 1) || (maxH < 1) ) {
	      maxW = 640;
	      maxH = 480;
	    }
	    autoResize = true;

	    // Skalierung ermitteln
	    if( (wImg > maxW) || (hImg > maxH) ) {
	      myScale = Math.min(
				(double) maxW / (double) wImg,
				(double) maxH / (double) hImg );
	    }
	  }
	}
      }
    }

    // Bild anzeigen
    boolean  transp   = false;
    ImgEntry oldEntry = getCurImgEntry();
    if( baseImg ) {
      this.imgStack.clear();
      clearHistoryMenu();
    } else {
      oldEntry.setRotation( this.imgFld.getRotation() );
    }
    if( image != null ) {
      this.imgStack.push(
		new ImgEntry(
			image,
			mode,
			rotation,
			title,
			file,
			videoMemBytes ) );
      rebuildHistoryMenu();
      transp = (image.getTransparency() != Transparency.OPAQUE);
    }
    this.scrollPane.invalidate();
    this.imgFld.setImage( image );
    if( scale != null ) {
      this.imgFld.setScale( scale.doubleValue() );
      this.autoFitImage = false;
    } else {
      this.imgFld.setScale( myScale );
      this.autoFitImage = true;
    }
    this.imgFld.setRotation( rotation );
    this.scrollPane.validate();
    this.scrollPane.repaint();
    setTitleInternal( title );
    updViewScaleFld();
    if( oldEntry != null ) {
      oldEntry.getImage().flush();
    }
    this.btnSaveAs.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnRotateLeft.setEnabled( state );
    this.btnRotateRight.setEnabled( state );
    this.mnuSaveAs.setEnabled( state );
    this.mnuExpImgA5105.setEnabled( state );
    this.mnuExpImgLLC2Hires.setEnabled( state );
    this.mnuExpAppAC1.setEnabled( state );
    this.mnuExpAppKC854Hires.setEnabled( state );
    this.mnuExpAppLLC2Hires.setEnabled( state );
    this.mnuExpAppZ1013.setEnabled( state );
    this.mnuExpAppZ9001.setEnabled( state );
    this.mnuExpMemAC1.setEnabled( state );
    this.mnuExpMemZ1013.setEnabled( state );
    this.mnuExpMemZ9001.setEnabled( state );
    this.mnuExpColorTab.setEnabled( state );
    this.mnuImgProps.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuUndo.setEnabled( this.imgStack.size() > 1 );
    this.mnuHistory.setEnabled( this.imgStack.size() > 1 );
    this.mnuCopy.setEnabled( state );
    this.mnuAdjustImage.setEnabled( state );
    this.mnuFlipHorizontal.setEnabled( state );
    this.mnuIndexColors.setEnabled( state );
    this.mnuRoundCorners.setEnabled( state );
    this.mnuRotateImage.setEnabled( state );
    this.mnuSelectArea.setEnabled( state );
    this.mnuScaleImage.setEnabled( state );
    this.mnuToGray.setEnabled( state );
    this.mnuToMonoFloydSteinberg.setEnabled( state );
    this.mnuToMonoSierra3.setEnabled( state );
    this.mnuToMonoAtkinson.setEnabled( state );
    this.mnuThreshold.setEnabled( state );
    this.mnuInvertImage.setEnabled( state );
    this.mnuRemoveTransparency.setEnabled( state && transp );
    this.mnuToA5105.setEnabled( state );
    this.mnuToAC1_ACC.setEnabled( state );
    this.mnuToAC1_SCCH.setEnabled( state );
    this.mnuToAC1_2010.setEnabled( state );
    this.mnuToKC854Hires.setEnabled( state );
    this.mnuToLLC2Hires21.setEnabled( state );
    this.mnuToLLC2Hires43.setEnabled( state );
    this.mnuToZ1013.setEnabled( state );
    this.mnuToZ9001.setEnabled( state );
    if( this.cropDlg != null ) {
      if( state ) {
	this.cropDlg.setImageSize( wImg, hImg );
      } else {
	this.cropDlg.doClose();
      }
    }
    if( autoResize ) {
      updWindowSize();
    }
    if( scale == null ) {
      fitImage( false );
    }
  }


  private void showImgAlreadyMonochrome()
  {
    BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits monochrom." );
  }


  private void showNoMoreImageFileFound()
  {
    BaseDlg.showErrorDlg( this, "Keine weitere Bilddatei gefunden" );
  }


  private void toMonochromCharImage(
			BufferedImage image,
			String        title,
			String        fontResource,
			int           w,
			int           h,
			ImgEntry.Mode mode,
			int           chXPixels,
			boolean       ac1 )
  {
    // zuerst monochrome und skalierte Vollgrafik erzeugen
    BufferedImage newImg = new BufferedImage(
					w,
					h,
					BufferedImage.TYPE_BYTE_BINARY,
					ImgUtil.getColorModelBW() );
    drawImageTo( image, null, newImg );

    /*
     * anschliessend passende Blockgrafiksymbole suchen
     * und durch deren Bitmuster ersetzen
     */
    byte[] fontBytes = EmuUtil.readResource( this, fontResource );
    byte[] fldPixels = new byte[ 8 ];
    int    wChrs     = w / chXPixels;
    int    hChrs     = h / 8;
    ByteArrayOutputStream buf = new ByteArrayOutputStream( wChrs * hChrs );
    for( int chY = 0; chY < hChrs; chY++ ) {
      for( int chX = 0; chX < wChrs; chX++ ) {
	int pos = 0;
	for( int pixY = 0; pixY < 8; pixY++ ) {
	  int y = (chY * 8) + pixY;

	  // Pixeldaten der Zeichenposition lesen
	  int v = 0;
	  int m = (ac1 ? 0x01 : 0x80);
	  for( int pixX = 0; pixX < chXPixels; pixX++ ) {
	    int rgb = 0;
	    int x   = (chX * chXPixels) + pixX;
	    if( (x < w) && (y < h) ) {
	      rgb = newImg.getRGB( x, y );
	    }
	    if( (((rgb >> 16) & 0xFF)
			+ ((rgb >> 8) & 0xFF)
			+ (rgb & 0xFF)) > 0x180 )
	    {
	      v |= m;
	    }
	    if( ac1 ) {
	      m <<= 1;
	    } else {
	      m >>= 1;
	    }
	  }
	  fldPixels[ pos++ ] = (byte) v;
	}

	// Zeichen mit den meisten uebereinstimmenden Pixel suchen
	int bestCode   = 0xFF;
	int bestRating = getNumMatchingPixels(
					fontBytes,
					bestCode * 8,
					fldPixels,
					chXPixels );
	for( int i = 0xFE; i >= 0; --i ) {
	  int rating = getNumMatchingPixels(
					fontBytes,
					i * 8,
					fldPixels,
					chXPixels );
	  if( rating > bestRating ) {
	    bestCode   = i;
	    bestRating = rating;
	    if( bestRating == (8 * chXPixels) ) {
	      break;
	    }
	  }
	}
	buf.write( bestCode );

	// Pixel des gefundenen Zeichens ins Bild uebertragen
	int idx = bestCode * 8;
	for( int pixY = 0; pixY < 8; pixY++ ) {
	  int y = (chY * 8) + pixY;
	  int v = 0;
	  if( fontBytes != null ) {
	    if( idx < fontBytes.length ) {
	      v = (int) fontBytes[ idx ] & 0xFF;
	    }
	  }
	  int m = (ac1 ? 0x01 : 0x80);
	  for( int pixX = 0; pixX < chXPixels; pixX++ ) {
	    int x = (chX * chXPixels) + pixX;
	    if( (x < w) && (y < h) ) {
	      newImg.setRGB( x, y, (v & m) != 0 ? 0xFFFFFFFF : 0xFF000000 );
	    }
	    if( ac1 ) {
	      m <<= 1;
	    } else {
	      m >>= 1;
	    }
	  }
	  idx++;
	}
      }
    }
    byte[] videoMemBytes = null;
    if( buf.size() > 0 ) {
      if( ac1 ) {
	byte[] a = buf.toByteArray();
	if( a != null ) {
	  if( a.length > 0 ) {
	    videoMemBytes = new byte[ a.length ];
	    int srcIdx    = a.length - 1;;
	    int dstIdx    = 0;
	    while( (srcIdx >= 0) && (dstIdx < videoMemBytes.length) ) {
	      videoMemBytes[ dstIdx++ ] = a[ srcIdx-- ];
	    }
	  }
	}
      } else {
	videoMemBytes = buf.toByteArray();
      }
    }
    showImageInternal(
		newImg,
		mode,
		ImgFld.Rotation.NONE,
		null,
		title,
		false,
		null,
		videoMemBytes );
  }


  private Point toUnscaledPoint( MouseEvent e )
  {
    double f = this.imgFld.getCurScale();
    return new Point(
		(int) Math.round( (double) e.getX() / f ),
		(int) Math.round( (double) e.getY() / f ) );
  }


  private void updCropDlg()
  {
    if( this.cropDlg != null ) {
      if( this.cropDlg.isVisible() ) {
	Dimension imgSize = this.imgFld.getRotatedImageSize();
	if( imgSize != null ) {
	  this.cropDlg.setImageSize( imgSize.width, imgSize.height );
	} else {
	  this.cropDlg.doClose();
	}
      }
    }
  }


  private void updPasteBtn()
  {
    boolean state = false;
    if( this.clipboard != null ) {
      try {
	state = this.clipboard.isDataFlavorAvailable( DataFlavor.imageFlavor );
      }
      catch( IllegalStateException ex ) {}
    }
    this.mnuPaste.setEnabled( state );
  }


  private void updFileListBtnsEnabled()
  {
    boolean statePrev = false;
    boolean stateNext = false;
    if( (this.files != null) && !this.imgStack.isEmpty() ) {
      File file = this.imgStack.get( 0 ).getFile();	// von Basisdatei!
      if( (file != null) && (this.files.length > 0) ) {
	try {
	  int idx = Arrays.binarySearch(
				this.files,
				file,
				FileComparator.getInstance() );
	  if( idx < 0 ) {
	    idx       = -(idx + 1);
	    stateNext = (idx < files.length);
	  } else {
	    stateNext = ((idx + 1) < files.length);
	  }
	  statePrev = (idx > 0);
	}
	catch( ClassCastException ex ) {}
      }
    }
    this.btnPrev.setEnabled( statePrev );
    this.mnuPrev.setEnabled( statePrev );
    this.btnNext.setEnabled( stateNext );
    this.mnuNext.setEnabled( stateNext );
  }


  private void updFileList()
  {
    File[] files = null;
    if( !this.imgStack.isEmpty() ) {
      File file = this.imgStack.get( 0 ).getFile();	// von Basisdatei!
      if( file != null ) {
	File dir = file.getParentFile();
	if( dir != null ) {
	  files = dir.listFiles(
			new FilenameFilter()
			{
			  @Override
			  public boolean accept( File dir, String fName )
			  {
			    return fName != null ?
				ImgLoader.accept( new File( dir, fName ) )
				: false;
			  }
			} );
	  if( files != null ) {
	    if( files.length > 1 ) {
	      try {
		FileComparator fc = FileComparator.getInstance();
		Arrays.sort( files, fc );
	      }
	      catch( Exception ex ) {}
	    } else {
	      files = null;
	    }
	  }
	}
      }
    }
    this.files = files;
    updFileListBtnsEnabled();
  }
}
