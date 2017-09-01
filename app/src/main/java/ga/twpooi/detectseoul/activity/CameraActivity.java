package ga.twpooi.detectseoul.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

import ga.twpooi.detectseoul.BaseActivity;
import ga.twpooi.detectseoul.Classifier;
import ga.twpooi.detectseoul.Detecter;
import ga.twpooi.detectseoul.R;
import ga.twpooi.detectseoul.StartActivity;
import ga.twpooi.detectseoul.TensorFlowImageClassifier;
import ga.twpooi.detectseoul.util.OnDetecterListener;

public class CameraActivity extends BaseActivity implements OnDetecterListener{

    private MyHandler handler = new MyHandler();
    private final int MSG_MESSAGE_SHOW_PROGRESS = 500;
    private final int MSG_MESSAGE_HIDE_PROGRESS = 501;

    private Detecter detecter;

    private CameraView cameraView;
    private Button btnDetect;

    private MaterialDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();

    }

    private void init(){

        detecter = new Detecter(getApplicationContext(), this);

        progressDialog = new MaterialDialog.Builder(this)
                .content("잠시만 기다려주세요.")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .theme(Theme.LIGHT)
                .build();

        cameraView = (CameraView)findViewById(R.id.cameraView);
        btnDetect = (Button)findViewById(R.id.btnDetect);

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.captureImage();
            }
        });

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

        // cameraview library has its own permission check method
        cameraView.setPermissions(CameraKit.Constants.PERMISSIONS_PICTURE);

        // invoke tensorflow inference when picture taken from camera
        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(final byte[] picture) {
                super.onPictureTaken(picture);

                handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_SHOW_PROGRESS));

                new Thread(){
                    @Override
                    public void run(){

                        Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                        detecter.recognize_bitmap(bitmap);

                    }
                }.start();

            }
        });

        cameraView.start();

    }

    private class MyHandler extends Handler {

        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_MESSAGE_SHOW_PROGRESS:
                    progressDialog.show();
                    break;
                case MSG_MESSAGE_HIDE_PROGRESS:
                    progressDialog.hide();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.stop();
        detecter.onDestroy();
    }

    @Override
    public void onDetectFinish(List<Classifier.Recognition> results) {
//        showSnackbar(results.toString());
        handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_HIDE_PROGRESS));
        ArrayList<Classifier.Recognition> list = new ArrayList<>();
        list.addAll(results);
        Intent intent = new Intent(CameraActivity.this, DetailActivity.class);
        intent.putExtra("data", list);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
