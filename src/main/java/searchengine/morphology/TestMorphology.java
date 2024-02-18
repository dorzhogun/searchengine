package searchengine.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
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
                luceneMorph.getNormalForms("первая");
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

        // the same test for english words / text
        LuceneMorphology enMorphology = new EnglishLuceneMorphology();
        List<String> enWordBaseForms =
                enMorphology.getNormalForms("issue");
        enWordBaseForms.forEach(System.out::println);
        List<String> enWordMorphInfo = enMorphology.getMorphInfo("love");
        enWordMorphInfo.forEach(System.out::println);


        String enText = "Marry loves capitals. London is a capital of Great Britain.";
        HashMap<String, Integer> map2en = analyzer.getLemmaMap(enText);
        map2en.forEach((key,value) -> System.out.println(key + " - " + value));

        // get morphology information for each word to find service words in the text
        List<String> wordsList2 = map2en.keySet().stream().toList();
        for (String word : wordsList2) {
            List<String> morphInfo2 = enMorphology.getMorphInfo(word);
            morphInfo2.forEach(System.out::println);
        }
    }

}
