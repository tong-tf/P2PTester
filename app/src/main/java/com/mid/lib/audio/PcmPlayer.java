package com.mid.lib.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class PcmPlayer implements AACDecoder.Callback {
    private int mFrequent;  // 采样率
    private int mChannel;  // 声道
    private int mSampleBit;  // 采样精度
    private AudioTrack mAudioTrack;

    public PcmPlayer(int frequent, int channel, int sampleBit){
        mChannel = channel;
        mFrequent = frequent;
        mSampleBit = sampleBit;
    }

    public  PcmPlayer(){
        mFrequent = 44100;
        mChannel = AudioFormat.CHANNEL_IN_STEREO;
        mSampleBit = AudioFormat.ENCODING_PCM_16BIT;
    }

    public void init(){
        if(mAudioTrack != null){
            release();
        }

        int size = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFrequent, mChannel, mSampleBit,
                size, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    public void play(byte[] pcm, int start , int end){
        if(pcm==null || end - start <= 0){
            return ;
        }
        mAudioTrack.write(pcm, start, end-start);
    }

    public void release(){
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }


    private int getMinBufferSize(){
        return AudioTrack.getMinBufferSize(mFrequent, mChannel, mSampleBit);
    }


    @Override
    public void onAudioFrame(ByteBuffer buff, MediaCodec.BufferInfo info) {
        if(buff == null){
            release();
        }else{
            byte[] data = new byte[info.size];
            buff.get(data);
            play(data, 0, data.length);
        }

    }
}
