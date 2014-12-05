package org.fruct.oss.audioguide.track;

import android.database.Cursor;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.fruct.oss.audioguide.config.Config;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;

public class Point implements Parcelable, Comparable<Point> {
	public static class CursorFields {
		public int name;
		public int desc;
		public int _id;
		public int latitude;
		public int longitude;
		public int audioUrl;
		public int photoUrl;
		public int isPrivate;
		public int uuid;
	}

	private String name;

	private String description;
	private int latE6;
	private int lonE6;

	private String audioUrl;
	private String photoUrl;
	private List<String> photoUrlList = new ArrayList<String>();

	private String time;
	private boolean isPrivate;
	private long categoryId = -1;
	private String uuid;

	// Index in track
	private long idx = -1;

	public void setCoordinates(int latE6, int lonE6) {
		this.latE6 = latE6;
		this.lonE6 = lonE6;
		cachedLocation = null;
	}

	public void setCoordinates(String coordinates) {
		StringTokenizer tok = new StringTokenizer(coordinates, ",", false);

		double longitude = Double.parseDouble(tok.nextToken());
		double latitude = Double.parseDouble(tok.nextToken());
		latE6 = (int) (latitude * 1e6);
		lonE6 = (int) (longitude * 1e6);
		cachedLocation = null;
	}

	public String getCoordinates() {
		return (lonE6 / 1e6) + ", " + (latE6 / 1e6) + ", 0.0";
	}

	private transient Location cachedLocation;

	public Point() {
	}

	public Point(Cursor cursor) {
		this(cursor.getString(0), cursor.getString(1), Utils.getString(cursor, 2),
				Utils.getString(cursor, 3), cursor.getInt(4), cursor.getInt(5));
		setPrivate(cursor.getInt(6) != 0);

		if (!cursor.isNull(7)) {
			setCategoryId(cursor.getLong(7));
		}

		setTime(cursor.getString(8));
		uuid = cursor.getString(9);
	}

	public Point(Point point) {
		name = point.name;
		description = point.description;
		latE6 = point.latE6;
		lonE6 = point.lonE6;
		audioUrl = point.audioUrl;
		photoUrl = point.photoUrl;
		categoryId = point.categoryId;
		isPrivate = point.isPrivate;
		time = point.time;
		uuid = point.uuid;
		photoUrlList = new ArrayList<String>(point.photoUrlList);
	}

	public Point(String name, String description, String audioUrl, String photoUrl, int latE6, int lonE6) {
		this();

		this.name = name;
		this.description = description;
		this.audioUrl = audioUrl;
		this.latE6 = latE6;
		this.lonE6 = lonE6;
		this.photoUrl = photoUrl;
	}

	public Point(String name, String description, String audioUrl, int latE6, int lonE6) {
		this(name, description, audioUrl, "", latE6, lonE6);
	}

	public Point(String name, String description, String audioUrl, double lat, double lon) {
		this(name, description, audioUrl, "", lat, lon);
	}

