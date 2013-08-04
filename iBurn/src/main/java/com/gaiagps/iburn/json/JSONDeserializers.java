package com.gaiagps.iburn.json;

import android.content.ContentValues;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.EventTable;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class JSONDeserializers {
	/**
	 * Deserializes an array of JSON objects representing Burning Man Camps
	 * into an array of ContentValues for direct insertion into a database
	 * @author davidbrodsky
	 *
	 */
	public static class CampsDeserializer implements JsonDeserializer<ArrayList<ContentValues>>{
		
		public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
		        JsonDeserializationContext context) throws JsonParseException {
			
			ArrayList<ContentValues> result = new ArrayList<ContentValues>();
	
			JsonArray array= json.getAsJsonArray();
			int len = array.size();
			JsonObject object;
			
			for(int x=0; x<len; x++){
				object = array.get(x).getAsJsonObject();
				ContentValues cv = new ContentValues();
				try{
					if(object.has(CampJSON.KEY_NAME))
						if(!object.get(CampJSON.KEY_NAME).isJsonNull())
						cv.put(CampTable.COLUMN_NAME, object.get(CampJSON.KEY_NAME).getAsString());
					
					if(object.has(CampJSON.KEY_DESCRIPTION))
						if(!object.get(CampJSON.KEY_DESCRIPTION).isJsonNull())
						cv.put(CampTable.COLUMN_DESCRIPTION, object.get(CampJSON.KEY_DESCRIPTION).getAsString());
					
					if(object.has(CampJSON.KEY_CAMP_ID))
						if(!object.get(CampJSON.KEY_CAMP_ID).isJsonNull())
						cv.put(CampTable.COLUMN_CAMP_ID, object.get(CampJSON.KEY_CAMP_ID).getAsInt()); 
					
					if(object.has(CampJSON.KEY_CONTACT))
						if(!object.get(CampJSON.KEY_CONTACT).isJsonNull())
							cv.put(CampTable.COLUMN_CONTACT, object.get(CampJSON.KEY_CONTACT).getAsString()); 
					
					if(object.has(CampJSON.KEY_HOMETOWN))
						if(!object.get(CampJSON.KEY_HOMETOWN).isJsonNull())
						cv.put(CampTable.COLUMN_HOMETOWN, object.get(CampJSON.KEY_HOMETOWN).getAsString()); 
					
					if(object.has(CampJSON.KEY_LATITUDE))
						if(!object.get(CampJSON.KEY_LATITUDE).isJsonNull())
						cv.put(CampTable.COLUMN_LATITUDE, object.get(CampJSON.KEY_LATITUDE).getAsDouble()); 
					
					if(object.has(CampJSON.KEY_LONGITUDE))
						if(!object.get(CampJSON.KEY_LONGITUDE).isJsonNull())
						cv.put(CampTable.COLUMN_LONGITUDE, object.get(CampJSON.KEY_LONGITUDE).getAsDouble()); 
					
					if(object.has(CampJSON.KEY_LOCATION))
						if(!object.get(CampJSON.KEY_LOCATION).isJsonNull())
						cv.put(CampTable.COLUMN_LOCATION, object.get(CampJSON.KEY_LOCATION).getAsString());
					
					if(object.has(CampJSON.KEY_YEAR))
						if(!object.get(CampJSON.KEY_YEAR).isJsonNull())
						cv.put(CampTable.COLUMN_YEAR, ((JsonObject)object.get(CampJSON.KEY_YEAR)).get(CampJSON.KEY_YEAR).getAsInt());
					
					if(object.has(CampJSON.KEY_URL))
						if(!object.get(CampJSON.KEY_URL).isJsonNull())
						cv.put(CampTable.COLUMN_URL, object.get(CampJSON.KEY_URL).getAsString());
					
				    result.add(cv);
			    } catch(Throwable t){
			    	throw new JsonParseException(t);
			    }				
			}

		    return result;
		}
	}
	
	public static class EventsDeserializer implements JsonDeserializer<ArrayList<ContentValues>>{
		
		public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
		        JsonDeserializationContext context) throws JsonParseException {
			
			// Playa-data date input format
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// Date print format E hh:mm
			SimpleDateFormat datePrinter = new SimpleDateFormat("EEEE h:mm a");
			// All day date print format E
			SimpleDateFormat dayPrinter = new SimpleDateFormat("EEEE");
			ArrayList<ContentValues> result = new ArrayList<ContentValues>();
	
			JsonArray array= json.getAsJsonArray();
			int len = array.size();
			JsonObject object;
			
			for(int x=0; x<len; x++){
				object = array.get(x).getAsJsonObject();
				ContentValues cv = new ContentValues();
				try{
					if(object.has(EventJSON.KEY_NAME))
						if(!object.get(EventJSON.KEY_NAME).isJsonNull())
						cv.put(EventTable.COLUMN_NAME, object.get(EventJSON.KEY_NAME).getAsString());
					
					if(object.has(EventJSON.KEY_DESCRIPTION))
						if(!object.get(EventJSON.KEY_DESCRIPTION).isJsonNull())
						cv.put(EventTable.COLUMN_DESCRIPTION, object.get(EventJSON.KEY_DESCRIPTION).getAsString());
					
					if(object.has(EventJSON.KEY_ALL_DAY))
						if(!object.get(EventJSON.KEY_ALL_DAY).isJsonNull()){
							if(object.get(EventJSON.KEY_ALL_DAY).getAsString().trim().toLowerCase().compareTo("false")==0)
								cv.put(EventTable.COLUMN_ALL_DAY, 0); 
							else if(object.get(EventJSON.KEY_ALL_DAY).getAsString().trim().toLowerCase().compareTo("true")==0)
								cv.put(EventTable.COLUMN_ALL_DAY, 1); 
						}
					
					if(object.has(EventJSON.KEY_CHECK_LOCATION))
						if(!object.get(EventJSON.KEY_CHECK_LOCATION).isJsonNull()){
							if(object.get(EventJSON.KEY_CHECK_LOCATION).getAsString().trim().toLowerCase().compareTo("false")==0)
								cv.put(EventTable.COLUMN_CHECK_LOCATION, 0); 
							else if(object.get(EventJSON.KEY_CHECK_LOCATION).getAsString().trim().toLowerCase().compareTo("true")==0)
								cv.put(EventTable.COLUMN_CHECK_LOCATION, 1); 
						} 
	
					if(object.has(EventJSON.KEY_HOST_CAMP)){
						if(!object.get(EventJSON.KEY_HOST_CAMP).isJsonNull()){
							JsonObject camp = object.get(EventJSON.KEY_HOST_CAMP).getAsJsonObject();
							if(camp.has(EventJSON.KEY_HOST_CAMP_NAME))
								if(!camp.get(EventJSON.KEY_HOST_CAMP_NAME).isJsonNull())
									cv.put(EventTable.COLUMN_HOST_CAMP_NAME, camp.get(EventJSON.KEY_HOST_CAMP_NAME).getAsString());
									
							if(camp.has(EventJSON.KEY_HOST_CAMP_ID))
								if(!camp.get(EventJSON.KEY_HOST_CAMP_ID).isJsonNull())
									cv.put(EventTable.COLUMN_HOST_CAMP_ID, camp.get(EventJSON.KEY_HOST_CAMP_ID).getAsString()); 
						}
					}
					
					if(object.has(EventJSON.KEY_LATITUDE))
						if(!object.get(EventJSON.KEY_LATITUDE).isJsonNull())
						cv.put(EventTable.COLUMN_LATITUDE, object.get(EventJSON.KEY_LATITUDE).getAsDouble()); 
					
					if(object.has(EventJSON.KEY_LONGITUDE))
						if(!object.get(EventJSON.KEY_LONGITUDE).isJsonNull())
						cv.put(EventTable.COLUMN_LONGITUDE, object.get(EventJSON.KEY_LONGITUDE).getAsDouble()); 
					
					if(object.has(EventJSON.KEY_LOCATION))
						if(!object.get(EventJSON.KEY_LOCATION).isJsonNull())
						cv.put(EventTable.COLUMN_LOCATION, object.get(EventJSON.KEY_LOCATION).getAsString());
					
					if(object.has(EventJSON.KEY_YEAR))
						if(!object.get(EventJSON.KEY_YEAR).isJsonNull())
						cv.put(EventTable.COLUMN_YEAR, ((JsonObject)object.get(EventJSON.KEY_YEAR)).get(EventJSON.KEY_YEAR).getAsInt());
					
					if(object.has(EventJSON.KEY_URL))
						if(!object.get(EventJSON.KEY_URL).isJsonNull())
						cv.put(EventTable.COLUMN_URL, object.get(EventJSON.KEY_URL).getAsString());
					
					if(object.has(EventJSON.KEY_OCCURENCE_SET)){
						if(!object.get(EventJSON.KEY_OCCURENCE_SET).isJsonNull()){
							JsonArray occurences = object.get(EventJSON.KEY_OCCURENCE_SET).getAsJsonArray();
							JsonObject occurence;
							for(int y=0;y<occurences.size();y++){
								occurence = (JsonObject) occurences.get(y);
								
								if(occurence.has(EventJSON.KEY_OCCURENCE_START_TIME))
									if(!occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).isJsonNull()){
										cv.put(EventTable.COLUMN_START_TIME, occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).getAsString());
										Date startDate = dateFormatter.parse(occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).getAsString());
										if(cv.getAsInteger(EventTable.COLUMN_ALL_DAY) == 0){
											cv.put(EventTable.COLUMN_START_TIME_PRINT, datePrinter.format(startDate));
										}else if(cv.getAsInteger(EventTable.COLUMN_ALL_DAY) == 1){
											cv.put(EventTable.COLUMN_START_TIME_PRINT, dayPrinter.format(startDate));
										}	
									}
								
								if(occurence.has(EventJSON.KEY_OCCURENCE_END_TIME))
									if(!occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).isJsonNull()){
										cv.put(EventTable.COLUMN_END_TIME, occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).getAsString());
										Date endDate = dateFormatter.parse(occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).getAsString());
										if(cv.getAsInteger(EventTable.COLUMN_ALL_DAY) == 0){
											cv.put(EventTable.COLUMN_END_TIME_PRINT, datePrinter.format(endDate));
										}else if(cv.getAsInteger(EventTable.COLUMN_ALL_DAY) == 1){
											cv.put(EventTable.COLUMN_END_TIME_PRINT, dayPrinter.format(endDate));
										}	
									}
								
								result.add(cv);	
								cv = new ContentValues(cv);
							}
							
						}
					}
					
				    
			    } catch(Throwable t){
			    	throw new JsonParseException(t);
			    }				
			}// end event loop

		    return result;
		}// end deserialize
	} // end EventDeserializer
	
	public static class ArtDeserializer implements JsonDeserializer<ArrayList<ContentValues>>{
		
		public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
		        JsonDeserializationContext context) throws JsonParseException {
			
			ArrayList<ContentValues> result = new ArrayList<ContentValues>();
	
			JsonArray array= json.getAsJsonArray();
			int len = array.size();
			JsonObject object;
			
			for(int x=0; x<len; x++){
				object = array.get(x).getAsJsonObject();
				ContentValues cv = new ContentValues();
				try{
					if(object.has(ArtJSON.KEY_NAME))
						if(!object.get(ArtJSON.KEY_NAME).isJsonNull())
						cv.put(ArtTable.COLUMN_NAME, object.get(ArtJSON.KEY_NAME).getAsString());
					
					if(object.has(ArtJSON.KEY_DESCRIPTION))
						if(!object.get(ArtJSON.KEY_DESCRIPTION).isJsonNull())
						cv.put(ArtTable.COLUMN_DESCRIPTION, object.get(ArtJSON.KEY_DESCRIPTION).getAsString());
					
					if(object.has(ArtJSON.KEY_ARTIST))
						if(!object.get(ArtJSON.KEY_ARTIST).isJsonNull())
						cv.put(ArtTable.COLUMN_ARTIST, object.get(ArtJSON.KEY_ARTIST).getAsString());
					
					if(object.has(ArtJSON.KEY_ART_ID))
						if(!object.get(ArtJSON.KEY_ART_ID).isJsonNull())
						cv.put(ArtTable.COLUMN_ART_ID, object.get(ArtJSON.KEY_ART_ID).getAsInt());
					
					if(object.has(ArtJSON.KEY_ARTIST_LOCATION))
						if(!object.get(ArtJSON.KEY_ARTIST_LOCATION).isJsonNull())
						cv.put(ArtTable.COLUMN_ARTIST_LOCATION, object.get(ArtJSON.KEY_ARTIST_LOCATION).getAsString());
					
					if(object.has(ArtJSON.KEY_CONTACT))
						if(!object.get(ArtJSON.KEY_CONTACT).isJsonNull())
							cv.put(ArtTable.COLUMN_CONTACT, object.get(ArtJSON.KEY_CONTACT).getAsString()); 
					
					if(object.has(ArtJSON.KEY_LATITUDE))
						if(!object.get(ArtJSON.KEY_LATITUDE).isJsonNull())
						cv.put(ArtTable.COLUMN_LATITUDE, object.get(ArtJSON.KEY_LATITUDE).getAsDouble()); 
					
					if(object.has(ArtJSON.KEY_LONGITUDE))
						if(!object.get(ArtJSON.KEY_LONGITUDE).isJsonNull())
						cv.put(ArtTable.COLUMN_LONGITUDE, object.get(ArtJSON.KEY_LONGITUDE).getAsDouble()); 
					
					if(object.has(ArtJSON.KEY_CIRCULAR_STREET))
						if(!object.get(ArtJSON.KEY_CIRCULAR_STREET).isJsonNull())
						cv.put(ArtTable.COLUMN_CIRCULAR_STREET, object.get(ArtJSON.KEY_CIRCULAR_STREET).getAsString());
					
					if(object.has(ArtJSON.KEY_TIME_ADDRESS))
						if(!object.get(ArtJSON.KEY_TIME_ADDRESS).isJsonNull())
						cv.put(ArtTable.COLUMN_TIME_ADDRESS, object.get(ArtJSON.KEY_TIME_ADDRESS).getAsString());
					
					if(object.has(ArtJSON.KEY_HOUR))
						if(!object.get(ArtJSON.KEY_HOUR).isJsonNull())
						cv.put(ArtTable.COLUMN_HOUR, object.get(ArtJSON.KEY_HOUR).getAsInt());
					
					if(object.has(ArtJSON.KEY_MINUTE))
						if(!object.get(ArtJSON.KEY_MINUTE).isJsonNull())
						cv.put(ArtTable.COLUMN_MINUTE, object.get(ArtJSON.KEY_MINUTE).getAsInt());
					
					if(object.has(ArtJSON.KEY_DISTANCE))
						if(!object.get(ArtJSON.KEY_DISTANCE).isJsonNull())
						cv.put(ArtTable.COLUMN_DISTANCE, object.get(ArtJSON.KEY_DISTANCE).getAsDouble());
					
					if(object.has(ArtJSON.KEY_YEAR))
						if(!object.get(ArtJSON.KEY_YEAR).isJsonNull())
						cv.put(ArtTable.COLUMN_YEAR, ((JsonObject)object.get(ArtJSON.KEY_YEAR)).get(ArtJSON.KEY_YEAR).getAsInt());
					
					if(object.has(ArtJSON.KEY_URL))
						if(!object.get(ArtJSON.KEY_URL).isJsonNull())
						cv.put(ArtTable.COLUMN_URL, object.get(ArtJSON.KEY_URL).getAsString());
					
				    result.add(cv);
			    } catch(Throwable t){
			    	throw new JsonParseException(t);
			    }				
			}

		    return result;
		}
	}

}
