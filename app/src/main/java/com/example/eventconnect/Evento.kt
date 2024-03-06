package com.example.eventconnect

import android.os.Parcel
import android.os.Parcelable

data class Evento(
    var id: String?,
    var name: String?,
    var ciudad: String?,
    var lugar: String?,
    var link: String?,
    var info: String?,
    var date: String?,
    var imagenUrl: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(ciudad)
        parcel.writeString(lugar)
        parcel.writeString(link)
        parcel.writeString(info)
        parcel.writeString(date)
        parcel.writeString(imagenUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Evento> {
        override fun createFromParcel(parcel: Parcel): Evento {
            return Evento(parcel)
        }

        override fun newArray(size: Int): Array<Evento?> {
            return arrayOfNulls(size)
        }
    }
}
