package searchengine.utils.parsers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.DtoLemma;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.utils.morphology.Morphology;
import searchengine.repositories.PageRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class LemmaParserImpl implements LemmaParser
{
    private final PageRepository pageRepository;
    private final Morphology morphology;
    @Getter
    private List<DtoLemma> DtoLemmaList;

    @Override
    public void run(SiteEntity siteEntity) {
        DtoLemmaList = new CopyOnWriteArrayList<>();
        Iterable<PageEntity> pageList = pageRepository.findAll();
        TreeMap<String, Integer> lemmaMap = new TreeMap<>();
        for (PageEntity page : pageList) {
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
        }
        for (String lemma : lemmaMap.keySet()) {
            Integer frequency = lemmaMap.get(lemma);
            DtoLemmaList.add(new DtoLemma(lemma, frequency));
        }
    }
}
