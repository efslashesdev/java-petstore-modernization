package com.petstore.e2e;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.petstore.support.MongoTestContainer;

/**
 * Boots the real application on a random port against a real MongoDB and drives it with headless Chromium.
 * Every test gets a fresh browser context, i.e. its own cookies and session. Set {@code -De2e.headed=true}
 * to watch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mongo")
@Import(MongoTestContainer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BrowserTest {

    private static final Path ARTIFACTS = Paths.get("target", "e2e-artifacts");

    @LocalServerPort
    int port;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    protected Page page;
    protected Shopper shopper;

    @RegisterExtension
    final AfterTestExecutionCallback screenshotOnFailure = this::screenshotIfFailed;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(!Boolean.getBoolean("e2e.headed")));
    }

    @AfterAll
    void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openFreshSession() {
        context = browser.newContext(new Browser.NewContextOptions().setBaseURL("http://localhost:" + port));
        page = context.newPage();
        page.setDefaultTimeout(10_000);
        shopper = new Shopper(page);
    }

    @AfterEach
    void closeSession() {
        context.close();
    }

    private void screenshotIfFailed(ExtensionContext extension) {
        if (extension.getExecutionException().isPresent() && page != null) {
            String name = getClass().getSimpleName() + "-" + extension.getRequiredTestMethod().getName() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(ARTIFACTS.resolve(name)).setFullPage(true));
        }
    }
}
