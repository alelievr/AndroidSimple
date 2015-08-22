package com.drivetogethercompany.androidsimple;

import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class AndroidSimple {

    private static Context con = null;
    private static FragmentManager fm = null;
    private static View v = null;

    public AndroidSimple(Context applicationContext, FragmentManager fragmentManager, View view) {
        con = applicationContext;
        fm = fragmentManager;
        v = view;
    }

    public AndroidSimple() {
    }

    private static class callback {

        public void onEvent(String ret, Context c, FragmentManager fm, View v) {
            //base: nothing !
        }
    }

    public static class Callback extends callback {
        public Callback() {
        }
    }

    public class Callbacks<T> {

        private Callback cancelled = null;
        private Callback postExecute = null;
        private Callback progressUpdate = null;
        private Callback failed = null;
        private Callback preExecute = null;

        public T setOnPreExecuteCallback(Callback pre) {
            preExecute = pre;
            return (T)this;
        }

        public T setOnFailedCallback(Callback failed) {
            this.failed = failed;
            return (T) this;
        }

        public T setOnCancelledCallback(Callback c) {
            this.cancelled = c;
            return (T) this;
        }

        public T setOnPostExecuteCallback(Callback c) {
            this.postExecute = c;
            return (T) this;
        }

        public T setOnProgressUpdateCallback(Callback c) {
            this.progressUpdate = c;
            return (T) this;
        }

        public Callback getCancelledCallback() {
            return cancelled;
        }

        public Callback getProgressUpdateCallback() {
            return progressUpdate;
        }

        public Callback getPostExecuteCallback() {
            return postExecute;
        }

        public Callback getFailedCallback() {
            return failed;
        }

        public Callback getPreExecuteCallback() {
            return preExecute;
        }
    }

    public static class Convert {
        public static String streamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }

    protected class Upload extends Callbacks<Upload> {
        private String url = null;
        private String path = null;
        private String addPOST = null;
        private UploadAsynctask ua = null;

        public Upload(String url, String path, String addPOST) {
            this.url = url;
            this.path = path;
            this.addPOST = addPOST;
        }

        public Upload setTimeOut(long time, TimeUnit unit)
        {
            if (ua != null)
            {
                try {
                    ua.get(time, unit);
                } catch (Exception ex) {
                    Log.e("Error", "Can't set The timeout: " + ex);
                }
            }
            return this;
        }

        public Upload execute() {
            String[] u = new String[2];
            u[0] = path;
            u[1] = url;
            ua = new UploadAsynctask();
            if (ua.getStatus() != AsyncTask.Status.RUNNING)
                ua.execute(u);
            else
                Log.e("Error", "AsyncTask is already Running !");
            return this;
        }

        public boolean cancel(boolean threadInterupt) {
            if (ua != null && ua.getStatus() == AsyncTask.Status.RUNNING)
                ua.cancel(threadInterupt);
            else
                return false;
            return true;
        }

        private class UploadAsynctask extends AsyncTask<String, Void, Void> {
            private String ret;
            private boolean error = false;

            @Override
            protected void onPreExecute()
            {
                if (getPreExecuteCallback() != null)
                    getPreExecuteCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected Void doInBackground(String... params) {

                String file = params[0];
                String urlString = params[1];

                HttpURLConnection conn = null;
                DataOutputStream dos;
                DataInputStream inStream;
                String lineEnd = "\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1048576;
                try {
                    File f = new File(file);
                    int fileSize = (int) f.length();
                    FileInputStream fileInputStream = new FileInputStream(f);
                    URL url = new URL(urlString);

                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + file + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    int count = 0;
                    while (bytesRead > 0) {
                        onProgressUpdate((int) ((count / fileSize) * 100.0f));
                        dos.write(buffer, 0, bufferSize);
                        count += bufferSize;
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                } catch (IOException ioe) {
                    Log.e("Debug", "error: " + ioe.getMessage(), ioe);
                    error = true;
                }
                try {
                    assert conn != null;
                    inStream = new DataInputStream(conn.getInputStream());
                    String str;

                    while ((str = inStream.readLine()) != null) {
                        ret += str;
                    }
                    inStream.close();

                } catch (IOException ioex) {
                    error = true;
                    Log.e("Debug", "error: " + ioex.getMessage(), ioex);
                }
                return null;
            }

            protected void onProgressUpdate(Integer progress) {
                if (getProgressUpdateCallback() != null)
                    getProgressUpdateCallback().onEvent(progress.toString(), con, fm, v);
            }

            @Override
            protected void onCancelled() {
                if (getCancelledCallback() != null) {
                    getCancelledCallback().onEvent(null, con, fm, v);
                }
                super.onCancelled();
            }

            @Override
            protected void onPostExecute(Void useless) {
                if (ret == null || error) {
                    onFailed();
                    return;
                }
                if (getPostExecuteCallback() != null)
                    getCancelledCallback().onEvent(ret, con, fm, v);
            }

            protected void onFailed() {
                if (getFailedCallback() != null)
                    getFailedCallback().onEvent(null, con, fm, v);
            }
        }
    }

    public Upload upload(String url, String path, String post) {
        return new Upload(url, path, post);
    }


    public class Download extends Callbacks<Download> {
        private String ret;
        private String dst;
        private String url;
        private DownloadAsynctask ua = null;

        public Download(String url, String destination) {
            this.url = url;
            dst = destination;
        }

        public boolean execute() {
            String[] u = new String[2];
            u[0] = url;
            u[1] = dst;
            DownloadAsynctask ua = new DownloadAsynctask();
            if (ua.getStatus() != AsyncTask.Status.RUNNING) {
                ua.execute(u);
                return true;
            }
            return false;
        }

        public Download setTimeOut(long time, TimeUnit unit)
        {
            if (ua != null)
            {
                try {
                    ua.get(time, unit);
                } catch (Exception ex) {
                    Log.e("Error", "Can't set The timeout: " + ex);
                }
            }
            return this;
        }

        public boolean cancel(boolean threadInterupt) {
            if (ua != null && ua.getStatus() == AsyncTask.Status.RUNNING)
                ua.cancel(threadInterupt);
            else
                return false;
            return true;
        }

        private class DownloadAsynctask extends AsyncTask<String, Void, Void> {
            private boolean error;

            @Override
            protected void onPreExecute()
            {
                if (getPreExecuteCallback() != null)
                    getPreExecuteCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected Void doInBackground(String... info) {
                error = false;
                String fileurl = info[0];
                String dst = info[1];
                if (new File(dst).isDirectory())
                    dst += "/" + new File(fileurl).getName();
                String file = new File(info[1]).getName();
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(fileurl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e("Error", "can't connet to the specified URL !");
                        error = true;
                        return null;
                    }

                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream(dst);
                    int total = connection.getContentLength();

                    byte data[] = new byte[4096];
                    int count = 0;
                    int ret;
                    while ((ret = input.read(data)) != -1) {
                        onProgressUpdate((int) ((count / (float) total) * 100));
                        count += ret;
                        if (isCancelled()) {
                            input.close();
                            Log.i("Info", "Download canceled !");
                            error = true;
                            return null;
                        }
                        output.write(data, 0, ret);
                    }
                } catch (Exception e) {
                    error = true;
                    System.out.println("error = " + e);
                } finally {
                    error = true;
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                        return null;
                    }

                    if (connection != null)
                        connection.disconnect();
                }
                return null;
            }

            protected void onProgressUpdate(Integer percent) {
                if (getProgressUpdateCallback() != null) {
                    getProgressUpdateCallback().onEvent(percent.toString(), con, fm, v);
                }
            }

            @Override
            protected void onPostExecute(Void voi) {
                if (error) {
                    onFailed();
                    return;
                }
                if (getPostExecuteCallback() != null)
                    getPostExecuteCallback().onEvent(ret, con, fm, v);
            }

            protected void onFailed() {
                if (getFailedCallback() != null)
                    getFailedCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected void onCancelled() {
                if (getCancelledCallback() != null) {
                    getCancelledCallback().onEvent(null, con, fm, v);
                }
                super.onCancelled();
            }
        }
    }

    public Download download(String url, String dst) {
        return new Download(url, dst);
    }


    private class Request extends Callbacks<Request> {
        private String phpfile = null;
        private String POST = null;
        private requestAsyncTask ua = null;

        public Request(String phpfile, String POST) {
            this.POST = POST;
            this.phpfile = phpfile;
        }

        public boolean execute() {
            String[] u = new String[2];
            u[0] = phpfile;
            u[1] = POST;
            requestAsyncTask ua = new requestAsyncTask();
            if (ua.getStatus() != AsyncTask.Status.RUNNING) {
                ua.execute(u);
                return true;
            }
            return false;
        }

        public Request setTimeOut(long time, TimeUnit unit)
        {
            if (ua != null)
            {
                try {
                    ua.get(time, unit);
                } catch (Exception ex) {
                    Log.e("Error", "Can't set The timeout: " + ex);
                }
            }
            return this;
        }

        public boolean cancel(boolean threadInterupt) {
            if (ua != null && ua.getStatus() == AsyncTask.Status.RUNNING)
                ua.cancel(threadInterupt);
            else
                return false;
            return true;
        }

        private class requestAsyncTask extends AsyncTask<String, Void, Void> {
            private String ret;

            @Override
            protected void onPreExecute()
            {
                if (getPreExecuteCallback() != null)
                    getPreExecuteCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected Void doInBackground(String... params) {
                String urlphp = params[0];
                String data = params[1];

                System.out.println("url = " + urlphp);
                try {
                    URL url = new URL(urlphp);

                    try {
                        URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                        wr.write(data);
                        wr.flush();

                        try {
                            InputStream in = new BufferedInputStream(conn.getInputStream());
                            ret = Convert.streamToString(in);
                        } catch (Exception ex) {
                            Log.e("Error", "Can't get the server response: " + ex);
                        }

                    } catch (Exception ex) {
                        System.out.println("HTTP Request fail:" + ex);
                    }
                } catch (Exception ex) {
                    System.out.println("Http client failed to init: " + ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void voi) {
                if (ret == null) {
                    onFailed();
                    return;
                }
                if (getPostExecuteCallback() != null)
                    getPostExecuteCallback().onEvent(ret, con, fm, v);
            }

            protected void onFailed() {
                if (getFailedCallback() != null)
                    getFailedCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected void onCancelled() {
                if (getCancelledCallback() != null) {
                    getCancelledCallback().onEvent(null, con, fm, v);
                }
                super.onCancelled();
            }
        }
    }

    public Request request(String url, String POSTargs) {
        return new Request(url, POSTargs);
    }
}
