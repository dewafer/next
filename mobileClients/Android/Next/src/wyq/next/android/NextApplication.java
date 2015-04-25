package wyq.next.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wyq.next.android.api.NextServerApi;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.AbsListView;

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

	class DownloadImageUrlsTask extends AsyncTask<Object, Object, String[]> {
		private AbsListView mContainer;

		public DownloadImageUrlsTask(AbsListView parentView) {
			this.mContainer = parentView;
		}

		@Override
		protected String[] doInBackground(Object... params) {
			// TODO 正真地拉是在这里拉，我不是说拉屎

			NextServerApi nextApi = new NextServerApi();
			return nextApi.pullImageList();

			// // 临时地用一个固定的数组来替代一下
			// return new String[] {
			// "http://ww2.sinaimg.cn/large/51d3f408gw1eqzld8ueetj207407ggln.jpg",
			// "http://ww1.sinaimg.cn/large/51d3f408gw1eqzkqh3fhag20az06u1kz.gif",
			// "http://ww3.sinaimg.cn/large/51d3f408gw1eqzld9mg0bj20dw0afaaj.jpg",
			// "http://ww1.sinaimg.cn/large/51d3f408gw1eqzlda4swgj20ba0cgjrn.jpg"
			// };
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

}
