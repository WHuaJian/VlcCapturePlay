package com.whj.vlc.player;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.whj.vlc.player.utils.ScreenCapture;
import com.whj.vlc.player.utils.VlcUIManager;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/6/4 下午1:45
 */

public class VlcPlayActivity extends AppCompatActivity implements IVLCVout.OnNewVideoLayoutListener, View.OnClickListener {
    private static final boolean ENABLE_SUBTITLES = true;
    private static final String TAG = VlcPlayActivity.class.getSimpleName();
    public static final String URL = "vlc_url";
    private static String SAMPLE_URL = "rtsp://192.168.1.10:554/main";
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_SCREEN = 1;
    private static final int SURFACE_FILL = 2;
    private static final int SURFACE_16_9 = 3;
    private static final int SURFACE_4_3 = 4;
    private static final int SURFACE_ORIGINAL = 5;
    private static int CURRENT_SIZE = SURFACE_BEST_FIT;

    private FrameLayout mVideoSurfaceFrame = null;
    private SurfaceView mVideoSurface = null;
    private SurfaceView mSubtitlesSurface = null;

    private final Handler mHandler = new Handler();
    private View.OnLayoutChangeListener mOnLayoutChangeListener = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;

    private ImageView refresh, capture;
    private Rect markedArea = new Rect();
    private LinearLayout ll_tool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VlcUIManager.getAppManager().addActivity(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setContentView(R.layout.activity_vlc_play);

        SAMPLE_URL = getIntent().getExtras().getString(URL);

        initVlcPlayer();
        mVideoSurfaceFrame = (FrameLayout) findViewById(R.id.video_surface_frame);
        mVideoSurface = (SurfaceView) findViewById(R.id.video_surface);
        refresh = (ImageView) findViewById(R.id.refresh);
        capture = (ImageView) findViewById(R.id.capture);
        ll_tool = (LinearLayout) findViewById(R.id.ll_tool);
        refresh.setOnClickListener(this);
        capture.setOnClickListener(this);
        if (ENABLE_SUBTITLES) {
            final ViewStub stub = (ViewStub) findViewById(R.id.subtitles_stub);
            mSubtitlesSurface = (SurfaceView) stub.inflate();
            mSubtitlesSurface.setZOrderMediaOverlay(true);
            mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }

        createMediaProject();
        mVideoSurface.getWindowVisibleDisplayFrame(markedArea);

    }

    private void initVlcPlayer() {
        ArrayList<String> options = new ArrayList<>();
//        options.add("--aout=opensles");
//        options.add("--audio-time-stretch");
//        options.add("-vvv");

        options.add("--aout=opensles");
        options.add("--audio-time-stretch");
        options.add("--audio-resampler");
        options.add("soxr");
        options.add("--avcodec-skiploopfilter");
        options.add("1");
        options.add("--avcodec-skip-frame");
        options.add("0");
        options.add("--avcodec-skip-idct");
        options.add("0");
        options.add("--udp-timeout=2");
//        options.add("500");
        // deinterlace and deinterlace-mode, see https://wiki.videolan.org/Deinterlacing/#VLC_deinterlace_modes
        options.add("--deinterlace");
        options.add("1");
        options.add("--deinterlace-mode");
        options.add("bob");

        options.add("--udp-caching=100");
//        options.add("500");

        options.add(":rtsp-udp");
        options.add(":demux=h264");
        options.add("-vvv");

        mLibVLC = new LibVLC(this, options);
        mMediaPlayer = new MediaPlayer(mLibVLC);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (screenCaptureService != null)
            screenCaptureService.onDestroy();

        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }

        mMediaPlayer.release();
        mLibVLC.release();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        VlcUIManager.getAppManager().finishActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);
        if (mSubtitlesSurface != null)
            vlcVout.setSubtitlesView(mSubtitlesSurface);
        vlcVout.attachViews(this);

        Media media = new Media(mLibVLC, Uri.parse(SAMPLE_URL));
        int cache = 256;
        media.addOption(":sout-udp-caching=" + 100);
        media.addOption(":network-caching=" + 64);
        media.addOption(":file-caching=" + 3000);
        media.addOption(":live-caching=" + 64);
        media.addOption(":sout-mux-caching=" + 3000);
        media.addOption(":codec=mediacodec,iomx,all");

        //        options.add(":file-caching=6000");//文件缓存
