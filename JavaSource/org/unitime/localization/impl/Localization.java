/*
 * UniTime 3.3 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.localization.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.ApplicationProperties;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.Messages;

/**
 * @author Tomas Muller
 */
public class Localization {
	private static Log sLog = LogFactory.getLog(Localization.class);
	public static final String ROOT = "org.unitime.localization.messages.";
	public static final String GWTROOT = "org.unitime.timetable.gwt.resources.";
	private static Map<Class, Object> sBundles = new Hashtable<Class, Object>();
	
	private static final ThreadLocal<String> sLocale = new ThreadLocal<String>() {
		 @Override
		 protected String initialValue() {
             return ApplicationProperties.getProperty("unitime.locale", "en");
		 }
	};
	
	public static void setLocale(String locale) { sLocale.set(locale); }
	public static String getLocale() { return sLocale.get(); }
	public static String getFirstLocale() {
		String locale = getLocale();
		if (locale.indexOf(',') >= 0) locale = locale.substring(0, locale.indexOf(','));
		if (locale.indexOf(';') >= 0) locale = locale.substring(0, locale.indexOf(';'));
		return locale.trim();
	}
	
	public static <T> T create(Class<T> bundle) {
		synchronized (sBundles) {
			T ret = (T)sBundles.get(bundle);
			if (ret == null) {
				ret = (T)Proxy.newProxyInstance(Localization.class.getClassLoader(), new Class[] {bundle, StrutsActionsRetriever.class}, new Bundle(bundle));
				sBundles.put(bundle, ret);
			}
			return ret;
		}
	}
	
	public static class Bundle implements InvocationHandler {
		private Map<String, Properties> iProperties = new Hashtable<String, Properties>();
		private Class<?> iMessages = null;

		public Bundle(Class<?> messages) {
			iMessages = messages;
		}
		
		private synchronized String getProperty(String locale, String name) {
			Properties properties = iProperties.get(locale);
			if (properties == null) {
				properties = new Properties();
				String resource = iMessages.getName().replace('.', '/') + (locale.isEmpty() ? "" : "_" + locale) + ".properties"; 
				try {
					InputStream is = Localization.class.getClassLoader().getResourceAsStream(resource);
					if (is != null)
						properties.load(is);
				} catch (Exception e) {
					sLog.warn("Failed to load message bundle " + iMessages.getName().substring(iMessages.getName().lastIndexOf('.') + 1) + " for " + locale + ": "  + e.getMessage(), e);
				}
				iProperties.put(locale, properties);
			}
			return properties.getProperty(name);
		}
		
		private String getProperty(String name) {
			for (String locale: getLocale().split(",")) {
				if (locale.indexOf(';') >= 0) locale = locale.substring(0, locale.indexOf(';'));
				String value = getProperty(locale.trim(), name);
				if (value != null) return value;
				if (locale.indexOf('_') >= 0) {
					locale = locale.substring(0, locale.indexOf('_'));
					value = getProperty(locale.trim(), name);
					if (value != null) return value;
				}
			}
			return getProperty("", name); // try default message bundle
		}
		
		private String fillArgumentsIn(String value, Object[] args, int firstIndex) {
			if (value == null || args == null) return value;
			for (int i = 0; i + firstIndex < args.length; i++)
				value = value.replace("{" + i + "}", (args[i + firstIndex] == null ? "" : args[i + firstIndex].toString()));
			return value;
		}
		
		private String[] string2array(String value) {
			return value.split("(?<=^.*[^\\\\]),(?=.*$)");
		}
		
		private Map<String, String> array2map(String[] value) {
			Map<String, String> map = new HashMap<String, String>();
			for (int i = 0; i < value.length - 1; i += 2)
				map.put(value[i], value[i + 1]);
			return map;
		}
		
