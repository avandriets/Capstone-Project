package com.digitallifelab.environmentmonitor;

import android.os.Parcel;
import android.os.Parcelable;


public class PhotoStorage implements Parcelable {

    public PhotoStorage() {
    }

    public PhotoStorage(boolean newPhoto, String photoPath, long id) {
        this.newPhoto = newPhoto;
        this.photoPath = photoPath;
        this.id = id;
    }

    public boolean  newPhoto;
    public String   photoPath;
    public long     id;

    protected PhotoStorage(Parcel in) {
        newPhoto = in.readByte() != 0;
        photoPath = in.readString();
        id = in.readLong();
    }

    public static final Creator<PhotoStorage> CREATOR = new Creator<PhotoStorage>() {
        @Override
        public PhotoStorage createFromParcel(Parcel in) {
            return new PhotoStorage(in);
        }

        @Override
        public PhotoStorage[] newArray(int size) {
            return new PhotoStorage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (newPhoto ? 1 : 0));
        dest.writeString(photoPath);
        dest.writeLong(id);
    }
}
