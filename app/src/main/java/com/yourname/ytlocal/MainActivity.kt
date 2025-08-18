package com.yourname.ytlocal

import android.content.Intent
import android.os.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val ytUrl = extractYoutubeUrl(sharedText)

        if (ytUrl != null) {
            statusText.text = "Downloading video..."
            val binary = extractYtDlpBinary()
            downloadVideo(binary, ytUrl)
        } else {
            statusText.text = "Invalid or no YouTube URL shared"
        }
    }

    private fun extractYoutubeUrl(text: String?): String? {
        val regex = Regex("https?://(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[^\\s]+")
        return regex.find(text ?: "")?.value
    }

    private fun extractYtDlpBinary(): File {
        val file = File(filesDir, "yt-dlp")
        if (!file.exists()) {
            assets.open("yt-dlp").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.setExecutable(true)
        }
        return file
    }

    private fun downloadVideo(binary: File, url: String) {
        Thread {
            try {
                val outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

                val process = ProcessBuilder(
                    binary.absolutePath,
                    "-f", "best",
                    "-o", "$outputPath/%(title)s.%(ext)s",
                    url
                )
                    .redirectErrorStream(true)
                    .start()

                val log = process.inputStream.bufferedReader().readText()
                val code = process.waitFor()

                runOnUiThread {
                    statusText.text = if (code == 0) {
                        "Download complete!"
                    } else {
                        "Download failed (code $code)"
                    }
                    Toast.makeText(this, log.takeLast(300), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
}
