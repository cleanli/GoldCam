package com.justice.clean.goldcam;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.support.v7.appcompat.R.attr.height;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GOLDCAM";
    private Camera oldcam = null;
    private SurfaceView sfv;
    private SurfaceHolder sh;
    private TextView tw;
    private Spinner sp;
    private Button ocbt;
    private Button trigger;
    private EditText filename;
    private EditText et;
    private EditText max_ct;
    private List<Camera.Size> mPicSizes;
    private Camera.Size mPreSize;
    private boolean mIsRecordingVideo;
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

    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Handler mHder = new Handler(){
        @Override
        public void handleMessage(Message msg){
            set_cam_hint(mTWstring);
        }
    };


    private void mylog(String l){
        Log.i(TAG, "|||||||||||||||||||||||||||||||||||||||||||");
        Log.i(TAG, l);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        sfv = (SurfaceView)findViewById(R.id.surfaceView);
        tw = (TextView) findViewById(R.id.text);
        sp = (Spinner) findViewById(R.id.spinner);
        ocbt = (Button) findViewById(R.id.button);
        trigger = (Button) findViewById(R.id.trigger);
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

    public void onTriggerClk(View v){
        Log.d(TAG, "pic click button");
        if(!mIsPreviewing){
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
                        take_pic();
                    }
                }, 0, mTmrIntev * 1000);
                mTmrIsrunning = true;
                trigger.setText("PIC...");
                set_cam_hint("timer is running @ " + mTmrIntev);
            } else {
                mTmr.cancel();
                set_cam_hint("timer canceled");
                mTmrIsrunning = false;
                trigger.setText("PIC");
            }
        } else {
            //runPrecaptureSequence();
            take_pic();
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
        mylog("preview size is w "  + mPreSize.width + " h "+ mPreSize.height);
        mylog("sur size is w "  + sfv.getWidth() + " h "+ sfv.getHeight());
        mylog("sur r " + sfv.getRight() + " l "+ sfv.getLeft() +
        " b "+ sfv.getBottom() + " t " + sfv.getTop());
        //sfv.setRight(sfv.getLeft()+mPreSize.height);
        //sfv.setBottom(sfv.getTop()+mPreSize.width);
        mylog("sur r " + sfv.getRight() + " l "+ sfv.getLeft() +
                " b "+ sfv.getBottom() + " t " + sfv.getTop());
        sfv.setLayoutParams(new FrameLayout.LayoutParams(480, 640));//
    }

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
        mTWstring = sc;
        mHder.sendMessage(new Message());
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

        PicSizesString = new String[mPicSizes.size()];
        int i = 0;
        for(Camera.Size tsz:mPicSizes){
            PicSizesString[i++] = ""+tsz.width+"x"+tsz.height;
        }
        //mPicSizes.toArray(PicSizesString);
        if(last_camID != camID) {
            ArrayAdapter<String> tmpAd = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, PicSizesString);
            tmpAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(tmpAd);
            last_camID = camID;
        }

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
            p.setPreviewSize(640,480);
            oldcam.setParameters(p);
            p = oldcam.getParameters();
            mPreSize = p.getPreviewSize();
            adjust_sur();
            oldcam.startPreview();
            set_cam_hint(tw.getText().toString()+"\nold camera preview started");
            mIsPreviewing = true;
            //sfv.setBottom(sfv.getTop()+sfv.getWidth()*4/3);
            //sfv.setTop(300);
            oldcam.autoFocus(myAutoFocusCallback);

            onset(true);
        }catch (IOException e){
            oldcam.release();
            oldcam=null;
        }
    }
    private void stopOldPreview(){
        if(mTmrIsrunning){
            mTmr.cancel();
            set_cam_hint("timer canceled");
            mTmrIsrunning = false;
        }
        oldcam.stopPreview();
        set_cam_hint("old camera preview stopped");
        mIsPreviewing = false;
        onset(false);
    }
    private void closeOldCamera(){
        oldcam.release();
        oldcam=null;
        set_cam_hint("old camera closed");
        mIsOpened = false;
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
            Camera.Size tmpSz = P.getPictureSize();
            mylog("orignal size is " + tmpSz.width + "x" + tmpSz.height);
            P.setPictureSize(mPicSizes.get(index).width, mPicSizes.get(index).height);
            Camera.Size tmpSz2 = P.getPictureSize();
            mylog("current size is " + tmpSz2.width + "x" + tmpSz2.height);
            oldcam.setParameters(P);
            set_cam_hint(tw.getText().toString() + "\nsize change from " + tmpSz.width + "x" + tmpSz.height
                    + " to " + tmpSz2.width + "x" + tmpSz2.height);
            last_index = index;

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
