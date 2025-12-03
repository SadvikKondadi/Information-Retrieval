//Name: K.Sadvik Student_ID: 11785837

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TextParser {

    private Set<String> terms = new HashSet<>();
    private Map<String, Integer> termMap = new LinkedHashMap<>();
    private Map<String, Integer> docMap = new LinkedHashMap<>();
    private Set<String> stop = new HashSet<>();
    private WordStemmer stemmer = new WordStemmer();

    private void loadStop(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String s;
        while ((s = br.readLine()) != null) stop.add(s.trim().toLowerCase());
        br.close();
        System.out.println("Loaded " + stop.size() + " stopwords successfully!");
    }

    private void readFolder(String dir) throws IOException {
        File folder = new File(dir);
        if (!folder.exists() || !folder.isDirectory()) throw new IOException("Invalid directory: " + dir);
        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) if (f.isFile()) readFile(f);
    }

    private void readFile(File f) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String s;
        while ((s = br.readLine()) != null) buf.append(s).append("\n");
        br.close();
        String[] docs = buf.toString().split("(?i)(?=<DOC[^>]*>)");
        for (String d : docs) {
            Matcher m1 = Pattern.compile("<DOCNO>\\s*(.*?)\\s*</DOCNO>", Pattern.CASE_INSENSITIVE).matcher(d);
            Matcher m2 = Pattern.compile("<TEXT>([\\s\\S]*?)</TEXT>", Pattern.CASE_INSENSITIVE).matcher(d);
            if (m1.find() && m2.find()) {
                String docNum = m1.group(1).trim();
                String content = m2.group(1);
                handleDoc(docNum, content);
            }
        }
    }

    private void handleDoc(String doc, String text) {
        Matcher m = Pattern.compile("FT911-(\\d+)").matcher(doc);
        if (m.find()) {
            int id = Integer.parseInt(m.group(1));
            docMap.put(doc, id);
        } else docMap.put(doc, docMap.size() + 1);
        for (String t : tokenize(text)) {
            if (!stop.contains(t)) {
                String st = stemmer.stemWord(t);
                if (!st.isEmpty() && !stop.contains(st)) terms.add(st);
            }
        }
    }

    private List<String> tokenize(String txt) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("[a-zA-Z]+").matcher(txt.toLowerCase());
        while (m.find()) list.add(m.group());
        return list;
    }

    private void assignIDs() {
        List<String> sorted = new ArrayList<>(terms);
        Collections.sort(sorted);
        int id = 1;
        for (String w : sorted) termMap.put(w, id++);
    }

    private void export(String out) throws IOException {
        assignIDs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        bw.write("   TextParser Output File\n");
        bw.write("   Created by: Sadvik Kondadi\n");
        bw.write("   Course: CSCE 5200 - Information Retrieval\n");
        bw.write("T\n TERMS AND THEIR IDs\n");
        for (Map.Entry<String, Integer> e : termMap.entrySet())
            bw.write(String.format("%-20s\t%d%n", e.getKey(), e.getValue()));
        bw.write("\n DOCUMENTS AND THEIR IDs \n");
        List<String> docs = new ArrayList<>(docMap.keySet());
        docs.sort(Comparator.comparingInt(docMap::get));
        for (String d : docs)
            bw.write(String.format("%-20s\t%d%n", d, docMap.get(d)));
        bw.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java TextParser <input_folder> <stopword_file> <output_file>");
            return;
        }
        TextParser p = new TextParser();
        p.loadStop(args[1]);
        p.readFolder(args[0]);
        p.export(args[2]);
        System.out.println("✅ Parsing complete! Output written to " + args[2]);
        System.out.println("👨‍💻 Project by: Sadvik Kondadi");
    }
}

class WordStemmer {
    private char[] b;
    private int i, i_end, j, k;
    private static final int INC = 50;

    public WordStemmer() {
        b = new char[INC];
        i = 0;
        i_end = 0;
    }

    public String stemWord(String word) {
        char[] w = word.toCharArray();
        add(w, w.length);
        stem();
        return toString();
    }

    private void add(char[] w, int wLen) {
        if (i + wLen >= b.length) {
            char[] new_b = new char[i + wLen + INC];
            System.arraycopy(b, 0, new_b, 0, i);
            b = new_b;
        }
        System.arraycopy(w, 0, b, i, wLen);
        i += wLen;
    }

    public String toString() {
        return new String(b, 0, i_end);
    }

    private boolean cons(int i) {
        switch (b[i]) {
            case 'a': case 'e': case 'i': case 'o': case 'u': return false;
            case 'y': return (i == 0) ? true : !cons(i - 1);
            default: return true;
        }
    }

