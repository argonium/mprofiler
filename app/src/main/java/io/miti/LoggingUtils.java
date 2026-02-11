package io.miti;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for logging.
 */
public final class LoggingUtils {
    private static final String PACKAGE_OF_INTEREST = "io.miti";

    /** Default constructor is private.  All methods will be static. */
    private LoggingUtils() {
        super();
    }

    /**
     * Get a summary of a generic exception, including just our method references.
     *
     * @param ex the exception of interest
     * @return a string summarizing the exception
     */
    public static String getExceptionSummary(final Exception ex) {

        // Build a message with a summary of the exception
        final StringBuilder sb = new StringBuilder(100);
        sb.append(String.format("Detected exception %s with message '%s'",
                ex.getClass().getName(), ex.getMessage()));

        // If we have a root cause, append it
        if (ex.getCause() != null) {

            // Find the first few distinct "caused by" messages
            final int maxDepth = 4;
            sb.append(" (cause: '");

            // The current throwable on the exception stack
            Throwable throwable = ex.getCause();

            // The set of messages we've already seen, so we don't repeat them
            final Set<String> causes = new HashSet<>(maxDepth);

            // Go a few levels deep to look for distinct messages
            for (int i = 0; (i <= maxDepth) && (throwable != null); ++i) {

                // Save the message for the current throwable
                final String cause = throwable.getMessage();

                // If we've already seen this cause, skip it
                if (causes.contains(cause)) {
                    continue;
                }

                // If we're past the first cause, add more text
                if (i > 0) {
                    sb.append("' caused by '");
                }

                // Append the cause message
                sb.append(cause);

                // Save the cause to the set, so we don't repeat it
                causes.add(cause);

                // Update throwable to go another level deeper
                throwable = throwable.getCause();
            }

            sb.append("')");
        }

        // Prepare to add the stack trace next
        sb.append("; stacktrace: ");

        // Get the stack trace for the exception and find the most recent class
        // references before the exception happened
        final StackTraceElement[] stackTrace = ex.getStackTrace();
        final int maxCount = 15;
        int currCount = 0;
        boolean addedTrace = false;
        for (int i = 0; i < stackTrace.length && (currCount < maxCount); i++) {
            if (stackTrace[i].getClassName().contains(PACKAGE_OF_INTEREST)) {
                ++currCount;
                sb.append(String.format("#%d: %s.%s (line %d) | ", currCount, stackTrace[i].getClassName(), stackTrace[i].getMethodName(), stackTrace[i].getLineNumber()));
                addedTrace = true;
            }
        }

        // If no stack trace added, append "none" to the summary (meaning, no stack trace available)
        if (!addedTrace) {
            sb.append("none");
        } else {
            // We added at least one stack trace, ending with " | ", so remove the last 3 characters
            final int len = sb.length();
            sb.delete(len - 3, len);
        }

        return sb.toString();
    }

    /**
     * Get a stack trace without using an exception.
     *
     * @return a stack trace string
     */
    public static String getStackTrace() {

        // Get a stack trace of frames and then iterate over the list
        final StringBuilder sb = new StringBuilder(100);
        sb.append("Stack Trace: ");
        final List<StackWalker.StackFrame> frames = StackWalker.getInstance().walk(frame -> frame.collect(Collectors.toList()));
        final int maxCount = 15;
        int currCount = 0;
        boolean addedTrace = false;
        final int numFrames = frames.size();

        // Check each stack frame to see if we should log it.  Skip
        // the first one since we don't need to log this method.
        for (int i = 1; i < numFrames && (currCount < maxCount); i++) {

            // We only care about classes in our package
            if (frames.get(i).getClassName().contains(PACKAGE_OF_INTEREST)) {
                ++currCount;
                sb.append(String.format("#%d: %s.%s (line %d) | ", currCount, frames.get(i).getClassName(), frames.get(i).getMethodName(), frames.get(i).getLineNumber()));
                addedTrace = true;
            }
        }

        // If no stack trace added, append "None" to the summary (meaning, no stack trace available)
        if (!addedTrace) {
            sb.append("None");
        } else {
            // We added at least one stack trace, ending with " | ", so remove the last 3 characters
            final int len = sb.length();
            sb.delete(len - 3, len);
        }

        return sb.toString();
    }
}
