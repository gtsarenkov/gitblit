package com.gitblit.wicket.pages;

import com.gitblit.Constants.AccessPermission;
import com.gitblit.Constants.FederationStrategy;
import com.gitblit.wicket.panels.RegistrantPermissionsPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChoiceRendererRoundTripTest {

    @Test
    public void permissionIdsResolveAgainstTheDisplayedChoices() throws Exception {
        Map<AccessPermission, String> labels = new LinkedHashMap<>();
        for (AccessPermission permission : AccessPermission.values()) {
            labels.put(permission, "Translated permission " + permission.name());
        }
        Constructor<?> constructor = Class.forName(RegistrantPermissionsPanel.class.getName()
                + "$AccessPermissionRenderer").getDeclaredConstructor(RegistrantPermissionsPanel.class, Map.class);
        constructor.setAccessible(true);
        IChoiceRenderer<AccessPermission> renderer = (IChoiceRenderer<AccessPermission>) constructor.newInstance(null, labels);

        assertRoundTrip(renderer, Arrays.asList(AccessPermission.values()));
        // The add-permission dropdown uses a subset with its own indexes.
        assertRoundTrip(renderer, Arrays.asList(AccessPermission.NEWPERMISSIONS));
    }

    @Test
    public void federationIdsResolveIndependentlyOfTranslatedLabels() throws Exception {
        Map<FederationStrategy, String> labels = new LinkedHashMap<>();
        for (FederationStrategy strategy : FederationStrategy.values()) {
            labels.put(strategy, "Translated strategy " + strategy.name());
        }
        EditRepositoryPage page = mock(EditRepositoryPage.class);
        when(page.getFederationTypes()).thenReturn(labels);
        Constructor<?> constructor = Class.forName(EditRepositoryPage.class.getName()
                + "$FederationTypeRenderer").getDeclaredConstructor(EditRepositoryPage.class);
        constructor.setAccessible(true);
        IChoiceRenderer<FederationStrategy> renderer = (IChoiceRenderer<FederationStrategy>) constructor.newInstance(page);

        assertRoundTrip(renderer, Arrays.asList(FederationStrategy.values()));
    }

    @Test
    public void gcPeriodIdsResolveToDaysRatherThanIndexes() throws Exception {
        assertRoundTrip(integerRenderer("GCPeriodRenderer"), Arrays.asList(1, 2, 3, 4, 5, 7, 10, 14));
    }

    @Test
    public void activityLimitIdsResolveToCommitCountsIncludingSpecialValues() throws Exception {
        assertRoundTrip(integerRenderer("MaxActivityCommitsRenderer"),
                Arrays.asList(-1, 0, 25, 50, 75, 100, 150, 200, 250, 500));
    }

    private IChoiceRenderer<Integer> integerRenderer(String name) throws Exception {
        Constructor<?> constructor = Class.forName(EditRepositoryPage.class.getName() + "$" + name)
                .getDeclaredConstructor(EditRepositoryPage.class);
        constructor.setAccessible(true);
        return (IChoiceRenderer<Integer>) constructor.newInstance((Object) null);
    }

    private <T> void assertRoundTrip(IChoiceRenderer<T> renderer, List<T> choices) {
        for (int i = 0; i < choices.size(); i++) {
            T choice = choices.get(i);
            String submittedId = renderer.getIdValue(choice, i);
            assertSame(choice, renderer.getObject(submittedId, Model.ofList(choices)));
        }
    }
}
