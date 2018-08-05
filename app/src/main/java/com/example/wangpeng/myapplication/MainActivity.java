package com.example.wangpeng.myapplication;

import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerUtils;
import javazoom.jl.decoder.SampleBuffer;


public class MainActivity extends Activity {

    private Decoder mDecoder;
    private AudioTrack mAudioTrack;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        String[] permissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestPermissions();
        JavaLayerUtils.setContext(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int sampleRate = 16000;
        final int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM);

        mDecoder = new Decoder();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = new URL("https://weixin.wangp.org/pre-decode.mp3")
                            .openConnection()
                            .getInputStream();
                    Bitstream bitstream = new Bitstream(in);

                    final int READ_THRESHOLD = 2147483647;
                    int framesReaded = READ_THRESHOLD;

                    Header header;
                    for(; framesReaded-- > 0 && (header = bitstream.readFrame()) != null;) {
                        SampleBuffer sampleBuffer = (SampleBuffer) mDecoder.decodeFrame(header, bitstream);
                        short[] buffer = sampleBuffer.getBuffer();
                        short[] newBuffer = new short[buffer.length / 4];
                        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length / 4);
                        Log.d("longyin01", "buffer size:" + newBuffer.length + "," + Arrays.toString(newBuffer));
                        writeToFile(newBuffer);
                        mAudioTrack.write(newBuffer, 0, newBuffer.length);
                        bitstream.closeFrame();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        mAudioTrack.play();
    }
    private void writeToFile(short[] buffer) {
        byte[] pcm = new byte[buffer.length * 2];
        for (int i = 0; i < buffer.length; i++) {
            int j = i * 2;
            if (j >= 0 && j < pcm.length - 1) {
                pcm[j] = (byte) (buffer[i] & 0xff);
                pcm[j + 1] = (byte) ((buffer[i] >> 8) & 0xff);
            }
        }
        File file = new File("/sdcard/1.pcm");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(pcm);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAudioTrack.stop();
    }
}
