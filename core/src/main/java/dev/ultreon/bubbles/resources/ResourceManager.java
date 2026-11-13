package dev.ultreon.bubbles.resources;

import dev.ultreon.bubbles.resources.android.DeferredResourcePackage;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Logger;
import dev.ultreon.libs.commons.v0.exceptions.SyntaxException;
import dev.ultreon.libs.commons.v0.util.IOUtils;
import dev.ultreon.libs.functions.v0.misc.ThrowingSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceManager {
    private final Map<Identifier, byte[]> assets = new ConcurrentHashMap<>();
    protected final List<ResourcePackage> resourcePackages = new ArrayList<>();
    public static Logger logger = (level, msg, t) -> {};
    private final String root;

    public ResourceManager(String root) {
        this.root = root;
    }

    public boolean canScanFiles() {
        return true;
    }

    public InputStream openResourceStream(Identifier entry) {
        @Nullable Resource resource = this.getResource(entry);
        return resource == null ? null : resource.openStream();
    }

    @Nullable
    public Resource getResource(Identifier entry) {
        for (var resourcePackage : this.resourcePackages) {
            if (resourcePackage.has(entry)) {
                return resourcePackage.get(entry);
            }
        }

        logger.warn("Unknown resource: " + entry);


        return null;
    }

    public void importDeferredPackage(Class<?> ref) {
        this.resourcePackages.add(new DeferredResourcePackage(ref, this.root));
    }

    public void importPackage(URL url) throws IOException {
        if (url.getProtocol().equals("file")) {
            try {
                this.importPackage(new File(url.toURI()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if (url.getProtocol().equals("jar")) {
            this.importFilePackage(new ZipInputStream(new URL(url.getPath().split("!/", 2)[0]).openStream()));
        } else {
            this.importFilePackage(new ZipInputStream(url.openStream()));
        }
    }

    public void importPackage(Path path) throws IOException {
        this.importPackage(path.toUri().toURL());
    }

    public void importPackage(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Resource package doesn't exists: " + file.getAbsolutePath());
        }

        if (file.isFile()) {
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                this.importFilePackage(new ZipInputStream(Files.newInputStream(file.toPath())));
            } else {
                logger.warn("Resource package isn't a .jar or .zip file: " + file.getPath());
            }
        } else if (file.isDirectory()) {
            this.importDirectoryPackage(file);
        }
    }

    @SuppressWarnings({"unused"})
    private void importDirectoryPackage(File file) {
        // Check if it's a directory.
        assert file.isDirectory();

        try {
            // Prepare (entry -> resource) mappings/
            Map<Identifier, Resource> map = new HashMap<>();

            // Get assets directory.
            var assets = new File(file, this.root + "/");

            // Check if assets directory exists.
            if (assets.exists()) {
                // List files in assets dir.
                var files = assets.listFiles();

                // Loop listed files.
                for (var assetsPackage : files != null ? files : new File[0]) {
                    // Get assets-package namespace from the name create the listed file (that's a dir).
                    var namespace = assetsPackage.getName();

                    // Walk assets package.
                    try (var walk = Files.walk(assetsPackage.toPath())) {
                        for (var assetPath : walk.collect(Collectors.toList())) {
                            // Convert to file object.
                            var asset = assetPath.toFile();

                            // Check if it's a file, if not we will walk to the next file / folder in the Files.walk(...) list.
                            if (!asset.isFile()) {
                                continue;
                            }

                            // Create resource with file input stream.
                            ThrowingSupplier<InputStream, IOException> sup = () -> Files.newInputStream(asset.toPath());
                            var resource = new Resource(sup);

                            // Continue to next file / folder if asset path is the same path as the assets package.
                            if (assetPath.toFile().equals(assetsPackage)) {
                                continue;
                            }

                            // Calculate resource path.
                            var relative = assetsPackage.toPath().relativize(assetPath);
                            var s = relative.toString().replaceAll("\\\\", "/");

                            // Create resource entry/
                            Identifier entry;
                            try {
                                entry = new Identifier(namespace, s);
                            } catch (SyntaxException e) {
                                logger.error("Invalid resource identifier:", e);
                                continue;
                            }

                            // Add resource mapping for (entry -> resource).
                            map.put(entry, resource);
                        }
                    }
                }

                this.resourcePackages.add(new ResourcePackage(map));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void importFilePackage(ZipInputStream file) throws IOException {
        // Check for .jar files.
        // Prepare (entry -> resource) mappings.
        Map<Identifier, Resource> map = new HashMap<>();

        // Create jar file instance from file.
        try {
            // Loop jar entries.
            ZipEntry entry;
            while ((entry = file.getNextEntry()) != null) {
                // Get name create the jar entry.
                var name = entry.getName();
                var bytes = IOUtils.readAllBytes(file);
                ThrowingSupplier<InputStream, IOException> sup = () -> new ByteArrayInputStream(bytes);

                // Check if it isn't a directory, because we want a file.
                if (!entry.isDirectory()) {
                    this.addEntry(map, name, sup);
                }
                file.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.resourcePackages.add(new ResourcePackage(map));

        file.close();
    }

    private void addEntry(Map<Identifier, Resource> map, String name, ThrowingSupplier<InputStream, IOException> sup) {
        var splitPath = name.split("/", 3);

        if (splitPath.length >= 3) {
            if (name.startsWith(this.root + "/")) {
                // Get namespace and path from split path
                var namespace = splitPath[1];
                var path = splitPath[2];

                // Resource
                var resource = new Resource(sup);

                try {
                    // Entry
                    var entry = new Identifier(namespace, path);

                    // Add (entry -> resource) mapping.
                    map.put(entry, resource);
                } catch (Throwable ignored) {

                }
            }
        }
    }

    @NotNull
    public List<byte[]> getAllDataByPath(@NotNull String path) {
        List<byte[]> data = new ArrayList<>();
        for (var resourcePackage : this.resourcePackages) {
            var identifierResourceMap = resourcePackage.mapEntries();
            for (var entry : identifierResourceMap.entrySet()) {
                if (entry.getKey().path().equals(path)) {
                    var bytes = entry.getValue().loadOrGet();
                    if (bytes == null) continue;

                    data.add(entry.getValue().getData());
                }
            }
        }

        return data;
    }

    @NotNull
    public List<byte[]> getAllDataById(@NotNull Identifier id) {
        List<byte[]> data = new ArrayList<>();
        for (var resourcePackage : this.resourcePackages) {
            if (resourcePackage.has(id)) {
                var resource = resourcePackage.get(id);
                if (resource == null) continue;
                var bytes = resource.loadOrGet();
                if (bytes == null) continue;

                data.add(resource.getData());
            }
        }

        return data;
    }

    public String getRoot() {
        return this.root;
    }
}
