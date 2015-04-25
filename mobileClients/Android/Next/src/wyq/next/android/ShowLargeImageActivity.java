package wyq.next.android;

import wyq.next.android.NextApplication.DefaultImageViewCreator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

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

		getNextApplication().downloadImage(imgUrl, imageContainer,
				new LargeImageViewCreator(this));
	}

	public NextApplication getNextApplication() {
		return (NextApplication) getApplication();
	}

	class LargeImageViewCreator extends DefaultImageViewCreator {

		public LargeImageViewCreator(Context context) {
			super(context);
		}

		@Override
		public ImageView createImageView(Context context, String imageCacheFile) {
			ImageView imageView = new ImageView(context);
			imageView.setImageBitmap(getNextApplication().readCacheFile(
					imageCacheFile));
			LayoutParams params = new FrameLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			imageView.setLayoutParams(params);
			imageView.setScaleType(ImageView.ScaleType.CENTER);
			return imageView;
		}
	}

}
