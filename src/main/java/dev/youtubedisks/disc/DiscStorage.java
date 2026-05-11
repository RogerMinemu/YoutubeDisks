package dev.youtubedisks.disc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side disc file store. Content-addressable: discId = SHA-256(.ogg bytes).
 * Idempotent stores — writing the same bytes twice is a no-op.
 *
 * <p>Layout: {@code <world-save>/youtubedisks/discs/<discId>.ogg}
 */
public final class DiscStorage {

    private final Path baseDir;

    public DiscStorage(MinecraftServer server) {
        this.baseDir = server.getWorldPath(LevelResource.ROOT).resolve("youtubedisks").resolve("discs");
    }

    public Path baseDir() {
        return baseDir;
    }

    public Path pathFor(String discId) {
        return baseDir.resolve(discId + ".ogg");
    }

    public boolean exists(String discId) {
        return Files.exists(pathFor(discId));
    }

    /**
     * Persists {@code oggBytes} keyed by its SHA-256, returning the resulting discId.
     * Skips the disk write if the file already exists.
     */
    public String store(byte[] oggBytes) throws IOException {
        String discId = DiscData.computeDiscId(oggBytes);
        Files.createDirectories(baseDir);
        Path target = pathFor(discId);
        if (!Files.exists(target)) {
            Files.write(target, oggBytes);
        }
        return discId;
    }

    public byte[] load(String discId) throws IOException {
        return Files.readAllBytes(pathFor(discId));
    }
}
