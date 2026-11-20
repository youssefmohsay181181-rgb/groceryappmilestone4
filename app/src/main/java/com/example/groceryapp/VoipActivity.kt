package com.example.groceryapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.groceryapp.databinding.ActivityVoipBinding
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Simple one-way VoIP over UDP (static IP and port from [VoipConfig]).
 *  - Buyer  -> "Start talking": microphone -> UDP packets to LISTENER_IP:PORT
 *  - Seller -> "Start listening": UDP packets on PORT -> speaker (AudioTrack)
 */
class VoipActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoipBinding
    private var isBuyer = true

    @Volatile private var running = false
    private var worker: Thread? = null
    private var socket: DatagramSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isBuyer = intent.getStringExtra(Roles.EXTRA_ROLE) != Roles.SELLER
        binding.tvVoipInfo.text = if (isBuyer)
            "Talking to ${VoipConfig.LISTENER_IP} on UDP port ${VoipConfig.PORT}"
        else
            "Listening on UDP port ${VoipConfig.PORT}"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnVoip.setOnClickListener { if (running) stop() else start() }
        showState()
    }

    private fun start() {
        if (isBuyer && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        running = true
        worker = Thread { if (isBuyer) talk() else listen() }.also { it.start() }
        showState()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) start()
        else binding.tvVoipStatus.text = "Microphone permission is needed to talk"
    }

    private fun stop() {
        running = false
        socket?.close() // unblocks receive()
        worker = null
        showState()
    }

    private fun showState() {
        binding.btnVoip.text = when {
            running -> "Stop"
            isBuyer -> "Start talking"
            else -> "Start listening"
        }
        binding.tvVoipStatus.text = when {
            !running -> "Stopped"
            isBuyer -> "Talking… speak into the microphone"
            else -> "Listening… waiting for audio"
        }
    }

    /** Buyer: microphone -> UDP. */
    @Suppress("MissingPermission") // checked in start()
    private fun talk() {
        val minBuf = AudioRecord.getMinBufferSize(
            VoipConfig.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, VoipConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, VoipConfig.PACKET_BYTES * 4)
        )
        val sock = DatagramSocket().also { socket = it }
        try {
            val target = InetAddress.getByName(VoipConfig.LISTENER_IP)
            val buffer = ByteArray(VoipConfig.PACKET_BYTES)
            recorder.startRecording()
            while (running) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) sock.send(DatagramPacket(buffer, read, target, VoipConfig.PORT))
            }
        } catch (e: Exception) {
            if (running) showError(e)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            sock.close()
        }
    }

    /** Seller: UDP -> speaker. */
    private fun listen() {
        val minBuf = AudioTrack.getMinBufferSize(
            VoipConfig.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(VoipConfig.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, VoipConfig.PACKET_BYTES * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val sock = try {
            DatagramSocket(VoipConfig.PORT).also { socket = it }
        } catch (e: Exception) {
            track.release()
            showError(e)
            return
        }
        try {
            val buffer = ByteArray(2048)
            val packet = DatagramPacket(buffer, buffer.size)
            track.play()
            var count = 0
            while (running) {
                packet.length = buffer.size
                sock.receive(packet)
                track.write(packet.data, 0, packet.length)
                count++
                if (count % 50 == 0) { // about once a second
                    val from = packet.address.hostAddress
                    runOnUiThread { binding.tvVoipStatus.text = "Receiving audio from $from ($count packets)" }
                }
            }
        } catch (e: Exception) {
            if (running) showError(e)
        } finally {
            runCatching { track.stop() }
            track.release()
            sock.close()
        }
    }

    private fun showError(e: Exception) = runOnUiThread {
        running = false
        showState()
        binding.tvVoipStatus.text = "Error: ${e.message}"
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }
}
