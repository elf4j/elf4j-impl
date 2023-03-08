package elf4j.impl.writer.pattern;

import elf4j.impl.service.LogEntry;
import lombok.NonNull;
import lombok.Value;

@Value
public class ClassPattern implements LogPattern {
    private static final DisplayOption DEFAULT_DISPLAY_OPTION = DisplayOption.FULL;
    @NonNull DisplayOption classDisplayOption;

    public static ClassPattern from(@NonNull String pattern) {
        if (!LogPatternType.isPatternOfType(pattern, LogPatternType.CLASS)) {
            throw new IllegalArgumentException("pattern: " + pattern);
        }
        return new ClassPattern(LogPattern.getPatternOption(pattern)
                .map(displayOption -> ClassPattern.DisplayOption.valueOf(displayOption.toUpperCase()))
                .orElse(DEFAULT_DISPLAY_OPTION));
    }

    @Override
    public boolean includeCallerDetail() {
        return true;
    }

    @Override
    public boolean includeCallerThread() {
        return false;
    }

    @Override
    public void render(LogEntry logEntry, StringBuilder logText) {
        String fullName = logEntry.getCallerClassName();
        switch (classDisplayOption) {
            case FULL:
                logText.append(fullName);
                return;
            case SIMPLE:
                logText.append(fullName.substring(fullName.lastIndexOf('.') + 1));
                return;
            case COMPRESSED: {
                String[] tokens = fullName.split("\\.");
                String simpleName = tokens[tokens.length - 1];
                for (int i = 0; i < tokens.length - 1; i++) {
                    logText.append(tokens[i].charAt(0)).append('.');
                }
                logText.append(simpleName);
                return;
            }
            default:
                throw new IllegalArgumentException("class display option: " + classDisplayOption);
        }
    }

    public enum DisplayOption {
        FULL,
        SIMPLE,
        COMPRESSED
    }
}
