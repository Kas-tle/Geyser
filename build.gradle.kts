plugins {
    // Ensure AP works in eclipse (no effect on other IDEs)
    eclipse
    id("geyser.base-conventions")
}

# java
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
