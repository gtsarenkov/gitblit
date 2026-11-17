package com.gitblit.tests;

import com.gitblit.models.Menu.ParameterMenuItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParameterMenuItemTest {

    @Test
    public void changingAnExistingFilterReplacesItsValue() {
        PageParameters current = new PageParameters().add("db", "7").add("r", "example.git");
        ParameterMenuItem item = new ParameterMenuItem("Today", "db", "1", current);

        PageParameters target = item.getPageParameters();
        assertEquals("1", target.get("db").toString());
        assertEquals(1, target.getValues("db").size());
        assertEquals("example.git", target.get("r").toString());
        assertEquals("7", current.get("db").toString());
        assertFalse(item.isSelected());
    }

    @Test
    public void selectingTheCurrentFilterDoesNotDuplicateIt() {
        ParameterMenuItem item = new ParameterMenuItem("Week", "db", "7",
                new PageParameters().add("db", "7"));

        assertTrue(item.isSelected());
        assertEquals(1, item.getPageParameters().getValues("db").size());
    }

    @Test
    public void clearingAFilterRemovesAllItsValues() {
        ParameterMenuItem item = new ParameterMenuItem("All", "db", "",
                new PageParameters().add("db", "7").add("db", "1"));

        assertTrue(item.getPageParameters().getValues("db").isEmpty());
    }
}
