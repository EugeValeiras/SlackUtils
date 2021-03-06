package support.utils.slack;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import support.utils.slack.builder.SlackMessageBuilder;

public class SlackUtil {

	protected String token =  "INVALID TOKEN";
	public ObjectMapper mapper = new ObjectMapper();
	 
	public SlackUtil(String token) {
		this.token = token;
	}
	
	public Boolean sendMessageIfOnline(SlackMessageObject message) throws Exception {
		if(isEmployeeOnline(message.getChannel())){
			return sendMessage(message);
		}
		return false;
	}

	public Boolean isEmployeeOnline(String employeeID) throws Exception{
		
		String presenceUrl =  "https://slack.com/api/users.getPresence";
		
		String presenceParameters = "?"
				+ "token="+token
				+ "&user=" + UriUtils.encodeQueryParam(employeeID, "UTF-8")
				+ "&pretty=1";
		
		HttpGet httpGet = new HttpGet(presenceUrl+presenceParameters);
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(httpGet);
		notifyIfNotSuccess(response);
		
		System.out.println(response.getEntity().getContent());
		SlackUserPresence presenceResponse = map(mapper, response, SlackUserPresence.class);
		
		
		return presenceResponse.isOnline() != null ? presenceResponse.isOnline() : false;
	}
	
	public boolean sendMessage(SlackMessageObject message) throws IOException {
		
		JSONArray attachments = new JSONArray();
		for(SlackAttachment attach : message.getAttachments()){
			JSONObject attachment = new JSONObject(); 
			attachment.put("fallback", attach.getFallback());
			attachment.put("pretext", attach.getPretext());
			attachment.put("color", attach.getColor()); //AA3939
			JSONArray fields = new JSONArray();
			for(SlackField attcField : attach.getFields()){
				JSONObject field = new JSONObject();
				field.put("title", attcField.getTitle());
				field.put("value", attcField.getValue());
				field.put("short", false);
				fields.add(field);
			}
			
			attachment.put("fields", fields);
			attachments.add(attachment);
		}
		
		String chatMessageUrl = 
				"https://slack.com/api/chat.postMessage";
		
		String chatMessageParameters = "?"
						+ "token="+token
						+ "&channel=" + UriUtils.encodeQueryParam(message.getChannel(), "UTF-8")
						+ "&text=" + UriUtils.encodeQueryParam(message.getText(), "UTF-8")
						+ "&username=" + UriUtils.encodeQueryParam(message.getUsername(), "UTF-8")
						+ "&attachments=" + UriUtils.encodeQueryParam(attachments.toJSONString(), "UTF-8")
						+ "&icon_emoji="+UriUtils.encodeQueryParam(message.getIconEmoji(), "UTF-8")
						+ "&link_names=1"
						+ "&pretty=1";
		
		HttpGet httpGet = new HttpGet(chatMessageUrl+chatMessageParameters);
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(httpGet);
		notifyIfNotSuccess(response);
		
		return response.getStatusLine().getStatusCode() == 200;
	}
	
	public Boolean createNewChannel(String channelName) throws ClientProtocolException, IOException{
		String urlString = "https://slack.com/api/channels.create?token="+token+"&name="+channelName+"&pretty=1";
		String encodeUri = UriUtils.encodeQuery(urlString, "UTF-8");
		
		HttpPost httpPost = new HttpPost(encodeUri);
		httpPost.addHeader("Content-Type", "application/json");
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(httpPost);
		
		System.out.println("URL: \n"+encodeUri);
		System.out.println("Data: \n"+"{}");
		
		System.out.println("Status: \n"+response.getStatusLine().getStatusCode());
		System.out.println("Response: \n"+response);
		
		return response.getStatusLine().getStatusCode() == 200;
		
	}
	
	public Boolean archiveChannel(String channelName) throws ClientProtocolException, IOException{
		String urlString = "https://slack.com/api/channels.archive?token="+token+"&name="+channelName;
		String encodeUri = UriUtils.encodeQuery(urlString, "UTF-8");
		
		HttpPost httpPost = new HttpPost(encodeUri);
		httpPost.addHeader("Content-Type", "application/json");
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(httpPost);
		
		System.out.println("URL: \n"+encodeUri);
		System.out.println("Data: \n"+"{}");
		
		System.out.println("Status: \n"+response.getStatusLine().getStatusCode());
		System.out.println("Response: \n"+response);
		
		return response.getStatusLine().getStatusCode() == 200;
		
	}
	
	private void notifyIfNotSuccess(HttpResponse response){
		if(response.getStatusLine().getStatusCode() != 200){
			System.out.println("Data: \n"+"{}");
			
			System.out.println("Status: \n"+response.getStatusLine().getStatusCode());
			System.out.println("Response: \n"+response);
		}
	}

	private <T> T map(ObjectMapper mapper, HttpResponse response,  Class<T> entity) throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
		return mapper.readValue(response.getEntity().getContent(), entity);
	}
	
}
