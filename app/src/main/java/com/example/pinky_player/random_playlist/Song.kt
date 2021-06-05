package com.example.pinky_player.random_playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "songs")
class Song(@field:PrimaryKey val id: Long, @field:ColumnInfo(name = "title") val title: String) : Comparable<Song>, Serializable {
    @Ignore
    private var selected = false
    fun isSelected(): Boolean {
        return selected
    }

    fun setSelected(selected: Boolean) {
        this.selected = selected
    }

    override fun compareTo(other: Song): Int {
        return title.compareTo(other.title)
    }

    override fun equals(other: Any?): Boolean {
        return other is Song && id == (other).id
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}