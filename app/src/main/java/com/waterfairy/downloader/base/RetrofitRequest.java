package com.waterfairy.downloader.base;


import com.waterfairy.downloader.down.DownloadService;
import com.waterfairy.downloader.upload.UploadService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Created by water_fairy on 2017/5/18.
 * 995637517@qq.com
 */

public class RetrofitRequest {
    private static RetrofitRequest retrofitRequest;
    private UploadService uploadRetrofitService;
    private DownloadService downloadRetrofitService;
    private String baseUrl;

    private RetrofitRequest() {

    }

    public static RetrofitRequest getInstance() {
        if (retrofitRequest == null) retrofitRequest = new RetrofitRequest();
        return retrofitRequest;
    }

    public void initBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public UploadService getUploadRetrofit() {
        if (uploadRetrofitService == null) {
            uploadRetrofitService = buildClient().create(UploadService.class);
        }
        return uploadRetrofitService;
    }

    public DownloadService getDownloadRetrofit() {
        if (downloadRetrofitService == null) {
            downloadRetrofitService = buildClient().create(DownloadService.class);
        }
        return downloadRetrofitService;
    }

    private Retrofit buildClient() {
        OkHttpClient.Builder okHttpClient = new OkHttpClient().newBuilder();
        OkHttpClient httpClient = okHttpClient.build();
        okHttpClient.connectTimeout(1500, TimeUnit.MILLISECONDS);
        okHttpClient.readTimeout(1500, TimeUnit.MILLISECONDS);
        return new Retrofit.Builder().baseUrl(baseUrl).client(httpClient).build();
    }
}
