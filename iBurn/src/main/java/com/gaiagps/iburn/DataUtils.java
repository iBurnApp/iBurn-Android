package com.gaiagps.iburn;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.gaiagps.iburn.json.JSONDeserializers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DataUtils {
	
	private static final String CAMP_DATA_PATH = "playa-json/camp_data.json";
	private static final String EVENT_DATA_PATH = "playa-json/event_data.json";
	private static final String ART_DATA_PATH = "playa-json/art_data.json";
	// Relative to getFilesDir() (/data/data/app.namespace/)
	
	//Location of THE MAN
	private static final double MAN_LAT = 40.782818;
	private static final double MAN_LON = -119.209042;
	
	public static final double MAN_DISTANCE_THRESHOLD = 3; // miles
	
	public static class PopulateDBFromJsonTask extends AsyncTask<Void, Void, Integer>{
        Context c;

        public PopulateDBFromJsonTask(Context c){
            this.c = c;
        }
		// This method is executed in a separate thread
		@Override
		protected Integer doInBackground(Void... input) {
			if(c == null){
				Log.d("ImportJsonToCampTable","Context not set");
				return null;
			}
			
			AssetManager assets = c.getAssets();
			
			
			try {
				
				// CAMPS
				Gson gson = new GsonBuilder().registerTypeAdapter(ArrayList.class, new JSONDeserializers.CampsDeserializer()).create();
				//String[] asset_list = assets.list("playa-json");
				// Get Asset
				InputStream is = assets.open(CAMP_DATA_PATH);
				// Parse JSON
				ArrayList<ContentValues> result = gson.fromJson(inputStreamToChar(is), ArrayList.class);
				// Insert JSON into database
				//content://com.trailbehind.android.iburn.playacontentprovider/camp
				DBWrapper.contentValuesToTable(result, PlayaContentProvider.CAMP_URI);
				/*
				Gson gson;
				InputStream is;
				ArrayList<ContentValues> result;
				*/
				// EVENTS
				gson = new GsonBuilder().registerTypeAdapter(ArrayList.class, new JSONDeserializers.EventsDeserializer()).create();
				is = assets.open(EVENT_DATA_PATH);
				result = gson.fromJson(inputStreamToChar(is), ArrayList.class);
				DBWrapper.contentValuesToTable(result, PlayaContentProvider.EVENT_URI);
				
				// ART
				gson = new GsonBuilder().registerTypeAdapter(ArrayList.class, new JSONDeserializers.ArtDeserializer()).create();
				is = assets.open(ART_DATA_PATH);
				result = gson.fromJson(inputStreamToChar(is), ArrayList.class);
                DBWrapper.contentValuesToTable(result, PlayaContentProvider.ART_URI);
				
				Log.d("ImportJsonToCampTable","Camps sent to database");
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
				return 0;
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
			return 1;
		}
		
		@Override
	    protected void onPostExecute(Integer result) {
			sendDbReadyMessage(result);
			super.onPostExecute(result);

	    }
		
		private void sendDbReadyMessage(int result) { 
		  	  Intent intent = new Intent("dbReady");
		  	  intent.putExtra("status", result);
		  	  LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
		}
		
	}

	public static String inputStreamToChar(InputStream is) throws IOException{
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    total.append(line);
		}
		return total.toString();
	}
	
	public static double distanceFromTheMan(double lat, double lon){
		//40.782818, -119.209042

		double theta = lon - MAN_LON;
		double dist = Math.sin(deg2rad(lat)) * Math.sin(deg2rad(MAN_LAT)) + Math.cos(deg2rad(lat)) * Math.cos(deg2rad(MAN_LAT)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return dist; // miles
	}
	
	static private double deg2rad(double deg) {
		  return (deg * Math.PI / 180.0);
	}

	
	static private double rad2deg(double rad) {
		  return (rad * 180 / Math.PI);
	}
	


}