		private Object type(String value, Class returnType) {
			if (value == null) return value;
			if (String.class.equals(returnType))
				return value;
			
			if (Boolean.class.equals(returnType) || boolean.class.equals(returnType))
				return "true".equalsIgnoreCase(value);
			if (Double.class.equals(returnType) || double.class.equals(returnType))
				return Double.valueOf(value);
			if (Float.class.equals(returnType) || float.class.equals(returnType))
				return Float.valueOf(value);
			if (Integer.class.equals(returnType) || int.class.equals(returnType))
				return Integer.valueOf(value);

			if (String[].class.equals(returnType))
				return string2array(value);
			
			if (Map.class.equals(returnType)) {
				Map<String, String> map = new HashMap<String, String>();
				for (String key: string2array(value)) {
					String val = getProperty(key.trim());
					if (val != null) map.put(key.trim(), val);
				}
				if (map.isEmpty())
					return array2map(string2array(value));
				return map;
			}

			return value;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ("getStrutsActions".equals(method.getName()) && method.getParameterTypes().length == 1)
				return getStrutsActions(proxy, (Class<? extends LocalizedLookupDispatchAction>) args[0]);
			if ("translateMessage".equals(method.getName()) && method.getParameterTypes().length >= 2) {
				String value = (args[0] == null ? null : getProperty((String) args[0]));
				return (value == null ? (String) args[1] : fillArgumentsIn(value, args, 2));
			}
			String value = getProperty(method.getName());
			if (value != null) 
				return type(fillArgumentsIn(value, args, 0), method.getReturnType());
			Messages.DefaultMessage dm = method.getAnnotation(Messages.DefaultMessage.class);
			if (dm != null)
				return fillArgumentsIn(dm.value(), args, 0);
			Constants.DefaultBooleanValue db = method.getAnnotation(Constants.DefaultBooleanValue.class);
			if (db != null)
				return db.value();
			Constants.DefaultDoubleValue dd = method.getAnnotation(Constants.DefaultDoubleValue.class);
			if (dd != null)
				return dd.value();
			Constants.DefaultFloatValue df = method.getAnnotation(Constants.DefaultFloatValue.class);
			if (df != null)
				return df.value();
			Constants.DefaultIntValue di = method.getAnnotation(Constants.DefaultIntValue.class);
			if (di != null)
				return di.value();
			Constants.DefaultStringValue ds = method.getAnnotation(Constants.DefaultStringValue.class);
			if (ds != null)
				return ds.value();
			Constants.DefaultStringArrayValue dsa = method.getAnnotation(Constants.DefaultStringArrayValue.class);
			if (dsa != null)
				return dsa.value();
			Constants.DefaultStringMapValue dsm = method.getAnnotation(Constants.DefaultStringMapValue.class);
			if (dsm != null)
				return array2map(dsm.value());
			
			return method.getName();
		}
		
		private Map<String, String> getStrutsActions(Object proxy, Class<? extends LocalizedLookupDispatchAction> apply) throws Throwable {
			Map<String, String> ret = new HashMap<String, String>();
			for (Method m: iMessages.getDeclaredMethods()) {
				if (m.getParameterTypes().length > 0) continue;
				org.unitime.localization.messages.Messages.StrutsAction action = m.getAnnotation(org.unitime.localization.messages.Messages.StrutsAction.class);
				if (action != null) {
					Messages.DefaultMessage dm = m.getAnnotation(Messages.DefaultMessage.class);
					if (action.apply() == null || action.apply().length == 0) {
						try {
							if (apply.getMethod(action.value(), new Class<?>[] {
								ActionMapping.class, ActionForm.class, HttpServletRequest.class, HttpServletResponse.class
								}) != null) {
								ret.put((String)invoke(proxy, m, new Object[] {}), action.value());
								if (dm != null)
									ret.put(dm.value(), action.value());
							}
						} catch (NoSuchMethodException e) {}
					} else {
						for (Class<? extends LocalizedLookupDispatchAction> a: action.apply())
							if (a.equals(apply)) {
								ret.put((String)invoke(proxy, m, new Object[] {}), action.value());
								if (dm != null)
									ret.put(dm.value(), action.value());
							}
					}
				}
			}
			return ret;
		}
	}
	
	public static interface StrutsActionsRetriever {
		Map<String, String> getStrutsActions(Class<? extends LocalizedLookupDispatchAction> apply);
	}

}
