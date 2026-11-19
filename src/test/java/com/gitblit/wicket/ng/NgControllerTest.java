package com.gitblit.wicket.ng;

import java.util.Arrays;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.response.StringResponse;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NgControllerTest {

	@Test
	public void declaresTheProjectsControllerAtPageScopeBeforeAngularBootstraps() {
		NgController controller = new NgController("projectsCtrl");
		controller.addVariable("projectsList", Arrays.asList("Main Repositories", "tickets"));
		IHeaderResponse header = mock(IHeaderResponse.class);
		controller.renderHead(header);

		ArgumentCaptor<HeaderItem> items = ArgumentCaptor.forClass(HeaderItem.class);
		verify(header, times(2)).render(items.capture());
		StringResponse response = new StringResponse();
		items.getAllValues().get(1).render(response);
		String script = Jsoup.parse(response.toString()).select("script").first().data();

		String controllerScript = script.replace("/*<![CDATA[*/", "")
				.replace("<!-- AngularJS projectsCtrl data controller -->", "").trim();
		assertTrue(script, controllerScript.startsWith("function projectsCtrl($scope) {"));
		assertTrue(script, script.contains("$scope.projectsList = [\"Main Repositories\",\"tickets\"];"));
	}
}
