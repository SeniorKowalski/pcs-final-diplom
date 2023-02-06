import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BooleanSearchEngine implements SearchEngine {
    private final Map<String, List<PageEntry>> allWordsEntriesMap = new HashMap<>();
    private final File stopWords = new File("stop-ru.txt");
    private final Set<String> stopWordsList = new HashSet<>();

    public BooleanSearchEngine(File pdfsDir) throws IOException {
        File dir = new File(String.valueOf(pdfsDir));
        File[] filesInDir = dir.listFiles();
        if (filesInDir != null) {
            for (File file : filesInDir) {
                File pdf = new File(String.valueOf(new File(file.getPath())));
                var doc = new PdfDocument(new PdfReader(pdf));
                for (int i = 1; i < doc.getNumberOfPages(); i++) {
                    var textFromPage = PdfTextExtractor.getTextFromPage(doc.getPage(i));
                    var allWordsOnPage = textFromPage.split("\\P{IsAlphabetic}+");
                    Map<String, Integer> frequencyMap = new HashMap<>();
                    getStopList();
                    for (var word : allWordsOnPage) {
                        if (word.isEmpty() || stopWordsList.contains(word.toLowerCase(Locale.ROOT))) {
                            continue;
                        }
                        word = word.toLowerCase();
                        frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
                    }
                    for (String wordToSearch : frequencyMap.keySet()) {
                        List<PageEntry> entries = allWordsEntriesMap.get(wordToSearch);
                        if (allWordsEntriesMap.get(wordToSearch) == null) {
                            entries = new ArrayList<>();
                            entries.add(new PageEntry(pdf.getName(), i, frequencyMap.get(wordToSearch)));
                            allWordsEntriesMap.put(wordToSearch, entries);
                        } else {
                            entries.add(new PageEntry(pdf.getName(), i, frequencyMap.get(wordToSearch)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<PageEntry> search(String wordsForSearch) {
        String[] words = wordsForSearch.toLowerCase(Locale.ROOT).split(" ");
        List<PageEntry> allWordsPageEntries = new ArrayList<>();
        List<PageEntry> allWordsResult = new ArrayList<>();
        for (String word : words) {
            if (allWordsEntriesMap.get(word) != null) {
                allWordsPageEntries.addAll(new ArrayList<>(allWordsEntriesMap.get(word)));
            }
        }
        Map<String, List<PageEntry>> pdfNameResMap = allWordsPageEntries.stream().collect(Collectors.groupingBy(PageEntry::getPdfName));
        for (Map.Entry<String, List<PageEntry>> entry : pdfNameResMap.entrySet()) {
            List<PageEntry> entryList = entry.getValue();
            Map<String, Map<Integer, Integer>> pageEntryMap = new HashMap<>();
            for (PageEntry pageEntry : entryList) {
                pageEntryMap.computeIfAbsent(pageEntry.getPdfName(), v -> new HashMap<>()).merge(pageEntry.getEntryPage(), pageEntry.getCount(), Integer::sum);
            }
            pageEntryMap.forEach((pdfName, v) -> {
                for (var pageCount : v.entrySet()) {
                    allWordsResult.add(new PageEntry(pdfName, pageCount.getKey(), pageCount.getValue()));
                }
            });
        }
        allWordsResult.sort(Collections.reverseOrder());
        return allWordsResult;
    }

    private void getStopList() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(stopWords))) {
            String line = br.readLine();
            while (line != null) {
                stopWordsList.add(line.toLowerCase(Locale.ROOT));
                line = br.readLine();
            }
        }
    }
}