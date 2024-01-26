package searchengine.morphology;

import java.util.HashMap;
import java.util.List;

public interface Morphology
{
    HashMap<String, Integer> getLemmaMap(String content);
    List<String> getLemmaList(String word);
    List<Integer> getLemmaIndexListInText(String content, String lemma);
}
