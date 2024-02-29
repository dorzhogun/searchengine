package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.utils.morphology.Morphology;
import searchengine.repositories.IndexSearchRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.parsers.IndexParser;
import searchengine.utils.parsers.LemmaParser;
import searchengine.utils.parsers.SinglePageParser;
import searchengine.utils.parsers.SiteIndexer;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final SitesList sitesList;
    private final Morphology morphology;
    private ExecutorService executorService;

    @Override
    public boolean indexingAll() {
        if (isIndexing()) {
            log.debug("Indexing is in process!");
            return false;
        } else {
            executorService = Executors.newFixedThreadPool(2);
            List<Site> siteList = sitesList.getSites();
            for (Site site : siteList) {
                if (siteRepository.findByUrl(site.getUrl()) != null) {
                    log.info("Removed from DB : " + site.getUrl());
                    siteRepository.deleteByUrl(site.getUrl());
                }
                SiteEntity siteEntity = new SiteEntity();
                siteEntity.setName(site.getName());
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setUrl(site.getUrl());
                siteEntity.setLastError("");
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);
                siteRepository.flush();
                executorService.submit(new SiteIndexer(siteRepository,
                        pageRepository, lemmaRepository, indexSearchRepository,
                        lemmaParser, indexParser, site.getUrl()));
                log.info("Start parsing the site " + siteEntity.getUrl());
            }
            executorService.shutdown();
        }
        return true;
    }

    @Override
    public boolean urlIndexing(String url) {
        int first = url.indexOf("/", url.indexOf("/") + 2);
        String path = first != -1 ? url.substring(first) : "";
        if (!path.isEmpty() && urlCheck(url) && !(pageRepository.findPageByPath(path) == null)) {
            log.info("Start indexing for single page : " + path);
            PageEntity page = pageRepository.findPageByPath(path);
            SiteEntity sitePage = page.getSiteEntity();
            pageRepository.deleteById(page.getId());
            pageRepository.flush();
            log.info("Page with id : " + page.getId() + " - was deleted successfully!");
            executorService = Executors.newFixedThreadPool(2);
            executorService.submit(new SinglePageParser(url, sitePage, path, siteRepository,
                    pageRepository, lemmaRepository, indexSearchRepository, morphology));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexing()) {
            log.info("Indexing is stopped by User");
            executorService.shutdownNow();
            List<SiteEntity> sites = siteRepository.findAllByStatus(Status.INDEXING);
            sites.forEach(s -> {
                s.setStatus(Status.FAILED);
                s.setStatusTime(LocalDateTime.now());
                s.setLastError("Индексация остановлена пользователем");
                siteRepository.saveAndFlush(s);
            });
            return true;
        } else {
            log.info("Indexing can't be stopped cause it wasn't started");
            return false;
        }
    }

    private boolean isIndexing() {
        siteRepository.flush();
        Iterable<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private boolean urlCheck(String url) {
        List<Site> urlList = sitesList.getSites();
        for (Site site : urlList) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

}


