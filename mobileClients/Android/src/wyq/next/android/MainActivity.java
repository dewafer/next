package wyq.next.android;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 使用gridView来展示图片
		// 参考Android Develop Guide中User Interface中的Grid View
		// 使用ImageAdapter来加载图片
		GridView gridview = (GridView) findViewById(R.id.gridView);
		gridview.setAdapter(new ImageAdapter(this));
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return mThumbUrl.length;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) { // if it's not recycled, initialize some
										// attributes
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(dp2pix(85),
						dp2pix(85)));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView
						.setPadding(dp2pix(8), dp2pix(8), dp2pix(8), dp2pix(8));
			} else {
				imageView = (ImageView) convertView;
			}

			DownloadImageTask task = new DownloadImageTask(imageView);
			task.execute(mThumbUrl[position]);
			return imageView;
		}

	}

	// 图片的url
	private String[] mThumbUrl = {
			"http://ww2.sinaimg.cn/large/51d3f408gw1eqzld8ueetj207407ggln.jpg",
			"http://ww1.sinaimg.cn/large/51d3f408gw1eqzkqh3fhag20az06u1kz.gif",
			"http://ww3.sinaimg.cn/large/51d3f408gw1eqzld9mg0bj20dw0afaaj.jpg",
			"http://ww1.sinaimg.cn/large/51d3f408gw1eqzlda4swgj20ba0cgjrn.jpg" };

	// 因为在mainThread上不能进行网络操作，所以使用AsyncTask来加载图片
	class DownloadImageTask extends AsyncTask<String, Object, Bitmap> {

		private ImageView mImageView;

		public DownloadImageTask(ImageView mImageView) {
			this.mImageView = mImageView;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bitmap = null;
			try {
				URL image = new URL(params[0]);
				HttpURLConnection conn = (HttpURLConnection) image
						.openConnection();
				conn.connect();
				bitmap = BitmapFactory.decodeStream(conn.getInputStream());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			mImageView.setImageBitmap(result);
			mImageView.postInvalidate();
		}

	}

	// 这个方法把dp单位转换成像素单位
	private int dp2pix(int pixel) {
		return (int) Math.ceil(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, pixel, getResources()
						.getDisplayMetrics()));
	}
}
