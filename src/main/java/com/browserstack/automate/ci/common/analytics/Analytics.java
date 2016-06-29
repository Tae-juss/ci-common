package com.browserstack.automate.ci.common.analytics;

import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsRequest;
import com.brsanthu.googleanalytics.TimingHit;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class Analytics {

    private static final String PLUGIN_PROPERTIES_FILE = "plugin.properties";
    private static final String GOOGLE_PROPERTIES_KEY = "google.analytics.tracking.id";

    private static final String DEFAULT_CLIENT_ID = "unknown-client";

    private final GoogleAnalytics googleAnalyticsClient;

    private static final Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    private String clientId;

    private boolean isEnabled;

    private VersionTracker versionTracker;

    private final AnalyticsDataProvider dataProvider;

    private static Analytics analyticsInstance;

    public Analytics(AnalyticsDataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.versionTracker = new VersionTracker(dataProvider.getRootDir());
        this.googleAnalyticsClient = buildGoogleAnalyticsClient();
        this.isEnabled = true;

        trackInstall();
    }

    /**
     * Method that builds a {@link GoogleAnalytics} object with the tracking id read from a plugins.properties file.
     *
     * @return a new instance of GoogleAnalytics.
     */
    private GoogleAnalytics buildGoogleAnalyticsClient() {
        Properties pluginProps = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(PLUGIN_PROPERTIES_FILE);
            pluginProps.load(inputStream);

            String trackingId = pluginProps.getProperty(GOOGLE_PROPERTIES_KEY, "");
            if (trackingId.length() > 0) {
                return new GoogleAnalytics(trackingId);
            }
        } catch (IOException ioe) {
            isEnabled = false;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }


    protected void postAsync(GoogleAnalyticsRequest request) {
        if (isEnabled && dataProvider.isEnabled() && googleAnalyticsClient != null) {
            googleAnalyticsClient.postAsync(request);
        }
    }

    protected EventHit newEventHit(String category, String action) {
        EventHit eventHit = new EventHit(category, action);
        attachGlobalProperties(eventHit);
        return eventHit;
    }

    protected TimingHit newTimingHit(String category, String variable, int time) {
        TimingHit timingHit = new TimingHit()
                .userTimingCategory(category)
                .userTimingVariableName(variable)
                .userTimingTime(time);
        attachGlobalProperties(timingHit);
        return timingHit;
    }

    protected void attachGlobalProperties(GoogleAnalyticsRequest gaRequest) {
        gaRequest.clientId((clientId != null) ? clientId : getClientId());
        gaRequest.applicationName(dataProvider.getApplicationVersion());
        gaRequest.applicationId(dataProvider.getPluginName());
        gaRequest.applicationVersion(dataProvider.getPluginVersion());
    }

    public void trackInstall() {
        clientId = getClientId();

        String version = dataProvider.getPluginVersion();
        try {
            if (versionTracker.init(version)) {
                postAsync(newEventHit("install", "install"));
            } else if (versionTracker.updateVersion(version)) {
                postAsync(newEventHit("install", "update"));
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to track install: " + e.getMessage());
        }
    }

    public void trackBuildRun(boolean localEnabled, boolean localPathSet,
                              boolean localOptionsSet) {
        EventHit eventHit = newEventHit((localEnabled ? "with" : "without") + "Local", "buildRun");
        if (localPathSet) {
            eventHit.customDimension(1, "withLocalPath");
        } else {
            eventHit.customDimension(2, "withoutLocalPath");
        }

        if (localOptionsSet) {
            eventHit.customDimension(3, "withLocalOptions");
        } else {
            eventHit.customDimension(4, "withoutLocalOptions");
        }

        postAsync(eventHit);
    }

    public void trackReportView() {
        postAsync(newEventHit("report", "separateTab"));
    }

    public void trackReportingEvent(boolean isReportEmbedded) {
        String action = isReportEmbedded ? "reportEmbedded" : "reportNotEmbedded";
        EventHit eventHit = newEventHit("reporting", action);

        postAsync(eventHit);
    }

    public void trackIframeRequest() {
        postAsync(newEventHit("iframeRequested", "iframe"));
    }

    public void trackIframeLoad(int loadTime) {
        postAsync(newTimingHit("iframeLoadTimeMs", "iframe", loadTime));
    }

    protected String getClientId() {
        if (versionTracker != null) {
            try {
                return versionTracker.getClientId();
            } catch (IOException e) {
                return DEFAULT_CLIENT_ID;
            }
        }

        return null;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public static Analytics createInstance(final AnalyticsDataProvider dataProvider) {
        analyticsInstance = new Analytics(dataProvider);
        return analyticsInstance;
    }

    public static Analytics getInstance() {
        return analyticsInstance;
    }
}
