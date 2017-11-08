package com.wzf.imagepicker.ImageLoader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.wzf.imagepicker.MyApplication;
import com.wzf.imagepicker.utils.ScreenUtils;

public class ImageLoader {
    private String TAG = ImageLoader.class.getSimpleName();
    /**
     * 网络图片本地缓存库
     */
    private DiskLruCache mDiskLruCache;
    private static final int DISK_CACHE_INDEX = 0;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 500;//500M
    private boolean mIsDiskLruCacheCreated = false;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    /**
     * 图片缓存的核心类
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    /**
     * 线程池的线程数量，默认为1
     */
    private int mThreadCount = 1;
    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTasks;
    /**
     * 轮询的线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHander;

    /**
     * 运行在UI线程的handler，用于给ImageView设置图片
     */
    private Handler mHandler;

    /**
     * 引入一个值为1的信号量，防止mPoolThreadHander未初始化完成
     */
    private volatile Semaphore mSemaphore = new Semaphore(0);

    /**
     * 引入一个值为1的信号量，由于线程池内部也有一个阻塞线程，防止加入任务的速度过快，使LIFO效果不明显
     */
    private volatile Semaphore mPoolSemaphore;

    private static ImageLoader mInstance;

    /**
     * 队列的调度方式
     *
     * @author zhy
     *
     */
    public enum Type
    {
        FIFO,
        LIFO
    }


    /**
     * 单例获得该实例对象
     *
     * @return
     */
    public static ImageLoader getInstance()
    {

        if (mInstance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new ImageLoader(1, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    private ImageLoader(int threadCount, Type type)
    {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type)
    {
        // loop thread
        mPoolThread = new Thread()
        {
            @Override
            public void run()
            {
                Looper.prepare();

                mPoolThreadHander = new Handler()
                {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        mThreadPool.execute(getTask());
                        try
                        {
                            mPoolSemaphore.acquire();
                        } catch (InterruptedException e)
                        {
                        }
                    }
                };
                // 释放一个信号量
                mSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap value)
            {
                return value.getRowBytes() * value.getHeight();
            };
        };

        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mPoolSemaphore = new Semaphore(threadCount);
        mTasks = new LinkedList<Runnable>();
        mType = type == null ? Type.LIFO : type;

        File diskCacheDir = getDiskCacheDir(MyApplication.getAppInstance(), "bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getUsableSpace(diskCacheDir) > DISK_CACHE_INDEX){
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取指定路径的可用空间大小
     * @param diskCacheDir
     * @return
     */
    private long getUsableSpace(File diskCacheDir) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return diskCacheDir.getUsableSpace();
        }
        final StatFs statFs = new StatFs(diskCacheDir.getPath());
        return statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
    }

    /**
     * 获取硬盘存储路径
     * @param mContext
     * @param bitmap
     * @return
     */
    private File getDiskCacheDir(Context mContext, String bitmap) {
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if(externalStorageAvailable){
            if(mContext.getExternalCacheDir() != null){
                cachePath = mContext.getExternalCacheDir().getPath();
            }else {
                cachePath = mContext.getCacheDir().getPath();
            }
        }else {
            cachePath = mContext.getCacheDir().getPath();
        }
        return  new File(cachePath + File.separator +bitmap);
    }

    /**
     * 加载图片
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView)
    {
        if(imageView == null || TextUtils.isEmpty(path)){
            return;
        }
        // set tag
        imageView.setTag(path);
        // UI线程
        if (mHandler == null)
        {
            mHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    ImageView imageView = holder.imageView;
                    Bitmap bm = holder.bitmap;
                    String path = holder.path;
                    if (imageView.getTag().toString().equals(path))
                    {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null)
        {
            ImgBeanHolder holder = new ImgBeanHolder();
            holder.bitmap = bm;
            holder.imageView = imageView;
            holder.path = path;
            Message message = Message.obtain();
            message.obj = holder;
            mHandler.sendMessage(message);
        }else {
            if(path.startsWith("http")){
                addNetImageTask(path, imageView);
            }else if(path.startsWith("/")){
                addLocalImageTask(path, imageView);
            }
        }
    }

    /**
     * 添加加载网络图片任务
     * @param url
     * @param imageView
     */
    private void addNetImageTask(final String url, final ImageView imageView) {
        addTask(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadNetImage(url, getImageViewWidth(imageView));
                if(bitmap != null){
                    ImgBeanHolder holder = new ImgBeanHolder();
                    holder.bitmap = bitmap;
                    holder.imageView = imageView;
                    holder.path = url;
                    Message message = Message.obtain();
                    message.obj = holder;
                    mHandler.sendMessage(message);
                }
                mPoolSemaphore.release();
            }
        });
    }

