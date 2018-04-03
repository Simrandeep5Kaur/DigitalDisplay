package com.kmsg.digitaldisplay.util;

import android.content.ContentValues;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;


public class JSONParser {

    private InputStream is = null;

    public JSONParser() {
    }


    public JSONObject makeHttpRequest(String url, String method, ContentValues param) {
        is = null;
        param.put("uKey", "ba0d1890-b3b4-4ee2-8d06-a210a9932a7a");

        UtilityServices.appendLog(param.toString());

        // Making HTTP request
        try {
            // check for request method
            switch (method) {
                case "POST": {
                    // request method is POST
                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                    con.setRequestMethod("POST");
                    con.setDoInput(true);
                    if (param.size() > 0) {
                        con.setDoOutput(true);
                        String query = getQuery(param);
                        OutputStream os = con.getOutputStream();
                        os.write(query.getBytes());
                        os.flush();
                        os.close();
                    }

                    is = new BufferedInputStream(con.getInputStream());
                    break;
                }
                case "PUT": {
                    // request method is PUT
                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                    con.setRequestMethod("PUT");
                    con.setDoInput(true);
                    if (param.size() > 0) {
                        con.setDoOutput(true);
                        String query = getQuery(param);
                        OutputStream os = con.getOutputStream();
                        os.write(query.getBytes());
                        os.flush();
                        os.close();
                    }
                    con.connect();
                    is = new BufferedInputStream(con.getInputStream());
                    break;
                }
                case "GET": {
                    // request method is GET
                    if (param.size() > 0) {
                        // have param
                        url += "?" + getQuery(param);
                    }
                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                    is = new BufferedInputStream(con.getInputStream());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getJsonFromResponse(is);
    }


    private String getQuery(ContentValues param) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : param.valueSet()) {
            if (first)
                first = false;
            else
                sb.append("&");

            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
        }
        return sb.toString();
    }


    private JSONObject getJsonFromResponse(InputStream is) {
        String json = "";
        JSONObject obj = new JSONObject();
        try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(
                    is), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
        // parse the string to a JSON object

        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }

        UtilityServices.appendLog(obj.toString());
        return obj;
    }

}
