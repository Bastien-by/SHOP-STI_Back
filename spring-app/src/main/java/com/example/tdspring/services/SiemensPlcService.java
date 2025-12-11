package com.example.tdspring.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SiemensPlcService {

    private final OkHttpClient httpClient;

    @Value("${PLC_URL}")
    private String plcUrl;

    @Value("${PLC_USER}")
    private String plcUser;

    @Value("${PLC_PASSWORD}")
    private String plcPassword;

    private String token;

    public SiemensPlcService() {
        this.httpClient = createUnsafeOkHttpClient();
    }

    // Client HTTP qui accepte les certificats auto-signés (PLC)
    private OkHttpClient createUnsafeOkHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean ping() {
        try {
            log.info("=== PING PLC {} ===", plcUrl);

            String jsonBody = "{\"jsonrpc\":\"2.0\",\"method\":\"Api.Ping\",\"id\":1}";

            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(plcUrl + "/api/jsonrpc")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            log.info("Requête Ping body = {}", jsonBody);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "null";

                log.info("Ping status = {}", response.code());
                log.info("Ping body   = {}", responseBody);

                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("Ping PLC échoué", e);
            return false;
        }
    }

    public boolean login() {
        try {
            log.info("=== LOGIN PLC {} avec user {} ===", plcUrl, plcUser);

            String jsonBody =
                    "{\"jsonrpc\":\"2.0\",\"method\":\"Api.Login\",\"id\":0," +
                            "\"params\":{\"user\":\"" + plcUser + "\",\"password\":\"" + plcPassword + "\"}}";

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(plcUrl + "/api/jsonrpc")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            log.info("Requête Login body = {}", jsonBody);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                log.info("Login status = {}", response.code());
                log.info("Login body   = {}", responseBody);

                if (response.isSuccessful() && responseBody.contains("\"token\"")) {
                    int start = responseBody.indexOf("\"token\":\"") + 9;
                    int end = responseBody.indexOf("\"", start);
                    this.token = responseBody.substring(start, end);
                    log.info("Token reçu = {}", token);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Login PLC échoué", e);
            return false;
        }
    }

    public String getToken() {
        return token;
    }

    public boolean openLocker(int lockerId) {
        try {
            if (token == null && !login()) {
                log.error("Impossible de se logger au PLC, abandon ouverture casier {}", lockerId);
                return false;
            }

            String jsonBody =
                    "{\"jsonrpc\":\"2.0\",\"method\":\"PlcProgram.Write\",\"id\":1," +
                            "\"params\":{\"var\":\"DB_Casiers.Casier" + lockerId + ".Ouverture\",\"value\":true}}";

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(plcUrl + "/api/jsonrpc")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Auth-Token", token)
                    .build();

            log.info("Ouverture casier {} body = {}", lockerId, jsonBody);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "null";

                log.info("Open status = {}", response.code());
                log.info("Open body   = {}", responseBody);

                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'ouverture du casier {}", lockerId, e);
            return false;
        }
    }
}
