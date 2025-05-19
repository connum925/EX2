package com.example.exm2

import android.os.Parcel
import android.os.Parcelable

data class Book(
    var key: String = "",
    var title: String = "",
    var author_name: List<String>? = null,
    var cover_i: Int? = null,
    var description: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString()
    )

    fun getCoverUrl(): String? {
        return cover_i?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(title)
        parcel.writeStringList(author_name)
        parcel.writeValue(cover_i)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book {
            return Book(parcel)
        }

        override fun newArray(size: Int): Array<Book?> {
            return arrayOfNulls(size)
        }
    }
}