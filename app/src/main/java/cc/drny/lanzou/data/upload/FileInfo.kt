package cc.drny.lanzou.data.upload

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class FileInfo(
    var id: Long = -1L,
    val name:String,
    var path: String,
    var fileLength: Long = -1,
    var fileDesc: String = "",
    var extension: String? = null,
): Parcelable {
    var type: Int = -1
    var isSelected: Boolean = false
    var icon: Drawable? = null

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()
    ) {
        type = parcel.readInt()
        isSelected = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(path)
        parcel.writeLong(fileLength)
        parcel.writeString(fileDesc)
        parcel.writeString(extension)
        parcel.writeInt(type)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileInfo) return false
        if (id != other.id) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<FileInfo> {
        override fun createFromParcel(parcel: Parcel): FileInfo {
            return FileInfo(parcel)
        }

        override fun newArray(size: Int): Array<FileInfo?> {
            return arrayOfNulls(size)
        }
    }
}