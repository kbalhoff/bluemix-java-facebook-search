/*-------------------------------------------------------------------*/
/*                                                                   */
/* Copyright IBM Corp. 2013 All Rights Reserved                      */
/*                                                                   */
/*-------------------------------------------------------------------*/
/*                                                                   */
/*        NOTICE TO USERS OF THE SOURCE CODE EXAMPLES                */
/*                                                                   */
/* The source code examples provided by IBM are only intended to     */
/* assist in the development of a working software program.          */
/*                                                                   */
/* International Business Machines Corporation provides the source   */
/* code examples, both individually and as one or more groups,       */
/* "as is" without warranty of any kind, either expressed or         */
/* implied, including, but not limited to the warranty of            */
/* non-infringement and the implied warranties of merchantability    */
/* and fitness for a particular purpose. The entire risk             */
/* as to the quality and performance of the source code              */
/* examples, both individually and as one or more groups, is with    */
/* you. Should any part of the source code examples prove defective, */
/* you (and not IBM or an authorized dealer) assume the entire cost  */
/* of all necessary servicing, repair or correction.                 */
/*                                                                   */
/* IBM does not warrant that the contents of the source code         */
/* examples, whether individually or as one or more groups, will     */
/* meet your requirements or that the source code examples are       */
/* error-free.                                                       */
/*                                                                   */
/* IBM may make improvements and/or changes in the source code       */
/* examples at any time.                                             */
/*                                                                   */
/* Changes may be made periodically to the information in the        */
/* source code examples; these changes may be reported, for the      */
/* sample code included herein, in new editions of the examples.     */
/*                                                                   */
/* References in the source code examples to IBM products, programs, */
/* or services do not imply that IBM intends to make these           */
/* available in all countries in which IBM operates. Any reference   */
/* to the IBM licensed program in the source code examples is not    */
/* intended to state or imply that IBM's licensed program must be    */
/* used. Any functionally equivalent program may be used.            */
/*-------------------------------------------------------------------*/
package com.ibm.bluemix.samples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TextAnalyzer {
	
	private TextAnalyzer() {}; // prevent initialization of static class
	
	/**
	 * Get an analysis of a JSONArray of text. This will identify and count 
	 * the number of occurrences of an entity based on the option String.
	 * 
	 * @param texts JSONArray of text to analyze
	 * @param option currently, just 'people' or 'companies'
	 * 
	 * @return a String (JSON format) of identified entities and the number 
	 * of times they occurred in the search text
	 * @throws Exception 
	 */
	public static String analyze(List<String> texts, String option) throws Exception {
		JSONArray annotations = getAnnotations(option);
		
		try {
			String url = getUrl(option);
			String body = createBody(texts, annotations);
			System.out.println(body);
			String response = post(body, url);
			
			return countUnique(response);
		} catch (ParseException e) {
			throw new Exception("Problem finding service details. Make sure you have bound the correct services to your app.", e);
		}
	}
	
	/**
	 * Get the URL of the analytics service
	 * 
	 * @param option What we want to identify: 'companies' or 'people'
	 * 
	 * @return the URL of the analytics service
	 * @throws Exception 
	 */
	private static String getUrl(String option) throws Exception {
		Map<String, String> env = System.getenv();
		
		if (env.containsKey("VCAP_SERVICES")) {
			// we are running on cloud foundry, let's grab the service details from vcap_services
			JSONParser parser = new JSONParser();
			JSONObject vcap = (JSONObject) parser.parse(env.get("VCAP_SERVICES"));
			JSONObject service = null;
			String serviceName = "";
			
			if ("people".equals(option)) {
				serviceName = "NamesTextAnalyticsService";
			} else {
				serviceName = "CompaniesTextAnalyticsService";
			}
			
			// We don't know exactly what the service is called, but it will contain serviceName
			for (Object key : vcap.keySet()) {
				String keyStr = (String) key;
				if (keyStr.toLowerCase().contains(serviceName.toLowerCase())) {
					service = (JSONObject) ((JSONArray) vcap.get(keyStr)).get(0);
					break;
				}
			}
			
			if (service != null) {
				return (String) ((JSONObject) service.get("credentials")).get("url");
			}
		}
		
		throw new Exception("No service URL found.");
	}
	
	/**
	 * Get annotations array for analytics body
	 * 
	 * @param option 'people' or 'companies'
	 * 
	 * @return JSONArray containing the requested annotation
	 */
	@SuppressWarnings("unchecked")
	private static JSONArray getAnnotations(String option) {
		JSONArray annotations = new JSONArray();
		
		if ("people".equals(option)) {
			annotations.add("com.ibm.langware.en.Person");
		} else {
			annotations.add("com.ibm.langware.en.Company");
		}
		
		return annotations;
	}
	
	/**
	 * Create a String representing a JSON object with 'texts' and 
	 * 'annotations' fields. This is used as the body for the 
	 * request to the analytics service.
	 * 
	 * @param tweets
	 * @param annotations
	 * 
	 * @return request body as String
	 */
	@SuppressWarnings("unchecked")
	private static String createBody(List<String> texts, JSONArray annotations) {
		JSONObject body = new JSONObject();

		body.put("texts", texts);
		body.put("annotations", annotations);
		
		return body.toJSONString().toString();
	}

	/**
	 * POST the request to the analytics service URL
	 * 
	 * @param body JSON body as String
	 * @param url URL of the analytics service
	 * @return response from the analytics service or null if something failed
	 * @throws Exception 
	 */
	private static String post(String body, String url) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(url);
		request.setHeader("Content-Type", "application/json");
		request.setEntity(new StringEntity(body));
		
		try {
			HttpResponse response = client.execute(request);
			
			int statusCode = response.getStatusLine().getStatusCode();
			String responseText = EntityUtils.toString(response.getEntity());
			
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception("Status code: " + statusCode);
			}
			
			return responseText;
		} catch (Throwable e) {
			throw new Exception("Problem with request to service at: " + url, e);
		}
	}
	
	/**
	 * Parse the analytics service response, counting number of unique 
	 * entities that it identified
	 * @param response
	 * @return results as JSON String, to be passed directly to client side
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	private static String countUnique(String response) throws Exception {
		if (response == null) {
			throw new Exception("Problem with analytics service.");
		}
		
		// This will hold the total number of times each entity occurs throughout all the tweets
		Map<String, Integer> histogram = new HashMap<String, Integer>();
		JSONArray texts = null;
		
		try {
			texts = (JSONArray) new JSONParser().parse(response);
		} catch (ParseException e) {
			throw new Exception("Problem parsing analytics service response.", e);
		}
		
		try {	
			for (Object text : texts) {
				/* Each text can have multiple annotations, so we need to grab the "annotations" array 
				 * from the first object of the "analyticsResults" array. Since we are only looking for 
				 * one "type" of entity at a time, we will always want analyticsResults[0] for each text.
				 */
				JSONArray results = (JSONArray) ((JSONObject) text).get("analyticsResults");
				JSONArray annotations = (JSONArray) ((JSONObject) results.get(0)).get("annotations");
				
				for (Object annotation : annotations) {
					/* For each annotation, there will be a "covered-text" string identifying the 
					 * entity it found (e.g. "IBM"). We implement that entity's counter in the 
					 * histogram each time we encounter it 
					 */
					String coveredText = (String) ((JSONObject) annotation).get("covered-text");
					String key = coveredText.toLowerCase(); // so matches aren't case-sensitive
					
					histogram.put(key, histogram.containsKey(key) ? histogram.get(key) + 1 : 1);
				}
			}
		} catch (Exception e) {
			throw new Exception("Problem with analytics service response.", e);
		}
		
		/* Bundle up the results in a JSON object that looks like this:
		 * {
		 *   "labels": [],
		 *   "response": []
		 * }
		 * Where each element of "labels" is the name of the entity, and the corresponding 
		 * element in "response" is the number of times it occurred.
		 */
		JSONObject result = new JSONObject();
		ArrayList<String> keys = new ArrayList<String>(histogram.size());
		ArrayList<Integer> values = new ArrayList<Integer>(histogram.size());
		
		for (String key : histogram.keySet()) {
			keys.add(key);
			values.add(histogram.get(key));
		}
		
		result.put("labels", keys);
		result.put("response", values);
		
		return result.toJSONString();
	}
}
