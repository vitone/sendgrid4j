package jp.co.flect.sendgrid.transport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jp.co.flect.sendgrid.json.JsonUtils;
import jp.co.flect.sendgrid.model.WebMail;
import jp.co.flect.sendgrid.SendGridException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

public class HttpClientTransport implements Transport {
	
	private HttpClient client = null;
	private ProxyInfo proxyInfo = null;
	
	private int soTimeout = 0;
	private int connectionTimeout = 0;
	
	public String send(String url, Map<String, String[]> params) throws IOException, SendGridException {
		HttpClient client = getHttpClient();
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			String key = entry.getKey();
			for (String s : entry.getValue()) {
				list.add(new BasicNameValuePair(key, s));
			}
		}
		HttpPost method = new HttpPost(url);
		method.setEntity(new UrlEncodedFormEntity(list, "utf-8"));
		
		HttpResponse res = client.execute(method);
		String body = EntityUtils.toString(res.getEntity(), "utf-8");
System.out.println("execute " + url + ": " + res.getStatusLine());
System.out.println(body);
		
		if (res.getStatusLine().getStatusCode() != 200) {
			if (body != null && body.length() > 0 && body.charAt(0) == '{') {
				Map<String, Object> map = null;
				try {
					map = JsonUtils.parse(body);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (map != null && (map.get("error") != null || map.get("errors") != null)) {
					throw new SendGridException(map);
				}
			}
			throw new SendGridException(res.getStatusLine().toString());
		}
		return body;
	}
	
	public void send(String url, WebMail mail, File... attachement) throws IOException, SendGridException {
	}
	
	public ProxyInfo getProxyInfo() { return this.proxyInfo;}
	public void setProxyInfo(ProxyInfo proxy) { this.proxyInfo = proxy;}
	
	private HttpClient getHttpClient() {
		if (this.client == null) {
			BasicHttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, this.connectionTimeout);
			HttpConnectionParams.setSoTimeout(params, this.soTimeout);
		
			DefaultHttpClient client = new DefaultHttpClient(params);
			if (this.proxyInfo != null) {
				HttpHost proxy = new HttpHost(proxyInfo.getHost(), proxyInfo.getPort());
				client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				if (proxyInfo.getUserName() != null && proxyInfo.getPassword() != null) {
					client.getCredentialsProvider().setCredentials(
						new AuthScope(proxyInfo.getHost(), proxyInfo.getPort()),
						new UsernamePasswordCredentials(proxyInfo.getUserName(), proxyInfo.getPassword()));
				}
			}
			this.client = client;
		}
		return this.client;
	}
	
	public int getSoTimeout() { return this.soTimeout;}
	public void setSoTimeout(int n) { this.soTimeout = n;}
	
	public int getConnectionTimeout() { return this.connectionTimeout;}
	public void setConnectionTimeout(int n) { this.connectionTimeout = n;}
}

