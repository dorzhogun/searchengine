package searchengine.services;

import searchengine.dto.statistics.SearchResult;
import searchengine.dto.statistics.StatisticsSearch;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface SearchService
{
    SearchResult getSearchResult(String text, String url, int offset, int limit);

}
