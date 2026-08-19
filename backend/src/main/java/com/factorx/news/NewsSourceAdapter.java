package com.factorx.news;

import java.time.Instant;
import java.util.List;

public interface NewsSourceAdapter {

    String sourceCode();

    List<RawNewsItem> fetch(Instant since);
}
