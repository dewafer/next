package wyq.next.android;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements OnItemClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// gridview使用ImageAdapter适配器来获得各个子view
		gridview = (GridView) findViewById(R.id.gridView);
		gridview.setAdapter(new ImageAdapter(this));
		gridview.setOnItemClickListener(this);

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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(this, ShowLargeImageActivity.class);
		intent.putExtra(ShowLargeImageActivity.IMAGE_URL,
				(String) parent.getItemAtPosition(position));
		startActivity(intent);
	}

	public NextApplication getNextApplication() {
		return (NextApplication) getApplication();
	}

	// 从NextApplication中取得thumbUrls的list
	protected List<String> getThumbUrls() {
		return getNextApplication().getThumbUrls();
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
			return getThumbUrls().get(position);
		}

		public long getItemId(int position) {
			return position;
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

			// 开启下载任务
			getNextApplication().downloadImage(getThumbUrls().get(position),
					view);

			// view返回到画面上显示
			return view;
		}
	}

	private GridView gridview;

	private int dp2pix(int dp) {
		return NextApplication.dp2pix(this, dp);
	}

}
