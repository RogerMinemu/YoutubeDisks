package dev.youtubedisks.client.cache;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Per-MC-instance local cache for downloaded disc .ogg files. Shared across worlds — same
 * discId = same file = no re-download.
 *
 * <p>Layout: {@code <minecraft-dir>/youtubedisks-cache/<discId>.ogg}.
 */
public final class ClientDiscCache {

    private static final String CACHE_DIR_NAME = "youtubedisks-cache";

    private static final ClientDiscCache INSTANCE = new ClientDiscCache();

    public static ClientDiscCache get() {
        return INSTANCE;
    }

    private final Path baseDir;

    private ClientDiscCache() {
        this.baseDir = Minecraft.getInstance().gameDirectory.toPath().resolve(CACHE_DIR_NAME);
    }

    public Path pathFor(String discId) {
        return baseDir.resolve(discId + ".ogg");
    }

    public boolean has(String discId) {
        return Files.exists(pathFor(discId));
    }

    public byte[] load(String discId) throws IOException {
        return Files.readAllBytes(pathFor(discId));
    }

    public void store(String discId, byte[] bytes) throws IOException {
        Files.createDirectories(baseDir);
        Path target = pathFor(discId);
        Path tmp = baseDir.resolve(discId + ".ogg.tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
