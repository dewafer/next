package wyq.next.android;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// gridview使用ImageAdapter适配器来获得各个子view
		gridview = (GridView) findViewById(R.id.gridView);
		gridview.setAdapter(new ImageAdapter(this));

		// 加载图片列表
		((NextApplication) getApplication()).loadImageUrls(gridview);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			// 都手动刷新了，能不清除cache么？
			getNextApplication().getThumbCache().clear();
			getNextApplication().removeCacheFileAll();
			// 加载图片列表
			getNextApplication().loadImageUrls(gridview);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public NextApplication getNextApplication() {
		return (NextApplication) getApplication();
	}

	// 从NextApplication中取得thumbUrls的list
	protected List<String> getThumbUrls() {
		return getNextApplication().getThumbUrls();
	}

	// 从NextApplication中取得cache文件的map
	protected Map<String, String> getThumbCache() {
		return getNextApplication().getThumbCache();
	}

	// 读cache文件到bitmap
	protected Bitmap readCacheFile(String fileName) {
		return getNextApplication().readCacheFile(fileName);
	}

	// 把inputStream写到cache目录中的fileName文件
	protected void saveImageAsCacheFile(String fileName, InputStream is)
			throws FileNotFoundException, IOException {
		getNextApplication().saveImageAsCacheFile(fileName, is);
	}

	/**
	 * ImageAdapter适配器，用来给gridView填充图片
	 */
	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return getThumbUrls().size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			RelativeLayout view;

			// convertView是循环使用的view
			// 如果为null则表明该view没有循环使用，创建新view
			if (convertView == null) {
				// 使用relativeLayout作为子view
				// 85dp x 85dp大小
				view = new RelativeLayout(mContext);
				view.setLayoutParams(new GridView.LayoutParams(dp2pix(85),
						dp2pix(85)));
			} else {
				// 循环使用
				view = (RelativeLayout) convertView;
				view.removeAllViews();
			}

			// relativeLayout里面放入一个progressBar表明正在加载
			ProgressBar progressBar = new ProgressBar(mContext);
			// 设置progressBar表示为无限循环
			progressBar.setIndeterminate(true);
			// 正常大小并且居中
			LayoutParams params = new RelativeLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
			progressBar.setLayoutParams(params);

			// progressBar加入relativeLayout中
			view.addView(progressBar);

			// 开启下载任务
			DownloadImageTask task = new DownloadImageTask(getThumbUrls().get(
					position), view);
			task.execute();

			// view返回到画面上显示
			return view;
		}
	}

	private GridView gridview;

	// 因为在mainThread上不能进行网络操作，所以使用AsyncTask来加载图片
	class DownloadImageTask extends AsyncTask<Object, Object, Bitmap> {

		// 这个view用来装图片子imageView
		// 理论上应该是relativeView，但在这里我们不需要知道是什么layout
		// 所以用父类ViewGroup
		private ViewGroup mParentView;
		private String mImageUrl;

		public DownloadImageTask(String mImageUrl, ViewGroup mParentView) {
			this.mImageUrl = mImageUrl;
			this.mParentView = mParentView;
		}

		@Override
		protected Bitmap doInBackground(Object... params) {

			Bitmap bitmap = null;
			String cacheFileName = null;
			Map<String, String> cache = getThumbCache();
			if (cache.containsKey(mImageUrl)) {
				// 想啥呢？！如果没cache，每次都去下载表累死我啊
				// 如果有cache就从cache里拿
				cacheFileName = cache.get(mImageUrl);
				bitmap = readCacheFile(cacheFileName);
			}

			if (bitmap != null) {
				return bitmap;
			} else if (cacheFileName != null) {
				getNextApplication().removeCacheFile(cacheFileName);
			}

			// 开始下载
			try {
				// 加载图片
				URL image = new URL(mImageUrl);
				HttpURLConnection conn = (HttpURLConnection) image
						.openConnection();
				conn.connect();
				// 用UUID作为文件名
				String fileName = UUID.randomUUID().toString();
				// 图片下载后写成cache文件
				saveImageAsCacheFile(fileName, conn.getInputStream());
				// 再把cache文件解码成bitmap
				bitmap = readCacheFile(fileName);
				// cache文件读取成功的话
				if (bitmap != null) {
					// 保存cache
					cache.put(mImageUrl, fileName);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 如果没有搞到图片则会返回null
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			View resultView;

			if (result == null) {
				// 没有搞到图片肿木板？
				// 放一个textView写上
				// 木有图片
				TextView textView = new TextView(mParentView.getContext());
				LayoutParams params = new RelativeLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_IN_PARENT);
				textView.setLayoutParams(params);

				textView.setText(R.string.no_image);

				resultView = textView;
			} else {

				// 搞到图片了
				// 设置好居中及大小后
				// 显示
				ImageView imageView = new ImageView(mParentView.getContext());
				LayoutParams params = new RelativeLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				params.addRule(RelativeLayout.CENTER_IN_PARENT);
				imageView.setLayoutParams(params);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView
						.setPadding(dp2pix(8), dp2pix(8), dp2pix(8), dp2pix(8));
				imageView.setImageBitmap(result);

				resultView = imageView;
			}

			// 父viewGroup中的子view都删了吧
			// 然后才能正常显示啊
			mParentView.removeAllViews();
			mParentView.addView(resultView);
			mParentView.postInvalidate();
		}

	}

	// 这个方法把dp单位转换成像素单位
	private int dp2pix(int pixel) {
		return (int) Math.ceil(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, pixel, getResources()
						.getDisplayMetrics()));
	}
}
