package com.gitblit.wicket;

import com.gitblit.Keys;
import com.gitblit.tests.mock.MemorySettings;
import com.gitblit.utils.XssFilter;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.mock.MockWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Test;

import static org.junit.Assert.*;

public class GitblitParamUrlRequestMapperTest {

    @Test
    public void mapsRepositoryAndOptionalParameters() {
        TestMapper mapper = new TestMapper(new MemorySettings(), "r", "h", "f");
        PageParameters parameters = mapper.decode("summary/example.git");
        assertEquals("example.git", parameters.get("r").toString());
        assertTrue(parameters.get("h").isNull());
        assertEquals("summary/example.git", mapper.encode(parameters));
        assertEquals("summary", mapper.encode(new PageParameters()));
    }

    @Test
    public void keepsLegacyQueryParameterWhenMountEndsWithSlash() {
        TestMapper mapper = new TestMapper(new MemorySettings(), "r");
        assertEquals("example.git", mapper.decode("summary/?r=example.git").get("r").toString());
        assertEquals("example.git", mapper.decode("summary/example.git?r=other.git").get("r").toString());
    }

    @Test
    public void roundTripsPathsAndRepeatedQueryParameters() {
        MemorySettings settings = new MemorySettings();
        settings.put(Keys.web.forwardSlashCharacter, "!");
        TestMapper mapper = new TestMapper(settings, "r", "h", "f");
        PageParameters parameters = new PageParameters().add("r", "group/example.git")
                .add("h", "refs/heads/main").add("f", "docs/a b.txt")
                .add("s", "first").add("s", "second");

        String url = mapper.encode(parameters);
        assertEquals("summary/group!example.git/refs!heads!main/docs!a%20b.txt?s=first&s=second", url);
        PageParameters decoded = mapper.decode(url);
        assertEquals("group/example.git", decoded.get("r").toString());
        assertEquals("refs/heads/main", decoded.get("h").toString());
        assertEquals("docs/a b.txt", decoded.get("f").toString());
        assertEquals(2, decoded.getValues("s").size());
        assertEquals("second", decoded.getValues("s").get(1).toString());
        assertEquals("group/example.git", parameters.get("r").toString());
    }

    @Test
    public void preservesAnEmptyIntermediateParameter() {
        TestMapper mapper = new TestMapper(new MemorySettings(), "r", "h", "f");
        String url = mapper.encode(new PageParameters().add("r", "example.git").add("f", "README.md"));
        assertEquals("summary/example.git//README.md", url);
        assertEquals("README.md", mapper.decode(url).get("f").toString());
    }

    @Test
    public void keepsParametersInQueryWhenPathMountingIsDisabled() {
        MemorySettings settings = new MemorySettings();
        settings.put(Keys.web.forwardSlashCharacter, "!");
        TestMapper mapper = new TestMapper(settings);
        PageParameters parameters = new PageParameters().add("r", "group/example.git");
        String url = mapper.encode(parameters);
        assertTrue(url.startsWith("summary?r="));
        assertEquals("group/example.git", mapper.decode(url).get("r").toString());
    }

    @Test
    public void keepsSlashContainingParametersInQueryWithoutAReplacementCharacter() {
        TestMapper mapper = new TestMapper(new MemorySettings(), "r", "h");
        PageParameters parameters = new PageParameters().add("r", "group/example.git").add("h", "main");
        String url = mapper.encode(parameters);
        assertEquals("summary?r=group/example.git&h=main", url);
        assertEquals("group/example.git", mapper.decode(url).get("r").toString());
        assertEquals("main", mapper.decode(url).get("h").toString());
        assertEquals("group/example.git", mapper.decode("summary/group%2Fexample.git/main").get("r").toString());
    }

    @Test
    public void filtersPathAndEveryQueryValue() {
        XssFilter filter = new XssFilter.AllowXssFilter() {
            @Override
            public String none(String input) {
                return input.replace("<b>", "").replace("</b>", "");
            }
        };
        TestMapper mapper = new TestMapper(new MemorySettings(), filter, "r");
        PageParameters parameters = mapper.decode("summary/%3Cb%3Eexample.git%3C%2Fb%3E"
                + "?s=%3Cb%3Efirst%3C%2Fb%3E&s=%3Cb%3Esecond%3C%2Fb%3E");
        assertEquals("example.git", parameters.get("r").toString());
        assertEquals("first", parameters.getValues("s").get(0).toString());
        assertEquals("second", parameters.getValues("s").get(1).toString());
    }

    private static class TestMapper extends GitblitParamUrlRequestMapper {
        TestMapper(MemorySettings settings, String... names) {
            this(settings, new XssFilter.AllowXssFilter(), names);
        }

        TestMapper(MemorySettings settings, XssFilter filter, String... names) {
            super(settings, filter, "/summary", WebPage.class, names);
        }

        String encode(PageParameters parameters) {
            return buildUrl(new UrlInfo(null, WebPage.class, parameters)).toString();
        }

        PageParameters decode(String value) {
            Url url = Url.parse(value);
            return extractPageParameters(new MockWebRequest(url), url);
        }
    }
}
