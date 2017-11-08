package com.wzf.imagepicker.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ImageView;

import com.wzf.imagepicker.ImageLoader.ImageLoader;
import com.wzf.imagepicker.R;
import com.wzf.imagepicker.adapter.CommonAdapter;
import com.wzf.imagepicker.adapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;

public class LoadNetImageActivity extends AppCompatActivity {
    private ImageView im;
    private List<String> mUrList = new ArrayList<String>();
    private CommonAdapter adapter;
    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_image);
        gridView = (GridView) findViewById(R.id.gridView1);
        intiData();
        setAdapter();
    }

    private void setAdapter() {
        adapter = new CommonAdapter<String>(mUrList, this, R.layout.image_list_item) {
            @Override
            public void convert(ViewHolder viewHolder, String item) {
                ImageView im = viewHolder.getView(R.id.image);
                im.setImageResource(R.mipmap.pictures_no);
                ImageLoader.getInstance().loadImage(item,im);

            }
        };
        gridView.setAdapter(adapter);
    }

    private void intiData() {
        String[] imageUrls = {
                "http://pic55.nipic.com/file/20141208/19462408_171130083000_2.jpg",
                "http://imgsrc.baidu.com/forum/pic/item/e5ff0081cb237db138012f73.jpg",
                "http://v1.qzone.cc/skin/201511/29/09/55/565a5b0d64410783.jpg%21600x600.jpg",
                "http://pic62.nipic.com/file/20150319/12632424_132215178296_2.jpg",
                "http://img02.tooopen.com/images/20140504/sy_60294738471.jpg",
                "http://sucai.qqjay.com/qqjayxiaowo/201210/26/1.jpg",
                "http://pic78.huitu.com/res/20160604/1029007_20160604114552332126_1.jpg",
                "http://pic6.huitu.com/res/20130116/84481_20130116142820494200_1.jpg",
                "http://img2.imgtn.bdimg.com/it/u=819201812,3553302270&fm=214&gp=0.jpg",
                "http://img13.poco.cn/mypoco/myphoto/20120828/15/55689209201208281549023849547194135_001.jpg",
                "http://pic28.nipic.com/20130424/11588775_115415688157_2.jpg",
                "http://pic41.nipic.com/20140518/18521768_133448822000_2.jpg",
                "http://pic47.nipic.com/20140904/18981839_095218870000_2.jpg",
                "http://pic.58pic.com/58pic/16/62/63/97m58PICyWM_1024.jpg",
                "http://pic.58pic.com/58pic/17/41/38/88658PICNuP_1024.jpg",
                "http://pic32.nipic.com/20130827/12906030_123121414000_2.png",
                "http://pic44.nipic.com/20140717/12432466_121957328000_2.jpg",
                "http://pic.58pic.com/58pic/13/85/85/73T58PIC9aj_1024.jpg",
                "http://www.51wendang.com/pic/d2791169614460bb195dab7b/1-810-jpg_6-1080-0-0-1080.jpg",
                "http://pic35.nipic.com/20131112/2531170_204256005000_2.jpg",
                "http://pic75.nipic.com/file/20150820/18576408_084713324536_2.jpg",
                "http://d.hiphotos.baidu.com/zhidao/pic/item/0e2442a7d933c895623a9a8fd11373f0830200f9.jpg",
                "http://pic41.nipic.com/20140507/18602184_142854861000_2.jpg",
                "http://pic35.nipic.com/20131115/6704106_153707247000_2.jpg",
                "http://pic2.ooopic.com/12/62/16/24bOOOPIC57_1024.jpg",
                "http://pic44.nipic.com/20140721/11624852_001107119409_2.jpg",
                "http://pic35.nipic.com/20131121/2531170_145358633000_2.jpg",
                "http://pic.58pic.com/58pic/15/69/99/55958PICpUx_1024.jpg",
                "http://r4.ykimg.com/0541040850E6FD8F6A0A4471CA0F2231",
                "http://img.taopic.com/uploads/allimg/120423/107913-12042323220753.jpg"
        };
        for (String url : imageUrls) {
            mUrList.add(url);
        }
    }

}
