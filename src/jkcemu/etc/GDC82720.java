/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Graphics Display Controllers U82720
 * (kompatibel zu Intel 82720)
 */

package jkcemu.etc;

import java.lang.*;
import z80emu.Z80CPU;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class GDC82720 implements
				Z80MaxSpeedListener,
				Z80TStatesListener
{
  public interface GDCListener
  {
    public void screenConfigChanged( GDC82720 gdc );
    public void screenDirty( GDC82720 gdc );
  };


  public interface VRAM
  {
    public int  getVRAMWord( int addr );
    public void setVRAMWord( int addr, int value );
  };

  public enum Mode { CHARACTER, GRAPHICS, MIXED, INVALID };

  public static final int DISPL_ADDR_MASK     = 0x03FFFF;
  public static final int DISPL_IMAGE_MASK    = 0x040000;
  public static final int DISPL_CURSOR_MASK   = 0x080000;
  public static final int DISPL_NEW_CROW_MASK = 0x100000;
  public static final int DISPL_BLINK_MASK    = 0x200000;

  private final static int STATUS_DATA_AVAILABLE = 0x01;
  private final static int STATUS_FIFO_EMPTY     = 0x04;
  private final static int STATUS_VSYNC_ACTIVE   = 0x20;
  private final static int STATUS_HBLANK_ACTIVE  = 0x40;

  private GDCListener     gdcListener;
  private VRAM            vram;
  private volatile byte[] pram;
  private volatile int    pramLen;
  private Mode            mode;
  private int[]           args;
  private int             argIdx;
  private int             cmd;
  private int             nBytesToRead;
  private int             displayLines;
  private int             lineCounter;
  private int             lineTStateCounter;
  private volatile int    tStatesPerLine;
  private volatile int    tStatesPerHBlank;
  private int             mask;
  private int             statusReg;
  private int             linesPerCharRow;
  private int             wordsPerCharRow;
  private int             memWordsPerChRow;
  private boolean         drawingSL;
  private boolean         drawingR;
  private boolean         drawingA;
  private boolean         drawingGC;
  private boolean         drawingL;
  private boolean         drawingGD;
  private int             drawingDir;
  private int             drawingD;
  private int             drawingDC;
  private int             drawingD1;
  private int             drawingD2;
  private int             drawingDM;
  private int             blinkCounter;
  private int             blinkRate;
  private int             charBlinkNum;
  private int             cursorAddr;
  private int             cursorDotAddr;
  private int             cursorTopLine;
  private int             cursorBottomLine;
  private boolean         cursorBlinkState;
  private boolean         cursorBlinking;
  private boolean         cursorEnabled;
  private boolean         screenEnabled;


  public GDC82720()
  {
    this.gdcListener       = null;
    this.vram              = null;
    this.pram              = new byte[ 16 ];
    this.pramLen           = 0;
    this.mode              = Mode.INVALID;
    this.args              = new int[ 16 ];
    this.argIdx            = 0;
    this.cmd               = 0;
    this.nBytesToRead      = 0;
    this.displayLines      = 0;
    this.lineCounter       = 0;
    this.lineTStateCounter = 0;
    this.tStatesPerLine    = 0;
    this.tStatesPerHBlank  = 0;
    this.mask              = 0;
    this.linesPerCharRow   = 0;
    this.wordsPerCharRow   = 0;
    this.memWordsPerChRow  = 0;
    this.drawingSL         = false;
    this.drawingR          = false;
    this.drawingA          = false;
    this.drawingGC         = false;
    this.drawingL          = false;
    this.drawingGD         = false;
    this.drawingDir        = 0;
    this.drawingD          = 0;
    this.drawingDC         = 0;
    this.drawingD1         = 0;
    this.drawingD2         = 0;
    this.drawingDM         = 0;
    this.blinkCounter      = 0;
    this.blinkRate         = 0;
    this.charBlinkNum      = 0;
    this.cursorAddr        = 0;
    this.cursorDotAddr     = 0;
    this.cursorTopLine     = 0;
    this.cursorBottomLine  = 0;
    this.cursorBlinkState  = false;
    this.cursorBlinking    = false;
    this.cursorEnabled     = false;
    this.screenEnabled     = false;
    this.statusReg         = STATUS_FIFO_EMPTY;
  }


  public boolean canExtractScreenText()
  {
    return (getCharRowCount() > 0);
  }


  public int getCharColCount()
  {
    return this.wordsPerCharRow;
  }


  public int getCharRowCount()
  {
    int rv = 0;
    if( this.screenEnabled ) {
      /*
       * Bei weniger als 8 Pixelzeilen pro Zeichenzeile wird davon
       * ausgegangen, dass Grafik simuliert wird
       * statt echte Zeichen darzustellen.
       */
      int hChar = this.linesPerCharRow;
      if( hChar >= 8 ) {
	if( this.mode == Mode.CHARACTER ) {
	  for( int i = 0; i < 4; i++ ) {
	    int idx = i * 4;
	    if( (idx + 3) >= this.pramLen ) {
	      break;
	    }
	    int len = ((this.pram[ idx + 2 ] >> 4) & 0x0F)
				| ((this.pram[ idx + 3 ] << 4) & 0x3F0);
	    rv += (len / hChar);
	    if( (len % hChar) != 0 ) {
	      break;
	    }
	  }
	} else if( this.mode == Mode.MIXED ) {
	  for( int i = 0; i < 2; i++ ) {
	    int idx = i * 4;
	    if( (idx + 3) >= this.pramLen ) {
	      break;
	    }
	    if( (this.pram[ idx + 3 ] & 0x40) == 0 ) {
	      int len = ((this.pram[ idx + 2 ] >> 4) & 0x0F)
				| ((this.pram[ idx + 3 ] << 4) & 0x3F0);
	      rv += (len / hChar);
	      if( (len % hChar) != 0 ) {
		break;
	      }
	    }
	  }
	}
	if( (rv * hChar) > this.displayLines ) {
	  rv = this.displayLines / hChar;
	}
      }
    }
    return rv;
  }


  public int getCharRowHeight()
  {
    return this.linesPerCharRow;
  }


  public int getCharTopLine()
  {
    int rv = 0;
    if( (this.mode == Mode.MIXED)
	&& (this.pramLen >= 4)
	&& ((this.pram[ 3 ] & 0x40) != 0) )
    {
      /*
       * Wenn im Mixed-Mode der erste Bereich Grafik ist,
       * beginnt die erste Zeichenzeile unter diesem Grafikbereich.
       * Der Fall, dass auch der zweite Bereich Grafik ist,
       * wird hier nicht weiter beachtet,
       * da in dem Fall die Methode getCharRowCount() eine Null liefert
       * und somit das Ergebnis von getCharTopLine() egal ist.
       */
      rv = ((this.pram[ 2 ] >> 4) & 0x0F) | ((this.pram[ 3 ] << 4) & 0x3F0);
    }
    return rv;
  }


  public int getDisplayLines()
  {
    return this.displayLines;
  }


  /*
   * Diese Methode liefert die Adresse im Video-Speicher sowie zusaetzliche
   * Informationen fuer die anzuzeigende Bildschirmposition (x,y)
   * und repraesentiert damit den Zustand der Ausgaenge AD0-AD17 des GDC
   * zum Zeitpunkt eines Visualisierungszyklusses.
   * Da im Original die Ausgaenge AD16 und AD17 jeweils zwei Informationen
   * zeitlich hintereinander uebertragen,
   * werden diese Informationen hier auf hoehere Bits gelegt.
   * Die einzelnen Werte werden durch bitweise UND-Verknuepfung
   * des Rueckgabewert mit den Konstanten DISPL_... extrahiert.
   * Die Konstante DISPL_IMAGE_MODE markiert das Image Flag,
   * d.h. die Ausgabe hat im Pixelmode zu erfolgen
   * und x muss anschliessend um die Anzahl der horizontalen Pixel
   * pro Datenwort (gewoehnlich 16) erhoeht werden.
   * Ist das Bit nicht gesetzt, muss x um eins erhoeht werden
   * (naechste Character-Position).
   * y steht fuer die Pixelzeile.
   * Ein Rueckgabewert von -1 besagt, dass keine Ausgabe erfolgen soll,
   * z.B. weil der GDC gerade auf Blanking steht oder
   * weil die Position (x,y) ausserhalb des aktiven Fensters liegt.
   */
  public int getDisplayValue( int x, int y )
  {
    int rv = -1;
    if( this.screenEnabled && (y >= 0) && (y < this.displayLines) ) {
      if( this.mode == Mode.CHARACTER ) {
	if( this.linesPerCharRow > 0 ) {
	  int n = Math.min( this.pramLen, 16 );
	  int pBegY       = 0;
	  for( int i = 0; i < n; i += 4 ) {
	    int pLines = ((this.pram[ i + 2 ] >> 4) & 0x0F)
				| ((this.pram[ i + 3 ] << 4) & 0x3F0);
	    if( (pLines > 0) && (y < (pBegY + pLines)) ) {
	      int pY    = y - pBegY;
	      int pAddr = ((this.pram[ i + 1 ] << 8) & 0x0F00)
				| (this.pram[ i ] & 0x00FF);
	      rv = pAddr + ((pY / this.linesPerCharRow)
					* this.memWordsPerChRow) + x;
	      int crsY = pY % this.linesPerCharRow;
	      if( this.cursorEnabled
		  && (rv == this.cursorAddr)
		  && (crsY >= this.cursorTopLine)
		  && (crsY <= this.cursorBottomLine) )
	      {
		if( !this.cursorBlinking || this.cursorBlinkState ) {
		  rv |= DISPL_CURSOR_MASK;
		}
	      }
	      if( crsY == 0 ) {
		rv |= DISPL_NEW_CROW_MASK;
	      }
	      break;
	    }
	    pBegY += pLines;
	  }
	}
      } else if( (this.mode == Mode.GRAPHICS)
		 || (this.mode == Mode.MIXED) )
      {
	int n     = Math.min( this.pramLen, 8 );
	int pBegY = 0;
	for( int i = 0; i < n; i += 4 ) {
	  int pLines = ((this.pram[ i + 2 ] >> 4) & 0x0F)
				| ((this.pram[ i + 3 ] << 4) & 0x3F0);
	  if( (pLines > 0) && (y < (pBegY + pLines)) ) {
	    int pY    = y - pBegY;
	    int pAddr = ((this.pram[ i + 2 ] << 16) & 0x30000)
				| ((this.pram[ i + 1 ] << 8) & 0x0FF00)
				| (this.pram[ i ] & 0x000FF);
	    if( (this.mode == Mode.MIXED)
		&& ((this.pram[ i + 3 ] & 0x40) == 0) )
	    {
	      if( this.linesPerCharRow > 0 ) {
		rv = pAddr + ((pY / this.linesPerCharRow)
					* this.memWordsPerChRow) + x;
		int crsY = pY % this.linesPerCharRow;
		if( this.cursorEnabled
		    && (rv == this.cursorAddr)
		    && (crsY >= this.cursorTopLine)
		    && (crsY <= this.cursorBottomLine) )
		{
		  if( !this.cursorBlinking || this.cursorBlinkState ) {
		    rv |= DISPL_CURSOR_MASK;
		  }
		}
		if( crsY == 0 ) {
		  rv |= DISPL_NEW_CROW_MASK;
		}
		if( this.charBlinkNum < 3 ) {
		  rv |= DISPL_BLINK_MASK;
		}
	      }
	    } else {
	      rv = pAddr + (pY * this.memWordsPerChRow) + x;
	    }
	    break;
	  }
	  pBegY += pLines;
	}
      }
    }
    return rv;
  }


  public Mode getMode()
  {
    return this.mode;
  }


  public int getScreenChar( int chX, int chY )
  {
    int rv = -1;
    int hChar = this.linesPerCharRow;
    if( (chX >= 0) && (chY >= 0)
	&& (chX < this.wordsPerCharRow)
	&& (hChar > 0)
	&& ((chY * hChar) < this.displayLines) )
    {
      int rowAddr = -1;
      if( this.mode == Mode.CHARACTER ) {
	for( int i = 0; i < 4; i++ ) {
	  int idx = i * 4;
	  if( (idx + 3) >= this.pramLen ) {
	    break;
	  }
	  int len = ((this.pram[ idx + 2 ] >> 4) & 0x0F)
				| ((this.pram[ idx + 3 ] << 4) & 0x3F0);
	  int rows = (len / hChar);
	  if( chY < rows ) {
	    rowAddr = ((this.pram[ idx + 1 ] << 8) & 0x01F00)
				| (this.pram[ idx ] & 0x000FF);
	    break;
	  }
	  if( (len % hChar) != 0 ) {
	    break;
	  }
	  chY -= rows;
	}
      } else if( this.mode == Mode.MIXED ) {
	for( int i = 0; i < 2; i++ ) {
	  int idx = i * 4;
	  if( (idx + 3) >= this.pramLen ) {
	    break;
	  }
	  if( (this.pram[ idx + 3 ] & 0x40) == 0 ) {
	    int len = ((this.pram[ idx + 2 ] >> 4) & 0x0F)
				| ((this.pram[ idx + 3 ] << 4) & 0x3F0);
	    int rows = (len / hChar);
	    if( chY < rows ) {
	      rowAddr = ((this.pram[ idx + 2 ] << 16) & 0x30000)
				| ((this.pram[ idx + 1 ] << 8) & 0x0FF00)
				| (this.pram[ idx ] & 0x000FF);
	      break;
	    }
	    if( (len % hChar) != 0 ) {
	      break;
	    }
	    chY -= rows;
	  }
	}
      }
      if( rowAddr >= 0 ) {
	rv = this.vram.getVRAMWord(
		rowAddr + (chY * this.memWordsPerChRow) + chX ) & 0xFF;
      }
    }
    return rv;
  }


  public int readData()
  {
    int rv = 0;
    if( (this.nBytesToRead > 0)
	&& ((this.statusReg & STATUS_DATA_AVAILABLE) != 0) )
    {
      if( (this.cmd & 0xE4) == 0xA0 ) {			// RDAT
	switch( this.cmd & 0x18 ) {
	  case 0x00:
	    if( (this.nBytesToRead & 0x01) == 0 ) {
	      rv = this.vram.getVRAMWord( this.cursorAddr ) & 0xFF;
	      --this.nBytesToRead;
	    } else {
	      rv = (this.vram.getVRAMWord( this.cursorAddr ) >> 8) & 0xFF;
	      --this.nBytesToRead;
	      this.cursorAddr = (this.cursorAddr + 1) & 0x3FFFF;
	    }
	    break;
	  case 0x10:
	    rv = this.vram.getVRAMWord( this.cursorAddr ) & 0xFF;
	    --this.nBytesToRead;
	    this.cursorAddr = (this.cursorAddr + 1) & 0x3FFFF;
	    break;
	  case 0x18:
	    rv = (this.vram.getVRAMWord( this.cursorAddr ) >> 8) & 0xFF;
	    --this.nBytesToRead;
	    this.cursorAddr = (this.cursorAddr + 1) & 0x3FFFF;
	    break;
	}
	if( this.nBytesToRead <= 0 ) {
	  this.drawingDC = 0;
	}
      } else if( this.cmd == 0xE0 ) {			// CURD
	switch( this.nBytesToRead ) {
	  case 5:
	    rv = this.cursorAddr & 0xFF;;
	    --this.nBytesToRead;
	    break;
	  case 4:
	    rv = (this.cursorAddr >> 8) & 0xFF;
	    --this.nBytesToRead;
	    break;
	  case 3:
	    rv = (this.cursorAddr >> 16) & 0x03;
	    --this.nBytesToRead;
	    break;
	  case 2:
	    rv = this.cursorDotAddr & 0xFF;
	    --this.nBytesToRead;
	    break;
	  case 1:
	    rv = (this.cursorDotAddr >> 8) & 0xFF;
	    --this.nBytesToRead;
	    break;
	  default:
	    this.nBytesToRead = 0;
	}
      }
    }
    if( this.nBytesToRead <= 0 ) {
      this.statusReg &= ~STATUS_DATA_AVAILABLE;
      this.statusReg |= STATUS_FIFO_EMPTY;
    }
    return rv;
  }


  public int readStatus()
  {
    int rv = this.statusReg;
    if( this.lineCounter < 50 ) {
      rv |= STATUS_VSYNC_ACTIVE;
    }
    if( this.lineTStateCounter < this.tStatesPerHBlank ) {
      rv |= STATUS_HBLANK_ACTIVE;
    }
    return rv | STATUS_FIFO_EMPTY;
  }


  public void setGDCListener( GDCListener listener )
  {
    this.gdcListener = listener;
  }


  public void setVRAM( VRAM vram )
  {
    this.vram = vram;
  }


  public void writeArg( int value )
  {
    this.statusReg &= ~STATUS_FIFO_EMPTY;
    if( this.argIdx < this.args.length ) {
      this.args[ this.argIdx++ ] = value;
    }
    execCmd();
  }


  public void writeCmd( int value )
  {
    this.statusReg &= ~STATUS_FIFO_EMPTY;
    this.argIdx = 0;
    this.cmd    = value & 0xFF;
    execCmd();
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.tStatesPerLine   = cpu.getMaxSpeedKHz() * 20 / 312;
    this.tStatesPerHBlank = this.tStatesPerLine / 2;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.screenEnabled && (tStates > 0) ) {
      int tStatesPerLine = this.tStatesPerLine;
      if( tStatesPerLine > 0 ) {
	this.lineTStateCounter += tStates;
	if( this.lineTStateCounter >= tStatesPerLine ) {
	  this.lineTStateCounter -= tStatesPerLine;
	  if( lineCounter < 311 ) {
	    this.lineCounter++;
	  } else {
	    this.lineCounter = 0;
	    if( this.blinkCounter > 0 ) {
	      --this.blinkCounter;
	    } else {
	      this.blinkCounter = (this.blinkRate > 0 ? this.blinkRate : 32);
	      this.cursorBlinkState = !this.cursorBlinkState;
	      if( this.cursorBlinkState ) {
		if( this.charBlinkNum < 3 ) {
		  this.charBlinkNum++;
		} else {
		  this.charBlinkNum = 0;
		}
	      }
	      GDCListener listener = this.gdcListener;
	      if( listener != null ) {
		listener.screenDirty( this );
	      }
	    }
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void execCmd()
  {
    boolean configChanged = false;

    this.statusReg &= ~STATUS_DATA_AVAILABLE;
    this.statusReg |= STATUS_FIFO_EMPTY;
    if( (this.cmd == 0)					// RESET
	|| ((this.cmd & 0xFE) == 0x0E) )		// SYNC
    {
      boolean oldScreenEnabled = this.screenEnabled;
      this.screenEnabled       = ((this.cmd & 0x01) != 0);
      if( this.screenEnabled != oldScreenEnabled ) {
	configChanged = true;
      }
      this.screenEnabled     = screenEnabled;
      this.lineCounter       = 0;
      this.lineTStateCounter = 0;
      switch( this.argIdx ) {
	case 1:
	  Mode oldMode = this.mode;
	  switch( this.args[ 0 ] & 0x22 ) {
	    case 0:
	      this.mode = Mode.MIXED;
	      break;
	    case 0x02:
	      this.mode = Mode.GRAPHICS;
	      break;
	    case 0x20:
	      this.mode = Mode.CHARACTER;
	      break;
	    default:
	      this.mode = Mode.INVALID;
	  }
	  if( this.mode != oldMode ) {
	    configChanged = true;
	  }
	  break;
	case 2:
	  int oldWordsPerCharRow = this.wordsPerCharRow;
	  this.wordsPerCharRow   = this.args[ 1 ] + 2;
	  if( this.wordsPerCharRow != oldWordsPerCharRow ) {
	    configChanged = true;
	  }
	  break;
	case 7:
	  int oldDisplayLines = this.displayLines;
	  this.displayLines   = this.args[ 6 ];
	  if( this.displayLines != oldDisplayLines ) {
	    configChanged = true;
	  }
	  break;
	case 8:
	  this.argIdx = 0;
	  break;
      }
      /*
       * Das FIFO-EMPTY-Bit wird hier immer gesetzt,
       * damit es nicht zu einer Verklemmung kommt,
       * falls ein Programm ein RESET- oder SYNC-Kommando
       * ohne die volle Argumentanzahl sendet
       * und dann auf FIFO-EMPTY wartet.
       */
      this.statusReg |= STATUS_FIFO_EMPTY;
    } else if( (this.cmd & 0xFE) == 0x0C ) {		// BCTRL
      boolean oldScreenEnabled = this.screenEnabled;
      this.screenEnabled       = ((this.cmd & 0x01) != 0);
      if( this.screenEnabled != oldScreenEnabled ) {
	configChanged = true;
      }
    } else if( (this.cmd & 0xE4) == 0x20 ) {		// WDAT
      if( this.argIdx > 0 ) {
	int vNew      = -1;
	int transType = this.cmd & 0x18;
	if( (transType == 0) && (this.argIdx == 2) ) {
	  vNew = (this.args[ 1 ] << 8) | this.args[ 0 ];
	} else if( (transType == 0x10) && (this.argIdx == 1) ) {
	  vNew = this.args[ 0 ];
	} else if( (transType == 0x18) && (this.argIdx == 1) ) {
	  vNew = this.args[ 1 ] << 8;
	}
	if( vNew >= 0 ) {
	  VRAM vram = this.vram;
	  int  mode = this.cmd & 0x07;
	  int  n    = (this.drawingDC + 1) & 0x3FFF;
	  n    = this.drawingDC + 1;
	  for( int i = 0; i < n; i++ ) {
	    if( vram != null ) {
	      if( this.drawingSL || this.drawingGC || this.drawingGD ) {

// TODO: Grafikmode
System.out.println( "GDC82720: WDAT: Grafikmode nicht implementiert" );

	      } else {
		int vOld    = this.vram.getVRAMWord( this.cursorAddr );
		int oldBits = vOld & ~this.mask;
		switch( mode ) {
		  case 0:			// replace
		    this.vram.setVRAMWord(
				this.cursorAddr,
				(vNew & this.mask) | oldBits );
		    break;
		  case 1:			// complement
		    this.vram.setVRAMWord(
				this.cursorAddr,
				(~vOld & this.mask) | oldBits );
		    break;
		  case 2:			// reset
		    this.vram.setVRAMWord(
				this.cursorAddr,
				oldBits );
		    break;
		  case 3:			// set
		    this.vram.setVRAMWord(
				this.cursorAddr,
				this.mask | oldBits );
		    break;
		}
	      }
	    }
	    // Cursor vertikal verschieben
	    switch( this.drawingDir ) {
	      case 0:
	      case 1:
	      case 7:
		// runter
		this.cursorAddr += this.memWordsPerChRow;
		break;
	      case 3:
	      case 4:
	      case 5:
		// hoch
		this.cursorAddr -= this.memWordsPerChRow;
		break;
	    }
	    // Cursor horizontal verschieben
	    switch( this.drawingDir ) {
	      case 1:
	      case 2:
	      case 3:
		// rechts
		if( transType == 0 ) {			// MSB+LSB
		  if( (this.mask & 0x8000) != 0 ) {
		    this.mask = (((this.mask << 1) | 0x0001) & 0xFFFF);
		    this.cursorAddr++;
		  } else {
		    this.mask <<= 1;
		  }
		} else if( transType == 0x10 ) {	// nur LSB
		  if( (this.mask & 0x0080) != 0 ) {
		    this.mask = (this.mask & 0xFF00)
				| (((this.mask << 1) | 0x0001) & 0x00FF);
		    this.cursorAddr++;
		  } else {
		    this.mask = (this.mask & 0xFF00)
				| ((this.mask << 1) | 0x00FF);
		  }
		} else if( transType == 0x18 ) {	// nur MSB
		  if( (this.mask & 0x8000) != 0 ) {
		    this.mask = (((this.mask << 1) | 0x0100) & 0xFF00)
				| (this.mask & 0x00FF);
		    this.cursorAddr++;
		  } else {
		    this.mask = ((this.mask << 1) | 0xFF00)
				| (this.mask & 0x00FF);
		  }
		}
		break;
	      case 5:
	      case 6:
	      case 7:
		// links
		if( transType == 0 ) {			// MSB+LSB
		  if( (this.mask & 0x0001) != 0 ) {
		    this.mask = ((this.mask >> 1) | 0x8000) & 0xFFFF;
		    --this.cursorAddr;
		  } else {
		    this.mask >>= 1;
		  }
		} else if( transType == 0x10 ) {	// nur LSB
		  if( (this.mask & 0x0001) != 0 ) {
		    this.mask = (this.mask & 0xFF00)
				| (((this.mask >> 1) | 0x0080) & 0x00FF);
		    --this.cursorAddr;
		  } else {
		    this.mask = (this.mask & 0xFF00)
				| ((this.mask >> 1) | 0x00FF);
		  }
		} else if( transType == 0x18 ) {	// nur MSB
		  if( (this.mask & 0x0100) != 0 ) {
		    this.mask = (((this.mask >> 1) | 0x8000) & 0xFF00)
				| (this.mask & 0x00FF);
		    --this.cursorAddr;
		  } else {
		    this.mask = ((this.mask >> 1) | 0xFF00)
				| (this.mask & 0x00FF);
		  }
		}
		break;
	    }
	    this.cursorAddr &= 0x3FFFF;
	  }
	  this.argIdx    = 0;
	  this.drawingDC = 0;
	  this.statusReg |= STATUS_FIFO_EMPTY;
	}
      } else {
	if( this.argIdx > 2 ) {
	  // ungueltige Werte
	  this.argIdx        = 0;
	  this.statusReg |= STATUS_FIFO_EMPTY;
	}
      }
    } else if( this.cmd == 0x46 ) {			// ZOOM
      if( this.argIdx == 1 ) {
	if( this.args[ 0 ] != 0 ) {

// TODO: Zoom
System.out.println( "GDC82720: Zoomfaktor groesser 1 nicht implementiert" );

	}
	this.argIdx           = 0;
	this.statusReg |= STATUS_FIFO_EMPTY;
      }

    } else if( this.cmd == 0x47 ) {			// PITCH
      if( this.argIdx == 1 ) {
	int oldMemWordsPerChRow = this.memWordsPerChRow;
	this.memWordsPerChRow   = this.args[ 0 ];
	if( this.memWordsPerChRow != oldMemWordsPerChRow ) {
	  configChanged = true;
	}
	this.argIdx = 0;
	this.statusReg |= STATUS_FIFO_EMPTY;
      }
    } else if( this.cmd == 0x49 ) {			// CURS
      if( this.argIdx == 2 ) {
	this.cursorAddr = ((this.args[ 1 ] << 8) & 0xFF00)
				| (this.args[ 0 ] & 0xFF);
	this.statusReg |= STATUS_FIFO_EMPTY;
      } else if( this.argIdx == 3 ) {
	this.cursorAddr = (this.cursorAddr & 0xFFFF)
				| ((this.args[ 2 ] << 16) & 0x030000);
	this.cursorDotAddr = (this.args[ 2 ] >> 4) & 0x0F;
	if( this.cursorDotAddr > 0 ) {
	  this.mask = (1 << this.cursorDotAddr);
	} else {
	  this.mask = 1;
	}
	this.argIdx = 0;
	this.statusReg |= STATUS_FIFO_EMPTY;
      }
    } else if( this.cmd == 0x4A ) {			// MASK
      if( this.argIdx == 1 ) {
	this.mask = this.args[ 0 ];
      } else if( this.argIdx == 2 ) {
	this.mask = ((this.args[ 1 ] << 8) & 0xFF00) | this.args[ 0 ];
	this.argIdx = 0;
	this.statusReg |= STATUS_FIFO_EMPTY;
      }
    } else if( this.cmd == 0x4B ) {			// CCHAR
      if( this.argIdx == 3 ) {
	int oldLinesPerCharRow  = this.linesPerCharRow;
	int oldCursorTopLine    = this.cursorTopLine;
	int oldCursorBottomLine = this.cursorBottomLine;
	this.linesPerCharRow    = (this.args[ 0 ] & 0x1F) + 1;
	this.cursorTopLine      = this.args[ 1 ] & 0x1F;
	this.cursorBottomLine   = (this.args[ 2 ] >> 3) & 0x1F;
	if( (this.linesPerCharRow != oldLinesPerCharRow)
	    || (oldCursorTopLine != this.cursorTopLine)
	    || (oldCursorBottomLine != this.cursorBottomLine) )
	{
	  configChanged = true;
	}
	this.cursorEnabled  = ((this.args[ 0 ] & 0x80) != 0);
	this.cursorBlinking = ((this.args[ 1 ] & 0x20) == 0);
	this.blinkRate      = ((this.args[ 1 ] >> 6) & 0x03)
					| ((this.args[ 2 ] << 2) & 0x1C);
	this.argIdx = 0;
	this.statusReg |= STATUS_FIFO_EMPTY;
      }
    } else if( this.cmd == 0x4C ) {			// FIGS
      switch( this.argIdx ) {
	case 1:
	  {
	    int v = this.args[ 0 ];
	    this.drawingSL = ((v & 0x80) != 0);
	    this.drawingR   = ((v & 0x40) != 0);
	    this.drawingA   = ((v & 0x20) != 0);
	    this.drawingGC  = ((v & 0x10) != 0);
	    this.drawingL   = ((v & 0x08) != 0);
	    this.drawingDir = v & 0x07;
	  }
	  break;
	case 2:
	  this.drawingDC = this.args[ 1 ];
	  break;
	case 3:
	  {
	    int v = this.args[ 2 ];
	    this.drawingDC = ((v << 8) & 0x3F00) | this.args[ 1 ];
	    this.drawingGD = ((v & 0x40) != 0);
	  }
	  break;
	case 4:
	  this.drawingD = this.args[ 3 ];
	  break;
	case 5:
	  this.drawingD = ((this.args[ 4 ] << 8) & 0x3F00) | this.args[ 3 ];
	  break;
	case 6:
	  this.drawingD2 = this.args[ 5 ];
	  break;
	case 7:
	  this.drawingD2 = ((this.args[ 6 ] << 8) & 0x3F00) | this.args[ 5 ];
	  break;
	case 8:
	  this.drawingD1 = this.args[ 7 ];
	  break;
	case 9:
	  this.drawingD1 = ((this.args[ 8 ] << 8) & 0x3F00) | this.args[ 7 ];
	  break;
	case 10:
	  this.drawingDM = this.args[ 9 ];
	case 11:
	  this.drawingDM = ((this.args[ 10 ] << 8) & 0x3F00) | this.args[ 9 ];
	  this.argIdx    = 0;
	  this.statusReg |= STATUS_FIFO_EMPTY;
	  break;
      }
    } else if( this.cmd == 0x6B ) {			// START
      if( !this.screenEnabled ) {
	configChanged = true;
      }
      this.screenEnabled = true;
      this.argIdx        = 0;
      this.statusReg |= STATUS_FIFO_EMPTY;
    } else if( (this.cmd & 0xF0) == 0x70 ) {		// PRAM
      if( this.argIdx > 0 ) {
	int srcIdx = this.argIdx - 1;
	int dstIdx = (this.cmd & 0x0F) + srcIdx;
	if( dstIdx < this.pram.length ) {
	  byte b = (byte) this.args[ srcIdx ];
	  if( b != this.pram[ dstIdx ] ) {
	    configChanged = true;
	  }
	  this.pram[ dstIdx ] = b;
	}
	this.pramLen = dstIdx + 1;
      }
      this.statusReg |= STATUS_FIFO_EMPTY;
    } else if( (this.cmd & 0xE4) == 0xA0 ) {		// RDAT
      if( ((this.cmd & 0x18) != 0x08) && (this.drawingDC > 0) ) {
	if( (this.cmd & 0x18) == 0 ) {
	  this.nBytesToRead = this.drawingDC * 2;
	} else {
	  this.nBytesToRead = this.drawingDC;
	}
	this.statusReg |= STATUS_DATA_AVAILABLE;
	this.statusReg &= ~STATUS_FIFO_EMPTY;
      }
      this.argIdx = 0;
    } else if( this.cmd == 0xE0 ) {			// CURD
      this.nBytesToRead = 5;
      this.argIdx       = 0;
      this.statusReg |= STATUS_DATA_AVAILABLE;
      this.statusReg &= ~STATUS_FIFO_EMPTY;
    } else {
      /*
       * Unbekanntes oder nicht emuliertes Kommando
       *
       * Nicht emuliert werden:
       *   VSYNC
       */
      this.argIdx = 0;
      this.statusReg |= STATUS_FIFO_EMPTY;
    }
    GDCListener listener = this.gdcListener;
    if( listener != null ) {
      if( configChanged ) {
	listener.screenConfigChanged( this );
      }
      listener.screenDirty( this );
    }
  }
}
