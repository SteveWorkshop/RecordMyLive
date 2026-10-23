package rachel.materialapps.recordmylive.logic.importer;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConcertLinkImportService {
    private final List<ConcertLinkParser> parsers;

    public ConcertLinkImportService(List<ConcertLinkParser> parsers) {
        this.parsers = parsers;
    }

    public ConcertImportResult parse(String text) throws Exception {
        Matcher matcher = Pattern.compile("https?://[^\\s<>\"\\u3000【】]+", Pattern.CASE_INSENSITIVE)
                .matcher(text == null ? "" : text);
        while (matcher.find()) {
            String link = matcher.group().replace("\\&", "&");
            link = link.replaceAll("[)）\\]】。，,；;]+$", "");
            URI uri;
            try { uri = URI.create(link); } catch (IllegalArgumentException ignored) { continue; }
            for (ConcertLinkParser parser : parsers) {
                if (parser.supports(uri)) return parser.parse(uri);
            }
        }
        throw new IllegalArgumentException("请粘贴大麦或秀动的演出详情链接或分享文案，暂不支持短链接");
    }
}
