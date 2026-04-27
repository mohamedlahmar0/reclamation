package com.gestionaudit.utils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Records microphone input as 16 kHz mono PCM and converts to WAV bytes.
 * JavaFX WebView does not expose the Web Speech API; this is used for dictation instead.
 */
public final class MicToWavRecorder {

    private static final float SAMPLE_RATE = 16000f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final AudioFormat format;
    private TargetDataLine line;
    private ByteArrayOutputStream pcmBuffer;
    private volatile boolean capturing;
    private Thread captureThread;

    public MicToWavRecorder() {
        format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    }

    public boolean isLineAvailable() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        return AudioSystem.isLineSupported(info);
    }

    public void start() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format, line.getBufferSize());
        pcmBuffer = new ByteArrayOutputStream();
        capturing = true;
        line.start();
        captureThread = new Thread(this::runCapture, "gestionaudit-mic");
        captureThread.start();
    }

    private void runCapture() {
        byte[] buf = new byte[2048];
        try {
            while (capturing && line != null && line.isOpen()) {
                int n = line.read(buf, 0, buf.length);
                if (n > 0 && pcmBuffer != null) {
                    pcmBuffer.write(buf, 0, n);
                }
            }
        } catch (Exception ignored) {
            // stop() may interrupt read
        }
    }

    /**
     * Stops capture and returns WAV bytes, or empty array if almost no audio was captured.
     */
    public byte[] stopAndGetWav() throws IOException, InterruptedException {
        capturing = false;
        if (line != null) {
            line.stop();
        }
        if (captureThread != null) {
            captureThread.join(10_000);
            captureThread = null;
        }
        if (line != null) {
            line.close();
            line = null;
        }
        if (pcmBuffer == null) {
            return new byte[0];
        }
        byte[] pcm = pcmBuffer.toByteArray();
        pcmBuffer = null;
        int frameSize = format.getFrameSize();
        if (frameSize <= 0 || pcm.length < frameSize * 15) {
            return new byte[0];
        }
        long frames = pcm.length / frameSize;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcm);
             AudioInputStream ais = new AudioInputStream(bais, format, frames);
             ByteArrayOutputStream wavOut = new ByteArrayOutputStream()) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        }
    }
}
