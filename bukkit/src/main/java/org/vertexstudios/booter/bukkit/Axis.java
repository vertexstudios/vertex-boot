package org.vertexstudios.booter.bukkit;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class Axis {
    public static final String HOST = "axis:3042"; 

    private static final Function<String, Axis.Response> ERROR_RESP = (msg) -> Axis.Response.builder().status(505).message(msg).build(); 

    public static Axis.Response fetch(String license, String build, byte[] privateKey) {
        try(CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost(HOST + "/decode");
            List<NameValuePair> params = new ArrayList<>();

            params.add(new BasicNameValuePair("license", license));
            params.add(new BasicNameValuePair("build", build));
            params.add(new BasicNameValuePair("key", new String(privateKey, StandardCharsets.UTF_8)));

            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            CloseableHttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (InputStream instream = entity.getContent()) {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(instream, Response.class);
                }
            }
            return ERROR_RESP.apply("Response without body.");
        } catch (Exception ex) {
            return ERROR_RESP.apply(ex.getMessage());
        }
    }

    @Builder
    @AllArgsConstructor
    public static class Response {
        @Getter private int status;
        @Getter private String message;
        @Getter private byte[] body;
    }
}
