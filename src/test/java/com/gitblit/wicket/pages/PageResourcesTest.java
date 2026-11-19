package com.gitblit.wicket.pages;

import java.io.InputStream;
import javax.imageio.ImageIO;

import com.gitblit.Keys;
import com.gitblit.tests.mock.MemorySettings;
import com.gitblit.wicket.GitBlitWebApp;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.response.StringResponse;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PageResourcesTest {
    @Test
    public void bundledHeaderLogoIsAReadablePng() throws Exception {
        try (InputStream logo = getClass().getResourceAsStream("/logo.png")) {
            assertNotNull("Bundled logo is missing", logo);
            assertNotNull("Bundled logo is not a readable image", ImageIO.read(logo));
        }
    }

    @Test
    public void packagedBottomScriptsCanBeFetched() {
        WicketTester tester = new WicketTester();
        try {
            BasePage page = mock(BasePage.class);
            doCallRealMethod().when(page).addBottomScript(any(String.class));
            for (String path : new String[] { "scripts/imgdiff.js", "scripts/wicketHtml5Patch.js" }) {
                clearInvocations(page);
                page.addBottomScript(path);
                ArgumentCaptor<String> scriptUrl = ArgumentCaptor.forClass(String.class);
                verify(page).addBottomScriptUrl(scriptUrl.capture());
                String url = scriptUrl.getValue();
                assertTrue(url, url.contains("wicket/resource/"));
                tester.executeUrl(url);
                assertEquals(200, tester.getLastResponse().getStatus());
                assertFalse(tester.getLastResponseAsString().isEmpty());
            }
        } finally {
            tester.destroy();
        }
    }

    @Test
    public void customHeaderCssHasOnlyOneStyleWrapper() {
        RootPage page = mock(RootPage.class);
        GitBlitWebApp app = mock(GitBlitWebApp.class);
        MemorySettings settings = new MemorySettings();
        settings.put(Keys.web.headerBackgroundColor, "#123456");
        settings.put(Keys.web.headerBorderColor, "#654321");
        when(page.app()).thenReturn(app);
        when(app.settings()).thenReturn(settings);
        Behavior[] captured = new Behavior[1];
        // Stop after the header contribution; the rest of setup needs a full Gitblit server.
        RuntimeException stop = new RuntimeException("header captured");
        doAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            throw stop;
        }).when(page).add(any(Behavior.class));
        doCallRealMethod().when(page).setupPage(any(String.class), any(String.class));
        try {
            page.setupPage("repo.git", "summary");
            fail("Expected setup to stop after capturing the header");
        } catch (RuntimeException exception) {
            assertSame(stop, exception);
        }
        StringResponse output = new StringResponse();
        IHeaderResponse response = mock(IHeaderResponse.class);
        doAnswer(invocation -> {
            ((HeaderItem) invocation.getArgument(0)).render(output);
            return null;
        }).when(response).render(any(HeaderItem.class));
        captured[0].renderHead(page, response);
        String html = output.toString();
        assertEquals(1, html.split("<style", -1).length - 1);
        assertEquals(1, html.split("</style>", -1).length - 1);
        assertTrue(html, html.contains("background-color: #123456;"));
        assertTrue(html, html.contains("border-bottom: 1px solid #654321 !important;"));
    }
}
