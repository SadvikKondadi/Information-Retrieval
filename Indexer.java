import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Indexer {

    private Map<String,Integer> termDict = new LinkedHashMap<>();
    private Map<Integer,String> idToTerm = new LinkedHashMap<>();
    private Map<String,Integer> docDict = new LinkedHashMap<>();
    private Map<Integer,Map<Integer,Integer>> forwardIndex = new TreeMap<>();
    private Map<Integer,Map<Integer,Integer>> invertedIndex = new TreeMap<>();
    private Set<String> stopwords = new HashSet<>();
    private Porter stemmer = new Porter();

    // Load Stopword List
    private void loadStopwords(String stopwordFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(stopwordFile));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (!line.isEmpty()) stopwords.add(line);
        }
        br.close();
        System.out.println("Loaded " + stopwords.size() + " stopwords.");
    }

    // Load Existing Dictionary from parser_output.txt
    private void loadDictionaryFromParser(String parserOutputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(parserOutputFile));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts.length < 2) continue;
            String term = parts[0].toLowerCase();
            try {
                int id = Integer.parseInt(parts[1]);
                if (!term.matches("[a-z]+")) continue;
                if (stopwords.contains(term)) continue;
                termDict.put(term, id);
                idToTerm.put(id, term);
            } catch (NumberFormatException e) {
                
            }
        }
        br.close();
        System.out.println("Loaded " + termDict.size() + " terms directly from parser_output.txt (keeping same IDs).");
    }

    // Build Forward & Inverted Indices
    private void buildIndices(String inputPath) throws IOException {
        File in = new File(inputPath);
        if (in.isDirectory()) {
            File[] files = in.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File f : files)
                    if (f.isFile()) processFile(f);
            }
        } else {
            processFile(in);
        }
    }

    private void processFile(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line, docNo = null;
        StringBuilder text = new StringBuilder();
        boolean inText = false;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("<DOCNO>")) {
                docNo = line.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
            } else if (line.startsWith("<TEXT>")) {
                text.setLength(0);
                inText = true;
            } else if (line.startsWith("</TEXT>")) {
                inText = false;
                if (docNo != null) {
                    int docID = extractDocID(docNo);
                    docDict.put(docNo, docID);
                    processDocument(docID, text.toString());
                }
            } else if (inText) {
                text.append(line).append(" ");
            }
        }
        br.close();
    }

    private int extractDocID(String docNo) {
        try {
            String[] parts = docNo.split("-");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return docDict.size() + 1;
        }
    }

    private void processDocument(int docID, String content) {
        Matcher m = Pattern.compile("[a-zA-Z]+").matcher(content.toLowerCase());
        Map<Integer,Integer> freq = new TreeMap<>();

        while (m.find()) {
            String w = m.group();
            if (stopwords.contains(w)) continue;
            String stemmed = stemmer.stripAffixes(w);
            Integer termID = termDict.get(stemmed);
            if (termID == null) continue;
            freq.put(termID, freq.getOrDefault(termID, 0) + 1);
        }

        if (!freq.isEmpty()) {
            forwardIndex.put(docID, freq);
            for (Map.Entry<Integer,Integer> e : freq.entrySet()) {
                invertedIndex.computeIfAbsent(e.getKey(), k -> new TreeMap<>()).put(docID, e.getValue());
            }
        }
    }

    // Save Index Files
    private void saveIndex(Map<Integer,Map<Integer,Integer>> map, String filename) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
        for (Map.Entry<Integer,Map<Integer,Integer>> e : map.entrySet()) {
            bw.write(e.getKey() + ": ");
            Map<Integer,Integer> inner = new TreeMap<>(e.getValue());
            for (Map.Entry<Integer,Integer> v : inner.entrySet()) {
                bw.write(v.getKey() + ":" + v.getValue() + "; ");
            }
            bw.newLine();
        }
        bw.close();
    }

    private void saveDictionary(String filename) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
        for (Map.Entry<String,Integer> e : termDict.entrySet()) {
            bw.write(e.getKey() + " = " + e.getValue());
            bw.newLine();
        }
        bw.close();
    }

    // Interactive Search
    private void interactiveSearch() throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("\nEnter a word to search: ");
        String word = sc.nextLine().trim().toLowerCase();
        if (stopwords.contains(word)) {
            System.out.println("The word is a stopword and not indexed.");
            sc.close();
            return;
        }
        String stem = stemmer.stripAffixes(word);
        Integer id = termDict.get(stem);
        if (id == null) {
            System.out.println("Word not found in dictionary.");
            sc.close();
            return;
        }
        Map<Integer,Integer> postings = invertedIndex.get(id);
        if (postings == null) {
            System.out.println("No postings found for " + stem);
            sc.close();
            return;
        }
        System.out.print(stem + " (ID=" + id + "): ");
        for (Map.Entry<Integer,Integer> e : postings.entrySet()) {
            System.out.print(e.getKey() + ":" + e.getValue() + "; ");
        }
        System.out.println();
        sc.close();
    }

    // Main
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java Indexer <parser_output.txt> <forward_index> <inverted_index> <dictionary>");
            return;
        }

        Indexer idx = new Indexer();
        idx.loadStopwords("stopwordlist.txt");

        long start = System.currentTimeMillis();
        idx.loadDictionaryFromParser(args[0]);
        idx.buildIndices("./ft911");

        idx.saveIndex(idx.forwardIndex, args[1]);
        idx.saveIndex(idx.invertedIndex, args[2]);
        idx.saveDictionary(args[3]);

        long end = System.currentTimeMillis();
        System.out.println("\n✅ Indexing complete in " + (end - start)/1000.0 + " seconds.");
        System.out.println("Terms: " + idx.termDict.size() + " | Documents: " + idx.docDict.size());
        System.out.println("Forward index: " + args[1]);
        System.out.println("Inverted index: " + args[2]);
        System.out.println("Dictionary: " + args[3]);

        idx.interactiveSearch();
    }
}
