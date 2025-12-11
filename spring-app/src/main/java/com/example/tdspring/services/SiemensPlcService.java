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

    public SiemensPlcService() {
        this.httpClient = createUnsafeOkHttpClient();
    }

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

    public boolean openLocker(int lockerId) {
        try {
            log.info("=== LOGIN + OPEN casier {} ===", lockerId);

            // 1) LOGIN
            String loginBody =
                    "{\"jsonrpc\":\"2.0\",\"method\":\"Api.Login\",\"id\":0," +
                            "\"params\":{\"user\":\"" + plcUser + "\",\"password\":\"" + plcPassword + "\"}}";

            RequestBody loginRequestBody = RequestBody.create(
                    loginBody,
                    MediaType.parse("application/json")
            );

            Request loginRequest = new Request.Builder()
                    .url(plcUrl + "/api/jsonrpc")
                    .post(loginRequestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            String token = null;

            try (Response loginResponse = httpClient.newCall(loginRequest).execute()) {
                String loginResponseBody = loginResponse.body() != null ? loginResponse.body().string() : "";
                log.info("Login status = {}", loginResponse.code());
                log.info("Login body   = {}", loginResponseBody);

                if (!loginResponse.isSuccessful() || !loginResponseBody.contains("\"token\"")) {
                    log.error("Login PLC échoué");
                    return false;
                }

                int start = loginResponseBody.indexOf("\"token\":\"") + 9;
                int end = loginResponseBody.indexOf("\"", start);
                token = loginResponseBody.substring(start, end);
                log.info("Token reçu = {}", token);
            }

            // 2) OUVERTURE CASIER
            String openBody =
                    "{\"jsonrpc\":\"2.0\",\"method\":\"PlcProgram.Write\",\"id\":1," +
                            "\"params\":{\"var\":\"\\\"Data\\\".Open_Casier_" + lockerId + "\",\"value\":true}}";

            RequestBody openRequestBody = RequestBody.create(
                    openBody,
                    MediaType.parse("application/json")
            );

            Request openRequest = new Request.Builder()
                    .url(plcUrl + "/api/jsonrpc")
                    .post(openRequestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Auth-Token", token)
                    .build();

            log.info("Ouverture casier {} body = {}", lockerId, openBody);

            try (Response openResponse = httpClient.newCall(openRequest).execute()) {
                String openResponseBody = openResponse.body() != null ? openResponse.body().string() : "null";

                log.info("Open status = {}", openResponse.code());
                log.info("Open body   = {}", openResponseBody);

                return openResponse.isSuccessful();
            }

        } catch (Exception e) {
            log.error("Erreur lors du login + ouverture casier {}", lockerId, e);
            return false;
        }
    }
}
