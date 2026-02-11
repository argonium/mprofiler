package io.miti;

import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Custom method profiler class.  Typical usage is:
 *
 * MProfiler.reset(); // on repeated runs, we need to clear the list first
 * MProfiler.start("label1"); // start a new profile point
 * ....
 * MProfiler.start("label2", true); // start another, while others are still running; the
 *                                  // 'true' argument means 'print when this starts and stops'
 * ...
 * MProfiler.stop(); // stop the last started profile point
 * ...
 * MProfiler.stop(true); // when stopping the final profile point, pass true to get a summary printed
 */
public class MProfiler {

    /**
     * Used to store the output (summarized) data.
     */
    static class Profiled implements Comparable<Profiled> {

        private double percentTime;

        private long totalMSecs;

        private int numCalls;

        private double msPerCall;

        private String label;

        public double getPercentTime() {
            return percentTime;
        }

        public void setPercentTime(double percentTime) {
            this.percentTime = percentTime;
        }

        public long getTotalMSecs() {
            return totalMSecs;
        }

        public void setTotalMSecs(long totalMSecs) {
            this.totalMSecs = totalMSecs;
        }

        public int getNumCalls() {
            return numCalls;
        }

        public void setNumCalls(int numCalls) {
            this.numCalls = numCalls;
        }

        public double getMsPerCall() {
            return msPerCall;
        }

        public void setMsPerCall(double msPerCall) {
            this.msPerCall = msPerCall;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public int compareTo(Profiled o) {
            // First sort by "% time" in descending order
            int result = Double.compare(o.percentTime, percentTime);
            if (result == 0) {
                // If the above comparison is zero (equal numbers),
                // then sort by "total time" in ascending order
                result = Long.compare(totalMSecs, o.totalMSecs);
            }
            return result;
        }

        @Override
        public String toString() {
            return "Profiled{" +
                    "percentTime=" + percentTime +
                    ", totalMSecs=" + totalMSecs +
                    ", numCalls=" + numCalls +
                    ", msPerCall=" + msPerCall +
                    ", label='" + label + '\'' +
                    '}';
        }
    }

    /** POJO for a profile data point. */
    static class ProfilePoint {

        /** A descriptive name for this profile point. */
        private String label;

        /** Whether this profile point is currently accumulating data. */
        private boolean isRunning = true;

        /** If false, this is a top-level profile point. */
        private boolean isChild = false;

        /** The total duration for this profile point, including the children's duration. */
        private long totalDuration = 0L;

        /** Just the duration for the children. */
        private long childDuration = 0L;

        /** The start time for this profile point. */
        private long startTime = 0L;

        /** The number of times this profile point has been started. */
        private int count = 1;

        /** Whether to log the time this point is started and stopped. */
        private boolean logTime = false;

