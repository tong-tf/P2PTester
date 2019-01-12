package com.mid.lib.audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AACDecoder  implements ADTSDecoder.ADTSFrameHandler {
    private final static String TAG = "rwtest";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private int SAMPLE_RATE = 44100; //采样率 8K或16K 44100
    private int CHANNEL_COUNT = 2;
    private int PCM_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO; //CHANNEL_IN_MONO音频通道(单声道)  双：CHANNEL_IN_STEREO

    private BlockingQueue<byte[]> mQueue;  // data will put in it when decode one frame done
    private MediaCodec mDecoder;                // API >= 16(Android4.1.2)
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private MediaFormat mFormat;
    // we do encode in a seperate thread
    private volatile boolean mDecodeLooping;  // encoding is running
    private volatile boolean mDecodeDone; 		// encoding done
    private Thread mDecodeThread;
    private AACDecoder.DecodeTask mDecodeTask;
    private long mNowUs;  // us的当前时间
    private Callback mCallback;

    public AACDecoder(){ mDecodeTask =  new DecodeTask();}

    public void setCallback(Callback callback){
        mCallback = callback;
    }

    public void initEncoder(int sampleRate, int pcmFormat, int channelCount){
        if(mDecoder != null){
            return;
        }
        mBufferInfo = new MediaCodec.BufferInfo();
        mQueue = new LinkedBlockingQueue<byte[]>();
        mFormat = new MediaFormat();

        mFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);

        mFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        mFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
      //  mFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, pcmFormat);
        mFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
       // mFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, CHANNEL_CONFIG);
        //int bitRate = sampleRate * pcmFormat * channelCount; // 44.1 * 2 * 2
        final  int bitRate = 64000;  //  32k 64k 128k etc, 这个控制生成的文件大小
        mFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
       mFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);

        byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
        ByteBuffer csd_0 = ByteBuffer.wrap(data);
        mFormat.setByteBuffer("csd-0", csd_0);
        String msg = String.format("mForat = %s", mFormat.toString());
        Log.i(TAG, msg);

        try {
            mDecoder = MediaCodec.createDecoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleFrame(byte[] data, int start, int end) {
        try {
            mQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface Callback {
        public void onAudioFrame(final ByteBuffer buff, final MediaCodec.BufferInfo info);
    }
    public void startDecode(){
        if(mDecoder == null){
            Log.e(TAG, "startEncode, mDecoder is null, called initEncoder");
            initEncoder(SAMPLE_RATE, PCM_FORMAT, CHANNEL_COUNT);
        }
        if(mDecodeLooping){
            Log.e(TAG, "startEncode, mEncodeLooping is true, you should stop current encoding first");
        }
        mDecodeThread = new Thread(mDecodeTask);
        mDecodeLooping = true;
        mDecodeThread.start();
    }
    private void doDecode(byte[] data){
        Log.w(TAG, String.format("data 0=%x, 1=%x, 2=%x. 3=%x", data[0], data[1], data[2], data[3]));
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        int inputBufferIndex = mDecoder.dequeueInputBuffer(-1); // wait for ever
        if(inputBufferIndex >= 0 ){
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(data);
            long pts = new Date().getTime() * 1000 - mNowUs;
            mDecoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts,
                    mDecodeDone ? MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);
        }
        ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
        int outputBufferIndex = 0;
        mBufferInfo = new MediaCodec.BufferInfo();
        try{
            // timeoutUs 此值不要设置成-1或者0， 会出现异常，
            while((outputBufferIndex=mDecoder.dequeueOutputBuffer(mBufferInfo, 200))!=
                    MediaCodec.INFO_TRY_AGAIN_LATER){
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                    // encode done
                    mCallback.onAudioFrame(null, null);  // null data means end of decode.
                    mDecodeLooping = false;
                    mDecodeThread.interrupt();
                    break;
                }
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = mDecoder.getOutputBuffers();
                        Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        // TODO:
                        //加入音轨的时刻,一定要等编码器设置编码格式完成后，再将它加入到混合器中，
                        // 编码器编码格式设置完成的标志是dequeueOutputBuffer得到返回值为MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                        final MediaFormat newformat = mDecoder.getOutputFormat(); // API >= 16
                        Log.i(TAG, "new format is: " + newformat);
                        break;
                    default:
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // You shoud set output format to muxer here when you target Android4.3 or less
                            // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                            // therefor we should expand and prepare output format from buffer data.
                            // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                            Log.d(TAG, " Audio====drain:BUFFER_FLAG_CODEC_CONFIG===");
                            mBufferInfo.size = 0;
                        }
                        if(outputBufferIndex >= 0 && mBufferInfo.size != 0 && !mDecodeDone){
                            Log.d(TAG, String.format("One frame OK, index=%d, size=%d",  outputBufferIndex, mBufferInfo.size));
                            mCallback.onAudioFrame(outputBuffers[outputBufferIndex], mBufferInfo);
                        }
                        mDecoder.releaseOutputBuffer(outputBufferIndex, false);  // 释放资源
                        break;
                }
            }

        }catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

    }

    public class DecodeTask implements Runnable {

        @Override
        public void run() {
            mNowUs = System.currentTimeMillis() * 1000;
            Log.i(TAG, "DecodeTask : " + Thread.currentThread());
            if(mDecoder == null) return;
            mDecodeDone = false;
            mDecoder.configure(mFormat, null, null, 0);
            mDecoder.start();
            while(mDecodeLooping && !Thread.interrupted()){
                try {
                    byte[] data = mQueue.take();
                    Log.i(TAG, "AACDecoder receive data: " + data.length);
                    doDecode(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            if(mDecoder != null){
                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
            }
            mQueue.clear();
        }

    }
}
