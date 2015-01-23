/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gadelkareem.serverload;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class ServerLoadConfig extends Activity {
	static final String TAG = "ServerLoadConfig";

	private static final String PREFS_NAME = "com.gadelkareem.serverload.ServerLoadProvider";
	private static final String PREF_PREFIX_KEY = "SERVERLOAD_";

	int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	EditText servername_Input, URL_Input;

	public ServerLoadConfig() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		// Set the view layout resource to use.
		setContentView(R.layout.serverload_config);

		// Find the EditText
		URL_Input = (EditText) findViewById(R.id.URL);
		servername_Input = (EditText) findViewById(R.id.servername);

		// Bind the action for the save button.
		findViewById(R.id.save_button).setOnClickListener(mOnClickListener);

		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If they gave us an intent without the widget id, just bail.
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			Log.d(TAG, "Widget id is invalid");
			finish();
		}

		URL_Input.setText(loadPref(ServerLoadConfig.this, appWidgetId, "URL"));
		servername_Input.setText(loadPref(ServerLoadConfig.this, appWidgetId,
				"servername"));

	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			final Context context = ServerLoadConfig.this;

			// When the button is clicked, save the string in our prefs and
			// return that they
			// clicked OK.
			String URL = URL_Input.getText().toString();
			String servername = servername_Input.getText().toString();

			String error = "";

			// check URL syntax
			if (!URLUtil.isValidUrl(URL)) {
				error = "Please enter a valid URL";
			} else if (getUrlContent(URL) == "") {
				error = "Could not retrieve your URL, Check your connection";
			}

			if (error != "") {
				Toast toast = Toast.makeText(context, error, Toast.LENGTH_LONG);
				toast.show();
				return;
			}

			savePref(context, appWidgetId, "URL", URL);
			savePref(context, appWidgetId, "servername", servername);
			// Log.d(TAG, "Prefs saved :" + titlePrefix);

			// Push widget update to surface with newly set prefix
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			ServerLoadProvider.updateAppWidget(context, appWidgetManager,
					appWidgetId, URL, servername);

			// Make sure we pass back the original appWidgetId
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	};

	// Write the prefix to the SharedPreferences object for this widget
	static void savePref(Context context, int appWidgetId, String type,
			String text) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(
				PREFS_NAME, 0).edit();
		prefs.putString(PREF_PREFIX_KEY + type + appWidgetId, text);
		prefs.commit();
	}

	// Read the prefix from the SharedPreferences object for this widget.
	// If there is no preference saved, get the default from a resource
	static String loadPref(Context context, int appWidgetId, String type) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		String result = prefs.getString(PREF_PREFIX_KEY + type + appWidgetId,
				null);

		if (result != null) {
			return result;
		} else {
			return "";
		}
	}

	/**
	 * Pull the raw text content of the given URL. This call blocks until the
	 * operation has completed, and is synchronized because it uses a shared
	 * 
	 * @param url The exact URL to request.
	 * @return The raw content returned by the server.
	 */
	protected static synchronized String getUrlContent(String url) {

		byte[] sBuffer = new byte[512];
		// Create client and set our specific user-agent string
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		Log.d(TAG, "opening :  " + url);

		try {
			HttpResponse response = client.execute(request);
			// Log.d( TAG, "getting status :  " + url );
			// Check if server response is valid
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 200) {
				Log.d(TAG, "Invalid response from server: " + status.toString());
				return "";
			}

			// Pull content stream from response
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();

			ByteArrayOutputStream content = new ByteArrayOutputStream();

			// Read response into a buffered stream
			int readBytes = 0;
			while ((readBytes = inputStream.read(sBuffer)) != -1) {
				content.write(sBuffer, 0, readBytes);
			}

			// Return result from buffered stream
			return new String(content.toByteArray());
		} catch (Exception e) {
			Log.d(TAG, "Problem communicating with API : " + e);
			return "";
		}
	}

	static void deletePref(Context context, int appWidgetId) {
	}

	static void loadAllPrefs(Context context, ArrayList<Integer> appWidgetIds,
			ArrayList<String> URLs, ArrayList<String> servernames) {
	}

}
