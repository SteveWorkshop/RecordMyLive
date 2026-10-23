package rachel.materialapps.recordmylive.logic.importer;

import java.net.URI;

public interface ConcertLinkParser {
    boolean supports(URI uri);
    /** Blocking operation; callers must use a worker thread. */
    ConcertImportResult parse(URI uri) throws Exception;
}
