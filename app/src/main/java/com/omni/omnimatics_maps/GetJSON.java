package com.omni.omnimatics_maps;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Kit on 27/10/2017.
 */

public class GetJSON {

    OkHttpClient client = new OkHttpClient();
    public String getJSONRequest(String url, RequestBody json_body) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String MyResult = response.body().string();
        System.out.println("getJSONRequest response.body : "+MyResult);
        return MyResult ;
    }
}