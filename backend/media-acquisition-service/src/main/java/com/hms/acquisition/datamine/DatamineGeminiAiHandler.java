package com.hms.acquisition.datamine;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.ElementHandle.FillOptions;
import com.microsoft.playwright.Keyboard.TypeOptions;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;

// @Service
public class DatamineGeminiAiHandler {

    private static DatamineGeminiAiHandler INSTANCE;

    private final ChatClient chatClient;

    public DatamineGeminiAiHandler(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
        INSTANCE = this;        
    }

    public String prompt(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    public static String promptGeminiAi(String prompt) {
        if (INSTANCE == null) {
            throw new IllegalStateException("DatamineGeminiAiHandler is not initialized yet");
        }
        return INSTANCE.prompt(prompt);
    }

    // public static void main(String[] args) throws Exception {
    //     DatamineGeminiAiHandler handler = new DatamineGeminiAiHandler();
    //     handler.handle();
    // }

    public void handle() {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium()
                        .launch(new BrowserType.LaunchOptions()
                                .setHeadless(false)
                                .setSlowMo(1000)
                                .setArgs(List.of("--disable-blink-features=AutomationControlled",
                                        "--disable-infobars",
                                        "--no-sandbox",
                                        "--disable-dev-shm-usage")));) {
            try (BrowserContext context = browser.newContext(new NewContextOptions()
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(new ViewportSize(1920, 1080))
                    .setLocale("en-US")
                    .setTimezoneId("America/New_York"));) {

                context.addInitScript("""
                            // Hide webdriver flag
                            Object.defineProperty(navigator, 'webdriver', {
                                get: () => undefined
                            });

                            // Fake plugins
                            Object.defineProperty(navigator, 'plugins', {
                                get: () => [1, 2, 3, 4, 5]
                            });

                            // Fake languages
                            Object.defineProperty(navigator, 'languages', {
                                get: () => ['en-US', 'en']
                            });

                            // Add chrome object
                            window.chrome = {
                                runtime: {},
                                loadTimes: function() {},
                                csi: function() {},
                                app: {},
                            };
                        """);
                handle(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handle(BrowserContext context) throws Exception {
        try (Page page = context.newPage()) {

            // String url = "https://www.google.com/";
            // String url = "https://chatgpt.com/";
            // String url = "https://chat.openai.com/";
            String url = "https://gemini.google.com/app";
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            ElementHandle textarea = page.querySelector("rich-textarea");
            // textarea.set
            // textarea.fill("Gemini AI");
            textarea.click();
            page.keyboard().type(
                    "Provide me with the complete series information and every seasons and every episodes information for the tv series: The Office. Do this for every season in the series. Provide the information in a json object with the following format: {\"series\": \"seriesName\", \"synopsis\": \"officialSeriesSynopsis\", \"rating\": seriesImdbRatingFloatValue, \"seasons\": [ { \"number\": integerValue, \"firstAirDate\": \"firstEpisodeAirDate\", \"lastAirDate\": \"lastEpisodeAirDate\", \"episodes\": [ { \"episode\": \"episodeName\", \"number\": integerValue, \"synopsis\": \"officialEpisodeSynopsis\", \"rating\": episodeImdbRatingFloatValue, \"airDate\": \"episodeAirDate\", \"runtime\": runtimeInSeconds } ] } ] }",
                    new TypeOptions().setDelay(20));
            textarea.press("Enter");
            Thread.sleep(100000);

            // Implement logic to find the correct series and navigate to its page
            // Then navigate to the specific season and episode based on
            // entry.seasonNumber() and entry.episodeNumber()
            // Extract the required information and process it as needed
        }
    }

}
