package com.hms.acquisition.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hms.shared.pipline.Pipeline;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Playwright.CreateOptions;

@Service
public class TorrentSearchService {
    private final Logger LOG = LoggerFactory.getLogger(TorrentSearchService.class);

    public SearchResponseList searchTorrents(String query) {
        try (Playwright playwright = Playwright.create(new CreateOptions());
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));) {

            Pipeline<SearchResponsePipelineEntry> pipeline = Pipeline.<SearchResponsePipelineEntry>builder()
                    .addHandler(new SearchPirateBayHandler())
                    .addHandler(new LimeTorrentSearchHandler())
                    .onError((entry, e) -> {
                        // Handle errors during the search process
                        LOG.error("Error during torrent search: " + e.getMessage(), e);
                    })
                    .build();
            return pipeline.handle(new SearchResponsePipelineEntry(playwright, browser, new SearchResponseList(query)))
                    .getSearchResponseList();
        } catch (Exception e) {
            // Handle exceptions thrown by the pipeline
            LOG.error("Error during torrent search: " + e.getMessage(), e);
            return new SearchResponseList(query);
        }
    }
}
