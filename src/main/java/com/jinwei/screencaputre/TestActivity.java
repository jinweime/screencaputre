package com.jinwei.screencaputre;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.jinwei.screencaputre.activity.CapoutActivity;
import com.jinwei.screencaputre.cutout.ScreenService;

/**
 * 项目名称：
 * 类描述：
 * 创建人：JinWei
 * 创建时间：2017/6/5
 * 修改人：
 * 修改时间：2017/6/5
 * 修改备注：
 */

public class TestActivity extends Activity {
    Button mButton, mCapButton;
    ImageView mImageView;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ScreenService recordService;
    private static int RECORD_REQUEST_CODE = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintest);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mButton = (Button) findViewById(R.id.butview);
        mCapButton = (Button) findViewById(R.id.capout);
        mImageView = (ImageView) findViewById(R.id.img);
        Intent intent = new Intent(this, ScreenService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //######## 录屏逻辑 ########
                if (recordService.isRunning()) {
                    recordService.stopRecord();
                    mButton.setText("录屏");
                } else {
                    //这里是请求录屏权限
                    Intent captureIntent = projectionManager
                            .createScreenCaptureIntent();
                    startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                }
            }
        });
        mCapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(TestActivity.this, CapoutActivity.class);
                startActivity(in);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            //######## 录屏逻辑 ########
            mediaProjection = projectionManager
                    .getMediaProjection(resultCode, data);
            recordService.setMediaProject(mediaProjection);
            recordService.startRecord();
            mButton.setText("结束");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            ScreenService.RecordBinder binder = (ScreenService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService
                    .setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            mButton.setEnabled(true);
            mButton.setText(recordService.isRunning() ? "结束" : "开始");
        }


        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
}