//        options.add(":network-caching=6000");//网络缓存
//        options.add(":live-caching=6000");//直播缓存
//        options.add(":sout-mux-caching=6000");//输出缓存
//        options.add(":codec=mediacodec,iomx,all");
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();

        if (mOnLayoutChangeListener == null) {
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                private final Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVideoSurfaces();
                    }
                };

                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.post(mRunnable);
                    }
                }
            };
        }
        mVideoSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mOnLayoutChangeListener != null) {
            mVideoSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
            mOnLayoutChangeListener = null;
        }

        mMediaPlayer.stop();

        mMediaPlayer.getVLCVout().detachViews();
    }

    private void changeMediaPlayerLayout(int displayW, int displayH) {
        /* Change the video placement using the MediaPlayer API */
        switch (CURRENT_SIZE) {
            case SURFACE_BEST_FIT:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_FIT_SCREEN:
            case SURFACE_FILL: {
                Media.VideoTrack vtrack = mMediaPlayer.getCurrentVideoTrack();
                if (vtrack == null)
                    return;
                final boolean videoSwapped = vtrack.orientation == Media.VideoTrack.Orientation.LeftBottom
                        || vtrack.orientation == Media.VideoTrack.Orientation.RightTop;
                if (CURRENT_SIZE == SURFACE_FIT_SCREEN) {
                    int videoW = vtrack.width;
                    int videoH = vtrack.height;

                    if (videoSwapped) {
                        int swap = videoW;
                        videoW = videoH;
                        videoH = swap;
                    }
                    if (vtrack.sarNum != vtrack.sarDen)
                        videoW = videoW * vtrack.sarNum / vtrack.sarDen;

                    float ar = videoW / (float) videoH;
                    float dar = displayW / (float) displayH;

                    float scale;
                    if (dar >= ar)
                        scale = displayW / (float) videoW; /* horizontal */
                    else
                        scale = displayH / (float) videoH; /* vertical */
                    mMediaPlayer.setScale(scale);
                    mMediaPlayer.setAspectRatio(null);
                } else {
                    mMediaPlayer.setScale(0);
                    mMediaPlayer.setAspectRatio(!videoSwapped ? "" + displayW + ":" + displayH
                            : "" + displayH + ":" + displayW);
                }
                break;
            }
            case SURFACE_16_9:
                mMediaPlayer.setAspectRatio("16:9");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_4_3:
                mMediaPlayer.setAspectRatio("4:3");
                mMediaPlayer.setScale(0);
                break;
            case SURFACE_ORIGINAL:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(1);
                break;
        }
    }

    private void updateVideoSurfaces() {
        int sw = getWindow().getDecorView().getWidth();
        int sh = getWindow().getDecorView().getHeight();

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        mMediaPlayer.getVLCVout().setWindowSize(sw, sh);

        ViewGroup.LayoutParams lp = mVideoSurface.getLayoutParams();
        if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoSurface.setLayoutParams(lp);
            lp = mVideoSurfaceFrame.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoSurfaceFrame.setLayoutParams(lp);
            changeMediaPlayerLayout(sw, sh);
            return;
        }

        if (lp.width == lp.height && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            mMediaPlayer.setAspectRatio(null);
            mMediaPlayer.setScale(0);
        }

        double dw = sw, dh = sh;
        final boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mVideoSarDen == mVideoSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double) mVideoSarNum / mVideoSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        switch (CURRENT_SIZE) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_SCREEN:
                if (dar >= ar)
                    dh = dw / ar; /* horizontal */
                else
                    dw = dh * ar; /* vertical */
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        // set display size
        lp.width = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        mVideoSurface.setLayoutParams(lp);
        if (mSubtitlesSurface != null)
            mSubtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = mVideoSurfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        mVideoSurfaceFrame.setLayoutParams(lp);

        mVideoSurface.invalidate();
        if (mSubtitlesSurface != null)
            mSubtitlesSurface.invalidate();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
        updateVideoSurfaces();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    private void createMediaProject() {
        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    private Bitmap bitmap;
    private int REQUEST_MEDIA_PROJECTION = 1;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenCapture screenCaptureService;

    private void startScreenCapture(Intent intent, int resultCode) {
        screenCaptureService = new ScreenCapture(this, intent, resultCode, markedArea);
        screenCaptureService.setCaptureListener(new ScreenCapture.ICaptureListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onError(String message) {
            }

            @Override
            public void onComplete(Bitmap b) {
                bitmap = b;
                showCaptureImage(b);
            }
        });
        try {
            screenCaptureService.toCapture();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            } else if (data != null && resultCode != 0) {
                startScreenCapture(data, resultCode);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startIntent() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.refresh) {
            onStop();
            onStart();
        } else if (v.getId() == R.id.capture) {
            ll_tool.setVisibility(View.GONE);
            startIntent();
        }
    }

    /**
     * 显示截图
     * */
    private void showCaptureImage(final Bitmap bitmap){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_capture_preview_layout, null);
        TextView btn_cancel = view.findViewById(R.id.btn_cancel);
        TextView btn_save = view.findViewById(R.id.btn_save);

        ImageView preview_capture = view.findViewById(R.id.preview_capture);
        preview_capture.setImageBitmap(bitmap);

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        WindowManager m = this.getWindowManager();
        final Display d = m.getDefaultDisplay();
        android.view.WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.height = (int) (d.getHeight() * 0.5);
        p.width = (int) (d.getWidth() * 0.5);
        dialog.getWindow().setAttributes(p);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ll_tool.setVisibility(View.VISIBLE);
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenCaptureService.saveCutBitmap(bitmap);
                dialog.dismiss();
                ll_tool.setVisibility(View.VISIBLE);
            }
        });
    }

}
