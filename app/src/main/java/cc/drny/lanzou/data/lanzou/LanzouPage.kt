package cc.drny.lanzou.data.lanzou

import android.os.Parcel
import android.os.Parcelable

data class LanzouPage(
    val folderId: Long = -1,
    var name: String = "根目录",
    var page: Int = 1
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(folderId)
        parcel.writeString(name)
        parcel.writeInt(page)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LanzouPage> {
        override fun createFromParcel(parcel: Parcel): LanzouPage {
            return LanzouPage(parcel)
        }

        override fun newArray(size: Int): Array<LanzouPage?> {
            return arrayOfNulls(size)
        }
    }
}
