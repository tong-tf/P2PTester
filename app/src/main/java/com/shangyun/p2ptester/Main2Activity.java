package com.shangyun.p2ptester;

import android.Manifest;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.mid.lib.DemoSink;
import com.mid.lib.DemoSource;
import com.mid.lib.FrameCallback;
import com.mid.lib.audio.AudioRecorder;
import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_NetInfo;
import com.shangyun.p2ptester.handler.ReceiveHandler;
import com.shangyun.p2ptester.handler.SendHandler;
import com.shangyun.p2ptester.utils.ErrMsg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class Main2Activity extends AppCompatActivity  implements SurfaceHolder.Callback,
        FrameCallback {
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int TIME_INTERNAL = 30;
    private final String TAG = "Main2Activity";
    int UDP_Port = 0;
    int channel = 1;
    String initString = "EBGAEIBIKHJJGFJKEOGCFAEPHPMAHONDGJFPBKCPAJJMLFKBDBAGCJPBGOLKIKLKAJMJKFDOOFMOBECEJIMM";
    @BindView(R.id.phone_monitor)
    Button btnStart;
    @BindView(R.id.hang_up)
    Button btsHangup;
    @BindView(R.id.h264sf)
    SurfaceView h264sf;
    MediaCodec mCodec;
    int VIDEO_WIDTH = 640;
    int VIDEO_HEIGHT = 480;
    private int mHandleSession = -1;
    private String mdid = "PPCS-017130-FETFS";
    private byte mMode = 1;
    private ReceiveHandler mReceiveHandler;
    private SendHandler mSendHandler;
    private HandlerThread mReceiveThread;
    private HandlerThread mSendThread;
    private int mCount = 0;
    private AudioRecorder mRecorder;

    private String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    public static final int CODE = 0x001;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    btnStart.setEnabled(true);
                    //  btsHangup.setEnabled(true);
                    if(mRecorder != null){
                        mRecorder.setNetinfo(mHandleSession, 4);
                    }
            }
        }
    };

    private void initAudio(){
        mRecorder = new AudioRecorder();
    }
    private void initSurfaceHolder() {
        SurfaceHolder holder = h264sf.getHolder();
        holder.addCallback(this);
        Log.i(TAG, "initSurfaceHolder called");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        requestPermission();
        ButterKnife.bind(this);
        btnStart.setEnabled(false);
        btsHangup.setEnabled(false);
        initNet();
        initAudio();
        initSurfaceHolder();

    }


    private void requestPermission(){
        int need[] = new int[mPermissions.length];
        int cnt = 0;
        for(int i=0; i<mPermissions.length; i++){
            if(!EasyPermissions.hasPermissions(this, mPermissions[i])){
                need[cnt++] = i;
            }
        }

        String needPerm[] = new String[cnt];
        for(int i=0; i<cnt; i++){
            needPerm[i] = mPermissions[need[i]];
        }
        if(cnt > 0){
            requestPermissions(needPerm, 0x001);
        }

    }

    @OnClick({R.id.phone_monitor, R.id.hang_up})
    public void OnClick(View v){
        switch(v.getId()){
            case R.id.phone_monitor:
                Log.i(TAG, "xxxxx");
                btnStart.setEnabled(false);
                btsHangup.setEnabled(true);
                mSendHandler.sendMessage(mSendHandler.obtainMessage(SendHandler.MSG_SEND_JSON));
                mRecorder.startAudioRecord();
                break;
            case R.id.hang_up:
                Log.i(TAG, "hang_up");
                btnStart.setEnabled(true);
                btsHangup.setEnabled(false);
                mSendHandler.sendMessage(mSendHandler.obtainMessage(SendHandler.MSG_SEND_HANG_UP));
                mRecorder.stopAudioRecord();
                break;
        }

    }


    public void initNet() {
        // we do it in a thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (initConnection()) {
                    DemoSink sink = new DemoSink(Main2Activity.this);
                    DemoSource source = new DemoSource();
                    source.addSink(sink);
                    List<Thread> ths = new ArrayList<>(10);
                    ths.add(new Thread(source));
                    ths.add(new Thread(sink));
                    for (Thread th : ths) {
                        th.start();
                    }
                    mSendThread = new HandlerThread("send");
                    mSendThread.start();
                    mReceiveThread = new HandlerThread("receive");
                    mReceiveThread.start();
                    mReceiveHandler = new ReceiveHandler(mReceiveThread.getLooper(), mHandleSession, source);
                    mSendHandler = new SendHandler(mSendThread.getLooper(), mHandleSession);
                    mSendHandler.AddNotify(mReceiveHandler);
                    mHandler.sendMessage(mHandler.obtainMessage(1));
                }
            }
        }).start();
    }

    public boolean initConnection() {
        int nRet = PPCS_APIs.PPCS_Initialize(initString.getBytes());
        st_PPCS_NetInfo NetInfo = new st_PPCS_NetInfo();
        PPCS_APIs.PPCS_NetworkDetect(NetInfo, 0);
        mHandleSession = PPCS_APIs.PPCS_Connect(mdid, mMode, UDP_Port);
        if (mHandleSession >= 0) {
            Log.i(TAG, "Connect OK, session=" + mHandleSession);
        } else {
            if (mHandleSession == PPCS_APIs.ERROR_PPCS_USER_CONNECT_BREAK) {
                Log.i(TAG, "Connect break is called !\n");
            } else {
                String err = ErrMsg.getErrorMessage(mHandleSession);
                Log.i(TAG, String.format("Connect failed(%d) : %s \n", mHandleSession, err));
            }
        }
        return mHandleSession >= 0 ? true : false;
    }


    @Override
    public boolean onFrame(byte[] buf, int offset, int length) {
        // Get input buffer index
        Log.i(TAG, "onFrame offset=" + offset + " length: " + length);
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        // -1 表示一直等待, 0不等待, >0 等待时间, ms为单位
        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            // 清空buffer
            inputBuffer.clear();
            // 放要解码的数据
            inputBuffer.put(buf, offset, length);
            // 解码 presenstationTimeUs 可为0
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, 0, 0);
            mCount++;
        } else {
            Log.i(TAG, "inputBufferIndex fetch fail,ret = " + inputBufferIndex);
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            // render true to show frame on SF
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }

    private void initCodec() {
        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                VIDEO_WIDTH, VIDEO_HEIGHT);
        mCodec.configure(mediaFormat, h264sf.getHolder().getSurface(),
                null, 0);
        mCodec.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i(TAG, "surfaceCreated");
        initCodec();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}