	public Point(String name, String description, String audioUrl, String photoUrl, double lat, double lon) {
		this(name, description, audioUrl, photoUrl, (int)(lat * 1e6), (int)(lon * 1e6));
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

	public String getPhotoUrl() {
		return photoUrl;
	}

	public long getCategoryId() {
		return categoryId;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public String getTime() {
		return time;
	}

	public String getUuid() {
		return uuid;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Deprecated
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public void setAudioUrl(String audioUrl) {
		this.audioUrl = audioUrl;
	}

	public void setCategoryId(long categoryId) {
		this.categoryId = categoryId;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public void setTime(String timeStr) {
		this.time = timeStr;
	}

	public long getIdx() {
		return idx;
	}

	public void setIdx(long idx) {
		this.idx = idx;
	}

	public void addPhotoUrl(String photoUrl) {
		if (this.photoUrl == null) {
			this.photoUrl = photoUrl;
		}

		photoUrlList.add(photoUrl);
	}

	public void createTime() {
		this.time = new SimpleDateFormat("dd MM yyyy HH:mm:ss.SSS", Locale.ROOT).format(new Date());
	}

	public void createUuid() {
		uuid = UUID.randomUUID().toString();
	}

	public int getLatE6() {
		cachedLocation = null; // TODO: ???
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

	public boolean hasAudio() {
		return audioUrl != null && !audioUrl.isEmpty();
	}

	public boolean hasPhoto() {
		return photoUrl != null && !photoUrl.isEmpty();
	}

	public boolean isEditable() {
		return isPrivate && uuid != null && !Config.isEditLocked();
	}

	// Generated by IDEA

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || ((Object) this).getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (latE6 != point.latE6) return false;
		if (lonE6 != point.lonE6) return false;
		if (description != null ? !description.equals(point.description) : point.description != null)
			return false;
		if (name != null ? !name.equals(point.name) : point.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + latE6;
		result = 31 * result + lonE6;
		return result;
	}


	// End generated by IDEA

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

		parcel.writeString(photoUrl);
		parcel.writeLong(categoryId);

		parcel.writeInt(isPrivate ? 1 : 0);
		parcel.writeString(time);
		parcel.writeString(uuid);

		parcel.writeStringList(photoUrlList);
	}

	public static final Creator<Point> CREATOR = new Creator<Point>() {
		@Override
		public Point createFromParcel(Parcel parcel) {
			String name = parcel.readString();
			String desc = parcel.readString();
			String audioUrl = parcel.readString();
			int latE6 = parcel.readInt();
			int lonE6 = parcel.readInt();

			String photoUrl = parcel.readString();

			Point point = new Point(name, desc, audioUrl, photoUrl, latE6, lonE6);
			point.setCategoryId(parcel.readLong());
			point.setPrivate(parcel.readInt() != 0);
			point.setTime(parcel.readString());
			point.uuid = parcel.readString();
			parcel.readStringList(point.photoUrlList);

			return point;
		}

		@Override
		public Point[] newArray(int i) {
			return new Point[i];
		}
	};

	public static Point parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "Placemark");
		Point point = new Point();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("name")) {
				point.name = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "name");
			} else if (tagName.equals("description")) {
				point.description = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "description");
			} else if (tagName.equals("Point")) {
				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "coordinates");

				point.setCoordinates(GetsResponse.readText(parser));

				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "Point");
			} else if (tagName.equals("ExtendedData")) {
				readExtendedData(parser, point);
				parser.require(XmlPullParser.END_TAG, null, "ExtendedData");
			} else {
				Utils.skip(parser);
			}
		}

		return point;
	}

	private static void readExtendedData(XmlPullParser parser, Point point) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "ExtendedData");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("gets:photo")) {
				String value = GetsResponse.readText(parser);
				if (point.photoUrl == null) {
					point.photoUrl = value;
				}
				point.addPhotoUrl(value);
			} else if (tagName.equals("Data")) {
				String key = parser.getAttributeValue(null, "name");
				if (key == null)
					throw new XmlPullParserException("Data tag have to have attribute 'name'");

				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "value");
				String value = GetsResponse.readText(parser);

				if (key.equals("uuid")) {
					point.uuid = value;
				} else if (key.equals("time")) {
					point.time = value;
				} else if (key.equals("photo")) {
					if (point.photoUrl == null) {
						point.photoUrl = value;
					}
					point.addPhotoUrl(value);
				} else if (key.equals("audio")) {
					point.audioUrl = value;
				} else if (key.equals("access")) {
					point.setPrivate(value.equals("rw"));
				} else if (key.equals("idx")) {
					point.setIdx(Long.parseLong(value));
				}

				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "Data");
			} else {
				Utils.skip(parser);
			}
		}
	}

	@Override
	public int compareTo(Point point) {
		return (int) (idx - point.idx);
	}

	public static CursorFields getCursorFields(Cursor cursor) {
		CursorFields cf = new CursorFields();
		cf.name = cursor.getColumnIndex("name");
		cf.desc = cursor.getColumnIndex("desc");
		cf._id = cursor.getColumnIndex("_id");
		cf.latitude = cursor.getColumnIndex("latitude");
		cf.longitude = cursor.getColumnIndex("longitude");
		cf.audioUrl = cursor.getColumnIndex("audioUrl");
		cf.photoUrl = cursor.getColumnIndex("photoUrl");
		cf.isPrivate = cursor.getColumnIndex("private");
		cf.uuid = cursor.getColumnIndex("uuid");
		return cf;
	}
}
