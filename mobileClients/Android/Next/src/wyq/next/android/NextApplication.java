package wyq.next.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import wyq.next.android.api.NextServerApi;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class NextApplication extends Application {

	private final List<String> thumbUrls = new ArrayList<String>();
	// 图片的cache，key是URL，value是对应的文件名（不包含路径）
	private final Map<String, String> thumbCache = new HashMap<String, String>();

	@Override
	public void onCreate() {
		super.onCreate();

		// app启动的时候去加载图片的url
		// 算了，还是MainActivity起来的时候再去加载吧
		// loadImageUrls(null);
	}

	public void loadImageUrls(AbsListView gridview) {
		// 这个方法将会去服务器把图片的URL地址给拉下来并更新thumbUrls
		// 传入一个gridView，保证url拉完之后去更新这个gridView
		// 当然如果你传个null我也没办法
		// 确保这货是异步执行的
		new DownloadImageUrlsTask(gridview).execute();
	}

	// 把inputStream写到cache目录中的fileName文件
	public void saveImageAsCacheFile(String fileName, InputStream is)
			throws FileNotFoundException, IOException {

		File cache = new File(getCacheDir(), fileName);
		FileOutputStream out = null;
		BufferedInputStream in = null;

		try {
			out = new FileOutputStream(cache);
			in = new BufferedInputStream(is);
			int len = 4086;
			byte[] buffer = new byte[len];
			while ((len = in.read(buffer, 0, len)) != -1) {
				out.write(buffer, 0, len);
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				throw e;
			}
		}
	}

	// 读cache文件到bitmap
	public Bitmap readCacheFile(String fileName) {
		File cache = new File(getCacheDir(), fileName);
		return BitmapFactory.decodeFile(cache.getPath());
	}

	public void removeCacheFile(String fileName) {
		File cache = new File(getCacheDir(), fileName);
		cache.delete();
	}

	public void removeCacheFileAll() {
		for (File cache : getCacheDir().listFiles()) {
			cache.delete();
		}
	}

	class DownloadImageUrlsTask extends AsyncTask<Object, Object, String[]> {
		private AbsListView mContainer;

		public DownloadImageUrlsTask(AbsListView parentView) {
			this.mContainer = parentView;
		}

		@Override
		protected String[] doInBackground(Object... params) {
			// 正真地拉是在这里拉，我不是说拉屎

			NextServerApi nextApi = new NextServerApi();
			return nextApi.pullImageList();
		}

		@Override
		protected void onPostExecute(String[] result) {
			thumbUrls.clear();
			Collections.addAll(thumbUrls, result);
			if (mContainer != null) {
				// 让装载图片的视图经行更新
				// 刷新按钮使每个子view失效
				mContainer.invalidateViews();
			}
		}

	}

	public List<String> getThumbUrls() {
		return thumbUrls;
	}

	public Map<String, String> getThumbCache() {
		return thumbCache;
	}

	public void downloadImage(String imgUrl, ViewGroup parentView,
			ImageViewCreator mImageViewCreator) {
		// relativeLayout里面放入一个progressBar表明正在加载
		ProgressBar progressBar = new ProgressBar(parentView.getContext());
		// 设置progressBar表示为无限循环
		progressBar.setIndeterminate(true);
		// 正常大小并且居中
		LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		progressBar.setLayoutParams(params);

		// progressBar加入relativeLayout中
		parentView.addView(progressBar);
		new DownloadImageTask(imgUrl, parentView, mImageViewCreator).execute();
	}

	public void downloadImage(String imgUrl, ViewGroup parentView) {
		downloadImage(imgUrl, parentView, new DefaultImageViewCreator(
				parentView.getContext()));
	}

	// 因为在mainThread上不能进行网络操作，所以使用AsyncTask来加载图片
	public class DownloadImageTask extends AsyncTask<Object, Object, String> {

		// 这个view用来装图片子imageView
		// 理论上应该是relativeView，但在这里我们不需要知道是什么layout
		// 所以用父类ViewGroup
		private ViewGroup mParentView;
		private String mImageUrl;
		private ImageViewCreator mImageViewCreator;

		public DownloadImageTask(String mImageUrl, ViewGroup mParentView,
				ImageViewCreator mImageViewCreator) {
			this.mImageUrl = mImageUrl;
			this.mParentView = mParentView;
			this.mImageViewCreator = mImageViewCreator;
		}

		@Override
		protected String doInBackground(Object... params) {

			String cacheFileName = null;
			Map<String, String> cache = getThumbCache();
			if (cache.containsKey(mImageUrl)) {
				// 想啥呢？！如果没cache，每次都去下载表累死我啊
				// 如果有cache就从cache里拿
				cacheFileName = cache.get(mImageUrl);
				return cacheFileName;
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
				// 保存cache
				cache.put(mImageUrl, fileName);
				cacheFileName = fileName;
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 如果没有搞到图片则会返回null
			return cacheFileName;
		}

		@Override
		protected void onPostExecute(String result) {
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
				ImageView imageView = mImageViewCreator.createImageView(
						mParentView.getContext(), result);
				resultView = imageView;
			}

			// 父viewGroup中的子view都删了吧
			// 然后才能正常显示啊
			mParentView.removeAllViews();
			mParentView.addView(resultView);
			mParentView.postInvalidate();
		}

	}

	public static interface ImageViewCreator {
		public ImageView createImageView(Context context, String imageCacheFile);
	}

	public static class DefaultImageViewCreator implements ImageViewCreator {

		protected Context mContext;

		public DefaultImageViewCreator(Context mContext) {
			this.mContext = mContext;
		}

		@Override
		public ImageView createImageView(Context context, String imageCacheFile) {
			ImageView imageView = new ImageView(context);
			imageView.setImageURI(Uri.fromFile(new File(mContext.getCacheDir(),
					imageCacheFile)));
			LayoutParams params = new RelativeLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT);
			imageView.setLayoutParams(params);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(dp2pix(8), dp2pix(8), dp2pix(8), dp2pix(8));
			return imageView;
		}

		protected final int dp2pix(int dp) {
			return NextApplication.dp2pix(mContext, dp);
		}

	}

	// 这个方法把dp单位转换成像素单位
	public static int dp2pix(Context context, int dp) {
		return (int) Math.ceil(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources()
						.getDisplayMetrics()));
	}

}
