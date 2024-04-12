package com.fourthFinger.pinkyPlayer.random_playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "songs")
class Song(
    @field:PrimaryKey val id: Long,
    @field:ColumnInfo(name = "title") val title: String
) : Comparable<Song>, Serializable {

    @Ignore @Volatile
    private var selected = false
    @Synchronized
    fun isSelected(): Boolean {
        return selected
    }
    @Synchronized
    fun setSelected(selected: Boolean) {
        this.selected = selected
    }

    override fun compareTo(other: Song): Int {
        return title.compareTo(other.title)
    }

    override fun equals(other: Any?): Boolean {
        return other is Song && title == (other).title
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }

}