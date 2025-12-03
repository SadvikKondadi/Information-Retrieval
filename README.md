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
