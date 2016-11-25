/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.tv.service.recommendation;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import javax.inject.Inject;

import butter.droid.tv.TVButterApplication;

public class RecommendationContentProvider extends ContentProvider {

	public static final String AUTHORITY = "pct.droid.tv.RecommendationContentProvider";
	public static final String CONTENT_URI = "content://" + AUTHORITY + "/";

	@Inject
	OkHttpClient client;

	private boolean initialized;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		ParcelFileDescriptor[] pipe = null;

		String url = uri.getPath();

		try {
			String decodedUrl = URLDecoder.decode(url.replaceFirst("/", ""),
					"UTF-8");
			pipe = ParcelFileDescriptor.createPipe();

			OkUrlFactory factory = new OkUrlFactory(getOkHttpClient());
			HttpURLConnection connection = factory.open(new URL(decodedUrl));

			new TransferThread(connection.getInputStream(),
					new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]))
					.start();
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
			throw new FileNotFoundException("Could not open pipe for: "
					+ uri.toString());
		}

		return (pipe[0]);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public String getType(Uri uri) {
		return "image/*";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	private OkHttpClient getOkHttpClient() {
		init();
		return client;
	}

	private void init() {
		if (!initialized) {
			TVButterApplication.getAppContext()
					.getComponent()
					.inject(this);
		}
	}

	static class TransferThread extends Thread {
		InputStream in;
		OutputStream out;

		TransferThread(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			byte[] buf = new byte[8192];
			int len;

			try {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				in.close();
				out.flush();
				out.close();
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(),
						"Exception transferring file", e);
			}
		}
	}

}