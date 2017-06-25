package com.justice.clean.goldcam;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
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
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import static android.support.v7.appcompat.R.attr.height;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GOLDCAM";
    private Camera oldcam;
    private SurfaceView sfv;
    private SurfaceHolder sh;
    private TextView tw;
    private Spinner sp;
    private Button ocbt;
    private List<Camera.Size> mPicSizes;
    private Camera.Size mPreSize;
    private boolean mIsRecordingVideo;
    private boolean mIsOpened = false;
    private boolean mIsPreviewing = false;
    private boolean mIsFocused = false;
    private String PicSizesString[];

    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
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
        ocbt.setText("Open");
        sh = sfv.getHolder();
        //sh.setFixedSize(480,640);
        sh.addCallback(shCB);
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

    public boolean checkpm(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (!hasPermissionsGranted(NEEDED_PERMISSIONS)) {
                mylog("no permission, quit");
                tw.setText("no permission, open camera failed");
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

    private SurfaceHolder.Callback shCB = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mylog("surfacecreated mIsopened " + mIsOpened);
            if(!mIsOpened) {
                if (!checkpm())
                    return;
                openOldCamera();
                startOldPreview();
                ocbt.setText("Close");
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mylog("surfacedestroyed mIsopened " + mIsOpened);
            if(mIsOpened) {
                stopOldPreview();
                closeOldCamera();
                ocbt.setText("Open");
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


    private void openOldCamera(){
        int camID = 0;

        sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if(Camera.getNumberOfCameras()>1){
            camID = 0;
        }

        oldcam = Camera.open(camID);
        tw.setText("old camera opened");
        mIsOpened = true;

        Camera.Parameters p = oldcam.getParameters();
        mPicSizes = p.getSupportedPictureSizes();

        PicSizesString = new String[mPicSizes.size()];
        int i = 0;
        for(Camera.Size tsz:mPicSizes){
            PicSizesString[i++] = ""+tsz.width+"x"+tsz.height;
        }
        //mPicSizes.toArray(PicSizesString);
        ArrayAdapter<String> tmpAd = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, PicSizesString);
        tmpAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(tmpAd);

        /*
        int rt4_3_h = sfv.getWidth()/3*4;
        if(rt4_3_h > sfv.getHeight()){
            int tmp = sfv.getHeight()/4*3;
            sfv.setRight(sfv.getLeft()+tmp);
            tw.setText("\nadjust right to "+sfv.getRight());
        }
        else{
            tw.setText("\nno need to adjust view");
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
            tw.setText(tw.getText().toString()+"\nold camera preview started");
            mIsPreviewing = true;
            //sfv.setBottom(sfv.getTop()+sfv.getWidth()*4/3);
            //sfv.setTop(300);
        }catch (IOException e){
            oldcam.release();
            oldcam=null;
        }
    }
    private void stopOldPreview(){
        oldcam.stopPreview();
        tw.setText("old camera preview stopped");
        mIsPreviewing = false;
    }
    private void closeOldCamera(){
        oldcam.release();
        oldcam=null;
        tw.setText("old camera closed");
        mIsOpened = false;
    }


}
