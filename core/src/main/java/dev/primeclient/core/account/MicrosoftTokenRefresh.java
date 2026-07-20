package dev.primeclient.core.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Refreshes a Microsoft account into a live Minecraft access token (same flow as the launcher). */
public final class MicrosoftTokenRefresh {

    public record Result(String accessToken, String refreshToken, String username, String uuid) {
    }

    private MicrosoftTokenRefresh() {
    }

    public static Result refresh(String refreshToken, String clientId) throws IOException {
        JsonObject token = postForm(
                "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                "client_id=" + enc(clientId)
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + enc(refreshToken)
                        + "&scope=" + enc("XboxLive.signin offline_access"));
        String msAccess = req(token, "access_token");
        String newRefresh = token.has("refresh_token")
                ? token.get("refresh_token").getAsString()
                : refreshToken;

        JsonObject xboxBody = new JsonObject();
        JsonObject xboxProps = new JsonObject();
        xboxProps.addProperty("AuthMethod", "RPS");
        xboxProps.addProperty("SiteName", "user.auth.xboxlive.com");
        xboxProps.addProperty("RpsTicket", "d=" + msAccess);
        xboxBody.add("Properties", xboxProps);
        xboxBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xboxBody.addProperty("TokenType", "JWT");
        JsonObject xbox = postJson("https://user.auth.xboxlive.com/user/authenticate", xboxBody);
        String xboxToken = req(xbox, "Token");
        String uhs = xbox.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString();

        JsonObject xstsBody = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken);
        xstsProps.add("UserTokens", userTokens);
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
        String xstsToken = req(xsts, "Token");

        JsonObject mcLogin = new JsonObject();
        mcLogin.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        JsonObject mc = postJson(
                "https://api.minecraftservices.com/authentication/login_with_xbox", mcLogin);
        String mcToken = req(mc, "access_token");

        JsonObject profile = getJson(
                "https://api.minecraftservices.com/minecraft/profile", mcToken);
        String rawId = req(profile, "id");
        String name = profile.has("name") ? profile.get("name").getAsString() : "Player";
        String dashed = dashUuid(rawId);
        return new Result(mcToken, newRefresh, name, dashed);
    }

    private static String dashUuid(String uuid) {
        if (uuid.contains("-")) {
            return uuid;
        }
        if (uuid.length() != 32) {
            return uuid;
        }
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-"
                + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-"
                + uuid.substring(20, 32);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String req(JsonObject o, String key) throws IOException {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new IOException("Missing field: " + key);
        }
        return o.get(key).getAsString();
    }

    private static JsonObject postForm(String url, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Prime-Client/1.2");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readJson(conn);
    }

    private static JsonObject postJson(String url, JsonObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Prime-Client/1.2");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return readJson(conn);
    }

    private static JsonObject getJson(String url, String bearer) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Authorization", "Bearer " + bearer);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Prime-Client/1.2");
        return readJson(conn);
    }

    private static JsonObject readJson(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = "";
        if (stream != null) {
            body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (code >= 400) {
            throw new IOException("HTTP " + code + (body.isBlank() ? "" : ": " + body.lines().limit(2).collect(Collectors.joining(" "))));
        }
        return JsonParser.parseString(body).getAsJsonObject();
    }
}