        public ProfilePoint(final String label, final long startTime) {
            this.label = label;
            this.startTime = startTime;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        public boolean isChild() {
            return isChild;
        }

        public void setChild(boolean child) {
            isChild = child;
        }

        public long getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(long totalDuration) {
            this.totalDuration = totalDuration;
        }

        public long getChildDuration() {
            return childDuration;
        }

        public void setChildDuration(long childDuration) {
            this.childDuration = childDuration;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean getLogTime() {
            return logTime;
        }

        public void setLogTime(final boolean bLogTime) {
            logTime = bLogTime;
        }

        /**
         * Helper method to set this profile as "not running" and update the total duration.
         *
         * @return the new total duration for this profile point
         */
        private long close() {

            // Mark is as not-running since we are closing this profile point
            setRunning(false);

            // Increment the total duration by the time of this current duration
            final long delta = System.currentTimeMillis() - startTime;
            setTotalDuration(totalDuration + delta);

            // Return the amount of time spent in this method (including children methods),
            // so we can increment the parent's child-duration time
            return delta;
        }

        public void incrementCount() {
            ++count;
        }

        public void incrementChildDuration(final long lChildDuration) {
            childDuration += lChildDuration;
        }

        @Override
        public String toString() {
            return "ProfilePoint{" +
                    "label='" + label + '\'' +
                    ", isRunning=" + isRunning +
                    ", isChild=" + isChild +
                    ", totalDuration=" + totalDuration +
                    ", childDuration=" + childDuration +
                    ", startTime=" + startTime +
                    ", count=" + count +
                    ", logTime=" + logTime +
                    '}';
        }
    }

    /** This holds our list of running profile points. */
    private static List<ProfilePoint> profiles = null;

    /**
     * Default constructor; make it private so the class
     * cannot be instantiated.
     */
    private MProfiler() {
        super();
    }

    /**
     * Start (or resume) a profile for this label.  Do not
     * reset the list of existing profile points.
     *
     * @param label a unique label for this profile
     */
    public static void start(final String label) {
        start(label, false);
    }

    /**
     * Start (or resume) a profile for this label.
     *
     * @param label a unique label for this profile
     * @param logEvents whether to log the start and stop time of this profile point
     */
    public static void start(final String label, final boolean logEvents) {

        // If we don't have any profiles, create the list now
        if (profiles == null) {
            profiles = new ArrayList<>(3);
        }

        // See if this label is already in our list
        boolean matchFound = false;
        for (ProfilePoint profile : profiles) {
            if (profile.getLabel().equals(label)) {

                // We found a match.  Confirm it's not already running.
                matchFound = true;
                if (profile.isRunning()) {
                    System.err.println("Warning: entry point " + label + " is already running");
                }
                profile.setRunning(true);
                profile.setStartTime(System.currentTimeMillis());
                profile.incrementCount();
                profile.setLogTime(logEvents);

                logProfilePoint(profile, true);

                // We can stop searching now
                break;
            }
        }

        // If no match found on label, add a new entry to the profiles list
        if (!matchFound) {
            final ProfilePoint profile = new ProfilePoint(label, System.currentTimeMillis());
            // If there are any active (running) profiles, then this is a child profile
            profile.setChild(hasRunningProfile());
            profile.setLogTime(logEvents);
            profiles.add(profile);

            logProfilePoint(profile, true);
        }
    }

    /**
     * Stop the most recently started profile point.
     */
    public static void stop() {
        stop(false);
    }

    /**
     * Stop all active profile points.
     */
    public static void stopAll() {
        stopAll(false);
    }

    /**
     * Stop all active profile points.
     *
     * @param summarize whether to print a summary at the end
     */
    public static void stopAll(final boolean summarize) {
        // Stop any running profiles
        while (hasRunningProfile()) {
            stop(false);
        }

        if (summarize) {
            summarize(true);
        }
    }

    /**
     * Check if we have any active profiles.
     *
     * @return whether any profiles are ongoing
     */
    public static boolean hasRunningProfile() {

        if (!hasProfiles()) {
            return false;
        }

        boolean result = false;
        for (ProfilePoint profile : profiles) {
            if (profile.isRunning()) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Stop the most recently started profile.
     *
     * @param summarize whether to summarize at the end
     */
    public static void stop(final boolean summarize) {

        if (!hasProfiles()) {
            return;
        }

        // Find the latest-started profile (so start searching at the end)
        long childDuration = 0L;
        int index = profiles.size() - 1;
        for (; index >= 0; --index) {

            // If this profile is still running, we found it
            if (profiles.get(index).isRunning()) {
                logProfilePoint(profiles.get(index), false);
                childDuration = profiles.get(index).close();
                --index;
                break;
            }
        }

        // Update the child-duration of the parent's profile point (it'll
        // be the running profile earlier in the list, so iterate backwards)
        if (childDuration > 0L) {
            for (; index >= 0; --index) {
                if (profiles.get(index).isRunning()) {
                    // We found the immediate parent, so increment the
                    // child duration and then break the loop
                    profiles.get(index).incrementChildDuration(childDuration);
                    break;
                }
            }
        }

        if (summarize && hasRunningProfile()) {
            System.err.println("Summarizing but there are still running profiles");
        }

        // Check if we should print a summary
        if (summarize) {
            summarize(true);
        }
    }

    /**
     * Print a profile summary.
     */
    public static List<Profiled> summarize(final boolean printSummary) {

        // Check for any profile points
        if (!hasProfiles()) {
            return Collections.emptyList();
        }

        // Get the total running time of all top-level processes
        final double totalProcessDuration = profiles.stream().filter(profile -> !profile.isChild())
                .mapToDouble(profile -> profile.totalDuration).sum();

        // Build the list we'll print out for the summary
        final List<Profiled> summary = new ArrayList<>(profiles.size());
        for (ProfilePoint profile : profiles) {

            // Build the summary for this profile point
            Profiled session = new Profiled();
            session.setLabel(profile.getLabel());
            session.setNumCalls(profile.getCount());
            session.setTotalMSecs(profile.getTotalDuration());

            // Check for bad data (child duration should always be <= total duration)
            if (profile.getTotalDuration() < profile.getChildDuration()) {
                System.err.println("Error: total duration < child duration for node " + profile.getLabel());
            }

            // Compute the % of time spent in this node
            final double delta = (double) (profile.getTotalDuration() - profile.getChildDuration());
            if (totalProcessDuration < 1.0) {
                session.setPercentTime(0.0);
            } else {
                session.setPercentTime(100.0 * delta / totalProcessDuration);
            }
            session.setMsPerCall(delta / profile.getCount());

            // Add to the overall summary
            summary.add(session);
        }

        // Sort the list (by percentTime in descending order)
        java.util.Collections.sort(summary);

        // Print the summary
        if (printSummary) {
            final List<String> output = buildSummaryLines(summary);
            output.forEach(System.out::println);
        }

        return summary;
    }

    /**
     * Helper method to build the output data for the summarize method.
     *
     * @param summary the summary of data
     * @return a list of strings so we can print each row
     */
    private static List<String> buildSummaryLines(final List<Profiled> summary) {

        // This list will hold the array of array of strings (each line and each field)
        final List<List<String>> list = new ArrayList<>(10);

        // Add the column header row
        list.add(Arrays.asList("% Time", "Total Time (ms)", "# Calls", "MS / Call", "Label"));

        // Build each row of data
        for (Profiled session : summary) {
            final List<String> childList = new ArrayList<>(5);
            childList.add(formatDouble(session.getPercentTime()) + "%");
            childList.add(java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(session.getTotalMSecs()));
            childList.add(Integer.toString(session.getNumCalls()));
            childList.add(formatDouble(session.getMsPerCall()));
            childList.add(session.getLabel());
            list.add(childList);
        }

        // Build the list of output strings
        final ListFormatter fmt = new ListFormatter(list);
        return fmt.format(4, list);
    }

    /**
     * Helper method to format a floating-point number for output.
     *
     * @param val the double value to format as a string
     * @return a string representing the value
     */
    public static String formatDouble(final double val) {

        // If the input has no digits in the mantissa, then format it as a long integer
        if (Double.compare(val, Math.floor(val)) == 0) {
            return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format((long) val);
        }

        // Format with separating commas
        DecimalFormat decimalFormat = new DecimalFormat("0.##");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        return decimalFormat.format(val);
    }

    /**
     * Reset the list of profiles.
     */
    public static void reset() {

        // If we have any profiles, clear the list
        if (hasProfiles()) {
            profiles.clear();
        }
    }

    /**
     * If a profile point is so configured, print whether it's starting or stopping.
     *
     * @param point the profile point
     * @param isStarting whether it's starting or stopping
     */
    private static void logProfilePoint(ProfilePoint point, final boolean isStarting) {

        // Check if we want to log this event
        if (point.getLogTime()) {
            log((isStarting ? "Starting " : "Stopping ") + point.getLabel());
        }
    }

    /**
     * Return whether we have any profiles in our list.
     *
     * @return whether the profiles list is non-null and non-empty
     */
    public static boolean hasProfiles() {
        return ((profiles != null) && !profiles.isEmpty());
    }

    /**
     * Log a message (with the current time) to standard out.
     *
     * @param message the text message
     */
    private static void log(final String message) {
        final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        System.out.println(formatter.format(new java.util.Date()) + ": " + message);
    }

    /**
     * Print out all profile points in a JSON format.
     */
    public static void dumpState() {
        final String json = new Gson().toJson(profiles);
        System.out.println(json);
    }

    /**
     * Get an iterator to the profile points so we can run unit tests.
     *
     * @return an iterator to the profile points, or null
     */
    public static Iterator<ProfilePoint> getProfilePoints() {
        return (profiles == null) ? null : profiles.iterator();
    }
}
