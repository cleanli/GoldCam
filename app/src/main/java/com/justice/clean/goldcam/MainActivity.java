package com.justice.clean.goldcam;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v7.appcompat.R.attr.height;
import static android.support.v7.appcompat.R.attr.switchMinWidth;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GOLDCAM";
    private static final int PIC_MODE = 0;
    private static final int VID_MODE = 1;
    private static final int MAX_MODE = 2;
    private int oper_mode = PIC_MODE;
    private int last_mode = MAX_MODE;
    private Camera oldcam = null;
    private SurfaceView sfv;
    private SurfaceHolder sh;
    private TextView tw;
    private Spinner sp;
    private Button ocbt;
    private Button trigger;
    private Button mode_bt;
    private EditText filename;
    private EditText et;
    private EditText max_ct;
    private LinearLayout ll1;
    private LinearLayout ll2;
    private List<Camera.Size> mPicSizes;
    private List<Camera.Size> mPreSizes;
    private List<Camera.Size> mVidSizes;
    private Camera.Size mPreSize;
    private Camera.Size mPicSize;
    private Camera.Size mVidSize;
    private boolean mIsRecordingVideo = false;
    private boolean mIsOpened = false;
    private boolean mIsPreviewing = false;
    private boolean mIsFocused = false;
    private String PicSizesString[];

    private int camID = 0;
    private int last_camID = -1;
    private int pic_cut_index = 0;
    private boolean IsExtraMode = false;
    private String ExtraFilePath = null;
    private File mFile, gFile;
    private String globleFilename;
    private int max_piccount = 0;
    private boolean delete_when_max = false;
    private int piccount = 0;
    private int last_index = 0;
    private int mTmrIntev = 0;
    private boolean mTmrIsrunning = false;
    private Timer mTmr;

    private String mTWstring;
    private int org_sur_w;
    private int org_sur_h;
    private int pre_w;
    private int pre_h;

    private MediaRecorder mMediaRecorder;

    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Handler mHder = new Handler(){
        @Override
        public void handleMessage(Message msg){
            mylog("arg1 "+msg.arg1);
            switch(msg.arg1){
                case 1:
                    set_cam_hint(mTWstring);
                    break;
                case 2:
                    trigger.setText(mTWstring);
                    break;
                case 3:
                    start_rec();
                    break;
                case 4:
                    stop_rec();
                    break;
            }
        }
    };


    private void mylog(String l){
        Log.i(TAG, "|||||||||||||||||||||||||||||||||||||||||||");
        Log.i(TAG, l);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        ll1 = (LinearLayout)findViewById(R.id.ll1);
        ll2 = (LinearLayout)findViewById(R.id.ll2);
        sfv = (SurfaceView)findViewById(R.id.surfaceView);
        tw = (TextView) findViewById(R.id.text);
        sp = (Spinner) findViewById(R.id.spinner);
        ocbt = (Button) findViewById(R.id.button);
        trigger = (Button) findViewById(R.id.trigger);
        mode_bt = (Button) findViewById(R.id.button2);
        ocbt.setText("Open");
        sh = sfv.getHolder();
        //sh.setFixedSize(480,640);
        sh.addCallback(shCB);

        filename = (EditText) findViewById(R.id.name);
        et = (EditText) findViewById(R.id.interv);
        max_ct = (EditText) findViewById(R.id.editText2);
        mFile = new File(this.getExternalFilesDir(null), "pic.jpg");
        gFile = this.getExternalFilesDir(null);

        ExtraFilePath = super.getIntent().getStringExtra("filepath");
        if(ExtraFilePath != null) {
            IsExtraMode = true;
        }

    }

    private boolean hasPermissionsGranted(String[] permissions){
        requestPermissions(permissions, 1);
        for(String pms:permissions){
            if (ActivityCompat.checkSelfPermission(this, pms)
                    != PackageManager.PERMISSION_GRANTED){
                //if (ActivityCompat.checkSelfPermission(CatcamActivity.this, pms)
                //        != PackageManager.PERMISSION_GRANTED){
                return false;
                //}
            }
        }
        return true;
    }

    private void set_cam_hint(String l)
    {
        tw.setVisibility(View.VISIBLE);
        tw.setText(l);
    }

    public boolean checkpm(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (!hasPermissionsGranted(NEEDED_PERMISSIONS)) {
                mylog("no permission, quit");
                set_cam_hint("no permission, open camera failed");
                return false;
            } else {
                mylog("permision granted, continue");
                return true;
            }
        }
        return true;

        /*
        new AlertDialog.Builder(this)
                .setMessage("Need to divide picture?")
                .setCancelable(false)
                .setTitle("Choose")
                .setPositiveButton("4 parts", PicDivideListener)
                .setNegativeButton("2 parts", PicDivideListener)
                .setNeutralButton("No need", PicDivideListener)
                .show();
                */
    }

    public void onModeClk(View v){
        oper_mode++;
        if(oper_mode == MAX_MODE){
            oper_mode = 0;
        }
        switch(oper_mode){
            case PIC_MODE:
                trigger.setText("PIC");
                break;
            case VID_MODE:
                trigger.setText("REC");
                break;
        }
        restart_cam();
    }

    public void on_cam_clk(View v){

        if(Camera.getNumberOfCameras() == 0){
            return;
        }
        camID ++;
        if(camID >= Camera.getNumberOfCameras()){
            camID = 0;
        }
        restart_cam();
    }

    public void on_hint_clk(View v){
        tw.setVisibility(View.INVISIBLE);
    }

    public void on_sur_clk(View v){
        if(oldcam != null && mIsPreviewing){
            set_cam_hint("start focus ...");
            oldcam.autoFocus(myAutoFocusCallback);
        }
    }

    private void do_trigger(){
        switch(oper_mode){
            case PIC_MODE:
                take_pic();
                break;
            case VID_MODE:
                mylog("do recording");
                if(mIsRecordingVideo){
                    stop_rec();
                    trigger.setText("REC");
                }
                else{
                    start_rec();
                    trigger.setText("REC---");
                }
                break;
        }
    }
    private void do_conti_trigger(){
        switch(oper_mode){
            case PIC_MODE:
                take_pic();
                //update_trigger("PIC...");
                break;
            case VID_MODE:
                mylog("do recording");
                if(mIsRecordingVideo){
                    call_rec(false);
                }
                call_rec(true);
                //update_trigger("REC...");
                break;
        }
    }
    private void do_end_trigger(){
        switch(oper_mode){
            case PIC_MODE:
                trigger.setText("PIC");
                break;
            case VID_MODE:
                if(mIsRecordingVideo){
                    stop_rec();
                }
                trigger.setText("REC");
                break;
        }
        mode_bt.setEnabled(true);
    }
    public void onTriggerClk(View v){
        Log.d(TAG, "trigger click button");
        if(!mIsPreviewing && !mIsRecordingVideo){
            set_cam_hint("open camera first");
            return;
        }
        if (mTmrIntev > 0) {
            if (!mTmrIsrunning) {

                mylog("timer " + mTmrIntev);
                mTmr=new Timer();
                mTmr.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        updateHint("timer triggering... ");
                        if(piccount > max_piccount){
                            if(delete_when_max){
                                piccount = 0;
                            }
                            else{
                                updateHint("get to max count, done of " + piccount);
                                return;
                            }

                        }
                        mylog("from timer--------------------------------------------------");
                        do_conti_trigger();
                    }
                }, 0, mTmrIntev * 1000);
                mTmrIsrunning = true;
                switch(oper_mode){
                    case PIC_MODE:
                        trigger.setText("PIC...");
                        break;
                    case VID_MODE:
                        trigger.setText("REC...");
                        break;
                }
                mode_bt.setEnabled(false);

                set_cam_hint("timer is running @ " + mTmrIntev);
            } else {
                mTmr.cancel();
                set_cam_hint("timer canceled");
                mTmrIsrunning = false;

                do_end_trigger();
            }
        } else {
            //runPrecaptureSequence();
            do_trigger();
        }
    }

    public void onButClk(View v){
        Log.d(TAG, "click button");
        if(mIsOpened){
            stopOldPreview();
            closeOldCamera();
            ocbt.setText("Open");
        }
        else{
            switch(oper_mode){
                case PIC_MODE:
                    trigger.setText("PIC");
                    break;
                case VID_MODE:
                    trigger.setText("REC");
                    break;
            }
            mode_bt.setEnabled(true);
            if(!checkpm())
                return;
            openOldCamera();
            startOldPreview();
            ocbt.setText("Close");
        }

    }

    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        public void onAutoFocus(boolean success, Camera camera) {
            // TODO Auto-generated method stub
            if (success)
            {
                mylog("focus callback, success = " + success);
            } else {
                mylog("focus callback, success = " + success);
            }
            set_cam_hint(tw.getText().toString() + "\nfocus success = " + success);
        }
    };

    private SurfaceHolder.Callback shCB = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mylog("surfacecreated mIsopened " + mIsOpened);
            org_sur_w = sfv.getWidth();
            org_sur_h = sfv.getHeight();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mylog("surfacechanged mIsopened " + mIsOpened);
            restart_cam();

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mylog("surfacedestroyed mIsopened " + mIsOpened);
            if(mIsOpened) {
                stopOldPreview();
                closeOldCamera();
                //ocbt.setText("Open");
            }
        }
    };

    private void adjust_sur(){
        final double ASPECT_TOLERANCE = 0.1;
        Camera.Size TSz;
        switch(oper_mode){
            default:
            case PIC_MODE:
                TSz = mPicSize;
                break;
            case VID_MODE:
                TSz = mVidSize;
                break;
        }
        double targetRatio = (double) TSz.width / TSz.height;
        int av_w, av_h;
        mylog("target ratio "+targetRatio);
        mylog("TSz.width / TSz.height " + TSz.width + " " + TSz.height);
        mylog("original surface w x h is " + org_sur_w + " x " + org_sur_h);
        //mylog("preview size is w "  + mPreSize.width + " h "+ mPreSize.height);
        mylog("sur size is w "  + sfv.getWidth() + " h "+ sfv.getHeight());
        mylog("sur r " + sfv.getRight() + " l "+ sfv.getLeft() +
        " b "+ sfv.getBottom() + " t " + sfv.getTop());

        mylog("ll1 ll2 "+ ll1.getHeight() + " " + ll2.getHeight());
        //av_h is w, av_w is h
        av_h = org_sur_w;
        av_w = org_sur_h - ll1.getHeight() - ll2.getHeight();
        mylog("av "+av_w + " av_h "+av_h);

        //sfv.setRight(sfv.getLeft()+mPreSize.height);
        //sfv.setBottom(sfv.getTop()+mPreSize.width);
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (Camera.Size trysize : mPreSizes) {
            mylog("for : try w x h " + trysize.width + " x " + trysize.height);
            double ratio = (double) trysize.width / trysize.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            mylog("ratio meet~~~");
            if (Math.abs(trysize.height - av_h) < minDiff) {
                optimalSize = trysize;
                minDiff = Math.abs(trysize.height - av_h);
                mylog("new min diff "+ minDiff);
            }
        }
        if(optimalSize == null) {//if can't find, find the nearest ratio
            optimalSize = mPreSizes.get(0);
            double lessratio = (double) optimalSize.width / optimalSize.height - targetRatio;
            for (Camera.Size trysize : mPreSizes) {
                mylog("for : try w x h " + trysize.width + " x " + trysize.height);
                //if(trysize.width > av_w || trysize.height > av_h) continue;optimalSize.height * av_w /optimalSize.width
                double ratio = (double) trysize.width / trysize.height;
                if (Math.abs(ratio - targetRatio) < lessratio) {
                    lessratio = Math.abs(ratio - targetRatio);
                    optimalSize = trysize;
                    mylog("new lessratio " + lessratio);
                }
            }
        }
        mylog("final pre size w x h " + optimalSize.width + " x " + optimalSize.height);
        mylog("1 "+(optimalSize.height * av_w /optimalSize.width) + " x " + av_w);
        mylog("2 "+ av_h + " x "+(optimalSize.width * av_h / optimalSize.height));
        if(optimalSize.height * av_w /optimalSize.width < av_h){
            sfv.setLayoutParams(new FrameLayout.LayoutParams(optimalSize.height * av_w /optimalSize.width, av_w));
            mylog("go 1");
            set_cam_hint(tw.getText().toString() +
                    "\nsur go 1 "+(optimalSize.height * av_w /optimalSize.width) + " x " + av_w);
        }
        else {
            sfv.setLayoutParams(new FrameLayout.LayoutParams(av_h, optimalSize.width * av_h / optimalSize.height));
            mylog("go 2");
            set_cam_hint(tw.getText().toString() +
                    "\nsur go 2 "+ av_h + " x "+(optimalSize.width * av_h / optimalSize.height));
        }
        mPreSize = optimalSize;
    }

    /*
        private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    */

    private void restart_cam(){
        if(mIsOpened) {
            stopOldPreview();
            closeOldCamera();
            ocbt.setText("Open");
        }
        if(!mIsOpened) {
            if (!checkpm())
                return;
            openOldCamera();
            startOldPreview();
            ocbt.setText("Close");
        }
    }

    void updateHint(String sc){
        Message msg = new Message();
        mTWstring = sc;
        msg.arg1 = 1;
        mHder.sendMessage(msg);
    }

    void update_trigger(String sc){
        Message msg = new Message();
        mTWstring = sc;
        msg.arg1 = 2;
        mHder.sendMessage(msg);
    }

    void call_rec(boolean s){
        Message msg = new Message();
        if(s) {
            msg.arg1 = 3;
        }
        else
        {
            msg.arg1 = 4;
        }
        mHder.sendMessage(msg);
    }


    class SavePictureTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]...params){
            try{
                if(IsExtraMode) {
                    mFile = new File(ExtraFilePath);
                    FileOutputStream fos = new FileOutputStream(mFile.getPath());
                    fos.write(params[0]);
                    fos.close();
                    oldcam.stopPreview();
                    oldcam.release();
                    setResult(RESULT_OK, getIntent());
                    IsExtraMode = false;
                    finish();
                }
                mFile = new File(gFile, globleFilename + piccount + ".jpg");
                FileOutputStream fos = new FileOutputStream(mFile.getPath());
                fos.write(params[0]);
                fos.close();
                mylog(gFile.getPath());

                updateHint("Saved picture:" + mFile);
                piccount++;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }


    Camera.PictureCallback picOldCB = new Camera.PictureCallback(){
        //@Override
        public void onPictureTaken(byte[] data, Camera cam){
            new SavePictureTask().execute(data);
            oldcam.startPreview();
        }
    };

    Camera.ShutterCallback shutCB = new Camera.ShutterCallback(){
        public void onShutter(){
            set_cam_hint("Shutter received ============================");
        }

    };

    private void take_pic(){
        mylog("take pic in oldcam is "+oldcam);
        if(oldcam != null) {
            oldcam.takePicture(shutCB, null, picOldCB);
        }
    }

    private void start_rec(){
        if(mIsPreviewing){
            stopOldPreview();
            //closeOldCamera();
        }
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        oldcam.unlock();
        mMediaRecorder.setCamera(oldcam);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mFile = new File(gFile, globleFilename + piccount++ + ".mp4");
        mMediaRecorder.setOutputFile(mFile.getPath());
        tw.setText("Video File: "+ mFile.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVidSize.width, mVidSize.height);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setPreviewDisplay(sh.getSurface());
        //int rotation = getWindowManager().getDefaultDisplay().getRotation();
        mMediaRecorder.setOrientationHint(90);
        try {
            mMediaRecorder.prepare();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            mMediaRecorder.start();
            tw.setText(tw.getText().toString() + "\nold video started");
            mIsRecordingVideo = true;
        }catch (Exception e){
            e.printStackTrace();
            tw.setText("start video ERROR, please restart APP");
        }
    }

    private void stop_rec(){
        mMediaRecorder.stop();
        mMediaRecorder.release();
        oldcam.lock();
        //openOldCamera();
        startOldPreview();
        tw.setText("Video stopped");
        mIsRecordingVideo = false;
    }

    private void openOldCamera(){

        sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if(Camera.getNumberOfCameras() == 0){
            mylog("Number of cameras is 0");
            return;
        }

        oldcam = Camera.open(camID);
        set_cam_hint("old camera opened");
        mIsOpened = true;

        Camera.Parameters p = oldcam.getParameters();
        mPicSizes = p.getSupportedPictureSizes();
        mPreSizes = p.getSupportedPreviewSizes();
        mVidSizes = p.getSupportedVideoSizes();
        int i = 0;
        switch(oper_mode){
            case PIC_MODE:
                PicSizesString = new String[mPicSizes.size()];
                i = 0;
                for(Camera.Size tsz:mPicSizes){
                    PicSizesString[i++] = ""+tsz.width+"x"+tsz.height;
                }
                break;
            case VID_MODE:
                PicSizesString = new String[mVidSizes.size()];
                i = 0;
                for(Camera.Size tsz:mVidSizes){
                    PicSizesString[i++] = ""+tsz.width+"x"+tsz.height;
                }
                break;
        }

        //mPicSizes.toArray(PicSizesString);
        if(last_camID != camID || last_mode != oper_mode) {
            ArrayAdapter<String> tmpAd = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, PicSizesString);
            tmpAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(tmpAd);
            last_camID = camID;
            last_mode = oper_mode;
        }
        onset(true);
        trigger.setEnabled(true);

        /*
        int rt4_3_h = sfv.getWidth()/3*4;
        if(rt4_3_h > sfv.getHeight()){
            int tmp = sfv.getHeight()/4*3;
            sfv.setRight(sfv.getLeft()+tmp);
            set_cam_hint("\nadjust right to "+sfv.getRight());
        }
        else{
            set_cam_hint("\nno need to adjust view");
        }
        */

    }
    private void startOldPreview(){
        try{
            oldcam.setPreviewDisplay(sh);
            oldcam.setDisplayOrientation(90);
            Camera.Parameters p = oldcam.getParameters();
            mPicSizes = p.getSupportedPictureSizes();
            adjust_sur();
            p.setPreviewSize(mPreSize.width, mPreSize.height);
            oldcam.setParameters(p);
            oldcam.startPreview();
            set_cam_hint(tw.getText().toString()+"\nold camera preview started, "
                    +mPreSize.width + " x " + mPreSize.height);
            if(oper_mode == PIC_MODE) {
                p = oldcam.getParameters();
                p.setPictureSize(mPicSize.width, mPicSize.height);
                oldcam.setParameters(p);
            }
            mIsPreviewing = true;
            //sfv.setBottom(sfv.getTop()+sfv.getWidth()*4/3);
            //sfv.setTop(300);
            oldcam.autoFocus(myAutoFocusCallback);

        }catch (IOException e){
            oldcam.release();
            oldcam=null;
        }
    }
    private void stopOldPreview(){
        oldcam.stopPreview();
        set_cam_hint("old camera preview stopped");
        mIsPreviewing = false;
    }
    private void closeOldCamera(){
        if(mTmrIsrunning){
            mTmr.cancel();
            set_cam_hint("timer canceled");
            mylog("timer cancelled in stopoldpreview");
            mTmrIsrunning = false;
        }
        oldcam.release();
        oldcam=null;
        set_cam_hint("old camera closed");
        mIsOpened = false;
        onset(false);
        trigger.setEnabled(false);
    }

    public void onset(boolean b){
        if(b) {
            filename.setEnabled(false);
            et.setEnabled(false);
            max_ct.setEnabled(false);
            globleFilename = filename.getText().toString();
            if(globleFilename == null || globleFilename.isEmpty()){
                globleFilename = "default";
            }
            int index = sp.getSelectedItemPosition();
            Camera.Parameters P = oldcam.getParameters();
            mylog("last index & index is " + last_index + " & " + index);
            /*
            if (last_index == index &&
                    gFile.getPath().equals(this.getExternalFilesDir(null) + "/" + filename.getText().toString())) {
                P.set("rotation", pic_rotate);
                tw.setText("set rotate " + pic_rotate);
                pic_rotate += 90;
                if (pic_rotate == 360) {
                    pic_rotate = 0;
                }
            }
            */
            switch (oper_mode){
                case PIC_MODE: {
                    Camera.Size tmpSz = P.getPictureSize();
                    mylog("orignal size is " + tmpSz.width + "x" + tmpSz.height);
                    P.setPictureSize(mPicSizes.get(index).width, mPicSizes.get(index).height);
                    Camera.Size tmpSz2 = P.getPictureSize();
                    mPicSize = tmpSz2;
                    mylog("current size is " + tmpSz2.width + "x" + tmpSz2.height);
                    oldcam.setParameters(P);
                    set_cam_hint(tw.getText().toString() + "\nsize change from " + tmpSz.width + "x" + tmpSz.height
                            + " to " + tmpSz2.width + "x" + tmpSz2.height);
                    last_index = index;
                    break;
                }
                case VID_MODE: {
                    mVidSize = mVidSizes.get(index);
                    Camera.Size tmpSz2 = mVidSize;
                    set_cam_hint(tw.getText().toString() + "\nvideo size "
                            + tmpSz2.width + "x" + tmpSz2.height);
                    last_index = index;
                    break;
                }
            }


            if (!gFile.getPath().equals(this.getExternalFilesDir(null) + "/" + filename.getText().toString())) {
                String tmp_str = filename.getText().toString();
                if(tmp_str == null || tmp_str.isEmpty()){
                    tmp_str = "default";
                }
                File t_gFile = new File(this.getExternalFilesDir(null) + "/" + tmp_str);
                if (!t_gFile.exists()) {
                    t_gFile.mkdirs();
                    gFile = t_gFile;
                    set_cam_hint(tw.getText().toString() + "\npath=" + gFile.getPath());
                } else {
                    if (t_gFile.isDirectory()) {
                        gFile = t_gFile;
                        set_cam_hint(tw.getText().toString() + "\npath=" + gFile.getPath());
                    } else {
                        set_cam_hint(tw.getText().toString() + "\nsame name file exist,path not create");
                    }
                }

            }

            if (et.getText().toString().isEmpty()) {
                mTmrIntev = 0;
            }
            else {
                mTmrIntev = Integer.parseInt(et.getText().toString());
            }
            if (max_ct.getText().toString().isEmpty()) {
                max_piccount = 99999999;
            }
            else {
                max_piccount = Integer.parseInt(max_ct.getText().toString());
            }
            if(max_piccount < 0){
                max_piccount = -max_piccount;
                delete_when_max = true;
            }
            if(max_piccount == 0){
                max_piccount = 99999999;
            }
            sp.setEnabled(false);
        }
        else{
            filename.setEnabled(true);
            et.setEnabled(true);
            max_ct.setEnabled(true);
            sp.setEnabled(true);
        }

    }

    @Override
    protected void onDestroy(){
        if(mTmr!=null) {
            mTmr.cancel();
        }
        super.onDestroy();
    }

}
