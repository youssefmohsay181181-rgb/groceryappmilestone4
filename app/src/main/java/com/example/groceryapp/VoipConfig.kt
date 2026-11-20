package com.example.groceryapp

/**
 * Static VoIP settings (hard-coded on purpose, no signalling).
 *
 * Buyer (tablet) records the microphone and sends raw PCM audio over UDP to PC_IP:PORT.
 * The PC forwards that UDP port into the emulator (adb emu redir add udp:50005:50005),
 * where the seller's screen plays it with AudioTrack. Wireshark on the PC captures it (udp.port == 50005).
 */
object VoipConfig {
    const val PC_IP = "192.168.1.189"   // PC running Android Studio + emulator + Wireshark (Wi-Fi IPv4)
    const val PORT = 50005              // UDP port

    const val SAMPLE_RATE = 16000       // 16 kHz, mono, 16-bit PCM
    const val PACKET_BYTES = 640        // 20 ms of audio per UDP packet
}
