/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstracte Klasse fuerr alle KC85-Tastaturen
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.emusys.KC85;


public abstract class AbstractKC85KeyboardFld
				extends AbstractKeyboardFld<KC85>
{
  public AbstractKC85KeyboardFld( KC85 kc85, int numKeys )
  {
    super( kc85, numKeys, true );
  }


  public void fireKOut()
  {
    // leer
  }


  public abstract void updKeySelection( int keyNum );
}
