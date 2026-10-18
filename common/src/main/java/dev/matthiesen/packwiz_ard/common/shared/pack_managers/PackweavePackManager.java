/**
 * Inspired by Packweave's Installer included in the Packweave source code, but rewritten and adapted to work with PackWiz-ard's architecture.
 * Original Author: Packweavers
 * Source: https://github.com/packweavers/packweave/tree/main/installer
 * License: GNU General Public License v3.0
 */
package dev.matthiesen.packwiz_ard.common.shared.pack_managers;

import dev.matthiesen.matthiesen_core.common.api.platform.loader.Environment;
import dev.matthiesen.packwiz_ard.common.PackWizardCommon;
import dev.matthiesen.packwiz_ard.common.PackWizardServerCommon;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.PackStatus;
import dev.matthiesen.packwiz_ard.common.config.PWConfig;
import dev.matthiesen.packwiz_ard.common.shared.exceptions.PackUrlException;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.AsyncCommandTask;
import dev.matthiesen.packwiz_ard.common.shared.interfaces.IPackManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class PackweavePackManager implements IPackManager {
    public static final String UPDATE_PACKWEAVE_TASK_NAME = "updatePackweave";
    private static final int STATE_VERSION = 2;
    private static final String STATE_FILE = ".packweaver-state.json";
    private static final String WORK_DIR = ".packweaver";
    private static final String MODPACK_URL_FILE = "modpack.url";
    private static final String RELEASE_PREFIX = "release+";
    private static final String CHECKED = "packweaver.checked";
    private static final String RULE = "================================================================";
    private static final Pattern RELEASE = Pattern.compile("\\d+(\\.\\d+)+");
    private static final String[] LOADERS = { "fabric", "quilt", "forge", "neoforge" };
    private static final String UA = "packwiz-ard/1.0.0 (+https://github.com/Matthiesen-dev/packwiz-ard/)";
    private static final String CONSOLE_LOG = "packwiz-ard: %s";

    private static final Component UPDATE_FINISHED = Component.literal("Packweave has finished updating. Restart for changes to take effect.");
    private static final List<AsyncCommandTask> TASKS = new ArrayList<>();
    private static final AtomicBoolean MODS_CHANGED = new AtomicBoolean();

    private static final java.util.function.Predicate<String> HAS_TASK = name -> TASKS.stream().anyMatch(task -> task.hasName(name));

    private static boolean restartRequired = false;

    private static void consoleLog(String message) {
        String log = String.format(CONSOLE_LOG, message);
        System.out.println(log);
    }

    private static void consoleError(String message) {
        String log = String.format(CONSOLE_LOG, message);
        System.err.println(log);
    }

    @Override
    public String getUpdateTaskName() {
        return UPDATE_PACKWEAVE_TASK_NAME;
    }

    @Override
    public String getConfiguredLink() {
        try {
            return readModpackUrl(gameDir());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void setConfiguredLink(String link) throws IOException {
        Path dir = gameDir();
        Files.createDirectories(dir);
        String normalized = normalizeLink(link);
        Path target = dir.resolve(MODPACK_URL_FILE);
        if (normalized == null || normalized.isBlank()) {
            Files.deleteIfExists(target);
            return;
        }
        atomicWrite(target, (normalized + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public PackStatus getPackStatus(String packLink) {
        String normalizedLink = resolvePackLink(packLink);
        long checkedAt = System.currentTimeMillis();

        if (normalizedLink == null || normalizedLink.isBlank()) {
            return new PackStatus(
                    PackStatus.State.UNCONFIGURED,
                    null,
                    "Set a modpack.url link to check for client updates.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }

        if (restartRequired) {
            return new PackStatus(
                    PackStatus.State.RESTART_REQUIRED,
                    normalizedLink,
                    "The game needs to be restarted to apply updates.",
                    true,
                    false,
                    false,
                    checkedAt
            );
        }

        try {
            URL url = testPackLink(normalizedLink);
            String currentHash = getLatestPackHash(normalizedLink);
            String lastSeenHash = PWConfig.COMMON_CONFIG.lastSeenPackTomlHash.get();
            boolean updateAvailable = !currentHash.equals(lastSeenHash);

            return new PackStatus(
                    updateAvailable ? PackStatus.State.UPDATE_AVAILABLE : PackStatus.State.UP_TO_DATE,
                    url.toExternalForm(),
                    updateAvailable ? "A client update is available." : "The client pack source is valid and up to date.",
                    true,
                    updateAvailable,
                    false,
                    checkedAt
            );
        } catch (PackUrlException e) {
            String message = e.getMessage() == null ? "The modpack URL could not be validated." : e.getMessage();
            PackStatus.State state = message.contains("valid URL")
                    ? PackStatus.State.INVALID_URL
                    : message.contains("valid data") || message.contains("invalid data") || message.contains("JSON")
                    ? PackStatus.State.INVALID_TOML
                    : PackStatus.State.ERROR;

            return new PackStatus(
                    state,
                    normalizedLink,
                    message,
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IOException e) {
            return new PackStatus(
                    PackStatus.State.UNREACHABLE,
                    normalizedLink,
                    "The modpack URL could not be reached or read.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        } catch (IllegalStateException e) {
            return new PackStatus(
                    PackStatus.State.INVALID_TOML,
                    normalizedLink,
                    "The modpack source contains invalid data.",
                    false,
                    false,
                    false,
                    checkedAt
            );
        }
    }

    @Override
    public String getLatestPackHash(String packLink) throws PackUrlException, IOException {
        String base = resolvePackLink(packLink);
        if (base == null || base.isBlank()) {
            throw new IOException("No modpack URL provided.");
        }
        if (base.startsWith(RELEASE_PREFIX)) {
            String webUrl = base.substring(RELEASE_PREFIX.length()).trim();
            String token = readToken(gameDir());
            String[] or = ownerRepo(webUrl);
            String repoApi = apiBase(webUrl) + "/repos/" + or[0] + "/" + or[1];
            ApiResp resp = apiGet(repoApi + "/releases/latest", token, null);
            if (resp.body == null) {
                throw new IOException("No releases found for " + or[0] + "/" + or[1] + ".");
            }
            Map<String, Object> rel = asMap(Json.parse(resp.body));
            String tag = str(rel.get("tag_name"));
            if (tag == null || tag.isEmpty()) {
                throw new IOException("Malformed release data.");
            }
            return sha1Of(tag.getBytes(StandardCharsets.UTF_8));
        }
        return sha1Of(getBytes(base + "/index.json"));
    }

    @Override
    public void pollTasks() {
        var tasksIterator = TASKS.listIterator();

        while (tasksIterator.hasNext()) {
            var task = tasksIterator.next();
            task.tick();

            if (task.pollFinished()) {
                Throwable exception = null;
                Component message = null;

                try {
                    task.getFuture().join();
                    if (task.hasName(UPDATE_PACKWEAVE_TASK_NAME)) {
                        message = UPDATE_FINISHED;
                    }
                } catch (Throwable e) {
                    exception = e;
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    if (cause instanceof RuntimeException runtimeException && runtimeException.getCause() != null) {
                        cause = runtimeException.getCause();
                    }

                    if (cause instanceof InterruptedException) {
                        message = Component.literal("Process was interrupted. Check the console for details.");
                    } else if (cause instanceof IOException) {
                        message = Component.literal("Read/write process failed. Check the console for details.");
                    } else if (cause != null && cause.getMessage() != null) {
                        message = Component.literal(cause.getMessage());
                    }

                    if (message == null) {
                        message = Component.literal("Command failed. Check the console for errors.");
                    }
                }

                task.sendMessage(message);
                if (exception != null) {
                    PackWizardCommon.INSTANCE.createErrorLog("Unexpected exception occurred whilst polling Packweave command status", exception);
                }
                tasksIterator.remove();
            }
        }
    }

    @Override
    public boolean hasBootstrap() {
        return false;
    }

    @Override
    public boolean isAsyncTaskRunning(String name) {
        return HAS_TASK.test(name);
    }

    @Override
    public URL testPackLink(@NotNull String packLink) throws PackUrlException {
        try {
            String normalized = normalizeLink(resolvePackLink(packLink));
            if (normalized == null || normalized.isBlank()) {
                throw new PackUrlException("There is no modpack URL link to update from. Add this using /packwizard link [url].");
            }

            if (normalized.startsWith(RELEASE_PREFIX)) {
                String raw = normalized.substring(RELEASE_PREFIX.length()).trim();
                URL url = URI.create(raw).toURL();
                validatePackSource(normalized);
                return url;
            }

            URL url = URI.create(normalized).toURL();
            validatePackSource(url.toExternalForm());
            return url;
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            throw new PackUrlException("The link submitted is not a valid URL.");
        } catch (IOException e) {
            throw new PackUrlException("Check this file exists and is a valid Packweave source.");
        } catch (IllegalStateException e) {
            throw new PackUrlException("The file contains invalid data.");
        }
    }

    @Override
    public boolean update(String packLink, boolean ignoredHasBootstrap, Consumer<Component> messageSink, Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (!HAS_TASK.test(UPDATE_PACKWEAVE_TASK_NAME)) {
            TASKS.add(new AsyncCommandTask(CompletableFuture.runAsync(() -> {
                try {
                    if (PackWizardServerCommon.isServerRunning()) {
                        sendWebhook(PWConfig.getPackUpdateTriggeredEmbed());
                    }

                    String baseUrl = normalizeLink(resolvePackLink(packLink));
                    if (baseUrl == null || baseUrl.isBlank()) {
                        throw new IOException("No modpack URL provided.");
                    }

                    run(gameDir(), baseUrl, currentEnv(), inGame());
                    PWConfig.setPackTomlHash(getLatestPackHash(baseUrl));
                    onSuccess.run();
                    if (PackWizardServerCommon.isServerRunning()) {
                        sendWebhook(PWConfig.getPackUpdateFinishedEmbed());
                    }
                } catch (Exception e) {
                    onFailure.accept(e);
                    if (PackWizardServerCommon.isServerRunning()) {
                        sendWebhook(PWConfig.getPackUpdateFailedEmbed());
                    }
                    throw new RuntimeException(e);
                }
            }), UPDATE_PACKWEAVE_TASK_NAME, 10, messageSink));
            return true;
        }

        return false;
    }

    private static void run(Path dir, String url, String env, boolean inGame) throws Exception {
        url = normalizeLink(url);
        if (url == null || url.isBlank()) {
            throw new IOException("No modpack URL provided.");
        }
        if (url.startsWith(RELEASE_PREFIX)) {
            url = RELEASE_PREFIX + url.substring(RELEASE_PREFIX.length()).trim();
        }
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith(RELEASE_PREFIX)) {
            throw new IOException("Invalid pack URL: " + url);
        }

        finishStage(dir, 1);

        if (url.startsWith(RELEASE_PREFIX)) {
            runRelease(dir, url.substring(RELEASE_PREFIX.length()), env, inGame);
        } else {
            runCommit(dir, url, env, inGame);
        }

        checkVersions(dir, inGame);
        if (inGame && MODS_CHANGED.get()) {
            restartRequired = true;
            consoleLog("mods updated, please restart the game to play");
        }
    }

    private static void runCommit(Path dir, String baseUrl, String env, boolean inGame) throws Exception {
        String manifestText = "";
        Map<String, Object> mp = null;
        try {
            String raw = remoteText(dir, baseUrl + "/modpack.json", "modpack.json");
            if (raw == null) {
                throw new IOException("HTTP 404 for " + baseUrl + "/modpack.json");
            }
            manifestText = raw;
            cacheManifest(dir, raw.getBytes(StandardCharsets.UTF_8));
            mp = asMap(Json.parse(raw));
            String mc = str(mp.get("minecraft"));
            String loader = str(mp.get("loader"));
            consoleLog("syncing from " + baseUrl
                    + (mc != null ? " (Minecraft " + mc + (loader != null ? " / " + loader : "") + ")" : ""));
        } catch (Exception e) {
            consoleLog("syncing from " + baseUrl);
            consoleError("! couldn't read modpack.json: " + e.getMessage());
        }

        String indexText = remoteText(dir, baseUrl + "/index.json", "index.json");
        if (indexText == null) {
            throw new IOException("HTTP 404 for " + baseUrl + "/index.json");
        }
        String rulesText;
        try {
            rulesText = remoteText(dir, baseUrl + "/.packweaveunlock", ".packweaveunlock");
            if (rulesText == null) {
                rulesText = "";
            }
        } catch (IOException e) {
            rulesText = readRulesText(dir);
            if (rulesText == null) {
                throw e;
            }
        }
        saveRules(dir, rulesText);

        String signature = "raw:" + sha1Of((manifestText + "\u0000" + indexText + "\u0000" + rulesText + "\u0000" + env).getBytes(StandardCharsets.UTF_8));
        if (signature.equals(loadMeta(dir)[0])) {
            List<String> needs = verifyFiles(dir, parsePreserve(rulesText));
            if (needs.isEmpty()) {
                consoleLog("up to date");
                return;
            }
            consoleLog("repairing " + needs.size() + (needs.size() == 1 ? " file" : " files"));
            SyncInput repaired = buildFromSource(dir, baseUrl, env, mp, indexText);
            int failed = apply(dir, repairTasks(repaired.tasks, needs), repaired.local, repaired.preserve, inGame, null, null, true);
            if (failed > 0) {
                throw new IOException("Done with " + failed + " failures.");
            }
            return;
        }

        SyncInput input = buildFromSource(dir, baseUrl, env, mp, indexText);
        input.rules = rulesText;
        input.preserve = parsePreserve(rulesText);
        int failed = apply(dir, input.tasks, input.local, input.preserve, inGame, signature, null, false);
        if (failed > 0) {
            throw new IOException("Done with " + failed + " failures.");
        }
    }

    private static void runRelease(Path dir, String webUrl, String env, boolean inGame) throws Exception {
        consoleLog("syncing from " + webUrl);
        String token = readToken(dir);
        String[] or = ownerRepo(webUrl);
        String repoApi = apiBase(webUrl) + "/repos/" + or[0] + "/" + or[1];
        String[] meta = loadMeta(dir);
        String lastTag = meta[0];
        boolean needManifest = !Files.isRegularFile(dir.resolve(WORK_DIR).resolve("modpack.json"));

        ApiResp resp = apiGet(repoApi + "/releases/latest", token, lastTag != null && !needManifest ? meta[1] : null);
        if (resp.code == 304) {
            verifyLocal(dir, repoApi, token, env, inGame, lastTag);
            return;
        }
        if (resp.code == 404) {
            throw new IOException("No releases found for " + or[0] + "/" + or[1] + ".");
        }

        Map<String, Object> rel = asMap(Json.parse(resp.body));
        String tag = str(rel.get("tag_name"));
        if (tag == null || tag.isEmpty()) {
            throw new IOException("Malformed release data.");
        }

        Map<String, String> assets = new LinkedHashMap<>();
        for (Object ao : asList(rel.get("assets"))) {
            Map<String, Object> a = asMap(ao);
            String n = str(a.get("name"));
            String u = str(a.get("url"));
            if (n != null && u != null) {
                assets.put(n, u);
            }
        }

        if (tag.equals(lastTag) && !needManifest) {
            saveState(dir, loadState(dir), tag, resp.etag, null);
            verifyLocal(dir, repoApi, token, env, inGame, tag);
            return;
        }

        consoleLog("updating to " + tag);
        String packUrl = assets.get("pack.zip");
        String deltaName = lastTag == null || needManifest ? null : "delta-" + lastTag + ".zip";
        boolean usedDelta = false;
        Map<String, byte[]> entries;

        if (deltaName != null && assets.containsKey(deltaName)) {
            entries = unzip(fetchBytes(assets.get(deltaName), token, "application/octet-stream"));
            usedDelta = true;
        } else {
            if (packUrl == null) {
                throw new IOException("Release " + tag + " has no pack.zip asset.");
            }
            entries = unzip(fetchBytes(packUrl, token, "application/octet-stream"));
        }

        SyncInput in = buildFromZip(entries, env);
        if (usedDelta && needsFull(dir, in)) {
            if (packUrl == null) {
                throw new IOException("Release " + tag + " has no pack.zip asset.");
            }
            entries = unzip(fetchBytes(packUrl, token, "application/octet-stream"));
            in = buildFromZip(entries, env);
        }

        byte[] mpBytes = entries.get("modpack.json");
        if (mpBytes != null) {
            cacheManifest(dir, mpBytes);
        }

        byte[] newToken = entries.get("modpack.token");
        if (newToken != null) {
            String t = cleanToken(new String(newToken, StandardCharsets.UTF_8));
            if (t != null && !t.equals(token)) {
                atomicWrite(dir.resolve("modpack.token"), (t + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        byte[] newUrl = entries.get("modpack.url");
        if (newUrl != null) {
            String moved = cleanUrl(new String(newUrl, StandardCharsets.UTF_8));
            if (moved != null && !moved.equalsIgnoreCase(RELEASE_PREFIX + webUrl)) {
                atomicWrite(dir.resolve("modpack.url"), (moved + "\n").getBytes(StandardCharsets.UTF_8));
                consoleLog("the pack moved to " + moved);
            }
        }

        saveRules(dir, in.rules);

        int failed = apply(dir, in.tasks, in.local, in.preserve, inGame, tag, resp.etag, false);
        if (failed > 0) {
            throw new IOException("Done with " + failed + " failures.");
        }
    }

    private static void verifyLocal(Path dir, String repoApi, String token, String env, boolean inGame, String tag) throws Exception {
        Map<String, String> state = loadState(dir);
        Map<String, String> stats = loadStats(dir);
        List<String> needs = new ArrayList<>();
        boolean dirty = false;

        for (Map.Entry<String, String> e : state.entrySet()) {
            String rel = e.getKey();
            Path p = dir.resolve(rel);
            if (!Files.isRegularFile(p)) {
                if (Files.isRegularFile(dir.resolve(rel + ".disabled"))) {
                    continue;
                }
                needs.add(rel);
                continue;
            }
            if (contentPath(rel)) {
                String cur;
                try {
                    cur = Files.size(p) + "," + Files.getLastModifiedTime(p).toMillis();
                } catch (Exception ex) {
                    cur = "";
                }

                if (cur.equals(stats.get(rel))) {
                    continue;
                }

                String disk = diskSha(p);
                if (disk != null && disk.equalsIgnoreCase(e.getValue())) {
                    stats.put(rel, cur);
                    dirty = true;
                    continue;
                }
            }
            needs.add(rel);
        }

        if (needs.isEmpty()) {
            if (dirty) {
                saveState(dir, state, null, null, stats);
            }
            consoleLog("up to date (" + tag + ")");
            return;
        }

        consoleLog("repairing " + needs.size() + (needs.size() == 1 ? " file" : " files"));

        ApiResp resp = apiGet(repoApi + "/releases/latest", token, null);
        if (resp.body == null) {
            throw new IOException("Couldn't reach the release to repair files.");
        }
        Map<String, Object> rel = asMap(Json.parse(resp.body));
        Map<String, String> assets = new LinkedHashMap<>();
        for (Object ao : asList(rel.get("assets"))) {
            Map<String, Object> a = asMap(ao);
            String n = str(a.get("name"));
            String u = str(a.get("url"));
            if (n != null && u != null) {
                assets.put(n, u);
            }
        }

        String packUrl = assets.get("pack.zip");
        if (packUrl == null) {
            throw new IOException("Release has no pack.zip asset.");
        }
        Map<String, byte[]> entries = unzip(fetchBytes(packUrl, token, "application/octet-stream"));
        SyncInput in = buildFromZip(entries, env);
        List<String[]> tasks = new ArrayList<>();
        for (String[] t : in.tasks) {
            if (needs.contains(t[0])) {
                tasks.add(t);
            }
        }
        int failed = apply(dir, tasks, in.local, in.preserve, inGame, null, null, true);
        if (failed > 0) {
            throw new IOException("Done with " + failed + " failures.");
        }
    }

    private static boolean contentPath(String rel) {
        return rel.startsWith("mods/") || rel.startsWith("resourcepacks/") || rel.startsWith("shaderpacks/") || rel.startsWith("datapacks/") || isDocument(rel);
    }

    private static List<String> verifyFiles(Path dir, List<Pattern> preserve) {
        Map<String, String> state = loadState(dir);
        Map<String, String> stats = loadStats(dir);
        List<String> needs = new ArrayList<>();

        for (Map.Entry<String, String> e : state.entrySet()) {
            String rel = e.getKey();
            Path p = dir.resolve(rel);
            if (!Files.isRegularFile(p)) {
                if (!Files.isRegularFile(dir.resolve(rel + ".disabled"))) {
                    needs.add(rel);
                }
                continue;
            }
            if (contentPath(rel)) {
                String cur;
                try {
                    cur = Files.size(p) + "," + Files.getLastModifiedTime(p).toMillis();
                } catch (Exception ex) {
                    cur = "";
                }

                if (cur.equals(stats.get(rel))) {
                    continue;
                }

                String disk = diskSha(p);
                if (disk != null && disk.equalsIgnoreCase(e.getValue())) {
                    stats.put(rel, cur);
                    continue;
                }
                if (matches(preserve, rel)) {
                    continue;
                }
            }
            needs.add(rel);
        }

        return needs;
    }

    private static SyncInput buildFromZip(Map<String, byte[]> entries, String env) throws IOException {
        SyncInput in = new SyncInput();
        Map<String, Object> manifest = null;
        byte[] mpBytes = entries.get("modpack.json");
        if (mpBytes != null) {
            try {
                manifest = asMap(Json.parse(new String(mpBytes, StandardCharsets.UTF_8)));
            } catch (Exception e) {
                consoleError("! the release's modpack.json isn't valid: " + e.getMessage());
            }
        }
        String datapacks = datapackFolder(manifest);
        byte[] idx = entries.get("index.json");
        if (idx == null) {
            throw new IOException("Release is missing index.json.");
        }
        Map<String, Object> index = asMap(Json.parse(new String(idx, StandardCharsets.UTF_8)));
        for (Object eo : asList(index.get("files"))) {
            Map<String, Object> f = asMap(eo);
            String path = str(f.get("path"));
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.startsWith("content/") && path.endsWith(".json")) {
                byte[] cj = entries.get(path);
                if (cj == null) {
                    consoleError("! " + path + ": missing from release");
                    continue;
                }
                String[] t = contentTask(path, asMap(Json.parse(new String(cj, StandardCharsets.UTF_8))), env, datapacks);
                if (t != null) {
                    in.tasks.add(t);
                }
            } else if (path.startsWith("overrides/")) {
                String inst = path.substring("overrides/".length());
                if (inst.isEmpty()) {
                    continue;
                }
                in.tasks.add(new String[] { inst, null, str(f.get("sha1")) });
                byte[] bytes = entries.get(path);
                if (bytes != null) {
                    in.local.put(inst, bytes);
                }
            } else if (isDocument(path)) {
                in.tasks.add(new String[] { path, null, str(f.get("sha1")) });
                byte[] bytes = entries.get(path);
                if (bytes != null) {
                    in.local.put(path, bytes);
                }
            }
        }
        byte[] pres = entries.get(".packweaveunlock");
        if (pres != null) {
            in.preserve = parsePreserve(new String(pres, StandardCharsets.UTF_8));
            in.rules = new String(pres, StandardCharsets.UTF_8);
        }
        return in;
    }

    private static boolean needsFull(Path dir, SyncInput in) {
        for (String[] t : in.tasks) {
            if (t[1] != null || in.local.containsKey(t[0])) {
                continue;
            }
            String want = t[2];
            String disk = diskSha(dir.resolve(t[0]));
            if (want == null || !want.equalsIgnoreCase(disk)) {
                return true;
            }
        }
        return false;
    }

    private static SyncInput buildFromSource(Path dir, String baseUrl, String env, Map<String, Object> manifest, String indexText) throws Exception {
        SyncInput in = new SyncInput();
        Map<String, Object> index = asMap(Json.parse(indexText));
        String datapacks = datapackFolder(manifest);

        Map<String, String> contentPaths = new LinkedHashMap<>();
        for (Object eo : asList(index.get("files"))) {
            Map<String, Object> f = asMap(eo);
            String path = str(f.get("path"));
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.startsWith("content/") && path.endsWith(".json")) {
                contentPaths.put(path, str(f.get("sha1")));
            } else if (path.startsWith("overrides/")) {
                String inst = path.substring("overrides/".length());
                if (!inst.isEmpty()) {
                    in.tasks.add(new String[] { inst, null, str(f.get("sha1")) });
                    in.local.put(inst, getBytes(baseUrl + "/" + enc(path)));
                }
            } else if (isDocument(path)) {
                in.tasks.add(new String[] { path, null, str(f.get("sha1")) });
                in.local.put(path, getBytes(baseUrl + "/" + enc(path)));
            }
        }

        ExecutorService metaPool = Executors.newFixedThreadPool(8);
        List<Future<String[]>> metaFutures = new ArrayList<>();
        for (final Map.Entry<String, String> entry : contentPaths.entrySet()) {
            final String cp = entry.getKey();
            final String sha1 = entry.getValue();
            metaFutures.add(metaPool.submit(() -> {
                try {
                    String text = contentJson(dir, baseUrl, cp, sha1);
                    Map<String, Object> m = asMap(Json.parse(text));
                    return contentTask(cp, m, env, datapacks);
                } catch (Exception e) {
                    consoleError("! " + cp + ": " + e.getMessage());
                    return null;
                }
            }));
        }
        for (Future<String[]> ft : metaFutures) {
            String[] t = ft.get();
            if (t != null) {
                in.tasks.add(t);
            }
        }
        metaPool.shutdown();

        String rulesText;
        try {
            rulesText = remoteText(dir, baseUrl + "/.packweaveunlock", ".packweaveunlock");
            if (rulesText == null) {
                rulesText = "";
            }
        } catch (Exception e) {
            rulesText = readRulesText(dir);
            if (rulesText == null) {
                rulesText = "";
            }
        }
        in.rules = rulesText;
        in.preserve = parsePreserve(rulesText);

        return in;
    }

    private static List<String[]> repairTasks(List<String[]> all, List<String> needs) {
        List<String[]> tasks = new ArrayList<>();
        for (String[] t : all) {
            if (needs.contains(t[0])) {
                tasks.add(t);
            }
        }
        return tasks;
    }

    private static String[] contentTask(String cp, Map<String, Object> m, String env, String datapacks) {
        Map<String, Object> src = asMap(asMap(m.get("sources")).get(str(m.get("preferred"))));
        String dl = str(src.get("downloadUrl"));
        String fn = str(src.get("filename"));
        if (dl == null || dl.isEmpty() || fn == null || fn.isEmpty()) {
            return null;
        }
        if ("server".equals(env) && "unsupported".equals(str(m.get("serverSide")))) {
            return null;
        }
        if ("client".equals(env) && "unsupported".equals(str(m.get("clientSide")))) {
            return null;
        }
        String[] segs = cp.split("/");
        String sub = segs.length >= 2 ? segs[1] : "mods";
        String folder = "shaders".equals(sub) ? "shaderpacks" : "datapacks".equals(sub) ? datapacks : sub;
        return new String[] { folder + "/" + fn, dl, str(src.get("sha1")) };
    }

    private static boolean isDocument(String path) {
        return path.indexOf('/') < 0 && path.toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private static String datapackFolder(Map<String, Object> manifest) {
        String folder = manifest == null ? null : safeRel(str(manifest.get("datapacks")));
        return folder == null ? "datapacks" : folder;
    }

    private static String contentJson(Path dir, String baseUrl, String path, String sha1) throws Exception {
        Path cache = sha1 == null || sha1.isEmpty() ? null : dir.resolve(WORK_DIR).resolve("content").resolve(sha1.toLowerCase(java.util.Locale.ROOT) + ".json");
        if (cache != null && Files.isRegularFile(cache)) {
            return Files.readString(cache);
        }
        byte[] bytes = getBytes(baseUrl + "/" + enc(path));
        if (cache != null) {
            if (!sha1.equalsIgnoreCase(sha1Of(bytes))) {
                throw new IOException(path + " doesn't match index.json yet. Try again in a few minutes");
            }
            Files.createDirectories(cache.getParent());
            atomicWrite(cache, bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String remoteText(Path dir, String url, String name) throws IOException {
        Path folder = dir.resolve(WORK_DIR).resolve("remote");
        Path body = folder.resolve(name);
        Path tagFile = folder.resolve(name + ".etag");
        String etag = null;
        if (Files.isRegularFile(body) && Files.isRegularFile(tagFile)) {
            etag = Files.readString(tagFile).trim();
        }
        Remote r = fetchRemote(url, etag);
        if (r.code == 304 && Files.isRegularFile(body)) {
            return Files.readString(body);
        }
        if (r.code == 304) {
            r = fetchRemote(url, null);
        }
        if (r.code == 404) {
            Files.deleteIfExists(body);
            Files.deleteIfExists(tagFile);
            return null;
        }
        Files.createDirectories(folder);
        atomicWrite(body, r.body);
        if (r.etag != null && !r.etag.isEmpty()) {
            atomicWrite(tagFile, r.etag.getBytes(StandardCharsets.UTF_8));
        } else {
            Files.deleteIfExists(tagFile);
        }
        return new String(r.body, StandardCharsets.UTF_8);
    }

    private static final class Remote {
        private int code;
        private byte[] body;
        private String etag;
    }

    private static Remote fetchRemote(String url, String etag) throws IOException {
        String current = url;
        for (int i = 0; i < 5; i++) {
            HttpURLConnection c = (HttpURLConnection) URI.create(current).toURL().openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);
            c.setRequestProperty("User-Agent", UA);
            if (etag != null && !etag.isEmpty()) {
                c.setRequestProperty("If-None-Match", etag);
            }
            Remote r = new Remote();
            r.code = c.getResponseCode();
            if (r.code == 304 || r.code == 404) {
                c.disconnect();
                return r;
            }
            if (r.code / 100 == 3) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) {
                    throw new IOException("HTTP " + r.code + " for " + current);
                }
                current = URI.create(current).resolve(loc).toString();
                continue;
            }
            if (r.code / 100 != 2) {
                c.disconnect();
                throw new IOException("HTTP " + r.code + " for " + current);
            }
            r.etag = c.getHeaderField("ETag");
            try (InputStream in = c.getInputStream()) {
                r.body = readAll(in);
            }
            return r;
        }
        throw new IOException("Too many redirects for " + url);
    }

    private static void cacheManifest(Path dir, byte[] bytes) {
        try {
            Path f = dir.resolve(WORK_DIR).resolve("modpack.json");
            Files.createDirectories(f.getParent());
            atomicWrite(f, bytes);
        } catch (Exception e) {
            consoleError("! couldn't save modpack.json: " + e.getMessage());
        }
    }

    private static Map<String, Object> cachedManifest(Path dir) {
        Path f = dir.resolve(WORK_DIR).resolve("modpack.json");
        if (!Files.isRegularFile(f)) {
            return null;
        }
        try {
            return asMap(Json.parse(Files.readString(f)));
        } catch (Exception e) {
            consoleError("! could not read modpack.json: " + e.getMessage());
            return null;
        }
    }

    private static void saveRules(Path dir, String text) {
        try {
            Path f = dir.resolve(WORK_DIR).resolve("unlock");
            Files.createDirectories(f.getParent());
            atomicWrite(f, (text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            consoleError("! couldn't save the unlock rules: " + e.getMessage());
        }
    }

    private static String readRulesText(Path dir) {
        try {
            Path f = dir.resolve(WORK_DIR).resolve("unlock");
            return Files.isRegularFile(f) ? Files.readString(f) : null;
        } catch (Exception e) {
            consoleError("! couldn't read the saved unlock rules: " + e.getMessage());
            return null;
        }
    }

    private static void checkVersions(Path dir, boolean inGame) {
        Map<String, Object> mp = cachedManifest(dir);
        if (mp == null) {
            return;
        }
        Versions have = inGame ? runningVersions() : instanceVersions(dir);
        if (have == null) {
            return;
        }
        System.setProperty(CHECKED, "1");
        String wantMc = blank(str(mp.get("minecraft")));
        String wantLoader = blank(str(mp.get("loader")));
        String wantLv = blank(str(mp.get("loaderVersion")));
        boolean stop = false;
        if (wantMc != null && have.minecraft != null && !wantMc.equals(have.minecraft)) {
            stop = RELEASE.matcher(wantMc).matches() && RELEASE.matcher(have.minecraft).matches();
            banner("WRONG MINECRAFT VERSION", "This pack needs Minecraft " + wantMc + ". This instance runs Minecraft " + have.minecraft + ".", "Change the instance's Minecraft version to " + wantMc + " in your launcher" + (stop ? ", then start the game again." : ". The game may crash until you do."));
        }
        if (wantLoader != null && !wantLoader.equalsIgnoreCase("vanilla")) {
            String loader = wantLoader.toLowerCase(java.util.Locale.ROOT);
            String wanted = loaderText(loader, wantLv);
            if (!sameLoader(loader, have.loader, wantMc)) {
                banner("WRONG MOD LOADER", "This pack uses " + wanted + ". This instance runs " + (have.loader == null ? "no mod loader" : loaderText(have.loader, have.loaderVersion)) + ".", "Switch the instance to " + wanted + " in your launcher. The game may crash until you do.");
            } else if (wantLv != null && have.loaderVersion != null && !wantLv.equals(have.loaderVersion)) {
                banner("WRONG LOADER VERSION", "This pack uses " + wanted + ". This instance runs " + loaderText(loader, have.loaderVersion) + ".", "Change the instance's " + loaderName(loader) + " version to " + wantLv + " in your launcher. The game may crash until you do.");
            }
        }
        if (stop) {
            consoleError(inGame ? "closing the game" : "stopping this launch");
            System.exit(1);
        }
    }

    private static void banner(String title, String problem, String fix) {
        consoleError(RULE);
        consoleError(title);
        consoleError(problem);
        consoleError(fix);
        consoleError(RULE);
    }

    private static boolean sameLoader(String want, String have, String minecraft) {
        if (want.equals(have)) {
            return true;
        }
        return want.equals("neoforge") && "forge".equals(have) && "1.20.1".equals(minecraft);
    }

    private static String loaderText(String loader, String version) {
        return version == null ? loaderName(loader) : loaderName(loader) + " " + version;
    }

    private static String loaderName(String loader) {
        if (loader == null) {
            return "";
        }
        return switch (loader) {
            case "fabric" -> "Fabric";
            case "quilt" -> "Quilt";
            case "forge" -> "Forge";
            case "neoforge" -> "NeoForge";
            default -> loader;
        };
    }

    private static Versions instanceVersions(Path dir) {
        Path root = instanceRoot(dir);
        if (root == null) {
            return null;
        }
        try {
            Map<String, Object> pack = asMap(Json.parse(Files.readString(root.resolve("mmc-pack.json"))));
            List<Object> comps = asList(pack.get("components"));
            String mc = blank(componentVersion(comps, "net.minecraft"));
            if (mc == null) {
                return null;
            }
            for (String loader : LOADERS) {
                String version = componentVersion(comps, loaderUid(loader));
                if (version != null) {
                    return new Versions(mc, loader, blank(version));
                }
            }
            return new Versions(mc, null, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Versions runningVersions() {
        try {
            Class<?> fl = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fl.getMethod("getInstance").invoke(null);
            boolean quilt = hasQuiltClass();
            return new Versions(fabricVersion(fl, loader, "minecraft"), quilt ? "quilt" : "fabric", fabricVersion(fl, loader, quilt ? "quilt_loader" : "fabricloader"));
        } catch (Throwable ignore) {
        }
        Versions neo = fmlVersions("net.neoforged.fml", "neoforge");
        return neo != null ? neo : fmlVersions("net.minecraftforge.fml", "forge");
    }

    private static Versions fmlVersions(String fml, String loader) {
        try {
            return new Versions(fmlVersion(fml, "minecraft"), loader, fmlVersion(fml, loader));
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static String fabricVersion(Class<?> fl, Object loader, String id) throws Exception {
        Object found = ((Optional<?>) fl.getMethod("getModContainer", String.class).invoke(loader, id)).orElse(null);
        if (found == null) {
            return null;
        }
        Object meta = Class.forName("net.fabricmc.loader.api.ModContainer").getMethod("getMetadata").invoke(found);
        Object version = Class.forName("net.fabricmc.loader.api.ModMetadata").getMethod("getVersion").invoke(meta);
        Object text = Class.forName("net.fabricmc.loader.api.Version").getMethod("getFriendlyString").invoke(version);
        return text == null ? null : blank(text.toString());
    }

    private static String fmlVersion(String fml, String id) throws Exception {
        Class<?> list = Class.forName(fml + ".ModList");
        Object mods = list.getMethod("get").invoke(null);
        Object found = ((Optional<?>) list.getMethod("getModContainerById", String.class).invoke(mods, id)).orElse(null);
        if (found == null) {
            return null;
        }
        Method info = Class.forName(fml + ".ModContainer").getMethod("getModInfo");
        Object version = info.getReturnType().getMethod("getVersion").invoke(info.invoke(found));
        return version == null ? null : blank(version.toString());
    }

    private static boolean hasQuiltClass() {
        try {
            Class.forName("org.quiltmc.loader.api.QuiltLoader", false, PackweavePackManager.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String blank(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String loaderUid(String loader) {
        if (loader == null) {
            return null;
        }
        return switch (loader.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "fabric" -> "net.fabricmc.fabric-loader";
            case "quilt" -> "org.quiltmc.quilt-loader";
            case "forge" -> "net.minecraftforge";
            case "neoforge", "neoforged" -> "net.neoforged";
            default -> null;
        };
    }

    private static Path instanceRoot(Path gameDir) {
        Path parent = gameDir.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("mmc-pack.json"))) {
            return parent;
        }
        if (Files.isRegularFile(gameDir.resolve("mmc-pack.json"))) {
            return gameDir;
        }
        return null;
    }

    private static String componentVersion(List<Object> comps, String uid) {
        if (uid == null) {
            return null;
        }
        for (Object o : comps) {
            Map<String, Object> c = asMap(o);
            if (uid.equals(str(c.get("uid")))) {
                String v = str(c.get("version"));
                return v == null ? "" : v;
            }
        }
        return null;
    }

    private static final class Versions {
        private final String minecraft;
        private final String loader;
        private final String loaderVersion;

        private Versions(String minecraft, String loader, String loaderVersion) {
            this.minecraft = minecraft;
            this.loader = loader;
            this.loaderVersion = loaderVersion;
        }
    }

    private static int apply(Path dir, List<String[]> tasksIn, final Map<String, byte[]> local,
                             final List<Pattern> preserve, boolean inGame, String tag,
                             String etag, boolean partial) throws Exception {
        LinkedHashMap<String, String> desired = new LinkedHashMap<>();
        List<String[]> downloads = new ArrayList<>();
        List<String[]> pending = new ArrayList<>();

        for (String[] t : tasksIn) {
            String rel = safeRel(t[0]);
            if (rel == null) {
                consoleError("! skipped unsafe path: " + t[0]);
                continue;
            }
            t[0] = rel;
            if (protectedPath(t[0]) || desired.containsKey(t[0])) {
                continue;
            }
            desired.put(t[0], t[2]);
            downloads.add(t);
        }

        Map<String, String> state = loadState(dir);
        final Map<String, String> newState = partial
                ? new ConcurrentHashMap<>(state)
                : new ConcurrentHashMap<>();
        final Map<String, String> newStats = new ConcurrentHashMap<>(loadStats(dir));
        final int total = downloads.size();
        final AtomicInteger done = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();

        for (Map.Entry<String, String> e : partial
                ? Collections.<String, String>emptyMap().entrySet()
                : state.entrySet()) {
            String old = e.getKey();
            if (desired.containsKey(old)) {
                continue;
            }
            if (protectedPath(old) || safeRel(old) == null) {
                continue;
            }

            Path p = dir.resolve(old);
            String disk = diskSha(p);
            if (disk == null) {
                try {
                    Files.deleteIfExists(dir.resolve(old + ".disabled"));
                } catch (Exception ignore) {
                }
            }
            if (disk != null && matches(preserve, old) && !disk.equalsIgnoreCase(e.getValue())) {
                consoleLog("~ kept " + old + " (edited locally)");
                continue;
            }
            if (disk != null && inGame) {
                pending.add(new String[] { "del", old, null, null });
                newState.put(old, e.getValue());
                continue;
            }
            try {
                Files.deleteIfExists(p);
                newStats.remove(old);
                newState.remove(old);
            } catch (Exception ex) {
                pending.add(new String[] { "del", old, null, null });
                newState.put(old, e.getValue());
            }
        }

        final Path root = dir;
        final boolean game = inGame;
        final Map<String, String> prevState = state;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        final AtomicBoolean modsChanged = new AtomicBoolean();

        for (final String[] d : downloads) {
            futures.add(pool.submit(() -> {
                final String rel = d[0];
                final String src = d[1];
                final String sha1 = d[2];
                try {
                    Path out = root.resolve(rel);
                    String disk = diskSha(out);
                    String wrote = prevState.get(rel);

                    if (sha1 != null && !sha1.isEmpty() && sha1.equalsIgnoreCase(disk)) {
                        newState.put(rel, disk.toLowerCase());
                        recordStat(newStats, root, rel);
                        done.incrementAndGet();
                        return;
                    }
                    if (disk == null && Files.isRegularFile(root.resolve(rel + ".disabled"))) {
                        if (wrote != null) {
                            newState.put(rel, wrote);
                        }
                        done.incrementAndGet();
                        return;
                    }
                    if (disk != null && matches(preserve, rel) && (wrote == null || !wrote.equalsIgnoreCase(disk))) {
                        if (wrote != null) {
                            newState.put(rel, wrote);
                        }
                        consoleLog("~ kept " + rel + " (edited locally)");
                        done.incrementAndGet();
                        return;
                    }

                    byte[] bytes = local.get(rel);
                    if (bytes == null && src != null) {
                        bytes = getBytes(src);
                    }
                    if (bytes == null) {
                        throw new IOException("no data in release");
                    }

                    String got = sha1Of(bytes);
                    if (sha1 != null && !sha1.isEmpty() && !sha1.equalsIgnoreCase(got)) {
                        throw new IOException("checksum mismatch");
                    }

                    boolean stage = game && Files.exists(out);
                    if (rel.startsWith("mods/") && !rel.equals("packweaver.jar")) {
                        modsChanged.set(true);
                    }
                    if (!stage) {
                        try {
                            if (out.getParent() != null) {
                                Files.createDirectories(out.getParent());
                            }
                            Files.write(out, bytes);
                            newState.put(rel, got);
                            recordStat(newStats, root, rel);
                            int n = done.incrementAndGet();
                            consoleLog("[" + n + "/" + total + "] " + rel);
                            return;
                        } catch (IOException ignored) {
                        }
                    }

                    Path sf = stageFile(root, rel);
                    Files.createDirectories(sf.getParent());
                    Files.write(sf, bytes);
                    pending.add(new String[] { "put", rel, got, root.relativize(sf).toString().replace('\\', '/') });
                    if (wrote != null) {
                        newState.put(rel, wrote);
                    }
                    int n = done.incrementAndGet();
                    consoleLog("[" + n + "/" + total + "] " + rel + " (queued)");
                } catch (Exception e) {
                    failed.incrementAndGet();
                    consoleError("! " + rel + ": " + e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        boolean ok = failed.get() == 0;
        saveState(dir, newState, ok ? tag : null, ok ? etag : null, newStats);

        if (!pending.isEmpty()) {
            savePending(dir, pending);
            scheduleHelper(dir);
            consoleLog("packweave: " + pending.size() + (pending.size() == 1 ? " change finishes" : " changes finish") + " after the game closes");
        }

        if (failed.get() > 0) {
            consoleLog("Done with " + failed.get() + " failures, " + newState.size() + " files tracked in " + dir);
        } else {
            consoleLog("Up to date, " + newState.size() + " files in " + dir);
        }
        if (modsChanged.get()) {
            MODS_CHANGED.set(true);
        }
        return failed.get();
    }

    private static void validatePackSource(String baseUrl) throws IOException {
        if (baseUrl.startsWith(RELEASE_PREFIX)) {
            String webUrl = baseUrl.substring(RELEASE_PREFIX.length()).trim();
            String token = readToken(gameDir());
            String[] or = ownerRepo(webUrl);
            String repoApi = apiBase(webUrl) + "/repos/" + or[0] + "/" + or[1];
            ApiResp resp = apiGet(repoApi + "/releases/latest", token, null);
            if (resp.code == 404 || resp.body == null) {
                throw new IOException("No releases found for " + or[0] + "/" + or[1] + ".");
            }
            Map<String, Object> rel = asMap(Json.parse(resp.body));
            String tag = str(rel.get("tag_name"));
            if (tag == null || tag.isEmpty()) {
                throw new IllegalStateException("The file contains invalid data.");
            }
            return;
        }

        Map<String, Object> modpack = asMap(Json.parse(get(baseUrl + "/modpack.json")));
        Map<String, Object> index = asMap(Json.parse(get(baseUrl + "/index.json")));
        if (!index.containsKey("files")) {
            throw new IllegalStateException("The file contains invalid data.");
        }
        if (modpack.isEmpty()) {
            throw new IllegalStateException("The file contains invalid data.");
        }
    }

    private static String resolvePackLink(String packLink) {
        String normalized = normalizeLink(packLink);
        if (normalized != null && !normalized.isBlank()) {
            return normalized;
        }
        return getConfiguredLinkStatic();
    }

    private static String getConfiguredLinkStatic() {
        try {
            return readModpackUrl(gameDir());
        } catch (IOException e) {
            return null;
        }
    }

    private static String currentEnv() {
        return PackWizardCommon.INSTANCE.getCommonUtils().getEnvironment() == Environment.SERVER ? "server" : "client";
    }

    private static boolean inGame() {
        return PackWizardCommon.INSTANCE.getCommonUtils().getEnvironment() != Environment.SERVER;
    }

    private static Path gameDir() {
        return PackWizardCommon.INSTANCE.getGameDir().toPath();
    }

    private static String normalizeLink(String t) {
        if (t == null) {
            return null;
        }
        t = t.replace("\uFEFF", "").replace('"', ' ').replace('\'', ' ').trim();
        if (t.startsWith(RELEASE_PREFIX)) {
            t = RELEASE_PREFIX + t.substring(RELEASE_PREFIX.length()).trim();
        }
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                t = t.substring(0, i);
                break;
            }
        }
        return t.isEmpty() ? null : t;
    }

    private static String cleanUrl(String raw) {
        String url = raw.replace("\uFEFF", "").replace("\"", "").replace("'", "").trim();
        if (url.startsWith(RELEASE_PREFIX)) {
            url = RELEASE_PREFIX + url.substring(RELEASE_PREFIX.length()).trim();
        }
        for (int i = 0; i < url.length(); i++) {
            if (Character.isWhitespace(url.charAt(i))) {
                url = url.substring(0, i);
                break;
            }
        }
        url = url.replaceAll("/+$", "");
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith(RELEASE_PREFIX)) {
            return null;
        }
        return url;
    }

    private static String readModpackUrl(Path dir) throws IOException {
        Path file = dir.resolve(MODPACK_URL_FILE);
        if (!Files.exists(file)) {
            return null;
        }
        return normalizeLink(Files.readString(file));
    }

    private static void scheduleHelper(final Path dir) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                finishStage(dir, 200);
            } catch (Exception ignore) {
            }
        }));
    }

    private static String readToken(Path dir) {
        try {
            Path f = dir.resolve("modpack.token");
            if (Files.exists(f)) {
                return cleanToken(Files.readString(f));
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static String cleanToken(String t) {
        if (t == null) {
            return null;
        }
        t = t.replace("\uFEFF", "").replace("\"", "").replace("'", "").trim();
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                t = t.substring(0, i);
                break;
            }
        }
        return t.isEmpty() ? null : t;
    }

    private static String[] ownerRepo(String webUrl) throws IOException {
        List<String> segs = new ArrayList<>();
        try {
            for (String s : URI.create(webUrl).getPath().split("/")) {
                if (!s.isEmpty()) {
                    segs.add(s);
                }
            }
        } catch (Exception e) {
            throw new IOException("Release URL must look like https://github.com/you/pack");
        }
        if (segs.size() < 2) {
            throw new IOException("Release URL must look like https://github.com/you/pack");
        }
        String owner = segs.get(segs.size() - 2);
        String repo = segs.getLast();
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        return new String[] { owner, repo };
    }

    private static String apiBase(String webUrl) {
        URI u = URI.create(webUrl);
        String auth = u.getAuthority();
        if (auth.equals("github.com") || auth.equals("www.github.com")) {
            return "https://api.github.com";
        }
        return u.getScheme() + "://" + auth + "/api/v3";
    }

    private static final class ApiResp {
        private int code;
        private String body;
        private String etag;
    }

    private static ApiResp apiGet(String url, String token, String etag) throws IOException {
        HttpURLConnection c = getC(url, token, etag);

        ApiResp r = new ApiResp();
        r.code = c.getResponseCode();
        r.etag = c.getHeaderField("ETag");
        if (r.code / 100 == 2) {
            try (InputStream in = c.getInputStream()) {
                r.body = new String(readAll(in), StandardCharsets.UTF_8);
            }
        } else {
            InputStream err = c.getErrorStream();
            if (err != null) {
                try (err) {
                    readAll(err);
                }
            }
            if (r.code != 304 && r.code != 404) {
                throw new IOException("HTTP " + r.code + " for " + url);
            }
        }
        return r;
    }

    private static @NotNull HttpURLConnection getC(String url, String token, String etag) throws IOException {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        if (token != null && !token.isEmpty()) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (etag != null && !etag.isEmpty()) {
            c.setRequestProperty("If-None-Match", etag);
        }
        return c;
    }

    private static void savePending(Path dir, List<String[]> ops) throws IOException {
        StringBuilder sb = new StringBuilder("{\"ops\":[");
        boolean first = true;
        for (String[] op : ops) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"op\":").append(Json.quote(op[0]));
            sb.append(",\"path\":").append(Json.quote(op[1]));
            if (op[2] != null) {
                sb.append(",\"sha\":").append(Json.quote(op[2]));
            }
            if (op[3] != null) {
                sb.append(",\"stage\":").append(Json.quote(op[3]));
            }
            sb.append('}');
        }
        sb.append("]}\n");
        Path f = dir.resolve(WORK_DIR).resolve("pending.json");
        Files.createDirectories(f.getParent());
        atomicWrite(f, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void finishStage(Path dir, int rounds) throws Exception {
        Path pf = dir.resolve(WORK_DIR).resolve("pending.json");
        if (!Files.exists(pf)) {
            return;
        }
        List<String[]> ops = new ArrayList<>();
        Map<String, Object> parsed = asMap(Json.parse(Files.readString(pf)));
        for (Object oo : asList(parsed.get("ops"))) {
            Map<String, Object> o = asMap(oo);
            ops.add(new String[] { str(o.get("op")), str(o.get("path")), str(o.get("sha")), str(o.get("stage")) });
        }
        Map<String, String> state = loadState(dir);
        for (int r = 0; r < rounds && !ops.isEmpty(); r++) {
            if (r > 0) {
                Thread.sleep(300);
            }
            List<String[]> remaining = new ArrayList<>();
            for (String[] op : ops) {
                try {
                    if (safeRel(op[1]) == null) {
                        state.remove(op[1]);
                        continue;
                    }
                    Path target = dir.resolve(op[1]);
                    if ("del".equals(op[0])) {
                        Files.deleteIfExists(target);
                        state.remove(op[1]);
                    } else {
                        Path sf = op[3] == null || safeRel(op[3]) == null ? null : dir.resolve(op[3]);
                        if (sf == null || !Files.exists(sf)) {
                            continue;
                        }
                        if (target.getParent() != null) {
                            Files.createDirectories(target.getParent());
                        }
                        Files.move(sf, target, StandardCopyOption.REPLACE_EXISTING);
                        if (op[2] != null) {
                            state.put(op[1], op[2]);
                        }
                    }
                } catch (Exception e) {
                    remaining.add(op);
                }
            }
            ops = remaining;
        }
        saveState(dir, state, null, null, null);
        if (ops.isEmpty()) {
            Files.deleteIfExists(pf);
            Path stage = dir.resolve(WORK_DIR).resolve("stage");
            if (Files.isDirectory(stage)) {
                for (Path p : listFiles(stage)) {
                    Files.deleteIfExists(p);
                }
                Files.deleteIfExists(stage);
            }
        } else {
            savePending(dir, ops);
        }
    }

    private static List<Path> listFiles(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                out.add(p);
            }
        }
        return out;
    }

    private static List<Pattern> parsePreserve(String text) {
        List<Pattern> out = new ArrayList<>();
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            out.add(glob(t));
        }
        return out;
    }

    private static boolean matches(List<Pattern> preserve, String rel) {
        for (Pattern p : preserve) {
            if (p.matcher(rel).matches()) {
                return true;
            }
        }
        return false;
    }

    private static Pattern glob(String pat) {
        String p = pat.replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.endsWith("/")) {
            p = p + "**";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '*') {
                if (i + 1 < p.length() && p.charAt(i + 1) == '*') {
                    sb.append(".*");
                    i += 2;
                } else {
                    sb.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else {
                if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                    sb.append('\\');
                }
                sb.append(c);
                i++;
            }
        }
        return Pattern.compile(sb.toString());
    }

    private static boolean protectedPath(String rel) {
        return rel.equals(MODPACK_URL_FILE) || rel.equals("modpack.token") || rel.equals(STATE_FILE) || rel.startsWith(WORK_DIR + "/");
    }

    private static String safeRel(String rel) {
        if (rel == null) {
            return null;
        }
        String r = rel.replace('\\', '/').trim();
        while (r.startsWith("/")) {
            r = r.substring(1);
        }
        if (r.isEmpty() || r.indexOf(':') == 1 || r.contains("://")) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : r.split("/")) {
            String p = part.trim();
            if (p.isEmpty() || p.equals(".")) {
                continue;
            }
            if (p.equals("..")) {
                return null;
            }
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(p);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static Path stageFile(Path dir, String rel) throws Exception {
        return dir.resolve(WORK_DIR).resolve("stage").resolve(sha1Of(rel.getBytes(StandardCharsets.UTF_8)) + ".bin");
    }

    private static void recordStat(Map<String, String> stats, Path root, String rel) {
        try {
            Path p = root.resolve(rel);
            stats.put(rel, Files.size(p) + "," + Files.getLastModifiedTime(p).toMillis());
        } catch (Exception ignore) {
        }
    }

    private static Map<String, Object> readStateSnapshot(Path dir) {
        Path f = dir.resolve(STATE_FILE);
        if (!Files.exists(f)) {
            return null;
        }
        try {
            Map<String, Object> st = asMap(Json.parse(Files.readString(f)));
            if (!String.valueOf(STATE_VERSION).equals(str(st.get("version")))) {
                Files.deleteIfExists(f);
                return null;
            }
            return st;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Map<String, String> loadState(Path dir) {
        Map<String, String> out = new LinkedHashMap<>();
        Map<String, Object> st = readStateSnapshot(dir);
        if (st == null) {
            return out;
        }
        try {
            Object files = st.get("files");
            if (files instanceof Map) {
                for (Map.Entry<String, Object> e : asMap(files).entrySet()) {
                    String sha = str(e.getValue());
                    if (sha != null && !sha.isEmpty()) {
                        out.put(e.getKey(), sha.toLowerCase());
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    private static Map<String, String> loadStats(Path dir) {
        Map<String, String> out = new LinkedHashMap<>();
        Map<String, Object> st = readStateSnapshot(dir);
        if (st == null) {
            return out;
        }
        try {
            for (Map.Entry<String, Object> e : asMap(st.get("stat")).entrySet()) {
                String v = str(e.getValue());
                if (v != null && !v.isEmpty()) {
                    out.put(e.getKey(), v);
                }
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    private static String[] loadMeta(Path dir) {
        Map<String, Object> st = readStateSnapshot(dir);
        if (st == null) {
            return new String[] { null, null };
        }
        try {
            return new String[] { str(st.get("tag")), str(st.get("etag")) };
        } catch (Exception ignore) {
            return new String[] { null, null };
        }
    }

    private static void saveState(Path dir, Map<String, String> state, String tag, String etag, Map<String, String> stats) throws IOException {
        if (tag == null) {
            String[] m = loadMeta(dir);
            tag = m[0];
            etag = m[1];
        }
        if (stats == null) {
            stats = loadStats(dir);
        }
        List<String> keys = new ArrayList<>(state.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder("{\"version\":" + STATE_VERSION);
        if (tag != null) {
            sb.append(",\"tag\":").append(Json.quote(tag));
        }
        if (etag != null) {
            sb.append(",\"etag\":").append(Json.quote(etag));
        }
        sb.append(",\"files\":{");
        boolean first = true;
        for (String k : keys) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(Json.quote(k)).append(':').append(Json.quote(state.get(k)));
        }
        sb.append("},\"stat\":{");
        first = true;
        for (String k : keys) {
            String v = stats.get(k);
            if (v == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(Json.quote(k)).append(':').append(Json.quote(v));
        }
        sb.append("}}\n");
        atomicWrite(dir.resolve(STATE_FILE), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        if (tmp.getParent() != null) {
            Files.createDirectories(tmp.getParent());
        }
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String diskSha(Path p) {
        try {
            if (!Files.isRegularFile(p)) {
                return null;
            }
            return sha1Of(Files.readAllBytes(p));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] getBytes(String url) throws IOException {
        return fetchBytes(url, null, null);
    }

    private static Map<String, byte[]> unzip(byte[] data) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                out.put(e.getName(), readAll(z));
            }
        }
        return out;
    }

    private static String get(String url) throws IOException {
        return new String(getBytes(url), StandardCharsets.UTF_8);
    }

    private static byte[] fetchBytes(String url, String token, String accept) throws IOException {
        String current = url;
        String authHost;
        try {
            authHost = URI.create(url).getAuthority();
        } catch (Exception e) {
            authHost = "";
        }
        for (int i = 0; i < 5; i++) {
            HttpURLConnection c = (HttpURLConnection) URI.create(current).toURL().openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);
            c.setRequestProperty("User-Agent", UA);
            if (accept != null) {
                c.setRequestProperty("Accept", accept);
            }
            if (token != null && !token.isEmpty() && URI.create(current).getAuthority().equals(authHost)) {
                c.setRequestProperty("Authorization", "Bearer " + token);
            }
            int code = c.getResponseCode();
            if (code / 100 == 3) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) {
                    throw new IOException("HTTP " + code + " for " + current);
                }
                current = URI.create(current).resolve(loc).toString();
                continue;
            }
            if (code / 100 != 2) {
                c.disconnect();
                throw new IOException("HTTP " + code + " for " + current);
            }
            try (InputStream in = c.getInputStream()) {
                return readAll(in);
            }
        }
        throw new IOException("Too many redirects for " + url);
    }

    private static String sha1Of(byte[] b) throws IOException {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : h) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return bo.toByteArray();
    }

    private static String enc(String path) {
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static final class SyncInput {
        private final List<String[]> tasks = new ArrayList<>();
        private final Map<String, byte[]> local = new HashMap<>();
        private List<Pattern> preserve = new ArrayList<>();
        private String rules = "";
    }

    private static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        private static Object parse(String s) {
            Json j = new Json(s);
            return j.val();
        }

        private Object val() {
            ws();
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> obj();
                case '[' -> arr();
                case '"' -> strv();
                case 't' -> {
                    i += 4;
                    yield Boolean.TRUE;
                }
                case 'f' -> {
                    i += 5;
                    yield Boolean.FALSE;
                }
                case 'n' -> {
                    i += 4;
                    yield null;
                }
                default -> num();
            };
        }

        private Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String k = strv();
                ws();
                i++;
                Object v = val();
                m.put(k, v);
                ws();
                char c = s.charAt(i++);
                if (c == '}') { break; }
            }
            return m;
        }

        private List<Object> arr() {
            List<Object> a = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return a; }
            while (true) {
                a.add(val());
                ws();
                char c = s.charAt(i++);
                if (c == ']') { break; }
            }
            return a;
        }

        private String strv() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') { break; }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object num() {
            int st = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9')) {
                    i++;
                } else {
                    break;
                }
            }
            String n = s.substring(st, i);
            try {
                if (n.contains(".") || n.contains("e") || n.contains("E")) {
                    return Double.parseDouble(n);
                }
                return Long.parseLong(n);
            } catch (Exception e) {
                return n;
            }
        }

        private void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                    i++;
                } else {
                    break;
                }
            }
        }

        private static String quote(String v) {
            StringBuilder sb = new StringBuilder("\"");
            for (int k = 0; k < v.length(); k++) {
                char c = v.charAt(k);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default: sb.append(c);
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
}