    /**
     * 加载网络图片
     * @param url
     * @param size
     * @return
     */
    private Bitmap loadNetImage(String url, ImageSize size) {
        Bitmap bitmap = null;
        try {
            bitmap = loadBitmapFromDiskCache(url, size.width, size.height); // 再从硬盘寻找
            if(bitmap != null){
                Log.i(TAG, "loadBitmapFromDiskCache,url:" + url);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url, size.width, size.height);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(bitmap == null && !mIsDiskLruCacheCreated){
            Log.i(TAG, "DiskLurCache id not Created"); // 无法初始化磁盘缓存cache
            bitmap = downloadBitmapFromUrl(url);
        }

        return bitmap;
    }

    /**
     * 从网络加载图片
     * @param urlString
     * @return
     */
    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }


    /**
     * 从网络加载图片到硬盘,并且读取出来
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     * @throws Exception
     */
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight)
            throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI Thread.");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        Log.d(TAG, "loadBitmapFromHttp,url:" + url);
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    /**
     * 将url转化为流
     * @param urlString
     * @param outputStream
     * @return
     */
    public boolean downloadUrlToStream(String urlString,
                                       OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        Log.e(TAG, "下载到缓存地址：" + urlString);
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(),
                    IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "downloadBitmap failed." + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if(out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }



    /**
     * 从硬盘拿图片资源
     * @param url
     * @return
     */
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws Exception {
        if(Looper.myLooper() == Looper.getMainLooper()){
            Log.i(TAG, "load bitmap from ui thread, it is not recommended");
        }
        if(mDiskLruCache == null){
            return  null;
        }
        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if(snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = decodeSampleBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if(bitmap != null){
                addBitmapToLruCache(url, bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 对url进行MD5编码
     * @param url
     * @return
     */
    private String hashKeyFromUrl(String url) {
        String cacheKey = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return cacheKey;
    }

    private String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < digest.length; i++){
            String hex = Integer.toHexString(0xFF & digest[i]);
            if(hex.length() == 1){
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 从文件描述符加载图片
     * @param fd
     * @param reqWith
     * @param reqHeight
     * @return
     */
    public Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd, int reqWith, int reqHeight){
        //第一步，只解析图片的边框
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 只解析边框
        BitmapFactory.decodeFileDescriptor(fd,null,options);

        //计算缩放比例
        options.inSampleSize = calculateInSampleSize(options, reqWith,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd,null,options);
    }

    /**
     * 添加加载本地图片任务
     * @param path
     * @param imageView
     */
    private void addLocalImageTask(final String path, final ImageView imageView) {

     addTask(new Runnable()
     {
         @Override
         public void run()
         {
             ImageSize imageSize = getImageViewWidth(imageView);
             int reqWidth = imageSize.width;
             int reqHeight = imageSize.height;

             Bitmap bm = decodeSampledBitmapFromResource(path, reqWidth,
                     reqHeight);
             addBitmapToLruCache(path, bm);
             ImgBeanHolder holder = new ImgBeanHolder();
             holder.bitmap = getBitmapFromLruCache(path);
             holder.imageView = imageView;
             holder.path = path;
             Message message = Message.obtain();
             message.obj = holder;
             // Log.e("TAG", "mHandler.sendMessage(message);");
             mHandler.sendMessage(message);
             mPoolSemaphore.release();
         }
     });

    }

    /**
     * 添加一个任务
     *
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable)
    {
        try
        {
            // 请求信号量，防止mPoolThreadHander为null
            if (mPoolThreadHander == null)
                mSemaphore.acquire();
        } catch (InterruptedException e)
        {
        }
        mTasks.add(runnable);

        mPoolThreadHander.sendEmptyMessage(0x110);
    }

    /**
     * 取出一个任务
     *
     * @return
     */
    private synchronized Runnable getTask()
    {
        if (mType == Type.FIFO)
        {
            return mTasks.removeFirst();
        } else if (mType == Type.LIFO)
        {
            return mTasks.removeLast();
        }
        return null;
    }

    /**
     * 单例获得该实例对象
     *
     * @return
     */
    public static ImageLoader getInstance(int threadCount, Type type)
    {

        if (mInstance == null)
        {
            synchronized (ImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }


    /**
     * 根据ImageView获得适当的压缩的宽和高
     *
     * @param imageView
     * @return
     */
    private ImageSize getImageViewWidth(ImageView imageView)
    {
        ImageSize imageSize = new ImageSize();
        final DisplayMetrics displayMetrics = imageView.getContext()
                .getResources().getDisplayMetrics();
        final LayoutParams params = imageView.getLayoutParams();

        int width = params.width == LayoutParams.WRAP_CONTENT ? 0 : imageView
                .getWidth(); // Get actual image width
        if (width <= 0)
            width = params.width; // Get layout width parameter
        if (width <= 0)
//            width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check
            width = ScreenUtils.getScreenWidth(imageView.getContext()); // Check
        // maxWidth
        // parameter
        if (width <= 0)
            width = displayMetrics.widthPixels;
        int height = params.height == LayoutParams.WRAP_CONTENT ? 0 : imageView
                .getHeight(); // Get actual image height
        if (height <= 0)
            height = params.height; // Get layout height parameter
        if (height <= 0)
            height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check
        // maxHeight
        // parameter
        if (height <= 0)
            height = displayMetrics.heightPixels;
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;

    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     */
    private Bitmap getBitmapFromLruCache(String url)
    {
        String key = hashKeyFromUrl(url);
        return mLruCache.get(key);
    }

    /**
     * 往LruCache中添加一张图片
     *
     * @param url
     * @param bitmap
     */
    private void addBitmapToLruCache(String url, Bitmap bitmap)
    {
        if (getBitmapFromLruCache(url) == null) {
            if (bitmap != null) {
                String key = hashKeyFromUrl(url);
                mLruCache.put(key, bitmap);
            }
        }
    }

    /**
     * 计算inSampleSize，用于压缩图片
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight)
    {
        // 源图片的宽度
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqWidth && height > reqHeight)
        {
            // 计算出实际宽度和目标宽度的比率
            int widthRatio = Math.round((float) width / (float) reqWidth);
            int heightRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = Math.max(widthRatio, heightRatio);
        }
        return inSampleSize;
    }

    /**
     * 根据计算的inSampleSize，得到压缩后图片
     *
     * @param pathName
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap decodeSampledBitmapFromResource(String pathName,
                                                   int reqWidth, int reqHeight)
    {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(pathName, options);

        return bitmap;
    }

    private class ImgBeanHolder
    {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    private class ImageSize
    {
        int width;
        int height;
    }

    /**
     * 反射获得ImageView设置的最大宽度和高度
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName)
    {
        int value = 0;
        try
        {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE)
            {
                value = fieldValue;

                Log.e("TAG", value + "");
            }
        } catch (Exception e)
        {
        }
        return value;
    }

}
