package org.positronicnet.demo.shopping

import com.google.android.maps.MapActivity
import org.positronicnet.ui.PositronicActivityHelpers
import android.os.Bundle

class TodoMapActivity
  extends MapActivity
  with PositronicActivityHelpers
{
  onCreate { setContentView( R.layout.map ) }

  def isRouteDisplayed = false
}
