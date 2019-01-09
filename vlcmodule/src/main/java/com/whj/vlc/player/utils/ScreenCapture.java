package com.whj.vlc.player.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.whj.vlc.player.VlcPlayActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/9 下午7:17
 */

public class ScreenCapture {

    private static final String TAG = "ScreenCaptureActivity";
    public static final String MESSAGE = "message";
    public static final String FILE_NAME = "temp_file";
    private SimpleDateFormat dateFormat = null;
    private String strDate = null;
    private String pathImage = null;
    private String nameImage = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;

    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager1 = null;

    private WindowManager mWindowManager1 = null;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    private int mScreenDensity = 0;
    private int mScreenWidth = 0;

    Handler handler = new Handler(Looper.getMainLooper());
    private Rect mRect;
    private VlcPlayActivity activity;


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public ScreenCapture(VlcPlayActivity activity, Intent intent, int resultCode, Rect mRect) {
        this.activity = activity;
        mResultData = intent;
        mResultCode = resultCode;
        this.mRect = mRect;
        this.mScreenWidth = ScreenUtils.getScreenWidth(activity);
        try {
            createVirtualEnvironment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void toCapture() {
        if (captureListener != null) {
            captureListener.onStart();
        }
        try {
            handler.postDelayed(new Runnable() {
                public void run() {
                    startVirtual();
                }
            }, 10);

            handler.postDelayed(new Runnable() {
                public void run() {
                    //capture the screen
                    try {
                        startCapture();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 100);
        } catch (Exception e) {
            e.printStackTrace();
            if (captureListener != null) {
                captureListener.onError(e.getMessage());
            }
        } catch (Error e) {
            e.printStackTrace();
            if (captureListener != null) {
                captureListener.onError(e.getMessage());
            }
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createVirtualEnvironment() {
        dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        strDate = dateFormat.format(new java.util.Date());
        pathImage = Environment.getExternalStorageDirectory().getPath() + "/Pictures/";
        nameImage = pathImage + strDate + ".png";
        mMediaProjectionManager1 = (MediaProjectionManager) activity.getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mWindowManager1 = (WindowManager) activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = ScreenUtils.getScreenWidth(activity);
        windowHeight = ScreenUtils.getScreenHeight(activity);
        metrics = new DisplayMetrics();
        mWindowManager1.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay();
        } else {
            setUpMediaProjection();

            virtualDisplay();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpMediaProjection() {
        try {
            mMediaProjection = mMediaProjectionManager1.getMediaProjection(mResultCode, mResultData);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay() {
        try {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startCapture() throws Exception {
        strDate = dateFormat.format(new java.util.Date());
        nameImage = pathImage + strDate + ".png";

        Image image = mImageReader.acquireLatestImage();

        if (image == null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    toCapture();
                }
            });
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        if (width != mScreenWidth || rowPadding != 0) {
            int[] pixel = new int[width + rowPadding / pixelStride];
            bitmap.getPixels(pixel, 0, width + rowPadding / pixelStride, 0, 0, width + rowPadding / pixelStride, 1);
            int leftPadding = 0;
            int rightPadding = width + rowPadding / pixelStride;
            for (int i = 0; i < pixel.length; i++) {
                if (pixel[i] != 0) {
                    leftPadding = i;
                    break;
                }
            }
            for (int i = pixel.length - 1; i >= 0; i--) {
                if (pixel[i] != 0) {
                    rightPadding = i;
                    break;
                }
            }
            width = Math.min(width, mScreenWidth);
            if (rightPadding - leftPadding > width) {
                rightPadding = width;
            }
            bitmap = Bitmap.createBitmap(bitmap, leftPadding, 0, rightPadding - leftPadding, height);
        }

        if (mRect != null) {

            if (mRect.left < 0)
                mRect.left = 0;
            if (mRect.right < 0)
                mRect.right = 0;
            if (mRect.top < 0)
                mRect.top = 0;
            if (mRect.bottom < 0)
                mRect.bottom = 0;
            int cut_width = Math.abs(mRect.left - mRect.right);
            int cut_height = Math.abs(mRect.top - mRect.bottom);
            if (cut_width > 0 && cut_height > 0) {
                Bitmap cutBitmap = Bitmap.createBitmap(bitmap, mRect.left, mRect.top, cut_width, cut_height);
//                saveCutBitmap(cutBitmap);
                if (captureListener != null) {
                    captureListener.onComplete(cutBitmap);
                }
            }

        } else {
//            saveCutBitmap(bitmap);
            if (captureListener != null) {
                captureListener.onComplete(bitmap);
            }
        }


        bitmap.recycle();//自由选择是否进行回收
    }

    public void saveCutBitmap(final Bitmap cutBitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                String name = System.currentTimeMillis() + ".png";
                File localFile = new File(path + name);
                final String fileName = localFile.getAbsolutePath();
                try {
                    File file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    if (!localFile.exists()) {
                        localFile.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(localFile);
                    if (fileOutputStream != null) {
                        cutBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    if (captureListener != null) {
                        captureListener.onError(e.getMessage());
                    }
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        galleryAddPic(fileName);
                        Toast.makeText(activity, "保存成功：" + fileName, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();

    }

    public void galleryAddPic(String resultPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(resultPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    public void onDestroy() {
        stopVirtual();
        tearDownMediaProjection();
    }

    public interface ICaptureListener {
        void onStart();

        void onError(String message);

        void onComplete(Bitmap path);
    }

    public ICaptureListener captureListener;

    public void setCaptureListener(ICaptureListener captureListener) {
        this.captureListener = captureListener;
    }
}
