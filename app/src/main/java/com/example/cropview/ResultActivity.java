package com.example.cropview;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ImageView;

import com.example.cropview.util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultActivity extends Activity {

    private static final String TAG = ResultActivity.class.getSimpleName();
    private ImageView mImageView;
    private ExecutorService mExecutor;

    public static Intent createIntent(Activity activity, Uri uri) {
        Intent intent = new Intent(activity, ResultActivity.class);
        intent.setData(uri);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getActionBar().setHomeAsUpIndicator(R.drawable.ic_done_black_24dp);
        setContentView(R.layout.activity_result);
        mImageView = findViewById(R.id.result_image);
        mExecutor = Executors.newSingleThreadExecutor();

        final Uri uri = getIntent().getData();
        mExecutor.submit(new LoadScaledImageTask(this, uri, mImageView, calcImageSize()));
    }

    @Override protected void onDestroy() {
        mExecutor.shutdown();
        super.onDestroy();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private int calcImageSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        return Math.min(Math.max(metrics.widthPixels, metrics.heightPixels), 2048);
    }

    public static class LoadScaledImageTask implements Runnable {
        private Handler mHandler = new Handler(Looper.getMainLooper());
        Context context;
        Uri uri;
        ImageView imageView;
        int width;

        public LoadScaledImageTask(Context context, Uri uri, ImageView imageView, int width) {
            this.context = context;
            this.uri = uri;
            this.imageView = imageView;
            this.width = width;
        }

        @Override public void run() {
            final int exifRotation = Utils.getExifOrientation(context, uri);
            int maxSize = Utils.getMaxSize();
            int requestSize = Math.min(width, maxSize);
            try {
                final Bitmap sampledBitmap = Utils.decodeSampledBitmapFromUri(context, uri, requestSize);
                mHandler.post(new Runnable() {
                    @Override public void run() {
                        imageView.setImageMatrix(Utils.getMatrixFromExifOrientation(exifRotation));
                        imageView.setImageBitmap(sampledBitmap);
                    }
                });
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
