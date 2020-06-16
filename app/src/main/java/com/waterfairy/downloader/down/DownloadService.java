package com.waterfairy.downloader.down;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by water_fairy on 2017/4/26.
 */

public interface DownloadService {
    @Streaming
    @GET
    Call<ResponseBody> download(@Header("RANGE") String start, @Url String url);

    @Streaming
    @POST
    Call<ResponseBody> downloadPost(@Header("RANGE") String start, @Url String url);
}
