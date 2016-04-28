package com.company;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.util.EntityUtils;
import sun.misc.IOUtils;

import javax.xml.ws.spi.http.HttpContext;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple example that uses HttpClient to execute an HTTP request against
 * a target site that requires user authentication.
 */
public class Main {
    private static String URL_LOGIN = "https://login.uj.edu.pl/login?service=https%3A%2F%2Fwww.usosweb.uj.edu.pl%2Fkontroler.php%3F_action%3Dlogowaniecas%2Findex&locale=pl";

    public static void main(String[] args) throws Exception {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        httpclient.start();
        try {
            HttpGet httpget = new HttpGet(URL_LOGIN);
            System.out.println("Executing request " + httpget.getRequestLine());
            Future<HttpResponse> future = httpclient.execute(httpget, null);
            HttpResponse response = future.get();
            System.out.println("Response: " + response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            Pattern p = Pattern.compile("<input type=\"hidden\" name=\"lt\" value=\"(.*)\" />");
            Matcher m = p.matcher(responseString);
            m.find(); String  jakis_token = m.group(1);
            System.out.println("jakis_token = "+jakis_token);

            HttpPost httpPost = new HttpPost(URL_LOGIN);
            String postparams = "username=daniel.skowronski%40student.uj.edu.pl&password=xD&lt="+jakis_token+"&_eventId=submit&submit=zaloguj";
            entity = new ByteArrayEntity(postparams.getBytes("UTF-8"));
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type","application/x-www-form-urlencoded");
            System.out.println("Executing request " + httpPost.getRequestLine());
            future = httpclient.execute(httpPost,null);
            response = future.get();
            System.out.println("Response: " + response.getStatusLine());
            entity = response.getEntity();
            responseString = EntityUtils.toString(entity, "UTF-8");

            System.out.println("odp = "+responseString);


        } finally {
            System.out.println("Shutting down");
            httpclient.close();
        }
    }
}
