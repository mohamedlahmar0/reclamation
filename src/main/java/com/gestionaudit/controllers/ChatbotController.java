package com.gestionaudit.controllers;

import com.gestionaudit.utils.BadWordEnforcement;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import javafx.geometry.Pos;
import netscape.javascript.JSObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ChatbotController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatHistory;
    @FXML private TextField messageField;
    @FXML private Button btnMic;
    @FXML private WebView speechWebView;

    private boolean isRecording = false;

    private static final String API_KEY = "AIzaSyDcrjj2F2DGcmEnlUOplVCs3Hwl8Brc1mg";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        setupSpeechRecognition();
    }

    private void setupSpeechRecognition() {
        speechWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) speechWebView.getEngine().executeScript("window");
                window.setMember("javaApp", new SpeechBridge());
            }
        });

        // Load a minimal HTML that uses the Web Speech API
        String html = "<html><body>" +
                "<script>" +
                "  var recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();" +
                "  recognition.lang = 'fr-FR';" +
                "  recognition.onresult = function(event) { " +
                "    var transcript = event.results[0][0].transcript; " +
                "    javaApp.onSpeechReady(transcript); " +
                "  };" +
                "  recognition.onend = function() { javaApp.onSpeechEnd(); };" +
                "  function startRec() { recognition.start(); }" +
                "  function stopRec() { recognition.stop(); }" +
                "</script></body></html>";
        speechWebView.getEngine().loadContent(html);
    }

    public class SpeechBridge {
        public void onSpeechReady(String text) {
            javafx.application.Platform.runLater(() -> {
                messageField.setText(text);
                handleSend(); // Auto-send the dictation
            });
        }
        public void onSpeechEnd() {
            javafx.application.Platform.runLater(() -> {
                isRecording = false;
                btnMic.setText("🎤");
                btnMic.setStyle("-fx-font-size: 18px;");
            });
        }
    }

    @FXML
    private void toggleRecording() {
        if (!isRecording) {
            speechWebView.getEngine().executeScript("startRec()");
            isRecording = true;
            btnMic.setText("🛑");
            btnMic.setStyle("-fx-font-size: 18px; -fx-text-fill: #ef4444; -fx-border-color: #ef4444;");
        } else {
            speechWebView.getEngine().executeScript("stopRec()");
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

        addMessageToChat(msg, true); // true = user (right)
        messageField.clear();

        callGeminiAPI(msg).thenAccept(reply -> {
            javafx.application.Platform.runLater(() -> {
                addMessageToChat(reply, false); // false = AI (left)
                
                Notifications.create()
                    .title("IA Gestion Audit")
                    .text("Nouvelle réponse reçue de l'assistant.")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .showInformation();
            });
        }).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                addMessageToChat("Désolé, j'ai rencontré un problème technique.", false);
            });
            ex.printStackTrace();
            return null;
        });
    }

    private void addMessageToChat(String content, boolean isUser) {
        HBox row = new HBox();
        row.setAlignment(isUser ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);

        VBox bubble = new VBox(5);
        bubble.getStyleClass().add(isUser ? "chat-bubble-client" : "chat-bubble-admin");
        bubble.setMaxWidth(450);

        Label textLabel = new Label(content);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("chat-bubble-text");
        if (isUser) textLabel.setStyle("-fx-text-fill: white;");

        bubble.getChildren().add(textLabel);
        row.getChildren().add(bubble);
        chatHistory.getChildren().add(row);

        // Auto-scroll
        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private CompletableFuture<String> callGeminiAPI(String prompt) {
        JSONObject jsonRequest = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject entry = new JSONObject();
        JSONArray partsArr = new JSONArray();
        JSONObject textPart = new JSONObject();
        
        textPart.put("text", prompt);
        partsArr.put(textPart);
        entry.put("parts", partsArr);
        contents.put(entry);
        jsonRequest.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        try {
                            return jsonResponse.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                        } catch (Exception e) {
                            return "Désolé, je n'ai pas pu traiter votre demande.";
                        }
                    } else {
                        return "Erreur API (" + response.statusCode() + ")";
                    }
                });
    }
}
