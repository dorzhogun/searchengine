package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
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
    private final SitesList sitesList;
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
                    log.info("Removed from DB data about : " + site.getUrl());
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
                executorService.submit(new SiteIndexer(siteRepository, pageRepository, site.getUrl()));
                log.info("Start parsing the site " + siteEntity.getUrl());
            }
            executorService.shutdown();
        }
        return true;
    }

    @Override
    public boolean indexingUrl() {
        return false;
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
}


