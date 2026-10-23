// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.bytes
import app.reseam.youtube.core.number
import app.reseam.youtube.core.string
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private const val PREFERENCES = "youtube"
private const val ENCRYPTION_KEY = "downloads_encryption_key"
private const val ACCOUNT_FOLDER_PREFIX = "offline_identity_nonce_mapping_"

// PlayerResponse.videoDetails and its fields.
private const val VIDEO_DETAILS = 11
private const val DETAILS_TITLE = 15
private const val DETAILS_AUTHOR = 33
private const val DETAILS_THUMBNAIL = 25
private const val THUMBNAIL_IMAGES = 1
private const val IMAGE_URL = 1
private const val IMAGE_WIDTH = 2

// The offline video record's music metadata extension, which YouTube Music fills for songs.
private const val OFFLINE_MUSIC_METADATA = 14
private const val MUSIC_METADATA_EXTENSION = 112520939
private const val MUSIC_TITLE = 1
private const val MUSIC_ARTIST = 3
private const val MUSIC_ALBUM = 9

// FormatStream fields.
private const val FORMAT_ITAG = 1
private const val FORMAT_MIME_TYPE = 5
private const val FORMAT_LAST_MODIFIED = 11

private const val PLAYLIST_TITLE = 5

internal class SavedStream(
    val cacheKey: String,
    val mimeType: String,
    val length: Long,
    val folders: List<File>,
)

internal class SavedVideo(
    val videoId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val cover: File?,
    val streams: List<SavedStream>,
)

internal class SavedPlaylist(val title: String, val videos: List<SavedVideo>)

/**
 * The app's offline library as its downloader lays it out: per signed-in account, a database named after the
 * account and a stream cache in a folder the shared preferences map it to.
 */
