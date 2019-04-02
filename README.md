# Web crawler 

Implementation of web crawler in Java that crawls .gov.si web sites. 

The crawler consists of several parts:
- HTTP downloader and renderer, implemented with HTMLUnit
- Data extractor, implemented with JSoup
	- extracting links from web pages from href in <a></a> tags
	- extracting images from <img /> tags
	- extracting documents (.pdf, .doc, .docx, .ppt, .pptx)
- Duplicate detector - we detect already visited and parsed pages (based on hashes of HTML content) 
- URL Frontier implemented as a Queue
	- parsing robots.txt files and crawling only allowed pages with specified crawl delay
	- adding links from sitemap directly to frontier
- The crawler follows breadth-first strategy strategy for extracting links and allows parallel retrieval implemented with threads. 
- Retrieved data is stored in PostgreSQL database.

*** HOW TO RUN

- set up database using prepared script in the directory (baza.sql)
- open project in IntelliJ Idea
- setup parameters in class Main.java:
	- url = "jdbc:postgresql://localhost:5432/"
        - dbName = <DBname>
        - user = <PostgreSQLuser>
	- password = <PostgreSQLpassword>
    	- numberOfThreads = <wantedNumberOfThreads>
- Run
