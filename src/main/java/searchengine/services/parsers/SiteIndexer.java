package searchengine.services.parsers;

import lombok.extern.slf4j.Slf4j;
import searchengine.dto.statistics.DtoPage;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
@Slf4j

public class SiteIndexer implements Runnable
{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final String url;

    public SiteIndexer(SiteRepository siteRepository, PageRepository pageRepository, String url) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.url = url;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) {
            handleInterrupt();
            return;
        }
            try {
                List<DtoPage> pages = getDtoPages();
                savePagesIntoDb(pages);
            } catch (InterruptedException | IOException e) {
                handleException();
                stopSiteIndexing();
        }
    }

    private List<DtoPage> getDtoPages() throws InterruptedException, IOException {
        String urlToParse = url + "/";
        List<DtoPage> dtoPagesVector = new Vector<>();
        List<String> linksList = new Vector<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        List<DtoPage> dtoPages = forkJoinPool.invoke(new RecursiveParser(urlToParse, dtoPagesVector, linksList));
        return new CopyOnWriteArrayList<>(dtoPages);
    }

    public void savePagesIntoDb(List<DtoPage> pages) throws InterruptedException {
        if (!Thread.interrupted()) {
            List<PageEntity> pageList = new CopyOnWriteArrayList<>();
            SiteEntity site = siteRepository.findByUrl(url);
            for (DtoPage dp : pages) {
                int first = dp.getPath().indexOf(url) + url.length();
                String format = dp.getPath().substring(first);
                String content = dp.getContent();
                int code = dp.getCode();
                PageEntity page = new PageEntity();
                page.setSiteEntity(site);
                page.setCode(code);
                page.setPath(format);
                page.setContent(content);
                pageList.add(page);
            }
            pageRepository.saveAllAndFlush(pageList);
            finishIndexing();
        } else {
            log.error("The saving process was interrupted");
            throw new InterruptedException();
        }
    }

    private void finishIndexing() {
        log.info("Finish indexing");
        SiteEntity site = siteRepository.findByUrl(url);
        site.setStatusTime(LocalDateTime.now());
        site.setStatus(Status.INDEXED);
        siteRepository.saveAndFlush(site);
    }

    private void stopSiteIndexing() {
        log.info("User stopped indexing for site : " + url);
        SiteEntity site = siteRepository.findByUrl(url);
        site.setLastError("Индексация остановлена пользователем");
        site.setStatus(Status.FAILED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
    }

    private void handleInterrupt() {
        String intMessage = "Indexing was interrupted for " + url;
        log.error(intMessage);
        throw new RuntimeException(new InterruptedException(intMessage));
    }

    private void handleException() {
        String excMessage = "Indexing was stopped for : " + url;
        log.error(excMessage);
    }

}
