package searchengine.utils.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.dto.statistics.DtoIndex;
import searchengine.dto.statistics.DtoLemma;
import searchengine.dto.statistics.DtoPage;
import searchengine.model.*;
import searchengine.repositories.IndexSearchRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
public class SiteIndexer implements Runnable
{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final String url;

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) {
            handleInterrupt();
            return;
        }
        try {
            savePagesIntoDb(getDtoPages());
            getPageLemmas();
            indexParsing();
        } catch (InterruptedException | IOException e) {
            handleException();
            stopSiteIndexing();
        }
    }

    private List<DtoPage> getDtoPages() throws InterruptedException, IOException {
        String urlToParse = url + "/";
        List<DtoPage> dtoPagesVector = new Vector<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
        List<DtoPage> dtoPages = forkJoinPool.invoke(new RecursiveParser(urlToParse, dtoPagesVector, urlToParse));
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

    private void getPageLemmas() {
        if (!Thread.interrupted()) {
            SiteEntity siteEntity = siteRepository.findByUrl(url);
            siteEntity.setStatusTime(LocalDateTime.now());
            lemmaParser.run(siteEntity);
            List<DtoLemma> DtoLemmaList = lemmaParser.getDtoLemmaList();
            List<LemmaEntity> lemmaList = new CopyOnWriteArrayList<>();
            for (DtoLemma dtoLemma : DtoLemmaList) {
                lemmaList.add(new LemmaEntity(dtoLemma.getLemma(), dtoLemma.getFrequency(), siteEntity));
            }
            lemmaRepository.flush();
            lemmaRepository.saveAll(lemmaList);
        } else {
            throw new RuntimeException();
        }
    }

    private void indexParsing() throws InterruptedException {
        if (!Thread.interrupted()) {
            SiteEntity siteEntity = siteRepository.findByUrl(url);
            indexParser.run(siteEntity);
            List<DtoIndex> dtoIndexList = new CopyOnWriteArrayList<>(indexParser.getDtoIndexList());
            List<IndexSearch> indexList = new CopyOnWriteArrayList<>();
            siteEntity.setStatusTime(LocalDateTime.now());
            for (DtoIndex dtoIndex : dtoIndexList) {
                PageEntity page = pageRepository.getReferenceById(dtoIndex.getPageID());
                LemmaEntity lemma = lemmaRepository.getReferenceById(dtoIndex.getLemmaID());
                indexList.add(new IndexSearch(page, lemma, dtoIndex.getRank()));
            }
            indexSearchRepository.flush();
            indexSearchRepository.saveAll(indexList);
            log.info("Done indexing - " + url);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setStatus(Status.INDEXED);
            siteRepository.save(siteEntity);
        } else {
            throw new InterruptedException();
        }
    }
}
