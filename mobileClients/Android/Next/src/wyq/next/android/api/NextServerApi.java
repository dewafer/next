package wyq.next.android.api;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.util.JsonReader;

/**
 * 这个类负责从服务器拉图片url的数据
 * 
 * @author dewafer
 *
 */
public class NextServerApi {

	// 临时定义服务器的url地址，因为是模拟器，所以访问本机地址为10.0.2.2
	private static final String NEXT_API_HOST = "http://10.0.2.2:3000";
	private static final String API_URL = NEXT_API_HOST + "/api/";
	private static final String API_DOWNLOAD_IMG_URL = NEXT_API_HOST
			+ "/api/%s/%s";

	// private static final SimpleDateFormat DATE_FORMATTER = new
	// SimpleDateFormat(
	// "yyyy-MM-dd HH:mm:ss");

	/**
	 * 拉图片地址的方法
	 */
	public String[] pullImageList() {

		URL imageListUrl;
		JsonReader reader = null;
		HttpURLConnection conn = null;
		try {
			// 发送api请求
			imageListUrl = new URL(API_URL);
			conn = (HttpURLConnection) imageListUrl.openConnection();
			conn.connect();

			// 解析返回的json
			reader = new JsonReader(
					new InputStreamReader(conn.getInputStream()));

			// 预备一个list放图片的url
			List<String> imageList = new ArrayList<String>();

			// root是array
			reader.beginArray();
			while (reader.hasNext()) {
				// 解析array中的object对象
				reader.beginObject();
				// 预备一个类来存放解析的结果
				Image img = new Image();
				while (reader.hasNext()) {
					// object中的key
					String name = reader.nextName();
					if ("title".equals(name)) {
						// 如果key是title的话
						img.imgTitle = reader.nextString();
					} else if ("file".equals(name)) {
						// 如果key是file的话
						img.imgFileName = reader.nextString();
					} else if ("date".equals(name)) {
						// 如果key是date的话，暂未使用到comment out，然后skip
						reader.skipValue();
						// try {
						// img.imgDate = DATE_FORMATTER.parse(reader
						// .nextString());
						// } catch (ParseException e) {
						// img.imgDate = new Date();
						// e.printStackTrace();
						// }
					} else if ("id".equals(name)) {
						// 如果key是id的话
						img.imgId = reader.nextString();
					} else if ("rev".equals(name)) {
						// 如果key是rev的话
						img.imgRev = reader.nextString();
					} else if ("_files".equals(name)) {
						// 如果key是_files的话，未使用到，skip
						reader.skipValue();
					} else {
						// 其他的也就skip了
						reader.skipValue();
					}
				}
				// 对象解析结束
				reader.endObject();
				// 使用解析玩的值拼接图片url
				String imgURL = String.format(API_DOWNLOAD_IMG_URL, img.imgId,
						img.imgFileName);
				// 拼接完后放到list中
				imageList.add(imgURL);
			}
			// json解析完成
			reader.endArray();

			// list转换成array返回
			return imageList.toArray(new String[imageList.size()]);

		} catch (Exception e) {
			// 无视所有exception
			e.printStackTrace();
		} finally {
			// 尝试关闭reader
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 能跑到这里说明有错，返回一个空数组
		return new String[] {};
	}

	class Image {
		String imgTitle;
		String imgFileName;
		Date imgDate;
		String imgId;
		String imgRev;
	}
}
