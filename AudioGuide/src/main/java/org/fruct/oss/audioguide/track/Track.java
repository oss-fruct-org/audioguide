package org.fruct.oss.audioguide.track;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Field;

public class Track implements Parcelable, Comparable<Track> {
	private String name;
	private String description;
	private String url;

	private boolean isLocal;
	private boolean isActive;

	private long localId;

	public Track(String name, String description, String url) {
		this.name = name;
		this.description = description;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
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


	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
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


	public long getLocalId() {
		return localId;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}


	public String getId() {
		return name;
	}


	public void setField(String field, Object value) throws NoSuchFieldException {
		// TODO: use annonations or if..else if... otherwise it will break proguard

		Field rField = Track.class.getDeclaredField(field);
		try {
			rField.set(this, value);
		} catch (IllegalAccessException e) {
			throw new NoSuchFieldException("Trying to access illegal field");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

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
		parcel.writeLong(localId);
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
			track.localId = parcel.readLong();

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
