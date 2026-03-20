package junzi.iwara.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import junzi.iwara.model.DownloadListItem
import junzi.iwara.model.DownloadStatus
import junzi.iwara.model.VideoDetail
import junzi.iwara.model.VideoVariant
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class IwaraDownloads(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)
        ?: error("DownloadManager unavailable")
    private val indexFile = File(appContext.filesDir, "iwara-download-index.json")

    fun enqueue(detail: VideoDetail, variant: VideoVariant): DownloadListItem {
        require(variant.downloadUrl.isNotBlank()) { "Download URL unavailable" }

        val qualityLabel = variantLabel(variant)
        val fileName = buildFileName(detail.title, detail.id, qualityLabel)
        val request = DownloadManager.Request(Uri.parse(variant.downloadUrl))
            .setMimeType("video/mp4")
            .setTitle(detail.title.ifBlank { fileName })
            .setDescription(qualityLabel)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)
        val record = DownloadRecord(
            downloadId = downloadId,
            videoId = detail.id,
            title = detail.title,
            thumbnailUrl = detail.posterUrl,
            qualityLabel = qualityLabel,
            fileName = fileName,
            createdAtMs = System.currentTimeMillis(),
        )
        upsertRecord(record)
        return mergeRecord(record, queryStatuses(longArrayOf(downloadId))[downloadId])
    }

    fun list(): List<DownloadListItem> {
        val records = readRecords().sortedByDescending { it.createdAtMs }
        val statuses = queryStatuses(records.map { it.downloadId }.toLongArray())
        return records.map { record -> mergeRecord(record, statuses[record.downloadId]) }
    }

    private fun queryStatuses(ids: LongArray): Map<Long, DownloadQueryState> {
        if (ids.isEmpty()) return emptyMap()
        val query = DownloadManager.Query().setFilterById(*ids)
        val result = linkedMapOf<Long, DownloadQueryState>()
        downloadManager.query(query)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val statusIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val downloadedIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val localUriIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
            while (cursor.moveToNext()) {
                val downloadId = cursor.getLong(idIndex)
                val status = mapStatus(cursor.getInt(statusIndex))
                val downloaded = cursor.getLong(downloadedIndex)
                val total = cursor.getLong(totalIndex)
                val progress = if (total > 0L && downloaded >= 0L) {
                    ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                } else {
                    null
                }
                result[downloadId] = DownloadQueryState(
                    status = status,
                    progressPercent = progress,
                    localUri = cursor.getString(localUriIndex),
                )
            }
        }
        return result
    }

    private fun mapStatus(status: Int): DownloadStatus = when (status) {
        DownloadManager.STATUS_PENDING -> DownloadStatus.Pending
        DownloadManager.STATUS_RUNNING -> DownloadStatus.Running
        DownloadManager.STATUS_PAUSED -> DownloadStatus.Paused
        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Successful
        DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
        else -> DownloadStatus.Unknown
    }

    private fun mergeRecord(record: DownloadRecord, state: DownloadQueryState?): DownloadListItem =
        DownloadListItem(
            downloadId = record.downloadId,
            videoId = record.videoId,
            title = record.title,
            thumbnailUrl = record.thumbnailUrl,
            qualityLabel = record.qualityLabel,
            fileName = record.fileName,
            createdAtMs = record.createdAtMs,
            status = state?.status ?: DownloadStatus.Unknown,
            progressPercent = state?.progressPercent,
            localUri = state?.localUri,
        )

    private fun upsertRecord(record: DownloadRecord) {
        val records = readRecords().filterNot { it.downloadId == record.downloadId } + record
        writeRecords(records)
    }

    private fun readRecords(): List<DownloadRecord> {
        if (!indexFile.exists()) return emptyList()
        val text = indexFile.readText(Charsets.UTF_8)
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                add(DownloadRecord.fromJson(array.getJSONObject(index)))
            }
        }
    }

    private fun writeRecords(records: List<DownloadRecord>) {
        val array = JSONArray()
        records.sortedByDescending { it.createdAtMs }.forEach { array.put(it.toJson()) }
        indexFile.writeText(array.toString(), Charsets.UTF_8)
    }

    private fun buildFileName(title: String, videoId: String, qualityLabel: String): String {
        val safeTitle = title
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '.')
            .ifBlank { "Iwara Video" }
            .take(120)
        val safeQuality = qualityLabel
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .trim(' ', '.')
            .ifBlank { "default" }
        return "$safeTitle [$safeQuality] [$videoId].mp4"
    }

    private fun variantLabel(variant: VideoVariant): String = when {
        variant.name.equals("Source", ignoreCase = true) -> "Source"
        variant.name.endsWith("p", ignoreCase = true) -> variant.name
        else -> "${variant.name}p"
    }

    private data class DownloadQueryState(
        val status: DownloadStatus,
        val progressPercent: Int?,
        val localUri: String?,
    )

    private data class DownloadRecord(
        val downloadId: Long,
        val videoId: String,
        val title: String,
        val thumbnailUrl: String?,
        val qualityLabel: String,
        val fileName: String,
        val createdAtMs: Long,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("downloadId", downloadId)
            .put("videoId", videoId)
            .put("title", title)
            .put("thumbnailUrl", thumbnailUrl)
            .put("qualityLabel", qualityLabel)
            .put("fileName", fileName)
            .put("createdAtMs", createdAtMs)

        companion object {
            fun fromJson(json: JSONObject): DownloadRecord = DownloadRecord(
                downloadId = json.optLong("downloadId"),
                videoId = json.optString("videoId"),
                title = json.optString("title"),
                thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" },
                qualityLabel = json.optString("qualityLabel"),
                fileName = json.optString("fileName"),
                createdAtMs = json.optLong("createdAtMs"),
            )
        }
    }
}
