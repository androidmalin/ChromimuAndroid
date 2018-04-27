package org.chromium.base.process_launcher;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import javax.annotation.concurrent.Immutable;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.annotations.UsedByReflection;

@MainDex
@Immutable
@UsedByReflection("child_process_launcher_helper_android.cc")
public final class FileDescriptorInfo implements Parcelable {
    public static final Creator<FileDescriptorInfo> CREATOR = new Creator<FileDescriptorInfo>() {
        public FileDescriptorInfo createFromParcel(Parcel in) {
            return new FileDescriptorInfo(in);
        }

        public FileDescriptorInfo[] newArray(int size) {
            return new FileDescriptorInfo[size];
        }
    };
    public final ParcelFileDescriptor fd;
    public final int id;
    public final long offset;
    public final long size;

    public FileDescriptorInfo(int id, ParcelFileDescriptor fd, long offset, long size) {
        this.id = id;
        this.fd = fd;
        this.offset = offset;
        this.size = size;
    }

    FileDescriptorInfo(Parcel in) {
        this.id = in.readInt();
        this.fd = (ParcelFileDescriptor) in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        this.offset = in.readLong();
        this.size = in.readLong();
    }

    public int describeContents() {
        return 1;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeParcelable(this.fd, 1);
        dest.writeLong(this.offset);
        dest.writeLong(this.size);
    }
}
