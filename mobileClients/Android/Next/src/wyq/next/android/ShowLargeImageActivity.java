package wyq.next.android;

import wyq.next.android.NextApplication.ImageViewLayoutInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

public class ShowLargeImageActivity extends Activity {

	public static final String IMAGE_URL = ShowLargeImageActivity.class
			+ ".img_url";
	private ViewGroup imageContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_large_image);
		imageContainer = (ViewGroup) findViewById(R.id.imageContainer);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		String imgUrl = getIntent().getStringExtra(IMAGE_URL);

		ImageViewLayoutInfo layoutInfo = new ImageViewLayoutInfo();
		layoutInfo.layoutParams_h = LayoutParams.WRAP_CONTENT;
		layoutInfo.layoutParams_w = LayoutParams.WRAP_CONTENT;
		layoutInfo.imageScaleType = ImageView.ScaleType.CENTER;

		getNextApplication().downloadImage(imgUrl, imageContainer, layoutInfo);
	}

	public NextApplication getNextApplication() {
		return (NextApplication) getApplication();
	}

}
