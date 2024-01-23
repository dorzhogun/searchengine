package searchengine.services;

public interface IndexingService {
    boolean indexingAll();
    boolean urlIndexing(String url);
    boolean stopIndexing();
}
