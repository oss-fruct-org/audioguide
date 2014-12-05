package org.fruct.oss.audioguide.track;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable, Comparable<Track> {
	private String name;

	private String description;
	private String hname;

	private String url;

	private boolean isLocal;
	private boolean isActive;
	private boolean isPrivate;

	private String photoUrl;

	private long localId;
	private long categoryId = -1;

	public Track() {
		this("", "", "");
	}

	public Track(Track track) {
		name = track.name;
		description = track.description;
		hname = track.hname;
		url = track.url;
		isLocal = track.isLocal;
		isActive = track.isActive;
		isPrivate = track.isPrivate;
		localId = track.localId;
		categoryId = track.categoryId;
	}

	public Track(Cursor cursor) {
		this(cursor.getString(0), cursor.getString(1), cursor.getString(2));

		setLocal(cursor.getInt(3) != 0);
		setCategoryId(cursor.getLong(4));
		setPrivate(cursor.getInt(5) != 0);
		if (!cursor.isNull(6)) setHname(cursor.getString(6));
		if (!cursor.isNull(7)) setPhotoUrl(cursor.getString(7));
	}

	public Track(String name, String description, String url) {
		this.name = name;
		this.description = description;
		this.url = url;
	}

	// Getters
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getHname() {
		return hname;
	}

	public String getUrl() {
		return url;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public long getCategoryId() {
		return categoryId;
	}

	public long getLocalId() {
		return localId;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	// Setters
	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setHname(String hname) {
		this.hname = hname;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public void setCategoryId(long categoryId) {
		this.categoryId = categoryId;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public boolean hasPhoto() {
		return photoUrl != null && !photoUrl.isEmpty();
	}

	public String getHumanReadableName() {
		if (hname != null)
			return hname;
		else
			return name;
	}
	public String getId() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || ((Object) this).getClass() != o.getClass()) return false;

		Track track = (Track) o;

		if (getId() != null ? !getId().equals(track.getId()) : track.getId() != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(name);
		parcel.writeString(description);
		parcel.writeString(url);
		parcel.writeInt(isActive ? 1 : 0);
		parcel.writeInt(isLocal ? 1 : 0);
		parcel.writeInt(isPrivate ? 1 : 0);
		parcel.writeLong(localId);
		parcel.writeLong(categoryId);
		parcel.writeString(photoUrl);
	}

	public static final Creator<Track> CREATOR = new Creator<Track>() {
		@Override
		public Track createFromParcel(Parcel parcel) {
			final String name = parcel.readString();
			final String description = parcel.readString();
			final String url = parcel.readString();

			Track track = new Track(name, description, url);
			track.setActive(parcel.readInt() != 0);
			track.setLocal(parcel.readInt() != 0);
			track.setPrivate(parcel.readInt() != 0);

			track.localId = parcel.readLong();
			track.categoryId = parcel.readLong();
			track.photoUrl = parcel.readString();

			return track;
		}

		@Override
		public Track[] newArray(int i) {
			return new Track[i];
		}
	};

	@Override
	public int compareTo(Track track) {
		return this.getName().compareTo(track.getName());
	}
}
