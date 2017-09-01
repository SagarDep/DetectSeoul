package ga.twpooi.detectseoul;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.yongchun.library.view.ImageSelectorActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ga.twpooi.detectseoul.activity.CameraActivity;
import ga.twpooi.detectseoul.activity.DetailActivity;
import ga.twpooi.detectseoul.util.OnDetecterListener;

public class StartActivity extends BaseActivity implements OnDetecterListener{

    private MyHandler handler = new MyHandler();
    private final int MSG_MESSAGE_SHOW_PROGRESS = 500;
    private final int MSG_MESSAGE_HIDE_PROGRESS = 501;

    private Detecter detecter;
    private Executor executor = Executors.newSingleThreadExecutor();

    private ImageView backgroundImg;
    private TextView tv_title;
    private TextView tv_sub_title;
    private Button searchBtn, detectBtn;

    private MaterialDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        init();

    }

    private void init(){

        backgroundImg = (ImageView)findViewById(R.id.background_img);
        tv_title = (TextView)findViewById(R.id.tv_title);
        tv_sub_title = (TextView)findViewById(R.id.tv_sub_title);
        searchBtn = (Button)findViewById(R.id.search_btn);
        detectBtn = (Button)findViewById(R.id.detect_btn);

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(StartActivity.this, MainActivity.class);
//                startActivity(intent);
                showDetectDialog();
            }
        });

        detecter = new Detecter(getApplicationContext(), this);

        progressDialog = new MaterialDialog.Builder(this)
                .content("잠시만 기다려주세요.")
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .theme(Theme.LIGHT)
                .build();

    }

    public void showDetectDialog(){
        String[] list = {
                "Gallery",
                "URL",
                "Camera"
        };
        new MaterialDialog.Builder(this)
                .title("선택")
                .items(list)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which){
                            case 0: // Gallery
                                ImageSelectorActivity.start(StartActivity.this, 1, ImageSelectorActivity.MODE_SINGLE, false,false,false);
                                break;
                            case 1: // URL
                                loadImgFromURL();
                                break;
                            case 2: { // Camera
                                Intent intent = new Intent(StartActivity.this, CameraActivity.class);
                                startActivity(intent);
                                break;
                            }
                        }
                    }
                })
                .show();
    }

    public void loadImgFromURL(){

        new MaterialDialog.Builder(StartActivity.this)
                .title("입력")
                .inputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PERSON_NAME |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .theme(Theme.LIGHT)
                .positiveText("확인")
                .negativeText("취소")
                .input("사진의 URL 주소를 입력해주세요.", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        final String url = input.toString();

                        handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_SHOW_PROGRESS));

                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
//                                    String url = editURL.getText().toString();
                                    InputStream input = new java.net.URL(url).openStream();
                                    //InputStream input = new java.net.URL(editURL.getText().toString()).openConnection().getInputStream();

                                    final Bitmap bitmap =  BitmapFactory.decodeStream(input);
                                    // recognize_bitmap needs to update the UI(imgResult, txtResult), so invoke it in runOnUiThread
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //
                                            detecter.recognize_bitmap(bitmap);
                                        }
                                    });


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                    }
                })
                .show();

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
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // pass the selected image from image picker to tensorflow
        // image picker returns image(s) in arrayList

        if(resultCode == RESULT_OK && requestCode == ImageSelectorActivity.REQUEST_IMAGE){

            handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_SHOW_PROGRESS));

            new Thread() {
                @Override
                public void run() {

                    ArrayList<String> images = (ArrayList<String>) data.getSerializableExtra(ImageSelectorActivity.REQUEST_OUTPUT);

                    // image decoded to bitmap, which can be recognized by tensorflow
                    Bitmap bitmap = BitmapFactory.decodeFile(images.get(0));

                    detecter.recognize_bitmap(bitmap);

                }
            }.start();

        }
    }

    @Override
    public void onDetectFinish(List<Classifier.Recognition> results) {
//        showSnackbar(results.toString());
        handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_HIDE_PROGRESS));
        ArrayList<Classifier.Recognition> list = new ArrayList<>();
        list.addAll(results);
        Intent intent = new Intent(StartActivity.this, DetailActivity.class);
        intent.putExtra("data", list);
        startActivity(intent);
    }
}
