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



import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class ServerLoadProvider extends AppWidgetProvider {
	// log tag
	private static final String TAG = "ServerLoadProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");

		// add service update for every widget
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			Intent active = new Intent(context, UpdateService.class);
			if (active != null) {
				active.setAction("Start");
				active.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

				context.startService(active);
			}
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Log.d(TAG, "onDeleted");
		// When the user deletes the widget, delete the preference associated
		// with it.
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			ServerLoadConfig.deletePref(context, appWidgetIds[i]);
		}
	}

	@Override
	public void onEnabled(Context context) {
		Log.d(TAG, "onEnabled");
		// When the first widget is created, register for the TIMEZONE_CHANGED
		// and TIME_CHANGED
		// broadcasts. We don't want to be listening for these if nobody has our
		// widget active.
		// This setting is sticky across reboots, but that doesn't matter,
		// because this will
		// be called after boot if there is a widget instance for this provider.
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(
				"com.gadelkareem.serverload", ".ServerLoadProvider"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onDisabled(Context context) {
		// When the first widget is created, stop listening for the
		// TIMEZONE_CHANGED and
		// TIME_CHANGED broadcasts.
		Log.d(TAG, "onDisabled");
//		PackageManager pm = context.getPackageManager();
//		pm.setComponentEnabledSetting(new ComponentName(
//				"com.gadelkareem.serverload", ".ServerLoadReceiver"),
//				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//				PackageManager.DONT_KILL_APP);
		context.stopService(new Intent(context, UpdateService.class));

	}

	static void updateAppWidget(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId, String URL,
			String servername) {
		Log.d(TAG, "updateAppWidget appWidgetId=" + appWidgetId + " URL=" + URL
				+ " servername=" + servername);

		// getting load average for current URL
		String Loadavg = ServerLoadConfig.getUrlContent(URL);

		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.serverload_provider);
		views.setTextViewText(R.id.serverload_text, Loadavg + "%");
		views.setTextViewText(R.id.serverload_wtitle,
				ServerLoadConfig.loadPref(context, appWidgetId, "servername"));

		// Tell the widget manager
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	public static class UpdateService extends Service {
		@Override
		public void onStart(Intent intent, int startId) {

			Log.d(TAG, "Update Service started");
			int appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID);
			Context context = getApplicationContext();
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(getApplicationContext());

			String URL = ServerLoadConfig.loadPref(context, appWidgetId, "URL");
			String servername = ServerLoadConfig.loadPref(context, appWidgetId,
					"servername");
			updateAppWidget(context, appWidgetManager, appWidgetId, URL,
					servername);

			super.onStart(intent, startId);
		}

		@Override
		public IBinder onBind(Intent intent) {
			// We don't need to bind to this service
			return null;
		}
	}
}
