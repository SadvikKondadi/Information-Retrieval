

public class Porter {

    public String stripAffixes(String word) {
        if (word == null || word.length() == 0)
            return word;
        word = clean(word.toLowerCase());
        if (word.length() <= 2)
            return word;
        word = step1(word);
        word = step2(word);
        word = step3(word);
        word = step4(word);
        word = step5(word);
        return word;
    }

    private String clean(String s) {
        return s.replaceAll("[^a-z]", "");
    }

    private boolean vowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }

    private boolean containsVowel(String s) {
        for (char c : s.toCharArray())
            if (vowel(c))
                return true;
        return false;
    }

    private boolean endsWithDouble(String s) {
        int len = s.length();
        return len >= 2 && s.charAt(len - 1) == s.charAt(len - 2);
    }

    private String step1(String s) {
        if (s.endsWith("sses"))
            return s.substring(0, s.length() - 2);
        if (s.endsWith("ies"))
            return s.substring(0, s.length() - 2);
        if (s.endsWith("ss"))
            return s;
        if (s.endsWith("s"))
            return s.substring(0, s.length() - 1);
        return s;
    }

    private String step2(String s) {
        if (s.endsWith("eed")) {
            String stem = s.substring(0, s.length() - 3);
            if (measure(stem) > 0)
                return stem + "ee";
        } else if ((s.endsWith("ed") && containsVowel(s.substring(0, s.length() - 2))) ||
                   (s.endsWith("ing") && containsVowel(s.substring(0, s.length() - 3)))) {
            if (s.endsWith("ed"))
                s = s.substring(0, s.length() - 2);
            else
                s = s.substring(0, s.length() - 3);

            if (s.endsWith("at") || s.endsWith("bl") || s.endsWith("iz"))
                s = s + "e";
            else if (endsWithDouble(s) && !s.endsWith("ll") && !s.endsWith("ss") && !s.endsWith("zz"))
                s = s.substring(0, s.length() - 1);
            else if (measure(s) == 1 && cvc(s))
                s = s + "e";
        }
        return s;
    }

    private String step3(String s) {
        if (s.endsWith("y") && containsVowel(s.substring(0, s.length() - 1)))
            s = s.substring(0, s.length() - 1) + "i";
        return s;
    }

    private String step4(String s) {
        String[][] suffixes = {
                {"ational", "ate"}, {"tional", "tion"}, {"enci", "ence"}, {"anci", "ance"},
                {"izer", "ize"}, {"abli", "able"}, {"alli", "al"}, {"entli", "ent"},
                {"eli", "e"}, {"ousli", "ous"}, {"ization", "ize"}, {"ation", "ate"},
                {"ator", "ate"}, {"alism", "al"}, {"iveness", "ive"}, {"fulness", "ful"},
                {"ousness", "ous"}, {"aliti", "al"}, {"iviti", "ive"}, {"biliti", "ble"}
        };
        for (String[] suf : suffixes) {
            if (s.endsWith(suf[0])) {
                String stem = s.substring(0, s.length() - suf[0].length());
                if (measure(stem) > 0)
                    return stem + suf[1];
            }
        }
        return s;
    }

    private String step5(String s) {
        String[][] suffixes = {
                {"icate", "ic"}, {"ative", ""}, {"alize", "al"}, {"iciti", "ic"},
                {"ical", "ic"}, {"ful", ""}, {"ness", ""}
        };
        for (String[] suf : suffixes) {
            if (s.endsWith(suf[0])) {
                String stem = s.substring(0, s.length() - suf[0].length());
                if (measure(stem) > 0)
                    return stem + suf[1];
            }
        }
        return s;
    }

    private int measure(String stem) {
        boolean vowelSeen = false;
        int count = 0;
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if (vowel(c)) {
                vowelSeen = true;
            } else if (vowelSeen) {
                count++;
                vowelSeen = false;
            }
        }
        return count;
    }

    private boolean cvc(String s) {
        if (s.length() < 3) return false;
        char c1 = s.charAt(s.length() - 1);
        char c2 = s.charAt(s.length() - 2);
        char c3 = s.charAt(s.length() - 3);
        return !vowel(c1) && vowel(c2) && !vowel(c3)
                && (c1 != 'w' && c1 != 'x' && c1 != 'y');
    }
}
