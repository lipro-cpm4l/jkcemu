/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Tastatur D005
 *
 * Die Tastatur kennt zwei Betriebsmodi: CAOS und MicroDOS (PC-Mode)
 * Im PC-Mode senden einige Tasten einen anderen Tastencode,
 * teilweise auch Zwei-Byte-Tastencodes.
 *
 * Die Shiftebene hat nicht die gleiche Zuordnung zu den Tasten
 * wie die Originaltastatur.
 * Aus diesem Grund, aber auch weil es die neue Control-Ebene gibt,
 * muessen die Tastencodes fuer jede Ebene separat gemerkt werden.
 * Dazu wird KeyData.value folgendermassen genutzt:
 *  Bit 0-7:   Tastecode ohne Shift und ohne Control
 *  Bit 8-15:  Tastencode bei Shift
 *  Bit 16-23: Tastencode bei Control
 *  Bit 24:    0: 1-Byte-Tastencode senden
 *             1: Bei CTRL im PC-Mode zwei-Byte-Tastencode senden
 *
 * Die Tastatur kennt zwei Betriebsmodi: CAOS und MicroDOS (PC-Mode)
 * Im PC-Mode sind einige Tasten anders belegt.
 * Ausserdem gibt es auch Zwei-Byte-Tastencodes.
 * F
 */

package jkcemu.emusys.kc85;

import java.awt.*;
import java.lang.*;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.KC85;


public class D005KeyboardFld extends AbstractKC85KeyboardFld
{
  private static final int TEXT_FONT_SIZE   = 9;
  private static final int LETTER_FONT_SIZE = 14;
  private static final int DIGIT_FONT_SIZE  = 11;
  private static final int LED_SIZE         = 8;
  private static final int KEY_SIZE         = 40;
  private static final int KEY_HALF_SIZE    = KEY_SIZE / 2;
  private static final int MEDIUM_KEY_SIZE  = KEY_SIZE / 4 * 5;
  private static final int LARGE_KEY_SIZE   = KEY_SIZE / 2 * 3;
  private static final int SPACE_KEY_SIZE   = KEY_SIZE * 8;
  private static final int MARGIN           = 20;
  private static final int PRE_F1_MASK      = (124 << 8) & 0xFF00;

  private Image   imgKey40x40;
  private Image   imgKey50x40;
  private Image   imgKey60x40;
  private Image   imgKey320x40;
  private Image   imgLeft;
  private Image   imgRight;
  private Image   imgUp;
  private Image   imgDown;
  private Image   imgPoint;
  private Font    fontText;
  private Font    fontLetter;
  private Font    fontDigit;
  private KeyData digit1Key;
  private KeyData digit2Key;
  private KeyData deleteKey;
  private KeyData escapeKey;
  private KeyData controlKey;
  private KeyData spaceKey;
  private KeyData shiftKey1;
  private KeyData shiftKey2;
  private KeyData capsLockKey;
  private KeyData f1Key;
  private KeyData f4Key;
  private KeyData f5Key;
  private KeyData aeKey;
  private KeyData oeKey;
  private KeyData ueKey;
  private KeyData szKey;
  private int[]   kbMatrix;
  private int     curIdx;
  private int     curX;
  private int     curY;
  private int     xRow1Left;
  private int     xRow1Right;
  private int     xRow3Right;
  private boolean capsLockMode;
  private boolean pcMode;
  private Color   colorLEDRedOn;
  private Color   colorLEDRedOff;


