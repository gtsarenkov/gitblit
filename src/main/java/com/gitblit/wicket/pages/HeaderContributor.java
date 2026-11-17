package com.gitblit.wicket.pages;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.head.IHeaderResponse;

public class HeaderContributor<T extends IHeaderContributor> extends Behavior {
    private final T data;

    public HeaderContributor(T data) {
        this.data = data;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        data.renderHead(response);
    }
}
