package com.mid.lib.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;


import com.p2p.pppp_api.PPCS_APIs;
import com.shangyun.p2ptester.utils.ErrMsg;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioRecorder {
    private  final static String TAG = "rwtest";
    private int SAMPLE_RATE = 44100; //采样率 8K或16K 44100
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO; //CHANNEL_IN_MONO音频通道(单声道)  双：CHANNEL_IN_STEREO
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; //音频格式
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;  //音频源（麦克风）
    private String encodeType = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int SAMPLED_PER_FRAME = 1024*4;//100 * 1024 ;//4096
    private MediaCodec mediaEncode;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;

    public static AudioRecord audioRecord;
    private Thread recorderThread;
    private RecorderTask recorderTask;
    public boolean isRecording ;

    private int mHandleSession;  //
    private int mCommunicationChannel; //

    public List<Callback> mCallbacks = new ArrayList<>();

    public AudioRecorder(int s, int ch){
        mHandleSession = s;
        mCommunicationChannel = ch;
        isRecording = false;
        recorderTask = new RecorderTask();
    }

    public void setNetinfo(int s, int ch){
        mHandleSession = s;
        mCommunicationChannel = ch;
    }
    public AudioRecorder() {
        mHandleSession = -1;
        mCommunicationChannel = -1;
        isRecording = false;
        recorderTask = new RecorderTask();
    }

    public void startAudioRecord(){
        recorderThread = new Thread(recorderTask);
        recorderThread.start();
    }

    private void send(byte[] data){
        int rv = 0;
        int []wsize = new int[1];
        wsize[0] = data.length;
        if(mHandleSession != -1){
            rv = PPCS_APIs.PPCS_Check_Buffer(mHandleSession, (byte) mCommunicationChannel, wsize, null);
            Log.i(TAG, String.format("PPCS_Check_Buffer: rv = %d, msg=%s", rv, ErrMsg.getErrorMessage(rv)));
            rv = PPCS_APIs.PPCS_Write(mHandleSession, (byte) mCommunicationChannel, data, data.length);
            Log.i(TAG, String.format("ThreadWrite CH=%d, ret=%d, msg=%s!!\n", mCommunicationChannel, rv, ErrMsg.getErrorMessage(rv)));
        }
    }


    public void stopAudioRecord(){
        isRecording = false;
    }

    public void addCallback(Callback cb){
        mCallbacks.add(cb);
    }
    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 8:16KHz  4:44.1KHZ 7:22050
        int chanCfg = 2; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;

    }

    public interface  Callback{
        public void onPcmData(byte[] pcm);
    }
    private final class RecorderTask implements  Runnable{
        OutputStream fout;
        public  RecorderTask(){

        }

        @Override
        public void run() {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(
                    AUDIO_SOURCE,   //音频源
                    SAMPLE_RATE,    //采样率
                    CHANNEL_CONFIG,  //音频通道
                    AUDIO_FORMAT,    //音频格式\采样精度
                    bufferSizeInBytes * 4 //缓冲区
                    );
            try {
                fout = new FileOutputStream("/sdcard/test.aac");
            } catch (FileNotFoundException e) {
                Log.i(TAG, e.getMessage());
                fout = null;
            }
            AACEncoder encoder = new AACEncoder();
            AACEncoder.Callback callback = new AACEncoder.Callback() {

                @Override
                public void onAudioFrame(ByteBuffer buff, MediaCodec.BufferInfo info) {
                    try {
                        Log.i(TAG, String.format("onAudioFrame, offset=%d, size=%d", info.offset, info.size));
                        Log.i(TAG, String.format("onAudioFrame, bf.position=%d, limit=%d", buff.position(), buff.limit()));
                        byte[] data = new byte[info.size + 7];
                        addADTStoPacket(data, info.size+7);
                        buff.get(data, 7, info.size);
                        send(data);
                        fout.write(data);
                        fout.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            encoder.setCallback(callback);
            addCallback(encoder);
            encoder.startEncode();
            audioRecord.startRecording();
            isRecording = true;
            byte[] buff = new byte[SAMPLED_PER_FRAME];
            int rv;
            Log.i(TAG, "RecorderTask: running!!!!");
            while(isRecording){
                rv = audioRecord.read(buff, 0, SAMPLED_PER_FRAME);
                if(rv == AudioRecord.ERROR_BAD_VALUE || rv == AudioRecord.ERROR_INVALID_OPERATION){
                    Log.e(TAG, "audioRecord.read FAIL rv=" + rv);
                    continue;
                }
                Log.i(TAG, "audioRecord.read, rv=" + rv);
                if(rv > 0){
                    for(Callback cb: mCallbacks){
                        cb.onPcmData(buff);
                    }
                }
            }
            if(encoder != null){
                encoder.stopEncode();
            }
            if(fout != null){
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(audioRecord != null){
                Log.i(TAG, "audioRecord.release");
                audioRecord.release();
            }


        }
    }
}
