package com.mid.lib.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

public class AACEncoder  implements  AudioRecorder.Callback{
	private final static String TAG = "AACEncoder";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private int SAMPLE_RATE = 44100; //采样率 8K或16K 44100
    private int CHANNEL_COUNT = 2;
    private int PCM_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO; //CHANNEL_IN_MONO音频通道(单声道)  双：CHANNEL_IN_STEREO
 
    private BlockingQueue<byte[]> mQueue;  // data will put in it when decode one frame done
    private MediaCodec mEncoder;                // API >= 16(Android4.1.2)
    private BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private MediaFormat mFormat;
    // we do encode in a seperate thread
    private volatile boolean mEncodeLooping;  // encoding is running
    private volatile boolean mEncodeDone; 		// encoding done
    private Thread mEncodeThread;
    private EncodeTask mEncodeTask;
    private long mNowUs;  // us的当前时间
    
    private Callback mCallback;
    
    public AACEncoder(){
    	mEncodeTask = new EncodeTask();
    }
    
    public void setCallback(Callback callback){
    	mCallback = callback;
    }

	@Override
	public void onPcmData(byte[] pcm) {
		try {
			mQueue.put(pcm);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public interface Callback {
    	public void onAudioFrame(final ByteBuffer buff, final BufferInfo info);
    }
    
    /**
     * 根据参数信息来初始化AAC编码器信息
     * @param sampleRate 采样率 441000
     * @param pcmFormat  AudioFormat.ENCODING_PCM_16BIT AudioFormat.ENCODING_PCM_8BIT
     * @param channelCount 1 2 
     */
    public void initEncoder(int sampleRate, int pcmFormat, int channelCount){
    	if(mEncoder != null){
    		return;
    	} 
    	mBufferInfo = new BufferInfo();
    	mQueue = new LinkedBlockingQueue<byte[]>();
    	mFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
    	mFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
    	mFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, CHANNEL_CONFIG);
    	//int bitRate = sampleRate * pcmFormat * channelCount; // 44.1 * 2 * 2
        final  int bitRate = 64000;  //  32k 64k 128k etc, 这个控制生成的文件大小
    	mFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    	String msg = String.format("mForat = %s", mFormat.toString());
    	Log.i(TAG, msg);
		try {
			mEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public void startEncode(){
    	if(mEncoder == null){
    		Log.e(TAG, "startEncode, mEncoder is null, called initEncoder");
    		initEncoder(SAMPLE_RATE, PCM_FORMAT, CHANNEL_COUNT);
    	}
    	if(mEncodeLooping){
    		Log.e(TAG, "startEncode, mEncodeLooping is true, you should stop current encoding first");
    	}
    	mEncodeThread = new Thread(mEncodeTask);
    	mEncodeLooping = true;
    	mEncodeThread.start();
    }
    
    public void stopEncode(){
    	mEncodeDone = true;
    }
    
    private void doEncode(byte[] data){
    	ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
    	int inputBufferIndex = mEncoder.dequeueInputBuffer(0); // wait for ever
    	if(inputBufferIndex >= 0 ){
    		ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
    		inputBuffer.clear();
    		inputBuffer.put(data);
    		long pts = new Date().getTime() * 1000 - mNowUs;
    		mEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, pts, 
    				mEncodeDone ? MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);
    	}
    	ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
    	int outputBufferIndex = 0;
    	while((outputBufferIndex=mEncoder.dequeueOutputBuffer(mBufferInfo, 0))!= 
    			MediaCodec.INFO_TRY_AGAIN_LATER){
    		if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
    			// encode done
    			mEncodeLooping = false;
    			mEncodeThread.interrupt();
    			break;
    		}
    		switch (outputBufferIndex) {
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				outputBuffers = mEncoder.getOutputBuffers();
				break;
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				// TODO: 
				//加入音轨的时刻,一定要等编码器设置编码格式完成后，再将它加入到混合器中，
				// 编码器编码格式设置完成的标志是dequeueOutputBuffer得到返回值为MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
				final MediaFormat newformat = mEncoder.getOutputFormat(); // API >= 16
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
                if(outputBufferIndex >= 0 && mBufferInfo.size != 0 && !mEncodeDone){
                	Log.d(TAG, String.format("One frame OK, index=%d, size=%d",  outputBufferIndex, mBufferInfo.size));
                	mCallback.onAudioFrame(outputBuffers[outputBufferIndex], mBufferInfo);
                }
                mEncoder.releaseOutputBuffer(outputBufferIndex, false);  // 释放资源
				break;
			}
    	}

    }
    
    public class EncodeTask implements Runnable {

		@Override
		public void run() {
			mNowUs = System.currentTimeMillis() * 1000;
			if(mEncoder == null) return;
			mEncodeDone = false;
			mEncoder.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mEncoder.start();
			while(mEncodeLooping && !Thread.interrupted()){
				try {
					byte[] data = mQueue.take();
					doEncode(data);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			if(mEncoder != null){
				mEncoder.stop();
				mEncoder.release();
				mEncoder = null;
			}
			mQueue.clear();
		}
    	
    }
}
