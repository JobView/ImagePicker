package com.wzf.imagepicker.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.werb.permissionschecker.PermissionChecker;
import com.wzf.imagepicker.I.IDirChoose;
import com.wzf.imagepicker.R;
import com.wzf.imagepicker.utils.ScreenUtils;
import com.wzf.imagepicker.adapter.ImgListAdapter;
import com.wzf.imagepicker.model.ImageFloder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ImagePickerActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_IMAGE_PICKER = 0X264;
    public static final String DATA_KEY_FOR_IMAGES = "DATA_KEY_FOR_IMAGES";
    public static final String DATA_KEY_FOR_MAX_COUNT = "DATA_KEY_FOR_MAX_COUNT";
    public  ArrayList<String> imgs = new ArrayList<>();
    private int maxCount = 5;
    private ProgressDialog mProgressDialog;
    private TextView tvImgDirName;
    private TextView tvImgCount;
    private RecyclerView rv;
    private ImgListAdapter mAdapter;
    private TextView tvRight;
    /**
     * 扫描拿到所有的图片文件夹
     */
    private List<ImageFloder> mImageFloders = new ArrayList<ImageFloder>();

    ImageFloder currentFloder = new ImageFloder();
    /**
     * 临时的辅助类，用于防止同一个文件夹的多次扫描
     */
    private HashSet<String> mDirPaths = new HashSet<String>();

    private ImageDirPopDialog dirPopDialog;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            updateView();
            initDialog();
        }
    };
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private PermissionChecker permissionChecker;

    private void initDialog() {
        if(dirPopDialog == null){
            dirPopDialog = new ImageDirPopDialog(this, mImageFloders);
            dirPopDialog.setiListener(new IDirChoose<ImageFloder>() {
                @Override
                public void choose(ImageFloder imageFloder) {
                    if(imageFloder != null){
                        currentFloder = imageFloder;
                        updateView();
                    }
                }
            });
        }
        findViewById(R.id.rl_show_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dirPopDialog.isShowing()){
                    dirPopDialog.dismiss();
                }else {
                    dirPopDialog.show();
                }
            }
        });
    }

    private void updateView() {
        if (TextUtils.isEmpty(currentFloder.getDir())) {
            Toast.makeText(this, "擦，一张图片都没有扫描到", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> mImgs = Arrays.asList(new File(currentFloder.getDir()).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".jpg") || filename.endsWith(".png")
                        || filename.endsWith(".jpeg"))
                    return true;
                return false;
            }
        }));
        mAdapter.setBasePath(currentFloder.getDir());
        mAdapter.refresh(mImgs);
        tvImgDirName.setText(currentFloder.getName());
        tvImgCount.setText(currentFloder.getCount() + "张");
        mProgressDialog.dismiss();
    }

    int totalCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);
        initView();
        initData();

    }


    private void initView() {
        tvImgDirName = (TextView) findViewById(R.id.tv_img_name);
        tvImgCount = (TextView) findViewById(R.id.tv_img_count);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        mAdapter = new ImgListAdapter(this, new ArrayList<String>(), false, rv, imgs){
            @Override
            public void onItemClickListener(int position) {
                String path = basePath + File.separator + mDatas.get(position);
                if(imgs.contains(path)){
                    imgs.remove(path);
                }else {
                    if(imgs.size() < maxCount){
                        imgs.add(path);
                    }
                }
                notifyItemChanged(position);
                tvRight.setText("完成(" + imgs.size() + ")");
            }
        };
        rv.addItemDecoration(new RecyclerView.ItemDecoration() {
            private int space = ScreenUtils.dip2px(ImagePickerActivity.this, 2);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.top = (space * 2);
                outRect.left = space;
                outRect.right = space;
            }
        });
        rv.setAdapter(mAdapter);
        findViewById(R.id.iv_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        tvRight = (TextView) findViewById(R.id.tv_right);
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, new Intent().putStringArrayListExtra(DATA_KEY_FOR_IMAGES, (ArrayList<String>) imgs.clone()));
                imgs.clear();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvRight.setText("完成(" + imgs.size() + ")");
    }

    private void initData() {
        maxCount = getIntent().getIntExtra(DATA_KEY_FOR_MAX_COUNT, 0);
        permissionChecker = new PermissionChecker(this);
        if (permissionChecker.isLackPermissions(PERMISSIONS)) {
            permissionChecker.requestPermissions();
        } else {
            // 执行你的相关操作
            getPicture();
        }
    }

    private void getPicture() {
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "暂无外部存储", Toast.LENGTH_SHORT).show();
            return;
        }
        // 显示进度条
        mProgressDialog = ProgressDialog.show(this, null, "正在扫描图片...");
        new Thread() {
            @Override
            public void run() {
                scanImages();
            }
        }.start();
    }

    /**
     * 利用ContentProvider扫描手机中的图片，此方法在运行在子线程中 完成图片的扫描，最终获得jpg最多的那个文件夹
     */
    private void scanImages() {
        String firstImage = null;
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = ImagePickerActivity.this
                .getContentResolver();

        // 只查询jpeg和png的图片
        Cursor mCursor = mContentResolver.query(mImageUri, null,
                MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?",
                new String[]{"image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_MODIFIED);

        Log.e("TAG", mCursor.getCount() + "");
        while (mCursor.moveToNext()) {
            // 获取图片的路径
            String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
            Log.e("TAG", path);
            // 拿到第一张图片的路径
            if (firstImage == null)
                firstImage = path;
            // 获取该图片的父路径名
            File parentFile = new File(path).getParentFile();
            if (parentFile == null)
                continue;
            //获取父文路径的绝对路径
            String dirPath = parentFile.getAbsolutePath();
            ImageFloder imageFloder = null;
            // 利用一个HashSet防止多次扫描同一个文件夹（不加这个判断，图片多起来还是相当恐怖的~~）
            if (mDirPaths.contains(dirPath)) {
                continue;
            } else {
                mDirPaths.add(dirPath);
                // 初始化imageFloder
                imageFloder = new ImageFloder();
                imageFloder.setDir(dirPath);
                imageFloder.setFirstImagePath(path);
            }
            int picSize = parentFile.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.endsWith(".jpg")
                            || filename.endsWith(".png")
                            || filename.endsWith(".jpeg"))
                        return true;
                    return false;
                }
            }).length;
            totalCount += picSize;

            imageFloder.setCount(picSize);
            mImageFloders.add(imageFloder);

            if (picSize > currentFloder.getCount()) {
                currentFloder = imageFloder;
            }
        }
        mCursor.close();
        // 扫描完成，辅助的HashSet也就可以释放内存了
        mDirPaths = null;
        // 通知Handler扫描图片完成
        mHandler.sendEmptyMessage(0x110);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionChecker.PERMISSION_REQUEST_CODE:
                boolean b = permissionChecker.hasAllPermissionsGranted(grantResults);
                if (b) {
                    // 执行你的相关操作
                    getPicture();
                } else {
                    // 权限拒绝后的提示
                    permissionChecker.showDialog();
                }
                break;
        }
    }

    /**
     * startMethod
     * @param activity
     * @param maxCount 最大图片数量
     */
    public static void startMethod(Activity activity, int maxCount){
        activity.startActivityForResult(
                new Intent(activity, ImagePickerActivity.class).
                        putExtra(DATA_KEY_FOR_MAX_COUNT, maxCount),
                ImagePickerActivity.REQUEST_CODE_IMAGE_PICKER);
    }
}
