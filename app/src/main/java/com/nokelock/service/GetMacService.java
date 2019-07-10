package com.nokelock.service;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * 发送验证码
 */
public interface GetMacService {
    @GET("code/{id}")
    Call<String> call(@Path("id") String id);
}
