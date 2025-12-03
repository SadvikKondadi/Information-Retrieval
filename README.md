# Information Retrieval Search Engine  
### Text Parser • Indexer • Vector Space Model Query Processor

This repository contains a complete implementation of a mini search engine developed across three phases for **CSCE 5200 – Information Retrieval**.  
The project includes document parsing, index construction, TF–IDF weighting, vector space retrieval, ranking, and evaluation using the **TREC FT collection**.

---

## 🚀 Features  
- Document parsing: tokenization, stopword removal, Porter stemming  
- Dictionary, Forward Index, and Inverted Index generation  
- TF and TF–IDF weight computation  
- Vector Space Model with cosine similarity ranking  
- Query modes:
  - **Title**
  - **Title + Description**
  - **Title + Narrative**
- TREC-format ranked output  
- Precision & Recall evaluation using **main.qrels**  
- Internal → FT document ID mapping (`docids.txt`)

---

## 📁 Project Structure
├── TextParser.java

├── Indexer.java

├── QueryProcessor.java

├── dictionary.txt

├── forward_index.txt

├── inverted_index.txt

├── stopwordlist.txt

├── topics.txt

├── main.qrels

├── docids.txt

├── vsm_output_title.txt

├── vsm_output_titledesc.txt

├── vsm_output_titlenarr.txt

└── README.md


---

## 📘 Phase 1 – Text Parsing
Processes FT documents by:

- Extracting **DOCNO**  
- Lowercasing  
- Tokenizing  
- Removing stopwords  
- Applying **Porter Stemming**  
- Saving cleaned terms to `parser_output.txt`

This output is the basis for indexing.

---

## 📗 Phase 2 – Index Construction  
Builds all required index structures:

### **Dictionary**
Maps terms → termIDs  

### **Forward Index**
Stores `(termID, tf)` for each document  

### **Inverted Index**
Stores posting lists `(docID, tf)` for each term  

### Output Files
- `dictionary.txt`  
- `forward_index.txt`  
- `inverted_index.txt`

TF data stored here is used in Phase 3 to compute TF–IDF.

---

## 📙 Phase 3 – Query Processing (Vector Space Model)

### Query Handling
- Reads queries from `topics.txt`  
- Supports modes: **title**, **titledesc**, **titlenarr**  
- Preprocesses queries (tokenization → stopword removal → stemming)

### Scoring & Ranking
- Computes TF–IDF for documents and queries  
- Calculates cosine similarity using posting lists  
- Ranks documents by similarity score  
- Converts internal docIDs → FT IDs using `docids.txt`  

### Run Commands
```bash
javac QueryProcessor.java

java QueryProcessor title vsm_output_title.txt
java QueryProcessor titledesc vsm_output_titledesc.txt
java QueryProcessor titlenarr vsm_output_titlenarr.txt
