package dev.youtubedisks.youtube;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HttpDownloader extends Downloader {

    private static final Set<String> RESTRICTED_HEADERS = Set.of(
        "host", "connection", "content-length", "upgrade"
    );

    // YouTube returns "Page needs to be reloaded" when it suspects a bot; sending a realistic
    // browser User-Agent + Accept-Language sidesteps the cheapest layer of that detection.
    private static final String DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String DEFAULT_ACCEPT_LANGUAGE = "en-US,en;q=0.9";

    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(request.url()))
            .timeout(Duration.ofSeconds(60));

        byte[] body = request.dataToSend();
        if (body != null && body.length > 0) {
            builder.method(request.httpMethod(), HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            builder.method(request.httpMethod(), HttpRequest.BodyPublishers.noBody());
        }

        boolean hasUserAgent = false;
        boolean hasAcceptLanguage = false;
        for (Map.Entry<String, List<String>> e : request.headers().entrySet()) {
            String name = e.getKey();
            String lower = name.toLowerCase(Locale.ROOT);
            if (RESTRICTED_HEADERS.contains(lower)) {
                continue;
            }
            if (lower.equals("user-agent")) hasUserAgent = true;
            if (lower.equals("accept-language")) hasAcceptLanguage = true;
            for (String value : e.getValue()) {
                builder.header(name, value);
            }
        }
        if (!hasUserAgent) builder.header("User-Agent", DEFAULT_USER_AGENT);
        if (!hasAcceptLanguage) builder.header("Accept-Language", DEFAULT_ACCEPT_LANGUAGE);

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new ReCaptchaException("YouTube returned 429 — rate limited / reCaptcha", request.url());
            }
            return new Response(
                response.statusCode(),
                Integer.toString(response.statusCode()),
                response.headers().map(),
                response.body(),
                response.uri().toString()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", ex);
        }
    }
}
