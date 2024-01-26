package searchengine.utils.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.dto.statistics.DtoIndex;
import searchengine.model.*;
import searchengine.morphology.Morphology;
import searchengine.repositories.IndexSearchRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Slf4j
public class SinglePageParser implements Runnable {

    private final String url;
    private final SiteEntity sitePage;
    private final String path;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final Morphology morphology;

    @Override
    public void run() {
        saveNewPageEntity();
        getPageLemmas();
        try {
            indexParsing();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveNewPageEntity() {
        String content = getDocument(url).outerHtml();
        int code = getResponse(url).statusCode();
        PageEntity page = new PageEntity();
        page.setSiteEntity(sitePage);
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        pageRepository.saveAndFlush(page);
        log.info("New page - " + page.getPath() + " saved successfully!");
    }


    private Document getDocument(String url) {
        try {
            return Jsoup.connect(url).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Connection.Response getResponse(String url) {
        try {
            return Jsoup.connect(url).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).execute();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private void getPageLemmas() {
        PageEntity page = pageRepository.findPageByPath(path);
        TreeMap<String, Integer> lemmaMap = new TreeMap<>();
        String content = page.getContent();
        String title = HtmlCodeCleaner.clear(content, "title");
        String body = HtmlCodeCleaner.clear(content, "body");
        HashMap<String, Integer> textTitle = morphology.getLemmaMap(title);
        HashMap<String, Integer> textBody = morphology.getLemmaMap(body);
        Set<String> allTheWords = new HashSet<>();
        allTheWords.addAll(textTitle.keySet());
        allTheWords.addAll(textBody.keySet());
        for (String word : allTheWords) {
            int frequency = lemmaMap.getOrDefault(word, 0) + 1;
            lemmaMap.put(word, frequency);
        }
        List<LemmaEntity> lemmaList = new CopyOnWriteArrayList<>();
        lemmaMap.forEach((key, value) -> lemmaList.add(new LemmaEntity(key, value, sitePage)));
        lemmaRepository.flush();
        lemmaRepository.saveAll(lemmaList);
    }

    private void indexParsing() throws InterruptedException {
        if (!Thread.interrupted()) {
            PageEntity page = pageRepository.findPageByPath(path);
            List<LemmaEntity> lemmaList = lemmaRepository.findBySiteEntity(sitePage);
            List<DtoIndex> dtoIndexList = new ArrayList<>();
            long pageId = page.getId();
            String content = page.getContent();
            String title = HtmlCodeCleaner.clear(content, "title");
            String body = HtmlCodeCleaner.clear(content, "body");
            HashMap<String, Integer> titleList = morphology.getLemmaMap(title);
            HashMap<String, Integer> bodyList = morphology.getLemmaMap(body);

            for (LemmaEntity lemma : lemmaList) {
                long lemmaId = lemma.getId();
                String theExactLemma = lemma.getLemma();
                if (titleList.containsKey(theExactLemma) || bodyList.containsKey(theExactLemma)) {
                    float wholeRank = 0.0F;
                    if (titleList.get(theExactLemma) != null) {
                        Float titleRank = Float.valueOf(titleList.get(theExactLemma));
                        wholeRank += titleRank;
                    }
                    if (bodyList.get(theExactLemma) != null) {
                        float bodyRank = (float) (bodyList.get(theExactLemma) * 0.8);
                        wholeRank += bodyRank;
                    }
                    dtoIndexList.add(new DtoIndex(pageId, lemmaId, wholeRank));
                } else {
                    log.debug("Lemma not found");
                }
            }

            List<DtoIndex> dtoIndexList2 = new CopyOnWriteArrayList<>(dtoIndexList);
            List<IndexSearch> indexList = new CopyOnWriteArrayList<>();
            for (DtoIndex dtoIndex : dtoIndexList2) {
                PageEntity page2 = pageRepository.getReferenceById(dtoIndex.getPageID());
                LemmaEntity lemma = lemmaRepository.getReferenceById(dtoIndex.getLemmaID());
                indexList.add(new IndexSearch(page2, lemma, dtoIndex.getRank()));
            }
            indexSearchRepository.saveAllAndFlush(indexList);
            log.info("Done single page indexing - " + url);
            sitePage.setStatusTime(LocalDateTime.now());
            siteRepository.saveAndFlush(sitePage);
        } else {
            throw new InterruptedException();
        }
    }
}
