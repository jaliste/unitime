/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
*/
package org.unitime.timetable.gwt.client.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.unitime.timetable.gwt.client.Client;
import org.unitime.timetable.gwt.client.Client.GwtPageChangeEvent;
import org.unitime.timetable.gwt.client.Client.GwtPageChangedHandler;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

public class UniTimeNotifications {
	private static UniTimeNotifications sInstance;
	private List<Notification> iNotifications = new ArrayList<Notification>();
	private Timer iMoveTimer = null;
	private NotificationAnimation iAnimation = new NotificationAnimation();
	
	private UniTimeNotifications() {
		Window.addResizeHandler(new ResizeHandler() {
			@Override
			public void onResize(ResizeEvent event) {
				delayedMove();
			}
		});
		Window.addWindowScrollHandler(new Window.ScrollHandler() {
			@Override
			public void onWindowScroll(ScrollEvent event) {
				delayedMove();
			}
		});
		Client.addGwtPageChangedHandler(new GwtPageChangedHandler() {
			@Override
			public void onChange(GwtPageChangeEvent event) {
				delayedMove();
			}
		});
		iMoveTimer = new Timer() {
			@Override
			public void run() {
				move();
			}
		};
	}
	
	
	public static UniTimeNotifications getInstance() {
		if (sInstance == null) {
			sInstance = new UniTimeNotifications();
		}
		return sInstance;
	}
	
	public void addNotification(final Notification notification) {
		RootPanel.get().add(notification, Window.getScrollLeft() + Window.getClientWidth() - 445, Window.getScrollTop() + Window.getClientHeight());
		iAnimation.cancel();
		for (Iterator<Notification> i = iNotifications.iterator(); i.hasNext(); ) {
			Notification n = i.next();
			if (n.equals(notification)) {
				n.hide(); i.remove();
			}
		}
		move();
		iNotifications.add(0, notification);
		iAnimation.run(1000);
		Timer timer = new Timer() {
			@Override
			public void run() {
				notification.hide();
				iNotifications.remove(notification);
			}
		};
		notification.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				notification.hide();
				iNotifications.remove(notification);
				move();
			}
		});
		timer.schedule(10000);
	}
	
	private void move() {
		int height = 0;
		iAnimation.cancel();
		for (HTML notification: iNotifications) {
			height += notification.getElement().getClientHeight() + 10;
			DOM.setStyleAttribute(notification.getElement(), "left", (Window.getScrollLeft() + Window.getClientWidth() - 445) + "px");
			DOM.setStyleAttribute(notification.getElement(), "top", (Window.getScrollTop() + Window.getClientHeight() - height) + "px");
		}
	}
	
	private void delayedMove() {
		if (!iNotifications.isEmpty())
			iMoveTimer.schedule(100);
	}
	
	public static void info(String text) {
		getInstance().addNotification(new Notification(text, "unitime-NotificationInfo"));
	}
	
	public static void warn(String text) {
		getInstance().addNotification(new Notification(text, "unitime-NotificationWarning"));
	}

	public static void error(String text) {
		getInstance().addNotification(new Notification(text, "unitime-NotificationError"));
	}

	private class NotificationAnimation extends Animation {
		@Override
		protected void onUpdate(double progress) {
			if (iNotifications.isEmpty()) return;
			int height = - (int) Math.round((1.0 - progress) * (iNotifications.get(0).getElement().getClientHeight() + 10));
			for (Notification notification: iNotifications) {
				height += notification.getElement().getClientHeight() + 10;
				DOM.setStyleAttribute(notification.getElement(), "left", (Window.getScrollLeft() + Window.getClientWidth() - 445) + "px");
				DOM.setStyleAttribute(notification.getElement(), "top", (Window.getScrollTop() + Window.getClientHeight() - height) + "px");
			}
		}
	}
	
	public static class Notification extends HTML {
		Notification(String text, String style) {
			super(text);
			setStyleName("unitime-Notification");
			addStyleName(style);
		}
		
		Notification(String text) {
			this(text, "unitime-NotificationInfo");
		}
		
		public String toString() { return getHTML(); }
		public int hashCode() { return getHTML().hashCode(); }
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Notification)) return false;
			return ((Notification)o).getHTML().equals(getHTML());
		}
		
		public void hide() {
			RootPanel.get().remove(this);
		}
		
		public void show() {
			RootPanel.get().add(this, Window.getScrollLeft() + Window.getClientWidth() - 445, Window.getScrollTop() + Window.getClientHeight());
		}
	}
	
	public static native void createTriggers()/*-{
		$wnd.gwtShowMessage = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::info(Ljava/lang/String;)(message);
		};
		$wnd.gwtShowWarning = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::warn(Ljava/lang/String;)(message);
		};
		$wnd.gwtShowError = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::error(Ljava/lang/String;)(message);
		};
	}-*/;
}
