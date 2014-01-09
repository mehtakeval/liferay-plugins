/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This file is part of Liferay Social Office. Liferay Social Office is free
 * software: you can redistribute it and/or modify it under the terms of the GNU
 * Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Liferay Social Office is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Liferay Social Office. If not, see http://www.gnu.org/licenses/agpl-3.0.html.
 */

package com.liferay.so.hook.upgrade.v3_0_0;

import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.persistence.LayoutActionableDynamicQuery;
import com.liferay.portal.util.PortalUtil;
import com.liferay.so.service.SocialOfficeServiceUtil;
import com.liferay.so.util.PortletKeys;

/**
 * @author Matthew Kong
 */
public class UpgradeLayout extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		for (long companyId : PortalUtil.getCompanyIds()) {
			updateSOAnnouncements(companyId);
		}
	}

	protected void updateSOAnnouncements(long companyId) throws Exception {
		ActionableDynamicQuery actionableDynamicQuery =
			new LayoutActionableDynamicQuery() {

				@Override
				protected void performAction(Object object)
					throws PortalException, SystemException {

					Layout layout = (Layout)object;

					if (!SocialOfficeServiceUtil.isSocialOfficeGroup(
							layout.getGroupId())) {

						return;
					}

					Group group = GroupLocalServiceUtil.fetchGroup(
						layout.getGroupId());

					if (layout.isPublicLayout() && group.isUser()) {
						return;
					}

					LayoutTypePortlet layoutTypePortlet =
						(LayoutTypePortlet)layout.getLayoutType();

					if (layoutTypePortlet.hasPortletId(
							PortletKeys.SO_ANNOUNCEMENTS)) {

						return;
					}

					UnicodeProperties typeSettingsProperties =
						layout.getTypeSettingsProperties();

					String columnProperty = StringPool.BLANK;

					if (layoutTypePortlet.hasPortletId(
							PortletKeys.ANNOUNCEMENTS)) {

						LayoutTemplate layoutTemplate =
							layoutTypePortlet.getLayoutTemplate();

						for (String columnName : layoutTemplate.getColumns()) {
							columnProperty = typeSettingsProperties.getProperty(
								columnName);

							columnProperty = StringUtil.replace(
								columnProperty, PortletKeys.ANNOUNCEMENTS,
								PortletKeys.SO_ANNOUNCEMENTS);

							typeSettingsProperties.setProperty(
								columnName, columnProperty);
						}

						layout.setTypeSettingsProperties(
							typeSettingsProperties);

						LayoutLocalServiceUtil.updateLayout(layout);
					}
					else {
						columnProperty = typeSettingsProperties.getProperty(
							"column-1");

						if (Validator.isNotNull(columnProperty)) {
							int columnPos = 0;

							if (StringUtil.contains(
									columnProperty, PortletKeys.MICROBLOGS)) {

								columnPos = 1;
							}

							layoutTypePortlet.addPortletId(
								0, PortletKeys.SO_ANNOUNCEMENTS, "column-1",
								columnPos, false);

							layout = layoutTypePortlet.getLayout();

							LayoutLocalServiceUtil.updateLayout(layout);
						}
					}
				}
			};

		actionableDynamicQuery.performActions();
	}

}