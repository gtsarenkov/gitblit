/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.eclipse.jgit.lib.PersonIdent;

import com.gitblit.AvatarGenerator;
import com.gitblit.Keys;
import com.gitblit.models.UserModel;
import com.gitblit.wicket.ExternalImage;
import com.gitblit.wicket.WicketUtils;

/**
 * Renders an avatar with a local fallback.
 *
 * @author James Moger
 *
 */
public class AvatarImage extends BasePanel {

	private static final long serialVersionUID = 1L;

	public AvatarImage(String id, PersonIdent person) {
		this(id, person, 0);
	}

	public AvatarImage(String id, PersonIdent person, int width) {
		this(id, person.getName(), person.getEmailAddress(), "gravatar", width, true);
	}

	public AvatarImage(String id, PersonIdent person, String cssClass, int width, boolean identicon) {
		this(id, person.getName(), person.getEmailAddress(), cssClass, width, identicon);
	}

	public AvatarImage(String id, UserModel user, String cssClass, int width, boolean identicon) {
		this(id, user.getDisplayName(), user.emailAddress, cssClass, width, identicon);
	}

	public AvatarImage(String id, String username, String emailaddress, String cssClass, int width, boolean identicon) {
		super(id);

		AvatarGenerator avatarGenerator = app().runtime().getInjector().getInstance(AvatarGenerator.class);
		String url = avatarGenerator.getURL(username, emailaddress, identicon, width);
		ExternalImage image = new ExternalImage("image", url);
		image.add(AttributeModifier.replace("alt", username));
		image.add(AttributeModifier.replace("data-avatar-fallback", getRequest().getContextPath() + "/user_16x16.png"));
		if (width > 0) {
			image.add(AttributeModifier.replace("width", width));
		}
		if (cssClass != null) {
			WicketUtils.setCssClass(image, cssClass);
		}
		add(image);
		WicketUtils.setHtmlTooltip(image, username);
		setVisible(app().settings().getBoolean(Keys.web.allowGravatar, true));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(
				new JavaScriptResourceReference(AvatarImage.class, "avatar-fallback.js")));
	}

	public void setTooltip(String tooltip) {
		WicketUtils.setHtmlTooltip(get("image"), tooltip);
	}
}