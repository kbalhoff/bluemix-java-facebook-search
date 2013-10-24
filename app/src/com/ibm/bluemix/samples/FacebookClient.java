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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FacebookClient {

	public List<String> search(String search) throws Exception {
		HttpClient client = new DefaultHttpClient();
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme("https");
		uriBuilder.setHost("graph.facebook.com");
		uriBuilder.setPath("/search");
		uriBuilder.addParameter("q", search);
		uriBuilder.addParameter("limit", "25");
		uriBuilder.addParameter("fields", "message");
		uriBuilder.addParameter("access_token", getAccessToken());
		
		try {
			HttpGet request = new HttpGet(uriBuilder.build());
			request.setHeader("Content-Type", "application/json");
			HttpResponse response = client.execute(request);
			
			String responseText = EntityUtils.toString(response.getEntity());
			JSONObject result = (JSONObject) new JSONParser().parse(responseText);
			JSONArray data = (JSONArray) result.get("data");
			List<String> posts = new ArrayList<String>(data.size());
			
			for (int i = 0; i < data.size(); i++) {
				JSONObject post = (JSONObject) data.get(i);
				posts.add((String) post.get("message"));
			}
			
			return posts;
		} catch (URISyntaxException e) {
			throw new Exception("Problem generating URI for Facebook API.", e);
		} catch (ParseException e) {
			throw new Exception("Problem parsing response from Facebook.", e);
		}
	}
	
	private String getAccessToken() throws Exception {
    	Properties prop = new Properties();
    	 
    	try {
    		prop.load(getClass().getClassLoader().getResourceAsStream("facebook.properties"));
    		String id = prop.getProperty("appId");
    		String secret = prop.getProperty("appSecret");
    		
    		if (id == null || id.length() == 0 || secret == null || secret.length() == 0) {
    			System.out.println("Access token is null.");
    			throw new Exception("Access token is null. Be sure to enter your App ID and App Secret.");
    		}
    		
    		return id + "|" + secret;
    	} catch (IOException e) {
    		throw new Exception("Problem loading access token from facebook.properties.", e);
        }
	}
}
