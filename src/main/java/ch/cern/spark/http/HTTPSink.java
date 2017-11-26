package ch.cern.spark.http;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.apache.spark.streaming.api.java.JavaDStream;

import ch.cern.Taggable;
import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.Stream;
import ch.cern.spark.json.JSONObject;
import ch.cern.spark.json.JSONParser;

public class HTTPSink implements Serializable{
	
	private static final long serialVersionUID = 2779022310649799825L;

	private final static Logger LOG = Logger.getLogger(HTTPSink.class.getName());

	public static final String AUTH_PROPERTY = "auth";
    public static final String AUTH_USERNAME_PROPERTY = AUTH_PROPERTY + ".user";
    public static final String AUTH_PASSWORD_PROPERTY = AUTH_PROPERTY + ".password";
    private Header authHeader;

	private String url;
	
	private static transient HttpClient httpClient = new HttpClient();
	
	private Map<String, String> propertiesToAdd;

	public void config(Properties properties) throws ConfigurationException {
		url = properties.getProperty("url");
		
		propertiesToAdd = properties.getSubset("add").toStringMap();
		
		// Authentication configs
        boolean authentication = properties.getBoolean(AUTH_PROPERTY);
        if(authentication){
            String username = properties.getProperty(AUTH_USERNAME_PROPERTY);
            String password = properties.getProperty(AUTH_PASSWORD_PROPERTY);
            
            try {
                String encoding = Base64.getEncoder().encodeToString((username+":"+password).getBytes("UTF-8"));
                
                authHeader = new Header("Authorization", "Basic " + encoding);
                
                LOG.info("Authentication enabled, user: " + username);
            } catch (UnsupportedEncodingException e) {
                throw new ConfigurationException("Problem when creating authentication header");
            }
        }
	}
	
	public void sink(Stream<?> outputStream) {
		Stream<Object> jsonStream = outputStream.map(object -> {
			JSONObject json = JSONParser.parse(object);
			
			Map<String, String> tags = null;
			if(object instanceof Taggable)
				tags = ((Taggable) object).getTags();
			
			for (Map.Entry<String, String> propertyToAdd : propertiesToAdd.entrySet()) {
				String value = propertyToAdd.getValue();
				
				if(value.startsWith("%") && tags != null && tags.containsKey(value.substring(1)))
					value = tags.get(value.substring(1));
				
				json.setProperty(propertyToAdd.getKey(), value);
			}
			
			return json;
		});
		
		JavaDStream<String> jsonStringStream = jsonStream.asString().asJavaDStream();
		
		jsonStringStream.foreachRDD(rdd -> {
			List<String> jsonStringList = rdd.collect();
			
			for (String jsonString : jsonStringList)
				send(jsonString);
		});
	}

	private void send(String jsonString) throws HttpException, IOException {
        StringRequestEntity requestEntity = new StringRequestEntity(jsonString, "application/json", "UTF-8");
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(requestEntity);
        
        if(authHeader != null)
            postMethod.setRequestHeader(authHeader);
        
        int statusCode = httpClient.executeMethod(postMethod);
        
        if (statusCode != 201) {
            throw new IOException("Unable to POST to url=" + url + " with status code=" + statusCode);
        } else {
            LOG.debug("Results posted to " + url);
        }
	}
	
	public static void setClient(HttpClient httpClient) {
		HTTPSink.httpClient = httpClient;
	}

}
