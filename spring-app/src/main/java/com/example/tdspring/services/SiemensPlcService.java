package com.example.tdspring.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Token JSON-RPC mis en cache et réutilisé entre les appels.
    // Avant ce cache, chaque opération (readScan, clearScan, openLocker...)
    // relogguait systématiquement auprès du web server du S7-1200, ce qui
    // doublait le nombre de requêtes et surchargeait l'automate en polling.
    private volatile String cachedToken;
    private final Object tokenLock = new Object();

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
     *  CASIERS — COMMANDES INDIVIDUELLES
     * ========================================================= */

    public boolean openLocker(int lockerId) {
        log.info("=== OPEN casier {} ===", lockerId);
        try {
            String openBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Open_Casier_" + lockerId + "\","
                    + "\"value\":true"
                    + "}"
                    + "}";

            String response = executeAuthenticated(openBody);
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
            String closeBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Open_Casier_" + lockerId + "\","
                    + "\"value\":false"
                    + "}"
                    + "}";

            String response = executeAuthenticated(closeBody);
            log.info("Close casier {} response = {}", lockerId, response);
            return response != null;
        } catch (Exception e) {
            log.error("Erreur lors de la fermeture casier {}", lockerId, e);
            return false;
        }
    }

    /* =========================================================
     *  CASIERS — COMMANDES BATCH (plusieurs casiers, 1 seule requête)
     * =========================================================
     *
     * JSON-RPC 2.0 supporte les "batch requests" : un tableau de requêtes
     * dans un seul body HTTP. Le S7-1200 répond avec un tableau de résultats
     * dans le même ordre.
     *
     * Exemple body envoyé pour ouvrir les casiers 1, 3 et 7 :
     * [
     *   {"jsonrpc":"2.0","method":"PlcProgram.Write","id":1,"params":{"var":"\"Data\".Open_Casier_1","value":true}},
     *   {"jsonrpc":"2.0","method":"PlcProgram.Write","id":2,"params":{"var":"\"Data\".Open_Casier_3","value":true}},
     *   {"jsonrpc":"2.0","method":"PlcProgram.Write","id":3,"params":{"var":"\"Data\".Open_Casier_7","value":true}}
     * ]
     *
     * Réponse du PLC :
     * [
     *   {"jsonrpc":"2.0","id":1,"result":true},
     *   {"jsonrpc":"2.0","id":2,"result":true},
     *   {"jsonrpc":"2.0","id":3,"result":true}
     * ]
     */

    /**
     * Ouvre une liste de casiers en UNE SEULE requête JSON-RPC batch.
     *
     * @param lockerIds liste des numéros de casiers à ouvrir
     * @return liste des IDs effectivement ouverts (réponse PLC sans erreur)
     */
    public List<Integer> openLockers(List<Integer> lockerIds) {
        log.info("=== OPEN BATCH casiers {} ===", lockerIds);
        return sendBatchLockerCommand(lockerIds, true);
    }

    /**
     * Ferme une liste de casiers en UNE SEULE requête JSON-RPC batch.
     *
     * @param lockerIds liste des numéros de casiers à fermer
     * @return liste des IDs effectivement fermés (réponse PLC sans erreur)
     */
    public List<Integer> closeLockers(List<Integer> lockerIds) {
        log.info("=== CLOSE BATCH casiers {} ===", lockerIds);
        return sendBatchLockerCommand(lockerIds, false);
    }

    /**
     * Construit et envoie une requête JSON-RPC batch pour ouvrir ou fermer
     * une liste de casiers en un seul appel HTTP vers le S7-1200.
     *
     * Utilise l'id JSON-RPC (1-based) pour relier chaque réponse au casier
     * correspondant dans la liste d'entrée (id - 1 = index).
     */
    private List<Integer> sendBatchLockerCommand(List<Integer> lockerIds, boolean open) {
        List<Integer> succeeded = new ArrayList<>();

        if (lockerIds == null || lockerIds.isEmpty()) {
            return succeeded;
        }

        try {
            // ── Construction du body batch ────────────────────────────────
            // Chaque requête individuelle a un "id" = index + 1 pour pouvoir
            // retrouver le casier correspondant dans le tableau de réponses.
            StringBuilder batchBody = new StringBuilder("[");
            for (int i = 0; i < lockerIds.size(); i++) {
                int lockerId = lockerIds.get(i);
                if (i > 0) batchBody.append(",");
                batchBody.append("{")
                        .append("\"jsonrpc\":\"2.0\",")
                        .append("\"method\":\"PlcProgram.Write\",")
                        .append("\"id\":").append(i + 1).append(",")
                        .append("\"params\":{")
                        .append("\"var\":\"\\\"Data\\\".Open_Casier_").append(lockerId).append("\",")
                        .append("\"value\":").append(open)
                        .append("}}");
            }
            batchBody.append("]");

            log.info("Batch {} body = {}", open ? "OPEN" : "CLOSE", batchBody);

            // ── Envoi de la requête batch (un seul appel HTTP) ────────────
            String response = executeAuthenticated(batchBody.toString());
            log.info("Batch {} response = {}", open ? "OPEN" : "CLOSE", response);

            if (response == null) {
                log.error("Batch: réponse null du PLC");
                return succeeded;
            }

            // ── Parsing de la réponse batch ───────────────────────────────
            // On cherche chaque objet JSON de la réponse, on extrait son "id"
            // pour retrouver le casier, et on vérifie l'absence d'erreur.
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            // Découpe grossièrement le tableau de réponses en items individuels
            // (suffisant car chaque item est un objet JSON simple à 1 niveau)
            String[] items = response
                    .replaceAll("^\\s*\\[\\s*", "")   // retire le [ initial
                    .replaceAll("\\s*\\]\\s*$", "")   // retire le ] final
                    .split("\\},\\s*\\{");             // sépare les objets

            for (String item : items) {
                Matcher idMatcher = idPattern.matcher(item);
                boolean hasError  = item.contains("\"error\"");
                boolean hasResult = item.contains("\"result\"");

                if (idMatcher.find() && hasResult && !hasError) {
                    int rpcId = Integer.parseInt(idMatcher.group(1));
                    int index = rpcId - 1;
                    if (index >= 0 && index < lockerIds.size()) {
                        int casier = lockerIds.get(index);
                        succeeded.add(casier);
                        log.info("✅ Casier {} {} avec succès", casier, open ? "ouvert" : "fermé");
                    }
                } else if (idMatcher.find()) {
                    int rpcId = Integer.parseInt(idMatcher.group(1));
                    int index = rpcId - 1;
                    if (index >= 0 && index < lockerIds.size()) {
                        log.warn("⚠️ Casier {} : erreur PLC dans la réponse batch — item = {}",
                                lockerIds.get(index), item);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erreur batch {} casiers {}", open ? "open" : "close", lockerIds, e);
        }

        return succeeded;
    }

    /* =========================================================
     *  SCAN DOUCHETTE
     * ========================================================= */

    /**
     * Lit la valeur scannée par la douchette depuis "Data".Scan
     * Retourne uniquement la partie avant le CR (13)
     */
    public String readScan() {
        try {
            String readBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Read\","
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"mode\":\"raw\""
                    + "},"
                    + "\"id\":1"
                    + "}";

            String response = executeAuthenticated(readBody);
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
     * Efface la variable "Data".Scan en écrivant une chaîne vide.
     * Évite de relire la même valeur au prochain polling.
     */
    public boolean clearScan() {
        log.info("=== CLEAR \"Data\".Scan ===");
        try {
            // Écrire un espace " " — le S7 accepte une string non-vide
            // decodeUntilCR ignorera un espace grâce au .trim() final
            String clearBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"value\":\" \""   // ← un espace, pas une string vide
                    + "}"
                    + "}";

            String response = executeAuthenticated(clearBody);
            log.info("ClearScan response = {}", response);
            return response != null && !response.contains("\"error\"");

        } catch (Exception e) {
            log.error("Erreur clearScan", e);
            return false;
        }
    }

    /* =========================================================
     *  MANAGE ALL
     * ========================================================= */

    /**
     * Écrit dans la variable Manage_All (true = ouvrir tout, false = fermer tout)
     */
    public boolean writeManageAll(boolean openAll) {
        log.info("=== WRITE Manage_All = {} ===", openAll);
        try {
            String writeBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Write\","
                    + "\"id\":1,"
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Manage_All\","
                    + "\"value\":" + openAll
                    + "}"
                    + "}";

            log.info("Payload Manage_All: {}", writeBody);
            String response = executeAuthenticated(writeBody);
            log.info("WriteManageAll response = {}", response);

            if (response != null && !response.contains("\"error\"")) {
                log.info("✅ Manage_All = {} écrit avec succès", openAll);
                return true;
            } else {
                log.error("❌ Erreur JSON-RPC Manage_All: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Erreur writeManageAll = {}", openAll, e);
            return false;
        }
    }

    /* =========================================================
     *  HEALTH CHECK
     * ========================================================= */

    public boolean healthCheck() {
        log.info("=== HEALTH CHECK PLC ===");
        try {
            String readBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"method\":\"PlcProgram.Read\","
                    + "\"params\":{"
                    + "\"var\":\"\\\"Data\\\".Scan\","
                    + "\"mode\":\"raw\""
                    + "},"
                    + "\"id\":1"
                    + "}";

            String response = executeAuthenticated(readBody);

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
     *  MÉTHODES PRIVÉES — PARSING
     * ========================================================= */

    /**
     * Décode les bytes de la réponse et s'arrête au premier CR (13).
     * Skip les 2 premiers bytes de header (254 = þ, 73 = I).
     */
    private String decodeUntilCR(String jsonResponse) {
        try {
            int arrayStart = jsonResponse.indexOf("\"result\":[") + 10;
            int arrayEnd   = jsonResponse.indexOf(']', arrayStart);

            if (arrayStart == 9 || arrayEnd == -1) {
                return null;
            }

            String[] bytes = jsonResponse.substring(arrayStart, arrayEnd).split(",");
            StringBuilder result = new StringBuilder();

            // Structure d'un STRING Siemens en mode "raw" :
            // Byte 0 : longueur max déclarée du String (ex: 254 = 0xFE)
            // Byte 1 : longueur actuelle de la chaîne  (ex: 6 pour "012244")
            // Byte 2+: les caractères ASCII de la chaîne
            // → On skip les 2 premiers bytes de metadata, on lit les suivants

            if (bytes.length < 3) return null;

            int maxLen    = Integer.parseInt(bytes[0].trim()); // longueur max (ignorée)
            int actualLen = Integer.parseInt(bytes[1].trim()); // longueur réelle utile

            for (int i = 2; i < bytes.length; i++) {
                int val = Integer.parseInt(bytes[i].trim());

                if (val == 0 || val == 13) break; // NULL ou CR = fin de chaîne

                result.append((char) val);
            }

            // On tronque à la longueur réelle annoncée par l'automate
            String decoded = result.toString();
            if (actualLen > 0 && actualLen < decoded.length()) {
                decoded = decoded.substring(0, actualLen);
            }

            return decoded.trim();

        } catch (Exception e) {
            log.error("Erreur décodage bytes", e);
            return null;
        }
    }

    /* =========================================================
     *  MÉTHODES PRIVÉES — COMMUNICATION PLC
     * ========================================================= */

    /**
     * Retourne le token en cache s'il existe, sinon se logue une seule fois.
     * Plusieurs threads peuvent appeler ceci en parallèle (plusieurs postes
     * qui scannent en même temps) : le login effectif est synchronisé pour
     * n'ouvrir qu'une seule session auprès du PLC.
     */
    private String getToken() throws IOException {
        String token = cachedToken;
        if (token != null) {
            return token;
        }
        synchronized (tokenLock) {
            if (cachedToken == null) {
                cachedToken = loginAndGetToken();
            }
            return cachedToken;
        }
    }

    private void invalidateToken() {
        synchronized (tokenLock) {
            cachedToken = null;
        }
    }

    /**
     * Exécute une requête JSON-RPC en réutilisant le token en cache.
     * Si le PLC répond par une erreur (session expirée par exemple), le
     * token est invalidé et l'appel est retenté une seule fois avec un
     * nouveau login, sans faire planter l'appelant.
     */
    private String executeAuthenticated(String jsonBody) throws IOException {
        String token = getToken();
        if (token == null) {
            return null;
        }

        String response = callJsonRpc(jsonBody, token);

        if (response == null || response.contains("\"error\"")) {
            log.warn("Appel JSON-RPC en échec avec le token en cache, ré-authentification...");
            invalidateToken();
            token = getToken();
            if (token == null) {
                return null;
            }
            response = callJsonRpc(jsonBody, token);
        }

        return response;
    }

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

            int start = responseBody.indexOf("\"token\":\"") + 9;
            int end   = responseBody.indexOf("\"", start);
            String token = responseBody.substring(start, end);

            log.info("Token reçu = {}", token);
            return token;
        }
    }

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