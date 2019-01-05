package com.shangyun.p2ptester;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.mid.lib.DemoSink;
import com.mid.lib.DemoSource;
import com.p2p.pppp_api.PPCS_APIs;
import com.p2p.pppp_api.st_PPCS_NetInfo;
import com.shangyun.p2ptester.handler.ReceiveHandler;
import com.shangyun.p2ptester.handler.SendHandler;
import com.shangyun.p2ptester.utils.ErrMsg;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class TestActivity extends Activity implements SurfaceHolder.Callback {
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int TIME_INTERNAL = 30;
    private final String TAG = "rwtest";
    byte CH_CMD = 0;
    byte CH_DATA = 1;
    int UDP_Port = 0;
    int channel = 1;
    String initString = "EBGAEIBIKHJJGFJKEOGCFAEPHPMAHONDGJFPBKCPAJJMLFKBDBAGCJPBGOLKIKLKAJMJKFDOOFMOBECEJIMM";
    @BindView(R.id.rw_test1)
    Button btnStart;
    @BindView(R.id.hang_up)
    Button btsHangup;
    @BindView(R.id.starth264)
    Button btnStartH264;
    @BindView(R.id.h264_test)
    Button btsH264Test;
    @BindView(R.id.MediaStarme)
    Button MediaStarme;
    @BindView(R.id.h264show)
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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    btnStart.setEnabled(true);
                    btsHangup.setEnabled(true);
            }
        }
    };
    private Runnable mH264RawHandler = new Runnable() {
        @Override
        public void run() {
            final int MAX_BUFF_SIZE = 4 * 1024 * 1024;
            byte[] buffer = new byte[1024 * 1024];
            byte[] fb = new byte[MAX_BUFF_SIZE];  // make sure space is enough for one frame
            String h264 = "/mnt/sdcard/Neil_test.h264";
            while (!Thread.interrupted()) {
                try {
                    InputStream ins = new BufferedInputStream(new FileInputStream(h264));
                    int pos = 0;
                    int end = 0;
                    int offset = 0;
                    int last_offset = -1;
                    int i = 0;
                    while (true) {
                        int rv = ins.read(buffer);
                        if (rv <= 0) {
                            break;
                        }
                        if (rv + end < MAX_BUFF_SIZE) {
                            System.arraycopy(buffer, 0, fb, end, rv);
                            end += rv;
                        } else {
                            // TODO: fix
                            end = 0;
                            System.arraycopy(buffer, 0, fb, end, rv);
                            end += rv;
                        }
                        pos = 0;
                        while ((offset = findH264(fb, pos, end)) >= 0) {
                            if (last_offset != -1) {
                                onFrame(fb, last_offset, offset);
                                last_offset = offset;
                                pos = offset + 3;
                            } else {
                                last_offset = offset;
                                pos = offset + 3;
                            }
                        }
                        for (i = 0; last_offset < end; i++, last_offset++) {
                            fb[i] = fb[last_offset + i];
                        }
                        end = i;

                    }
                    ins.close();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    /**
     * 检查指定位置是否是一个h264起始标记
     *
     * @param buff
     * @param offset
     * @param length
     * @return true if a h264 start marker found, otherwise false
     */
    public static boolean checkH264(byte[] buff, int offset, int length) {
        boolean ok = false;
        if (length >= 3 && buff[offset + 0] == 0x00 && buff[offset + 1] == 0x00 && buff[offset + 2] == 0x01) {
            ok = true;
        } else if (length >= 4 && buff[offset + 0] == 0x00 && buff[offset + 1] == 0x00 &&
                buff[offset + 2] == 0x00 && buff[offset + 3] == 0x01) {
            ok = true;
        }
        return ok;

    }

    /**
     * 找到h264头的位置
     *
     * @param buff
     * @param offset
     * @param end
     * @return
     */
    public static int findH264(byte[] buff, int offset, int end) {
        for (int i = 0; i < end; i++) {
            if (checkH264(buff, offset + i, end - offset - i)) {
                return offset + i;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);


        btnStart.setEnabled(false);
        btsHangup.setEnabled(false);
        initNet();
        initSurfaceHolder();

    }

    private void initSurfaceHolder() {
        SurfaceHolder holder = h264sf.getHolder();
        holder.addCallback(this);
        Log.i(TAG, "initSurfaceHolder called");
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

    public void startFFly() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initFFlyParser();
            }
        }).start();
    }


    public void initFFlyParser() {
        DemoSink sink = new DemoSink(this);
        DemoSource source = new DemoSource();
        source.addSink(sink);
        List<Thread> ths = new ArrayList<>(10);
        ths.add(new Thread(source));
        ths.add(new Thread(sink));
        for (Thread th : ths) {
            th.start();
        }
        InputStream in = null;
        int counter = 1;
        try {
            int rv = 0;
            byte[] buff = new byte[4096]; // 4k
            in = new FileInputStream("/sdcard/MediaStarme");
            while (true) {
                rv = in.read(buff);
                if (rv <= 0) {
                    source.stop();
                    System.out.println("Data done , quit");
                    break;

                }
                Log.i(TAG, "send data once");
                source.send(Arrays.copyOfRange(buff, 0, rv));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Thread t : ths) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


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

    public void initNet() {
        // we do it in a thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (initConnection()) {
                    mSendThread = new HandlerThread("send");
                    mSendThread.start();
                    mReceiveThread = new HandlerThread("receive");
                    mReceiveThread.start();
                    mReceiveHandler = new ReceiveHandler(mReceiveThread.getLooper(), mHandleSession);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mHandleSession > 0) {
            mReceiveThread.quit();
            mSendThread.quit();
            PPCS_APIs.PPCS_Close(mHandleSession);
        }
    }

    @OnClick({R.id.h264_test, R.id.starth264, R.id.hang_up, R.id.rw_test1, R.id.MediaStarme})
    public void onViewClick(View view) {
        Log.i(TAG, "OnClick");
        int id = view.getId();
        switch (id) {
            case R.id.rw_test1:
                Log.i(TAG, "xxxxx");
                mSendHandler.sendMessage(mSendHandler.obtainMessage(SendHandler.MSG_SEND_JSON));
                break;
            case R.id.h264_test:
                Log.i(TAG, "h264_test");
                startActivity(new Intent(TestActivity.this, H264Activity.class));
                break;
            case R.id.hang_up:
                Log.i(TAG, "hang_up");
                mSendHandler.sendMessage(mSendHandler.obtainMessage(SendHandler.MSG_SEND_HANG_UP));
            case R.id.starth264:
                Log.i(TAG, "starth264");
                new Thread(mH264RawHandler).run();
                break;
            case R.id.MediaStarme:
                Log.i(TAG, "MediaStarme");
                startFFly();
            default:
                break;

        }
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
