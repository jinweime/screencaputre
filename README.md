# screencaputre
android  5.0 the screencaputre



# 问题
在android中有时候我们需要对屏幕进行截屏操作，单一的截屏操作好解决可以通过activity的顶层view
DecorView获取一个bitmap，得到就是当前activity上面的全部视图。

 View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        Bitmap ret = Bitmap
                .createBitmap(bmp, 0, 0, dm.widthPixels, dm.heightPixels);
        view.destroyDrawingCache();

 如果activity中包含一些视频播放器比如SurfaceView GLSurfaceView
 TextureView，在调用截屏代码会发现播放视频的部分是黑屏的，原因是这几种视频渲染的view通过以上代码拿到的是缓冲区不是真正的图像。

# 解决办法
android5.0以上系统提供了一个 MediaProjectionManager
类来对手机进行录屏操作，也支持获取手机的Image图像的操作，知道了这些我们就可以通过提供的api来进行截屏操作了。

 这里通过Service来操作截屏和录屏的api


 # 1.绑定截屏的Service
 Intent intent = new Intent(this, ScreenService.class);
 bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

 public void onServiceConnected(ComponentName className, IBinder service) {
             DisplayMetrics metrics = new DisplayMetrics();
             getWindowManager().getDefaultDisplay().getMetrics(metrics);
             ScreenService.RecordBinder binder = (ScreenService.RecordBinder) service;
             recordService = binder.getRecordService();
             recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
             mButton.setEnabled(true);
             mButton.setText(recordService.isRunning() ? "结束" : "开始");
  }


 # 2.请求权限 onActivityResult 方法中回调。
 Intent captureIntent = projectionManager.createScreenCaptureIntent();
 startActivityForResult(captureIntent, RECORD_REQUEST_CODE);

![](http://or66xwp90.bkt.clouddn.com/device-2017-06-07-155240.png)
  成功后
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {

             //######## 截屏逻辑 ########
             mediaProjection = projectionManager.getMediaProjection(resultCode, data);
             recordService.setMediaProject(mediaProjection);
             recordService.initImageReader();

         }
  }

 # 3. 获取截屏

  @Override
  public void onClick(View view) {

    //########  截屏逻辑 ########
     Bitmap bitmap = recordService.getBitmap();
     mImageView.setImageBitmap(bitmap);
  }

![](http://or66xwp90.bkt.clouddn.com/device-2017-06-07-155424.png)
 # 录屏
 录屏需要初始化一些录屏参数，输入麦克风类型视频类型，保存路径等

 private void initRecorder() {
         mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
         mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         mediaRecorder.setOutputFile(
                 getSavePath() + System.currentTimeMillis() + ".mp4");
         mediaRecorder.setVideoSize(width, height);
         mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
         mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
         mediaRecorder.setVideoFrameRate(30);
         try {
             mediaRecorder.prepare();
         } catch (IOException e) {
             e.printStackTrace();
         }
 }

 # 开始录屏

  mediaRecorder.start();

# 保存路径
![](http://or66xwp90.bkt.clouddn.com/device-2017-06-07-155354.png)
 # 完整Service代码

 public class ScreenService extends Service {
     private MediaRecorder mediaRecorder;
     private VirtualDisplay virtualDisplay;
     private boolean running;
     private int width = 720;
     private int height = 1080;
     private int dpi;
     private ImageReader mImageReader;
     private MediaProjection mediaProjection;


     @Override
     public IBinder onBind(Intent intent) {
         return new RecordBinder();
     }


     @Override
     public void onCreate() {
         super.onCreate();
         running = false;
         mediaRecorder = new MediaRecorder();
     }


     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         return super.onStartCommand(intent, flags, startId);
     }


     @Override
     public void onDestroy() {
         super.onDestroy();
     }


     public void setMediaProject(MediaProjection project) {
         mediaProjection = project;
     }


     public boolean isRunning() {
         return running;
     }


     public void setConfig(int width, int height, int dpi) {
         this.width = width;
         this.height = height;
         this.dpi = dpi;
     }


     /**
      * 开始录屏
      *
      * @return true
      */
     public boolean startRecord() {
         if (mediaProjection == null || running) {
             return false;
         }
         initRecorder();
         createVirtualDisplay();
         mediaRecorder.start();
         running = true;
         return true;
     }


     /**
      * 结束录屏
      *
      * @return true
      */
     public boolean stopRecord() {
         if (!running) {
             return false;
         }
         running = false;
         mediaRecorder.stop();
         mediaRecorder.reset();
         virtualDisplay.release();
         mediaProjection.stop();

         return true;
     }


     public void setMediaProjection(MediaProjection mediaProjection) {
         this.mediaProjection = mediaProjection;
     }


     /**
      * 初始化ImageRead参数
      */
     public void initImageReader() {
         if (mImageReader == null) {
             int maxImages = 2;
             mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, maxImages);
             createImageVirtualDisplay();
         }
     }


     /**
      * 创建一个录屏 Virtual
      */

     private void createVirtualDisplay() {
         virtualDisplay = mediaProjection
                 .createVirtualDisplay("mediaprojection", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder
                         .getSurface(), null, null);
     }


     /**
      * 创建一个ImageReader Virtual
      */
     private void createImageVirtualDisplay() {
         virtualDisplay = mediaProjection
                 .createVirtualDisplay("mediaprojection", width, height, dpi,
                         DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader
                         .getSurface(), null, null);
     }


     /**
      * 初始化保存屏幕录像的参数
      */
     private void initRecorder() {
         mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
         mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         mediaRecorder.setOutputFile(
                 getSavePath() + System.currentTimeMillis() + ".mp4");
         mediaRecorder.setVideoSize(width, height);
         mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
         mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
         mediaRecorder.setVideoFrameRate(30);
         try {
             mediaRecorder.prepare();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }


     /**
      * 获取一个保存屏幕录像的路径
      *
      * @return path
      */
     public String getSavePath() {
         if (Environment.getExternalStorageState()
                        .equals(Environment.MEDIA_MOUNTED)) {
             String rootDir = Environment.getExternalStorageDirectory()
                                         .getAbsolutePath() + "/" +
                     "ScreenRecord" + "/";

             File file = new File(rootDir);
             if (!file.exists()) {
                 if (!file.mkdirs()) {
                     return null;
                 }
             }
             return rootDir;
         } else {
             return null;
         }
     }


     /**
      * 请求完权限后马上获取有可能为null，可以通过判断is null来重复获取。
      */
     public Bitmap getBitmap() {
         Bitmap bitmap = cutoutFrame();
         if (bitmap == null) {
             getBitmap();
         }
         return bitmap;
     }


     /**
      * 通过底层来获取下一帧的图像
      *
      * @return bitmap
      */
     public Bitmap cutoutFrame() {
         Image image = mImageReader.acquireLatestImage();
         if (image == null) {
             return null;
         }
         int width = image.getWidth();
         int height = image.getHeight();
         final Image.Plane[] planes = image.getPlanes();
         final ByteBuffer buffer = planes[0].getBuffer();
         int pixelStride = planes[0].getPixelStride();
         int rowStride = planes[0].getRowStride();
         int rowPadding = rowStride - pixelStride * width;
         Bitmap bitmap = Bitmap.createBitmap(width +
                 rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
         bitmap.copyPixelsFromBuffer(buffer);
         return Bitmap.createBitmap(bitmap, 0, 0, width, height);
     }


     public class RecordBinder extends Binder {
         public ScreenService getRecordService() {
             return ScreenService.this;
         }
     }
