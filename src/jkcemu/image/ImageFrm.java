/*
 * (c) 2008-2021 Jens Mueller
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
import java.awt.RenderingHints;
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
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.file.Downloader;
import jkcemu.file.FileComparator;
import jkcemu.file.FileUtil;
import jkcemu.print.PrintUtil;


public class ImageFrm extends AbstractImageFrm implements
						Downloader.Consumer,
						DropTargetListener,
						FlavorListener,
						MouseMotionListener
{
  public static final String TITLE = Main.APPNAME + " Bildbetrachter";

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
			"AC1-ACC-Blockgrafik (384x256, monochrom)";
  private static final String TEXT_AC1_SCCH_FMT =
			"AC1-SCCH-Blockgrafik (384x256, monochrom)";
  private static final String TEXT_AC1_2010_FMT =
			"AC1-2010-Blockgrafik (384x256, monochrom)";
  private static final String TEXT_KC85MONO_FMT =
			"KC85/2..5-Format ohne Farben (320x256, monochrom)";
  private static final String TEXT_KC854HIRES_FMT =
			"KC85/4,5-HIRES-Format (320x256, 4 Farben)";
  private static final String TEXT_LLC2HIRES_FMT =
			"LLC2-HIRES-Format (512x256, monochrom)";
  private static final String TEXT_Z1013_FMT =
			"Z1013-Blockgrafik (256x256, monochrom)";
  private static final String TEXT_Z9001_FMT =
			"Z9001-Blockgrafik (320x192, monochrom)";


  private enum ExpFmt {
		IMG_A5105,
		IMG_KC852_MONOCHROME,
		IMG_KC854_MONOCHROME,
		IMG_KC854_HIRES,
		IMG_LLC2_HIRES,
		APP_AC1,
		APP_KC852,
		APP_KC854,
		APP_LLC2_HIRES,
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
  private Stack<ImageEntry> imgStack;
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
  private JMenuItem         mnuExpImgKC852Monochrome;
  private JMenuItem         mnuExpImgKC854Monochrome;
  private JMenuItem         mnuExpImgKC854Hires;
  private JMenuItem         mnuExpImgLLC2Hires;
  private JMenuItem         mnuExpMemAC1;
  private JMenuItem         mnuExpMemZ1013;
  private JMenuItem         mnuExpMemZ9001;
  private JMenuItem         mnuExpAppAC1;
  private JMenuItem         mnuExpAppKC852;
  private JMenuItem         mnuExpAppKC854;
  private JMenuItem         mnuExpAppLLC2Hires;
  private JMenuItem         mnuExpAppZ1013;
  private JMenuItem         mnuExpAppZ9001;
  private JMenuItem         mnuPrint;
  private JMenuItem         mnuImgProps;
  private JMenuItem         mnuExifEdit;
  private JMenuItem         mnuExifRemove;
  private JMenuItem         mnuPrev;
  private JMenuItem         mnuNext;
  private JMenuItem         mnuClose;
  private JMenu             mnuEdit;
  private JMenu             mnuHistory;
  private JMenuItem         mnuCopy;
  private JMenuItem         mnuUndo;
  private JMenuItem         mnuPaste;
  private JMenuItem         mnuCropImage;
  private JMenuItem         mnuDetectEdges;
  private JMenuItem         mnuInvertImage;
  private JMenuItem         mnuScaleImage;
  private JMenuItem         mnuSharpenImage;
  private JMenuItem         mnuSoftenImage;
  private JMenuItem         mnuRotateImage;
  private JMenuItem         mnuFlipHorizontal;
  private JMenuItem         mnuRoundCorners;
  private JMenuItem         mnuAdjustImage;
  private JMenuItem         mnuIndexColors;
  private JMenu             mnuToBW;
  private JMenuItem         mnuToGray;
  private JMenuItem         mnuToMonoFloydSteinberg;
  private JMenuItem         mnuToMonoSierra3;
  private JMenuItem         mnuToMonoAtkinson;
  private JMenuItem         mnuThreshold;
  private JMenuItem         mnuRemoveTransparency;
  private JMenuItem         mnuColorPalette;
  private JMenu             mnuConvert;
  private JMenuItem         mnuToA5105;
  private JMenuItem         mnuToAC1_ACC;
  private JMenuItem         mnuToAC1_SCCH;
  private JMenuItem         mnuToAC1_2010;
  private JMenuItem         mnuToKC85Monochrome;
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


  public ImageFld getImageFld()
  {
    return this.imageFld;
  }


  public String getMenuPathTextReduceColors()
  {
    return String.format(
		"\'%s\' \u2192 \'%s\'",
		this.mnuEdit.getText(),
		this.mnuIndexColors.getText() );
  }


  public static ImageFrm open()
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
    return instance;
  }


  public static ImageFrm open(
			BufferedImage image,
			ExifData      exifData,
			String        title )
  {
    open();
    if( image != null ) {
      if( instance.confirmImageSaved() ) {
	instance.showImageInternal(
				image,
				exifData,
				ImageEntry.Action.INITIAL_LOADED,
				ImageUtil.probeMode( image ),
				ImageFld.Rotation.NONE,
				null,
				title,
				null,
				null );
	instance.updFileList();
      }
    }
    return instance;
  }


  public static ImageFrm open( File file )
  {
    open();
    if( file != null ) {
      if( instance.confirmImageSaved() ) {
	instance.showImageFile( file, null, null );
      }
    }
    return instance;
  }


  public void setSelection( int x, int y, int w, int h )
  {
    this.imageFld.setSelection( x, y, w, h );
  }


  public void setSelectionColor( Color color )
  {
    this.imageFld.setSelectionColor( color );
  }


  public void showCroppedImage( BufferedImage image )
  {
    ImageEntry entry = getCurImageEntry();
    if( (entry != null) && (image != null) ) {
      showImageInternal(
		image,
		entry.getSharedExifDataCopyForResizedImage(),
		ImageEntry.Action.CHANGED,
		entry.getMode(),
		this.imageFld.getRotation(),
		null,
		"Zugeschnitten",
		null,
		null );
    }
  }


	/* --- Downloader.Consumer --- */

  @Override
  public void consume( byte[] fileBytes, String fileName )
  {
    showImageFile( null, fileBytes, fileName );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      if( confirmImageSaved() ) {
	if( !Downloader.checkAndStart(
			this,
			file,
			64 * 1024 * 1024,	// 64 MByte
			true,			// GZip-Dateien entpacken
			e,
			this ) )
	{
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    showImageFile( file, null, null );
			  }
			} );
	}
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
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
    if( e.getComponent() == this.imageFld ) {
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
	  this.imageFld.setSelection( r );
	} else {
	  this.imageFld.setSelection( x, y, w, h );
	}
	r = this.imageFld.getSelectedArea();
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
    if( (e.getComponent() == this.imageFld)
	&& (this.imageFld.getSelection() == null) )
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
  public boolean applySettings( Properties props )
  {
    boolean rv = false;
    if( props != null ) {
      rv = super.applySettings( props );

      String prefix = getSettingsPrefix();

      this.mnuAutoResize.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				prefix + PROP_AUTORESIZE,
				false ) );

      AbstractButton btn   = this.mnuBgSystem;
      Color          color = SystemColor.window;
      String         text  = props.getProperty( prefix + PROP_BACKGROUND );
      if( text != null ) {
	text = text.trim().toLowerCase();
	if( text.equals( VALUE_BLACK ) ) {
	  color = Color.BLACK;
	  btn   = this.mnuBgBlack;
	}
	else if( text.equals( VALUE_GRAY ) ) {
	  color = Color.GRAY;
	  btn   = this.mnuBgGray;
	}
	else if( text.equals( VALUE_WHITE ) ) {
	  color = Color.WHITE;
	  btn   = this.mnuBgWhite;
	}
      }
      btn.setSelected( true );
      this.imageFld.setBackground( color );
      this.imageFld.repaint();
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
    Object  src = e.getSource();
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
    else if( src == this.mnuExpImgKC852Monochrome ) {
      rv = true;
      doExport( ExpFmt.IMG_KC852_MONOCHROME );
    }
    else if( src == this.mnuExpImgKC854Monochrome ) {
      rv = true;
      doExport( ExpFmt.IMG_KC854_MONOCHROME );
    }
    else if( src == this.mnuExpImgKC854Hires ) {
      rv = true;
      doExport( ExpFmt.IMG_KC854_HIRES );
    }
    else if( src == this.mnuExpImgLLC2Hires ) {
      rv = true;
      doExport( ExpFmt.IMG_LLC2_HIRES );
    }
    else if( src == this.mnuExpAppAC1 ) {
      rv = true;
      doExport( ExpFmt.APP_AC1 );
    }
    else if( src == this.mnuExpAppKC852 ) {
      rv = true;
      doExport( ExpFmt.APP_KC852 );
    }
    else if( src == this.mnuExpAppKC854 ) {
      rv = true;
      doExport( ExpFmt.APP_KC854 );
    }
    else if( src == this.mnuExpAppLLC2Hires ) {
      rv = true;
      doExport( ExpFmt.APP_LLC2_HIRES );
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
    else if( (src == this.btnPrint) || (src == this.mnuPrint) ) {
      rv = true;
      if( getImage() != null ) {
	PrintUtil.doPrint( this, this.imageFld, "Bildbetrachter" );
      }
    }
    else if( src == this.mnuImgProps ) {
      rv = true;
      doImgProps();
    }
    else if( src == this.mnuExifEdit ) {
      rv = true;
      doExifEdit();
    }
    else if( src == this.mnuExifRemove ) {
      rv = true;
      doExifRemove();
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
    else if( src == this.mnuDetectEdges ) {
      rv = true;
      doDetectEdges();
    }
    else if( src == this.mnuInvertImage ) {
      rv = true;
      doInvertImage();
    }
    else if( src == this.mnuRoundCorners ) {
      rv = true;
      doRoundCorners();
    }
    else if( src == this.mnuCropImage ) {
      rv = true;
      doCropImage();
    }
    else if( src == this.mnuScaleImage ) {
      rv = true;
      doScaleImage();
    }
    else if( src == this.mnuSharpenImage ) {
      rv = true;
      doSharpenImage();
    }
    else if( src == this.mnuSoftenImage ) {
      rv = true;
      doSoftenImage();
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
    else if( src == this.mnuRemoveTransparency ) {
      rv = true;
      doRemoveTransparency();
    }
    else if( src == this.mnuColorPalette ) {
      rv = true;
      doColorPalette();
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
	ImageEntry.Mode.AC1_ACC );
    }
    else if( src == this.mnuToAC1_SCCH ) {
      rv = true;
      doToAC1(
	"AC1-SCCH-Blockgrafik",
	"/rom/ac1/scchfont.bin",
	ImageEntry.Mode.AC1_SCCH );
    }
    else if( src == this.mnuToAC1_2010 ) {
      rv = true;
      doToAC1(
	"AC1-2010-Blockgrafik",
	"/rom/ac1/font2010.bin",
	ImageEntry.Mode.AC1_2010 );
    }
    else if( src == this.mnuToKC85Monochrome ) {
      rv = true;
      doToKC85Monochrome();
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
      this.imageFld.setBackground( SystemColor.window );
      this.imageFld.repaint();
    }
    else if( src == this.mnuBgBlack ) {
      rv = true;
      this.imageFld.setBackground( Color.BLACK );
      this.imageFld.repaint();
    }
    else if( src == this.mnuBgGray ) {
      rv = true;
      this.imageFld.setBackground( Color.GRAY );
      this.imageFld.repaint();
    }
    else if( src == this.mnuBgWhite ) {
      rv = true;
      this.imageFld.setBackground( Color.WHITE );
      this.imageFld.repaint();
    }
    else if( src == this.mnuHelpContent ) {
      rv = true;
      HelpFrm.openPage( HELP_PAGE );
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
      if( Main.isTopFrm( this ) ) {
	rv = EmuUtil.closeOtherFrames( this );
	if( rv ) {
	  rv = super.doClose();
	}
	if( rv ) {
	  Main.exitSuccess();
	}
      } else {
	rv = super.doClose();
      }
      if( rv ) {
	// damit beim erneuten Oeffnen das Fenster leer ist
	clearContent();
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
  protected ExifData getExifData()
  {
    return this.imgStack.isEmpty() ?
			null
			: this.imgStack.peek().getExifData();
  }


  @Override
  protected BufferedImage getImage()
  {
    return this.imgStack.isEmpty() ? null : this.imgStack.peek().getImage();
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    if( e.getComponent() == this.imageFld )
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this.imageFld ) {
      this.imageFld.setSelection( null );
      this.imageFld.requestFocus();
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( e.getComponent() == this.imageFld )
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
      Dimension imgSize = this.imageFld.getRotatedImageSize();
      if( imgSize != null ) {
	int wImg = imgSize.width;
	int hImg = imgSize.height;
	if( (wImg > 0) && (hImg > 0) ) {
	  removeComponentListener( this );
	  double    scale   = this.imageFld.getScale();
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

  private void doAdjustImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      saveView();
      BufferedImage image = ImageAdjustDlg.showDlg( this );
      if( image != null ) {
	showSameBoundsImage(
		image,
		entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		entry.getMode() == ImageEntry.Mode.INDEXED_COLORS ?
			ImageEntry.Mode.INDEXED_COLORS 
			: ImageEntry.Mode.UNSPECIFIED,
		"Helligkeit,  Kontrast, Farben eingestellt" );
      } else {
	restoreView();
      }
    }
  }


  private void doColorPalette()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      saveView();
      BufferedImage image = ColorPaletteDlg.showDlg( this );
      if( image != null ) {
	showSameBoundsImage(
		image,
		entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		ImageEntry.Mode.INDEXED_COLORS,
		"Farbpalette ge\u00E4ndert" );
      } else {
	restoreView();
      }
    }
  }


  private void doCropImage()
  {
    BufferedImage image = getImage();
    if( image != null ) {
      int w = image.getWidth();
      int h = image.getHeight();
      if( (w > 0) && (h > 0) ) {
	Rectangle r = this.imageFld.getSelectedArea();
	if( this.cropDlg == null ) {
	  this.cropDlg = new CropDlg( this );
	}
	this.cropDlg.setImageSize( w, h );
	this.cropDlg.setSelectedArea( r );
	this.cropDlg.setVisible( true );
	this.cropDlg.toFront();
	this.imageFld.setSelection( r );
      }
    }
  }


  private void doDetectEdges()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( convolveAndShowImage(
			entry,
			new Kernel(
				3, 3,
				new float[] {
					-0.7F, -1F,   -0.7F,
					-1F,    6.8F, -1F,
					-0.7F, -1F,   -0.7F } ),
			ImageEntry.Action.CHANGED,
			"Konturen hervorgehoben" ) != null )
      {
	BaseDlg.showSuppressableInfoDlg(
		this,
		"M\u00F6glicherweise m\u00FCssen Sie das Bild aufhellen"
			+ " und und den Kontrast erh\u00F6hen,"
			+ " um die Konturen zu sehen.\n"
			+ "Auch die Funktion \'"
			+ this.mnuToBW.getText()
			+ "\' \u2192 \'"
			+ this.mnuThreshold.getText()
			+ "\'\n"
			+ "kann f\u00FCr die weitere Bearbeitung"
			+ " n\u00FCtzlich sein." );
      }
    }
  }


  private void doExifEdit()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      ExifData exifData = ExifEditDlg.showDlg( this, entry.getExifData() );
      if( exifData != null ) {
	showSameBoundsImage(
			entry.getImage(),
			exifData,
			ImageEntry.Action.CHANGED,
			entry.getMode(),
			"Zusatzinformationen ge\u00E4ndert" );
      }
    }
  }


  private void doExifRemove()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die an dem Bild angeh\u00E4ngten\n"
			+ "Zusatzinformationen (EXIF-Daten) entfernen?" ) )
      {
	showSameBoundsImage(
			entry.getImage(),
			null,
			ImageEntry.Action.CHANGED,
			entry.getMode(),
			"Zusatzinformationen entfernt" );
      }
    }
  }


  private void doExport( ExpFmt expFmt )
  {
    ImageEntry entry = getCurImageEntry();
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
	    fileFilter = ImageUtil.createA5105ImageFileFilter();
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
	      fileFilter = FileUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = FileUtil.getBinaryFileFilter();
	      fileExt    = "_1000_17FF.bin";
	    }
	    status = true;
	  }
	  break;
	case APP_KC852:
	  formatText  = "KC85/2,3-";
	  softwareDir = true;
	  if( entry.isKC85MonochromeFormat() ) {
	    fileFilter = FileUtil.getKCSystemFileFilter();
	    fileExt    = ".kcc";
	    status     = true;
	  }
	  break;
	case APP_KC854:
	  formatText  = "KC85/4,5-";
	  softwareDir = true;
	  if( entry.isKC854HiresFormat() || entry.isKC85MonochromeFormat() ) {
	    fileFilter = FileUtil.getKCSystemFileFilter();
	    fileExt    = ".kcc";
	    status     = true;
	  }
	  break;
	case IMG_KC852_MONOCHROME:
	  formatText  = "KC85/2,3-Monochrom-";
	  softwareDir = true;
	  if( entry.isKC85MonochromeFormat() ) {
	    fileFilter = ImageUtil.createKC852ImageFileFilter();
	    fileExt    = ".pic";
	    status     = true;
	  }
	  break;
	case IMG_KC854_MONOCHROME:
	  formatText  = "KC85/4,5-Monochrom-";
	  softwareDir = true;
	  if( entry.isKC85MonochromeFormat() ) {
	    fileFilter = ImageUtil.createKC854LowresImageFileFilter();
	    fileExt    = ".pip";
	    status     = true;
	  }
	  break;
	case IMG_KC854_HIRES:
	  formatText  = "KC85/4,5-HIRES-";
	  softwareDir = true;
	  if( entry.isKC854HiresFormat() || entry.isKC85MonochromeFormat() ) {
	    fileFilter = ImageUtil.createKC854HiresImageFileFilter();
	    fileExt    = ".hip";
	    status     = true;
	  }
	  break;
	case APP_LLC2_HIRES:
	case IMG_LLC2_HIRES:
	  formatText  = "LLC2-HIRES-";
	  softwareDir = true;
	  if( entry.isLLC2HiresFormat() ) {
	    if( entry.getMemBytes() == null ) {
	      entry.setMemBytes( ImageUtil.createLLC2HiresMemBytes( image ) );
	    }
	    if( expFmt.equals( ExpFmt.APP_LLC2_HIRES ) ) {
	      fileFilter = FileUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = ImageUtil.createLLC2HiresImageFileFilter();
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
	      fileFilter = FileUtil.getHeadersaveFileFilter();
	      fileExt    = ".z80";
	    } else {
	      fileFilter = FileUtil.getBinaryFileFilter();
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
	      fileFilter = FileUtil.getKCSystemFileFilter();
	      fileExt    = ".kcc";
	    } else {
	      fileFilter = FileUtil.getBinaryFileFilter();
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
	File file = FileUtil.showFileSaveDlg(
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
		ImageSaver.saveImageA5105( this, image, file );
		break;
	      case IMG_KC852_MONOCHROME:
		ImageSaver.saveImageKC852Monochrome( image, file );
		break;
	      case IMG_KC854_MONOCHROME:
		ImageSaver.saveImageKC854Monochrome( image, file );
		break;
	      case IMG_KC854_HIRES:
		ImageSaver.saveImageKC854Hires( image, file );
		break;
	      case IMG_LLC2_HIRES:
		ImageUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case APP_AC1:
		if( entry.equalsMode( ImageEntry.Mode.AC1_ACC ) ) {
		  ImageSaver.saveAppAC1ACC( entry.getMemBytes(), file );
		} else {
		  ImageSaver.saveAppAC1SCCH( entry.getMemBytes(), file );
		}
		break;
	      case APP_KC852:
		ImageSaver.saveAppKC852BW( image, file );
		break;
	      case APP_KC854:
		if( entry.isKC854HiresFormat() ) {
		  ImageSaver.saveAppKC854Hires( image, file );
		} else {
		  ImageSaver.saveAppKC854LowresBW( image, file );
		}
		break;
	      case APP_LLC2_HIRES:
		ImageSaver.saveAppLLC2Hires( entry.getMemBytes(), file );
		break;
	      case APP_Z1013:
		ImageSaver.saveAppZ1013( entry.getMemBytes(), file );
		break;
	      case APP_Z9001:
		ImageSaver.saveAppZ9001( entry.getMemBytes(), file );
		break;
	      case MEM_AC1:
		ImageUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case MEM_Z1013:
		ImageUtil.writeToFile( entry.getMemBytes(), file );
		break;
	      case MEM_Z9001:
		ImageUtil.writeToFile( entry.getMemBytes(), file );
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
			+ "in das entsprechende Format umwandeln,\n"
			+ "bevor Sie es in dem Format exportieren"
			+ " k\u00F6nnen.\n"
			+ "Lesen Sie dazu bitte auch die Hinweise"
			+ " in der Hilfe!",
		"Exportieren" );
      }
    }
  }


  private void doFitImage( boolean allowScaleUp )
  {
    this.autoFitImage = true;
    fitImage( allowScaleUp );
  }


  private void doFlipHorizontal()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.getAction() == ImageEntry.Action.HORIZONTAL_FLIPPED ) {
	doUndo();
      } else {
	int w = entry.getWidth();
	int h = entry.getHeight();
	if( (w > 0) && (h > 0) ) {
	  BufferedImage newImg = ImageUtil.createCompatibleImage(
							entry.getImage(),
							w,
							h );
	  if( newImg != null ) {
	    Graphics g = newImg.createGraphics();
	    g.drawImage( entry.getImage(), w, 0, -w, h, this );
	    g.dispose();
	    showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.HORIZONTAL_FLIPPED,
			entry.getMode(),
			"Horizontal gespiegelt" );
	  }
	}
      }
    }
  }


  private void doImgProps()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      ImagePropsDlg.showDlg( this, entry );
    }
  }


  private void doIndexColors()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      BufferedImage newImg = IndexColorsDlg.showDlg(
					this,
					entry.getImage() );
      if( newImg != null ) {
	String          title = "Indexierte Farben";
	IndexColorModel icm   = ImageUtil.getIndexColorModel( newImg );
	if( icm != null ) {
	  int nColors = icm.getMapSize();
	  if( nColors > 0 ) {
	    title = String.format( "%d indexierte Farben", nColors );
	  }
	}
	if( newImg != null ) {
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			ImageEntry.Mode.INDEXED_COLORS,
			title );
	}
      }
    }
  }


  private void doOpen()
  {
    if( instance.confirmImageSaved() ) {
      File file = FileUtil.showFileOpenDlg(
			this,
			"Bilddatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_IMAGE ),
			ImageLoader.createFileFilter() );
      if( file != null ) {
	if( showImageFile( file, null, null ) ) {
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
		  if( ImageLoader.accept( f ) ) {
		    showImageFile( f, null, null );
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
		  if( ImageLoader.accept( f ) ) {
		    showImageFile( f, null, null );
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


  private void doPaste()
  {
    if( this.clipboard != null ) {
      try {
	if( this.clipboard.isDataFlavorAvailable( DataFlavor.imageFlavor ) ) {
	  Object o = this.clipboard.getData( DataFlavor.imageFlavor );
	  if( o != null ) {
	    if( o instanceof Image ) {
	      if( confirmImageSaved() ) {
		BufferedImageBuilder builder
				    = new BufferedImageBuilder( this );
		BufferedImage image = builder.buildOf(
						(Image) o,
						"Bild einf\u00FCgen..." );
		if( image != null ) {
		  showImageInternal(
				image,
				null,
				ImageEntry.Action.INITIAL_LOADED,
				builder.getMode(),
				ImageFld.Rotation.NONE,
				null,
				"Eingef\u00FCgtes Bild",
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
      File file = null;
      int  idx  = nEntries - 1;
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


  private void doRotateImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      saveView();
      BufferedImage image = RotateDlg.showDlg( this );
      if( image != null ) {
	showImageInternal(
			image,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			entry.getMode(),
			this.imageFld.getRotation(),
			getViewScale(),
			"Gedreht",
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
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      RoundCornersDlg dlg = new RoundCornersDlg(
					this,
					this.lastRoundTopPixels,
					this.lastRoundBottomPixels );
      dlg.setVisible( true );
      int nTopPixels    = dlg.getNumTopPixels();
      int nBottomPixels = dlg.getNumBottomPixels();
      if( (nTopPixels > 0) || (nBottomPixels > 0) ) {
	BufferedImage newImg = ImageUtil.roundCorners(
						this,
						entry.getImage(),
						nTopPixels,
						nBottomPixels );
	if( newImg != null ) {
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			entry.getMode(),
			"Ecken abgerundet" );
	}
	this.lastRoundTopPixels    = nTopPixels;
	this.lastRoundBottomPixels = nBottomPixels;
      }
    }
  }


  private void doScaleImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      BufferedImage newImg = ScaleDlg.showDlg( this, entry.getImage() );
      if( newImg != null ) {
	showImageInternal(
		newImg,
		entry.getSharedExifDataCopyForResizedImage(),
		ImageEntry.Action.CHANGED,
		ImageUtil.probeMode( newImg ),
		this.imageFld.getRotation(),
		null,
		"Skaliert",
		null,
		null );
      }
    }
  }


  private void doSharpenImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.getAction() == ImageEntry.Action.SOFTENED ) {
	if( BaseDlg.showSuppressableConfirmDlg(
		this,
		"Statt zu sch\u00E4rfen wird der letzte Beabeitungsschritt"
			+ " \'Weichzeichnen\' zur\u00FCckgenommen." ) )
	{
	  doUndo();
	}
      } else {
	saveView();
	BufferedImage newImg = SharpenDlg.showDlg( this );
	if( newImg != null ) {
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.SHARPENED,
			entry.getMode(),
			"Gesch\u00E4rft" );
	} else {
	  restoreView();
	}
      }
    }
  }


  private void doSoftenImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.getAction() == ImageEntry.Action.SHARPENED ) {
	if( BaseDlg.showSuppressableConfirmDlg(
		this,
		"Statt weichzuzeichnen wird der letzte Beabeitungsschritt"
			+ " \'Sch\u00E4rfen\' zur\u00FCckgenommen." ) )
	{
	  doUndo();
	}
      } else {
	convolveAndShowImage(
			entry,
			new Kernel(
				3, 3,
				new float[] {
					0.07F, 0.1F,  0.07F,
					0.1F,  0.32F, 0.1F,
					0.07F, 0.1F,  0.07F } ),
			ImageEntry.Action.SOFTENED,
			"Weichgezeichnet" );
      }
    }
  }


  private void doToGray()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.isGray() || entry.isMonochrome() ) {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits in Graustufen" );
      } else {
	BufferedImage newImg = GrayScaler.toGray( this, entry.getImage() );
	if( newImg != null ) {
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			ImageEntry.Mode.GRAY,
			"Graustufen" );
	}
      }
    }
  }


  private void doToMonochrome( Dithering.Algorithm algorithm )
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.isMonochrome() ) {
	showImgAlreadyMonochrome();
      } else {
	BufferedImage newImg = Dithering.work(
					this,
					entry.getImage(),
					ImageUtil.getColorModelBW(),
					algorithm );
	if( newImg != null ) {
	  double viewScale = getViewScale();
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			ImageEntry.Mode.MONOCHROME,
			"Monochrom" );
	}
      }
    }
  }


  private void doThreshold()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.equalsMode( ImageEntry.Mode.MONOCHROME ) ) {
	showImgAlreadyMonochrome();
      } else {
	saveView();
	BufferedImage newImg = ThresholdDlg.showDlg( this );
	if( newImg != null ) {
	  showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			ImageEntry.Mode.MONOCHROME,
			"Monochrom" );
	} else {
	  restoreView();
	}
      }
    }
  }


  private void doInvertImage()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.getAction() == ImageEntry.Action.INVERTED ) {
	doUndo();
      } else {
	BufferedImage   srcImg = entry.getImage();
	IndexColorModel icm    = ImageUtil.getIndexColorModel( srcImg );
	if( icm != null ) {
	  int colorCnt = icm.getMapSize();
	  if( colorCnt > 0 ) {
	    boolean transp = false;
	    byte[]  alphas = new byte[ colorCnt ];
	    byte[]  reds   = new byte[ colorCnt ];
	    byte[]  greens = new byte[ colorCnt ];
	    byte[]  blues  = new byte[ colorCnt ];
	    for( int i = 0; i < colorCnt; i++ ) {
	      int argb = icm.getRGB( i );
	      int a    = (argb >> 24) & 0xFF;
	      if( a < 0xFF ) {
		transp = true;
	      }
	      alphas[ i ] = (byte) a;
	      reds[ i ]   = (byte) (0xFF - ((argb >> 16) & 0xFF));
	      greens[ i ] = (byte) (0xFF - ((argb >> 8) & 0xFF));
	      blues[ i ]  = (byte) (0xFF - (argb & 0xFF));
	    }
	    if( !transp ) {
	      alphas = null;
	    }
	    showSameBoundsImage(
			new BufferedImage(
				ImageUtil.createIndexColorModel(
						colorCnt,
						reds,
						greens,
						blues,
						alphas ),
				srcImg.getRaster(),
				false,
				new Hashtable<>() ),
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.INVERTED,
			entry.getMode(),			
			"Invertiert" );
	  }
	} else {
	  byte[] elements = new byte[ 256 ];
	  for( int i = 0; i < elements.length; i++ ) {
	    elements[ i ] = (byte) (0xFF - i);
	  }
	  filterAndShowImage(
			entry,
			new LookupOp(
				new ByteLookupTable( 0, elements ),
				null ),
			ImageEntry.Action.INVERTED,
			"Invertiert" );
	}
      }
    }
  }


  private void doRemoveTransparency()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      BufferedImage image = entry.getImage();
      if( image.getTransparency() != Transparency.OPAQUE ) {
	BufferedImage retImg = RemoveTransparencyDlg.showDlg(
							this,
							image );
	if( retImg != null ) {
	  showSameBoundsImage(
			retImg,
			entry.getSharedExifDataCopyForChangedImage(),
			ImageEntry.Action.CHANGED,
			ImageEntry.Mode.UNSPECIFIED,
			"Transparenz entfernt" );
	}
      }
    }
  }


  private void doToA5105()
  {
    ImageEntry entry = getCurImageEntry();
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
				ImageUtil.A5105_W,
				ImageUtil.A5105_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImageUtil.getColorModelA5105() ) ),
		entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		ImageEntry.Mode.A5105,
		ImageFld.Rotation.NONE,
		null,
		TEXT_A5105_FMT,
		null,
		null );
      }
    }
  }


  private void doToAC1(
		String          title,
		String          fontResource,
		ImageEntry.Mode mode )
  {
    ImageEntry entry = getCurImageEntry();
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
			entry.getSharedExifDataCopyForChangedImage(),
			title,
			fontResource,
			ImageUtil.AC1_W,
			ImageUtil.AC1_H,
			mode,
			6,
			true );
      }
    }
  }


  private void doToKC85Monochrome()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( entry.isKC85MonochromeFormat() && isUnrotated() ) {
	BaseDlg.showInfoDlg(
		this,
		"Das Bild ist bereits im KC85/2..5-Format ohne Farben." );
      } else {
	showImageInternal(
		drawImageTo(
			entry.getImage(),
			null,
			ImageUtil.createBlackKC85BWImage() ),
			entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		ImageEntry.Mode.MONOCHROME,
		ImageFld.Rotation.NONE,
		null,
		TEXT_KC85MONO_FMT,
		null,
		null );
      }
    }
  }


  private void doToKC854Hires()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      if( (entry.isKC854HiresFormat() || entry.isKC85MonochromeFormat())
	  && isUnrotated() )
      {
	BaseDlg.showInfoDlg(
			this,
			"Das Bild ist bereits im KC85/4,5-HIRES-Format." );
      } else {
	showImageInternal(
		drawImageTo(
			entry.getImage(),
			null,
			ImageUtil.createBlackKC854HiresImage() ),
		entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		ImageEntry.Mode.KC854_HIRES,
		ImageFld.Rotation.NONE,
		null,
		TEXT_KC854HIRES_FMT,
		null,
		null );
      }
    }
  }


  private void doToLLC2Hires( boolean for43 )
  {
    ImageEntry entry = getCurImageEntry();
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
				ImageUtil.LLC2_W,
				ImageUtil.LLC2_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImageUtil.getColorModelBW() ) ),
		entry.getSharedExifDataCopyForChangedImage(),
		ImageEntry.Action.CHANGED,
		ImageEntry.Mode.MONOCHROME,
		ImageFld.Rotation.NONE,
		null,
		TEXT_LLC2HIRES_FMT,
		null,
		null );
      }
    }
  }


  private void doToZ1013()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      toMonochromCharImage(
			entry.getImage(),
			entry.getSharedExifDataCopyForChangedImage(),
			"Z1013-Blockgrafik",
			"/rom/z1013/z1013font.bin",
			ImageUtil.Z1013_W,
			ImageUtil.Z1013_H,
			ImageEntry.Mode.Z1013,
			8,
			false );
    }
  }


  private void doToZ9001()
  {
    ImageEntry entry = getCurImageEntry();
    if( entry != null ) {
      toMonochromCharImage(
			entry.getImage(),
			entry.getSharedExifDataCopyForChangedImage(),
			"Z9001-Blockgrafik",
			"/rom/z9001/z9001font.bin",
			ImageUtil.Z9001_W,
			ImageUtil.Z9001_H,
			ImageEntry.Mode.Z9001,
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
	ImageEntry curEntry     = getCurImageEntry();
	Double     curViewScale = getViewScale();

	// zu verwerfende Eintraege uebergeben
	while( (idx + 1) < this.imgStack.size() ) {
	  this.imgStack.pop();
	}

	// anzuzeigendes Bild
	ImageEntry    entry = this.imgStack.pop();
	BufferedImage image = entry.getImage();

	/*
	 * Wenn die Bildgroesse und die Drehung sich nicht aendern,
	 * dann auch die Skalierung belassen
	 */
	Double viewScale = null;
	if( curEntry != null ) {
	  BufferedImage curImage = curEntry.getImage();
	  if( (curEntry.getRotation() == entry.getRotation())
	      && (curImage.getWidth() == image.getWidth())
	      && (curImage.getHeight() == image.getHeight()) )
	  {
	    viewScale = curViewScale;
	  }
	}

	// Bild anzeigen
	rebuildHistoryMenu();
	showImageInternal(
		image,
		entry.getExifData(),
		entry.getAction(),
		entry.getMode(),
		entry.getRotation(),
		viewScale,
		entry.getTitle(),
		entry.getFile(),
		entry.getMemBytes() );
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


    // Menu
    JMenu mnuFile = createMenuFile();

    // Menu Datei
    this.mnuOpen = createMenuItemWithStandardAccelerator(
					EmuUtil.TEXT_OPEN_OPEN,
					KeyEvent.VK_O );
    mnuFile.add( this.mnuOpen );

    this.mnuSaveAs = createMenuItemSaveAs( true );
    mnuFile.add( this.mnuSaveAs );

    JMenu mnuExp = GUIFactory.createMenu( "Exportieren als" );
    mnuFile.add( mnuExp );

    this.mnuExpImgA5105 = createMenuItem( "A5105-Bilddatei..." );
    mnuExp.add( this.mnuExpImgA5105 );

    this.mnuExpImgKC852Monochrome = createMenuItem(
				"KC85/2,3-Bilddatei ohne Farben..." );
    mnuExp.add( this.mnuExpImgKC852Monochrome );

    this.mnuExpImgKC854Monochrome = createMenuItem(
				"KC85/4,5-Bilddatei ohne Farben..." );
    mnuExp.add( this.mnuExpImgKC854Monochrome );

    this.mnuExpImgKC854Hires = createMenuItem(
					"KC85/4,5-HIRES-Bilddatei..." );
    mnuExp.add( this.mnuExpImgKC854Hires );

    this.mnuExpImgLLC2Hires = createMenuItem(
					"LLC2-HIRES-Bilddatei..." );
    mnuExp.add( this.mnuExpImgLLC2Hires );

    JMenu mnuExpMem = GUIFactory.createMenu(
		"Abbilddatei f\u00FCr Bildwiederholspeicher f\u00FCr" );
    mnuExp.add( mnuExpMem );

    this.mnuExpMemAC1 = createMenuItem( "AC1..." );
    mnuExpMem.add( this.mnuExpMemAC1 );

    this.mnuExpMemZ1013 = createMenuItem( "Z1013..." );
    mnuExpMem.add( this.mnuExpMemZ1013 );

    this.mnuExpMemZ9001 = createMenuItem( "Z9001..." );
    mnuExpMem.add( this.mnuExpMemZ9001 );

    JMenu mnuExpApp = GUIFactory.createMenu(
			"Programm zur Anzeige des Bildes im" );
    mnuExp.add( mnuExpApp );

    this.mnuExpAppAC1 = createMenuItem( "AC1..." );
    mnuExpApp.add( this.mnuExpAppAC1 );

    this.mnuExpAppKC852 = createMenuItem( "KC85/2,3..." );
    mnuExpApp.add( this.mnuExpAppKC852 );

    this.mnuExpAppKC854 = createMenuItem( "KC85/4,5..." );
    mnuExpApp.add( this.mnuExpAppKC854 );

    this.mnuExpAppLLC2Hires = createMenuItem( "LLC2..." );
    mnuExpApp.add( this.mnuExpAppLLC2Hires );

    this.mnuExpAppZ1013 = createMenuItem( "Z1013..." );
    mnuExpApp.add( this.mnuExpAppZ1013 );

    this.mnuExpAppZ9001 = createMenuItem( "Z9001..." );
    mnuExpApp.add( this.mnuExpAppZ9001 );

    this.mnuPrint = createMenuItemOpenPrint( true );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuImgProps = createMenuItem( "Bildeigenschaften..." );
    mnuFile.add( this.mnuImgProps );

    this.mnuExifEdit = createMenuItem( "Zusatzinformationen bearbeiten..." );
    mnuFile.add( this.mnuExifEdit );

    this.mnuExifRemove = createMenuItem( "Zusatzinformationen entfernen" );
    mnuFile.add( this.mnuExifRemove );

    mnuFile.addSeparator();

    this.mnuPrev = createMenuItemWithStandardAccelerator(
						"Vorheriges Bild",
						KeyEvent.VK_LEFT );
    mnuFile.add( this.mnuPrev );

    this.mnuNext = createMenuItemWithStandardAccelerator(
						"N\u00E4chstes Bild",
						KeyEvent.VK_RIGHT );
    mnuFile.add( this.mnuNext );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    this.mnuEdit = createMenuEdit();

    this.mnuUndo = createMenuItemWithStandardAccelerator(
					"R\u00FCckg\u00E4ngig",
					KeyEvent.VK_Z );
    this.mnuEdit.add( this.mnuUndo );

    this.mnuHistory = GUIFactory.createMenu( "Historie" );
    this.mnuEdit.add( this.mnuHistory );
    this.mnuEdit.addSeparator();

    this.mnuCopy = createMenuItemWithStandardAccelerator(
					"Bild kopieren",
					KeyEvent.VK_C );
    this.mnuEdit.add( this.mnuCopy );

    this.mnuPaste = createMenuItemWithStandardAccelerator(
					"Bild einf\u00FCgen",
					KeyEvent.VK_V );
    this.mnuEdit.add( this.mnuPaste );
    this.mnuEdit.addSeparator();

    this.mnuFlipHorizontal = createMenuItem( "Spiegeln" );
    this.mnuEdit.add( this.mnuFlipHorizontal );

    this.mnuRotateImage = createMenuItemWithStandardAccelerator(
					"Drehen...",
					KeyEvent.VK_R );
    this.mnuEdit.add( this.mnuRotateImage );

    this.mnuCropImage = createMenuItemWithStandardAccelerator(
					"Zuschneiden...",
					KeyEvent.VK_C,
					true );
    this.mnuEdit.add( this.mnuCropImage );

    this.mnuScaleImage = createMenuItem( "Skalieren..." );
    this.mnuEdit.add( this.mnuScaleImage );

    this.mnuSharpenImage = createMenuItem( "Sch\u00E4rfen..." );
    this.mnuEdit.add( this.mnuSharpenImage );

    this.mnuSoftenImage = createMenuItem( "Weichzeichnen" );
    this.mnuEdit.add( this.mnuSoftenImage );

    this.mnuDetectEdges = createMenuItem(
				"Konturen erkennen und darstellen" );
    this.mnuEdit.add( this.mnuDetectEdges );
    this.mnuEdit.addSeparator();

    this.mnuRoundCorners = createMenuItem( "Ecken abrunden..." );
    this.mnuEdit.add( this.mnuRoundCorners );

    this.mnuAdjustImage = createMenuItem(
				"Helligkeit, Kontrast, Farben..." );
    this.mnuEdit.add( this.mnuAdjustImage );

    this.mnuIndexColors = createMenuItem(
				"Farben reduzieren und indexieren..." );
    this.mnuEdit.add( this.mnuIndexColors );

    this.mnuToBW = GUIFactory.createMenu( "Schwarz/Wei\u00DF wandeln" );
    this.mnuEdit.add( this.mnuToBW );

    this.mnuToGray = createMenuItem( "Graustufen" );
    this.mnuToBW.add( this.mnuToGray );

    this.mnuThreshold = createMenuItem( "Monochrom mittels Schwellwert" );
    this.mnuToBW.add( this.mnuThreshold );

    JMenu mnuToMonoDith = GUIFactory.createMenu(
					"Monochrom mittels Dithering" );
    this.mnuToBW.add( mnuToMonoDith );

    this.mnuToMonoFloydSteinberg = createMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.FLOYD_STEINBERG ) );
    mnuToMonoDith.add( this.mnuToMonoFloydSteinberg );

    this.mnuToMonoSierra3 = createMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.SIERRA3 ) );
    mnuToMonoDith.add( this.mnuToMonoSierra3 );

    this.mnuToMonoAtkinson = createMenuItem(
	Dithering.getAlgorithmText( Dithering.Algorithm.ATKINSON ) );
    mnuToMonoDith.add( this.mnuToMonoAtkinson );

    this.mnuInvertImage = createMenuItem( "Invertieren" );
    this.mnuEdit.add( this.mnuInvertImage );

    this.mnuRemoveTransparency = createMenuItem(
					"Transparenz entfernen..." );
    this.mnuEdit.add( this.mnuRemoveTransparency );

    this.mnuColorPalette = createMenuItem( "Farbpalette..." );
    this.mnuEdit.add( this.mnuColorPalette );
    this.mnuEdit.addSeparator();

    this.mnuConvert = GUIFactory.createMenu( "Konvertieren in" );
    this.mnuEdit.add( mnuConvert );

    this.mnuToA5105 = createMenuItem( TEXT_A5105_FMT );
    this.mnuConvert.add( this.mnuToA5105 );

    this.mnuToAC1_ACC = createMenuItem( TEXT_AC1_ACC_FMT );
    this.mnuConvert.add( this.mnuToAC1_ACC );

    this.mnuToAC1_SCCH = createMenuItem( TEXT_AC1_SCCH_FMT );
    this.mnuConvert.add( this.mnuToAC1_SCCH );

    this.mnuToAC1_2010 = createMenuItem( TEXT_AC1_2010_FMT );
    this.mnuConvert.add( this.mnuToAC1_2010 );

    this.mnuToKC85Monochrome = createMenuItem( TEXT_KC85MONO_FMT );
    this.mnuConvert.add( this.mnuToKC85Monochrome );

    this.mnuToKC854Hires = createMenuItem( TEXT_KC854HIRES_FMT );
    this.mnuConvert.add( this.mnuToKC854Hires );

    this.mnuToLLC2Hires21 = createMenuItem(
		TEXT_LLC2HIRES_FMT + " ohne Anpassung f\u00FCr 4:3-Anzeige" );
    this.mnuConvert.add( this.mnuToLLC2Hires21 );

    this.mnuToLLC2Hires43 = createMenuItem(
		TEXT_LLC2HIRES_FMT + " mit Anpassung f\u00FCr 4:3-Anzeige" );
    this.mnuConvert.add( this.mnuToLLC2Hires43 );

    this.mnuToZ1013 = createMenuItem( TEXT_Z1013_FMT );
    this.mnuConvert.add( this.mnuToZ1013 );

    this.mnuToZ9001 = createMenuItem( TEXT_Z9001_FMT );
    this.mnuConvert.add( this.mnuToZ9001 );


    // Menu Einstellungen
    JMenu mnuSettings = createMenuSettings();

    this.mnuAutoResize = GUIFactory.createCheckBoxMenuItem(
				"Fenster an Bildgr\u00F6\u00DFe anpassen",
				false );
    mnuSettings.add( this.mnuAutoResize );
    mnuSettings.addSeparator();

    JMenu mnuBgColor = GUIFactory.createMenu( "Hintergrundfarbe" );
    mnuSettings.add( mnuBgColor );

    ButtonGroup grpBgColor = new ButtonGroup();

    this.mnuBgSystem = GUIFactory.createRadioButtonMenuItem(
							"System",
							true );
    this.mnuBgSystem.addActionListener( this );
    grpBgColor.add( this.mnuBgSystem );
    mnuBgColor.add( this.mnuBgSystem );
    mnuBgColor.addSeparator();

    this.mnuBgBlack = GUIFactory.createRadioButtonMenuItem( "schwarz" );
    this.mnuBgBlack.addActionListener( this );
    grpBgColor.add( this.mnuBgBlack );
    mnuBgColor.add( this.mnuBgBlack );

    this.mnuBgGray = GUIFactory.createRadioButtonMenuItem( "grau" );
    this.mnuBgGray.addActionListener( this );
    grpBgColor.add( this.mnuBgGray );
    mnuBgColor.add( this.mnuBgGray );

    this.mnuBgWhite = GUIFactory.createRadioButtonMenuItem( "wei\u00DF" );
    this.mnuBgWhite.addActionListener( this );
    grpBgColor.add( this.mnuBgWhite );
    mnuBgColor.add( this.mnuBgWhite );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem(
		"Hilfe zu Bildbetrachter/Bildbearbeitung..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar(
					mnuFile,
					this.mnuEdit,
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

    this.btnOpen = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					this.mnuOpen.getText() );
    this.btnOpen.addActionListener( this );
    toolBar.add( this.btnOpen );

    this.btnSaveAs = GUIFactory.createRelImageResourceButton(
					this,
					"file/save_as.png",
					this.mnuSaveAs.getText() );
    this.btnSaveAs.addActionListener( this );
    toolBar.add( this.btnSaveAs );

    this.btnPrint = GUIFactory.createRelImageResourceButton(
					this,
					"file/print.png",
					this.mnuPrint.getText() );
    this.btnPrint.addActionListener( this );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnRotateLeft = GUIFactory.createRelImageResourceButton(
					this,
					"edit/rotate_left.png",
					"Nach links drehen" );
    this.btnRotateLeft.addActionListener( this );
    toolBar.add( this.btnRotateLeft );

    this.btnRotateRight = GUIFactory.createRelImageResourceButton(
					this,
					"edit/rotate_right.png",
					"Nach rechts drehen" );
    this.btnRotateRight.addActionListener( this );
    toolBar.add( this.btnRotateRight );
    toolBar.addSeparator();

    this.comboViewScale = createScaleComboBox(
		100, 10, 15, 25, 33, 50, 75, 100, 200, 300, 400 );
    toolBar.add( this.comboViewScale );

    this.btnFitImage = GUIFactory.createRelImageResourceButton(
				this,
				"edit/fit.png",
				"Bild an Fenstergr\u00F6\u00DFe anpassen" );
    this.btnFitImage.addActionListener( this );
    toolBar.add( this.btnFitImage );
    toolBar.addSeparator();

    this.btnPrev = GUIFactory.createRelImageResourceButton(
					this,
					"nav/back.png",
					this.mnuPrev.getText() );
    this.btnPrev.addActionListener( this );
    toolBar.add( this.btnPrev );
    toolBar.addSeparator();

    this.btnNext = GUIFactory.createRelImageResourceButton(
					this,
					"nav/next.png",
					this.mnuNext.getText() );
    this.btnNext.addActionListener( this );
    toolBar.add( this.btnNext );
    toolBar.addSeparator();


    // Statuszeile
    this.labelStatus = GUIFactory.createLabel( DEFAULT_STATUS_TEXT );
    JPanel panelStatus = GUIFactory.createPanel(
		new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelStatus.add( this.labelStatus );
    add( panelStatus, BorderLayout.SOUTH );


    // Drop-Ziele
    (new DropTarget( this.imageFld, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }


    // sonstiges
    this.imageFld.addMouseListener( this );
    this.imageFld.addMouseMotionListener( this );
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
		null,
		ImageEntry.Action.INITIAL_LOADED,
		ImageEntry.Mode.UNSPECIFIED,
		ImageFld.Rotation.NONE,
		null,
		null,
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


  private BufferedImage convolveAndShowImage(
					ImageEntry        entry,
					Kernel            kernel,
					ImageEntry.Action action,
					String            title )
  {
    return filterAndShowImage(
		entry,
		new ConvolveOp(
			kernel,
			ConvolveOp.EDGE_NO_OP,
			new RenderingHints(
					RenderingHints.KEY_DITHERING,
					RenderingHints.VALUE_DITHER_ENABLE ) ),
		action,
		title );
  }


  private BufferedImage drawImageTo(
				BufferedImage srcImg,
				Float         ratioCorrection,
				BufferedImage dstImg )
  {
    if( (srcImg != null) && (dstImg != null) ) {
      float             srcW     = (float) srcImg.getWidth();
      float             srcH     = (float) srcImg.getHeight();
      ImageFld.Rotation rotation = this.imageFld.getRotation();
      if( rotation.equals( ImageFld.Rotation.LEFT )
	  || rotation.equals( ImageFld.Rotation.RIGHT ) )
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
      g.setColor( Color.BLACK );
      g.fillRect( 0, 0, dstW, dstH );
      if( !rotation.equals( ImageFld.Rotation.NONE ) ) {
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


  private BufferedImage filterAndShowImage(
				ImageEntry        entry,
				BufferedImageOp   op,
				ImageEntry.Action action,
				String            title )
  {
    BufferedImage newImg = null;
    if( entry != null ) {
      newImg = op.filter( entry.getImage(), null );
      showSameBoundsImage(
			newImg,
			entry.getSharedExifDataCopyForChangedImage(),
			action,
			entry.getMode(),
			title );
    }
    return newImg;
  }


  private void fitImage( boolean allowScaleUp )
  {
    boolean   fitBtnState = false;
    Dimension imgSize     = this.imageFld.getRotatedImageSize();
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
	    this.imageFld.setScale(
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


  private ImageEntry getCurImageEntry()
  {
    return this.imgStack.isEmpty() ? null : this.imgStack.peek();
  }


  private Dimension getMaxContentSize()
  {
    Dimension rv = null;
    Toolkit   tk = EmuUtil.getToolkit( this );
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
	ImageEntry entry = getCurImageEntry();
	if( entry != null ) {
	  entry.setFile( file );
	}
      }
    }
  }


  private boolean isUnrotated()
  {
    return this.imageFld.getRotation().equals( ImageFld.Rotation.NONE );
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
      JMenuItem item = GUIFactory.createMenuItem( title );
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
    this.imageFld.save();
  }


  private void restoreView()
  {
    this.imageFld.restore();

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
    buf.append( TITLE );
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
	  buf.append( ')' );
	}
      }
    }
    setTitle( buf.toString() );
  }


  private ImageEntry showSameBoundsImage(
				BufferedImage     image,
				ExifData          exifData,
				ImageEntry.Action action,
				ImageEntry.Mode   mode,
				String            title )
  {
    return showImageInternal(
			image,
			exifData,
			action,
			mode,
			this.imageFld.getRotation(),
			getViewScale(),
			title,
			null,
			null );
  }


  private boolean showImageFile(
			File   file,
			byte[] fileBytes,
			String fileName )
  {
    boolean rv = false;
    if( (file != null) || (fileBytes != null) ) {
      ImageEntry entry  = null;
      String   errMsg = null;
      try {
	entry = ImageLoader.load( file, fileBytes, fileName );
	if( entry != null ) {
	  String title = fileName;
	  if( (title == null) && (file != null) ) {
	    title = file.getName();
	    if( title == null ) {
	      title = file.getPath();
	    }
	  }
	  if( title != null ) {
	    if( title.isEmpty() ) {
	      title = null;
	    }
	  }
	  BufferedImage image = entry.getImage();
	  showImageInternal(
			image,
			entry.getExifData(),
			ImageEntry.Action.INITIAL_LOADED,
			entry.getMode(),
			ImageFld.Rotation.NONE,
			null,
			title,
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
	  buf.append( '.' );
	}
	BaseDlg.showErrorDlg( this, buf.toString() );
      }
    }
    return rv;
  }


  private ImageEntry showImageInternal(
			BufferedImage     image,
			ExifData          exifData,
			ImageEntry.Action action,
			ImageEntry.Mode   mode,
			ImageFld.Rotation rotation,
			Double            scale,
			String            title,
			File              file,
			byte[]            videoMemBytes )
  {
    ImageEntry newEntry   = null;
    boolean    autoResize = false;
    boolean    state      = false;
    int        wImg       = 0;
    int        hImg       = 0;
    double     myScale    = 1.0;
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
    boolean    transp   = false;
    ImageEntry oldEntry = getCurImageEntry();
    if( action == ImageEntry.Action.INITIAL_LOADED ) {
      this.imgStack.clear();
      clearHistoryMenu();
    } else {
      oldEntry.setRotation( this.imageFld.getRotation() );
    }
    if( image != null ) {
      newEntry = new ImageEntry(
			image,
			exifData,
			action,
			mode,
			rotation,
			title,
			file,
			videoMemBytes );
      this.imgStack.push( newEntry );
      rebuildHistoryMenu();
      transp = (image.getTransparency() != Transparency.OPAQUE);
    }
    this.scrollPane.invalidate();
    this.imageFld.setImage( image );
    if( scale != null ) {
      this.imageFld.setScale( scale.doubleValue() );
      this.autoFitImage = false;
    } else {
      this.imageFld.setScale( myScale );
      this.autoFitImage = true;
    }
    this.imageFld.setRotation( rotation );
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
    this.mnuExpImgKC852Monochrome.setEnabled( state );
    this.mnuExpImgKC854Monochrome.setEnabled( state );
    this.mnuExpImgKC854Hires.setEnabled( state );
    this.mnuExpImgLLC2Hires.setEnabled( state );
    this.mnuExpAppAC1.setEnabled( state );
    this.mnuExpAppKC852.setEnabled( state );
    this.mnuExpAppKC854.setEnabled( state );
    this.mnuExpAppLLC2Hires.setEnabled( state );
    this.mnuExpAppZ1013.setEnabled( state );
    this.mnuExpAppZ9001.setEnabled( state );
    this.mnuExpMemAC1.setEnabled( state );
    this.mnuExpMemZ1013.setEnabled( state );
    this.mnuExpMemZ9001.setEnabled( state );
    this.mnuColorPalette.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuImgProps.setEnabled( state );
    this.mnuExifEdit.setEnabled( state );
    this.mnuExifRemove.setEnabled( state );
    this.mnuUndo.setEnabled( this.imgStack.size() > 1 );
    this.mnuHistory.setEnabled( this.imgStack.size() > 1 );
    this.mnuCopy.setEnabled( state );
    this.mnuAdjustImage.setEnabled( state );
    this.mnuCropImage.setEnabled( state );
    this.mnuDetectEdges.setEnabled( state );
    this.mnuFlipHorizontal.setEnabled( state );
    this.mnuIndexColors.setEnabled( state );
    this.mnuRoundCorners.setEnabled( state );
    this.mnuRotateImage.setEnabled( state );
    this.mnuScaleImage.setEnabled( state );
    this.mnuSharpenImage.setEnabled( state );
    this.mnuSoftenImage.setEnabled( state );
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
    this.mnuToKC85Monochrome.setEnabled( state );
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
    return newEntry;
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
			BufferedImage   image,
			ExifData        exifData,
			String          title,
			String          fontResource,
			int             w,
			int             h,
			ImageEntry.Mode mode,
			int             chXPixels,
			boolean         ac1 )
  {
    // zuerst monochrome und skalierte Vollgrafik erzeugen
    BufferedImage newImg = new BufferedImage(
					w,
					h,
					BufferedImage.TYPE_BYTE_BINARY,
					ImageUtil.getColorModelBW() );
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
		exifData,
		ImageEntry.Action.CHANGED,
		mode,
		ImageFld.Rotation.NONE,
		null,
		title,
		null,
		videoMemBytes );
  }


  private Point toUnscaledPoint( MouseEvent e )
  {
    double f = this.imageFld.getCurScale();
    return new Point(
		(int) Math.round( (double) e.getX() / f ),
		(int) Math.round( (double) e.getY() / f ) );
  }


  private void updCropDlg()
  {
    if( this.cropDlg != null ) {
      if( this.cropDlg.isVisible() ) {
	Dimension imgSize = this.imageFld.getRotatedImageSize();
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
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.imageFlavor );
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
				ImageLoader.accept( new File( dir, fName ) )
				: false;
			  }
			} );
	  if( files != null ) {
	    if( files.length > 1 ) {
	      try {
		Arrays.sort( files, FileComparator.getInstance() );
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