  public D005KeyboardFld( KC85 kc85 )
  {
    super( kc85, 69 );
    this.imgKey40x40  = Main.getImage( "/images/keyboard/key40x40.png" );
    this.imgKey50x40  = Main.getImage( "/images/keyboard/key50x40.png" );
    this.imgKey60x40  = Main.getImage( "/images/keyboard/key60x40.png" );
    this.imgKey320x40 = Main.getImage( "/images/keyboard/key320x40.png" );
    this.imgLeft      = Main.getImage( "/images/keyboard/left.png" );
    this.imgRight     = Main.getImage( "/images/keyboard/right.png" );
    this.imgUp        = Main.getImage( "/images/keyboard/up.png" );
    this.imgDown      = Main.getImage( "/images/keyboard/down.png" );
    this.imgPoint     = Main.getImage( "/images/keyboard/point.png" );

    this.colorLEDRedOn  = Color.red;
    this.colorLEDRedOff = new Color( 120, 60, 60 );

    this.fontText     = new Font( "SansSerif", Font.PLAIN, TEXT_FONT_SIZE );
    this.fontLetter   = new Font( "SansSerif", Font.PLAIN, LETTER_FONT_SIZE );
    this.fontDigit    = new Font( "SansSerif", Font.PLAIN, DIGIT_FONT_SIZE );
    this.kbMatrix     = new int[ 10 ];
    this.capsLockMode = false;
    this.pcMode       = false;
    this.curIdx       = 0;
    this.curX         = MARGIN;
    this.curY         = MARGIN;
    this.f1Key        = addKey( "F1", "F7", null, 124, 125, 124 );
    this.curX += (KEY_SIZE / 2);
    this.xRow1Left = this.curX;
    addKey( "/", "\'", "\u00AC", 106, 53, 23 );
    this.digit1Key = addKey( "1", "!", 116, 117 );
    this.digit2Key = addKey( "2", "\"", 4, 5 );
    addKey( "3", "#", 20, 21 );
    addKey( "4", "$", 100, 101 );
    addKey( "5", "%", 36, 37 );
    addKey( "6", "&", 84, 85 );
    addKey( "7", "^", "@", 52, 22, 43 );
    addKey( "8", "(", "|", 68, 69, 103 );
    addKey( "9", ")", 58, 59 );
    addKey( "0", "=", 42, 11 );
    this.szKey = addKey( "\u00DF", "?", "F4", 108, 107, -1 );
    addKey( "HOME", 8, 9 );
    addKey( "INS", null, null, 56, 57, 118 );
    this.xRow1Right = this.curX - 1;
    this.curX += (KEY_SIZE * 3 / 4);
    int xCrsMiddle = this.curX + (LARGE_KEY_SIZE / 2);
    this.deleteKey = addLargeKey( "DEL", 40, 41 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F2", "F8", 12, 13 );
    this.curX += KEY_SIZE;
    addKey( "<", ">", 75, 91 );
    addControlSensitiveKey( "Q", 112, 113 );
    addControlSensitiveKey( "W", 0, 1 );
    addControlSensitiveKey( "E", 16, 17 );
    addControlSensitiveKey( "R", null, 96, 97, 87 );	// CTRL: RUN
    addControlSensitiveKey( "T", 32, 33 );
    addControlSensitiveKey( "Z", 80, 81 );
    addControlSensitiveKey( "U", 48, 49 );
    addControlSensitiveKey( "I", 64, 65 );
    addControlSensitiveKey( "O", 54, 55 );
    addControlSensitiveKey( "P", 38, 39 );
    this.ueKey = addKey( "\u00DC", null, "F3", 28, 29, -1 );
    addKey( "+", "*", 104, 27 );
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgUp, 120, 121 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F3", "F9", 28, 29 );
    this.curX += KEY_SIZE;
    this.capsLockKey = new KeyData(
				this.curX,
				this.curY,
				MEDIUM_KEY_SIZE,
				KEY_SIZE,
				"CAPS\nLOCK",
				null,
				null,
				null,
				null,
				-1,
				114,
				false );
    this.keys[ this.curIdx++ ] = this.capsLockKey;
    this.curX += MEDIUM_KEY_SIZE;
    addControlSensitiveKey( "A", 2, 3 );
    addControlSensitiveKey( "S", 18, 19 );
    addControlSensitiveKey( "D", 98, 99 );
    addControlSensitiveKey( "F", 34, 35 );
    addControlSensitiveKey( "G", 82, 83 );
    addControlSensitiveKey( "H", 50, 51 );
    addControlSensitiveKey( "J", 66, 67 );
    addControlSensitiveKey( "K", 72, 73 );
    addControlSensitiveKey( "L", null, 88, 89, 86 );	// CTRL: LIST
    this.oeKey = addKey( "\u00D6", null, "F2", 12, 13, -1 );
    this.aeKey = addKey( "\u00C4", null, "F1", 124, 125, -1 );
    addControlSensitiveKey( "CLR", 24, 25 );
    this.xRow3Right = this.curX - 1;
    this.curX = xCrsMiddle - KEY_SIZE;
    addKey( this.imgLeft, 6, 7 );
    addKey( this.imgRight, 122, 123 );

    int w = this.curX + MARGIN;

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    this.f4Key = addKey( "F4", "F10", 108, 109 );
    this.curX += (KEY_SIZE / 2);
    addKey( "F6", "F12", 92, 93 );
    this.shiftKey1 = new KeyData(
				this.curX,
				this.curY,
				MEDIUM_KEY_SIZE,
				KEY_SIZE,
				"SHIFT",
				null,
				null,
				null,
				null,
				-1,
				-1,
				true );
    this.keys[ this.curIdx++ ] = this.shiftKey1;
    this.curX += MEDIUM_KEY_SIZE;
    addControlSensitiveKey( "Y", 14, 15 );
    addControlSensitiveKey( "X", 30, 31 );
    addControlSensitiveKey( "C", 110, 111 );
    addControlSensitiveKey( "V", 46, 47 );
    addControlSensitiveKey( "B", 94, 95 );
    addControlSensitiveKey( "N", 62, 63 );
    addControlSensitiveKey( "M", 78, 79 );
    addKey( ",", ";", 74, 105 );
    addKey( ".", ":", 90, 26 );
    addKey( "-", "_", 10, 102 );
    this.shiftKey2 = new KeyData(
				this.curX,
				this.curY,
				LARGE_KEY_SIZE,
				KEY_SIZE,
				"SHIFT",
				null,
				null,
				null,
				null,
				-1,
				-1,
				true );
    this.keys[ this.curIdx++ ] = this.shiftKey2;
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgDown, 118, 119 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    this.f5Key = addKey( "F5", "F11", 44, 45 );
    this.curX += ((KEY_SIZE / 2) + (2 * KEY_SIZE) - MEDIUM_KEY_SIZE);
    this.controlKey = new KeyData(
				this.curX,
				this.curY,
				KEY_SIZE,
				KEY_SIZE,
				"CTRL",
				null,
				null,
				null,
				null,
				-1,
				-1,
				true );
    this.keys[ this.curIdx++ ] = this.controlKey;
    this.curX += KEY_SIZE;
    this.escapeKey = addKey( "ESC", 77, 77 );
    this.spaceKey  = new KeyData(
			this.curX,
			this.curY,
			8 * KEY_SIZE,
			KEY_SIZE,
			null,
			null,
			null,
			null,
			null,
			-1,
			0x00FF0000
				| ((71 << 8) & 0x0000FF00)
				| (70 & 0x000000FF),
			false );
    this.keys[ this.curIdx++ ] = this.spaceKey;
    this.curX += (8 * KEY_SIZE);
    addControlSensitiveKey( "BRK", 60, 61 );
    addControlSensitiveKey( "STOP", 76, 77 );
    this.curX = xCrsMiddle - (LARGE_KEY_SIZE / 2);
    addLargeKey( "RETURN", 126, 127 );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize( new Dimension( w, h ) );
    setShiftKeys( this.shiftKey1, this.shiftKey2 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys instanceof KC85 ) {
      if( ((KC85) emuSys).getKCTypeNum() >= 4 ) {
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void fireKOut()
  {
    this.capsLockMode = false;
    this.pcMode       = false;
    fireRepaint();
  }


  @Override
  public String getKeyboardName()
  {
    return "KC85 Komforttastatur D005";
  }


  @Override
  protected void keySelectionChanged()
  {
    int     keyNum = -1;
    boolean ctrl   = false;
    boolean shift  = false;
    KeyData key    = null;
    synchronized( this.selectedKeys ) {
      for( KeyData tmpKey : this.selectedKeys ) {
	if( tmpKey == this.controlKey ) {
	  ctrl = true;
	} else if( (tmpKey == this.shiftKey1)
		   || (tmpKey == this.shiftKey2) )
	{
	  shift = true;
	} else {
	  if( key != null ) {
	    key = null;
	    break;
	  }
	  key = tmpKey;
	}
      }
    }
    if( key != null ) {
      if( ctrl ) {
	if( key == this.escapeKey ) {
	  this.pcMode = !this.pcMode;
	  repaint();
	} else {
	  if( this.pcMode && (key == this.digit1Key) ) {
	    keyNum = PRE_F1_MASK | 52;				// F1 7
	  } else if( this.pcMode && (key == this.digit2Key) ) {
	    keyNum = PRE_F1_MASK | 68;				// F1 8
	  } else if( this.pcMode && (key == this.deleteKey) ) {
	    keyNum = PRE_F1_MASK | 40;				// F1 DEL 
	  } else if( this.pcMode && (key == this.f4Key) ) {
	    keyNum = PRE_F1_MASK | 108;				// F1 F4 
	  } else if( this.pcMode && (key == this.f5Key) ) {
	    keyNum = PRE_F1_MASK | 44;				// F1 F5 
	  } else if( this.pcMode && (key == this.spaceKey) ) {
	    keyNum = PRE_F1_MASK | 70;				// F1 SPACE 
	  } else if( this.pcMode && ((key.value & 0x01000000) != 0) ) {
	    keyNum = PRE_F1_MASK | (key.value & 0x00FF);
	  } else {
	    keyNum = (key.value >> 16) & 0xFF;
	  }
	}
      } else if( key == this.capsLockKey ) {
	if( this.pcMode ) {
	  keyNum = PRE_F1_MASK | 56;				// F1 1Ah
	} else {
	  keyNum = key.value & 0xFF;
	}
	this.capsLockMode = !this.capsLockMode;
      } else {
	boolean sh2 = shift;		// wirkt nur auf die 3 Umlaute-Tasten
	if( this.capsLockMode ) {
	  sh2 = !sh2;
	}
	if( key == this.aeKey ) {
	  if( this.pcMode ) {
	    keyNum = PRE_F1_MASK | (sh2 ? 116 : 100);		// F1 31h/34h
	  } else {
	    keyNum = (sh2 ? (key.value >> 8) : key.value) & 0xFF;
	  }
	} else if( key == this.oeKey ) {
	  if( this.pcMode ) {
	    keyNum = PRE_F1_MASK | (sh2 ? 4 : 36);		// F1 32h/35h
	  } else {
	    keyNum = (sh2 ? (key.value >> 8) : key.value) & 0xFF;
	  }
	} else if( key == this.ueKey ) {
	  if( this.pcMode ) {
	    keyNum = PRE_F1_MASK | (sh2 ? 20 : 84);		// F1 33h/36h
	  } else {
	    keyNum = (sh2 ? (key.value >> 8) : key.value) & 0xFF;
	  }
	} else if( this.pcMode && (key == this.f1Key) ) {
	  keyNum = PRE_F1_MASK | (shift ? 28 : 12);		// F1 F3/F2
	} else if( !shift && this.pcMode && (key == this.szKey) ) {
	  keyNum = PRE_F1_MASK | 42;				// F1 30h
	} else if( this.pcMode && (key == this.escapeKey) ) {
	  keyNum = 125;						// F7
	} else {
	  keyNum = (shift ? (key.value >> 8) : key.value) & 0xFF;
	}
      }
      if( (keyNum & 0xFF) == 0xFF ) {
	keyNum = -1;
      }
    }
    this.kc85.setKeyNumPressed( keyNum );
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setPaintMode();
    g.setColor( Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      if( isKeySelected( key ) ) {
	g.setColor( Color.gray );
	g.fillRect( key.x, key.y, key.w, key.h );
      }
      switch( key.w ) {
	case KEY_SIZE:
	  if( this.imgKey40x40 != null ) {
	    g.drawImage( this.imgKey40x40, key.x, key.y, this );
	  }
	  break;
	case MEDIUM_KEY_SIZE:
	  if( this.imgKey50x40 != null ) {
	    g.drawImage( this.imgKey50x40, key.x, key.y, this );
	  }
	  break;
	case LARGE_KEY_SIZE:
	  if( this.imgKey60x40 != null ) {
	    g.drawImage( this.imgKey60x40, key.x, key.y, this );
	  }
	  break;
	case SPACE_KEY_SIZE:
	  if( this.imgKey320x40 != null ) {
	    g.drawImage( this.imgKey320x40, key.x, key.y, this );
	  }
	  break;
      }
      if( key.image != null ) {
	g.drawImage( key.image, key.x + 8, key.y + 10, this );
      } else {
	g.setColor( Color.black );
	if( key.text1 != null ) {
	  if( key.text2 != null ) {
	    g.setFont( this.fontDigit );
	    g.drawString(
			key.text2,
			key.x + 8,
			key.y + 4 + DIGIT_FONT_SIZE );
	    g.drawString(
			key.text1,
			key.x + 8,
			key.y + KEY_SIZE - 9 );
	  } else {
	    int len = key.text1.length();
	    if( len == 1 ) {
	      g.setFont( this.fontLetter );
	      g.drawString(
			key.text1,
			key.x + 8,
			key.y + 6 + LETTER_FONT_SIZE );
	    } else {
	      g.setFont( this.fontText );
	      int eol = key.text1.indexOf( '\n' );
	      if( eol >= 0 ) {
		g.drawString(
			key.text1.substring( 0, eol ),
			key.x + 5,
			key.y + 6 + TEXT_FONT_SIZE );
		if( (eol + 1) < len ) {
		  g.drawString(
			key.text1.substring( eol + 1 ),
			key.x + 5,
			key.y + 8 + (2 * TEXT_FONT_SIZE) );
		}
	      } else {
		g.drawString(
			key.text1,
			key.x + 5,
			key.y + 6 + TEXT_FONT_SIZE );
	      }
	    }
	  }
	  if( key.text3 != null ) {
	    int x = key.x + 22;
	    int y = key.y + KEY_SIZE - 9;
	    if( key.text3.length() == 1 ) {
	      y = key.y + 4 + DIGIT_FONT_SIZE;
	    }
	    g.setFont( this.fontDigit );
	    FontMetrics fm = g.getFontMetrics();
	    if( fm != null ) {
	      x = key.x + key.w - 7 - fm.stringWidth( key.text3 );
	    }
	    g.drawString( key.text3, x, y );
	  }
	}
      }
    }

    // linker LED-Block (nur Attrappe)
    g.setColor( Color.gray );
    g.drawLine(
        this.xRow1Left,
        MARGIN + KEY_SIZE,
        this.xRow1Left,
        MARGIN + (3 * KEY_SIZE ) );
    int x = this.xRow1Left + (KEY_HALF_SIZE - LED_SIZE) / 2;
    int y = MARGIN + KEY_SIZE + ((KEY_SIZE - LED_SIZE) / 2);
    g.setColor( Color.gray );
    g.fillOval( x, y, LED_SIZE, LED_SIZE );
    g.fillOval( x, y + KEY_SIZE, LED_SIZE, LED_SIZE );

    // rechter LED-Block (nur Attrappe)
    y = MARGIN + (2 * KEY_SIZE );
    g.setColor( Color.gray );
    g.drawLine( this.xRow1Right, MARGIN + KEY_SIZE, this.xRow1Right, y );
    g.drawLine( this.xRow1Right, y, this.xRow3Right, y );
    g.setColor( this.pcMode ? this.colorLEDRedOn : this.colorLEDRedOff );
    x = this.xRow1Right - KEY_HALF_SIZE + ((KEY_HALF_SIZE - LED_SIZE) / 2);
    y = MARGIN + KEY_SIZE + ((KEY_SIZE - LED_SIZE) / 2);
    g.fillOval( x, y, LED_SIZE, LED_SIZE );
  }


  public void reset()
  {
    this.capsLockMode = false;
    this.pcMode       = false;
    super.reset();
  }


  @Override
  public void setEmuSys( EmuSys emuSys ) throws IllegalArgumentException
  {
    if( accepts( emuSys ) ) {
      this.kc85 = (KC85) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != KC85/4..5" );
    }
  }


  @Override
  public void updKeySelection( int keyNum )
  {
    boolean dirty = false;
    synchronized( this.selectedKeys ) {
      dirty = !this.selectedKeys.isEmpty();
      this.selectedKeys.clear();
      if( keyNum >= 0 ) {
        for( KeyData key : this.keys ) {
	  int normalValue  = key.value & 0xFF;
	  int shiftValue   = (key.value >> 8) & 0xFF;
	  int controlValue = (key.value >> 16) & 0xFF;
          if( (keyNum == normalValue) && (normalValue != 0xFF) ) {
            this.selectedKeys.add( key );
            dirty = true;
          } else if( (keyNum == shiftValue) && (shiftValue != 0xFF) ) {
            this.selectedKeys.add( this.shiftKey1 );
            this.selectedKeys.add( this.shiftKey2 );
            this.selectedKeys.add( key );
            dirty = true;
          }
          else if( (keyNum == controlValue) && (controlValue != 0xFF) ) {
            this.selectedKeys.add( this.controlKey );
            this.selectedKeys.add( key );
            dirty = true;
          }
        }
      }
    }
    if( dirty ) {
      repaint();
    }
  }


	/* --- private Methoden --- */

  /*
   * Diese addControlSensitiveKey-Methoden fuegen eine Taste hinzu
   * und markiert sie so, dass sie im PC-Mode auch zusammen
   * mit der Control-Taste zusammen gedrueckt werden kann.
   */
  private void addControlSensitiveKey(
			String textNormal,
			int    keyNumNormal,
			int    keyNumShift )
  {
    addControlSensitiveKey( textNormal, null, keyNumNormal, keyNumShift, -1 );
  }


  private void addControlSensitiveKey(
			String textNormal,
			String textShift,
			int    keyNumNormal,
			int    keyNumShift )
  {
    addControlSensitiveKey(
			textNormal,
			textShift,
			keyNumNormal,
			keyNumShift,
			-1 );
  }


  private void addControlSensitiveKey(
			String textNormal,
			String textShift,
			int    keyNumNormal,
			int    keyNumShift,
			int    keyNumControl )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
			this.curX,
			this.curY,
			KEY_SIZE,
			KEY_SIZE,
			textNormal,
			textShift,
			null,
			null,
			null,
			-1,
			0x01000000
				| ((keyNumControl << 16) & 0x00FF0000)
				| ((keyNumShift << 8) & 0x0000FF00)
				| (keyNumNormal & 0x000000FF),
			false );
    this.curX += KEY_SIZE;
  }


  private KeyData addKey(
			Image image,
			int   keyNumNormal,
			int   keyNumShift )
  {
    KeyData keyData = new KeyData(
			this.curX,
			this.curY,
			KEY_SIZE,
			KEY_SIZE,
			null,
			null,
			null,
			null,
			image,
			-1,
			0x00FF0000
				| ((keyNumShift << 8) & 0x0000FF00)
				| (keyNumNormal & 0x000000FF),
			false );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_SIZE;
    return keyData;
  }


  private KeyData addKey(
		String textNormal,
		String textShift,
		String textControl,
		int    keyNumNormal,
		int    keyNumShift,
		int    keyNumControl )
  {
    KeyData keyData = new KeyData(
			this.curX,
			this.curY,
			KEY_SIZE,
			KEY_SIZE,
			textNormal,
			textShift,
			textControl,
			null,
			null,
			-1,
			((keyNumControl << 16) & 0xFF0000)
				| ((keyNumShift << 8) & 0x00FF00)
				| (keyNumNormal & 0x0000FF),
			false );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_SIZE;
    return keyData;
  }


  private KeyData addKey(
		String textNormal,
		String textShift,
		int    keyNumNormal,
		int    keyNumShift )
  {
    return addKey(
		textNormal,
		textShift,
		null,
		keyNumNormal,
		keyNumShift,
		0xFF );
  }


  private KeyData addKey(
		String textNormal,
		int    keyNumNormal,
		int    keyNumShift )
  {
    return addKey(
		textNormal,
		null,
		null,
		keyNumNormal,
		keyNumShift,
		0xFF );
  }


  private KeyData addLargeKey(
			String textNormal,
			int    keyNumNormal,
			int    keyNumShift )
  {
    KeyData keyData = new KeyData(
			this.curX,
			this.curY,
			LARGE_KEY_SIZE,
			KEY_SIZE,
			textNormal,
			null,
			null,
			null,
			null,
			-1,
			0x00FF0000
				| ((keyNumShift << 8) & 0x0000FF00)
				| (keyNumNormal & 0x000000FF),
			false );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += LARGE_KEY_SIZE;
    return keyData;
  }
}
