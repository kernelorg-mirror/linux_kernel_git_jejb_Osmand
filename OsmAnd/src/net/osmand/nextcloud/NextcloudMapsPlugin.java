package net.osmand.nextcloud;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.StateChangedListener;

import android.app.Activity;
import android.util.Base64;

/**
 *
 * Plugin that stores and retrieves Favourites from Nextcloud Maps
 *
 * @author James Bottomley
 */
public class NextcloudMapsPlugin extends OsmandPlugin
	implements FavouritesDbHelper.FavouritesPlugin,
		   StateChangedListener {

	public static final String ID = "osmand.nextcloud.maps";

	private static final org.apache.commons.logging.Log log =
		net.osmand.PlatformUtil.getLog(NextcloudMapsPlugin.class);

	// Our updateable settings
	private String baseUrl;
	private String username;
	private String password;
	private boolean debug = false;

	// Our real private data
	final private String FAVOURITES;
	private final String IDstr = "nextcloud-id";

	private OsmandApplication app;
	private OsmandSettings settings;

	private void debug(String str) {
		if (debug)
			log.info(str);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.nextcloud_maps_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.nextcloud_maps_plugin_name);
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.parking_position;
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return NextcloudMapsSettingsActivity.class;
	}

	@Override
	public boolean init(OsmandApplication app, Activity activity) {
		if (baseUrl == null)
			return false;
		return true;
	}

	public void setActive(boolean active) {
		super.setActive(active);

		if (active)
			FavouritesDbHelper.setFavouritesPlugin(this);
		else
			FavouritesDbHelper.setFavouritesPlugin(null);
	}

	private void settingsUpdate() {
		boolean prev = debug;
		baseUrl = settings.NEXTCLOUD_URL.get();
		username = settings.NEXTCLOUD_USERNAME.get();
		password = settings.NEXTCLOUD_PASSWORD.get();
		debug = settings.NEXTCLOUD_DEBUG.get();
		if (prev == false && debug == true)
			log.info("Osmand Nextcloud plugin enabling debugging");
		else if (prev == true && debug == false)
			log.info("Osmand Nextcloud plugin disabling debugging");
	}

	public NextcloudMapsPlugin(OsmandApplication a) {
		app = a;
		settings = app.getSettings();
		FAVOURITES = app.getString(R.string.shared_string_favorites);
		settingsUpdate();
		settings.NEXTCLOUD_URL.addListener(this);
		settings.NEXTCLOUD_USERNAME.addListener(this);
		settings.NEXTCLOUD_PASSWORD.addListener(this);
		settings.NEXTCLOUD_DEBUG.addListener(this);
	}

	public void stateChanged(Object change) {
		settingsUpdate();
	}

	/*
	 * Nextcloud point handling
	 */

	private String websend(String relativeUrl, String method, String send)
		throws MalformedURLException, IOException {
		URL url;
		HttpURLConnection c;

		url = new URL(baseUrl + relativeUrl);
		byte[] auth = (username+":"+password).getBytes("UTF-8");
		String encodedAuth =
			Base64.encodeToString(auth, Base64.NO_WRAP);
		c = (HttpURLConnection) url.openConnection();

		try {
			c.setRequestMethod(method);
			c.setRequestProperty("Authorization",
					     "Basic " + encodedAuth);
			c.setRequestProperty("Accept", "application/json");
			if (send == null) {
				c.setDoInput(true);
				c.connect();
			} else {
				byte[] b = send.getBytes("UTF-8");
				c.setRequestProperty("Content-Type",
						     "application/json; utf-8");
				c.setDoOutput(true);
				c.getOutputStream().write(b);
			}

			int status = c.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK &&
			    status != HttpURLConnection.HTTP_CREATED)
				throw new IOException("Connection failed, status = " + status);
			Scanner s = new Scanner(c.getInputStream());
			String response = s.useDelimiter("\\A").next();

			return response;
		} finally {
			c.disconnect();
		}
	}

	public void addFavourite(FavouritePoint p) {
		String method;
		String url = "/index.php/apps/maps/api/v1/favorites";
		String id = p.getExtension(IDstr);
		String name = p.getName();

		if (id == null) {
			method = "POST";
			debug("add Favourite: " + name);
		} else {
			url += "/" + id;
			method = "PUT";
			debug("edit Favourite[" + id + "]: " + name);
		}
		// strip id from saved extensions
		p.setExtension(IDstr, null);
		try {
			JSONObject jo = new JSONObject();
			String reply;

			jo.put("lat", p.getLatitude());
			jo.put("lng", p.getLongitude());
			jo.put("name", name);
			// blank in OsmAnd shows up as "Favourite" but will
			// show up blank in Nextcloud
			if (p.getCategory().length() == 0)
				jo.put("category", FAVOURITES);
			else
				jo.put("category", p.getCategory());
			jo.put("comment", p.getDescription());
			jo.put("extensions", p.getExtensions());

			reply = websend(url, method, jo.toString());
			jo = new JSONObject(reply);
			id = jo.getString("id");

			p.setExtension(IDstr, id);
			debug("received " + name + " [" + id + "]");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void deleteFavourite(FavouritePoint p) {
		String id = p.getExtension(IDstr);

		debug("deleteFavourite: " + p.getName() + " ["+id+"]");

		if (id == null)
			// point isn't on remote
			return;
		try {
			String reply;

			reply = websend("/index.php/apps/maps/api/v2/favorites/" + id,
					"DELETE", null);
			if (!"\"DELETED\"".equals(reply.trim())) {
				throw new Exception("Reply was: " + reply);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public boolean loadFavourites(FavouritesDbHelper h) {
		JSONArray jarr = new JSONArray();
		List<FavouritePoint> l = h.getFavouritePoints();
		String reply;

		debug("loadFavourites");

		try {
			reply = websend("/index.php/apps/maps/api/v2/favorites",
					"GET", null);
			jarr = new JSONArray(reply);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}

		Map<String, FavouritePoint> m =
			new HashMap<String, FavouritePoint>();

		for (int i = 0; i < jarr.length(); i++) {
			try {
				JSONObject jo = jarr.getJSONObject(i);
				double lat = jo.getDouble("lat");
				double lng = jo.getDouble("lng");
				String name = jo.getString("name");
				String category = jo.getString("category");
				// the Favorites category is blank on OsmAnd
				if (category.equals(FAVOURITES))
					category = "";
				String comment = jo.getString("comment");

				FavouritePoint p = new FavouritePoint(lat, lng, name, category);
				p.setDescription(comment);
				JSONObject extensions = jo.optJSONObject("extensions");
				if (extensions != null)
					p.setExtensions(extensions);

				p.setExtension(IDstr, jo.getString("id"));
				m.put(name, p);
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
		}

		// This is the heart of the machinery for merging the
		// points lists where one or the other may have been
		// offline when the update was made

		// First is the extra list: any points left in here
		// after the iteration exist remotely but not locally
		Map<String,FavouritePoint> extra = new HashMap<String,FavouritePoint>(m);
		boolean changed = false;
		for (FavouritePoint p: l) {
			String name = p.getName();
			String id = p.getExtension(IDstr);
			FavouritePoint rp = m.get(name);

			// deletion case: point doesn't exist remotely
			if (rp == null) {
				// If point has an "id" extension it
				// once existed remotely, so delete it
				// otherwise assume it was locally
				// created while the remote was offline
				if (id == null) {
					addFavourite(p);
				} else {
					debug("loadFavourites, delete: " + name + " [" + id +"]");
					changed = true;
					h.deleteFromFavourites(p);
				}
			} else {
				debug("Found Existing: " + name + " [" + id + "]");
				extra.remove(name);
				// point must have been saved to remote
				// but not updated locally
				if (p.getExtension(IDstr) == null) {
					p.setExtensions(rp.getExtensions());
					changed = true;
				}
			}
		}
		if (!extra.isEmpty()) {
			debug("Adding Extras: " + extra.keySet());
			h.addToFavourites(extra.values());
			changed = true;
		}
		debug("loadFavourites changed = " + changed);
		log.info("Favourites updated from nextcloud server");
		return changed;
	}
}
