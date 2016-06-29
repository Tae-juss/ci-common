package com.browserstack.automate.ci.common.analytics;

import java.io.File;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public interface AnalyticsDataProvider {

    File getRootDir();

    String getApplicationName();

    String getApplicationVersion();

    String getPluginName();

    String getPluginVersion();

    boolean isEnabled();

}
