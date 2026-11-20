package com.example.groceryapp

/**
 * Static VoIP settings (hard-coded on purpose, no signalling).
 *
 * Buyer (tablet) records the microphone and sends raw PCM audio over UDP to LISTENER_IP:PORT.
 * The seller's phone (LISTENER_IP) receives it on PORT and plays it with AudioTrack.
 * Both devices must be on the same Wi-Fi.
 */
object VoipConfig {
    const val LISTENER_IP = "192.168.1.170" // seller's phone that listens (Wi-Fi IPv4)
    const val PORT = 50005                  // UDP port

    const val SAMPLE_RATE = 16000           // 16 kHz, mono, 16-bit PCM
    const val PACKET_BYTES = 640            // 20 ms of audio per UDP packet
}
