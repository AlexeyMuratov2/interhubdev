package com.example.interhubdev.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads checked-out .env files for IDE and Maven runs before Spring binds configuration.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "interhubdevDotenv";
    private static final List<String> ENV_FILE_NAMES = List.of(".env", ".env.local");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = loadFromRoots(candidateRoots());
        if (values.isEmpty()) {
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.remove(PROPERTY_SOURCE_NAME);
        }

        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (propertySources.contains("defaultProperties")) {
            propertySources.addBefore("defaultProperties", source);
        } else {
            propertySources.addLast(source);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    static Map<String, Object> loadFromRoots(List<Path> roots) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Path root : roots) {
            for (String fileName : ENV_FILE_NAMES) {
                Path file = root.resolve(fileName);
                if (Files.isRegularFile(file)) {
                    values.putAll(loadFile(file));
                }
            }
        }
        return values;
    }

    private static Map<String, Object> loadFile(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                parseLine(line, values);
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return values;
    }

    private static void parseLine(String line, Map<String, Object> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = trimmed.substring(0, separator).trim();
        if (!key.matches("[A-Za-z_][A-Za-z0-9_.-]*")) {
            return;
        }

        String value = trimmed.substring(separator + 1).trim();
        values.put(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            String unquoted = value.substring(1, value.length() - 1);
            return first == '"' ? unescapeDoubleQuoted(unquoted) : unquoted;
        }

        return value;
    }

    private static String unescapeDoubleQuoted(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static List<Path> candidateRoots() {
        LinkedHashMap<Path, Path> roots = new LinkedHashMap<>();
        addRoot(roots, Paths.get("").toAbsolutePath().normalize());
        addRoot(roots, Paths.get("").toAbsolutePath().normalize().resolve("interhubdev"));
        codeSourceRoot().ifPresent(root -> {
            addRoot(roots, root);
            addRoot(roots, root.resolve("interhubdev"));
        });
        return new ArrayList<>(roots.values());
    }

    private static java.util.Optional<Path> codeSourceRoot() {
        try {
            CodeSource codeSource = DotenvEnvironmentPostProcessor.class
                    .getProtectionDomain()
                    .getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return java.util.Optional.empty();
            }

            Path location = Paths.get(codeSource.getLocation().toURI())
                    .toAbsolutePath()
                    .normalize();
            if (Files.isRegularFile(location)) {
                return java.util.Optional.ofNullable(location.getParent());
            }
            if (location.endsWith(Path.of("target", "classes"))) {
                return java.util.Optional.ofNullable(location.getParent())
                        .map(Path::getParent);
            }
            return java.util.Optional.of(location);
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private static void addRoot(Map<Path, Path> roots, Path root) {
        if (root != null) {
            Path normalized = root.toAbsolutePath().normalize();
            roots.putIfAbsent(normalized, normalized);
        }
    }
}
