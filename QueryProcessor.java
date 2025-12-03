// Name: Sadvik Kondadi 
// Student Id: 11785837


import java.io.*;
import java.util.*;
import java.util.regex.*;

public class QueryProcessor {

    enum QueryMode {
        TITLE,
        TITLE_DESC,
        TITLE_NARR
    }

    static class Posting {
        int docId;
        int tf;
        Posting(int d, int t) {
            this.docId = d;
            this.tf = t;
        }
    }

    // term -> termID
    private Map<String, Integer> termToId = new HashMap<>();
    // termID -> postings list
    private Map<Integer, List<Posting>> inverted = new HashMap<>();
    // termID -> idf
    private Map<Integer, Double> idf = new HashMap<>();
    // docID -> |d|
    private Map<Integer, Double> docNorm = new HashMap<>();
    // stopwords
    private Set<String> stopwords = new HashSet<>();
    // docID -> DOCNO (FT923-3189)
    private Map<Integer, String> docIdToDocNo = new HashMap<>();

    private int numDocs = 0;

    private Porter porter = new Porter();

    // MAIN

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java QueryProcessor <mode> <outputFile>");
            System.err.println("  <mode>: title | titledesc | titlenarr");
            return;
        }

        QueryMode mode;
        switch (args[0].toLowerCase()) {
            case "title":
                mode = QueryMode.TITLE;
                break;
            case "titledesc":
                mode = QueryMode.TITLE_DESC;
                break;
            case "titlenarr":
                mode = QueryMode.TITLE_NARR;
                break;
            default:
                System.err.println("Unknown mode: " + args[0]);
                return;
        }

        String outputFile = args[1];

        QueryProcessor qp = new QueryProcessor();
        qp.loadStopwords("stopwordlist.txt");
        qp.loadDictionary("dictionary.txt");
        qp.loadInvertedIndex("inverted_index.txt");

        // Try to load mapping docID -> DOCNO (FT923-3189)
        try {
            qp.loadDocIdMapping("docids.txt");
        } catch (IOException e) {
            System.err.println("Warning: could not load docids.txt; "
                    + "DOCUMENT field will be numeric docID.");
        }

        qp.computeIdf();
        qp.computeDocNorms("forward_index.txt");

        List<Topic> topics = qp.loadTopics("topics.txt");
        qp.processAllTopics(topics, mode, outputFile);
    }

    // LOADERS

    private void loadStopwords(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (!line.isEmpty()) {
                stopwords.add(line);
            }
        }
        br.close();
    }

    private void loadDictionary(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // format: term = id
            int eqPos = line.indexOf('=');
            if (eqPos < 0) continue;
            String term = line.substring(0, eqPos).trim();
            String idStr = line.substring(eqPos + 1).trim();
            if (term.isEmpty() || idStr.isEmpty()) continue;
            try {
                int termId = Integer.parseInt(idStr);
                termToId.put(term, termId);
            } catch (NumberFormatException ex) {
                // ignore malformed
            }
        }
        br.close();
    }

    private void loadInvertedIndex(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        Set<Integer> docsSeen = new HashSet<>();

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // format: termId: docId:tf; docId:tf; ...
            int firstColon = line.indexOf(':');
            if (firstColon < 0) continue;

            String termIdStr = line.substring(0, firstColon).trim();
            int termId;
            try {
                termId = Integer.parseInt(termIdStr);
            } catch (NumberFormatException ex) {
                continue;
            }

            String rest = line.substring(firstColon + 1).trim();
            List<Posting> plist = new ArrayList<>();
            String[] postings = rest.split(";");
            for (String p : postings) {
                p = p.trim();
                if (p.isEmpty()) continue;
                String[] dt = p.split(":");
                if (dt.length != 2) continue;
                try {
                    int docId = Integer.parseInt(dt[0].trim());
                    int tf = Integer.parseInt(dt[1].trim());
                    plist.add(new Posting(docId, tf));
                    docsSeen.add(docId);
                } catch (NumberFormatException ex) {
                    // ignore weird tokens
                }
            }
            if (!plist.isEmpty()) {
                inverted.put(termId, plist);
            }
        }
        br.close();
        numDocs = docsSeen.size();
    }

    private void computeIdf() {
        for (Map.Entry<Integer, List<Posting>> e : inverted.entrySet()) {
            int termId = e.getKey();
            int df = e.getValue().size();
            if (df == 0) continue;
            double value = Math.log((double) numDocs / (double) df);
            idf.put(termId, value);
        }
    }

    private void computeDocNorms(String filename) throws IOException {
        Map<Integer, Map<Integer, Integer>> docTermTf = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        Pattern colonPattern = Pattern.compile(":");

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int firstColon = line.indexOf(':');
            if (firstColon < 0) continue;

            String docIdStr = line.substring(0, firstColon).trim();
            int docId;
            try {
                docId = Integer.parseInt(docIdStr);
            } catch (NumberFormatException ex) {
                continue;
            }

            String rest = line.substring(firstColon + 1).trim();
            Map<Integer, Integer> tfMap = new HashMap<>();
            String[] pairs = rest.split(";");
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.isEmpty()) continue;
                String[] tidTf = colonPattern.split(pair);
                if (tidTf.length != 2) continue;
                try {
                    int termId = Integer.parseInt(tidTf[0].trim());
                    int tf = Integer.parseInt(tidTf[1].trim());
                    tfMap.put(termId, tf);
                } catch (NumberFormatException ex) {
                    // ignore malformed
                }
            }
            docTermTf.put(docId, tfMap);
        }
        br.close();

        for (Map.Entry<Integer, Map<Integer, Integer>> e : docTermTf.entrySet()) {
            int docId = e.getKey();
            Map<Integer, Integer> tfMap = e.getValue();
            double sumSq = 0.0;

            for (Map.Entry<Integer, Integer> tfEntry : tfMap.entrySet()) {
                int termId = tfEntry.getKey();
                int tf = tfEntry.getValue();
                Double idfVal = idf.get(termId);
                if (idfVal == null) continue;
                double w = (1.0 + Math.log(tf)) * idfVal;
                sumSq += w * w;
            }
            docNorm.put(docId, Math.sqrt(sumSq));
        }
    }

    private void loadDocIdMapping(String filename) throws IOException {
        // Format: docId DOCNO
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts.length < 2) continue;
            try {
                int docId = Integer.parseInt(parts[0]);
                String docno = parts[1];
                docIdToDocNo.put(docId, docno);
            } catch (NumberFormatException ex) {
                // ignore malformed
            }
        }
        br.close();
        System.out.println("Loaded " + docIdToDocNo.size() + " docID->DOCNO mappings from " + filename);
    }

    // TOPIC PARSING

    static class Topic {
        int number;
        String title;
        String desc;
        String narr;
    }

    private List<Topic> loadTopics(String filename) throws IOException {
        List<Topic> topics = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        Topic current = null;
        StringBuilder descBuf = null;
        StringBuilder narrBuf = null;
        boolean inDesc = false;
        boolean inNarr = false;

        while ((line = br.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("<top>")) {
                current = new Topic();
                descBuf = new StringBuilder();
                narrBuf = new StringBuilder();
                inDesc = false;
                inNarr = false;
            } else if (line.startsWith("</top>")) {
                if (current != null) {
                    current.desc = descBuf.toString();
                    current.narr = narrBuf.toString();
                    topics.add(current);
                }
                current = null;
                descBuf = null;
                narrBuf = null;
                inDesc = false;
                inNarr = false;
            } else if (line.startsWith("<num>")) {
                // <num> Number: 352
                String[] parts = line.split("Number:");
                if (parts.length == 2 && current != null) {
                    String numStr = parts[1].trim();
                    try {
                        current.number = Integer.parseInt(numStr);
                    } catch (NumberFormatException ex) {
                        current.number = -1;
                    }
                }
            } else if (line.startsWith("<title>")) {
                if (current != null) {
                    current.title = line.replace("<title>", "").trim();
                }
            } else if (line.startsWith("<desc>")) {
                inDesc = true;
                inNarr = false;
            } else if (line.startsWith("<narr>")) {
                inDesc = false;
                inNarr = true;
            } else {
                if (inDesc && descBuf != null) {
                    descBuf.append(" ").append(line);
                } else if (inNarr && narrBuf != null) {
                    narrBuf.append(" ").append(line);
                }
            }
        }

        br.close();
        return topics;
    }

    // QUERY PROCESSING

    private void processAllTopics(List<Topic> topics, QueryMode mode, String outputFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));

        for (Topic t : topics) {
            if (t == null) continue;

            String text;
            switch (mode) {
                case TITLE:
                    text = t.title;
                    break;
                case TITLE_DESC:
                    text = (t.title == null ? "" : t.title) + " " + (t.desc == null ? "" : t.desc);
                    break;
                case TITLE_NARR:
                    text = (t.title == null ? "" : t.title) + " " + (t.narr == null ? "" : t.narr);
                    break;
                default:
                    text = t.title;
            }

            List<String> qTerms = preprocess(text);
            Map<Integer, Double> scores = scoreQuery(qTerms);
            List<Map.Entry<Integer, Double>> ranked = new ArrayList<>(scores.entrySet());
            ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            int rank = 1;
            for (Map.Entry<Integer, Double> e : ranked) {
                int docId = e.getKey();
                double score = e.getValue();
                if (score <= 0.0) continue;

                // Prefer DOCNO (FT923-3189) if mapping exists, otherwise numeric docId
                String docno = docIdToDocNo.getOrDefault(docId, String.valueOf(docId));

                bw.write(t.number + "\t" +
                         docno + "\t" +
                         rank + "\t" +
                         String.format(Locale.US, "%.6f", score));
                bw.newLine();
                rank++;
            }
        }

        bw.flush();
        bw.close();
    }

    private List<String> preprocess(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;

        text = text.toLowerCase();
        text = text.replaceAll("[^a-z0-9]+", " ");

        String[] tokens = text.split("\\s+");
        for (String tok : tokens) {
            if (tok.isEmpty()) continue;
            if (stopwords.contains(tok)) continue;
            String stem = porter.stripAffixes(tok);
            if (stem == null || stem.isEmpty()) continue;
            if (stopwords.contains(stem)) continue;
            result.add(stem);
        }
        return result;
    }

    private Map<Integer, Double> scoreQuery(List<String> queryTerms) {
        Map<Integer, Integer> qtf = new HashMap<>();

        for (String term : queryTerms) {
            Integer termId = termToId.get(term);
            if (termId == null) continue;
            qtf.put(termId, qtf.getOrDefault(termId, 0) + 1);
        }

        if (qtf.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Double> qWeight = new HashMap<>();
        double qNormSq = 0.0;

        for (Map.Entry<Integer, Integer> e : qtf.entrySet()) {
            int termId = e.getKey();
            int tf = e.getValue();
            Double idfVal = idf.get(termId);
            if (idfVal == null) continue;
            double w = (1.0 + Math.log(tf)) * idfVal;
            qWeight.put(termId, w);
            qNormSq += w * w;
        }

        double qNorm = Math.sqrt(qNormSq);
        if (qNorm == 0.0) {
            return Collections.emptyMap();
        }

        Map<Integer, Double> dotProd = new HashMap<>();

        for (Map.Entry<Integer, Double> e : qWeight.entrySet()) {
            int termId = e.getKey();
            double wq = e.getValue();
            List<Posting> plist = inverted.get(termId);
            if (plist == null) continue;

            double idfVal = idf.get(termId);
            for (Posting p : plist) {
                Double docNormVal = docNorm.get(p.docId);
                if (docNormVal == null || docNormVal == 0.0) continue;
                double wd = (1.0 + Math.log(p.tf)) * idfVal;
                double partial = wq * wd;
                dotProd.put(p.docId, dotProd.getOrDefault(p.docId, 0.0) + partial);
            }
        }

        Map<Integer, Double> scores = new HashMap<>();
        for (Map.Entry<Integer, Double> e : dotProd.entrySet()) {
            int docId = e.getKey();
            double dot = e.getValue();
            Double dn = docNorm.get(docId);
            if (dn == null || dn == 0.0) continue;
            double score = dot / (dn * qNorm);
            scores.put(docId, score);
        }

        return scores;
    }
}
