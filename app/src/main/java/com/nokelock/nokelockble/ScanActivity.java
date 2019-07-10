package com.nokelock.nokelockble;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nokelock.constant.ExtraConstant;
import com.nokelock.constant.Url;
import com.nokelock.service.GetMacService;
import com.nokelock.utils.ToastUtil;
import com.nokelock.utils.retrofit.MyCallback;
import com.nokelock.utils.retrofit.RetrofitUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import io.reactivex.functions.Consumer;
import retrofit2.Call;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";

    private Button btnScan;
    private int REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        RxPermissions permissions = new RxPermissions(this);
        permissions.setLogging(true);
        permissions.request(Manifest.permission.CAMERA)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            //请求成功
                            Toast.makeText(ScanActivity.this, "camera success", Toast.LENGTH_SHORT).show();
                        } else {
                            //请求失败
                            Toast.makeText(ScanActivity.this, "camera failure" +
                                    "", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnScan = (Button) findViewById(R.id.btn_scan);
        initListener();

    }



    private void initListener() {
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ScanActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE);

            }
        });
    }

    /**
     * 处理二维码扫描结果
     *
     * @param requestCode requestCode
     * @param resultCode  resultCode
     * @param data        data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    Toast.makeText(this, "解析结果:" + result, Toast.LENGTH_LONG).show();
                    startMain(result);
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(ScanActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }

    }


    private void startMain(final String result) {
        GetMacService request = RetrofitUtils
                .getRetrofit(Url.GET_MAC)
                .create(GetMacService.class);
        Call<String> call = request.call(result);
        call.enqueue(new MyCallback<String>() {
            @Override
            public void onSuc(Response<String> response) {
                Log.i("sss", "onSuc-->: " + response.code());
                String macAddress = response.body();
                ToastUtil.showShortToast("发送成功");
                Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                intent.putExtra(ExtraConstant.MAC_ADDRESS, macAddress);
                intent.putExtra(ExtraConstant.NAME, result);
                startActivity(intent);
            }

            @Override
            public void onFail(String message) {
                Log.i("eee", "onFail: " + message);
                ToastUtil.showShortToast("二维码发送失败");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }

}
