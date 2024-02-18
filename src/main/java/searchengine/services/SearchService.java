package searchengine.services;

import searchengine.dto.statistics.SearchResult;

public interface SearchService
{
    SearchResult getSearchResult(String text, String url, int offset, int limit);

}
