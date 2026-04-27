package com.gestionaudit.controllers;

import com.gestionaudit.utils.BadWordEnforcement;
import com.gestionaudit.utils.MicToWavRecorder;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ChatbotController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatHistory;
    @FXML private TextField messageField;
    @FXML private Button btnMic;
    @FXML private WebView speechWebView;

    private boolean isRecording = false;
    private final MicToWavRecorder micRecorder = new MicToWavRecorder();

    private static final String API_KEY = "AIzaSyBKa9Cew_xKMBRs_WsCdinT4BU-Vfhp3kw";
    /**
     * v1beta only: {@code systemInstruction} is not accepted on the v1 generateContent API (HTTP 400).
     */
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    /** Single model as requested — chat + dictée use this endpoint only. */
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    private static final String AUDIT_SYSTEM_INSTRUCTION =
            "Tu es l'assistant « Gestion Audit » d'une application de gestion des réclamations et d'audit interne.\n"
                    + "Tu réponds UNIQUEMENT en français.\n"
                    + "Tu traites exclusivement : réclamations clients, statuts (en_attente, en_cours, resolue, cloturee), "
                    + "priorités, catégories, bonnes pratiques de suivi, communication client/admin, conformité légère, "
                    + "organisation du traitement des plaintes, et questions liées à l'usage de l'application Gestion Audit.\n"
                    + "Si la question n'a aucun lien avec l'audit ou la gestion des réclamations, refuse en UNE courte phrase "
                    + "et invite à poser une question métier (sans donner de contenu hors sujet).\n"
                    + "Réponses concises et professionnelles. Ne divulgue pas de clés API ni de données sensibles fictives.";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(12))
            .build();

    @FXML
    public void initialize() {
        /* WebView does not support Web Speech API — keep empty to avoid JS errors if touched */
        speechWebView.getEngine().loadContent("<html><head><meta charset='UTF-8'></head><body></body></html>");
    }

    @FXML
    private void toggleRecording() {
        javafx.stage.Window win = messageField.getScene() != null ? messageField.getScene().getWindow() : null;
        if (!isRecording) {
            if (!micRecorder.isLineAvailable()) {
                Notifications.create()
                        .owner(win)
                        .title("Microphone indisponible")
                        .text("Aucune entrée audio compatible (16 kHz mono). Dictée impossible — saisissez le texte au clavier.")
                        .position(Pos.TOP_RIGHT)
                        .hideAfter(Duration.seconds(5))
                        .showWarning();
                return;
            }
            try {
                micRecorder.start();
                isRecording = true;
                btnMic.setText("🛑");
                btnMic.setStyle("-fx-font-size: 18px; -fx-text-fill: #ef4444; -fx-border-color: #ef4444;");
                Notifications.create()
                        .owner(win)
                        .title("Dictée")
                        .text("Enregistrement… Cliquez à nouveau sur le micro pour arrêter et transcrire.")
                        .position(Pos.TOP_RIGHT)
                        .hideAfter(Duration.seconds(3))
                        .showInformation();
            } catch (LineUnavailableException e) {
                Notifications.create()
                        .owner(win)
                        .title("Microphone")
                        .text("Impossible d'ouvrir le micro : " + e.getMessage())
                        .showError();
            }
        } else {
            CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] wav = micRecorder.stopAndGetWav();
                    if (wav.length == 0) {
                        return "";
                    }
                    return transcribeAudioWithGemini(wav);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "_ERR_" + e.getMessage();
                }
            }).thenAccept(result -> Platform.runLater(() -> {
                isRecording = false;
                btnMic.setText("🎤");
                btnMic.setStyle("-fx-font-size: 18px;");
                if (result.startsWith("_ERR_")) {
                    Notifications.create()
                            .owner(win)
                            .title("Transcription")
                            .text(result.substring(7))
                            .position(Pos.TOP_RIGHT)
                            .hideAfter(Duration.seconds(6))
                            .showError();
                } else if (!result.isBlank()) {
                    messageField.setText(result.trim());
                    Notifications.create()
                            .owner(win)
                            .title("Dictée")
                            .text("Texte inséré — vous pouvez envoyer ou modifier avant envoi.")
                            .position(Pos.TOP_RIGHT)
                            .hideAfter(Duration.seconds(2))
                            .showInformation();
                } else {
                    Notifications.create()
                            .owner(win)
                            .title("Dictée")
                            .text("Audio trop court. Réessayez en parlant un peu plus longtemps.")
                            .position(Pos.TOP_RIGHT)
                            .hideAfter(Duration.seconds(4))
                            .showWarning();
                }
            }));
        }
    }

    /** Transcription only (no audit system prompt) — raw audio → French text. */
    private String transcribeAudioWithGemini(byte[] wav) throws Exception {
        String b64 = Base64.getEncoder().encodeToString(wav);

        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text",
                "Transcris fidèlement cet enregistrement audio en français. "
                        + "Réponds uniquement par la transcription brute, sans guillemets ni phrase d'introduction."));
        JSONObject inline = new JSONObject();
        inline.put("mime_type", "audio/wav");
        inline.put("data", b64);
        parts.put(new JSONObject().put("inline_data", inline));

        JSONObject userTurn = new JSONObject();
        userTurn.put("role", "user");
        userTurn.put("parts", parts);

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("contents", new JSONArray().put(userTurn));

        try {
            String body = postGeminiJson(jsonRequest.toString());
            return extractGeminiReplyText(body);
        } catch (Exception e) {
            return "_ERR_" + e.getMessage();
        }
    }

    @FXML
    private void handleSend() {
        String msg = messageField.getText();
        if (msg.isEmpty()) return;

        javafx.stage.Window win = messageField.getScene() != null ? messageField.getScene().getWindow() : null;
        if (BadWordEnforcement.blockIfViolating(msg, win)) {
            return;
        }

        addMessageToChat(msg, true);
        messageField.clear();

        callGeminiAPI(msg).thenAccept(reply -> Platform.runLater(() -> {
            addMessageToChat(reply, false);
            Notifications.create()
                    .title("IA Gestion Audit")
                    .text("Nouvelle réponse reçue de l'assistant.")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .showInformation();
        })).exceptionally(ex -> {
            Platform.runLater(() -> addMessageToChat("Désolé, j'ai rencontré un problème technique.", false));
            ex.printStackTrace();
            return null;
        });
    }

    private void addMessageToChat(String content, boolean isUser) {
        String safe = content == null ? "" : content;
        HBox row = new HBox();
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        /* Row fills the thread width (ScrollPane fitToWidth); avoids bubble width collapsing to label pref */
        row.prefWidthProperty().bind(chatHistory.widthProperty());

        VBox bubble = new VBox(5);
        bubble.getStyleClass().add(isUser ? "chat-bubble-client" : "chat-bubble-admin");
        /* Max width from content area — do NOT bind label to bubble.width (circular layout → ultra-narrow wrap) */
        var bubbleMax = Bindings.createDoubleBinding(
                () -> Math.min(520, Math.max(220, chatHistory.getWidth() - 32)),
                chatHistory.widthProperty());
        bubble.maxWidthProperty().bind(bubbleMax);

        Label textLabel = new Label(safe);
        textLabel.setWrapText(true);
        textLabel.setTextOverrun(OverrunStyle.CLIP);
        textLabel.maxWidthProperty().bind(bubbleMax.subtract(36));
        textLabel.getStyleClass().add("chat-bubble-text");
        if (isUser) {
            textLabel.setStyle("-fx-text-fill: white;");
        } else {
            textLabel.setStyle("-fx-text-fill: #0f172a;");
        }

        bubble.getChildren().add(textLabel);
        row.getChildren().add(bubble);
        chatHistory.getChildren().add(row);

        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private CompletableFuture<String> callGeminiAPI(String prompt) {
        JSONObject jsonRequest = new JSONObject();

        JSONArray sysParts = new JSONArray();
        sysParts.put(new JSONObject().put("text", AUDIT_SYSTEM_INSTRUCTION));
        JSONObject systemInstruction = new JSONObject();
        systemInstruction.put("parts", sysParts);
        jsonRequest.put("systemInstruction", systemInstruction);

        JSONArray contents = new JSONArray();
        JSONObject userTurn = new JSONObject();
        userTurn.put("role", "user");
        JSONArray userParts = new JSONArray();
        userParts.put(new JSONObject().put("text", prompt));
        userTurn.put("parts", userParts);
        contents.put(userTurn);
        jsonRequest.put("contents", contents);

        final String jsonBody = jsonRequest.toString();
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = postGeminiJson(jsonBody);
                return extractGeminiReplyText(body);
            } catch (Exception e) {
                e.printStackTrace();
                return "Service Gemini indisponible ou surchargé (503/429). Réessayez dans un instant.\n"
                        + "(" + e.getMessage() + ")";
            }
        });
    }

    /**
     * POST {@code gemini-2.5-flash:generateContent} on v1beta. Retries on HTTP 429/503 with short pauses
     * (previous exponential backoff made overload feel very slow).
     */
    private static final int GEMINI_HTTP_TIMEOUT_SEC = 55;
    private static final int GEMINI_MAX_ATTEMPTS = 5;

    /** ~0.3s, 1.2s, 2.7s, 3.5s cap — total wait much lower than 800×2^n while still spacing retries. */
    private static long geminiRetryBackoffMs(int attemptZeroBased) {
        long quadratic = 300L * (attemptZeroBased + 1L) * (attemptZeroBased + 1L);
        return Math.min(3500L, quadratic) + (long) (Math.random() * 180L);
    }

    private String postGeminiJson(String jsonBody) throws IOException, InterruptedException {
        String url = GEMINI_API_BASE + GEMINI_MODEL + ":generateContent?key=" + API_KEY;
        for (int attempt = 0; attempt < GEMINI_MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(GEMINI_HTTP_TIMEOUT_SEC))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 200) {
                return response.body();
            }
            if (code == 429 || code == 503) {
                Thread.sleep(geminiRetryBackoffMs(attempt));
                continue;
            }
            throw new IOException(GEMINI_MODEL + " HTTP " + code + ": " + abbreviateBody(response.body()));
        }
        throw new IOException("Surcharge serveur (503/429) sur " + GEMINI_MODEL + " — réessayez dans un instant.");
    }

    private static String abbreviateBody(String body) {
        if (body == null) {
            return "";
        }
        String t = body.replace("\n", " ");
        return t.length() > 280 ? t.substring(0, 280) + "…" : t;
    }

    private static String extractGeminiReplyText(String body) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.has("error")) {
                JSONObject err = json.optJSONObject("error");
                return "Erreur API : " + (err != null ? err.optString("message", "inconnue") : "inconnue");
            }
            if (!json.has("candidates") || json.getJSONArray("candidates").length() == 0) {
                if (json.has("promptFeedback")) {
                    return "Impossible de répondre (filtre de sécurité). Posez une question sur les réclamations ou l'audit.";
                }
                return "Aucune réponse. Reformulez une question liée à la gestion des réclamations.";
            }
            JSONObject cand = json.getJSONArray("candidates").getJSONObject(0);
            String finish = cand.optString("finishReason", "");
            if ("SAFETY".equalsIgnoreCase(finish) || "BLOCKLIST".equalsIgnoreCase(finish)) {
                return "Réponse bloquée. Restez sur le thème des réclamations et de l'audit.";
            }
            if (!cand.has("content")) {
                return "Réponse vide. Reformulez votre question (contexte Gestion Audit).";
            }
            JSONObject content = cand.getJSONObject("content");
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) {
                return "Réponse vide du modèle.";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.getJSONObject(i);
                if (p.has("text")) {
                    sb.append(p.getString("text"));
                }
            }
            String t = sb.toString().trim();
            return t.isEmpty() ? "Réponse vide." : t;
        } catch (Exception e) {
            return "Impossible de lire la réponse du serveur.";
        }
    }
}