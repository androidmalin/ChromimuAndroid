package org.chromium.base;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import org.chromium.base.annotations.CalledByNative;

public class UnguessableToken implements Parcelable {
    public static final Creator<UnguessableToken> CREATOR = new Creator<UnguessableToken>() {
        public UnguessableToken createFromParcel(Parcel source) {
            long high = source.readLong();
            long low = source.readLong();
            if (high == 0 || low == 0) {
                return null;
            }
            return new UnguessableToken(high, low);
        }

        public UnguessableToken[] newArray(int size) {
            return new UnguessableToken[size];
        }
    };
    private final long mHigh;
    private final long mLow;

    private UnguessableToken(long high, long low) {
        this.mHigh = high;
        this.mLow = low;
    }

    @CalledByNative
    private static UnguessableToken create(long high, long low) {
        return new UnguessableToken(high, low);
    }

    @CalledByNative
    public long getHighForSerialization() {
        return this.mHigh;
    }

    @CalledByNative
    public long getLowForSerialization() {
        return this.mLow;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mHigh);
        dest.writeLong(this.mLow);
    }

    @CalledByNative
    private UnguessableToken parcelAndUnparcelForTesting() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        UnguessableToken token = (UnguessableToken) CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return token;
    }
}
