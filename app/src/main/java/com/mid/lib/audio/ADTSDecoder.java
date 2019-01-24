package com.mid.lib.audio;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class ADTSDecoder {
	public final static String TAG = "rwtest";
	ByteBuffer header = ByteBuffer.allocate(7); // max header size 
	ByteBuffer data = ByteBuffer.allocate(2048);
	
	private final int STAGE_SYNCWORD = 0;
	private final int STAGE_HEADER = 1;
	private final int STAGE_DATA = 2;
	private int mStage = STAGE_SYNCWORD;
	private int counter = 0; // for debug 
	
	byte prev = 0x00;  // 前一个byte
	ADTSHeader adts = new ADTSHeader();
	List<ADTSFrameHandler> mHandlers = new ArrayList<ADTSFrameHandler>();



	public interface  ADTSFrameHandler {
		public void handleFrame(byte[] data, int start, int end);

	}

	public void addHandler(ADTSFrameHandler handler){
		mHandlers.add(handler);
	}

	/**
	 * 处理缓冲区中的数据，每解析一帧调用相应的处理函数
	 * @param buff 输入缓冲区
	 * @param start 起始位置
	 * @param end 终端位置
	 */
	public void process(byte buff[], int start , int end) {
		byte b;
		String msg;
		for(; start < end; start++) {
			counter++;
			b = buff[start];
			switch (mStage) {
			case STAGE_SYNCWORD:
				if(((prev & 0xff) == 0xff) && ((b&0xf0) == 0xf0)) {
					msg = String.format("syncword done=%d", counter);
					System.out.println(msg);
					mStage = STAGE_HEADER;
					header.rewind();
					header.put(prev);
					header.put(b);
				}else {
					prev = b;
				}
				break;
			case STAGE_HEADER:
				if(fillHeader(b)) {
					msg = String.format("header done=%d", counter);
					System.out.println(msg);
					if(prepareData()){
						mStage = STAGE_DATA;
					}else{
						reset(); //  header is invalid, begin search again.
					}
				}
				break;
			case STAGE_DATA:
				if(fillData(b)) {
		//			msg = String.format("data done=%d", counter);
			//		System.out.println(msg);
					data.rewind();  // for data read.
					if(mHandlers != null){
					//	Log.i(TAG, msg);

						byte[] fb = new byte[data.limit()];
						data.get(fb);
                        for(ADTSFrameHandler h : mHandlers){
                            h.handleFrame(fb, 0, fb.length);
                        }
					}
					reset(); // one frame done, start again.
				}
				break;
			default:
				break;
			}
		}
	}

	private void reset(){
		prev = 0x00;
		mStage = STAGE_SYNCWORD;

	}
	
	public void process(byte[] buff) {
		process(buff, 0, buff.length);
	}

	/**
	 * 为接收帧数据做准备
	 * @return true header is valid , false header is invalid, should start over.
	 */
	private boolean prepareData() {
		adts.setData(header.array());
		data.rewind();
		if(adts.frameLen < header.capacity() || adts.frameLen > data.capacity()){
			Log.w(TAG, "bad frame length: " + adts.frameLen);
			return false;
		}
		data.limit(adts.frameLen);
		data.put(header.array()); // put header first
		header.rewind();
		return true;
	}
	
	private boolean fillHeader(byte b) {
		if(!header.hasRemaining()) {
			return true;
		}
		header.put(b);
		return !header.hasRemaining();
	}
	
	public boolean fillData(byte ch) {
		if(!data.hasRemaining()) {
			return true;
		}
		data.put(ch);
		return !data.hasRemaining();
	}
	
	
	private static final class ADTSHeader {
		private final int[] SAMPLE_RATE = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};
		private final String[] PROFILE_DESCRIPTION = {"AAC Main", "AAC LC", "AAC SSR", "AAC LTP"};
		public int profile;
		public int sample;
		public int channel;
		public int frameLen;  // include header and aac frame
		
		public ADTSHeader() {}
		
		public void setData(byte[] buff) {
			profile = ((buff [2] >> 6) & 0x3);
			sample = (buff[2] &0x3c)>>2;
			channel = (buff[2] &0x1)<<2 | (buff[3]>>6)&0x3;
			frameLen = ((buff[3]&0x3) <<11) | ((buff[4] & 0xff) << 3) | (buff[5]>>5 & 0x7);
			String msg = String.format("profile=%s, sample=%d channel=%d, frameLen=%d",
					PROFILE_DESCRIPTION[profile],sample, channel, frameLen);
			System.out.println(msg);			
		}
	}	
}