    private int m() {
        int n = 0; int i = 0;
        while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
        i++;
        while (true) {
            while (true) { if (i > j) return n; if (cons(i)) break; i++; }
            i++; n++;
            while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
            i++;
        }
    }

    private boolean vowelInStem() {
        for (int i = 0; i <= j; i++) if (!cons(i)) return true;
        return false;
    }

    private boolean doubleC(int j) {
        if (j < 1) return false;
        if (b[j] != b[j - 1]) return false;
        return cons(j);
    }

    private boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
        char ch = b[i];
        return !(ch == 'w' || ch == 'x' || ch == 'y');
    }

    private boolean ends(String s) {
        int l = s.length();
        int o = k - l + 1;
        if (o < 0) return false;
        for (int i = 0; i < l; i++) if (b[o + i] != s.charAt(i)) return false;
        j = k - l;
        return true;
    }

    private void setTo(String s) {
        int l = s.length();
        int o = j + 1;
        for (int i = 0; i < l; i++) b[o + i] = s.charAt(i);
        k = j + l;
    }

    private void r(String s) { if (m() > 0) setTo(s); }

    private void step1() {
        if (b[k] == 's') {
            if (ends("sses")) k -= 2;
            else if (ends("ies")) setTo("i");
            else if (b[k - 1] != 's') k--;
        }
        if (ends("eed")) { if (m() > 0) k--; }
        else if ((ends("ed") || ends("ing")) && vowelInStem()) {
            k = j;
            if (ends("at")) setTo("ate");
            else if (ends("bl")) setTo("ble");
            else if (ends("iz")) setTo("ize");
            else if (doubleC(k)) {
                k--;
                char ch = b[k];
                if (ch == 'l' || ch == 's' || ch == 'z') k++;
            } else if (m() == 1 && cvc(k)) setTo("e");
        }
    }

    private void step2() { if (ends("y") && vowelInStem()) b[k] = 'i'; }

    private void step3() {
        if (k == 0) return;
        switch (b[k - 1]) {
            case 'a': if (ends("ational")) { r("ate"); break; }
                      if (ends("tional")) { r("tion"); break; } break;
            case 'c': if (ends("enci")) { r("ence"); break; }
                      if (ends("anci")) { r("ance"); break; } break;
            case 'e': if (ends("izer")) { r("ize"); break; } break;
            case 'l': if (ends("bli")) { r("ble"); break; } break;
            case 'o': if (ends("ization")) { r("ize"); break; }
                      if (ends("ation")) { r("ate"); break; }
                      if (ends("ator")) { r("ate"); break; } break;
            case 's': if (ends("alism")) { r("al"); break; }
                      if (ends("iveness")) { r("ive"); break; }
                      if (ends("fulness")) { r("ful"); break; }
                      if (ends("ousness")) { r("ous"); break; } break;
            case 't': if (ends("aliti")) { r("al"); break; }
                      if (ends("iviti")) { r("ive"); break; }
                      if (ends("biliti")) { r("ble"); break; } break;
            case 'g': if (ends("logi")) { r("log"); break; }
        }
    }

    private void step4() {
        switch (b[k]) {
            case 'e': if (ends("icate")) { r("ic"); break; }
                      if (ends("ative")) { r(""); break; }
                      if (ends("alize")) { r("al"); break; } break;
            case 'i': if (ends("iciti")) { r("ic"); break; } break;
            case 'l': if (ends("ful")) { r(""); break; } break;
            case 's': if (ends("ness")) { r(""); break; } break;
        }
    }

    private void step5() {
        if (k == 0) return;
        switch (b[k - 1]) {
            case 'a': if (ends("al")) break; else return;
            case 'c': if (ends("ance")) break; if (ends("ence")) break; return;
            case 'e': if (ends("er")) break; return;
            case 'i': if (ends("ic")) break; return;
            case 'l': if (ends("able")) break; if (ends("ible")) break; return;
            case 'n': if (ends("ant")) break; if (ends("ement")) break; if (ends("ment")) break; if (ends("ent")) break; return;
            case 'o': if ((ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) || ends("ou")) break; return;
            case 's': if (ends("ism")) break; return;
            case 't': if (ends("ate")) break; if (ends("iti")) break; return;
            case 'u': if (ends("ous")) break; return;
            case 'v': if (ends("ive")) break; return;
            case 'z': if (ends("ize")) break; return;
            default: return;
        }
        if (m() > 1) k = j;
    }

    private void step6() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || (a == 1 && !cvc(k - 1))) k--;
        }
        if (b[k] == 'l' && doubleC(k) && m() > 1) k--;
    }

    private void stem() {
        k = i - 1;
        if (k > 1) {
            step1(); step2(); step3(); step4(); step5(); step6();
        }
        i_end = k + 1;
        i = 0;
    }
}
