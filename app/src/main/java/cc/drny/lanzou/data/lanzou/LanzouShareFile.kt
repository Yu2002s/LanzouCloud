package cc.drny.lanzou.data.lanzou

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

data class LanzouShareFile(
    val url: String = "",
    val pwd: String? = null,
    var extension: String? = null,
    var name: String? = null,
    var desc: String? = null,
    var downloadUrl: String? = null
): Parcelable {

    var id: Long = 0L

    var icon: Drawable? = null

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
        id = parcel.readLong()
    }

    fun getShareDesc(): String {
        return desc ?: if (pwd == null) "无密码 - 暂未解析" else "$pwd - 暂未解析"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(pwd)
        parcel.writeString(extension)
        parcel.writeString(name)
        parcel.writeString(desc)
        parcel.writeString(downloadUrl)
        parcel.writeLong(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LanzouShareFile) return false

        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<LanzouShareFile> {
        override fun createFromParcel(parcel: Parcel): LanzouShareFile {
            return LanzouShareFile(parcel)
        }

        override fun newArray(size: Int): Array<LanzouShareFile?> {
            return arrayOfNulls(size)
        }
    }


}
