package com.gitblit.wicket;

import com.gitblit.tests.mock.MemorySettings;
import com.gitblit.utils.XssFilter;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Test;

import static org.junit.Assert.*;

public class CachedRequestTest {
    @Test
    public void loginContinuesToMountedRepositoryRevisionAndFile() {
        WicketTester tester = new WicketTester(new WebApplication() {
            @Override
            public Class<? extends WebPage> getHomePage() {
                return WebPage.class;
            }

            @Override
            public Session newSession(Request request, Response response) {
                return new GitBlitWebSession(request);
            }

            @Override
            protected void init() {
                super.init();
                mount(new GitblitParamUrlRequestMapper(new MemorySettings(), new XssFilter.AllowXssFilter(),
                        "/blob", ProtectedPage.class, new String[] { "r", "h", "f" }));
            }
        });
        try {
            tester.setFollowRedirects(false);
            tester.executeUrl("blob?f=docs/README.md&h=feature/branch&r=group/repo.git&s=first&s=second");
            assertEquals("Initial response status " + tester.getLastResponse().getStatus(), "login",
                    tester.getLastResponse().getRedirectLocation());
            GitBlitWebSession session = (GitBlitWebSession) tester.getSession();
            try {
                session.continueRequest();
                fail("Expected the cached request to redirect after login");
            } catch (RedirectToUrlException redirect) {
                String url = ((RedirectRequestHandler) redirect.getReplacementRequestHandler()).getRedirectUrl();
                assertTrue(url, url.endsWith("/blob?f=docs/README.md&h=feature/branch&r=group/repo.git&s=first&s=second"));
            }
            assertFalse(session.continueRequest());
        } finally {
            tester.destroy();
        }
    }

    public static class ProtectedPage extends WebPage {
        public ProtectedPage(PageParameters parameters) {
            super(parameters);
            GitBlitWebSession.get().cacheRequest(ProtectedPage.class);
            throw new RedirectToUrlException("/login");
        }
    }
}
