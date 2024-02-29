package searchengine.utils.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.DtoIndex;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.utils.morphology.Morphology;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexParserImpl implements IndexParser {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final Morphology morphology;
    private List<DtoIndex> DtoIndexList;

    @Override
    public List<DtoIndex> getDtoIndexList() {
        return DtoIndexList;
    }

    @Override
    public void run(SiteEntity siteEntity) {
        Iterable<PageEntity> pageList = pageRepository.findBySiteEntity(siteEntity);
        List<LemmaEntity> lemmaList = lemmaRepository.findBySiteEntity(siteEntity);
        DtoIndexList = new ArrayList<>();

        for (PageEntity page : pageList) {
            if (page.getCode() >= 400) {
                log.debug("Status code - " + page.getCode());
                continue;
            }

            long pageId = page.getId();
            String content = page.getContent();
            String title = HtmlCodeCleaner.clear(content, "title");
            String body = HtmlCodeCleaner.clear(content, "body");
            HashMap<String, Integer> titleList = morphology.getLemmaMap(title);
            HashMap<String, Integer> bodyList = morphology.getLemmaMap(body);

            for (LemmaEntity lemma : lemmaList) {
                long lemmaId = lemma.getId();
                String theExactLemma = lemma.getLemma();
                if (!titleList.containsKey(theExactLemma) && !bodyList.containsKey(theExactLemma)) {
                    log.debug("Lemma not found");
                    continue;
                }

                float wholeRank = 0.0F;
                if (titleList.get(theExactLemma) != null) {
                    Float titleRank = Float.valueOf(titleList.get(theExactLemma));
                    wholeRank += titleRank;
                }
                if (bodyList.get(theExactLemma) != null) {
                    float bodyRank = (float) (bodyList.get(theExactLemma) * 0.8);
                    wholeRank += bodyRank;
                }
                DtoIndexList.add(new DtoIndex(pageId, lemmaId, wholeRank));
            }
        }
    }
}
