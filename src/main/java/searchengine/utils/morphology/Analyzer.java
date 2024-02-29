package searchengine.utils.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class Analyzer implements Morphology
{
    private static EnglishLuceneMorphology englishLuceneMorphology;
    private static RussianLuceneMorphology russianLuceneMorphology;
    private final static String REGEX = "\\p{Punct}|[0-9]|№|©|◄|«|»|—|-|@|…";
    private final static Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");
    private final static Logger LOGGER = LogManager.getLogger(LuceneMorphology.class);


    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
            englishLuceneMorphology = new EnglishLuceneMorphology();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private String checkLanguage(String word) {
        String rusAlphabet = "[а-яА-Я]+";
        String enAlphabet = "[a-zA-Z]+";
        if (word.matches(rusAlphabet) && russianLuceneMorphology.checkString(word)) {
            return "RUS";
        } else if (word.matches(enAlphabet) && englishLuceneMorphology.checkString(word)) {
            return "ENG";
        } else {
            return "";
        }
    }

    @Override
    public HashMap<String, Integer> getLemmaMap(String content) {
        content = content.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX, " ");
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\s+");
        for (String el : elements) {
            List<String> wordsList = getLemmaList(el);
            for (String word : wordsList) {
                int count = lemmaMap.getOrDefault(word, 0);
                lemmaMap.put(word, count + 1);
            }
        }
        return lemmaMap;
    }

    @Override
    public List<String> getLemmaList(String word) {
        List<String> lemmaList = new ArrayList<>();
        try {
            if (checkLanguage(word).equals("RUS") && !isRusServiceWord(word)) {
                List<String> baseRusForm = russianLuceneMorphology.getNormalForms(word);
                lemmaList.addAll(baseRusForm);
            } else if (checkLanguage(word).equals("ENG") && !isEngServiceWord(word)) {
                List<String> baseEngForm = englishLuceneMorphology.getNormalForms(word);
                lemmaList.addAll(baseEngForm);
                }
        } catch (Exception e) {
            LOGGER.debug(INVALID_SYMBOL_MARKER, "Символ не найден - " + word);
        }
        return lemmaList;
    }

    @Override
    public List<Integer> getLemmaIndexListInText(String content, String lemma) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String el : elements) {
            List<String> lemmas = getLemmaList(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }

    private boolean isRusServiceWord(String word) {
        List<String> rusMorphologyForms = russianLuceneMorphology.getMorphInfo(word);
        for (String form : rusMorphologyForms) {
            if (form.contains("ПРЕДЛ")
                    || form.contains("СОЮЗ")
                    || form.contains("МЕЖД")
                    || form.contains("МС")
                    || form.contains("ЧАСТ")
                    || form.length() <= 3) {
                return true;
            }
        }
        return false;
    }
    private boolean isEngServiceWord(String word) {
        List<String> enMorphologyForms = englishLuceneMorphology.getMorphInfo(word);
        for (String form : enMorphologyForms) {
            if (form.contains("PREP")
                    || form.contains("CONJ")
                    || form.contains("INTER")
                    || form.contains("ARTICLE")
                    || form.length() <= 2) {
                return true;
            }
        }
        return false;
    }

}
