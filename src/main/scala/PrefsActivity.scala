package org.positronicnet.demo.shopping

class PrefsActivity
  extends android.preference.PreferenceActivity
  with org.positronicnet.ui.PositronicActivityHelpers
{
  onCreate{ addPreferencesFromResource( R.xml.app_prefs ) }
}
