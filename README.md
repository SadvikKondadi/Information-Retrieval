Information Retrieval Search Engine
Text Parser • Indexer • Vector Space Model Query Processor
Course: CSCE 5200 – Information Retrieval
This repository contains a complete end-to-end implementation of a mini search engine developed across three phases. The project covers document parsing, index construction, vector space retrieval, and performance evaluation using the TREC FT document collection. All components are implemented in Java.
🚀 Features
Document parsing with tokenization, stopword removal, and Porter stemming
Construction of dictionary, forward index, and inverted index
TF and TF–IDF term weighting
Vector Space Model with cosine similarity scoring
Support for three query modes:
Title
Title + Description
Title + Narrative
Ranking and TREC-format output generation
Evaluation with precision and recall using main.qrels
Mapping of internal docIDs to FT document identifiers (e.g., FT911-4016)
📁 Project Structure
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
📘 Phase 1 – Text Parsing
Phase 1 handles preprocessing of the FT dataset:
Extract DOCNO tags
Normalize text to lowercase
Tokenize into terms
Remove stopwords using a given stopword list
Apply Porter stemming
Save cleaned document terms in parser_output.txt
Output is used directly for indexing in Phase 2.
📗 Phase 2 – Index Construction
Phase 2 creates all required index structures for retrieval:
Dictionary: Assigns a unique termID to each term
Forward Index: Stores (termID, tf) pairs for every document
Inverted Index: Stores posting lists with (docID, tf) for each term
Outputs:
dictionary.txt
forward_index.txt
inverted_index.txt
These files store all term frequencies required for TF–IDF computation in Phase 3.
📙 Phase 3 – Query Processing (Vector Space Model)
Phase 3 implements the retrieval system:
Query Handling
Reads topics.txt
Supports three modes:
title
titledesc
titlenarr
Preprocesses query terms (tokenization → stopword removal → stemming)
Scoring & Ranking
Computes TF–IDF for query and documents
Calculates cosine similarity using the inverted index
Ranks documents in descending order of similarity
Uses docids.txt to map internal docIDs to FT document IDs
Running the system:
javac QueryProcessor.java

java QueryProcessor title vsm_output_title.txt
java QueryProcessor titledesc vsm_output_titledesc.txt
java QueryProcessor titlenarr vsm_output_titlenarr.txt
Output files follow TREC format:
<QueryID> <DOCNO> <Rank> <Score>
📊 Evaluation
Performance is evaluated using relevance judgments from main.qrels.
Metrics include:
Precision
Recall
Comparison across query modes
Findings:
Title-only queries provide fast results but lower recall
Title + Description gives the best balanced performance
Title + Narrative increases recall but introduces noise
🛠️ Technologies Used
Java
Porter Stemmer
Vector Space Model
TREC Evaluation Format
📌 Notes
Document identifiers are mapped using docids.txt.
Only term frequencies are stored in index files; IDF is computed at runtime.
Cosine similarity is implemented directly using posting lists for efficiency.
📬 Author
Sadvik Kondadi
CSCE 5200 – Information Retrieval
