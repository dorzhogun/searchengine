package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.model.IndexSearch;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.morphology.Morphology;
import searchengine.repositories.IndexSearchRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.parsers.HtmlCodeCleaner;

import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService
{
    private final Morphology morphology;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final SiteRepository siteRepository;

    @Override
    public List<StatisticsSearch> allSitesSearch(String searchText, int offset, int limit) {
        log.info("Getting results of the search from all sites \"" + searchText + "\"");
        List<SiteEntity> siteList = siteRepository.findAll();
        List<StatisticsSearch> result = new ArrayList<>();
        List<LemmaEntity> lemmaListResult = new ArrayList<>();
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        for (SiteEntity site : siteList) {
            lemmaListResult.addAll(getLemmaEntityListFromSite(textLemmaList, site));
        }
        List<StatisticsSearch> statisticsSearchList = null;
        for (String searchLemma : textLemmaList) {
            for (LemmaEntity l : lemmaListResult) {
                if (l.getLemma().equals(searchLemma)) {
                    statisticsSearchList = new ArrayList<>(getSearchDtoList(lemmaListResult, textLemmaList, offset, limit));
                    statisticsSearchList.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
                    if (statisticsSearchList.size() > limit) {
                        for (int i = offset; i < limit; i++) {
                            result.add(statisticsSearchList.get(i));
                        }
                        return result;
                    }
                }
            }
        }
        log.info("Search done. Got results.");
        return statisticsSearchList;
    }

    @Override
    public List<StatisticsSearch> siteSearch(String searchText, String url, int offset, int limit) {
        log.info("Searching for \"" + searchText + "\" in - " + url);
        SiteEntity site = siteRepository.findByUrl(url);
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<LemmaEntity> foundLemmaList = getLemmaEntityListFromSite(textLemmaList, site);
        log.info("Search done. Got results.");
        return getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
    }

    private List<String> getLemmaFromSearchText(String searchText) {
        String[] words = searchText.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String lemma : words) {
            List<String> list = morphology.getLemmaList(lemma);
            lemmaList.addAll(list);
        }
        return lemmaList;
    }

    private List<LemmaEntity> getLemmaEntityListFromSite(List<String> lemmas, SiteEntity site) {
        lemmaRepository.flush();
        List<LemmaEntity> lemmaList = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<LemmaEntity> result = new ArrayList<>(lemmaList);
        result.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return result;
    }

    private List<StatisticsSearch> getStatisticsSearchList(Hashtable<PageEntity,
            Float> pageList, List<String> textLemmaList) {
        List<StatisticsSearch> result = new ArrayList<>();

        for (PageEntity page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            SiteEntity pageSite = page.getSiteEntity();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = HtmlCodeCleaner.clear(content, "title");
            String body = HtmlCodeCleaner.clear(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), textLemmaList);

            result.add(new StatisticsSearch(site, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, List<String> lemmaList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmaList) {
            lemmaIndex.addAll(morphology.getLemmaIndexListInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size()
                    && lemmaIndex.get(nextPoint) - end > 0
                    && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private List<StatisticsSearch> getSearchDtoList(List<LemmaEntity> lemmaList,
                                                    List<String> textLemmaList, int offset, int limit) {
        List<StatisticsSearch> result = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<PageEntity> foundPageList = pageRepository.findByLemmaList(lemmaList);
            indexSearchRepository.flush();
            List<IndexSearch> foundIndexList = indexSearchRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Hashtable<PageEntity, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<StatisticsSearch> dataList = getStatisticsSearchList(sortedPageByAbsRelevance, textLemmaList);

            if (offset > dataList.size()) {
                return new ArrayList<>();
            }

            if (dataList.size() > limit) {
                for (int i = offset; i < limit; i++) {
                    result.add(dataList.get(i));
                }
                return result;
            } else return dataList;
        } else return result;
    }

    private Hashtable<PageEntity, Float> getPageAbsRelevance(List<PageEntity> pageList,
                                                             List<IndexSearch> indexList) {
        HashMap<PageEntity, Float> pageWithRelevance = new HashMap<>();
        for (PageEntity page : pageList) {
            float relevant = 0;
            for (IndexSearch index : indexList) {
                if (index.getPageEntity() == page) {
                    relevant += index.getRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<PageEntity, Float> pageWithAbsRelevance = new HashMap<>();
        for (PageEntity page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, absRelevant);
        }
        return pageWithAbsRelevance.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }
}
