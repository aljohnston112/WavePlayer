package io.fourthFinger.pinkyPlayer.random_playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * A selectable song.
 *
 * @param id The unique id of the song.
 * @param title The title of the song.
 */
@Entity(tableName = "songs")
class Song(
    @field:PrimaryKey val id: Long,
    @field:ColumnInfo(name = "title") val title: String
) : Comparable<Song>, Serializable {

    @Ignore @Volatile
    private var selected = false

    /**
     * Gets the selection status of this song.
     *
     * @return The selection status of this song.
     */
    @Synchronized
    fun isSelected(): Boolean {
        return selected
    }

    /**
     * Sets this song's selection status.
     *
     * @param selected Whether to set this songs to selected.
     */
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