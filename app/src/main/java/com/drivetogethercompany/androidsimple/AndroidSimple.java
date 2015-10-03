package com.drivetogethercompany.androidsimple;

import android.app.FragmentManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AndroidSimple {

    private static Context con = null;
    private static FragmentManager fm = null;
    private static View v = null;
    private static FailedAndroidSimpleAsync failedasync = null;

    public static void setContext(Context con) {
        AndroidSimple.con = con;
    }

    public static void setFragmentManager(FragmentManager fm) {
        AndroidSimple.fm = fm;
    }

    public static void setView(View v) {
        AndroidSimple.v = v;
    }

    public AndroidSimple(Context applicationContext, FragmentManager fragmentManager, View view) {
        failedasync = new FailedAndroidSimpleAsync();
        con = applicationContext;
        fm = fragmentManager;
        v = view;
    }

    public AndroidSimple() {
    }

    public void executeAllFailedRequest() {
        if (failedasync != null)
            failedasync.executeAll();
    }

    public abstract static class Callback {
        public abstract void onEvent(String ret, Context c, FragmentManager fm, View v);
    }

    public static abstract class CallbackProgress {
        public abstract void onProgress(float progress);
    }

    public static abstract class CallbackTime {
        public abstract void onTime();
    }

    public static abstract class CallbackFileProgress {
        public abstract void onProgress(int percent, String file);
    }

    public class Callbacks<T> {
        private Callback cancelled = null;
        private Callback postExecute = null;
        private CallbackFileProgress progressUpdate = null;
        private Callback failed = null;
        private Callback preExecute = null;

        public T setOnPreExecuteCallback(Callback pre) {
            preExecute = pre;
            return (T) this;
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

        public T setOnProgressUpdateCallback(CallbackFileProgress c) {
            this.progressUpdate = c;
            return (T) this;
        }

        public Callback getCancelledCallback() {
            return cancelled;
        }

        public CallbackFileProgress getProgressUpdateCallback() {
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

    public class Upload extends Callbacks<Upload> {
        private String url = null;
        private String path = null;
        private String addPOST = null;
        private UploadAsynctask ua = null;
        private String[] u;
        private boolean retry_if_no_internet;

        public Upload(String url, String path, String addPOST) {
            this.url = url;
            this.path = path;
            this.addPOST = addPOST;
        }

        public Upload setRetryOnNoInternet(boolean val) {
            retry_if_no_internet = val;
            return (this);
        }

        public Upload setTimeOut(long time, TimeUnit unit) {
            if (ua != null) {
                try {
                    ua.get(time, unit);
                } catch (Exception ex) {
                    Log.e("Error", "Can't set The timeout: " + ex);
                }
            }
            return this;
        }

        public Upload execute() {
            u = new String[2];
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

        private class UploadAsynctask extends AsyncTask<String, Integer, Void> {
            private String ret = "";
            private boolean error = false;

            @Override
            protected void onPreExecute() {
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
                        publishProgress((int) ((count / fileSize) * 100.0f));
                        dos.write(buffer, 0, bufferSize);
                        count += bufferSize;
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    publishProgress((int) ((count / fileSize) * 100f));
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

            @Override
            protected void onProgressUpdate(Integer... progress) {
                if (getProgressUpdateCallback() != null)
                    getProgressUpdateCallback().onProgress(progress[0], path);
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
                if (error) {
                    onFailed();
                    return;
                }
                if (getPostExecuteCallback() != null)
                    getPostExecuteCallback().onEvent(ret, con, fm, v);
            }

            protected void onFailed() {
                if (retry_if_no_internet) {
                    FailedAndroidSimpleAsync.add((AsyncTask) new UploadAsynctask(), u);
                }
                if (getFailedCallback() != null)
                    getFailedCallback().onEvent(null, con, fm, v);
            }
        }
    }

    public Upload upload(String url, String path, String post) {
        return new Upload(url, path, post);
    }


    public class Download extends Callbacks<Download> {
        private String dst;
        private String url;
        private boolean retry_if_no_internet = false;
        private DownloadAsynctask ua = null;
        private String[] u;

        public Download(String url, String destination) {
            this.url = url;
            dst = destination;
        }

        public Download setRetryOnNoInternet(boolean val) {
            retry_if_no_internet = val;
            return (this);
        }

        public boolean execute() {
            u = new String[2];
            u[0] = url;
            u[1] = dst;
            DownloadAsynctask ua = new DownloadAsynctask();
            if (ua.getStatus() != AsyncTask.Status.RUNNING) {
                ua.execute(u);
                return true;
            } else {
                Log.d("download asynktask", "already stared !");
            }
            return false;
        }

        public Download setTimeOut(long time, TimeUnit unit) {
            if (ua != null) {
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

        private class DownloadAsynctask extends AsyncTask<String, Integer, Void> {
            private boolean error;
            private int lastProgress = -1;

            @Override
            protected void onPreExecute() {
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
                InputStream input;
                OutputStream output;
                HttpURLConnection connection;
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
                        count += ret;
                        if (isCancelled()) {
                            input.close();
                            Log.i("Info", "Download canceled !");
                            error = true;
                            return null;
                        }
                        if (total > 0)
                            publishProgress((int) ((count / (float) total) * 100));
                        output.write(data, 0, ret);
                    }
                } catch (Exception e) {
                    error = true;
                    Log.e("e", "error = " + e);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... percent) {
                if (percent[0] == lastProgress)
                    return ;
                if (getProgressUpdateCallback() != null) {
                    getProgressUpdateCallback().onProgress(percent[0], url);
                    lastProgress = percent[0];
                }
            }

            @Override
            protected void onPostExecute(Void voi) {
                if (error) {
                    onFailed();
                    return;
                }
                if (getPostExecuteCallback() != null)
                    getPostExecuteCallback().onEvent(dst, con, fm, v);
            }

            protected void onFailed() {
                if (retry_if_no_internet) {
                    FailedAndroidSimpleAsync.add(new DownloadAsynctask(), u);
                }
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


    public class Request extends Callbacks<Request> {
        private String phpfile = null;
        private String POST = null;
        private requestAsyncTask ua = null;
        private boolean retry_if_no_internet = false;
        private String[] u = new String[2];

        public Request(String phpfile, String POST) {
            this.POST = POST;
            this.phpfile = phpfile;
        }

        public Request setRetryOnNoInternet(boolean val) {
            retry_if_no_internet = val;
            return (this);
        }

        public boolean execute() {
            u[0] = phpfile;
            u[1] = POST;
            ua = new requestAsyncTask();
            if (ua.getStatus() != AsyncTask.Status.RUNNING) {
                ua.execute(u);
                return true;
            }
            return false;
        }

        public Request setTimeOut(long time, TimeUnit unit) {
            if (ua != null) {
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

            public requestAsyncTask() {
            }

            @Override
            protected void onPreExecute() {
                if (getPreExecuteCallback() != null)
                    getPreExecuteCallback().onEvent(null, con, fm, v);
            }

            @Override
            protected Void doInBackground(String... params) {
                String urlphp = params[0];
                String data = (params[1] == null) ? "" : params[1];

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
                        Log.e("e", "HTTP Request fail:" + ex);
                    }
                } catch (Exception ex) {
                    Log.e("e", "Http client failed to init: " + ex);
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
                if (retry_if_no_internet) {
                    FailedAndroidSimpleAsync.add((AsyncTask) new requestAsyncTask(), u);
                }
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

    public static class FailedAndroidSimpleAsync {
        public FailedAndroidSimpleAsync() {
        }

        static List<Request> lst = new ArrayList<>();

        public void executeAll() {
            if (lst != null && lst.size() > 0) {
                for (Request r : lst)
                    r.asy.execute(r.para);
                lst.clear();
            }
        }

        private static class Request {
            public AsyncTask<String, Void, Void> asy;
            public String[] para;

            Request(AsyncTask<String, Void, Void> a, String[] p) {
                this.asy = a;
                this.para = p;
            }
        }

        public static void add(AsyncTask a, String[] data) {
            lst.add(new Request(a, data));
        }
    }

    public static class interval {
        public Handler handler = new Handler();
        public Runnable run;
        private boolean running = false;
        private int milis;
        private CallbackTime ct;

        public boolean isrunning() {
            return running;
        }

        public interval start() {
            running = true;
            run.run();
            return this;
        }

        public interval stop() {
            running = false;
            handler.removeCallbacks(run);
            return this;
        }

        public interval(final CallbackTime c, final int mil) {
            this.milis = mil;
            ct = c;

            run = new Runnable() {
                @Override
                public void run() {
                    c.onTime();
                    if (handler != null)
                        handler.postDelayed(run, milis);
                }
            };
        }

        public void restart() {
            running = true;
            handler = new Handler();
            run = new Runnable() {
                @Override
                public void run() {
                    ct.onTime();
                    if (handler != null)
                        handler.postDelayed(run, milis);
                }
            };
            run.run();
        }
    }

    public interval setInterval(CallbackTime callback, int i) {
        return new interval(callback, i);
    }

    public static class timeout {
        public Handler handler = new Handler();
        public Runnable run;

        public timeout(final CallbackTime c, final int milis) {
            run = new Runnable() {
                @Override
                public void run() {
                    c.onTime();
                }
            };
            handler.postDelayed(run, milis);
        }
    }

    public timeout setTimeout(CallbackTime c, int i) {
        return new timeout(c, i);
    }


    public static class intervalProgress {
        public Handler handler = new Handler();
        public Runnable run;
        private float progress;
        public boolean running = false;

        public intervalProgress start() {
            running = true;
            run.run();
            return this;
        }

        public intervalProgress stop() {
            running = false;
            handler.removeCallbacks(run);
            return this;
        }

        public intervalProgress(final CallbackProgress c, final int milis, final int maxInMilis) {
            run = new Runnable() {
                @Override
                public void run() {
                    c.onProgress((progress / (float) maxInMilis) * 100f);
                    if (handler != null)
                        handler.postDelayed(run, milis);
                    progress += milis;
                }
            };
        }
    }

    public intervalProgress setIntervalProgress(CallbackProgress callback, int i, int max) {
        return new intervalProgress(callback, i, max);
    }

    public class Record {

        private MediaRecorder rec;
        private String outFile;
        private boolean isRecording;
        private int maxRecTime;
        private CallbackTime c;
        private intervalProgress progress;
        private Handler recordHandler = new Handler();
        private Runnable stopRecord = new Runnable() {
            @Override
            public void run() {
                stopRecord();
            }
        };

        public Record(String file, int maxTime) {
            isRecording = false;
            rec = new MediaRecorder();
            outFile = file;
            maxRecTime = maxTime;
        }

        public Record start() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    rec.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                        @Override
                        public void onInfo(MediaRecorder mr, int what, int extra) {
                            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                                Log.d("d", "time out ! stopping mediRecorder ...");
                                stop();
                            }
                        }
                    });
                    rec.setAudioSource(MediaRecorder.AudioSource.MIC);
                    rec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    rec.setOutputFile(outFile);
                    rec.setAudioEncoder(MediaRecorder.OutputFormat.THREE_GPP);
                    rec.setMaxDuration(maxRecTime);
                    try {
                        rec.prepare();
                        rec.start();
                        isRecording = true;
                        System.out.println("record begin, output file = " + outFile);

                        if (progress != null) {
                            progress.start();
                        }
                    } catch (Exception e) {
                        System.out.println("record reparation failed !" + e);
                    }
                }
            }).start();
            return this;
        }

        public Record setOnProgressCallback(CallbackProgress c, int milis) {
            progress = setIntervalProgress(c, milis, maxRecTime);
            return this;
        }

        private void stopRecord() {
            if (progress != null && progress.running)
                progress.stop();
            if (!isRecording)
                return ;
            isRecording = false;
            rec.stop();
            rec.release();
            c.onTime();
        }

        public Record stop() {
            recordHandler.postDelayed(stopRecord, 500);
            return this;
        }

        public boolean isRecording() {
            return isRecording;
        }

        public Record setOnEndCallback(CallbackTime c) {
            this.c = c;
            return this;
        }

        public void cancel() {
            if (progress != null && progress.running)
                progress.stop();
            if (!isRecording)
                return;
            try {
                rec.stop();
                isRecording = false;
                rec.release();
            } catch (Exception e) {
                Log.e("Recorder", "failed to stop");
            }
        }
    }

    public Record record(String file, int maxTime) {
        return new Record(file, maxTime);
    }

    private static List<String> playList = new ArrayList<>();

    public class Player {
        private MediaPlayer mp;
        private boolean duplicate = false;
        private CallbackTime ct = null;
        private CallbackProgress cp = null;
        private intervalProgress inter = null;
        private Handler pauseHandler = new Handler();
        private boolean pauseState = false;
        private Runnable pause = new Runnable() {
            @Override
            public void run() {
                pauseState = false;
                playAll();
            }
        };

        private void initMediaPlayer() {
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    onMediaPlayerEnd();
                    return false;
                }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer a) {
                    onMediaPlayerEnd();
                }
            });
        }

        private void onMediaPlayerEnd() {
            if (ct != null)
                ct.onTime();
            if (cp != null && inter != null)
                inter.stop();
            System.out.println("Media finish play song, removing " + playList.get(0) + " from the list");
            playList.remove(0);
            System.out.println("Stay to play: " + playList);
            mp.stop();
            mp.reset();
            mp.release();
            initMediaPlayer();
            Log.d("d", "wait for playing ...");
            for (String m : playList)
                Log.d("music", m);
            if (playList.size() > 0) {
                pauseState = true;
                playAll();
                //pauseHandler.postDelayed(pause, 500);
            }
        }

        private void playAll() {
            if (playList.size() <= 0)
                return ;
            System.out.println("Playing song " + playList.get(0));
            try {
                if (cp != null)
                    inter = setIntervalProgress(cp, 100, mp.getDuration());
                mp.setDataSource(playList.get(0));
                mp.prepare();
                mp.start();
            } catch (IOException e) {
                System.out.println("error = " + e);
            }
        }

        public Player() {
            initMediaPlayer();
        }

        public boolean addToPlay(String path) {
            if (!duplicate && playList.contains(path))
                return false;
            playList.add(path);
            if (playList.size() == 1)
                playAll();
            return true;
        }

        public void playOnly(String path) {
            MediaPlayer m = new MediaPlayer();
            m.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                m.setDataSource(path);
                m.prepare();
                m.start();
            } catch (IOException e) {
                Log.e("mediaplayer", "failed to prepare: " + e);
            }
        }

        public void setDuplicateFilePlay(boolean b) {
            duplicate = b;
        }

        public void setProgressCallback(CallbackProgress c) {
            cp = c;
        }

        public void onPlayEnd(CallbackTime ct) {
            this.ct = ct;
        }
    }

    public Player player() {
        return new Player();
    }
}

