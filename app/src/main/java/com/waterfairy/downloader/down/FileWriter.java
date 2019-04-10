package com.waterfairy.downloader.down;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import okhttp3.ResponseBody;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/10 10:04
 * @info:
 */
public class FileWriter {

    private static final String TAG = "fileWrite";

    public ResultBean write(ResponseBody responseBody, String saveFilePath, long currentLength, long totalLength) {
        boolean success = true;
        String msg = "";
        //判断responseBody
        if (success = responseBody != null) {
            if (totalLength == 0) totalLength = responseBody.contentLength();
            if (success = totalLength != 0) {
                //判断 路径为空
                if (success = !TextUtils.isEmpty(saveFilePath)) {
                    File saveFile = new File(saveFilePath);
                    //判断文件存在
                    if (success = createFile(saveFile)) {

                        //创建randomAccessFile
                        RandomAccessFile randomAccessFile = null;
                        MappedByteBuffer mappedByteBuffer = null;
                        FileChannel channel = null;
                        try {
                            randomAccessFile = new RandomAccessFile(saveFile, "rw");
                            channel = randomAccessFile.getChannel();
                            mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, currentLength, totalLength - currentLength);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            success = false;
                            msg = "file not found";
                        } catch (IOException e) {
                            success = false;
                            e.printStackTrace();
                            msg = "file  random write error";
                        }
                        //创建 randomAccessFile 成功
                        if (success) {
                            int length = 0;
                            byte[] readBytes = new byte[1024 * 512];
                            try {
                                long total = 0;
                                while ((length = responseBody.byteStream().read(readBytes)) != -1) {
                                    total++;
                                    mappedByteBuffer.put(readBytes, 0, length);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                success = false;
                                //网络断开 不稳定 手动取消
                                msg = "stream read error";
                            }
                            try {
                                responseBody.byteStream().close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                if (success) {
                                    success = false;
                                    msg = "responseBody stream close error";
                                }
                            }
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                if (success) {
                                    success = false;
                                    msg = "randomAccessFile channel stream close error";
                                }
                            }
                            try {
                                randomAccessFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                if (success) {
                                    success = false;
                                    msg = "randomAccessFile stream close error";
                                }
                            }
                        }
                    } else {
                        msg = "file create failed";
                    }
                } else {
                    msg = "file path is null";
                }
            } else {
                msg = "contentLength = 0";
            }
        } else {
            msg = "responseBody =  null";
        }
        ResultBean resultBean = new ResultBean();
        resultBean.msg = msg;
        resultBean.success = success;
        return resultBean;
    }


    public static class ResultBean {
        private String msg;
        private boolean success;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    /**
     * 创建文件
     *
     * @param file
     * @return
     */
    private boolean createFile(File file) {
        boolean canSave = false;
        if (file.exists()) {
            canSave = true;
        } else {
            if (!file.getParentFile().exists()) {
                canSave = file.getParentFile().mkdirs();
            } else {
                canSave = true;
            }
            if (canSave) {
                try {
                    canSave = file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                    canSave = false;
                }
            }
        }
        return canSave;
    }

}
