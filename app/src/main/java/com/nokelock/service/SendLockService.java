package com.nokelock.service;


import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * 发送验证码
 */
public interface SendLockService {
    @POST("ajaxks")
    @FormUrlEncoded
    Call<String> call(@Field("phoneno") String phoneno,
                      @Field("code") String code,
                      @Field("reason") String reason);
}
