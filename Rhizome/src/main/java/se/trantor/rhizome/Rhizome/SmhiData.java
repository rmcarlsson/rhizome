/**
 * 
 */
package se.trantor.rhizome.Rhizome;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.json.*;

/**
 * @author rmcar
 *
 */
public class SmhiData {

	/*
	 * Vid valet av gridpunkt så är det viktigt att tänka på att vald gridpunkt
	 * på land kan ge närmsta gridpunkt ute i havet och omvänt. Vissa parametrar
	 * har stora gradienter i övergången mellan land och hav (ex. det blåser
	 * kraftigare över havet ).
	 */

	public static final String URI_SMHI = "http://opendata-download-metfcst.smhi.se/api/category/pmp2g/version/2/geotype/point";

	public static double getReignAmount_mm(double latitude, double longitude, int forcastPeriod_h) throws ClientProtocolException, IOException, JSONException {

		String LAT = Double.toString(latitude);
		String LONG = Double.toString(longitude);

		//JsonReader reader = null;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		//try {
		///lon/16/lat/58/data.json
		String uri = URI_SMHI + "/lon/" + LONG +"/lat/" + LAT +  "/data.json";
		HttpGet httpget = new HttpGet(uri);

		System.out.println("Executing request " + httpget.getRequestLine());
		CloseableHttpResponse response = httpclient.execute(httpget);
		//StringBuilder rawStrResponse = new StringBuilder();
		//try {
		System.out.println("----------------------------------------");
		System.out.println(response.getStatusLine());

		// Get hold of the response entity
		HttpEntity entity = response.getEntity();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			entity.writeTo(os);
		} catch (IOException e1) {
		}
		String contentString = new String(os.toByteArray());


		//String retSrc = EntityUtils.toString(entity);
		//InputStreamReader in = new InputStreamReader(entity.getContent()
		//reader = Json.createReader(entity.getContent());
		// parsing JSON
		JSONObject result = new JSONObject(contentString); //Convert String to JSON Object
		System.out.println("referenceTime is " + result.getString("referenceTime"));

		JSONArray jsonMainArr = null;
		try {
			jsonMainArr = result.getJSONArray("timeSeries");
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

		final int reignPeriodHours = 24;
		double reign = 0;
		for (int i = 0; i < jsonMainArr.length(); i++) {  // **line 2**
			JSONObject childJSONObject = jsonMainArr.getJSONObject(i);
			//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SS[xxx][xx][X]");
			DateTimeFormatter formatter = new DateTimeFormatterBuilder()
					// date/time
					.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
					// offset (hh:mm - "+00:00" when it's zero)
					.optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
					// offset (hhmm - "+0000" when it's zero)
					.optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
					// offset (hh - "Z" when it's zero)
					.optionalStart().appendOffset("+HH", "Z").optionalEnd()
					// create formatter
					.toFormatter();
			//System.out.println(childJSONObject.getString("validTime"));

			OffsetDateTime t = OffsetDateTime.parse(childJSONObject.getString("validTime"), formatter);
			
			if (t.isAfter(OffsetDateTime.now().plusHours(reignPeriodHours)))
					{
						System.out.println(t.toString());
						break;
					}

			//System.out.println("At " + t.getHour() + " h");


			//System.out.println("validTime is " + childJSONObject.getString("validTime"));
			JSONArray jsonarray1 = null;
			jsonarray1 = (JSONArray)childJSONObject.get("parameters");//4
			//System.out.println("Length is " + jsonarray1.length());

			for (int j = 0; j < jsonarray1.length(); j++) {
				if (((JSONObject)jsonarray1.get(j)).get("name").toString().equals("pmean"))
				{
					JSONObject jObj = (JSONObject)jsonarray1.get(j);
					JSONArray valuesArray = null;
					valuesArray = (JSONArray)jObj.get("values");

					reign += valuesArray.getDouble(0);

					//System.out.println("Regnmängd " + valuesArray.get(0).toString() + " " + jObj.get("levelType").toString());

				}

			}
			

		}
		System.out.println("Total reign is " + reign);
		return reign;


	}
}