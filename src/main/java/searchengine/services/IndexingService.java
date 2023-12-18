package searchengine.services;

public interface IndexingService {
    boolean indexingAll();
    boolean indexingUrl();
    boolean stopIndexing();
}
