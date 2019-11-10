package net.osmand.data;

import java.io.Serializable;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;

import net.osmand.util.Algorithms;

public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;
	private String name = "";
	private String description;
	private String category = "";
	private double latitude;
	private double longitude;
	private JSONObject extensions;

	public FavouritePoint(){
		this.extensions = new JSONObject();
	}

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		if(name == null) {
			name = "";
		}
		this.name = name;
		this.extensions = new JSONObject();
	}
	
	public int getColor() {
		try {
			String c = extensions.getString("color");
			return Color.parseColor(c.toUpperCase());
		} catch (Exception e) {
			return 0;
		}
	}
	
	public PointDescription getPointDescription() {
		return new PointDescription(PointDescription.POINT_TYPE_FAVORITE, name);
	}
	
	@Override
	public PointDescription getPointDescription(Context ctx) {
		return getPointDescription();
	}
	
	public void setColor(int color) {
		try {
			extensions.put("color", Algorithms.colorToString(color));
		} catch (Exception e) {
		}
	}
	
	public boolean isVisible() {
		try {
			extensions.getString("HIDDEN");
			return false;
		} catch (Exception e) {
			return true;
		}
	}
	
	public void setVisible(boolean visible) {
		try {
			if (visible)
				extensions.put("HIDDEN", null);
			else
				extensions.put("HIDDEN", "true");
		} catch (Exception e) {
		}
	}
	

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	public String getName(Context ctx) {
		return name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription () {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getExtension(String ext) {
		try {
			return this.extensions.getString(ext);
		} catch (Exception e) {
			return null;
		}
	}

	public void setExtension(String ext, String value) {
		try {
			this.extensions.put(ext, value);
		} catch (Exception e) {
		}
	}

	public void setExtensions(Map e) {
		this.extensions = new JSONObject(e);
	}

	public void setExtensions(JSONObject jo) {
		this.extensions = jo;
	}

	public JSONObject getExtensions() {
		return this.extensions;
	}

	@Override
	public String toString() {
		return "Favourite " + getName(); //$NON-NLS-1$
	}

}
