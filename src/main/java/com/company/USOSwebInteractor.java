package com.company;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Prototype of USOSweb dirty auth and basic data retrieval.
 * I have bad feeling that this code will be on production as-is...
 */
public class USOSwebInteractor {
    private static String URL_LOGIN = "https://login.uj.edu.pl/login?service=https%3A%2F%2Fwww.usosweb.uj.edu.pl%2Fkontroler.php%3F_action%3Dlogowaniecas%2Findex&locale=pl";
    private static String URL_HOME  = "https://www.usosweb.uj.edu.pl/kontroler.php?_action=home/index";
    private static String URL_REG_LIST = "https://www.usosweb.uj.edu.pl/kontroler.php?_action=news/rejestracje/rejJednostki&jed_org_kod=";

    private static String USER_LOGIN = "";
    private static String USER_PWD = "";

    private static void readAuthStrings() throws Exception /* ;_; */ {
        InputStream is;
        StringWriter writer;

        try {
            is = USOSwebInteractor.class.getResourceAsStream("/auth.txt");
            writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
        } catch (Exception ex){
            throw new Exception("auth.txt not found");
        }

        try{
            String fc = writer.toString();
            String[] tmp = fc.split("\\|");
            USER_LOGIN= URLEncoder.encode(tmp[0],"UTF-8");
            USER_PWD  = URLEncoder.encode(tmp[1],"UTF-8");
        } catch (Exception ex){
            throw new Exception("auth.txt has invalid format");
        }
    }

    private enum HTTP_TYPE {   GET, POST   }
    private static String httpQuery(
            CloseableHttpAsyncClient httpclient, String URL, HTTP_TYPE type,
            String regex, int patternMode,
            String payloadText
    ) throws Exception {
        HttpRequestBase requestBase;

        if (type==HTTP_TYPE.GET) {
            requestBase = new HttpGet(URL);
        }
        else if (type==HTTP_TYPE.POST) {
            requestBase = new HttpPost(URL);
            if (payloadText!=null){
                HttpEntity payloadRaw = new ByteArrayEntity(payloadText.getBytes("UTF-8"));
                ((HttpPost)requestBase).setEntity(payloadRaw);
                requestBase.setHeader("Content-Type","application/x-www-form-urlencoded");
            }
        }
        else {
            throw new Exception("Invalid query type");
        }

        System.out.println("Executing request " + requestBase.getRequestLine());
        Future<HttpResponse> future = httpclient.execute(requestBase, null);
        HttpResponse response = future.get();

        if (response.getStatusLine().toString().contains("HTTP/1.1 500")){
            throw new Exception("USOSweb panicked - HTTP 500");
        }

        HttpEntity responseRaw = response.getEntity();
        String responseText = EntityUtils.toString(responseRaw, "UTF-8");

        if (regex!=null && regex!=""){
            try{
                Pattern p = Pattern.compile(regex,patternMode);
                Matcher m = p.matcher(responseText); m.find();
                String matchText = m.group(1);
                return matchText;
            } catch (Exception ex){
                throw new Exception("Regex not matched");
            }
        }
        else{
            return responseText;
        }
    }



    public static void main(String[] args) throws Exception {
        readAuthStrings();

        CloseableHttpAsyncClient httpclient =
                HttpAsyncClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        httpclient.start();

        String loginFormToken = httpQuery(
                httpclient, URL_LOGIN, HTTP_TYPE.GET,
                "<input type=\"hidden\" name=\"lt\" value=\"(.*)\" />", Pattern.CANON_EQ,
                null);
        String indexNumber = httpQuery(
                httpclient, URL_LOGIN, HTTP_TYPE.POST,
                "indeksu.*?(\\d\\d\\d\\d\\d\\d\\d).*?span", Pattern.DOTALL,
                "username="+USER_LOGIN+"&password="+USER_PWD+"&lt="+loginFormToken+"&_eventId=submit&submit=zaloguj"
                );
        System.out.println("Numer indeksu: "+indexNumber);
        String factultyCode = httpQuery(
                httpclient, URL_HOME, HTTP_TYPE.POST,
                "katalog2/jednostki/pokazJednostke&kod=(.*?)'", Pattern.DOTALL,
                "username="+USER_LOGIN+"&password="+USER_PWD+"&lt="+loginFormToken+"&_eventId=submit&submit=zaloguj"
        );
        System.out.println("Kod jednostki: "+factultyCode);
        String registratiosnAvailable = httpQuery(
                httpclient, URL_REG_LIST+factultyCode, HTTP_TYPE.POST,
                Pattern.quote("<table class='grey' cellspacing='1px' style='margin-top:10px; border: 0'>")+"(.*)"+Pattern.quote("</table>"), Pattern.DOTALL,
                "username="+USER_LOGIN+"&password="+USER_PWD+"&lt="+loginFormToken+"&_eventId=submit&submit=zaloguj"
        );
        Pattern p = Pattern.compile("tura_id='(.*?)'");
        Matcher m = p.matcher(registratiosnAvailable);
        List<Integer> ra = new ArrayList<Integer>();
        registratiosnAvailable="";
        while (m.find()){
            ra.add(Integer.valueOf(m.group(1)));
            registratiosnAvailable += m.group(1)+", ";
        }
        System.out.println("Dostępne rejestracje: "+registratiosnAvailable);

        User user = new User(indexNumber,factultyCode,ra);

        httpclient.close();
    }
}

