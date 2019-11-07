package net.osmand.nextcloud;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.text.InputType;

public class NextcloudMapsSettingsActivity extends SettingsBaseActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.nextcloud_settings);

		PreferenceScreen ps = getPreferenceScreen();
		EditTextPreference pref;
		String str = settings.NEXTCLOUD_URL.get();

		pref = createEditTextPreference(settings.NEXTCLOUD_URL,
						R.string.nextcloud_url,
						R.string.nextcloud_url_desc);
		if (!str.isEmpty())
			pref.setSummary(str);
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference p,  Object o) {
					String str = (String)o;
					if (!str.isEmpty())
						p.setSummary(str);
					else
						p.setSummary(R.string.nextcloud_url_desc);
					return NextcloudMapsSettingsActivity.this.onPreferenceChange(p, o);
				}
			});
		ps.addPreference(pref);

		pref = createEditTextPreference(settings.NEXTCLOUD_USERNAME,
						R.string.nextcloud_username,
						R.string.nextcloud_username_desc);
		str = settings.NEXTCLOUD_USERNAME.get();
		if (!str.isEmpty())
			pref.setSummary(str);
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference p,  Object o) {
					String str = (String)o;
					if (!str.isEmpty())
						p.setSummary(str);
					else
						p.setSummary(R.string.nextcloud_username_desc);
					return NextcloudMapsSettingsActivity.this.onPreferenceChange(p, o);
				}
			});
		ps.addPreference(pref);

		pref = createEditTextPreference(settings.NEXTCLOUD_PASSWORD,
						R.string.nextcloud_password,
						R.string.nextcloud_password_desc);
		pref.getEditText().setInputType(InputType.TYPE_CLASS_TEXT |
						InputType.TYPE_TEXT_VARIATION_PASSWORD);
		ps.addPreference(pref);

		CheckBoxPreference dbg =
			createCheckBoxPreference(settings.NEXTCLOUD_DEBUG,
						 R.string.nextcloud_debug,
						 R.string.nextcloud_debug_desc);
		ps.addPreference(dbg);
	}
}