internal class OfflineStore(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val key: SecretKey
        get() =
            SecretKeySpec(
                Base64.decode(checkNotNull(preferences.getString(ENCRYPTION_KEY, null)), Base64.DEFAULT),
                "AES",
            )

    private val accounts: List<Pair<String, String>>
        get() =
            preferences.all.mapNotNull { (name, folder) ->
                if (name.startsWith(ACCOUNT_FOLDER_PREFIX) && folder is String)
                    name.removePrefix(ACCOUNT_FOLDER_PREFIX) to folder
                else null
            }

    // Menus ask these while they open, so they read only the stream rows.
    fun isSaved(videoId: String): Boolean =
        accounts.any { (identity, _) -> read(identity) { it.exists(SAVED_VIDEO, videoId) } == true }

    fun isPlaylistSaved(playlistId: String): Boolean =
        accounts.any { (identity, _) -> read(identity) { it.exists(SAVED_PLAYLIST_VIDEO, playlistId) } == true }

    fun video(videoId: String): SavedVideo? =
        accounts.firstNotNullOfOrNull { (identity, folder) ->
            read(identity) { database -> video(database, videoId, streamFolders(folder)) }
        }

    fun playlist(playlistId: String): SavedPlaylist? =
        accounts.firstNotNullOfOrNull { (identity, folder) ->
            read(identity) { database ->
                val record =
                    database.blob("SELECT offline_playlist_data_proto FROM playlistsV13 WHERE id = ?", playlistId)
                        ?: return@read null
                val folders = streamFolders(folder)
                val videos =
                    database
                        .rawQuery(
                            "SELECT video_id FROM playlist_video WHERE playlist_id = ? ORDER BY index_in_playlist",
                            arrayOf(playlistId),
                        )
                        .use { rows -> buildList { while (rows.moveToNext()) add(rows.getString(0)) } }
                        .mapNotNull { video(database, it, folders) }
                SavedPlaylist(Proto.string(record, PLAYLIST_TITLE) ?: playlistId, videos)
            }
        }

    // Storage locations the downloader can choose: each external files directory, then internal storage.
    private fun streamFolders(folder: String): List<File> =
        (context.getExternalFilesDirs(null).filterNotNull() + context.filesDir).map {
            File(it, "offline/$folder/streams")
        }

    private fun <T> read(identity: String, query: (SQLiteDatabase) -> T?): T? {
        val path = context.getDatabasePath("offline.$identity.db")
        if (!path.exists()) return null
        return SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use(query)
    }

    private fun video(database: SQLiteDatabase, videoId: String, folders: List<File>): SavedVideo? {
        val streams =
            database
                .rawQuery(
                    "SELECT format_stream_proto, bytes_total, bytes_transferred FROM streams WHERE video_id = ?",
                    arrayOf(videoId),
                )
                .use { rows ->
                    buildList {
                        while (rows.moveToNext()) {
                            val length = rows.getLong(1)
                            if (length <= 0 || rows.getLong(2) < length) return null
                            add(stream(videoId, rows.getBlob(0), length, folders))
                        }
                    }
                }
        if (streams.isEmpty()) return null
        val (response, record) =
            database
                .rawQuery(
                    "SELECT player_response_proto, offline_video_data_proto FROM videosV2 WHERE id = ? AND deleted = 0",
                    arrayOf(videoId),
                )
                .use { if (it.moveToFirst()) it.getBlob(0) to it.getBlob(1) else return null }
        val fields = Proto.fields(checkNotNull(Proto.bytes(response, VIDEO_DETAILS)))
        val music =
            record
                ?.let { Proto.bytes(it, OFFLINE_MUSIC_METADATA) }
                ?.let { Proto.bytes(it, MUSIC_METADATA_EXTENSION) }
                ?.let(Proto::fields)
        return SavedVideo(
            videoId = videoId,
            title = music?.text(MUSIC_TITLE) ?: fields.text(DETAILS_TITLE) ?: videoId,
            artist = music?.text(MUSIC_ARTIST) ?: fields.text(DETAILS_AUTHOR),
            album = music?.text(MUSIC_ALBUM),
            cover = fields.bytes(DETAILS_THUMBNAIL)?.let(::localThumbnail),
            streams = streams,
        )
    }

    private fun stream(videoId: String, format: ByteArray, length: Long, folders: List<File>): SavedStream {
        val fields = Proto.fields(format)
        val itag = checkNotNull(fields.number(FORMAT_ITAG))
        val lastModified = checkNotNull(fields.number(FORMAT_LAST_MODIFIED))
        return SavedStream(
            cacheKey = "$videoId.$itag.$lastModified",
            mimeType = checkNotNull(fields.string(FORMAT_MIME_TYPE)).substringBefore(';').trim(),
            length = length,
            folders = folders,
        )
    }

    // The downloader keeps the largest thumbnail it saved as a local file next to the record.
    private fun localThumbnail(thumbnail: ByteArray): File? =
        Proto.fields(thumbnail)
            .filter { it.number == THUMBNAIL_IMAGES && it.bytes != null }
            .map { Proto.fields(checkNotNull(it.bytes)) }
            .mapNotNull { image ->
                val url = image.string(IMAGE_URL) ?: return@mapNotNull null
                if (!url.startsWith("file://")) return@mapNotNull null
                File(url.removePrefix("file://")).takeIf(File::isFile)?.let { it to (image.number(IMAGE_WIDTH) ?: 0) }
            }
            .maxByOrNull { it.second }
            ?.first
}

private const val COMPLETE_STREAMS =
    "SELECT video_id FROM streams GROUP BY video_id HAVING MIN(bytes_total > 0 AND bytes_transferred >= bytes_total) = 1"
private const val SAVED_VIDEO =
    "SELECT 1 FROM videosV2 WHERE id = ? AND deleted = 0 AND id IN ($COMPLETE_STREAMS) LIMIT 1"
private const val SAVED_PLAYLIST_VIDEO =
    "SELECT 1 FROM playlist_video WHERE playlist_id = ? AND video_id IN ($COMPLETE_STREAMS) LIMIT 1"

// Records leave unknown metadata as empty strings.
private fun List<Proto.Field>.text(number: Int): String? = string(number)?.takeIf(String::isNotBlank)

private fun SQLiteDatabase.exists(sql: String, argument: String): Boolean =
    rawQuery(sql, arrayOf(argument)).use { it.moveToFirst() }

private fun SQLiteDatabase.blob(sql: String, argument: String): ByteArray? =
    rawQuery(sql, arrayOf(argument)).use { if (it.moveToFirst()) it.getBlob(0) else null }
