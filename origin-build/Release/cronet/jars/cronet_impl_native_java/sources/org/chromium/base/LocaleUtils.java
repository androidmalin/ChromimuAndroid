package org.chromium.base;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.LocaleList;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Map;
import org.chromium.base.annotations.CalledByNative;

public class LocaleUtils {
    private static final Map<String, String> LANGUAGE_MAP_FOR_ANDROID;
    private static final Map<String, String> LANGUAGE_MAP_FOR_CHROMIUM;

    private LocaleUtils() {
    }

    static {
        HashMap<String, String> mapForChromium = new HashMap();
        mapForChromium.put("iw", "he");
        mapForChromium.put("ji", "yi");
        mapForChromium.put("in", "id");
        mapForChromium.put("tl", "fil");
        LANGUAGE_MAP_FOR_CHROMIUM = Collections.unmodifiableMap(mapForChromium);
        HashMap<String, String> mapForAndroid = new HashMap();
        mapForAndroid.put("und", "");
        mapForAndroid.put("fil", "tl");
        LANGUAGE_MAP_FOR_ANDROID = Collections.unmodifiableMap(mapForAndroid);
    }

    public static String getUpdatedLanguageForChromium(String language) {
        String updatedLanguageCode = (String) LANGUAGE_MAP_FOR_CHROMIUM.get(language);
        return updatedLanguageCode == null ? language : updatedLanguageCode;
    }

    @TargetApi(21)
    @VisibleForTesting
    public static Locale getUpdatedLocaleForChromium(Locale locale) {
        String languageForChrome = (String) LANGUAGE_MAP_FOR_CHROMIUM.get(locale.getLanguage());
        return languageForChrome == null ? locale : new Builder().setLocale(locale).setLanguage(languageForChrome).build();
    }

    public static String getUpdatedLanguageForAndroid(String language) {
        String updatedLanguageCode = (String) LANGUAGE_MAP_FOR_ANDROID.get(language);
        return updatedLanguageCode == null ? language : updatedLanguageCode;
    }

    @TargetApi(21)
    @VisibleForTesting
    public static Locale getUpdatedLocaleForAndroid(Locale locale) {
        String languageForAndroid = (String) LANGUAGE_MAP_FOR_ANDROID.get(locale.getLanguage());
        return languageForAndroid == null ? locale : new Builder().setLocale(locale).setLanguage(languageForAndroid).build();
    }

    public static Locale forLanguageTagCompat(String languageTag) {
        String[] tag = languageTag.split("-");
        if (tag.length == 0) {
            return new Locale("");
        }
        String language = getUpdatedLanguageForAndroid(tag[0]);
        if ((language.length() != 2 && language.length() != 3) || language.equals("und")) {
            return new Locale("");
        }
        if (tag.length == 1) {
            return new Locale(language);
        }
        String country = tag[1];
        if (country.length() == 2 || country.length() == 3) {
            return new Locale(language, country);
        }
        return new Locale(language);
    }

    public static Locale forLanguageTag(String languageTag) {
        if (VERSION.SDK_INT >= 21) {
            return getUpdatedLocaleForAndroid(Locale.forLanguageTag(languageTag));
        }
        return forLanguageTagCompat(languageTag);
    }

    public static String toLanguageTag(Locale locale) {
        String language = getUpdatedLanguageForChromium(locale.getLanguage());
        String country = locale.getCountry();
        if (language.equals("no") && country.equals("NO") && locale.getVariant().equals("NY")) {
            return "nn-NO";
        }
        return !country.isEmpty() ? language + "-" + country : language;
    }

    @TargetApi(24)
    public static String toLanguageTags(LocaleList localeList) {
        ArrayList<String> newLocaleList = new ArrayList();
        for (int i = 0; i < localeList.size(); i++) {
            newLocaleList.add(toLanguageTag(getUpdatedLocaleForChromium(localeList.get(i))));
        }
        return TextUtils.join(",", newLocaleList);
    }

    @CalledByNative
    public static String getDefaultLocaleString() {
        return toLanguageTag(Locale.getDefault());
    }

    public static String getDefaultLocaleListString() {
        if (VERSION.SDK_INT >= 24) {
            return toLanguageTags(LocaleList.getDefault());
        }
        return getDefaultLocaleString();
    }

    @CalledByNative
    private static String getDefaultCountryCode() {
        CommandLine commandLine = CommandLine.getInstance();
        if (commandLine.hasSwitch(BaseSwitches.DEFAULT_COUNTRY_CODE_AT_INSTALL)) {
            return commandLine.getSwitchValue(BaseSwitches.DEFAULT_COUNTRY_CODE_AT_INSTALL);
        }
        return Locale.getDefault().getCountry();
    }
}
