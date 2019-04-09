package com.waterfairy.downloader.upload;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/3/26 14:39
 * @info:
 */


import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;


public class UploadRequestBody extends RequestBody {
    private static final String TAG = "UploadRequestBody";
    private BaseBeanInfo beanInfo;
    private ProgressListener progressListener;
    private RequestBody sourceBody;


    public UploadRequestBody(RequestBody sourceBody, BaseBeanInfo beanInfo, ProgressListener progressListener) {
        this.beanInfo = beanInfo;
        this.progressListener = progressListener;
        this.sourceBody = sourceBody;

    }

    @Override
    public long contentLength() throws IOException {
        return sourceBody.contentLength();
    }

    @Override
    public MediaType contentType() {
        return sourceBody.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
//buffer(sink(com.android.tools.profiler.support.network.HttpTracker$OutputStreamTracker@37365f01))
//buffer(CountingSink(okhttp3.internal.http1.Http1Codec$FixedLengthSink@edef1a6))
        String s = sink.toString();

        if (s.contains("CountingSink")) {
            //读取进度
            //包装
            BufferedSink bufferedSink = Okio.buffer(sink(sink));
            //写入
            sourceBody.writeTo(bufferedSink);
            //必须调用flush，否则最后一部分数据可能不会被写入
            bufferedSink.flush();
        } else {
            sourceBody.writeTo(sink);
        }
    }

    /**
     * 写入，回调进度接口
     *
     * @param sink Sink
     * @return Sink
     */
    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                //增加当前写入的字节数
                bytesWritten += byteCount;
                //回调
                beanInfo.setCurrentLength(bytesWritten);
                beanInfo.setTotalLength(contentLength);
                progressListener.onProgressing(beanInfo, contentLength, bytesWritten);
            }
        };
    }
}

//package com.xueduoduo.wande.evaluation.upload;
//
///**
// * @author water_fairy
// * @email 995637517@qq.com
// * @date 2019/3/26 14:39
// * @info:
// */
//
//
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import com.xueduoduo.wande.evaluation.base.bean.MediaResBean;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okio.BufferedSink;
//
///**
// * Created by water_fiay on 2017/6/26.
// * 995637517@qq.com
// */
//
//public class UploadRequestBody extends RequestBody {
//    private static final String TAG = "uploadRequestBody";
//    private MediaResBean beanInfo;
//    private ProgressListener progressListener;
//    private RequestBody sourceBody;
//
//    private File mFile;
//    private boolean mIsSecond;
//    private final int DEFAULT_BUFFER_SIZE = 2048;
//
//    public void setBeanInfo(MediaResBean beanInfo) {
//        this.beanInfo = beanInfo;
//        mFile = new File(beanInfo.getLocalPath());
//    }
//
//    public UploadRequestBody(RequestBody sourceBody, ProgressListener progressListener) {
//        this.progressListener = progressListener;
//        this.sourceBody = sourceBody;
//
//    }
//
//    @Override
//    public long contentLength() throws IOException {
//        return sourceBody.contentLength();
//    }
//
//    @Override
//    public MediaType contentType() {
//        return sourceBody.contentType();
//    }
//
//
//    @Override
//    public void writeTo(BufferedSink sink) throws IOException {
//        Log.i(TAG, "writeTo: ");
//        long fileLength = mFile.length();
//        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
//        FileInputStream in = new FileInputStream(mFile);
//        long uploaded = 0;
//
//        try {
//            int read;
//            if (!mIsSecond) {
//                if (progressListener!=null)progressListener.onProgressing(beanInfo,fileLength,0);
//            }
//            while ((read = in.read(buffer)) != -1) {
//                uploaded += read;
//                if (!mIsSecond) {
//                    if (progressListener!=null)progressListener.onProgressing(beanInfo,fileLength,uploaded);
//                }
//                sink.write(buffer, 0, read);
//            }
//        } finally {
//            in.close();
//        }
//        mIsSecond = true;
//    }

//}
