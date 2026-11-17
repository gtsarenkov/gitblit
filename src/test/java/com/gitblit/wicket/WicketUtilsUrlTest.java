package com.gitblit.wicket;

import java.net.URI;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.mock.MockWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.cycle.RequestCycleContext;
import org.apache.wicket.request.mapper.CompoundRequestMapper;
import org.apache.wicket.response.StringResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WicketUtilsUrlTest {
    private RequestCycle previousCycle;

    @Before
    public void saveRequestCycle() {
        previousCycle = RequestCycle.get();
    }

    @After
    public void restoreRequestCycle() {
        ThreadContext.setRequestCycle(previousCycle);
    }

    @Test
    public void preservesRootDeployment() {
        setRequest("", "", "edit");
        assertEquals("/summary?r=repo.git",
                WicketUtils.toAbsoluteUrl("summary?r=repo.git"));
    }

    @Test
    public void preservesApplicationContext() {
        setRequest("/gitblit", "", "edit");
        assertEquals("/gitblit/summary?r=repo.git",
                WicketUtils.toAbsoluteUrl("summary?r=repo.git"));
    }

    @Test
    public void resolvesNestedPageAndPreservesFilterPath() {
        setRequest("/gitblit", "/ui", "tickets/edit");
        assertEquals("/gitblit/ui/summary?r=repo.git",
                WicketUtils.toAbsoluteUrl("../summary?r=repo.git"));
    }

    @Test
    public void preservesEncodedSlashesAndQueryParameters() {
        setRequest("/gitblit", "", "tickets/edit");
        String absoluteUrl = WicketUtils.toAbsoluteUrl(
                "../tree/group%2Frepo.git?h=feature%2Fbranch&status=new&status=open");
        assertEquals("/gitblit/tree/group%2Frepo.git", URI.create(absoluteUrl).getRawPath());
        Url url = Url.parse(absoluteUrl);
        assertEquals("feature/branch", url.getQueryParameterValue("h").toString());
        assertEquals(3, url.getQueryParameters().size());
    }

    private void setRequest(String contextPath, String filterPath, String pagePath) {
        Url clientUrl = Url.parse(pagePath);
        clientUrl.setProtocol("http");
        clientUrl.setHost("localhost");
        MockWebRequest request = new MockWebRequest(clientUrl)
                .setContextPath(contextPath).setFilterPath(filterPath);
        RequestCycle cycle = new RequestCycle(new RequestCycleContext(request,
                new StringResponse(), new CompoundRequestMapper(), exception -> null));
        ThreadContext.setRequestCycle(cycle);
    }
}
