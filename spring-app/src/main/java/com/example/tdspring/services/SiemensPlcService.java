package com.example.tdspring.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
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

    private static final MediaType JSON = MediaType.parse("application/json");

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

    /* =========================================================
     *  MÉTHODES PUBLIQUES - CASIERS
     * ========================================================= */

    public boolean openLocker(int lockerId) {
        log.info("=== OPEN casier {} ===", lockerId);
        try {
            String token = loginAndGetToken();
            if (token == null) {
                log.error("OpenLocker: login échoué");
                return false;
            }

            String openBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Open_Casier_" + lockerId + "\","
                    + "\"value\":true"
                    + "}"
                    + "}";

            String response = callJsonRpc(openBody, token);
            log.info("Open casier {} response = {}", lockerId, response);

            return response != null;
        } catch (Exception e) {
            log.error("Erreur lors de l'ouverture casier {}", lockerId, e);
            return false;
        }
    }

    public boolean closeLocker(int lockerId) {
        log.info("=== CLOSE casier {} ===", lockerId);
        try {
            String token = loginAndGetToken();
            if (token == null) {
                log.error("CloseLocker: login échoué");
                return false;
            }

            String closeBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Open_Casier_" + lockerId + "\","
                    + "\"value\":false"
                    + "}"
                    + "}";

            String response = callJsonRpc(closeBody, token);
            log.info("Close casier {} response = {}", lockerId, response);

            return response != null;
        } catch (Exception e) {
            log.error("Erreur lors de la fermeture casier {}", lockerId, e);
            return false;
        }
    }

    /* =========================================================
     *  MÉTHODES PUBLIQUES - SCAN DOUCHETTE
     * ========================================================= */

    /**
     * Lit la valeur scannée par la douchette depuis "Data".Scan
     * Retourne uniquement la partie avant le CR (13)
     */
    public String readScan() {
        try {
            String token = loginAndGetToken();
            if (token == null) return null;

            String readBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Read\","
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"mode\":\"raw\""
                    + "},"
                    + "\"id\":1"
                    + "}";

            String response = callJsonRpc(readBody, token);
            log.info("ReadScan raw response = {}", response);

            if (response == null || response.contains("\"error\"")) {
                return null;
            }

            String decoded = decodeUntilCR(response);
            log.info("ReadScan decoded value = '{}'", decoded);

            return decoded;

        } catch (Exception e) {
            log.error("Erreur readScan", e);
            return null;
        }
    }

    /**
     * Efface la variable "Data".Scan en écrivant des bytes à 0
     * Pour éviter de relire la même valeur au prochain polling
     */
    public boolean clearScan() {
        log.info("=== CLEAR \"Data\".Scan ===");
        try {
            String token = loginAndGetToken();
            if (token == null) {
                log.error("ClearScan: login échoué");
                return false;
            }

            // Écrit un tableau de 10 bytes à 0 (efface la variable)
            String clearBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"value\":\"\""  // ← String vide simple
                    + "}"
                    + "}";

            String response = callJsonRpc(clearBody, token);
            log.info("ClearScan response = {}", response);

            return response != null && !response.contains("\"error\"");

        } catch (Exception e) {
            log.error("Erreur clearScan", e);
            return false;
        }
    }

    /**
     * Health check : vérifie que le PLC répond correctement
     */
    public boolean healthCheck() {
        log.info("=== HEALTH CHECK PLC ===");
        try {
            String token = loginAndGetToken();
            if (token == null) {
                log.error("HealthCheck: login échoué");
                return false;
            }

            // Tente de lire la variable Scan pour vérifier la connexion
            String readBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Read\","
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"mode\":\"raw\""
                    + "},"
                    + "\"id\":1"
                    + "}";

            String response = callJsonRpc(readBody, token);

            if (response == null) {
                log.error("HealthCheck: pas de réponse");
                return false;
            }

            if (response.contains("\"error\"")) {
                log.error("HealthCheck: erreur PLC = {}", response);
                return false;
            }

            log.info("HealthCheck: PLC OK");
            return true;

        } catch (Exception e) {
            log.error("HealthCheck: exception", e);
            return false;
        }
    }

    /* =========================================================
     *  MÉTHODES PRIVÉES - PARSING
     * ========================================================= */

    /**
     * Decode les bytes et s'arrête au premier CR (13)
     * Skip les 2 premiers bytes (254 = þ, 73 = I)
     */
    private String decodeUntilCR(String jsonResponse) {
        try {
            int arrayStart = jsonResponse.indexOf("\"result\":[") + 10;
            int arrayEnd = jsonResponse.indexOf(']', arrayStart);

            if (arrayStart == -1 || arrayEnd == -1) {
                return null;
            }

            String[] bytes = jsonResponse.substring(arrayStart, arrayEnd).split(",");

            StringBuilder result = new StringBuilder();
            boolean skipHeader = true;

            for (String b : bytes) {
                int val = Integer.parseInt(b.trim());

                // STOP au premier CR (13)
                if (val == 13) {
                    break;
                }

                // Skip les 2 premiers bytes de header (254 = þ, 73 = I)
                if (skipHeader && (val == 254 || val == 73)) {
                    continue;
                }
                skipHeader = false;

                // Skip les null bytes
                if (val == 0) {
                    break;
                }

                result.append((char) val);
            }

            return result.toString().trim();

        } catch (Exception e) {
            log.error("Erreur décodage bytes", e);
            return null;
        }
    }

    /* =========================================================
     *  MÉTHODES PRIVÉES - COMMUNICATION PLC
     * ========================================================= */

    /**
     * Effectue un login et retourne le token, ou null en cas d'échec.
     */
    private String loginAndGetToken() throws IOException {
        String loginBody = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"method\":\"Api.Login\","
                + "\"id\":0,"
                + "\"params\":{"
                + "\"user\":\"" + plcUser + "\","
                + "\"password\":\"" + plcPassword + "\""
                + "}"
                + "}";

        RequestBody body = RequestBody.create(loginBody, JSON);

        Request request = new Request.Builder()
                .url(plcUrl + "/api/jsonrpc")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("Login status = {}", response.code());
            log.debug("Login body   = {}", responseBody);

            if (!response.isSuccessful() || !responseBody.contains("\"token\"")) {
                log.error("Login PLC échoué");
                return null;
            }

            // Extraction simple du token
            int start = responseBody.indexOf("\"token\":\"") + 9;
            int end = responseBody.indexOf("\"", start);
            String token = responseBody.substring(start, end);

            log.info("Token reçu = {}", token);
            return token;
        }
    }

    /**
     * Envoie une requête JSON-RPC au PLC avec token d'authentification.
     * Retourne le body en string ou null en cas d'erreur.
     */
    private String callJsonRpc(String jsonBody, String token) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(plcUrl + "/api/jsonrpc")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-Auth-Token", token)
                .build();

        log.debug("JSON-RPC request body = {}", jsonBody);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "null";
            log.info("JSON-RPC status = {}", response.code());
            log.debug("JSON-RPC body   = {}", responseBody);

            if (!response.isSuccessful()) {
                return null;
            }
            return responseBody;
        }
    }
}
