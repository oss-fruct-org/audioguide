package org.fruct.oss.audioguide.track;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
	private String name;
	private String description;

	private String audioUrl;
	private int latE6;
	private int lonE6;

	private transient Location cachedLocation;

	public Point(String name, String description, String audioUrl, int latE6, int lonE6) {
		this.name = name;
		this.description = description;
		this.audioUrl = audioUrl;
		this.latE6 = latE6;
		this.lonE6 = lonE6;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getAudioUrl() {
		return audioUrl;
	}

	public int getLatE6() {
		cachedLocation = null;
		return latE6;
	}

	public int getLonE6() {
		cachedLocation = null;
		return lonE6;
	}

	public Location toLocation() {
		if (cachedLocation != null)
			return cachedLocation;

		Location loc = new Location("empty-provider");

		loc.setLatitude(latE6 / 1e6);
		loc.setLongitude(lonE6 / 1e6);

		return cachedLocation = loc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (name != null ? !name.equals(point.name) : point.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(name);
		parcel.writeString(description);
		parcel.writeString(audioUrl);

		parcel.writeInt(latE6);
		parcel.writeInt(lonE6);
	}

	public static final Creator<Point> CREATOR = new Creator<Point>() {
		@Override
		public Point createFromParcel(Parcel parcel) {
			String name = parcel.readString();
			String desc = parcel.readString();
			String audioUrl = parcel.readString();
			int latE6 = parcel.readInt();
			int lonE6 = parcel.readInt();

			return new Point(name, desc, audioUrl, latE6, lonE6);
		}

		@Override
		public Point[] newArray(int i) {
			return new Point[i];
		}
	};
}
