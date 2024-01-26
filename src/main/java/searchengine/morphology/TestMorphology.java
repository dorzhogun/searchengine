package searchengine.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


public class TestMorphology
{
    public static void main(String[] args) throws IOException
    {
        // get normal form of the word
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("первый");
        wordBaseForms.forEach(System.out::println);

        // clean, split text and get lemma list with its quantities in text
        Analyzer analyzer = new Analyzer();
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        HashMap<String, Integer> map = analyzer.getLemmaMap(text);
        map.forEach((key,value) -> System.out.println(key + " - " + value));

        // get morphology information for each word to find service words in the text
        List<String> wordsList = map.keySet().stream().toList();
        for (String word : wordsList) {
            List<String> morphInfo = luceneMorph.getMorphInfo(word);
            morphInfo.forEach(System.out::println);
        }

    }

}
