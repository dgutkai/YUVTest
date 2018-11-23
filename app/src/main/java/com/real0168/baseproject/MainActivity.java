package com.real0168.baseproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.real0168.base.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity implements Camera.PreviewCallback {
    private Camera mCamera;
//    private Camera.Parameters parameters;
//    private SurfaceView mSurfaceView;
//    private SurfaceHolder mSurfaceHolder;
    private ImageView imageView;
    private final int WIDHT = 480;
    private final int HEIGHT = 640;
    @Override
    public int getLayoutID() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        if(mCamera==null){
            mCamera=Camera.open(mCameraId);//前置
        }

        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, camInfo);
        int cameraRotationOffset = camInfo.orientation;
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
            cameraRotationOffset += 180;
        int rotate = (360 + cameraRotationOffset - 90) % 360;
        parameters.setRotation(rotate);
        parameters.setPreviewFormat(ImageFormat.NV21);
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        parameters.setPreviewSize(WIDHT, HEIGHT);
        parameters.setPreviewFrameRate(20);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(parameters);
        int displayRotation;
        displayRotation = (cameraRotationOffset - 90 + 360) % 360;

        mCamera.setDisplayOrientation(displayRotation);


//        mSurfaceView=(SurfaceView)findViewById(R.id.id_surface_view_unlock);//获取surfaceView控件
//        mSurfaceHolder=mSurfaceView.getHolder();//获取holder参数
//        mSurfaceHolder.addCallback(new SurfaceHolderCB());//设置holder的回调
//        try {
//            mCamera.setPreviewDisplay(mSurfaceHolder);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        imageView = findViewById(R.id.imageview);
        startPreview();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        byte[] result = Util.rotateNV21Degree90(bytes, previewSize.width, previewSize.height);

        Bitmap bitmap = Bitmap.createBitmap(350,50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String dS = sdf.format(new Date());
        canvas.drawText(dS, 10, 40, paint);
        byte[] bytes1 = getYUVByBitmap(bitmap);
        for (int i = 0; i < 350; i++){
            for (int j = 0; j < 50; j++){
                byte b = bytes1[j*350 + i];
                if (b == 0x10){
                    continue;
                }
                if (result[j*previewSize.height + i] >= 0 && result[j*previewSize.height + i] <= 0x60) {
                    result[j * previewSize.height + i] = (byte) 0xff;
                }else{
                    result[j * previewSize.height + i] = 0;
                }
            }
        }
        for (int i = 0; i < 350; i++){
            for (int j = 0; j < 25; j++){
                byte b = bytes1[j*350 + i + 17500];
                if (b == (byte) 0x80){
                    continue;
                }
                result[j*previewSize.height + i + previewSize.height*previewSize.width] = b;
            }
        }
        YuvImage image = new YuvImage(result,ImageFormat.NV21,previewSize.height,previewSize.width,null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0,0,previewSize.height,previewSize.width),80,stream);
        Bitmap bmp=BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size());
        camera.addCallbackBuffer(bytes);
        imageView.setImageBitmap(bmp);
    }


    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {

            }
            mCamera = null;
        }
    }

    private void startPreview() {
        mCamera.startPreview();//开启预览
        mCamera.cancelAutoFocus();//聚焦

        int previewFormat = mCamera.getParameters().getPreviewFormat();
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        int size = previewSize.width * previewSize.height
                * ImageFormat.getBitsPerPixel(previewFormat)
                / 8;
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.setPreviewCallbackWithBuffer(this);
    }


    /*
     * 获取位图的YUV数据
     */
    public static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // byte[] data = convertColorToByte(pixels);
        byte[] data = rgb2YCbCr420(pixels, width, height);

        return data;
    }

    public static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // 屏蔽ARGB的透明度值
                int rgb = pixels[i * width + j] & 0x00FFFFFF;
                // 像素的颜色顺序为bgr，移位运算。
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                // rgb2yuv
                // y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                // u = (int) (-0.147 * r - 0.289 * g + 0.437 * b);
                // v = (int) (0.615 * r - 0.515 * g - 0.1 * b);
                // RGB转换YCbCr
                // y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                // u = (int) (-0.1687 * r - 0.3313 * g + 0.5 * b + 128);
                // if (u > 255)
                // u = 255;
                // v = (int) (0.5 * r - 0.4187 * g - 0.0813 * b + 128);
                // if (v > 255)
                // v = 255;
                // 调整
                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }
}
